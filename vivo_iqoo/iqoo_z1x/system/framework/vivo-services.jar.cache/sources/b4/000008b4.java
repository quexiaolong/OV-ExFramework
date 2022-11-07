package com.vivo.services.vgc.cbs;

import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.vivo.face.common.data.Constants;
import com.vivo.services.vgc.VgcUtils;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class CbsUtils {
    public static List<String> defSimInfoList;
    public static boolean DEBUG = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
    public static final String TAG = VivoCbsService.class.getSimpleName();
    public static int STRATEGY_DEFAULT_DATA_CARD_PREFER = 0;
    public static int STRATEGY_DEFAULT_DATA_CARD_ONLY = 1;
    public static int CONFIG_TRIGGER_STRATEGY = SystemProperties.getInt("ro.vivo.simtrigger.strategy", 0);
    public static final String CARRIER_DIR = SystemProperties.get("ro.vgc.config.carrierdir", "/vgc/cust/carrier/");
    public static final String REGION = SystemProperties.get("ro.product.country.region", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
    public static final String DEF_CARRIER_SIMINFO = SystemProperties.get("ro.vgc.config.regiondir", "/vgc/cust/region/") + REGION + "/carrier_siminfo.xml";

    static {
        ArrayList arrayList = new ArrayList();
        defSimInfoList = arrayList;
        arrayList.add(VgcUtils.VIVO_DIR + "carrier_siminfo.xml");
        defSimInfoList.add(DEF_CARRIER_SIMINFO);
        defSimInfoList.add(VgcUtils.VGC_PROJECT_ROOT_DIR + "cust/vivo/carrier_siminfo.xml");
        defSimInfoList.add(VgcUtils.VGC_COMMON_ROOT_DIR + "cust/vivo/carrier_siminfo.xml");
    }

    public static boolean isSimInsertinSlot(TelephonyManager tm, int slotIdx) {
        int simState = tm.getSimState(slotIdx);
        return (simState == 1 || simState == 0) ? false : true;
    }

    public static int getNextPhoneId(int currPhoneId) {
        return (currPhoneId + 1) % TelephonyManager.getDefault().getSimCount();
    }

    public static int parseInt(String s, int radix, int def) {
        try {
            return Integer.parseInt(s, radix);
        } catch (Exception e) {
            return def;
        }
    }

    public static int parseInt(String s) {
        return parseInt(s, 10, 0);
    }

    public static int parseInt(String s, int def) {
        return parseInt(s, 10, def);
    }

    public static String getDiscern() {
        String iccid = SystemProperties.get("persist.radio.sim1.iccid", SystemProperties.get("persist.radio.sim2.iccid", (String) null));
        if (TextUtils.isEmpty(iccid)) {
            iccid = SystemProperties.get("persist.radio.sim1.discern", SystemProperties.get("persist.radio.sim2.discern", (String) null));
        }
        if (TextUtils.isEmpty(iccid)) {
            return SystemProperties.get("gsm.sim.preiccid_0", SystemProperties.get("gsm.sim.preiccid_1", (String) null));
        }
        return iccid;
    }
}