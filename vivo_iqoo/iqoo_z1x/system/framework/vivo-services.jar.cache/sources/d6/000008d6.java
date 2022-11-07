package com.vivo.services.vivolight;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class SensorUtil {
    private static final float COMPONENT_Z = 4.9f;
    private static final float COS_Z = 0.5f;
    private static final float GRAVITATIONAL_ACCELERATION = 9.8f;
    public static final int STATUS_NO = 1;
    public static final int STATUS_UNKNOW = -1;
    public static final int STATUS_YES = 0;
    private static final String TAG = "VivoLightManagerService";
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private boolean isRegisterProximitySensor;
    private Sensor mAccelerometerSensor;
    private Context mContext;
    private Sensor mProximitySensor;
    private float mProximityThreshold;
    private SensorManager mSensorManager;
    private final VivoLightManagerService mService;
    private int mProximityState = -1;
    private int mAccelerometerState = -1;
    private boolean isRegisterAccelerometerListener = false;
    private float mLastValueX = -9999.0f;
    private float mLastValueY = -9999.0f;
    private float mLastValueZ = -9999.0f;
    private SensorEventListener mProximityListener = new SensorEventListener() { // from class: com.vivo.services.vivolight.SensorUtil.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            float distance = event.values[0];
            boolean proximity = distance >= 0.0f && distance < SensorUtil.this.mProximityThreshold;
            int proximityState = proximity ? 0 : 1;
            if (proximityState != SensorUtil.this.mProximityState) {
                if (proximityState == 0) {
                    SensorUtil.this.setCoverMode(true);
                } else {
                    SensorUtil.this.setCoverMode(false);
                }
                SensorUtil.this.mProximityState = proximityState;
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private SensorEventListener mAccelerometerListener = new SensorEventListener() { // from class: com.vivo.services.vivolight.SensorUtil.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            SensorUtil.this.flipAnalysis(event.values[0], event.values[1], event.values[2]);
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public SensorUtil(Context context, VivoLightManagerService service) {
        this.mProximityThreshold = TYPICAL_PROXIMITY_THRESHOLD;
        this.mContext = context;
        this.mService = service;
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        if (sensorManager == null) {
            VLog.e("VivoLightManagerService", "mSensorManager is NULL!!!");
            return;
        }
        Sensor defaultSensor = sensorManager.getDefaultSensor(8);
        this.mProximitySensor = defaultSensor;
        if (defaultSensor != null) {
            this.mProximityThreshold = Math.min(defaultSensor.getMaximumRange(), (float) TYPICAL_PROXIMITY_THRESHOLD);
        } else {
            VLog.e("VivoLightManagerService", "mProximitySensor is null!");
            this.mProximityThreshold = Math.min(1.0f, (float) TYPICAL_PROXIMITY_THRESHOLD);
        }
        this.mAccelerometerSensor = this.mSensorManager.getDefaultSensor(9);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void flipAnalysis(float xValue, float yValue, float zValue) {
        if (this.mLastValueX != -9999.0f) {
            if (Math.abs(zValue) < COMPONENT_Z) {
                if (this.mAccelerometerState != 0) {
                    setAccidentalTouch(true);
                    this.mAccelerometerState = 0;
                }
            } else if (this.mAccelerometerState != 1) {
                setAccidentalTouch(false);
                this.mAccelerometerState = 1;
            }
        }
        this.mLastValueX = xValue;
        this.mLastValueY = yValue;
        this.mLastValueZ = zValue;
    }

    private void resetAccelerometerValue() {
        this.mLastValueX = -9999.0f;
        this.mLastValueY = -9999.0f;
        this.mLastValueZ = -9999.0f;
    }

    public void registerAccelerometerListener() {
        if (this.isRegisterAccelerometerListener) {
            return;
        }
        if (this.mSensorManager != null) {
            VLog.d("VivoLightManagerService", "registerAccelerometerListener");
            resetAccelerometerValue();
            this.mAccelerometerState = -1;
            this.isRegisterAccelerometerListener = this.mSensorManager.registerListener(this.mAccelerometerListener, this.mAccelerometerSensor, 3);
            return;
        }
        VLog.w("VivoLightManagerService", "registerProximitySensorListener sensorManager is null");
    }

    public void unregisterAccelerometerListener() {
        if (this.isRegisterAccelerometerListener && this.mSensorManager != null) {
            this.mAccelerometerState = -1;
            resetAccelerometerValue();
            VLog.d("VivoLightManagerService", "unregisterAccelerometerListener");
            this.mSensorManager.unregisterListener(this.mAccelerometerListener, this.mAccelerometerSensor);
            this.isRegisterAccelerometerListener = false;
        }
    }

    public void registerProximitySensorListener() {
        if (this.isRegisterProximitySensor) {
            return;
        }
        if (this.mSensorManager != null) {
            this.mProximityState = -1;
            VLog.d("VivoLightManagerService", "registerProximitySensorListener");
            this.isRegisterProximitySensor = this.mSensorManager.registerListener(this.mProximityListener, this.mProximitySensor, 3);
            return;
        }
        VLog.w("VivoLightManagerService", "registerProximitySensorListener sensorManager is null");
    }

    public void unregisterProximitySensorListener() {
        if (this.isRegisterProximitySensor && this.mSensorManager != null) {
            VLog.d("VivoLightManagerService", "unregisterLightSensorListener");
            this.mProximityState = -1;
            this.isRegisterProximitySensor = false;
            this.mSensorManager.unregisterListener(this.mProximityListener, this.mProximitySensor);
        }
    }

    public void setCoverMode(boolean isCover) {
        VLog.d("VivoLightManagerService", "setCoverMode " + isCover);
        if (isCover) {
            VivoLightManagerService vivoLightManagerService = this.mService;
            vivoLightManagerService.setCurrentState(vivoLightManagerService.getCurrentState() | 2);
        } else {
            VivoLightManagerService vivoLightManagerService2 = this.mService;
            vivoLightManagerService2.setCurrentState(vivoLightManagerService2.getCurrentState() & (-3));
        }
        this.mService.notifyUpdateLight(1000L);
    }

    public void setAccidentalTouch(boolean isAccidentalTouch) {
        VLog.d("VivoLightManagerService", "setAccidentalTouch " + isAccidentalTouch);
        if (isAccidentalTouch) {
            VivoLightManagerService vivoLightManagerService = this.mService;
            vivoLightManagerService.setCurrentState(vivoLightManagerService.getCurrentState() | 4);
        } else {
            VivoLightManagerService vivoLightManagerService2 = this.mService;
            vivoLightManagerService2.setCurrentState(vivoLightManagerService2.getCurrentState() & (-5));
        }
        this.mService.notifyUpdateLight(1000L);
    }
}