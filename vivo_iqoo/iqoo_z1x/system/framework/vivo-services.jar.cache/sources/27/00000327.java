package com.android.server.notification;

import java.util.Comparator;

/* compiled from: lambda */
/* renamed from: com.android.server.notification.-$$Lambda$VivoRankingHelperImpl$E_efZTk0fXJNSUzlbCh_vtANumo  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$VivoRankingHelperImpl$E_efZTk0fXJNSUzlbCh_vtANumo implements Comparator {
    public static final /* synthetic */ $$Lambda$VivoRankingHelperImpl$E_efZTk0fXJNSUzlbCh_vtANumo INSTANCE = new $$Lambda$VivoRankingHelperImpl$E_efZTk0fXJNSUzlbCh_vtANumo();

    private /* synthetic */ $$Lambda$VivoRankingHelperImpl$E_efZTk0fXJNSUzlbCh_vtANumo() {
    }

    @Override // java.util.Comparator
    public final int compare(Object obj, Object obj2) {
        int compare;
        compare = Long.compare(((NotificationRecord) obj2).getRankingTimeMs(), ((NotificationRecord) obj).getRankingTimeMs());
        return compare;
    }
}