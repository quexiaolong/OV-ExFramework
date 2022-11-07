package android.hardware.tv.cec.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class SendMessageResult {
    public static final int BUSY = 2;
    public static final int FAIL = 3;
    public static final int NACK = 1;
    public static final int SUCCESS = 0;

    public static final String toString(int o) {
        if (o == 0) {
            return "SUCCESS";
        }
        if (o == 1) {
            return "NACK";
        }
        if (o == 2) {
            return "BUSY";
        }
        if (o == 3) {
            return "FAIL";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("SUCCESS");
        if ((o & 1) == 1) {
            list.add("NACK");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("BUSY");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("FAIL");
            flipped |= 3;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}