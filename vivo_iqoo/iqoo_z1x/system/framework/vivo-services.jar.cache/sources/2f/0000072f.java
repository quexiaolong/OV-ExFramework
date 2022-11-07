package com.vivo.services.rms.proxy;

import android.os.Bundle;
import android.os.SystemProperties;

/* loaded from: classes.dex */
public final class ProxyUtils {
    private static final String KEY_FEATURE_DELAY = "key_delay_send";
    private static final String KEY_FEATURE_ENABLE = "key_orientation_enable";
    static final String TAG = "VivoBinderProxy";
    public static boolean feature_support = SystemProperties.getBoolean("persist.vivo.orientation.opt", true);
    public static boolean delay_enable = SystemProperties.getBoolean("persist.vivo.orientation.delay_enable", false);

    public static boolean isFeatureSupport() {
        return feature_support;
    }

    public static boolean needDelaySend() {
        return delay_enable;
    }

    public static void setConfigs(Bundle data) {
        if (data != null) {
            setFeatureEnable(data.getBoolean(KEY_FEATURE_ENABLE, feature_support));
            setDelaySendEnable(data.getBoolean(KEY_FEATURE_DELAY, delay_enable));
        }
    }

    public static void setFeatureEnable(boolean enable) {
        feature_support = enable;
        SystemProperties.set("persist.vivo.orientation.opt", enable ? "true" : "false");
    }

    public static void setDelaySendEnable(boolean enable) {
        delay_enable = enable;
        SystemProperties.set("persist.vivo.orientation.delay_enable", enable ? "true" : "false");
    }
}