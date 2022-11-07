package com.vivo.face.internal.ui;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.vivo.face.internal.ui.IFaceUI;
import java.io.FileDescriptor;

/* loaded from: classes.dex */
public interface IFaceUIManagerService extends IInterface {
    FileDescriptor getMemoryFileDescriptor(int i) throws RemoteException;

    void registerFaceUI(IFaceUI iFaceUI) throws RemoteException;

    void sendAuthenticationResult(boolean z) throws RemoteException;

    void sendDialogVisibleState(boolean z) throws RemoteException;

    void sendLockoutState(boolean z) throws RemoteException;

    void sendMessageToFingerprint(int i, String str, byte[] bArr) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IFaceUIManagerService {
        @Override // com.vivo.face.internal.ui.IFaceUIManagerService
        public void registerFaceUI(IFaceUI callback) throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUIManagerService
        public void sendMessageToFingerprint(int msg, String extra, byte[] array) throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUIManagerService
        public FileDescriptor getMemoryFileDescriptor(int memorySize) throws RemoteException {
            return null;
        }

        @Override // com.vivo.face.internal.ui.IFaceUIManagerService
        public void sendLockoutState(boolean lockoutReady) throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUIManagerService
        public void sendDialogVisibleState(boolean visible) throws RemoteException {
        }

        @Override // com.vivo.face.internal.ui.IFaceUIManagerService
        public void sendAuthenticationResult(boolean succeed) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IFaceUIManagerService {
        private static final String DESCRIPTOR = "com.vivo.face.internal.ui.IFaceUIManagerService";
        static final int TRANSACTION_getMemoryFileDescriptor = 3;
        static final int TRANSACTION_registerFaceUI = 1;
        static final int TRANSACTION_sendAuthenticationResult = 6;
        static final int TRANSACTION_sendDialogVisibleState = 5;
        static final int TRANSACTION_sendLockoutState = 4;
        static final int TRANSACTION_sendMessageToFingerprint = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IFaceUIManagerService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IFaceUIManagerService)) {
                return (IFaceUIManagerService) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            boolean _arg0;
            if (code == 1598968902) {
                reply.writeString(DESCRIPTOR);
                return true;
            }
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    registerFaceUI(IFaceUI.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    String _arg1 = data.readString();
                    byte[] _arg2 = data.createByteArray();
                    sendMessageToFingerprint(_arg02, _arg1, _arg2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    FileDescriptor _result = getMemoryFileDescriptor(data.readInt());
                    reply.writeNoException();
                    reply.writeRawFileDescriptor(_result);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readInt() != 0;
                    sendLockoutState(_arg0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readInt() != 0;
                    sendDialogVisibleState(_arg0);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readInt() != 0;
                    sendAuthenticationResult(_arg0);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IFaceUIManagerService {
            public static IFaceUIManagerService sDefaultImpl;
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

            @Override // com.vivo.face.internal.ui.IFaceUIManagerService
            public void registerFaceUI(IFaceUI callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().registerFaceUI(callback);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUIManagerService
            public void sendMessageToFingerprint(int msg, String extra, byte[] array) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(msg);
                    _data.writeString(extra);
                    _data.writeByteArray(array);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().sendMessageToFingerprint(msg, extra, array);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUIManagerService
            public FileDescriptor getMemoryFileDescriptor(int memorySize) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(memorySize);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getMemoryFileDescriptor(memorySize);
                    }
                    _reply.readException();
                    FileDescriptor _result = _reply.readRawFileDescriptor();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUIManagerService
            public void sendLockoutState(boolean lockoutReady) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(lockoutReady ? 1 : 0);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().sendLockoutState(lockoutReady);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUIManagerService
            public void sendDialogVisibleState(boolean visible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(visible ? 1 : 0);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().sendDialogVisibleState(visible);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.ui.IFaceUIManagerService
            public void sendAuthenticationResult(boolean succeed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(succeed ? 1 : 0);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().sendAuthenticationResult(succeed);
                    }
                } finally {
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IFaceUIManagerService impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IFaceUIManagerService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}