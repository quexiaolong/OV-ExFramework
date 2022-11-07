package com.android.server.notification;

/* loaded from: classes.dex */
public interface InjectableSystemClock {
    long currentThreadTimeMillis();

    long currentTimeMillis();

    long elapsedRealtime();

    long elapsedRealtimeNanos();

    long uptimeMillis();
}