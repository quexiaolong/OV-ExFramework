package com.android.server;

import android.os.Handler;

/* loaded from: classes.dex */
public final class DisplayThread extends ServiceThread {
    private static Handler sHandler;
    private static DisplayThread sInstance;

    private DisplayThread() {
        super("android.display", -3, false);
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            DisplayThread displayThread = new DisplayThread();
            sInstance = displayThread;
            displayThread.start();
            sInstance.getLooper().setTraceTag(524288L);
            sHandler = new Handler(sInstance.getLooper());
        }
    }

    public static DisplayThread get() {
        DisplayThread displayThread;
        synchronized (DisplayThread.class) {
            ensureThreadLocked();
            displayThread = sInstance;
        }
        return displayThread;
    }

    public static Handler getHandler() {
        Handler handler;
        synchronized (DisplayThread.class) {
            ensureThreadLocked();
            handler = sHandler;
        }
        return handler;
    }

    public static void dispose() {
        synchronized (DisplayThread.class) {
            if (sInstance == null) {
                return;
            }
            getHandler().runWithScissors($$Lambda$DisplayThread$f5MRsrGyBEbIMjOX5lqvMSkf2g.INSTANCE, 0L);
            sInstance = null;
        }
    }
}