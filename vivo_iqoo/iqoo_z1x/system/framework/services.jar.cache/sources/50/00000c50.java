package com.android.server.deviceidle;

/* loaded from: classes.dex */
public class DeviceIdleConstraintTracker {
    public final int minState;
    public final String name;
    public boolean active = false;
    public boolean monitoring = false;

    public DeviceIdleConstraintTracker(String name, int minState) {
        this.name = name;
        this.minState = minState;
    }
}