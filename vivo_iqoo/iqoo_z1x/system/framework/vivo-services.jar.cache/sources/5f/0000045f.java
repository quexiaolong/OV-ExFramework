package com.android.server.policy.key;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.FtFeature;
import android.view.KeyEvent;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class VivoScreenClockKeyHandler extends AVivoInterceptKeyCallback {
    private static final int SENSOR_CHANGED_COUNT = 3;
    private static final String TAG = "VivoScreenClockKeyHandler";
    private Context mContext;
    private boolean mIsFeatureSupport;
    private SensorManager mSensorManager;
    private PowerManager.WakeLock mWakeLock;
    private boolean mIsSensorRegistered = false;
    private boolean mIsPhoneHeadStand = false;
    private volatile boolean mHasGdataRefresh = false;
    private volatile int mGDataCount = 0;
    private Runnable mStartNightPearlService = new Runnable() { // from class: com.android.server.policy.key.VivoScreenClockKeyHandler.1
        @Override // java.lang.Runnable
        public void run() {
            VivoScreenClockKeyHandler.this.startNightPearlService();
        }
    };
    private SensorEventListener mACCListener = new SensorEventListener() { // from class: com.android.server.policy.key.VivoScreenClockKeyHandler.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (VivoScreenClockKeyHandler.this.mGDataCount < 3) {
                VivoScreenClockKeyHandler.access$108(VivoScreenClockKeyHandler.this);
            }
            if (!VivoScreenClockKeyHandler.this.mHasGdataRefresh && VivoScreenClockKeyHandler.this.mGDataCount == 3) {
                VLog.d(VivoScreenClockKeyHandler.TAG, "x:" + x + " y:" + y + "z:" + z);
                if (y < -6.0f) {
                    VivoScreenClockKeyHandler.this.mIsPhoneHeadStand = true;
                } else {
                    VivoScreenClockKeyHandler.this.mIsPhoneHeadStand = false;
                }
                VivoScreenClockKeyHandler.this.mHasGdataRefresh = true;
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            VLog.d(VivoScreenClockKeyHandler.TAG, "accuracy changed " + accuracy);
        }
    };
    private Handler mHandler = new Handler();

    static /* synthetic */ int access$108(VivoScreenClockKeyHandler x0) {
        int i = x0.mGDataCount;
        x0.mGDataCount = i + 1;
        return i;
    }

    public VivoScreenClockKeyHandler(Context context) {
        this.mIsFeatureSupport = false;
        this.mContext = context;
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, TAG);
        this.mIsFeatureSupport = FtFeature.isFeatureSupport("vivo.software.nightpearl");
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        return false;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        if (this.mState != 0) {
            return -1;
        }
        if (!this.mIsFeatureSupport) {
            VLog.d(TAG, "feature not support");
            return -1;
        }
        this.mHasGdataRefresh = false;
        this.mWakeLock.acquire();
        registerAccSensor();
        int waitCount = 0;
        while (!this.mHasGdataRefresh && waitCount < 40) {
            waitCount++;
            SystemClock.sleep(5L);
        }
        this.mWakeLock.release();
        unregisterAccSensor();
        if (waitCount == 40) {
            VLog.d(TAG, "wait sensor data timeout");
        }
        if (this.mIsPhoneHeadStand) {
            VLog.d(TAG, "Phone is headstand, drop key");
            return -1;
        }
        this.mHandler.post(this.mStartNightPearlService);
        return 0;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        int i = this.mState;
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startNightPearlService() {
        try {
            VLog.d(TAG, "startNightPearlService");
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.vivo.nightpearl", "com.vivo.nightpearl.NightPearlShowService"));
            intent.putExtra("msg", 5);
            this.mContext.startService(intent);
        } catch (Exception e) {
            VLog.e(TAG, "start nightpearl service failed : " + e);
        }
    }

    private void registerAccSensor() {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null) {
            return;
        }
        Sensor accSensor = sensorManager.getDefaultSensor(1);
        if (accSensor == null) {
            VLog.d(TAG, "no accelerometer sensor");
            return;
        }
        this.mIsSensorRegistered = true;
        this.mGDataCount = 0;
        VLog.d(TAG, "register sensor");
        this.mSensorManager.registerListener(this.mACCListener, accSensor, 0);
    }

    private void unregisterAccSensor() {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null || !this.mIsSensorRegistered) {
            VLog.d(TAG, "no sensor registered");
            return;
        }
        sensorManager.unregisterListener(this.mACCListener);
        this.mIsSensorRegistered = false;
    }
}