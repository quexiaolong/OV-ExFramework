package com.vivo.services.memc;

import com.vivo.common.utils.VLog;
import com.vivo.vcodetransbase.EventTransfer;
import java.util.Map;

/* loaded from: classes.dex */
public class DataReport {
    public static final int CUR_MODEL = 100;
    public static final String MODULE_ID = "F339";
    public static final String REPORT_MAIN_SWITCH_STATE_ID = "F339|10001";
    public static final String REPORT_MAIN_SWITCH_STATE_ID_KEY = "sw_st";
    private static final String TAG = "DataReport";

    public static void reportMainSwitchState(Map<String, String> map) {
        VLog.d(TAG, "run: report state map = " + map);
        EventTransfer.getInstance().singleEvent("F339", REPORT_MAIN_SWITCH_STATE_ID, System.currentTimeMillis(), 0L, map);
    }
}