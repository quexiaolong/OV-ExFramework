package com.vivo.services.rms.sdk;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import com.vivo.services.rms.sdk.args.Args;
import java.io.IOException;

/* loaded from: classes.dex */
public abstract class IEventCallbackNative extends Binder implements IEventCallback {
    public IEventCallbackNative() {
        attachInterface(this, IEventCallback.DESCRIPTOR);
    }

    public static IEventCallback asInterface(IBinder obj) {
        if (obj == null) {
            return null;
        }
        IEventCallback in = (IEventCallback) obj.queryLocalInterface(IEventCallback.DESCRIPTOR);
        if (in != null) {
            return in;
        }
        return new IEventCallbackProxy(obj);
    }

    @Override // android.os.IInterface
    public IBinder asBinder() {
        return this;
    }

    @Override // android.os.Binder
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == 1) {
            int event = data.readInt();
            Args args = Args.CREATOR.createFromParcel(data);
            onProcessEvent(event, args);
            reply.writeNoException();
            return true;
        } else if (code == 2) {
            int event2 = data.readInt();
            Args args2 = Args.CREATOR.createFromParcel(data);
            onSystemEvent(event2, args2);
            reply.writeNoException();
            return true;
        } else if (code == 3) {
            ParcelFileDescriptor fd = data.readFileDescriptor();
            String[] args3 = data.createStringArray();
            if (fd != null) {
                try {
                    dumpData(fd.getFileDescriptor(), args3);
                } finally {
                    try {
                        fd.close();
                    } catch (IOException e) {
                    }
                }
            }
            reply.writeNoException();
            return true;
        } else if (code == 4) {
            reply.writeInt(myPid());
            return true;
        } else {
            if (code == 5) {
                Bundle bundle = new Bundle();
                bundle.readFromParcel(data);
                doInit(bundle);
                reply.writeNoException();
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    @Override // com.vivo.services.rms.sdk.IEventCallback
    public int myPid() throws RemoteException {
        return Process.myPid();
    }
}