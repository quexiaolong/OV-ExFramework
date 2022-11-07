package com.android.server.pm.dex;

import com.vivo.statistics.sdk.GatherManager;

/* loaded from: classes.dex */
public class DexInterceptHelper {
    public static final int INTERCEPT_FAIL = 0;
    public static final int INTERCEPT_SUCCESS = 1;
    private static final String TAG = "dex_intercept";
    private int mDexInterceptSuccess;
    private String mPkgName;

    public static boolean isOpen() {
        return GatherManager.getInstance().getService() != null;
    }

    public void send(String pkgName, int dexInterceptSuccess) {
        this.mPkgName = pkgName;
        this.mDexInterceptSuccess = dexInterceptSuccess;
        GatherManager.getInstance().gather(TAG, new Object[]{this.mPkgName, Integer.valueOf(this.mDexInterceptSuccess)});
    }

    public String toString() {
        return this.mPkgName + " " + this.mDexInterceptSuccess;
    }
}