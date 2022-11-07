package com.android.server.broadcastradio.hal2;

import android.hardware.radio.ProgramSelector;
import java.util.Objects;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.broadcastradio.hal2.-$$Lambda$orrX1qQ1nXd8k5pLkjug2DaCbzI  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$orrX1qQ1nXd8k5pLkjug2DaCbzI implements Function {
    public static final /* synthetic */ $$Lambda$orrX1qQ1nXd8k5pLkjug2DaCbzI INSTANCE = new $$Lambda$orrX1qQ1nXd8k5pLkjug2DaCbzI();

    private /* synthetic */ $$Lambda$orrX1qQ1nXd8k5pLkjug2DaCbzI() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        ProgramSelector.Identifier identifier = (ProgramSelector.Identifier) obj;
        Objects.requireNonNull(identifier);
        return identifier;
    }
}