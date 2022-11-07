package com.android.server.wm;

import android.app.ActivityManager;
import android.app.EventLogTags;
import android.content.Context;
import android.content.IClipboard;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.multidisplay.MultiDisplayManager;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArraySet;
import android.view.SurfaceControl;
import com.android.internal.os.PerfThread;
import com.android.server.wm.ActivityStack;
import com.google.android.collect.Sets;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.ProcessList;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoTaskDisplayAreaImpl implements IVivoTaskDisplayArea {
    static final String TAG = "VivoTaskDisplayAreaImpl";
    private Task mMiniLauncherTask;
    private Task mRootPrimaryTask;
    private Task mRootSecondaryTask;
    private TaskDisplayArea mTaskDisplayArea;
    private WindowManagerService mWmService;
    public static boolean mPerfSendTapHint = false;
    public static boolean mIsPerfBoostAcquired = false;
    public static int mPerfHandle = -1;
    private ActivityStack mVivoFreeformStack = null;
    private boolean mSetCheckFocus = false;
    public AbsVivoPerfManager mPerfBoost = null;
    public AbsVivoPerfManager mUxPerf = null;
    public AbsVivoPerfManager mPerfHome = null;
    boolean mSplitScreenDeathDismiss = false;
    private boolean isMultiWindowAfterEnteredJust = false;
    private volatile boolean isValidSplitMultiWindow = false;
    private ActivityStack rootSecondaryStack = null;
    private Runnable TransientMultiWindowEntered = new Runnable() { // from class: com.android.server.wm.VivoTaskDisplayAreaImpl.2
        @Override // java.lang.Runnable
        public void run() {
            VivoTaskDisplayAreaImpl.this.isMultiWindowAfterEnteredJust = false;
        }
    };
    final int mIdent = 100;
    final String MultiWindow_TAG = "SplitMultiwindow";
    private boolean moveHomeToFrontWhenDissmissSplit = false;

    public VivoTaskDisplayAreaImpl(TaskDisplayArea taskDisplayArea, WindowManagerService wmService) {
        this.mTaskDisplayArea = taskDisplayArea;
        this.mWmService = wmService;
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public ActivityStack getVivoFreeformStack() {
        return this.mVivoFreeformStack;
    }

    public void setVivoFreeformStack(ActivityStack freeformStack) {
        this.mVivoFreeformStack = freeformStack;
    }

    public boolean hasVivoFreeformStack() {
        return this.mVivoFreeformStack != null;
    }

    public boolean isVivoFreeformStack(int windowingMode) {
        return this.mWmService.isVivoFreeFormValid() && windowingMode == 5;
    }

    public boolean isVivoFreeformStack(ActivityStack stack) {
        return this.mWmService.isVivoFreeformFeatureSupport() && stack == getVivoFreeformStack();
    }

    public void setFreeformSettingsState(Task launchRootTask, int windowingMode, int activityType, int value) {
        if (this.mWmService.isVivoFreeFormValid() && windowingMode == 5) {
            if (activityType == 1 && launchRootTask == null) {
                try {
                    int currentUserId = this.mWmService.mAtmService.mAmInternal.getCurrentUserId();
                    Settings.System.putIntForUser(this.mWmService.mAtmService.mContext.getContentResolver(), "smartmultiwindow_freeform", value, currentUserId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ActivityStack fullscreenStack = this.mTaskDisplayArea.getStack(1, 1);
                if (fullscreenStack != null && !this.mWmService.isInDirectFreeformState()) {
                    ActivityRecord topRun = fullscreenStack.topRunningActivityLocked();
                    boolean behindPasswd = false;
                    if (topRun != null && topRun.isPasswordActivity()) {
                        int indexBehind = this.mTaskDisplayArea.getIndexOf(fullscreenStack) - 1;
                        ActivityStack stackBehind = indexBehind >= 0 ? this.mTaskDisplayArea.getStackAt(indexBehind) : null;
                        if (stackBehind != null && stackBehind.getWindowingMode() == 1 && (topRun = (fullscreenStack = stackBehind).topRunningActivityLocked()) != null && topRun.getState() == ActivityStack.ActivityState.STOPPED) {
                            behindPasswd = true;
                        }
                    }
                    if (topRun != null && ((topRun.nowVisible || behindPasswd) && !topRun.occludesParent() && fullscreenStack.isTranslucent((ActivityRecord) null) && !topRun.isEmergentActivity())) {
                        topRun.setOccludesParent(true);
                        this.mWmService.mRoot.setWindowTranslucencyChangedActivity(topRun);
                    }
                }
                VivoStatsInServerImpl.setWhichPIP(value == 1);
            }
        }
    }

    public void initFreeformStatusIfNeed(ActivityStack stack, int windowingMode, boolean create) {
        if (this.mWmService.isVivoFreeFormValid() && windowingMode == 5) {
            stack.setAlwaysOnTop(true);
            if (create) {
                EventLogTags.writeWmTaskRemoved(stack.getRootTaskId(), "freeform stack created");
            } else {
                EventLogTags.writeWmTaskRemoved(stack.getRootTaskId(), "toggled to freeform stack");
            }
            new Thread(new Runnable() { // from class: com.android.server.wm.VivoTaskDisplayAreaImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    VCD_FF_1.VCD_FF_2(VivoTaskDisplayAreaImpl.this.mWmService.mContext);
                }
            }).start();
            this.mWmService.setEnteringFreeForm(true);
            Handler handler = this.mWmService.getHandler();
            if (handler != null) {
                Message msg = handler.obtainMessage(106);
                handler.sendMessageDelayed(msg, 300L);
            }
            this.mWmService.registerFreeformPointEventListener(stack.getDisplayContent());
        }
    }

    public boolean forceSupportInVivoFreeform(int windowingMode) {
        if (this.mWmService.isVivoFreeFormValid() && windowingMode == 5) {
            return true;
        }
        return false;
    }

    public boolean forceSupportInVivoFreeform() {
        if (this.mWmService.isVivoFreeFormValid()) {
            return true;
        }
        return false;
    }

    public int validateWindowingModeForVivoFreeform(ActivityRecord r, Task task, int windowingMode) {
        boolean supportsFreeform = this.mWmService.mAtmService.mSupportsFreeformWindowManagement;
        boolean supportsMultiWindow = this.mWmService.mAtmService.mSupportsMultiWindow;
        if (supportsMultiWindow && r != null) {
            supportsFreeform = r.supportsFreeform();
        }
        if (this.mWmService.isVivoFreeFormValid() && r != null && r.isPasswordActivity() && !supportsFreeform && windowingMode == 5) {
            return 1;
        }
        return windowingMode;
    }

    public int adjustWindowModeForFreeformInSecondDisplay(int windowingMode) {
        if (this.mWmService.mRoot.mService.isMultiDisplyPhone() && this.mWmService.isVivoFreeFormValid() && this.mWmService.isInVivoFreeform() && windowingMode == 5 && this.mTaskDisplayArea.getDisplayId() != 0) {
            return 0;
        }
        return windowingMode;
    }

    public void setCheckFocusInVivoFreeform(ActivityStack currentFocus, ActivityStack lastFocus) {
        Task lastTopTask;
        if (this.mWmService.isVivoFreeFormValid() && this.mWmService.isInVivoFreeform() && currentFocus != null && currentFocus.inFreeformWindowingMode()) {
            ActivityRecord lastTop = lastFocus != null ? lastFocus.topRunningActivityLocked() : null;
            if (lastTop == null && lastFocus != null && (lastTopTask = lastFocus.getTopMostTask()) != null && lastTopTask.getChildCount() >= 1) {
                lastTop = (ActivityRecord) lastTopTask.getChildAt(lastTopTask.getChildCount() - 1);
            }
            if (lastTop != null && lastTop.isPasswordActivity()) {
                setCheckFocus(true);
                this.mWmService.mRoot.mService.setIsUnlockingToFreeform(true);
            }
        }
    }

    public boolean hasSetCheckFocus() {
        return this.mSetCheckFocus;
    }

    public void setCheckFocus(boolean checkFocus) {
        this.mSetCheckFocus = checkFocus;
    }

    public boolean skipStackInVivoFreeform(int currentWindowingMode, ActivityStack stack) {
        if (!this.mWmService.isVivoFreeFormValid() || !this.mWmService.isInVivoFreeform() || !this.mWmService.isInDirectFreeformState() || currentWindowingMode != 1 || !stack.inFreeformWindowingMode()) {
            return false;
        }
        return true;
    }

    public void checkAndExitFreeformModeIfNeeded(String reason) {
        ActivityStack stack = this.mVivoFreeformStack;
        if (this.mWmService.isVivoFreeFormValid() && this.mWmService.isInVivoFreeform() && stack != null && stack.getDisplayId() == this.mTaskDisplayArea.getDisplayId()) {
            ActivityStack fullscreenStack = this.mTaskDisplayArea.getStack(1, 1);
            ActivityRecord fullscreenR = fullscreenStack != null ? fullscreenStack.topRunningActivityLocked() : null;
            Task top = stack.getTopMostTask();
            boolean isTopTaskFinishing = top != null && stack.isTopFinishingTask(top);
            boolean freeformFinishingOnFullsceenStack = ((stack.mResumedActivity != null && stack.mResumedActivity.finishing) || isTopTaskFinishing) && "finishActivity adjustFocus".equals(reason);
            freeformFinishingOnFullsceenStack = (fullscreenR == null || !fullscreenR.nowVisible) ? false : false;
            if (this.mTaskDisplayArea.getFocusedStack() == stack && (!this.mWmService.isInDirectFreeformState() || freeformFinishingOnFullsceenStack)) {
                moveFreeformTasksToFullscreenStackLocked(stack, false);
            } else if (this.mTaskDisplayArea.getFocusedStack() == stack && this.mWmService.isInDirectFreeformState() && isTopTaskFinishing && "finishActivity adjustFocus".equals(reason) && stack.mResumedActivity != null && this.mWmService.isImeTarget(stack.mResumedActivity.appToken, this.mTaskDisplayArea.getDisplayContent())) {
                this.mWmService.setIsDirectFreeformFinishingResizing(true);
                this.mWmService.mRoot.mService.setShortFreezingAnimaiton(stack.mResumedActivity.appToken);
            }
        }
    }

    public void moveVivoFreeformTasksToFullscreenStackLocked(boolean onTop) {
        ActivityStack activityStack = this.mVivoFreeformStack;
        if (activityStack != null) {
            moveFreeformTasksToFullscreenStackLocked(activityStack, onTop);
        }
    }

    private void moveFreeformTasksToFullscreenStackLocked(final ActivityStack fromStack, final boolean onTop) {
        if (fromStack == null || fromStack.getWindowingMode() != 5) {
            VSlog.e(TAG, "moveFreeformTasksToFullscreenStackLocked don't have freeform stack!");
        } else {
            this.mWmService.inSurfaceTransaction(new Runnable() { // from class: com.android.server.wm.-$$Lambda$VivoTaskDisplayAreaImpl$mBEhdVGdTX-iaAIjBuYduDtbp_Y
                @Override // java.lang.Runnable
                public final void run() {
                    VivoTaskDisplayAreaImpl.this.lambda$moveFreeformTasksToFullscreenStackLocked$0$VivoTaskDisplayAreaImpl(fromStack, onTop);
                }
            });
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public /* synthetic */ void lambda$moveFreeformTasksToFullscreenStackLocked$0$VivoTaskDisplayAreaImpl(ActivityStack fromStack, boolean onTop) {
        fromStack.cancelAnimation();
        if (!onTop) {
            if (this.mWmService.mTaskSnapshotController != null) {
                ArraySet<Task> tasks = Sets.newArraySet(new Task[]{fromStack});
                this.mWmService.mTaskSnapshotController.snapshotTasks(tasks);
                this.mWmService.mTaskSnapshotController.addSkipClosingAppSnapshotTasks(tasks);
            }
            fromStack.setForceHidden(1, true);
        }
        fromStack.ensureActivitiesVisible((ActivityRecord) null, 0, true);
        this.mWmService.mAtmService.mStackSupervisor.activityIdleInternal((ActivityRecord) null, false, true, (Configuration) null);
        this.mWmService.mAtmService.deferWindowLayout();
        try {
            fromStack.setWindowingMode(1);
            if (fromStack.getDisplayArea() != null) {
                if (onTop) {
                    fromStack.getDisplayArea().positionStackAtTop(fromStack, false);
                } else {
                    fromStack.getDisplayArea().positionStackAtBottom(fromStack);
                }
            }
            if (!onTop) {
                fromStack.setForceHidden(1, false);
            }
            this.mWmService.mRoot.ensureActivitiesVisible((ActivityRecord) null, 0, true);
            this.mWmService.mRoot.resumeFocusedStacksTopActivities();
        } finally {
            this.mWmService.mAtmService.continueWindowLayout();
        }
    }

    public void exitFreeformWhenHomeInFront() {
        if (this.mWmService.isVivoFreeFormValid() && this.mWmService.isInVivoFreeform() && this.mVivoFreeformStack != null && !this.mWmService.isInDirectFreeformState() && this.mVivoFreeformStack.getDisplayId() == this.mTaskDisplayArea.getDisplayId()) {
            moveFreeformTasksToFullscreenStackLocked(this.mVivoFreeformStack, false);
        }
    }

    public boolean isHomeStackVisible() {
        ActivityRecord homeActivity = this.mTaskDisplayArea.getHomeActivity();
        if (this.mTaskDisplayArea.getRootHomeTask() == null || homeActivity == null) {
            return false;
        }
        ActivityRecord top = this.mTaskDisplayArea.getRootHomeTask().getTopMostActivity();
        TaskDisplayArea taskDisplayArea = this.mTaskDisplayArea;
        ActivityStack topFullSrceenStack = taskDisplayArea != null ? taskDisplayArea.getTopStackInWindowingMode(1) : null;
        ActivityRecord topFullScreenAr = topFullSrceenStack != null ? topFullSrceenStack.topRunningActivityLocked() : null;
        if (topFullSrceenStack == null || !topFullSrceenStack.shouldBeVisible((ActivityRecord) null) || topFullScreenAr == null || !topFullScreenAr.equals(homeActivity)) {
            if (top != null) {
                if (top.isVisible() || top.mVisibleRequested) {
                    return homeActivity.isVisible() || homeActivity.mVisibleRequested;
                }
                return false;
            }
            return false;
        }
        return true;
    }

    public boolean adjustMaxPositionInVivoFreeform(ActivityStack curr, ActivityStack stack) {
        if (this.mWmService.mRoot.mService.isVivoFreeFormValid() && curr.inFreeformWindowingMode() && curr != stack) {
            if (this.mWmService.mRoot.mService.isFreeFormMin()) {
                return true;
            }
            Task top = stack.getTopMostTask();
            boolean moveEmergentActivity = (top != null && (top.isActivityTypeRecents() || top.isEmergentTask())) || this.mWmService.mRoot.mService.isStartingEmergent();
            if (moveEmergentActivity) {
                return true;
            }
            if (this.mWmService.mRoot.mService.isStartingEmergent()) {
                this.mWmService.mRoot.mService.setIsStartingEmergent(false);
            }
        }
        return false;
    }

    public boolean isVivoFreeformStackAndCanGoBottom(ActivityStack stack) {
        if (this.mWmService.mRoot.mService.isVivoFreeFormValid() && stack.inFreeformWindowingMode() && this.mWmService.mRoot.mService.isFreeFormMin()) {
            return true;
        }
        return false;
    }

    public void setCheckFocusInVivoFreeform(ActivityStack currentFocusedStack, String updateLastFocusedStackReason) {
        if (this.mWmService.mRoot.mService.isVivoFreeFormValid() && this.mWmService.mRoot.mService.isInVivoFreeform() && currentFocusedStack != null && currentFocusedStack.inFreeformWindowingMode()) {
            ActivityRecord lastTop = this.mTaskDisplayArea.mLastFocusedStack != null ? this.mTaskDisplayArea.mLastFocusedStack.topRunningActivityLocked() : null;
            ActivityRecord foucusTop = currentFocusedStack.getTopNonFinishingActivity();
            if (lastTop != null && (lastTop.isPasswordActivity() || (lastTop.getState() == ActivityStack.ActivityState.STOPPED && foucusTop != null && (foucusTop.getState() == ActivityStack.ActivityState.INITIALIZING || foucusTop.getState() == ActivityStack.ActivityState.STOPPED)))) {
                setCheckFocus(true);
            } else if ("setFocusDisplay".equals(updateLastFocusedStackReason) && !this.mWmService.mRoot.mService.isFreeFormStackMax()) {
                setCheckFocus(true);
            }
        }
    }

    public void positionPasswdStackToBottom() {
        ActivityStack freeformStack = this.mVivoFreeformStack;
        ActivityRecord freeformTopActivity = freeformStack != null ? freeformStack.topRunningActivityLocked() : null;
        if (freeformTopActivity != null && freeformTopActivity.isLaunchFromSoftware() && this.mTaskDisplayArea.getIndexOf(freeformStack) >= 1) {
            int indexBehind = this.mTaskDisplayArea.getIndexOf(freeformStack) - 1;
            ActivityStack stackBehindFreeFormStack = this.mTaskDisplayArea.getStackAt(indexBehind);
            ActivityRecord topActivity = stackBehindFreeFormStack.topRunningActivityLocked();
            if (topActivity != null && topActivity.isPasswordActivity()) {
                if (ActivityTaskManagerDebugConfig.DEBUG_STACK) {
                    VSlog.d(TAG, "positionChildAtTopInVivofreeform mStacks = " + this.mTaskDisplayArea.mChildren);
                }
                this.mTaskDisplayArea.positionStackAtBottom(stackBehindFreeFormStack);
            }
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_STACK) {
            VSlog.d(TAG, "positionChildAtTopInVivofreeform " + this.mTaskDisplayArea.mChildren + " " + Debug.getCallers(10));
        }
    }

    public void onSplitScreenModeDismissed(boolean death) {
        try {
            this.mSplitScreenDeathDismiss = death;
            this.mTaskDisplayArea.onSplitScreenModeDismissed((ActivityStack) null);
        } finally {
            this.mSplitScreenDeathDismiss = false;
        }
    }

    public boolean isSplitScreenDeathDismiss() {
        return this.mSplitScreenDeathDismiss;
    }

    public void enableTracksTransientMultiWindowEnter() {
        WindowManagerService windowManagerService = this.mWmService;
        Handler handler = windowManagerService != null ? windowManagerService.getHandler() : null;
        if (handler != null) {
            handler.removeCallbacks(this.TransientMultiWindowEntered);
            handler.postDelayed(this.TransientMultiWindowEntered, SystemProperties.getInt("persist.vivo.split_entered_transient_timeout", (int) ProcessList.HOME_APP_ADJ));
            this.isMultiWindowAfterEnteredJust = true;
        }
    }

    public void disableTracksTransientMultiWindowEnter() {
        WindowManagerService windowManagerService = this.mWmService;
        Handler handler = windowManagerService != null ? windowManagerService.getHandler() : null;
        if (handler != null) {
            handler.removeCallbacks(this.TransientMultiWindowEntered);
            this.isMultiWindowAfterEnteredJust = false;
        }
    }

    public boolean isMultiWindowEnterJustWithDisplay() {
        return this.isMultiWindowAfterEnteredJust;
    }

    public void onParentChangedOfChild(Task child, ConfigurationContainer newParent, ConfigurationContainer oldParent, boolean displayChanged) {
        TaskDisplayArea taskDisplayArea;
        if (displayChanged) {
            return;
        }
        parentSplitMessage(child, newParent, oldParent);
        if (newParent != null && newParent != oldParent) {
            if (((newParent instanceof ActivityStack) || (oldParent != null && (oldParent instanceof ActivityStack))) && child != null && child.isAttached() && (taskDisplayArea = this.mTaskDisplayArea) != null) {
                ActivityStack primaryStack = taskDisplayArea != null ? taskDisplayArea.getRootSplitScreenPrimaryTask() : null;
                boolean isEmptyOfSecondaryStack = true;
                if ((newParent instanceof ActivityStack) && !newParent.isActivityTypeHome() && newParent.inSplitScreenPrimaryWindowingMode()) {
                    ActivityStack parent = (ActivityStack) newParent;
                    if (primaryStack != null && parent == primaryStack && parent.getChildCount() == 1 && !child.inSplitScreenWindowingMode()) {
                        VSlog.i(TAG, this.mTaskDisplayArea + " really enter split multiwindow");
                        this.isValidSplitMultiWindow = true;
                        this.rootSecondaryStack = null;
                        onSplitScreenModeActivated();
                    }
                } else if (this.isValidSplitMultiWindow && oldParent != null && (oldParent instanceof ActivityStack) && oldParent.inSplitScreenWindowingMode()) {
                    ActivityStack oldParentTask = (ActivityStack) oldParent;
                    boolean isEmptyOfPrimaryStack = (primaryStack == null || primaryStack.hasChild()) ? false : true;
                    if (oldParentTask != null && oldParentTask.mCreatedByOrganizer && oldParentTask.inSplitScreenSecondaryWindowingMode()) {
                        this.rootSecondaryStack = oldParentTask;
                    }
                    ActivityStack activityStack = this.rootSecondaryStack;
                    if (activityStack == null || activityStack.hasChild()) {
                        isEmptyOfSecondaryStack = false;
                    }
                    if (child.inSplitScreenWindowingMode() && isEmptyOfSecondaryStack && isEmptyOfPrimaryStack && this.isValidSplitMultiWindow) {
                        VSlog.i(TAG, this.mTaskDisplayArea + " really drop out split multiwindow");
                        this.isValidSplitMultiWindow = false;
                        this.rootSecondaryStack = null;
                        onSplitScreenModeDismissed();
                    }
                }
            }
        }
    }

    private void onSplitScreenModeActivated() {
        EventLogTags.writeWmOnTopResumedGainedCalled(100, "SplitMultiwindow", "onSplitScreenModeActivated");
        debugSplitModeState("onSplitScreenModeActivated");
        enableTracksTransientMultiWindowEnter();
        if (this.mWmService.isRotationFrozen()) {
            this.mTaskDisplayArea.mDisplayContent.freezeCurrentRotationEnterSplit(this.mTaskDisplayArea.mDisplayContent.getRotation());
        }
        try {
            IClipboard clipboard = IClipboard.Stub.asInterface(ServiceManager.getService("clipboard"));
            if (clipboard != null && clipboard.isClipboardDialogShowing()) {
                clipboard.hideClipboardDialog();
            }
        } catch (RemoteException e) {
        }
        TaskDisplayArea taskDisplayArea = this.mTaskDisplayArea;
        DisplayContent content = taskDisplayArea != null ? taskDisplayArea.getDisplayContent() : null;
        if (content != null) {
            content.disableVivoTracksTransientMultiWindowExited();
            content.getDockedDividerController().notifyDockedStackExistsChanged(true);
        }
        this.mWmService.mH.removeMessages(70);
    }

    private void onSplitScreenModeDismissed() {
        EventLogTags.writeWmOnTopResumedLostCalled(100, "SplitMultiwindow", "onSplitScreenModeDismissed");
        debugSplitModeState("onSplitScreenModeDismissed");
        this.mWmService.mAtmService.setResizingJustAfterSplit(false);
        disableTracksTransientMultiWindowEnter();
        if (this.mWmService.isRotationFrozen()) {
            this.mTaskDisplayArea.mDisplayContent.freezeRotation0LeaveSplit();
        }
        TaskDisplayArea taskDisplayArea = this.mTaskDisplayArea;
        DisplayContent content = taskDisplayArea != null ? taskDisplayArea.getDisplayContent() : null;
        if (content != null) {
            content.enableVivoTracksTransientMultiWindowExited();
            content.destroyDockBackground();
            content.getDockedDividerController().notifyDockedStackExistsChanged(false);
            if (this.mWmService.isVivoMultiWindowSupport()) {
                content.getDisplayPolicy().updateSystemUiVisibilityLw();
            }
        }
        hideSplitNavBarIfNeeded();
        WindowManagerService windowManagerService = this.mWmService;
        if (windowManagerService != null && windowManagerService.mAtmInternal != null) {
            this.mWmService.mAtmInternal.setVivoEnterSplitWay(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        }
        if (this.mWmService.mAtmService != null) {
            this.mWmService.mAtmService.resetSplitInterfaceCaller();
        }
        WindowManagerService windowManagerService2 = this.mWmService;
        Handler handler = windowManagerService2 != null ? windowManagerService2.getHandler() : null;
        if (handler != null) {
            handler.postDelayed(new Runnable() { // from class: com.android.server.wm.-$$Lambda$VivoTaskDisplayAreaImpl$zh9rM_yfyVWquw5SxlAnXE77Lqc
                @Override // java.lang.Runnable
                public final void run() {
                    VivoTaskDisplayAreaImpl.this.lambda$onSplitScreenModeDismissed$1$VivoTaskDisplayAreaImpl();
                }
            }, 200L);
        }
    }

    public /* synthetic */ void lambda$onSplitScreenModeDismissed$1$VivoTaskDisplayAreaImpl() {
        synchronized (this.mWmService.mGlobalLock) {
            ActivityStack rootHomeTask = this.mTaskDisplayArea.getRootHomeTask();
            Rect mTmpRect = new Rect();
            this.mTaskDisplayArea.getDisplayContent().getBounds(mTmpRect);
            if (rootHomeTask != null && rootHomeTask.mSurfaceControl != null && !mTmpRect.isEmpty()) {
                SurfaceControl surfaceControl = rootHomeTask.mSurfaceControl;
                SurfaceControl.openTransaction();
                rootHomeTask.mSurfaceControl.setPosition(0.0f, 0.0f);
                rootHomeTask.mSurfaceControl.setWindowCrop(mTmpRect);
                SurfaceControl surfaceControl2 = rootHomeTask.mSurfaceControl;
                SurfaceControl.closeTransaction();
                VSlog.d(TAG, "vivo_multiwindow_fmk onSplitScreenModeDismissed setWindowCrop:" + rootHomeTask.mSurfaceControl + " Rect:" + mTmpRect);
            }
            ActivityRecord homeActivity = this.mTaskDisplayArea.getHomeActivity();
            WindowState mainWc = homeActivity != null ? homeActivity.findMainWindow() : null;
            if (mainWc != null && mainWc.mSurfaceControl != null) {
                SurfaceControl surfaceControl3 = mainWc.mSurfaceControl;
                SurfaceControl.openTransaction();
                mainWc.mSurfaceControl.setPosition(0.0f, 0.0f);
                SurfaceControl surfaceControl4 = mainWc.mSurfaceControl;
                SurfaceControl.closeTransaction();
                VSlog.d(TAG, "vivo_multiwindow_fmk onSplitScreenModeDismissed mainWc:" + mainWc);
            }
        }
    }

    public void debugSplitModeState(String action) {
        if (this.mWmService.mAtmService.isSplitLogDebug()) {
            VSlog.i(TAG, "vivo_multiwindow_fmk multistack state " + action + " " + Debug.getCallers(10));
        }
    }

    public void hideSplitNavBarIfNeeded() {
        if (this.mWmService.mH != null) {
            this.mWmService.mH.removeMessages(70);
            this.mWmService.mH.sendEmptyMessageDelayed(70, SystemProperties.getInt("persist.vivo.splitnarbar_transient_timeout", 1200));
        }
    }

    public void debugOnTaskInfoChanged(ActivityManager.RunningTaskInfo taskInfo) {
        if (taskInfo == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(" onTaskInfoChanged { stackId=");
        sb.append(taskInfo.stackId);
        sb.append(" taskId=");
        sb.append(taskInfo.taskId);
        sb.append(" ,topActivity=");
        sb.append(taskInfo.topActivity);
        sb.append(" ,resizeMode=");
        sb.append(taskInfo.resizeMode);
        sb.append(" ,topActivityType=");
        sb.append(taskInfo.topActivityType);
        sb.append(" ,of windowConfiguration=");
        sb.append(taskInfo.getConfiguration() != null ? taskInfo.getConfiguration().windowConfiguration : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        sb.append(" }");
        VSlog.i("TaskOrganizerController", sb.toString());
    }

    public void setSplitRootTask(int windowingMode, Task task) {
        if (windowingMode == 3) {
            this.mRootPrimaryTask = (ActivityStack) task;
            VSlog.d(TAG, "vivo_multiwindow_fmk setSplitRootTask mRootPrimaryTask:" + task + " TaskDisplayArea:" + this.mTaskDisplayArea);
        } else if (windowingMode == 4) {
            this.mRootSecondaryTask = (ActivityStack) task;
            VSlog.d(TAG, "vivo_multiwindow_fmk setSplitRootTask mRootSecondaryTask:" + task + " TaskDisplayArea:" + this.mTaskDisplayArea);
        }
    }

    public void setMiniLauncherTask(Task task) {
        this.mMiniLauncherTask = task;
        VSlog.d(TAG, "vivo_multiwindow_fmk setMiniLauncherTask mMiniLauncherTask:" + task + " TaskDisplayArea:" + this.mTaskDisplayArea);
    }

    public Task getMiniLauncherTask() {
        return this.mMiniLauncherTask;
    }

    public Task getRootPrimaryTask() {
        return this.mRootPrimaryTask;
    }

    public Task getRootSecondaryTask() {
        return this.mRootSecondaryTask;
    }

    public void resetBoundsChangeTransactionIfNeeded(WindowContainer container) {
        TaskDisplayArea taskDisplayArea;
        Task task;
        if (this.mWmService.mAtmService.isMultiWindowSupport() && (taskDisplayArea = this.mTaskDisplayArea) != null && taskDisplayArea.isSplitScreenModeActivated() && container != null && container.isActivityTypeHome() && (task = container.asTask()) != null) {
            task.setMainWindowSizeChangeTransaction((SurfaceControl.Transaction) null);
            VSlog.d(TAG, "resetBoundsChangeTransaction of task :" + task);
        }
    }

    private void parentSplitMessage(Task child, ConfigurationContainer newParent, ConfigurationContainer oldParent) {
        if (this.mRootPrimaryTask == null || this.mRootSecondaryTask == null) {
            return;
        }
        if ((newParent == null && oldParent == null) || newParent == oldParent) {
            return;
        }
        TaskDisplayArea targetArea = null;
        if (newParent != null && (newParent instanceof Task)) {
            if (oldParent != null && (oldParent instanceof TaskDisplayArea)) {
                targetArea = (TaskDisplayArea) oldParent;
            }
            if (((Task) newParent) == this.mRootPrimaryTask) {
                StringBuilder sb = new StringBuilder();
                sb.append(" onParentChange child:");
                sb.append(child);
                sb.append(" to mRootPrimaryTask,oldParent is ");
                sb.append(targetArea != null ? targetArea : "null");
                sb.append(",child.mPausingActivity is ");
                sb.append(child != null ? child.mPausingActivity : "null");
                sb.append(",mRootSecondaryTask is ");
                sb.append(this.mRootSecondaryTask);
                sb.append(",local rootSecondaryStack is ");
                ActivityStack activityStack = this.rootSecondaryStack;
                if (activityStack == null) {
                    activityStack = "null";
                }
                sb.append(activityStack);
                VSlog.d(TAG, sb.toString());
                if (child != null && child.mPausingActivity != null) {
                    child.mPausingActivity = null;
                    VSlog.d(TAG, " onParentChange child:" + child + " set mPausingActivity null");
                }
            } else if (((Task) newParent) == this.mRootSecondaryTask) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(" onParentChange child:");
                sb2.append(child);
                sb2.append(" to mRootSecondaryTask,oldParent is ");
                sb2.append(targetArea != null ? targetArea : "null");
                sb2.append(",child.mPausingActivity is ");
                sb2.append(child != null ? child.mPausingActivity : "null");
                sb2.append(",mRootSecondaryTask is ");
                sb2.append(this.mRootSecondaryTask);
                sb2.append(",local rootSecondaryStack is ");
                ActivityStack activityStack2 = this.rootSecondaryStack;
                if (activityStack2 == null) {
                    activityStack2 = "null";
                }
                sb2.append(activityStack2);
                VSlog.d(TAG, sb2.toString());
                if (child != null && child.mPausingActivity != null && (isMultiWindowEnterJustWithDisplay() || child.isActivityTypeHome())) {
                    child.mPausingActivity = null;
                    VSlog.d(TAG, " onParentChange child:" + child + " set mPausingActivity null");
                }
            }
        }
        if (oldParent != null && (oldParent instanceof Task)) {
            if (newParent != null && (newParent instanceof TaskDisplayArea)) {
                targetArea = (TaskDisplayArea) newParent;
            }
            Task task = (Task) oldParent;
            Task task2 = this.mRootPrimaryTask;
            TaskDisplayArea taskDisplayArea = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            if (task == task2) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append(" onParentChange child:");
                sb3.append(child);
                sb3.append(" out mRootPrimaryTask,newParent is ");
                if (targetArea != null) {
                    taskDisplayArea = targetArea;
                }
                sb3.append(taskDisplayArea);
                sb3.append(",mRootSecondaryTask is ");
                sb3.append(this.mRootSecondaryTask);
                sb3.append(",local rootSecondaryStack is ");
                String str = this.rootSecondaryStack;
                sb3.append((Object) (str != null ? str : "null"));
                VSlog.d(TAG, sb3.toString());
            } else if (((Task) oldParent) == this.mRootSecondaryTask) {
                StringBuilder sb4 = new StringBuilder();
                sb4.append(" onParentChange child:");
                sb4.append(child);
                sb4.append(" out mRootSecondaryTask,newParent is ");
                if (targetArea != null) {
                    taskDisplayArea = targetArea;
                }
                sb4.append(taskDisplayArea);
                sb4.append(",mRootSecondaryTask is ");
                sb4.append(this.mRootSecondaryTask);
                sb4.append(",local rootSecondaryStack is ");
                String str2 = this.rootSecondaryStack;
                sb4.append((Object) (str2 != null ? str2 : "null"));
                VSlog.d(TAG, sb4.toString());
            }
        }
    }

    public void setMoveHomeToFrontWhenDissmissSplit() {
        this.moveHomeToFrontWhenDissmissSplit = true;
    }

    public boolean getMoveHomeToFrontWhenDissmissSplit() {
        try {
            return this.moveHomeToFrontWhenDissmissSplit;
        } finally {
            this.moveHomeToFrontWhenDissmissSplit = false;
        }
    }

    public void acquireAppLaunchPerfLock(final ActivityRecord r) {
        PerfThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.-$$Lambda$VivoTaskDisplayAreaImpl$zxx2XRS2NP0sBxlaS5bSreEjVOc
            @Override // java.lang.Runnable
            public final void run() {
                VivoTaskDisplayAreaImpl.this.lambda$acquireAppLaunchPerfLock$2$VivoTaskDisplayAreaImpl(r);
            }
        });
    }

    public /* synthetic */ void lambda$acquireAppLaunchPerfLock$2$VivoTaskDisplayAreaImpl(ActivityRecord r) {
        WindowProcessController wpc;
        if (this.mPerfBoost == null) {
            this.mPerfBoost = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        }
        AbsVivoPerfManager absVivoPerfManager = this.mPerfBoost;
        if (absVivoPerfManager != null) {
            absVivoPerfManager.perfHintAsync(4225, r.packageName, -1, 1);
            mPerfSendTapHint = true;
            this.mPerfBoost.perfHintAsync(4225, r.packageName, -1, 2);
            WindowManagerService windowManagerService = this.mWmService;
            if (windowManagerService != null && windowManagerService.mAtmService != null && r != null && r.info != null && r.info.applicationInfo != null && (wpc = this.mWmService.mAtmService.getProcessController(r.processName, r.info.applicationInfo.uid)) != null && wpc.hasThread()) {
                this.mPerfBoost.perfHintAsync(4225, r.packageName, wpc.getPid(), (int) KernelConfig.DBG_TARGET_REGADDR_VALUE_GET);
            }
            if (this.mPerfBoost.perfGetFeedback(5633, r.packageName) == 2) {
                mPerfHandle = this.mPerfBoost.perfHint(4225, r.packageName, -1, 4);
            } else {
                mPerfHandle = this.mPerfBoost.perfHint(4225, r.packageName, -1, 3);
            }
            if (mPerfHandle > 0) {
                mIsPerfBoostAcquired = true;
            }
            if (r.info.applicationInfo != null && r.info.applicationInfo.sourceDir != null) {
                this.mPerfBoost.perfIOPrefetchStart(-1, r.packageName, r.info.applicationInfo.sourceDir.substring(0, r.info.applicationInfo.sourceDir.lastIndexOf(47)));
            }
        }
    }

    public void acquireUxPerfLock(String packageName) {
        AbsVivoPerfManager vivoPerfManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        this.mUxPerf = vivoPerfManager;
        if (vivoPerfManager != null) {
            vivoPerfManager.perfUXEngine_events(6, 0, packageName, 0);
        }
    }

    public void moveHomeActivityToTopBoost() {
        if (this.mPerfHome == null) {
            this.mPerfHome = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        }
        AbsVivoPerfManager absVivoPerfManager = this.mPerfHome;
        if (absVivoPerfManager != null) {
            absVivoPerfManager.perfHint(4228, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, -1, 2);
        }
    }

    public int getmPerfHandle() {
        return mPerfHandle;
    }

    public void resetmPerfHandle() {
        mPerfHandle = -1;
    }

    public boolean getPerfBoostAcquired() {
        return mIsPerfBoostAcquired;
    }

    public void resetPerfBoostAcquired() {
        mIsPerfBoostAcquired = false;
    }

    public boolean getPerfSendTapHint() {
        return mPerfSendTapHint;
    }

    public void resetPerfSendTapHint() {
        mPerfSendTapHint = false;
    }

    public boolean removeForVirtualDisplay(ActivityStack stack, TaskDisplayArea toDisplayArea) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && MultiDisplayManager.isVivoDisplay(this.mTaskDisplayArea.getDisplayId())) {
            VSlog.d("VivoStack", "removeForVirtualDisplay : " + stack);
            if (this.mTaskDisplayArea.mDisplayContent.shouldDestroyContentOnRemove()) {
                stack.finishAllActivitiesImmediately();
                return true;
            }
            stack.reparent(toDisplayArea, Integer.MIN_VALUE);
            stack.setWindowingMode(0);
            return true;
        }
        return false;
    }
}