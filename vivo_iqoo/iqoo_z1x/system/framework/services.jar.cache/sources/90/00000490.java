package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

/* loaded from: classes.dex */
public interface IVivoServiceWatcher {

    /* loaded from: classes.dex */
    public interface Callback {
        void onMccChanged(boolean z);
    }

    Handler getVivoHandler();

    void registerMccChangedListener(Callback callback, Context context);

    int updateCurrentUserIdForFixMode(int i, int i2);

    void updateNlpIntent(Intent intent);
}