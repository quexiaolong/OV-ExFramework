package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface INetworkMonitor extends IInterface {
    public static final String HASH = "02cd6fd07d5c04eca0c35a350f7b0be576242883";
    public static final int NETWORK_TEST_RESULT_INVALID = 1;
    public static final int NETWORK_TEST_RESULT_PARTIAL_CONNECTIVITY = 2;
    public static final int NETWORK_TEST_RESULT_VALID = 0;
    public static final int NETWORK_VALIDATION_PROBE_DNS = 4;
    public static final int NETWORK_VALIDATION_PROBE_FALLBACK = 32;
    public static final int NETWORK_VALIDATION_PROBE_HTTP = 8;
    public static final int NETWORK_VALIDATION_PROBE_HTTPS = 16;
    public static final int NETWORK_VALIDATION_PROBE_PRIVDNS = 64;
    public static final int NETWORK_VALIDATION_RESULT_PARTIAL = 2;
    public static final int NETWORK_VALIDATION_RESULT_VALID = 1;
    public static final int VERSION = 7;

    void forceReevaluation(int i) throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void launchCaptivePortalApp() throws RemoteException;

    void notifyCaptivePortalAppFinished(int i) throws RemoteException;

    void notifyDnsResponse(int i) throws RemoteException;

    void notifyLinkPropertiesChanged(LinkProperties linkProperties) throws RemoteException;

    void notifyNetworkCapabilitiesChanged(NetworkCapabilities networkCapabilities) throws RemoteException;

    void notifyNetworkConnected(LinkProperties linkProperties, NetworkCapabilities networkCapabilities) throws RemoteException;

    void notifyNetworkDisconnected() throws RemoteException;

    void notifyPrivateDnsChanged(PrivateDnsConfigParcel privateDnsConfigParcel) throws RemoteException;

    void setAcceptPartialConnectivity() throws RemoteException;

    void start() throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements INetworkMonitor {
        @Override // android.net.INetworkMonitor
        public void start() throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void launchCaptivePortalApp() throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyCaptivePortalAppFinished(int response) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void setAcceptPartialConnectivity() throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void forceReevaluation(int uid) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyPrivateDnsChanged(PrivateDnsConfigParcel config) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyDnsResponse(int returnCode) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyNetworkConnected(LinkProperties lp, NetworkCapabilities nc) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyNetworkDisconnected() throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyLinkPropertiesChanged(LinkProperties lp) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyNetworkCapabilitiesChanged(NetworkCapabilities nc) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.INetworkMonitor
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements INetworkMonitor {
        private static final String DESCRIPTOR = "android$net$INetworkMonitor".replace('$', '.');
        static final int TRANSACTION_forceReevaluation = 5;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_launchCaptivePortalApp = 2;
        static final int TRANSACTION_notifyCaptivePortalAppFinished = 3;
        static final int TRANSACTION_notifyDnsResponse = 7;
        static final int TRANSACTION_notifyLinkPropertiesChanged = 10;
        static final int TRANSACTION_notifyNetworkCapabilitiesChanged = 11;
        static final int TRANSACTION_notifyNetworkConnected = 8;
        static final int TRANSACTION_notifyNetworkDisconnected = 9;
        static final int TRANSACTION_notifyPrivateDnsChanged = 6;
        static final int TRANSACTION_setAcceptPartialConnectivity = 4;
        static final int TRANSACTION_start = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkMonitor asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INetworkMonitor)) {
                return (INetworkMonitor) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            PrivateDnsConfigParcel _arg0;
            LinkProperties _arg02;
            NetworkCapabilities _arg1;
            LinkProperties _arg03;
            NetworkCapabilities _arg04;
            String descriptor = DESCRIPTOR;
            if (code == 1598968902) {
                reply.writeString(descriptor);
                return true;
            }
            switch (code) {
                case 1:
                    data.enforceInterface(descriptor);
                    start();
                    return true;
                case 2:
                    data.enforceInterface(descriptor);
                    launchCaptivePortalApp();
                    return true;
                case 3:
                    data.enforceInterface(descriptor);
                    int _arg05 = data.readInt();
                    notifyCaptivePortalAppFinished(_arg05);
                    return true;
                case 4:
                    data.enforceInterface(descriptor);
                    setAcceptPartialConnectivity();
                    return true;
                case 5:
                    data.enforceInterface(descriptor);
                    int _arg06 = data.readInt();
                    forceReevaluation(_arg06);
                    return true;
                case 6:
                    data.enforceInterface(descriptor);
                    if (data.readInt() != 0) {
                        _arg0 = PrivateDnsConfigParcel.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    notifyPrivateDnsChanged(_arg0);
                    return true;
                case 7:
                    data.enforceInterface(descriptor);
                    int _arg07 = data.readInt();
                    notifyDnsResponse(_arg07);
                    return true;
                case 8:
                    data.enforceInterface(descriptor);
                    if (data.readInt() != 0) {
                        _arg02 = (LinkProperties) LinkProperties.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg1 = (NetworkCapabilities) NetworkCapabilities.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    notifyNetworkConnected(_arg02, _arg1);
                    return true;
                case 9:
                    data.enforceInterface(descriptor);
                    notifyNetworkDisconnected();
                    return true;
                case 10:
                    data.enforceInterface(descriptor);
                    if (data.readInt() != 0) {
                        _arg03 = (LinkProperties) LinkProperties.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    notifyLinkPropertiesChanged(_arg03);
                    return true;
                case 11:
                    data.enforceInterface(descriptor);
                    if (data.readInt() != 0) {
                        _arg04 = (NetworkCapabilities) NetworkCapabilities.CREATOR.createFromParcel(data);
                    } else {
                        _arg04 = null;
                    }
                    notifyNetworkCapabilitiesChanged(_arg04);
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
        public static class Proxy implements INetworkMonitor {
            public static INetworkMonitor sDefaultImpl;
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

            @Override // android.net.INetworkMonitor
            public void start() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().start();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void launchCaptivePortalApp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().launchCaptivePortalApp();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyCaptivePortalAppFinished(int response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(response);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().notifyCaptivePortalAppFinished(response);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void setAcceptPartialConnectivity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setAcceptPartialConnectivity();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void forceReevaluation(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().forceReevaluation(uid);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyPrivateDnsChanged(PrivateDnsConfigParcel config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().notifyPrivateDnsChanged(config);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyDnsResponse(int returnCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(returnCode);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().notifyDnsResponse(returnCode);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyNetworkConnected(LinkProperties lp, NetworkCapabilities nc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (lp != null) {
                        _data.writeInt(1);
                        lp.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (nc != null) {
                        _data.writeInt(1);
                        nc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().notifyNetworkConnected(lp, nc);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyNetworkDisconnected() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(9, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().notifyNetworkDisconnected();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyLinkPropertiesChanged(LinkProperties lp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (lp != null) {
                        _data.writeInt(1);
                        lp.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    boolean _status = this.mRemote.transact(10, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().notifyLinkPropertiesChanged(lp);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyNetworkCapabilitiesChanged(NetworkCapabilities nc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (nc != null) {
                        _data.writeInt(1);
                        nc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    boolean _status = this.mRemote.transact(11, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().notifyNetworkCapabilitiesChanged(nc);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
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

            @Override // android.net.INetworkMonitor
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

        public static boolean setDefaultImpl(INetworkMonitor impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static INetworkMonitor getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}