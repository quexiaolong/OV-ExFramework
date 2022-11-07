package android.hardware.graphics.common.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class Hdr {
    public static final int DOLBY_VISION = 1;
    public static final int HDR10 = 2;
    public static final int HLG = 3;

    public static final String toString(int o) {
        if (o == 1) {
            return "DOLBY_VISION";
        }
        if (o == 2) {
            return "HDR10";
        }
        if (o == 3) {
            return "HLG";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 1) == 1) {
            list.add("DOLBY_VISION");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("HDR10");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("HLG");
            flipped |= 3;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}