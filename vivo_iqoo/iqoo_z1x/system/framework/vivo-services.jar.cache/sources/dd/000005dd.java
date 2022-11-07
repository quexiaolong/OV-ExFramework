package com.vivo.face.internal.keyguard;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.KeyEvent;
import com.vivo.face.internal.keyguard.IKeyguardCallback;

/* loaded from: classes.dex */
public interface IFaceKeyguardService extends IInterface {
    void onAppStarted(String str, int i) throws RemoteException;

    void onBootCompleted() throws RemoteException;

    void onDisplayStateChanged(int i, int i2, int i3) throws RemoteException;

    void onFinishedGoingToSleep(int i, boolean z) throws RemoteException;

    void onFinishedWakingUp() throws RemoteException;

    void onKeyEvent(KeyEvent keyEvent) throws RemoteException;

    void onKeyguardMsgChanged(String str, String str2, String str3) throws RemoteException;

    void onKeyguardOccluded(boolean z) throws RemoteException;

    void onKeyguardShown(boolean z) throws RemoteException;

    void onScreenTurnedOff() throws RemoteException;

    void onScreenTurnedOn() throws RemoteException;

    void onScreenTurningOff() throws RemoteException;

    void onScreenTurningOn() throws RemoteException;

    void onStartedGoingToSleep(int i) throws RemoteException;

    void onStartedWakingUp() throws RemoteException;

    void onSystemReady() throws RemoteException;

    void setCurrentUser(int i) throws RemoteException;

    void setKeyguardCallback(IKeyguardCallback iKeyguardCallback) throws RemoteException;

    void unlockReason(int i) throws RemoteException;

    void wakeUpFromAiKey(long j) throws RemoteException;

    void wakeUpFromPowerKey(long j) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IFaceKeyguardService {
        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onAppStarted(String reason, int crashNum) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void setKeyguardCallback(IKeyguardCallback callback) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void setCurrentUser(int currentUser) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onBootCompleted() throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onSystemReady() throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onKeyguardShown(boolean shown) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onKeyguardOccluded(boolean occluded) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onKeyguardMsgChanged(String msgType, String msg, String extra) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onScreenTurningOff() throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onScreenTurnedOff() throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onScreenTurningOn() throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onScreenTurnedOn() throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onKeyEvent(KeyEvent event) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void wakeUpFromPowerKey(long eventTime) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void wakeUpFromAiKey(long eventTime) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onStartedWakingUp() throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onFinishedWakingUp() throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onStartedGoingToSleep(int why) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onFinishedGoingToSleep(int why, boolean cameraGestureTriggered) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void onDisplayStateChanged(int displayId, int state, int backlight) throws RemoteException {
        }

        @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
        public void unlockReason(int reason) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IFaceKeyguardService {
        private static final String DESCRIPTOR = "com.vivo.face.internal.keyguard.IFaceKeyguardService";
        static final int TRANSACTION_onAppStarted = 1;
        static final int TRANSACTION_onBootCompleted = 4;
        static final int TRANSACTION_onDisplayStateChanged = 20;
        static final int TRANSACTION_onFinishedGoingToSleep = 19;
        static final int TRANSACTION_onFinishedWakingUp = 17;
        static final int TRANSACTION_onKeyEvent = 13;
        static final int TRANSACTION_onKeyguardMsgChanged = 8;
        static final int TRANSACTION_onKeyguardOccluded = 7;
        static final int TRANSACTION_onKeyguardShown = 6;
        static final int TRANSACTION_onScreenTurnedOff = 10;
        static final int TRANSACTION_onScreenTurnedOn = 12;
        static final int TRANSACTION_onScreenTurningOff = 9;
        static final int TRANSACTION_onScreenTurningOn = 11;
        static final int TRANSACTION_onStartedGoingToSleep = 18;
        static final int TRANSACTION_onStartedWakingUp = 16;
        static final int TRANSACTION_onSystemReady = 5;
        static final int TRANSACTION_setCurrentUser = 3;
        static final int TRANSACTION_setKeyguardCallback = 2;
        static final int TRANSACTION_unlockReason = 21;
        static final int TRANSACTION_wakeUpFromAiKey = 15;
        static final int TRANSACTION_wakeUpFromPowerKey = 14;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IFaceKeyguardService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IFaceKeyguardService)) {
                return (IFaceKeyguardService) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            boolean _arg1;
            KeyEvent _arg0;
            if (code == 1598968902) {
                reply.writeString(DESCRIPTOR);
                return true;
            }
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    onAppStarted(_arg02, data.readInt());
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IKeyguardCallback _arg03 = IKeyguardCallback.Stub.asInterface(data.readStrongBinder());
                    setKeyguardCallback(_arg03);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    setCurrentUser(_arg04);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    onBootCompleted();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    onSystemReady();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    _arg1 = data.readInt() != 0;
                    onKeyguardShown(_arg1);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    _arg1 = data.readInt() != 0;
                    onKeyguardOccluded(_arg1);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    String _arg12 = data.readString();
                    String _arg2 = data.readString();
                    onKeyguardMsgChanged(_arg05, _arg12, _arg2);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    onScreenTurningOff();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    onScreenTurnedOff();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    onScreenTurningOn();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    onScreenTurnedOn();
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = (KeyEvent) KeyEvent.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    onKeyEvent(_arg0);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg06 = data.readLong();
                    wakeUpFromPowerKey(_arg06);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg07 = data.readLong();
                    wakeUpFromAiKey(_arg07);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    onStartedWakingUp();
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    onFinishedWakingUp();
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    onStartedGoingToSleep(_arg08);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    _arg1 = data.readInt() != 0;
                    onFinishedGoingToSleep(_arg09, _arg1);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    int _arg13 = data.readInt();
                    int _arg22 = data.readInt();
                    onDisplayStateChanged(_arg010, _arg13, _arg22);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg011 = data.readInt();
                    unlockReason(_arg011);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IFaceKeyguardService {
            public static IFaceKeyguardService sDefaultImpl;
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

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onAppStarted(String reason, int crashNum) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(reason);
                    _data.writeInt(crashNum);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onAppStarted(reason, crashNum);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void setKeyguardCallback(IKeyguardCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setKeyguardCallback(callback);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void setCurrentUser(int currentUser) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(currentUser);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setCurrentUser(currentUser);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onBootCompleted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onBootCompleted();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onSystemReady() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onSystemReady();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onKeyguardShown(boolean shown) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(shown ? 1 : 0);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onKeyguardShown(shown);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onKeyguardOccluded(boolean occluded) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(occluded ? 1 : 0);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onKeyguardOccluded(occluded);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onKeyguardMsgChanged(String msgType, String msg, String extra) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgType);
                    _data.writeString(msg);
                    _data.writeString(extra);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onKeyguardMsgChanged(msgType, msg, extra);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onScreenTurningOff() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(9, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onScreenTurningOff();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onScreenTurnedOff() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(10, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onScreenTurnedOff();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onScreenTurningOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(11, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onScreenTurningOn();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onScreenTurnedOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(12, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onScreenTurnedOn();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onKeyEvent(KeyEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (event != null) {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    boolean _status = this.mRemote.transact(13, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onKeyEvent(event);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void wakeUpFromPowerKey(long eventTime) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(eventTime);
                    boolean _status = this.mRemote.transact(14, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().wakeUpFromPowerKey(eventTime);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void wakeUpFromAiKey(long eventTime) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(eventTime);
                    boolean _status = this.mRemote.transact(15, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().wakeUpFromAiKey(eventTime);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onStartedWakingUp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(16, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onStartedWakingUp();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onFinishedWakingUp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(17, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onFinishedWakingUp();
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onStartedGoingToSleep(int why) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(why);
                    boolean _status = this.mRemote.transact(18, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onStartedGoingToSleep(why);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onFinishedGoingToSleep(int why, boolean cameraGestureTriggered) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(why);
                    _data.writeInt(cameraGestureTriggered ? 1 : 0);
                    boolean _status = this.mRemote.transact(19, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onFinishedGoingToSleep(why, cameraGestureTriggered);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void onDisplayStateChanged(int displayId, int state, int backlight) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(state);
                    _data.writeInt(backlight);
                    boolean _status = this.mRemote.transact(20, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().onDisplayStateChanged(displayId, state, backlight);
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.vivo.face.internal.keyguard.IFaceKeyguardService
            public void unlockReason(int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reason);
                    boolean _status = this.mRemote.transact(21, _data, null, 1);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().unlockReason(reason);
                    }
                } finally {
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IFaceKeyguardService impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IFaceKeyguardService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}