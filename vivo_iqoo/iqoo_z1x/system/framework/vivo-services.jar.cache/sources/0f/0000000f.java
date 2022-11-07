package android.hardware.graphics.common.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class Transform {
    public static final int FLIP_H = 1;
    public static final int FLIP_V = 2;
    public static final int ROT_180 = 3;
    public static final int ROT_270 = 7;
    public static final int ROT_90 = 4;

    public static final String toString(int o) {
        if (o == 1) {
            return "FLIP_H";
        }
        if (o == 2) {
            return "FLIP_V";
        }
        if (o == 4) {
            return "ROT_90";
        }
        if (o == 3) {
            return "ROT_180";
        }
        if (o == 7) {
            return "ROT_270";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 1) == 1) {
            list.add("FLIP_H");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("FLIP_V");
            flipped |= 2;
        }
        if ((o & 4) == 4) {
            list.add("ROT_90");
            flipped |= 4;
        }
        if ((o & 3) == 3) {
            list.add("ROT_180");
            flipped |= 3;
        }
        if ((o & 7) == 7) {
            list.add("ROT_270");
            flipped |= 7;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}