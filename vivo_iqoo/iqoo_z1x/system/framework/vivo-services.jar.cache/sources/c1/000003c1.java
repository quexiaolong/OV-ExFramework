package com.android.server.policy;

import android.view.KeyEvent;
import com.android.internal.policy.KeyInterceptionInfo;

/* loaded from: classes.dex */
public abstract class AVivoInterceptKeyCallback implements IVivoKeyCallback {
    public boolean mIsKeyguardActive;
    public boolean mIsScreenOn;
    public KeyEvent mKeyEvent;
    public KeyInterceptionInfo mKeyInterceptionInfo;
    public int mPolicyFlags;
    public int mState = -1;

    public boolean onCheckNeedWakeLockWhenScreenOff(int keyCode, KeyEvent event) {
        return false;
    }

    @Override // com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        return false;
    }

    @Override // com.android.server.policy.IVivoKeyCallback
    public boolean onCheckDoubleClickEnabled(int keyCode, KeyEvent event) {
        return false;
    }

    @Override // com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        return -100;
    }

    @Override // com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        return -100;
    }

    @Override // com.android.server.policy.IVivoKeyCallback
    public void onKeyLongPress(int keyCode, KeyEvent event) {
    }

    @Override // com.android.server.policy.IVivoKeyCallback
    public void onKeyDoubleClick(int keyCode, KeyEvent event) {
    }

    @Override // com.android.server.policy.IVivoKeyCallback
    public void cancelPendingKeyAction() {
    }
}