package com.android.server.timezonedetector;

import android.app.timezonedetector.ManualTimeZoneSuggestion;
import android.app.timezonedetector.TelephonyTimeZoneSuggestion;
import java.io.PrintWriter;

/* loaded from: classes2.dex */
public interface TimeZoneDetectorStrategy {
    void dump(PrintWriter printWriter, String[] strArr);

    void handleAutoTimeZoneDetectionChanged();

    void suggestManualTimeZone(ManualTimeZoneSuggestion manualTimeZoneSuggestion);

    void suggestTelephonyTimeZone(TelephonyTimeZoneSuggestion telephonyTimeZoneSuggestion);
}