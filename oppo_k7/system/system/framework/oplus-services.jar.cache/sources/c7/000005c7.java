package com.android.server;

import android.common.IOplusCommonFeature;

/* loaded from: classes.dex */
public interface IOplusCommonSystemServerEx extends IOplusCommonFeature {
    default void startBootstrapServices() {
    }

    default void startCoreServices() {
    }

    default void startOtherServices() {
    }

    default void systemReady() {
    }

    default void systemRunning() {
    }
}