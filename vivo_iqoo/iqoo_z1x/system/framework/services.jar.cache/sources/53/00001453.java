package com.android.server.people.data;

import android.util.Range;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.people.data.-$$Lambda$EventIndex$Nd5ot_vT3MfYlbajA1zcoqOlGW8  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$EventIndex$Nd5ot_vT3MfYlbajA1zcoqOlGW8 implements Function {
    public static final /* synthetic */ $$Lambda$EventIndex$Nd5ot_vT3MfYlbajA1zcoqOlGW8 INSTANCE = new $$Lambda$EventIndex$Nd5ot_vT3MfYlbajA1zcoqOlGW8();

    private /* synthetic */ $$Lambda$EventIndex$Nd5ot_vT3MfYlbajA1zcoqOlGW8() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        Range createFourHoursLongTimeSlot;
        createFourHoursLongTimeSlot = EventIndex.createFourHoursLongTimeSlot(((Long) obj).longValue());
        return createFourHoursLongTimeSlot;
    }
}