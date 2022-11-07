package com.android.server.policy.keyguard;

import android.view.KeyEvent;
import com.android.internal.policy.IKeyguardService;

/* loaded from: classes2.dex */
public interface IVivoKeyguardServiceDelegate {
    void bindFaceKeyguardService();

    void bindFingerprintKeyguardService();

    void onBootCompleted();

    void onBootMessageDialogShown(boolean z);

    void onFinishedGoingToSleep(int i, boolean z);

    void onFinishedWakingUp();

    void onKeyguardMsgChanged(String str, String str2, String str3);

    void onKeyguardOccluded(boolean z);

    void onKeyguardShown(boolean z);

    void onScreenTurnedOff();

    void onScreenTurnedOn();

    void onScreenTurningOff();

    void onScreenTurningOn();

    void onSoftKeyboardShown(boolean z);

    void onStartedGoingToSleep(int i);

    void onStartedWakingUp(int i);

    void onSystemKey(KeyEvent keyEvent, boolean z);

    void onSystemReady();

    void onTakeScreenshot();

    void onUnlockReason(int i);

    void setCurrentUser(int i);

    void setKeyguardService(IKeyguardService iKeyguardService);

    void wakeUpFromAiKey(long j, boolean z);

    void wakeUpFromPowerKey(long j, boolean z);
}