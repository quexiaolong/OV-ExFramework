package com.android.server.content;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import com.android.server.am.firewall.VivoFirewall;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoSyncManagerImpl implements IVivoSyncManager {
    static final String TAG = "VivoSyncManagerImpl";
    private SyncManager mSyncManager;
    private VivoFirewall mVivoFirewall;

    public VivoSyncManagerImpl(SyncManager syncManager, Context context) {
        if (syncManager == null) {
            VSlog.i(TAG, "container is " + syncManager);
        }
        this.mSyncManager = syncManager;
        this.mVivoFirewall = VivoFirewall.getInstance(context);
    }

    public Intent handleBindIntent(Intent intent) {
        intent.putExtra("reason", SyncOperation.reasonToString((PackageManager) null, getFirewallInteger().intValue()));
        return intent;
    }

    public void setFirewallInteger(Integer integer) {
        this.mVivoFirewall.setThreadLocalInteger(integer);
    }

    private Integer getFirewallInteger() {
        return this.mVivoFirewall.getThreadLocalInteger();
    }

    public void removeFirewallInteger() {
        this.mVivoFirewall.removeThreadLocalInteger();
    }
}