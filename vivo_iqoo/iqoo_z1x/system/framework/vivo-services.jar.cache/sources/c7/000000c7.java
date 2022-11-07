package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.ApplicationErrorReport;
import android.app.ContentProviderHolder;
import android.app.IVivoProcessObserver;
import android.app.admin.DevicePolicyManager;
import android.app.anr.ANRManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.IVivoTypeface;
import android.graphics.Typeface;
import android.graphics.fonts.SystemFonts;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.hardware.input.InputManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.VCarConfigManager;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.android.server.VivoFrameworkLockMonitor;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.anr.ANRManagerService;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.am.frozen.FrozenInjectorImpl;
import com.android.server.input.InputManagerService;
import com.android.server.policy.InputExceptionReport;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.android.server.uri.UriGrantsManagerService;
import com.android.server.wm.ActivityTaskManagerDebugConfig;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.VivoAppShareManager;
import com.android.server.wm.VivoEasyShareManager;
import com.android.server.wm.WindowManagerService;
import com.vivo.appshare.AppShareConfig;
import com.vivo.common.VivoCloudData;
import com.vivo.common.doubleinstance.DoubleInstanceConfig;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.framework.systemdefence.SystemDefenceManager;
import com.vivo.services.autorecover.SystemAutoRecoverManagerInternal;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import com.vivo.services.daemon.VivoDmServiceProxy;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.rms.RMAms;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.rms.sp.SpManagerImpl;
import com.vivo.services.security.server.VivoPermissionUtils;
import com.vivo.statistics.sdk.ArgPack;
import dalvik.system.VMRuntime;
import graphics.fonts.IVivoSystemFonts;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.app.VivoFrameworkFactory;
import vivo.app.epm.ExceptionPolicyManager;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoAmsImpl implements IVivoAms {
    static final String BBKLOG_ACTION = "android.vivo.bbklog.action.CHANGED";
    static final String BBKLOG_STATUS = "adblog_status";
    private static final String CLONE_USER_PATH = "/storage/emulated/999";
    static final long CONTENT_PROVIDER_TIMEOUT = 50000;
    private static final String CRASH_TAG = "AppCrashRescue";
    static final int DROPBOX_DEFAULT_MAX_SIZE = 786432;
    public static final int FIX_MODE_USER_ID = 888;
    static final int MAXACTIVITIES = 60;
    static final int MAXSAMEACTIVITIES = 20;
    private static final String SPS_CRASH_HANDLE_CLASS = "com.vivo.sp.SpCrashHandler";
    static final String TAG = "VivoAmsImpl";
    static final int TOMBSTONES_LOG_SIZE = 196608;
    private static final String UPDATE_FONT = "update_font";
    private static final String VIRTUAL_CLONE_USER_PATH = "/storage/caf-999";
    private static final String VIVO_CMD_PREFIX = "#@VIVO_CMD@#";
    public static AbsVivoPerfManager mAnimPerf;
    public static AbsVivoPerfManager mUxPerf;
    public ANRManager mANRManager;
    private ActivityManagerService mAms;
    private AmsConfigManager mAmsConfigManager;
    private AmsDataManager mAmsDataManager;
    private AppCrashRescueUtil mAppCrashRescueUtil;
    private AppsHandler mAppsHandler;
    public Context mContext;
    private DoubleInstanceConfig mDoubleInstanceConfig;
    private EmergencyBroadcastManager mEmergencyBroadcastManager;
    private String mInstallerPackageName;
    final ProcessList mProcessList;
    private VivoAppShareManager mVivoAppShareManager;
    private VivoCloudData mVivoCloudData;
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService;
    private VivoFirewall mVivoFirewall;
    private VivoFrozenPackageSupervisor mVivoFrozenPackageSupervisor;
    private HandlerThread vivoThread;
    static final File TOMBSTONE_DIR = new File("/data/tombstones");
    private static boolean mPlus = true;
    public static AbsVivoPerfManager mPerfServiceStartHint = null;
    public static boolean mForceStopKill = false;
    private static Map<String, String> binderToProcessNameMap = new HashMap<String, String>() { // from class: com.android.server.am.VivoAmsImpl.11
        {
            put("IWindowManager", "system_server");
            put("IWindowSession", "system_server");
            put("ActivityManagerProxy", "system_server");
            put("IActivityManager", "system_server");
            put("IInputMethodManager", "system_server");
            put("IStatusBarService", "system_server");
            put("IPackageManager", "system_server");
            put("IDevicePolicyManager", "system_server");
            put("IPowerManager", "system_server");
            put("IDisplayManager", "system_server");
            put("ILockSettings", "system_server");
            put("IKeyguardStateCallback", "system_server");
            put("IDreamManager", "system_server");
            put("IVivoPerfService", "system_server");
            put("ThreadedRenderer", "surfaceflinger");
            put("SurfaceControl", "surfaceflinger");
            put("SubscriptionManager", "com.android.server.telecom");
            put("ITelephony", "com.android.server.telecom");
            put("ITelecomService", "com.android.server.telecom");
            put("SystemSensorManager", "sensors");
            put("IAudioService", "audioserver");
            put("AudioManager", "audioserver");
            put("MediaPlayer", "mediaserver");
            put("ISessionController", "mediaserver");
            put("SoundPool", "mediaserver");
        }
    };
    ArrayList<String> mKillBgServiceAppList = new ArrayList<String>() { // from class: com.android.server.am.VivoAmsImpl.1
        {
            add("com.android.bbkmusic");
            add("com.vivo.smartshot");
        }
    };
    final ArrayList<String> PKG_DEPENDENCY_WHITELIST = new ArrayList<String>() { // from class: com.android.server.am.VivoAmsImpl.2
        {
            add("com.bbk.launcher2");
            add("com.vivo.hiboard");
        }
    };
    final ArrayList<String> SOTER_ALIPAY_WHITELIST = new ArrayList<String>() { // from class: com.android.server.am.VivoAmsImpl.3
        {
            add("com.tencent.soter.soterserver.ISoterService");
            add("com.tencent.soter.soterserver.IAlipayService");
            add("org.ifaa.aidl.manager.IfaaManagerService");
        }
    };
    final String FP_MESSENGER_SERVICE = "com.vivo.fingerprintui.export.MessengerService";
    String[] whiteList = {"org.ironrabbit.bhoboard", "com.vlife.vivo.wallpaper", "com.idddx.lwp.vivo", "com.vivo.bsptest", "com.vivo.monkeytest", "com.vivo.apitest", "com.test.daemon", "inputmethod", "softkeyboard", "keyboard", "com.baidu.input", "com.tencent.qqpinyin", "com.redteamobile.virtual.softsim", "com.bbk.theme", "com.vivo.ai.ime"};
    String[] stopPkgs = {"com.android.email", "com.vivo.email", "com.bbk.account", "com.android.browser", "com.vivo.browser", "com.android.BBKPhoneInstructions", "com.android.filemanager", "com.vivo.ewarranty", VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.bbk.iqoo.appanalysis", "com.chaozh.iReader", "com.bbk.appstore", "com.bbk.iqoo.logsystem", "com.vivo.msgpush", "com.vivo.space", "com.vivo.game", "com.bbk.VoiceAssistant", "com.bbk.cloud", "com.vivo.Tips", "com.android.htmlviewer"};
    String[] restartPkgs = {"com.bbk.scene.indoor", "com.bbk.launcher2", "com.vivo.simplelauncher", "com.bbk.scene.tech", "com.bbk.scene.interstellar", "com.viber.voip"};
    UserController mUserController = null;
    private String mAppSharePackageName = null;
    private int mAppShareUserId = -1;
    private final String ACTION_BBK_SYSTEM_UPDATE = "bbk.receiver.action.SYSTEM_UPDATE";
    private boolean mW2mShieldBroadcast = false;
    ArrayMap<String, ProviderPublishTimedOutRecord> mPublishTimedOutRecords = new ArrayMap<>();

    public VivoAmsImpl(ActivityManagerService ams, Context ctx, ProcessList processList) {
        this.mVivoDoubleInstanceService = null;
        this.mDoubleInstanceConfig = null;
        this.mVivoCloudData = null;
        if (ams == null) {
            Slog.i(TAG, "container is " + ams);
        }
        this.mAms = ams;
        this.mProcessList = processList;
        RMAms.getInstance().initialize(ams, this, ctx);
        this.mEmergencyBroadcastManager = new EmergencyBroadcastManager(ams);
        this.mContext = ctx;
        VivoFirewall vivoFirewall = VivoFirewall.getInstance(ctx);
        this.mVivoFirewall = vivoFirewall;
        vivoFirewall.init(ams);
        this.mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
        this.mDoubleInstanceConfig = DoubleInstanceConfig.getInstance();
        HandlerThread handlerThread = new HandlerThread("ThermalGame");
        this.vivoThread = handlerThread;
        handlerThread.start();
        this.mAppsHandler = new AppsHandler(this.vivoThread.getLooper());
        mUxPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        mAnimPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        SystemDefenceHelper.getInstance().init(ams);
        this.mAmsDataManager = AmsDataManager.getInstance();
        VivoFrozenPackageSupervisor vivoFrozenPackageSupervisor = VivoFrozenPackageSupervisor.getInstance();
        this.mVivoFrozenPackageSupervisor = vivoFrozenPackageSupervisor;
        vivoFrozenPackageSupervisor.init(ams, this.mContext);
        FrozenInjectorImpl.getInstance().initialize(ctx);
        VivoAppShareManager vivoAppShareManager = VivoAppShareManager.getInstance();
        this.mVivoAppShareManager = vivoAppShareManager;
        if (vivoAppShareManager != null) {
            vivoAppShareManager.initAms(ams);
        }
        this.mVivoCloudData = VivoCloudData.getInstance(this.mContext);
    }

    public void dummy() {
        Slog.i(TAG, "dummy, this=" + this);
    }

    public void systemReady() {
        RmsInjectorImpl.getInstance().systemReady();
        this.mEmergencyBroadcastManager.systemReady();
        this.mVivoFirewall.systemReady();
        this.mVivoFrozenPackageSupervisor.systemReady();
        AppCrashRescueUtil instance = AppCrashRescueUtil.setInstance(this.mContext, this.mAms);
        this.mAppCrashRescueUtil = instance;
        instance.init();
        VivoFrameworkLockMonitor.getInstance().systemReady();
    }

    public void initAmsConfigManager() {
        this.mAmsConfigManager = AmsConfigManager.getInstance();
    }

    public boolean isBgStartAllowed(String pkgName) {
        AmsConfigManager amsConfigManager = this.mAmsConfigManager;
        if (amsConfigManager != null) {
            return amsConfigManager.isBgStartAllowed(pkgName);
        }
        return false;
    }

    public List<String> getForceStopAppList() {
        AmsConfigManager amsConfigManager = this.mAmsConfigManager;
        if (amsConfigManager != null) {
            return amsConfigManager.getForceStopAppList();
        }
        return null;
    }

    public List<String> getAnrMonitorPackageList() {
        AmsConfigManager amsConfigManager = this.mAmsConfigManager;
        if (amsConfigManager != null) {
            return amsConfigManager.getAnrMonitorPackageList();
        }
        return null;
    }

    public List<String> getAnrDumpTracePersistentProcesses() {
        AmsConfigManager amsConfigManager = this.mAmsConfigManager;
        if (amsConfigManager != null) {
            return amsConfigManager.getAnrDumpTracePersistentProcesses();
        }
        return null;
    }

    public List<String> getAnrDumpTraceNativeProcesses() {
        AmsConfigManager amsConfigManager = this.mAmsConfigManager;
        if (amsConfigManager != null) {
            return amsConfigManager.getAnrDumpTraceNativeProcesses();
        }
        return null;
    }

    public void dumpAmsConfigList(PrintWriter pw, String[] args, int opti) {
        AmsConfigManager amsConfigManager = this.mAmsConfigManager;
        if (amsConfigManager != null) {
            amsConfigManager.dumpConfigList(pw, args, opti);
        }
    }

    public boolean isEnableAnrMonitor(String name) {
        if (getAnrMonitorPackageList() == null) {
            return false;
        }
        for (String pkg : getAnrMonitorPackageList()) {
            if (pkg.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void checkNotifyForForceStop(Handler handler, String packageName, String reason, final int userId) {
        if (this.mAms.mSystemReady && getForceStopAppList() != null && getForceStopAppList().contains(packageName) && reason != null && reason.startsWith("stop") && !reason.startsWith("stop user")) {
            final Intent mIntent = new Intent("vivo.intent.action.FORCE_STOP_PACKAGE." + packageName);
            mIntent.putExtra("FORCE_STOP_PACKAGENAME", packageName);
            mIntent.addFlags(Dataspace.TRANSFER_GAMMA2_2);
            if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                Slog.d(TAG, "FORCE_STOP_PACKAGENAME " + this.mAms.isClearData);
            }
            if (this.mAms.isClearData) {
                mIntent.putExtra("IS_CLEAR_DATA", this.mAms.isClearData);
                this.mAms.isClearData = false;
            }
            if (handler != null) {
                handler.postDelayed(new Runnable() { // from class: com.android.server.am.VivoAmsImpl.4
                    @Override // java.lang.Runnable
                    public void run() {
                        VivoAmsImpl.this.mAms.mContext.sendBroadcastAsUser(mIntent, UserHandle.of(userId));
                    }
                }, 3000L);
            }
        }
    }

    public void checkNotifyForKillService(String pkgName) {
        if (this.mAms.mSystemReady && this.mKillBgServiceAppList.contains(pkgName)) {
            Slog.i(TAG, "calling stop services !!!!");
            Intent mIntent = new Intent("android.intent.action.KILL_BACKGROUND_SERVICE." + pkgName);
            mIntent.putExtra("pkgName", pkgName);
            this.mAms.mContext.sendBroadcastAsUser(mIntent, UserHandle.getUserHandleForUid(Binder.getCallingUid()));
        }
    }

    public boolean isInActivityNumControl() {
        AmsConfigManager amsConfigManager = this.mAmsConfigManager;
        if (amsConfigManager != null) {
            return amsConfigManager.isInActivityNumControl();
        }
        return false;
    }

    public int getMaxActiviesInTask() {
        AmsConfigManager amsConfigManager = this.mAmsConfigManager;
        if (amsConfigManager != null) {
            return amsConfigManager.getMaxActiviesInTask();
        }
        return 60;
    }

    public int getMaxSameActivitiesInTask() {
        AmsConfigManager amsConfigManager = this.mAmsConfigManager;
        if (amsConfigManager != null) {
            return amsConfigManager.getMaxSameActivitiesInTask();
        }
        return 20;
    }

    public boolean dumpBroadcastsLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        return this.mEmergencyBroadcastManager.dumpBroadcastsLocked(fd, pw, args, opti, dumpAll, dumpPackage);
    }

    public BroadcastQueue broadcastQueueForIntent(Intent intent, String callerApp, BroadcastQueue emergencyBroadcastQueue, BroadcastQueue fgKeyAppBroadcastQueue, BroadcastQueue bgKeyAppBroadcastQueue) {
        return this.mEmergencyBroadcastManager.broadcastQueueForIntent(intent, callerApp, emergencyBroadcastQueue, fgKeyAppBroadcastQueue, bgKeyAppBroadcastQueue);
    }

    public BroadcastRecord getMatchingOrderedReceiver(IBinder who, int flags, BroadcastQueue emergencyBroadcastQueue, BroadcastQueue fgKeyAppBroadcastQueue, BroadcastQueue bgKeyAppBroadcastQueue) {
        return this.mEmergencyBroadcastManager.getMatchingOrderedReceiver(who, flags, emergencyBroadcastQueue, fgKeyAppBroadcastQueue, bgKeyAppBroadcastQueue);
    }

    public void vivoKillProcess(int pid, int uid) {
        int puid = Process.getUidForPid(pid);
        if (puid == uid) {
            Process.killProcess(pid);
            return;
        }
        Slog.w(TAG, "The uid of process:" + pid + " is " + puid + "does not match uid " + uid);
    }

    public void vivoKillProcessQuiet(int pid, int uid) {
        int puid = Process.getUidForPid(pid);
        if (puid == uid) {
            Process.killProcessQuiet(pid);
            return;
        }
        Slog.w(TAG, "The uid of process:" + pid + " is " + puid + " does not match uid " + uid + " during appDiedLocked.");
    }

    public boolean shouldDependPkg(ProcessRecord proc, String packageName) {
        ActivityManagerService activityManagerService = this.mAms;
        ActivityTaskManagerService mAtm = activityManagerService != null ? activityManagerService.mActivityTaskManager : null;
        boolean result = ((mAtm == null || mAtm.getAtmInternal().getHomeProcess() == null || proc.pid != mAtm.getAtmInternal().getHomeProcess().getPid()) ? false : true) | this.PKG_DEPENDENCY_WHITELIST.contains(proc.processName);
        if (result) {
            VSlog.d(TAG, "Skip PackageDependency callingProcess:" + proc.processName + " packageName:" + packageName);
        }
        return result;
    }

    public void setAmsDumpTimeout() {
        VivoAmsUtils.setAmsDumpTimeout();
    }

    public void cancelAmsDumpTimeout() {
        VivoAmsUtils.cancelAmsDumpTimeout();
    }

    public String getProviderType(ContentProviderHolder holder, Uri uri) {
        try {
            String type = holder.provider.getType(uri);
            if (ActivityTaskManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG, "return providerType : " + type);
            }
            return type;
        } catch (Exception e) {
            Slog.w(TAG, "Exception while determining type of " + uri, e);
            return null;
        }
    }

    public Runnable postKillerRunnable(ProviderMap mProviderMap, String name, int userId) {
        final ContentProviderRecord cpr = mProviderMap.getProviderByName(name, userId);
        if (cpr != null && cpr.proc != null) {
            String mProviderProcessName = cpr.proc.processName;
            if (ActivityTaskManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG, "providerPid pid : " + cpr.proc.pid + " ,processName : " + mProviderProcessName);
            }
        }
        Runnable runnable = new Runnable() { // from class: com.android.server.am.VivoAmsImpl.5
            @Override // java.lang.Runnable
            public void run() {
                ContentProviderRecord contentProviderRecord = cpr;
                if (contentProviderRecord != null && contentProviderRecord.proc != null) {
                    int killingProviderPid = cpr.proc.pid;
                    Slog.i(VivoAmsImpl.TAG, "killingProviderPid: " + killingProviderPid);
                    if (killingProviderPid <= 0 || killingProviderPid == Process.myPid()) {
                        return;
                    }
                    Slog.i(VivoAmsImpl.TAG, "kill contentprovider process because of timeout providerPid: " + killingProviderPid);
                    Process.killProcess(killingProviderPid);
                }
            }
        };
        ProcessList.sKillHandler.postDelayed(runnable, (long) CONTENT_PROVIDER_TIMEOUT);
        return runnable;
    }

    public void removeCallBack(Runnable runnable) {
        if (runnable != null) {
            ProcessList.sKillHandler.removeCallbacks(runnable);
        }
    }

    public void firewallStart() {
        this.mVivoFirewall.firewallStart();
    }

    public boolean startProviderCheck(ProcessRecord callerProcessRecord, ProviderInfo providerInfo, String name, int userId) {
        if (shouldPreventStartProvider(callerProcessRecord, providerInfo, name, userId)) {
            return false;
        }
        thirdLifeControlProvider(callerProcessRecord, providerInfo);
        return true;
    }

    public boolean startServiceCheck(ProcessRecord callerProcessRecord, ServiceRecord serviceRecord, int callingUidFilter, int callingPidFilter) {
        if (shouldPreventStartService(callerProcessRecord, serviceRecord, callingUidFilter, callingPidFilter)) {
            this.mAms.mServices.bringDownServiceLocked(serviceRecord);
            return false;
        }
        thirdLifeControlService(callerProcessRecord, serviceRecord);
        return true;
    }

    public boolean startBroadcastCheck(BroadcastRecord broadcastRecord, ComponentInfo callingComponent) {
        thirdLifeControlBroadcast(broadcastRecord, callingComponent);
        if (broadcastRecord.intent.getIsVivoWidget()) {
            noteImportantEvent(2, callingComponent.packageName);
            return true;
        }
        return true;
    }

    public boolean startInstrumentCheck(int callerPid, int callerUid, ComponentName className, ApplicationInfo calledInfo) {
        ApplicationInfo callerInfo = null;
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            String[] packageNames = pm.getPackagesForUid(callerUid);
            callerInfo = packageNames != null ? pm.getApplicationInfo(packageNames[0], 0, 0) : null;
        } catch (Exception e) {
            VSlog.w(TAG, "get callerAppInfo error uid: " + callerUid);
        }
        if (shouldPreventStartInstrument(callerPid, callerUid, callerInfo, className, calledInfo)) {
            return false;
        }
        thirdLifeControlInstrument(callerInfo, className, calledInfo);
        return true;
    }

    private boolean shouldPreventStartInstrument(int callerPid, int callerUid, ApplicationInfo callerInfo, ComponentName className, ApplicationInfo calledInfo) {
        long beginTime = SystemClock.uptimeMillis();
        if (callerInfo != null && calledInfo != null && (((callerInfo.flags & KernelConfig.AP_TE) == 0 || this.mVivoFirewall.isSystemAppControlled(callerInfo.packageName)) && ((calledInfo.flags & KernelConfig.AP_TE) == 0 || this.mVivoFirewall.isSystemAppControlled(calledInfo.packageName)))) {
            ComponentInfo calledCompInfo = new ComponentInfo();
            calledCompInfo.applicationInfo = calledInfo;
            calledCompInfo.packageName = className.getPackageName();
            calledCompInfo.name = className.getClassName();
            if (this.mVivoFirewall.shouldPreventStartProcess(callerInfo.packageName, calledCompInfo, VivoFirewall.TYPE_INSTRUMENT, callerPid, callerUid)) {
                VSlog.w(TAG, "==/==> " + calledInfo.packageName + "/" + calledInfo.uid + " for instrument " + calledCompInfo.name + ": callingPkg " + callerInfo.packageName + ": XXXX");
                checkTime(beginTime, "instrument shouldPreventStartProcess");
                return true;
            }
        }
        checkTime(beginTime, "instrument shouldPreventStartProcess");
        return false;
    }

    private boolean shouldPreventStartProvider(ProcessRecord callerProcessRecord, ProviderInfo providerInfo, String name, int userId) {
        String str;
        String str2;
        String str3;
        String str4;
        String str5;
        long beginTime;
        String str6;
        long beginTime2;
        VivoAmsImpl vivoAmsImpl;
        long beginTime3 = SystemClock.uptimeMillis();
        if (callerProcessRecord == null || callerProcessRecord.info == null || providerInfo.applicationInfo == null) {
            str = "provider shouldPreventStartProcess";
            str2 = TAG;
            str3 = "==/==> ";
            str4 = "/";
            str5 = ": user ";
            beginTime = beginTime3;
        } else if ((callerProcessRecord.info.flags & KernelConfig.AP_TE) != 0 && !this.mVivoFirewall.isSystemAppControlled(callerProcessRecord.info.packageName)) {
            str = "provider shouldPreventStartProcess";
            str2 = TAG;
            str3 = "==/==> ";
            str4 = "/";
            str5 = ": user ";
            beginTime = beginTime3;
        } else if ((providerInfo.applicationInfo.flags & KernelConfig.AP_TE) == 0 || this.mVivoFirewall.isSystemAppControlled(providerInfo.packageName)) {
            if (this.mVivoFirewall.shouldPreventStartProcess(callerProcessRecord.info.packageName, providerInfo, VivoFirewall.TYPE_PROVIDER, callerProcessRecord.pid, Binder.getCallingUid())) {
                VSlog.w(TAG, "==/==> " + providerInfo.applicationInfo.packageName + "/" + providerInfo.applicationInfo.uid + " for provider " + name + ": user " + userId + ": XXXX");
                checkTime(beginTime3, "provider shouldPreventStartProcess");
                return true;
            } else if (!this.mVivoFirewall.shouldPreventAppInteraction(callerProcessRecord.info.packageName, providerInfo, VivoFirewall.TYPE_PROVIDER, callerProcessRecord.pid, Binder.getCallingUid())) {
                vivoAmsImpl = this;
                beginTime2 = beginTime3;
                str6 = "provider shouldPreventStartProcess";
                vivoAmsImpl.checkTime(beginTime2, str6);
                return false;
            } else {
                Slog.w(TAG, "==/==> " + providerInfo.applicationInfo.packageName + "/" + providerInfo.applicationInfo.uid + " for app isolation provider " + name + ": user " + userId + ": XXXX");
                VivoFirewall.checkTime(beginTime3, "provider shouldPreventAppInteraction");
                return true;
            }
        } else {
            str = "provider shouldPreventStartProcess";
            str2 = TAG;
            str3 = "==/==> ";
            str4 = "/";
            str5 = ": user ";
            beginTime = beginTime3;
        }
        if (callerProcessRecord != null || providerInfo.applicationInfo == null || (providerInfo.applicationInfo.flags & KernelConfig.AP_TE) != 0) {
            vivoAmsImpl = this;
            beginTime2 = beginTime;
            str6 = str;
        } else {
            String str7 = str3;
            vivoAmsImpl = this;
            if (vivoAmsImpl.mVivoFirewall.getThreadLocalInteger() == null) {
                beginTime2 = beginTime;
                str6 = str;
            } else {
                VivoFirewall vivoFirewall = vivoAmsImpl.mVivoFirewall;
                long beginTime4 = beginTime;
                if (!vivoFirewall.shouldPreventStartProcess(null, providerInfo, VivoFirewall.TYPE_PROVIDER, 0, vivoFirewall.getThreadLocalInteger().intValue())) {
                    str6 = str;
                    beginTime2 = beginTime4;
                } else {
                    VSlog.w(str2, str7 + providerInfo.applicationInfo.packageName + str4 + providerInfo.applicationInfo.uid + " for provider " + name + str5 + userId + ": XXXX");
                    vivoAmsImpl.checkTime(beginTime4, str);
                    return true;
                }
            }
        }
        vivoAmsImpl.checkTime(beginTime2, str6);
        return false;
    }

    private boolean shouldPreventStartService(ProcessRecord callerProcessRecord, ServiceRecord serviceRecord, int callingUidFilter, int callingPidFilter) {
        String str;
        ProcessRecord callerProcessRecord2 = callerProcessRecord;
        int callingUidFilter2 = callingUidFilter;
        long beginTime = SystemClock.uptimeMillis();
        if (callerProcessRecord2 != null) {
            VSlog.w(TAG, "mCallerApp.pid =" + callerProcessRecord2.pid + ",mCallerApp.uid=" + callerProcessRecord2.uid);
        }
        if (serviceRecord.callerApp != null) {
            callerProcessRecord2 = serviceRecord.callerApp;
            VSlog.w(TAG, "DelayStartedService : mCallerApp.pid =" + callerProcessRecord2.pid + ",mCallerApp.uid=" + callerProcessRecord2.uid);
            serviceRecord.callerApp = null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("mCallingPidFilter=");
        int callingPidFilter2 = callingPidFilter;
        sb.append(callingPidFilter2);
        sb.append(",mCallingUidFilter=");
        sb.append(callingUidFilter2);
        VSlog.w(TAG, sb.toString());
        String type = null;
        String callerPackage = null;
        if (callerProcessRecord2 != null && callerProcessRecord2.info != null && callingUidFilter2 == 1000 && VivoPermissionUtils.OS_PKG.equals(callerProcessRecord2.info.packageName) && serviceRecord.appInfo != null && ((serviceRecord.appInfo.flags & KernelConfig.AP_TE) == 0 || this.mVivoFirewall.isSystemAppControlled(serviceRecord.packageName))) {
            String action = serviceRecord.intent.getIntent().getAction();
            callerPackage = VivoPermissionUtils.OS_PKG;
            if (action != null) {
                if (action.equals("android.vivo.JobService")) {
                    int jobuid = serviceRecord.intent.getIntent().getIntExtra("uid", 1000);
                    callingUidFilter2 = jobuid;
                    type = VivoFirewall.TYPE_SYSTEM_JOB;
                } else if (action.equals("android.content.SyncAdapter")) {
                    String reason = serviceRecord.intent.getIntent().getStringExtra("reason");
                    VSlog.w(TAG, "reason = " + reason);
                    if (this.mVivoFirewall.shouldValidateSyncType(reason)) {
                        type = VivoFirewall.TYPE_SYSTEM_SYNC;
                    }
                } else if (action.equals("android.accounts.AccountAuthenticator")) {
                    type = VivoFirewall.TYPE_SYSTEM_ACCO;
                } else if (action.equals("android.service.notification.NotificationListenerService")) {
                    type = VivoFirewall.TYPE_SYSTEM_NOTI;
                }
            }
        } else if (callerProcessRecord2 != null && callerProcessRecord2.info != null && serviceRecord.appInfo != null && (((callerProcessRecord2.info.flags & KernelConfig.AP_TE) == 0 || this.mVivoFirewall.isSystemAppControlled(callerProcessRecord2.info.packageName)) && ((serviceRecord.appInfo.flags & KernelConfig.AP_TE) == 0 || this.mVivoFirewall.isSystemAppControlled(serviceRecord.packageName)))) {
            type = VivoFirewall.TYPE_SERVICE;
            callerPackage = callerProcessRecord2.info.packageName;
            callingPidFilter2 = callerProcessRecord2.pid;
            callingUidFilter2 = callerProcessRecord2.uid;
        } else if (callerProcessRecord2 == null && serviceRecord.appInfo != null && (serviceRecord.appInfo.flags & KernelConfig.AP_TE) == 0 && (serviceRecord.appInfo.uid != callingUidFilter2 || !serviceRecord.intent.getIntent().getIsVivoWidget())) {
            VSlog.w(TAG, "mCallerApp null");
            callerPackage = null;
            type = VivoFirewall.TYPE_SERVICE;
        }
        if (type == null) {
            str = " shouldPreventStartProcess";
        } else {
            if (this.mVivoFirewall.shouldPreventStartProcess(callerPackage, serviceRecord.serviceInfo, type, callingPidFilter2, callingUidFilter2)) {
                String msg = "==/==>: " + serviceRecord.appInfo.packageName + "/" + serviceRecord.appInfo.uid + " for " + type + " callerPackage " + callerPackage + " " + serviceRecord.intent.getIntent() + ": XXXX";
                VSlog.w(TAG, msg);
                VivoFirewall.checkTime(beginTime, type + " shouldPreventStartProcess");
                return true;
            } else if (!VivoFirewall.TYPE_SERVICE.equals(type) || TextUtils.isEmpty(callerPackage)) {
                str = " shouldPreventStartProcess";
            } else {
                str = " shouldPreventStartProcess";
                if (!this.mVivoFirewall.shouldPreventAppInteraction(callerPackage, serviceRecord.serviceInfo, type, callingPidFilter2, callingUidFilter2)) {
                    beginTime = beginTime;
                } else {
                    Slog.w(TAG, "==/==>: " + serviceRecord.appInfo.packageName + "/" + serviceRecord.appInfo.uid + " for app isolation " + type + " callerPackage " + callerPackage + " " + serviceRecord.intent.getIntent() + ": XXXX");
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(type);
                    sb2.append(" shouldPreventAppInteraction");
                    VivoFirewall.checkTime(beginTime, sb2.toString());
                    return true;
                }
            }
        }
        VivoFirewall.checkTime(beginTime, type + str);
        return false;
    }

    private void thirdLifeControlService(ProcessRecord callerProcessRecord, ServiceRecord serviceRecord) {
        String callerPkg = null;
        int callerUid = 0;
        if (callerProcessRecord != null && callerProcessRecord.info != null) {
            callerPkg = callerProcessRecord.info.packageName;
            callerUid = callerProcessRecord.uid;
        }
        if (serviceRecord != null && serviceRecord.appInfo != null) {
            String calledPkg = serviceRecord.appInfo.packageName;
            String calledClassName = serviceRecord.serviceInfo.name;
            int calledUid = serviceRecord.appInfo.uid;
            this.mVivoFirewall.sendThirdLifeControlIntent(callerPkg, callerUid, calledPkg, calledUid, calledClassName, VivoFirewall.TYPE_SERVICE);
        }
    }

    private void thirdLifeControlProvider(ProcessRecord callerProcessRecord, ComponentInfo callingComponent) {
        if (this.mAms.mSystemReady) {
            String callerPkg = null;
            int callerUid = 0;
            if (callerProcessRecord != null && callerProcessRecord.info != null) {
                callerPkg = callerProcessRecord.info.packageName;
                callerUid = Binder.getCallingUid();
            } else if (this.mVivoFirewall.getThreadLocalInteger() != null) {
                callerUid = this.mVivoFirewall.getThreadLocalInteger().intValue();
            }
            if (callingComponent != null && callingComponent.applicationInfo != null) {
                String calledPkg = callingComponent.applicationInfo.packageName;
                String calledClassName = callingComponent.name;
                int calledUid = callingComponent.applicationInfo.uid;
                this.mVivoFirewall.sendThirdLifeControlIntent(callerPkg, callerUid, calledPkg, calledUid, calledClassName, VivoFirewall.TYPE_PROVIDER);
            }
        }
    }

    public void thirdLifeControlBroadcast(BroadcastRecord broadcastRecord, ComponentInfo callingComponent) {
        String callerPkg = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        int callerUid = -1;
        if (broadcastRecord != null) {
            callerPkg = broadcastRecord.callerPackage;
            callerUid = broadcastRecord.callingUid;
        }
        if (callingComponent != null) {
            String calledPkg = callingComponent.packageName;
            String calledClassName = callingComponent.name;
            int calledUid = callingComponent.applicationInfo.uid;
            this.mVivoFirewall.sendThirdLifeControlIntent(callerPkg, callerUid, calledPkg, calledUid, calledClassName, "broadcast");
        }
    }

    public void thirdLifeControlInstrument(ApplicationInfo callerInfo, ComponentName className, ApplicationInfo calledInfo) {
        String callerPkg = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        int callerUid = -1;
        if (callerInfo != null) {
            callerPkg = callerInfo.packageName;
            callerUid = callerInfo.uid;
        }
        if (calledInfo != null) {
            String calledPkg = calledInfo.packageName;
            String calledClassName = className.getClassName();
            int calledUid = calledInfo.uid;
            this.mVivoFirewall.sendThirdLifeControlIntent(callerPkg, callerUid, calledPkg, calledUid, calledClassName, VivoFirewall.TYPE_INSTRUMENT);
        }
    }

    public void noteImportantEvent(int eventType, String packageName) {
        this.mVivoFirewall.noteImportantEvent(eventType, packageName);
    }

    public int getBringupContinuousSwitch() {
        return this.mVivoFirewall.getBringupContinuousSwitch();
    }

    public void dumpCachedInfo(FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        this.mVivoFirewall.dumpCachedInfo(fd, pw, args, opti);
    }

    public void checkTime(long startTime, String where) {
        VivoFirewall.checkTime(startTime, where);
    }

    public void setFirewallInteger(Integer integer) {
        this.mVivoFirewall.setThreadLocalInteger(integer);
    }

    public Integer getFirewallInteger() {
        return this.mVivoFirewall.getThreadLocalInteger();
    }

    public void removeFirewallInteger() {
        this.mVivoFirewall.removeThreadLocalInteger();
    }

    public boolean hasForegroundWindow(String packageName) {
        return this.mAms.mWindowManager.hasForegroundWindow(packageName);
    }

    public void requestFrozen(String packageName, boolean isFrozen) {
        this.mVivoFrozenPackageSupervisor.requestFrozen(packageName, isFrozen, -1);
    }

    public void requestFrozen(String packageName, boolean isFrozen, int flag) {
        this.mVivoFrozenPackageSupervisor.requestFrozenWithFlag(packageName, isFrozen, flag, -1);
    }

    public void setFrozenEnable(boolean frozenEnable) {
        this.mVivoFrozenPackageSupervisor.setFrozenEnable(frozenEnable);
    }

    public void setVmrEnable(boolean enable) {
        this.mVivoFrozenPackageSupervisor.setVmrEnable(enable);
    }

    public boolean isFrozenEnable() {
        return this.mVivoFrozenPackageSupervisor.isEnableFunction();
    }

    public boolean isFrozenPackage(String packageName, int uid) {
        return this.mVivoFrozenPackageSupervisor.isFrozenPackage(packageName, uid);
    }

    public boolean setCallerWhiteList(List<String> pkgNames) {
        return this.mVivoFrozenPackageSupervisor.setCallerWhiteList(pkgNames);
    }

    public boolean setBeCalledWhiteList(List<String> pkgNames) {
        return this.mVivoFrozenPackageSupervisor.setBeCalledWhiteList(pkgNames);
    }

    public boolean setInterfaceWhiteList(List<String> pkgNames) {
        return this.mVivoFrozenPackageSupervisor.setInterfaceWhiteList(pkgNames);
    }

    public boolean isFrozenProcess(ProcessRecord app) {
        if (app != null && app.info != null && app.info.packageName != null) {
            return isFrozenPackage(app.info.packageName, app.info.uid);
        }
        return false;
    }

    public boolean shutDown(int timeout) {
        if (timeout == 0) {
            this.mVivoFrozenPackageSupervisor.unfrozenAllPackages();
            return true;
        }
        return false;
    }

    public void dumpFrozenPackageLocked(PrintWriter pw, String[] args, int opti) {
        synchronized (this.mAms) {
            this.mVivoFrozenPackageSupervisor.dumpFrozenPackageLocked(pw);
        }
    }

    public boolean isFrozenProcess(ProcessRecord app, PrintWriter pw) {
        if (app != null && app.info != null && app.info.packageName != null && this.mVivoFrozenPackageSupervisor.isFrozenPackage(app.info.packageName, app.info.uid)) {
            if (pw != null) {
                pw.println("\n** this package: " + app.info.packageName + " process: " + app.processName + " has been frozen **");
                return true;
            }
            return true;
        }
        return false;
    }

    public void dumpBinderProxy() {
        VSlog.d(TAG, "force unfrozenAllPackages for dumpBinderProxy");
        this.mVivoFrozenPackageSupervisor.unfrozenAllPackages();
    }

    /* loaded from: classes.dex */
    class AppsHandler extends Handler {
        public AppsHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
        }
    }

    public HandlerThread getVivoThread() {
        return this.vivoThread;
    }

    public AppsHandler getAppsHandler() {
        return this.mAppsHandler;
    }

    public void appDiedLockedBoost(String packageName, int pid, boolean mNotResponding, boolean mCrashing) {
        AbsVivoPerfManager absVivoPerfManager = mUxPerf;
        if (absVivoPerfManager != null && !mForceStopKill && !mNotResponding && !mCrashing) {
            absVivoPerfManager.perfUXEngine_events(4, 0, packageName, 0);
        }
        AbsVivoPerfManager absVivoPerfManager2 = mUxPerf;
        if (absVivoPerfManager2 != null) {
            absVivoPerfManager2.perfHintAsync(4243, packageName, pid, 0);
        }
    }

    public void appDiedLockedBoostStop() {
        mForceStopKill = true;
    }

    public void attachApplicationLockedBoost(ProcessRecord app) {
        if (mUxPerf != null && app.hostingRecord != null && app.hostingRecord.isTopApp()) {
            mUxPerf.perfHintAsync(4225, app.processName, app.pid, (int) KernelConfig.DBG_TARGET_REGADDR_VALUE_GET);
        }
    }

    public int getDex2oatAppropriateStatus() {
        InputManagerInternal mInputManagerInternal;
        String str;
        ComponentName r = this.mAms.getTopAppComponentName();
        if (r == null) {
            return 0;
        }
        VSlog.d(TAG, "getDex2oatAppropriateStatus : fg processName: " + r.getPackageName() + " mInstallerPackageName = " + this.mInstallerPackageName);
        if (r.getPackageName() != null && (r.getPackageName().equals("com.bbk.appstore") || r.getPackageName().equals("com.android.packageinstaller") || r.getPackageName().equals("com.vivo.browser") || ((str = this.mInstallerPackageName) != null && str.equals(r.getPackageName())))) {
            return 1;
        }
        if (r.getPackageName() == null || !r.getPackageName().contains("com.bbk.launcher") || (mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class)) == null) {
            return 0;
        }
        mInputManagerInternal.getLastInputTime();
        long elapseTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) - mInputManagerInternal.getLastInputTime();
        if (elapseTime > 3000) {
            return 1;
        }
        return 0;
    }

    public void notifyInstallerPackageName(String packageName) {
        this.mInstallerPackageName = packageName;
    }

    public void setAppUiFifo(ProcessRecord app) {
        AbsVivoPerfManager absVivoPerfManager;
        if (this.mAms.mUseFifoUiScheduling) {
            return;
        }
        try {
            if (app.runningRemoteAnimation) {
                app.savedPriority = Process.getThreadPriority(app.pid);
                Process.setThreadPriority(app.pid, -19);
                if (app.renderThreadTid != 0) {
                    Process.setThreadPriority(app.renderThreadTid, -19);
                    if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                        VSlog.d("UI_PRIO", "Set RenderThread (TID " + app.renderThreadTid + ") priority to -19");
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                    VSlog.d("UI_PRIO", "Not setting RenderThread TID");
                }
                mAnimPerf.perfHint(4227, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, 800, 4);
            } else {
                try {
                    try {
                        int curSchedGroup = app.getCurrentSchedulingGroup();
                        if (curSchedGroup == 3 && app.savedPriority > -10) {
                            app.savedPriority = -10;
                        }
                        Process.setThreadPriority(app.pid, app.savedPriority);
                        if (app.renderThreadTid != 0) {
                            Process.setThreadPriority(app.renderThreadTid, app.savedPriority);
                        }
                    } catch (IllegalArgumentException e) {
                        VSlog.w(TAG, "Failed to set scheduling policy, thread does not exist:\n" + e);
                        if (mAnimPerf != null) {
                            absVivoPerfManager = mAnimPerf;
                        }
                    }
                } catch (SecurityException e2) {
                    VSlog.w(TAG, "Failed to set scheduling policy, not allowed:\n" + e2);
                    if (mAnimPerf != null) {
                        absVivoPerfManager = mAnimPerf;
                    }
                }
                if (mAnimPerf != null) {
                    absVivoPerfManager = mAnimPerf;
                    absVivoPerfManager.perfLockRelease();
                }
            }
        } catch (Exception e3) {
            if (ActivityManagerDebugConfig.DEBUG_ALL) {
                VSlog.w(TAG, "Failed setting thread priority of " + app.pid, e3);
            }
        }
    }

    public void finishBooting(Context context) {
        setDebugConfig("yes".equals(SystemProperties.get("persist.sys.log.ctrl", "no")));
        IntentFilter bbklogFilter = new IntentFilter();
        bbklogFilter.addAction("android.vivo.bbklog.action.CHANGED");
        context.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.am.VivoAmsImpl.6
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                boolean status = "on".equals(intent.getStringExtra(VivoAmsImpl.BBKLOG_STATUS));
                VivoAmsImpl.this.setDebugConfig(status);
            }
        }, bbklogFilter, null, this.mAppsHandler);
        if (SystemProperties.getInt("persist.vivo.carnetworking.rate", 0) > 0) {
            try {
                SystemProperties.set("persist.vivo.carnetworking.rate", "0");
            } catch (RuntimeException e) {
                VSlog.i(TAG, "Failure to setprop!");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDebugConfig(boolean status) {
        ActivityTaskManagerDebugConfig.DEBUG_ALL_ACTIVITIES = status;
        ActivityTaskManagerDebugConfig.DEBUG_ADD_REMOVE = status;
        ActivityTaskManagerDebugConfig.DEBUG_APP = status;
        ActivityManagerDebugConfig.DEBUG_ANR = status;
        ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT = status;
        ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL = status;
        ActivityTaskManagerDebugConfig.DEBUG_CONFIGURATION = status;
        ActivityTaskManagerDebugConfig.DEBUG_PAUSE = status;
        ActivityTaskManagerDebugConfig.DEBUG_FOCUS = status;
        ActivityTaskManagerDebugConfig.DEBUG_SAVED_STATE = status;
        ActivityTaskManagerDebugConfig.DEBUG_STACK = status;
        ActivityTaskManagerDebugConfig.DEBUG_STATES = status;
        ActivityTaskManagerDebugConfig.DEBUG_TASKS = status;
        ActivityTaskManagerDebugConfig.DEBUG_TRANSITION = status;
        ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY = status;
        ActivityTaskManagerDebugConfig.DEBUG_IDLE = status;
        ActivityManagerDebugConfig.DEBUG_SERVICE_VIVO = status;
        ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE = status;
        Binder.LOG_RUNTIME_EXCEPTION = status;
    }

    public void dumpLogTag(PrintWriter pw, String[] args, int opti) {
        boolean on;
        if (args.length <= 1) {
            if (pw != null) {
                pw.println("  Invalid argument!");
                return;
            }
            return;
        }
        String type = args[opti];
        String zone = "0";
        if ("enable".equals(type) || "disable".equals(type)) {
            if (args.length <= 2) {
                if (pw != null) {
                    pw.println("  Invalid argument!");
                    return;
                }
                return;
            }
            zone = args[opti + 1];
        }
        if ("disable".equals(type)) {
            on = false;
        } else {
            boolean on2 = "enable".equals(type);
            if (on2) {
                on = true;
            } else if ("list".equals(type)) {
                pw.print(" 0 . DEBUG_ALL = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_ALL);
                pw.print(" 1 . DEBUG_ALL_ACTIVITIES & ALL_ACTIVITY = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_ALL_ACTIVITIES);
                pw.print(" 2 . DEBUG_ADD_REMOVE = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_ADD_REMOVE);
                pw.print(" 3 . DEBUG_APP = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_APP);
                pw.print(" 4 . DEBUG_BACKUP = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_BACKUP);
                pw.print(" 5 . DEBUG_BROADCAST = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_BROADCAST);
                pw.print(" 6 . DEBUG_BROADCAST_LIGHT & DEBUG_BROADCAST_DEFERRAL = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT);
                pw.print(" 7 . DEBUG_BROADCAST_BACKGROUND = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_BROADCAST_BACKGROUND);
                pw.print(" 8 . DEBUG_CLEANUP = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_CLEANUP);
                pw.print(" 9 . DEBUG_CONFIGURATION = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_CONFIGURATION);
                pw.print(" 10 . DEBUG_FOCUS = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_FOCUS);
                pw.print(" 11 . DEBUG_IDLE = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_IDLE);
                pw.print(" 12 . DEBUG_IMMERSIVE = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_IMMERSIVE);
                pw.print(" 13 . DEBUG_FREEZER = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_FREEZER);
                pw.print(" 14 . DEBUG_LOCKTASK = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_LOCKTASK);
                pw.print(" 15 . DEBUG_OOM_ADJ = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_OOM_ADJ);
                pw.print(" 16 . DEBUG_PAUSE = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_PAUSE);
                pw.print(" 17 . DEBUG_POWER = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_POWER);
                pw.print(" 18 . DEBUG_POWER_QUICK = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_POWER_QUICK);
                pw.print(" 19 . DEBUG_PROCESS_OBSERVERS = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS);
                pw.print(" 20 . DEBUG_PROCESSES = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_PROCESSES);
                pw.print(" 21 . DEBUG_PROVIDER = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_PROVIDER);
                pw.print(" 22 . DEBUG_PSS = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_PSS);
                pw.print(" 23 . DEBUG_RECENTS = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_RECENTS);
                pw.print(" 24 . DEBUG_RELEASE = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_RELEASE);
                pw.print(" 25 . DEBUG_RESULTS = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_RESULTS);
                pw.print(" 26 . DEBUG_SAVED_STATE = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_SAVED_STATE);
                pw.print(" 27 . DEBUG_FOREGROUND_SERVICE = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE);
                pw.print(" 28 . DEBUG_SERVICE = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_SERVICE);
                pw.print(" 29 . DEBUG_SERVICE_EXECUTING = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING);
                pw.print(" 30 . DEBUG_STACK = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_STACK);
                pw.print(" 31 . DEBUG_STATES = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_STATES);
                pw.print(" 32 . DEBUG_SWITCH = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_SWITCH);
                pw.print(" 33 . DEBUG_TASKS = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_TASKS);
                pw.print(" 34 . DEBUG_BACKGROUND_CHECK = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK);
                pw.print(" 35 . DEBUG_TRANSITION = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_TRANSITION);
                pw.print(" 36 . DEBUG_UID_OBSERVERS = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS);
                pw.print(" 37 . DEBUG_COMPACTION = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_COMPACTION);
                pw.print(" 38 . DEBUG_USER_LEAVING = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_USER_LEAVING);
                pw.print(" 39 . DEBUG_VISIBILITY = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY);
                pw.print(" 40 . DEBUG_METRICS = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_METRICS);
                pw.print(" 41 . DEBUG_ANR = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_ANR);
                pw.print(" 42 . DEBUG_CONTAINERS = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_CONTAINERS);
                pw.print(" 43 . DEBUG_LRU = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_LRU);
                pw.print(" 44 . DEBUG_MU = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_MU);
                pw.print(" 45 . DEBUG_USAGE_STATS = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_USAGE_STATS);
                pw.print(" 46 . DEBUG_PERMISSIONS_REVIEW = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW);
                pw.print(" 47 . DEBUG_WHITELISTS = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_WHITELISTS);
                pw.print(" 48 . DEBUG_NETWORK = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_NETWORK);
                pw.print(" 49 . DEBUG_OOM_ADJ_REASON = ");
                pw.println(ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON);
                pw.print(" 50 . DEBUG_RECENTS_TRIM_TASKS = ");
                pw.println(ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS);
                return;
            } else if (pw != null) {
                pw.println("Invalid argument, usage: dumpsys activity -d [enable|disable] [zone]");
                return;
            } else {
                return;
            }
        }
        if ("0".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_ALL = on;
            ActivityTaskManagerDebugConfig.DEBUG_ALL = on;
            pw.print("ActivityManagerDebugConfig.DEBUG_ALL = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_ALL);
            pw.print("ActivityTaskManagerDebugConfig.DEBUG_ALL = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_ALL);
        } else if ("1".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_ALL_ACTIVITIES = on;
            ActivityTaskManagerDebugConfig.DEBUG_ADD_REMOVE = on;
            ActivityTaskManagerDebugConfig.DEBUG_APP = on;
            ActivityTaskManagerDebugConfig.DEBUG_CONFIGURATION = on;
            ActivityTaskManagerDebugConfig.DEBUG_PAUSE = on;
            ActivityTaskManagerDebugConfig.DEBUG_SAVED_STATE = on;
            ActivityTaskManagerDebugConfig.DEBUG_STACK = on;
            ActivityTaskManagerDebugConfig.DEBUG_STATES = on;
            ActivityTaskManagerDebugConfig.DEBUG_TASKS = on;
            ActivityTaskManagerDebugConfig.DEBUG_TRANSITION = on;
            ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY = on;
            pw.print("DEBUG_ALL_ACTIVITIES & ALL_ACTIVITY = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_ALL_ACTIVITIES);
        } else if ("2".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_ADD_REMOVE = on;
            pw.print("DEBUG_ADD_REMOVE = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_ADD_REMOVE);
        } else if (InputExceptionReport.LEVEL_MEDIUM.equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_APP = on;
            pw.print("DEBUG_APP = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_APP);
        } else if ("4".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_BACKUP = on;
            pw.print("DEBUG_BACKUP = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_BACKUP);
        } else if (InputExceptionReport.LEVEL_VERY_LOW.equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_BROADCAST = on;
            pw.print("DEBUG_BROADCAST = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_BROADCAST);
        } else if ("6".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT = on;
            ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL = on;
            pw.print("DEBUG_BROADCAST_LIGHT = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT);
            pw.print("DEBUG_BROADCAST_DEFERRAL = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL);
        } else if ("7".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_BROADCAST_BACKGROUND = on;
            pw.print("DEBUG_BROADCAST_BACKGROUND = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_BROADCAST_BACKGROUND);
        } else if ("8".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_CLEANUP = on;
            pw.print("DEBUG_CLEANUP = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_CLEANUP);
        } else if ("9".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_CONFIGURATION = on;
            pw.print("DEBUG_CONFIGURATION = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_CONFIGURATION);
        } else if ("10".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_FOCUS = on;
            pw.print("DEBUG_FOCUS = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_FOCUS);
        } else if ("11".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_IDLE = on;
            pw.print("DEBUG_IDLE = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_IDLE);
        } else if ("12".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_IMMERSIVE = on;
            pw.print("DEBUG_IMMERSIVE = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_IMMERSIVE);
        } else if ("13".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_FREEZER = on;
            pw.print("DEBUG_FREEZER = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_FREEZER);
        } else if ("14".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_LOCKTASK = on;
            pw.print("DEBUG_LOCKTASK = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_LOCKTASK);
        } else if ("15".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_OOM_ADJ = on;
            pw.print("DEBUG_OOM_ADJ = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_OOM_ADJ);
        } else if ("16".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_PAUSE = on;
            pw.print("DEBUG_PAUSE = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_PAUSE);
        } else if ("17".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_POWER = on;
            pw.print("DEBUG_POWER = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_POWER);
        } else if ("18".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_POWER_QUICK = on;
            pw.print("DEBUG_POWER_QUICK = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_POWER_QUICK);
        } else if ("19".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS = on;
            pw.print("DEBUG_PROCESS_OBSERVERS = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS);
        } else if ("20".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_PROCESSES = on;
            pw.print("DEBUG_PROCESSES = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_PROCESSES);
        } else if ("21".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_PROVIDER = on;
            pw.print("DEBUG_PROVIDER = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_PROVIDER);
        } else if ("22".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_PSS = on;
            pw.print("DEBUG_PSS = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_PSS);
        } else if ("23".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_RECENTS = on;
            pw.print("DEBUG_RECENTS = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_RECENTS);
        } else if ("24".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_RELEASE = on;
            pw.print("DEBUG_RELEASE = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_RELEASE);
        } else if ("25".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_RESULTS = on;
            pw.print("DEBUG_RESULTS = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_RESULTS);
        } else if ("26".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_SAVED_STATE = on;
            pw.print("DEBUG_SAVED_STATE = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_SAVED_STATE);
        } else if ("27".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE = on;
            pw.print("DEBUG_FOREGROUND_SERVICE = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE);
        } else if ("28".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_SERVICE = on;
            pw.print("DEBUG_SERVICE = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_SERVICE);
        } else if ("29".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING = on;
            pw.print("DEBUG_SERVICE_EXECUTING = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING);
        } else if ("30".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_STACK = on;
            pw.print("DEBUG_STACK = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_STACK);
        } else if ("31".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_STATES = on;
            pw.print("DEBUG_STATES = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_STATES);
        } else if ("32".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_SWITCH = on;
            pw.print("DEBUG_SWITCH = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_SWITCH);
        } else if ("33".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_TASKS = on;
            pw.print("DEBUG_TASKS = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_TASKS);
        } else if ("34".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK = on;
            pw.print("DEBUG_BACKGROUND_CHECK = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK);
        } else if ("35".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_TRANSITION = on;
            pw.print("DEBUG_TRANSITION = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_TRANSITION);
        } else if ("36".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS = on;
            pw.print("DEBUG_UID_OBSERVERS = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS);
        } else if ("37".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_COMPACTION = on;
            pw.print("DEBUG_COMPACTION = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_COMPACTION);
        } else if ("38".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_USER_LEAVING = on;
            pw.print("DEBUG_USER_LEAVING = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_USER_LEAVING);
        } else if ("39".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY = on;
            pw.print("DEBUG_VISIBILITY = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY);
        } else if ("40".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_METRICS = on;
            pw.print("DEBUG_METRICS = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_METRICS);
        } else if ("41".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_ANR = on;
            pw.print("DEBUG_ANR = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_ANR);
        } else if ("42".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_CONTAINERS = on;
            pw.print("DEBUG_CONTAINERS = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_CONTAINERS);
        } else if ("43".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_LRU = on;
            pw.print("DEBUG_LRU = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_LRU);
        } else if ("44".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_MU = on;
            pw.print("DEBUG_MU = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_MU);
        } else if ("45".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_USAGE_STATS = on;
            pw.print("DEBUG_USAGE_STATS = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_USAGE_STATS);
        } else if ("46".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW = on;
            ActivityTaskManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW = on;
            pw.print("ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW);
            pw.print("ActivityTaskManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW);
        } else if ("47".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_WHITELISTS = on;
            pw.print("DEBUG_WHITELISTS = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_WHITELISTS);
        } else if ("48".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_NETWORK = on;
            pw.print("DEBUG_NETWORK = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_NETWORK);
        } else if ("49".equals(zone)) {
            ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON = on;
            pw.print("DEBUG_OOM_ADJ_REASON = ");
            pw.println(ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON);
        } else if ("50".equals(zone)) {
            ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS = on;
            pw.print("DEBUG_RECENTS_TRIM_TASKS = ");
            pw.println(ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS);
        } else if ("51".equals(zone)) {
            UriGrantsManagerService.DEBUG_URI_PERMISSION = on;
            pw.print("DEBUG_URI_PERMISSION = ");
            pw.println(UriGrantsManagerService.DEBUG_URI_PERMISSION);
        } else if (pw != null) {
            pw.println("  Invalid argument!");
        }
    }

    public boolean forbidBroadcastIfNeed(String action, String caller, int callingUid) {
        if ("bbk.receiver.action.SYSTEM_UPDATE".equals(action)) {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                VSlog.d(TAG, "Pkg:" + caller + "(pid:callingPid, uid:" + callingUid + ") send broadcast with action:" + action + ".");
            }
            try {
                int checkResult = AppGlobals.getPackageManager().checkSignatures(VivoPermissionUtils.OS_PKG, caller);
                if (checkResult != 0) {
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        VSlog.d(TAG, "Not allow pkg:" + caller + " without system signature to send broadcast with action:" + action + ".");
                    }
                    return true;
                }
                return false;
            } catch (RemoteException e) {
                VSlog.w(TAG, "Remote exception", e);
                return true;
            }
        }
        return false;
    }

    public ArrayList<String> getExcessiveCpuFilterList() {
        AmsConfigManager amsConfigManager = this.mAmsConfigManager;
        if (amsConfigManager != null) {
            return amsConfigManager.getExcessiveCpuFilterList();
        }
        return null;
    }

    public boolean filterExcessiveCPUIfNeed(String processName) {
        if (getExcessiveCpuFilterList() != null && getExcessiveCpuFilterList().contains(processName)) {
            return true;
        }
        return false;
    }

    public void sendProcessActivityChangeMessage(int pid, int uid) {
        ActivityManagerService.ProcessChangeItem tempItem = new ActivityManagerService.ProcessChangeItem();
        tempItem.changes |= 1;
        tempItem.foregroundActivities = true;
        tempItem.pid = -1;
        tempItem.processState = 2;
        tempItem.uid = -1;
        this.mAms.mPendingProcessChanges.add(tempItem);
        this.mAms.mUiHandler.obtainMessage(31).sendToTarget();
        ActivityManagerService.ProcessChangeItem item = new ActivityManagerService.ProcessChangeItem();
        item.changes |= 1;
        item.foregroundActivities = true;
        item.pid = pid;
        item.processState = 2;
        item.uid = uid;
        this.mAms.mPendingProcessChanges.add(item);
        this.mAms.mUiHandler.obtainMessage(31).sendToTarget();
    }

    public void sendProcessActivityChangeMessageOnce(int pid, int uid) {
        ActivityManagerService.ProcessChangeItem item = new ActivityManagerService.ProcessChangeItem();
        item.changes |= 1;
        item.foregroundActivities = true;
        item.pid = pid;
        item.processState = 2;
        item.uid = uid;
        this.mAms.mPendingProcessChanges.add(item);
        this.mAms.mUiHandler.obtainMessage(31).sendToTarget();
    }

    public boolean writeLmkdByRms(ByteBuffer buf) {
        return this.mAms.mProcessList.writeLmkdByRms(buf);
    }

    public boolean killProcessByRms(int[] pids, int[] curAdjs, String reason, boolean secure) {
        ArrayList<ProcessRecord> procs = new ArrayList<>();
        boolean killed = false;
        synchronized (this.mAms.mPidsSelfLocked) {
            for (int i = 0; i < pids.length; i++) {
                ProcessRecord proc = this.mAms.mPidsSelfLocked.get(pids[i]);
                if (proc != null && proc.setAdj >= curAdjs[i]) {
                    procs.add(proc);
                }
            }
            Iterator<ProcessRecord> it = procs.iterator();
            while (it.hasNext()) {
                ProcessRecord proc2 = it.next();
                if (!proc2.killedByAm) {
                    proc2.kill(reason, 3, true);
                    killed = true;
                }
            }
        }
        return killed;
    }

    public void forceStopPackageByRms(String packageName, int userId, String reason) {
        this.mAms.forceStopPackage(packageName, userId, true, reason);
    }

    /* JADX WARN: Type inference failed for: r15v1 */
    /* JADX WARN: Type inference failed for: r15v2, types: [int, boolean] */
    /* JADX WARN: Type inference failed for: r15v3 */
    public void startProcessByRMS(int userId, String pkg, String proc, String reason, boolean keepQuiet) {
        ?? r15;
        ProcessRecord app;
        if (pkg == null || proc == null) {
            return;
        }
        try {
            ApplicationInfo appInfo = AppGlobals.getPackageManager().getApplicationInfo(pkg, (int) Consts.ProcessStates.FOCUS, userId);
            if (appInfo != null) {
                synchronized (this.mAms) {
                    VSlog.i(TAG, "Start process for " + pkg + " by RMS for reason:" + reason + ", keepQuiet:" + keepQuiet);
                    ProcessRecord app2 = this.mAms.getProcessRecordLocked(proc, appInfo.uid, true);
                    if (app2 == null) {
                        r15 = 0;
                        ProcessRecord app3 = this.mProcessList.newProcessRecordLocked(appInfo, proc, false, 0, new HostingRecord(reason, proc));
                        if (app3 == null) {
                            return;
                        }
                        this.mAms.updateLruProcessLocked(app3, false, (ProcessRecord) null);
                        this.mAms.updateOomAdjLocked("updateOomAdj_processBegin");
                        app = app3;
                    } else {
                        r15 = 0;
                        app = app2;
                    }
                    try {
                        AppGlobals.getPackageManager().setPackageStoppedState(pkg, (boolean) r15, userId);
                    } catch (RemoteException | IllegalArgumentException e) {
                        VSlog.w(TAG, "Failed trying to unstop package " + pkg + ": " + e);
                    }
                    if (app != null && app.thread == null) {
                        if (app.getVivoInjectInstance() != null) {
                            app.getVivoInjectInstance().setRmsPreloaded(true);
                            VSlog.i(TAG, "Start process for " + pkg + " by RMS for reason:" + reason + ", keepQuiet:" + keepQuiet);
                            app.getVivoInjectInstance().setNeedKeepQuiet(keepQuiet);
                        }
                        this.mProcessList.startProcessLocked(app, new HostingRecord(reason, proc), (int) r15);
                    }
                }
            }
        } catch (RemoteException e2) {
            VSlog.w(TAG, "Failed to start process for " + reason);
        }
    }

    public void setUserController(UserController controller) {
        this.mUserController = controller;
    }

    public Debug.MemoryInfo[] getProcessMemoryInfo(int[] pids) {
        return this.mAms.getProcessMemoryInfo(pids);
    }

    public void handleAppCrashRescue(ProcessRecord app, HashMap<String, String> map) {
        if (this.mAppCrashRescueUtil == null) {
            this.mAppCrashRescueUtil = AppCrashRescueUtil.getInstance();
        }
        AppCrashRescueUtil appCrashRescueUtil = this.mAppCrashRescueUtil;
        if (appCrashRescueUtil != null && appCrashRescueUtil.initReady()) {
            if (AppCrashRescueUtil.sRescueSwitch) {
                this.mAppCrashRescueUtil.handleAppCrashTooMuch(app, map);
                return;
            }
            VSlog.i(CRASH_TAG, "appcrash rescue switch status: " + AppCrashRescueUtil.sRescueSwitch);
            return;
        }
        VSlog.w(CRASH_TAG, "mAppCrashRescueUtil is " + this.mAppCrashRescueUtil + ", initReady is " + this.mAppCrashRescueUtil.initReady());
    }

    public void cleanupProcessRecordLocked(ProcessRecord app) {
        BroadcastProxyManager.cleanupProcessRecordLocked(app);
    }

    public void cleanupReceiverLocked(ReceiverList rl) {
        BroadcastProxyManager.cleanupReceiverLocked(rl);
    }

    public boolean isBroadcastRegistered(String pkgName, int userId, String action, int flags) {
        boolean z;
        Intent intent = new Intent(action);
        intent.setPackage(pkgName);
        synchronized (this.mAms) {
            z = false;
            List<BroadcastFilter> registered = this.mAms.mReceiverResolver.queryIntent(intent, (String) null, false, userId);
            if (registered != null && !registered.isEmpty()) {
                z = true;
            }
        }
        return z;
    }

    public final int getWakefulness() {
        return this.mAms.mWakefulness;
    }

    public ArgPack exeAppCmd(int pid, int cmd, ArgPack argPack) throws RemoteException {
        IBinder remote;
        synchronized (this.mAms.mPidsSelfLocked) {
            ProcessRecord proc = this.mAms.mPidsSelfLocked.get(pid);
            if (proc == null || proc.thread == null) {
                throw new IllegalArgumentException("Unknown pid: " + pid);
            }
            remote = proc.thread.asBinder();
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInt(cmd);
            argPack.writeToParcel(data, 0);
            remote.transact(201908, data, reply, 0);
            ArgPack result = ArgPack.createFromParcel(reply);
            return result;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public boolean needAddIncludeStoppedFlag(Intent intent, String packageName) {
        return VivoFcmGoldPassSupervisor.getInstance().needAddIncludeStoppedFlag(intent, packageName);
    }

    public void setDoubleInstanceConfig(boolean enabled, Map map) {
        this.mDoubleInstanceConfig.setDoubleInstanceConfig(enabled, map);
    }

    public List<String> getDoubleInstanceConfig(int type) {
        return this.mDoubleInstanceConfig.getDoubleInstanceConfig(type);
    }

    public boolean weatherResolvePIForDoubleInstance(int userHandle, String authority) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && userHandle == 999 && authority != null && authority.equals("media")) {
            return true;
        }
        return false;
    }

    public boolean cancelCheckCPForDoubleInstance(int userId, String authority) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && userId == 999 && authority != null) {
            if (authority.equals("settings") || authority.equals("sms")) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean bindServiceIngnoreOtherUser() {
        int callingUserId = UserHandle.getCallingUserId();
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && callingUserId != 0 && callingUserId != 999) {
            return true;
        }
        return false;
    }

    public boolean isDoubleInstanceEnable() {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null) {
            return vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable();
        }
        return false;
    }

    public boolean isDoubleAppUserExist() {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null) {
            return vivoDoubleInstanceServiceImpl.isDoubleAppUserExist();
        }
        return false;
    }

    public int[] deliverBroadcastForDoubleInstance(int userId, Intent intent) {
        int[] users = {userId};
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && this.mVivoDoubleInstanceService.isDoubleAppUserExist() && intent != null) {
            synchronized (DoubleInstanceConfig.getInstance()) {
                if (userId == 999) {
                    try {
                        if (DoubleInstanceConfig.getInstance().getBroadcastToOwnerUser().contains(intent.getAction())) {
                            users = new int[]{userId, 0};
                        }
                    } finally {
                    }
                }
                if (userId == 0 && DoubleInstanceConfig.getInstance().getBroadcastToDoubleUser().contains(intent.getAction())) {
                    users = new int[]{userId, ProcessList.CACHED_APP_MAX_ADJ};
                }
            }
        }
        return users;
    }

    public boolean weatherReplaceReceiverForDoubleInstance(int userId, int callingUid) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable()) {
            if (userId == 999 || UserHandle.getUserId(callingUid) == 999 || userId == 0 || UserHandle.getUserId(callingUid) == 0) {
                return true;
            }
            return false;
        }
        return false;
    }

    public Intent replaceIntentUriPathForDoubleInstance(Intent intent) {
        Uri intentUri = intent.getData();
        if (intentUri != null) {
            String intentUriPath = intentUri.getPath();
            String scheme = intentUri.getScheme();
            if (intentUriPath != null && "file".equals(scheme)) {
                intent.setData(Uri.fromFile(new File(intentUriPath.replace(CLONE_USER_PATH, VIRTUAL_CLONE_USER_PATH))));
                VSlog.i(TAG, "replace intentUriPath intent = " + intent);
            }
        }
        return intent;
    }

    public boolean isNotMountedStorageVolumeForDoubleInstance(String service) {
        if ("com.whatsapp.contact.sync.ContactsSyncAdapterService".equals(service)) {
            StorageVolume volume = StorageManager.getStorageVolume(new File(CLONE_USER_PATH), ProcessList.CACHED_APP_MAX_ADJ);
            return volume == null || !"mounted".equals(volume.getState());
        }
        return false;
    }

    public void reportCrashEvent(ProcessRecord r, ApplicationErrorReport.CrashInfo crashInfo) {
        String shortMsg = crashInfo.exceptionClassName;
        String longMsg = crashInfo.exceptionMessage;
        String stackTrace = crashInfo.stackTrace;
        if (shortMsg != null && longMsg != null) {
            longMsg = shortMsg + ": " + longMsg;
        } else if (shortMsg != null) {
            longMsg = shortMsg;
        }
        if (r != null && r.info != null && !r.info.isSystemApp() && !r.info.isUpdatedSystemApp()) {
            boolean isForegroundCrash = false;
            isForegroundCrash = (r.isInterestingToUserLocked() || (r.info != null && FaceUIState.PKG_SYSTEMUI.equals(r.info.packageName)) || r.hasTopUi() || r.hasOverlayUi()) ? true : true;
            ContentValues cv = new ContentValues();
            cv.put("packageName", r.info.packageName);
            cv.put("versionCode", Integer.valueOf(r.info.versionCode));
            cv.put("longMsg", longMsg);
            cv.put("isForeground", Boolean.valueOf(isForegroundCrash));
            cv.put("stackTrace", stackTrace);
            cv.put("crash_type", (Integer) 2);
            if (SystemClock.elapsedRealtime() - r.startTime < 10000) {
                cv.put("directcrash", (Integer) 1);
            } else {
                cv.put("directcrash", (Integer) 0);
            }
            ExceptionPolicyManager.getInstance().reportEvent(2, System.currentTimeMillis(), cv);
            if (isForegroundCrash) {
                SystemDefenceManager.getInstance().reportFgCrashData(r.info.packageName, longMsg, stackTrace);
            }
        }
    }

    public void reportAnrEvent(String annotation, ApplicationInfo info, boolean isInteresting) {
        if (info != null && !info.isSystemApp() && !info.isUpdatedSystemApp()) {
            ContentValues cv = new ContentValues();
            cv.put("packageName", info.packageName);
            cv.put("versionCode", Integer.valueOf(info.versionCode));
            cv.put("crash_type", (Integer) 1);
            cv.put("isSilentANR", Boolean.valueOf(!isInteresting));
            cv.put("anrReason", annotation);
            ExceptionPolicyManager.getInstance().reportEvent(2, System.currentTimeMillis(), cv);
        }
    }

    public void resetFontSize(Handler handler) {
        String language = Locale.getDefault().getLanguage();
        String country = Locale.getDefault().getCountry();
        int fontsize = SystemProperties.getInt("persist.system.vivo.fontsize", 550);
        IVivoSystemFonts vivoSystemFonts = SystemFonts.getVivoSystemFonts();
        if (Typeface.sMonster && fontsize != 550 && vivoSystemFonts.isDefaultFont() && !language.isEmpty() && !country.isEmpty()) {
            if (!"CN".equals(country) || !"zh".equals(language)) {
                handler.post(new Runnable() { // from class: com.android.server.am.VivoAmsImpl.7
                    @Override // java.lang.Runnable
                    public void run() {
                        SystemProperties.set("persist.system.vivo.fontsize", String.valueOf(550));
                        Configuration configuration = VivoAmsImpl.this.mAms.getConfiguration();
                        boolean unused = VivoAmsImpl.mPlus = !VivoAmsImpl.mPlus;
                        if (VivoAmsImpl.mPlus) {
                            configuration.fontScale += 0.001f;
                        } else {
                            configuration.fontScale -= 0.001f;
                        }
                        VivoAmsImpl.this.mAms.updatePersistentConfiguration(configuration);
                        VSlog.d("Typeface", "resetVivoFontSize!");
                        VivoAmsImpl.this.forceStopPkgsAfterFontChanged();
                    }
                });
            }
        }
    }

    public void forceStopPkgsAfterFontChanged() {
        String[] strArr;
        List<ActivityManager.RunningAppProcessInfo> allProc = this.mAms.getRunningAppProcesses();
        if (allProc == null || allProc.isEmpty()) {
            VSlog.d(TAG, "no RunningAppProcesses");
            return;
        }
        for (ActivityManager.RunningAppProcessInfo proc : allProc) {
            for (String s : proc.pkgList) {
                if (needKillOrRestartProcess(s, this.restartPkgs)) {
                    this.mAms.killBackgroundProcesses(s, this.mContext.getUserId());
                } else if (needForceStop(this.mContext, s, this.whiteList, this.stopPkgs)) {
                    this.mAms.forceStopPackage(s, proc.uid);
                }
            }
        }
    }

    public boolean needKillOrRestartProcess(String pkgName, String[] Pkgs) {
        if (Pkgs == null || pkgName == null) {
            return false;
        }
        for (String pkg : Pkgs) {
            if (pkgName.compareTo(pkg) == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean needForceStop(Context context, String pkgName, String[] whiteList, String[] stopPkgs) {
        if (pkgName == null) {
            return false;
        }
        if (isSystemApp(context.getPackageManager(), pkgName)) {
            if (stopPkgs == null) {
                return false;
            }
            for (String pkg : stopPkgs) {
                if (pkgName.compareTo(pkg) == 0) {
                    return true;
                }
            }
            return false;
        } else if (whiteList == null) {
            return true;
        } else {
            for (String pkg2 : whiteList) {
                if (pkgName.contains(pkg2)) {
                    return false;
                }
            }
            return true;
        }
    }

    public boolean isSystemApp(PackageManager pm, String packageName) {
        int appFlags = 0;
        try {
            appFlags = pm.getApplicationInfo(packageName, 0).flags;
        } catch (Exception e) {
        }
        if ((appFlags & 1) == 0) {
            return false;
        }
        return true;
    }

    public void reloadVivoFont(Configuration config, Handler uiHandler) {
        Configuration tmpConfig = this.mAms.mActivityTaskManager.getGlobalConfiguration();
        int diff = tmpConfig.diff(config);
        if ((1073741824 & diff) != 0 && Math.abs(tmpConfig.fontScale - config.fontScale) > 0.0f && Math.abs(tmpConfig.fontScale - config.fontScale) <= 0.01f) {
            uiHandler.post(new Runnable() { // from class: com.android.server.am.VivoAmsImpl.8
                @Override // java.lang.Runnable
                public void run() {
                    IVivoTypeface vivoTypeface = Typeface.getVivoTypeface();
                    if (vivoTypeface != null) {
                        VLog.d("Typeface", "AndroidUI reloadVivoFont!");
                        vivoTypeface.reloadVivoFont();
                        Canvas.freeCaches();
                        Canvas.freeTextLayoutCaches();
                        return;
                    }
                    VLog.d("Typeface", "AndroidUI reloadFont faild!");
                }
            });
            nofityZygoteUpdateFont();
        }
    }

    public void nofityZygoteUpdateFont() {
        VLog.d("Typeface", "nofityZygoteUpdateFont!");
        Process.start(VIVO_CMD_PREFIX, VIVO_CMD_PREFIX, 1000, 1000, null, 0, 0, 0, null, Build.SUPPORTED_32_BIT_ABIS[0], UPDATE_FONT, null, null, null, 0, false, null, null, null, false, false, null);
        if (VMRuntime.getRuntime().is64Bit()) {
            Process.start(VIVO_CMD_PREFIX, VIVO_CMD_PREFIX, 1000, 1000, null, 0, 0, 0, null, Build.SUPPORTED_64_BIT_ABIS[0], UPDATE_FONT, null, null, null, 0, false, null, null, null, false, false, null);
        }
    }

    public void registerW2MShieldToggle(Handler handler) {
        ContentObserver contentObserver = new ContentObserver(handler) { // from class: com.android.server.am.VivoAmsImpl.9
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                int shield = Settings.Global.getInt(VivoAmsImpl.this.mContext.getContentResolver(), "vivo_gaming_w2m_shield_broadcast", 0);
                VSlog.v(VivoAmsImpl.TAG, "vivo_gaming_w2m_shield_broadcast onChange shield:" + shield);
                if (shield == 1) {
                    VivoAmsImpl.this.mW2mShieldBroadcast = true;
                } else {
                    VivoAmsImpl.this.mW2mShieldBroadcast = false;
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("vivo_gaming_w2m_shield_broadcast"), false, contentObserver);
    }

    public boolean isW2mShieldBroadcast(Intent intent, String packageName) {
        if (!this.mW2mShieldBroadcast || !"android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction()) || packageName == null || !packageName.contains("com.tencent.tmgp.sgame")) {
            return false;
        }
        return true;
    }

    public void handleAppDiedLocked(Handler mHandler, final int pid) {
        mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.VivoAmsImpl.10
            @Override // java.lang.Runnable
            public void run() {
                if (VivoAmsImpl.this.mANRManager != null) {
                    VivoAmsImpl.this.mANRManager.clearAllMessages(pid);
                }
            }
        }, 10000L);
    }

    public void initANRManager(Context context) {
        this.mANRManager = ANRManager.getDefault(context);
    }

    private void dumpSurfaceFlingerInfo(StringBuilder sb) {
        if (sb == null) {
            return;
        }
        sb.append("\n===dump SurfaceFlinger start===\n");
        try {
            File fSF = new File("/data/anr/surfaceflinger.txt");
            if (fSF.exists()) {
                fSF.delete();
            }
            fSF.createNewFile();
            FileUtils.setPermissions(fSF.getPath(), 438, -1, -1);
            VivoDmServiceProxy vivoDmSrvProxy = VivoDmServiceProxy.asInterface(ServiceManager.getService("vivo_daemon.service"));
            if (vivoDmSrvProxy != null) {
                if (SystemProperties.get("persist.vivo.vivo_daemon", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) != Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) {
                    String cmd_dump_sf = "hsEl1pWvp8keI+T1L7CcPKA4yi8h/wLw9pDmcDvzYKw1ZwMSiupo/gddE+jnEWXd/Oq/e0l46yVY75sv2TdjI6JgWMK4MvWJ3YX5uUzJZ/02etiGliXjMpYS2pA3SQYYp0RxVY4yevfm33+mhSWo50Z4ItpdOF+/DNhUH56o9Tn01I9CaNEk9/Sh+VNuA8yzxhRPHFYoXkMVZ8YdnD1QVcAsDpT9uERVQcrbHgMC+Wti/vLV+9FFy8i6Z17NKGrRGTpcu6cei7wzN24Z75dYFeqwPvwsz6pybPyl6L/wRMhtSK+cQxMOGMf/e5CyiCyLfEZsDfOgAQRhscVyaqr55Q==?" + fSF.getPath();
                    vivoDmSrvProxy.runShell(cmd_dump_sf);
                } else {
                    vivoDmSrvProxy.runShell("dumpsys SurfaceFlinger > " + fSF.getPath());
                }
            }
            String surfaceFlingerInfo = FileUtils.readTextFile(fSF, DROPBOX_DEFAULT_MAX_SIZE, "\n\n[[TRUNCATED]]");
            sb.append(surfaceFlingerInfo);
            if (vivoDmSrvProxy != null) {
                if (SystemProperties.get("persist.vivo.vivo_daemon", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) != Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) {
                    String cmd_rm_f = "ktDp/8ZVKYBjzJbbJ+MytxpcCcMKwqGfL9I1HC1K6X+/Xki0HrnlUawQJuUXIOZVnGVMvSciWvxCGQMuogyh7XlTlwtmWW2bPVEYfqkR9bquEtKyI5sC6Q8XfHGfqbcqfxFmACOXOpOizJy7TflBxDuDcCONzFUC5uG+8syBft1EXcaD+P+TwhVZ+geWXu6nha35+hCcsZe/AEjORv1N5AtWDFEFXniksCTPqnIlbXa0Bq6xYtY/92bmqp7u8xlcN+c/rpRjhmsdTlvQcaqXzKnXzHarCp3xiOKrsx1k/ePuM+ZEWor8riNa//wqpGK712QNCiMod39u0XKTnDxrCQ==?" + fSF.getPath();
                    vivoDmSrvProxy.runShell(cmd_rm_f);
                } else {
                    vivoDmSrvProxy.runShell("rm -f " + fSF.getPath());
                }
            }
        } catch (Throwable th) {
            VSlog.e(TAG, "dump SurfaceFlingerInfo failed:", th);
        }
        sb.append("\n===dump SurfaceFlinger end===\n");
    }

    private void dumpPSInfo(StringBuilder sb) {
        if (sb == null) {
            return;
        }
        sb.append("\n===dump ps start===\n");
        try {
            File fPS = new File("/data/anr/ps.txt");
            if (fPS.exists()) {
                fPS.delete();
            }
            fPS.createNewFile();
            FileUtils.setPermissions(fPS.getPath(), 438, -1, -1);
            VivoDmServiceProxy vivoDmSrvProxy = VivoDmServiceProxy.asInterface(ServiceManager.getService("vivo_daemon.service"));
            if (vivoDmSrvProxy != null) {
                if (SystemProperties.get("persist.vivo.vivo_daemon", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) != Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) {
                    String cmd_ps_AT = "cy5Ly+NWw3b7SEIQDQaT4RXPXXOUC1h/Q01kGbOG/o9Uu3R8cPEZcesY5kp6wpi/lKbSymYDMfbSBVQ5ADLZ44yvsN51j0r2KiKnh4GR0I9JrbOrx7HxH3IWUZKijia6mJ3t3Y7eF6idoO/2dgbVrE9wAvklcX0rT59czdxYV6GbnY2U46wM2lbGSZm9wuyQIzDpkZOa1kMT77Qevk1nxpcOSOFpsZjCfH/l+uUuDDBvw4OAFkBvp/d1pFL9kFU9zyo9onNIXLqxzird5pmLoenen1aBNDb1TKG8HmM4XwXKYiLsgp1ZQWBPW1+hG7W52pEdc2U3uwhDB/jz4Gax8g==?" + fPS.getPath();
                    vivoDmSrvProxy.runShell(cmd_ps_AT);
                } else {
                    vivoDmSrvProxy.runShell("ps -AT > " + fPS.getPath());
                }
            }
            String psInfo = FileUtils.readTextFile(fPS, DROPBOX_DEFAULT_MAX_SIZE, "\n\n[[TRUNCATED]]");
            sb.append(psInfo);
            if (vivoDmSrvProxy != null) {
                if (SystemProperties.get("persist.vivo.vivo_daemon", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) != Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) {
                    String cmd_rm_f = "ktDp/8ZVKYBjzJbbJ+MytxpcCcMKwqGfL9I1HC1K6X+/Xki0HrnlUawQJuUXIOZVnGVMvSciWvxCGQMuogyh7XlTlwtmWW2bPVEYfqkR9bquEtKyI5sC6Q8XfHGfqbcqfxFmACOXOpOizJy7TflBxDuDcCONzFUC5uG+8syBft1EXcaD+P+TwhVZ+geWXu6nha35+hCcsZe/AEjORv1N5AtWDFEFXniksCTPqnIlbXa0Bq6xYtY/92bmqp7u8xlcN+c/rpRjhmsdTlvQcaqXzKnXzHarCp3xiOKrsx1k/ePuM+ZEWor8riNa//wqpGK712QNCiMod39u0XKTnDxrCQ==?" + fPS.getPath();
                    vivoDmSrvProxy.runShell(cmd_rm_f);
                } else {
                    vivoDmSrvProxy.runShell("rm -f " + fPS.getPath());
                }
            }
        } catch (Throwable th) {
            VSlog.e(TAG, "dump PSInfo failed: ", th);
        }
        sb.append("\n===dump ps end===\n");
    }

    private void dumpBinderInfo(StringBuilder sb) {
        if (sb == null) {
            return;
        }
        sb.append("\n===dump binder info start===\n");
        try {
            String stats = FileUtils.readTextFile(new File("/sys/kernel/debug/binder/stats"), DROPBOX_DEFAULT_MAX_SIZE, "\n\n[[TRUNCATED]]");
            String state = FileUtils.readTextFile(new File("/sys/kernel/debug/binder/state"), DROPBOX_DEFAULT_MAX_SIZE, "\n\n[[TRUNCATED]]");
            String failed_transaction_log = FileUtils.readTextFile(new File("/sys/kernel/debug/binder/failed_transaction_log"), DROPBOX_DEFAULT_MAX_SIZE, "\n\n[[TRUNCATED]]");
            String transaction_log = FileUtils.readTextFile(new File("/sys/kernel/debug/binder/transaction_log"), DROPBOX_DEFAULT_MAX_SIZE, "\n\n[[TRUNCATED]]");
            String transactions = FileUtils.readTextFile(new File("/sys/kernel/debug/binder/transactions"), DROPBOX_DEFAULT_MAX_SIZE, "\n\n[[TRUNCATED]]");
            sb.append(stats);
            sb.append(state);
            sb.append(failed_transaction_log);
            sb.append(transaction_log);
            sb.append(transactions);
        } catch (Throwable th) {
            VSlog.e(TAG, "dump BinderInfo failed: ", th);
        }
        sb.append("\n===dump binder info end===\n");
    }

    public int getBinderTargetPID(String strBinderName) {
        VivoDmServiceProxy vivoDmSrvProxy;
        String cmd_ps_grep_i;
        String[] columns;
        if (strBinderName == null || strBinderName.length() <= 0) {
            return -1;
        }
        try {
            Iterator<Map.Entry<String, String>> iterator = binderToProcessNameMap.entrySet().iterator();
            String strProcessName = null;
            while (true) {
                if (!iterator.hasNext()) {
                    break;
                }
                Map.Entry<String, String> item = iterator.next();
                if (strBinderName.contains(item.getKey())) {
                    strProcessName = item.getValue();
                    break;
                }
            }
            if (strProcessName != null && (vivoDmSrvProxy = VivoDmServiceProxy.asInterface(ServiceManager.getService("vivo_daemon.service"))) != null) {
                if (SystemProperties.get("persist.vivo.vivo_daemon", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) != Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) {
                    cmd_ps_grep_i = "ASiSVhy6ekee76lfgsXQus+rsArHl0b7AlcqUYcOG2jjgzGmdBRWnpfZG0en3vQPHZ2Bmc/5lW0GIjEvaHgiMpl2e3M007bg6R6GwK+6DsuDM53vX++vbwSBUFdCkyba5X1X8urRbPL1GWB9xMPAXTLNbe1RTu+d6xqMeQb0KQ9EFRS68Yve+/TWqMr2tU7+JlmbF4I0hh6iYBq4j9ZgJpAJ/vOobDp1rzDGr2ZqJa98T2bB7txdZtv0vVnUu9SfwtfSgMqSDthdtzx6O4pyWh9LnHqyl0oMDmLa+a5ikSQnmc7CEd0m/UVYHRq3NgtnvFUrc524wEqFix7X5uBIrw==?" + strProcessName;
                } else {
                    cmd_ps_grep_i = "ps -A | grep -i " + strProcessName;
                }
                String processInfo = vivoDmSrvProxy.runShellWithResult(cmd_ps_grep_i);
                if (processInfo != null && processInfo.length() > 0 && (columns = processInfo.split("\\s+")) != null && columns.length > 1) {
                    return Integer.valueOf(columns[1]).intValue();
                }
            }
        } catch (Throwable th) {
            VSlog.e(TAG, "getBinderTargetPID failed: ", th);
        }
        return -1;
    }

    public void appendDropBoxSystemHeaders(StringBuilder sb) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Date timestamp = new Date();
        String vivoVersion = SystemProperties.get("ro.build.version.bbk", "Unavailable");
        sb.append("\n");
        sb.append("Time: ");
        sb.append(sdf.format(timestamp));
        sb.append("\n");
        sb.append("Softversion: ");
        sb.append(vivoVersion);
        sb.append("\n");
    }

    public Boolean printCrashLinesLimit(String eventType, String processName, Long crashBeginTime, Map<String, Long> crashMap) {
        Boolean crashFlag = true;
        if ("crash".equals(eventType) || "native_crash".equals(eventType)) {
            Long crashBeginTime2 = Long.valueOf(SystemClock.uptimeMillis());
            if (crashMap != null && processName != null && processName.length() != 0) {
                if (!crashMap.containsKey(processName)) {
                    crashMap.put(processName, crashBeginTime2);
                    crashFlag = true;
                } else if (crashBeginTime2.longValue() > crashMap.get(processName).longValue() + VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL) {
                    crashMap.put(processName, crashBeginTime2);
                    crashFlag = true;
                } else {
                    crashFlag = false;
                }
                Iterator<Map.Entry<String, Long>> it = crashMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Long> item = it.next();
                    if (crashBeginTime2.longValue() > item.getValue().longValue() + VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL) {
                        it.remove();
                    }
                }
            }
        }
        return crashFlag;
    }

    public void appendDropBoxAppHeaders(ProcessRecord process, String processName, StringBuilder sb) {
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        new Date();
        String appVersion = "version uncatchable";
        IPackageManager pm = AppGlobals.getPackageManager();
        for (int ip = 0; ip < process.pkgList.size(); ip++) {
            String pkg = process.pkgList.keyAt(ip);
            try {
                PackageInfo pi = pm.getPackageInfo(pkg, 0, UserHandle.getCallingUserId());
                if (pi != null) {
                    if (ip == 0) {
                        String appVersion2 = "versionName = " + pi.versionName;
                        appVersion = appVersion2 + " versionCode = " + pi.versionCode;
                    }
                    if (processName != null && processName.equals(pkg)) {
                        String appVersion3 = "versionName = " + pi.versionName;
                        appVersion = appVersion3 + " versionCode = " + pi.versionCode;
                    }
                }
            } catch (RemoteException e) {
                VSlog.e(TAG, "Error getting package info: " + pkg, e);
            }
        }
        SystemProperties.get("ro.build.version.bbk", "Unavailable");
        sb.append("Appversion: ");
        sb.append(appVersion);
        sb.append("\n");
    }

    private String processClass(ProcessRecord process) {
        int MY_PID = Process.myPid();
        if (process == null || process.pid == MY_PID) {
            return "system_server";
        }
        if ((process.info.flags & 1) != 0) {
            return "system_app";
        }
        return "data_app";
    }

    public int setLogcatlines(String eventTypeAnr, int lines, String dropboxTag) {
        lines = ("anr".equals(eventTypeAnr) || "crash".equals(eventTypeAnr) || "watchdog".equals(eventTypeAnr) || "native_crash".equals(eventTypeAnr)) ? 10000 : 10000;
        if ("system_server_crash".equals(dropboxTag)) {
            return 20000;
        }
        return lines;
    }

    public void addErrorToDropBoxByTraces(String eventTypeAnr, File dataFile, StringBuilder sb) {
        if ("anr".equals(eventTypeAnr) || "watchdog".equals(eventTypeAnr)) {
            if (dataFile != null) {
                try {
                    sb.append(FileUtils.readTextFile(dataFile, DROPBOX_DEFAULT_MAX_SIZE, "\n\n[[TRUNCATED]]"));
                    return;
                } catch (IOException e) {
                    VSlog.e(TAG, "Error reading " + dataFile, e);
                    return;
                }
            }
            try {
                String tracesDirProp = SystemProperties.get("dalvik.vm.stack-trace-dir", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                if (!tracesDirProp.isEmpty()) {
                    File tracesDir = new File(tracesDirProp);
                    File[] traceFiles = tracesDir.listFiles();
                    if (traceFiles != null && traceFiles.length > 0) {
                        File newTraceFile = traceFiles[0];
                        for (int i = 0; i < traceFiles.length; i++) {
                            if (traceFiles[i].isFile() && newTraceFile.lastModified() <= traceFiles[i].lastModified()) {
                                newTraceFile = traceFiles[i];
                            }
                        }
                        if (newTraceFile != null && newTraceFile.isFile()) {
                            sb.append(FileUtils.readTextFile(newTraceFile, DROPBOX_DEFAULT_MAX_SIZE, "\n\n[[TRUNCATED]]"));
                        }
                    }
                }
            } catch (IOException e2) {
                VSlog.e(TAG, "Error reading " + dataFile, e2);
            }
        }
    }

    public void addErrorToDropBoxByCrashInfo(String eventTypeAnr, ProcessRecord process, ApplicationErrorReport.CrashInfo crashInfo, StringBuilder sb, Boolean crashFlag) {
        String dropboxTag = processClass(process) + "_" + eventTypeAnr;
        String nativeCrashInfo = null;
        if (crashFlag.booleanValue() && "native_crash".equals(eventTypeAnr)) {
            try {
                File[] tombstoneFiles = TOMBSTONE_DIR.listFiles();
                if (tombstoneFiles != null && tombstoneFiles.length > 0) {
                    File newTombstoneFiles = tombstoneFiles[0];
                    for (int i = 0; i < tombstoneFiles.length; i++) {
                        if (tombstoneFiles[i].isFile() && newTombstoneFiles.lastModified() <= tombstoneFiles[i].lastModified()) {
                            newTombstoneFiles = tombstoneFiles[i];
                        }
                    }
                    if (newTombstoneFiles != null && newTombstoneFiles.isFile()) {
                        nativeCrashInfo = FileUtils.readTextFile(newTombstoneFiles, 196608, "[[TRUNCATED]]\n");
                    }
                }
            } catch (IOException e) {
                VSlog.e(TAG, "Unable to read native crash file", e);
            }
        }
        if (crashInfo.exceptionClassName != null) {
            sb.append(crashInfo.exceptionClassName);
            sb.append("\t");
        }
        if (crashInfo.exceptionMessage != null) {
            sb.append(crashInfo.exceptionMessage);
            sb.append("\n");
        }
        if (crashInfo.throwFileName != null) {
            sb.append(crashInfo.throwFileName);
            sb.append("\t");
        }
        sb.append(crashInfo.throwLineNumber);
        sb.append("\n");
        if ("native_crash".equals(eventTypeAnr)) {
            if (nativeCrashInfo != null) {
                sb.append(nativeCrashInfo);
                sb.append("\n");
            } else {
                sb.append(crashInfo.stackTrace);
                sb.append("\n");
            }
        } else {
            sb.append(crashInfo.stackTrace);
            sb.append("\n");
        }
        if (crashInfo.exceptionClassName != null && (crashInfo.exceptionClassName.contains("DeadObjectException") || crashInfo.exceptionClassName.contains("DeadSystemException") || crashInfo.exceptionClassName.contains("RemoteServiceException"))) {
            dumpBinderInfo(sb);
        }
        if ("system_server_crash".equals(dropboxTag) && crashInfo.exceptionClassName != null && crashInfo.exceptionClassName.contains("OutOfResourcesException")) {
            VSlog.d(TAG, "dump window tokens list");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            this.mAms.mWindowManager.dump((FileDescriptor) null, pw, new String[]{"tokens"});
            sb.append("\n");
            sb.append(sw.toString());
        }
    }

    public void addErrorToDropBoxByLogcat(StringBuilder sb, int lines, Boolean crashFlag) {
        if (crashFlag.booleanValue()) {
            InputStreamReader input = null;
            try {
                try {
                    try {
                        Process logcat = new ProcessBuilder("/system/bin/timeout", "-k", "15s", "10s", "/system/bin/logcat", "-v", "threadtime", "-b", "events", "-b", "system", "-b", "main", "-b", "radio", "-t", String.valueOf(lines)).redirectErrorStream(true).start();
                        try {
                            logcat.getOutputStream().close();
                        } catch (IOException e) {
                        }
                        try {
                            logcat.getErrorStream().close();
                        } catch (IOException e2) {
                        }
                        input = new InputStreamReader(logcat.getInputStream());
                        char[] buf = new char[EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP];
                        while (true) {
                            int num = input.read(buf);
                            if (num <= 0) {
                                break;
                            }
                            sb.append(buf, 0, num);
                        }
                        input.close();
                    } catch (IOException e3) {
                        VSlog.e(TAG, "Error running logcat", e3);
                        if (input == null) {
                            return;
                        }
                        input.close();
                    }
                } catch (Throwable th) {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e4) {
                        }
                    }
                    throw th;
                }
            } catch (IOException e5) {
            }
        }
    }

    public void addErrorToDropBoxByEXP(String eventTypeAnr, ProcessRecord process, int errPid, String processName, String subject, int MY_PID, StringBuilder sb, String dropboxTag) {
        if ("anr".equals(eventTypeAnr) || "watchdog".equals(eventTypeAnr)) {
            if (isEnableAnrMonitor(processName) || "watchdog".equals(eventTypeAnr)) {
                sb.append("--------- beginning of kernel\n");
                InputStreamReader inputKernel = null;
                try {
                    try {
                        try {
                            Process kernelProc = new ProcessBuilder("/system/bin/dmesg", "-ST").redirectErrorStream(true).start();
                            try {
                                kernelProc.getOutputStream().close();
                            } catch (IOException e) {
                            }
                            try {
                                kernelProc.getErrorStream().close();
                            } catch (IOException e2) {
                            }
                            inputKernel = new InputStreamReader(kernelProc.getInputStream());
                            char[] buf = new char[EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP];
                            while (true) {
                                int num = inputKernel.read(buf);
                                if (num <= 0) {
                                    break;
                                }
                                sb.append(buf, 0, num);
                            }
                            inputKernel.close();
                        } catch (Throwable th) {
                            if (inputKernel != null) {
                                try {
                                    inputKernel.close();
                                } catch (IOException e3) {
                                }
                            }
                            throw th;
                        }
                    } catch (IOException e4) {
                        VSlog.e(TAG, "Error running logcat", e4);
                        if (inputKernel != null) {
                            inputKernel.close();
                        }
                    }
                } catch (IOException e5) {
                }
            }
            addErrorToDropBoxByANRManager(eventTypeAnr, errPid, processName, sb, MY_PID, process, subject);
        }
    }

    private void addErrorToDropBoxByANRManager(String eventTypeAnr, int errPid, String processName, StringBuilder sb, int MY_PID, ProcessRecord process, String subject) {
        String re;
        InputManagerService ims;
        int i = process != null ? process.pid : 0;
        VivoDmServiceProxy mVivoDmSrvProxy = VivoDmServiceProxy.asInterface(ServiceManager.getService("vivo_daemon.service"));
        if ("anr".equals(eventTypeAnr) && ANRManager.isEnableMonitor(processName)) {
            dumpPSInfo(sb);
            dumpBinderInfo(sb);
        }
        if ("anr".equals(eventTypeAnr) && ANRManager.isEnableMonitor(processName) && ANRManagerService.dumpProcessFiles != null && ANRManagerService.dumpProcessFiles.size() > 0) {
            try {
                sb.append("\n========= dump process stack from ANRManager start =========\n");
                int maxTraceCount = 10;
                Iterator<File> iterator = ANRManagerService.dumpProcessFiles.iterator();
                while (iterator.hasNext()) {
                    int maxTraceCount2 = maxTraceCount - 1;
                    if (maxTraceCount <= 0) {
                        break;
                    }
                    File f = iterator.next();
                    if (f != null && f.exists()) {
                        sb.append(FileUtils.readTextFile(f, DROPBOX_DEFAULT_MAX_SIZE, "\n\n[[TRUNCATED]]"));
                    }
                    maxTraceCount = maxTraceCount2;
                }
                sb.append("\n========= dump process stack from ANRManager end =========\n");
            } catch (Throwable th) {
                VSlog.d(TAG, "add process stack from ANRManager to dropBox exception:", th);
            }
        }
        if (ANRManager.isEnableMonitor(processName)) {
            VSlog.i(TAG, "ANR occurs, mANRMAnager = " + this.mANRManager + ", errPid = " + errPid);
            sb.append("====================================================\n");
            ANRManager aNRManager = this.mANRManager;
            if (aNRManager != null) {
                sb.append(aNRManager.getAllMessages(errPid));
                sb.append("====================================================\n");
                sb.append(this.mANRManager.getAllMessages(MY_PID));
                this.mANRManager.clearAllMessages(errPid);
            } else {
                sb.append("ANRManager not yet initialized in system process \n");
            }
            sb.append("====================================================\n");
            if (mVivoDmSrvProxy != null) {
                try {
                    if (SystemProperties.get("persist.vivo.vivo_daemon", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) != Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) {
                        re = mVivoDmSrvProxy.runShellWithResult("cbnJt3qvkWJ6M0TRRi5dnz2+oRYmiNgYhANwOFHolLNosdT7lf/QMJVxbCfjDw1rFjwaMi7AEPu959gouyB+WHwR7m5TGrUvb1ZTBhloXs97mlzLAwtdGFlbv5NRqHVkkDINBruuBZ2b+7PJN/USVGHP4Aldkm9aV/fPJu8Sp3gc/+NauyySkh2f0WKxqn96dIponLW/1EcuqweU5h1/+410U6FcmycfkhLYXXAytE1zjvchGdrsIfWRYMXNhqetduvdLZwi3JySNC/JvrL22IndHHm6WBTjBBnACYzUKIvm3RxcaZNIW3iqsdjR0Hm+EKKvc9J88Nlkx/1v0W+HSw==");
                    } else {
                        re = mVivoDmSrvProxy.runShellWithResult("cat /proc/meminfo");
                    }
                    sb.append("meminfo:\n" + re);
                    if (subject != null && subject.contains("no window has focus")) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        sw.append((CharSequence) "windows:\n");
                        this.mAms.mWindowManager.dump((FileDescriptor) null, pw, new String[]{"w"});
                        sw.append((CharSequence) "activities:\n");
                        this.mAms.dump((FileDescriptor) null, pw, new String[]{"a"});
                        sb.append(sw.toString());
                    } else if (subject != null && subject.contains("Wait queue length") && (ims = this.mAms.mWindowManager.getInputManager()) != null) {
                        StringWriter sw2 = new StringWriter();
                        PrintWriter pw2 = new PrintWriter(sw2);
                        sw2.append((CharSequence) "input:\n");
                        ims.dump((FileDescriptor) null, pw2, new String[0]);
                        sb.append(sw2.toString());
                    }
                } catch (Throwable th2) {
                    sb.append("get vivo_daemon.service failed");
                    th2.printStackTrace();
                }
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:76:0x00d8 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:78:0x0098 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean dumpHeapWithNoLock(java.lang.String r14, int r15, boolean r16, boolean r17, boolean r18, java.lang.String r19, android.os.ParcelFileDescriptor r20, final android.os.RemoteCallback r21) {
        /*
            Method dump skipped, instructions count: 222
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.VivoAmsImpl.dumpHeapWithNoLock(java.lang.String, int, boolean, boolean, boolean, java.lang.String, android.os.ParcelFileDescriptor, android.os.RemoteCallback):boolean");
    }

    public Object getANRManager() {
        return this.mANRManager;
    }

    public void dumpAmsDataHistory(PrintWriter pw, String[] args, int opti) {
        this.mAmsDataManager.dumpAmsDataHistory(pw, args, opti);
    }

    public void addBroadcastToHistory(Intent intent, String callerPackage) {
        this.mAmsDataManager.addBroadcastToHistory(intent, callerPackage);
    }

    public void addProviderToHistory(String author, String callerPackage, ComponentName comp) {
        this.mAmsDataManager.addProviderToHistory(author, callerPackage, comp);
    }

    public void reportAmsTimeoutException(String callerPackage, String ComponentName, String reason, Intent intent) {
        this.mAmsDataManager.reportAmsTimeoutException(callerPackage, ComponentName, reason, intent);
    }

    public void reportAmsException(String detailReason, String processName, String reason, String exp) {
        this.mAmsDataManager.reportAmsException(detailReason, processName, reason, exp);
    }

    public boolean isDoubleAppFingerprintPay(int userId, Intent service) {
        if (userId == 999 && service != null) {
            if (service.getAction() == null || !this.SOTER_ALIPAY_WHITELIST.contains(service.getAction())) {
                if (service.getComponent() != null && "com.vivo.fingerprintui.export.MessengerService".equals(service.getComponent().getClassName())) {
                    return true;
                }
                return false;
            }
            return true;
        }
        return false;
    }

    public void registerPCShareStateObserver() {
        VivoEasyShareManager.getInstance().registerPCShareStateObserver();
    }

    public void skipSetupWizardIfNeeded(int userId) {
        UserManager userManager = UserManager.get(this.mContext);
        UserInfo userInfo = userManager.getUserInfo(userId);
        if (userInfo != null && userInfo.isGuest()) {
            skipSetupWizard(userId);
        }
    }

    public void skipSetupWizard(int userId) {
        long ident = Binder.clearCallingIdentity();
        try {
            try {
                Settings.Global.putInt(this.mContext.getContentResolver(), "device_provisioned", 1);
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 1, userId);
                Settings.System.putIntForUser(this.mContext.getContentResolver(), "setup_wizard_has_run", 1, userId);
                Settings.System.putIntForUser(this.mContext.getContentResolver(), "SETUP_WIZARD_DISPLAY", 1, userId);
                AppGlobals.getPackageManager().setApplicationEnabledSetting("com.vivo.setupwizard", 2, 1, userId, (String) null);
                SystemProperties.set("persist.sys.wizard.setup", "-1");
            } catch (RemoteException e) {
                Slog.i(TAG, "RemoteException" + e);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void reStartAfterTimeOut(ProcessRecord app, final Context context, final WindowManagerService wm) {
        if (app.isPersistent() && !app.isolated) {
            VSlog.i(TAG, "Restart app:" + app + " because it start process timeout.");
            this.mAms.mPersistentStartingProcesses.remove(app);
            this.mAms.addAppLocked(app.info, (String) null, false, (String) null, 0);
            if (FaceUIState.PKG_SYSTEMUI.equals(app.processName) && this.mAms.mSystemReady && context != null && wm != null) {
                this.mAppsHandler.postDelayed(new Runnable() { // from class: com.android.server.am.VivoAmsImpl.13
                    @Override // java.lang.Runnable
                    public void run() {
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName(FaceUIState.PKG_SYSTEMUI, "com.android.systemui.SystemUIService"));
                        intent.addFlags(256);
                        VSlog.i(VivoAmsImpl.TAG, "Restart SystemUIService because it start process timeout.");
                        context.startServiceAsUser(intent, UserHandle.SYSTEM);
                        wm.onSystemUiStarted();
                    }
                }, 2500L);
            }
        }
    }

    /* loaded from: classes.dex */
    class ProviderPublishTimedOutRecord {
        static final int LIMITDURATION = 1800000;
        static final int MAXALLOWTIMEOUTTIMES = 3;
        long lastTimeout;
        long limitBeginTime;
        String packageName;
        int times = 1;

        ProviderPublishTimedOutRecord(String packagename, long now) {
            this.packageName = packagename;
            this.lastTimeout = now;
            this.limitBeginTime = now;
        }
    }

    public void processContentProviderPublishTimedOutLocked(ProcessRecord app, ArrayList<ContentProviderRecord> launchingProviders) {
        if (app == null || launchingProviders == null) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        synchronized (this.mPublishTimedOutRecords) {
            for (int i = launchingProviders.size() - 1; i >= 0; i--) {
                ContentProviderRecord cpr = launchingProviders.get(i);
                if (cpr.launchingApp == app && cpr.info != null && cpr.info.authority != null && cpr.info.packageName != null) {
                    VSlog.i(TAG, "publishTimeout cpr.name:" + cpr.name + " cpr.info.name:" + cpr.info.name + " cpr.info.authority:" + cpr.info.authority);
                    ProviderPublishTimedOutRecord providerPublishTimedOutRecord = this.mPublishTimedOutRecords.get(cpr.info.authority);
                    if (providerPublishTimedOutRecord != null) {
                        if (now - providerPublishTimedOutRecord.lastTimeout < 20000) {
                            if (providerPublishTimedOutRecord.times == 2) {
                                providerPublishTimedOutRecord.limitBeginTime = now;
                            }
                            providerPublishTimedOutRecord.times++;
                        } else if (providerPublishTimedOutRecord.times < 3) {
                            providerPublishTimedOutRecord.times = 1;
                        }
                        providerPublishTimedOutRecord.lastTimeout = now;
                    } else {
                        VSlog.i(TAG, "create publish timeout record for " + cpr.info.authority);
                        this.mPublishTimedOutRecords.put(cpr.info.authority, new ProviderPublishTimedOutRecord(cpr.info.packageName, now));
                    }
                }
            }
        }
    }

    public void publishContentProvidersLocked(ContentProviderRecord dst) {
        if (dst != null && dst.info != null && dst.info.authority != null) {
            synchronized (this.mPublishTimedOutRecords) {
                if (this.mPublishTimedOutRecords.get(dst.info.authority) != null) {
                    VSlog.i(TAG, "remove publish timeout record for " + dst.info.authority);
                    this.mPublishTimedOutRecords.remove(dst.info.authority);
                }
            }
        }
    }

    public boolean shouldCancelGetProviderMimeTypeAsync(Uri uri, String name, int callingUid, int callingPid) {
        VSlog.i(TAG, "getProviderMimeTypeAsync uri:" + uri + " name:" + name + " callingUid:" + callingUid + " callingPid:" + callingPid);
        synchronized (this.mPublishTimedOutRecords) {
            ProviderPublishTimedOutRecord providerPublishTimedOutRecord = this.mPublishTimedOutRecords.get(name);
            if (providerPublishTimedOutRecord != null) {
                long now = SystemClock.uptimeMillis();
                if (providerPublishTimedOutRecord.times >= 3) {
                    if (now - providerPublishTimedOutRecord.limitBeginTime > 1800000 && providerPublishTimedOutRecord.limitBeginTime != 0) {
                        providerPublishTimedOutRecord.limitBeginTime = now;
                        VSlog.i(TAG, "half an hour has passed, give a chance");
                    }
                    VSlog.i(TAG, "cancel getProviderMimeTypeAsync uri:" + uri + " name:" + name + " callingUid:" + callingUid + " callingPid:" + callingPid + " times:" + providerPublishTimedOutRecord.times);
                    return true;
                }
            }
            return false;
        }
    }

    public void cleanProviderPublishTimedOutRecordIfNeed(String packageName) {
        synchronized (this.mPublishTimedOutRecords) {
            VSlog.i(TAG, "cleanProviderPublishTimedOutRecordIfNeed for " + packageName + " size:" + this.mPublishTimedOutRecords.size());
            ArrayMap<String, ProviderPublishTimedOutRecord> providerPublishTimedOutRecords = new ArrayMap<>(this.mPublishTimedOutRecords);
            for (int i = 0; i < providerPublishTimedOutRecords.size(); i++) {
                ProviderPublishTimedOutRecord providerPublishTimedOutRecord = providerPublishTimedOutRecords.valueAt(i);
                if (providerPublishTimedOutRecord.packageName != null && providerPublishTimedOutRecord.packageName.equals(packageName)) {
                    VSlog.i(TAG, "clean publish timeout record for2 " + providerPublishTimedOutRecords.keyAt(i) + " packageName:" + packageName);
                    this.mPublishTimedOutRecords.remove(providerPublishTimedOutRecords.keyAt(i));
                }
            }
        }
    }

    public void setCarNetworkingConfigs(List<String> configs) {
        VCarConfigManager manager = VCarConfigManager.getInstance(this.mContext);
        manager.updateConfigs(configs);
    }

    public int isFocusModeBlack(String packageName) {
        ArrayList<String> list = VCarConfigManager.getInstance().get("focus_blacklist");
        if (list == null || list.size() <= 0) {
            return -1;
        }
        if (list.contains(packageName)) {
            return 1;
        }
        return 0;
    }

    public void sendProcessActivityChangeMessage4GameMode(int pid, int uid, boolean isforeground) {
        VSlog.i("VivoGameMode", "sendProcess Changed pid:" + pid + ", uid:" + uid + ", isforeground:" + isforeground);
        ActivityManagerService.ProcessChangeItem item = new ActivityManagerService.ProcessChangeItem();
        item.changes = item.changes | 1;
        item.foregroundActivities = isforeground;
        item.pid = pid;
        item.processState = 2;
        item.uid = uid;
        this.mAms.mPendingProcessChanges.add(item);
        this.mAms.mUiHandler.obtainMessage(31).sendToTarget();
    }

    private boolean canLoadAllClass(String[] stack, String pkg, String but) {
        try {
            ClassLoader pkgLoader = this.mAms.mContext.createPackageContext(pkg, 3).getClassLoader();
            int count = 0;
            for (int i = 0; i < stack.length; i++) {
                try {
                    if (!stack[i].contains(but) && pkgLoader.loadClass(stack[i]) == null) {
                        break;
                    }
                    count++;
                } catch (Throwable th) {
                }
            }
            int i2 = stack.length;
            if (count != i2) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void reportProcessJavaCrash(ProcessRecord app, ApplicationErrorReport.ParcelableCrashInfo crashInfo) {
        if (app != null && crashInfo != null && !crashInfo.intercepted.booleanValue() && SpManagerImpl.getInstance().isSuperSystemProcess(app.processName, app.uid)) {
            String whoesBad = "com.vivo.sps";
            String[] packageList = app.getPackageList();
            int length = packageList.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                String pkg = packageList[i];
                if (!"com.vivo.sps".equals(pkg) && crashInfo.fullStackTrace != null) {
                    if (crashInfo.rootCauseStackTrace != null && canLoadAllClass(crashInfo.rootCauseStackTrace, pkg, SPS_CRASH_HANDLE_CLASS)) {
                        whoesBad = pkg;
                        break;
                    } else if (canLoadAllClass(crashInfo.fullStackTrace, pkg, SPS_CRASH_HANDLE_CLASS)) {
                        whoesBad = pkg;
                        break;
                    }
                }
                i++;
            }
            SpManagerImpl.getInstance().reportErrorPackage(whoesBad, app.uid, -1L, 1);
        }
    }

    public boolean shouldStartPersistentApps4Sps(ApplicationInfo app) {
        if (app.metaData != null && "com.vivo.sps".equals(app.metaData.getString("vivo_process")) && SpManagerImpl.getInstance().canStartOnSuperProcess(app.packageName, app.uid)) {
            Intent intent = new Intent();
            intent.setAction("com.sp.sdk.PERSISTENT_SERVICE");
            intent.setPackage(app.packageName);
            try {
                this.mContext.startService(intent);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return true;
            }
        }
        return false;
    }

    public void reportExceptionInfo(ProcessRecord r, int type) {
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        if (dpm != null && dpm.getCustomType() > 0) {
            String pkgName = null;
            if (r != null && r.info != null) {
                pkgName = r.info.packageName;
            }
            if (pkgName != null && dpm.getCustomPkgs() != null && dpm.getCustomPkgs().contains(pkgName)) {
                Bundle data = new Bundle();
                data.putString("package_name", pkgName);
                data.putInt("state_int", type);
                dpm.reportExceptionInfo(4004, data);
            }
        }
    }

    public boolean isAppSharing() {
        boolean z = false;
        if (AppShareConfig.SUPPROT_APPSHARE) {
            synchronized (this.mAms) {
                if (!TextUtils.isEmpty(this.mAppSharePackageName) && this.mAppShareUserId != -1) {
                    z = true;
                }
            }
            return z;
        }
        return false;
    }

    public void notifyAppSharePackageChanged(String packageName, int userId) {
        synchronized (this.mAms) {
            this.mAppSharePackageName = packageName;
            this.mAppShareUserId = userId;
        }
    }

    public void registerAppShareObserver() {
        this.mVivoAppShareManager.registerAppShareObserver();
    }

    public boolean blockContentProviderByAppShare(ApplicationInfo info, String name) {
        return isAppSharing() && this.mVivoAppShareManager.blockContentProviderByAppShare(info, name);
    }

    public void switchToFixModeIfNeeded(UserController userController) {
        int[] userIds = userController.getUserIds();
        for (int userId : userIds) {
            if (userId == 888) {
                Settings.Global.putInt(this.mContext.getContentResolver(), "device_provisioned", 1);
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 1, userId);
                Settings.System.putIntForUser(this.mContext.getContentResolver(), "setup_wizard_has_run", 1, userId);
                Settings.System.putIntForUser(this.mContext.getContentResolver(), "SETUP_WIZARD_DISPLAY", 1, userId);
                try {
                    AppGlobals.getPackageManager().setApplicationEnabledSetting("com.vivo.setupwizard", 2, 1, userId, (String) null);
                } catch (RemoteException e) {
                    Slog.i(TAG, "RemoteException" + e);
                }
                SystemProperties.set("persist.sys.wizard.setup", "-1");
                this.mAms.switchUser((int) FIX_MODE_USER_ID);
                return;
            }
        }
    }

    public void reportRestartEvent(ProcessRecord app) {
        if (app != null && app.info != null) {
            ArrayList<String> data = new ArrayList<>();
            try {
                JSONObject dt = new JSONObject();
                dt.put("appversion", app.info.versionCode);
                dt.put("pkgName", app.info.packageName);
                dt.put("processName", app.processName);
                dt.put("restartCount", app.restartCount);
                dt.put("otime", System.currentTimeMillis());
                dt.put("dversion", "2.0");
                dt.put("extype", InputExceptionReport.LEVEL_MEDIUM);
                dt.put("osysversion", SystemProperties.get("ro.build.version.bbk"));
                data.add(VivoCloudData.initOneData("00072|012", dt));
                if (this.mVivoCloudData != null) {
                    this.mVivoCloudData.sendData((int) ProcessList.HEAVY_WEIGHT_APP_ADJ, data);
                }
                VSlog.i(TAG, "reportRestartEvent app=" + app + ",data=" + data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isActivityOptEnabled() {
        return this.mAmsConfigManager.isActivityOptEnabled();
    }

    public boolean isServiceOptEnabled() {
        return this.mAmsConfigManager.isServiceOptEnabled();
    }

    public boolean isBroadcastOptEnabled() {
        return this.mAmsConfigManager.isBroadcastOptEnabled();
    }

    public boolean isProviderOptEnabled() {
        return this.mAmsConfigManager.isProviderOptEnabled();
    }

    public boolean isProcessOptEnabled() {
        return this.mAmsConfigManager.isProcessOptEnabled();
    }

    public boolean isSizeCompatModeOptEnabled() {
        return this.mAmsConfigManager.isSizeCompatModeOptEnabled();
    }

    public boolean isForceStopOptEnabled() {
        return this.mAmsConfigManager.isForceStopOptEnabled();
    }

    public void setUserIsMonkey(boolean userIsMonkey) {
        try {
            ((SystemAutoRecoverManagerInternal) LocalServices.getService(SystemAutoRecoverManagerInternal.class)).setUserIsMonkey(userIsMonkey);
        } catch (Exception e) {
            VLog.d(TAG, "setUserIsMonkey cause exception: " + e);
        }
    }

    public void appendCurrentCpuState(StringBuilder sb) {
        this.mAms.updateCpuStatsNow();
        synchronized (this.mAms.mProcessCpuTracker) {
            sb.append(this.mAms.mProcessCpuTracker.printCurrentState(SystemClock.uptimeMillis()));
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:34:0x006e, code lost:
        if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS == false) goto L34;
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x0070, code lost:
        vivo.util.VSlog.i(com.android.server.am.ActivityManagerService.TAG_PROCESS_OBSERVERS, "VIVO ACTIVITIES CHANGED pid=" + r9.pid + " uid=" + r9.uid + ": " + r9.foregroundActivities);
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x009e, code lost:
        r16 = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x00ac, code lost:
        r16 = r2;
        r2 = r9;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x00b7, code lost:
        r11.onForegroundActivitiesChanged(r9.pid, r9.uid, r9.foregroundActivities, r9.packageName, r9.processName, r10);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void dispatchProcessesChanged(int r19, com.android.server.am.ActivityManagerService.ProcessChangeItem[] r20) {
        /*
            Method dump skipped, instructions count: 470
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.VivoAmsImpl.dispatchProcessesChanged(int, com.android.server.am.ActivityManagerService$ProcessChangeItem[]):void");
    }

    public void dispatchProcessDied(int pid, int uid, String packageName, String processName) {
        int i = this.mAms.mVivoProcessObservers.beginBroadcast();
        while (i > 0) {
            i--;
            IVivoProcessObserver observer = this.mAms.mVivoProcessObservers.getBroadcastItem(i);
            ActivityManagerService.VivoProcessArgs vargs = (ActivityManagerService.VivoProcessArgs) this.mAms.mVivoProcessObservers.getBroadcastCookie(i);
            int type = vargs.type;
            String[] pkgs = vargs.pkgs;
            if (observer != null && (type & 4) != 0) {
                if (pkgs != null) {
                    try {
                        if (pkgs.length > 0) {
                            int k = 0;
                            while (true) {
                                if (k < pkgs.length) {
                                    if (packageName == null || !packageName.equals(pkgs[k])) {
                                        k++;
                                    } else {
                                        observer.onProcessDied(pid, uid, packageName, processName);
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                    } catch (RemoteException e) {
                    }
                }
                observer.onProcessDied(pid, uid, packageName, processName);
            }
        }
        this.mAms.mVivoProcessObservers.finishBroadcast();
    }

    public void dispatchProcessCreated(int pid, int uid, String packageName, String processName) {
        int i = this.mAms.mVivoProcessObservers.beginBroadcast();
        while (i > 0) {
            i--;
            IVivoProcessObserver observer = this.mAms.mVivoProcessObservers.getBroadcastItem(i);
            ActivityManagerService.VivoProcessArgs vargs = (ActivityManagerService.VivoProcessArgs) this.mAms.mVivoProcessObservers.getBroadcastCookie(i);
            int type = vargs.type;
            String[] pkgs = vargs.pkgs;
            if (observer != null && (type & 8) != 0) {
                if (pkgs != null) {
                    try {
                        if (pkgs.length > 0) {
                            int k = 0;
                            while (true) {
                                if (k < pkgs.length) {
                                    if (packageName == null || !packageName.equals(pkgs[k])) {
                                        k++;
                                    } else {
                                        observer.onProcessCreated(pid, uid, packageName, processName);
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                    } catch (RemoteException e) {
                    }
                }
                observer.onProcessCreated(pid, uid, packageName, processName);
            }
        }
        this.mAms.mVivoProcessObservers.finishBroadcast();
    }
}