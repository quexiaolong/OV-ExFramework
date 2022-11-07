package android.hardware.audio.common.V2_0;

import com.android.server.utils.PriorityDump;
import java.util.ArrayList;

/* loaded from: classes.dex */
public final class AudioMixLatencyClass {
    public static final int LOW = 0;
    public static final int NORMAL = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "LOW";
        }
        if (o == 1) {
            return PriorityDump.PRIORITY_ARG_NORMAL;
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("LOW");
        if ((o & 1) == 1) {
            list.add(PriorityDump.PRIORITY_ARG_NORMAL);
            flipped = 0 | 1;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}