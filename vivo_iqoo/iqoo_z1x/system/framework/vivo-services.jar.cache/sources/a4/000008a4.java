package com.vivo.services.vcodehaltransfer;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.IHwBinder;
import com.vivo.common.utils.VLog;
import com.vivo.vcodetransbase.EventTransfer;
import java.util.ArrayList;
import vendor.vivo.hardware.vcode.V1_0.IEventTransfer;
import vendor.vivo.hardware.vcode.V1_0.IVcode;
import vivo.app.vcodehaltransfer.IVivoVcodeHalTransferService;

/* loaded from: classes.dex */
public final class HalEventTransfer extends IVivoVcodeHalTransferService.Stub {
    private static final long MAX_WAIT_COUNT = 600;
    private static final long PERIOD_WAIT_TIME = 2000;
    private static final String TAG = "VCode/HalEventTransfer";
    private static final long VCODE_HAL_DEATH_COOKIE = 110120119;
    private static final Object mLock = new Object();
    private final Runnable mConnectRunnable;
    private final Handler mHandler;
    private IVcode mProxy;
    private final HalCodeReceiver mReceiver;
    private int mWaitCount;

    private HalEventTransfer() {
        this.mWaitCount = 0;
        this.mProxy = null;
        HandlerThread ht = new HandlerThread("halCode");
        ht.start();
        this.mReceiver = new HalCodeReceiver();
        this.mHandler = new Handler(ht.getLooper());
        this.mConnectRunnable = new Runnable() { // from class: com.vivo.services.vcodehaltransfer.HalEventTransfer.1
            @Override // java.lang.Runnable
            public void run() {
                HalEventTransfer.this.tryConnect();
            }
        };
    }

    public static HalEventTransfer getInstance() {
        return Holder.INSTANCE;
    }

    private boolean connectToProxy() {
        IVcode service;
        synchronized (mLock) {
            if (this.mProxy != null) {
                return true;
            }
            try {
                service = IVcode.getService();
                this.mProxy = service;
            } catch (Exception e) {
                VLog.e(TAG, "registerReceiver failed", e);
                this.mProxy = null;
            }
            if (service != null) {
                service.linkToDeath(new DeathRecipient(), VCODE_HAL_DEATH_COOKIE);
                this.mProxy.registerReceiver(this.mReceiver);
                VLog.i(TAG, "registerReceiver success");
                return true;
            }
            return false;
        }
    }

    public void start() {
        if (!this.mHandler.hasCallbacks(this.mConnectRunnable)) {
            this.mHandler.postDelayed(this.mConnectRunnable, PERIOD_WAIT_TIME);
        }
    }

    private void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tryConnect() {
        while (true) {
            int i = this.mWaitCount;
            this.mWaitCount = i + 1;
            if (i < MAX_WAIT_COUNT) {
                if (connectToProxy()) {
                    return;
                }
                delay(PERIOD_WAIT_TIME);
            } else {
                VLog.e(TAG, "skip registerReceiver for hal vcode, try 1200000");
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Holder {
        private static final HalEventTransfer INSTANCE = new HalEventTransfer();

        private Holder() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class HalCodeReceiver extends IEventTransfer.Stub {
        HalCodeReceiver() {
        }

        @Override // vendor.vivo.hardware.vcode.V1_0.IEventTransfer
        public void trackEvent(final String moduleId, final String eventId, final String params) {
            if (moduleId != null && eventId != null && params != null) {
                synchronized (HalEventTransfer.mLock) {
                    if (HalEventTransfer.this.mProxy == null) {
                        VLog.e(HalEventTransfer.TAG, "<trackEventData> is not registered");
                        return;
                    }
                    VLog.d(HalEventTransfer.TAG, "<trackEvent> moduleId:" + moduleId + ", eventId:" + eventId + ", params:" + params);
                    HalEventTransfer.this.mHandler.post(new Runnable() { // from class: com.vivo.services.vcodehaltransfer.HalEventTransfer.HalCodeReceiver.1
                        @Override // java.lang.Runnable
                        public void run() {
                            try {
                                EventTransfer.getInstance().simpleEvent(moduleId, eventId, params);
                            } catch (IllegalArgumentException e) {
                            }
                        }
                    });
                    return;
                }
            }
            VLog.e(HalEventTransfer.TAG, "<trackEventData> invalid parameters");
        }

        @Override // vendor.vivo.hardware.vcode.V1_0.IEventTransfer
        public void trackEventData(final String moduleId, final String eventId, final String fn, final ArrayList<Byte> data, final int size) {
            if (moduleId != null && eventId != null && fn != null && size >= 0 && data != null && data.size() == size) {
                synchronized (HalEventTransfer.mLock) {
                    if (HalEventTransfer.this.mProxy == null) {
                        VLog.e(HalEventTransfer.TAG, "<trackEventData> is not registered");
                        return;
                    }
                    VLog.d(HalEventTransfer.TAG, "<trackEvent> moduleId: " + moduleId + ", eventId: " + eventId + ", fn: " + fn + ", size: " + size);
                    HalEventTransfer.this.mHandler.post(new Runnable() { // from class: com.vivo.services.vcodehaltransfer.HalEventTransfer.HalCodeReceiver.2
                        @Override // java.lang.Runnable
                        public void run() {
                            try {
                                byte[] data2 = new byte[size];
                                for (int i = 0; i < size; i++) {
                                    data2[i] = ((Byte) data.get(i)).byteValue();
                                }
                                EventTransfer.getInstance().dataEvent(moduleId, eventId, fn, data2, size);
                            } catch (IllegalArgumentException e) {
                            }
                        }
                    });
                    return;
                }
            }
            VLog.e(HalEventTransfer.TAG, "<trackEventData> invalid parameters");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class DeathRecipient implements IHwBinder.DeathRecipient {
        DeathRecipient() {
        }

        public void serviceDied(long cookie) {
            if (cookie == HalEventTransfer.VCODE_HAL_DEATH_COOKIE) {
                VLog.w(HalEventTransfer.TAG, "Vcode hal service died cookie:" + cookie);
                synchronized (HalEventTransfer.mLock) {
                    HalEventTransfer.this.mProxy = null;
                    HalEventTransfer.this.start();
                }
            }
        }
    }
}