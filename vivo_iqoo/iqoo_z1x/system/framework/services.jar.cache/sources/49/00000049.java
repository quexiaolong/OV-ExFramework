package android.hardware.audio.common.V2_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class AudioSessionConsts {
    public static final int ALLOCATE = 0;
    public static final int NONE = 0;
    public static final int OUTPUT_MIX = 0;
    public static final int OUTPUT_STAGE = -1;

    public static final String toString(int o) {
        if (o == -1) {
            return "OUTPUT_STAGE";
        }
        if (o == 0) {
            return "OUTPUT_MIX";
        }
        if (o == 0) {
            return "ALLOCATE";
        }
        if (o == 0) {
            return "NONE";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & (-1)) == -1) {
            list.add("OUTPUT_STAGE");
            flipped = 0 | (-1);
        }
        list.add("OUTPUT_MIX");
        list.add("ALLOCATE");
        list.add("NONE");
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}