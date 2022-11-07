package com.android.server;

import android.os.Handler;
import android.os.HandlerThread;

/* loaded from: classes.dex */
public final class UnifiedConfigThread extends HandlerThread {
    private static Handler sHandler;
    private static UnifiedConfigThread sInstance;

    private UnifiedConfigThread() {
        super("vivo.unifiedconfig", 0);
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            UnifiedConfigThread unifiedConfigThread = new UnifiedConfigThread();
            sInstance = unifiedConfigThread;
            unifiedConfigThread.start();
            sHandler = new Handler(sInstance.getLooper());
        }
    }

    public static UnifiedConfigThread get() {
        UnifiedConfigThread unifiedConfigThread;
        synchronized (UnifiedConfigThread.class) {
            ensureThreadLocked();
            unifiedConfigThread = sInstance;
        }
        return unifiedConfigThread;
    }

    public static Handler getHandler() {
        Handler handler;
        synchronized (UnifiedConfigThread.class) {
            ensureThreadLocked();
            handler = sHandler;
        }
        return handler;
    }
}