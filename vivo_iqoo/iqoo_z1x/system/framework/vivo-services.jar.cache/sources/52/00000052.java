package com.android.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.TimingsTraceLog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.anr.ANRManagerService;
import com.android.server.biometrics.fingerprint.AnalysisService;
import com.android.server.biometrics.fingerprint.FingerprintUIManagerService;
import com.android.server.display.VivoDisplayStateService;
import com.android.server.hangvivodebug.HangVivoDebugConfig;
import com.android.server.inputmethod.VivoInputMethodManagerServiceImpl;
import com.vivo.face.common.data.Config;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.services.ServiceFactoryImpl;
import com.vivo.services.autorecover.SystemAutoRecoverService;
import com.vivo.services.rms.RmsInjectorImpl;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoSystemServerImpl implements IVivoSystemServer {
    private static final String TAG = "VivoSystemServer";
    private Context mContext;
    private SystemServiceManager mSystemServiceManager;
    private VivoDisplayStateService mVivoDisplayStateService;
    private static final String SYSTEM_SERVER_TIMING_TAG = "VivoSystemServerTiming";
    private static final TimingsTraceLog BOOT_TIMINGS_TRACE_LOG = new TimingsTraceLog(SYSTEM_SERVER_TIMING_TAG, 524288);

    public VivoSystemServerImpl(SystemServiceManager systemServiceManager, Context context) {
        this.mSystemServiceManager = systemServiceManager;
        this.mContext = context;
    }

    private static void traceBeginAndSlog(String name) {
        VSlog.i(TAG, name);
        BOOT_TIMINGS_TRACE_LOG.traceBegin(name);
    }

    private static void traceEnd() {
        BOOT_TIMINGS_TRACE_LOG.traceEnd();
    }

    private void reportWtf(String msg, Throwable e) {
        VSlog.w(TAG, "***********************************************");
        VSlog.wtf(TAG, "BOOT FAILURE " + msg, e);
    }

    public void startConfigurationManagerService(Context context) {
        try {
            ServiceManager.addService("configuration_service", ServiceFactoryImpl.getConfigurationManagerService(context));
        } catch (Throwable th) {
            VSlog.e(TAG, "starting ConfigurationManagerService failed");
        }
    }

    public void startVgcManagerService(Context context) {
        traceBeginAndSlog("startVgcManagerService");
        try {
            ServiceManager.addService("vgc_service", ServiceFactoryImpl.getVgcManagerService(context));
        } catch (Throwable e) {
            reportWtf("starting startVgcManagerService", e);
        }
        try {
            this.mSystemServiceManager.startService("com.vivo.services.vgc.cbs.VivoCbsService");
        } catch (Throwable e2) {
            reportWtf("starting cbs_service", e2);
        }
        traceEnd();
    }

    public void startSecInputMethodManagerService() {
        VivoInputMethodManagerServiceImpl.Lifecycle secureLifecycle = new VivoInputMethodManagerServiceImpl.Lifecycle(this.mContext);
        secureLifecycle.setSecFlag(true);
        this.mSystemServiceManager.startService(secureLifecycle);
    }

    public void startFingerprintUIManagerService() {
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager.hasSystemFeature("android.hardware.fingerprint")) {
            traceBeginAndSlog("startFingerprintUIManagerService");
            this.mSystemServiceManager.startService(FingerprintUIManagerService.class);
            traceEnd();
        }
    }

    public void startFingerprintAnalysisService(int factoryTestMode) {
        if (factoryTestMode != 1) {
            PackageManager packageManager = this.mContext.getPackageManager();
            if (packageManager.hasSystemFeature("android.hardware.fingerprint")) {
                traceBeginAndSlog("startFingerprintAnalysisService");
                this.mSystemServiceManager.startService(AnalysisService.class);
                traceEnd();
            }
        }
    }

    public void startFingerprintUi() {
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager.hasSystemFeature("android.hardware.fingerprint")) {
            traceBeginAndSlog("StartFingerprintUI");
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.vivo.fingerprintui", "com.vivo.fingerprintui.FingerprintUIService"));
                intent.addFlags(256);
                this.mContext.startServiceAsUser(intent, UserHandle.SYSTEM);
            } catch (Throwable e) {
                reportWtf("starting Fingerprint UI", e);
            }
            traceEnd();
        }
    }

    public void startFaceUi() {
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager.hasSystemFeature("android.hardware.biometrics.face")) {
            Config.intProperties(this.mContext);
            traceBeginAndSlog("StartFaceUI");
            try {
                startFaceUiInternal(this.mContext);
            } catch (Throwable e) {
                reportWtf("starting FaceUI", e);
            }
            traceEnd();
        }
    }

    private void startFaceUiInternal(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(FaceUIState.PKG_FACEUI, "com.vivo.faceui.FaceUIService"));
        intent.addFlags(256);
        context.startServiceAsUser(intent, UserHandle.SYSTEM);
    }

    public void startDisplayStateService() {
        traceBeginAndSlog("StartVivoDisplayStateManager");
        try {
            this.mVivoDisplayStateService = (VivoDisplayStateService) this.mSystemServiceManager.startService(VivoDisplayStateService.class);
        } catch (Throwable e) {
            reportWtf("starting VivoDisplayStateService", e);
        }
        traceEnd();
    }

    public void makeDisplayStateServiceReady() {
        traceBeginAndSlog("MakeVivoDisplayStateServiceReady");
        try {
            this.mVivoDisplayStateService.systemReady();
        } catch (Throwable e) {
            reportWtf("making Vivo Display State Service ready", e);
        }
        traceEnd();
    }

    public void startHangVivoConfigService() {
        VSlog.w(TAG, "start to HangVivoDebugConfig");
        HangVivoDebugConfig.getInstance().init(this.mContext);
    }

    public void startAutoRecoverService(ActivityManagerService ams) {
        traceBeginAndSlog("startAutoRecoverService");
        SystemAutoRecoverService systemAutoRecoverService = (SystemAutoRecoverService) this.mSystemServiceManager.startService(SystemAutoRecoverService.class);
        systemAutoRecoverService.setAms(ams);
        traceEnd();
    }

    public void startANRManagerService(Context context, ActivityManagerService mActivityManagerService) {
        traceBeginAndSlog("StartANRManager");
        try {
            ServiceManager.addService("vivo_anrmanager", new ANRManagerService(context, mActivityManagerService));
        } catch (Throwable e) {
            reportWtf("starting ANR Manager", e);
        }
        traceEnd();
    }

    public void startCoreServices() {
        RmsInjectorImpl.getInstance().startCoreServices();
    }

    public void startOtherServices() {
        RmsInjectorImpl.getInstance().startOtherServices();
    }
}