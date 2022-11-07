package com.android.server.power;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SystemSensorManager;
import android.hardware.biometrics.fingerprint.FingerprintKeyguardInternal;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.VivoDisplayStateInternal;
import android.multidisplay.MultiDisplayManagerInternal;
import android.os.Binder;
import android.os.Build;
import android.os.FtBuild;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.FtFeature;
import android.view.WindowManagerGlobal;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.lights.LogicalLight;
import com.android.server.policy.VivoPolicyConstant;
import com.android.server.power.PowerManagerService;
import com.android.server.wm.VivoAppShareManager;
import com.android.server.wm.WindowManagerInternal;
import com.vivo.appshare.AppShareConfig;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.sensor.autobrightness.GameModeLockAutobrightness;
import com.vivo.sensor.implement.SensorConfig;
import com.vivo.sensor.implement.VivoSensorCallback;
import com.vivo.sensor.implement.VivoSensorImpl;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import com.vivo.services.security.server.VivoPermissionUtils;
import com.vivo.services.superresolution.Constant;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoPowerManagerServiceImpl implements IVivoPowerManagerService {
    private static final int BUTTON_LIGHT_ON_DELAY = 100;
    private static final int CALL_STATE_DIALING = 2;
    private static final int CALL_STATE_IDLE = 0;
    private static final int CALL_STATE_OFFHOOK = 3;
    private static final int CALL_STATE_RINGING = 1;
    private static boolean DEBUG = false;
    private static boolean DEBUG_POWER = false;
    private static final int DEFAULT_SCREEN_OFF_TIMEOUT = 30000;
    private static final int DUMP_TIMEOUT = 30000;
    private static final float INITIALBRIGHTNESS = -1.0f;
    private static final int INVALID_PID = -99;
    private static final boolean IS_ENG = Build.TYPE.equals("eng");
    private static final boolean IS_LOG_CTRL_OPEN;
    private static final boolean IS_POWER_LOG_CTRL_OPEN;
    private static final String KEY_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    private static final String KEY_VIVO_POWER_LOG_CTRL = "persist.sys.power.log.ctrl";
    private static final float MAXIMUM_SCREEN_DIM_RATIO = 0.5f;
    private static final int MSG_BUTTON_LIGHT_ON = 11;
    private static final int MSG_DUMP_TIMEOUT = 12;
    private static final int MSG_FOREGROUND_ACTIVITIES_CHANGED = 1;
    private static final String TAG = "VivoPowerManagerService";
    private static final int USER_ACTIVITY_BUTTON_BRIGHT = 8;
    private static final String WAKE_UP_FOR_AIKEY = "AiKey";
    private static final int WAKE_UP_FOR_AIKEY_REASON = 17;
    private static final int WAKE_UP_FOR_DEFAULT_REASON = 10;
    private static final int WAKE_UP_FOR_DOUBLETAP_REASON = 11;
    private static final String WAKE_UP_FOR_DOUBLE_TAP = "double-tap";
    private static final String WAKE_UP_FOR_FACE = "FaceKey";
    private static final int WAKE_UP_FOR_FACE_REASON = 19;
    private static final String WAKE_UP_FOR_FLINGERPRINT = "android.policy:FINGERPRINT";
    private static final int WAKE_UP_FOR_FLINGERPRINT_REASON = 12;
    private static final String WAKE_UP_FOR_POCKETMODE = "PocketMode";
    private static final int WAKE_UP_FOR_POCKETMODE_REASON = 13;
    private static final String WAKE_UP_FOR_RAISEUP = "RaiseUpWakeService";
    private static final int WAKE_UP_FOR_RAISEUP_REASON = 14;
    private static final String WAKE_UP_FOR_SMARTKEY = "SmartKey";
    private static final int WAKE_UP_FOR_SMARTKEY_REASON = 16;
    private static final String WAKE_UP_FOR_SMARTWAKE = "SmartWake";
    private static final int WAKE_UP_FOR_SMARTWAKE_REASON = 18;
    private static final String WAKE_UP_FOR_WAKEUPKEY = "android.policy:POWER";
    private static final int WAKE_UP_FOR_WAKEUPKEYT_REASON = 15;
    private static final boolean isWakeLockControlOn;
    private static WindowManagerInternal mWindowManagerInternal;
    private DisplayManagerInternal displayManagerInternal;
    private boolean isButtonLightSpecialScene;
    private boolean isButtonLightSpecialSceneException;
    private IActivityManager mActivityManager;
    private LogicalLight mBacklight;
    private LogicalLight mButtonLight;
    private boolean mButtonLightAlwaysOn;
    private boolean mButtonLightSensorControl;
    private boolean mButtonLightSensorLastActive;
    private Handler mCallEndWaitHandler;
    private HandlerThread mCallEndWaitThread;
    private boolean mColorFadeAnimationOn;
    private Context mContext;
    private DisplayManagerInternal.DisplayPowerRequest mDisplayPowerRequest;
    private DumpTimeoutHandler mDumpTimeoutHandler;
    private boolean mEasyShareOn;
    private FingerprintKeyguardInternal mFingerprintUnlockController;
    private GameModeLockAutobrightness mGameModeLockAutobrightness;
    private PowerManagerHandler mHandler;
    private long mLastUserActivityButtonTime;
    private Sensor mLightSensorForButtonLightControl;
    private MultiDisplayManagerInternal mMultiDisplayManagerInternal;
    private PowerDataReport mPowerDataReport;
    private PowerManagerService mPowerManagerService;
    private SensorManager mSensorManager;
    private VivoAppShareManager mVivoAppShareManager;
    private VivoDisplayStateInternal mVivoDisplayStateInternal;
    private VivoSensorImpl mVivoSensorImpl;
    private Handler mWakeLockControlHandler;
    private HandlerThread mWakeLockControlThread;
    private boolean mButtonLightOn = false;
    private int buttonLightControlMode = 0;
    private boolean isHolsterHasWindow = "Have_holster_with_window".equalsIgnoreCase(SystemProperties.get("persist.vivo.phone.holster", (String) null));
    private boolean isPhoneStateIdle = true;
    private boolean isPhoneStateRing = false;
    private boolean isPhoneStateDialing = false;
    private boolean isKeyguarActive = false;
    private final int MAX_SUPPORTED_BUTTON_LIGHT_VALUE = 255;
    private final int MSG_CLEAR_BUTTON_REFECLT = 99;
    private boolean isButtonNeedReflectGlobal = false;
    private ArrayList<Integer> mTopPids = new ArrayList<>();
    private String[] mWhitePkgList = {Constant.APP_WEIXIN, "com.tencent.mobileqq", "com.iqoo.engineermode"};
    private ArrayList<Integer> mWhiteUidList = new ArrayList<>();
    int mEngineerModeUid = -1;
    int myPid = Process.myPid();
    private int mTemporaryScreenBrightnessSettingOverride = -1;
    private boolean mBlockWakeupWakelock = false;
    private boolean mHasUnansweredCall = false;
    private boolean mHasUnansweredCallBlockScreenOn = false;
    private boolean mProximityStatus = false;
    private boolean mProximityEnabled = false;
    private boolean mScreenBrightnessOverrideFromWindowManagerStatus = false;
    private String mSettingBrightnessMultilevelChangeBy = "unknown";
    private int mSettingScreenBrightnessMultiLevel = -1;
    private LockPatternUtils mLockPatternUtils = null;
    private PowerManager.WakeLock mCallEndWaitWakeLock = null;
    private float mScreenBrightnessOverrideFromPem = INITIALBRIGHTNESS;
    private int mDefaultScreenBrightnessSetting = -1;
    DisplayManager mDisplayManager = null;
    private boolean mPrimaryDisplayDozeRequired = false;
    private boolean mSecondaryDisplayDozeRequired = false;
    protected final ArrayList<PowerManagerService.WakeLock> mFrozenWakeLocks = new ArrayList<>();
    private int mPhoneCallState = 0;
    private int mWakeUpReason = 1;
    private int mZenMode = 0;
    private boolean mNotInterruptDuringDrive = false;
    private float mScreenBrightnessSetting = INITIALBRIGHTNESS;
    private String mSettingBrightnessChangeBy = "unknown";
    private String mBrightnessModeOffBy = "unknown";
    private boolean tempAutoBrightness = false;
    private boolean mLightUpNow = false;
    private int mCurrentUser = -2;
    private boolean isBoot = true;
    private String mAppSharePackageName = null;
    private int mAppShareUserId = -1;
    private boolean mAppShareDisplayHasScreenOnWakeLock = false;
    private boolean mLastAppShareDisplayHasScreenOnWakeLock = false;
    public VivoSensorCallback.SensorCallbackPMS mVivoSensorCallback = new VivoSensorCallback.SensorCallbackPMS() { // from class: com.android.server.power.VivoPowerManagerServiceImpl.1
        public void wakeUpSystem(String details) {
            VLog.i(VivoPowerManagerServiceImpl.TAG, "Anti logout, wakeUp details = " + details);
            VivoPowerManagerServiceImpl.this.mPowerManagerService.wakeUpInternal(SystemClock.uptimeMillis(), 2, details, 1000, VivoPowerManagerServiceImpl.this.mContext.getOpPackageName(), 1000);
        }

        public int countBrightFullLocks() {
            int size = VivoPowerManagerServiceImpl.this.mPowerManagerService.mWakeLocks.size();
            int counts = 0;
            String strPartials = "brightFullLocks:={";
            for (int i = 0; i < size; i++) {
                PowerManagerService.WakeLock wakeLock = (PowerManagerService.WakeLock) VivoPowerManagerServiceImpl.this.mPowerManagerService.mWakeLocks.get(i);
                int i2 = wakeLock.mFlags & 65535;
                if (i2 != 6) {
                    if (i2 == 10) {
                        counts++;
                        strPartials = strPartials + " mTag=" + wakeLock.mTag;
                    } else if (i2 != 26) {
                    }
                }
                counts++;
                strPartials = strPartials + " mTag=" + wakeLock.mTag;
            }
            if (VivoPowerManagerServiceImpl.DEBUG) {
                if (counts == 0) {
                    VSlog.d(VivoPowerManagerServiceImpl.TAG, "ANTI-MIS: countBrightFullLocks counts=0");
                } else {
                    VSlog.d(VivoPowerManagerServiceImpl.TAG, "ANTI-MIS: countBrightFullLocks " + strPartials + "}");
                }
            }
            return counts;
        }
    };
    private Runnable mCallEndWaitRunnable = new Runnable() { // from class: com.android.server.power.VivoPowerManagerServiceImpl.2
        @Override // java.lang.Runnable
        public void run() {
            if (!VivoPowerManagerServiceImpl.this.mCallEndWaitWakeLock.isHeld()) {
                VSlog.d(VivoPowerManagerServiceImpl.TAG, "0ops mCallEndWaitWakeLock acquire");
                VivoPowerManagerServiceImpl.this.mCallEndWaitWakeLock.acquire(3000L);
            }
        }
    };
    private Runnable mSendBroadcastForApp = new Runnable() { // from class: com.android.server.power.VivoPowerManagerServiceImpl.3
        @Override // java.lang.Runnable
        public void run() {
            VSlog.d(VivoPowerManagerServiceImpl.TAG, "handleSmartMirrorFirstoff sendbroadcast");
            Intent intent = new Intent();
            intent.setPackage("com.vivo.upnpserver");
            intent.setAction("vivo.intent.action.firstoffscreen");
            VivoPowerManagerServiceImpl.this.mContext.sendBroadcast(intent);
            Settings.Secure.putInt(VivoPowerManagerServiceImpl.this.mContext.getContentResolver(), "vscreen_first_off", 1);
        }
    };
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.android.server.power.VivoPowerManagerServiceImpl.4
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (VivoPowerManagerServiceImpl.DEBUG) {
                VSlog.d(VivoPowerManagerServiceImpl.TAG, "onForegroundActivityChanged PID = " + pid + " ,uid = " + uid + " ,foregroundActivities = " + foregroundActivities);
            }
            VivoPowerManagerServiceImpl.this.mWakeLockControlHandler.obtainMessage(1, pid, uid, Boolean.valueOf(foregroundActivities)).sendToTarget();
        }

        public void onProcessDied(int pid, int uid) {
            VivoPowerManagerServiceImpl.this.mWakeLockControlHandler.obtainMessage(1, pid, uid, false).sendToTarget();
        }

        public void onProcessStateChanged(int pid, int uid, int procState) throws RemoteException {
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    };
    SensorEventListener mLightListenerForButtonLightControl = new SensorEventListener() { // from class: com.android.server.power.VivoPowerManagerServiceImpl.5
        int totalValueInThreeSeconds = 0;
        int countNumberInThreeSeconds = 1;
        long now = SystemClock.uptimeMillis();

        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            int avgValue;
            boolean active;
            synchronized (VivoPowerManagerServiceImpl.this.mPowerManagerService.mLock) {
                int value = (int) event.values[0];
                int value2 = (int) ((value * 500.0f) / Float.parseFloat(SystemProperties.get("persist.sys.light_threshold", "500")));
                if (SystemClock.uptimeMillis() - this.now <= 3000) {
                    this.totalValueInThreeSeconds += value2;
                    this.countNumberInThreeSeconds++;
                } else {
                    if (this.countNumberInThreeSeconds > 1) {
                        avgValue = this.totalValueInThreeSeconds / (this.countNumberInThreeSeconds - 1);
                    } else {
                        avgValue = this.totalValueInThreeSeconds / this.countNumberInThreeSeconds;
                    }
                    this.totalValueInThreeSeconds = 0;
                    this.countNumberInThreeSeconds = 1;
                    this.now = SystemClock.uptimeMillis();
                    if (VivoPowerManagerServiceImpl.this.mButtonLightSensorLastActive) {
                        active = avgValue < 15;
                    } else {
                        active = avgValue <= 4;
                    }
                    if (VivoPowerManagerServiceImpl.this.mButtonLightSensorLastActive != active) {
                        VivoPowerManagerServiceImpl.this.mButtonLightSensorLastActive = active;
                        VivoPowerManagerServiceImpl.this.mButtonLightOn = active;
                    }
                }
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private boolean mBatteryLow = false;
    private Runnable mRegisterRunnable = new Runnable() { // from class: com.android.server.power.VivoPowerManagerServiceImpl.7
        @Override // java.lang.Runnable
        public void run() {
            VivoPowerManagerServiceImpl vivoPowerManagerServiceImpl = VivoPowerManagerServiceImpl.this;
            vivoPowerManagerServiceImpl.registerLogBroadcast(vivoPowerManagerServiceImpl.mContext);
        }
    };

    static {
        boolean equals = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
        IS_LOG_CTRL_OPEN = equals;
        DEBUG = equals || IS_ENG;
        boolean equals2 = SystemProperties.get(KEY_VIVO_POWER_LOG_CTRL, "no").equals("yes");
        IS_POWER_LOG_CTRL_OPEN = equals2;
        DEBUG_POWER = equals2;
        isWakeLockControlOn = FtFeature.isFeatureSupport("vivo.software.wakelockcontrol");
    }

    public VivoPowerManagerServiceImpl(Context context, PowerManagerService powerManagerService, DisplayManagerInternal.DisplayPowerRequest displaypowerrequest) {
        this.mGameModeLockAutobrightness = null;
        this.mVivoAppShareManager = null;
        this.mContext = context;
        this.mPowerManagerService = powerManagerService;
        this.mDisplayPowerRequest = displaypowerrequest;
        HandlerThread handlerThread = new HandlerThread("WakelockControl");
        this.mWakeLockControlThread = handlerThread;
        handlerThread.start();
        this.mWakeLockControlHandler = new WakeLockControlHandler(this.mWakeLockControlThread.getLooper());
        this.mGameModeLockAutobrightness = new GameModeLockAutobrightness(context);
        HandlerThread handlerThread2 = new HandlerThread("CallEndWaitThread");
        this.mCallEndWaitThread = handlerThread2;
        handlerThread2.start();
        this.mCallEndWaitHandler = new Handler(this.mCallEndWaitThread.getLooper());
        this.mDumpTimeoutHandler = new DumpTimeoutHandler(IoThread.getHandler().getLooper());
        this.mVivoAppShareManager = VivoAppShareManager.getInstance();
        SystemProperties.set("sys.castdisplay.plus.support", "yes");
    }

    public void isDebugAllow() {
        PowerManagerService.DEBUG = DEBUG;
        PowerManagerService.DEBUG_SPEW = DEBUG;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class PowerManagerHandler extends Handler {
        public PowerManagerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 11) {
                if (i == 99) {
                    VivoPowerManagerServiceImpl.this.isButtonNeedReflectGlobal = false;
                }
            } else if (VivoPowerManagerServiceImpl.this.mButtonLight != null) {
                VivoPowerManagerServiceImpl.this.mButtonLight.setBrightness(255.0f);
                VSlog.d(VivoPowerManagerServiceImpl.TAG, "handleMessage MSG_BUTTON_LIGHT_ON");
            } else {
                VSlog.d(VivoPowerManagerServiceImpl.TAG, "mButtonLight == null");
            }
        }
    }

    /* loaded from: classes.dex */
    private final class ShutDownReceiver extends BroadcastReceiver {
        private ShutDownReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            synchronized (VivoPowerManagerServiceImpl.this.mPowerManagerService.mLock) {
                VSlog.d(VivoPowerManagerServiceImpl.TAG, "receive shutdown broadcast,turn off button light.");
                VivoPowerManagerServiceImpl.this.mButtonLightOn = false;
                VivoPowerManagerServiceImpl.this.mButtonLightAlwaysOn = false;
                VivoPowerManagerServiceImpl.this.mHandler.removeMessages(11);
                if (VivoPowerManagerServiceImpl.this.mButtonLight != null) {
                    VivoPowerManagerServiceImpl.this.mButtonLight.turnOff();
                } else {
                    VSlog.d(VivoPowerManagerServiceImpl.TAG, "mButtonLight == null");
                }
            }
        }
    }

    private void sensorOperationReady(PowerManager pm) {
        this.mCallEndWaitWakeLock = pm.newWakeLock(1, TAG);
        this.mDefaultScreenBrightnessSetting = pm.getDefaultScreenBrightnessSetting();
    }

    private void blockWakeUp(IBinder lock, int flags, String tag, String packageName, WorkSource ws, int uid, int pid, boolean mSystemReady) {
        VivoSensorImpl vivoSensorImpl;
        if (mSystemReady && (vivoSensorImpl = this.mVivoSensorImpl) != null) {
            this.mBlockWakeupWakelock = vivoSensorImpl.onAcquireWakeLock(flags, tag, packageName, ws, uid, pid, Objects.hashCode(lock));
            if (blockRingingWake(flags)) {
                this.mBlockWakeupWakelock = true;
            }
        }
        if ("ProximitySensorManager".equals(tag) && (flags & 32) != 0) {
            VSlog.d(TAG, "acquireWakeLockInternal tag = " + tag);
            DisplayManagerInternal.DisplayPowerRequest displayPowerRequest = this.mDisplayPowerRequest;
            if (displayPowerRequest != null) {
                displayPowerRequest.bIsProximitySensorTag = true;
            }
            VivoSensorImpl vivoSensorImpl2 = this.mVivoSensorImpl;
            if (vivoSensorImpl2 != null) {
                vivoSensorImpl2.onProximityLockChanged(true);
            }
        }
    }

    private boolean blockRingingWake(int flags) {
        if (this.mPhoneCallState == 0 || !this.mProximityStatus || (268435456 & flags) == 0) {
            return false;
        }
        VSlog.d(TAG, "mBlockWakeupWakelock = " + this.mBlockWakeupWakelock + ",mProximityStatus=" + this.mProximityStatus);
        return true;
    }

    public void acquireWakeLockInOtherThread() {
        if (this.mCallEndWaitWakeLock != null) {
            VSlog.d(TAG, "0ops mCallEndWaitWakeLock will be acquired in a new thread");
            this.mCallEndWaitHandler.removeCallbacks(this.mCallEndWaitRunnable);
            this.mCallEndWaitHandler.post(this.mCallEndWaitRunnable);
        }
    }

    public void isProximitySensorTag(int flags, String mTag, boolean mProximityPositive) {
        if ((flags & 32) != 0 && "ProximitySensorManager".equals(mTag)) {
            VSlog.d(TAG, "releaseWakeLockInternal mHasUnansweredCall=" + this.mHasUnansweredCall + " mProximityPositive=" + mProximityPositive);
            DisplayManagerInternal.DisplayPowerRequest displayPowerRequest = this.mDisplayPowerRequest;
            if (displayPowerRequest != null) {
                displayPowerRequest.bIsProximitySensorTag = false;
            }
            VivoSensorImpl vivoSensorImpl = this.mVivoSensorImpl;
            if (vivoSensorImpl != null) {
                vivoSensorImpl.onProximityLockChanged(false);
            }
        }
    }

    public void notifyAntimisoperation(IBinder lock, boolean mSystemReady) {
        VivoSensorImpl vivoSensorImpl;
        if (this.mHasUnansweredCallBlockScreenOn) {
            this.mVivoSensorImpl.enableAntiMonitor(true);
        }
        if (mSystemReady && (vivoSensorImpl = this.mVivoSensorImpl) != null) {
            vivoSensorImpl.onReleaseWakelock(Objects.hashCode(lock));
        }
    }

    public boolean wakeUpByPowerKey(boolean mProximityPositive, int reason, long eventTime) {
        if (mProximityPositive && reason == 4) {
            VSlog.d(TAG, "Proximity positive sleep and force wakeup by power button");
            this.mPowerManagerService.setWakefulnessLocked(0, reason, eventTime);
            this.mPowerManagerService.mProximityPositive = false;
            this.mPowerManagerService.updatePowerStateLocked();
            return true;
        }
        VSlog.d(TAG, "goToSleepInternal mProximityPositive=" + mProximityPositive + " reason=" + reason);
        return false;
    }

    public void onGotoSleep(int reason) {
        VivoSensorImpl vivoSensorImpl = this.mVivoSensorImpl;
        if (vivoSensorImpl != null) {
            vivoSensorImpl.onGotoSleep(reason);
        }
    }

    public void enableAntiMonitor(boolean mSystemReady, int oldPolicy) {
        String antiInfo;
        this.mDisplayPowerRequest.callState = this.mPhoneCallState;
        VivoSensorImpl vivoSensorImpl = this.mVivoSensorImpl;
        if (vivoSensorImpl == null) {
            return;
        }
        vivoSensorImpl.onPolicyChanged(this.mDisplayPowerRequest.policy);
        if (mSystemReady && oldPolicy != 0 && this.mDisplayPowerRequest.policy == 0) {
            if (!this.mVivoSensorImpl.getAntiEnable()) {
                if (DEBUG) {
                    VSlog.d(TAG, "enableAntiMonitor(true)");
                }
                this.mVivoSensorImpl.enableAntiMonitor(true);
                antiInfo = "use enable_true";
            } else {
                antiInfo = "use enable_false";
            }
        } else if (mSystemReady && oldPolicy == 0 && this.mDisplayPowerRequest.policy != 0 && (oldPolicy != 0 || this.mDisplayPowerRequest.policy != 1)) {
            if (this.mVivoSensorImpl.getAntiEnable()) {
                if (DEBUG) {
                    VSlog.d(TAG, "enableAntiMonitor(false)");
                }
                this.mVivoSensorImpl.enableAntiMonitor(false);
                antiInfo = "use enable_false";
            } else {
                antiInfo = "use already_false";
            }
        } else {
            antiInfo = "enable = " + this.mVivoSensorImpl.getAntiEnable();
        }
        if (oldPolicy != this.mDisplayPowerRequest.policy) {
            VSlog.d(TAG, "oldPolicy=" + oldPolicy + " policy=" + this.mDisplayPowerRequest.policy + " ready=" + mSystemReady + " phoneState:" + this.mPhoneCallState + " useProx=" + this.mDisplayPowerRequest.useProximitySensor + " antiInfo:" + antiInfo);
        }
    }

    public boolean isWindowBrightness(int mScreenBrightnessOverrideFromWindowManager) {
        if (PowerManagerService.isValidBrightness(mScreenBrightnessOverrideFromWindowManager)) {
            return true;
        }
        return false;
    }

    public boolean notWakeUpWhenPlugged() {
        if (this.mPhoneCallState != 0 && this.mProximityStatus) {
            VSlog.d(TAG, "Do not react to USB plugging event as ProximityStatus is " + this.mProximityStatus + " when in calling");
            return true;
        }
        return false;
    }

    public void updateDisplayPowerRequest(int mScreenBrightnessSetting, int mScreenBrightnessModeSetting) {
        this.mDisplayPowerRequest.settingBrightness = mScreenBrightnessSetting;
        this.mDisplayPowerRequest.settingScreenBrightnessMode = mScreenBrightnessModeSetting;
        this.mDisplayPowerRequest.brightnessModeOffBy = this.mBrightnessModeOffBy;
        VivoSensorImpl vivoSensorImpl = this.mVivoSensorImpl;
        if (vivoSensorImpl != null) {
            this.mDisplayPowerRequest.antimisoperationTriggered = vivoSensorImpl.getAntiTrigger();
        }
    }

    public void onWakeupKeyPressd(String who) {
        VivoSensorImpl vivoSensorImpl = this.mVivoSensorImpl;
        if (vivoSensorImpl != null) {
            vivoSensorImpl.onWakeupKeyPressd(who);
        }
    }

    public boolean setBlockWakeup() {
        if (this.mBlockWakeupWakelock) {
            this.mBlockWakeupWakelock = false;
            return true;
        }
        return false;
    }

    public boolean applyWakeLockDoNoting(boolean mSystemReady) {
        VivoSensorImpl vivoSensorImpl;
        if (mSystemReady && (vivoSensorImpl = this.mVivoSensorImpl) != null && vivoSensorImpl.getAntiEnable() && this.mVivoSensorImpl.isAntiProxPositive()) {
            VSlog.i(TAG, "Anti-Mis:release wake lock, do nothing");
            return true;
        }
        return false;
    }

    public void setProximityStatus(boolean proximity) {
        VSlog.i(TAG, "proximity status:" + proximity);
        this.mProximityStatus = proximity;
    }

    public void setProximityEnabled(boolean enabled) {
        VSlog.i(TAG, "proximity enabled:" + enabled);
        this.mProximityEnabled = enabled;
    }

    public void onProximityPositive() {
        if (this.mProximityEnabled) {
            VSlog.i(TAG, "onProximityPositive");
            this.mPowerManagerService.mProximityPositive = true;
            this.mPowerManagerService.mDirty |= 512;
            if (DEBUG_POWER) {
                VSlog.i(TAG, "setBrightness mButtonLight 0 333.");
                VSlog.d(TAG, "set isButtonNeedReflectGlobal false.");
            }
            if ("vos".equals(FtBuild.getOsName())) {
                this.mPowerManagerService.updateProximityState(1);
            }
            if (this.mPhoneCallState != 0 && !"vos".equals(FtBuild.getOsName())) {
                if (this.mPowerManagerService.goToSleepNoUpdateLocked(SystemClock.uptimeMillis(), 100, 1, 1000)) {
                    this.mPowerManagerService.updatePowerStateLocked();
                    return;
                }
                return;
            }
            this.mPowerManagerService.updatePowerStateLocked();
            return;
        }
        VSlog.i(TAG, "onProximityPositive, mProximityEnabled = " + this.mProximityEnabled + ", mProximityStatus = " + this.mProximityStatus);
    }

    public void onProximityNegative() {
        VSlog.i(TAG, "onProximityNegative");
        if ("vos".equals(FtBuild.getOsName())) {
            this.mPowerManagerService.updateProximityState(0);
        }
        if (this.mHasUnansweredCallBlockScreenOn) {
            VSlog.d(TAG, "onProximityNegative mHasUnansweredCallBlockScreenOn: go block screen on.");
            this.mHasUnansweredCallBlockScreenOn = false;
            if (this.mPowerManagerService.goToSleepNoUpdateLocked(SystemClock.uptimeMillis(), 2, 1, 1000)) {
                this.mPowerManagerService.updatePowerStateLocked();
                return;
            }
            return;
        }
        this.mPowerManagerService.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 0, 1000);
        this.mPowerManagerService.wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), 0, "android.server.power:onProximityNegative", 1000, this.mContext.getOpPackageName(), 1000);
    }

    public boolean getPolicyWhenThirdMicro() {
        int i;
        if (!this.mPowerManagerService.mProximityPositive || (i = this.mPhoneCallState) == 0) {
            return false;
        }
        return i != 1 || (i == 1 && this.mDisplayPowerRequest.useProximitySensor);
    }

    public void systemReady_step1(PowerManager pm) {
        sensorOperationReady(pm);
        this.mHandler = new PowerManagerHandler(this.mPowerManagerService.mHandlerThread.getLooper());
        this.mSensorManager = new SystemSensorManager(this.mContext, this.mHandler.getLooper());
        PowerManagerService powerManagerService = this.mPowerManagerService;
        powerManagerService.mWirelessChargerDetector = powerManagerService.mInjector.createWirelessChargerDetector(this.mSensorManager, this.mPowerManagerService.mInjector.createSuspendBlocker(this.mPowerManagerService, "PowerManagerService.WirelessChargerDetector"), this.mHandler);
        this.mPowerManagerService.mDisplayManagerInternal.initPowerManagement(this.mPowerManagerService.mDisplayPowerCallbacks, this.mHandler, this.mSensorManager);
        PowerManagerService.DEFAULT_SCREEN_OFF_TIMEOUT = 30000;
        this.mButtonLight = this.mPowerManagerService.mLightsManager.getLight(2);
        this.mBacklight = this.mPowerManagerService.mLightsManager.getLight(0);
        getWhiteUidList();
        this.buttonLightControlMode = Settings.System.getInt(this.mContext.getContentResolver(), "key_button_light_control_mode", 3);
        VLog.d(TAG, "the mode acquired from database  mode = " + this.buttonLightControlMode);
        int i = this.buttonLightControlMode;
        if (i == 0) {
            this.mButtonLightOn = true;
            this.mButtonLightAlwaysOn = false;
            this.mButtonLightSensorControl = false;
        } else if (i == 1) {
            this.mButtonLightOn = true;
            this.mButtonLightAlwaysOn = false;
            this.mButtonLightSensorControl = false;
        } else if (i == 2) {
            this.mButtonLightOn = true;
            this.mButtonLightAlwaysOn = true;
            this.mButtonLightSensorControl = false;
        } else if (i == 3) {
            this.mButtonLightOn = false;
            this.mButtonLightAlwaysOn = false;
            this.mButtonLightSensorControl = false;
        } else if (i == 4) {
            this.mButtonLightOn = true;
            this.mButtonLightAlwaysOn = false;
            this.mButtonLightSensorControl = true;
            this.mSensorManager.registerListener(this.mLightListenerForButtonLightControl, this.mLightSensorForButtonLightControl, 3);
            this.mButtonLightSensorLastActive = false;
        } else {
            VLog.d(TAG, "the mode acquired from database is not supported yet mode = " + this.buttonLightControlMode);
        }
        this.mHandler.post(this.mRegisterRunnable);
        FingerprintKeyguardInternal fingerprintUnlockController = getFingerprintUnlockController();
        if (fingerprintUnlockController != null) {
            fingerprintUnlockController.setPowerManagerHandler(this.mHandler);
            fingerprintUnlockController.setFingerprintWakeUpCallback(new FingerprintWakeUpCallback());
            return;
        }
        VSlog.w(TAG, "fingerprintUnlockController is null!");
    }

    public void systemReady_step2() {
        this.mActivityManager = ActivityManager.getService();
        VSlog.d(TAG, "isWakeLockControlOn = " + isWakeLockControlOn);
        if (isWakeLockControlOn) {
            try {
                this.mActivityManager.registerProcessObserver(this.mProcessObserver);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Settings.Global.getUriFor("zen_mode"), false, this.mPowerManagerService.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.System.getUriFor("drive_mode_enabled"), false, this.mPowerManagerService.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.System.getUriFor("shield_notification_reminder_enabled"), false, this.mPowerManagerService.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.System.getUriFor("global_animation_color_fade_style"), false, this.mPowerManagerService.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.System.getUriFor("screen_brightness"), false, this.mPowerManagerService.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_float"), false, this.mPowerManagerService.mSettingsObserver, -1);
        VivoSensorImpl vivoSensorImpl = VivoSensorImpl.getInstance(this.mContext);
        this.mVivoSensorImpl = vivoSensorImpl;
        vivoSensorImpl.setPMSCallback(this.mDisplayPowerRequest, this.mVivoSensorCallback);
        resolver.registerContentObserver(Settings.System.getUriFor("easy_share_force_brightness_off"), false, this.mPowerManagerService.mSettingsObserver, -1);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_SHUTDOWN");
        filter.setPriority(1000);
        this.mContext.registerReceiver(new ShutDownReceiver(), filter, null, this.mHandler);
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        if (SensorConfig.isGameOptimizeBrightness()) {
            resolver.registerContentObserver(Settings.System.getUriFor("gamewatch_game_target_fps"), false, this.mPowerManagerService.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("is_game_mode"), false, this.mPowerManagerService.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("vivo_dc_dimming_enabled"), false, this.mPowerManagerService.mSettingsObserver, -1);
        }
    }

    public void updateSettingsLocked() {
        this.mZenMode = Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", 0);
        this.mNotInterruptDuringDrive = computeDriveMode();
        this.mColorFadeAnimationOn = getColorFadeAnimationOn();
        handleEasyShareChanged();
        if (SensorConfig.isGameOptimizeBrightness()) {
            getGameFrameRateMode();
        }
    }

    public void wakeUpNoUpdateLocked_step1(long eventTime, int reason, String details, int reasonUid, String opPackageName, int opUid) {
        if (DEBUG) {
            StackTraceElement[] stack = new Throwable().getStackTrace();
            for (StackTraceElement element : stack) {
                VSlog.d(TAG, "   |----" + element.toString());
            }
        }
        if (DEBUG) {
            VSlog.d(TAG, "wakeUpNoUpdateLocked: eventTime=" + eventTime + ", reason=" + details + ", uid=" + reasonUid);
        }
        if (this.mButtonLightSensorControl) {
            this.mSensorManager.registerListener(this.mLightListenerForButtonLightControl, this.mLightSensorForButtonLightControl, 3);
        }
    }

    public int wakeUpNoUpdateLocked_step2(int reason, String details, int reasonUid, String opPackageName, int opUid, long eventTime, long sleepTimeout) {
        VivoSensorImpl vivoSensorImpl;
        this.isButtonLightSpecialScene = true;
        if (fingerprintBlockNotifier()) {
            this.isButtonLightSpecialScene = false;
        } else if (!this.isButtonLightSpecialSceneException) {
            this.isButtonNeedReflectGlobal = true;
            this.mHandler.removeMessages(99);
            PowerManagerHandler powerManagerHandler = this.mHandler;
            powerManagerHandler.sendMessageDelayed(powerManagerHandler.obtainMessage(99), getButtonLightDurationLocked(sleepTimeout));
        }
        int wakeUpReason = getWakeUpReason(details);
        this.mWakeUpReason = wakeUpReason;
        if (this.mPhoneCallState == 0 && wakeUpReason == 15) {
            this.mDisplayPowerRequest.isPowerKeyWakeUp = true;
        } else {
            this.mDisplayPowerRequest.isPowerKeyWakeUp = false;
        }
        if (details.toLowerCase().contains("finger") && (vivoSensorImpl = this.mVivoSensorImpl) != null) {
            vivoSensorImpl.notifyFingerWakeUp();
        }
        return this.mWakeUpReason;
    }

    public void goToSleepNoUpdateLocked_step1() {
        if (DEBUG) {
            StackTraceElement[] stack = new Throwable().getStackTrace();
            for (StackTraceElement element : stack) {
                VSlog.d(TAG, "   |----" + element.toString());
            }
        }
        if (this.mButtonLightSensorControl) {
            this.mSensorManager.unregisterListener(this.mLightListenerForButtonLightControl);
        }
        this.isButtonNeedReflectGlobal = false;
        if (DEBUG_POWER) {
            VSlog.d(TAG, "set isButtonNeedReflectGlobal false.");
        }
    }

    public void goToSleepNoUpdateLocked_step2(String string) {
        VSlog.d(TAG, "Cleared screen wakelock :" + string);
    }

    public void goToSleepNoUpdateLocked_step3() {
        dumpWakeLockLocked();
    }

    public void acquireWakeLockInternal(IBinder lock, int flags, String tag, String packageName, boolean mSystemReady, WorkSource ws, String historyTag, int uid, int pid, int displayId) {
        if (!isFlagPartialLock(flags) || DEBUG_POWER) {
            VSlog.d(TAG, "acquireWakeLockInternal: lock=" + Objects.hashCode(lock) + ", flags=0x" + Integer.toHexString(flags) + ", tag=\"" + tag + "\", ws=" + ws + ", uid=" + uid + ", pid=" + pid);
        }
        notifyProxLockUid(flags, uid);
        blockWakeUp(lock, flags, tag, packageName, ws, uid, pid, mSystemReady);
    }

    public void releaseWakeLockInternal(IBinder lock, int flags, PowerManagerService.WakeLock wakeLock) {
        if (!isFlagPartialLock(wakeLock.mFlags) || DEBUG_POWER) {
            VSlog.d(TAG, "releaseWakeLockInternal: lock=" + Objects.hashCode(lock) + " [" + wakeLock.mTag + "], flags=0x" + Integer.toHexString(flags));
        }
        if (PowerManagerService.isScreenLock(wakeLock)) {
            this.mPowerManagerService.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 1, 1000);
        }
    }

    private void notifyProxLockUid(int flags, int uid) {
        VivoSensorImpl vivoSensorImpl;
        if ((65535 & flags) == 32 && (vivoSensorImpl = this.mVivoSensorImpl) != null) {
            vivoSensorImpl.notifyProxLockUid(uid);
        }
    }

    public void updateDisplayPowerStateLocked() {
        this.mDisplayPowerRequest.isPowerKeyWakeUp = false;
        if ((this.mPowerManagerService.mDisplayPowerRequest.policy == 3 || this.mPowerManagerService.mDisplayPowerRequest.policy == 2) && this.mPowerManagerService.getWakefulnessLocked() == 1) {
            if ((this.mPowerManagerService.mUserActivitySummary & 8) != 0) {
                this.mHandler.sendEmptyMessageDelayed(11, 100L);
                if (DEBUG_POWER) {
                    VSlog.i(TAG, "setBrightness mButtonLight, delayed=100");
                    return;
                }
                return;
            }
            this.mHandler.removeMessages(11);
            LogicalLight logicalLight = this.mButtonLight;
            if (logicalLight != null) {
                logicalLight.turnOff();
            } else {
                VSlog.d(TAG, "mButtonLight == null");
            }
            if (DEBUG_POWER) {
                VSlog.i(TAG, "setBrightness mButtonLight 0 111.");
                return;
            }
            return;
        }
        this.mHandler.removeMessages(11);
        LogicalLight logicalLight2 = this.mButtonLight;
        if (logicalLight2 != null) {
            logicalLight2.turnOff();
        } else {
            VSlog.d(TAG, "mButtonLight == null");
        }
        if (DEBUG_POWER) {
            VSlog.i(TAG, "setBrightness mButtonLight 0 222.");
        }
    }

    public void updateIsPoweredLocked() {
        if (this.mPowerManagerService.mBatteryLevel <= 10) {
            this.mButtonLightOn = false;
            this.mButtonLightAlwaysOn = false;
            LogicalLight logicalLight = this.mButtonLight;
            if (logicalLight != null) {
                logicalLight.turnOff();
                VSlog.d(TAG, "button light turn off when BatteryLevel <= 10%.");
                return;
            }
            VSlog.d(TAG, "mButtonLight == null");
            return;
        }
        if (this.buttonLightControlMode != 3 && !this.mButtonLightOn) {
            this.mButtonLightOn = true;
            VSlog.d(TAG, "set button light flag to true.");
        }
        if (this.buttonLightControlMode == 2 && !this.mButtonLightAlwaysOn) {
            this.mButtonLightAlwaysOn = true;
            VSlog.d(TAG, "set button light always flag to true.");
        }
        if (this.mButtonLightAlwaysOn && PowerManagerInternal.isInteractive(this.mPowerManagerService.getWakefulnessLocked())) {
            LogicalLight logicalLight2 = this.mButtonLight;
            if (logicalLight2 != null) {
                logicalLight2.setBrightness(255.0f);
                VSlog.d(TAG, "button light always on light up it");
                return;
            }
            VSlog.d(TAG, "mButtonLight == null");
        }
    }

    public long updateUserActivitySummaryLocked_step1(long now, int dirty, long screenDimDuration, long screenOffTimeout, long sleepTimeout) {
        int screenButtonLightDuration = getButtonLightDurationLocked(screenOffTimeout);
        screenDimDuration = (this.isKeyguarActive || this.mEasyShareOn) ? 0L : 0L;
        boolean isButtonNeedReflect = this.mPowerManagerService.mPolicy.isButtonNeedReflect();
        if (isButtonNeedReflect) {
            this.isButtonNeedReflectGlobal = true;
            this.mHandler.removeMessages(99);
            PowerManagerHandler powerManagerHandler = this.mHandler;
            powerManagerHandler.sendMessageDelayed(powerManagerHandler.obtainMessage(99), screenButtonLightDuration);
        }
        if (DEBUG) {
            VSlog.d(TAG, "updateUserActivitySummaryLocked: sleepTimeout = " + sleepTimeout + " screenOffTimeout = " + screenOffTimeout + " screenDimDuration = " + screenDimDuration + " screenButtonLightDuration = " + screenButtonLightDuration + " isKeyguarActive = " + this.isKeyguarActive + " isButtonNeedReflect = " + isButtonNeedReflect + " isButtonLightSpecialScene = " + this.isButtonLightSpecialScene + " isButtonLightSpecialSceneException = " + this.isButtonLightSpecialSceneException + " isButtonNeedReflectGlobal = " + this.isButtonNeedReflectGlobal);
        }
        return screenDimDuration;
    }

    public long updateUserActivitySummaryLocked_step2(long now, int dirty, long screenDimDuration, long screenOffTimeout, long nextTimeout) {
        long nextTimeout2;
        int screenButtonLightDuration = getButtonLightDurationLocked(screenOffTimeout);
        if (this.mLastUserActivityButtonTime >= this.mPowerManagerService.mLastWakeTime && now < this.mLastUserActivityButtonTime + screenButtonLightDuration) {
            if ((this.mButtonLightAlwaysOn && !fingerprintBlockNotifier()) || ((this.isButtonLightSpecialScene && !this.isButtonLightSpecialSceneException && this.mButtonLightOn) || (this.mButtonLightOn && !fingerprintBlockNotifier() && (screenDimDuration == 0 || this.isButtonNeedReflectGlobal)))) {
                if (DEBUG_POWER) {
                    VSlog.d(TAG, " updateUserActivitySummaryLocked mButtonLight 11111");
                }
                this.mPowerManagerService.mUserActivitySummary |= 8;
                if (this.isButtonLightSpecialScene) {
                    this.isButtonLightSpecialScene = false;
                }
                if (this.isButtonLightSpecialSceneException) {
                    this.isButtonLightSpecialSceneException = false;
                }
            }
            this.mPowerManagerService.mUserActivitySummary |= 1;
            nextTimeout2 = (this.mPowerManagerService.mLastUserActivityTime + screenOffTimeout) - screenDimDuration;
        } else if (now < (this.mPowerManagerService.mLastUserActivityTime + screenOffTimeout) - screenDimDuration) {
            if (DEBUG_POWER) {
                VSlog.d(TAG, " updateUserActivitySummaryLocked mButtonLight 2222");
            }
            nextTimeout2 = (this.mPowerManagerService.mLastUserActivityTime + screenOffTimeout) - screenDimDuration;
            this.mPowerManagerService.mUserActivitySummary |= 1;
            if (this.mButtonLightAlwaysOn) {
                this.mPowerManagerService.mUserActivitySummary |= 8;
            }
        } else {
            if (DEBUG_POWER) {
                VSlog.d(TAG, "mButtonLight test 1111 ");
            }
            nextTimeout2 = this.mPowerManagerService.mLastUserActivityTime + screenOffTimeout;
            if (now < nextTimeout2) {
                this.mPowerManagerService.mUserActivitySummary |= 2;
                if (this.mButtonLightAlwaysOn) {
                    this.mPowerManagerService.mUserActivitySummary |= 8;
                }
            }
        }
        if (DEBUG_POWER) {
            VSlog.d(TAG, "mButtonLight test mUserActivitySummary = " + this.mPowerManagerService.mUserActivitySummary);
        }
        return nextTimeout2;
    }

    public long getScreenOffTimeoutLocked(long sleepTimeout, long timeout) {
        return this.isKeyguarActive ? (int) Math.min(timeout, 7000L) : timeout;
    }

    private boolean isSmartMirrorFirstoff() {
        boolean ret = false;
        if (SystemProperties.getInt("sys.vivo.mirroring", 0) == 1 && Settings.Secure.getInt(this.mContext.getContentResolver(), "vscreen_always_on", 0) == 0 && Settings.Secure.getInt(this.mContext.getContentResolver(), "vscreen_first_off", 0) == 0) {
            ret = true;
        }
        VSlog.d(TAG, "isSmartMirrorFirstoff ret:" + ret);
        return ret;
    }

    private void realHandleSmartMirrorFirstoff() {
        this.mCallEndWaitHandler.post(this.mSendBroadcastForApp);
        this.mPowerManagerService.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 1, 1000);
        this.mPowerManagerService.mDirty |= 4;
        this.mPowerManagerService.updatePowerStateLocked();
    }

    public boolean handleSmartMirrorFirstoffIfNeeded(boolean shouldHandle) {
        boolean ret = isSmartMirrorFirstoff();
        if (ret && shouldHandle) {
            realHandleSmartMirrorFirstoff();
        }
        return ret;
    }

    private boolean isSmartMirrorOn() {
        if (SystemProperties.getInt("sys.vivo.mirroring", 0) == 1) {
            try {
                if (Settings.Secure.getInt(this.mContext.getContentResolver(), "vscreen_always_on", 0) == 1) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean handleSmartMirrorOnIfNeeded(boolean shouldHandle) {
        boolean ret = isSmartMirrorOn();
        if (ret && shouldHandle) {
            VSlog.d(TAG, "handleSmartMirrorOn ");
            if (!this.mPowerManagerService.mDisplayManagerInternal.shouldWakeUpWhileInteractive()) {
                this.mPowerManagerService.mDisplayManagerInternal.setForceDisplayBrightnessOff(true, "smartmirror");
            }
        }
        return ret;
    }

    public boolean isBeingKeptAwakeLocked(boolean mStayOn, boolean mProximityPositive, int mWakeLockSummary, int mUserActivitySummary, boolean mScreenBrightnessBoostInProgress) {
        return mStayOn || mProximityPositive || !((mWakeLockSummary & 32) == 0 || this.isKeyguarActive) || (mUserActivitySummary & 3) != 0 || mScreenBrightnessBoostInProgress || this.mEasyShareOn || isSmartMirrorOn() || isSmartMirrorFirstoff();
    }

    public boolean getWakeLockSummaryFlags(PowerManagerService.WakeLock wakeLock) {
        return wakeLock.isShouldKeepScreenOn();
    }

    public void dumpInternal(PrintWriter pw) {
        pw.println("  mButtonLightOn=" + this.mButtonLightOn);
        pw.println("  mButtonLightAlwaysOn=" + this.mButtonLightAlwaysOn);
        pw.println("  mButtonLightSensorControl=" + this.mButtonLightSensorControl);
        pw.println("  buttonLightControlMode=" + this.buttonLightControlMode);
        pw.println("  isButtonLightSpecialScene=" + this.isButtonLightSpecialScene);
        pw.println("  isButtonLightSpecialSceneException=" + this.isButtonLightSpecialSceneException);
        pw.println("  isButtonNeedReflectGlobal=" + this.isButtonNeedReflectGlobal);
    }

    public void finishWakefulnessChangeIfNeededLocked() {
    }

    /* loaded from: classes.dex */
    private class DumpTimeoutHandler extends Handler {
        DumpTimeoutHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 12) {
                int pid = msg.arg1;
                int uid = msg.arg2;
                if (pid != Process.myPid()) {
                    Process.killProcess(pid);
                    VSlog.d(VivoPowerManagerServiceImpl.TAG, "PowerManager kill pid:" + pid + " ,uid:" + uid + " ,reason: do dump take too long time.");
                    return;
                }
                VSlog.d(VivoPowerManagerServiceImpl.TAG, "oops, system_server do dump take too long time");
            }
        }
    }

    public boolean dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args != null && args.length >= 2) {
            if (args[0].equals("log") && args[1].equals("enable")) {
                DEBUG = true;
                DEBUG_POWER = true;
                PowerManagerService.DEBUG = true;
                PowerManagerService.DEBUG_SPEW = true;
            } else if (args[0].equals("log") && args[1].equals("disable")) {
                DEBUG = false;
                DEBUG_POWER = false;
                PowerManagerService.DEBUG = false;
                PowerManagerService.DEBUG_SPEW = false;
            }
            VSlog.d(TAG, "DEBUG = " + DEBUG + ", DEBUG_POWER = " + DEBUG_POWER);
            return true;
        }
        VSlog.d(TAG, "dump called here! DEBUG = " + DEBUG + ", DEBUG_POWER = " + DEBUG_POWER + "Calling PID = " + Binder.getCallingPid());
        Message message = this.mDumpTimeoutHandler.obtainMessage(12);
        message.arg1 = Binder.getCallingPid();
        message.arg2 = Binder.getCallingUid();
        this.mDumpTimeoutHandler.sendMessageDelayed(message, VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
        return false;
    }

    public void removeDumpTimeoutMsg() {
        this.mDumpTimeoutHandler.removeMessages(12);
    }

    public int BinderService_getPlugType() {
        long ident = Binder.clearCallingIdentity();
        try {
            return getPlugTypeInternal();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void BinderService_lockNow() {
        VSlog.d(TAG, "lockNow ! Calling PID = " + Binder.getCallingPid());
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            this.mPowerManagerService.goToSleepInternal(SystemClock.uptimeMillis(), 0, 0, uid);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void BinderService_lightupNow() {
        VSlog.d(TAG, "lightupNow ! Calling PID = " + Binder.getCallingPid());
        this.mLightUpNow = true;
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            this.mPowerManagerService.wakeUpInternal(SystemClock.uptimeMillis(), 2, "vivo-app", uid, "vivo-app", uid);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void BinderService_setButtonLightMode(int buttonLightMode) {
        long ident = Binder.clearCallingIdentity();
        try {
            setButtonLightModeInternal(buttonLightMode);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void BinderService_notifyPhoneState(int state) {
        long ident = Binder.clearCallingIdentity();
        try {
            notifyPhoneStateInternal(state);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void BinderService_notifyKeyguardActive(boolean active) {
        long ident = Binder.clearCallingIdentity();
        try {
            notifyKeyguardActiveInternal(active);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void dumpWakeLockLocked() {
        int numWakeLocks = this.mPowerManagerService.mWakeLocks.size();
        if (numWakeLocks > 0) {
            VSlog.d(TAG, "wakelock list dump: mLocks.size=" + numWakeLocks + ":");
            for (int i = 0; i < numWakeLocks; i++) {
                PowerManagerService.WakeLock wakeLock = (PowerManagerService.WakeLock) this.mPowerManagerService.mWakeLocks.get(i);
                String type = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                int i2 = wakeLock.mFlags & 65535;
                if (i2 == 1) {
                    type = "PARTIAL_WAKE_LOCK";
                } else if (i2 == 6) {
                    type = "SCREEN_DIM_WAKE_LOCK";
                } else if (i2 == 10) {
                    type = "SCREEN_BRIGHT_WAKE_LOCK";
                } else if (i2 == 26) {
                    type = "FULL_WAKE_LOCK";
                } else if (i2 == 32) {
                    type = "PROXIMITY_SCREEN_OFF_WAKE_LOCK";
                } else if (i2 == 64) {
                    type = "DOZE_WAKE_LOCK";
                }
                long total_time = SystemClock.uptimeMillis() - wakeLock.mAcquireTime;
                VSlog.d(TAG, "No." + i + ": " + type + " '" + wakeLock.mTag + "'activated(flags=" + wakeLock.mFlags + ", uid=" + wakeLock.mOwnerUid + ", pid=" + wakeLock.mOwnerPid + ") during_total=" + total_time + "ms)");
            }
        }
    }

    private boolean fingerprintBlockNotifier() {
        return false;
    }

    private int getButtonLightDurationLocked(long screenOffTimeout) {
        int i = this.buttonLightControlMode;
        if (i != 1) {
            if (i == 3) {
                return 0;
            }
            return 2000;
        }
        return 6000;
    }

    /* loaded from: classes.dex */
    private class WakeLockControlHandler extends Handler {
        WakeLockControlHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int oldPid;
            if (msg.what == 1) {
                boolean isSplidMode = VivoPowerManagerServiceImpl.isSplitMode();
                boolean isPinnedStackExist = VivoPowerManagerServiceImpl.isPinnedStackExist();
                synchronized (VivoPowerManagerServiceImpl.this.mPowerManagerService.mLock) {
                    int pid = msg.arg1;
                    int i = msg.arg2;
                    boolean foregroundActivity = ((Boolean) msg.obj).booleanValue();
                    int sizePids = VivoPowerManagerServiceImpl.this.mTopPids.size();
                    int newPid = VivoPowerManagerServiceImpl.INVALID_PID;
                    if (sizePids > 0) {
                        oldPid = ((Integer) VivoPowerManagerServiceImpl.this.mTopPids.get(sizePids - 1)).intValue();
                    } else {
                        oldPid = VivoPowerManagerServiceImpl.INVALID_PID;
                    }
                    if (foregroundActivity) {
                        int index = VivoPowerManagerServiceImpl.this.mTopPids.indexOf(Integer.valueOf(pid));
                        if (-1 == index) {
                            VivoPowerManagerServiceImpl.this.mTopPids.add(Integer.valueOf(pid));
                        } else if (index != sizePids - 1) {
                            VivoPowerManagerServiceImpl.this.mTopPids.remove(index);
                            VivoPowerManagerServiceImpl.this.mTopPids.add(Integer.valueOf(pid));
                        }
                    } else {
                        int rmIndex = VivoPowerManagerServiceImpl.this.mTopPids.indexOf(Integer.valueOf(pid));
                        if (-1 != rmIndex) {
                            VivoPowerManagerServiceImpl.this.mTopPids.remove(rmIndex);
                        }
                    }
                    int newSize = VivoPowerManagerServiceImpl.this.mTopPids.size();
                    if (newSize > 0) {
                        newPid = ((Integer) VivoPowerManagerServiceImpl.this.mTopPids.get(newSize - 1)).intValue();
                    }
                    if (newPid == oldPid) {
                        return;
                    }
                    if (VivoPowerManagerServiceImpl.DEBUG_POWER) {
                        VSlog.d(VivoPowerManagerServiceImpl.TAG, "fgApp changed!");
                    }
                    VivoPowerManagerServiceImpl.this.updateWakeLockScreenLocked(newPid, isSplidMode, isPinnedStackExist);
                    return;
                }
            }
            VSlog.d(VivoPowerManagerServiceImpl.TAG, "unknow message" + msg.what);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateWakeLockScreenLocked(int foregroundPid, boolean isSplitMode, boolean isPinnedStackExist) {
        if (foregroundPid == INVALID_PID) {
            return;
        }
        boolean isChange = false;
        int wakeLockCount = this.mPowerManagerService.mWakeLocks.size();
        for (int i = 0; i < wakeLockCount; i++) {
            PowerManagerService.WakeLock wakeLock = (PowerManagerService.WakeLock) this.mPowerManagerService.mWakeLocks.get(i);
            if (PowerManagerService.isScreenLock(wakeLock)) {
                if (foregroundPid == wakeLock.mOwnerPid) {
                    if (!wakeLock.isShouldKeepScreenOn()) {
                        wakeLock.setShouldKeepScreenOn(true);
                        isChange = true;
                    }
                } else if (wakeLock.mOwnerPid != this.myPid && !this.mWhiteUidList.contains(Integer.valueOf(wakeLock.mOwnerUid)) && !isSplitMode && !isPinnedStackExist && wakeLock.isShouldKeepScreenOn()) {
                    wakeLock.setShouldKeepScreenOn(false);
                    isChange = true;
                }
            }
        }
        if (isChange) {
            VSlog.d(TAG, "wake lock shouldKeepScreenOn isChange");
            this.mPowerManagerService.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 1, 1000);
            PowerManagerService powerManagerService = this.mPowerManagerService;
            powerManagerService.mDirty = 1 | powerManagerService.mDirty;
            this.mPowerManagerService.updatePowerStateLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isPinnedStackExist() {
        if (mWindowManagerInternal == null) {
            mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        }
        return mWindowManagerInternal.isPinnedStackExist();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isSplitMode() {
        try {
            return WindowManagerGlobal.getWindowManagerService().isSplitScreenModeActivated();
        } catch (RemoteException e) {
            VSlog.w(TAG, "isSplitMode remote exception:" + e);
            return false;
        }
    }

    private boolean isFlagPartialLock(int flags) {
        return (65535 & flags) == 1;
    }

    private void getWhiteUidList() {
        for (int i = 0; i < this.mWhitePkgList.length; i++) {
            try {
                ApplicationInfo applicationInfo = this.mContext.getPackageManager().getApplicationInfo(this.mWhitePkgList[i], 0);
                if (applicationInfo != null) {
                    this.mWhiteUidList.add(Integer.valueOf(applicationInfo.uid));
                    ArrayList<Integer> arrayList = this.mWhiteUidList;
                    arrayList.add(Integer.valueOf(Integer.parseInt("999" + Integer.toString(applicationInfo.uid))));
                }
            } catch (PackageManager.NameNotFoundException e) {
                VSlog.d(TAG, this.mWhitePkgList[i] + " App not found!");
            }
        }
    }

    public int getPlugTypeInternal() {
        return this.mPowerManagerService.mPlugType;
    }

    public void notifyKeyguardActiveInternal(boolean active) {
        synchronized (this.mPowerManagerService.mLock) {
            if (this.isKeyguarActive != active) {
                VSlog.d(TAG, " Received from keyguard isKeyguarActive = " + this.isKeyguarActive);
                this.isKeyguarActive = active;
                long now = SystemClock.uptimeMillis();
                this.mPowerManagerService.userActivityNoUpdateLocked(now, 0, 0, 1000);
                this.mPowerManagerService.mDirty |= 1;
                this.mPowerManagerService.updatePowerStateLocked();
            }
        }
    }

    public void notifyPhoneStateInternal(int state) {
        if (state == 0 || state == 1 || state == 2 || state == 3) {
            this.mPhoneCallState = state;
        } else {
            VSlog.d(TAG, "notifyPhoneInternal unkown:" + state);
        }
        VivoSensorImpl vivoSensorImpl = this.mVivoSensorImpl;
        if (vivoSensorImpl != null) {
            vivoSensorImpl.notifyPhoneState(state);
        }
        if (state == 0) {
            this.mPowerManagerService.mDisplayManagerInternal.notifyStateChanged(8);
        } else if (state == 1) {
            this.mPowerManagerService.mDisplayManagerInternal.notifyStateChanged(9);
        } else if (state != 2) {
            if (state == 3) {
                this.mPowerManagerService.mDisplayManagerInternal.notifyStateChanged(11);
            }
        } else {
            this.mPowerManagerService.mDisplayManagerInternal.notifyStateChanged(10);
        }
    }

    private void setButtonLightModeInternal(int buttonLightMode) {
        VLog.d(TAG, " ** setButtonLightMode( " + buttonLightMode + " )");
        if (buttonLightMode != 0) {
            if (buttonLightMode == 1) {
                if (this.mButtonLightSensorControl) {
                    this.mSensorManager.unregisterListener(this.mLightListenerForButtonLightControl);
                    this.mButtonLightSensorControl = false;
                    this.mButtonLightSensorLastActive = false;
                }
                Settings.System.putInt(this.mContext.getContentResolver(), "key_button_light_control_mode", 1);
                synchronized (this.mPowerManagerService.mLock) {
                    this.buttonLightControlMode = buttonLightMode;
                    this.mButtonLightOn = true;
                    this.mButtonLightAlwaysOn = false;
                    this.isButtonLightSpecialScene = true;
                    if (this.mPowerManagerService.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 1, 0, 1000)) {
                        this.mPowerManagerService.mDirty |= 256;
                        this.mPowerManagerService.updatePowerStateLocked();
                    }
                }
                return;
            } else if (buttonLightMode == 2) {
                if (this.mButtonLightSensorControl) {
                    this.mSensorManager.unregisterListener(this.mLightListenerForButtonLightControl);
                    this.mButtonLightSensorControl = false;
                    this.mButtonLightSensorLastActive = false;
                }
                VSlog.i(TAG, "setBrightness mButtonLight on 222.");
                Settings.System.putInt(this.mContext.getContentResolver(), "key_button_light_control_mode", 2);
                synchronized (this.mPowerManagerService.mLock) {
                    this.buttonLightControlMode = buttonLightMode;
                    this.mButtonLightOn = true;
                    this.mButtonLightAlwaysOn = true;
                    if (this.mPowerManagerService.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 1, 0, 1000)) {
                        this.mPowerManagerService.mDirty |= 256;
                        this.mPowerManagerService.updatePowerStateLocked();
                    }
                }
                return;
            } else if (buttonLightMode != 3) {
                if (buttonLightMode == 4) {
                    this.mSensorManager.registerListener(this.mLightListenerForButtonLightControl, this.mLightSensorForButtonLightControl, 3);
                    this.mButtonLightSensorLastActive = false;
                    this.mButtonLightSensorControl = true;
                    Settings.System.putInt(this.mContext.getContentResolver(), "key_button_light_control_mode", 4);
                    synchronized (this.mPowerManagerService.mLock) {
                        this.buttonLightControlMode = buttonLightMode;
                        this.mButtonLightOn = false;
                        this.mButtonLightAlwaysOn = false;
                    }
                    return;
                }
                VLog.d(TAG, "*** the argument is not correct mode = " + buttonLightMode);
                return;
            } else {
                if (this.mButtonLightSensorControl) {
                    this.mSensorManager.unregisterListener(this.mLightListenerForButtonLightControl);
                    this.mButtonLightSensorControl = false;
                    this.mButtonLightSensorLastActive = false;
                }
                VSlog.i(TAG, "setBrightness mButtonLight 0 444.");
                Settings.System.putInt(this.mContext.getContentResolver(), "key_button_light_control_mode", 3);
                synchronized (this.mPowerManagerService.mLock) {
                    this.buttonLightControlMode = buttonLightMode;
                    this.mButtonLightOn = false;
                    this.mButtonLightAlwaysOn = false;
                    this.mHandler.removeMessages(11);
                    if (this.mButtonLight != null) {
                        this.mButtonLight.turnOff();
                    } else {
                        VSlog.d(TAG, "mButtonLight == null");
                    }
                }
                return;
            }
        }
        if (this.mButtonLightSensorControl) {
            this.mSensorManager.unregisterListener(this.mLightListenerForButtonLightControl);
            this.mButtonLightSensorControl = false;
            this.mButtonLightSensorLastActive = false;
        }
        Settings.System.putInt(this.mContext.getContentResolver(), "key_button_light_control_mode", 0);
        synchronized (this.mPowerManagerService.mLock) {
            this.buttonLightControlMode = buttonLightMode;
            this.mButtonLightOn = true;
            this.mButtonLightAlwaysOn = false;
            this.isButtonLightSpecialScene = true;
            if (this.mPowerManagerService.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 1, 0, 1000)) {
                this.mPowerManagerService.mDirty |= 256;
                this.mPowerManagerService.updatePowerStateLocked();
            }
        }
    }

    private void proximityStatusReset(long eventTime, String who, boolean mSystemReady) {
        VivoSensorImpl vivoSensorImpl;
        if (mSystemReady && (vivoSensorImpl = this.mVivoSensorImpl) != null) {
            if (vivoSensorImpl.getAntiTrigger()) {
                VSlog.d(TAG, "ANTI-MIS: wakeUp who=" + who + " enableAntiMonitor(false)");
                this.mVivoSensorImpl.enableAntiMonitor(false);
            } else if (this.mVivoSensorImpl.getAntiEnable()) {
                VSlog.d(TAG, "ANTI-MIS: wakeUp who=" + who + " enableAntiMonitor(false)");
                this.mVivoSensorImpl.enableAntiMonitor(false);
            }
        }
        if (this.mPowerManagerService.mProximityPositive) {
            VSlog.d(TAG, "reset mProximityPositive for screen on");
            this.mPowerManagerService.mProximityPositive = false;
            this.mHasUnansweredCallBlockScreenOn = false;
        }
    }

    public void wakeUp(long eventTime, int reason, String details, int uid, String opPackageName, int opUid, long identity, boolean mSystemReady) {
        VivoSensorImpl vivoSensorImpl;
        VSlog.e(TAG, "wakeUp(Binder call) uid=" + uid + " ident=" + identity + " reason=" + reason + " details=" + details + " pkgName=" + opPackageName + " mProximityEnabled=" + this.mProximityEnabled + " mProximityStatus=" + this.mProximityStatus);
        if (details != null && opPackageName != null && (reason == 2 || details.contains("onProximityNegative") || opPackageName.contains("android.server.telecom"))) {
            if (this.mProximityEnabled && this.mProximityStatus) {
                VSlog.d(TAG, "useProximity && mProximityPositive , can not wake up! return");
                return;
            } else if (mSystemReady && (vivoSensorImpl = this.mVivoSensorImpl) != null) {
                if (vivoSensorImpl.getAntiEnable() && this.mVivoSensorImpl.isAntiProxPositive() && this.mVivoSensorImpl.isBlockWakeUp(details)) {
                    VSlog.d(TAG, "ANTI-MIS: wakeUp BLOCKED!!! return. triggered=" + this.mVivoSensorImpl.getAntiTrigger() + " proximity=" + this.mVivoSensorImpl.isAntiProxPositive());
                    return;
                } else if (this.mVivoSensorImpl.getAntiTrigger() || this.mVivoSensorImpl.getAntiEnable()) {
                    VSlog.d(TAG, "ANTI-MIS: wakeUp enableAntiMonitor(false)");
                    this.mVivoSensorImpl.enableAntiMonitor(false);
                }
            }
        }
        if (details != null && reason == 1) {
            proximityStatusReset(eventTime, details, mSystemReady);
        }
        this.mPowerManagerService.wakeUpInternal(eventTime, reason, details, uid, opPackageName, uid);
        onWakeupKeyPressd(details);
    }

    private int removeHoldWakeLockUid(int uid) {
        ArrayList<PowerManagerService.WakeLock> needReleaseWakeLocks = new ArrayList<>();
        synchronized (this.mPowerManagerService.mLock) {
            int numWkLocks = this.mPowerManagerService.mWakeLocks.size();
            if (numWkLocks <= 0) {
                return 0;
            }
            Iterator it = this.mPowerManagerService.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wl = (PowerManagerService.WakeLock) it.next();
                if (wl != null) {
                    if (wl.mOwnerUid == uid) {
                        needReleaseWakeLocks.add(wl);
                    } else if (wl.mWorkSource != null && wl.mWorkSource.get(0) == uid) {
                        needReleaseWakeLocks.add(wl);
                    }
                }
            }
            Iterator<PowerManagerService.WakeLock> it2 = needReleaseWakeLocks.iterator();
            while (it2.hasNext()) {
                PowerManagerService.WakeLock next = it2.next();
                VSlog.d(TAG, "removeWakeLockByFrozen: lock=" + Objects.hashCode(next.mLock) + " [" + next.mTag + "], flags=0x" + Integer.toHexString(next.mFlags));
                int index = this.mPowerManagerService.findWakeLockIndexLocked(next.mLock);
                if (index >= 0) {
                    try {
                        next.mLock.unlinkToDeath(next, 0);
                        this.mPowerManagerService.removeWakeLockLocked(next, index);
                    } catch (NoSuchElementException e) {
                        VSlog.e(TAG, "removeHoldWakeLockUid NoSuchElementException");
                    }
                } else {
                    VSlog.d(TAG, "removeWakeLockByFrozen: lock=" + Objects.hashCode(next.mLock) + " [not found], flags=0x" + Integer.toHexString(next.mFlags));
                }
            }
            synchronized (this.mFrozenWakeLocks) {
                this.mFrozenWakeLocks.addAll(needReleaseWakeLocks);
            }
            return needReleaseWakeLocks.size();
        }
    }

    public boolean removeFrozenWakeLock(IBinder lock, boolean needRemove) {
        synchronized (this.mFrozenWakeLocks) {
            int find = -1;
            int count = this.mFrozenWakeLocks.size();
            int i = 0;
            while (true) {
                if (i >= count) {
                    break;
                }
                PowerManagerService.WakeLock wl = this.mFrozenWakeLocks.get(i);
                if (wl.mLock != lock) {
                    i++;
                } else {
                    find = i;
                    VSlog.d(TAG, "lock=" + Objects.hashCode(wl.mLock) + " [" + wl.mTag + "], flags=0x" + Integer.toHexString(wl.mFlags) + " has been remove.");
                    break;
                }
            }
            if (find > -1) {
                if (needRemove) {
                    this.mFrozenWakeLocks.remove(find);
                }
                return true;
            }
            return false;
        }
    }

    public int onFrozenPackage(String packageName, int uid) {
        if (packageName == null || uid < 10000) {
            return 0;
        }
        try {
            return removeHoldWakeLockUid(uid);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void changeDisplayDozeState() {
        boolean oldPrimaryDisplayDozeRequired = this.mPrimaryDisplayDozeRequired;
        boolean oldSecondaryDisplayDozeRequired = this.mSecondaryDisplayDozeRequired;
        this.mPrimaryDisplayDozeRequired = false;
        this.mSecondaryDisplayDozeRequired = false;
        int numWakeLocks = this.mPowerManagerService.mWakeLocks.size();
        for (int i = 0; i < numWakeLocks; i++) {
            PowerManagerService.WakeLock wakeLock = (PowerManagerService.WakeLock) this.mPowerManagerService.mWakeLocks.get(i);
            if ((wakeLock.mFlags & 65535) == 128) {
                if (wakeLock.getDisplayId() == 0) {
                    this.mPrimaryDisplayDozeRequired = true;
                } else if (wakeLock.getDisplayId() == 4096) {
                    this.mSecondaryDisplayDozeRequired = true;
                }
            }
        }
        if (oldPrimaryDisplayDozeRequired != this.mPrimaryDisplayDozeRequired) {
            VSlog.d(TAG, "primary-display: pending-doze: " + this.mPrimaryDisplayDozeRequired + " required-doze: " + oldPrimaryDisplayDozeRequired);
            if (this.mPrimaryDisplayDozeRequired && this.mDisplayManager != null) {
                requestDisplayState(0, 3);
            } else if (!this.mPrimaryDisplayDozeRequired && this.mDisplayManager != null) {
                requestDisplayState(0, 4);
            }
        }
        if (oldSecondaryDisplayDozeRequired != this.mSecondaryDisplayDozeRequired) {
            VSlog.d(TAG, "secondary-display: pending-doze: " + this.mSecondaryDisplayDozeRequired + " required-doze: " + oldSecondaryDisplayDozeRequired);
            if (this.mSecondaryDisplayDozeRequired && this.mDisplayManager != null) {
                requestDisplayState(4096, 3);
            } else if (!this.mSecondaryDisplayDozeRequired && this.mDisplayManager != null) {
                requestDisplayState(4096, 4);
            }
        }
    }

    private void requestDisplayState(int displayId, int displayState) {
        if (this.mVivoDisplayStateInternal == null) {
            this.mVivoDisplayStateInternal = (VivoDisplayStateInternal) LocalServices.getService(VivoDisplayStateInternal.class);
        }
        VivoDisplayStateInternal vivoDisplayStateInternal = this.mVivoDisplayStateInternal;
        if (vivoDisplayStateInternal != null) {
            vivoDisplayStateInternal.requestDisplayState(displayId, -1, displayState, 0);
        }
    }

    public boolean shutdownReturnIfNeeded(boolean confirm, String reason, boolean wait, int pid) {
        VSlog.d(TAG, "reboot confirm=" + confirm + " ,reason=" + reason + " ,wait=" + wait + " ,callingPid=" + Binder.getCallingPid());
        if (isMonkeyRunning() && !this.mBatteryLow) {
            VSlog.d(TAG, "Forbit shutdown while monkey is running");
            return true;
        }
        return false;
    }

    public boolean rebootSafeModeReturnIfNeeded(boolean confirm, boolean wait, int pid) {
        VSlog.d(TAG, "reboot confirm=" + confirm + " ,wait=" + wait + " ,callingPid=" + Binder.getCallingPid());
        if (isMonkeyRunning() && !this.mBatteryLow) {
            VSlog.d(TAG, "Forbit rebootSafeMode while monkey is running");
            return true;
        }
        return false;
    }

    public boolean rebootReturnIfNeeded(boolean confirm, String reason, boolean wait, int pid) {
        VSlog.d(TAG, "reboot confirm=" + confirm + " ,reason=" + reason + " ,wait=" + wait + " ,callingPid=" + Binder.getCallingPid());
        if (isMonkeyRunning() && !this.mBatteryLow) {
            VSlog.d(TAG, "Forbit reboot while monkey is running");
            return true;
        }
        return false;
    }

    private int getWakeUpReason(String reason) {
        if (reason == null) {
            return 10;
        }
        if (reason.equals(WAKE_UP_FOR_DOUBLE_TAP)) {
            return 11;
        }
        if (reason.equals(WAKE_UP_FOR_FLINGERPRINT)) {
            return 12;
        }
        if (reason.equals(WAKE_UP_FOR_POCKETMODE)) {
            return 13;
        }
        if (reason.equals(WAKE_UP_FOR_RAISEUP)) {
            return 14;
        }
        if (reason.equals(WAKE_UP_FOR_WAKEUPKEY)) {
            return 15;
        }
        if (reason.equals(WAKE_UP_FOR_SMARTKEY)) {
            return 16;
        }
        if (reason.equals(WAKE_UP_FOR_AIKEY)) {
            return 17;
        }
        if (!reason.equals(WAKE_UP_FOR_SMARTWAKE)) {
            return 10;
        }
        return 18;
    }

    private boolean computeDriveMode() {
        boolean driveModeEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "drive_mode_enabled", 0, -2) == 1;
        boolean shieldNotificationEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "shield_notification_reminder_enabled", 0, -2) == 1;
        return driveModeEnabled && shieldNotificationEnabled;
    }

    private boolean getColorFadeAnimationOn() {
        int style = Settings.System.getInt(this.mContext.getContentResolver(), "global_animation_color_fade_style", -1);
        return style != -1;
    }

    private boolean isMonkeyRunning() {
        return SystemProperties.get("sys.bsptest.monkey", "0").equals("1");
    }

    public boolean adjustDrawWakeLockSummary(int wakefulness) {
        return (wakefulness == 3 || this.mPowerManagerService.mDisplayManagerInternal.isDozeOrSuspendMode(0) || this.mPowerManagerService.mDisplayManagerInternal.isDozeOrSuspendMode(4096)) ? false : true;
    }

    public boolean considerColorFade(boolean dozeAfterScreenOff) {
        if (!dozeAfterScreenOff) {
            if (this.mColorFadeAnimationOn && this.mPowerManagerService.mPolicy != null && !this.mPowerManagerService.mPolicy.isKeyguardShowingAndNotOccluded()) {
                return true;
            }
            return false;
        }
        return true;
    }

    public void handleBatteryStateChangedLocked(Context context, Intent intent) {
        this.mBatteryLow = intent.getIntExtra("level", -1) == 0 || intent.getIntExtra("voltage", -1) < 3300;
    }

    public void setScreenBrightnessOverrideFromPem(int brightness) {
        if (brightness > 255 || brightness < 0) {
            this.mScreenBrightnessOverrideFromPem = INITIALBRIGHTNESS;
        } else {
            this.mScreenBrightnessOverrideFromPem = SensorConfig.algoBrightness2Float(SensorConfig.br255MapToalgo(brightness));
        }
        this.mDisplayPowerRequest.screenBrightnessOverrideFromPem = this.mScreenBrightnessOverrideFromPem;
        this.mPowerManagerService.mDirty |= 32;
        this.mPowerManagerService.updatePowerStateLocked();
        VSlog.i(TAG, "brightness = " + brightness + " mScreenBrightnessOverrideFromPem " + this.mScreenBrightnessOverrideFromPem);
    }

    public void notifyBrightnessFromWindowManager(float brightness) {
        VivoSensorImpl vivoSensorImpl = this.mVivoSensorImpl;
        if (vivoSensorImpl != null) {
            vivoSensorImpl.notifyBrightnessFromWindowManager(brightness);
        } else {
            VSlog.w(TAG, "notifyBrightnessFromWindowManager mVivoSensorImpl null");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerLogBroadcast(Context mContext) {
        IntentFilter bbklogFilter = new IntentFilter();
        bbklogFilter.addAction(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED);
        mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.power.VivoPowerManagerServiceImpl.6
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                boolean status = "on".equals(intent.getStringExtra("adblog_status"));
                VSlog.w(VivoPowerManagerServiceImpl.TAG, "*****SWITCH LOG TO " + status);
                boolean unused = VivoPowerManagerServiceImpl.DEBUG = status;
                PowerManagerService unused2 = VivoPowerManagerServiceImpl.this.mPowerManagerService;
                PowerManagerService.DEBUG = status;
                PowerManagerService unused3 = VivoPowerManagerServiceImpl.this.mPowerManagerService;
                PowerManagerService.DEBUG_SPEW = status;
            }
        }, bbklogFilter, null, this.mHandler);
    }

    public void positiveSetOverrideState() {
        if (this.mMultiDisplayManagerInternal == null) {
            this.mMultiDisplayManagerInternal = (MultiDisplayManagerInternal) LocalServices.getService(MultiDisplayManagerInternal.class);
        }
        this.mMultiDisplayManagerInternal.onWakefulnessChanged(0);
    }

    public void negativeSetOverrideState() {
        if (this.mMultiDisplayManagerInternal == null) {
            this.mMultiDisplayManagerInternal = (MultiDisplayManagerInternal) LocalServices.getService(MultiDisplayManagerInternal.class);
        }
        this.mMultiDisplayManagerInternal.onWakefulnessChanged(1);
    }

    public void getSettingBrightness(ContentResolver resolver) {
        String offBy = Settings.System.getStringForUser(resolver, "screen_brightness_mode_off_by", this.mCurrentUser);
        String changeBy = Settings.System.getStringForUser(resolver, "screen_brightness_change_by", this.mCurrentUser);
        this.mScreenBrightnessSetting = getBrightnessFloat(resolver);
        if (offBy != null) {
            this.mBrightnessModeOffBy = offBy;
        }
        if (changeBy != null) {
            this.mSettingBrightnessChangeBy = changeBy;
        }
        if (DEBUG) {
            VSlog.d(TAG, "updateSettingsLocked: mScreenBrightnessSetting = " + this.mScreenBrightnessSetting + " mCurrentUser " + this.mCurrentUser);
        }
    }

    private float getBrightnessFloat(ContentResolver resolver) {
        float brightnessFloat = Settings.System.getFloatForUser(resolver, "screen_brightness_float", this.mPowerManagerService.mScreenBrightnessSettingDefault, this.mCurrentUser);
        if (this.isBoot && brightnessFloat == this.mPowerManagerService.mScreenBrightnessSettingDefault) {
            this.isBoot = false;
            int brightnessInt = Settings.System.getIntForUser(resolver, "screen_brightness", 102, -2);
            if (brightnessInt != 102) {
                float brightnessFloat2 = SensorConfig.algoBrightness2Float(SensorConfig.br255MapToalgo(brightnessInt));
                Settings.System.putFloatForUser(resolver, "screen_brightness_float", brightnessFloat2, this.mCurrentUser);
                VSlog.i(TAG, "float is default value, we use 255 setting brightness, user = -2");
                return brightnessFloat2;
            }
            return brightnessFloat;
        }
        return brightnessFloat;
    }

    public void checkUserSwitch() {
        int currentUser = ActivityManager.getCurrentUser();
        int i = this.mCurrentUser;
        if (i == -2) {
            this.mCurrentUser = currentUser;
            if (DEBUG) {
                VSlog.d(TAG, "checkUserSwitch  " + this.mCurrentUser + " -> " + currentUser);
            }
        } else if (i != currentUser) {
            VSlog.d(TAG, "checkUserSwitch  " + this.mCurrentUser + " -> " + currentUser);
            VivoSensorImpl vivoSensorImpl = this.mVivoSensorImpl;
            if (vivoSensorImpl != null) {
                vivoSensorImpl.notifySwitchUser(currentUser);
            }
            this.mCurrentUser = currentUser;
        } else if (DEBUG) {
            VSlog.d(TAG, "checkUserSwitch same " + this.mCurrentUser);
        }
    }

    public boolean getVivoAutoBrightnessState() {
        return this.tempAutoBrightness;
    }

    public float getScreenBrightness() {
        float tempBrightness;
        float f = this.mPowerManagerService.mScreenBrightnessSettingDefault;
        boolean z = true;
        if (this.mPowerManagerService.mScreenBrightnessModeSetting != 1 || isGameModeLockAutobrightness()) {
            z = false;
        }
        this.tempAutoBrightness = z;
        if (PowerManagerService.isValidBrightness(this.mPowerManagerService.mScreenBrightnessOverrideFromWindowManager)) {
            tempBrightness = this.mPowerManagerService.mScreenBrightnessOverrideFromWindowManager;
            this.tempAutoBrightness = false;
        } else {
            tempBrightness = this.mScreenBrightnessSetting;
        }
        if (isValidBrightnessFromPem(tempBrightness)) {
            VSlog.d(TAG, "limit screenBrightness to " + this.mScreenBrightnessOverrideFromPem);
            tempBrightness = this.mScreenBrightnessOverrideFromPem;
        }
        return Math.max(Math.min(tempBrightness, this.mPowerManagerService.mScreenBrightnessSettingMaximum), this.mPowerManagerService.mScreenBrightnessSettingMinimum);
    }

    private boolean isValidBrightnessFromPem(float screenBrightness) {
        return PowerManagerService.isValidBrightness(this.mScreenBrightnessOverrideFromPem) && screenBrightness >= this.mScreenBrightnessOverrideFromPem;
    }

    public boolean isGameModeLockAutobrightness() {
        GameModeLockAutobrightness gameModeLockAutobrightness = this.mGameModeLockAutobrightness;
        return gameModeLockAutobrightness != null && gameModeLockAutobrightness.isLockAutobrightness();
    }

    public void updateDisplayPowerRequest(int mScreenBrightnessModeSetting) {
        this.mPowerManagerService.mDisplayPowerRequest.settingBrightness = this.mScreenBrightnessSetting;
        this.mPowerManagerService.mDisplayPowerRequest.settingBrightnessChangeBy = this.mSettingBrightnessChangeBy;
        this.mPowerManagerService.mDisplayPowerRequest.settingScreenBrightnessMode = mScreenBrightnessModeSetting;
        this.mPowerManagerService.mDisplayPowerRequest.brightnessModeOffBy = this.mBrightnessModeOffBy;
        VivoSensorImpl vivoSensorImpl = this.mVivoSensorImpl;
        if (vivoSensorImpl != null) {
            this.mDisplayPowerRequest.antimisoperationTriggered = vivoSensorImpl.getAntiTrigger();
            this.mVivoSensorImpl.notifyChangeProximityParam(this.mDisplayPowerRequest.useProximitySensor);
        }
        if (this.mLightUpNow && this.mDisplayPowerRequest.callState != 1) {
            this.mLightUpNow = false;
        }
        this.mDisplayPowerRequest.lightUpNow = this.mLightUpNow;
    }

    public void setWakeLockWhiteUid(int uid) {
        this.mWhiteUidList.add(Integer.valueOf(uid));
        ArrayList<Integer> arrayList = this.mWhiteUidList;
        arrayList.add(Integer.valueOf(Integer.parseInt("999" + Integer.toString(uid))));
    }

    private FingerprintKeyguardInternal getFingerprintUnlockController() {
        if (this.mFingerprintUnlockController == null) {
            this.mFingerprintUnlockController = (FingerprintKeyguardInternal) LocalServices.getService(FingerprintKeyguardInternal.class);
        }
        return this.mFingerprintUnlockController;
    }

    /* loaded from: classes.dex */
    private class FingerprintWakeUpCallback implements FingerprintKeyguardInternal.WakeUpCallback {
        private FingerprintWakeUpCallback() {
        }

        public void prepareDisplay() {
            synchronized (VivoPowerManagerServiceImpl.this.mPowerManagerService.mLock) {
                if (VivoPowerManagerServiceImpl.this.mPowerManagerService.mDisplayManagerInternal != null) {
                    VSlog.d(VivoPowerManagerServiceImpl.TAG, "prepareDisplay.");
                    VivoPowerManagerServiceImpl.this.mPowerManagerService.mDisplayManagerInternal.prepareDisplay();
                } else {
                    VSlog.w(VivoPowerManagerServiceImpl.TAG, "prepareDisplay null");
                }
            }
        }

        public void setDisplayState(int state) {
            synchronized (VivoPowerManagerServiceImpl.this.mPowerManagerService.mLock) {
                if (VivoPowerManagerServiceImpl.this.mPowerManagerService.mDisplayManagerInternal != null) {
                    VSlog.d(VivoPowerManagerServiceImpl.TAG, "setDisplayState: " + state);
                    VivoPowerManagerServiceImpl.this.mPowerManagerService.mDisplayManagerInternal.setDisplayState(state);
                } else {
                    VSlog.w(VivoPowerManagerServiceImpl.TAG, "setDisplayState null");
                }
            }
        }

        public void updateDisplay() {
            synchronized (VivoPowerManagerServiceImpl.this.mPowerManagerService.mLock) {
                if (VivoPowerManagerServiceImpl.this.mPowerManagerService.mDisplayManagerInternal != null) {
                    VSlog.d(VivoPowerManagerServiceImpl.TAG, "updateDisplay");
                    VivoPowerManagerServiceImpl.this.mPowerManagerService.mDisplayManagerInternal.updateDisplay();
                } else {
                    VSlog.w(VivoPowerManagerServiceImpl.TAG, "updateDisplay null");
                }
            }
        }
    }

    public void handleSetBrightnessOff(boolean setOff) {
        if (setOff && isSmartMirrorFirstoff()) {
            VSlog.d(TAG, "handleSetBrightnessOff execute handleSmartMirrorFirstoff ");
            realHandleSmartMirrorFirstoff();
        }
        if (this.mEasyShareOn || !setOff || isSmartMirrorOn()) {
            VSlog.d(TAG, "handleSetBrightnessOff setOff = " + setOff);
            if (setOff) {
                this.mDisplayManager.setForceDisplayBrightnessOff(setOff, "user activity timeout");
            } else {
                this.mDisplayManager.setForceDisplayBrightnessOff(setOff, "wakeup");
            }
        }
        if (!setOff) {
            this.mPowerManagerService.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 0, 1000);
        }
    }

    public boolean getForceSetBrightnessOff() {
        if (this.displayManagerInternal == null) {
            this.displayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        }
        return this.displayManagerInternal.shouldWakeUpWhileInteractive();
    }

    private void handleEasyShareChanged() {
        boolean easyShareOn = Settings.System.getInt(this.mContext.getContentResolver(), "easy_share_force_brightness_off", 0) == 1;
        if (this.mEasyShareOn != easyShareOn) {
            this.mPowerManagerService.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 0, 1000);
            this.mEasyShareOn = easyShareOn;
            VSlog.d(TAG, "EasyShare status changed, mEasyShareOn = " + this.mEasyShareOn);
        }
    }

    public boolean getDesiredScreenPolicyLocked() {
        return ("vos".equals(FtBuild.getOsName()) || (this.mPowerManagerService.mUserActivitySummary & 4) == 0 || !this.isKeyguarActive) ? false : true;
    }

    public void notifyAppSharePackageChanged(String packageName, int userId) {
        synchronized (this.mPowerManagerService.mLock) {
            this.mAppSharePackageName = packageName;
            this.mAppShareUserId = userId;
        }
    }

    public boolean shouldIgnoreWakeLock(PowerManagerService.WakeLock wakeLock) {
        if (AppShareConfig.SUPPROT_APPSHARE && this.mAppSharePackageName != null && this.mAppShareUserId != -1 && PowerManagerService.isScreenLock(wakeLock)) {
            if (DEBUG) {
                VSlog.d(TAG, "shouldIgnoreWakeLock: wakeLock = " + wakeLock + ", mAppSharePackageName = " + this.mAppSharePackageName + ", mAppShareUserId = " + this.mAppShareUserId);
            }
            if (this.mAppSharePackageName.equals(wakeLock.mPackageName) && this.mAppShareUserId == UserHandle.getUserId(wakeLock.mOwnerUid)) {
                return true;
            }
            if (VivoPermissionUtils.OS_PKG.equals(wakeLock.mPackageName) && "WindowManager_AppShareDisplay".equals(wakeLock.mTag) && wakeLock.mWorkSource != null && wakeLock.mWorkSource.size() == 1) {
                int uid = wakeLock.mWorkSource.get(0);
                PackageManager packageManager = this.mContext.getPackageManager();
                String[] packages = packageManager.getPackagesForUid(uid);
                if (DEBUG && packages != null) {
                    for (String pkg : packages) {
                        VSlog.d(TAG, "shouldIgnoreWakeLock: uid = " + uid + ", pkg = " + pkg);
                    }
                }
                if (packages != null && packages.length > 0 && this.mAppSharePackageName.equals(packages[0]) && this.mAppShareUserId == UserHandle.getUserId(uid)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public void resetAppShareStateIfNeededLocked() {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            this.mLastAppShareDisplayHasScreenOnWakeLock = this.mAppShareDisplayHasScreenOnWakeLock;
            this.mAppShareDisplayHasScreenOnWakeLock = false;
        }
    }

    public void setAppShareDisplayHasScreenOnWakeLock(boolean hasScreenOnWakeLock) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            this.mAppShareDisplayHasScreenOnWakeLock = hasScreenOnWakeLock;
        }
    }

    public void notifyAppShareKeepScreenOnIfNeededLocked() {
        if (AppShareConfig.SUPPROT_APPSHARE && this.mLastAppShareDisplayHasScreenOnWakeLock != this.mAppShareDisplayHasScreenOnWakeLock) {
            VSlog.i(TAG, "notifyAppShareKeepScreenOnIfNeededLocked, mAppShareDisplayHasScreenOnWakeLock = " + this.mAppShareDisplayHasScreenOnWakeLock);
            this.mVivoAppShareManager.notifyKeepScreenOnFromPowerManager(this.mAppShareDisplayHasScreenOnWakeLock);
        }
    }

    private void getGameFrameRateMode() {
        int gameFrameRateMode = 0;
        boolean gameCrack = 90 == Settings.System.getIntForUser(this.mContext.getContentResolver(), "gamewatch_game_target_fps", -1, this.mCurrentUser);
        boolean gameMode = 1 == Settings.System.getIntForUser(this.mContext.getContentResolver(), "is_game_mode", -1, this.mCurrentUser);
        boolean dcDimMode = 1 == Settings.System.getIntForUser(this.mContext.getContentResolver(), "vivo_dc_dimming_enabled", -1, this.mCurrentUser);
        if (gameCrack && gameMode && dcDimMode) {
            gameFrameRateMode = 1;
        }
        if (this.mDisplayPowerRequest.gameFrameRateMode != gameFrameRateMode) {
            this.mDisplayPowerRequest.gameFrameRateMode = gameFrameRateMode;
            this.mPowerManagerService.mDirty |= 32;
            this.mPowerManagerService.updatePowerStateLocked();
        }
    }

    public void setValueForPowerReport(long eventTime, int reason) {
        if (this.mPowerDataReport == null) {
            this.mPowerDataReport = PowerDataReport.getInstance();
        }
        if (this.mPowerDataReport.getReasonOfScreenOn() != null) {
            return;
        }
        if (reason == 11) {
            this.mPowerDataReport.setStartTimeOfScrenOn(eventTime);
            this.mPowerDataReport.setReasonOfScreenOn("DoubleTap");
        } else if (reason == 15) {
            this.mPowerDataReport.setStartTimeOfScrenOn(eventTime);
            this.mPowerDataReport.setReasonOfScreenOn("PowerKey");
        }
    }
}