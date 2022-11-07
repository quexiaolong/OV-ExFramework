package com.android.server;

import android.app.AlarmManager;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManagerInternal;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.UserManager;
import android.util.NtpTrustedTime;
import android.view.IWindowManager;
import android.view.RemoteAnimationAdapter;
import com.android.server.BatteryService;
import com.android.server.DropBoxManagerService;
import com.android.server.am.ActiveServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.BatteryStatsService;
import com.android.server.am.BroadcastQueue;
import com.android.server.am.IVivoActiveService;
import com.android.server.am.IVivoActivityManagerShellCommand;
import com.android.server.am.IVivoAms;
import com.android.server.am.IVivoAppExitInfoTracker;
import com.android.server.am.IVivoBatteryStatsService;
import com.android.server.am.IVivoBroadcastQueue;
import com.android.server.am.IVivoCachedAppOptimizer;
import com.android.server.am.IVivoOomAdjuster;
import com.android.server.am.IVivoPcbaCotrol;
import com.android.server.am.IVivoPendingIntentController;
import com.android.server.am.IVivoProcessList;
import com.android.server.am.IVivoProcessRecord;
import com.android.server.am.IVivoServiceRecord;
import com.android.server.am.IVivoUserController;
import com.android.server.am.ProcessFreezeManager;
import com.android.server.am.ProcessList;
import com.android.server.am.ProcessRecord;
import com.android.server.am.VivoActiveServiceImpl;
import com.android.server.am.VivoActivityManagerShellCommandImpl;
import com.android.server.am.VivoAmsImpl;
import com.android.server.am.VivoAppExitInfoTrackerImpl;
import com.android.server.am.VivoBatteryStatsServiceImpl;
import com.android.server.am.VivoBroadcastQueueImpl;
import com.android.server.am.VivoCachedAppOptimizer;
import com.android.server.am.VivoOomAdjusterImpl;
import com.android.server.am.VivoPcbaCotrolImpl;
import com.android.server.am.VivoPendingIntentControllerImpl;
import com.android.server.am.VivoProcessListImpl;
import com.android.server.am.VivoProcessRecordImpl;
import com.android.server.am.VivoServiceRecordImpl;
import com.android.server.am.VivoUserControllerImpl;
import com.android.server.am.frozen.FrozenInjectorImpl;
import com.android.server.audio.AudioService;
import com.android.server.audio.IVivoAudioService;
import com.android.server.audio.VivoAudioServiceImpl;
import com.android.server.autofill.IVivoAutofillService;
import com.android.server.autofill.VivoAutofillServiceImpl;
import com.android.server.backup.IVivoAppMetadataBackupWriter;
import com.android.server.backup.IVivoFullBackupEngine;
import com.android.server.backup.IVivoFullBackupRestoreObserverUtils;
import com.android.server.backup.IVivoFullBackupTask;
import com.android.server.backup.IVivoFullBackupUtils;
import com.android.server.backup.IVivoFullRestoreEngine;
import com.android.server.backup.IVivoFullRestoreEngineThread;
import com.android.server.backup.IVivoPerformAdbBackupTask;
import com.android.server.backup.IVivoPerformAdbRestoreTask;
import com.android.server.backup.IVivoRestoreUtils;
import com.android.server.backup.IVivoTarBackupReader;
import com.android.server.backup.IVivoTrampoline;
import com.android.server.backup.IVivoUserBackupManagerService;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.VivoAppMetadataBackupWriterImpl;
import com.android.server.backup.VivoFullBackupEngineImpl;
import com.android.server.backup.VivoFullBackupRestoreObserverUtilsImpl;
import com.android.server.backup.VivoFullBackupTaskImpl;
import com.android.server.backup.VivoFullBackupUtilsImpl;
import com.android.server.backup.VivoFullRestoreEngineImpl;
import com.android.server.backup.VivoFullRestoreEngineThreadImpl;
import com.android.server.backup.VivoPerformAdbBackupTaskImpl;
import com.android.server.backup.VivoPerformAdbRestoreTaskImpl;
import com.android.server.backup.VivoRestoreUtilsImpl;
import com.android.server.backup.VivoTarBackupReaderImpl;
import com.android.server.backup.VivoTrampolineImpl;
import com.android.server.backup.VivoUserBackupManagerServiceImpl;
import com.android.server.biometrics.face.FaceService;
import com.android.server.biometrics.face.IVivoFaceService;
import com.android.server.biometrics.face.VivoFaceServiceImpl;
import com.android.server.biometrics.fingerprint.FingerprintService;
import com.android.server.biometrics.fingerprint.IVivoFingerprintService;
import com.android.server.biometrics.fingerprint.VivoFingerprintServiceImpl;
import com.android.server.clipboard.ClipboardService;
import com.android.server.clipboard.IVivoClipboardService;
import com.android.server.clipboard.VivoClipboardServiceImpl;
import com.android.server.connectivity.IVivoVpn;
import com.android.server.connectivity.VivoVpnImpl;
import com.android.server.connectivity.Vpn;
import com.android.server.content.ContentService;
import com.android.server.content.IVivoSyncManager;
import com.android.server.content.SyncManager;
import com.android.server.content.VivoContentServiceImpl;
import com.android.server.content.VivoSyncManagerImpl;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.devicepolicy.IVivoCustomDpms;
import com.android.server.devicepolicy.VivoCustomDpmsImpl;
import com.android.server.display.ColorFade;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.DisplayPowerController;
import com.android.server.display.DisplayPowerState;
import com.android.server.display.IVivoColorFade;
import com.android.server.display.IVivoDisplayManagerService;
import com.android.server.display.IVivoDisplayPowerController;
import com.android.server.display.IVivoDisplayPowerState;
import com.android.server.display.IVivoLocalDisplayAdapter;
import com.android.server.display.IVivoLocalDisplayDevice;
import com.android.server.display.IVivoLogicalDisplay;
import com.android.server.display.IVivoRampAnimator;
import com.android.server.display.IVivoVirtualDisplayAdapter;
import com.android.server.display.IVivoWifiDisplayAdapter;
import com.android.server.display.LocalDisplayAdapter;
import com.android.server.display.LogicalDisplay;
import com.android.server.display.RampAnimator;
import com.android.server.display.VirtualDisplayAdapter;
import com.android.server.display.VivoColorFadeImpl;
import com.android.server.display.VivoDisplayManagerServiceImpl;
import com.android.server.display.VivoDisplayPowerControllerImpl;
import com.android.server.display.VivoDisplayPowerStateImpl;
import com.android.server.display.VivoLocalDisplayAdapterImpl;
import com.android.server.display.VivoLocalDisplayDeviceImpl;
import com.android.server.display.VivoLogicalDisplayImpl;
import com.android.server.display.VivoRampAnimatorImpl;
import com.android.server.display.VivoVirtualDisplayAdapterImpl;
import com.android.server.display.VivoWifiDisplayAdapterImpl;
import com.android.server.display.color.ColorDisplayService;
import com.android.server.display.color.IVivoColorDisplayService;
import com.android.server.display.color.VivoColorDisplayServiceImpl;
import com.android.server.input.IVivoInputManagerService;
import com.android.server.input.IVivoWindowManagerCallbacks;
import com.android.server.input.InputManagerService;
import com.android.server.input.VivoInputManagerServiceImpl;
import com.android.server.inputmethod.IVivoInputMethodManagerService;
import com.android.server.inputmethod.IVivoInputMethodManagerServiceLifecycle;
import com.android.server.inputmethod.InputMethodManagerService;
import com.android.server.inputmethod.VivoInputMethodManagerServiceImpl;
import com.android.server.inputmethod.VivoInputMethodManagerServiceLifecycleImpl;
import com.android.server.job.IVivoJobSchedulerService;
import com.android.server.job.VivoJobSchedulerServiceImpl;
import com.android.server.lights.IVivoLightImpl;
import com.android.server.lights.LightsService;
import com.android.server.lights.VivoLightImplImpl;
import com.android.server.location.IVivoGnssLocationProvider;
import com.android.server.location.IVivoLocationManagerService;
import com.android.server.location.LocationManagerService;
import com.android.server.location.VivoGnssLocationProviderExt;
import com.android.server.location.VivoLocationManagerServiceExt;
import com.android.server.lockmonitor.IVivoFrameworkLockMonitor;
import com.android.server.locksettings.IVivoLockSettingsService;
import com.android.server.locksettings.IVivoLockSettingsShellCommand;
import com.android.server.locksettings.IVivoLockSettingsStorage;
import com.android.server.locksettings.IVivoLockSettingsStrongAuth;
import com.android.server.locksettings.IVivoSyntheticPasswordManager;
import com.android.server.locksettings.LockSettingsService;
import com.android.server.locksettings.LockSettingsStorage;
import com.android.server.locksettings.LockSettingsStrongAuth;
import com.android.server.locksettings.SyntheticPasswordManager;
import com.android.server.locksettings.VivoLockSettingsServiceImpl;
import com.android.server.locksettings.VivoLockSettingsShellCommandImpl;
import com.android.server.locksettings.VivoLockSettingsStorageImpl;
import com.android.server.locksettings.VivoLockSettingsStrongAuthImpl;
import com.android.server.locksettings.VivoSyntheticPasswordManagerImpl;
import com.android.server.media.IVivoMediaSessionService;
import com.android.server.media.MediaSessionService;
import com.android.server.media.VivoMediaSessionServiceImpl;
import com.android.server.net.IVivoNetworkPolicyManagerService;
import com.android.server.net.IVivoNetworkStatsAccess;
import com.android.server.net.IVivoNetworkStatsService;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.net.VivoNetworkPolicyManagerServiceImpl;
import com.android.server.net.VivoNetworkStatsAccessImpl;
import com.android.server.net.VivoNetworkStatsServiceImpl;
import com.android.server.networktime.IVivoNetworkTimeUpdateService;
import com.android.server.networktime.VivoNetworkTimeUpdateServiceImpl;
import com.android.server.notification.ConditionProviders;
import com.android.server.notification.IVivoConditionProviders;
import com.android.server.notification.IVivoGroupHelper;
import com.android.server.notification.IVivoNotificationManagerService;
import com.android.server.notification.IVivoPreferencesHelper;
import com.android.server.notification.IVivoRankingHelper;
import com.android.server.notification.IVivoValidateNotificationPeople;
import com.android.server.notification.IVivoZenModeHelper;
import com.android.server.notification.NotificationManagerService;
import com.android.server.notification.PreferencesHelper;
import com.android.server.notification.RankingHelper;
import com.android.server.notification.VivoConditionProvidersImpl;
import com.android.server.notification.VivoGroupHelperImpl;
import com.android.server.notification.VivoNotificationManagerServiceImpl;
import com.android.server.notification.VivoPreferencesHelperImpl;
import com.android.server.notification.VivoRankingHelperImpl;
import com.android.server.notification.VivoValidateNotificationPeopleImpl;
import com.android.server.notification.VivoZenModeHelperImpl;
import com.android.server.notification.ZenModeHelper;
import com.android.server.pm.IVivoComponentResolver;
import com.android.server.pm.IVivoPackageInstallerSession;
import com.android.server.pm.IVivoPms;
import com.android.server.pm.IVivoSettings;
import com.android.server.pm.IVivoUms;
import com.android.server.pm.IVivoUserDataPreparer;
import com.android.server.pm.PackageDexOptimizer;
import com.android.server.pm.PackageInstallerSession;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.VivoComponentResolverImpl;
import com.android.server.pm.VivoPackageInstallerSessionImpl;
import com.android.server.pm.VivoPmsImpl;
import com.android.server.pm.VivoSettingsImpl;
import com.android.server.pm.VivoUmsImpl;
import com.android.server.pm.VivoUserDataPreparerImpl;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.IVivoDexManager;
import com.android.server.pm.dex.PackageDexUsage;
import com.android.server.pm.dex.VivoDexManagerImpl;
import com.android.server.pm.permission.IVivoOneTimePermissionUserManager;
import com.android.server.pm.permission.IVivoPermission;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.permission.PermissionSettings;
import com.android.server.pm.permission.VivoOneTimePermissionUserManagerImpl;
import com.android.server.pm.permission.VivoPermissionImpl;
import com.android.server.policy.IVivoPhoneWindowManager;
import com.android.server.policy.IVivoRatioControllerUtils;
import com.android.server.policy.IVivoWindowOrientationListener;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.policy.VivoPhoneWindowManagerImpl;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.android.server.policy.VivoWindowOrientationListenerImpl;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.WindowOrientationListener;
import com.android.server.policy.keyguard.IVivoKeyguardServiceDelegate;
import com.android.server.policy.keyguard.KeyguardServiceDelegate;
import com.android.server.policy.keyguard.VivoKeyguardServiceDelegateImpl;
import com.android.server.power.IVivoNotifier;
import com.android.server.power.IVivoPowerManagerService;
import com.android.server.power.IVivoShutdownThread;
import com.android.server.power.IVivoWakeLock;
import com.android.server.power.PowerManagerService;
import com.android.server.power.ShutdownThread;
import com.android.server.power.VivoNotifierImpl;
import com.android.server.power.VivoPowerManagerServiceImpl;
import com.android.server.power.VivoShutdownThreadImpl;
import com.android.server.power.VivoWakeLockImpl;
import com.android.server.print.IVivoRemotePrintSpooler;
import com.android.server.print.VivoRemotePrintSpoolerImpl;
import com.android.server.statusbar.IVivoStatusBarManagerService;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.statusbar.VivoStatusBarManagerServiceImpl;
import com.android.server.timedetector.IVivoTimeDetectorStrategy;
import com.android.server.timedetector.TimeDetectorStrategyImpl;
import com.android.server.timedetector.VivoTimeDetectorStrategyImpl;
import com.android.server.trust.IVivoTrustManagerService;
import com.android.server.trust.TrustManagerService;
import com.android.server.trust.VivoTrustManagerServiceImpl;
import com.android.server.uri.IVivoUriGrantsManagerService;
import com.android.server.uri.VivoUriGrantsManagerServiceImpl;
import com.android.server.usb.IVivoUsbDeviceManager;
import com.android.server.usb.IVivoUsbHandler;
import com.android.server.usb.IVivoUsbHandlerLegacy;
import com.android.server.usb.IVivoUsbProfileGroupSettingsManager;
import com.android.server.usb.IVivoUsbUserPermissionManager;
import com.android.server.usb.UsbDeviceManager;
import com.android.server.usb.VivoUsbDeviceManagerImpl;
import com.android.server.usb.VivoUsbHandlerImpl;
import com.android.server.usb.VivoUsbHandlerLegacyImpl;
import com.android.server.usb.VivoUsbProfileGroupSettingsManagerImpl;
import com.android.server.usb.VivoUsbUserPermissionManagerImpl;
import com.android.server.wallpaper.IVivoWallpaperManagerService;
import com.android.server.wallpaper.VivoWallpaperManagerServiceImpl;
import com.android.server.wallpaper.WallpaperManagerService;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityStack;
import com.android.server.wm.ActivityStackSupervisor;
import com.android.server.wm.ActivityStarter;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.AppTransition;
import com.android.server.wm.AppTransitionController;
import com.android.server.wm.DisplayContent;
import com.android.server.wm.DisplayPolicy;
import com.android.server.wm.DisplayRotation;
import com.android.server.wm.DockedStackDividerController;
import com.android.server.wm.DragState;
import com.android.server.wm.IVivoActivityMetricsLogger;
import com.android.server.wm.IVivoActivityRecord;
import com.android.server.wm.IVivoActivityStack;
import com.android.server.wm.IVivoActivityStackSupervisor;
import com.android.server.wm.IVivoActivityStarter;
import com.android.server.wm.IVivoActivityStarterRequest;
import com.android.server.wm.IVivoActivityTaskManagerService;
import com.android.server.wm.IVivoAppTransition;
import com.android.server.wm.IVivoAppTransitionController;
import com.android.server.wm.IVivoDimmer;
import com.android.server.wm.IVivoDisplayContent;
import com.android.server.wm.IVivoDisplayPolicy;
import com.android.server.wm.IVivoDisplayRotation;
import com.android.server.wm.IVivoDockedStackDividerController;
import com.android.server.wm.IVivoDragState;
import com.android.server.wm.IVivoEnsureActivitiesVisibleHelper;
import com.android.server.wm.IVivoImmersiveModeConfirmation;
import com.android.server.wm.IVivoKeyguardController;
import com.android.server.wm.IVivoLetterbox;
import com.android.server.wm.IVivoRecentTasks;
import com.android.server.wm.IVivoRecentsAnimationController;
import com.android.server.wm.IVivoRemoteAnimationController;
import com.android.server.wm.IVivoResetTargetTaskHelper;
import com.android.server.wm.IVivoRootWindowContainer;
import com.android.server.wm.IVivoScreenRotationAnimation;
import com.android.server.wm.IVivoSession;
import com.android.server.wm.IVivoTask;
import com.android.server.wm.IVivoTaskDisplayArea;
import com.android.server.wm.IVivoTaskLaunchParamsModifier;
import com.android.server.wm.IVivoTaskTapPointerEventListener;
import com.android.server.wm.IVivoWindowAnimator;
import com.android.server.wm.IVivoWindowManagerShellCommand;
import com.android.server.wm.IVivoWindowProcessController;
import com.android.server.wm.IVivoWindowState;
import com.android.server.wm.IVivoWindowSurfaceController;
import com.android.server.wm.IVivoWms;
import com.android.server.wm.KeyguardController;
import com.android.server.wm.Letterbox;
import com.android.server.wm.RecentsAnimationController;
import com.android.server.wm.RootWindowContainer;
import com.android.server.wm.Task;
import com.android.server.wm.TaskDisplayArea;
import com.android.server.wm.VivoActivityMetricsLoggerImpl;
import com.android.server.wm.VivoActivityRecordImpl;
import com.android.server.wm.VivoActivityStackImpl;
import com.android.server.wm.VivoActivityStackSupervisorImpl;
import com.android.server.wm.VivoActivityStarterImpl;
import com.android.server.wm.VivoActivityStarterRequestImpl;
import com.android.server.wm.VivoActivityTaskManagerServiceImpl;
import com.android.server.wm.VivoAppTransitionControllerImpl;
import com.android.server.wm.VivoAppTransitionImpl;
import com.android.server.wm.VivoDimmerImpl;
import com.android.server.wm.VivoDisplayContentImpl;
import com.android.server.wm.VivoDisplayPolicyImpl;
import com.android.server.wm.VivoDisplayRotationImpl;
import com.android.server.wm.VivoDockedStackDividerControllerImpl;
import com.android.server.wm.VivoDragStateImpl;
import com.android.server.wm.VivoEnsureActivitiesVisibleHelperImpl;
import com.android.server.wm.VivoImmersiveModeConfirmationImpl;
import com.android.server.wm.VivoInputWindowCallbacksImpl;
import com.android.server.wm.VivoKeyguardControllerImpl;
import com.android.server.wm.VivoLetterboxImpl;
import com.android.server.wm.VivoRecentTasksImpl;
import com.android.server.wm.VivoRecentsAnimationControllerImpl;
import com.android.server.wm.VivoRemoteAnimationControllerImpl;
import com.android.server.wm.VivoResetTargetTaskHelperImpl;
import com.android.server.wm.VivoRootWindowContainerImpl;
import com.android.server.wm.VivoScreenRotationAnimationImpl;
import com.android.server.wm.VivoSessionImpl;
import com.android.server.wm.VivoStatsInServerImpl;
import com.android.server.wm.VivoTaskDisplayAreaImpl;
import com.android.server.wm.VivoTaskImpl;
import com.android.server.wm.VivoTaskLaunchParamsModifierImpl;
import com.android.server.wm.VivoTaskTapPointerEventListenerImpl;
import com.android.server.wm.VivoWindowAnimatorImpl;
import com.android.server.wm.VivoWindowManagerShellCommandImpl;
import com.android.server.wm.VivoWindowProcessControllerImpl;
import com.android.server.wm.VivoWindowStateImpl;
import com.android.server.wm.VivoWindowSurfaceControllerImpl;
import com.android.server.wm.VivoWmsImpl;
import com.android.server.wm.WindowAnimator;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowManagerShellCommand;
import com.android.server.wm.WindowProcessController;
import com.android.server.wm.WindowState;
import com.vivo.services.proxy.VivoProxyImpl;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.sp.SpManagerImpl;
import content.IVivoContentService;
import java.util.ArrayList;
import java.util.HashMap;

/* loaded from: classes.dex */
public class VivoSystemServiceFactoryImpl extends VivoSystemServiceFactory {
    public IVivoStats createVivoStats() {
        IVivoStats vivoStats = VivoStatsInServerImpl.getInstance();
        return vivoStats;
    }

    public IVivoSession createVivoSession() {
        IVivoSession vivoSession = new VivoSessionImpl();
        return vivoSession;
    }

    public IVivoSyncManager createVivoSyncManager(SyncManager syncManager, Context ctx) {
        IVivoSyncManager vivoSyncManager = new VivoSyncManagerImpl(syncManager, ctx);
        return vivoSyncManager;
    }

    public IVivoWindowAnimator createVivoWindowAnimator(WindowAnimator windowAnimator) {
        IVivoWindowAnimator vivoWindowAnimator = new VivoWindowAnimatorImpl(windowAnimator);
        return vivoWindowAnimator;
    }

    public IVivoActivityStackSupervisor createVivoActivityStackSupervisor(ActivityStackSupervisor supervisor, ActivityTaskManagerService service) {
        IVivoActivityStackSupervisor vivoActivityStackSupervisor = new VivoActivityStackSupervisorImpl(supervisor, service);
        return vivoActivityStackSupervisor;
    }

    public IVivoDisplayContent createVivoDisplayContent(DisplayContent displayContent, WindowManagerService wmService) {
        IVivoDisplayContent vivoDisplayContent = new VivoDisplayContentImpl(displayContent, wmService);
        return vivoDisplayContent;
    }

    public IVivoPms createVivoPms(PackageManagerService pms, Context context, PermissionManagerServiceInternal permissionManager) {
        IVivoPms vivoPms = new VivoPmsImpl(pms, context, permissionManager);
        return vivoPms;
    }

    public IVivoPackageInstallerSession createVivoPackageInstallerSession(PackageManagerService pms, PackageInstallerSession pis, Context context, Handler handler) {
        IVivoPackageInstallerSession vivoPackageInstallerSession = new VivoPackageInstallerSessionImpl(pms, pis, context, handler);
        return vivoPackageInstallerSession;
    }

    public IVivoDexManager createVivoDexManager(DexManager dm, IPackageManager pms, PackageDexUsage pdu, PackageDexOptimizer pdo, Context context) {
        IVivoDexManager vivoDexManager = new VivoDexManagerImpl(dm, pms, pdu, pdo, context);
        return vivoDexManager;
    }

    public IVivoAms createVivoAms(ActivityManagerService ams, Context ctx, ProcessList processList) {
        IVivoAms vivoAms = new VivoAmsImpl(ams, ctx, processList);
        return vivoAms;
    }

    public IVivoActiveService createVivoActiverService(ActiveServices activeService) {
        IVivoActiveService vivoActiveService = new VivoActiveServiceImpl(activeService);
        return vivoActiveService;
    }

    public IVivoProcessList createVivoProcessList(ProcessList processList) {
        IVivoProcessList vivoProcessList = new VivoProcessListImpl(processList);
        return vivoProcessList;
    }

    public IVivoWms createVivoWms(WindowManagerService wms) {
        IVivoWms vivoWms = new VivoWmsImpl(wms);
        return vivoWms;
    }

    public IVivoStatusBarManagerService createVivoStatusBarManagerService(StatusBarManagerService statusbarMgrService) {
        IVivoStatusBarManagerService vivoStatusBarManagerService = new VivoStatusBarManagerServiceImpl(statusbarMgrService);
        return vivoStatusBarManagerService;
    }

    public IVivoKeyguardController createVivoKeyguardController(KeyguardController controller) {
        IVivoKeyguardController vivoKeyguardController = new VivoKeyguardControllerImpl(controller);
        return vivoKeyguardController;
    }

    public IVivoActivityTaskManagerService createVivoActivityTaskManagerService(ActivityTaskManagerService atm) {
        IVivoActivityTaskManagerService vivoAtm = new VivoActivityTaskManagerServiceImpl(atm);
        return vivoAtm;
    }

    public IVivoAppTransition createVivoAppTransition(AppTransition appTransition) {
        IVivoAppTransition vivoAppTransition = new VivoAppTransitionImpl(appTransition);
        return vivoAppTransition;
    }

    public IVivoAppTransitionController createVivoAppTransitionController(WindowManagerService wms, AppTransitionController appTransitionController) {
        IVivoAppTransitionController vivoAppTransitionController = new VivoAppTransitionControllerImpl(wms, appTransitionController);
        return vivoAppTransitionController;
    }

    public IVivoAlarmMgrService createVivoAlarmMgrService(AlarmManagerService alarmMgrService) {
        return new VivoAlarmMgrServiceImpl(alarmMgrService);
    }

    public IVivoOpenFdMonitor createVivoOpenFdMonitor() {
        return new VivoOpenFdMonitorImpl();
    }

    public IVivoActivityStarter createVivoActivityStarter(ActivityStarter activityStarter, ActivityTaskManagerService service, ActivityStackSupervisor supervisor, Context ctx) {
        IVivoActivityStarter vivoActivityStarter = new VivoActivityStarterImpl(activityStarter, service, supervisor, ctx);
        return vivoActivityStarter;
    }

    public IVivoActivityStarterRequest createVivoActivityStarterRequest() {
        IVivoActivityStarterRequest vivoActivityStarterRequest = new VivoActivityStarterRequestImpl();
        return vivoActivityStarterRequest;
    }

    public IVivoNotificationManagerService createVivoNotificationManagerService(NotificationManagerService notificationManagerService, Handler handler, PreferencesHelper preferencesHelper, PackageManager ackageManagerClient, IPackageManager packageManager, NotificationManagerService.NotificationListeners listeners, AlarmManager alarmManager, RankingHelper rankingHelper) {
        return new VivoNotificationManagerServiceImpl(notificationManagerService, handler, preferencesHelper, ackageManagerClient, packageManager, listeners, alarmManager, rankingHelper);
    }

    public IVivoZenModeHelper createVivoZenModeHelper(ZenModeHelper zenModeHelper, Context context) {
        return new VivoZenModeHelperImpl(zenModeHelper, context);
    }

    public IVivoPreferencesHelper createVivoPreferencesHelper(Context context, PreferencesHelper preferencesHelper) {
        return new VivoPreferencesHelperImpl(context, preferencesHelper);
    }

    public IVivoGroupHelper createVivoGroupHelper() {
        return new VivoGroupHelperImpl();
    }

    public IVivoValidateNotificationPeople createVivoValidateNotificationPeople() {
        return new VivoValidateNotificationPeopleImpl();
    }

    public IVivoRankingHelper createVivoRankingHelper() {
        return new VivoRankingHelperImpl();
    }

    public IVivoSystemServer createVivoSystemServer(SystemServiceManager systemServiceManager, Context context) {
        IVivoSystemServer vivoSystemServer = new VivoSystemServerImpl(systemServiceManager, context);
        return vivoSystemServer;
    }

    public IVivoInputMethodManagerService createVivoInputMethodManagerService(InputMethodManagerService imms, Context context) {
        IVivoInputMethodManagerService vImms = new VivoInputMethodManagerServiceImpl(imms, context);
        return vImms;
    }

    public IVivoInputMethodManagerServiceLifecycle createVivoInputMethodManagerServiceLifecycle(Context context, InputMethodManagerService.Lifecycle immsLifecycle) {
        IVivoInputMethodManagerServiceLifecycle mImmsLifecycle = new VivoInputMethodManagerServiceLifecycleImpl(context, immsLifecycle);
        return mImmsLifecycle;
    }

    public IVivoBinderService getVivoBinderService() {
        return VivoBinderServiceImpl.getInstance();
    }

    public IVivoPhoneWindowManager createVivoPhoneWindowManager(PhoneWindowManager phoneWindowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs, Context context, Object lock, Handler handler) {
        IVivoPhoneWindowManager vivoPhoneWindowManager = new VivoPhoneWindowManagerImpl(phoneWindowManager, windowManagerFuncs, context, lock, handler);
        return vivoPhoneWindowManager;
    }

    public IVivoDisplayPolicy createVivoDisplayPolicy(DisplayPolicy policy, WindowManagerService service, WindowManagerPolicy manager, Context context, Handler handler, IWindowManager wm, boolean isDefaultDisplay) {
        IVivoDisplayPolicy vivoDisplayPolicy = new VivoDisplayPolicyImpl(policy, service, manager, context, handler, wm, isDefaultDisplay);
        return vivoDisplayPolicy;
    }

    public IVivoActivityRecord createVivoActivityRecord(ActivityRecord activityRecord, ActivityTaskManagerService atmService, ComponentName activityComponent) {
        IVivoActivityRecord vivoActivityRecord = new VivoActivityRecordImpl(activityRecord, atmService, activityComponent);
        return vivoActivityRecord;
    }

    public IVivoInputManagerService createVivoInputManagerService(Context context, Handler handler, InputManagerService inputManagerService, long ptr) {
        IVivoInputManagerService vivoInputManagerService = new VivoInputManagerServiceImpl(context, handler, inputManagerService, ptr);
        return vivoInputManagerService;
    }

    public IVivoWindowManagerCallbacks createVivoWindowManagerCallbacks(WindowManagerService service) {
        IVivoWindowManagerCallbacks vivoWindowManagerCallbacks = new VivoInputWindowCallbacksImpl(service);
        return vivoWindowManagerCallbacks;
    }

    public IVivoPowerManagerService createVivoPowerManagerService(Context context, PowerManagerService powerManagerService, DisplayManagerInternal.DisplayPowerRequest displaypowerrequest) {
        IVivoPowerManagerService vivoPowerManagerService = new VivoPowerManagerServiceImpl(context, powerManagerService, displaypowerrequest);
        return vivoPowerManagerService;
    }

    public IVivoWakeLock createVivoWakeLock(int displayId) {
        IVivoWakeLock vivoWakeLock = new VivoWakeLockImpl(displayId);
        return vivoWakeLock;
    }

    public IVivoConnectivityService createVivoConnectivityService(Context context, ConnectivityService connectivityService, INetworkManagementService networkManagementService, Handler handler) {
        IVivoConnectivityService vivoConnectivityService = new VivoConnectivityServiceImplPriv(context, connectivityService, networkManagementService, handler);
        return vivoConnectivityService;
    }

    public IVivoLightImpl createVivoLightImpl(Context context, LightsService.LightImpl lightImpl) {
        IVivoLightImpl vivoLightImpl = new VivoLightImplImpl(context, lightImpl);
        return vivoLightImpl;
    }

    public IVivoLed createVivoLed(BatteryService.Led led, Context context, boolean debug) {
        IVivoLed vivoLed = new VivoLedImpl(led, context, debug);
        return vivoLed;
    }

    public IVivoColorFade createVivoColorFade(ColorFade colorFade) {
        IVivoColorFade vivoColorFade = new VivoColorFadeImpl(colorFade);
        return vivoColorFade;
    }

    public IVivoDisplayPowerController createVivoDisplayPowerController(SensorManager sensorManager, DisplayPowerController displaypowercontroller, Context context) {
        IVivoDisplayPowerController vivoDisplayPowerController = new VivoDisplayPowerControllerImpl(sensorManager, displaypowercontroller, context);
        return vivoDisplayPowerController;
    }

    public IVivoDisplayPowerState createVivoDisplayPowerState(DisplayPowerState displayPowerState, Context context, Handler handler) {
        IVivoDisplayPowerState vivoDisplayPowerState = new VivoDisplayPowerStateImpl(displayPowerState, context, handler);
        return vivoDisplayPowerState;
    }

    public IVivoLocalDisplayDevice createVivoLocalDisplayDevice(LocalDisplayAdapter.LocalDisplayDevice localDisplayDevice) {
        IVivoLocalDisplayDevice vivoLocalDisplayDevice = new VivoLocalDisplayDeviceImpl(localDisplayDevice);
        return vivoLocalDisplayDevice;
    }

    public IVivoLogicalDisplay createVivoLogicalDisplay(LogicalDisplay display) {
        IVivoLogicalDisplay vivoLogicalDisplay = new VivoLogicalDisplayImpl(display);
        return vivoLogicalDisplay;
    }

    public IVivoDragState createVivoDragState() {
        IVivoDragState vivoDragState = new VivoDragStateImpl();
        return vivoDragState;
    }

    public IVivoDragState createVivoDragState(DragState dragState) {
        IVivoDragState vivoDragState = new VivoDragStateImpl(dragState);
        return vivoDragState;
    }

    public IVivoDockedStackDividerController createVivoDockedStackDividerController(DockedStackDividerController dockedStackDividerController, WindowManagerService service, DisplayContent displayContent) {
        IVivoDockedStackDividerController vivoDockedStackDividerController = new VivoDockedStackDividerControllerImpl(dockedStackDividerController, service, displayContent);
        return vivoDockedStackDividerController;
    }

    public IVivoWifiDisplayAdapter createVivoWifiDisplayAdapter(Context context, Object adapter) {
        IVivoWifiDisplayAdapter adapterValue = new VivoWifiDisplayAdapterImpl(context, adapter);
        return adapterValue;
    }

    public IVivoBatteryService createVivoBatteryService(BatteryService batteryService) {
        IVivoBatteryService vivoBatteryService = new VivoBatteryServiceImpl(batteryService);
        return vivoBatteryService;
    }

    public IVivoShutdownThread createVivoShutdownThread(ShutdownThread shutdownThread) {
        IVivoShutdownThread vivoShutdownThread = new VivoShutdownThreadImpl(shutdownThread);
        return vivoShutdownThread;
    }

    public IVivoDbms createVivoDbms(DropBoxManagerService.DropBoxManagerBroadcastHandler mHandler) {
        IVivoDbms vivoDbms = new VivoDbmsImpl(mHandler);
        return vivoDbms;
    }

    public IVivoDisplayManagerService createVivoDisplayManagerService(DisplayManagerService displayMgrService) {
        IVivoDisplayManagerService vivoDisplayManagerService = new VivoDisplayManagerServiceImpl(displayMgrService);
        return vivoDisplayManagerService;
    }

    public IVivoLocalDisplayAdapter createVivoLocalDisplayAdapter(Context context, Handler handler, LocalDisplayAdapter localDisplayAdapter) {
        IVivoLocalDisplayAdapter vivoLocalDisplayAdapter = new VivoLocalDisplayAdapterImpl(context, handler, localDisplayAdapter);
        return vivoLocalDisplayAdapter;
    }

    public IVivoNotifier createVivoNotifier(Context context) {
        IVivoNotifier vivoNotifier = new VivoNotifierImpl(context);
        return vivoNotifier;
    }

    public IVivoLockSettingsService createVivoLockSettingsService(LockSettingsService lss, Context context, LockSettingsStorage storage, UserManager um) {
        IVivoLockSettingsService vivoLockSettingsService = new VivoLockSettingsServiceImpl(lss, context, storage, um);
        return vivoLockSettingsService;
    }

    public IVivoLockSettingsStorage createVivoLockSettingsStorage(LockSettingsStorage locksettingsstorage, LockSettingsStorage.DatabaseHelper dbHelper, Context context) {
        IVivoLockSettingsStorage vivoLockSettingsStorage = new VivoLockSettingsStorageImpl(locksettingsstorage, dbHelper, context);
        return vivoLockSettingsStorage;
    }

    public IVivoLockSettingsStrongAuth createVivoLockSettingsStrongAuth(LockSettingsStrongAuth strongauth, Context context, AlarmManager alarm) {
        IVivoLockSettingsStrongAuth vivoLockSettingsStrongAuth = new VivoLockSettingsStrongAuthImpl(strongauth, context, alarm);
        return vivoLockSettingsStrongAuth;
    }

    public IVivoSyntheticPasswordManager createVivoSyntheticPasswordManager(LockSettingsStorage storage, UserManager um, SyntheticPasswordManager spManager) {
        IVivoSyntheticPasswordManager vivoSPManager = new VivoSyntheticPasswordManagerImpl(storage, um, spManager);
        return vivoSPManager;
    }

    public IVivoLockSettingsShellCommand createVivoLockSettingsShellCommand() {
        IVivoLockSettingsShellCommand vivoLockSettingsShellcmd = new VivoLockSettingsShellCommandImpl();
        return vivoLockSettingsShellcmd;
    }

    public IVivoVibratorService createVivoVibratorService(Handler handler, Context context) {
        IVivoVibratorService vivoVibratorService = new VivoVibratorServiceImpl(handler, context);
        return vivoVibratorService;
    }

    public IVivoWindowState createVivoWindowState(WindowState windowState) {
        IVivoWindowState vivoWindowState = new VivoWindowStateImpl(windowState);
        return vivoWindowState;
    }

    public IVivoNetworkTimeUpdateService createVivoNetworkTimeUpdateService(NetworkTimeUpdateService networktimeUpService, NtpTrustedTime ntpTime, AlarmManager alarmMgr, Context context) {
        IVivoNetworkTimeUpdateService vivoNetworkTimeUpdateService = new VivoNetworkTimeUpdateServiceImpl(networktimeUpService, ntpTime, alarmMgr, context);
        return vivoNetworkTimeUpdateService;
    }

    public IVivoTimeDetectorStrategy createVivoTimeDetectorStrategy(TimeDetectorStrategyImpl timeDetectorStrategy) {
        IVivoTimeDetectorStrategy vivoTimeDetectorStrategy = new VivoTimeDetectorStrategyImpl(timeDetectorStrategy);
        return vivoTimeDetectorStrategy;
    }

    public IVivoAudioService createVivoAudioService(AudioService as, Context context) {
        IVivoAudioService vivoAudioService = new VivoAudioServiceImpl(as, context);
        return vivoAudioService;
    }

    public IVivoMediaSessionService createVivoMediaSessionService(MediaSessionService as, Context context) {
        IVivoMediaSessionService vivoMediaSessionService = new VivoMediaSessionServiceImpl(as, context);
        return vivoMediaSessionService;
    }

    public IVivoColorDisplayService createVivoColorDisplayService(ColorDisplayService colorDisplayService, Context context) {
        IVivoColorDisplayService vivoColorDisplayService = new VivoColorDisplayServiceImpl(colorDisplayService, context);
        return vivoColorDisplayService;
    }

    public IVivoFaceService createVivoFaceService(Context context, FaceService service, Handler handler) {
        IVivoFaceService vivoFaceService = new VivoFaceServiceImpl(context, service, handler);
        return vivoFaceService;
    }

    public IVivoFingerprintService createVivoFingerprintService(Context context, FingerprintService service, Handler handler) {
        IVivoFingerprintService iservice = new VivoFingerprintServiceImpl(context, service, handler);
        return iservice;
    }

    public IVivoWindowSurfaceController createVivoWindowSurfaceController() {
        IVivoWindowSurfaceController vivoWindowSurfaceController = new VivoWindowSurfaceControllerImpl();
        return vivoWindowSurfaceController;
    }

    public IVivoKeyguardServiceDelegate createVivoKeyguardServiceDelegate(Context context, KeyguardServiceDelegate keyguardServiceDelegate) {
        IVivoKeyguardServiceDelegate idelegate = new VivoKeyguardServiceDelegateImpl(context, keyguardServiceDelegate);
        return idelegate;
    }

    public IVivoClipboardService createVivoClipboardService(ClipboardService service) {
        IVivoClipboardService vivoclipboardservice = new VivoClipboardServiceImpl(service);
        return vivoclipboardservice;
    }

    public IVivoPermission createVivoPermission(PermissionManagerService permManagerService, Context context, PermissionSettings settings) {
        IVivoPermission vivoPermission = new VivoPermissionImpl(permManagerService, context, settings);
        return vivoPermission;
    }

    public IVivoPcbaCotrol createPcbaCotrol() {
        IVivoPcbaCotrol vivoPcbaCotrol = new VivoPcbaCotrolImpl();
        return vivoPcbaCotrol;
    }

    public IVivoActivityManagerShellCommand createVivoAmsCommand() {
        IVivoActivityManagerShellCommand vivoAmsCommand = new VivoActivityManagerShellCommandImpl();
        return vivoAmsCommand;
    }

    public IVivoTrustManagerService createVivoTrustManagerService(TrustManagerService trustManagerSerivce, Context context, Handler handler) {
        IVivoTrustManagerService vivoTrustManagerService = new VivoTrustManagerServiceImpl(trustManagerSerivce, context, handler);
        return vivoTrustManagerService;
    }

    public IVivoRmsInjector createVivoRmsInjector() {
        return RmsInjectorImpl.getInstance();
    }

    public IVivoProxy createVivoProxy() {
        return VivoProxyImpl.getInstance();
    }

    public IVivoUserBackupManagerService createVivoUserBackupManagerService(Object userBackupManagerService) {
        IVivoUserBackupManagerService vivoUserBackupManagerService = new VivoUserBackupManagerServiceImpl((UserBackupManagerService) userBackupManagerService);
        return vivoUserBackupManagerService;
    }

    public IVivoTrampoline createVivoTrampoline() {
        IVivoTrampoline vivoTrampoline = new VivoTrampolineImpl();
        return vivoTrampoline;
    }

    public IVivoPerformAdbBackupTask createVivoPerformAdbBackupTask(int token, int fd, Object observer) {
        IVivoPerformAdbBackupTask vivoPerformAdbBackupTask = new VivoPerformAdbBackupTaskImpl(token, fd, (IFullBackupRestoreObserver) observer);
        return vivoPerformAdbBackupTask;
    }

    public IVivoFullBackupEngine createVivoFullBackupEngine() {
        IVivoFullBackupEngine vivoFullBackupEngine = new VivoFullBackupEngineImpl();
        return vivoFullBackupEngine;
    }

    public IVivoFullBackupTask createVivoFullBackupTask() {
        IVivoFullBackupTask vivoFullBackupTask = new VivoFullBackupTaskImpl();
        return vivoFullBackupTask;
    }

    public IVivoAppMetadataBackupWriter createVivoAppMetadataBackupWriter() {
        IVivoAppMetadataBackupWriter vivoAppMetadataBackupWriter = new VivoAppMetadataBackupWriterImpl();
        return vivoAppMetadataBackupWriter;
    }

    public IVivoPerformAdbRestoreTask createVivoPerformAdbRestoreTask(int token, int fd) {
        IVivoPerformAdbRestoreTask vivoPerformAdbRestoreTask = new VivoPerformAdbRestoreTaskImpl(token, fd);
        return vivoPerformAdbRestoreTask;
    }

    public IVivoFullRestoreEngine createVivoFullRestoreEngine() {
        IVivoFullRestoreEngine vivoFullRestoreEngine = new VivoFullRestoreEngineImpl();
        return vivoFullRestoreEngine;
    }

    public IVivoFullRestoreEngineThread createVivoFullRestoreEngineThread() {
        IVivoFullRestoreEngineThread vivoFullRestoreEngineThread = new VivoFullRestoreEngineThreadImpl();
        return vivoFullRestoreEngineThread;
    }

    public IVivoTarBackupReader createVivoTarBackupReader() {
        IVivoTarBackupReader vivoTarBackupReader = new VivoTarBackupReaderImpl();
        return vivoTarBackupReader;
    }

    public IVivoRestoreUtils createVivoRestoreUtils() {
        IVivoRestoreUtils vivoRestoreUtils = new VivoRestoreUtilsImpl();
        return vivoRestoreUtils;
    }

    public IVivoFullBackupRestoreObserverUtils createVivoFullBackupRestoreObserverUtils() {
        IVivoFullBackupRestoreObserverUtils vivoFullBackupRestoreObserverUtils = new VivoFullBackupRestoreObserverUtilsImpl();
        return vivoFullBackupRestoreObserverUtils;
    }

    public IVivoFullBackupUtils createVivoFullBackupUtils() {
        IVivoFullBackupUtils vivoFullBackupUtils = new VivoFullBackupUtilsImpl();
        return vivoFullBackupUtils;
    }

    public IVivoGnssLocationProvider createVivoGnssLocationProvider() {
        IVivoGnssLocationProvider vivoGnssLocationProvider = new VivoGnssLocationProviderExt();
        return vivoGnssLocationProvider;
    }

    public IVivoLocationManagerService createVivoLocationManagerService(Context context, boolean D, LocationManagerService locationManagerService, Object lock, HashMap<Object, LocationManagerService.Receiver> receivers) {
        IVivoLocationManagerService vivoLocationManagerService = new VivoLocationManagerServiceExt(context, D, locationManagerService, lock, receivers);
        return vivoLocationManagerService;
    }

    public IVivoServiceWatcher createVivoServiceWatcher() {
        IVivoServiceWatcher vivoServiceWatcher = new VivoServiceWatcherImpl();
        return vivoServiceWatcher;
    }

    public IVivoUsbDeviceManager createVivoUsbDeviceManager(Object usbDeviceManager, Context context) {
        IVivoUsbDeviceManager udm = new VivoUsbDeviceManagerImpl((UsbDeviceManager) usbDeviceManager, context);
        return udm;
    }

    public IVivoUsbHandler createVivoUsbHandler(Object usbHandler, Context context) {
        IVivoUsbHandler uh = new VivoUsbHandlerImpl((UsbDeviceManager.UsbHandler) usbHandler, context);
        return uh;
    }

    public IVivoUsbHandlerLegacy createVivoUsbHandlerLegacy(Object usbHandlerLegacy, Context context) {
        IVivoUsbHandlerLegacy uhl = new VivoUsbHandlerLegacyImpl((UsbDeviceManager.UsbHandlerLegacy) usbHandlerLegacy, context);
        return uhl;
    }

    public IVivoUiModeMgrService createVivoUiModeMgrService(UiModeManagerService uiModeManagerService) {
        IVivoUiModeMgrService vivoUiModeMgrService = new VivoUiModeMgrServiceImpl(uiModeManagerService);
        return vivoUiModeMgrService;
    }

    public IVivoRootWindowContainer createVivoRootWindowContainer(RootWindowContainer mRoot, WindowManagerService wms) {
        IVivoRootWindowContainer vivoRootWindowContainer = new VivoRootWindowContainerImpl(mRoot, wms);
        return vivoRootWindowContainer;
    }

    public IVivoDisplayRotation createVivoDisplayRotation(DisplayRotation displayRotation, WindowManagerService service) {
        IVivoDisplayRotation vivoDisplayRotation = new VivoDisplayRotationImpl(displayRotation, service);
        return vivoDisplayRotation;
    }

    public IVivoTaskDisplayArea createVivoTaskDisplayArea(TaskDisplayArea taskDisplayArea, WindowManagerService wms) {
        IVivoTaskDisplayArea vivoTaskDisplayArea = new VivoTaskDisplayAreaImpl(taskDisplayArea, wms);
        return vivoTaskDisplayArea;
    }

    public IVivoTask createVivoTask(Task task, ActivityTaskManagerService atmService) {
        IVivoTask vivoTask = new VivoTaskImpl(task, atmService);
        return vivoTask;
    }

    public IVivoBroadcastQueue createVivoBroadcastQueue(BroadcastQueue broadcastQueue, ActivityManagerService service) {
        IVivoBroadcastQueue vivoBroadcastQueue = new VivoBroadcastQueueImpl(broadcastQueue, service);
        return vivoBroadcastQueue;
    }

    public IVivoRecentTasks createVivoRecentTasks() {
        IVivoRecentTasks vivoRecentTasks = new VivoRecentTasksImpl();
        return vivoRecentTasks;
    }

    public IVivoDoubleInstanceService createVivoDoubleInstanceService() {
        IVivoDoubleInstanceService vivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
        return vivoDoubleInstanceService;
    }

    public IVivoUms createVivoUms(UserManagerService ums) {
        IVivoUms vivoUms = new VivoUmsImpl(ums);
        return vivoUms;
    }

    public IVivoSettings createSettingsImpl() {
        IVivoSettings vivoSettings = new VivoSettingsImpl();
        return vivoSettings;
    }

    public IVivoResetTargetTaskHelper createResetTargetTaskHelperImpl() {
        IVivoResetTargetTaskHelper vivoResetTargetTaskHelper = new VivoResetTargetTaskHelperImpl();
        return vivoResetTargetTaskHelper;
    }

    public IVivoNetworkStatsAccess createVivoNetworkStatsAccessImpl() {
        IVivoNetworkStatsAccess vivoNetworkStatsAccess = new VivoNetworkStatsAccessImpl();
        return vivoNetworkStatsAccess;
    }

    public IVivoRemotePrintSpooler createVivoRemotePrintSpoolerImpl() {
        IVivoRemotePrintSpooler vivoRps = new VivoRemotePrintSpoolerImpl();
        return vivoRps;
    }

    public IVivoUriGrantsManagerService createVivoUgmsImpl() {
        IVivoUriGrantsManagerService vivoUgms = new VivoUriGrantsManagerServiceImpl();
        return vivoUgms;
    }

    public IVivoBluetoothManagerService createVivoBluetoothManagerService(BluetoothManagerService bluetoothManagerService) {
        IVivoBluetoothManagerService vivoBluetoothManagerService = new VivoBluetoothManagerServiceImpl(bluetoothManagerService);
        return vivoBluetoothManagerService;
    }

    public IVivoStorageMgrService createVivoStorageManagerService(StorageManagerService service, Context context, Handler handler) {
        return new VivoStorageMgrServiceImpl(service, context, handler);
    }

    public IVivoNetworkPolicyManagerService createVivoNetworkPolicyManagerService(NetworkPolicyManagerService npms, Context context) {
        IVivoNetworkPolicyManagerService vivoNetworkPolicyManagerService = new VivoNetworkPolicyManagerServiceImpl(npms, context);
        return vivoNetworkPolicyManagerService;
    }

    public IVivoImmersiveModeConfirmation createVivoImmersiveModeConfirmation() {
        IVivoImmersiveModeConfirmation vivoImmersiveModeConfirmation = new VivoImmersiveModeConfirmationImpl();
        return vivoImmersiveModeConfirmation;
    }

    public IVivoWallpaperManagerService createVivoWallpaperManagerService(WallpaperManagerService wallpaperMgrService, Context context) {
        IVivoWallpaperManagerService vivoWallpaperManagerService = new VivoWallpaperManagerServiceImpl(wallpaperMgrService, context);
        return vivoWallpaperManagerService;
    }

    public IVivoLetterbox createVivoLetterbox(Letterbox letterbox) {
        IVivoLetterbox vivoLetterbox = new VivoLetterboxImpl(letterbox);
        return vivoLetterbox;
    }

    public IVivoLetterbox.IVivoLetterboxSurface createVivoLetterboxSurface(Letterbox.LetterboxSurface letterboxSurface) {
        IVivoLetterbox.IVivoLetterboxSurface vivoLetterboxSurface = new VivoLetterboxImpl.VivoLetterboxSurfaceImpl(letterboxSurface);
        return vivoLetterboxSurface;
    }

    public IVivoRecentsAnimationController createVivoRecentsAnimationController(RecentsAnimationController controller, WindowManagerService wms, int displayId) {
        IVivoRecentsAnimationController vivoRecentsAnimationController = new VivoRecentsAnimationControllerImpl(controller, wms, displayId);
        return vivoRecentsAnimationController;
    }

    public IVivoRemoteAnimationController createVivoRemoteAnimationController(RemoteAnimationAdapter adapter, WindowManagerService wms) {
        IVivoRemoteAnimationController vivoRemoteAnimationController = new VivoRemoteAnimationControllerImpl(adapter, wms);
        return vivoRemoteAnimationController;
    }

    public IVivoRampAnimator createVivoRampAnimator(RampAnimator rampanimator, Context context) {
        IVivoRampAnimator vivoRampAnimator = new VivoRampAnimatorImpl(rampanimator, context);
        return vivoRampAnimator;
    }

    public IVivoWindowOrientationListener createVivoWindowOrientationListener(WindowOrientationListener winorientation, SensorManager sensormanager) {
        IVivoWindowOrientationListener vivoWinOrientation = new VivoWindowOrientationListenerImpl(winorientation, sensormanager);
        return vivoWinOrientation;
    }

    public IVivoNetworkStatsService createVivoNetworkStatsService(NetworkStatsService networkStatsService) {
        IVivoNetworkStatsService vivoNetworkStatsService = new VivoNetworkStatsServiceImpl(networkStatsService);
        return vivoNetworkStatsService;
    }

    public IVivoWatchdog createVivoWatchdog(ActivityManagerService service) {
        IVivoWatchdog vivoWatchdog = new VivoWatchdogImpl(service);
        return vivoWatchdog;
    }

    public IVivoPendingIntentController createVivoPendingIntentController() {
        IVivoPendingIntentController vivoPendingIntentController = new VivoPendingIntentControllerImpl();
        return vivoPendingIntentController;
    }

    public IVivoActivityStack createVivoActivityStack(ActivityTaskManagerService atm, ActivityStack as) {
        IVivoActivityStack vivoActivityStack = new VivoActivityStackImpl(atm, as);
        return vivoActivityStack;
    }

    public IVivoProcessRecord createVivoProcessRecord(ProcessRecord _processRecord, ApplicationInfo _info, String _processName, int _uid) {
        IVivoProcessRecord vivoProcessRecord = new VivoProcessRecordImpl(_processRecord, _info, _processName, _uid);
        return vivoProcessRecord;
    }

    public IVivoCustomDpms createVivoCustomDpmsImpl(Context context, Object service) {
        IVivoCustomDpms vivoCustomDpms = new VivoCustomDpmsImpl(context, (DevicePolicyManagerService) service);
        return vivoCustomDpms;
    }

    public IVivoUsbUserPermissionManager createVivoUsbUserPermissionManager(Object manager) {
        IVivoUsbUserPermissionManager usm = new VivoUsbUserPermissionManagerImpl(manager);
        return usm;
    }

    public IVivoUsbProfileGroupSettingsManager createVivoUsbProfileGroupSettingsManager() {
        IVivoUsbProfileGroupSettingsManager upsm = new VivoUsbProfileGroupSettingsManagerImpl();
        return upsm;
    }

    public IVivoTaskLaunchParamsModifier createVivoTaskLaunchParamsModifier() {
        IVivoTaskLaunchParamsModifier taskLaunchParams = new VivoTaskLaunchParamsModifierImpl();
        return taskLaunchParams;
    }

    public IVivoEnsureActivitiesVisibleHelper createVivoEnsureActivitiesVisibleHelper() {
        IVivoEnsureActivitiesVisibleHelper eavh = new VivoEnsureActivitiesVisibleHelperImpl();
        return eavh;
    }

    public IVivoNetworkManagementService createVivoNetworkManagementService(NetworkManagementService networkManagementService, Context context) {
        IVivoNetworkManagementService vivoNetworkManagementService = new VivoNetworkManagementServiceImpl(networkManagementService, context);
        return vivoNetworkManagementService;
    }

    public IVivoTaskTapPointerEventListener createVivoTaskTapPointerEventListener() {
        IVivoTaskTapPointerEventListener vivoTaskTapPointerEventListener = new VivoTaskTapPointerEventListenerImpl();
        return vivoTaskTapPointerEventListener;
    }

    public IVivoActivityMetricsLogger createVivoActivityMetricsLogger() {
        IVivoActivityMetricsLogger vivoActivityMetricsLoggerImpl = new VivoActivityMetricsLoggerImpl();
        return vivoActivityMetricsLoggerImpl;
    }

    public IVivoScreenRotationAnimation createVivoScreenRotationAnimation() {
        IVivoScreenRotationAnimation vivoScreenRotationAnimationImpl = new VivoScreenRotationAnimationImpl();
        return vivoScreenRotationAnimationImpl;
    }

    public IVivoCachedAppOptimizer createVivoCachedAppOptimizer() {
        IVivoCachedAppOptimizer vivoCachedAppOptimizer = new VivoCachedAppOptimizer();
        return vivoCachedAppOptimizer;
    }

    public IVivoOomAdjuster createVivoOomAdjusterImpl() {
        IVivoOomAdjuster mVivoOomAdjusterImpl = new VivoOomAdjusterImpl();
        return mVivoOomAdjusterImpl;
    }

    public IVivoServiceRecord createVivoServiceRecordImpl() {
        IVivoServiceRecord vivoServiceRecordImpl = new VivoServiceRecordImpl();
        return vivoServiceRecordImpl;
    }

    public IVivoVpn createVivoVpn(Vpn vpn) {
        IVivoVpn vivoVpn = new VivoVpnImpl(vpn);
        return vivoVpn;
    }

    public IVivoContentService createVivoContentService(ContentService contentService, Context ctx) {
        IVivoContentService vivoContentService = new VivoContentServiceImpl(contentService, ctx);
        return vivoContentService;
    }

    public IVivoBatteryStatsService createVivoBatteryStatsServiceImpl(BatteryStatsService batteryStats) {
        IVivoBatteryStatsService vivoBatteryStatsService = new VivoBatteryStatsServiceImpl(batteryStats);
        return vivoBatteryStatsService;
    }

    public IVivoVirtualDisplayAdapter createVivoVirtualDisplayAdapter(VirtualDisplayAdapter virtualDisplayAdapter) {
        IVivoVirtualDisplayAdapter vivoVirtualDisplayAdapter = new VivoVirtualDisplayAdapterImpl(virtualDisplayAdapter);
        return vivoVirtualDisplayAdapter;
    }

    public IVivoWindowProcessController createVivoWindowProcessController(WindowProcessController windowProcessController, ActivityTaskManagerService atm, ArrayList<ActivityRecord> activities) {
        IVivoWindowProcessController vivoWindowProcessController = new VivoWindowProcessControllerImpl(windowProcessController, atm, activities);
        return vivoWindowProcessController;
    }

    public IVivoJobSchedulerService createJobSchedulerService(Context context) {
        IVivoJobSchedulerService vivoJobSchedulerService = new VivoJobSchedulerServiceImpl(context);
        return vivoJobSchedulerService;
    }

    public IVivoFrozenInjector createVivoFrozenInjector() {
        return FrozenInjectorImpl.getInstance();
    }

    public IVivoProcFrozenManager createVivoProcFrozenManager(ActivityManagerService ams) {
        return ProcessFreezeManager.getInstance(ams);
    }

    public IVivoDimmer createVivoDimmerImpl() {
        IVivoDimmer vivoDimmerImpl = new VivoDimmerImpl();
        return vivoDimmerImpl;
    }

    public IVivoOneTimePermissionUserManager createVivoOneTimePermissionUserManager() {
        return new VivoOneTimePermissionUserManagerImpl();
    }

    public IVivoSpManager createVivoSpManager() {
        return SpManagerImpl.getInstance();
    }

    public IVivoComponentResolver createVivoComponentResolver() {
        IVivoComponentResolver vivoComponentResolver = VivoComponentResolverImpl.getInstance();
        return vivoComponentResolver;
    }

    public IVivoUserController createVivoUserController() {
        IVivoUserController vivoUserController = new VivoUserControllerImpl();
        return vivoUserController;
    }

    public IVivoRatioControllerUtils createVivoRatioControllerUtils() {
        return VivoRatioControllerUtilsImpl.getInstance();
    }

    public IVivoUserDataPreparer createVivoUserDataPreparer() {
        return VivoUserDataPreparerImpl.getInstance();
    }

    public IVivoAutofillService createVivoAutofillService(Context context) {
        IVivoAutofillService vivoautofillservice = new VivoAutofillServiceImpl(context);
        return vivoautofillservice;
    }

    public IVivoAppExitInfoTracker createVivoAppExitInfoTracker() {
        IVivoAppExitInfoTracker vivoAppExitInfoTracker = new VivoAppExitInfoTrackerImpl();
        return vivoAppExitInfoTracker;
    }

    public IVivoWindowManagerShellCommand createVivoWindowManagerShellCommand(WindowManagerShellCommand shell, WindowManagerService service) {
        return new VivoWindowManagerShellCommandImpl(shell, service);
    }

    public IVivoConditionProviders createVivoConditionProviders(ConditionProviders conditionProviders) {
        return new VivoConditionProvidersImpl(conditionProviders);
    }

    public IVivoFrameworkLockMonitor createVivoFrameworkLockMonitor() {
        IVivoFrameworkLockMonitor vivoFrameworkLockMonitor = VivoFrameworkLockMonitor.getInstance();
        return vivoFrameworkLockMonitor;
    }
}