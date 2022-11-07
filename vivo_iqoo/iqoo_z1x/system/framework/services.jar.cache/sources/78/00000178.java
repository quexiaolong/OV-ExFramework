package android.hardware.tv.cec.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class OptionKey {
    public static final int ENABLE_CEC = 2;
    public static final int SYSTEM_CEC_CONTROL = 3;
    public static final int WAKEUP = 1;

    public static final String toString(int o) {
        if (o == 1) {
            return "WAKEUP";
        }
        if (o == 2) {
            return "ENABLE_CEC";
        }
        if (o == 3) {
            return "SYSTEM_CEC_CONTROL";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 1) == 1) {
            list.add("WAKEUP");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("ENABLE_CEC");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("SYSTEM_CEC_CONTROL");
            flipped |= 3;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}