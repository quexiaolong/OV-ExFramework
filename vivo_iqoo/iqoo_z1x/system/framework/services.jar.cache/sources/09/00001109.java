package com.android.server.location.gnss;

import com.android.server.location.gnss.GnssConfiguration;

/* compiled from: lambda */
/* renamed from: com.android.server.location.gnss.-$$Lambda$GnssConfiguration$3$asYDZWbJH-Uv1XJjQXTa7G2qg-Y  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$GnssConfiguration$3$asYDZWbJHUv1XJjQXTa7G2qgY implements GnssConfiguration.SetCarrierProperty {
    public static final /* synthetic */ $$Lambda$GnssConfiguration$3$asYDZWbJHUv1XJjQXTa7G2qgY INSTANCE = new $$Lambda$GnssConfiguration$3$asYDZWbJHUv1XJjQXTa7G2qgY();

    private /* synthetic */ $$Lambda$GnssConfiguration$3$asYDZWbJHUv1XJjQXTa7G2qgY() {
    }

    @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
    public final boolean set(int i) {
        boolean native_set_gnss_pos_protocol_select;
        native_set_gnss_pos_protocol_select = GnssConfiguration.native_set_gnss_pos_protocol_select(i);
        return native_set_gnss_pos_protocol_select;
    }
}