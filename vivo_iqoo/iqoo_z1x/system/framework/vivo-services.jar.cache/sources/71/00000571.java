package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.res.Configuration;
import android.multidisplay.MultiDisplayManager;
import com.android.server.wm.ActivityStack;
import com.android.server.wm.LaunchParamsController;
import com.vivo.appshare.AppShareConfig;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.display.SceneManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoRootWindowContainerImpl implements IVivoRootWindowContainer {
    private RootWindowContainer mRoot;
    private WindowManagerService mWmService;
    private String TAG = "VivoRootWindowContainerImpl";
    private ActivityRecord mWindowTranslucencyChangedActivity = null;
    private boolean mSendProcessChangedForTranslucencyActivity = false;
    private Session mAppShareHoldScreen = null;
    private WindowState mAppShareHoldScreenWindow = null;
    private int mGlobalCarDirection = 0;

    public VivoRootWindowContainerImpl(RootWindowContainer root, WindowManagerService wms) {
        this.mRoot = null;
        this.mWmService = null;
        this.mRoot = root;
        this.mWmService = wms;
    }

    public void dummy() {
        String str = this.TAG;
        VSlog.i(str, "dummy, this=" + this);
    }

    public boolean hasVivoFreeformStack() {
        for (int i = this.mRoot.mChildren.size() - 1; i >= 0; i--) {
            DisplayContent displayContent = (DisplayContent) this.mRoot.mChildren.get(i);
            if (displayContent != null && displayContent.getDefaultTaskDisplayArea().hasVivoFreeformStack()) {
                return true;
            }
        }
        return false;
    }

    public ActivityStack getVivoFreeformStack() {
        for (int i = this.mRoot.mChildren.size() - 1; i >= 0; i--) {
            DisplayContent displayContent = (DisplayContent) this.mRoot.mChildren.get(i);
            if (displayContent != null && displayContent.getDefaultTaskDisplayArea().getVivoFreeformStack() != null) {
                return displayContent.getDefaultTaskDisplayArea().getVivoFreeformStack();
            }
        }
        return null;
    }

    public TaskDisplayArea changeTargetDisplayAreaForVivoFreeform(TaskDisplayArea taskDisplayArea, int displayId, ActivityOptions options) {
        if (this.mRoot.mService.isMultiDisplyPhone() && this.mWmService.isVivoFreeFormValid() && options != null && options.getLaunchWindowingMode() == 5) {
            if (taskDisplayArea != null && taskDisplayArea.getDisplayId() != 0) {
                return this.mRoot.getDefaultTaskDisplayArea();
            }
            if (taskDisplayArea == null && displayId != 0) {
                return this.mRoot.getDefaultTaskDisplayArea();
            }
            return taskDisplayArea;
        }
        return taskDisplayArea;
    }

    public TaskDisplayArea changeDisplayForVivoFreeform(TaskDisplayArea taskDisplayArea, ActivityOptions options) {
        if (this.mRoot.mService.isMultiDisplyPhone() && this.mWmService.isVivoFreeFormValid() && taskDisplayArea != null && taskDisplayArea.getDisplayId() != 0 && options != null && options.getLaunchWindowingMode() == 5) {
            return null;
        }
        return taskDisplayArea;
    }

    public ActivityRecord getWindowTranslucencyChangedActivity() {
        return this.mWindowTranslucencyChangedActivity;
    }

    public void setWindowTranslucencyChangedActivity(ActivityRecord windowTranslucencyChangedActivity) {
        this.mWindowTranslucencyChangedActivity = windowTranslucencyChangedActivity;
    }

    public boolean needSendProcessChangedForTranslucencyActivity() {
        return this.mSendProcessChangedForTranslucencyActivity;
    }

    public void setSendProcessChangedForTranslucencyActivity(boolean sendProcessChangedForTranslucencyActivity) {
        this.mSendProcessChangedForTranslucencyActivity = sendProcessChangedForTranslucencyActivity;
    }

    public void exitFreeformWhenStartHomeActivity(TaskDisplayArea taskDisplayArea) {
        ActivityStack freeformStack;
        if (this.mRoot.mService.isVivoFreeFormValid() && this.mRoot.mService.isInVivoFreeform() && !this.mRoot.mService.isInDirectFreeformState() && (freeformStack = this.mRoot.getVivoFreeformStack()) != null && freeformStack.getDisplayArea() == taskDisplayArea) {
            taskDisplayArea.moveVivoFreeformTasksToFullscreenStackLocked(false);
        }
    }

    public boolean exitFreeformWhenResumeHome(TaskDisplayArea taskDisplayArea) {
        ActivityStack stack = this.mRoot.getVivoFreeformStack();
        if (this.mRoot.mService.isVivoFreeFormValid() && this.mRoot.mService.isInVivoFreeform() && stack != null && stack.getDisplayArea() != null && stack.getDisplayArea() == taskDisplayArea && this.mRoot.getTopDisplayFocusedStack() == stack && !this.mRoot.mService.isInDirectFreeformState()) {
            stack.getDisplayArea().moveVivoFreeformTasksToFullscreenStackLocked(false);
            return true;
        }
        return false;
    }

    public void exitFreeformWhenMoveHomeToTop(TaskDisplayArea taskDisplayArea) {
        ActivityStack stack = this.mRoot.getVivoFreeformStack();
        if (this.mRoot.mService.isVivoFreeFormValid() && this.mRoot.mService.isInVivoFreeform() && stack != null && stack.getDisplayArea() != null && stack.getDisplayArea() == taskDisplayArea && !this.mRoot.mService.isInDirectFreeformState() && stack.getDisplayId() == this.mRoot.mService.getFocusedDisplayId()) {
            stack.getDisplayArea().moveVivoFreeformTasksToFullscreenStackLocked(false);
        }
    }

    public void resumeFreeformStackAgain(ActivityRecord target, ActivityStack targetStack, ActivityOptions targetOptions) {
        if (this.mRoot.mService.isVivoFreeFormValid() && this.mRoot.mService.isInVivoFreeform() && target != null && !this.mRoot.mService.isFreeFormMin()) {
            ActivityStack stack = target.getStack();
            ActivityRecord topRunning = stack != null ? stack.topRunningActivityLocked() : null;
            if (topRunning != null && stack != targetStack && stack.inFreeformWindowingMode() && target.getState() == ActivityStack.ActivityState.DESTROYING && topRunning.isVisible() && topRunning.getState() == ActivityStack.ActivityState.STOPPED && stack.shouldBeVisible((ActivityRecord) null) && targetStack.mResumedActivity != null) {
                VSlog.i(this.TAG, "Resume freeform stack again");
                stack.resumeTopActivityUncheckedLocked(target, targetOptions);
            }
        }
    }

    public boolean skipExecutTransitionInFreeform() {
        if (this.mRoot.mService.isVivoFreeFormValid() && this.mRoot.mService.isInVivoFreeform() && this.mRoot.mService.getFreeformKeepR() != null) {
            return true;
        }
        return false;
    }

    public void setCheckFocusIfNeed(ActivityStack currentFocus, TaskDisplayArea display) {
        if (currentFocus != null && display != null && this.mRoot.mService.isVivoFreeFormValid() && this.mRoot.mService.isInVivoFreeform() && !this.mRoot.mService.isFreeFormStackMax() && currentFocus.inFreeformWindowingMode() && currentFocus.isFocusableAndVisible()) {
            display.setCheckFocus(true);
        }
    }

    public boolean skipReuseStackInVivoFreeform(ActivityOptions options) {
        if (!this.mRoot.mService.isVivoFreeFormValid() || options == null || options.getLaunchWindowingMode() != 5) {
            return false;
        }
        return true;
    }

    public boolean isTopFocusedDisplay(DisplayContent displayContent) {
        DisplayContent focusedDisplayContent;
        return (displayContent == null || (focusedDisplayContent = this.mRoot.getTopFocusedDisplayContent()) == null || displayContent != focusedDisplayContent) ? false : true;
    }

    public DisplayContent getTopFocusedDisplay() {
        DisplayContent focusedDisplayContent = this.mRoot.getTopFocusedDisplayContent();
        return focusedDisplayContent;
    }

    public ActivityStack getTopFocusedDisplayStack(int windowingMode, int activityType) {
        DisplayContent displayContent = getTopFocusedDisplay();
        if (displayContent != null) {
            ActivityStack stack = displayContent.getStack(windowingMode, activityType);
            return stack;
        }
        return null;
    }

    public void setDisplayOverrideConfigInFreeform(Configuration newConfiguration, DisplayContent displayContent, Configuration currentConfig, boolean configChanged) {
        if (displayContent == null || displayContent.getDisplayRotation() == null) {
            return;
        }
        String configdiff = Configuration.configurationDiffToString(currentConfig.diff(newConfiguration));
        if (displayContent.getDisplayRotation().isRotationChanging() && configChanged && !configdiff.contains("CONFIG_ORIENTATION")) {
            displayContent.getDisplayRotation().setRotationChanging(false);
            if (this.mRoot.mWmService.isVivoFreeFormValid() && this.mRoot.mWmService.isInVivoFreeform() && this.mRoot.mWmService.isFreeFormResizing() && !this.mRoot.mWmService.isVivoFreeFormStackMax()) {
                this.mRoot.mWmService.setFreeformRotateWhenResize();
            }
        }
        if (displayContent.getDisplayRotation().isRotationChanging()) {
            boolean hasSetCheckFocus = displayContent.getDefaultTaskDisplayArea().hasSetCheckFocus();
            ActivityStack freeformStack = this.mRoot.getVivoFreeformStack();
            if (this.mRoot.mWmService.isVivoFreeFormValid() && this.mRoot.mWmService.isInVivoFreeform() && !hasSetCheckFocus && freeformStack != null && freeformStack.getDisplay() == displayContent) {
                this.mRoot.mWmService.setStackUnderFreeformFocusedIfNeeded();
            }
            displayContent.getDisplayRotation().setRotationChanging(false);
        }
    }

    public ActivityRecord getTopFocusedApp() {
        DisplayContent focusedDisplayContent = this.mRoot.getTopFocusedDisplayContent();
        if (focusedDisplayContent != null) {
            return focusedDisplayContent.mFocusedApp;
        }
        return null;
    }

    public boolean isTopFocusedDisplayNavBarVisible() {
        DisplayContent focusedDisplayContent = this.mRoot.getTopFocusedDisplayContent();
        DisplayPolicy displayPolicy = null;
        if (focusedDisplayContent != null) {
            displayPolicy = focusedDisplayContent.getDisplayPolicy();
        }
        if (displayPolicy != null) {
            return displayPolicy.isNavigationbarVisible();
        }
        return false;
    }

    public void checkRecentTaskIfNeedChangeWinModeWhenStart(Task task) {
        if (this.mWmService.isVivoFreeFormValid() && this.mWmService.isInVivoFreeform() && this.mRoot.getVivoFreeformStack() != null && task != null && task.inFreeformWindowingMode() && task != this.mRoot.getVivoFreeformStack()) {
            task.setWindowingMode(1);
        }
    }

    public boolean skipReuseStackWhenPrimaryToSecondary(ActivityOptions options, Task candidateStack) {
        if (this.mWmService.mAtmService.isMultiWindowSupport() && this.mWmService.mAtmService.isSplittingScreenByVivo() && options != null && candidateStack != null && options.getLaunchWindowingMode() == 4 && candidateStack.getWindowingMode() == 3) {
            String str = this.TAG;
            VSlog.i(str, "skipReuseStackWhenPrimaryToSecondary, candidateStack=" + candidateStack + " options:" + options);
            return true;
        }
        return false;
    }

    public boolean skipReuseStackWhenLuanchToPrimary(ActivityOptions options, Task candidateStack) {
        if (this.mWmService.mAtmService.isMultiWindowSupport() && this.mWmService.mAtmService.isSplittingScreenByVivo() && options != null && candidateStack != null) {
            if ((options.getLaunchWindowingMode() == 3 && candidateStack.getWindowingMode() == 4) || (options.getLaunchWindowingMode() == 4 && candidateStack.getWindowingMode() == 3)) {
                String str = this.TAG;
                VSlog.i(str, "skipReuseStackWhenLuanchToPrimary, candidateStack=" + candidateStack + " options:" + options);
                return true;
            }
            return false;
        }
        return false;
    }

    public void attachApplication(WindowProcessController app, boolean didSomething, boolean exitPreload) {
        if (exitPreload && app != null && app.mInfo != null) {
            RmsInjectorImpl.getInstance().setRmsPreload(app.mInfo.packageName, app.mUid, false, false);
        }
    }

    public void leaveCarFocusedMode() {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING || this.mGlobalCarDirection == 0 || !MultiDisplayManager.isVCarDisplayRunning()) {
            return;
        }
        this.mGlobalCarDirection = 0;
        final DisplayContent dc = this.mRoot.getDisplayContent((int) SceneManager.APP_REQUEST_PRIORITY);
        if (dc != null) {
            dc.shouldUpdateWindowForVCar(0);
            this.mWmService.mH.post(new Runnable() { // from class: com.android.server.wm.VivoRootWindowContainerImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    synchronized (VivoRootWindowContainerImpl.this.mWmService.mGlobalLock) {
                        if (VivoRootWindowContainerImpl.this.mRoot.mTopFocusedDisplayId != 80000) {
                            VSlog.d("VivoFocused", "leaveCarFocusedMode not in cardisplay!");
                            return;
                        }
                        dc.updateFocusedWindowLocked(0, true, -1, true);
                        WindowState newFocus = dc.mCurrentFocus;
                        if (newFocus != null) {
                            VSlog.d("VivoFocused", "find focusWindow: " + newFocus);
                            VivoRootWindowContainerImpl.this.mRoot.mTopFocusedAppByProcess.clear();
                            int pidOfNewFocus = newFocus.mSession.mPid;
                            if (VivoRootWindowContainerImpl.this.mRoot.mTopFocusedAppByProcess.get(Integer.valueOf(pidOfNewFocus)) == null) {
                                VivoRootWindowContainerImpl.this.mRoot.mTopFocusedAppByProcess.put(Integer.valueOf(pidOfNewFocus), newFocus.mActivityRecord);
                            }
                        }
                    }
                }
            });
        }
    }

    public boolean updateFocusedWindowForVCar(int direction) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && MultiDisplayManager.isVCarDisplayRunning()) {
            DisplayContent dc = this.mRoot.getDisplayContent((int) SceneManager.APP_REQUEST_PRIORITY);
            if (dc == null) {
                VSlog.d("VivoFocused", "updateFocusedWindow dc is null!");
                return false;
            }
            this.mGlobalCarDirection = direction;
            if (dc.shouldUpdateWindowForVCar(direction)) {
                if (this.mRoot.mTopFocusedDisplayId != 80000) {
                    VSlog.d("VivoFocused", "updateFocusedWindow not in cardisplay!");
                    return false;
                }
                boolean changed = dc.updateFocusedWindowLocked(0, true, -1, true);
                WindowState newFocus = dc.mCurrentFocus;
                if (newFocus != null) {
                    this.mRoot.mTopFocusedAppByProcess.clear();
                    int pidOfNewFocus = newFocus.mSession.mPid;
                    if (this.mRoot.mTopFocusedAppByProcess.get(Integer.valueOf(pidOfNewFocus)) == null) {
                        this.mRoot.mTopFocusedAppByProcess.put(Integer.valueOf(pidOfNewFocus), newFocus.mActivityRecord);
                    }
                }
                return changed;
            }
            return false;
        }
        return false;
    }

    public void anyTaskForVirtualDisplay(ActivityStack launchStack, ActivityOptions aOptions) {
        DisplayContent displayContent;
        TaskDisplayArea taskDisplayArea;
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return;
        }
        int historyId = launchStack != null ? launchStack.getDisplayId() : 0;
        int preferredDisplayId = aOptions.getLaunchDisplayId() == -1 ? 0 : aOptions.getLaunchDisplayId();
        if (MultiDisplayManager.DEBUG) {
            VSlog.d("VivoCar", "anyTaskForVirtualDisplay launchStack: " + launchStack + " ,preferredDisplayId: " + preferredDisplayId);
        }
        if (historyId != preferredDisplayId && MultiDisplayManager.isVivoDisplay(historyId) && historyId != 80000 && (displayContent = this.mRoot.getDisplayContent(preferredDisplayId)) != null && (taskDisplayArea = displayContent.getDefaultTaskDisplayArea()) != null) {
            ActivityRecord top = launchStack.topRunningActivity();
            boolean fromResume = top != null ? top.isState(ActivityStack.ActivityState.RESUMED) : false;
            launchStack.reparent(taskDisplayArea, true);
            if (fromResume && MultiDisplayManager.isVivoDisplay(historyId) && preferredDisplayId == 0) {
                if (MultiDisplayManager.DEBUG) {
                    VSlog.d("VivoCar", "Move from resume-to-resume " + launchStack.topRunningActivity());
                }
                this.mRoot.mService.updateVivoActivityState(top, 0);
                this.mRoot.ensureVisibilityAndConfig(top, top.getDisplayId(), true, false);
            }
        }
    }

    public TaskDisplayArea updateForVirtualDisplay(LaunchParamsController.LaunchParams launchParams, TaskDisplayArea container, int displayId) {
        DisplayContent displayContent;
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return container;
        }
        int preferredDisplayId = -1;
        if (launchParams != null && launchParams.hasPreferredTaskDisplayArea()) {
            preferredDisplayId = launchParams.mPreferredTaskDisplayArea.getDisplayId();
        } else if (displayId != -1) {
            preferredDisplayId = displayId;
        }
        if (container == null && MultiDisplayManager.isVivoDisplay(preferredDisplayId) && (displayContent = this.mRoot.getDisplayContent(preferredDisplayId)) != null) {
            container = displayContent.getDefaultTaskDisplayArea();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Display#");
        sb.append(preferredDisplayId);
        sb.append(" ,container: ");
        sb.append(container != null ? Integer.valueOf(container.mDisplayContent.mDisplayId) : "NULL");
        VSlog.d("VivoCar", sb.toString());
        return container;
    }

    public boolean skipResumeAppShareDisplay(DisplayContent display) {
        return display != null && MultiDisplayManager.isAppShareDisplayId(display.mDisplayId) && display.isSleeping();
    }

    public boolean allowSetScreenBrightnessLocked(WindowState w) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            boolean isAppShareDisplayId = MultiDisplayManager.isAppShareDisplayId(w.getDisplayId());
            WindowState appShareWindow = this.mRoot.mWmService.getAppShareWindowLocked();
            boolean appShareHasSurface = appShareWindow != null ? appShareWindow.mHasSurface : false;
            boolean appShareCanBeSeen = appShareWindow != null ? appShareWindow.isDisplayedLw() : false;
            if (isAppShareDisplayId) {
                return appShareHasSurface && appShareCanBeSeen;
            }
            return true;
        }
        return true;
    }

    public void setAppShareHoldScreen(Session session) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            this.mAppShareHoldScreen = session;
        }
    }

    public void setAppShareHoldScreenWindow(WindowState w) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            this.mAppShareHoldScreenWindow = w;
        }
    }

    public Session getAppShareHoldScreen() {
        return this.mAppShareHoldScreen;
    }
}