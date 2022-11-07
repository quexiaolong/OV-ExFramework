package com.android.server;

import com.android.internal.os.LooperStats;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.-$$Lambda$LooperStatsService$XtFJEDeyYRT79ZkVP96XkHribxg  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$LooperStatsService$XtFJEDeyYRT79ZkVP96XkHribxg implements Function {
    public static final /* synthetic */ $$Lambda$LooperStatsService$XtFJEDeyYRT79ZkVP96XkHribxg INSTANCE = new $$Lambda$LooperStatsService$XtFJEDeyYRT79ZkVP96XkHribxg();

    private /* synthetic */ $$Lambda$LooperStatsService$XtFJEDeyYRT79ZkVP96XkHribxg() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        String str;
        str = ((LooperStats.ExportedEntry) obj).messageName;
        return str;
    }
}