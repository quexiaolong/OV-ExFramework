package com.android.server.people.data;

import android.util.Range;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.people.data.-$$Lambda$EventIndex$G8WkLHrQiIIwWFEZDn-UhnYOqD4  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$EventIndex$G8WkLHrQiIIwWFEZDnUhnYOqD4 implements Function {
    public static final /* synthetic */ $$Lambda$EventIndex$G8WkLHrQiIIwWFEZDnUhnYOqD4 INSTANCE = new $$Lambda$EventIndex$G8WkLHrQiIIwWFEZDnUhnYOqD4();

    private /* synthetic */ $$Lambda$EventIndex$G8WkLHrQiIIwWFEZDnUhnYOqD4() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        Range createOneDayLongTimeSlot;
        createOneDayLongTimeSlot = EventIndex.createOneDayLongTimeSlot(((Long) obj).longValue());
        return createOneDayLongTimeSlot;
    }
}