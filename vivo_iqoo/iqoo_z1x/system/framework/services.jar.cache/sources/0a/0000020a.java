package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface INetdUnsolicitedEventListener extends IInterface {
    public static final String HASH = "63adaa5098e4d8621e90c5a84f7cb93505c79311";
    public static final int VERSION = 4;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void onInterfaceAdded(String str) throws RemoteException;

    void onInterfaceAddressRemoved(String str, String str2, int i, int i2) throws RemoteException;

    void onInterfaceAddressUpdated(String str, String str2, int i, int i2) throws RemoteException;

    void onInterfaceChanged(String str, boolean z) throws RemoteException;

    void onInterfaceClassActivityChanged(boolean z, int i, long j, int i2) throws RemoteException;

    void onInterfaceDnsServerInfo(String str, long j, String[] strArr) throws RemoteException;

    void onInterfaceLinkStateChanged(String str, boolean z) throws RemoteException;

    void onInterfaceRemoved(String str) throws RemoteException;

    void onQuotaLimitReached(String str, String str2) throws RemoteException;

    void onRouteChanged(boolean z, String str, String str2, String str3) throws RemoteException;

    void onStrictCleartextDetected(int i, String str) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements INetdUnsolicitedEventListener {
        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceClassActivityChanged(boolean isActive, int timerLabel, long timestampNs, int uid) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onQuotaLimitReached(String alertName, String ifName) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceDnsServerInfo(String ifName, long lifetimeS, String[] servers) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceAddressUpdated(String addr, String ifName, int flags, int scope) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceAddressRemoved(String addr, String ifName, int flags, int scope) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceAdded(String ifName) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceRemoved(String ifName) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceChanged(String ifName, boolean up) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceLinkStateChanged(String ifName, boolean up) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onRouteChanged(boolean updated, String route, String gateway, String ifName) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onStrictCleartextDetected(int uid, String hex) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements INetdUnsolicitedEventListener {
        private static final String DESCRIPTOR = "android$net$INetdUnsolicitedEventListener".replace('$', '.');
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_onInterfaceAdded = 6;
        static final int TRANSACTION_onInterfaceAddressRemoved = 5;
        static final int TRANSACTION_onInterfaceAddressUpdated = 4;
        static final int TRANSACTION_onInterfaceChanged = 8;
        static final int TRANSACTION_onInterfaceClassActivityChanged = 1;
        static final int TRANSACTION_onInterfaceDnsServerInfo = 3;
        static final int TRANSACTION_onInterfaceLinkStateChanged = 9;
        static final int TRANSACTION_onInterfaceRemoved = 7;
        static final int TRANSACTION_onQuotaLimitReached = 2;
        static final int TRANSACTION_onRouteChanged = 10;
        static final int TRANSACTION_onStrictCleartextDetected = 11;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetdUnsolicitedEventListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INetdUnsolicitedEventListener)) {
                return (INetdUnsolicitedEventListener) iin;
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
            String descriptor = DESCRIPTOR;
            if (code == 1598968902) {
                reply.writeString(descriptor);
                return true;
            }
            switch (code) {
                case 1:
                    data.enforceInterface(descriptor);
                    boolean _arg02 = data.readInt() != 0;
                    int _arg1 = data.readInt();
                    long _arg2 = data.readLong();
                    int _arg3 = data.readInt();
                    onInterfaceClassActivityChanged(_arg02, _arg1, _arg2, _arg3);
                    return true;
                case 2:
                    data.enforceInterface(descriptor);
                    String _arg03 = data.readString();
                    String _arg12 = data.readString();
                    onQuotaLimitReached(_arg03, _arg12);
                    return true;
                case 3:
                    data.enforceInterface(descriptor);
                    String _arg04 = data.readString();
                    long _arg13 = data.readLong();
                    String[] _arg22 = data.createStringArray();
                    onInterfaceDnsServerInfo(_arg04, _arg13, _arg22);
                    return true;
                case 4:
                    data.enforceInterface(descriptor);
                    String _arg05 = data.readString();
                    String _arg14 = data.readString();
                    int _arg23 = data.readInt();
                    int _arg32 = data.readInt();
                    onInterfaceAddressUpdated(_arg05, _arg14, _arg23, _arg32);
                    return true;
                case 5:
                    data.enforceInterface(descriptor);
                    String _arg06 = data.readString();
                    String _arg15 = data.readString();
                    int _arg24 = data.readInt();
                    int _arg33 = data.readInt();
                    onInterfaceAddressRemoved(_arg06, _arg15, _arg24, _arg33);
                    return true;
                case 6:
                    data.enforceInterface(descriptor);
                    onInterfaceAdded(data.readString());
                    return true;
                case 7:
                    data.enforceInterface(descriptor);
                    onInterfaceRemoved(data.readString());
                    return true;
                case 8:
                    data.enforceInterface(descriptor);
                    String _arg07 = data.readString();
                    _arg0 = data.readInt() != 0;
                    onInterfaceChanged(_arg07, _arg0);
                    return true;
                case 9:
                    data.enforceInterface(descriptor);
                    String _arg08 = data.readString();
                    _arg0 = data.readInt() != 0;
                    onInterfaceLinkStateChanged(_arg08, _arg0);
                    return true;
                case 10:
                    data.enforceInterface(descriptor);
                    _arg0 = data.readInt() != 0;
                    String _arg16 = data.readString();
                    String _arg25 = data.readString();
                    String _arg34 = data.readString();
                    onRouteChanged(_arg0, _arg16, _arg25, _arg34);
                    return true;
                case 11:
                    data.enforceInterface(descriptor);
                    int _arg09 = data.readInt();
                    String _arg17 = data.readString();
                    onStrictCleartextDetected(_arg09, _arg17);
                    return true;
                default:
                    switch (code) {
                        case TRANSACTION_getInterfaceHash /* 16777214 */:
                            data.enforceInterface(descriptor);
                            reply.writeNoException();
                            reply.writeString(getInterfaceHash());
                            return true;
                        case 16777215:
                            data.enforceInterface(descriptor);
                            reply.writeNoException();
                            reply.writeInt(getInterfaceVersion());
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements INetdUnsolicitedEventListener {
            public static INetdUnsolicitedEventListener sDefaultImpl;
            private IBinder mRemote;
            private int mCachedVersion = -1;
            private String mCachedHash = "-1";

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

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceClassActivityChanged(boolean isActive, int timerLabel, long timestampNs, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(isActive ? 1 : 0);
                    _data.writeInt(timerLabel);
                    _data.writeLong(timestampNs);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onInterfaceClassActivityChanged(isActive, timerLabel, timestampNs, uid);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onQuotaLimitReached(String alertName, String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alertName);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onQuotaLimitReached(alertName, ifName);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceDnsServerInfo(String ifName, long lifetimeS, String[] servers) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeLong(lifetimeS);
                    _data.writeStringArray(servers);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onInterfaceDnsServerInfo(ifName, lifetimeS, servers);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceAddressUpdated(String addr, String ifName, int flags, int scope) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(addr);
                    _data.writeString(ifName);
                    _data.writeInt(flags);
                    _data.writeInt(scope);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onInterfaceAddressUpdated(addr, ifName, flags, scope);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceAddressRemoved(String addr, String ifName, int flags, int scope) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(addr);
                    _data.writeString(ifName);
                    _data.writeInt(flags);
                    _data.writeInt(scope);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onInterfaceAddressRemoved(addr, ifName, flags, scope);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceAdded(String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onInterfaceAdded(ifName);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceRemoved(String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onInterfaceRemoved(ifName);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceChanged(String ifName, boolean up) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeInt(up ? 1 : 0);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onInterfaceChanged(ifName, up);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceLinkStateChanged(String ifName, boolean up) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeInt(up ? 1 : 0);
                    boolean _status = this.mRemote.transact(9, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onInterfaceLinkStateChanged(ifName, up);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onRouteChanged(boolean updated, String route, String gateway, String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(updated ? 1 : 0);
                    _data.writeString(route);
                    _data.writeString(gateway);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(10, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onRouteChanged(updated, route, gateway, ifName);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onStrictCleartextDetected(int uid, String hex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeString(hex);
                    boolean _status = this.mRemote.transact(11, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onStrictCleartextDetected(uid, hex);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(Stub.DESCRIPTOR);
                        boolean _status = this.mRemote.transact(16777215, data, reply, 0);
                        if (!_status && Stub.getDefaultImpl() != null) {
                            return Stub.getDefaultImpl().getInterfaceVersion();
                        }
                        reply.readException();
                        this.mCachedVersion = reply.readInt();
                    } finally {
                        reply.recycle();
                        data.recycle();
                    }
                }
                return this.mCachedVersion;
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        String interfaceHash = Stub.getDefaultImpl().getInterfaceHash();
                        reply.recycle();
                        data.recycle();
                        return interfaceHash;
                    }
                    reply.readException();
                    this.mCachedHash = reply.readString();
                    reply.recycle();
                    data.recycle();
                }
                return this.mCachedHash;
            }
        }

        public static boolean setDefaultImpl(INetdUnsolicitedEventListener impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static INetdUnsolicitedEventListener getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}