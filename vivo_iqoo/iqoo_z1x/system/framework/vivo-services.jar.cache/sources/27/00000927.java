package vendor.vivo.hardware.camera.vivopostproc.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class StreamType {
    public static final int INPUT = 1;
    public static final int OUTPUT = 0;

    public static final String toString(int o) {
        if (o == 0) {
            return "OUTPUT";
        }
        if (o == 1) {
            return "INPUT";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("OUTPUT");
        if ((o & 1) == 1) {
            list.add("INPUT");
            flipped = 0 | 1;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}