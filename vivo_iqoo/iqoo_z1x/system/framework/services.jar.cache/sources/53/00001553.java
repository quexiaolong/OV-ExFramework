package com.android.server.pm;

import android.content.pm.parsing.component.ParsedIntentInfo;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.pm.-$$Lambda$mI6eiz-cSKp3gDx4_MNMYKTJXG4  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$mI6eizcSKp3gDx4_MNMYKTJXG4 implements Function {
    public static final /* synthetic */ $$Lambda$mI6eizcSKp3gDx4_MNMYKTJXG4 INSTANCE = new $$Lambda$mI6eizcSKp3gDx4_MNMYKTJXG4();

    private /* synthetic */ $$Lambda$mI6eizcSKp3gDx4_MNMYKTJXG4() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return ((ParsedIntentInfo) obj).authoritiesIterator();
    }
}