package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.multidisplay.MultiDisplayManager;
import android.os.Debug;
import android.os.FtBuild;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.FtFeature;
import android.view.DisplayInfo;
import android.view.InsetsState;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import com.android.server.LocalServices;
import com.android.server.am.EmergencyBroadcastManager;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.android.server.wm.WindowManagerService;
import com.vivo.appshare.AppShareConfig;
import com.vivo.services.autorecover.SystemAutoRecoverManagerInternal;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.rms.RmsInjectorImpl;
import java.util.ArrayList;
import java.util.function.Consumer;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoDisplayContentImpl implements IVivoDisplayContent {
    static final String TAG = "VivoDisplayContentImpl";
    WindowState currentDockDivider;
    boolean forceHideBelow;
    BarAnimController mBarAnimController;
    private VivoTaskDisplayAreaWrapper mDefaultVivoTaskDisplayAreaWrapper;
    private DisplayContent mDisplayContent;
    WindowState mMovingInputMethod;
    SurfaceControl.Transaction mTransaction;
    private WindowManagerService mWmService;
    WindowState rotationDockDivider;
    private WindowContainer mDividerBgWindowsContainers = null;
    private SurfaceControl mDividerBackground = null;
    private int mLastImeHeight = 0;
    final int mMaxForceUpdateDockBackgroundForIME = SystemProperties.getInt("persist.vivo.force_update_dockback.ime", (int) ProcessList.HEAVY_WEIGHT_APP_ADJ);
    final Runnable mForceUpdateDockBackgroundForIME = new Runnable() { // from class: com.android.server.wm.VivoDisplayContentImpl.1
        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoDisplayContentImpl.this.mWmService.mGlobalLock) {
                if (VivoDisplayContentImpl.this.mForceShowFlag) {
                    VivoDisplayContentImpl.this.mForceShowFlag = false;
                    if (VivoMultiWindowConfig.DEBUG) {
                        VSlog.i(VivoDisplayContentImpl.TAG, "mForceUpdateDockBackgroundForIME for updatedockbackground");
                    }
                    VivoDisplayContentImpl.this.updateDockBackground();
                }
            }
        }
    };
    boolean mForceShowFlag = false;
    boolean mLastImeFlag = false;
    private boolean isVivoMultiWindowAfterExitedJust = false;
    private Runnable TransientMultiWindowExited = new Runnable() { // from class: com.android.server.wm.VivoDisplayContentImpl.2
        @Override // java.lang.Runnable
        public void run() {
            VivoDisplayContentImpl.this.isVivoMultiWindowAfterExitedJust = false;
        }
    };
    private boolean isVivoMultiWindowOpSplitnavbar = false;
    private long exitSplitTime = 0;
    private final int MIN_DURATION = SystemProperties.getInt("persist.vivo.split_exited_op_splitnavbar_min", (int) ProcessList.BACKUP_APP_ADJ);
    private final int MAX_DURATION = 1500;
    private boolean mWallpaperVisible = false;
    private boolean mWallpaperVisibilityChanaged = false;
    boolean mVivoRotationToPortraitInProcess = false;
    private boolean mPrimaryStackRemovedState = true;
    private ActivityRecord mUpdateOrientationToken = null;
    private WindowState foucusOfHome = null;
    private boolean mWpClientForcedInvisible = false;
    private ArrayMap<WindowToken, Runnable> mDelayShowRunnables = new ArrayMap<>();
    private final ArrayList<WindowToken> mTempImeWindowTokens = new ArrayList<>();
    private Runnable mHideInputMethodTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.VivoDisplayContentImpl.3
        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoDisplayContentImpl.this.mWmService.mGlobalLock) {
                VSlog.d(VivoDisplayContentImpl.TAG, "restore mMovingInputMethod~~~");
                if (VivoDisplayContentImpl.this.mMovingInputMethod != null && VivoDisplayContentImpl.this.mMovingInputMethod.isHideByFingerPrint()) {
                    VivoDisplayContentImpl.this.mMovingInputMethod.hideByFingerPrint(false);
                    VivoDisplayContentImpl.this.mWmService.mWindowPlacerLocked.performSurfacePlacement();
                }
            }
        }
    };
    private boolean mAppSharedRecordingDisplay = false;
    private Configuration mTmpConfiguration = new Configuration();
    private VivoAppShareManager mVivoAppShareManager = VivoAppShareManager.getInstance();

    public VivoDisplayContentImpl(DisplayContent displayContent, WindowManagerService wmService) {
        this.mDefaultVivoTaskDisplayAreaWrapper = null;
        this.mTransaction = null;
        this.mDisplayContent = displayContent;
        this.mWmService = wmService;
        this.mDefaultVivoTaskDisplayAreaWrapper = new VivoTaskDisplayAreaWrapper(this.mDisplayContent.mRootDisplayArea, this.mWmService, displayContent);
        this.mTransaction = new SurfaceControl.Transaction();
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public void setDividerBgWindowsContainers(WindowContainer container) {
        if (this.mDividerBgWindowsContainers == container || container == null) {
            return;
        }
        this.mDividerBgWindowsContainers = container;
    }

    public void showDockBackground(WindowContainer topStack, Rect bounds) {
        SurfaceControl.Transaction t = this.mDisplayContent.getPendingTransaction();
        if (this.mDividerBackground == null) {
            SurfaceControl.Builder colorLayer = this.mDividerBgWindowsContainers.makeChildSurface((WindowContainer) null).setParent(this.mDividerBgWindowsContainers.getSurfaceControl()).setColorLayer();
            SurfaceControl build = colorLayer.setName("DividerBackground Layer for - " + this.mDividerBgWindowsContainers.getName()).build();
            this.mDividerBackground = build;
            if (build != null && t != null) {
                int colorValue = SystemProperties.getInt("persist.vivo.dividerbackground.color", 16777215);
                Color bgcolor = Color.valueOf(colorValue);
                float[] bgcolorArray = {bgcolor.red(), bgcolor.green(), bgcolor.blue()};
                VSlog.v(TAG, " updateDockBackground colorValue=" + colorValue + " {" + bgcolorArray[0] + ", " + bgcolorArray[1] + ", " + bgcolorArray[2] + "}");
                t.setColor(this.mDividerBackground, bgcolorArray);
            }
        }
        SurfaceControl surfaceControl = this.mDividerBackground;
        if (surfaceControl == null || topStack == null || bounds == null || t == null) {
            return;
        }
        t.setRelativeLayer(surfaceControl, topStack.getSurfaceControl(), -1);
        t.setAlpha(this.mDividerBackground, 1.0f);
        t.setBufferSize(this.mDividerBackground, bounds.width(), bounds.height());
        t.setWindowCrop(this.mDividerBackground, bounds.width(), bounds.height());
        t.setPosition(this.mDividerBackground, bounds.left, bounds.top);
        t.show(this.mDividerBackground);
    }

    public void hideDockBackground() {
        SurfaceControl surfaceControl = this.mDividerBackground;
        if (surfaceControl == null) {
            return;
        }
        this.mTransaction.hide(surfaceControl);
        this.mTransaction.apply(false);
    }

    public void destroyDockBackground() {
        if (this.mDividerBackground != null) {
            this.mDisplayContent.getPendingTransaction().remove(this.mDividerBackground);
            this.mDividerBackground = null;
            VSlog.v(TAG, "vivo_multiwindow_fmk destroy DockBackground");
        }
    }

    public void updateDockBackground() {
        VivoMultiWindowConfig.checkSplitThreadLockIfEnabled("updateDockBackground", this.mWmService.mGlobalLock);
        DockedStackDividerController dividerController = this.mDisplayContent.getDockedDividerController();
        if (this.mWmService.isInVivoMultiWindowConsiderVisibility() && dividerController != null) {
            updateDockBackground(dividerController.isResizing());
        } else {
            updateDockBackground(false);
        }
    }

    public void updateDockBackgroundForIME(boolean flag) {
        if (VivoMultiWindowConfig.IS_VIVO_SHOW_DIVIDER_BG && ((!flag && this.mLastImeFlag) || (flag && !this.mLastImeFlag))) {
            this.mForceShowFlag = true;
            if (this.mWmService.mH.hasCallbacks(this.mForceUpdateDockBackgroundForIME)) {
                this.mWmService.mH.removeCallbacks(this.mForceUpdateDockBackgroundForIME);
            }
            this.mWmService.mH.postDelayed(this.mForceUpdateDockBackgroundForIME, this.mMaxForceUpdateDockBackgroundForIME);
            updateDockBackground();
        }
        if (this.mLastImeFlag != flag) {
            this.mLastImeFlag = flag;
        }
    }

    boolean isVideoAppRunning(Task primaryTask, Task secondaryTask) {
        if (primaryTask != null && secondaryTask != null && primaryTask != null && secondaryTask != null) {
            ActivityRecord primaryApp = primaryTask.getTopVisibleActivity();
            ActivityRecord secondaryApp = secondaryTask.getTopVisibleActivity();
            VivoMultiWindowConfig vivoMultiWindowConfig = VivoMultiWindowConfig.getInstance();
            if (vivoMultiWindowConfig != null && primaryApp != null && secondaryApp != null && vivoMultiWindowConfig.isVideoAppRunning(primaryApp.toString(), secondaryApp.toString())) {
                return true;
            }
            return false;
        }
        return false;
    }

    public SurfaceControl.Builder makeSurface(SurfaceSession s) {
        return this.mWmService.makeSurfaceBuilder(s).setParent(this.mDisplayContent.mSurfaceControl);
    }

    public void init() {
        this.mDefaultVivoTaskDisplayAreaWrapper.updateContainer();
        if (this.mDisplayContent.isDefaultDisplay && FtBuild.getRomVersion() >= 12.0f && FtFeature.isFeatureSupport("vivo.software.baranim")) {
            this.mBarAnimController = new BarAnimController(this.mDisplayContent);
        }
        if (this.mDisplayContent.mDisplayId == 90000) {
            this.mDisplayContent.setCastOrientation(0);
        }
    }

    boolean isInNightMode() {
        if (VivoMultiWindowConfig.IS_VIVO_SUPPORT_NIGHTMODE_PROPERTY) {
            int mode = this.mWmService.mContext.getResources().getConfiguration().uiMode & 48;
            return mode == 32;
        }
        return false;
    }

    public void updateDockBackground(boolean bForceShow) {
        if (this.mDividerBgWindowsContainers == null) {
            return;
        }
        ActivityStack splitScreenPrimaryStack = this.mDisplayContent.getDefaultTaskDisplayArea().getRootSplitScreenPrimaryTask();
        ActivityStack splitScreenSecondaryStack = this.mDisplayContent.getDefaultTaskDisplayArea().getTopStackInWindowingMode(4);
        boolean bShowBg = false;
        Rect appBounds = this.mDisplayContent.mDisplayFrames.mStable;
        int rotation = this.mDisplayContent.getRotation();
        if (appBounds != null && (rotation == 1 || rotation == 3)) {
            appBounds.top = 0;
        }
        ActivityStack topStack = this.mDisplayContent.getTopStack();
        boolean divider_bg_enable = SystemProperties.getBoolean("sys.vivo.divider_bg_enable", true);
        if (isInNightMode()) {
            divider_bg_enable = false;
        }
        if (VivoMultiWindowConfig.IS_VIVO_SHOW_DIVIDER_BG && divider_bg_enable && splitScreenPrimaryStack != null && splitScreenSecondaryStack != null) {
            Task splitScreenPrimaryTask = splitScreenPrimaryStack.getTopMostTask();
            Task splitScreenSecondaryTask = splitScreenSecondaryStack.getTopMostTask();
            if ((bForceShow || this.mForceShowFlag) && splitScreenPrimaryTask != null && splitScreenSecondaryTask != null && splitScreenSecondaryTask.isVisible() && appBounds != null && !splitScreenSecondaryTask.isActivityTypeHome() && !splitScreenSecondaryTask.isActivityTypeRecents() && !isVideoAppRunning(splitScreenPrimaryTask, splitScreenSecondaryTask)) {
                bShowBg = true;
                if (topStack == splitScreenPrimaryStack) {
                    showDockBackground(splitScreenSecondaryStack, appBounds);
                } else {
                    showDockBackground(splitScreenPrimaryStack, appBounds);
                }
            }
        }
        if (VivoMultiWindowConfig.DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append(" updateDockBackground bForceShow=");
            sb.append(bForceShow);
            sb.append(" divider_bg_enable=");
            sb.append(divider_bg_enable);
            sb.append(" bShowBg=");
            sb.append(bShowBg);
            sb.append(" mForceShowFlag=");
            sb.append(this.mForceShowFlag);
            sb.append(" isVivoDockedDividerResizing=");
            sb.append(this.mWmService.isVivoDockedDividerResizing());
            sb.append(" topStack=");
            sb.append(topStack);
            sb.append(" splitScreenPrimaryTask=");
            sb.append(splitScreenPrimaryStack != null ? splitScreenPrimaryStack.getTopMostTask() : "null");
            sb.append(" splitScreenSecondaryTask=");
            sb.append(splitScreenSecondaryStack != null ? splitScreenSecondaryStack.getTopMostTask() : "null");
            sb.append(" appBounds=");
            sb.append(appBounds);
            VSlog.v(TAG, sb.toString());
        }
        if (!bShowBg) {
            hideDockBackground();
        }
    }

    public boolean skipAdjustedForIME() {
        if (VivoMultiWindowConfig.IS_VIVO_SKIP_ADJUST_IME) {
            if (ifForceSkipAdjustImeForRecents()) {
                return true;
            }
            return this.mWmService.isVivoMultiWindowSupport() && this.mWmService.isInVivoMultiWindowConsiderVisibility() && this.mDisplayContent.getDisplayId() == 0 && this.mWmService.isVivoDockedDividerResizing();
        }
        return false;
    }

    public boolean isSupportRotateSuggestion() {
        return VivoMultiWindowConfig.IS_VIVO_ROTATE_SUGGESTION;
    }

    public boolean isSupportLandscapeLauncher() {
        return VivoMultiWindowConfig.IS_VIVO_SUPPORT_LANDSCAPE_LAUNCHER;
    }

    public void freezeCurrentRotationEnterSplit(int rotation) {
        if (VivoMultiWindowConfig.IS_VIVO_ROTATE_SUGGESTION && this.mWmService.isVivoMultiWindowSupport()) {
            if (rotation != 0) {
                this.mWmService.freezeRotationWithReason(rotation, "dockedstack");
                VSlog.d(TAG, "addStackReferenceIfNeeded freezeRotation Rotation is " + rotation);
            }
            this.mWmService.mPolicy.updateOrientationListenerFromSplit();
        }
    }

    public void freezeRotation0LeaveSplit() {
        if (VivoMultiWindowConfig.IS_VIVO_ROTATE_SUGGESTION && this.mWmService.isVivoMultiWindowSupport()) {
            int userRotation = Settings.System.getIntForUser(this.mWmService.mContext.getContentResolver(), "user_rotation", 0, -2);
            if (userRotation != 0) {
                this.mWmService.freezeRotation(0);
                VSlog.d(TAG, "removeStackReferenceIfNeeded freezeRotation ROTATION_0");
            }
        }
    }

    public boolean changeForceUpdateValueForSplit(int req, int mLastOrientation, boolean forceUpdate) {
        boolean bVivoForceUpdateValid = true;
        boolean update = forceUpdate;
        if (this.mWmService.mAtmService.isMultiWindowSupport() && this.mWmService.isInVivoMultiWindowIgnoreVisibility() && this.mWmService.getVivoPauseRotationFlag() && this.mDisplayContent.getDisplayId() == 0) {
            bVivoForceUpdateValid = false;
        }
        if (this.mWmService.mAtmService.isMultiWindowSupport() && forceUpdate && !bVivoForceUpdateValid) {
            update = false;
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.v(TAG, "updateOrientationFromAppTokensLocked:forceUpdate=" + forceUpdate + " displayId=" + this.mDisplayContent.getDisplayId() + " req=" + req + " dc.getLastOrientation()=" + mLastOrientation + " isInVivoMultiWindowIgnoreVisibility()=" + this.mWmService.isInVivoMultiWindowIgnoreVisibility() + " mVivoPauseRotationFlag=" + this.mWmService.getVivoPauseRotationFlag() + " mDeferredRotationPauseCount=" + this.mWmService.getDefaultDisplayContentLocked().getDisplayRotation().getDeferredRotationPauseCount() + " bVivoForceUpdateValid=" + bVivoForceUpdateValid);
            }
        }
        return update;
    }

    public void createSplitScreenNavBar() {
        VivoTaskDisplayAreaWrapper vivoTaskDisplayAreaWrapper = this.mDefaultVivoTaskDisplayAreaWrapper;
        if (vivoTaskDisplayAreaWrapper != null) {
            vivoTaskDisplayAreaWrapper.createSplitScreenNavBar();
        }
    }

    public void removeSplitScreenNavBar() {
        VivoTaskDisplayAreaWrapper vivoTaskDisplayAreaWrapper = this.mDefaultVivoTaskDisplayAreaWrapper;
        if (vivoTaskDisplayAreaWrapper != null) {
            vivoTaskDisplayAreaWrapper.removeSplitScreenNavBar();
        }
    }

    public SurfaceControl getSplitScreenNavBar() {
        VivoTaskDisplayAreaWrapper vivoTaskDisplayAreaWrapper = this.mDefaultVivoTaskDisplayAreaWrapper;
        if (vivoTaskDisplayAreaWrapper != null) {
            return vivoTaskDisplayAreaWrapper.getSplitScreenNavBar();
        }
        return null;
    }

    public boolean isSplitScreenNavBarShow() {
        VivoTaskDisplayAreaWrapper vivoTaskDisplayAreaWrapper = this.mDefaultVivoTaskDisplayAreaWrapper;
        if (vivoTaskDisplayAreaWrapper != null) {
            return vivoTaskDisplayAreaWrapper.isSplitScreenNavBarShow();
        }
        return false;
    }

    public void showSplitScreenNavBar() {
        VivoTaskDisplayAreaWrapper vivoTaskDisplayAreaWrapper = this.mDefaultVivoTaskDisplayAreaWrapper;
        if (vivoTaskDisplayAreaWrapper != null) {
            vivoTaskDisplayAreaWrapper.showSplitScreenNavBar();
        }
    }

    public void hideSplitScreenNavBar() {
        VivoTaskDisplayAreaWrapper vivoTaskDisplayAreaWrapper = this.mDefaultVivoTaskDisplayAreaWrapper;
        if (vivoTaskDisplayAreaWrapper != null) {
            vivoTaskDisplayAreaWrapper.hideSplitScreenNavBar();
        }
    }

    public void setSplitScreenNavBarColor(int navColor) {
        VivoTaskDisplayAreaWrapper vivoTaskDisplayAreaWrapper = this.mDefaultVivoTaskDisplayAreaWrapper;
        if (vivoTaskDisplayAreaWrapper != null) {
            vivoTaskDisplayAreaWrapper.setSplitScreenNavBarColor(navColor);
        }
    }

    public int getSplitScreenNavBarColor() {
        VivoTaskDisplayAreaWrapper vivoTaskDisplayAreaWrapper = this.mDefaultVivoTaskDisplayAreaWrapper;
        if (vivoTaskDisplayAreaWrapper != null) {
            return vivoTaskDisplayAreaWrapper.getSplitScreenNavBarColor();
        }
        VSlog.w(TAG, "vivo_multiwindow_nvabar null warning color");
        return 0;
    }

    boolean ifForceSkipAdjustImeForRecents() {
        if (this.mDisplayContent == null) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            StringBuilder sb = new StringBuilder();
            sb.append("vivo_multiwindow_input, get some details info mCurrentFocus=");
            sb.append(this.mDisplayContent.mCurrentFocus != null ? this.mDisplayContent.mCurrentFocus : "null");
            sb.append(" mInputMethodTarget= ");
            sb.append(this.mDisplayContent.mInputMethodTarget);
            sb.append(" insplitmode ");
            sb.append(this.mDisplayContent.mInputMethodTarget != null ? Boolean.valueOf(this.mDisplayContent.mInputMethodTarget.inSplitScreenWindowingMode()) : "null");
            sb.append(" mInputMethodWindow=");
            sb.append(this.mDisplayContent.mInputMethodWindow);
            sb.append(" visible ");
            sb.append(this.mDisplayContent.mInputMethodWindow != null ? Boolean.valueOf(this.mDisplayContent.mInputMethodWindow.isVisibleLw()) : "null");
            sb.append(" recentstack visible = ");
            sb.append(isRecentStackVisible());
            VSlog.i(TAG, sb.toString());
        }
        if (this.mWmService.isVivoMultiWindowSupport() && this.mWmService.isInVivoMultiWindowConsiderVisibilityFocusedDisplay() && this.mWmService.getDefaultDisplayRotation() == 0 && ((this.mDisplayContent.mCurrentFocus == null && isRecentStackVisible()) || (this.mDisplayContent.mCurrentFocus != null && this.mDisplayContent.mCurrentFocus.isActivityTypeRecents()))) {
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.d(TAG, "vivo_multiwindow_input,force skip adjust for recents");
                return true;
            }
            return true;
        }
        return false;
    }

    private boolean isRecentStackVisible() {
        ActivityStack recentStack;
        DisplayContent displayContent = this.mDisplayContent;
        return (displayContent == null || (recentStack = displayContent.getStack(0, 3)) == null || !recentStack.isVisible()) ? false : true;
    }

    public boolean ifLetDividerHigherAppAnim(ActivityStack stack) {
        return false;
    }

    public void closeMultiWindowTransLockedIfInRotate() {
        synchronized (this.mWmService.mGlobalLock) {
            if (this.mWmService.mAtmService.isMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_SPLITFULL_ANIM && VivoMultiWindowTransManager.getInstance() != null && VivoMultiWindowTransManager.getInstance().hasSetAnimation()) {
                VivoMultiWindowTransManager.getInstance().closeMultiWindowTransitionLocked();
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.i(TAG, "closeMultiWindowTransLockedIfNeeded force finish multi transition for rotation");
                }
            }
        }
    }

    public void doSomethingInRemovePrimaryBeforeNull(int displayId, ActivityStack primaryStack) {
        if (displayId != 0 || !this.mWmService.isVivoMultiWindowSupport()) {
            return;
        }
        enableVivoTracksTransientMultiWindowExited();
    }

    public void doSomethingInRemovePrimaryAfterNull(int displayId) {
        if (displayId != 0 || !this.mWmService.isVivoMultiWindowSupport()) {
            return;
        }
        if (this.mWmService.isRotationFrozen()) {
            freezeRotation0LeaveSplit();
        }
        destroyDockBackground();
    }

    public void enableVivoTracksTransientMultiWindowExited() {
        WindowManagerService windowManagerService = this.mWmService;
        if (windowManagerService != null && windowManagerService.mH != null) {
            this.mWmService.mH.removeCallbacks(this.TransientMultiWindowExited);
            this.mWmService.mH.postDelayed(this.TransientMultiWindowExited, SystemProperties.getInt("persist.vivo.split_exited_transient_timeout", 800));
            this.isVivoMultiWindowAfterExitedJust = true;
            this.exitSplitTime = SystemClock.uptimeMillis();
        }
    }

    public void disableVivoTracksTransientMultiWindowExited() {
        WindowManagerService windowManagerService = this.mWmService;
        if (windowManagerService != null && windowManagerService.mH != null) {
            this.mWmService.mH.removeCallbacks(this.TransientMultiWindowExited);
            this.isVivoMultiWindowAfterExitedJust = false;
            this.exitSplitTime = 0L;
        }
    }

    public boolean isVivoMultiWindowExitedJustWithDisplay() {
        return this.isVivoMultiWindowAfterExitedJust;
    }

    public boolean isVivoOpSplitnavbarOfNoSplit() {
        int duration = (int) (SystemClock.uptimeMillis() - this.exitSplitTime);
        boolean z = duration >= this.MIN_DURATION && duration <= 1500;
        this.isVivoMultiWindowOpSplitnavbar = z;
        return z;
    }

    public boolean isInDisplay(ActivityStack stack) {
        VivoTaskDisplayAreaWrapper vivoTaskDisplayAreaWrapper = this.mDefaultVivoTaskDisplayAreaWrapper;
        return vivoTaskDisplayAreaWrapper != null && vivoTaskDisplayAreaWrapper.isInDisplay(stack);
    }

    public void notifyWallpaperVisibility(boolean isWallpaperVisible) {
        this.mWallpaperVisibilityChanaged = this.mWallpaperVisible ^ isWallpaperVisible;
        this.mWallpaperVisible = isWallpaperVisible;
    }

    public boolean isWallpaperVisibilityChangedOfVisible() {
        return this.mWallpaperVisibilityChanaged & this.mWallpaperVisible;
    }

    public void resetWallpaperVisibilityChanged() {
        this.mWallpaperVisibilityChanaged = false;
    }

    public void setRotationDockDividerIfNeeded(int displayId, int rotation) {
        if (this.mWmService.isVivoMultiWindowSupport() && displayId == 0 && !this.mVivoRotationToPortraitInProcess) {
            if (rotation == 0 || rotation == 2) {
                this.mVivoRotationToPortraitInProcess = true;
                this.rotationDockDivider = this.currentDockDivider;
            }
        }
    }

    public void setCurrentDockDivider(WindowState dockdivider) {
        this.currentDockDivider = dockdivider;
    }

    public void resetRotationToPortraitFlag() {
        this.mVivoRotationToPortraitInProcess = false;
    }

    public boolean getRotationToPortraitInProcess() {
        return this.mVivoRotationToPortraitInProcess;
    }

    public WindowState getRotationDockDivider() {
        return this.rotationDockDivider;
    }

    public boolean assignImeForSplitIfNeeded() {
        WindowState imeTarget = this.mDisplayContent.mInputMethodTarget;
        if (imeTarget == null || !imeTarget.inSplitScreenWindowingMode() || imeTarget.getSurfaceControl() == null || !this.mWmService.isVivoMultiWindowSupport()) {
            return false;
        }
        return true;
    }

    public boolean assignRelativeLayerForImeInSplitIfNeeded(boolean needAssignIme, int layer, int windowType) {
        if (this.mWmService.isVivoMultiWindowSupport()) {
            WindowState imeTarget = this.mDisplayContent.mInputMethodTarget;
            if ((VivoMultiWindowConfig.DEBUG || DEBUG_SPLIT_DISP) && imeTarget != null && imeTarget.inSplitScreenWindowingMode()) {
                VSlog.i(TAG, "assignChildLayers, isSplitSpecial false needAssignIme " + needAssignIme + " layer " + layer + " " + windowType);
            }
            return false;
        }
        return false;
    }

    public boolean skipAdjustForImeSplit(boolean dockVisible, boolean imeVisible, int displayId) {
        return false;
    }

    public void setPrimaryStackRemovedState(boolean state) {
        this.mPrimaryStackRemovedState = state;
    }

    public boolean getPrimaryStackRemovedState() {
        return this.mPrimaryStackRemovedState;
    }

    public void assignChildLayerForSplitScreenSecondaryTask(WindowContainer wc, SurfaceControl.Transaction t) {
        if (VivoMultiWindowConfig.IS_VIVO_MODIFY_HOME_LAYER) {
            TaskDisplayArea taskDisplayArea = this.mDisplayContent.getDefaultTaskDisplayArea();
            if (wc != null && wc == taskDisplayArea.getRootSplitScreenSecondaryTask() && taskDisplayArea.isSplitScreenModeActivated()) {
                wc.assignChildLayersForSplitScreenSecondaryTask(t);
            }
        }
    }

    public void unRegisterContentDetection() {
        DisplayPolicy displayPolicy = this.mDisplayContent.getDisplayPolicy();
        if (displayPolicy != null) {
            displayPolicy.unRegisterContentDetection();
        }
    }

    public void setFocusedWindow(int displayId, WindowState oldFocus, WindowState newFocus) {
        try {
            ((SystemAutoRecoverManagerInternal) LocalServices.getService(SystemAutoRecoverManagerInternal.class)).setFocusedWindow(displayId, oldFocus, newFocus);
        } catch (Exception e) {
            VSlog.d(TAG, "SystemAutoRecoverService not start yet~");
        }
    }

    public void setFocusedApps(int displayId, ActivityRecord oldFocus, ActivityRecord newFocus) {
        if (displayId == 0 && newFocus != null && newFocus.mActivityComponent != null) {
            this.mWmService.mDisplayManagerInternal.setDefaultDisplayFocusAppName(newFocus.mActivityComponent.getPackageName());
        }
        try {
            ((SystemAutoRecoverManagerInternal) LocalServices.getService(SystemAutoRecoverManagerInternal.class)).setFocusedApps(displayId, oldFocus, newFocus);
        } catch (Exception e) {
            VSlog.d(TAG, "SystemAutoRecoverService not start yet~");
        }
    }

    public boolean findFocusedWindowInFreeform(ActivityRecord activityRecord, ActivityRecord focusApp) {
        if (this.mWmService.isVivoFreeFormValid() && this.mWmService.isInVivoFreeform() && activityRecord != null && focusApp != activityRecord) {
            return true;
        }
        return false;
    }

    public ActivityRecord getUpdateOrientationToken() {
        return this.mUpdateOrientationToken;
    }

    public void setUpdateOrientationToken(ActivityRecord activityRecord) {
        this.mUpdateOrientationToken = activityRecord;
    }

    public void unRegisterSogouImeObserver() {
        DisplayPolicy displayPolicy = this.mDisplayContent.getDisplayPolicy();
        if (displayPolicy != null) {
            displayPolicy.unRegisterSogouImeObserver();
        }
    }

    public void hideInputMethodForVivoFreeformIfNeed() {
        InputMethodManagerInternal inputMethodManagerInternal;
        if (this.mWmService.isVivoFreeFormValid() && this.mWmService.isInVivoFreeform() && this.mDisplayContent.mCurrentFocus != null && this.mDisplayContent.mCurrentFocus.inFreeformWindowingMode() && this.mDisplayContent.mInputMethodTarget != null && this.mDisplayContent.mInputMethodWindow != null && this.mDisplayContent.mInputMethodWindow.isVisibleLw() && this.mDisplayContent.mInputMethodTarget.inFreeformWindowingMode() && this.mDisplayContent.mCurrentFocus != this.mDisplayContent.mInputMethodTarget && (inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class)) != null) {
            inputMethodManagerInternal.hideCurrentInputMethod(11);
        }
    }

    public SurfaceControl.ScreenshotGraphicBuffer screenshotDisplayToBuffer() {
        int rot;
        ScreenRotationAnimation screenRotationAnimation;
        synchronized (this.mWmService.mGlobalLock) {
            if (!this.mWmService.mPolicy.isScreenOn()) {
                if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                    VSlog.i("WindowManager", "Attempted to take screenshot while display was off.");
                }
                return null;
            }
            int dw = this.mDisplayContent.getDisplayInfo().logicalWidth;
            int dh = this.mDisplayContent.getDisplayInfo().logicalHeight;
            if (dw > 0 && dh > 0) {
                boolean z = false;
                Rect frame = new Rect(0, 0, dw, dh);
                int rot2 = this.mDisplayContent.getDisplay().getRotation();
                int rot3 = 3;
                if (rot2 != 1 && rot2 != 3) {
                    rot = rot2;
                    convertCropForSurfaceFlinger(frame, rot, dw, dh);
                    screenRotationAnimation = this.mWmService.mRoot.getDisplayContent(0).getRotationAnimation();
                    if (screenRotationAnimation != null && screenRotationAnimation.isAnimating()) {
                        z = true;
                    }
                    boolean inRotation = z;
                    if (WindowManagerDebugConfig.DEBUG_SCREENSHOT && inRotation) {
                        VSlog.v("WindowManager", "Taking screenshot while rotating");
                    }
                    return SurfaceControl.screenshotToBuffer(SurfaceControl.getInternalDisplayToken(), frame, dw, dh, inRotation, rot);
                }
                rot3 = 1;
                rot = rot3;
                convertCropForSurfaceFlinger(frame, rot, dw, dh);
                screenRotationAnimation = this.mWmService.mRoot.getDisplayContent(0).getRotationAnimation();
                if (screenRotationAnimation != null) {
                    z = true;
                }
                boolean inRotation2 = z;
                if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                    VSlog.v("WindowManager", "Taking screenshot while rotating");
                }
                return SurfaceControl.screenshotToBuffer(SurfaceControl.getInternalDisplayToken(), frame, dw, dh, inRotation2, rot);
            }
            return null;
        }
    }

    private static void convertCropForSurfaceFlinger(Rect crop, int rot, int dw, int dh) {
        if (rot == 1) {
            int tmp = crop.top;
            crop.top = dw - crop.right;
            crop.right = crop.bottom;
            crop.bottom = dw - crop.left;
            crop.left = tmp;
        } else if (rot == 2) {
            int tmp2 = crop.top;
            crop.top = dh - crop.bottom;
            crop.bottom = dh - tmp2;
            int tmp3 = crop.right;
            crop.right = dw - crop.left;
            crop.left = dw - tmp3;
        } else if (rot == 3) {
            int tmp4 = crop.top;
            crop.top = crop.left;
            crop.left = dh - crop.bottom;
            crop.bottom = crop.right;
            crop.right = dh - tmp4;
        }
    }

    public void notifyInputMethodPosWhenMinimized(boolean imeVisible, int imeHeight, WindowState imeWin) {
        if (VivoFreeformUtils.sIsVosProduct && this.mWmService.isVivoFreeFormValid() && this.mWmService.isInVivoFreeform() && this.mWmService.isFreeFormMin() && this.mWmService.isFreeformMiniStateChanged() && imeVisible && imeHeight > 0) {
            this.mWmService.setFreeformMiniStateChanged(false);
            Settings.System.putIntForUser(this.mWmService.mContext.getContentResolver(), "game_show_ime_pos", -1, this.mWmService.mCurrentUserId);
            Settings.System.putIntForUser(this.mWmService.mContext.getContentResolver(), "game_show_ime_pos", this.mDisplayContent.mDisplayFrames.mCurrent.bottom, this.mWmService.mCurrentUserId);
        }
    }

    public void adjustFreeformTaskForIme(boolean imeVisible, int imeHeight, WindowState imeWin) {
        if (this.mWmService.isVivoFreeFormValid() && this.mWmService.isInVivoFreeform() && this.mDisplayContent.mFocusedApp != null && this.mDisplayContent.mFocusedApp.getTask() != null && this.mDisplayContent.mFocusedApp.getTask().inFreeformWindowingMode() && imeVisible && (this.mDisplayContent.getRotation() == 0 || this.mDisplayContent.getRotation() == 2)) {
            boolean imeHeightChanged = false;
            if (this.mLastImeHeight != imeHeight) {
                this.mLastImeHeight = imeHeight;
                imeHeightChanged = true;
            }
            resizeFreeformTaskForImeVisible(imeHeight, imeHeightChanged);
            return;
        }
        resizeFreeformTaskBackForIme();
    }

    private void resizeFreeformTaskForImeVisible(int imeHeight, boolean imeHeightChanged) {
        if (imeHeight > 0) {
            if ((!this.mWmService.isFreeformStackMove() || imeHeightChanged) && this.mWmService.getFreeformPosition() != null) {
                Rect tmpRect = new Rect();
                if (this.mWmService.getFreeformPosition().isEmpty() && !this.mWmService.isFreeformStackMove()) {
                    this.mDisplayContent.mFocusedApp.getTask().getBounds(this.mWmService.getFreeformPosition());
                }
                tmpRect.set(this.mWmService.getFreeformPosition());
                int scaleBootom = tmpRect.top + ((int) (tmpRect.height() * this.mWmService.getFreeformScale()));
                Rect dockRect = new Rect();
                dockRect.set(this.mDisplayContent.mDisplayFrames.mDock);
                int dockHeight = dockRect.height();
                if (scaleBootom <= dockHeight - imeHeight) {
                    if (!this.mWmService.getFreeformPosition().isEmpty()) {
                        this.mWmService.getFreeformPosition().setEmpty();
                        return;
                    }
                    return;
                }
                int marginWithIME = WindowManagerService.dipToPixel(4, this.mDisplayContent.getDisplayMetrics());
                tmpRect.offset(0, (Math.round(dockHeight - imeHeight) - scaleBootom) - marginWithIME);
                if (tmpRect.isEmpty()) {
                    return;
                }
                VSlog.d(TAG, "resizeFreeformTaskForImeVisible send resize task message task:" + this.mDisplayContent.mFocusedApp.getTask().mTaskId);
                this.mWmService.getHandler().removeMessages(105);
                this.mWmService.getHandler().obtainMessage(105, this.mDisplayContent.mFocusedApp.getTask().mTaskId, 0, tmpRect).sendToTarget();
            }
        }
    }

    private void resizeFreeformTaskBackForIme() {
        if (this.mWmService.isVivoFreeFormValid() && this.mWmService.isInVivoFreeform() && this.mWmService.isFreeformStackMove() && this.mWmService.getFreeformPosition() != null && !this.mWmService.getFreeformPosition().isEmpty() && !this.mWmService.isStartingRecentBreakAdjust()) {
            ActivityStack freeformStack = this.mWmService.mRoot.getVivoFreeformStack();
            Task freeformTask = freeformStack != null ? freeformStack.getTopMostTask() : null;
            if (freeformTask != null) {
                if (this.mDisplayContent.mFocusedApp != null && this.mDisplayContent.mFocusedApp.getTask() != null) {
                    Rect newFreeformPosition = new Rect();
                    freeformTask.getBounds(newFreeformPosition);
                    int width = newFreeformPosition.width();
                    int height = newFreeformPosition.height();
                    if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                        VSlog.i(TAG, "adjustForImeIfNeeded newFreeformPosition = " + newFreeformPosition + " mService.mFreeformPosition = " + this.mWmService.getFreeformPosition());
                    }
                    if (width != this.mWmService.getFreeformPosition().width() || height != this.mWmService.getFreeformPosition().height()) {
                        this.mWmService.getFreeformPosition().right = this.mWmService.getFreeformPosition().left + width;
                        this.mWmService.getFreeformPosition().bottom = this.mWmService.getFreeformPosition().top + height;
                    }
                }
                VSlog.d(TAG, "resizeFreeformTaskBackForIme send resize task message task:" + freeformTask.mTaskId);
                this.mWmService.getHandler().removeMessages(105);
                this.mWmService.getHandler().obtainMessage(105, freeformTask.mTaskId, 1, this.mWmService.getFreeformPosition()).sendToTarget();
            }
        }
    }

    public boolean adjustDragBoundsForIme(Task task, Rect windowDragBounds) {
        WindowState imeWin;
        DisplayContent displayContent = task.getDisplayContent();
        if (displayContent != null && (imeWin = displayContent.mInputMethodWindow) != null && imeWin.isVisibleLw() && task.inFreeformWindowingMode()) {
            int imeHeight = displayContent.mDisplayFrames.getInputMethodWindowVisibleHeight();
            Rect dockRect = new Rect();
            dockRect.set(this.mDisplayContent.mDisplayFrames.mDock);
            int dockHeight = dockRect.height();
            int scaleBootom = windowDragBounds.top + ((int) (windowDragBounds.height() * this.mWmService.getFreeformScale()));
            if (scaleBootom > dockHeight - imeHeight) {
                int marginWithIME = WindowManagerService.dipToPixel(4, this.mDisplayContent.getDisplayMetrics());
                windowDragBounds.offset(0, (Math.round(dockHeight - imeHeight) - scaleBootom) - marginWithIME);
                return true;
            }
        }
        return false;
    }

    public void updateWindowFocus(int displayId, WindowState newFocus, WindowState oldFocus) {
        RmsInjectorImpl.getInstance().updateWindowFocus(displayId, newFocus, oldFocus);
        updateWindowFocusOfHome(newFocus, oldFocus);
    }

    public boolean isRefreshRateAdjusterSupported() {
        return RmsInjectorImpl.getInstance().isRefreshRateAdjusterSupported();
    }

    public void applyWindowRefreshRate(int displayId) {
        RmsInjectorImpl.getInstance().applyWindowRefreshRate(displayId);
    }

    private void updateWindowFocusOfHome(WindowState newFocus, WindowState oldFocus) {
        if (oldFocus != newFocus) {
            if (newFocus != null && newFocus.isActivityTypeHome()) {
                this.foucusOfHome = newFocus;
            } else if (oldFocus != null && oldFocus.isActivityTypeHome()) {
                this.foucusOfHome = oldFocus;
            } else if (oldFocus != null && newFocus != null) {
                this.foucusOfHome = null;
            } else if (oldFocus != null) {
                this.foucusOfHome = null;
            }
        }
    }

    public WindowState getLatestFoucusOfHome() {
        return this.foucusOfHome;
    }

    public void resetForceHideBelow() {
        this.forceHideBelow = false;
    }

    public void setForceHideVisibility(WindowState win) {
        if (win.mWinAnimator.hasSurface()) {
            boolean z = true;
            if (win.setForceHideVisibility(!this.forceHideBelow)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Now policy ");
                sb.append(this.forceHideBelow ? "hidden" : "shown");
                sb.append(" by force hiding: ");
                sb.append(win);
                VSlog.v(TAG, sb.toString());
            }
            this.forceHideBelow |= ((win.mAttrs.privateFlags & Integer.MIN_VALUE) == 0 || !win.hasDrawnLw()) ? false : false;
        }
    }

    public void updateWallpaperClientVisibility(boolean visible) {
        StringBuilder sb = new StringBuilder();
        sb.append("updateWallpaperClientVisibility ");
        sb.append(visible);
        sb.append(" lastForceVis = ");
        sb.append(!this.mWpClientForcedInvisible);
        VSlog.v(TAG, sb.toString());
        if (this.mWpClientForcedInvisible != visible) {
            return;
        }
        this.mWpClientForcedInvisible = !visible;
        WallpaperController wallpaperController = this.mDisplayContent.mWallpaperController;
        for (int curTokenNdx = wallpaperController.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            WallpaperWindowToken token = (WallpaperWindowToken) wallpaperController.mWallpaperTokens.get(curTokenNdx);
            for (int ndx = token.mChildren.size() - 1; ndx >= 0; ndx--) {
                WindowState windowState = (WindowState) token.mChildren.get(ndx);
                windowState.dispatchWallpaperClientVisibility(visible);
            }
        }
    }

    public boolean getWpClientForcedInvisible() {
        return this.mWpClientForcedInvisible;
    }

    public boolean hasBarAnimController() {
        return this.mBarAnimController != null;
    }

    public void prepareAppTransition() {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.prepareAppTransition();
        }
    }

    public void handleAppTransitionReady(int transit) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.handleAppTransitionReady(transit);
        }
    }

    public void setActivityVisibilityBeforeCommit() {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.setActivityVisibilityBeforeCommit();
        }
    }

    public void transferStartingWindow(ActivityRecord fromActivity, ActivityRecord toActivity) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.transferStartingWindow(fromActivity, toActivity);
        }
    }

    public void appTransitionFinished() {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.appTransitionFinished();
        }
    }

    public DisplayFrames getFixedDisplayFrames(WindowState win, DisplayFrames displayFrames) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.getFixedDisplayFrames(win, displayFrames);
        }
        return displayFrames;
    }

    public InsetsState mayFixedState(InsetsState state) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.mayFixedState(state);
        }
        return state;
    }

    public void notifyWindowRemoved(WindowState win) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.notifyWindowRemoved(win);
        }
    }

    public WindowState getWindowContainerForStatus(WindowState wc) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.getWindowContainerForStatus(wc);
        }
        return wc;
    }

    public WindowState getWindowContainerForGesture(WindowState wc) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.getWindowContainerForGesture(wc);
        }
        return wc;
    }

    public boolean shouldWinApplyRotation(WindowState win) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.shouldWinApplyRotation(win);
        }
        return true;
    }

    public void notifyAppRemoved(ActivityRecord token) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.notifyAppRemoved(token);
        }
    }

    public void notifyRemoteAnimationInitial(ArrayList<Task> visibleTasks, ActivityRecord homeApp, boolean isFromRemote, boolean isLandscapeGesture) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.notifyRemoteAnimationInitial(visibleTasks, homeApp, isFromRemote, isLandscapeGesture);
        }
    }

    public void notifyRemoteAnimationStart() {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.notifyRemoteAnimationStart();
        }
    }

    public void notifyRemoteAnimationFinished(int reorderMode, int behavMode) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.notifyRemoteAnimationFinished(reorderMode, behavMode);
        }
    }

    public boolean shouldHideStatusBar() {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.shouldHideStatusBar();
        }
        return true;
    }

    public void onRotationChanged() {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.onRotationChanged();
        }
    }

    public boolean shouldChangeDrawState(WindowState win) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.shouldChangeDrawState(win);
        }
        return true;
    }

    public SurfaceControl getParentSurfaceControlForToken(WindowToken token) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.getParentSurfaceControlForToken(token);
        }
        return null;
    }

    public RemoteAnimationTarget[] startRemoteAnimationForBars(long durationHint, long statusBarTransitionDelay, Consumer<AnimationAdapter> animationCanceledRunnable, ArrayList<AnimationAdapter> adaptersOut) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.startRemoteAnimationForBars(durationHint, statusBarTransitionDelay, animationCanceledRunnable, adaptersOut);
        }
        return null;
    }

    public void assignChildLayersForStatus(WindowContainer parent, int layer) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.assignChildLayersForStatus(parent, layer);
        }
    }

    public boolean shouldAssignLayerForWindowContainer(WindowContainer wc) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.shouldAssignLayerForWindowContainer(wc);
        }
        return true;
    }

    public SurfaceControl[] getBarsLayers() {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.getBarsLayers();
        }
        return null;
    }

    public void notifyRotationWatcher(WindowManagerService.RotationWatcher watcher, int pid) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.notifyRotationWatcher(watcher, pid);
        }
    }

    public void notifyRotationWatcherRemoved(WindowManagerService.RotationWatcher watcher) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            barAnimController.notifyRotationWatcherRemoved(watcher);
        }
    }

    public boolean isBarAnimReady() {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.isBarAnimReady();
        }
        return true;
    }

    public boolean shouldFixedRotationAnimateForBar() {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.shouldFixedRotationAnimateForBar();
        }
        return true;
    }

    public boolean isInFakeBarCapture(WindowState wc) {
        BarAnimController barAnimController = this.mBarAnimController;
        if (barAnimController != null) {
            return barAnimController.isInFakeBarCapture(wc);
        }
        return false;
    }

    public void showForFixedRotationAnimation(FixedRotationAnimationController controller) {
        controller.showDelay();
    }

    public void cancelDalayShowForFixedRotationAnimation() {
        for (int i = this.mDelayShowRunnables.size() - 1; i >= 0; i--) {
            Runnable r = this.mDelayShowRunnables.valueAt(i);
            this.mDisplayContent.mWmService.getHandler().removeCallbacks(r);
        }
    }

    public ArrayMap<WindowToken, Runnable> getDelayShowRunnables() {
        return this.mDelayShowRunnables;
    }

    public boolean isWindowSecure() {
        if (!VivoEasyShareManager.SUPPORT_PCSHARE) {
            return false;
        }
        return this.mDisplayContent.forAllWindows($$Lambda$VivoDisplayContentImpl$cbUtN2IJcNGjjAp1R3hEhgWfudM.INSTANCE, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$isWindowSecure$0(WindowState w) {
        return w.isVisible() && w.mViewVisibility == 0 && (w.mAttrs.flags & EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP) != 0;
    }

    public int getFlagsOfLastMotionEvent() {
        DisplayContent displayContent = this.mDisplayContent;
        if (displayContent != null && displayContent.mPointerEventDispatcher != null) {
            return this.mDisplayContent.mPointerEventDispatcher.getFlagsOfLastMotionEvent();
        }
        return 0;
    }

    public void notifyDisplayRemoved() {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return;
        }
        if (this.mDisplayContent.mDisplayId == 80000) {
            SystemProperties.set("persist.vivo.carnetworking.rate", "0");
            VSlog.d("VivoCar", "notifyDisplayRemoved " + this.mDisplayContent.mDisplayId + " callers=" + Debug.getCallers(5));
        }
        if (this.mDisplayContent.mDisplayId == 90000) {
            this.mWmService.mAtmService.updateCastStates((String) null);
        }
    }

    public boolean shouldUpdateWindowForVCar(int direction) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && direction != this.mDisplayContent.mCarDirection) {
            if (this.mDisplayContent.mDisplayId == 80000) {
                VSlog.d("VivoFocused", "!!update from " + this.mDisplayContent.mCarDirection + " to " + direction);
            }
            this.mDisplayContent.mCarDirection = direction;
            return true;
        }
        return false;
    }

    public void notifyAppShareDisplayRemoved(DisplayContent display) {
        this.mVivoAppShareManager.notifyAppShareDisplayRemovedLocked(display);
    }

    public void moveAllStacksToDefaultDisplayForAppShareLocked(TaskDisplayArea currentDisplay, TaskDisplayArea targetDisplay) {
        this.mVivoAppShareManager.moveAllStacksToDefaultDisplayForAppShareLocked(currentDisplay, targetDisplay);
    }

    public boolean isAppShareForeground() {
        return this.mVivoAppShareManager.isAppShareForeground();
    }

    public void notifyAppShareAboutFreeFormMode(int displayId, int windowingMode) {
        this.mVivoAppShareManager.notifyAppShareAboutFreeFormMode(displayId, windowingMode);
    }

    public void onInputMethodMoved(int displayId) {
        DisplayContent displayContent = this.mWmService.mRoot.getDisplayContent(displayId);
        for (int i = 0; i < displayContent.mImeWindowsContainers.mChildren.size(); i++) {
            this.mTempImeWindowTokens.add((WindowToken) displayContent.mImeWindowsContainers.mChildren.get(i));
        }
        for (int i2 = 0; i2 < this.mTempImeWindowTokens.size(); i2++) {
            WindowToken wtoken = this.mTempImeWindowTokens.get(i2);
            wtoken.onDisplayChanged(this.mDisplayContent);
        }
        this.mTempImeWindowTokens.clear();
    }

    public void moveImeToDisplay(DisplayContent prevDc) {
        if (prevDc == null) {
            VSlog.v(TAG, "moveImeToDisplay return due to NULL prevDc!");
        } else if (this.mDisplayContent.getDisplayId() == prevDc.getDisplayId()) {
            VSlog.v(TAG, "moveImeToDisplay return due to same display!");
        } else {
            VSlog.v(TAG, "moveImeToDisplay :  FromDisplayId = " + prevDc.getDisplayId() + " ; ToDisplayId = " + this.mDisplayContent.getDisplayId());
            for (int i = 0; i < prevDc.mImeWindowsContainers.mChildren.size(); i++) {
                this.mTempImeWindowTokens.add((WindowToken) prevDc.mImeWindowsContainers.mChildren.get(i));
            }
            for (int i2 = 0; i2 < this.mTempImeWindowTokens.size(); i2++) {
                WindowToken wtoken = this.mTempImeWindowTokens.get(i2);
                VSlog.v(TAG, "moveImeToDisplay : i = " + i2 + "; wtoken = " + wtoken + " ; windowType = " + wtoken.windowType);
                if (!AppShareConfig.SUPPROT_APPSHARE || !this.mWmService.getVivoInjectInstance().isInputMethodSelectDialogToken(wtoken)) {
                    wtoken.onDisplayChanged(this.mDisplayContent);
                    if (wtoken.windowType == 2011) {
                        if (wtoken.mChildren.size() > 0) {
                            WindowState inputMethod = (WindowState) wtoken.mChildren.get(0);
                            if (inputMethod != null) {
                                if (inputMethod.isVisible()) {
                                    this.mMovingInputMethod = inputMethod;
                                    inputMethod.hideByFingerPrint(true);
                                    this.mWmService.mH.removeCallbacks(this.mHideInputMethodTimeoutRunnable);
                                    this.mWmService.mH.postDelayed(this.mHideInputMethodTimeoutRunnable, 2000L);
                                }
                                VivoRatioControllerUtilsImpl.getInstance().addDisplayId(inputMethod.mSession.mPid, this.mDisplayContent.getDisplayId(), "moveIME");
                            }
                        } else {
                            VivoRatioControllerUtilsImpl.getInstance().handleInputMethodProcessMoved(this.mDisplayContent.getDisplayId());
                        }
                    }
                }
            }
            if (AppShareConfig.SUPPROT_APPSHARE && this.mTempImeWindowTokens.size() > 0) {
                this.mDisplayContent.setLayoutNeeded();
                prevDc.setLayoutNeeded();
                this.mWmService.mWindowPlacerLocked.requestTraversal();
                this.mWmService.getVivoInjectInstance().updateLastInputMethodMove(this.mDisplayContent.getDisplayId());
            }
            this.mTempImeWindowTokens.clear();
        }
    }

    public void moveImeToDisplay(int displayId, boolean updateConfig, boolean force) {
    }

    public void updateAppSharedRecordDisplay() {
        int displayId;
        DisplayInfo displayInfo;
        if (!AppShareConfig.SUPPROT_APPSHARE || (displayId = this.mDisplayContent.getDisplayId()) == 0 || (displayInfo = this.mDisplayContent.getDisplayInfo()) == null) {
            return;
        }
        if (displayId == 10086) {
            if (displayInfo.logicalWidth > displayInfo.logicalHeight && this.mDisplayContent.getDisplayRotation() != null) {
                this.mDisplayContent.getDisplayRotation().setRotation(1);
                VSlog.d(TAG, "display construct displayId: : " + displayId + ", rotation: " + this.mDisplayContent.getRotation());
                return;
            }
            return;
        }
        VSlog.d(TAG, "display construct name : " + displayInfo.name);
        if (displayInfo.name != null && displayInfo.ownerPackageName != null && displayInfo.ownerPackageName.equals(AppShareConfig.APP_SHARE_PKG_NAME) && displayInfo.name.equals("app_share_screen_recorder")) {
            this.mAppSharedRecordingDisplay = true;
        }
    }

    public boolean isAppSharedRecordDisplay() {
        return this.mAppSharedRecordingDisplay;
    }

    public boolean isWindowSecureForAppShare() {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return false;
        }
        return this.mDisplayContent.forAllWindows($$Lambda$VivoDisplayContentImpl$W2IqOaJ1kQEyQkE8nhCrsP33Y8Y.INSTANCE, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$isWindowSecureForAppShare$1(WindowState w) {
        return w.isVisible() && w.mViewVisibility == 0 && (w.mAttrs.flags & EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP) != 0;
    }

    public void decideResizeDisplay() {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        int displayId = this.mDisplayContent.getDisplayId();
        if (!this.mWmService.canResizeDisplay(displayId)) {
            VSlog.d(TAG, "decideResizeDisplay displayId : " + displayId + ", do not resize.");
            return;
        }
        boolean isLandscapeOrientation = this.mWmService.isLandscapeOrientation(this.mDisplayContent.getLastOrientation(), this.mDisplayContent.getRotation());
        int w = this.mDisplayContent.mInitialDisplayWidth;
        int h = this.mDisplayContent.mInitialDisplayHeight;
        if (isLandscapeOrientation || isLandscapeOrientation) {
            if (w < h) {
                w = h;
                h = w;
            }
        } else if (w > h) {
            w = h;
            h = w;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("decideResizeDisplay displayId : ");
        sb.append(displayId);
        sb.append(", rotation : ");
        int rotation = isLandscapeOrientation ? 1 : 0;
        sb.append(rotation);
        sb.append(", w : ");
        sb.append(w);
        sb.append(", h : ");
        sb.append(h);
        VSlog.d(TAG, sb.toString());
        this.mDisplayContent.mInitialDisplayWidth = w;
        this.mDisplayContent.mInitialDisplayHeight = h;
        updateBaseDisplayMetricsAppShared(this.mDisplayContent.mInitialDisplayWidth, this.mDisplayContent.mInitialDisplayHeight, this.mDisplayContent.mBaseDisplayDensity);
        this.mWmService.mDisplayManagerInternal.resizeVirtualDisplay(displayId, w, h);
    }

    private void updateBaseDisplayMetricsAppShared(int baseWidth, int baseHeight, int baseDensity) {
        this.mDisplayContent.mBaseDisplayWidth = baseWidth;
        this.mDisplayContent.mBaseDisplayHeight = baseHeight;
        this.mDisplayContent.mBaseDisplayDensity = baseDensity;
        this.mDisplayContent.mBaseDisplayRect.set(0, 0, baseWidth, baseHeight);
    }

    public void relayoutForDisplayUpdate() {
        this.mDisplayContent.initializeDisplayBaseInfo();
        this.mDisplayContent.setLayoutNeeded();
        DisplayContent displayContent = this.mDisplayContent;
        displayContent.updateDisplayAndOrientation(displayContent.getConfiguration().uiMode, this.mTmpConfiguration);
    }
}