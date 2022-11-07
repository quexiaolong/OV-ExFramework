package com.android.server.content;

import android.content.Intent;

/* loaded from: classes.dex */
public interface IVivoSyncManager {

    /* loaded from: classes.dex */
    public interface IVivoSyncManagerExport {
        IVivoSyncManager getVivoInjectInstance();
    }

    Intent handleBindIntent(Intent intent);

    void removeFirewallInteger();

    void setFirewallInteger(Integer num);
}