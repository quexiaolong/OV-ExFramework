package android.hardware.tv.cec.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class HdmiPortType {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "INPUT";
        }
        if (o == 1) {
            return "OUTPUT";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("INPUT");
        if ((o & 1) == 1) {
            list.add("OUTPUT");
            flipped = 0 | 1;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}