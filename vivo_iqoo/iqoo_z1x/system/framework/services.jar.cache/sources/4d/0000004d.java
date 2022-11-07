package android.hardware.audio.common.V2_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class FixedChannelCount {
    public static final int FCC_2 = 2;
    public static final int FCC_8 = 8;

    public static final String toString(int o) {
        if (o == 2) {
            return "FCC_2";
        }
        if (o == 8) {
            return "FCC_8";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 2) == 2) {
            list.add("FCC_2");
            flipped = 0 | 2;
        }
        if ((o & 8) == 8) {
            list.add("FCC_8");
            flipped |= 8;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}