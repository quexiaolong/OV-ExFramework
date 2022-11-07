package com.vivo.services.superresolution;

import com.vivo.vcodetransbase.EventTransfer;
import java.util.Map;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class DataReport {
    public static final String MODULE_ID = "F294";
    public static final String REPORT_ACCU_TIME_ID = "F294|10005";
    public static final String REPORT_STATE_ID = "F294|10002";
    public static final String REPORT_SWITCH_OFF_ID = "F294|10004";
    private static final String TAG = "SR_DataReport";

    public static void reportState(Map<String, String> map) {
        VSlog.d(TAG, "run: report state map = " + map);
        EventTransfer.getInstance().singleEvent(MODULE_ID, REPORT_STATE_ID, System.currentTimeMillis(), 0L, map);
    }

    public static void reportSwitchOff(Map<String, String> map) {
        VSlog.d(TAG, "run: switch off map = " + map);
        EventTransfer.getInstance().singleEvent(MODULE_ID, REPORT_SWITCH_OFF_ID, System.currentTimeMillis(), 0L, map);
    }

    public static void reportAccuTime(Map<String, String> map) {
        VSlog.d(TAG, "run: accu time map = " + map);
        EventTransfer.getInstance().singleEvent(MODULE_ID, REPORT_ACCU_TIME_ID, System.currentTimeMillis(), 0L, map);
    }
}