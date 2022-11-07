package com.android.server.tv;

import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvStreamConfig;
import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import java.util.LinkedList;
import java.util.Queue;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class TvInputHal implements Handler.Callback {
    private static final boolean DEBUG = false;
    public static final int ERROR_NO_INIT = -1;
    public static final int ERROR_STALE_CONFIG = -2;
    public static final int ERROR_UNKNOWN = -3;
    public static final int EVENT_DEVICE_AVAILABLE = 1;
    public static final int EVENT_DEVICE_UNAVAILABLE = 2;
    public static final int EVENT_FIRST_FRAME_CAPTURED = 4;
    public static final int EVENT_STREAM_CONFIGURATION_CHANGED = 3;
    public static final int SUCCESS = 0;
    private static final String TAG = TvInputHal.class.getSimpleName();
    private final Callback mCallback;
    private final Object mLock = new Object();
    private long mPtr = 0;
    private final SparseIntArray mStreamConfigGenerations = new SparseIntArray();
    private final SparseArray<TvStreamConfig[]> mStreamConfigs = new SparseArray<>();
    private final Queue<Message> mPendingMessageQueue = new LinkedList();
    private final Handler mHandler = new Handler(this);

    /* loaded from: classes2.dex */
    public interface Callback {
        void onDeviceAvailable(TvInputHardwareInfo tvInputHardwareInfo, TvStreamConfig[] tvStreamConfigArr);

        void onDeviceUnavailable(int i);

        void onFirstFrameCaptured(int i, int i2);

        void onStreamConfigurationChanged(int i, TvStreamConfig[] tvStreamConfigArr);
    }

    private static native int nativeAddOrUpdateStream(long j, int i, int i2, Surface surface);

    private static native void nativeClose(long j);

    private static native TvStreamConfig[] nativeGetStreamConfigs(long j, int i, int i2);

    private native long nativeOpen(MessageQueue messageQueue);

    private static native int nativeRemoveStream(long j, int i, int i2);

    public TvInputHal(Callback callback) {
        this.mCallback = callback;
    }

    public void init() {
        synchronized (this.mLock) {
            this.mPtr = nativeOpen(this.mHandler.getLooper().getQueue());
        }
    }

    public int addOrUpdateStream(int deviceId, Surface surface, TvStreamConfig streamConfig) {
        synchronized (this.mLock) {
            if (this.mPtr == 0) {
                return -1;
            }
            int generation = this.mStreamConfigGenerations.get(deviceId, 0);
            if (generation != streamConfig.getGeneration()) {
                return -2;
            }
            return nativeAddOrUpdateStream(this.mPtr, deviceId, streamConfig.getStreamId(), surface) == 0 ? 0 : -3;
        }
    }

    public int removeStream(int deviceId, TvStreamConfig streamConfig) {
        synchronized (this.mLock) {
            if (this.mPtr == 0) {
                return -1;
            }
            int generation = this.mStreamConfigGenerations.get(deviceId, 0);
            if (generation != streamConfig.getGeneration()) {
                return -2;
            }
            return nativeRemoveStream(this.mPtr, deviceId, streamConfig.getStreamId()) == 0 ? 0 : -3;
        }
    }

    public void close() {
        synchronized (this.mLock) {
            if (this.mPtr != 0) {
                nativeClose(this.mPtr);
            }
        }
    }

    private void retrieveStreamConfigsLocked(int deviceId) {
        int generation = this.mStreamConfigGenerations.get(deviceId, 0) + 1;
        this.mStreamConfigs.put(deviceId, nativeGetStreamConfigs(this.mPtr, deviceId, generation));
        this.mStreamConfigGenerations.put(deviceId, generation);
    }

    private void deviceAvailableFromNative(TvInputHardwareInfo info) {
        this.mHandler.obtainMessage(1, info).sendToTarget();
    }

    private void deviceUnavailableFromNative(int deviceId) {
        this.mHandler.obtainMessage(2, deviceId, 0).sendToTarget();
    }

    private void streamConfigsChangedFromNative(int deviceId) {
        this.mHandler.obtainMessage(3, deviceId, 0).sendToTarget();
    }

    private void firstFrameCapturedFromNative(int deviceId, int streamId) {
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(3, deviceId, streamId));
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        TvStreamConfig[] configs;
        TvStreamConfig[] configs2;
        int i = msg.what;
        if (i == 1) {
            TvInputHardwareInfo info = (TvInputHardwareInfo) msg.obj;
            synchronized (this.mLock) {
                retrieveStreamConfigsLocked(info.getDeviceId());
                configs = this.mStreamConfigs.get(info.getDeviceId());
            }
            this.mCallback.onDeviceAvailable(info, configs);
        } else if (i == 2) {
            this.mCallback.onDeviceUnavailable(msg.arg1);
        } else if (i == 3) {
            int deviceId = msg.arg1;
            synchronized (this.mLock) {
                retrieveStreamConfigsLocked(deviceId);
                configs2 = this.mStreamConfigs.get(deviceId);
            }
            this.mCallback.onStreamConfigurationChanged(deviceId, configs2);
        } else if (i == 4) {
            int deviceId2 = msg.arg1;
            int streamId = msg.arg2;
            this.mCallback.onFirstFrameCaptured(deviceId2, streamId);
        } else {
            String str = TAG;
            Slog.e(str, "Unknown event: " + msg);
            return false;
        }
        return true;
    }
}