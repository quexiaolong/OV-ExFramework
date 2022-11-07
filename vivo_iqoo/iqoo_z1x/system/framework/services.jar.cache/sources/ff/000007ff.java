package com.android.server.appop;

import android.os.RemoteCallback;
import com.android.internal.util.function.DecConsumer;

/* compiled from: lambda */
/* renamed from: com.android.server.appop.-$$Lambda$ztf_FWUCLkjfRoVMTTWb7ZsjhNk  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$ztf_FWUCLkjfRoVMTTWb7ZsjhNk implements DecConsumer {
    public static final /* synthetic */ $$Lambda$ztf_FWUCLkjfRoVMTTWb7ZsjhNk INSTANCE = new $$Lambda$ztf_FWUCLkjfRoVMTTWb7ZsjhNk();

    private /* synthetic */ $$Lambda$ztf_FWUCLkjfRoVMTTWb7ZsjhNk() {
    }

    public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10) {
        ((HistoricalRegistry) obj).getHistoricalOpsFromDiskRaw(((Integer) obj2).intValue(), (String) obj3, (String) obj4, (String[]) obj5, ((Integer) obj6).intValue(), ((Long) obj7).longValue(), ((Long) obj8).longValue(), ((Integer) obj9).intValue(), (RemoteCallback) obj10);
    }
}