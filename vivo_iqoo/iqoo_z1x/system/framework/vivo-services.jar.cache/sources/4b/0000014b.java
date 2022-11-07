package com.android.server.biometrics.face;

import android.content.Context;
import android.hardware.biometrics.fingerprint.FingerprintUIManagerInternal;
import android.os.IBinder;
import com.android.server.LocalServices;
import com.vivo.face.common.memory.FaceSharedMemory;
import com.vivo.face.common.wake.FaceWakeController;
import java.io.FileDescriptor;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public abstract class AbsFaceUIManagerServiceAdapter {
    protected static final int CMD_HIDL_WRITE_DATA = 1002;
    protected static final String TAG = "FaceUIManagerServiceAdapter";
    protected Context mContext;
    private FaceWakeController mFaceWakeController;
    protected FileDescriptor mFileDescriptor;
    private FingerprintUIManagerInternal mFingerprintUIManager;
    private boolean mLockout;
    private boolean mLockoutReady;
    private int mMemorySize;
    private FaceSharedMemory mSharedMemory;

    public abstract IBinder asBinder();

    public AbsFaceUIManagerServiceAdapter(Context context) {
        this.mContext = context;
    }

    private FingerprintUIManagerInternal checkFingerprintUIManager() {
        if (this.mFingerprintUIManager == null) {
            this.mFingerprintUIManager = (FingerprintUIManagerInternal) LocalServices.getService(FingerprintUIManagerInternal.class);
        }
        return this.mFingerprintUIManager;
    }

    public void sendMessageToFingerprint(int msg, String extra, byte[] array) {
        FingerprintUIManagerInternal mFingerprintUIManager = checkFingerprintUIManager();
        mFingerprintUIManager.onFaceAuthenticated(msg);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setSharedMemorySize(int size) {
        this.mMemorySize = size;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getSharedMemorySize() {
        return this.mMemorySize;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public FileDescriptor generateFileDescriptor(int memorySize) {
        VSlog.i(TAG, "generate memory FileDescriptor " + memorySize);
        if (this.mSharedMemory == null) {
            this.mSharedMemory = new FaceSharedMemory(FaceSharedMemory.SHARED_MEMORY_NAME, memorySize);
        }
        FaceSharedMemory faceSharedMemory = this.mSharedMemory;
        if (faceSharedMemory != null) {
            this.mFileDescriptor = faceSharedMemory.getFileDescriptor();
        }
        return this.mFileDescriptor;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void sendLockoutState(boolean lockoutReady) {
        this.mLockoutReady = lockoutReady;
        if (!lockoutReady) {
            setLockout(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean getLockoutReady() {
        return this.mLockoutReady;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setLockout(boolean lockout) {
        this.mLockout = lockout;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean getLockout() {
        return this.mLockout;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void sendAuthenticationResult(boolean succeed) {
        if (this.mFaceWakeController == null) {
            this.mFaceWakeController = FaceWakeController.getInstance();
        }
        FaceWakeController faceWakeController = this.mFaceWakeController;
        if (faceWakeController != null) {
            faceWakeController.onAuthenticationResult(succeed);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void sendDialogVisibleState(boolean visible) {
        if (this.mFaceWakeController == null) {
            this.mFaceWakeController = FaceWakeController.getInstance();
        }
        FaceWakeController faceWakeController = this.mFaceWakeController;
        if (faceWakeController != null) {
            faceWakeController.onDialogVisibleStateChanged(visible);
        }
    }
}