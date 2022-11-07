package com.android.server.integrity.model;

import android.content.integrity.Rule;
import java.util.function.Predicate;

/* compiled from: lambda */
/* renamed from: com.android.server.integrity.model.-$$Lambda$IntegrityCheckResult$Cdma_yQnvj3lcPg1ximae51_zEo  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$IntegrityCheckResult$Cdma_yQnvj3lcPg1ximae51_zEo implements Predicate {
    public static final /* synthetic */ $$Lambda$IntegrityCheckResult$Cdma_yQnvj3lcPg1ximae51_zEo INSTANCE = new $$Lambda$IntegrityCheckResult$Cdma_yQnvj3lcPg1ximae51_zEo();

    private /* synthetic */ $$Lambda$IntegrityCheckResult$Cdma_yQnvj3lcPg1ximae51_zEo() {
    }

    @Override // java.util.function.Predicate
    public final boolean test(Object obj) {
        boolean isAppCertificateFormula;
        isAppCertificateFormula = ((Rule) obj).getFormula().isAppCertificateFormula();
        return isAppCertificateFormula;
    }
}