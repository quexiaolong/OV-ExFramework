package com.android.server.biometrics.face;

import android.hardware.biometrics.IBiometricAuthenticator;
import android.hardware.biometrics.IBiometricServiceReceiverInternal;
import android.hardware.face.IFaceService;
import android.os.IBinder;
import android.os.RemoteException;

/* loaded from: classes.dex */
public final class FaceAuthenticator extends IBiometricAuthenticator.Stub {
    private final IFaceService mFaceService;

    public FaceAuthenticator(IFaceService faceService) {
        this.mFaceService = faceService;
    }

    public void prepareForAuthentication(boolean requireConfirmation, IBinder token, long sessionId, int userId, IBiometricServiceReceiverInternal wrapperReceiver, String opPackageName, int cookie, int callingUid, int callingPid, int callingUserId) throws RemoteException {
        this.mFaceService.prepareForAuthentication(requireConfirmation, token, sessionId, userId, wrapperReceiver, opPackageName, cookie, callingUid, callingPid, callingUserId);
    }

    public void startPreparedClient(int cookie) throws RemoteException {
        this.mFaceService.startPreparedClient(cookie);
    }

    public void cancelAuthenticationFromService(IBinder token, String opPackageName, int callingUid, int callingPid, int callingUserId, boolean fromClient) throws RemoteException {
        this.mFaceService.cancelAuthenticationFromService(token, opPackageName, callingUid, callingPid, callingUserId, fromClient);
    }

    public boolean isHardwareDetected(String opPackageName) throws RemoteException {
        return this.mFaceService.isHardwareDetected(opPackageName);
    }

    public boolean hasEnrolledTemplates(int userId, String opPackageName) throws RemoteException {
        return this.mFaceService.hasEnrolledFaces(userId, opPackageName);
    }

    public void resetLockout(byte[] token) throws RemoteException {
        this.mFaceService.resetLockout(token);
    }

    public void setActiveUser(int uid) throws RemoteException {
        this.mFaceService.setActiveUser(uid);
    }

    public long getAuthenticatorId(int callingUserId) throws RemoteException {
        return this.mFaceService.getAuthenticatorId(callingUserId);
    }
}