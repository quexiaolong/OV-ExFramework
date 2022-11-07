package com.android.server.wm;

import android.graphics.GraphicBuffer;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;
import android.view.InsetsSource;
import android.view.InsetsState;
import android.view.RemoteAnimationTarget;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;
import com.android.server.wm.BarAnimController;
import com.android.server.wm.SurfaceAnimator;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowToken;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.sdk.Consts;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class BarAnimController {
    private static final int ANIMATION_MODE_FOR_FAKE_GESTURE = 6;
    private static final int ANIMATION_MODE_FOR_FAKE_STATUS = 4;
    private static final int ANIMATION_MODE_FOR_GESTURE = 5;
    private static final int ANIMATION_MODE_FOR_STATUS = 3;
    private static final String BLANK_ACTIVITY_SHORT_COMP = "com.bbk.launcher2/com.android.quickstep.recents.RecentsNoDisplayActivity";
    private static final String LAUNCHER_ACTIVITY_SHORT_COMP = "com.bbk.launcher2/.Launcher";
    private static final int STATE_REMOTE_ANIM_BACK_HOME_INITIAL = 2;
    private static final int STATE_REMOTE_ANIM_FROM_HOME_INITIAL = 1;
    private static final int STATE_REMOTE_ANIM_NONE = 0;
    private static final int STATE_REMOTE_ANIM_ROTATE = 3;
    private static final int STATE_TRANSITION_IDEL = 0;
    private static final int STATE_TRANSITION_INITIAL = 1;
    private static final int STATE_TRANSITION_READY = 2;
    static final String TAG = "BarAnimController";
    private static final long TIME_OUT_FOR_NOT_READY = 1000;
    private static final int TYPE_FAKE_GESTURE = 1;
    private static final int TYPE_FAKE_STATUS = 0;
    private DisplayContent mDisplayContent;
    private FakeBarSurfaceControl mFakeGesture;
    private FakeBarSurfaceControl mFakeStatus;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private ArrayList<RemoteAnimationTarget> mPendingBarAnimTargets;
    private ActivityRecord mTopClosingApp;
    private ActivityRecord mTopOpeningApp;
    private final boolean DEBUG_COLOR = SystemProperties.getBoolean("persist.bar.anim.color", false);
    private int mTransitionState = 0;
    private int mRemoteAnimState = 0;
    private boolean mWaitingForSideSlideRemote = false;
    private boolean mIsSideSlideRemote = false;
    private boolean mIsBackToRecents = false;
    private boolean mIsLandscapeGesture = false;
    private ActivityRecord mRealStatusParent = null;
    private ActivityRecord mRealGestureParent = null;
    private ActivityRecord mFakeStatusParent = null;
    private ActivityRecord mFakeGestureParent = null;
    private ActivityRecord mActivtityResponsibleForStatus = null;
    private ActivityRecord mActivtityResponsibleForNav = null;
    private WindowState mLastWinForStatus = null;
    private boolean mBarsFinalShowingIsFake = false;
    private ArrayList<AnimationAdapter> mPendingBarsAnimations = new ArrayList<>();
    private boolean mHasCaptureReady = false;
    private long mLastTimeNotReady = -1;
    private ArraySet<WindowManagerService.RotationWatcher> mGestureRotationWatchers = new ArraySet<>();
    private Rect mTmpSavedFrame = new Rect();
    private int mGestureLastFixedRotation = -1;
    private Runnable mResetRunnable = new Runnable() { // from class: com.android.server.wm.-$$Lambda$BarAnimController$X-o0mnBsz0RqvEZzkPEh3jwPqok
        @Override // java.lang.Runnable
        public final void run() {
            BarAnimController.this.lambda$new$0$BarAnimController();
        }
    };
    private Runnable mCaptureRunnable = new Runnable() { // from class: com.android.server.wm.-$$Lambda$BarAnimController$vtzqEUN8WaC1PB3Ew0aWlemP2_4
        @Override // java.lang.Runnable
        public final void run() {
            BarAnimController.this.lambda$new$1$BarAnimController();
        }
    };

    public BarAnimController(DisplayContent dc) {
        VSlog.d(TAG, "create BarAnimController");
        this.mDisplayContent = dc;
        this.mFakeStatus = new FakeBarSurfaceControl(2000);
        this.mFakeGesture = new FakeBarSurfaceControl(2024);
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
    }

    public boolean isBarAnimReady() {
        int i;
        if (this.mTransitionState == 1 || (i = this.mRemoteAnimState) == 1 || i == 2) {
            if (!this.mHasCaptureReady) {
                long curTime = SystemClock.uptimeMillis();
                long j = this.mLastTimeNotReady;
                if (j != -1 && curTime - j > 1000) {
                    this.mLastTimeNotReady = -1L;
                    VSlog.d(TAG, "isBarAnimReady timeout");
                    return true;
                } else if (this.mLastTimeNotReady == -1) {
                    this.mLastTimeNotReady = curTime;
                    VSlog.d(TAG, "isBarAnimReady = false");
                }
            } else {
                this.mLastTimeNotReady = -1L;
                VSlog.d(TAG, "isBarAnimReady = true");
            }
            return this.mHasCaptureReady;
        }
        return true;
    }

    public void prepareAppTransition() {
        int transit = this.mDisplayContent.mAppTransition.getAppTransition();
        VSlog.d(TAG, "prepareAppTransition " + transit + " " + this.mTransitionState);
        int i = this.mTransitionState;
        if ((i == 0 || i == 2) && AppTransition.isTaskTransit(transit)) {
            if (!isStateSuitable()) {
                return;
            }
            clearRestRunnable();
            this.mTransitionState = 1;
            FillFakeBarsWithContent();
        } else if (this.mTransitionState > 0 && !AppTransition.isTaskTransit(transit)) {
            this.mTransitionState = 0;
            judgeStateIsResetNeeded();
        }
    }

    public void setActivityVisibilityBeforeCommit() {
        if (this.mTransitionState == 0 && this.mRemoteAnimState != 0) {
            return;
        }
        this.mTopOpeningApp = getTopApp(this.mDisplayContent.mOpeningApps);
        this.mTopClosingApp = getTopApp(this.mDisplayContent.mClosingApps);
        VSlog.d(TAG, "setActivityVisibilityBeforeCommit " + this.mTopOpeningApp + " " + this.mTopClosingApp);
    }

    public void transferStartingWindow(ActivityRecord fromActivity, ActivityRecord toActivity) {
        WindowState gesture;
        WindowState status;
        VSlog.d(TAG, "transferStartingWindow fromActivity = " + fromActivity + " toActivity = " + toActivity);
        if (fromActivity != null && toActivity != null) {
            if (fromActivity == this.mRealStatusParent && (status = getRealBarFromFakeBarType(0)) != null && status.mToken != null) {
                status.mToken.reparentSurfaceControl(this.mDisplayContent.getPendingTransaction(), toActivity.getSurfaceControl());
                this.mRealStatusParent = toActivity;
            }
            if (fromActivity == this.mRealGestureParent && (gesture = getRealBarFromFakeBarType(1)) != null && gesture.mToken != null) {
                gesture.mToken.reparentSurfaceControl(this.mDisplayContent.getPendingTransaction(), toActivity.getSurfaceControl());
                this.mRealGestureParent = toActivity;
            }
            if (fromActivity == this.mRealStatusParent) {
                this.mFakeStatus.reparentTo(toActivity);
                this.mFakeStatusParent = toActivity;
            }
            if (fromActivity == this.mRealGestureParent) {
                this.mFakeGesture.reparentTo(toActivity);
                this.mFakeGestureParent = toActivity;
            }
        }
    }

    public void handleAppTransitionReady(int transit) {
        VSlog.d(TAG, "handleAppTransitionReady mTransitionState = " + this.mTransitionState + " mRemoteAnimState = " + this.mRemoteAnimState + " transit = " + transit);
        if (this.mTransitionState == 0) {
            return;
        }
        Iterator it = this.mDisplayContent.mClosingApps.iterator();
        while (it.hasNext()) {
            ActivityRecord record = (ActivityRecord) it.next();
            if (record != null && LAUNCHER_ACTIVITY_SHORT_COMP.equals(record.shortComponentName) && record.getRemoteAnimationDefinition() != null) {
                VSlog.d(TAG, "handleAppTransitionReady, closing app has launcher, wait for recentsAnimation");
                return;
            }
        }
        if (!isTopAppFull(this.mTopClosingApp)) {
            this.mTopClosingApp = null;
        }
        if (!isTopAppFull(this.mTopOpeningApp)) {
            this.mTopOpeningApp = null;
        }
        this.mTransitionState = 2;
        this.mBarsFinalShowingIsFake = false;
        try {
            notifyBarReparent();
        } catch (Exception e) {
            VSlog.e(TAG, "handleAppTransitionReady " + e.toString());
            e.printStackTrace();
        }
    }

    public void appTransitionFinished() {
        VSlog.d(TAG, "appTransitionFinished");
        this.mTransitionState = 0;
        this.mHasCaptureReady = false;
        judgeStateIsResetNeeded();
    }

    public void onRotationChanged() {
        VSlog.d(TAG, "onRotationChanged mRemoteAnimState = " + this.mRemoteAnimState);
        if (this.mRemoteAnimState == 3) {
            this.mRemoteAnimState = 0;
            judgeStateIsResetNeeded();
        }
    }

    public void notifyRemoteAnimationInitial(ArrayList<Task> visibleTasks, ActivityRecord homeApp, boolean isFromRemote, boolean isLandscapeGesture) {
        ActivityRecord app;
        VSlog.d(TAG, "notifyRemoteAnimationInitial homeApp=" + homeApp + " isFromRemote=" + isFromRemote + " isLandscapeGesture=" + isLandscapeGesture);
        if (!isStateSuitable()) {
            return;
        }
        clearRestRunnable();
        if (isFromRemote) {
            this.mRemoteAnimState = 1;
            ActivityRecord activityRecord = this.mTopClosingApp;
            if (activityRecord == null || !BLANK_ACTIVITY_SHORT_COMP.equals(activityRecord.shortComponentName)) {
                this.mTopClosingApp = homeApp;
            }
            this.mIsSideSlideRemote = this.mWaitingForSideSlideRemote;
            this.mWaitingForSideSlideRemote = false;
            return;
        }
        this.mRemoteAnimState = 2;
        FillFakeBarsWithContent();
        ActivityRecord topActivity = null;
        int taskCount = visibleTasks.size();
        int i = 0;
        while (true) {
            if (i >= taskCount) {
                break;
            }
            Task task = visibleTasks.get(i);
            if (task.getConfiguration().windowConfiguration.getWindowingMode() != 1 || (app = getTopApp(new ArraySet<>((Collection<? extends Object>) task.mChildren))) == null) {
                i++;
            } else {
                topActivity = app;
                break;
            }
        }
        this.mTopClosingApp = topActivity == homeApp ? null : topActivity;
        this.mTopOpeningApp = homeApp;
        this.mIsLandscapeGesture = isLandscapeGesture;
    }

    public void notifyRemoteAnimationStart() {
        VSlog.d(TAG, "notifyRemoteAnimationStart");
        int i = this.mRemoteAnimState;
        if (i != 1 && i != 2) {
            return;
        }
        notifyBarReparent();
    }

    public void notifyRemoteAnimationFinished(int reorderMode, int behavMode) {
        ActivityRecord activityRecord;
        VSlog.d(TAG, "notifyRemoteAnimationFinished " + this.mRemoteAnimState + " " + reorderMode + " " + behavMode);
        this.mIsBackToRecents = behavMode == 2;
        if ((this.mRemoteAnimState == 2 && reorderMode != 2) || (this.mRemoteAnimState == 1 && reorderMode == 2)) {
            this.mWaitingForSideSlideRemote = behavMode == 1;
            this.mHasCaptureReady = false;
            this.mBarsFinalShowingIsFake = false;
            if (this.mIsLandscapeGesture && this.mIsBackToRecents) {
                this.mActivtityResponsibleForStatus = null;
                this.mActivtityResponsibleForNav = null;
                updateStatusFixedRotationTransform(null);
                updateGestureRotation(null);
                this.mDisplayContent.getDisplayPolicy().updateSystemUiVisibilityLw();
                this.mDisplayContent.setLayoutNeeded();
            }
            if ((!this.mIsLandscapeGesture || !this.mIsBackToRecents) && (activityRecord = this.mTopClosingApp) != null && this.mTopOpeningApp != null && activityRecord.getWindowConfiguration().getRotation() != this.mTopOpeningApp.getWindowConfiguration().getRotation()) {
                this.mRemoteAnimState = 3;
                if (!this.mWaitingForSideSlideRemote) {
                    removeRemoteAnimations();
                    ActivityRecord activityRecord2 = this.mRealStatusParent;
                    if (activityRecord2 != null) {
                        activityRecord2.assignChildLayers();
                    }
                    ActivityRecord activityRecord3 = this.mRealGestureParent;
                    if (activityRecord3 != null) {
                        activityRecord3.assignChildLayers();
                        return;
                    }
                    return;
                }
                return;
            }
        } else if ((this.mRemoteAnimState == 2 && reorderMode == 2) || (this.mRemoteAnimState == 1 && reorderMode != 2)) {
            this.mBarsFinalShowingIsFake = true;
            ActivityRecord activityRecord4 = this.mTopClosingApp;
            this.mActivtityResponsibleForStatus = activityRecord4;
            this.mActivtityResponsibleForNav = activityRecord4;
            updateStatusFixedRotationTransform(null);
            updateGestureRotation(null);
            this.mDisplayContent.getDisplayPolicy().updateSystemUiVisibilityLw();
            this.mDisplayContent.setLayoutNeeded();
        }
        this.mRemoteAnimState = 0;
        judgeStateIsResetNeeded();
    }

    public void notifyWindowRemoved(WindowState win) {
        VSlog.d(TAG, "notifyWindowRemoved  " + win);
        if (win == getRealBarFromFakeBarType(0) && this.mRealStatusParent != null) {
            resetFakeBarForType(getFakeBarTypeFromRealBarType(win.getAttrs().type));
        } else if (win == getRealBarFromFakeBarType(1) && this.mRealStatusParent != null) {
            resetFakeBarForType(getFakeBarTypeFromRealBarType(win.getAttrs().type));
        }
    }

    public void notifyAppRemoved(ActivityRecord token) {
        VSlog.d(TAG, "notifyAppRemoved  " + token);
        if (token == this.mRealStatusParent) {
            resetRealBarForType(0);
        }
        if (token == this.mRealGestureParent) {
            resetRealBarForType(1);
        }
        if (token == this.mFakeStatusParent) {
            resetFakeBarForType(0);
        }
        if (token == this.mFakeGestureParent) {
            resetFakeBarForType(1);
        }
        if (token == this.mTopOpeningApp) {
            this.mTopOpeningApp = null;
        }
        if (token == this.mTopClosingApp) {
            this.mTopClosingApp = null;
        }
    }

    public DisplayFrames getFixedDisplayFrames(WindowState win, DisplayFrames displayFrames) {
        ActivityRecord activityRecord;
        ActivityRecord activityRecord2;
        WindowState status = getRealBarFromFakeBarType(0);
        WindowState gesture = getRealBarFromFakeBarType(1);
        DisplayFrames fixed = null;
        if (win == status && (activityRecord2 = this.mActivtityResponsibleForStatus) != null) {
            fixed = activityRecord2.getFixedRotationTransformDisplayFrames();
        }
        if (win == gesture && (activityRecord = this.mActivtityResponsibleForNav) != null) {
            fixed = activityRecord.getFixedRotationTransformDisplayFrames();
        }
        if (fixed != null) {
            VSlog.d(TAG, "getFixedDisplayFrames " + fixed.mUnrestricted + " for " + win);
            return fixed;
        }
        return displayFrames;
    }

    public InsetsState mayFixedState(InsetsState state) {
        WindowState status;
        InsetsSource source = state.peekSource(0);
        if (source != null && (status = getRealBarFromFakeBarType(0)) != null && status.mToken != null && status.mToken.mFixedRotationTransformState != null) {
            source.setFrame(this.mTmpSavedFrame);
        }
        return state;
    }

    public WindowState getWindowContainerForStatus(WindowState wc) {
        WindowState windowState;
        ActivityRecord token = this.mActivtityResponsibleForStatus;
        if (wc == this.mLastWinForStatus) {
            this.mLastWinForStatus = null;
        }
        if (token == null && (windowState = this.mLastWinForStatus) != null && wc != null && windowState.mActivityRecord == wc.mActivityRecord) {
            token = wc.mActivityRecord;
        }
        WindowState top = getTopFullWin(token);
        if (top != null) {
            this.mLastWinForStatus = top;
        }
        if (this.DEBUG_COLOR) {
            VSlog.d(TAG, "getWindowContainerForStatus " + top + ", default:" + wc + " mActivtityResponsibleForStatus=" + this.mActivtityResponsibleForStatus);
        }
        return top != null ? top : wc;
    }

    public WindowState getWindowContainerForGesture(WindowState wc) {
        ActivityRecord token = this.mActivtityResponsibleForNav;
        WindowState top = getTopFullWin(token);
        if (this.DEBUG_COLOR) {
            VSlog.d(TAG, "getWindowContainerForGesture " + top + ", default:" + wc);
        }
        return top != null ? top : wc;
    }

    public boolean shouldHideStatusBar() {
        WindowState win = getWindowContainerForStatus(null);
        if (win != null && (win.getAttrs().flags & Consts.ProcessStates.FOCUS) == 0 && (PolicyControl.getSystemUiVisibility(win, (WindowManager.LayoutParams) null) & 4) == 0) {
            return false;
        }
        return true;
    }

    public boolean shouldWinApplyRotation(WindowState win) {
        if ((this.mRealStatusParent == null || win != getRealBarFromFakeBarType(0)) && (this.mRealGestureParent == null || win != getRealBarFromFakeBarType(1))) {
            return true;
        }
        VSlog.d(TAG, "shouldWinApplyRotation, return false for " + win);
        return false;
    }

    public boolean shouldChangeDrawState(WindowState win) {
        WindowState status = getRealBarFromFakeBarType(0);
        WindowState gesture = getRealBarFromFakeBarType(1);
        if (status != win && gesture != win) {
            return true;
        }
        VSlog.d(TAG, "shouldChangeDrawState, return false for " + win);
        return false;
    }

    public RemoteAnimationTarget[] startRemoteAnimationForBars(long durationHint, long statusBarTransitionDelay, Consumer<AnimationAdapter> animationCanceledRunnable, ArrayList<AnimationAdapter> adaptersOut) {
        if (this.mRemoteAnimState == 0 && this.mTransitionState == 0) {
            return null;
        }
        boolean isRecents = this.mRemoteAnimState != 0;
        VSlog.d(TAG, "startRemoteAnimationForBars");
        ArrayList<RemoteAnimationTarget> arrayList = this.mPendingBarAnimTargets;
        if (arrayList != null && arrayList.size() > 0) {
            VSlog.d(TAG, "startRemoteAnimationForBars re-use old");
            ArrayList<RemoteAnimationTarget> targets = this.mPendingBarAnimTargets;
            Collection<? extends AnimationAdapter> collection = this.mPendingBarsAnimations;
            if (collection != null) {
                adaptersOut.addAll(collection);
            }
            return (RemoteAnimationTarget[]) targets.toArray(new RemoteAnimationTarget[targets.size()]);
        }
        ArrayList<RemoteAnimationTarget> targets2 = new ArrayList<>();
        WindowState status = getRealBarFromFakeBarType(0);
        if (status != null && status.mToken != null && this.mFakeStatus != null) {
            WindowToken statusToken = status.mToken;
            BarAnimationAdapter barAdapter = new BarAnimationAdapter(statusToken, durationHint, statusBarTransitionDelay, animationCanceledRunnable, false);
            BarAnimationAdapter barAdapterForFake = new BarAnimationAdapter(statusToken, durationHint, statusBarTransitionDelay, animationCanceledRunnable, true);
            statusToken.startAnimation(statusToken.getPendingTransaction(), barAdapter, false, 16);
            this.mFakeStatus.mSurfaceAnimator.startAnimation(statusToken.getPendingTransaction(), barAdapterForFake, false, 16);
            targets2.add(barAdapter.createRemoteAnimationTarget(3));
            targets2.add(barAdapterForFake.createRemoteAnimationTarget(4));
            adaptersOut.add(barAdapter);
            adaptersOut.add(barAdapterForFake);
        }
        WindowState gesture = getRealBarFromFakeBarType(1);
        if (gesture != null && gesture.mToken != null && this.mFakeGesture != null) {
            WindowToken gestureToken = gesture.mToken;
            BarAnimationAdapter barAdapter2 = new BarAnimationAdapter(gestureToken, durationHint, statusBarTransitionDelay, animationCanceledRunnable, false);
            BarAnimationAdapter barAdapterForFake2 = new BarAnimationAdapter(gestureToken, durationHint, statusBarTransitionDelay, animationCanceledRunnable, true);
            gestureToken.startAnimation(gestureToken.getPendingTransaction(), barAdapter2, false, 16);
            this.mFakeGesture.mSurfaceAnimator.startAnimation(gestureToken.getPendingTransaction(), barAdapterForFake2, false, 16);
            targets2.add(barAdapter2.createRemoteAnimationTarget(5));
            targets2.add(barAdapterForFake2.createRemoteAnimationTarget(6));
            adaptersOut.add(barAdapter2);
            adaptersOut.add(barAdapterForFake2);
        }
        if (isRecents) {
            this.mPendingBarAnimTargets = targets2;
            this.mPendingBarsAnimations = adaptersOut;
        }
        return (RemoteAnimationTarget[]) targets2.toArray(new RemoteAnimationTarget[targets2.size()]);
    }

    public SurfaceControl getParentSurfaceControlForToken(WindowToken token) {
        WindowState gesture;
        ActivityRecord activityRecord;
        ActivityRecord activityRecord2;
        if (token.windowType == 2000) {
            WindowState status = getRealBarFromFakeBarType(0);
            if (status == null || status.mToken != token || (activityRecord2 = this.mRealStatusParent) == null) {
                return null;
            }
            return activityRecord2.getSurfaceControl();
        } else if (token.windowType != 2024 || (gesture = getRealBarFromFakeBarType(1)) == null || gesture.mToken != token || (activityRecord = this.mRealGestureParent) == null) {
            return null;
        } else {
            return activityRecord.getSurfaceControl();
        }
        return null;
    }

    public void assignChildLayersForStatus(WindowContainer parent, int layer) {
        FakeBarSurfaceControl fakeBarSurfaceControl;
        FakeBarSurfaceControl fakeBarSurfaceControl2;
        if (parent == this.mRealStatusParent) {
            WindowState status = getRealBarFromFakeBarType(0);
            if (status != null) {
                status.mToken.setLayer(parent.getSyncTransaction(), layer);
                status.mToken.mLastLayer = layer;
                layer++;
            }
        } else if (parent == this.mFakeStatusParent && (fakeBarSurfaceControl = this.mFakeStatus) != null) {
            fakeBarSurfaceControl.setLayer(parent.getSyncTransaction(), layer);
            layer++;
        }
        if (parent != this.mRealGestureParent) {
            if (parent == this.mFakeGestureParent && (fakeBarSurfaceControl2 = this.mFakeGesture) != null) {
                int i = layer + 1;
                fakeBarSurfaceControl2.setLayer(parent.getSyncTransaction(), layer);
                return;
            }
            return;
        }
        WindowState gesture = getRealBarFromFakeBarType(1);
        if (gesture != null) {
            gesture.mToken.setLayer(parent.getSyncTransaction(), layer);
            int i2 = layer + 1;
            gesture.mToken.mLastLayer = layer;
        }
    }

    public boolean shouldAssignLayerForWindowContainer(WindowContainer wc) {
        WindowState gesture;
        WindowState status;
        if (this.mRealStatusParent == null || (status = getRealBarFromFakeBarType(0)) == null || status.mToken != wc) {
            return this.mRealGestureParent == null || (gesture = getRealBarFromFakeBarType(1)) == null || gesture.mToken != wc;
        }
        return false;
    }

    public SurfaceControl[] getBarsLayers() {
        ArrayList<SurfaceControl> barsLayers = new ArrayList<>();
        barsLayers.add(this.mFakeStatus.mSurfaceControl);
        barsLayers.add(this.mFakeGesture.mSurfaceControl);
        WindowState status = getRealBarFromFakeBarType(0);
        if (status != null) {
            barsLayers.add(status.mToken.mSurfaceControl);
        }
        WindowState gesture = getRealBarFromFakeBarType(1);
        if (gesture != null) {
            barsLayers.add(gesture.mToken.mSurfaceControl);
        }
        return (SurfaceControl[]) barsLayers.toArray(new SurfaceControl[barsLayers.size()]);
    }

    public void notifyRotationWatcher(WindowManagerService.RotationWatcher watcher, int pid) {
        WindowState gesture = this.mDisplayContent.getDisplayPolicy().getBottomGestureBar();
        if (gesture != null) {
            if (gesture.mSession.mPid == pid) {
                this.mGestureRotationWatchers.add(watcher);
                return;
            }
            return;
        }
        WindowProcessController app = this.mDisplayContent.mWmService.mAtmService.mProcessMap.getProcess(pid);
        if (app != null && app.mName != null && app.mName.equals("com.vivo.upslide")) {
            this.mGestureRotationWatchers.add(watcher);
        }
    }

    public void notifyRotationWatcherRemoved(WindowManagerService.RotationWatcher watcher) {
        this.mGestureRotationWatchers.remove(watcher);
    }

    public boolean shouldFixedRotationAnimateForBar() {
        if (this.mTransitionState == 0 && this.mRemoteAnimState == 0 && this.mDisplayContent.mWmService.getRecentsAnimationController() == null && !AppTransition.isTaskTransit(this.mDisplayContent.mAppTransition.getAppTransition())) {
            return true;
        }
        return false;
    }

    public boolean isInFakeBarCapture(WindowState wc) {
        if (wc == getRealBarFromFakeBarType(0) && this.mFakeStatus.mInCapture) {
            return true;
        }
        return wc == getRealBarFromFakeBarType(1) && this.mFakeGesture.mInCapture;
    }

    private boolean isTopAppFull(ActivityRecord topApp) {
        if (topApp != null && !topApp.fillsParent() && !"com.vivo.globalsearch".equals(topApp.packageName)) {
            return false;
        }
        return true;
    }

    private ActivityRecord getTopApp(ArraySet<? extends WindowContainer> apps) {
        int prefixOrderIndex;
        int topPrefixOrderIndex = Integer.MIN_VALUE;
        ActivityRecord topApp = null;
        for (int i = apps.size() - 1; i >= 0; i--) {
            ActivityRecord app = AppTransitionController.getAppFromContainer(apps.valueAt(i));
            if (app != null && !app.mIsExiting && app.getConfiguration().windowConfiguration.getWindowingMode() == 1 && (prefixOrderIndex = app.getPrefixOrderIndex()) > topPrefixOrderIndex) {
                topPrefixOrderIndex = prefixOrderIndex;
                topApp = app;
            }
        }
        return topApp;
    }

    private void notifyBarReparent() {
        WindowState status = getRealBarFromFakeBarType(0);
        WindowState gesture = getRealBarFromFakeBarType(1);
        ActivityRecord activityRecord = this.mTopClosingApp;
        if (activityRecord != null && !activityRecord.mIsExiting) {
            this.mFakeStatus.reparentTo(this.mTopClosingApp);
            this.mFakeStatus.show();
            WindowContainer windowContainer = this.mTopClosingApp;
            this.mFakeStatusParent = windowContainer;
            this.mFakeGesture.reparentTo(windowContainer);
            this.mFakeGesture.show();
            this.mFakeGestureParent = this.mTopClosingApp;
            VSlog.d(TAG, "notifyBarReparent fake bars reparent to " + this.mTopClosingApp);
        }
        ActivityRecord activityRecord2 = this.mTopOpeningApp;
        if (activityRecord2 != null && !activityRecord2.mIsExiting) {
            if (status != null) {
                WindowState topFullWin = getTopFullWin(this.mTopOpeningApp);
                VSlog.d(TAG, "notifyBarReparent topFullWin " + topFullWin);
                status.mToken.reparentSurfaceControl(this.mDisplayContent.getPendingTransaction(), this.mTopOpeningApp.getSurfaceControl());
                updateStatusFixedRotationTransform(this.mTopOpeningApp.mFixedRotationTransformState);
                ActivityRecord activityRecord3 = this.mTopOpeningApp;
                this.mRealStatusParent = activityRecord3;
                this.mActivtityResponsibleForStatus = activityRecord3;
                activityRecord3.assignChildLayers();
                VSlog.d(TAG, "notifyBarReparent status reparent to " + this.mTopOpeningApp);
            }
            if (gesture != null) {
                gesture.mToken.reparentSurfaceControl(this.mDisplayContent.getPendingTransaction(), this.mTopOpeningApp.getSurfaceControl());
                updateGestureRotation(this.mTopOpeningApp.mFixedRotationTransformState);
                ActivityRecord activityRecord4 = this.mTopOpeningApp;
                this.mRealGestureParent = activityRecord4;
                activityRecord4.assignChildLayers();
                VSlog.d(TAG, "notifyBarReparent gesture reparent to " + this.mTopOpeningApp);
            }
            this.mActivtityResponsibleForNav = this.mTopOpeningApp;
            if (this.mRealGestureParent != null || this.mRealStatusParent != null) {
                this.mDisplayContent.getDisplayPolicy().updateSystemUiVisibilityLw();
                this.mDisplayContent.setLayoutNeeded();
            }
        }
    }

    private WindowState getTopFullWin(ActivityRecord app) {
        if (app != null) {
            WindowList<WindowState> children = app.mChildren;
            for (int i = children.size() - 1; i >= 0; i--) {
                WindowState child = (WindowState) children.get(i);
                if (this.DEBUG_COLOR) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("getTopFullWin ");
                    sb.append(child);
                    sb.append(" ");
                    sb.append(child.getAttrs().isFullscreen());
                    sb.append(" ");
                    sb.append(!child.mAnimatingExit);
                    VSlog.d(TAG, sb.toString());
                }
                if ((child.getAttrs().isFullscreen() || child.isDimming()) && !child.mAnimatingExit && child.getAttrs().type != 3) {
                    return child;
                }
            }
            return null;
        }
        return null;
    }

    private void updateStatusFixedRotationTransform(WindowToken.FixedRotationTransformState state) {
        WindowState status = getRealBarFromFakeBarType(0);
        if (status != null) {
            status.mToken.mFixedRotationTransformState = null;
            if (state == null) {
                VSlog.d(TAG, "updateStatusFixedRotationTransform cancel");
                status.mToken.cancelFixedRotationTransform();
                return;
            }
            VSlog.d(TAG, "updateStatusFixedRotationTransform rotation = " + state.mDisplayInfo.rotation);
            if (status.mToken.mFixedRotationTransformState == null) {
                this.mTmpSavedFrame.set(status.getFrameLw());
            }
            status.mToken.mFixedRotationRotateSurfaceNeeded = false;
            status.mToken.linkFixedRotationTransform(this.mTopOpeningApp);
        }
    }

    private void updateGestureRotation(WindowToken.FixedRotationTransformState state) {
        int i;
        if (!this.mGestureRotationWatchers.isEmpty()) {
            int rotation = state != null ? state.mDisplayInfo.rotation : this.mDisplayContent.getRotation();
            if (state != null || ((i = this.mGestureLastFixedRotation) != -1 && rotation != i)) {
                try {
                    Iterator<WindowManagerService.RotationWatcher> it = this.mGestureRotationWatchers.iterator();
                    while (it.hasNext()) {
                        WindowManagerService.RotationWatcher watcher = it.next();
                        watcher.mWatcher.onRotationChanged(rotation);
                        VSlog.d(TAG, "updateGestureRotation to " + rotation + " " + watcher);
                    }
                } catch (Exception e) {
                }
            }
            this.mGestureLastFixedRotation = state != null ? rotation : -1;
        }
    }

    private void clearRestRunnable() {
        this.mDisplayContent.mWmService.getHandler().removeCallbacks(this.mResetRunnable);
    }

    private void judgeStateIsResetNeeded() {
        if (this.mTransitionState == 0 && this.mRemoteAnimState == 0) {
            clearRestRunnable();
            if (!this.mWaitingForSideSlideRemote && !this.mIsBackToRecents) {
                removeRemoteAnimations();
                ActivityRecord activityRecord = this.mRealStatusParent;
                if (activityRecord != null) {
                    activityRecord.assignChildLayers();
                }
                ActivityRecord activityRecord2 = this.mRealGestureParent;
                if (activityRecord2 != null) {
                    activityRecord2.assignChildLayers();
                }
            }
            this.mDisplayContent.mWmService.getHandler().postDelayed(this.mResetRunnable, 500L);
        }
    }

    private void removeRemoteAnimations() {
        for (int i = this.mPendingBarsAnimations.size() - 1; i >= 0; i += -1) {
            BarAnimationAdapter adapter = (BarAnimationAdapter) this.mPendingBarsAnimations.get(i);
            if (adapter == null) {
                return;
            }
            VSlog.d(TAG, "removeBarsAnimation for type=" + adapter.getToken().windowType);
            adapter.getLeashFinishedCallback().onAnimationFinished(adapter.getLastAnimationType(), adapter);
            this.mPendingBarsAnimations.remove(adapter);
        }
        this.mPendingBarAnimTargets = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: reset */
    public void lambda$new$0$BarAnimController() {
        synchronized (this.mDisplayContent.mWmService.mGlobalLock) {
            if (this.mTransitionState == 0 && this.mRemoteAnimState == 0) {
                VSlog.d(TAG, "reset");
                removeRemoteAnimations();
                resetRealBars();
                resetFakeBars();
                this.mTopOpeningApp = null;
                this.mTopClosingApp = null;
                this.mBarsFinalShowingIsFake = false;
                this.mActivtityResponsibleForStatus = null;
                this.mActivtityResponsibleForNav = null;
                this.mHasCaptureReady = false;
                this.mDisplayContent.mWmService.mWindowPlacerLocked.performSurfacePlacement();
            }
        }
    }

    private void resetFakeBars() {
        VSlog.d(TAG, "resetFakeBars ");
        resetFakeBarForType(0);
        resetFakeBarForType(1);
    }

    private void resetRealBars() {
        VSlog.d(TAG, "resetRealBars ");
        resetRealBarForType(0);
        resetRealBarForType(1);
    }

    private void resetRealBarForType(int type) {
        VSlog.d(TAG, "resetRealBarForType " + getFakeBarNameFromType(type));
        if (type == 0) {
            WindowState status = getRealBarFromFakeBarType(0);
            if (status != null && this.mRealStatusParent != null) {
                this.mRealStatusParent = null;
                WindowContainer originParent = status.mToken.getParent();
                if (originParent != null) {
                    status.mToken.reparentSurfaceControl(this.mDisplayContent.getPendingTransaction(), originParent.mSurfaceControl);
                    originParent.assignChildLayers();
                }
            }
            this.mRealStatusParent = null;
        } else if (type == 1) {
            WindowState gesture = getRealBarFromFakeBarType(1);
            updateGestureRotation(null);
            if (gesture != null && this.mRealGestureParent != null) {
                this.mRealGestureParent = null;
                WindowContainer originParent2 = gesture.mToken.getParent();
                if (originParent2 != null) {
                    gesture.mToken.reparentSurfaceControl(this.mDisplayContent.getPendingTransaction(), originParent2.mSurfaceControl);
                    originParent2.assignChildLayers();
                }
            }
            this.mRealGestureParent = null;
        }
    }

    private void resetFakeBarForType(int type) {
        VSlog.d(TAG, "resetFakeBarForType " + getFakeBarNameFromType(type));
        if (type == 0) {
            this.mFakeStatus.hide();
            if (this.mFakeStatusParent != null) {
                this.mFakeStatus.reparentBack();
            }
            this.mFakeStatusParent = null;
        } else if (type == 1) {
            this.mFakeGesture.hide();
            if (this.mFakeGestureParent != null) {
                this.mFakeGesture.reparentBack();
            }
            this.mFakeGestureParent = null;
        }
    }

    private void FillFakeBarsWithContent() {
        if (this.mBarsFinalShowingIsFake) {
            this.mHasCaptureReady = true;
            return;
        }
        if (!this.mHandler.hasCallbacks(this.mCaptureRunnable)) {
            this.mHasCaptureReady = false;
            this.mHandler.post(this.mCaptureRunnable);
        }
        VSlog.d(TAG, "FillFakeBarsWithContent");
    }

    public /* synthetic */ void lambda$new$1$BarAnimController() {
        VSlog.d(TAG, "mCaptureRunnable run");
        Trace.traceBegin(64L, "BarAnimController.mCaptureRunnable");
        this.mFakeStatus.capture();
        this.mFakeGesture.capture();
        this.mHasCaptureReady = true;
        Trace.traceEnd(64L);
        synchronized (this.mDisplayContent.mWmService.mGlobalLock) {
            this.mDisplayContent.mWmService.mWindowPlacerLocked.performSurfacePlacement();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getFakeBarTypeFromRealBarType(int type) {
        if (type != 2000) {
            if (type == 2024) {
                return 1;
            }
            return -1;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public WindowState getRealBarFromFakeBarType(int type) {
        if (type == 0) {
            return this.mDisplayContent.getDisplayPolicy().mStatusBar;
        }
        if (type == 1 && !this.mDisplayContent.getDisplayPolicy().getVivoHasNavBar()) {
            return this.mDisplayContent.getDisplayPolicy().getBottomGestureBar();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getFakeBarNameFromType(int type) {
        if (type != 0) {
            if (type == 1) {
                return "FakeGestureBar";
            }
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        return "FakeStatusBar";
    }

    private boolean isStateSuitable() {
        boolean splitMode = this.mDisplayContent.getDefaultTaskDisplayArea().isSplitScreenModeActivated();
        boolean isFreeformPerform = this.mDisplayContent.mWmService.isEnteringFreeForm() || this.mDisplayContent.mWmService.isClosingFreeForm();
        VSlog.d(TAG, "isStateSuitable splitMode = " + splitMode + ", isFreeformPerform = " + isFreeformPerform);
        return (splitMode || isFreeformPerform) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class FakeBarSurfaceControl implements SurfaceAnimator.Animatable {
        boolean mCaptureWithTransient;
        boolean mHasReparent;
        WindowContainer mParentContainer;
        SurfaceControl.Transaction mPendingTransaction;
        SurfaceAnimator mSurfaceAnimator;
        SurfaceControl mSurfaceControl;
        int mType;
        String mTag = "FakeBarSurfaceControl";
        final int STATE_HIDDEN = 0;
        final int STATE_READY = 1;
        final int STATE_SHOWN = 2;
        int mState = 0;
        boolean mInCapture = false;

        public FakeBarSurfaceControl(int type) {
            this.mType = BarAnimController.this.getFakeBarTypeFromRealBarType(type);
            this.mPendingTransaction = (SurfaceControl.Transaction) BarAnimController.this.mDisplayContent.mWmService.mTransactionFactory.get();
            this.mParentContainer = BarAnimController.this.mDisplayContent.mOverlayContainers;
            String name = BarAnimController.this.getFakeBarNameFromType(this.mType);
            this.mTag += "-" + name;
            try {
                SurfaceControl.Builder bc = this.mParentContainer.makeChildSurface((WindowContainer) null).setParent(this.mParentContainer.getSurfaceControl()).setName(BarAnimController.this.getFakeBarNameFromType(this.mType)).setBufferSize(1080, 99).setFormat(-3).setMetadata(2, type).setMetadata(1, BarAnimController.this.mDisplayContent.mWmService.mCurrentUserId);
                this.mSurfaceControl = bc.build();
            } catch (Exception e) {
                VSlog.e(this.mTag, "new FakeBarSurfaceControl error");
                e.printStackTrace();
            }
            this.mSurfaceAnimator = new SurfaceAnimator(this, new SurfaceAnimator.OnAnimationFinishedCallback() { // from class: com.android.server.wm.-$$Lambda$QHEr0oILnQCVMgvUgzGeniu87-Q
                public final void onAnimationFinished(int i, AnimationAdapter animationAdapter) {
                    BarAnimController.FakeBarSurfaceControl.this.onAnimationFinished(i, animationAdapter);
                }
            }, BarAnimController.this.mDisplayContent.mWmService);
            VSlog.e(this.mTag, "create success " + this.mType);
        }

        public void show() {
            if (this.mState == 1) {
                WindowState realWin = BarAnimController.this.getRealBarFromFakeBarType(this.mType);
                if (realWin == null) {
                    return;
                }
                if (this.mCaptureWithTransient && !isInTransientState()) {
                    return;
                }
                getPendingTransaction().show(this.mSurfaceControl);
                VSlog.d(this.mTag, "show");
            }
        }

        private boolean isInTransientState() {
            return this.mType == 0 && BarAnimController.this.mDisplayContent.getInsetsPolicy().isTransient(0);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean capture() {
            WindowState realWin = null;
            this.mInCapture = true;
            try {
                try {
                    synchronized (BarAnimController.this.mDisplayContent.mWmService.mGlobalLock) {
                        WindowState realWin2 = BarAnimController.this.getRealBarFromFakeBarType(this.mType);
                        if (realWin2 != null && realWin2.mHasSurface && realWin2.isVisible()) {
                            if (isInTransientState()) {
                                this.mCaptureWithTransient = true;
                            } else {
                                this.mCaptureWithTransient = false;
                            }
                            int width = realWin2.getFrameLw().width();
                            int height = realWin2.getFrameLw().height();
                            Point realPos = realWin2.getLastSurfacePosition();
                            Rect bounds = new Rect();
                            bounds.set(0, 0, realWin2.getFrameLw().width(), realWin2.getFrameLw().height());
                            SurfaceControl realBarSurface = realWin2.getSurfaceControl();
                            String str = this.mTag;
                            VSlog.d(str, "capture 1 w=" + width + " h=" + height + " posx=" + realPos.x + " posy=" + realPos.y + " bounds=" + bounds);
                            this.mPendingTransaction.setBufferSize(this.mSurfaceControl, width, height);
                            this.mPendingTransaction.setPosition(this.mSurfaceControl, realPos.x, realPos.y);
                            Surface surface = new Surface();
                            surface.copyFrom(this.mSurfaceControl);
                            SurfaceControl.ScreenshotGraphicBuffer buffer = SurfaceControl.captureLayers(realBarSurface, bounds, 1.0f);
                            GraphicBuffer graphicBuffer = buffer.getGraphicBuffer();
                            surface.attachAndQueueBuffer(graphicBuffer);
                            surface.release();
                            this.mPendingTransaction.apply();
                            this.mInCapture = false;
                            synchronized (BarAnimController.this.mDisplayContent.mWmService.mGlobalLock) {
                                if (realWin2 != null) {
                                    realWin2.onExitAnimationDone();
                                }
                            }
                            this.mState = 1;
                            return true;
                        }
                        this.mInCapture = false;
                        synchronized (BarAnimController.this.mDisplayContent.mWmService.mGlobalLock) {
                            if (realWin2 != null) {
                                realWin2.onExitAnimationDone();
                            }
                        }
                        return false;
                    }
                } catch (Exception e) {
                    VSlog.d(this.mTag, "capture error");
                    e.printStackTrace();
                    this.mInCapture = false;
                    synchronized (BarAnimController.this.mDisplayContent.mWmService.mGlobalLock) {
                        if (0 != 0) {
                            realWin.onExitAnimationDone();
                        }
                        return false;
                    }
                }
            } catch (Throwable th) {
                this.mInCapture = false;
                synchronized (BarAnimController.this.mDisplayContent.mWmService.mGlobalLock) {
                    if (0 != 0) {
                        realWin.onExitAnimationDone();
                    }
                    throw th;
                }
            }
        }

        public void hide() {
            if (this.mState == 0) {
                return;
            }
            this.mState = 0;
            getPendingTransaction().hide(this.mSurfaceControl);
            VSlog.d(this.mTag, "hide");
        }

        public void reparentTo(WindowContainer token) {
            if (token == null || this.mState != 1) {
                return;
            }
            this.mParentContainer = token;
            this.mSurfaceAnimator.reparent(getPendingTransaction(), this.mParentContainer.getSurfaceControl());
            this.mHasReparent = true;
            String str = this.mTag;
            VSlog.d(str, "reparentTo " + this.mParentContainer);
        }

        public void reparentBack() {
            if (this.mHasReparent) {
                this.mParentContainer = BarAnimController.this.mDisplayContent.mOverlayContainers;
                this.mSurfaceAnimator.reparent(getPendingTransaction(), this.mParentContainer.getSurfaceControl());
                this.mHasReparent = false;
                VSlog.d(this.mTag, "reparentBack");
            }
        }

        public void setLayer(SurfaceControl.Transaction t, int layer) {
            this.mSurfaceAnimator.setLayer(t, layer);
        }

        public SurfaceControl.Transaction getPendingTransaction() {
            if (BarAnimController.this.mDisplayContent != null) {
                return BarAnimController.this.mDisplayContent.getPendingTransaction();
            }
            return this.mPendingTransaction;
        }

        public SurfaceControl.Builder makeAnimationLeash() {
            return this.mParentContainer.makeChildSurface((WindowContainer) null);
        }

        public void commitPendingTransaction() {
            this.mParentContainer.scheduleAnimation();
        }

        public void onAnimationLeashCreated(SurfaceControl.Transaction t, SurfaceControl leash) {
        }

        public void onAnimationLeashLost(SurfaceControl.Transaction t) {
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public void onAnimationFinished(int type, AnimationAdapter anim) {
            BarAnimController.this.mDisplayContent.mWmService.onAnimationFinished();
        }

        public SurfaceControl getAnimationLeashParent() {
            return getParentSurfaceControl();
        }

        public SurfaceControl getSurfaceControl() {
            return this.mSurfaceControl;
        }

        public SurfaceControl getParentSurfaceControl() {
            return this.mParentContainer.getSurfaceControl();
        }

        public int getSurfaceWidth() {
            return BarAnimController.this.mDisplayContent.getSurfaceWidth();
        }

        public int getSurfaceHeight() {
            return BarAnimController.this.mDisplayContent.getSurfaceHeight();
        }
    }

    /* loaded from: classes.dex */
    public class BarAnimationAdapter implements AnimationAdapter {
        private final String TAG = "BarAnimationAdapter";
        private Consumer<AnimationAdapter> mAnimationCanceledRunnable;
        private SurfaceControl mCapturedLeash;
        private SurfaceAnimator.OnAnimationFinishedCallback mCapturedLeashFinishCallback;
        private long mDurationHint;
        private final boolean mIsForFake;
        private int mLastAnimationType;
        private long mStatusBarTransitionDelay;
        private RemoteAnimationTarget mTarget;
        private final WindowToken mWindowToken;

        BarAnimationAdapter(WindowToken windowToken, long durationHint, long statusBarTransitionDelay, Consumer<AnimationAdapter> animationCanceledRunnable, boolean forFake) {
            this.mWindowToken = windowToken;
            this.mIsForFake = forFake;
            this.mDurationHint = durationHint;
            this.mStatusBarTransitionDelay = statusBarTransitionDelay;
            this.mAnimationCanceledRunnable = animationCanceledRunnable;
        }

        RemoteAnimationTarget createRemoteAnimationTarget(int mode) {
            RemoteAnimationTarget remoteAnimationTarget = new RemoteAnimationTarget(-1, mode, getLeash(), false, (Rect) null, (Rect) null, this.mWindowToken.getPrefixOrderIndex(), new Point(), (Rect) null, (Rect) null, this.mWindowToken.getWindowConfiguration(), true, (SurfaceControl) null, (Rect) null);
            this.mTarget = remoteAnimationTarget;
            return remoteAnimationTarget;
        }

        SurfaceControl getLeash() {
            return this.mCapturedLeash;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public SurfaceAnimator.OnAnimationFinishedCallback getLeashFinishedCallback() {
            return this.mCapturedLeashFinishCallback;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getLastAnimationType() {
            return this.mLastAnimationType;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public WindowToken getToken() {
            return this.mWindowToken;
        }

        public boolean getShowWallpaper() {
            return false;
        }

        public void startAnimation(SurfaceControl animationLeash, SurfaceControl.Transaction t, int type, SurfaceAnimator.OnAnimationFinishedCallback finishCallback) {
            VSlog.d("BarAnimationAdapter", "startAnimation for " + animationLeash);
            t.setLayer(animationLeash, this.mWindowToken.getPrefixOrderIndex());
            this.mCapturedLeash = animationLeash;
            this.mCapturedLeashFinishCallback = finishCallback;
            this.mLastAnimationType = type;
        }

        public void onAnimationCancelled(SurfaceControl animationLeash) {
            VSlog.d("BarAnimationAdapter", "onAnimationCancelled " + animationLeash);
            this.mAnimationCanceledRunnable.accept(this);
        }

        public long getDurationHint() {
            return this.mDurationHint;
        }

        public long getStatusBarTransitionsStartTime() {
            return SystemClock.uptimeMillis() + this.mStatusBarTransitionDelay;
        }

        public void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.print("token=");
            pw.println(this.mWindowToken);
            if (this.mTarget != null) {
                pw.print(prefix);
                pw.println("Target:");
                RemoteAnimationTarget remoteAnimationTarget = this.mTarget;
                remoteAnimationTarget.dump(pw, prefix + "  ");
                return;
            }
            pw.print(prefix);
            pw.println("Target: null");
        }

        public void dumpDebug(ProtoOutputStream proto) {
            long token = proto.start(1146756268034L);
            RemoteAnimationTarget remoteAnimationTarget = this.mTarget;
            if (remoteAnimationTarget != null) {
                remoteAnimationTarget.dumpDebug(proto, 1146756268033L);
            }
            proto.end(token);
        }
    }
}