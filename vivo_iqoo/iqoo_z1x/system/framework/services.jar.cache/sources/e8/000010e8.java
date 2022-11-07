package com.android.server.location;

import android.util.Log;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.job.controllers.JobStatus;
import com.android.server.usage.AppStandbyController;
import java.time.Instant;

/* loaded from: classes.dex */
public class LocationUsageLogger {
    private static final int API_USAGE_LOG_HOURLY_CAP = 60;
    private static final int ONE_HOUR_IN_MILLIS = 3600000;
    private static final int ONE_MINUTE_IN_MILLIS = 60000;
    private static final int ONE_SEC_IN_MILLIS = 1000;
    private long mLastApiUsageLogHour = 0;
    private int mApiUsageLogHourlyCount = 0;

    /* JADX WARN: Removed duplicated region for block: B:38:0x0072  */
    /* JADX WARN: Removed duplicated region for block: B:39:0x0075 A[Catch: Exception -> 0x008e, TryCatch #0 {Exception -> 0x008e, blocks: (B:2:0x0000, B:36:0x0066, B:40:0x007f, B:39:0x0075, B:33:0x0059, B:28:0x004c, B:24:0x003c, B:21:0x002f, B:18:0x0026, B:15:0x0019), top: B:45:0x0000 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void logLocationApiUsage(int r19, int r20, java.lang.String r21, android.location.LocationRequest r22, boolean r23, boolean r24, android.location.Geofence r25, int r26) {
        /*
            r18 = this;
            boolean r0 = r18.hitApiUsageLogCap()     // Catch: java.lang.Exception -> L8e
            if (r0 == 0) goto L7
            return
        L7:
            r0 = 1
            r1 = 0
            if (r22 != 0) goto Ld
            r2 = r0
            goto Le
        Ld:
            r2 = r1
        Le:
            if (r25 != 0) goto L12
            r3 = r0
            goto L13
        L12:
            r3 = r1
        L13:
            r4 = 210(0xd2, float:2.94E-43)
            if (r2 == 0) goto L19
            r8 = r1
            goto L22
        L19:
            java.lang.String r5 = r22.getProvider()     // Catch: java.lang.Exception -> L8e
            int r5 = bucketizeProvider(r5)     // Catch: java.lang.Exception -> L8e
            r8 = r5
        L22:
            if (r2 == 0) goto L26
            r9 = r1
            goto L2b
        L26:
            int r5 = r22.getQuality()     // Catch: java.lang.Exception -> L8e
            r9 = r5
        L2b:
            if (r2 == 0) goto L2f
            r10 = r1
            goto L38
        L2f:
            long r5 = r22.getInterval()     // Catch: java.lang.Exception -> L8e
            int r5 = bucketizeInterval(r5)     // Catch: java.lang.Exception -> L8e
            r10 = r5
        L38:
            if (r2 == 0) goto L3c
            r11 = r1
            goto L46
        L3c:
            float r5 = r22.getSmallestDisplacement()     // Catch: java.lang.Exception -> L8e
            int r5 = bucketizeDistance(r5)     // Catch: java.lang.Exception -> L8e
            r11 = r5
        L46:
            if (r2 == 0) goto L4c
            r5 = 0
        L4a:
            r12 = r5
            goto L52
        L4c:
            int r5 = r22.getNumUpdates()     // Catch: java.lang.Exception -> L8e
            long r5 = (long) r5     // Catch: java.lang.Exception -> L8e
            goto L4a
        L52:
            if (r2 != 0) goto L63
            r15 = r19
            if (r15 != r0) goto L59
            goto L65
        L59:
            long r5 = r22.getExpireIn()     // Catch: java.lang.Exception -> L8e
            int r0 = bucketizeExpireIn(r5)     // Catch: java.lang.Exception -> L8e
            r14 = r0
            goto L66
        L63:
            r15 = r19
        L65:
            r14 = r1
        L66:
            r7 = r20
            r6 = r23
            r5 = r24
            int r0 = getCallbackType(r7, r6, r5)     // Catch: java.lang.Exception -> L8e
            if (r3 == 0) goto L75
            r16 = r1
            goto L7f
        L75:
            float r1 = r25.getRadius()     // Catch: java.lang.Exception -> L8e
            int r1 = bucketizeRadius(r1)     // Catch: java.lang.Exception -> L8e
            r16 = r1
        L7f:
            int r17 = categorizeActivityImportance(r26)     // Catch: java.lang.Exception -> L8e
            r5 = r19
            r6 = r20
            r7 = r21
            r15 = r0
            com.android.internal.util.FrameworkStatsLog.write(r4, r5, r6, r7, r8, r9, r10, r11, r12, r14, r15, r16, r17)     // Catch: java.lang.Exception -> L8e
            goto L96
        L8e:
            r0 = move-exception
            java.lang.String r1 = "LocationManagerService"
            java.lang.String r2 = "Failed to log API usage to statsd."
            android.util.Log.w(r1, r2, r0)
        L96:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.LocationUsageLogger.logLocationApiUsage(int, int, java.lang.String, android.location.LocationRequest, boolean, boolean, android.location.Geofence, int):void");
    }

    public void logLocationApiUsage(int usageType, int apiInUse, String providerName) {
        try {
            if (hitApiUsageLogCap()) {
                return;
            }
            try {
                FrameworkStatsLog.write(210, usageType, apiInUse, (String) null, bucketizeProvider(providerName), 0, 0, 0, 0L, 0, getCallbackType(apiInUse, true, true), 0, 0);
            } catch (Exception e) {
                e = e;
                Log.w(LocationManagerService.TAG, "Failed to log API usage to statsd.", e);
            }
        } catch (Exception e2) {
            e = e2;
        }
    }

    private static int bucketizeProvider(String provider) {
        if ("network".equals(provider)) {
            return 1;
        }
        if ("gps".equals(provider)) {
            return 2;
        }
        if ("passive".equals(provider)) {
            return 3;
        }
        if ("fused".equals(provider)) {
            return 4;
        }
        return 0;
    }

    private static int bucketizeInterval(long interval) {
        if (interval < 1000) {
            return 1;
        }
        if (interval < 5000) {
            return 2;
        }
        if (interval < 60000) {
            return 3;
        }
        if (interval < 600000) {
            return 4;
        }
        if (interval < AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT) {
            return 5;
        }
        return 6;
    }

    private static int bucketizeDistance(float smallestDisplacement) {
        if (smallestDisplacement <= 0.0f) {
            return 1;
        }
        if (smallestDisplacement > 0.0f && smallestDisplacement <= 100.0f) {
            return 2;
        }
        return 3;
    }

    private static int bucketizeRadius(float radius) {
        if (radius < 0.0f) {
            return 7;
        }
        if (radius < 100.0f) {
            return 1;
        }
        if (radius < 200.0f) {
            return 2;
        }
        if (radius < 300.0f) {
            return 3;
        }
        if (radius < 1000.0f) {
            return 4;
        }
        if (radius < 10000.0f) {
            return 5;
        }
        return 6;
    }

    private static int bucketizeExpireIn(long expireIn) {
        if (expireIn == JobStatus.NO_LATEST_RUNTIME) {
            return 6;
        }
        if (expireIn < 20000) {
            return 1;
        }
        if (expireIn < 60000) {
            return 2;
        }
        if (expireIn < 600000) {
            return 3;
        }
        if (expireIn < AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT) {
            return 4;
        }
        return 5;
    }

    private static int categorizeActivityImportance(int importance) {
        if (importance == 100) {
            return 1;
        }
        if (importance == 125) {
            return 2;
        }
        return 3;
    }

    private static int getCallbackType(int apiType, boolean hasListener, boolean hasIntent) {
        if (apiType == 5) {
            return 1;
        }
        if (hasIntent) {
            return 3;
        }
        if (hasListener) {
            return 2;
        }
        return 0;
    }

    private synchronized boolean hitApiUsageLogCap() {
        long currentHour = Instant.now().toEpochMilli() / AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT;
        if (currentHour > this.mLastApiUsageLogHour) {
            this.mLastApiUsageLogHour = currentHour;
            this.mApiUsageLogHourlyCount = 0;
            return false;
        }
        int min = Math.min(this.mApiUsageLogHourlyCount + 1, 60);
        this.mApiUsageLogHourlyCount = min;
        return min >= 60;
    }
}