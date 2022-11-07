package android.hardware.audio.common.V2_0;

import com.android.server.utils.PriorityDump;
import java.util.ArrayList;

/* loaded from: classes.dex */
public final class AudioMode {
    public static final int CNT = 4;
    public static final int CURRENT = -1;
    public static final int INVALID = -2;
    public static final int IN_CALL = 2;
    public static final int IN_COMMUNICATION = 3;
    public static final int MAX = 3;
    public static final int NORMAL = 0;
    public static final int RINGTONE = 1;

    public static final String toString(int o) {
        if (o == -2) {
            return "INVALID";
        }
        if (o == -1) {
            return "CURRENT";
        }
        if (o == 0) {
            return PriorityDump.PRIORITY_ARG_NORMAL;
        }
        if (o == 1) {
            return "RINGTONE";
        }
        if (o == 2) {
            return "IN_CALL";
        }
        if (o == 3) {
            return "IN_COMMUNICATION";
        }
        if (o == 4) {
            return "CNT";
        }
        if (o == 3) {
            return "MAX";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & (-2)) == -2) {
            list.add("INVALID");
            flipped = 0 | (-2);
        }
        if ((o & (-1)) == -1) {
            list.add("CURRENT");
            flipped |= -1;
        }
        list.add(PriorityDump.PRIORITY_ARG_NORMAL);
        if ((o & 1) == 1) {
            list.add("RINGTONE");
            flipped |= 1;
        }
        if ((o & 2) == 2) {
            list.add("IN_CALL");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("IN_COMMUNICATION");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("CNT");
            flipped |= 4;
        }
        if ((o & 3) == 3) {
            list.add("MAX");
            flipped |= 3;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}