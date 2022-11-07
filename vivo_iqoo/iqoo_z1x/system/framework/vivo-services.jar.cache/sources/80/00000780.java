package com.vivo.services.rms.sp.sdk;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public abstract class SpServerStub extends Binder implements ISpServer {
    public static final String SERVICE_NAME = "sps";
    public static final String VERSION = "1.0";

    public SpServerStub() {
        attachInterface(this, ISpServer.DESCRIPTOR);
    }

    public static ISpServer asInterface(IBinder obj) {
        if (obj == null) {
            return null;
        }
        ISpServer in = (ISpServer) obj.queryLocalInterface(ISpServer.DESCRIPTOR);
        if (in != null) {
            return in;
        }
        return new SpServerProxy(obj);
    }

    @Override // android.os.IInterface
    public IBinder asBinder() {
        return this;
    }

    @Override // android.os.Binder
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == 1) {
            String name = ParcelUtils.readString(data);
            Bundle bundle = new Bundle();
            bundle.readFromParcel(data);
            boolean bundle2 = setBundle(name, bundle);
            reply.writeNoException();
            reply.writeInt(bundle2 ? 1 : 0);
            return true;
        } else if (code == 2) {
            String name2 = ParcelUtils.readString(data);
            Bundle bundle3 = getBundle(name2);
            if (bundle3 == null) {
                bundle3 = new Bundle();
            }
            reply.writeNoException();
            bundle3.writeToParcel(reply, 0);
            return true;
        } else if (code == 3) {
            String pkgName = ParcelUtils.readString(data);
            int uid = data.readInt();
            long versionCode = data.readLong();
            int flag = data.readInt();
            reportErrorPackage(pkgName, uid, versionCode, flag);
            reply.writeNoException();
            return true;
        } else {
            return super.onTransact(code, data, reply, flags);
        }
    }
}