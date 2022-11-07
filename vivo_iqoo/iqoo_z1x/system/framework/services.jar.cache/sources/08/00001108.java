package com.android.server.location.gnss;

import com.android.server.location.gnss.GnssConfiguration;

/* compiled from: lambda */
/* renamed from: com.android.server.location.gnss.-$$Lambda$GnssConfiguration$3$QHpnPo-fcCD4Q6pRu_I-W9e-aP0  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$GnssConfiguration$3$QHpnPofcCD4Q6pRu_IW9eaP0 implements GnssConfiguration.SetCarrierProperty {
    public static final /* synthetic */ $$Lambda$GnssConfiguration$3$QHpnPofcCD4Q6pRu_IW9eaP0 INSTANCE = new $$Lambda$GnssConfiguration$3$QHpnPofcCD4Q6pRu_IW9eaP0();

    private /* synthetic */ $$Lambda$GnssConfiguration$3$QHpnPofcCD4Q6pRu_IW9eaP0() {
    }

    @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
    public final boolean set(int i) {
        boolean native_set_emergency_supl_pdn;
        native_set_emergency_supl_pdn = GnssConfiguration.native_set_emergency_supl_pdn(i);
        return native_set_emergency_supl_pdn;
    }
}