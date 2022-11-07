package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.multidisplay.MultiDisplayManager;
import android.os.Debug;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.view.IApplicationToken;
import android.widget.Toast;
import com.android.server.UiThread;
import com.android.server.VCarConfigManager;
import com.vivo.services.rms.display.SceneManager;
import java.util.ArrayList;
import java.util.List;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoActivityStackSupervisorImpl implements IVivoActivityStackSupervisor {
    private static final String ACTION_CAR_ACTIVITY_TO_START = "CAR_ACTIVITY_TO_START";
    private static final String CAR_NETWORKING_PACKAGE = "com.vivo.car.networking";
    private static final String PACKAGE_NAME = "package";
    static final String TAG = "VivoActivityStackSupervisorImpl";
    private ActivityTaskManagerService mService;
    private ActivityStackSupervisor mSuperVisor;
    private VivoAppShareManager mVivoAppShareManager;
    public static boolean mPerfSendTapHint = false;
    public static boolean mIsPerfBoostAcquired = false;
    public static int mPerfHandle = -1;
    VivoMultiWindowTransManager mMultiWindowWmsInstance = VivoMultiWindowTransManager.getInstance();
    private int oldRotation = -1;
    private Rect mLastFocusedStackRectInFreeform = new Rect();
    private boolean mFreeFormForceFullScreen = false;
    private int forceFullscreenTaskId = -1;
    private boolean mFreeformMoveToFullscreenAndToTop = false;
    private boolean mIsRemovingFreeformStackDirect = false;
    private boolean mHomeVisible = false;
    private boolean restoreOverideConfigWhenEnterToMax = false;
    private Rect backLastBoundsWhenEnterToMax = new Rect();
    private boolean willCallContactsFromFreefromMms = false;
    private boolean willCallContactsFromPasswd = false;
    public AbsVivoPerfManager mPerfBoost = null;
    Point mBaseDisplaySize = new Point();

    public VivoActivityStackSupervisorImpl(ActivityStackSupervisor supervisor, ActivityTaskManagerService service) {
        if (supervisor == null) {
            VSlog.i(TAG, "container is " + supervisor);
        }
        this.mSuperVisor = supervisor;
        this.mService = service;
        this.mVivoAppShareManager = VivoAppShareManager.getInstance();
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public boolean needForceNonResizable(Task task) {
        ActivityRecord topActivity;
        if (!this.mService.isMultiWindowSupport() || !VivoMultiWindowConfig.IS_VIVO_AM_RESIZABLE_PROPERTY || task == null || (topActivity = task.getTopMostActivity()) == null || topActivity.isResizeable() || !task.isResizeable()) {
            return false;
        }
        VSlog.v(TAG, "handleNonResizableTaskIfNeeded task=" + task + " force to fullscreen for topActivity=" + topActivity);
        return true;
    }

    public void specialFreezingMultiWindow(final String packageName, int type) {
        if (!this.mService.isMultiWindowSupport() || !VivoMultiWindowConfig.IS_VIVO_SPLIT_SPLITFULL_ANIM) {
            return;
        }
        if (this.mMultiWindowWmsInstance == null) {
            VSlog.w(TAG, "vivo_multiwindow_fmk mMultiWindowWmsInstance null return");
            return;
        }
        KeyguardManager mKeyguardManager = (KeyguardManager) this.mSuperVisor.mService.mContext.getSystemService("keyguard");
        boolean keyguardlocked = mKeyguardManager.isKeyguardLocked();
        if (!keyguardlocked) {
            if ("specifyTime".equals(packageName)) {
                int time = type;
                if (time < 100) {
                    time = 100;
                }
                if (time > 1000) {
                    time = 1000;
                }
                this.mMultiWindowWmsInstance.prepareSpecifyTimeTransFreezeWindowFrame(time);
            } else if (type == 1) {
                this.mMultiWindowWmsInstance.prepareShortTransFreezeWindowFrame();
            } else if (type == 2) {
                this.mMultiWindowWmsInstance.prepareNormalTransFreezeWindowFrame();
            } else if (type == 3) {
                this.mMultiWindowWmsInstance.prepareLongTransFreezeWindowFrame();
            } else if (type == 4) {
                this.mMultiWindowWmsInstance.prepareSuperShortTransFreezeWindowFrame();
            }
            this.mMultiWindowWmsInstance.waitExitMultiWindowFreezeAnimation(new IApplicationToken.Stub() { // from class: com.android.server.wm.VivoActivityStackSupervisorImpl.1
                public String getName() {
                    return packageName;
                }

                public String toString() {
                    StringBuilder sb = new StringBuilder(128);
                    sb.append("Token{");
                    sb.append(Integer.toHexString(System.identityHashCode(this)));
                    sb.append(' ');
                    sb.append(getName());
                    sb.append('}');
                    return sb.toString();
                }
            });
        }
    }

    public void handleNonResizableAnimNoTarget(int type) {
        if (!VivoMultiWindowConfig.IS_VIVO_SPLIT_SPLITFULL_ANIM) {
            return;
        }
        if (this.mMultiWindowWmsInstance == null) {
            VSlog.w(TAG, "vivo_multiwindow_fmk mMultiWindowWmsInstance null return");
            return;
        }
        KeyguardManager mKeyguardManager = (KeyguardManager) this.mSuperVisor.mService.mContext.getSystemService("keyguard");
        boolean keyguardlocked = mKeyguardManager.isKeyguardLocked();
        if (!keyguardlocked) {
            if (type == 1) {
                this.mMultiWindowWmsInstance.prepareShortTransFreezeWindowFrame();
            } else if (type == 2) {
                this.mMultiWindowWmsInstance.prepareNormalTransFreezeWindowFrame();
            } else if (type == 4) {
                this.mMultiWindowWmsInstance.prepareSuperShortTransFreezeWindowFrame();
            } else if (type == 3) {
                this.mMultiWindowWmsInstance.prepareLongTransFreezeWindowFrame();
            }
            if (VivoMultiWindowConfig.DEBUG) {
                if (type == 1 || type == 2) {
                    VSlog.i(TAG, "vivo_multiwindow_fmk handleNonResizableAnimNoTarget prepare freeze time is " + SystemClock.elapsedRealtime() + Debug.getCallers(6));
                }
            }
        }
    }

    public void handleNonResizableAnim(ActivityRecord topActivity) {
        if (VivoMultiWindowConfig.IS_VIVO_SPLIT_SPLITFULL_ANIM) {
            if (topActivity == null || topActivity.appToken == null) {
                VSlog.w(TAG, "vivo_multiwindow_fmk handleNonResizableAnim null return");
            } else if (this.mMultiWindowWmsInstance == null) {
                VSlog.w(TAG, "vivo_multiwindow_fmk mMultiWindowWmsInstance null return");
            } else {
                KeyguardManager mKeyguardManager = (KeyguardManager) this.mSuperVisor.mService.mContext.getSystemService("keyguard");
                boolean keyguardlocked = mKeyguardManager.isKeyguardLocked();
                if (!keyguardlocked) {
                    if (topActivity.toString().contains("com.bbk.cloud/.ui.DelegateActivity")) {
                        this.mMultiWindowWmsInstance.prepareShortTransFreezeWindowFrame();
                    } else {
                        this.mMultiWindowWmsInstance.prepareNormalTransFreezeWindowFrame();
                    }
                    if (VivoMultiWindowConfig.DEBUG) {
                        VSlog.i(TAG, "vivo_multiwindow_fmk handleNonResizableAnim prepare exit split anim time is " + SystemClock.elapsedRealtime() + " for " + topActivity.appToken + " " + Debug.getCallers(6));
                    }
                    this.mMultiWindowWmsInstance.waitExitMultiWindowFreezeAnimation(topActivity.appToken);
                }
            }
        }
    }

    public void handleNonResizableAnim(ActivityRecord topActivity, int type) {
        if (VivoMultiWindowConfig.IS_VIVO_SPLIT_SPLITFULL_ANIM) {
            if (topActivity == null || topActivity.appToken == null) {
                VSlog.w(TAG, "vivo_multiwindow_fmk handleNonResizableAnim null return");
            } else if (this.mMultiWindowWmsInstance == null) {
                VSlog.w(TAG, "vivo_multiwindow_fmk mMultiWindowWmsInstance null return");
            } else {
                KeyguardManager mKeyguardManager = (KeyguardManager) this.mSuperVisor.mService.mContext.getSystemService("keyguard");
                boolean keyguardlocked = mKeyguardManager.isKeyguardLocked();
                if (!keyguardlocked) {
                    if (type == 4) {
                        if (topActivity.toString().contains("com.android.bbkcalculator/.Calculator")) {
                            this.mMultiWindowWmsInstance.prepareShortTransFreezeWindowFrame();
                        } else {
                            this.mMultiWindowWmsInstance.prepareSuperShortTransFreezeWindowFrame();
                        }
                    } else if (type == 1) {
                        this.mMultiWindowWmsInstance.prepareShortTransFreezeWindowFrame();
                    } else if (type == 2) {
                        this.mMultiWindowWmsInstance.prepareNormalTransFreezeWindowFrame();
                    }
                    if (VivoMultiWindowConfig.DEBUG) {
                        VSlog.i(TAG, "vivo_multiwindow_fmk handleNonResizableAnim type:" + type + " prepare exit split anim time is " + SystemClock.elapsedRealtime() + " for " + topActivity.appToken + " " + Debug.getCallers(6));
                    }
                    this.mMultiWindowWmsInstance.waitExitMultiWindowFreezeAnimation(topActivity.appToken);
                }
            }
        }
    }

    public void updateBaseDisplaySize() {
        if (this.mService.isVivoMultiWindowSupport()) {
            this.mService.mWindowManager.getBaseDisplaySize(0, this.mBaseDisplaySize);
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.w(TAG, "updateBaseDisplaySize: screenWidth=" + this.mBaseDisplaySize.x + " screenHeight=" + this.mBaseDisplaySize.y);
            }
        }
    }

    public void resizeDockedStackLockedFromAms(Rect dockedBounds, Rect tempDockedTaskBounds, Rect tempDockedTaskInsetBounds, Rect tempOtherTaskBounds, Rect tempOtherTaskInsetBounds, boolean preserveWindows, boolean deferResume) {
    }

    public void debugNonResizableTask(ActivityStack dockedStack) {
        ActivityRecord topRunningActivity = null;
        if (dockedStack != null) {
            topRunningActivity = dockedStack.topRunningActivityLocked();
        }
        if (VivoMultiWindowConfig.DEBUG) {
            VSlog.d(TAG, "handleNonResizableTaskIfNeeded: topRunningActivity=" + topRunningActivity + " dockedStack=" + dockedStack);
        }
    }

    public void doAnimBeforeMoveToFull(Task task) {
        if (!this.mService.isMultiWindowSupport()) {
            return;
        }
        ActivityRecord topActivity = task != null ? task.getTopMostActivity() : null;
        if (topActivity != null && !topActivity.isActivityTypeHome() && !topActivity.isActivityTypeRecents() && !topActivity.isActivityTypeAssistant() && this.mService.getFocusedDisplayId() == 0) {
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.i(TAG, "vivo_multiwindow_splitfull doAnimBeforeMoveToFull topActivity is " + topActivity);
            }
            if (this.mService.mWindowManager.isMinimizedDock() && this.mSuperVisor.mWindowManager.getDefaultDisplayRotation() == 0) {
                handleNonResizableAnimNoTarget(4);
            } else {
                handleNonResizableAnim(topActivity);
            }
        }
    }

    public boolean forceNonResizableIfNeeded(boolean inSplitScreenMode, boolean isSecondaryDisplayPreferred, Task task, ActivityStack actualStack) {
        return false;
    }

    public void debugUpdateMultiWindowMode(String shortComponentName, boolean attached) {
        if (VivoMultiWindowConfig.DEBUG) {
            VSlog.v(TAG, "Add UpdateMultiWindowMode shortComponentName=" + shortComponentName + " attached = " + attached);
        }
    }

    public boolean isStackNeedResizeConsiderVisible(ActivityStack targetStack) {
        return true;
    }

    public boolean ignoreUpdateMultiModeInSplitIfNeeded(ActivityRecord activity) {
        return false;
    }

    public boolean skipDimissSplitScreenModeIfNeeded(ActivityRecord topActivity) {
        if (topActivity != null && "com.vivo.wallet/com.vivo.pay.swing.activity.NewSwipeActivity".equals(topActivity.shortComponentName)) {
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.v(TAG, "skipDimissSplitScreenModeIfNeeded shortComponentName:" + topActivity.shortComponentName);
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean canUseAcivityOptionBoundsForVivoFreeform(ActivityOptions options) {
        return (this.mService.mSupportsPictureInPicture && options.getLaunchWindowingMode() == 2) || this.mService.mSupportsFreeformWindowManagement || this.mService.isVivoFreeFormValid();
    }

    public void resetVivoFreeformMaxmizeState() {
        if (this.mFreeFormForceFullScreen || this.mService.mWindowManager.isVivoFreeFormStackMax()) {
            this.mFreeFormForceFullScreen = false;
            this.mService.notifyFreeFormStackMaxChanged(false);
        }
    }

    public boolean isWillCallContactsFromFreefromMms() {
        return this.willCallContactsFromFreefromMms;
    }

    public void setWillCallContactsFromFreefromMms(boolean willCallContactsFromFreefromMms) {
        this.willCallContactsFromFreefromMms = willCallContactsFromFreefromMms;
    }

    public boolean isWillCallContactsFromPasswd() {
        return this.willCallContactsFromPasswd;
    }

    public void setWillCallContactsFromPasswd(boolean willCallContactsFromPasswd) {
        this.willCallContactsFromPasswd = willCallContactsFromPasswd;
    }

    public void pauseAnotherStackForVivoFreeform(ActivityStack fromStack, TaskDisplayArea taskDisplayArea, boolean onTop) {
        this.mFreeformMoveToFullscreenAndToTop = false;
        if (this.mService.isVivoFreeFormValid() && fromStack.inFreeformWindowingMode()) {
            ActivityStack fullscreenStack = taskDisplayArea.getTopStackInWindowingMode(1);
            if (!onTop) {
                fromStack.startPausingLocked(false, false, (ActivityRecord) null);
                return;
            }
            this.mFreeformMoveToFullscreenAndToTop = true;
            if (fullscreenStack != null && fullscreenStack.hasChild()) {
                fullscreenStack.startPausingLocked(false, false, (ActivityRecord) null);
            }
        }
    }

    public boolean notPreserveWindowWhenFreeformExitToTop() {
        if (this.mFreeformMoveToFullscreenAndToTop) {
            this.mFreeformMoveToFullscreenAndToTop = false;
            this.mSuperVisor.mRootWindowContainer.ensureActivitiesVisible((ActivityRecord) null, 0, false);
            return true;
        }
        return false;
    }

    public boolean setContinueResumeForVivoFreeform(ActivityStack stack, boolean andResume) {
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mService.isInDirectFreeformState() && stack.getWindowingMode() == 5 && this.mService.isFirstTimeUnlock() && this.mSuperVisor.readyToResume()) {
            this.mService.setIsFirstTimeUnlock(false);
            return true;
        }
        return andResume;
    }

    public void setCheckFocusForFreeformIfNeeded(ActivityStack currentStack) {
        if (currentStack != null && currentStack.getDisplayArea() != null && this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isFreeFormStackMax() && currentStack.inFreeformWindowingMode() && currentStack.isFocusable()) {
            currentStack.getDisplayArea().setCheckFocus(true);
        }
    }

    public void setIsRemovingFreeformDirectIfNeed(ActivityStack stack) {
        if (this.mService.isVivoFreeFormValid() && stack.inFreeformWindowingMode()) {
            this.mSuperVisor.mWindowManager.setClosingFreeForm(true);
            this.mIsRemovingFreeformStackDirect = true;
        }
    }

    public void ensureFreeformStackRemoved(ActivityStack stack) {
        if (!stack.isLeafTask() && stack.inFreeformWindowingMode() && this.mIsRemovingFreeformStackDirect && this.mService.isInVivoFreeform()) {
            this.mIsRemovingFreeformStackDirect = false;
            if (stack.getChildCount() == 0) {
                stack.removeIfPossible();
                return;
            }
            for (int i = stack.getChildCount() - 1; i >= 0; i--) {
                Task temp = stack.getChildAt(i);
                if (temp.getChildCount() == 0) {
                    stack.removeChild(temp, "remove freeform stack");
                } else {
                    this.mSuperVisor.mRootWindowContainer.getDefaultTaskDisplayArea().moveVivoFreeformTasksToFullscreenStackLocked(false);
                }
            }
            if (stack.getChildCount() == 0) {
                stack.removeIfPossible();
            }
        }
    }

    public ActivityStack restoreFreeformTaskToFullscreenStack(Task task, boolean onTop) {
        if (!this.mService.isVivoFreeFormValid() || !this.mService.isInDirectFreeformState() || !this.mService.isInVivoFreeform() || !task.inFreeformWindowingMode() || task.getParent() != null) {
            return null;
        }
        ActivityStack stack = this.mSuperVisor.mRootWindowContainer.getDefaultTaskDisplayArea().getOrCreateStack(1, 1, onTop);
        return stack;
    }

    public boolean moveFreeformToSecondDisplayFromRecentsIfNeeded(ActivityOptions activityOptions, int taskId) {
        Task taskTemp;
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mService.isMultiDisplyPhone() && activityOptions != null && (taskTemp = this.mSuperVisor.mRootWindowContainer.anyTaskForId(taskId)) != null && taskTemp.inFreeformWindowingMode() && activityOptions.getLaunchDisplayId() == 4096) {
            this.mService.moveFreeformTaskToSecondDisplay(taskTemp.topRunningActivityLocked().appToken);
            return true;
        }
        return false;
    }

    public ActivityRecord getFullsceenStackTopRunningInVivoFreeform() {
        if (!this.mService.isVivoFreeFormValid()) {
            return null;
        }
        ActivityStack fullscreenStack = this.mService.mRootWindowContainer.getTopFocusedDisplayStack(1, 1);
        ActivityRecord fullscreenStackTopRuningActivity = fullscreenStack == null ? null : fullscreenStack.topRunningActivityLocked();
        return fullscreenStackTopRuningActivity;
    }

    public Task getTaskToLaunchForVivofreeform(int taskId, ActivityOptions activityOptions) {
        ActivityStack freeformStack;
        Task taskToLaunch = null;
        if (this.mService.isVivoFreeFormValid() && this.mService.isInDirectFreeformState() && (freeformStack = this.mService.mRootWindowContainer.getVivoFreeformStack()) != null) {
            ActivityRecord freeformStackTopRuningActivity = freeformStack.topRunningActivityLocked();
            taskToLaunch = this.mService.mRootWindowContainer.anyTaskForId(taskId, 2, activityOptions, true);
            TaskDisplayArea taskDisplayArea = freeformStack.getDisplayArea();
            if (taskToLaunch != null && freeformStackTopRuningActivity != null && !freeformStackTopRuningActivity.nowVisible && taskToLaunch.getTopMostActivity() == freeformStackTopRuningActivity && taskDisplayArea != null) {
                int freeformIndex = taskDisplayArea.getIndexOf(freeformStack);
                ActivityStack stackUnderFreeform = taskDisplayArea.getChildAt(freeformIndex - 1);
                if (this.mService.isStartingRecentOnHome() && stackUnderFreeform != null && !stackUnderFreeform.isActivityTypeHome()) {
                    stackUnderFreeform = taskDisplayArea.getRootHomeTask();
                    this.mService.setIsStartingRecentOnHome(false);
                }
                if (stackUnderFreeform != null && !stackUnderFreeform.isLeafTask() && stackUnderFreeform.getTopChild() != null && (activityOptions == null || activityOptions.getLaunchWindowingMode() == 0)) {
                    taskToLaunch = (Task) stackUnderFreeform.getTopChild();
                }
            }
        }
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform()) {
            int launchDisplayId = activityOptions != null ? activityOptions.getLaunchDisplayId() : -1;
            if (activityOptions != null && launchDisplayId == 0 && launchDisplayId == this.mService.getFocusedDisplayId()) {
                activityOptions.setLaunchDisplayId(-1);
            }
        }
        return taskToLaunch;
    }

    public void checkAndSendResizeFreeformTaskForIme() {
        ActivityStack freeformStack;
        Task freeformTask;
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && (freeformStack = this.mService.mRootWindowContainer.getVivoFreeformStack()) != null) {
            ActivityRecord freeformStackTopRuningActivity = freeformStack.topRunningActivityLocked();
            if (this.mService.mWindowManager.getFreeformPosition() != null && !this.mService.mWindowManager.getFreeformPosition().isEmpty() && this.mService.mWindowManager.isFreeformStackMove() && freeformStackTopRuningActivity != null && (freeformTask = freeformStackTopRuningActivity.getTask()) != null) {
                this.mService.mWindowManager.sendResizeFreeformTaskForIme(this.mService.mWindowManager.getFreeformPosition(), freeformTask.mTaskId);
            }
        }
    }

    public void checkAndExitFreeform(Task task, ActivityRecord fullscreenStackTopRuningActivity) {
        ActivityStack freeformStack;
        if (this.mService.isVivoFreeFormValid() && task.getStack() != null && task.getStack().getDisplayId() == 0 && (freeformStack = this.mService.mRootWindowContainer.getVivoFreeformStack()) != null && fullscreenStackTopRuningActivity != null && freeformStack.getDisplayArea() != null) {
            ActivityRecord freeformStackTopRuningActivity = freeformStack.topRunningActivityLocked();
            if (task.getTopMostActivity() != freeformStackTopRuningActivity && task.getTopMostActivity() != fullscreenStackTopRuningActivity && !this.mService.isInDirectFreeformState()) {
                freeformStack.getDisplayArea().moveVivoFreeformTasksToFullscreenStackLocked(freeformStack.mTaskId == task.getRootTaskId());
            }
        }
    }

    public boolean ignoreMoveHomeToFrontWhenStartFreeformTask(Task task) {
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isInDirectFreeformState() && task.inFreeformWindowingMode()) {
            return true;
        }
        return false;
    }

    public void moveFreeformToTopAndSetFocusIfNeed(Task task, ActivityRecord targetActivity) {
        ActivityStack freeform;
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform()) {
            if (task.getWindowingMode() == 1 && task.getActivityType() == 1 && !this.mService.isFreeFormMin() && (freeform = this.mService.mRootWindowContainer.getVivoFreeformStack()) != null) {
                freeform.moveFreeformWindowStateToTop();
            }
            if (targetActivity != null && targetActivity.getDisplay() != null) {
                targetActivity.getDisplay().setFocusedApp(targetActivity, true);
            }
        }
    }

    public void handleNonResizableActivityIfNeededForFreeForm(ActivityRecord topActivity) {
        ActivityStack freeFormStack = this.mService.mRootWindowContainer.getVivoFreeformStack();
        if (freeFormStack == null || topActivity == null) {
            this.mFreeFormForceFullScreen = false;
        } else if (topActivity.getTask().isActivityTypeHome()) {
        } else {
            if (topActivity.mActivityComponent.toString().contains("com.vivo.contacts") && this.willCallContactsFromFreefromMms) {
                this.willCallContactsFromFreefromMms = false;
            }
            if (topActivity.getTask().getStack() == freeFormStack) {
                if (topActivity.isForceFullScreenForFreeForm()) {
                    if (!this.mFreeFormForceFullScreen) {
                        this.mService.setNormalFreezingAnimaiton(topActivity.appToken);
                        this.mFreeFormForceFullScreen = true;
                        this.mLastFocusedStackRectInFreeform.set(topActivity.getTask().getBounds());
                        if (!this.backLastBoundsWhenEnterToMax.isEmpty()) {
                            this.mLastFocusedStackRectInFreeform.set(this.backLastBoundsWhenEnterToMax);
                            this.backLastBoundsWhenEnterToMax.setEmpty();
                        }
                        Rect mWindowDragBounds = new Rect();
                        if (topActivity.getDisplay() != null && topActivity.getDisplayArea() != null) {
                            topActivity.getDisplay().mDisplayContent.getStableRect(mWindowDragBounds);
                            this.mHomeVisible = topActivity.getDisplayArea().isHomeStackVisible();
                        }
                        if (this.restoreOverideConfigWhenEnterToMax) {
                            this.restoreOverideConfigWhenEnterToMax = false;
                            mWindowDragBounds.top = 0;
                            topActivity.updateRequestOverrideConfigUseBounds(new Rect(0, 0, 0, 0));
                        }
                        this.oldRotation = this.mService.getDisplayRotation(topActivity.getDisplay());
                        this.mService.notifyFreeFormStackMaxChanged(true);
                        this.forceFullscreenTaskId = topActivity.getTask().mTaskId;
                        this.mService.resizeTask(topActivity.getTask().mTaskId, mWindowDragBounds, 1);
                    }
                } else if (!topActivity.isForceFullScreenForFreeForm() && topActivity.getTask().mTaskId == this.forceFullscreenTaskId && this.mFreeFormForceFullScreen) {
                    this.mFreeFormForceFullScreen = false;
                    this.forceFullscreenTaskId = -1;
                    this.mService.setNormalFreezingAnimaiton(topActivity.appToken);
                    int newRotation = this.mService.getDisplayRotation(topActivity.getDisplay());
                    rotateBounds(this.oldRotation, newRotation, this.mLastFocusedStackRectInFreeform, topActivity.getDisplayId());
                    this.mService.notifyFreeFormStackMaxChanged(false);
                    if (topActivity.getDisplayArea() != null && !this.mHomeVisible) {
                        topActivity.getDisplayArea().setCheckFocus(true);
                    }
                    this.mHomeVisible = false;
                    this.mService.resizeTask(topActivity.getTask().mTaskId, this.mLastFocusedStackRectInFreeform, 1);
                }
            }
        }
    }

    public void setRestoreOverideConfigWhenEnterToMax(boolean restoreOverideConfigWhenMax) {
        this.restoreOverideConfigWhenEnterToMax = restoreOverideConfigWhenMax;
    }

    public void setbackLastBoundsWhenEnterToMax(Rect backRect) {
        this.backLastBoundsWhenEnterToMax = backRect;
    }

    private void rotateBounds(int oldRotation, int newRotation, Rect bounds, int displayId) {
        if (newRotation == oldRotation) {
            return;
        }
        Point size = new Point();
        this.mService.mWindowManager.getBaseDisplaySize(displayId, size);
        int baseDisplayHeight = size.y;
        int baseDisplayWidth = size.x;
        if (oldRotation == 3 && newRotation == 0) {
            bounds.set(bounds.top, baseDisplayHeight - bounds.right, bounds.bottom, baseDisplayHeight - bounds.left);
        } else if (oldRotation == 1 && newRotation == 0) {
            bounds.set(baseDisplayWidth - bounds.bottom, bounds.left, baseDisplayWidth - bounds.top, bounds.right);
        } else {
            bounds.set(bounds.top, bounds.left, bounds.bottom, bounds.right);
        }
    }

    public ActivityRecord getFreeformTopActivity() {
        ActivityStack freeformStack = this.mSuperVisor.mRootWindowContainer.getVivoFreeformStack();
        if (freeformStack != null) {
            return freeformStack.topRunningActivityLocked();
        }
        return null;
    }

    public void acquireRecentAppLaunchPerfLock(ActivityRecord r) {
        WindowProcessController wpc;
        if (this.mPerfBoost == null) {
            this.mPerfBoost = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        }
        AbsVivoPerfManager absVivoPerfManager = this.mPerfBoost;
        if (absVivoPerfManager != null) {
            absVivoPerfManager.perfHintAsync(4225, r.packageName, -1, 1);
            mPerfSendTapHint = true;
            this.mPerfBoost.perfHintAsync(4225, r.packageName, -1, 2);
            if (this.mService != null && r != null && r.info != null && r.info.applicationInfo != null && (wpc = this.mService.getProcessController(r.processName, r.info.applicationInfo.uid)) != null && wpc.hasThread()) {
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

    public void startSpecificActivityBoost(String packageName, int pid) {
        if (this.mPerfBoost != null) {
            VSlog.i(TAG, "The Process " + packageName + " Already Exists in BG. So sending its PID: " + pid);
            this.mPerfBoost.perfHintAsync(4225, packageName, pid, 102);
        }
    }

    public void reportActivityLaunchedLockedBoost(long totalTime, ActivityRecord r) {
        if (totalTime > 0 && this.mPerfBoost != null && r.app != null) {
            this.mPerfBoost.perfHintAsync(4162, r.packageName, r.app.getPid(), 1);
        }
    }

    public boolean shouldBlockedByAppShare(Task task) {
        return this.mVivoAppShareManager.shouldBlockedByAppShareLocked(task);
    }

    public boolean blockStartForCar(final ActivityRecord r, int preferredDisplayId) {
        final Context carContext;
        ArrayList<String> blackList;
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            ArrayList<String> pList = null;
            ArrayList<String> whiteList = null;
            String topCarPackage = null;
            final String packageName = (r.intent == null || r.intent.getComponent() == null) ? null : r.intent.getComponent().getPackageName();
            if (packageName == null) {
                return false;
            }
            WindowProcessController proc = r.app;
            if (proc == null) {
                proc = (WindowProcessController) this.mService.mProcessNames.get(r.processName, r.info.applicationInfo.uid);
            }
            if (proc != null && proc.hasThread() && proc.isRunningInCar && MultiDisplayManager.isVCarDisplayRunning() && (blackList = VCarConfigManager.getInstance().get("black_activity")) != null && blackList.size() > 0 && blackList.contains(r.intent.getComponent().flattenToString())) {
                VSlog.d("VivoCar", "forbid  start " + r.intent);
                return true;
            } else if (preferredDisplayId != 0) {
                return false;
            } else {
                if (MultiDisplayManager.isVCarDisplayRunning()) {
                    pList = VCarConfigManager.getInstance().get("casting_toast_package");
                    whiteList = VCarConfigManager.getInstance().get("casting_toast_white_activity");
                }
                if (pList != null && pList.size() > 0 && pList.contains(packageName)) {
                    final String activityName = r.intent.getComponent().flattenToString();
                    if (whiteList == null || whiteList.size() <= 0 || !whiteList.contains(activityName)) {
                        UiThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.VivoActivityStackSupervisorImpl.2
                            @Override // java.lang.Runnable
                            public void run() {
                                VSlog.d("VivoCar", " forbid " + activityName + " to start when casting");
                                ToastUtils.show(VivoActivityStackSupervisorImpl.this.mSuperVisor.mService.mContext, 51249177, VivoActivityStackSupervisorImpl.this.getAppName(packageName));
                            }
                        });
                        return true;
                    }
                }
                if (proc != null && proc.hasThread() && proc.isRunningInCar) {
                    ArrayList<String> pList2 = VCarConfigManager.getInstance().get("confirm_by_user_package");
                    if (pList2 != null && pList2.size() > 0 && pList2.contains(packageName) && MultiDisplayManager.isVCarDisplayRunning()) {
                        this.mSuperVisor.mService.mH.post(new Runnable() { // from class: com.android.server.wm.VivoActivityStackSupervisorImpl.3
                            @Override // java.lang.Runnable
                            public void run() {
                                Intent intent = new Intent(VivoActivityStackSupervisorImpl.ACTION_CAR_ACTIVITY_TO_START);
                                intent.addFlags(1073741824);
                                intent.setPackage(VivoActivityStackSupervisorImpl.CAR_NETWORKING_PACKAGE);
                                intent.putExtra(VivoActivityStackSupervisorImpl.PACKAGE_NAME, packageName);
                                VivoActivityStackSupervisorImpl.this.mSuperVisor.mService.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, "android.permission.SET_ACTIVITY_WATCHER");
                            }
                        });
                        VSlog.d("VivoCar", "User confirms whether to start " + r);
                        return true;
                    }
                    DisplayContent carDisplay = this.mSuperVisor.mRootWindowContainer.getDisplayContent((int) SceneManager.APP_REQUEST_PRIORITY);
                    if (carDisplay != null) {
                        if (carDisplay.getTopStack() != null && carDisplay.getTopStack().realActivity != null) {
                            topCarPackage = carDisplay.getTopStack().realActivity.getPackageName();
                        }
                        if (packageName.equals(topCarPackage) && (carContext = carDisplay.getDisplayUiContext()) != null) {
                            UiThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.VivoActivityStackSupervisorImpl.4
                                @Override // java.lang.Runnable
                                public void run() {
                                    Toast.makeText(carContext, 51249178, 0).show();
                                }
                            });
                        }
                    }
                    ArrayList<String> pList3 = VCarConfigManager.getInstance().get("kill_package");
                    if (pList3 != null && pList3.size() > 0 && pList3.contains(packageName)) {
                        Intent tempIntent = getPackageIntent(packageName);
                        final Intent startIntent = tempIntent != null ? tempIntent : r.intent;
                        if (startIntent != null) {
                            this.mSuperVisor.mService.mH.post(new Runnable() { // from class: com.android.server.wm.VivoActivityStackSupervisorImpl.5
                                @Override // java.lang.Runnable
                                public void run() {
                                    try {
                                        VSlog.d("VivoCar", "forcestop and restart  intent=" + startIntent);
                                        ActivityManager.getService().forceStopPackage(packageName, r.mUserId);
                                        VivoActivityStackSupervisorImpl.this.mSuperVisor.mService.mContext.startActivityAsUser(startIntent, UserHandle.CURRENT);
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            return true;
                        }
                    }
                }
                return false;
            }
        }
        return false;
    }

    private Intent getPackageIntent(String packageName) {
        Context context = this.mSuperVisor.mService.mContext;
        Context context2 = this.mSuperVisor.mService.mContext;
        LauncherApps launcherApps = (LauncherApps) context.getSystemService("launcherapps");
        List<LauncherActivityInfo> matches = launcherApps.getActivityList(packageName, Process.myUserHandle());
        if (matches == null || matches.size() == 0) {
            VSlog.d("VivoCar", "cannot find launch intent for package " + packageName);
            return null;
        } else if (matches.get(0) != null) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.setComponent(matches.get(0).getComponentName());
            intent.setFlags(270532608);
            return intent;
        } else {
            return null;
        }
    }

    public String getAppName(String packageName) {
        PackageManager packageManager = this.mSuperVisor.mService.mContext.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
            return applicationName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}