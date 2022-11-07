package com.android.internal.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface IOemNetdUnsolicitedEventListener extends IInterface {
    void onRegistered() throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IOemNetdUnsolicitedEventListener {
        @Override // com.android.internal.net.IOemNetdUnsolicitedEventListener
        public void onRegistered() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IOemNetdUnsolicitedEventListener {
        private static final String DESCRIPTOR = "com$android$internal$net$IOemNetdUnsolicitedEventListener".replace('$', '.');
        static final int TRANSACTION_onRegistered = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IOemNetdUnsolicitedEventListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IOemNetdUnsolicitedEventListener)) {
                return (IOemNetdUnsolicitedEventListener) iin;
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
                onRegistered();
                return true;
            } else if (code == 1598968902) {
                reply.writeString(descriptor);
                return true;
            } else {
                return super.onTransact(code, data, reply, flags);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IOemNetdUnsolicitedEventListener {
            public static IOemNetdUnsolicitedEventListener sDefaultImpl;
            private IBinder mRemote;

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

            @Override // com.android.internal.net.IOemNetdUnsolicitedEventListener
            public void onRegistered() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onRegistered();
                    }
                } finally {
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IOemNetdUnsolicitedEventListener impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IOemNetdUnsolicitedEventListener getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}