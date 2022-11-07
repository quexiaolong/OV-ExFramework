package com.android.server.notification;

/* loaded from: classes.dex */
public class RateEstimator {
    private static final double MINIMUM_DT = 5.0E-4d;
    private static final double RATE_ALPHA = 0.8d;
    private double mInterarrivalTime = 1000.0d;
    private Long mLastEventTime;

    public float update(long now) {
        float rate;
        if (this.mLastEventTime == null) {
            rate = 0.0f;
        } else {
            double interarrivalEstimate = getInterarrivalEstimate(now);
            this.mInterarrivalTime = interarrivalEstimate;
            rate = (float) (1.0d / interarrivalEstimate);
        }
        this.mLastEventTime = Long.valueOf(now);
        return rate;
    }

    public float getRate(long now) {
        if (this.mLastEventTime == null) {
            return 0.0f;
        }
        return (float) (1.0d / getInterarrivalEstimate(now));
    }

    private double getInterarrivalEstimate(long now) {
        double dt = (now - this.mLastEventTime.longValue()) / 1000.0d;
        return (this.mInterarrivalTime * RATE_ALPHA) + (0.19999999999999996d * Math.max(dt, (double) MINIMUM_DT));
    }
}