package com.android.server.wm;

import android.app.IAssistDataReceiver;
import android.content.Intent;
import android.util.ArraySet;
import android.util.Slog;
import android.view.IRecentsAnimationRunner;
import android.view.RemoteAnimationAdapter;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoAppTransitionControllerImpl implements IVivoAppTransitionController {
    private static final String TAG = "VivoAppTransitionControllerImpl";
    private ActivityRecord lastHomeOrRecents = null;
    private AppTransitionController mAppTransitionController;
    int mCallingPid;
    int mCallingUid;
    Intent mIntentForRecentsAnim;
    IRecentsAnimationRunner mRunnerForRecentsAnim;
    private WindowManagerService mWmService;

    public VivoAppTransitionControllerImpl(WindowManagerService wms, AppTransitionController appTransitionController) {
        this.mWmService = null;
        if (appTransitionController == null) {
            throw new IllegalArgumentException("appTransitionController must be not null");
        }
        this.mAppTransitionController = appTransitionController;
        this.mWmService = wms;
    }

    public boolean checkLauncherResuming(ArraySet<ActivityRecord> apps) {
        if (apps.size() == 1) {
            ActivityRecord r = apps.valueAt(0);
            if (r.allDrawn && r.mIsResuming && isBBKLauncherLocked(r)) {
                VSlog.i(TAG, "Launcher not resumed, delay app transition.");
                return true;
            }
        }
        return false;
    }

    public boolean ignoreAdjusttTransitInSplitIfNeeded(WindowManagerService service, ActivityRecord topOpeningApp, ActivityRecord topClosingApp) {
        boolean ignore = false;
        if (service == null || !service.isVivoMultiWindowSupport() || !service.isInVivoMultiWindowConsiderVisibilityFocusedDisplay() || topOpeningApp == null || topClosingApp == null) {
            return false;
        }
        if ((isBBKLauncherLocked(topClosingApp) && topOpeningApp.isMultiWindowAppListActivity()) || (isBBKLauncherLocked(topOpeningApp) && topClosingApp.isMultiWindowAppListActivity())) {
            ignore = true;
        }
        if (service.isSplitLogDebug()) {
            VSlog.d(TAG, "ignoreAdjusttTransitInSplitIfNeeded of " + ignore + ",topOpeningApp = " + topOpeningApp + ",topClosingApp is " + topClosingApp);
        }
        return ignore;
    }

    public void recordLastHomeOrRecentsActivity(ActivityRecord target) {
        if (target != null) {
            if (target.isActivityTypeHome() || target.isActivityTypeRecents()) {
                this.lastHomeOrRecents = target;
            }
        }
    }

    public void triggerMinimizedIfNeeded(WindowManagerService service) {
        if (service != null && service.mAtmService.isMultiWindowSupport() && service.isInVivoMultiWindowConsiderVisibilityFocusedDisplay() && this.lastHomeOrRecents != null) {
            if (service.isSplitLogDebug()) {
                VSlog.d(TAG, "triggerMinimizedIfNeeded to checkMinimizeChanged");
            }
            service.getDefaultDisplayContentLocked().getDockedDividerController().checkMinimizeChanged(true);
        }
        this.lastHomeOrRecents = null;
    }

    private boolean isBBKLauncherLocked(ActivityRecord r) {
        return r.toString() != null && r.toString().contains("com.bbk.launcher2/.Launcher");
    }

    public boolean overrideWithRecentsAnimationIfSet(ActivityRecord animLpActivity, int transit, ArraySet<Integer> activityTypes) {
        RemoteAnimationAdapter adapter;
        if (transit == 26 || animLpActivity == null || (adapter = this.mAppTransitionController.getRemoteAnimationOverride(animLpActivity, transit, activityTypes)) == null) {
            return false;
        }
        this.mRunnerForRecentsAnim = adapter.mRecentsAnimRunner;
        this.mIntentForRecentsAnim = adapter.mIntentForRecentsAnim;
        this.mCallingPid = adapter.getCallingPid();
        this.mCallingUid = adapter.getCallingUid();
        VSlog.d("lianjie", "overrideWithRecentsAnimationIfSet : mRunnerForRecentsAnim = " + this.mRunnerForRecentsAnim + " , mIntentForRecentsAnim = " + this.mIntentForRecentsAnim);
        if (this.mRunnerForRecentsAnim != null && this.mIntentForRecentsAnim != null) {
            return true;
        }
        animLpActivity.getDisplayContent().mAppTransition.overridePendingAppTransitionRemote(adapter);
        return false;
    }

    public void startRecentsActivityForRemote() {
        VSlog.d("lianjie", " ~ startRecentsActivityForRemote : callingPid = " + this.mCallingPid + ", runnerForRecentsAnim = " + this.mRunnerForRecentsAnim + " , intentForRecentsAnim = " + this.mIntentForRecentsAnim);
        this.mWmService.mAtmService.startRecentsActivityForRemote(this.mIntentForRecentsAnim, (IAssistDataReceiver) null, this.mRunnerForRecentsAnim, this.mCallingPid, this.mCallingUid);
        this.mRunnerForRecentsAnim = null;
        this.mIntentForRecentsAnim = null;
        this.mCallingPid = 0;
        this.mCallingUid = 0;
    }

    public boolean wouldTryRecentsAnim() {
        ArraySet<ActivityRecord> openingApps = this.mAppTransitionController.mDisplayContent.mOpeningApps;
        int appsCount = openingApps.size();
        for (int i = 0; i < appsCount; i++) {
            ActivityRecord open = (ActivityRecord) openingApps.valueAtUnchecked(i);
            String shortComponentName = open.shortComponentName;
            if (shortComponentName.contains("server.wm.app") || shortComponentName.contains("launcherapps.simpleapp") || shortComponentName.contains("android.accessibilityservice.cts") || shortComponentName.contains("android.media.cts") || shortComponentName.contains("android.app.stubs")) {
                return false;
            }
        }
        return true;
    }

    public void handleOpeningApps(boolean useRecentsAnim) {
        ArraySet<ActivityRecord> openingApps = this.mAppTransitionController.mDisplayContent.mOpeningApps;
        int appsCount = openingApps.size();
        for (int i = 0; i < appsCount; i++) {
            ActivityRecord app = openingApps.valueAt(i);
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                VSlog.d(TAG, "Now opening app: " + app);
            }
            app.commitVisibility(true, false, useRecentsAnim);
            WindowContainer wc = app.getAnimatingContainer(2, 1);
            if (wc == null || !wc.getAnimationSources().contains(app)) {
                this.mAppTransitionController.mDisplayContent.mNoAnimationNotifyOnTransitionFinished.add(app.token);
            }
            app.updateReportedVisibilityLocked();
            app.waitingToShow = false;
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, ">>> OPEN TRANSACTION handleAppTransitionReady()");
            }
            this.mWmService.openSurfaceTransaction();
            try {
                app.showAllWindowsLocked();
                if (this.mAppTransitionController.mDisplayContent.mAppTransition.isNextAppTransitionThumbnailUp()) {
                    app.attachThumbnailAnimation();
                } else if (this.mAppTransitionController.mDisplayContent.mAppTransition.isNextAppTransitionOpenCrossProfileApps()) {
                    app.attachCrossProfileAppsThumbnailAnimation();
                }
            } finally {
                this.mWmService.closeSurfaceTransaction("handleAppTransitionReady");
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i(TAG, "<<< CLOSE TRANSACTION handleAppTransitionReady()");
                }
            }
        }
    }

    public void handleClosingApps(boolean useRecentsAnim) {
        ArraySet<ActivityRecord> closingApps = this.mAppTransitionController.mDisplayContent.mClosingApps;
        int appsCount = closingApps.size();
        for (int i = 0; i < appsCount; i++) {
            ActivityRecord app = closingApps.valueAt(i);
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                VSlog.d(TAG, "Now closing app: " + app);
            }
            app.commitVisibility(false, false, useRecentsAnim);
            app.updateReportedVisibilityLocked();
            app.allDrawn = true;
            if (app.startingWindow != null && !app.startingWindow.mAnimatingExit) {
                app.removeStartingWindow();
            }
            if (this.mAppTransitionController.mDisplayContent.mAppTransition.isNextAppTransitionThumbnailDown()) {
                app.attachThumbnailAnimation();
            }
        }
    }
}