package com.android.server;

import android.hardware.health.V1_0.HealthInfo;

/* loaded from: classes.dex */
public interface IVivoBatteryService {
    void dummy();

    int getCriticalBatteryLevel();

    HealthInfo getHealthInfo();

    void processValuesLocked();

    /* loaded from: classes.dex */
    public interface IVivoBatteryServiceExport {
        IVivoBatteryService getVivoInjectInstance();

        default void dummyExport() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }

        default HealthInfo getHealthInfo() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().getHealthInfo();
            }
            return null;
        }

        default int getCriticalBatteryLevel() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().getCriticalBatteryLevel();
            }
            return 5;
        }
    }
}