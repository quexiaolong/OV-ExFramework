package vendor.pixelworks.hardware.display.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class VendorConfig {
    public static final int CALIBRATION = 263;
    public static final int SET_CALI_PATTERN = 265;
    public static final int SET_GAME_MODE = 261;
    public static final int SET_HDR_FORMAL = 258;
    public static final int SET_HDR_SETTING = 260;
    public static final int SET_MEMC_SETTING = 266;
    public static final int SET_N2M_ENABLE = 262;
    public static final int SET_ORIENTATION = 257;
    public static final int SET_SDR2HDR_SETTING = 267;
    public static final int SET_SERVICE_DEBUG = 259;
    public static final int SET_WCG_GAMUT = 264;
    public static final int START_TRANSITION = 256;
    public static final int TYPE_MAX = 268;

    public static final String toString(int o) {
        if (o == 256) {
            return "START_TRANSITION";
        }
        if (o == 257) {
            return "SET_ORIENTATION";
        }
        if (o == 258) {
            return "SET_HDR_FORMAL";
        }
        if (o == 259) {
            return "SET_SERVICE_DEBUG";
        }
        if (o == 260) {
            return "SET_HDR_SETTING";
        }
        if (o == 261) {
            return "SET_GAME_MODE";
        }
        if (o == 262) {
            return "SET_N2M_ENABLE";
        }
        if (o == 263) {
            return "CALIBRATION";
        }
        if (o == 264) {
            return "SET_WCG_GAMUT";
        }
        if (o == 265) {
            return "SET_CALI_PATTERN";
        }
        if (o == 266) {
            return "SET_MEMC_SETTING";
        }
        if (o == 267) {
            return "SET_SDR2HDR_SETTING";
        }
        if (o == 268) {
            return "TYPE_MAX";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 256) == 256) {
            list.add("START_TRANSITION");
            flipped = 0 | 256;
        }
        if ((o & 257) == 257) {
            list.add("SET_ORIENTATION");
            flipped |= 257;
        }
        if ((o & 258) == 258) {
            list.add("SET_HDR_FORMAL");
            flipped |= 258;
        }
        if ((o & 259) == 259) {
            list.add("SET_SERVICE_DEBUG");
            flipped |= 259;
        }
        if ((o & 260) == 260) {
            list.add("SET_HDR_SETTING");
            flipped |= 260;
        }
        if ((o & SET_GAME_MODE) == 261) {
            list.add("SET_GAME_MODE");
            flipped |= SET_GAME_MODE;
        }
        if ((o & SET_N2M_ENABLE) == 262) {
            list.add("SET_N2M_ENABLE");
            flipped |= SET_N2M_ENABLE;
        }
        if ((o & CALIBRATION) == 263) {
            list.add("CALIBRATION");
            flipped |= CALIBRATION;
        }
        if ((o & SET_WCG_GAMUT) == 264) {
            list.add("SET_WCG_GAMUT");
            flipped |= SET_WCG_GAMUT;
        }
        if ((o & SET_CALI_PATTERN) == 265) {
            list.add("SET_CALI_PATTERN");
            flipped |= SET_CALI_PATTERN;
        }
        if ((o & SET_MEMC_SETTING) == 266) {
            list.add("SET_MEMC_SETTING");
            flipped |= SET_MEMC_SETTING;
        }
        if ((o & SET_SDR2HDR_SETTING) == 267) {
            list.add("SET_SDR2HDR_SETTING");
            flipped |= SET_SDR2HDR_SETTING;
        }
        if ((o & TYPE_MAX) == 268) {
            list.add("TYPE_MAX");
            flipped |= TYPE_MAX;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}