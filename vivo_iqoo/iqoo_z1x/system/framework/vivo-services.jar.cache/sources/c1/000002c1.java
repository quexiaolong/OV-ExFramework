package com.android.server.location;

import java.util.HashMap;

/* loaded from: classes.dex */
public class VivoLocationFeature {
    public static final String FEATURE_API_CONTROL = "api_control";
    public static final String FEATURE_BIG_DATA = "location_big_data";
    public static final String FEATURE_BLACKLIST_APP = "gps_blacklist_app";
    public static final String FEATURE_CN0_WEAK = "cn0_weak";
    public static final String FEATURE_DOUBLE_INSTANCE = "double_instance";
    public static final String FEATURE_DR_LOCATION = "dr_location";
    public static final String FEATURE_DYNAMIC_LOG = "dynamic_log";
    public static final String FEATURE_FACTORY_MODE = "factory_mode";
    public static final String FEATURE_FILT_SAME_REQUEST = "filter_same_request";
    public static final String FEATURE_FUSED_LOCATION = "fused_location";
    public static final String FEATURE_INDEPENDENT_THREAD = "independent_thread";
    public static final String FEATURE_LISTENER_CONTROL = "listener_control";
    public static final String FEATURE_LOCATIION_NOTIFY = "location_off_notify";
    public static final String FEATURE_LOCATION_CONFIG = "location_config";
    public static final String FEATURE_NLP_SWITCH = "nlp_switch";
    public static final String FEATURE_NO_SV_FIX_RECOVERY = "no_sv_or_fix_recovery";
    public static final String FEATURE_PEM_FROZEN = "vivo_pem_frozen";
    public static final String FEATURE_POWER_MONITOR = "gps_power_monitor";
    public static final String FEATURE_TEST_APP_CHECK = "cts_skip_report_check";
    public static final String FEATURE_VIVO_PERMISSION = "vivo_permission";
    public static final String FEATURE_WEIXIN_OPTIMIZE = "weixin_location_optimize";
    private static final String TAG = "VivoLocationFeature";
    private static HashMap<String, Boolean> sCachedFeatureMap = new HashMap<>();

    static {
        initDefaultFeature();
    }

    private static void initDefaultFeature() {
        sCachedFeatureMap.put(FEATURE_LOCATIION_NOTIFY, true);
        sCachedFeatureMap.put(FEATURE_WEIXIN_OPTIMIZE, true);
        sCachedFeatureMap.put(FEATURE_PEM_FROZEN, true);
        sCachedFeatureMap.put(FEATURE_TEST_APP_CHECK, true);
        sCachedFeatureMap.put(FEATURE_LISTENER_CONTROL, true);
        sCachedFeatureMap.put(FEATURE_BLACKLIST_APP, true);
        sCachedFeatureMap.put(FEATURE_INDEPENDENT_THREAD, true);
        sCachedFeatureMap.put(FEATURE_VIVO_PERMISSION, true);
        sCachedFeatureMap.put(FEATURE_BIG_DATA, true);
        sCachedFeatureMap.put(FEATURE_NLP_SWITCH, true);
        sCachedFeatureMap.put(FEATURE_LOCATION_CONFIG, true);
        sCachedFeatureMap.put(FEATURE_FILT_SAME_REQUEST, true);
        sCachedFeatureMap.put(FEATURE_DOUBLE_INSTANCE, true);
        sCachedFeatureMap.put(FEATURE_DYNAMIC_LOG, true);
        sCachedFeatureMap.put(FEATURE_FACTORY_MODE, true);
        sCachedFeatureMap.put(FEATURE_FUSED_LOCATION, true);
        sCachedFeatureMap.put(FEATURE_CN0_WEAK, true);
        sCachedFeatureMap.put(FEATURE_DR_LOCATION, true);
        sCachedFeatureMap.put(FEATURE_NO_SV_FIX_RECOVERY, true);
        sCachedFeatureMap.put(FEATURE_POWER_MONITOR, true);
        sCachedFeatureMap.put(FEATURE_API_CONTROL, true);
    }

    public static boolean isFeatureSupport(String feature) {
        if (sCachedFeatureMap.containsKey(feature)) {
            return sCachedFeatureMap.get(feature).booleanValue();
        }
        return false;
    }
}