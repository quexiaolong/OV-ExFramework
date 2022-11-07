package com.android.server.wm;

import android.app.IApplicationThread;
import android.app.servertransaction.ActivityLifecycleItem;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.ClientTransactionItem;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ClientLifecycleManager {
    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleTransaction(ClientTransaction transaction) throws RemoteException {
        IApplicationThread client = transaction.getClient();
        transaction.schedule();
        if (!(client instanceof Binder)) {
            transaction.recycle();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleTransaction(IApplicationThread client, IBinder activityToken, ActivityLifecycleItem stateRequest) throws RemoteException {
        ClientTransaction clientTransaction = transactionWithState(client, activityToken, stateRequest);
        scheduleTransaction(clientTransaction);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleTransaction(IApplicationThread client, IBinder activityToken, ClientTransactionItem callback) throws RemoteException {
        ClientTransaction clientTransaction = transactionWithCallback(client, activityToken, callback);
        scheduleTransaction(clientTransaction);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleTransaction(IApplicationThread client, ClientTransactionItem callback) throws RemoteException {
        ClientTransaction clientTransaction = transactionWithCallback(client, null, callback);
        scheduleTransaction(clientTransaction);
    }

    private static ClientTransaction transactionWithState(IApplicationThread client, IBinder activityToken, ActivityLifecycleItem stateRequest) {
        ClientTransaction clientTransaction = ClientTransaction.obtain(client, activityToken);
        clientTransaction.setLifecycleStateRequest(stateRequest);
        return clientTransaction;
    }

    private static ClientTransaction transactionWithCallback(IApplicationThread client, IBinder activityToken, ClientTransactionItem callback) {
        ClientTransaction clientTransaction = ClientTransaction.obtain(client, activityToken);
        clientTransaction.addCallback(callback);
        return clientTransaction;
    }
}