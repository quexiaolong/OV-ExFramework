package com.android.server.accessibility.gestures;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class GestureMatcher {
    static final int STATE_CLEAR = 0;
    static final int STATE_GESTURE_CANCELED = 3;
    static final int STATE_GESTURE_COMPLETED = 2;
    static final int STATE_GESTURE_STARTED = 1;
    private final int mGestureId;
    private final Handler mHandler;
    private final StateChangeListener mListener;
    private int mState = 0;
    protected final DelayedTransition mDelayedTransition = new DelayedTransition();

    /* loaded from: classes.dex */
    public @interface State {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface StateChangeListener {
        void onStateChanged(int i, int i2, MotionEvent motionEvent, MotionEvent motionEvent2, int i3);
    }

    abstract String getGestureName();

    /* JADX INFO: Access modifiers changed from: package-private */
    public GestureMatcher(int gestureId, Handler handler, StateChangeListener listener) {
        this.mGestureId = gestureId;
        this.mHandler = handler;
        this.mListener = listener;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void clear() {
        this.mState = 0;
        cancelPendingTransitions();
    }

    public int getState() {
        return this.mState;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setState(int state, MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        this.mState = state;
        cancelPendingTransitions();
        this.mListener.onStateChanged(this.mGestureId, this.mState, event, rawEvent, policyFlags);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void startGesture(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        setState(1, event, rawEvent, policyFlags);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void cancelGesture(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        setState(3, event, rawEvent, policyFlags);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void completeGesture(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        setState(2, event, rawEvent, policyFlags);
    }

    public int getGestureId() {
        return this.mGestureId;
    }

    public final int onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        int i = this.mState;
        if (i == 3 || i == 2) {
            return this.mState;
        }
        int actionMasked = event.getActionMasked();
        if (actionMasked == 0) {
            onDown(event, rawEvent, policyFlags);
        } else if (actionMasked == 1) {
            onUp(event, rawEvent, policyFlags);
        } else if (actionMasked == 2) {
            onMove(event, rawEvent, policyFlags);
        } else if (actionMasked == 5) {
            onPointerDown(event, rawEvent, policyFlags);
        } else if (actionMasked == 6) {
            onPointerUp(event, rawEvent, policyFlags);
        } else {
            setState(3, event, rawEvent, policyFlags);
        }
        return this.mState;
    }

    protected void onDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
    }

    protected void onPointerDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
    }

    protected void onMove(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
    }

    protected void onPointerUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
    }

    protected void onUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void cancelAfterTapTimeout(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelAfter(ViewConfiguration.getTapTimeout(), event, rawEvent, policyFlags);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void cancelAfterDoubleTapTimeout(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelAfter(ViewConfiguration.getDoubleTapTimeout(), event, rawEvent, policyFlags);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void cancelAfter(long timeout, MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        this.mDelayedTransition.cancel();
        this.mDelayedTransition.post(3, timeout, event, rawEvent, policyFlags);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void cancelPendingTransitions() {
        this.mDelayedTransition.cancel();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void completeAfterLongPressTimeout(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        completeAfter(ViewConfiguration.getLongPressTimeout(), event, rawEvent, policyFlags);
    }

    protected final void completeAfterTapTimeout(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        completeAfter(ViewConfiguration.getTapTimeout(), event, rawEvent, policyFlags);
    }

    protected final void completeAfter(long timeout, MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        this.mDelayedTransition.cancel();
        this.mDelayedTransition.post(2, timeout, event, rawEvent, policyFlags);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void completeAfterDoubleTapTimeout(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        completeAfter(ViewConfiguration.getDoubleTapTimeout(), event, rawEvent, policyFlags);
    }

    public static String getStateSymbolicName(int state) {
        if (state != 0) {
            if (state != 1) {
                if (state != 2) {
                    if (state == 3) {
                        return "STATE_GESTURE_CANCELED";
                    }
                    return "Unknown state: " + state;
                }
                return "STATE_GESTURE_COMPLETED";
            }
            return "STATE_GESTURE_STARTED";
        }
        return "STATE_CLEAR";
    }

    public String toString() {
        return getGestureName() + ":" + getStateSymbolicName(this.mState);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public final class DelayedTransition implements Runnable {
        private static final String LOG_TAG = "GestureMatcher.DelayedTransition";
        MotionEvent mEvent;
        int mPolicyFlags;
        MotionEvent mRawEvent;
        int mTargetState;

        protected DelayedTransition() {
        }

        public void cancel() {
            GestureMatcher.this.mHandler.removeCallbacks(this);
        }

        public void post(int state, long delay, MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            this.mTargetState = state;
            this.mEvent = event;
            this.mRawEvent = rawEvent;
            this.mPolicyFlags = policyFlags;
            GestureMatcher.this.mHandler.postDelayed(this, delay);
        }

        public boolean isPending() {
            return GestureMatcher.this.mHandler.hasCallbacks(this);
        }

        public void forceSendAndRemove() {
            if (isPending()) {
                run();
                cancel();
            }
        }

        @Override // java.lang.Runnable
        public void run() {
            GestureMatcher.this.setState(this.mTargetState, this.mEvent, this.mRawEvent, this.mPolicyFlags);
        }
    }
}