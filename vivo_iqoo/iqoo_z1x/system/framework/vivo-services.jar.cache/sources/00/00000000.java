package android.hardware.camera.common.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class CameraDeviceStatus {
    public static final int ENUMERATING = 2;
    public static final int NOT_PRESENT = 0;
    public static final int PRESENT = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "NOT_PRESENT";
        }
        if (o == 1) {
            return "PRESENT";
        }
        if (o == 2) {
            return "ENUMERATING";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("NOT_PRESENT");
        if ((o & 1) == 1) {
            list.add("PRESENT");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("ENUMERATING");
            flipped |= 2;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}