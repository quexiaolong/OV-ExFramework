package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
/* renamed from: com.android.server.wm.-$$Lambda$HLz_SQuxQoIiuaK5SB5xJ6FnoxY  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$HLz_SQuxQoIiuaK5SB5xJ6FnoxY implements Consumer {
    public static final /* synthetic */ $$Lambda$HLz_SQuxQoIiuaK5SB5xJ6FnoxY INSTANCE = new $$Lambda$HLz_SQuxQoIiuaK5SB5xJ6FnoxY();

    private /* synthetic */ $$Lambda$HLz_SQuxQoIiuaK5SB5xJ6FnoxY() {
    }

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        ((WindowProcessListener) obj).updateServiceConnectionActivities();
    }
}