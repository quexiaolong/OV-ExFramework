package com.android.server.timezone;

/* loaded from: classes2.dex */
interface ConfigHelper {
    int getCheckTimeAllowedMillis();

    String getDataAppPackageName();

    int getFailedCheckRetryCount();

    String getUpdateAppPackageName();

    boolean isTrackingEnabled();
}