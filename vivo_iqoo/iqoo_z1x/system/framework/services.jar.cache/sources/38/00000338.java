package com.android.server;

import java.util.Map;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.-$$Lambda$CSz_ibwXhtkKNl72Q8tR5oBgkWk  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$CSz_ibwXhtkKNl72Q8tR5oBgkWk implements Function {
    public static final /* synthetic */ $$Lambda$CSz_ibwXhtkKNl72Q8tR5oBgkWk INSTANCE = new $$Lambda$CSz_ibwXhtkKNl72Q8tR5oBgkWk();

    private /* synthetic */ $$Lambda$CSz_ibwXhtkKNl72Q8tR5oBgkWk() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return (String) ((Map.Entry) obj).getKey();
    }
}