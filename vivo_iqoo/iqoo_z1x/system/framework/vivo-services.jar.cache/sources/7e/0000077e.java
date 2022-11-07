package com.vivo.services.rms.sp.sdk;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import java.io.IOException;

/* loaded from: classes.dex */
public abstract class SpClientStub extends Binder implements ISpClient {
    public SpClientStub() {
        attachInterface(this, ISpClient.DESCRIPTOR);
    }

    public static ISpClient asInterface(IBinder obj) {
        if (obj == null) {
            return null;
        }
        ISpClient in = (ISpClient) obj.queryLocalInterface(ISpClient.DESCRIPTOR);
        if (in != null) {
            return in;
        }
        return new SpClientProxy(obj);
    }

    @Override // android.os.IInterface
    public IBinder asBinder() {
        return this;
    }

    @Override // android.os.Binder
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        switch (code) {
            case 1:
                ParcelFileDescriptor fd = data.readFileDescriptor();
                String[] args = data.createStringArray();
                if (fd != null) {
                    try {
                        dumpData(fd.getFileDescriptor(), args);
                        try {
                            fd.close();
                        } catch (IOException e) {
                        }
                    } catch (Throwable th) {
                        try {
                            fd.close();
                        } catch (IOException e2) {
                        }
                        throw th;
                    }
                }
                reply.writeNoException();
                return true;
            case 2:
                reply.writeInt(myPid());
                reply.writeNoException();
                return true;
            case 3:
                Bundle bundle = new Bundle();
                bundle.readFromParcel(data);
                doInit(bundle);
                reply.writeNoException();
                return true;
            case 4:
                String name = ParcelUtils.readString(data);
                Bundle bundle2 = new Bundle();
                bundle2.readFromParcel(data);
                boolean bundle3 = setBundle(name, bundle2);
                reply.writeNoException();
                reply.writeInt(bundle3 ? 1 : 0);
                return true;
            case 5:
                String name2 = ParcelUtils.readString(data);
                Bundle bundle4 = getBundle(name2);
                if (bundle4 == null) {
                    bundle4 = new Bundle();
                }
                reply.writeNoException();
                bundle4.writeToParcel(reply, 0);
                return true;
            case 6:
                String pkgName = ParcelUtils.readString(data);
                int uid = data.readInt();
                long versionCode = data.readLong();
                int flag = data.readInt();
                notifyErrorPackage(pkgName, uid, versionCode, flag);
                reply.writeNoException();
                return true;
            default:
                return super.onTransact(code, data, reply, flags);
        }
    }

    @Override // com.vivo.services.rms.sp.sdk.ISpClient
    public int myPid() throws RemoteException {
        return Process.myPid();
    }
}