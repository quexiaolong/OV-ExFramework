package com.android.server.hdmi;

import android.media.AudioFormat;
import java.util.function.IntFunction;

/* compiled from: lambda */
/* renamed from: com.android.server.hdmi.-$$Lambda$Q3K33XXIADNcvSncyQ2-wHWHi4c  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Q3K33XXIADNcvSncyQ2wHWHi4c implements IntFunction {
    public static final /* synthetic */ $$Lambda$Q3K33XXIADNcvSncyQ2wHWHi4c INSTANCE = new $$Lambda$Q3K33XXIADNcvSncyQ2wHWHi4c();

    private /* synthetic */ $$Lambda$Q3K33XXIADNcvSncyQ2wHWHi4c() {
    }

    @Override // java.util.function.IntFunction
    public final Object apply(int i) {
        return AudioFormat.toLogFriendlyEncoding(i);
    }
}