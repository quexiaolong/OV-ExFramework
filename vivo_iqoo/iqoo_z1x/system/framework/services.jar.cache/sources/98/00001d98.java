package com.android.server.wm;

import java.util.function.BiConsumer;

/* compiled from: lambda */
/* renamed from: com.android.server.wm.-$$Lambda$uwO6wQlqU3CG7OTdH7NBCKnHs64  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$uwO6wQlqU3CG7OTdH7NBCKnHs64 implements BiConsumer {
    public static final /* synthetic */ $$Lambda$uwO6wQlqU3CG7OTdH7NBCKnHs64 INSTANCE = new $$Lambda$uwO6wQlqU3CG7OTdH7NBCKnHs64();

    private /* synthetic */ $$Lambda$uwO6wQlqU3CG7OTdH7NBCKnHs64() {
    }

    @Override // java.util.function.BiConsumer
    public final void accept(Object obj, Object obj2) {
        ((WindowProcessListener) obj).setRunningRemoteAnimation(((Boolean) obj2).booleanValue());
    }
}