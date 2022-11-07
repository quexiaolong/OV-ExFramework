package com.android.server.biometrics.fingerprint;

import android.hardware.biometrics.IBiometricAuthenticator;
import android.hardware.biometrics.IBiometricServiceReceiverInternal;
import android.hardware.fingerprint.IFingerprintService;
import android.os.IBinder;
import android.os.RemoteException;

/* loaded from: classes.dex */
public final class FingerprintAuthenticator extends IBiometricAuthenticator.Stub {
    private final IFingerprintService mFingerprintService;

    public FingerprintAuthenticator(IFingerprintService fingerprintService) {
        this.mFingerprintService = fingerprintService;
    }

    public void prepareForAuthentication(boolean requireConfirmation, IBinder token, long sessionId, int userId, IBiometricServiceReceiverInternal wrapperReceiver, String opPackageName, int cookie, int callingUid, int callingPid, int callingUserId) throws RemoteException {
        this.mFingerprintService.prepareForAuthentication(token, sessionId, userId, wrapperReceiver, opPackageName, cookie, callingUid, callingPid, callingUserId);
    }

    public void startPreparedClient(int cookie) throws RemoteException {
        this.mFingerprintService.startPreparedClient(cookie);
    }

    public void cancelAuthenticationFromService(IBinder token, String opPackageName, int callingUid, int callingPid, int callingUserId, boolean fromClient) throws RemoteException {
        this.mFingerprintService.cancelAuthenticationFromService(token, opPackageName, callingUid, callingPid, callingUserId, fromClient);
    }

    public boolean isHardwareDetected(String opPackageName) throws RemoteException {
        return this.mFingerprintService.isHardwareDetected(opPackageName);
    }

    public boolean hasEnrolledTemplates(int userId, String opPackageName) throws RemoteException {
        return this.mFingerprintService.hasEnrolledFingerprints(userId, opPackageName);
    }

    public void resetLockout(byte[] token) throws RemoteException {
        this.mFingerprintService.resetTimeout(token);
    }

    public void setActiveUser(int uid) throws RemoteException {
        this.mFingerprintService.setActiveUser(uid);
    }

    public long getAuthenticatorId(int callingUserId) throws RemoteException {
        return this.mFingerprintService.getAuthenticatorId(callingUserId);
    }
}