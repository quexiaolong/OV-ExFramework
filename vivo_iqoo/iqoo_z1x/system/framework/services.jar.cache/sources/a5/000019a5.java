package com.android.server.textclassifier;

import java.util.UUID;
import java.util.function.Supplier;

/* compiled from: lambda */
/* renamed from: com.android.server.textclassifier.-$$Lambda$IconsUriHelper$xs4gzwHiyi5M-NRelcf1JWo71zo  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$IconsUriHelper$xs4gzwHiyi5MNRelcf1JWo71zo implements Supplier {
    public static final /* synthetic */ $$Lambda$IconsUriHelper$xs4gzwHiyi5MNRelcf1JWo71zo INSTANCE = new $$Lambda$IconsUriHelper$xs4gzwHiyi5MNRelcf1JWo71zo();

    private /* synthetic */ $$Lambda$IconsUriHelper$xs4gzwHiyi5MNRelcf1JWo71zo() {
    }

    @Override // java.util.function.Supplier
    public final Object get() {
        String uuid;
        uuid = UUID.randomUUID().toString();
        return uuid;
    }
}