package com.iqoo.engineermode;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.iqoo.engineermode.PhoneInterfaceCallBack;

/* loaded from: classes.dex */
public interface PhoneInterface extends IInterface {
    void registerListener(PhoneInterfaceCallBack phoneInterfaceCallBack) throws RemoteException;

    String sendATCommand(String str, int i) throws RemoteException;

    void unregisterListener(PhoneInterfaceCallBack phoneInterfaceCallBack) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements PhoneInterface {
        @Override // com.iqoo.engineermode.PhoneInterface
        public void registerListener(PhoneInterfaceCallBack callBack) throws RemoteException {
        }

        @Override // com.iqoo.engineermode.PhoneInterface
        public void unregisterListener(PhoneInterfaceCallBack callBack) throws RemoteException {
        }

        @Override // com.iqoo.engineermode.PhoneInterface
        public String sendATCommand(String atCommand, int phone) throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements PhoneInterface {
        private static final String DESCRIPTOR = "com.iqoo.engineermode.PhoneInterface";
        static final int TRANSACTION_registerListener = 1;
        static final int TRANSACTION_sendATCommand = 3;
        static final int TRANSACTION_unregisterListener = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static PhoneInterface asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof PhoneInterface)) {
                return (PhoneInterface) iin;
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
                PhoneInterfaceCallBack _arg0 = PhoneInterfaceCallBack.Stub.asInterface(data.readStrongBinder());
                registerListener(_arg0);
                reply.writeNoException();
                return true;
            } else if (code == 2) {
                data.enforceInterface(DESCRIPTOR);
                PhoneInterfaceCallBack _arg02 = PhoneInterfaceCallBack.Stub.asInterface(data.readStrongBinder());
                unregisterListener(_arg02);
                reply.writeNoException();
                return true;
            } else if (code != 3) {
                if (code == 1598968902) {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(code, data, reply, flags);
            } else {
                data.enforceInterface(DESCRIPTOR);
                String _arg03 = data.readString();
                int _arg1 = data.readInt();
                String _result = sendATCommand(_arg03, _arg1);
                reply.writeNoException();
                reply.writeString(_result);
                return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements PhoneInterface {
            public static PhoneInterface sDefaultImpl;
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

            @Override // com.iqoo.engineermode.PhoneInterface
            public void registerListener(PhoneInterfaceCallBack callBack) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callBack != null ? callBack.asBinder() : null);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().registerListener(callBack);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.iqoo.engineermode.PhoneInterface
            public void unregisterListener(PhoneInterfaceCallBack callBack) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callBack != null ? callBack.asBinder() : null);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().unregisterListener(callBack);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.iqoo.engineermode.PhoneInterface
            public String sendATCommand(String atCommand, int phone) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(atCommand);
                    _data.writeInt(phone);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().sendATCommand(atCommand, phone);
                    }
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(PhoneInterface impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static PhoneInterface getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}