package com.android.server;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public interface INativeDaemonConnectorCallbacks {
    boolean onCheckHoldWakeLock(int i);

    void onDaemonConnected();

    boolean onEvent(int i, String str, String[] strArr);
}