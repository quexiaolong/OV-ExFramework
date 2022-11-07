package com.vivo.face.common.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/* loaded from: classes.dex */
public final class FaceUIFactory {
    private static final String TAG = "FaceUIFactory";

    public static boolean checkSystemApplication(Context context, String pkgName) {
        PackageInfo pi = null;
        boolean z = false;
        try {
            PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(pkgName, 0);
        } catch (Exception e) {
            FaceLog.e(TAG, "Failed to get package info " + pkgName, e);
        }
        if (pi == null) {
            return false;
        }
        boolean isSysApp = (pi.applicationInfo.flags & 1) == 1;
        boolean isSysUpd = (pi.applicationInfo.flags & 128) == 1;
        if (isSysApp || isSysUpd) {
            z = true;
        }
        boolean isSystemApp = z;
        return isSystemApp;
    }
}