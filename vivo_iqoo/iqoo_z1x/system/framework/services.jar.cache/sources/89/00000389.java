package com.android.server;

import java.util.function.BiConsumer;

/* compiled from: lambda */
/* renamed from: com.android.server.-$$Lambda$PinnerService$3$RQBbrt9b8esLBxJImxDgVTsP34I  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$PinnerService$3$RQBbrt9b8esLBxJImxDgVTsP34I implements BiConsumer {
    public static final /* synthetic */ $$Lambda$PinnerService$3$RQBbrt9b8esLBxJImxDgVTsP34I INSTANCE = new $$Lambda$PinnerService$3$RQBbrt9b8esLBxJImxDgVTsP34I();

    private /* synthetic */ $$Lambda$PinnerService$3$RQBbrt9b8esLBxJImxDgVTsP34I() {
    }

    @Override // java.util.function.BiConsumer
    public final void accept(Object obj, Object obj2) {
        ((PinnerService) obj).handleUidGone(((Integer) obj2).intValue());
    }
}