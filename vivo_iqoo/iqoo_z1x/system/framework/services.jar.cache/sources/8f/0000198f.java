package com.android.server.systemcaptions;

import java.util.function.Consumer;

/* compiled from: lambda */
/* renamed from: com.android.server.systemcaptions.-$$Lambda$FWiGrgnndUWGwX-f3Sn_9kgFkfk  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$FWiGrgnndUWGwXf3Sn_9kgFkfk implements Consumer {
    public static final /* synthetic */ $$Lambda$FWiGrgnndUWGwXf3Sn_9kgFkfk INSTANCE = new $$Lambda$FWiGrgnndUWGwXf3Sn_9kgFkfk();

    private /* synthetic */ $$Lambda$FWiGrgnndUWGwXf3Sn_9kgFkfk() {
    }

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        ((RemoteSystemCaptionsManagerService) obj).handleDestroy();
    }
}