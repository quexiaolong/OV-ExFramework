package com.vivo.face.common.notification;

import android.content.Context;
import java.util.ArrayList;

/* loaded from: classes.dex */
public final class FaceSystemNotify {
    private static final Object LOCK = new Object();
    private static final String TAG = "FaceSystemNotify";
    private static FaceSystemNotify sInstance;
    private final ArrayList<SystemCallback> mCallbacks = new ArrayList<>();
    private Context mContext;

    /* loaded from: classes.dex */
    public interface SystemCallback {
        void onDisplayStateChanged(int i, int i2, int i3);
    }

    private FaceSystemNotify() {
    }

    public static FaceSystemNotify getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = new FaceSystemNotify();
                }
            }
        }
        return sInstance;
    }

    public void registerCallback(SystemCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        synchronized (this) {
            int index = findCallbackLocked(callback);
            if (index < 0) {
                this.mCallbacks.add(callback);
            }
        }
    }

    public void unregisterCallback(SystemCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        synchronized (this) {
            int index = findCallbackLocked(callback);
            if (index >= 0) {
                this.mCallbacks.remove(index);
            }
        }
    }

    private int findCallbackLocked(SystemCallback callback) {
        int numCallbacks = this.mCallbacks.size();
        for (int i = 0; i < numCallbacks; i++) {
            if (this.mCallbacks.get(i) == callback) {
                return i;
            }
        }
        return -1;
    }

    public void onDisplayStateChanged(int displayId, int state, int backlight) {
        synchronized (this) {
            int numCallbacks = this.mCallbacks.size();
            for (int i = 0; i < numCallbacks; i++) {
                this.mCallbacks.get(i).onDisplayStateChanged(displayId, state, backlight);
            }
        }
    }
}