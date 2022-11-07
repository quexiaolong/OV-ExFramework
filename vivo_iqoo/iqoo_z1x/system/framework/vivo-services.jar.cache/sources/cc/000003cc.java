package com.android.server.policy;

import android.os.IBinder;

/* loaded from: classes.dex */
public interface IVivoWindowListener {
    IBinder getWindowToken();

    void notifyWindowsDrawn(IBinder iBinder);

    void onFinishScreenTurningOn();
}