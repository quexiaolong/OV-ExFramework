package com.vivo.services.rms.cgrp;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.appmng.AppManager;

/* loaded from: classes.dex */
public class AnimationManager {
    private static final int FIRST_DRAWING_FINISH = 3;
    private static final int HOT_START_MAX_DELAYED_MILLIS = 800;
    private static final int RECENT_ANIMATION_NONE = 0;
    private static final int RECENT_ANIMATION_READY = 1;
    private static final int RECENT_ANIMATION_START = 2;
    static final String TAG = "CgrpController";
    private volatile int mAnimationOptPreviousState;
    private volatile ProcessInfo mAnimationOptProcess;
    private volatile int mAnimationOptState;
    private SetDelayMessageHandler mSetDelayMessageHandler;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final AnimationManager INSTANCE = new AnimationManager();

        private Instance() {
        }
    }

    public static AnimationManager getInstance() {
        return Instance.INSTANCE;
    }

    private AnimationManager() {
        this.mAnimationOptProcess = null;
        this.mAnimationOptState = 0;
        this.mAnimationOptPreviousState = 0;
    }

    public void initialize(Looper looper) {
        this.mSetDelayMessageHandler = new SetDelayMessageHandler(looper);
    }

    public boolean isHotStartDelayed(ProcessInfo pi) {
        ProcessInfo optProcess = this.mAnimationOptProcess;
        return CgrpUtils.isHotStartDelayedEnable() && this.mAnimationOptState >= 2 && this.mAnimationOptPreviousState >= 2 && this.mAnimationOptPreviousState != this.mAnimationOptState && optProcess != null && pi == optProcess;
    }

    public boolean isAnimationTimeout(ProcessInfo pi) {
        return SystemClock.uptimeMillis() - pi.mLastAnimationTime > 800;
    }

    public void removeHotStartDelayed(ProcessInfo pi) {
        if (this.mAnimationOptState > 0 && pi == this.mAnimationOptProcess) {
            this.mAnimationOptState = 0;
            this.mAnimationOptPreviousState = 0;
            this.mAnimationOptProcess = null;
            SetDelayMessageHandler setDelayMessageHandler = this.mSetDelayMessageHandler;
            if (setDelayMessageHandler != null && setDelayMessageHandler.hasMessages(pi.mPid)) {
                this.mSetDelayMessageHandler.removeMessages(pi.mPid);
            }
            VLog.d(TAG, "removeHotStartDelayed is " + pi.mProcName);
        }
    }

    public void onActivityStart(ProcessInfo pi, int caller) {
        if (!CgrpUtils.isFeatureEnable()) {
            return;
        }
        if (AppManager.getInstance().getHomeProcessPid() == caller) {
            if (CgrpUtils.isRealtimeBlurEnable() && !CgrpUtils.isHotStartForbidDelayed(pi.mProcName) && this.mAnimationOptState == 0 && pi != null && pi.mPid != caller && !pi.hasFocus() && pi.hasShownUi() && this.mAnimationOptProcess == null && pi.mCgrpGroup < 6) {
                this.mAnimationOptProcess = pi;
                this.mAnimationOptState = 1;
                pi.mLastAnimationTime = SystemClock.uptimeMillis();
                VLog.d(TAG, "onActivityStart " + pi.mProcName);
            }
        } else if (pi != null && this.mAnimationOptState >= 2 && pi.mPid == caller) {
            onAnimationEnd();
        }
    }

    public void onAnimationStart() {
        SetDelayMessageHandler setDelayMessageHandler;
        ProcessInfo pi = this.mAnimationOptProcess;
        if (((this.mAnimationOptState == 1 && this.mAnimationOptPreviousState == 0) || (this.mAnimationOptState == 3 && this.mAnimationOptPreviousState == 1)) && pi != null) {
            this.mAnimationOptPreviousState = this.mAnimationOptState;
            this.mAnimationOptState = 2;
            VLog.d(TAG, "onAnimationStart " + pi.mProcName + " AnimationOptState " + this.mAnimationOptState + " mAnimationOptPreviousState " + this.mAnimationOptPreviousState);
            if (this.mAnimationOptPreviousState == 3 && pi.isAlive() && (setDelayMessageHandler = this.mSetDelayMessageHandler) != null) {
                setDelayMessageHandler.removeMessages(pi.mPid);
                SetDelayMessageHandler setDelayMessageHandler2 = this.mSetDelayMessageHandler;
                setDelayMessageHandler2.sendMessageDelayed(setDelayMessageHandler2.obtainMessage(pi.mPid, pi), 800L);
            }
        }
    }

    public void onFirstDrawingFinish(int pid, int state) {
        SetDelayMessageHandler setDelayMessageHandler;
        ProcessInfo pi = this.mAnimationOptProcess;
        if (((this.mAnimationOptState == 1 && this.mAnimationOptPreviousState == 0) || (this.mAnimationOptState == 2 && this.mAnimationOptPreviousState == 1)) && pi != null && state == 4 && pid == pi.mPid) {
            this.mAnimationOptPreviousState = this.mAnimationOptState;
            this.mAnimationOptState = 3;
            VLog.d(TAG, "onFirstDrawingFinish " + pi.mProcName + " AnimationOptState " + this.mAnimationOptState + " mAnimationOptPreviousState " + this.mAnimationOptPreviousState);
            if (this.mAnimationOptPreviousState == 2 && pi.isAlive() && (setDelayMessageHandler = this.mSetDelayMessageHandler) != null) {
                setDelayMessageHandler.removeMessages(pi.mPid);
                SetDelayMessageHandler setDelayMessageHandler2 = this.mSetDelayMessageHandler;
                setDelayMessageHandler2.sendMessageDelayed(setDelayMessageHandler2.obtainMessage(pi.mPid, pi), 800L);
            }
        }
    }

    public void onAnimationEnd() {
        if (this.mAnimationOptState > 0) {
            ProcessInfo pi = this.mAnimationOptProcess;
            this.mAnimationOptProcess = null;
            this.mAnimationOptPreviousState = 0;
            this.mAnimationOptState = 0;
            if (CgrpUtils.isFeatureEnable() && pi != null && pi.isAlive()) {
                CgrpController.getInstance().resetProcessGroupHandler(pi);
                SetDelayMessageHandler setDelayMessageHandler = this.mSetDelayMessageHandler;
                if (setDelayMessageHandler != null) {
                    setDelayMessageHandler.removeMessages(pi.mPid);
                }
                VLog.d(TAG, "onAnimationEnd " + pi.mProcName);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SetDelayMessageHandler extends Handler {
        private SetDelayMessageHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            ProcessInfo pi = (ProcessInfo) msg.obj;
            if (pi.isAlive()) {
                AnimationManager.this.onAnimationEnd();
            }
        }
    }
}