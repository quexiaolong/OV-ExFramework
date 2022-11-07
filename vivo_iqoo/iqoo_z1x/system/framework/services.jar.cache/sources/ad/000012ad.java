package com.android.server.net;

import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.VivoNetworkStats;
import android.telephony.SubscriptionPlan;
import android.util.ArrayMap;

/* loaded from: classes.dex */
public interface IVivoNetworkStatsService {
    NetworkStats changeToMergedStats(String str, int i, NetworkStats networkStats);

    void dummy();

    NetworkStatsHistory getHistoryForPack(String str, int i, NetworkTemplate networkTemplate, String str2, int i2, int i3, int i4, int i5);

    NetworkStatsHistory getMergedHistory(String str, int i, NetworkStatsCollection networkStatsCollection, NetworkTemplate networkTemplate, SubscriptionPlan subscriptionPlan, int i2, int i3, int i4, int i5, long j, long j2, int i6);

    int[] getSeparateUids();

    VivoNetworkStats getSummaryForAllPack(String str, int i, NetworkTemplate networkTemplate, long j, long j2, boolean z);

    VivoNetworkStats getSummaryForAllRat(String str, int i, NetworkTemplate networkTemplate, long j, long j2, boolean z);

    NetworkStats getSummaryForMergedUid(String str, int i, NetworkTemplate networkTemplate, long j, long j2, boolean z, boolean z2);

    long getUidStats(int i, int i2);

    void initVivoRecorder();

    void noteStackedIface(String str, String str2);

    void performPollVivo(int i);

    void recordVivoSnapshot(ArrayMap<String, NetworkIdentitySet> arrayMap, long j);

    void shutdownVivoRecorder();

    void updateIfaceRatinfo(String str, int i);

    void updateMergedDisplayUids();

    void updateVivoPersistThresholds();

    /* loaded from: classes.dex */
    public interface IVivoNetworkStatsServiceExport {
        IVivoNetworkStatsService getVivoInjectInstance();

        default void dummyExport() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }
    }
}