package com.vivo.dr;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.vivo.dr.IDMClient;
import java.util.Map;

/* loaded from: classes.dex */
public interface IDMService extends IInterface {
    void recordKeyInfo(Map map) throws RemoteException;

    void recordLoopLog(String str) throws RemoteException;

    void reportLocationEvent(int i, String str) throws RemoteException;

    void setCallback(IDMClient iDMClient) throws RemoteException;

    void updateConfig(Map map) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IDMService {
        @Override // com.vivo.dr.IDMService
        public void reportLocationEvent(int eventType, String stack) throws RemoteException {
        }

        @Override // com.vivo.dr.IDMService
        public void recordLoopLog(String log) throws RemoteException {
        }

        @Override // com.vivo.dr.IDMService
        public void recordKeyInfo(Map map) throws RemoteException {
        }

        @Override // com.vivo.dr.IDMService
        public void setCallback(IDMClient client) throws RemoteException {
        }

        @Override // com.vivo.dr.IDMService
        public void updateConfig(Map configs) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IDMService {
        private static final String DESCRIPTOR = "com.vivo.dr.IDMService";
        static final int TRANSACTION_recordKeyInfo = 3;
        static final int TRANSACTION_recordLoopLog = 2;
        static final int TRANSACTION_reportLocationEvent = 1;
        static final int TRANSACTION_setCallback = 4;
        static final int TRANSACTION_updateConfig = 5;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDMService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IDMService)) {
                return (IDMService) iin;
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
                String _arg1 = data.readString();
                reportLocationEvent(_arg0, _arg1);
                return true;
            } else if (code == 2) {
                data.enforceInterface(DESCRIPTOR);
                String _arg02 = data.readString();
                recordLoopLog(_arg02);
                return true;
            } else if (code == 3) {
                data.enforceInterface(DESCRIPTOR);
                ClassLoader cl = getClass().getClassLoader();
                Map _arg03 = data.readHashMap(cl);
                recordKeyInfo(_arg03);
                return true;
            } else if (code == 4) {
                data.enforceInterface(DESCRIPTOR);
                IDMClient _arg04 = IDMClient.Stub.asInterface(data.readStrongBinder());
                setCallback(_arg04);
                return true;
            } else if (code != 5) {
                if (code == 1598968902) {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(code, data, reply, flags);
            } else {
                data.enforceInterface(DESCRIPTOR);
                ClassLoader cl2 = getClass().getClassLoader();
                Map _arg05 = data.readHashMap(cl2);
                updateConfig(_arg05);
                return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IDMService {
            public static IDMService sDefaultImpl;
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

            @Override // com.vivo.dr.IDMService
            public void reportLocationEvent(int eventType, String stack) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(eventType);
                    _data.writeString(stack);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().reportLocationEvent(eventType, stack);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.dr.IDMService
            public void recordLoopLog(String log) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(log);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().recordLoopLog(log);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.dr.IDMService
            public void recordKeyInfo(Map map) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeMap(map);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().recordKeyInfo(map);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.dr.IDMService
            public void setCallback(IDMClient client) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setCallback(client);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.dr.IDMService
            public void updateConfig(Map configs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeMap(configs);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().updateConfig(configs);
                    }
                } finally {
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IDMService impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IDMService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}