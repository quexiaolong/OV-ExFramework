package com.android.server.am;

import android.content.Context;
import android.multidisplay.MultiDisplayManager;
import android.util.Slog;
import com.android.server.VCarConfigManager;
import java.util.ArrayList;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;

/* loaded from: classes.dex */
public class VivoOomAdjusterImpl implements IVivoOomAdjuster {
    static final String TAG = "VivoOomAdjusterImpl";
    VCarConfigManager carConfigManager = VCarConfigManager.getInstance();
    boolean mEnableBgt;
    private static AbsVivoPerfManager mVPerf = null;
    public static AbsVivoPerfManager mVPerfBoost = null;
    public static int mPerfHandle = -1;
    public static int mCurRenderThreadTid = -1;
    public static boolean mIsTopAppRenderThreadBoostEnabled = false;

    public VivoOomAdjusterImpl() {
        this.mEnableBgt = false;
        if (mVPerf == null || mVPerfBoost == null) {
            mVPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
            mVPerfBoost = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        }
        AbsVivoPerfManager absVivoPerfManager = mVPerf;
        if (absVivoPerfManager != null) {
            mIsTopAppRenderThreadBoostEnabled = Boolean.parseBoolean(absVivoPerfManager.perfGetProp("vendor.perf.topAppRenderThreadBoost.enable", "false"));
            this.mEnableBgt = Boolean.parseBoolean(mVPerf.perfGetProp("vendor.perf.bgt.enable", "false"));
            mIsTopAppRenderThreadBoostEnabled = false;
            this.mEnableBgt = false;
        }
    }

    public void computeOomAdjLockedTopAppRenderBoost(ProcessRecord app) {
        if (mIsTopAppRenderThreadBoostEnabled && mCurRenderThreadTid != app.renderThreadTid && app.renderThreadTid > 0) {
            mCurRenderThreadTid = app.renderThreadTid;
            if (mVPerfBoost != null) {
                Slog.d(TAG, "TOP-APP: pid:" + app.pid + ", processName: " + app.processName + ", renderThreadTid: " + app.renderThreadTid);
                if (mPerfHandle >= 0) {
                    mVPerfBoost.perfLockRelease();
                    mPerfHandle = -1;
                }
                mPerfHandle = mVPerfBoost.perfHint(4246, app.processName, app.renderThreadTid, 1);
                Slog.d(TAG, "VENDOR_HINT_BOOST_RENDERTHREAD perfHint was called. mPerfHandle: " + mPerfHandle);
            }
        }
    }

    public void applyOomAdjLockedBgtBoost(ProcessRecord app) {
        if (this.mEnableBgt) {
            if (app.setAdj >= 900 && app.setAdj <= 999 && app.curAdj == 0 && app.hasForegroundActivities()) {
                Slog.d(TAG, "App adj change from cached state to fg state : " + app.pid + " " + app.processName);
                if (mVPerf != null) {
                    int[] fgAppPerfLockArgs = {1115815936, app.pid};
                    mVPerf.perfLockAcquireAsync(10, fgAppPerfLockArgs);
                }
            }
            if (app.setAdj == 700 && app.curAdj >= 900 && app.curAdj <= 999 && app.hasActivities()) {
                Slog.d(TAG, "App adj change from previous state to cached state : " + app.pid + " " + app.processName);
                if (mVPerf != null) {
                    int[] bgAppPerfLockArgs = {1115832320, app.pid};
                    mVPerf.perfLockAcquireAsync(10, bgAppPerfLockArgs);
                }
            }
        }
    }

    public void promoteOomForCarNet(ProcessRecord app) {
        ArrayList<String> pList;
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && MultiDisplayManager.isVCarDisplayRunning() && (pList = this.carConfigManager.get("oom")) != null && pList.size() > 0 && app.info != null && pList.contains(app.info.packageName)) {
            app.curAdj = 0;
        }
    }
}