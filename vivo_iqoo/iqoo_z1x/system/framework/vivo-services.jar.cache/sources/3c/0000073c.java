package com.vivo.services.rms.sdk;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.vivo.services.rms.sdk.args.Args;
import java.io.FileDescriptor;

/* loaded from: classes.dex */
public class IEventCallbackProxy implements IEventCallback {
    private IBinder mRemote;

    public IEventCallbackProxy(IBinder remote) {
        this.mRemote = remote;
    }

    @Override // android.os.IInterface
    public IBinder asBinder() {
        return this.mRemote;
    }

    public String getInterfaceDescriptor() {
        return IEventCallback.DESCRIPTOR;
    }

    @Override // com.vivo.services.rms.sdk.IEventCallback
    public void onProcessEvent(int event, Args args) throws RemoteException {
        Parcel data = Parcel.obtain();
        try {
            data.writeInt(event);
            args.writeToParcel(data, 0);
            this.mRemote.transact(1, data, null, 0);
        } finally {
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IEventCallback
    public void onSystemEvent(int event, Args args) throws RemoteException {
        Parcel data = Parcel.obtain();
        try {
            data.writeInt(event);
            args.writeToParcel(data, 0);
            this.mRemote.transact(2, data, null, 0);
        } finally {
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IEventCallback
    public void dumpData(FileDescriptor fd, String[] args) throws RemoteException {
        Parcel data = Parcel.obtain();
        try {
            data.writeFileDescriptor(fd);
            data.writeStringArray(args);
            this.mRemote.transact(3, data, null, 0);
        } finally {
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IEventCallback
    public int myPid() throws RemoteException {
        Parcel reply = Parcel.obtain();
        Parcel data = Parcel.obtain();
        try {
            this.mRemote.transact(4, data, reply, 0);
            int pid = reply.readInt();
            return pid;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IEventCallback
    public void doInit(Bundle bundle) throws RemoteException {
        Parcel data = Parcel.obtain();
        try {
            bundle.writeToParcel(data, 0);
            this.mRemote.transact(5, data, null, 0);
        } finally {
            data.recycle();
        }
    }
}