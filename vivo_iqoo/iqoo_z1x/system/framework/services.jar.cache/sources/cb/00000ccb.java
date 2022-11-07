package com.android.server.devicepolicy;

import android.app.admin.SecurityLog;
import java.util.Comparator;

/* compiled from: lambda */
/* renamed from: com.android.server.devicepolicy.-$$Lambda$SecurityLogMonitor$y5Q3dMmmJ8bk5nBh8WR2MUroKrI  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$SecurityLogMonitor$y5Q3dMmmJ8bk5nBh8WR2MUroKrI implements Comparator {
    public static final /* synthetic */ $$Lambda$SecurityLogMonitor$y5Q3dMmmJ8bk5nBh8WR2MUroKrI INSTANCE = new $$Lambda$SecurityLogMonitor$y5Q3dMmmJ8bk5nBh8WR2MUroKrI();

    private /* synthetic */ $$Lambda$SecurityLogMonitor$y5Q3dMmmJ8bk5nBh8WR2MUroKrI() {
    }

    @Override // java.util.Comparator
    public final int compare(Object obj, Object obj2) {
        int signum;
        signum = Long.signum(((SecurityLog.SecurityEvent) obj).getTimeNanos() - ((SecurityLog.SecurityEvent) obj2).getTimeNanos());
        return signum;
    }
}