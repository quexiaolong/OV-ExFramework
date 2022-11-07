package com.vivo.dr;

import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.vivo.dr.IDRClient;

/* loaded from: classes.dex */
public interface IDRService extends IInterface {
    void setListener(IDRClient iDRClient) throws RemoteException;

    void start() throws RemoteException;

    void stop() throws RemoteException;

    void updateDRConfig(int i, int i2) throws RemoteException;

    void updateDRConfig_v2(int i, int i2, boolean z) throws RemoteException;

    void updateLocationChanged(Location location) throws RemoteException;

    void updateNmeaChanged(String str, long j) throws RemoteException;

    void updateSatelliteStatusChanged(int i, int[] iArr, float[] fArr, float[] fArr2, float[] fArr3, float[] fArr4) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IDRService {
        @Override // com.vivo.dr.IDRService
        public void start() throws RemoteException {
        }

        @Override // com.vivo.dr.IDRService
        public void stop() throws RemoteException {
        }

        @Override // com.vivo.dr.IDRService
        public void setListener(IDRClient client) throws RemoteException {
        }

        @Override // com.vivo.dr.IDRService
        public void updateLocationChanged(Location location) throws RemoteException {
        }

        @Override // com.vivo.dr.IDRService
        public void updateSatelliteStatusChanged(int svCount, int[] prnWithFlags, float[] cn0s, float[] elevations, float[] azimuths, float[] carrierFreqs) throws RemoteException {
        }

        @Override // com.vivo.dr.IDRService
        public void updateNmeaChanged(String nmea, long time) throws RemoteException {
        }

        @Override // com.vivo.dr.IDRService
        public void updateDRConfig(int mode, int vdrMaxPredictTime) throws RemoteException {
        }

        @Override // com.vivo.dr.IDRService
        public void updateDRConfig_v2(int mode, int vdrMaxPredictTime, boolean dcStatus) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IDRService {
        private static final String DESCRIPTOR = "com.vivo.dr.IDRService";
        static final int TRANSACTION_setListener = 3;
        static final int TRANSACTION_start = 1;
        static final int TRANSACTION_stop = 2;
        static final int TRANSACTION_updateDRConfig = 7;
        static final int TRANSACTION_updateDRConfig_v2 = 8;
        static final int TRANSACTION_updateLocationChanged = 4;
        static final int TRANSACTION_updateNmeaChanged = 6;
        static final int TRANSACTION_updateSatelliteStatusChanged = 5;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDRService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IDRService)) {
                return (IDRService) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Location _arg0;
            if (code == 1598968902) {
                reply.writeString(DESCRIPTOR);
                return true;
            }
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    start();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    stop();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IDRClient _arg02 = IDRClient.Stub.asInterface(data.readStrongBinder());
                    setListener(_arg02);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = (Location) Location.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    updateLocationChanged(_arg0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    int[] _arg1 = data.createIntArray();
                    float[] _arg2 = data.createFloatArray();
                    float[] _arg3 = data.createFloatArray();
                    float[] _arg4 = data.createFloatArray();
                    float[] _arg5 = data.createFloatArray();
                    updateSatelliteStatusChanged(_arg03, _arg1, _arg2, _arg3, _arg4, _arg5);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    long _arg12 = data.readLong();
                    updateNmeaChanged(_arg04, _arg12);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    int _arg13 = data.readInt();
                    updateDRConfig(_arg05, _arg13);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    int _arg14 = data.readInt();
                    boolean _arg22 = data.readInt() != 0;
                    updateDRConfig_v2(_arg06, _arg14, _arg22);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IDRService {
            public static IDRService sDefaultImpl;
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

            @Override // com.vivo.dr.IDRService
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

            @Override // com.vivo.dr.IDRService
            public void stop() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().stop();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.dr.IDRService
            public void setListener(IDRClient client) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setListener(client);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.dr.IDRService
            public void updateLocationChanged(Location location) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (location != null) {
                        _data.writeInt(1);
                        location.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().updateLocationChanged(location);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.dr.IDRService
            public void updateSatelliteStatusChanged(int svCount, int[] prnWithFlags, float[] cn0s, float[] elevations, float[] azimuths, float[] carrierFreqs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeInt(svCount);
                    } catch (Throwable th) {
                        th = th;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeIntArray(prnWithFlags);
                    } catch (Throwable th2) {
                        th = th2;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeFloatArray(cn0s);
                    } catch (Throwable th3) {
                        th = th3;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
                try {
                    _data.writeFloatArray(elevations);
                    try {
                        _data.writeFloatArray(azimuths);
                        try {
                            _data.writeFloatArray(carrierFreqs);
                        } catch (Throwable th5) {
                            th = th5;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        boolean _status = this.mRemote.transact(5, _data, null, 1);
                        if (!_status && Stub.getDefaultImpl() != null) {
                            Stub.getDefaultImpl().updateSatelliteStatusChanged(svCount, prnWithFlags, cn0s, elevations, azimuths, carrierFreqs);
                            _data.recycle();
                            return;
                        }
                        _data.recycle();
                    } catch (Throwable th7) {
                        th = th7;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th8) {
                    th = th8;
                    _data.recycle();
                    throw th;
                }
            }

            @Override // com.vivo.dr.IDRService
            public void updateNmeaChanged(String nmea, long time) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(nmea);
                    _data.writeLong(time);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().updateNmeaChanged(nmea, time);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.dr.IDRService
            public void updateDRConfig(int mode, int vdrMaxPredictTime) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mode);
                    _data.writeInt(vdrMaxPredictTime);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().updateDRConfig(mode, vdrMaxPredictTime);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.dr.IDRService
            public void updateDRConfig_v2(int mode, int vdrMaxPredictTime, boolean dcStatus) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mode);
                    _data.writeInt(vdrMaxPredictTime);
                    _data.writeInt(dcStatus ? 1 : 0);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().updateDRConfig_v2(mode, vdrMaxPredictTime, dcStatus);
                    }
                } finally {
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IDRService impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IDRService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}