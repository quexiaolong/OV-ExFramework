package com.android.server;

import android.content.Context;

/* loaded from: classes.dex */
public class OplusDefaultSystemServerEx implements IOplusCommonSystemServerEx {
    protected final Context mSystemContext;
    protected final String TAG = getClass().getSimpleName();
    protected final SystemServiceManager mSystemServiceManager = (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);

    public OplusDefaultSystemServerEx(Context context) {
        this.mSystemContext = context;
    }

    @Override // com.android.server.IOplusCommonSystemServerEx
    public void startBootstrapServices() {
    }

    @Override // com.android.server.IOplusCommonSystemServerEx
    public void startCoreServices() {
    }

    @Override // com.android.server.IOplusCommonSystemServerEx
    public void startOtherServices() {
    }

    @Override // com.android.server.IOplusCommonSystemServerEx
    public void systemReady() {
    }

    @Override // com.android.server.IOplusCommonSystemServerEx
    public void systemRunning() {
    }
}