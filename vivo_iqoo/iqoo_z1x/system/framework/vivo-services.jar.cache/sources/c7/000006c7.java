package com.vivo.services.proxy.transact;

import android.hardware.display.IDisplayManagerCallback;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.vivo.common.utils.VLog;
import java.lang.reflect.Field;
import java.util.Iterator;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class IDisplayManagerCallbackProxy extends BpTransactProxy implements IDisplayManagerCallback {
    private static final String DESCRIPTOR = "android.hardware.display.IDisplayManagerCallback";
    private static int TRANSACTION_onDisplayEvent;

    static {
        TRANSACTION_onDisplayEvent = -1;
        try {
            Field f = IDisplayManagerCallback.Stub.class.getDeclaredField("TRANSACTION_onDisplayEvent");
            f.setAccessible(true);
            TRANSACTION_onDisplayEvent = f.getInt(null);
        } catch (Exception e) {
            TransactProxyManager.CODE_READY = false;
            VLog.e(TransactProxyManager.TAG, "IDisplayManagerCallback init TRANSACTION_onDisplayEvent fail:" + e.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IDisplayManagerCallbackProxy(String descriptor, IBinder bpBinder, String pkg, String process, int uid, int pid) {
        super(descriptor, bpBinder, pkg, process, uid, pid);
    }

    @Override // com.vivo.services.proxy.transact.BpTransactProxy
    protected Transaction onTransactionCreated(int code, Parcel data) {
        return new MyTransaction(code, data);
    }

    @Override // com.vivo.services.proxy.transact.BpTransactProxy
    protected boolean onTansactionPreAdded(Transaction t) {
        MyTransaction tt = (MyTransaction) t;
        if (tt.mCode == TRANSACTION_onDisplayEvent && tt.mEvent == 3 && removeByDislayId(tt.mDisplayId)) {
            return false;
        }
        return true;
    }

    private boolean removeByDislayId(int displayId) {
        boolean removeCurrent = false;
        synchronized (this) {
            Iterator<Transaction> it = this.mTransactions.iterator();
            while (it.hasNext()) {
                MyTransaction tt = (MyTransaction) it.next();
                if (tt.mDisplayId == displayId) {
                    if (tt.mEvent == 1) {
                        removeCurrent = true;
                    }
                    it.remove();
                    tt.recycleData();
                }
            }
        }
        return removeCurrent;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MyTransaction extends Transaction {
        int mDisplayId;
        int mEvent;

        MyTransaction(int code, Parcel data) {
            super(code, data);
            data.setDataPosition(0);
            data.enforceInterface(IDisplayManagerCallbackProxy.DESCRIPTOR);
            this.mDisplayId = data.readInt();
            this.mEvent = data.readInt();
        }

        @Override // com.vivo.services.proxy.transact.Transaction
        public boolean isEqual(Transaction t) {
            MyTransaction other = (MyTransaction) t;
            return this.mCode == other.mCode && this.mDisplayId == other.mDisplayId && this.mEvent == other.mEvent;
        }

        @Override // com.vivo.services.proxy.transact.Transaction
        public String toString() {
            return String.format("code=%d displayId=%d event=%d", Integer.valueOf(this.mCode), Integer.valueOf(this.mDisplayId), Integer.valueOf(this.mEvent));
        }
    }

    public IBinder asBinder() {
        return null;
    }

    public void onDisplayEvent(int displayId, int event) throws RemoteException {
    }
}