package com.android.server.wm;

import android.app.ActivityManager;
import android.content.Context;
import com.android.server.wm.ActivityMetricsLogger;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;

/* loaded from: classes.dex */
public class VivoActivityMetricsLoggerImpl implements IVivoActivityMetricsLogger {
    private static final String TAG = "VivoActivityMetricsLoggerImpl";
    public static AbsVivoPerfManager mUxPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);

    public void logAppDisplayedBoost(ActivityMetricsLogger.TransitionInfoSnapshot info, ActivityRecord mLaunchedActivity) {
        int isGame;
        AbsVivoPerfManager absVivoPerfManager = mUxPerf;
        if (absVivoPerfManager != null) {
            absVivoPerfManager.perfUXEngine_events(3, 0, info.packageName, info.windowsDrawnDelayMs);
        }
        if (mUxPerf != null) {
            if (ActivityManager.isLowRamDeviceStatic()) {
                isGame = mLaunchedActivity.isAppInfoGame();
            } else {
                isGame = mUxPerf.perfGetFeedback(5633, mLaunchedActivity.packageName) == 2 ? 1 : 0;
            }
            mUxPerf.perfUXEngine_events(5, 0, info.packageName, isGame);
        }
        if (mLaunchedActivity.mPerf != null && mLaunchedActivity.perfActivityBoostHandler > 0) {
            mLaunchedActivity.mPerf.perfLockReleaseHandler(mLaunchedActivity.perfActivityBoostHandler);
            mLaunchedActivity.perfActivityBoostHandler = -1;
        }
    }
}