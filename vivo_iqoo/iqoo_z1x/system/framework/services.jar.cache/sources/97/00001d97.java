package com.android.server.wm;

import android.app.ActivityManagerInternal;
import com.android.internal.util.function.TriConsumer;

/* compiled from: lambda */
/* renamed from: com.android.server.wm.-$$Lambda$swA_sUfSJdP8eC8AA9Iby3-SuOY  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$swA_sUfSJdP8eC8AA9Iby3SuOY implements TriConsumer {
    public static final /* synthetic */ $$Lambda$swA_sUfSJdP8eC8AA9Iby3SuOY INSTANCE = new $$Lambda$swA_sUfSJdP8eC8AA9Iby3SuOY();

    private /* synthetic */ $$Lambda$swA_sUfSJdP8eC8AA9Iby3SuOY() {
    }

    public final void accept(Object obj, Object obj2, Object obj3) {
        ((ActivityManagerInternal) obj).broadcastGlobalConfigurationChanged(((Integer) obj2).intValue(), ((Boolean) obj3).booleanValue());
    }
}