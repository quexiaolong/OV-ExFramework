package com.android.server.pm;

import android.content.pm.parsing.component.ParsedIntentInfo;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.pm.-$$Lambda$YY245IBQr5Qygm_NJ7MG_oIzCHk  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$YY245IBQr5Qygm_NJ7MG_oIzCHk implements Function {
    public static final /* synthetic */ $$Lambda$YY245IBQr5Qygm_NJ7MG_oIzCHk INSTANCE = new $$Lambda$YY245IBQr5Qygm_NJ7MG_oIzCHk();

    private /* synthetic */ $$Lambda$YY245IBQr5Qygm_NJ7MG_oIzCHk() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return ((ParsedIntentInfo) obj).actionsIterator();
    }
}