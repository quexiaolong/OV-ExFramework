package com.vivo.face.internal.wrapper;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.app.IBatteryStats;
import com.vivo.common.utils.VLog;
import com.vivo.framework.pem.VivoStats;

/* loaded from: classes.dex */
public final class VivoStatsWrapper {
    private static final String TAG = "Utils";

    public static int noteStats(int what, int arg1, int arg2) {
        VivoStats.note(what, arg1, arg2);
        IBinder batteryService = ServiceManager.getService("batterystats");
        if (batteryService == null) {
            VLog.w(TAG, "no battery service!");
            return -1;
        }
        IBatteryStats batteryStats = IBatteryStats.Stub.asInterface(batteryService);
        try {
            int ret = batteryStats.notePem(what, arg1, arg2);
            return ret;
        } catch (RemoteException e) {
            VLog.w(TAG, "notePem failed: ", e);
            return -1;
        }
    }
}