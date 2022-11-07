package vendor.pixelworks.hardware.display.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class PanelType {
    public static final int LCD_P3 = 1;
    public static final int LCD_SRGB = 0;
    public static final int OLED = 2;

    public static final String toString(int o) {
        if (o == 0) {
            return "LCD_SRGB";
        }
        if (o == 1) {
            return "LCD_P3";
        }
        if (o == 2) {
            return "OLED";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("LCD_SRGB");
        if ((o & 1) == 1) {
            list.add("LCD_P3");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("OLED");
            flipped |= 2;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}