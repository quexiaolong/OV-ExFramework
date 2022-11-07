package android.os;

import java.io.FileDescriptor;

/* loaded from: classes.dex */
public interface IVoldMountCallback extends IInterface {
    boolean onVolumeChecking(FileDescriptor fileDescriptor, String str, String str2) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IVoldMountCallback {
        @Override // android.os.IVoldMountCallback
        public boolean onVolumeChecking(FileDescriptor fuseFd, String path, String internalPath) throws RemoteException {
            return false;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IVoldMountCallback {
        private static final String DESCRIPTOR = "android.os.IVoldMountCallback";
        static final int TRANSACTION_onVolumeChecking = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IVoldMountCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IVoldMountCallback)) {
                return (IVoldMountCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code != 1) {
                if (code == 1598968902) {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(code, data, reply, flags);
            }
            data.enforceInterface(DESCRIPTOR);
            FileDescriptor _arg0 = data.readRawFileDescriptor();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            boolean onVolumeChecking = onVolumeChecking(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(onVolumeChecking ? 1 : 0);
            return true;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IVoldMountCallback {
            public static IVoldMountCallback sDefaultImpl;
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

            @Override // android.os.IVoldMountCallback
            public boolean onVolumeChecking(FileDescriptor fuseFd, String path, String internalPath) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeRawFileDescriptor(fuseFd);
                    _data.writeString(path);
                    _data.writeString(internalPath);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().onVolumeChecking(fuseFd, path, internalPath);
                    }
                    _reply.readException();
                    boolean _status2 = _reply.readInt() != 0;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IVoldMountCallback impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IVoldMountCallback getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}