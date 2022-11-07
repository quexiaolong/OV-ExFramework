package com.android.server.location.gnss;

import com.android.server.location.gnss.GnssConfiguration;

/* compiled from: lambda */
/* renamed from: com.android.server.location.gnss.-$$Lambda$GnssConfiguration$3$5KAZXMidKHgmCx-mlFmT8sGpFEE  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$GnssConfiguration$3$5KAZXMidKHgmCxmlFmT8sGpFEE implements GnssConfiguration.SetCarrierProperty {
    public static final /* synthetic */ $$Lambda$GnssConfiguration$3$5KAZXMidKHgmCxmlFmT8sGpFEE INSTANCE = new $$Lambda$GnssConfiguration$3$5KAZXMidKHgmCxmlFmT8sGpFEE();

    private /* synthetic */ $$Lambda$GnssConfiguration$3$5KAZXMidKHgmCxmlFmT8sGpFEE() {
    }

    @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
    public final boolean set(int i) {
        boolean native_set_supl_mode;
        native_set_supl_mode = GnssConfiguration.native_set_supl_mode(i);
        return native_set_supl_mode;
    }
}