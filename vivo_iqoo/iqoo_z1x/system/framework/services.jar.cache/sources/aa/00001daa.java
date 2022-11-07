package com.android.server.wm;

import android.content.Intent;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* loaded from: classes2.dex */
public interface ActivityMetricsLaunchObserver {
    public static final int TEMPERATURE_COLD = 1;
    public static final int TEMPERATURE_HOT = 3;
    public static final int TEMPERATURE_WARM = 2;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ActivityRecordProto {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Temperature {
    }

    void onActivityLaunchCancelled(byte[] bArr);

    void onActivityLaunchFinished(byte[] bArr, long j);

    void onActivityLaunched(byte[] bArr, int i);

    void onIntentFailed();

    void onIntentStarted(Intent intent, long j);

    void onReportFullyDrawn(byte[] bArr, long j);
}