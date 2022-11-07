package com.android.server.people.prediction;

import com.android.server.people.data.AppUsageStatsData;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.people.prediction.-$$Lambda$cimiWkq7Holbf24FNF8j5P8r50M  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$cimiWkq7Holbf24FNF8j5P8r50M implements Function {
    public static final /* synthetic */ $$Lambda$cimiWkq7Holbf24FNF8j5P8r50M INSTANCE = new $$Lambda$cimiWkq7Holbf24FNF8j5P8r50M();

    private /* synthetic */ $$Lambda$cimiWkq7Holbf24FNF8j5P8r50M() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return Integer.valueOf(((AppUsageStatsData) obj).getLaunchCount());
    }
}