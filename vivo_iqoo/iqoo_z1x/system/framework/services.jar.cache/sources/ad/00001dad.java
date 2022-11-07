package com.android.server.wm;

/* loaded from: classes2.dex */
public interface ActivityMetricsLaunchObserverRegistry {
    void registerLaunchObserver(ActivityMetricsLaunchObserver activityMetricsLaunchObserver);

    void unregisterLaunchObserver(ActivityMetricsLaunchObserver activityMetricsLaunchObserver);
}