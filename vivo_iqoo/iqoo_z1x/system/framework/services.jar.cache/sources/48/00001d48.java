package com.android.server.wm;

import java.util.function.BiConsumer;

/* compiled from: lambda */
/* renamed from: com.android.server.wm.-$$Lambda$VDG7MoD_7v7qIdkguJXls8nmhGU  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$VDG7MoD_7v7qIdkguJXls8nmhGU implements BiConsumer {
    public static final /* synthetic */ $$Lambda$VDG7MoD_7v7qIdkguJXls8nmhGU INSTANCE = new $$Lambda$VDG7MoD_7v7qIdkguJXls8nmhGU();

    private /* synthetic */ $$Lambda$VDG7MoD_7v7qIdkguJXls8nmhGU() {
    }

    @Override // java.util.function.BiConsumer
    public final void accept(Object obj, Object obj2) {
        ((WindowProcessListener) obj).appDied((String) obj2);
    }
}