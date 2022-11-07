package com.android.server.wm;

import java.util.function.BiConsumer;

/* compiled from: lambda */
/* renamed from: com.android.server.wm.-$$Lambda$LI60v4Y5Me6khV12IZ-zEQtSx7A  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$LI60v4Y5Me6khV12IZzEQtSx7A implements BiConsumer {
    public static final /* synthetic */ $$Lambda$LI60v4Y5Me6khV12IZzEQtSx7A INSTANCE = new $$Lambda$LI60v4Y5Me6khV12IZzEQtSx7A();

    private /* synthetic */ $$Lambda$LI60v4Y5Me6khV12IZzEQtSx7A() {
    }

    @Override // java.util.function.BiConsumer
    public final void accept(Object obj, Object obj2) {
        ((WindowProcessListener) obj).setPendingUiCleanAndForceProcessStateUpTo(((Integer) obj2).intValue());
    }
}