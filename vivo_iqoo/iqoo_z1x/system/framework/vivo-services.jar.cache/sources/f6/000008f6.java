package vendor.pixelworks.hardware.display.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class Vendor2Config {
    public static final int BYPASS_MODE = 520;
    public static final int CALIBRATION = 518;
    public static final int CM_COLOR_GAMUT = 516;
    public static final int CM_COLOR_TEMP_MODE = 515;
    public static final int COLOR_TEMP_VALUE = 517;
    public static final int DATA_GAIN = 512;
    public static final int DISPLAY_BRIGHTNESS = 514;
    public static final int FORCE_LUT = 513;
    public static final int PQ_TARGET = 519;
    public static final int TYPE_MAX = 521;

    public static final String toString(int o) {
        if (o == 512) {
            return "DATA_GAIN";
        }
        if (o == 513) {
            return "FORCE_LUT";
        }
        if (o == 514) {
            return "DISPLAY_BRIGHTNESS";
        }
        if (o == 515) {
            return "CM_COLOR_TEMP_MODE";
        }
        if (o == 516) {
            return "CM_COLOR_GAMUT";
        }
        if (o == 517) {
            return "COLOR_TEMP_VALUE";
        }
        if (o == 518) {
            return "CALIBRATION";
        }
        if (o == 519) {
            return "PQ_TARGET";
        }
        if (o == 520) {
            return "BYPASS_MODE";
        }
        if (o == 521) {
            return "TYPE_MAX";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 512) == 512) {
            list.add("DATA_GAIN");
            flipped = 0 | 512;
        }
        if ((o & 513) == 513) {
            list.add("FORCE_LUT");
            flipped |= 513;
        }
        if ((o & DISPLAY_BRIGHTNESS) == 514) {
            list.add("DISPLAY_BRIGHTNESS");
            flipped |= DISPLAY_BRIGHTNESS;
        }
        if ((o & CM_COLOR_TEMP_MODE) == 515) {
            list.add("CM_COLOR_TEMP_MODE");
            flipped |= CM_COLOR_TEMP_MODE;
        }
        if ((o & CM_COLOR_GAMUT) == 516) {
            list.add("CM_COLOR_GAMUT");
            flipped |= CM_COLOR_GAMUT;
        }
        if ((o & COLOR_TEMP_VALUE) == 517) {
            list.add("COLOR_TEMP_VALUE");
            flipped |= COLOR_TEMP_VALUE;
        }
        if ((o & CALIBRATION) == 518) {
            list.add("CALIBRATION");
            flipped |= CALIBRATION;
        }
        if ((o & PQ_TARGET) == 519) {
            list.add("PQ_TARGET");
            flipped |= PQ_TARGET;
        }
        if ((o & BYPASS_MODE) == 520) {
            list.add("BYPASS_MODE");
            flipped |= BYPASS_MODE;
        }
        if ((o & TYPE_MAX) == 521) {
            list.add("TYPE_MAX");
            flipped |= TYPE_MAX;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}