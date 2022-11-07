package com.android.server.am;

import android.content.Context;
import android.provider.DeviceConfig;
import com.android.server.am.frozen.BaseState;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;

/* loaded from: classes.dex */
public class VivoCachedAppOptimizer implements IVivoCachedAppOptimizer {
    private static final int COMPACT_ACTION_ANON_FLAG = 2;
    private static AbsVivoPerfManager mVperf = null;

    public VivoCachedAppOptimizer() {
        if (mVperf == null) {
            mVperf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        }
    }

    public void init() {
        boolean useCompaction = Boolean.valueOf(mVperf.perfGetProp("vendor.appcompact.enable_app_compact", "false")).booleanValue();
        int someCompactionType = Integer.valueOf(mVperf.perfGetProp("vendor.appcompact.some_compact_type", String.valueOf(2))).intValue();
        int fullCompactionType = Integer.valueOf(mVperf.perfGetProp("vendor.appcompact.full_compact_type", String.valueOf(2))).intValue();
        int compactThrottleSomeSome = Integer.valueOf(mVperf.perfGetProp("vendor.appcompact.compact_throttle_somesome", String.valueOf(5000L))).intValue();
        int compactThrottleSomeFull = Integer.valueOf(mVperf.perfGetProp("vendor.appcompact.compact_throttle_somefull", String.valueOf(10000L))).intValue();
        int compactThrottleFullSome = Integer.valueOf(mVperf.perfGetProp("vendor.appcompact.compact_throttle_fullsome", String.valueOf(500L))).intValue();
        int compactThrottleFullFull = Integer.valueOf(mVperf.perfGetProp("vendor.appcompact.compact_throttle_fullfull", String.valueOf(10000L))).intValue();
        int compactThrottleBfgs = Integer.valueOf(mVperf.perfGetProp("vendor.appcompact.compact_throttle_bfgs", String.valueOf(600000L))).intValue();
        int compactThrottlePersistent = Integer.valueOf(mVperf.perfGetProp("vendor.appcompact.compact_throttle_persistent", String.valueOf(600000L))).intValue();
        int fullRssThrottleKB = Integer.valueOf(mVperf.perfGetProp("vendor.appcompact.rss_throttle_kb", String.valueOf((long) BaseState.RECYCLE_DEFERRED_STATES_TIME))).intValue();
        int deltaRssThrottleKB = Integer.valueOf(mVperf.perfGetProp("vendor.appcompact.delta_rss_throttle_kb", String.valueOf(8000L))).intValue();
        if ("PD2054".equals(System.getProperty("ro.vivo.product.model"))) {
            DeviceConfig.setProperty("activity_manager", "compact_action_1", String.valueOf(someCompactionType), true);
            DeviceConfig.setProperty("activity_manager", "compact_action_2", String.valueOf(fullCompactionType), true);
            DeviceConfig.setProperty("activity_manager", "compact_throttle_1", String.valueOf(compactThrottleSomeSome), true);
            DeviceConfig.setProperty("activity_manager", "compact_throttle_2", String.valueOf(compactThrottleSomeFull), true);
            DeviceConfig.setProperty("activity_manager", "compact_throttle_3", String.valueOf(compactThrottleFullSome), true);
            DeviceConfig.setProperty("activity_manager", "compact_throttle_4", String.valueOf(compactThrottleFullFull), true);
            DeviceConfig.setProperty("activity_manager", "compact_throttle_5", String.valueOf(compactThrottleBfgs), true);
            DeviceConfig.setProperty("activity_manager", "compact_throttle_6", String.valueOf(compactThrottlePersistent), true);
            DeviceConfig.setProperty("activity_manager", "compact_full_rss_throttle_kb", String.valueOf(fullRssThrottleKB), true);
            DeviceConfig.setProperty("activity_manager", "compact_full_delta_rss_throttle_kb", String.valueOf(deltaRssThrottleKB), true);
            DeviceConfig.setProperty("activity_manager", "use_compaction", String.valueOf(useCompaction), true);
        }
    }
}