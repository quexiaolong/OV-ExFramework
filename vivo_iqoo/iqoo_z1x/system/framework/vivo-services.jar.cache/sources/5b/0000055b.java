package com.android.server.wm;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.InputWindowHandle;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import java.util.ArrayList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoFreeformWindowManager {
    private static final String TAG = "VivoFreeformWindowManager";
    private boolean closingFreeForm;
    private boolean enteringFreeForm;
    private boolean exitingFreeForm;
    private boolean isAdjustedForLeftNavBar;
    private boolean isFreeFormMax;
    private boolean isFreeFormMin;
    private boolean isFreeFormResizing;
    private boolean isFreeformAnimating;
    private boolean isFreeformExitToTop;
    private boolean isFreeformMiniStateChanged;
    private boolean isMovingFreeformTaskToSecondDisplay;
    private boolean isResizingTask;
    private boolean isVivoFreeformListValid;
    private ArrayList<String> mForceFullScreenActivitylistFreeform;
    private Rect mFreeformPosition;
    private float mFreeformScale;
    private boolean mFreeformStackMove;
    private VivoFreeformGesturesPointerEventListener mGesturesPointerEventListener;
    private boolean mIsDirectFreeformFinishingResizing;
    private boolean mIsInDirectFreeformState;
    private boolean mIsRemovingFreeformStack;
    private boolean mIsVivoFreeformRuntimeEnable;
    private float mLastFreeformScale;
    VivoMultiWindowTransManager mMultiWindowWmsInstance;
    private boolean mStartingRecent;
    private boolean mStartingRecentBreakAdjust;
    private Runnable mStopFreezingRunnable;
    private boolean mTopFullscreenIsTranslucency;
    private WindowManagerService mWmService;
    private boolean settingFreeformTaskFocused;

    private VivoFreeformWindowManager() {
        this.mFreeformStackMove = false;
        this.mFreeformPosition = new Rect();
        this.exitingFreeForm = false;
        this.enteringFreeForm = false;
        this.isFreeFormMax = false;
        this.isFreeFormMin = false;
        this.isFreeFormResizing = false;
        this.isResizingTask = false;
        this.closingFreeForm = false;
        this.isFreeformAnimating = false;
        this.mIsVivoFreeformRuntimeEnable = false;
        this.isFreeformExitToTop = false;
        this.mIsInDirectFreeformState = false;
        this.mIsDirectFreeformFinishingResizing = false;
        this.mStartingRecent = false;
        this.mStartingRecentBreakAdjust = false;
        this.mTopFullscreenIsTranslucency = false;
        this.mForceFullScreenActivitylistFreeform = new ArrayList<>();
        this.isVivoFreeformListValid = true;
        this.mMultiWindowWmsInstance = VivoMultiWindowTransManager.getInstance();
        this.settingFreeformTaskFocused = false;
        this.isAdjustedForLeftNavBar = false;
        this.isMovingFreeformTaskToSecondDisplay = false;
        this.mIsRemovingFreeformStack = false;
        this.isFreeformMiniStateChanged = false;
        this.mFreeformScale = 1.0f;
        this.mLastFreeformScale = 1.0f;
        this.mStopFreezingRunnable = new Runnable() { // from class: com.android.server.wm.VivoFreeformWindowManager.1
            @Override // java.lang.Runnable
            public void run() {
                VivoFreeformWindowManager.this.mWmService.stopFreezingScreen();
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class VivoFreeformWindowManagerHolder {
        private static final VivoFreeformWindowManager sVivoFreeformWindowManager = new VivoFreeformWindowManager();

        private VivoFreeformWindowManagerHolder() {
        }
    }

    public static VivoFreeformWindowManager getInstance() {
        return VivoFreeformWindowManagerHolder.sVivoFreeformWindowManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(WindowManagerService wm) {
        this.mWmService = wm;
        this.mGesturesPointerEventListener = new VivoFreeformGesturesPointerEventListener(wm);
    }

    public boolean isFreeformStackMove() {
        return this.mFreeformStackMove;
    }

    public void setFreeformStackMove(boolean freeformStackMove) {
        this.mFreeformStackMove = freeformStackMove;
    }

    public Rect getFreeformPosition() {
        return this.mFreeformPosition;
    }

    public boolean isExitingFreeForm() {
        return this.exitingFreeForm;
    }

    public void setExitingFreeForm(boolean exitingFreeForm) {
        this.exitingFreeForm = exitingFreeForm;
    }

    public boolean isEnteringFreeForm() {
        return this.enteringFreeForm;
    }

    public void setEnteringFreeForm(boolean enteringFreeForm) {
        this.enteringFreeForm = enteringFreeForm;
    }

    public boolean isResizingTask() {
        return this.isResizingTask;
    }

    public void setResizingTask(boolean resizingTask) {
        this.isResizingTask = resizingTask;
    }

    public boolean isClosingFreeForm() {
        return this.closingFreeForm;
    }

    public void setClosingFreeForm(boolean closingFreeForm) {
        this.closingFreeForm = closingFreeForm;
    }

    public boolean isDirectFreeformFinishingResizing() {
        return this.mIsDirectFreeformFinishingResizing;
    }

    public void setIsDirectFreeformFinishingResizing(boolean isDirectFreeformFinishingResizing) {
        this.mIsDirectFreeformFinishingResizing = isDirectFreeformFinishingResizing;
    }

    public boolean isStartingRecent() {
        return this.mStartingRecent;
    }

    public void setStartingRecent(boolean startingRecent) {
        this.mStartingRecent = startingRecent;
    }

    public boolean isStartingRecentBreakAdjust() {
        return this.mStartingRecentBreakAdjust;
    }

    public void setStartingRecentBreakAdjust(boolean startingRecentBreakAdjust) {
        this.mStartingRecentBreakAdjust = startingRecentBreakAdjust;
    }

    public boolean isSettingFreeformTaskFocused() {
        return this.settingFreeformTaskFocused;
    }

    public void setSettingFreeformTaskFocused(boolean settingFreeformTaskFocused) {
        this.settingFreeformTaskFocused = settingFreeformTaskFocused;
    }

    public boolean isRemovingFreeformStack() {
        return this.mIsRemovingFreeformStack;
    }

    public void setIsRemovingFreeformStack(boolean isRemovingFreeformStack) {
        this.mIsRemovingFreeformStack = isRemovingFreeformStack;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enableVivoFreeFormRuntime(boolean enable, boolean inDirectFreeformState) {
        this.mIsVivoFreeformRuntimeEnable = enable;
        this.mIsInDirectFreeformState = inDirectFreeformState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateFreeformForceList(ArrayList<String> forceFullScreenActivitylist) {
        synchronized (this.mWmService.mGlobalLock) {
            this.mForceFullScreenActivitylistFreeform = forceFullScreenActivitylist;
        }
    }

    public void setVivoFreeformWhiteListSwitchValue(boolean switchValue) {
        this.isVivoFreeformListValid = switchValue;
    }

    public boolean isWmInVivoFreeForm() {
        return isVivoFreeFormValid() && isInVivoFreeform();
    }

    public boolean isVivoFreeformFeatureSupport() {
        return VivoFreeformMultiWindowConfig.IS_VIVO_FREEFORM_SUPPORT;
    }

    public boolean isVivoFreeFormValid() {
        return VivoFreeformMultiWindowConfig.IS_VIVO_FREEFORM_SUPPORT && this.mIsVivoFreeformRuntimeEnable;
    }

    public boolean isInVivoFreeform() {
        return this.mWmService.mRoot.hasVivoFreeformStack();
    }

    public boolean isVivoFreeFormStackMax() {
        return this.isFreeFormMax;
    }

    public boolean isInDirectFreeformState() {
        return this.mIsInDirectFreeformState;
    }

    public boolean isFreeFormMin() {
        return this.isFreeFormMin;
    }

    public void setFreeFormMin(boolean freeFormMin) {
        this.isFreeFormMin = freeFormMin;
    }

    public boolean isFreeformAnimating() {
        return this.isFreeformAnimating;
    }

    public void setFreeformAnimating(boolean freeformAnimating) {
        this.isFreeformAnimating = freeformAnimating;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getCurFocusWindowVisibleFrame() {
        WindowState topFullscreenOpaqueWindow;
        int navHeight;
        ActivityRecord focusedApp = this.mWmService.mRoot.getTopFocusedApp();
        if (focusedApp == null || focusedApp.findMainWindow() == null || focusedApp.getTask() == null) {
            VSlog.e(TAG, "getCurFocusWindowVisibleFrame mCurrentFocus is null");
            return null;
        }
        ActivityRecord topFullscreenActivity = focusedApp.getTask().getTopFullscreenActivity();
        if (topFullscreenActivity != null) {
            topFullscreenOpaqueWindow = topFullscreenActivity.getTopFullscreenOpaqueWindow();
        } else {
            topFullscreenOpaqueWindow = null;
        }
        if (!focusedApp.getTask().inFreeformWindowingMode() || topFullscreenOpaqueWindow == null) {
            return null;
        }
        this.isAdjustedForLeftNavBar = false;
        Rect frame = new Rect();
        frame.set(topFullscreenOpaqueWindow.getVisibleFrameLw());
        DisplayContent ds = this.mWmService.mRoot.getTopFocusedDisplayContent();
        if (ds != null) {
            Rect content = ds.mDisplayFrames.mContent;
            int rotation = ds.getRotation();
            DisplayPolicy dp = ds.getDisplayPolicy();
            if (rotation == 3 && dp.mNavigationBar != null && dp.mNavigationBar.isVisibleLw() && dp.mNavigationBarPosition == 1 && content.left == (navHeight = dp.getNavigationBarHeight(rotation, this.mWmService.mPolicy.getUiMode()))) {
                frame.set(frame.left - navHeight, frame.top, frame.right - navHeight, frame.bottom);
                this.isAdjustedForLeftNavBar = true;
            }
        }
        scaleFreeformBack(frame);
        return frame;
    }

    public void scaleFreeformBack(Rect frame) {
        int heigthScaledBack = (int) ((frame.height() * this.mFreeformScale) + 0.5f);
        int widthScaledBack = (int) ((frame.width() * this.mFreeformScale) + 0.5f);
        frame.set(frame.left, frame.top, frame.left + widthScaledBack, frame.top + heigthScaledBack);
    }

    public void scaleFreeformToReal(Rect frame) {
        int scaledHeightToReal = (int) ((frame.height() / this.mFreeformScale) + 0.5f);
        int scaledWidthToReal = (int) ((frame.width() / this.mFreeformScale) + 0.5f);
        frame.set(frame.left, frame.top, frame.left + scaledWidthToReal, frame.top + scaledHeightToReal);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getIsAdjustedForLeftNavBar() {
        return this.isAdjustedForLeftNavBar;
    }

    public boolean isNavigationbarVisible() {
        DisplayContent focusedDisplayContent = this.mWmService.mRoot.getTopFocusedDisplayContent();
        DisplayPolicy displayPolicy = null;
        if (focusedDisplayContent != null) {
            displayPolicy = focusedDisplayContent.getDisplayPolicy();
        }
        return (displayPolicy == null || displayPolicy.mNavigationBar == null || !displayPolicy.mNavigationBar.isVisibleLw()) ? false : true;
    }

    public void updateFreeformTaskSnapshot(Task freeformTaskRecord) {
    }

    public int getPortraitLimitedTop() {
        DisplayContent ds = this.mWmService.getDefaultDisplayContentLocked();
        Rect content = ds.mDisplayFrames.mContent;
        DisplayMetrics displayMetrics = ds.getDisplayMetrics();
        int minVisibleHeight = WindowManagerService.dipToPixel(80, displayMetrics);
        return content.bottom - minVisibleHeight;
    }

    public int getLandLimitedLeft(int rotation) {
        DisplayContent dc = this.mWmService.getDefaultDisplayContentLocked();
        Rect content = dc.mDisplayFrames.mStable;
        int limitedLeft = content.right;
        DisplayMetrics displayMetrics = dc.getDisplayMetrics();
        int minVisibleWidth = WindowManagerService.dipToPixel(80, displayMetrics);
        if (rotation == 1) {
            if (this.mWmService.isNavigationBarGestureOff()) {
                return content.right - minVisibleWidth;
            }
            if (dc.mDisplayFrames.mDisplayCutoutSafe.left != Integer.MIN_VALUE) {
                return (content.right - dc.mDisplayFrames.mDisplayCutoutSafe.left) - minVisibleWidth;
            }
            return limitedLeft;
        } else if (rotation == 3) {
            if (this.mWmService.isNavigationBarGestureOff()) {
                return (content.right - content.left) - minVisibleWidth;
            }
            if (dc.mDisplayFrames.mDisplayCutoutSafe.right != Integer.MAX_VALUE) {
                return dc.mDisplayFrames.mDisplayCutoutSafe.right - minVisibleWidth;
            }
            return limitedLeft;
        } else {
            return limitedLeft;
        }
    }

    public boolean isImeTarget(IBinder appToken, DisplayContent displayContent) {
        if (displayContent == null) {
            return false;
        }
        ActivityRecord imeTargetActivity = displayContent.mInputMethodTarget != null ? displayContent.mInputMethodTarget.mActivityRecord : null;
        return (imeTargetActivity == null || imeTargetActivity.appToken == null || imeTargetActivity.appToken.asBinder() != appToken || displayContent.mInputMethodWindow == null || !displayContent.mInputMethodWindow.isVisibleLw()) ? false : true;
    }

    public boolean isNavigationBarGestureOff() {
        try {
            boolean isNavGestureOff = Settings.Secure.getInt(this.mWmService.mContext.getContentResolver(), VivoRatioControllerUtilsImpl.NAVIGATION_GESTURE_ON) == 0;
            return isNavGestureOff;
        } catch (Exception e) {
            VSlog.w(TAG, "Get navigation bar settings error : SettingNotFoundException");
            return true;
        }
    }

    public boolean isNavigationBarLandRight() {
        try {
            boolean isNavBarLandRight = Settings.Secure.getInt(this.mWmService.mContext.getContentResolver(), "nav_bar_landscape_position") == 1;
            return isNavBarLandRight;
        } catch (Exception e) {
            VSlog.w(TAG, "Get navigation bar position error : SettingNotFoundException");
            return true;
        }
    }

    public void sendResizeFreeformTaskForIme(Rect rect, int taskId) {
        VSlog.d(TAG, "sendResizeFreeformTaskForIme taskId:" + taskId + " rect:" + rect);
        this.mWmService.getHandler().obtainMessage(105, taskId, 1, rect).sendToTarget();
    }

    public void setFreeformRotateWhenResize() {
        Settings.System.putIntForUser(this.mWmService.mContext.getContentResolver(), "freeform_rotate_when_resize", 1, this.mWmService.mCurrentUserId);
    }

    public void setFreeformAdjustForImeWhenResize() {
        Settings.System.putIntForUser(this.mWmService.mContext.getContentResolver(), "freeform_adjustforime_when_resize", 1, this.mWmService.mCurrentUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFreeformMovedToSecondDisplay() {
        this.isMovingFreeformTaskToSecondDisplay = true;
        Settings.System.putIntForUser(this.mWmService.mContext.getContentResolver(), "freeform_moved_to_second_display", 1, this.mWmService.mCurrentUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setStackUnderFreeformFocusedIfNeeded() {
        DisplayContent dc = this.mWmService.mRoot.getTopFocusedDisplayContent();
        WindowState focusWin = this.mWmService.mRoot.getTopFocusedDisplayContent().mCurrentFocus;
        if (dc != null && focusWin != null && focusWin.inFreeformWindowingMode() && focusWin.isVisibleNow()) {
            ActivityStack topFullcreenStack = dc.getStack(1, 0);
            Task topFullscreenTask = topFullcreenStack != null ? topFullcreenStack.getTopMostTask() : null;
            if (topFullscreenTask != null) {
                try {
                    this.mWmService.mActivityTaskManager.setFocusedTask(topFullscreenTask.mTaskId);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public boolean isScreenRotating(DisplayContent displayContent) {
        ScreenRotationAnimation screenRotationAnimation = displayContent != null ? displayContent.getRotationAnimation() : null;
        return screenRotationAnimation != null && screenRotationAnimation.isAnimating();
    }

    public void notifyFreeFormStackMaxChanged(boolean fullScreen) {
        this.isFreeFormMax = fullScreen;
    }

    public boolean isFreeFormResizing() {
        return this.isFreeFormResizing;
    }

    public void setFreeFormResizing(boolean resize) {
        this.isFreeFormResizing = resize;
    }

    public void freezingMultiWindow(int during) {
        this.mWmService.startFreezingScreen(0, 0);
        this.mWmService.mH.postDelayed(this.mStopFreezingRunnable, SystemProperties.getInt("persist.debug.freeze.timeout", during));
    }

    public int getSensorOrientation(int displayId) {
        DisplayContent displayContent = this.mWmService.mRoot.getDisplayContent(displayId);
        DisplayRotation displayRotation = null;
        if (displayContent != null) {
            displayRotation = displayContent.getDisplayRotation();
        }
        if (displayRotation != null) {
            return displayRotation.getSensorOrientation();
        }
        return this.mWmService.getDefaultDisplayContentLocked().getDisplayRotation().getSensorOrientation();
    }

    public void getCurLogicalDisplayRect(Rect out, int displayId) {
        DisplayContent displayContent = this.mWmService.mRoot.getDisplayContent(displayId);
        if (displayContent != null) {
            displayContent.getBounds(out);
        }
    }

    public boolean isTopFullscreenIsTranslucency() {
        return this.mTopFullscreenIsTranslucency;
    }

    public void setTopFullscreenIsTranslucency(boolean topFullscreenIsTranslucency) {
        this.mTopFullscreenIsTranslucency = topFullscreenIsTranslucency;
    }

    public void setIsFreeformExitToTop(boolean toTop) {
        this.isFreeformExitToTop = toTop;
    }

    public boolean ignoreStopFreezingWhenHaveChangingApps(DisplayContent displayContent) {
        int numChangingApps = displayContent != null ? displayContent.mChangingContainers.size() : 0;
        if (isVivoFreeformFeatureSupport() && this.isFreeformExitToTop && isScreenRotating(displayContent) && numChangingApps > 0) {
            return true;
        }
        if (this.isFreeformExitToTop) {
            this.isFreeformExitToTop = false;
        }
        return false;
    }

    public void limitWindowDragBoundsForVivoFreeform(Task task, Rect windowDragBounds, InputWindowHandle dragWindowHandle, int minWidth, int minHeight) {
        if (isVivoFreeFormValid() && isInVivoFreeform()) {
            scaleFreeformBack(windowDragBounds);
            int left = windowDragBounds.left;
            int top = windowDragBounds.top;
            int right = windowDragBounds.right;
            int bottom = windowDragBounds.bottom;
            if (left > dragWindowHandle.frameRight - minWidth) {
                left = dragWindowHandle.frameRight - minWidth;
                right = left + windowDragBounds.width();
            }
            if (top < 0) {
                top = 0;
                bottom = 0 + windowDragBounds.height();
            }
            if (right < minWidth) {
                right = minWidth;
                left = right - windowDragBounds.width();
            }
            if (top > dragWindowHandle.frameBottom - minHeight) {
                top = dragWindowHandle.frameBottom - minHeight;
                bottom = top + windowDragBounds.height();
            }
            DisplayContent ds = task.getDisplayContent();
            int rot = ds != null ? ds.getRotation() : -1;
            if (rot == 0 && top > getPortraitLimitedTop() && getPortraitLimitedTop() != -1) {
                top = getPortraitLimitedTop();
                bottom = top + windowDragBounds.height();
            } else if ((rot == 1 || rot == 3) && left > getLandLimitedLeft(rot) && getLandLimitedLeft(rot) != -1) {
                left = getLandLimitedLeft(rot);
                right = left + windowDragBounds.width();
            }
            windowDragBounds.set(left, top, right, bottom);
            scaleFreeformToReal(windowDragBounds);
            if (ds != null) {
                ds.adjustDragBoundsForIme(task, windowDragBounds);
            }
        }
    }

    public void VCD_FF_MOVE(WindowState windowState) {
        if (isVivoFreeFormValid() && isInVivoFreeform() && windowState.inFreeformWindowingMode()) {
            VCD_FF_1.VCD_FF_5(this.mWmService.mContext, windowState, isInDirectFreeformState());
        }
    }

    public boolean isFreeformMiniStateChanged() {
        return this.isFreeformMiniStateChanged;
    }

    public void setFreeformMiniStateChanged(boolean freeformMiniStateChanged) {
        this.isFreeformMiniStateChanged = freeformMiniStateChanged;
    }

    public void miniMizeFreeformWhenShowSoftInputIfNeed(IBinder focusWin) {
        WindowState inputFocusWin;
        if (this.mWmService.isVivoFreeFormValid() && this.mWmService.isInVivoFreeform() && !isInDirectFreeformState() && !this.mWmService.isFreeFormMin() && !this.mWmService.isVivoFreeFormStackMax() && (inputFocusWin = (WindowState) this.mWmService.mWindowMap.get(focusWin)) != null && !inputFocusWin.inFreeformWindowingMode()) {
            try {
                this.mWmService.mActivityTaskManager.miniMizeWindowVivoFreeformMode((IBinder) null, true);
            } catch (RemoteException | IllegalStateException e) {
                VSlog.e(TAG, "VivoFreeformWindowManger miniMizeFreeformWhenShowSoftInputIfNeed e:" + e.getMessage());
            }
        }
    }

    public float getFreeformScale() {
        return this.mFreeformScale;
    }

    public void setFreeformScale(float freeformScale) {
        this.mLastFreeformScale = this.mFreeformScale;
        this.mFreeformScale = freeformScale;
    }

    public float getLastFreeformScale() {
        return this.mLastFreeformScale;
    }

    public boolean ignoreRemoveChangingContainersForVivoFreeform(WindowContainer windowContainer, DisplayContent dc) {
        if (this.mWmService.isVivoFreeFormValid() && windowContainer != null && windowContainer.inFreeformWindowingMode() && windowContainer.getDisplayContent() != null && windowContainer.getDisplayContent() == dc && windowContainer.getDisplayContent().mChangingContainers.contains(windowContainer)) {
            return true;
        }
        return false;
    }

    public void registerFreeformPointEventListener(DisplayContent displayContent) {
        VivoFreeformGesturesPointerEventListener vivoFreeformGesturesPointerEventListener = this.mGesturesPointerEventListener;
        if (vivoFreeformGesturesPointerEventListener == null || displayContent == null) {
            return;
        }
        synchronized (vivoFreeformGesturesPointerEventListener) {
            try {
                VSlog.d(TAG, "registerFreeformPointEventListener displayContent id:" + displayContent.getDisplayId());
                unRegisterFreeformPointEventListener(displayContent);
                displayContent.registerPointerEventListener(this.mGesturesPointerEventListener);
            } catch (Exception e) {
                VSlog.e(TAG, "registerFreeformPointEventListener failed ! e : " + e.getMessage());
            }
        }
    }

    public void unRegisterFreeformPointEventListener(DisplayContent displayContent) {
        VivoFreeformGesturesPointerEventListener vivoFreeformGesturesPointerEventListener;
        if (displayContent == null || (vivoFreeformGesturesPointerEventListener = this.mGesturesPointerEventListener) == null) {
            return;
        }
        synchronized (vivoFreeformGesturesPointerEventListener) {
            try {
                VSlog.d(TAG, "unRegisterFreeformPointEventListener displayContent id:" + displayContent.getDisplayId());
                displayContent.unregisterPointerEventListener(this.mGesturesPointerEventListener);
            } catch (Exception e) {
                VSlog.e(TAG, "unRegisterFreeformPointEventListener failed ! e : " + e.getMessage());
            }
        }
    }
}