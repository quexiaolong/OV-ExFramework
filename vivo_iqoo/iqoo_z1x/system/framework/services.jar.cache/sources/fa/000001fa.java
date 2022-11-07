package android.net;

import android.net.metrics.INetdEventListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface IDnsResolver extends IInterface {
    public static final int DNS_RESOLVER_LOG_DEBUG = 1;
    public static final int DNS_RESOLVER_LOG_ERROR = 4;
    public static final int DNS_RESOLVER_LOG_INFO = 2;
    public static final int DNS_RESOLVER_LOG_VERBOSE = 0;
    public static final int DNS_RESOLVER_LOG_WARNING = 3;
    public static final String HASH = "5bc773534ffa9d7614ccd27d5de6daefdeeee2b8";
    public static final int RESOLVER_PARAMS_BASE_TIMEOUT_MSEC = 4;
    public static final int RESOLVER_PARAMS_COUNT = 6;
    public static final int RESOLVER_PARAMS_MAX_SAMPLES = 3;
    public static final int RESOLVER_PARAMS_MIN_SAMPLES = 2;
    public static final int RESOLVER_PARAMS_RETRY_COUNT = 5;
    public static final int RESOLVER_PARAMS_SAMPLE_VALIDITY = 0;
    public static final int RESOLVER_PARAMS_SUCCESS_THRESHOLD = 1;
    public static final int RESOLVER_STATS_COUNT = 7;
    public static final int RESOLVER_STATS_ERRORS = 1;
    public static final int RESOLVER_STATS_INTERNAL_ERRORS = 3;
    public static final int RESOLVER_STATS_LAST_SAMPLE_TIME = 5;
    public static final int RESOLVER_STATS_RTT_AVG = 4;
    public static final int RESOLVER_STATS_SUCCESSES = 0;
    public static final int RESOLVER_STATS_TIMEOUTS = 2;
    public static final int RESOLVER_STATS_USABLE = 6;
    public static final int TC_MODE_DEFAULT = 0;
    public static final int TC_MODE_UDP_TCP = 1;
    public static final int TRANSPORT_BLUETOOTH = 2;
    public static final int TRANSPORT_CELLULAR = 0;
    public static final int TRANSPORT_ETHERNET = 3;
    public static final int TRANSPORT_LOWPAN = 6;
    public static final int TRANSPORT_TEST = 7;
    public static final int TRANSPORT_UNKNOWN = -1;
    public static final int TRANSPORT_VPN = 4;
    public static final int TRANSPORT_WIFI = 1;
    public static final int TRANSPORT_WIFI_AWARE = 5;
    public static final int VERSION = 6;

    void createNetworkCache(int i) throws RemoteException;

    void destroyNetworkCache(int i) throws RemoteException;

    void flushNetworkCache(int i) throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    String getPrefix64(int i) throws RemoteException;

    void getResolverInfo(int i, String[] strArr, String[] strArr2, String[] strArr3, int[] iArr, int[] iArr2, int[] iArr3) throws RemoteException;

    boolean isAlive() throws RemoteException;

    void registerEventListener(INetdEventListener iNetdEventListener) throws RemoteException;

    void setLogSeverity(int i) throws RemoteException;

    void setPrefix64(int i, String str) throws RemoteException;

    void setResolverConfiguration(ResolverParamsParcel resolverParamsParcel) throws RemoteException;

    void startPrefix64Discovery(int i) throws RemoteException;

    void stopPrefix64Discovery(int i) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IDnsResolver {
        @Override // android.net.IDnsResolver
        public boolean isAlive() throws RemoteException {
            return false;
        }

        @Override // android.net.IDnsResolver
        public void registerEventListener(INetdEventListener listener) throws RemoteException {
        }

        @Override // android.net.IDnsResolver
        public void setResolverConfiguration(ResolverParamsParcel resolverParams) throws RemoteException {
        }

        @Override // android.net.IDnsResolver
        public void getResolverInfo(int netId, String[] servers, String[] domains, String[] tlsServers, int[] params, int[] stats, int[] wait_for_pending_req_timeout_count) throws RemoteException {
        }

        @Override // android.net.IDnsResolver
        public void startPrefix64Discovery(int netId) throws RemoteException {
        }

        @Override // android.net.IDnsResolver
        public void stopPrefix64Discovery(int netId) throws RemoteException {
        }

        @Override // android.net.IDnsResolver
        public String getPrefix64(int netId) throws RemoteException {
            return null;
        }

        @Override // android.net.IDnsResolver
        public void createNetworkCache(int netId) throws RemoteException {
        }

        @Override // android.net.IDnsResolver
        public void destroyNetworkCache(int netId) throws RemoteException {
        }

        @Override // android.net.IDnsResolver
        public void setLogSeverity(int logSeverity) throws RemoteException {
        }

        @Override // android.net.IDnsResolver
        public void flushNetworkCache(int netId) throws RemoteException {
        }

        @Override // android.net.IDnsResolver
        public void setPrefix64(int netId, String prefix) throws RemoteException {
        }

        @Override // android.net.IDnsResolver
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.IDnsResolver
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IDnsResolver {
        private static final String DESCRIPTOR = "android$net$IDnsResolver".replace('$', '.');
        static final int TRANSACTION_createNetworkCache = 8;
        static final int TRANSACTION_destroyNetworkCache = 9;
        static final int TRANSACTION_flushNetworkCache = 11;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_getPrefix64 = 7;
        static final int TRANSACTION_getResolverInfo = 4;
        static final int TRANSACTION_isAlive = 1;
        static final int TRANSACTION_registerEventListener = 2;
        static final int TRANSACTION_setLogSeverity = 10;
        static final int TRANSACTION_setPrefix64 = 12;
        static final int TRANSACTION_setResolverConfiguration = 3;
        static final int TRANSACTION_startPrefix64Discovery = 5;
        static final int TRANSACTION_stopPrefix64Discovery = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDnsResolver asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IDnsResolver)) {
                return (IDnsResolver) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            ResolverParamsParcel _arg0;
            String[] _arg1;
            String[] _arg2;
            String[] _arg3;
            int[] _arg4;
            int[] _arg5;
            String descriptor = DESCRIPTOR;
            if (code == 1598968902) {
                reply.writeString(descriptor);
                return true;
            }
            switch (code) {
                case 1:
                    data.enforceInterface(descriptor);
                    boolean isAlive = isAlive();
                    reply.writeNoException();
                    reply.writeInt(isAlive ? 1 : 0);
                    return true;
                case 2:
                    data.enforceInterface(descriptor);
                    INetdEventListener _arg02 = INetdEventListener.Stub.asInterface(data.readStrongBinder());
                    registerEventListener(_arg02);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(descriptor);
                    if (data.readInt() != 0) {
                        _arg0 = ResolverParamsParcel.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    setResolverConfiguration(_arg0);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(descriptor);
                    int _arg03 = data.readInt();
                    int _arg1_length = data.readInt();
                    if (_arg1_length < 0) {
                        _arg1 = null;
                    } else {
                        String[] _arg12 = new String[_arg1_length];
                        _arg1 = _arg12;
                    }
                    int _arg2_length = data.readInt();
                    if (_arg2_length < 0) {
                        _arg2 = null;
                    } else {
                        String[] _arg22 = new String[_arg2_length];
                        _arg2 = _arg22;
                    }
                    int _arg3_length = data.readInt();
                    if (_arg3_length < 0) {
                        _arg3 = null;
                    } else {
                        String[] _arg32 = new String[_arg3_length];
                        _arg3 = _arg32;
                    }
                    int _arg4_length = data.readInt();
                    if (_arg4_length < 0) {
                        _arg4 = null;
                    } else {
                        int[] _arg42 = new int[_arg4_length];
                        _arg4 = _arg42;
                    }
                    int _arg5_length = data.readInt();
                    if (_arg5_length < 0) {
                        _arg5 = null;
                    } else {
                        _arg5 = new int[_arg5_length];
                    }
                    int _arg6_length = data.readInt();
                    int[] _arg6 = _arg6_length < 0 ? null : new int[_arg6_length];
                    int[] _arg43 = _arg4;
                    String[] _arg33 = _arg3;
                    String[] _arg34 = _arg2;
                    getResolverInfo(_arg03, _arg1, _arg34, _arg33, _arg43, _arg5, _arg6);
                    reply.writeNoException();
                    reply.writeStringArray(_arg1);
                    reply.writeStringArray(_arg2);
                    reply.writeStringArray(_arg33);
                    reply.writeIntArray(_arg43);
                    reply.writeIntArray(_arg5);
                    reply.writeIntArray(_arg6);
                    return true;
                case 5:
                    data.enforceInterface(descriptor);
                    int _arg04 = data.readInt();
                    startPrefix64Discovery(_arg04);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(descriptor);
                    int _arg05 = data.readInt();
                    stopPrefix64Discovery(_arg05);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(descriptor);
                    int _arg06 = data.readInt();
                    String _result = getPrefix64(_arg06);
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                case 8:
                    data.enforceInterface(descriptor);
                    int _arg07 = data.readInt();
                    createNetworkCache(_arg07);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(descriptor);
                    int _arg08 = data.readInt();
                    destroyNetworkCache(_arg08);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(descriptor);
                    int _arg09 = data.readInt();
                    setLogSeverity(_arg09);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(descriptor);
                    int _arg010 = data.readInt();
                    flushNetworkCache(_arg010);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(descriptor);
                    int _arg011 = data.readInt();
                    String _arg13 = data.readString();
                    setPrefix64(_arg011, _arg13);
                    reply.writeNoException();
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
        public static class Proxy implements IDnsResolver {
            public static IDnsResolver sDefaultImpl;
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

            @Override // android.net.IDnsResolver
            public boolean isAlive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isAlive();
                    }
                    _reply.readException();
                    boolean _status2 = _reply.readInt() != 0;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.IDnsResolver
            public void registerEventListener(INetdEventListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().registerEventListener(listener);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.IDnsResolver
            public void setResolverConfiguration(ResolverParamsParcel resolverParams) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (resolverParams != null) {
                        _data.writeInt(1);
                        resolverParams.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setResolverConfiguration(resolverParams);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.IDnsResolver
            public void getResolverInfo(int netId, String[] servers, String[] domains, String[] tlsServers, int[] params, int[] stats, int[] wait_for_pending_req_timeout_count) throws RemoteException {
                Parcel _reply;
                Parcel _data = Parcel.obtain();
                Parcel _reply2 = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    if (servers == null) {
                        try {
                            _data.writeInt(-1);
                        } catch (Throwable th) {
                            th = th;
                            _reply = _reply2;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } else {
                        _data.writeInt(servers.length);
                    }
                    if (domains == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(domains.length);
                    }
                    if (tlsServers == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(tlsServers.length);
                    }
                    if (params == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(params.length);
                    }
                    if (stats == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(stats.length);
                    }
                    if (wait_for_pending_req_timeout_count == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(wait_for_pending_req_timeout_count.length);
                    }
                    boolean _status = this.mRemote.transact(4, _data, _reply2, 0);
                    if (!_status) {
                        try {
                            if (Stub.getDefaultImpl() != null) {
                                try {
                                    Stub.getDefaultImpl().getResolverInfo(netId, servers, domains, tlsServers, params, stats, wait_for_pending_req_timeout_count);
                                    _reply2.recycle();
                                    _data.recycle();
                                    return;
                                } catch (Throwable th2) {
                                    th = th2;
                                    _reply = _reply2;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            _reply = _reply2;
                        }
                    }
                    try {
                        _reply2.readException();
                        _reply = _reply2;
                    } catch (Throwable th4) {
                        th = th4;
                        _reply = _reply2;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    _reply = _reply2;
                }
                try {
                    _reply.readStringArray(servers);
                    _reply.readStringArray(domains);
                    _reply.readStringArray(tlsServers);
                    _reply.readIntArray(params);
                    _reply.readIntArray(stats);
                    _reply.readIntArray(wait_for_pending_req_timeout_count);
                    _reply.recycle();
                    _data.recycle();
                } catch (Throwable th6) {
                    th = th6;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            @Override // android.net.IDnsResolver
            public void startPrefix64Discovery(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    boolean _status = this.mRemote.transact(5, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().startPrefix64Discovery(netId);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.IDnsResolver
            public void stopPrefix64Discovery(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    boolean _status = this.mRemote.transact(6, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().stopPrefix64Discovery(netId);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.IDnsResolver
            public String getPrefix64(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    boolean _status = this.mRemote.transact(7, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getPrefix64(netId);
                    }
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.IDnsResolver
            public void createNetworkCache(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    boolean _status = this.mRemote.transact(8, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().createNetworkCache(netId);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.IDnsResolver
            public void destroyNetworkCache(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    boolean _status = this.mRemote.transact(9, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().destroyNetworkCache(netId);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.IDnsResolver
            public void setLogSeverity(int logSeverity) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(logSeverity);
                    boolean _status = this.mRemote.transact(10, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setLogSeverity(logSeverity);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.IDnsResolver
            public void flushNetworkCache(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    boolean _status = this.mRemote.transact(11, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().flushNetworkCache(netId);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.IDnsResolver
            public void setPrefix64(int netId, String prefix) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(prefix);
                    boolean _status = this.mRemote.transact(12, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setPrefix64(netId, prefix);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.IDnsResolver
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

            @Override // android.net.IDnsResolver
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

        public static boolean setDefaultImpl(IDnsResolver impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IDnsResolver getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}