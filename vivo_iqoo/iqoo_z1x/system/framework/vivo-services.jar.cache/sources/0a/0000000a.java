package android.hardware.graphics.common.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class ColorMode {
    public static final int ADOBE_RGB = 8;
    public static final int DCI_P3 = 6;
    public static final int DISPLAY_P3 = 9;
    public static final int NATIVE = 0;
    public static final int SRGB = 7;
    public static final int STANDARD_BT601_525 = 3;
    public static final int STANDARD_BT601_525_UNADJUSTED = 4;
    public static final int STANDARD_BT601_625 = 1;
    public static final int STANDARD_BT601_625_UNADJUSTED = 2;
    public static final int STANDARD_BT709 = 5;

    public static final String toString(int o) {
        if (o == 0) {
            return "NATIVE";
        }
        if (o == 1) {
            return "STANDARD_BT601_625";
        }
        if (o == 2) {
            return "STANDARD_BT601_625_UNADJUSTED";
        }
        if (o == 3) {
            return "STANDARD_BT601_525";
        }
        if (o == 4) {
            return "STANDARD_BT601_525_UNADJUSTED";
        }
        if (o == 5) {
            return "STANDARD_BT709";
        }
        if (o == 6) {
            return "DCI_P3";
        }
        if (o == 7) {
            return "SRGB";
        }
        if (o == 8) {
            return "ADOBE_RGB";
        }
        if (o == 9) {
            return "DISPLAY_P3";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("NATIVE");
        if ((o & 1) == 1) {
            list.add("STANDARD_BT601_625");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("STANDARD_BT601_625_UNADJUSTED");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("STANDARD_BT601_525");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("STANDARD_BT601_525_UNADJUSTED");
            flipped |= 4;
        }
        if ((o & 5) == 5) {
            list.add("STANDARD_BT709");
            flipped |= 5;
        }
        if ((o & 6) == 6) {
            list.add("DCI_P3");
            flipped |= 6;
        }
        if ((o & 7) == 7) {
            list.add("SRGB");
            flipped |= 7;
        }
        if ((o & 8) == 8) {
            list.add("ADOBE_RGB");
            flipped |= 8;
        }
        if ((o & 9) == 9) {
            list.add("DISPLAY_P3");
            flipped |= 9;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}