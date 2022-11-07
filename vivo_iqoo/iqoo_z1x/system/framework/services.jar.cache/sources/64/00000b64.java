package com.android.server.companion;

import android.companion.ICompanionDeviceDiscoveryService;
import android.os.IBinder;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.companion.-$$Lambda$dmgYbfK3c1MAswkxujxbcRtjs9A  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$dmgYbfK3c1MAswkxujxbcRtjs9A implements Function {
    public static final /* synthetic */ $$Lambda$dmgYbfK3c1MAswkxujxbcRtjs9A INSTANCE = new $$Lambda$dmgYbfK3c1MAswkxujxbcRtjs9A();

    private /* synthetic */ $$Lambda$dmgYbfK3c1MAswkxujxbcRtjs9A() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return ICompanionDeviceDiscoveryService.Stub.asInterface((IBinder) obj);
    }
}