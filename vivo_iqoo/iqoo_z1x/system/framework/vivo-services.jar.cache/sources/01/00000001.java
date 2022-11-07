package android.hardware.camera.common.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class CameraMetadataType {
    public static final int BYTE = 0;
    public static final int DOUBLE = 4;
    public static final int FLOAT = 2;
    public static final int INT32 = 1;
    public static final int INT64 = 3;
    public static final int RATIONAL = 5;

    public static final String toString(int o) {
        if (o == 0) {
            return "BYTE";
        }
        if (o == 1) {
            return "INT32";
        }
        if (o == 2) {
            return "FLOAT";
        }
        if (o == 3) {
            return "INT64";
        }
        if (o == 4) {
            return "DOUBLE";
        }
        if (o == 5) {
            return "RATIONAL";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("BYTE");
        if ((o & 1) == 1) {
            list.add("INT32");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("FLOAT");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("INT64");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("DOUBLE");
            flipped |= 4;
        }
        if ((o & 5) == 5) {
            list.add("RATIONAL");
            flipped |= 5;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}