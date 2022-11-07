package com.vivo.services.proxy.transact;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
class Transaction {
    int mCode;
    Parcel mData;

    /* JADX INFO: Access modifiers changed from: package-private */
    public Transaction(int code, Parcel data) {
        this.mCode = code;
        this.mData = data;
        data.setRecycleable(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void send2Target(IBinder binder) throws RemoteException {
        Parcel parcel;
        if (binder != null && (parcel = this.mData) != null) {
            binder.transact(this.mCode, parcel, null, 1);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void recycleData() {
        Parcel parcel = this.mData;
        if (parcel != null) {
            parcel.setRecycleable(true);
            this.mCode = -1;
            this.mData = null;
        }
    }

    public boolean isEqual(Transaction t) {
        return this.mCode == t.mCode;
    }

    public String toString() {
        return "code=" + this.mCode;
    }
}