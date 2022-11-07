package com.android.server.policy;

import android.view.KeyEvent;

/* loaded from: classes.dex */
public interface IVivoKeyBeforeQueueingListener {
    void cancelPendingKeyAction(int i);

    int onInterceptKeyBeforeQueueing(KeyEvent keyEvent, int i, boolean z, boolean z2);
}