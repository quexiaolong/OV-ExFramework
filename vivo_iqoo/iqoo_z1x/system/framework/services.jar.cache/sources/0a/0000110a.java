package com.android.server.location.gnss;

import com.android.server.location.gnss.GnssConfiguration;

/* compiled from: lambda */
/* renamed from: com.android.server.location.gnss.-$$Lambda$GnssConfiguration$3$coaX28wgwR1rFNOMom2UbsZv5jM  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$GnssConfiguration$3$coaX28wgwR1rFNOMom2UbsZv5jM implements GnssConfiguration.SetCarrierProperty {
    public static final /* synthetic */ $$Lambda$GnssConfiguration$3$coaX28wgwR1rFNOMom2UbsZv5jM INSTANCE = new $$Lambda$GnssConfiguration$3$coaX28wgwR1rFNOMom2UbsZv5jM();

    private /* synthetic */ $$Lambda$GnssConfiguration$3$coaX28wgwR1rFNOMom2UbsZv5jM() {
    }

    @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
    public final boolean set(int i) {
        boolean native_set_gps_lock;
        native_set_gps_lock = GnssConfiguration.native_set_gps_lock(i);
        return native_set_gps_lock;
    }
}