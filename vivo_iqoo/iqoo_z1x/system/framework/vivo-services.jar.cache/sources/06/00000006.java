package android.hardware.camera.common.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class TorchModeStatus {
    public static final int AVAILABLE_OFF = 1;
    public static final int AVAILABLE_ON = 2;
    public static final int NOT_AVAILABLE = 0;

    public static final String toString(int o) {
        if (o == 0) {
            return "NOT_AVAILABLE";
        }
        if (o == 1) {
            return "AVAILABLE_OFF";
        }
        if (o == 2) {
            return "AVAILABLE_ON";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("NOT_AVAILABLE");
        if ((o & 1) == 1) {
            list.add("AVAILABLE_OFF");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("AVAILABLE_ON");
            flipped |= 2;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}