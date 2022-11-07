package com.android.server.display.color;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import com.android.server.am.frozen.FrozenQuicker;
import com.android.server.display.VivoDisplayPowerControllerImpl;
import com.android.server.pm.VivoPKMSLocManager;
import com.vivo.services.rms.sdk.Consts;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ExynosDisplayATC {
    private static final int DELAY_LIGHT_SENSOR = 500;
    private static final int MSG_ATC_ONOFF = 5;
    private static final int MSG_HW_TRIG = 4;
    private static final int MSG_LIGHT_SENSOR_APS = 3;
    private static final int MSG_LIGHT_SENSOR_LUX = 2;
    private static final int MSG_SIGNAL_REFRESH = 1;
    private static final String TAG = "ExynosDisplayATC";
    private String mAtcFactoryXMLPath;
    private final Context mContext;
    private Sensor mLightSensor;
    private final Handler mLocalHandler;
    private SensorManager mSensorManager;
    public static boolean TUNE_MODE = false;
    private static final long LUX_EVENT_HORIZON = TimeUnit.SECONDS.toNanos(10);
    private static ExynosDisplayATC mExynosDisplayATC = null;
    private final boolean DEBUG = "eng".equals(Build.TYPE);
    private boolean mLightSensorEnabled = false;
    private String ATC_SFR_SYSFS_PATH = "/sys/class/dqe/dqe/aps";
    private String ATC_LUX_SYSFS_PATH = "/sys/class/dqe/dqe/aps_lux";
    private String ATC_ONOFF_SYSFS_PATH = "/sys/class/dqe/dqe/aps_onoff";
    private String HW_TRIG_SYSFS_PATH = "/sys/class/dqe/dqe/hw_trig";
    private String ATC_DIMOFF_SYSFS_PATH = "/sys/class/dqe/dqe/aps_dim_off";
    private final String APS_INIT = "0,0,128,128,128,0,10,14,2,0,25,230,140,250,0,3,3,2,3,128,1";
    private String mApsInit = null;
    private String mLux = "3000,4000,5000,6000,8000,10000,15000,20000,25000,30000,40000,50000";
    private String mQsize = "8";
    private String mQcoeff = "10,5,4,3,2,1,1,1";
    private String mQalcoeff = "4";
    private String mQdelay = "0";
    private String[] mItem = null;
    private String XML_SYSFS_PATH = "/sys/class/dqe/dqe/xml";
    private String ATC_CAL_FILE_PATH = "/data/dqe/calib_data_atc.xml";
    private String ATC_BL_FILE_PATH = "/data/dqe/calib_data_atc_bl.xml";
    private String BYPASS_CAL_FILE_PATH = "/vendor/etc/dqe/calib_data_bypass.xml";
    private final Object mDataCollectionLock = new Object();
    private Deque<LightData> mLastSensorReadings = new ArrayDeque();
    private int[] mAmbientLight = {0, VivoPKMSLocManager.MAX_LOCATION_WAIT_TIME, VivoDisplayPowerControllerImpl.COLOR_FADE_ANIMATION_GLOBAL, FrozenQuicker.FREEZE_STATUS_CHECK_MS, 6000, 8000, 10000, 15000, 20000, 25000, 30000, 40000, 50000};
    private String[] mApsTable = null;
    private String[] mQueSizeTable = null;
    private String[] mQueCoeffTable = null;
    private String[] mQueDelayTable = null;
    private String[] mQueAlCoeffTable = null;
    private int mLastLuminance = 0;
    private int mPrevLuminance = 0;
    private int mLastHWTrig = 0;
    private String mLastAps = null;
    private String mPrevAps = null;
    private int mEventCount = 0;
    private CountDownTimer mCountdownTimer = null;
    private int mTimeoutMs = 2500;
    private int mIntervalMs = 500;
    private int mCountDownTimerCount = 0;
    private int[] mBrightnessInit = {0};
    private int[] mBrightnessLux = {0};
    private int[] mBrightnessSetting = {0};
    private final Object mLock = new Object();
    private boolean mATCEnabled = false;
    private String[] mAtcMode = {"default", "game", "video"};
    private int mAtcUserId = 0;
    private Handler mHandler = new Handler() { // from class: com.android.server.display.color.ExynosDisplayATC.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
            if (i == 1) {
                ExynosDisplayUtils.sendEmptyUpdate();
            } else if (i == 2) {
                ExynosDisplayATC.this.sysfsWriteLux(msg.arg1);
            } else if (i == 3) {
                ExynosDisplayATC.this.sysfsWriteAps(msg.obj.toString());
            } else if (i == 4) {
                ExynosDisplayATC.this.sysfsWriteHWTrig(msg.arg1);
            } else if (i == 5) {
                ExynosDisplayATC.this.enableLightSensor(msg.arg1 == 1);
                ExynosDisplayATC.this.enableATC(msg.arg1 == 1);
            }
        }
    };
    private SensorEventListener sensorListener = new SensorEventListener() { // from class: com.android.server.display.color.ExynosDisplayATC.3
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            ExynosDisplayATC.this.recordSensorEvent(event);
            if (ExynosDisplayATC.TUNE_MODE) {
                ExynosDisplayATC.this.printSensorDeque();
            }
            int lux = (int) event.values[0];
            ExynosDisplayATC.this.caculateLuminance();
            ExynosDisplayATC.this.loadLuminanceATCTable();
            int qdelay = ExynosDisplayATC.this.getQueDelay();
            if (qdelay > 0) {
                if (ExynosDisplayATC.access$808(ExynosDisplayATC.this) >= qdelay) {
                    if (ExynosDisplayATC.this.mEventCount >= qdelay) {
                        ExynosDisplayATC.this.mEventCount = 0;
                    }
                } else {
                    return;
                }
            }
            if (ExynosDisplayATC.this.mHandler != null) {
                Message msg1 = ExynosDisplayATC.this.mHandler.obtainMessage();
                msg1.what = 3;
                msg1.obj = ExynosDisplayATC.this.mLastAps;
                ExynosDisplayATC.this.mHandler.sendMessage(msg1);
                Message msg2 = ExynosDisplayATC.this.mHandler.obtainMessage();
                msg2.what = 2;
                msg2.arg1 = ExynosDisplayATC.this.mLastLuminance;
                ExynosDisplayATC.this.mHandler.sendMessage(msg2);
            }
            ExynosDisplayATC.this.startCountDownTimer();
            if (ExynosDisplayATC.TUNE_MODE) {
                ExynosDisplayATC.this.setBrightnessAdjustment(lux);
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    static /* synthetic */ int access$108(ExynosDisplayATC x0) {
        int i = x0.mCountDownTimerCount;
        x0.mCountDownTimerCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$808(ExynosDisplayATC x0) {
        int i = x0.mEventCount;
        x0.mEventCount = i + 1;
        return i;
    }

    public ExynosDisplayATC(Context context) {
        this.mLightSensor = null;
        this.mSensorManager = null;
        this.mAtcFactoryXMLPath = null;
        this.mContext = context;
        SensorManager sensorManager = (SensorManager) context.getSystemService(SensorManager.class);
        this.mSensorManager = sensorManager;
        this.mLightSensor = sensorManager.getDefaultSensor(5);
        this.mLocalHandler = new Handler(context.getMainLooper());
        getApsInit();
        initCountDownTimer();
        this.mAtcFactoryXMLPath = getAtcFactoryXmlPath();
        VSlog.d(TAG, "mAtcFactoryXMLPath:" + this.mAtcFactoryXMLPath);
        parserATCXML(this.mAtcFactoryXMLPath, this.mAtcMode[this.mAtcUserId]);
    }

    public static synchronized ExynosDisplayATC getInstance(Context context) {
        ExynosDisplayATC exynosDisplayATC;
        synchronized (ExynosDisplayATC.class) {
            if (mExynosDisplayATC == null) {
                mExynosDisplayATC = new ExynosDisplayATC(context);
            }
            exynosDisplayATC = mExynosDisplayATC;
        }
        return exynosDisplayATC;
    }

    private String getAtcFactoryXmlPath() {
        byte[] buffer = new byte[Consts.ProcessStates.FOCUS];
        String factoryXml = ExynosDisplayUtils.getStringFromFile(this.XML_SYSFS_PATH);
        if (factoryXml == null) {
            return null;
        }
        byte[] factoryXmlBuf = factoryXml.getBytes(StandardCharsets.UTF_8);
        int len = factoryXml.indexOf(".");
        if (len + 8 >= 1024) {
            return null;
        }
        for (int i = 0; i < len; i++) {
            buffer[i] = factoryXmlBuf[i];
        }
        byte[] atcSuffixBuf = "_atc.xml".getBytes(StandardCharsets.UTF_8);
        for (int i2 = 0; i2 < 8; i2++) {
            buffer[len + i2] = atcSuffixBuf[i2];
        }
        String value = new String(buffer, 0, len + 8, StandardCharsets.UTF_8);
        return value;
    }

    public void setLTMOn(boolean on, int userId) {
        VSlog.d(TAG, "setLTMOn: on=" + on + ", + userId=" + userId);
        if (this.mAtcUserId != userId) {
            parserATCXML(this.mAtcFactoryXMLPath, this.mAtcMode[userId]);
        }
        if (on) {
            if (this.mHandler.hasMessages(5)) {
                this.mHandler.removeMessages(5);
            } else {
                Message msg = this.mHandler.obtainMessage();
                msg.what = 5;
                msg.arg1 = 1;
                this.mHandler.sendMessage(msg);
            }
        } else {
            Message msg1 = this.mHandler.obtainMessage();
            msg1.what = 5;
            msg1.arg1 = 0;
            if (this.mAtcUserId == 0) {
                Message msg2 = this.mHandler.obtainMessage();
                msg2.what = 3;
                String[] strArr = this.mApsTable;
                msg2.obj = strArr != null ? strArr[0] : this.mApsInit;
                this.mHandler.sendMessage(msg2);
                this.mHandler.sendMessageDelayed(msg1, 5000L);
            } else {
                this.mHandler.sendMessage(msg1);
            }
        }
        this.mAtcUserId = userId;
    }

    private void getApsInit() {
        String[] temp_array = ExynosDisplayUtils.parserXMLALText(this.BYPASS_CAL_FILE_PATH, "atc", 0, "aps");
        if (temp_array == null || temp_array.length <= 0) {
            VSlog.d(TAG, "xml aps not found");
            this.mApsInit = "0,0,128,128,128,0,10,14,2,0,25,230,140,250,0,3,3,2,3,128,1";
            return;
        }
        this.mApsInit = temp_array[0];
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadLuminanceATCTable() {
        synchronized (this.mLock) {
            if (this.mApsTable == null) {
                return;
            }
            if (this.mQueSizeTable == null) {
                return;
            }
            if (this.mQueCoeffTable == null) {
                return;
            }
            if (this.mQueDelayTable == null) {
                return;
            }
            if (this.mQueAlCoeffTable == null) {
                return;
            }
            int pos = 1;
            while (pos < this.mAmbientLight.length && this.mLastLuminance >= this.mAmbientLight[pos]) {
                pos++;
            }
            int pos2 = pos - 1;
            this.mLastAps = this.mApsTable[pos2];
            this.mQsize = this.mQueSizeTable[pos2];
            this.mQcoeff = this.mQueCoeffTable[pos2];
            this.mQalcoeff = this.mQueAlCoeffTable[pos2];
            this.mQdelay = this.mQueDelayTable[pos2];
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class LightData {
        public float lux;
        public long timestamp;

        private LightData() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recordSensorEvent(SensorEvent event) {
        long elapsedRealtimeNanos = elapsedRealtimeNanos() - LUX_EVENT_HORIZON;
        synchronized (this.mDataCollectionLock) {
            if (!this.mLastSensorReadings.isEmpty() && event.timestamp < this.mLastSensorReadings.getLast().timestamp) {
                VSlog.d(TAG, "Ignore event " + event.values[0]);
                return;
            }
            String[] split = this.mQsize.split("\\s*,\\s*");
            this.mItem = split;
            int qsize = Integer.parseInt(split[0]);
            while (!this.mLastSensorReadings.isEmpty() && this.mLastSensorReadings.size() >= qsize) {
                this.mLastSensorReadings.removeFirst();
            }
            LightData data = new LightData();
            data.timestamp = event.timestamp;
            data.lux = event.values[0];
            this.mLastSensorReadings.addLast(data);
        }
    }

    private long elapsedRealtimeNanos() {
        return SystemClock.elapsedRealtimeNanos();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void caculateLuminance() {
        try {
            int readingCount = this.mLastSensorReadings.size();
            int pos = 0;
            float[] luxValues = new float[readingCount];
            for (LightData reading : this.mLastSensorReadings) {
                luxValues[pos] = reading.lux;
                pos++;
            }
            String[] split = this.mQsize.split("\\s*,\\s*");
            this.mItem = split;
            int qsize = Integer.parseInt(split[0]);
            int[] coefficient = new int[qsize];
            this.mItem = this.mQcoeff.split("\\s*,\\s*");
            for (int i = 0; i < qsize; i++) {
                coefficient[i] = 0;
            }
            for (int i2 = 0; i2 < this.mItem.length; i2++) {
                coefficient[i2] = Integer.parseInt(this.mItem[i2]);
            }
            String[] split2 = this.mQalcoeff.split("\\s*,\\s*");
            this.mItem = split2;
            int qalcoeff = Integer.parseInt(split2[0]);
            if (TUNE_MODE) {
                String temp = "qcoef: ";
                for (int i3 = 0; i3 < coefficient.length; i3++) {
                    temp = temp + Integer.toString(coefficient[i3]) + ",";
                }
                VSlog.d(TAG, temp + " qalcoeff: " + qalcoeff);
            }
            int sum = 0;
            int divider = 0;
            int index = 0;
            for (int i4 = readingCount - 1; i4 >= 0; i4--) {
                if (readingCount < qsize) {
                    sum += ((int) luxValues[i4]) * coefficient[index];
                    divider += coefficient[index];
                    index++;
                } else {
                    sum += (((int) luxValues[i4]) * coefficient[index]) + (this.mLastLuminance * qalcoeff);
                    int divider2 = coefficient[index] + divider + qalcoeff;
                    index++;
                    divider = divider2;
                }
            }
            int i5 = sum / divider;
            this.mLastLuminance = i5;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getQueDelay() {
        String[] split = this.mQdelay.split("\\s*,\\s*");
        this.mItem = split;
        int qdelay = Integer.parseInt(split[0]);
        if (TUNE_MODE) {
            VSlog.d(TAG, "qdelay: " + qdelay);
        }
        return qdelay;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initCountDownTimer() {
        this.mCountdownTimer = new CountDownTimer(this.mTimeoutMs, this.mIntervalMs) { // from class: com.android.server.display.color.ExynosDisplayATC.2
            @Override // android.os.CountDownTimer
            public void onTick(long millisUntilFinished) {
                ExynosDisplayATC.access$108(ExynosDisplayATC.this);
                if (ExynosDisplayATC.this.mHandler != null) {
                    ExynosDisplayATC.this.mHandler.sendEmptyMessage(1);
                }
            }

            @Override // android.os.CountDownTimer
            public void onFinish() {
                if (ExynosDisplayATC.this.mHandler != null) {
                    ExynosDisplayATC.this.mHandler.sendEmptyMessage(1);
                }
                ExynosDisplayATC.this.mCountDownTimerCount = 0;
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startCountDownTimer() {
        CountDownTimer countDownTimer = this.mCountdownTimer;
        if (countDownTimer != null) {
            this.mCountDownTimerCount = 0;
            countDownTimer.cancel();
            this.mCountdownTimer.start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setBrightnessAdjustment(int lux) {
        int[] iArr = this.mBrightnessLux;
        if (iArr.length <= 1) {
            return;
        }
        int[] iArr2 = this.mBrightnessSetting;
        if (iArr2.length <= 1 || iArr.length != iArr2.length) {
            return;
        }
        int pos = 1;
        while (true) {
            int[] iArr3 = this.mBrightnessLux;
            if (pos >= iArr3.length || lux < iArr3[pos]) {
                break;
            }
            pos++;
        }
        int pos2 = pos - 1;
        VSlog.d(TAG, "lux: " + lux + ", mBrightnessLux: " + this.mBrightnessLux[pos2] + ", mBrightnessSetting: " + this.mBrightnessSetting[pos2]);
        putScreenBrightnessSetting(this.mBrightnessSetting[pos2]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void printSensorDeque() {
        int readingCount = this.mLastSensorReadings.size();
        if (readingCount == 0) {
            return;
        }
        float[] fArr = new float[readingCount];
        long[] jArr = new long[readingCount];
        System.currentTimeMillis();
        elapsedRealtimeNanos();
        String str = null;
        for (LightData reading : this.mLastSensorReadings) {
            String temp = Integer.toString((int) reading.lux) + ", ";
            str = str == null ? temp : str + temp;
        }
        VSlog.d(TAG, "que: " + str);
    }

    protected void sysfsWriteLux(int lux) {
        if (!this.mLightSensorEnabled || this.mPrevLuminance == lux) {
            return;
        }
        ExynosDisplayUtils.sysfsWrite(this.ATC_LUX_SYSFS_PATH, lux);
        this.mPrevLuminance = lux;
    }

    protected void sysfsWriteAps(String aps) {
        if (!this.mLightSensorEnabled) {
            return;
        }
        String str = this.mPrevAps;
        if (str != null && aps.equals(str)) {
            if (TUNE_MODE) {
                VSlog.d(TAG, "aps skip : " + aps);
                return;
            }
            return;
        }
        if (TUNE_MODE) {
            VSlog.d(TAG, "aps: " + aps);
        }
        ExynosDisplayUtils.sysfsWriteSting(this.ATC_SFR_SYSFS_PATH, aps);
        this.mPrevAps = aps;
    }

    protected void sysfsWriteOnOff(boolean onoff) {
        String atc_onoff = ExynosDisplayUtils.getStringFromFile(this.ATC_ONOFF_SYSFS_PATH);
        if (atc_onoff != null) {
            VSlog.d(TAG, "onoff : " + onoff);
            if (onoff) {
                VSlog.d(TAG, "aps init: " + this.mApsInit);
                ExynosDisplayUtils.sysfsWriteSting(this.ATC_SFR_SYSFS_PATH, this.mApsInit);
                return;
            }
            ExynosDisplayUtils.sysfsWrite(this.ATC_ONOFF_SYSFS_PATH, onoff ? 1 : 0);
        }
    }

    protected void sysfsWriteHWTrig(int enable) {
        if (enable != 0 && this.mLastHWTrig == enable) {
            if (TUNE_MODE) {
                VSlog.d(TAG, "hw_trig: skip");
                return;
            }
            return;
        }
        if (TUNE_MODE) {
            VSlog.d(TAG, "hw_trig: " + enable);
        }
        ExynosDisplayUtils.sysfsWrite(this.HW_TRIG_SYSFS_PATH, enable);
        this.mLastHWTrig = enable;
    }

    protected void sysfsWriteDimOff(boolean dimoff) {
        String atc_dimoff = ExynosDisplayUtils.getStringFromFile(this.ATC_DIMOFF_SYSFS_PATH);
        if (atc_dimoff != null) {
            VSlog.d(TAG, "dimoff : " + dimoff);
            ExynosDisplayUtils.sysfsWrite(this.ATC_DIMOFF_SYSFS_PATH, dimoff ? 1 : 0);
        }
    }

    public void setATCDimOff(boolean enable) {
        if (enable) {
            sysfsWriteDimOff(true);
        } else {
            sysfsWriteDimOff(false);
        }
    }

    private void putScreenBrightnessSetting(int brightness) {
        Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness", brightness, -2);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void parserATCXML(String xml_path, String mode_name) {
        VSlog.d(TAG, "parserATCXML: " + mode_name);
        try {
            String[] temp_array = ExynosDisplayUtils.parserFactoryXMLAttribute(xml_path, mode_name, "atc", "al");
            if (temp_array == null) {
                return;
            }
            if (temp_array.length <= 0) {
                VSlog.d(TAG, "xml array size wrong: " + temp_array.length);
                return;
            }
            synchronized (this.mLock) {
                VSlog.d(TAG, "array_length: " + temp_array.length);
                this.mAmbientLight = new int[temp_array.length];
                for (int i = 0; i < temp_array.length; i++) {
                    this.mAmbientLight[i] = Integer.parseInt(temp_array[i]);
                    VSlog.d(TAG, "al: " + this.mAmbientLight[i]);
                }
                this.mApsTable = new String[this.mAmbientLight.length];
                this.mQueSizeTable = new String[this.mAmbientLight.length];
                this.mQueCoeffTable = new String[this.mAmbientLight.length];
                this.mQueDelayTable = new String[this.mAmbientLight.length];
                this.mQueAlCoeffTable = new String[this.mAmbientLight.length];
                for (int i2 = 0; i2 < this.mAmbientLight.length; i2++) {
                    String[] text_array = ExynosDisplayUtils.parserFactoryXMLALText(xml_path, mode_name, "atc", this.mAmbientLight[i2], "aps");
                    if (text_array != null) {
                        this.mApsTable[i2] = text_array[0];
                    }
                    String[] text_array2 = ExynosDisplayUtils.parserFactoryXMLALText(xml_path, mode_name, "atc", this.mAmbientLight[i2], "qsize");
                    if (text_array2 != null) {
                        this.mQueSizeTable[i2] = text_array2[0];
                    }
                    String[] text_array3 = ExynosDisplayUtils.parserFactoryXMLALText(xml_path, mode_name, "atc", this.mAmbientLight[i2], "qcoeff");
                    if (text_array3 != null) {
                        this.mQueCoeffTable[i2] = text_array3[0];
                    }
                    String[] text_array4 = ExynosDisplayUtils.parserFactoryXMLALText(xml_path, mode_name, "atc", this.mAmbientLight[i2], "qdelay");
                    if (text_array4 != null) {
                        this.mQueDelayTable[i2] = text_array4[0];
                    }
                    String[] text_array5 = ExynosDisplayUtils.parserFactoryXMLALText(xml_path, mode_name, "atc", this.mAmbientLight[i2], "qalcoeff");
                    if (text_array5 != null) {
                        this.mQueAlCoeffTable[i2] = text_array5[0];
                    }
                }
                for (int i3 = 0; i3 < this.mAmbientLight.length; i3++) {
                    VSlog.d(TAG, "<aps>" + this.mApsTable[i3]);
                    VSlog.d(TAG, "<qsize>" + this.mQueSizeTable[i3]);
                    VSlog.d(TAG, "<qcoeff>" + this.mQueCoeffTable[i3]);
                    VSlog.d(TAG, "<qdelay>" + this.mQueDelayTable[i3]);
                    VSlog.d(TAG, "<qalcoeff>" + this.mQueAlCoeffTable[i3]);
                }
            }
            loadLuminanceATCTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parserTuneATCBLXML() {
        try {
            String[] temp_array = ExynosDisplayUtils.parserXMLAttribute(this.ATC_BL_FILE_PATH, "atc", "al");
            if (temp_array == null) {
                return;
            }
            if (temp_array.length <= 0) {
                VSlog.d(TAG, "xml array size wrong: " + temp_array.length);
                return;
            }
            VSlog.d(TAG, "array_length: " + temp_array.length);
            this.mBrightnessLux = new int[temp_array.length];
            this.mBrightnessSetting = new int[temp_array.length];
            for (int i = 0; i < temp_array.length; i++) {
                this.mBrightnessLux[i] = Integer.parseInt(temp_array[i]);
                VSlog.d(TAG, "al: " + this.mBrightnessLux[i]);
            }
            for (int i2 = 0; i2 < this.mBrightnessLux.length; i2++) {
                String[] text_array = ExynosDisplayUtils.parserXMLALText(this.ATC_BL_FILE_PATH, "atc", this.mBrightnessLux[i2], "bl");
                this.mBrightnessSetting[i2] = Integer.parseInt(text_array[0]);
            }
            for (int i3 = 0; i3 < this.mBrightnessLux.length; i3++) {
                VSlog.d(TAG, "<bl>" + this.mBrightnessSetting[i3]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void enableATCTuneMode(boolean enable) {
        TUNE_MODE = enable;
        VSlog.d(TAG, "enableATCTuneMode: TUNE_MODE=" + TUNE_MODE);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void enableATC(boolean enable) {
        this.mLastAps = this.mApsInit;
        this.mPrevAps = null;
        this.mBrightnessLux = this.mBrightnessInit;
        if (enable) {
            if (TUNE_MODE && ExynosDisplayUtils.existFile(this.ATC_CAL_FILE_PATH)) {
                parserATCXML(this.ATC_CAL_FILE_PATH, "tune");
            }
            sysfsWriteOnOff(true);
            if (TUNE_MODE && ExynosDisplayUtils.existFile(this.ATC_BL_FILE_PATH)) {
                parserTuneATCBLXML();
            }
        } else {
            sysfsWriteOnOff(false);
        }
        Handler handler = this.mHandler;
        if (handler != null && this.mAtcUserId == 0) {
            handler.sendEmptyMessage(1);
        }
        this.mATCEnabled = enable;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void enableLightSensor(boolean enable) {
        VSlog.d(TAG, "enableLightSensor: enable=" + enable);
        if (enable) {
            Handler handler = this.mHandler;
            if (handler != null) {
                handler.removeMessages(2);
            }
            SensorManager sensorManager = this.mSensorManager;
            if (sensorManager != null) {
                sensorManager.registerListener(this.sensorListener, this.mLightSensor, 3);
            }
        } else {
            SensorManager sensorManager2 = this.mSensorManager;
            if (sensorManager2 != null) {
                sensorManager2.unregisterListener(this.sensorListener);
            }
            Handler handler2 = this.mHandler;
            if (handler2 != null) {
                handler2.removeMessages(2);
            }
        }
        this.mLightSensorEnabled = enable;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setLastLuminance(int lux) {
        this.mLastAps = this.mApsInit;
        this.mLastLuminance = lux;
        loadLuminanceATCTable();
        String[] split = this.mQsize.split("\\s*,\\s*");
        this.mItem = split;
        int qsize = Integer.parseInt(split[0]);
        while (!this.mLastSensorReadings.isEmpty() && this.mLastSensorReadings.size() >= qsize) {
            this.mLastSensorReadings.removeFirst();
        }
        LightData data = new LightData();
        data.timestamp = System.currentTimeMillis() / 1000;
        data.lux = lux;
        this.mLastSensorReadings.addLast(data);
        printSensorDeque();
        caculateLuminance();
        this.mLightSensorEnabled = true;
        Handler handler = this.mHandler;
        if (handler != null) {
            Message msg1 = handler.obtainMessage();
            msg1.what = 3;
            msg1.obj = this.mLastAps;
            this.mHandler.sendMessage(msg1);
            Message msg2 = this.mHandler.obtainMessage();
            msg2.what = 2;
            msg2.arg1 = this.mLastLuminance;
            this.mHandler.sendMessage(msg2);
        }
        startCountDownTimer();
        if ((this.mBrightnessLux.length <= 1 || this.mBrightnessSetting.length <= 1) && ExynosDisplayUtils.existFile(this.ATC_BL_FILE_PATH)) {
            parserTuneATCBLXML();
        }
        setBrightnessAdjustment(lux);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setCountDownTimer(int interval, int count) {
        if (interval < 100 || count < 0) {
            return;
        }
        this.mTimeoutMs = interval * count;
        this.mIntervalMs = interval;
        VSlog.d(TAG, "mTimeoutMs: " + this.mTimeoutMs + ", mIntervalMs: " + this.mIntervalMs);
        CountDownTimer countDownTimer = this.mCountdownTimer;
        if (countDownTimer != null) {
            this.mCountDownTimerCount = 0;
            countDownTimer.cancel();
        }
        this.mLocalHandler.postDelayed(new Runnable() { // from class: com.android.server.display.color.ExynosDisplayATC.4
            @Override // java.lang.Runnable
            public void run() {
                ExynosDisplayATC.this.initCountDownTimer();
            }
        }, 0L);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void enableScreenAction(boolean enable) {
        if (!this.mATCEnabled) {
            return;
        }
        VSlog.d(TAG, "enableScreenAction: enable=" + enable);
        if (!enable) {
            enableLightSensor(false);
            enableATC(false);
        }
    }
}