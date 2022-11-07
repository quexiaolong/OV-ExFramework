package com.android.server.integrity.model;

import android.content.integrity.Rule;
import java.util.function.Predicate;

/* compiled from: lambda */
/* renamed from: com.android.server.integrity.model.-$$Lambda$IntegrityCheckResult$uw4WN-XjK2pJvNXIEB_RL21qEcg  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$IntegrityCheckResult$uw4WNXjK2pJvNXIEB_RL21qEcg implements Predicate {
    public static final /* synthetic */ $$Lambda$IntegrityCheckResult$uw4WNXjK2pJvNXIEB_RL21qEcg INSTANCE = new $$Lambda$IntegrityCheckResult$uw4WNXjK2pJvNXIEB_RL21qEcg();

    private /* synthetic */ $$Lambda$IntegrityCheckResult$uw4WNXjK2pJvNXIEB_RL21qEcg() {
    }

    @Override // java.util.function.Predicate
    public final boolean test(Object obj) {
        boolean isInstallerFormula;
        isInstallerFormula = ((Rule) obj).getFormula().isInstallerFormula();
        return isInstallerFormula;
    }
}