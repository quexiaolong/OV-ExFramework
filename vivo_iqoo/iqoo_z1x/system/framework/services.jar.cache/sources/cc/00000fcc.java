package com.android.server.integrity.serializer;

import android.content.integrity.IntegrityFormula;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.integrity.serializer.-$$Lambda$RuleIndexingDetailsIdentifier$pIB6CD1IxMPhoIxtBpzBs5iPv6s  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$RuleIndexingDetailsIdentifier$pIB6CD1IxMPhoIxtBpzBs5iPv6s implements Function {
    public static final /* synthetic */ $$Lambda$RuleIndexingDetailsIdentifier$pIB6CD1IxMPhoIxtBpzBs5iPv6s INSTANCE = new $$Lambda$RuleIndexingDetailsIdentifier$pIB6CD1IxMPhoIxtBpzBs5iPv6s();

    private /* synthetic */ $$Lambda$RuleIndexingDetailsIdentifier$pIB6CD1IxMPhoIxtBpzBs5iPv6s() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        RuleIndexingDetails indexingDetails;
        indexingDetails = RuleIndexingDetailsIdentifier.getIndexingDetails((IntegrityFormula) obj);
        return indexingDetails;
    }
}