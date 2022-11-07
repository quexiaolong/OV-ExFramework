package com.android.server.location.gnss;

import java.util.Arrays;

/* loaded from: classes.dex */
public class GnssPositionMode {
    private final boolean lowPowerMode;
    private final int minInterval;
    private final int mode;
    private final int preferredAccuracy;
    private final int preferredTime;
    private final int recurrence;

    public GnssPositionMode(int mode, int recurrence, int minInterval, int preferredAccuracy, int preferredTime, boolean lowPowerMode) {
        this.mode = mode;
        this.recurrence = recurrence;
        this.minInterval = minInterval;
        this.preferredAccuracy = preferredAccuracy;
        this.preferredTime = preferredTime;
        this.lowPowerMode = lowPowerMode;
    }

    public boolean equals(Object other) {
        if (other instanceof GnssPositionMode) {
            GnssPositionMode that = (GnssPositionMode) other;
            return this.mode == that.mode && this.recurrence == that.recurrence && this.minInterval == that.minInterval && this.preferredAccuracy == that.preferredAccuracy && this.preferredTime == that.preferredTime && this.lowPowerMode == that.lowPowerMode && getClass() == that.getClass();
        }
        return false;
    }

    public int hashCode() {
        return Arrays.hashCode(new Object[]{Integer.valueOf(this.mode), Integer.valueOf(this.recurrence), Integer.valueOf(this.minInterval), Integer.valueOf(this.preferredAccuracy), Integer.valueOf(this.preferredTime), Boolean.valueOf(this.lowPowerMode), getClass()});
    }
}