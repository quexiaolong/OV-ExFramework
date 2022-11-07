package com.android.server.media;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* loaded from: classes.dex */
public abstract class SessionPolicyProvider {
    static final int SESSION_POLICY_IGNORE_BUTTON_RECEIVER = 1;
    static final int SESSION_POLICY_IGNORE_BUTTON_SESSION = 2;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface SessionPolicy {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSessionPoliciesForApplication(int uid, String packageName) {
        return 0;
    }
}