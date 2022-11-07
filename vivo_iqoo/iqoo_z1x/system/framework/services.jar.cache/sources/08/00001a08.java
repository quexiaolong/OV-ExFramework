package com.android.server.trust;

import android.content.ComponentName;
import android.content.pm.ResolveInfo;
import java.util.List;

/* loaded from: classes2.dex */
public interface IVivoTrustManagerService {
    boolean debugStatus();

    ComponentName removeDefaultIfNotExist(ComponentName componentName, List<ResolveInfo> list);

    void sendBroadcastToSmartunlockApp();
}