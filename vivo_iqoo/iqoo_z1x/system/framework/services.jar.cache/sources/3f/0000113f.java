package com.android.server.location.gnss;

import android.util.Log;

/* loaded from: classes.dex */
public class GnssBatchingProvider {
    private boolean mEnabled;
    private final GnssBatchingProviderNative mNative;
    private long mPeriodNanos;
    private boolean mStarted;
    private boolean mWakeOnFifoFull;
    private static final String TAG = "GnssBatchingProvider";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_cleanup_batching();

    /* JADX INFO: Access modifiers changed from: private */
    public static native void native_flush_batch();

    /* JADX INFO: Access modifiers changed from: private */
    public static native int native_get_batch_size();

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_init_batching();

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_start_batch(long j, boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_stop_batch();

    /* JADX INFO: Access modifiers changed from: package-private */
    public GnssBatchingProvider() {
        this(new GnssBatchingProviderNative());
    }

    GnssBatchingProvider(GnssBatchingProviderNative gnssBatchingProviderNative) {
        this.mNative = gnssBatchingProviderNative;
    }

    public int getBatchSize() {
        return this.mNative.getBatchSize();
    }

    public void enable() {
        boolean initBatching = this.mNative.initBatching();
        this.mEnabled = initBatching;
        if (!initBatching) {
            Log.e(TAG, "Failed to initialize GNSS batching");
        }
    }

    public boolean start(long periodNanos, boolean wakeOnFifoFull) {
        if (!this.mEnabled) {
            throw new IllegalStateException();
        }
        if (periodNanos <= 0) {
            Log.e(TAG, "Invalid periodNanos " + periodNanos + " in batching request, not started");
            return false;
        }
        boolean startBatch = this.mNative.startBatch(periodNanos, wakeOnFifoFull);
        this.mStarted = startBatch;
        if (startBatch) {
            this.mPeriodNanos = periodNanos;
            this.mWakeOnFifoFull = wakeOnFifoFull;
        }
        return this.mStarted;
    }

    public void flush() {
        if (!this.mStarted) {
            Log.w(TAG, "Cannot flush since GNSS batching has not started.");
        } else {
            this.mNative.flushBatch();
        }
    }

    public boolean stop() {
        boolean stopped = this.mNative.stopBatch();
        if (stopped) {
            this.mStarted = false;
        }
        return stopped;
    }

    public void disable() {
        stop();
        this.mNative.cleanupBatching();
        this.mEnabled = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resumeIfStarted() {
        if (DEBUG) {
            Log.d(TAG, "resumeIfStarted");
        }
        if (this.mStarted) {
            this.mNative.startBatch(this.mPeriodNanos, this.mWakeOnFifoFull);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class GnssBatchingProviderNative {
        GnssBatchingProviderNative() {
        }

        public int getBatchSize() {
            return GnssBatchingProvider.native_get_batch_size();
        }

        public boolean startBatch(long periodNanos, boolean wakeOnFifoFull) {
            return GnssBatchingProvider.native_start_batch(periodNanos, wakeOnFifoFull);
        }

        public void flushBatch() {
            GnssBatchingProvider.native_flush_batch();
        }

        public boolean stopBatch() {
            return GnssBatchingProvider.native_stop_batch();
        }

        public boolean initBatching() {
            return GnssBatchingProvider.native_init_batching();
        }

        public void cleanupBatching() {
            GnssBatchingProvider.native_cleanup_batching();
        }
    }
}