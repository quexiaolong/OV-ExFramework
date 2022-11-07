package com.android.server;

import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioFeatures;
import android.media.AudioManager;
import android.multidisplay.MultiDisplayManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IVibratorService;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.server.display.color.VivoColorManagerService;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.android.server.wm.VCD_FF_1;
import com.android.server.wm.VivoWmsImpl;
import com.vivo.appshare.AppShareConfig;
import com.vivo.common.VivoCollectData;
import com.vivo.services.rms.ProcessList;
import com.vivo.vcodetransbase.EventTransfer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import vendor.pixelworks.hardware.display.V1_0.Vendor2Config;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoVibratorServiceImpl implements IVivoVibratorService {
    private static final String BAIDU = "com.baidu.input";
    private static final int EFFECTID_ALARM_END = 600;
    private static final int EFFECTID_NOTIFICATION_END = 500;
    private static final int EFFECTID_RINGTONE_END = 400;
    private static final int EFFECTID_RINGTONE_START = 300;
    private static final String END_TIME = "end_time";
    private static final int GEAR_PRESS_KEY_INTENSITY_HIGH = 3;
    private static final int GEAR_PRESS_KEY_INTENSITY_LOW = 1;
    private static final int GEAR_PRESS_KEY_INTENSITY_MIDDLE = 2;
    private static final int GEAR_PRESS_KEY_INTENSITY_OFF = 0;
    private static final int HIGH_NAVIGATION_ID = 134;
    private static final int HIGH_VIBRATOR_INTENSITY = 255;
    private static final String IFLYTEK = "com.iflytek.inputmethod";
    private static final int INCREASING_INDEX = 2;
    private static final int INCREASING_INTERVAL = 800;
    private static final int INCREASING_LEVEL = 10;
    private static final String INTENSITY = "intensity";
    private static final String KEY_ASCENDING_VIBRATION = "ascending_vibration";
    private static final int LOW_END_STRENTH = 128;
    private static final int LOW_NAVIGATION_ID = 132;
    private static final int LOW_START_STRENTH = 51;
    private static final int LOW_VIBRATOR_INTENSITY = 1;
    private static final int MID_END_STRENTH = 200;
    private static final int MID_NAVIGATION_ID = 133;
    private static final int MID_START_STRENTH = 77;
    private static final int MID_VIBRATOR_INTENSITY = 145;
    private static final String MODULE = "module";
    private static final int MSG_C_D = 1;
    private static final String OCTOPUS = "com.komoxo.octopusime";
    private static final String PATTERN = "pattern";
    private static final String PRESS_KEY_LEFT_INTENSITY = "haptic_left_pressure_button_intensity";
    private static final String PRESS_KEY_RIGHT_INTENSITY = "haptic_right_pressure_button_intensity";
    private static final String QQ_PINYIN = "com.tencent.qqpinyin";
    private static final String SHOCK = "shock";
    private static final String SOGOU = "com.sohu.inputmethod.sogou";
    private static final String START_TIME = "start_time";
    private static final String TAG = "VivoVibratorServiceImpl";
    private static final String TIMES = "times";
    private static final String VCD_EID_VIBRATOR = "1900";
    private static final String VCD_EID_VIBRATOR_NEW = "F321";
    private static final String VCD_EID_VIBRATOR_PRO = "19001";
    private static final String VCD_EID_VIBRATOR_PRO_NEW = "F321|10001";
    private static final String VCD_THREAD_NAME = "VCD";
    public static final int VIBRATION_INTENSITY_HIGH = 3;
    public static final int VIBRATION_INTENSITY_LOW = 1;
    public static final int VIBRATION_INTENSITY_MEDIUM = 2;
    private static final long WAIT_AUDIOCALLBACK_TIME = 1000;
    private static final int defaultHapticFeedbackIntensity = 2;
    private static final int defaultNotificationVibrationIntensity = 2;
    private static final int defaultRingVibrationIntensity = 3;
    private AudioFeatureCallback mAudioFeatureCallback;
    private AudioFeatures mAudioFeatures;
    private CDHandler mCDHandler;
    private HandlerThread mCDThread;
    private final Context mContext;
    private String mCurrentPkg;
    private final Handler mH;
    private int mHapticFeedbackIntensity;
    private int mIncreasingVibrationEnable;
    private boolean mIsControlledByRemote;
    private int mLastLeftPressKeyIntensity;
    private int mLastRightPressKeyIntensity;
    private int mLeftPressKeyIntensity;
    private int mNotificationIntensity;
    private int mRightPressKeyIntensity;
    private int mRingIntensity;
    private IVibratorService mService;
    private SettingsObserver mSettingObserver;
    private VivoRatioControllerUtilsImpl mVivoRatioControllerUtils;
    private VivoVibratorSwr mVivoVibratorSwr;
    private int mWaveformIncreasingCount;
    private int mWaveformIncreasingStrength;
    private static ArrayList<Integer> DUAL_EFFECTID = new ArrayList() { // from class: com.android.server.VivoVibratorServiceImpl.1
        {
            add(311);
            add(315);
            add(323);
            add(25001);
            add(26001);
            add(26002);
            add(26003);
            add(26004);
            add(26005);
            add(26006);
        }
    };
    private static final CaseInsensitiveMap EFFECTID_MAP = new CaseInsensitiveMap() { // from class: com.android.server.VivoVibratorServiceImpl.2
        {
            put("Array Mbira", Integer.valueOf((int) VivoWmsImpl.NOTIFY_SPLIT_BAR_LAYOUT));
            put("Blue Meteor Showers", (Integer) 302);
            put("Elec Synth", (Integer) 303);
            put("Harp Bell", (Integer) 304);
            put("High Didi", (Integer) 305);
            put("Labyrinth", (Integer) 306);
            put("Limpid Drop", (Integer) 307);
            put("Lovely Xylophone", (Integer) 308);
            put("Marimba", (Integer) 309);
            put("Occarina", (Integer) 310);
            put("Resound", (Integer) 311);
            put("Rhythm", (Integer) 312);
            put("Ripples", (Integer) 313);
            put("Scene play", (Integer) 314);
            put("Set out", (Integer) 315);
            put("Spacious", (Integer) 316);
            put("Spring Charm", (Integer) 317);
            put("Sunrise View", (Integer) 318);
            put("Sunrise View Dubstep", (Integer) 319);
            put("Sunrise View Harp", (Integer) 320);
            put("Sunrise View Lyric", (Integer) 321);
            put("Sunrise View Piano", (Integer) 322);
            put("Sunrise View Relax", (Integer) 323);
            put("Tunk", (Integer) 324);
            put("Vintage Ring", (Integer) 325);
            put("Xtreme Tone", (Integer) 326);
            put("Xylophone Roll", (Integer) 327);
            put("Xyl Roll", (Integer) 327);
            put("Fantasy Blue", (Integer) 328);
            put("Indomitable Will", (Integer) 329);
            put("Jovi Lifestyle", (Integer) 330);
            put("Jovi Lifestyle Full", (Integer) 331);
            put("Newborn", (Integer) 332);
            put("Andes", (Integer) 401);
            put("Bell", (Integer) 402);
            put("Bird", (Integer) 403);
            put("Bubble", (Integer) 404);
            put("Childlike", (Integer) 405);
            put("Circle", (Integer) 406);
            put("Cuckoo", (Integer) 407);
            put("Default", (Integer) 408);
            put("Dita", (Integer) 409);
            put("Dobe", (Integer) 410);
            put("Doda", (Integer) 411);
            put("DoReMi", (Integer) 412);
            put("Dust", (Integer) 413);
            put("Echo", (Integer) 414);
            put("Grain", (Integer) 415);
            put("Harmonic", (Integer) 416);
            put("Klock", (Integer) 417);
            put("Little", (Integer) 418);
            put("Money", (Integer) 419);
            put("MusicBox", (Integer) 420);
            put("Naughty", (Integer) 421);
            put("Peristalsis", (Integer) 422);
            put("Promote", (Integer) 423);
            put("Scale", (Integer) 424);
            put("Simple", (Integer) 425);
            put("Surprised", (Integer) 426);
            put("Theme", (Integer) 427);
            put("Twist", (Integer) 428);
            put("Unobtrusive", (Integer) 429);
            put("Whisper", Integer.valueOf((int) ProcessList.VERY_LASTEST_PREVIOUS_APP_ADJ));
            put("Whistle", (Integer) 431);
            put("Arrival", (Integer) 432);
            put("Emergence", (Integer) 433);
            put("Transformation", (Integer) 434);
            put("Happy", (Integer) 435);
            put("Arrangement", (Integer) 436);
            put("Encounter", (Integer) 437);
            put("Beautiful Touching", (Integer) 501);
            put("Clock Alert", (Integer) 502);
            put("Crisp ring", (Integer) 503);
            put("Cycle Oscillation", (Integer) 504);
            put("Early in the morning", (Integer) 505);
            put("Fine Day", (Integer) 506);
            put("Flush of dawn", (Integer) 507);
            put("Get Up Action", Integer.valueOf((int) VivoColorManagerService.VIVO_COLOR_MODE_AOD));
            put("Glassy Lustre", Integer.valueOf((int) VivoColorManagerService.VIVO_COLOR_MODE_FINGERPRINT));
            put("Lights", (Integer) 510);
            put("Moonlight", (Integer) 511);
            put("Morning scene", (Integer) 512);
            put("Sound of the sea", (Integer) 513);
            put("Thump", Integer.valueOf((int) Vendor2Config.DISPLAY_BRIGHTNESS));
            put("New world", Integer.valueOf((int) Vendor2Config.CM_COLOR_TEMP_MODE));
        }
    };
    private static final ArrayList<String> VIBRATOR_IME_LIST = new ArrayList<String>() { // from class: com.android.server.VivoVibratorServiceImpl.3
        {
            add(VivoVibratorServiceImpl.OCTOPUS);
            add(VivoVibratorServiceImpl.BAIDU);
            add(VivoVibratorServiceImpl.QQ_PINYIN);
            add(VivoVibratorServiceImpl.SOGOU);
            add(VivoVibratorServiceImpl.IFLYTEK);
        }
    };
    private volatile boolean mCurrVibProVibrating = false;
    boolean vibrateCycle = false;
    boolean vibrateMonitorSound = true;
    private int mEffectId = 0;
    private int mCurVibProPid = -1;
    private String ARG = "TrackState:";
    private AtomicBoolean mAudioVibratorFlag = new AtomicBoolean(false);
    private long vibratorStartTime = 0;
    private long vibratorTimes = 0;
    private Object mVCD = null;
    private AtomicBoolean audioRegisterFlg = new AtomicBoolean(false);
    int SupportTypeLinearMotor = SystemProperties.getInt("persist.vivo.support.lra", 0);
    int SupportDualLinearMotor = SystemProperties.getInt("persist.sys.vivo.support.double.lra", 0);
    private List<HashMap<String, String>> VIBRATOR_LIST = new ArrayList();
    private HashMap<String, String> VIBRATOR_MAP = new HashMap<>();
    private long mVCDStartTime = 0;
    private String mProjectName = SystemProperties.get("ro.boot.vivo.hardware.pcb.detect");
    private int mSupportIncreasingVibration = SystemProperties.getInt("persist.vivo.ascending.vibration", 0);
    private int mStartStrength = 1;
    private int mEndStrength = 255;
    private String mAppSharePackageName = null;
    private int mAppShareUserId = -1;
    private final Runnable mVibratorProEndRunnable = new Runnable() { // from class: com.android.server.VivoVibratorServiceImpl.6
        @Override // java.lang.Runnable
        public void run() {
            VSlog.e(VivoVibratorServiceImpl.TAG, "mVibratorProEndRunnable called ");
            VivoVibratorServiceImpl vivoVibratorServiceImpl = VivoVibratorServiceImpl.this;
            vivoVibratorServiceImpl.cancelVibPro(vivoVibratorServiceImpl.mCurVibProPid, null, 0);
        }
    };
    private final Runnable mCurrentVibStartRunnable = new Runnable() { // from class: com.android.server.VivoVibratorServiceImpl.7
        @Override // java.lang.Runnable
        public void run() {
            VSlog.e(VivoVibratorServiceImpl.TAG, "mCurrentVibStartRunnable called ");
            VivoVibratorServiceImpl.this.mCurrVibProVibrating = true;
        }
    };
    private final Runnable mVibratorCycleRunnable = new Runnable() { // from class: com.android.server.VivoVibratorServiceImpl.8
        @Override // java.lang.Runnable
        public void run() {
            VivoVibratorServiceImpl.this.mAudioVibratorFlag.set(false);
            VivoVibratorServiceImpl.this.mAudioFeatureCallback.startVibrate();
        }
    };
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.android.server.VivoVibratorServiceImpl.9
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) throws RemoteException {
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) throws RemoteException {
        }

        public void onProcessDied(int pid, int uid) throws RemoteException {
            VSlog.e(VivoVibratorServiceImpl.TAG, "onProcessDied pid: " + pid + ",uid: " + uid);
            if (pid == VivoVibratorServiceImpl.this.mCurVibProPid) {
                VivoVibratorServiceImpl vivoVibratorServiceImpl = VivoVibratorServiceImpl.this;
                vivoVibratorServiceImpl.cancelVibPro(vivoVibratorServiceImpl.mCurVibProPid, null, 0);
            }
        }
    };
    private final Runnable mWaveformVibratorIncreasingRunnable = new Runnable() { // from class: com.android.server.VivoVibratorServiceImpl.10
        @Override // java.lang.Runnable
        public void run() {
            VivoVibratorServiceImpl.this.setWaveformVibratorIncreasing();
            if (VivoVibratorServiceImpl.this.mWaveformIncreasingStrength < VivoVibratorServiceImpl.this.mEndStrength) {
                VivoVibratorServiceImpl.this.mH.postDelayed(VivoVibratorServiceImpl.this.mWaveformVibratorIncreasingRunnable, 800L);
            }
        }
    };

    static native void vibratorExtCancel();

    static native boolean vibratorExtExists();

    static native long vibratorExtGameVibrate(int i, long j);

    static native void vibratorExtInit();

    static native boolean vibratorExtIsSupportAmplitudeControl();

    static native void vibratorExtOn(long j);

    static native long vibratorExtPerformEffect(long j, int i);

    static native void vibratorExtSetAmplitude(int i);

    static native void vibratorExtSetExternalControl(boolean z);

    static native void vibratorExtSetTriggerIntensity(int i);

    static native boolean vibratorExtSupportsExternalControl();

    static native long vibratorExtVibrate(int i, long j, int i2, long j2);

    static native void vibratorProCancel();

    static native long vibratorProGameVibrate(int i, long j);

    /* JADX INFO: Access modifiers changed from: package-private */
    public static native long vibratorProGetWavEffectSizeBytes(String str);

    static native boolean vibratorProIsEffectNoSupported(int i);

    static native boolean vibratorProIsSupportAmplitudeControl();

    static native void vibratorProSetAmplitude(int i);

    static native void vibratorProSetExternalControl(boolean z);

    static native void vibratorProSetMountingAmplitude(int i);

    static native void vibratorProSetTriggerIntensity(int i);

    static native boolean vibratorProSupportsExternalControl();

    static native long vibratorProVibrate(int i, long j, int i2, long j2);

    public VivoVibratorServiceImpl(Handler handler, Context context) {
        this.mH = handler;
        this.mContext = context;
        this.mSettingObserver = new SettingsObserver(this.mH);
        registerVibratorIntensity();
        this.mAudioFeatureCallback = new AudioFeatureCallback(this.mContext, this.ARG, new Object());
        this.mAudioFeatures = new AudioFeatures(this.mContext, this.ARG, (Object) null);
        registerProcessObserver();
        updateVibratorIntensity(true);
        HandlerThread handlerThread = new HandlerThread(VCD_THREAD_NAME);
        this.mCDThread = handlerThread;
        handlerThread.start();
        this.mCDHandler = new CDHandler(this.mCDThread.getLooper());
        this.mVivoVibratorSwr = new VivoVibratorSwr();
        initAppShareController();
        this.mVivoRatioControllerUtils = VivoRatioControllerUtilsImpl.getInstance();
    }

    public boolean isSupportAmplitudeControlVibPro(int deviceID) {
        StringBuilder sb;
        VSlog.e(TAG, "isSupportAmplitudeControlVibPro called");
        boolean isSupportAmplitudeControl = false;
        try {
            try {
                if (this.SupportTypeLinearMotor != 0) {
                    boolean z = true;
                    if (deviceID != 0) {
                        if (deviceID == 1) {
                            isSupportAmplitudeControl = vibratorProIsSupportAmplitudeControl();
                            VSlog.e(TAG, "isSupportAmplitudeControlVibPro bottom=" + isSupportAmplitudeControl);
                        } else if (deviceID == 2) {
                            isSupportAmplitudeControl = vibratorExtIsSupportAmplitudeControl();
                            VSlog.e(TAG, "isSupportAmplitudeControlVibPro top=" + isSupportAmplitudeControl);
                        } else {
                            isSupportAmplitudeControl = vibratorProIsSupportAmplitudeControl();
                            VSlog.e(TAG, "isSupportAmplitudeControlVibPro default=" + isSupportAmplitudeControl);
                        }
                    } else {
                        if (!vibratorProIsSupportAmplitudeControl() || !vibratorExtIsSupportAmplitudeControl()) {
                            z = false;
                        }
                        isSupportAmplitudeControl = z;
                        VSlog.e(TAG, "isSupportAmplitudeControlVibPro dual=" + isSupportAmplitudeControl);
                    }
                }
                sb = new StringBuilder();
            } catch (Exception e) {
                VSlog.e(TAG, "isSupportAmplitudeControlVibPro called error; " + e);
                sb = new StringBuilder();
            }
        } catch (Throwable th) {
            sb = new StringBuilder();
        }
        sb.append("isSupportAmplitudeControlVibPro=");
        sb.append(isSupportAmplitudeControl);
        VSlog.e(TAG, sb.toString());
        return isSupportAmplitudeControl;
    }

    public void setAmplitudeVibPro(int amplitude, int deviceID) {
        VSlog.e(TAG, "setAmplitudeVibPro called, amplitude: " + amplitude + "deviceID : " + deviceID);
        if (amplitude <= 0 || amplitude > 255) {
            VSlog.e(TAG, "error amplitude: " + amplitude + " . amplitude value must in (0,255] ");
            return;
        }
        try {
            if (this.SupportTypeLinearMotor != 0) {
                if (deviceID == 0) {
                    vibratorProSetAmplitude(amplitude);
                    vibratorExtSetAmplitude(amplitude);
                    VSlog.e(TAG, "setAmplitudeVibPro dual=" + amplitude);
                } else if (deviceID == 1) {
                    vibratorProSetAmplitude(amplitude);
                    VSlog.e(TAG, "setAmplitudeVibPro bottom=" + amplitude);
                } else if (deviceID == 2) {
                    vibratorExtSetAmplitude(amplitude);
                    VSlog.e(TAG, "setAmplitudeVibPro top=" + amplitude);
                } else {
                    vibratorProSetAmplitude(amplitude);
                    VSlog.e(TAG, "setAmplitudeVibPro default=" + amplitude);
                }
            }
        } catch (Exception e) {
            VSlog.e(TAG, "setAmplitudeVibPro called error; " + e);
        }
    }

    public void setIncreasingAmplitudeVibPro(int amplitude) {
        VSlog.e(TAG, "setIncreasingAmplitudeVibPro called, amplitude: " + amplitude);
        if (amplitude <= 0 || amplitude > 255) {
            VSlog.e(TAG, "error amplitude: " + amplitude + " . amplitude value must in (0,255] ");
            return;
        }
        try {
            if (this.SupportTypeLinearMotor != 0 && 1 == this.mSupportIncreasingVibration) {
                vibratorProSetMountingAmplitude(amplitude);
            }
        } catch (Exception e) {
            VSlog.e(TAG, "setIncreasingAmplitudeVibPro called error; " + e);
        }
    }

    public long vibrateVibPro(int pid, final String opPkg, final int effectID, final long timeoutMs, int strength, boolean hasSysVibrate, VibrationEffect mCurrentVibration_effect, IBinder mCurrentVibration_token, final int deviceID) {
        VSlog.e(TAG, "vibrate pro pid: " + pid + " ,opPkg: " + opPkg + " ,effectID: " + effectID + " ,timeoutMs: " + timeoutMs + " ,strength: " + strength + " deviceID :" + deviceID);
        if (-1 == effectID && -1 == timeoutMs) {
            return 0L;
        }
        if (!isSameVibratorType(effectID) || SystemClock.elapsedRealtime() - this.vibratorStartTime >= obtaionMinWaitTime(effectID) || ((opPkg == null || !opPkg.equals(this.mCurrentPkg)) && (opPkg != null || this.mCurrentPkg != null))) {
            boolean stopCurrentVibrator = whetherToStopCurrentVibrator(mCurrentVibration_effect, effectID, hasSysVibrate);
            if (stopCurrentVibrator) {
                this.mH.removeCallbacks(this.mVibratorProEndRunnable);
                if (this.mCurrVibProVibrating) {
                    cancelVibPro(this.mCurVibProPid, null, 0);
                } else if (hasSysVibrate) {
                    cancelSystemAcousticVibrator(mCurrentVibration_token);
                }
                this.vibratorStartTime = SystemClock.elapsedRealtime();
                this.mCurrentPkg = opPkg;
                VSlog.d(TAG, "hapticFeedbackIntensity: " + this.mHapticFeedbackIntensity + "notificationIntensity: " + this.mNotificationIntensity + "ringInensity: " + this.mRingIntensity);
                final int vibrateStrength = accordingToCustomJudge(effectID, strength);
                StringBuilder sb = new StringBuilder();
                sb.append("vibrateStrength: ");
                sb.append(vibrateStrength);
                VSlog.d(TAG, sb.toString());
                if (!this.mAudioFeatureCallback.vibrate(pid, opPkg, effectID, timeoutMs, vibrateStrength, deviceID)) {
                    this.mEffectId = changeEffectID(effectID);
                    this.mCurVibProPid = pid;
                    this.mH.post(this.mCurrentVibStartRunnable);
                    new Thread(new Runnable() { // from class: com.android.server.VivoVibratorServiceImpl.4
                        @Override // java.lang.Runnable
                        public void run() {
                            int mDeviceId = deviceID;
                            long vibratorDuration = -1;
                            try {
                                if (VivoVibratorServiceImpl.this.SupportTypeLinearMotor != 0) {
                                    if (VivoVibratorServiceImpl.this.isEffectSupportDualVib(VivoVibratorServiceImpl.this.mEffectId)) {
                                        mDeviceId = 0;
                                    }
                                    if (mDeviceId == 0) {
                                        long topVibratorDuration = VivoVibratorServiceImpl.this.mVivoVibratorSwr.isSupportSwr(VivoVibratorServiceImpl.this.mEffectId, 2) ? VivoVibratorServiceImpl.this.mVivoVibratorSwr.setSwrParameters(VivoVibratorServiceImpl.this.mEffectId, vibrateStrength) : VivoVibratorServiceImpl.vibratorExtVibrate(VivoVibratorServiceImpl.this.mEffectId, timeoutMs, vibrateStrength, SystemClock.elapsedRealtime());
                                        long bottomVibratorDuration = VivoVibratorServiceImpl.this.mVivoVibratorSwr.isSupportSwr(VivoVibratorServiceImpl.this.mEffectId, 1) ? VivoVibratorServiceImpl.this.mVivoVibratorSwr.setSwrParameters(VivoVibratorServiceImpl.this.mEffectId, vibrateStrength) : VivoVibratorServiceImpl.vibratorProVibrate(VivoVibratorServiceImpl.this.mEffectId, timeoutMs, vibrateStrength, SystemClock.elapsedRealtime());
                                        vibratorDuration = Math.max(topVibratorDuration, bottomVibratorDuration);
                                        VSlog.e(VivoVibratorServiceImpl.TAG, "vibrateVibPro dual=" + vibratorDuration);
                                    } else if (mDeviceId == 1) {
                                        vibratorDuration = VivoVibratorServiceImpl.this.mVivoVibratorSwr.isSupportSwr(VivoVibratorServiceImpl.this.mEffectId, 1) ? VivoVibratorServiceImpl.this.mVivoVibratorSwr.setSwrParameters(VivoVibratorServiceImpl.this.mEffectId, vibrateStrength) : VivoVibratorServiceImpl.vibratorProVibrate(VivoVibratorServiceImpl.this.mEffectId, timeoutMs, vibrateStrength, SystemClock.elapsedRealtime());
                                        VSlog.e(VivoVibratorServiceImpl.TAG, "vibrateVibPro bottom=" + vibratorDuration);
                                    } else if (mDeviceId != 2) {
                                        vibratorDuration = VivoVibratorServiceImpl.vibratorProVibrate(VivoVibratorServiceImpl.this.mEffectId, timeoutMs, vibrateStrength, SystemClock.elapsedRealtime());
                                        VSlog.e(VivoVibratorServiceImpl.TAG, "vibrateVibPro default=" + vibratorDuration);
                                    } else {
                                        vibratorDuration = VivoVibratorServiceImpl.this.mVivoVibratorSwr.isSupportSwr(VivoVibratorServiceImpl.this.mEffectId, 2) ? VivoVibratorServiceImpl.this.mVivoVibratorSwr.setSwrParameters(VivoVibratorServiceImpl.this.mEffectId, vibrateStrength) : VivoVibratorServiceImpl.vibratorExtVibrate(VivoVibratorServiceImpl.this.mEffectId, timeoutMs, vibrateStrength, SystemClock.elapsedRealtime());
                                        VSlog.e(VivoVibratorServiceImpl.TAG, "vibrateVibPro top=" + vibratorDuration);
                                    }
                                    VivoVibratorServiceImpl.this.vibratorTimes = vibratorDuration;
                                    if (VivoVibratorServiceImpl.this.mEffectId != -1) {
                                        VivoVibratorServiceImpl.this.mH.postDelayed(VivoVibratorServiceImpl.this.mVibratorProEndRunnable, vibratorDuration);
                                    } else {
                                        VivoVibratorServiceImpl.this.mH.postDelayed(VivoVibratorServiceImpl.this.mVibratorProEndRunnable, timeoutMs);
                                    }
                                }
                            } catch (Exception e) {
                                VivoVibratorServiceImpl vivoVibratorServiceImpl = VivoVibratorServiceImpl.this;
                                vivoVibratorServiceImpl.cancelVibPro(vivoVibratorServiceImpl.mCurVibProPid, null, 0);
                                VSlog.e(VivoVibratorServiceImpl.TAG, "vibratorProVibrate called error; " + e);
                            }
                            int i = effectID;
                            if (i == -1 || i == 0) {
                                VivoVibratorServiceImpl.this.prepareToSendMessage(opPkg, timeoutMs, vibrateStrength, effectID);
                            } else if (vibratorDuration != 0) {
                                VivoVibratorServiceImpl vivoVibratorServiceImpl2 = VivoVibratorServiceImpl.this;
                                vivoVibratorServiceImpl2.prepareToSendMessage(opPkg, vibratorDuration, vibrateStrength, vivoVibratorServiceImpl2.mEffectId);
                            } else {
                                VSlog.e(VivoVibratorServiceImpl.TAG, "vibratorProVibrate error,maybe no effectID");
                            }
                        }
                    }).start();
                    return 0L;
                }
                VSlog.e(TAG, "mAudioSyncVibrator process!");
                return 0L;
            }
            VSlog.d(TAG, "current has higher priority vibration,return");
            return 0L;
        }
        VSlog.d(TAG, "last vibration time: " + this.vibratorStartTime + "current vibration time: " + SystemClock.elapsedRealtime() + "The current vibration and the last vibration time interval are too short,return");
        return 0L;
    }

    public boolean isEffectIdSupported(int effectID) {
        if (this.SupportTypeLinearMotor != 0) {
            return vibratorProIsEffectNoSupported(effectID);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isEffectSupportDualVib(int effectID) {
        return this.SupportDualLinearMotor == 1 && DUAL_EFFECTID.contains(Integer.valueOf(effectID));
    }

    private int changeEffectID(int effectID) {
        int intForUser = Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_intensity", 2, -2);
        this.mHapticFeedbackIntensity = intForUser;
        if (1 != this.SupportTypeLinearMotor || 133 != effectID) {
            return effectID;
        }
        if (intForUser != 1) {
            if (intForUser != 2) {
                if (intForUser != 3) {
                    return effectID;
                }
                return 134;
            }
            return 133;
        }
        return 132;
    }

    public long gameVibrateVibPro(int pid, final String opPkg, final int effectID, boolean hasSysVibrate, VibrationEffect mCurrentVibration_effect, IBinder mCurrentVibration_token, final int deviceID) {
        VSlog.e(TAG, "game vibrate pro pid: " + pid + " ,opPkg: " + opPkg + " ,effectID: " + effectID + ", deviceID : " + deviceID);
        if (effectID < 0) {
            return 0L;
        }
        boolean stopCurrentVibrator = whetherToStopCurrentVibrator(mCurrentVibration_effect, effectID, hasSysVibrate);
        if (stopCurrentVibrator) {
            if (this.mCurrVibProVibrating) {
                cancelVibPro(this.mCurVibProPid, null, 0);
            } else if (hasSysVibrate) {
                cancelSystemAcousticVibrator(mCurrentVibration_token);
            }
            this.mH.post(this.mCurrentVibStartRunnable);
            this.mEffectId = effectID;
            this.mCurVibProPid = pid;
            new Thread(new Runnable() { // from class: com.android.server.VivoVibratorServiceImpl.5
                @Override // java.lang.Runnable
                public void run() {
                    long topVibratorDuration;
                    long bottomVibratorDuration;
                    long vibratorDuration;
                    int mDeviceId = deviceID;
                    try {
                        if (VivoVibratorServiceImpl.this.isEffectSupportDualVib(effectID)) {
                            mDeviceId = 0;
                        }
                        if (VivoVibratorServiceImpl.this.SupportTypeLinearMotor != 0) {
                            if (mDeviceId == 0) {
                                if (VivoVibratorServiceImpl.this.mVivoVibratorSwr.isSupportSwr(effectID, 2)) {
                                    topVibratorDuration = VivoVibratorServiceImpl.this.mVivoVibratorSwr.setSwrParameters(effectID);
                                } else {
                                    topVibratorDuration = VivoVibratorServiceImpl.vibratorExtGameVibrate(effectID, SystemClock.elapsedRealtime());
                                }
                                if (VivoVibratorServiceImpl.this.mVivoVibratorSwr.isSupportSwr(effectID, 1)) {
                                    bottomVibratorDuration = VivoVibratorServiceImpl.this.mVivoVibratorSwr.setSwrParameters(effectID);
                                } else {
                                    bottomVibratorDuration = VivoVibratorServiceImpl.vibratorProGameVibrate(effectID, SystemClock.elapsedRealtime());
                                }
                                vibratorDuration = Math.max(topVibratorDuration, bottomVibratorDuration);
                                VSlog.e(VivoVibratorServiceImpl.TAG, "gameVibrateVibPro dual=" + vibratorDuration);
                            } else if (mDeviceId == 1) {
                                if (VivoVibratorServiceImpl.this.mVivoVibratorSwr.isSupportSwr(effectID, 1)) {
                                    vibratorDuration = VivoVibratorServiceImpl.this.mVivoVibratorSwr.setSwrParameters(effectID);
                                } else {
                                    vibratorDuration = VivoVibratorServiceImpl.vibratorProGameVibrate(effectID, SystemClock.elapsedRealtime());
                                }
                                VSlog.e(VivoVibratorServiceImpl.TAG, "gameVibrateVibPro bottom=" + vibratorDuration);
                            } else if (mDeviceId == 2) {
                                if (VivoVibratorServiceImpl.this.mVivoVibratorSwr.isSupportSwr(effectID, 2)) {
                                    vibratorDuration = VivoVibratorServiceImpl.this.mVivoVibratorSwr.setSwrParameters(effectID);
                                } else {
                                    vibratorDuration = VivoVibratorServiceImpl.vibratorExtGameVibrate(effectID, SystemClock.elapsedRealtime());
                                }
                                VSlog.e(VivoVibratorServiceImpl.TAG, "gameVibrateVibPro top=" + vibratorDuration);
                            } else {
                                vibratorDuration = VivoVibratorServiceImpl.vibratorProGameVibrate(effectID, SystemClock.elapsedRealtime());
                                VSlog.e(VivoVibratorServiceImpl.TAG, "gameVibrateVibPro default=" + vibratorDuration);
                            }
                            if (vibratorDuration != 0) {
                                VivoVibratorServiceImpl.this.mH.postDelayed(VivoVibratorServiceImpl.this.mVibratorProEndRunnable, vibratorDuration);
                                VivoVibratorServiceImpl.this.prepareToSendMessage(opPkg, vibratorDuration, 255, effectID);
                                return;
                            }
                            VSlog.e(VivoVibratorServiceImpl.TAG, "vibratorProGameVibrate error,maybe no effectID");
                        }
                    } catch (Exception e) {
                        VivoVibratorServiceImpl vivoVibratorServiceImpl = VivoVibratorServiceImpl.this;
                        vivoVibratorServiceImpl.cancelVibPro(vivoVibratorServiceImpl.mCurVibProPid, null, 0);
                        VSlog.e(VivoVibratorServiceImpl.TAG, "vibratorProGameVibrate called error; " + e);
                    }
                }
            }).start();
            return 0L;
        }
        VSlog.d(TAG, "current has higher priority vibration,return");
        return 0L;
    }

    public boolean isSameVibratorType(int effectID) {
        if (isRingtone(this.mEffectId) && isRingtone(effectID)) {
            return true;
        }
        if (isAlarm(this.mEffectId) && isAlarm(effectID)) {
            return true;
        }
        if (isNotification(this.mEffectId) && isNotification(effectID)) {
            return true;
        }
        return isOtherType(this.mEffectId) && isOtherType(effectID);
    }

    private long obtaionMinWaitTime(int effectID) {
        if (effectID > 200 && effectID < 300) {
            return 40L;
        }
        long j = this.vibratorTimes;
        if (j > 5 && j < 15) {
            return j + 20;
        }
        return 30L;
    }

    public long ringVibPro(int pid, String opPkg, String ringStr, boolean isCycle, boolean monitorSound, boolean hasSysVibrate, VibrationEffect mCurrentVibration_effect, IBinder mCurrentVibration_token, int deviceID) {
        String ringStr2 = ringStr;
        if (TextUtils.isEmpty(ringStr)) {
            VSlog.e(TAG, "The ringStr is null return playMillis: -1");
            return -1L;
        }
        VSlog.e(TAG, "ringStr is : " + ringStr2 + ", deviceID : " + deviceID + ", isCycle=" + isCycle + ", monitorSound=" + monitorSound);
        int index = ringStr2.indexOf(".");
        if (index != -1) {
            ringStr2 = ringStr2.substring(0, index);
        }
        Integer effectID = EFFECTID_MAP.get(ringStr2.replaceAll("[^(A-Za-z)]", " ").trim());
        VSlog.e(TAG, "effectId : " + effectID);
        if (effectID == null) {
            VSlog.e(TAG, "the ringStr is error return playMillis: -1");
            return -1L;
        } else if (effectID.intValue() <= 300 || effectID.intValue() > 600) {
            return -1L;
        } else {
            this.vibrateCycle = isCycle;
            this.vibrateMonitorSound = monitorSound;
            long playMillis = vibrateVibPro(pid, opPkg, effectID.intValue(), -1L, -1, hasSysVibrate, mCurrentVibration_effect, mCurrentVibration_token, deviceID);
            return playMillis;
        }
    }

    public void cancelVibPro(int pid, String opPkg, int deviceID) {
        VSlog.e(TAG, "cancelVibPro called, uid: " + pid + ", mPkgName: " + opPkg + ", deviceID=" + deviceID);
        if (pid == this.mCurVibProPid) {
            this.mAudioFeatureCallback.cancel();
            if (this.mCurrVibProVibrating) {
                this.mH.removeCallbacks(this.mVibratorProEndRunnable);
                this.mH.removeCallbacks(this.mVibratorCycleRunnable);
                try {
                    try {
                        if (this.SupportTypeLinearMotor != 0) {
                            if (this.mVivoVibratorSwr.isSupportSwr(this.mEffectId, 1) || this.mVivoVibratorSwr.isSupportSwr(this.mEffectId, 2)) {
                                this.mVivoVibratorSwr.setSwrParameters("stop");
                            }
                            if (deviceID == 0) {
                                vibratorProCancel();
                                vibratorExtCancel();
                                VSlog.e(TAG, "cancelVibPro dual=" + deviceID);
                            } else if (deviceID == 1) {
                                vibratorProCancel();
                                VSlog.e(TAG, "cancelVibPro bottom=" + deviceID);
                            } else if (deviceID == 2) {
                                vibratorExtCancel();
                                VSlog.e(TAG, "cancelVibPro top=" + deviceID);
                            } else {
                                vibratorProCancel();
                                vibratorExtCancel();
                                VSlog.e(TAG, "cancelVibPro default=" + deviceID);
                            }
                        }
                    } catch (Exception e) {
                        VSlog.e(TAG, "vibratorProCancel called error; " + e);
                    }
                    return;
                } finally {
                    this.mCurrVibProVibrating = false;
                    this.mEffectId = 0;
                    this.mCurVibProPid = -1;
                }
            }
            return;
        }
        VSlog.e(TAG, "You can only cancel your own vibration,current uid: " + this.mCurVibProPid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AudioFeatureCallback extends AudioFeatures.AudioFeatureCallback {
        private int mDeviceID;
        private int mEffectID;
        private int mIncreasingCount;
        private int mIncreasingStrength;
        private String mOpPkg;
        private int mStrength;
        private long mTimeoutMs;
        private boolean mVibratorCycle;
        private final Runnable mVibratorIncreasingRunnable;
        private int mixNumber;
        private boolean mvibrator;
        private long startTime;

        AudioFeatureCallback(Context context, String arg0, Object obj) {
            super(context, arg0, obj);
            this.mvibrator = false;
            this.mixNumber = 0;
            this.mVibratorCycle = false;
            this.mVibratorIncreasingRunnable = new Runnable() { // from class: com.android.server.VivoVibratorServiceImpl.AudioFeatureCallback.1
                @Override // java.lang.Runnable
                public void run() {
                    AudioFeatureCallback.this.setVibratorIncreasing();
                    if (AudioFeatureCallback.this.mIncreasingStrength < VivoVibratorServiceImpl.this.mEndStrength) {
                        VivoVibratorServiceImpl.this.mH.postDelayed(AudioFeatureCallback.this.mVibratorIncreasingRunnable, 800L);
                    }
                }
            };
        }

        public String onCallback(String arg0, Object obj) {
            VSlog.e(VivoVibratorServiceImpl.TAG, "onCallback arg0=" + arg0);
            try {
            } catch (NumberFormatException e) {
                e = e;
            }
            if (!TextUtils.isEmpty(arg0) && arg0.startsWith("TrackState:")) {
                String subStr = arg0.substring("TrackState:".length());
                String[] split = subStr.split(";");
                if (split.length >= 3) {
                    String pidStr = split[0].split("=")[1];
                    String streamTypeStr = split[1].split("=")[1];
                    String playStateStr = split[2].split("=")[1];
                    VSlog.d(VivoVibratorServiceImpl.TAG, "pid: " + pidStr + ", type: " + streamTypeStr + ", state: " + playStateStr);
                    int pid = Integer.valueOf(pidStr).intValue();
                    int streamType = Integer.valueOf(streamTypeStr).intValue();
                    int playState = Integer.valueOf(playStateStr).intValue();
                    VSlog.d(VivoVibratorServiceImpl.TAG, "onCallback->pid: " + pid + ", streamType: " + streamType + ", playState: " + playState);
                    try {
                        if (audioCallbackTypeMatch(streamType) && playState == 1) {
                            startVibrate();
                        }
                    } catch (NumberFormatException e2) {
                        e = e2;
                        VSlog.e(VivoVibratorServiceImpl.TAG, "AudioFeatureCallback error", e);
                        return null;
                    }
                }
                return null;
            }
            return null;
        }

        private boolean audioCallbackTypeMatch(int streamType) {
            if (streamType == 2 && VivoVibratorServiceImpl.this.isRingtone(this.mEffectID)) {
                return true;
            }
            if (streamType == 4 && VivoVibratorServiceImpl.this.isAlarm(this.mEffectID)) {
                return true;
            }
            if (streamType == 5 && VivoVibratorServiceImpl.this.isNotification(this.mEffectID)) {
                return true;
            }
            return false;
        }

        public void startVibrate() {
            long vibratorDuration;
            VSlog.e(VivoVibratorServiceImpl.TAG, "startVibrate mAudioVibratorFlag=" + VivoVibratorServiceImpl.this.mAudioVibratorFlag.get());
            if (VivoVibratorServiceImpl.this.mAudioVibratorFlag.compareAndSet(false, true)) {
                VivoVibratorServiceImpl.this.mH.removeCallbacks(VivoVibratorServiceImpl.this.mVibratorProEndRunnable);
                VivoVibratorServiceImpl.this.mH.removeCallbacks(VivoVibratorServiceImpl.this.mVibratorCycleRunnable);
                if (this.startTime == 0) {
                    this.startTime = SystemClock.elapsedRealtime();
                }
                try {
                    if (VivoVibratorServiceImpl.this.SupportTypeLinearMotor != 0) {
                        if (VivoVibratorServiceImpl.this.isEffectSupportDualVib(this.mEffectID)) {
                            this.mDeviceID = 0;
                        }
                        int i = this.mDeviceID;
                        if (i == 0) {
                            long topVibratorDuration = VivoVibratorServiceImpl.this.mVivoVibratorSwr.isSupportSwr(this.mEffectID, 2) ? VivoVibratorServiceImpl.this.mVivoVibratorSwr.setSwrParameters(this.mEffectID, this.mStrength) : VivoVibratorServiceImpl.vibratorExtVibrate(this.mEffectID, this.mTimeoutMs, this.mStrength, SystemClock.elapsedRealtime());
                            long bottomVibratorDuration = VivoVibratorServiceImpl.this.mVivoVibratorSwr.isSupportSwr(this.mEffectID, 1) ? VivoVibratorServiceImpl.this.mVivoVibratorSwr.setSwrParameters(this.mEffectID, this.mStrength) : VivoVibratorServiceImpl.vibratorProVibrate(this.mEffectID, this.mTimeoutMs, this.mStrength, SystemClock.elapsedRealtime());
                            vibratorDuration = Math.max(topVibratorDuration, bottomVibratorDuration);
                            VSlog.e(VivoVibratorServiceImpl.TAG, "vibrateVibPro dual=" + vibratorDuration);
                        } else if (i == 1) {
                            vibratorDuration = VivoVibratorServiceImpl.this.mVivoVibratorSwr.isSupportSwr(this.mEffectID, 1) ? VivoVibratorServiceImpl.this.mVivoVibratorSwr.setSwrParameters(this.mEffectID, this.mStrength) : VivoVibratorServiceImpl.vibratorProVibrate(this.mEffectID, this.mTimeoutMs, this.mStrength, SystemClock.elapsedRealtime());
                            VSlog.e(VivoVibratorServiceImpl.TAG, "vibrateVibPro bottom=" + vibratorDuration);
                        } else if (i != 2) {
                            vibratorDuration = VivoVibratorServiceImpl.vibratorProVibrate(this.mEffectID, this.mTimeoutMs, this.mStrength, SystemClock.elapsedRealtime());
                            VSlog.e(VivoVibratorServiceImpl.TAG, "vibrateVibPro default=" + vibratorDuration);
                        } else {
                            vibratorDuration = VivoVibratorServiceImpl.this.mVivoVibratorSwr.isSupportSwr(this.mEffectID, 2) ? VivoVibratorServiceImpl.this.mVivoVibratorSwr.setSwrParameters(this.mEffectID, this.mStrength) : VivoVibratorServiceImpl.vibratorExtVibrate(this.mEffectID, this.mTimeoutMs, this.mStrength, SystemClock.elapsedRealtime());
                            VSlog.e(VivoVibratorServiceImpl.TAG, "vibrateVibPro top=" + vibratorDuration);
                        }
                        VSlog.e(VivoVibratorServiceImpl.TAG, "vibrator time :" + vibratorDuration);
                        if (isIncreasingVibration(this.mEffectID) && this.mixNumber == 0) {
                            setIncreasingStrengthRange(this.mStrength);
                            VivoVibratorServiceImpl.this.mH.post(this.mVibratorIncreasingRunnable);
                        }
                        if (this.mVibratorCycle && this.mixNumber < 20) {
                            this.mixNumber++;
                            VivoVibratorServiceImpl.this.mH.postDelayed(VivoVibratorServiceImpl.this.mVibratorCycleRunnable, 100 + vibratorDuration);
                        } else if (vibratorDuration != 0) {
                            VivoVibratorServiceImpl.this.mH.postDelayed(VivoVibratorServiceImpl.this.mVibratorProEndRunnable, vibratorDuration);
                        } else {
                            VSlog.e(VivoVibratorServiceImpl.TAG, "startVibrator error,maybe no effectID");
                            this.startTime = 0L;
                            VivoVibratorServiceImpl.this.cancelVibPro(VivoVibratorServiceImpl.this.mCurVibProPid, null, 0);
                        }
                    }
                } catch (Exception e) {
                    VivoVibratorServiceImpl vivoVibratorServiceImpl = VivoVibratorServiceImpl.this;
                    vivoVibratorServiceImpl.cancelVibPro(vivoVibratorServiceImpl.mCurVibProPid, null, 0);
                    VSlog.e(VivoVibratorServiceImpl.TAG, "vibratorProVibrate called error; " + e);
                }
            }
        }

        public int getRingOrAlarmVolume(int effectID) {
            int volume = -1;
            AudioManager audioManager = (AudioManager) VivoVibratorServiceImpl.this.mContext.getSystemService("audio");
            if (audioManager != null) {
                if (effectID > 300 && effectID <= 400) {
                    volume = audioManager.getStreamVolume(2);
                } else if (effectID > 400 && effectID <= 500) {
                    volume = audioManager.getStreamVolume(5);
                } else if (effectID > 500 && effectID <= 600) {
                    volume = audioManager.getStreamVolume(4);
                }
            }
            VSlog.e(VivoVibratorServiceImpl.TAG, "volume: " + volume);
            return volume;
        }

        public boolean vibrate(int pid, String opPkg, int effectID, long timeoutMs, int strength, int deviceID) {
            if (effectID > 300 && effectID <= 600) {
                int volume = getRingOrAlarmVolume(effectID);
                this.mOpPkg = opPkg;
                this.mEffectID = effectID;
                this.mTimeoutMs = timeoutMs;
                this.mStrength = strength;
                this.mDeviceID = deviceID;
                this.startTime = 0L;
                this.mixNumber = 0;
                this.mVibratorCycle = VivoVibratorServiceImpl.this.vibrateCycle;
                this.mvibrator = true;
                VivoVibratorServiceImpl.this.mCurrVibProVibrating = true;
                VivoVibratorServiceImpl.this.mEffectId = this.mEffectID;
                VivoVibratorServiceImpl.this.mCurVibProPid = pid;
                if (volume != 0 && VivoVibratorServiceImpl.this.vibrateMonitorSound) {
                    if (VivoVibratorServiceImpl.this.audioRegisterFlg.compareAndSet(false, true)) {
                        VivoVibratorServiceImpl.this.mAudioFeatures.registerAudioFeatureCallback(VivoVibratorServiceImpl.this.mAudioFeatureCallback, VivoVibratorServiceImpl.this.ARG, (Object) null);
                    }
                    VivoVibratorServiceImpl.this.mH.postDelayed(VivoVibratorServiceImpl.this.mVibratorProEndRunnable, 1000L);
                } else {
                    startVibrate();
                }
                return true;
            }
            VSlog.e(VivoVibratorServiceImpl.TAG, "effect " + effectID + " not ring sound");
            return false;
        }

        public void cancel() {
            this.mVibratorCycle = false;
            VivoVibratorServiceImpl.this.mAudioVibratorFlag.set(false);
            if (this.mvibrator) {
                this.mvibrator = false;
                VivoVibratorServiceImpl.this.mH.removeCallbacks(this.mVibratorIncreasingRunnable);
                if (VivoVibratorServiceImpl.this.audioRegisterFlg.compareAndSet(true, false)) {
                    VivoVibratorServiceImpl.this.mAudioFeatures.unregisterAudioFeatureCallback(VivoVibratorServiceImpl.this.mAudioFeatureCallback, VivoVibratorServiceImpl.this.ARG, (Object) null);
                }
                if (this.startTime != 0) {
                    long time = (SystemClock.elapsedRealtime() - this.startTime) / 3;
                    VivoVibratorServiceImpl.this.prepareToSendMessage(this.mOpPkg, time, this.mStrength, this.mEffectID);
                }
                this.mEffectID = -1;
            }
        }

        private void setIncreasingStrengthRange(int strength) {
            this.mIncreasingCount = 0;
            this.mIncreasingStrength = 0;
            if (strength == 1) {
                VivoVibratorServiceImpl.this.mStartStrength = 51;
                VivoVibratorServiceImpl.this.mEndStrength = 128;
            } else if (strength != VivoVibratorServiceImpl.MID_VIBRATOR_INTENSITY) {
                VivoVibratorServiceImpl.this.mStartStrength = VivoVibratorServiceImpl.MID_START_STRENTH;
                VivoVibratorServiceImpl.this.mEndStrength = 255;
            } else {
                VivoVibratorServiceImpl.this.mStartStrength = VivoVibratorServiceImpl.MID_START_STRENTH;
                VivoVibratorServiceImpl.this.mEndStrength = 200;
            }
        }

        private boolean isIncreasingVibration(int effectID) {
            VSlog.e(VivoVibratorServiceImpl.TAG, "mSupportIncreasingVibration=" + VivoVibratorServiceImpl.this.mSupportIncreasingVibration + ", mIncreasingVibrationEnable=" + VivoVibratorServiceImpl.this.mIncreasingVibrationEnable);
            return 1 == VivoVibratorServiceImpl.this.mSupportIncreasingVibration && VivoVibratorServiceImpl.this.isRingtone(effectID) && 1 == VivoVibratorServiceImpl.this.mIncreasingVibrationEnable;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setVibratorIncreasing() {
            if (this.mIncreasingStrength == VivoVibratorServiceImpl.this.mEndStrength) {
                VSlog.e(VivoVibratorServiceImpl.TAG, "setVibratorIncreasing arrive EndStrength");
                return;
            }
            int increasingStrength = getIncreasingStrength();
            this.mIncreasingStrength = increasingStrength;
            this.mStrength = increasingStrength;
            if (increasingStrength == VivoVibratorServiceImpl.this.mEndStrength) {
                VivoVibratorServiceImpl.this.mH.removeCallbacks(this.mVibratorIncreasingRunnable);
            }
            VivoVibratorServiceImpl.this.setIncreasingAmplitudeVibPro(this.mStrength);
        }

        private int getIncreasingStrength() {
            int i = this.mIncreasingCount;
            this.mIncreasingCount = i + 1;
            double val = Math.pow(i / 10.0f, 2.0d);
            int increasingStrength = (int) Math.round((VivoVibratorServiceImpl.this.mEndStrength - VivoVibratorServiceImpl.this.mStartStrength) * val);
            return Math.min(VivoVibratorServiceImpl.this.mStartStrength + increasingStrength, VivoVibratorServiceImpl.this.mEndStrength);
        }
    }

    private void registerVibratorIntensity() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("haptic_feedback_intensity"), true, this.mSettingObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("notification_vibration_intensity"), true, this.mSettingObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("ring_vibration_intensity"), true, this.mSettingObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_ASCENDING_VIBRATION), true, this.mSettingObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(PRESS_KEY_RIGHT_INTENSITY), true, this.mSettingObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(PRESS_KEY_LEFT_INTENSITY), true, this.mSettingObserver, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVibratorIntensity(boolean isBoot) {
        this.mHapticFeedbackIntensity = Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_intensity", 2, -2);
        if ("HW_PD2025".equals(this.mProjectName) && this.SupportTypeLinearMotor != 0) {
            this.mNotificationIntensity = Settings.System.getIntForUser(this.mContext.getContentResolver(), "notification_vibration_intensity", 3, -2);
        } else {
            this.mNotificationIntensity = Settings.System.getIntForUser(this.mContext.getContentResolver(), "notification_vibration_intensity", 2, -2);
        }
        this.mRingIntensity = Settings.System.getIntForUser(this.mContext.getContentResolver(), "ring_vibration_intensity", 3, -2);
        this.mIncreasingVibrationEnable = Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_ASCENDING_VIBRATION, 0, -2);
        if (isBoot) {
            boolean leftRet = Settings.System.putIntForUser(this.mContext.getContentResolver(), PRESS_KEY_LEFT_INTENSITY, -1, -2);
            boolean rightRet = Settings.System.putIntForUser(this.mContext.getContentResolver(), PRESS_KEY_RIGHT_INTENSITY, -1, -2);
            StringBuilder sb = new StringBuilder();
            sb.append("leftRet ---> ");
            sb.append(leftRet ? "true" : "false");
            sb.append("  rightRet ---> ");
            sb.append(rightRet ? "true" : "false");
            VSlog.e(TAG, sb.toString());
            return;
        }
        this.mLeftPressKeyIntensity = Settings.System.getIntForUser(this.mContext.getContentResolver(), PRESS_KEY_LEFT_INTENSITY, 0, -2);
        int intForUser = Settings.System.getIntForUser(this.mContext.getContentResolver(), PRESS_KEY_RIGHT_INTENSITY, 0, -2);
        this.mRightPressKeyIntensity = intForUser;
        setPressKeyIntensityToNative(this.mLeftPressKeyIntensity, intForUser);
    }

    private void setPressKeyIntensityToNative(int mLeftPressKeyIntensity, int mRightPressKeyIntensity) {
        if (this.SupportTypeLinearMotor != 0) {
            VSlog.e(TAG, "mLeftPressKeyIntensity:" + mLeftPressKeyIntensity + "  mLastLeftPressKeyIntensity:" + this.mLastLeftPressKeyIntensity + "  mRightPressKeyIntensity:" + mRightPressKeyIntensity + "  mLastRightPressKeyIntensity:" + this.mLastRightPressKeyIntensity);
            mLeftPressKeyIntensity = (mLeftPressKeyIntensity > 3 || mLeftPressKeyIntensity < 0) ? 0 : 0;
            mRightPressKeyIntensity = (mRightPressKeyIntensity > 3 || mRightPressKeyIntensity < 0) ? 0 : 0;
            if (mLeftPressKeyIntensity != this.mLastLeftPressKeyIntensity) {
                vibratorProSetTriggerIntensity(mLeftPressKeyIntensity);
                this.mLastLeftPressKeyIntensity = mLeftPressKeyIntensity;
            }
            if (mRightPressKeyIntensity != this.mLastRightPressKeyIntensity) {
                vibratorExtSetTriggerIntensity(mRightPressKeyIntensity);
                this.mLastRightPressKeyIntensity = mRightPressKeyIntensity;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean SelfChange) {
            VivoVibratorServiceImpl.this.updateVibratorIntensity(false);
        }
    }

    public int toSetStrengthValue(int mode) {
        if (mode != 1) {
            if (mode != 2) {
                if (mode == 3) {
                    return 255;
                }
                return MID_VIBRATOR_INTENSITY;
            }
            return MID_VIBRATOR_INTENSITY;
        }
        return 1;
    }

    public int accordingToCustomJudge(int effectID, int originalStrength) {
        if (-1 == originalStrength) {
            if (effectID > 300 && effectID <= 400) {
                int strength = toSetStrengthValue(this.mRingIntensity);
                return strength;
            } else if (effectID > 400 && effectID <= 500) {
                int strength2 = toSetStrengthValue(this.mNotificationIntensity);
                return strength2;
            } else {
                int strength3 = toSetStrengthValue(this.mHapticFeedbackIntensity);
                return strength3;
            }
        }
        return 255;
    }

    private boolean isWaveform(VibrationEffect effect) {
        return effect instanceof VibrationEffect.Waveform;
    }

    private boolean isOneShot(VibrationEffect effect) {
        return effect instanceof VibrationEffect.OneShot;
    }

    private boolean isPrebaked(VibrationEffect effect) {
        return effect instanceof VibrationEffect.Prebaked;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isRingtone(int effectID) {
        return effectID > 300 && effectID <= 400;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAlarm(int effectID) {
        return effectID > 500 && effectID <= 600;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isNotification(int effectID) {
        return effectID > 400 && effectID <= 500;
    }

    private boolean isOtherType(int effectID) {
        return effectID <= 300 || effectID > 600;
    }

    public boolean whetherToContinueThisVibrator(VibrationEffect effect) {
        if (this.mCurrVibProVibrating) {
            VSlog.e(TAG, "current has new interface vibrator,meffectid:" + this.mEffectId);
            if (isWaveform(effect) && !isRingtone(this.mEffectId)) {
                cancelVibPro(this.mCurVibProPid, null, 0);
                return true;
            } else if (isOneShot(effect) && !isRingtone(this.mEffectId) && !isAlarm(this.mEffectId)) {
                cancelVibPro(this.mCurVibProPid, null, 0);
                return true;
            } else if (isPrebaked(effect) && !isRingtone(this.mEffectId) && !isAlarm(this.mEffectId) && !isNotification(this.mEffectId)) {
                cancelVibPro(this.mCurVibProPid, null, 0);
                return true;
            } else {
                return false;
            }
        }
        cancelByInner(this.mCurVibProPid, null, 0);
        return true;
    }

    public boolean whetherToStopCurrentVibrator(VibrationEffect effect, int effectId, boolean currentHasSystemAcousticVibrator) {
        if (currentHasSystemAcousticVibrator) {
            VSlog.e(TAG, "current has old interface vibrator");
            if (isRingtone(effectId)) {
                return true;
            }
            if (isNotification(effectId) || isAlarm(effectId)) {
                if (!isWaveform(effect)) {
                    return true;
                }
            } else if (isPrebaked(effect)) {
                return true;
            }
            return false;
        } else if (this.mCurrVibProVibrating) {
            VSlog.e(TAG, "current has new interface vibrator,meffectid:" + this.mEffectId);
            if (isRingtone(effectId)) {
                return true;
            }
            if (isAlarm(effectId)) {
                if (!isRingtone(this.mEffectId)) {
                    return true;
                }
            } else if (isNotification(effectId)) {
                if (!isRingtone(this.mEffectId) && !isAlarm(this.mEffectId)) {
                    return true;
                }
            } else if (!isRingtone(this.mEffectId) && !isAlarm(this.mEffectId) && !isNotification(this.mEffectId)) {
                return true;
            }
            return false;
        } else {
            return true;
        }
    }

    private void cancelByInner(int pid, String opPkg, int deviceID) {
        VSlog.e(TAG, "cancelByInner called, uid: " + pid + ", mPkgName: " + opPkg + ", deviceID=" + deviceID);
        this.mAudioFeatureCallback.cancel();
        this.mH.removeCallbacks(this.mVibratorProEndRunnable);
        this.mH.removeCallbacks(this.mVibratorCycleRunnable);
        try {
            try {
                if (this.SupportTypeLinearMotor != 0) {
                    if (this.mVivoVibratorSwr.isSupportSwr(this.mEffectId, 1) || this.mVivoVibratorSwr.isSupportSwr(this.mEffectId, 2)) {
                        this.mVivoVibratorSwr.setSwrParameters("stop");
                    }
                    if (deviceID == 0) {
                        vibratorProCancel();
                        vibratorExtCancel();
                        VSlog.e(TAG, "cancelByInner dual=" + deviceID);
                    } else if (deviceID == 1) {
                        vibratorProCancel();
                        VSlog.e(TAG, "cancelByInner bottom=" + deviceID);
                    } else if (deviceID == 2) {
                        vibratorExtCancel();
                        VSlog.e(TAG, "cancelByInner top=" + deviceID);
                    } else {
                        vibratorProCancel();
                        vibratorExtCancel();
                        VSlog.e(TAG, "cancelByInner default=" + deviceID);
                    }
                }
            } catch (Exception e) {
                VSlog.e(TAG, "cancelByInner called error; " + e);
            }
        } finally {
            this.mCurrVibProVibrating = false;
            this.mEffectId = 0;
            this.mCurVibProPid = -1;
        }
    }

    public void cancelSystemAcousticVibrator(IBinder mCurrentVibration_token) {
        if (this.mService == null) {
            this.mService = IVibratorService.Stub.asInterface(ServiceManager.getService("vibrator"));
        }
        IVibratorService iVibratorService = this.mService;
        if (iVibratorService != null) {
            try {
                iVibratorService.cancelVibrate(mCurrentVibration_token);
            } catch (RemoteException e) {
                VSlog.e(TAG, "cancelVibrate called error; " + e);
            }
        }
    }

    private static int isNotificationOrRingtone(int usageHint) {
        if (usageHint != 17) {
            if (usageHint != 18) {
                if (usageHint != 33) {
                    if (usageHint == 49) {
                        return 2;
                    }
                    return 5;
                }
                return 1;
            }
            return 4;
        }
        return 3;
    }

    public void systemToSendMessage(String opPkg, long vibratorDuration, VibrationEffect.Prebaked prebaked, int usageHint) {
        int intensity;
        int strength;
        int strength2 = -1;
        if (prebaked != null) {
            int effectStrength = prebaked.getEffectStrength();
            if (effectStrength == 0) {
                strength2 = 1;
            } else if (effectStrength != 1) {
                if (effectStrength == 2) {
                    strength2 = 255;
                }
            } else {
                strength2 = MID_VIBRATOR_INTENSITY;
            }
            strength = strength2;
        } else {
            int flag = isNotificationOrRingtone(usageHint);
            if (flag == 1) {
                intensity = this.mRingIntensity;
            } else if (flag == 2) {
                intensity = this.mNotificationIntensity;
            } else if (flag == 3) {
                intensity = 3;
            } else if (flag == 4) {
                intensity = this.mHapticFeedbackIntensity;
            } else {
                intensity = 2;
            }
            if (intensity != 1) {
                if (intensity != 2) {
                    if (intensity != 3) {
                        strength = -1;
                    } else {
                        strength = 255;
                    }
                } else {
                    strength = MID_VIBRATOR_INTENSITY;
                }
            } else {
                strength = 1;
            }
        }
        prepareToSendMessage(opPkg, vibratorDuration, strength, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareToSendMessage(String opPkg, long vibratorDuration, int strength, int effectID) {
        if (vibratorDuration != 0) {
            Message msg = this.mCDHandler.obtainMessage(1);
            Bundle bundle = new Bundle();
            bundle.putString(MODULE, opPkg);
            bundle.putString(TIMES, String.valueOf(vibratorDuration));
            bundle.putString(INTENSITY, Integer.toString(strength));
            bundle.putString(PATTERN, Integer.toString(effectID));
            msg.setData(bundle);
            this.mCDHandler.sendMessageDelayed(msg, 0L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void VDC_vibrator_pro_F_X(Message message) {
        try {
            VivoCollectData VDC_vibrator_pro_P_X = (VivoCollectData) getVCD(this.mContext);
            if (VDC_vibrator_pro_P_X == null) {
                VSlog.e(TAG, "get VDC_vibrator_pro_P_X failed");
                return;
            }
            if (this.VIBRATOR_LIST.size() == 0) {
                this.mVCDStartTime = System.currentTimeMillis();
            }
            String module = message.getData().getString(MODULE);
            String times = message.getData().getString(TIMES);
            String intensity = message.getData().getString(INTENSITY);
            String pattern = message.getData().getString(PATTERN);
            HashMap<String, String> map = new HashMap<>();
            map.put(MODULE, module);
            map.put(TIMES, times);
            map.put(INTENSITY, intensity);
            map.put(PATTERN, pattern);
            this.VIBRATOR_LIST.add(map);
            if (this.VIBRATOR_LIST.size() == 10) {
                this.VIBRATOR_MAP.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                this.VIBRATOR_MAP.put(START_TIME, String.valueOf(this.mVCDStartTime));
                this.VIBRATOR_MAP.put(END_TIME, String.valueOf(System.currentTimeMillis()));
                this.VIBRATOR_MAP.put(SHOCK, this.VIBRATOR_LIST.toString());
                VSlog.e(TAG, "VCode collect vibrator Map=" + this.VIBRATOR_MAP);
                EventTransfer.getInstance().singleEvent(VCD_EID_VIBRATOR_NEW, VCD_EID_VIBRATOR_PRO_NEW, System.currentTimeMillis(), 0L, this.VIBRATOR_MAP);
            }
            VSlog.e(TAG, "collect vibrator map=" + map);
            if (VDC_vibrator_pro_P_X.getControlInfo(VCD_EID_VIBRATOR)) {
                VDC_vibrator_pro_P_X.writeData(VCD_EID_VIBRATOR, VCD_EID_VIBRATOR_PRO, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, map);
            }
            if (this.VIBRATOR_LIST.size() >= 10) {
                this.VIBRATOR_LIST.clear();
            }
        } catch (Exception e) {
            VSlog.e(TAG, "set VDC_vibrator_pro_P_X error;" + e);
            if (this.VIBRATOR_LIST.size() >= 10) {
                this.VIBRATOR_LIST.clear();
            }
        }
    }

    private Object getVCD(Context context) {
        if (this.mVCD == null) {
            this.mVCD = VivoCollectData.getInstance(context);
        }
        return this.mVCD;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class CDHandler extends Handler {
        public CDHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                VivoVibratorServiceImpl.this.VDC_vibrator_pro_F_X(msg);
            }
        }
    }

    public void registerProcessObserver() {
        try {
            VSlog.e(TAG, "registerProcessObserver");
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (Exception e) {
            VSlog.e(TAG, "error registerProcessObserver;" + e);
        }
    }

    public boolean isVibrateProForIME(String opPkg, VibrationEffect effect, boolean hasSysVibrate, VibrationEffect mCurrentVibration_effect, IBinder mCurrentVibration_token) {
        int duration;
        VSlog.e(TAG, "opPkg=" + opPkg + ", effect=" + effect + ", isSupportTypeLinearMotor=" + this.SupportTypeLinearMotor);
        if (this.SupportTypeLinearMotor == 1 && VIBRATOR_IME_LIST.contains(opPkg)) {
            int pid = Binder.getCallingPid();
            int duration2 = 0;
            if (effect instanceof VibrationEffect.OneShot) {
                VibrationEffect.OneShot newOneShot = (VibrationEffect.OneShot) effect;
                duration2 = (int) newOneShot.getDuration();
            } else if (effect instanceof VibrationEffect.Waveform) {
                VibrationEffect.Waveform newWaveform = (VibrationEffect.Waveform) effect;
                int duration3 = (int) newWaveform.getDuration();
                duration = duration3;
                int effectId = getEffectId(opPkg, duration);
                vibrateVibPro(pid, opPkg, effectId, -1L, 3, hasSysVibrate, mCurrentVibration_effect, mCurrentVibration_token, 1);
                return true;
            }
            duration = duration2;
            int effectId2 = getEffectId(opPkg, duration);
            vibrateVibPro(pid, opPkg, effectId2, -1L, 3, hasSysVibrate, mCurrentVibration_effect, mCurrentVibration_token, 1);
            return true;
        }
        return false;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int getEffectId(String opPkg, int timeoutMs) {
        char c;
        switch (opPkg.hashCode()) {
            case -1805061386:
                if (opPkg.equals(SOGOU)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -681243162:
                if (opPkg.equals(OCTOPUS)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 332706122:
                if (opPkg.equals(BAIDU)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 919110943:
                if (opPkg.equals(QQ_PINYIN)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1407696064:
                if (opPkg.equals(IFLYTEK)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        if (c == 0) {
            int effectId = getOctopusEffectId(timeoutMs);
            return effectId;
        } else if (c == 1) {
            int effectId2 = getBaiduEffectId(timeoutMs);
            return effectId2;
        } else if (c == 2) {
            int effectId3 = getQQpinyinEffectId(timeoutMs);
            return effectId3;
        } else if (c == 3 || c == 4) {
            int effectId4 = getCommonEffectId(timeoutMs);
            return effectId4;
        } else {
            return MID_VIBRATOR_INTENSITY;
        }
    }

    private int getOctopusEffectId(int timeoutMs) {
        if (timeoutMs != 12) {
            if (timeoutMs != 20) {
                if (timeoutMs != 28) {
                    if (timeoutMs != 33) {
                        if (timeoutMs != 38) {
                            if (timeoutMs != 44) {
                                if (timeoutMs == 50) {
                                    return 149;
                                }
                                return MID_VIBRATOR_INTENSITY;
                            }
                            return 147;
                        }
                        return 146;
                    }
                    return MID_VIBRATOR_INTENSITY;
                }
                return 143;
            }
            return 142;
        }
        return 141;
    }

    private int getBaiduEffectId(int timeoutMs) {
        switch (timeoutMs / 10) {
            case 1:
                return 141;
            case 2:
                return 142;
            case 3:
                return 143;
            case 4:
                return 144;
            case 5:
                return MID_VIBRATOR_INTENSITY;
            case 6:
                return 147;
            case 7:
                return 148;
            case 8:
                return 149;
            case 9:
                return 150;
            default:
                return MID_VIBRATOR_INTENSITY;
        }
    }

    private int getQQpinyinEffectId(int timeoutMs) {
        switch ((timeoutMs + 5) / 10) {
            case 1:
                return 141;
            case 2:
                return 142;
            case 3:
                return 143;
            case 4:
                return MID_VIBRATOR_INTENSITY;
            case 5:
                return 146;
            case 6:
                return 147;
            case 7:
                return 149;
            case 8:
                return 150;
            default:
                return MID_VIBRATOR_INTENSITY;
        }
    }

    private int getCommonEffectId(int timeoutMs) {
        switch ((timeoutMs + 3) / 5) {
            case 1:
                return 141;
            case 2:
                return 142;
            case 3:
                return 143;
            case 4:
                return 144;
            case 5:
                return MID_VIBRATOR_INTENSITY;
            case 6:
                return 146;
            case 7:
                return 147;
            case 8:
                return 148;
            case 9:
                return 149;
            case 10:
                return 150;
            default:
                return MID_VIBRATOR_INTENSITY;
        }
    }

    public void setIncreasingAmplitudeRange(int intensity) {
        this.mWaveformIncreasingCount = 0;
        if (intensity == 1) {
            this.mStartStrength = 51;
            this.mEndStrength = 128;
        } else if (intensity == 2) {
            this.mStartStrength = MID_START_STRENTH;
            this.mEndStrength = 200;
        } else {
            this.mStartStrength = MID_START_STRENTH;
            this.mEndStrength = 255;
        }
        this.mWaveformIncreasingStrength = this.mStartStrength;
    }

    public void doIncreaseWaveformVibration() {
        this.mH.post(this.mWaveformVibratorIncreasingRunnable);
    }

    public void doCancelIncreasingWaveformVibration() {
        this.mH.removeCallbacks(this.mWaveformVibratorIncreasingRunnable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setWaveformVibratorIncreasing() {
        if (this.mWaveformIncreasingStrength == this.mEndStrength) {
            VSlog.e(TAG, "setVibratorIncreasing arrive EndStrength");
            return;
        }
        int waveformIncreasingStrength = getWaveformIncreasingStrength();
        this.mWaveformIncreasingStrength = waveformIncreasingStrength;
        if (waveformIncreasingStrength == this.mEndStrength) {
            this.mH.removeCallbacks(this.mWaveformVibratorIncreasingRunnable);
        }
    }

    private int getWaveformIncreasingStrength() {
        int i = this.mWaveformIncreasingCount;
        this.mWaveformIncreasingCount = i + 1;
        double val = Math.pow(i / 10.0f, 2.0d);
        int increasingStrength = (int) Math.round((this.mEndStrength - this.mStartStrength) * val);
        return Math.min(this.mStartStrength + increasingStrength, this.mEndStrength);
    }

    public int getIncreasingAmplitude() {
        return this.mWaveformIncreasingStrength;
    }

    /* loaded from: classes.dex */
    private static class CaseInsensitiveMap extends HashMap<String, Integer> {
        private CaseInsensitiveMap() {
        }

        @Override // java.util.HashMap, java.util.AbstractMap, java.util.Map
        public Integer put(String key, Integer value) {
            return (Integer) super.put((CaseInsensitiveMap) key.toLowerCase(), (String) value);
        }

        public Integer get(String key) {
            return (Integer) super.get((Object) key.toLowerCase());
        }
    }

    public boolean isVibrateDisableWhenInCamera(String vibPkg) {
        boolean isSkipVibrate = SystemProperties.getBoolean("camera.vibrate.skip", false);
        boolean isPrivilegeApp = "com.android.BBKClock".equals(vibPkg) || "com.android.server.telecom".equals(vibPkg);
        if (!isSkipVibrate || isPrivilegeApp) {
            return false;
        }
        VSlog.d(TAG, vibPkg + "vibrate is disallowed");
        return true;
    }

    private void initAppShareController() {
        String appShareInputHandle = Settings.System.getString(this.mContext.getContentResolver(), "appshare_input_handle");
        if (TextUtils.isEmpty(appShareInputHandle)) {
            appShareInputHandle = "local";
        }
        this.mIsControlledByRemote = "remote".equals(appShareInputHandle);
    }

    public boolean shouldBlockVibratorByAppShare(String packageName, int pid, int uid) {
        if (AppShareConfig.SUPPROT_APPSHARE && !TextUtils.isEmpty(this.mAppSharePackageName) && this.mAppShareUserId != -1 && this.mIsControlledByRemote) {
            boolean isIme = this.mVivoRatioControllerUtils.isImeApplication(this.mContext, pid, uid);
            VSlog.i(TAG, "shouldBlockVibratorByAppShare, isIme = " + isIme);
            if (isIme) {
                if (MultiDisplayManager.isAppShareDisplayId(this.mVivoRatioControllerUtils.getCurrentInputMethodDisplayId())) {
                    VSlog.i(TAG, "Rejected " + packageName + " to enqueue vibrate for APP_SHARE ime");
                    return true;
                }
            } else if (this.mAppSharePackageName.equals(packageName) && this.mAppShareUserId == UserHandle.getUserId(uid)) {
                VSlog.i(TAG, "Rejected " + packageName + " to enqueue vibrate for APP_SHARE app");
                return true;
            }
        }
        return false;
    }

    public void updateAppShareInputHandle(boolean isControlledByRemote) {
        this.mIsControlledByRemote = isControlledByRemote;
    }

    public void notifyAppSharePackageChanged(String packageName, int userId) {
        this.mAppSharePackageName = packageName;
        this.mAppShareUserId = userId;
    }
}