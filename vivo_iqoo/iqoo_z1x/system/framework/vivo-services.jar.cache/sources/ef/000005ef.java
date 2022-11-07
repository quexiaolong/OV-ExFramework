package com.vivo.face.internal.wrapper;

import android.util.FtFeature;

/* loaded from: classes.dex */
public final class FtFeatureWrapper {
    private static final String TAG = "FtFeatureWrapper";

    public static boolean isFeatureSupport(int mask) {
        return FtFeature.isFeatureSupport(mask);
    }
}