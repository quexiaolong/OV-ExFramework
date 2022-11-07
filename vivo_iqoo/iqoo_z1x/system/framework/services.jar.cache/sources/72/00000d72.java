package com.android.server.display;

/* loaded from: classes.dex */
public interface IVivoLocalDisplayDevice {
    void onSetBrightnessFinished(long j, float f);

    void onSetBrightnessStarted(long j, float f);

    void setForceStateChanged(boolean z, boolean z2);

    void setPowerModeForState(int i, long j);

    Runnable whetherForceStateChanged(int i, boolean z, boolean z2, long j, float f, boolean z3);
}