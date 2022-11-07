package com.android.server.policy.keyguard;

import android.content.Context;
import android.hardware.biometrics.fingerprint.FingerprintKeyguardInternal;
import android.view.KeyEvent;
import com.android.internal.policy.IKeyguardService;
import com.android.server.LocalServices;
import com.android.server.biometrics.keyguard.FaceUnlockController;
import com.android.server.biometrics.keyguard.FingerprintUnlockController;
import com.android.server.display.color.VivoLightColorMatrixControl;
import com.android.server.wm.SnapshotWindow;

/* loaded from: classes.dex */
public final class VivoKeyguardServiceDelegateImpl implements IVivoKeyguardServiceDelegate {
    private static final String TAG = "VivoKeyguardServiceDelegateImpl";
    private Context mContext;
    private FaceUnlockController mFaceUnlockController;
    private FingerprintKeyguardInternal mFingerprintUnlockController;
    private KeyguardServiceDelegate mKeyguardServiceDelegate;
    private SnapshotWindow mSnapshotWin;

    public VivoKeyguardServiceDelegateImpl(Context context, KeyguardServiceDelegate keyguardServiceDelegate) {
        this.mContext = context;
        this.mKeyguardServiceDelegate = keyguardServiceDelegate;
        this.mFaceUnlockController = new FaceUnlockController(context);
        FingerprintUnlockController fingerprintUnlockController = new FingerprintUnlockController(context);
        this.mFingerprintUnlockController = fingerprintUnlockController;
        LocalServices.addService(FingerprintKeyguardInternal.class, fingerprintUnlockController);
    }

    public void setKeyguardService(IKeyguardService keyguardService) {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.setKeyguardService(keyguardService);
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.setKeyguardService(keyguardService);
        }
    }

    public void onBootCompleted() {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onBootCompleted();
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onBootCompleted();
        }
    }

    public void onSystemReady() {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onSystemReady();
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onSystemReady();
        }
    }

    public void setCurrentUser(int newUserId) {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.setCurrentUser(newUserId);
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.setCurrentUser(newUserId);
        }
    }

    public void onStartedWakingUp(int why) {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onStartedWakingUp();
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onStartedWakingUp(why);
        }
        VivoLightColorMatrixControl.onStartedWakingUp(why);
    }

    public void onFinishedWakingUp() {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onFinishedWakingUp();
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onFinishedWakingUp();
        }
        if (this.mSnapshotWin == null) {
            this.mSnapshotWin = SnapshotWindow.getInstance(this.mContext);
        }
        SnapshotWindow snapshotWindow = this.mSnapshotWin;
        if (snapshotWindow != null) {
            snapshotWindow.onFinishedWakingUp();
        }
        VivoLightColorMatrixControl.onFinishedWakingUp();
    }

    public void onScreenTurningOff() {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onScreenTurningOff();
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onScreenTurningOff();
        }
    }

    public void onScreenTurnedOff() {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onScreenTurnedOff();
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onScreenTurnedOff();
        }
    }

    public void onScreenTurningOn() {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onScreenTurningOn();
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onScreenTurningOn();
        }
    }

    public void onScreenTurnedOn() {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onScreenTurnedOn();
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onScreenTurnedOn();
        }
    }

    public void onStartedGoingToSleep(int why) {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onStartedGoingToSleep(why);
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onStartedGoingToSleep(why);
        }
        if (this.mSnapshotWin == null) {
            this.mSnapshotWin = SnapshotWindow.getInstance(this.mContext);
        }
        SnapshotWindow snapshotWindow = this.mSnapshotWin;
        if (snapshotWindow != null) {
            snapshotWindow.onStartedGoingToSleep();
        }
        VivoLightColorMatrixControl.onStartedGoingToSleep(why);
    }

    public void onFinishedGoingToSleep(int why, boolean cameraGestureTriggered) {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onFinishedGoingToSleep(why, cameraGestureTriggered);
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onFinishedGoingToSleep(why, cameraGestureTriggered);
        }
        VivoLightColorMatrixControl.onFinishedGoingToSleep(why);
    }

    public void onKeyguardShown(boolean showing) {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onKeyguardShown(showing);
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onKeyguardShown(showing);
        }
        if (this.mSnapshotWin == null) {
            this.mSnapshotWin = SnapshotWindow.getInstance(this.mContext);
        }
        SnapshotWindow snapshotWindow = this.mSnapshotWin;
        if (snapshotWindow != null) {
            snapshotWindow.onKeyguardShown(showing);
        }
    }

    public void onKeyguardOccluded(boolean occluded) {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onKeyguardOccluded(occluded);
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onKeyguardOccluded(occluded);
        }
    }

    public void onTakeScreenshot() {
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onTakeScreenshot();
        }
    }

    public void onSystemKey(KeyEvent event, boolean showing) {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onKeyEvent(event);
        }
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onSystemKey(event, showing);
        }
    }

    public void onKeyguardMsgChanged(String msgType, String msg, String extra) {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.onKeyguardMsgChanged(msgType, msg, extra);
        }
    }

    public void onSoftKeyboardShown(boolean shown) {
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onSoftKeyboardShown(shown);
        }
    }

    public void onBootMessageDialogShown(boolean shown) {
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.onBootMessageDialogShown(shown);
        }
    }

    public void bindFingerprintKeyguardService() {
        FingerprintKeyguardInternal fingerprintKeyguardInternal = this.mFingerprintUnlockController;
        if (fingerprintKeyguardInternal != null) {
            fingerprintKeyguardInternal.bindFingerprintKeyguardService();
        }
    }

    public void bindFaceKeyguardService() {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.bindFaceKeyguardService();
        }
    }

    public void wakeUpFromPowerKey(long eventTime, boolean wakeInTheaterMode) {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.wakeUpFromPowerKey(eventTime);
        }
    }

    public void wakeUpFromAiKey(long eventTime, boolean wakeInTheaterMode) {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.wakeUpFromAiKey(eventTime);
        }
    }

    public void onUnlockReason(int reason) {
        FaceUnlockController faceUnlockController = this.mFaceUnlockController;
        if (faceUnlockController != null) {
            faceUnlockController.unlockReason(reason);
        }
    }
}