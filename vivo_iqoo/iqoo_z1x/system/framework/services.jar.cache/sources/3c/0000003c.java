package android.hardware.audio.common.V2_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class AudioInterleave {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "LEFT";
        }
        if (o == 1) {
            return "RIGHT";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("LEFT");
        if ((o & 1) == 1) {
            list.add("RIGHT");
            flipped = 0 | 1;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}