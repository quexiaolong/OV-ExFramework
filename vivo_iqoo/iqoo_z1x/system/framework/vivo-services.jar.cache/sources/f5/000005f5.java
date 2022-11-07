package com.vivo.face.internal.wrapper;

import android.content.Context;

/* loaded from: classes.dex */
public final class SensorWrapper {
    public static final int SENSOR_TYPE_AMBIENT_LIGHT_SCENE = 66544;
    private static final String TAG = "SensorWrapper";
    public static final int TYPE_LIGHT_B = 66551;
    public static final int TYPE_PROXIMITY_B = 66550;
    public static final int TYPE_RAISEUP_DETECT = 66538;
    public static final int TYPE_VIVOMOTION_DETECT = 66540;
    private Context mContext;

    public SensorWrapper(Context context) {
        this.mContext = context;
    }
}