package com.android.server.utils;

import android.util.Slog;
import android.util.TimingsTraceLog;

/* loaded from: classes2.dex */
public final class TimingsTraceAndSlog extends TimingsTraceLog {
    private static final long BOTTLENECK_DURATION_MS = -1;
    private static final String SYSTEM_SERVER_TIMING_ASYNC_TAG = "SystemServerTimingAsync";
    public static final String SYSTEM_SERVER_TIMING_TAG = "SystemServerTiming";
    private final String mTag;

    public static TimingsTraceAndSlog newAsyncLog() {
        return new TimingsTraceAndSlog(SYSTEM_SERVER_TIMING_ASYNC_TAG, 524288L);
    }

    public TimingsTraceAndSlog() {
        this(SYSTEM_SERVER_TIMING_TAG);
    }

    public TimingsTraceAndSlog(String tag) {
        this(tag, 524288L);
    }

    public TimingsTraceAndSlog(String tag, long traceTag) {
        super(tag, traceTag);
        this.mTag = tag;
    }

    public void traceBegin(String name) {
        Slog.i(this.mTag, name);
        super.traceBegin(name);
    }

    public void logDuration(String name, long timeMs) {
        super.logDuration(name, timeMs);
    }

    public String toString() {
        return "TimingsTraceAndSlog[" + this.mTag + "]";
    }
}