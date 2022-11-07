package com.android.server.wm;

import android.app.WindowConfiguration;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.multidisplay.MultiDisplayManager;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.android.server.wm.ActivityStack;
import com.vivo.face.common.data.Constants;
import com.vivo.framework.systemdefence.SystemDefenceManager;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.superresolution.Constant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoTaskImpl implements IVivoTask {
    WindowState mExitingWindow;
    private Handler mHandler;
    private ActivityTaskManagerService mService;
    private Task mTask;
    private String TAG = "VivoTaskImpl";
    private boolean mIsSnapShotWhenExitFreeform = false;
    private boolean inResetFocusToFullsceenStack = false;
    private boolean checkFocus = false;
    private Task mTopTask = null;
    private boolean mIgnoreSurfaceLayoutDefer = false;
    private Rect mLastBounds = null;
    private Configuration mTmpConfig = new Configuration();
    private ActivityRecord mTopFinishingInFreeform = null;
    private boolean fromVivoFreeform = false;
    private boolean shouldGoHomeWhenFinish = false;
    private boolean mIsExtingDirectFreeform = false;
    private boolean isfreeformExit = false;
    private boolean isDirectFreeformExit = false;
    private final ArrayList<String> mFreeformUnResizableList = new ArrayList<>(Arrays.asList("com.android.notes", "com.vivo.notes", "com.ss.android.article.video", "com.chaoxing.mobile", "com.tencent.weishi", "com.baidu.BaiduMap", "ctrip.android.view"));
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
    private boolean mShouldFinishAllActivitiesAfterMovingToBack = false;
    private ArrayList<WindowContainer> transitioningDescendants = new ArrayList<>();
    final String STR_TOGGLE_SPLIT_REASON_THREEFINGER = "reas_tf";
    boolean inSanitizeAndApplyHierarchyOpState = false;
    int mUseBLASTSyncTransaction = -1;
    private Runnable TransientMultiWindowExited = new Runnable() { // from class: com.android.server.wm.VivoTaskImpl.2
        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoTaskImpl.this.mService.mGlobalLock) {
                VivoTaskImpl.this.mUseBLASTSyncTransaction = -1;
            }
        }
    };
    private Runnable BLASTSyncTransactionApply = new Runnable() { // from class: com.android.server.wm.VivoTaskImpl.3
        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoTaskImpl.this.mService.mGlobalLock) {
                if (VivoTaskImpl.this.mTask != null) {
                    VivoTaskImpl.this.mTask.mBLASTSyncTransaction.apply();
                }
            }
        }
    };
    private final ToBooleanFunction<WindowState> mFindExitingWindow = new ToBooleanFunction() { // from class: com.android.server.wm.-$$Lambda$VivoTaskImpl$tzv2kFWeQFSPMvGZQdwdHbR8yVo
        public final boolean apply(Object obj) {
            return VivoTaskImpl.this.lambda$new$1$VivoTaskImpl((WindowState) obj);
        }
    };
    private VivoAppShareManager mVivoAppShareManager = VivoAppShareManager.getInstance();

    /* loaded from: classes.dex */
    private class VivoActivityStackImplHandler extends Handler {
        VivoActivityStackImplHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 108) {
                ActivityRecord r = (ActivityRecord) msg.obj;
                boolean isStopForce = msg.arg1 != 0;
                String str = VivoTaskImpl.this.TAG;
                VSlog.w(str, "Activity FreezingScreen timeout for " + r + " and stop(" + isStopForce + ")");
                synchronized (VivoTaskImpl.this.mService.mGlobalLock) {
                    if (r != null) {
                        r.stopFreezingScreenLocked(isStopForce);
                    }
                }
            }
        }
    }

    public VivoTaskImpl(Task task, ActivityTaskManagerService service) {
        this.mTask = null;
        this.mService = null;
        this.mHandler = null;
        this.mTask = task;
        this.mService = service;
        this.mHandler = new VivoActivityStackImplHandler(service.mStackSupervisor.mLooper);
    }

    public void dummy() {
        String str = this.TAG;
        VSlog.i(str, "dummy, this=" + this);
    }

    public boolean checkUseridForDoubleInstance(int userid) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && userid == 999) {
            return true;
        }
        return false;
    }

    public int getDockSide(Configuration parentConfig, Rect bounds) {
        if (parentConfig != null && bounds != null && !bounds.isEmpty()) {
            return getDockSide(bounds, parentConfig.windowConfiguration.getBounds(), parentConfig.orientation, parentConfig.windowConfiguration.getRotation());
        }
        return -1;
    }

    public int getDockSide() {
        Task task = this.mTask;
        if (task != null && task.getDisplayContent() != null) {
            return getDockSide(this.mTask.getDisplayContent().getConfiguration(), this.mTask.getBounds());
        }
        return -1;
    }

    private int getDockSide(Rect bounds, Rect displayRect, int orientation, int rotation) {
        if (orientation == 1) {
            int imeHeight = getVisibleImeHeight();
            int diff = ((displayRect.bottom - bounds.bottom) - (bounds.top - displayRect.top)) - imeHeight;
            if (diff > 0) {
                return 2;
            }
            return diff <= 0 ? 4 : -1;
        } else if (orientation == 2) {
            int diff2 = (displayRect.right - bounds.right) - (bounds.left - displayRect.left);
            if (diff2 > 0) {
                return 1;
            }
            return diff2 < 0 ? 3 : -1;
        } else {
            return -1;
        }
    }

    private int getVisibleImeHeight() {
        Task task = this.mTask;
        DisplayContent content = task != null ? task.getDisplayContent() : null;
        if (content != null) {
            WindowState imeWin = content.mInputMethodWindow;
            boolean imeVisible = imeWin != null && imeWin.isVisibleLw() && imeWin.isDisplayedLw();
            DisplayFrames frames = content.mDisplayFrames;
            if (imeVisible && frames != null) {
                return frames.getInputMethodWindowVisibleHeight();
            }
        }
        return 0;
    }

    public void setTransitioningDescendants(ArrayList<WindowContainer> sources) {
        this.transitioningDescendants.clear();
        if (sources != null) {
            this.transitioningDescendants.addAll(sources);
        }
    }

    public WindowContainer getTransitioningDescendantsOfTop() {
        return this.transitioningDescendants.get(0);
    }

    private ArrayList<Task> getAllTasks() {
        ArrayList<Task> returnList = new ArrayList<>();
        int size = this.mTask.getChildCount();
        for (int i = 0; i < size; i++) {
            if (this.mTask.getChildAt(i) != null && this.mTask.getChildAt(i).asTask() != null) {
                returnList.add(this.mTask.getChildAt(i).asTask());
            }
        }
        return returnList;
    }

    public void forAllTopTasksSafety(Consumer<Task> callback, Task parent, boolean traverseTopToBottom) {
        boolean isTopTask;
        Task task = this.mTask;
        if (task == null || parent == null || callback == null) {
            return;
        }
        Task mParent = task.getParent() != null ? this.mTask.getParent().asTask() : null;
        if (parent == this.mTask) {
            isTopTask = false;
        } else if (parent == mParent) {
            isTopTask = true;
        } else {
            return;
        }
        if (isTopTask) {
            callback.accept(this.mTask);
            return;
        }
        ArrayList<Task> mChildren = getAllTasks();
        int count = mChildren.size();
        if (traverseTopToBottom) {
            for (int i = count - 1; i >= 0; i--) {
                Task child = mChildren.get(i);
                if (child != null) {
                    child.forAllTopTasksSafety(callback, parent, traverseTopToBottom);
                }
            }
            return;
        }
        for (int i2 = 0; i2 < count; i2++) {
            Task child2 = mChildren.get(i2);
            if (child2 != null) {
                child2.forAllTopTasksSafety(callback, parent, traverseTopToBottom);
            }
        }
    }

    public void vivoResetFreeFormStatusIfNeed() {
        boolean isInVivoFreeform = this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform();
        if (!isInVivoFreeform || !this.mTask.isRootTask()) {
            return;
        }
        VSlog.d(this.TAG, "vivoResetFreeFormStatus reset freeform status for exit");
        TaskDisplayArea taskDisplayArea = this.mTask.getDisplayArea();
        boolean resetMaxStateIfNeeded = false;
        if (this.mTask.inFreeformWindowingMode()) {
            resetMaxStateIfNeeded = true;
        } else if (this.mTask.getWindowingMode() == 1 && this.mTask.getActivityType() == 1 && !this.mService.isInDirectFreeformState() && taskDisplayArea != null && this.mTask == taskDisplayArea.getTopStackInWindowingMode(1)) {
            resetMaxStateIfNeeded = true;
        }
        if (resetMaxStateIfNeeded) {
            this.mService.mStackSupervisor.resetVivoFreeformMaxmizeState();
        }
        this.mService.mStackSupervisor.setWillCallContactsFromFreefromMms(false);
        this.mService.mStackSupervisor.setWillCallContactsFromPasswd(false);
        if (this.mTask.inFreeformWindowingMode()) {
            if (this.mService.isInDirectFreeformState()) {
                this.mService.setLastExitFromDirectFreeform(true);
            }
            this.mService.mWindowManager.setIsRemovingFreeformStack(true);
            ActivityTaskManagerService activityTaskManagerService = this.mService;
            activityTaskManagerService.enableVivoFreeFormRuntime(false, false, activityTaskManagerService.mWindowManager.getFreeformScale());
            VivoFreeformUtils.inFullscreenMode = false;
            try {
                int currentUserId = this.mService.mAmInternal.getCurrentUserId();
                Settings.System.putIntForUser(this.mService.mContext.getContentResolver(), "smartmultiwindow_freeform", 0, currentUserId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ActivityRecord windowTranslucencyChangedActivity = this.mService.mRootWindowContainer.getWindowTranslucencyChangedActivity();
            if (windowTranslucencyChangedActivity != null) {
                if (windowTranslucencyChangedActivity.getTask() != null) {
                    windowTranslucencyChangedActivity.setOccludesParent(false);
                }
                this.mService.mRootWindowContainer.setWindowTranslucencyChangedActivity((ActivityRecord) null);
                this.mService.mRootWindowContainer.setSendProcessChangedForTranslucencyActivity(true);
                this.mService.mWindowManager.setTopFullscreenIsTranslucency(true);
            }
            VivoStatsInServerImpl.setWhichPIP(false);
        }
    }

    public void setExitingFreeformStateIfNeed(boolean remove) {
        if (this.mService.mWindowManager.isVivoFreeformFeatureSupport()) {
            if ((this.mTask.inFreeformWindowingMode() || !remove) && this.mTask.isRootTask()) {
                String str = this.TAG;
                VSlog.d(str, "reset freeform status, remove:" + remove);
                if (this.mService.mWindowManager.getFreeformPosition() != null) {
                    this.mService.mWindowManager.getFreeformPosition().setEmpty();
                }
                this.mService.mWindowManager.setFreeformStackMove(false);
                this.mService.mWindowManager.setExitingFreeForm(true);
                Message msg = this.mService.mWindowManager.getHandler().obtainMessage(104);
                this.mService.mWindowManager.getHandler().sendMessageDelayed(msg, 1000L);
                if (this.mService.mWindowManager.isRemovingFreeformStack()) {
                    String str2 = this.TAG;
                    VSlog.d(str2, "reset mIsRemovingFreeformStack " + this.mTask.getRootTaskId());
                    this.mService.mWindowManager.setIsRemovingFreeformStack(false);
                }
                this.mService.setCurrentRotationHasResized(false);
                this.mService.mWindowManager.unRegisterFreeformPointEventListener(this.mTask.getDisplayContent());
                transitBackToLastFreeformTask();
            }
        }
    }

    private void transitBackToLastFreeformTask() {
        Task prevVivoFreeformTask = this.mService.getPrevVivoFreeformTask();
        if (prevVivoFreeformTask != null && this.mTask != prevVivoFreeformTask) {
            float freeformScale = this.mService.mWindowManager.getFreeformScale();
            this.mService.enableVivoFreeFormRuntime(true, true, freeformScale);
            prevVivoFreeformTask.mLastNonFullscreenBounds = this.mTask.mLastNonFullscreenBounds;
            prevVivoFreeformTask.setWindowingMode(5);
            this.mService.setPrevVivoFreeformTask((Task) null);
            Settings.System.putIntForUser(this.mService.mContext.getContentResolver(), VivoFreeformUtils.VIVO_SETTINGS_IN_FREEFORM_TRANSIT, 1, this.mService.mAmInternal.getCurrentUserId());
        }
    }

    public void printFreeformStackRemoveLogIfNeed() {
        if (this.mService.mWindowManager.isVivoFreeformFeatureSupport() && this.mTask.isRootTask() && this.mTask.inFreeformWindowingMode()) {
            EventLogTags.writeWmTaskRemoved(this.mTask.getRootTaskId(), "freeform stack removed");
        }
    }

    public boolean isTopFinishingTask(Task task) {
        if (!this.mTask.isRootTask() || this.mTask.isLeafTask()) {
            return false;
        }
        for (int i = this.mTask.getChildCount() - 1; i >= 0; i--) {
            Task childAt = this.mTask.getChildAt(i);
            if (childAt != null && (childAt instanceof ActivityStack)) {
                ActivityRecord r = childAt.topRunningActivityLocked();
                if (r != null) {
                    return false;
                }
                if (childAt == task) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean needIgnoreChangeTransitionForFreeform(int prevWinMode, int newWinMode) {
        if (this.mTask.mWmService.isVivoFreeformFeatureSupport()) {
            if (prevWinMode == 5 || newWinMode == 5) {
                WindowContainer p = this.mTask.getParent();
                if (p == null || p.getParent() == null) {
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    public void exitFreeformWhenReparentFullscreenStackToOtherDisplay(TaskDisplayArea newParent) {
        if (this.mTask.isRootTask() && this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform()) {
            ActivityStack topStack = this.mService.mRootWindowContainer.getDefaultDisplay().getTopStack();
            ActivityStack fullscreenStack = this.mService.mRootWindowContainer.getDefaultDisplay().getStack(1, 1);
            if (topStack != null && ((ActivityStack) this.mTask) == fullscreenStack && newParent.getDisplayId() != 0 && !this.mService.isInDirectFreeformState()) {
                this.mService.mRootWindowContainer.getDefaultTaskDisplayArea().moveVivoFreeformTasksToFullscreenStackLocked(topStack.inFreeformWindowingMode());
            }
        }
    }

    public void moveToFrontInVivoFreeform(Task task, String reason) {
        if (this.mTask.isRootTask() && this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform()) {
            checkAndResetVivoFreeformMinimizeState(reason);
            checkAndResizeVivoFreeformTask(reason, task);
            checkAndMoveVivoFreeformStackToFrontFirst();
        }
    }

    private void checkAndResetVivoFreeformMinimizeState(String reason) {
        if (this.mTask.isRootTask() && this.mTask.getWindowConfiguration().canResizeTask() && reason != null && !reason.equals("moveFreeFormStack")) {
            try {
                int currentUserId = this.mService.mAmInternal.getCurrentUserId();
                Settings.System.putIntForUser(this.mService.mContext.getContentResolver(), "freeform_minimize", 0, currentUserId);
                this.mService.setFreeFormMin(false);
                this.mService.mWindowManager.setFreeFormMin(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkAndResizeVivoFreeformTask(String reason, Task task) {
        Rect launchBounds;
        if (this.mTask.isRootTask() && task != null && "bringingFoundTaskToFront".equals(reason) && this.mTask.inFreeformWindowingMode() && (launchBounds = task.getLaunchBounds()) != null && !launchBounds.equals(task.getBounds())) {
            task.resize(launchBounds, 2, false);
        }
    }

    private void checkAndMoveVivoFreeformStackToFrontFirst() {
        ActivityStack freeformStack;
        if (!this.mTask.isRootTask()) {
            return;
        }
        ActivityStack topStack = this.mTask.getDisplayArea() != null ? this.mTask.getDisplayArea().getTopStack() : null;
        if (topStack != null && !topStack.inFreeformWindowingMode()) {
            if ((this.mTask.getWindowingMode() == 1 || (this.mService.isInDirectFreeformState() && this.mTask.getActivityType() == 2)) && !this.mService.isFreeFormMin() && (freeformStack = this.mTask.getDisplayArea().getVivoFreeformStack()) != null) {
                freeformStack.moveToFront("moveFreeFormStack");
            }
        }
    }

    public boolean notMoveHomeToFrontInMultiDisplay() {
        return this.mTask.isRootTask() && this.mService.mWindowManager.isVivoFreeformFeatureSupport() && this.mTask.isRootTask() && !this.mTask.isLeafTask() && this.mTask.getChildAt(0) != null && (this.mTask.getChildAt(0) instanceof ActivityStack) && this.mTask.getChildAt(0).isFromVivoFreeform() && !this.mTask.mRootWindowContainer.isTopFocusedDisplay(this.mTask.getDisplayContent());
    }

    public void setLaunchFromSoftwareLock(ActivityRecord r, ActivityRecord target) {
        if (this.mTask.isRootTask() && this.mService.isVivoFreeFormValid() && target.isLaunchFromSoftware()) {
            r.setLaunchFromSoftware(true);
        }
    }

    public int getVisibilityInVivoFreeform(ActivityRecord starting, TaskDisplayArea taskDisplayArea, int windowingMode) {
        boolean isInVivoFreeform = this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform();
        if (!isInVivoFreeform || !this.mTask.isRootTask()) {
            return -1;
        }
        ActivityStack focusedStack = taskDisplayArea != null ? taskDisplayArea.mPreferredTopFocusableStack : null;
        if (this.mTask.getWindowingMode() == 5 && taskDisplayArea != null) {
            return (taskDisplayArea.isTopStack(this.mTask) && this.mTask.getDisplayId() == this.mService.getFocusedDisplayId()) ? 0 : 2;
        } else if (focusedStack == null || focusedStack.isActivityTypeHome() || !this.mTask.isActivityTypeHome() || this.mService.isInDirectFreeformState() || this.mTask.getDisplayId() != 0) {
            if (focusedStack != null && focusedStack.inFreeformWindowingMode() && this.mTask.getDisplayId() == focusedStack.getDisplayId()) {
                if (this.mService.isFreeFormStackMax() && !this.mTask.inFreeformWindowingMode()) {
                    boolean isRecentsTop = this.mTask.isActivityTypeRecents() && taskDisplayArea.getIndexOf(this.mTask) > taskDisplayArea.getIndexOf(focusedStack);
                    if (!isRecentsTop) {
                        return 2;
                    }
                }
                int stackBehindTopIndex = taskDisplayArea.getIndexOf(taskDisplayArea.getTopStack()) - 1;
                int stackIndex = taskDisplayArea.getIndexOf(this.mTask);
                if (stackIndex == stackBehindTopIndex) {
                    return 0;
                }
            }
            if (windowingMode == 1 && focusedStack != null && taskDisplayArea.getVivoFreeformStack() != null && focusedStack.getWindowingMode() == 1 && this.mTask.getDisplayId() == taskDisplayArea.getVivoFreeformStack().getDisplayId()) {
                ActivityRecord focusTop = focusedStack.topRunningActivityLocked();
                if (starting != null && starting.isEmergentActivity() && starting.getStack() != ((ActivityStack) this.mTask)) {
                    return 2;
                }
                if (focusTop != null && focusTop.getState() == ActivityStack.ActivityState.RESUMED && focusTop.isEmergentActivity() && focusedStack != ((ActivityStack) this.mTask) && taskDisplayArea.getIndexOf(focusedStack) > taskDisplayArea.getIndexOf(this.mTask)) {
                    return 2;
                }
            }
            return -1;
        } else {
            return 2;
        }
    }

    public boolean skipMakeInvisibleForFreeform(ActivityRecord r) {
        return this.mTask.isRootTask() && this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mService.getFreeformKeepR() == r;
    }

    public void moveFreeformWindowToTopWhenResumeOtherStack() {
        ActivityStack freeformStack;
        if (this.mTask.isRootTask() && this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isFreeFormMin()) {
            if (((this.mTask.getWindowingMode() == 1 && this.mTask.getActivityType() == 1) || (this.mService.isInDirectFreeformState() && this.mTask.getActivityType() == 2)) && (freeformStack = this.mService.mRootWindowContainer.getVivoFreeformStack()) != null && freeformStack.getDisplay() != null && freeformStack.getDisplayId() == this.mService.getFocusedDisplayId() && !freeformStack.getDisplay().isSleeping()) {
                freeformStack.moveFreeformWindowStateToTop();
            }
        }
    }

    public void checkAndResetFocusToFullscreenStack() {
        ActivityStack fullscreenStack;
        if (!this.mTask.isRootTask() || this.inResetFocusToFullsceenStack) {
            return;
        }
        try {
            this.inResetFocusToFullsceenStack = true;
            if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mTask.inFreeformWindowingMode() && this.mTask.topRunningActivityLocked() != null && !this.mTask.topRunningActivityLocked().isForceFullScreenForFreeForm() && !this.mService.mWindowManager.isVivoFreeFormStackMax()) {
                TaskDisplayArea taskDisplayArea = this.mTask.getDisplayArea();
                ActivityRecord fullScreenAr = null;
                if (taskDisplayArea == null) {
                    fullscreenStack = null;
                } else {
                    fullscreenStack = taskDisplayArea.getStack(1, 1);
                }
                if (fullscreenStack != null) {
                    fullScreenAr = fullscreenStack.topRunningActivityLocked();
                }
                boolean alwaysFocuseFullsceen = false;
                if (this.checkFocus && taskDisplayArea != null) {
                    this.checkFocus = false;
                    if (fullscreenStack != null && taskDisplayArea.getFocusedStack() != fullscreenStack && fullScreenAr != null && fullScreenAr.getState() == ActivityStack.ActivityState.RESUMED) {
                        alwaysFocuseFullsceen = true;
                    }
                }
                if (((fullScreenAr != null && !fullScreenAr.isPasswordActivity() && taskDisplayArea.hasSetCheckFocus()) || alwaysFocuseFullsceen) && !taskDisplayArea.isHomeStackVisible() && fullScreenAr.moveFocusableActivityToTop("reset focus to fullscreen stack")) {
                    this.mService.mRootWindowContainer.resumeFocusedStacksTopActivities();
                    ActivityStack lastFocusStack = taskDisplayArea.getLastFocusedStack();
                    if (lastFocusStack != null && lastFocusStack.inFreeformWindowingMode() && taskDisplayArea.hasSetCheckFocus()) {
                        taskDisplayArea.setCheckFocus(false);
                        this.checkFocus = true;
                    }
                }
                if (taskDisplayArea != null && taskDisplayArea.hasSetCheckFocus() && fullScreenAr != null && !fullScreenAr.isPasswordActivity()) {
                    taskDisplayArea.setCheckFocus(false);
                }
            }
        } finally {
            this.inResetFocusToFullsceenStack = false;
        }
    }

    public void resetStartingPasswdOnHomeAndFirstUnlock(ActivityRecord prev) {
        if (this.mTask.isRootTask() && this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform()) {
            if ((this.mTask.getWindowingMode() == 5 || this.mTask.getWindowingMode() == 1) && this.mService.isInDirectFreeformState() && this.mService.isStartingPasswdOnHome() && prev != null && prev.isPasswordActivity() && prev.finishing) {
                this.mService.setIsStartingPasswdOnHome(false);
            }
        }
    }

    public void handleNonResizeableActivityInFreeform(ActivityRecord next) {
        TaskDisplayArea taskDisplayArea;
        if (!this.mTask.isRootTask()) {
            return;
        }
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform()) {
            ActivityStack freeformStack = this.mService.mRootWindowContainer.getVivoFreeformStack();
            this.mService.mStackSupervisor.handleNonResizableActivityIfNeededForFreeForm(next);
            if (freeformStack != null && next != null && this.mTask.inFreeformWindowingMode() && (("com.android.camera/.CameraActivity".equals(next.mActivityComponent.flattenToShortString()) || "com.android.camera/.view.ThirdCameraActivity".equals(next.mActivityComponent.flattenToShortString()) || "com.android.settings/com.vivo.settings.secret.ConfirmSecretPinNoTitle".equals(next.mActivityComponent.flattenToShortString()) || "com.android.settings/com.vivo.settings.secret.ChooseSecretLockGeneric".equals(next.mActivityComponent.flattenToShortString())) && (taskDisplayArea = this.mTask.getDisplayArea()) != null)) {
                taskDisplayArea.moveVivoFreeformTasksToFullscreenStackLocked(true);
            }
        }
        if (this.mService.isStartingPassword()) {
            this.mService.setIsStartingPassword(false);
        }
        if (this.mService.isStartingEmergent()) {
            this.mService.setIsStartingEmergent(false);
        }
    }

    public boolean prepareMinimizingFreeformTransitionIfNeed(ActivityRecord next, ActivityRecord prev) {
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && prev.inFreeformWindowingMode() && next.getWindowingMode() == 1 && this.mService.isFreeFormMin() && this.mTask.getDisplayArea() != null) {
            this.mTask.getDisplayArea().mDisplayContent.prepareAppTransition(11, false);
            return true;
        }
        return false;
    }

    public boolean shouldBeVisibleInVivoFreeform() {
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mTask.shouldBeVisible((ActivityRecord) null)) {
            return true;
        }
        return false;
    }

    public boolean setFocusedAppAndSendProcessChange(ActivityStack nextFocusedStack) {
        ActivityRecord top;
        if (this.mTask.isRootTask() && this.mService.isVivoFreeFormValid() && this.mTask.inFreeformWindowingMode() && (top = nextFocusedStack.topRunningActivityLocked()) != null && top.getState() == ActivityStack.ActivityState.RESUMED && top.getDisplay() != null) {
            top.getDisplay().setFocusedApp(top, true);
            if (top.app != null) {
                this.mService.mAmInternal.sendProcessActivityChangeMessageOnce(top.app.getPid(), top.info.applicationInfo.uid);
            }
            return true;
        }
        return false;
    }

    public ActivityStack adjustToHomeInForVivoFreeformIfNeed(ActivityStack focusableTask, String reason) {
        ActivityStack homeTask;
        boolean isImeTarget = false;
        boolean inVivoFreeform = this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform();
        if (!this.mService.mWindowManager.isVivoFreeformFeatureSupport()) {
            return focusableTask;
        }
        ActivityStack nextRootTask = focusableTask.getRootTask();
        ActivityStack curRootTask = (ActivityStack) this.mTask.getRootTask();
        if ("finish-top".equals(reason) && isShouldGoHomeWhenFinish()) {
            if (nextRootTask != curRootTask && !focusableTask.isActivityTypeHome() && this.mTask.getDisplayArea() != null) {
                setShouldGoHomeWhenFinish(false);
                ActivityStack homeTask2 = this.mTask.getDisplayArea().getRootHomeTask();
                if (homeTask2 != null && !inVivoFreeform) {
                    VSlog.d(this.TAG, "adjustToHomeInForVivoFreeformIfNeed homeTask:" + homeTask2 + " focusableTask:" + focusableTask);
                    return homeTask2;
                }
                return focusableTask;
            }
            return focusableTask;
        } else if (inVivoFreeform && focusableTask.getWindowingMode() == 5 && this.mTask.getWindowingMode() == 1 && "finish-top".equals(reason)) {
            return null;
        } else {
            if (inVivoFreeform && this.mTask.getWindowingMode() == 5 && focusableTask.getWindowingMode() == 1 && "finish-top".equals(reason) && this.mTask.isLeafTask()) {
                ActivityRecord top = this.mTask.getTopMostActivity();
                boolean isTopTaskFinishing = top != null && top.finishing;
                if (this.mTask.mResumedActivity != null && this.mService.mWindowManager.isImeTarget(this.mTask.mResumedActivity.appToken, this.mTask.getDisplayContent())) {
                    isImeTarget = true;
                }
                if (this.mService.isInDirectFreeformState() && this.mService.mRootWindowContainer.getTopDisplayFocusedStack() == this.mTask.getRootTask() && isTopTaskFinishing && isImeTarget) {
                    this.mService.setShortFreezingAnimaiton(this.mTask.mResumedActivity.appToken);
                }
                if (!focusableTask.isRootTask() && nextRootTask != null && nextRootTask.getTopMostTask() == focusableTask) {
                    return nextRootTask;
                }
                return focusableTask;
            } else if ("moveTaskToBackLocked(freeform)".equals(reason) && this.mService.isFreeFormMin() && inVivoFreeform && this.mService.isInDirectFreeformState() && focusableTask.getWindowingMode() == 5 && this.mTask.getWindowingMode() == 1 && nextRootTask != curRootTask && this.mTask.getDisplayArea() != null && !this.mTask.isActivityTypeHome() && (homeTask = this.mTask.getDisplayArea().getRootHomeTask()) != null) {
                return homeTask;
            } else {
                return focusableTask;
            }
        }
    }

    public boolean ignoreMoveTaskToBackForSpecial(Task tr) {
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && tr.getChildCount() > 0 && tr.isLeafTask()) {
            ActivityRecord topRunningActivity = tr.getChildAt(tr.getChildCount() - 1);
            String currentClassName = null;
            String packageName = null;
            if (topRunningActivity != null && topRunningActivity.mActivityComponent != null) {
                currentClassName = topRunningActivity.mActivityComponent.flattenToShortString();
                packageName = topRunningActivity.mActivityComponent.getPackageName();
            }
            return !(this.mTask == this.mService.mRootWindowContainer.getTopDisplayFocusedStack() || this.mTask.isActivityTypeHome() || this.mService.mRootWindowContainer.getTopDisplayFocusedStack().isActivityTypeHome() || !"com.tencent.mobileqq".equals(packageName)) || "com.tencent.mobileqq/.activity.QQLSActivity".equals(currentClassName) || "com.tencent.mobileqq/.activity.QQLSUnlockActivity".equals(currentClassName);
        }
        return false;
    }

    public void moveFreeformToFullscreenWhenTaskToBack() {
        ActivityStack freefromstack;
        TaskDisplayArea taskDisplayArea;
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mTask.getWindowingMode() == 1 && !isEmergentTask() && !this.mService.isInDirectFreeformState() && (freefromstack = this.mService.mRootWindowContainer.getVivoFreeformStack()) != null && (taskDisplayArea = freefromstack.getDisplayArea()) != null) {
            taskDisplayArea.moveVivoFreeformTasksToFullscreenStackLocked(false);
        }
    }

    public boolean moveTaskToBackForVivoFreeform(Task tr) {
        int numTasks = this.mTask.getChildCount();
        if (this.mTask.isLeafTask() && numTasks > 1) {
            numTasks = 1;
        }
        if (this.mTask.isRootTask() && numTasks <= 1 && this.mTask.isOnHomeDisplay()) {
            if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && tr.isActivityTypeRecents()) {
                return true;
            }
            if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mTask.inFreeformWindowingMode()) {
                this.mService.mRootWindowContainer.getDefaultTaskDisplayArea().moveVivoFreeformTasksToFullscreenStackLocked(false);
                return true;
            }
        }
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mTask.getWindowingMode() == 1) {
            ActivityRecord topPrevRunningActivity = null;
            TaskDisplayArea displayArea = this.mTask.getDisplayArea();
            if (displayArea != null) {
                int i = displayArea.getChildCount() - 1;
                while (true) {
                    if (i >= 0) {
                        Task task = (ActivityStack) displayArea.getChildAt(i);
                        if (task == this.mTask || task.getWindowingMode() != 1) {
                            i--;
                        } else {
                            topPrevRunningActivity = task.topRunningActivity();
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            int i2 = tr.getChildCount();
            if (i2 > 0 && tr.isLeafTask()) {
                topPrevRunningActivity = (ActivityRecord) tr.getChildAt(tr.getChildCount() - 1);
            }
            if (topPrevRunningActivity != null) {
                this.mTask.adjustFocusToNextFocusableTask("moveTaskToBackLocked(freeform)");
            }
            ActivityRecord topRun = tr.topRunningActivityLocked();
            ActivityStack freeformStack = this.mService.mRootWindowContainer.getVivoFreeformStack();
            if (topRun != null && topRun.isEmergentActivity() && !this.mService.isFreeFormStackMax() && !this.mService.isFreeFormMin() && this.mTask.getDisplayId() == freeformStack.getDisplayId() && displayArea != null) {
                displayArea.setCheckFocus(true);
            }
        }
        return false;
    }

    public void setTopTask(Task top) {
        if (this.mService.isVivoFreeFormValid()) {
            String str = this.TAG;
            VSlog.d(str, "setTopTask is removing task top:" + top + " tasks num:" + this.mTask.getChildCount());
        }
        this.mTopTask = top;
    }

    public void moveFreeformToFullscreenWhenTaskHistoryEmpty(Task task, String reason) {
        ActivityStack freeformStack;
        ActivityStack topFullStack = null;
        if (this.mTask.getDisplayArea() != null) {
            topFullStack = this.mTask.getDisplayArea().getStack(1, 1);
        }
        boolean isPassWordStack = task.realActivity != null && task.realActivity.toString().contains("PasswordActivity") && (reason.contains("appDied") || (this.mTask.mLastPausedActivity != null && this.mTask.mLastPausedActivity.isPasswordActivity()));
        if (this.mService.isVivoFreeFormValid()) {
            String str = this.TAG;
            VSlog.d(str, "moveFreeformToFullscreenWhenTaskHistoryEmpty remove task : " + task + " reason:" + reason + "realactivity = " + task.realActivity + "; mActivityStack.mLastPausedActivity = " + this.mTask.mLastPausedActivity);
        }
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && topFullStack == this.mTask && !reason.contains("moveTaskToStack") && !isPassWordStack && !this.mService.isInDirectFreeformState() && (freeformStack = this.mService.mRootWindowContainer.getVivoFreeformStack()) != null && freeformStack.getDisplayId() == this.mTask.getDisplayId()) {
            String str2 = this.TAG;
            VSlog.d(str2, "Move freeform task to fullscreen stack when game stack is empty. task:" + task + " reason:" + reason);
            this.mService.mRootWindowContainer.getDefaultTaskDisplayArea().moveVivoFreeformTasksToFullscreenStackLocked(true);
        }
    }

    public void moveFreeformToFullscreenWhenTopTaskRemoved(Task task, String reason) {
        ActivityStack freeformStack;
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform()) {
            TaskDisplayArea taskDisplayArea = this.mTask.getDisplayArea();
            ActivityStack topFullsceenStack = taskDisplayArea != null ? taskDisplayArea.getTopStackInWindowingMode(1) : null;
            if (this.mTask.getWindowingMode() == 1 && this.mTask.getActivityType() == 1 && this.mTopTask == topFullsceenStack && reason != null && ((reason.contains("appDied") || reason.contains("finish-idle hadNoApp")) && (freeformStack = this.mService.mRootWindowContainer.getVivoFreeformStack()) != null && this.mTopTask == task && this.mTask.getDisplayId() == freeformStack.getDisplayId() && !this.mService.isInDirectFreeformState() && taskDisplayArea != null)) {
                String str = this.TAG;
                VSlog.d(str, "Move freeform task to fullscreen stack when game died. task:" + task + " reason:" + reason);
                taskDisplayArea.moveVivoFreeformTasksToFullscreenStackLocked(true);
            }
        }
        this.mTopTask = null;
    }

    public void transferActivityStateToTargetStack(Task task) {
        boolean inVivoFreeform = this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform();
        if (!inVivoFreeform || task == null) {
            return;
        }
        if (task.getStack() != null && task.getStack().mPausingActivity != null && task.getStack().mPausingActivity.getTask() == task) {
            this.mTask.mPausingActivity = task.getStack().mPausingActivity;
        }
        if (task.getStack() != null && task.getStack().mResumedActivity != null && task.getStack().mResumedActivity.getTask() == task) {
            this.mTask.mResumedActivity = task.getStack().mResumedActivity;
        }
    }

    public void resumeStateForVivoFreeformInOtherDisplay(ActivityRecord r, String reason, boolean setResume, boolean setPause) {
        if (reason != null && reason.contains("moveFreeformTaskToSecondDisplay") && this.mTask.getDisplayId() == 4096) {
            if (setResume) {
                r.setState(ActivityStack.ActivityState.RESUMED, "moveToFrontAndResumeStateIfNeeded");
            }
            if (setPause) {
                this.mTask.mPausingActivity = r;
                r.schedulePauseTimeout();
            }
        }
    }

    public void moveFreeformWindowStateToTop() {
        if (this.mTask.isRootTask() && !this.mTask.isLeafTask()) {
            for (int taskNdx = this.mTask.getChildCount() - 1; taskNdx >= 0; taskNdx--) {
                Task childAt = this.mTask.getChildAt(taskNdx);
                if (childAt != null && (childAt instanceof ActivityStack)) {
                    Task freeformTask = childAt;
                    if (this.mTask.inFreeformWindowingMode()) {
                        ActivityStack parent = freeformTask.getParent();
                        ActivityStack activityStack = this.mTask;
                        if (parent == activityStack) {
                            activityStack.positionChildAtTop(freeformTask);
                        }
                    }
                }
            }
        }
    }

    public void setPausingActivity(ActivityRecord r) {
        this.mTask.mPausingActivity = r;
        r.schedulePauseTimeout();
    }

    public void resetCheckFocusState() {
        this.checkFocus = false;
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public void initFreeformTaskRotationIfNeed() {
        DisplayContent displayContent;
        if (this.mService.isVivoFreeFormValid() && this.mTask.isLeafTask() && this.mTask.getRootTask().inFreeformWindowingMode() && (displayContent = this.mTask.getRootTask().getDisplayContent()) != null) {
            this.mTask.mRotation = displayContent.getDisplayInfo().rotation;
        }
    }

    public void updateFreeformSurfacePositonIfNeed() {
        ActivityTaskManagerService activityTaskManagerService = this.mService;
        if (activityTaskManagerService != null && activityTaskManagerService.isVivoFreeFormValid() && this.mTask.isLeafTask() && this.mTask.inFreeformWindowingMode()) {
            this.mTask.updateSurfacePosition();
        }
    }

    public boolean ignoreUpdateRotationWhenResizingTask(int newRotation, int boundsChange) {
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mTask.isLeafTask() && this.mTask.inFreeformWindowingMode() && this.mService.mWindowManager.isResizingTask() && newRotation != this.mTask.mRotation && boundsChange == 0) {
            return true;
        }
        return false;
    }

    public boolean fillsParentInFreeform() {
        ActivityTaskManagerService activityTaskManagerService = this.mService;
        if (activityTaskManagerService != null && activityTaskManagerService.isVivoFreeFormValid() && this.mTask.isLeafTask() && this.mTask.getWindowConfiguration().canResizeTask() && this.mService.isFreeFormStackMax()) {
            return true;
        }
        return false;
    }

    public void setIsSnapShotWhenExitFreeform(boolean shot) {
        if (this.mTask.isLeafTask()) {
            this.mIsSnapShotWhenExitFreeform = shot;
        }
    }

    public boolean getIsSnapShotWhenExitFreeform() {
        if (this.mTask.isLeafTask()) {
            return this.mIsSnapShotWhenExitFreeform;
        }
        return false;
    }

    public boolean ignoreSurfaceLayoutDefer() {
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mService.mWindowManager.isFreeFormResizing() && this.mTask.isLeafTask()) {
            this.mIgnoreSurfaceLayoutDefer = true;
            return true;
        }
        return false;
    }

    public void stopFreezingWhenFreeformResize(ActivityRecord r) {
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mService.mWindowManager.isFreeFormResizing() && this.mTask.isLeafTask()) {
            r.stopFreezingScreenLocked(true);
        }
    }

    public boolean hasIgnoreSurfaceLayoutDefer() {
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mTask.isLeafTask() && this.mIgnoreSurfaceLayoutDefer) {
            this.mIgnoreSurfaceLayoutDefer = false;
            return true;
        }
        return false;
    }

    public Rect getLastBounds() {
        return this.mLastBounds;
    }

    public void setLastBounds(Rect lastBounds) {
        if (this.mTask.isLeafTask()) {
            this.mLastBounds = lastBounds;
        }
    }

    public void updateLastBoundsWhenUpdateConfig(Rect bounds) {
        if (this.mTask.isLeafTask()) {
            boolean matchParentBounds = bounds == null || bounds.isEmpty();
            if (matchParentBounds) {
                setLastBounds(null);
                return;
            }
            if (getLastBounds() == null) {
                setLastBounds(new Rect(this.mTask.getBounds()));
            } else {
                getLastBounds().set(this.mTask.getBounds());
            }
            if (this.mTask.inFreeformWindowingMode()) {
                if (bounds.width() != getLastBounds().width() || bounds.height() != getLastBounds().height()) {
                    startFreezingWhenFreeformResize();
                }
            }
        }
    }

    private void startFreezingWhenFreeformResize() {
        ActivityRecord top;
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mService.mWindowManager.isFreeFormResizing() && this.mTask.isLeafTask() && (top = this.mTask.topRunningActivityLocked()) != null) {
            top.startFreezingScreenLocked(top.app, (int) Consts.ProcessStates.FOCUS);
        }
    }

    public void limitBoundsForVivoFreeform(Rect bounds) {
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mTask.inFreeformWindowingMode() && this.mTask.isLeafTask() && !this.mService.isFreeFormStackMax() && bounds != null) {
            DisplayContent displayContent = this.mService.mRootWindowContainer.getTopFocusedDisplayContent();
            if (displayContent == null) {
                displayContent = this.mService.mRootWindowContainer.getDefaultDisplay();
            }
            int rot = this.mService.getDisplayRotation(displayContent);
            if (rot == 0 && bounds.top > this.mService.mWindowManager.getPortraitLimitedTop() && this.mService.mWindowManager.getPortraitLimitedTop() != -1) {
                int height = bounds.height();
                bounds.top = this.mService.mWindowManager.getPortraitLimitedTop();
                bounds.bottom = bounds.top + height;
            } else if ((rot == 1 || rot == 3) && bounds.left > this.mService.mWindowManager.getLandLimitedLeft(rot) && this.mService.mWindowManager.getLandLimitedLeft(rot) != -1) {
                int width = bounds.width();
                bounds.left = this.mService.mWindowManager.getLandLimitedLeft(rot);
                bounds.right = bounds.left + width;
            }
            Rect curBounds = this.mTask.getRequestedOverrideBounds();
            if (curBounds != null && curBounds.height() != bounds.height() && bounds.top > this.mService.mWindowManager.getPortraitLimitedTop() && this.mService.mWindowManager.getPortraitLimitedTop() != -1) {
                int height2 = curBounds.height();
                bounds.top = this.mService.mWindowManager.getPortraitLimitedTop();
                bounds.bottom = bounds.top + height2;
            }
        }
    }

    public void setResizingTask(int resizeMode, ActivityStack stack) {
        if (this.mTask.isLeafTask()) {
            this.mService.setResizeTaskFreeform(false);
            this.mService.mWindowManager.setResizingTask(false);
            this.mTmpConfig.setTo(this.mTask.getRequestedOverrideConfiguration());
            if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && stack != null && stack.inFreeformWindowingMode() && resizeMode == 1) {
                this.mService.setResizeTaskFreeform(true);
                this.mService.mWindowManager.setResizingTask(true);
            }
        }
    }

    public void forceNewConfigForSpecial(boolean forced) {
        ActivityRecord temp;
        ActivityRecord r = this.mTask.topRunningActivityLocked();
        Rect originBounds = this.mTmpConfig.windowConfiguration.getBounds();
        Rect curBounds = this.mTask.getRequestedOverrideBounds();
        if (this.mTask.isLeafTask() && r != null && r.mActivityComponent != null && !curBounds.equals(originBounds) && Constant.APP_WEIXIN.equals(r.mActivityComponent.getPackageName())) {
            if (this.mService.mWindowManager.isExitingFreeForm()) {
                r.forceNewConfig = true;
                if (r.getDisplayId() == 0) {
                    r.setIgnoreRelaunch(false);
                }
            } else if (this.mService.mWindowManager.isEnteringFreeForm()) {
                r.forceNewConfig = false;
                if (this.mService.isVivoFreeFormValid() && (this.mService.isInDirectFreeformState() || this.mService.isLastExitFromDirectFreeform())) {
                    this.mService.setLastExitFromDirectFreeform(false);
                    r.forceNewConfig = forced;
                } else {
                    r.forceNewConfig = false;
                }
                for (int index = this.mTask.getChildCount() - 1; index >= 0; index--) {
                    WindowContainer windowContainer = this.mTask.getChildAt(index);
                    if (windowContainer != null && (windowContainer instanceof ActivityRecord) && (temp = (ActivityRecord) windowContainer) != r) {
                        temp.forceNewConfig = forced;
                    }
                }
            }
        }
        this.mTmpConfig.unset();
    }

    public void resetResizingTask() {
        this.mService.setResizeTaskFreeform(false);
        this.mService.mWindowManager.setResizingTask(false);
    }

    public boolean isFromVivoFreeform() {
        if (this.mTask.isLeafTask()) {
            return this.fromVivoFreeform;
        }
        return false;
    }

    public void setFromVivoFreeform(boolean fromVivoFreeform) {
        if (this.mTask.isLeafTask()) {
            this.fromVivoFreeform = fromVivoFreeform;
        }
    }

    public boolean isShouldGoHomeWhenFinish() {
        return this.shouldGoHomeWhenFinish;
    }

    public void setShouldGoHomeWhenFinish(boolean shouldGoHomeWhenFinish) {
        this.shouldGoHomeWhenFinish = shouldGoHomeWhenFinish;
    }

    public void reparentFromFreeformToFullscreen(ActivityStack sourceStack, int toStackWindowingMode, ActivityStack toStack, int position, String reason, int moveStackMode, boolean wasFocused, boolean wasFront, ActivityRecord r) {
        if (!this.mTask.isLeafTask()) {
            return;
        }
        if (reason != null && reason.contains("moveFreeformTaskToSecondDisplay")) {
            this.mService.mWindowManager.setFreeformMovedToSecondDisplay();
        }
        this.mTopFinishingInFreeform = null;
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && sourceStack.inFreeformWindowingMode() && toStackWindowingMode == 1) {
            setFromVivoFreeform(true);
            if (r != null) {
                r.setIsFromFreeform(true);
            } else if (this.mTask.getChildCount() >= 1) {
                Task task = this.mTask;
                ActivityRecord top = task.getChildAt(task.getChildCount() - 1);
                if (top != null) {
                    top.setIsFromFreeform(true);
                }
            }
            if (position == Integer.MAX_VALUE && "exitFreeformMode".equals(reason)) {
                toStack.startPausingLocked(false, false, (ActivityRecord) null);
                if (moveStackMode == 1 && (wasFocused || wasFront)) {
                    setShouldGoHomeWhenFinish(true);
                }
                this.mService.mWindowManager.setIsFreeformExitToTop(true);
            }
            if (this.mService.isInDirectFreeformState()) {
                this.mIsExtingDirectFreeform = true;
            }
            if (this.mTask.getChildCount() > 0) {
                Task task2 = this.mTask;
                ActivityRecord top2 = task2.getChildAt(task2.getChildCount() - 1);
                if (top2 != null && top2.getState() == ActivityStack.ActivityState.PAUSING && sourceStack.mPausingActivity == top2 && top2.finishing && this.mService.mRootWindowContainer.isTopDisplayFocusedStack(sourceStack)) {
                    this.mTopFinishingInFreeform = top2;
                }
            }
        }
        if (this.mService.isVivoFreeFormValid() && sourceStack.getWindowingMode() == 1 && toStackWindowingMode == 5 && "reparentToDisplay".equals(reason) && position == Integer.MAX_VALUE && moveStackMode == 0 && r != null && r.getState() != ActivityStack.ActivityState.RESUMED && r.isVisible()) {
            r.setVisible(false);
        }
    }

    public void transferStateWhenHasFinishingActivity(ActivityStack toStack, String reason) {
        if (this.mTask.isLeafTask() && this.mTopFinishingInFreeform != null && this.mService.mWindowManager.isExitingFreeForm()) {
            toStack.moveToFrontAndResumeStateIfNeeded(this.mTopFinishingInFreeform, true, false, true, reason);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:27:0x004e  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0053  */
    /* JADX WARN: Removed duplicated region for block: B:30:0x0056  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean transferStateWhenExitingFreeform(com.android.server.wm.ActivityRecord r15, com.android.server.wm.ActivityStack r16, com.android.server.wm.ActivityStack r17, int r18, boolean r19, boolean r20, boolean r21, int r22, java.lang.String r23) {
        /*
            r14 = this;
            r0 = r14
            r1 = r23
            com.android.server.wm.Task r2 = r0.mTask
            boolean r2 = r2.isLeafTask()
            r3 = 0
            if (r2 != 0) goto Ld
            return r3
        Ld:
            com.android.server.wm.TaskDisplayArea r2 = r16.getDisplayArea()
            r4 = 0
            if (r2 == 0) goto L19
            com.android.server.wm.ActivityStack r5 = r2.getLastFocusedStack()
            goto L1a
        L19:
            r5 = r4
        L1a:
            boolean r6 = r16.inFreeformWindowingMode()
            r7 = 1
            if (r6 == 0) goto L47
            r6 = r18
            if (r6 != r7) goto L44
            com.android.server.wm.ActivityTaskManagerService r8 = r0.mService
            com.android.server.wm.WindowManagerService r8 = r8.mWindowManager
            boolean r8 = r8.isExitingFreeForm()
            if (r8 == 0) goto L41
            r8 = r16
            if (r5 == r8) goto L35
            if (r19 == 0) goto L4b
        L35:
            if (r1 == 0) goto L4b
            java.lang.String r9 = "moveTasksToFullscreenStack"
            boolean r9 = r1.contains(r9)
            if (r9 == 0) goto L4b
            r9 = r7
            goto L4c
        L41:
            r8 = r16
            goto L4b
        L44:
            r8 = r16
            goto L4b
        L47:
            r8 = r16
            r6 = r18
        L4b:
            r9 = r3
        L4c:
            if (r2 == 0) goto L53
            com.android.server.wm.ActivityStack r10 = r2.getRootHomeTask()
            goto L54
        L53:
            r10 = r4
        L54:
            if (r10 == 0) goto L5a
            com.android.server.wm.ActivityRecord r4 = r10.topRunningActivityLocked()
        L5a:
            if (r9 == 0) goto L6d
            if (r4 == 0) goto L6d
            boolean r11 = r4.nowVisible
            if (r11 == 0) goto L6d
            if (r21 != 0) goto L6d
            r11 = 2147483647(0x7fffffff, float:NaN)
            r12 = r22
            if (r12 == r11) goto L6f
            r9 = 0
            goto L6f
        L6d:
            r12 = r22
        L6f:
            if (r9 == 0) goto L7a
            if (r20 == 0) goto L7a
            r11 = r15
            r13 = r17
            r13.setPausingActivity(r15)
            return r7
        L7a:
            r11 = r15
            r13 = r17
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoTaskImpl.transferStateWhenExitingFreeform(com.android.server.wm.ActivityRecord, com.android.server.wm.ActivityStack, com.android.server.wm.ActivityStack, int, boolean, boolean, boolean, int, java.lang.String):boolean");
    }

    public void forceIgnoreRelaunchForSpecial(int sourceStackWindowingMode, String reason) {
        ActivityRecord r;
        if (this.mTask.isLeafTask() && (r = this.mTask.topRunningActivityLocked()) != null && r.mActivityComponent != null && reason != null && Constant.APP_WEIXIN.equals(r.mActivityComponent.getPackageName()) && reason.contains("moveTasksToFullscreenStack") && this.mService.mWindowManager.isExitingFreeForm() && sourceStackWindowingMode == 5 && r.getDisplayId() != 0) {
            String str = this.TAG;
            VSlog.d(str, "forceIgnoreRelaunchForSpecial reason = " + reason + ": sourceStackWindowingMode = " + sourceStackWindowingMode);
            r.setIgnoreRelaunch(true);
        }
    }

    public boolean isVivoFreformValid() {
        if (!this.mTask.isLeafTask()) {
            return false;
        }
        return this.mService.isVivoFreeFormValid();
    }

    public boolean resizeWhenReparentInFreeform(ActivityStack toStack, boolean mightReplaceWindow, boolean deferResume) {
        if (!this.mService.isVivoFreeFormValid()) {
            return true;
        }
        boolean kept = this.mTask.resize(toStack.getRequestedOverrideBounds(), mightReplaceWindow ? 2 : 0, !mightReplaceWindow);
        return kept;
    }

    public void setFreeformKeepR(ActivityRecord r) {
        if (!this.mTask.isLeafTask()) {
            return;
        }
        this.mService.setFreeformKeepR(r);
    }

    public boolean changeTaskResizeableForVivoFreeform() {
        if (this.mTask.isLeafTask()) {
            boolean inFreeform = this.mService.isVivoFreeFormValid() && (this.mTask.inFreeformWindowingMode() || (this.mService.mWindowManager.isEnteringFreeForm() && isSpecialPackageForFreeform()));
            if (this.mIsExtingDirectFreeform) {
                this.mIsExtingDirectFreeform = false;
            }
            if (!inFreeform && this.isfreeformExit && isSpecialPackageForFreeform()) {
                return true;
            }
            return inFreeform;
        }
        return false;
    }

    private boolean isSpecialPackageForFreeform() {
        ActivityRecord root = this.mTask.getRootActivity();
        ActivityRecord top = this.mTask.getTopMostActivity();
        String rootPackage = root != null ? root.packageName : null;
        String topPackage = top != null ? top.packageName : null;
        return this.mFreeformUnResizableList.contains(rootPackage) || this.mFreeformUnResizableList.contains(topPackage);
    }

    public boolean skipCheckOrientationForVivoFreeform() {
        if (this.mService.isVivoFreeFormValid() && this.mTask.isLeafTask() && this.mTask.getStack() != null && this.mTask.getStack().inFreeformWindowingMode()) {
            return true;
        }
        return false;
    }

    public void updateActivitiesRequestConfigUseBounds(Rect bounds) {
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mTask.isLeafTask()) {
            for (int activityNdx = this.mTask.getChildCount() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord activityRecord = this.mTask.getChildAt(activityNdx);
                if (activityRecord != null && !ActivityInfo.isResizeableMode(activityRecord.info.resizeMode) && !activityRecord.finishing) {
                    activityRecord.updateRequestOverrideConfigUseBounds(bounds);
                }
            }
        }
    }

    public void restoreActivitiesRequestConfigIfNeed() {
        if (this.isfreeformExit && this.mTask.isLeafTask() && this.mTask.isFromVivoFreeform()) {
            for (int activityNdx = this.mTask.getChildCount() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord activityRecord = this.mTask.getChildAt(activityNdx);
                if (activityRecord != null && !ActivityInfo.isResizeableMode(activityRecord.info.resizeMode) && activityRecord.hasOverrideConfiguration() && !activityRecord.finishing) {
                    activityRecord.updateRequestOverrideConfigUseBounds(new Rect(0, 0, 0, 0));
                }
            }
        }
    }

    public boolean isRotationChangingAndMismatch(Configuration newParentConfig, DisplayContent display) {
        return this.mTask.isLeafTask() && this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && display != null && display.getDisplayRotation().isRotationChanging() && display.mDisplayFrames.mRotation != newParentConfig.windowConfiguration.getRotation();
    }

    public boolean isEmergentTask() {
        ActivityRecord topRunning;
        return this.mTask.isLeafTask() && (topRunning = this.mTask.topRunningActivityLocked()) != null && topRunning.isEmergentActivity();
    }

    public boolean isFreeFormFullScreenApps() {
        return this.mTask.isLeafTask() && this.mTask.realActivity != null && this.mService.getFreeFormFullScreenApp().contains(this.mTask.realActivity.getPackageName());
    }

    public boolean isFreeFormEnabledApps() {
        return this.mTask.isLeafTask() && this.mTask.realActivity != null && this.mService.getFreeFormEnabledApp().contains(this.mTask.realActivity.getPackageName());
    }

    public void checkIfToggleFreeform(int targetWindowingMode) {
        TaskDisplayArea taskDisplayArea;
        if (this.mService.isVivoFreeFormValid() && this.mTask.isRootTask()) {
            if (this.mTask.inFreeformWindowingMode() && targetWindowingMode == 1) {
                this.isfreeformExit = true;
                this.isDirectFreeformExit = this.mService.isInDirectFreeformState();
                setFromVivoFreeform(true);
                ActivityRecord top = this.mTask.topRunningActivityLocked();
                if (top != null) {
                    top.setIsFromFreeform(true);
                }
                if (this.mTask.isFocusedStackOnDisplay()) {
                    setShouldGoHomeWhenFinish(true);
                    this.mService.mWindowManager.setIsFreeformExitToTop(true);
                }
                if (this.mService.isInDirectFreeformState()) {
                    this.mIsExtingDirectFreeform = true;
                }
                vivoResetFreeFormStatusIfNeed();
            } else if (targetWindowingMode == 5 && (taskDisplayArea = this.mTask.getDisplayArea()) != null) {
                taskDisplayArea.setFreeformSettingsState((Task) null, targetWindowingMode, this.mTask.getActivityType(), 1);
                this.mService.mWindowManager.setEnteringFreeForm(true);
            }
        }
    }

    public void processAfterToggledFreeformIfNeed(int targetWindowingMode) {
        TaskDisplayArea taskDisplayArea;
        if (this.isfreeformExit && this.mService.mWindowManager.isVivoFreeformFeatureSupport()) {
            this.isfreeformExit = false;
            ActivityRecord r = this.mTask.topRunningActivityLocked();
            if (r != null && r.getState() == ActivityStack.ActivityState.RESUMED && !this.mTask.isForceHidden() && r.app != null) {
                this.mService.mAmInternal.sendProcessActivityChangeMessage(r.app.getPid(), r.info.applicationInfo.uid);
            }
            EventLogTags.writeWmTaskRemoved(this.mTask.getRootTaskId(), "freeform stack toggled");
            setExitingFreeformStateIfNeed(false);
            String pkg = r != null ? r.packageName : null;
            VCD_FF_1.VCD_FF_3(this.mService.mContext, this.isDirectFreeformExit, pkg);
            this.isDirectFreeformExit = false;
        } else if (this.mService.isVivoFreeFormValid() && targetWindowingMode == 5 && this.mTask.inFreeformWindowingMode() && (taskDisplayArea = this.mTask.getDisplayArea()) != null) {
            taskDisplayArea.initFreeformStatusIfNeed(this.mTask, targetWindowingMode, false);
        }
    }

    public void adjustFreeformDimBound(Rect out) {
        if (this.mTask.inFreeformWindowingMode() && this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform()) {
            float globalScale = this.mService.mWindowManager.getFreeformScale();
            int scaleWidth = (int) (out.width() * globalScale);
            int scaleHeight = (int) (out.height() * globalScale);
            out.right = out.left + scaleWidth;
            out.bottom = out.top + scaleHeight;
        }
    }

    public boolean shouldChangeLayerWhenToggleFreeform() {
        if (((this.mService.isVivoFreeFormValid() && this.mTask.inFreeformWindowingMode()) || this.isfreeformExit) && this.mTask.isChangingAppTransition()) {
            return true;
        }
        return false;
    }

    public void notifyAppRequestOrientation(int orientation) {
        ActivityRecord activityRecord = this.mTask.getTopMostActivity();
        if (activityRecord == null || !activityRecord.isSpecialVideoActivityCantGoLandMode()) {
            if (orientation == 0) {
                Settings.System.putIntForUser(this.mService.mContext.getContentResolver(), "freeform_fullscreen_mode", 1, -2);
                VivoFreeformUtils.inFullscreenMode = true;
                this.mService.mWindowManager.freezingMultiWindow((int) ProcessList.BACKUP_APP_ADJ);
            } else if (orientation == 1) {
                Settings.System.putIntForUser(this.mService.mContext.getContentResolver(), "freeform_fullscreen_mode", 0, -2);
                VivoFreeformUtils.inFullscreenMode = false;
                this.mService.mWindowManager.freezingMultiWindow((int) ProcessList.BACKUP_APP_ADJ);
            }
        }
    }

    public boolean checkFreeFormBehindFullscreenActivityState(ActivityRecord r, ActivityRecord top, boolean isTop) {
        return this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mTask.inFreeformWindowingMode() && !isTop && top != null && r != null && top.visibleIgnoringKeyguard && top.occludesParent() && top.mVisibleRequested && r.getParent() == top.getParent();
    }

    private int getWindowingMode() {
        Task task = this.mTask;
        if (task != null) {
            return task.getWindowingMode();
        }
        return 0;
    }

    public boolean isTemporaryFullscreenOfMultiWindow(boolean isOpaqueSplitScreenPrimary, boolean isCheckSplitScreenSecondary) {
        Task task;
        ActivityTaskManagerService activityTaskManagerService = this.mService;
        if (activityTaskManagerService != null && activityTaskManagerService.isMultiWindowSupport() && this.mService.isVivoEnteringMultiWindowDefaultDisplay() && isOpaqueSplitScreenPrimary && !isCheckSplitScreenSecondary && this.mTask.getWindowingMode() == 1 && (task = this.mTask) != null && task.supportsSplitScreenWindowingMode()) {
            if (this.mService.isSplitLogDebug()) {
                String str = this.TAG;
                VSlog.d(str, this.mTask.mTaskId + " is temporary fullscreen state");
            }
            return true;
        }
        return false;
    }

    public boolean ignoreUpdateSurafaceSizeInSplit(int prevWinMode) {
        Task task;
        ActivityTaskManagerService activityTaskManagerService = this.mService;
        if (activityTaskManagerService != null && activityTaskManagerService.isMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_ENTER_SPLIT_OPTIMIZE && this.mService.isVivoEnteringMultiWindowDefaultDisplay() && !WindowConfiguration.isSplitScreenWindowingMode(prevWinMode) && (task = this.mTask) != null && task.getDisplayId() == 0 && this.mTask.isAttached() && !this.mTask.isRootTask() && this.mTask.inSplitScreenPrimaryWindowingMode()) {
            ActivityStack stack = this.mTask.getStack();
            String way = this.mService.getVivoEnterSplitWay();
            if (stack.getChildCount() == 1 && way != null && "reas_tf".equals(way)) {
                String str = this.TAG;
                VSlog.d(str, this.mTask.mTaskId + " need temporary ignoreUpdateSurafaceSizeInSplit");
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean needShowRecentApps() {
        if (this.mService.isVivoMultiWindowSupport() && this.mService.isSplittingScreenByVivo()) {
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.d(this.TAG, "vivo_multiwindow_fmk setWindowingMode: skip call showRecentApps");
                return false;
            }
            return false;
        } else if (VivoMultiWindowConfig.DEBUG) {
            VSlog.d(this.TAG, "vivo_multiwindow_fmk setWindowingMode: will call showRecentApps");
            return true;
        } else {
            return true;
        }
    }

    public boolean shouldDeferAnimationFinish(final Runnable endDeferFinishCallback) {
        boolean shouldDefer = false;
        if (this.mService.isVivoVosMultiWindowSupport() && this.mService.isInMultiWindowDefaultDisplay() && this.mTask.getDisplayId() == 0 && this.mTask.inSplitScreenSecondaryWindowingMode() && !this.mTask.isActivityTypeHome()) {
            ActivityStack rootHomeTask = this.mTask.getDisplayArea().getRootHomeTask();
            if (this.mTask.isVisible() && rootHomeTask != null && rootHomeTask.isAnimating() && !rootHomeTask.isVisible() && this.mTask.getDisplayContent() != null && !this.mTask.getDisplayContent().getDockedDividerController().isResizing()) {
                shouldDefer = true;
                if (VivoMultiWindowConfig.DEBUG) {
                    String str = this.TAG;
                    VSlog.d(str, "shouldDeferAnimationFinish for " + this.mTask + ",to defer remove animate leash");
                }
                this.mService.mWindowManager.mH.postDelayed(new Runnable() { // from class: com.android.server.wm.VivoTaskImpl.1
                    @Override // java.lang.Runnable
                    public void run() {
                        synchronized (VivoTaskImpl.this.mService.mWindowManager.mGlobalLock) {
                            endDeferFinishCallback.run();
                        }
                    }
                }, 50L);
            }
        }
        return shouldDefer;
    }

    public void debugSplitBoundsIfNeeded(int prevWinMode, int windowingMode, Rect prevBounds, Rect bounds, int taskId, int rootTaskId) {
        if (VivoMultiWindowConfig.DEBUG) {
            if (prevWinMode == 3 || prevWinMode == 4 || prevWinMode == 2 || windowingMode == 3 || windowingMode == 4 || windowingMode == 2 || VivoMultiWindowConfig.IS_VIVO_FORCE_DEBUG_TASK_BOUNDS || this.mService.isInMultiWindowDefaultDisplay()) {
                String str = this.TAG;
                VSlog.d(str, "debugSplitBoundsIfNeeded windowingMode:" + prevWinMode + " ==> " + windowingMode + " bounds:" + prevBounds + " ==> " + bounds + " taskId:" + taskId + " rootTaskId:" + rootTaskId, new Throwable("onConfigurationChanged"));
            }
        }
    }

    public void updateVivoOccludeStatus(ActivityRecord next) {
        if (next != null) {
            String str = this.TAG;
            Slog.w(str, "occlude resumeTop canOccclude = " + this.mService.vivoCanOccludeKeyguard(next.packageName, next.isVivoOccludeKeyguard()) + " next.occlude = " + next.isVivoOccludeKeyguard());
            if (this.mService.isKeyguardLocked() && this.mService.vivoCanOccludeKeyguard(next.packageName, next.isVivoOccludeKeyguard())) {
                next.setVivoOccludeKeyguard(true);
            } else {
                next.setVivoOccludeKeyguard(false);
            }
        }
    }

    public boolean vivoStackFinishForSplit(ActivityRecord r, int displayId) {
        if (!this.mService.isMultiWindowSupport() || !VivoMultiWindowConfig.IS_VIVO_SPLIT_BACK_PROCESS || displayId != 0 || !this.mService.isInMultiWindowDefaultDisplay() || r == null || r.getTask() == null || r.getStack() == null || r.isMultiwindowPasswrdActivity() || r.getTask().getWindowingMode() != 3 || r.getTask().getChildCount() > 1) {
            return false;
        }
        Task task = this.mTask;
        ActivityStack secondaryStack = task != null ? task.mRootWindowContainer.getDefaultTaskDisplayArea().getTopStackInWindowingMode(4) : null;
        ActivityRecord secondaryRecord = secondaryStack != null ? secondaryStack.getTopMostActivity() : null;
        if (VivoMultiWindowConfig.DEBUG) {
            String str = this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("vivo_multiwindow_fmk finishActivityLocked finish exit split for finishActivity: ");
            sb.append(r);
            sb.append(" task: ");
            sb.append(r != null ? r.getTask() : null);
            sb.append(" stack: ");
            sb.append(r != null ? r.getStack() : null);
            sb.append(" secondaryrecord: ");
            sb.append(secondaryRecord);
            sb.append("  ");
            sb.append(secondaryRecord != null ? secondaryRecord.isMultiWindowAppListActivity() : false);
            sb.append("   secondaryStack: ");
            sb.append(secondaryStack);
            sb.append(" isSpecial: ");
            sb.append(VivoMultiWindowConfig.getInstance().isSpecialFreezingActivity(r.toString()));
            sb.append("  ");
            sb.append(Debug.getCallers(6));
            VSlog.i(str, sb.toString());
        }
        VivoMultiWindowConfig.getInstance().splitBackEventThreadRun(this.mService.mContext);
        if (secondaryRecord != null && (!secondaryRecord.isMultiWindowAppListActivity() || (secondaryRecord.isMultiWindowAppListActivity() && !VivoMultiWindowConfig.getInstance().isSpecialFreezingActivity(r.toString())))) {
            if (secondaryRecord.isMultiWindowAppListActivity()) {
                this.mTask.mStackSupervisor.handleNonResizableAnim(this.mTask.mStackSupervisor.mRootWindowContainer.getDefaultDisplayHomeActivityForUser(this.mService.mAmInternal.getCurrentUserId()));
            } else if (secondaryRecord.isActivityTypeHome()) {
                this.mTask.mStackSupervisor.handleNonResizableAnimNoTarget(1);
                if (VivoMultiWindowConfig.DEBUG) {
                    String str2 = this.TAG;
                    VSlog.i(str2, "vivo_multiwindow_fmk finish when second is home and r is " + r);
                }
            } else {
                this.mTask.mStackSupervisor.handleNonResizableAnim(secondaryRecord, 4);
            }
        } else if (secondaryRecord == null || (secondaryRecord.isMultiWindowAppListActivity() && VivoMultiWindowConfig.getInstance().isSpecialFreezingActivity(r.toString()))) {
            int maxFinishAppStartFreezingScreenDuration = SystemProperties.getInt("persist.vivo.split_finish_resizefreezetime", (int) ProcessList.PREVIOUS_APP_ADJ);
            this.mTask.mStackSupervisor.mWindowManager.startFreezingSplitWindowDirect(maxFinishAppStartFreezingScreenDuration, "finishAppFreezingScreen");
            if (secondaryRecord == null) {
                String str3 = this.TAG;
                VSlog.i(str3, "vivo_multiwindow_fmk finish startFreezingScreen, secondaryrecord get null, when finish " + r);
            } else {
                String str4 = this.TAG;
                VSlog.i(str4, "vivo_multiwindow_fmk finish startFreezingScreen, special activity is " + r);
            }
        }
        return true;
    }

    public boolean vivoStackMoveTaskBackForSplit(Task tr, int displayId) {
        ActivityStack otherPrimaryStack;
        ActivityRecord primaryRecord;
        boolean isSoftLockSpecial = VivoMultiWindowConfig.IS_VIVO_SPLIT_SOFTLOCK_SPECIAL;
        if (this.mService.isMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_BACK_PROCESS && displayId == 0 && this.mTask != null && this.mService.isInMultiWindowDefaultDisplay()) {
            if (getWindowingMode() == 4 && tr != null && tr.getTopMostActivity() != null && ((tr.getTopMostActivity().isMultiWindowAppListActivity() || (isSoftLockSpecial && tr.getTopMostActivity().isMultiwindowPasswrdActivity())) && (otherPrimaryStack = this.mTask.mRootWindowContainer.getDefaultTaskDisplayArea().getTopStackInWindowingMode(3)) != null && (primaryRecord = otherPrimaryStack.getTopMostActivity()) != null)) {
                VivoMultiWindowConfig.getInstance().splitBackEventThreadRun(this.mService.mContext);
                if (isSoftLockSpecial && tr.getTopMostActivity().isMultiwindowPasswrdActivity() && primaryRecord.isMultiwindowPasswrdActivity()) {
                    this.mTask.mStackSupervisor.handleNonResizableAnimNoTarget(4);
                } else {
                    this.mTask.mStackSupervisor.handleNonResizableAnim(primaryRecord, 4);
                    this.mService.setSkipActivityRelaunch(primaryRecord);
                }
                this.mTask.mRootWindowContainer.getDefaultTaskDisplayArea().onSplitScreenModeDismissed(primaryRecord.getTask());
                if (VivoMultiWindowConfig.DEBUG) {
                    String str = this.TAG;
                    VSlog.i(str, "vivo_multiwindow_fmk vivoStackMoveTaskBackForSplit now windowingmode is secondary and primaryrecord is " + primaryRecord + " " + Debug.getCallers(5));
                }
                return true;
            } else if (getWindowingMode() == 3) {
                Task task = this.mTask;
                ActivityStack otherSecondStack = task != null ? task.mRootWindowContainer.getDefaultTaskDisplayArea().getTopStackInWindowingMode(4) : null;
                ActivityRecord secondRecord = otherSecondStack != null ? otherSecondStack.getTopMostActivity() : null;
                if (secondRecord != null) {
                    VivoMultiWindowConfig.getInstance().splitBackEventThreadRun(this.mService.mContext);
                    if (secondRecord.isMultiWindowAppListActivity()) {
                        this.mTask.mStackSupervisor.handleNonResizableAnim(this.mTask.mStackSupervisor.mRootWindowContainer.getDefaultDisplayHomeActivityForUser(this.mService.mAmInternal.getCurrentUserId()));
                    } else if (isSoftLockSpecial && secondRecord.isMultiwindowPasswrdActivity() && tr != null && tr.getTopMostActivity() != null && tr.getTopMostActivity().isMultiwindowPasswrdActivity()) {
                        this.mTask.mStackSupervisor.handleNonResizableAnimNoTarget(4);
                    } else {
                        this.mTask.mStackSupervisor.handleNonResizableAnim(secondRecord, 4);
                        this.mService.setSkipActivityRelaunch(secondRecord);
                    }
                    if (secondRecord.isMultiWindowAppListActivity()) {
                        this.mTask.mRootWindowContainer.getDefaultTaskDisplayArea().setMoveHomeToFrontWhenDissmissSplit();
                        this.mTask.mRootWindowContainer.getDefaultTaskDisplayArea().onSplitScreenModeDismissed();
                    } else {
                        this.mTask.mRootWindowContainer.getDefaultTaskDisplayArea().onSplitScreenModeDismissed(secondRecord.getTask());
                    }
                    if (VivoMultiWindowConfig.DEBUG) {
                        String str2 = this.TAG;
                        VSlog.i(str2, "vivo_split_fmk vivoStackMoveTaskBackForSplit now windowingmode is primary and secondRecord is " + secondRecord + " " + Debug.getCallers(5));
                    }
                    return true;
                }
                return false;
            } else {
                return false;
            }
        }
        return false;
    }

    public void adjustTaskOrderInSplitIfNeeded(Task target, ActivityRecord record, int displayId, String reason) {
        if (this.mService.isVivoMultiWindowSupport() && displayId == 0 && this.mTask != null && reason != null && this.mService.isInMultiWindowDefaultDisplay() && target.inSplitScreenSecondaryWindowingMode() && !target.isActivityTypeHome() && !target.isActivityTypeRecents()) {
            if ((target.getTopMostActivity() == null || !target.getTopMostActivity().isMultiWindowAppListActivity()) && !target.isRootTask()) {
                if (reason.contains("finish") && (target.topRunningActivityLocked() != null || (record != null && !record.isVisible()))) {
                    if (VivoMultiWindowConfig.DEBUG) {
                        String str = this.TAG;
                        StringBuilder sb = new StringBuilder();
                        sb.append("adjustTaskOrderInSplitIfNeeded return of finish record vis is ");
                        sb.append(record != null ? Boolean.valueOf(record.isVisible()) : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                        VSlog.d(str, sb.toString());
                        return;
                    }
                    return;
                }
                TaskDisplayArea area = this.mTask.mRootWindowContainer.getDefaultTaskDisplayArea();
                if (area != null) {
                    Task secondaryStack = area.getRootSecondaryTask();
                    Task multiActivityTask = area.getMiniLauncherTask();
                    ActivityStack rootHomeTask = area.getRootHomeTask();
                    ActivityStack rootRecentsTask = area.getRootRecentsTask();
                    if (multiActivityTask != null && multiActivityTask.isDescendantOf(secondaryStack) && rootHomeTask != null && rootHomeTask.isDescendantOf(secondaryStack)) {
                        if (rootRecentsTask == null || rootRecentsTask.isDescendantOf(secondaryStack)) {
                            int multiIndex = secondaryStack.mChildren.indexOf(multiActivityTask);
                            int homeOrRecetnsIndex = rootRecentsTask != null ? Math.max(secondaryStack.mChildren.indexOf(rootHomeTask), secondaryStack.mChildren.indexOf(rootRecentsTask)) : secondaryStack.mChildren.indexOf(rootHomeTask);
                            if (multiIndex < homeOrRecetnsIndex) {
                                secondaryStack.positionChildAt(homeOrRecetnsIndex, multiActivityTask, false);
                                if (VivoMultiWindowConfig.DEBUG) {
                                    String str2 = this.TAG;
                                    VSlog.d(str2, "adjustTaskOrderInSplitIfNeeded for reorder ,multiIndex is " + multiIndex + ",homeOrRecetnsIndex is " + homeOrRecetnsIndex + ",because of " + reason);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void adjustWindowModeOfSplitIfNeeded(Task launchStack) {
        if (this.mService.isVivoVosMultiWindowSupport() && !this.mService.isVivoFreeFormValid() && !this.mService.isInMultiWindowDefaultDisplay() && launchStack != null && launchStack.getDisplayId() == 0 && launchStack.inSplitScreenPrimaryWindowingMode()) {
            this.mTask.getRequestedOverrideConfiguration().windowConfiguration.setWindowingMode(0);
            if (VivoMultiWindowConfig.DEBUG) {
                String str = this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("adjustWindowModeOfSplitIfNeeded with  ");
                sb.append(this.mTask);
                sb.append(",parent null if ");
                sb.append(this.mTask.getParent() == null);
                sb.append(Debug.getCallers(5));
                VSlog.d(str, sb.toString());
            }
        }
    }

    public ActivityStack adjustFocusToHomeAndMoveTopIfNeeded(ActivityStack oldstack, ActivityRecord top, String reason) {
        if (oldstack != null && oldstack.isOnHomeDisplay() && "finish-top-primary".equals(reason) && top != null && top.isMultiWindowAppListActivity() && this.mService.getFocusedDisplayId() == 0 && VivoMultiWindowConfig.IS_VIVO_HIDE_SPLIT_APPLIST) {
            if (VivoMultiWindowConfig.DEBUG) {
                String str = this.TAG;
                VSlog.i(str, "vivo_multiwindow_splitfull hideapplist adjust from oldstack " + oldstack + " and top " + top + " to home");
            }
            oldstack.getDisplayArea().moveHomeActivityToTop(reason);
            return oldstack.getDisplayArea().getRootHomeTask();
        }
        return null;
    }

    public boolean pauseHomeImmediatelyforSplitIfNeeded(boolean isHome) {
        if (isHome && this.mService.isInSplittingState()) {
            VSlog.i(this.TAG, "vivo_multiwindow_fwk pauseHomeImmediatelyforSplit ");
            return true;
        }
        return false;
    }

    public void forceQuitSplitScreenIfNeeded(String reason, boolean isFocusedStackOnDisplay, ActivityRecord top) {
        Task task;
        ActivityStack primarySplitScreenStack;
        if (this.mService.isMultiWindowSupport() && this.mService.isInMultiWindowDefaultDisplay() && top != null && (task = this.mTask) != null && task.getDisplayId() == 0 && this.mTask.isRootTask()) {
            TaskDisplayArea area = this.mTask.mRootWindowContainer.getDefaultTaskDisplayArea();
            if (VivoMultiWindowConfig.DEBUG) {
                String str = this.TAG;
                VSlog.d(str, "forceQuitSplitScreenIfNeeded of last top " + top + ",in stack:" + this.mTask + ",isVisible is " + top.isVisible() + ",matchParentBounds is " + top.matchParentBounds() + ",fillsParent is " + top.fillsParent() + ",isFocusedStackOnDisplay is " + isFocusedStackOnDisplay);
            }
            if (isFocusedStackOnDisplay && reason != null && "finish top".equals(reason) && area != null && area.isTopStack(this.mTask) && top != null && top.isVisible() && top.matchParentBounds() && top.fillsParent() && !this.mTask.inSplitScreenWindowingMode() && !this.mTask.inMultiWindowMode() && (primarySplitScreenStack = area.getRootSplitScreenPrimaryTask()) != null && area.getIndexOf(this.mTask) > area.getIndexOf(primarySplitScreenStack)) {
                String str2 = this.TAG;
                VSlog.i(str2, "forceQuitSplitScreenIfNeeded of last top " + top + ",in stack:" + this.mTask + " " + Debug.getCallers(5));
                area.onSplitScreenModeDismissed(this.mTask);
            }
        }
    }

    public void setSanitizeAndApplyHierarchyOpState(boolean state) {
        this.inSanitizeAndApplyHierarchyOpState = state;
    }

    public void setUseBLASTSyncTransactionState() {
        if (this.inSanitizeAndApplyHierarchyOpState) {
            this.mUseBLASTSyncTransaction = this.mTask.useBLASTSync() ? 1 : 0;
            this.mTask.mWmService.mH.removeCallbacks(this.TransientMultiWindowExited);
            this.mTask.mWmService.mH.postDelayed(this.TransientMultiWindowExited, 600L);
        }
    }

    public boolean getUseBLASTSyncTransactionState() {
        return this.mUseBLASTSyncTransaction == 1;
    }

    public boolean useSameTransactionIfNeeded(ActivityRecord r) {
        DisplayContent displayContent;
        if (VivoMultiWindowConfig.IS_VIVO_USE_SAME_TRANSATION) {
            if ((!(this.mTask.mUsingBLASTSyncTransaction && this.mUseBLASTSyncTransaction == 0) && (this.mTask.mUsingBLASTSyncTransaction || this.mUseBLASTSyncTransaction != 1)) || (displayContent = this.mTask.getDisplayContent()) == null || !displayContent.isVivoMultiWindowExitedJustWithDisplay()) {
                return false;
            }
            String str = this.TAG;
            VSlog.d(str, "vivo_multiwindow_fwk useBLASTSyncTransactionIfNeeded task:" + this.mTask + " mUseBLASTSyncTransaction:" + this.mUseBLASTSyncTransaction + " getLastSurfaceSize:" + this.mTask.getLastSurfaceSize() + " mBLASTSyncTransaction:" + this.mTask.mBLASTSyncTransaction + " getPendingTransaction:" + this.mTask.getPendingTransaction());
            if (this.mUseBLASTSyncTransaction == 1) {
                this.mTask.mWmService.mH.removeCallbacks(this.BLASTSyncTransactionApply);
                this.mTask.mWmService.mH.postDelayed(this.BLASTSyncTransactionApply, 200L);
            }
            return true;
        }
        return false;
    }

    public void onSetActivityResumed(String packageName, String recordInfo, int pid, ApplicationInfo appInfo) {
        SystemDefenceManager.getInstance().onSetActivityResumed(packageName, recordInfo, pid, appInfo);
    }

    public boolean isPresentWithPackage(final String packageName) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && this.mTask.getParent() != null) {
            ActivityRecord find = this.mTask.getActivity(new Predicate() { // from class: com.android.server.wm.-$$Lambda$VivoTaskImpl$xGTjIJ2kN52HeUCv3xm0YCclbuQ
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return VivoTaskImpl.lambda$isPresentWithPackage$0(packageName, (ActivityRecord) obj);
                }
            });
            VSlog.d("VivoCar", "isPresentWithPackage: " + packageName + " ,find: " + find);
            return find != null;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$isPresentWithPackage$0(String packageName, ActivityRecord r) {
        return r.packageName != null && r.packageName.equals(packageName);
    }

    public void tryNotifyExitingWindow() {
        WindowContainer animatingChild = this.mTask.getAnimatingContainer(4, -1);
        if (animatingChild == null) {
            this.mExitingWindow = null;
            this.mTask.forAllWindows(this.mFindExitingWindow, true);
            if (this.mExitingWindow != null) {
                String str = this.TAG;
                VSlog.d(str, "~win.onExitAnimationDone when no anim " + this.mExitingWindow);
                this.mExitingWindow.onExitAnimationDone();
            }
        }
    }

    public /* synthetic */ boolean lambda$new$1$VivoTaskImpl(WindowState w) {
        if (w.mAnimatingExit) {
            this.mExitingWindow = w;
            return true;
        }
        return false;
    }

    public void notifyAppShareIfNeededLocked(ActivityStack stack, int displayId) {
        this.mVivoAppShareManager.notifyAppShareIfNeededLocked(stack, displayId);
    }

    public boolean isAppShareDisplayExist() {
        return this.mVivoAppShareManager.isAppShareDisplayExist();
    }

    public boolean isOnAppShareDisplay(String packageName, int userId) {
        return this.mVivoAppShareManager.isOnAppShareDisplay(packageName, userId);
    }

    public boolean shouldBlockPemNoteActivity(ActivityRecord r) {
        return r != null && MultiDisplayManager.isAppShareDisplayId(r.getDisplayId());
    }

    public boolean shouldSkipRecentTasksForAppShare(ActivityRecord record) {
        boolean skip = isAppShareDisplayExist() && isOnAppShareDisplay(record.packageName, record.mUserId) && !MultiDisplayManager.isAppShareDisplayId(record.getDisplayId());
        if (skip) {
            String str = this.TAG;
            VSlog.i(str, "skip Trim recent task: " + record.getTask());
        }
        return skip;
    }
}