package com.android.server.wm;

import android.os.Handler;
import com.android.server.ServiceThread;

/* loaded from: classes2.dex */
public final class SurfaceAnimationThread extends ServiceThread {
    private static Handler sHandler;
    private static SurfaceAnimationThread sInstance;

    private SurfaceAnimationThread() {
        super("android.anim.lf", -4, false);
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            SurfaceAnimationThread surfaceAnimationThread = new SurfaceAnimationThread();
            sInstance = surfaceAnimationThread;
            surfaceAnimationThread.start();
            sInstance.getLooper().setTraceTag(32L);
            sHandler = new Handler(sInstance.getLooper());
        }
    }

    public static SurfaceAnimationThread get() {
        SurfaceAnimationThread surfaceAnimationThread;
        synchronized (SurfaceAnimationThread.class) {
            ensureThreadLocked();
            surfaceAnimationThread = sInstance;
        }
        return surfaceAnimationThread;
    }

    public static Handler getHandler() {
        Handler handler;
        synchronized (SurfaceAnimationThread.class) {
            ensureThreadLocked();
            handler = sHandler;
        }
        return handler;
    }

    public static void dispose() {
        synchronized (SurfaceAnimationThread.class) {
            if (sInstance == null) {
                return;
            }
            getHandler().runWithScissors($$Lambda$SurfaceAnimationThread$frZMbXAzhUBmXwz0SwbLTXpw9k.INSTANCE, 0L);
            sInstance = null;
        }
    }
}