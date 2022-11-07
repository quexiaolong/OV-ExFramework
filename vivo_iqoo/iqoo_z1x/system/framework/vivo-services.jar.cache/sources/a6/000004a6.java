package com.android.server.timedetector;

import android.app.timedetector.NetworkTimeSuggestion;
import android.app.timedetector.TelephonyTimeSuggestion;
import android.os.TimestampedValue;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoTimeDetectorStrategyImpl implements IVivoTimeDetectorStrategy {
    private static final long MAX_DIFF = 86400000;
    private static final int ORIGIN_NETWORK = 3;
    private static final int ORIGIN_TELEPHONY = 1;
    private static final String TAG = "VivoTimeDetector";
    private TimeDetectorStrategyImpl mStrategy;

    public VivoTimeDetectorStrategyImpl(TimeDetectorStrategyImpl timeDetectorStrategy) {
        this.mStrategy = timeDetectorStrategy;
    }

    public void doAutoTimeDetection(String detectionReason) {
        NetworkTimeSuggestion networkSuggestion = this.mStrategy.findLatestValidNetworkSuggestion();
        TelephonyTimeSuggestion telephonySuggestion = this.mStrategy.findBestTelephonySuggestion();
        if (networkSuggestion != null && shouldUseNetworkTime(networkSuggestion, telephonySuggestion)) {
            TimestampedValue<Long> newUtcTime = networkSuggestion.getUtcTime();
            String cause = "Found good network suggestion., networkSuggestion=" + networkSuggestion + ", detectionReason=" + detectionReason;
            this.mStrategy.setSystemClockIfRequired(3, newUtcTime, cause);
        } else if (telephonySuggestion != null) {
            TimestampedValue<Long> newUtcTime2 = telephonySuggestion.getUtcTime();
            String cause2 = "Found good telephony suggestion., bestTelephonySuggestion=" + telephonySuggestion + ", detectionReason=" + detectionReason;
            this.mStrategy.setSystemClockIfRequired(1, newUtcTime2, cause2);
        } else {
            VSlog.d(TAG, "Could not determine time: No best telephony or network suggestion. detectionReason=" + detectionReason);
        }
    }

    private boolean shouldUseNetworkTime(NetworkTimeSuggestion networkSuggestion, TelephonyTimeSuggestion telephonySuggestion) {
        long networkTime;
        long telephonyTime;
        if (networkSuggestion == null) {
            networkTime = 0;
        } else {
            networkTime = ((Long) networkSuggestion.getUtcTime().getValue()).longValue() + networkSuggestion.getUtcTime().getReferenceTimeMillis();
        }
        if (telephonySuggestion == null) {
            telephonyTime = 0;
        } else {
            telephonyTime = ((Long) telephonySuggestion.getUtcTime().getValue()).longValue() + telephonySuggestion.getUtcTime().getReferenceTimeMillis();
        }
        VSlog.d(TAG, String.format("network time: %d, telephony time: %d", Long.valueOf(networkTime), Long.valueOf(telephonyTime)));
        if (networkTime != 0 && telephonyTime != 0 && Math.abs(networkTime - telephonyTime) > 86400000) {
            VSlog.i(TAG, "choose telephony time");
            return false;
        }
        VSlog.i(TAG, "choose network time");
        return true;
    }
}