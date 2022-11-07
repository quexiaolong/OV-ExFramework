package android.hardware.graphics.common.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class Dataspace {
    public static final int ADOBE_RGB = 151715840;
    public static final int ARBITRARY = 1;
    public static final int BT2020 = 147193856;
    public static final int BT2020_LINEAR = 138805248;
    public static final int BT2020_PQ = 163971072;
    public static final int BT601_525 = 259;
    public static final int BT601_625 = 258;
    public static final int BT709 = 260;
    public static final int DCI_P3 = 155844608;
    public static final int DCI_P3_LINEAR = 139067392;
    public static final int DEPTH = 4096;
    public static final int DISPLAY_P3 = 143261696;
    public static final int DISPLAY_P3_LINEAR = 139067392;
    public static final int JFIF = 257;
    public static final int RANGE_EXTENDED = 402653184;
    public static final int RANGE_FULL = 134217728;
    public static final int RANGE_LIMITED = 268435456;
    public static final int RANGE_MASK = 939524096;
    public static final int RANGE_SHIFT = 27;
    public static final int RANGE_UNSPECIFIED = 0;
    public static final int SENSOR = 4097;
    public static final int SRGB = 513;
    public static final int SRGB_LINEAR = 512;
    public static final int STANDARD_ADOBE_RGB = 720896;
    public static final int STANDARD_BT2020 = 393216;
    public static final int STANDARD_BT2020_CONSTANT_LUMINANCE = 458752;
    public static final int STANDARD_BT470M = 524288;
    public static final int STANDARD_BT601_525 = 262144;
    public static final int STANDARD_BT601_525_UNADJUSTED = 327680;
    public static final int STANDARD_BT601_625 = 131072;
    public static final int STANDARD_BT601_625_UNADJUSTED = 196608;
    public static final int STANDARD_BT709 = 65536;
    public static final int STANDARD_DCI_P3 = 655360;
    public static final int STANDARD_FILM = 589824;
    public static final int STANDARD_MASK = 4128768;
    public static final int STANDARD_SHIFT = 16;
    public static final int STANDARD_UNSPECIFIED = 0;
    public static final int TRANSFER_GAMMA2_2 = 16777216;
    public static final int TRANSFER_GAMMA2_6 = 20971520;
    public static final int TRANSFER_GAMMA2_8 = 25165824;
    public static final int TRANSFER_HLG = 33554432;
    public static final int TRANSFER_LINEAR = 4194304;
    public static final int TRANSFER_MASK = 130023424;
    public static final int TRANSFER_SHIFT = 22;
    public static final int TRANSFER_SMPTE_170M = 12582912;
    public static final int TRANSFER_SRGB = 8388608;
    public static final int TRANSFER_ST2084 = 29360128;
    public static final int TRANSFER_UNSPECIFIED = 0;
    public static final int UNKNOWN = 0;
    public static final int V0_BT601_525 = 281280512;
    public static final int V0_BT601_625 = 281149440;
    public static final int V0_BT709 = 281083904;
    public static final int V0_JFIF = 146931712;
    public static final int V0_SCRGB = 411107328;
    public static final int V0_SCRGB_LINEAR = 406913024;
    public static final int V0_SRGB = 142671872;
    public static final int V0_SRGB_LINEAR = 138477568;

    public static final String toString(int o) {
        if (o == 0) {
            return "UNKNOWN";
        }
        if (o == 1) {
            return "ARBITRARY";
        }
        if (o == 16) {
            return "STANDARD_SHIFT";
        }
        if (o == 4128768) {
            return "STANDARD_MASK";
        }
        if (o == 0) {
            return "STANDARD_UNSPECIFIED";
        }
        if (o == 65536) {
            return "STANDARD_BT709";
        }
        if (o == 131072) {
            return "STANDARD_BT601_625";
        }
        if (o == 196608) {
            return "STANDARD_BT601_625_UNADJUSTED";
        }
        if (o == 262144) {
            return "STANDARD_BT601_525";
        }
        if (o == 327680) {
            return "STANDARD_BT601_525_UNADJUSTED";
        }
        if (o == 393216) {
            return "STANDARD_BT2020";
        }
        if (o == 458752) {
            return "STANDARD_BT2020_CONSTANT_LUMINANCE";
        }
        if (o == 524288) {
            return "STANDARD_BT470M";
        }
        if (o == 589824) {
            return "STANDARD_FILM";
        }
        if (o == 655360) {
            return "STANDARD_DCI_P3";
        }
        if (o == 720896) {
            return "STANDARD_ADOBE_RGB";
        }
        if (o == 22) {
            return "TRANSFER_SHIFT";
        }
        if (o == 130023424) {
            return "TRANSFER_MASK";
        }
        if (o == 0) {
            return "TRANSFER_UNSPECIFIED";
        }
        if (o == 4194304) {
            return "TRANSFER_LINEAR";
        }
        if (o == 8388608) {
            return "TRANSFER_SRGB";
        }
        if (o == 12582912) {
            return "TRANSFER_SMPTE_170M";
        }
        if (o == 16777216) {
            return "TRANSFER_GAMMA2_2";
        }
        if (o == 20971520) {
            return "TRANSFER_GAMMA2_6";
        }
        if (o == 25165824) {
            return "TRANSFER_GAMMA2_8";
        }
        if (o == 29360128) {
            return "TRANSFER_ST2084";
        }
        if (o == 33554432) {
            return "TRANSFER_HLG";
        }
        if (o == 27) {
            return "RANGE_SHIFT";
        }
        if (o == 939524096) {
            return "RANGE_MASK";
        }
        if (o == 0) {
            return "RANGE_UNSPECIFIED";
        }
        if (o == 134217728) {
            return "RANGE_FULL";
        }
        if (o == 268435456) {
            return "RANGE_LIMITED";
        }
        if (o == 402653184) {
            return "RANGE_EXTENDED";
        }
        if (o == 512) {
            return "SRGB_LINEAR";
        }
        if (o == 138477568) {
            return "V0_SRGB_LINEAR";
        }
        if (o == 406913024) {
            return "V0_SCRGB_LINEAR";
        }
        if (o == 513) {
            return "SRGB";
        }
        if (o == 142671872) {
            return "V0_SRGB";
        }
        if (o == 411107328) {
            return "V0_SCRGB";
        }
        if (o == 257) {
            return "JFIF";
        }
        if (o == 146931712) {
            return "V0_JFIF";
        }
        if (o == 258) {
            return "BT601_625";
        }
        if (o == 281149440) {
            return "V0_BT601_625";
        }
        if (o == 259) {
            return "BT601_525";
        }
        if (o == 281280512) {
            return "V0_BT601_525";
        }
        if (o == 260) {
            return "BT709";
        }
        if (o == 281083904) {
            return "V0_BT709";
        }
        if (o == 139067392) {
            return "DCI_P3_LINEAR";
        }
        if (o == 155844608) {
            return "DCI_P3";
        }
        if (o == 139067392) {
            return "DISPLAY_P3_LINEAR";
        }
        if (o == 143261696) {
            return "DISPLAY_P3";
        }
        if (o == 151715840) {
            return "ADOBE_RGB";
        }
        if (o == 138805248) {
            return "BT2020_LINEAR";
        }
        if (o == 147193856) {
            return "BT2020";
        }
        if (o == 163971072) {
            return "BT2020_PQ";
        }
        if (o == 4096) {
            return "DEPTH";
        }
        if (o == 4097) {
            return "SENSOR";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("UNKNOWN");
        if ((o & 1) == 1) {
            list.add("ARBITRARY");
            flipped = 0 | 1;
        }
        if ((o & 16) == 16) {
            list.add("STANDARD_SHIFT");
            flipped |= 16;
        }
        if ((o & STANDARD_MASK) == 4128768) {
            list.add("STANDARD_MASK");
            flipped |= STANDARD_MASK;
        }
        list.add("STANDARD_UNSPECIFIED");
        if ((o & STANDARD_BT709) == 65536) {
            list.add("STANDARD_BT709");
            flipped |= STANDARD_BT709;
        }
        if ((o & STANDARD_BT601_625) == 131072) {
            list.add("STANDARD_BT601_625");
            flipped |= STANDARD_BT601_625;
        }
        if ((o & STANDARD_BT601_625_UNADJUSTED) == 196608) {
            list.add("STANDARD_BT601_625_UNADJUSTED");
            flipped |= STANDARD_BT601_625_UNADJUSTED;
        }
        if ((o & STANDARD_BT601_525) == 262144) {
            list.add("STANDARD_BT601_525");
            flipped |= STANDARD_BT601_525;
        }
        if ((o & STANDARD_BT601_525_UNADJUSTED) == 327680) {
            list.add("STANDARD_BT601_525_UNADJUSTED");
            flipped |= STANDARD_BT601_525_UNADJUSTED;
        }
        if ((o & STANDARD_BT2020) == 393216) {
            list.add("STANDARD_BT2020");
            flipped |= STANDARD_BT2020;
        }
        if ((o & STANDARD_BT2020_CONSTANT_LUMINANCE) == 458752) {
            list.add("STANDARD_BT2020_CONSTANT_LUMINANCE");
            flipped |= STANDARD_BT2020_CONSTANT_LUMINANCE;
        }
        if ((o & STANDARD_BT470M) == 524288) {
            list.add("STANDARD_BT470M");
            flipped |= STANDARD_BT470M;
        }
        if ((o & STANDARD_FILM) == 589824) {
            list.add("STANDARD_FILM");
            flipped |= STANDARD_FILM;
        }
        if ((o & STANDARD_DCI_P3) == 655360) {
            list.add("STANDARD_DCI_P3");
            flipped |= STANDARD_DCI_P3;
        }
        if ((o & STANDARD_ADOBE_RGB) == 720896) {
            list.add("STANDARD_ADOBE_RGB");
            flipped |= STANDARD_ADOBE_RGB;
        }
        if ((o & 22) == 22) {
            list.add("TRANSFER_SHIFT");
            flipped |= 22;
        }
        if ((o & TRANSFER_MASK) == 130023424) {
            list.add("TRANSFER_MASK");
            flipped |= TRANSFER_MASK;
        }
        list.add("TRANSFER_UNSPECIFIED");
        if ((o & TRANSFER_LINEAR) == 4194304) {
            list.add("TRANSFER_LINEAR");
            flipped |= TRANSFER_LINEAR;
        }
        if ((o & TRANSFER_SRGB) == 8388608) {
            list.add("TRANSFER_SRGB");
            flipped |= TRANSFER_SRGB;
        }
        if ((o & TRANSFER_SMPTE_170M) == 12582912) {
            list.add("TRANSFER_SMPTE_170M");
            flipped |= TRANSFER_SMPTE_170M;
        }
        if ((o & TRANSFER_GAMMA2_2) == 16777216) {
            list.add("TRANSFER_GAMMA2_2");
            flipped |= TRANSFER_GAMMA2_2;
        }
        if ((o & TRANSFER_GAMMA2_6) == 20971520) {
            list.add("TRANSFER_GAMMA2_6");
            flipped |= TRANSFER_GAMMA2_6;
        }
        if ((25165824 & o) == 25165824) {
            list.add("TRANSFER_GAMMA2_8");
            flipped |= TRANSFER_GAMMA2_8;
        }
        if ((29360128 & o) == 29360128) {
            list.add("TRANSFER_ST2084");
            flipped |= TRANSFER_ST2084;
        }
        if ((33554432 & o) == 33554432) {
            list.add("TRANSFER_HLG");
            flipped |= TRANSFER_HLG;
        }
        if ((o & 27) == 27) {
            list.add("RANGE_SHIFT");
            flipped |= 27;
        }
        if ((939524096 & o) == 939524096) {
            list.add("RANGE_MASK");
            flipped |= RANGE_MASK;
        }
        list.add("RANGE_UNSPECIFIED");
        if ((134217728 & o) == 134217728) {
            list.add("RANGE_FULL");
            flipped |= RANGE_FULL;
        }
        if ((268435456 & o) == 268435456) {
            list.add("RANGE_LIMITED");
            flipped |= 268435456;
        }
        if ((402653184 & o) == 402653184) {
            list.add("RANGE_EXTENDED");
            flipped |= RANGE_EXTENDED;
        }
        if ((o & 512) == 512) {
            list.add("SRGB_LINEAR");
            flipped |= 512;
        }
        if ((138477568 & o) == 138477568) {
            list.add("V0_SRGB_LINEAR");
            flipped |= V0_SRGB_LINEAR;
        }
        if ((406913024 & o) == 406913024) {
            list.add("V0_SCRGB_LINEAR");
            flipped |= V0_SCRGB_LINEAR;
        }
        if ((o & 513) == 513) {
            list.add("SRGB");
            flipped |= 513;
        }
        if ((142671872 & o) == 142671872) {
            list.add("V0_SRGB");
            flipped |= V0_SRGB;
        }
        if ((411107328 & o) == 411107328) {
            list.add("V0_SCRGB");
            flipped |= V0_SCRGB;
        }
        if ((o & 257) == 257) {
            list.add("JFIF");
            flipped |= 257;
        }
        if ((146931712 & o) == 146931712) {
            list.add("V0_JFIF");
            flipped |= V0_JFIF;
        }
        if ((o & 258) == 258) {
            list.add("BT601_625");
            flipped |= 258;
        }
        if ((281149440 & o) == 281149440) {
            list.add("V0_BT601_625");
            flipped |= V0_BT601_625;
        }
        if ((o & 259) == 259) {
            list.add("BT601_525");
            flipped |= 259;
        }
        if ((281280512 & o) == 281280512) {
            list.add("V0_BT601_525");
            flipped |= V0_BT601_525;
        }
        if ((o & 260) == 260) {
            list.add("BT709");
            flipped |= 260;
        }
        if ((281083904 & o) == 281083904) {
            list.add("V0_BT709");
            flipped |= V0_BT709;
        }
        if ((o & 139067392) == 139067392) {
            list.add("DCI_P3_LINEAR");
            flipped |= 139067392;
        }
        if ((155844608 & o) == 155844608) {
            list.add("DCI_P3");
            flipped |= DCI_P3;
        }
        if ((o & 139067392) == 139067392) {
            list.add("DISPLAY_P3_LINEAR");
            flipped |= 139067392;
        }
        if ((143261696 & o) == 143261696) {
            list.add("DISPLAY_P3");
            flipped |= DISPLAY_P3;
        }
        if ((151715840 & o) == 151715840) {
            list.add("ADOBE_RGB");
            flipped |= ADOBE_RGB;
        }
        if ((138805248 & o) == 138805248) {
            list.add("BT2020_LINEAR");
            flipped |= BT2020_LINEAR;
        }
        if ((147193856 & o) == 147193856) {
            list.add("BT2020");
            flipped |= BT2020;
        }
        if ((163971072 & o) == 163971072) {
            list.add("BT2020_PQ");
            flipped |= BT2020_PQ;
        }
        if ((o & 4096) == 4096) {
            list.add("DEPTH");
            flipped |= 4096;
        }
        if ((o & SENSOR) == 4097) {
            list.add("SENSOR");
            flipped |= SENSOR;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}