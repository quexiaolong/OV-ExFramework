package com.android.server;

import android.os.CarrierAssociatedAppEntry;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.-$$Lambda$SystemConfigService$1$48nhaXPvuCaH0ZzSd3oLBI99uhI  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$SystemConfigService$1$48nhaXPvuCaH0ZzSd3oLBI99uhI implements Function {
    public static final /* synthetic */ $$Lambda$SystemConfigService$1$48nhaXPvuCaH0ZzSd3oLBI99uhI INSTANCE = new $$Lambda$SystemConfigService$1$48nhaXPvuCaH0ZzSd3oLBI99uhI();

    private /* synthetic */ $$Lambda$SystemConfigService$1$48nhaXPvuCaH0ZzSd3oLBI99uhI() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        String str;
        str = ((CarrierAssociatedAppEntry) obj).packageName;
        return str;
    }
}