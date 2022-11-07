package com.vivo.services.sensorhub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import com.vivo.common.utils.VLog;
import com.vivo.sensor.implement.VivoSensorImpl;
import com.vivo.sensor.sensoroperate.SensorTestResult;
import com.vivo.sensor.sensoroperate.SensorTestResultCallback;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class SensorHubRestart {
    private static final String ACTION_DAY_START_CLOCK_TIMER = "android.intent.action.auto_bright.day.start";
    private static final int MSG_CHECK_SENSORHUB = 2;
    private static final int MSG_REGISTER_BROADCAST = 1;
    private static final int MSG_RESET_SENSORHUB = 3;
    private static SensorRestartHandler mHandler;
    private static HandlerThread thread;
    private Sensor lightSensor;
    private Context mContext;
    private IntentFilter mIntentFilter;
    private SensorManager mSensorManager;
    private VivoSensorImpl mVivoImpl;
    private String TAG = "SensorHubRestart";
    private final BroadcastReceiver mBroadcastReceiver = new SensorHubReceiver();
    private int alsAbnormalCount = 0;
    private boolean displayON = true;
    private long resetTs = -1;
    private int resetCount = 0;
    private SensorTestResultCallback mGetAlsCallback = new SensorTestResultCallback() { // from class: com.vivo.services.sensorhub.SensorHubRestart.1
        public void operateResult(SensorTestResult result) {
            if (result.mSuccess == 0) {
                String str = SensorHubRestart.this.TAG;
                VLog.i(str, "AbNormalAlsCount = " + SensorHubRestart.this.alsAbnormalCount);
                SensorHubRestart.this.handleAbNormalAls();
                return;
            }
            SensorHubRestart.this.handleNormalAls();
        }
    };

    public SensorHubRestart(Context context) {
        this.mContext = context;
        this.mVivoImpl = VivoSensorImpl.getInstance(context);
        getLightSensor();
        startHandler();
        registerReceiver();
    }

    private void getLightSensor() {
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.lightSensor = sensorManager.getDefaultSensor(5);
    }

    private void startHandler() {
        HandlerThread handlerThread = new HandlerThread("SensorHubRestart");
        thread = handlerThread;
        handlerThread.start();
        mHandler = new SensorRestartHandler(thread.getLooper());
    }

    private void registerReceiver() {
        mHandler.removeMessages(1);
        mHandler.sendEmptyMessageDelayed(1, 5000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SensorRestartHandler extends Handler {
        public SensorRestartHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                SensorHubRestart.this.registerBroadcast();
            } else if (i != 2) {
                if (i == 3 && SensorHubRestart.this.lightSensor != null) {
                    SensorHubRestart.this.resetSensorHub();
                }
            } else if (SensorHubRestart.this.lightSensor != null) {
                SensorHubRestart.this.checkSensorHubStatus();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        this.mIntentFilter = intentFilter;
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        this.mIntentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mIntentFilter.addAction(ACTION_DAY_START_CLOCK_TIMER);
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkSensorHubStatus() {
        if (!isNeedCheckAls()) {
            VLog.i(this.TAG, "restart sensor hub over 5 or interval is too shot");
        } else {
            checkAlsStatus();
        }
    }

    private boolean isNeedCheckAls() {
        if (this.resetCount > 10) {
            return false;
        }
        return this.resetTs == -1 || SystemClock.elapsedRealtime() - this.resetTs >= ((long) 1800000);
    }

    private void checkAlsStatus() {
        int[] cmd = new int[3];
        SensorTestResult result = new SensorTestResult();
        this.mVivoImpl.vivoSensorTest(201, result, cmd, 0L, this.mGetAlsCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAbNormalAls() {
        int i = this.alsAbnormalCount + 1;
        this.alsAbnormalCount = i;
        if (i > 5) {
            this.alsAbnormalCount = 0;
            mHandler.removeMessages(3);
            mHandler.sendEmptyMessage(3);
            return;
        }
        mHandler.removeMessages(2);
        mHandler.sendEmptyMessage(2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNormalAls() {
        this.alsAbnormalCount = 0;
        mHandler.removeMessages(2);
        mHandler.sendEmptyMessageDelayed(2, 15000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetSensorHub() {
        int[] cmd = new int[3];
        SensorTestResult result = new SensorTestResult();
        this.mVivoImpl.vivoSensorTest(544, result, cmd, 0L);
        this.resetCount++;
        this.resetTs = SystemClock.elapsedRealtime();
        VLog.i(this.TAG, "resetSensorHub!");
    }

    /* loaded from: classes.dex */
    private final class SensorHubReceiver extends BroadcastReceiver {
        private SensorHubReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            String str = SensorHubRestart.this.TAG;
            VLog.d(str, "broadcast = " + action);
            if (action.equals("android.intent.action.SCREEN_ON")) {
                SensorHubRestart.this.startCheckLooper();
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                SensorHubRestart.this.cancelCheckLooper();
            } else if (action.equals(SensorHubRestart.ACTION_DAY_START_CLOCK_TIMER)) {
                SensorHubRestart.this.resetCount = 0;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startCheckLooper() {
        mHandler.removeMessages(2);
        mHandler.sendEmptyMessageDelayed(2, 15000L);
        if (!this.displayON) {
            this.displayON = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelCheckLooper() {
        mHandler.removeMessages(2);
        if (this.displayON) {
            this.displayON = false;
        }
    }
}