package com.vivo.services.vivolight;

import com.vivo.common.utils.VLog;
import vendor.vivo.hardware.dualleds.V1_0.IDualleds;

/* loaded from: classes.dex */
public class DualLightHalWrapper {
    private static final String TAG = DualLightHalWrapper.class.getSimpleName();
    private static IDualleds sRingHalInstance = null;

    private static IDualleds getRingLightHalService() {
        if (sRingHalInstance == null) {
            try {
                sRingHalInstance = IDualleds.getService();
            } catch (Exception e) {
                sRingHalInstance = null;
                e.printStackTrace();
            }
        }
        return sRingHalInstance;
    }

    public static int setMode(int type, int initBrightness) {
        String str = TAG;
        VLog.d(str, "setMode type = " + type);
        IDualleds temp = getRingLightHalService();
        if (temp == null) {
            return -1;
        }
        try {
            return temp.setMode(type, initBrightness);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int setBrightness(int value) {
        String str = TAG;
        VLog.d(str, "setBrightness value = " + value);
        IDualleds temp = getRingLightHalService();
        if (temp == null) {
            return -1;
        }
        try {
            return temp.setBrightness(value);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int startLed(int flag) {
        String str = TAG;
        VLog.d(str, "startLed flag = " + flag);
        IDualleds temp = getRingLightHalService();
        if (temp == null) {
            return -1;
        }
        try {
            return temp.startLed(flag);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int dualledsExists() {
        VLog.d(TAG, "dualledsExists");
        IDualleds temp = getRingLightHalService();
        if (temp == null) {
            return -1;
        }
        try {
            return temp.dualledsExists();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}