package com.android.server.accessibility;

import android.content.res.Resources;
import android.hardware.fingerprint.IFingerprintClientActiveCallback;
import android.hardware.fingerprint.IFingerprintService;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class FingerprintGestureDispatcher extends IFingerprintClientActiveCallback.Stub implements Handler.Callback {
    private static final String LOG_TAG = "FingerprintGestureDispatcher";
    private static final int MSG_REGISTER = 1;
    private static final int MSG_UNREGISTER = 2;
    private final List<FingerprintGestureClient> mCapturingClients;
    private final IFingerprintService mFingerprintService;
    private final Handler mHandler;
    private final boolean mHardwareSupportsGestures;
    private final Object mLock;
    private boolean mRegisteredReadOnlyExceptInHandler;

    /* loaded from: classes.dex */
    public interface FingerprintGestureClient {
        boolean isCapturingFingerprintGestures();

        void onFingerprintGesture(int i);

        void onFingerprintGestureDetectionActiveChanged(boolean z);
    }

    public FingerprintGestureDispatcher(IFingerprintService fingerprintService, Resources resources, Object lock) {
        this.mCapturingClients = new ArrayList(0);
        this.mFingerprintService = fingerprintService;
        this.mHardwareSupportsGestures = resources.getBoolean(17891461);
        this.mLock = lock;
        this.mHandler = new Handler(this);
    }

    public FingerprintGestureDispatcher(IFingerprintService fingerprintService, Resources resources, Object lock, Handler handler) {
        this.mCapturingClients = new ArrayList(0);
        this.mFingerprintService = fingerprintService;
        this.mHardwareSupportsGestures = resources.getBoolean(17891461);
        this.mLock = lock;
        this.mHandler = handler;
    }

    public void updateClientList(List<? extends FingerprintGestureClient> clientList) {
        if (this.mHardwareSupportsGestures) {
            synchronized (this.mLock) {
                this.mCapturingClients.clear();
                for (int i = 0; i < clientList.size(); i++) {
                    FingerprintGestureClient client = clientList.get(i);
                    if (client.isCapturingFingerprintGestures()) {
                        this.mCapturingClients.add(client);
                    }
                }
                if (this.mCapturingClients.isEmpty()) {
                    if (this.mRegisteredReadOnlyExceptInHandler) {
                        this.mHandler.obtainMessage(2).sendToTarget();
                    }
                } else if (!this.mRegisteredReadOnlyExceptInHandler) {
                    this.mHandler.obtainMessage(1).sendToTarget();
                }
            }
        }
    }

    public void onClientActiveChanged(boolean nonGestureFingerprintClientActive) {
        if (this.mHardwareSupportsGestures) {
            synchronized (this.mLock) {
                for (int i = 0; i < this.mCapturingClients.size(); i++) {
                    this.mCapturingClients.get(i).onFingerprintGestureDetectionActiveChanged(!nonGestureFingerprintClientActive);
                }
            }
        }
    }

    public boolean isFingerprintGestureDetectionAvailable() {
        if (this.mHardwareSupportsGestures) {
            long identity = Binder.clearCallingIdentity();
            try {
                return !this.mFingerprintService.isClientActive();
            } catch (RemoteException e) {
                return false;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        return false;
    }

    public boolean onFingerprintGesture(int fingerprintKeyCode) {
        int idForFingerprintGestureManager;
        synchronized (this.mLock) {
            if (this.mCapturingClients.isEmpty()) {
                return false;
            }
            switch (fingerprintKeyCode) {
                case 280:
                    idForFingerprintGestureManager = 4;
                    break;
                case 281:
                    idForFingerprintGestureManager = 8;
                    break;
                case 282:
                    idForFingerprintGestureManager = 2;
                    break;
                case 283:
                    idForFingerprintGestureManager = 1;
                    break;
                default:
                    return false;
            }
            List<FingerprintGestureClient> clientList = new ArrayList<>(this.mCapturingClients);
            for (int i = 0; i < clientList.size(); i++) {
                clientList.get(i).onFingerprintGesture(idForFingerprintGestureManager);
            }
            return true;
        }
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message message) {
        long identity;
        if (message.what == 1) {
            identity = Binder.clearCallingIdentity();
            try {
                try {
                    this.mFingerprintService.addClientActiveCallback(this);
                    this.mRegisteredReadOnlyExceptInHandler = true;
                } catch (RemoteException e) {
                    Slog.e(LOG_TAG, "Failed to register for fingerprint activity callbacks");
                }
                return false;
            } finally {
            }
        } else if (message.what == 2) {
            identity = Binder.clearCallingIdentity();
            try {
                try {
                    this.mFingerprintService.removeClientActiveCallback(this);
                } finally {
                }
            } catch (RemoteException e2) {
                Slog.e(LOG_TAG, "Failed to unregister for fingerprint activity callbacks");
            }
            this.mRegisteredReadOnlyExceptInHandler = false;
            return true;
        } else {
            Slog.e(LOG_TAG, "Unknown message: " + message.what);
            return false;
        }
    }
}