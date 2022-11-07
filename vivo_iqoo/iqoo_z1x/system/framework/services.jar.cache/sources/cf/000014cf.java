package com.android.server.pm;

import android.content.pm.SharedLibraryInfo;
import java.util.function.BiConsumer;

/* compiled from: lambda */
/* renamed from: com.android.server.pm.-$$Lambda$PLzRNNUpYHZlGNIn1ofLtN374Ow  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$PLzRNNUpYHZlGNIn1ofLtN374Ow implements BiConsumer {
    public static final /* synthetic */ $$Lambda$PLzRNNUpYHZlGNIn1ofLtN374Ow INSTANCE = new $$Lambda$PLzRNNUpYHZlGNIn1ofLtN374Ow();

    private /* synthetic */ $$Lambda$PLzRNNUpYHZlGNIn1ofLtN374Ow() {
    }

    @Override // java.util.function.BiConsumer
    public final void accept(Object obj, Object obj2) {
        ((SharedLibraryInfo) obj).addDependency((SharedLibraryInfo) obj2);
    }
}