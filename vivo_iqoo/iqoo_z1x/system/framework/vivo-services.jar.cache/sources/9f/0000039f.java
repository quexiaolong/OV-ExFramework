package com.android.server.pm;

import android.app.ActivityManager;
import android.app.IVivoProcessObserver;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageParser;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.FtBuild;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.android.server.notification.VivoNotificationManagerServiceImpl;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.VivoPKMSDatabaseUtils;
import com.android.server.pm.VivoPKMSUtils;
import com.android.server.pm.VivoPmsImpl;
import com.android.server.pm.VivoVGCPKMSUtils;
import com.android.server.pm.dex.RMPms;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.parsing.PackageParser2;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.permission.BasePermission;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.android.server.wm.WindowManagerInternal;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.services.rms.GameOptManager;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.security.server.VivoPermissionUtils;
import com.vivo.services.superresolution.Constant;
import com.vivo.vcodetransbase.EventTransfer;
import dalvik.system.VMRuntime;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vgc.AbsVivoVgcManager;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoPmsImpl implements IVivoPms {
    private static final Map<String, String> BUILDIN_DIRS;
    private static final Map<String, String> BUILDIN_PKGS;
    private static final int DELETE_VIVO_SYS_APP = 1048576;
    private static final int[] EMPTY_INT_ARRAY;
    private static ArrayList<String> GAME_APPS = null;
    private static final String[] GMS_LAUNCHERS;
    private static final Set<String> GMS_PKGS;
    public static final ArrayList<String> INSTALL_BLACK_LIST;
    private static final int INSTALL_FROM_VIVO_CUSTOM_SYS_APP = 536870912;
    static final int MSG_FORCE_REPLACE_COTA_APK = 4;
    private static final int MSG_OPERATE_APK = 2;
    static final int MSG_RECOVERY_CUSTOM_SYSTEM_APP = 1;
    static final int MSG_SCAN_VGC_COTA_APK = 3;
    private static final int MSG_SEND_FIRST_BACK_TO_HIDE_BROADCAST = 1004;
    static final int MSG_SET_APP_BACK_TO_HIDE = 1003;
    private static final String PERSIST_SCANED_OPERATOR = "persist.vivo.scanedOperator";
    public static final String[] RUSSIA_IMSI;
    static final int SCAN_GOG_FRAMEWORK_APPS = 2333;
    public static final String[] SINGAPOR_IMSI;
    static final String TAG = "VivoPmsImpl";
    public static final int UNKNOWN_UID = -200;
    private static final String[] VIVO_LAUNCHERS;
    private static final List<String> VIVO_NOT_UNINSTALLABLE_APP;
    private static final List<String> VIVO_PROTECTED_BROADCAST;
    private static ScheduledExecutorService mScheduledService;
    public static ArrayList<String> mSpecialDeletedSysPkgs;
    boolean isOemScaned;
    private Context mContext;
    private String mCountryCode;
    private ArrayList<String> mDeletedBuiltIn3PartAppList;
    public ArrayList<String> mDeletedSysAppList;
    private DevicePolicyManager mDevicePolicyManager;
    private List<String> mFakeSystemFlagList;
    private Intent mHomeIntent;
    public final boolean mIsOverseas;
    private List<String> mNotUninstallableList;
    private final PermissionManagerServiceInternal mPermissionManager;
    private PackageManagerService mPms;
    private String mScanedOperator;
    public String[] mScanedOperatorArray;
    final boolean mSuptOem;
    private boolean mSystemReady;
    private ArrayList<VivoVGCPKMSUtils.VgcBlackWhitelistApps> mVgcBlackListApps;
    private final List<String> mVgcSysUninstallableList;
    private ArrayList<VivoVGCPKMSUtils.VgcBlackWhitelistApps> mVgcWhiteListApps;
    private VivoADBVerifyInstallManager mVivoADBVerifyInstallManager;
    private VivoPKMSCommonUtils mVivoPKMSCommonUtils;
    private VivoPKMSDatabaseUtils mVivoPKMSDatabaseUtils;
    private VivoPKMSUtils mVivoPKMSUtils;
    public VivoPmsImplHandler mVivoPmsImplHandler;
    private HandlerThread mVivoPmsImplHandlerThread;
    private VivoUninstallMgr mVivoUninstallMgr;
    private AbsVivoVgcManager mVivoVgcManager;
    private SimStateReceiver simStateReceiver;
    static final boolean IS_SUPPORT_SYS_APP_UNINSTALL = SystemProperties.get("ro.vivo.uninstall", "no").equals("yes");
    private static final boolean isGmsBuidIn = !Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(SystemProperties.get("ro.com.google.gmsversion", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
    public final HashMap<String, String> mSysCustomAppMap = new HashMap<>();
    private final HashMap<String, String> mSuportUninstallSysAppABPathMap = new HashMap<>();
    private final HashMap<String, VivoPKMSUtils.VSysAppInstallParam> mPendingVCustomAppInstallMap = new HashMap<>();
    private final IntentFilter mIntentFilter = new IntentFilter();
    private boolean isBattleModeOn = false;
    private int battleAppUid = -1;
    private final HashMap<String, String> mDoubleAppInstallMap = new HashMap<>();
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
    private final ArrayList<String> mDeletedVgcAppsList = new ArrayList<>();
    private final ArrayList<String> mSupportUninstallVgcAppsList = new ArrayList<>();
    private final HashMap<Integer, ScheduledFuture<?>> mScheduledJobMap = new HashMap<>();
    private ArrayMap<String, PackageInfo> mHybridPackageInfo = new ArrayMap<>();
    private ArrayMap<String, Hybridrules> mHybridrulesMap = new ArrayMap<>();
    private ComponentName bbkLauncher = new ComponentName("com.bbk.launcher2", Constant.ACTIVITY_LAUNCHER);
    private String mEnabledHomeActivity = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private IVivoProcessObserver mProcessObserver = new IVivoProcessObserver.Stub() { // from class: com.android.server.pm.VivoPmsImpl.3
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities, String packageName, String processName, ComponentName cpn) {
            int pkgHideState = VivoPmsImpl.this.getVHiddenApplicaiton(packageName, UserHandle.getUserId(uid));
            if (PackageManagerService.DEBUG_FOR_ALL) {
                VSlog.d(VivoPmsImpl.TAG, "PKMS onForegroundActivitiesChanged: pid=" + pid + ", uid=" + uid + ", foregroundActivities=" + foregroundActivities + " pkgname=" + packageName + " pkgHideState:" + pkgHideState);
            }
            String topPackage = cpn != null ? cpn.getPackageName() : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            VivoPmsImpl.this.updateHiddenPackageState(uid, packageName, pkgHideState, foregroundActivities, topPackage);
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes, String packageName, String processName) {
        }

        public void onProcessDied(int pid, int uid, String packageName, String processName) {
        }

        public void onProcessCreated(int pid, int uid, String packageName, String processName) {
        }
    };
    public ArrayList<String> mAllScanedOperatorApplist = new ArrayList<>();
    public ArrayMap<String, ArrayList<String>> imsiForOperatorMap = new ArrayMap<>();
    public ArrayMap<String, String> imsiForOperatorNameMap = new ArrayMap<>();
    public ArrayMap<String, String[]> contryForImsiMap = new ArrayMap<>();
    public ArrayList<String> allOperatorApkList = new ArrayList<>();

    static {
        ArraySet arraySet = new ArraySet();
        GMS_PKGS = arraySet;
        arraySet.add("com.android.vending");
        GMS_PKGS.add("com.google.android.gms");
        GMS_PKGS.add("com.google.android.googlequicksearchbox");
        ArrayMap arrayMap = new ArrayMap();
        BUILDIN_DIRS = arrayMap;
        arrayMap.put("GoogleServicesFramework", Environment.getProductDirectory() + "/priv-app/GoogleServicesFramework");
        BUILDIN_DIRS.put("GooglePartnerSetup", Environment.getProductDirectory() + "/priv-app/GooglePartnerSetup");
        BUILDIN_DIRS.put("GoogleContactsSyncAdapter", Environment.getProductDirectory() + "/app/GoogleContactsSyncAdapter");
        BUILDIN_DIRS.put("GoogleCalendarSyncAdapter", Environment.getProductDirectory() + "/app/GoogleCalendarSyncAdapter");
        BUILDIN_DIRS.put("GoogleLocationHistory", Environment.getProductDirectory() + "/app/GoogleRestore");
        BUILDIN_DIRS.put("AndroidPlatformServices", Environment.getProductDirectory() + "/priv-app/AndroidPlatformServices");
        ArrayMap arrayMap2 = new ArrayMap();
        BUILDIN_PKGS = arrayMap2;
        arrayMap2.put("com.google.android.gsf", "GoogleServicesFramework");
        BUILDIN_PKGS.put("com.google.android.partnersetup", "GooglePartnerSetup");
        BUILDIN_PKGS.put("com.google.android.syncadapters.contacts", "GoogleContactsSyncAdapter");
        BUILDIN_PKGS.put("com.google.android.syncadapters.calendar", "GoogleCalendarSyncAdapter");
        BUILDIN_PKGS.put("com.google.android.gms.location.history", "GoogleLocationHistory");
        BUILDIN_PKGS.put("com.google.android.gms.policy_sidecar_aps", "AndroidPlatformServices");
        EMPTY_INT_ARRAY = new int[0];
        ArrayList arrayList = new ArrayList();
        VIVO_NOT_UNINSTALLABLE_APP = arrayList;
        arrayList.add(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS);
        VIVO_NOT_UNINSTALLABLE_APP.add(FaceUIState.PKG_SYSTEMUI);
        VIVO_NOT_UNINSTALLABLE_APP.add("com.android.location.fused");
        VIVO_NOT_UNINSTALLABLE_APP.add("com.vivo.customtool");
        VIVO_NOT_UNINSTALLABLE_APP.add("com.vivo.cota");
        VIVO_NOT_UNINSTALLABLE_APP.add("com.vivo.pushservice");
        VIVO_NOT_UNINSTALLABLE_APP.add("com.vivo.daemonService");
        VIVO_NOT_UNINSTALLABLE_APP.add("co.sitic.pp");
        VIVO_NOT_UNINSTALLABLE_APP.add("com.rjio.slc");
        ArrayList arrayList2 = new ArrayList();
        VIVO_PROTECTED_BROADCAST = arrayList2;
        arrayList2.add("vivo.intent.action.CLEAR_PACKAGE_DATA");
        VIVO_PROTECTED_BROADCAST.add("com.vivo.abe.unifiedconfig.update.broadcast.acion");
        VIVO_PROTECTED_BROADCAST.add("com.vivo.daemonService.unifiedconfig.update_finish_broadcast");
        VIVO_PROTECTED_BROADCAST.add("EPM.EXCEPTION_NOTIFY");
        mScheduledService = Executors.newScheduledThreadPool(1);
        VIVO_LAUNCHERS = new String[]{"com.bbk.launcher2", "com.vivo.childrenmode", VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.bbk.scene.launcher.theme", "com.bbk.scene.tech", "com.vivo.simplelauncher"};
        GMS_LAUNCHERS = new String[]{"com.android.cts.deviceowner", "android.server.wm.app", "com.android.cts.deviceandprofileowner", "android.packageinstaller.admin.cts"};
        INSTALL_BLACK_LIST = new ArrayList<String>() { // from class: com.android.server.pm.VivoPmsImpl.2
            {
                add(FaceUIState.PKG_SYSTEMUI);
                add("com.vivo.upslide");
                add(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS);
                add("com.vivo.customtool");
                add("com.rjio.slc");
                add("com.vivo.cota");
                add("com.vivo.sdk.test");
                add("com.bbk.updater");
                add("com.android.bbkmusic");
                add("com.vivo.gamecube");
                add("com.vivo.voicewakeup");
                add("com.vivo.smartshot");
                add("com.vivo.smartanswer");
                add("com.android.wifisettings");
                add("com.android.vivo.tws.vivotws");
                add("com.bbk.launcher2");
                add("com.vivo.appfilter");
                add("com.vivo.permissionmanager");
                add("com.android.packageinstaller");
                add("com.google.android.gsf.login");
                add("com.baidu.input_bbk.service");
            }
        };
        ArrayList<String> arrayList3 = new ArrayList<>();
        mSpecialDeletedSysPkgs = arrayList3;
        arrayList3.add("com.vivo.game");
        mSpecialDeletedSysPkgs.add("com.chaozh.iReader");
        ArrayList<String> arrayList4 = new ArrayList<>();
        GAME_APPS = arrayList4;
        arrayList4.add("com.tencent.tmgp.sgame");
        GAME_APPS.add("com.tencent.tmgp.sgamece");
        GAME_APPS.add("com.tencent.tmgp.pubgmhd");
        SINGAPOR_IMSI = new String[]{"52501", "52502", "52503", "52504", "52505"};
        RUSSIA_IMSI = new String[]{"25001"};
    }

    /* loaded from: classes.dex */
    class Hybridrules {
        String mAction;
        String mCallingPkgname;
        Bundle mExtras;
        String mPlatfromPkgname;
        String mRealActitityname;
        String mXcxPkgname;

        Hybridrules(String mCallingPkgname, String mPlatfromPkgname, String mXcxPkgname, String mRealActitityname) {
            this.mAction = null;
            this.mExtras = null;
            this.mCallingPkgname = mCallingPkgname;
            this.mPlatfromPkgname = mPlatfromPkgname;
            this.mXcxPkgname = mXcxPkgname;
            this.mRealActitityname = mRealActitityname;
        }

        Hybridrules(String mCallingPkgname, String mPlatfromPkgname, String mXcxPkgname, String mRealActitityname, String action, Bundle extras) {
            this.mAction = null;
            this.mExtras = null;
            this.mCallingPkgname = mCallingPkgname;
            this.mPlatfromPkgname = mPlatfromPkgname;
            this.mXcxPkgname = mXcxPkgname;
            this.mRealActitityname = mRealActitityname;
            this.mAction = action;
            this.mExtras = extras;
        }
    }

    public VivoPmsImpl(PackageManagerService pms, Context context, PermissionManagerServiceInternal permissionManager) {
        this.mDeletedBuiltIn3PartAppList = new ArrayList<>();
        this.mDeletedSysAppList = new ArrayList<>();
        boolean z = false;
        this.mNotUninstallableList = new ArrayList();
        this.mVivoVgcManager = null;
        if (pms == null) {
            VSlog.i(TAG, "container is " + pms);
        }
        this.mPms = pms;
        this.mContext = context;
        this.mPermissionManager = permissionManager;
        HandlerThread handlerThread = new HandlerThread("VivoPmsImplHandlerThread");
        this.mVivoPmsImplHandlerThread = handlerThread;
        handlerThread.start();
        this.mVivoPmsImplHandler = new VivoPmsImplHandler(this.mVivoPmsImplHandlerThread.getLooper());
        VivoADBVerifyInstallManager vivoADBVerifyInstallManager = VivoADBVerifyInstallManager.getInstance();
        this.mVivoADBVerifyInstallManager = vivoADBVerifyInstallManager;
        vivoADBVerifyInstallManager.init(this.mContext, false);
        this.mVivoPKMSDatabaseUtils = new VivoPKMSDatabaseUtils(this.mPms, this.mContext);
        this.mVivoPKMSUtils = new VivoPKMSUtils(this.mContext, this.mPms, this.mVivoPKMSDatabaseUtils);
        this.mVivoUninstallMgr = VivoUninstallMgr.getInstance();
        this.mDeletedBuiltIn3PartAppList = VivoPKMSUtils.getDeletedPkgListFromLocalFile("data/system/v_deleted_built_in_app.xml");
        this.mIsOverseas = "yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no"));
        this.mSuptOem = "yes".equals(SystemProperties.get("ro.vivo.oem.support", "no"));
        this.isOemScaned = ("yes".equals(SystemProperties.get("persist.vivo.oem.scaned", "no")) || isGmsBuidIn) ? true : true;
        this.mDeletedSysAppList = this.mVivoPKMSUtils.getDeletedSysPkgListFromLocalFile();
        SystemProperties.set("persist.vivo.supDowngrade", "yes");
        this.mNotUninstallableList = VivoVGCPKMSUtils.getNotUninstallableList();
        VivoVGCPKMSUtils.getDeletedVgcAppsList(this.mDeletedVgcAppsList);
        this.mVgcBlackListApps = VivoVGCPKMSUtils.getVgcBlackListApps();
        this.mVgcWhiteListApps = VivoVGCPKMSUtils.getVgcWhiteListApps();
        if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
            this.mVivoVgcManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoVgcManager();
        }
        this.mFakeSystemFlagList = VivoVGCPKMSUtils.getFakeSystemFlagList();
        this.mVgcSysUninstallableList = VivoVGCPKMSUtils.getSysUninstallableList();
        this.mIntentFilter.addAction("android.intent.action.VIEW");
        this.mIntentFilter.addAction("android.intent.action.SENDTO");
        this.mIntentFilter.addAction("android.intent.action.SEND");
        this.mIntentFilter.addAction("android.intent.action.EDIT");
        this.mIntentFilter.addAction("android.intent.action.INSERT_OR_EDIT");
        this.mIntentFilter.addAction("android.intent.action.SEND_MULTIPLE");
        this.mIntentFilter.addAction("android.intent.action.PICK");
        this.mIntentFilter.addAction("android.intent.action.GET_CONTENT");
        this.mIntentFilter.addAction("com.android.camera.action.CROP");
        this.mIntentFilter.addAction("com.linecorp.b612.android.VIDEO_PROFILE");
        this.mIntentFilter.addAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        initCountryForOperator();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class VivoPmsImplHandler extends Handler {
        public VivoPmsImplHandler(Looper looper) {
            super(looper);
        }

        /* JADX WARN: Code restructure failed: missing block: B:55:0x0133, code lost:
            if (r0.equals("ABSENT") == false) goto L99;
         */
        @Override // android.os.Handler
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        public void handleMessage(android.os.Message r11) {
            /*
                Method dump skipped, instructions count: 533
                To view this dump change 'Code comments level' option to 'DEBUG'
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPmsImpl.VivoPmsImplHandler.handleMessage(android.os.Message):void");
        }
    }

    public void dummy() {
    }

    public void systemReady() {
        this.mSystemReady = true;
        this.mVivoADBVerifyInstallManager.systemReady();
        this.mVivoPKMSDatabaseUtils.systemReady();
        this.mVivoPKMSUtils.systemReady();
        this.mVivoUninstallMgr.systemReady(this.mSysCustomAppMap);
        registerReceivers();
        if (!this.mIsOverseas) {
            IntentFilter userSwitchFilter = new IntentFilter("android.intent.action.USER_SWITCHED");
            this.mContext.registerReceiver(new AnonymousClass1(), userSwitchFilter);
        }
        resetVHiddenApplicaiton();
        registerReceiverInPms();
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        setEmmPackageCacheIfNeed();
        RMPms.getInstance().initialize();
        ArrayList<String> builtIn3PartAppList = new ArrayList<>();
        builtIn3PartAppList.addAll(this.mVivoPKMSUtils.getBuiltIn3PartAppPkgNameList());
        builtIn3PartAppList.addAll(this.mSupportUninstallVgcAppsList);
        this.mPms.mSettings.setBuiltIn3PartApkNameList(builtIn3PartAppList);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.pm.VivoPmsImpl$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        public /* synthetic */ void lambda$onReceive$0$VivoPmsImpl$1() {
            VivoPmsImpl.this.mVivoPKMSDatabaseUtils.updatemLockSystemHomeState(VivoPmsImpl.this.mContext);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            VivoPmsImpl.this.mVivoPmsImplHandler.post(new Runnable() { // from class: com.android.server.pm.-$$Lambda$VivoPmsImpl$1$xKnkpBbdQ1laNqyWYzUt16QzSVA
                @Override // java.lang.Runnable
                public final void run() {
                    VivoPmsImpl.AnonymousClass1.this.lambda$onReceive$0$VivoPmsImpl$1();
                }
            });
        }
    }

    public void setWakelockWhiteList(AndroidPackage pkg) {
        if (pkg != null) {
            if (Constant.APP_WEIXIN.equals(pkg.getPackageName()) || "com.tencent.mobileqq".equals(pkg.getPackageName()) || "com.iqoo.engineermode".equals(pkg.getPackageName())) {
                VSlog.d(TAG, "setWakelockWhiteList pkg = " + pkg + ", uid = " + pkg.getUid());
                PowerManagerInternal powerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
                if (powerManagerInternal != null) {
                    powerManagerInternal.setWakeLockWhiteUid(pkg.getUid());
                } else {
                    VSlog.d(TAG, "setWakelockWhiteList failed because powerManagerInternal is null");
                }
            }
        }
    }

    public boolean getHomeRestrictionEnabled() {
        return this.mVivoPKMSDatabaseUtils.getHomeRestrictionEnabled();
    }

    public void scheduleReportEPM(int type, Bundle reportInfo) {
        if (FtBuild.getTierLevel() == 0) {
            if (type != 14) {
                if (type == 39) {
                    VivoPKMSReportMgr.getInstance().scheduleReport(type, reportInfo);
                    return;
                }
                return;
            }
            int returnCode = reportInfo.getInt("returnCode", 1);
            if (returnCode != 1) {
                VivoPKMSReportMgr.getInstance().scheduleReport(type, reportInfo);
            }
        }
    }

    public boolean interceptUninstall(int callingUid, String packageName, IPackageDeleteObserver2 observer, int userId) {
        if (interceptUninstallWhiteBlackList(packageName, observer, userId)) {
            return true;
        }
        PackageSetting ps = (PackageSetting) this.mPms.mSettings.mPackages.get(packageName);
        if (callingUid == 2000 && ps != null && ps.isSystem() && isNotUninstallable(packageName, userId)) {
            if (observer != null) {
                try {
                    observer.onPackageDeleted(packageName, -3, (String) null);
                } catch (RemoteException e) {
                    VSlog.i(TAG, "Observer no longer exists.");
                }
            }
            VSlog.w(TAG, "delete " + packageName + " not alllow!");
            return true;
        }
        if (callingUid != 0 && callingUid != 2000 && observer != null && Arrays.asList(VivoPKMSUtils.CANNOT_UNINSTALL_APPS).contains(packageName)) {
            try {
                String callerPkg = this.mPms.getNameForUid(callingUid);
                if ("com.google.android.packageinstaller".equals(callerPkg)) {
                    observer.onPackageDeleted(packageName, -3, (String) null);
                    VSlog.w(TAG, "interceptUninstall delete " + packageName + " not alllow! If delete, the data will lost.");
                    return true;
                }
            } catch (RemoteException e2) {
                VSlog.i(TAG, "Observer no longer exists.");
            }
        }
        return false;
    }

    public boolean isNotUninstallable(String packageName, int userId) {
        return userId == 0 && (this.mNotUninstallableList.contains(packageName) || VIVO_NOT_UNINSTALLABLE_APP.contains(packageName) || this.mVivoPKMSUtils.isNotSupportAdbUninstall(packageName));
    }

    /* JADX WARN: Removed duplicated region for block: B:52:0x012f  */
    /* JADX WARN: Removed duplicated region for block: B:53:0x0135  */
    /* JADX WARN: Removed duplicated region for block: B:56:0x01a8  */
    /* JADX WARN: Removed duplicated region for block: B:57:0x01dd  */
    /* JADX WARN: Removed duplicated region for block: B:73:0x01e6 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void vcdInstallResource(com.android.server.pm.PackageManagerService.VerificationInfo r28, java.lang.String r29, int r30, com.android.server.pm.PackageManagerService.PackageInstalledInfo r31, java.lang.String r32, boolean r33, android.os.Bundle r34, boolean r35, boolean r36, boolean r37, int r38, int[] r39, int[] r40, int r41) {
        /*
            Method dump skipped, instructions count: 630
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPmsImpl.vcdInstallResource(com.android.server.pm.PackageManagerService$VerificationInfo, java.lang.String, int, com.android.server.pm.PackageManagerService$PackageInstalledInfo, java.lang.String, boolean, android.os.Bundle, boolean, boolean, boolean, int, int[], int[], int):void");
    }

    private long getAppApkSize(String codePath) {
        File[] files;
        File codefile = new File(codePath);
        if (codefile.exists()) {
            if (codefile.isFile() && codefile.getName().toLowerCase().endsWith(".apk")) {
                return codefile.length();
            }
            if (codefile.isDirectory() && (files = codefile.listFiles()) != null) {
                long apksSize = 0;
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getName().toLowerCase().endsWith(".apk")) {
                        apksSize += files[i].length();
                    }
                }
                return apksSize;
            }
            return 0L;
        }
        return 0L;
    }

    public void sendPackageAddedForNewUsers(final String packageName, boolean sendBootCompleted, final boolean includeStopped, int appId, Bundle extras, final int[] userIds, int[] instantUserIds, int dataLoaderType) {
        if (ArrayUtils.isEmpty(userIds) && ArrayUtils.isEmpty(instantUserIds)) {
            return;
        }
        Bundle finalExtras = new Bundle(extras);
        int uid = UserHandle.getUid(ArrayUtils.isEmpty(userIds) ? instantUserIds[0] : userIds[0], appId);
        finalExtras.putInt("android.intent.extra.UID", uid);
        finalExtras.putInt("android.content.pm.extra.DATA_LOADER_TYPE", dataLoaderType);
        this.mPms.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageName, finalExtras, 0, (String) null, (IIntentReceiver) null, userIds, instantUserIds, (SparseArray) null);
        if (sendBootCompleted && !ArrayUtils.isEmpty(userIds)) {
            this.mPms.mHandler.post(new Runnable() { // from class: com.android.server.pm.-$$Lambda$VivoPmsImpl$Ro1XcWlvC_6ZYpbeWuqm5x8m0_0
                @Override // java.lang.Runnable
                public final void run() {
                    VivoPmsImpl.this.lambda$sendPackageAddedForNewUsers$0$VivoPmsImpl(userIds, packageName, includeStopped);
                }
            });
        }
    }

    public /* synthetic */ void lambda$sendPackageAddedForNewUsers$0$VivoPmsImpl(int[] userIds, String packageName, boolean includeStopped) {
        for (int userId : userIds) {
            this.mPms.sendVivoBootCompletedBroadcastToSystemApp(packageName, includeStopped, userId);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:21:0x0058  */
    /* JADX WARN: Removed duplicated region for block: B:33:0x0079  */
    /* JADX WARN: Removed duplicated region for block: B:36:0x0082  */
    /* JADX WARN: Removed duplicated region for block: B:62:0x0199  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void vcdUninstallResource(int r30, java.lang.String r31, com.android.server.pm.PackageSetting r32, android.content.pm.IPackageDeleteObserver2 r33, int r34, int r35, int r36) {
        /*
            Method dump skipped, instructions count: 420
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPmsImpl.vcdUninstallResource(int, java.lang.String, com.android.server.pm.PackageSetting, android.content.pm.IPackageDeleteObserver2, int, int, int):void");
    }

    public ResolveInfo getDefaultSetingOptimization(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, int userId) {
        if (PackageManagerService.DEBUG_PREFERRED) {
            VSlog.i(TAG, "chooseBestActivity_ check  intent:" + intent + " resolvedType:" + resolvedType + " flags:" + flags);
        }
        if (!this.mIsOverseas && this.mVivoPKMSUtils.isNeedIngnoreResolveActivity(intent, resolvedType)) {
            ResolveInfo info = this.mVivoPKMSUtils.findSetPreferredActivityOrSystemActivity(intent, resolvedType, flags, query, userId);
            VSlog.i(TAG, "check preferrOrSystem app.  " + info);
            if (info != null) {
                return info;
            }
            return null;
        } else if (PackageManagerService.DEBUG_PREFERRED) {
            VSlog.d(TAG, "Donot filter " + intent);
            return null;
        } else {
            return null;
        }
    }

    public boolean IsInVivoBlackapplist(File scanDir, File file) {
        ArrayList<VivoPKMSUtils.VivoBlacklistApps> vivoBlackApplist;
        if (scanDir.getPath() != null && file.getName() != null && (vivoBlackApplist = this.mVivoPKMSUtils.getVivoBlackAppList()) != null) {
            Iterator<VivoPKMSUtils.VivoBlacklistApps> it = vivoBlackApplist.iterator();
            while (it.hasNext()) {
                VivoPKMSUtils.VivoBlacklistApps tempBlackApp = it.next();
                if (scanDir.getPath().equals(tempBlackApp.mBlacklistAppDir) && file.getName().equals(tempBlackApp.mInnerFileName)) {
                    VSlog.i(TAG, "It is in blackapp.list, We do not need to scan:" + scanDir.getPath() + " filename:" + file.getName());
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean isNeedSkipScan(File scanDir, File file) {
        boolean z;
        if ((!IsInVivoBlackapplist(scanDir, file) || isNeedForVgc(scanDir, file)) && !interceptInsertSimScanAPK(file)) {
            if (!this.mIsOverseas && this.mSuptOem && !(z = this.isOemScaned)) {
                boolean containsKey = z | this.mPms.mSettings.mPackages.containsKey("com.google.android.gms");
                this.isOemScaned = containsKey;
                if (!containsKey) {
                    for (String s : BUILDIN_DIRS.keySet()) {
                        if (file.getName().contains(s)) {
                            BUILDIN_DIRS.put(s, file.getAbsolutePath());
                            return true;
                        }
                    }
                } else {
                    SystemProperties.set("persist.vivo.oem.scaned", "yes");
                }
            }
            return isNeedIgnoreForVgc(scanDir, file) || file.getPath().startsWith("/oem/app/JakartaBaca");
        }
        return true;
    }

    public void skipScanDeletedBuiltIn3PartApp(ParsedPackage pkg) throws PackageManagerException {
        if (!this.mSystemReady) {
            String pkgPath = pkg.getCodePath();
            ArrayList<String> builtIn3PartAppPkgNameList = this.mVivoPKMSUtils.getBuiltIn3PartAppPkgNameList();
            if (pkgPath != null && isBuildInThirdAppPath(pkgPath) && !builtIn3PartAppPkgNameList.contains(pkg.getPackageName())) {
                builtIn3PartAppPkgNameList.add(pkg.getPackageName());
            }
            if (builtIn3PartAppPkgNameList != null && builtIn3PartAppPkgNameList.size() > 0 && builtIn3PartAppPkgNameList.contains(pkg.getPackageName())) {
                VSlog.i(TAG, "scan_chekc " + pkg + " path:" + pkgPath);
                if (pkgPath != null && isBuildInThirdAppPath(pkgPath)) {
                    VSlog.i(TAG, "systemReady...  DeletedList " + this.mDeletedBuiltIn3PartAppList + " apkList:" + builtIn3PartAppPkgNameList);
                    if (this.mDeletedBuiltIn3PartAppList.contains(pkg.getPackageName())) {
                        throw new PackageManagerException((int) UNKNOWN_UID, pkg.getPackageName() + ", This app has uninstalled from data/vivo-apps, so not install! ####");
                    }
                }
            }
        }
    }

    private boolean isBuildInThirdAppPath(String pkgPath) {
        return pkgPath.contains("/data/vivo-apps") || pkgPath.contains("/data/preload");
    }

    public void addDeletedBuiltIn3PartApp(int returnCode, String packageName) {
        ArrayList<String> builtIn3PartAppPkgNameList;
        if (returnCode == 1 && !isAnyInstalled(packageName) && (builtIn3PartAppPkgNameList = this.mVivoPKMSUtils.getBuiltIn3PartAppPkgNameList()) != null && builtIn3PartAppPkgNameList.size() > 0 && builtIn3PartAppPkgNameList.contains(packageName)) {
            VSlog.i(TAG, "DelList " + this.mDeletedBuiltIn3PartAppList + " apkList:" + builtIn3PartAppPkgNameList);
            synchronized (this.mDeletedBuiltIn3PartAppList) {
                if (!this.mDeletedBuiltIn3PartAppList.contains(packageName)) {
                    this.mDeletedBuiltIn3PartAppList.add(packageName);
                    this.mVivoPKMSUtils.writeDeletedPkgToLocalFile("data/system/v_deleted_built_in_app.xml", this.mDeletedBuiltIn3PartAppList, packageName, false);
                }
            }
        }
    }

    public boolean checkDeviceInstallShouldBeSilent(PackageManagerService.ActiveInstallSession activeInstallSession, boolean isEng) {
        return this.mVivoPKMSUtils.checkDeviceInstallShouldBeSilent(activeInstallSession, isEng, this.mIsOverseas);
    }

    public void checkInstallingApkIsShouldBeForbid(String pkgName, boolean systemApp, boolean replace, PackageManagerService.InstallArgs args, ParsedPackage pkg) throws PackageManagerService.PrepareFailure {
        if (!this.mIsOverseas) {
            try {
                if (this.mVivoPKMSUtils.checkInstallingApkIsShouldBeForbid(pkgName, systemApp, replace, args, pkg)) {
                    VSlog.i(TAG, "checkInstallingApkIsShouldBeForbid " + pkgName + " from " + args.verificationInfo.installerUid);
                    throw new PackageManagerService.PrepareFailure(-201, pkgName + " install failed.");
                }
            } catch (Exception e) {
                VSlog.w(TAG, "check installing apk, catch exception, " + e.toString());
            }
        }
    }

    public void scanVivoSystemCustom(int scanFlags, PackageParser2 packageParser, ExecutorService executorService) {
        scanSystemCustomApps(scanFlags, packageParser, executorService);
        scanDirTrancedLIforOem(scanFlags, packageParser, executorService);
        scanDirForVgcSystem(scanFlags, packageParser, executorService);
    }

    private void scanDirTrancedLIforOem(int scanFlags, PackageParser2 packageParser, ExecutorService executorService) {
        if (this.mPms.mSuptOem) {
            File gOemFile = new File("/oem/priv-app");
            try {
                if (PackageManagerService.DEBUG) {
                    VSlog.d(TAG, "gOemFile" + gOemFile + " " + gOemFile.exists());
                }
                if (gOemFile.exists()) {
                    this.mPms.scanDirTracedLI(gOemFile, 16, 65536 | scanFlags | Dataspace.STANDARD_BT601_625, 0L, packageParser, executorService);
                }
            } catch (Exception e) {
            }
        }
    }

    public void scanSystemCustomApps(int scanFlags, PackageParser2 packageParser, ExecutorService executorService) {
        if (IS_SUPPORT_SYS_APP_UNINSTALL) {
            File sysCustomPriAppDir = new File(Environment.getRootDirectory(), "custom/priv-app");
            PackageManagerService packageManagerService = this.mPms;
            packageManagerService.scanDirTracedLI(sysCustomPriAppDir, packageManagerService.mDefParseFlags | 16, scanFlags | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT601_625, 0L, packageParser, executorService);
            File sysCustomAppDir = new File(Environment.getRootDirectory(), "custom/app");
            PackageManagerService packageManagerService2 = this.mPms;
            packageManagerService2.scanDirTracedLI(sysCustomAppDir, packageManagerService2.mDefParseFlags | 16, scanFlags | Dataspace.STANDARD_BT709, 0L, packageParser, executorService);
        }
    }

    private boolean isSystemSignedLPr(PackageParser.SigningDetails signingDetails) {
        try {
            AndroidPackage systemPkg = (AndroidPackage) this.mPms.mPackages.get(VivoPermissionUtils.OS_PKG);
            return systemPkg.getSigningDetails().signaturesMatchExactly(signingDetails);
        } catch (Exception e) {
            return false;
        }
    }

    public void addFlagsForSystemCustomApp(ParsedPackage pkg) {
        if (IS_SUPPORT_SYS_APP_UNINSTALL) {
            synchronized (this.mPms.mLock) {
                String codePathTemp = pkg.getCodePath();
                VSlog.i(TAG, "##scan01 " + codePathTemp + " " + pkg.getPackageName() + " systemCustomMap: " + this.mSysCustomAppMap + " DeletedSysApp:" + this.mDeletedSysAppList);
                if (this.mSysCustomAppMap.containsKey(pkg.getPackageName()) && codePathTemp != null && codePathTemp.contains("data/app") && isSystemSignedLPr(pkg.getSigningDetails())) {
                    PackageSetting disabledPs = this.mPms.mSettings.getDisabledSystemPkgLPr(pkg.getPackageName());
                    VSlog.i(TAG, "check pkg " + pkg.getPackageName() + " disabledPs:" + disabledPs);
                    if (disabledPs == null && !pkg.isSystem()) {
                        VSlog.w(TAG, "add flag for " + pkg.getPackageName());
                        pkg.setSystem(true);
                        String pkgPath = this.mSysCustomAppMap.get(pkg.getPackageName());
                        if (pkgPath != null && pkgPath.contains("priv-app")) {
                            VSlog.w(TAG, "add priv-flag for " + pkg.getPackageName());
                            pkg.setPrivileged(true);
                        }
                    }
                }
            }
        }
    }

    public boolean dealWithSpecialPackageInstall(PackageManagerService.ActiveInstallSession activeInstallSession) {
        return interceptPackageInstall(activeInstallSession) | installSystemCustomApp(activeInstallSession);
    }

    private boolean interceptPackageInstall(PackageManagerService.ActiveInstallSession activeInstallSession) {
        int originatingUid;
        String packageName = activeInstallSession.getPackageName();
        String installerPackageName = activeInstallSession.getInstallSource().installerPackageName;
        IPackageInstallObserver2 observer = activeInstallSession.getObserver();
        int callingUid = activeInstallSession.getInstallerUid();
        PackageInstaller.SessionParams sessionParams = activeInstallSession.getSessionParams();
        int originatingUid2 = sessionParams != null ? sessionParams.originatingUid : UNKNOWN_UID;
        if (originatingUid2 != -1) {
            originatingUid = originatingUid2;
        } else {
            originatingUid = -200;
        }
        UserHandle user = activeInstallSession.getUser();
        int installerUid = activeInstallSession.getInstallerUid();
        boolean fromAdb = (sessionParams.installFlags & 32) != 0;
        boolean isInterceptWithBlackList = interceptPackageInstallWithBlackList(packageName, installerPackageName, observer, callingUid, originatingUid);
        boolean isInterceptWithTrustedAPPStore = interceptPackageInstallWithTrustedAPPStore(packageName, installerPackageName, observer, user.getIdentifier(), installerUid, sessionParams.originatingUid, fromAdb);
        boolean isInterceptWithVivoCustomWhiteBlackList = interceptInstallWithVivoCustomWhiteBlackList(packageName, installerPackageName, observer, user.getIdentifier());
        boolean isIntercept = isInterceptWithBlackList | isInterceptWithTrustedAPPStore | isInterceptWithVivoCustomWhiteBlackList;
        return isIntercept;
    }

    private boolean interceptPackageInstallWithBlackList(String packageName, String installerPackageName, IPackageInstallObserver2 observer, int callingUid, int originatingUid) {
        if (callingUid == 0 || callingUid == 1000) {
            return false;
        }
        if (this.mIsOverseas || !"com.bbk.appstore".equals(installerPackageName)) {
            if (this.mIsOverseas && "com.vivo.appstore".equals(installerPackageName)) {
                return false;
            }
            int vMessageUid = this.mPms.getPackageUid("com.sie.mp", 0, UserHandle.getUserId(originatingUid));
            if (vMessageUid == originatingUid || "com.sie.mp".equals(installerPackageName)) {
                return false;
            }
            int packageNameUid = this.mPms.getPackageUid(packageName, 0, UserHandle.getUserId(originatingUid));
            VSlog.e(TAG, " interceptPackageInstallWithBlackList packageName:" + packageName + " installerPackageName:" + installerPackageName + " originatingUid:" + originatingUid + " packageNameUid:" + packageNameUid);
            if (packageNameUid == originatingUid || (packageName != null && packageName.equals(installerPackageName))) {
                return false;
            }
            ArrayList<String> mServerInstallBlackList = this.mVivoPKMSUtils.getServerInstallBlackList();
            if (mServerInstallBlackList != null && !mServerInstallBlackList.isEmpty()) {
                if (mServerInstallBlackList.contains(packageName)) {
                    callOnPackageInstalled(packageName, installerPackageName, observer, callingUid, -110);
                    return true;
                }
            } else if (INSTALL_BLACK_LIST.contains(packageName)) {
                callOnPackageInstalled(packageName, installerPackageName, observer, callingUid, -110);
                return true;
            }
            return false;
        }
        return false;
    }

    private void callOnPackageInstalled(String packageName, String installerPackageName, IPackageInstallObserver2 observer, int callingUid, int interceptReason) {
        if (observer != null) {
            try {
                observer.onPackageInstalled(packageName, interceptReason, (String) null, (Bundle) null);
            } catch (Exception e) {
                VSlog.e(TAG, " callOnPackageInstalled failed!!!" + e);
            }
        }
        VSlog.i(TAG, "This install is be intercepted!!!  packageName :" + packageName + " installerPackageName:" + installerPackageName + " interceptReason:" + interceptReason + " callingUid:" + callingUid);
    }

    private boolean installSystemCustomApp(PackageManagerService.ActiveInstallSession activeInstallSession) {
        String packageName = activeInstallSession.getPackageName();
        PackageInstaller.SessionParams sessionParams = activeInstallSession.getSessionParams();
        IPackageInstallObserver2 observer = activeInstallSession.getObserver();
        int installerUid = activeInstallSession.getInstallerUid();
        PackageManagerService.VerificationInfo verificationInfo = new PackageManagerService.VerificationInfo(sessionParams.originatingUri, sessionParams.referrerUri, sessionParams.originatingUid, installerUid);
        if (IS_SUPPORT_SYS_APP_UNINSTALL && this.mSysCustomAppMap.containsKey(packageName) && (sessionParams.installFlags & INSTALL_FROM_VIVO_CUSTOM_SYS_APP) != 0) {
            Message msg = this.mVivoPmsImplHandler.obtainMessage(1);
            VivoPKMSUtils.VSysAppInstallParam vParams = new VivoPKMSUtils.VSysAppInstallParam(packageName, observer, sessionParams.installFlags, -100, verificationInfo);
            msg.obj = vParams;
            this.mVivoPmsImplHandler.sendMessage(msg);
            VSlog.i(TAG, "recovery " + packageName + " from originatingUri:" + sessionParams.originatingUri + " installFlags:" + sessionParams.installFlags);
            return true;
        }
        if (FtBuild.getTierLevel() == 0) {
            synchronized (this.mPms.mLock) {
                try {
                    try {
                        if (!this.mIsOverseas && this.mSuptOem && !this.mPms.mPackages.containsKey(packageName)) {
                            if (BUILDIN_PKGS.containsKey(packageName) && !this.isOemScaned) {
                                File file = new File(BUILDIN_DIRS.get(BUILDIN_PKGS.get(packageName)), BUILDIN_PKGS.get(packageName) + ".apk");
                                if (file.exists()) {
                                    Message msg2 = this.mVivoPmsImplHandler.obtainMessage(SCAN_GOG_FRAMEWORK_APPS);
                                    VivoPKMSUtils.VSysAppInstallParam vParams2 = new VivoPKMSUtils.VSysAppInstallParam(packageName, observer, sessionParams.installFlags, -100, verificationInfo);
                                    msg2.obj = vParams2;
                                    msg2.arg1 = SCAN_GOG_FRAMEWORK_APPS;
                                    this.mVivoPmsImplHandler.sendMessage(msg2);
                                    return true;
                                }
                                try {
                                    return false;
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            }
                        }
                        if (!this.mIsOverseas && this.mSuptOem && !this.mPms.mPackages.containsKey(packageName) && (("com.google.android.gms".equals(packageName) || "com.google.android.gm".equals(packageName) || "com.android.vending".equals(packageName)) && !this.isOemScaned)) {
                            this.mVivoPmsImplHandler.sendEmptyMessage(SCAN_GOG_FRAMEWORK_APPS);
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:80:0x00b1 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean interceptPackageInstallWithTrustedAPPStore(java.lang.String r17, java.lang.String r18, android.content.pm.IPackageInstallObserver2 r19, int r20, int r21, int r22, boolean r23) {
        /*
            Method dump skipped, instructions count: 399
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPmsImpl.interceptPackageInstallWithTrustedAPPStore(java.lang.String, java.lang.String, android.content.pm.IPackageInstallObserver2, int, int, int, boolean):boolean");
    }

    /* JADX WARN: Removed duplicated region for block: B:32:0x00c6  */
    /* JADX WARN: Removed duplicated region for block: B:40:0x010a  */
    /* JADX WARN: Removed duplicated region for block: B:50:0x011b  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean interceptInstallWithVivoCustomWhiteBlackList(java.lang.String r17, java.lang.String r18, android.content.pm.IPackageInstallObserver2 r19, int r20) {
        /*
            Method dump skipped, instructions count: 294
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPmsImpl.interceptInstallWithVivoCustomWhiteBlackList(java.lang.String, java.lang.String, android.content.pm.IPackageInstallObserver2, int):boolean");
    }

    private boolean interceptUninstallWhiteBlackList(String packageName, IPackageDeleteObserver2 observer, int userId) {
        if (this.mDevicePolicyManager.getCustomType() <= 0) {
            return false;
        }
        boolean goOn = true;
        long callingId = Binder.clearCallingIdentity();
        try {
            int policy = this.mDevicePolicyManager.getRestrictionPolicy(null, 104, userId);
            List<String> blist = this.mDevicePolicyManager.getRestrictionInfoList(null, 1103, userId);
            List<String> wlist = this.mDevicePolicyManager.getRestrictionInfoList(null, 1104, userId);
            Binder.restoreCallingIdentity(callingId);
            try {
                if (policy == 3) {
                    if (blist != null && blist.contains(packageName)) {
                        goOn = false;
                    }
                } else if (policy == 4 && wlist != null && !wlist.contains(packageName)) {
                    goOn = false;
                }
                if (!goOn) {
                    VSlog.i(TAG, "interceptUninstallWhiteBlackList delete " + packageName + " not alllow!");
                    if (observer != null) {
                        observer.onPackageDeleted(packageName, -3, (String) null);
                        return true;
                    }
                    return true;
                }
            } catch (RemoteException e) {
                VSlog.i(TAG, "Observer no longer exists.");
            }
            return false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingId);
            throw th;
        }
    }

    private boolean isSystemPackageAndAlreadyInstalled(String packageName, int userId) {
        if (!TextUtils.isEmpty(packageName)) {
            long ident = Binder.clearCallingIdentity();
            try {
                PackageInfo packageInfo = this.mPms.getPackageInfo(packageName, 16384, userId);
                if (packageInfo != null) {
                    ApplicationInfo appInfo = packageInfo.applicationInfo;
                    if (appInfo != null) {
                        boolean isSystemApp = false;
                        if ((appInfo.flags & 1) != 0) {
                            isSystemApp = true;
                        }
                        return isSystemApp;
                    }
                } else {
                    VSlog.i(TAG, "   " + packageName + " is not exist ");
                }
                return false;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return false;
    }

    public void setEmmPackageCacheIfNeed() {
        if (this.mVivoPKMSUtils.emmPackageCacheList != null && this.mVivoPKMSUtils.emmPackageCacheList.size() > 0) {
            Iterator<VivoPKMSUtils.EmmPackage> it = this.mVivoPKMSUtils.emmPackageCacheList.iterator();
            while (it.hasNext()) {
                VivoPKMSUtils.EmmPackage emmPackage = it.next();
                this.mDevicePolicyManager.setEmmPackage(emmPackage.packageName, emmPackage.info, emmPackage.add, emmPackage.userHandle);
            }
        }
    }

    public boolean checkClearApplicationUserDataAllowed(String packageName, int userId) {
        if (this.mDevicePolicyManager.getCustomType() > 0) {
            long callingId = Binder.clearCallingIdentity();
            try {
                List<String> disallowedClearDataList = this.mDevicePolicyManager.getRestrictionInfoList(null, 1504, userId);
                if (disallowedClearDataList != null && disallowedClearDataList.contains(packageName)) {
                    VSlog.i(TAG, "Can not clear data for a disallowedClearData package: " + packageName);
                    return true;
                }
                return false;
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }
        return false;
    }

    public int modifyDeleteFlags(int deleteFlags, int uid, String packageName) {
        List<String> list;
        HashMap<String, String> hashMap;
        String[] callingPgks;
        int deleteFlagsTemp = deleteFlags;
        if (IS_SUPPORT_SYS_APP_UNINSTALL || this.mVgcSysUninstallableList != null) {
            HashMap<String, String> hashMap2 = this.mSysCustomAppMap;
            if ((hashMap2 == null || !hashMap2.containsKey(packageName)) && ((list = this.mVgcSysUninstallableList) == null || !list.contains(packageName))) {
                deleteFlagsTemp &= -1048577;
            }
            if ((deleteFlagsTemp & DELETE_VIVO_SYS_APP) != 0 && (callingPgks = this.mPms.getPackagesForUid(uid)) != null) {
                boolean find = false;
                for (String pkgTemp : callingPgks) {
                    if ("com.android.packageinstaller".equals(pkgTemp) || "com.bbk.launcher2".equals(pkgTemp) || "com.bbk.scene.launcher.theme".equals(pkgTemp) || VivoNotificationManagerServiceImpl.PKG_LAUNCHER.equals(pkgTemp) || VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS.equals(pkgTemp)) {
                        find = true;
                        VSlog.i(TAG, "Find " + pkgTemp + " " + uid);
                        break;
                    }
                }
                if (!find) {
                    deleteFlagsTemp &= -1048577;
                    VSlog.w(TAG, uid + " want uninstall " + packageName + ", but this is not allow!  ");
                }
            }
            if ((uid == 2000 || uid == 0) && (hashMap = this.mSysCustomAppMap) != null && hashMap.containsKey(packageName)) {
                int deleteFlagsTemp2 = deleteFlagsTemp | DELETE_VIVO_SYS_APP;
                if (PackageManagerService.DEBUG_FOR_ALL) {
                    VSlog.d(TAG, "Allow adb unistall special apk " + packageName);
                    return deleteFlagsTemp2;
                }
                return deleteFlagsTemp2;
            }
            return deleteFlagsTemp;
        }
        return deleteFlagsTemp & (-1048577);
    }

    public void addDeletedSystemCustomApp(int returnCode, String packageName) {
        List<String> list;
        VSlog.i(TAG, "del end " + this.mDeletedSysAppList + " returnCode:" + returnCode + " " + this.mSysCustomAppMap + " " + packageName + " " + this.mDeletedSysAppList);
        if (returnCode == 1 && !isAnyInstalled(packageName)) {
            HashMap<String, String> hashMap = this.mSysCustomAppMap;
            if ((hashMap != null && hashMap.containsKey(packageName)) || ((list = this.mVgcSysUninstallableList) != null && list.contains(packageName))) {
                boolean shouldWrite = true;
                synchronized (this.mPms.mLock) {
                    PackageSetting installedPsTemp = (PackageSetting) this.mPms.mSettings.mPackages.get(packageName);
                    VSlog.i(TAG, "del  " + packageName + " installedPsTemp:" + installedPsTemp);
                    if (installedPsTemp != null) {
                        String codePathString = installedPsTemp.codePathString;
                        VSlog.i(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + packageName + " codePath:" + codePathString);
                        if (codePathString != null && codePathString.contains("/system/custom")) {
                            shouldWrite = false;
                        } else if (codePathString != null && this.mVgcSysUninstallableList != null && this.mVgcSysUninstallableList.contains(packageName)) {
                            shouldWrite = false;
                        }
                    }
                }
                if (shouldWrite) {
                    synchronized (this.mSysCustomAppMap) {
                        if (!this.mDeletedSysAppList.contains(packageName)) {
                            this.mDeletedSysAppList.add(packageName);
                            this.mVivoPKMSUtils.writeDeletedSysPkgToLocalFile("data/system/v_deleted_sys_app.xml", this.mDeletedSysAppList, packageName, false);
                            if (PackageManagerService.DEBUG_REMOVE) {
                                VSlog.d(TAG, "#Should write to list " + this.mDeletedSysAppList);
                            }
                        } else {
                            VSlog.d(TAG, " " + packageName + " already in " + this.mDeletedSysAppList);
                        }
                    }
                } else if (PackageManagerService.DEBUG_REMOVE) {
                    VSlog.d(TAG, "Del app is not from custom, donot remove cache and local record; " + this.mDeletedSysAppList);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleInstallVCustomSysApp(VivoPKMSUtils.VSysAppInstallParam vAppInstallParam) {
        if (vAppInstallParam == null) {
            return;
        }
        try {
            if (!checkInstallVSysAppIsReallyNeed(vAppInstallParam)) {
                try {
                    if (vAppInstallParam.observer != null) {
                        vAppInstallParam.observer.onPackageInstalled(vAppInstallParam.packageName, -1, "Not need install this app.", (Bundle) null);
                    }
                    VSlog.w(TAG, "Not need install " + vAppInstallParam.packageName + " already exist.");
                    return;
                } catch (RemoteException e) {
                    VSlog.w(TAG, "remote died.");
                }
            }
            AndroidPackage installPkg = handleInstallVCustomSysAppInner(vAppInstallParam);
            if (installPkg == null) {
                try {
                    if (vAppInstallParam.observer != null) {
                        vAppInstallParam.observer.onPackageInstalled(vAppInstallParam.packageName, -18, "install failed.", (Bundle) null);
                        return;
                    }
                    return;
                } catch (RemoteException e2) {
                    VSlog.w(TAG, "remote died.");
                    return;
                }
            }
            sendBroadCastAfterInstallVCustomSysAPpSuc(vAppInstallParam.packageName);
            removeVCustomSysAppLocalDelMark(vAppInstallParam.packageName);
            try {
                if (vAppInstallParam.observer != null) {
                    vAppInstallParam.observer.onPackageInstalled(vAppInstallParam.packageName, 1, "install succ.", (Bundle) null);
                }
            } catch (RemoteException e3) {
                VSlog.w(TAG, "remote died.");
            }
        } catch (Exception e4) {
            VSlog.e(TAG, "#check " + e4.getMessage());
        }
    }

    private boolean checkInstallVSysAppIsReallyNeed(VivoPKMSUtils.VSysAppInstallParam vAppInstallParam) {
        String existApkSourceDir;
        if ((vAppInstallParam.installFlags & 2) != 0) {
            VSlog.i(TAG, "will replace install " + vAppInstallParam.packageName);
            return true;
        }
        int myUserId = UserHandle.myUserId();
        ApplicationInfo appInfo = this.mPms.getApplicationInfo(vAppInstallParam.packageName, 0, myUserId);
        if (appInfo == null || (existApkSourceDir = appInfo.sourceDir) == null || !existApkSourceDir.contains("/system/custom")) {
            return true;
        }
        if ((vAppInstallParam.installFlags & 2) != 0) {
            VSlog.i(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + vAppInstallParam.packageName + " already install in system_custom, but installer request replace install it.");
            return true;
        }
        VSlog.w(TAG, vAppInstallParam.packageName + " already exist.");
        return false;
    }

    private AndroidPackage handleInstallVCustomSysAppInner(VivoPKMSUtils.VSysAppInstallParam vAppInstallParam) {
        int scanFlags;
        AndroidPackage newPkg;
        AndroidPackage newPkg2 = null;
        String apkPath = this.mSysCustomAppMap.get(vAppInstallParam.packageName);
        if (PackageManagerService.DEBUG_FOR_ALL) {
            VSlog.d(TAG, "handleInstallVCustomSysAppInner apkPath:" + apkPath);
        }
        if (apkPath == null) {
            return null;
        }
        File installPkgFile = new File(apkPath);
        if (!installPkgFile.exists()) {
            return null;
        }
        int parserFlags = 0 | 17;
        if (apkPath.contains("priv-app")) {
            int scanFlags2 = 70160 | Dataspace.STANDARD_BT601_625;
            scanFlags = scanFlags2;
        } else {
            scanFlags = 70160;
        }
        try {
            PackageSetting installedPkg = this.mPms.mSettings.getPackageLPr(vAppInstallParam.packageName);
            if (installedPkg != null) {
                boolean successful = this.mPms.setSystemAppInstallState(vAppInstallParam.packageName, true, ActivityManager.getCurrentUser());
                VSlog.e(TAG, "setSystemAppInstallState:" + successful);
                if (successful) {
                    newPkg2 = installedPkg.pkg;
                }
                newPkg = newPkg2;
            } else {
                AndroidPackage newPkg3 = this.mPms.scanPackageTracedLI(installPkgFile, parserFlags, scanFlags, 0L, new UserHandle(ActivityManager.getCurrentUser()));
                newPkg = newPkg3;
            }
            VSlog.i(TAG, "#install vCustomSys end " + newPkg);
            if (newPkg == null) {
                return null;
            }
            try {
                synchronized (this.mPms.mLock) {
                    this.mPms.updateSharedLibrariesLocked(newPkg, this.mPms.mSettings.getPackageLPr(newPkg.getPackageName()), (AndroidPackage) null, (PackageSetting) null, Collections.unmodifiableMap(this.mPms.mPackages));
                }
            } catch (PackageManagerException e) {
                VSlog.e(TAG, "updateAllSharedLibrariesLPw failed: " + e.getMessage());
            }
            this.mPms.prepareAppDataAfterInstallLIF(newPkg);
            synchronized (this.mPms.mLock) {
                PackageSetting ps = (PackageSetting) this.mPms.mSettings.mPackages.get(newPkg.getPackageName());
                this.mPermissionManager.updatePermissions(newPkg.getPackageName(), newPkg);
                whitelistSystemAppPermissions(newPkg, ps);
                grantRuntimePermissionsForPkg(newPkg, false);
                this.mPms.mSettings.writeLPr();
            }
            return newPkg;
        } catch (PackageManagerException e2) {
            VSlog.w(TAG, "Failed to parse " + installPkgFile + ": " + e2.getMessage());
            return null;
        }
    }

    private void whitelistSystemAppPermissions(AndroidPackage newPkg, PackageSetting ps) {
        if (newPkg == null || newPkg.getRequestedPermissions() == null || ps == null) {
            return;
        }
        List<String> restictedPermissions = new ArrayList<>();
        for (String requestPerm : newPkg.getRequestedPermissions()) {
            BasePermission bp = this.mPermissionManager.getPermissionSettings().getPermission(requestPerm);
            if (bp != null && bp.isHardOrSoftRestricted()) {
                restictedPermissions.add(requestPerm);
            }
        }
        this.mPermissionManager.setWhitelistedRestrictedPermissions(newPkg, UserManagerService.getInstance().getUserIds(), restictedPermissions, 1000, 4);
    }

    private void removeVCustomSysAppLocalDelMark(String installPkgName) {
        synchronized (this.mSysCustomAppMap) {
            if (PackageManagerService.DEBUG_REMOVE) {
                VSlog.i(TAG, "##InstallDone1 for " + installPkgName + " " + this.mSysCustomAppMap + " mDeletedSysAppList:" + this.mDeletedSysAppList);
            }
            if (this.mSysCustomAppMap.containsKey(installPkgName) && this.mDeletedSysAppList.contains(installPkgName)) {
                this.mDeletedSysAppList.remove(installPkgName);
                this.mVivoPKMSUtils.writeDeletedSysPkgToLocalFile("data/system/v_deleted_sys_app.xml", this.mDeletedSysAppList, installPkgName, false);
                if (PackageManagerService.DEBUG_REMOVE) {
                    VSlog.d(TAG, "##InstallDone1  mDeletedSysAppList:" + this.mDeletedSysAppList);
                }
            }
        }
    }

    public void sendBroadCastAfterInstallVCustomSysAPpSuc(String packageName) {
        int pkgUid = this.mPms.getPackageUid(packageName, 0, UserHandle.myUserId());
        Bundle extras = new Bundle(1);
        extras.putInt("android.intent.extra.UID", pkgUid);
        int[] iArr = EMPTY_INT_ARRAY;
        VSlog.i(TAG, "##package " + packageName + " " + pkgUid + "  install suc, send bc.");
        this.mPms.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageName, extras, 0, (String) null, (IIntentReceiver) null, (int[]) null, (int[]) null, (SparseArray) null);
    }

    public void sendSpecialBroadCast() {
        this.mVivoPmsImplHandler.post(new Runnable() { // from class: com.android.server.pm.-$$Lambda$VivoPmsImpl$NYxcCZkxrHwIRVTZjGqcXVqs8Hk
            @Override // java.lang.Runnable
            public final void run() {
                VivoPmsImpl.this.lambda$sendSpecialBroadCast$1$VivoPmsImpl();
            }
        });
    }

    public /* synthetic */ void lambda$sendSpecialBroadCast$1$VivoPmsImpl() {
        Intent mIntent = new Intent("vivo.intent.action.INSTALLED_LOGLADECK");
        mIntent.setPackage("com.vivo.daemonService");
        this.mContext.sendBroadcast(mIntent);
    }

    public Map<String, String> getUninstallSysAppMap() {
        if (!IS_SUPPORT_SYS_APP_UNINSTALL) {
            return new HashMap();
        }
        if (!this.mSystemReady) {
            return new HashMap();
        }
        int callingUid = Binder.getCallingUid();
        int compResult = 0;
        if (callingUid != 1000) {
            compResult = this.mPms.checkUidSignatures(callingUid, 1000);
        }
        if (compResult != 0) {
            return new HashMap();
        }
        VSlog.i(TAG, "callingUid " + callingUid + " get support uninstall sys app, size:" + this.mSuportUninstallSysAppABPathMap.size());
        if (PackageManagerService.DEBUG_FOR_ALL) {
            VSlog.d(TAG, "Support uninstall sys app info:" + this.mSuportUninstallSysAppABPathMap);
        }
        return this.mSuportUninstallSysAppABPathMap;
    }

    public boolean isAdbUnInstallNeedVerify(boolean chatty, String packageName, String internalPackageName, IPackageDeleteObserver2 observer, int flags, int callingUid, int callingPid, boolean isEng, boolean isOverseas, int userId) {
        synchronized (this.mPms.mLock) {
            try {
                try {
                    PackageSetting uninstalledPsTemp = (PackageSetting) this.mPms.mSettings.mPackages.get(packageName);
                    try {
                        boolean needVerify = this.mVivoUninstallMgr.handleForAdbUnInstallVerify(chatty, packageName, internalPackageName, observer, uninstalledPsTemp, flags, callingUid, callingPid, isEng, isOverseas, userId);
                        VSlog.w(TAG, "not silent uninstall, needVerify? " + needVerify);
                        return needVerify;
                    } catch (Exception e) {
                        VSlog.w(TAG, "uninstall ," + e.toString());
                        return false;
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

    public void scheduleAdbUnInstallObserver(String packageName, int resultCode, int callbackType) {
        this.mVivoUninstallMgr.scheduleAdbUnInstallObserver(packageName, resultCode, callbackType);
    }

    public Boolean isPkgInDeletedSysAppList(String packageName) {
        synchronized (this.mSysCustomAppMap) {
            if (this.mDeletedSysAppList.contains(packageName)) {
                return true;
            }
            return false;
        }
    }

    public void isNeedSkipInstallAndAddFlag(ParsedPackage pkg, PackageSetting installedPkgSetting) throws PackageManagerException {
        initSysCustomAndSkipDeleted(pkg);
        skipScanUpdatedBuiltIn3PartApp(pkg, installedPkgSetting);
        skipScanDeletedBuiltIn3PartApp(pkg);
        skipScanBrushDeviceApp(pkg);
        skipScanUpdatedVgcPartApp(pkg, installedPkgSetting);
        skipScanDeletedVgcPartApp(pkg);
    }

    public void initSysCustomAndSkipDeleted(ParsedPackage pkg) throws PackageManagerException {
        if (IS_SUPPORT_SYS_APP_UNINSTALL) {
            synchronized (this.mSysCustomAppMap) {
                if (!this.mSystemReady) {
                    String codePath = pkg.getCodePath();
                    String baseCodePath = pkg.getBaseCodePath();
                    if (PackageManagerService.DEBUG_INSTALL) {
                        VSlog.d(TAG, "##scan " + pkg.getPackageName() + " codePath:" + codePath + " baseCodePath:" + baseCodePath);
                    }
                    if (PackageManagerService.DEBUG_FOR_ALL) {
                        VSlog.d(TAG, "Map1 " + this.mSysCustomAppMap + " " + this.mDeletedSysAppList);
                    }
                    if (codePath != null && codePath.contains("/system/custom") && !this.mSysCustomAppMap.containsKey(pkg.getPackageName())) {
                        VSlog.i(TAG, "##will add " + pkg.getPackageName());
                        this.mSysCustomAppMap.put(pkg.getPackageName(), codePath);
                        this.mSuportUninstallSysAppABPathMap.put(pkg.getPackageName(), baseCodePath);
                    }
                    if (this.mSysCustomAppMap.containsKey(pkg.getPackageName()) && this.mDeletedSysAppList.contains(pkg.getPackageName()) && codePath.contains("/system/custom")) {
                        throw new PackageManagerException((int) UNKNOWN_UID, pkg.getPackageName() + ", This app has uninstalled from custom, so not install! ####");
                    }
                }
            }
        }
        List<String> list = this.mVgcSysUninstallableList;
        if (list != null && list.contains(pkg.getPackageName()) && this.mDeletedSysAppList.contains(pkg.getPackageName()) && pkg.getCodePath() != null && !pkg.getCodePath().startsWith("/data/app")) {
            throw new PackageManagerException((int) UNKNOWN_UID, pkg.getPackageName() + ", This app has uninstalled from custom, so not install! ####");
        }
    }

    public void skipScanUpdatedBuiltIn3PartApp(ParsedPackage pkg, PackageSetting installedPkgSetting) throws PackageManagerException {
        if (isBuildInThirdAppPath(pkg.getCodePath()) && installedPkgSetting != null) {
            if (PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                VLog.d(TAG, "Examining " + pkg.getCodePath() + " and requiring known paths " + installedPkgSetting.codePathString + " & " + installedPkgSetting.resourcePathString);
            }
            if (!pkg.getCodePath().equals(installedPkgSetting.codePathString) && pkg.getLongVersionCode() <= installedPkgSetting.versionCode) {
                throw new PackageManagerException(-23, "Application package " + pkg.getPackageName() + " found at " + pkg.getCodePath() + " but expected at " + installedPkgSetting.codePathString + "; ignoring.");
            }
        }
    }

    public boolean isSkipRemoveCodePath(File codePath) {
        if (PackageManagerService.DEBUG_REMOVE) {
            VSlog.d(TAG, "removeCodePathLI call INSTALL TO rm  " + codePath);
        }
        if (codePath != null) {
            try {
                String path = codePath.getCanonicalPath();
                if (path != null && isBuildInThirdAppPath(path)) {
                    VSlog.w(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + path + " is not need remove.");
                    return true;
                } else if (path != null && path.contains("/system/custom")) {
                    VSlog.w(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + path + " is not need remove.");
                    return true;
                } else if (path != null && path.contains(VivoVGCPKMSUtils.DATA_VGC_DATA_PATH)) {
                    VSlog.w(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + path + " is not need remove.");
                    return true;
                } else if (path != null && path.contains(VivoVGCPKMSUtils.COTA_DATA_PATH)) {
                    VSlog.w(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + path + " is not need remove.");
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                VSlog.e(TAG, "file path " + codePath + " " + e.toString());
                return false;
            }
        }
        return false;
    }

    public void skipScanBrushDeviceApp(ParsedPackage pkg) throws PackageManagerException {
        if ("com.vivo.daemonService".equals(pkg.getPackageName())) {
            String codePath = pkg.getCodePath();
            VSlog.i(TAG, "codePath:" + codePath + " pkg:" + pkg);
            if (!"/system/app/VivoDaemonService".equals(codePath)) {
                throw new PackageManagerException(-2, "Application package " + pkg.getPackageName() + " from " + codePath + " is INVALIED apk.  Skipping duplicate.");
            }
        }
    }

    public void setDetailPackageForResolveIntent(Intent intent, int userId) {
        if (!this.mIsOverseas && "android.intent.action.VIEW".equals(intent.getAction()) && intent.getDataString() != null && intent.getPackage() == null && intent.getComponent() == null) {
            String scheme = intent.getScheme();
            if ("market".equals(scheme) && this.mPms.getPackageInfo("com.bbk.appstore", 0, userId) != null) {
                intent.setPackage("com.bbk.appstore");
            }
        }
    }

    private void grantRuntimePermissionsForPkg(AndroidPackage pkg, boolean systemFixed) {
        int newFlags;
        List<String> requestedPermissions = pkg.getRequestedPermissions();
        int grantablePermissionCount = requestedPermissions.size();
        for (int i = 0; i < grantablePermissionCount; i++) {
            String permission = requestedPermissions.get(i);
            int flags = this.mPms.getPermissionFlags(permission, pkg.getPackageName(), 0);
            BasePermission bp = this.mPms.mSettings.mPermissions.getPermission(permission);
            if (bp != null && ((bp.isRuntime() || bp.isDevelopment()) && (flags & 20) == 0)) {
                this.mPms.grantRuntimePermission(pkg.getPackageName(), permission, 0);
                if (!systemFixed) {
                    newFlags = 32;
                } else {
                    int newFlags2 = 32 | 16;
                    newFlags = newFlags2;
                }
                this.mPms.updatePermissionFlags(permission, pkg.getPackageName(), newFlags, newFlags, false, 0);
            }
        }
    }

    public boolean isTestApp(String packageName) {
        if (this.mVivoPKMSUtils.isGMSApk(packageName)) {
            return true;
        }
        return false;
    }

    public boolean isDeletedSpecialSysPkg(String packageName) {
        synchronized (this.mSysCustomAppMap) {
            if (packageName != null) {
                if (this.mDeletedSysAppList.contains(packageName) && mSpecialDeletedSysPkgs.contains(packageName)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean isPackageAvailable(String packageName, int userId) {
        PackageManagerService packageManagerService = this.mPms;
        if (packageManagerService != null) {
            return packageManagerService.isPackageAvailable(packageName, userId);
        }
        return false;
    }

    public void doubleAppInstallMessageHandle(Message msg, Context context) {
        if (FtBuild.getTierLevel() == 0 && isDoubleInstanceEnable() && msg != null) {
            if (msg.arg1 == 1000) {
                this.mDoubleAppInstallMap.put("p_r", VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS);
            } else {
                String[] callingPackages = this.mPms.getPackagesForUid(msg.arg1);
                if (callingPackages != null && callingPackages.length > 0) {
                    this.mDoubleAppInstallMap.put("p_r", callingPackages[0]);
                }
            }
            this.mDoubleAppInstallMap.put("p_c", "1");
            this.mDoubleAppInstallMap.put("p_d", (String) msg.obj);
            try {
                EventTransfer.getInstance().singleEvent("F292", "F292|10004", System.currentTimeMillis(), 0L, this.mDoubleAppInstallMap);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkDoubleAppUserid(int userId) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && userId == 999) {
            return true;
        }
        return false;
    }

    public boolean skipCurrentProfileIntents(Intent intent, int userId) {
        if (checkDoubleAppUserid(userId) && intent != null && intent.getType() != null && "android.intent.action.VIEW".equals(intent.getAction()) && "content".equals(intent.getScheme())) {
            return intent.getType().contains("application");
        }
        return false;
    }

    public boolean checkDeleteDoubleApp(int userId, String packageName) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl;
        if ((userId != 0 && isDoubleInstanceEnable()) || (vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService) == null || !vivoDoubleInstanceServiceImpl.isDoubleAppUserExist()) {
            return false;
        }
        return isPackageAvailable(packageName, ProcessList.CACHED_APP_MAX_ADJ);
    }

    public boolean checkNotDoubleApp(int userId, String packageName) {
        if (checkDoubleAppUserid(userId) && UserHandle.getUserId(Binder.getCallingUid()) == 999) {
            return !isPackageAvailable(packageName, userId);
        }
        return false;
    }

    public int getDoubleAppUserid() {
        if (!isDoubleInstanceEnable()) {
            return ProcessList.INVALID_ADJ;
        }
        int userid = this.mVivoDoubleInstanceService.getDoubleAppUserId();
        return userid;
    }

    public boolean isDoubleInstanceEnable() {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null) {
            return vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable();
        }
        return false;
    }

    public boolean checkReplaceRFSForDoubleInstance(Intent intent, int userId) {
        IntentFilter intentFilter;
        if (intent == null || !checkDoubleAppUserid(userId) || (intentFilter = this.mIntentFilter) == null) {
            return false;
        }
        return intentFilter.matchAction(intent.getAction());
    }

    public boolean queryIntentCheckSchemeForDoubleInstance(Intent intent, int userId) {
        String pkg;
        if (intent != null && userId != -10000 && isDoubleInstanceEnable() && checkDoubleAppUserid(userId)) {
            String scheme = intent.getScheme();
            if ("android.intent.action.VIEW".equals(intent.getAction()) && ("http".equals(scheme) || "https".equals(scheme) || "file".equals(scheme))) {
                return true;
            }
            int callingUserid = UserHandle.getUserId(Binder.getCallingUid());
            if (checkDoubleAppUserid(callingUserid) && (pkg = intent.getPackage()) != null && !isPackageAvailable(pkg, userId)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleInstallOemApp(boolean isReplace, VivoPKMSUtils.VSysAppInstallParam vOemInstallParam) throws Exception {
        if (isReplace) {
            String path = BUILDIN_DIRS.get(BUILDIN_PKGS.get(vOemInstallParam.packageName));
            File file = new File(path, BUILDIN_PKGS.get(vOemInstallParam.packageName) + ".apk");
            installGgPackage(isReplace, file, vOemInstallParam);
            this.mVivoPmsImplHandler.sendEmptyMessageDelayed(SCAN_GOG_FRAMEWORK_APPS, 1000L);
            return;
        }
        for (String s : BUILDIN_DIRS.keySet()) {
            String path2 = BUILDIN_DIRS.get(s);
            File file2 = new File(path2, s + ".apk");
            installGgPackage(isReplace, file2, vOemInstallParam);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:47:0x010c  */
    /* JADX WARN: Removed duplicated region for block: B:52:0x011c  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void installGgPackage(boolean r32, java.io.File r33, com.android.server.pm.VivoPKMSUtils.VSysAppInstallParam r34) throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 479
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPmsImpl.installGgPackage(boolean, java.io.File, com.android.server.pm.VivoPKMSUtils$VSysAppInstallParam):void");
    }

    public boolean isGgPackage(String packageName) {
        synchronized (this.mPms.mLock) {
            this.mPms.mSettings.getPackageLPr(packageName);
        }
        return false;
    }

    public boolean checkBattleModeOn() {
        return this.isBattleModeOn;
    }

    public boolean checkGrantedForBattleMode(String permName, int callingUid) {
        if (this.isBattleModeOn && "android.permission.ACCESS_NETWORK_STATE".equals(permName) && callingUid == this.battleAppUid) {
            return true;
        }
        return false;
    }

    public void configBattleModeTag(PrintWriter pw, String[] args, int opti) {
        if (opti >= args.length) {
            return;
        }
        boolean z = !"off".equals(args[opti]);
        this.isBattleModeOn = z;
        if (z && opti + 1 < args.length) {
            try {
                this.battleAppUid = Integer.parseInt(args[opti + 1]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else {
            this.battleAppUid = -1;
        }
        pw.print("battlemode = ");
        pw.print(this.isBattleModeOn);
        pw.print(" battleAppUid = ");
        pw.println(this.battleAppUid);
    }

    public boolean needPersistPermInfo(PackageParser.Package pkg, int userId) {
        if (pkg == null || pkg.packageName == null || this.mIsOverseas) {
            return false;
        }
        return checkDoubleAppUserid(userId) || pkg.isSystem() || pkg.isPrivileged() || pkg.isUpdatedSystemApp();
    }

    public int handleUserIdByVivo(int userId, String pkg) {
        return userId;
    }

    private void acquireUxPerfLock(int opcode, String pkgName, int dat) {
        AbsVivoPerfManager ux_perf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        if (ux_perf != null) {
            ux_perf.perfUXEngine_events(opcode, 0, pkgName, dat);
        }
    }

    public void updateSettingsInternalLIBoost(String packageName, int dat) {
        if (packageName != null) {
            acquireUxPerfLock(8, packageName, 0);
        }
    }

    public void preparePackageLIBoost(String packageName, int dat) {
        acquireUxPerfLock(8, packageName, 1);
        AbsVivoPerfManager mPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        if (mPerf != null) {
            mPerf.perfHint(4242, packageName, -1, 0);
        }
    }

    public void deletePackageX(boolean res, String packageName, int dat) {
        if (res && packageName != null) {
            acquireUxPerfLock(7, packageName, dat);
        }
    }

    public boolean isNeedVerifyAdbInstall(String packageName, int installerUid, int installFlags, int sessionId) {
        boolean needVerify = this.mVivoPKMSUtils.isNeedVerifyAdbInstall(packageName, installerUid, installFlags);
        if (needVerify) {
            Intent intent = new Intent("android.content.pm.action.CONFIRM_INSTALL");
            intent.setPackage(this.mPms.getPackageInstallerPackageName());
            intent.putExtra("android.content.pm.extra.SESSION_ID", sessionId);
            intent.putExtra("android.content.pm.extra.STATUS", -1);
            intent.putExtra("install_app_from_adb", "adb");
            intent.putExtra("requestTime", String.valueOf(SystemClock.elapsedRealtime()));
            intent.putExtra("installFlags", installFlags);
            intent.addFlags(268435456);
            String currentUserId = SystemProperties.get("persist.sys.currentuser", "0");
            int userId = 0;
            try {
                userId = Integer.parseInt(currentUserId);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            this.mContext.startActivityAsUser(intent, UserHandle.of(userId));
        }
        return needVerify;
    }

    public void scanDirForThirdApp(int scanFlags, PackageParser2 packageParser, ExecutorService executorService) {
        scanDirForVgcData(scanFlags, packageParser, executorService);
        scanPreloadDir(scanFlags, packageParser, executorService);
    }

    private void scanPreloadDir(int scanFlags, PackageParser2 packageParser, ExecutorService executorService) {
        VSlog.i(TAG, "scan preload apps.");
        File preloadDir = new File("/data/preload");
        if (preloadDir.exists()) {
            this.mPms.scanDirTracedLI(preloadDir, 0, scanFlags, 0L, packageParser, executorService);
        }
    }

    public void scanDirForVgcSystem(int scanFlags, PackageParser2 packageParser, ExecutorService executorService) {
        VSlog.i(TAG, "scan VGC system apps.");
        scanDirTracedLIForVGCSystem(scanFlags, packageParser, executorService);
    }

    public void scanDirForVgcData(int scanFlags, PackageParser2 packageParser, ExecutorService executorService) {
        VSlog.i(TAG, "scan VGC data apps.");
        File dataVgcDataDir = new File(VivoVGCPKMSUtils.DATA_VGC_DATA_PATH);
        if (dataVgcDataDir.exists()) {
            this.mPms.scanDirTracedLI(dataVgcDataDir, 0, scanFlags, 0L, packageParser, executorService);
        }
        File cotaDataDir = new File(VivoVGCPKMSUtils.COTA_DATA_PATH);
        if (cotaDataDir.exists()) {
            this.mPms.scanDirTracedLI(cotaDataDir, 0, scanFlags, 0L, packageParser, executorService);
        }
    }

    private void scanDirTracedLIForVGCSystem(int scanFlags, PackageParser2 packageParser, ExecutorService executorService) {
        File dataVgcPrivDir = new File(VivoVGCPKMSUtils.DATA_VGC_PRIV_APP_PATH);
        if (dataVgcPrivDir.exists()) {
            PackageManagerService packageManagerService = this.mPms;
            packageManagerService.scanDirTracedLI(dataVgcPrivDir, packageManagerService.mDefParseFlags | 16, scanFlags | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT601_625, 0L, packageParser, executorService);
        }
        File vgcPrivDir = new File(VivoVGCPKMSUtils.VGC_PRIV_APP_PATH);
        if (vgcPrivDir.exists()) {
            PackageManagerService packageManagerService2 = this.mPms;
            packageManagerService2.scanDirTracedLI(vgcPrivDir, packageManagerService2.mDefParseFlags | 16, scanFlags | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT601_625, 0L, packageParser, executorService);
        }
        File dataVgcAppDir = new File(VivoVGCPKMSUtils.DATA_VGC_APP_PATH);
        if (dataVgcAppDir.exists()) {
            PackageManagerService packageManagerService3 = this.mPms;
            packageManagerService3.scanDirTracedLI(dataVgcAppDir, packageManagerService3.mDefParseFlags | 16, scanFlags | Dataspace.STANDARD_BT709, 0L, packageParser, executorService);
        }
        File vgcAppDir = new File(VivoVGCPKMSUtils.VGC_APP_PATH);
        if (vgcAppDir.exists()) {
            PackageManagerService packageManagerService4 = this.mPms;
            packageManagerService4.scanDirTracedLI(vgcAppDir, packageManagerService4.mDefParseFlags | 16, scanFlags | Dataspace.STANDARD_BT709, 0L, packageParser, executorService);
        }
        File cotaPrivDir = new File(VivoVGCPKMSUtils.COTA_PRIV_APP_PATH);
        if (cotaPrivDir.exists()) {
            PackageManagerService packageManagerService5 = this.mPms;
            packageManagerService5.scanDirTracedLI(cotaPrivDir, packageManagerService5.mDefParseFlags | 16, scanFlags | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT601_625, 0L, packageParser, executorService);
        }
        File cotaThirdPrivDir = new File(VivoVGCPKMSUtils.COTA_THIRD_PRIV_APP_PATH);
        if (cotaThirdPrivDir.exists()) {
            PackageManagerService packageManagerService6 = this.mPms;
            packageManagerService6.scanDirTracedLI(cotaThirdPrivDir, packageManagerService6.mDefParseFlags | 16, scanFlags | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT601_625, 0L, packageParser, executorService);
        }
        File cotaAppDir = new File(VivoVGCPKMSUtils.COTA_APP_PATH);
        if (cotaAppDir.exists()) {
            PackageManagerService packageManagerService7 = this.mPms;
            packageManagerService7.scanDirTracedLI(cotaAppDir, packageManagerService7.mDefParseFlags | 16, scanFlags | Dataspace.STANDARD_BT709, 0L, packageParser, executorService);
        }
        File cotaThirdAppDir = new File(VivoVGCPKMSUtils.COTA_THIRD_APP_PATH);
        if (cotaThirdAppDir.exists()) {
            PackageManagerService packageManagerService8 = this.mPms;
            packageManagerService8.scanDirTracedLI(cotaThirdAppDir, packageManagerService8.mDefParseFlags | 16, scanFlags | Dataspace.STANDARD_BT709, 0L, packageParser, executorService);
        }
    }

    private boolean isNeedIgnoreForVgc(File scanDir, File file) {
        if (scanDir.getPath() != null && file.getName() != null) {
            ArrayList<VivoVGCPKMSUtils.VgcBlackWhitelistApps> arrayList = this.mVgcBlackListApps;
            if (arrayList != null && !arrayList.isEmpty()) {
                Iterator<VivoVGCPKMSUtils.VgcBlackWhitelistApps> it = this.mVgcBlackListApps.iterator();
                while (it.hasNext()) {
                    VivoVGCPKMSUtils.VgcBlackWhitelistApps tempApp = it.next();
                    if (scanDir.getPath().equals(tempApp.mAppDir) && file.getName().equals(tempApp.mInnerFileName)) {
                        VSlog.i(TAG, "It is in vgc black list, We do not need to scan:" + scanDir.getPath() + " filename:" + file.getName());
                        return true;
                    }
                }
            }
            if (VivoVGCPKMSUtils.isCodePathInVGC(scanDir.getPath())) {
                ArrayList<VivoVGCPKMSUtils.VgcBlackWhitelistApps> arrayList2 = this.mVgcWhiteListApps;
                if (arrayList2 == null || arrayList2.isEmpty()) {
                    VSlog.i(TAG, "Vgc white list is empty,We do not need to scan:" + scanDir.getPath() + " filename:" + file.getName());
                    return true;
                }
                boolean whiteListApp = false;
                Iterator<VivoVGCPKMSUtils.VgcBlackWhitelistApps> it2 = this.mVgcWhiteListApps.iterator();
                while (true) {
                    if (!it2.hasNext()) {
                        break;
                    }
                    VivoVGCPKMSUtils.VgcBlackWhitelistApps tempApp2 = it2.next();
                    if (scanDir.getPath().equals(tempApp2.mAppDir) && file.getName().equals(tempApp2.mInnerFileName)) {
                        whiteListApp = true;
                        break;
                    }
                }
                if (!whiteListApp) {
                    VSlog.i(TAG, "It isn't in vgc white list, We do not need to scan:" + scanDir.getPath() + " filename:" + file.getName());
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    private void skipScanUpdatedVgcPartApp(ParsedPackage pkg, PackageSetting installedPkgSetting) throws PackageManagerException {
        if ((pkg.getCodePath().contains(VivoVGCPKMSUtils.DATA_VGC_DATA_PATH) || pkg.getCodePath().contains(VivoVGCPKMSUtils.DATA_VGC_DATA_PATH)) && installedPkgSetting != null) {
            if (PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                VSlog.d(TAG, "Examining " + pkg.getCodePath() + " and requiring known paths " + installedPkgSetting.codePathString + " & " + installedPkgSetting.resourcePathString);
            }
            if (!pkg.getCodePath().equals(installedPkgSetting.codePathString) && installedPkgSetting.codePathString != null && installedPkgSetting.codePathString.contains("/data/app/") && pkg.getLongVersionCode() <= installedPkgSetting.versionCode) {
                throw new PackageManagerException(-23, "Application package " + pkg.getPackageName() + " found at " + pkg.getCodePath() + " but expected at " + installedPkgSetting.codePathString + "; ignoring.");
            }
        }
    }

    private void skipScanDeletedVgcPartApp(ParsedPackage pkg) throws PackageManagerException {
        if (!this.mSystemReady) {
            String codePath = pkg.getCodePath();
            synchronized (this.mSupportUninstallVgcAppsList) {
                if (codePath != null) {
                    if (codePath.contains(VivoVGCPKMSUtils.DATA_VGC_DATA_PATH) || codePath.contains(VivoVGCPKMSUtils.COTA_DATA_PATH)) {
                        VSlog.i(TAG, "vgc apps add " + pkg.getPackageName());
                        this.mSupportUninstallVgcAppsList.add(pkg.getPackageName());
                    }
                }
            }
            synchronized (this.mDeletedVgcAppsList) {
                if (codePath != null) {
                    if ((codePath.contains(VivoVGCPKMSUtils.DATA_VGC_DATA_PATH) || codePath.contains("/data/vivo-apps") || codePath.contains(VivoVGCPKMSUtils.COTA_DATA_PATH)) && this.mDeletedVgcAppsList.contains(pkg.getPackageName())) {
                        throw new PackageManagerException((int) UNKNOWN_UID, pkg.getPackageName() + ", This app has uninstalled from vgc data-app, so not install! ####");
                    }
                }
            }
        }
    }

    public boolean isNeedSkipVgcApp(String codePath) {
        if (this.mSystemReady) {
            return VivoVGCPKMSUtils.isCodePathInCota(codePath);
        }
        return VivoVGCPKMSUtils.isCodePathInVGC(codePath);
    }

    public void addDeletedVgcPartApp(int returnCode, String packageName) {
        if (!isAnyInstalled(packageName) && this.mSupportUninstallVgcAppsList.contains(packageName)) {
            synchronized (this.mDeletedVgcAppsList) {
                if (!this.mDeletedVgcAppsList.contains(packageName)) {
                    this.mDeletedVgcAppsList.add(packageName);
                    VSlog.i(TAG, "uninstall vgc app :" + packageName);
                    VivoVGCPKMSUtils.writeDeletedVgcAppList(this.mDeletedVgcAppsList);
                }
            }
        }
    }

    public void scanDirForVgcOverlay(int scanFlags, PackageParser2 packageParser, ExecutorService executorService) {
        VSlog.i(TAG, "scan VGC overlay.");
        String vgcOverlayDir = SystemProperties.get("ro.vgc.config.rootdir", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) + "/overlay";
        this.mPms.scanDirTracedLI(new File(vgcOverlayDir), this.mPms.mDefParseFlags | 16, scanFlags | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT470M, 0L, packageParser, executorService);
        File cotaOverlayDir = new File(VivoVGCPKMSUtils.COTA_OVERLAY_PATH);
        VSlog.i(TAG, "scan cota overlay.");
        PackageManagerService packageManagerService = this.mPms;
        packageManagerService.scanDirTracedLI(cotaOverlayDir, packageManagerService.mDefParseFlags | 16, scanFlags | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT470M, 0L, packageParser, executorService);
    }

    private boolean isNeedForVgc(File scanDir, File file) {
        ArrayList<VivoVGCPKMSUtils.VgcBlackWhitelistApps> arrayList = this.mVgcWhiteListApps;
        if (arrayList != null && !arrayList.isEmpty()) {
            Iterator<VivoVGCPKMSUtils.VgcBlackWhitelistApps> it = this.mVgcWhiteListApps.iterator();
            while (it.hasNext()) {
                VivoVGCPKMSUtils.VgcBlackWhitelistApps tempApp = it.next();
                if (scanDir.getPath().equals(tempApp.mAppDir) && file.getName().equals(tempApp.mInnerFileName)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public void installVgcCotaApp(boolean replace, Runnable finishRunnable) {
        Message msg = this.mVivoPmsImplHandler.obtainMessage(3);
        msg.obj = finishRunnable;
        msg.arg1 = replace ? 1 : 0;
        this.mVivoPmsImplHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scanInstallVgcCotaApp(boolean replace, Runnable finishRunnable) {
        scanInstallVgcApp(replace, 1, 4624);
        scanInstallCotaApp(replace, 1, 4624);
        if (finishRunnable != null) {
            finishRunnable.run();
        }
    }

    public void reInstallCotaApps(ArrayList<String> codePaths, Runnable finishRunnable) {
        Message msg = this.mVivoPmsImplHandler.obtainMessage(4);
        msg.obj = finishRunnable;
        Bundle data = new Bundle(1);
        data.putStringArrayList("paths", codePaths);
        msg.setData(data);
        this.mVivoPmsImplHandler.sendMessage(msg);
    }

    public void reInstallCotaAppsInternal(ArrayList<String> codePaths, Runnable finishRunnable) {
        VSlog.e(TAG, "replace cota app ...");
        Iterator<String> it = codePaths.iterator();
        while (it.hasNext()) {
            String codePath = it.next();
            File file = new File(codePath);
            VSlog.e(TAG, "codePath:" + codePath);
            if (file.exists() && VivoVGCPKMSUtils.isCodePathInCota(codePath)) {
                int newParserFlags = 1;
                int newScanFlags = 4624;
                if (codePath.contains(VivoVGCPKMSUtils.COTA_OVERLAY_PATH)) {
                    newParserFlags = 1 | 16;
                    newScanFlags = 4624 | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT470M;
                } else if (codePath.contains(VivoVGCPKMSUtils.COTA_PRIV_APP_PATH) || codePath.contains(VivoVGCPKMSUtils.COTA_THIRD_PRIV_APP_PATH)) {
                    newParserFlags = 1 | 16;
                    newScanFlags = 4624 | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT601_625;
                } else if (codePath.contains(VivoVGCPKMSUtils.COTA_APP_PATH) || codePath.contains(VivoVGCPKMSUtils.COTA_THIRD_APP_PATH)) {
                    newParserFlags = 1 | 16;
                    newScanFlags = 4624 | Dataspace.STANDARD_BT709;
                }
                scanInstallApp(file, newParserFlags, newScanFlags, true);
            }
        }
        if (finishRunnable != null) {
            finishRunnable.run();
        }
    }

    private void scanInstallApp(File file, int parseFlags, int scanFlags, boolean replyTo) {
        if (file == null || !file.exists()) {
            VSlog.i(TAG, "scanInstallApp file not exists " + file);
            return;
        }
        boolean isPackage = (PackageParser.isApkFile(file) || file.isDirectory()) && !PackageInstallerService.isStageName(file.getName());
        if (!isPackage || isNeedIgnoreForCota(file, replyTo)) {
            return;
        }
        try {
            AndroidPackage newPkg = this.mPms.scanPackageTracedLI(file, parseFlags, scanFlags, 0L, UserHandle.SYSTEM);
            if (newPkg == null) {
                return;
            }
            try {
                this.mPms.updateSharedLibrariesLocked(newPkg, this.mPms.mSettings.getPackageLPr(newPkg.getPackageName()), (AndroidPackage) null, (PackageSetting) null, Collections.unmodifiableMap(this.mPms.mPackages));
            } catch (PackageManagerException e) {
                VSlog.e(TAG, "updateAllSharedLibrariesLPw failed: " + e.getMessage());
            }
            this.mPms.prepareAppDataAfterInstallLIF(newPkg);
            synchronized (this.mPms.mLock) {
                this.mPermissionManager.updatePermissions(newPkg.getPackageName(), newPkg);
                this.mPms.mSettings.writeLPr();
            }
            removeDeletedVgcPartApp(newPkg.getPackageName());
            sendBroadCastAfterInstallVCustomSysAPpSuc(newPkg.getPackageName());
            reply2Vgc(newPkg.getPackageName(), newPkg.getCodePath(), replyTo, 1);
        } catch (PackageManagerException e2) {
            reply2Vgc(file.getName(), file.getAbsolutePath(), replyTo, e2.error);
            VSlog.w(TAG, "Failed to parse " + file + ": " + e2.getMessage());
        }
    }

    private void reply2Vgc(String pkgName, String path, boolean replyTo, int error) {
        if (replyTo && this.mVivoVgcManager != null) {
            Bundle b = new Bundle();
            b.putString("pkg", pkgName);
            b.putString("path", path);
            b.putInt("result", error);
            this.mVivoVgcManager.setParam("PMS", b);
        }
    }

    private boolean isNeedIgnoreForCota(File file, boolean replyTo) {
        String codePath = file.getAbsolutePath();
        if (!codePath.contains(VivoVGCPKMSUtils.COTA_DATA_PATH) || !this.mDeletedVgcAppsList.contains(file.getName())) {
            return this.mPms.mSettings.mPackages.containsKey(file.getName()) && checkDowngrade(file, replyTo);
        }
        reply2Vgc(file.getName(), file.getAbsolutePath(), replyTo, -9999);
        return true;
    }

    private boolean checkDowngrade(File file, boolean replyTo) {
        File[] listFiles;
        PackageSetting oldPkg = (PackageSetting) this.mPms.mSettings.mPackages.get(file.getName());
        if (oldPkg != null && file.isDirectory()) {
            File apkFile = null;
            for (File tmpFile : file.listFiles()) {
                if (PackageParser.isApkFile(tmpFile)) {
                    apkFile = tmpFile;
                }
            }
            if (apkFile != null) {
                try {
                    PackageParser.ApkLite apkLite = PackageParser.parseApkLite(apkFile, 0);
                    if (apkLite != null && apkLite.getLongVersionCode() <= oldPkg.versionCode) {
                        reply2Vgc(file.getName(), file.getAbsolutePath(), replyTo, -25);
                        return true;
                    }
                } catch (PackageParser.PackageParserException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private void scanInstallVgcApp(boolean replace, int parserFlags, int scanFlags) {
        VSlog.i(TAG, "scan install VGC system apps.");
        File dataVgcPrivDir = new File(VivoVGCPKMSUtils.DATA_VGC_PRIV_APP_PATH);
        if (dataVgcPrivDir.exists()) {
            File[] dataVgcPrivfiles = dataVgcPrivDir.listFiles();
            if (!ArrayUtils.isEmpty(dataVgcPrivfiles)) {
                for (File file : dataVgcPrivfiles) {
                    if (!isNeedIgnoreForVgc(dataVgcPrivDir, file)) {
                        scanInstallApp(file, parserFlags | 16, scanFlags | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT601_625, false);
                    }
                }
            }
        }
        File vgcPrivDir = new File(VivoVGCPKMSUtils.VGC_PRIV_APP_PATH);
        if (vgcPrivDir.exists()) {
            File[] vgcPrivfiles = vgcPrivDir.listFiles();
            if (!ArrayUtils.isEmpty(vgcPrivfiles)) {
                for (File file2 : vgcPrivfiles) {
                    if (!isNeedIgnoreForVgc(vgcPrivDir, file2)) {
                        scanInstallApp(file2, parserFlags | 16, scanFlags | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT601_625, false);
                    }
                }
            }
        }
        File dataVgcAppDir = new File(VivoVGCPKMSUtils.DATA_VGC_APP_PATH);
        if (dataVgcAppDir.exists()) {
            File[] dataVgcFiles = dataVgcAppDir.listFiles();
            if (!ArrayUtils.isEmpty(dataVgcFiles)) {
                for (File file3 : dataVgcFiles) {
                    if (!isNeedIgnoreForVgc(dataVgcAppDir, file3)) {
                        scanInstallApp(file3, parserFlags | 16, scanFlags | Dataspace.STANDARD_BT709, false);
                    }
                }
            }
        }
        File vgcAppDir = new File(VivoVGCPKMSUtils.VGC_APP_PATH);
        if (vgcAppDir.exists()) {
            File[] vgcAppFiles = vgcAppDir.listFiles();
            if (!ArrayUtils.isEmpty(vgcAppFiles)) {
                for (File file4 : vgcAppFiles) {
                    if (!isNeedIgnoreForVgc(vgcAppDir, file4)) {
                        scanInstallApp(file4, parserFlags | 16, scanFlags | Dataspace.STANDARD_BT709, false);
                    }
                }
            }
        }
        VSlog.i(TAG, "scan install VGC data apps.");
        File dataVgcDataDir = new File(VivoVGCPKMSUtils.DATA_VGC_DATA_PATH);
        if (dataVgcDataDir.exists()) {
            File[] dataVgcDataFiles = dataVgcDataDir.listFiles();
            if (!ArrayUtils.isEmpty(dataVgcDataFiles)) {
                for (File file5 : dataVgcDataFiles) {
                    if (!isNeedIgnoreForVgc(dataVgcDataDir, file5)) {
                        scanInstallApp(file5, 0, scanFlags, false);
                    }
                }
            }
        }
    }

    private void scanInstallCotaApp(boolean replace, int parserFlags, int scanFlags) {
        VSlog.i(TAG, "scan install cota system apps.");
        File cotaOverlayDir = new File(VivoVGCPKMSUtils.COTA_OVERLAY_PATH);
        if (cotaOverlayDir.exists()) {
            File[] cotaOverlayfiles = cotaOverlayDir.listFiles();
            if (!ArrayUtils.isEmpty(cotaOverlayfiles)) {
                for (File file : cotaOverlayfiles) {
                    scanInstallApp(file, parserFlags | 16, scanFlags | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT470M, true);
                }
            }
        }
        File cotaPrivDir = new File(VivoVGCPKMSUtils.COTA_PRIV_APP_PATH);
        if (cotaPrivDir.exists()) {
            File[] cotaPrivfiles = cotaPrivDir.listFiles();
            if (!ArrayUtils.isEmpty(cotaPrivfiles)) {
                for (File file2 : cotaPrivfiles) {
                    scanInstallApp(file2, parserFlags | 16, scanFlags | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT601_625, true);
                }
            }
        }
        File cotaThirdPrivDir = new File(VivoVGCPKMSUtils.COTA_THIRD_PRIV_APP_PATH);
        if (cotaThirdPrivDir.exists()) {
            File[] cotaThirdPrivfiles = cotaThirdPrivDir.listFiles();
            if (!ArrayUtils.isEmpty(cotaThirdPrivfiles)) {
                for (File file3 : cotaThirdPrivfiles) {
                    scanInstallApp(file3, parserFlags | 16, scanFlags | Dataspace.STANDARD_BT709 | Dataspace.STANDARD_BT601_625, true);
                }
            }
        }
        File cotaAppDir = new File(VivoVGCPKMSUtils.COTA_APP_PATH);
        if (cotaAppDir.exists()) {
            File[] cotaAppFiles = cotaAppDir.listFiles();
            if (!ArrayUtils.isEmpty(cotaAppFiles)) {
                for (File file4 : cotaAppFiles) {
                    scanInstallApp(file4, parserFlags | 16, scanFlags | Dataspace.STANDARD_BT709, true);
                }
            }
        }
        File cotaThirdAppDir = new File(VivoVGCPKMSUtils.COTA_THIRD_APP_PATH);
        if (cotaThirdAppDir.exists()) {
            File[] cotaThirdAppFiles = cotaThirdAppDir.listFiles();
            if (!ArrayUtils.isEmpty(cotaThirdAppFiles)) {
                for (File file5 : cotaThirdAppFiles) {
                    scanInstallApp(file5, parserFlags | 16, scanFlags | Dataspace.STANDARD_BT709, true);
                }
            }
        }
        VSlog.i(TAG, "scan install cota data apps.");
        File cotaDataDir = new File(VivoVGCPKMSUtils.COTA_DATA_PATH);
        if (cotaDataDir.exists()) {
            File[] cotaDataFiles = cotaDataDir.listFiles();
            if (!ArrayUtils.isEmpty(cotaDataFiles)) {
                for (File file6 : cotaDataFiles) {
                    scanInstallApp(file6, 0, scanFlags, true);
                }
            }
        }
    }

    private void removeDeletedVgcPartApp(String packageName) {
        if (this.mSupportUninstallVgcAppsList.contains(packageName)) {
            synchronized (this.mDeletedVgcAppsList) {
                if (this.mDeletedVgcAppsList.contains(packageName)) {
                    this.mDeletedVgcAppsList.remove(packageName);
                    VivoVGCPKMSUtils.writeDeletedVgcAppList(this.mDeletedVgcAppsList);
                }
            }
        }
    }

    private boolean isAnyInstalled(String packageName) {
        PackageSetting ps = this.mPms.mSettings.getPackageLPr(packageName);
        if (ps != null && ps.isAnyInstalled(this.mPms.mUserManager.getUserIds())) {
            return true;
        }
        return false;
    }

    public ParceledListSlice<PackageInfo> getInstalledPackages(String msg, int flags, int userId, boolean listUninstalled) {
        VivoPKMSDatabaseUtils.ForbidResult forbidResult = this.mVivoPKMSDatabaseUtils.checkCallerPackageIsForbidThreePartApp(msg);
        if (forbidResult.result == 2) {
            return this.mVivoPKMSDatabaseUtils.buildSysBasePackageInfoParceledList(forbidResult.packageName, forbidResult.callingUid, userId);
        }
        if (forbidResult.result == 1) {
            if (forbidResult.securityLevel == 0) {
                return buildPackageInfoParceledListSliceWithSecurityLevel(forbidResult, listUninstalled, flags, userId, 0);
            }
            if (forbidResult.securityLevel == 1) {
                return buildPackageInfoParceledListSliceWithSecurityLevel(forbidResult, listUninstalled, flags, userId, 1);
            }
            if (forbidResult.securityLevel == 2 && PackageManagerService.DEBUG_FOR_FB_APL) {
                VSlog.d(TAG, "security level is low,  do not intercept [" + forbidResult.callingUid + " " + forbidResult.packageName + "] get install package list.");
                return null;
            }
            return null;
        } else if (forbidResult.result == 0 && PackageManagerService.DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "do not intercept " + forbidResult.callingUid + " " + forbidResult.packageName + "  get install PackageInfo list.");
            return null;
        } else {
            return null;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:26:0x00ad, code lost:
        vivo.util.VSlog.d(com.android.server.pm.VivoPmsImpl.TAG, "add sys app -P> " + r12.packageName);
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x0108, code lost:
        vivo.util.VSlog.d(com.android.server.pm.VivoPmsImpl.TAG, "add sys app -P> " + r12.packageName);
     */
    /* JADX WARN: Code restructure failed: missing block: B:62:0x0194, code lost:
        vivo.util.VSlog.d(com.android.server.pm.VivoPmsImpl.TAG, "add sys app -P> " + r12.packageName);
     */
    /* JADX WARN: Code restructure failed: missing block: B:77:0x01ef, code lost:
        vivo.util.VSlog.d(com.android.server.pm.VivoPmsImpl.TAG, "add sys app -P> " + r12.packageName);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private android.content.pm.ParceledListSlice<android.content.pm.PackageInfo> buildPackageInfoParceledListSliceWithSecurityLevel(com.android.server.pm.VivoPKMSDatabaseUtils.ForbidResult r17, boolean r18, int r19, int r20, int r21) {
        /*
            Method dump skipped, instructions count: 564
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPmsImpl.buildPackageInfoParceledListSliceWithSecurityLevel(com.android.server.pm.VivoPKMSDatabaseUtils$ForbidResult, boolean, int, int, int):android.content.pm.ParceledListSlice");
    }

    private boolean isSystemApp(AndroidPackage pkg) {
        return pkg.isSystem();
    }

    private boolean isSystemApp(PackageSetting ps) {
        return (ps.pkgFlags & 1) != 0;
    }

    public List<ApplicationInfo> getInstalledApplications(String msg, int flags, int userId, boolean listUninstalled) {
        VivoPKMSDatabaseUtils.ForbidResult forbidResult = this.mVivoPKMSDatabaseUtils.checkCallerPackageIsForbidThreePartApp(msg);
        if (forbidResult.result == 2) {
            return this.mVivoPKMSDatabaseUtils.buildSysBaseApplicationInfoList(forbidResult.packageName, forbidResult.callingUid, userId);
        }
        if (forbidResult.result == 1) {
            if (forbidResult.securityLevel == 0) {
                return buildApplicationInfoListSliceWithSecurityLevel(forbidResult, listUninstalled, flags, userId, 0);
            }
            if (forbidResult.securityLevel == 1) {
                return buildApplicationInfoListSliceWithSecurityLevel(forbidResult, listUninstalled, flags, userId, 1);
            }
            if (forbidResult.securityLevel == 2 && PackageManagerService.DEBUG_FOR_FB_APL) {
                VSlog.d(TAG, "security level is low,  do not intercept [" + forbidResult.callingUid + "] app get install ApplicationInfo list.");
                return null;
            }
            return null;
        } else if (forbidResult.result == 0 && PackageManagerService.DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "do not intercept " + forbidResult.callingUid + " app get install ApplicationInfo list.");
            return null;
        } else {
            return null;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:29:0x00c2, code lost:
        vivo.util.VSlog.d(com.android.server.pm.VivoPmsImpl.TAG, "add sys app -A>  " + r11.name);
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x011d, code lost:
        vivo.util.VSlog.d(com.android.server.pm.VivoPmsImpl.TAG, "add sys app -A> " + r11.name);
     */
    /* JADX WARN: Code restructure failed: missing block: B:67:0x01b4, code lost:
        vivo.util.VSlog.d(com.android.server.pm.VivoPmsImpl.TAG, "add sys app -A> " + r11.getPackageName());
     */
    /* JADX WARN: Code restructure failed: missing block: B:83:0x0218, code lost:
        vivo.util.VSlog.d(com.android.server.pm.VivoPmsImpl.TAG, "add sys app -A> " + r11.getPackageName());
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private java.util.List<android.content.pm.ApplicationInfo> buildApplicationInfoListSliceWithSecurityLevel(com.android.server.pm.VivoPKMSDatabaseUtils.ForbidResult r17, boolean r18, int r19, int r20, int r21) {
        /*
            Method dump skipped, instructions count: 603
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPmsImpl.buildApplicationInfoListSliceWithSecurityLevel(com.android.server.pm.VivoPKMSDatabaseUtils$ForbidResult, boolean, int, int, int):java.util.List");
    }

    public boolean isVivoProtectedBroadcast(String actionName) {
        if (actionName != null) {
            for (String act : VIVO_PROTECTED_BROADCAST) {
                if (actionName.startsWith(act)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateHiddenPackageState(int uid, String pkgname, int pkgHideState, boolean foregroundActivities, String topPackage) {
        boolean foregroundActivities2;
        if (pkgHideState == 4 || pkgHideState == 8) {
            if (foregroundActivities) {
                foregroundActivities2 = foregroundActivities;
            } else {
                foregroundActivities2 = topPackage.equals(pkgname);
            }
            VSlog.i(TAG, "current top:" + topPackage);
            Integer integerUid = new Integer(uid);
            if (foregroundActivities2) {
                ScheduledFuture<?> scheduledJob = this.mScheduledJobMap.get(integerUid);
                if (scheduledJob != null) {
                    scheduledJob.cancel(true);
                    this.mScheduledJobMap.remove(integerUid);
                    VSlog.i(TAG, "PKMS mScheduledJobMap:" + this.mScheduledJobMap + " remove:" + uid);
                    return;
                }
                return;
            }
            int restoreHiddenAuto = Settings.Secure.getInt(this.mContext.getContentResolver(), "restore_hidden_auto", 1);
            int restoreHiddenHome = Settings.Secure.getInt(this.mContext.getContentResolver(), "restore_hidden_home", 1);
            if (restoreHiddenAuto == 1) {
                int restoreHiddenAutoTime = Settings.Secure.getInt(this.mContext.getContentResolver(), "restore_hidden_auto_time", 300000);
                ScheduledFuture<?> scheduledFuture = mScheduledService.schedule(new HideAppsScheduledExecutor(uid, pkgname), restoreHiddenAutoTime, TimeUnit.MILLISECONDS);
                this.mScheduledJobMap.put(integerUid, scheduledFuture);
                VSlog.i(TAG, "PKMS mScheduledJobMap:" + this.mScheduledJobMap + " put:" + integerUid);
            }
            if (restoreHiddenHome == 1) {
                if (("com.bbk.launcher2".equals(topPackage) || VivoNotificationManagerServiceImpl.PKG_LAUNCHER.equals(topPackage)) && !isRecentsView()) {
                    ScheduledFuture<?> scheduledJob2 = this.mScheduledJobMap.get(integerUid);
                    if (scheduledJob2 != null) {
                        scheduledJob2.cancel(true);
                        this.mScheduledJobMap.remove(integerUid);
                        VSlog.i(TAG, "from startingHideApp to launcher mScheduledJobMap:" + this.mScheduledJobMap + " remove:" + integerUid);
                    }
                    Message msg = this.mVivoPmsImplHandler.obtainMessage(1003, pkgname);
                    msg.arg1 = UserHandle.getUserId(uid);
                    this.mVivoPmsImplHandler.sendMessageDelayed(msg, 300L);
                    if ("true".equals(SystemProperties.get("persist.sys.btvh.first", "true"))) {
                        Message msg2 = this.mVivoPmsImplHandler.obtainMessage(1004, pkgname);
                        this.mVivoPmsImplHandler.sendMessageDelayed(msg2, 1500L);
                        SystemProperties.set("persist.sys.btvh.first", "false");
                    }
                }
            }
        }
    }

    private boolean isRecentsView() {
        WindowManagerInternal wm = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        if (wm != null) {
            return wm.isTopIsFullscreen();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class HideAppsScheduledExecutor implements Runnable {
        private String jobPackagename;
        private int jobuid;

        HideAppsScheduledExecutor(int jobuid, String jobPackagename) {
            this.jobuid = jobuid;
            this.jobPackagename = jobPackagename;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (PackageManagerService.DEBUG_FOR_ALL) {
                VSlog.d(VivoPmsImpl.TAG, "PKMS HideAppsScheduledExecutor run: jobuid=" + this.jobuid + " jobPackagename=" + this.jobPackagename);
            }
            int restoreHiddenAuto = Settings.Secure.getInt(VivoPmsImpl.this.mContext.getContentResolver(), "restore_hidden_auto", 1);
            int jobUserId = UserHandle.getUserId(this.jobuid);
            if (restoreHiddenAuto == 1) {
                VivoPmsImpl.this.mScheduledJobMap.remove(new Integer(this.jobuid));
                if (jobUserId == 0) {
                    VivoPmsImpl vivoPmsImpl = VivoPmsImpl.this;
                    if (vivoPmsImpl.getVHiddenApplicaiton(this.jobPackagename, vivoPmsImpl.getDoubleAppUserid()) == 4) {
                        VSlog.d(VivoPmsImpl.TAG, "It is time to hide user 0, but there are user 999 still in useing, we need wait user 999. jobPackagename:" + this.jobPackagename);
                        int restoreHiddenAutoTime = Settings.Secure.getInt(VivoPmsImpl.this.mContext.getContentResolver(), "restore_hidden_auto_time", 300000);
                        ScheduledFuture<?> scheduledFuture = VivoPmsImpl.mScheduledService.schedule(new HideAppsScheduledExecutor(this.jobuid, this.jobPackagename), (long) restoreHiddenAutoTime, TimeUnit.MILLISECONDS);
                        VivoPmsImpl.this.mScheduledJobMap.put(new Integer(this.jobuid), scheduledFuture);
                        return;
                    }
                }
                VivoPmsImpl.this.setVHiddenApplicaiton(this.jobPackagename, 2, jobUserId);
            }
        }
    }

    public void startHiddenAppActivityAsUser(Intent intent, int userId) {
        String pkgName;
        ComponentInfo targetComponentInfo;
        if (this.mPms.checkUidSignatures(Binder.getCallingUid(), 1000) == 0 && (pkgName = intent.getPackage()) != null) {
            if (!setVHiddenApplicaiton(pkgName, 4, userId)) {
                VSlog.w(TAG, "error!!! execStartActivity setVHiddenApplicaiton false!!!");
            } else if (userId == getDoubleAppUserid()) {
                synchronized (this.mPms.mLock) {
                    PackageSetting pkgSetting = (PackageSetting) this.mPms.mSettings.mPackages.get(pkgName);
                    if (pkgSetting == null) {
                        VSlog.w(TAG, "error!!! startHiddenAppActivityAsUser can not find " + pkgName);
                        return;
                    } else if (getVHiddenApplicaiton(pkgName, 0) == 2) {
                        setVHiddenApplicationWithoutBroadcast(pkgName, 8, 0);
                    }
                }
            }
            Intent mainIntent = new Intent("android.intent.action.MAIN", (Uri) null);
            mainIntent.addCategory("android.intent.category.LAUNCHER");
            mainIntent.setPackage(pkgName);
            List<ResolveInfo> apps = this.mPms.vivoQueryIntentActivitiesInternal(mainIntent, (String) null, 0, userId);
            ResolveInfo targetResolveInfo = apps.get(0);
            if (targetResolveInfo != null && (targetComponentInfo = targetResolveInfo.activityInfo) != null) {
                ComponentName targetComponentName = new ComponentName(targetComponentInfo.packageName, targetComponentInfo.name);
                intent.setComponent(targetComponentName);
            }
            long callingId = Binder.clearCallingIdentity();
            try {
                this.mContext.startActivityAsUser(intent, new UserHandle(userId));
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }
    }

    public ParceledListSlice<ApplicationInfo> getAllHiddenApplications(int flags, int userId) {
        ApplicationInfo ai;
        ApplicationInfo ai2;
        ApplicationInfo ai3;
        if (this.mPms.mUserManager.exists(userId)) {
            synchronized (this.mPms.mLock) {
                ArrayList<ApplicationInfo> list = new ArrayList<>();
                if (PackageManagerService.DEBUG_FOR_ALL) {
                    VSlog.d(TAG, "getAllHiddenApplications start list:" + list + " flags:" + flags + " userId:" + userId);
                }
                boolean addStartingVhiddenByDoubleApp = true;
                boolean addVhidden = (flags & 2) != 0;
                boolean addStartingVhidden = (flags & 4) != 0;
                if ((flags & 8) == 0) {
                    addStartingVhiddenByDoubleApp = false;
                }
                int callingUid = Binder.getCallingUid();
                if (this.mPms.checkUidSignatures(callingUid, 1000) != 0) {
                    VSlog.e(TAG, "Error! getAllHiddenApplications checkSignatures not match! callingUid:" + callingUid);
                    return null;
                }
                for (PackageSetting ps : this.mPms.mSettings.mPackages.values()) {
                    if (addVhidden && ps.getVHidden(userId) == 2 && (ai3 = PackageInfoUtils.generateApplicationInfo(ps.pkg, 256, ps.readUserState(userId), userId, ps)) != null) {
                        list.add(ai3);
                    }
                    if (addStartingVhidden && ps.getVHidden(userId) == 4 && (ai2 = PackageInfoUtils.generateApplicationInfo(ps.pkg, 256, ps.readUserState(userId), userId, ps)) != null) {
                        list.add(ai2);
                    }
                    if (addStartingVhiddenByDoubleApp && ps.getVHidden(userId) == 8 && (ai = PackageInfoUtils.generateApplicationInfo(ps.pkg, 256, ps.readUserState(userId), userId, ps)) != null) {
                        list.add(ai);
                    }
                }
                if (PackageManagerService.DEBUG_FOR_ALL) {
                    VSlog.i(TAG, "getAllHiddenApplications list:" + list);
                }
                return new ParceledListSlice<>(list);
            }
        }
        return ParceledListSlice.emptyList();
    }

    public boolean setVHiddenApplicaiton(String packageName, int state, int userId) {
        int doubleAppUserId;
        int callingUid = Binder.getCallingUid();
        VSlog.d(TAG, "setVHiddenApplicaiton  callingUid:" + callingUid + " packageName:" + packageName + " state:" + state + " userId:" + userId);
        if (this.mPms.checkUidSignatures(callingUid, 1000) != 0 && callingUid != 0) {
            VSlog.e(TAG, "Error! setVHiddenApplicaiton checkSignatures not match! callingUid:" + callingUid);
            return false;
        } else if (this.mPms.mProtectedPackages.isPackageStateProtected(userId, packageName)) {
            return false;
        } else {
            long callingId = Binder.clearCallingIdentity();
            boolean sendAdded = false;
            boolean sendRemoved = false;
            boolean hideDoubleAppByUser0 = false;
            try {
                doubleAppUserId = getDoubleAppUserid();
            } catch (Throwable th) {
                th = th;
            }
            try {
                synchronized (this.mPms.mLock) {
                    try {
                        PackageSetting pkgSetting = (PackageSetting) this.mPms.mSettings.mPackages.get(packageName);
                        try {
                            if (pkgSetting == null) {
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                            if (pkgSetting.getVHidden(userId) != state) {
                                pkgSetting.setVHidden(state, userId);
                                this.mPms.scheduleWritePackageRestrictionsLocked(userId);
                                if (state == 2) {
                                    sendRemoved = true;
                                    if (userId == 0 && pkgSetting.getVHidden(doubleAppUserId) == 4) {
                                        pkgSetting.setVHidden(state, doubleAppUserId);
                                        hideDoubleAppByUser0 = true;
                                    }
                                    if (userId == doubleAppUserId && pkgSetting.getVHidden(0) == 8) {
                                        pkgSetting.setVHidden(state, 0);
                                    }
                                } else if (state != 1) {
                                    Binder.restoreCallingIdentity(callingId);
                                    return true;
                                } else {
                                    sendAdded = true;
                                }
                            }
                            if (!sendAdded) {
                                if (!sendRemoved) {
                                    Binder.restoreCallingIdentity(callingId);
                                    return false;
                                }
                                this.mPms.vivoKillApplication(packageName, pkgSetting.appId, userId, "vhiding pkg");
                                this.mPms.vivoSendApplicationHiddenForUser(packageName, pkgSetting, userId);
                                if (hideDoubleAppByUser0) {
                                    this.mPms.vivoKillApplication(packageName, pkgSetting.appId, doubleAppUserId, "vhiding pkg");
                                    this.mPms.vivoSendApplicationHiddenForUser(packageName, pkgSetting, doubleAppUserId);
                                }
                                Binder.restoreCallingIdentity(callingId);
                                return true;
                            }
                            sendBroadCastAfterResetVHiddenApplicaiton(packageName, userId);
                            int jobuid = UserHandle.getUid(userId, pkgSetting.appId);
                            Integer integerUid = new Integer(jobuid);
                            ScheduledFuture<?> scheduledJob = this.mScheduledJobMap.get(integerUid);
                            if (scheduledJob != null) {
                                scheduledJob.cancel(true);
                                this.mScheduledJobMap.remove(integerUid);
                                StringBuilder sb = new StringBuilder();
                                sb.append("from unhide mScheduledJobMap:");
                                sb.append(this.mScheduledJobMap);
                                sb.append(" remove:");
                                sb.append(integerUid);
                                VSlog.i(TAG, sb.toString());
                            }
                            Binder.restoreCallingIdentity(callingId);
                            return true;
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
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
            } catch (Throwable th5) {
                th = th5;
                Binder.restoreCallingIdentity(callingId);
                throw th;
            }
        }
    }

    private boolean setVHiddenApplicationWithoutBroadcast(String packageName, int state, int userId) {
        synchronized (this.mPms.mLock) {
            PackageSetting pkgSetting = (PackageSetting) this.mPms.mSettings.mPackages.get(packageName);
            if (pkgSetting == null) {
                VSlog.w(TAG, "error!!! setVHiddenApplicaitonWithoutBroadcast can not find " + packageName);
                return false;
            } else if (pkgSetting.getVHidden(userId) == state) {
                return false;
            } else {
                pkgSetting.setVHidden(state, userId);
                return true;
            }
        }
    }

    public int getVHiddenApplicaiton(String packageName, int userId) {
        int callingUid = Binder.getCallingUid();
        if (this.mPms.checkUidSignatures(callingUid, 1000) != 0) {
            throw new IllegalArgumentException("getVHiddenApplicaiton checkSignatures not match " + callingUid);
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPms.mLock) {
                PackageSetting pkgSetting = (PackageSetting) this.mPms.mSettings.mPackages.get(packageName);
                if (pkgSetting == null) {
                    return 1;
                }
                int state = pkgSetting.getVHidden(userId);
                return state;
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public boolean isHasDoubleApp(String packageName) {
        PackageSetting pkgSetting = (PackageSetting) this.mPms.mSettings.mPackages.get(packageName);
        int doubleAppUserId = getDoubleAppUserid();
        if (PackageManagerService.DEBUG_FOR_ALL) {
            VSlog.i(TAG, "isHasDoubleApp packageName:" + packageName + " pkgSetting:" + pkgSetting + " doubleAppUserId=" + doubleAppUserId);
        }
        if (pkgSetting == null || doubleAppUserId == -10000 || !pkgSetting.readUserState(doubleAppUserId).isAvailable(256)) {
            return false;
        }
        return true;
    }

    private void registerReceiverInPms() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.pm.VivoPmsImpl.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                VivoPmsImpl.this.mPms.mIsBootCompleted = true;
                if (VivoPmsImpl.this.mPms.mIsNeedReSchedulePostBootUpdate) {
                    VivoPmsImpl.this.mPms.mIsNeedReSchedulePostBootUpdate = false;
                    BackgroundDexOptService.rescheduledPostBootUpdate(VivoPmsImpl.this.mContext);
                }
            }
        }, intentFilter, null, this.mVivoPmsImplHandler);
        try {
            ActivityManager.getService().registerVivoProcessObserver(this.mProcessObserver, 1);
        } catch (Exception e) {
            VSlog.e(TAG, "error registerProcessObserver " + e);
        }
        IntentFilter hide_appsFilter = new IntentFilter();
        hide_appsFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(new AnonymousClass5(), hide_appsFilter, null, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.pm.VivoPmsImpl$5  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass5 extends BroadcastReceiver {
        AnonymousClass5() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, final Intent intent) {
            VivoPmsImpl.this.mVivoPmsImplHandler.post(new Runnable() { // from class: com.android.server.pm.-$$Lambda$VivoPmsImpl$5$FF03Zz-ITYWlGD6beQJ3ibARylo
                @Override // java.lang.Runnable
                public final void run() {
                    VivoPmsImpl.AnonymousClass5.this.lambda$onReceive$0$VivoPmsImpl$5(intent);
                }
            });
        }

        public /* synthetic */ void lambda$onReceive$0$VivoPmsImpl$5(Intent intent) {
            VivoPmsImpl.this.hideAllHiddenApplicaitonByUser(intent, ActivityManager.getCurrentUser());
            VivoPmsImpl vivoPmsImpl = VivoPmsImpl.this;
            vivoPmsImpl.hideAllHiddenApplicaitonByUser(intent, vivoPmsImpl.getDoubleAppUserid());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideAllHiddenApplicaitonByUser(Intent intent, int userId) {
        ParceledListSlice<ApplicationInfo> allStartHiddenAppParceled = getAllHiddenApplications(4, userId);
        List<ApplicationInfo> allStartHiddenApp = null;
        if (allStartHiddenAppParceled != null) {
            allStartHiddenApp = allStartHiddenAppParceled.getList();
        }
        int restoreHiddenScreenLock = Settings.Secure.getInt(this.mContext.getContentResolver(), "restore_hidden_screen_lock", 1);
        VSlog.i(TAG, "onReceive intent:" + intent + " allStartHiddenApp:" + allStartHiddenApp + " restoreHiddenScreenLock:" + restoreHiddenScreenLock);
        if (allStartHiddenApp != null && allStartHiddenApp.size() > 0 && "android.intent.action.SCREEN_OFF".equals(intent.getAction()) && restoreHiddenScreenLock == 1) {
            for (ApplicationInfo pkginfo : allStartHiddenApp) {
                setVHiddenApplicaiton(pkginfo.packageName, 2, userId);
            }
            VSlog.i(TAG, "ACTION_SCREEN_OFF, set " + allStartHiddenApp + " back to hide OK!");
        }
    }

    private void resetVHiddenApplicaiton() {
        ParceledListSlice<ApplicationInfo> allStartHiddenAppParceled = getAllHiddenApplications(4, this.mContext.getUserId());
        List<ApplicationInfo> allStartHiddenApp = null;
        if (allStartHiddenAppParceled != null) {
            allStartHiddenApp = allStartHiddenAppParceled.getList();
        }
        if (allStartHiddenApp != null && allStartHiddenApp.size() > 0) {
            for (ApplicationInfo pkginfo : allStartHiddenApp) {
                setVHiddenApplicaiton(pkginfo.packageName, 2, this.mContext.getUserId());
            }
        }
    }

    private void sendBroadCastAfterResetVHiddenApplicaiton(String packageName, int userId) {
        int pkgUid = this.mPms.getPackageUid(packageName, 0, UserHandle.myUserId());
        Bundle extras = new Bundle(1);
        extras.putInt("android.intent.extra.UID", pkgUid);
        extras.putString("vhAction", "true");
        int[] iArr = EMPTY_INT_ARRAY;
        VSlog.i(TAG, "sendBroadCastAfterResetVHiddenApplicaiton " + packageName + " " + pkgUid);
        this.mPms.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageName, extras, 0, (String) null, (IIntentReceiver) null, new int[]{userId}, (int[]) null, (SparseArray) null);
    }

    public void insertHybridPackageInfo(String packageName, PackageInfo packageInfo) {
        if (this.mPms.checkUidSignatures(Binder.getCallingUid(), 1000) == 0) {
            this.mHybridPackageInfo.put(packageName, packageInfo);
        }
    }

    public void deleteHybridPackageInfo(String packageName) {
        if (this.mPms.checkUidSignatures(Binder.getCallingUid(), 1000) == 0) {
            this.mHybridPackageInfo.remove(packageName);
        }
    }

    public void insertHybridRule(String callingPkgname, String xcxPkgname, String platfromPkgname, String realActitityname) {
        if (this.mPms.checkUidSignatures(Binder.getCallingUid(), 1000) == 0) {
            Hybridrules hybridrules = new Hybridrules(callingPkgname, platfromPkgname, xcxPkgname, realActitityname);
            this.mHybridrulesMap.put(xcxPkgname, hybridrules);
        }
    }

    public void deleteHybridRule(String xcxPkgname) {
        if (this.mPms.checkUidSignatures(Binder.getCallingUid(), 1000) == 0) {
            this.mHybridrulesMap.remove(xcxPkgname);
        }
    }

    public void insertNewHybridRule(String callingPkgname, String xcxPkgname, String platfromPkgname, String realActitityname, String action, Bundle extras) {
        if (this.mPms.checkUidSignatures(Binder.getCallingUid(), 1000) == 0) {
            Hybridrules hybridrules = new Hybridrules(callingPkgname, platfromPkgname, xcxPkgname, realActitityname, action, extras);
            this.mHybridrulesMap.put(xcxPkgname, hybridrules);
        }
    }

    public String getHybridCallingPackage(int callinguid, String action) {
        String callerPkg = this.mPms.getNameForUid(callinguid);
        if (action != null && callerPkg != null) {
            Collection<Hybridrules> hybridrulesCollection = this.mHybridrulesMap.values();
            for (Hybridrules hybridrule : hybridrulesCollection) {
                if (action.equals(hybridrule.mAction) && callerPkg.equals(hybridrule.mCallingPkgname)) {
                    Bundle extras = hybridrule.mExtras;
                    if (extras != null) {
                        String hybrid_target_callingpackage = extras.getString("hybrid_target_callingpackage", "notarget");
                        if (!hybrid_target_callingpackage.equals("notarget")) {
                            return hybrid_target_callingpackage;
                        }
                    }
                    return hybridrule.mXcxPkgname;
                }
            }
            return null;
        }
        return null;
    }

    public ComponentName getHybridAppComponentName(int filterCallingUid, ComponentName comp) {
        Hybridrules hybridrules;
        String compPkg = comp.getPackageName();
        String compCls = comp.getClassName();
        if (compPkg != null && compCls != null && (hybridrules = this.mHybridrulesMap.get(compPkg)) != null && hybridrules.mCallingPkgname != null) {
            String callerPkg = this.mPms.getNameForUid(filterCallingUid);
            if (hybridrules.mCallingPkgname.equals(callerPkg)) {
                String compPkg2 = hybridrules.mPlatfromPkgname;
                String compCls2 = hybridrules.mRealActitityname;
                VSlog.d(TAG, "after compPkg=" + compPkg2 + " compCls=" + compCls2);
                ComponentName hybridComponentName = new ComponentName(compPkg2, compCls2);
                return hybridComponentName;
            }
            return comp;
        }
        return comp;
    }

    public PackageInfo getHybridPackageInfo(String packageName) {
        if (PackageManagerService.DEBUG_PACKAGE_INFO) {
            VSlog.v(TAG, "getPackageInfo return mHybridPackageInfo " + this.mHybridPackageInfo.get(packageName));
        }
        return this.mHybridPackageInfo.get(packageName);
    }

    public ResolveInfo goBackBbkHome(Intent intent, int flags, int userId) {
        ActivityInfo ai;
        if ("android.intent.action.MAIN".equals(intent.getAction()) && intent.getCategories() != null && intent.getCategories().contains("android.intent.category.HOME")) {
            boolean isUserUnlocked = this.mPms.mUserManager.isUserUnlocked(userId);
            if (this.mPms.mFirstBoot || ((!this.mIsOverseas && isUserUnlocked) || isBbkHomeNotLaunched(userId))) {
                if (PackageManagerService.DEBUG) {
                    VSlog.d("HomeRestriction", "return bbk launcher for no default!");
                }
                ResolveInfo ri_bbk = goBackBbkHome(flags, userId);
                if (ri_bbk != null) {
                    return ri_bbk;
                }
                return null;
            } else if (!isUserUnlocked && (ai = this.mPms.getActivityInfo(new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.FallbackHome"), flags, userId)) != null) {
                ResolveInfo follbackHome = new ResolveInfo();
                follbackHome.activityInfo = ai;
                return follbackHome;
            } else {
                return null;
            }
        }
        return null;
    }

    public ResolveInfo goBackBbkHome(Intent intent, int flags, int userId, ResolveInfo ri) {
        if (isHomeRestriction(intent)) {
            String currentHomePackage = ri.activityInfo.packageName;
            if (!Arrays.asList(VIVO_LAUNCHERS).contains(currentHomePackage) && Binder.getCallingUid() == 1000 && intent.getBooleanExtra("home_restriction", false) && !Arrays.asList(GMS_LAUNCHERS).contains(currentHomePackage)) {
                if (PackageManagerService.DEBUG) {
                    VSlog.d("HomeRestriction", "return bbk launcher ri != null ri:" + ri);
                }
                ResolveInfo ri_bbk = goBackBbkHome(flags, userId);
                if (ri_bbk != null) {
                    return ri_bbk;
                }
                return null;
            }
            return null;
        }
        return null;
    }

    public void setEnabledHomeActivity(String packageName, String className, int userId) {
        if (!this.mIsOverseas) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.HOME");
            intent.setPackage(packageName);
            List<ResolveInfo> resolveInfo = this.mPms.vivoQueryIntentActivitiesInternal(intent, (String) null, (int) Dataspace.STANDARD_BT709, userId);
            for (ResolveInfo ri : resolveInfo) {
                if (className.equals(ri.activityInfo.name)) {
                    this.mEnabledHomeActivity = className;
                    return;
                }
            }
        }
    }

    public boolean ignoreHomeSetChanged(Intent intent, List<ResolveInfo> query) {
        if (!this.mIsOverseas && !isHomeRestriction(intent) && "android.intent.action.MAIN".equals(intent.getAction()) && intent.getCategories() != null && intent.getCategories().contains("android.intent.category.HOME")) {
            for (ResolveInfo ri : query) {
                if (this.mEnabledHomeActivity.equals(ri.activityInfo.name)) {
                    VSlog.d("HomeRestriction", "ignore home set");
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public void refreshThirdLauncherPkgNames() {
        synchronized (this.mPms.mThirdLauncherNames) {
            this.mPms.mThirdLauncherNames.clear();
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        List<ResolveInfo> resolveInfo = this.mPms.vivoQueryIntentActivitiesInternal(intent, (String) null, (int) Dataspace.STANDARD_BT709, UserHandle.myUserId());
        for (ResolveInfo ri : resolveInfo) {
            String packageName = ri.activityInfo.packageName;
            if (!Arrays.asList(VIVO_LAUNCHERS).contains(packageName)) {
                synchronized (this.mPms.mThirdLauncherNames) {
                    this.mPms.mThirdLauncherNames.add(packageName);
                }
            }
        }
        VSlog.v(TAG, "refreshThirdLauncherPkgNames mPms.mThirdLauncherNames:" + this.mPms.mThirdLauncherNames);
    }

    public boolean isHomeRestriction(Intent intent) {
        boolean enable = intent != null && "android.intent.action.MAIN".equals(intent.getAction()) && intent.getCategories() != null && intent.getCategories().contains("android.intent.category.HOME") && getHomeRestrictionEnabled();
        if (this.mIsOverseas) {
            return false;
        }
        return enable;
    }

    public ResolveInfo goBackBbkHome(int flags, int userId) {
        if (this.mIsOverseas) {
            this.bbkLauncher = new ComponentName(VivoNotificationManagerServiceImpl.PKG_LAUNCHER, "com.android.launcher3.Launcher");
        }
        ActivityInfo ai = this.mPms.getActivityInfo(this.bbkLauncher, flags, userId);
        if (ai == null) {
            if (PackageManagerService.DEBUG) {
                VSlog.d("HomeRestriction", "Home restriction is enable, bbk launcher is null!");
                return null;
            }
            return null;
        }
        ResolveInfo mBbkHome = new ResolveInfo();
        mBbkHome.activityInfo = ai;
        if (!this.mIsOverseas && Binder.getCallingPid() == Process.myPid()) {
            Bundle extras = new Bundle(1);
            extras.putString("packageName", "com.bbk.launcher2");
            extras.putString("activityName", Constant.ACTIVITY_LAUNCHER);
            extras.putString("type", "1");
            this.mPms.sendPackageBroadcast("vivo.action.HOMERECOVERY", (String) null, extras, 0, (String) null, (IIntentReceiver) null, new int[]{userId}, (int[]) null, (SparseArray) null);
            if (PackageManagerService.DEBUG) {
                VSlog.d("HomeRestriction", "Home restriction is enable, return bbk launcher directory and notify iManager!");
            }
        }
        return mBbkHome;
    }

    public boolean isNeedAddPrivilegedFlag(String codePathString) {
        return locationIsOem(new File(codePathString)) || locationIsPrivileged(codePathString) || locationIsCustomPrivApp(new File(codePathString)) || codePathString.startsWith(VivoVGCPKMSUtils.DATA_VGC_PRIV_APP_PATH) || codePathString.startsWith(VivoVGCPKMSUtils.VGC_PRIV_APP_PATH) || codePathString.startsWith(VivoVGCPKMSUtils.COTA_PRIV_APP_PATH) || codePathString.startsWith(VivoVGCPKMSUtils.COTA_THIRD_PRIV_APP_PATH);
    }

    private boolean locationIsOem(File path) {
        if (path != null) {
            try {
                return path.getCanonicalPath().contains("oem/priv-app");
            } catch (IOException e) {
                VSlog.e(TAG, "Unable to access code path " + path);
                return false;
            }
        }
        return false;
    }

    private boolean locationIsPrivileged(String path) {
        try {
            File privilegedAppDir = new File(Environment.getRootDirectory(), "priv-app");
            File privilegedVendorAppDir = new File(Environment.getVendorDirectory(), "priv-app");
            File privilegedOdmAppDir = new File(Environment.getOdmDirectory(), "priv-app");
            File privilegedProductAppDir = new File(Environment.getProductDirectory(), "priv-app");
            File privilegedProductServicesAppDir = new File(Environment.getProductServicesDirectory(), "priv-app");
            if (!path.startsWith(privilegedAppDir.getCanonicalPath() + "/")) {
                if (!path.startsWith(privilegedVendorAppDir.getCanonicalPath() + "/")) {
                    if (!path.startsWith(privilegedOdmAppDir.getCanonicalPath() + "/")) {
                        if (!path.startsWith(privilegedProductAppDir.getCanonicalPath() + "/")) {
                            if (!path.startsWith(privilegedProductServicesAppDir.getCanonicalPath() + "/")) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        } catch (IOException e) {
            VSlog.e(TAG, "Unable to access code path " + path);
            return false;
        }
    }

    private boolean locationIsCustomPrivApp(File path) {
        if (path != null) {
            try {
                return path.getCanonicalPath().contains("custom/priv-app");
            } catch (IOException e) {
                VSlog.e(TAG, "Unable to access code path " + path);
                return false;
            }
        }
        return false;
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:25:0x0087 -> B:26:0x0088). Please submit an issue!!! */
    public boolean installExitingPackage(PackageManagerService.InstallArgs args, int currentStatus, PackageManagerService.PackageInstalledInfo res) {
        PackageSetting pkgSetting;
        boolean existing;
        if (-25 == currentStatus) {
            try {
                int installUserId = args.user.getIdentifier();
                synchronized (this.mPms.mLock) {
                    try {
                        pkgSetting = (PackageSetting) this.mPms.mSettings.mPackages.get(args.packageName);
                        if (pkgSetting != null && !pkgSetting.getInstalled(installUserId)) {
                            existing = true;
                        } else {
                            existing = false;
                        }
                    } catch (Throwable th) {
                        th = th;
                    }
                    try {
                        if (existing) {
                            res.returnCode = this.mPms.installExistingPackageAsUser(args.packageName, installUserId, args.installFlags, args.installReason, (List) null);
                            res.pkg = pkgSetting != null ? pkgSetting.pkg : null;
                            res.name = args.packageName;
                            res.newUsers = new int[]{installUserId};
                            if (this.mPms.mNextInstallToken < 0) {
                                this.mPms.mNextInstallToken = 1;
                            }
                            PackageManagerService packageManagerService = this.mPms;
                            int token = packageManagerService.mNextInstallToken;
                            packageManagerService.mNextInstallToken = token + 1;
                            this.mPms.mRunningInstalls.put(token, new PackageManagerService.PostInstallData(args, res, (Runnable) null));
                            Message msg = this.mPms.mHandler.obtainMessage(9, token, 0);
                            this.mPms.mHandler.sendMessage(msg);
                            return true;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            } catch (Exception e) {
                VSlog.e(TAG, e.getMessage());
            }
        }
        return false;
    }

    public void interruptInstallWebView(ParsedPackage parsedPackage) throws PackageManagerService.PrepareFailure {
        if (("com.google.android.webview".equals(parsedPackage.getPackageName()) || "com.google.android.gms".equals(parsedPackage.getPackageName())) && VMRuntime.getRuntime().is64Bit() && !"arm64-v8a".equals(parsedPackage.getPrimaryCpuAbi()) && !"arm64-v8a".equals(parsedPackage.getSecondaryCpuAbi())) {
            if (PackageManagerService.DEBUG_INSTALL) {
                VSlog.d(TAG, "install fail because of ABI incompatible");
            }
            throw new PackageManagerService.PrepareFailure(-16, "this apk is for " + parsedPackage.getPrimaryCpuAbi() + ";it is incompatible!!");
        } else if ("com.android.chrome".equals(parsedPackage.getPackageName()) && this.mIsOverseas && VMRuntime.getRuntime().is64Bit() && !"arm64-v8a".equals(parsedPackage.getPrimaryCpuAbi()) && !"arm64-v8a".equals(parsedPackage.getSecondaryCpuAbi())) {
            throw new PackageManagerService.PrepareFailure(-16, "this apk is for " + parsedPackage.getPrimaryCpuAbi() + ";it is incompatible!!");
        }
    }

    public boolean isAllowSpecialPakcageSilentUninstallApk(int callingUid, String pkgName, int callingUserId) {
        int compResult = this.mPms.checkUidSignatures(callingUid, 1000);
        if (PackageManagerService.DEBUG) {
            VSlog.d(TAG, "callingUid " + callingUid + " want uninstall " + pkgName + " result " + compResult);
        }
        if (compResult == 0) {
            return true;
        }
        return false;
    }

    public void writeSystemFixedPermissionSync(int userId, Map<String, Set<String>> systemFixedPkgs) {
        this.mVivoPKMSUtils.writeSystemFixedPermissionSync(userId, systemFixedPkgs);
    }

    public void collectEmmCertificates(ParsedPackage pkg, Context context, boolean isSystemReady) throws PackageParser.PackageParserException {
        this.mVivoPKMSUtils.collectEmmCertificates(pkg, context, isSystemReady);
    }

    public boolean interceptPackageWhenScan(ParsedPackage packageToScan) {
        return !VivoPKMSUtils.isSupportCurrentPlatform(packageToScan);
    }

    public List<String> uninstallListForCountryCodeChanged(String countryCode) {
        File[] listFiles;
        boolean isDeviceProvisioned = Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) == 1;
        if (!isDeviceProvisioned && this.mPms.checkUidSignatures(Binder.getCallingUid(), 1000) == 0) {
            String preinstalPath = SystemProperties.get("ro.preinstall.path", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            File countryDir = new File(preinstalPath, countryCode);
            if (countryDir.exists() && countryDir.isDirectory()) {
                coverChannel(countryDir, preinstalPath);
                return getUninstallList(countryDir);
            }
            File preinstallDir = new File(preinstalPath);
            List<String> defaultUninstallList = getUninstallList(preinstallDir);
            if (defaultUninstallList != null) {
                return defaultUninstallList;
            }
            if (preinstallDir.isDirectory()) {
                for (File file : preinstallDir.listFiles()) {
                    if (!file.isDirectory()) {
                        file.delete();
                    }
                }
            }
            return this.mVivoPKMSUtils.getBuiltIn3PartAppPkgNameList();
        }
        VSlog.d(TAG, "isDeviceProvisioned:" + isDeviceProvisioned);
        return null;
    }

    private void coverChannel(File countryDir, String preinstalPath) {
        File[] listFiles;
        File[] listFiles2;
        try {
            File preinstallDir = new File(preinstalPath);
            if (preinstallDir.isDirectory()) {
                for (File file : preinstallDir.listFiles()) {
                    if (!file.isDirectory()) {
                        file.delete();
                    }
                }
                for (File file2 : countryDir.listFiles()) {
                    File toFile = new File(preinstallDir, file2.getName());
                    FileUtils.copy(file2, toFile);
                    FileUtils.copyPermissions(file2, toFile);
                }
            }
        } catch (Exception e) {
            VSlog.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private List<String> getUninstallList(File countryDir) {
        ArrayList<String> uninstallList = new ArrayList<>();
        File uninstalFile = new File(countryDir, "uninstall.list");
        if (uninstalFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(uninstalFile));
                while (true) {
                    String pkgName = reader.readLine();
                    if (pkgName != null) {
                        uninstallList.add(pkgName);
                    } else {
                        return uninstallList;
                    }
                }
            } catch (IOException e) {
                VSlog.e(TAG, "Failed to read uninstall.list", e);
                return null;
            } finally {
                IoUtils.closeQuietly(reader);
            }
        } else {
            return null;
        }
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        PackageInfo info;
        List<String> list = this.mFakeSystemFlagList;
        if (list != null && packageName != null && list.contains(packageName) && packageName.equals(this.mPms.getNameForUid(Binder.getCallingUid())) && (info = this.mPms.getVivoPackageInfoInternal(packageName, flags, userId)) != null) {
            ApplicationInfo applicationInfo = new ApplicationInfo(info.applicationInfo);
            applicationInfo.flags |= 1;
            info.applicationInfo = applicationInfo;
            return info;
        }
        return null;
    }

    public int getUidForInputMethodApp(String packageName) {
        int uid = 0;
        try {
            uid = this.mContext.getPackageManager().getPackageUid(packageName, ActivityManager.getCurrentUser());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (InputMethodManager.sDebugMethod) {
            VSlog.d("VivoInputMethod", "getUidForInputMethodApp: " + packageName + " ,uid: " + uid);
        }
        return uid;
    }

    private boolean isBbkHomeNotLaunched(int userId) {
        String bbkPkg = this.bbkLauncher.getPackageName();
        PackageSetting bbkHome = (PackageSetting) this.mPms.mSettings.mPackages.get(bbkPkg);
        if (bbkHome != null) {
            return bbkHome.getNotLaunched(userId);
        }
        return false;
    }

    public boolean isGamingAndDefintelyNotInstant(int uid) {
        return uid == GameOptManager.getGamingUid() && GAME_APPS.contains(GameOptManager.getGameName());
    }

    public void initCountryForOperator() {
        String[] strArr;
        this.mScanedOperator = SystemProperties.get(PERSIST_SCANED_OPERATOR, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        if ("yes".equals(SystemProperties.get("persist.vivo.scanedliquid", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK))) {
            String str = this.mScanedOperator + ",m1";
            this.mScanedOperator = str;
            SystemProperties.set(PERSIST_SCANED_OPERATOR, str);
        }
        if ("yes".equals(SystemProperties.get("persist.vivo.scanedmysingtel", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK))) {
            String str2 = this.mScanedOperator + ",Singtel";
            this.mScanedOperator = str2;
            SystemProperties.set(PERSIST_SCANED_OPERATOR, str2);
        }
        if ("yes".equals(SystemProperties.get("persist.vivo.scanedmyStarHub", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK))) {
            String str3 = this.mScanedOperator + ",StarHub";
            this.mScanedOperator = str3;
            SystemProperties.set(PERSIST_SCANED_OPERATOR, str3);
        }
        this.mScanedOperatorArray = this.mScanedOperator.split(",");
        this.mCountryCode = getcountryCode();
        this.contryForImsiMap.put("SG", SINGAPOR_IMSI);
        this.contryForImsiMap.put("RU", RUSSIA_IMSI);
        ArrayList<String> apkListm1 = new ArrayList<>();
        apkListm1.add("/system/app/com.korvac.liquid");
        apkListm1.add("/system/app/sg.gov.mnd.OneService");
        this.allOperatorApkList.addAll(apkListm1);
        this.imsiForOperatorMap.put("52503", apkListm1);
        this.imsiForOperatorMap.put("52504", apkListm1);
        this.imsiForOperatorNameMap.put("52503", "m1");
        this.imsiForOperatorNameMap.put("52504", "m1");
        ArrayList<String> apkListSingtel = new ArrayList<>();
        apkListSingtel.add("/system/app/com.singtel.mysingtel");
        apkListSingtel.add("/data/vivo-apps/com.singtel.mysingtel");
        apkListSingtel.add("/system/priv-app/com.LogiaGroup.LogiaDeck");
        this.allOperatorApkList.addAll(apkListSingtel);
        this.imsiForOperatorMap.put("52501", apkListSingtel);
        this.imsiForOperatorMap.put("52502", apkListSingtel);
        this.imsiForOperatorNameMap.put("52501", "Singtel");
        this.imsiForOperatorNameMap.put("52502", "Singtel");
        ArrayList<String> apkListStarHub = new ArrayList<>();
        apkListStarHub.add("/data/vivo-apps/com.starhub.csselfhelp");
        apkListStarHub.add("/data/vivo-apps/com.starhub.itv");
        apkListStarHub.add("/data/vivo-apps/com.disney.disneyplus");
        this.allOperatorApkList.addAll(apkListStarHub);
        this.imsiForOperatorMap.put("52505", apkListStarHub);
        this.imsiForOperatorNameMap.put("52505", "StarHub");
        ArrayList<String> apkListMTS = new ArrayList<>();
        apkListMTS.add("/data/vivo-apps/ru.mts.mtstv");
        apkListMTS.add("/data/vivo-apps/ru.mts.mtscashback");
        apkListMTS.add("/data/vivo-apps/ru.mts.mymts");
        this.allOperatorApkList.addAll(apkListMTS);
        this.imsiForOperatorMap.put("25001", apkListMTS);
        this.imsiForOperatorNameMap.put("25001", "MTS");
        if (!Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(this.mScanedOperator) && !Arrays.asList(this.mScanedOperatorArray).isEmpty()) {
            for (String scanedOperator : this.mScanedOperatorArray) {
                String scanedimsi = getKey(this.imsiForOperatorNameMap, scanedOperator);
                ArrayList<String> operatorApplist = this.imsiForOperatorMap.get(scanedimsi);
                if (operatorApplist != null && operatorApplist.size() > 0) {
                    this.mAllScanedOperatorApplist.addAll(operatorApplist);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SimStateReceiver extends BroadcastReceiver {
        SimStateReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            VSlog.i(VivoPmsImpl.TAG, "conditionalPkg onReceive intent:" + intent);
            if (intent == null) {
                return;
            }
            String state = intent.getStringExtra("ss");
            int slotId = intent.getIntExtra("phone", -1);
            if (slotId < 0 || slotId > 1 || state == null) {
                VSlog.w(VivoPmsImpl.TAG, "Invalid slot Id!");
                return;
            }
            Message msg = VivoPmsImpl.this.mVivoPmsImplHandler.obtainMessage(2);
            msg.obj = state;
            msg.arg1 = slotId;
            VivoPmsImpl.this.mVivoPmsImplHandler.sendMessageDelayed(msg, 1000L);
        }
    }

    private void registerReceivers() {
        if (!this.contryForImsiMap.keySet().contains(this.mCountryCode)) {
            VSlog.i(TAG, "Current country:" + this.mCountryCode + " do not need to regist SIM_STATE_CHANGED receivers");
            return;
        }
        IntentFilter simStateFilter = new IntentFilter();
        this.simStateReceiver = new SimStateReceiver();
        simStateFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mContext.registerReceiver(this.simStateReceiver, simStateFilter);
    }

    public String getcountryCode() {
        String countryCode = SystemProperties.get("ro.product.country.region", "unknown");
        if ("unknown".equals(countryCode)) {
            return SystemProperties.get("ro.product.customize.bbk", "unknown");
        }
        return countryCode;
    }

    public boolean interceptInsertSimScanAPK(File file) {
        ArrayList<String> arrayList;
        String filePathString = file.getPath().toString();
        ArrayList<String> arrayList2 = this.allOperatorApkList;
        if (arrayList2 == null || !arrayList2.contains(filePathString)) {
            return false;
        }
        if (!Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(this.mScanedOperator) && (arrayList = this.mAllScanedOperatorApplist) != null && arrayList.contains(filePathString)) {
            VSlog.i(TAG, "do not intercept:" + filePathString + " mAllScanedOperatorApplist:" + this.mAllScanedOperatorApplist);
            return false;
        }
        VSlog.i(TAG, "interceptInsertSimAPK:" + filePathString);
        return true;
    }

    private String getKey(Map<String, String> map, String value) {
        String key = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                String key2 = entry.getKey();
                key = key2;
            }
        }
        return key;
    }

    public void scanInstallOperatorApp(String fileString) {
        int parseFlags;
        int scanFlags;
        File file = new File(fileString);
        if (!file.exists()) {
            VSlog.i(TAG, "ScanInstallApp file not exists " + file);
            return;
        }
        boolean isPackage = (PackageParser.isApkFile(file) || file.isDirectory()) && !PackageInstallerService.isStageName(file.getName());
        if (!isPackage) {
            return;
        }
        File privilegedAppDir = new File(Environment.getRootDirectory(), "priv-app");
        File systemAppDir = new File(Environment.getRootDirectory(), "app");
        File oemAppDir = new File(Environment.getOemDirectory(), "app");
        if (FileUtils.contains(privilegedAppDir, file)) {
            int parseFlags2 = 1 | 16;
            int scanFlags2 = 4624 | Dataspace.STANDARD_BT601_625_UNADJUSTED;
            parseFlags = parseFlags2;
            scanFlags = scanFlags2;
        } else if (FileUtils.contains(systemAppDir, file)) {
            int parseFlags3 = 1 | 16;
            int scanFlags3 = 4624 | Dataspace.STANDARD_BT709;
            parseFlags = parseFlags3;
            scanFlags = scanFlags3;
        } else if (!FileUtils.contains(oemAppDir, file)) {
            parseFlags = 1;
            scanFlags = 4624;
        } else {
            int parseFlags4 = 1 | 16;
            int scanFlags4 = 4624 | Dataspace.STANDARD_BT601_525_UNADJUSTED;
            parseFlags = parseFlags4;
            scanFlags = scanFlags4;
        }
        try {
            try {
                AndroidPackage newPkg = this.mPms.scanPackageTracedLI(file, parseFlags, scanFlags, 0L, UserHandle.SYSTEM);
                if (newPkg == null) {
                    return;
                }
                String packageName = newPkg.getPackageName();
                if (!this.mPms.mSettings.mPackages.containsKey(packageName)) {
                    try {
                        this.mPms.updateSharedLibrariesLocked(newPkg, this.mPms.mSettings.getPackageLPr(newPkg.getPackageName()), (AndroidPackage) null, (PackageSetting) null, Collections.unmodifiableMap(this.mPms.mPackages));
                    } catch (PackageManagerException e) {
                        VSlog.e(TAG, "updateAllSharedLibrariesLPw failed: " + e.getMessage());
                    }
                    this.mPms.prepareAppDataAfterInstallLIF(newPkg);
                    synchronized (this.mPms.mLock) {
                        this.mPermissionManager.updatePermissions(packageName, newPkg);
                        if ("com.LogiaGroup.LogiaDeck".equals(packageName)) {
                            grantRuntimePermissionsForPkg(newPkg, true);
                        }
                        this.mPms.mSettings.writeLPr();
                    }
                    sendBroadCastAfterInstallVCustomSysAPpSuc(packageName);
                    if ("com.LogiaGroup.LogiaDeck".equals(packageName)) {
                        sendSpecialBroadCast();
                        return;
                    }
                    return;
                }
                VSlog.i(TAG, "ScanInstallApp do not need to scaninstall " + packageName);
            } catch (PackageManagerException e2) {
                e = e2;
                VSlog.w(TAG, "Failed to parse " + file + ": " + e.getMessage());
            }
        } catch (PackageManagerException e3) {
            e = e3;
        }
    }

    public boolean isNeedIntercept(String pkgName, int uid) {
        return RMPms.getInstance().isNeedIntercept(pkgName, uid);
    }

    public AndroidPackage scanPackageAbi(File scanFile, ApplicationInfo applicationInfo, long currentTime, UserHandle user) {
        AndroidPackage newPkg;
        int parserFlags;
        int scanFlags;
        try {
            try {
                VSlog.i("AppCrashRescue", "scanPackageAbi in pms.");
                this.mPms.reScanFlag = true;
                if (applicationInfo.isSystemApp()) {
                    parserFlags = 0 | 17;
                    scanFlags = 70160;
                } else {
                    parserFlags = 0 | 1;
                    scanFlags = 4624;
                }
                if (applicationInfo.isPrivilegedApp()) {
                    scanFlags |= Dataspace.STANDARD_BT601_625;
                }
                newPkg = this.mPms.scanPackageTracedLI(scanFile, parserFlags, scanFlags, currentTime, user);
            } catch (PackageManagerException e) {
                VSlog.i("AppCrashRescue", "catch ex in scanPackageAbi", e.fillInStackTrace());
                newPkg = null;
            }
            this.mPms.reScanFlag = false;
            AndroidPackage newPkg2 = newPkg;
            if (newPkg2 == null) {
                return null;
            }
            try {
                synchronized (this.mPms.mLock) {
                    this.mPms.updateSharedLibrariesLocked(newPkg2, this.mPms.mSettings.getPackageLPr(newPkg2.getPackageName()), (AndroidPackage) null, (PackageSetting) null, Collections.unmodifiableMap(this.mPms.mPackages));
                }
            } catch (PackageManagerException e2) {
                VSlog.e(TAG, "updateAllSharedLibrariesLPw failed: " + e2.getMessage());
            }
            this.mPms.prepareAppDataAfterInstallLIF(newPkg2);
            synchronized (this.mPms.mLock) {
                this.mPermissionManager.updatePermissions(newPkg2.getPackageName(), newPkg2);
                this.mPms.mSettings.writeLPr();
            }
            return newPkg2;
        } catch (Throwable th) {
            this.mPms.reScanFlag = false;
            throw th;
        }
    }

    public void forceDeleteOatFile(String pkgName) {
        this.mPms.deleteOatArtifactsOfPackage(pkgName);
        VSlog.i("AppCrashRescue", "forceDeleteOatFile in PKMS");
    }

    public boolean isPlanA() {
        return RMPms.getInstance().isPlanA();
    }

    public boolean isDexoptFailLessThreshold(String pkgName) {
        return RMPms.getInstance().isDexoptFailLessThreshold(pkgName);
    }

    public void deleteDexoptFail(String pkgName) {
        RMPms.getInstance().deleteDexoptFail(pkgName);
    }

    public void gatherDexOptInformation(String pkgName, int compilerFilter, boolean result, long startTime, int startTemperature) {
        RMPms.getInstance().gatherDexOptInformation(pkgName, compilerFilter, result, startTime, startTemperature);
    }
}