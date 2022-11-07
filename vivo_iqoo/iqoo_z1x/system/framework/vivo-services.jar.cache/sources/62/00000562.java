package com.android.server.wm;

import android.content.Intent;
import android.multidisplay.MultiDisplayManager;
import android.os.Debug;
import android.util.FtFeature;
import com.android.server.wm.KeyguardController;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoKeyguardControllerImpl implements IVivoKeyguardController {
    private static final int KEYGURADVIEW_DISMISS = -200;
    private static final int KEYGURADVIEW_SHOW = 200;
    static final String TAG = "VivoKeyguardControllerImpl";
    private static boolean isKeyguardFeatureOn = FtFeature.isFeatureSupport("vivo.software.keyguard");
    private KeyguardController mController;
    private SnapshotWindow mSnapshotWin;
    private int mCameraState = 0;
    private boolean mTopActivityVisibleWhenKeyguardShowing = false;
    ActivityRecord mLastTopRunningActivity = null;
    ActivityRecord mTopRunningActivity = null;
    private VivoAppShareManager mVivoAppShareManager = VivoAppShareManager.getInstance();

    public VivoKeyguardControllerImpl(KeyguardController controller) {
        this.mController = controller;
    }

    boolean isOccludeChangingFalse() {
        KeyguardController.KeyguardDisplayState state = this.mController.getDisplay(0);
        return (state == null || state.mOccluded || !this.mController.mWindowManager.mPolicy.isKeyguardOccluded()) ? false : true;
    }

    public boolean canShowActivityWhileKeyguardShowing(ActivityRecord r, boolean dismissKeyguard, boolean isTop) {
        ActivityRecord activityRecord;
        if (isCameraPreviewing() || (this.mTopActivityVisibleWhenKeyguardShowing && (((activityRecord = this.mLastTopRunningActivity) == null || !(activityRecord == null || activityRecord.isAnimating())) && r.getDisplayId() == 0))) {
            return true;
        }
        if (dismissKeyguard && this.mController.canDismissKeyguard() && !this.mController.mAodShowing) {
            if (this.mController.mDismissalRequested) {
                return true;
            }
            if (r.canShowWhenLocked() && this.mController.getDisplay(r.getDisplayId()).mDismissingKeyguardActivity != r) {
                return true;
            }
        }
        return false;
    }

    public boolean canShowWhileOccluded(boolean dismissKeyguard, boolean showWhenLocked, boolean isTop) {
        return showWhenLocked || (dismissKeyguard && !this.mController.mWindowManager.isKeyguardSecure(this.mController.mService.getCurrentUserId()));
    }

    public boolean handleAllowOccludeKeyguard(KeyguardController.KeyguardDisplayState state, ActivityRecord mTopRunningActivity, boolean allow, boolean mOccluded, boolean lastOccluded) {
        VSlog.d(TAG, "handleAllowOccludeKeyguard FOR " + state.mDisplayId + " mTopRunningActivity = " + mTopRunningActivity + " mLastTopRunningActivity = " + this.mLastTopRunningActivity + " , mOccluded = " + state.mOccluded + ", lastOccluded = " + lastOccluded + ", allow = " + allow + Debug.getCallers(15));
        if (MultiDisplayManager.isResCarDisplay(state.mDisplayId)) {
            VSlog.d("VivoCar", "skip handleAllowOccludeKeyguard!");
            return true;
        } else if (allow || !mOccluded) {
            this.mController.handleOccludedChanged(state.mDisplayId);
            return mOccluded;
        } else if (this.mController.isKeyguardLocked() && mTopRunningActivity != null) {
            if ((mTopRunningActivity.canShowWhenLocked() || (mTopRunningActivity.containsDismissKeyguardWindow() && !mTopRunningActivity.mStartAllowOccludeKeyguard)) && !mTopRunningActivity.finishing) {
                VSlog.d(TAG, "finish OccludeActivity: " + mTopRunningActivity);
                this.mController.mWindowManager.allowOverlayKeyguard(mTopRunningActivity.packageName, mTopRunningActivity.shortComponentName, mTopRunningActivity.getUid());
                this.mController.mService.finishActivity(mTopRunningActivity.appToken.asBinder(), 0, (Intent) null, 0);
                return false;
            }
            return mOccluded;
        } else {
            return mOccluded;
        }
    }

    public boolean handleAllowDismissKeyguard(KeyguardController.KeyguardDisplayState state, ActivityRecord mTopRunningActivity, boolean allow, boolean request, ActivityRecord lastDismissActivity) {
        VSlog.d(TAG, " lastDismissActivity = " + lastDismissActivity);
        if (allow) {
            return true;
        }
        if (mTopRunningActivity != null && this.mController.isKeyguardLocked() && state.mDismissingKeyguardActivity != null) {
            if ((state.mDismissingKeyguardActivity.canShowWhenLocked() || state.mDismissingKeyguardActivity.containsDismissKeyguardWindow()) && !state.mDismissingKeyguardActivity.finishing && !mTopRunningActivity.mStartAllowOccludeKeyguard) {
                VSlog.d(TAG, "finish DismissActivity: " + state.mDismissingKeyguardActivity);
                this.mController.mWindowManager.allowOverlayKeyguard(mTopRunningActivity.packageName, mTopRunningActivity.shortComponentName, mTopRunningActivity.getUid());
                this.mController.mService.finishActivity(state.mDismissingKeyguardActivity.appToken.asBinder(), 0, (Intent) null, 0);
                state.mDismissingKeyguardActivity = lastDismissActivity;
                return request;
            }
            return request;
        }
        return request;
    }

    public void setKeyguardShown(boolean keyguardShowing, boolean aodShowing, boolean aodChanged, boolean mKeyguardGoingAway) {
        VSlog.d(TAG, "setKeyguardShown keyguardShowing = " + keyguardShowing + ", aodShowing = " + aodShowing + ", aodChanged = " + aodChanged + ", mKeyguardGoingAway = " + mKeyguardGoingAway + " ,caller by " + Debug.getCallers(10));
    }

    public void visibilitiesUpdated(KeyguardController controller, DisplayContent display, KeyguardController.KeyguardDisplayState state) {
        if (MultiDisplayManager.isResCarDisplay(state.mDisplayId)) {
            VSlog.d("VivoCar", "skip visibilitiesUpdated check!");
            return;
        }
        boolean lastOccluded = state.mOccluded;
        ActivityRecord lastDismissActivity = state.mDismissingKeyguardActivity;
        state.mRequestDismissKeyguard = false;
        state.mOccluded = false;
        state.mDismissingKeyguardActivity = null;
        boolean allow = true;
        this.mTopRunningActivity = null;
        ActivityStack stack = state.getStackForControllingOccluding(display);
        if (stack != null) {
            ActivityRecord topDismissing = stack.getTopDismissingKeyguardActivity();
            boolean z = true;
            state.mOccluded = stack.topActivityOccludesKeyguard() || (topDismissing != null && stack.topRunningActivityLocked() == topDismissing && controller.canShowWhileOccluded(true, false));
            if (stack.getTopDismissingKeyguardActivity() != null) {
                state.mDismissingKeyguardActivity = stack.getTopDismissingKeyguardActivity();
            }
            if (state.mDisplayId != 0) {
                boolean z2 = state.mOccluded;
                if (!stack.canShowWithInsecureKeyguard() || !controller.canDismissKeyguard()) {
                    z = false;
                }
                state.mOccluded = z2 | z;
            }
            ActivityRecord activityRecord = stack.topRunningActivityLocked();
            this.mTopRunningActivity = activityRecord;
            if (activityRecord != null && controller.isKeyguardLocked()) {
                if (this.mTopRunningActivity != null && controller.mService.vivoCanOccludeKeyguard(this.mTopRunningActivity.packageName, this.mTopRunningActivity.isVivoOccludeKeyguard()) && state.mOccluded) {
                    controller.mService.notifyVivoOccludeChange(this.mTopRunningActivity.packageName, 0);
                } else if (this.mLastTopRunningActivity != null && controller.mService.vivoCanOccludeKeyguard(this.mLastTopRunningActivity.packageName, this.mLastTopRunningActivity.isVivoOccludeKeyguard())) {
                    controller.mService.notifyVivoOccludeChange(this.mLastTopRunningActivity.packageName, 3);
                }
            }
        }
        if (state.mDisplayId == 0) {
            state.mOccluded |= controller.mService.mRootWindowContainer.getDefaultDisplay().mDisplayContent.getDisplayPolicy().isShowingDreamLw();
        }
        if (this.mTopRunningActivity != null && state.mOccluded && !(allow = controller.mWindowManager.allowOverlayKeyguard(this.mTopRunningActivity.packageName)) && this.mTopRunningActivity.mStartAllowOccludeKeyguard) {
            state.mOccluded = false;
        }
        boolean allow2 = allow;
        if (lastOccluded != state.mOccluded) {
            state.mOccluded = handleAllowOccludeKeyguard(state, this.mTopRunningActivity, allow2, state.mOccluded, lastOccluded);
        }
        if (state.mOccluded) {
            this.mLastTopRunningActivity = this.mTopRunningActivity;
        }
        if (lastDismissActivity != state.mDismissingKeyguardActivity && !state.mOccluded && state.mDismissingKeyguardActivity != null && controller.mWindowManager.isKeyguardSecure(controller.mService.getCurrentUserId())) {
            state.mRequestDismissKeyguard = handleAllowDismissKeyguard(state, this.mTopRunningActivity, allow2, state.mRequestDismissKeyguard, lastDismissActivity);
        }
        ActivityRecord activityRecord2 = this.mLastTopRunningActivity;
        if (activityRecord2 != null && activityRecord2.finishing && !this.mLastTopRunningActivity.isAnimating()) {
            VSlog.d(TAG, "last occlude activity finished, r = " + this.mLastTopRunningActivity);
            this.mLastTopRunningActivity = null;
        }
    }

    public void setCameraStateOnKeyguard(int state) {
        if (state == 200 || state == -200) {
            try {
                if (this.mSnapshotWin == null && this.mController.mWindowManager != null && this.mController.mWindowManager.mContext != null) {
                    this.mSnapshotWin = SnapshotWindow.getInstance(this.mController.mWindowManager.mContext);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("notifykeyguardViewVisibleChange state =");
                sb.append(state);
                sb.append("; ");
                sb.append(this.mSnapshotWin != null);
                VSlog.d(TAG, sb.toString());
                if (this.mSnapshotWin != null) {
                    this.mSnapshotWin.notifykeyguardViewVisibleChange(state);
                    return;
                }
                return;
            } catch (Exception e) {
                VSlog.w(TAG, "notifykeyguardViewVisibleChange Exception =" + state, e);
                return;
            }
        }
        VSlog.d(TAG, "setCameraStateOnKeyguard state =" + state + " laststate = " + this.mCameraState);
        if (state != this.mCameraState) {
            this.mCameraState = state;
            this.mController.updateKeyguardSleepToken();
        }
    }

    public void setTopActivityVisibleWhenKeyguardShowing(boolean visible) {
    }

    public boolean isCameraPreviewing() {
        return this.mCameraState == 1;
    }

    public DisplayContent getAppShareDisplay() {
        return this.mVivoAppShareManager.getAppShareDisplay();
    }
}