package com.vivo.face.common.utils;

import android.os.Build;
import android.os.SystemProperties;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class FaceLog {
    private static final String TAG_PREFIX = "BF/";
    private static final String VERIFY_SPEED_LOG_PREFIX = "face-speed: ";
    private static final Boolean BUILD_ENG = Boolean.valueOf(Build.TYPE.equals("eng"));
    private static final Boolean BUILD_DEBUG = Boolean.valueOf(Build.TYPE.equals("branddebug"));
    private static final boolean USE_TAG_PREFIX = SystemProperties.getBoolean("sys.vivo.face.use_tag_prefix", true);
    private static final boolean LOG_ON = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");

    private static boolean allowPrint() {
        return BUILD_ENG.booleanValue() || BUILD_DEBUG.booleanValue() || LOG_ON;
    }

    public static void v(String tag, String value) {
        String str;
        if (allowPrint()) {
            if (USE_TAG_PREFIX) {
                str = TAG_PREFIX + tag;
            } else {
                str = tag;
            }
            VSlog.v(str, value);
        }
    }

    public static void d(String tag, String value) {
        String str;
        if (allowPrint()) {
            if (USE_TAG_PREFIX) {
                str = TAG_PREFIX + tag;
            } else {
                str = tag;
            }
            VSlog.d(str, value);
        }
    }

    public static void i(String tag, String value) {
        String str;
        if (allowPrint()) {
            if (USE_TAG_PREFIX) {
                str = TAG_PREFIX + tag;
            } else {
                str = tag;
            }
            VSlog.i(str, value);
        }
    }

    public static void speed(String tag, String value) {
        String str;
        if (USE_TAG_PREFIX) {
            str = TAG_PREFIX + tag;
        } else {
            str = tag;
        }
        VSlog.i(str, value);
    }

    public static void e(String tag, String value) {
        String str;
        if (USE_TAG_PREFIX) {
            str = TAG_PREFIX + tag;
        } else {
            str = tag;
        }
        VSlog.e(str, value);
    }

    public static void e(String tag, String value, Throwable tr) {
        String str;
        if (USE_TAG_PREFIX) {
            str = TAG_PREFIX + tag;
        } else {
            str = tag;
        }
        VSlog.e(str, value, tr);
    }

    public static void w(String tag, String value) {
        String str;
        if (USE_TAG_PREFIX) {
            str = TAG_PREFIX + tag;
        } else {
            str = tag;
        }
        VSlog.w(str, value);
    }

    public static void w(String tag, String value, Throwable tr) {
        String str;
        if (USE_TAG_PREFIX) {
            str = TAG_PREFIX + tag;
        } else {
            str = tag;
        }
        VSlog.w(str, value, tr);
    }
}