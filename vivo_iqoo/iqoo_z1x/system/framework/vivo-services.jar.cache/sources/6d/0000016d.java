package com.android.server.biometrics.keyguard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.text.TextUtils;
import android.view.KeyEvent;
import com.android.internal.policy.IKeyguardService;
import com.android.server.UiThread;
import com.vivo.face.common.keyguard.KeyguardCallback;
import com.vivo.face.common.notification.FaceSystemNotify;
import com.vivo.face.common.state.KeyguardState;
import com.vivo.face.common.wake.FaceWakeController;
import com.vivo.face.internal.keyguard.IFaceKeyguardService;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class FaceUnlockController implements FaceSystemNotify.SystemCallback {
    private static final boolean DEBUG = false;
    private static final String TAG = "FaceUnlockController";
    private Context mContext;
    private IFaceKeyguardService mFaceKeyguardService;
    private FaceSystemNotify mSystemNotify;
    private boolean mBootFirstBind = true;
    private final ServiceConnection mFaceKeyguardConnection = new ServiceConnection() { // from class: com.android.server.biometrics.keyguard.FaceUnlockController.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            VSlog.i(FaceUnlockController.TAG, "FaceKeyguardService connected");
            FaceUnlockController.this.mFaceKeyguardService = IFaceKeyguardService.Stub.asInterface(service);
            try {
                if (FaceUnlockController.this.mBootFirstBind) {
                    FaceUnlockController.this.mBootFirstBind = false;
                    FaceUnlockController.this.mFaceKeyguardService.onAppStarted("boot-bind", FaceUnlockController.this.mKeyguardState.mCrashNum);
                } else {
                    FaceUnlockController.this.mFaceKeyguardService.onAppStarted("app-crash-rebind", FaceUnlockController.this.mKeyguardState.mCrashNum);
                }
                if (FaceUnlockController.this.mKeyguardCallback.isSystemKeyguardServiceValid()) {
                    FaceUnlockController.this.mFaceKeyguardService.setKeyguardCallback(FaceUnlockController.this.mKeyguardCallback);
                }
                if (FaceUnlockController.this.mKeyguardState.mSystemReady) {
                    FaceUnlockController.this.mFaceKeyguardService.onSystemReady();
                    if (FaceUnlockController.this.mKeyguardState.mCurrentUser != -10000) {
                        FaceUnlockController.this.mFaceKeyguardService.setCurrentUser(FaceUnlockController.this.mKeyguardState.mCurrentUser);
                    }
                    if (FaceUnlockController.this.mKeyguardState.mInteractiveState == 1) {
                        FaceUnlockController.this.mFaceKeyguardService.onStartedWakingUp();
                    }
                    if (FaceUnlockController.this.mKeyguardState.mInteractiveState == 2) {
                        FaceUnlockController.this.mFaceKeyguardService.onFinishedWakingUp();
                    }
                    if (FaceUnlockController.this.mKeyguardState.mScreenState == 3) {
                        FaceUnlockController.this.mFaceKeyguardService.onScreenTurningOn();
                    }
                    if (FaceUnlockController.this.mKeyguardState.mScreenState == 4) {
                        FaceUnlockController.this.mFaceKeyguardService.onScreenTurnedOn();
                    }
                    if (FaceUnlockController.this.mKeyguardState.mScreenState == 1) {
                        FaceUnlockController.this.mFaceKeyguardService.onScreenTurningOff();
                    }
                    if (FaceUnlockController.this.mKeyguardState.mScreenState == 2) {
                        FaceUnlockController.this.mFaceKeyguardService.onScreenTurnedOff();
                    }
                    if (!TextUtils.isEmpty(FaceUnlockController.this.mKeyguardState.mKeyguardMsgType)) {
                        FaceUnlockController.this.mFaceKeyguardService.onKeyguardMsgChanged(FaceUnlockController.this.mKeyguardState.mKeyguardMsgType, FaceUnlockController.this.mKeyguardState.mKeyguardMsg, FaceUnlockController.this.mKeyguardState.mKeyguardMsgExtra);
                    }
                }
                if (FaceUnlockController.this.mKeyguardState.mBootCompleted) {
                    FaceUnlockController.this.mFaceKeyguardService.onBootCompleted();
                }
                if (FaceUnlockController.this.mKeyguardState.mKeyguardShown) {
                    FaceUnlockController.this.mFaceKeyguardService.onKeyguardShown(FaceUnlockController.this.mKeyguardState.mKeyguardShown);
                }
                if (FaceUnlockController.this.mKeyguardState.mKeyguardOccluded) {
                    FaceUnlockController.this.mFaceKeyguardService.onKeyguardOccluded(FaceUnlockController.this.mKeyguardState.mKeyguardOccluded);
                }
                if (FaceUnlockController.this.mKeyguardState.mPrimaryDisplayState != 0) {
                    FaceUnlockController.this.mFaceKeyguardService.onDisplayStateChanged(0, FaceUnlockController.this.mKeyguardState.mPrimaryDisplayState, FaceUnlockController.this.mKeyguardState.mPrimaryDisplayBacklight);
                }
                if (FaceUnlockController.this.mKeyguardState.mSecondaryDisplayState != 0) {
                    FaceUnlockController.this.mFaceKeyguardService.onDisplayStateChanged(4096, FaceUnlockController.this.mKeyguardState.mSecondaryDisplayState, FaceUnlockController.this.mKeyguardState.mSecondaryDisplayBacklight);
                }
            } catch (Exception e) {
                VSlog.e(FaceUnlockController.TAG, "Remote exception while connecting face kayguard service");
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            VSlog.e(FaceUnlockController.TAG, "FaceKeyguardService disconnected");
            FaceUnlockController.this.mFaceKeyguardService = null;
            FaceUnlockController.this.mKeyguardState.mCrashNum++;
        }
    };
    private final Handler mHandler = UiThread.getHandler();
    private KeyguardState mKeyguardState = new KeyguardState();
    private KeyguardCallback mKeyguardCallback = new KeyguardCallback();

    public FaceUnlockController(Context context) {
        this.mContext = context;
        FaceSystemNotify faceSystemNotify = FaceSystemNotify.getInstance();
        this.mSystemNotify = faceSystemNotify;
        faceSystemNotify.registerCallback(this);
    }

    public void onBootCompleted() {
        this.mKeyguardState.mBootCompleted = true;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onBootCompleted();
            }
        } catch (Exception ex) {
            VSlog.e(TAG, "Remote exception while sending boot completed message", ex);
        }
    }

    public void onSystemReady() {
        this.mKeyguardState.mSystemReady = true;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onSystemReady();
            }
        } catch (Exception ex) {
            VSlog.e(TAG, "Remote exception while sending system ready message", ex);
        }
    }

    public void setKeyguardService(IKeyguardService keyguardService) {
        this.mKeyguardCallback.setSystemKeyguardService(keyguardService);
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.setKeyguardCallback(keyguardService != null ? this.mKeyguardCallback : null);
            }
        } catch (Exception ex) {
            VSlog.e(TAG, "Remote exception while setting keyguard callback", ex);
        }
    }

    public void setCurrentUser(int newUserId) {
        this.mKeyguardState.mCurrentUser = newUserId;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.setCurrentUser(newUserId);
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while setting current user", ex);
        }
    }

    public void onKeyguardShown(boolean shown) {
        this.mKeyguardState.mKeyguardShown = shown;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onKeyguardShown(shown);
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending keyguard shown message", ex);
        }
    }

    public void onKeyguardOccluded(boolean occluded) {
        this.mKeyguardState.mKeyguardOccluded = occluded;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onKeyguardOccluded(occluded);
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending keyguard occluded message", ex);
        }
    }

    public void onKeyguardMsgChanged(String msgType, String msg, String extra) {
        this.mKeyguardState.mKeyguardMsgType = msgType;
        this.mKeyguardState.mKeyguardMsg = msg;
        this.mKeyguardState.mKeyguardMsgExtra = extra;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onKeyguardMsgChanged(msgType, msg, extra);
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending keyguard changed message", ex);
        }
    }

    public void onScreenTurningOff() {
        this.mKeyguardState.mScreenState = 1;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onScreenTurningOff();
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending screen turning off message", ex);
        }
    }

    public void onScreenTurnedOff() {
        this.mKeyguardState.mScreenState = 2;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onScreenTurnedOff();
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending screen turned off message", ex);
        }
    }

    public void onScreenTurningOn() {
        this.mKeyguardState.mScreenState = 3;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onScreenTurningOn();
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending screen turning on message", ex);
        }
    }

    public void onScreenTurnedOn() {
        this.mKeyguardState.mScreenState = 4;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onScreenTurnedOn();
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending screen turned on message", ex);
        }
    }

    public void onKeyEvent(KeyEvent event) {
        event.getKeyCode();
        event.getAction();
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onKeyEvent(event);
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending key event message", ex);
        }
    }

    public void wakeUpFromPowerKey(long eventTime) {
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.wakeUpFromPowerKey(eventTime);
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending wake up from powker message", ex);
        }
    }

    public void wakeUpFromAiKey(long eventTime) {
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.wakeUpFromAiKey(eventTime);
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Exception while sending wake up from ai message", ex);
        }
    }

    public void onStartedWakingUp() {
        this.mKeyguardState.mInteractiveState = 1;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onStartedWakingUp();
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending started wakeing up message", ex);
        }
    }

    public void onFinishedWakingUp() {
        this.mKeyguardState.mInteractiveState = 2;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onFinishedWakingUp();
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending finished wakeing up message", ex);
        }
    }

    public void onStartedGoingToSleep(int why) {
        this.mKeyguardState.mInteractiveState = 3;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onStartedGoingToSleep(why);
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending started going to sleep message", ex);
        }
    }

    public void onFinishedGoingToSleep(int why, boolean cameraGestureTriggered) {
        this.mKeyguardState.mInteractiveState = 4;
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onFinishedGoingToSleep(why, cameraGestureTriggered);
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending finished going to sleep message", ex);
        }
    }

    @Override // com.vivo.face.common.notification.FaceSystemNotify.SystemCallback
    public void onDisplayStateChanged(int displayId, int state, int backlight) {
        if (displayId == 0) {
            if (state != this.mKeyguardState.mPrimaryDisplayState || ((backlight <= 0 && this.mKeyguardState.mPrimaryDisplayBacklight > 0) || (backlight > 0 && this.mKeyguardState.mPrimaryDisplayBacklight <= 0))) {
                onDisplayStateChangedInternal(0, state, backlight);
            }
            this.mKeyguardState.mPrimaryDisplayState = state;
            this.mKeyguardState.mPrimaryDisplayBacklight = backlight;
        } else if (4096 == displayId) {
            notifyFaceWakeDisplayState(state, backlight);
            if (state != this.mKeyguardState.mSecondaryDisplayState || ((backlight <= 0 && this.mKeyguardState.mSecondaryDisplayBacklight > 0) || (backlight > 0 && this.mKeyguardState.mSecondaryDisplayBacklight <= 0))) {
                onDisplayStateChangedInternal(4096, state, backlight);
            }
            this.mKeyguardState.mSecondaryDisplayState = state;
            this.mKeyguardState.mSecondaryDisplayBacklight = backlight;
        }
    }

    private void notifyFaceWakeDisplayState(int state, int backlight) {
        FaceWakeController wakeController = FaceWakeController.getInstance();
        if (this.mKeyguardState.mSecondaryDisplayState != state) {
            wakeController.setDisplayState(state);
        }
        if (this.mKeyguardState.mSecondaryDisplayBacklight != backlight) {
            wakeController.setDisplayBacklight(backlight);
        }
    }

    private void onDisplayStateChangedInternal(int displayId, int state, int backlight) {
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.onDisplayStateChanged(displayId, state, backlight);
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending display state changed message", ex);
        }
    }

    public void unlockReason(int reason) {
        try {
            if (this.mFaceKeyguardService != null) {
                this.mFaceKeyguardService.unlockReason(reason);
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "Remote Exception while sending unlock reason", ex);
        }
    }

    public void bindFaceKeyguardService() {
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.biometrics.face")) {
            VSlog.w(TAG, "face feature not supported");
            return;
        }
        ComponentName keyguardComponent = ComponentName.unflattenFromString("com.vivo.faceui/com.vivo.faceui.keyguard.KeyguardService");
        Intent intent = new Intent();
        intent.addFlags(256);
        intent.setComponent(keyguardComponent);
        if (!this.mContext.bindServiceAsUser(intent, this.mFaceKeyguardConnection, 1, this.mHandler, UserHandle.SYSTEM)) {
            VSlog.w(TAG, "Failed to bind face keyguard service");
        } else {
            VSlog.i(TAG, "Face keyguard service binded");
        }
    }
}