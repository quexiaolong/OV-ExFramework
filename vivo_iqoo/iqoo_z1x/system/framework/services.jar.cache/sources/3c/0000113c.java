package com.android.server.location.gnss;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ExponentialBackOff {
    private static final int MULTIPLIER = 2;
    private long mCurrentIntervalMillis;
    private final long mInitIntervalMillis;
    private final long mMaxIntervalMillis;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ExponentialBackOff(long initIntervalMillis, long maxIntervalMillis) {
        this.mInitIntervalMillis = initIntervalMillis;
        this.mMaxIntervalMillis = maxIntervalMillis;
        this.mCurrentIntervalMillis = initIntervalMillis / 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long nextBackoffMillis() {
        long j = this.mCurrentIntervalMillis;
        long j2 = this.mMaxIntervalMillis;
        if (j > j2) {
            return j2;
        }
        long j3 = j * 2;
        this.mCurrentIntervalMillis = j3;
        return j3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reset() {
        this.mCurrentIntervalMillis = this.mInitIntervalMillis / 2;
    }
}