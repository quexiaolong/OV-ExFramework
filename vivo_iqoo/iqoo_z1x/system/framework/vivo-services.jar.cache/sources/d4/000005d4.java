package com.vivo.face.common.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;

/* loaded from: classes.dex */
public final class SettingUtils {
    public static void putSystemSettingInt(Context context, String field, int fieldValue) {
        Settings.System.putIntForUser(context.getContentResolver(), field, fieldValue, ActivityManager.getCurrentUser());
    }

    public static void putSecureSettingInt(Context context, String field, int fieldValue) {
        Settings.Secure.putIntForUser(context.getContentResolver(), field, fieldValue, ActivityManager.getCurrentUser());
    }

    public static int getSystemSettingInt(Context context, String field, int defaultValue) {
        return Settings.System.getIntForUser(context.getContentResolver(), field, defaultValue, ActivityManager.getCurrentUser());
    }

    public static int getSecureSettingInt(Context context, String field, int defaultValue) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), field, defaultValue, ActivityManager.getCurrentUser());
    }
}