package com.android.server.wm;

import android.graphics.Rect;
import android.os.Debug;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.IVivoProposedRotationChangeListener;
import com.android.server.LocalServices;
import com.android.server.inputmethod.InputMethodManagerInternal;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoDockedStackDividerControllerImpl implements IVivoDockedStackDividerController {
    static final String TAG = "VivoDockedStackDividerControllerImpl";
    private final DisplayContent mDisplayContent;
    private DockedStackDividerController mDockedStackDividerController;
    private boolean mMinimizedDock;
    private long mResizingStartTime;
    private final WindowManagerService mService;
    private final RemoteCallbackList<IVivoProposedRotationChangeListener> mVivoProposedRotationChangeListeners = new RemoteCallbackList<>();
    private boolean mIsDeferRotationMinimizeChange = false;
    private boolean mIsMinimizeChangingState = false;
    final Runnable mUpdateDockBackground = new Runnable() { // from class: com.android.server.wm.VivoDockedStackDividerControllerImpl.1
        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoDockedStackDividerControllerImpl.this.mService.mGlobalLock) {
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.i(VivoDockedStackDividerControllerImpl.TAG, "dockedstackdivider mUpdateDockBackground updateDockBackground false");
                }
                VivoDockedStackDividerControllerImpl.this.mDisplayContent.updateDockBackground(false);
            }
        }
    };
    private ActivityStack mTempLastPrimaryStack = null;
    private final Rect mTmpRect = new Rect();
    private final Rect mTmpRect2 = new Rect();

    public VivoDockedStackDividerControllerImpl(DockedStackDividerController dockedStackDividerController, WindowManagerService service, DisplayContent displayContent) {
        if (dockedStackDividerController == null) {
            VSlog.i(TAG, "container is " + dockedStackDividerController);
        }
        this.mService = service;
        this.mDisplayContent = displayContent;
        this.mDockedStackDividerController = dockedStackDividerController;
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public void updateDeferRotationMinimizeChange(boolean isDefer) {
        if (this.mService.isVivoMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_DEFER_ROTATE) {
            this.mIsDeferRotationMinimizeChange = isDefer;
            VSlog.i(TAG, "vivo_multiwindow update deferrotation " + isDefer);
        }
    }

    public void updateMinimizeChangingState(boolean state) {
        if (this.mService.isVivoMultiWindowSupport()) {
            this.mIsMinimizeChangingState = state;
            VSlog.i(TAG, "vivo_multiwindow update MinimizeChanging State " + state);
        }
    }

    public boolean isVivoMinimizeChangingState() {
        return this.mIsMinimizeChangingState;
    }

    public boolean isDeferRotationMinimizeChange() {
        return this.mIsDeferRotationMinimizeChange && VivoMultiWindowConfig.IS_VIVO_DEFER_ROTATE;
    }

    public void setDeferMinimizeRotation(boolean isDefer) {
        if (this.mService.isVivoMultiWindowSupport()) {
            updateDeferRotationMinimizeChange(isDefer);
            updateMinimizeChangingState(isDefer);
        }
    }

    public void updateDockBackground(boolean resizing) {
        if (this.mService.mAtmService.isMultiWindowSupport() && this.mDisplayContent.getDisplayId() == 0) {
            if (resizing) {
                this.mResizingStartTime = SystemClock.elapsedRealtime();
                if (this.mService.mH.hasCallbacks(this.mUpdateDockBackground)) {
                    this.mService.mH.removeCallbacks(this.mUpdateDockBackground);
                }
                this.mDisplayContent.updateDockBackground(resizing);
            } else if (SystemClock.elapsedRealtime() - this.mResizingStartTime < 600) {
                this.mService.mH.postDelayed(this.mUpdateDockBackground, 200L);
            } else {
                this.mDisplayContent.updateDockBackground(resizing);
            }
        }
    }

    public void notifyVivoProposedRotationChange(int rotation, boolean isValid) {
        if (this.mService.isVivoMultiWindowSupport()) {
            int size = this.mVivoProposedRotationChangeListeners.beginBroadcast();
            for (int i = 0; i < size; i++) {
                IVivoProposedRotationChangeListener listener = this.mVivoProposedRotationChangeListeners.getBroadcastItem(i);
                try {
                    listener.onProposedRotationChange(rotation, isValid);
                } catch (RemoteException e) {
                    VSlog.e(TAG, "Error delivering divider visibility changed event.", e);
                }
            }
            this.mVivoProposedRotationChangeListeners.finishBroadcast();
        }
    }

    public void registerVivoProposedRotationChangeListener(IVivoProposedRotationChangeListener listener) {
        this.mVivoProposedRotationChangeListeners.register(listener);
    }

    public void unregisterVivoProposedRotationChangeListener(IVivoProposedRotationChangeListener listener) {
        this.mVivoProposedRotationChangeListeners.unregister(listener);
    }

    public void setTempLastPrimaryStack(ActivityStack stack) {
        this.mTempLastPrimaryStack = stack;
    }

    public boolean ifNeedForceMinimizedAmount(ActivityStack primaryStack, boolean minimized) {
        ActivityStack activityStack;
        if (VivoMultiWindowConfig.DEBUG && this.mTempLastPrimaryStack != null) {
            VSlog.i(TAG, "setMinimizedDockedStack stack is " + primaryStack + " minimized " + minimized + " tempLastPrimaryStack info " + this.mTempLastPrimaryStack + " " + this.mDisplayContent.isInDisplay(this.mTempLastPrimaryStack) + " " + this.mTempLastPrimaryStack.getParent() + " " + Debug.getCallers(5));
        }
        if (VivoMultiWindowConfig.IS_VIVO_SPLIT_THIRD_LAUNCHER && this.mService.isVivoMultiWindowSupport() && !minimized && primaryStack == null && (activityStack = this.mTempLastPrimaryStack) != null && activityStack.getParent() != null && this.mDisplayContent.isInDisplay(this.mTempLastPrimaryStack)) {
            return true;
        }
        return false;
    }

    public boolean forceSetMinimizedAmountIfNeeded(ActivityStack primaryStack, boolean minimized) {
        return false;
    }

    public boolean ignoreTopSecondaryStackZorder(ActivityStack stack) {
        if (stack.getTopChild() != null && !stack.getTopChild().isVisible() && !stack.getTopChild().toString().contains(VivoMultiWindowConfig.SMART_MULTIWINDOW_NAME)) {
            VSlog.i(TAG, "ignoreTopSecondaryStackZorder because " + stack.getTopChild() + " is not visible");
            return true;
        }
        return false;
    }

    public boolean isMinimizedDock() {
        return this.mMinimizedDock;
    }

    private boolean isValidStateIfNeeded() {
        DisplayContent displayContent;
        TaskDisplayArea area;
        return this.mService.mAtmService.isMultiWindowSupport() && (displayContent = this.mDisplayContent) != null && displayContent.mDisplayId == 0 && (area = this.mDisplayContent.getDefaultTaskDisplayArea()) != null && area.isSplitScreenModeActivated();
    }

    private boolean isWithinDisplay(Task task) {
        task.getBounds(this.mTmpRect);
        this.mDisplayContent.getBounds(this.mTmpRect2);
        VSlog.i(TAG, " isWithinDisplay to task of " + task + ",mTmpRect is " + this.mTmpRect + ",mTmpRect2 is " + this.mTmpRect2 + ",result is " + this.mTmpRect.intersect(this.mTmpRect2));
        return this.mTmpRect.intersect(this.mTmpRect2);
    }

    private boolean isHomeStackResizable() {
        TaskDisplayArea area = this.mDisplayContent.getDefaultTaskDisplayArea();
        ActivityStack rootHomeTask = area != null ? area.getRootHomeTask() : null;
        return (rootHomeTask == null || rootHomeTask == null || !rootHomeTask.isResizeable() || rootHomeTask.preserveOrientationOnResize()) ? false : true;
    }

    public void checkMinimizedLog(String func, String reason, int debugNum) {
        String str = func + " " + reason + " ";
        if (debugNum >= 1) {
            str = str + " " + Debug.getCallers(debugNum);
        }
        VSlog.i(TAG, str);
    }

    public void checkMinimizeChanged(boolean animate) {
        Task top;
        if (isValidStateIfNeeded()) {
            TaskDisplayArea area = this.mDisplayContent.getDefaultTaskDisplayArea();
            ActivityStack rootHomeTask = area.getRootHomeTask();
            if (rootHomeTask == null) {
                checkMinimizedLog("checkMinimizeChanged", "homeTask is null", 0);
            } else if (!isWithinDisplay(rootHomeTask) || rootHomeTask.isRootTask()) {
                checkMinimizedLog("checkMinimizeChanged", "homeTask is abnormal ro is roottask", 0);
            } else if (this.mMinimizedDock && this.mService.mKeyguardOrAodShowingOnDefaultDisplay && !this.mService.mKeyguardGoingAway) {
                checkMinimizedLog("checkMinimizeChanged", "minimize status", 0);
            } else {
                Task topSecondaryStack = area.getRootSecondaryTask();
                boolean homeVisible = rootHomeTask.getTopVisibleAppMainWindow() != null;
                if (homeVisible && topSecondaryStack != null && !ignoreTopSecondaryStackZorder((ActivityStack) topSecondaryStack) && (top = topSecondaryStack.getTopChild()) != null && !top.isActivityTypeHome()) {
                    homeVisible = rootHomeTask.compareTo(top) >= 0;
                }
                setMinimizedDockedStack(homeVisible, animate);
            }
        }
    }

    private void setMinimizedDockedStack(boolean minimizedDock, boolean animate) {
        boolean wasMinimized = this.mMinimizedDock;
        this.mMinimizedDock = minimizedDock;
        if (minimizedDock == wasMinimized) {
            return;
        }
        boolean minimizedChange = false;
        if (isHomeStackResizable()) {
            notifyDockedStackMinimizedChanged(minimizedDock, animate, true);
            minimizedChange = true;
        }
        if (minimizedChange) {
            VSlog.i(TAG, "vivo_multiwindow setMinimizedDockedStack from " + wasMinimized + " to " + this.mMinimizedDock + ",caller from " + Debug.getCallers(10));
        }
    }

    private void notifyDockedStackMinimizedChanged(boolean minimizedDock, boolean animate, boolean isHomeStackResizable) {
    }

    public void notifyDockedStackExistsChanged(boolean exists) {
        if (exists) {
            InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
            if (inputMethodManagerInternal != null) {
                inputMethodManagerInternal.hideCurrentInputMethod(17);
            }
            this.mService.mH.postDelayed(new Runnable() { // from class: com.android.server.wm.VivoDockedStackDividerControllerImpl.2
                @Override // java.lang.Runnable
                public void run() {
                    synchronized (VivoDockedStackDividerControllerImpl.this.mService.mGlobalLock) {
                        VivoDockedStackDividerControllerImpl.this.checkMinimizeChanged(false);
                    }
                }
            }, 500L);
            return;
        }
        setMinimizedDockedStack(false, false);
    }

    public void notifyAppVisibilityChanged() {
        if (delayCheckMinimizeWithAnimForVos()) {
            this.mService.mH.postDelayed(new Runnable() { // from class: com.android.server.wm.VivoDockedStackDividerControllerImpl.3
                @Override // java.lang.Runnable
                public void run() {
                    synchronized (VivoDockedStackDividerControllerImpl.this.mService.mGlobalLock) {
                        VivoDockedStackDividerControllerImpl.this.checkMinimizeChanged(true);
                    }
                }
            }, 250L);
        } else {
            checkMinimizeChanged(false);
        }
    }

    private boolean delayCheckMinimizeWithAnimForVos() {
        if (this.mService.mAtmService.isVivoVosMultiWindowSupport() && !this.mService.mAtmService.isSplittingScreenByVivo()) {
            VSlog.d(TAG, "delay checkMinimize for drawing launcher and divider anim in Vos!");
            return true;
        }
        return false;
    }
}