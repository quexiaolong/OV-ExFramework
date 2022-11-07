package com.android.server;

import android.common.OplusExtPluginFactory;
import android.common.OplusFeatureCache;
import android.content.Context;
import android.graphics.ITypefaceExt;
import android.graphics.Typeface;
import android.os.IBinder;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.input.InputManagerService;
import com.android.server.input.SubInputManagerService;
import com.android.server.oplus.CabcService;
import com.android.server.oplus.LinearmotorVibratorService;
import com.android.server.oplus.datanormalization.IOplusDataNormalizationManager;
import com.android.server.oplus.filter.DynamicFilterService;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.policy.SubPhoneWindowManager;
import com.android.server.test.OplusTestServiceExt;
import com.android.server.usage.UsageStatsService;
import com.android.server.utils.TimingsTraceAndSlog;
import com.oplus.content.OplusFeatureConfigManager;
import com.oplus.phoenix.Phoenix;
import com.oplus.theme.IOplusThemeStyle;
import java.util.concurrent.Future;

/* loaded from: classes.dex */
public class SystemServerExtImpl implements ISystemServerExt {
    private static final String TAG = "SystemServer";
    private DynamicFilterService mDynamicFilterService;
    private Future<?> mOplusFeatureStart;
    StorageHealthInfoService mStorageHealthInfo = null;
    private OplusSystemServerHelper mHelper = null;
    LinearmotorVibratorService linearVibrator = null;
    InputManagerService mSubInputManagerService = null;
    PhoneWindowManager mSubPhoneWindowManager = null;

    public static int getSystemThemeStyle() {
        return ((IOplusThemeStyle) OplusFeatureCache.getOrCreate(IOplusThemeStyle.DEFAULT, new Object[0])).getSystemThemeStyle(16974849);
    }

    public void initFontsForserializeFontMap() {
        OplusExtPluginFactory.getInstance().getFeature(ITypefaceExt.DEFAULT, new Object[]{Typeface.DEFAULT}).initFontsForserializeFontMap();
    }

    public void initSystemServer(Context systemContext) {
        Slog.i(TAG, "CRYPTO_SELF_TEST_COMPLETED");
        System.loadLibrary("oplus_servers");
        this.mHelper = new OplusSystemServerHelper(systemContext);
        this.mOplusFeatureStart = SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.SystemServerExtImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SystemServerExtImpl.this.lambda$initSystemServer$0$SystemServerExtImpl();
            }
        }, "LoadOplusFeatures");
        this.mHelper = new OplusSystemServerHelper(systemContext);
        SystemProperties.set("sys.oplus.boot_completed", "0");
    }

    public /* synthetic */ void lambda$initSystemServer$0$SystemServerExtImpl() {
        this.mHelper.loadOplusFeaturesAsync();
    }

    public void setDataNormalizationManager() {
        IOplusDataNormalizationManager dnm = (IOplusDataNormalizationManager) OplusServiceFactory.getInstance().getFeature(IOplusDataNormalizationManager.DEFAULT, new Object[0]);
        OplusFeatureCache.set(dnm);
    }

    public void addOplusDevicePolicyService() {
        this.mHelper.addOplusDevicePolicyService();
    }

    public void waitForFutureNoInterrupt() {
        ConcurrentUtils.waitForFutureNoInterrupt(this.mOplusFeatureStart, "LoadOplusFeatures");
    }

    public void startBootstrapServices() {
        this.mHelper.startBootstrapServices();
    }

    public void startCoreServices() {
        this.mHelper.startCoreServices();
    }

    public InputManagerService getInputManagerService(Context context) {
        InputManagerService inputManagerService = this.mSubInputManagerService;
        if (inputManagerService == null) {
            return new SubInputManagerService(context);
        }
        return inputManagerService;
    }

    public PhoneWindowManager getSubPhoneWindowManager() {
        PhoneWindowManager phoneWindowManager = this.mSubPhoneWindowManager;
        if (phoneWindowManager == null) {
            return new SubPhoneWindowManager();
        }
        return phoneWindowManager;
    }

    public boolean startJobSchedulerService() {
        return this.mHelper.startJobSchedulerService();
    }

    /* JADX WARN: Type inference failed for: r0v2, types: [com.android.server.oplus.LinearmotorVibratorService, android.os.IBinder] */
    public void addLinearmotorVibratorService(Context context) {
        if (OplusFeatureConfigManager.getInstacne().hasFeature("oplus.software.vibrator_lmvibrator")) {
            ?? linearmotorVibratorService = new LinearmotorVibratorService(context);
            this.linearVibrator = linearmotorVibratorService;
            ServiceManager.addService("linearmotor", (IBinder) linearmotorVibratorService);
        }
    }

    public void addStorageHealthInfoService(Context context) {
        try {
            StorageHealthInfoService storageHealthInfoService = new StorageHealthInfoService(context);
            this.mStorageHealthInfo = storageHealthInfoService;
            ServiceManager.addService("storage_healthinfo", storageHealthInfoService);
            Slog.i(TAG, "StorageHealthInfoService  Succeed!");
        } catch (Throwable e) {
            Slog.e(TAG, "Failure starting StorageHealthInfoService Service", e);
        }
    }

    public void startOtherServices() {
        this.mHelper.startOtherServices();
    }

    public void linearVibratorSystemReady() {
        try {
            Slog.i(TAG, "LinearMotor Service systemReady");
            LinearmotorVibratorService linearmotorVibratorService = this.linearVibrator;
            if (linearmotorVibratorService != null) {
                linearmotorVibratorService.systemReady();
            }
        } catch (Throwable e) {
            reportWtf("LinearMotor Service systemReady e:", e);
        }
    }

    public void systemReady() {
        this.mHelper.systemReady();
    }

    public void systemRunning() {
        this.mHelper.systemRunning();
    }

    public void startUsageStatsService(SystemServiceManager systemServiceManager) {
        try {
            Watchdog watchdog = Watchdog.getInstance();
            watchdog.pauseWatchingCurrentThread("process UsageStats;" + Thread.currentThread().toString());
            systemServiceManager.startService(UsageStatsService.class);
        } finally {
            try {
            } finally {
            }
        }
    }

    private void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + msg, e);
    }

    public void writeAgingCriticalEvent() {
        AgingCriticalEvent agingCriticalEvent = AgingCriticalEvent.getInstance();
        agingCriticalEvent.writeEvent(AgingCriticalEvent.EVENT_SYSTEM_BOOTUP, "systemserver pid:" + Process.myPid());
    }

    public void setBootstage(boolean start) {
        if (start) {
            Phoenix.setBootstage("ANDROID_SYSTEMSERVER_INIT_START");
        } else {
            Phoenix.setBootstage("ANDROID_SYSTEMSERVER_READY");
        }
    }

    public void startDynamicFilterService(SystemServiceManager systemServiceManager) {
        Slog.i(TAG, "Dynamic filter service");
        this.mDynamicFilterService = (DynamicFilterService) systemServiceManager.startService(DynamicFilterService.class);
    }

    public void dynamicFilterServiceSystemReady(TimingsTraceAndSlog t) {
        t.traceBegin("MakeDynamicFilterServiceReady");
        try {
            Slog.i(TAG, "Dynamic filter service systemReady");
            this.mDynamicFilterService.systemReady();
        } catch (Throwable e) {
            reportWtf("making dynamic filter service ready", e);
        }
        t.traceEnd();
    }

    public void addCabcService(Context context, TimingsTraceAndSlog t) {
        t.traceBegin("CabcService");
        try {
            Slog.i(TAG, "Cabc Service");
            ServiceManager.addService("cabc", new CabcService(context));
        } catch (Throwable e) {
            reportWtf("starting Cabc Service", e);
        }
        t.traceEnd();
    }

    public void addOplusTestService(Context context) {
        try {
            ServiceManager.addService("coverage", new OplusTestServiceExt());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}