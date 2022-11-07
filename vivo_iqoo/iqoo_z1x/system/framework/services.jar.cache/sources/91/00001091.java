package com.android.server.location;

import android.util.ArraySet;
import com.android.server.SystemConfig;
import java.util.function.Supplier;

/* compiled from: lambda */
/* renamed from: com.android.server.location.-$$Lambda$SettingsHelper$DVmNGa9ypltgL35WVwJuSTIxRS8  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$SettingsHelper$DVmNGa9ypltgL35WVwJuSTIxRS8 implements Supplier {
    public static final /* synthetic */ $$Lambda$SettingsHelper$DVmNGa9ypltgL35WVwJuSTIxRS8 INSTANCE = new $$Lambda$SettingsHelper$DVmNGa9ypltgL35WVwJuSTIxRS8();

    private /* synthetic */ $$Lambda$SettingsHelper$DVmNGa9ypltgL35WVwJuSTIxRS8() {
    }

    @Override // java.util.function.Supplier
    public final Object get() {
        ArraySet allowUnthrottledLocation;
        allowUnthrottledLocation = SystemConfig.getInstance().getAllowUnthrottledLocation();
        return allowUnthrottledLocation;
    }
}