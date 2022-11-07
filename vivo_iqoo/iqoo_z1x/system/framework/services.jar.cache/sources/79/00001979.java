package com.android.server.storage;

/* loaded from: classes2.dex */
public interface DeviceStorageMonitorInternal {
    void checkMemory();

    long getMemoryLowThreshold();

    boolean isMemoryLow();
}