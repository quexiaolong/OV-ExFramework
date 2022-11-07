package com.android.server.am;

import android.os.Build;
import android.os.SystemProperties;
import com.android.server.UnifiedConfigThread;
import com.android.server.am.firewall.VivoAppIsolationController;
import com.android.server.notification.VivoNotificationManagerServiceImpl;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.services.superresolution.Constant;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.ContentValuesList;
import vivo.app.configuration.StringList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class AmsConfigManager {
    private static final String ACTIVITYOPT = "ActivityOpt";
    private static final String ACTIVITY_NUM_CONTROL = "activity_num_control";
    private static final String AMS_CONFIG_FILE = "/data/bbkcore/AmsConfigManager_amsconfig_1.0.xml";
    private static final String AMS_FEATURE_FILE = "/data/bbkcore/AmsConfigManager_amsfeature_1.0.xml";
    private static final String ANR_MONITOR_PACKAGE = "anr_monitor_package";
    private static final String BGSTART_ALLOWED_ACTIVITY = "bgstart_allowed_activity";
    private static final String BGSTART_ALLOWED_SERVICE = "bgstart_allowed_service";
    private static final String BROADCASTOPT = "BroadcastOpt";
    private static final String FORCESTOPOPT = "ForceStopOpt";
    private static final String FORCE_STOP_APP = "force_stop_app";
    private static final String IS_INCONTROL = "is_incontrol";
    private static final String MAXACTIVITIES = "maxActivitiesInTask";
    private static final String MAXSAMEACTIVITIES = "maxSameActivitiesInTask";
    private static final String PROCESSOPT = "ProcessOpt";
    private static final String PROVIDEROPT = "ProviderOpt";
    private static final String SERVICEOPT = "ServiceOpt";
    private static final String SIZECOMPATMODEOPT = "SizeCompatModeOpt";
    private static final String TAG = "AmsConfigManager";
    private static final String VIVO_BGSTART = "vivo_bgstart";
    private static AmsConfigManager sInstance = null;
    private final List<String> BGSTART_ALLOWED_APP;
    private final boolean IS_LOG_CTRL_OPEN;
    private ContentValuesList mActivityControlList;
    private ConfigurationObserver mActivityControlObserver;
    private boolean mActivityOpt;
    private List<String> mAnrDumpTraceNativeProcesses;
    private List<String> mAnrDumpTracePersistentProcesses;
    private List<String> mAnrMonitorPackageList;
    private ConfigurationObserver mAnrMonitorPackageObserver;
    private StringList mAnrMonitorPackageStringList;
    private List<String> mBgstartAllowedActivityList;
    private ConfigurationObserver mBgstartAllowedActivityObserver;
    private StringList mBgstartAllowedActivityStringList;
    private List<String> mBgstartAllowedServiceList;
    private ConfigurationObserver mBgstartAllowedServiceObserver;
    private StringList mBgstartAllowedServiceStringList;
    private boolean mBroadcastOpt;
    private AbsConfigurationManager mConfigurationManager;
    private ArrayList<String> mExcessiveCpuFilterList;
    private List<String> mForceStopAppList;
    private ConfigurationObserver mForceStopAppObserver;
    private StringList mForceStopAppStringList;
    private boolean mForceStopOpt;
    private boolean mIs_Incontrol;
    private int mMaxActivitiesInTask;
    private int mMaxSameActivitiesInTask;
    private boolean mProcessOpt;
    private boolean mProviderOpt;
    private boolean mServiceOpt;
    private boolean mSizeCompatModeOpt;
    private boolean mVivoBgstartAllowed;
    private VivoFrameworkFactory mVivoFrameworkFactory;

    public ArrayList<String> getExcessiveCpuFilterList() {
        return this.mExcessiveCpuFilterList;
    }

    private AmsConfigManager() {
        this.IS_LOG_CTRL_OPEN = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes") || Build.TYPE.equals("eng");
        this.BGSTART_ALLOWED_APP = new ArrayList<String>() { // from class: com.android.server.am.AmsConfigManager.1
            {
                add(VivoAppIsolationController.NOTIFY_IQOO_SECURE_PACKAGE);
                add("com.vivo.safecenter");
                add("com.bbk.cloud");
                add("com.bbk.updater");
                add("com.vivo.browser");
                add("com.mobile.cos.iroaming");
                add("com.eg.android.AlipayGphone");
                add("com.vivo.share");
                add("com.google.android.gms");
                add("com.bbk.theme");
                add("com.unionpay.tsmservice");
                add("com.vivo.customtool");
                add("com.vivo.imanager");
                add("com.vivo.weather");
                add("com.bbk.appstore");
                add("com.vivo.cota");
                add("com.vivo.widget.cleanspeed");
                add("com.vivo.video.floating");
                add("com.vivo.gamecube");
                add("com.vivo.SmartKey");
            }
        };
        this.mBgstartAllowedServiceList = new ArrayList<String>() { // from class: com.android.server.am.AmsConfigManager.2
        };
        this.mForceStopAppList = new ArrayList<String>() { // from class: com.android.server.am.AmsConfigManager.3
            {
                add("com.android.bbkmusic");
                add(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS);
                add("com.bbk.theme");
                add("com.vivo.appstore");
                add("com.bbk.appstore");
                add("com.android.providers.downloads");
                add("com.android.bbksoundrecorder");
                add("com.vivo.smartshot");
                add("com.vivo.browser");
                add("com.vivo.email");
                add("com.vivo.FMRadio");
                add("com.vivo.appfilter");
                add("com.iqoo.trafficupgrade");
            }
        };
        this.mAnrMonitorPackageList = new ArrayList<String>() { // from class: com.android.server.am.AmsConfigManager.4
            {
                add(FaceUIState.PKG_SYSTEMUI);
                add("com.bbk.launcher2");
                add(VivoNotificationManagerServiceImpl.PKG_LAUNCHER);
                add("com.vivo.upslide");
                add(Constant.APP_GALLERY);
                add("com.android.bbkmusic");
                add("com.vivo.daemonService");
                add("com.android.camera");
                add("com.vivo.browser");
                add("com.vivo.car.networking");
                add("com.vivo.carlauncher");
            }
        };
        this.mAnrDumpTracePersistentProcesses = new ArrayList<String>() { // from class: com.android.server.am.AmsConfigManager.5
            {
                add("com.android.phone");
            }
        };
        this.mAnrDumpTraceNativeProcesses = new ArrayList<String>() { // from class: com.android.server.am.AmsConfigManager.6
            {
                add("/system/bin/audioserver");
                add("/system/bin/cameraserver");
                add("/system/bin/mediaserver");
                add("/system/bin/surfaceflinger");
            }
        };
        this.mExcessiveCpuFilterList = new ArrayList<String>() { // from class: com.android.server.am.AmsConfigManager.7
            {
                add("com.vivo.bsptest");
                add("com.netease.my.vivo");
                add("com.netease.l10.vivo");
                add("com.netease.tx.vivo");
            }
        };
        this.mIs_Incontrol = true;
        this.mMaxActivitiesInTask = 60;
        this.mMaxSameActivitiesInTask = 20;
        this.mVivoBgstartAllowed = false;
        this.mBgstartAllowedActivityList = new ArrayList<String>() { // from class: com.android.server.am.AmsConfigManager.8
        };
        this.mActivityOpt = true;
        this.mServiceOpt = true;
        this.mBroadcastOpt = true;
        this.mProviderOpt = true;
        this.mProcessOpt = true;
        this.mSizeCompatModeOpt = true;
        this.mForceStopOpt = true;
        this.mBgstartAllowedActivityObserver = new ConfigurationObserver() { // from class: com.android.server.am.AmsConfigManager.10
            public void onConfigChange(String file, String name) {
                if (AmsConfigManager.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(AmsConfigManager.TAG, "onConfigChange file:" + file + ",name = " + name);
                }
                if (AmsConfigManager.this.mConfigurationManager == null) {
                    if (AmsConfigManager.this.IS_LOG_CTRL_OPEN) {
                        VSlog.e(AmsConfigManager.TAG, "mConfigurationManager is null");
                        return;
                    }
                    return;
                }
                UnifiedConfigThread.getHandler().post(new Runnable() { // from class: com.android.server.am.AmsConfigManager.10.1
                    @Override // java.lang.Runnable
                    public void run() {
                        AmsConfigManager.this.mBgstartAllowedActivityStringList = AmsConfigManager.this.mConfigurationManager.getStringList(AmsConfigManager.AMS_CONFIG_FILE, AmsConfigManager.BGSTART_ALLOWED_ACTIVITY);
                        AmsConfigManager.this.updateBgstartAllowedActivityList();
                    }
                });
            }
        };
        this.mBgstartAllowedServiceObserver = new ConfigurationObserver() { // from class: com.android.server.am.AmsConfigManager.11
            public void onConfigChange(String file, String name) {
                if (AmsConfigManager.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(AmsConfigManager.TAG, "onConfigChange file:" + file + ",name = " + name);
                }
                if (AmsConfigManager.this.mConfigurationManager == null) {
                    if (AmsConfigManager.this.IS_LOG_CTRL_OPEN) {
                        VSlog.e(AmsConfigManager.TAG, "mConfigurationManager is null");
                        return;
                    }
                    return;
                }
                UnifiedConfigThread.getHandler().post(new Runnable() { // from class: com.android.server.am.AmsConfigManager.11.1
                    @Override // java.lang.Runnable
                    public void run() {
                        AmsConfigManager.this.mBgstartAllowedServiceStringList = AmsConfigManager.this.mConfigurationManager.getStringList(AmsConfigManager.AMS_CONFIG_FILE, AmsConfigManager.BGSTART_ALLOWED_SERVICE);
                        AmsConfigManager.this.updateBgstartAllowedServiceList();
                    }
                });
            }
        };
        this.mForceStopAppObserver = new ConfigurationObserver() { // from class: com.android.server.am.AmsConfigManager.12
            public void onConfigChange(String file, String name) {
                if (AmsConfigManager.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(AmsConfigManager.TAG, "onConfigChange file:" + file + ",name =" + name);
                }
                if (AmsConfigManager.this.mConfigurationManager == null) {
                    VSlog.e(AmsConfigManager.TAG, "mConfigurationManager is null");
                } else {
                    UnifiedConfigThread.getHandler().post(new Runnable() { // from class: com.android.server.am.AmsConfigManager.12.1
                        @Override // java.lang.Runnable
                        public void run() {
                            AmsConfigManager.this.mForceStopAppStringList = AmsConfigManager.this.mConfigurationManager.getStringList(AmsConfigManager.AMS_CONFIG_FILE, AmsConfigManager.FORCE_STOP_APP);
                            AmsConfigManager.this.updateForceStopAppList();
                        }
                    });
                }
            }
        };
        this.mAnrMonitorPackageObserver = new ConfigurationObserver() { // from class: com.android.server.am.AmsConfigManager.13
            public void onConfigChange(String file, String name) {
                if (AmsConfigManager.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(AmsConfigManager.TAG, "onConfigChange file:" + file + ",name=" + name);
                }
                if (AmsConfigManager.this.mConfigurationManager == null) {
                    if (AmsConfigManager.this.IS_LOG_CTRL_OPEN) {
                        VSlog.e(AmsConfigManager.TAG, "mConfigurationManager is null");
                        return;
                    }
                    return;
                }
                UnifiedConfigThread.getHandler().post(new Runnable() { // from class: com.android.server.am.AmsConfigManager.13.1
                    @Override // java.lang.Runnable
                    public void run() {
                        AmsConfigManager.this.mAnrMonitorPackageStringList = AmsConfigManager.this.mConfigurationManager.getStringList(AmsConfigManager.AMS_CONFIG_FILE, AmsConfigManager.ANR_MONITOR_PACKAGE);
                        AmsConfigManager.this.updateAnrMonitorPackageList();
                    }
                });
            }
        };
        this.mActivityControlObserver = new ConfigurationObserver() { // from class: com.android.server.am.AmsConfigManager.14
            public void onConfigChange(String file, String name) {
                if (AmsConfigManager.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(AmsConfigManager.TAG, "onConfigChange file:" + file + ",name=" + name);
                }
                if (AmsConfigManager.this.mConfigurationManager == null) {
                    if (AmsConfigManager.this.IS_LOG_CTRL_OPEN) {
                        VSlog.i(AmsConfigManager.TAG, "mConfigurationManager is null");
                        return;
                    }
                    return;
                }
                UnifiedConfigThread.getHandler().post(new Runnable() { // from class: com.android.server.am.AmsConfigManager.14.1
                    @Override // java.lang.Runnable
                    public void run() {
                        AmsConfigManager.this.mActivityControlList = AmsConfigManager.this.mConfigurationManager.getContentValuesList(AmsConfigManager.AMS_FEATURE_FILE, AmsConfigManager.ACTIVITY_NUM_CONTROL);
                        AmsConfigManager.this.updateActivityControlConfig();
                    }
                });
            }
        };
        this.mVivoFrameworkFactory = VivoFrameworkFactory.getFrameworkFactoryImpl();
        UnifiedConfigThread.getHandler().post(new Runnable() { // from class: com.android.server.am.AmsConfigManager.9
            @Override // java.lang.Runnable
            public void run() {
                AmsConfigManager.this.init();
                AmsConfigManager.this.update();
            }
        });
    }

    public static synchronized AmsConfigManager getInstance() {
        AmsConfigManager amsConfigManager;
        synchronized (AmsConfigManager.class) {
            if (sInstance == null) {
                sInstance = new AmsConfigManager();
            }
            amsConfigManager = sInstance;
        }
        return amsConfigManager;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void init() {
        VivoFrameworkFactory vivoFrameworkFactory = this.mVivoFrameworkFactory;
        if (vivoFrameworkFactory != null) {
            this.mConfigurationManager = vivoFrameworkFactory.getConfigurationManager();
        }
        AbsConfigurationManager absConfigurationManager = this.mConfigurationManager;
        if (absConfigurationManager != null) {
            this.mBgstartAllowedServiceStringList = absConfigurationManager.getStringList(AMS_CONFIG_FILE, BGSTART_ALLOWED_SERVICE);
            this.mForceStopAppStringList = this.mConfigurationManager.getStringList(AMS_CONFIG_FILE, FORCE_STOP_APP);
            this.mAnrMonitorPackageStringList = this.mConfigurationManager.getStringList(AMS_CONFIG_FILE, ANR_MONITOR_PACKAGE);
            this.mBgstartAllowedActivityStringList = this.mConfigurationManager.getStringList(AMS_CONFIG_FILE, BGSTART_ALLOWED_ACTIVITY);
            VSlog.i(TAG, "init mBgstartAllowedServiceStringList:" + this.mBgstartAllowedServiceStringList + ",mForceStopAppStringList = " + this.mForceStopAppStringList + ", mAnrMonitorPackageStringList=" + this.mAnrMonitorPackageStringList + ", mBgstartAllowedActivityStringList=" + this.mBgstartAllowedActivityStringList);
            this.mConfigurationManager.registerObserver(this.mBgstartAllowedServiceStringList, this.mBgstartAllowedServiceObserver);
            this.mConfigurationManager.registerObserver(this.mForceStopAppStringList, this.mForceStopAppObserver);
            this.mConfigurationManager.registerObserver(this.mAnrMonitorPackageStringList, this.mAnrMonitorPackageObserver);
            this.mConfigurationManager.registerObserver(this.mBgstartAllowedActivityStringList, this.mBgstartAllowedActivityObserver);
            ContentValuesList contentValuesList = this.mConfigurationManager.getContentValuesList(AMS_FEATURE_FILE, ACTIVITY_NUM_CONTROL);
            this.mActivityControlList = contentValuesList;
            this.mConfigurationManager.registerObserver(contentValuesList, this.mActivityControlObserver);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void update() {
        updateBgstartAllowedServiceList();
        updateForceStopAppList();
        updateAnrMonitorPackageList();
        updateActivityControlConfig();
        updateBgstartAllowedActivityList();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBgstartAllowedActivityList() {
        try {
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "updateBgstartAllowedActivityList.. mBgstartAllowedActivityStringList=" + this.mBgstartAllowedActivityStringList);
            }
            if (this.mBgstartAllowedActivityStringList != null) {
                VSlog.i(TAG, "mBgstartAllowedServiceStringList size=" + this.mBgstartAllowedActivityStringList.getValues().size());
                this.mBgstartAllowedActivityList = this.mBgstartAllowedActivityStringList.getValues().size() > 0 ? this.mBgstartAllowedActivityStringList.getValues() : this.mBgstartAllowedActivityList;
            }
            for (String bgStartAllowedActivity : this.mBgstartAllowedActivityList) {
                if (this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(TAG, "bgStartAllowedActivity = " + bgStartAllowedActivity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBgstartAllowedServiceList() {
        try {
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "updateBgstartAllowedServiceList.. mBgstartAllowedServiceStringList=" + this.mBgstartAllowedServiceStringList);
            }
            if (this.mBgstartAllowedServiceStringList != null) {
                VSlog.i(TAG, "mBgstartAllowedServiceStringList size=" + this.mBgstartAllowedServiceStringList.getValues().size());
                this.mBgstartAllowedServiceList = this.mBgstartAllowedServiceStringList.getValues().size() > 0 ? this.mBgstartAllowedServiceStringList.getValues() : this.mBgstartAllowedServiceList;
            }
            for (String bgStartAllowedService : this.mBgstartAllowedServiceList) {
                if (this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(TAG, "bgStartAllowedService = " + bgStartAllowedService);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForceStopAppList() {
        try {
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "updateForceStopAppList.. mForceStopAppStringList=" + this.mForceStopAppStringList);
            }
            if (this.mForceStopAppStringList != null) {
                VSlog.i(TAG, "mForceStopAppStringList size=" + this.mForceStopAppStringList.getValues().size());
                this.mForceStopAppList = this.mForceStopAppStringList.getValues().size() > 0 ? this.mForceStopAppStringList.getValues() : this.mForceStopAppList;
            }
            for (String forceStopApp : this.mForceStopAppList) {
                if (this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(TAG, "forceStopApp = " + forceStopApp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAnrMonitorPackageList() {
        try {
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "updateAnrMonitorPackageList.. mAnrMonitorPackageStringList=" + this.mAnrMonitorPackageStringList);
            }
            if (this.mAnrMonitorPackageStringList != null) {
                VSlog.i(TAG, "mAnrMonitorPackageStringList size=" + this.mAnrMonitorPackageStringList.getValues().size());
                List<String> mAnrMonitorPackageTempList = this.mAnrMonitorPackageStringList.getValues();
                mAnrMonitorPackageTempList.removeAll(this.mAnrMonitorPackageList);
                this.mAnrMonitorPackageList.addAll(mAnrMonitorPackageTempList);
            }
            if (this.IS_LOG_CTRL_OPEN) {
                for (String anrMonitorPackage : this.mAnrMonitorPackageList) {
                    VSlog.i(TAG, "anrMonitorPackage = " + anrMonitorPackage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateActivityControlConfig() {
        try {
            if (this.mActivityControlList != null) {
                VSlog.i(TAG, "updateActivityControlConfig...");
                this.mIs_Incontrol = this.mActivityControlList.getValue(IS_INCONTROL) != null ? Boolean.parseBoolean(this.mActivityControlList.getValue(IS_INCONTROL)) : true;
                this.mMaxActivitiesInTask = this.mActivityControlList.getValue(MAXACTIVITIES) != null ? Integer.parseInt(this.mActivityControlList.getValue(MAXACTIVITIES)) : 60;
                this.mMaxSameActivitiesInTask = this.mActivityControlList.getValue(MAXSAMEACTIVITIES) != null ? Integer.parseInt(this.mActivityControlList.getValue(MAXSAMEACTIVITIES)) : 20;
                this.mVivoBgstartAllowed = this.mActivityControlList.getValue(VIVO_BGSTART) != null ? Boolean.parseBoolean(this.mActivityControlList.getValue(VIVO_BGSTART)) : false;
                this.mActivityOpt = this.mActivityControlList.getValue(ACTIVITYOPT) != null ? Boolean.parseBoolean(this.mActivityControlList.getValue(ACTIVITYOPT)) : true;
                this.mServiceOpt = this.mActivityControlList.getValue(SERVICEOPT) != null ? Boolean.parseBoolean(this.mActivityControlList.getValue(SERVICEOPT)) : true;
                this.mBroadcastOpt = this.mActivityControlList.getValue(BROADCASTOPT) != null ? Boolean.parseBoolean(this.mActivityControlList.getValue(BROADCASTOPT)) : true;
                this.mProviderOpt = this.mActivityControlList.getValue(PROVIDEROPT) != null ? Boolean.parseBoolean(this.mActivityControlList.getValue(PROVIDEROPT)) : true;
                this.mProcessOpt = this.mActivityControlList.getValue(PROCESSOPT) != null ? Boolean.parseBoolean(this.mActivityControlList.getValue(PROCESSOPT)) : true;
                this.mSizeCompatModeOpt = this.mActivityControlList.getValue(SIZECOMPATMODEOPT) != null ? Boolean.parseBoolean(this.mActivityControlList.getValue(SIZECOMPATMODEOPT)) : true;
                this.mForceStopOpt = this.mActivityControlList.getValue(FORCESTOPOPT) != null ? Boolean.parseBoolean(this.mActivityControlList.getValue(FORCESTOPOPT)) : true;
            }
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "updateActivityControlConfig finished. mIs_Incontrol:" + this.mIs_Incontrol + "  mMaxActivitiesInTask:" + this.mMaxActivitiesInTask + "  mMaxSameActivitiesInTask:" + this.mMaxSameActivitiesInTask + "  mVivoBgstartAllowed:" + this.mVivoBgstartAllowed + "  mActivityOpt:" + this.mActivityOpt + "  mServiceOpt:" + this.mServiceOpt + "  mBroadcastOpt:" + this.mBroadcastOpt + "  mProviderOpt:" + this.mProviderOpt + "  mProcessOpt:" + this.mProcessOpt + "  mSizeCompatModeOpt:" + this.mSizeCompatModeOpt + "  mForceStopOpt:" + this.mForceStopOpt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isBgStartAllowed(String pkgName) {
        return this.BGSTART_ALLOWED_APP.contains(pkgName) || this.mBgstartAllowedServiceList.contains(pkgName);
    }

    public List<String> getForceStopAppList() {
        return this.mForceStopAppList;
    }

    public List<String> getAnrMonitorPackageList() {
        return this.mAnrMonitorPackageList;
    }

    public List<String> getAnrDumpTracePersistentProcesses() {
        return this.mAnrDumpTracePersistentProcesses;
    }

    public List<String> getAnrDumpTraceNativeProcesses() {
        return this.mAnrDumpTraceNativeProcesses;
    }

    public boolean isInActivityNumControl() {
        return this.mIs_Incontrol;
    }

    public int getMaxActiviesInTask() {
        return this.mMaxActivitiesInTask;
    }

    public int getMaxSameActivitiesInTask() {
        return this.mMaxSameActivitiesInTask;
    }

    public boolean isActivityBgstartAllowed() {
        return this.mVivoBgstartAllowed;
    }

    public List<String> getBgStartAllowedActivityList() {
        return this.mBgstartAllowedActivityList;
    }

    public boolean isActivityOptEnabled() {
        return this.mActivityOpt;
    }

    public boolean isServiceOptEnabled() {
        return this.mServiceOpt;
    }

    public boolean isBroadcastOptEnabled() {
        return this.mBroadcastOpt;
    }

    public boolean isProviderOptEnabled() {
        return this.mProviderOpt;
    }

    public boolean isProcessOptEnabled() {
        return this.mProcessOpt;
    }

    public boolean isSizeCompatModeOptEnabled() {
        return this.mSizeCompatModeOpt;
    }

    public boolean isForceStopOptEnabled() {
        return this.mForceStopOpt;
    }

    public void dumpConfigList(PrintWriter pw, String[] args, int opti) {
        if (args.length > 1 || opti > 1) {
            pw.println("Bad activity command.");
            return;
        }
        pw.println("BgstartAllowedServiceList :");
        for (String bgStartAllowedService1 : this.BGSTART_ALLOWED_APP) {
            pw.println("  bgStartAllowedService: " + bgStartAllowedService1);
        }
        pw.println("  --------  ");
        for (String bgStartAllowedService : this.mBgstartAllowedServiceList) {
            pw.println("  bgStartAllowedService: " + bgStartAllowedService);
        }
        pw.println("ForceStopAppList :");
        for (String ForceStopApp : this.mForceStopAppList) {
            pw.println("  ForceStopApp: " + ForceStopApp);
        }
        pw.println("AnrMonitorPackageList :");
        for (String AnrMonitorPackage : this.mAnrMonitorPackageList) {
            pw.println("  AnrMonitorPackage: " + AnrMonitorPackage);
        }
        pw.println("BgstartAllowedActivityList :");
        for (String bgStartAllowedActivity : this.mBgstartAllowedActivityList) {
            pw.println("  bgStartAllowedActivity: " + bgStartAllowedActivity);
        }
        pw.println("ActivityNumControlConfig :");
        pw.println("  mIs_Incontrol: " + this.mIs_Incontrol);
        pw.println("  mMaxActivitiesInTask: " + this.mMaxActivitiesInTask);
        pw.println("  mMaxSameActivitiesInTask: " + this.mMaxSameActivitiesInTask);
        pw.println("  mVivoBgstartAllowed: " + this.mVivoBgstartAllowed);
        pw.println("  mActivityOpt: " + this.mActivityOpt);
        pw.println("  mServiceOpt: " + this.mServiceOpt);
        pw.println("  mBroadcastOpt: " + this.mBroadcastOpt);
        pw.println("  mProviderOpt: " + this.mProviderOpt);
        pw.println("  mProcessOpt: " + this.mProcessOpt);
        pw.println("  mSizeCompatModeOpt: " + this.mSizeCompatModeOpt);
        pw.println("  mForceStopOpt: " + this.mForceStopOpt);
    }
}