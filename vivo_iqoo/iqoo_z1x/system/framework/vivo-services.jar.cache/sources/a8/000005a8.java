package com.samsung.slsi.telephony.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.samsung.slsi.telephony.aidl.ISarServiceCallback;

/* loaded from: classes.dex */
public interface ISarService extends IInterface {
    void getState(int i) throws RemoteException;

    void registerCallback(ISarServiceCallback iSarServiceCallback) throws RemoteException;

    void setState(int i, int i2) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ISarService {
        @Override // com.samsung.slsi.telephony.aidl.ISarService
        public void registerCallback(ISarServiceCallback cb) throws RemoteException {
        }

        @Override // com.samsung.slsi.telephony.aidl.ISarService
        public void setState(int state, int phoneId) throws RemoteException {
        }

        @Override // com.samsung.slsi.telephony.aidl.ISarService
        public void getState(int phoneId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ISarService {
        private static final String DESCRIPTOR = "com.samsung.slsi.telephony.aidl.ISarService";
        static final int TRANSACTION_getState = 3;
        static final int TRANSACTION_registerCallback = 1;
        static final int TRANSACTION_setState = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISarService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISarService)) {
                return (ISarService) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 1) {
                data.enforceInterface(DESCRIPTOR);
                ISarServiceCallback _arg0 = ISarServiceCallback.Stub.asInterface(data.readStrongBinder());
                registerCallback(_arg0);
                return true;
            } else if (code == 2) {
                data.enforceInterface(DESCRIPTOR);
                int _arg02 = data.readInt();
                int _arg1 = data.readInt();
                setState(_arg02, _arg1);
                return true;
            } else if (code != 3) {
                if (code == 1598968902) {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(code, data, reply, flags);
            } else {
                data.enforceInterface(DESCRIPTOR);
                int _arg03 = data.readInt();
                getState(_arg03);
                return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements ISarService {
            public static ISarService sDefaultImpl;
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

            @Override // com.samsung.slsi.telephony.aidl.ISarService
            public void registerCallback(ISarServiceCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().registerCallback(cb);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.samsung.slsi.telephony.aidl.ISarService
            public void setState(int state, int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeInt(phoneId);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setState(state, phoneId);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.samsung.slsi.telephony.aidl.ISarService
            public void getState(int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().getState(phoneId);
                    }
                } finally {
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(ISarService impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static ISarService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}