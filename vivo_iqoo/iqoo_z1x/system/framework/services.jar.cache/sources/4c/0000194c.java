package com.android.server.stats.pull;

import android.app.AppOpsManager;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/* compiled from: lambda */
/* renamed from: com.android.server.stats.pull.-$$Lambda$wPejPqIRC0ueiw9uak8ULakT1R8  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$wPejPqIRC0ueiw9uak8ULakT1R8 implements Consumer {
    public final /* synthetic */ CompletableFuture f$0;

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        this.f$0.complete((AppOpsManager.HistoricalOps) obj);
    }
}