package com.android.server.backup.remote;

import android.app.backup.IBackupCallback;
import android.os.RemoteException;
import java.util.concurrent.CompletableFuture;

/* loaded from: classes.dex */
public class FutureBackupCallback extends IBackupCallback.Stub {
    private final CompletableFuture<RemoteResult> mFuture;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FutureBackupCallback(CompletableFuture<RemoteResult> future) {
        this.mFuture = future;
    }

    public void operationComplete(long result) throws RemoteException {
        this.mFuture.complete(RemoteResult.of(result));
    }
}