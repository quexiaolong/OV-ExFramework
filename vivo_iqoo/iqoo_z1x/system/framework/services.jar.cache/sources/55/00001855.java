package com.android.server.protolog;

import com.android.server.protolog.common.IProtoLogGroup;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.protolog.-$$Lambda$PfxMAktVLMbQMPp_FRkrQxibSKE  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$PfxMAktVLMbQMPp_FRkrQxibSKE implements Function {
    public static final /* synthetic */ $$Lambda$PfxMAktVLMbQMPp_FRkrQxibSKE INSTANCE = new $$Lambda$PfxMAktVLMbQMPp_FRkrQxibSKE();

    private /* synthetic */ $$Lambda$PfxMAktVLMbQMPp_FRkrQxibSKE() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return ((IProtoLogGroup) obj).name();
    }
}