package com.android.server.notification;

/* loaded from: classes.dex */
public interface RankingHandler {
    void requestReconsideration(RankingReconsideration rankingReconsideration);

    void requestSort();
}