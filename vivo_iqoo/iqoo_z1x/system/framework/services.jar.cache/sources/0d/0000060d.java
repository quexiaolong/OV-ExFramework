package com.android.server.accessibility.gestures;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.internal.util.Preconditions;
import com.android.server.accessibility.gestures.GestureMatcher;
import java.util.ArrayList;
import java.util.Arrays;

/* loaded from: classes.dex */
class MultiFingerMultiTap extends GestureMatcher {
    private PointF[] mBases;
    protected int mCompletedTapCount;
    private int mDoubleTapSlop;
    private ArrayList<PointF> mExcludedPointsForDownSlopChecked;
    protected boolean mIsTargetFingerCountReached;
    final int mTargetFingerCount;
    final int mTargetTapCount;
    private int mTouchSlop;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MultiFingerMultiTap(Context context, int fingers, int taps, int gestureId, GestureMatcher.StateChangeListener listener) {
        super(gestureId, new Handler(context.getMainLooper()), listener);
        this.mIsTargetFingerCountReached = false;
        Preconditions.checkArgument(fingers >= 2);
        Preconditions.checkArgumentPositive(taps, "Tap count must greater than 0.");
        this.mTargetTapCount = taps;
        this.mTargetFingerCount = fingers;
        this.mDoubleTapSlop = ViewConfiguration.get(context).getScaledDoubleTapSlop() * fingers;
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop() * fingers;
        this.mBases = new PointF[this.mTargetFingerCount];
        int i = 0;
        while (true) {
            PointF[] pointFArr = this.mBases;
            if (i < pointFArr.length) {
                pointFArr[i] = new PointF();
                i++;
            } else {
                this.mExcludedPointsForDownSlopChecked = new ArrayList<>(this.mTargetFingerCount);
                clear();
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public void clear() {
        this.mCompletedTapCount = 0;
        this.mIsTargetFingerCountReached = false;
        int i = 0;
        while (true) {
            PointF[] pointFArr = this.mBases;
            if (i < pointFArr.length) {
                pointFArr[i].set(Float.NaN, Float.NaN);
                i++;
            } else {
                this.mExcludedPointsForDownSlopChecked.clear();
                super.clear();
                return;
            }
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (this.mCompletedTapCount == this.mTargetTapCount) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        cancelAfterTapTimeout(event, rawEvent, policyFlags);
        if (this.mCompletedTapCount == 0) {
            initBaseLocation(rawEvent);
            return;
        }
        PointF nearest = findNearestPoint(rawEvent, this.mDoubleTapSlop, true);
        if (nearest != null) {
            int index = event.getActionIndex();
            nearest.set(event.getX(index), event.getY(index));
            return;
        }
        cancelGesture(event, rawEvent, policyFlags);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public void onUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelAfterDoubleTapTimeout(event, rawEvent, policyFlags);
        PointF nearest = findNearestPoint(rawEvent, this.mTouchSlop, false);
        if ((getState() == 1 || getState() == 0) && nearest != null) {
            if (this.mIsTargetFingerCountReached) {
                this.mCompletedTapCount++;
                this.mIsTargetFingerCountReached = false;
                this.mExcludedPointsForDownSlopChecked.clear();
            }
            if (this.mCompletedTapCount == 1) {
                startGesture(event, rawEvent, policyFlags);
            }
            if (this.mCompletedTapCount == this.mTargetTapCount) {
                completeAfterDoubleTapTimeout(event, rawEvent, policyFlags);
                return;
            }
            return;
        }
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onMove(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (findNearestPoint(rawEvent, this.mTouchSlop, false) == null) {
            cancelGesture(event, rawEvent, policyFlags);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public void onPointerDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        PointF nearest;
        cancelAfterTapTimeout(event, rawEvent, policyFlags);
        int currentFingerCount = event.getPointerCount();
        if (currentFingerCount > this.mTargetFingerCount || this.mIsTargetFingerCountReached) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        if (this.mCompletedTapCount == 0) {
            nearest = initBaseLocation(rawEvent);
        } else {
            nearest = findNearestPoint(rawEvent, this.mDoubleTapSlop, true);
        }
        if ((getState() == 1 || getState() == 0) && nearest != null) {
            if (currentFingerCount == this.mTargetFingerCount) {
                this.mIsTargetFingerCountReached = true;
            }
            int index = event.getActionIndex();
            nearest.set(event.getX(index), event.getY(index));
            return;
        }
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onPointerUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (!this.mIsTargetFingerCountReached) {
            cancelGesture(event, rawEvent, policyFlags);
        } else if (getState() == 1 || getState() == 0) {
            cancelAfterTapTimeout(event, rawEvent, policyFlags);
        } else {
            cancelGesture(event, rawEvent, policyFlags);
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public String getGestureName() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.mTargetFingerCount);
        builder.append("-Finger ");
        int i = this.mTargetTapCount;
        if (i == 1) {
            builder.append("Single");
        } else if (i == 2) {
            builder.append("Double");
        } else if (i == 3) {
            builder.append("Triple");
        } else if (i > 3) {
            builder.append(i);
        }
        builder.append(" Tap");
        return builder.toString();
    }

    private PointF initBaseLocation(MotionEvent event) {
        int index = event.getActionIndex();
        int baseIndex = event.getPointerCount() - 1;
        PointF p = this.mBases[baseIndex];
        if (Float.isNaN(p.x) && Float.isNaN(p.y)) {
            p.set(event.getX(index), event.getY(index));
        }
        return p;
    }

    private PointF findNearestPoint(MotionEvent event, float slop, boolean filterMatched) {
        float moveDelta = Float.MAX_VALUE;
        PointF nearest = null;
        int i = 0;
        while (true) {
            PointF[] pointFArr = this.mBases;
            if (i < pointFArr.length) {
                PointF p = pointFArr[i];
                if ((!Float.isNaN(p.x) || !Float.isNaN(p.y)) && (!filterMatched || !this.mExcludedPointsForDownSlopChecked.contains(p))) {
                    int index = event.getActionIndex();
                    float dX = p.x - event.getX(index);
                    float dY = p.y - event.getY(index);
                    if (dX == 0.0f && dY == 0.0f) {
                        if (filterMatched) {
                            this.mExcludedPointsForDownSlopChecked.add(p);
                        }
                        return p;
                    }
                    float delta = (float) Math.hypot(dX, dY);
                    if (moveDelta > delta) {
                        moveDelta = delta;
                        nearest = p;
                    }
                }
                i++;
            } else {
                int i2 = (moveDelta > slop ? 1 : (moveDelta == slop ? 0 : -1));
                if (i2 < 0) {
                    if (filterMatched) {
                        this.mExcludedPointsForDownSlopChecked.add(nearest);
                    }
                    return nearest;
                }
                return null;
            }
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        if (getState() != 3) {
            builder.append(", CompletedTapCount: ");
            builder.append(this.mCompletedTapCount);
            builder.append(", IsTargetFingerCountReached: ");
            builder.append(this.mIsTargetFingerCountReached);
            builder.append(", Bases: ");
            builder.append(Arrays.toString(this.mBases));
            builder.append(", ExcludedPointsForDownSlopChecked: ");
            builder.append(this.mExcludedPointsForDownSlopChecked.toString());
        }
        return builder.toString();
    }
}