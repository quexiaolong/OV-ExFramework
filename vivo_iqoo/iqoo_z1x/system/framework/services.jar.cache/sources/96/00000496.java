package com.android.server;

import android.content.Context;
import android.content.res.Configuration;

/* loaded from: classes.dex */
public interface IVivoUiModeMgrService {
    void registerProcessObserver();

    void sendConfigurationLockedAfter(Context context);

    void sendConfigurationLockedBefore(Context context, Configuration configuration);

    void setNightMode(int i);

    int updateNightModeFromSettings(Context context, int i);
}