package com.android.server;

import com.android.internal.os.LooperStats;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.-$$Lambda$LooperStatsService$Byo6QAxZpVXDCMtjrcYJc6YLAks  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$LooperStatsService$Byo6QAxZpVXDCMtjrcYJc6YLAks implements Function {
    public static final /* synthetic */ $$Lambda$LooperStatsService$Byo6QAxZpVXDCMtjrcYJc6YLAks INSTANCE = new $$Lambda$LooperStatsService$Byo6QAxZpVXDCMtjrcYJc6YLAks();

    private /* synthetic */ $$Lambda$LooperStatsService$Byo6QAxZpVXDCMtjrcYJc6YLAks() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        Integer valueOf;
        valueOf = Integer.valueOf(((LooperStats.ExportedEntry) obj).workSourceUid);
        return valueOf;
    }
}