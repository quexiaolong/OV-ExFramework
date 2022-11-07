package com.android.server.people.data;

import android.util.Range;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.people.data.-$$Lambda$EventIndex$5vJ4iTv1E2na1FXUge8q9OUVsxo  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$EventIndex$5vJ4iTv1E2na1FXUge8q9OUVsxo implements Function {
    public static final /* synthetic */ $$Lambda$EventIndex$5vJ4iTv1E2na1FXUge8q9OUVsxo INSTANCE = new $$Lambda$EventIndex$5vJ4iTv1E2na1FXUge8q9OUVsxo();

    private /* synthetic */ $$Lambda$EventIndex$5vJ4iTv1E2na1FXUge8q9OUVsxo() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        Range createTwoMinutesLongTimeSlot;
        createTwoMinutesLongTimeSlot = EventIndex.createTwoMinutesLongTimeSlot(((Long) obj).longValue());
        return createTwoMinutesLongTimeSlot;
    }
}