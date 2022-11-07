package com.android.server.integrity.engine;

import android.content.integrity.Rule;
import java.util.function.Predicate;

/* compiled from: lambda */
/* renamed from: com.android.server.integrity.engine.-$$Lambda$RuleEvaluator$_yl214m5sWGIgjBG_8qMT_pIqSI  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$RuleEvaluator$_yl214m5sWGIgjBG_8qMT_pIqSI implements Predicate {
    public static final /* synthetic */ $$Lambda$RuleEvaluator$_yl214m5sWGIgjBG_8qMT_pIqSI INSTANCE = new $$Lambda$RuleEvaluator$_yl214m5sWGIgjBG_8qMT_pIqSI();

    private /* synthetic */ $$Lambda$RuleEvaluator$_yl214m5sWGIgjBG_8qMT_pIqSI() {
    }

    @Override // java.util.function.Predicate
    public final boolean test(Object obj) {
        return RuleEvaluator.lambda$evaluateRules$2((Rule) obj);
    }
}