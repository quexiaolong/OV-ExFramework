package com.android.server.location.gnss;

import android.location.GnssCapabilities;
import android.util.Log;

/* loaded from: classes.dex */
public class GnssCapabilitiesProvider {
    private static final long GNSS_CAPABILITIES_SUB_HAL_MEASUREMENT_CORRECTIONS = 480;
    private static final long GNSS_CAPABILITIES_TOP_HAL = 31;
    private long mGnssCapabilities;
    private static final String TAG = "GnssCapabilitiesProvider";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    public long getGnssCapabilities() {
        long j;
        synchronized (this) {
            j = this.mGnssCapabilities;
        }
        return j;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTopHalCapabilities(int topHalCapabilities) {
        long gnssCapabilities = hasCapability(topHalCapabilities, 256) ? 0 | 1 : 0L;
        if (hasCapability(topHalCapabilities, 512)) {
            gnssCapabilities |= 2;
        }
        if (hasCapability(topHalCapabilities, 32)) {
            gnssCapabilities |= 4;
        }
        if (hasCapability(topHalCapabilities, 64)) {
            gnssCapabilities |= 8;
        }
        if (hasCapability(topHalCapabilities, 128)) {
            gnssCapabilities |= 16;
        }
        if (hasCapability(topHalCapabilities, 2048)) {
            gnssCapabilities |= 512;
        }
        synchronized (this) {
            long j = this.mGnssCapabilities & (-32);
            this.mGnssCapabilities = j;
            this.mGnssCapabilities = j | gnssCapabilities;
            if (DEBUG) {
                Log.d(TAG, "setTopHalCapabilities, mGnssCapabilities=0x" + Long.toHexString(this.mGnssCapabilities) + ", " + GnssCapabilities.of(this.mGnssCapabilities));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSubHalMeasurementCorrectionsCapabilities(int measurementCorrectionsCapabilities) {
        long gnssCapabilities = hasCapability(measurementCorrectionsCapabilities, 1) ? 32 | 64 : 32L;
        if (hasCapability(measurementCorrectionsCapabilities, 2)) {
            gnssCapabilities |= 128;
        }
        if (hasCapability(measurementCorrectionsCapabilities, 4)) {
            gnssCapabilities |= 256;
        }
        synchronized (this) {
            long j = this.mGnssCapabilities & (-481);
            this.mGnssCapabilities = j;
            this.mGnssCapabilities = j | gnssCapabilities;
            if (DEBUG) {
                Log.d(TAG, "setSubHalMeasurementCorrectionsCapabilities, mGnssCapabilities=0x" + Long.toHexString(this.mGnssCapabilities) + ", " + GnssCapabilities.of(this.mGnssCapabilities));
            }
        }
    }

    private static boolean hasCapability(int halCapabilities, int capability) {
        return (halCapabilities & capability) != 0;
    }
}