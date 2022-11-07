package android.os;

/* loaded from: classes.dex */
public interface IIdmap2 extends IInterface {
    String createIdmap(String str, String str2, int i, boolean z, int i2) throws RemoteException;

    String getIdmapPath(String str, int i) throws RemoteException;

    boolean removeIdmap(String str, int i) throws RemoteException;

    boolean verifyIdmap(String str, String str2, int i, boolean z, int i2) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IIdmap2 {
        @Override // android.os.IIdmap2
        public String getIdmapPath(String overlayApkPath, int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IIdmap2
        public boolean removeIdmap(String overlayApkPath, int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IIdmap2
        public boolean verifyIdmap(String targetApkPath, String overlayApkPath, int fulfilledPolicies, boolean enforceOverlayable, int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IIdmap2
        public String createIdmap(String targetApkPath, String overlayApkPath, int fulfilledPolicies, boolean enforceOverlayable, int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IIdmap2 {
        private static final String DESCRIPTOR = "android.os.IIdmap2";
        static final int TRANSACTION_createIdmap = 4;
        static final int TRANSACTION_getIdmapPath = 1;
        static final int TRANSACTION_removeIdmap = 2;
        static final int TRANSACTION_verifyIdmap = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IIdmap2 asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IIdmap2)) {
                return (IIdmap2) iin;
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
                String _arg0 = data.readString();
                int _arg1 = data.readInt();
                String _result = getIdmapPath(_arg0, _arg1);
                reply.writeNoException();
                reply.writeString(_result);
                return true;
            } else if (code == 2) {
                data.enforceInterface(DESCRIPTOR);
                String _arg02 = data.readString();
                int _arg12 = data.readInt();
                boolean removeIdmap = removeIdmap(_arg02, _arg12);
                reply.writeNoException();
                reply.writeInt(removeIdmap ? 1 : 0);
                return true;
            } else if (code == 3) {
                data.enforceInterface(DESCRIPTOR);
                String _arg03 = data.readString();
                String _arg13 = data.readString();
                int _arg2 = data.readInt();
                boolean _arg3 = data.readInt() != 0;
                int _arg4 = data.readInt();
                boolean verifyIdmap = verifyIdmap(_arg03, _arg13, _arg2, _arg3, _arg4);
                reply.writeNoException();
                reply.writeInt(verifyIdmap ? 1 : 0);
                return true;
            } else if (code != 4) {
                if (code == 1598968902) {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(code, data, reply, flags);
            } else {
                data.enforceInterface(DESCRIPTOR);
                String _arg04 = data.readString();
                String _arg14 = data.readString();
                int _arg22 = data.readInt();
                boolean _arg32 = data.readInt() != 0;
                int _arg42 = data.readInt();
                String _result2 = createIdmap(_arg04, _arg14, _arg22, _arg32, _arg42);
                reply.writeNoException();
                reply.writeString(_result2);
                return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IIdmap2 {
            public static IIdmap2 sDefaultImpl;
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

            @Override // android.os.IIdmap2
            public String getIdmapPath(String overlayApkPath, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(overlayApkPath);
                    _data.writeInt(userId);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getIdmapPath(overlayApkPath, userId);
                    }
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IIdmap2
            public boolean removeIdmap(String overlayApkPath, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(overlayApkPath);
                    _data.writeInt(userId);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().removeIdmap(overlayApkPath, userId);
                    }
                    _reply.readException();
                    boolean _status2 = _reply.readInt() != 0;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IIdmap2
            public boolean verifyIdmap(String targetApkPath, String overlayApkPath, int fulfilledPolicies, boolean enforceOverlayable, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    _data.writeString(targetApkPath);
                } catch (Throwable th2) {
                    th = th2;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
                try {
                    _data.writeString(overlayApkPath);
                    try {
                        _data.writeInt(fulfilledPolicies);
                        _data.writeInt(enforceOverlayable ? 1 : 0);
                        try {
                            _data.writeInt(userId);
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                        if (!_status && Stub.getDefaultImpl() != null) {
                            boolean verifyIdmap = Stub.getDefaultImpl().verifyIdmap(targetApkPath, overlayApkPath, fulfilledPolicies, enforceOverlayable, userId);
                            _reply.recycle();
                            _data.recycle();
                            return verifyIdmap;
                        }
                        _reply.readException();
                        boolean _result = _reply.readInt() != 0;
                        _reply.recycle();
                        _data.recycle();
                        return _result;
                    } catch (Throwable th5) {
                        th = th5;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th6) {
                    th = th6;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            @Override // android.os.IIdmap2
            public String createIdmap(String targetApkPath, String overlayApkPath, int fulfilledPolicies, boolean enforceOverlayable, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(targetApkPath);
                    _data.writeString(overlayApkPath);
                    _data.writeInt(fulfilledPolicies);
                    _data.writeInt(enforceOverlayable ? 1 : 0);
                    _data.writeInt(userId);
                    boolean _status = this.mRemote.transact(4, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().createIdmap(targetApkPath, overlayApkPath, fulfilledPolicies, enforceOverlayable, userId);
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

        public static boolean setDefaultImpl(IIdmap2 impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IIdmap2 getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}