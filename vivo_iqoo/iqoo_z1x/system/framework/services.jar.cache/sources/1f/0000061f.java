package com.android.server.accessibility.magnification;

import android.os.IBinder;
import android.os.RemoteException;
import android.view.accessibility.IWindowMagnificationConnection;
import android.view.accessibility.IWindowMagnificationConnectionCallback;

/* loaded from: classes.dex */
class WindowMagnificationConnectionWrapper {
    private static final boolean DBG = false;
    private static final String TAG = "WindowMagnificationConnectionWrapper";
    private final IWindowMagnificationConnection mConnection;

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowMagnificationConnectionWrapper(IWindowMagnificationConnection connection) {
        this.mConnection = connection;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unlinkToDeath(IBinder.DeathRecipient deathRecipient) {
        this.mConnection.asBinder().unlinkToDeath(deathRecipient, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void linkToDeath(IBinder.DeathRecipient deathRecipient) throws RemoteException {
        this.mConnection.asBinder().linkToDeath(deathRecipient, 0);
    }

    boolean enableWindowMagnification(int displayId, float scale, float centerX, float centerY) {
        try {
            this.mConnection.enableWindowMagnification(displayId, scale, centerX, centerY);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    boolean setScale(int displayId, float scale) {
        try {
            this.mConnection.setScale(displayId, scale);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    boolean disableWindowMagnification(int displayId) {
        try {
            this.mConnection.disableWindowMagnification(displayId);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    boolean moveWindowMagnifier(int displayId, float offsetX, float offsetY) {
        try {
            this.mConnection.moveWindowMagnifier(displayId, offsetX, offsetY);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setConnectionCallback(IWindowMagnificationConnectionCallback connectionCallback) {
        try {
            this.mConnection.setConnectionCallback(connectionCallback);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }
}