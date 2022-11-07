package android.hardware.audio.common.V2_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class AudioPortConfigMask {
    public static final int ALL = 15;
    public static final int CHANNEL_MASK = 2;
    public static final int FORMAT = 4;
    public static final int GAIN = 8;
    public static final int SAMPLE_RATE = 1;

    public static final String toString(int o) {
        if (o == 1) {
            return "SAMPLE_RATE";
        }
        if (o == 2) {
            return "CHANNEL_MASK";
        }
        if (o == 4) {
            return "FORMAT";
        }
        if (o == 8) {
            return "GAIN";
        }
        if (o == 15) {
            return "ALL";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 1) == 1) {
            list.add("SAMPLE_RATE");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("CHANNEL_MASK");
            flipped |= 2;
        }
        if ((o & 4) == 4) {
            list.add("FORMAT");
            flipped |= 4;
        }
        if ((o & 8) == 8) {
            list.add("GAIN");
            flipped |= 8;
        }
        if ((o & 15) == 15) {
            list.add("ALL");
            flipped |= 15;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}