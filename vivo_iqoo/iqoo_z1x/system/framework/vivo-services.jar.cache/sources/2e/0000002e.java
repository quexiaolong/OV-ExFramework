package com.android.server;

import android.content.Context;
import android.os.FtBuild;
import android.os.ServiceManager;
import android.util.FtFeature;
import android.util.Slog;
import com.vivo.common.utils.VLog;
import com.vivo.framework.vivo4dgamevibrator.IVivo4DGameVibratorService;
import com.vivo.services.ServiceFactoryImpl;
import com.vivo.services.security.client.VivoPermissionManager;
import vivo.app.aivirus.IVivoBehaviorEngService;
import vivo.app.artkeeper.IVivoArtKeeperManager;
import vivo.app.backup.IVivoBackupManager;
import vivo.app.capacitykey.ICapacityKey;
import vivo.app.common.IVivoCommon;
import vivo.app.engineerutile.IBBKEngineerUtileService;
import vivo.app.memc.IMemcManager;
import vivo.app.motion.IMotionManager;
import vivo.app.nightmode.IVivoNightModeManager;
import vivo.app.phonelock.IVivoPhoneLockService;
import vivo.app.physicalfling.IPhysicalFlingManager;
import vivo.app.popupcamera.IPopupCameraManager;
import vivo.app.proxcali.IVivoProxCali;
import vivo.app.sarpower.IVivoSarPowerState;
import vivo.app.security.IVivoPermissionService;
import vivo.app.sensorhub.IVivoSensorHub;
import vivo.app.superresolution.ISuperResolutionManager;
import vivo.app.systemdefence.ISystemDefenceManager;
import vivo.app.themeicon.IThemeIconManager;
import vivo.app.timezone.ITZManager;
import vivo.app.touchscreen.ITouchScreen;
import vivo.app.vcodehaltransfer.IVivoVcodeHalTransferService;
import vivo.app.vivolight.IVivoLightManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoBinderServiceImpl implements IVivoBinderService {
    private static final String TAG = "VivoBinderServiceImpl";
    private static VivoBinderServiceImpl sInstance;

    private VivoBinderServiceImpl() {
    }

    public static VivoBinderServiceImpl getInstance() {
        VivoBinderServiceImpl vivoBinderServiceImpl;
        synchronized (VivoBinderServiceImpl.class) {
            if (sInstance == null) {
                sInstance = new VivoBinderServiceImpl();
            }
            vivoBinderServiceImpl = sInstance;
        }
        return vivoBinderServiceImpl;
    }

    public void loadVivoServersLib() {
        System.loadLibrary("vivo_servers");
    }

    public void addVivoBinderService(Context context) {
        IVivoPermissionService.Stub permissionService;
        VSlog.d(TAG, "addVivoBinderService");
        try {
            IVivoCommon.Stub commonService = ServiceFactoryImpl.getVivoCommonService(context);
            if (commonService != null) {
                ServiceManager.addService("vivo_common_service", commonService);
            }
        } catch (Throwable e) {
            VSlog.e(TAG, "Failure starting vivo_common_service", e);
        }
        try {
            ITouchScreen.Stub touchScreenService = ServiceFactoryImpl.getTouchScreenService(context);
            if (touchScreenService != null) {
                ServiceManager.addService("bbk_touch_screen_service", touchScreenService);
            }
        } catch (Throwable e2) {
            VSlog.e(TAG, "Failure starting bbk_touch_screen_service", e2);
        }
        try {
            ICapacityKey.Stub capacityKeyService = ServiceFactoryImpl.getCapacityKeyService(context);
            if (capacityKeyService != null) {
                ServiceManager.addService("capacitykey_service", capacityKeyService);
            }
        } catch (Throwable e3) {
            VSlog.e(TAG, "Failure starting capacity_key_service", e3);
        }
        if (FtFeature.isFeatureSupport("vivo.hardware.popupcamera")) {
            try {
                IPopupCameraManager.Stub popupCameraService = ServiceFactoryImpl.getPopupCameraManagerService(context);
                if (popupCameraService != null) {
                    ServiceManager.addService("popup_camera_service", popupCameraService);
                }
            } catch (Throwable e4) {
                VSlog.e(TAG, "Failure starting popup_camera_service", e4);
            }
        }
        try {
            ITZManager.Stub tzMoniterManager = ServiceFactoryImpl.getTZManagerService(context);
            if (tzMoniterManager != null) {
                ServiceManager.addService("tz_moniter", tzMoniterManager);
            }
        } catch (Throwable e5) {
            VSlog.e(TAG, "Failure starting TZMoniter service", e5);
        }
        if (FtBuild.getTierLevel() == 0) {
            try {
                if (VivoPermissionManager.needVPM() && (permissionService = ServiceFactoryImpl.getVivoPermissionService(context, UiThread.getHandler())) != null) {
                    ServiceManager.addService("vivo_permission_service", permissionService);
                }
            } catch (Throwable e6) {
                VSlog.e(TAG, "Failure starting vivo_permission_service", e6);
            }
        }
        boolean z = true;
        try {
            VSlog.d(TAG, "VivoVcodeHalTransferService addService start");
            IVivoVcodeHalTransferService.Stub vivoVcodeHalTransferService = ServiceFactoryImpl.getVivoVcodeHalTransferService(context);
            StringBuilder sb = new StringBuilder();
            sb.append("VivoVcodeHalTransferService is null?:");
            sb.append(vivoVcodeHalTransferService == null);
            VSlog.d(TAG, sb.toString());
            if (vivoVcodeHalTransferService == null) {
                VSlog.d(TAG, "VivoVcodeHalTransferService addService null");
            } else {
                ServiceManager.addService("vivo_vcode_hal_transfer_service", vivoVcodeHalTransferService);
                VSlog.d(TAG, "VivoVcodeHalTransferService addService complete");
            }
        } catch (Throwable e7) {
            VSlog.e(TAG, "Failure starting VivoVcodeHalTransferService", e7);
        }
        try {
            IVivoBackupManager.Stub vivoBackupService = ServiceFactoryImpl.getVivoBackupService(context);
            if (vivoBackupService != null) {
                ServiceManager.addService("vivo_backup_service", vivoBackupService);
            }
        } catch (Throwable e8) {
            VSlog.e(TAG, "Failure starting vivo_backup_service", e8);
        }
        try {
            IThemeIconManager.Stub themeIconManager = ServiceFactoryImpl.getThemeIconService(context);
            if (themeIconManager == null) {
                VLog.d(TAG, "themeIconService null");
            } else {
                ServiceManager.addService("theme_icon_service", themeIconManager);
            }
        } catch (Throwable e9) {
            VSlog.e(TAG, "failed to add themeIconService", e9);
        }
        try {
            IVivoNightModeManager.Stub nightModeManager = ServiceFactoryImpl.getNightModeService(context);
            if (nightModeManager == null) {
                VLog.d(TAG, "get NightModeService null");
            } else {
                ServiceManager.addService("nightmode", nightModeManager);
            }
        } catch (Throwable e10) {
            VSlog.e(TAG, "Failure starting Nightmode service", e10);
        }
        try {
            IBBKEngineerUtileService.Stub engineerUtileService = ServiceFactoryImpl.getBBKEngineerUtileService(context);
            if (engineerUtileService == null) {
                VSlog.d(TAG, "engineerUtileService is null");
            } else {
                ServiceManager.addService("engineer_utile", engineerUtileService);
            }
        } catch (Throwable e11) {
            VSlog.d(TAG, "Failure starting engineer_utile service", e11);
        }
        try {
            IVivoSensorHub.Stub sensorHubService = ServiceFactoryImpl.getVivoSensorHubService(context);
            if (sensorHubService != null) {
                ServiceManager.addService("vivo_sensorhub_service", sensorHubService);
            }
        } catch (Throwable e12) {
            VSlog.e(TAG, "Failure starting vivo_sensorhub_service", e12);
        }
        try {
            IVivoProxCali.Stub proxcaliService = ServiceFactoryImpl.getVivoProxCaliService(context);
            if (proxcaliService != null) {
                ServiceManager.addService("vivo_prox_cali_service", proxcaliService);
            }
        } catch (Throwable e13) {
            VSlog.e(TAG, "Failure starting vivo_prox_cali_service", e13);
        }
        try {
            IVivoSarPowerState.Stub sarPowerService = ServiceFactoryImpl.getVivoSarPowerStateService(context);
            if (sarPowerService != null) {
                ServiceManager.addService("vivo_sar_power_state_service", sarPowerService);
            }
        } catch (Throwable e14) {
            VSlog.e(TAG, "Failure starting vivo_sar_power_state_service", e14);
        }
        try {
            IMotionManager.Stub motionService = ServiceFactoryImpl.getMotionManagerService(context);
            if (motionService != null) {
                ServiceManager.addService("motion_manager", motionService);
            }
        } catch (Throwable e15) {
            VSlog.e(TAG, "Failure starting motion_manager service", e15);
        }
        try {
            IVivo4DGameVibratorService.Stub vivo4DGameVibratorService = ServiceFactoryImpl.getVivo4DGameVibratorService(context);
            StringBuilder sb2 = new StringBuilder();
            sb2.append("vivo4DGameVibratorService is null?:");
            sb2.append(vivo4DGameVibratorService == null);
            VSlog.d(TAG, sb2.toString());
            if (vivo4DGameVibratorService != null) {
                ServiceManager.addService("vivo_4d_game_vibrator_service", vivo4DGameVibratorService);
            }
        } catch (Throwable e16) {
            VSlog.e(TAG, "Failure starting VIVO_4D_GAME_VIBRATOR_SERVICE", e16);
        }
        String supportLightType = FtFeature.getFeatureAttribute("vivo.hardware.extralight", "support_light_type", "0");
        if ("2".equals(supportLightType)) {
            try {
                IVivoLightManager.Stub vivoLightService = ServiceFactoryImpl.getVivoLightManagerService(context);
                if (vivoLightService != null) {
                    ServiceManager.addService("vivo_light_service", vivoLightService);
                }
            } catch (Throwable e17) {
                Slog.e(TAG, "Failure starting VivoLight service", e17);
            }
        }
        try {
            VSlog.d(TAG, "vivoArtKeeperService addService start");
            IVivoArtKeeperManager.Stub vivoArtKeeperService = ServiceFactoryImpl.getVivoArtKeeperService(context);
            StringBuilder sb3 = new StringBuilder();
            sb3.append("vivoArtKeeperService is null?:");
            if (vivoArtKeeperService != null) {
                z = false;
            }
            sb3.append(z);
            VSlog.d(TAG, sb3.toString());
            if (vivoArtKeeperService != null) {
                ServiceManager.addService("vivo_art_keeper_service", vivoArtKeeperService);
            }
            VSlog.d(TAG, "vivoArtKeeperService addService complete");
        } catch (Throwable e18) {
            VSlog.e(TAG, "Failure starting vivoArtKeeperService", e18);
        }
        try {
            ISystemDefenceManager.Stub systemDefecneService = ServiceFactoryImpl.getSystemDefenceService(context);
            if (systemDefecneService == null) {
                VSlog.d(TAG, "get systemDefecneService null");
            } else {
                ServiceManager.addService("system_defence_service", systemDefecneService);
            }
        } catch (Throwable e19) {
            VSlog.e(TAG, "Failure starting systemDefecneService", e19);
        }
        try {
            IVivoPhoneLockService.Stub vivoPhoneLockManager = ServiceFactoryImpl.getPhoneLockService(context);
            if (vivoPhoneLockManager == null) {
                VLog.d(TAG, "vivoPhoneLockManager null");
            } else {
                ServiceManager.addService("phonelock_service", vivoPhoneLockManager);
            }
        } catch (Throwable e20) {
            VSlog.e(TAG, "Failure starting phonelock service", e20);
        }
        try {
            ISuperResolutionManager.Stub superResolutionManager = ServiceFactoryImpl.getSuperResolutionService(context);
            if (superResolutionManager != null) {
                ServiceManager.addService("vivo_super_resolution_service", superResolutionManager);
            }
        } catch (Throwable e21) {
            VSlog.e(TAG, "Failure starting  SuperResolution service", e21);
        }
        try {
            IVivoBehaviorEngService.Stub behaviorEngService = ServiceFactoryImpl.getVivoBehaviorEngService(context);
            if (behaviorEngService != null) {
                ServiceManager.addService("vivo_behavior_service", behaviorEngService);
            }
        } catch (Throwable e22) {
            VSlog.e(TAG, "Failure starting vivo_behavior_service", e22);
        }
        String supportMemcType = FtFeature.getFeatureAttribute("vivo.software.memc", "support_memc_type", "0");
        if ("1".equals(supportMemcType)) {
            try {
                IMemcManager.Stub memcService = ServiceFactoryImpl.getMemcService(context);
                if (memcService != null) {
                    ServiceManager.addService("vivo_memc_service", memcService);
                }
            } catch (Throwable e23) {
                VSlog.e(TAG, "Failure starting memc service", e23);
            }
        }
        try {
            IPhysicalFlingManager.Stub physicalFlingService = ServiceFactoryImpl.getPhysicalFlingService(context);
            if (physicalFlingService == null) {
                VLog.d(TAG, "get physical fling service null");
            } else {
                ServiceManager.addService("physical_fling_service", physicalFlingService);
            }
        } catch (Throwable e24) {
            VSlog.e(TAG, "Failure starting physical fling service", e24);
        }
    }
}