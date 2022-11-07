package com.android.server.twilight;

import android.text.format.DateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

/* loaded from: classes2.dex */
public final class TwilightState {
    private final long mSunriseTimeMillis;
    private final long mSunsetTimeMillis;

    public TwilightState(long sunriseTimeMillis, long sunsetTimeMillis) {
        this.mSunriseTimeMillis = sunriseTimeMillis;
        this.mSunsetTimeMillis = sunsetTimeMillis;
    }

    public long sunriseTimeMillis() {
        return this.mSunriseTimeMillis;
    }

    public LocalDateTime sunrise() {
        ZoneId zoneId = TimeZone.getDefault().toZoneId();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.mSunriseTimeMillis), zoneId);
    }

    public long sunsetTimeMillis() {
        return this.mSunsetTimeMillis;
    }

    public LocalDateTime sunset() {
        ZoneId zoneId = TimeZone.getDefault().toZoneId();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.mSunsetTimeMillis), zoneId);
    }

    public boolean isNight() {
        long now = System.currentTimeMillis();
        return now >= this.mSunsetTimeMillis && now < this.mSunriseTimeMillis;
    }

    public boolean equals(Object o) {
        return (o instanceof TwilightState) && equals((TwilightState) o);
    }

    public boolean equals(TwilightState other) {
        return other != null && this.mSunriseTimeMillis == other.mSunriseTimeMillis && this.mSunsetTimeMillis == other.mSunsetTimeMillis;
    }

    public int hashCode() {
        return Long.hashCode(this.mSunriseTimeMillis) ^ Long.hashCode(this.mSunsetTimeMillis);
    }

    public String toString() {
        return "TwilightState { sunrise=" + ((Object) DateFormat.format("MM-dd HH:mm", this.mSunriseTimeMillis)) + " sunset=" + ((Object) DateFormat.format("MM-dd HH:mm", this.mSunsetTimeMillis)) + " }";
    }
}