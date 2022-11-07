package com.vivo.face.internal.wrapper;

import android.content.Context;
import android.os.PowerManager;

/* loaded from: classes.dex */
public final class PowerManagerWrapper {
    private static final String TAG = "PowerManagerWrapper";
    private Context mContext;
    private PowerManager.WakeLock mDrawWakeLock;
    private PowerManager mPowerManager;

    public PowerManagerWrapper(Context context) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
    }

    public void acquire() {
        if (this.mDrawWakeLock == null) {
            PowerManager.WakeLock newWakeLock = this.mPowerManager.newWakeLock(128, "FaceUIDialog");
            this.mDrawWakeLock = newWakeLock;
            newWakeLock.setDisplayId(4096);
        }
        PowerManager.WakeLock wakeLock = this.mDrawWakeLock;
        if (wakeLock != null) {
            wakeLock.acquire(120L);
        }
    }

    public void acquire(int timeout) {
        if (this.mDrawWakeLock == null) {
            PowerManager.WakeLock newWakeLock = this.mPowerManager.newWakeLock(128, "FaceUIDialog");
            this.mDrawWakeLock = newWakeLock;
            newWakeLock.setDisplayId(4096);
        }
        PowerManager.WakeLock wakeLock = this.mDrawWakeLock;
        if (wakeLock != null) {
            wakeLock.acquire(timeout + 100);
        }
    }
}