package com.vivo.services.rms.sp.config;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.SystemProperties;

/* loaded from: classes.dex */
public class Helpers {
    private Helpers() {
    }

    private static ApplicationInfo getApplicationInfo(String packageName, Context context) {
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }
        try {
            return pm.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static long getVersion(String packageName, Context context) {
        ApplicationInfo aInfo;
        if (context == null || (aInfo = getApplicationInfo(packageName, context)) == null) {
            return -1L;
        }
        return aInfo.longVersionCode;
    }

    public static String determineModel() {
        String model = SystemProperties.get("ro.vivo.product.model", "UNKNOWN");
        if (model.equals("UNKNOWN")) {
            return SystemProperties.get("ro.product.name", "UNKNOWN");
        }
        return model;
    }
}