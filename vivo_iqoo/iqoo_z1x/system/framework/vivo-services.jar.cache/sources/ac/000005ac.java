package com.samsung.slsi.telephony.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface ISarServiceCallback extends IInterface {
    void getSarStateRsp(int i, int i2, int i3) throws RemoteException;

    void notifyRfConnection(int i, int i2) throws RemoteException;

    void setSarStateRsp(int i, int i2) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ISarServiceCallback {
        @Override // com.samsung.slsi.telephony.aidl.ISarServiceCallback
        public void setSarStateRsp(int error, int phoneId) throws RemoteException {
        }

        @Override // com.samsung.slsi.telephony.aidl.ISarServiceCallback
        public void getSarStateRsp(int error, int state, int phoneId) throws RemoteException {
        }

        @Override // com.samsung.slsi.telephony.aidl.ISarServiceCallback
        public void notifyRfConnection(int rfstate, int phoneId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ISarServiceCallback {
        private static final String DESCRIPTOR = "com.samsung.slsi.telephony.aidl.ISarServiceCallback";
        static final int TRANSACTION_getSarStateRsp = 2;
        static final int TRANSACTION_notifyRfConnection = 3;
        static final int TRANSACTION_setSarStateRsp = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISarServiceCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISarServiceCallback)) {
                return (ISarServiceCallback) iin;
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
                int _arg0 = data.readInt();
                int _arg1 = data.readInt();
                setSarStateRsp(_arg0, _arg1);
                return true;
            } else if (code == 2) {
                data.enforceInterface(DESCRIPTOR);
                int _arg02 = data.readInt();
                int _arg12 = data.readInt();
                int _arg2 = data.readInt();
                getSarStateRsp(_arg02, _arg12, _arg2);
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
                int _arg13 = data.readInt();
                notifyRfConnection(_arg03, _arg13);
                return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements ISarServiceCallback {
            public static ISarServiceCallback sDefaultImpl;
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

            @Override // com.samsung.slsi.telephony.aidl.ISarServiceCallback
            public void setSarStateRsp(int error, int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(error);
                    _data.writeInt(phoneId);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setSarStateRsp(error, phoneId);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.samsung.slsi.telephony.aidl.ISarServiceCallback
            public void getSarStateRsp(int error, int state, int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(error);
                    _data.writeInt(state);
                    _data.writeInt(phoneId);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().getSarStateRsp(error, state, phoneId);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.samsung.slsi.telephony.aidl.ISarServiceCallback
            public void notifyRfConnection(int rfstate, int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(rfstate);
                    _data.writeInt(phoneId);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().notifyRfConnection(rfstate, phoneId);
                    }
                } finally {
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(ISarServiceCallback impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static ISarServiceCallback getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}