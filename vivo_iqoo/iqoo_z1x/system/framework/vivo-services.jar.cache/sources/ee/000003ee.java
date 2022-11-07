package com.android.server.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telecom.DefaultDialerManager;
import com.android.server.UnifiedConfigThread;
import com.android.server.am.firewall.VivoAppIsolationController;
import com.android.server.policy.WindowManagerPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoKeyguardOverlayController {
    static final String ACTION_KOC_UPDATE_KEYWORDS = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_KeyguardOverlayController";
    static final String ACTION_KOC_UPDATE_WHITELIST = "com.vivo.permissionmanger.ACTION_LOAD_DB";
    private static final boolean DBG;
    public static final boolean IS_ENG = Build.TYPE.equals("branddebug");
    public static final boolean IS_LOG_CTRL_OPEN;
    private static final String KEYWORD_DISABLEKOC = "disableKOC";
    private static final String KEYWORD_ENABLEKOC = "enableKOC";
    public static final String KEY_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    private static final String KOC_FEATURE_ENABLE = "enable_koc_feature";
    private static final String PM_CURRENT_STATE = "currentstate";
    private static String PM_KOC_URI = null;
    private static final String PM_PKGNAME = "pkgname";
    private static final String PM_PKG_UID = "pkguid";
    private static final String PM_SET_BY_USER = "setbyuser";
    private static final String REASON_ALLOWED_IN_PERMISSIONLIST = "allowedInlist";
    private static final String REASON_DEFAULT = "notInAnyList";
    private static final String REASON_FORBID_IN_PERMISSIONLIST = "fobidInlist";
    private static final String REASON_IN_BLACKLIST = "forbidInBlacklist";
    private static final String REASON_KOC_DISABLED = "kocDisabled";
    private static final String TAG = "VivoKeyguardOverlayController";
    private static final String UCS_KOC_URI = "content://com.vivo.daemonservice.unifiedconfigprovider/configs";
    private static boolean sPermissionManagerExist;
    private ContentObserver mContentObserver;
    private Context mContext;
    private boolean mEnableBlackList;
    private Handler mHandler;
    private IntentFilter mIntentFilter;
    private int mPkgRule;
    private boolean mEnabled = true;
    private HashMap<String, Integer> mPkgWhitelist = new HashMap<>();
    private ArrayList<String> mKOCKeywords = new ArrayList<>();
    private ArrayList<String> mKOCForbidComponents = new ArrayList<>();
    private WindowManagerPolicy.WindowState mLastOverlayWindow = null;
    private boolean mNeedForbid = false;
    private Runnable readPMKOCRunnable = new Runnable() { // from class: com.android.server.policy.VivoKeyguardOverlayController.2
        @Override // java.lang.Runnable
        public void run() {
            if (VivoKeyguardOverlayController.DBG) {
                VSlog.d(VivoKeyguardOverlayController.TAG, "readPMKOCRunnable!");
            }
            VivoKeyguardOverlayController.this.getKOCInfoFromPM();
        }
    };
    private Runnable readUCSKOCRunnable = new Runnable() { // from class: com.android.server.policy.VivoKeyguardOverlayController.3
        @Override // java.lang.Runnable
        public void run() {
            if (VivoKeyguardOverlayController.DBG) {
                VSlog.d(VivoKeyguardOverlayController.TAG, "readUCSKOCRunnable!");
            }
            VivoKeyguardOverlayController.this.getKOCInfoFromUCS();
        }
    };
    BroadcastReceiver mKOCReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.VivoKeyguardOverlayController.4
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            VSlog.d(VivoKeyguardOverlayController.TAG, "DEBUG_KOC:PMDBListener onReceive intent=" + intent);
            if (VivoKeyguardOverlayController.ACTION_KOC_UPDATE_KEYWORDS.equals(intent.getAction())) {
                VivoKeyguardOverlayController.this.mHandler.removeCallbacks(VivoKeyguardOverlayController.this.readUCSKOCRunnable);
                VivoKeyguardOverlayController.this.mHandler.postDelayed(VivoKeyguardOverlayController.this.readUCSKOCRunnable, 500L);
            } else if (VivoKeyguardOverlayController.ACTION_KOC_UPDATE_WHITELIST.equals(intent.getAction())) {
                VivoKeyguardOverlayController.this.startObserver();
                VivoKeyguardOverlayController.this.mHandler.removeCallbacks(VivoKeyguardOverlayController.this.readPMKOCRunnable);
                VivoKeyguardOverlayController.this.mHandler.removeCallbacks(VivoKeyguardOverlayController.this.readUCSKOCRunnable);
                VivoKeyguardOverlayController.this.mHandler.postDelayed(VivoKeyguardOverlayController.this.readPMKOCRunnable, 500L);
                VivoKeyguardOverlayController.this.mHandler.postDelayed(VivoKeyguardOverlayController.this.readUCSKOCRunnable, 500L);
            } else if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                VivoKeyguardOverlayController.this.stopObserver();
                VivoKeyguardOverlayController.this.mHandler.removeCallbacks(VivoKeyguardOverlayController.this.readPMKOCRunnable);
                VivoKeyguardOverlayController.this.mHandler.postDelayed(VivoKeyguardOverlayController.this.readPMKOCRunnable, 500L);
                VivoKeyguardOverlayController.this.startObserver();
            }
        }
    };

    static {
        boolean equals = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
        IS_LOG_CTRL_OPEN = equals;
        DBG = equals || IS_ENG;
        PM_KOC_URI = "content://com.vivo.permissionmanager.provider.permission/control_locked_screen_action";
        sPermissionManagerExist = true;
    }

    private static boolean isPermissionManagerAppExisted(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo("com.vivo.permissionmanager", 0);
        } catch (Exception e) {
            VSlog.e(TAG, "com.vivo.permissionmanager apk is not exsisted !");
        }
        return applicationInfo != null;
    }

    public VivoKeyguardOverlayController(Context context) {
        this.mEnableBlackList = true;
        this.mPkgRule = 0;
        boolean isPermissionManagerAppExisted = isPermissionManagerAppExisted(context);
        sPermissionManagerExist = isPermissionManagerAppExisted;
        if (!isPermissionManagerAppExisted) {
            PM_KOC_URI = "content://com.iqoo.secure.provider.secureprovider/control_locked_screen_action";
        }
        this.mContext = context;
        this.mHandler = UnifiedConfigThread.getHandler();
        this.mContentObserver = new DBObserver(this.mHandler);
        this.mEnableBlackList = true;
        this.mPkgRule = 1;
        IntentFilter KOCFilter = new IntentFilter();
        KOCFilter.addAction(ACTION_KOC_UPDATE_KEYWORDS);
        KOCFilter.addAction(ACTION_KOC_UPDATE_WHITELIST);
        KOCFilter.addAction("android.intent.action.USER_SWITCHED");
        context.registerReceiver(this.mKOCReceiver, KOCFilter);
        try {
            Settings.System.putInt(this.mContext.getContentResolver(), KOC_FEATURE_ENABLE, 1);
        } catch (Exception e) {
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:22:0x0084, code lost:
        if (r9 == null) goto L20;
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x0089, code lost:
        if (com.android.server.policy.VivoKeyguardOverlayController.DBG == false) goto L23;
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x008b, code lost:
        vivo.util.VSlog.d(com.android.server.policy.VivoKeyguardOverlayController.TAG, "getKOCInfoFromPM END itemInfoMap=" + r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x009f, code lost:
        r11.mPkgWhitelist = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x00a1, code lost:
        return true;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    boolean getKOCInfoFromPM() {
        /*
            r11 = this;
            boolean r0 = com.android.server.policy.VivoKeyguardOverlayController.DBG
            java.lang.String r1 = "VivoKeyguardOverlayController"
            if (r0 == 0) goto Lb
            java.lang.String r0 = "getKOCInfoFromPM START"
            vivo.util.VSlog.d(r1, r0)
        Lb:
            java.util.HashMap r0 = new java.util.HashMap
            r0.<init>()
            android.content.Context r2 = r11.mContext
            android.content.ContentResolver r2 = r2.getContentResolver()
            r9 = 0
            r10 = 1
            java.lang.String r3 = com.android.server.policy.VivoKeyguardOverlayController.PM_KOC_URI     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            android.net.Uri r3 = android.net.Uri.parse(r3)     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            int r4 = android.app.ActivityManager.getCurrentUser()     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            android.net.Uri r4 = android.content.ContentProvider.maybeAddUserId(r3, r4)     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            java.lang.String r3 = "_id"
            java.lang.String r5 = "pkgname"
            java.lang.String r6 = "currentstate"
            java.lang.String[] r5 = new java.lang.String[]{r3, r5, r6}     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            r6 = 0
            r7 = 0
            r8 = 0
            r3 = r2
            android.database.Cursor r3 = r3.query(r4, r5, r6, r7, r8)     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            r9 = r3
            if (r9 == 0) goto L78
            boolean r3 = r9.moveToFirst()     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            if (r3 == 0) goto L78
        L41:
            java.lang.String r4 = r9.getString(r10)     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            r5 = 2
            int r5 = r9.getInt(r5)     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            java.lang.Integer r6 = java.lang.Integer.valueOf(r5)     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            r0.put(r4, r6)     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            boolean r6 = com.android.server.policy.VivoKeyguardOverlayController.DBG     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            if (r6 == 0) goto L71
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            r6.<init>()     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            java.lang.String r7 = "GET pkgName="
            r6.append(r7)     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            r6.append(r4)     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            java.lang.String r7 = " currentState="
            r6.append(r7)     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            r6.append(r5)     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            vivo.util.VSlog.d(r1, r6)     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
        L71:
            boolean r6 = r9.moveToNext()     // Catch: java.lang.Throwable -> L7e java.lang.Exception -> L80
            r3 = r6
            if (r3 != 0) goto L41
        L78:
            if (r9 == 0) goto L87
        L7a:
            r9.close()
            goto L87
        L7e:
            r1 = move-exception
            goto La2
        L80:
            r3 = move-exception
            r3.printStackTrace()     // Catch: java.lang.Throwable -> L7e
            if (r9 == 0) goto L87
            goto L7a
        L87:
            boolean r3 = com.android.server.policy.VivoKeyguardOverlayController.DBG
            if (r3 == 0) goto L9f
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "getKOCInfoFromPM END itemInfoMap="
            r3.append(r4)
            r3.append(r0)
            java.lang.String r3 = r3.toString()
            vivo.util.VSlog.d(r1, r3)
        L9f:
            r11.mPkgWhitelist = r0
            return r10
        La2:
            if (r9 == 0) goto La7
            r9.close()
        La7:
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.VivoKeyguardOverlayController.getKOCInfoFromPM():boolean");
    }

    boolean getKOCInfoFromUCS() {
        return getKOCInfoFromUCS(UCS_KOC_URI, "KeyguardOverlayController", "1", "1.0", this.mKOCForbidComponents);
    }

    /* JADX WARN: Code restructure failed: missing block: B:26:0x00b1, code lost:
        if (r10 != null) goto L22;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x00b3, code lost:
        r10.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x00da, code lost:
        if (r10 == null) goto L23;
     */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x00df, code lost:
        return !r11;
     */
    /* JADX WARN: Removed duplicated region for block: B:42:0x00e3  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean getKOCInfoFromUCS(java.lang.String r14, java.lang.String r15, java.lang.String r16, java.lang.String r17, java.util.ArrayList<java.lang.String> r18) {
        /*
            Method dump skipped, instructions count: 231
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.VivoKeyguardOverlayController.getKOCInfoFromUCS(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.ArrayList):boolean");
    }

    /* JADX WARN: Code restructure failed: missing block: B:33:0x0092, code lost:
        if (0 != 0) goto L47;
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x00aa, code lost:
        if (com.android.server.policy.VivoKeyguardOverlayController.DBG == false) goto L46;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x00ac, code lost:
        vivo.util.VSlog.d(com.android.server.policy.VivoKeyguardOverlayController.TAG, "parseAndAddContent end " + r9);
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x00c0, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:65:?, code lost:
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void parseAndAddContent(java.io.StringReader r8, java.util.ArrayList<java.lang.String> r9) {
        /*
            r7 = this;
            boolean r0 = com.android.server.policy.VivoKeyguardOverlayController.DBG
            java.lang.String r1 = "VivoKeyguardOverlayController"
            if (r0 == 0) goto Lb
            java.lang.String r0 = "parseAndAddContent start"
            vivo.util.VSlog.d(r1, r0)
        Lb:
            if (r9 != 0) goto L14
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r9 = r0
            goto L17
        L14:
            r9.clear()
        L17:
            r0 = 0
            java.io.BufferedReader r2 = new java.io.BufferedReader     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r2.<init>(r8)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r0 = r2
            java.lang.String r2 = r0.readLine()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.lang.String r3 = ""
        L24:
            if (r2 == 0) goto L8e
            r3 = r2
            boolean r4 = com.android.server.policy.VivoKeyguardOverlayController.DBG     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r4 == 0) goto L3f
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r4.<init>()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.lang.String r5 = "parseAndAddContent line="
            r4.append(r5)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r4.append(r3)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            vivo.util.VSlog.d(r1, r4)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
        L3f:
            if (r3 == 0) goto L88
            java.lang.String r4 = "disableKOC"
            boolean r4 = r4.equals(r3)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.lang.String r5 = "enable_koc_feature"
            if (r4 == 0) goto L61
            r4 = 0
            r7.mEnabled = r4     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            android.content.Context r6 = r7.mContext     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            android.content.ContentResolver r6 = r6.getContentResolver()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            android.provider.Settings.System.putInt(r6, r5, r4)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            boolean r4 = com.android.server.policy.VivoKeyguardOverlayController.DBG     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r4 == 0) goto L88
            java.lang.String r4 = "disable KOC!"
            vivo.util.VSlog.d(r1, r4)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            goto L88
        L61:
            java.lang.String r4 = "enableKOC"
            boolean r4 = r4.equals(r3)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r4 == 0) goto L7f
            r4 = 1
            r7.mEnabled = r4     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            android.content.Context r6 = r7.mContext     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            android.content.ContentResolver r6 = r6.getContentResolver()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            android.provider.Settings.System.putInt(r6, r5, r4)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            boolean r4 = com.android.server.policy.VivoKeyguardOverlayController.DBG     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r4 == 0) goto L88
            java.lang.String r4 = "enable KOC!"
            vivo.util.VSlog.d(r1, r4)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            goto L88
        L7f:
            boolean r4 = r9.contains(r3)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r4 != 0) goto L88
            r9.add(r3)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
        L88:
            java.lang.String r4 = r0.readLine()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r2 = r4
            goto L24
        L8e:
            r0.close()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r0 = 0
            if (r0 == 0) goto La8
            goto La7
        L95:
            r1 = move-exception
            goto L9e
        L97:
            r2 = move-exception
            if (r0 == 0) goto La4
            r0.close()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> La2
            goto La4
        L9e:
            if (r0 == 0) goto La1
            r0 = 0
        La1:
            throw r1
        La2:
            r3 = move-exception
            goto La5
        La4:
        La5:
            if (r0 == 0) goto La8
        La7:
            r0 = 0
        La8:
            boolean r2 = com.android.server.policy.VivoKeyguardOverlayController.DBG
            if (r2 == 0) goto Lc0
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "parseAndAddContent end "
            r2.append(r3)
            r2.append(r9)
            java.lang.String r2 = r2.toString()
            vivo.util.VSlog.d(r1, r2)
        Lc0:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.VivoKeyguardOverlayController.parseAndAddContent(java.io.StringReader, java.util.ArrayList):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bringupPMSerivce(Message msg) {
        Intent intent;
        if (sPermissionManagerExist) {
            intent = new Intent("com.vivo.permissionmanger.ACTION_CONTROLED_NOTIFY");
            intent.setPackage("com.vivo.permissionmanager");
        } else {
            intent = new Intent("com.vivo.permissionmanger.ACTION_CONTROLED_NOTIFY");
            intent.setPackage(VivoAppIsolationController.NOTIFY_IQOO_SECURE_PACKAGE);
        }
        intent.putExtras(msg.getData());
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            VSlog.d(TAG, "bringupPMSerivce FAILED!!!! msg=" + msg);
        }
    }

    void sendKOCInfo(String packageName, String componentName, boolean result, int callerUid, String reason) {
        Bundle bundle = new Bundle();
        bundle.putString("pkgName", packageName);
        bundle.putString("componentName", componentName);
        bundle.putString("interceptState", result ? "false" : "true");
        bundle.putString("interceptReason", reason);
        bundle.putLong("time", System.currentTimeMillis());
        bundle.putInt("calleruid", callerUid);
        if (DBG) {
            VSlog.d(TAG, "sendInterceptInfo: " + packageName + "(callerUid:" + callerUid + ") bring up " + componentName + " is allowed? " + result + " Reason=" + reason);
        }
        final Message msg = new Message();
        msg.setData(bundle);
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.policy.VivoKeyguardOverlayController.1
            @Override // java.lang.Runnable
            public void run() {
                if (VivoKeyguardOverlayController.DBG) {
                    VSlog.d(VivoKeyguardOverlayController.TAG, "bringupPMSerivce! msg=" + msg);
                }
                VivoKeyguardOverlayController.this.bringupPMSerivce(msg);
            }
        }, 50L);
    }

    void startObserver() {
        if (DBG) {
            VSlog.d(TAG, "startObserver");
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Uri.parse(PM_KOC_URI), true, this.mContentObserver, -2);
    }

    void stopObserver() {
        if (DBG) {
            VSlog.d(TAG, "stopObserver");
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.unregisterContentObserver(this.mContentObserver);
    }

    boolean checkKeyguardOverlayWindow(WindowManagerPolicy.WindowState win) {
        if (DBG) {
            VSlog.d(TAG, "checkKeyguardOverlayWindow now coming window=" + win + " mLastOverlayWindow=" + this.mLastOverlayWindow + "mNeedForbid=" + this.mNeedForbid);
        }
        if (this.mLastOverlayWindow == win) {
            if (DBG) {
                VSlog.d(TAG, "updateKeyguardOverlayWindow same TopWindow,not Notify ,forbid=" + this.mNeedForbid);
            }
        } else {
            if (DBG) {
                VSlog.d(TAG, "updateKeyguardOverlayWindow try to checkout allowOverlayKeyguard,and we need notify anyway");
            }
            this.mLastOverlayWindow = win;
            String packageName = win.getOwningPackage();
            String componentName = win.getAttrs().getTitle().toString();
            int callerUid = win.getOwningUid();
            this.mNeedForbid = !allowOverlayKeyguard(packageName, componentName, callerUid);
        }
        return this.mNeedForbid;
    }

    public boolean allowOverlayKeyguard(String packageName) {
        Integer state;
        if (this.mEnabled && this.mPkgWhitelist != null) {
            String dialer = DefaultDialerManager.getDefaultDialerApplication(this.mContext);
            return (dialer != null && dialer.equals(packageName)) || (state = this.mPkgWhitelist.get(packageName)) == null || state.intValue() == 0;
        }
        return true;
    }

    public boolean allowOverlayKeyguard(String packageName, String componentName, int callerUid) {
        String reason;
        boolean res;
        String reason2;
        boolean res2;
        if (DBG) {
            VSlog.d(TAG, "allowOverlayKeyguard  for packageName=" + packageName + " mPkgWhitelist=" + this.mPkgWhitelist + " mKOCForbidComponents=" + this.mKOCForbidComponents);
        }
        if (packageName == null || packageName.length() == 0) {
            return true;
        }
        if (!this.mEnabled) {
            if (DBG) {
                VSlog.d(TAG, "1res mEnabled =" + this.mEnabled);
            }
            reason2 = REASON_KOC_DISABLED;
            res2 = true;
        } else {
            HashMap<String, Integer> hashMap = this.mPkgWhitelist;
            if (hashMap != null) {
                Integer state = hashMap.get(packageName);
                if (DBG) {
                    VSlog.d(TAG, "state for " + packageName + " is " + state);
                }
                if (state == null) {
                    if (DBG) {
                        VSlog.d(TAG, "2res state =" + state);
                    }
                    reason = REASON_DEFAULT;
                    res = true;
                } else if (state.intValue() == 0) {
                    if (DBG) {
                        VSlog.d(TAG, "3res state =" + state);
                    }
                    reason = REASON_ALLOWED_IN_PERMISSIONLIST;
                    res = true;
                } else {
                    if (DBG) {
                        VSlog.d(TAG, "4res state =" + state);
                    }
                    reason = REASON_FORBID_IN_PERMISSIONLIST;
                    res = false;
                }
            } else {
                if (DBG) {
                    VSlog.d(TAG, "ERROR!!mPkgWhitelist NOT INITED");
                }
                reason = REASON_DEFAULT;
                res = true;
            }
            if (res && this.mKOCForbidComponents != null) {
                if (DBG) {
                    VSlog.d(TAG, "5res mKOCForbidComponents =" + this.mKOCForbidComponents);
                }
                reason2 = reason;
                for (int j = this.mKOCForbidComponents.size() - 1; j >= 0; j--) {
                    String temp = this.mKOCForbidComponents.get(j);
                    if (componentName.endsWith(temp) || componentName.equals(temp)) {
                        reason2 = REASON_IN_BLACKLIST;
                        res = false;
                    }
                }
                res2 = res;
            } else {
                reason2 = reason;
                res2 = res;
            }
        }
        sendKOCInfo(packageName, componentName, res2, callerUid, reason2);
        return res2;
    }

    /* loaded from: classes.dex */
    class DBObserver extends ContentObserver {
        public DBObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (VivoKeyguardOverlayController.DBG) {
                VSlog.d(VivoKeyguardOverlayController.TAG, "PMDBListener onChange uri=" + uri);
            }
            VivoKeyguardOverlayController.this.mHandler.removeCallbacks(VivoKeyguardOverlayController.this.readPMKOCRunnable);
            VivoKeyguardOverlayController.this.mHandler.postDelayed(VivoKeyguardOverlayController.this.readPMKOCRunnable, 500L);
        }
    }
}