package com.android.server.autofill;

import android.os.IBinder;
import android.service.autofill.IAutoFillService;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.autofill.-$$Lambda$Q-iZrXrDBZAnj-gnbNOhH00i8uU  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$QiZrXrDBZAnjgnbNOhH00i8uU implements Function {
    public static final /* synthetic */ $$Lambda$QiZrXrDBZAnjgnbNOhH00i8uU INSTANCE = new $$Lambda$QiZrXrDBZAnjgnbNOhH00i8uU();

    private /* synthetic */ $$Lambda$QiZrXrDBZAnjgnbNOhH00i8uU() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return IAutoFillService.Stub.asInterface((IBinder) obj);
    }
}