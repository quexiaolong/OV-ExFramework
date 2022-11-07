package com.vivo.face.common.state;

import android.os.IBinder;

/* loaded from: classes.dex */
public final class FaceUIState {
    public static final int FACE_ACQUIRED_NOT_DETECTED = 11;
    public static final String PKG_FACEUI = "com.vivo.faceui";
    public static final String PKG_SYSTEMUI = "com.android.systemui";
    public boolean mFaceRemoved;
    public boolean mFaceRemovedCmdEffective;
    public boolean mHidlServiceDied;
    public boolean mHidlServiceDiedCmdEffective;
    public ErrorInfo mErrorInfo = new ErrorInfo();
    public CommandInfo mCommandInfo = new CommandInfo();
    public AcquireInfo mAcquireInfo = new AcquireInfo();
    public AuthenticateInfo mAuthenticateInfo = new AuthenticateInfo();
    public AlgorithmResultInfo mAlgorithmResultInfo = new AlgorithmResultInfo();
    public AuthenticationResultInfo mAuthenticationResultInfo = new AuthenticationResultInfo();

    /* loaded from: classes.dex */
    public static class AuthenticateInfo {
        public boolean cancelCmdEffective;
        public boolean inAuth;
        public String opPackageName;
        public boolean received;
        public boolean startCmdEffective;
        public IBinder token;

        public void setInfo(IBinder token, String opPackageName) {
            this.token = token;
            this.opPackageName = opPackageName;
        }

        public boolean isSystemUI() {
            return FaceUIState.PKG_SYSTEMUI.equals(this.opPackageName);
        }

        public boolean isFaceUI() {
            return FaceUIState.PKG_FACEUI.equals(this.opPackageName);
        }
    }

    /* loaded from: classes.dex */
    public static class AcquireInfo {
        public int acquiredInfo;
        public boolean cmdEffective;
        public boolean received;
        public int vendorCode;

        public void setInfo(int acquiredInfo, int vendorCode) {
            this.acquiredInfo = acquiredInfo;
            this.vendorCode = vendorCode;
        }

        public boolean isFaceNotDetected() {
            return this.acquiredInfo == 11;
        }
    }

    /* loaded from: classes.dex */
    public static class AuthenticationResultInfo {
        public boolean cmdEffective;
        public int faceId;
        public boolean received;
        public boolean succeed;
        public int userId;

        public void setInfo(int faceId, int userId) {
            this.faceId = faceId;
            this.userId = userId;
        }
    }

    /* loaded from: classes.dex */
    public static class ErrorInfo {
        public boolean cmdEffective;
        public int error;
        public boolean received;
        public int vendorCode;

        public void setInfo(int error, int vendorCode) {
            this.error = error;
            this.vendorCode = vendorCode;
        }
    }

    /* loaded from: classes.dex */
    public static class AlgorithmResultInfo {
        public String bundle;
        public boolean cmdEffective;
        public int command;
        public int extras;
        public boolean received;
        public int result;

        public void setInfo(int command, int result, int extras, String bundle) {
            this.command = command;
            this.result = result;
            this.extras = extras;
            this.bundle = bundle;
        }
    }

    /* loaded from: classes.dex */
    public static class CommandInfo {
        public String bundle;
        public boolean cmdEffective;
        public int command;
        public int extra;
        public boolean received;

        public void setInfo(int command, int extra, String bundle) {
            this.command = command;
            this.extra = extra;
            this.bundle = bundle;
        }
    }
}