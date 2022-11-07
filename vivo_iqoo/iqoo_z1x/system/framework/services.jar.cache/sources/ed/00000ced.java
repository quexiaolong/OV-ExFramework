package com.android.server.devicepolicy;

import android.app.admin.DeviceStateCache;
import com.android.internal.util.IndentingPrintWriter;

/* loaded from: classes.dex */
public class DeviceStateCacheImpl extends DeviceStateCache {
    private final Object mLock = new Object();
    private boolean mIsDeviceProvisioned = false;

    public boolean isDeviceProvisioned() {
        return this.mIsDeviceProvisioned;
    }

    public void setDeviceProvisioned(boolean provisioned) {
        synchronized (this.mLock) {
            this.mIsDeviceProvisioned = provisioned;
        }
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("Device state cache:");
        pw.increaseIndent();
        pw.println("Device provisioned: " + this.mIsDeviceProvisioned);
        pw.decreaseIndent();
    }
}