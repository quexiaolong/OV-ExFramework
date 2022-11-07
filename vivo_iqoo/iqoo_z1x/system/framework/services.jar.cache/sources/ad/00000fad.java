package com.android.server.integrity.engine;

import android.content.integrity.Rule;
import java.util.function.Predicate;

/* compiled from: lambda */
/* renamed from: com.android.server.integrity.engine.-$$Lambda$RuleEvaluator$_b_bnHZ6Lv_0UPoz1qRhvn2moQI  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$RuleEvaluator$_b_bnHZ6Lv_0UPoz1qRhvn2moQI implements Predicate {
    public static final /* synthetic */ $$Lambda$RuleEvaluator$_b_bnHZ6Lv_0UPoz1qRhvn2moQI INSTANCE = new $$Lambda$RuleEvaluator$_b_bnHZ6Lv_0UPoz1qRhvn2moQI();

    private /* synthetic */ $$Lambda$RuleEvaluator$_b_bnHZ6Lv_0UPoz1qRhvn2moQI() {
    }

    @Override // java.util.function.Predicate
    public final boolean test(Object obj) {
        return RuleEvaluator.lambda$evaluateRules$1((Rule) obj);
    }
}