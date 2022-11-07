package com.android.server.networktime;

import android.net.Network;

/* loaded from: classes.dex */
public interface IVivoNetworkTimeUpdateService {
    void enforceForceRefresh(int i, int i2);

    void initDefaultTime();

    void setCurrentTime(long j);

    boolean shouldIgnoreNitzTime();

    void showOnPollNetworkTimeEvent(int i, Network network);
}