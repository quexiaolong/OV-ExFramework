package com.android.server.people.data;

import java.util.function.BiFunction;

/* compiled from: lambda */
/* renamed from: com.android.server.people.data.-$$Lambda$LrkJFe4YP5g-sc0rXJgTGXS3PRE  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$LrkJFe4YP5gsc0rXJgTGXS3PRE implements BiFunction {
    public static final /* synthetic */ $$Lambda$LrkJFe4YP5gsc0rXJgTGXS3PRE INSTANCE = new $$Lambda$LrkJFe4YP5gsc0rXJgTGXS3PRE();

    private /* synthetic */ $$Lambda$LrkJFe4YP5gsc0rXJgTGXS3PRE() {
    }

    @Override // java.util.function.BiFunction
    public final Object apply(Object obj, Object obj2) {
        return Integer.valueOf(Integer.sum(((Integer) obj).intValue(), ((Integer) obj2).intValue()));
    }
}