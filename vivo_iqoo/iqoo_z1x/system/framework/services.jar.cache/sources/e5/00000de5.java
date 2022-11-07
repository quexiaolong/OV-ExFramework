package com.android.server.display.utils;

import android.content.res.Resources;
import android.util.TypedValue;
import com.android.server.display.utils.AmbientFilter;

/* loaded from: classes.dex */
public class AmbientFilterFactory {
    public static AmbientFilter createAmbientFilter(String tag, int horizon, float intercept) {
        if (!Float.isNaN(intercept)) {
            return new AmbientFilter.WeightedMovingAverageAmbientFilter(tag, horizon, intercept);
        }
        throw new IllegalArgumentException("missing configurations: expected config_displayWhiteBalanceBrightnessFilterIntercept");
    }

    public static AmbientFilter createBrightnessFilter(String tag, Resources resources) {
        int horizon = resources.getInteger(17694792);
        float intercept = getFloat(resources, 17105063);
        return createAmbientFilter(tag, horizon, intercept);
    }

    public static AmbientFilter createColorTemperatureFilter(String tag, Resources resources) {
        int horizon = resources.getInteger(17694795);
        float intercept = getFloat(resources, 17105064);
        return createAmbientFilter(tag, horizon, intercept);
    }

    private AmbientFilterFactory() {
    }

    private static float getFloat(Resources resources, int id) {
        TypedValue value = new TypedValue();
        resources.getValue(id, value, true);
        if (value.type != 4) {
            return Float.NaN;
        }
        return value.getFloat();
    }
}