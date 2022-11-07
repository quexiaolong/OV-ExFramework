package com.android.server.location;

import android.location.Location;
import android.os.SystemClock;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Random;

/* loaded from: classes.dex */
public class LocationFudger {
    private static final int APPROXIMATE_METERS_PER_DEGREE_AT_EQUATOR = 111000;
    private static final double CHANGE_PER_INTERVAL = 0.03d;
    private static final double MAX_LATITUDE = 89.999990990991d;
    private static final float MIN_ACCURACY_M = 200.0f;
    private static final double NEW_WEIGHT = 0.03d;
    static final long OFFSET_UPDATE_INTERVAL_MS = 3600000;
    private static final double OLD_WEIGHT = Math.sqrt(0.9991d);
    private final float mAccuracyM;
    private Location mCachedCoarseLocation;
    private Location mCachedFineLocation;
    private final Clock mClock;
    private double mLatitudeOffsetM;
    private double mLongitudeOffsetM;
    private long mNextUpdateRealtimeMs;
    private final Random mRandom;

    public LocationFudger(float accuracyM) {
        this(accuracyM, SystemClock.elapsedRealtimeClock(), new SecureRandom());
    }

    LocationFudger(float accuracyM, Clock clock, Random random) {
        this.mClock = clock;
        this.mRandom = random;
        this.mAccuracyM = Math.max(accuracyM, (float) MIN_ACCURACY_M);
        resetOffsets();
    }

    public void resetOffsets() {
        this.mLatitudeOffsetM = nextRandomOffset();
        this.mLongitudeOffsetM = nextRandomOffset();
        this.mNextUpdateRealtimeMs = this.mClock.millis() + 3600000;
    }

    public Location createCoarse(Location fine) {
        synchronized (this) {
            if (fine == this.mCachedFineLocation) {
                return new Location(this.mCachedCoarseLocation);
            }
            updateOffsets();
            Location coarse = new Location(fine);
            coarse.removeBearing();
            coarse.removeSpeed();
            coarse.removeAltitude();
            coarse.setExtras(null);
            double latitude = wrapLatitude(coarse.getLatitude());
            double longitude = wrapLongitude(coarse.getLongitude());
            double longitude2 = longitude + wrapLongitude(metersToDegreesLongitude(this.mLongitudeOffsetM, latitude));
            double latitude2 = latitude + wrapLatitude(metersToDegreesLatitude(this.mLatitudeOffsetM));
            double latGranularity = metersToDegreesLatitude(this.mAccuracyM);
            double latitude3 = wrapLatitude(Math.round(latitude2 / latGranularity) * latGranularity);
            double lonGranularity = metersToDegreesLongitude(this.mAccuracyM, latitude3);
            double longitude3 = wrapLongitude(Math.round(longitude2 / lonGranularity) * lonGranularity);
            coarse.setLatitude(latitude3);
            coarse.setLongitude(longitude3);
            coarse.setAccuracy(Math.max(this.mAccuracyM, coarse.getAccuracy()));
            synchronized (this) {
                this.mCachedFineLocation = fine;
                this.mCachedCoarseLocation = coarse;
            }
            return new Location(this.mCachedCoarseLocation);
        }
    }

    private synchronized void updateOffsets() {
        long now = this.mClock.millis();
        if (now < this.mNextUpdateRealtimeMs) {
            return;
        }
        this.mLatitudeOffsetM = (OLD_WEIGHT * this.mLatitudeOffsetM) + (nextRandomOffset() * 0.03d);
        this.mLongitudeOffsetM = (OLD_WEIGHT * this.mLongitudeOffsetM) + (nextRandomOffset() * 0.03d);
        this.mNextUpdateRealtimeMs = 3600000 + now;
    }

    private double nextRandomOffset() {
        return this.mRandom.nextGaussian() * (this.mAccuracyM / 4.0d);
    }

    private static double wrapLatitude(double lat) {
        if (lat > MAX_LATITUDE) {
            lat = MAX_LATITUDE;
        }
        if (lat < -89.999990990991d) {
            return -89.999990990991d;
        }
        return lat;
    }

    private static double wrapLongitude(double lon) {
        double lon2 = lon % 360.0d;
        if (lon2 >= 180.0d) {
            lon2 -= 360.0d;
        }
        if (lon2 < -180.0d) {
            return lon2 + 360.0d;
        }
        return lon2;
    }

    private static double metersToDegreesLatitude(double distance) {
        return distance / 111000.0d;
    }

    private static double metersToDegreesLongitude(double distance, double lat) {
        return (distance / 111000.0d) / Math.cos(Math.toRadians(lat));
    }
}