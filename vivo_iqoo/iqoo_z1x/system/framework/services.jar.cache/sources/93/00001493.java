package com.android.server.people.prediction;

import com.android.server.people.data.AppUsageStatsData;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.people.prediction.-$$Lambda$DMVkqXFJLO7sJ5RamFQqFFai5uw  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$DMVkqXFJLO7sJ5RamFQqFFai5uw implements Function {
    public static final /* synthetic */ $$Lambda$DMVkqXFJLO7sJ5RamFQqFFai5uw INSTANCE = new $$Lambda$DMVkqXFJLO7sJ5RamFQqFFai5uw();

    private /* synthetic */ $$Lambda$DMVkqXFJLO7sJ5RamFQqFFai5uw() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return Integer.valueOf(((AppUsageStatsData) obj).getChosenCount());
    }
}