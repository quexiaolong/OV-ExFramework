package vendor.vivo.hardware.camera.vivopostproc.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class StreamRotation {
    public static final int ROTATION_0 = 0;
    public static final int ROTATION_180 = 2;
    public static final int ROTATION_270 = 3;
    public static final int ROTATION_90 = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "ROTATION_0";
        }
        if (o == 1) {
            return "ROTATION_90";
        }
        if (o == 2) {
            return "ROTATION_180";
        }
        if (o == 3) {
            return "ROTATION_270";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("ROTATION_0");
        if ((o & 1) == 1) {
            list.add("ROTATION_90");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("ROTATION_180");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("ROTATION_270");
            flipped |= 3;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}