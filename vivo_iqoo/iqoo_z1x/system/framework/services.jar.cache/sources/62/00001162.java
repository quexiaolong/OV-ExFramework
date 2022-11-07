package com.android.server.location.gnss;

import android.location.GnssMeasurementCorrections;
import android.os.Handler;
import android.util.Log;

/* loaded from: classes.dex */
public class GnssMeasurementCorrectionsProvider {
    static final int CAPABILITY_EXCESS_PATH_LENGTH = 2;
    static final int CAPABILITY_LOS_SATS = 1;
    static final int CAPABILITY_REFLECTING_PLANE = 4;
    private static final int INVALID_CAPABILITIES = Integer.MIN_VALUE;
    private static final String TAG = "GnssMeasurementCorrectionsProvider";
    private volatile int mCapabilities;
    private final Handler mHandler;
    private final GnssMeasurementCorrectionsProviderNative mNative;

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_inject_gnss_measurement_corrections(GnssMeasurementCorrections gnssMeasurementCorrections);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_is_measurement_corrections_supported();

    /* JADX INFO: Access modifiers changed from: package-private */
    public GnssMeasurementCorrectionsProvider(Handler handler) {
        this(handler, new GnssMeasurementCorrectionsProviderNative());
    }

    GnssMeasurementCorrectionsProvider(Handler handler, GnssMeasurementCorrectionsProviderNative aNative) {
        this.mCapabilities = Integer.MIN_VALUE;
        this.mHandler = handler;
        this.mNative = aNative;
    }

    public boolean isAvailableInPlatform() {
        return this.mNative.isMeasurementCorrectionsSupported();
    }

    public void injectGnssMeasurementCorrections(final GnssMeasurementCorrections measurementCorrections) {
        if (!isCapabilitiesReceived()) {
            Log.w(TAG, "Failed to inject GNSS measurement corrections. Capabilities not received yet.");
        } else {
            this.mHandler.post(new Runnable() { // from class: com.android.server.location.gnss.-$$Lambda$GnssMeasurementCorrectionsProvider$d4q-3xaRMcxrHCXpK2KTUwTmahY
                @Override // java.lang.Runnable
                public final void run() {
                    GnssMeasurementCorrectionsProvider.this.lambda$injectGnssMeasurementCorrections$0$GnssMeasurementCorrectionsProvider(measurementCorrections);
                }
            });
        }
    }

    public /* synthetic */ void lambda$injectGnssMeasurementCorrections$0$GnssMeasurementCorrectionsProvider(GnssMeasurementCorrections measurementCorrections) {
        if (!this.mNative.injectGnssMeasurementCorrections(measurementCorrections)) {
            Log.e(TAG, "Failure in injecting GNSS corrections.");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onCapabilitiesUpdated(int capabilities) {
        if (hasCapability(capabilities, 1) || hasCapability(capabilities, 2)) {
            this.mCapabilities = capabilities;
            return true;
        }
        Log.e(TAG, "Failed to set capabilities. Received capabilities 0x" + Integer.toHexString(capabilities) + " does not contain the mandatory LOS_SATS or the EXCESS_PATH_LENGTH capability.");
        return false;
    }

    int getCapabilities() {
        return this.mCapabilities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String toStringCapabilities() {
        int capabilities = getCapabilities();
        StringBuilder s = new StringBuilder();
        s.append("mCapabilities=0x");
        s.append(Integer.toHexString(capabilities));
        s.append(" ( ");
        if (hasCapability(capabilities, 1)) {
            s.append("LOS_SATS ");
        }
        if (hasCapability(capabilities, 2)) {
            s.append("EXCESS_PATH_LENGTH ");
        }
        if (hasCapability(capabilities, 4)) {
            s.append("REFLECTING_PLANE ");
        }
        s.append(")");
        return s.toString();
    }

    private static boolean hasCapability(int halCapabilities, int capability) {
        return (halCapabilities & capability) != 0;
    }

    private boolean isCapabilitiesReceived() {
        return this.mCapabilities != Integer.MIN_VALUE;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class GnssMeasurementCorrectionsProviderNative {
        GnssMeasurementCorrectionsProviderNative() {
        }

        public boolean isMeasurementCorrectionsSupported() {
            return GnssMeasurementCorrectionsProvider.native_is_measurement_corrections_supported();
        }

        public boolean injectGnssMeasurementCorrections(GnssMeasurementCorrections measurementCorrections) {
            return GnssMeasurementCorrectionsProvider.native_inject_gnss_measurement_corrections(measurementCorrections);
        }
    }
}