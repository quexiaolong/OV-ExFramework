package com.vivo.services.perf.bigdata;

import android.os.SystemProperties;
import com.vivo.face.common.data.Constants;
import com.vivo.statistics.sdk.GatherManager;

/* loaded from: classes.dex */
public class PerfBigdata {
    public static final boolean isPerfBigdataEnable = SystemProperties.getBoolean("persist.rms.bigdata_enable", true);

    public static void onProcAnr(String packageName, String processName, String activity, String reason) {
        if (isPerfBigdataEnable) {
            long time = System.currentTimeMillis();
            GatherManager.getInstance().gather("anr", new Object[]{packageName, processName, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, -1, activity, Integer.valueOf(getAnrReason(reason)), Long.valueOf(time)});
        }
    }

    private static int getAnrReason(String reason) {
        if (reason == null) {
            return 0;
        }
        if (reason.contains("Input dispatching timed out")) {
            return 1;
        }
        if (reason.contains("Broadcast of Intent")) {
            return 2;
        }
        if (reason.contains("Executing service")) {
            return 3;
        }
        if (!reason.contains("ContentProvider not responding")) {
            return 0;
        }
        return 4;
    }
}