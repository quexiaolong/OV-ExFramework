package android.media;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface ICaptureStateListener extends IInterface {
    void setCaptureState(boolean z) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ICaptureStateListener {
        @Override // android.media.ICaptureStateListener
        public void setCaptureState(boolean active) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ICaptureStateListener {
        private static final String DESCRIPTOR = "android$media$ICaptureStateListener".replace('$', '.');
        static final int TRANSACTION_setCaptureState = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICaptureStateListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ICaptureStateListener)) {
                return (ICaptureStateListener) iin;
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
            if (code != 1) {
                if (code == 1598968902) {
                    reply.writeString(descriptor);
                    return true;
                }
                return super.onTransact(code, data, reply, flags);
            }
            data.enforceInterface(descriptor);
            boolean _arg0 = data.readInt() != 0;
            setCaptureState(_arg0);
            reply.writeNoException();
            return true;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements ICaptureStateListener {
            public static ICaptureStateListener sDefaultImpl;
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

            @Override // android.media.ICaptureStateListener
            public void setCaptureState(boolean active) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(active ? 1 : 0);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setCaptureState(active);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(ICaptureStateListener impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static ICaptureStateListener getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}