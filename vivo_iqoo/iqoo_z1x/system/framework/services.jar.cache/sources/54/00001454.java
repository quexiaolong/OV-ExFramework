package com.android.server.people.data;

import android.util.Range;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.people.data.-$$Lambda$EventIndex$OSX9HM2LXKK0pNoaI_v3ROQ6Z58  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$EventIndex$OSX9HM2LXKK0pNoaI_v3ROQ6Z58 implements Function {
    public static final /* synthetic */ $$Lambda$EventIndex$OSX9HM2LXKK0pNoaI_v3ROQ6Z58 INSTANCE = new $$Lambda$EventIndex$OSX9HM2LXKK0pNoaI_v3ROQ6Z58();

    private /* synthetic */ $$Lambda$EventIndex$OSX9HM2LXKK0pNoaI_v3ROQ6Z58() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        Range createOneHourLongTimeSlot;
        createOneHourLongTimeSlot = EventIndex.createOneHourLongTimeSlot(((Long) obj).longValue());
        return createOneHourLongTimeSlot;
    }
}