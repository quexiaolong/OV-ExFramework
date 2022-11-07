package com.android.server.pm.parsing.pkg;

import android.util.SparseArray;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public interface PkgAppInfo {
    String getAppComponentFactory();

    String getBackupAgentName();

    int getBanner();

    int getCategory();

    String getClassLoaderName();

    String getClassName();

    int getCompatibleWidthLimitDp();

    int getCompileSdkVersion();

    String getCompileSdkVersionCodeName();

    int getDescriptionRes();

    int getFullBackupContent();

    int getIconRes();

    int getInstallLocation();

    int getLabelRes();

    int getLargestWidthLimitDp();

    int getLogo();

    long getLongVersionCode();

    String getManageSpaceActivityName();

    float getMaxAspectRatio();

    float getMinAspectRatio();

    int getMinSdkVersion();

    String getNativeLibraryDir();

    String getNativeLibraryRootDir();

    int getNetworkSecurityConfigRes();

    CharSequence getNonLocalizedLabel();

    String getPermission();

    String getPrimaryCpuAbi();

    String getProcessName();

    int getRequiresSmallestWidthDp();

    int getRoundIconRes();

    String getSeInfo();

    String getSeInfoUser();

    String getSecondaryCpuAbi();

    String getSecondaryNativeLibraryDir();

    String[] getSplitClassLoaderNames();

    String[] getSplitCodePaths();

    SparseArray<int[]> getSplitDependencies();

    @Deprecated
    int getTargetSandboxVersion();

    int getTargetSdkVersion();

    String getTaskAffinity();

    int getTheme();

    int getUiOptions();

    int getUid();

    @Deprecated
    int getVersionCode();

    String getVolumeUuid();

    String getZygotePreloadName();

    boolean isAllowAudioPlaybackCapture();

    boolean isAllowBackup();

    boolean isAllowClearUserData();

    boolean isAllowClearUserDataOnFailedRestore();

    boolean isAllowTaskReparenting();

    boolean isAnyDensity();

    boolean isBackupInForeground();

    boolean isBaseHardwareAccelerated();

    boolean isCantSaveState();

    boolean isDebuggable();

    boolean isDefaultToDeviceProtectedStorage();

    boolean isDirectBootAware();

    boolean isEnabled();

    boolean isExternalStorage();

    boolean isExtractNativeLibs();

    boolean isFactoryTest();

    boolean isFullBackupOnly();

    @Deprecated
    boolean isGame();

    boolean isHasCode();

    boolean isHasDomainUrls();

    boolean isHasFragileUserData();

    boolean isIsolatedSplitLoading();

    boolean isKillAfterRestore();

    boolean isLargeHeap();

    boolean isMultiArch();

    boolean isNativeLibraryRootRequiresIsa();

    boolean isOdm();

    boolean isOem();

    boolean isOverlay();

    boolean isPartiallyDirectBootAware();

    boolean isPersistent();

    boolean isPrivileged();

    boolean isProduct();

    boolean isProfileableByShell();

    boolean isRequestLegacyExternalStorage();

    boolean isResizeable();

    boolean isResizeableActivityViaSdkVersion();

    boolean isRestoreAnyVersion();

    boolean isSignedWithPlatformKey();

    boolean isStaticSharedLibrary();

    boolean isSupportsExtraLargeScreens();

    boolean isSupportsLargeScreens();

    boolean isSupportsNormalScreens();

    boolean isSupportsRtl();

    boolean isSupportsSmallScreens();

    boolean isSystem();

    boolean isSystemExt();

    boolean isTestOnly();

    boolean isUseEmbeddedDex();

    boolean isUsesCleartextTraffic();

    boolean isUsesNonSdkApi();

    boolean isVendor();

    boolean isVmSafeMode();
}