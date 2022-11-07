package com.android.server.policy.key;

import android.content.Context;
import android.os.FtBuild;
import com.android.server.policy.VivoPolicyConstant;
import com.vivo.common.VivoCollectData;
import java.util.HashMap;

/* loaded from: classes.dex */
public class VDC_KEY_J_1 {
    private static final String EVENT_ID = "142";
    private static final String EVENT_JOVI = "jovi";
    private static final String EVENT_LABEL = "1421";
    private static final String EVENT_LABEL_POWER_KEY = "14210";
    private static final String EVENT_LABEL_TOP_POWER_KEY = "14211";
    private static final String EVENT_PARAMS = "button_press";
    private static final String EVENT_POWER = "power";
    private static final String EVENT_POWER_WAKE_UP = "power_screen_push";
    private static final String EVENT_TOP_POWER_WAKE_UP = "top_button_screen_push";
    private static final String EVENT_VOLUME_DOWN = "vol_down";
    private static final String EVENT_VOLUME_UP = "vol_up";
    private Context mContext;
    private HashMap<Integer, String> mKeyCodeEventMap = new HashMap<>();

    public VDC_KEY_J_1(Context context) {
        this.mContext = context;
        initKeyCodeEventMap();
    }

    private void initKeyCodeEventMap() {
        this.mKeyCodeEventMap.put(26, EVENT_POWER);
        this.mKeyCodeEventMap.put(24, EVENT_VOLUME_UP);
        this.mKeyCodeEventMap.put(25, EVENT_VOLUME_DOWN);
        if (VivoPolicyConstant.OBJECT_AIKEY != null) {
            this.mKeyCodeEventMap.put(Integer.valueOf(VivoPolicyConstant.KEYCODE_AI), EVENT_JOVI);
        }
    }

    public void VDC_Key_F_1(int keycode) {
        String paramValue;
        if (!FtBuild.isOverSeas() && (paramValue = this.mKeyCodeEventMap.get(Integer.valueOf(keycode))) != null) {
            VivoCollectData collectData = VivoCollectData.getInstance(this.mContext);
            HashMap<String, String> params = new HashMap<>();
            params.put(EVENT_PARAMS, paramValue);
            collectData.writeData(EVENT_ID, EVENT_LABEL, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params);
        }
    }

    public static void VDC_Key_F_2(Context context, boolean isTopPowerKey, boolean wakeUp) {
        if (FtBuild.isOverSeas()) {
            return;
        }
        VivoCollectData collectData = VivoCollectData.getInstance(context);
        HashMap<String, String> params = new HashMap<>();
        params.put(isTopPowerKey ? EVENT_TOP_POWER_WAKE_UP : EVENT_POWER_WAKE_UP, wakeUp ? "1" : "0");
        collectData.writeData(EVENT_ID, isTopPowerKey ? EVENT_LABEL_TOP_POWER_KEY : EVENT_LABEL_POWER_KEY, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params);
    }
}