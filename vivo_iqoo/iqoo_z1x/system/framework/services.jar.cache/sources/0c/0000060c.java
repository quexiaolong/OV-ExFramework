package com.android.server.accessibility.gestures;

import android.graphics.PointF;
import android.util.MathUtils;
import android.view.MotionEvent;
import com.android.server.job.JobPackageTracker;

/* loaded from: classes.dex */
public final class GestureUtils {
    public static int MM_PER_CM = 10;
    public static float CM_PER_INCH = 2.54f;

    private GestureUtils() {
    }

    public static boolean isMultiTap(MotionEvent firstUp, MotionEvent secondUp, int multiTapTimeSlop, int multiTapDistanceSlop) {
        if (firstUp == null || secondUp == null) {
            return false;
        }
        return eventsWithinTimeAndDistanceSlop(firstUp, secondUp, multiTapTimeSlop, multiTapDistanceSlop);
    }

    private static boolean eventsWithinTimeAndDistanceSlop(MotionEvent first, MotionEvent second, int timeout, int distance) {
        if (isTimedOut(first, second, timeout)) {
            return false;
        }
        double deltaMove = distance(first, second);
        return deltaMove < ((double) distance);
    }

    public static double distance(MotionEvent first, MotionEvent second) {
        return MathUtils.dist(first.getX(), first.getY(), second.getX(), second.getY());
    }

    public static double distanceClosestPointerToPoint(PointF pointerDown, MotionEvent moveEvent) {
        float movement = Float.MAX_VALUE;
        for (int i = 0; i < moveEvent.getPointerCount(); i++) {
            float moveDelta = MathUtils.dist(pointerDown.x, pointerDown.y, moveEvent.getX(i), moveEvent.getY(i));
            if (movement > moveDelta) {
                movement = moveDelta;
            }
        }
        return movement;
    }

    public static boolean isTimedOut(MotionEvent firstUp, MotionEvent secondUp, int timeout) {
        long deltaTime = secondUp.getEventTime() - firstUp.getEventTime();
        return deltaTime >= ((long) timeout);
    }

    public static boolean isDraggingGesture(float firstPtrDownX, float firstPtrDownY, float secondPtrDownX, float secondPtrDownY, float firstPtrX, float firstPtrY, float secondPtrX, float secondPtrY, float maxDraggingAngleCos) {
        float firstDeltaX = firstPtrX - firstPtrDownX;
        float firstDeltaY = firstPtrY - firstPtrDownY;
        if (firstDeltaX == 0.0f && firstDeltaY == 0.0f) {
            return true;
        }
        float firstMagnitude = (float) Math.hypot(firstDeltaX, firstDeltaY);
        float firstXNormalized = firstMagnitude > 0.0f ? firstDeltaX / firstMagnitude : firstDeltaX;
        float firstYNormalized = firstMagnitude > 0.0f ? firstDeltaY / firstMagnitude : firstDeltaY;
        float secondDeltaX = secondPtrX - secondPtrDownX;
        float secondDeltaY = secondPtrY - secondPtrDownY;
        if (secondDeltaX == 0.0f && secondDeltaY == 0.0f) {
            return true;
        }
        float secondMagnitude = (float) Math.hypot(secondDeltaX, secondDeltaY);
        float secondXNormalized = secondMagnitude > 0.0f ? secondDeltaX / secondMagnitude : secondDeltaX;
        float secondYNormalized = secondMagnitude > 0.0f ? secondDeltaY / secondMagnitude : secondDeltaY;
        float angleCos = (firstXNormalized * secondXNormalized) + (firstYNormalized * secondYNormalized);
        return angleCos >= maxDraggingAngleCos;
    }

    public static int getActionIndex(MotionEvent event) {
        return (event.getAction() & JobPackageTracker.EVENT_STOP_REASON_MASK) >> 8;
    }
}