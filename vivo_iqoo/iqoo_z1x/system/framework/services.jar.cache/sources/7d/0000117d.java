package com.android.server.lockmonitor;

import java.io.PrintWriter;

/* loaded from: classes.dex */
public interface IVivoFrameworkLockMonitor {
    void dumpFrameworkLockInformation(PrintWriter printWriter, String[] strArr, int i);

    void monitorLockBegin(int i);

    void monitorLockEnd(int i, String str, int i2);
}