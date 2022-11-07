package com.android.server.appop;

import java.util.function.Consumer;

/* compiled from: lambda */
/* renamed from: com.android.server.appop.-$$Lambda$bQMBlCyJOKKFDz59ICFPuj1hKGE  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$bQMBlCyJOKKFDz59ICFPuj1hKGE implements Consumer {
    public static final /* synthetic */ $$Lambda$bQMBlCyJOKKFDz59ICFPuj1hKGE INSTANCE = new $$Lambda$bQMBlCyJOKKFDz59ICFPuj1hKGE();

    private /* synthetic */ $$Lambda$bQMBlCyJOKKFDz59ICFPuj1hKGE() {
    }

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        ((HistoricalRegistry) obj).persistPendingHistory();
    }
}