package com.android.server.location;

import android.content.Context;

/* loaded from: classes.dex */
public final class LocationPermissionUtil {
    public static boolean doesCallerReportToAppOps(Context context, CallerIdentity callerIdentity) {
        return hasPermissionLocationHardware(context, callerIdentity) && hasPermissionUpdateAppOpsStats(context, callerIdentity);
    }

    private static boolean hasPermissionLocationHardware(Context context, CallerIdentity callerIdentity) {
        return context.checkPermission("android.permission.LOCATION_HARDWARE", callerIdentity.pid, callerIdentity.uid) == 0;
    }

    private static boolean hasPermissionUpdateAppOpsStats(Context context, CallerIdentity callerIdentity) {
        return context.checkPermission("android.permission.UPDATE_APP_OPS_STATS", callerIdentity.pid, callerIdentity.uid) == 0;
    }

    private LocationPermissionUtil() {
    }
}