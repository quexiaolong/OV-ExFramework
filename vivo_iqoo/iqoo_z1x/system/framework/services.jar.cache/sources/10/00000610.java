package com.android.server.accessibility.gestures;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.server.accessibility.gestures.GestureMatcher;

/* loaded from: classes.dex */
class MultiTap extends GestureMatcher {
    public static final int MAX_TAPS = 10;
    float mBaseX;
    float mBaseY;
    int mCurrentTaps;
    int mDoubleTapSlop;
    int mDoubleTapTimeout;
    int mTapTimeout;
    final int mTargetTaps;
    int mTouchSlop;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MultiTap(Context context, int taps, int gesture, GestureMatcher.StateChangeListener listener) {
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
        super.clear();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public void onDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelAfterTapTimeout(event, rawEvent, policyFlags);
        if (Float.isNaN(this.mBaseX) && Float.isNaN(this.mBaseY)) {
            this.mBaseX = event.getX();
            this.mBaseY = event.getY();
        }
        if (!isInsideSlop(rawEvent, this.mDoubleTapSlop)) {
            cancelGesture(event, rawEvent, policyFlags);
        }
        this.mBaseX = event.getX();
        this.mBaseY = event.getY();
        if (this.mCurrentTaps + 1 == this.mTargetTaps) {
            startGesture(event, rawEvent, policyFlags);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public void onUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelAfterDoubleTapTimeout(event, rawEvent, policyFlags);
        if (!isInsideSlop(rawEvent, this.mTouchSlop)) {
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
        if (!isInsideSlop(rawEvent, this.mTouchSlop)) {
            cancelGesture(event, rawEvent, policyFlags);
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onPointerDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onPointerUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public String getGestureName() {
        int i = this.mTargetTaps;
        if (i != 2) {
            if (i == 3) {
                return "Triple Tap";
            }
            return Integer.toString(this.mTargetTaps) + " Taps";
        }
        return "Double Tap";
    }

    private boolean isInsideSlop(MotionEvent rawEvent, int slop) {
        float deltaX = this.mBaseX - rawEvent.getX();
        float deltaY = this.mBaseY - rawEvent.getY();
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