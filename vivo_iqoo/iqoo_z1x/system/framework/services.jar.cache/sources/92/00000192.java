package android.hardware.usb.V1_2;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class ContaminantProtectionStatus {
    public static final int DISABLED = 8;
    public static final int FORCE_DISABLE = 4;
    public static final int FORCE_SINK = 1;
    public static final int FORCE_SOURCE = 2;
    public static final int NONE = 0;

    public static final String toString(int o) {
        if (o == 0) {
            return "NONE";
        }
        if (o == 1) {
            return "FORCE_SINK";
        }
        if (o == 2) {
            return "FORCE_SOURCE";
        }
        if (o == 4) {
            return "FORCE_DISABLE";
        }
        if (o == 8) {
            return "DISABLED";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("NONE");
        if ((o & 1) == 1) {
            list.add("FORCE_SINK");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("FORCE_SOURCE");
            flipped |= 2;
        }
        if ((o & 4) == 4) {
            list.add("FORCE_DISABLE");
            flipped |= 4;
        }
        if ((o & 8) == 8) {
            list.add("DISABLED");
            flipped |= 8;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}