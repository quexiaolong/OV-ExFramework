package com.android.server.policy;

import android.view.KeyEvent;
import android.view.View;

/* loaded from: classes.dex */
public interface IVivoKeyFallbackListener {
    int onInterceptKeyFallback(KeyEvent keyEvent, View view);
}