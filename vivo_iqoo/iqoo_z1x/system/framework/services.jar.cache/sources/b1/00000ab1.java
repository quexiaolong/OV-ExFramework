package com.android.server.biometrics.iris;

import android.hardware.biometrics.IBiometricAuthenticator;
import android.hardware.biometrics.IBiometricServiceReceiverInternal;
import android.hardware.iris.IIrisService;
import android.os.IBinder;
import android.os.RemoteException;

/* loaded from: classes.dex */
public final class IrisAuthenticator extends IBiometricAuthenticator.Stub {
    private final IIrisService mIrisService;

    public IrisAuthenticator(IIrisService irisService) {
        this.mIrisService = irisService;
    }

    public void prepareForAuthentication(boolean requireConfirmation, IBinder token, long sessionId, int userId, IBiometricServiceReceiverInternal wrapperReceiver, String opPackageName, int cookie, int callingUid, int callingPid, int callingUserId) throws RemoteException {
    }

    public void startPreparedClient(int cookie) throws RemoteException {
    }

    public void cancelAuthenticationFromService(IBinder token, String opPackageName, int callingUid, int callingPid, int callingUserId, boolean fromClient) throws RemoteException {
    }

    public boolean isHardwareDetected(String opPackageName) throws RemoteException {
        return false;
    }

    public boolean hasEnrolledTemplates(int userId, String opPackageName) throws RemoteException {
        return false;
    }

    public void resetLockout(byte[] token) throws RemoteException {
    }

    public void setActiveUser(int uid) throws RemoteException {
    }

    public long getAuthenticatorId(int callingUserId) throws RemoteException {
        return 0L;
    }
}