package com.android.internal.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.net.IOemNetdUnsolicitedEventListener;

/* loaded from: classes.dex */
public interface IOemNetd extends IInterface {
    void addPolicyRoute(String str, String str2, String str3) throws RemoteException;

    int bindUidToNetwork(int i, String str) throws RemoteException;

    void clearFirewallChain(String str) throws RemoteException;

    int delBindUid(int i) throws RemoteException;

    void destroySocketsOnAddr(String str) throws RemoteException;

    void firewallSetMobileUidRule(int i, boolean z) throws RemoteException;

    boolean firewallSetMobileUids(int[] iArr) throws RemoteException;

    boolean firewallSetUidForUserChain(int i, String str, int[] iArr) throws RemoteException;

    void firewallSetWifiUidRule(int i, boolean z) throws RemoteException;

    boolean firewallSetWifiUids(int[] iArr) throws RemoteException;

    void flushDnsCache(int i) throws RemoteException;

    int getUidBindedToNetwork(int i) throws RemoteException;

    boolean isAlive() throws RemoteException;

    void registerOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener iOemNetdUnsolicitedEventListener) throws RemoteException;

    int setAccelerationMode(int i) throws RemoteException;

    void setFirewallUidChainRule(int i, int i2, boolean z) throws RemoteException;

    void setInterfaceThrottle(String str, int i, int i2) throws RemoteException;

    int setNetidGwForNetCoexist(int i, int i2, String str, String str2) throws RemoteException;

    int setNetworkAccessRuleForUid(String str, String str2, String str3) throws RemoteException;

    int setNetworkForbidRule(String str, String str2) throws RemoteException;

    void setUidPackSeparate(int i, boolean z) throws RemoteException;

    void updateIfaceRatinfo(String str, int i) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IOemNetd {
        @Override // com.android.internal.net.IOemNetd
        public boolean isAlive() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public void registerOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener listener) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean firewallSetMobileUids(int[] uids) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public void firewallSetMobileUidRule(int uid, boolean allow) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean firewallSetWifiUids(int[] uids) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public void firewallSetWifiUidRule(int uid, boolean allow) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean firewallSetUidForUserChain(int networkType, String chain, int[] uids) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public void setUidPackSeparate(int uid, boolean needSepar) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void updateIfaceRatinfo(String name, int rat) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void setInterfaceThrottle(String interfaceName, int rxKbps, int txKbps) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void setFirewallUidChainRule(int uid, int networkType, boolean allow) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void clearFirewallChain(String chain) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void flushDnsCache(int netId) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void addPolicyRoute(String tableName, String detNetAddr, String devName) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public int bindUidToNetwork(int uid, String info) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.net.IOemNetd
        public int delBindUid(int uid) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.net.IOemNetd
        public int getUidBindedToNetwork(int uid) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.net.IOemNetd
        public void destroySocketsOnAddr(String addrstr) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public int setNetworkForbidRule(String operate, String iface) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.net.IOemNetd
        public int setNetworkAccessRuleForUid(String operate, String iface, String uid) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.net.IOemNetd
        public int setAccelerationMode(int mode) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.net.IOemNetd
        public int setNetidGwForNetCoexist(int state, int netid, String netseg, String mask) throws RemoteException {
            return 0;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IOemNetd {
        private static final String DESCRIPTOR = "com$android$internal$net$IOemNetd".replace('$', '.');
        static final int TRANSACTION_addPolicyRoute = 14;
        static final int TRANSACTION_bindUidToNetwork = 15;
        static final int TRANSACTION_clearFirewallChain = 12;
        static final int TRANSACTION_delBindUid = 16;
        static final int TRANSACTION_destroySocketsOnAddr = 18;
        static final int TRANSACTION_firewallSetMobileUidRule = 4;
        static final int TRANSACTION_firewallSetMobileUids = 3;
        static final int TRANSACTION_firewallSetUidForUserChain = 7;
        static final int TRANSACTION_firewallSetWifiUidRule = 6;
        static final int TRANSACTION_firewallSetWifiUids = 5;
        static final int TRANSACTION_flushDnsCache = 13;
        static final int TRANSACTION_getUidBindedToNetwork = 17;
        static final int TRANSACTION_isAlive = 1;
        static final int TRANSACTION_registerOemUnsolicitedEventListener = 2;
        static final int TRANSACTION_setAccelerationMode = 21;
        static final int TRANSACTION_setFirewallUidChainRule = 11;
        static final int TRANSACTION_setInterfaceThrottle = 10;
        static final int TRANSACTION_setNetidGwForNetCoexist = 22;
        static final int TRANSACTION_setNetworkAccessRuleForUid = 20;
        static final int TRANSACTION_setNetworkForbidRule = 19;
        static final int TRANSACTION_setUidPackSeparate = 8;
        static final int TRANSACTION_updateIfaceRatinfo = 9;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IOemNetd asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IOemNetd)) {
                return (IOemNetd) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            boolean _arg2;
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
                    IOemNetdUnsolicitedEventListener _arg0 = IOemNetdUnsolicitedEventListener.Stub.asInterface(data.readStrongBinder());
                    registerOemUnsolicitedEventListener(_arg0);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(descriptor);
                    int[] _arg02 = data.createIntArray();
                    boolean firewallSetMobileUids = firewallSetMobileUids(_arg02);
                    reply.writeNoException();
                    reply.writeInt(firewallSetMobileUids ? 1 : 0);
                    return true;
                case 4:
                    data.enforceInterface(descriptor);
                    int _arg03 = data.readInt();
                    _arg2 = data.readInt() != 0;
                    firewallSetMobileUidRule(_arg03, _arg2);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(descriptor);
                    int[] _arg04 = data.createIntArray();
                    boolean firewallSetWifiUids = firewallSetWifiUids(_arg04);
                    reply.writeNoException();
                    reply.writeInt(firewallSetWifiUids ? 1 : 0);
                    return true;
                case 6:
                    data.enforceInterface(descriptor);
                    int _arg05 = data.readInt();
                    _arg2 = data.readInt() != 0;
                    firewallSetWifiUidRule(_arg05, _arg2);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(descriptor);
                    int _arg06 = data.readInt();
                    String _arg1 = data.readString();
                    boolean firewallSetUidForUserChain = firewallSetUidForUserChain(_arg06, _arg1, data.createIntArray());
                    reply.writeNoException();
                    reply.writeInt(firewallSetUidForUserChain ? 1 : 0);
                    return true;
                case 8:
                    data.enforceInterface(descriptor);
                    int _arg07 = data.readInt();
                    _arg2 = data.readInt() != 0;
                    setUidPackSeparate(_arg07, _arg2);
                    return true;
                case 9:
                    data.enforceInterface(descriptor);
                    String _arg08 = data.readString();
                    int _arg12 = data.readInt();
                    updateIfaceRatinfo(_arg08, _arg12);
                    return true;
                case 10:
                    data.enforceInterface(descriptor);
                    String _arg09 = data.readString();
                    int _arg13 = data.readInt();
                    setInterfaceThrottle(_arg09, _arg13, data.readInt());
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(descriptor);
                    int _arg010 = data.readInt();
                    int _arg14 = data.readInt();
                    _arg2 = data.readInt() != 0;
                    setFirewallUidChainRule(_arg010, _arg14, _arg2);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(descriptor);
                    String _arg011 = data.readString();
                    clearFirewallChain(_arg011);
                    reply.writeNoException();
                    return true;
                case 13:
                    data.enforceInterface(descriptor);
                    int _arg012 = data.readInt();
                    flushDnsCache(_arg012);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(descriptor);
                    String _arg013 = data.readString();
                    String _arg15 = data.readString();
                    addPolicyRoute(_arg013, _arg15, data.readString());
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface(descriptor);
                    int _arg014 = data.readInt();
                    String _arg16 = data.readString();
                    int _result = bindUidToNetwork(_arg014, _arg16);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 16:
                    data.enforceInterface(descriptor);
                    int _arg015 = data.readInt();
                    int _result2 = delBindUid(_arg015);
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 17:
                    data.enforceInterface(descriptor);
                    int _arg016 = data.readInt();
                    int _result3 = getUidBindedToNetwork(_arg016);
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case 18:
                    data.enforceInterface(descriptor);
                    String _arg017 = data.readString();
                    destroySocketsOnAddr(_arg017);
                    reply.writeNoException();
                    return true;
                case 19:
                    data.enforceInterface(descriptor);
                    String _arg018 = data.readString();
                    String _arg17 = data.readString();
                    int _result4 = setNetworkForbidRule(_arg018, _arg17);
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    return true;
                case 20:
                    data.enforceInterface(descriptor);
                    String _arg019 = data.readString();
                    String _arg18 = data.readString();
                    int _result5 = setNetworkAccessRuleForUid(_arg019, _arg18, data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result5);
                    return true;
                case 21:
                    data.enforceInterface(descriptor);
                    int _arg020 = data.readInt();
                    int _result6 = setAccelerationMode(_arg020);
                    reply.writeNoException();
                    reply.writeInt(_result6);
                    return true;
                case 22:
                    data.enforceInterface(descriptor);
                    int _arg021 = data.readInt();
                    int _arg19 = data.readInt();
                    String _arg22 = data.readString();
                    String _arg3 = data.readString();
                    int _result7 = setNetidGwForNetCoexist(_arg021, _arg19, _arg22, _arg3);
                    reply.writeNoException();
                    reply.writeInt(_result7);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IOemNetd {
            public static IOemNetd sDefaultImpl;
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

            @Override // com.android.internal.net.IOemNetd
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

            @Override // com.android.internal.net.IOemNetd
            public void registerOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().registerOemUnsolicitedEventListener(listener);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean firewallSetMobileUids(int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(uids);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().firewallSetMobileUids(uids);
                    }
                    _reply.readException();
                    boolean _status2 = _reply.readInt() != 0;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void firewallSetMobileUidRule(int uid, boolean allow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(allow ? 1 : 0);
                    boolean _status = this.mRemote.transact(4, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().firewallSetMobileUidRule(uid, allow);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean firewallSetWifiUids(int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(uids);
                    boolean _status = this.mRemote.transact(5, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().firewallSetWifiUids(uids);
                    }
                    _reply.readException();
                    boolean _status2 = _reply.readInt() != 0;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void firewallSetWifiUidRule(int uid, boolean allow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(allow ? 1 : 0);
                    boolean _status = this.mRemote.transact(6, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().firewallSetWifiUidRule(uid, allow);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean firewallSetUidForUserChain(int networkType, String chain, int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(networkType);
                    _data.writeString(chain);
                    _data.writeIntArray(uids);
                    boolean _status = this.mRemote.transact(7, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().firewallSetUidForUserChain(networkType, chain, uids);
                    }
                    _reply.readException();
                    boolean _status2 = _reply.readInt() != 0;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void setUidPackSeparate(int uid, boolean needSepar) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(needSepar ? 1 : 0);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setUidPackSeparate(uid, needSepar);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void updateIfaceRatinfo(String name, int rat) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(rat);
                    boolean _status = this.mRemote.transact(9, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().updateIfaceRatinfo(name, rat);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void setInterfaceThrottle(String interfaceName, int rxKbps, int txKbps) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(interfaceName);
                    _data.writeInt(rxKbps);
                    _data.writeInt(txKbps);
                    boolean _status = this.mRemote.transact(10, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setInterfaceThrottle(interfaceName, rxKbps, txKbps);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void setFirewallUidChainRule(int uid, int networkType, boolean allow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(networkType);
                    _data.writeInt(allow ? 1 : 0);
                    boolean _status = this.mRemote.transact(11, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setFirewallUidChainRule(uid, networkType, allow);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void clearFirewallChain(String chain) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chain);
                    boolean _status = this.mRemote.transact(12, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().clearFirewallChain(chain);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void flushDnsCache(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    boolean _status = this.mRemote.transact(13, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().flushDnsCache(netId);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void addPolicyRoute(String tableName, String detNetAddr, String devName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(tableName);
                    _data.writeString(detNetAddr);
                    _data.writeString(devName);
                    boolean _status = this.mRemote.transact(14, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().addPolicyRoute(tableName, detNetAddr, devName);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int bindUidToNetwork(int uid, String info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeString(info);
                    boolean _status = this.mRemote.transact(15, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().bindUidToNetwork(uid, info);
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int delBindUid(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(16, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().delBindUid(uid);
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int getUidBindedToNetwork(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(17, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getUidBindedToNetwork(uid);
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void destroySocketsOnAddr(String addrstr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(addrstr);
                    boolean _status = this.mRemote.transact(18, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().destroySocketsOnAddr(addrstr);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int setNetworkForbidRule(String operate, String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(operate);
                    _data.writeString(iface);
                    boolean _status = this.mRemote.transact(19, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().setNetworkForbidRule(operate, iface);
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int setNetworkAccessRuleForUid(String operate, String iface, String uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(operate);
                    _data.writeString(iface);
                    _data.writeString(uid);
                    boolean _status = this.mRemote.transact(20, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().setNetworkAccessRuleForUid(operate, iface, uid);
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int setAccelerationMode(int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mode);
                    boolean _status = this.mRemote.transact(21, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().setAccelerationMode(mode);
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int setNetidGwForNetCoexist(int state, int netid, String netseg, String mask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeInt(netid);
                    _data.writeString(netseg);
                    _data.writeString(mask);
                    boolean _status = this.mRemote.transact(22, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().setNetidGwForNetCoexist(state, netid, netseg, mask);
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IOemNetd impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IOemNetd getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}