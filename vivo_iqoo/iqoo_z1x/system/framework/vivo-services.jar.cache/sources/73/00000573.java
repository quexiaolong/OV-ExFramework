package com.android.server.wm;

import android.content.Context;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.display.SceneManager;
import com.vivo.services.rms.display.scene.AnimationScene;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;

/* loaded from: classes.dex */
public class VivoScreenRotationAnimationImpl implements IVivoScreenRotationAnimation {
    private static final String TAG = "VivoActivityMetricsLoggerImpl";
    private AbsVivoPerfManager mPerf;
    private boolean mIsPerfLockAcquired = false;
    private long mRefreshRateHandle = -1;
    private boolean mIsCalculator = false;

    public VivoScreenRotationAnimationImpl() {
        this.mPerf = null;
        this.mPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
    }

    public void startAnimationBoost() {
        AbsVivoPerfManager absVivoPerfManager = this.mPerf;
        if (absVivoPerfManager != null && !this.mIsPerfLockAcquired) {
            absVivoPerfManager.perfHint(4240, (String) null);
            this.mIsPerfLockAcquired = true;
        }
        this.mRefreshRateHandle = RmsInjectorImpl.getInstance().acquireRefreshRate(SceneManager.ANIMATION_SCENE, AnimationScene.SCREEN_ROTATION_ANIMATION_NAME, 0, 0, 0, 0);
    }

    public void startAnimationRelease() {
        AbsVivoPerfManager absVivoPerfManager = this.mPerf;
        if (absVivoPerfManager != null && this.mIsPerfLockAcquired) {
            absVivoPerfManager.perfLockRelease();
            this.mIsPerfLockAcquired = false;
        }
        if (this.mRefreshRateHandle > 0) {
            RmsInjectorImpl.getInstance().releaseRefreshRate(this.mRefreshRateHandle);
            this.mRefreshRateHandle = 0L;
        }
    }

    public void handleBBKCalculatorCase(DisplayContent dc) {
        ActivityStack stack;
        WindowState ws;
        CharSequence title;
        this.mIsCalculator = false;
        if (dc != null && (stack = dc.getTopStack()) != null && (ws = stack.getTopVisibleAppMainWindow()) != null && (title = ws.getWindowTag()) != null && "com.android.bbkcalculator/com.android.bbkcalculator.Calculator".equals(title.toString())) {
            this.mIsCalculator = true;
        }
    }

    public boolean isBBKCalculatorRotating() {
        return this.mIsCalculator;
    }
}