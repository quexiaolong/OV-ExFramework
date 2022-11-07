package com.android.server.am;

import com.android.internal.util.function.QuadConsumer;

/* compiled from: lambda */
/* renamed from: com.android.server.am.-$$Lambda$OomAdjProfiler$oLbVP84ACmxo_1QlnwlSuhi91W4  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$OomAdjProfiler$oLbVP84ACmxo_1QlnwlSuhi91W4 implements QuadConsumer {
    public static final /* synthetic */ $$Lambda$OomAdjProfiler$oLbVP84ACmxo_1QlnwlSuhi91W4 INSTANCE = new $$Lambda$OomAdjProfiler$oLbVP84ACmxo_1QlnwlSuhi91W4();

    private /* synthetic */ $$Lambda$OomAdjProfiler$oLbVP84ACmxo_1QlnwlSuhi91W4() {
    }

    public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
        ((OomAdjProfiler) obj).updateSystemServerCpuTime(((Boolean) obj2).booleanValue(), ((Boolean) obj3).booleanValue(), ((Boolean) obj4).booleanValue());
    }
}