package com.vivo.services.rms.sp.sdk;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public class SpServerProxy implements ISpServer {
    private IBinder mRemote;

    public SpServerProxy(IBinder remote) {
        this.mRemote = remote;
    }

    @Override // android.os.IInterface
    public IBinder asBinder() {
        return this.mRemote;
    }

    public String getInterfaceDescriptor() {
        return ISpServer.DESCRIPTOR;
    }

    @Override // com.vivo.services.rms.sp.sdk.ISpServer
    public boolean setBundle(String name, Bundle bundle) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        ParcelUtils.writeString(data, name);
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.writeToParcel(data, 0);
        try {
            this.mRemote.transact(1, data, reply, 0);
            reply.readException();
            boolean result = reply.readInt() == 1;
            return result;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sp.sdk.ISpServer
    public Bundle getBundle(String name) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        ParcelUtils.writeString(data, name);
        Bundle bundle = new Bundle();
        try {
            this.mRemote.transact(2, data, reply, 0);
            reply.readException();
            bundle.readFromParcel(reply);
            return bundle;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sp.sdk.ISpServer
    public void reportErrorPackage(String pkgName, int uid, long versionCode, int flag) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        ParcelUtils.writeString(data, pkgName);
        data.writeInt(uid);
        data.writeLong(versionCode);
        data.writeInt(flag);
        try {
            this.mRemote.transact(3, data, reply, 1);
            reply.readException();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }
}