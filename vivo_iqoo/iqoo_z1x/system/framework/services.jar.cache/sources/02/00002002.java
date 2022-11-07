package com.vivo.server.adapter.location;

import android.content.Context;
import android.location.Location;

/* loaded from: classes2.dex */
public abstract class AbsIZatDCControllerAdapter {
    public abstract boolean initDC(Context context);

    public abstract boolean lostLocation();

    public abstract boolean onNlpPassiveLocation(long j, double d, double d2);

    public abstract boolean onReportLocation(Location location);

    public abstract boolean onSetRequest(String str, String str2);

    public abstract boolean setDebug(boolean z);

    public abstract void setDiagnosticCallback(Diagnoster diagnoster);

    public abstract boolean setNetwork(boolean z);

    public abstract boolean startNavigating(long j, boolean z);

    public abstract boolean stopNavigating(long j, boolean z);

    public abstract boolean updateSvStatus(int i, int[] iArr, float[] fArr);
}