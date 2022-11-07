package com.vivo.face.common.keyguard;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.hardware.display.DisplayManagerInternal;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.policy.IKeyguardService;
import com.android.server.LocalServices;
import com.vivo.face.common.utils.FaceLog;
import com.vivo.face.internal.keyguard.IKeyguardCallback;

/* loaded from: classes.dex */
public final class KeyguardCallback extends IKeyguardCallback.Stub {
    private static final int KEYGUARD_HIDE = 1;
    private static final int KEYGUARD_HIDING = 3;
    private static final int KEYGUARD_NONE = 0;
    private static final int KEYGUARD_SHOW = 2;
    private static final int KEYGUARD_SHOW_STATUS_BAR = 8;
    private static final int KEYGUARD_UNLOCK_FACE = 5;
    private static final int KEYGUARD_UNLOCK_FINGER = 4;
    private static final int KEYGUARD_UNLOCK_NATIVE = 7;
    private static final String TAG = "KeyguardCallback";
    private IKeyguardService mKeyguardService;
    private IActivityTaskManager mActivityTaskManager = ActivityTaskManager.getService();
    private IWindowManager mWindowManager = WindowManagerGlobal.getWindowManagerService();
    private DisplayManagerInternal mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);

    public void setSystemKeyguardService(IKeyguardService keyguardService) {
        this.mKeyguardService = keyguardService;
    }

    public boolean isSystemKeyguardServiceValid() {
        return this.mKeyguardService != null;
    }

    @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
    public void hideKeyguard() {
        if (this.mKeyguardService == null) {
            FaceLog.e(TAG, "invalid keyguard service while hiding keyguard");
            return;
        }
        IWindowManager iWindowManager = this.mWindowManager;
        if (iWindowManager == null) {
            FaceLog.e(TAG, "invalid window manager while hiding keyguard");
            return;
        }
        try {
            iWindowManager.hideKeyguard(1);
        } catch (RemoteException e) {
            FaceLog.e(TAG, "Failed to hide keyguard", e);
        }
    }

    @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
    public void showKeyguard() {
        if (this.mKeyguardService == null) {
            FaceLog.e(TAG, "invalid keyguard service while showing keyguard");
            return;
        }
        IWindowManager iWindowManager = this.mWindowManager;
        if (iWindowManager == null) {
            FaceLog.e(TAG, "invalid window manager while showing keyguard");
            return;
        }
        try {
            iWindowManager.hideKeyguard(2);
        } catch (RemoteException e) {
            FaceLog.e(TAG, "Failed to show keyguard", e);
        }
    }

    @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
    public void unlockKeyguard(boolean isNativeUnlock) {
        if (this.mKeyguardService == null) {
            FaceLog.e(TAG, "invalid keyguard service while unlocking keyguard");
            return;
        }
        if (isNativeUnlock) {
            SystemProperties.set("sys.fingerprint.keguard", "2");
        } else {
            SystemProperties.set("sys.fingerprint.keguard", "1");
        }
        try {
            this.mKeyguardService.hideKeyguardByFingerprint(isNativeUnlock ? 7 : 5);
        } catch (RemoteException e) {
            FaceLog.e(TAG, "Failed to unlock keyguard", e);
        }
    }

    @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
    public void sendMessageToKeyguard(String message, int extra) {
        IKeyguardService iKeyguardService = this.mKeyguardService;
        if (iKeyguardService == null) {
            FaceLog.e(TAG, "invalid keyguard service while sending message to keyguard");
            return;
        }
        try {
            iKeyguardService.sendMessageToKeyguard("faceunlock", message, extra);
        } catch (RemoteException e) {
            FaceLog.e(TAG, "Failed to send keyguard message", e);
        }
    }

    @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
    public void blockScreenOn() {
        if (this.mKeyguardService == null) {
            FaceLog.e(TAG, "invalid keyguard service while blocking screen on");
            return;
        }
        DisplayManagerInternal displayManagerInternal = this.mDisplayManagerInternal;
        if (displayManagerInternal == null) {
            FaceLog.e(TAG, "invalid display manager while blocking screen on");
        } else {
            displayManagerInternal.faceBlockScreenOn(true);
        }
    }

    @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
    public void unblockScreenOn() {
        if (this.mKeyguardService == null) {
            FaceLog.e(TAG, "invalid keyguard service while unblocking screen on");
            return;
        }
        DisplayManagerInternal displayManagerInternal = this.mDisplayManagerInternal;
        if (displayManagerInternal == null) {
            FaceLog.e(TAG, "invalid display manager while unblocking screen on");
        } else {
            displayManagerInternal.faceBlockScreenOn(false);
        }
    }

    @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
    public void showStatusBar() {
        IKeyguardService iKeyguardService = this.mKeyguardService;
        if (iKeyguardService == null) {
            FaceLog.e(TAG, "invalid keyguard service while showing status bar");
            return;
        }
        try {
            iKeyguardService.hideKeyguardByFingerprint(8);
        } catch (RemoteException e) {
            FaceLog.e(TAG, "Failed to show status bar", e);
        }
    }
}