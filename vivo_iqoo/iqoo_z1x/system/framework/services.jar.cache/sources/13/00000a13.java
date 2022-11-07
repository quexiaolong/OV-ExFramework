package com.android.server.backup.restore;

import android.content.pm.IPackageDeleteObserver;
import android.os.RemoteException;
import java.util.concurrent.atomic.AtomicBoolean;

/* loaded from: classes.dex */
public class RestoreDeleteObserver extends IPackageDeleteObserver.Stub {
    private final AtomicBoolean mDone = new AtomicBoolean();

    public void reset() {
        synchronized (this.mDone) {
            this.mDone.set(false);
        }
    }

    public void waitForCompletion() {
        synchronized (this.mDone) {
            while (!this.mDone.get()) {
                try {
                    this.mDone.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void packageDeleted(String packageName, int returnCode) throws RemoteException {
        synchronized (this.mDone) {
            this.mDone.set(true);
            this.mDone.notifyAll();
        }
    }
}