package com.android.server.location.gnss;

import com.android.server.location.gnss.GnssConfiguration;

/* compiled from: lambda */
/* renamed from: com.android.server.location.gnss.-$$Lambda$GnssConfiguration$3$lLw-CkN9VuTe91diDEr_D-rYYmo  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$GnssConfiguration$3$lLwCkN9VuTe91diDEr_DrYYmo implements GnssConfiguration.SetCarrierProperty {
    public static final /* synthetic */ $$Lambda$GnssConfiguration$3$lLwCkN9VuTe91diDEr_DrYYmo INSTANCE = new $$Lambda$GnssConfiguration$3$lLwCkN9VuTe91diDEr_DrYYmo();

    private /* synthetic */ $$Lambda$GnssConfiguration$3$lLwCkN9VuTe91diDEr_DrYYmo() {
    }

    @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
    public final boolean set(int i) {
        boolean native_set_supl_version;
        native_set_supl_version = GnssConfiguration.native_set_supl_version(i);
        return native_set_supl_version;
    }
}