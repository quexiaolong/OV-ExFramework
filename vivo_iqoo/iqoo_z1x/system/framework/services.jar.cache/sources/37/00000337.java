package com.android.server;

import android.os.Binder;
import com.android.internal.os.BinderInternal;

/* compiled from: lambda */
/* renamed from: com.android.server.-$$Lambda$BinderCallsStatsService$SettingsObserver$bif9uA0lzoT6htcKe6MNsrH_ha4  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$BinderCallsStatsService$SettingsObserver$bif9uA0lzoT6htcKe6MNsrH_ha4 implements BinderInternal.WorkSourceProvider {
    public static final /* synthetic */ $$Lambda$BinderCallsStatsService$SettingsObserver$bif9uA0lzoT6htcKe6MNsrH_ha4 INSTANCE = new $$Lambda$BinderCallsStatsService$SettingsObserver$bif9uA0lzoT6htcKe6MNsrH_ha4();

    private /* synthetic */ $$Lambda$BinderCallsStatsService$SettingsObserver$bif9uA0lzoT6htcKe6MNsrH_ha4() {
    }

    public final int resolveWorkSourceUid(int i) {
        int callingUid;
        callingUid = Binder.getCallingUid();
        return callingUid;
    }
}