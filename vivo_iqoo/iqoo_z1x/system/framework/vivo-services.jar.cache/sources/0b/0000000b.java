package android.hardware.graphics.common.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class ColorTransform {
    public static final int ARBITRARY_MATRIX = 1;
    public static final int CORRECT_DEUTERANOPIA = 5;
    public static final int CORRECT_PROTANOPIA = 4;
    public static final int CORRECT_TRITANOPIA = 6;
    public static final int GRAYSCALE = 3;
    public static final int IDENTITY = 0;
    public static final int VALUE_INVERSE = 2;

    public static final String toString(int o) {
        if (o == 0) {
            return "IDENTITY";
        }
        if (o == 1) {
            return "ARBITRARY_MATRIX";
        }
        if (o == 2) {
            return "VALUE_INVERSE";
        }
        if (o == 3) {
            return "GRAYSCALE";
        }
        if (o == 4) {
            return "CORRECT_PROTANOPIA";
        }
        if (o == 5) {
            return "CORRECT_DEUTERANOPIA";
        }
        if (o == 6) {
            return "CORRECT_TRITANOPIA";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("IDENTITY");
        if ((o & 1) == 1) {
            list.add("ARBITRARY_MATRIX");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("VALUE_INVERSE");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("GRAYSCALE");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("CORRECT_PROTANOPIA");
            flipped |= 4;
        }
        if ((o & 5) == 5) {
            list.add("CORRECT_DEUTERANOPIA");
            flipped |= 5;
        }
        if ((o & 6) == 6) {
            list.add("CORRECT_TRITANOPIA");
            flipped |= 6;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}