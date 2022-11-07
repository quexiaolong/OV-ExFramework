package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.multidisplay.MultiDisplayManager;
import android.os.Debug;
import android.os.FtBuild;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.FtFeature;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.widget.IVigourTierConfig;
import com.android.internal.policy.DrawableUtils;
import com.android.internal.policy.NavigationBarPolicy;
import com.android.server.AttributeCache;
import com.android.server.am.AmsConfigManager;
import com.android.server.am.ProcessRecord;
import com.android.server.am.VivoFrozenPackageSupervisor;
import com.android.server.policy.IVivoRatioControllerUtils;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.android.server.wm.ActivityStack;
import com.google.android.collect.Sets;
import com.vivo.appshare.AppShareConfig;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.superresolution.Constant;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.content.res.VivoThemeResources;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoActivityRecordImpl implements IVivoActivityRecord {
    protected static boolean DBEUG_LETTERBOX = false;
    protected static boolean DEBUG_SPLIT_NAV_COLOR = false;
    private static boolean IS_VOS = false;
    private static final ArrayList<ComponentName> NO_RELAUNCH_ACTIVITY_LIST;
    private static ArrayList<ComponentName> NO_RELAUNCH_ACTIVITY_LIST_GLOBAL_THEME = null;
    private static final ArrayList<String> NO_RELAUNCH_PACKAGE_LIST;
    private static final ArrayList<String> NO_RELAUNCH_PACKAGE_LIST_GLOBAL_THEME;
    private static final String PASSWORDMWUD_CLASS_NAME = "com.vivo.settings.secret.PasswordActivityMultiWindowUD";
    private static final String PASSWORDUD_CLASS_NAME = "com.vivo.settings.secret.PasswordActivityUD";
    private static final String PASSWORD_AUTHEN_CLASS_NAME = "com.vivo.settings.secret.SecretAuthentication";
    private static final String PASSWORD_CLASS_NAME = "com.vivo.settings.secret.PasswordActivity";
    private static final String PASSWORD_CONFIRMTIP_CLASS_NAME = "com.vivo.settings.password.ConfirmTipProblem";
    private static final String PASSWORD_SETSECRET_CLASS_NAME = "com.vivo.settings.secret.SetSecretPin";
    private static final ArrayList<String> RELAUNCH_PACKAGE_LIST;
    private static final ArrayList<String> RELAUNCH_PACKAGE_LIST_GLOBAL_THEME;
    private static final String SUPERPOWER_CLASS_NAME = "com.bbk.SuperPowerSave.SuperPowerSaveActivity";
    static final String TAG = "VivoActivityRecordImpl";
    private static boolean mIsSpecialPlatform;
    private static final boolean mSupportGlobalTheme;
    private static Set<String> mSupportGlobalThemeAppSet;
    private final ComponentName mActivityComponent;
    private ActivityRecord mActivityRecord;
    private ActivityTaskManagerService mAtmService;
    int mGlobalChangesForOrientRelaunch;
    int mGlobalChangesForRelaunch;
    boolean mHasNavBar;
    boolean mInvolveInDisplayAreaAnim;
    private boolean mIsFromFreeform;
    private boolean mIsTranslucent;
    public AbsVivoPerfManager mPerf;
    private SnapshotWindow mSnapshotWin;
    private VivoFrozenPackageSupervisor mVivoFrozenPackageSupervisor;
    private IVivoRatioControllerUtils mVivoRatioControllerUtils;
    boolean mVivoOccludeKeyguard = false;
    private boolean mSkipedRelaunchForSpecial = false;
    private boolean mIgnoreRelaunch = false;
    private final Configuration mTempConfig = new Configuration();
    private boolean mIsRequestActivityRelaunch = false;
    private boolean mLaunchFromSoftware = false;
    private final ArrayList<Integer> navColors = new ArrayList<>();
    private int mLastNavColor = -16777216;
    private boolean mNoWindowApply = true;
    Rect mFrameForLetterbox = new Rect();
    boolean mNeedLetterbox = false;
    boolean mNeedSplitNavBar = false;
    boolean mNeedBlackSplitNavBarForSpecialApp = false;
    ArrayList<WindowState> mContributesWins = new ArrayList<>();
    WindowState mTopWinForLetterbox = null;
    WindowState mLastWinForLetterbox = null;
    boolean mShouldRelativeToWindow = true;
    boolean mHasPreservedForStartingWin = false;
    private final Point mTmpPoint = new Point();
    public AbsVivoPerfManager mPerf_iop = null;
    private boolean mMoveBetweenDefaultAndAppShareDisplay = false;
    final String STR_TOGGLE_SPLIT_REASON_THREEFINGER = "reas_tf";
    private volatile boolean isIgnoreLeftRectOfLetterboxInSplit = false;
    private Rect temp = new Rect();
    private long lastShowSplitNarBarChangeTimeOfToNoNeeded = -1;
    private boolean lastNeedSplitNavBar = false;
    private boolean isDealyHideSplitNav = false;
    private long relaunchOfTime = 0;
    private final int RELAUNCH_DURATION = 2000;
    private final Runnable mUnknowAppHideOrRemoveRunnable = new Runnable() { // from class: com.android.server.wm.VivoActivityRecordImpl.1
        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoActivityRecordImpl.this.mAtmService.mGlobalLock) {
                VSlog.v(VivoActivityRecordImpl.TAG, "setAppVisibility invisible ,remove unknow!");
                VivoActivityRecordImpl.this.mActivityRecord.mDisplayContent.mUnknownAppVisibilityController.appRemovedOrHidden(VivoActivityRecordImpl.this.mActivityRecord);
            }
        }
    };
    final ArrayList<String> ENABLE_STARTWINDOW_WHITELIST = new ArrayList<String>() { // from class: com.android.server.wm.VivoActivityRecordImpl.2
        {
            add("com.umetrip.android.msky.app");
            add("com.sunboxsoft.oilforgdandroid");
            add("com.tztzhonghangsc");
        }
    };
    private int vPreloadFlags = 0;
    private boolean alreadyCheck = false;
    private int mLastDisplayIdForVirtual = Integer.MIN_VALUE;
    private boolean isVirtualDisplayChange = false;
    private boolean mSnapshotDone = false;

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        RELAUNCH_PACKAGE_LIST = arrayList;
        arrayList.add("com.android.vending");
        RELAUNCH_PACKAGE_LIST.add("com.google.android.gm");
        RELAUNCH_PACKAGE_LIST.add("com.google.android.gms");
        RELAUNCH_PACKAGE_LIST.add("com.douban.frodo");
        RELAUNCH_PACKAGE_LIST.add("com.instagram.android");
        RELAUNCH_PACKAGE_LIST.add("jp.naver.line.android");
        ArrayList<String> arrayList2 = new ArrayList<>();
        NO_RELAUNCH_PACKAGE_LIST = arrayList2;
        arrayList2.add("com.ximalaya.ting.android");
        NO_RELAUNCH_PACKAGE_LIST.add("com.microsoft.office.word");
        NO_RELAUNCH_PACKAGE_LIST.add("com.vivo.car.networking");
        NO_RELAUNCH_PACKAGE_LIST.add("com.microsoft.office.excel");
        ArrayList<ComponentName> arrayList3 = new ArrayList<>();
        NO_RELAUNCH_ACTIVITY_LIST = arrayList3;
        arrayList3.add(new ComponentName("com.twitter.android", "com.twitter.camera.controller.root.CameraActivity"));
        NO_RELAUNCH_ACTIVITY_LIST.add(new ComponentName("com.facebook.katana", "com.facebook.inspiration.activity.InspirationCameraActivity"));
        NO_RELAUNCH_ACTIVITY_LIST.add(new ComponentName("cn.wps.moffice_eng", "cn.wps.moffice.spreadsheet.multiactivity.Spreadsheet1"));
        boolean z = true;
        mSupportGlobalTheme = FtFeature.isFeatureSupport("vivo.software.globaltheme") && !IVigourTierConfig.VIGOUR_TIER1_GLOBAL_THEME;
        mSupportGlobalThemeAppSet = new HashSet();
        if (mSupportGlobalTheme) {
            if (IVigourTierConfig.VIGOUR_OVER_SEAS_VOS2) {
                mSupportGlobalThemeAppSet.addAll(Arrays.asList(VivoThemeResources.mSupportGlobalThemePackagesVos));
                mSupportGlobalThemeAppSet.addAll(Arrays.asList(VivoThemeResources.mIsolateSystemResPackagesVos));
            } else if ("Funtouch".equals(FtBuild.getOsName())) {
                mSupportGlobalThemeAppSet.addAll(Arrays.asList(VivoThemeResources.mSupportGlobalThemePackagesFos));
                mSupportGlobalThemeAppSet.addAll(Arrays.asList(VivoThemeResources.mIsolateSystemResPackagesFos));
            }
        }
        RELAUNCH_PACKAGE_LIST_GLOBAL_THEME = new ArrayList<>();
        NO_RELAUNCH_PACKAGE_LIST_GLOBAL_THEME = new ArrayList<>();
        NO_RELAUNCH_ACTIVITY_LIST_GLOBAL_THEME = new ArrayList<>();
        mIsSpecialPlatform = "sdm660".equals(SystemProperties.get("ro.board.platform", "def"));
        DBEUG_LETTERBOX = SystemProperties.getBoolean("persist.vivo.letterbox", false);
        if (!SystemProperties.getBoolean("persist.vivo.multiwindow_navcolor_debug_log", false) && !VivoMultiWindowConfig.DEBUG_ALL_SPLIT_PRIV_LOG) {
            z = false;
        }
        DEBUG_SPLIT_NAV_COLOR = z;
        IS_VOS = FtBuild.getOsName().equals("vos");
    }

    public VivoActivityRecordImpl(ActivityRecord activityRecord, ActivityTaskManagerService atmService, ComponentName activityComponent) {
        this.mPerf = null;
        if (activityRecord == null) {
            VSlog.i(TAG, "container is " + activityRecord);
        }
        this.mActivityRecord = activityRecord;
        this.mAtmService = atmService;
        this.mActivityComponent = activityComponent;
        if (this.mPerf == null) {
            this.mPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        }
        this.mVivoRatioControllerUtils = VivoRatioControllerUtilsImpl.getInstance();
        this.mVivoFrozenPackageSupervisor = VivoFrozenPackageSupervisor.getInstance();
        this.mSnapshotWin = SnapshotWindow.getInstance(this.mActivityRecord.mWmService.mContext);
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public void setInvolveInDisplayAreaAnim(boolean involveInDisplayAreaAnim) {
        if (this.mInvolveInDisplayAreaAnim != involveInDisplayAreaAnim) {
            VSlog.d(TAG, "setInvolveInDisplayAreaAnim " + involveInDisplayAreaAnim + " , " + this.mActivityRecord);
            this.mInvolveInDisplayAreaAnim = involveInDisplayAreaAnim;
        }
    }

    public boolean shouldInvolveInDisplayAreaAnim() {
        return this.mInvolveInDisplayAreaAnim;
    }

    public void changeResizeModeForSplit(String packageName, ActivityInfo info) {
        boolean forceFullscreenActivity = this.mAtmService.isForceFullscreenActivity(this.mActivityComponent.flattenToShortString());
        boolean forceResizeableActivity = this.mAtmService.isForceResizableActivity(this.mActivityComponent.flattenToShortString());
        boolean forceUnResizeApp = this.mAtmService.isForceUnResizeApp(packageName);
        boolean forceResizeableApp = this.mAtmService.isForceResizeApp(packageName);
        if (this.mAtmService.isMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_AM_RESIZABLE_PROPERTY) {
            if (forceFullscreenActivity) {
                info.resizeMode = 0;
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.v(TAG, "changeResizeModeForSplit " + this + " force info.resizeMode to RESIZE_MODE_UNRESIZEABLE:packageName=" + packageName + " mActivityComponent=" + this.mActivityComponent.flattenToShortString());
                }
            } else if (forceResizeableActivity) {
                info.resizeMode = 2;
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.v(TAG, "changeResizeModeForSplit " + this + " force info.resizeMode to RESIZE_MODE_RESIZEABLE:packageName=" + packageName + " mActivityComponent=" + this.mActivityComponent.flattenToShortString());
                }
            } else if (forceUnResizeApp) {
                info.resizeMode = 0;
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.v(TAG, "changeResizeModeForSplit " + this + " force info.resizeMode to RESIZE_MODE_UNRESIZEABLE:packageName=" + packageName + " mActivityComponent=" + this.mActivityComponent.flattenToShortString());
                }
            } else if (forceResizeableApp) {
                info.resizeMode = 2;
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.v(TAG, "changeResizeModeForSplit " + this + " force info.resizeMode to RESIZE_MODE_RESIZEABLE:packageName=" + packageName + " mActivityComponent=" + this.mActivityComponent.flattenToShortString());
                }
            }
        }
        if (VivoMultiWindowConfig.DEBUG) {
            VSlog.v(TAG, "changeResizeModeForSplit " + this + " packageName=" + packageName + " mActivityComponent=" + this.mActivityComponent.flattenToShortString() + " info.resizeMode=" + info.resizeMode);
        }
    }

    public int initGlobalChanges(MergedConfiguration lastReportedConfiguration, int changes) {
        if (!this.mAtmService.isMultiWindowSupport() || !VivoMultiWindowConfig.IS_VIVO_AM_RELAUNCH_PROPERTY) {
            return changes;
        }
        if (!this.mAtmService.isInMultiWindowFocusedDisplay() && !this.mAtmService.mWindowManager.isVivoDockedDividerResizing()) {
            return changes;
        }
        Configuration currentGlobalConfig = this.mAtmService.getGlobalConfiguration();
        Configuration lastGlobalConfig = lastReportedConfiguration.getGlobalConfiguration();
        String fullActivityName = this.mActivityComponent.flattenToShortString();
        this.mGlobalChangesForOrientRelaunch = 0;
        if (currentGlobalConfig != null && lastGlobalConfig != null && this.mAtmService.isIgnoreRelaunchOrientationActivity(fullActivityName)) {
            this.mGlobalChangesForOrientRelaunch = lastGlobalConfig.diff(currentGlobalConfig);
        }
        this.mGlobalChangesForRelaunch = 0;
        if (currentGlobalConfig == null || lastGlobalConfig == null || !this.mAtmService.isNeedRelaunchOrientationActivity(fullActivityName)) {
            return changes;
        }
        int globalChangesNow = lastGlobalConfig.diff(currentGlobalConfig);
        this.mGlobalChangesForRelaunch = globalChangesNow;
        if ((globalChangesNow & 128) == 0) {
            return changes;
        }
        int change = changes | 128;
        return change;
    }

    public boolean skipFreezingProcess(WindowProcessController app) {
        if (this.mAtmService.isVivoMultiWindowSupport() && this.mActivityRecord.getDisplayId() == 0 && this.mAtmService.isInMultiWindowFocusedDisplay() && this.mAtmService.getVivoSwapDockInFreezeProcess()) {
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.e(TAG, "skip startFreezingScreenLocked app=" + app + Debug.getCallers(10));
                return true;
            }
            return true;
        }
        return false;
    }

    public int relaunchOrientationActivity(int configChanged) {
        String fullActivityName = this.mActivityComponent.flattenToShortString();
        boolean isExitedJust = this.mActivityRecord.getDisplay() != null && this.mActivityRecord.getDisplay().isVivoMultiWindowExitedJustWithDisplay();
        if (!this.mAtmService.isMultiWindowSupport() || !VivoMultiWindowConfig.IS_VIVO_AM_RELAUNCH_PROPERTY) {
            return configChanged;
        }
        if ((!this.mAtmService.isInMultiWindowFocusedDisplay() && (this.mActivityRecord.getDisplay() == null || this.mActivityRecord.getDisplayId() != 0 || !isExitedJust)) || 128 != (this.mGlobalChangesForRelaunch & 128) || !this.mAtmService.isNeedRelaunchOrientationActivity(fullActivityName)) {
            return configChanged;
        }
        if (VivoMultiWindowConfig.DEBUG) {
            VSlog.v(TAG, "Need Relaunching(orientation) for " + fullActivityName);
        }
        int config = configChanged & (-129);
        return config;
    }

    public boolean shouldSkipRelaunchForSplit(int changes, int windowingMode) {
        boolean bSkipRelaunch = false;
        String pkgName = this.mActivityComponent.getPackageName();
        String fullActivityName = this.mActivityComponent.flattenToShortString();
        boolean isExitedJustSplit = this.mActivityRecord.getDisplay() != null && this.mActivityRecord.getDisplayId() == 0 && this.mActivityRecord.getDisplay().isVivoMultiWindowExitedJustWithDisplay();
        if (this.mAtmService.isMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_AM_RELAUNCH_PROPERTY && (this.mAtmService.isInMultiWindowFocusedDisplay() || isExitedJustSplit)) {
            if (128 == (this.mGlobalChangesForRelaunch & 128) && this.mAtmService.isNeedRelaunchOrientationActivity(fullActivityName)) {
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.v(TAG, "Need Relaunching(orientation) for " + fullActivityName);
                }
            } else if (this.mAtmService.isNeedRelaunchApp(pkgName) || this.mAtmService.isNeedRelaunchActivity(fullActivityName)) {
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.v(TAG, "Need Relaunching for " + fullActivityName);
                }
            } else if (128 == (this.mGlobalChangesForOrientRelaunch & 128) && 4 != (changes & 4) && 512 != (changes & 512) && this.mAtmService.isIgnoreRelaunchOrientationActivity(fullActivityName)) {
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.v(TAG, "skip orientation Relaunching for " + fullActivityName);
                }
                bSkipRelaunch = true;
            } else if ((this.mAtmService.isIgnoreRelaunchApp(pkgName) || this.mAtmService.isIgnoreRelaunchActivity(fullActivityName)) && 4 != (changes & 4) && 512 != (changes & 512) && (!this.mAtmService.getVivoFloatMessageFlag() || !this.mAtmService.isNotIgnoreRelaunchActivityWhenFloatMsg(fullActivityName) || 4 != windowingMode)) {
                if (this.mAtmService.isIgnoreRelaunchActivityAlreadySplit(fullActivityName) && !this.mAtmService.getVivoSwapDockInFreezeProcess() && !isExitedJustSplit) {
                    if (VivoMultiWindowConfig.DEBUG) {
                        VSlog.v(TAG, "Ignore Relaunching: " + this + " after split and fullActivityName=" + fullActivityName);
                    }
                    bSkipRelaunch = true;
                } else {
                    if (VivoMultiWindowConfig.DEBUG) {
                        VSlog.v(TAG, "Skip Relaunching for " + fullActivityName);
                    }
                    bSkipRelaunch = true;
                }
            }
            if (bSkipRelaunch && VivoMultiWindowConfig.DEBUG) {
                VSlog.v(TAG, "Skip Relaunching: " + this + " fullActivityName=" + fullActivityName + " configChange=" + Configuration.configurationDiffToString(changes) + " isVivoMultiWindowExitedJustWithDisplay() = " + isExitedJustSplit + " mStackSupervisor.mService.isInMultiWindowFocusedDisplay()=" + this.mAtmService.isInMultiWindowFocusedDisplay() + " mStackSupervisor.mWindowManager.isVivoDockedDividerResizing()=" + this.mAtmService.mWindowManager.isVivoDockedDividerResizing());
            }
        }
        return bSkipRelaunch;
    }

    public boolean skipContainerOfSurfaceUpdateInSplit() {
        ActivityTaskManagerService activityTaskManagerService;
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null && activityRecord.getDisplay() != null && this.mActivityRecord.getDisplayId() == 0 && this.mActivityRecord.inSplitScreenPrimaryWindowingMode() && (activityTaskManagerService = this.mAtmService) != null && activityTaskManagerService.isMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_ENTER_SPLIT_OPTIMIZE && this.mAtmService.isVivoEnteringMultiWindowDefaultDisplay()) {
            ActivityStack stack = this.mActivityRecord.getStack();
            ActivityRecord top = stack != null ? stack.topRunningActivity() : null;
            boolean isTop = top == this.mActivityRecord;
            String way = this.mAtmService.getVivoEnterSplitWay();
            if (isTop && this.mActivityRecord.mVisibleRequested && this.mActivityRecord.isClientVisible() && way != null && "reas_tf".equals(way)) {
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.v(TAG, "skipContainerOfSurfaceUpdateInSplit of record with " + this.mActivityRecord);
                }
                return true;
            }
        }
        return false;
    }

    public boolean isMultiWindowAppListActivity() {
        ComponentName componentName = this.mActivityComponent;
        if (componentName == null || componentName.getClassName() == null) {
            return false;
        }
        return this.mActivityComponent.getClassName().equals(Constant.ACTIVITY_LAUNCHER_SMART);
    }

    public boolean isMultiwindowPasswrdActivity() {
        ComponentName componentName = this.mActivityComponent;
        if (componentName == null || componentName.getClassName() == null) {
            return false;
        }
        return this.mActivityComponent.getClassName().contains("com.vivo.settings.secret.PasswordActivityMultiWindow");
    }

    public boolean isOurRecentsActivity() {
        ComponentName componentName = this.mActivityComponent;
        if (componentName == null || componentName.getClassName() == null) {
            return false;
        }
        return this.mActivityComponent.getClassName().equals("com.vivo.upslide.recents.RecentsActivity");
    }

    public boolean skipSetVisableWhenFloatMessage(boolean visible, String packageName) {
        if (this.mAtmService.isMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_SWITCH_ANIM && this.mAtmService.isInMultiWindowFocusedDisplay() && visible && this.mAtmService.getVivoIgnoreSetVisableFlag()) {
            if (VivoMultiWindowConfig.getInstance().skipSetVisableWhenFloatMessage(packageName)) {
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.d(TAG, "skip setVisibility when in FloatMessage state for " + packageName);
                    return true;
                }
                return true;
            } else if (packageName != null && packageName.equals(this.mAtmService.getVivoPendingPackageInSplit()) && (!"com.viber.voip".equals(packageName) || !"com.viber.voip.WelcomeActivity".equals(this.mActivityComponent.getClassName()))) {
                this.mAtmService.setVivoIgnoreSetVisableFlag(false);
            }
        }
        return false;
    }

    public boolean isAlwaysOpaqueInSplit(boolean reallyVisible) {
        if (this.mAtmService.isVivoVosMultiWindowSupport() && this.mAtmService.isInMultiWindowFocusedDisplay() && reallyVisible && isTransitActivity()) {
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.v(TAG, " AlwaysOpaqueInSplit for activity " + this.mActivityRecord);
                return false;
            }
            return false;
        }
        return true;
    }

    private boolean isTransitActivity() {
        ComponentName componentName = this.mActivityComponent;
        if (componentName == null || componentName.flattenToString() == null) {
            return false;
        }
        return this.mActivityComponent.flattenToString().equals("com.google.android.apps.messaging/com.google.android.apps.messaging.welcome.WelcomeActivity");
    }

    public boolean isMatchSnapShotSizeIfNeeded(ActivityManager.TaskSnapshot snapshot) {
        boolean mSizeMismatch = false;
        if (this.mAtmService.isMultiWindowSupport()) {
            if (this.mActivityRecord.getTask() != null && this.mActivityRecord.getTask().mExitSplitWithNoResize) {
                VSlog.v(TAG, "isMatchSnapShotSize of " + this.mActivityRecord.getTask().mTaskId + " snapshot " + snapshot);
                this.mActivityRecord.getTask().mExitSplitWithNoResize = false;
                return false;
            }
            if (snapshot != null && this.mActivityRecord.getTask() != null && this.mActivityRecord.getDisplayId() == 0 && !this.mAtmService.isInMultiWindowFocusedDisplay() && this.mAtmService.mWindowManager.mTaskSnapshotController.isSplitSnapshot(this.mActivityRecord.getTask().mTaskId)) {
                DisplayContent displayContent = this.mActivityRecord.getDisplayContent();
                int dw = displayContent.getDisplayInfo().logicalWidth;
                int dh = displayContent.getDisplayInfo().logicalHeight;
                mSizeMismatch = Math.abs(dw - snapshot.getSnapshot().getWidth()) > dw / 3 || Math.abs(dh - snapshot.getSnapshot().getHeight()) > dh / 3;
                VSlog.v(TAG, "isMatchSnapShotSize of " + snapshot + ",to match " + mSizeMismatch);
            }
            return !mSizeMismatch;
        }
        return true;
    }

    public boolean isDefaultHomeActivity() {
        ComponentName componentName = this.mActivityComponent;
        if (componentName == null || componentName.getClassName() == null) {
            return false;
        }
        return this.mActivityComponent.getClassName().equals(" com.bbk.launcher2.Launcher");
    }

    public void debugUpdateMultiWindowMode(String shortComponentName, boolean inMultiWindowMode, boolean lastMultiWindowMode) {
        if (VivoMultiWindowConfig.DEBUG) {
            VSlog.v(TAG, "UpdateMultiWindowMode shortComponentName=" + shortComponentName + " inMultiWindowMode = " + inMultiWindowMode + " lastMultiWindowMode = " + lastMultiWindowMode);
        }
    }

    public boolean preserveWindowForExitSplit(String packageName) {
        if ("com.android.bbkcalculator".equals(packageName) && this.mAtmService.isVivoMultiWindowSupport() && this.mAtmService.mWindowManager.getDefaultDisplayContentLocked().isVivoMultiWindowExitedJustWithDisplay()) {
            return true;
        }
        return false;
    }

    public boolean setClientInVisibleForSecondarySplit(boolean pause, boolean visibleRequested, boolean visible) {
        if (pause && !visibleRequested && !visible && !this.mActivityRecord.isActivityTypeHome() && this.mActivityRecord.getWindowingMode() == 4) {
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.d(TAG, "setClientInVisibleForSecondarySplit " + this.mActivityRecord);
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean isFrozenProcess(WindowProcessController app, PrintWriter pw) {
        if (app != null && app.mInfo != null && app.mInfo.packageName != null && this.mVivoFrozenPackageSupervisor.isFrozenPackage(app.mInfo.packageName, app.mInfo.uid)) {
            if (pw != null) {
                pw.println("\n** this package: " + app.mInfo.packageName + " has been frozen **");
                return true;
            }
            return true;
        }
        return false;
    }

    /* JADX WARN: Can't wrap try/catch for region: R(29:5|6|7|8|9|10|(6:77|78|79|(1:81)(1:85)|82|(21:84|13|14|15|16|17|18|19|(3:24|(1:29)|28)|34|(2:35|(2:37|(2:40|41)(1:39))(1:69))|42|(2:43|(2:45|(2:48|49)(1:47))(1:68))|(1:51)(1:67)|52|(1:54)|55|56|(1:63)(1:60)|61|62))|12|13|14|15|16|17|18|19|(1:70)(6:21|22|24|(1:26)|29|28)|34|(3:35|(0)(0)|39)|42|(3:43|(0)(0)|47)|(0)(0)|52|(0)|55|56|(0)|63|61|62) */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x009e, code lost:
        if (r4.metaData == null) goto L34;
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x00a0, code lost:
        r6 = r4.metaData.getBoolean("android.vivo_nightmode_support", true);
        r8 = r4.metaData.getBoolean("android.vivo_nightmode_relaunch", true);
     */
    /* JADX WARN: Code restructure failed: missing block: B:62:0x00fb, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:63:0x00fc, code lost:
        r5 = r5;
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x00ff, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:65:0x0100, code lost:
        r5 = r5;
     */
    /* JADX WARN: Removed duplicated region for block: B:44:0x00bd A[Catch: Exception -> 0x00fb, TryCatch #1 {Exception -> 0x00fb, blocks: (B:24:0x0071, B:28:0x007b, B:30:0x007f, B:32:0x0089, B:38:0x009c, B:40:0x00a0, B:41:0x00b1, B:42:0x00b7, B:44:0x00bd, B:48:0x00cc, B:49:0x00d2, B:51:0x00d8, B:56:0x00e9, B:58:0x00ef), top: B:83:0x0071 }] */
    /* JADX WARN: Removed duplicated region for block: B:51:0x00d8 A[Catch: Exception -> 0x00fb, TryCatch #1 {Exception -> 0x00fb, blocks: (B:24:0x0071, B:28:0x007b, B:30:0x007f, B:32:0x0089, B:38:0x009c, B:40:0x00a0, B:41:0x00b1, B:42:0x00b7, B:44:0x00bd, B:48:0x00cc, B:49:0x00d2, B:51:0x00d8, B:56:0x00e9, B:58:0x00ef), top: B:83:0x0071 }] */
    /* JADX WARN: Removed duplicated region for block: B:56:0x00e9 A[Catch: Exception -> 0x00fb, TryCatch #1 {Exception -> 0x00fb, blocks: (B:24:0x0071, B:28:0x007b, B:30:0x007f, B:32:0x0089, B:38:0x009c, B:40:0x00a0, B:41:0x00b1, B:42:0x00b7, B:44:0x00bd, B:48:0x00cc, B:49:0x00d2, B:51:0x00d8, B:56:0x00e9, B:58:0x00ef), top: B:83:0x0071 }] */
    /* JADX WARN: Removed duplicated region for block: B:57:0x00ee  */
    /* JADX WARN: Removed duplicated region for block: B:60:0x00f7  */
    /* JADX WARN: Removed duplicated region for block: B:73:0x0159 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:92:0x00cc A[EDGE_INSN: B:92:0x00cc->B:48:0x00cc ?: BREAK  , SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:94:0x00e7 A[EDGE_INSN: B:94:0x00e7->B:55:0x00e7 ?: BREAK  , SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public int getConfigChangedFromNightMode(java.lang.String r21, android.content.pm.ActivityInfo r22, android.util.MergedConfiguration r23, int r24, int r25) {
        /*
            Method dump skipped, instructions count: 432
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoActivityRecordImpl.getConfigChangedFromNightMode(java.lang.String, android.content.pm.ActivityInfo, android.util.MergedConfiguration, int, int):int");
    }

    public int getConfigChangedFromGlobalTheme(ActivityInfo info, int configChanged, int mUserId) {
        ComponentName componentName = null;
        boolean actRelaunch = true;
        boolean actRelaunchRemove = false;
        boolean forceRelaunch = false;
        boolean noRelaunch = false;
        try {
            IPackageManager pm = ActivityThread.getPackageManager();
            ActivityInfo actIn = pm.getActivityInfo(info.getComponentName(), 128, mUserId);
            if (actIn != null && actIn.metaData != null) {
                actRelaunchRemove = actIn.metaData.getBoolean("android.vivo_globaltheme_relaunch_remove", false);
                actRelaunch = actIn.metaData.getBoolean("android.vivo_globaltheme_relaunch", true);
            }
            componentName = info != null ? info.getComponentName() : null;
            String packageName = componentName != null ? componentName.getPackageName() : null;
            if (!mSupportGlobalThemeAppSet.contains(packageName)) {
                noRelaunch = true;
            }
            Iterator<String> it = RELAUNCH_PACKAGE_LIST_GLOBAL_THEME.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                String pack = it.next();
                if (pack.equals(packageName)) {
                    forceRelaunch = true;
                    break;
                }
            }
            Iterator<String> it2 = NO_RELAUNCH_PACKAGE_LIST_GLOBAL_THEME.iterator();
            while (true) {
                if (!it2.hasNext()) {
                    break;
                }
                String pack2 = it2.next();
                if (pack2.equals(packageName)) {
                    noRelaunch = true;
                    break;
                }
            }
            if (NO_RELAUNCH_ACTIVITY_LIST_GLOBAL_THEME.contains(componentName)) {
                noRelaunch = true;
            }
            if (actRelaunchRemove) {
                noRelaunch = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        int mConfigChanged = ((noRelaunch || !actRelaunch) && !forceRelaunch) ? configChanged | Integer.MIN_VALUE : configChanged & Integer.MAX_VALUE;
        VSlog.d(TAG, "global theme : name = " + componentName + ", noRelaunch = " + noRelaunch + ", actRelaunch = " + actRelaunch + ", actRelaunchRemove = " + actRelaunchRemove + ", forceRelaunch = " + forceRelaunch + ", final change = " + mConfigChanged);
        return mConfigChanged;
    }

    public boolean changeSupportsFreeform() {
        return (this.mAtmService.mSupportsFreeformWindowManagement || this.mAtmService.isVivoFreeFormValid()) && changeSupportsResizeableMultiWindow() && !isPasswordActivity();
    }

    private boolean changeSupportsResizeableMultiWindow() {
        return this.mAtmService.mSupportsMultiWindow && !this.mActivityRecord.isActivityTypeHome() && (ActivityInfo.isResizeableMode(this.mActivityRecord.info.resizeMode) || this.mAtmService.mForceResizableActivities || this.mAtmService.isVivoFreeFormValid());
    }

    public boolean isPasswordActivity() {
        return (this.mActivityRecord.mActivityComponent == null || this.mActivityRecord.mActivityComponent.getClassName() == null || (!this.mActivityRecord.mActivityComponent.getClassName().equals(PASSWORD_CLASS_NAME) && !this.mActivityRecord.mActivityComponent.getClassName().equals(PASSWORDUD_CLASS_NAME) && !this.mActivityRecord.mActivityComponent.getClassName().equals(PASSWORDMWUD_CLASS_NAME))) ? false : true;
    }

    public boolean isPasswordAuthenActivity() {
        if (this.mActivityRecord.mActivityComponent == null || this.mActivityRecord.mActivityComponent.getClassName() == null) {
            return false;
        }
        return this.mActivityRecord.mActivityComponent.getClassName().equals(PASSWORD_AUTHEN_CLASS_NAME) || this.mActivityRecord.mActivityComponent.getClassName().equals(PASSWORD_CONFIRMTIP_CLASS_NAME) || this.mActivityRecord.mActivityComponent.getClassName().equals(PASSWORD_SETSECRET_CLASS_NAME);
    }

    public boolean isSuperPowerActivity() {
        return (this.mActivityRecord.mActivityComponent == null || this.mActivityRecord.mActivityComponent.getClassName() == null || !this.mActivityRecord.mActivityComponent.getClassName().equals(SUPERPOWER_CLASS_NAME)) ? false : true;
    }

    public boolean changeResizeableForVivoFreeform(ActivityInfo info) {
        boolean result = this.mAtmService.mForceResizableActivities || ActivityInfo.isResizeableMode(info.resizeMode) || info.supportsPictureInPicture();
        if (!this.mAtmService.isVivoFreeFormValid()) {
            return result;
        }
        boolean isFreeformEnableApps = isFreeformEnableApps(this.mActivityRecord);
        boolean isDirectFreeformEnableApps = isDirectFreeformEnableApps(this.mActivityRecord);
        return result || isFreeformEnableApps || isDirectFreeformEnableApps;
    }

    private boolean isFreeformEnableApps(ActivityRecord activityRecord) {
        boolean isFreeformEnableApps = activityRecord.mActivityComponent != null && this.mAtmService.getFreeFormEnabledApp().contains(activityRecord.mActivityComponent.getPackageName());
        Task task = activityRecord.getTask();
        boolean isTaskStartedByFreeformEnableApps = (task == null || task.realActivity == null || !this.mAtmService.getFreeFormEnabledApp().contains(task.realActivity.getPackageName())) ? false : true;
        boolean isFreeformStackTopRun = isFreeformStackTopRun(activityRecord);
        if (isFreeformEnableApps) {
            return true;
        }
        return isTaskStartedByFreeformEnableApps && (activityRecord.inFreeformWindowingMode() || isFreeformStackTopRun);
    }

    private boolean isDirectFreeformEnableApps(ActivityRecord activityRecord) {
        boolean isFreeformStackTopRun = isFreeformStackTopRun(activityRecord);
        return this.mAtmService.isInDirectFreeformState() && (activityRecord.inFreeformWindowingMode() || isFreeformStackTopRun) && activityRecord.supportsFreeform();
    }

    private boolean isFreeformStackTopRun(ActivityRecord activityRecord) {
        ActivityStack stack = activityRecord.getStack();
        ActivityRecord stackTopRun = stack != null ? stack.topRunningActivityLocked() : null;
        return stackTopRun != null && stack.inFreeformWindowingMode() && activityRecord == stackTopRun;
    }

    public boolean isEmergentActivity() {
        return this.mActivityRecord.mActivityComponent != null && this.mAtmService.getFreeFormEmergentActivity().contains(this.mActivityRecord.mActivityComponent.flattenToShortString());
    }

    public boolean isNeedExitFreeformActivity() {
        return "com.facebook.katana/.dbl.activity.FacebookLoginActivity".contains(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "com.android.vending/com.google.android.finsky.activities.MainActivity".contains(this.mActivityRecord.mActivityComponent.flattenToShortString());
    }

    public boolean isShareChooserActivity() {
        return this.mActivityRecord.mActivityComponent != null && "com.android.internal.app.ChooserActivity".equals(this.mActivityRecord.mActivityComponent.getClassName());
    }

    public boolean isVivoEditWidgetActivity() {
        return this.mActivityRecord.mActivityComponent != null && ("com.android.notes/.EditWidget".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "com.vivo.notes/.EditWidget".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()));
    }

    public boolean isVivoBillEditActivity() {
        return this.mActivityRecord.mActivityComponent != null && ("com.android.notes/.NotesBillEditActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "com.vivo.notes/.NotesBillEditActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || isSpecialUnresizeableActivity());
    }

    private boolean isSpecialUnresizeableActivity() {
        return this.mActivityRecord.mActivityComponent != null && ("com.ss.android.article.video/.activity.SplashActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "com.chaoxing.mobile/.study.account.LoginActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "com.chaoxing.mobile/.main.ui.MainTabActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "com.tencent.weishi/com.tencent.oscar.module.main.MainActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "com.baidu.BaiduMap/com.baidu.baidumaps.MapsActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "ctrip.android.view/ctrip.business.splash.CtripSplashActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "ctrip.android.view/ctrip.android.publicproduct.home.view.CtripHomeActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()));
    }

    public boolean isVivoRecentFilesActivity() {
        return this.mActivityRecord.mActivityComponent != null && "com.android.filemanager/.recent.litefiles.view.activity.LiteRecentFilesClassifyActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString());
    }

    public boolean isWhatsAppRegisterActivity() {
        return this.mActivityRecord.mActivityComponent != null && "com.whatsapp/.registration.EULA".equals(this.mActivityRecord.mActivityComponent.flattenToShortString());
    }

    public boolean isForceFullScreenForFreeForm() {
        return false;
    }

    public boolean isSpecialVideoActivityCantDeliverIntent() {
        return this.mActivityRecord.mActivityComponent != null && ("com.sohu.sohuvideo/.mvp.ui.activity.VideoDetailActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "in.amazon.mShop.android.shopping/com.amazon.mShop.home.web.MShopWebGatewayMigrationActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()));
    }

    public boolean isSpecialVideoActivityCantGoLandMode() {
        return this.mActivityRecord.mActivityComponent != null && ("com.qiyi.video/org.iqiyi.video.activity.PlayerActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "tv.pps.mobile/org.qiyi.android.video.MainActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "com.sohu.sohuvideo/.mvp.ui.activity.VideoDetailActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "com.tencent.qqlive/.ona.activity.VideoDetailActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "com.le123.ysdq/com.elinkway.infinitemovies.ui.activity.NewMainActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()) || "com.le123.ysdq/com.elinkway.infinitemovies.play.core.VideoDetailActivity".equals(this.mActivityRecord.mActivityComponent.flattenToShortString()));
    }

    public void reUpdateConfigurationForVivoFreeform(Task task) {
        if (task != null && this.mAtmService.isVivoFreeFormValid() && this.mAtmService.isInVivoFreeform()) {
            if (this.mAtmService.mWindowManager.isEnteringFreeForm() || (this.mActivityRecord.getStack() != null && this.mActivityRecord.getStack().inFreeformWindowingMode())) {
                if (isVivoEditWidgetActivity() || isVivoRecentFilesActivity() || isVivoBillEditActivity() || task.realActivity.flattenToShortString().contains("EditWidget")) {
                    task.onRequestedOverrideConfigurationChanged(task.getRequestedOverrideConfiguration());
                }
            }
        }
    }

    public void ensureTargetStackInFrontAndGetFocused(ActivityStack stack, String reason) {
        if (this.mAtmService.isVivoFreeFormValid() && this.mAtmService.isInVivoFreeform() && stack.getWindowingMode() == 1 && stack.getActivityType() == 1) {
            if (!this.mAtmService.isFreeFormMin() && ("finishActivity adjustFocus".equals(reason) || "moveTaskToBackLocked(freeform) adjustFocus".equals(reason) || "bringingFoundTaskToFront".equals(reason))) {
                stack.moveToFront(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, this.mActivityRecord.getTask());
            } else if (this.mAtmService.isFreeFormMin() && "setFocusedTask".equals(reason)) {
                stack.getDisplay().setFocusedApp(this.mActivityRecord, true);
            }
        }
    }

    public boolean skipMoveFreeformToFrontWhenGoningToMinimize(ActivityStack stack, String reason) {
        if (this.mAtmService.isVivoFreeFormValid() && this.mAtmService.isInVivoFreeform() && stack.getWindowingMode() == 5 && this.mAtmService.isFreeFormMin() && "setFocusedTask".equals(reason)) {
            return true;
        }
        return false;
    }

    public boolean skipMoveFreeformToFrontWhenClickToExit(Task task, String reason) {
        if (this.mAtmService.mWindowManager.isSettingFreeformTaskFocused()) {
            this.mAtmService.mWindowManager.setSettingFreeformTaskFocused(false);
            if (this.mAtmService.mWindowManager.isVivoFreeformFeatureSupport() && "setFocusedTask".equals(reason) && this.mAtmService.mWindowManager.isExitingFreeForm() && task != null && !task.inFreeformWindowingMode()) {
                return true;
            }
        } else if (this.mAtmService.isVivoFreeFormValid() && this.mAtmService.isInVivoFreeform() && task != null) {
            ActivityStack stack = task.getStack();
            if (task.getParent() == null && stack != null && stack.inFreeformWindowingMode() && this.mAtmService.getFreeformKeepR() != null) {
                VSlog.d(TAG, "during reparent exit freeform task:" + task);
                return true;
            }
        }
        return false;
    }

    public void updateFreeformTaskSnapShotIfNeed(Task task) {
        if (this.mAtmService.isVivoFreeFormValid() && this.mAtmService.isInVivoFreeform() && task != null && task.inFreeformWindowingMode() && !this.mActivityRecord.isVisible()) {
            if (this.mAtmService.mRootWindowContainer.topRunningActivity() == null || !this.mAtmService.mRootWindowContainer.topRunningActivity().isPasswordActivity()) {
                this.mAtmService.mWindowManager.updateFreeformTaskSnapshot(task);
            }
        }
    }

    public void scheduleIdleToEnsureFreeformFinishIfNeed() {
        if (this.mAtmService.isVivoFreeFormValid() && this.mAtmService.isInVivoFreeform() && this.mActivityRecord.getState() == ActivityStack.ActivityState.STOPPING && this.mActivityRecord.finishing && this.mActivityRecord.getTask().inFreeformWindowingMode()) {
            VSlog.d(TAG, "windowsGone --- correct the state of activity in FreeForm mode.");
            this.mActivityRecord.mStackSupervisor.scheduleIdle();
        }
    }

    public boolean shouldIgnoreShowStartingWindowForFreeform() {
        if (this.mAtmService.isVivoFreeFormValid() && this.mAtmService.isInVivoFreeform() && this.mActivityRecord.getWindowingMode() == 5) {
            VSlog.d(TAG, "IgnoreShowStartingWindowForFreeform : " + this.mActivityRecord);
            return true;
        }
        return false;
    }

    public boolean ignoreUseSizeCompatModeForFreeform() {
        Task task = this.mActivityRecord.getTask();
        ActivityRecord topRun = task != null ? task.topRunningActivityLocked() : null;
        if (task != null && task.isFromVivoFreeform()) {
            if (isVivoEditWidgetActivity() || isVivoBillEditActivity()) {
                return true;
            }
            if (topRun != null) {
                return topRun.isVivoEditWidgetActivity() || topRun.isVivoBillEditActivity();
            }
            return false;
        }
        return false;
    }

    public boolean ignoreRelaunchWhenTaskBoundsNotChangeInFreform() {
        Task task;
        if (this.mAtmService.isVivoFreeFormValid() && this.mAtmService.isInVivoFreeform() && (task = this.mActivityRecord.getTask()) != null && this.mActivityRecord.getStack() != null && task.getLastBounds() != null && task.inFreeformWindowingMode() && task.equivalentRequestedOverrideBounds(task.getLastBounds()) && !getIsRequestActivityRelaunch()) {
            return true;
        }
        return false;
    }

    public boolean stopFreezingForVivoFreeform() {
        if (this.mAtmService.isVivoFreeFormValid() && this.mAtmService.isInVivoFreeform()) {
            ActivityStack activityStack = this.mActivityRecord.getStack();
            Handler handler = activityStack != null ? activityStack.getHandler() : null;
            if (this.mAtmService.mWindowManager.isScreenRotating(this.mActivityRecord.getDisplay().mDisplayContent) && handler != null) {
                handler.removeMessages(KernelConfig.DBG_LOOP_BACK_MODE);
                Message msg = handler.obtainMessage(KernelConfig.DBG_LOOP_BACK_MODE);
                msg.obj = this.mActivityRecord;
                msg.arg1 = 1;
                handler.sendMessageDelayed(msg, 1000L);
            }
            if (this.mSkipedRelaunchForSpecial) {
                this.mSkipedRelaunchForSpecial = false;
                return true;
            }
        }
        return false;
    }

    public boolean notRelaunchForSpecialInFreeform(Task task, boolean andResume) {
        if ((this.mAtmService.isVivoFreeFormValid() && this.mAtmService.isInVivoFreeform()) || (this.mAtmService.mWindowManager.isExitingFreeForm() && this.mIgnoreRelaunch)) {
            if (task != null) {
                task.getRequestedOverrideConfiguration();
            }
            if (("com.viber.voip".equals(this.mActivityRecord.mActivityComponent.getPackageName()) && this.mAtmService.mWindowManager.isScreenRotating(this.mActivityRecord.getDisplay().mDisplayContent)) || (("com.tencent.mobileqq".equals(this.mActivityRecord.mActivityComponent.getPackageName()) && this.mAtmService.mWindowManager.isEnteringFreeForm() && andResume) || this.mIgnoreRelaunch)) {
                this.mActivityRecord.configChangeFlags = 0;
                this.mIgnoreRelaunch = false;
                this.mSkipedRelaunchForSpecial = true;
                return true;
            }
        }
        if (this.mAtmService.mWindowManager.isVivoFreeformFeatureSupport() && this.mAtmService.mWindowManager.isExitingFreeForm() && Constant.APP_WEIXIN.equals(this.mActivityRecord.mActivityComponent.getPackageName())) {
            Configuration taskConfig = task != null ? task.getMergedOverrideConfiguration() : null;
            if (taskConfig != null && taskConfig.windowConfiguration.getWindowingMode() != 5) {
                for (int index = task.mChildren.size() - 2; index >= 0; index--) {
                    ActivityRecord activityRecord = (ActivityRecord) task.mChildren.get(index);
                }
            }
        }
        return false;
    }

    public void setRequestActivityRelaunch(boolean request) {
        this.mIsRequestActivityRelaunch = request;
    }

    public boolean getIsRequestActivityRelaunch() {
        return this.mIsRequestActivityRelaunch;
    }

    public void setLaunchFromSoftware(boolean launchFromSoftware) {
        this.mLaunchFromSoftware = launchFromSoftware;
    }

    public boolean isLaunchFromSoftware() {
        return this.mLaunchFromSoftware;
    }

    public boolean notShowSnapshotStartingWindowInFreeform() {
        if (this.mAtmService.isVivoFreeFormValid() && this.mAtmService.isInVivoFreeform() && this.mActivityRecord.inFreeformWindowingMode()) {
            VSlog.d(TAG, "Don't show snapshot starting window when in freeform mode");
            return true;
        }
        return false;
    }

    public void setIsFromFreeform(boolean fromFreeform) {
        this.mIsFromFreeform = fromFreeform;
    }

    public boolean getIsFromFreeform() {
        return this.mIsFromFreeform;
    }

    public void onFreeformAnimationFinished() {
        this.mIsFromFreeform = false;
        this.mActivityRecord.mWmService.setFreeformAnimating(false);
    }

    public boolean isIgnoreRelaunch() {
        return this.mIgnoreRelaunch;
    }

    public void setIgnoreRelaunch(boolean ignoreRelaunch) {
        this.mIgnoreRelaunch = ignoreRelaunch;
    }

    public void updateRequestOverrideConfigUseBounds(Rect bounds) {
        Configuration overrideConfig = this.mTempConfig;
        overrideConfig.unset();
        overrideConfig.windowConfiguration.setBounds(bounds);
        this.mActivityRecord.onRequestedOverrideConfigurationChanged(overrideConfig);
    }

    public boolean skipGetOrientationFromLauncherInFreeform() {
        boolean isRecent = this.mActivityRecord.mDisplayContent.getUpdateOrientationToken() != null && this.mActivityRecord.mDisplayContent.getUpdateOrientationToken().toString().contains("com.vivo.upslide/.recents.RecentsActivity");
        boolean isFreeformApp = false;
        if (this.mActivityRecord.mDisplayContent.getUpdateOrientationToken() != null && this.mActivityRecord.mDisplayContent.getUpdateOrientationToken().getStack() != null) {
            isFreeformApp = this.mActivityRecord.mDisplayContent.getUpdateOrientationToken().getStack().inFreeformWindowingMode();
        }
        if (this.mActivityRecord.mWmService.isVivoFreeFormValid() && this.mActivityRecord.mWmService.isInVivoFreeform() && !this.mActivityRecord.mWmService.isVivoFreeFormStackMax() && toString().contains("com.bbk.launcher2/.Launcher") && this.mActivityRecord.mDisplayContent.getLastOrientation() != this.mActivityRecord.mOrientation && (isRecent || isFreeformApp)) {
            VSlog.d(TAG, "Skip getOrientaton from launcher for vivo freeform mode mOrientation:" + this.mActivityRecord.mOrientation);
            return true;
        }
        return false;
    }

    public boolean ignoreGetOrientationFromFreeformApp() {
        if (this.mActivityRecord.mWmService.isVivoFreeFormValid() && this.mActivityRecord.inFreeformWindowingMode() && this.mActivityRecord.mWmService.isEnteringFreeForm() && !this.mActivityRecord.isVisible() && !this.mActivityRecord.getDisplayContent().mOpeningApps.contains(this.mActivityRecord)) {
            return true;
        }
        return false;
    }

    public boolean skipUsingSnapShotStringWindow(ActivityManager.TaskSnapshot snapshot, boolean allowTaskSnapshot) {
        if (this.mActivityRecord.mWmService.isVivoFreeformFeatureSupport() && snapshot != null && allowTaskSnapshot && snapshot.getWindowingMode() == 5 && snapshot.getWindowingMode() != this.mActivityRecord.getWindowingMode()) {
            return true;
        }
        return false;
    }

    public boolean notFinishNoHistoryForFreeform() {
        ActivityRecord topRunning = this.mActivityRecord.mAtmService.mRootWindowContainer.topRunningActivity();
        boolean topIsRecent = this.mActivityRecord.getState() == ActivityStack.ActivityState.PAUSED && topRunning != null && topRunning.isActivityTypeRecents();
        if (!this.mActivityRecord.mAtmService.isVivoFreeFormValid() || !this.mActivityRecord.mAtmService.isInVivoFreeform()) {
            return false;
        }
        if ((!this.mActivityRecord.mAtmService.isFreeFormMin() && !topIsRecent) || !this.mActivityRecord.inFreeformWindowingMode()) {
            return false;
        }
        return true;
    }

    public boolean isFinishFreeformNotes() {
        if (this.mActivityRecord.mAtmService.isVivoFreeFormValid() && this.mActivityRecord.mAtmService.isInVivoFreeform() && this.mActivityRecord.inFreeformWindowingMode()) {
            if (!isVivoEditWidgetActivity() && !isVivoBillEditActivity()) {
                return false;
            }
            return true;
        }
        return false;
    }

    public void scheduleIdleWhenFreeformActivityRemovedIfNeed() {
        boolean lastActivity = true;
        boolean inFreeform = this.mActivityRecord.mAtmService.isVivoFreeFormValid() && this.mActivityRecord.mAtmService.isInVivoFreeform();
        if (inFreeform && this.mActivityRecord.inFreeformWindowingMode()) {
            TaskDisplayArea taskDisplayArea = this.mActivityRecord.getDisplayArea();
            ActivityRecord topRun = taskDisplayArea != null ? taskDisplayArea.topRunningActivity() : null;
            boolean noFocusedStack = (taskDisplayArea == null || this.mActivityRecord.getStack() == null || this.mActivityRecord.getStack() == taskDisplayArea.getFocusedStack()) ? false : true;
            if (this.mActivityRecord.getTask() == null || this.mActivityRecord.getTask().getChildCount() != 0) {
                lastActivity = false;
            }
            if (noFocusedStack && !lastActivity && this.mActivityRecord.getStack().topRunningActivityLocked() == null && topRun != null && topRun.isState(ActivityStack.ActivityState.RESUMED)) {
                this.mActivityRecord.mAtmService.mStackSupervisor.scheduleIdle();
            }
        }
    }

    public boolean checkWhenEnterFreeFormRelaunchIfNeed() {
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null && activityRecord.mWmService.isVivoFreeFormValid() && this.mActivityRecord.mWmService.isInVivoFreeform() && this.mActivityRecord.mWmService.isEnteringFreeForm() && this.mActivityRecord.inFreeformWindowingMode() && Constant.APP_WEIXIN.equals(this.mActivityRecord.packageName)) {
            VSlog.d(TAG, "We need relaunch the app to start in freeform mode");
            return true;
        }
        return false;
    }

    public boolean canUpdateForVivoFreeformInSameProcess(ActivityRecord target) {
        if (this.mAtmService.isVivoFreeFormValid() && this.mAtmService.isInVivoFreeform() && this.mActivityRecord.inFreeformWindowingMode() && target != null && !target.inFreeformWindowingMode() && target.packageName != null && target.packageName.equals(this.mActivityRecord.packageName)) {
            return true;
        }
        return false;
    }

    private boolean needUpdateLetterbox() {
        return !this.mActivityRecord.inFreeformWindowingMode();
    }

    public void vivoUpdateLetterboxSurface(WindowState winHint) {
        if (needUpdateLetterbox()) {
            vivoLayoutLetterbox(winHint);
        } else if (this.mActivityRecord.mLetterbox != null) {
            this.mTopWinForLetterbox = null;
            this.mActivityRecord.mLetterbox.hide();
        }
        if (this.mActivityRecord.mLetterbox != null) {
            if (this.mActivityRecord.mLetterbox.needsApplySurfaceChanges() || this.mTopWinForLetterbox != this.mLastWinForLetterbox) {
                this.mLastWinForLetterbox = this.mTopWinForLetterbox;
                updateLetterboxColor();
                this.mActivityRecord.mLetterbox.applySurfaceChanges(this.mActivityRecord.getPendingTransaction(), this.mShouldRelativeToWindow ? this.mTopWinForLetterbox : null);
            }
        }
    }

    public void vivoLayoutLetterbox(WindowState winHint) {
        Rect spaceToFill;
        if (this.mActivityRecord.getParent() == null) {
            return;
        }
        WindowState mainWin = this.mActivityRecord.findMainWindow();
        boolean isFromTransferStarting = isFromTransferStarting();
        if (mainWin == null && !isFromTransferStarting) {
            if (this.mActivityRecord.mLetterbox != null) {
                this.mTopWinForLetterbox = null;
                this.mActivityRecord.mLetterbox.hide();
                return;
            }
            return;
        }
        Rect transformedBounds = this.mActivityRecord.getFixedRotationTransformDisplayBounds();
        if (transformedBounds != null) {
            spaceToFill = transformedBounds;
        } else {
            spaceToFill = this.mActivityRecord.getRootTask().getParent().getBounds();
        }
        this.mFrameForLetterbox.set(spaceToFill);
        this.mNeedLetterbox = false;
        this.mTopWinForLetterbox = null;
        if (mIsSpecialPlatform) {
            this.mShouldRelativeToWindow = false;
        }
        if (winHint != null && !this.mContributesWins.contains(winHint)) {
            return;
        }
        this.mContributesWins.clear();
        if (isFromTransferStarting) {
            computeLetterboxForTransferStarting();
        }
        computeLetterboxTraversal(this.mActivityRecord, mainWin);
        layoutLetterbox();
    }

    public void vivoLayoutSplitNav() {
        WindowState mainWin;
        if (!this.mActivityRecord.mWmService.isVivoMultiWindowSupport() || !VivoMultiWindowConfig.IS_VIVO_SPLIT_NAV_COLOR || !isNavBarShowDefaultDisplay() || (mainWin = this.mActivityRecord.findMainWindow()) == null) {
            return;
        }
        this.mNeedSplitNavBar = false;
        this.mNeedBlackSplitNavBarForSpecialApp = false;
        boolean isInMulti = this.mActivityRecord.inSplitScreenWindowingMode();
        if (isInMulti || (this.mActivityRecord.getDisplayContent().isVivoMultiWindowExitedJustWithDisplay() && this.mActivityRecord.getDisplayContent().isVivoOpSplitnavbarOfNoSplit())) {
            WindowState currentfocus = this.mActivityRecord.getDisplayContent().mCurrentFocus;
            if (currentfocus != null && currentfocus.getName().contains("com.vivo.wallet/com.vivo.pay.swing.activity.NewSwipeActivity")) {
                this.mNeedBlackSplitNavBarForSpecialApp = true;
            } else {
                computeSplitNav(mainWin, isInMulti);
            }
            layoutSplitNav(isInMulti);
        }
    }

    private void computeLetterboxTraversal(WindowContainer windowContainer, WindowState mainWin) {
        for (int i = windowContainer.mChildren.size() - 1; i >= 0; i--) {
            WindowState win = (WindowState) windowContainer.mChildren.get(i);
            if (DBEUG_LETTERBOX) {
                VSlog.d(TAG, this.mActivityRecord + " computeLetterbox win = " + win);
            }
            computeLetterboxTraversal(win, mainWin);
            computeLetterbox(win, win == mainWin);
        }
    }

    private void computeLetterbox(WindowState w, boolean isMainWin) {
        if (w == null) {
            return;
        }
        boolean needsLetterbox = false;
        boolean surfaceReady = (w.isDrawnLw() || w.mWinAnimator.mSurfaceDestroyDeferred || w.isDragResizeChanged()) && w.isVisibleByPolicy();
        if (DBEUG_LETTERBOX) {
            VSlog.d(TAG, this.mActivityRecord + " computeLetterbox win = " + w + " surfaceReady = " + surfaceReady);
        }
        boolean origNeedsLetterbox = false;
        boolean inMulti = w.inSplitScreenWindowingMode();
        if (isMainWin) {
            boolean origNeedsLetterbox2 = surfaceReady && w.isLetterboxedAppWindow() && this.mActivityRecord.fillsParent();
            boolean shouldShow = w.shouldShowLetterbox(true);
            boolean vivoReadyForLetterbox = inMulti ? w.isVisible() : surfaceReady;
            boolean needsLetterbox2 = origNeedsLetterbox2 || (vivoReadyForLetterbox && shouldShow);
            adjustLetterboxInSplitIfNeeded(w, inMulti, isMainWin, needsLetterbox2, origNeedsLetterbox2, vivoReadyForLetterbox);
            needsLetterbox = needsLetterbox2;
            origNeedsLetterbox = origNeedsLetterbox2;
        } else {
            boolean shouldShow2 = w.shouldShowLetterbox(false);
            if (surfaceReady && shouldShow2) {
                needsLetterbox = true;
            }
        }
        if (mIsSpecialPlatform && !this.mShouldRelativeToWindow && surfaceReady && (w.getAttrs().flags & 2) != 0) {
            this.mShouldRelativeToWindow = true;
        }
        if (needsLetterbox) {
            this.mNeedLetterbox = true;
            this.mContributesWins.add(w);
            if (this.mTopWinForLetterbox == null) {
                this.mTopWinForLetterbox = w;
            }
            Rect tmpRect = new Rect();
            if (isMainWin) {
                tmpRect.set(w.getFrameForLetterbox());
                if (origNeedsLetterbox) {
                    tmpRect.intersectUnchecked(w.getFrameLw());
                }
                if (DBEUG_LETTERBOX) {
                    VSlog.d(TAG, this.mActivityRecord + " computeLetterbox origin, win = " + w + " frame = " + tmpRect);
                }
            } else {
                tmpRect.set(w.getFrameForLetterbox());
                if (DBEUG_LETTERBOX) {
                    VSlog.d(TAG, this.mActivityRecord + " computeLetterbox, win = " + w + " frame = " + tmpRect);
                }
            }
            this.mFrameForLetterbox.intersect(tmpRect);
        }
    }

    private void computeSplitNav(WindowState w, boolean inMulti) {
        if (w == null) {
            return;
        }
        boolean shouldShowSplitNavBar = inMulti ? w.shouldShowSplitNavBar(true) : false;
        if (shouldShowSplitNavBar && ((w.isVisible() || (this.mActivityRecord.getDisplayContent() != null && this.mActivityRecord.getDisplayContent().isWallpaperVisibilityChangedOfVisible())) && w.getWindowingMode() == 4)) {
            this.mNeedSplitNavBar = true;
        }
        if (inMulti && !this.mNeedSplitNavBar && this.mActivityRecord.mWmService.getVivoFloatMessageFlag() && this.mActivityRecord.mWmService.getVivoPendingPackageInSplit() != null && this.mActivityRecord.mWmService.getVivoPendingPackageInSplit().equals(w.mAttrs.packageName)) {
            ActivityStack stack = this.mActivityRecord.getDisplayContent().getDefaultTaskDisplayArea().getRootSplitScreenPrimaryTask();
            Task pt = stack != null ? stack.getTopMostTask() : null;
            WindowState pw = pt != null ? pt.getTopVisibleAppMainWindow() : null;
            if (pt != null && pw != null && pw.mAttrs != null && ("tv.danmaku.bili".equals(pw.mAttrs.packageName) || "cn.xiaochuankeji.tieba".equals(pw.mAttrs.packageName))) {
                this.mNeedBlackSplitNavBarForSpecialApp = true;
            }
        }
        if (DEBUG_SPLIT_NAV_COLOR) {
            VSlog.i(TAG, "mNeedSplitNavBar is " + this.mNeedSplitNavBar + " win is " + w + " shouldShow is " + shouldShowSplitNavBar + ",isVisible =" + w.isVisible() + ",isDealyHideSplitNav is " + this.isDealyHideSplitNav);
        }
    }

    public void adjustLetterboxInSplitIfNeeded(WindowState w, boolean inMulti, boolean isMainWin, boolean needsLetterbox, boolean origNeedsLetterbox, boolean vivoReadyForLetterbox) {
    }

    public void correctLetterboxOfPart() {
        this.isIgnoreLeftRectOfLetterboxInSplit = false;
    }

    public boolean isInVivoMultiWindowOrMomentInFocusedDisplay() {
        return false;
    }

    private boolean isFromTransferStarting() {
        WindowState lastTopWin = this.mLastWinForLetterbox;
        if (this.mHasPreservedForStartingWin || lastTopWin == null || lastTopWin.mAttrs.type != 3 || lastTopWin.mActivityRecord == this.mActivityRecord) {
            return false;
        }
        this.mHasPreservedForStartingWin = true;
        return true;
    }

    private void computeLetterboxForTransferStarting() {
        WindowState lastTopWin = this.mLastWinForLetterbox;
        VSlog.d(TAG, this.mActivityRecord + " computeLetterboxForTransferStarting win = " + lastTopWin);
        computeLetterbox(lastTopWin, true);
    }

    private boolean isLeaveHomeAnimating() {
        if (this.mActivityRecord.inSplitScreenWindowingMode()) {
            DisplayContent content = this.mActivityRecord.getDisplayContent();
            WindowState latestFoucus = content != null ? content.getLatestFoucusOfHome() : null;
            return (latestFoucus == null || content.mCurrentFocus == latestFoucus || (content.mCurrentFocus != null && (content.mCurrentFocus == null || content.mCurrentFocus.isActivityTypeHome())) || !latestFoucus.isAnimatingLw() || latestFoucus.isAnimating() || !latestFoucus.isVisibleNow()) ? false : true;
        }
        return false;
    }

    private void layoutSplitNav(boolean isMulti) {
        WindowState w = this.mActivityRecord.findMainWindow();
        if (isMulti) {
            if (isMatchDockNavPos(0)) {
                boolean winvis = w != null && w.isVisibleNow();
                if (winvis || this.mActivityRecord.getDisplayContent().isWallpaperVisibilityChangedOfVisible()) {
                    if (this.mNeedSplitNavBar || this.isDealyHideSplitNav) {
                        if (isLeaveHomeAnimating() && w != null && !w.isActivityTypeHome()) {
                            if (DEBUG_SPLIT_NAV_COLOR) {
                                VSlog.d(TAG, "ignore showSplitScreenNavBar of w:" + w);
                            }
                        } else {
                            this.mActivityRecord.getDisplayContent().showSplitScreenNavBar();
                            updateLetterboxColor();
                            if (!winvis) {
                                this.mActivityRecord.getDisplayContent().resetWallpaperVisibilityChanged();
                                if (DEBUG_SPLIT_NAV_COLOR) {
                                    VSlog.d(TAG, "resetWallpaperVisibilityChanged of this");
                                }
                            }
                        }
                    } else if (!this.mNeedBlackSplitNavBarForSpecialApp && !this.mActivityRecord.mWmService.isVivoDockedDividerResizing()) {
                        this.mActivityRecord.getDisplayContent().hideSplitScreenNavBar();
                    }
                }
                if (this.mNeedBlackSplitNavBarForSpecialApp) {
                    this.mActivityRecord.getDisplayContent().showSplitScreenNavBar();
                    this.mActivityRecord.getDisplayContent().setSplitScreenNavBarColor(-16777216);
                }
            }
            if (DEBUG_SPLIT_NAV_COLOR) {
                StringBuilder sb = new StringBuilder();
                sb.append("layoutLetterbox, split-screen-navcolor update macnavpos = ");
                sb.append(isMatchDockNavPos(0));
                sb.append(" mNeedSplitNavBar is ");
                sb.append(this.mNeedSplitNavBar);
                sb.append(" mNeedBlackSplitNavBarForSpecialApp is ");
                sb.append(this.mNeedBlackSplitNavBarForSpecialApp);
                sb.append(" isVisibleNow:");
                sb.append(w != null ? Boolean.valueOf(w.isVisibleNow()) : " w is null");
                sb.append(" packageName:");
                sb.append(w != null ? w.mAttrs.packageName : "null");
                VSlog.d(TAG, sb.toString());
                return;
            }
            return;
        }
        WindowState curretF = this.mActivityRecord.getDisplayContent().mCurrentFocus;
        this.mActivityRecord.getDisplayContent().hideSplitScreenNavBar();
        this.mAtmService.mWindowManager.mH.removeMessages(70);
        if (DEBUG_SPLIT_NAV_COLOR) {
            VSlog.d(TAG, " start hideSplitScreenNavBar of focus " + curretF);
        }
    }

    public void recordRelaunchState() {
        this.relaunchOfTime = SystemClock.uptimeMillis();
    }

    public long getRelaunchStartTime() {
        return this.relaunchOfTime;
    }

    private boolean isRelaunchingOfWindow(WindowState w) {
        int duration;
        return w != null && w.mActivityRecord != null && w.isVisibleLw() && (duration = (int) (SystemClock.uptimeMillis() - w.mActivityRecord.getRelaunchStartTime())) >= 0 && duration <= 2000;
    }

    private void layoutLetterbox() {
        Rect spaceToFill;
        if (this.mNeedLetterbox) {
            if (this.mActivityRecord.mLetterbox == null) {
                this.mActivityRecord.mLetterbox = new Letterbox(new Supplier() { // from class: com.android.server.wm.-$$Lambda$VivoActivityRecordImpl$zpVk6vOVZFhEPWE0UJrEimh-pmE
                    @Override // java.util.function.Supplier
                    public final Object get() {
                        return VivoActivityRecordImpl.this.lambda$layoutLetterbox$0$VivoActivityRecordImpl();
                    }
                }, this.mActivityRecord.mWmService.mTransactionFactory);
                this.mActivityRecord.mLetterbox.attachInput(this.mTopWinForLetterbox);
                onLetterboxCreated();
            }
            this.mActivityRecord.getPosition(this.mTmpPoint);
            Rect transformedBounds = this.mActivityRecord.getFixedRotationTransformDisplayBounds();
            if (transformedBounds != null) {
                spaceToFill = transformedBounds;
            } else if (this.mActivityRecord.inMultiWindowMode()) {
                spaceToFill = this.mActivityRecord.getTask().getBounds();
            } else {
                spaceToFill = this.mActivityRecord.getRootTask().getParent().getBounds();
            }
            this.mActivityRecord.mLetterbox.layout(spaceToFill, this.mFrameForLetterbox, this.mTmpPoint);
            correctLetterboxOfPart();
            if (DBEUG_LETTERBOX) {
                VSlog.d(TAG, this.mActivityRecord + " layoutLetterbox, mFrameForLetterbox = " + this.mFrameForLetterbox);
            }
        } else if (this.mActivityRecord.mLetterbox != null) {
            this.mActivityRecord.mLetterbox.hide();
            if (DBEUG_LETTERBOX) {
                VSlog.d(TAG, this.mActivityRecord + " layoutLetterbox, hide letterbox");
            }
        }
    }

    public /* synthetic */ SurfaceControl.Builder lambda$layoutLetterbox$0$VivoActivityRecordImpl() {
        return this.mActivityRecord.makeChildSurface((WindowContainer) null);
    }

    void onLetterboxCreated() {
        updateLetterboxColor();
    }

    public void updateLetterboxColor() {
        boolean inMultiMode = this.mActivityRecord.inMultiWindowMode() && VivoMultiWindowConfig.IS_VIVO_SPLIT_NAV_COLOR;
        if (this.mActivityRecord.mLetterbox == null && !inMultiMode) {
            return;
        }
        this.navColors.clear();
        this.mNoWindowApply = true;
        collectNavColorTraversal(this.mActivityRecord, inMultiMode);
        int navColor = computeNavColor();
        if (inMultiMode && this.mNoWindowApply) {
            navColor = this.mLastNavColor;
        } else {
            this.mLastNavColor = navColor;
        }
        if (inMultiMode) {
            WindowState win = this.mActivityRecord.findMainWindow();
            if (win != null && isNavBarShowDefaultDisplay() && isMatchDockNavPos(0) && win.isVisibleNow()) {
                boolean topSameId = false;
                if (!this.mActivityRecord.mWmService.isVivoDockedDividerResizing()) {
                    int windowingMode = this.mActivityRecord.getStack().getWindowingMode();
                    ActivityStack rootSecondaryStack = this.mActivityRecord.getDisplayContent().getDefaultTaskDisplayArea().getRootSplitScreenSecondaryTask();
                    Task task = null;
                    Task topTask = rootSecondaryStack != null ? rootSecondaryStack.getTopMostTask() : null;
                    if (windowingMode == 4 || windowingMode == 3) {
                        task = topTask;
                    }
                    if (task == null || this.mActivityRecord.getTask().mTaskId == task.mTaskId) {
                        if (DEBUG_SPLIT_NAV_COLOR) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("|-- split-screen-navcolor with top task is ");
                            sb.append(task != null ? task : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                            VSlog.d(TAG, sb.toString());
                        }
                        topSameId = true;
                    }
                } else {
                    topSameId = true;
                }
                if (topSameId) {
                    this.mActivityRecord.getDisplayContent().setSplitScreenNavBarColor(navColor);
                    if (DEBUG_SPLIT_NAV_COLOR) {
                        VSlog.d(TAG, "|-- split-screen-navcolor " + win + " startingDisplayed:" + this.mActivityRecord.startingDisplayed + " splitnavbar show = " + this.mActivityRecord.getDisplayContent().isSplitScreenNavBarShow() + " split update color : " + Integer.toHexString(navColor) + " " + Debug.getCallers(3));
                        return;
                    }
                    return;
                }
                return;
            } else if (win == null && DEBUG_SPLIT_NAV_COLOR) {
                VSlog.d(TAG, "|-- split-screen-navcolor w is null in split nav color for split update color : " + Integer.toHexString(navColor) + " " + Debug.getCallers(3));
                return;
            } else {
                return;
            }
        }
        if (DBEUG_LETTERBOX) {
            VSlog.d(TAG, this.mActivityRecord + " normal update color : " + Integer.toHexString(navColor) + " " + Debug.getCallers(2));
        }
        int rotation = this.mActivityRecord.mWmService.getDefaultDisplayContentLocked().getRotation();
        if (rotation == 0) {
            this.mActivityRecord.mLetterbox.setColors(this.mActivityRecord.getPendingTransaction(), -16777216, -16777216, -16777216, navColor);
        } else if (rotation == 1) {
            this.mActivityRecord.mLetterbox.setColors(this.mActivityRecord.getPendingTransaction(), -16777216, -16777216, navColor, -16777216);
        } else if (rotation == 3) {
            this.mActivityRecord.mLetterbox.setColors(this.mActivityRecord.getPendingTransaction(), navColor, -16777216, -16777216, -16777216);
        }
    }

    void collectNavColorTraversal(WindowContainer windowContainer, boolean inMultiMode) {
        for (int i = windowContainer.mChildren.size() - 1; i >= 0; i--) {
            WindowState win = (WindowState) windowContainer.mChildren.get(i);
            collectNavColorTraversal(win, inMultiMode);
            int shouldApply = win.shouldApplyNavColor();
            if (shouldApply == NavigationBarPolicy.APPLY_COLOR_NONE) {
                if (DBEUG_LETTERBOX) {
                    VSlog.d(TAG, "apply navColor, skip win = " + win);
                }
            } else {
                if (shouldApply == NavigationBarPolicy.APPLY_COLOR_FULL) {
                    int navcolor = win.getWindowNavColor();
                    if (DBEUG_LETTERBOX) {
                        VSlog.d(TAG, "apply navColor win = " + win + " color = " + Integer.toHexString(navcolor));
                    }
                    this.navColors.add(Integer.valueOf(navcolor));
                    this.mNoWindowApply = false;
                }
                if (inMultiMode) {
                    WindowManager.LayoutParams attrs = win.getAttrs();
                    if ((attrs.flags & 2) != 0) {
                        this.navColors.add(Integer.valueOf(Color.argb((int) (attrs.dimAmount * 255.0f), 0, 0, 0)));
                        this.mNoWindowApply = false;
                    }
                }
            }
        }
    }

    private int computeNavColor() {
        int color = -16777216;
        int count = this.navColors.size();
        for (int i = count - 1; i >= 0; i--) {
            int maskColor = this.navColors.get(i).intValue();
            color = DrawableUtils.blendColor(color, maskColor);
        }
        if (DBEUG_LETTERBOX) {
            VSlog.d(TAG, " computeNavColor final get color = " + Integer.toHexString(color));
        }
        return color;
    }

    boolean isNavBarShowDefaultDisplay() {
        DisplayContent displayContent = this.mActivityRecord.getDisplayContent();
        return displayContent != null && displayContent.isDefaultDisplay && displayContent.getDisplayPolicy() != null && displayContent.getDisplayPolicy().hasNavigationBar() && displayContent.getDisplayPolicy().getVivoHasNavBar() && isNavigationBarGestureOff();
    }

    boolean isMatchDockNavPos(int displayId) {
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord == null || activityRecord.getStack() == null || this.mActivityRecord.getDisplayId() != displayId) {
            return false;
        }
        int dockSide = this.mActivityRecord.getStack().getDockSide();
        DisplayInfo displayInfo = this.mActivityRecord.getDisplayContent().getDisplayInfo();
        return VivoMultiWindowConfig.isMatchDockNavPos(dockSide, displayInfo, this.mActivityRecord.getDisplayContent().getDisplayPolicy().getNavBarPosition(), this.mActivityRecord.getStack().getActivityType(), this.mActivityRecord.getStack().getWindowingMode());
    }

    public boolean isNavigationBarGestureOff() {
        try {
            boolean isNavGestureOff = Settings.Secure.getInt(this.mActivityRecord.mWmService.mContext.getContentResolver(), VivoRatioControllerUtilsImpl.NAVIGATION_GESTURE_ON) == 0;
            return isNavGestureOff;
        } catch (Exception e) {
            VSlog.w(TAG, "Get navigation bar settings error : SettingNotFoundException");
            return true;
        }
    }

    public int shouldRelaunch(int globalConfig, Configuration tmpConfig) {
        if ((globalConfig & 1073741824) != 0) {
            Configuration currentConfig = this.mActivityRecord.getConfiguration();
            int diff = tmpConfig.diff(currentConfig);
            if ((1073741824 & diff) != 0 && Math.abs(tmpConfig.fontScale - currentConfig.fontScale) > 0.0f && Math.abs(tmpConfig.fontScale - currentConfig.fontScale) <= 0.01f) {
                return globalConfig & (-1073741825);
            }
            return globalConfig;
        }
        return globalConfig;
    }

    public void setVivoOccludeKeyguard(boolean occlude) {
        this.mVivoOccludeKeyguard = occlude;
    }

    public boolean isVivoOccludeKeyguard() {
        return this.mVivoOccludeKeyguard;
    }

    public boolean canShowWhenLockedWithoutFlag() {
        return this.mVivoOccludeKeyguard && this.mAtmService.isKeyguardLocked() && this.mAtmService.vivoCanOccludeKeyguard(this.mActivityRecord.packageName, this.mVivoOccludeKeyguard);
    }

    public void updateVivoOccludeStatus(String packageName, boolean lastOcclude) {
        if (this.mAtmService.isKeyguardLocked() && this.mAtmService.vivoCanOccludeKeyguard(this.mActivityRecord.packageName, this.mVivoOccludeKeyguard)) {
            setVivoOccludeKeyguard(true);
        }
    }

    public ComponentName getActivityComponent() {
        return this.mActivityRecord.mActivityComponent;
    }

    public IBinder getActivityToken() {
        return this.mActivityRecord.appToken.asBinder();
    }

    public WindowProcessController getApp() {
        return this.mActivityRecord.app;
    }

    public int getUidForPublic() {
        return this.mActivityRecord.getUid();
    }

    public void loadInfoFromTheme(AttributeCache.Entry ent) {
        if (ent != null) {
            this.mIsTranslucent = ent.array.getBoolean(5, false);
        }
    }

    public boolean isWindowOpaque() {
        return (this.mIsTranslucent || this.mActivityRecord.hasWallpaper) ? false : true;
    }

    public int getPidForPublic() {
        return this.mActivityRecord.getPid();
    }

    public boolean isIncallUIFullscreen(String className, AttributeCache.Entry ent) {
        if ("com.android.incallui.InCallActivity".equals(className) || "com.android.incallui.LandCallActivity".equals(className)) {
            boolean mOccludesParent = !ent.array.getBoolean(4, false);
            return mOccludesParent;
        }
        boolean mOccludesParent2 = !ActivityInfo.isTranslucentOrFloating(ent.array);
        return mOccludesParent2;
    }

    public boolean shouldUpdateSizeCompatMode(DisplayContent displayContent, Configuration newParentConfig) {
        DisplayPolicy displayPolicy;
        Configuration activityConfiguration;
        if (AmsConfigManager.getInstance().isSizeCompatModeOptEnabled() && (displayPolicy = displayContent.getDisplayPolicy()) != null) {
            DisplayMetrics displayMetrics = displayContent.getDisplayMetrics();
            boolean navBarChange = displayPolicy.getVivoHasNavBar() != this.mHasNavBar;
            this.mHasNavBar = displayPolicy.getVivoHasNavBar();
            if (!IS_VOS && displayMetrics != null && this.mActivityRecord.info != null) {
                if (this.mActivityRecord.mCompatDisplayInsets != null && displayContent.mDisplayId == 0 && (activityConfiguration = this.mActivityRecord.getConfiguration()) != null && newParentConfig != null && activityConfiguration.orientation == newParentConfig.orientation) {
                    float navHeightDp = displayPolicy.getNavigationBarHeight(displayContent.getRotation(), this.mActivityRecord.mWmService.mPolicy.getUiMode()) / displayMetrics.density;
                    int lastScreenHeight = activityConfiguration.screenHeightDp;
                    int lastScreenWidth = activityConfiguration.screenWidthDp;
                    int screenHeight = newParentConfig.screenHeightDp;
                    int screenWidth = newParentConfig.screenWidthDp;
                    boolean updateSizeCompatMode = false;
                    if (activityConfiguration.orientation == 1) {
                        if (screenWidth == lastScreenWidth && Math.abs(screenHeight - lastScreenHeight) <= navHeightDp) {
                            updateSizeCompatMode = true;
                        }
                    } else if (activityConfiguration.orientation == 2 && screenHeight == lastScreenHeight && Math.abs(screenWidth - lastScreenWidth) <= navHeightDp) {
                        updateSizeCompatMode = true;
                    }
                    if (updateSizeCompatMode) {
                        float aspectRatio = (Math.max(screenWidth, screenHeight) + 0.5f) / Math.min(screenWidth, screenHeight);
                        if (this.mActivityRecord.info.maxAspectRatio > 0.0f && aspectRatio >= this.mActivityRecord.info.maxAspectRatio) {
                            updateSizeCompatMode = false;
                        }
                        if (this.mActivityRecord.info.minAspectRatio > 0.0f && aspectRatio <= this.mActivityRecord.info.minAspectRatio) {
                            updateSizeCompatMode = false;
                        }
                    }
                    if (navBarChange && updateSizeCompatMode) {
                        VSlog.d(TAG, "shouldUpdateSizeCompatMode activityConfiguration : " + activityConfiguration + " newParentConfig:" + newParentConfig + " mVisibleRequested: " + this.mActivityRecord.mVisibleRequested + " " + this.mActivityRecord);
                        return true;
                    }
                    return false;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:94:0x024b  */
    /* JADX WARN: Removed duplicated region for block: B:98:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void applyAspectRatioForVivo(android.graphics.Rect r24, android.graphics.Rect r25, android.graphics.Rect r26, android.content.pm.ActivityInfo r27) {
        /*
            Method dump skipped, instructions count: 627
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoActivityRecordImpl.applyAspectRatioForVivo(android.graphics.Rect, android.graphics.Rect, android.graphics.Rect, android.content.pm.ActivityInfo):void");
    }

    public void handleUnknowAppHideOrRemove() {
        this.mAtmService.mH.removeCallbacks(this.mUnknowAppHideOrRemoveRunnable);
        this.mAtmService.mH.postDelayed(this.mUnknowAppHideOrRemoveRunnable, 1000L);
    }

    public void updateActivityRegistry() {
        AnimatingActivityRegistry animatingActivityRegistry;
        VSlog.d(TAG, "updateActivityRegistry");
        ActivityStack stack = this.mActivityRecord.getStack();
        if (this.mActivityRecord.isAnimating()) {
            this.mActivityRecord.cancelAnimation();
        }
        if (this.mActivityRecord.mAnimatingActivityRegistry != null) {
            this.mActivityRecord.mAnimatingActivityRegistry.notifyFinished(this.mActivityRecord);
        }
        ActivityRecord activityRecord = this.mActivityRecord;
        if (stack != null) {
            animatingActivityRegistry = stack.getAnimatingActivityRegistry();
        } else {
            animatingActivityRegistry = null;
        }
        activityRecord.mAnimatingActivityRegistry = animatingActivityRegistry;
    }

    public boolean isStartWindowEnable(String pkgName) {
        if (this.ENABLE_STARTWINDOW_WHITELIST.contains(pkgName)) {
            VSlog.i(TAG, "Enable starting window for:" + pkgName);
            return true;
        }
        return false;
    }

    public boolean shouldUseSnapshot(ActivityManager.TaskSnapshot snapshot) {
        boolean globalConfigurationChanged = false;
        if (snapshot != null) {
            Configuration currentConfiguration = this.mActivityRecord.getTask().getConfiguration();
            Configuration snapshotConfiguration = snapshot.getConfiguration();
            globalConfigurationChanged = (currentConfiguration.uiMode == snapshotConfiguration.uiMode && currentConfiguration.getThemeId() == snapshotConfiguration.getThemeId() && (currentConfiguration.getLocales().size() <= 0 || snapshotConfiguration.getLocales().size() <= 0 || currentConfiguration.getLocales().get(0) == null || snapshotConfiguration.getLocales().get(0) == null || currentConfiguration.getLocales().get(0).equals(snapshotConfiguration.getLocales().get(0)))) ? false : true;
        }
        boolean isGlobalSearch = "com.vivo.globalsearch".equals(this.mActivityRecord.packageName);
        if (WindowManagerDebugConfig.DEBUG_SNAPSHOT_STARTING_WINDOW && !globalConfigurationChanged && !isGlobalSearch) {
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                VSlog.d(TAG, "useSnapshotStartingWindow, appWin = " + this.mActivityRecord);
            }
            return true;
        }
        if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
            VSlog.d(TAG, "Not useSnapshotStartingWindow, appWin = " + this.mActivityRecord + ", due to , globalConfigurationChanged=" + globalConfigurationChanged);
        }
        return false;
    }

    public int getPreloadFlags() {
        return this.vPreloadFlags;
    }

    public void notifyConfigurationChanged(DisplayContent display) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && this.vPreloadFlags != 0 && display.mDisplayId == 0) {
            if (MultiDisplayManager.DEBUG) {
                VSlog.d("VivoStack", "reset vflags:" + Integer.toHexString(this.vPreloadFlags) + " ,r : " + this.mActivityRecord);
            }
            if ((this.vPreloadFlags & 1) != 0) {
                this.mAtmService.notifyActivityColdStarting(this.mActivityRecord);
            }
            this.vPreloadFlags = 0;
        }
    }

    public boolean shouldProcessColdStarting() {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && MultiDisplayManager.isVivoDisplay(this.mActivityRecord.getDisplayId())) {
            this.vPreloadFlags |= 1;
            return true;
        }
        return false;
    }

    public void markIdForVirtualDisplay(int lastDisplayId) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && MultiDisplayManager.isVivoDisplay(this.mActivityRecord.getDisplayId()) && this.mLastDisplayIdForVirtual == Integer.MIN_VALUE && lastDisplayId == 0) {
            this.mLastDisplayIdForVirtual = this.mActivityRecord.getDisplayId();
        }
    }

    public int restoreIdForVirtualDisplay(int lastDisplayId) {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return lastDisplayId;
        }
        int tempDisplayId = this.mLastDisplayIdForVirtual;
        this.mLastDisplayIdForVirtual = Integer.MAX_VALUE;
        if (MultiDisplayManager.isVivoDisplay(tempDisplayId)) {
            if (MultiDisplayManager.DEBUG) {
                VSlog.d("VivoStack", "restored from: " + tempDisplayId + " => " + this.mActivityRecord.getDisplayId() + " ,this: " + this.mActivityRecord);
            }
            return tempDisplayId;
        }
        return lastDisplayId;
    }

    public boolean shouldskipRelaunchForVCar(int changes) {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return false;
        }
        if (this.isVirtualDisplayChange && MultiDisplayManager.DEBUG) {
            VSlog.d("VivoStack", "skip relaunch: " + this.mActivityRecord);
        }
        return this.isVirtualDisplayChange;
    }

    public void checkIfVCarDisplayChange(int newDisplayId, int lastDisplayId, boolean displayChanged) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            if (lastDisplayId == 80000 || newDisplayId == 80000) {
                this.isVirtualDisplayChange = false;
            } else if (displayChanged && (MultiDisplayManager.isVivoDisplay(newDisplayId) || MultiDisplayManager.isVivoDisplay(lastDisplayId))) {
                if (MultiDisplayManager.DEBUG) {
                    VSlog.d("VivoStack", "mark vd change!");
                }
                this.isVirtualDisplayChange = true;
            } else {
                this.isVirtualDisplayChange = false;
            }
        }
    }

    public void snapshotForCast() {
        if (this.mSnapshotDone || this.mActivityRecord.getDisplayId() != 80000) {
            return;
        }
        this.mSnapshotDone = true;
        this.mAtmService.mH.postDelayed(new Runnable() { // from class: com.android.server.wm.VivoActivityRecordImpl.3
            @Override // java.lang.Runnable
            public void run() {
                synchronized (VivoActivityRecordImpl.this.mActivityRecord.mWmService.mGlobalLock) {
                    Task task = VivoActivityRecordImpl.this.mActivityRecord.getTask();
                    VSlog.d("VivoCar", "snapshotForCast " + task);
                    if (task != null) {
                        TaskSnapshotController snapshotController = VivoActivityRecordImpl.this.mActivityRecord.mWmService.mTaskSnapshotController;
                        ArraySet<Task> tasks = Sets.newArraySet(new Task[]{task});
                        snapshotController.snapshotTasks(tasks);
                    }
                }
            }
        }, 250L);
    }

    public void checkDisplayForCar(ActivityRecord r) {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING || r.app == null || !r.app.hasThread()) {
            return;
        }
        if (MultiDisplayManager.isResCarDisplay(r.getDisplayId())) {
            r.app.isRunningInCar = true;
            VSlog.d("VivoCar", r + " is running in car display " + r.getDisplayId());
            return;
        }
        r.app.isRunningInCar = false;
    }

    public void onDisplayChanged(int preDisplayId, int newDisplayId) {
        WindowProcessController wpc;
        if (preDisplayId == 95555 && newDisplayId != 95555 && (wpc = this.mActivityRecord.app) != null) {
            VSlog.d("VivoCar", this.mActivityRecord + " setRmsPreload  false");
            RmsInjectorImpl.getInstance().setRmsPreload(this.mActivityRecord.packageName, wpc.mUid, false, false);
            RmsInjectorImpl.getInstance().notifyRmsActivityOnVdStart(this.mActivityRecord.packageName, this.mActivityRecord.processName, this.mActivityRecord.shortComponentName, wpc.mPid, wpc.mUid, 0);
            if (this.mActivityRecord.isState(ActivityStack.ActivityState.RESUMED) || this.mActivityRecord.isState(ActivityStack.ActivityState.INITIALIZING) || this.mActivityRecord.isInterestingToUserLocked()) {
                if (MultiDisplayManager.DEBUG) {
                    Slog.d("VivoCar", "Change to top: " + wpc.mOwner);
                }
                RmsInjectorImpl.getInstance().applyOomAdjLocked((ProcessRecord) wpc.mOwner);
            }
        }
    }

    public boolean isBBKLauncher() {
        return this.mActivityComponent.getClassName() != null && Constant.ACTIVITY_LAUNCHER.equals(this.mActivityComponent.getClassName());
    }

    public void onCheckTopAppChanged() {
        SnapshotWindow snapshotWindow = this.mSnapshotWin;
        if (snapshotWindow != null) {
            snapshotWindow.onCheckTopAppChanged();
        }
    }

    public boolean skipSizeCompatModeForAppShareLocked() {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            return isAppShareContainerLocked() || MultiDisplayManager.isAppShareDisplayId(this.mActivityRecord.getDisplayId());
        }
        return false;
    }

    public boolean isAppShareContainerLocked() {
        return AppShareConfig.SUPPROT_APPSHARE && this.mActivityComponent != null && AppShareConfig.APP_SHARE_ACTIVITY_SHORT_STRING.equals(this.mActivityComponent.flattenToShortString());
    }

    public void checkIfAppShareDisplayChange(int newDisplayId, int lastReportedDisplayId, boolean displayChanged) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        this.mMoveBetweenDefaultAndAppShareDisplay = false;
        if (displayChanged) {
            if (lastReportedDisplayId == 0 && MultiDisplayManager.isAppShareDisplayId(newDisplayId)) {
                this.mMoveBetweenDefaultAndAppShareDisplay = true;
            } else if (MultiDisplayManager.isAppShareDisplayId(lastReportedDisplayId) && newDisplayId == 0) {
                this.mMoveBetweenDefaultAndAppShareDisplay = true;
            }
        }
    }

    public boolean skipRelaunchForAppShareLocked(int changes, int newDisplayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return false;
        }
        if (this.mMoveBetweenDefaultAndAppShareDisplay && ((changes & 16384) != 0 || (changes & Consts.ProcessStates.FOCUS) != 0 || (changes & 512) != 0)) {
            VSlog.i(TAG, "skip relaunch: " + this.mActivityRecord + " for APP_SHARE.");
            return true;
        } else if (!MultiDisplayManager.isAppShareDisplayId(newDisplayId) || (changes & 512) == 0) {
            return false;
        } else {
            VSlog.i(TAG, "skip relaunch: " + this.mActivityRecord + " for APP_SHARE.");
            return true;
        }
    }

    public boolean skipMakeActiveForAppShareLocked(DisplayContent display) {
        return display != null && MultiDisplayManager.isAppShareDisplayId(display.mDisplayId) && display.isSleeping();
    }

    public boolean isHomeActivity(Intent intent) {
        boolean result = intent.getComponent() != null && ("com.bbk.launcher2/.Launcher".contains(intent.getComponent().flattenToShortString()) || "com.android.launcher3/.Launcher".contains(intent.getComponent().flattenToShortString()));
        if (result) {
            VSlog.i(TAG, "Home Activity Always!");
        }
        return result;
    }
}