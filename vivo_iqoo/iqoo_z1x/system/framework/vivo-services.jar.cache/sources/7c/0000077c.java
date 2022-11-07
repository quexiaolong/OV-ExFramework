package com.vivo.services.rms.sp.sdk;

import android.os.Parcel;
import android.text.TextUtils;

/* loaded from: classes.dex */
public class ParcelUtils {
    public static String readString(Parcel p) {
        if (p.readInt() != 0) {
            return p.readString();
        }
        return null;
    }

    public static void writeString(Parcel p, String val) {
        if (!TextUtils.isEmpty(val)) {
            p.writeInt(1);
            p.writeString(val);
            return;
        }
        p.writeInt(0);
    }
}