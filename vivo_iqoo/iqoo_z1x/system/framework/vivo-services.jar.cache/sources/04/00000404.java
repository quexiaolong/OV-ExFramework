package com.android.server.policy;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class VivoProximitySensorListener {
    private static final boolean IS_LOG_OPEN = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
    private static final String KEY_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    private static final int MSG_PROXIMITY_SENSOR_DEBOUNCED = 1;
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private static final int PROXIMITY_SENSOR_NEGATIVE_DEBOUNCE_DELAY = 400;
    private static final int PROXIMITY_SENSOR_POSITIVE_DEBOUNCE_DELAY = 0;
    private static final int PROXIMITY_UNKNOWN = -1;
    private static final String TAG = "VivoProximitySensorListener";
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 1.5f;
    private Callbacks mCallbacks;
    private Context mContext;
    private boolean mEnabled;
    private Handler mHandler;
    private final Object mLock;
    private int mPendingProximity;
    private long mPendingProximityDebounceTime;
    private int mProximity;
    private final SensorEventListener mProximitySensorListener;
    private float mProximityThreshold;
    private int mRate;
    private Sensor mSensor;
    private SensorManager mSensorManager;

    /* loaded from: classes.dex */
    public interface Callbacks {
        void onProximityNegative();

        void onProximityPositive();
    }

    public VivoProximitySensorListener(Context context, Looper looper, Callbacks callbacks) {
        this(context, looper, callbacks, 3);
    }

    public VivoProximitySensorListener(Context context, Looper looper, Callbacks callbacks, int rate) {
        this.mLock = new Object();
        this.mContext = null;
        this.mHandler = null;
        this.mCallbacks = null;
        this.mSensorManager = null;
        this.mEnabled = false;
        this.mRate = 3;
        this.mSensor = null;
        this.mProximityThreshold = TYPICAL_PROXIMITY_THRESHOLD;
        this.mProximity = -1;
        this.mPendingProximity = -1;
        this.mProximitySensorListener = new SensorEventListener() { // from class: com.android.server.policy.VivoProximitySensorListener.1
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                synchronized (VivoProximitySensorListener.this.mLock) {
                    if (VivoProximitySensorListener.this.mEnabled) {
                        long time = SystemClock.uptimeMillis();
                        boolean positive = false;
                        float distance = event.values[0];
                        if (distance >= 0.0f && distance < VivoProximitySensorListener.this.mProximityThreshold) {
                            positive = true;
                        }
                        VivoProximitySensorListener.this.handleProximitySensorEvent(time, positive);
                    }
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mContext = context;
        this.mHandler = new MessageHandler(looper);
        this.mCallbacks = callbacks;
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mRate = rate;
        Sensor defaultSensor = sensorManager.getDefaultSensor(8);
        this.mSensor = defaultSensor;
        if (defaultSensor != null) {
            this.mProximityThreshold = Math.min(defaultSensor.getMaximumRange(), (float) TYPICAL_PROXIMITY_THRESHOLD);
        }
    }

    public void enable() {
        synchronized (this.mLock) {
            if (this.mSensor == null) {
                VLog.w(TAG, "Cannot detect sensors. Not enabled");
                return;
            }
            if (!this.mEnabled) {
                printf("Enable VivoProximitySensorListener");
                this.mEnabled = true;
                this.mPendingProximity = -1;
                this.mSensorManager.registerListener(this.mProximitySensorListener, this.mSensor, this.mRate, this.mHandler);
            }
        }
    }

    public void disable() {
        synchronized (this.mLock) {
            if (this.mSensor == null) {
                VLog.w(TAG, "Cannot detect sensors. Invalid disable");
                return;
            }
            if (this.mEnabled) {
                printf("Disable VivoProximitySensorListener");
                this.mEnabled = false;
                this.mProximity = -1;
                this.mHandler.removeMessages(1);
                this.mSensorManager.unregisterListener(this.mProximitySensorListener);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleProximitySensorEvent(long time, boolean positive) {
        if (this.mProximity == 0 && !positive) {
            return;
        }
        if (this.mProximity == 1 && positive) {
            this.mHandler.removeMessages(1);
            return;
        }
        if (positive) {
            this.mPendingProximity = 1;
            this.mPendingProximityDebounceTime = 0 + time;
        } else {
            this.mPendingProximity = 0;
            this.mPendingProximityDebounceTime = 400 + time;
        }
        debounceProximitySensor();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void debounceProximitySensor() {
        if (this.mPendingProximity != -1) {
            long now = SystemClock.uptimeMillis();
            if (this.mPendingProximityDebounceTime <= now) {
                int i = this.mPendingProximity;
                this.mProximity = i;
                if (i == 1) {
                    printf("PROXIMITY_POSITIVE");
                    this.mCallbacks.onProximityPositive();
                    return;
                } else if (i == 0) {
                    printf("PROXIMITY_NEGATIVE");
                    this.mCallbacks.onProximityNegative();
                    return;
                } else {
                    VLog.e(TAG, "Invalid mProximity=" + this.mProximity);
                    return;
                }
            }
            Message msg = this.mHandler.obtainMessage(1);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageAtTime(msg, this.mPendingProximityDebounceTime);
        }
    }

    private void printf(String msg) {
        if (IS_LOG_OPEN) {
            VLog.d(TAG, msg);
        }
    }

    /* loaded from: classes.dex */
    private final class MessageHandler extends Handler {
        public MessageHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                VivoProximitySensorListener.this.debounceProximitySensor();
            }
        }
    }
}