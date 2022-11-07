package com.android.server;

import java.util.Map;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.-$$Lambda$htemI6hNv3kq1UVGrXpRlPIVXRU  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$htemI6hNv3kq1UVGrXpRlPIVXRU implements Function {
    public static final /* synthetic */ $$Lambda$htemI6hNv3kq1UVGrXpRlPIVXRU INSTANCE = new $$Lambda$htemI6hNv3kq1UVGrXpRlPIVXRU();

    private /* synthetic */ $$Lambda$htemI6hNv3kq1UVGrXpRlPIVXRU() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return (Integer) ((Map.Entry) obj).getKey();
    }
}