package com.android.server.integrity.engine;

import android.content.integrity.AppInstallMetadata;
import android.content.integrity.Rule;
import com.android.server.integrity.model.IntegrityCheckResult;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/* loaded from: classes.dex */
final class RuleEvaluator {
    RuleEvaluator() {
    }

    public static IntegrityCheckResult evaluateRules(List<Rule> rules, final AppInstallMetadata appInstallMetadata) {
        List<Rule> matchedRules = (List) rules.stream().filter(new Predicate() { // from class: com.android.server.integrity.engine.-$$Lambda$RuleEvaluator$unAwA1sQfXbWYCFQp7qIaNkgC10
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RuleEvaluator.lambda$evaluateRules$0(appInstallMetadata, (Rule) obj);
            }
        }).collect(Collectors.toList());
        List<Rule> matchedPowerAllowRules = (List) matchedRules.stream().filter($$Lambda$RuleEvaluator$_b_bnHZ6Lv_0UPoz1qRhvn2moQI.INSTANCE).collect(Collectors.toList());
        if (!matchedPowerAllowRules.isEmpty()) {
            return IntegrityCheckResult.allow(matchedPowerAllowRules);
        }
        List<Rule> matchedDenyRules = (List) matchedRules.stream().filter($$Lambda$RuleEvaluator$_yl214m5sWGIgjBG_8qMT_pIqSI.INSTANCE).collect(Collectors.toList());
        if (!matchedDenyRules.isEmpty()) {
            return IntegrityCheckResult.deny(matchedDenyRules);
        }
        return IntegrityCheckResult.allow();
    }

    public static /* synthetic */ boolean lambda$evaluateRules$0(AppInstallMetadata appInstallMetadata, Rule rule) {
        return rule.getFormula().matches(appInstallMetadata);
    }

    public static /* synthetic */ boolean lambda$evaluateRules$1(Rule rule) {
        return rule.getEffect() == 1;
    }

    public static /* synthetic */ boolean lambda$evaluateRules$2(Rule rule) {
        return rule.getEffect() == 0;
    }
}