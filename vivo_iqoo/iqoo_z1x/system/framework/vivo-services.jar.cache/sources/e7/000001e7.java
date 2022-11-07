package com.android.server.display;

import android.content.Context;

/* loaded from: classes.dex */
public class VivoWifiDisplayAdapterImpl implements IVivoWifiDisplayAdapter {
    private WifiDisplayAdapter mWfdAdapter;

    public VivoWifiDisplayAdapterImpl(Context context, Object adapter) {
        this.mWfdAdapter = null;
        this.mWfdAdapter = (WifiDisplayAdapter) adapter;
    }
}