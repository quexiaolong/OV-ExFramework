package com.android.server.integrity.serializer;

import android.content.integrity.IntegrityFormula;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.integrity.serializer.-$$Lambda$RuleIndexingDetailsIdentifier$QjEbQG4grYc2sxy-8s7FRimeOEI  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$RuleIndexingDetailsIdentifier$QjEbQG4grYc2sxy8s7FRimeOEI implements Function {
    public static final /* synthetic */ $$Lambda$RuleIndexingDetailsIdentifier$QjEbQG4grYc2sxy8s7FRimeOEI INSTANCE = new $$Lambda$RuleIndexingDetailsIdentifier$QjEbQG4grYc2sxy8s7FRimeOEI();

    private /* synthetic */ $$Lambda$RuleIndexingDetailsIdentifier$QjEbQG4grYc2sxy8s7FRimeOEI() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        RuleIndexingDetails indexingDetails;
        indexingDetails = RuleIndexingDetailsIdentifier.getIndexingDetails((IntegrityFormula) obj);
        return indexingDetails;
    }
}