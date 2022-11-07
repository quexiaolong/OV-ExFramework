package vendor.pixelworks.hardware.display.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class AppFilter {
    public static final int BLACK = 1;
    public static final int NORMAL = 0;
    public static final int WHITE = 2;

    public static final String toString(int o) {
        if (o == 0) {
            return "NORMAL";
        }
        if (o == 1) {
            return "BLACK";
        }
        if (o == 2) {
            return "WHITE";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("NORMAL");
        if ((o & 1) == 1) {
            list.add("BLACK");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("WHITE");
            flipped |= 2;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}