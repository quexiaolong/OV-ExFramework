package com.vivo.services.systemdefence;

import android.app.AppGlobals;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import com.android.internal.util.DumpUtils;
import com.android.server.am.SystemDefenceHelper;
import com.vivo.face.common.data.Constants;
import com.vivo.services.superresolution.Constant;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.ContentValuesList;
import vivo.app.configuration.StringList;
import vivo.app.epm.ExceptionPolicyManager;
import vivo.app.systemdefence.ISystemDefenceManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class SystemDefenceService extends ISystemDefenceManager.Stub {
    private static final String APPTRANSITIONTIMEOUT_PACKAGES = "appTransition_timeout_packages";
    private static final long APP_TRANSITION_TIMEOUT_MS = 3500;
    private static final String CRASH_REASON_GPU = "vendor/lib64/egl";
    private static final String CRASH_REASON_WEBVIEW = "TrichromeLibrary";
    private static final String DELAY_UPDATE_PACKAGES = "delay_update_packages";
    private static final String DUMPSTACKTRACE_PACKAGES = "dumpStackTrace_packages";
    private static final int EXCEPTION_TYPE_SYSTEM_DEFENCE = 42;
    private static final String KEY_APPTRANSITION_TIMEOUT = "apptransition_timeout_ms";
    private static final String KEY_SMOOTH_ENABLE = "smooth_enable";
    private static final int MSG_DUMP_STACKTRACE = 100;
    private static final String SKIP_REGISTEROBSERVER_PACKAGES = "skip_registerObserver_packages";
    private static final String SKIP_RUNNING_NOTIFICATION_PACKAGES = "skip_running_notification_packages";
    private static final int SUBTYPE_CRASH_RESON = 9;
    private static final int SUBTYPE_DELAY_UPDATE = 4;
    private static final int SUBTYPE_FG_SERVICE_ANR = 3;
    private static final int SUBTYPE_FG_SERVICE_CRASH = 2;
    private static final int SUBTYPE_KILLSELF = 8;
    private static final int SUBTYPE_QQ_DBCORRUPT = 5;
    private static final int SUBTYPE_SMODEL_KILL = 7;
    private static final int SUBTYPE_START_BG_SERVICE = 1;
    private static final int SUBTYPE_WX_DBCORRUPT = 6;
    private static final String SWITCH_APPTRANSITION_TIMEOUT = "switch_apptransition_timeout";
    private static final String SWITCH_DUMP_STACKTRACE = "switch_dump_stacktrace";
    private static final String SWITCH_FG_SERVICE_ANR = "switch_fg_service_anr";
    private static final String SWITCH_FG_SERVICE_CRASH = "switch_fg_service_crash";
    private static final String SWITCH_SKIP_KILLED_BY_REMOVETASK = "switch_skip_killed_by_removetask";
    private static final String SWITCH_SKIP_REGISTER_OBSERVER = "switch_skip_register_observer";
    private static final String SWITCH_SKIP_RUNNING_NOTIFICATION = "switch_skip_running_notification";
    private static final String SWITCH_START_BG_SERVICE = "switch_start_bg_service";
    private static final String SYSTEMDEFENCE_CONFIG_FILE = "/data/bbkcore/systemdefence_config_v1.xml";
    private static final String SYSTEMDEFENCE_CONFIG_LIST_FILE = "/data/bbkcore/systemdefence_config_list.xml";
    private static final String SYSTEMDEFENCE_SWITCH = "systemdefence_switch";
    private static final String TAG = "SDS";
    private final boolean IS_LOG_CTRL_OPEN;
    private List<String> mAppTransitionTimeoutList;
    private ConfigurationObserver mAppTransitionTimeoutObserver;
    private StringList mAppTransitionTimeoutStringList;
    private long mApptransitionTimeoutMs;
    private boolean mApptransitionTimeoutSwitch;
    private AbsConfigurationManager mConfigurationManager;
    private Context mContext;
    private String mCrashPkgName;
    private List<String> mDelayUpdateList;
    private ConfigurationObserver mDelayUpdatePackagesObserver;
    private StringList mDelayUpdatePackagesStringList;
    private String[] mDumpStacktraceInterests;
    private boolean mDumpStacktraceSwitch;
    private ConfigurationObserver mDumpTracePackageObserver;
    private List<String> mDumpTracePackagesList;
    private StringList mDumpTracePackagesStringList;
    private boolean mFgServiceAnrSwitch;
    private boolean mFgServiceCrashSwitch;
    private SystemDefenceHandler mHandler;
    private HandlerThread mHandlerThread;
    private String mResumePackageName;
    private ApplicationInfo mResumedAppInfo;
    private int mResumedPid;
    private boolean mSkipKilledByRemoveTaskSwitch;
    private List<String> mSkipRegisterObserverList;
    private StringList mSkipRegisterObserverStringList;
    private boolean mSkipRegisterObserverSwitch;
    private ConfigurationObserver mSkipReigsterObserverObserver;
    private boolean mSkipRuningNotificationSwitch;
    private List<String> mSkipRunningNotificationList;
    private ConfigurationObserver mSkipRunningNotificationPackagesObserver;
    private StringList mSkipRunningNotificationPackagesStringList;
    private List<String> mSkipUploadedReason;
    private int mSmoothEnable;
    private boolean mStartBgServcieSwitch;
    private ConfigurationObserver mSwitchObserver;
    private ContentValuesList mSwitchValuesList;
    private VivoFrameworkFactory mVivoFrameworkFactory;

    /* loaded from: classes.dex */
    class SystemDefenceHandler extends Handler {
        public SystemDefenceHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                VSlog.i(SystemDefenceService.TAG, "handle dumpStackTraceOfInterrest.");
                SystemDefenceService.this.dumpStackTraceOfInterrest();
            }
        }
    }

    public SystemDefenceService(Context context) {
        this.IS_LOG_CTRL_OPEN = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes") || Build.TYPE.equals("eng");
        this.mDumpTracePackagesList = Arrays.asList("com.tencent.tmgp.sgame", "com.tencent.tmgp.speedmobile", "com.tencent.tmgp.pubgmhd", "com.mobile.legends", "com.garena.game.kgtw", "com.riotgames.league.wildrift", "com.tencent.ig", "com.garena.game.codm", "com.dts.freefireth", "com.imangi.templerun2", "com.kiloo.subwaysurf", Constant.APP_WEIXIN, "com.tencent.mobileqq", "com.whatsapp", Constant.APP_DOUYIN, "com.eg.android.AlipayGphone", "com.xunmeng.pinduoduo", Constant.APP_WEIBO, "com.smile.gifmaker", "com.facebook.katana", "com.instagram.android");
        this.mAppTransitionTimeoutList = new ArrayList<String>() { // from class: com.vivo.services.systemdefence.SystemDefenceService.1
            {
                add(Constant.APP_WEIXIN);
            }
        };
        this.mSkipRunningNotificationList = new ArrayList<String>() { // from class: com.vivo.services.systemdefence.SystemDefenceService.2
            {
                add(Constant.APP_WEIXIN);
                add("com.tencent.mobileqq");
                add("im.yixin");
            }
        };
        this.mSkipRegisterObserverList = new ArrayList<String>() { // from class: com.vivo.services.systemdefence.SystemDefenceService.3
            {
                add("com.google.android.gms.phenotype");
            }
        };
        this.mDelayUpdateList = new ArrayList<String>() { // from class: com.vivo.services.systemdefence.SystemDefenceService.4
            {
                add("com.vivo.contentcatcher");
            }
        };
        this.mSkipUploadedReason = new ArrayList<String>() { // from class: com.vivo.services.systemdefence.SystemDefenceService.5
            {
                add("by com.bbk.launcher2");
                add("by com.vivo.upslide");
            }
        };
        this.mSmoothEnable = 1;
        this.mApptransitionTimeoutMs = APP_TRANSITION_TIMEOUT_MS;
        this.mStartBgServcieSwitch = true;
        this.mApptransitionTimeoutSwitch = true;
        this.mDumpStacktraceSwitch = true;
        this.mSkipRegisterObserverSwitch = true;
        this.mSkipKilledByRemoveTaskSwitch = true;
        this.mSkipRuningNotificationSwitch = true;
        this.mFgServiceCrashSwitch = true;
        this.mFgServiceAnrSwitch = true;
        this.mResumedPid = -1;
        this.mHandlerThread = new HandlerThread("SystemDefeceThread");
        this.mSwitchObserver = new ConfigurationObserver() { // from class: com.vivo.services.systemdefence.SystemDefenceService.7
            public void onConfigChange(String file, String name) {
                if (SystemDefenceService.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(SystemDefenceService.TAG, "onConfigChange file:" + file + ",name=" + name);
                }
                if (SystemDefenceService.this.mConfigurationManager == null) {
                    if (SystemDefenceService.this.IS_LOG_CTRL_OPEN) {
                        VSlog.i(SystemDefenceService.TAG, "mConfigurationManager is null");
                        return;
                    }
                    return;
                }
                SystemDefenceService.this.mHandler.post(new Runnable() { // from class: com.vivo.services.systemdefence.SystemDefenceService.7.1
                    @Override // java.lang.Runnable
                    public void run() {
                        SystemDefenceService.this.mSwitchValuesList = SystemDefenceService.this.mConfigurationManager.getContentValuesList(SystemDefenceService.SYSTEMDEFENCE_CONFIG_FILE, SystemDefenceService.SYSTEMDEFENCE_SWITCH);
                        SystemDefenceService.this.updateSwitchConfig();
                    }
                });
            }
        };
        this.mDumpTracePackageObserver = new ConfigurationObserver() { // from class: com.vivo.services.systemdefence.SystemDefenceService.8
            public void onConfigChange(String file, String name) {
                if (SystemDefenceService.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(SystemDefenceService.TAG, "onConfigChange file:" + file + ",name=" + name);
                }
                if (SystemDefenceService.this.mConfigurationManager == null) {
                    if (SystemDefenceService.this.IS_LOG_CTRL_OPEN) {
                        VSlog.i(SystemDefenceService.TAG, "mConfigurationManager is null");
                        return;
                    }
                    return;
                }
                SystemDefenceService.this.mHandler.post(new Runnable() { // from class: com.vivo.services.systemdefence.SystemDefenceService.8.1
                    @Override // java.lang.Runnable
                    public void run() {
                        SystemDefenceService.this.mDumpTracePackagesStringList = SystemDefenceService.this.mConfigurationManager.getStringList(SystemDefenceService.SYSTEMDEFENCE_CONFIG_LIST_FILE, SystemDefenceService.DUMPSTACKTRACE_PACKAGES);
                        SystemDefenceService.this.updateDumpTracePackages();
                    }
                });
            }
        };
        this.mAppTransitionTimeoutObserver = new ConfigurationObserver() { // from class: com.vivo.services.systemdefence.SystemDefenceService.9
            public void onConfigChange(String file, String name) {
                if (SystemDefenceService.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(SystemDefenceService.TAG, "onConfigChange file:" + file + ",name=" + name);
                }
                if (SystemDefenceService.this.mConfigurationManager == null) {
                    if (SystemDefenceService.this.IS_LOG_CTRL_OPEN) {
                        VSlog.i(SystemDefenceService.TAG, "mConfigurationManager is null");
                        return;
                    }
                    return;
                }
                SystemDefenceService.this.mHandler.post(new Runnable() { // from class: com.vivo.services.systemdefence.SystemDefenceService.9.1
                    @Override // java.lang.Runnable
                    public void run() {
                        SystemDefenceService.this.mAppTransitionTimeoutStringList = SystemDefenceService.this.mConfigurationManager.getStringList(SystemDefenceService.SYSTEMDEFENCE_CONFIG_LIST_FILE, SystemDefenceService.APPTRANSITIONTIMEOUT_PACKAGES);
                        SystemDefenceService.this.updateAppTransitionTimeoutPackages();
                    }
                });
            }
        };
        this.mSkipReigsterObserverObserver = new ConfigurationObserver() { // from class: com.vivo.services.systemdefence.SystemDefenceService.10
            public void onConfigChange(String file, String name) {
                if (SystemDefenceService.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(SystemDefenceService.TAG, "onConfigChange file:" + file + ",name=" + name);
                }
                if (SystemDefenceService.this.mConfigurationManager == null) {
                    if (SystemDefenceService.this.IS_LOG_CTRL_OPEN) {
                        VSlog.i(SystemDefenceService.TAG, "mConfigurationManager is null");
                        return;
                    }
                    return;
                }
                SystemDefenceService.this.mHandler.post(new Runnable() { // from class: com.vivo.services.systemdefence.SystemDefenceService.10.1
                    @Override // java.lang.Runnable
                    public void run() {
                        SystemDefenceService.this.mSkipRegisterObserverStringList = SystemDefenceService.this.mConfigurationManager.getStringList(SystemDefenceService.SYSTEMDEFENCE_CONFIG_LIST_FILE, SystemDefenceService.SKIP_REGISTEROBSERVER_PACKAGES);
                        SystemDefenceService.this.updateSkipRegisterObserverPackages();
                    }
                });
            }
        };
        this.mSkipRunningNotificationPackagesObserver = new ConfigurationObserver() { // from class: com.vivo.services.systemdefence.SystemDefenceService.11
            public void onConfigChange(String file, String name) {
                if (SystemDefenceService.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(SystemDefenceService.TAG, "onConfigChange file:" + file + ",name=" + name);
                }
                if (SystemDefenceService.this.mConfigurationManager == null) {
                    if (SystemDefenceService.this.IS_LOG_CTRL_OPEN) {
                        VSlog.i(SystemDefenceService.TAG, "mConfigurationManager is null");
                        return;
                    }
                    return;
                }
                SystemDefenceService.this.mHandler.post(new Runnable() { // from class: com.vivo.services.systemdefence.SystemDefenceService.11.1
                    @Override // java.lang.Runnable
                    public void run() {
                        SystemDefenceService.this.mSkipRunningNotificationPackagesStringList = SystemDefenceService.this.mConfigurationManager.getStringList(SystemDefenceService.SYSTEMDEFENCE_CONFIG_LIST_FILE, SystemDefenceService.SKIP_RUNNING_NOTIFICATION_PACKAGES);
                        SystemDefenceService.this.updateSkipRunningNotificationPackages();
                    }
                });
            }
        };
        this.mDelayUpdatePackagesObserver = new ConfigurationObserver() { // from class: com.vivo.services.systemdefence.SystemDefenceService.12
            public void onConfigChange(String file, String name) {
                if (SystemDefenceService.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(SystemDefenceService.TAG, "onConfigChange file:" + file + ",name=" + name);
                }
                if (SystemDefenceService.this.mConfigurationManager == null) {
                    if (SystemDefenceService.this.IS_LOG_CTRL_OPEN) {
                        VSlog.i(SystemDefenceService.TAG, "mConfigurationManager is null");
                        return;
                    }
                    return;
                }
                SystemDefenceService.this.mHandler.post(new Runnable() { // from class: com.vivo.services.systemdefence.SystemDefenceService.12.1
                    @Override // java.lang.Runnable
                    public void run() {
                        SystemDefenceService.this.mDelayUpdatePackagesStringList = SystemDefenceService.this.mConfigurationManager.getStringList(SystemDefenceService.SYSTEMDEFENCE_CONFIG_LIST_FILE, SystemDefenceService.DELAY_UPDATE_PACKAGES);
                        SystemDefenceService.this.updateDelayUpdatePackages();
                    }
                });
            }
        };
        if (this.IS_LOG_CTRL_OPEN) {
            VSlog.i(TAG, "SystemDefenceService inital..");
        }
        this.mContext = context;
        this.mHandlerThread.start();
        this.mHandler = new SystemDefenceHandler(this.mHandlerThread.getLooper());
        this.mVivoFrameworkFactory = VivoFrameworkFactory.getFrameworkFactoryImpl();
        this.mHandler.post(new Runnable() { // from class: com.vivo.services.systemdefence.SystemDefenceService.6
            @Override // java.lang.Runnable
            public void run() {
                if (SystemDefenceService.this.mVivoFrameworkFactory != null) {
                    SystemDefenceService systemDefenceService = SystemDefenceService.this;
                    systemDefenceService.mConfigurationManager = systemDefenceService.mVivoFrameworkFactory.getConfigurationManager();
                    SystemDefenceService.this.initAndRegistSwitch();
                    SystemDefenceService.this.initAndRegistStringList();
                    SystemDefenceService.this.updateSwitchConfig();
                    SystemDefenceService.this.updateSystemDefenceStringList();
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initAndRegistSwitch() {
        ContentValuesList contentValuesList = this.mConfigurationManager.getContentValuesList(SYSTEMDEFENCE_CONFIG_FILE, SYSTEMDEFENCE_SWITCH);
        this.mSwitchValuesList = contentValuesList;
        this.mConfigurationManager.registerObserver(contentValuesList, this.mSwitchObserver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initAndRegistStringList() {
        StringList stringList = this.mConfigurationManager.getStringList(SYSTEMDEFENCE_CONFIG_LIST_FILE, DUMPSTACKTRACE_PACKAGES);
        this.mDumpTracePackagesStringList = stringList;
        this.mConfigurationManager.registerObserver(stringList, this.mDumpTracePackageObserver);
        StringList stringList2 = this.mConfigurationManager.getStringList(SYSTEMDEFENCE_CONFIG_LIST_FILE, APPTRANSITIONTIMEOUT_PACKAGES);
        this.mAppTransitionTimeoutStringList = stringList2;
        this.mConfigurationManager.registerObserver(stringList2, this.mAppTransitionTimeoutObserver);
        StringList stringList3 = this.mConfigurationManager.getStringList(SYSTEMDEFENCE_CONFIG_LIST_FILE, SKIP_REGISTEROBSERVER_PACKAGES);
        this.mSkipRegisterObserverStringList = stringList3;
        this.mConfigurationManager.registerObserver(stringList3, this.mSkipReigsterObserverObserver);
        StringList stringList4 = this.mConfigurationManager.getStringList(SYSTEMDEFENCE_CONFIG_LIST_FILE, SKIP_RUNNING_NOTIFICATION_PACKAGES);
        this.mSkipRunningNotificationPackagesStringList = stringList4;
        this.mConfigurationManager.registerObserver(stringList4, this.mSkipRunningNotificationPackagesObserver);
        StringList stringList5 = this.mConfigurationManager.getStringList(SYSTEMDEFENCE_CONFIG_LIST_FILE, DELAY_UPDATE_PACKAGES);
        this.mDelayUpdatePackagesStringList = stringList5;
        this.mConfigurationManager.registerObserver(stringList5, this.mDelayUpdatePackagesObserver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSystemDefenceStringList() {
        updateDumpTracePackages();
        updateAppTransitionTimeoutPackages();
        updateSkipRegisterObserverPackages();
        updateSkipRunningNotificationPackages();
        updateDelayUpdatePackages();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSwitchConfig() {
        try {
            if (this.mSwitchValuesList != null) {
                VSlog.i(TAG, "updateSwitchConfig...");
                this.mSmoothEnable = this.mSwitchValuesList.getValue(KEY_SMOOTH_ENABLE) != null ? Integer.parseInt(this.mSwitchValuesList.getValue(KEY_SMOOTH_ENABLE)) : 1;
                this.mStartBgServcieSwitch = this.mSwitchValuesList.getValue(SWITCH_START_BG_SERVICE) != null ? Boolean.parseBoolean(this.mSwitchValuesList.getValue(SWITCH_START_BG_SERVICE)) : true;
                this.mApptransitionTimeoutSwitch = this.mSwitchValuesList.getValue(SWITCH_APPTRANSITION_TIMEOUT) != null ? Boolean.parseBoolean(this.mSwitchValuesList.getValue(SWITCH_APPTRANSITION_TIMEOUT)) : true;
                this.mDumpStacktraceSwitch = this.mSwitchValuesList.getValue(SWITCH_DUMP_STACKTRACE) != null ? Boolean.parseBoolean(this.mSwitchValuesList.getValue(SWITCH_DUMP_STACKTRACE)) : true;
                this.mSkipRegisterObserverSwitch = this.mSwitchValuesList.getValue(SWITCH_SKIP_REGISTER_OBSERVER) != null ? Boolean.parseBoolean(this.mSwitchValuesList.getValue(SWITCH_SKIP_REGISTER_OBSERVER)) : true;
                this.mApptransitionTimeoutMs = this.mSwitchValuesList.getValue(KEY_APPTRANSITION_TIMEOUT) != null ? Long.parseLong(this.mSwitchValuesList.getValue(KEY_APPTRANSITION_TIMEOUT)) : APP_TRANSITION_TIMEOUT_MS;
                this.mSkipKilledByRemoveTaskSwitch = this.mSwitchValuesList.getValue(SWITCH_SKIP_KILLED_BY_REMOVETASK) != null ? Boolean.parseBoolean(this.mSwitchValuesList.getValue(SWITCH_SKIP_KILLED_BY_REMOVETASK)) : true;
                this.mSkipRuningNotificationSwitch = this.mSwitchValuesList.getValue(SWITCH_SKIP_RUNNING_NOTIFICATION) != null ? Boolean.parseBoolean(this.mSwitchValuesList.getValue(SWITCH_SKIP_RUNNING_NOTIFICATION)) : true;
                this.mFgServiceCrashSwitch = this.mSwitchValuesList.getValue(SWITCH_FG_SERVICE_CRASH) != null ? Boolean.parseBoolean(this.mSwitchValuesList.getValue(SWITCH_FG_SERVICE_CRASH)) : true;
                this.mFgServiceAnrSwitch = this.mSwitchValuesList.getValue(SWITCH_FG_SERVICE_ANR) != null ? Boolean.parseBoolean(this.mSwitchValuesList.getValue(SWITCH_FG_SERVICE_ANR)) : true;
            }
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "mStartBgServcieSwitch:" + this.mStartBgServcieSwitch + "  mApptransitionTimeoutSwitch:" + this.mApptransitionTimeoutSwitch + "  mDumpStacktraceSwitch:" + this.mDumpStacktraceSwitch + "  mSkipRegisterObserverSwitch:" + this.mSkipRegisterObserverSwitch + "  mSmoothEnable:" + this.mSmoothEnable + "  mApptransitionTimeoutMs:" + this.mApptransitionTimeoutMs + "  mSkipKilledByRemoveTaskSwitch:" + this.mSkipKilledByRemoveTaskSwitch + "  mSkipRuningNotificationSwitch:" + this.mSkipRuningNotificationSwitch + "  mFgServiceCrashSwitch:" + this.mFgServiceCrashSwitch + "  mFgServiceAnrSwitch:" + this.mFgServiceAnrSwitch);
            }
            updateMTKPlatformProperty();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDumpTracePackages() {
        try {
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "updateDumpTracePackages..");
            }
            if (this.mDumpTracePackagesStringList != null) {
                this.mDumpTracePackagesList = this.mDumpTracePackagesStringList.getValues().size() > 0 ? this.mDumpTracePackagesStringList.getValues() : this.mDumpTracePackagesList;
            }
            for (String dumpTracePackage : this.mDumpTracePackagesList) {
                if (this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(TAG, "dumpTracePackage= " + dumpTracePackage);
                }
            }
            this.mDumpStacktraceInterests = (String[]) this.mDumpTracePackagesList.toArray(new String[this.mDumpTracePackagesList.size()]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAppTransitionTimeoutPackages() {
        try {
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "updateAppTransitionTimeoutPackages..");
            }
            if (this.mAppTransitionTimeoutStringList != null) {
                this.mAppTransitionTimeoutList = this.mAppTransitionTimeoutStringList.getValues().size() > 0 ? this.mAppTransitionTimeoutStringList.getValues() : this.mAppTransitionTimeoutList;
            }
            for (String timeoutPackage : this.mAppTransitionTimeoutList) {
                if (this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(TAG, "timeoutPackage= " + timeoutPackage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSkipRegisterObserverPackages() {
        try {
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "updateSkipRegisterObserverPackages..");
            }
            if (this.mSkipRegisterObserverStringList != null) {
                this.mSkipRegisterObserverList = this.mSkipRegisterObserverStringList.getValues().size() > 0 ? this.mSkipRegisterObserverStringList.getValues() : this.mSkipRegisterObserverList;
            }
            for (String skipRegisterObserverPackage : this.mSkipRegisterObserverList) {
                if (this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(TAG, "skipRegisterObserverPackage= " + skipRegisterObserverPackage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSkipRunningNotificationPackages() {
        try {
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "updateSkipRunningNotificationPackages..");
            }
            if (this.mSkipRunningNotificationPackagesStringList != null) {
                this.mSkipRunningNotificationList = this.mSkipRunningNotificationPackagesStringList.getValues().size() > 0 ? this.mSkipRunningNotificationPackagesStringList.getValues() : this.mSkipRunningNotificationList;
            }
            for (String skipRunningNotificationPackage : this.mSkipRunningNotificationList) {
                if (this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(TAG, "skipRunningNotificationPackage= " + skipRunningNotificationPackage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDelayUpdatePackages() {
        try {
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "updateDelayUpdatePackages..");
            }
            if (this.mDelayUpdatePackagesStringList != null) {
                this.mDelayUpdateList = this.mDelayUpdatePackagesStringList.getValues().size() > 0 ? this.mDelayUpdatePackagesStringList.getValues() : this.mDelayUpdateList;
            }
            for (String delayUpdatePackage : this.mDelayUpdateList) {
                if (this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(TAG, "delayUpdatePackage= " + delayUpdatePackage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMTKPlatformProperty() {
        if ("PD1901F_EX".equals(SystemProperties.get("ro.vivo.product.model"))) {
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "mSmoothEnable:" + this.mSmoothEnable);
            }
            SystemProperties.set("persist.vivo.smoothly", Integer.toString(this.mSmoothEnable));
        }
    }

    public void dumpStacktraceToDebug() {
        if (!this.mDumpStacktraceSwitch) {
            VSlog.i(TAG, "dumpStacktrace switch is off,just return.");
            return;
        }
        SystemDefenceHandler systemDefenceHandler = this.mHandler;
        if (systemDefenceHandler != null) {
            systemDefenceHandler.sendEmptyMessage(100);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpStackTraceOfInterrest() {
        VSlog.i(TAG, "do dumpStackTraceOfInterrest.");
        int[] pids = Process.getPidsForCommands(this.mDumpStacktraceInterests);
        if (pids != null) {
            for (int i : pids) {
                VSlog.i(TAG, "dumpStackTrace pid = " + i);
                Process.sendSignal(i, 3);
            }
        }
    }

    public boolean checkStartBgServiceErrorDefence(ApplicationInfo appInfo) {
        if (!this.mStartBgServcieSwitch) {
            VSlog.i(TAG, "start bg service switch is off,just return.");
            return false;
        } else if (appInfo == null || appInfo == null || appInfo.targetSdkVersion >= 30 || appInfo.isSystemApp()) {
            return false;
        } else {
            VSlog.d(TAG, "skip start bg service error,caller packageName:" + appInfo.packageName);
            ContentValues cv = new ContentValues();
            cv.put("level", (Integer) 5);
            cv.put("subtype", (Integer) 1);
            cv.put("expose", appInfo.packageName);
            cv.put("trouble", (Integer) 0);
            recordEventToEpm(42, cv);
            return true;
        }
    }

    public boolean checkFgServiceCrashDefence(ApplicationInfo appInfo) {
        if (!this.mFgServiceCrashSwitch) {
            VSlog.i(TAG, "fg service crash switch is off,just return.");
            return false;
        } else if (appInfo == null || appInfo == null || appInfo.targetSdkVersion >= 30 || appInfo.isSystemApp()) {
            return false;
        } else {
            VSlog.d(TAG, "skip fg service crash,caller packageName:" + appInfo.packageName);
            ContentValues cv = new ContentValues();
            cv.put("level", (Integer) 5);
            cv.put("subtype", (Integer) 2);
            cv.put("expose", appInfo.packageName);
            cv.put("trouble", (Integer) 0);
            recordEventToEpm(42, cv);
            return true;
        }
    }

    public boolean checkFgServiceAnrDefence(ApplicationInfo appInfo) {
        if (!this.mFgServiceCrashSwitch) {
            VSlog.i(TAG, "fg service anr switch is off,just return.");
            return false;
        } else if (appInfo == null || appInfo == null || appInfo.targetSdkVersion >= 30 || appInfo.isSystemApp()) {
            return false;
        } else {
            VSlog.d(TAG, "skip fg service anr,caller packageName:" + appInfo.packageName);
            ContentValues cv = new ContentValues();
            cv.put("level", (Integer) 5);
            cv.put("subtype", (Integer) 3);
            cv.put("expose", appInfo.packageName);
            cv.put("trouble", (Integer) 0);
            recordEventToEpm(42, cv);
            return true;
        }
    }

    public long checkTransitionTimoutErrorDefence(String packageName) {
        if (!this.mApptransitionTimeoutSwitch) {
            VSlog.i(TAG, "apptransition timeout switch is off,just return.");
            return -1L;
        } else if (packageName == null || !this.mAppTransitionTimeoutList.contains(packageName)) {
            return -1L;
        } else {
            return this.mApptransitionTimeoutMs;
        }
    }

    public boolean checkProviderExist(Uri uri) {
        if (!this.mSkipRegisterObserverSwitch) {
            VSlog.i(TAG, "checkProviderExist switch is off,just return.");
            return true;
        }
        if (uri != null && this.mSkipRegisterObserverList.contains(uri.getAuthority())) {
            ProviderInfo cpi = null;
            try {
                cpi = AppGlobals.getPackageManager().resolveContentProvider(uri.getAuthority(), 790016, UserHandle.getUserId(Binder.getCallingUid()));
            } catch (RemoteException e) {
            }
            if (cpi == null) {
                VSlog.i(TAG, "skip registerContentObserver because the providerInfo is not exsit. Uri= " + uri);
                return false;
            }
        }
        return true;
    }

    public boolean checkSkipKilledByRemoveTask(String reason, String processName) {
        if (!this.mSkipKilledByRemoveTaskSwitch) {
            VSlog.i(TAG, "checkSkipKilledByRemoveTask switch is off,just return.");
            return false;
        } else if ("remove task".equals(reason) && "com.tencent.mm:push".equals(processName)) {
            VSlog.i(TAG, "skip interesting process to kill when remove task.");
            return true;
        } else {
            return false;
        }
    }

    public boolean checkSmallIconNULLPackage(String packageName) {
        if (!this.mSkipRuningNotificationSwitch) {
            VSlog.i(TAG, "checkSmallIconNULLPackage switch is off,just return.");
            return false;
        } else if (this.mSkipRunningNotificationList.contains(packageName)) {
            VSlog.i(TAG, "skip show running notification for " + packageName);
            return true;
        } else {
            return false;
        }
    }

    private void recordEventToEpm(int exeType, ContentValues cv) {
        try {
            VSlog.i(TAG, "recordEventToEpm=" + cv.toString());
            ExceptionPolicyManager.getInstance().recordEvent(exeType, System.currentTimeMillis(), cv);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean checkDelayUpdate(String packageName) {
        if (SystemDefenceHelper.getInstance().checkDelayUpdate(this.mResumePackageName, packageName)) {
            VSlog.i(TAG, "checkDelayUpdate=" + packageName);
            ContentValues cv = new ContentValues();
            cv.put("level", (Integer) 5);
            cv.put("subtype", (Integer) 4);
            cv.put("expose", packageName);
            cv.put("trouble", (Integer) 0);
            recordEventToEpm(42, cv);
            return true;
        }
        return false;
    }

    public void checkUploadStabilityData(String processName, String reason) {
        String killReason;
        if (reason != null) {
            try {
                if (reason.contains("by")) {
                    killReason = reason.substring(reason.indexOf("by"), reason.length());
                } else {
                    killReason = reason;
                }
                if (!this.mSkipUploadedReason.contains(killReason) && !killReason.contains("deletePackage") && !killReason.contains("REQUEST_INSTALL_PACKAGE")) {
                    VSlog.i(TAG, "checkUploadStabilityData processName=" + processName + ",reason=" + reason);
                    ContentValues cv = new ContentValues();
                    cv.put("level", (Integer) 5);
                    cv.put("subtype", (Integer) 7);
                    cv.put("expose", processName);
                    cv.put("reason", killReason);
                    cv.put("trouble", (Integer) 0);
                    recordEventToEpm(42, cv);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkReinstallPacakge(String packageName) {
        if (this.mDelayUpdateList.contains(packageName)) {
            return true;
        }
        return false;
    }

    public void uploadSelfKillData(int pid) {
        ApplicationInfo applicationInfo;
        if (this.mResumedPid != pid || (applicationInfo = this.mResumedAppInfo) == null || applicationInfo.isSystemApp()) {
            return;
        }
        String str = this.mCrashPkgName;
        if (str == null || this.mResumePackageName != str) {
            VSlog.i(TAG, "uploadSelfKillData pid=" + pid + ",pkgName=" + this.mResumePackageName);
            ContentValues cv = new ContentValues();
            cv.put("level", (Integer) 5);
            cv.put("subtype", (Integer) 8);
            cv.put("expose", this.mResumePackageName);
            cv.put("trouble", (Integer) 0);
            recordEventToEpm(42, cv);
        }
    }

    public void onSetActivityResumed(String packageName, String recordInfo, int pid, ApplicationInfo appInfo) {
        VSlog.i(TAG, "onSetActivityResumed=" + packageName + "  recordInfo=" + recordInfo + "  pid=" + pid);
        this.mResumePackageName = packageName;
        this.mResumedPid = pid;
        this.mResumedAppInfo = appInfo;
        this.mCrashPkgName = null;
        if (Constant.APP_WEIXIN.equals(packageName) && recordInfo != null && recordInfo.contains("DBRecoveryUI")) {
            VSlog.i(TAG, "Wechat EnMicroMsg.db corrupt,notify user modify.");
            execRunShellWithResult(buildCmdString("Wechat", "EnMicroMsg.db"));
            ContentValues cv = new ContentValues();
            cv.put("level", (Integer) 5);
            cv.put("subtype", (Integer) 6);
            cv.put("expose", packageName);
            cv.put("trouble", (Integer) 0);
            recordEventToEpm(42, cv);
        } else if ("com.tencent.mobileqq".equals(packageName) && recordInfo != null && recordInfo.contains("DBFixDialogActivity")) {
            VSlog.i(TAG, "QQ QQ.db corrupt,notify user modify.");
            execRunShellWithResult(buildCmdString("QQ", "QQ.db"));
            ContentValues cv2 = new ContentValues();
            cv2.put("level", (Integer) 5);
            cv2.put("subtype", (Integer) 5);
            cv2.put("expose", packageName);
            cv2.put("trouble", (Integer) 0);
            recordEventToEpm(42, cv2);
        }
    }

    public void reportFgCrashData(String pkgName, String longMsg, String stackTrace) {
        VSlog.i(TAG, "pkgName=" + pkgName + ",longMsg=" + longMsg);
        this.mCrashPkgName = pkgName;
        if (longMsg != null && longMsg.contains("Native crash") && stackTrace != null) {
            String reason = null;
            if (stackTrace.contains(CRASH_REASON_GPU)) {
                reason = CRASH_REASON_GPU;
            } else if (stackTrace.contains(CRASH_REASON_WEBVIEW)) {
                reason = CRASH_REASON_WEBVIEW;
            }
            if (reason != null) {
                ContentValues cv = new ContentValues();
                cv.put("level", (Integer) 5);
                cv.put("subtype", (Integer) 9);
                cv.put("expose", pkgName);
                cv.put("reason", reason);
                cv.put("trouble", (Integer) 0);
                recordEventToEpm(42, cv);
            }
        }
    }

    private String buildCmdString(String moduleName, String dbName) {
        String timestamps = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(Long.valueOf(System.currentTimeMillis()));
        String ret = "aTBNVJgAq/pMbJhXJigdWAkfKWG3B76Ae2K8KCexm1gFlDcMId46GfaKQQZgwyzQhnlyh9QWYBEM+EpjWhJLx4fffx0FSrgrR9tTUNSUuVG9CIW2hDxMAqZBYjPI+0BsaenT1AIbuGaCyRb4MjKKvd3mpk4aYJQNwKQ1t/UEf/Lf821XvVHf7seGsTxf1uoxHw6d7Wlxeto9FbFxhXXgCVpW4sE0mUekyhD46AgEqP24R9tzHRISAboZehi7Pz3SlhgWhcOrJ/sxRfhVpYY3Bhyho0dQ5+6kXC5rT8/W9qMW14yD4tEK44dpZyMw+0KoVtGNyeLFiYBZ+ShYIIWAqA==?" + moduleName + "?" + dbName + "?" + timestamps;
        return ret;
    }

    private String execRunShellWithResult(String cmd) {
        String result = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        try {
            Class<?> ServiceManager = Class.forName("android.os.ServiceManager");
            Method getService = ServiceManager.getMethod("getService", String.class);
            Object service = getService.invoke(ServiceManager, "vivo_daemon.service");
            Class<?> VivoDmServiceProxy = Class.forName("com.vivo.services.daemon.VivoDmServiceProxy");
            Method asInterface = VivoDmServiceProxy.getMethod("asInterface", IBinder.class);
            Object dmService = asInterface.invoke(VivoDmServiceProxy, service);
            Method runShell = VivoDmServiceProxy.getMethod("runShellWithResult", String.class);
            result = (String) runShell.invoke(dmService, cmd);
            VSlog.d(TAG, "execRunShellWithResult result: " + result);
            return result;
        } catch (Exception e) {
            VSlog.e(TAG, "execRunShellWithResult get vivo_daemon.service failed: " + e);
            return result;
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("SystemDefence Switch:");
            pw.println("mStartBgServcieSwitch:" + this.mStartBgServcieSwitch);
            pw.println("mApptransitionTimeoutSwitch:" + this.mApptransitionTimeoutSwitch);
            pw.println("mDumpStacktraceSwitch:" + this.mDumpStacktraceSwitch);
            pw.println("mSkipRegisterObserverSwitch:" + this.mSkipRegisterObserverSwitch);
            pw.println("mSmoothEnable:" + this.mSmoothEnable);
            pw.println("mApptransitionTimeoutMs:" + this.mApptransitionTimeoutMs);
            pw.println("mSkipKilledByRemoveTaskSwitch:" + this.mSkipKilledByRemoveTaskSwitch);
            pw.println("mSkipRuningNotificationSwitch:" + this.mSkipRuningNotificationSwitch);
            pw.println("mFgServiceCrashSwitch:" + this.mFgServiceCrashSwitch);
            pw.println("mFgServiceAnrSwitch:" + this.mFgServiceAnrSwitch);
            pw.println(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            pw.println("SystemDefence StringList:");
            pw.println("mDumpTracePackagesStringList:" + this.mDumpTracePackagesStringList + "  " + this.mDumpTracePackagesList);
            pw.println("mAppTransitionTimeoutStringList:" + this.mAppTransitionTimeoutStringList + "  " + this.mAppTransitionTimeoutList);
            pw.println("mSkipRegisterObserverStringList:" + this.mSkipRegisterObserverStringList + "  " + this.mSkipRegisterObserverList);
            pw.println("mDelayUpdatePackagesStringList:" + this.mDelayUpdatePackagesStringList + "  " + this.mDelayUpdateList);
        }
    }
}