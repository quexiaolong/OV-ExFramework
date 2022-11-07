package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface INetworkStackStatusCallback extends IInterface {
    public static final String HASH = "02cd6fd07d5c04eca0c35a350f7b0be576242883";
    public static final int VERSION = 7;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void onStatusAvailable(int i) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements INetworkStackStatusCallback {
        @Override // android.net.INetworkStackStatusCallback
        public void onStatusAvailable(int statusCode) throws RemoteException {
        }

        @Override // android.net.INetworkStackStatusCallback
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.INetworkStackStatusCallback
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements INetworkStackStatusCallback {
        private static final String DESCRIPTOR = "android$net$INetworkStackStatusCallback".replace('$', '.');
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_onStatusAvailable = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkStackStatusCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INetworkStackStatusCallback)) {
                return (INetworkStackStatusCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code == 1) {
                data.enforceInterface(descriptor);
                int _arg0 = data.readInt();
                onStatusAvailable(_arg0);
                return true;
            } else if (code == 1598968902) {
                reply.writeString(descriptor);
                return true;
            } else {
                switch (code) {
                    case TRANSACTION_getInterfaceHash /* 16777214 */:
                        data.enforceInterface(descriptor);
                        reply.writeNoException();
                        reply.writeString(getInterfaceHash());
                        return true;
                    case 16777215:
                        data.enforceInterface(descriptor);
                        reply.writeNoException();
                        reply.writeInt(getInterfaceVersion());
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements INetworkStackStatusCallback {
            public static INetworkStackStatusCallback sDefaultImpl;
            private IBinder mRemote;
            private int mCachedVersion = -1;
            private String mCachedHash = "-1";

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // android.net.INetworkStackStatusCallback
            public void onStatusAvailable(int statusCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(statusCode);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onStatusAvailable(statusCode);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkStackStatusCallback
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(Stub.DESCRIPTOR);
                        boolean _status = this.mRemote.transact(16777215, data, reply, 0);
                        if (!_status && Stub.getDefaultImpl() != null) {
                            return Stub.getDefaultImpl().getInterfaceVersion();
                        }
                        reply.readException();
                        this.mCachedVersion = reply.readInt();
                    } finally {
                        reply.recycle();
                        data.recycle();
                    }
                }
                return this.mCachedVersion;
            }

            @Override // android.net.INetworkStackStatusCallback
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        String interfaceHash = Stub.getDefaultImpl().getInterfaceHash();
                        reply.recycle();
                        data.recycle();
                        return interfaceHash;
                    }
                    reply.readException();
                    this.mCachedHash = reply.readString();
                    reply.recycle();
                    data.recycle();
                }
                return this.mCachedHash;
            }
        }

        public static boolean setDefaultImpl(INetworkStackStatusCallback impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static INetworkStackStatusCallback getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}