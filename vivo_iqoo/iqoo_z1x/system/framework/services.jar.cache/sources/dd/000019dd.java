package com.android.server.timedetector;

import android.app.timedetector.ManualTimeSuggestion;
import android.app.timedetector.NetworkTimeSuggestion;
import android.app.timedetector.TelephonyTimeSuggestion;
import android.os.TimestampedValue;
import java.io.PrintWriter;

/* loaded from: classes2.dex */
public interface TimeDetectorStrategy {

    /* loaded from: classes2.dex */
    public interface Callback {
        void acquireWakeLock();

        long elapsedRealtimeMillis();

        boolean isAutoTimeDetectionEnabled();

        void releaseWakeLock();

        void setSystemClock(long j);

        long systemClockMillis();

        int systemClockUpdateThresholdMillis();
    }

    void dump(PrintWriter printWriter, String[] strArr);

    void handleAutoTimeDetectionChanged();

    void initialize(Callback callback);

    void suggestManualTime(ManualTimeSuggestion manualTimeSuggestion);

    void suggestNetworkTime(NetworkTimeSuggestion networkTimeSuggestion);

    void suggestTelephonyTime(TelephonyTimeSuggestion telephonyTimeSuggestion);

    static long getTimeAt(TimestampedValue<Long> timeValue, long referenceClockMillisNow) {
        return (referenceClockMillisNow - timeValue.getReferenceTimeMillis()) + ((Long) timeValue.getValue()).longValue();
    }
}