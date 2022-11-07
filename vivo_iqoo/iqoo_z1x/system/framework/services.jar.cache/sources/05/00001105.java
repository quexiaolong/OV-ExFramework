package com.android.server.location.gnss;

import com.android.server.location.gnss.GnssConfiguration;

/* compiled from: lambda */
/* renamed from: com.android.server.location.gnss.-$$Lambda$GnssConfiguration$3$-8rPsr1Qu3pQ4-ZuFkOkp6tAmCg  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$GnssConfiguration$3$8rPsr1Qu3pQ4ZuFkOkp6tAmCg implements GnssConfiguration.SetCarrierProperty {
    public static final /* synthetic */ $$Lambda$GnssConfiguration$3$8rPsr1Qu3pQ4ZuFkOkp6tAmCg INSTANCE = new $$Lambda$GnssConfiguration$3$8rPsr1Qu3pQ4ZuFkOkp6tAmCg();

    private /* synthetic */ $$Lambda$GnssConfiguration$3$8rPsr1Qu3pQ4ZuFkOkp6tAmCg() {
    }

    @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
    public final boolean set(int i) {
        boolean native_set_supl_es;
        native_set_supl_es = GnssConfiguration.native_set_supl_es(i);
        return native_set_supl_es;
    }
}