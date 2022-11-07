package com.android.server.display.color;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import com.android.server.am.frozen.FrozenQuicker;
import com.vivo.face.common.data.Constants;
import com.vivo.face.internal.wrapper.SensorWrapper;
import com.vivo.services.rms.display.SceneManager;
import java.lang.reflect.Field;
import java.util.Calendar;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLcmSre {
    private static final int BACK_SCREEN = 1;
    private static final String LCM_LCD_SRE_LEVEL_BACK = "/sys/lcm/lcm_sre1";
    private static final String LCM_LCD_SRE_LEVEL_FRONT = "/sys/lcm/lcm_sre";
    private static final String LCM_LCD_SRE_MAX_LEVEL_BACK = "/sys/lcm/lcm_sre_max_level1";
    private static final String LCM_LCD_SRE_MAX_LEVEL_FRONT = "/sys/lcm/lcm_sre_max_level";
    private static final String LCM_OLED_HBM_BACK = "/sys/lcm/oled_hbm1";
    private static final String LCM_OLED_HBM_FRONT = "/sys/lcm/oled_hbm";
    private static final String LCM_OLED_ORE_BACK = "/sys/lcm/oled_ore1";
    private static final String LCM_OLED_ORE_FRONT = "/sys/lcm/oled_ore";
    private static final String LCM_SRE_ENABLE_BACK = "/sys/lcm/sre1_enable";
    private static final String LCM_SRE_ENABLE_FRONT = "/sys/lcm/sre_enable";
    private static final int MAIN_SCREEN = 0;
    private static final int MAX_ORE_VALUE = 255;
    private static final int MIN_ORE_VALUE = 0;
    private static final int MSG_ALARM_CHANGE = 4;
    private static final int MSG_ALARM_TIME_LIMIT = 13;
    private static final int MSG_HBM_BACK_LEVEL = 18;
    private static final int MSG_HBM_LEVEL = 17;
    private static final int MSG_HBM_LIMIT = 9;
    private static final int MSG_LTM_LIMIT = 10;
    private static final int MSG_LTM_TEST = 22;
    private static final int MSG_LUX_CHANGE = 1;
    private static final int MSG_ORE_BACK_LEVEL = 21;
    private static final int MSG_ORE_LEVEL = 20;
    private static final int MSG_ORE_LIMIT = 11;
    private static final int MSG_POWER_OFF = 3;
    private static final int MSG_POWER_ON = 2;
    private static final int MSG_SRE_LEVEL = 19;
    private static final int MSG_SRE_LEVEL_LIMIT = 12;
    private static int ORE_LEVEL_MSLEEP_VALUE = 0;
    private static int ORE_LEVEL_SPAN_VALUE = 0;
    private static int SRE_LEVEL_MSLEEP_VALUE = 0;
    private static int SRE_LEVEL_SPAN_VALUE = 0;
    public static final String SRE_TIMEOUT_BACK_ACTION = "com.android.server.display.color.VivoLcmSre.timeout_back";
    public static final String SRE_TIMEOUT_FRONT_ACTION = "com.android.server.display.color.VivoLcmSre.timeout_front";
    public static final int SRE_TIMEOUT_MINUTE = 10;
    private static final String TAG = "VivoLcmSre";
    private static final String VIVO_ALARM_TIME_LIMIT = "vivo_alarm_time_limit";
    private static final String VIVO_HBM_BACK_LEVEL = "vivo_hbm_back_level";
    private static final String VIVO_HBM_LEVEL = "vivo_hbm_level";
    private static final String VIVO_LTM_LIMIT = "vivo_ltm_limit";
    private static final String VIVO_LTM_TEST = "vivo_ltm_test";
    private static final String VIVO_MAX_HBM_LIMIT = "vivo_max_hbm_limit";
    private static final String VIVO_ORE_BACK_LEVEL = "vivo_ore_back_level";
    private static final String VIVO_ORE_LEVEL = "vivo_ore_level";
    private static final String VIVO_ORE_LIMIT = "vivo_ore_limit";
    private static final String VIVO_PRODUCT_MODEL;
    private static final String VIVO_SRE_LEVEL = "vivo_sre_level";
    private static final String VIVO_SRE_LEVEL_LIMIT = "vivo_sre_level_limit";
    private static AlarmManager mAlarm;
    private static final boolean mLowNitProject;
    private static int mPanelMaxNit;
    private static PendingIntent[] mPendingIntent;
    private int[] mAmblientDownLevels;
    private int[] mAmblientUpLevels;
    private final Context mContext;
    SensorType mCurSensorType;
    private int mInitResult;
    private Sensor mLightSensor;
    private SreHandler mSreHandler;
    private HandlerThread mSreThread;
    private static final boolean DEBUG = SystemProperties.getBoolean("debug.lcmsre.log", true);
    private static String mPanelType = SystemProperties.get("persist.vivo.phone.panel_type", "unknown").toLowerCase();
    private SensorManager mSensorManager = null;
    private Object mLock = new Object();
    private Object mPowerLock = new Object();
    private boolean mDoubleScreen = false;
    private boolean mPowerOn = false;
    private boolean mLimitHbm = false;
    private int mMaxHbmLevel = -1;
    private ContentObserver mContentObserver = null;
    private VivoLcmEventTransferUtils mEtUtil = null;
    private long mOledHbmStartTime = System.currentTimeMillis();
    private long mOledHbmStartTime_1 = System.currentTimeMillis();
    private long mOledOreStartTime = System.currentTimeMillis();
    private long mOledOreStartTime_1 = System.currentTimeMillis();
    private long mLcdSreStartTime = System.currentTimeMillis();
    private int mLcdSreLevel = 0;
    private int mOledOreLevel = 0;
    private int mOledOreLevel_1 = 0;
    private int mSreMaxLevel_test = 0;
    private boolean[] mLcmSreEnable = {false, false};
    private boolean[] mOreEnable = {false, false};
    private boolean[] mCurOreUp = {false, false};
    private boolean[] mSreLevelEnable = {false, false};
    private boolean[] mCurSreUp = {false, false};
    private int[] mSreMaxLevel = {0, 0};
    private int[] mTypeLightUpLevels = {25000, 29900, SceneManager.ANIMATION_PRIORITY};
    private int[] mTypeLightDownLevels = {3500, 21000, 25000};
    private int mMaxLightLuxLevel = 29900;
    private int[] mCurHbmLevel = {0, 0};
    private int[] mCurLcmHbmLevel = {0, 0};
    private int[] mRestoreHbmLevel = {-1, -1};
    private int[] mCurOreLevel = {0, 0};
    private int[] mCurSreLevel = {0, 0};
    private boolean[] mBeingStrongSunlight = {false, false};
    private int mHbmVersion = 0;
    private int mLtmStartLux = 30000;
    private VivoLtmController mVivoLtmController = null;
    private SreAlarmRecevier mSreAlarmRecevier = null;
    private boolean mTimerSupport = false;
    private boolean[] mTimerStart = {false, false};
    private boolean[] mTimeOutState = {false, false};
    private int mSreTimeoutMinute = 10;
    private int[] mLastLux = {0, 0};
    private int[] mCurLux = {0, 0};
    private SensorEventListener mSensorEventListener = new SensorEventListener() { // from class: com.android.server.display.color.VivoLcmSre.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            synchronized (VivoLcmSre.this.mLock) {
                int iLightLux = (int) event.values[0];
                if (VivoLcmSre.DEBUG) {
                    VSlog.d(VivoLcmSre.TAG, "onSensorChanged lightLux[0]=" + iLightLux);
                }
                if (iLightLux > VivoLcmSre.this.mMaxLightLuxLevel) {
                    iLightLux = VivoLcmSre.this.mMaxLightLuxLevel;
                }
                VSlog.d(VivoLcmSre.TAG, "onSensorChanged new lux=" + iLightLux);
                int i = 1000;
                if (VivoLcmSre.this.mLastLux[0] != iLightLux) {
                    int delay = iLightLux > VivoLcmSre.this.mLastLux[0] ? 1000 : 6000;
                    VivoLcmSre.this.sendLuxMsg(0, iLightLux, delay);
                    VivoLcmSre.this.mLastLux[0] = iLightLux;
                    if (VivoLcmSre.this.mVivoLtmController != null) {
                        VSlog.d(VivoLcmSre.TAG, "LTM:onSensorChanged sensor_type=AMBLIENT_TYPE mLastLux[0]=" + VivoLcmSre.this.mLastLux[0]);
                        VivoLcmSre.this.mVivoLtmController.setLuxOn(VivoLcmSre.this.mLastLux[0] >= VivoLcmSre.this.mLtmStartLux);
                    }
                }
                if (VivoLcmSre.this.mDoubleScreen) {
                    int iLightLux2 = (int) event.values[2];
                    if (VivoLcmSre.DEBUG) {
                        VSlog.d(VivoLcmSre.TAG, "onSensorChanged lightLux[1]=" + iLightLux2);
                    }
                    if (iLightLux2 > VivoLcmSre.this.mMaxLightLuxLevel) {
                        iLightLux2 = VivoLcmSre.this.mMaxLightLuxLevel;
                    }
                    if (VivoLcmSre.this.mLastLux[1] != iLightLux2) {
                        if (iLightLux2 <= VivoLcmSre.this.mLastLux[1]) {
                            i = 6000;
                        }
                        int delay2 = i;
                        VivoLcmSre.this.sendLuxMsg(1, iLightLux2, delay2);
                        VivoLcmSre.this.mLastLux[1] = iLightLux2;
                    }
                }
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    static {
        int i = SystemProperties.getInt("persist.vivo.phone.panel_max_nit", 0);
        mPanelMaxNit = i;
        mLowNitProject = i == 430;
        VIVO_PRODUCT_MODEL = SystemProperties.get("ro.vivo.product.model", "unkown").toLowerCase();
        ORE_LEVEL_MSLEEP_VALUE = SystemProperties.getInt("debug.vivosre.ore_level_msleep_value", 8);
        ORE_LEVEL_SPAN_VALUE = SystemProperties.getInt("debug.vivosre.ore_level_step_value", 3600);
        SRE_LEVEL_MSLEEP_VALUE = SystemProperties.getInt("debug.vivosre.sre_level_msleep_value", 8);
        SRE_LEVEL_SPAN_VALUE = SystemProperties.getInt("debug.vivosre.sre_level_step_value", 3600);
        mAlarm = null;
        mPendingIntent = new PendingIntent[2];
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String msgToString(int msg) {
        if (msg != 1) {
            if (msg != 2) {
                if (msg != 3) {
                    if (msg == 4) {
                        return "MSG_ALARM_CHANGE";
                    }
                    switch (msg) {
                        case 9:
                            return "MSG_HBM_LIMIT";
                        case 10:
                            return "MSG_LTM_LIMIT";
                        case 11:
                            return "MSG_ORE_LIMIT";
                        case 12:
                            return "MSG_SRE_LEVEL_LIMIT";
                        case 13:
                            return "MSG_ALARM_TIME_LIMIT";
                        default:
                            return "unkown";
                    }
                }
                return "MSG_POWER_OFF";
            }
            return "MSG_POWER_ON";
        }
        return "MSG_LUX_CHANGE";
    }

    /* loaded from: classes.dex */
    public enum SensorType {
        TYPE_LIGHT_SENSOR("TYPE_LIGHT_TYPE", 0),
        AMBLIENT_SENSOR("AMBLIENT_TYPE", 1);
        
        private int index;
        private String name;

        SensorType(String name, int index) {
            this.name = name;
            this.index = index;
        }

        @Override // java.lang.Enum
        public String toString() {
            return this.index + "_" + this.name;
        }
    }

    public VivoLcmSre(Context context, SensorType sensor_type) {
        this.mInitResult = 0;
        this.mCurSensorType = SensorType.TYPE_LIGHT_SENSOR;
        VSlog.d(TAG, "create VivoLcmSre");
        this.mContext = context;
        this.mCurSensorType = sensor_type;
        int init = init();
        this.mInitResult = init;
        if (init != 0) {
            VSlog.e(TAG, "VivoLcmSre init failed");
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
        if (VIVO_PRODUCT_MODEL.startsWith("pd1821")) {
            this.mDoubleScreen = true;
        }
        VivoLtmController vivoLtmController = VivoLtmController.getInstance(this.mContext);
        this.mVivoLtmController = vivoLtmController;
        if (vivoLtmController == null) {
            VSlog.e(TAG, "mVivoLtmController is null");
            return -2;
        }
        this.mLcmSreEnable[0] = isLcmSreEnable(0);
        if (!this.mLcmSreEnable[0]) {
            VSlog.e(TAG, "This product have not enable sre for main screen");
            return -2;
        }
        this.mOreEnable[0] = isLcmOreEnable(0);
        this.mSreLevelEnable[0] = isLcmSreLevelEnable(0);
        if (this.mSreLevelEnable[0]) {
            this.mSreMaxLevel[0] = getLcmSreMaxLevel(0);
        }
        if (this.mDoubleScreen) {
            this.mLcmSreEnable[1] = isLcmSreEnable(1);
            if (!this.mLcmSreEnable[1]) {
                VSlog.e(TAG, "This product have not enable sre for back screen");
            } else {
                this.mOreEnable[1] = isLcmOreEnable(1);
                this.mSreLevelEnable[1] = isLcmSreLevelEnable(1);
                if (this.mSreLevelEnable[1]) {
                    this.mSreMaxLevel[1] = getLcmSreMaxLevel(1);
                }
            }
        }
        Sensor lightSensor = getLightSensor();
        this.mLightSensor = lightSensor;
        if (lightSensor == null) {
            VSlog.e(TAG, "light sensor not support");
            return -2;
        }
        this.mEtUtil = VivoLcmEventTransferUtils.getInstance();
        updateLuxTable();
        startSreThrad();
        initAlarmTimer();
        registSreObservers();
        onPowerOn();
        return 0;
    }

    public int deInit() {
        int iRet = 0;
        int i = this.mInitResult;
        if (i != 0) {
            return i;
        }
        if (this.mEtUtil != null) {
            VivoLcmEventTransferUtils.destroy();
            this.mEtUtil = null;
        }
        HandlerThread handlerThread = this.mSreThread;
        if (handlerThread != null) {
            handlerThread.quitSafely();
            try {
                this.mSreThread.join();
            } catch (InterruptedException e) {
                VSlog.e(TAG, "InterruptedException:", e);
                iRet = -1;
            }
            this.mSreThread = null;
        }
        disableSre();
        unregistSreObservers();
        return iRet;
    }

    private void registSreObservers() {
        VSlog.d(TAG, "registSreObservers");
        ContentResolver cr = this.mContext.getContentResolver();
        ContentObserver contentObserver = new ContentObserver(null) { // from class: com.android.server.display.color.VivoLcmSre.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                String setting = uri == null ? null : uri.getLastPathSegment();
                VSlog.d(VivoLcmSre.TAG, "status onChange: setting=" + setting);
                if (setting != null) {
                    char c = 65535;
                    switch (setting.hashCode()) {
                        case -1972579391:
                            if (setting.equals(VivoLcmSre.VIVO_MAX_HBM_LIMIT)) {
                                c = 0;
                                break;
                            }
                            break;
                        case -1111657046:
                            if (setting.equals(VivoLcmSre.VIVO_ALARM_TIME_LIMIT)) {
                                c = 3;
                                break;
                            }
                            break;
                        case -984953836:
                            if (setting.equals(VivoLcmSre.VIVO_ORE_LEVEL)) {
                                c = 7;
                                break;
                            }
                            break;
                        case -984843189:
                            if (setting.equals(VivoLcmSre.VIVO_ORE_LIMIT)) {
                                c = 1;
                                break;
                            }
                            break;
                        case -827603756:
                            if (setting.equals(VivoLcmSre.VIVO_SRE_LEVEL_LIMIT)) {
                                c = 2;
                                break;
                            }
                            break;
                        case -748215396:
                            if (setting.equals(VivoLcmSre.VIVO_ORE_BACK_LEVEL)) {
                                c = '\b';
                                break;
                            }
                            break;
                        case -527773915:
                            if (setting.equals(VivoLcmSre.VIVO_HBM_LEVEL)) {
                                c = 4;
                                break;
                            }
                            break;
                        case -72129749:
                            if (setting.equals(VivoLcmSre.VIVO_HBM_BACK_LEVEL)) {
                                c = 5;
                                break;
                            }
                            break;
                        case 375162904:
                            if (setting.equals(VivoLcmSre.VIVO_SRE_LEVEL)) {
                                c = 6;
                                break;
                            }
                            break;
                        case 900495071:
                            if (setting.equals(VivoLcmSre.VIVO_LTM_TEST)) {
                                c = '\t';
                                break;
                            }
                            break;
                    }
                    switch (c) {
                        case 0:
                            VivoLcmSre.this.onMaxHbmLimitChanged();
                            return;
                        case 1:
                            VivoLcmSre.this.onOreLimitChanged();
                            return;
                        case 2:
                            VivoLcmSre.this.onSreLevelLimitChanged();
                            return;
                        case 3:
                            VivoLcmSre.this.onAlarmTimeLimitChanged();
                            return;
                        case 4:
                            VivoLcmSre.this.onHbmLevelChanged(0);
                            return;
                        case 5:
                            VivoLcmSre.this.onHbmLevelChanged(1);
                            return;
                        case 6:
                            VivoLcmSre.this.onSreLevelChanged();
                            return;
                        case 7:
                            VivoLcmSre.this.onOreLevelChanged(0);
                            return;
                        case '\b':
                            VivoLcmSre.this.onOreLevelChanged(1);
                            return;
                        case '\t':
                            VivoLcmSre.this.onLtmTest();
                            return;
                        default:
                            return;
                    }
                }
            }
        };
        this.mContentObserver = contentObserver;
        if (contentObserver != null) {
            if (this.mOreEnable[0]) {
                Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_ORE_LIMIT, 0, 0);
                cr.registerContentObserver(Settings.System.getUriFor(VIVO_ORE_LIMIT), false, this.mContentObserver, -1);
            }
            if (this.mSreLevelEnable[0]) {
                Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_SRE_LEVEL_LIMIT, 0, 0);
                cr.registerContentObserver(Settings.System.getUriFor(VIVO_SRE_LEVEL_LIMIT), false, this.mContentObserver, -1);
            }
            if (this.mTimerSupport) {
                Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_ALARM_TIME_LIMIT, 10, 0);
                cr.registerContentObserver(Settings.System.getUriFor(VIVO_ALARM_TIME_LIMIT), false, this.mContentObserver, -1);
            }
            if (!this.mSreLevelEnable[0]) {
                Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_MAX_HBM_LIMIT, -1, 0);
                cr.registerContentObserver(Settings.System.getUriFor(VIVO_MAX_HBM_LIMIT), false, this.mContentObserver, -1);
            }
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_HBM_LEVEL), false, this.mContentObserver, -1);
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_HBM_BACK_LEVEL), false, this.mContentObserver, -1);
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_SRE_LEVEL), false, this.mContentObserver, -1);
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_ORE_LEVEL), false, this.mContentObserver, -1);
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_ORE_BACK_LEVEL), false, this.mContentObserver, -1);
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_LTM_TEST), false, this.mContentObserver, -1);
        }
    }

    private void unregistSreObservers() {
        VSlog.d(TAG, "unregistSreObservers");
        if (this.mContentObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
        }
    }

    public void onPowerOn() {
        if (DEBUG) {
            VSlog.d(TAG, "power on sre");
        }
        if (this.mInitResult != 0) {
            return;
        }
        synchronized (this.mPowerLock) {
            this.mPowerOn = true;
        }
        if (this.mSreHandler != null) {
            clearSreMsg();
            this.mSreHandler.sendEmptyMessage(2);
        }
    }

    public void onPowerOff() {
        if (DEBUG) {
            VSlog.d(TAG, "power off sre");
        }
        if (this.mInitResult != 0) {
            return;
        }
        VivoLtmController vivoLtmController = this.mVivoLtmController;
        if (vivoLtmController != null) {
            vivoLtmController.setLuxOn(false);
        }
        synchronized (this.mPowerLock) {
            this.mPowerOn = false;
        }
        if (this.mSreHandler != null) {
            clearSreMsg();
            this.mSreHandler.sendEmptyMessage(3);
        }
    }

    void sendLuxMsg(int type, int lux, int delay) {
        VSlog.d(TAG, "sendLuxMsg type =" + type + " lux=" + lux + " delay=" + delay);
        clearSreMsg();
        Message message = this.mSreHandler.obtainMessage();
        message.arg1 = type;
        message.arg2 = lux;
        message.what = 1;
        this.mSreHandler.sendMessageDelayed(message, (long) delay);
    }

    private boolean isLcmSreEnable(int type) {
        String sre_enable_str;
        if (type == 0) {
            sre_enable_str = VivoLcmUtils.readKernelNode(LCM_SRE_ENABLE_FRONT);
        } else {
            sre_enable_str = VivoLcmUtils.readKernelNode(LCM_SRE_ENABLE_BACK);
        }
        if (sre_enable_str == null) {
            VSlog.e(TAG, "read sre enable failed");
            return false;
        }
        String sre_enable_str2 = sre_enable_str.replace("\n", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        int iLen = sre_enable_str2.length() <= 2 ? sre_enable_str2.length() : 2;
        int iSreEnable = Integer.parseInt(sre_enable_str2.substring(0, iLen), 10);
        this.mHbmVersion = iSreEnable;
        VSlog.d(TAG, "sre enable version = " + this.mHbmVersion);
        return iSreEnable != 0;
    }

    private boolean isLcmOreEnable(int type) {
        VivoLtmController vivoLtmController = this.mVivoLtmController;
        if (vivoLtmController == null || vivoLtmController.isAvailable() || !mPanelType.startsWith("amoled")) {
            return false;
        }
        if (type == 0) {
            boolean bOreEnable = VivoLcmUtils.peekKernelNode(LCM_OLED_ORE_FRONT);
            return bOreEnable;
        }
        boolean bOreEnable2 = VivoLcmUtils.peekKernelNode(LCM_OLED_ORE_BACK);
        return bOreEnable2;
    }

    private boolean isLcmSreLevelEnable(int type) {
        if (!mPanelType.startsWith("tft")) {
            return false;
        }
        if (type == 0) {
            boolean bSreLevelEnable = VivoLcmUtils.peekKernelNode(LCM_LCD_SRE_LEVEL_FRONT);
            return bSreLevelEnable;
        }
        boolean bSreLevelEnable2 = VivoLcmUtils.peekKernelNode(LCM_LCD_SRE_LEVEL_BACK);
        return bSreLevelEnable2;
    }

    private int getLcmSreMaxLevel(int type) {
        String sre_max_str;
        if (type == 0) {
            sre_max_str = VivoLcmUtils.readKernelNode(LCM_LCD_SRE_MAX_LEVEL_FRONT);
        } else {
            sre_max_str = VivoLcmUtils.readKernelNode(LCM_LCD_SRE_MAX_LEVEL_BACK);
        }
        if (sre_max_str != null) {
            String sre_max_str2 = sre_max_str.replace("\n", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            int iLen = sre_max_str2.length() <= 2 ? sre_max_str2.length() : 2;
            int iSreMaxLevel = Integer.parseInt(sre_max_str2.substring(0, iLen), 16);
            return iSreMaxLevel;
        }
        VSlog.e(TAG, "read sre max failed");
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.color.VivoLcmSre$3  reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$display$color$VivoLcmSre$SensorType;

        static {
            int[] iArr = new int[SensorType.values().length];
            $SwitchMap$com$android$server$display$color$VivoLcmSre$SensorType = iArr;
            try {
                iArr[SensorType.AMBLIENT_SENSOR.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
        }
    }

    private Sensor getLightSensor() {
        if (AnonymousClass3.$SwitchMap$com$android$server$display$color$VivoLcmSre$SensorType[this.mCurSensorType.ordinal()] == 1) {
            return getAmbientSensor();
        }
        return this.mSensorManager.getDefaultSensor(5);
    }

    private Sensor getAmbientSensor() {
        Sensor target = this.mSensorManager.getDefaultSensor(getAmbientLightSceneType(), true);
        return target;
    }

    private int getAmbientLightSceneType() {
        if (DEBUG) {
            VSlog.d(TAG, "Start get new SENSOR_TYPE_AMBIENT_LIGHT_SCENE");
        }
        try {
            Class<?> sensorClass = Class.forName("android.hardware.Sensor");
            Field field = sensorClass.getDeclaredField("SENSOR_TYPE_AMBIENT_LIGHT_SCENE");
            int sensor_type = field.getInt(sensorClass);
            if (DEBUG) {
                VSlog.d(TAG, "getAmbientLightSceneType sensor_type=" + sensor_type);
            }
            return sensor_type;
        } catch (Exception e) {
            if (DEBUG) {
                VSlog.d(TAG, "getAmbientLightSceneType get default sensor_type = 66544");
            }
            e.printStackTrace();
            return SensorWrapper.SENSOR_TYPE_AMBIENT_LIGHT_SCENE;
        }
    }

    private void updateLuxTable() {
        Sensor sTypeSensor = this.mSensorManager.getDefaultSensor(5);
        if (sTypeSensor == null) {
            VSlog.e(TAG, "TYPE_LIGHT not support");
        } else {
            float fMaxLightLux = sTypeSensor.getMaximumRange();
            if (fMaxLightLux >= 21000.0f && fMaxLightLux < 25000.0f) {
                this.mMaxLightLuxLevel = 21000;
            } else if (fMaxLightLux >= 25000.0f && fMaxLightLux < 29900.0f) {
                this.mMaxLightLuxLevel = 25000;
            } else {
                this.mMaxLightLuxLevel = 29900;
            }
            if (DEBUG) {
                VSlog.d(TAG, "TYPE_LIGHT sensor fMaxLightLux=" + fMaxLightLux + " mMaxLightLuxLevel=" + this.mMaxLightLuxLevel);
            }
        }
        int i = this.mHbmVersion;
        int i2 = SceneManager.APP_REQUEST_PRIORITY;
        if (i == 2) {
            if (mLowNitProject) {
                this.mAmblientUpLevels = new int[]{30000, 40000};
                this.mAmblientDownLevels = new int[]{20000, 30000};
                this.mMaxLightLuxLevel = 40000;
            } else {
                this.mAmblientUpLevels = new int[]{30000, 40000, 50000, FrozenQuicker.ONE_MIN, SceneManager.INTERACTION_PRIORITY, SceneManager.APP_REQUEST_PRIORITY};
                this.mAmblientDownLevels = new int[]{20000, 30000, 40000, 50000, FrozenQuicker.ONE_MIN, SceneManager.INTERACTION_PRIORITY};
                this.mMaxLightLuxLevel = SceneManager.APP_REQUEST_PRIORITY;
            }
        } else {
            this.mAmblientUpLevels = new int[]{25000, 29900, SceneManager.ANIMATION_PRIORITY};
            this.mAmblientDownLevels = new int[]{3500, 21000, 25000};
        }
        Sensor sAmbientSensor = getAmbientSensor();
        if (sAmbientSensor == null) {
            VSlog.e(TAG, "AmbientSensor not support");
        } else {
            float fAmbientMaxLightLux = sAmbientSensor.getMaximumRange();
            VSlog.d(TAG, "mAmbientSensor support fAmbientMaxLightLux=" + fAmbientMaxLightLux);
            if (this.mHbmVersion == 2) {
                if (mLowNitProject) {
                    this.mMaxLightLuxLevel = fAmbientMaxLightLux < 40000.0f ? (int) fAmbientMaxLightLux : 40000;
                } else {
                    if (fAmbientMaxLightLux < 80000.0f) {
                        i2 = (int) fAmbientMaxLightLux;
                    }
                    this.mMaxLightLuxLevel = i2;
                }
            } else {
                if (mLowNitProject && fAmbientMaxLightLux > 29900.0f) {
                    fAmbientMaxLightLux = 29900.0f;
                    VSlog.d(TAG, "limit fAmbientMaxLightLux to 29900.0 for low nit project.");
                }
                if (fAmbientMaxLightLux >= 21000.0f) {
                    this.mMaxLightLuxLevel = (int) fAmbientMaxLightLux;
                    VSlog.d(TAG, "get mAmbientSensor max light lux is=" + this.mMaxLightLuxLevel);
                    int i3 = this.mMaxLightLuxLevel;
                    if (i3 == 100000) {
                        int[] iArr = this.mAmblientUpLevels;
                        iArr[0] = 50000;
                        iArr[1] = 65000;
                        iArr[2] = i3;
                        int[] iArr2 = this.mAmblientDownLevels;
                        iArr2[0] = 29900;
                        iArr2[1] = 50000;
                        iArr2[2] = 65000;
                    } else if (i3 >= 29900) {
                        int[] iArr3 = this.mAmblientUpLevels;
                        iArr3[0] = 25000;
                        iArr3[1] = 29900;
                        if (i3 == 29900) {
                            iArr3[2] = 50000;
                        } else {
                            iArr3[2] = i3;
                        }
                        int[] iArr4 = this.mAmblientDownLevels;
                        iArr4[0] = 3500;
                        iArr4[1] = 21000;
                        iArr4[2] = 25000;
                    } else if (i3 >= 25000) {
                        int[] iArr5 = this.mAmblientUpLevels;
                        iArr5[0] = 21000;
                        iArr5[1] = 25000;
                        if (i3 == 25000) {
                            iArr5[2] = 29900;
                        } else {
                            iArr5[2] = i3;
                        }
                        int[] iArr6 = this.mAmblientDownLevels;
                        iArr6[0] = 3500;
                        iArr6[1] = 15000;
                        iArr6[2] = 25000;
                    } else {
                        int[] iArr7 = this.mAmblientUpLevels;
                        iArr7[0] = 18000;
                        iArr7[1] = 21000;
                        if (i3 == 21000) {
                            iArr7[2] = 25000;
                        } else {
                            iArr7[2] = i3;
                        }
                        int[] iArr8 = this.mAmblientDownLevels;
                        iArr8[0] = 3500;
                        iArr8[1] = 12000;
                        iArr8[2] = 25000;
                    }
                }
            }
        }
        int i4 = this.mMaxLightLuxLevel;
        if (i4 > 30000) {
            i4 = 30000;
        }
        this.mLtmStartLux = i4;
        VSlog.d(TAG, "Init mLtmStartLux to " + this.mLtmStartLux);
    }

    private void startSreThrad() {
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mSreThread = handlerThread;
        if (handlerThread != null) {
            handlerThread.start();
            this.mSreHandler = new SreHandler(this.mSreThread.getLooper());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SreHandler extends Handler {
        public SreHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (VivoLcmSre.DEBUG) {
                VSlog.d(VivoLcmSre.TAG, "handleMessage " + VivoLcmSre.msgToString(msg.what));
            }
            int i = msg.what;
            if (i == 1) {
                VivoLcmSre.this.handleLuxChange(msg);
            } else if (i == 2) {
                VivoLcmSre.this.handlePowerOn();
            } else if (i == 3) {
                VivoLcmSre.this.handlePowerOff();
            } else if (i == 4) {
                VivoLcmSre.this.handleAlarmChange(msg);
            } else if (i != 9) {
                switch (i) {
                    case 11:
                        VivoLcmSre.this.handleOreLimitChange();
                        return;
                    case 12:
                        VivoLcmSre.this.handleSreLevelLimitChange();
                        return;
                    case 13:
                        VivoLcmSre.this.handleAlarmTimeLimitChange();
                        return;
                    default:
                        switch (i) {
                            case 17:
                                int hbm_level = Settings.System.getIntForUser(VivoLcmSre.this.mContext.getContentResolver(), VivoLcmSre.VIVO_HBM_LEVEL, 0, 0);
                                VivoLcmSre.this.updateHbmToLcm(0, hbm_level);
                                return;
                            case 18:
                                int hbm_level_1 = Settings.System.getIntForUser(VivoLcmSre.this.mContext.getContentResolver(), VivoLcmSre.VIVO_HBM_BACK_LEVEL, 0, 0);
                                VivoLcmSre.this.updateHbmToLcm(1, hbm_level_1);
                                return;
                            case 19:
                                int sre_level = Settings.System.getIntForUser(VivoLcmSre.this.mContext.getContentResolver(), VivoLcmSre.VIVO_SRE_LEVEL, 0, 0);
                                if (sre_level == 0) {
                                    VivoLcmSre vivoLcmSre = VivoLcmSre.this;
                                    vivoLcmSre.mSreMaxLevel_test = vivoLcmSre.mSreMaxLevel[0];
                                    VivoLcmSre.this.mSreMaxLevel[0] = 255;
                                    VivoLcmSre.this.updateSreToLcm(0, 0);
                                    return;
                                } else if (sre_level == 1) {
                                    VivoLcmSre.this.mCurSreUp[0] = true;
                                    VivoLcmSre.this.lcmSreDimming(0, false);
                                    return;
                                } else if (sre_level == 2) {
                                    VivoLcmSre.this.mCurSreUp[0] = false;
                                    VivoLcmSre.this.lcmSreDimming(0, true);
                                    return;
                                } else if (sre_level == 3) {
                                    VivoLcmSre.this.mSreMaxLevel[0] = VivoLcmSre.this.mSreMaxLevel_test;
                                    return;
                                } else {
                                    return;
                                }
                            case 20:
                                int ore_level = Settings.System.getIntForUser(VivoLcmSre.this.mContext.getContentResolver(), VivoLcmSre.VIVO_ORE_LEVEL, 0, 0);
                                if (ore_level == 0) {
                                    VivoLcmSre.this.mCurOreUp[0] = false;
                                    VivoLcmSre.this.oreDimming(0, true);
                                    return;
                                }
                                VivoLcmSre.this.mCurOreUp[0] = true;
                                VivoLcmSre.this.oreDimming(0, false);
                                return;
                            case 21:
                                int ore_level_1 = Settings.System.getIntForUser(VivoLcmSre.this.mContext.getContentResolver(), VivoLcmSre.VIVO_ORE_BACK_LEVEL, 0, 0);
                                if (ore_level_1 == 0) {
                                    VivoLcmSre.this.mCurOreUp[1] = false;
                                    VivoLcmSre.this.oreDimming(1, true);
                                    return;
                                }
                                VivoLcmSre.this.mCurOreUp[1] = true;
                                VivoLcmSre.this.oreDimming(1, false);
                                return;
                            case 22:
                                int ltm = Settings.System.getIntForUser(VivoLcmSre.this.mContext.getContentResolver(), VivoLcmSre.VIVO_LTM_TEST, 0, 0);
                                if (VivoLcmSre.this.mVivoLtmController != null) {
                                    VivoLcmSre.this.mVivoLtmController.setLuxOnTest(ltm == 1);
                                    return;
                                }
                                return;
                            default:
                                return;
                        }
                }
            } else {
                VivoLcmSre.this.handleHbmLimitChange();
            }
        }
    }

    private void clearSreMsg() {
        SreHandler sreHandler = this.mSreHandler;
        if (sreHandler != null) {
            sreHandler.removeMessages(1);
            boolean[] zArr = this.mOreEnable;
            if (zArr[0] || zArr[1]) {
                this.mSreHandler.removeMessages(11);
            }
            boolean[] zArr2 = this.mSreLevelEnable;
            if (zArr2[0] || zArr2[1]) {
                this.mSreHandler.removeMessages(12);
            } else {
                this.mSreHandler.removeMessages(9);
            }
            if (this.mTimerSupport) {
                this.mSreHandler.removeMessages(4);
                this.mSreHandler.removeMessages(13);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePowerOn() {
        if (DEBUG) {
            VSlog.d(TAG, "handlePowerOn");
        }
        enableSre();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePowerOff() {
        if (DEBUG) {
            VSlog.d(TAG, "handlePowerOff");
        }
        disableSre();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAlarmChange(Message msg) {
        int index = msg.arg1;
        if (index < 0 || index > 1) {
            VSlog.e(TAG, "invalid index=" + index);
            return;
        }
        if (this.mSreLevelEnable[index]) {
            lcmSreDimming(index, false);
        } else {
            updateHbmToLcm(index, 0);
        }
        if (this.mOreEnable[index]) {
            oreDimming(index, false);
        }
        this.mTimeOutState[index] = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleHbmLimitChange() {
        int lightLux;
        synchronized (this.mLock) {
            lightLux = this.mLastLux[0];
        }
        this.mMaxHbmLevel = getMaxHbmLimit();
        this.mLimitHbm = isMaxHbmLimitActivated();
        VSlog.d(TAG, "handleHbmLimitChange VIVO_MAX_HBM_LIMIT change, mMaxHbmLevel = " + this.mMaxHbmLevel);
        if (lightLux == this.mCurLux[0]) {
            int i = this.mMaxHbmLevel;
            if (i >= 0 && this.mCurHbmLevel[0] > i) {
                VSlog.d(TAG, "force change hbm from " + this.mCurHbmLevel[0] + " to " + this.mMaxHbmLevel);
                this.mRestoreHbmLevel[0] = this.mCurHbmLevel[0];
                updateHbmToLcm(0, this.mMaxHbmLevel);
            }
            if (this.mMaxHbmLevel == -1) {
                int i2 = this.mCurHbmLevel[0];
                int[] iArr = this.mRestoreHbmLevel;
                if (i2 != iArr[0] && iArr[0] != -1) {
                    VSlog.d(TAG, "restore force change hbm from " + this.mCurHbmLevel[0] + " to " + this.mMaxHbmLevel);
                    updateHbmToLcm(0, this.mRestoreHbmLevel[0]);
                    this.mRestoreHbmLevel[0] = -1;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOreLimitChange() {
        int lightLux;
        VSlog.d(TAG, "handleOreLimitChange");
        synchronized (this.mLock) {
            lightLux = this.mLastLux[0];
        }
        if (isOreLimitActivated()) {
            oreDimming(0, false);
        } else if (this.mMaxLightLuxLevel == lightLux) {
            oreDimming(0, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSreLevelLimitChange() {
        int lightLux;
        VSlog.d(TAG, "handleSreLevelLimitChange");
        synchronized (this.mLock) {
            lightLux = this.mLastLux[0];
        }
        if (isSreLevelLimitActivated()) {
            lcmSreDimming(0, false);
        } else if (this.mMaxLightLuxLevel == lightLux) {
            lcmSreDimming(0, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAlarmTimeLimitChange() {
        VSlog.d(TAG, "handleAlarmTimeLimitChange");
        if (isAlarmTimeLimitActivated()) {
            this.mSreTimeoutMinute = getAlarmTimeLimit();
            if (this.mBeingStrongSunlight[0]) {
                stopAlarm(0);
                startAlarm(0);
                return;
            }
            return;
        }
        this.mSreTimeoutMinute = 10;
        if (this.mBeingStrongSunlight[0]) {
            stopAlarm(0);
            startAlarm(0);
        }
    }

    private void enableSre() {
        boolean[] zArr = this.mBeingStrongSunlight;
        zArr[0] = false;
        if (this.mDoubleScreen) {
            zArr[1] = false;
        }
        this.mSensorManager.registerListener(this.mSensorEventListener, this.mLightSensor, 2);
        clearSreMsg();
    }

    private void disableSre() {
        boolean[] zArr = this.mBeingStrongSunlight;
        zArr[0] = false;
        if (this.mDoubleScreen) {
            zArr[1] = false;
        }
        this.mSensorManager.unregisterListener(this.mSensorEventListener);
        clearSreMsg();
        stopAlarm(0);
        if (this.mSreLevelEnable[0]) {
            this.mCurSreUp[0] = false;
            this.mCurSreLevel[0] = 0;
            updateSreToLcm(0, 0);
        } else {
            updateHbmToLcm(0, 0);
        }
        if (this.mOreEnable[0]) {
            this.mCurOreUp[0] = false;
            this.mCurOreLevel[0] = 0;
            updateOreToLcm(0, 0);
        }
        if (this.mDoubleScreen) {
            stopAlarm(1);
            if (this.mSreLevelEnable[1]) {
                this.mCurSreUp[1] = false;
                this.mCurSreLevel[1] = 0;
                updateSreToLcm(1, 0);
            } else {
                updateHbmToLcm(1, 0);
            }
            if (this.mOreEnable[1]) {
                this.mCurOreUp[1] = false;
                this.mCurOreLevel[1] = 0;
                updateOreToLcm(1, 0);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLuxChange(Message msg) {
        int index;
        boolean bOreChange;
        int i;
        int iLength;
        boolean bOreChange2;
        int i2;
        int type = msg.arg1;
        int lux = msg.arg2;
        boolean bOreChange3 = false;
        boolean bSreChange = false;
        boolean bOreUp = false;
        boolean bSreUp = false;
        if (type == 0) {
            index = 0;
        } else {
            index = 1;
        }
        int iHbmLevel = this.mCurHbmLevel[index];
        boolean lastBeingStrongSunlight = this.mBeingStrongSunlight[index];
        int[] iArr = this.mCurLux;
        if (iArr[index] == lux) {
            return;
        }
        boolean bRaiseUp = lux > iArr[index];
        VSlog.d(TAG, "handleLuxChange mCurLux=" + this.mCurLux[index] + " lux=" + lux + " mCurHbmLevel[" + index + "]=" + this.mCurHbmLevel[index]);
        if (this.mCurSensorType == SensorType.AMBLIENT_SENSOR) {
            if (!bRaiseUp) {
                int iLength2 = this.mAmblientDownLevels.length;
                int iMaxHbm = iLength2 - 1;
                int i3 = iLength2 - 1;
                while (true) {
                    if (i3 < 0) {
                        break;
                    }
                    if (i3 == 0) {
                        if (lux > this.mAmblientDownLevels[i3]) {
                            iLength = iLength2;
                        } else {
                            iHbmLevel = 0;
                            this.mBeingStrongSunlight[index] = false;
                            iLength = iLength2;
                        }
                    } else {
                        int[] iArr2 = this.mAmblientDownLevels;
                        iLength = iLength2;
                        if (lux <= iArr2[i3] && lux > iArr2[i3 - 1]) {
                            if (this.mCurHbmLevel[index] > i3) {
                                iHbmLevel = i3;
                            }
                        }
                    }
                    i3--;
                    iLength2 = iLength;
                }
                VSlog.d(TAG, "down index=" + index + " iHbmLevel=" + iHbmLevel + " mMaxHbmLevel=" + this.mMaxHbmLevel);
                this.mRestoreHbmLevel[index] = iHbmLevel;
                if (index == 0 && (i = this.mMaxHbmLevel) >= 0 && iHbmLevel > i) {
                    iHbmLevel = this.mMaxHbmLevel;
                }
                if (iHbmLevel >= iMaxHbm) {
                    bOreChange = false;
                } else {
                    if (this.mOreEnable[index] && this.mCurOreUp[index]) {
                        bOreUp = false;
                        bOreChange = true;
                    } else {
                        bOreChange = false;
                    }
                    if (this.mSreLevelEnable[index] && this.mCurSreUp[index]) {
                        bSreUp = false;
                        bSreChange = true;
                    }
                }
            } else {
                int iLength3 = this.mAmblientUpLevels.length;
                int i4 = 0;
                while (true) {
                    if (i4 >= iLength3) {
                        bOreChange2 = bOreChange3;
                        break;
                    }
                    int type2 = type;
                    if (i4 == iLength3 - 1) {
                        if (lux < this.mAmblientUpLevels[i4]) {
                            bOreChange2 = bOreChange3;
                        } else {
                            iHbmLevel = iLength3;
                            bOreChange2 = bOreChange3;
                        }
                    } else {
                        int[] iArr3 = this.mAmblientUpLevels;
                        bOreChange2 = bOreChange3;
                        if (lux >= iArr3[i4] && lux < iArr3[i4 + 1]) {
                            if (this.mCurHbmLevel[index] < i4 + 1) {
                                iHbmLevel = i4 + 1;
                            }
                        }
                    }
                    i4++;
                    type = type2;
                    bOreChange3 = bOreChange2;
                }
                if (iHbmLevel > 0) {
                    this.mBeingStrongSunlight[index] = true;
                }
                VSlog.d(TAG, "up index=" + index + " iHbmLevel=" + iHbmLevel + " mMaxHbmLevel=" + this.mMaxHbmLevel);
                this.mRestoreHbmLevel[index] = iHbmLevel;
                if (index == 0 && (i2 = this.mMaxHbmLevel) >= 0 && iHbmLevel > i2) {
                    iHbmLevel = this.mMaxHbmLevel;
                }
                if (this.mMaxLightLuxLevel != lux) {
                    bOreChange = bOreChange2;
                } else {
                    if (this.mOreEnable[index] && !this.mCurOreUp[index]) {
                        bOreUp = true;
                        bOreChange = true;
                    } else {
                        bOreChange = bOreChange2;
                    }
                    if (this.mSreLevelEnable[index] && !this.mCurSreUp[index]) {
                        bSreUp = true;
                        bSreChange = true;
                    }
                }
            }
        } else if (bRaiseUp) {
            int[] iArr4 = this.mTypeLightUpLevels;
            if (lux >= iArr4[0] && lux < iArr4[1]) {
                if (this.mCurHbmLevel[index] < 1) {
                    iHbmLevel = 1;
                }
            } else {
                int[] iArr5 = this.mTypeLightUpLevels;
                if (lux >= iArr5[1] && lux < iArr5[2]) {
                    if (this.mCurHbmLevel[index] < 2) {
                        iHbmLevel = 2;
                    }
                } else if (lux >= this.mTypeLightUpLevels[2] && this.mCurHbmLevel[index] < 3) {
                    iHbmLevel = 3;
                }
            }
            if (iHbmLevel > 0) {
                this.mBeingStrongSunlight[index] = true;
            }
            if (this.mMaxLightLuxLevel != lux) {
                bOreChange = false;
            } else {
                if (this.mOreEnable[index] && !this.mCurOreUp[index]) {
                    bOreUp = true;
                    bOreChange = true;
                } else {
                    bOreChange = false;
                }
                if (this.mSreLevelEnable[index] && !this.mCurSreUp[index]) {
                    bSreUp = true;
                    bSreChange = true;
                }
            }
        } else {
            int[] iArr6 = this.mTypeLightDownLevels;
            if (lux <= iArr6[2] && lux > iArr6[1]) {
                if (this.mCurHbmLevel[index] > 2) {
                    iHbmLevel = 2;
                }
            } else {
                int[] iArr7 = this.mTypeLightDownLevels;
                if (lux <= iArr7[1] && lux > iArr7[0]) {
                    if (this.mCurHbmLevel[index] > 1) {
                        iHbmLevel = 1;
                    }
                } else if (lux <= this.mTypeLightUpLevels[0]) {
                    iHbmLevel = 0;
                    this.mBeingStrongSunlight[index] = false;
                }
            }
            if (iHbmLevel >= 3) {
                bOreChange = false;
            } else {
                if (this.mOreEnable[index] && this.mCurOreUp[index]) {
                    bOreUp = false;
                    bOreChange = true;
                } else {
                    bOreChange = false;
                }
                if (this.mSreLevelEnable[index] && this.mCurSreUp[index]) {
                    bSreUp = false;
                    bSreChange = true;
                }
            }
        }
        boolean[] zArr = this.mBeingStrongSunlight;
        if (lastBeingStrongSunlight != zArr[index]) {
            if (zArr[index]) {
                startAlarm(index);
            } else {
                stopAlarm(index);
            }
        }
        VSlog.d(TAG, "handleLuxChange bOreUp=" + bOreUp + " bOreChange=" + bOreChange + " iHbmLevel=" + iHbmLevel + " mTimeOutState=" + this.mTimeOutState[index] + " mBeingStrongSunlight=" + this.mBeingStrongSunlight[index]);
        if (!this.mTimeOutState[index]) {
            if (this.mSreLevelEnable[index]) {
                if (bSreChange) {
                    lcmSreDimming(index, bSreUp);
                }
            } else {
                updateHbmToLcm(index, iHbmLevel);
            }
            if (this.mOreEnable[index] && bOreChange) {
                oreDimming(index, bOreUp);
            }
        }
        this.mCurLux[index] = lux;
    }

    int mapLcmHbmLevel(int index, int iHbmLevel) {
        int iLcmHbmLevel;
        int iLcmHbmLevel2 = this.mCurLcmHbmLevel[index];
        int[] iArr = this.mCurHbmLevel;
        if (iArr[index] == iHbmLevel) {
            if (DEBUG) {
                VSlog.d(TAG, "mapLcmHbmLevel screen=" + index + " ignore same sre_level " + iHbmLevel);
            }
            return iLcmHbmLevel2;
        }
        iArr[index] = iHbmLevel;
        if (this.mHbmVersion == 2) {
            if (iHbmLevel <= 3) {
                iLcmHbmLevel = iHbmLevel;
            } else {
                iLcmHbmLevel = iHbmLevel + 16;
            }
        } else {
            iLcmHbmLevel = iHbmLevel;
        }
        VSlog.d(TAG, "mapLcmHbmLevel screen=" + index + " iHbmLevel=" + iHbmLevel + " iLcmHbmLevel=" + iLcmHbmLevel);
        return iLcmHbmLevel;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateHbmToLcm(int index, int hbm_level) {
        int iLcmHbmLevel = mapLcmHbmLevel(index, hbm_level);
        if (this.mCurLcmHbmLevel[index] == iLcmHbmLevel) {
            if (DEBUG) {
                VSlog.d(TAG, "updateSreToLcm screen=" + index + " ignore same sre_level " + iLcmHbmLevel);
                return;
            }
            return;
        }
        if (index == 0) {
            if (DEBUG) {
                VSlog.d(TAG, "screen=" + index + " Write node " + LCM_OLED_HBM_FRONT + " " + iLcmHbmLevel);
            }
            if (this.mEtUtil != null) {
                long tempTime = System.currentTimeMillis();
                int[] iArr = this.mCurLcmHbmLevel;
                if (iArr[0] != 0) {
                    VivoLcmEventTransferUtils vivoLcmEventTransferUtils = this.mEtUtil;
                    long j = this.mOledHbmStartTime;
                    vivoLcmEventTransferUtils.send(1, j, tempTime - j, iArr[0]);
                }
                this.mOledHbmStartTime = tempTime;
            }
            VivoLcmUtils.writeKernelNode(LCM_OLED_HBM_FRONT, Integer.toString(iLcmHbmLevel));
        } else {
            if (DEBUG) {
                VSlog.d(TAG, "screen=" + index + " Write node " + LCM_OLED_HBM_BACK + " " + iLcmHbmLevel);
            }
            if (this.mEtUtil != null) {
                long tempTime2 = System.currentTimeMillis();
                int[] iArr2 = this.mCurLcmHbmLevel;
                if (iArr2[1] != 0) {
                    VivoLcmEventTransferUtils vivoLcmEventTransferUtils2 = this.mEtUtil;
                    long j2 = this.mOledHbmStartTime_1;
                    vivoLcmEventTransferUtils2.send(2, j2, tempTime2 - j2, iArr2[1]);
                }
                this.mOledHbmStartTime_1 = tempTime2;
            }
            VivoLcmUtils.writeKernelNode(LCM_OLED_HBM_BACK, Integer.toString(iLcmHbmLevel));
        }
        this.mCurLcmHbmLevel[index] = iLcmHbmLevel;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSreToLcm(int index, int sre_level) {
        if (this.mCurSreLevel[index] == sre_level) {
            if (DEBUG) {
                VSlog.d(TAG, "updateSreToLcm screen=" + index + " ignore same sre_level " + sre_level);
                return;
            }
            return;
        }
        if (index == 0) {
            if (DEBUG) {
                VSlog.d(TAG, "screen=" + index + " Write node " + LCM_LCD_SRE_LEVEL_FRONT + " " + sre_level);
            }
            VivoLcmUtils.writeKernelNode(LCM_LCD_SRE_LEVEL_FRONT, Integer.toString(sre_level));
        } else {
            if (DEBUG) {
                VSlog.d(TAG, "screen=" + index + " Write node " + LCM_LCD_SRE_LEVEL_BACK + " " + sre_level);
            }
            VivoLcmUtils.writeKernelNode(LCM_LCD_SRE_LEVEL_BACK, Integer.toString(sre_level));
        }
        this.mCurSreLevel[index] = sre_level;
    }

    private void updateOreToLcm(int index, int ore_level) {
        if (this.mCurOreLevel[index] == ore_level) {
            if (DEBUG) {
                VSlog.d(TAG, "updateOreToLcm screen=" + index + " ignore same sre_level " + ore_level);
                return;
            }
            return;
        }
        if (index == 0) {
            if (DEBUG) {
                VSlog.d(TAG, "screen=" + index + " Write node " + LCM_OLED_ORE_FRONT + " " + ore_level);
            }
            VivoLcmUtils.writeKernelNode(LCM_OLED_ORE_FRONT, Integer.toString(ore_level));
        } else {
            if (DEBUG) {
                VSlog.d(TAG, "screen=" + index + " Write node " + LCM_OLED_ORE_BACK + " " + ore_level);
            }
            VivoLcmUtils.writeKernelNode(LCM_OLED_ORE_BACK, Integer.toString(ore_level));
        }
        this.mCurOreLevel[index] = ore_level;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:13:0x003f  */
    /* JADX WARN: Removed duplicated region for block: B:16:0x0064  */
    /* JADX WARN: Removed duplicated region for block: B:17:0x006c  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x0087  */
    /* JADX WARN: Removed duplicated region for block: B:26:0x0096  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x00a8  */
    /* JADX WARN: Removed duplicated region for block: B:31:0x00af  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x00b3  */
    /* JADX WARN: Removed duplicated region for block: B:35:0x00bb  */
    /* JADX WARN: Removed duplicated region for block: B:36:0x00f9  */
    /* JADX WARN: Removed duplicated region for block: B:56:0x012a A[Catch: all -> 0x0176, TRY_LEAVE, TryCatch #1 {all -> 0x0176, blocks: (B:39:0x00ff, B:51:0x0120, B:54:0x0126, B:56:0x012a, B:46:0x0114, B:49:0x011c), top: B:84:0x00ff }] */
    /* JADX WARN: Removed duplicated region for block: B:63:0x014a  */
    /* JADX WARN: Removed duplicated region for block: B:84:0x00ff A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void lcmSreDimming(int r29, boolean r30) {
        /*
            Method dump skipped, instructions count: 396
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.color.VivoLcmSre.lcmSreDimming(int, boolean):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:13:0x003b  */
    /* JADX WARN: Removed duplicated region for block: B:22:0x0073  */
    /* JADX WARN: Removed duplicated region for block: B:23:0x0082  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x0092  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0097  */
    /* JADX WARN: Removed duplicated region for block: B:29:0x009b  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x00a3  */
    /* JADX WARN: Removed duplicated region for block: B:45:0x00fc  */
    /* JADX WARN: Removed duplicated region for block: B:61:0x015b  */
    /* JADX WARN: Removed duplicated region for block: B:78:0x00e4 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void oreDimming(int r31, boolean r32) {
        /*
            Method dump skipped, instructions count: 402
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.color.VivoLcmSre.oreDimming(int, boolean):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SreAlarmRecevier extends BroadcastReceiver {
        private SreAlarmRecevier() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int index;
            String action = intent.getAction();
            if (action.equals(VivoLcmSre.SRE_TIMEOUT_FRONT_ACTION)) {
                index = 0;
            } else if (action.equals(VivoLcmSre.SRE_TIMEOUT_BACK_ACTION)) {
                index = 1;
            } else {
                VSlog.e(VivoLcmSre.TAG, "invalid action");
                return;
            }
            if (VivoLcmSre.DEBUG) {
                VSlog.d(VivoLcmSre.TAG, "screen=" + index + " recevie timeout message");
            }
            VivoLcmSre.this.sendAlarmMsg(index);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendAlarmMsg(int index) {
        VSlog.d(TAG, "sendAlarmMsg index =" + index);
        clearSreMsg();
        Message message = this.mSreHandler.obtainMessage();
        message.arg1 = index;
        message.what = 4;
        this.mSreHandler.sendMessage(message);
    }

    private void initAlarmTimer() {
        Intent alarmIntent = new Intent(SRE_TIMEOUT_FRONT_ACTION).addFlags(1342177280);
        mPendingIntent[0] = PendingIntent.getBroadcast(this.mContext, 0, alarmIntent, 0);
        if (this.mDoubleScreen) {
            Intent alarmIntent2 = new Intent(SRE_TIMEOUT_BACK_ACTION).addFlags(1342177280);
            mPendingIntent[1] = PendingIntent.getBroadcast(this.mContext, 0, alarmIntent2, 0);
        }
        mAlarm = (AlarmManager) this.mContext.getSystemService("alarm");
        IntentFilter filter = new IntentFilter();
        filter.addAction(SRE_TIMEOUT_FRONT_ACTION);
        filter.addAction(SRE_TIMEOUT_BACK_ACTION);
        SreAlarmRecevier sreAlarmRecevier = new SreAlarmRecevier();
        this.mSreAlarmRecevier = sreAlarmRecevier;
        this.mContext.registerReceiver(sreAlarmRecevier, filter);
        this.mTimerSupport = true;
    }

    private void startAlarm(int index) {
        if (DEBUG) {
            VSlog.d(TAG, "startAlarm screen=" + index);
        }
        if (this.mTimerStart[index]) {
            if (DEBUG) {
                VSlog.d(TAG, "startAlarm: already start timer");
                return;
            }
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(12, this.mSreTimeoutMinute);
        mAlarm.setExact(0, calendar.getTimeInMillis(), mPendingIntent[index]);
        this.mTimerStart[index] = true;
    }

    private void stopAlarm(int index) {
        if (DEBUG) {
            VSlog.d(TAG, "stopAlarm index=" + index);
        }
        if (!this.mTimerStart[index]) {
            if (DEBUG) {
                VSlog.d(TAG, "stopAlarm: already stop timer");
                return;
            }
            return;
        }
        mAlarm.cancel(mPendingIntent[index]);
        this.mTimerStart[index] = false;
        this.mTimeOutState[index] = false;
    }

    private boolean isMaxHbmLimitActivated() {
        return -1 != Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_MAX_HBM_LIMIT, -1, 0);
    }

    private int getMaxHbmLimit() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_MAX_HBM_LIMIT, -1, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onMaxHbmLimitChanged() {
        if (this.mSreHandler != null) {
            clearSreMsg();
            this.mSreHandler.sendEmptyMessage(9);
        }
    }

    private boolean isOreLimitActivated() {
        return 1 == Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_ORE_LIMIT, 0, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onOreLimitChanged() {
        if (this.mSreHandler != null) {
            clearSreMsg();
            this.mSreHandler.sendEmptyMessage(11);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onHbmLevelChanged(int main) {
        Message msg = this.mSreHandler.obtainMessage();
        if (main == 0) {
            msg.what = 17;
        } else {
            msg.what = 18;
        }
        this.mSreHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSreLevelChanged() {
        Message msg = this.mSreHandler.obtainMessage();
        msg.what = 19;
        this.mSreHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onOreLevelChanged(int main) {
        Message msg = this.mSreHandler.obtainMessage();
        if (main == 0) {
            msg.what = 20;
        } else {
            msg.what = 21;
        }
        this.mSreHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLtmTest() {
        Message msg = this.mSreHandler.obtainMessage();
        msg.what = 22;
        this.mSreHandler.sendMessage(msg);
    }

    private boolean isSreLevelLimitActivated() {
        return 1 == Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_SRE_LEVEL_LIMIT, 0, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSreLevelLimitChanged() {
        if (this.mSreHandler != null) {
            clearSreMsg();
            this.mSreHandler.sendEmptyMessage(12);
        }
    }

    private boolean isAlarmTimeLimitActivated() {
        return 10 != Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_ALARM_TIME_LIMIT, 10, 0);
    }

    private int getAlarmTimeLimit() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_ALARM_TIME_LIMIT, 10, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAlarmTimeLimitChanged() {
        if (this.mSreHandler != null) {
            clearSreMsg();
            this.mSreHandler.sendEmptyMessage(13);
        }
    }
}