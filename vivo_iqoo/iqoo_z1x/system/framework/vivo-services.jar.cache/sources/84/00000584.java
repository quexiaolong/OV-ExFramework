package com.android.server.wm;

import android.content.Context;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;

/* loaded from: classes.dex */
public class VivoTaskTapPointerEventListenerImpl implements IVivoTaskTapPointerEventListener {
    public AbsVivoPerfManager mPerfObj;

    public VivoTaskTapPointerEventListenerImpl() {
        this.mPerfObj = null;
        if (0 == 0) {
            this.mPerfObj = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        }
    }

    public void onPointerEventBoost() {
        AbsVivoPerfManager absVivoPerfManager;
        AbsVivoPerfManager absVivoPerfManager2;
        if (VivoActivityStackSupervisorImpl.mIsPerfBoostAcquired && this.mPerfObj != null) {
            if (VivoActivityStackSupervisorImpl.mPerfHandle > 0) {
                this.mPerfObj.perfLockReleaseHandler(VivoActivityStackSupervisorImpl.mPerfHandle);
                VivoActivityStackSupervisorImpl.mPerfHandle = -1;
            }
            VivoActivityStackSupervisorImpl.mIsPerfBoostAcquired = false;
        }
        if (VivoActivityStackSupervisorImpl.mPerfSendTapHint && (absVivoPerfManager2 = this.mPerfObj) != null) {
            absVivoPerfManager2.perfHintAsync(4163, (String) null);
            VivoActivityStackSupervisorImpl.mPerfSendTapHint = false;
        }
        if (VivoTaskDisplayAreaImpl.mIsPerfBoostAcquired && this.mPerfObj != null) {
            if (VivoTaskDisplayAreaImpl.mPerfHandle > 0) {
                this.mPerfObj.perfLockReleaseHandler(VivoTaskDisplayAreaImpl.mPerfHandle);
                VivoTaskDisplayAreaImpl.mPerfHandle = -1;
            }
            VivoTaskDisplayAreaImpl.mIsPerfBoostAcquired = false;
        }
        if (VivoTaskDisplayAreaImpl.mPerfSendTapHint && (absVivoPerfManager = this.mPerfObj) != null) {
            absVivoPerfManager.perfHintAsync(4163, (String) null);
            VivoTaskDisplayAreaImpl.mPerfSendTapHint = false;
        }
    }
}