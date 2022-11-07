package android.hardware.camera.common.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class TagBoundaryId {
    public static final int AOSP = 0;
    public static final int VENDOR = Integer.MIN_VALUE;

    public static final String toString(int o) {
        if (o == 0) {
            return "AOSP";
        }
        if (o == Integer.MIN_VALUE) {
            return "VENDOR";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("AOSP");
        if ((o & Integer.MIN_VALUE) == Integer.MIN_VALUE) {
            list.add("VENDOR");
            flipped = 0 | Integer.MIN_VALUE;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}