package com.vivo.services;

import android.content.Context;
import android.os.Handler;
import com.vivo.framework.vivo4dgamevibrator.IVivo4DGameVibratorService;
import com.vivo.services.aivirus.VivoBehaviorEngService;
import com.vivo.services.artkeeper.VivoArtKeeperService;
import com.vivo.services.backup.VivoBackupManagerService;
import com.vivo.services.capacitykey.CapacityKeyService;
import com.vivo.services.common.VivoCommonService;
import com.vivo.services.configurationManager.ConfigurationManagerImpl;
import com.vivo.services.engineerutile.BBKEngineerUtileService;
import com.vivo.services.memc.MemcManagerService;
import com.vivo.services.motion.MotionManagerService;
import com.vivo.services.nightmode.VivoNightModeService;
import com.vivo.services.phonelock.VivoPhoneLockService;
import com.vivo.services.physicalfling.PhysicalFlingService;
import com.vivo.services.popupcamera.PopupCameraManagerService;
import com.vivo.services.proxcali.VivoProxCaliService;
import com.vivo.services.sarpower.VivoSarPowerStateService;
import com.vivo.services.security.server.VivoPermissionService;
import com.vivo.services.sensorhub.VivoSensorHubService;
import com.vivo.services.superresolution.SuperResolutionManagerService;
import com.vivo.services.systemdefence.SystemDefenceService;
import com.vivo.services.themeicon.ThemeIconService;
import com.vivo.services.timezone.TZManagerService;
import com.vivo.services.touchscreen.TouchScreenService;
import com.vivo.services.vcodehaltransfer.HalEventTransfer;
import com.vivo.services.vgc.VivoVgcService;
import com.vivo.services.vivo4dgamevibrator.Vivo4DGameVibratorService;
import com.vivo.services.vivolight.VivoLightManagerService;
import vivo.app.aivirus.IVivoBehaviorEngService;
import vivo.app.artkeeper.IVivoArtKeeperManager;
import vivo.app.backup.IVivoBackupManager;
import vivo.app.capacitykey.ICapacityKey;
import vivo.app.common.IVivoCommon;
import vivo.app.configuration.IConfigurationManager;
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
import vivo.app.vgc.IVivoVgcService;
import vivo.app.vivolight.IVivoLightManager;

/* loaded from: classes.dex */
public class ServiceFactoryImpl {
    public static IVivoCommon.Stub getVivoCommonService(Context context) {
        return VivoCommonService.getInstance(context);
    }

    public static ITouchScreen.Stub getTouchScreenService(Context context) {
        return new TouchScreenService(context);
    }

    public static IPopupCameraManager.Stub getPopupCameraManagerService(Context context) {
        return PopupCameraManagerService.getInstance(context);
    }

    public static IConfigurationManager.Stub getConfigurationManagerService(Context context) {
        return ConfigurationManagerImpl.getInstance(context);
    }

    public static IVivoVgcService.Stub getVgcManagerService(Context context) {
        return VivoVgcService.getInstance(context);
    }

    public static IVivoPhoneLockService.Stub getPhoneLockService(Context context) {
        return VivoPhoneLockService.getInstance(context);
    }

    public static ITZManager.Stub getTZManagerService(Context context) {
        return TZManagerService.getInstance(context);
    }

    public static IVivoPermissionService.Stub getVivoPermissionService(Context context, Handler uiHandler) {
        return new VivoPermissionService(context, uiHandler);
    }

    public static ICapacityKey.Stub getCapacityKeyService(Context context) {
        return new CapacityKeyService(context);
    }

    public static IVivoVcodeHalTransferService.Stub getVivoVcodeHalTransferService(Context context) {
        HalEventTransfer het = HalEventTransfer.getInstance();
        het.start();
        return het;
    }

    public static IVivoBackupManager.Stub getVivoBackupService(Context context) {
        return new VivoBackupManagerService(context);
    }

    public static IVivoNightModeManager.Stub getNightModeService(Context context) {
        return new VivoNightModeService(context);
    }

    public static IThemeIconManager.Stub getThemeIconService(Context context) {
        return new ThemeIconService(context);
    }

    public static IBBKEngineerUtileService.Stub getBBKEngineerUtileService(Context context) {
        return new BBKEngineerUtileService(context);
    }

    public static IVivoProxCali.Stub getVivoProxCaliService(Context context) {
        return new VivoProxCaliService(context);
    }

    public static IVivoSarPowerState.Stub getVivoSarPowerStateService(Context context) {
        return new VivoSarPowerStateService(context);
    }

    public static IMotionManager.Stub getMotionManagerService(Context context) {
        return new MotionManagerService(context);
    }

    public static IVivoSensorHub.Stub getVivoSensorHubService(Context context) {
        return new VivoSensorHubService(context);
    }

    public static IVivo4DGameVibratorService.Stub getVivo4DGameVibratorService(Context context) {
        return new Vivo4DGameVibratorService(context);
    }

    public static IVivoLightManager.Stub getVivoLightManagerService(Context context) {
        return VivoLightManagerService.getInstance(context);
    }

    public static IVivoArtKeeperManager.Stub getVivoArtKeeperService(Context context) {
        return VivoArtKeeperService.getInstance(context);
    }

    public static ISystemDefenceManager.Stub getSystemDefenceService(Context context) {
        return new SystemDefenceService(context);
    }

    public static ISuperResolutionManager.Stub getSuperResolutionService(Context context) {
        return SuperResolutionManagerService.getInstance(context);
    }

    public static IVivoBehaviorEngService.Stub getVivoBehaviorEngService(Context context) {
        return VivoBehaviorEngService.getInstance(context);
    }

    public static IMemcManager.Stub getMemcService(Context context) {
        return MemcManagerService.getInstance(context);
    }

    public static IPhysicalFlingManager.Stub getPhysicalFlingService(Context context) {
        return new PhysicalFlingService(context);
    }
}