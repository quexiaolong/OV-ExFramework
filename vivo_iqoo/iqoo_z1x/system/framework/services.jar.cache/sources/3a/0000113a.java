package com.android.server.location.gnss;

import android.location.IGnssMeasurementsListener;
import android.os.IBinder;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.location.gnss.-$$Lambda$qoNbXUvSu3yuTPVXPUfZW_HDrTQ  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$qoNbXUvSu3yuTPVXPUfZW_HDrTQ implements Function {
    public static final /* synthetic */ $$Lambda$qoNbXUvSu3yuTPVXPUfZW_HDrTQ INSTANCE = new $$Lambda$qoNbXUvSu3yuTPVXPUfZW_HDrTQ();

    private /* synthetic */ $$Lambda$qoNbXUvSu3yuTPVXPUfZW_HDrTQ() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return IGnssMeasurementsListener.Stub.asInterface((IBinder) obj);
    }
}