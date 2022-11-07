package com.android.server;

import android.os.HandlerThread;
import android.os.Process;
import android.os.StrictMode;

/* loaded from: classes.dex */
public class ServiceThread extends HandlerThread {
    private static final String TAG = "ServiceThread";
    private final boolean mAllowIo;

    public ServiceThread(String name, int priority, boolean allowIo) {
        super(name, priority);
        this.mAllowIo = allowIo;
    }

    @Override // android.os.HandlerThread, java.lang.Thread, java.lang.Runnable
    public void run() {
        Process.setCanSelfBackground(false);
        if (!this.mAllowIo) {
            StrictMode.initThreadDefaults(null);
        }
        super.run();
    }
}