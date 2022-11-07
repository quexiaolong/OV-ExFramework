package com.android.server.wm;

import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoWindowAnimatorImpl implements IVivoWindowAnimator {
    static final String TAG = "VivoWindowAnimatorImpl";
    private WindowAnimator mWindowAnimator;

    public VivoWindowAnimatorImpl(WindowAnimator windowAnimator) {
        if (windowAnimator == null) {
            VSlog.i(TAG, "container is " + windowAnimator);
        }
        this.mWindowAnimator = windowAnimator;
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public void removeMultiWindowTransitionLocked(int displayId) {
        if (this.mWindowAnimator.getDisplayContentsAnimatorLocked(displayId) != null && this.mWindowAnimator.getDisplayContentsAnimatorLocked(displayId).mMultiWindowTrans != null) {
            ((VivoMultiWindowTrans) this.mWindowAnimator.getDisplayContentsAnimatorLocked(displayId).mMultiWindowTrans).removeAllSetMultiWindowTransition();
            this.mWindowAnimator.getDisplayContentsAnimatorLocked(displayId).mMultiWindowTrans = null;
            return;
        }
        VSlog.wtf(TAG, "failed to remove multiwindow transition");
    }

    public Object getMultiWindowTransitionLocked(int displayId) {
        if (this.mWindowAnimator.getDisplayContentsAnimatorLocked(displayId) != null) {
            return this.mWindowAnimator.getDisplayContentsAnimatorLocked(displayId).mMultiWindowTrans;
        }
        return null;
    }

    public void setMultiWindowTransitionLocked(int displayId, Object multitrans) {
        if (this.mWindowAnimator.getDisplayContentsAnimatorLocked(displayId) != null) {
            this.mWindowAnimator.getDisplayContentsAnimatorLocked(displayId).mMultiWindowTrans = multitrans;
        } else {
            VSlog.wtf(TAG, "failed to set transition of multiwindow");
        }
    }
}