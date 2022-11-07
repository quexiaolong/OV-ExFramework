package com.android.server;

import com.android.server.Watchdog;
import java.util.ArrayList;

/* loaded from: classes.dex */
public interface IVivoWatchdog {
    void checkBlockedAndWarningLocked(ArrayList<Watchdog.HandlerChecker> arrayList);

    void completionLocked();
}