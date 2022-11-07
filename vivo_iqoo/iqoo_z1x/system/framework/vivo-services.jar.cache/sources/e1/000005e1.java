package com.vivo.face.internal.keyguard;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface IKeyguardCallback extends IInterface {
    void blockScreenOn() throws RemoteException;

    void hideKeyguard() throws RemoteException;

    void sendMessageToKeyguard(String str, int i) throws RemoteException;

    void showKeyguard() throws RemoteException;

    void showStatusBar() throws RemoteException;

    void unblockScreenOn() throws RemoteException;

    void unlockKeyguard(boolean z) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IKeyguardCallback {
        @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
        public void hideKeyguard() throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
        public void showKeyguard() throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
        public void unlockKeyguard(boolean isNativeUnlock) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
        public void sendMessageToKeyguard(String message, int extra) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
        public void blockScreenOn() throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
        public void unblockScreenOn() throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
        public void showStatusBar() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IKeyguardCallback {
        private static final String DESCRIPTOR = "com.vivo.face.internal.keyguard.IKeyguardCallback";
        static final int TRANSACTION_blockScreenOn = 5;
        static final int TRANSACTION_hideKeyguard = 1;
        static final int TRANSACTION_sendMessageToKeyguard = 4;
        static final int TRANSACTION_showKeyguard = 2;
        static final int TRANSACTION_showStatusBar = 7;
        static final int TRANSACTION_unblockScreenOn = 6;
        static final int TRANSACTION_unlockKeyguard = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IKeyguardCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IKeyguardCallback)) {
                return (IKeyguardCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 1598968902) {
                reply.writeString(DESCRIPTOR);
                return true;
            }
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    hideKeyguard();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    showKeyguard();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg0 = data.readInt() != 0;
                    unlockKeyguard(_arg0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    int _arg1 = data.readInt();
                    sendMessageToKeyguard(_arg02, _arg1);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    blockScreenOn();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    unblockScreenOn();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    showStatusBar();
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IKeyguardCallback {
            public static IKeyguardCallback sDefaultImpl;
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

            @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
            public void hideKeyguard() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().hideKeyguard();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
            public void showKeyguard() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().showKeyguard();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
            public void unlockKeyguard(boolean isNativeUnlock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(isNativeUnlock ? 1 : 0);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().unlockKeyguard(isNativeUnlock);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
            public void sendMessageToKeyguard(String message, int extra) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(message);
                    _data.writeInt(extra);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().sendMessageToKeyguard(message, extra);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
            public void blockScreenOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().blockScreenOn();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
            public void unblockScreenOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().unblockScreenOn();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IKeyguardCallback
            public void showStatusBar() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().showStatusBar();
                    }
                } finally {
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IKeyguardCallback impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IKeyguardCallback getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}