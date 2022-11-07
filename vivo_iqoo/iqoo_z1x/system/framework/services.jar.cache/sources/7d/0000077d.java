package com.android.server.am;

import android.os.WorkSource;
import com.android.server.IVivoFrozenInjector;

/* loaded from: classes.dex */
public interface IVivoBatteryStatsService {
    void noteGpsChanged(IVivoFrozenInjector iVivoFrozenInjector, WorkSource workSource, WorkSource workSource2);

    void noteStartSensor(int i, int i2);

    void removeDumpTimeoutMsg();

    void sendDumpTimeoutMsg();
}