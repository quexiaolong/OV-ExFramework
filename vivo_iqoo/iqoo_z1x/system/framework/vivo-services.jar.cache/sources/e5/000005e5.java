package com.vivo.face.internal.ui;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface IFaceUI extends IInterface {
    void cancelAuthenticate(IBinder iBinder, String str) throws RemoteException;

    void onAcquired(int i, int i2) throws RemoteException;

    void onAuthenticationFailed() throws RemoteException;

    void onAuthenticationSucceeded(int i, int i2) throws RemoteException;

    void onEnrollmentStateChanged(boolean z) throws RemoteException;

    void onError(int i, int i2) throws RemoteException;

    void onFaceAlgorithmResult(int i, int i2, int i3, String str) throws RemoteException;

    void onHidlServiceDied() throws RemoteException;

    void onRemoved() throws RemoteException;

    void onSystemTime(long j, int i) throws RemoteException;

    void sendCommand(int i, int i2, String str) throws RemoteException;

    void startAuthenticate(IBinder iBinder, String str) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IFaceUI {
        @Override // com.vivo.face.internal.ui.IFaceUI
        public void startAuthenticate(IBinder token, String opPackageName) throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUI
        public void cancelAuthenticate(IBinder token, String opPackageName) throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUI
        public void onAcquired(int acquiredInfo, int vendorCode) throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUI
        public void onAuthenticationSucceeded(int faceId, int userId) throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUI
        public void onAuthenticationFailed() throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUI
        public void onError(int error, int vendorCode) throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUI
        public void onRemoved() throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUI
        public void onFaceAlgorithmResult(int command, int result, int extras, String bundle) throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUI
        public void onHidlServiceDied() throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUI
        public void sendCommand(int command, int extra, String bundle) throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUI
        public void onEnrollmentStateChanged(boolean started) throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUI
        public void onSystemTime(long elapsedRealtime, int what) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IFaceUI {
        private static final String DESCRIPTOR = "com.vivo.face.internal.ui.IFaceUI";
        static final int TRANSACTION_cancelAuthenticate = 2;
        static final int TRANSACTION_onAcquired = 3;
        static final int TRANSACTION_onAuthenticationFailed = 5;
        static final int TRANSACTION_onAuthenticationSucceeded = 4;
        static final int TRANSACTION_onEnrollmentStateChanged = 11;
        static final int TRANSACTION_onError = 6;
        static final int TRANSACTION_onFaceAlgorithmResult = 8;
        static final int TRANSACTION_onHidlServiceDied = 9;
        static final int TRANSACTION_onRemoved = 7;
        static final int TRANSACTION_onSystemTime = 12;
        static final int TRANSACTION_sendCommand = 10;
        static final int TRANSACTION_startAuthenticate = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IFaceUI asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IFaceUI)) {
                return (IFaceUI) iin;
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
                    IBinder _arg0 = data.readStrongBinder();
                    String _arg1 = data.readString();
                    startAuthenticate(_arg0, _arg1);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg02 = data.readStrongBinder();
                    String _arg12 = data.readString();
                    cancelAuthenticate(_arg02, _arg12);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    int _arg13 = data.readInt();
                    onAcquired(_arg03, _arg13);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    int _arg14 = data.readInt();
                    onAuthenticationSucceeded(_arg04, _arg14);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    onAuthenticationFailed();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    int _arg15 = data.readInt();
                    onError(_arg05, _arg15);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    onRemoved();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    int _arg16 = data.readInt();
                    int _arg2 = data.readInt();
                    String _arg3 = data.readString();
                    onFaceAlgorithmResult(_arg06, _arg16, _arg2, _arg3);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    onHidlServiceDied();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    int _arg17 = data.readInt();
                    String _arg22 = data.readString();
                    sendCommand(_arg07, _arg17, _arg22);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg08 = data.readInt() != 0;
                    onEnrollmentStateChanged(_arg08);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg09 = data.readLong();
                    int _arg18 = data.readInt();
                    onSystemTime(_arg09, _arg18);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IFaceUI {
            public static IFaceUI sDefaultImpl;
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

            @Override // com.vivo.face.internal.ui.IFaceUI
            public void startAuthenticate(IBinder token, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(opPackageName);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().startAuthenticate(token, opPackageName);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUI
            public void cancelAuthenticate(IBinder token, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(opPackageName);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().cancelAuthenticate(token, opPackageName);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUI
            public void onAcquired(int acquiredInfo, int vendorCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(acquiredInfo);
                    _data.writeInt(vendorCode);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onAcquired(acquiredInfo, vendorCode);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUI
            public void onAuthenticationSucceeded(int faceId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(faceId);
                    _data.writeInt(userId);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onAuthenticationSucceeded(faceId, userId);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUI
            public void onAuthenticationFailed() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onAuthenticationFailed();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUI
            public void onError(int error, int vendorCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(error);
                    _data.writeInt(vendorCode);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onError(error, vendorCode);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUI
            public void onRemoved() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onRemoved();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUI
            public void onFaceAlgorithmResult(int command, int result, int extras, String bundle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(command);
                    _data.writeInt(result);
                    _data.writeInt(extras);
                    _data.writeString(bundle);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onFaceAlgorithmResult(command, result, extras, bundle);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUI
            public void onHidlServiceDied() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(9, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onHidlServiceDied();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUI
            public void sendCommand(int command, int extra, String bundle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(command);
                    _data.writeInt(extra);
                    _data.writeString(bundle);
                    boolean _status = this.mRemote.transact(10, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().sendCommand(command, extra, bundle);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUI
            public void onEnrollmentStateChanged(boolean started) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(started ? 1 : 0);
                    boolean _status = this.mRemote.transact(11, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onEnrollmentStateChanged(started);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUI
            public void onSystemTime(long elapsedRealtime, int what) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(elapsedRealtime);
                    _data.writeInt(what);
                    boolean _status = this.mRemote.transact(12, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onSystemTime(elapsedRealtime, what);
                    }
                } finally {
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IFaceUI impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IFaceUI getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}