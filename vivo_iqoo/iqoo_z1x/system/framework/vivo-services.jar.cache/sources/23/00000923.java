package vendor.vivo.hardware.camera.vivopostproc.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class Status {
    public static final int ILLEGAL_ARGUMENT = 1;
    public static final int INTERNAL_ERROR = 7;
    public static final int OK = 0;
    public static final int REQUEST_ERROR = 3;
    public static final int REQUEST_STREAM_ID_ERROR = 4;
    public static final int RESOURCE_ERROR = 2;
    public static final int RESULT_ERROR = 5;

    public static final String toString(int o) {
        if (o == 0) {
            return "OK";
        }
        if (o == 1) {
            return "ILLEGAL_ARGUMENT";
        }
        if (o == 2) {
            return "RESOURCE_ERROR";
        }
        if (o == 3) {
            return "REQUEST_ERROR";
        }
        if (o == 4) {
            return "REQUEST_STREAM_ID_ERROR";
        }
        if (o == 5) {
            return "RESULT_ERROR";
        }
        if (o == 7) {
            return "INTERNAL_ERROR";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("OK");
        if ((o & 1) == 1) {
            list.add("ILLEGAL_ARGUMENT");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("RESOURCE_ERROR");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("REQUEST_ERROR");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("REQUEST_STREAM_ID_ERROR");
            flipped |= 4;
        }
        if ((o & 5) == 5) {
            list.add("RESULT_ERROR");
            flipped |= 5;
        }
        if ((o & 7) == 7) {
            list.add("INTERNAL_ERROR");
            flipped |= 7;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}