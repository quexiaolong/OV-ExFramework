package com.vivo.services.autorecover;

import android.os.HandlerThread;
import android.text.TextUtils;
import com.android.server.policy.VivoPolicyUtil;
import java.lang.Thread;
import vivo.app.configuration.ContentValuesList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class SystemExceptionHandler {
    static final String TAG = "SystemAutoRecoverService";
    private final ExceptionMatcher mExceptionMatcher = new ExceptionMatcher();
    private Thread.UncaughtExceptionHandler mOriginHandler;

    public void onStart() {
        initProcessDefaultExceptionCatcher();
    }

    private void initProcessDefaultExceptionCatcher() {
        this.mOriginHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler autoCoverHandler = new Thread.UncaughtExceptionHandler() { // from class: com.vivo.services.autorecover.-$$Lambda$SystemExceptionHandler$ehwMm3U0vxSj5EKALzfMRLErorw
            @Override // java.lang.Thread.UncaughtExceptionHandler
            public final void uncaughtException(Thread thread, Throwable th) {
                SystemExceptionHandler.this.lambda$initProcessDefaultExceptionCatcher$0$SystemExceptionHandler(thread, th);
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(autoCoverHandler);
    }

    public /* synthetic */ void lambda$initProcessDefaultExceptionCatcher$0$SystemExceptionHandler(Thread thread, Throwable exception) {
        if (VivoPolicyUtil.IS_LOG_OPEN) {
            VSlog.d("SystemAutoRecoverService", "exception happened in thread " + thread.getName());
        }
        String threadName = thread.getName();
        if ((exception instanceof Error) && !TextUtils.isEmpty(threadName) && threadName.startsWith("Binder")) {
            dispatchUncaughtException(thread, exception);
        }
        if (thread instanceof HandlerThread) {
            dispatchUncaughtException(thread, exception);
        }
        if (this.mExceptionMatcher.matchException(exception)) {
            return;
        }
        dispatchUncaughtException(thread, exception);
    }

    private void dispatchUncaughtException(Thread thread, Throwable exception) {
        Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = this.mOriginHandler;
        if (defaultUncaughtExceptionHandler != uncaughtExceptionHandler && uncaughtExceptionHandler != null) {
            uncaughtExceptionHandler.uncaughtException(thread, exception);
        }
    }

    public boolean matchException(Throwable t) {
        try {
            return this.mExceptionMatcher.matchException(t);
        } catch (Exception e) {
            return false;
        }
    }

    public void updateExceptionMatcher(ContentValuesList contentValuesList) {
        this.mExceptionMatcher.updateMatcher(contentValuesList);
    }

    public ContentValuesList getCurrentList() {
        return this.mExceptionMatcher.getCurrentList();
    }
}