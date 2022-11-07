package android.hardware.graphics.common.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class PixelFormat {
    public static final int BGRA_8888 = 5;
    public static final int BLOB = 33;
    public static final int IMPLEMENTATION_DEFINED = 34;
    public static final int RAW10 = 37;
    public static final int RAW12 = 38;
    public static final int RAW16 = 32;
    public static final int RAW_OPAQUE = 36;
    public static final int RGBA_1010102 = 43;
    public static final int RGBA_8888 = 1;
    public static final int RGBA_FP16 = 22;
    public static final int RGBX_8888 = 2;
    public static final int RGB_565 = 4;
    public static final int RGB_888 = 3;
    public static final int Y16 = 540422489;
    public static final int Y8 = 538982489;
    public static final int YCBCR_420_888 = 35;
    public static final int YCBCR_422_I = 20;
    public static final int YCBCR_422_SP = 16;
    public static final int YCRCB_420_SP = 17;
    public static final int YV12 = 842094169;

    public static final String toString(int o) {
        if (o == 1) {
            return "RGBA_8888";
        }
        if (o == 2) {
            return "RGBX_8888";
        }
        if (o == 3) {
            return "RGB_888";
        }
        if (o == 4) {
            return "RGB_565";
        }
        if (o == 5) {
            return "BGRA_8888";
        }
        if (o == 16) {
            return "YCBCR_422_SP";
        }
        if (o == 17) {
            return "YCRCB_420_SP";
        }
        if (o == 20) {
            return "YCBCR_422_I";
        }
        if (o == 22) {
            return "RGBA_FP16";
        }
        if (o == 32) {
            return "RAW16";
        }
        if (o == 33) {
            return "BLOB";
        }
        if (o == 34) {
            return "IMPLEMENTATION_DEFINED";
        }
        if (o == 35) {
            return "YCBCR_420_888";
        }
        if (o == 36) {
            return "RAW_OPAQUE";
        }
        if (o == 37) {
            return "RAW10";
        }
        if (o == 38) {
            return "RAW12";
        }
        if (o == 43) {
            return "RGBA_1010102";
        }
        if (o == 538982489) {
            return "Y8";
        }
        if (o == 540422489) {
            return "Y16";
        }
        if (o == 842094169) {
            return "YV12";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 1) == 1) {
            list.add("RGBA_8888");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("RGBX_8888");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("RGB_888");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("RGB_565");
            flipped |= 4;
        }
        if ((o & 5) == 5) {
            list.add("BGRA_8888");
            flipped |= 5;
        }
        if ((o & 16) == 16) {
            list.add("YCBCR_422_SP");
            flipped |= 16;
        }
        if ((o & 17) == 17) {
            list.add("YCRCB_420_SP");
            flipped |= 17;
        }
        if ((o & 20) == 20) {
            list.add("YCBCR_422_I");
            flipped |= 20;
        }
        if ((o & 22) == 22) {
            list.add("RGBA_FP16");
            flipped |= 22;
        }
        if ((o & 32) == 32) {
            list.add("RAW16");
            flipped |= 32;
        }
        if ((o & 33) == 33) {
            list.add("BLOB");
            flipped |= 33;
        }
        if ((o & 34) == 34) {
            list.add("IMPLEMENTATION_DEFINED");
            flipped |= 34;
        }
        if ((o & 35) == 35) {
            list.add("YCBCR_420_888");
            flipped |= 35;
        }
        if ((o & 36) == 36) {
            list.add("RAW_OPAQUE");
            flipped |= 36;
        }
        if ((o & 37) == 37) {
            list.add("RAW10");
            flipped |= 37;
        }
        if ((o & 38) == 38) {
            list.add("RAW12");
            flipped |= 38;
        }
        if ((o & 43) == 43) {
            list.add("RGBA_1010102");
            flipped |= 43;
        }
        if ((o & Y8) == 538982489) {
            list.add("Y8");
            flipped |= Y8;
        }
        if ((o & Y16) == 540422489) {
            list.add("Y16");
            flipped |= Y16;
        }
        if ((o & YV12) == 842094169) {
            list.add("YV12");
            flipped |= YV12;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}