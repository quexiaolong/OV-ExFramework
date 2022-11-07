package com.vivo.services.proxy.transact;

import android.os.IBinder;
import android.os.IMessenger;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class IMessengerProxy extends BpTransactProxy implements IMessenger {
    private static final String DESCRIPTOR = "android.os.IMessenger";

    /* JADX INFO: Access modifiers changed from: package-private */
    public IMessengerProxy(String descriptor, IBinder bpBinder, String pkg, String process, int uid, int pid) {
        super(descriptor, bpBinder, pkg, process, uid, pid);
    }

    @Override // com.vivo.services.proxy.transact.BpTransactProxy
    protected Transaction onTransactionCreated(int code, Parcel data) {
        return new MyTransaction(code, data);
    }

    /* loaded from: classes.dex */
    private static class MyTransaction extends Transaction {
        int mWhat;

        MyTransaction(int code, Parcel data) {
            super(code, data);
            data.setDataPosition(0);
            data.enforceInterface(IMessengerProxy.DESCRIPTOR);
            if (data.readInt() != 0) {
                Message message = (Message) Message.CREATOR.createFromParcel(data);
                this.mWhat = message.what;
                return;
            }
            this.mWhat = -1;
        }

        @Override // com.vivo.services.proxy.transact.Transaction
        public boolean isEqual(Transaction t) {
            MyTransaction rt = (MyTransaction) t;
            return this.mCode == rt.mCode && this.mWhat == rt.mWhat;
        }

        @Override // com.vivo.services.proxy.transact.Transaction
        public String toString() {
            return String.format("code=%d what=%d", Integer.valueOf(this.mCode), Integer.valueOf(this.mWhat));
        }
    }

    public IBinder asBinder() {
        return null;
    }

    public void send(Message msg) throws RemoteException {
    }
}