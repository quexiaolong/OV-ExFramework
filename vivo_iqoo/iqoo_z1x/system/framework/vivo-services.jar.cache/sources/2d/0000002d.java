package com.android.server;

import android.hardware.health.V1_0.HealthInfo;

/* loaded from: classes.dex */
public class VivoBatteryServiceImpl implements IVivoBatteryService {
    private static final int BATTERY_LOW_LEVEL_STATE = 0;
    private static final String TAG = "VivoBatteryService";
    private BatteryService mBatteryService;

    public VivoBatteryServiceImpl(BatteryService batteryService) {
        this.mBatteryService = batteryService;
        batteryService.mLed.setBatteryService(batteryService);
    }

    public void processValuesLocked() {
        this.mBatteryService.mLed.updateLightsLocked(0);
    }

    public HealthInfo getHealthInfo() {
        return this.mBatteryService.mHealthInfo;
    }

    public int getCriticalBatteryLevel() {
        return this.mBatteryService.mCriticalBatteryLevel;
    }

    public void dummy() {
    }
}