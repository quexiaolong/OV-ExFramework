package com.android.server.integrity.serializer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class RuleIndexingDetails {
    static final int APP_CERTIFICATE_INDEXED = 2;
    static final String DEFAULT_RULE_KEY = "N/A";
    static final int NOT_INDEXED = 0;
    static final int PACKAGE_NAME_INDEXED = 1;
    private int mIndexType;
    private String mRuleKey;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface IndexType {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RuleIndexingDetails(int indexType) {
        this.mIndexType = indexType;
        this.mRuleKey = DEFAULT_RULE_KEY;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RuleIndexingDetails(int indexType, String ruleKey) {
        this.mIndexType = indexType;
        this.mRuleKey = ruleKey;
    }

    public int getIndexType() {
        return this.mIndexType;
    }

    public String getRuleKey() {
        return this.mRuleKey;
    }
}