package com.android.server;

import com.android.internal.os.LooperStats;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.-$$Lambda$LooperStatsService$Vzysuo2tO86qjfcWeh1Rdb47NQQ  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$LooperStatsService$Vzysuo2tO86qjfcWeh1Rdb47NQQ implements Function {
    public static final /* synthetic */ $$Lambda$LooperStatsService$Vzysuo2tO86qjfcWeh1Rdb47NQQ INSTANCE = new $$Lambda$LooperStatsService$Vzysuo2tO86qjfcWeh1Rdb47NQQ();

    private /* synthetic */ $$Lambda$LooperStatsService$Vzysuo2tO86qjfcWeh1Rdb47NQQ() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        String str;
        str = ((LooperStats.ExportedEntry) obj).threadName;
        return str;
    }
}