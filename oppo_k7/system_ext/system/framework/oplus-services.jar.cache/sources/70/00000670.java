package com.android.server;

import android.app.NotificationManager;
import android.app.OplusUxIconConstants;
import android.common.IOplusCommonFeature;
import android.common.OplusFeatureCache;
import android.common.OplusFeatureList;
import android.common.OplusFeatureManager;
import android.content.Context;
import android.freeze.FreezeManagerHelp;
import android.freeze.IFreezeManagerHelp;
import android.freeze.IFreezeManagerService;
import android.hardware.biometrics.IOplusBiometricFaceConstantsEx;
import android.net.OplusWifiCommonConstant;
import android.net.wifi.owm.OwmBaseUtils;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.IOplusAbnormalComponentManager;
import com.android.server.am.IOplusActivityManagerDynamicLogConfigFeature;
import com.android.server.am.IOplusActivityManagerServiceEx;
import com.android.server.am.IOplusAppCrashClearManager;
import com.android.server.am.IOplusAppStartupManager;
import com.android.server.am.IOplusAthenaAmManager;
import com.android.server.am.IOplusBootPressureHolder;
import com.android.server.am.IOplusBootTraceManager;
import com.android.server.am.IOplusBroadcastManager;
import com.android.server.am.IOplusBroadcastStaticRegisterWhitelistManager;
import com.android.server.am.IOplusCpuExceptionMonitor;
import com.android.server.am.IOplusEapManager;
import com.android.server.am.IOplusEdgeTouchManager;
import com.android.server.am.IOplusFastAppManager;
import com.android.server.am.IOplusHansManager;
import com.android.server.am.IOplusJoystickManager;
import com.android.server.am.IOplusKeepAliveManager;
import com.android.server.am.IOplusKeyEventManager;
import com.android.server.am.IOplusKeyLayoutManager;
import com.android.server.am.IOplusMultiAppManager;
import com.android.server.am.IOplusOsenseCommonManager;
import com.android.server.am.IOplusPerfManager;
import com.android.server.am.IOplusResourcePreloadManager;
import com.android.server.am.IOplusSceneManager;
import com.android.server.am.IOplusSecurityPermissionManager;
import com.android.server.am.IOplusSystemUIInjector;
import com.android.server.am.OplusAbnormalComponentManager;
import com.android.server.am.OplusActivityManagerDynamicLogConfigFeature;
import com.android.server.am.OplusActivityManagerServiceEx;
import com.android.server.am.OplusAppCrashClearManager;
import com.android.server.am.OplusAppStartupManager;
import com.android.server.am.OplusAthenaAmManager;
import com.android.server.am.OplusBootPressureHolder;
import com.android.server.am.OplusBootTraceManager;
import com.android.server.am.OplusBroadcastManager;
import com.android.server.am.OplusBroadcastStaticRegisterWhitelistManager;
import com.android.server.am.OplusCpuExceptionMonitor;
import com.android.server.am.OplusEapManager;
import com.android.server.am.OplusEdgeTouchManagerService;
import com.android.server.am.OplusFastAppManager;
import com.android.server.am.OplusHansManager;
import com.android.server.am.OplusJoystickManager;
import com.android.server.am.OplusKeepAliveManager;
import com.android.server.am.OplusKeyEventManagerService;
import com.android.server.am.OplusKeyLayoutManagerService;
import com.android.server.am.OplusMultiAppManagerService;
import com.android.server.am.OplusOsenseCommonManager;
import com.android.server.am.OplusPerfManager;
import com.android.server.am.OplusResourcePreloadManager;
import com.android.server.am.OplusSceneManager;
import com.android.server.am.OplusSecurityPermissionManager;
import com.android.server.am.OplusSystemUIInjector;
import com.android.server.appop.IOplusAppOpsManager;
import com.android.server.appop.OplusAppOpsManager;
import com.android.server.audio.IOplusAlertSliderManager;
import com.android.server.audio.IOplusDualHeadPhoneFeature;
import com.android.server.audio.IOplusHeadsetFadeIn;
import com.android.server.audio.OplusAlertSliderManager;
import com.android.server.audio.OplusDualHeadPhoneFeature;
import com.android.server.audio.OplusHeadsetFadeIn;
import com.android.server.bluetooth.IOplusBluetoothManagerServiceExt;
import com.android.server.bluetooth.OplusBluetoothManagerServiceExtImpl;
import com.android.server.bluetooth.abnormaldetect.OplusBluetoothMonitorManager;
import com.android.server.bluetooth.interfaces.IOplusBluetoothMonitorManager;
import com.android.server.bluetooth.utils.OplusBtFeatureConfigHelper;
import com.android.server.connectivity.IOplusVpnHelper;
import com.android.server.connectivity.IOplusVpnManager;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.connectivity.OplusVpnHelper;
import com.android.server.connectivity.OplusVpnManager;
import com.android.server.connectivity.oplus.IOplusAutoConCaptivePortalControl;
import com.android.server.connectivity.oplus.IOplusDnsManagerHelper;
import com.android.server.connectivity.oplus.IOplusNetdEventListener;
import com.android.server.connectivity.oplus.IOplusNetworkNotificationManager;
import com.android.server.connectivity.oplus.OplusAutoConCaptivePortalControl;
import com.android.server.connectivity.oplus.OplusNetworkNotificationManager;
import com.android.server.connectivity.privatedns.OplusDnsManagerHelper;
import com.android.server.content.IOplusFeatureConfigManagerInternal;
import com.android.server.content.OplusFeatureConfigManagerService;
import com.android.server.display.ColorAIBrightManager;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.IColorAIBrightManager;
import com.android.server.display.IOplusBrightness;
import com.android.server.display.IOplusBrightnessCurveHelper;
import com.android.server.display.IOplusDisplayManagerServiceEx;
import com.android.server.display.IOplusDisplayPowerControllerFeature;
import com.android.server.display.IOplusEyeProtectManager;
import com.android.server.display.IOplusFeatureBrightness;
import com.android.server.display.IOplusFeatureBrightnessBarController;
import com.android.server.display.IOplusFeatureDCBacklight;
import com.android.server.display.IOplusFeatureHDREnhanceBrightness;
import com.android.server.display.IOplusFeatureMEMC;
import com.android.server.display.IOplusFeatureReduceBrightness;
import com.android.server.display.IOplusFeatureScreenRecordForceLowRefreshRate;
import com.android.server.display.IOplusMirageDisplayManager;
import com.android.server.display.IOplusVFXScreenEffectFeature;
import com.android.server.display.IOplusVisionCorrectionManager;
import com.android.server.display.IOplusWifiDisplayController;
import com.android.server.display.IOplusWifiDisplayUsageHelper;
import com.android.server.display.OplusBrightnessCurveHelper;
import com.android.server.display.OplusDisplayManagerServiceEx;
import com.android.server.display.OplusDisplayPowerControllerFeature;
import com.android.server.display.OplusFeatureBrightness;
import com.android.server.display.OplusFeatureBrightnessBarController;
import com.android.server.display.OplusFeatureDCBacklight;
import com.android.server.display.OplusFeatureHDREnhanceBrightness;
import com.android.server.display.OplusFeatureMEMC;
import com.android.server.display.OplusFeatureReduceBrightness;
import com.android.server.display.OplusFeatureScreenRecordForceLowRefreshRate;
import com.android.server.display.OplusMirageDisplayManagerService;
import com.android.server.display.OplusVFXScreenEffectFeature;
import com.android.server.display.OplusWifiDisplayController;
import com.android.server.display.OplusWifiDisplayUsageHelper;
import com.android.server.display.memc.SettingUtils;
import com.android.server.display.oplus.OplusEyeProtectManager;
import com.android.server.display.oplus.OplusVisionCorrectionManager;
import com.android.server.display.oplus.eyeprotect.util.OplusEyeProtectLcdInfoHelper;
import com.android.server.display.stat.BackLightStat;
import com.android.server.display.stat.IBackLightStat;
import com.android.server.doframe.IOplusSkipDoframeFeature;
import com.android.server.doframe.OplusSkipDoframeFeature;
import com.android.server.dynamicvsync.IOplusDynamicVsyncManagerFeature;
import com.android.server.dynamicvsync.OplusDynamicVsyncManagerFeature;
import com.android.server.engineer.OplusEngineerService;
import com.android.server.freeze.FreezeManagerService;
import com.android.server.inputmethod.IOplusInputMethodManagerServiceEx;
import com.android.server.inputmethod.IOplusVerificationCodeController;
import com.android.server.inputmethod.InputMethodManagerService;
import com.android.server.inputmethod.OplusInputMethodManagerService;
import com.android.server.inputmethod.OplusInputMethodManagerServiceEx;
import com.android.server.inputmethod.OplusVerificationCodeController;
import com.android.server.multiuser.IOplusMultiSystemManager;
import com.android.server.multiuser.OplusMultiSystemManager;
import com.android.server.net.IOplusDozeNetworkOptimization;
import com.android.server.net.IOplusNetworkManagement;
import com.android.server.net.IOplusNetworkPolicyManagerServiceEx;
import com.android.server.net.IOplusNetworkStatsEx;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.net.OplusDozeNetworkOptimization;
import com.android.server.net.OplusNetworkManagementService;
import com.android.server.net.OplusNetworkPolicyManagerServiceEx;
import com.android.server.net.OplusNetworkStatsServiceEx;
import com.android.server.notification.IOplusWLBManager;
import com.android.server.notification.OplusWLBManager;
import com.android.server.om.IOplusLanguageManager;
import com.android.server.om.OplusLanguageManager;
import com.android.server.operator.IOplusCarrierManager;
import com.android.server.operator.OplusCarrierManager;
import com.android.server.oplus.IOplusListManager;
import com.android.server.oplus.datanormalization.IOplusDataNormalizationManager;
import com.android.server.oplus.datanormalization.OplusDataNormalizationManager;
import com.android.server.oplus.nfdm.IOplusNewFeaturesDisplayingManager;
import com.android.server.oplus.nfdm.OplusNewFeaturesDisplayingManager;
import com.android.server.oplus.orms.config.OplusResourceManageDataStruct;
import com.android.server.performance.IOplusPerformanceService;
import com.android.server.performance.OplusPerformanceService;
import com.android.server.pm.CompatibilityHelper;
import com.android.server.pm.ICompatibilityHelper;
import com.android.server.pm.IOplusAppConfigManager;
import com.android.server.pm.IOplusAppDataMigrateManager;
import com.android.server.pm.IOplusAppDetectManager;
import com.android.server.pm.IOplusAppInstallProgressManager;
import com.android.server.pm.IOplusAppListInterceptManager;
import com.android.server.pm.IOplusAppQuickFreezeManager;
import com.android.server.pm.IOplusChildrenModeInstallManager;
import com.android.server.pm.IOplusClearDataProtectManager;
import com.android.server.pm.IOplusClipboardNotifyManager;
import com.android.server.pm.IOplusCustomizePmsFeature;
import com.android.server.pm.IOplusDataFreeManager;
import com.android.server.pm.IOplusDefaultAppPolicyManager;
import com.android.server.pm.IOplusDexMetadataManager;
import com.android.server.pm.IOplusDexOptimizeManager;
import com.android.server.pm.IOplusDexSceneManager;
import com.android.server.pm.IOplusDynamicFeatureManager;
import com.android.server.pm.IOplusFixupDataManager;
import com.android.server.pm.IOplusForbidHideOrDisableManager;
import com.android.server.pm.IOplusForbidUninstallAppManager;
import com.android.server.pm.IOplusFullmodeManager;
import com.android.server.pm.IOplusIconCachesManager;
import com.android.server.pm.IOplusIconPackManager;
import com.android.server.pm.IOplusInstallAccelerateManager;
import com.android.server.pm.IOplusInstallThreadsControlManager;
import com.android.server.pm.IOplusLanguageEnableManager;
import com.android.server.pm.IOplusMergedProcessSplitManager;
import com.android.server.pm.IOplusOptimizingProgressManager;
import com.android.server.pm.IOplusOtaDataManager;
import com.android.server.pm.IOplusPackageInstallInterceptManager;
import com.android.server.pm.IOplusPackageInstallStatisticManager;
import com.android.server.pm.IOplusPackageManagerNativeEx;
import com.android.server.pm.IOplusPackageManagerServiceEx;
import com.android.server.pm.IOplusPkgStartInfoManager;
import com.android.server.pm.IOplusPmsSupportedFunctionManager;
import com.android.server.pm.IOplusRemovableAppManager;
import com.android.server.pm.IOplusRuntimePermGrantPolicyManager;
import com.android.server.pm.IOplusSecurePayManager;
import com.android.server.pm.IOplusSecurityAnalysisBroadCastSender;
import com.android.server.pm.IOplusSellModeManager;
import com.android.server.pm.IOplusSensitivePermGrantPolicyManager;
import com.android.server.pm.IOplusSystemAppProtectManager;
import com.android.server.pm.IOplusThirdPartyAppSignCheckManager;
import com.android.server.pm.OplusAppConfigManager;
import com.android.server.pm.OplusAppDataMigrateManager;
import com.android.server.pm.OplusAppDetectManager;
import com.android.server.pm.OplusAppInstallProgressManager;
import com.android.server.pm.OplusAppListInterceptManager;
import com.android.server.pm.OplusAppQuickFreezeManager;
import com.android.server.pm.OplusChildrenModeInstallManager;
import com.android.server.pm.OplusClearDataProtectManager;
import com.android.server.pm.OplusClipboardNotifyManager;
import com.android.server.pm.OplusCustomizePmsFeature;
import com.android.server.pm.OplusDataFreeManager;
import com.android.server.pm.OplusDefaultAppPolicyManager;
import com.android.server.pm.OplusDexMetadataManager;
import com.android.server.pm.OplusDexOptimizeManager;
import com.android.server.pm.OplusDexSceneManager;
import com.android.server.pm.OplusDynamicFeatureManager;
import com.android.server.pm.OplusFixupDataManager;
import com.android.server.pm.OplusForbidHideOrDisableManager;
import com.android.server.pm.OplusForbidUninstallAppManager;
import com.android.server.pm.OplusFullmodeManager;
import com.android.server.pm.OplusIconCachesManager;
import com.android.server.pm.OplusIconPackManager;
import com.android.server.pm.OplusInstallAccelerateManager;
import com.android.server.pm.OplusInstallThreadsControlManager;
import com.android.server.pm.OplusLanguageEnableManager;
import com.android.server.pm.OplusMergedProcessSplitManager;
import com.android.server.pm.OplusOptimizingProgressManager;
import com.android.server.pm.OplusOtaDataManager;
import com.android.server.pm.OplusPackageInstallInterceptManager;
import com.android.server.pm.OplusPackageInstallStatisticManager;
import com.android.server.pm.OplusPackageManagerNativeEx;
import com.android.server.pm.OplusPackageManagerServiceEx;
import com.android.server.pm.OplusPkgStartInfoManager;
import com.android.server.pm.OplusPmsSupportedFunctionManager;
import com.android.server.pm.OplusRemovableAppManager;
import com.android.server.pm.OplusRuntimePermGrantPolicyManager;
import com.android.server.pm.OplusSecurePayManager;
import com.android.server.pm.OplusSecurityAnalysisBroadCastSender;
import com.android.server.pm.OplusSellModeManager;
import com.android.server.pm.OplusSensitivePermGrantPolicyManager;
import com.android.server.pm.OplusSystemAppProtectManager;
import com.android.server.pm.OplusThirdPartyAppSignCheckManager;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.permission.IOplusOsPermissionManagerServiceEx;
import com.android.server.pm.permission.IOplusPermSupportedFunctionManager;
import com.android.server.pm.permission.OplusOsPermissionManagerServiceEx;
import com.android.server.pm.permission.OplusPermSupportedFunctionManager;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.policy.IOplusAODScreenshotManager;
import com.android.server.policy.OplusAODScreenshotManager;
import com.android.server.power.CommonPowerManagerServiceEx;
import com.android.server.power.ICommonPowerManagerServiceEx;
import com.android.server.power.IOplusBatterySaveExtend;
import com.android.server.power.IOplusFeatureAOD;
import com.android.server.power.IOplusGuardElfFeature;
import com.android.server.power.IOplusPowerManagerServiceEx;
import com.android.server.power.IOplusPowerManagerServiceFeature;
import com.android.server.power.IOplusScreenOffOptimization;
import com.android.server.power.IOplusShutdownFeature;
import com.android.server.power.IOplusSilentRebootManager;
import com.android.server.power.IOplusWakeLockCheck;
import com.android.server.power.OplusBatterySaveExtend;
import com.android.server.power.OplusFeatureAOD;
import com.android.server.power.OplusGuardElfFeature;
import com.android.server.power.OplusPowerManagerServiceEx;
import com.android.server.power.OplusPowerManagerServiceFeature;
import com.android.server.power.OplusScreenOffOptimization;
import com.android.server.power.OplusShutdownFeature;
import com.android.server.power.OplusSilentRebootManager;
import com.android.server.power.OplusWakeLockCheck;
import com.android.server.power.PowerManagerService;
import com.android.server.sensor.IOplusSensorControlFeature;
import com.android.server.sensor.OplusSensorControlFeature;
import com.android.server.storage.IOplusStorageAllFileAccessManager;
import com.android.server.storage.OplusStorageAllFileAccessManager;
import com.android.server.storage.OplusStorageManagerFeature;
import com.android.server.usb.IOplusUsbDeviceFeature;
import com.android.server.usb.OplusUsbDeviceFeature;
import com.android.server.video.IOplusFeatureHBMMode;
import com.android.server.video.OplusFeatureHBMMode;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.ColorTouchNodeManagerService;
import com.android.server.wm.IOplusAccessControlLocalManager;
import com.android.server.wm.IOplusActivityTaskManagerServiceEx;
import com.android.server.wm.IOplusAmsUtilsFeatrue;
import com.android.server.wm.IOplusAppRunningControllerFeatrue;
import com.android.server.wm.IOplusAppStoreTraffic;
import com.android.server.wm.IOplusAppSwitchManager;
import com.android.server.wm.IOplusAthenaManager;
import com.android.server.wm.IOplusBackgroundTaskManagerService;
import com.android.server.wm.IOplusCarModeManager;
import com.android.server.wm.IOplusConfineModeManager;
import com.android.server.wm.IOplusDarkModeMaskManager;
import com.android.server.wm.IOplusFoldingAngleManager;
import com.android.server.wm.IOplusFullScreenDisplayManager;
import com.android.server.wm.IOplusGameRotationManager;
import com.android.server.wm.IOplusGameSpaceToolBoxManager;
import com.android.server.wm.IOplusGlobalDragAndDropManager;
import com.android.server.wm.IOplusInterceptLockScreenWindow;
import com.android.server.wm.IOplusLockTaskController;
import com.android.server.wm.IOplusMirageWindowManager;
import com.android.server.wm.IOplusPkgStateDetectFeature;
import com.android.server.wm.IOplusRefreshRatePolicy;
import com.android.server.wm.IOplusResolutionManagerFeature;
import com.android.server.wm.IOplusRotationOptimization;
import com.android.server.wm.IOplusSaveSurfaceManager;
import com.android.server.wm.IOplusScreenFrozenBooster;
import com.android.server.wm.IOplusScreenFrozenManager;
import com.android.server.wm.IOplusSeamlessAnimationManager;
import com.android.server.wm.IOplusSplitScreenManager;
import com.android.server.wm.IOplusStartingWindowManager;
import com.android.server.wm.IOplusTouchNodeManager;
import com.android.server.wm.IOplusWatermarkManager;
import com.android.server.wm.IOplusWindowAnimationManager;
import com.android.server.wm.IOplusWindowContainerControl;
import com.android.server.wm.IOplusWindowManagerServiceEx;
import com.android.server.wm.IOplusWmsUtilsFeatrue;
import com.android.server.wm.IOplusZoomWindowManager;
import com.android.server.wm.OplusAccessControlManagerService;
import com.android.server.wm.OplusActivityTaskManagerServiceEx;
import com.android.server.wm.OplusAmsUtilsFeatrue;
import com.android.server.wm.OplusAppRunningControllerFeatrue;
import com.android.server.wm.OplusAppStoreTraffic;
import com.android.server.wm.OplusAppSwitchManager;
import com.android.server.wm.OplusAthenaManager;
import com.android.server.wm.OplusBackgroundTaskManagerService;
import com.android.server.wm.OplusCarModeManager;
import com.android.server.wm.OplusConfineModeManagerService;
import com.android.server.wm.OplusDarkModeMaskManager;
import com.android.server.wm.OplusFoldingAngleManager;
import com.android.server.wm.OplusFullScreenDisplayManager;
import com.android.server.wm.OplusGameRotationService;
import com.android.server.wm.OplusGameSpaceToolBoxManager;
import com.android.server.wm.OplusGlobalDragAndDropManagerService;
import com.android.server.wm.OplusInterceptLockScreenWindow;
import com.android.server.wm.OplusLockTaskController;
import com.android.server.wm.OplusMirageWindowManagerService;
import com.android.server.wm.OplusPkgStateDetectFeature;
import com.android.server.wm.OplusRefreshRatePolicy;
import com.android.server.wm.OplusResolutionManagerFeature;
import com.android.server.wm.OplusRotationOptimization;
import com.android.server.wm.OplusSaveSurfaceManager;
import com.android.server.wm.OplusScreenFrozenBooster;
import com.android.server.wm.OplusScreenFrozenManager;
import com.android.server.wm.OplusSeamlessAnimationManager;
import com.android.server.wm.OplusSplitScreenManagerService;
import com.android.server.wm.OplusStartingWindowManager;
import com.android.server.wm.OplusWatermarkManager;
import com.android.server.wm.OplusWindowAnimationManager;
import com.android.server.wm.OplusWindowContainerControlService;
import com.android.server.wm.OplusWindowManagerServiceEx;
import com.android.server.wm.OplusWmsUtilsFeatrue;
import com.android.server.wm.OplusZoomWindowManagerService;
import com.android.server.wm.WindowManagerService;
import com.oplus.deepthinker.sdk.common.utils.DeepthinkerConstants;
import com.oplus.deepthinker.service.IOplusDeepThinkerExService;
import com.oplus.deepthinker.service.OplusDeepThinkerExService;
import com.oplus.media.MediaFile;
import com.oplus.neuron.NsConstants;
import com.oplus.screenmode.OplusScreenModeConstants;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class OplusServiceFactoryImpl extends OplusServiceFactory {
    private static final String TAG = "OplusServiceFactoryImpl";

    public <T extends IOplusCommonFeature> T getFeature(T def, Object... vars) {
        verityParams(def);
        if (!OplusFeatureManager.isSupport(def)) {
            return def;
        }
        switch (AnonymousClass1.$SwitchMap$android$common$OplusFeatureList$OplusIndex[def.index().ordinal()]) {
            case 1:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSystemServerEx(vars));
            case 2:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusActivityManagerServiceEx(vars));
            case 3:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusActivityTaskManagerServiceEx(vars));
            case 4:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusWindowManagerService(vars));
            case 5:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPackageManagerService(vars));
            case 6:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPackageManagerNative(vars));
            case 7:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPowerManagerService(vars));
            case 8:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFeatureAOD(vars));
            case 9:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusShutdownFeature(vars));
            case 10:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusBrightness(vars));
            case 11:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFeatureReduceBrightness(vars));
            case 12:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFeatureBrightness(vars));
            case 13:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDisplayPowerControllerFeature(vars));
            case 14:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFeatureDCBacklight(vars));
            case 15:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSkipDoframeFeature(vars));
            case 16:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFeatureHDREnhanceBrightness(vars));
            case 17:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFeatureScreenRecordForceLowRefreshRate(vars));
            case 18:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFeatureMEMC(vars));
            case 19:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusBrightnessCurveHelper(vars));
            case 20:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFeatureBrightnessBarController(vars));
            case 21:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getBackLightStat(vars));
            case 22:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOppoUsbDeviceManagerFeature(vars));
            case 23:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusStorageManagerFeature(vars));
            case 24:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusNewNetworkTimeUpdateServiceFeature(vars));
            case 25:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusActivityManagerDynamicLogConfigFeature(vars));
            case 26:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAppRunningControllerFeatrue(vars));
            case 27:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAmsUtilsFeatrue(vars));
            case 28:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusWmsUtilsFeatrue(vars));
            case 29:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusResolutionManagerFeature(vars));
            case 30:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusRefreshRatePolicy(vars));
            case 31:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPkgStateDetectFeature(vars));
            case 32:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSensorController(vars));
            case 33:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDynamicVsyncManagerFeature(vars));
            case 34:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusHeadsetFadeIn(vars));
            case 35:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAlertSliderManager(vars));
            case 36:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getCompatibilityHelper(vars));
            case 37:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPowerManagerServiceFeature(vars));
            case 38:
                return (T) OplusFeatureManager.getTraceMonitor(getFeatureConfigManagerService(vars));
            case DeepthinkerConstants.TYPE_SECURITY /* 39 */:
                return (T) OplusFeatureManager.getTraceMonitor(OplusEngineerService.getInstance());
            case 40:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusVpnHelperService(vars));
            case 41:
                return (T) OplusFeatureManager.getTraceMonitor(getOppoWifiDisplayUsageHelper(vars));
            case 42:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusWifiDisplayController(vars));
            case 43:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDnsManagerHelper(vars));
            case 44:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusScreenFrozenBooster(vars));
            case DeepthinkerConstants.TYPE_OFFICE /* 45 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAutoConCaptivePortalControl(vars));
            case 46:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusNetworkNotificationManager(vars));
            case 47:
                T feature = getOplusKeepAliveManager(vars);
                if (feature != null) {
                    return (T) OplusFeatureManager.getTraceMonitor(feature);
                }
                break;
            case 48:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusCpuExceptionMonitor(vars));
            case 49:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusRotationOptimization(vars));
            case 50:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDualHeadPhoneFeature(vars));
            case MediaFile.FILE_TYPE_FL /* 51 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOpluDexOptimizeManager(vars));
            case 52:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusOptimizingProgressManager(vars));
            case 53:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusBatteryServiceManager(vars));
            case 54:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPermissionManagerServiceEx(vars));
            case 55:
                return (T) OplusFeatureManager.getTraceMonitor(getCommonPowerManagerServiceEx(vars));
            case SettingUtils.BYPASS_TYPE /* 56 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusInputMethodManagerServiceEx(vars));
            case 57:
                return (T) OplusFeatureManager.getTraceMonitor(getColorDisplayManagerServiceEx(vars));
            case 58:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusNetworkPolicyManagerServiceEx(vars));
            case 59:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAccessControlManagerService(vars));
            case 60:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusInterceptLockScreenWindow(vars));
            case OplusUxIconConstants.IconTheme.DARKMODE_ICON_TRANSLATE_BIT_LENGTH /* 61 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusLockTaskController(vars));
            case 62:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAppStoreTraffic(vars));
            case 63:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFullmodeManager(vars));
            case 64:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDataFreeManager(vars));
            case 65:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusIconCachesManager(vars));
            case 66:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAppStartupManager(vars));
            case 67:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusChildrenModeInstallManager(vars));
            case 68:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDynamicFeatureManager(vars));
            case 69:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAppCrashClearManager(vars));
            case 70:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAppSwitchManager(vars));
            case 71:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusBroadcastStaticRegisterWhitelistManager(vars));
            case 72:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSellModeManager(vars));
            case 73:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAppInstallProgressManager(vars));
            case 74:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAppDataMigrateManager(vars));
            case 75:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusJoystickManager(vars));
            case 76:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusScreenOffOptimization(vars));
            case 77:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSplitScreenManagerService(vars));
            case 78:
                return (T) OplusFeatureManager.getTraceMonitor(getColorMultiAppManagerService(vars));
            case OplusWifiCommonConstant.OplusNetworkAgent.WIFI_BASE_SCORE_VALID /* 79 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusBatterySaveExtend(vars));
            case OplusResourceManageDataStruct.StatisticsConfig.DEFAULT_ABNORMAL_TIMES /* 80 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusGuardElfFeature(vars));
            case 81:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAppListInterceptManager(vars));
            case 82:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAppQuickFreezeManager(vars));
            case OplusEyeProtectLcdInfoHelper.DEV_INFO_LCD_NODE /* 83 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusRuntimePermGrantPolicyManager(vars));
            case 84:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDefaultAppPolicyManager(vars));
            case 85:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDozeNetworkOptimization(vars));
            case 86:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusClearDataProtectManager(vars));
            case 87:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPackageInstallStatisticManager(vars));
            case 88:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSecurePayManager(vars));
            case 89:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPackageInstallInterceptManager(vars));
            case 90:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSensitivePermGrantPolicyManager(vars));
            case 91:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusInstallThreadsControlManager(vars));
            case 92:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusThirdPartyAppSignCheckManager(vars));
            case 93:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusBroadcastManager(vars));
            case 94:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusForbidUninstallAppManager(vars));
            case 95:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusStrictModeManager(vars));
            case 96:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFastAppManager(vars));
            case 97:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSecurityPermissionManager(vars));
            case 98:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDynamicLogManager(vars));
            case 99:
                return (T) OplusFeatureManager.getTraceMonitor(getColorAIBrightManager(vars));
            case 100:
                return (T) OplusFeatureManager.getTraceMonitor(getColorVFXScreenEffectFeature(vars));
            case 101:
                return (T) OplusFeatureManager.getTraceMonitor(getColorLanguageManager(vars));
            case 102:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusForbidHideOrDisableManager(vars));
            case 103:
                return (T) OplusFeatureManager.getTraceMonitor(getColorLanguageEnableManager(vars));
            case 104:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPkgStartInfoManager(vars));
            case 105:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFixupDataManager(vars));
            case 106:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusOtaManager(vars));
            case 107:
                return (T) OplusFeatureManager.getTraceMonitor(getColorFullScreenDisplayManager(vars));
            case 108:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusRemovableAppManager(vars));
            case NsConstants.NFC_EVENT_TINDEX /* 109 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSystemAppProtectManager(vars));
            case 110:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSilentRebootManager(vars));
            case 111:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusWakeLockCheck(vars));
            case 112:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusZoomWindowManagerService(vars));
            case IOplusBiometricFaceConstantsEx.FACE_ACQUIRED_MOUTH_OCCLUSION /* 113 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPerfManager(vars));
            case IOplusBiometricFaceConstantsEx.FACE_ACQUIRED_NOT_FRONTAL_FACE /* 114 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAthenaAmManager(vars));
            case IOplusBiometricFaceConstantsEx.FACE_ACQUIRED_NOSE_OCCLUSION /* 115 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAthenaManager(vars));
            case IOplusBiometricFaceConstantsEx.FACE_ACQUIRED_MULTI_FACE /* 116 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusGameRotationManager(vars));
            case 117:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusWatermarkManager(vars));
            case 118:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusEapManager(vars));
            case 119:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDexMetadataManager(vars));
            case OplusScreenModeConstants.RATE_VAL_120 /* 120 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusStartingWindowManager(vars));
            case IOplusJoystickManager.MSG_SCREEN_OFF /* 121 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDarkModeMasekManager(vars));
            case 122:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDarkModeServiceManager(vars));
            case 123:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusEdgeTouchManager(vars));
            case 124:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusKeyLayoutManagerService(vars));
            case 125:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusConfineModeManager(vars));
            case 126:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPmsSupportedFunctionManager(vars));
            case 127:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPermSupportedFunctionManager(vars));
            case 128:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusStorageAllFileAccessManager(vars));
            case 129:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSaveSurfaceManager(vars));
            case 130:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusKeyEventManager(vars));
            case 131:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusWindowAnimationManager(vars));
            case 132:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusIconPackManager(vars));
            case 133:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusMergedProcessSplitManager(vars));
            case 134:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusMirageDisplayManager(vars));
            case 135:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusMirageWindowManager(vars));
            case 136:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusWindowContainerControlService(vars));
            case 137:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSeamlessAnimationManager(vars));
            case 138:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAbnormalComponentManager(vars));
            case 139:
                return (T) OplusFeatureManager.getTraceMonitor(getColorTouchNodeManager(vars));
            case 140:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusResourcePreloadManager(vars));
            case 141:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAppDetectManager(vars));
            case 142:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusGlobalDragAndDropManager(vars));
            case 143:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAppOpsManager(vars));
            case OplusScreenModeConstants.RATE_VAL_144 /* 144 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusListManager(vars));
            case 145:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDeepThinkerExService(vars));
            case 146:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAppConfigManager(vars));
            case 147:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusInstallAccelerateManager(vars));
            case 148:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusScreenFrozenManager(vars));
            case 149:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusBootPressureHolder(vars));
            case 150:
                return (T) OplusFeatureManager.getTraceMonitor(getOpVerificationCodeController(vars));
            case 151:
                return (T) OplusFeatureManager.getTraceMonitor(getIOplusBackgroundTaskManagerService(vars));
            case 152:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getFreezeManagerService(vars));
            case 153:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getFreezeManagerHelp(vars));
            case 154:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusVisionCorrectionManager(vars));
            case 155:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFeatureHBMMode(vars));
            case 156:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusGameSpaceToolBoxManager());
            case 157:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSecurityAnalysisBroadCastSender(vars));
            case 158:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSysStateManager(vars));
            case 159:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPerformanceService(vars));
            case 160:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusFoldingAngleManager(vars));
            case 161:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusCustomizePmsFeature(vars));
            case 162:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusCOTAFeature(vars));
            case 163:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusMultiSystemManager());
            case 164:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusCarrierManager(vars));
            case 165:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDataNormalizationManager(vars));
            case 166:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusNetworkStatsEx(vars));
            case 167:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusNetworkManagement(vars));
            case 168:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusNetdEventListener(vars));
            case 169:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusWLBManager());
            case 170:
                Slog.i(TAG, "get feature:" + def.index().name());
                return (T) OplusFeatureManager.getTraceMonitor(getOplusBluetoothManagerServiceExt(vars));
            case 171:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSystemUIInjector());
            case 172:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusHansManager(vars));
            case 173:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusSceneManager(vars));
            case 174:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusAODScreenshotManager());
            case 175:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusClipboardNotifyManager(vars));
            case 176:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusBootTraceManager(vars));
            case OwmBaseUtils.BAND_5_GHZ_LAST_CH_NUM /* 177 */:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusNewFeaturesDisplayingManager());
            case 178:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusNecConnectMonitor(vars));
            case 179:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusVpnManager(vars));
            case 180:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusCarModeManager(vars));
            case 181:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusDexSceneManager(vars));
            case 182:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusOsenseCommonManager(vars));
            case 183:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusPinFileManager(vars));
            case 184:
                return (T) OplusFeatureManager.getTraceMonitor(getOplusBluetoothMonitorManager(vars));
            default:
                Slog.i(TAG, "Unknow feature:" + def.index().name());
                break;
        }
        return def;
    }

    /* renamed from: com.android.server.OplusServiceFactoryImpl$1  reason: invalid class name */
    /* loaded from: classes.dex */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$common$OplusFeatureList$OplusIndex;

        static {
            int[] iArr = new int[OplusFeatureList.OplusIndex.values().length];
            $SwitchMap$android$common$OplusFeatureList$OplusIndex = iArr;
            try {
                iArr[OplusFeatureList.OplusIndex.IOplusSystemServerEx.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusActivityManagerServiceEx.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusActivityTaskManagerServiceEx.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusWindowManagerServiceEx.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPackageManagerServiceEx.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPackageManagerNativeEx.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPowerManagerServiceEx.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFeatureAOD.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusShutdownFeature.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusBrightness.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFeatureReduceBrightness.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFeatureBrightness.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDisplayPowerControllerFeature.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFeatureDCBacklight.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSkipDoframeFeature.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFeatureHDREnhanceBrightness.ordinal()] = 16;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFeatureScreenRecordForceLowRefreshRate.ordinal()] = 17;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFeatureMEMC.ordinal()] = 18;
            } catch (NoSuchFieldError e18) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusBrightnessCurveHelper.ordinal()] = 19;
            } catch (NoSuchFieldError e19) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFeatureBrightnessBarController.ordinal()] = 20;
            } catch (NoSuchFieldError e20) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IBackLightStat.ordinal()] = 21;
            } catch (NoSuchFieldError e21) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusUsbDeviceFeature.ordinal()] = 22;
            } catch (NoSuchFieldError e22) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusStorageManagerFeature.ordinal()] = 23;
            } catch (NoSuchFieldError e23) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusNewNetworkTimeUpdateServiceFeature.ordinal()] = 24;
            } catch (NoSuchFieldError e24) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusActivityManagerDynamicLogConfigFeature.ordinal()] = 25;
            } catch (NoSuchFieldError e25) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAppRunningControllerFeatrue.ordinal()] = 26;
            } catch (NoSuchFieldError e26) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAmsUtilsFeatrue.ordinal()] = 27;
            } catch (NoSuchFieldError e27) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusWmsUtilsFeatrue.ordinal()] = 28;
            } catch (NoSuchFieldError e28) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusResolutionManagerFeature.ordinal()] = 29;
            } catch (NoSuchFieldError e29) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusRefreshRatePolicy.ordinal()] = 30;
            } catch (NoSuchFieldError e30) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPkgStateDetectFeature.ordinal()] = 31;
            } catch (NoSuchFieldError e31) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSensorControlFeature.ordinal()] = 32;
            } catch (NoSuchFieldError e32) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDynamicVsyncManagerFeature.ordinal()] = 33;
            } catch (NoSuchFieldError e33) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusHeadsetFadeIn.ordinal()] = 34;
            } catch (NoSuchFieldError e34) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAlertSliderManager.ordinal()] = 35;
            } catch (NoSuchFieldError e35) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.ICompatibilityHelper.ordinal()] = 36;
            } catch (NoSuchFieldError e36) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPowerManagerServiceFeature.ordinal()] = 37;
            } catch (NoSuchFieldError e37) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFeatureConfigManagerInternal.ordinal()] = 38;
            } catch (NoSuchFieldError e38) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusEngineerService.ordinal()] = 39;
            } catch (NoSuchFieldError e39) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusVpnHelper.ordinal()] = 40;
            } catch (NoSuchFieldError e40) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusWifiDisplayUsageHelper.ordinal()] = 41;
            } catch (NoSuchFieldError e41) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusWifiDisplayController.ordinal()] = 42;
            } catch (NoSuchFieldError e42) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDnsManagerHelper.ordinal()] = 43;
            } catch (NoSuchFieldError e43) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusScreenFrozenBooster.ordinal()] = 44;
            } catch (NoSuchFieldError e44) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAutoConCaptivePortalControl.ordinal()] = 45;
            } catch (NoSuchFieldError e45) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusNetworkNotificationManager.ordinal()] = 46;
            } catch (NoSuchFieldError e46) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusKeepAliveManager.ordinal()] = 47;
            } catch (NoSuchFieldError e47) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusCpuExceptionMonitor.ordinal()] = 48;
            } catch (NoSuchFieldError e48) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusRotationOptimization.ordinal()] = 49;
            } catch (NoSuchFieldError e49) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDualHeadPhoneFeature.ordinal()] = 50;
            } catch (NoSuchFieldError e50) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDexOptimizeManager.ordinal()] = 51;
            } catch (NoSuchFieldError e51) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusOptimizingProgressManager.ordinal()] = 52;
            } catch (NoSuchFieldError e52) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusBatteryServiceFeature.ordinal()] = 53;
            } catch (NoSuchFieldError e53) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPermissionManagerServiceEx.ordinal()] = 54;
            } catch (NoSuchFieldError e54) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.ICommonPowerManagerServiceEx.ordinal()] = 55;
            } catch (NoSuchFieldError e55) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusInputMethodManagerServiceEx.ordinal()] = 56;
            } catch (NoSuchFieldError e56) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDisplayManagerServiceEx.ordinal()] = 57;
            } catch (NoSuchFieldError e57) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusNetworkPolicyManagerServiceEx.ordinal()] = 58;
            } catch (NoSuchFieldError e58) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAccessControlLocalManager.ordinal()] = 59;
            } catch (NoSuchFieldError e59) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusInterceptLockScreenWindow.ordinal()] = 60;
            } catch (NoSuchFieldError e60) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusLockTaskController.ordinal()] = 61;
            } catch (NoSuchFieldError e61) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAppStoreTraffic.ordinal()] = 62;
            } catch (NoSuchFieldError e62) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFullmodeManager.ordinal()] = 63;
            } catch (NoSuchFieldError e63) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDataFreeManager.ordinal()] = 64;
            } catch (NoSuchFieldError e64) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusIconCachesManager.ordinal()] = 65;
            } catch (NoSuchFieldError e65) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAppStartupManager.ordinal()] = 66;
            } catch (NoSuchFieldError e66) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusChildrenModeInstallManager.ordinal()] = 67;
            } catch (NoSuchFieldError e67) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDynamicFeatureManager.ordinal()] = 68;
            } catch (NoSuchFieldError e68) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAppCrashClearManager.ordinal()] = 69;
            } catch (NoSuchFieldError e69) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAppSwitchManager.ordinal()] = 70;
            } catch (NoSuchFieldError e70) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusBroadcastStaticRegisterWhitelistManager.ordinal()] = 71;
            } catch (NoSuchFieldError e71) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSellModeManager.ordinal()] = 72;
            } catch (NoSuchFieldError e72) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAppInstallProgressManager.ordinal()] = 73;
            } catch (NoSuchFieldError e73) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAppDataMigrateManager.ordinal()] = 74;
            } catch (NoSuchFieldError e74) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusJoystickManager.ordinal()] = 75;
            } catch (NoSuchFieldError e75) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusScreenOffOptimization.ordinal()] = 76;
            } catch (NoSuchFieldError e76) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSplitScreenManager.ordinal()] = 77;
            } catch (NoSuchFieldError e77) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusMultiAppManager.ordinal()] = 78;
            } catch (NoSuchFieldError e78) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusBatterySaveExtend.ordinal()] = 79;
            } catch (NoSuchFieldError e79) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusGuardElfFeature.ordinal()] = 80;
            } catch (NoSuchFieldError e80) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAppListInterceptManager.ordinal()] = 81;
            } catch (NoSuchFieldError e81) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAppQuickFreezeManager.ordinal()] = 82;
            } catch (NoSuchFieldError e82) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusRuntimePermGrantPolicyManager.ordinal()] = 83;
            } catch (NoSuchFieldError e83) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDefaultAppPolicyManager.ordinal()] = 84;
            } catch (NoSuchFieldError e84) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDozeNetworkOptimization.ordinal()] = 85;
            } catch (NoSuchFieldError e85) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusClearDataProtectManager.ordinal()] = 86;
            } catch (NoSuchFieldError e86) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPackageInstallStatisticManager.ordinal()] = 87;
            } catch (NoSuchFieldError e87) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSecurePayManager.ordinal()] = 88;
            } catch (NoSuchFieldError e88) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPackageInstallInterceptManager.ordinal()] = 89;
            } catch (NoSuchFieldError e89) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSensitivePermGrantPolicyManager.ordinal()] = 90;
            } catch (NoSuchFieldError e90) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusInstallThreadsControlManager.ordinal()] = 91;
            } catch (NoSuchFieldError e91) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusThirdPartyAppSignCheckManager.ordinal()] = 92;
            } catch (NoSuchFieldError e92) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusBroadcastManager.ordinal()] = 93;
            } catch (NoSuchFieldError e93) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusForbidUninstallAppManager.ordinal()] = 94;
            } catch (NoSuchFieldError e94) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusStrictModeManager.ordinal()] = 95;
            } catch (NoSuchFieldError e95) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFastAppManager.ordinal()] = 96;
            } catch (NoSuchFieldError e96) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSecurityPermissionManager.ordinal()] = 97;
            } catch (NoSuchFieldError e97) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDynamicLogManager.ordinal()] = 98;
            } catch (NoSuchFieldError e98) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IColorAIBrightManager.ordinal()] = 99;
            } catch (NoSuchFieldError e99) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusVFXScreenEffectFeature.ordinal()] = 100;
            } catch (NoSuchFieldError e100) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusLanguageManager.ordinal()] = 101;
            } catch (NoSuchFieldError e101) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusForbidHideOrDisableManager.ordinal()] = 102;
            } catch (NoSuchFieldError e102) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusLanguageEnableManager.ordinal()] = 103;
            } catch (NoSuchFieldError e103) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPkgStartInfoManager.ordinal()] = 104;
            } catch (NoSuchFieldError e104) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFixupDataManager.ordinal()] = 105;
            } catch (NoSuchFieldError e105) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusOtaDataManager.ordinal()] = 106;
            } catch (NoSuchFieldError e106) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFullScreenDisplayManager.ordinal()] = 107;
            } catch (NoSuchFieldError e107) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusRemovableAppManager.ordinal()] = 108;
            } catch (NoSuchFieldError e108) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSystemAppProtectManager.ordinal()] = 109;
            } catch (NoSuchFieldError e109) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSilentRebootManager.ordinal()] = 110;
            } catch (NoSuchFieldError e110) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusWakeLockCheck.ordinal()] = 111;
            } catch (NoSuchFieldError e111) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusZoomWindowManager.ordinal()] = 112;
            } catch (NoSuchFieldError e112) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPerfManager.ordinal()] = 113;
            } catch (NoSuchFieldError e113) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAthenaAmManager.ordinal()] = 114;
            } catch (NoSuchFieldError e114) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAthenaManager.ordinal()] = 115;
            } catch (NoSuchFieldError e115) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusGameRotationManager.ordinal()] = 116;
            } catch (NoSuchFieldError e116) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusWatermarkManager.ordinal()] = 117;
            } catch (NoSuchFieldError e117) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusEapManager.ordinal()] = 118;
            } catch (NoSuchFieldError e118) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDexMetadataManager.ordinal()] = 119;
            } catch (NoSuchFieldError e119) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusStartingWindowManager.ordinal()] = 120;
            } catch (NoSuchFieldError e120) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDarkModeMaskManager.ordinal()] = 121;
            } catch (NoSuchFieldError e121) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDarkModeServiceManager.ordinal()] = 122;
            } catch (NoSuchFieldError e122) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusEdgeTouchManager.ordinal()] = 123;
            } catch (NoSuchFieldError e123) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusKeyLayoutManager.ordinal()] = 124;
            } catch (NoSuchFieldError e124) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusConfineModeManager.ordinal()] = 125;
            } catch (NoSuchFieldError e125) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPmsSupportedFunctionManager.ordinal()] = 126;
            } catch (NoSuchFieldError e126) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPermSupportedFunctionManager.ordinal()] = 127;
            } catch (NoSuchFieldError e127) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusStorageAllFileAccessManager.ordinal()] = 128;
            } catch (NoSuchFieldError e128) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSaveSurfaceManager.ordinal()] = 129;
            } catch (NoSuchFieldError e129) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusKeyEventManager.ordinal()] = 130;
            } catch (NoSuchFieldError e130) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusWindowAnimationManager.ordinal()] = 131;
            } catch (NoSuchFieldError e131) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusIconPackManager.ordinal()] = 132;
            } catch (NoSuchFieldError e132) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusMergedProcessSplitManager.ordinal()] = 133;
            } catch (NoSuchFieldError e133) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusMirageDisplayManager.ordinal()] = 134;
            } catch (NoSuchFieldError e134) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusMirageWindowManager.ordinal()] = 135;
            } catch (NoSuchFieldError e135) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusWindowContainerControl.ordinal()] = 136;
            } catch (NoSuchFieldError e136) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSeamlessAnimationManager.ordinal()] = 137;
            } catch (NoSuchFieldError e137) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAbnormalComponentManager.ordinal()] = 138;
            } catch (NoSuchFieldError e138) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusTouchNodeManager.ordinal()] = 139;
            } catch (NoSuchFieldError e139) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusResourcePreloadManager.ordinal()] = 140;
            } catch (NoSuchFieldError e140) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAppDetectManager.ordinal()] = 141;
            } catch (NoSuchFieldError e141) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusGlobalDragAndDropManager.ordinal()] = 142;
            } catch (NoSuchFieldError e142) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAppOpsManager.ordinal()] = 143;
            } catch (NoSuchFieldError e143) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusListManager.ordinal()] = 144;
            } catch (NoSuchFieldError e144) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDeepThinkerExService.ordinal()] = 145;
            } catch (NoSuchFieldError e145) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAppConfigManager.ordinal()] = 146;
            } catch (NoSuchFieldError e146) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusInstallAccelerateManager.ordinal()] = 147;
            } catch (NoSuchFieldError e147) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusScreenFrozenManager.ordinal()] = 148;
            } catch (NoSuchFieldError e148) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusBootPressureHolder.ordinal()] = 149;
            } catch (NoSuchFieldError e149) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusVerificationCodeController.ordinal()] = 150;
            } catch (NoSuchFieldError e150) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusBackgroundTaskManagerService.ordinal()] = 151;
            } catch (NoSuchFieldError e151) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IFreezeManagerService.ordinal()] = 152;
            } catch (NoSuchFieldError e152) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IFreezeManagerHelp.ordinal()] = 153;
            } catch (NoSuchFieldError e153) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusVisionCorrectionManager.ordinal()] = 154;
            } catch (NoSuchFieldError e154) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFeatureHBMMode.ordinal()] = 155;
            } catch (NoSuchFieldError e155) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusGameSpaceToolBoxManager.ordinal()] = 156;
            } catch (NoSuchFieldError e156) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSecurityAnalysisBroadCastSender.ordinal()] = 157;
            } catch (NoSuchFieldError e157) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSysStateManager.ordinal()] = 158;
            } catch (NoSuchFieldError e158) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPerformanceService.ordinal()] = 159;
            } catch (NoSuchFieldError e159) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFoldingAngleManager.ordinal()] = 160;
            } catch (NoSuchFieldError e160) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusCustomizePmsFeature.ordinal()] = 161;
            } catch (NoSuchFieldError e161) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusCOTAFeature.ordinal()] = 162;
            } catch (NoSuchFieldError e162) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusMultiSystemManager.ordinal()] = 163;
            } catch (NoSuchFieldError e163) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusCarrierManager.ordinal()] = 164;
            } catch (NoSuchFieldError e164) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDataNormalizationManager.ordinal()] = 165;
            } catch (NoSuchFieldError e165) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusNetworkStatsEx.ordinal()] = 166;
            } catch (NoSuchFieldError e166) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusNetworkManagement.ordinal()] = 167;
            } catch (NoSuchFieldError e167) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusNetdEventListener.ordinal()] = 168;
            } catch (NoSuchFieldError e168) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusWLBManager.ordinal()] = 169;
            } catch (NoSuchFieldError e169) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusBluetoothManagerServiceExt.ordinal()] = 170;
            } catch (NoSuchFieldError e170) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSystemUIInjector.ordinal()] = 171;
            } catch (NoSuchFieldError e171) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusHansManager.ordinal()] = 172;
            } catch (NoSuchFieldError e172) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusSceneManager.ordinal()] = 173;
            } catch (NoSuchFieldError e173) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusAODScreenshotManager.ordinal()] = 174;
            } catch (NoSuchFieldError e174) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusClipboardNotifyManager.ordinal()] = 175;
            } catch (NoSuchFieldError e175) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusBootTraceManager.ordinal()] = 176;
            } catch (NoSuchFieldError e176) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusNewFeaturesDisplayingManager.ordinal()] = 177;
            } catch (NoSuchFieldError e177) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusNecConnectMonitor.ordinal()] = 178;
            } catch (NoSuchFieldError e178) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusVpnManager.ordinal()] = 179;
            } catch (NoSuchFieldError e179) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusCarModeManager.ordinal()] = 180;
            } catch (NoSuchFieldError e180) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusDexSceneManager.ordinal()] = 181;
            } catch (NoSuchFieldError e181) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusOsenseCommonManager.ordinal()] = 182;
            } catch (NoSuchFieldError e182) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusPinFileManager.ordinal()] = 183;
            } catch (NoSuchFieldError e183) {
            }
            try {
                $SwitchMap$android$common$OplusFeatureList$OplusIndex[OplusFeatureList.OplusIndex.IOplusFeatureBluetoothMonitorManager.ordinal()] = 184;
            } catch (NoSuchFieldError e184) {
            }
        }
    }

    private IOplusSystemServerEx getOplusSystemServerEx(Object... vars) {
        Slog.i(TAG, "getOplusSystemServerEx impl = " + this);
        verityParamsType("getOplusSystemServerEx", vars, 1, new Class[]{Context.class});
        Context context = (Context) vars[0];
        return new OplusSystemServerEx(context);
    }

    private IOplusActivityManagerServiceEx getOplusActivityManagerServiceEx(Object... vars) {
        verityParamsType("getOplusActivityManagerServiceEx", vars, 2, new Class[]{Context.class, ActivityManagerService.class});
        Context context = (Context) vars[0];
        ActivityManagerService ams = (ActivityManagerService) vars[1];
        Slog.i(TAG, "getOplusActivityManagerServiceEx size = " + vars.length + " context = " + context + " ams = " + ams);
        return new OplusActivityManagerServiceEx(context, ams);
    }

    private IOplusActivityTaskManagerServiceEx getOplusActivityTaskManagerServiceEx(Object... vars) {
        verityParamsType("getOplusActivityTaskManagerServiceEx", vars, 2, new Class[]{Context.class, ActivityTaskManagerService.class});
        Context context = (Context) vars[0];
        ActivityTaskManagerService atms = (ActivityTaskManagerService) vars[1];
        return new OplusActivityTaskManagerServiceEx(context, atms);
    }

    private IOplusWindowManagerServiceEx getOplusWindowManagerService(Object... vars) {
        verityParamsType("getOplusWindowManagerService", vars, 2, new Class[]{Context.class, WindowManagerService.class});
        Context context = (Context) vars[0];
        WindowManagerService wms = (WindowManagerService) vars[1];
        return new OplusWindowManagerServiceEx(context, wms);
    }

    private IOplusPackageManagerServiceEx getOplusPackageManagerService(Object... vars) {
        verityParamsType("getOplusPackageManagerService", vars, 2, new Class[]{Context.class, PackageManagerService.class});
        Context context = (Context) vars[0];
        PackageManagerService pms = (PackageManagerService) vars[1];
        return new OplusPackageManagerServiceEx(context, pms);
    }

    private IOplusPackageManagerNativeEx getOplusPackageManagerNative(Object... vars) {
        verityParamsType("getOplusPackageManagerNative", vars, 2, new Class[]{Context.class, PackageManagerService.class});
        Context context = (Context) vars[0];
        PackageManagerService pms = (PackageManagerService) vars[1];
        return new OplusPackageManagerNativeEx(context, pms);
    }

    private IOplusPowerManagerServiceEx getOplusPowerManagerService(Object... vars) {
        verityParamsType("getOplusPowerManagerService", vars, 2, new Class[]{Context.class, PowerManagerService.class});
        Context context = (Context) vars[0];
        PowerManagerService power = (PowerManagerService) vars[1];
        return new OplusPowerManagerServiceEx(context, power);
    }

    private IOplusFeatureAOD getOplusFeatureAOD(Object... vars) {
        return OplusFeatureAOD.getInstance(vars);
    }

    private IOplusShutdownFeature getOplusShutdownFeature(Object... vars) {
        verityParamsType("getOplusShutdownFeature", vars, 1, new Class[]{Context.class});
        Context context = (Context) vars[0];
        return OplusShutdownFeature.getInstance(context);
    }

    private IOplusBrightness getOplusBrightness(Object... vars) {
        return createOplusBrightness();
    }

    private IOplusBrightness createOplusBrightness() {
        try {
            Class<?> clazz = Class.forName("com.android.server.display.OplusBrightUtils");
            Method method = clazz.getDeclaredMethod("getInstance", new Class[0]);
            return (IOplusBrightness) method.invoke(null, new Object[0]);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return IOplusBrightness.DEFAULT;
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
            return IOplusBrightness.DEFAULT;
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
            return IOplusBrightness.DEFAULT;
        } catch (InvocationTargetException e4) {
            e4.printStackTrace();
            return IOplusBrightness.DEFAULT;
        }
    }

    private IOplusFeatureReduceBrightness getOplusFeatureReduceBrightness(Object... vars) {
        Slog.i(TAG, "vars is :" + vars.length);
        if (vars.length == 0) {
            try {
                Class<?> clazz = Class.forName("com.android.server.display.OplusFeatureReduceBrightness");
                Method method = clazz.getDeclaredMethod("getMethod", new Class[0]);
                return (IOplusFeatureReduceBrightness) method.invoke(null, new Object[0]);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return IOplusFeatureReduceBrightness.DEFAULT;
            } catch (IllegalAccessException e2) {
                e2.printStackTrace();
                return IOplusFeatureReduceBrightness.DEFAULT;
            } catch (NoSuchMethodException e3) {
                e3.printStackTrace();
                return IOplusFeatureReduceBrightness.DEFAULT;
            } catch (InvocationTargetException e4) {
                e4.printStackTrace();
                return IOplusFeatureReduceBrightness.DEFAULT;
            }
        }
        return OplusFeatureReduceBrightness.getInstance(vars);
    }

    private IOplusFeatureBrightness getOplusFeatureBrightness(Object... vars) {
        return OplusFeatureBrightness.getInstance(vars);
    }

    private IOplusDisplayPowerControllerFeature getOplusDisplayPowerControllerFeature(Object... vars) {
        verityParamsType("getOplusDisplayPowerControllerFeature", vars, 1, new Class[]{Context.class});
        Context context = (Context) vars[0];
        return OplusDisplayPowerControllerFeature.getInstance(context);
    }

    private IOplusFeatureDCBacklight getOplusFeatureDCBacklight(Object... vars) {
        return OplusFeatureDCBacklight.getInstance(vars);
    }

    private IOplusFeatureHDREnhanceBrightness getOplusFeatureHDREnhanceBrightness(Object... vars) {
        return OplusFeatureHDREnhanceBrightness.getInstance(vars);
    }

    private IOplusFeatureScreenRecordForceLowRefreshRate getOplusFeatureScreenRecordForceLowRefreshRate(Object... vars) {
        return OplusFeatureScreenRecordForceLowRefreshRate.getInstance(vars);
    }

    private IOplusFeatureMEMC getOplusFeatureMEMC(Object... vars) {
        return OplusFeatureMEMC.getInstance(vars);
    }

    private IOplusBrightnessCurveHelper getOplusBrightnessCurveHelper(Object... vars) {
        return OplusBrightnessCurveHelper.getInstance(new Object[0]);
    }

    private IOplusFeatureBrightnessBarController getOplusFeatureBrightnessBarController(Object... vars) {
        return OplusFeatureBrightnessBarController.getInstance(vars);
    }

    private IBackLightStat getBackLightStat(Object... vars) {
        Slog.i(TAG, "BackLightStat vars.len:" + vars.length);
        if (vars.length != 0) {
            Context context = (Context) vars[0];
            return BackLightStat.getInstance(context);
        }
        return null;
    }

    private IOplusUsbDeviceFeature getOppoUsbDeviceManagerFeature(Object... vars) {
        return OplusUsbDeviceFeature.getInstance();
    }

    private IOplusNewNetworkTimeUpdateServiceFeature getOplusNewNetworkTimeUpdateServiceFeature(Object... vars) {
        verityParamsType("getPswShutdownFeature", vars, 1, new Class[]{Context.class});
        Context context = (Context) vars[0];
        return OplusNewNetworkTimeUpdateServiceFeature.getInstance(context);
    }

    private IOplusStorageManagerFeature getOplusStorageManagerFeature(Object... vars) {
        verityParamsType("getOplusStorageManagerFeature", vars, 1, new Class[]{Context.class});
        Context context = (Context) vars[0];
        return OplusStorageManagerFeature.getInstance(context);
    }

    private IOplusSkipDoframeFeature getOplusSkipDoframeFeature(Object... vars) {
        return OplusSkipDoframeFeature.getInstance();
    }

    private IOplusActivityManagerDynamicLogConfigFeature getOplusActivityManagerDynamicLogConfigFeature(Object... vars) {
        return OplusActivityManagerDynamicLogConfigFeature.getInstance();
    }

    private IOplusAppRunningControllerFeatrue getOplusAppRunningControllerFeatrue(Object... vars) {
        return OplusAppRunningControllerFeatrue.getInstance();
    }

    private IOplusAmsUtilsFeatrue getOplusAmsUtilsFeatrue(Object... vars) {
        return OplusAmsUtilsFeatrue.getInstance();
    }

    private IOplusWmsUtilsFeatrue getOplusWmsUtilsFeatrue(Object... vars) {
        return OplusWmsUtilsFeatrue.getInstance();
    }

    private IOplusResolutionManagerFeature getOplusResolutionManagerFeature(Object... vars) {
        WindowManagerService wms = (WindowManagerService) vars[0];
        Context context = (Context) vars[1];
        return OplusResolutionManagerFeature.getInstance(wms, context);
    }

    private IOplusRefreshRatePolicy getOplusRefreshRatePolicy(Object... vars) {
        return OplusRefreshRatePolicy.newInstance(vars);
    }

    private IOplusPkgStateDetectFeature getOplusPkgStateDetectFeature(Object... vars) {
        return OplusPkgStateDetectFeature.getInstance();
    }

    private IOplusDynamicVsyncManagerFeature getOplusDynamicVsyncManagerFeature(Object... vars) {
        return OplusDynamicVsyncManagerFeature.getInstance();
    }

    private IOplusHeadsetFadeIn getOplusHeadsetFadeIn(Object... vars) {
        return OplusHeadsetFadeIn.getInstance(vars);
    }

    private IOplusSensorControlFeature getOplusSensorController(Object... vars) {
        return OplusSensorControlFeature.getInstance();
    }

    private IOplusAlertSliderManager getOplusAlertSliderManager(Object... vars) {
        return OplusAlertSliderManager.getInstance(vars);
    }

    private ICompatibilityHelper getCompatibilityHelper(Object... vars) {
        verityParamsType("getCompatibilityHelper", vars, 1, new Class[]{Context.class});
        Context context = (Context) vars[0];
        return CompatibilityHelper.getInstance(context);
    }

    private IOplusPowerManagerServiceFeature getOplusPowerManagerServiceFeature(Object... vars) {
        verityParamsType("getOplusPowerManagerServiceFeature", vars, 2, new Class[]{Context.class, PowerManagerService.class});
        Context context = (Context) vars[0];
        PowerManagerService pms = (PowerManagerService) vars[1];
        return OplusPowerManagerServiceFeature.getInstance(context, pms);
    }

    private IOplusFeatureConfigManagerInternal getFeatureConfigManagerService(Object... vars) {
        Slog.i(TAG, "getFeatureConfigManagerService");
        return OplusFeatureConfigManagerService.getInstance();
    }

    private IOplusVpnHelper getOplusVpnHelperService(Object... vars) {
        Context context = (Context) vars[0];
        Slog.i(TAG, "getOplusVpnHelperService");
        return OplusVpnHelper.getInstance(context);
    }

    private IOplusDualHeadPhoneFeature getOplusDualHeadPhoneFeature(Object... vars) {
        return OplusDualHeadPhoneFeature.getInstance(vars);
    }

    private IOplusWifiDisplayUsageHelper getOppoWifiDisplayUsageHelper(Object... vars) {
        return OplusWifiDisplayUsageHelper.getInstance();
    }

    private IOplusWifiDisplayController getOplusWifiDisplayController(Object... vars) {
        Context context = (Context) vars[0];
        return OplusWifiDisplayController.getInstance(context);
    }

    private IOplusDnsManagerHelper getOplusDnsManagerHelper(Object... vars) {
        Context context = (Context) vars[0];
        return OplusDnsManagerHelper.getInstance(context);
    }

    private IOplusScreenFrozenBooster getOplusScreenFrozenBooster(Object... vars) {
        return OplusScreenFrozenBooster.getInstance();
    }

    private IOplusAutoConCaptivePortalControl getOplusAutoConCaptivePortalControl(Object... vars) {
        Context context = (Context) vars[0];
        IOplusNetworkNotificationManager notifier = (IOplusNetworkNotificationManager) vars[1];
        NetworkAgentInfo nai = (NetworkAgentInfo) vars[2];
        boolean isDualWifiSta2Network = ((Boolean) vars[3]).booleanValue();
        return OplusAutoConCaptivePortalControl.getInstance(context, notifier, nai, isDualWifiSta2Network);
    }

    private IOplusNetworkNotificationManager getOplusNetworkNotificationManager(Object... vars) {
        Context context = (Context) vars[0];
        TelephonyManager t = (TelephonyManager) vars[1];
        NotificationManager n = (NotificationManager) vars[2];
        return OplusNetworkNotificationManager.getInstance(context, t, n);
    }

    private IOplusKeepAliveManager getOplusKeepAliveManager(Object... vars) {
        return OplusKeepAliveManager.getInstance();
    }

    private IOplusCpuExceptionMonitor getOplusCpuExceptionMonitor(Object... vars) {
        Slog.i(TAG, IOplusCpuExceptionMonitor.Name);
        verityParamsType("getOplusCpuExceptionMonitor", vars, 1, new Class[]{Context.class});
        return OplusCpuExceptionMonitor.getInstance();
    }

    private IOplusRotationOptimization getOplusRotationOptimization(Object... vars) {
        return OplusRotationOptimization.getInstance();
    }

    private IOplusDexOptimizeManager getOpluDexOptimizeManager(Object... vars) {
        Context context = (Context) vars[0];
        return OplusDexOptimizeManager.getInstance(context);
    }

    private IOplusOptimizingProgressManager getOplusOptimizingProgressManager(Object... vars) {
        Slog.i(TAG, "getOplusOptimizingProgressManager");
        Context context = (Context) vars[0];
        return OplusOptimizingProgressManager.getInstance(context);
    }

    private IOplusBatteryServiceFeature getOplusBatteryServiceManager(Object... vars) {
        Slog.i(TAG, "getOplusBatteryServiceManager");
        verityParamsType("getOplusBatteryServiceManager", vars, 0, new Class[0]);
        return OplusBatteryServiceFeature.getInstance();
    }

    private IOplusPackageManagerServiceEx getOplusPackageManagerServiceEx(Object... vars) {
        Slog.i(TAG, "getOplusPackageManagerServiceEx");
        verityParamsType("getOplusPackageManagerServiceEx", vars, 2, new Class[]{Context.class, PackageManagerService.class});
        Context context = (Context) vars[0];
        PackageManagerService pms = (PackageManagerService) vars[1];
        return new OplusPackageManagerServiceEx(context, pms);
    }

    private IOplusOsPermissionManagerServiceEx getOplusPermissionManagerServiceEx(Object... vars) {
        Slog.i(TAG, "getOplusPermissionManagerServiceEx");
        verityParamsType("getOplusPermissionManagerServiceEx", vars, 2, new Class[]{Context.class, PermissionManagerService.class});
        Context context = (Context) vars[0];
        PermissionManagerService permissionManagerService = (PermissionManagerService) vars[1];
        return new OplusOsPermissionManagerServiceEx(context, permissionManagerService);
    }

    private ICommonPowerManagerServiceEx getCommonPowerManagerServiceEx(Object... vars) {
        Slog.i(TAG, "getCommonPowerManagerServiceEx");
        verityParamsType("getCommonPowerManagerServiceEx", vars, 2, new Class[]{Context.class, PowerManagerService.class});
        Context context = (Context) vars[0];
        PowerManagerService powerMs = (PowerManagerService) vars[1];
        return new CommonPowerManagerServiceEx(context, powerMs);
    }

    private int getColorSystemThemeEx(Object... vars) {
        Slog.i(TAG, "getColorSystemThemeEx impl");
        return ColorSystemThemeEx.DEFAULT_SYSTEM_THEME;
    }

    private InputMethodManagerService getOplusInputMethodManagerService(Object... vars) {
        Slog.i(TAG, "getOplusInputMethodManagerService");
        verityParamsType("getOplusInputMethodManagerService", vars, 1, new Class[]{Context.class});
        Context context = (Context) vars[0];
        return new OplusInputMethodManagerService(context);
    }

    private IOplusNetworkPolicyManagerServiceEx getOplusNetworkPolicyManagerServiceEx(Object... vars) {
        Slog.i(TAG, "getOplusNetworkPolicyManagerServiceEx impl");
        verityParamsType("getOplusNetworkPolicyManagerServiceEx", vars, 2, new Class[]{Context.class, NetworkPolicyManagerService.class});
        Context context = (Context) vars[0];
        NetworkPolicyManagerService nms = (NetworkPolicyManagerService) vars[1];
        OplusNetworkPolicyManagerServiceEx mNetworkPolicyMS = new OplusNetworkPolicyManagerServiceEx(context, nms);
        OplusFeatureCache.set(mNetworkPolicyMS);
        return mNetworkPolicyMS;
    }

    private IOplusInputMethodManagerServiceEx getOplusInputMethodManagerServiceEx(Object... vars) {
        Slog.i(TAG, "getOplusInputMethodManagerServiceEx");
        return OplusInputMethodManagerServiceEx.getInstance();
    }

    private IOplusEyeProtectManager getOplusEyeProtectManager(Object... vars) {
        Slog.i(TAG, "getOplusEyeProtectManager");
        return OplusEyeProtectManager.getInstance();
    }

    private IOplusStartingWindowManager getOplusStartingWindowManager(Object... vars) {
        return OplusStartingWindowManager.getInstance();
    }

    private IOplusDarkModeMaskManager getOplusDarkModeMasekManager(Object... vars) {
        return OplusDarkModeMaskManager.getInstance();
    }

    private IOplusDarkModeServiceManager getOplusDarkModeServiceManager(Object... vars) {
        return OplusDarkModeServiceManager.getInstance();
    }

    private IOplusTouchNodeManager getColorTouchNodeManager(Object... vars) {
        Slog.i(TAG, "getColorTouchNodeManager");
        verityParamsType("getColorTouchNodeManager", vars, 0, new Class[0]);
        return ColorTouchNodeManagerService.getInstance();
    }

    private IOplusMirageDisplayManager getOplusMirageDisplayManager(Object... vars) {
        Slog.i(TAG, "getOplusMirageDisplayManager");
        verityParamsType("getOplusMirageDisplayManager", vars, 0, new Class[0]);
        return OplusMirageDisplayManagerService.getInstance();
    }

    private IOplusMirageWindowManager getOplusMirageWindowManager(Object... vars) {
        Slog.i(TAG, "getOplusMirageWindowManager");
        verityParamsType("getOplusMirageWindowManager", vars, 0, new Class[0]);
        return OplusMirageWindowManagerService.getInstance();
    }

    private IOplusWindowContainerControl getOplusWindowContainerControlService(Object... vars) {
        Slog.i(TAG, "getOplusWindowContainerControlService");
        verityParamsType("getOplusWindowContainerControlService", vars, 0, new Class[0]);
        return OplusWindowContainerControlService.getInstance();
    }

    private IOplusDisplayManagerServiceEx getColorDisplayManagerServiceEx(Object... vars) {
        Slog.i(TAG, "getColorDisplayManagerServiceEx");
        verityParamsType("getColorDisplayManagerServiceEx", vars, 2, new Class[]{Context.class, DisplayManagerService.class});
        Context context = (Context) vars[0];
        DisplayManagerService dms = (DisplayManagerService) vars[1];
        return new OplusDisplayManagerServiceEx(context, dms);
    }

    private IOplusAccessControlLocalManager getOplusAccessControlManagerService(Object... vars) {
        Slog.i(TAG, "getOplusAccessControlManagerService");
        verityParamsType("getOplusAccessControlManagerService", vars, 1, new Class[]{ActivityTaskManagerService.class});
        ActivityTaskManagerService atms = (ActivityTaskManagerService) vars[0];
        return new OplusAccessControlManagerService(atms);
    }

    private IOplusLockTaskController getOplusLockTaskController(Object... vars) {
        Slog.i(TAG, "getOplusLockTaskController");
        verityParamsType("getOplusLockTaskController", vars, 0, new Class[0]);
        return OplusLockTaskController.getInstance();
    }

    private IOplusInterceptLockScreenWindow getOplusInterceptLockScreenWindow(Object... vars) {
        Slog.i(TAG, "getOplusInterceptLockScreenWindow");
        verityParamsType("getOplusInterceptLockScreenWindow", vars, 0, new Class[0]);
        return OplusInterceptLockScreenWindow.getInstance();
    }

    private IOplusAppStoreTraffic getOplusAppStoreTraffic(Object... vars) {
        Slog.i(TAG, "getOplusAppStoreTraffic");
        verityParamsType("getOplusAppStoreTraffic", vars, 0, new Class[0]);
        return OplusAppStoreTraffic.getInstance();
    }

    private IOplusFullmodeManager getOplusFullmodeManager(Object... vars) {
        Slog.i(TAG, "getOplusFullmodeManager");
        verityParamsType("getOplusFullmodeManager", vars, 0, new Class[0]);
        return OplusFullmodeManager.getInstance();
    }

    private IOplusDataFreeManager getOplusDataFreeManager(Object... vars) {
        Slog.i(TAG, "getOplusDataFreeManager");
        verityParamsType("getOplusDataFreeManager", vars, 0, new Class[0]);
        return OplusDataFreeManager.getInstance();
    }

    private IOplusIconCachesManager getOplusIconCachesManager(Object... vars) {
        Slog.i(TAG, "getOplusIconCachesManager");
        verityParamsType("getOplusIconCachesManager", vars, 0, new Class[0]);
        return OplusIconCachesManager.getInstance();
    }

    private IOplusAppStartupManager getOplusAppStartupManager(Object... vars) {
        Slog.i(TAG, "getOplusAppStartupManager");
        verityParamsType("getOplusAppStartupManager", vars, 0, new Class[0]);
        return OplusAppStartupManager.getInstance();
    }

    private IOplusChildrenModeInstallManager getOplusChildrenModeInstallManager(Object... vars) {
        Slog.i(TAG, "getOplusChildrenModeInstallManager");
        verityParamsType("getOplusChildrenModeInstallManager", vars, 0, new Class[0]);
        return OplusChildrenModeInstallManager.getInstance();
    }

    private IOplusDynamicFeatureManager getOplusDynamicFeatureManager(Object... vars) {
        Slog.i(TAG, "getOplusDynamicFeatureManager");
        verityParamsType("getOplusDynamicFeatureManager", vars, 0, new Class[0]);
        return OplusDynamicFeatureManager.getInstance();
    }

    private IOplusAppCrashClearManager getOplusAppCrashClearManager(Object... vars) {
        Slog.i(TAG, "getOplusAppCrashClearManager");
        verityParamsType("getOplusAppCrashClearManager", vars, 0, new Class[0]);
        return OplusAppCrashClearManager.getInstance();
    }

    private IOplusAppSwitchManager getOplusAppSwitchManager(Object... vars) {
        Slog.i(TAG, "getOplusAppSwitchManager");
        verityParamsType("getOplusAppSwitchManager", vars, 0, new Class[0]);
        return OplusAppSwitchManager.getInstance();
    }

    private IOplusBroadcastStaticRegisterWhitelistManager getOplusBroadcastStaticRegisterWhitelistManager(Object... vars) {
        Slog.i(TAG, "getOplusBroadcastStaticRegisterWhitelistManager");
        verityParamsType("getOplusBroadcastStaticRegisterWhitelistManager", vars, 0, new Class[0]);
        return OplusBroadcastStaticRegisterWhitelistManager.getInstance();
    }

    private IOplusSellModeManager getOplusSellModeManager(Object... vars) {
        Slog.i(TAG, "getOplusSellModeManager");
        verityParamsType("getOplusSellModeManager", vars, 0, new Class[0]);
        return OplusSellModeManager.getInstance();
    }

    private IOplusAppInstallProgressManager getOplusAppInstallProgressManager(Object... vars) {
        Slog.i(TAG, "getOplusAppInstallProgressManager");
        verityParamsType("getOplusAppInstallProgressManager", vars, 0, new Class[0]);
        return OplusAppInstallProgressManager.getInstance();
    }

    private IOplusAppDataMigrateManager getOplusAppDataMigrateManager(Object... vars) {
        Slog.i(TAG, "getOplusAppDataMigrateManager");
        verityParamsType("getOplusAppDataMigrateManager", vars, 0, new Class[0]);
        return OplusAppDataMigrateManager.getInstance();
    }

    private IOplusJoystickManager getOplusJoystickManager(Object... vars) {
        Slog.i(TAG, "getOplusJoystickManager");
        verityParamsType("getOplusJoystickManager", vars, 0, new Class[0]);
        return OplusJoystickManager.getInstance();
    }

    private IOplusScreenOffOptimization getOplusScreenOffOptimization(Object... vars) {
        Slog.i(TAG, "getOplusScreenOffOptimization");
        verityParamsType("getOplusScreenOffOptimization", vars, 0, new Class[0]);
        return OplusScreenOffOptimization.getInstance();
    }

    private IOplusSplitScreenManager getOplusSplitScreenManagerService(Object... vars) {
        Slog.i(TAG, "getOplusSplitScreenManagerService");
        verityParamsType("getOplusSplitScreenManagerService", vars, 0, new Class[0]);
        return OplusSplitScreenManagerService.getInstance();
    }

    private IOplusMultiAppManager getColorMultiAppManagerService(Object... vars) {
        Slog.i(TAG, "getColorMultiAppManagerService");
        verityParamsType("getColorMultiAppManagerService", vars, 0, new Class[0]);
        return OplusMultiAppManagerService.getInstance();
    }

    private IOplusBatterySaveExtend getOplusBatterySaveExtend(Object... vars) {
        Slog.i(TAG, "getOplusBatterySaveExtend");
        verityParamsType("getOplusBatterySaveExtend", vars, 1, new Class[]{Context.class});
        Context context = (Context) vars[0];
        return OplusBatterySaveExtend.getInstance(context);
    }

    private IOplusGuardElfFeature getOplusGuardElfFeature(Object... vars) {
        Slog.i(TAG, "getOplusGuardElfFeature");
        verityParamsType("getOplusGuardElfFeature", vars, 0, new Class[0]);
        return OplusGuardElfFeature.getInstance();
    }

    private IOplusAppListInterceptManager getOplusAppListInterceptManager(Object... vars) {
        Slog.i(TAG, "getOplusAppListInterceptManager");
        verityParamsType("getOplusAppListInterceptManager", vars, 0, new Class[0]);
        return OplusAppListInterceptManager.getInstance();
    }

    private IOplusAppQuickFreezeManager getOplusAppQuickFreezeManager(Object... vars) {
        Slog.i(TAG, "getOplusAppQuickFreezeManager");
        verityParamsType("getOplusAppQuickFreezeManager", vars, 0, new Class[0]);
        return OplusAppQuickFreezeManager.getInstance();
    }

    private IOplusRuntimePermGrantPolicyManager getOplusRuntimePermGrantPolicyManager(Object... vars) {
        Slog.i(TAG, "getOplusRuntimePermGrantPolicyManager");
        verityParamsType("getOplusRuntimePermGrantPolicyManager", vars, 0, new Class[0]);
        return OplusRuntimePermGrantPolicyManager.getInstance();
    }

    private IOplusDefaultAppPolicyManager getOplusDefaultAppPolicyManager(Object... vars) {
        Slog.i(TAG, "getOplusDefaultAppPolicyManager");
        verityParamsType("getOplusDefaultAppPolicyManager", vars, 0, new Class[0]);
        return OplusDefaultAppPolicyManager.getInstance();
    }

    private IOplusDozeNetworkOptimization getOplusDozeNetworkOptimization(Object... vars) {
        Slog.i(TAG, "getOplusDozeNetworkOptimization");
        verityParamsType("getOplusDozeNetworkOptimization", vars, 0, new Class[0]);
        return new OplusDozeNetworkOptimization();
    }

    private IOplusClearDataProtectManager getOplusClearDataProtectManager(Object... vars) {
        Slog.i(TAG, "getOplusClearDataProtectManager");
        verityParamsType("getOplusClearDataProtectManager", vars, 0, new Class[0]);
        return OplusClearDataProtectManager.getInstance();
    }

    private IOplusPackageInstallStatisticManager getOplusPackageInstallStatisticManager(Object... vars) {
        Slog.i(TAG, "getOplusPackageInstallStatisticManager");
        verityParamsType("getOplusPackageInstallStatisticManager", vars, 0, new Class[0]);
        return OplusPackageInstallStatisticManager.getInstance();
    }

    private IOplusSecurePayManager getOplusSecurePayManager(Object... vars) {
        Slog.i(TAG, "getOplusSecurePayManager");
        verityParamsType("getOplusSecurePayManager", vars, 0, new Class[0]);
        return OplusSecurePayManager.getInstance();
    }

    private IOplusPackageInstallInterceptManager getOplusPackageInstallInterceptManager(Object... vars) {
        Slog.i(TAG, "getOplusPackageInstallInterceptManager");
        verityParamsType("getOplusPackageInstallInterceptManager", vars, 0, new Class[0]);
        return OplusPackageInstallInterceptManager.getInstance();
    }

    private IOplusSensitivePermGrantPolicyManager getOplusSensitivePermGrantPolicyManager(Object... vars) {
        Slog.i(TAG, "getOplusSensitivePermGrantPolicyManager");
        verityParamsType("getOplusSensitivePermGrantPolicyManager", vars, 0, new Class[0]);
        return OplusSensitivePermGrantPolicyManager.getInstance();
    }

    private IOplusInstallThreadsControlManager getOplusInstallThreadsControlManager(Object... vars) {
        Slog.i(TAG, "getOplusInstallThreadsControlManager");
        verityParamsType("getOplusInstallThreadsControlManager", vars, 0, new Class[0]);
        return OplusInstallThreadsControlManager.getInstance();
    }

    private IOplusThirdPartyAppSignCheckManager getOplusThirdPartyAppSignCheckManager(Object... vars) {
        Slog.i(TAG, "getOplusThirdPartyAppSignCheckManager");
        verityParamsType("getOplusThirdPartyAppSignCheckManager", vars, 0, new Class[0]);
        return OplusThirdPartyAppSignCheckManager.getInstance();
    }

    private IOplusBroadcastManager getOplusBroadcastManager(Object... vars) {
        Slog.i(TAG, "getOplusBroadcastManager");
        verityParamsType("getOplusBroadcastManager", vars, 0, new Class[0]);
        return OplusBroadcastManager.getInstance();
    }

    private IOplusAbnormalComponentManager getOplusAbnormalComponentManager(Object... vars) {
        Slog.i(TAG, "getOplusAbnormalComponentManager");
        verityParamsType("getOplusAbnormalComponentManager", vars, 0, new Class[0]);
        return OplusAbnormalComponentManager.getInstance();
    }

    private IOplusForbidUninstallAppManager getOplusForbidUninstallAppManager(Object... vars) {
        Slog.i(TAG, "getOplusForbidUninstallAppManager");
        verityParamsType("getOplusForbidUninstallAppManager", vars, 0, new Class[0]);
        return OplusForbidUninstallAppManager.getInstance();
    }

    private IOplusStrictModeManager getOplusStrictModeManager(Object... vars) {
        Slog.i(TAG, "getOplusStrictModeManager");
        verityParamsType("getOplusStrictModeManager", vars, 0, new Class[0]);
        return OplusStrictModeManager.getInstance();
    }

    private IOplusFastAppManager getOplusFastAppManager(Object... vars) {
        Slog.i(TAG, "getOplusFastAppManager");
        verityParamsType("getOplusFastAppManager", vars, 0, new Class[0]);
        return OplusFastAppManager.getInstance();
    }

    private IOplusSecurityPermissionManager getOplusSecurityPermissionManager(Object... vars) {
        Slog.i(TAG, "getOplusSecurityPermissionManager");
        verityParamsType("getOplusSecurityPermissionManager", vars, 0, new Class[0]);
        return OplusSecurityPermissionManager.getInstance();
    }

    private IOplusDynamicLogManager getOplusDynamicLogManager(Object... vars) {
        Slog.i(TAG, "getOplusDynamicLogManager");
        verityParamsType("getOplusDynamicLogManager", vars, 0, new Class[0]);
        return OplusDynamicLogManager.getInstance();
    }

    private IColorAIBrightManager getColorAIBrightManager(Object... vars) {
        Slog.i(TAG, "getColorAIBrightManager");
        verityParamsType("getColorAIBrightManager", vars, 0, new Class[0]);
        return ColorAIBrightManager.getInstance();
    }

    private IOplusVFXScreenEffectFeature getColorVFXScreenEffectFeature(Object... vars) {
        Slog.i(TAG, "getColorVFXScreenEffectFeature");
        verityParamsType("getColorVFXScreenEffectFeature", vars, 0, new Class[0]);
        return OplusVFXScreenEffectFeature.getInstance();
    }

    private IOplusLanguageManager getColorLanguageManager(Object... vars) {
        Slog.i(TAG, "getColorLanguageManager");
        verityParamsType("getColorLanguageManager", vars, 0, new Class[0]);
        return OplusLanguageManager.getInstance();
    }

    private IOplusForbidHideOrDisableManager getOplusForbidHideOrDisableManager(Object... vars) {
        Slog.i(TAG, "getOplusForbidHideOrDisableManager");
        verityParamsType("getOplusForbidHideOrDisableManager", vars, 0, new Class[0]);
        return OplusForbidHideOrDisableManager.getInstance();
    }

    private IOplusLanguageEnableManager getColorLanguageEnableManager(Object... vars) {
        Slog.i(TAG, "getColorLanguageEnableManager");
        verityParamsType("getColorLanguageEnableManager", vars, 0, new Class[0]);
        return OplusLanguageEnableManager.getInstance();
    }

    private IOplusPkgStartInfoManager getOplusPkgStartInfoManager(Object... vars) {
        Slog.i(TAG, "getOplusPkgStartInfoManager");
        verityParamsType("getOplusPkgStartInfoManager", vars, 0, new Class[0]);
        return OplusPkgStartInfoManager.getInstance();
    }

    private IOplusFixupDataManager getOplusFixupDataManager(Object... vars) {
        Slog.i(TAG, "getOplusFixupDataManager");
        verityParamsType("getOplusFixupDataManager", vars, 0, new Class[0]);
        return OplusFixupDataManager.getInstance();
    }

    private IOplusOtaDataManager getOplusOtaManager(Object... vars) {
        Slog.i(TAG, "getOplusOtaDataManager");
        verityParamsType("getOplusOtaDataManager", vars, 0, new Class[0]);
        return OplusOtaDataManager.getInstance();
    }

    private IOplusFullScreenDisplayManager getColorFullScreenDisplayManager(Object... vars) {
        Slog.i(TAG, "getColorFullScreenDisplayManager");
        verityParamsType("getColorFullScreenDisplayManager", vars, 0, new Class[0]);
        return OplusFullScreenDisplayManager.getInstance();
    }

    private IOplusRemovableAppManager getOplusRemovableAppManager(Object... vars) {
        Slog.i(TAG, "getOplusRemovableAppManager");
        verityParamsType("getOplusRemovableAppManager", vars, 0, new Class[0]);
        return OplusRemovableAppManager.getInstance();
    }

    private IOplusSystemAppProtectManager getOplusSystemAppProtectManager(Object... vars) {
        Slog.i(TAG, "getOplusSystemAppProtectManager");
        verityParamsType("getOplusSystemAppProtectManager", vars, 0, new Class[0]);
        return OplusSystemAppProtectManager.getInstance();
    }

    private IOplusSilentRebootManager getOplusSilentRebootManager(Object... vars) {
        Slog.i(TAG, "getOplusSilentRebootManager");
        verityParamsType("getOplusSilentRebootManager", vars, 0, new Class[0]);
        return OplusSilentRebootManager.getInstance();
    }

    private IOplusWakeLockCheck getOplusWakeLockCheck(Object... vars) {
        Slog.i(TAG, "getOplusWakeLockCheck");
        verityParamsType("getOplusWakeLockCheck", vars, 0, new Class[0]);
        return new OplusWakeLockCheck();
    }

    private IOplusZoomWindowManager getOplusZoomWindowManagerService(Object... vars) {
        Slog.i(TAG, "getOplusZoomWindowManagerService");
        verityParamsType("getOplusZoomWindowManagerService", vars, 0, new Class[0]);
        return OplusZoomWindowManagerService.getInstance();
    }

    private IOplusPerfManager getOplusPerfManager(Object... vars) {
        Slog.i(TAG, "getOplusPerfManager");
        verityParamsType("getOplusPerfManager", vars, 0, new Class[0]);
        return OplusPerfManager.getInstance();
    }

    private IOplusAthenaAmManager getOplusAthenaAmManager(Object... vars) {
        Slog.i(TAG, "getOplusAthenaAmManager");
        verityParamsType("getOplusAthenaAmManager", vars, 0, new Class[0]);
        return OplusAthenaAmManager.getInstance();
    }

    private IOplusAthenaManager getOplusAthenaManager(Object... vars) {
        Slog.i(TAG, "getOplusAthenaManager");
        verityParamsType("getOplusAthenaManager", vars, 0, new Class[0]);
        return OplusAthenaManager.getInstance();
    }

    private IOplusGameRotationManager getOplusGameRotationManager(Object... vars) {
        Slog.i(TAG, "getOplusGameRotationManager");
        verityParamsType("getOplusGameRotationManager", vars, 0, new Class[0]);
        return OplusGameRotationService.getInstance();
    }

    private IOplusWatermarkManager getOplusWatermarkManager(Object... vars) {
        Slog.i(TAG, "getOplusWatermarkManager");
        verityParamsType("getOplusWatermarkManager", vars, 0, new Class[0]);
        return OplusWatermarkManager.getInstance();
    }

    private IOplusEapManager getOplusEapManager(Object... vars) {
        Slog.i(TAG, "getOplusEapManager");
        verityParamsType("getOplusEapManager", vars, 0, new Class[0]);
        return OplusEapManager.getInstance();
    }

    private IOplusDexMetadataManager getOplusDexMetadataManager(Object... vars) {
        Slog.i(TAG, "getOplusDexMetadataManager");
        verityParamsType("getOplusDexMetadataManager", vars, 0, new Class[0]);
        return OplusDexMetadataManager.getInstance();
    }

    private IOplusKeyLayoutManager getOplusKeyLayoutManagerService(Object... vars) {
        Slog.i(TAG, "getOplusKeyLayoutManagerService");
        verityParamsType("getOplusKeyLayoutManagerService", vars, 0, new Class[0]);
        return OplusKeyLayoutManagerService.getInstance();
    }

    private IOplusEdgeTouchManager getOplusEdgeTouchManager(Object... vars) {
        Slog.i(TAG, "IOplusEdgeTouchManager");
        verityParamsType("getOplusEdgeTouchManager", vars, 0, new Class[0]);
        return OplusEdgeTouchManagerService.getInstance();
    }

    private IOplusConfineModeManager getOplusConfineModeManager(Object... vars) {
        Slog.i(TAG, IOplusConfineModeManager.NAME);
        verityParamsType("getOplusConfineModeManager", vars, 0, new Class[0]);
        return OplusConfineModeManagerService.getInstance();
    }

    private IOplusPermSupportedFunctionManager getOplusPermSupportedFunctionManager(Object... vars) {
        Slog.i(TAG, IOplusPermSupportedFunctionManager.NAME);
        verityParamsType("getOplusPermSupportedFunctionManager", vars, 0, new Class[0]);
        return OplusPermSupportedFunctionManager.getInstance();
    }

    private IOplusStorageAllFileAccessManager getOplusStorageAllFileAccessManager(Object... vars) {
        Slog.i(TAG, "IOplusStorageAllFileAccessManager");
        verityParamsType("getOplusStorageAllFileAccessManager", vars, 1, new Class[]{Context.class});
        Context context = (Context) vars[0];
        return new OplusStorageAllFileAccessManager(context);
    }

    private IOplusKeyEventManager getOplusKeyEventManager(Object... vars) {
        Slog.i(TAG, "getOplusKeyEventManager");
        verityParamsType("getOplusKeyEventManager", vars, 0, new Class[0]);
        return OplusKeyEventManagerService.getInstance();
    }

    private IOplusWindowAnimationManager getOplusWindowAnimationManager(Object... vars) {
        Slog.i(TAG, "IOplusWindowAnimationManager");
        verityParamsType("getOplusWindowAnimationManager", vars, 0, new Class[0]);
        return OplusWindowAnimationManager.getInstance();
    }

    private IOplusIconPackManager getOplusIconPackManager(Object... vars) {
        verityParamsType("getOplusIconPackManager", vars, 0, new Class[0]);
        return OplusIconPackManager.getInstance();
    }

    private IOplusMergedProcessSplitManager getOplusMergedProcessSplitManager(Object... vars) {
        Slog.i(TAG, IOplusMergedProcessSplitManager.NAME);
        verityParamsType("getOplusMergedProcessSplitManager", vars, 0, new Class[0]);
        return OplusMergedProcessSplitManager.getInstance();
    }

    private IOplusResourcePreloadManager getOplusResourcePreloadManager(Object... vars) {
        Slog.i(TAG, "getOplusResourcePreloadManager");
        verityParamsType("getOplusResourcePreloadManager", vars, 0, new Class[0]);
        return OplusResourcePreloadManager.getInstance();
    }

    private IOplusOsenseCommonManager getOplusOsenseCommonManager(Object... vars) {
        Slog.i(TAG, "getOplusOsenseCommonManager");
        verityParamsType("getOplusOsenseCommonManager", vars, 0, new Class[0]);
        return OplusOsenseCommonManager.getInstance();
    }

    private IOplusAppDetectManager getOplusAppDetectManager(Object... vars) {
        Slog.i(TAG, "getOplusAppDetectManager");
        verityParamsType("getOplusAppDetectManager", vars, 0, new Class[0]);
        return OplusAppDetectManager.getInstance();
    }

    private IOplusBootPressureHolder getOplusBootPressureHolder(Object... vars) {
        Slog.i(TAG, "getOplusBootPressureHolder");
        verityParamsType("getOplusBootPressureHolder", vars, 0, new Class[0]);
        return OplusBootPressureHolder.getInstance();
    }

    private IOplusGameSpaceToolBoxManager getOplusGameSpaceToolBoxManager() {
        return OplusGameSpaceToolBoxManager.getInstance();
    }

    private IOplusAODScreenshotManager getOplusAODScreenshotManager() {
        return OplusAODScreenshotManager.getInstance();
    }

    public int getColorSystemThemeEx(int theme) {
        warn("getColorSystemThemeEx impl");
        return ColorSystemThemeEx.DEFAULT_SYSTEM_THEME;
    }

    public InputMethodManagerService getOplusInputMethodManagerService(Context context) {
        warn("getOplusInputMethodManagerService");
        return new OplusInputMethodManagerService(context);
    }

    public IOplusEyeProtectManager getOplusEyeProtectManager() {
        return OplusEyeProtectManager.getInstance();
    }

    public IOplusDisplayManagerServiceEx getColorDisplayManagerServiceEx(Context context, DisplayManagerService dms) {
        return new OplusDisplayManagerServiceEx(context, dms);
    }

    public IOplusGlobalDragAndDropManager getOplusGlobalDragAndDropManager(Object... vars) {
        Slog.i(TAG, "getOplusGlobalDragAndDropManager");
        verityParamsType("getOplusGlobalDragAndDropManager", vars, 0, new Class[0]);
        return OplusGlobalDragAndDropManagerService.getInstance();
    }

    private IOplusAppOpsManager getOplusAppOpsManager(Object... vars) {
        Slog.i(TAG, "getOplusAppOpsManager");
        verityParamsType("getOplusAppOpsManager", vars, 0, new Class[0]);
        return OplusAppOpsManager.getInstance();
    }

    private IOplusPmsSupportedFunctionManager getOplusPmsSupportedFunctionManager(Object... vars) {
        Slog.i(TAG, "getOplusPmsSupportedFunctionManager");
        verityParamsType("getOplusPmsSupportedFunctionManager", vars, 0, new Class[0]);
        return OplusPmsSupportedFunctionManager.getInstance();
    }

    private IOplusSaveSurfaceManager getOplusSaveSurfaceManager(Object... vars) {
        return OplusSaveSurfaceManager.getInstance();
    }

    private IOplusSeamlessAnimationManager getOplusSeamlessAnimationManager(Object... vars) {
        return OplusSeamlessAnimationManager.getInstance();
    }

    private IOplusListManager getOplusListManager(Object... vars) {
        Slog.i(TAG, "getOplusListManager");
        verityParamsType("getOplusListManager", vars, 0, new Class[0]);
        return OplusListManagerImpl.getInstance();
    }

    private IOplusDeepThinkerExService getOplusDeepThinkerExService(Object... vars) {
        verityParamsType("getOplusDeepThinkerExService", vars, 1, new Class[]{Context.class});
        Context context = (Context) vars[0];
        return OplusDeepThinkerExService.getInstance(context);
    }

    private IOplusAppConfigManager getOplusAppConfigManager(Object... vars) {
        Slog.i(TAG, "getOplusAppConfigManager");
        verityParamsType("getOplusAppConfigManager", vars, 0, new Class[0]);
        return OplusAppConfigManager.getInstance();
    }

    private IOplusInstallAccelerateManager getOplusInstallAccelerateManager(Object... vars) {
        Slog.i(TAG, "getOplusInstallAccelerateManager");
        verityParamsType("getOplusInstallAccelerateManager", vars, 0, new Class[0]);
        return OplusInstallAccelerateManager.getInstance();
    }

    private IOplusScreenFrozenManager getOplusScreenFrozenManager(Object... vars) {
        return OplusScreenFrozenManager.getInstance();
    }

    private IOplusVerificationCodeController getOpVerificationCodeController(Object... vars) {
        return OplusVerificationCodeController.getInstance();
    }

    private IOplusBackgroundTaskManagerService getIOplusBackgroundTaskManagerService(Object... vars) {
        Slog.i(TAG, "getIOplusBackgroundTaskManagerService");
        verityParamsType("getIOplusBackgroundTaskManagerService", vars, 2, new Class[]{Context.class, ActivityTaskManagerService.class});
        return new OplusBackgroundTaskManagerService((Context) vars[0], (ActivityTaskManagerService) vars[1]);
    }

    private IFreezeManagerService getFreezeManagerService(Object... vars) {
        return new FreezeManagerService((Context) vars[0]);
    }

    private IFreezeManagerHelp getFreezeManagerHelp(Object... vars) {
        return FreezeManagerHelp.getInstance();
    }

    private IOplusFeatureHBMMode getOplusFeatureHBMMode(Object... vars) {
        return OplusFeatureHBMMode.getInstance(vars);
    }

    private IOplusSecurityAnalysisBroadCastSender getOplusSecurityAnalysisBroadCastSender(Object... vars) {
        Slog.i(TAG, "getOplusSecurityAnalysisBroadCastSender");
        verityParamsType("getOplusSecurityAnalysisBroadCastSender", vars, 1, new Class[]{Context.class});
        return OplusSecurityAnalysisBroadCastSender.getInstance((Context) vars[0]);
    }

    private IOplusVisionCorrectionManager getOplusVisionCorrectionManager(Object... vars) {
        return OplusVisionCorrectionManager.getInstance(vars);
    }

    private IOplusSysStateManager getOplusSysStateManager(Object... vars) {
        Slog.i(TAG, "getOplusSysStateManager");
        verityParamsType("getOplusSysStateManager", vars, 0, new Class[0]);
        return OplusSysStateManager.getInstance();
    }

    private IOplusPerformanceService getOplusPerformanceService(Object... vars) {
        return OplusPerformanceService.getInstance();
    }

    private IOplusFoldingAngleManager getOplusFoldingAngleManager(Object... vars) {
        return OplusFoldingAngleManager.getInstance();
    }

    private IOplusCustomizePmsFeature getOplusCustomizePmsFeature(Object... vars) {
        return new OplusCustomizePmsFeature();
    }

    private IOplusCOTAFeature getOplusCOTAFeature(Object... vars) {
        return new OplusCOTAFeature();
    }

    private IOplusMultiSystemManager getOplusMultiSystemManager() {
        return OplusMultiSystemManager.getInstance();
    }

    private IOplusCarrierManager getOplusCarrierManager(Object... vars) {
        return OplusCarrierManager.getInstance();
    }

    private IOplusDataNormalizationManager getOplusDataNormalizationManager(Object... vars) {
        verityParamsType("getOplusDataNormalizationManager", vars, 0, new Class[0]);
        return OplusDataNormalizationManager.getInstance();
    }

    private IOplusNetworkStatsEx getOplusNetworkStatsEx(Object... vars) {
        Slog.i(TAG, "getOplusNetworkStatsEx");
        verityParamsType("getOplusNetworkStatsEx", vars, 2, new Class[]{Context.class, NetworkStatsService.class});
        Context context = (Context) vars[0];
        NetworkStatsService nss = (NetworkStatsService) vars[1];
        OplusNetworkStatsServiceEx onss = OplusNetworkStatsServiceEx.getInstance(context, nss);
        OplusFeatureCache.set(onss);
        return onss;
    }

    private IOplusNetworkManagement getOplusNetworkManagement(Object... vars) {
        Slog.i(TAG, "getOplusNetworkManagement");
        verityParamsType("getOplusNetworkManagement", vars, 1, new Class[]{Context.class});
        Context context = (Context) vars[0];
        return OplusNetworkManagementService.getInstance(context);
    }

    private IOplusNetdEventListener getOplusNetdEventListener(Object... vars) {
        Slog.i(TAG, "getOplusNetdEventListener");
        verityParamsType("getOplusNetdEventListener", vars, 0, new Class[0]);
        return IOplusNetdEventListener.DEFAULT;
    }

    private IOplusWLBManager getOplusWLBManager() {
        return OplusWLBManager.getInstance();
    }

    private IOplusBluetoothManagerServiceExt getOplusBluetoothManagerServiceExt(Object... vars) {
        verityParamsType("getOplusBluetoothRecorder", vars, 2, new Class[]{Context.class, Object.class});
        Context context = (Context) vars[0];
        Object service = vars[1];
        return OplusBluetoothManagerServiceExtImpl.makeSingleInstance(service, context);
    }

    private IOplusSystemUIInjector getOplusSystemUIInjector() {
        return OplusSystemUIInjector.getInstance();
    }

    private IOplusHansManager getOplusHansManager(Object... vars) {
        Slog.i(TAG, "getOplusHansManager");
        verityParamsType("getOplusHansManager", vars, 0, new Class[0]);
        return OplusHansManager.getInstance();
    }

    private IOplusSceneManager getOplusSceneManager(Object... vars) {
        Slog.i(TAG, "getOplusSceneManager");
        verityParamsType("getOplusSceneManager", vars, 0, new Class[0]);
        return OplusSceneManager.getInstance();
    }

    private IOplusClipboardNotifyManager getOplusClipboardNotifyManager(Object... vars) {
        Slog.i(TAG, "getOplusClipboardNotifyManager");
        verityParamsType("getOplusClipboardNotifyManager", vars, 1, new Class[]{Context.class});
        return OplusClipboardNotifyManager.getInstance((Context) vars[0]);
    }

    private IOplusBootTraceManager getOplusBootTraceManager(Object... vars) {
        Slog.i(TAG, "getOplusBootTraceManager");
        verityParamsType("getOplusBootTraceManager", vars, 0, new Class[0]);
        return OplusBootTraceManager.getInstance();
    }

    private IOplusNewFeaturesDisplayingManager getOplusNewFeaturesDisplayingManager() {
        return OplusNewFeaturesDisplayingManager.getInstance();
    }

    private IOplusNecConnectMonitor getOplusNecConnectMonitor(Object... vars) {
        verityParamsType("getOplusNecConnectMonitor", vars, 1, new Class[]{Context.class});
        return OplusNecConnectMonitor.getInstance((Context) vars[0]);
    }

    private IOplusVpnManager getOplusVpnManager(Object... vars) {
        Slog.i(TAG, "getOplusVpnManager");
        verityParamsType("getOplusVpnManager", vars, 1, new Class[]{Context.class});
        Context context = (Context) vars[0];
        return OplusVpnManager.getInstance(context);
    }

    private IOplusCarModeManager getOplusCarModeManager(Object... vars) {
        Slog.i(TAG, "getOplusCarModeManager");
        verityParamsType("getOplusCarModeManager", vars, 0, new Class[0]);
        return OplusCarModeManager.getInstance();
    }

    private IOplusDexSceneManager getOplusDexSceneManager(Object... vars) {
        verityParamsType("getOplusDexSceneManager", vars, 0, new Class[0]);
        return OplusDexSceneManager.getInstance();
    }

    private IOplusPinFileManager getOplusPinFileManager(Object... vars) {
        Slog.i(TAG, "getOplusPinFileManager");
        verityParamsType("getOplusPinFileManager", vars, 0, new Class[0]);
        return OplusPinFileManager.getInstance();
    }

    private IOplusBluetoothMonitorManager getOplusBluetoothMonitorManager(Object... vars) {
        if (OplusBtFeatureConfigHelper.hasBluetoothAbnormalDetect()) {
            verityParamsType("getOplusBluetoothMonitorManager", vars, 2, new Class[]{Context.class, Object.class});
            Context context = (Context) vars[0];
            Object service = vars[1];
            return OplusBluetoothMonitorManager.getInstance(service, context);
        }
        return IOplusBluetoothMonitorManager.DEFAULT;
    }
}