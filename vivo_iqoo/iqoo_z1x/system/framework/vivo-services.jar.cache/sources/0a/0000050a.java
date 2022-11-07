package com.android.server.wm;

import android.app.IActivityController;
import android.content.Context;
import android.multidisplay.MultiDisplayManager;
import android.os.RemoteException;
import com.android.server.Watchdog;
import com.android.server.am.VivoAmsUtils;
import com.android.server.wm.ActivityStack;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoActivityStackImpl implements IVivoActivityStack {
    static final String TAG = "VivoActivityStackImpl";
    private final ActivityStack mActivityStack;
    private final ActivityTaskManagerService mAtmService;
    public AbsVivoPerfManager mPerf;
    private boolean mShouldFinishAllActivitiesAfterMovingToBack = false;
    private VivoAppShareManager mVivoAppShareManager;

    public VivoActivityStackImpl(ActivityTaskManagerService service, ActivityStack as) {
        this.mPerf = null;
        this.mAtmService = service;
        this.mActivityStack = as;
        if (0 == 0) {
            this.mPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        }
        this.mVivoAppShareManager = VivoAppShareManager.getInstance();
    }

    public boolean checkFromActivityController(Task tr) {
        boolean moveOK = true;
        ActivityRecord next = tr.getTopNonFinishingActivity();
        if (next != null) {
            try {
                VivoAmsUtils.setActivityControllerTimeout();
                moveOK = this.mAtmService.mController.activityResuming(next.packageName);
                VivoAmsUtils.cancelActivityControllerTimeout();
            } catch (RemoteException e) {
                this.mAtmService.mController = null;
                Watchdog.getInstance().setActivityController((IActivityController) null);
            }
        }
        VSlog.i(TAG, "moveOK : " + moveOK + " next:" + next);
        return moveOK;
    }

    public void resumeTopActivityInnerLockedBoost(String packageName) {
        this.mPerf.perfHintAsync(4227, packageName, -1, 4);
    }

    public void resumeTopActivityInnerLockedBoost(String packageName, int type) {
        this.mPerf.perfHintAsync(4227, packageName, -1, type);
    }

    public boolean skipAddInNoAnimActivities(String shortComponentName) {
        if ("com.bbk.launcher2/.Launcher".equals(shortComponentName) && this.mAtmService.mRootWindowContainer.getDisplayContent(0).isVivoMultiWindowExitedJustWithDisplay()) {
            VSlog.d(TAG, "skipAddInNoAnimActivities for com.bbk.launcher2, because recent need RemoteAnimation!");
            return true;
        }
        return false;
    }

    private void reparentForMiracast(TaskDisplayArea targetDisplay) {
        if (this.mActivityStack.getDisplayId() == 90000 && targetDisplay.getDisplayId() == 0) {
            this.mAtmService.updateCastStates((String) null);
        } else if (targetDisplay.getDisplayId() == 90000 && this.mActivityStack.getDisplayId() == 0) {
            if (this.mActivityStack.toDesktop) {
                this.mActivityStack.getDisplayArea().moveHomeStackToFront("reparentForMiracast");
            }
            if (this.mActivityStack.mResumedActivity != null) {
                this.mAtmService.updateCastStates(this.mActivityStack.mResumedActivity.packageName);
            }
        }
    }

    public boolean reparentForVirtualDisplay(TaskDisplayArea originalDisplay, TaskDisplayArea targetDisplay, boolean onTop) {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING || originalDisplay == null || targetDisplay == null) {
            return false;
        }
        if (this.mActivityStack.getDisplayId() == 85000 && targetDisplay.getDisplayId() == 0) {
            this.mAtmService.autoExitGameMode(onTop);
        }
        reparentForMiracast(targetDisplay);
        if (MultiDisplayManager.isAnyDisplayRunning() && MultiDisplayManager.isVivoDisplay(originalDisplay.getDisplayId()) && targetDisplay.getDisplayId() == 0 && this.mAtmService.isInMultiWindowDefaultDisplay()) {
            Task task = this.mActivityStack.getRootTask();
            if (!this.mActivityStack.isActivityTypeHome() && task != null && task.supportsSplitScreenWindowingMode()) {
                VSlog.d("VivoCar", task + " will reparent to secondary.");
                task.reparent(targetDisplay.getRootSplitScreenSecondaryTask(), Integer.MAX_VALUE, false, "reparentForVirtualDisplay");
                return true;
            }
        }
        return false;
    }

    public boolean isMainStack() {
        return this.mVivoAppShareManager.isMainStack(this.mActivityStack);
    }

    public void notifyAppShareMoveTaskToBack() {
        if (MultiDisplayManager.isAppShareDisplayId(this.mActivityStack.getDisplayId()) && !isMainStack() && this.mActivityStack.getDisplay() != null && this.mActivityStack.getDisplay().getChildCount() > 1) {
            this.mShouldFinishAllActivitiesAfterMovingToBack = true;
        }
    }

    public void notifyAppShareActivityStateChanged(ActivityStack.ActivityState state) {
        if (state == ActivityStack.ActivityState.STOPPED && this.mShouldFinishAllActivitiesAfterMovingToBack) {
            this.mActivityStack.finishAllActivitiesImmediately();
        }
    }
}