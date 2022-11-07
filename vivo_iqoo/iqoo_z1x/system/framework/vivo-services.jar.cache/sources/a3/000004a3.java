package com.android.server.power;

/* loaded from: classes.dex */
public class VivoWakeLockImpl implements IVivoWakeLock {
    public int mDisplayId;
    public boolean mShouldKeepScreenOn = true;

    public VivoWakeLockImpl(int displayId) {
        this.mDisplayId = 0;
        this.mDisplayId = displayId;
    }

    public void toString(StringBuilder sb) {
        if (!this.mShouldKeepScreenOn) {
            sb.append(" mShouldKeepScreenOn=false");
        }
    }

    public void setShouldKeepScreenOn(boolean shouldKeepScreenOn) {
        this.mShouldKeepScreenOn = shouldKeepScreenOn;
    }

    public boolean isShouldKeepScreenOn() {
        return this.mShouldKeepScreenOn;
    }

    public int getDisplayId() {
        return this.mDisplayId;
    }

    public void dummy() {
    }
}