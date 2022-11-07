package com.android.server;

import android.os.Handler;
import android.os.HandlerExecutor;
import java.util.concurrent.Executor;

/* loaded from: classes.dex */
public final class IoThread extends ServiceThread {
    private static Handler sHandler;
    private static HandlerExecutor sHandlerExecutor;
    private static IoThread sInstance;

    private IoThread() {
        super("android.io", 0, true);
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            IoThread ioThread = new IoThread();
            sInstance = ioThread;
            ioThread.start();
            sInstance.getLooper().setTraceTag(524288L);
            sHandler = new Handler(sInstance.getLooper());
            sHandlerExecutor = new HandlerExecutor(sHandler);
        }
    }

    public static IoThread get() {
        IoThread ioThread;
        synchronized (IoThread.class) {
            ensureThreadLocked();
            ioThread = sInstance;
        }
        return ioThread;
    }

    public static Handler getHandler() {
        Handler handler;
        synchronized (IoThread.class) {
            ensureThreadLocked();
            handler = sHandler;
        }
        return handler;
    }

    public static Executor getExecutor() {
        HandlerExecutor handlerExecutor;
        synchronized (IoThread.class) {
            ensureThreadLocked();
            handlerExecutor = sHandlerExecutor;
        }
        return handlerExecutor;
    }
}