package com.android.server.location;

import android.util.ArraySet;
import com.android.server.SystemConfig;
import java.util.function.Supplier;

/* compiled from: lambda */
/* renamed from: com.android.server.location.-$$Lambda$SettingsHelper$Ez8giHaZAPYwS7zICeUtrlXPpBo  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$SettingsHelper$Ez8giHaZAPYwS7zICeUtrlXPpBo implements Supplier {
    public static final /* synthetic */ $$Lambda$SettingsHelper$Ez8giHaZAPYwS7zICeUtrlXPpBo INSTANCE = new $$Lambda$SettingsHelper$Ez8giHaZAPYwS7zICeUtrlXPpBo();

    private /* synthetic */ $$Lambda$SettingsHelper$Ez8giHaZAPYwS7zICeUtrlXPpBo() {
    }

    @Override // java.util.function.Supplier
    public final Object get() {
        ArraySet allowIgnoreLocationSettings;
        allowIgnoreLocationSettings = SystemConfig.getInstance().getAllowIgnoreLocationSettings();
        return allowIgnoreLocationSettings;
    }
}