package vendor.vivo.hardware.camera.vivoreprocess.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class MsgType {
    public static final int REQUEST_ERROR = 1;

    public static final String toString(int o) {
        if (o == 1) {
            return "REQUEST_ERROR";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 1) == 1) {
            list.add("REQUEST_ERROR");
            flipped = 0 | 1;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}