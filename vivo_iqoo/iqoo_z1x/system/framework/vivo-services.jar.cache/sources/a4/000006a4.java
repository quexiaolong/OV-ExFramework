package com.vivo.services.popupcamera;

import android.os.Handler;
import android.os.IHwBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import com.vivo.common.utils.VLog;
import java.util.NoSuchElementException;
import vendor.vivo.hardware.vibrator_hall.V1_0.IVibHallStatusCallback;
import vendor.vivo.hardware.vibrator_hall.V1_0.IVib_Hall;

/* loaded from: classes.dex */
public class VibHallWrapper {
    private static final String TAG = "PopupCameraManagerService";
    private static VibHallDeathRecipient sVibHallDeathRecipient = null;
    private static final long sVibHallDeathRecipientCookie = 100000;
    private static VibHallStatusCallback sVibHallStatusCallback;
    private static IVib_Hall sVibHallInstance = null;
    private static volatile boolean isVibHallDied = false;
    private static final Object mVibHallDeathLock = new Object();

    public static void initVibHallWrapper(Handler handler) {
        sVibHallDeathRecipient = new VibHallDeathRecipient(handler);
        sVibHallStatusCallback = new VibHallStatusCallback(handler);
    }

    private static IVib_Hall getVibHallService() {
        synchronized (mVibHallDeathLock) {
            if (sVibHallInstance == null) {
                try {
                    sVibHallInstance = IVib_Hall.getService();
                } catch (RemoteException e) {
                    sVibHallInstance = null;
                    e.printStackTrace();
                } catch (NoSuchElementException e1) {
                    sVibHallInstance = null;
                    e1.printStackTrace();
                }
                if (sVibHallInstance != null) {
                    if (isVibHallDied) {
                        VLog.d(TAG, "vib_hall service is restarted, get IVib_Hall again");
                    }
                    isVibHallDied = false;
                    try {
                        sVibHallInstance.registVibHallStatusCallback(sVibHallStatusCallback);
                    } catch (RemoteException e2) {
                        e2.printStackTrace();
                    }
                    try {
                        sVibHallInstance.linkToDeath(sVibHallDeathRecipient, sVibHallDeathRecipientCookie);
                    } catch (RemoteException e3) {
                        e3.printStackTrace();
                    }
                } else {
                    VLog.e(TAG, "IVib_Hall.getService() get error !!!");
                }
            }
        }
        return sVibHallInstance;
    }

    public static int openStepVibrator(int cookie) {
        VLog.d(TAG, "openStepVibrator cookie=" + cookie);
        IVib_Hall tmp = getVibHallService();
        if (tmp == null) {
            return -1;
        }
        try {
            return tmp.open_step_vib(cookie);
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int closeStepVibrator(int cookie) {
        VLog.d(TAG, "closeStepVibrator cookie=" + cookie);
        IVib_Hall tmp = getVibHallService();
        if (tmp == null) {
            return -1;
        }
        try {
            return tmp.close_step_vib(cookie);
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int closeStepVibratorAfterFalling() {
        VLog.d(TAG, "closeStepVibratorAfterFalling");
        IVib_Hall tmp = getVibHallService();
        if (tmp == null) {
            return -1;
        }
        try {
            return tmp.close_step_vib_after_falling();
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int notifyEvent(int event) {
        VLog.d(TAG, "notifyEvent event=" + event);
        IVib_Hall tmp = getVibHallService();
        if (tmp == null) {
            return -1;
        }
        try {
            return tmp.notify_event(event);
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int openHall() {
        IVib_Hall tmp = getVibHallService();
        if (tmp == null) {
            return -1;
        }
        try {
            return tmp.open_hall();
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int closeHall() {
        IVib_Hall tmp = getVibHallService();
        if (tmp == null) {
            return -1;
        }
        try {
            return tmp.close_hall();
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int getHallValue() {
        IVib_Hall tmp = getVibHallService();
        if (tmp == null) {
            return -1;
        }
        try {
            return tmp.get_hall_value();
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int registVibHallStatusCallback(IVibHallStatusCallback callback) {
        IVib_Hall tmp = getVibHallService();
        if (tmp == null) {
            return -1;
        }
        try {
            tmp.registVibHallStatusCallback(callback);
            return 1;
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class VibHallDeathRecipient implements IHwBinder.DeathRecipient {
        private Handler mHandler;

        public VibHallDeathRecipient(Handler handler) {
            this.mHandler = handler;
        }

        public void serviceDied(long cookie) {
            VLog.d(VibHallWrapper.TAG, "VibHall Died");
            if (cookie == VibHallWrapper.sVibHallDeathRecipientCookie && VibHallWrapper.sVibHallInstance != null) {
                synchronized (VibHallWrapper.mVibHallDeathLock) {
                    try {
                        VibHallWrapper.sVibHallInstance.unlinkToDeath(VibHallWrapper.sVibHallDeathRecipient);
                    } catch (RemoteException e) {
                        VLog.e(VibHallWrapper.TAG, "RemoteException : unable to unlink vibrator_hall DeathRecipient");
                    }
                    IVib_Hall unused = VibHallWrapper.sVibHallInstance = null;
                    boolean unused2 = VibHallWrapper.isVibHallDied = true;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class VibHallStatusCallback extends IVibHallStatusCallback.Stub {
        private Handler mHandler;

        public VibHallStatusCallback(Handler handler) {
            this.mHandler = handler;
        }

        private boolean isValidVibHallStatus(int status) {
            return status == 1 || status == 2 || status == 3 || status == 4 || status == 5 || status == 0;
        }

        private String getStatusTypeString(int status) {
            if (status != 0) {
                if (status != 1) {
                    if (status != 2) {
                        if (status != 3) {
                            if (status != 4) {
                                if (status == 5) {
                                    return "pressed";
                                }
                                return "invalid";
                            }
                            return "popup-jammed";
                        }
                        return "push-jammed";
                    }
                    return "popup-ok";
                }
                return "push-ok";
            }
            return "canceled";
        }

        @Override // vendor.vivo.hardware.vibrator_hall.V1_0.IVibHallStatusCallback
        public int onVibHallStatusChanged(int statusType, int extra, int cookie) {
            VLog.d(VibHallWrapper.TAG, "onVibHallStatusChanged statusType=" + getStatusTypeString(statusType) + " extra=" + extra + " cookie=" + cookie);
            SystemProperties.set("sys.vibhall.status", String.valueOf(statusType));
            if (this.mHandler != null && isValidVibHallStatus(statusType)) {
                Message msg = this.mHandler.obtainMessage(statusType);
                msg.arg1 = extra;
                msg.arg2 = cookie;
                this.mHandler.sendMessage(msg);
                return 1;
            }
            return 1;
        }
    }
}