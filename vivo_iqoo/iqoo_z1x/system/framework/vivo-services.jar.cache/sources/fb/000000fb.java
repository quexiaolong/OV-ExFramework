package com.android.server.am.firewall;

import android.app.AppGlobals;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.internal.telecom.ITelecomService;
import com.android.internal.telephony.VivoTelephonyApiParams;
import com.android.server.UnifiedConfigThread;
import com.android.server.wm.VivoAppShareManager;
import com.vivo.appshare.AppShareConfig;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.superresolution.Constant;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoBackgroundActivityController {
    static final String ACTION_PM_UPDATE_WHITELIST = "com.vivo.permissionmanger.BG_ACTIVITY_LOAD_DB";
    static final String ACTION_UCS_UPDATE_BLACK_COMPONENT = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_BackgroundActivityBlack";
    static final String ACTION_UCS_UPDATE_WHITE_COMPONENT = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_BackgroundActivityWhite";
    private static final Uri BACKGROUND_ACTIVITY_COMPONENT_LIST_URI;
    private static final Uri BACKGROUND_ACTIVITY_WHITE_LIST_URI;
    private static final String BG_ACTIVITY_FEATURE_ENABLE = "enable_bg_activity_feature";
    private static final String[] BLACK_COMPONENT_LIST_SELECTION;
    private static final boolean DBG;
    public static final boolean IS_ENG;
    public static final boolean IS_LOG_CTRL_OPEN;
    private static final String KEYWORD_ACTIVITY_EVENT_TIMEOUT = "activity_event_timeout";
    private static final String KEYWORD_DISABLE_BG_ACTIVITY = "disable_bg_activity_v2";
    private static final String KEYWORD_ENABLE_BG_ACTIVITY = "enable_bg_activity";
    private static final String KEYWORD_EVENT_TIMEOUT = "event_timeout";
    public static final String KEY_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    private static final String REASON_ALLOWED_IN_PERMISSIONLIST = "allowedInlist";
    private static final String REASON_BG_ACTIVITY_DISABLED = "bgActivityDisabled";
    private static final String REASON_DEFAULT = "notInAnyList";
    private static final String REASON_FORBID_IN_PERMISSIONLIST = "fobidInlist";
    private static final String REASON_HAS_EVENT_RECORD = "hasEventRecord";
    private static final String REASON_IN_BLACKLIST = "forbidInBlackCompentlist";
    private static final String REASON_IN_FG_BLACKLIST = "forbidInFgBlackCompentlist";
    private static final String REASON_IN_WHITELIST = "allowInWhiteCompentlist";
    private static final String TAG = "VivoBackgroundActivityController";
    private static final String[] WHITE_COMPONENT_LIST_SELECTION;
    private static final String[] WHITE_LIST_SELECTION;
    private Context mContext;
    private ITelecomService mTelecomService;
    private VivoFirewall mVivoFirewall;
    private boolean mEnabled = true;
    private HashMap<String, Integer> mWhitePackageMap = new HashMap<>();
    private ArrayList<String> mBlackComponentList = new ArrayList<>();
    private ArrayList<String> mWhiteComponentList = new ArrayList<>();
    private ArrayList<String> mFgBlackCompList = new ArrayList<>();
    private LinkedList<EventRecord> mRecentRecords = new LinkedList<>();
    private long mEventSaveTime = 8000;
    private long mActivityEventSaveTime = 5000;
    private Runnable mReadWhiteListRunnable = new Runnable() { // from class: com.android.server.am.firewall.VivoBackgroundActivityController.1
        @Override // java.lang.Runnable
        public void run() {
            VivoBackgroundActivityController.this.log("mReadWhiteListRunnable!");
            VivoBackgroundActivityController.this.getWhiteListFromPermissionManager();
        }
    };
    private Runnable mReadCompentListRunnable = new Runnable() { // from class: com.android.server.am.firewall.VivoBackgroundActivityController.2
        @Override // java.lang.Runnable
        public void run() {
            VivoBackgroundActivityController.this.log("mReadCompentListRunnable!");
            VivoBackgroundActivityController.this.mFgBlackCompList.clear();
            VivoBackgroundActivityController.this.getBlackListFromUCS(VivoBackgroundActivityController.BACKGROUND_ACTIVITY_COMPONENT_LIST_URI, VivoBackgroundActivityController.BLACK_COMPONENT_LIST_SELECTION, VivoBackgroundActivityController.this.mBlackComponentList);
            VivoBackgroundActivityController.this.getBlackListFromUCS(VivoBackgroundActivityController.BACKGROUND_ACTIVITY_COMPONENT_LIST_URI, VivoBackgroundActivityController.WHITE_COMPONENT_LIST_SELECTION, VivoBackgroundActivityController.this.mWhiteComponentList);
        }
    };
    BroadcastReceiver mBackgroundControllerReceiver = new BroadcastReceiver() { // from class: com.android.server.am.firewall.VivoBackgroundActivityController.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            VivoBackgroundActivityController vivoBackgroundActivityController = VivoBackgroundActivityController.this;
            vivoBackgroundActivityController.log("DEBUG_BG_ACTIVITY:PMDBListener onReceive intent=" + intent);
            if (VivoBackgroundActivityController.ACTION_UCS_UPDATE_BLACK_COMPONENT.equals(intent.getAction()) || VivoBackgroundActivityController.ACTION_UCS_UPDATE_WHITE_COMPONENT.equals(intent.getAction())) {
                VivoBackgroundActivityController.this.mHandler.removeCallbacks(VivoBackgroundActivityController.this.mReadCompentListRunnable);
                VivoBackgroundActivityController.this.mHandler.postDelayed(VivoBackgroundActivityController.this.mReadCompentListRunnable, 500L);
            } else if (VivoBackgroundActivityController.ACTION_PM_UPDATE_WHITELIST.equals(intent.getAction())) {
                VivoBackgroundActivityController.this.mHandler.removeCallbacks(VivoBackgroundActivityController.this.mReadWhiteListRunnable);
                VivoBackgroundActivityController.this.mHandler.removeCallbacks(VivoBackgroundActivityController.this.mReadCompentListRunnable);
                VivoBackgroundActivityController.this.mHandler.postDelayed(VivoBackgroundActivityController.this.mReadWhiteListRunnable, 500L);
                VivoBackgroundActivityController.this.mHandler.postDelayed(VivoBackgroundActivityController.this.mReadCompentListRunnable, 500L);
            }
        }
    };
    private Handler mHandler = UnifiedConfigThread.getHandler();
    private ContentObserver mContentObserver = new WhiteListObserver(this.mHandler);
    private VivoAppShareManager mVivoAppShareManager = VivoAppShareManager.getInstance();

    static {
        boolean z = false;
        IS_ENG = Build.TYPE.equals("branddebug") || Build.TYPE.equals("eng");
        boolean equals = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
        IS_LOG_CTRL_OPEN = equals;
        if (equals || IS_ENG) {
            z = true;
        }
        DBG = z;
        BLACK_COMPONENT_LIST_SELECTION = new String[]{"BackgroundActivityBlack", "1", "1.0"};
        WHITE_COMPONENT_LIST_SELECTION = new String[]{"BackgroundActivityWhite", "1", "1.0"};
        WHITE_LIST_SELECTION = new String[]{"_id", "pkgname", "currentstate"};
        BACKGROUND_ACTIVITY_WHITE_LIST_URI = Uri.parse("content://com.vivo.permissionmanager.provider.permission/start_bg_activity");
        BACKGROUND_ACTIVITY_COMPONENT_LIST_URI = Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs");
    }

    public VivoBackgroundActivityController(Context context, VivoFirewall vivoFirewall) {
        this.mContext = context;
        this.mVivoFirewall = vivoFirewall;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getWhiteListFromPermissionManager() {
        boolean moveToNext;
        log("getWhiteListFromPermissionManager START");
        HashMap<String, Integer> itemInfoMap = new HashMap<>();
        ContentResolver resolver = this.mContext.getContentResolver();
        try {
            Cursor cursor = resolver.query(BACKGROUND_ACTIVITY_WHITE_LIST_URI, WHITE_LIST_SELECTION, null, null, null);
            if (cursor != null) {
                boolean moveToNext2 = cursor.moveToFirst();
                if (moveToNext2) {
                    do {
                        String pkgName = cursor.getString(1);
                        int currentState = cursor.getInt(2);
                        itemInfoMap.put(pkgName, Integer.valueOf(currentState));
                        moveToNext = cursor.moveToNext();
                    } while (moveToNext);
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        itemInfoMap.put(Constant.APP_WEIXIN, 0);
        itemInfoMap.put("com.tencent.mobileqq", 0);
        log("getWhiteListFromPermissionManager END itemInfoMap=" + itemInfoMap);
        this.mWhitePackageMap = itemInfoMap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getBlackListFromUCS(Uri uri, String[] selectionArgs, ArrayList<String> dstList) {
        log("start getListFromUCS " + selectionArgs[0]);
        ContentResolver resolver = this.mContext.getContentResolver();
        try {
            Cursor cursor = resolver.query(uri, null, null, selectionArgs, null);
            if (cursor != null) {
                cursor.moveToFirst();
                if (cursor.getCount() > 0) {
                    while (!cursor.isAfterLast()) {
                        cursor.getString(cursor.getColumnIndex("id"));
                        cursor.getString(cursor.getColumnIndex("identifier"));
                        cursor.getString(cursor.getColumnIndex("fileversion"));
                        byte[] filecontent = cursor.getBlob(cursor.getColumnIndex("filecontent"));
                        String contents = new String(filecontent, "UTF-8");
                        StringReader reader = new StringReader(contents);
                        parseAndAddContent(reader, dstList);
                        cursor.moveToNext();
                    }
                } else {
                    log("NO DATA in DB!");
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            log("ERROR OPEN DB!!! e=" + e);
        }
    }

    private void parseAndAddContent(StringReader reader, ArrayList<String> dstList) {
        log("parseAndAddContent start");
        if (dstList == null) {
            dstList = new ArrayList<>();
        } else {
            dstList.clear();
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(reader);
            for (String decreptedLine = bufferedReader.readLine(); decreptedLine != null; decreptedLine = bufferedReader.readLine()) {
                log("parseAndAddContent line=" + decreptedLine);
                if (!TextUtils.isEmpty(decreptedLine)) {
                    if (KEYWORD_DISABLE_BG_ACTIVITY.equals(decreptedLine)) {
                        this.mEnabled = false;
                        Settings.System.putInt(this.mContext.getContentResolver(), BG_ACTIVITY_FEATURE_ENABLE, 0);
                        log("disable bg_activity!");
                    } else if (KEYWORD_ENABLE_BG_ACTIVITY.equals(decreptedLine)) {
                        this.mEnabled = true;
                        Settings.System.putInt(this.mContext.getContentResolver(), BG_ACTIVITY_FEATURE_ENABLE, 1);
                        log("enable bg_activity!");
                    } else if (decreptedLine.startsWith(KEYWORD_EVENT_TIMEOUT)) {
                        String[] arr = decreptedLine.split("=");
                        String timeout = arr[arr.length - 1].trim();
                        try {
                            this.mEventSaveTime = Integer.parseInt(timeout);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    } else if (decreptedLine.startsWith(KEYWORD_ACTIVITY_EVENT_TIMEOUT)) {
                        String[] arr2 = decreptedLine.split("=");
                        String timeout2 = arr2[arr2.length - 1].trim();
                        try {
                            this.mActivityEventSaveTime = Integer.parseInt(timeout2);
                        } catch (NumberFormatException e2) {
                            e2.printStackTrace();
                        }
                    } else if (decreptedLine.startsWith("fg#")) {
                        String fgBlackCmp = decreptedLine.substring(3);
                        if (!TextUtils.isEmpty(fgBlackCmp) && !this.mFgBlackCompList.contains(fgBlackCmp)) {
                            this.mFgBlackCompList.add(fgBlackCmp);
                        }
                    } else if (!dstList.contains(decreptedLine)) {
                        dstList.add(decreptedLine);
                    }
                }
            }
            bufferedReader.close();
        } catch (Exception e3) {
            log("parseAndAddContent error e=" + e3);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyPermissionManager(Message msg) {
        Intent intent = new Intent("com.vivo.permissionmanager.BG_ACTIVITY_NOTIFY");
        intent.setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.service.NotificationService");
        intent.putExtras(msg.getData());
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            log("notifyPermissionManager FAILED!!!! msg=" + msg);
        }
    }

    private void sendMessageToPermissionManager(String packageName, int callerPid, int callerUid, String componentName, boolean result, String reason) {
        if (!result && RmsInjectorImpl.getInstance().needKeepQuiet(packageName, UserHandle.getUserId(callerUid), callerUid, 32)) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("pkgName", packageName);
        bundle.putString("componentName", componentName);
        bundle.putString("interceptState", result ? "false" : "true");
        bundle.putString("interceptReason", reason);
        bundle.putLong("time", System.currentTimeMillis());
        log("sendInterceptInfo: " + packageName + " bring up=" + componentName + " is allowed=" + result + " Reason=" + reason);
        final Message msg = new Message();
        msg.setData(bundle);
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.firewall.VivoBackgroundActivityController.4
            @Override // java.lang.Runnable
            public void run() {
                VivoBackgroundActivityController.this.notifyPermissionManager(msg);
            }
        }, 50L);
    }

    public void startMonitor() {
        enableFeatureDefault();
        registerBroadcast();
        registerObserver();
    }

    private void registerBroadcast() {
        log("registerBroadcast");
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.firewall.VivoBackgroundActivityController.5
            @Override // java.lang.Runnable
            public void run() {
                IntentFilter backgroundActivityFilter = new IntentFilter();
                backgroundActivityFilter.addAction(VivoBackgroundActivityController.ACTION_UCS_UPDATE_BLACK_COMPONENT);
                backgroundActivityFilter.addAction(VivoBackgroundActivityController.ACTION_UCS_UPDATE_WHITE_COMPONENT);
                backgroundActivityFilter.addAction(VivoBackgroundActivityController.ACTION_PM_UPDATE_WHITELIST);
                VivoBackgroundActivityController.this.mContext.registerReceiver(VivoBackgroundActivityController.this.mBackgroundControllerReceiver, backgroundActivityFilter);
            }
        });
    }

    public boolean isControlerEnabled() {
        return this.mEnabled;
    }

    private void registerObserver() {
        log("registerObserver");
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.firewall.VivoBackgroundActivityController.6
            @Override // java.lang.Runnable
            public void run() {
                ContentResolver resolver = VivoBackgroundActivityController.this.mContext.getContentResolver();
                resolver.registerContentObserver(VivoBackgroundActivityController.BACKGROUND_ACTIVITY_WHITE_LIST_URI, true, VivoBackgroundActivityController.this.mContentObserver);
            }
        });
    }

    public boolean shouldPreventActivityStart(boolean callerVisible, String callerPackage, ComponentInfo bringupSide, String type, int callerPid, int callerUid) {
        if (!this.mEnabled) {
            log(" activity prevent disabled, allow to start");
            return false;
        }
        if (TextUtils.isEmpty(callerPackage)) {
            callerPackage = getNonSystemPackageName(callerUid);
            if (TextUtils.isEmpty(callerPackage)) {
                log(" callerPackage is null, callerUid:" + callerUid);
                return false;
            }
        }
        ComponentName cmpName = (bringupSide == null || bringupSide.packageName == null || bringupSide.name == null) ? null : bringupSide.getComponentName();
        int blockType = -1;
        if (!allowStart(callerPackage, callerPid, callerUid, cmpName)) {
            blockType = 1;
        } else if (this.mVivoFirewall.isScreenOff()) {
            if (!allowStartFromBackground(callerPackage, callerPid, callerUid, cmpName, this.mVivoFirewall.getTopAppComponentName())) {
                blockType = 2;
            }
        } else if (!callerVisible && !allowStartCheckBlackList(callerPackage, cmpName, callerPid, callerUid)) {
            blockType = 3;
        }
        if (blockType != -1) {
            log("activity start not allowed, callerPackage: " + callerPackage + ", cmpName: " + cmpName + ", blockType: " + blockType);
            return true;
        }
        return false;
    }

    public boolean shouldPreventBringUpBackgroundActivity(String callerPackage, ComponentInfo bringupSide, String type, int callerPid, int callerUid) {
        if (!isControlerEnabled()) {
            log(type + " type not enable,allow to bringup");
            return false;
        } else if (callerPackage == null) {
            return false;
        } else {
            if (isAppRunningTop(callerPackage) || this.mVivoFirewall.hasForegroundWindow(callerPackage) || isUidSystem(callerUid) || isPidSystem(callerPid)) {
                log(callerPackage + " is foreground or call by system_server");
                return false;
            } else if (allowStartFromBackground(callerPackage, callerPid, callerUid, bringupSide.getComponentName(), this.mVivoFirewall.getTopAppComponentName())) {
                log(callerPackage + " allow start activity from backgroud");
                return false;
            } else {
                return true;
            }
        }
    }

    public boolean allowStart(String callerPackage, int callerPid, int callerUid, ComponentName cmp) {
        if (cmp == null) {
            return true;
        }
        String cmpName = cmp.getPackageName() + "/" + cmp.getClassName();
        Iterator<String> it = this.mFgBlackCompList.iterator();
        while (it.hasNext()) {
            String fgBlackCmp = it.next();
            if (cmpName.endsWith(fgBlackCmp)) {
                log("start cmp in black list, cmp: " + cmpName + ", fgBlackList: " + this.mFgBlackCompList);
                sendMessageToPermissionManager(callerPackage, callerPid, callerUid, cmpName, false, REASON_IN_FG_BLACKLIST);
                return false;
            }
        }
        return true;
    }

    public boolean checkBgActivityInWhiteList(ComponentInfo bringupSide, String callingPackage, int callingUid) {
        String compName;
        if (bringupSide == null || TextUtils.isEmpty(callingPackage)) {
            return false;
        }
        if (!this.mEnabled) {
            log("bgactivity feature disabled!");
            return false;
        }
        Integer state = this.mWhitePackageMap.get(callingPackage);
        if (state != null && state.intValue() == 0) {
            log("caller pkg in bg white list " + callingPackage);
            return true;
        }
        ComponentName cmpName = bringupSide.getComponentName();
        if (cmpName == null) {
            compName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        } else {
            compName = cmpName.getPackageName() + "/" + cmpName.getClassName();
        }
        if (TextUtils.isEmpty(compName) || !this.mWhiteComponentList.contains(compName)) {
            return false;
        }
        log("bringup component in bg white list " + compName);
        return true;
    }

    public boolean allowStartCheckBlackList(String callerPackage, ComponentName startComp, int callerPid, int callerUid) {
        ArrayList<String> arrayList;
        if (startComp == null || (arrayList = this.mBlackComponentList) == null || arrayList.size() <= 0) {
            return true;
        }
        String startCompName = startComp.getPackageName() + "/" + startComp.getClassName();
        boolean allow = true;
        for (int index = this.mBlackComponentList.size() - 1; index >= 0; index--) {
            String temp = this.mBlackComponentList.get(index);
            if (startCompName.endsWith(temp) || startCompName.equals(temp)) {
                allow = false;
                break;
            }
        }
        if (!allow) {
            sendMessageToPermissionManager(callerPackage, callerPid, callerUid, startCompName, false, REASON_IN_BLACKLIST);
        }
        return allow;
    }

    public boolean allowStartFromBackground(String callerPackage, int callerPid, int callerUid, ComponentName bringUpCompenent, ComponentName topComponent) {
        String reason;
        boolean allow;
        boolean allow2;
        String reason2;
        boolean allow3;
        ArrayList<String> arrayList;
        ArrayList<String> arrayList2;
        if (bringUpCompenent == null) {
            return true;
        }
        if (AppShareConfig.SUPPROT_APPSHARE && this.mVivoAppShareManager.mAppSharePackageName != null && this.mVivoAppShareManager.mAppSharePackageName.equals(callerPackage) && this.mVivoAppShareManager.mAppShareUserId != -1 && this.mVivoAppShareManager.mAppShareUserId == UserHandle.getUserId(callerUid)) {
            VSlog.d(TAG, "allow appShareDisplay : " + callerPackage);
            return true;
        }
        String bringUpCompentName = bringUpCompenent.getPackageName() + "/" + bringUpCompenent.getClassName();
        if (!this.mEnabled) {
            log("1res mEnabled =" + this.mEnabled);
            reason2 = REASON_BG_ACTIVITY_DISABLED;
            allow3 = true;
        } else {
            HashMap<String, Integer> hashMap = this.mWhitePackageMap;
            if (hashMap != null) {
                Integer state = hashMap.get(callerPackage);
                log("state for " + callerPackage + " is " + state);
                if (state == null) {
                    reason = REASON_DEFAULT;
                    allow = true;
                } else if (state.intValue() == 0) {
                    reason = REASON_ALLOWED_IN_PERMISSIONLIST;
                    allow = true;
                } else {
                    reason = REASON_FORBID_IN_PERMISSIONLIST;
                    allow = false;
                }
                if (!allow && (arrayList2 = this.mWhiteComponentList) != null && arrayList2.size() != 0) {
                    if (topComponent != null) {
                        String str = topComponent.getPackageName() + "/" + topComponent.getClassName();
                    }
                    for (int j = this.mWhiteComponentList.size() - 1; j >= 0; j--) {
                        String temp = this.mWhiteComponentList.get(j);
                        if (bringUpCompentName.endsWith(temp) || bringUpCompentName.equals(temp)) {
                            reason = REASON_IN_WHITELIST;
                            allow = true;
                            break;
                        }
                    }
                }
            } else {
                log("ERROR!!mWhitePackageMap NOT INITED");
                reason = REASON_DEFAULT;
                allow = true;
            }
            if (!allow && isVivoInCall(callerPackage)) {
                log("caller app is dialer and in/out call");
                reason = "DialerInCall";
                allow2 = true;
            } else {
                allow2 = allow;
            }
            if (allow2 && (arrayList = this.mBlackComponentList) != null && arrayList.size() != 0) {
                for (int j2 = this.mBlackComponentList.size() - 1; j2 >= 0; j2--) {
                    String temp2 = this.mBlackComponentList.get(j2);
                    if (bringUpCompentName.endsWith(temp2) || bringUpCompentName.equals(temp2)) {
                        reason2 = REASON_IN_BLACKLIST;
                        allow3 = false;
                        break;
                    }
                }
            }
            reason2 = reason;
            allow3 = allow2;
        }
        sendMessageToPermissionManager(callerPackage, callerPid, callerUid, bringUpCompentName, allow3, reason2);
        return allow3;
    }

    /* loaded from: classes.dex */
    class WhiteListObserver extends ContentObserver {
        public WhiteListObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            VivoBackgroundActivityController vivoBackgroundActivityController = VivoBackgroundActivityController.this;
            vivoBackgroundActivityController.log("PMDBListener onChange uri=" + uri);
            VivoBackgroundActivityController.this.mHandler.removeCallbacks(VivoBackgroundActivityController.this.mReadWhiteListRunnable);
            VivoBackgroundActivityController.this.mHandler.postDelayed(VivoBackgroundActivityController.this.mReadWhiteListRunnable, 500L);
        }
    }

    private void enableFeatureDefault() {
        int enable = Settings.System.getInt(this.mContext.getContentResolver(), BG_ACTIVITY_FEATURE_ENABLE, -1);
        if (enable == -1) {
            this.mEnabled = true;
            Settings.System.putInt(this.mContext.getContentResolver(), BG_ACTIVITY_FEATURE_ENABLE, 1);
            log("enable featrue default");
        } else if (enable == 0) {
            this.mEnabled = false;
        }
    }

    private boolean isUidSystem(int uid) {
        int appid = UserHandle.getAppId(uid);
        return appid == 1000 || uid == 0;
    }

    private boolean isAppRunningTop(String pkgName) {
        ComponentName topcomp = this.mVivoFirewall.getTopAppComponentName();
        String topPkgName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (topcomp != null) {
            topPkgName = topcomp.getPackageName();
        }
        log("top component:" + topcomp + " pkgName:" + pkgName);
        return TextUtils.equals(pkgName, topPkgName);
    }

    private boolean isPidSystem(int pid) {
        return pid == Process.myPid();
    }

    private String getNonSystemPackageName(int uid) {
        ApplicationInfo apinfo;
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            String[] packageNames = pm.getPackagesForUid(uid);
            if (packageNames != null && (apinfo = pm.getApplicationInfo(packageNames[0], 0, 0)) != null && (apinfo.flags & KernelConfig.AP_TE) == 0) {
                log("getNonSystemPackageName=" + packageNames[0]);
                return packageNames[0];
            }
            log("getNonSystemPackageName null");
            return null;
        } catch (Exception e) {
            log("fail to get package");
            return null;
        }
    }

    public void noteImportantEvent(int eventType, String packageName) {
    }

    private boolean checkAllEvents(String packageName) {
        log("checkAllEvents " + packageName);
        verifyEventRecords();
        Iterator<EventRecord> it = this.mRecentRecords.iterator();
        while (it.hasNext()) {
            EventRecord record = it.next();
            if (record.packageName.equals(packageName) && (record.eventType == 1 || record.eventType == 2 || record.eventType == 3)) {
                return true;
            }
        }
        return false;
    }

    private void verifyEventRecords() {
        long nowTime = System.currentTimeMillis();
        synchronized (this.mRecentRecords) {
            Iterator<EventRecord> iterator = this.mRecentRecords.iterator();
            while (iterator.hasNext()) {
                EventRecord temp = iterator.next();
                if (nowTime < temp.time) {
                    iterator.remove();
                } else if (temp.eventType == 3) {
                    if (nowTime - temp.time > this.mActivityEventSaveTime) {
                        iterator.remove();
                    }
                } else if (nowTime - temp.time > this.mEventSaveTime) {
                    iterator.remove();
                }
            }
        }
    }

    private boolean isVivoInCall(String pkgName) {
        boolean isInCall = false;
        try {
            if (this.mTelecomService == null) {
                this.mTelecomService = ITelecomService.Stub.asInterface(ServiceManager.getService("telecom"));
            }
            VivoTelephonyApiParams params = new VivoTelephonyApiParams("API_TAG_isVivoInCall");
            params.put("app_packageName", pkgName);
            isInCall = this.mTelecomService.vivoTelephonyApi(params).getAsBoolean("isVivoInCall").booleanValue();
        } catch (Exception e) {
            log("isVivoInCall exception " + e);
        }
        log("isVivoInCall " + isInCall);
        return isInCall;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void log(String info) {
        VSlog.d(TAG, info);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class EventRecord {
        int eventType;
        String packageName;
        long time = System.currentTimeMillis();

        EventRecord(int type, String packageName) {
            this.eventType = type;
            this.packageName = packageName;
        }
    }
}