package com.android.server.am;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.FtBuild;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.ArrayMap;
import com.android.server.UnifiedConfigThread;
import com.android.server.wm.VivoAppShareManager;
import com.vivo.face.common.data.Constants;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vgc.AbsVivoVgcManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoBroadcastQueueImpl implements IVivoBroadcastQueue {
    private static final String AUTHORITY = "com.vivo.permissionmanager.provider.permission";
    private static final Uri CONTENT_URI;
    private static final String PKGNAME = "pkgname";
    static final String TAG = "VivoBroadcastQueueImpl";
    private static final ArrayMap<String, ArrayList<String>> UNFILTER_ACTION_MAP;
    private static final boolean isVos;
    private static ArrayList<String> mForbidAppsName = new ArrayList<>();
    private static boolean mIsQuerying = false;
    private final String FORBID_BGAPPS_LIST_NAMES;
    private final String KEY_AUTOSTART_BLACKLIST;
    private final String QUERY_SELECTION;
    private final String dataBbkCoreDir;
    private final String dataDir;
    private final boolean isInFactoryMode;
    private BgAppForbidDBObserver mBgAppForbidDBObserver;
    List<String> mBlackListVgc;
    private BroadcastQueue mBroadcastQueue;
    public Context mContext;
    private FileReader mFileReader;
    private BufferedReader mFileReaderBuffer;
    private BufferedWriter mFileWriteBuffer;
    private FileWriter mFilewriter;
    private BroadcastProxyQueue mProxyQueue;
    ActivityManagerService mService;
    VivoAppShareManager mVivoAppShareManager;
    private VivoFrozenPackageSupervisor mVivoFrozenPackageSupervisor;
    AbsVivoVgcManager mVivoVgcManager;
    private ArrayList<String> savedWhiteListAppsName;
    private Pattern mVivoUnitTestPattern = Pattern.compile(".*android.*servicestests.*");
    final int MSG_QUERY_DB = 202;

    public VivoBroadcastQueueImpl(BroadcastQueue broadcastQueue, ActivityManagerService service) {
        this.mVivoAppShareManager = null;
        this.QUERY_SELECTION = isVos ? "currentstate=1" : "currentstate=0";
        this.dataDir = "/data/";
        this.dataBbkCoreDir = "/data/bbkcore/";
        this.FORBID_BGAPPS_LIST_NAMES = "forbidBgStartAppsName.txt";
        this.mVivoVgcManager = null;
        this.mBlackListVgc = new ArrayList();
        this.KEY_AUTOSTART_BLACKLIST = "autostart_blacklist";
        this.isInFactoryMode = "yes".equals(SystemProperties.get("persist.sys.factory.mode", "no"));
        if (broadcastQueue == null || service == null) {
            VSlog.i(TAG, "container is " + broadcastQueue);
        }
        this.mBroadcastQueue = broadcastQueue;
        this.mService = service;
        this.mProxyQueue = new BroadcastProxyQueue(broadcastQueue, this);
        this.mVivoFrozenPackageSupervisor = VivoFrozenPackageSupervisor.getInstance();
        this.mVivoAppShareManager = VivoAppShareManager.getInstance();
        if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
            AbsVivoVgcManager vivoVgcManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoVgcManager();
            this.mVivoVgcManager = vivoVgcManager;
            this.mBlackListVgc = vivoVgcManager.getStringList("autostart_blacklist", (List) null);
        }
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    static {
        ArrayMap<String, ArrayList<String>> arrayMap = new ArrayMap<>();
        UNFILTER_ACTION_MAP = arrayMap;
        arrayMap.put("android.appwidget.action.APPWIDGET_DISABLED", new ArrayList<>(Arrays.asList(VivoPermissionUtils.OS_PKG)));
        UNFILTER_ACTION_MAP.put("android.appwidget.action.APPWIDGET_UPDATE_OPTIONS", new ArrayList<>(Arrays.asList(VivoPermissionUtils.OS_PKG)));
        UNFILTER_ACTION_MAP.put("android.appwidget.action.APPWIDGET_UPDATE", new ArrayList<>(Arrays.asList(VivoPermissionUtils.OS_PKG)));
        UNFILTER_ACTION_MAP.put("android.appwidget.action.APPWIDGET_ENABLED", new ArrayList<>(Arrays.asList(VivoPermissionUtils.OS_PKG)));
        UNFILTER_ACTION_MAP.put("android.appwidget.action.APPWIDGET_DELETED", new ArrayList<>(Arrays.asList(VivoPermissionUtils.OS_PKG)));
        UNFILTER_ACTION_MAP.put("android.appwidget.action.APPWIDGET_CONFIGURE", new ArrayList<>(Arrays.asList(VivoPermissionUtils.OS_PKG)));
        UNFILTER_ACTION_MAP.put("android.appwidget.action.APPWIDGET_BIND", new ArrayList<>(Arrays.asList(VivoPermissionUtils.OS_PKG)));
        UNFILTER_ACTION_MAP.put("android.appwidget.action.APPWIDGET_PICK", new ArrayList<>(Arrays.asList(VivoPermissionUtils.OS_PKG)));
        UNFILTER_ACTION_MAP.put("android.intent.action.HEADSET_PLUG", new ArrayList<>(Arrays.asList(VivoPermissionUtils.OS_PKG)));
        UNFILTER_ACTION_MAP.put("android.intent.action.ANALOG_AUDIO_DOCK_PLUG", new ArrayList<>(Arrays.asList(VivoPermissionUtils.OS_PKG)));
        UNFILTER_ACTION_MAP.put("android.intent.action.MEDIA_BUTTON", new ArrayList<>(Arrays.asList("unknown")));
        UNFILTER_ACTION_MAP.put("vivo.intent.action.PRELOAD_VIVOCAM", new ArrayList<>(Arrays.asList("com.vivo.abe", "com.vivo.sps")));
        if ("TW".equals(SystemProperties.get("ro.product.customize.bbk", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK))) {
            UNFILTER_ACTION_MAP.put("com.google.android.c2dm.intent.RECEIVE", new ArrayList<>(Arrays.asList("com.google.android.gms")));
            UNFILTER_ACTION_MAP.put("com.google.firebase.MESSAGING_EVENT", new ArrayList<>(Arrays.asList("unknown")));
        }
        CONTENT_URI = Uri.parse("content://com.vivo.permissionmanager.provider.permission/bg_start_up_apps");
        isVos = FtBuild.getTierLevel() != 0;
    }

    public void startObserveForbidDB() {
        if (this.mBgAppForbidDBObserver == null) {
            BgAppForbidDBObserver bgAppForbidDBObserver = new BgAppForbidDBObserver(UnifiedConfigThread.getHandler());
            this.mBgAppForbidDBObserver = bgAppForbidDBObserver;
            bgAppForbidDBObserver.observe();
        }
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            VSlog.v(TAG, " calling getForbidBgStartAppsName method ! ");
        }
        mForbidAppsName = getForbidBgStartAppsName("forbidBgStartAppsName.txt");
    }

    private boolean isFileExist(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            return true;
        }
        return false;
    }

    public void systemReady() {
        if (!isFileExist("/data/bbkcore/forbidBgStartAppsName.txt")) {
            UnifiedConfigThread.getHandler().post(new Runnable() { // from class: com.android.server.am.-$$Lambda$VivoBroadcastQueueImpl$Rg-bWn29sMY7xMRHaCEt3AtNx60
                @Override // java.lang.Runnable
                public final void run() {
                    VivoBroadcastQueueImpl.this.lambda$systemReady$0$VivoBroadcastQueueImpl();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x0045, code lost:
        if (r1 != null) goto L20;
     */
    /* JADX WARN: Code restructure failed: missing block: B:16:0x0047, code lost:
        r1.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x0057, code lost:
        if (r1 == null) goto L21;
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x005a, code lost:
        r0 = com.android.server.am.VivoBroadcastQueueImpl.mForbidAppsName;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x005e, code lost:
        if (r0 == null) goto L24;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x0060, code lost:
        saveForbidBgStartAppsName(r0, "forbidBgStartAppsName.txt");
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x0063, code lost:
        r0 = com.android.server.am.VivoBroadcastQueueImpl.mForbidAppsName;
     */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x0065, code lost:
        if (r0 == null) goto L30;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x006b, code lost:
        if (r0.size() != 0) goto L28;
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x006d, code lost:
        com.android.server.am.VivoBroadcastQueueImpl.mForbidAppsName = getForbidBgStartAppsName("forbidBgStartAppsName.txt");
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x0073, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:?, code lost:
        return;
     */
    /* JADX WARN: Removed duplicated region for block: B:37:0x0077  */
    /* renamed from: queryFromForbidBgStartUpList */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void lambda$systemReady$0$VivoBroadcastQueueImpl() {
        /*
            r9 = this;
            r0 = 0
            r1 = 0
            com.android.server.am.BroadcastQueue r2 = r9.mBroadcastQueue     // Catch: java.lang.Throwable -> L4d java.lang.Exception -> L51
            com.android.server.am.ActivityManagerService r2 = r2.mService     // Catch: java.lang.Throwable -> L4d java.lang.Exception -> L51
            android.content.Context r2 = r2.mContext     // Catch: java.lang.Throwable -> L4d java.lang.Exception -> L51
            android.content.ContentResolver r3 = r2.getContentResolver()     // Catch: java.lang.Throwable -> L4d java.lang.Exception -> L51
            android.net.Uri r4 = com.android.server.am.VivoBroadcastQueueImpl.CONTENT_URI     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            java.lang.String r0 = "pkgname"
            java.lang.String[] r5 = new java.lang.String[]{r0}     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            java.lang.String r6 = r9.QUERY_SELECTION     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            r7 = 0
            r8 = 0
            android.database.Cursor r0 = r3.query(r4, r5, r6, r7, r8)     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            r1 = r0
            java.util.ArrayList<java.lang.String> r0 = com.android.server.am.VivoBroadcastQueueImpl.mForbidAppsName     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            if (r0 == 0) goto L26
            java.util.ArrayList<java.lang.String> r0 = com.android.server.am.VivoBroadcastQueueImpl.mForbidAppsName     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            r0.clear()     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
        L26:
            if (r1 == 0) goto L45
        L28:
            boolean r0 = r1.moveToNext()     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            if (r0 == 0) goto L45
            r0 = 0
            java.lang.String r0 = r1.getString(r0)     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            java.util.ArrayList<java.lang.String> r2 = com.android.server.am.VivoBroadcastQueueImpl.mForbidAppsName     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            if (r2 != 0) goto L3e
            java.util.ArrayList r2 = new java.util.ArrayList     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            r2.<init>()     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            com.android.server.am.VivoBroadcastQueueImpl.mForbidAppsName = r2     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
        L3e:
            java.util.ArrayList<java.lang.String> r2 = com.android.server.am.VivoBroadcastQueueImpl.mForbidAppsName     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            r2.add(r0)     // Catch: java.lang.Exception -> L4b java.lang.Throwable -> L74
            goto L28
        L45:
            if (r1 == 0) goto L5a
        L47:
            r1.close()
            goto L5a
        L4b:
            r0 = move-exception
            goto L54
        L4d:
            r2 = move-exception
            r3 = r0
            r0 = r2
            goto L75
        L51:
            r2 = move-exception
            r3 = r0
            r0 = r2
        L54:
            r0.printStackTrace()     // Catch: java.lang.Throwable -> L74
            if (r1 == 0) goto L5a
            goto L47
        L5a:
            java.util.ArrayList<java.lang.String> r0 = com.android.server.am.VivoBroadcastQueueImpl.mForbidAppsName
            java.lang.String r2 = "forbidBgStartAppsName.txt"
            if (r0 == 0) goto L63
            r9.saveForbidBgStartAppsName(r0, r2)
        L63:
            java.util.ArrayList<java.lang.String> r0 = com.android.server.am.VivoBroadcastQueueImpl.mForbidAppsName
            if (r0 == 0) goto L6d
            int r0 = r0.size()
            if (r0 != 0) goto L73
        L6d:
            java.util.ArrayList r0 = r9.getForbidBgStartAppsName(r2)
            com.android.server.am.VivoBroadcastQueueImpl.mForbidAppsName = r0
        L73:
            return
        L74:
            r0 = move-exception
        L75:
            if (r1 == 0) goto L7a
            r1.close()
        L7a:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.VivoBroadcastQueueImpl.lambda$systemReady$0$VivoBroadcastQueueImpl():void");
    }

    /* loaded from: classes.dex */
    class BgAppForbidDBObserver extends ContentObserver {
        public BgAppForbidDBObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            VSlog.i(VivoBroadcastQueueImpl.TAG, "calling observe method ! ");
            ContentResolver resolver = VivoBroadcastQueueImpl.this.mBroadcastQueue.mService.mContext.getContentResolver();
            resolver.registerContentObserver(VivoBroadcastQueueImpl.CONTENT_URI, false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VSlog.i(VivoBroadcastQueueImpl.TAG, "calling onChange method !");
            VivoBroadcastQueueImpl.this.lambda$systemReady$0$VivoBroadcastQueueImpl();
        }
    }

    public void saveForbidBgStartAppsName(ArrayList<String> whiteList, String fileName) {
        try {
            File file = new File("/data/bbkcore/" + fileName);
            if (file.exists() && file.isFile()) {
                file.delete();
            }
            if (whiteList == null || whiteList.size() == 0) {
                return;
            }
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.mFilewriter = new FileWriter(file);
            this.mFileWriteBuffer = new BufferedWriter(this.mFilewriter);
            Iterator<String> it = whiteList.iterator();
            while (it.hasNext()) {
                String appName = it.next();
                this.mFileWriteBuffer.write(appName);
                this.mFileWriteBuffer.newLine();
            }
            this.mFileWriteBuffer.close();
            this.mFilewriter.close();
        } catch (Exception e2) {
            VSlog.i(TAG, "exception is : " + e2.getMessage());
        }
    }

    public ArrayList<String> getForbidBgStartAppsName(String fileName) {
        this.savedWhiteListAppsName = new ArrayList<>();
        try {
            File file = new File("/data/bbkcore/" + fileName);
            if (!file.exists()) {
                return this.savedWhiteListAppsName;
            }
            this.mFileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(this.mFileReader);
            this.mFileReaderBuffer = bufferedReader;
            String appName = bufferedReader.readLine();
            while (appName != null) {
                this.savedWhiteListAppsName.add(appName);
                appName = this.mFileReaderBuffer.readLine();
            }
            this.mFileReaderBuffer.close();
            this.mFileReader.close();
            return this.savedWhiteListAppsName;
        } catch (Exception e) {
            VSlog.i(TAG, "exception is : " + e.getMessage() + " ! ! ");
            return this.savedWhiteListAppsName;
        }
    }

    public boolean filterBroadcastIfNeed(BroadcastRecord r, ResolveInfo info) {
        if (!isWhiteList(info.activityInfo.packageName) && SystemProperties.get("ro.cmcc.test", "no").equals("no")) {
            boolean toBeFiltered = false;
            ArrayList<String> arrayList = mForbidAppsName;
            boolean isInAppList = arrayList != null && arrayList.contains(info.activityInfo.applicationInfo.packageName);
            if ((isVos && isInAppList) || (!isVos && !isInAppList)) {
                String packageName = info.activityInfo.applicationInfo.packageName;
                if (packageName != null && (packageName.startsWith(VivoPermissionUtils.OS_PKG) || packageName.startsWith("com.android"))) {
                    return false;
                }
                toBeFiltered = true;
                if ((info.activityInfo.applicationInfo.flags & 1) != 0) {
                    toBeFiltered = false;
                }
                String action = r.intent.getAction();
                ArrayList<String> senderPkgList = UNFILTER_ACTION_MAP.get(action);
                int receiverUserId = UserHandle.getUserId(info.activityInfo.applicationInfo.uid);
                int currentUserId = this.mService.mUserController.getCurrentUserId();
                if (receiverUserId != currentUserId && receiverUserId != 999) {
                    VSlog.i(TAG, "toBefiltered is false, sender or receiver is not the same user");
                    toBeFiltered = false;
                } else if (senderPkgList != null && (senderPkgList.contains(r.callerPackage) || senderPkgList.contains("unknown"))) {
                    VSlog.i(TAG, "toBefiltered is false, action = " + action);
                    toBeFiltered = false;
                } else if (r.callingUid == info.activityInfo.applicationInfo.uid) {
                    VSlog.i(TAG, "toBefiltered is false, app.uid equals r.callingUid is " + r.callingUid);
                    toBeFiltered = false;
                    if (r.callerApp == null && (1 & info.activityInfo.applicationInfo.flags) == 0 && this.mService.mProcessList.getUidProcStateLocked(r.callingUid) == 20) {
                        VSlog.i(TAG, "==/==> B=1, app.processName " + info.activityInfo.applicationInfo.processName);
                        toBeFiltered = true;
                    }
                    if (toBeFiltered) {
                        try {
                            if (r.intent.getIsVivoWidget()) {
                                toBeFiltered = false;
                                VSlog.w(TAG, "ignore widget action");
                            }
                        } catch (Exception e) {
                            toBeFiltered = false;
                            VSlog.w(TAG, "Exception!");
                        }
                    }
                } else {
                    Intent intent = new Intent("android.intent.action.SENDTO");
                    intent.setData(Uri.parse("mmsto:"));
                    ResolveInfo ri = this.mBroadcastQueue.mService.mContext.getPackageManager().resolveActivity(intent, 0);
                    if (ri != null && ri.activityInfo.packageName.equals(info.activityInfo.applicationInfo.packageName)) {
                        VSlog.i(TAG, "Do not filtered : " + ri.activityInfo.packageName + " is a default Message app.");
                        toBeFiltered = false;
                    }
                }
            }
            try {
                PackageInfo pi = this.mBroadcastQueue.mService.mContext.getPackageManager().getPackageInfo(info.activityInfo.applicationInfo.packageName, 0);
                if (pi != null && !info.activityInfo.applicationInfo.sourceDir.equals(pi.applicationInfo.sourceDir)) {
                    VSlog.d(TAG, "exception! origin dir :" + info.activityInfo.applicationInfo.sourceDir + " change to " + pi.applicationInfo.sourceDir);
                    toBeFiltered = true;
                }
            } catch (Exception e2) {
            }
            if (this.mBroadcastQueue.mService.getPackageManagerInternalLocked().isPackageDataProtected(r.userId, info.activityInfo.applicationInfo.packageName)) {
                toBeFiltered = false;
            }
            if ("com.google.android.c2dm.intent.RECEIVE".equals(r.intent.getAction()) && "1".equals(SystemProperties.get("persist.vivo.custom.phonelock"))) {
                toBeFiltered = true;
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    VSlog.d(TAG, "filter google c2dm broadcast when phonelock");
                }
            }
            if (this.isInFactoryMode && this.mBlackListVgc != null) {
                String pkg = info.activityInfo.applicationInfo.packageName;
                if (this.mBlackListVgc.contains(info.activityInfo.applicationInfo.packageName)) {
                    VSlog.i(TAG, "==/==> B=2, package name is " + pkg);
                    toBeFiltered = true;
                }
            }
            if (toBeFiltered) {
                VSlog.w(TAG, "==/==> " + info.activityInfo.applicationInfo.packageName + "/" + info.activityInfo.applicationInfo.uid + " for broadcast " + r.intent + ": XXXX");
                this.mBroadcastQueue.logBroadcastReceiverDiscardLocked(r);
            }
            return toBeFiltered;
        }
        return false;
    }

    private boolean isWhiteList(String packageName) {
        try {
            if (this.mVivoUnitTestPattern.matcher(packageName).matches()) {
                return this.mService.getPackageManager().checkSignatures(packageName, VivoPermissionUtils.OS_PKG) == 0;
            }
            return false;
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote exception", e);
            return false;
        }
    }

    public boolean enqueuePendingBroadcastLocked(BroadcastRecord r, Object target, int recIdx, boolean ordered) {
        if (this.mProxyQueue.enqueuePendingBroadcastLocked(r, target, ordered)) {
            r.state = 0;
            return true;
        }
        return false;
    }

    public void onStartProcessLocked(BroadcastRecord r, Object target, String targetProcess) {
        this.mProxyQueue.onStartProcessLocked(r, target, targetProcess);
    }

    public void cleanupDisabledPackageReceiversLocked(String packageName, Set<String> filterByClasses, int userId, boolean doit) {
        this.mProxyQueue.cleanupDisabledPackageReceiversLocked(packageName, filterByClasses, userId, doit);
    }

    public boolean isKeepFrozenBroadcastPrcocess(BroadcastRecord br, ProcessRecord app, boolean forceUnfreeze, String reason) {
        return this.mVivoFrozenPackageSupervisor.isKeepFrozenBroadcastPrcocess(br, app, forceUnfreeze, reason);
    }

    public boolean isKeepScreenOffPrcocessAppShare(String action, String packageName) {
        return this.mBroadcastQueue.mService.isAppSharing() && this.mVivoAppShareManager.isKeepScreenOffPrcocessAppShare(action, packageName);
    }
}