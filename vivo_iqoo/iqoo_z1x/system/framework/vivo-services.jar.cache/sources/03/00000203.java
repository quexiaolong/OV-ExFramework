package com.android.server.display.color;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.FtFeature;
import com.android.server.am.VivoFrozenPackageSupervisor;
import com.android.server.am.frozen.FrozenQuicker;
import com.android.server.display.VivoDisplayPowerControllerImpl;
import com.android.server.display.color.ColorDisplayService;
import com.vivo.services.proxy.broadcast.BroadcastConfigs;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.rms.display.DisplayConfigsManager;
import java.io.PrintWriter;
import java.util.Vector;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoAutoColorTempController extends TintController {
    private static final int CCT_MAX_VALUE = 11000;
    private static final String LCM_CONFIG_PATH = "/system/etc/LcmConfig/LcmConfig.json";
    public static final int LCM_FAILED = -1;
    public static final int LCM_SUCCESS = 0;
    public static final int LCM_UNSUPPORT = -2;
    private static final int MSG_ACC_CHANGE = 4;
    private static final int MSG_ACC_ENABLE_CHANGE = 5;
    private static final int MSG_CCT_FACTOR_CHANGE = 7;
    private static final int MSG_CCT_RGB_CHANGE = 8;
    private static final int MSG_POWER_OFF = 3;
    private static final int MSG_POWER_ON = 2;
    private static final int MSG_PROX_CHANGE = 6;
    private static final int NOT_SET = -1;
    static final String TAG = "VivoAutoColorTempController";
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final String VIVO_AUTO_EYE_CCT_ENABLE = "vivo_auto_eye_cct_enable";
    private static final String VIVO_AUTO_EYE_CCT_VALUE = "vivo_auto_eye_cct_value";
    private static final String VIVO_COLOR_TEMPERATURE_CHANGE = "vivo_color_temperature_change";
    private static final String VIVO_DEBUG_CCT_B = "vivo_debug_cct_b";
    private static final String VIVO_DEBUG_CCT_FACTOR = "vivo_debug_cct_factor";
    private static final String VIVO_DEBUG_CCT_G = "vivo_debug_cct_g";
    private static final String VIVO_DEBUG_CCT_R = "vivo_debug_cct_r";
    private static final int VIVO_EYE_CARE_MODE = 1;
    private static final String VIVO_NIGHT_DISPLAY_ANIMATION = "vivo_night_display_animation";
    private static final int VIVO_NORMAL_MODE = 0;
    private static final String VIVO_TEMP_COLOR_B = "persist.vivo.temp_color.b";
    private static final String VIVO_TEMP_COLOR_G = "persist.vivo.temp_color.g";
    private static final String VIVO_TEMP_COLOR_R = "persist.vivo.temp_color.r";
    private static String mAlgoUnderStr = SystemProperties.get("persist.vivo.proximity", "null");
    private static VivoAutoColorTempController mAutoColorTempController = null;
    private ActHandler mActHandler;
    private HandlerThread mActThread;
    private ColorDisplayService mColorDisplayService;
    private ContentObserver mContentObserver;
    private final Context mContext;
    private Sensor mLightSensor;
    private Sensor mProximitySensor;
    private float mProximityThreshold;
    private ScreenStatusReceiver mScreenStatusReceiver;
    private final String mProductModel = SystemProperties.get("ro.product.model.bbk", "unkown").toLowerCase();
    private float mLightThreshold = Float.parseFloat(SystemProperties.get("persist.sys.light_threshold", "500"));
    private boolean mIsAlgoUnder = mAlgoUnderStr.equals("algo_under");
    private boolean mNear = false;
    private Boolean mIsAvailable = Boolean.valueOf(FtFeature.isFeatureSupport("vivo.hardware.autoeyecct"));
    private final float[] mMatrix = new float[16];
    private final float[] MATRIX_IDENTITY = new float[16];
    private SensorManager mSensorManager = null;
    private boolean mEnableLightSensor = false;
    private boolean mEnableProxSensor = false;
    private Object mLock = new Object();
    private int mInitResult = 0;
    private int mCurrentUser = ProcessList.INVALID_ADJ;
    private boolean mPowerOn = false;
    private Object mPowerLock = new Object();
    private boolean mEnableListenPower = false;
    private Vector<Double> mCctMap = new Vector<>(0);
    private int VIVO_CCT_BOUND = 0;
    private int mMaxCctLevel = 1000;
    private int mCurLevel = -1;
    private int mLastAccLevel = -1;
    private int[][] mTempColor = {new int[]{1, 0, 255, 248, 217}, new int[]{2, 3500, 255, 248, 220}, new int[]{3, VivoDisplayPowerControllerImpl.COLOR_FADE_ANIMATION_GLOBAL, 255, 248, 222}, new int[]{4, 4500, 255, 250, 227}, new int[]{5, FrozenQuicker.FREEZE_STATUS_CHECK_MS, 255, 252, DisplayConfigsManager.DisplayMode.BIT_AUTO_MODE_1HZ_BRIGHTNESS}, new int[]{6, 5500, 255, 252, 236}, new int[]{7, 6000, 255, 252, BroadcastConfigs.PROXY_BR_ABNORMAL_SIZE}, new int[]{8, 6500, 255, 255, 243}, new int[]{9, 7000, 255, 255, 247}, new int[]{10, 7500, 255, 254, 249}, new int[]{11, 8000, 255, 255, 252}, new int[]{12, 8500, 255, 255, 254}, new int[]{13, 9000, VivoFrozenPackageSupervisor.FLAG_FROZEN_DEFAULT, 255, 254}, new int[]{14, 9500, VivoFrozenPackageSupervisor.FLAG_FROZEN_DEFAULT, 255, 255}, new int[]{15, 10000, 251, 254, 255}, new int[]{16, 10500, 250, 254, 255}, new int[]{17, CCT_MAX_VALUE, 255, 255, 255}};
    private int mIndex = 0;
    private final int CCT_UP_DELAY = 500;
    private final int CCT_DOWN_DELAY = 1000;
    private final int CCT_DEBOUNCE_TIME = 500;
    private final int CCT_UP_CCT_STEP = ProcessList.HEAVY_WEIGHT_APP_ADJ;
    private final int CCT_DOWN_CCT_STEP = ProcessList.HEAVY_WEIGHT_APP_ADJ;
    private long mDebounceTime = 0;
    private int mDebouncedCct = -1;
    private SensorEventListener mSensorEventListener = new SensorEventListener() { // from class: com.android.server.display.color.VivoAutoColorTempController.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            synchronized (VivoAutoColorTempController.this.mLock) {
                if (event.values.length >= 7) {
                    int lux = VivoAutoColorTempController.this.getRectifiedLux(event.values[0]);
                    VSlog.d(VivoAutoColorTempController.TAG, "onSensorChanged: lux =" + lux);
                    if (lux <= 50) {
                        VSlog.e(VivoAutoColorTempController.TAG, "onSensorChanged: lower lux=" + lux);
                        return;
                    }
                    int iCct = (int) event.values[6];
                    VSlog.d(VivoAutoColorTempController.TAG, "onSensorChanged: iCct=" + iCct);
                    if (iCct < 0) {
                        VSlog.d(VivoAutoColorTempController.TAG, "onSensorChanged: invalid cct");
                        return;
                    }
                    if (iCct > VivoAutoColorTempController.CCT_MAX_VALUE) {
                        iCct = VivoAutoColorTempController.CCT_MAX_VALUE;
                        VSlog.d(VivoAutoColorTempController.TAG, "onSensorChanged: limit cct to " + VivoAutoColorTempController.CCT_MAX_VALUE);
                    }
                    if (VivoAutoColorTempController.this.mLastAccLevel == -1) {
                        VivoAutoColorTempController.this.mDebouncedCct = iCct;
                    } else {
                        long currentTime = System.currentTimeMillis();
                        long debounceTime = VivoAutoColorTempController.this.mDebounceTime + 500;
                        VSlog.d(VivoAutoColorTempController.TAG, "onSensorChanged: debounceTime=" + debounceTime + " currentTime=" + currentTime);
                        boolean bUp = false;
                        boolean bDown = false;
                        if (iCct > VivoAutoColorTempController.this.mDebouncedCct) {
                            int debounceCct = iCct - VivoAutoColorTempController.this.mDebouncedCct;
                            if (debounceCct >= 400) {
                                bUp = true;
                            }
                        } else if (iCct < VivoAutoColorTempController.this.mDebouncedCct) {
                            int debounceCct2 = VivoAutoColorTempController.this.mDebouncedCct - iCct;
                            if (debounceCct2 >= 400) {
                                bDown = true;
                            }
                        } else {
                            VSlog.d(VivoAutoColorTempController.TAG, "onSensorChanged: same cct");
                            return;
                        }
                        if (currentTime < debounceTime) {
                            return;
                        }
                        VivoAutoColorTempController.this.mDebounceTime = currentTime;
                        if (!bUp && !bDown) {
                            return;
                        }
                        VivoAutoColorTempController.this.mDebouncedCct = iCct;
                    }
                    int level = 0;
                    for (int j = 0; j < VivoAutoColorTempController.this.mTempColor.length; j++) {
                        if (VivoAutoColorTempController.this.mDebouncedCct < VivoAutoColorTempController.this.mTempColor[j][1] || ((j >= VivoAutoColorTempController.this.mTempColor.length - 1 || VivoAutoColorTempController.this.mDebouncedCct >= VivoAutoColorTempController.this.mTempColor[j + 1][1]) && j != VivoAutoColorTempController.this.mTempColor.length - 1)) {
                            if (VivoAutoColorTempController.this.mDebouncedCct < VivoAutoColorTempController.this.mTempColor[j][1]) {
                                break;
                            }
                        } else {
                            level = VivoAutoColorTempController.this.mTempColor[j][0];
                            break;
                        }
                    }
                    VSlog.d(VivoAutoColorTempController.TAG, "onSensorChanged: mDebouncedCct = " + VivoAutoColorTempController.this.mDebouncedCct + " level=" + level);
                    VivoAutoColorTempController.this.onAccChange(level);
                    return;
                }
                VSlog.e(VivoAutoColorTempController.TAG, "onSensorChanged: invalid length=" + event.values.length);
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private SensorEventListener mProximityListener = new SensorEventListener() { // from class: com.android.server.display.color.VivoAutoColorTempController.3
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            synchronized (VivoAutoColorTempController.this.mLock) {
                boolean bNear = false;
                float distance = event.values[0];
                if (distance >= 0.0f && distance < VivoAutoColorTempController.this.mProximityThreshold) {
                    bNear = true;
                }
                VSlog.d(VivoAutoColorTempController.TAG, "onSensorChanged distance=" + distance + " bNear=" + bNear);
                VivoAutoColorTempController.this.onProxChange(bNear);
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public /* bridge */ /* synthetic */ void cancelAnimator() {
        super.cancelAnimator();
    }

    public /* bridge */ /* synthetic */ void dump(PrintWriter printWriter) {
        super.dump(printWriter);
    }

    public /* bridge */ /* synthetic */ void endAnimator() {
        super.endAnimator();
    }

    public /* bridge */ /* synthetic */ ColorDisplayService.TintValueAnimator getAnimator() {
        return super.getAnimator();
    }

    public /* bridge */ /* synthetic */ int getData() {
        return super.getData();
    }

    public /* bridge */ /* synthetic */ boolean isActivated() {
        return super.isActivated();
    }

    public /* bridge */ /* synthetic */ boolean isActivatedStateNotSet() {
        return super.isActivatedStateNotSet();
    }

    public /* bridge */ /* synthetic */ void setAnimator(ColorDisplayService.TintValueAnimator tintValueAnimator) {
        super.setAnimator(tintValueAnimator);
    }

    public /* bridge */ /* synthetic */ void setData(int i) {
        super.setData(i);
    }

    private VivoAutoColorTempController(ColorDisplayService colorDisplayService, Context context) {
        this.mContext = context;
        this.mColorDisplayService = colorDisplayService;
        init();
    }

    public static synchronized VivoAutoColorTempController getInstance(ColorDisplayService colorDisplayService, Context context) {
        VivoAutoColorTempController vivoAutoColorTempController;
        synchronized (VivoAutoColorTempController.class) {
            if (mAutoColorTempController == null) {
                mAutoColorTempController = new VivoAutoColorTempController(colorDisplayService, context);
            }
            vivoAutoColorTempController = mAutoColorTempController;
        }
        return vivoAutoColorTempController;
    }

    public static synchronized VivoAutoColorTempController getExistInstance() {
        VivoAutoColorTempController vivoAutoColorTempController;
        synchronized (VivoAutoColorTempController.class) {
            vivoAutoColorTempController = mAutoColorTempController;
        }
        return vivoAutoColorTempController;
    }

    private int init() {
        if (!this.mIsAvailable.booleanValue()) {
            VSlog.e(TAG, "vivo.hardware.autoeyecct is disable");
            this.mInitResult = -2;
            return -2;
        }
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        if (sensorManager == null) {
            VSlog.e(TAG, "mSensorManager is null");
            this.mInitResult = -1;
            return -1;
        }
        Sensor lightSensor = getLightSensor();
        this.mLightSensor = lightSensor;
        if (lightSensor == null) {
            VSlog.e(TAG, "light sensor not support");
            this.mInitResult = -2;
            return -2;
        }
        Sensor defaultSensor = this.mSensorManager.getDefaultSensor(8);
        this.mProximitySensor = defaultSensor;
        if (defaultSensor != null) {
            this.mProximityThreshold = Math.min(defaultSensor.getMaximumRange(), (float) TYPICAL_PROXIMITY_THRESHOLD);
            VSlog.d(TAG, "mProximityThreshold = " + this.mProximityThreshold);
            startActThread();
            return this.mInitResult;
        }
        VSlog.e(TAG, "mProximitySensor is null");
        this.mInitResult = -2;
        return -2;
    }

    private int deInit() {
        if (!this.mIsAvailable.booleanValue()) {
            VSlog.e(TAG, "vivo.hardware.autoeyecct is disable");
            this.mInitResult = -2;
            return -2;
        }
        sendAccEnableMsg(false);
        int quitActThread = quitActThread();
        this.mInitResult = quitActThread;
        return quitActThread;
    }

    private void initDefaultStatus() {
        Matrix.setIdentityM(this.MATRIX_IDENTITY, 0);
        Matrix.setIdentityM(this.mMatrix, 0);
        setActivated(null);
        synchronized (this.mLock) {
            this.mCurLevel = -1;
            this.mLastAccLevel = -1;
            this.mDebouncedCct = -1;
            this.mDebounceTime = 0L;
        }
    }

    public void onStartUser(int userHandle) {
        if (this.mInitResult != 0 || !this.mIsAvailable.booleanValue()) {
            return;
        }
        this.mCurrentUser = userHandle;
    }

    public void onSetUser(int userHandle) {
        VSlog.d(TAG, "onSetUser: userHandle=" + userHandle);
        if (this.mInitResult != 0 || !this.mIsAvailable.booleanValue()) {
            return;
        }
        this.mCurrentUser = userHandle;
    }

    public void setUp(Context context, boolean needsLinear) {
        VSlog.d(TAG, "setUp");
        if (this.mInitResult != 0 || !this.mIsAvailable.booleanValue()) {
            return;
        }
        initDefaultStatus();
        boolean bAutoEyeEnable = isAccessiblityAutoEyeCctEnabled();
        setActivated(Boolean.valueOf(bAutoEyeEnable));
        if (this.mContentObserver == null) {
            this.mContentObserver = new ContentObserver(this.mColorDisplayService.mHandler) { // from class: com.android.server.display.color.VivoAutoColorTempController.1
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    String setting = uri == null ? null : uri.getLastPathSegment();
                    VSlog.d(VivoAutoColorTempController.TAG, "onChange: setting=" + setting);
                    if (setting != null) {
                        char c = 65535;
                        switch (setting.hashCode()) {
                            case -750604455:
                                if (setting.equals(VivoAutoColorTempController.VIVO_AUTO_EYE_CCT_ENABLE)) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case -318931208:
                                if (setting.equals(VivoAutoColorTempController.VIVO_DEBUG_CCT_B)) {
                                    c = 4;
                                    break;
                                }
                                break;
                            case -318931203:
                                if (setting.equals(VivoAutoColorTempController.VIVO_DEBUG_CCT_G)) {
                                    c = 3;
                                    break;
                                }
                                break;
                            case -318931192:
                                if (setting.equals(VivoAutoColorTempController.VIVO_DEBUG_CCT_R)) {
                                    c = 2;
                                    break;
                                }
                                break;
                            case 1598826617:
                                if (setting.equals(VivoAutoColorTempController.VIVO_DEBUG_CCT_FACTOR)) {
                                    c = 1;
                                    break;
                                }
                                break;
                        }
                        if (c == 0) {
                            VivoAutoColorTempController.this.onAutoEyeCctEnableChanged();
                        } else if (c == 1) {
                            VivoAutoColorTempController.this.onCctFactorChanged();
                        } else if (c == 2 || c == 3 || c == 4) {
                            VivoAutoColorTempController.this.onCctRgbChanged();
                        }
                    }
                }
            };
        }
        ContentResolver cr = context.getContentResolver();
        if (this.mIsAvailable.booleanValue()) {
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_AUTO_EYE_CCT_ENABLE), false, this.mContentObserver, this.mCurrentUser);
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_DEBUG_CCT_FACTOR), false, this.mContentObserver, this.mCurrentUser);
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_DEBUG_CCT_R), false, this.mContentObserver, this.mCurrentUser);
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_DEBUG_CCT_G), false, this.mContentObserver, this.mCurrentUser);
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_DEBUG_CCT_B), false, this.mContentObserver, this.mCurrentUser);
        }
    }

    public void tearDown() {
        VSlog.d(TAG, "tearDown: currentUser=" + this.mCurrentUser);
        if (this.mInitResult != 0 || !this.mIsAvailable.booleanValue()) {
            return;
        }
        sendAccEnableMsg(false);
        if (this.mContentObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
        }
    }

    private boolean isColorTempActivated() {
        return this.mCurrentUser == -10000 || Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "night_display_activated", 0, this.mCurrentUser) == 0;
    }

    private double[] getCctColorTempFactor(int level) {
        double[] retVal = {1.0d, 1.0d, 1.0d};
        if (level > 0) {
            int[][] iArr = this.mTempColor;
            if (level <= iArr.length) {
                retVal[0] = iArr[level - 1][2] / 255.0d;
                retVal[1] = iArr[level - 1][3] / 255.0d;
                retVal[2] = iArr[level - 1][4] / 255.0d;
                VSlog.d(TAG, "getCctColorTempFactor level=" + level + " r=" + retVal[0] + " g=" + retVal[1] + " b=" + retVal[2]);
                return retVal;
            }
        }
        VSlog.d(TAG, "getCctColorTempFactor invalid level=" + level);
        return retVal;
    }

    private int updateDebugCctProperty(float[] matrix) {
        if (matrix.length < 16) {
            return -1;
        }
        VSlog.d(TAG, "updateDebugCctProperty r=" + matrix[0] + " g=" + matrix[5] + " b=" + matrix[10]);
        SystemProperties.set(VIVO_TEMP_COLOR_R, String.format("%.2f", Float.valueOf(matrix[0])));
        SystemProperties.set(VIVO_TEMP_COLOR_G, String.format("%.2f", Float.valueOf(matrix[5])));
        SystemProperties.set(VIVO_TEMP_COLOR_B, String.format("%.2f", Float.valueOf(matrix[10])));
        return 0;
    }

    private double[] getColorTempRGBFactor(int factor) {
        double ratio_r;
        double ratio_g;
        double ratio_b;
        double factor_g;
        double[] retVal = {0.0d, 0.0d, 0.0d};
        int min_colortemp_r = SystemProperties.getInt("persist.sys.autocct.minred", 255);
        double factor_r = 1.0d;
        int min_colortemp_g = SystemProperties.getInt("persist.sys.autocct.mingreen", 241);
        int min_colortemp_b = SystemProperties.getInt("persist.sys.autocct.minblue", 198);
        int max_colortemp_r = SystemProperties.getInt("persist.sys.autocct.maxred", 238);
        int max_colortemp_g = SystemProperties.getInt("persist.sys.autocct.maxgreen", 243);
        double factor_b = 1.0d;
        int max_colortemp_b = SystemProperties.getInt("persist.sys.autocct.maxblue", 255);
        if (factor < 0 || factor > 127) {
            ratio_r = 1.0d;
            ratio_g = 1.0d;
            ratio_b = 1.0d;
            if (factor > 127 && factor <= 255) {
                double factor_r2 = (255.0d - (((255.0d - min_colortemp_r) / 127.0d) * (factor - 128))) / 255.0d;
                double factor_g2 = (255.0d - (((255.0d - min_colortemp_g) / 127.0d) * (factor - 128))) / 255.0d;
                factor_r = factor_r2;
                factor_g = factor_g2;
                factor_b = (255.0d - (((255.0d - min_colortemp_b) / 127.0d) * (factor - 128))) / 255.0d;
            } else {
                factor_g = 1.0d;
            }
        } else {
            ratio_b = 1.0d;
            ratio_g = 1.0d;
            ratio_r = 1.0d;
            double factor_r3 = (max_colortemp_r + (((255.0d - max_colortemp_r) / 127.0d) * factor)) / 255.0d;
            double factor_g3 = (max_colortemp_g + (((255.0d - max_colortemp_g) / 127.0d) * factor)) / 255.0d;
            double factor_g4 = factor;
            factor_b = (max_colortemp_b + (((255.0d - max_colortemp_b) / 127.0d) * factor_g4)) / 255.0d;
            factor_g = factor_g3;
            factor_r = factor_r3;
        }
        retVal[0] = factor_r * ratio_r;
        retVal[1] = factor_g * ratio_g;
        retVal[2] = factor_b * ratio_b;
        return retVal;
    }

    public void setMatrix(int level) {
        float[] fArr = this.mMatrix;
        if (fArr.length != 16) {
            VSlog.d(TAG, "The display transformation matrix must be 4x4");
        } else if (this.mCurLevel == level) {
            VSlog.d(TAG, "Ignore same level=" + level);
        } else {
            Matrix.setIdentityM(fArr, 0);
            double[] dArr = {0.0d, 0.0d, 0.0d};
            double[] RGB = getCctColorTempFactor(level);
            this.mCurLevel = level;
            float[] fArr2 = this.mMatrix;
            fArr2[0] = (float) RGB[0];
            fArr2[5] = (float) RGB[1];
            fArr2[10] = (float) RGB[2];
            updateDebugCctProperty(fArr2);
            VSlog.d(TAG, "setMatrix mMatrix R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
        }
    }

    private void setFactorMatrix(int value) {
        if (this.mMatrix.length != 16) {
            VSlog.d(TAG, "The display transformation matrix must be 4x4");
            return;
        }
        VSlog.d(TAG, "setFactorMatrix value=" + value);
        Matrix.setIdentityM(this.mMatrix, 0);
        double[] dArr = {0.0d, 0.0d, 0.0d};
        double[] RGB = getColorTempRGBFactor(value);
        float[] fArr = this.mMatrix;
        fArr[0] = (float) RGB[0];
        fArr[5] = (float) RGB[1];
        fArr[10] = (float) RGB[2];
        updateDebugCctProperty(fArr);
        VSlog.d(TAG, "setFactorMatrix mMatrix R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
    }

    private void setRgbMatrix(int r, int g, int b) {
        if (this.mMatrix.length != 16) {
            VSlog.d(TAG, "The display transformation matrix must be 4x4");
            return;
        }
        VSlog.d(TAG, "setRgbMatrix r=" + r + " g=" + g + " b=" + b);
        Matrix.setIdentityM(this.mMatrix, 0);
        double[] RGB = {((double) r) / 255.0d, ((double) g) / 255.0d, ((double) b) / 255.0d};
        float[] fArr = this.mMatrix;
        fArr[0] = (float) RGB[0];
        fArr[5] = (float) RGB[1];
        fArr[10] = (float) RGB[2];
        updateDebugCctProperty(fArr);
        VSlog.d(TAG, "setRgbMatrix mMatrix R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
    }

    public float[] getMatrix() {
        VSlog.d(TAG, "getMatrix autoCctMatrix active=" + isActivated() + " R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
        return isActivated() ? this.mMatrix : this.MATRIX_IDENTITY;
    }

    private int getCctSetting() {
        return this.mCurLevel;
    }

    public void setActivated(Boolean activated) {
        if (activated == null) {
            super.setActivated((Boolean) null);
            return;
        }
        boolean activationStateChanged = activated.booleanValue() != isActivated();
        if (isActivatedStateNotSet() || activationStateChanged) {
            super.setActivated(activated);
            VSlog.d(TAG, activated.booleanValue() ? "Turning on auto color temperature" : "Turning off auto color temperature");
            if (activated.booleanValue()) {
                if (isColorTempActivated()) {
                    VSlog.d(TAG, "close color temp");
                    updateColorTemperature();
                }
                enableSensorListen();
                enablePowerListen();
                synchronized (this.mLock) {
                    this.mCurLevel = -1;
                    this.mLastAccLevel = -1;
                    this.mDebouncedCct = -1;
                    this.mDebounceTime = 0L;
                }
                setMatrix(0);
            } else {
                disableSensorListen();
                disablePowerListen();
                updateDebugCctProperty(this.MATRIX_IDENTITY);
                if (isColorTempActivated()) {
                    VSlog.d(TAG, "enable color temp");
                    updateColorTemperature();
                }
                applyColorMatrix(true);
            }
            VSlog.d(TAG, "setActivated auto cct R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
        }
    }

    private void updateColorTemperature() {
        int iLastState = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), VIVO_COLOR_TEMPERATURE_CHANGE, 0, this.mCurrentUser);
        int iChange = iLastState == 0 ? 1 : 0;
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), VIVO_COLOR_TEMPERATURE_CHANGE, iChange, this.mCurrentUser);
    }

    private void enableSensorListen() {
        enableLightSensorListen();
        enableProxSensorListen();
    }

    private void disableSensorListen() {
        disableLightSensorListen();
        disableProxSensorListen();
    }

    private void enableLightSensorListen() {
        if (!this.mEnableLightSensor) {
            this.mSensorManager.registerListener(this.mSensorEventListener, this.mLightSensor, 2);
            this.mEnableLightSensor = true;
            this.mNear = false;
        }
    }

    private void disableLightSensorListen() {
        if (this.mEnableLightSensor) {
            this.mSensorManager.unregisterListener(this.mSensorEventListener);
            this.mEnableLightSensor = false;
        }
    }

    private void enableProxSensorListen() {
        if (!this.mEnableProxSensor) {
            this.mSensorManager.registerListener(this.mProximityListener, this.mProximitySensor, 2);
            this.mEnableProxSensor = true;
            this.mNear = false;
        }
    }

    private void disableProxSensorListen() {
        if (this.mEnableProxSensor) {
            this.mSensorManager.unregisterListener(this.mProximityListener);
            this.mEnableProxSensor = false;
        }
    }

    public int getLevel() {
        return KernelConfig.N2M_ENABLE;
    }

    public boolean isAvailable(Context context) {
        return this.mIsAvailable.booleanValue();
    }

    private int getDebugCctFactor() {
        if (this.mCurrentUser == -10000) {
            this.mCurrentUser = -2;
        }
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_DEBUG_CCT_FACTOR, 0, this.mCurrentUser);
    }

    private int getDebugRed() {
        if (this.mCurrentUser == -10000) {
            this.mCurrentUser = -2;
        }
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_DEBUG_CCT_R, 255, this.mCurrentUser);
    }

    private int getDebugGreen() {
        if (this.mCurrentUser == -10000) {
            this.mCurrentUser = -2;
        }
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_DEBUG_CCT_G, 255, this.mCurrentUser);
    }

    private int getDebugBlue() {
        if (this.mCurrentUser == -10000) {
            this.mCurrentUser = -2;
        }
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_DEBUG_CCT_B, 255, this.mCurrentUser);
    }

    private boolean isAccessiblityAutoEyeCctEnabled() {
        if (this.mCurrentUser == -10000) {
            this.mCurrentUser = -2;
        }
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_AUTO_EYE_CCT_ENABLE, 0, this.mCurrentUser) != 0;
    }

    private void applyColorMatrix(boolean bAnimated) {
        int i;
        VSlog.d(TAG, "applyColorMatrix bAnimated=" + bAnimated);
        Handler handler = this.mColorDisplayService.mHandler;
        if (bAnimated) {
            i = 15;
        } else {
            i = 14;
        }
        handler.sendEmptyMessage(i);
    }

    private Sensor getLightSensor() {
        return this.mSensorManager.getDefaultSensor(5);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getRectifiedLux(float light) {
        float rectifiedLux = light;
        float f = this.mLightThreshold;
        if (f > 0.0f) {
            rectifiedLux = (500.0f * light) / f;
        }
        return Math.round(rectifiedLux);
    }

    private void startActThread() {
        HandlerThread handlerThread = new HandlerThread("VivoActThread");
        this.mActThread = handlerThread;
        if (handlerThread != null) {
            handlerThread.start();
            this.mActHandler = new ActHandler(this.mActThread.getLooper());
        }
    }

    private int quitActThread() {
        int iRet = 0;
        HandlerThread handlerThread = this.mActThread;
        if (handlerThread != null) {
            handlerThread.quitSafely();
            try {
                this.mActThread.join();
            } catch (InterruptedException e) {
                VSlog.e(TAG, "InterruptedException:", e);
                iRet = -1;
            }
            this.mActThread = null;
        }
        return iRet;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String msgToString(int msg) {
        switch (msg) {
            case 2:
                return "MSG_POWER_ON";
            case 3:
                return "MSG_POWER_OFF";
            case 4:
                return "MSG_ACC_CHANGE";
            case 5:
                return "MSG_ACC_ENABLE_CHANGE";
            case 6:
                return "MSG_PROX_CHANGE";
            case 7:
                return "MSG_CCT_FACTOR_CHANGE";
            case 8:
                return "MSG_CCT_RGB_CHANGE";
            default:
                return "unkown";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ActHandler extends Handler {
        public ActHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            VSlog.d(VivoAutoColorTempController.TAG, "handleMessage " + VivoAutoColorTempController.msgToString(msg.what));
            switch (msg.what) {
                case 2:
                    VivoAutoColorTempController.this.handlePowerOn();
                    return;
                case 3:
                    VivoAutoColorTempController.this.handlePowerOff();
                    return;
                case 4:
                    VivoAutoColorTempController.this.handleAccChange(msg);
                    return;
                case 5:
                    VivoAutoColorTempController.this.handleAccEnableChange(msg);
                    return;
                case 6:
                    VivoAutoColorTempController.this.handleProxChange(msg);
                    return;
                case 7:
                    VivoAutoColorTempController.this.handleCctFactorChange(msg);
                    return;
                case 8:
                    VivoAutoColorTempController.this.handleCctRgbChange(msg);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPowerOn() {
        VSlog.d(TAG, "power on");
        synchronized (this.mPowerLock) {
            this.mPowerOn = true;
        }
        ActHandler actHandler = this.mActHandler;
        if (actHandler != null) {
            actHandler.sendEmptyMessage(2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPowerOff() {
        VSlog.d(TAG, "power off");
        synchronized (this.mPowerLock) {
            this.mPowerOn = false;
        }
        ActHandler actHandler = this.mActHandler;
        if (actHandler != null) {
            actHandler.sendEmptyMessage(3);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAccChange(int level) {
        int i = this.mLastAccLevel;
        if (i == level) {
            VSlog.d(TAG, "onAccChange: same level=" + level);
            return;
        }
        int delay = 0;
        if (i != -1) {
            delay = level > i ? 500 : 1000;
        }
        VSlog.d(TAG, "onAccChange: update level=" + level + " delay =" + delay);
        this.mLastAccLevel = level;
        ActHandler actHandler = this.mActHandler;
        if (actHandler != null) {
            actHandler.removeMessages(4);
            Message message = this.mActHandler.obtainMessage();
            message.arg1 = level;
            message.what = 4;
            this.mActHandler.sendMessageDelayed(message, delay);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCctFactorChanged() {
        int factor = getDebugCctFactor();
        VSlog.d(TAG, "onCctFactorChanged factor=" + factor);
        if (factor < 0 && factor > 255) {
            VSlog.e(TAG, "invalid factor");
            return;
        }
        ActHandler actHandler = this.mActHandler;
        if (actHandler != null) {
            actHandler.removeMessages(4);
            Message message = this.mActHandler.obtainMessage();
            message.obj = Integer.valueOf(factor);
            message.what = 7;
            this.mActHandler.sendMessage(message);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCctRgbChanged() {
        VSlog.d(TAG, "onCctRgbChanged");
        ActHandler actHandler = this.mActHandler;
        if (actHandler != null) {
            actHandler.removeMessages(4);
            Message message = this.mActHandler.obtainMessage();
            message.what = 8;
            this.mActHandler.sendMessage(message);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAutoEyeCctEnableChanged() {
        boolean bAutoEyeEnable = isAccessiblityAutoEyeCctEnabled();
        VSlog.d(TAG, "onAutoEyeCctEnableChanged bAutoEyeEnable=" + bAutoEyeEnable);
        sendAccEnableMsg(bAutoEyeEnable);
    }

    private void sendAccEnableMsg(boolean bEnable) {
        VSlog.d(TAG, "sendAccEnableMsg");
        ActHandler actHandler = this.mActHandler;
        if (actHandler != null) {
            actHandler.removeMessages(4);
            Message message = this.mActHandler.obtainMessage();
            message.obj = Boolean.valueOf(bEnable);
            message.what = 5;
            this.mActHandler.sendMessage(message);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onProxChange(boolean bNear) {
        if (this.mNear == bNear) {
            VSlog.d(TAG, "onProxChange same bNear=" + bNear);
            return;
        }
        VSlog.d(TAG, "onProxChange bNear=" + bNear);
        this.mNear = bNear;
        ActHandler actHandler = this.mActHandler;
        if (actHandler != null) {
            actHandler.removeMessages(4);
            Message message = this.mActHandler.obtainMessage();
            message.obj = Boolean.valueOf(bNear);
            message.what = 6;
            this.mActHandler.sendMessage(message);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePowerOn() {
        VSlog.d(TAG, "handlePowerOn");
        enableSensorListen();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePowerOff() {
        VSlog.d(TAG, "handlePowerOff");
        disableSensorListen();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAccChange(Message msg) {
        int level = msg.arg1;
        VSlog.d(TAG, "handleAccChange level=" + level);
        setMatrix(level);
        applyColorMatrix(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAccEnableChange(Message msg) {
        boolean enable = ((Boolean) msg.obj).booleanValue();
        VSlog.d(TAG, "handleAccEnableChange enable=" + enable);
        if (enable && !isActivated()) {
            setActivated(true);
        }
        if (!enable && isActivated()) {
            setActivated(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleProxChange(Message msg) {
        boolean near = ((Boolean) msg.obj).booleanValue();
        VSlog.d(TAG, "handleProxChange near=" + near);
        if (near) {
            disableLightSensorListen();
        } else {
            enableLightSensorListen();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCctFactorChange(Message msg) {
        int factor = ((Integer) msg.obj).intValue();
        VSlog.d(TAG, "handleCctFactorChange factor=" + factor);
        setFactorMatrix(factor);
        applyColorMatrix(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCctRgbChange(Message msg) {
        int r = getDebugRed();
        int g = getDebugGreen();
        int b = getDebugBlue();
        VSlog.d(TAG, "handleRgbChange rgb=(" + r + "," + g + ",b" + b);
        if ((r < 0 && r > 255) || ((g < 0 && g > 255) || (b < 0 && b > 255))) {
            VSlog.e(TAG, "invalid rgb=(" + r + "," + g + ",b" + b);
            return;
        }
        setRgbMatrix(r, g, b);
        applyColorMatrix(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ScreenStatusReceiver extends BroadcastReceiver {
        ScreenStatusReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                VSlog.d(VivoAutoColorTempController.TAG, "receive: " + action);
                VivoAutoColorTempController.this.onPowerOn();
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                VSlog.d(VivoAutoColorTempController.TAG, "receive: " + action);
                VivoAutoColorTempController.this.onPowerOff();
            }
        }
    }

    private void enablePowerListen() {
        if (!this.mEnableListenPower) {
            this.mScreenStatusReceiver = new ScreenStatusReceiver();
            IntentFilter screenStatusIF = new IntentFilter();
            screenStatusIF.addAction("android.intent.action.SCREEN_ON");
            screenStatusIF.addAction("android.intent.action.SCREEN_OFF");
            this.mContext.registerReceiverAsUser(this.mScreenStatusReceiver, UserHandle.ALL, screenStatusIF, null, null);
            this.mEnableListenPower = true;
        }
    }

    private void disablePowerListen() {
        if (this.mEnableListenPower) {
            VSlog.d(TAG, "disablePowerListen");
            ScreenStatusReceiver screenStatusReceiver = this.mScreenStatusReceiver;
            if (screenStatusReceiver != null) {
                this.mContext.unregisterReceiver(screenStatusReceiver);
                this.mScreenStatusReceiver = null;
            }
            this.mEnableListenPower = false;
        }
    }

    private void endCctAnimator() {
        if (Looper.myLooper() != this.mColorDisplayService.mHandler.getLooper()) {
            this.mColorDisplayService.mHandler.post(new Runnable() { // from class: com.android.server.display.color.VivoAutoColorTempController.4
                @Override // java.lang.Runnable
                public void run() {
                    VSlog.d(VivoAutoColorTempController.TAG, "endAnimator in other thread");
                    VivoAutoColorTempController.this.endAnimator();
                }
            });
            return;
        }
        VSlog.d(TAG, "endAnimator in display service thread");
        endAnimator();
    }
}