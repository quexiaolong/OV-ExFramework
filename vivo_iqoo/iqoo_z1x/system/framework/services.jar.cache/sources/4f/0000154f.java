package com.android.server.pm;

import android.content.pm.parsing.component.ParsedIntentInfo;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.pm.-$$Lambda$bpFcEVMboFCYFnC3BHdOPCQV19Y  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$bpFcEVMboFCYFnC3BHdOPCQV19Y implements Function {
    public static final /* synthetic */ $$Lambda$bpFcEVMboFCYFnC3BHdOPCQV19Y INSTANCE = new $$Lambda$bpFcEVMboFCYFnC3BHdOPCQV19Y();

    private /* synthetic */ $$Lambda$bpFcEVMboFCYFnC3BHdOPCQV19Y() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return ((ParsedIntentInfo) obj).schemesIterator();
    }
}