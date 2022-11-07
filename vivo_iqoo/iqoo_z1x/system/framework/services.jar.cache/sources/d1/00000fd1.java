package com.android.server.integrity.serializer;

import android.content.integrity.AtomicFormula;
import android.content.integrity.CompoundFormula;
import android.content.integrity.IntegrityFormula;
import android.content.integrity.Rule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/* loaded from: classes.dex */
class RuleIndexingDetailsIdentifier {
    RuleIndexingDetailsIdentifier() {
    }

    public static Map<Integer, Map<String, List<Rule>>> splitRulesIntoIndexBuckets(List<Rule> rules) {
        if (rules == null) {
            throw new IllegalArgumentException("Index buckets cannot be created for null rule list.");
        }
        Map<Integer, Map<String, List<Rule>>> typeOrganizedRuleMap = new HashMap<>();
        typeOrganizedRuleMap.put(0, new HashMap<>());
        typeOrganizedRuleMap.put(1, new HashMap<>());
        typeOrganizedRuleMap.put(2, new HashMap<>());
        for (Rule rule : rules) {
            try {
                RuleIndexingDetails indexingDetails = getIndexingDetails(rule.getFormula());
                int ruleIndexType = indexingDetails.getIndexType();
                String ruleKey = indexingDetails.getRuleKey();
                if (!typeOrganizedRuleMap.get(Integer.valueOf(ruleIndexType)).containsKey(ruleKey)) {
                    typeOrganizedRuleMap.get(Integer.valueOf(ruleIndexType)).put(ruleKey, new ArrayList<>());
                }
                typeOrganizedRuleMap.get(Integer.valueOf(ruleIndexType)).get(ruleKey).add(rule);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("Malformed rule identified. [%s]", rule.toString()));
            }
        }
        return typeOrganizedRuleMap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static RuleIndexingDetails getIndexingDetails(IntegrityFormula formula) {
        int tag = formula.getTag();
        if (tag != 0) {
            if (tag != 1) {
                if (tag == 2 || tag == 3 || tag == 4) {
                    return new RuleIndexingDetails(0);
                }
                throw new IllegalArgumentException(String.format("Invalid formula tag type: %s", Integer.valueOf(formula.getTag())));
            }
            return getIndexingDetailsForStringAtomicFormula((AtomicFormula.StringAtomicFormula) formula);
        }
        return getIndexingDetailsForCompoundFormula((CompoundFormula) formula);
    }

    private static RuleIndexingDetails getIndexingDetailsForCompoundFormula(CompoundFormula compoundFormula) {
        int connector = compoundFormula.getConnector();
        List<IntegrityFormula> formulas = compoundFormula.getFormulas();
        if (connector == 0 || connector == 1) {
            Optional<RuleIndexingDetails> packageNameRule = formulas.stream().map($$Lambda$RuleIndexingDetailsIdentifier$pIB6CD1IxMPhoIxtBpzBs5iPv6s.INSTANCE).filter($$Lambda$RuleIndexingDetailsIdentifier$9Og8AMtBlYIqTH1ZkWMuKL0jdhA.INSTANCE).findAny();
            if (packageNameRule.isPresent()) {
                return packageNameRule.get();
            }
            Optional<RuleIndexingDetails> appCertificateRule = formulas.stream().map($$Lambda$RuleIndexingDetailsIdentifier$QjEbQG4grYc2sxy8s7FRimeOEI.INSTANCE).filter($$Lambda$RuleIndexingDetailsIdentifier$HOqHZdSqJBC63MqtcWCQnluE7Q.INSTANCE).findAny();
            if (appCertificateRule.isPresent()) {
                return appCertificateRule.get();
            }
            return new RuleIndexingDetails(0);
        }
        return new RuleIndexingDetails(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getIndexingDetailsForCompoundFormula$1(RuleIndexingDetails ruleIndexingDetails) {
        return ruleIndexingDetails.getIndexType() == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getIndexingDetailsForCompoundFormula$3(RuleIndexingDetails ruleIndexingDetails) {
        return ruleIndexingDetails.getIndexType() == 2;
    }

    private static RuleIndexingDetails getIndexingDetailsForStringAtomicFormula(AtomicFormula.StringAtomicFormula atomicFormula) {
        int key = atomicFormula.getKey();
        if (key != 0) {
            if (key == 1) {
                return new RuleIndexingDetails(2, atomicFormula.getValue());
            }
            return new RuleIndexingDetails(0);
        }
        return new RuleIndexingDetails(1, atomicFormula.getValue());
    }
}