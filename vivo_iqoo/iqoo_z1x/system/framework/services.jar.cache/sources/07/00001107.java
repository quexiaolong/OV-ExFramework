package com.android.server.location.gnss;

import com.android.server.location.gnss.GnssConfiguration;

/* compiled from: lambda */
/* renamed from: com.android.server.location.gnss.-$$Lambda$GnssConfiguration$3$9oYv2aOkEOYfR-c6t6z5q1C2SGo  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$GnssConfiguration$3$9oYv2aOkEOYfRc6t6z5q1C2SGo implements GnssConfiguration.SetCarrierProperty {
    public static final /* synthetic */ $$Lambda$GnssConfiguration$3$9oYv2aOkEOYfRc6t6z5q1C2SGo INSTANCE = new $$Lambda$GnssConfiguration$3$9oYv2aOkEOYfRc6t6z5q1C2SGo();

    private /* synthetic */ $$Lambda$GnssConfiguration$3$9oYv2aOkEOYfRc6t6z5q1C2SGo() {
    }

    @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
    public final boolean set(int i) {
        boolean native_set_lpp_profile;
        native_set_lpp_profile = GnssConfiguration.native_set_lpp_profile(i);
        return native_set_lpp_profile;
    }
}