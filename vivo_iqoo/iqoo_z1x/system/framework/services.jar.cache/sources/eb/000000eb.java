package android.hardware.health.V2_1;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class Constants {
    public static final long BATTERY_CHARGE_TIME_TO_FULL_NOW_SECONDS_UNSUPPORTED = -1;

    public static final String toString(long o) {
        if (o == -1) {
            return "BATTERY_CHARGE_TIME_TO_FULL_NOW_SECONDS_UNSUPPORTED";
        }
        return "0x" + Long.toHexString(o);
    }

    public static final String dumpBitfield(long o) {
        ArrayList<String> list = new ArrayList<>();
        long flipped = 0;
        if ((o & (-1)) == -1) {
            list.add("BATTERY_CHARGE_TIME_TO_FULL_NOW_SECONDS_UNSUPPORTED");
            flipped = 0 | (-1);
        }
        if (o != flipped) {
            list.add("0x" + Long.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}