package com.android.server.biometrics.face;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.NativeHandle;
import android.os.RemoteException;
import com.android.server.LocalServices;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.face.common.utils.FaceUIFactory;
import com.vivo.face.internal.ui.IFaceUI;
import com.vivo.face.internal.ui.IFaceUIManagerService;
import java.io.FileDescriptor;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class FaceUIManagerServiceAdapter extends AbsFaceUIManagerServiceAdapter implements IBinder.DeathRecipient {
    private Binder mBinder;
    private boolean mEnrollmentStarted;
    private IFaceUI mFaceUI;
    private FaceUIState mFaceUIState;
    private NativeHandle windowId;

    public FaceUIManagerServiceAdapter(Context context) {
        super(context);
        this.windowId = null;
        this.mBinder = new IFaceUIManagerService.Stub() { // from class: com.android.server.biometrics.face.FaceUIManagerServiceAdapter.1
            @Override // com.vivo.face.internal.ui.IFaceUIManagerService
            public void registerFaceUI(IFaceUI callback) {
                FaceUIManagerServiceAdapter.this.mFaceUI = callback;
                if (FaceUIManagerServiceAdapter.this.mFaceUI == null) {
                    VSlog.e("FaceUIManagerServiceAdapter", "Invalid callback while registering FaceUI");
                    return;
                }
                VSlog.i("FaceUIManagerServiceAdapter", "register FaceUI");
                try {
                    FaceUIManagerServiceAdapter.this.mFaceUI.asBinder().linkToDeath(FaceUIManagerServiceAdapter.this, 0);
                    if (FaceUIManagerServiceAdapter.this.mFaceUIState.mHidlServiceDied && !FaceUIManagerServiceAdapter.this.mFaceUIState.mHidlServiceDiedCmdEffective) {
                        VSlog.i("FaceUIManagerServiceAdapter", "register FaceUI-send hidl service died message");
                        FaceUIManagerServiceAdapter.this.onHidlServiceDied();
                    }
                    if (FaceUIManagerServiceAdapter.this.mFaceUIState.mFaceRemoved && !FaceUIManagerServiceAdapter.this.mFaceUIState.mFaceRemovedCmdEffective) {
                        VSlog.i("FaceUIManagerServiceAdapter", "register FaceUI-send face removed message");
                        FaceUIManagerServiceAdapter.this.onRemoved();
                    }
                    if (FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticateInfo.received && FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticateInfo.inAuth) {
                        if (!FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticateInfo.startCmdEffective) {
                            VSlog.i("FaceUIManagerServiceAdapter", "FaceUI restart authenticate error v1");
                            FaceUIManagerServiceAdapter.this.startAuthenticate(FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticateInfo.token, FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticateInfo.opPackageName);
                        }
                        if (FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticateInfo.startCmdEffective && !FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticateInfo.cancelCmdEffective) {
                            VSlog.i("FaceUIManagerServiceAdapter", "FaceUI restart authenticate error v2");
                            FaceUIManagerServiceAdapter.this.startAuthenticate(FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticateInfo.token, FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticateInfo.opPackageName);
                        }
                    }
                    if (FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticationResultInfo.received && FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticationResultInfo.succeed && !FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticationResultInfo.cmdEffective) {
                        VSlog.i("FaceUIManagerServiceAdapter", "register FaceUI-send authentication succeed message");
                        FaceUIManagerServiceAdapter.this.onAuthenticationSucceeded(FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticationResultInfo.faceId, FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticationResultInfo.userId);
                    }
                    if (FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticationResultInfo.received && !FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticationResultInfo.succeed && !FaceUIManagerServiceAdapter.this.mFaceUIState.mAuthenticationResultInfo.cmdEffective) {
                        VSlog.i("FaceUIManagerServiceAdapter", "register FaceUI-send authentication failed message");
                        FaceUIManagerServiceAdapter.this.onAuthenticationFailed();
                    }
                    if (FaceUIManagerServiceAdapter.this.mFaceUIState.mAcquireInfo.received && !FaceUIManagerServiceAdapter.this.mFaceUIState.mAcquireInfo.cmdEffective) {
                        VSlog.i("FaceUIManagerServiceAdapter", "register FaceUI-send acquired message");
                        FaceUIManagerServiceAdapter.this.onAcquired(FaceUIManagerServiceAdapter.this.mFaceUIState.mAcquireInfo.acquiredInfo, FaceUIManagerServiceAdapter.this.mFaceUIState.mAcquireInfo.vendorCode);
                    }
                    if (FaceUIManagerServiceAdapter.this.mFaceUIState.mErrorInfo.received && !FaceUIManagerServiceAdapter.this.mFaceUIState.mErrorInfo.cmdEffective) {
                        VSlog.i("FaceUIManagerServiceAdapter", "register FaceUI-send error message");
                        FaceUIManagerServiceAdapter.this.onError(FaceUIManagerServiceAdapter.this.mFaceUIState.mErrorInfo.error, FaceUIManagerServiceAdapter.this.mFaceUIState.mErrorInfo.vendorCode);
                    }
                    if (FaceUIManagerServiceAdapter.this.mFaceUIState.mAlgorithmResultInfo.received && !FaceUIManagerServiceAdapter.this.mFaceUIState.mAlgorithmResultInfo.cmdEffective) {
                        VSlog.i("FaceUIManagerServiceAdapter", "register FaceUI-send algorithm result message");
                        FaceUIManagerServiceAdapter.this.onFaceAlgorithmResult(FaceUIManagerServiceAdapter.this.mFaceUIState.mAlgorithmResultInfo.command, FaceUIManagerServiceAdapter.this.mFaceUIState.mAlgorithmResultInfo.result, FaceUIManagerServiceAdapter.this.mFaceUIState.mAlgorithmResultInfo.extras, FaceUIManagerServiceAdapter.this.mFaceUIState.mAlgorithmResultInfo.bundle);
                    }
                    if (!FaceUIManagerServiceAdapter.this.mFaceUIState.mCommandInfo.received || FaceUIManagerServiceAdapter.this.mFaceUIState.mCommandInfo.cmdEffective) {
                        if (FaceUIManagerServiceAdapter.this.mFaceUIState.mCommandInfo.received && FaceUIManagerServiceAdapter.this.mFaceUIState.mCommandInfo.cmdEffective && FaceUIManagerServiceAdapter.this.mEnrollmentStarted) {
                            VSlog.i("FaceUIManagerServiceAdapter", "register FaceUI-send write data command");
                            FaceUIManagerServiceAdapter.this.sendCommand(1002, FaceUIManagerServiceAdapter.this.mFaceUIState.mCommandInfo.extra, FaceUIManagerServiceAdapter.this.mFaceUIState.mCommandInfo.bundle);
                            return;
                        }
                        return;
                    }
                    VSlog.i("FaceUIManagerServiceAdapter", "register FaceUI-send command");
                    FaceUIManagerServiceAdapter.this.sendCommand(FaceUIManagerServiceAdapter.this.mFaceUIState.mCommandInfo.command, FaceUIManagerServiceAdapter.this.mFaceUIState.mCommandInfo.extra, FaceUIManagerServiceAdapter.this.mFaceUIState.mCommandInfo.bundle);
                } catch (RemoteException ex) {
                    VSlog.e("FaceUIManagerServiceAdapter", "Failed to register FaceUI", ex);
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUIManagerService
            public void sendMessageToFingerprint(int msg, String extra, byte[] array) {
                FaceUIManagerServiceAdapter.this.sendMessageToFingerprint(msg, extra, array);
            }

            @Override // com.vivo.face.internal.ui.IFaceUIManagerService
            public FileDescriptor getMemoryFileDescriptor(int memorySize) {
                FaceUIManagerServiceAdapter.this.setSharedMemorySize(memorySize);
                return FaceUIManagerServiceAdapter.this.generateFileDescriptor(memorySize);
            }

            @Override // com.vivo.face.internal.ui.IFaceUIManagerService
            public void sendLockoutState(boolean lockoutReady) {
                FaceUIManagerServiceAdapter.this.sendLockoutState(lockoutReady);
            }

            @Override // com.vivo.face.internal.ui.IFaceUIManagerService
            public void sendDialogVisibleState(boolean visible) {
                FaceUIManagerServiceAdapter.this.sendDialogVisibleState(visible);
            }

            @Override // com.vivo.face.internal.ui.IFaceUIManagerService
            public void sendAuthenticationResult(boolean succeed) {
                FaceUIManagerServiceAdapter.this.sendAuthenticationResult(succeed);
            }
        };
        this.mFaceUIState = new FaceUIState();
        LocalServices.addService(FaceInternal.class, new LocalService());
    }

    @Override // com.android.server.biometrics.face.AbsFaceUIManagerServiceAdapter
    public IBinder asBinder() {
        return this.mBinder;
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        VSlog.e("FaceUIManagerServiceAdapter", "binderDied");
        this.mFaceUI = null;
    }

    public FileDescriptor getShareMemoryFd(int memorySize, String opPackageName) {
        if (getSharedMemorySize() == memorySize && FaceUIFactory.checkSystemApplication(this.mContext, opPackageName)) {
            return generateFileDescriptor(memorySize);
        }
        return null;
    }

    public NativeHandle getNativeHandle() {
        if (this.mFileDescriptor != null && this.windowId == null) {
            try {
                this.windowId = new NativeHandle(this.mFileDescriptor, false);
            } catch (Exception e) {
                VSlog.e("FaceUIManagerServiceAdapter", "fail to get native handle", e);
            }
        }
        return this.windowId;
    }

    public void startAuthenticate(IBinder token, String opPackageName) {
        VSlog.i("FaceUIManagerServiceAdapter", "startAuthenticate(" + opPackageName + ")");
        this.mFaceUIState.mAuthenticateInfo.setInfo(token, opPackageName);
        this.mFaceUIState.mAuthenticateInfo.inAuth = true;
        this.mFaceUIState.mAuthenticateInfo.received = true;
        this.mFaceUIState.mAuthenticateInfo.startCmdEffective = false;
        this.mFaceUIState.mAuthenticateInfo.cancelCmdEffective = false;
        if (this.mFaceUI == null) {
            VSlog.w("FaceUIManagerServiceAdapter", "Failed to start authenticate");
            return;
        }
        try {
            this.mFaceUIState.mAuthenticateInfo.startCmdEffective = true;
            this.mFaceUI.startAuthenticate(token, opPackageName);
        } catch (RemoteException ex) {
            this.mFaceUIState.mAuthenticateInfo.cancelCmdEffective = false;
            VSlog.e("FaceUIManagerServiceAdapter", "Remote exception while starting authentication", ex);
        }
    }

    public void cancelAuthenticate(IBinder token, String opPackageName) {
        VSlog.i("FaceUIManagerServiceAdapter", "cancelAuthenticate(" + opPackageName + ")");
        this.mFaceUIState.mAuthenticateInfo.inAuth = false;
        this.mFaceUIState.mAuthenticateInfo.received = true;
        this.mFaceUIState.mAuthenticateInfo.cancelCmdEffective = false;
        if (this.mFaceUI == null) {
            VSlog.w("FaceUIManagerServiceAdapter", "Failed to cancel authenticate");
            return;
        }
        try {
            this.mFaceUIState.mAuthenticateInfo.cancelCmdEffective = true;
            this.mFaceUI.cancelAuthenticate(token, opPackageName);
        } catch (RemoteException ex) {
            this.mFaceUIState.mAuthenticateInfo.cancelCmdEffective = false;
            VSlog.e("FaceUIManagerServiceAdapter", "Remote exception while canceling authentication", ex);
        }
    }

    public void onAcquired(int acquiredInfo, int vendorCode) {
        this.mFaceUIState.mAcquireInfo.setInfo(acquiredInfo, vendorCode);
        this.mFaceUIState.mAcquireInfo.received = true;
        this.mFaceUIState.mAcquireInfo.cmdEffective = false;
        if (this.mFaceUI == null) {
            VSlog.w("FaceUIManagerServiceAdapter", "Failed to send acquire message");
            return;
        }
        try {
            this.mFaceUIState.mAcquireInfo.cmdEffective = true;
            this.mFaceUI.onAcquired(acquiredInfo, vendorCode);
        } catch (RemoteException ex) {
            this.mFaceUIState.mAcquireInfo.cmdEffective = false;
            VSlog.e("FaceUIManagerServiceAdapter", "Remote exception while sending acquire message", ex);
        }
    }

    public void onAuthenticationSucceeded(int faceId, int userId) {
        this.mFaceUIState.mAuthenticationResultInfo.setInfo(faceId, userId);
        this.mFaceUIState.mAuthenticationResultInfo.succeed = true;
        this.mFaceUIState.mAuthenticationResultInfo.received = true;
        this.mFaceUIState.mAuthenticationResultInfo.cmdEffective = false;
        this.mFaceUIState.mAuthenticateInfo.inAuth = false;
        if (this.mFaceUI == null) {
            VSlog.w("FaceUIManagerServiceAdapter", "Failed to send succeed authentication result");
            return;
        }
        try {
            this.mFaceUIState.mAuthenticationResultInfo.cmdEffective = true;
            this.mFaceUI.onAuthenticationSucceeded(faceId, userId);
        } catch (RemoteException ex) {
            this.mFaceUIState.mAuthenticationResultInfo.cmdEffective = false;
            VSlog.e("FaceUIManagerServiceAdapter", "Remote exception while sending succeed authentication result", ex);
        }
    }

    public void onAuthenticationFailed() {
        this.mFaceUIState.mAuthenticationResultInfo.setInfo(0, 0);
        this.mFaceUIState.mAuthenticationResultInfo.succeed = false;
        this.mFaceUIState.mAuthenticationResultInfo.received = true;
        this.mFaceUIState.mAuthenticationResultInfo.cmdEffective = false;
        this.mFaceUIState.mAuthenticateInfo.inAuth = false;
        if (this.mFaceUI == null) {
            VSlog.w("FaceUIManagerServiceAdapter", "Failed to send failed authentication result");
            return;
        }
        try {
            this.mFaceUIState.mAuthenticationResultInfo.cmdEffective = true;
            this.mFaceUI.onAuthenticationFailed();
        } catch (RemoteException ex) {
            this.mFaceUIState.mAuthenticationResultInfo.cmdEffective = false;
            VSlog.e("FaceUIManagerServiceAdapter", "Remote exception while sending failed authentication result", ex);
        }
        setSystemUIAuthFailedAfterLockoutReady();
    }

    public void onError(int error, int vendorCode) {
        this.mFaceUIState.mErrorInfo.setInfo(error, vendorCode);
        this.mFaceUIState.mErrorInfo.received = true;
        this.mFaceUIState.mErrorInfo.cmdEffective = false;
        if (this.mFaceUI == null) {
            VSlog.w("FaceUIManagerServiceAdapter", "Failed to send error message");
            return;
        }
        try {
            this.mFaceUIState.mErrorInfo.cmdEffective = true;
            this.mFaceUI.onError(error, vendorCode);
        } catch (RemoteException ex) {
            this.mFaceUIState.mErrorInfo.cmdEffective = false;
            VSlog.e("FaceUIManagerServiceAdapter", "Remote exception while sending error message", ex);
        }
    }

    public void onRemoved() {
        this.mFaceUIState.mFaceRemoved = true;
        this.mFaceUIState.mFaceRemovedCmdEffective = false;
        if (this.mFaceUI == null) {
            VSlog.w("FaceUIManagerServiceAdapter", "Failed to send face removed message");
            return;
        }
        try {
            this.mFaceUIState.mFaceRemovedCmdEffective = true;
            this.mFaceUI.onRemoved();
        } catch (RemoteException ex) {
            this.mFaceUIState.mFaceRemovedCmdEffective = false;
            VSlog.e("FaceUIManagerServiceAdapter", "Remote exception while sending face removed message", ex);
        }
    }

    public void onFaceAlgorithmResult(int command, int result, int extras, String bundle) {
        this.mFaceUIState.mAlgorithmResultInfo.setInfo(command, result, extras, bundle);
        this.mFaceUIState.mAlgorithmResultInfo.received = true;
        this.mFaceUIState.mAlgorithmResultInfo.cmdEffective = false;
        if (this.mFaceUI == null) {
            VSlog.w("FaceUIManagerServiceAdapter", "Failed to send algorithm message");
            return;
        }
        try {
            this.mFaceUIState.mAlgorithmResultInfo.cmdEffective = true;
            this.mFaceUI.onFaceAlgorithmResult(command, result, extras, bundle);
        } catch (RemoteException ex) {
            this.mFaceUIState.mAlgorithmResultInfo.cmdEffective = false;
            VSlog.e("FaceUIManagerServiceAdapter", "Remote exception while sending algorithm message", ex);
        }
    }

    public void onHidlServiceDied() {
        this.mFaceUIState.mHidlServiceDied = true;
        this.mFaceUIState.mHidlServiceDiedCmdEffective = false;
        if (this.mFaceUI == null) {
            VSlog.w("FaceUIManagerServiceAdapter", "Failed to send hidl service died message");
            return;
        }
        try {
            this.mFaceUIState.mHidlServiceDiedCmdEffective = true;
            this.mFaceUI.onHidlServiceDied();
        } catch (RemoteException ex) {
            this.mFaceUIState.mHidlServiceDiedCmdEffective = false;
            VSlog.e("FaceUIManagerServiceAdapter", "Remote exception while sending hidl service died message", ex);
        }
    }

    public void sendCommand(int command, int extra, String bundle) {
        this.mFaceUIState.mCommandInfo.setInfo(command, extra, bundle);
        this.mFaceUIState.mCommandInfo.received = true;
        this.mFaceUIState.mCommandInfo.cmdEffective = false;
        if (this.mFaceUI == null) {
            VSlog.w("FaceUIManagerServiceAdapter", "Failed to send command");
            return;
        }
        try {
            this.mFaceUIState.mCommandInfo.cmdEffective = true;
            this.mFaceUI.sendCommand(command, extra, bundle);
        } catch (RemoteException ex) {
            this.mFaceUIState.mCommandInfo.cmdEffective = false;
            VSlog.e("FaceUIManagerServiceAdapter", "Remote exception while sending command", ex);
        }
    }

    public void onSystemTime(long elapsedRealtime, int what) {
        IFaceUI iFaceUI = this.mFaceUI;
        if (iFaceUI == null) {
            VSlog.w("FaceUIManagerServiceAdapter", "Failed to send systemTime");
            return;
        }
        try {
            iFaceUI.onSystemTime(elapsedRealtime, what);
        } catch (RemoteException ex) {
            VSlog.e("FaceUIManagerServiceAdapter", "Remote exception while sending command", ex);
        }
    }

    public void onEnrollmentStateChanged(boolean started) {
        this.mEnrollmentStarted = started;
        IFaceUI iFaceUI = this.mFaceUI;
        if (iFaceUI == null) {
            VSlog.w("FaceUIManagerServiceAdapter", "Failed to notify enrollment state changed");
            return;
        }
        try {
            iFaceUI.onEnrollmentStateChanged(started);
        } catch (RemoteException e) {
            VSlog.e("FaceUIManagerServiceAdapter", "Remote exception while notify enrollment state changed");
        }
    }

    public boolean isLockout() {
        return getLockout();
    }

    public void setSystemUIAuthFailedAfterLockoutReady() {
        if (this.mFaceUIState.mAuthenticateInfo.isSystemUI() && !this.mFaceUIState.mAcquireInfo.isFaceNotDetected()) {
            setLockout(getLockoutReady());
        }
    }

    /* loaded from: classes.dex */
    private class LocalService extends FaceInternal {
        private LocalService() {
        }

        @Override // com.android.server.biometrics.face.FaceInternal
        public void systemTime(long elapsedRealtime, int what) {
            FaceUIManagerServiceAdapter.this.onSystemTime(elapsedRealtime, what);
        }
    }
}