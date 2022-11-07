package com.android.server;

import android.app.ActivityThread;
import android.app.AppCompatCallbacks;
import android.app.ApplicationErrorReport;
import android.app.ContextImpl;
import android.app.SystemServiceRegistry;
import android.app.admin.DevicePolicySafetyChecker;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.database.sqlite.SQLiteCompatibilityWalFlags;
import android.database.sqlite.SQLiteGlobal;
import android.graphics.Typeface;
import android.hardware.display.DisplayManagerInternal;
import android.net.ConnectivityManager;
import android.net.ConnectivityModuleConnector;
import android.net.NetworkStackClient;
import android.os.BaseBundle;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.FactoryTest;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.IIncidentManager;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.sysprop.VoldProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.IndentingPrintWriter;
import android.util.Pair;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.i18n.timezone.ZoneInfoDb;
import com.android.internal.os.BinderInternal;
import com.android.internal.os.RuntimeInit;
import com.android.internal.policy.AttributeCache;
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
import com.android.server.devicepolicy.DevicePolicyManagerService;
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
import com.android.server.os.NativeTombstoneManagerService;
import com.android.server.pm.DataLoaderManagerService;
import com.android.server.pm.Installer;
import com.android.server.pm.OtaDexoptService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.dex.SystemServerDexLoadReporter;
import com.android.server.pm.verify.domain.DomainVerificationService;
import com.android.server.policy.AppOpsPolicy;
import com.android.server.power.PowerManagerService;
import com.android.server.power.ShutdownThread;
import com.android.server.power.ThermalManagerService;
import com.android.server.power.hint.HintManagerService;
import com.android.server.powerstats.PowerStatsService;
import com.android.server.recoverysystem.RecoverySystemService;
import com.android.server.rotationresolver.RotationResolverManagerService;
import com.android.server.security.FileIntegrityService;
import com.android.server.sensors.SensorService;
import com.android.server.uri.UriGrantsManagerService;
import com.android.server.usage.AppStandbyController;
import com.android.server.utils.TimingsTraceAndSlog;
import com.android.server.webkit.WebViewUpdateService;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowManagerGlobalLock;
import com.android.server.wm.WindowManagerService;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Timer;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import system.ext.loader.core.ExtLoader;
import system.ext.preload.IServicesPreloadExt;

/* loaded from: classes.dex */
public final class SystemServer implements Dumpable {
    private static final String ACCESSIBILITY_MANAGER_SERVICE_CLASS = "com.android.server.accessibility.AccessibilityManagerService$Lifecycle";
    private static final String ACCOUNT_SERVICE_CLASS = "com.android.server.accounts.AccountManagerService$Lifecycle";
    private static final String ADB_SERVICE_CLASS = "com.android.server.adb.AdbService$Lifecycle";
    private static final String ALARM_MANAGER_SERVICE_CLASS = "com.android.server.alarm.AlarmManagerService";
    private static final String APPWIDGET_SERVICE_CLASS = "com.android.server.appwidget.AppWidgetService";
    private static final String APP_HIBERNATION_SERVICE_CLASS = "com.android.server.apphibernation.AppHibernationService";
    private static final String APP_PREDICTION_MANAGER_SERVICE_CLASS = "com.android.server.appprediction.AppPredictionManagerService";
    private static final String APP_SEARCH_MANAGER_SERVICE_CLASS = "com.android.server.appsearch.AppSearchManagerService";
    private static final String AUTO_FILL_MANAGER_SERVICE_CLASS = "com.android.server.autofill.AutofillManagerService";
    private static final String BACKUP_MANAGER_SERVICE_CLASS = "com.android.server.backup.BackupManagerService$Lifecycle";
    private static final String BLOB_STORE_MANAGER_SERVICE_CLASS = "com.android.server.blob.BlobStoreManagerService";
    private static final String BLOCK_MAP_FILE = "/cache/recovery/block.map";
    private static final String CAR_SERVICE_HELPER_SERVICE_CLASS = "com.android.internal.car.CarServiceHelperService";
    private static final String COMPANION_DEVICE_MANAGER_SERVICE_CLASS = "com.android.server.companion.CompanionDeviceManagerService";
    private static final String CONNECTIVITY_SERVICE_APEX_PATH = "/apex/com.android.tethering/javalib/service-connectivity.jar";
    private static final String CONNECTIVITY_SERVICE_INITIALIZER_CLASS = "com.android.server.ConnectivityServiceInitializer";
    private static final String CONTENT_CAPTURE_MANAGER_SERVICE_CLASS = "com.android.server.contentcapture.ContentCaptureManagerService";
    private static final String CONTENT_SERVICE_CLASS = "com.android.server.content.ContentService$Lifecycle";
    private static final String CONTENT_SUGGESTIONS_SERVICE_CLASS = "com.android.server.contentsuggestions.ContentSuggestionsManagerService";
    private static final String DEVICE_IDLE_CONTROLLER_CLASS = "com.android.server.DeviceIdleController";
    private static final String ENCRYPTED_STATE = "1";
    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String ETHERNET_SERVICE_CLASS = "com.android.server.ethernet.EthernetService";
    private static final String GAME_MANAGER_SERVICE_CLASS = "com.android.server.app.GameManagerService$Lifecycle";
    private static final String GNSS_TIME_UPDATE_SERVICE_CLASS = "com.android.server.timedetector.GnssTimeUpdateService$Lifecycle";
    private static final String IOT_SERVICE_CLASS = "com.android.things.server.IoTSystemService";
    private static final String IP_CONNECTIVITY_METRICS_CLASS = "com.android.server.connectivity.IpConnectivityMetrics";
    private static final String JOB_SCHEDULER_SERVICE_CLASS = "com.android.server.job.JobSchedulerService";
    private static final String LOCATION_TIME_ZONE_MANAGER_SERVICE_CLASS = "com.android.server.timezonedetector.location.LocationTimeZoneManagerService$Lifecycle";
    private static final String LOCK_SETTINGS_SERVICE_CLASS = "com.android.server.locksettings.LockSettingsService$Lifecycle";
    private static final String LOWPAN_SERVICE_CLASS = "com.android.server.lowpan.LowpanService";
    private static final int MAX_HEAP_DUMPS = 2;
    private static final String MEDIA_COMMUNICATION_SERVICE_CLASS = "com.android.server.media.MediaCommunicationService";
    private static final String MEDIA_RESOURCE_MONITOR_SERVICE_CLASS = "com.android.server.media.MediaResourceMonitorService";
    private static final String MEDIA_SESSION_SERVICE_CLASS = "com.android.server.media.MediaSessionService";
    private static final String MIDI_SERVICE_CLASS = "com.android.server.midi.MidiService$Lifecycle";
    private static final String MUSIC_RECOGNITION_MANAGER_SERVICE_CLASS = "com.android.server.musicrecognition.MusicRecognitionManagerService";
    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";
    private static final String PRINT_MANAGER_SERVICE_CLASS = "com.android.server.print.PrintManagerService";
    private static final String REBOOT_READINESS_LIFECYCLE_CLASS = "com.android.server.scheduling.RebootReadinessManagerService$Lifecycle";
    private static final String ROLE_SERVICE_CLASS = "com.android.role.RoleService";
    private static final String ROLLBACK_MANAGER_SERVICE_CLASS = "com.android.server.rollback.RollbackManagerService";
    private static final String SCHEDULING_APEX_PATH = "/apex/com.android.scheduling/javalib/service-scheduling.jar";
    private static final String SEARCH_MANAGER_SERVICE_CLASS = "com.android.server.search.SearchManagerService$Lifecycle";
    private static final String SEARCH_UI_MANAGER_SERVICE_CLASS = "com.android.server.searchui.SearchUiManagerService";
    private static final String SLICE_MANAGER_SERVICE_CLASS = "com.android.server.slice.SliceManagerService$Lifecycle";
    private static final long SLOW_DELIVERY_THRESHOLD_MS = 200;
    private static final long SLOW_DISPATCH_THRESHOLD_MS = 100;
    private static final String SMARTSPACE_MANAGER_SERVICE_CLASS = "com.android.server.smartspace.SmartspaceManagerService";
    private static final String SPEECH_RECOGNITION_MANAGER_SERVICE_CLASS = "com.android.server.speech.SpeechRecognitionManagerService";
    private static final String START_BLOB_STORE_SERVICE = "startBlobStoreManagerService";
    private static final String START_HIDL_SERVICES = "StartHidlServices";
    private static final String STATS_COMPANION_APEX_PATH = "/apex/com.android.os.statsd/javalib/service-statsd.jar";
    private static final String STATS_COMPANION_LIFECYCLE_CLASS = "com.android.server.stats.StatsCompanion$Lifecycle";
    private static final String STATS_PULL_ATOM_SERVICE_CLASS = "com.android.server.stats.pull.StatsPullAtomService";
    private static final String STORAGE_MANAGER_SERVICE_CLASS = "com.android.server.StorageManagerService$Lifecycle";
    private static final String STORAGE_STATS_SERVICE_CLASS = "com.android.server.usage.StorageStatsService$Lifecycle";
    private static final String SYSPROP_FDTRACK_ABORT_THRESHOLD = "persist.sys.debug.fdtrack_abort_threshold";
    private static final String SYSPROP_FDTRACK_ENABLE_THRESHOLD = "persist.sys.debug.fdtrack_enable_threshold";
    private static final String SYSPROP_FDTRACK_INTERVAL = "persist.sys.debug.fdtrack_interval";
    private static final String SYSPROP_START_COUNT = "sys.system_server.start_count";
    private static final String SYSPROP_START_ELAPSED = "sys.system_server.start_elapsed";
    private static final String SYSPROP_START_UPTIME = "sys.system_server.start_uptime";
    private static final String SYSTEM_CAPTIONS_MANAGER_SERVICE_CLASS = "com.android.server.systemcaptions.SystemCaptionsManagerService";
    private static final String TAG = "SystemServer";
    private static final String TETHERING_CONNECTOR_CLASS = "android.net.ITetheringConnector";
    private static final String TEXT_TO_SPEECH_MANAGER_SERVICE_CLASS = "com.android.server.texttospeech.TextToSpeechManagerService";
    private static final String THERMAL_OBSERVER_CLASS = "com.google.android.clockwork.ThermalObserver";
    private static final String TIME_DETECTOR_SERVICE_CLASS = "com.android.server.timedetector.TimeDetectorService$Lifecycle";
    private static final String TIME_ZONE_DETECTOR_SERVICE_CLASS = "com.android.server.timezonedetector.TimeZoneDetectorService$Lifecycle";
    private static final String TIME_ZONE_RULES_MANAGER_SERVICE_CLASS = "com.android.server.timezone.RulesManagerService$Lifecycle";
    private static final String TRANSLATION_MANAGER_SERVICE_CLASS = "com.android.server.translation.TranslationManagerService";
    private static final String UNCRYPT_PACKAGE_FILE = "/cache/recovery/uncrypt_file";
    private static final String USB_SERVICE_CLASS = "com.android.server.usb.UsbService$Lifecycle";
    private static final String UWB_SERVICE_CLASS = "com.android.server.uwb.UwbService";
    private static final String VOICE_RECOGNITION_MANAGER_SERVICE_CLASS = "com.android.server.voiceinteraction.VoiceInteractionManagerService";
    private static final String WALLPAPER_SERVICE_CLASS = "com.android.server.wallpaper.WallpaperManagerService$Lifecycle";
    private static final String WEAR_CONNECTIVITY_SERVICE_CLASS = "com.android.clockwork.connectivity.WearConnectivityService";
    private static final String WEAR_DISPLAY_SERVICE_CLASS = "com.google.android.clockwork.display.WearDisplayService";
    private static final String WEAR_GLOBAL_ACTIONS_SERVICE_CLASS = "com.android.clockwork.globalactions.GlobalActionsService";
    private static final String WEAR_LEFTY_SERVICE_CLASS = "com.google.android.clockwork.lefty.WearLeftyService";
    private static final String WEAR_POWER_SERVICE_CLASS = "com.android.clockwork.power.WearPowerService";
    private static final String WEAR_SIDEKICK_SERVICE_CLASS = "com.google.android.clockwork.sidekick.SidekickService";
    private static final String WEAR_TIME_SERVICE_CLASS = "com.google.android.clockwork.time.WearTimeService";
    private static final String WIFI_APEX_SERVICE_JAR_PATH = "/apex/com.android.wifi/javalib/service-wifi.jar";
    private static final String WIFI_AWARE_SERVICE_CLASS = "com.android.server.wifi.aware.WifiAwareService";
    private static final String WIFI_P2P_SERVICE_CLASS = "com.android.server.wifi.p2p.WifiP2pService";
    private static final String WIFI_RTT_SERVICE_CLASS = "com.android.server.wifi.rtt.RttService";
    private static final String WIFI_SCANNING_SERVICE_CLASS = "com.android.server.wifi.scanner.WifiScanningService";
    private static final String WIFI_SERVICE_CLASS = "com.android.server.wifi.WifiService";
    private static final int sMaxBinderThreads = 31;
    private static LinkedList<Pair<String, ApplicationErrorReport.CrashInfo>> sPendingWtfs;
    private ActivityManagerService mActivityManagerService;
    private Future<?> mBlobStoreServiceStart;
    private ContentResolver mContentResolver;
    private DataLoaderManagerService mDataLoaderManagerService;
    private DisplayManagerService mDisplayManagerService;
    private EntropyMixer mEntropyMixer;
    private boolean mFirstBoot;
    private boolean mOnlyCore;
    private PackageManager mPackageManager;
    private PackageManagerService mPackageManagerService;
    private PowerManagerService mPowerManagerService;
    private Timer mProfilerSnapshotTimer;
    private final boolean mRuntimeRestart;
    private final long mRuntimeStartElapsedTime;
    private final long mRuntimeStartUptime;
    private Context mSystemContext;
    private SystemServiceManager mSystemServiceManager;
    private WebViewUpdateService mWebViewUpdateService;
    private WindowManagerGlobalLock mWindowManagerGlobalLock;
    private Future<?> mZygotePreload;
    private static final int DEFAULT_SYSTEM_THEME = ((Integer) SystemServerExtPlugin.getSystemThemeStyle.call(new Object[0])).intValue();
    private static final File HEAP_DUMP_PATH = new File("/data/system/heapdump/");
    private ISystemServerExt mSystemServerExt = (ISystemServerExt) SystemServerExtPlugin.constructor.newInstance();
    private long mIncrementalServiceHandle = 0;
    private final SystemServerDumper mDumper = new SystemServerDumper();
    private IServicesPreloadExt mServicesPreloadExt = (IServicesPreloadExt) ExtLoader.type(IServicesPreloadExt.class).base(this).create();
    private final int mFactoryTestMode = FactoryTest.getMode();
    private final int mStartCount = SystemProperties.getInt(SYSPROP_START_COUNT, 0) + 1;

    private static native void fdtrackAbort();

    private static native void initZygoteChildHeapProfiling();

    private static native void setIncrementalServiceSystemReady(long j);

    private static native void startHidlServices();

    private static native void startIStatsService();

    private static native long startIncrementalService();

    private static native void startMemtrackProxyService();

    private static int getMaxFd() {
        FileDescriptor fd = null;
        try {
            try {
                fd = Os.open("/dev/null", OsConstants.O_RDONLY | OsConstants.O_CLOEXEC, 0);
                int int$ = fd.getInt$();
                if (fd != null) {
                    try {
                        Os.close(fd);
                    } catch (ErrnoException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return int$;
            } catch (ErrnoException ex2) {
                Slog.e("System", "Failed to get maximum fd: " + ex2);
                if (fd != null) {
                    try {
                        Os.close(fd);
                        return Integer.MAX_VALUE;
                    } catch (ErrnoException ex3) {
                        throw new RuntimeException(ex3);
                    }
                }
                return Integer.MAX_VALUE;
            }
        } catch (Throwable ex4) {
            if (fd != null) {
                try {
                    Os.close(fd);
                } catch (ErrnoException ex5) {
                    throw new RuntimeException(ex5);
                }
            }
            throw ex4;
        }
    }

    private static void dumpHprof() {
        File[] listFiles;
        TreeSet<File> existingTombstones = new TreeSet<>();
        for (File file : HEAP_DUMP_PATH.listFiles()) {
            if (file.isFile() && file.getName().startsWith("fdtrack-")) {
                existingTombstones.add(file);
            }
        }
        if (existingTombstones.size() >= 2) {
            for (int i = 0; i < 1; i++) {
                existingTombstones.pollLast();
            }
            Iterator<File> it = existingTombstones.iterator();
            while (it.hasNext()) {
                File file2 = it.next();
                if (!file2.delete()) {
                    Slog.w("System", "Failed to clean up hprof " + file2);
                }
            }
        }
        try {
            String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
            String filename = "/data/system/heapdump/fdtrack-" + date + ".hprof";
            Debug.dumpHprofData(filename);
        } catch (IOException ex) {
            Slog.e("System", "Failed to dump fdtrack hprof", ex);
        }
    }

    private static void spawnFdLeakCheckThread() {
        final int enableThreshold = SystemProperties.getInt(SYSPROP_FDTRACK_ENABLE_THRESHOLD, 1024);
        final int abortThreshold = SystemProperties.getInt(SYSPROP_FDTRACK_ABORT_THRESHOLD, 2048);
        final int checkInterval = SystemProperties.getInt(SYSPROP_FDTRACK_INTERVAL, 120);
        new Thread(new Runnable() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                SystemServer.lambda$spawnFdLeakCheckThread$0(enableThreshold, abortThreshold, checkInterval);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$spawnFdLeakCheckThread$0(int enableThreshold, int abortThreshold, int checkInterval) {
        boolean enabled = false;
        long nextWrite = 0;
        while (true) {
            int maxFd = getMaxFd();
            if (maxFd > enableThreshold) {
                System.gc();
                System.runFinalization();
                maxFd = getMaxFd();
            }
            int i = 2;
            if (maxFd > enableThreshold && !enabled) {
                Slog.i("System", "fdtrack enable threshold reached, enabling");
                FrameworkStatsLog.write(364, 2, maxFd);
                System.loadLibrary("fdtrack");
                enabled = true;
            } else if (maxFd > abortThreshold) {
                Slog.i("System", "fdtrack abort threshold reached, dumping and aborting");
                FrameworkStatsLog.write(364, 3, maxFd);
                dumpHprof();
                fdtrackAbort();
            } else {
                long now = SystemClock.elapsedRealtime();
                if (now > nextWrite) {
                    long nextWrite2 = AppStandbyController.ConstantsObserver.DEFAULT_STRONG_USAGE_TIMEOUT + now;
                    if (!enabled) {
                        i = 1;
                    }
                    FrameworkStatsLog.write(364, i, maxFd);
                    nextWrite = nextWrite2;
                }
            }
            try {
                Thread.sleep(checkInterval * 1000);
            } catch (InterruptedException e) {
            }
        }
    }

    public static void main(String[] args) {
        new SystemServer().run();
    }

    public SystemServer() {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        this.mRuntimeStartElapsedTime = elapsedRealtime;
        long uptimeMillis = SystemClock.uptimeMillis();
        this.mRuntimeStartUptime = uptimeMillis;
        Process.setStartTimes(elapsedRealtime, uptimeMillis);
        this.mRuntimeRestart = "1".equals(SystemProperties.get("sys.boot_completed"));
        this.mServicesPreloadExt.preload(getClass().getClassLoader());
    }

    @Override // com.android.server.Dumpable
    public void dump(IndentingPrintWriter pw, String[] args) {
        pw.printf("Runtime restart: %b\n", new Object[]{Boolean.valueOf(this.mRuntimeRestart)});
        pw.printf("Start count: %d\n", new Object[]{Integer.valueOf(this.mStartCount)});
        pw.print("Runtime start-up time: ");
        TimeUtils.formatDuration(this.mRuntimeStartUptime, pw);
        pw.println();
        pw.print("Runtime start-elapsed time: ");
        TimeUtils.formatDuration(this.mRuntimeStartElapsedTime, pw);
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SystemServerDumper extends Binder {
        private final ArrayMap<String, Dumpable> mDumpables;

        private SystemServerDumper() {
            this.mDumpables = new ArrayMap<>(4);
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            boolean hasArgs = args != null && args.length > 0;
            synchronized (this.mDumpables) {
                if (hasArgs) {
                    try {
                        if ("--list".equals(args[0])) {
                            int dumpablesSize = this.mDumpables.size();
                            for (int i = 0; i < dumpablesSize; i++) {
                                pw.println(this.mDumpables.keyAt(i));
                            }
                            return;
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                if (hasArgs && "--name".equals(args[0])) {
                    if (args.length < 2) {
                        pw.println("Must pass at least one argument to --name");
                        return;
                    }
                    String name = args[1];
                    Dumpable dumpable = this.mDumpables.get(name);
                    if (dumpable == null) {
                        pw.printf("No dummpable named %s\n", name);
                        return;
                    }
                    IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
                    String[] actualArgs = (String[]) Arrays.copyOfRange(args, 2, args.length);
                    dumpable.dump(ipw, actualArgs);
                    ipw.close();
                    return;
                }
                int dumpablesSize2 = this.mDumpables.size();
                IndentingPrintWriter ipw2 = new IndentingPrintWriter(pw, "  ");
                for (int i2 = 0; i2 < dumpablesSize2; i2++) {
                    Dumpable dumpable2 = this.mDumpables.valueAt(i2);
                    ipw2.printf("%s:\n", new Object[]{dumpable2.getDumpableName()});
                    ipw2.increaseIndent();
                    dumpable2.dump(ipw2, args);
                    ipw2.decreaseIndent();
                    ipw2.println();
                }
                ipw2.close();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addDumpable(Dumpable dumpable) {
            synchronized (this.mDumpables) {
                this.mDumpables.put(dumpable.getDumpableName(), dumpable);
            }
        }
    }

    private void run() {
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        try {
            t.traceBegin("InitBeforeStartServices");
            SystemProperties.set(SYSPROP_START_COUNT, String.valueOf(this.mStartCount));
            SystemProperties.set(SYSPROP_START_ELAPSED, String.valueOf(this.mRuntimeStartElapsedTime));
            SystemProperties.set(SYSPROP_START_UPTIME, String.valueOf(this.mRuntimeStartUptime));
            EventLog.writeEvent((int) EventLogTags.SYSTEM_SERVER_START, Integer.valueOf(this.mStartCount), Long.valueOf(this.mRuntimeStartUptime), Long.valueOf(this.mRuntimeStartElapsedTime));
            String timezoneProperty = SystemProperties.get("persist.sys.timezone");
            if (!isValidTimeZoneId(timezoneProperty)) {
                Slog.w(TAG, "persist.sys.timezone is not valid (" + timezoneProperty + "); setting to GMT.");
                SystemProperties.set("persist.sys.timezone", "GMT");
            }
            if (!SystemProperties.get("persist.sys.language").isEmpty()) {
                String languageTag = Locale.getDefault().toLanguageTag();
                SystemProperties.set("persist.sys.locale", languageTag);
                SystemProperties.set("persist.sys.language", "");
                SystemProperties.set("persist.sys.country", "");
                SystemProperties.set("persist.sys.localevar", "");
            }
            Binder.setWarnOnBlocking(true);
            PackageItemInfo.forceSafeLabels();
            SQLiteGlobal.sDefaultSyncMode = "FULL";
            SQLiteCompatibilityWalFlags.init((String) null);
            Slog.i(TAG, "Entered the Android system server!");
            long uptimeMillis = SystemClock.elapsedRealtime();
            EventLog.writeEvent((int) EventLogTags.BOOT_PROGRESS_SYSTEM_RUN, uptimeMillis);
            if (!this.mRuntimeRestart) {
                FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ELAPSED_TIME_REPORTED, 19, uptimeMillis);
            }
            this.mSystemServerExt.setBootstage(true);
            SystemProperties.set("persist.sys.dalvik.vm.lib.2", VMRuntime.getRuntime().vmLibrary());
            VMRuntime.getRuntime().clearGrowthLimit();
            Build.ensureFingerprintProperty();
            Environment.setUserRequired(true);
            BaseBundle.setShouldDefuse(true);
            Parcel.setStackTraceParceling(true);
            BinderInternal.disableBackgroundScheduling(true);
            BinderInternal.setMaxThreads(31);
            Process.setCanSelfBackground(false);
            Looper.prepareMainLooper();
            Looper.getMainLooper().setSlowLogThresholdMs(SLOW_DISPATCH_THRESHOLD_MS, SLOW_DELIVERY_THRESHOLD_MS);
            SystemServiceRegistry.sEnableServiceNotFoundWtf = true;
            System.loadLibrary("android_servers");
            initZygoteChildHeapProfiling();
            performPendingShutdown();
            createSystemContext();
            ActivityThread.initializeMainlineModules();
            ServiceManager.addService("system_server_dumper", this.mDumper);
            this.mDumper.addDumpable(this);
            SystemServiceManager systemServiceManager = new SystemServiceManager(this.mSystemContext);
            this.mSystemServiceManager = systemServiceManager;
            systemServiceManager.setStartInfo(this.mRuntimeRestart, this.mRuntimeStartElapsedTime, this.mRuntimeStartUptime);
            this.mDumper.addDumpable(this.mSystemServiceManager);
            LocalServices.addService(SystemServiceManager.class, this.mSystemServiceManager);
            SystemServerInitThreadPool tp = SystemServerInitThreadPool.start();
            this.mDumper.addDumpable(tp);
            this.mSystemServerExt.initFontsForserializeFontMap();
            Typeface.loadPreinstalledSystemFontMap();
            if (Build.IS_DEBUGGABLE) {
                String jvmtiAgent = SystemProperties.get("persist.sys.dalvik.jvmtiagent");
                if (!jvmtiAgent.isEmpty()) {
                    int equalIndex = jvmtiAgent.indexOf(61);
                    String libraryPath = jvmtiAgent.substring(0, equalIndex);
                    String parameterList = jvmtiAgent.substring(equalIndex + 1, jvmtiAgent.length());
                    try {
                        Debug.attachJvmtiAgent(libraryPath, parameterList, null);
                    } catch (Exception e) {
                        Slog.e("System", "*************************************************");
                        Slog.e("System", "********** Failed to load jvmti plugin: " + jvmtiAgent);
                    }
                }
            }
            t.traceEnd();
            this.mSystemServerExt.initSystemServer(this.mSystemContext);
            RuntimeInit.setDefaultApplicationWtfHandler(SystemServer$$ExternalSyntheticLambda1.INSTANCE);
            try {
                t.traceBegin("StartServices");
                startBootstrapServices(t);
                startCoreServices(t);
                startOtherServices(t);
                t.traceEnd();
                StrictMode.initVmDefaults(null);
                if (!this.mRuntimeRestart && !isFirstBootOrUpgrade()) {
                    long uptimeMillis2 = SystemClock.elapsedRealtime();
                    FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ELAPSED_TIME_REPORTED, 20, uptimeMillis2);
                    if (uptimeMillis2 > 60000) {
                        Slog.wtf(TimingsTraceAndSlog.SYSTEM_SERVER_TIMING_TAG, "SystemServer init took too long. uptimeMillis=" + uptimeMillis2);
                    }
                }
                this.mSystemServerExt.setBootstage(false);
                Process.setThreadPriority(-2);
                Looper.loop();
                throw new RuntimeException("Main thread loop unexpectedly exited");
            } finally {
            }
        } finally {
        }
    }

    private static boolean isValidTimeZoneId(String timezoneProperty) {
        return (timezoneProperty == null || timezoneProperty.isEmpty() || !ZoneInfoDb.getInstance().hasTimeZone(timezoneProperty)) ? false : true;
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
        int i = DEFAULT_SYSTEM_THEME;
        systemContext.setTheme(i);
        activityThread.getSystemUiContext().setTheme(i);
    }

    /* JADX WARN: Type inference failed for: r0v6, types: [com.android.server.compat.PlatformCompat, android.os.IBinder] */
    private void startBootstrapServices(TimingsTraceAndSlog t) {
        Installer installer;
        Context context;
        t.traceBegin("startBootstrapServices");
        this.mSystemServerExt.setDataNormalizationManager();
        t.traceBegin("StartWatchdog");
        Watchdog watchdog = Watchdog.getInstance();
        watchdog.start();
        t.traceEnd();
        Slog.i(TAG, "Reading configuration...");
        t.traceBegin("ReadingSystemConfig");
        SystemServerInitThreadPool.submit(SystemServer$$ExternalSyntheticLambda6.INSTANCE, "ReadingSystemConfig");
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
        t.traceBegin("StartInstaller");
        Installer installer2 = (Installer) this.mSystemServiceManager.startService(Installer.class);
        t.traceEnd();
        t.traceBegin("DeviceIdentifiersPolicyService");
        this.mSystemServiceManager.startService(DeviceIdentifiersPolicyService.class);
        t.traceEnd();
        t.traceBegin("UriGrantsManagerService");
        this.mSystemServiceManager.startService(UriGrantsManagerService.Lifecycle.class);
        t.traceEnd();
        t.traceBegin("StartPowerStatsService");
        this.mSystemServiceManager.startService(PowerStatsService.class);
        t.traceEnd();
        this.mSystemServerExt.addOplusDevicePolicyService();
        t.traceBegin("StartIStatsService");
        startIStatsService();
        t.traceEnd();
        t.traceBegin("MemtrackProxyService");
        startMemtrackProxyService();
        t.traceEnd();
        t.traceBegin("StartActivityManager");
        ActivityTaskManagerService atm = ((ActivityTaskManagerService.Lifecycle) this.mSystemServiceManager.startService(ActivityTaskManagerService.Lifecycle.class)).getService();
        Slog.i(TAG, "Ams Service");
        ActivityManagerService startService = ActivityManagerService.Lifecycle.startService(this.mSystemServiceManager, atm);
        this.mActivityManagerService = startService;
        startService.setSystemServiceManager(this.mSystemServiceManager);
        this.mActivityManagerService.setInstaller(installer2);
        this.mWindowManagerGlobalLock = atm.getGlobalLock();
        t.traceEnd();
        t.traceBegin("StartDataLoaderManagerService");
        this.mDataLoaderManagerService = (DataLoaderManagerService) this.mSystemServiceManager.startService(DataLoaderManagerService.class);
        t.traceEnd();
        t.traceBegin("StartIncrementalService");
        this.mIncrementalServiceHandle = startIncrementalService();
        t.traceEnd();
        t.traceBegin("StartPowerManager");
        Slog.i(TAG, "Power Service");
        this.mPowerManagerService = (PowerManagerService) this.mSystemServiceManager.startService(PowerManagerService.class);
        t.traceEnd();
        t.traceBegin("StartThermalManager");
        this.mSystemServiceManager.startService(ThermalManagerService.class);
        t.traceEnd();
        t.traceBegin("StartHintManager");
        this.mSystemServiceManager.startService(HintManagerService.class);
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
        Slog.i(TAG, "DisplayManager Service");
        t.traceBegin("StartDisplayManager");
        this.mDisplayManagerService = (DisplayManagerService) this.mSystemServiceManager.startService(DisplayManagerService.class);
        t.traceEnd();
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
        if (this.mRuntimeRestart) {
            installer = installer2;
        } else {
            installer = installer2;
            FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ELAPSED_TIME_REPORTED, 14, SystemClock.elapsedRealtime());
        }
        this.mSystemServerExt.waitForFutureNoInterrupt();
        t.traceBegin("StartDomainVerificationService");
        DomainVerificationService domainVerificationService = new DomainVerificationService(this.mSystemContext, SystemConfig.getInstance(), platformCompat);
        this.mSystemServiceManager.startService(domainVerificationService);
        t.traceEnd();
        t.traceBegin("StartPackageManagerService");
        try {
            Watchdog.getInstance().pauseWatchingCurrentThread("packagemanagermain");
            context = this.mSystemContext;
            if (this.mFactoryTestMode == 0) {
                z = false;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            this.mPackageManagerService = PackageManagerService.main(context, installer, domainVerificationService, z, this.mOnlyCore);
            Watchdog.getInstance().resumeWatchingCurrentThread("packagemanagermain");
            SystemServerDexLoadReporter.configureSystemServerDexReporter(this.mPackageManagerService);
            this.mFirstBoot = this.mPackageManagerService.isFirstBoot();
            this.mPackageManager = this.mSystemContext.getPackageManager();
            t.traceEnd();
            if (!this.mRuntimeRestart && !isFirstBootOrUpgrade()) {
                FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ELAPSED_TIME_REPORTED, 15, SystemClock.elapsedRealtime());
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
            platformCompat.registerPackageReceiver(this.mSystemContext);
            t.traceBegin("InitWatchdog");
            watchdog.init(this.mSystemContext, this.mActivityManagerService);
            t.traceEnd();
            this.mDisplayManagerService.setupSchedulerPolicies();
            t.traceBegin("StartOverlayManagerService");
            this.mSystemServiceManager.startService(new OverlayManagerService(this.mSystemContext));
            t.traceEnd();
            Slog.i(TAG, "Sensor Service");
            t.traceBegin("StartSensorPrivacyService");
            this.mSystemServiceManager.startService(new SensorPrivacyService(this.mSystemContext));
            t.traceEnd();
            if (SystemProperties.getInt("persist.sys.displayinset.top", 0) > 0) {
                this.mActivityManagerService.updateSystemUiContext();
                ((DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class)).onOverlayChanged();
            }
            t.traceBegin("StartSensorService");
            this.mSystemServiceManager.startService(SensorService.class);
            t.traceEnd();
            t.traceEnd();
            this.mSystemServerExt.startBootstrapServices();
        } catch (Throwable th2) {
            th = th2;
            Watchdog.getInstance().resumeWatchingCurrentThread("packagemanagermain");
            throw th;
        }
    }

    private void startCoreServices(TimingsTraceAndSlog t) {
        t.traceBegin("startCoreServices");
        t.traceBegin("StartSystemConfigService");
        this.mSystemServiceManager.startService(SystemConfigService.class);
        t.traceEnd();
        Slog.i(TAG, "Battery Service");
        t.traceBegin("StartBatteryService");
        this.mSystemServiceManager.startService(BatteryService.class);
        t.traceEnd();
        Slog.i(TAG, "UsageStats Service");
        t.traceBegin("StartUsageService");
        this.mSystemServerExt.startUsageStatsService(this.mSystemServiceManager);
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
        t.traceBegin("StartNativeTombstoneManagerService");
        this.mSystemServiceManager.startService(NativeTombstoneManagerService.class);
        t.traceEnd();
        t.traceBegin("StartBugreportManagerService");
        this.mSystemServiceManager.startService(BugreportManagerService.class);
        t.traceEnd();
        t.traceBegin(GpuService.TAG);
        this.mSystemServiceManager.startService(GpuService.class);
        t.traceEnd();
        t.traceEnd();
        this.mSystemServerExt.startCoreServices();
    }

    /* JADX WARN: Can't wrap try/catch for region: R(171:213|(3:214|215|216)|217|(1:219)|220|(1:623)|224|(5:226|227|228|229|230)(1:622)|231|(1:233)(1:618)|234|(1:236)(1:617)|237|(1:239)(1:616)|240|(2:241|242)|243|(2:244|245)|246|(2:247|248)|249|(2:250|251)|252|(1:254)|255|(3:256|257|258)|(3:259|260|261)|262|263|264|265|266|267|268|269|(1:271)(1:591)|272|(1:274)|275|(1:277)|278|(1:280)|281|(1:283)|284|(1:590)|288|289|290|291|292|293|294|295|296|297|298|299|300|301|(5:567|568|569|570|571)(1:303)|304|305|306|307|308|309|310|311|312|313|314|315|316|317|318|319|320|321|322|323|324|325|326|327|328|329|(4:331|332|333|334)|(4:339|340|341|342)|346|(1:348)(1:542)|349|(1:351)(3:536|537|538)|352|(1:354)|355|(1:357)|358|(2:359|360)|361|(1:363)|364|365|366|367|(1:529)|(7:373|374|375|376|377|378|379)|386|387|388|389|390|391|392|393|(1:395)|396|(1:398)|399|(1:522)|403|(1:405)|406|(1:408)|409|410|411|412|413|414|415|(1:515)(1:419)|420|(1:422)|(7:425|426|427|428|429|430|431)|438|439|440|441|(1:443)|444|(1:446)|447|(1:449)|450|(1:452)|453|(1:511)|457|(1:459)|460|(1:462)|463|(1:465)|466|467|468|469|470|471|472|473|(1:475)(1:504)|(1:477)|(1:479)|480|(2:481|482)|483|(4:485|486|487|488)|(4:493|494|495|496)|500) */
    /* JADX WARN: Can't wrap try/catch for region: R(172:213|(3:214|215|216)|217|(1:219)|220|(1:623)|224|(5:226|227|228|229|230)(1:622)|231|(1:233)(1:618)|234|(1:236)(1:617)|237|(1:239)(1:616)|240|(2:241|242)|243|244|245|246|(2:247|248)|249|(2:250|251)|252|(1:254)|255|(3:256|257|258)|(3:259|260|261)|262|263|264|265|266|267|268|269|(1:271)(1:591)|272|(1:274)|275|(1:277)|278|(1:280)|281|(1:283)|284|(1:590)|288|289|290|291|292|293|294|295|296|297|298|299|300|301|(5:567|568|569|570|571)(1:303)|304|305|306|307|308|309|310|311|312|313|314|315|316|317|318|319|320|321|322|323|324|325|326|327|328|329|(4:331|332|333|334)|(4:339|340|341|342)|346|(1:348)(1:542)|349|(1:351)(3:536|537|538)|352|(1:354)|355|(1:357)|358|(2:359|360)|361|(1:363)|364|365|366|367|(1:529)|(7:373|374|375|376|377|378|379)|386|387|388|389|390|391|392|393|(1:395)|396|(1:398)|399|(1:522)|403|(1:405)|406|(1:408)|409|410|411|412|413|414|415|(1:515)(1:419)|420|(1:422)|(7:425|426|427|428|429|430|431)|438|439|440|441|(1:443)|444|(1:446)|447|(1:449)|450|(1:452)|453|(1:511)|457|(1:459)|460|(1:462)|463|(1:465)|466|467|468|469|470|471|472|473|(1:475)(1:504)|(1:477)|(1:479)|480|(2:481|482)|483|(4:485|486|487|488)|(4:493|494|495|496)|500) */
    /* JADX WARN: Can't wrap try/catch for region: R(173:213|(3:214|215|216)|217|(1:219)|220|(1:623)|224|(5:226|227|228|229|230)(1:622)|231|(1:233)(1:618)|234|(1:236)(1:617)|237|(1:239)(1:616)|240|241|242|243|244|245|246|(2:247|248)|249|(2:250|251)|252|(1:254)|255|(3:256|257|258)|(3:259|260|261)|262|263|264|265|266|267|268|269|(1:271)(1:591)|272|(1:274)|275|(1:277)|278|(1:280)|281|(1:283)|284|(1:590)|288|289|290|291|292|293|294|295|296|297|298|299|300|301|(5:567|568|569|570|571)(1:303)|304|305|306|307|308|309|310|311|312|313|314|315|316|317|318|319|320|321|322|323|324|325|326|327|328|329|(4:331|332|333|334)|(4:339|340|341|342)|346|(1:348)(1:542)|349|(1:351)(3:536|537|538)|352|(1:354)|355|(1:357)|358|(2:359|360)|361|(1:363)|364|365|366|367|(1:529)|(7:373|374|375|376|377|378|379)|386|387|388|389|390|391|392|393|(1:395)|396|(1:398)|399|(1:522)|403|(1:405)|406|(1:408)|409|410|411|412|413|414|415|(1:515)(1:419)|420|(1:422)|(7:425|426|427|428|429|430|431)|438|439|440|441|(1:443)|444|(1:446)|447|(1:449)|450|(1:452)|453|(1:511)|457|(1:459)|460|(1:462)|463|(1:465)|466|467|468|469|470|471|472|473|(1:475)(1:504)|(1:477)|(1:479)|480|(2:481|482)|483|(4:485|486|487|488)|(4:493|494|495|496)|500) */
    /* JADX WARN: Can't wrap try/catch for region: R(174:213|(3:214|215|216)|217|(1:219)|220|(1:623)|224|(5:226|227|228|229|230)(1:622)|231|(1:233)(1:618)|234|(1:236)(1:617)|237|(1:239)(1:616)|240|241|242|243|244|245|246|(2:247|248)|249|250|251|252|(1:254)|255|(3:256|257|258)|(3:259|260|261)|262|263|264|265|266|267|268|269|(1:271)(1:591)|272|(1:274)|275|(1:277)|278|(1:280)|281|(1:283)|284|(1:590)|288|289|290|291|292|293|294|295|296|297|298|299|300|301|(5:567|568|569|570|571)(1:303)|304|305|306|307|308|309|310|311|312|313|314|315|316|317|318|319|320|321|322|323|324|325|326|327|328|329|(4:331|332|333|334)|(4:339|340|341|342)|346|(1:348)(1:542)|349|(1:351)(3:536|537|538)|352|(1:354)|355|(1:357)|358|(2:359|360)|361|(1:363)|364|365|366|367|(1:529)|(7:373|374|375|376|377|378|379)|386|387|388|389|390|391|392|393|(1:395)|396|(1:398)|399|(1:522)|403|(1:405)|406|(1:408)|409|410|411|412|413|414|415|(1:515)(1:419)|420|(1:422)|(7:425|426|427|428|429|430|431)|438|439|440|441|(1:443)|444|(1:446)|447|(1:449)|450|(1:452)|453|(1:511)|457|(1:459)|460|(1:462)|463|(1:465)|466|467|468|469|470|471|472|473|(1:475)(1:504)|(1:477)|(1:479)|480|(2:481|482)|483|(4:485|486|487|488)|(4:493|494|495|496)|500) */
    /* JADX WARN: Can't wrap try/catch for region: R(176:213|214|215|216|217|(1:219)|220|(1:623)|224|(5:226|227|228|229|230)(1:622)|231|(1:233)(1:618)|234|(1:236)(1:617)|237|(1:239)(1:616)|240|241|242|243|244|245|246|(2:247|248)|249|250|251|252|(1:254)|255|(3:256|257|258)|(3:259|260|261)|262|263|264|265|266|267|268|269|(1:271)(1:591)|272|(1:274)|275|(1:277)|278|(1:280)|281|(1:283)|284|(1:590)|288|289|290|291|292|293|294|295|296|297|298|299|300|301|(5:567|568|569|570|571)(1:303)|304|305|306|307|308|309|310|311|312|313|314|315|316|317|318|319|320|321|322|323|324|325|326|327|328|329|(4:331|332|333|334)|(4:339|340|341|342)|346|(1:348)(1:542)|349|(1:351)(3:536|537|538)|352|(1:354)|355|(1:357)|358|(2:359|360)|361|(1:363)|364|365|366|367|(1:529)|(7:373|374|375|376|377|378|379)|386|387|388|389|390|391|392|393|(1:395)|396|(1:398)|399|(1:522)|403|(1:405)|406|(1:408)|409|410|411|412|413|414|415|(1:515)(1:419)|420|(1:422)|(7:425|426|427|428|429|430|431)|438|439|440|441|(1:443)|444|(1:446)|447|(1:449)|450|(1:452)|453|(1:511)|457|(1:459)|460|(1:462)|463|(1:465)|466|467|468|469|470|471|472|473|(1:475)(1:504)|(1:477)|(1:479)|480|(2:481|482)|483|(4:485|486|487|488)|(4:493|494|495|496)|500) */
    /* JADX WARN: Can't wrap try/catch for region: R(177:213|214|215|216|217|(1:219)|220|(1:623)|224|(5:226|227|228|229|230)(1:622)|231|(1:233)(1:618)|234|(1:236)(1:617)|237|(1:239)(1:616)|240|241|242|243|244|245|246|247|248|249|250|251|252|(1:254)|255|(3:256|257|258)|(3:259|260|261)|262|263|264|265|266|267|268|269|(1:271)(1:591)|272|(1:274)|275|(1:277)|278|(1:280)|281|(1:283)|284|(1:590)|288|289|290|291|292|293|294|295|296|297|298|299|300|301|(5:567|568|569|570|571)(1:303)|304|305|306|307|308|309|310|311|312|313|314|315|316|317|318|319|320|321|322|323|324|325|326|327|328|329|(4:331|332|333|334)|(4:339|340|341|342)|346|(1:348)(1:542)|349|(1:351)(3:536|537|538)|352|(1:354)|355|(1:357)|358|(2:359|360)|361|(1:363)|364|365|366|367|(1:529)|(7:373|374|375|376|377|378|379)|386|387|388|389|390|391|392|393|(1:395)|396|(1:398)|399|(1:522)|403|(1:405)|406|(1:408)|409|410|411|412|413|414|415|(1:515)(1:419)|420|(1:422)|(7:425|426|427|428|429|430|431)|438|439|440|441|(1:443)|444|(1:446)|447|(1:449)|450|(1:452)|453|(1:511)|457|(1:459)|460|(1:462)|463|(1:465)|466|467|468|469|470|471|472|473|(1:475)(1:504)|(1:477)|(1:479)|480|(2:481|482)|483|(4:485|486|487|488)|(4:493|494|495|496)|500) */
    /* JADX WARN: Code restructure failed: missing block: B:186:0x06ce, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:187:0x06cf, code lost:
        r17 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:188:0x06d2, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:189:0x06d3, code lost:
        reportWtf("starting NetworkPolicy Service", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:216:0x07b8, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:217:0x07b9, code lost:
        r24 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:218:0x07bc, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:219:0x07bd, code lost:
        reportWtf("starting PacProxyService", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:223:0x07eb, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:224:0x07ec, code lost:
        reportWtf("starting VPN Manager Service", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:228:0x0806, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:229:0x0807, code lost:
        reportWtf("starting VCN Management Service", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:247:0x08b2, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:248:0x08b3, code lost:
        r18 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:249:0x08b6, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:250:0x08b7, code lost:
        reportWtf("starting Service Discovery Service", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:254:0x08d1, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:255:0x08d2, code lost:
        reportWtf("starting SystemUpdateManagerService", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:259:0x08ec, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:260:0x08ed, code lost:
        reportWtf("starting UpdateLockService", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:264:0x0945, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:265:0x0946, code lost:
        reportWtf("starting Country Detector", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:269:0x095c, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:270:0x095d, code lost:
        reportWtf("starting TimeDetectorService service", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:274:0x0973, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:275:0x0974, code lost:
        reportWtf("starting TimeZoneDetectorService service", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:279:0x098a, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:280:0x098b, code lost:
        reportWtf("starting LocationTimeZoneManagerService service", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:326:0x0ad2, code lost:
        android.util.Slog.e(com.android.server.SystemServer.TAG, "Failure starting AdbService");
     */
    /* JADX WARN: Code restructure failed: missing block: B:349:0x0b3b, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:350:0x0b3c, code lost:
        r23 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:351:0x0b3f, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:352:0x0b40, code lost:
        android.util.Slog.e(com.android.server.SystemServer.TAG, "Failure starting HardwarePropertiesManagerService", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:373:0x0c4b, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:374:0x0c4c, code lost:
        reportWtf("starting DiskStats Service", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:378:0x0c66, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:379:0x0c67, code lost:
        reportWtf("starting RuntimeService", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:404:0x0cce, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:405:0x0ccf, code lost:
        reportWtf("starting CertBlacklister", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:437:0x0e19, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:438:0x0e1a, code lost:
        r36 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:439:0x0e1d, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:440:0x0e1e, code lost:
        reportWtf("starting MediaRouterService", r0);
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:192:0x06e8  */
    /* JADX WARN: Removed duplicated region for block: B:193:0x070d  */
    /* JADX WARN: Removed duplicated region for block: B:196:0x071b  */
    /* JADX WARN: Removed duplicated region for block: B:199:0x0738  */
    /* JADX WARN: Removed duplicated region for block: B:202:0x0755  */
    /* JADX WARN: Removed duplicated region for block: B:205:0x0772  */
    /* JADX WARN: Removed duplicated region for block: B:208:0x078b  */
    /* JADX WARN: Removed duplicated region for block: B:241:0x089d  */
    /* JADX WARN: Removed duplicated region for block: B:283:0x09a1  */
    /* JADX WARN: Removed duplicated region for block: B:290:0x09ba  */
    /* JADX WARN: Removed duplicated region for block: B:298:0x09e3  */
    /* JADX WARN: Removed duplicated region for block: B:299:0x09f3  */
    /* JADX WARN: Removed duplicated region for block: B:302:0x0a01  */
    /* JADX WARN: Removed duplicated region for block: B:303:0x0a09  */
    /* JADX WARN: Removed duplicated region for block: B:310:0x0a5d  */
    /* JADX WARN: Removed duplicated region for block: B:313:0x0a84  */
    /* JADX WARN: Removed duplicated region for block: B:321:0x0ab5  */
    /* JADX WARN: Removed duplicated region for block: B:329:0x0ae6  */
    /* JADX WARN: Removed duplicated region for block: B:334:0x0b03  */
    /* JADX WARN: Removed duplicated region for block: B:355:0x0b75  */
    /* JADX WARN: Removed duplicated region for block: B:358:0x0ba7  */
    /* JADX WARN: Removed duplicated region for block: B:361:0x0bc0  */
    /* JADX WARN: Removed duplicated region for block: B:366:0x0c04  */
    /* JADX WARN: Removed duplicated region for block: B:369:0x0c2c  */
    /* JADX WARN: Removed duplicated region for block: B:382:0x0c79  */
    /* JADX WARN: Removed duplicated region for block: B:388:0x0c8c  */
    /* JADX WARN: Removed duplicated region for block: B:390:0x0c9d A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:408:0x0d1a  */
    /* JADX WARN: Removed duplicated region for block: B:411:0x0d31  */
    /* JADX WARN: Removed duplicated region for block: B:414:0x0d4a  */
    /* JADX WARN: Removed duplicated region for block: B:417:0x0d88  */
    /* JADX WARN: Removed duplicated region for block: B:420:0x0da1  */
    /* JADX WARN: Removed duplicated region for block: B:425:0x0dc4  */
    /* JADX WARN: Removed duplicated region for block: B:428:0x0ddd  */
    /* JADX WARN: Removed duplicated region for block: B:431:0x0df6  */
    /* JADX WARN: Removed duplicated region for block: B:443:0x0e43  */
    /* JADX WARN: Removed duplicated region for block: B:444:0x0e58  */
    /* JADX WARN: Removed duplicated region for block: B:446:0x0e5c  */
    /* JADX WARN: Removed duplicated region for block: B:448:0x0e6d  */
    /* JADX WARN: Removed duplicated region for block: B:456:0x0eb4  */
    /* JADX WARN: Removed duplicated region for block: B:463:0x0ecb  */
    /* JADX WARN: Removed duplicated region for block: B:471:0x0f66  */
    /* JADX WARN: Removed duplicated region for block: B:473:0x0f77  */
    /* JADX WARN: Removed duplicated region for block: B:479:0x0fdd  */
    /* JADX WARN: Removed duplicated region for block: B:482:0x0ff8  */
    /* JADX WARN: Removed duplicated region for block: B:485:0x1049  */
    /* JADX WARN: Removed duplicated region for block: B:488:0x106c  */
    /* JADX WARN: Removed duplicated region for block: B:491:0x1084  */
    /* JADX WARN: Removed duplicated region for block: B:492:0x1094  */
    /* JADX WARN: Removed duplicated region for block: B:648:0x110a A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:654:0x10da A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:660:0x117a A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:702:0x0812 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Type inference failed for: r0v279, types: [com.android.server.media.MediaRouterService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r0v327, types: [com.android.server.SerialService] */
    /* JADX WARN: Type inference failed for: r0v380, types: [android.os.IBinder, com.android.server.net.NetworkPolicyManagerService] */
    /* JADX WARN: Type inference failed for: r0v383, types: [com.android.server.net.NetworkStatsService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r0v402, types: [com.android.server.statusbar.StatusBarManagerService] */
    /* JADX WARN: Type inference failed for: r11v5, types: [com.android.server.VpnManagerService] */
    /* JADX WARN: Type inference failed for: r12v11, types: [com.android.server.VcnManagementService] */
    /* JADX WARN: Type inference failed for: r13v9, types: [com.android.server.CountryDetectorService] */
    /* JADX WARN: Type inference failed for: r6v15, types: [android.os.IBinder, com.android.server.wm.WindowManagerService] */
    /* JADX WARN: Type inference failed for: r7v34, types: [com.android.server.input.InputManagerService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r7v48, types: [com.android.server.am.ActivityManagerService] */
    /* JADX WARN: Type inference failed for: r9v17, types: [com.android.server.TelephonyRegistry, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r9v27, types: [com.android.server.IpSecService] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void startOtherServices(final com.android.server.utils.TimingsTraceAndSlog r66) {
        /*
            Method dump skipped, instructions count: 5127
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

    public /* synthetic */ void lambda$startOtherServices$6$SystemServer(TimingsTraceAndSlog t, DevicePolicyManagerService.Lifecycle dpms, boolean safeMode, ConnectivityManager connectivityF, NetworkManagementService networkManagementF, NetworkPolicyManagerService networkPolicyF, IpSecService ipSecServiceF, NetworkStatsService networkStatsF, VpnManagerService vpnManagerF, VcnManagementService vcnManagementF, CountryDetectorService countryDetectorF, NetworkTimeUpdateService networkTimeUpdaterF, InputManagerService inputManagerF, TelephonyRegistry telephonyRegistryF, MediaRouterService mediaRouterF, MmsServiceBroker mmsServiceF) {
        Future<?> webviewPrep;
        CountDownLatch networkPolicyInitReadySignal;
        Slog.i(TAG, "Making services ready");
        t.traceBegin("StartActivityManagerReadyPhase");
        this.mSystemServiceManager.startBootPhase(t, SystemService.PHASE_ACTIVITY_MANAGER_READY);
        t.traceEnd();
        this.mSystemServerExt.systemRunning();
        t.traceBegin("StartObservingNativeCrashes");
        try {
            this.mActivityManagerService.startObservingNativeCrashes();
        } catch (Throwable e) {
            reportWtf("observing native crashes", e);
        }
        t.traceEnd();
        t.traceBegin("RegisterAppOpsPolicy");
        try {
            this.mActivityManagerService.setAppOpsPolicy(new AppOpsPolicy(this.mSystemContext));
        } catch (Throwable e2) {
            reportWtf("registering app ops policy", e2);
        }
        t.traceEnd();
        if (!this.mOnlyCore && this.mWebViewUpdateService != null) {
            Future<?> webviewPrep2 = SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda4
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
            SystemService cshs = this.mSystemServiceManager.startService(CAR_SERVICE_HELPER_SERVICE_CLASS);
            if (cshs instanceof Dumpable) {
                this.mDumper.addDumpable((Dumpable) cshs);
            }
            if (cshs instanceof DevicePolicySafetyChecker) {
                dpms.setDevicePolicySafetyChecker((DevicePolicySafetyChecker) cshs);
            }
            t.traceEnd();
        }
        if (safeMode) {
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
        t.traceBegin("MakeConnectivityServiceReady");
        if (connectivityF != null) {
            try {
                connectivityF.systemReady();
            } catch (Throwable e7) {
                reportWtf("making Connectivity Service ready", e7);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeVpnManagerServiceReady");
        if (vpnManagerF != null) {
            try {
                vpnManagerF.systemReady();
            } catch (Throwable e8) {
                reportWtf("making VpnManagerService ready", e8);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeVcnManagementServiceReady");
        if (vcnManagementF != null) {
            try {
                vcnManagementF.systemReady();
            } catch (Throwable e9) {
                reportWtf("making VcnManagementService ready", e9);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeNetworkPolicyServiceReady");
        if (networkPolicyF != null) {
            try {
                networkPolicyF.systemReady(networkPolicyInitReadySignal);
            } catch (Throwable e10) {
                reportWtf("making Network Policy Service ready", e10);
            }
        }
        t.traceEnd();
        this.mPackageManagerService.waitForAppDataPrepared();
        t.traceBegin("PhaseThirdPartyAppsCanStart");
        if (webviewPrep != null) {
            ConcurrentUtils.waitForFutureNoInterrupt(webviewPrep, "WebViewFactoryPreparation");
        }
        this.mSystemServiceManager.startBootPhase(t, 600);
        t.traceEnd();
        t.traceBegin("StartNetworkStack");
        try {
            NetworkStackClient.getInstance().start();
        } catch (Throwable e11) {
            reportWtf("starting Network Stack", e11);
        }
        t.traceEnd();
        t.traceBegin("StartTethering");
        try {
            ConnectivityModuleConnector.getInstance().startModuleService(TETHERING_CONNECTOR_CLASS, "android.permission.MAINLINE_NETWORK_STACK", SystemServer$$ExternalSyntheticLambda0.INSTANCE);
        } catch (Throwable e12) {
            reportWtf("starting Tethering", e12);
        }
        t.traceEnd();
        t.traceBegin("MakeCountryDetectionServiceReady");
        if (countryDetectorF != null) {
            try {
                countryDetectorF.systemRunning();
            } catch (Throwable e13) {
                reportWtf("Notifying CountryDetectorService running", e13);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeNetworkTimeUpdateReady");
        if (networkTimeUpdaterF != null) {
            try {
                networkTimeUpdaterF.systemRunning();
            } catch (Throwable e14) {
                reportWtf("Notifying NetworkTimeService running", e14);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeInputManagerServiceReady");
        if (inputManagerF != null) {
            try {
                inputManagerF.systemRunning();
            } catch (Throwable e15) {
                reportWtf("Notifying InputManagerService running", e15);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeTelephonyRegistryReady");
        if (telephonyRegistryF != null) {
            try {
                telephonyRegistryF.systemRunning();
            } catch (Throwable e16) {
                reportWtf("Notifying TelephonyRegistry running", e16);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeMediaRouterServiceReady");
        if (mediaRouterF != null) {
            try {
                mediaRouterF.systemRunning();
            } catch (Throwable e17) {
                reportWtf("Notifying MediaRouterService running", e17);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeMmsServiceReady");
        if (mmsServiceF != null) {
            try {
                mmsServiceF.systemRunning();
            } catch (Throwable e18) {
                reportWtf("Notifying MmsService running", e18);
            }
        }
        t.traceEnd();
        t.traceBegin("IncidentDaemonReady");
        try {
            IIncidentManager incident = IIncidentManager.Stub.asInterface(ServiceManager.getService("incident"));
            if (incident != null) {
                incident.systemRunning();
            }
        } catch (Throwable e19) {
            reportWtf("Notifying incident daemon running", e19);
        }
        t.traceEnd();
        if (this.mIncrementalServiceHandle != 0) {
            t.traceBegin("MakeIncrementalServiceReady");
            setIncrementalServiceSystemReady(this.mIncrementalServiceHandle);
            t.traceEnd();
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
        if (!deviceHasConfigString(context, 17039926)) {
            Slog.d(TAG, "SystemCaptionsManagerService disabled because resource is not overlaid");
            return;
        }
        t.traceBegin("StartSystemCaptionsManagerService");
        this.mSystemServiceManager.startService(SYSTEM_CAPTIONS_MANAGER_SERVICE_CLASS);
        t.traceEnd();
    }

    private void startTextToSpeechManagerService(Context context, TimingsTraceAndSlog t) {
        t.traceBegin("StartTextToSpeechManagerService");
        this.mSystemServiceManager.startService(TEXT_TO_SPEECH_MANAGER_SERVICE_CLASS);
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
        if (!explicitlyEnabled && !deviceHasConfigString(context, 17039909)) {
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

    private void startRotationResolverService(Context context, TimingsTraceAndSlog t) {
        if (!RotationResolverManagerService.isServiceConfigured(context)) {
            Slog.d(TAG, "RotationResolverService is not configured on this device");
            return;
        }
        t.traceBegin("StartRotationResolverService");
        this.mSystemServiceManager.startService(RotationResolverManagerService.class);
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
    public static boolean handleEarlySystemWtf(IBinder app, String tag, boolean system2, ApplicationErrorReport.ParcelableCrashInfo crashInfo, int immediateCallerPid) {
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