package com.android.server.location.gnss;

import android.location.IGnssAntennaInfoListener;
import android.os.IBinder;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.location.gnss.-$$Lambda$D_8O7MDYM_zvDJaJvJVfzXhIfZY  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$D_8O7MDYM_zvDJaJvJVfzXhIfZY implements Function {
    public static final /* synthetic */ $$Lambda$D_8O7MDYM_zvDJaJvJVfzXhIfZY INSTANCE = new $$Lambda$D_8O7MDYM_zvDJaJvJVfzXhIfZY();

    private /* synthetic */ $$Lambda$D_8O7MDYM_zvDJaJvJVfzXhIfZY() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return IGnssAntennaInfoListener.Stub.asInterface((IBinder) obj);
    }
}