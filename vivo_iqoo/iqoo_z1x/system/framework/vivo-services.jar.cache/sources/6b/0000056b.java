package com.android.server.wm;

import android.os.Debug;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.SystemProperties;
import java.util.ArrayList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoMultiWindowTransManager {
    static final boolean DEBUG;
    static final boolean DEBUG_PERFORMANCE;
    static final String TAG_TRANSITION = "vivo_debug_multitrans";
    private static VivoMultiWindowTransManager mInstance;
    private WindowManagerService mWindowManagerService = null;

    static {
        boolean z = SystemProperties.getBoolean("persist.vivo.multiwindow_freezetransaction_debug", false);
        DEBUG = z;
        DEBUG_PERFORMANCE = SystemProperties.getBoolean("persist.vivo.multiwindow_freezetransaction_debug_performance", z);
        mInstance = new VivoMultiWindowTransManager();
    }

    public static VivoMultiWindowTransManager getInstance() {
        if (mInstance == null) {
            synchronized (VivoMultiWindowTransManager.class) {
                if (mInstance == null) {
                    mInstance = new VivoMultiWindowTransManager();
                }
            }
        }
        return mInstance;
    }

    private VivoMultiWindowTransManager() {
    }

    public void setWindowManagerService(WindowManagerService service) {
        this.mWindowManagerService = service;
    }

    private void setAppTokenDrawnForMultiWindow(ActivityRecord ar) {
        VivoMultiWindowTrans multiTransit;
        WindowManagerService windowManagerService = this.mWindowManagerService;
        if (windowManagerService != null && ar != null && (multiTransit = (VivoMultiWindowTrans) windowManagerService.mAnimator.getMultiWindowTransitionLocked(this.mWindowManagerService.getDefaultDisplayContentLocked().getDisplayId())) != null && multiTransit.isTransReady()) {
            multiTransit.setAppWinDrawnAndRemoveInWaitingList(ar);
            if (multiTransit.getFinishedIfOneNotified()) {
                this.mWindowManagerService.vivoReSendMultiWindowAnimTimeOut(50L);
            } else if (multiTransit.isAllTargetProcessed()) {
                closeMultiWindowTransLocked(this.mWindowManagerService.getDefaultDisplayContentLocked());
            }
        }
    }

    VivoMultiWindowTrans getMultiWindowTransitionLocked() {
        DisplayContent displaycontent;
        WindowManagerService windowManagerService = this.mWindowManagerService;
        if (windowManagerService == null || (displaycontent = windowManagerService.getDefaultDisplayContentLocked()) == null) {
            return null;
        }
        int displayid = displaycontent.getDisplayId();
        VivoMultiWindowTrans multitransit = (VivoMultiWindowTrans) this.mWindowManagerService.mAnimator.getMultiWindowTransitionLocked(displayid);
        if (multitransit == null) {
            VivoMultiWindowTrans multitransit2 = new VivoMultiWindowTrans(this.mWindowManagerService.mContext, displaycontent, displaycontent.getSession(), this.mWindowManagerService);
            this.mWindowManagerService.mAnimator.setMultiWindowTransitionLocked(displayid, multitransit2);
            return multitransit2;
        }
        return multitransit;
    }

    private void waitExitMultiWindowFreezeAnimationSimpleLocked(ArrayList<IBinder> tokens, int transit) {
        if (this.mWindowManagerService == null) {
            return;
        }
        if (DEBUG) {
            VSlog.d(TAG_TRANSITION, "multiwindow prepareWindowTransitionLocked : transit = " + transit + ", animlist size = , Callers = " + Debug.getCallers(4));
        }
        if (this.mWindowManagerService.getDefaultDisplayContentLocked().getDisplayRotation().getDeferredRotationPauseCount() > 0) {
            VSlog.w(TAG_TRANSITION, "multiwindow prepareWindowTransitionLocked : Other window animation is already running.. skip ");
        } else if (transit != 1 && transit != 3) {
        } else {
            VivoMultiWindowTrans multiTransit = getMultiWindowTransitionLocked();
            multiTransit.setWaitingTokenAndWindowTransition(tokens, transit);
        }
    }

    public void closeMultiWindowTransitionLocked() {
        closeMultiWindowTransLocked(this.mWindowManagerService.getDefaultDisplayContentLocked());
    }

    public void closeMultiWindowTransLocked(DisplayContent displaycontent) {
        if (this.mWindowManagerService == null) {
            return;
        }
        VSlog.i(TAG_TRANSITION, "closeMultiWindowTransLocked");
        VivoMultiWindowTrans multiTransit = (VivoMultiWindowTrans) this.mWindowManagerService.mAnimator.getMultiWindowTransitionLocked(displaycontent.getDisplayId());
        removeAllSetMultiWindowTransition(multiTransit);
        this.mWindowManagerService.vivoRemoveMultiWindowAnimTimeOut();
        if (DEBUG) {
            VSlog.i(TAG_TRANSITION, "closeMultiWindowTransLocked finish time is " + SystemClock.elapsedRealtime() + " from " + Debug.getCallers(3));
        }
    }

    void removeAllSetMultiWindowTransition(VivoMultiWindowTrans multiTransit) {
        if (multiTransit != null) {
            multiTransit.removeAllSetMultiWindowTransition();
        }
    }

    public void notifyAppTokenDrawnForMultiWindow(IBinder obj) {
        WindowManagerService windowManagerService;
        if (DEBUG) {
            VSlog.i(TAG_TRANSITION, "notifyAppTokenDrawnForMultiWindow obj = " + obj);
        }
        if (obj == null || (windowManagerService = this.mWindowManagerService) == null) {
            return;
        }
        synchronized (windowManagerService.mGlobalLock) {
            ActivityRecord ar = this.mWindowManagerService.mRoot.isInAnyStack(obj);
            setAppTokenDrawnForMultiWindow(ar);
        }
    }

    public void multiWindowTransAnimationTimeOut() {
        WindowManagerService windowManagerService = this.mWindowManagerService;
        if (windowManagerService == null) {
            return;
        }
        synchronized (windowManagerService.mGlobalLock) {
            VivoMultiWindowTrans multiTransit = (VivoMultiWindowTrans) this.mWindowManagerService.mAnimator.getMultiWindowTransitionLocked(0);
            if (multiTransit != null && multiTransit.isTransReady()) {
                multiTransit.doTimeout();
            }
            if (DEBUG) {
                VSlog.i(TAG_TRANSITION, "multiWindowTransAnimationTimeOut");
            }
            closeMultiWindowTransLocked(this.mWindowManagerService.getDefaultDisplayContentLocked());
        }
    }

    public void waitExitMultiWindowFreezeAnimation(IBinder token) {
        Object obj;
        WindowManagerService windowManagerService = this.mWindowManagerService;
        if (windowManagerService == null) {
            return;
        }
        VivoMultiWindowTrans multitransit = (VivoMultiWindowTrans) windowManagerService.mAnimator.getMultiWindowTransitionLocked(this.mWindowManagerService.getDefaultDisplayContentLocked().getDisplayId());
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("prepareExitSplitAnimation multitransit = ");
            sb.append(multitransit);
            sb.append(", multitransit.isTransReady() = ");
            if (multitransit != null) {
                obj = Boolean.valueOf(multitransit.isTransReady());
            } else {
                obj = "null";
            }
            sb.append(obj);
            VSlog.i(TAG_TRANSITION, sb.toString());
        }
        if (multitransit == null || multitransit.isTransReady()) {
            return;
        }
        ArrayList layoutanimlist = new ArrayList();
        layoutanimlist.add(token);
        waitExitMultiWindowFreezeAnimationSimpleLocked(layoutanimlist, 1);
        multitransit.setFinishedImmediatelyOneNotified(false);
    }

    public void prepareNormalTransFreezeWindowFrame() {
        int timeOut = VivoMultiWindowTrans.getTimeOutByTimeInd(2);
        prepareWindowTransitionFreezeWindowFrameDirect(timeOut);
    }

    public void prepareShortTransFreezeWindowFrame() {
        int timeOut = VivoMultiWindowTrans.getTimeOutByTimeInd(1);
        prepareWindowTransitionFreezeWindowFrameDirect(timeOut);
    }

    public void prepareLongTransFreezeWindowFrame() {
        int timeOut = VivoMultiWindowTrans.getTimeOutByTimeInd(3);
        prepareWindowTransitionFreezeWindowFrameDirect(timeOut);
    }

    public void prepareSuperShortTransFreezeWindowFrame() {
        int timeOut = VivoMultiWindowTrans.getTimeOutByTimeInd(4);
        prepareWindowTransitionFreezeWindowFrameDirect(timeOut);
    }

    public void prepareSpecifyTimeTransFreezeWindowFrame(int time) {
        prepareWindowTransitionFreezeWindowFrameDirect(time);
    }

    public void prepareWindowTransitionFreezeWindowFrameDirect(int timeOut) {
        WindowManagerService windowManagerService = this.mWindowManagerService;
        if (windowManagerService == null || windowManagerService.getDefaultDisplayContentLocked().getDisplayRotation().getDeferredRotationPauseCount() > 0) {
            return;
        }
        synchronized (this.mWindowManagerService.mGlobalLock) {
            VivoMultiWindowTrans transit = getMultiWindowTransitionLocked();
            if (transit == null) {
                return;
            }
            if (DEBUG) {
                VSlog.i(TAG_TRANSITION, "prepareWindowTransitionFreezeWindowFrameDirect");
            }
            transit.createMultiWindowFreezeWindowFrame(timeOut);
        }
    }

    public boolean hasSetAnimation() {
        VivoMultiWindowTrans multitransit = getMultiWindowTransitionLocked();
        return multitransit != null && multitransit.isTransReady();
    }
}