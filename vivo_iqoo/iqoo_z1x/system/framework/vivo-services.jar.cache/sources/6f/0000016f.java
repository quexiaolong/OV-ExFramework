package com.android.server.biometrics.keyguard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.biometrics.fingerprint.FingerprintKeyguardInternal;
import android.hardware.display.DisplayManagerInternal;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;
import com.android.internal.policy.IKeyguardService;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.biometrics.keyguard.FingerprintUnlockController;
import com.vivo.fingerprint.keyguard.IFingerprintKeyguardCallback;
import com.vivo.fingerprint.keyguard.IFingerprintKeyguardService;
import com.vivo.services.rms.ProcessList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class FingerprintUnlockController implements FingerprintKeyguardInternal {
    private static final boolean DEBUG = true;
    private static final int INTERACTIVE_STATE_AWAKE = 2;
    private static final int INTERACTIVE_STATE_GOING_TO_SLEEP = 3;
    private static final int INTERACTIVE_STATE_SLEEP = 0;
    private static final int INTERACTIVE_STATE_WAKING = 1;
    private static final int SCREEN_STATE_INIT = -1;
    private static final int SCREEN_STATE_OFF = 0;
    private static final int SCREEN_STATE_ON = 2;
    private static final int SCREEN_STATE_TURNING_OFF = 3;
    private static final int SCREEN_STATE_TURNING_ON = 1;
    private static final String TAG = "FingerprintUnlockController";
    private Context mContext;
    private DisplayManagerInternal mDisplayManagerService;
    private FingerprintKeyguardCallback mFingerprintKeyguardCallback;
    private IFingerprintKeyguardService mFingerprintKeyguardService;
    private PowerManager mPowerManager;
    private Handler mPowerManagerHandler;
    private FingerprintKeyguardInternal.WakeUpCallback mWakeUpCallback;
    private final ServiceConnection mFingerprintKeyguardConnection = new ServiceConnection() { // from class: com.android.server.biometrics.keyguard.FingerprintUnlockController.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            FingerprintUnlockController.info("*** FingerprintKeyguard connected (yay!)");
            FingerprintUnlockController.this.mFingerprintKeyguardService = IFingerprintKeyguardService.Stub.asInterface(service);
            try {
                Bundle extras = new Bundle();
                extras.putInt("crashCount", FingerprintUnlockController.this.mKeyguardState.crashNum);
                extras.putBoolean("bootMessageDialogShown", FingerprintUnlockController.this.mKeyguardState.bootMessageDialogShown);
                FingerprintUnlockController.this.mFingerprintKeyguardService.setKeyguardCallback(FingerprintUnlockController.this.mFingerprintKeyguardCallback, FingerprintUnlockController.this.mFingerprintKeyguardCallback.isSystemKeyguardAvalid());
                if (FingerprintUnlockController.this.mKeyguardState.systemIsReady) {
                    FingerprintUnlockController.this.mFingerprintKeyguardService.onSystemReady();
                    if (FingerprintUnlockController.this.mKeyguardState.currentUser != -10000) {
                        FingerprintUnlockController.this.mFingerprintKeyguardService.setCurrentUser(FingerprintUnlockController.this.mKeyguardState.currentUser);
                    }
                    if (FingerprintUnlockController.this.mKeyguardState.interactiveState == 2 || FingerprintUnlockController.this.mKeyguardState.interactiveState == 1) {
                        FingerprintUnlockController.this.mFingerprintKeyguardService.onStartedWakingUp(FingerprintUnlockController.this.mKeyguardState.wakeUpReason);
                    }
                    if (FingerprintUnlockController.this.mKeyguardState.interactiveState == 2) {
                        FingerprintUnlockController.this.mFingerprintKeyguardService.onFinishedWakingUp();
                    }
                    if (FingerprintUnlockController.this.mKeyguardState.screenState == 2 || FingerprintUnlockController.this.mKeyguardState.screenState == 1) {
                        FingerprintUnlockController.this.mFingerprintKeyguardService.onScreenTurningOn();
                    }
                    if (FingerprintUnlockController.this.mKeyguardState.screenState == 2) {
                        FingerprintUnlockController.this.mFingerprintKeyguardService.onScreenTurnedOn();
                    }
                }
                if (FingerprintUnlockController.this.mKeyguardState.bootCompleted) {
                    FingerprintUnlockController.this.mFingerprintKeyguardService.onBootCompleted(extras);
                }
                if (FingerprintUnlockController.this.mKeyguardState.bootMessageDialogShown) {
                    FingerprintUnlockController.this.mFingerprintKeyguardService.onBootMessageDialogShown(FingerprintUnlockController.this.mKeyguardState.bootMessageDialogShown);
                }
                if (FingerprintUnlockController.this.mKeyguardState.occluded) {
                    FingerprintUnlockController.this.mFingerprintKeyguardService.onKeyguardOccluded(FingerprintUnlockController.this.mKeyguardState.occluded);
                }
                FingerprintUnlockController.this.mFingerprintKeyguardService.onKeyguardShown(FingerprintUnlockController.this.mKeyguardState.showing);
                if (FingerprintUnlockController.this.mKeyguardState.softKeyboardShown) {
                    FingerprintUnlockController.this.mFingerprintKeyguardService.onSoftKeyboardShown(FingerprintUnlockController.this.mKeyguardState.softKeyboardShown);
                }
            } catch (RemoteException e) {
                VSlog.w(FingerprintUnlockController.TAG, "Remote Exception.", e);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            FingerprintUnlockController.info("*** FingerprintKeyguard disconnected (boo!)");
            FingerprintUnlockController.this.mFingerprintKeyguardService = null;
            FingerprintUnlockController.this.mKeyguardState.crashNum++;
            if (FingerprintUnlockController.this.mKeyguardState.forceDisplayState == 2 && FingerprintUnlockController.this.mKeyguardState.isNeedBlockBrightness) {
                FingerprintUnlockController.this.mFingerprintKeyguardCallback.setDisplayState(1);
            }
            FingerprintUnlockController.this.mKeyguardState.isNeedBlockBrightness = false;
            FingerprintUnlockController.this.mKeyguardState.isFingerprintWakingUp = false;
        }
    };
    private final Handler mHandler = UiThread.getHandler();
    private FingerprintKeyguardState mKeyguardState = new FingerprintKeyguardState();

    public FingerprintUnlockController(Context context) {
        this.mContext = context;
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mFingerprintKeyguardCallback = new FingerprintKeyguardCallback(powerManager);
        this.mDisplayManagerService = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
    }

    public void setKeyguardService(IKeyguardService keyguardService) {
        this.mFingerprintKeyguardCallback.setSystemKeyguardService(keyguardService);
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.setKeyguardCallback(this.mFingerprintKeyguardCallback, keyguardService != null);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
    }

    public void onBootCompleted() {
        info("onBootCompleted()");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onBootCompleted(new Bundle());
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.bootCompleted = true;
    }

    public void onSystemReady() {
        info("onSystemReady()");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onSystemReady();
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.systemIsReady = true;
    }

    public void setCurrentUser(int newUserId) {
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.setCurrentUser(newUserId);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.currentUser = newUserId;
    }

    public void onStartedWakingUp(int reason) {
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                debug("onStartedWakingUp()");
                fingerprintKeyguardService.onStartedWakingUp(reason);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.wakeUpReason = reason;
        this.mKeyguardState.interactiveState = 1;
    }

    public void onFinishedWakingUp() {
        debug("onFinishedWakingUp()");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onFinishedWakingUp();
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.interactiveState = 2;
    }

    public void onScreenTurningOff() {
        debug("onScreenTurningOff() " + screenStateToString(this.mKeyguardState.screenState));
        if (this.mKeyguardState.screenState == 3) {
            warning("repeating screen turning off");
            return;
        }
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onScreenTurningOff();
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.screenState = 3;
    }

    public void onScreenTurnedOff() {
        debug("onScreenTurnedOff() " + screenStateToString(this.mKeyguardState.screenState));
        if (this.mKeyguardState.screenState == 0) {
            warning("repeating screen turned off");
            return;
        }
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onScreenTurnedOff();
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.screenState = 0;
    }

    public void onScreenTurningOn() {
        debug("onScreenTurningOn() " + screenStateToString(this.mKeyguardState.screenState));
        if (this.mKeyguardState.screenState == 1) {
            warning("repeating screen turning on");
            return;
        }
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onScreenTurningOn();
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.screenState = 1;
    }

    public void onScreenTurnedOn() {
        debug("onScreenTurnedOn() " + screenStateToString(this.mKeyguardState.screenState));
        if (this.mKeyguardState.screenState == 2) {
            warning("repeating screen turned on");
            return;
        }
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onScreenTurnedOn();
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.screenState = 2;
    }

    public void onStartedGoingToSleep(int why) {
        debug("onStartedGoingToSleep(" + why + ")");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onStartedGoingToSleep(why);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.interactiveState = 3;
    }

    public void onFinishedGoingToSleep(int why, boolean cameraGestureTriggered) {
        debug("onFinishedGoingToSleep(" + why + ", " + cameraGestureTriggered + ")");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onFinishedGoingToSleep(why, cameraGestureTriggered);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.interactiveState = 0;
    }

    public void onKeyguardShown(boolean showing) {
        debug("onKeyguardShown(" + showing + ")");
        this.mKeyguardState.showing = showing;
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onKeyguardShown(showing);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
    }

    public void onKeyguardOccluded(boolean occluded) {
        debug("onKeyguardOccluded(" + occluded + ")");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onKeyguardOccluded(occluded);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.occluded = occluded;
    }

    public void onTakeScreenshot() {
        debug("onTakeScreenshot()");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onTakeScreenshot();
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
    }

    public void onSystemKey(KeyEvent event, boolean showing) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        debug(String.format("keyCode: %d, action: %d", Integer.valueOf(keyCode), Integer.valueOf(action)));
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onSystemKey(event, showing);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
    }

    public void onSoftKeyboardShown(boolean shown) {
        debug("onSoftKeyboardShown(" + shown + ")");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onSoftKeyboardShown(shown);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.softKeyboardShown = shown;
    }

    public void onFingerprintRemoved(int remain) {
        debug("onFingerprintRemoved(" + remain + ")");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onFingerprintRemoved(remain);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
    }

    public void onBootMessageDialogShown(boolean shown) {
        debug("onBootMessageDialogShown(" + shown + ")");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onBootMessageDialogShown(shown);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
        this.mKeyguardState.bootMessageDialogShown = shown;
    }

    public void onFaceAuthenticated(int status) {
        debug("onFaceAuthenticated(" + status + ")");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onFaceAuthenticated(status);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
    }

    public void onCredentialVerified(int stage) {
        debug("onCredentialVerified(" + stage + ")");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onCredentialVerified(stage);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
    }

    public void setPowerManagerHandler(Handler handler) {
        this.mPowerManagerHandler = handler;
    }

    public void onInteractiveChanged(boolean interactive) {
        debug("onInteractiveChanged(" + interactive + ")");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onInteractiveChanged(interactive);
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
    }

    public boolean isNeedBlockBrightness() {
        return this.mKeyguardState.isNeedBlockBrightness;
    }

    public boolean isFingerprintWakingUp() {
        return this.mKeyguardState.isFingerprintWakingUp;
    }

    public void onFingerprintWakeUpFinished() {
        debug("onFingerprintWakeUpFinished");
        try {
            IFingerprintKeyguardService fingerprintKeyguardService = this.mFingerprintKeyguardService;
            if (fingerprintKeyguardService != null) {
                fingerprintKeyguardService.onFingerprintWakeUpFinished();
            }
        } catch (RemoteException e) {
            VSlog.w(TAG, "Remote Exception.", e);
        }
    }

    public void setFingerprintWakeUpCallback(FingerprintKeyguardInternal.WakeUpCallback callback) {
        this.mWakeUpCallback = callback;
    }

    public void bindFingerprintKeyguardService() {
        debug("bindFingerprintKeyguardService");
        bindService(this.mContext);
    }

    private void bindService(Context context) {
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint")) {
            debug("Don't bind fingerprint keyguard service due to have no FEATURE_FINGERPRINT !");
            return;
        }
        Intent intent = new Intent();
        ComponentName keyguardComponent = ComponentName.unflattenFromString("com.vivo.fingerprintui/com.vivo.fingerprintui.KeyguardService");
        intent.addFlags(256);
        intent.setComponent(keyguardComponent);
        if (!context.bindServiceAsUser(intent, this.mFingerprintKeyguardConnection, 1, this.mHandler, UserHandle.SYSTEM)) {
            debug("*** FingerprintKeyguard: can't bind to " + keyguardComponent);
            this.mKeyguardState.showing = false;
            return;
        }
        debug("*** FingerprintKeyguard started");
    }

    private static void debug(String msg) {
        VSlog.d(TAG, msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void info(String msg) {
        VSlog.i(TAG, msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void warning(String msg) {
        VSlog.w(TAG, msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class FingerprintKeyguardState {
        boolean bootCompleted;
        boolean bootMessageDialogShown;
        int crashNum = 0;
        int currentUser;
        int forceDisplayState;
        int interactiveState;
        boolean isFingerprintWakingUp;
        boolean isNeedBlockBrightness;
        boolean occluded;
        int screenState;
        boolean showing;
        boolean softKeyboardShown;
        boolean systemIsReady;
        int wakeUpReason;

        FingerprintKeyguardState() {
            reset();
        }

        private void reset() {
            this.showing = true;
            this.softKeyboardShown = false;
            this.currentUser = ProcessList.INVALID_ADJ;
            this.screenState = -1;
            this.isNeedBlockBrightness = false;
            this.isFingerprintWakingUp = false;
            this.forceDisplayState = 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class FingerprintKeyguardCallback extends IFingerprintKeyguardCallback.Stub {
        private static final int KEYGUARD_DONE = 4;
        private IKeyguardService mKeyguardService;
        private PowerManager mPowerManager;
        private IWindowManager mWindowManager;

        public FingerprintKeyguardCallback(PowerManager powerManager) {
            this.mPowerManager = powerManager;
            if (this.mWindowManager == null) {
                this.mWindowManager = WindowManagerGlobal.getWindowManagerService();
            }
        }

        public void setSystemKeyguardService(IKeyguardService keyguardService) {
            this.mKeyguardService = keyguardService;
        }

        public boolean isSystemKeyguardAvalid() {
            return this.mKeyguardService != null;
        }

        public void hideKeyguard(int flag) {
            FingerprintUnlockController.info("hideKeyguard(flag:" + flag + ")");
            if (this.mKeyguardService == null) {
                FingerprintUnlockController.warning("hideKeyguard: no keyguard service");
                return;
            }
            IWindowManager iWindowManager = this.mWindowManager;
            if (iWindowManager == null) {
                FingerprintUnlockController.warning("hideKeyguard: no window manager");
                return;
            }
            try {
                iWindowManager.hideKeyguard(flag);
            } catch (RemoteException e) {
                VSlog.w(FingerprintUnlockController.TAG, "Remote Exception.", e);
            }
        }

        public void unlock(int keyguardState) {
            FingerprintUnlockController.info("unlock(keyguardState:" + keyguardState + ")");
            IKeyguardService iKeyguardService = this.mKeyguardService;
            if (iKeyguardService == null) {
                FingerprintUnlockController.warning("unlock: no keyguard service");
                return;
            }
            try {
                iKeyguardService.hideKeyguardByFingerprint(keyguardState);
            } catch (RemoteException e) {
                VSlog.w(FingerprintUnlockController.TAG, "Remote Exception.", e);
            }
        }

        public void sendMessageToKeyguard(String what, int arg) {
            FingerprintUnlockController.info("sendMessageToKeyguard(msg:" + what + ", arg:" + arg + ")");
            IKeyguardService iKeyguardService = this.mKeyguardService;
            if (iKeyguardService == null) {
                FingerprintUnlockController.warning("sendMessageToKeyguard: no keyguard service");
                return;
            }
            try {
                iKeyguardService.onMessageFromFingerprint(what, arg);
            } catch (RemoteException e) {
                VSlog.w(FingerprintUnlockController.TAG, "Remote Exception.", e);
            }
        }

        public void prepareDisplay() {
            if (FingerprintUnlockController.this.mPowerManagerHandler == null) {
                FingerprintUnlockController.warning("prepareDisplay: no powermanager handler");
            } else if (FingerprintUnlockController.this.mWakeUpCallback == null) {
                FingerprintUnlockController.warning("no wakeup callback");
            } else {
                FingerprintUnlockController.info("prepareDisplay");
                FingerprintUnlockController.this.mPowerManagerHandler.post(new Runnable() { // from class: com.android.server.biometrics.keyguard.-$$Lambda$FingerprintUnlockController$FingerprintKeyguardCallback$sHG9zGCeo5SpVWeuR1rLi1gkxzM
                    @Override // java.lang.Runnable
                    public final void run() {
                        FingerprintUnlockController.FingerprintKeyguardCallback.this.lambda$prepareDisplay$0$FingerprintUnlockController$FingerprintKeyguardCallback();
                    }
                });
            }
        }

        public /* synthetic */ void lambda$prepareDisplay$0$FingerprintUnlockController$FingerprintKeyguardCallback() {
            FingerprintUnlockController.this.mWakeUpCallback.prepareDisplay();
        }

        public void setDisplayState(final int state) {
            if (FingerprintUnlockController.this.mPowerManagerHandler == null) {
                FingerprintUnlockController.warning("setDisplayState: no powermanager handler");
            } else if (FingerprintUnlockController.this.mWakeUpCallback == null) {
                FingerprintUnlockController.warning("no wakeup callback");
            } else {
                FingerprintUnlockController.info("setDisplayState(" + state + ")");
                FingerprintUnlockController.this.mKeyguardState.forceDisplayState = state;
                FingerprintUnlockController.this.mPowerManagerHandler.post(new Runnable() { // from class: com.android.server.biometrics.keyguard.-$$Lambda$FingerprintUnlockController$FingerprintKeyguardCallback$qERl7f-SUlLJiBlmWjEijSyaIws
                    @Override // java.lang.Runnable
                    public final void run() {
                        FingerprintUnlockController.FingerprintKeyguardCallback.this.lambda$setDisplayState$1$FingerprintUnlockController$FingerprintKeyguardCallback(state);
                    }
                });
            }
        }

        public /* synthetic */ void lambda$setDisplayState$1$FingerprintUnlockController$FingerprintKeyguardCallback(int state) {
            FingerprintUnlockController.this.mWakeUpCallback.setDisplayState(state);
        }

        public void updateDisplay() {
            if (FingerprintUnlockController.this.mPowerManagerHandler == null) {
                FingerprintUnlockController.warning("updateDisplay: no powermanager handler");
            } else if (FingerprintUnlockController.this.mWakeUpCallback == null) {
                FingerprintUnlockController.warning("no wakeup callback");
            } else {
                FingerprintUnlockController.info("updateDisplay");
                FingerprintUnlockController.this.mPowerManagerHandler.post(new Runnable() { // from class: com.android.server.biometrics.keyguard.-$$Lambda$FingerprintUnlockController$FingerprintKeyguardCallback$gOaJHMzCuJibdvCLp_P3auNRcMc
                    @Override // java.lang.Runnable
                    public final void run() {
                        FingerprintUnlockController.FingerprintKeyguardCallback.this.lambda$updateDisplay$2$FingerprintUnlockController$FingerprintKeyguardCallback();
                    }
                });
            }
        }

        public /* synthetic */ void lambda$updateDisplay$2$FingerprintUnlockController$FingerprintKeyguardCallback() {
            FingerprintUnlockController.this.mWakeUpCallback.updateDisplay();
        }

        public void requestDraw(final boolean open) {
            if (FingerprintUnlockController.this.mPowerManagerHandler == null) {
                FingerprintUnlockController.warning("requestDraw: no powermanager handler");
            } else if (FingerprintUnlockController.this.mDisplayManagerService == null) {
                FingerprintUnlockController.warning("display manager service is null");
            } else {
                FingerprintUnlockController.info("requestDraw(" + open + ")");
                FingerprintUnlockController.this.mPowerManagerHandler.post(new Runnable() { // from class: com.android.server.biometrics.keyguard.-$$Lambda$FingerprintUnlockController$FingerprintKeyguardCallback$CadSfIc43EOR80uw8420NVq8vZg
                    @Override // java.lang.Runnable
                    public final void run() {
                        FingerprintUnlockController.FingerprintKeyguardCallback.this.lambda$requestDraw$3$FingerprintUnlockController$FingerprintKeyguardCallback(open);
                    }
                });
            }
        }

        public /* synthetic */ void lambda$requestDraw$3$FingerprintUnlockController$FingerprintKeyguardCallback(boolean open) {
            FingerprintUnlockController.this.mDisplayManagerService.requestDraw(open);
        }

        public void setNeedBlockBrightness(boolean blockBrightness) {
            FingerprintUnlockController.info("setNeedBlockBrightness(" + blockBrightness + ")");
            FingerprintUnlockController.this.mKeyguardState.isNeedBlockBrightness = blockBrightness;
        }

        public void setFingerprintWakingUp(boolean fingerprintWakingUp) {
            FingerprintUnlockController.info("setFingerprintWakingUp(" + fingerprintWakingUp + ")");
            FingerprintUnlockController.this.mKeyguardState.isFingerprintWakingUp = fingerprintWakingUp;
        }
    }

    private static String screenStateToString(int state) {
        if (state != -1) {
            if (state != 0) {
                if (state != 1) {
                    if (state != 2) {
                        if (state != 3) {
                            return "unknown";
                        }
                        return "screen_turning_off";
                    }
                    return "screen_turned_on";
                }
                return "screen_turning_on";
            }
            return "screen_turned_off";
        }
        return "screen_init";
    }
}