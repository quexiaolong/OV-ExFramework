package com.android.server.accessibility.gestures;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.server.accessibility.gestures.GestureMatcher;

/* loaded from: classes.dex */
class SecondFingerMultiTap extends GestureMatcher {
    float mBaseX;
    float mBaseY;
    int mCurrentTaps;
    int mDoubleTapSlop;
    int mDoubleTapTimeout;
    int mSecondFingerPointerId;
    int mTapTimeout;
    final int mTargetTaps;
    int mTouchSlop;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SecondFingerMultiTap(Context context, int taps, int gesture, GestureMatcher.StateChangeListener listener) {
        super(gesture, new Handler(context.getMainLooper()), listener);
        this.mTargetTaps = taps;
        this.mDoubleTapSlop = ViewConfiguration.get(context).getScaledDoubleTapSlop();
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mTapTimeout = ViewConfiguration.getTapTimeout();
        this.mDoubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();
        clear();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public void clear() {
        this.mCurrentTaps = 0;
        this.mBaseX = Float.NaN;
        this.mBaseY = Float.NaN;
        this.mSecondFingerPointerId = -1;
        super.clear();
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onPointerDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (event.getPointerCount() > 2) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        int index = GestureUtils.getActionIndex(event);
        this.mSecondFingerPointerId = event.getPointerId(index);
        cancelAfterTapTimeout(event, rawEvent, policyFlags);
        if (Float.isNaN(this.mBaseX) && Float.isNaN(this.mBaseY)) {
            this.mBaseX = event.getX();
            this.mBaseY = event.getY();
        }
        if (!isSecondFingerInsideSlop(rawEvent, this.mDoubleTapSlop)) {
            cancelGesture(event, rawEvent, policyFlags);
        }
        this.mBaseX = event.getX();
        this.mBaseY = event.getY();
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onPointerUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (event.getPointerCount() > 2) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        cancelAfterDoubleTapTimeout(event, rawEvent, policyFlags);
        if (!isSecondFingerInsideSlop(rawEvent, this.mTouchSlop)) {
            cancelGesture(event, rawEvent, policyFlags);
        }
        if (getState() == 1 || getState() == 0) {
            int i = this.mCurrentTaps + 1;
            this.mCurrentTaps = i;
            if (i == this.mTargetTaps) {
                completeGesture(event, rawEvent, policyFlags);
                return;
            } else {
                cancelAfterDoubleTapTimeout(event, rawEvent, policyFlags);
                return;
            }
        }
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onMove(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        int pointerCount = event.getPointerCount();
        if (pointerCount != 1) {
            if (pointerCount == 2) {
                if (!isSecondFingerInsideSlop(rawEvent, this.mTouchSlop)) {
                    cancelGesture(event, rawEvent, policyFlags);
                    return;
                }
                return;
            }
            cancelGesture(event, rawEvent, policyFlags);
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public String getGestureName() {
        int i = this.mTargetTaps;
        if (i != 2) {
            if (i == 3) {
                return "Second Finger Triple Tap";
            }
            return "Second Finger " + Integer.toString(this.mTargetTaps) + " Taps";
        }
        return "Second Finger Double Tap";
    }

    private boolean isSecondFingerInsideSlop(MotionEvent rawEvent, int slop) {
        int pointerIndex = rawEvent.findPointerIndex(this.mSecondFingerPointerId);
        if (pointerIndex == -1) {
            return false;
        }
        float deltaX = this.mBaseX - rawEvent.getX(pointerIndex);
        float deltaY = this.mBaseY - rawEvent.getY(pointerIndex);
        if (deltaX == 0.0f && deltaY == 0.0f) {
            return true;
        }
        double moveDelta = Math.hypot(deltaX, deltaY);
        return moveDelta <= ((double) slop);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public String toString() {
        return super.toString() + ", Taps:" + this.mCurrentTaps + ", mBaseX: " + Float.toString(this.mBaseX) + ", mBaseY: " + Float.toString(this.mBaseY);
    }
}