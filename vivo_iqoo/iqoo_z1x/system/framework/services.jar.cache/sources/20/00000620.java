package com.android.server.accessibility.magnification;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.view.accessibility.IWindowMagnificationConnection;
import android.view.accessibility.IWindowMagnificationConnectionCallback;

/* loaded from: classes.dex */
public final class WindowMagnificationManager {
    private static final String TAG = "WindowMagnificationMgr";
    private ConnectionCallback mConnectionCallback;
    WindowMagnificationConnectionWrapper mConnectionWrapper;
    private final Object mLock = new Object();

    public void setConnection(IWindowMagnificationConnection connection) {
        synchronized (this.mLock) {
            if (this.mConnectionWrapper != null) {
                this.mConnectionWrapper.setConnectionCallback(null);
                if (this.mConnectionCallback != null) {
                    this.mConnectionCallback.mExpiredDeathRecipient = true;
                }
                this.mConnectionWrapper.unlinkToDeath(this.mConnectionCallback);
                this.mConnectionWrapper = null;
            }
            if (connection != null) {
                this.mConnectionWrapper = new WindowMagnificationConnectionWrapper(connection);
            }
            if (this.mConnectionWrapper != null) {
                try {
                    ConnectionCallback connectionCallback = new ConnectionCallback();
                    this.mConnectionCallback = connectionCallback;
                    this.mConnectionWrapper.linkToDeath(connectionCallback);
                    this.mConnectionWrapper.setConnectionCallback(this.mConnectionCallback);
                } catch (RemoteException e) {
                    Slog.e(TAG, "setConnection failed", e);
                    this.mConnectionWrapper = null;
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private class ConnectionCallback extends IWindowMagnificationConnectionCallback.Stub implements IBinder.DeathRecipient {
        private boolean mExpiredDeathRecipient;

        private ConnectionCallback() {
            this.mExpiredDeathRecipient = false;
        }

        public void onWindowMagnifierBoundsChanged(int display, Rect frame) throws RemoteException {
        }

        public void onChangeMagnificationMode(int display, int magnificationMode) throws RemoteException {
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (WindowMagnificationManager.this.mLock) {
                if (this.mExpiredDeathRecipient) {
                    Slog.w(WindowMagnificationManager.TAG, "binderDied DeathRecipient is expired");
                    return;
                }
                WindowMagnificationManager.this.mConnectionWrapper.unlinkToDeath(this);
                WindowMagnificationManager.this.mConnectionWrapper = null;
                WindowMagnificationManager.this.mConnectionCallback = null;
            }
        }
    }
}