package com.android.server.display.color;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import com.vivo.face.common.data.Constants;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLcmEsd {
    private static final String LCM_ESD_CHECK_EN_NODE = "/sys/lcm/vivo_esd_check_enable";
    private static final String LCM_ESD_CHECK_NODE = "/sys/lcm/vivo_esd_check_ps";
    private static final int MSG_POWER_OFF = 3;
    private static final int MSG_POWER_ON = 2;
    private static final int MSG_SENSOR_CHANGE = 1;
    private static final int SENSOR_TYPE_LCM_ESDCHECK = 66567;
    private static final String TAG = "VivoLcmEsd";
    private static final float TYPICAL_ACCELEROMETER_THRESHOLD = -5.0f;
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private Sensor mAccelerometerSensor;
    private float mAccelerometerThreshold;
    private final Context mContext;
    private int mInitResult;
    private Sensor mProximitySensor;
    private float mProximityThreshold;
    private static final boolean DEBUG = SystemProperties.getBoolean("debug.lcmesd.log", true);
    private static final String mProductModel = SystemProperties.get("ro.product.model.bbk", "nuknown");
    private static String mAlgoUnderStr = SystemProperties.get("persist.vivo.proximity", "null");
    private boolean mIsAlgoUnder = false;
    private boolean mCheckPs = false;
    private boolean mPowerOn = true;
    private Object mLock = new Object();
    private EsdHandler mEsdHandler = null;
    private HandlerThread mEsdThread = null;
    private SensorManager mSensorManager = null;
    private boolean mEsdEnable = false;
    private boolean mIsLimitAcc = false;
    private boolean mRegisterPro = false;
    private boolean mRegisterAcc = false;
    private boolean mNear = false;
    private boolean mDown = false;
    private SensorEventListener mProximityListener = new SensorEventListener() { // from class: com.android.server.display.color.VivoLcmEsd.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            boolean bNear;
            synchronized (VivoLcmEsd.this.mLock) {
                boolean positive = false;
                float distance = event.values[0];
                if (distance >= 0.0f && distance < VivoLcmEsd.this.mProximityThreshold) {
                    positive = true;
                }
                boolean unused = VivoLcmEsd.this.mNear;
                if (positive) {
                    bNear = true;
                } else {
                    bNear = false;
                }
                if (bNear != VivoLcmEsd.this.mNear) {
                    if (VivoLcmEsd.DEBUG) {
                        VSlog.d(VivoLcmEsd.TAG, "onSensorChanged change mNear from " + VivoLcmEsd.this.mNear + " to " + bNear);
                    }
                    VivoLcmEsd.this.mNear = bNear;
                    VivoLcmEsd.this.mEsdHandler.removeMessages(1);
                    VivoLcmEsd.this.mEsdHandler.sendEmptyMessage(1);
                    if (bNear) {
                        VivoLcmEsd.this.registerAccSensor();
                    } else {
                        VivoLcmEsd.this.unregisterAccSensor();
                    }
                }
                if (VivoLcmEsd.DEBUG) {
                    VSlog.d(VivoLcmEsd.TAG, "proximity positive is :" + positive);
                }
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private SensorEventListener mAccelerometerListener = new SensorEventListener() { // from class: com.android.server.display.color.VivoLcmEsd.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            boolean bDown;
            synchronized (VivoLcmEsd.this.mLock) {
                if (VivoLcmEsd.this.mIsLimitAcc) {
                    int x = (int) event.values[0];
                    bDown = x == 1;
                    VSlog.d(VivoLcmEsd.TAG, "onSensorChanged x=" + x);
                } else {
                    float f = event.values[0];
                    float f2 = event.values[1];
                    float z = event.values[2];
                    bDown = z < VivoLcmEsd.this.mAccelerometerThreshold;
                }
                if (bDown != VivoLcmEsd.this.mDown) {
                    if (VivoLcmEsd.DEBUG) {
                        VSlog.d(VivoLcmEsd.TAG, "onSensorChanged change mDown from " + VivoLcmEsd.this.mDown + " to " + bDown);
                    }
                    VivoLcmEsd.this.mDown = bDown;
                    VivoLcmEsd.this.mEsdHandler.removeMessages(1);
                    VivoLcmEsd.this.mEsdHandler.sendEmptyMessage(1);
                }
                if (VivoLcmEsd.DEBUG) {
                    VSlog.d(VivoLcmEsd.TAG, "accelerometer down is :" + bDown);
                }
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public VivoLcmEsd(Context context) {
        this.mInitResult = 0;
        if (DEBUG) {
            VSlog.d(TAG, "create VivoLcmEsd");
        }
        this.mContext = context;
        int init = init();
        this.mInitResult = init;
        if (init != 0) {
            VSlog.e(TAG, "VivoLcmEsd init failed");
        }
    }

    private int init() {
        Context context = this.mContext;
        if (context == null) {
            VSlog.e(TAG, "mAppContext is null");
            return -1;
        }
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        if (sensorManager == null) {
            VSlog.e(TAG, "mSensorManager is null");
            return -2;
        }
        String esd_en_str = VivoLcmUtils.readKernelNode(LCM_ESD_CHECK_EN_NODE);
        int iEsdCheckenable = 0;
        if (esd_en_str != null) {
            String esd_en_str2 = esd_en_str.replace("\n", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            int iLen = esd_en_str2.length() <= 2 ? esd_en_str2.length() : 2;
            iEsdCheckenable = Integer.parseInt(esd_en_str2.substring(0, iLen), 10);
        }
        if (iEsdCheckenable == 0) {
            VSlog.e(TAG, "This product have not enable esd check!");
            return -2;
        }
        if (mAlgoUnderStr.equals("algo_under")) {
            this.mIsAlgoUnder = true;
            if (DEBUG) {
                VSlog.d(TAG, "It's algo under (screen under the infrared)");
            }
        }
        int iRet = initSensors();
        if (iRet != 0) {
            VSlog.e(TAG, "initSensors failed");
            return iRet;
        }
        startEsdThread();
        onPowerOn();
        return 0;
    }

    public int deInit() {
        int iRet = 0;
        int i = this.mInitResult;
        if (i != 0) {
            return i;
        }
        HandlerThread handlerThread = this.mEsdThread;
        if (handlerThread != null) {
            handlerThread.quitSafely();
            try {
                this.mEsdThread.join();
            } catch (InterruptedException e) {
                VSlog.e(TAG, "InterruptedException:", e);
                iRet = -1;
            }
            this.mEsdThread = null;
        }
        setSensorStatus(false);
        return iRet;
    }

    public void onPowerOn() {
        EsdHandler esdHandler;
        if (DEBUG) {
            VSlog.d(TAG, "power on esd");
        }
        if (this.mInitResult == 0 && (esdHandler = this.mEsdHandler) != null) {
            esdHandler.removeMessages(1);
            this.mEsdHandler.sendEmptyMessage(2);
        }
    }

    public void onPowerOff() {
        EsdHandler esdHandler;
        if (DEBUG) {
            VSlog.d(TAG, "power off esd");
        }
        if (this.mInitResult == 0 && (esdHandler = this.mEsdHandler) != null) {
            esdHandler.removeMessages(1);
            this.mEsdHandler.sendEmptyMessage(3);
        }
    }

    private int initSensors() {
        if (!this.mIsAlgoUnder) {
            Sensor defaultSensor = this.mSensorManager.getDefaultSensor(8);
            this.mProximitySensor = defaultSensor;
            if (defaultSensor != null) {
                this.mProximityThreshold = Math.min(defaultSensor.getMaximumRange(), (float) TYPICAL_PROXIMITY_THRESHOLD);
                if (DEBUG) {
                    VSlog.d(TAG, "mProximityThreshold = " + this.mProximityThreshold);
                }
            } else {
                VSlog.e(TAG, "mProximitySensor is null");
                return -2;
            }
        }
        Sensor defaultSensor2 = this.mSensorManager.getDefaultSensor(SENSOR_TYPE_LCM_ESDCHECK);
        this.mAccelerometerSensor = defaultSensor2;
        if (defaultSensor2 != null) {
            this.mIsLimitAcc = true;
        } else {
            this.mIsLimitAcc = false;
            this.mAccelerometerSensor = this.mSensorManager.getDefaultSensor(1);
        }
        if (this.mAccelerometerSensor == null) {
            VSlog.e(TAG, "mAccelerometerSensor is null");
            return -2;
        }
        this.mAccelerometerThreshold = TYPICAL_ACCELEROMETER_THRESHOLD;
        if (DEBUG) {
            VSlog.d(TAG, "mIsLimitAcc=" + this.mIsLimitAcc + " mAccelerometerThreshold = " + this.mAccelerometerThreshold);
        }
        return 0;
    }

    private void startEsdThread() {
        if (DEBUG) {
            VSlog.d(TAG, "startEsdThread");
        }
        HandlerThread handlerThread = new HandlerThread("VivoESDCheck");
        this.mEsdThread = handlerThread;
        if (handlerThread != null) {
            handlerThread.start();
            this.mEsdHandler = new EsdHandler(this.mEsdThread.getLooper());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String msgToString(int msg) {
        if (msg != 1) {
            if (msg != 2) {
                if (msg == 3) {
                    return "MSG_POWER_OFF";
                }
                return "unkown";
            }
            return "MSG_POWER_ON";
        }
        return "MSG_LUX_CHANGE";
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class EsdHandler extends Handler {
        public EsdHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (VivoLcmEsd.DEBUG) {
                VSlog.d(VivoLcmEsd.TAG, "handleMessage " + VivoLcmEsd.msgToString(msg.what));
            }
            int i = msg.what;
            if (i == 1) {
                VivoLcmEsd.this.handleSensorChange();
            } else if (i == 2) {
                VivoLcmEsd.this.handlePowerOn();
            } else if (i == 3) {
                VivoLcmEsd.this.handlePowerOff();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePowerOn() {
        if (DEBUG) {
            VSlog.d(TAG, "handlePowerOn");
        }
        this.mNear = false;
        this.mDown = false;
        this.mPowerOn = true;
        setSensorStatus(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePowerOff() {
        if (DEBUG) {
            VSlog.d(TAG, "handlePowerOff");
        }
        this.mPowerOn = false;
        setSensorStatus(false);
        updateEsdToLcm();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSensorChange() {
        if (DEBUG) {
            VSlog.d(TAG, "handleSensorChange");
        }
        updateEsdToLcm();
    }

    private void updateEsdToLcm() {
        if (this.mCheckPs && ((!this.mIsAlgoUnder && !this.mNear) || !this.mDown || !this.mPowerOn)) {
            if (DEBUG) {
                VSlog.d(TAG, "Write /sys/lcm/vivo_esd_check_ps 0");
            }
            VivoLcmUtils.writeKernelNode(LCM_ESD_CHECK_NODE, "0");
            this.mCheckPs = false;
        }
        if (this.mIsAlgoUnder) {
            if (!this.mCheckPs && this.mDown && this.mPowerOn) {
                if (DEBUG) {
                    VSlog.d(TAG, "Write /sys/lcm/vivo_esd_check_ps 1");
                }
                VivoLcmUtils.writeKernelNode(LCM_ESD_CHECK_NODE, "1");
                this.mCheckPs = true;
            }
        } else if (!this.mCheckPs && this.mNear && this.mDown && this.mPowerOn) {
            if (DEBUG) {
                VSlog.d(TAG, "Write /sys/lcm/vivo_esd_check_ps 1");
            }
            VivoLcmUtils.writeKernelNode(LCM_ESD_CHECK_NODE, "1");
            this.mCheckPs = true;
        }
    }

    private synchronized boolean setSensorStatus(boolean enable) {
        if (DEBUG) {
            VSlog.d(TAG, "setSensorStatus mEsdEnable=" + this.mEsdEnable + " enable=" + enable + " mIsAlgoUnder=" + this.mIsAlgoUnder);
        }
        if (this.mEsdEnable != enable) {
            if (enable) {
                if (!this.mIsAlgoUnder) {
                    registerProSensor();
                } else {
                    registerAccSensor();
                }
                this.mEsdEnable = enable;
            } else {
                if (!this.mIsAlgoUnder) {
                    unregisterProSensor();
                }
                unregisterAccSensor();
                this.mEsdEnable = enable;
            }
        }
        return this.mEsdEnable;
    }

    private void registerProSensor() {
        if (DEBUG) {
            VSlog.d(TAG, "registerProSensor: register pro sensor mRegisterPro=" + this.mRegisterPro);
        }
        if (this.mRegisterPro) {
            if (DEBUG) {
                VSlog.d(TAG, "registerProSensor: already register pro sensor");
                return;
            }
            return;
        }
        this.mSensorManager.registerListener(this.mProximityListener, this.mProximitySensor, 1000000);
        this.mRegisterPro = true;
    }

    private void unregisterProSensor() {
        if (DEBUG) {
            VSlog.d(TAG, "unregisterProSensor: unregister pro sensor mRegisterPro=" + this.mRegisterPro);
        }
        if (!this.mRegisterPro) {
            if (DEBUG) {
                VSlog.d(TAG, "unregisterProSensor: already unregister pro sensor");
                return;
            }
            return;
        }
        this.mSensorManager.unregisterListener(this.mProximityListener);
        this.mRegisterPro = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerAccSensor() {
        if (DEBUG) {
            VSlog.d(TAG, "registerAccelerometerSensor: register acc sensor mRegisterAcc=" + this.mRegisterAcc);
        }
        if (this.mRegisterAcc) {
            if (DEBUG) {
                VSlog.d(TAG, "registerAccelerometerSensor: already register acc sensor");
                return;
            }
            return;
        }
        if (this.mIsLimitAcc) {
            this.mSensorManager.registerListener(this.mAccelerometerListener, this.mAccelerometerSensor, 1000000);
        } else {
            this.mSensorManager.registerListener(this.mAccelerometerListener, this.mAccelerometerSensor, 1000000);
        }
        this.mRegisterAcc = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterAccSensor() {
        if (DEBUG) {
            VSlog.d(TAG, "unregisterAccelerometerSensor: already unregister acc sensor mRegisterAcc=" + this.mRegisterAcc);
        }
        if (!this.mRegisterAcc) {
            if (DEBUG) {
                VSlog.d(TAG, "unregisterAccelerometerSensor: already unregister acc sensor");
                return;
            }
            return;
        }
        this.mSensorManager.unregisterListener(this.mAccelerometerListener);
        this.mRegisterAcc = false;
    }
}