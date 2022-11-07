package com.android.server;

import com.android.internal.os.LooperStats;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.-$$Lambda$LooperStatsService$XjYmSR91xdWG1Xgt-Gj9GBZZbjk  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$LooperStatsService$XjYmSR91xdWG1XgtGj9GBZZbjk implements Function {
    public static final /* synthetic */ $$Lambda$LooperStatsService$XjYmSR91xdWG1XgtGj9GBZZbjk INSTANCE = new $$Lambda$LooperStatsService$XjYmSR91xdWG1XgtGj9GBZZbjk();

    private /* synthetic */ $$Lambda$LooperStatsService$XjYmSR91xdWG1XgtGj9GBZZbjk() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        String str;
        str = ((LooperStats.ExportedEntry) obj).handlerClassName;
        return str;
    }
}