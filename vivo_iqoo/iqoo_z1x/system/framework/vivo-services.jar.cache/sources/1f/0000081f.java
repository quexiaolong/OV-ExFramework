package com.vivo.services.security.server;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IPermissionController;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.location.VivoLocationFeature;
import com.android.server.policy.InputExceptionReport;
import com.vivo.common.VivoCollectData;
import com.vivo.face.common.data.Constants;
import com.vivo.framework.security.VivoPermissionManager;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.security.client.IVivoPermissionCallback;
import com.vivo.services.security.client.VivoPermissionInfo;
import com.vivo.services.security.client.VivoPermissionType;
import com.vivo.services.superresolution.Constant;
import com.vivo.vcodetransbase.EventTransfer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import vivo.app.security.IVivoPermissionService;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoPermissionService extends IVivoPermissionService.Stub {
    private static final long CHECK_DELETE_TIME = 300000;
    private static final boolean DEBUG_CTS_TEST_23;
    public static final int MSG_CONFIG_LOADER = 3;
    public static final int MSG_DELETE_FILE_BROADCAST = 5;
    public static final int MSG_DELETE_MEDIA_SCAN_BROADCAST = 6;
    public static final int MSG_INSTALL_SOURCE_SETS = 4;
    public static final int PERM_FG_ACTIVITY_CHANGED = 2;
    public static final int PERM_PROCESS_DIED = 1;
    private static boolean PRINTF_DEBUG = false;
    private static final String PROP_SUPER_SAVER = "sys.super_power_save";
    private static final String TAG = "VPS";
    private static final int TIME_DELAY = 1000;
    private static final int VERSION_1 = 1;
    private static final int VERSION_2 = 2;
    private static ArraySet<String> installtrustSets;
    private static VivoPermHandler mVivoPermHandler;
    private static ArrayList<String> mWhitePkgs;
    private static long minSpace;
    private final int MAX_LOCATION_BINDER_CHECK_TIME;
    private final int SILL;
    ActivityManagerInternal amInternal;
    private boolean configRecycle;
    private int count;
    private HashSet<String> lastPath;
    private HashSet<String> lastPkg;
    private long lastTime;
    private int lastUid;
    private ArrayMap<String, ArrayMap<String, CheckDeleteState>> mCheckDeleteState;
    private HashMap<String, Integer> mCheckLocationBinderTimes;
    private WeakHashMap<String, Message> mIsPackageSyncsScheduled;
    private int mMediaStoreAuthorityAppId;
    PackageManagerInternal mPackageManagerInt;
    private final SparseArray<String> mPackageUid;
    private final SparseArray<String> mPkgLabelUid;
    private Handler mUiHandler;
    private HashMap<String, VivoDeleteDialog> mVDDMap;
    private VivoPermissionConfig mVPC;
    private HashMap<String, VivoPermissionDialog> mVPDMap;
    private HashMap<String, VivoPermissionDeniedDialogModeOne> mVPDMap1;
    private HashMap<String, VivoPermissionDeniedDialogModeTwo> mVPDMap2;
    private HashMap<String, VivoPermissionDeniedDialogModeThree> mVPDMap3;
    private VivoPermissionReceiver mVPR;
    public HandlerThread permThread;
    private int retryCount;
    private int showIMIEOneTipsLimit;
    private int showIMIETwoTipsLimit;
    private ArraySet<String> vivoImeiSets;
    private static final boolean DEBUG_VPS = SystemProperties.get("persist.sys.debug.vps", "yes").equals("yes");
    private static Context mContext = null;
    private static byte[] mVPSLock = new byte[0];
    private static final int DEBUG_CTS_TEST = SystemProperties.getInt("persist.debug.c_test", 0);

    static /* synthetic */ int access$008(VivoPermissionService x0) {
        int i = x0.retryCount;
        x0.retryCount = i + 1;
        return i;
    }

    static {
        DEBUG_CTS_TEST_23 = "1".equals(SystemProperties.get("ro.build.g_test", "0")) || "1".equals(SystemProperties.get("ro.build.aia", "0"));
        ArraySet<String> arraySet = new ArraySet<>();
        installtrustSets = arraySet;
        arraySet.add("com.bbk.appstore");
        installtrustSets.add("com.vivo.browser");
        installtrustSets.add("com.vivo.game");
        installtrustSets.add("com.vivo.easyshare");
        installtrustSets.add("com.vivo.sharezone");
        PRINTF_DEBUG = false;
        mWhitePkgs = new ArrayList<>();
        minSpace = 519168000L;
    }

    public VivoPermissionService() {
        this.mUiHandler = null;
        this.mVPC = null;
        this.mVPR = null;
        this.mVPDMap = null;
        this.mVDDMap = null;
        this.mVPDMap1 = null;
        this.mVPDMap2 = null;
        this.mVPDMap3 = null;
        this.mCheckLocationBinderTimes = null;
        this.MAX_LOCATION_BINDER_CHECK_TIME = 3;
        this.mCheckDeleteState = new ArrayMap<>();
        this.mPackageUid = new SparseArray<>();
        this.mPkgLabelUid = new SparseArray<>();
        this.vivoImeiSets = new ArraySet<>();
        this.retryCount = 0;
        this.showIMIEOneTipsLimit = 3;
        this.showIMIETwoTipsLimit = 1;
        this.lastPath = new HashSet<>();
        this.lastPkg = new HashSet<>();
        this.lastUid = 0;
        this.lastTime = 0L;
        this.count = 0;
        this.SILL = 200;
        this.configRecycle = true;
        this.amInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mIsPackageSyncsScheduled = new WeakHashMap<>();
        this.mMediaStoreAuthorityAppId = -1;
    }

    public VivoPermissionService(Context context, Handler uiHandler) {
        this.mUiHandler = null;
        this.mVPC = null;
        this.mVPR = null;
        this.mVPDMap = null;
        this.mVDDMap = null;
        this.mVPDMap1 = null;
        this.mVPDMap2 = null;
        this.mVPDMap3 = null;
        this.mCheckLocationBinderTimes = null;
        this.MAX_LOCATION_BINDER_CHECK_TIME = 3;
        this.mCheckDeleteState = new ArrayMap<>();
        this.mPackageUid = new SparseArray<>();
        this.mPkgLabelUid = new SparseArray<>();
        this.vivoImeiSets = new ArraySet<>();
        this.retryCount = 0;
        this.showIMIEOneTipsLimit = 3;
        this.showIMIETwoTipsLimit = 1;
        this.lastPath = new HashSet<>();
        this.lastPkg = new HashSet<>();
        this.lastUid = 0;
        this.lastTime = 0L;
        this.count = 0;
        this.SILL = 200;
        this.configRecycle = true;
        this.amInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mIsPackageSyncsScheduled = new WeakHashMap<>();
        this.mMediaStoreAuthorityAppId = -1;
        printfInfo("Start:VivoPermissionService");
        mContext = context;
        this.mUiHandler = uiHandler;
        this.mVPDMap = new HashMap<>();
        this.mVDDMap = new HashMap<>();
        this.mVPDMap1 = new HashMap<>();
        this.mVPDMap2 = new HashMap<>();
        this.mVPDMap3 = new HashMap<>();
        this.mCheckLocationBinderTimes = new HashMap<>();
        HandlerThread handlerThread = new HandlerThread("VivoPermission");
        this.permThread = handlerThread;
        handlerThread.start();
        mVivoPermHandler = new VivoPermHandler(this.permThread.getLooper());
        registerBroadcastReceiver();
        this.mVPC = new VivoPermissionConfig(this, context, mVivoPermHandler);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.vivo.daemonService.unifiedconfig.update_finish_broadcast_vivoimei");
        intentFilter.addAction("com.vivo.daemonService.unifiedconfig.update_finish_broadcast_VivoImeiApps");
        intentFilter.addAction("com.vivo.daemonService.unifiedconfig.update_finish_broadcast_appdeletewarn");
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        mContext.registerReceiver(new BroadcastReceiver() { // from class: com.vivo.services.security.server.VivoPermissionService.1
            /* JADX WARN: Code restructure failed: missing block: B:14:0x002c, code lost:
                if (r0.equals("android.intent.action.BOOT_COMPLETED") == false) goto L22;
             */
            @Override // android.content.BroadcastReceiver
            /*
                Code decompiled incorrectly, please refer to instructions dump.
                To view partially-correct code enable 'Show inconsistent code' option in preferences
            */
            public void onReceive(android.content.Context r7, android.content.Intent r8) {
                /*
                    r6 = this;
                    java.lang.String r0 = "unifiedconfig onReceive"
                    com.vivo.services.security.server.VivoPermissionService.printfInfo(r0)
                    com.vivo.services.security.server.VivoPermissionService r0 = com.vivo.services.security.server.VivoPermissionService.this
                    r1 = 0
                    com.vivo.services.security.server.VivoPermissionService.access$002(r0, r1)
                    if (r8 != 0) goto Le
                    return
                Le:
                    java.lang.String r0 = r8.getAction()
                    if (r0 != 0) goto L15
                    return
                L15:
                    r2 = -1
                    int r3 = r0.hashCode()
                    r4 = -1541635470(0xffffffffa41c8272, float:-3.39376E-17)
                    r5 = 1
                    if (r3 == r4) goto L2f
                    r4 = 798292259(0x2f94f923, float:2.7098065E-10)
                    if (r3 == r4) goto L26
                L25:
                    goto L39
                L26:
                    java.lang.String r3 = "android.intent.action.BOOT_COMPLETED"
                    boolean r3 = r0.equals(r3)
                    if (r3 == 0) goto L25
                    goto L3a
                L2f:
                    java.lang.String r1 = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_appdeletewarn"
                    boolean r1 = r0.equals(r1)
                    if (r1 == 0) goto L25
                    r1 = r5
                    goto L3a
                L39:
                    r1 = r2
                L3a:
                    java.lang.String r2 = "data/bbkcore/delete_white_pkgs.xml"
                    if (r1 == 0) goto L4c
                    if (r1 == r5) goto L41
                    goto L57
                L41:
                    com.vivo.services.security.server.VivoPermissionService r1 = com.vivo.services.security.server.VivoPermissionService.this
                    java.io.File r3 = new java.io.File
                    r3.<init>(r2)
                    com.vivo.services.security.server.VivoPermissionService.access$100(r1, r3)
                    goto L57
                L4c:
                    com.vivo.services.security.server.VivoPermissionService r1 = com.vivo.services.security.server.VivoPermissionService.this
                    java.io.File r3 = new java.io.File
                    r3.<init>(r2)
                    com.vivo.services.security.server.VivoPermissionService.access$100(r1, r3)
                L57:
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.security.server.VivoPermissionService.AnonymousClass1.onReceive(android.content.Context, android.content.Intent):void");
            }
        }, intentFilter);
        ServiceManager.addService(VivoLocationFeature.FEATURE_VIVO_PERMISSION, new PermissionController(this));
        printfInfo("Finish:VivoPermissionService");
    }

    /* loaded from: classes.dex */
    static class PermissionController extends IPermissionController.Stub {
        private VivoPermissionService mVPS;

        PermissionController(VivoPermissionService vps) {
            this.mVPS = null;
            this.mVPS = vps;
        }

        public boolean checkPermission(String permission, int pid, int uid) {
            VivoPermissionService.printfDebug("PermissionController-->checkPermission (" + permission + ")pid:" + pid + " uid:" + uid);
            return permission != null && this.mVPS.checkPermission(permission, pid, uid, null) == 1;
        }

        public String[] getPackagesForUid(int uid) {
            if (VivoPermissionService.mContext != null) {
                return VivoPermissionService.mContext.getPackageManager().getPackagesForUid(uid);
            }
            VivoPermissionService.printfDebug("PermissionController-->getPackagesForUid mContext is null");
            return null;
        }

        public boolean isRuntimePermission(String permission) {
            if (permission != null && VivoPermissionService.mContext != null) {
                try {
                    PermissionInfo info = VivoPermissionService.mContext.getPackageManager().getPermissionInfo(permission, 0);
                    return info.protectionLevel == 1;
                } catch (PackageManager.NameNotFoundException nnfe) {
                    VSlog.e(VivoPermissionService.TAG, "PermissionController-->isRuntimePermission No such permission: " + permission, nnfe);
                }
            }
            return false;
        }

        public int getPackageUid(String packageName, int flags) {
            if (packageName != null && VivoPermissionService.mContext != null) {
                try {
                    return VivoPermissionService.mContext.getPackageManager().getPackageUid(packageName, flags);
                } catch (PackageManager.NameNotFoundException nnfe) {
                    VSlog.e(VivoPermissionService.TAG, "PermissionController-->getPackageUid : " + packageName, nnfe);
                    return -1;
                }
            }
            return -1;
        }

        public int noteOp(String op, int uid, String packageName) {
            return 0;
        }
    }

    public int checkPermission(String permission, int pid, int uid, IVivoPermissionCallback cb) {
        int uid2;
        printfQuatitiesLog("checkPermission, perm: " + permission + ", pid: " + pid + ", uid: " + uid);
        boolean isMonitorSystemApp = VivoPermissionManager.getInstance().needMonitorSystemApp();
        boolean isMonitorSystemUid = VivoPermissionManager.getInstance().needMonitorSystemUid();
        if (1 == DEBUG_CTS_TEST) {
            printfDebug("debug mode,then PERMISSION_GRANTED");
            return 1;
        } else if (uid == 0) {
            printfDebug("root then PERMISSION_GRANTED!");
            return 1;
        } else if (uid == 1000 && !isMonitorSystemUid) {
            printfQuatitiesLog("SYSTEM_UID then PERMISSION_GRANTED!");
            return 1;
        } else {
            VivoPermissionType vpType = VivoPermissionType.getVPType(permission);
            if (vpType == VivoPermissionType.LAST) {
                printfQuatitiesLog("permission=" + permission + "; It's VivoPermissionType.LAST, then PERMISSION_GRANTED!");
                return 1;
            } else if (vpType.getVPCategory() == VivoPermissionType.VivoPermissionCategory.OTHERS) {
                printfDebug("permission=" + permission + "; It's VivoPermissionCategory.OTHERS, then PERMISSION_GRANTED!");
                return 1;
            } else {
                long identity = Binder.clearCallingIdentity();
                try {
                    PackageInfo appInfo = VivoPermissionManager.getInstance().getCallingPackageInfo(mContext, uid);
                    Binder.restoreCallingIdentity(identity);
                    if (appInfo == null || appInfo.applicationInfo == null) {
                        printfInfo("getCallingPackageInfo == null, then PERMISSION_GRANTED! uid:" + uid + " pid:" + pid);
                        return 1;
                    } else if (appInfo.applicationInfo.targetSdkVersion < 23 || (!this.mVPC.isRuntimePermission(permission) && !"android.permission.ACCESS_WIFI_STATE".equals(permission))) {
                        if (isTestApp(appInfo.packageName)) {
                            printfDebug("test: " + appInfo.packageName + "; (" + permission + ") then PERMISSION_GRANTED!");
                            return 1;
                        } else if (!needCheckPkg(appInfo) && !isMonitorSystemApp) {
                            printfDebug("no need Check: " + appInfo.packageName + "; (" + permission + ") then PERMISSION_GRANTED!");
                            return 1;
                        } else {
                            if ("android.permission.BLUETOOTH".equals(permission) || "android.permission.CHANGE_WIFI_STATE".equals(permission)) {
                                PackageManager pm = mContext.getPackageManager();
                                if (!isMonitorSystemApp && (pm.checkSignatures(VivoPermissionUtils.OS_PKG, appInfo.packageName) == 0 || pm.checkSignatures("com.android.providers.contacts", appInfo.packageName) == 0 || pm.checkSignatures("com.android.providers.media", appInfo.packageName) == 0)) {
                                    printfDebug("vivo app: " + appInfo.packageName + "; (" + permission + ") then PERMISSION_GRANTED!");
                                    return 1;
                                }
                            }
                            if (!checkDoubleAppUserid(UserHandle.getUserId(uid))) {
                                uid2 = uid;
                            } else {
                                printfDebug("uid=" + uid + " is not owner user,adjust to owner user :" + UserHandle.getAppId(uid));
                                uid2 = UserHandle.getAppId(uid);
                            }
                            String packageName = appInfo.packageName;
                            printfDebug("start checkPermission packageName=" + packageName + " (" + permission + ") ;pid=" + pid + ";uid=" + uid2);
                            int configResult = waitConfirmPermission(pid, packageName, permission, cb, uid2);
                            if (configResult == 1 || configResult == 5) {
                                return 1;
                            }
                            if (configResult == 3) {
                                return 3;
                            }
                            return 2;
                        }
                    } else {
                        printfQuatitiesLog("Runtime permission, just pass for vps! uid:" + uid + " pid:" + pid);
                        return 1;
                    }
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                    throw th;
                }
            }
        }
    }

    public int checkDelete(String path, String pkgName, boolean recycle, String key, int uid, long availdableSpace) {
        long currentTime;
        long currentTime2;
        String packageName;
        String pkgLabel;
        ArrayList<String> tempPkgs;
        String pkgLabel2;
        long currentTime3;
        if (uid == getMediaProviderUid()) {
            printfDebug("uid is the uid of mediaprovider, then pass! uid:" + uid + " then return FILE_DELETE_AGREE");
            return VivoPermissionManager.FILE_DELETE_AGREE;
        }
        long currentTime4 = System.currentTimeMillis();
        if (uid == this.lastUid && this.mPkgLabelUid.get(uid) != null) {
            packageName = this.mPackageUid.get(uid);
            pkgLabel = this.mPkgLabelUid.get(uid);
        } else {
            long identity = Binder.clearCallingIdentity();
            try {
                PackageInfo appInfo = VivoPermissionManager.getInstance().getCallingPackageInfo(mContext, uid);
                if (appInfo == null) {
                    currentTime = currentTime4;
                } else if (appInfo.packageName == null) {
                    currentTime = currentTime4;
                } else {
                    String packageName2 = appInfo.packageName;
                    String pkgLabel3 = appInfo.applicationInfo.loadLabel(mContext.getPackageManager()).toString();
                    printfInfo("check delete pkgName: " + packageName2 + ", pkgLable: " + pkgLabel3);
                    this.mPackageUid.put(uid, packageName2);
                    this.mPkgLabelUid.put(uid, pkgLabel3);
                    this.lastUid = uid;
                    packageName = packageName2;
                    pkgLabel = pkgLabel3;
                }
                printfDebug("getCallingPackageInfo == null, then pass! uid:" + uid);
                if (this.lastPath.contains(key) && this.lastPkg.contains(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                    currentTime2 = currentTime;
                    if (currentTime2 - this.lastTime < 200) {
                        this.count++;
                        return VivoPermissionManager.FILE_DELETE_AGREE;
                    }
                } else {
                    currentTime2 = currentTime;
                }
                VCD_VC_2(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, path, 0, -1, 6, this.count);
                this.count = 0;
                this.lastTime = currentTime2;
                this.lastPath.clear();
                this.lastPath.add(key);
                this.lastPkg.clear();
                this.lastPkg.add(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                return VivoPermissionManager.FILE_DELETE_AGREE;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        ArrayList<String> tempPkgs2 = VivoDeleteUtils.mWhitePkgs;
        if (mWhitePkgs.size() <= 0) {
            tempPkgs = tempPkgs2;
        } else {
            ArrayList<String> tempPkgs3 = mWhitePkgs;
            tempPkgs = tempPkgs3;
        }
        if (tempPkgs.size() <= 0) {
            pkgLabel2 = pkgLabel;
            currentTime3 = currentTime4;
        } else {
            Iterator<String> it = tempPkgs.iterator();
            while (it.hasNext()) {
                String white = it.next();
                if (TextUtils.equals(white, packageName)) {
                    printfDebug("white list: " + packageName + "; (" + path + ") then pass!");
                    if (this.lastPath.contains(key) && this.lastPkg.contains(packageName) && currentTime4 - this.lastTime < 200) {
                        this.count++;
                    } else {
                        VCD_VC_2(packageName, path, 0, -1, 1, this.count);
                        this.count = 0;
                        this.lastTime = currentTime4;
                        this.lastPath.clear();
                        this.lastPath.add(key);
                        this.lastPkg.clear();
                        this.lastPkg.add(packageName);
                    }
                    return VivoPermissionManager.FILE_DELETE_AGREE;
                }
                pkgLabel = pkgLabel;
            }
            pkgLabel2 = pkgLabel;
            currentTime3 = currentTime4;
        }
        if (TextUtils.equals(pkgName, packageName)) {
            printfDebug("Owner: " + packageName + "; (" + path + ") then pass!");
            if (this.lastPath.contains(key) && this.lastPkg.contains(packageName) && currentTime3 - this.lastTime < 200) {
                this.count++;
            } else {
                VCD_VC_2(packageName, path, 0, -1, 4, this.count);
                this.count = 0;
                this.lastTime = currentTime3;
                this.lastPath.clear();
                this.lastPath.add(key);
                this.lastPkg.clear();
                this.lastPkg.add(packageName);
            }
            return VivoPermissionManager.FILE_DELETE_AGREE;
        } else if (isTestApp(packageName)) {
            printfDebug("test: " + packageName + "; (" + path + ") then pass!");
            if (this.lastPath.contains(key) && this.lastPkg.contains(packageName) && currentTime3 - this.lastTime < 200) {
                this.count++;
            } else {
                VCD_VC_2(packageName, path, 0, -1, 5, this.count);
                this.count = 0;
                this.lastTime = currentTime3;
                this.lastPath.clear();
                this.lastPath.add(key);
                this.lastPkg.clear();
                this.lastPkg.add(packageName);
            }
            return VivoPermissionManager.FILE_DELETE_AGREE;
        } else if (VivoPermissionManager.getInstance().isVivoApp(mContext, packageName)) {
            printfDebug("vivo App: " + packageName + "; (" + path + ") then pass!");
            if (this.lastPath.contains(key) && this.lastPkg.contains(packageName) && currentTime3 - this.lastTime < 200) {
                this.count++;
            } else {
                VCD_VC_2(packageName, path, 0, -1, 0, this.count);
                this.count = 0;
                this.lastTime = currentTime3;
                this.lastPath.clear();
                this.lastPath.add(key);
                this.lastPkg.clear();
                this.lastPkg.add(packageName);
            }
            return VivoPermissionManager.FILE_DELETE_AGREE;
        } else {
            boolean isRuningForeground = isRunningForeground(packageName);
            if (recycle && this.configRecycle && isGallerySupportRecycle(uid) && availdableSpace > minSpace) {
                sendDeleteFileBroadCast(pkgName, pkgLabel2, uid, isRuningForeground);
                printfQuatitiesLog("recycle the picture, then return FILE_DELETE_RECYCLE");
                recordDelete(key, packageName, currentTime3, path, isRuningForeground, 9, 0, this.count);
                return VivoPermissionManager.FILE_DELETE_RECYCLE;
            }
            printfQuatitiesLog("all check pass, then return FILE_DELETE_AGREE");
            recordDelete(key, packageName, currentTime3, path, isRuningForeground, 8, 0, this.count);
            return VivoPermissionManager.FILE_DELETE_AGREE;
        }
    }

    public List<VivoPermissionInfo> getSpecifiedPermAppList(int vpTypeId) {
        int userId = verifyIncomingUserId(UserHandle.getUserId(Binder.getCallingUid()));
        printfDebug("getSpecifiedPermAppList userId = " + userId);
        return this.mVPC.getSpecifiedPermAppList(vpTypeId, userId);
    }

    public List<VivoPermissionInfo> getTrustedAppList() {
        int userId = verifyIncomingUserId(UserHandle.getUserId(Binder.getCallingUid()));
        printfDebug("getTrustedAppList userId = " + userId);
        return this.mVPC.getTrustedAppList(userId);
    }

    public List<VivoPermissionInfo> getMonitorAppList() {
        int userId = verifyIncomingUserId(UserHandle.getUserId(Binder.getCallingUid()));
        printfDebug("getMonitorAppList userId = " + userId);
        return this.mVPC.getMonitorAppList(userId);
    }

    public VivoPermissionInfo getAppPermission(String packageName) {
        int userId = verifyIncomingUserId(UserHandle.getUserId(Binder.getCallingUid()));
        printfDebug("getAppPermission userId = " + userId + ", pkg: " + packageName);
        long identity = Binder.clearCallingIdentity();
        try {
            this.mVPC.handleRuntimePermission(packageName, false, userId);
            Binder.restoreCallingIdentity(identity);
            return this.mVPC.getAppPermission(packageName, userId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public VivoPermissionInfo getAppPermission(String packageName, int uid) {
        int userId = verifyIncomingUserId(UserHandle.getUserId(uid));
        printfDebug("getAppPermission userId = " + userId);
        long identity = Binder.clearCallingIdentity();
        try {
            this.mVPC.handleRuntimePermission(packageName, false, userId);
            Binder.restoreCallingIdentity(identity);
            return this.mVPC.getAppPermission(packageName, userId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public void setAppPermissionExt(VivoPermissionInfo vpi) {
        enforcePermission();
        int userId = verifyIncomingUserId(UserHandle.getUserId(Binder.getCallingUid()));
        printfDebug("setAppPermissionExt userId = " + userId);
        this.mVPC.saveAppPermission(vpi, userId);
    }

    public void setAppPermission(String packageName, int vpTypeId, int result) {
        printfDebug("setAppPermission    packageName=" + packageName + " vpTypeId=" + vpTypeId + " result=" + result);
        enforcePermission();
        int userId = verifyIncomingUserId(UserHandle.getUserId(Binder.getCallingUid()));
        StringBuilder sb = new StringBuilder();
        sb.append("setAppPermission userId = ");
        sb.append(userId);
        printfDebug(sb.toString());
        if (!this.mVPC.doForRuntimePermission(packageName, vpTypeId, result, userId)) {
            this.mVPC.saveAppPermission(packageName, vpTypeId, result, userId);
        }
    }

    public void setAppPermission(String packageName, int vpTypeId, int result, int uid) {
        enforcePermission();
        int userId = verifyIncomingUserId(UserHandle.getUserId(uid));
        printfDebug("setAppPermissions userId = " + userId);
        if (!this.mVPC.doForRuntimePermission(packageName, vpTypeId, result, userId)) {
            this.mVPC.saveAppPermission(packageName, vpTypeId, result, userId);
        }
    }

    public void setWhiteListApp(String packageName, boolean enable) {
        enforcePermission();
        int userId = verifyIncomingUserId(UserHandle.getUserId(Binder.getCallingUid()));
        printfDebug("setWhiteListApp userId = " + userId + ", pkg: " + packageName + ", enable: " + enable);
        this.mVPC.grantAllRuntimePermissions(packageName, enable, userId);
        this.mVPC.setWhiteListApp(packageName, enable, userId);
    }

    public void setBlackListApp(String packageName, boolean enable) {
        enforcePermission();
        int userId = verifyIncomingUserId(UserHandle.getUserId(Binder.getCallingUid()));
        printfDebug("setBlackListApp userId = " + userId + ", pkg: " + packageName + ", enable: " + enable);
        this.mVPC.setBlackListApp(packageName, enable, userId);
    }

    public void noteStartActivityProcess(String packageName) {
    }

    public boolean isBuildInThirdPartApp(String packageName) {
        int userId = verifyIncomingUserId(UserHandle.getUserId(Binder.getCallingUid()));
        printfDebug("isBuildInThirdPartApp userId = " + userId);
        return this.mVPC.isBuildInThirdPartApp(packageName, userId);
    }

    public int getVPMVersion() {
        return 2;
    }

    public int getVPMDataBaseState() {
        int userId = verifyIncomingUserId(UserHandle.getUserId(Binder.getCallingUid()));
        printfDebug("getVPMDataBaseState userId = " + userId);
        return this.mVPC.getDataBaseState(userId);
    }

    private void enforcePermission() {
        if (Binder.getCallingPid() == Process.myPid()) {
            return;
        }
        mContext.enforcePermission("android.permission.WRITE_SECURE_SETTINGS", Binder.getCallingPid(), Binder.getCallingUid(), null);
    }

    public boolean isCheckingPermission(int pid) {
        int uid = Process.getUidForPid(pid);
        synchronized (this.mVPDMap) {
            int size = this.mVPDMap.size();
            printfInfo("isCheckingPermission pid=" + pid + " uid=" + uid + " size=" + size);
            if (size > 0) {
                for (Map.Entry<String, VivoPermissionDialog> entry : this.mVPDMap.entrySet()) {
                    VivoPermissionDialog vpd = entry.getValue();
                    int callingUid = vpd.getCallingUid();
                    printfInfo("isCheckingPermission pid=" + pid + " uid=" + uid + " callingUid=" + callingUid);
                    if (callingUid == uid) {
                        printfInfo("isCheckingPermission=true; pid=" + pid + " uid=" + uid);
                        return true;
                    }
                }
            }
            synchronized (this.mVPDMap3) {
                int sizeThree = this.mVPDMap3.size();
                if (sizeThree > 0) {
                    for (Map.Entry<String, VivoPermissionDeniedDialogModeThree> entryThree : this.mVPDMap3.entrySet()) {
                        VivoPermissionDeniedDialogModeThree vpdThree = entryThree.getValue();
                        printfInfo("3isCheckingPermission pid=" + pid + " vpdThree.getCallingUid()=" + vpdThree.getCallingUid());
                        if (vpdThree.getCallingUid() == uid) {
                            printfInfo("3isCheckingPermission=true; uid=" + uid);
                            return true;
                        }
                    }
                }
                synchronized (this.mVDDMap) {
                    int sizeDelete = this.mVDDMap.size();
                    if (sizeDelete > 0) {
                        for (Map.Entry<String, VivoDeleteDialog> entryDelete : this.mVDDMap.entrySet()) {
                            VivoDeleteDialog vdd = entryDelete.getValue();
                            printfInfo("delete isCheckingPermission uid=" + uid + " vdd.getCallingUid()=" + vdd.getCallingUid());
                            if (vdd.getCallingUid() == uid) {
                                printfInfo("delete isCheckingPermission=true; uid=" + uid);
                                return true;
                            }
                        }
                    }
                    return false;
                }
            }
        }
    }

    public static boolean isScreenOn(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        boolean isScreenOn = powerManager.isScreenOn();
        return isScreenOn;
    }

    public static boolean isKeyguardLocked(Context context) {
        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService("keyguard");
        boolean isKeyguardLocked = mKeyguardManager.isKeyguardLocked();
        return isKeyguardLocked;
    }

    public static boolean isSuperPowerSaveOn() {
        return SystemProperties.getBoolean("sys.super_power_save", false);
    }

    private boolean isProcessForeground(int uid) {
        ActivityManagerInternal activityManagerInternal = this.amInternal;
        if (activityManagerInternal == null) {
            printfDebug("amInternal is null then return true");
            return true;
        }
        int processState = activityManagerInternal.getUidProcessState(uid);
        printfDebug("process state is " + processState);
        return processState >= 0 && processState <= 4;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isRunningForeground(String packageName) {
        long origId = Binder.clearCallingIdentity();
        ActivityManager am = (ActivityManager) mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
        try {
            try {
                ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
                Binder.restoreCallingIdentity(origId);
                String currentPackageName = cn.getPackageName();
                return !TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(packageName);
            } catch (SecurityException e) {
                printfInfo(packageName + " requires android.permission.GET_TASKS fail,so consider isRunningForeground");
                e.printStackTrace();
                Binder.restoreCallingIdentity(origId);
                return true;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
            throw th;
        }
    }

    private String getVPDMapKey(String packageName, VivoPermissionType vpType) {
        String key = packageName + vpType;
        return key;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getVPDMapKey(String packageName, String permName) {
        String key = packageName + VivoPermissionType.getVPType(permName);
        return key;
    }

    private int waitConfirmPermission(int pid, String packageName, String permName, IVivoPermissionCallback cb, int uid) {
        boolean isConfigResultDenied;
        int userId = verifyIncomingUserId(UserHandle.getUserId(uid));
        printfDebug("waitConfirmPermission userId = " + userId);
        String currVPDMapKey = getVPDMapKey(packageName, permName);
        synchronized (mVPSLock) {
            try {
                int configResult = this.mVPC.checkConfigPermission(packageName, permName, userId);
                if (configResult == 2) {
                    isConfigResultDenied = true;
                } else if (configResult == 1) {
                    printfDebug(permName + " of " + packageName + " was set GRANTED");
                    if (VivoPermissionType.getVPType(permName).getVPTypeId() == 30) {
                        VCD_VC_1(packageName, "1", "-1");
                    }
                    return 1;
                } else if (configResult != 0) {
                    isConfigResultDenied = false;
                } else {
                    printfInfo(packageName + " has UNKNOWN PERMISSION:" + permName + ", but GRANTED");
                    return 1;
                }
                try {
                    if (VivoPermissionType.getVPType(permName).getVPTypeId() == 30) {
                        if (isConfigResultDenied) {
                            VCD_VC_1(packageName, "2", "-1");
                            return 2;
                        } else if (isRunningForeground(packageName)) {
                            if (configResult == 5) {
                                VCD_VC_1(packageName, InputExceptionReport.LEVEL_VERY_LOW, "1");
                                return 1;
                            }
                        } else {
                            if (configResult == 3) {
                                VCD_VC_1(packageName, InputExceptionReport.LEVEL_MEDIUM, "0");
                            } else if (configResult == 5) {
                                VCD_VC_1(packageName, InputExceptionReport.LEVEL_VERY_LOW, "0");
                            }
                            return 2;
                        }
                    }
                    if (isKeyguardLocked(mContext)) {
                        printfInfo(packageName + " is requesting " + permName + ", but KeyguardLocked11, so DENIED");
                        return 2;
                    } else if (isSuperPowerSaveOn()) {
                        printfInfo(packageName + " is requesting " + permName + ", but SuperPowerSaveOn, so DENIED");
                        return 2;
                    } else if (configResult == 5) {
                        return isProcessForeground(uid) ? 1 : 2;
                    } else if (isConfigResultDenied) {
                        if (!isRunningForeground(packageName)) {
                            printfInfo(packageName + " is requesting " + permName + ", but is not RunningForeground, just return DENIED");
                            return 2;
                        }
                        printfInfo(permName + " of " + packageName + " was set DENIED");
                        int deniedMode = this.mVPC.checkConfigDeniedMode(packageName, permName, userId);
                        int deniedDialogMode = this.mVPC.checkConfigDeniedDialogMode(packageName, permName, userId);
                        if (deniedMode == 48 || deniedMode == 64) {
                            if (deniedDialogMode == 256) {
                                showDeniedDialogToSetting(packageName, permName, uid, currVPDMapKey);
                            } else if (deniedDialogMode == 512) {
                                showDeniedDialogToChoose(packageName, permName, uid, currVPDMapKey);
                            } else if (deniedDialogMode == 768) {
                                return showDeniedDialogToChooseAndCountDown(packageName, permName, uid, currVPDMapKey, cb);
                            }
                            return 2;
                        }
                        return 2;
                    } else {
                        return showWarningDialogToChoose(packageName, permName, pid, uid, currVPDMapKey, cb);
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void removeVPD(String key) {
        synchronized (this.mVPDMap) {
            if (this.mVPDMap.containsKey(key)) {
                this.mVPDMap.remove(key);
            }
        }
    }

    public void removeVDD(String key) {
        synchronized (this.mVDDMap) {
            if (this.mVDDMap.containsKey(key)) {
                this.mVDDMap.remove(key);
            }
        }
    }

    public void removeVPD1(String key) {
        synchronized (this.mVPDMap1) {
            if (this.mVPDMap1.containsKey(key)) {
                this.mVPDMap1.remove(key);
            }
        }
    }

    public void removeVPD2(String key) {
        synchronized (this.mVPDMap2) {
            if (this.mVPDMap2.containsKey(key)) {
                this.mVPDMap2.remove(key);
            }
        }
    }

    public void removeVPD3(String key) {
        synchronized (this.mVPDMap3) {
            if (this.mVPDMap3.containsKey(key)) {
                this.mVPDMap3.remove(key);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int showWarningDialogToChoose(final String packageName, final String permName, int pid, int uid, String currVPDMapKey, IVivoPermissionCallback cb) {
        VivoPermissionDialog vpd;
        final VivoPermissionDialog vpd2;
        VivoPermissionType vpType = VivoPermissionType.getVPType(permName);
        synchronized (this.mVPDMap) {
            try {
                vpd = this.mVPDMap.get(currVPDMapKey);
            } catch (Throwable th) {
                th = th;
            }
            try {
                if (vpd == null) {
                    if (vpType == VivoPermissionType.ACCESS_LOCATION) {
                        if (this.mCheckLocationBinderTimes.containsKey(currVPDMapKey)) {
                            this.mCheckLocationBinderTimes.remove(currVPDMapKey);
                        }
                        this.mCheckLocationBinderTimes.put(currVPDMapKey, 0);
                    }
                    vpd2 = new VivoPermissionDialog(this, mContext, this.mUiHandler, packageName, permName, uid, currVPDMapKey);
                    this.mVPDMap.put(currVPDMapKey, vpd2);
                    this.mUiHandler.post(new Runnable() { // from class: com.vivo.services.security.server.VivoPermissionService.2
                        @Override // java.lang.Runnable
                        public void run() {
                            VivoPermissionService.printfInfo("0 Showing VivoPermissionDialog: " + packageName + "; " + permName);
                            vpd2.show();
                        }
                    });
                } else {
                    if (vpType == VivoPermissionType.ACCESS_LOCATION) {
                        if (this.mCheckLocationBinderTimes.containsKey(currVPDMapKey)) {
                            int binderCheckTimes = this.mCheckLocationBinderTimes.get(currVPDMapKey).intValue();
                            printfInfo("0 binderCheckTimes:" + binderCheckTimes);
                            if (binderCheckTimes >= 3) {
                                return 2;
                            }
                            this.mCheckLocationBinderTimes.remove(currVPDMapKey);
                            int binderCheckTimesAdd = binderCheckTimes + 1;
                            this.mCheckLocationBinderTimes.put(currVPDMapKey, Integer.valueOf(binderCheckTimesAdd));
                        } else {
                            printfError("0 check package:" + packageName + " location perm,but not have record");
                        }
                    }
                    vpd2 = vpd;
                }
                String curThreadInfo = Thread.currentThread().toString();
                synchronized (vpd2) {
                    try {
                        if (!vpd2.isPermissionConfirmed()) {
                            if (cb != null) {
                                vpd2.registerCallback(cb);
                                printfInfo("0 AsyncModeConfirm: return WARNING to Client!");
                                return 3;
                            }
                            try {
                                printfInfo("0 Waiting ThreadInfo: " + curThreadInfo + "; " + packageName + "; " + permName);
                                vpd2.wait(25000L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (!vpd2.isPermissionConfirmed()) {
                                printfInfo("0 wait 25s timeout");
                                vpd2.handleWaitTimeOut();
                            }
                        }
                        int result = vpd2.getPermissionResult(permName);
                        printfInfo("0 Finishing ThreadInfo: " + curThreadInfo + "; " + packageName + "; " + permName + "=" + result);
                        if (result == 5) {
                            if (!isProcessForeground(uid)) {
                                return 2;
                            }
                            return 1;
                        }
                        return result;
                    } catch (Throwable th2) {
                        th = th2;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        }
                        throw th;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th5) {
                        th = th5;
                    }
                }
                throw th;
            }
        }
    }

    private void showDeniedDialogToSetting(final String packageName, final String permName, int uid, String currVPDMapKey) {
        VivoPermissionType.getVPType(permName);
        synchronized (this.mVPDMap1) {
            try {
                try {
                    if (this.mVPDMap1.get(currVPDMapKey) == null) {
                        try {
                            final VivoPermissionDeniedDialogModeOne vpd = new VivoPermissionDeniedDialogModeOne(this, mContext, this.mUiHandler, packageName, permName, uid, currVPDMapKey);
                            this.mVPDMap1.put(currVPDMapKey, vpd);
                            this.mUiHandler.post(new Runnable() { // from class: com.vivo.services.security.server.VivoPermissionService.3
                                @Override // java.lang.Runnable
                                public void run() {
                                    VivoPermissionService.printfInfo("1 Showing VivoPermissionDeniedDialogModeOne: " + packageName + "; " + permName);
                                    vpd.show();
                                }
                            });
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    private void showDeniedDialogToChoose(final String packageName, final String permName, int uid, String currVPDMapKey) {
        VivoPermissionType.getVPType(permName);
        synchronized (this.mVPDMap2) {
            try {
                try {
                    if (this.mVPDMap2.get(currVPDMapKey) == null) {
                        try {
                            final VivoPermissionDeniedDialogModeTwo vpd = new VivoPermissionDeniedDialogModeTwo(this, mContext, this.mUiHandler, packageName, permName, uid, currVPDMapKey);
                            this.mVPDMap2.put(currVPDMapKey, vpd);
                            this.mUiHandler.post(new Runnable() { // from class: com.vivo.services.security.server.VivoPermissionService.4
                                @Override // java.lang.Runnable
                                public void run() {
                                    VivoPermissionService.printfInfo("2 Showing VivoPermissionDeniedDialogModeTwo: " + packageName + "; " + permName);
                                    vpd.show();
                                }
                            });
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    private int showDeniedDialogToChooseAndCountDown(final String packageName, final String permName, int uid, String currVPDMapKey, IVivoPermissionCallback cb) {
        final VivoPermissionDeniedDialogModeThree vpd;
        VivoPermissionType vpType = VivoPermissionType.getVPType(permName);
        synchronized (this.mVPDMap3) {
            try {
                VivoPermissionDeniedDialogModeThree vpd2 = this.mVPDMap3.get(currVPDMapKey);
                try {
                    if (vpd2 == null) {
                        if (vpType == VivoPermissionType.ACCESS_LOCATION) {
                            if (this.mCheckLocationBinderTimes.containsKey(currVPDMapKey)) {
                                this.mCheckLocationBinderTimes.remove(currVPDMapKey);
                            }
                            this.mCheckLocationBinderTimes.put(currVPDMapKey, 0);
                        }
                        vpd = new VivoPermissionDeniedDialogModeThree(this, mContext, this.mUiHandler, packageName, permName, uid, currVPDMapKey);
                        this.mVPDMap3.put(currVPDMapKey, vpd);
                        this.mUiHandler.post(new Runnable() { // from class: com.vivo.services.security.server.VivoPermissionService.5
                            @Override // java.lang.Runnable
                            public void run() {
                                VivoPermissionService.printfInfo("3 Showing VivoPermissionDeniedDialogModeThree: " + packageName + "; " + permName);
                                vpd.show();
                            }
                        });
                    } else {
                        if (vpType == VivoPermissionType.ACCESS_LOCATION) {
                            if (this.mCheckLocationBinderTimes.containsKey(currVPDMapKey)) {
                                int binderCheckTimes = this.mCheckLocationBinderTimes.get(currVPDMapKey).intValue();
                                if (binderCheckTimes >= 3) {
                                    return 2;
                                }
                                this.mCheckLocationBinderTimes.remove(currVPDMapKey);
                                int binderCheckTimesAdd = binderCheckTimes + 1;
                                this.mCheckLocationBinderTimes.put(currVPDMapKey, Integer.valueOf(binderCheckTimesAdd));
                            } else {
                                printfError("3 check package:" + packageName + " location perm,but not have record");
                            }
                        }
                        vpd = vpd2;
                    }
                    String curThreadInfo = Thread.currentThread().toString();
                    synchronized (vpd) {
                        if (!vpd.isPermissionConfirmed()) {
                            if (cb != null) {
                                vpd.registerCallback(cb);
                                printfInfo("3 AsyncModeConfirm: return WARNING to Client!");
                                return 3;
                            }
                            try {
                                printfInfo("3 Waiting ThreadInfo: " + curThreadInfo + "; " + packageName + "; " + permName);
                                vpd.wait(25000L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (!vpd.isPermissionConfirmed()) {
                                printfInfo("3 wait 25s timeout");
                                vpd.handleWaitTimeOut();
                            }
                        }
                        int result = vpd.getPermissionResult(permName);
                        printfInfo("3 Finishing ThreadInfo: " + curThreadInfo + "; " + packageName + "; " + permName + "=" + result);
                        return result;
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private void registerBroadcastReceiver() {
        if (this.mVPR == null) {
            this.mVPR = new VivoPermissionReceiver(this);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        intentFilter.addDataScheme("package");
        mContext.registerReceiverAsUser(this.mVPR, UserHandle.ALL, intentFilter, null, mVivoPermHandler);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        mContext.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.vivo.services.security.server.VivoPermissionService.6
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                VivoPermissionService.printfInfo("add onReceive = " + intent);
                if (action.equals("android.intent.action.USER_ADDED")) {
                    int user = intent.getIntExtra("android.intent.extra.user_handle", ProcessList.INVALID_ADJ);
                    VivoPermissionService.this.mVPC.doForUserInit(user);
                } else if (action.equals("android.intent.action.USER_REMOVED")) {
                    int user2 = intent.getIntExtra("android.intent.extra.user_handle", ProcessList.INVALID_ADJ);
                    VivoPermissionService.this.mVPC.doForUserRemoved(user2);
                }
            }
        }, UserHandle.ALL, filter, null, mVivoPermHandler);
    }

    private void unregisterBroadcastReceiver() {
        VivoPermissionReceiver vivoPermissionReceiver = this.mVPR;
        if (vivoPermissionReceiver != null) {
            mContext.unregisterReceiver(vivoPermissionReceiver);
            this.mVPR = null;
        }
    }

    public void dismissAllDialog() {
        int size = this.mVPDMap.size();
        if (size <= 0) {
            return;
        }
        HashMap<String, VivoPermissionDialog> vpdMapBackUp = (HashMap) this.mVPDMap.clone();
        for (Map.Entry<String, VivoPermissionDialog> entry : vpdMapBackUp.entrySet()) {
            VivoPermissionDialog vpd = entry.getValue();
            vpd.dismiss();
        }
        vpdMapBackUp.clear();
    }

    public int checkConfigDeniedMode(String packageName, String permName) {
        int userId = verifyIncomingUserId(UserHandle.getUserId(Binder.getCallingUid()));
        printfDebug("checkConfigDeniedMode userId = " + userId);
        return this.mVPC.checkConfigDeniedMode(packageName, permName, userId);
    }

    public void setConfigDeniedMode(String packageName, String permName, int deniedMode) {
        int userId = verifyIncomingUserId(UserHandle.getUserId(Binder.getCallingUid()));
        printfDebug("setConfigDeniedMode userId = " + userId);
        this.mVPC.setConfigDeniedMode(packageName, permName, deniedMode, userId);
    }

    public void setConfigDeniedMode(String packageName, String permName, int deniedMode, int uid) {
        int userId = verifyIncomingUserId(UserHandle.getUserId(uid));
        printfDebug("setConfigDeniedMode userId = " + userId);
        this.mVPC.setConfigDeniedMode(packageName, permName, deniedMode, userId);
    }

    public void updateForPackageReplaced(String packageName, int userId) {
        int userId2 = verifyIncomingUserId(userId);
        printfDebug("updateForPackageReplaced userId = " + userId2);
        this.mVPC.updateForPackageReplaced(packageName, userId2);
    }

    public void updateForPackageRemoved(String packageName, int userId) {
        int userId2 = verifyIncomingUserId(userId);
        printfDebug("updateForPackageRemoved userId = " + userId2);
        this.mVPC.updateForPackageRemoved(packageName, userId2);
    }

    public void updateForPackageAdded(String packageName, boolean grantPermissions, int userId) {
        int userId2 = verifyIncomingUserId(userId);
        printfDebug("updateForPackageAdded userId = " + userId2);
        this.mVPC.updateForPackageAdded(packageName, grantPermissions, userId2);
    }

    public static void printfDebug(String msg) {
        if (DEBUG_VPS) {
            if (VivoPermissionManager.ENG || VivoPermissionManager.IS_LOG_CTRL_OPEN || PRINTF_DEBUG) {
                VSlog.d(TAG, msg);
            }
        }
    }

    public static void printfInfo(String msg) {
        if (DEBUG_VPS) {
            VSlog.i(TAG, msg);
        }
    }

    public static void printfError(String msg) {
        if (DEBUG_VPS) {
            VSlog.e(TAG, msg);
        }
    }

    public static void printfQuatitiesLog(String msg) {
        if (PRINTF_DEBUG) {
            VSlog.d(TAG, msg);
        }
    }

    public int checkOnePermission(String packageName, String perm, int uid) {
        return this.mVPC.checkOnePermission(packageName, perm, uid);
    }

    public boolean setOnePermission(String packageName, String perm, int uid, boolean granted) {
        enforcePermission();
        return this.mVPC.setOnePermission(packageName, perm, uid, granted);
    }

    public boolean setOnePermissionExt(String packageName, String perm, int uid, int result) {
        printfDebug("setOnePermissionExt,packageName: " + packageName + ", perm: " + perm + ", result: " + result);
        enforcePermission();
        return this.mVPC.setOnePermissionExt(packageName, perm, uid, result);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static boolean needHandleGroup(String perm) {
        char c;
        switch (perm.hashCode()) {
            case -1674700861:
                if (perm.equals("android.permission.ANSWER_PHONE_CALLS")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -1164582768:
                if (perm.equals("android.permission.READ_PHONE_NUMBERS")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 441496538:
                if (perm.equals("android.permission.ACCEPT_HANDOVER")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 784519842:
                if (perm.equals("android.permission.USE_SIP")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 952819282:
                if (perm.equals("android.permission.PROCESS_OUTGOING_CALLS")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 958655846:
                if (perm.equals("android.permission.READ_CELL_BROADCASTS")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1271781903:
                if (perm.equals("android.permission.GET_ACCOUNTS")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 2133799037:
                if (perm.equals("com.android.voicemail.permission.ADD_VOICEMAIL")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                return true;
            default:
                return false;
        }
    }

    PackageManagerInternal getPackageManagerInternalLocked() {
        if (this.mPackageManagerInt == null) {
            this.mPackageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }
        return this.mPackageManagerInt;
    }

    public boolean isTestApp(String pkg) {
        try {
            PackageManagerInternal pmil = getPackageManagerInternalLocked();
            boolean isTest = pmil.isTestApp(pkg);
            return isTest;
        } catch (Exception e) {
            printfError("errors " + e);
            return false;
        }
    }

    public boolean isHiddenApplication(String pkgName) {
        try {
            PackageManager pm = mContext.getPackageManager();
            if (pm.getClass() == null) {
                return false;
            }
            Method mtd = pm.getClass().getMethod("getVHiddenApplicaiton", String.class);
            int result = ((Integer) mtd.invoke(pm, pkgName)).intValue();
            if (result == 1) {
                return false;
            }
            return true;
        } catch (Exception e) {
            printfError("errors " + e);
            return false;
        }
    }

    public void systemReady() {
    }

    public int verifyIncomingUserId(int userId) {
        return userId;
    }

    public boolean checkDoubleAppUserid(int userId) {
        return false;
    }

    public boolean isDeletedSpecialSysPkg(String pkg) {
        try {
            PackageManagerInternal pmil = getPackageManagerInternalLocked();
            boolean isSpecial = pmil.isDeletedSpecialSysPkg(pkg);
            return isSpecial;
        } catch (Exception e) {
            printfError("errors " + e);
            return false;
        }
    }

    public boolean needCheckPkg(PackageInfo pi) {
        if (pi == null || pi.applicationInfo == null || ((!pi.applicationInfo.isSystemApp() && !pi.applicationInfo.isUpdatedSystemApp() && !pi.applicationInfo.isPrivilegedApp()) || isDeletedSpecialSysPkg(pi.packageName))) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class VivoPermHandler extends Handler {
        public VivoPermHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
            if (i == 1) {
                synchronized (VivoPermissionService.this.mCheckDeleteState) {
                    ArrayMap arrayMap = VivoPermissionService.this.mCheckDeleteState;
                    arrayMap.remove(msg.arg1 + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                }
            } else if (i != 2) {
                if (i == 3) {
                    VivoPermissionService.this.getconfig();
                    VivoPermissionService.access$008(VivoPermissionService.this);
                } else if (i == 5) {
                    String pkgName = msg.getData().getString("pkgName");
                    String pkgLable = msg.getData().getString("pkgLable");
                    boolean isForeground = msg.getData().getBoolean("isForeground");
                    int callingUid = msg.getData().getInt("callingUid");
                    VivoPermissionService.this.doSendDeleteFileBroadCast(pkgName, pkgLable, callingUid, isForeground);
                }
            } else {
                int uid = msg.arg2;
                if (uid > 10000) {
                    long identity = Binder.clearCallingIdentity();
                    try {
                        PackageInfo appInfo = VivoPermissionManager.getInstance().getCallingPackageInfo(VivoPermissionService.mContext, uid);
                        if (appInfo == null || appInfo.packageName == null) {
                            VivoPermissionService.printfDebug("getCallingPackageInfo == null");
                        } else if (appInfo.applicationInfo.targetSdkVersion >= 23) {
                            String packageName = appInfo.packageName;
                            if (VivoPermissionService.this.isTestApp(packageName)) {
                                VivoPermissionService.printfDebug("test: " + packageName);
                            } else if (!VivoPermissionService.this.needCheckPkg(appInfo)) {
                                VivoPermissionService.printfDebug("vivo App: " + packageName);
                            } else if (VivoPermissionService.this.checkOnePermission(packageName, "android.permission.READ_PHONE_STATE", uid) == 2) {
                                if (!VivoPermissionService.this.isVivoImeiPkg(packageName)) {
                                    VivoPermissionService.printfDebug("no need: " + packageName);
                                    return;
                                }
                                VivoPermissionInfo vpi = VivoPermissionService.this.getAppPermission(packageName, uid);
                                int vpid = VivoPermissionType.getVPType("android.permission.READ_PHONE_STATE").getVPTypeId();
                                if ((VivoPermissionService.this.needShowImeiTipsDialogOne(vpi, vpid) || VivoPermissionService.this.needShowImeiTipsDialogTwo(vpi, vpid)) && VivoPermissionService.this.isRunningForeground(packageName)) {
                                    VivoPermissionService.this.showWarningDialogToChoose(packageName, "android.permission.READ_PHONE_STATE", msg.arg1, uid, VivoPermissionService.this.getVPDMapKey(packageName, "android.permission.READ_PHONE_STATE"), null);
                                }
                            }
                        }
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getconfig() {
        ContentResolver resolver = mContext.getContentResolver();
        String[] selectionArgs = {"VivoImeiApps", "2", "2.5"};
        Cursor cursor = null;
        try {
            try {
                cursor = resolver.query(Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs"), null, null, selectionArgs, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    if (cursor.getCount() > 0) {
                        while (!cursor.isAfterLast()) {
                            byte[] filecontent = cursor.getBlob(3);
                            loadPkgs(filecontent);
                            cursor.moveToNext();
                        }
                    }
                } else if (this.retryCount < 4) {
                    if (mVivoPermHandler.hasMessages(3)) {
                        mVivoPermHandler.removeMessages(3);
                    }
                    mVivoPermHandler.sendEmptyMessageDelayed(3, CHECK_DELETE_TIME);
                }
            } catch (Exception e) {
                printfError("getconfig = " + e);
            }
            IoUtils.closeQuietly(cursor);
            cursor = cursor;
            String[] selectionArgs1 = {"vivoimei", "1", "1.0"};
            try {
                try {
                    Cursor cursor2 = resolver.query(Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs"), null, null, selectionArgs1, null);
                    if (cursor2 != null) {
                        cursor2.moveToFirst();
                        if (cursor2.getCount() > 0) {
                            while (!cursor2.isAfterLast()) {
                                byte[] filecontent2 = cursor2.getBlob(3);
                                loadConfig(filecontent2);
                                cursor2.moveToNext();
                            }
                        }
                    } else if (this.retryCount < 4) {
                        if (mVivoPermHandler.hasMessages(3)) {
                            mVivoPermHandler.removeMessages(3);
                        }
                        mVivoPermHandler.sendEmptyMessageDelayed(3, CHECK_DELETE_TIME);
                    }
                } catch (Exception e2) {
                    printfError("getconfig = " + e2);
                }
            } finally {
            }
        } finally {
        }
    }

    private void loadPkgs(byte[] bytes) {
        InputStream inputStream = null;
        ArraySet<String> tempSets = new ArraySet<>();
        try {
            if (bytes == null) {
                return;
            }
            try {
                XmlPullParserFactory pullFactory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = pullFactory.newPullParser();
                inputStream = new ByteArrayInputStream(bytes);
                parser.setInput(inputStream, "utf-8");
                tempSets.clear();
                for (int eventCode = parser.getEventType(); eventCode != 1; eventCode = parser.next()) {
                    if (eventCode == 2) {
                        String name = parser.getName();
                        if (TextUtils.equals(name, "package")) {
                            String pkgName = parser.nextText();
                            if (!TextUtils.isEmpty(pkgName)) {
                                tempSets.add(pkgName);
                            }
                        }
                    }
                }
                synchronized (this.vivoImeiSets) {
                    this.vivoImeiSets.clear();
                    this.vivoImeiSets.addAll((ArraySet<? extends String>) tempSets);
                }
            } catch (XmlPullParserException xmle) {
                printfError("XmlPullParserException = " + xmle);
            } catch (Exception e) {
                printfError("loadPkgs = " + e);
            }
        } finally {
            IoUtils.closeQuietly(inputStream);
        }
    }

    private void loadConfig(byte[] bytes) {
        InputStream inputStream = null;
        try {
            if (bytes == null) {
                return;
            }
            try {
                XmlPullParserFactory pullFactory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = pullFactory.newPullParser();
                inputStream = new ByteArrayInputStream(bytes);
                parser.setInput(inputStream, "utf-8");
                for (int eventCode = parser.getEventType(); eventCode != 1; eventCode = parser.next()) {
                    if (eventCode == 2) {
                        String name = parser.getName();
                        if (TextUtils.equals(name, "shwo-dialog-one")) {
                            String count = parser.getAttributeValue(null, "counts");
                            if (!TextUtils.isEmpty(count)) {
                                this.showIMIEOneTipsLimit = Integer.parseInt(count);
                            }
                        } else if (TextUtils.equals(name, "shwo-dialog-two")) {
                            String count2 = parser.getAttributeValue(null, "counts");
                            if (!TextUtils.isEmpty(count2)) {
                                this.showIMIETwoTipsLimit = Integer.parseInt(count2);
                            }
                        }
                    }
                }
            } catch (XmlPullParserException xmle) {
                printfError("XmlPullParserException = " + xmle);
            } catch (Exception e) {
                printfError("loadPkgs = " + e);
            }
        } finally {
            IoUtils.closeQuietly(inputStream);
        }
    }

    public boolean isVivoImeiPkg(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return false;
        }
        if (isBuildInThirdPartApp(pkg)) {
            return true;
        }
        synchronized (this.vivoImeiSets) {
            return isInstallTrustPkgs(pkg) && this.vivoImeiSets.contains(pkg);
        }
    }

    public boolean isInstallTrustPkgs(String pkg) {
        String installResource = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        try {
            installResource = mContext.getPackageManager().getInstallerPackageName(pkg);
        } catch (IllegalArgumentException e) {
            printfError("error-->" + e);
        }
        return !TextUtils.isEmpty(installResource) && installtrustSets.contains(installResource);
    }

    public boolean needShowImeiTipsDialogOne(VivoPermissionInfo vpi, int typeId) {
        return false;
    }

    public boolean needShowImeiTipsDialogTwo(VivoPermissionInfo vpi, int typeId) {
        return false;
    }

    /* loaded from: classes.dex */
    private class CheckDeleteState {
        private long checkTime;
        private boolean grant;

        public CheckDeleteState(boolean grant, long time) {
            this.grant = grant;
            this.checkTime = time;
        }

        public boolean isDeleteGrant() {
            return this.grant;
        }

        public boolean isNeedCheck() {
            if (SystemClock.elapsedRealtime() - this.checkTime > VivoPermissionService.CHECK_DELETE_TIME) {
                return true;
            }
            return false;
        }

        public void setDeleteGrant(boolean grant, long time) {
            this.grant = grant;
            this.checkTime = time;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean parserXml(File file) {
        FileInputStream fis = null;
        mWhitePkgs.clear();
        try {
            fis = new FileInputStream(file);
            XmlPullParserFactory pullFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = pullFactory.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            for (int eventCode = parser.getEventType(); eventCode != 1; eventCode = parser.next()) {
                if (eventCode == 2) {
                    String name = parser.getName();
                    if (TextUtils.equals("package", name)) {
                        String whitePkg = parser.getAttributeValue(null, "name");
                        mWhitePkgs.add(whitePkg);
                        printfDebug(" whitePkg=" + whitePkg);
                    } else if (TextUtils.equals("configRecycle", name)) {
                        String isConfigRecycle = parser.getAttributeValue(null, "value");
                        this.configRecycle = TextUtils.equals("true", isConfigRecycle);
                        printfDebug(" configRecycle=" + this.configRecycle);
                    } else if (TextUtils.equals("minSpace", name)) {
                        String minSpaceString = parser.getAttributeValue(null, "value");
                        minSpace = Long.valueOf(minSpaceString).longValue();
                        printfInfo("minSpaceString: " + minSpaceString + " minSpace=" + minSpace);
                    }
                }
            }
            return true;
        } catch (IOException e) {
            printfError("Failed parserXml +" + e);
            mWhitePkgs.clear();
            return false;
        } catch (XmlPullParserException e2) {
            printfError("Failed parserXml +" + e2);
            mWhitePkgs.clear();
            return false;
        } catch (Exception e3) {
            printfError("Failed parserXml +" + e3);
            mWhitePkgs.clear();
            return false;
        } finally {
            IoUtils.closeQuietly(fis);
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpAndUsageStatsPermission(mContext, TAG, pw)) {
            pw.println("VPS dump:");
            pw.println("showIMIEOneTipsLimit = " + this.showIMIEOneTipsLimit);
            pw.println("showIMIETwoTipsLimit = " + this.showIMIETwoTipsLimit);
            pw.println("mWhitePkgs = " + mWhitePkgs);
            int opti = 0;
            while (opti < args.length) {
                String opt = args[opti];
                opti++;
                if ("-p".equals(opt) || "package".equals(opt)) {
                    if (opti < args.length) {
                        String pkg = args[opti];
                        opti++;
                        pw.println("pkg = " + pkg);
                        pw.println("isDeletedSpecialSysPkg = " + isDeletedSpecialSysPkg(pkg));
                        pw.println("isBuildInThirdPartApp = " + isBuildInThirdPartApp(pkg));
                        pw.println("isTestApp = " + isTestApp(pkg));
                        pw.println("isVivoImeiPkg = " + isVivoImeiPkg(pkg));
                        if (opti < args.length) {
                            String perm = args[opti];
                            pw.println("checkConfigPermission = " + this.mVPC.checkConfigPermission(pkg, perm, 0));
                        }
                    }
                } else if ("-imei".equals(opt)) {
                    pw.println("vivoImeiSets = " + this.vivoImeiSets);
                } else if ("log".equals(opt)) {
                    configLogTag(pw, args, opti);
                }
            }
        }
    }

    private void configLogTag(PrintWriter pw, String[] args, int opti) {
        if (args.length <= opti) {
            if (pw != null) {
                pw.println("  Invalid argument!");
                return;
            }
            return;
        }
        String type = args[opti];
        String zone = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if ("enable".equals(type) || "disable".equals(type)) {
            if (args.length <= opti + 1) {
                if (pw != null) {
                    pw.println("  Invalid argument!");
                    return;
                }
                return;
            }
            zone = args[opti + 1];
        }
        boolean on = false;
        if ("disable".equals(type)) {
            on = false;
        } else if ("enable".equals(type)) {
            on = true;
        } else if ("list".equals(type)) {
            pw.print(" 1 . PRINTF_DEBUG = ");
            pw.println(PRINTF_DEBUG);
        }
        if ("1".equals(zone)) {
            PRINTF_DEBUG = on;
            pw.print("PRINTF_DEBUG = ");
            pw.println(PRINTF_DEBUG);
        }
    }

    private void recordDelete(String key, String packageName, long currentTime, String path, boolean isRuningForeground, int rson, int rt, int count) {
        if (this.lastPath.contains(key) && this.lastPkg.contains(packageName) && currentTime - this.lastTime < 200) {
            int i = count + 1;
            return;
        }
        int frontOrBackground = !isRuningForeground ? 1 : 0;
        VCD_VC_2(packageName, path, 0, frontOrBackground, rson, count);
        this.lastTime = currentTime;
        this.lastPath.clear();
        this.lastPath.add(key);
        this.lastPkg.clear();
        this.lastPkg.add(packageName);
    }

    private void VCD_VC_1(String pkg, String type, String isFg) {
        VivoCollectData mVCD = VivoCollectData.getInstance(mContext);
        try {
            long curTime = System.currentTimeMillis();
            HashMap<String, String> params = new HashMap<>();
            params.put("app", pkg);
            params.put("3party_change", type);
            params.put("type", isFg);
            if (mVCD != null && mVCD.getControlInfo("243")) {
                mVCD.writeData("243", "24332", curTime, curTime, 0L, 1, params);
            }
            EventTransfer.getInstance().singleEvent("243", "24332", curTime, 0L, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void VCD_VC_2(String pkg, String path, int rt, int front_or_background, int rson, int count) {
        VivoCollectData mVCD = VivoCollectData.getInstance(mContext);
        try {
            long curTime = System.currentTimeMillis();
            HashMap<String, String> params = new HashMap<>();
            params.put("app", pkg);
            params.put("path", path);
            params.put("rt", rt + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            params.put("front_or_background", front_or_background + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            params.put("rson", rson + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            params.put("count", count + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            if (mVCD != null && mVCD.getControlInfo("262")) {
                mVCD.writeData("262", "2621", curTime, curTime, 0L, 1, params, false);
            }
            EventTransfer mVcode = EventTransfer.getInstance();
            if (mVcode != null) {
                mVcode.singleEvent("262", "2621", curTime, 0L, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendDeleteFileBroadCast(String pkgName, String pkgLable, int callingUid, boolean isForeground) {
        synchronized (this.mIsPackageSyncsScheduled) {
            if (!this.mIsPackageSyncsScheduled.containsKey(pkgName)) {
                Message message = new Message();
                message.what = 5;
                message.obj = pkgName;
                Bundle bundle = new Bundle();
                bundle.putString("pkgName", pkgName);
                bundle.putString("pkgLable", pkgLable);
                bundle.putBoolean("isForeground", isForeground);
                bundle.putInt("callingUid", callingUid);
                message.setData(bundle);
                mVivoPermHandler.sendMessageDelayed(message, 1000L);
                this.mIsPackageSyncsScheduled.put(pkgName, message);
            } else {
                printfQuatitiesLog("sync for " + pkgLable + " already scheduled");
                Message message2 = this.mIsPackageSyncsScheduled.get(pkgName);
                if (mVivoPermHandler.hasMessages(5, message2.obj)) {
                    mVivoPermHandler.removeMessages(5, message2.obj);
                }
                Message message3 = new Message();
                message3.what = 5;
                message3.obj = pkgName;
                Bundle bundle2 = new Bundle();
                bundle2.putString("pkgName", pkgName);
                bundle2.putString("pkgLable", pkgLable);
                bundle2.putBoolean("isForeground", isForeground);
                bundle2.putInt("callingUid", callingUid);
                message3.setData(bundle2);
                mVivoPermHandler.sendMessageDelayed(message3, 1000L);
                this.mIsPackageSyncsScheduled.put(pkgName, message3);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doSendDeleteFileBroadCast(String pkgName, String pkgLable, int uid, boolean isForeground) {
        if (pkgName == null || pkgLable == null) {
            printfInfo("doSendDeleteFileBroadCast pkg is null, then return;");
            return;
        }
        synchronized (this.mIsPackageSyncsScheduled) {
            this.mIsPackageSyncsScheduled.remove(pkgName);
            printfInfo("send delete broadcast: pkg: " + pkgLable + ", uid: " + uid + ", isForeground: " + isForeground);
            Intent intent = new Intent("com.vivo.services.security.client.FileDelete");
            intent.setPackage(Constant.APP_GALLERY);
            Bundle bundle = new Bundle();
            bundle.putString("callingPkg", pkgLable);
            bundle.putBoolean("isForeground", isForeground);
            intent.putExtras(bundle);
            mContext.sendBroadcastAsUser(intent, UserHandle.getUserHandleForUid(uid), "com.vivo.daemonService.permission.safelyDelete");
        }
    }

    private boolean isGallerySupportRecycle(int uid) {
        PackageManagerInternal pmil = getPackageManagerInternalLocked();
        PackageInfo vivoGallery = pmil.getPackageInfo(Constant.APP_GALLERY, 0, 1000, UserHandle.getUserId(uid));
        if (vivoGallery == null) {
            return false;
        }
        if (vivoGallery.getLongVersionCode() != 6100400 && vivoGallery.getLongVersionCode() != 6100401 && (vivoGallery.getLongVersionCode() < 6100510 || vivoGallery.getLongVersionCode() == 6110000)) {
            return false;
        }
        return true;
    }

    private int getMediaProviderUid() {
        int i = this.mMediaStoreAuthorityAppId;
        if (i != -1) {
            return i;
        }
        PackageManagerInternal pmil = getPackageManagerInternalLocked();
        ProviderInfo provider = pmil.resolveContentProvider("media", 786432, UserHandle.getUserId(0));
        if (provider != null && provider.applicationInfo != null) {
            this.mMediaStoreAuthorityAppId = UserHandle.getAppId(provider.applicationInfo.uid);
        }
        return this.mMediaStoreAuthorityAppId;
    }
}