package com.android.server.location.gnss;

import android.location.IGpsGeofenceHardware;
import android.util.Log;
import android.util.SparseArray;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class GnssGeofenceProvider extends IGpsGeofenceHardware.Stub {
    private final SparseArray<GeofenceEntry> mGeofenceEntries;
    private final Object mLock;
    private final GnssGeofenceProviderNative mNative;
    private static final String TAG = "GnssGeofenceProvider";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_add_geofence(int i, double d, double d2, double d3, int i2, int i3, int i4, int i5);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_is_geofence_supported();

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_pause_geofence(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_remove_geofence(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_resume_geofence(int i, int i2);

    /* loaded from: classes.dex */
    private static class GeofenceEntry {
        public int geofenceId;
        public int lastTransition;
        public double latitude;
        public double longitude;
        public int monitorTransitions;
        public int notificationResponsiveness;
        public boolean paused;
        public double radius;
        public int unknownTimer;

        private GeofenceEntry() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GnssGeofenceProvider() {
        this(new GnssGeofenceProviderNative());
    }

    GnssGeofenceProvider(GnssGeofenceProviderNative gnssGeofenceProviderNative) {
        this.mLock = new Object();
        this.mGeofenceEntries = new SparseArray<>();
        this.mNative = gnssGeofenceProviderNative;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resumeIfStarted() {
        if (DEBUG) {
            Log.d(TAG, "resumeIfStarted");
        }
        synchronized (this.mLock) {
            for (int i = 0; i < this.mGeofenceEntries.size(); i++) {
                GeofenceEntry entry = this.mGeofenceEntries.valueAt(i);
                boolean added = this.mNative.addGeofence(entry.geofenceId, entry.latitude, entry.longitude, entry.radius, entry.lastTransition, entry.monitorTransitions, entry.notificationResponsiveness, entry.unknownTimer);
                if (added && entry.paused) {
                    this.mNative.pauseGeofence(entry.geofenceId);
                }
            }
        }
    }

    public boolean isHardwareGeofenceSupported() {
        boolean isGeofenceSupported;
        synchronized (this.mLock) {
            isGeofenceSupported = this.mNative.isGeofenceSupported();
        }
        return isGeofenceSupported;
    }

    public boolean addCircularHardwareGeofence(int geofenceId, double latitude, double longitude, double radius, int lastTransition, int monitorTransitions, int notificationResponsiveness, int unknownTimer) {
        synchronized (this.mLock) {
            try {
                try {
                    boolean added = this.mNative.addGeofence(geofenceId, latitude, longitude, radius, lastTransition, monitorTransitions, notificationResponsiveness, unknownTimer);
                    if (added) {
                        GeofenceEntry entry = new GeofenceEntry();
                        entry.geofenceId = geofenceId;
                        try {
                            entry.latitude = latitude;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                        try {
                            entry.longitude = longitude;
                            try {
                                entry.radius = radius;
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                            try {
                                entry.lastTransition = lastTransition;
                            } catch (Throwable th3) {
                                th = th3;
                                throw th;
                            }
                            try {
                                entry.monitorTransitions = monitorTransitions;
                            } catch (Throwable th4) {
                                th = th4;
                                throw th;
                            }
                            try {
                                entry.notificationResponsiveness = notificationResponsiveness;
                                entry.unknownTimer = unknownTimer;
                                this.mGeofenceEntries.put(geofenceId, entry);
                            } catch (Throwable th5) {
                                th = th5;
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            throw th;
                        }
                    }
                    return added;
                } catch (Throwable th7) {
                    th = th7;
                }
            } catch (Throwable th8) {
                th = th8;
            }
        }
    }

    public boolean removeHardwareGeofence(int geofenceId) {
        boolean removed;
        synchronized (this.mLock) {
            removed = this.mNative.removeGeofence(geofenceId);
            if (removed) {
                this.mGeofenceEntries.remove(geofenceId);
            }
        }
        return removed;
    }

    public boolean pauseHardwareGeofence(int geofenceId) {
        boolean paused;
        GeofenceEntry entry;
        synchronized (this.mLock) {
            paused = this.mNative.pauseGeofence(geofenceId);
            if (paused && (entry = this.mGeofenceEntries.get(geofenceId)) != null) {
                entry.paused = true;
            }
        }
        return paused;
    }

    public boolean resumeHardwareGeofence(int geofenceId, int monitorTransitions) {
        boolean resumed;
        GeofenceEntry entry;
        synchronized (this.mLock) {
            resumed = this.mNative.resumeGeofence(geofenceId, monitorTransitions);
            if (resumed && (entry = this.mGeofenceEntries.get(geofenceId)) != null) {
                entry.paused = false;
                entry.monitorTransitions = monitorTransitions;
            }
        }
        return resumed;
    }

    /* loaded from: classes.dex */
    static class GnssGeofenceProviderNative {
        GnssGeofenceProviderNative() {
        }

        public boolean isGeofenceSupported() {
            return GnssGeofenceProvider.native_is_geofence_supported();
        }

        public boolean addGeofence(int geofenceId, double latitude, double longitude, double radius, int lastTransition, int monitorTransitions, int notificationResponsiveness, int unknownTimer) {
            return GnssGeofenceProvider.native_add_geofence(geofenceId, latitude, longitude, radius, lastTransition, monitorTransitions, notificationResponsiveness, unknownTimer);
        }

        public boolean removeGeofence(int geofenceId) {
            return GnssGeofenceProvider.native_remove_geofence(geofenceId);
        }

        public boolean resumeGeofence(int geofenceId, int transitions) {
            return GnssGeofenceProvider.native_resume_geofence(geofenceId, transitions);
        }

        public boolean pauseGeofence(int geofenceId) {
            return GnssGeofenceProvider.native_pause_geofence(geofenceId);
        }
    }
}