package android.hardware.tv.cec.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class MaxLength {
    public static final int MESSAGE_BODY = 15;

    public static final String toString(int o) {
        if (o == 15) {
            return "MESSAGE_BODY";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 15) == 15) {
            list.add("MESSAGE_BODY");
            flipped = 0 | 15;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}