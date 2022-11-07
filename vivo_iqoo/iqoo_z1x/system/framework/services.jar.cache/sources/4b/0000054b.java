package com.android.server;

import android.app.ActivityThread;
import android.app.AppCompatCallbacks;
import android.app.ApplicationErrorReport;
import android.app.ContextImpl;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.hardware.display.DisplayManagerInternal;
import android.net.ConnectivityModuleConnector;
import android.net.NetworkStackClient;
import android.os.Build;
import android.os.FactoryTest;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.IIncidentManager;
import android.os.Message;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.sysprop.VoldProperties;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.BatteryService;
import com.android.server.BinderCallsStatsService;
import com.android.server.LooperStatsService;
import com.android.server.am.ActivityManagerService;
import com.android.server.attention.AttentionManagerService;
import com.android.server.compat.PlatformCompat;
import com.android.server.compat.PlatformCompatNative;
import com.android.server.contentcapture.ContentCaptureManagerInternal;
import com.android.server.display.DisplayManagerService;
import com.android.server.gpu.GpuService;
import com.android.server.input.InputManagerService;
import com.android.server.lights.LightsService;
import com.android.server.media.MediaRouterService;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.om.OverlayManagerService;
import com.android.server.os.BugreportManagerService;
import com.android.server.os.DeviceIdentifiersPolicyService;
import com.android.server.pm.DataLoaderManagerService;
import com.android.server.pm.Installer;
import com.android.server.pm.OtaDexoptService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.dex.SystemServerDexLoadReporter;
import com.android.server.power.PowerManagerService;
import com.android.server.power.ShutdownThread;
import com.android.server.power.ThermalManagerService;
import com.android.server.recoverysystem.RecoverySystemService;
import com.android.server.security.FileIntegrityService;
import com.android.server.uri.UriGrantsManagerService;
import com.android.server.usage.UsageStatsService;
import com.android.server.utils.TimingsTraceAndSlog;
import com.android.server.webkit.WebViewUpdateService;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowManagerGlobalLock;
import com.android.server.wm.WindowManagerService;
import com.vivo.CTSUtils;
import com.vivo.perf.bigdata.IVivoPerfBigdata;
import com.vivo.server.adapter.AbsSystemServerAdapter;
import com.vivo.server.adapter.ServiceAdapterFactory;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class SystemServer {
    private static final String ACCESSIBILITY_MANAGER_SERVICE_CLASS = "com.android.server.accessibility.AccessibilityManagerService$Lifecycle";
    private static final String ACCOUNT_SERVICE_CLASS = "com.android.server.accounts.AccountManagerService$Lifecycle";
    private static final String ADB_SERVICE_CLASS = "com.android.server.adb.AdbService$Lifecycle";
    private static final String APPWIDGET_SERVICE_CLASS = "com.android.server.appwidget.AppWidgetService";
    private static final String APP_PREDICTION_MANAGER_SERVICE_CLASS = "com.android.server.appprediction.AppPredictionManagerService";
    private static final String AUTO_FILL_MANAGER_SERVICE_CLASS = "com.android.server.autofill.AutofillManagerService";
    private static final String BACKUP_MANAGER_SERVICE_CLASS = "com.android.server.backup.BackupManagerService$Lifecycle";
    private static final String BLOB_STORE_MANAGER_SERVICE_CLASS = "com.android.server.blob.BlobStoreManagerService";
    private static final String BLOCK_MAP_FILE = "/cache/recovery/block.map";
    private static final TimingsTraceAndSlog BOOT_TIMINGS_TRACE_LOG = new TimingsTraceAndSlog("SystemServerTiming", 524288);
    private static final String CAR_SERVICE_HELPER_SERVICE_CLASS = "com.android.internal.car.CarServiceHelperService";
    private static final String COMPANION_DEVICE_MANAGER_SERVICE_CLASS = "com.android.server.companion.CompanionDeviceManagerService";
    private static final String CONTENT_CAPTURE_MANAGER_SERVICE_CLASS = "com.android.server.contentcapture.ContentCaptureManagerService";
    private static final String CONTENT_SERVICE_CLASS = "com.android.server.content.ContentService$Lifecycle";
    private static final String CONTENT_SUGGESTIONS_SERVICE_CLASS = "com.android.server.contentsuggestions.ContentSuggestionsManagerService";
    private static final int DEFAULT_SYSTEM_THEME = 16974851;
    private static final String DEVICE_IDLE_CONTROLLER_CLASS = "com.android.server.DeviceIdleController";
    private static final long EARLIEST_SUPPORTED_TIME = 86400000;
    private static final String ENCRYPTED_STATE = "1";
    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String ETHERNET_SERVICE_CLASS = "com.android.server.ethernet.EthernetService";
    private static final String GSI_RUNNING_PROP = "ro.gsid.image_running";
    private static final String IOT_SERVICE_CLASS = "com.android.things.server.IoTSystemService";
    private static final String JOB_SCHEDULER_SERVICE_CLASS = "com.android.server.job.JobSchedulerService";
    public static final String LAST_AIRPLANE_MODE_ON = "last_airplane_mode_on";
    public static final String LAST_SAFEMODE_ON = "last_safemode_on";
    private static final String LOCK_SETTINGS_SERVICE_CLASS = "com.android.server.locksettings.LockSettingsService$Lifecycle";
    private static final String LOWPAN_SERVICE_CLASS = "com.android.server.lowpan.LowpanService";
    private static final String MIDI_SERVICE_CLASS = "com.android.server.midi.MidiService$Lifecycle";
    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";
    private static final String PRINT_MANAGER_SERVICE_CLASS = "com.android.server.print.PrintManagerService";
    private static final String ROLLBACK_MANAGER_SERVICE_CLASS = "com.android.server.rollback.RollbackManagerService";
    private static final String SEARCH_MANAGER_SERVICE_CLASS = "com.android.server.search.SearchManagerService$Lifecycle";
    private static final String SLICE_MANAGER_SERVICE_CLASS = "com.android.server.slice.SliceManagerService$Lifecycle";
    private static final long SLOW_DELIVERY_THRESHOLD_MS = 200;
    private static final long SLOW_DISPATCH_THRESHOLD_MS = 100;
    private static final long SNAPSHOT_INTERVAL = 3600000;
    private static final String START_BLOB_STORE_SERVICE = "startBlobStoreManagerService";
    private static final String START_HIDL_SERVICES = "StartHidlServices";
    private static final String START_SENSOR_SERVICE = "StartSensorService";
    private static final String STATS_COMPANION_APEX_PATH = "/apex/com.android.os.statsd/javalib/service-statsd.jar";
    private static final String STATS_COMPANION_LIFECYCLE_CLASS = "com.android.server.stats.StatsCompanion$Lifecycle";
    private static final String STATS_PULL_ATOM_SERVICE_CLASS = "com.android.server.stats.pull.StatsPullAtomService";
    private static final String STORAGE_MANAGER_SERVICE_CLASS = "com.android.server.StorageManagerService$Lifecycle";
    private static final String STORAGE_STATS_SERVICE_CLASS = "com.android.server.usage.StorageStatsService$Lifecycle";
    private static final String SYSPROP_START_COUNT = "sys.system_server.start_count";
    private static final String SYSPROP_START_ELAPSED = "sys.system_server.start_elapsed";
    private static final String SYSPROP_START_UPTIME = "sys.system_server.start_uptime";
    private static final String SYSTEM_CAPTIONS_MANAGER_SERVICE_CLASS = "com.android.server.systemcaptions.SystemCaptionsManagerService";
    private static final String SYSTEM_SERVER_TIMING_TAG = "SystemServerTiming";
    private static final String TAG = "SystemServer";
    private static final String TETHERING_CONNECTOR_CLASS = "android.net.ITetheringConnector";
    private static final String THERMAL_OBSERVER_CLASS = "com.google.android.clockwork.ThermalObserver";
    private static final String TIME_DETECTOR_SERVICE_CLASS = "com.android.server.timedetector.TimeDetectorService$Lifecycle";
    private static final String TIME_ZONE_DETECTOR_SERVICE_CLASS = "com.android.server.timezonedetector.TimeZoneDetectorService$Lifecycle";
    private static final String TIME_ZONE_RULES_MANAGER_SERVICE_CLASS = "com.android.server.timezone.RulesManagerService$Lifecycle";
    private static final String UNCRYPT_PACKAGE_FILE = "/cache/recovery/uncrypt_file";
    private static final String USB_SERVICE_CLASS = "com.android.server.usb.UsbService$Lifecycle";
    private static final String VOICE_RECOGNITION_MANAGER_SERVICE_CLASS = "com.android.server.voiceinteraction.VoiceInteractionManagerService";
    private static final String WALLPAPER_SERVICE_CLASS = "com.android.server.wallpaper.WallpaperManagerService$Lifecycle";
    private static final String WEAR_CONNECTIVITY_SERVICE_CLASS = "com.android.clockwork.connectivity.WearConnectivityService";
    private static final String WEAR_DISPLAY_SERVICE_CLASS = "com.google.android.clockwork.display.WearDisplayService";
    private static final String WEAR_GLOBAL_ACTIONS_SERVICE_CLASS = "com.android.clockwork.globalactions.GlobalActionsService";
    private static final String WEAR_LEFTY_SERVICE_CLASS = "com.google.android.clockwork.lefty.WearLeftyService";
    private static final String WEAR_POWER_SERVICE_CLASS = "com.android.clockwork.power.WearPowerService";
    private static final String WEAR_SIDEKICK_SERVICE_CLASS = "com.google.android.clockwork.sidekick.SidekickService";
    private static final String WEAR_TIME_SERVICE_CLASS = "com.google.android.clockwork.time.WearTimeService";
    private static final String WIFI_AWARE_SERVICE_CLASS = "com.android.server.wifi.aware.WifiAwareService";
    private static final String WIFI_P2P_SERVICE_CLASS = "com.android.server.wifi.p2p.WifiP2pService";
    private static final String WIFI_SERVICE_CLASS = "com.android.server.wifi.WifiService";
    private static final int sMaxBinderThreads = 31;
    private static LinkedList<Pair<String, ApplicationErrorReport.CrashInfo>> sPendingWtfs;
    private ActivityManagerService mActivityManagerService;
    private Future<?> mBlobStoreServiceStart;
    private ContentResolver mContentResolver;
    private DataLoaderManagerService mDataLoaderManagerService;
    private DisplayManagerService mDisplayManagerService;
    private EntropyMixer mEntropyMixer;
    private final int mFactoryTestMode;
    private boolean mFirstBoot;
    private long mIncrementalServiceHandle = 0;
    private boolean mIsAirplaneModeChanged = false;
    private boolean mOnlyCore;
    private PackageManager mPackageManager;
    private PackageManagerService mPackageManagerService;
    private PowerManagerService mPowerManagerService;
    private Timer mProfilerSnapshotTimer;
    private final boolean mRuntimeRestart;
    private final long mRuntimeStartElapsedTime;
    private final long mRuntimeStartUptime;
    private Future<?> mSensorServiceStart;
    private final int mStartCount;
    private Context mSystemContext;
    private AbsSystemServerAdapter mSystemServerAdapter;
    private SystemServiceManager mSystemServiceManager;
    private IVivoPerfBigdata mVivoPerfBigdata;
    private IVivoSystemServer mVivoSystemServer;
    private WebViewUpdateService mWebViewUpdateService;
    private WindowManagerGlobalLock mWindowManagerGlobalLock;
    private Future<?> mZygotePreload;

    private static native void initZygoteChildHeapProfiling();

    private static native void setIncrementalServiceSystemReady(long j);

    private static native void spawnFdLeakCheckThread();

    private static native void startHidlServices();

    private static native long startIncrementalService();

    private static native void startSensorService();

    public static void main(String[] args) {
        new SystemServer().run();
    }

    public SystemServer() {
        Slog.e(CTSUtils.wrapLogTag("PackageManager"), "Fix for b/169414761 is applied");
        this.mFactoryTestMode = FactoryTest.getMode();
        this.mStartCount = SystemProperties.getInt(SYSPROP_START_COUNT, 0) + 1;
        this.mRuntimeStartElapsedTime = SystemClock.elapsedRealtime();
        long uptimeMillis = SystemClock.uptimeMillis();
        this.mRuntimeStartUptime = uptimeMillis;
        Process.setStartTimes(this.mRuntimeStartElapsedTime, uptimeMillis);
        this.mRuntimeRestart = "1".equals(SystemProperties.get("sys.boot_completed"));
        if (ServiceAdapterFactory.getServiceAdapterFactory() != null) {
            this.mSystemServerAdapter = ServiceAdapterFactory.getServiceAdapterFactory().getSystemServerAdapter();
        } else {
            Slog.w(TAG, "ServiceAdapterFactory null.");
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:40:0x0159, code lost:
        if (android.os.SystemProperties.get("persist.vivo.stability.debug", "0").equals("1") != false) goto L100;
     */
    /* JADX WARN: Code restructure failed: missing block: B:7:0x0067, code lost:
        if (r5.isEmpty() != false) goto L6;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void run() {
        /*
            Method dump skipped, instructions count: 660
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.SystemServer.run():void");
    }

    private boolean isFirstBootOrUpgrade() {
        return this.mPackageManagerService.isFirstBoot() || this.mPackageManagerService.isDeviceUpgrading();
    }

    private void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + msg, e);
    }

    private void performPendingShutdown() {
        final String reason;
        String shutdownAction = SystemProperties.get(ShutdownThread.SHUTDOWN_ACTION_PROPERTY, "");
        if (shutdownAction != null && shutdownAction.length() > 0) {
            final boolean reboot = shutdownAction.charAt(0) == '1';
            if (shutdownAction.length() > 1) {
                reason = shutdownAction.substring(1, shutdownAction.length());
            } else {
                reason = null;
            }
            if (reason != null && reason.startsWith("recovery-update")) {
                File packageFile = new File(UNCRYPT_PACKAGE_FILE);
                if (packageFile.exists()) {
                    String filename = null;
                    try {
                        filename = FileUtils.readTextFile(packageFile, 0, null);
                    } catch (IOException e) {
                        Slog.e(TAG, "Error reading uncrypt package file", e);
                    }
                    if (filename != null && filename.startsWith("/data") && !new File(BLOCK_MAP_FILE).exists()) {
                        Slog.e(TAG, "Can't find block map file, uncrypt failed or unexpected runtime restart?");
                        return;
                    }
                }
            }
            Runnable runnable = new Runnable() { // from class: com.android.server.SystemServer.1
                @Override // java.lang.Runnable
                public void run() {
                    synchronized (this) {
                        ShutdownThread.rebootOrShutdown(null, reboot, reason);
                    }
                }
            };
            Message msg = Message.obtain(UiThread.getHandler(), runnable);
            msg.setAsynchronous(true);
            UiThread.getHandler().sendMessage(msg);
        }
    }

    private void createSystemContext() {
        ActivityThread activityThread = ActivityThread.systemMain();
        ContextImpl systemContext = activityThread.getSystemContext();
        this.mSystemContext = systemContext;
        systemContext.setTheme(DEFAULT_SYSTEM_THEME);
        activityThread.getSystemUiContext().setTheme(DEFAULT_SYSTEM_THEME);
    }

    /* JADX WARN: Type inference failed for: r5v2, types: [com.android.server.compat.PlatformCompat, android.os.IBinder] */
    private void startBootstrapServices(TimingsTraceAndSlog t) {
        t.traceBegin("startBootstrapServices");
        t.traceBegin("StartWatchdog");
        Watchdog watchdog = Watchdog.getInstance();
        watchdog.start();
        t.traceEnd();
        Slog.i(TAG, "Reading configuration...");
        t.traceBegin("ReadingSystemConfig");
        SystemServerInitThreadPool.submit($$Lambda$YWiwiKm_Qgqb55C6tTuq_n2JzdY.INSTANCE, "ReadingSystemConfig");
        t.traceEnd();
        t.traceBegin("PlatformCompat");
        ?? platformCompat = new PlatformCompat(this.mSystemContext);
        ServiceManager.addService("platform_compat", (IBinder) platformCompat);
        ServiceManager.addService("platform_compat_native", new PlatformCompatNative(platformCompat));
        AppCompatCallbacks.install(new long[0]);
        t.traceEnd();
        t.traceBegin("StartFileIntegrityService");
        this.mSystemServiceManager.startService(FileIntegrityService.class);
        t.traceEnd();
        IVivoSystemServer iVivoSystemServer = this.mVivoSystemServer;
        if (iVivoSystemServer != null) {
            iVivoSystemServer.startConfigurationManagerService(this.mSystemContext);
        }
        IVivoSystemServer iVivoSystemServer2 = this.mVivoSystemServer;
        if (iVivoSystemServer2 != null) {
            iVivoSystemServer2.startVgcManagerService(this.mSystemContext);
        }
        t.traceBegin("StartInstaller");
        Installer installer = (Installer) this.mSystemServiceManager.startService(Installer.class);
        t.traceEnd();
        t.traceBegin("DeviceIdentifiersPolicyService");
        this.mSystemServiceManager.startService(DeviceIdentifiersPolicyService.class);
        t.traceEnd();
        t.traceBegin("UriGrantsManagerService");
        this.mSystemServiceManager.startService(UriGrantsManagerService.Lifecycle.class);
        t.traceEnd();
        t.traceBegin("StartActivityManager");
        ActivityTaskManagerService atm = ((ActivityTaskManagerService.Lifecycle) this.mSystemServiceManager.startService(ActivityTaskManagerService.Lifecycle.class)).getService();
        ActivityManagerService startService = ActivityManagerService.Lifecycle.startService(this.mSystemServiceManager, atm);
        this.mActivityManagerService = startService;
        startService.setSystemServiceManager(this.mSystemServiceManager);
        this.mActivityManagerService.setInstaller(installer);
        this.mWindowManagerGlobalLock = atm.getGlobalLock();
        t.traceEnd();
        t.traceBegin("StartDataLoaderManagerService");
        this.mDataLoaderManagerService = (DataLoaderManagerService) this.mSystemServiceManager.startService(DataLoaderManagerService.class);
        t.traceEnd();
        t.traceBegin("StartIncrementalService");
        this.mIncrementalServiceHandle = startIncrementalService();
        t.traceEnd();
        t.traceBegin("StartPowerManager");
        this.mPowerManagerService = (PowerManagerService) this.mSystemServiceManager.startService(PowerManagerService.class);
        t.traceEnd();
        t.traceBegin("StartThermalManager");
        this.mSystemServiceManager.startService(ThermalManagerService.class);
        t.traceEnd();
        t.traceBegin("InitPowerManagement");
        this.mActivityManagerService.initPowerManagement();
        t.traceEnd();
        t.traceBegin("StartRecoverySystemService");
        this.mSystemServiceManager.startService(RecoverySystemService.Lifecycle.class);
        t.traceEnd();
        RescueParty.registerHealthObserver(this.mSystemContext);
        PackageWatchdog.getInstance(this.mSystemContext).noteBoot();
        t.traceBegin("StartLightsService");
        this.mSystemServiceManager.startService(LightsService.class);
        t.traceEnd();
        t.traceBegin("StartSidekickService");
        if (SystemProperties.getBoolean("config.enable_sidekick_graphics", false)) {
            this.mSystemServiceManager.startService(WEAR_SIDEKICK_SERVICE_CLASS);
        }
        t.traceEnd();
        t.traceBegin("StartDisplayManager");
        this.mDisplayManagerService = (DisplayManagerService) this.mSystemServiceManager.startService(DisplayManagerService.class);
        t.traceEnd();
        IVivoSystemServer iVivoSystemServer3 = this.mVivoSystemServer;
        if (iVivoSystemServer3 != null) {
            iVivoSystemServer3.startDisplayStateService();
        }
        t.traceBegin("WaitForDisplay");
        this.mSystemServiceManager.startBootPhase(t, 100);
        t.traceEnd();
        String cryptState = (String) VoldProperties.decrypt().orElse("");
        boolean z = true;
        if (ENCRYPTING_STATE.equals(cryptState)) {
            Slog.w(TAG, "Detected encryption in progress - only parsing core apps");
            this.mOnlyCore = true;
        } else if ("1".equals(cryptState)) {
            Slog.w(TAG, "Device encrypted - only parsing core apps");
            this.mOnlyCore = true;
        }
        if (!this.mRuntimeRestart) {
            FrameworkStatsLog.write(240, 14, SystemClock.elapsedRealtime());
        }
        t.traceBegin("StartPackageManagerService");
        try {
            Watchdog.getInstance().pauseWatchingCurrentThread("packagemanagermain");
            Context context = this.mSystemContext;
            if (this.mFactoryTestMode == 0) {
                z = false;
            }
            this.mPackageManagerService = PackageManagerService.main(context, installer, z, this.mOnlyCore);
            Watchdog.getInstance().resumeWatchingCurrentThread("packagemanagermain");
            SystemServerDexLoadReporter.configureSystemServerDexReporter(this.mPackageManagerService);
            this.mFirstBoot = this.mPackageManagerService.isFirstBoot();
            this.mPackageManager = this.mSystemContext.getPackageManager();
            t.traceEnd();
            if (!this.mRuntimeRestart && !isFirstBootOrUpgrade()) {
                FrameworkStatsLog.write(240, 15, SystemClock.elapsedRealtime());
            }
            if (!this.mOnlyCore) {
                boolean disableOtaDexopt = SystemProperties.getBoolean("config.disable_otadexopt", false);
                if (!disableOtaDexopt) {
                    t.traceBegin("StartOtaDexOptService");
                    try {
                        Watchdog.getInstance().pauseWatchingCurrentThread("moveab");
                        OtaDexoptService.main(this.mSystemContext, this.mPackageManagerService);
                    } finally {
                        try {
                        } finally {
                        }
                    }
                }
            }
            t.traceBegin("StartUserManagerService");
            this.mSystemServiceManager.startService(UserManagerService.LifeCycle.class);
            t.traceEnd();
            t.traceBegin("InitAttributerCache");
            AttributeCache.init(this.mSystemContext);
            t.traceEnd();
            t.traceBegin("SetSystemProcess");
            this.mActivityManagerService.setSystemProcess();
            t.traceEnd();
            t.traceBegin("InitWatchdog");
            watchdog.init(this.mSystemContext, this.mActivityManagerService);
            t.traceEnd();
            this.mDisplayManagerService.setupSchedulerPolicies();
            t.traceBegin("StartOverlayManagerService");
            this.mSystemServiceManager.startService(new OverlayManagerService(this.mSystemContext));
            t.traceEnd();
            t.traceBegin("StartSensorPrivacyService");
            this.mSystemServiceManager.startService(new SensorPrivacyService(this.mSystemContext));
            t.traceEnd();
            if (SystemProperties.getInt("persist.sys.displayinset.top", 0) > 0) {
                this.mActivityManagerService.updateSystemUiContext();
                ((DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class)).onOverlayChanged();
            }
            this.mSensorServiceStart = SystemServerInitThreadPool.submit($$Lambda$SystemServer$UyrPns7R814gZEylCbDKhe8It4.INSTANCE, START_SENSOR_SERVICE);
            t.traceEnd();
        } catch (Throwable th) {
            Watchdog.getInstance().resumeWatchingCurrentThread("packagemanagermain");
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$startBootstrapServices$0() {
        TimingsTraceAndSlog traceLog = TimingsTraceAndSlog.newAsyncLog();
        traceLog.traceBegin(START_SENSOR_SERVICE);
        startSensorService();
        traceLog.traceEnd();
    }

    private void startCoreServices(TimingsTraceAndSlog t) {
        t.traceBegin("startCoreServices");
        t.traceBegin("StartSystemConfigService");
        this.mSystemServiceManager.startService(SystemConfigService.class);
        t.traceEnd();
        t.traceBegin("StartBatteryService");
        this.mSystemServiceManager.startService(BatteryService.class);
        t.traceEnd();
        t.traceBegin("StartUsageService");
        this.mSystemServiceManager.startService(UsageStatsService.class);
        this.mActivityManagerService.setUsageStatsManager((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class));
        t.traceEnd();
        if (this.mPackageManager.hasSystemFeature("android.software.webview")) {
            t.traceBegin("StartWebViewUpdateService");
            this.mWebViewUpdateService = (WebViewUpdateService) this.mSystemServiceManager.startService(WebViewUpdateService.class);
            t.traceEnd();
        }
        t.traceBegin("StartCachedDeviceStateService");
        this.mSystemServiceManager.startService(CachedDeviceStateService.class);
        t.traceEnd();
        t.traceBegin("StartBinderCallsStatsService");
        this.mSystemServiceManager.startService(BinderCallsStatsService.LifeCycle.class);
        t.traceEnd();
        t.traceBegin("StartLooperStatsService");
        this.mSystemServiceManager.startService(LooperStatsService.Lifecycle.class);
        t.traceEnd();
        t.traceBegin("StartRollbackManagerService");
        this.mSystemServiceManager.startService(ROLLBACK_MANAGER_SERVICE_CLASS);
        t.traceEnd();
        t.traceBegin("StartBugreportManagerService");
        this.mSystemServiceManager.startService(BugreportManagerService.class);
        t.traceEnd();
        t.traceBegin(GpuService.TAG);
        this.mSystemServiceManager.startService(GpuService.class);
        t.traceEnd();
        IVivoSystemServer iVivoSystemServer = this.mVivoSystemServer;
        if (iVivoSystemServer != null) {
            iVivoSystemServer.startCoreServices();
        }
        t.traceEnd();
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:258:0x0854
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    private void startOtherServices(com.android.server.utils.TimingsTraceAndSlog r57) {
        /*
            Method dump skipped, instructions count: 4725
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.SystemServer.startOtherServices(com.android.server.utils.TimingsTraceAndSlog):void");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$startOtherServices$1() {
        try {
            Slog.i(TAG, "SecondaryZygotePreload");
            TimingsTraceAndSlog traceLog = TimingsTraceAndSlog.newAsyncLog();
            traceLog.traceBegin("SecondaryZygotePreload");
            if (!Process.ZYGOTE_PROCESS.preloadDefault(Build.SUPPORTED_32_BIT_ABIS[0])) {
                Slog.e(TAG, "Unable to preload default resources");
            }
            traceLog.traceEnd();
        } catch (Exception ex) {
            Slog.e(TAG, "Exception preloading default resources", ex);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$startOtherServices$2() {
        TimingsTraceAndSlog traceLog = TimingsTraceAndSlog.newAsyncLog();
        traceLog.traceBegin(START_HIDL_SERVICES);
        startHidlServices();
        traceLog.traceEnd();
    }

    public /* synthetic */ void lambda$startOtherServices$3$SystemServer() {
        TimingsTraceAndSlog traceLog = TimingsTraceAndSlog.newAsyncLog();
        traceLog.traceBegin(START_BLOB_STORE_SERVICE);
        this.mSystemServiceManager.startService(BLOB_STORE_MANAGER_SERVICE_CLASS);
        traceLog.traceEnd();
    }

    public /* synthetic */ void lambda$startOtherServices$6$SystemServer(TimingsTraceAndSlog t, Context context, WindowManagerService windowManagerF, boolean safeMode, ConnectivityService connectivityF, NetworkManagementService networkManagementF, NetworkPolicyManagerService networkPolicyF, IpSecService ipSecServiceF, NetworkStatsService networkStatsF, CountryDetectorService countryDetectorF, NetworkTimeUpdateService networkTimeUpdaterF, InputManagerService inputManagerF, TelephonyRegistry telephonyRegistryF, MediaRouterService mediaRouterF, MmsServiceBroker mmsServiceF) {
        Future<?> webviewPrep;
        CountDownLatch networkPolicyInitReadySignal;
        Slog.i(TAG, "Making services ready");
        IVivoSystemServer iVivoSystemServer = this.mVivoSystemServer;
        if (iVivoSystemServer != null) {
            iVivoSystemServer.startOtherServices();
        }
        IVivoSystemServer iVivoSystemServer2 = this.mVivoSystemServer;
        if (iVivoSystemServer2 != null) {
            iVivoSystemServer2.startFaceUi();
        }
        t.traceBegin("StartActivityManagerReadyPhase");
        this.mSystemServiceManager.startBootPhase(t, SystemService.PHASE_ACTIVITY_MANAGER_READY);
        t.traceEnd();
        t.traceBegin("StartObservingNativeCrashes");
        try {
            this.mActivityManagerService.startObservingNativeCrashes();
        } catch (Throwable e) {
            reportWtf("observing native crashes", e);
        }
        t.traceEnd();
        if (!this.mOnlyCore && this.mWebViewUpdateService != null) {
            Future<?> webviewPrep2 = SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.-$$Lambda$SystemServer$72PvntN28skIthlRYR9w5EhsdX8
                @Override // java.lang.Runnable
                public final void run() {
                    SystemServer.this.lambda$startOtherServices$4$SystemServer();
                }
            }, "WebViewFactoryPreparation");
            webviewPrep = webviewPrep2;
        } else {
            webviewPrep = null;
        }
        if (this.mPackageManager.hasSystemFeature("android.hardware.type.automotive")) {
            t.traceBegin("StartCarServiceHelperService");
            this.mSystemServiceManager.startService(CAR_SERVICE_HELPER_SERVICE_CLASS);
            t.traceEnd();
        }
        IVivoSystemServer iVivoSystemServer3 = this.mVivoSystemServer;
        if (iVivoSystemServer3 != null) {
            iVivoSystemServer3.startANRManagerService(context, this.mActivityManagerService);
        }
        t.traceBegin("StartSystemUI");
        try {
            startSystemUi(context, windowManagerF);
        } catch (Throwable e2) {
            reportWtf("starting System UI", e2);
        }
        t.traceEnd();
        if (!safeMode) {
            try {
                if (Settings.Global.getInt(context.getContentResolver(), LAST_SAFEMODE_ON, 0) == 1) {
                    Settings.Global.putInt(context.getContentResolver(), LAST_SAFEMODE_ON, 0);
                    if (this.mIsAirplaneModeChanged) {
                        t.traceBegin("disableAirplaneModeInSafeMode");
                        connectivityF.setAirplaneMode(false);
                        t.traceEnd();
                    }
                    VSlog.d(TAG, "exitSafeMode last safemode: " + Settings.Global.getInt(context.getContentResolver(), LAST_SAFEMODE_ON, 0));
                    VSlog.d(TAG, "exitSafeMode last airplanemode: " + Settings.Global.getInt(context.getContentResolver(), LAST_AIRPLANE_MODE_ON, 0));
                    VSlog.d(TAG, "exitSafeMode AIRPLANE_MODE_ON: " + Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0));
                }
            } catch (Exception ex) {
                VSlog.e(TAG, "exitSafeMode Exception.", ex);
            }
        } else {
            t.traceBegin("EnableAirplaneModeInSafeMode");
            try {
                connectivityF.setAirplaneMode(true);
            } catch (Throwable e3) {
                reportWtf("enabling Airplane Mode during Safe Mode bootup", e3);
            }
            t.traceEnd();
        }
        t.traceBegin("MakeNetworkManagementServiceReady");
        if (networkManagementF != null) {
            try {
                networkManagementF.systemReady();
            } catch (Throwable e4) {
                reportWtf("making Network Managment Service ready", e4);
            }
        }
        if (networkPolicyF == null) {
            networkPolicyInitReadySignal = null;
        } else {
            CountDownLatch networkPolicyInitReadySignal2 = networkPolicyF.networkScoreAndNetworkManagementServiceReady();
            networkPolicyInitReadySignal = networkPolicyInitReadySignal2;
        }
        t.traceEnd();
        t.traceBegin("MakeIpSecServiceReady");
        if (ipSecServiceF != null) {
            try {
                ipSecServiceF.systemReady();
            } catch (Throwable e5) {
                reportWtf("making IpSec Service ready", e5);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeNetworkStatsServiceReady");
        if (networkStatsF != null) {
            try {
                networkStatsF.systemReady();
            } catch (Throwable e6) {
                reportWtf("making Network Stats Service ready", e6);
            }
        }
        t.traceEnd();
        AbsSystemServerAdapter absSystemServerAdapter = this.mSystemServerAdapter;
        if (absSystemServerAdapter != null) {
            absSystemServerAdapter.addBootEvent("SystemServer:NetworkStatsService systemReady");
        }
        t.traceBegin("MakeConnectivityServiceReady");
        if (connectivityF != null) {
            try {
                connectivityF.systemReady();
            } catch (Throwable e7) {
                reportWtf("making Connectivity Service ready", e7);
            }
        }
        t.traceEnd();
        AbsSystemServerAdapter absSystemServerAdapter2 = this.mSystemServerAdapter;
        if (absSystemServerAdapter2 != null) {
            absSystemServerAdapter2.addBootEvent("SystemServer:ConnectivityService systemReady");
        }
        t.traceBegin("MakeNetworkPolicyServiceReady");
        if (networkPolicyF != null) {
            try {
                networkPolicyF.systemReady(networkPolicyInitReadySignal);
            } catch (Throwable e8) {
                reportWtf("making Network Policy Service ready", e8);
            }
        }
        t.traceEnd();
        AbsSystemServerAdapter absSystemServerAdapter3 = this.mSystemServerAdapter;
        if (absSystemServerAdapter3 != null) {
            absSystemServerAdapter3.addBootEvent("SystemServer:NetworkPolicyManagerServ systemReady");
        }
        this.mPackageManagerService.waitForAppDataPrepared();
        t.traceBegin("PhaseThirdPartyAppsCanStart");
        if (webviewPrep != null) {
            ConcurrentUtils.waitForFutureNoInterrupt(webviewPrep, "WebViewFactoryPreparation");
        }
        this.mSystemServiceManager.startBootPhase(t, SystemService.PHASE_THIRD_PARTY_APPS_CAN_START);
        t.traceEnd();
        t.traceBegin("StartNetworkStack");
        try {
            NetworkStackClient.getInstance().start();
        } catch (Throwable e9) {
            reportWtf("starting Network Stack", e9);
        }
        t.traceEnd();
        t.traceBegin("StartTethering");
        try {
            ConnectivityModuleConnector.getInstance().startModuleService(TETHERING_CONNECTOR_CLASS, "android.permission.MAINLINE_NETWORK_STACK", $$Lambda$SystemServer$zn6ji6g70a_qrK5QZEPCaarZSik.INSTANCE);
        } catch (Throwable e10) {
            reportWtf("starting Tethering", e10);
        }
        t.traceEnd();
        t.traceBegin("MakeCountryDetectionServiceReady");
        if (countryDetectorF != null) {
            try {
                countryDetectorF.systemRunning();
            } catch (Throwable e11) {
                reportWtf("Notifying CountryDetectorService running", e11);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeNetworkTimeUpdateReady");
        if (networkTimeUpdaterF != null) {
            try {
                networkTimeUpdaterF.systemRunning();
            } catch (Throwable e12) {
                reportWtf("Notifying NetworkTimeService running", e12);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeInputManagerServiceReady");
        if (inputManagerF != null) {
            try {
                inputManagerF.systemRunning();
            } catch (Throwable e13) {
                reportWtf("Notifying InputManagerService running", e13);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeTelephonyRegistryReady");
        if (telephonyRegistryF != null) {
            try {
                telephonyRegistryF.systemRunning();
            } catch (Throwable e14) {
                reportWtf("Notifying TelephonyRegistry running", e14);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeMediaRouterServiceReady");
        if (mediaRouterF != null) {
            try {
                mediaRouterF.systemRunning();
            } catch (Throwable e15) {
                reportWtf("Notifying MediaRouterService running", e15);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeMmsServiceReady");
        if (mmsServiceF != null) {
            try {
                mmsServiceF.systemRunning();
            } catch (Throwable e16) {
                reportWtf("Notifying MmsService running", e16);
            }
        }
        t.traceEnd();
        t.traceBegin("IncidentDaemonReady");
        try {
            IIncidentManager incident = IIncidentManager.Stub.asInterface(ServiceManager.getService("incident"));
            if (incident != null) {
                incident.systemRunning();
            }
        } catch (Throwable e17) {
            reportWtf("Notifying incident daemon running", e17);
        }
        t.traceEnd();
        if (this.mSystemServerAdapter != null) {
            t.traceBegin("NetworkDataControllerService");
            try {
                this.mSystemServerAdapter.startNetworkDataControllerService(context);
            } catch (Throwable e18) {
                reportWtf("starting NetworkDataControllerService:", e18);
            }
            t.traceEnd();
        }
        AbsSystemServerAdapter absSystemServerAdapter4 = this.mSystemServerAdapter;
        if (absSystemServerAdapter4 != null) {
            absSystemServerAdapter4.addBootEvent("SystemServer:PhaseThirdPartyAppsCanStart");
        }
        if (this.mIncrementalServiceHandle != 0) {
            t.traceBegin("MakeIncrementalServiceReady");
            setIncrementalServiceSystemReady(this.mIncrementalServiceHandle);
            t.traceEnd();
        }
        IVivoSystemServer iVivoSystemServer4 = this.mVivoSystemServer;
        if (iVivoSystemServer4 != null) {
            iVivoSystemServer4.startHangVivoConfigService();
        }
        IVivoSystemServer iVivoSystemServer5 = this.mVivoSystemServer;
        if (iVivoSystemServer5 != null) {
            iVivoSystemServer5.startAutoRecoverService(this.mActivityManagerService);
        }
    }

    public /* synthetic */ void lambda$startOtherServices$4$SystemServer() {
        Slog.i(TAG, "WebViewFactoryPreparation");
        TimingsTraceAndSlog traceLog = TimingsTraceAndSlog.newAsyncLog();
        traceLog.traceBegin("WebViewFactoryPreparation");
        ConcurrentUtils.waitForFutureNoInterrupt(this.mZygotePreload, "Zygote preload");
        this.mZygotePreload = null;
        this.mWebViewUpdateService.prepareWebViewInSystemServer();
        traceLog.traceEnd();
    }

    private boolean deviceHasConfigString(Context context, int resId) {
        String serviceName = context.getString(resId);
        return !TextUtils.isEmpty(serviceName);
    }

    private void startSystemCaptionsManagerService(Context context, TimingsTraceAndSlog t) {
        if (!deviceHasConfigString(context, 17039892)) {
            Slog.d(TAG, "SystemCaptionsManagerService disabled because resource is not overlaid");
            return;
        }
        t.traceBegin("StartSystemCaptionsManagerService");
        this.mSystemServiceManager.startService(SYSTEM_CAPTIONS_MANAGER_SERVICE_CLASS);
        t.traceEnd();
    }

    private void startContentCaptureService(Context context, TimingsTraceAndSlog t) {
        ActivityManagerService activityManagerService;
        boolean explicitlyEnabled = false;
        String settings = DeviceConfig.getProperty("content_capture", "service_explicitly_enabled");
        if (settings != null && !settings.equalsIgnoreCase(BatteryService.HealthServiceWrapper.INSTANCE_VENDOR)) {
            explicitlyEnabled = Boolean.parseBoolean(settings);
            if (explicitlyEnabled) {
                Slog.d(TAG, "ContentCaptureService explicitly enabled by DeviceConfig");
            } else {
                Slog.d(TAG, "ContentCaptureService explicitly disabled by DeviceConfig");
                return;
            }
        }
        if (!explicitlyEnabled && !deviceHasConfigString(context, 17039882)) {
            Slog.d(TAG, "ContentCaptureService disabled because resource is not overlaid");
            return;
        }
        t.traceBegin("StartContentCaptureService");
        this.mSystemServiceManager.startService(CONTENT_CAPTURE_MANAGER_SERVICE_CLASS);
        ContentCaptureManagerInternal ccmi = (ContentCaptureManagerInternal) LocalServices.getService(ContentCaptureManagerInternal.class);
        if (ccmi != null && (activityManagerService = this.mActivityManagerService) != null) {
            activityManagerService.setContentCaptureManager(ccmi);
        }
        t.traceEnd();
    }

    private void startAttentionService(Context context, TimingsTraceAndSlog t) {
        if (!AttentionManagerService.isServiceConfigured(context)) {
            Slog.d(TAG, "AttentionService is not configured on this device");
            return;
        }
        t.traceBegin("StartAttentionManagerService");
        this.mSystemServiceManager.startService(AttentionManagerService.class);
        t.traceEnd();
    }

    private static void startSystemUi(Context context, WindowManagerService windowManager) {
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        Intent intent = new Intent();
        intent.setComponent(pm.getSystemUiServiceComponent());
        intent.addFlags(256);
        context.startServiceAsUser(intent, UserHandle.SYSTEM);
        windowManager.onSystemUiStarted();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean handleEarlySystemWtf(IBinder app, String tag, boolean system, ApplicationErrorReport.ParcelableCrashInfo crashInfo, int immediateCallerPid) {
        int myPid = Process.myPid();
        com.android.server.am.EventLogTags.writeAmWtf(UserHandle.getUserId(1000), myPid, "system_server", -1, tag, crashInfo.exceptionMessage);
        FrameworkStatsLog.write(80, 1000, tag, "system_server", myPid, 3);
        synchronized (SystemServer.class) {
            if (sPendingWtfs == null) {
                sPendingWtfs = new LinkedList<>();
            }
            sPendingWtfs.add(new Pair<>(tag, crashInfo));
        }
        return false;
    }
}