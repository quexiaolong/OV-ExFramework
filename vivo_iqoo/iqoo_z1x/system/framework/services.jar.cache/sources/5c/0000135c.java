package com.android.server.notification;

import android.os.SystemClock;

/* loaded from: classes.dex */
public class InjectableSystemClockImpl implements InjectableSystemClock {
    @Override // com.android.server.notification.InjectableSystemClock
    public long uptimeMillis() {
        return SystemClock.uptimeMillis();
    }

    @Override // com.android.server.notification.InjectableSystemClock
    public long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    @Override // com.android.server.notification.InjectableSystemClock
    public long elapsedRealtimeNanos() {
        return SystemClock.elapsedRealtimeNanos();
    }

    @Override // com.android.server.notification.InjectableSystemClock
    public long currentThreadTimeMillis() {
        return SystemClock.currentThreadTimeMillis();
    }

    @Override // com.android.server.notification.InjectableSystemClock
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}