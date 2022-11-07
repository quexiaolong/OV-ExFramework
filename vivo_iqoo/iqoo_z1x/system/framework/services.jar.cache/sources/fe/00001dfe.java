package com.android.server.wm;

import android.content.res.Configuration;

/* loaded from: classes2.dex */
public interface ConfigurationContainerListener {
    default void onRequestedOverrideConfigurationChanged(Configuration overrideConfiguration) {
    }

    default void onMergedOverrideConfigurationChanged(Configuration mergedOverrideConfiguration) {
    }
}