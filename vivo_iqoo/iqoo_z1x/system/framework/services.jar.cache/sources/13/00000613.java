package com.android.server.accessibility.gestures;

import android.content.Context;
import android.graphics.PointF;
import android.net.INetd;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.server.accessibility.gestures.GestureMatcher;
import java.util.ArrayList;

/* loaded from: classes.dex */
class Swipe extends GestureMatcher {
    private static final float ANGLE_THRESHOLD = 0.0f;
    public static final int DOWN = 3;
    public static final int GESTURE_CONFIRM_CM = 1;
    public static final int LEFT = 0;
    public static final long MAX_TIME_TO_CONTINUE_SWIPE_MS = 350;
    public static final long MAX_TIME_TO_START_SWIPE_MS = 150;
    private static final float MIN_CM_BETWEEN_SAMPLES = 0.25f;
    public static final int RIGHT = 1;
    public static final int UP = 2;
    private long mBaseTime;
    private float mBaseX;
    private float mBaseY;
    private int[] mDirections;
    private final float mGestureDetectionThresholdPixels;
    private final float mMinPixelsBetweenSamplesX;
    private final float mMinPixelsBetweenSamplesY;
    private float mPreviousGestureX;
    private float mPreviousGestureY;
    private final ArrayList<PointF> mStrokeBuffer;
    private int mTouchSlop;

    /* JADX INFO: Access modifiers changed from: package-private */
    public Swipe(Context context, int direction, int gesture, GestureMatcher.StateChangeListener listener) {
        this(context, new int[]{direction}, gesture, listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Swipe(Context context, int direction1, int direction2, int gesture, GestureMatcher.StateChangeListener listener) {
        this(context, new int[]{direction1, direction2}, gesture, listener);
    }

    private Swipe(Context context, int[] directions, int gesture, GestureMatcher.StateChangeListener listener) {
        super(gesture, new Handler(context.getMainLooper()), listener);
        this.mStrokeBuffer = new ArrayList<>(100);
        this.mDirections = directions;
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        this.mGestureDetectionThresholdPixels = TypedValue.applyDimension(5, GestureUtils.MM_PER_CM, displayMetrics) * 1.0f;
        float pixelsPerCmX = displayMetrics.xdpi / 2.54f;
        float pixelsPerCmY = displayMetrics.ydpi / 2.54f;
        this.mMinPixelsBetweenSamplesX = pixelsPerCmX * MIN_CM_BETWEEN_SAMPLES;
        this.mMinPixelsBetweenSamplesY = MIN_CM_BETWEEN_SAMPLES * pixelsPerCmY;
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        clear();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public void clear() {
        this.mBaseX = Float.NaN;
        this.mBaseY = Float.NaN;
        this.mBaseTime = 0L;
        this.mPreviousGestureX = Float.NaN;
        this.mPreviousGestureY = Float.NaN;
        this.mStrokeBuffer.clear();
        super.clear();
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (Float.isNaN(this.mBaseX) && Float.isNaN(this.mBaseY)) {
            this.mBaseX = rawEvent.getX();
            this.mBaseY = rawEvent.getY();
            this.mBaseTime = rawEvent.getEventTime();
            this.mPreviousGestureX = this.mBaseX;
            this.mPreviousGestureY = this.mBaseY;
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onMove(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        float x = rawEvent.getX();
        float y = rawEvent.getY();
        long time = rawEvent.getEventTime();
        float dX = Math.abs(x - this.mPreviousGestureX);
        float dY = Math.abs(y - this.mPreviousGestureY);
        double moveDelta = Math.hypot(Math.abs(x - this.mBaseX), Math.abs(y - this.mBaseY));
        long timeDelta = time - this.mBaseTime;
        if (getState() == 0) {
            if (moveDelta < this.mTouchSlop) {
                return;
            }
            if (this.mStrokeBuffer.size() == 0) {
                int direction = toDirection(x - this.mBaseX, y - this.mBaseY);
                if (direction == this.mDirections[0]) {
                    this.mStrokeBuffer.add(new PointF(this.mBaseX, this.mBaseY));
                } else {
                    cancelGesture(event, rawEvent, policyFlags);
                    return;
                }
            }
        }
        if (moveDelta > this.mGestureDetectionThresholdPixels) {
            this.mBaseX = x;
            this.mBaseY = y;
            this.mBaseTime = time;
            startGesture(event, rawEvent, policyFlags);
        } else if (getState() == 0) {
            if (timeDelta > 150) {
                cancelGesture(event, rawEvent, policyFlags);
                return;
            }
        } else if (getState() == 1 && timeDelta > 350) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        if (dX >= this.mMinPixelsBetweenSamplesX || dY >= this.mMinPixelsBetweenSamplesY) {
            this.mPreviousGestureX = x;
            this.mPreviousGestureY = y;
            this.mStrokeBuffer.add(new PointF(x, y));
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (getState() != 1) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        float x = rawEvent.getX();
        float y = rawEvent.getY();
        float dX = Math.abs(x - this.mPreviousGestureX);
        float dY = Math.abs(y - this.mPreviousGestureY);
        if (dX >= this.mMinPixelsBetweenSamplesX || dY >= this.mMinPixelsBetweenSamplesY) {
            this.mStrokeBuffer.add(new PointF(x, y));
        }
        recognizeGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onPointerDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onPointerUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelGesture(event, rawEvent, policyFlags);
    }

    private void recognizeGesture(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (this.mStrokeBuffer.size() < 2) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        ArrayList<PointF> path = new ArrayList<>();
        PointF lastDelimiter = this.mStrokeBuffer.get(0);
        path.add(lastDelimiter);
        float dX = ANGLE_THRESHOLD;
        float dY = ANGLE_THRESHOLD;
        int count = 0;
        float length = ANGLE_THRESHOLD;
        PointF next = null;
        for (int i = 1; i < this.mStrokeBuffer.size(); i++) {
            PointF next2 = this.mStrokeBuffer.get(i);
            next = next2;
            if (count > 0) {
                float currentDX = dX / count;
                float currentDY = dY / count;
                PointF newDelimiter = new PointF((length * currentDX) + lastDelimiter.x, (length * currentDY) + lastDelimiter.y);
                float nextDX = next.x - newDelimiter.x;
                float nextDY = next.y - newDelimiter.y;
                float nextLength = (float) Math.sqrt((nextDX * nextDX) + (nextDY * nextDY));
                float dot = (currentDX * (nextDX / nextLength)) + (currentDY * (nextDY / nextLength));
                if (dot < ANGLE_THRESHOLD) {
                    path.add(newDelimiter);
                    lastDelimiter = newDelimiter;
                    dX = ANGLE_THRESHOLD;
                    dY = ANGLE_THRESHOLD;
                    count = 0;
                }
            }
            float currentDX2 = next.x - lastDelimiter.x;
            float currentDY2 = next.y - lastDelimiter.y;
            length = (float) Math.sqrt((currentDX2 * currentDX2) + (currentDY2 * currentDY2));
            count++;
            dX += currentDX2 / length;
            dY += currentDY2 / length;
        }
        path.add(next);
        recognizeGesturePath(event, rawEvent, policyFlags, path);
    }

    private void recognizeGesturePath(MotionEvent event, MotionEvent rawEvent, int policyFlags, ArrayList<PointF> path) {
        event.getDisplayId();
        if (path.size() != this.mDirections.length + 1) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        for (int i = 0; i < path.size() - 1; i++) {
            PointF start = path.get(i);
            PointF end = path.get(i + 1);
            float dX = end.x - start.x;
            float dY = end.y - start.y;
            int direction = toDirection(dX, dY);
            if (direction != this.mDirections[i]) {
                cancelGesture(event, rawEvent, policyFlags);
                return;
            }
        }
        completeGesture(event, rawEvent, policyFlags);
    }

    private static int toDirection(float dX, float dY) {
        return Math.abs(dX) > Math.abs(dY) ? dX < ANGLE_THRESHOLD ? 0 : 1 : dY < ANGLE_THRESHOLD ? 2 : 3;
    }

    public static String directionToString(int direction) {
        if (direction != 0) {
            if (direction != 1) {
                if (direction != 2) {
                    if (direction == 3) {
                        return INetd.IF_STATE_DOWN;
                    }
                    return "Unknown Direction";
                }
                return INetd.IF_STATE_UP;
            }
            return "right";
        }
        return "left";
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    String getGestureName() {
        StringBuilder builder = new StringBuilder();
        builder.append("Swipe ");
        builder.append(directionToString(this.mDirections[0]));
        for (int i = 1; i < this.mDirections.length; i++) {
            builder.append(" and ");
            builder.append(directionToString(this.mDirections[i]));
        }
        return builder.toString();
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        if (getState() != 3) {
            builder.append(", mBaseX: ");
            builder.append(this.mBaseX);
            builder.append(", mBaseY: ");
            builder.append(this.mBaseY);
            builder.append(", mGestureDetectionThreshold:");
            builder.append(this.mGestureDetectionThresholdPixels);
            builder.append(", mMinPixelsBetweenSamplesX:");
            builder.append(this.mMinPixelsBetweenSamplesX);
            builder.append(", mMinPixelsBetweenSamplesY:");
            builder.append(this.mMinPixelsBetweenSamplesY);
        }
        return builder.toString();
    }
}