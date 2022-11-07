package com.android.server.autofill;

import android.os.IBinder;
import android.service.autofill.augmented.IAugmentedAutofillService;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.autofill.-$$Lambda$sdnPz1IsKKVKSEXwI7z4h2-SxiM  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$sdnPz1IsKKVKSEXwI7z4h2SxiM implements Function {
    public static final /* synthetic */ $$Lambda$sdnPz1IsKKVKSEXwI7z4h2SxiM INSTANCE = new $$Lambda$sdnPz1IsKKVKSEXwI7z4h2SxiM();

    private /* synthetic */ $$Lambda$sdnPz1IsKKVKSEXwI7z4h2SxiM() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return IAugmentedAutofillService.Stub.asInterface((IBinder) obj);
    }
}