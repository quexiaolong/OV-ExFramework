package com.android.server.pm;

import android.content.pm.parsing.component.ParsedIntentInfo;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.pm.-$$Lambda$DpkuTFpeWPmvN7iGgFrn8VkMVd4  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$DpkuTFpeWPmvN7iGgFrn8VkMVd4 implements Function {
    public static final /* synthetic */ $$Lambda$DpkuTFpeWPmvN7iGgFrn8VkMVd4 INSTANCE = new $$Lambda$DpkuTFpeWPmvN7iGgFrn8VkMVd4();

    private /* synthetic */ $$Lambda$DpkuTFpeWPmvN7iGgFrn8VkMVd4() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return ((ParsedIntentInfo) obj).categoriesIterator();
    }
}