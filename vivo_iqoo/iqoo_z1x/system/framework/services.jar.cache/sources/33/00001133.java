package com.android.server.location.gnss;

import android.location.IGnssNavigationMessageListener;
import android.os.IBinder;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.location.gnss.-$$Lambda$HALkbmbB2IPr_wdFkPjiIWCzJsY  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$HALkbmbB2IPr_wdFkPjiIWCzJsY implements Function {
    public static final /* synthetic */ $$Lambda$HALkbmbB2IPr_wdFkPjiIWCzJsY INSTANCE = new $$Lambda$HALkbmbB2IPr_wdFkPjiIWCzJsY();

    private /* synthetic */ $$Lambda$HALkbmbB2IPr_wdFkPjiIWCzJsY() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return IGnssNavigationMessageListener.Stub.asInterface((IBinder) obj);
    }
}