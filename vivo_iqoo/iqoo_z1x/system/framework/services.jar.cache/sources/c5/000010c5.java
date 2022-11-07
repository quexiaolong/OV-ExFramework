package com.android.server.location;

import android.app.PendingIntent;
import android.location.Geofence;
import android.location.Location;

/* loaded from: classes.dex */
public class GeofenceState {
    public static final int FLAG_ENTER = 1;
    public static final int FLAG_EXIT = 2;
    private static final int STATE_INSIDE = 1;
    private static final int STATE_OUTSIDE = 2;
    private static final int STATE_UNKNOWN = 0;
    public final long mExpireAt;
    public final Geofence mFence;
    public final CallerIdentity mIdentity;
    public final PendingIntent mIntent;
    private final Location mLocation;
    int mState = 0;
    double mDistanceToCenter = Double.MAX_VALUE;

    public GeofenceState(Geofence fence, long expireAt, CallerIdentity identity, PendingIntent intent) {
        this.mFence = fence;
        this.mExpireAt = expireAt;
        this.mIdentity = identity;
        this.mIntent = intent;
        Location location = new Location("");
        this.mLocation = location;
        location.setLatitude(fence.getLatitude());
        this.mLocation.setLongitude(fence.getLongitude());
    }

    public int processLocation(Location location) {
        double distanceTo = this.mLocation.distanceTo(location);
        this.mDistanceToCenter = distanceTo;
        int prevState = this.mState;
        boolean inside = distanceTo <= ((double) Math.max(this.mFence.getRadius(), location.getAccuracy()));
        if (inside) {
            this.mState = 1;
            if (prevState != 1) {
                return 1;
            }
        } else {
            this.mState = 2;
            if (prevState == 1) {
                return 2;
            }
        }
        return 0;
    }

    public double getDistanceToBoundary() {
        if (Double.compare(this.mDistanceToCenter, Double.MAX_VALUE) == 0) {
            return Double.MAX_VALUE;
        }
        return Math.abs(this.mFence.getRadius() - this.mDistanceToCenter);
    }

    public String toString() {
        String state;
        int i = this.mState;
        if (i == 1) {
            state = "IN";
        } else if (i == 2) {
            state = "OUT";
        } else {
            state = "?";
        }
        return String.format("%s d=%.0f %s", this.mFence.toString(), Double.valueOf(this.mDistanceToCenter), state);
    }
}