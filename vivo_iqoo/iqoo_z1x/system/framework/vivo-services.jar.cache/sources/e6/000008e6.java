package vendor.pixelworks.hardware.display.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class ChipVersion {
    public static final int IRIS2 = 0;
    public static final int IRIS2_PLUS = 1;
    public static final int IRIS3_LITE = 2;

    public static final String toString(int o) {
        if (o == 0) {
            return "IRIS2";
        }
        if (o == 1) {
            return "IRIS2_PLUS";
        }
        if (o == 2) {
            return "IRIS3_LITE";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("IRIS2");
        if ((o & 1) == 1) {
            list.add("IRIS2_PLUS");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("IRIS3_LITE");
            flipped |= 2;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}