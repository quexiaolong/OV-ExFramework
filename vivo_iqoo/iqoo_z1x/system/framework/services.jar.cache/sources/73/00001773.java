package com.android.server.policy.keyguard;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.server.policy.keyguard.KeyguardStateMonitor;
import java.io.PrintWriter;
import vivo.util.VSlog;

/* loaded from: classes2.dex */
public class KeyguardServiceWrapper implements IKeyguardService {
    private String TAG;
    private KeyguardStateMonitor mKeyguardStateMonitor;
    private IKeyguardService mService;

    public KeyguardServiceWrapper(Context context, IKeyguardService service, KeyguardStateMonitor.StateCallback callback) {
        this(context, service, callback, null);
    }

    public KeyguardServiceWrapper(Context context, IKeyguardService service, KeyguardStateMonitor.StateCallback callback, KeyguardStateMonitor.StateMonitorCallback fingerprintCallback) {
        this.TAG = "KeyguardServiceWrapper";
        this.mService = service;
        this.mKeyguardStateMonitor = new KeyguardStateMonitor(context, service, callback, fingerprintCallback);
    }

    public void verifyUnlock(IKeyguardExitCallback callback) {
        try {
            this.mService.verifyUnlock(callback);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void setOccluded(boolean isOccluded, boolean animate) {
        try {
            this.mService.setOccluded(isOccluded, animate);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void addStateMonitorCallback(IKeyguardStateCallback callback) {
        try {
            this.mService.addStateMonitorCallback(callback);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void dismiss(IKeyguardDismissCallback callback, CharSequence message) {
        try {
            this.mService.dismiss(callback, message);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onDreamingStarted() {
        try {
            this.mService.onDreamingStarted();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onDreamingStopped() {
        try {
            this.mService.onDreamingStopped();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onStartedGoingToSleep(int reason) {
        try {
            this.mService.onStartedGoingToSleep(reason);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onFinishedGoingToSleep(int reason, boolean cameraGestureTriggered) {
        try {
            this.mService.onFinishedGoingToSleep(reason, cameraGestureTriggered);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onStartedWakingUp() {
        try {
            this.mService.onStartedWakingUp();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onStartedWakingUpForReason(int reason) {
        try {
            this.mService.onStartedWakingUpForReason(reason);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onFinishedWakingUp() {
        try {
            this.mService.onFinishedWakingUp();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onScreenTurningOn(IKeyguardDrawnCallback callback) {
        try {
            this.mService.onScreenTurningOn(callback);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onScreenTurnedOn() {
        try {
            this.mService.onScreenTurnedOn();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onScreenTurningOff() {
        try {
            this.mService.onScreenTurningOff();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onScreenTurnedOff() {
        try {
            this.mService.onScreenTurnedOff();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void setKeyguardEnabled(boolean enabled) {
        try {
            this.mService.setKeyguardEnabled(enabled);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onSystemReady() {
        try {
            this.mService.onSystemReady();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void doKeyguardTimeout(Bundle options) {
        try {
            this.mService.doKeyguardTimeout(options);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void setSwitchingUser(boolean switching) {
        try {
            this.mService.setSwitchingUser(switching);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void setCurrentUser(int userId) {
        this.mKeyguardStateMonitor.setCurrentUser(userId);
        try {
            this.mService.setCurrentUser(userId);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onBootCompleted() {
        try {
            this.mService.onBootCompleted();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        try {
            this.mService.startKeyguardExitAnimation(startTime, fadeoutDuration);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onShortPowerPressedGoHome() {
        try {
            this.mService.onShortPowerPressedGoHome();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public IBinder asBinder() {
        return this.mService.asBinder();
    }

    public void hideKeyguardByFingerprint(int hide) {
        try {
            this.mKeyguardStateMonitor.unlockReason(hide);
            this.mService.hideKeyguardByFingerprint(hide);
        } catch (RemoteException e) {
            VSlog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onMessageFromFingerprint(String what, int arg) {
        try {
            this.mService.onMessageFromFingerprint(what, arg);
        } catch (RemoteException e) {
            VSlog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void sendMessageToKeyguard(String module, String message, int extra) {
        try {
            this.mService.sendMessageToKeyguard(module, message, extra);
        } catch (RemoteException e) {
            VSlog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void screenFadingOn(boolean isOn, int code) {
        try {
            this.mService.screenFadingOn(isOn, code);
        } catch (RemoteException e) {
            VSlog.w(this.TAG, "Remote Exception", e);
        }
    }

    public boolean isShowing() {
        return this.mKeyguardStateMonitor.isShowing();
    }

    public boolean isTrusted() {
        return this.mKeyguardStateMonitor.isTrusted();
    }

    public boolean hasLockscreenWallpaper() {
        return this.mKeyguardStateMonitor.hasLockscreenWallpaper();
    }

    public boolean isSecure(int userId) {
        return this.mKeyguardStateMonitor.isSecure(userId);
    }

    public boolean isInputRestricted() {
        return this.mKeyguardStateMonitor.isInputRestricted();
    }

    public void dump(String prefix, PrintWriter pw) {
        this.mKeyguardStateMonitor.dump(prefix, pw);
    }
}