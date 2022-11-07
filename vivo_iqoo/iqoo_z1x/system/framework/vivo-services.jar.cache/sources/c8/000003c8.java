package com.android.server.policy;

import android.view.KeyEvent;
import com.android.internal.policy.KeyInterceptionInfo;

/* loaded from: classes.dex */
public interface IVivoKeyBeforeDispatchingListener {
    int onInterceptKeyBeforeDispatching(KeyInterceptionInfo keyInterceptionInfo, KeyEvent keyEvent, int i, boolean z);
}