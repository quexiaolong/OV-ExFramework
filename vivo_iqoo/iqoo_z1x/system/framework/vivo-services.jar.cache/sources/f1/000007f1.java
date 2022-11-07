package com.vivo.services.sarpower;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.samsung.slsi.telephony.aidl.ISarService;
import com.vivo.sensor.autobrightness.utils.SElog;
import com.vivo.sensor.sarpower.VivoSarConfig;
import java.io.PrintWriter;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class VivoSamsungSarPowerStateController extends VivoSarConfig {
    private static final int ACTION_SAR_POWER_MAX = 8;
    private static final int ACTION_SAR_POWER_MIN = 0;
    private static final byte DEFAULT_POWER_STATE = 0;
    private static final int MSG_SAR_POWER_CHANGE = 0;
    private static final String TAG = "SarPowerStateService";
    private static Context mContext;
    private static HandlerThread mThread;
    private static final String model = SystemProperties.get("ro.vivo.product.model", "unkown").toLowerCase();
    private int mLastOneNetworkType;
    private int mLastTwoNetworkType;
    private Looper mMainLooper;
    private PowerChangeHandler mPowerChangeHandler;
    private ISarService mSarService;
    private SarServiceConnection mSarServiceConnection;
    private TelephonyManager mTelephonyManager;

    public VivoSamsungSarPowerStateController(VivoSarPowerStateService service, Context context) {
        super(service);
        this.mSarService = null;
        this.mSarServiceConnection = null;
        this.mTelephonyManager = null;
        this.mLastOneNetworkType = -1;
        this.mLastTwoNetworkType = -1;
        mContext = context;
        HandlerThread handlerThread = new HandlerThread("SarPowerStateService_Samsung");
        mThread = handlerThread;
        handlerThread.start();
        this.mMainLooper = mThread.getLooper();
        this.mPowerChangeHandler = new PowerChangeHandler(this.mMainLooper);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SarServiceConnection implements ServiceConnection {
        SarServiceConnection() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            SElog.d(VivoSamsungSarPowerStateController.TAG, "onServiceConnected:" + name);
            SElog.d(VivoSamsungSarPowerStateController.TAG, "onServiceConnected:" + service);
            try {
                VivoSamsungSarPowerStateController.this.mSarService = ISarService.Stub.asInterface(service);
            } catch (Exception e) {
                SElog.e(VivoSamsungSarPowerStateController.TAG, "Exception", e);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            SElog.d(VivoSamsungSarPowerStateController.TAG, "onServiceDisconnected:" + name);
        }
    }

    private void startSarService() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.samsung.slsi.telephony.oemextservice", "com.samsung.slsi.telephony.oemextservice.SarService"));
        intent.setAction("com.samsung.slsi.telephony.oemextservice.SarService");
        this.mSarServiceConnection = new SarServiceConnection();
        SElog.d(TAG, "startSarService");
        mContext.bindService(intent, this.mSarServiceConnection, 1);
    }

    private void stopSarService() {
        try {
            if (this.mSarService != null) {
                SElog.d(TAG, "stopSarService");
                mContext.unbindService(this.mSarServiceConnection);
                this.mSarServiceConnection = null;
                this.mSarService = null;
            }
        } catch (Exception e) {
            SElog.e(TAG, "Exception", e);
        }
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public boolean initialPowerState() {
        startSarService();
        return true;
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void handleSarMessage(int sarMsg, int delayTimes) {
        if (sarMsg == 0) {
            this.mPowerChangeHandler.removeMessages(0);
            PowerChangeHandler powerChangeHandler = this.mPowerChangeHandler;
            powerChangeHandler.sendMessageDelayed(powerChangeHandler.obtainMessage(0), delayTimes);
        }
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void notifyServiceStateChanged() {
        int currentOneNetworkType = -1;
        int currentTwoNetworkType = -1;
        boolean changed = false;
        if (this.mTelephonyManager == null) {
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService("phone");
            this.mTelephonyManager = telephonyManager;
            if (telephonyManager == null) {
                SElog.e(TAG, "mTelephonyManager null return");
                return;
            }
        }
        try {
            int[] cardOneSubId = SubscriptionManager.getSubId(0);
            int[] cardTwoSubId = SubscriptionManager.getSubId(1);
            if (cardOneSubId.length > 0 && cardOneSubId[0] > 0) {
                currentOneNetworkType = this.mTelephonyManager.getDataNetworkType(cardOneSubId[0]);
            }
            if (cardTwoSubId.length > 0 && cardTwoSubId[0] > 0) {
                currentTwoNetworkType = this.mTelephonyManager.getDataNetworkType(cardTwoSubId[0]);
            }
            if (currentOneNetworkType > 0 && currentOneNetworkType != this.mLastOneNetworkType) {
                this.mLastOneNetworkType = currentOneNetworkType;
                changed = true;
            }
            if (currentTwoNetworkType > 0 && currentTwoNetworkType != this.mLastTwoNetworkType) {
                this.mLastTwoNetworkType = currentTwoNetworkType;
                changed = true;
            }
            if (changed) {
                handleSarMessage(0, 0);
            }
            SElog.d(TAG, "network one = " + currentOneNetworkType + " ,two = " + currentTwoNetworkType + ", changed: " + changed);
        } catch (Exception e) {
            SElog.e(TAG, "mTelephonyManager null return");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSarPowerState(int powerState, int cardNum) {
        ISarService iSarService = this.mSarService;
        if (iSarService == null) {
            SElog.e(TAG, "mSarService is null return");
            return;
        }
        try {
            iSarService.setState(powerState, cardNum);
        } catch (Exception e) {
            SElog.e(TAG, "setSarPowerState throws exception ");
            e.printStackTrace();
        }
        SystemProperties.set("sys.sar.dsi", String.valueOf(powerState));
        SElog.d(TAG, "setSarPowerState powerState = " + powerState + " ,card= " + cardNum);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class PowerChangeHandler extends Handler {
        public PowerChangeHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                byte powerState = VivoSamsungSarPowerStateController.this.processStateChange();
                SElog.d(VivoSamsungSarPowerStateController.TAG, "PowerChangeHandler power change, powerState = " + ((int) powerState) + ", mLastSarPowerState = " + VivoSamsungSarPowerStateController.this.mLastSarPowerState + ", mScreenState = " + VivoSamsungSarPowerStateController.this.mScreenState + ", mCardOneState = " + VivoSamsungSarPowerStateController.this.mCardOneState + ", mCardTwoState = " + VivoSamsungSarPowerStateController.this.mCardTwoState + ", mHasOneCard = " + VivoSamsungSarPowerStateController.this.mHasOneCard + ", mHasTwoCard = " + VivoSamsungSarPowerStateController.this.mHasTwoCard + ", mProximityState = " + VivoSamsungSarPowerStateController.this.mProximityState + ", mAudioState = " + VivoSamsungSarPowerStateController.this.mAudioState + ", mWIFIState = " + VivoSamsungSarPowerStateController.this.mWIFIState);
                if (VivoSamsungSarPowerStateController.this.mLastSarPowerState != powerState || VivoSamsungSarPowerStateController.this.mForceUpdateState) {
                    if (VivoSamsungSarPowerStateController.this.mForceUpdateState) {
                        SElog.d(VivoSamsungSarPowerStateController.TAG, "force update");
                        VivoSamsungSarPowerStateController.this.mForceUpdateState = false;
                    }
                    VivoSamsungSarPowerStateController.this.mLastSarPowerState = powerState;
                    if (!VivoSamsungSarPowerStateController.this.mHasOneCard || !VivoSamsungSarPowerStateController.this.mHasTwoCard) {
                        if (VivoSamsungSarPowerStateController.this.mHasTwoCard) {
                            VivoSamsungSarPowerStateController.this.setSarPowerState(powerState, 1);
                            return;
                        } else {
                            VivoSamsungSarPowerStateController.this.setSarPowerState(powerState, 0);
                            return;
                        }
                    }
                    VivoSamsungSarPowerStateController.this.setSarPowerState(powerState, 0);
                    VivoSamsungSarPowerStateController.this.setSarPowerState(powerState, 1);
                    return;
                }
                return;
            }
            SElog.d(VivoSamsungSarPowerStateController.TAG, "PowerChangeHandler default, the mProximityState is" + VivoSamsungSarPowerStateController.this.mProximityState);
        }
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void notifySarPowerTest(int powerStateValue) {
        if (powerStateValue <= 8 && powerStateValue >= 0) {
            byte powerState = (byte) powerStateValue;
            setSarPowerState(powerState, 1);
        }
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void notifyDsiToWifi(byte SarPowerStateToWifi) {
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void dump(PrintWriter pw) {
        pw.println(String.format("---- %s ----", TAG));
    }
}