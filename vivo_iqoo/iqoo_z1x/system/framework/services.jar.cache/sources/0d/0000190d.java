package com.android.server.soundtrigger_middleware;

import android.util.Log;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/* loaded from: classes2.dex */
public class ExternalCaptureStateTracker {
    private static final String TAG = "CaptureStateTracker";
    private final Consumer<Boolean> mListener;
    private final Semaphore mNeedToConnect = new Semaphore(1);

    private native void connect();

    public static /* synthetic */ void lambda$Ygm9zjschDPyC1_diGoIJXbnmGc(ExternalCaptureStateTracker externalCaptureStateTracker) {
        externalCaptureStateTracker.run();
    }

    public ExternalCaptureStateTracker(Consumer<Boolean> listener) {
        this.mListener = listener;
        new Thread(new Runnable() { // from class: com.android.server.soundtrigger_middleware.-$$Lambda$ExternalCaptureStateTracker$Ygm9zjschDPyC1_diGoIJXbnmGc
            @Override // java.lang.Runnable
            public final void run() {
                ExternalCaptureStateTracker.lambda$Ygm9zjschDPyC1_diGoIJXbnmGc(ExternalCaptureStateTracker.this);
            }
        }).start();
    }

    public void run() {
        while (true) {
            this.mNeedToConnect.acquireUninterruptibly();
            connect();
        }
    }

    private void setCaptureState(boolean active) {
        this.mListener.accept(Boolean.valueOf(active));
    }

    private void binderDied() {
        Log.w(TAG, "Audio policy service died");
        this.mNeedToConnect.release();
    }
}