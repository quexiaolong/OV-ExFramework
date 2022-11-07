package android.hardware.audio.common.V2_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class AudioGainMode {
    public static final int CHANNELS = 2;
    public static final int JOINT = 1;
    public static final int RAMP = 4;

    public static final String toString(int o) {
        if (o == 1) {
            return "JOINT";
        }
        if (o == 2) {
            return "CHANNELS";
        }
        if (o == 4) {
            return "RAMP";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 1) == 1) {
            list.add("JOINT");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("CHANNELS");
            flipped |= 2;
        }
        if ((o & 4) == 4) {
            list.add("RAMP");
            flipped |= 4;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}