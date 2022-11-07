package com.android.server;

import android.common.OplusFeatureList;
import com.android.server.display.DisplayManagerService;

/* loaded from: classes.dex */
public interface IOplusSystemServerEx extends IOplusCommonSystemServerEx {
    public static final IOplusSystemServerEx DEFAULT = new IOplusSystemServerEx() { // from class: com.android.server.IOplusSystemServerEx.1
    };
    public static final String NAME = "IOplusSystemServerEx";

    default OplusFeatureList.OplusIndex index() {
        return OplusFeatureList.OplusIndex.IOplusSystemServerEx;
    }

    default IOplusSystemServerEx getDefault() {
        return DEFAULT;
    }

    default boolean startOplusLightService() {
        return false;
    }

    default boolean startOplusAccessibilityService() {
        return false;
    }

    default boolean startColorJobSchedulerService() {
        return false;
    }

    default void startColorScreenshotManagerService() {
    }

    default DisplayManagerService startColorDisplayManagerService() {
        return null;
    }

    default void addOplusDevicePolicyService() {
    }
}