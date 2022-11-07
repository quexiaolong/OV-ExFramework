package com.android.server.systemcaptions;

import java.util.function.Consumer;

/* compiled from: lambda */
/* renamed from: com.android.server.systemcaptions.-$$Lambda$RemoteSystemCaptionsManagerService$P9HS4I_OwuvbenRQbLezxI-qmx8  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$RemoteSystemCaptionsManagerService$P9HS4I_OwuvbenRQbLezxIqmx8 implements Consumer {
    public static final /* synthetic */ $$Lambda$RemoteSystemCaptionsManagerService$P9HS4I_OwuvbenRQbLezxIqmx8 INSTANCE = new $$Lambda$RemoteSystemCaptionsManagerService$P9HS4I_OwuvbenRQbLezxIqmx8();

    private /* synthetic */ $$Lambda$RemoteSystemCaptionsManagerService$P9HS4I_OwuvbenRQbLezxIqmx8() {
    }

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        ((RemoteSystemCaptionsManagerService) obj).handleEnsureBound();
    }
}