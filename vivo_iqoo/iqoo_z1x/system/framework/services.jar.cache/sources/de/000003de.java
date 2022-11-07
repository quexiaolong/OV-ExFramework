package com.android.server;

import android.os.Handler;

/* loaded from: classes.dex */
public final class AnimationThread extends ServiceThread {
    private static Handler sHandler;
    private static AnimationThread sInstance;

    private AnimationThread() {
        super("android.anim", -4, false);
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            AnimationThread animationThread = new AnimationThread();
            sInstance = animationThread;
            animationThread.start();
            sInstance.getLooper().setTraceTag(32L);
            sHandler = new Handler(sInstance.getLooper());
        }
    }

    public static AnimationThread get() {
        AnimationThread animationThread;
        synchronized (AnimationThread.class) {
            ensureThreadLocked();
            animationThread = sInstance;
        }
        return animationThread;
    }

    public static Handler getHandler() {
        Handler handler;
        synchronized (AnimationThread.class) {
            ensureThreadLocked();
            handler = sHandler;
        }
        return handler;
    }

    public static void dispose() {
        synchronized (AnimationThread.class) {
            if (sInstance == null) {
                return;
            }
            getHandler().runWithScissors($$Lambda$AnimationThread$mMqvPqhsYaiy1Cu67m4VbAgabsM.INSTANCE, 0L);
            sInstance = null;
        }
    }
}