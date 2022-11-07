package com.android.server.display;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.hardware.biometrics.fingerprint.FingerprintKeyguardInternal;
import android.hardware.biometrics.fingerprint.FingerprintUIManagerInternal;
import android.hardware.display.DisplayManagerInternal;
import android.os.BatteryManagerInternal;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManagerInternal;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.FtFeature;
import android.view.Display;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import com.android.server.LocalServices;
import com.android.server.display.VivoDisplayModuleController;
import com.android.server.display.color.VivoLightColorMatrixControl;
import com.android.server.lights.LightsManager;
import com.android.server.lights.LogicalLight;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.android.server.wm.SnapshotWindow;
import com.android.server.wm.WindowManagerInternal;
import com.vivo.fingerprint.FingerprintConfig;
import com.vivo.fingerprint.analysis.AnalysisManager;
import com.vivo.sensor.autobrightness.AutoBrightnessManagerImpl;
import com.vivo.sensor.autobrightness.LightCoverPointerEventListener;
import com.vivo.sensor.autobrightness.PowerAssistant;
import com.vivo.sensor.autobrightness.callback.AutobrightInfo;
import com.vivo.sensor.autobrightness.callback.CameraLumaCallback;
import com.vivo.sensor.autobrightness.policy.AutoBrightnessQuickSet;
import com.vivo.sensor.autobrightness.utils.AutoBrightnessAlgoLcmBrightnessMap;
import com.vivo.sensor.implement.SensorConfig;
import com.vivo.sensor.implement.VivoSensorImpl;
import com.vivo.services.security.server.VivoPermissionUtils;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoDisplayPowerControllerImpl implements IVivoDisplayPowerController {
    private static final int BRIGHTNESS_RAMP_RATE_FAST = 200;
    private static final int BRIGHTNESS_RAMP_RATE_PEM = 500;
    private static final int BRIGHTNESS_RAMP_RATE_SLOW = 40;
    private static final int CALL_STATE_IDLE = 0;
    public static final int COLOR_FADE_ANIMATION_DEFAULT = -2;
    public static final int COLOR_FADE_ANIMATION_GLOBAL = 4000;
    public static final int COLOR_FADE_ANIMATION_HORIZONTAL = 4001;
    public static final int COLOR_FADE_ANIMATION_NONE = -1;
    public static final int COLOR_FADE_ANIMATION_VERTICAL = 4002;
    private static boolean DEBUG = false;
    private static boolean DEBUG_LIGHT_SENSOR = false;
    private static final int DIM_RAMPER_RATE = 600;
    public static final int DYNAMIC_EFFECTS_DEFAULT = 1;
    private static final boolean IS_ENG;
    private static final boolean IS_LOG_CTRL_OPEN;
    private static final String KEY_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    private static final String POSITIVE_DEBOUNCE_TIME = "persist.sys.debounce_time";
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private static final int PROXIMITY_SENSOR_NEGATIVE_DEBOUNCE_DELAY = 0;
    private static final int PROXIMITY_SENSOR_POSITIVE_DEBOUNCE_DELAY = 0;
    private static final int PROXIMITY_UNKNOWN = -1;
    private static final String TAG = "DisplayPowerControllerImpl";
    private static final int TYPE_NOTIFY_BRIGHTNESS = 0;
    private static final int TYPE_NOTIFY_ORIENTATION = 1;
    private static int oldHallState;
    private static int oldPolicy;
    private Handler mAnimateBrightnessHandler;
    private AutoBrightnessManagerImpl mAutoBrightnessManager;
    private final LogicalLight mBacklight;
    private BatteryManagerInternal mBatteryManagerInternal;
    private DisplayManagerInternal.DisplayPowerCallbacks mCallbacks;
    private Handler mColorFadeHandler;
    private HandlerThread mColorFadeThread;
    private final Context mContext;
    private DisplayManagerInternal mDisplayManagerInternal;
    private DisplayPowerController mDisplayPowerController;
    private FingerprintKeyguardInternal mFingerprintKeyguard;
    private FingerprintUIManagerInternal mFingerprintUIManager;
    private Handler mHandler;
    private boolean mIsKeyguardHide;
    private boolean mIsPowered;
    private boolean mKeyguardOccluded;
    private VivoLightColorMatrixControl mLigntColorMatrixControl;
    private boolean mNeedChangeStateBecauseProximity;
    private PowerAssistant mPowerAssistant;
    private PowerManagerInternal mPowerManagerInternal;
    private DisplayManagerInternal.DisplayPowerRequest mPowerRequest;
    private DisplayPowerState mPowerState;
    private SensorManager mSensorManager;
    private HandlerThread mSetProxCaliThread;
    private SnapshotWindow mSnapshotWin;
    private boolean mSupportRtblur;
    private Handler mVivoDebounceHandler;
    private HandlerThread mVivoDebounceThread;
    private VivoSensorImpl mVivoSensorImpl;
    private WindowManagerInternal mWindowManagerInternal;
    private WindowManagerPolicy mWindowManagerPolicy;
    private float mAnimatedBrightness = Float.NaN;
    private int mLastColorFadeStyle = -2;
    private boolean mLastDynamicEffectsOn = true;
    private Handler mSetPsCaliHandler = null;
    private AutobrightInfo mAutobrightInfo = new AutobrightInfo();
    private int[] mTestArg = new int[3];
    private boolean bIsProximitySensorTag = false;
    private boolean mScreenBrightnessMode = false;
    private boolean isPowerKeyWakeUp = false;
    private boolean isVivoWMPHookInit = false;
    private boolean mAnimateToDirectly = false;
    private boolean mLightSensorEnabled = false;
    private boolean mUseAlsBeforeScreenOff = true;
    private boolean mBeingAckSettingChanging = false;
    private boolean mWaitingApplyFirstLight = false;
    private boolean mManualLightSensorEnabled = false;
    private boolean mWatingFirstLightSensorValue = false;
    private boolean mAnimateBrightnessTaskStarted = false;
    private boolean mBeingAckBrightnessModeChanging = false;
    private boolean mBrightnessModeWhenPowerSaveEnable = false;
    private boolean mBeingAckSettingScreenBrightnessChange = false;
    private boolean mBrightnessSettingChangedWhenPowerSaveEnable = false;
    private boolean oldUseAutoBrightness = false;
    private boolean dimStateChanged = false;
    private boolean policyChanged = false;
    private boolean mBlockScreenFlag = false;
    private boolean mIsNeedQuickSetBrightness = false;
    private int mPowerSaveType = 0;
    private String mLastUdPackage = "unknown";
    private float mVivoWaitingBrightness = -1.0f;
    private float mScreenAutoBrightness = -1.0f;
    private float mLastScreenAutoBrightness = -1.0f;
    private float mSettingBrightness = -1.0f;
    private int mPositiveDelaytime = 0;
    private long mProximityPositiveTime = -1;
    private AutoBrightnessQuickSet mQuickSetBrightness = null;
    private float mLastScreenBrightnessOverrideFromPem = -1.0f;
    private String mBlurSwitchName = "enhanced_dynamic_effects";
    private boolean colorFadeOffFlag = false;
    private Runnable mVivoGetFtColorManagerTask = new Runnable() { // from class: com.android.server.display.VivoDisplayPowerControllerImpl.4
        @Override // java.lang.Runnable
        public void run() {
        }
    };
    public CameraLumaCallback.AutoBrightnessCallback mAutoBrightnessCallback = new CameraLumaCallback.AutoBrightnessCallback() { // from class: com.android.server.display.VivoDisplayPowerControllerImpl.5
        public void onNewScreenValue(AutobrightInfo info) {
            if (VivoDisplayPowerControllerImpl.DEBUG_LIGHT_SENSOR) {
                VSlog.d(VivoDisplayPowerControllerImpl.TAG, "onNewScreenValue brightness:" + info.mBrightnessFloat + " screenLevel=" + info.mScreenLevel + " delay=" + info.mDelayTime + " mAutobrightInfo.mBrightnessMultilevel=" + VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mBrightnessFloat + " mAutobrightInfo.mDelayTime=" + VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mDelayTime);
            }
            if (info != null) {
                if (!VivoDisplayPowerControllerImpl.this.mAutobrightInfo.compare(info) || VivoDisplayPowerControllerImpl.this.mWatingFirstLightSensorValue) {
                    VSlog.d(VivoDisplayPowerControllerImpl.TAG, "mVivoWaitingBrightness brightness change from " + VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mBrightnessFloat + " to " + info.mBrightnessFloat + "; delay  =" + info.mDelayTime + "; DriverMode = " + VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mDriveMode);
                    VivoDisplayPowerControllerImpl.this.mWatingFirstLightSensorValue = false;
                    VivoDisplayPowerControllerImpl.this.mAutobrightInfo.copyFrom(info);
                    if (VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mDriveMode) {
                        int minAlgoBrightnessInDriveMode = 10;
                        if (SensorConfig.isFloatBright()) {
                            minAlgoBrightnessInDriveMode = AutoBrightnessAlgoLcmBrightnessMap.algo255MapTo16383(10);
                        }
                        VivoDisplayPowerControllerImpl.this.mVivoWaitingBrightness = Math.max(AutoBrightnessAlgoLcmBrightnessMap.algoBrightness2Float(minAlgoBrightnessInDriveMode), VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mBrightnessFloat);
                    } else {
                        VivoDisplayPowerControllerImpl vivoDisplayPowerControllerImpl = VivoDisplayPowerControllerImpl.this;
                        vivoDisplayPowerControllerImpl.mVivoWaitingBrightness = vivoDisplayPowerControllerImpl.mAutobrightInfo.mBrightnessFloat;
                    }
                    VivoDisplayPowerControllerImpl.this.mVivoDebounceHandler.removeCallbacks(VivoDisplayPowerControllerImpl.this.mVivoAutoBrightnessTask);
                    if (VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mDelayTime == 0) {
                        VivoDisplayPowerControllerImpl.this.mWaitingApplyFirstLight = true;
                        VivoDisplayPowerControllerImpl.this.mBeingAckBrightnessModeChanging = true;
                        VivoDisplayPowerControllerImpl.this.mVivoDebounceHandler.post(VivoDisplayPowerControllerImpl.this.mVivoAutoBrightnessTask);
                        return;
                    } else if (info.mIsNeedQuickSetBrightness) {
                        VivoDisplayPowerControllerImpl.this.mIsNeedQuickSetBrightness = true;
                        VivoDisplayPowerControllerImpl.this.mVivoDebounceHandler.post(VivoDisplayPowerControllerImpl.this.mVivoAutoBrightnessTask);
                        return;
                    } else {
                        VivoDisplayPowerControllerImpl.this.mVivoDebounceHandler.postDelayed(VivoDisplayPowerControllerImpl.this.mVivoAutoBrightnessTask, VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mDelayTime);
                        return;
                    }
                } else if (SensorConfig.isUnderLight()) {
                    VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mRecitfiedLuxLock = info.mRecitfiedLuxLock;
                    VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mDriverLuxLock = info.mDriverLuxLock;
                    VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mChangeDownLux = info.mChangeDownLux;
                    VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mChangeUpLux = info.mChangeUpLux;
                    VivoDisplayPowerControllerImpl.this.mAutobrightInfo.mUnderDisplayThreshChanged = info.mUnderDisplayThreshChanged;
                    return;
                } else {
                    return;
                }
            }
            VSlog.d(VivoDisplayPowerControllerImpl.TAG, "onNewScreenValue info is null,return.");
        }

        public int getCurrentAutoBrightness() {
            return (int) (VivoDisplayPowerControllerImpl.this.mScreenAutoBrightness * 255.0f);
        }

        public void onNeedCancelBrightness(int reason) {
            VSlog.d(VivoDisplayPowerControllerImpl.TAG, "onNeedCancelBrightness reason=" + reason);
            VivoDisplayPowerControllerImpl.this.mVivoDebounceHandler.removeCallbacks(VivoDisplayPowerControllerImpl.this.mVivoAutoBrightnessTask);
        }

        public float getFinalLcmLevel(float brightness) {
            float brightAdjust = VivoDisplayPowerControllerImpl.this.mPowerState.getLcmFinalLevel(SensorConfig.lcmBrightness2FloatAfterDPC(SensorConfig.float2LcmBrightness(brightness)));
            VSlog.d(VivoDisplayPowerControllerImpl.TAG, "getFinalLcmLevel " + brightness + " --> " + brightAdjust);
            return brightAdjust;
        }

        public void notifyFingerprintUiBrightness(float brightness) {
            if (VivoDisplayPowerControllerImpl.this.mFingerprintUIManager != null) {
                float finalBrightness = VivoDisplayPowerControllerImpl.this.mPowerState.getLcmFinalLevel(brightness);
                VSlog.d(VivoDisplayPowerControllerImpl.TAG, "onAutoBrightness brightness=" + brightness + " ,finalBrightness=" + finalBrightness + ", " + VivoDisplayPowerControllerImpl.this.mPowerRequest.useAutoBrightness);
                VivoDisplayPowerControllerImpl.this.mFingerprintUIManager.onAutoBrightness(finalBrightness, VivoDisplayPowerControllerImpl.this.mPowerRequest.useAutoBrightness);
                if (VivoDisplayPowerControllerImpl.this.mLigntColorMatrixControl == null) {
                    VivoDisplayPowerControllerImpl.this.mLigntColorMatrixControl = VivoLightColorMatrixControl.getExistInstance();
                }
                if (VivoDisplayPowerControllerImpl.this.mLigntColorMatrixControl != null) {
                    VivoDisplayPowerControllerImpl.this.mLigntColorMatrixControl.onAutoBrightness(brightness, VivoDisplayPowerControllerImpl.this.mPowerRequest.useAutoBrightness);
                }
            }
            if (VivoDisplayPowerControllerImpl.this.mPowerState != null) {
                VivoDisplayPowerControllerImpl.this.mPowerState.mSensorScreenBrightness = brightness;
                if (VivoDisplayPowerControllerImpl.DEBUG) {
                    VSlog.i(VivoDisplayPowerControllerImpl.TAG, "onAutoBrightness sensor brightness changed to " + brightness);
                    return;
                }
                return;
            }
            VSlog.w(VivoDisplayPowerControllerImpl.TAG, "sensor brightness changed, send failed");
        }
    };
    public CameraLumaCallback.PowerAssistantCallback mPowerAssistantCallback = new CameraLumaCallback.PowerAssistantCallback() { // from class: com.android.server.display.VivoDisplayPowerControllerImpl.6
        public void onPowerSaveTypeChanged(int type) {
            VivoDisplayPowerControllerImpl.this.mPowerSaveType = type;
            if (VivoDisplayPowerControllerImpl.this.mAutoBrightnessManager != null && (VivoDisplayPowerControllerImpl.this.mPowerSaveType == 0 || VivoDisplayPowerControllerImpl.this.mPowerSaveType == 1)) {
                VivoDisplayPowerControllerImpl.this.mAutoBrightnessManager.notifyPowerAssistantMode(VivoDisplayPowerControllerImpl.this.mPowerSaveType == 1);
            }
            if (VivoDisplayPowerControllerImpl.this.mPowerState != null) {
                VivoDisplayPowerControllerImpl.this.mPowerState.onPowerAssistantModeChanged(VivoDisplayPowerControllerImpl.this.mPowerSaveType);
                if (VivoDisplayPowerControllerImpl.this.mPowerSaveType != 1 && VivoDisplayPowerControllerImpl.this.mPowerSaveType != 2 && VivoDisplayPowerControllerImpl.this.mPowerSaveType != 3) {
                    if (!VivoDisplayPowerControllerImpl.this.mBrightnessModeWhenPowerSaveEnable || VivoDisplayPowerControllerImpl.this.mPowerSaveType != 0) {
                        if (VivoDisplayPowerControllerImpl.this.mBrightnessModeWhenPowerSaveEnable || VivoDisplayPowerControllerImpl.this.mPowerSaveType != 0) {
                            if (VivoDisplayPowerControllerImpl.this.mBrightnessSettingChangedWhenPowerSaveEnable && VivoDisplayPowerControllerImpl.this.mPowerSaveType == 0) {
                                VivoDisplayPowerControllerImpl.this.mBrightnessSettingChangedWhenPowerSaveEnable = false;
                                VivoDisplayPowerControllerImpl.this.mPowerState.onPowerAssistantModeChangedNotify();
                                return;
                            }
                            return;
                        }
                        VivoDisplayPowerControllerImpl.this.mPowerState.onPowerAssistantModeChangedNotify();
                        return;
                    }
                    VivoDisplayPowerControllerImpl.this.mPowerState.onPowerAssistantModeChangedNotify();
                    return;
                }
                VivoDisplayPowerControllerImpl vivoDisplayPowerControllerImpl = VivoDisplayPowerControllerImpl.this;
                vivoDisplayPowerControllerImpl.mBrightnessModeWhenPowerSaveEnable = vivoDisplayPowerControllerImpl.mScreenBrightnessMode;
                VivoDisplayPowerControllerImpl.this.mPowerState.onPowerAssistantModeChangedNotify();
                return;
            }
            VSlog.d(VivoDisplayPowerControllerImpl.TAG, "onPowerSaveTypeChanged mPowerState is null.");
        }

        public void onUnderLightBrightnessChange(int brightness) {
            if (VivoDisplayPowerControllerImpl.DEBUG) {
                VSlog.d(VivoDisplayPowerControllerImpl.TAG, "onUnderLightBrightnessChange  brightness=" + brightness);
            }
            float tempbrightness = AutoBrightnessAlgoLcmBrightnessMap.lcmBrightness2FloatAfterDPC(brightness);
            VivoDisplayPowerControllerImpl.this.mPowerState.setScreenBrightness(tempbrightness);
        }

        public void onPemBrightnessScale(float scale) {
            if (VivoDisplayPowerControllerImpl.DEBUG) {
                VSlog.d(VivoDisplayPowerControllerImpl.TAG, "onPemBrightnessScale  scale=" + scale);
            }
            if (scale < 0.0f) {
                scale = 1.0f;
            }
            VivoDisplayPowerControllerImpl.this.mDisplayPowerController.mScreenBrightnessRampAnimator.setPemBrightnessScale(scale);
        }
    };
    public CameraLumaCallback.UnderDisplayLightCallback mUDLightCallback = new CameraLumaCallback.UnderDisplayLightCallback() { // from class: com.android.server.display.VivoDisplayPowerControllerImpl.7
        public void onAppChanged(String pkg, boolean isPortrait) {
            if (VivoDisplayPowerControllerImpl.DEBUG_LIGHT_SENSOR) {
                VSlog.d(VivoDisplayPowerControllerImpl.TAG, "onAppChanged in dpc, pkg = " + pkg + ", isPortrait = " + isPortrait);
            }
            VSlog.d(VivoDisplayPowerControllerImpl.TAG, "onAC iPo " + isPortrait);
            VivoDisplayPowerControllerImpl.this.notifyToSensorService(1);
            VivoDisplayPowerControllerImpl.this.isSettingPackage(pkg);
            if (!isPortrait && !VivoDisplayPowerControllerImpl.this.isVivoWMPHookInit) {
                VivoDisplayPowerControllerImpl.this.isVivoWMPHookInit = true;
                if (VivoDisplayPowerControllerImpl.this.mWindowManagerPolicy != null && VivoDisplayPowerControllerImpl.this.mContext != null) {
                    LightCoverPointerEventListener lcListener = new LightCoverPointerEventListener(VivoDisplayPowerControllerImpl.this.mContext, VivoDisplayPowerControllerImpl.this.mLightCoverDetectCallback);
                    VivoDisplayPowerControllerImpl.this.mWindowManagerPolicy.registerPointerEventListener(lcListener, 0);
                    return;
                }
                VSlog.d(VivoDisplayPowerControllerImpl.TAG, "falied to register pe listener");
            }
        }
    };
    private Runnable mAnimateBrightnessTask = new Runnable() { // from class: com.android.server.display.VivoDisplayPowerControllerImpl.8
        @Override // java.lang.Runnable
        public void run() {
            if (VivoDisplayPowerControllerImpl.this.mUseAlsBeforeScreenOff) {
                VivoDisplayPowerControllerImpl.this.mDisplayPowerController.animateScreenBrightness(VivoDisplayPowerControllerImpl.this.mDisplayPowerController.clampScreenBrightness(VivoDisplayPowerControllerImpl.this.mScreenAutoBrightness), 0.0f);
            } else {
                VivoDisplayPowerControllerImpl.this.mDisplayPowerController.animateScreenBrightness(VivoDisplayPowerControllerImpl.this.mDisplayPowerController.clampScreenBrightness(VivoDisplayPowerControllerImpl.this.mScreenAutoBrightness), VivoDisplayPowerControllerImpl.this.mScreenAutoBrightness > VivoDisplayPowerControllerImpl.this.mSettingBrightness ? 200.0f : 40.0f);
            }
            VivoDisplayPowerControllerImpl.this.mWatingFirstLightSensorValue = false;
            VivoDisplayPowerControllerImpl.this.mWaitingApplyFirstLight = false;
            VivoDisplayPowerControllerImpl.this.mAnimateBrightnessTaskStarted = false;
        }
    };
    private Runnable mVivoAutoBrightnessTask = new Runnable() { // from class: com.android.server.display.VivoDisplayPowerControllerImpl.9
        @Override // java.lang.Runnable
        public void run() {
            VivoDisplayPowerControllerImpl.this.vivoUpdateAutoBrightness();
        }
    };
    private CameraLumaCallback.LightCoverDetectCallback mLightCoverDetectCallback = new CameraLumaCallback.LightCoverDetectCallback() { // from class: com.android.server.display.VivoDisplayPowerControllerImpl.10
        public void onLightCoverChanged(boolean isCover) {
            if (VivoDisplayPowerControllerImpl.this.mAutoBrightnessManager != null) {
                VivoDisplayPowerControllerImpl.this.mAutoBrightnessManager.onLightCoverChanged(isCover);
            }
        }
    };

    static {
        boolean z = false;
        IS_ENG = Build.TYPE.equals("eng") || Build.TYPE.equals("branddebug");
        IS_LOG_CTRL_OPEN = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
        DEBUG_LIGHT_SENSOR = SensorConfig.isDebug();
        if (IS_LOG_CTRL_OPEN || IS_ENG) {
            z = true;
        }
        DEBUG = z;
        oldPolicy = -1;
    }

    public VivoDisplayPowerControllerImpl(SensorManager sensorManager, DisplayPowerController displaypowercontroller, Context context) {
        this.mLigntColorMatrixControl = null;
        this.mContext = context;
        this.mDisplayPowerController = displaypowercontroller;
        checkFingerprintUI();
        checkFingerprintKeyguard();
        LightsManager lights = (LightsManager) LocalServices.getService(LightsManager.class);
        if (lights != null) {
            this.mBacklight = lights.getLight(0);
        } else {
            this.mBacklight = null;
        }
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mLigntColorMatrixControl = VivoLightColorMatrixControl.getExistInstance();
        HandlerThread handlerThread = new HandlerThread("colorFade");
        this.mColorFadeThread = handlerThread;
        handlerThread.start();
        this.mColorFadeHandler = new Handler(this.mColorFadeThread.getLooper());
        this.mSupportRtblur = FtFeature.isFeatureSupport("vivo.software.rtblur");
        this.mBatteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
        this.mWindowManagerPolicy = this.mDisplayPowerController.mWindowManagerPolicy;
        this.mVivoSensorImpl = VivoSensorImpl.getInstance(context);
    }

    public void setStateAndRequest(DisplayPowerState PowerState, DisplayManagerInternal.DisplayPowerRequest PowerRequest) {
        this.mPowerState = PowerState;
        this.mPowerRequest = PowerRequest;
    }

    private FingerprintUIManagerInternal checkFingerprintUI() {
        if (this.mFingerprintUIManager == null) {
            this.mFingerprintUIManager = (FingerprintUIManagerInternal) LocalServices.getService(FingerprintUIManagerInternal.class);
        }
        return this.mFingerprintUIManager;
    }

    private FingerprintKeyguardInternal checkFingerprintKeyguard() {
        if (this.mFingerprintKeyguard == null) {
            this.mFingerprintKeyguard = (FingerprintKeyguardInternal) LocalServices.getService(FingerprintKeyguardInternal.class);
        }
        return this.mFingerprintKeyguard;
    }

    public void updateAnimatedBrightness(float target, float rate) {
        this.mAnimatedBrightness = target;
    }

    @Deprecated
    public void notifyFpWakeHookPowerState() {
        FingerprintKeyguardInternal fk;
        if (this.mPowerRequest.policy == 0 && (fk = checkFingerprintKeyguard()) != null) {
            fk.onFingerprintWakeUpFinished();
        }
    }

    public void onAnimateScreenBrightness(float target, float rate) {
        FingerprintKeyguardInternal fk;
        if (this.mAnimatedBrightness == -1.0f && target > 0.0f && (fk = checkFingerprintKeyguard()) != null) {
            fk.onFingerprintWakeUpFinished();
        }
    }

    public boolean shouldSetScreenState(int state) {
        FingerprintKeyguardInternal fk = checkFingerprintKeyguard();
        return fk == null || state != 1 || !fk.isNeedBlockBrightness() || fk.isFingerprintWakingUp();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void closeColorFade() {
        if (this.mDisplayPowerController.mColorFadeOnAnimator.isStarted()) {
            this.mDisplayPowerController.mColorFadeOnAnimator.end();
        }
        DisplayPowerState displayPowerState = this.mPowerState;
        if (displayPowerState != null && displayPowerState.getColorFadeLevel() != 1.0f) {
            VSlog.d(TAG, "Fingerprint wake up remove colorFade");
            this.mPowerState.setColorFadeLevel(1.0f);
            this.mPowerState.dismissColorFade();
        }
    }

    public void dismissColorFadeIfNeed() {
        FingerprintKeyguardInternal fk = checkFingerprintKeyguard();
        if (this.mSnapshotWin == null) {
            this.mSnapshotWin = SnapshotWindow.getInstance(this.mContext);
        }
        if (fk != null && fk.isFingerprintWakingUp()) {
            SnapshotWindow snapshotWindow = this.mSnapshotWin;
            if (snapshotWindow == null || !snapshotWindow.isGoogleUnlock()) {
                closeColorFade();
            }
        }
    }

    public void prepareDisplay() {
        if (!this.mHandler.getLooper().isCurrentThread()) {
            VSlog.w(TAG, "prepareDisplay must be called in power manager thread.");
            return;
        }
        VSlog.i(TAG, "prepareDisplay");
        setAutoBrightnessEnable(true);
    }

    public void updateDisplay() {
        if (!this.mHandler.getLooper().isCurrentThread()) {
            VSlog.d(TAG, "updateDisplay must called in power manager thread.");
            return;
        }
        VSlog.i(TAG, "updateDisplay");
        if (this.mPowerState.getScreenState() != 2) {
            this.mPowerState.setScreenState(2);
            VSlog.i(TAG, "updateDisplay set STATE_ON");
        }
        this.mPowerState.restoreScreenBrightness();
    }

    public void unlockFastSetBrightness() {
        VSlog.d(TAG, "first setBrightness ...");
        if (this.mBacklight != null) {
            this.mPowerState.fingerprintUnlockSetBlockColorFade();
            this.mDisplayPowerController.sendUpdatePowerState();
            final float firstBrightness = this.mPowerState.mSensorScreenBrightness;
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayPowerControllerImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    if (VivoDisplayPowerControllerImpl.this.mPowerState.mSensorScreenBrightness > 0.0f) {
                        AnalysisManager.trace("colorFade");
                        VivoDisplayPowerControllerImpl.this.closeColorFade();
                        VSlog.d(VivoDisplayPowerControllerImpl.TAG, "first setBrightness " + firstBrightness);
                        AnalysisManager.trace(VivoDisplayModuleController.VivoDisplayModuleConfig.STR_MODULE_LIGHT);
                        VivoDisplayPowerControllerImpl.this.mBacklight.setBrightness(VivoDisplayPowerControllerImpl.this.mPowerState.mSensorScreenBrightness);
                        AnalysisManager.trace(VivoDisplayModuleController.VivoDisplayModuleConfig.STR_MODULE_LIGHT);
                        VSlog.d(VivoDisplayPowerControllerImpl.TAG, "first setBrightness end " + VivoDisplayPowerControllerImpl.this.mPowerState.mSensorScreenBrightness);
                        if (!FingerprintConfig.isOpticalFingerprint()) {
                            AnalysisManager.trace("offSpeedExit");
                        }
                    }
                }
            });
        }
    }

    public void setBrightnessForFingerprintCalibration(float brightness) {
        VSlog.d(TAG, "setBrightnessForFingerprintCalibration ...");
        LogicalLight logicalLight = this.mBacklight;
        if (logicalLight != null) {
            logicalLight.setBrightnessForFingerprintCalibration(brightness);
        }
    }

    public void setFingerprintCalibrationState(boolean isCalibrating) {
        VSlog.d(TAG, "setFingerprintCalibrationState ...");
        LogicalLight logicalLight = this.mBacklight;
        if (logicalLight != null) {
            logicalLight.setFingerprintCalibrationState(isCalibrating);
        }
    }

    public void setScreenStateWithoutNotify(final int state) {
        if (!this.mHandler.getLooper().isCurrentThread()) {
            VSlog.d(TAG, "setScreenStateWithoutNotify must called in power manager thread.");
            return;
        }
        this.mPowerState.setScreenStateFingerprint(state);
        if (this.mHandler.getLooper().isCurrentThread()) {
            setAutoBrightnessEnable(state == 2);
        } else {
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayPowerControllerImpl.2
                @Override // java.lang.Runnable
                public void run() {
                    VivoDisplayPowerControllerImpl.this.setAutoBrightnessEnable(state == 2);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAutoBrightnessEnable(boolean enable) {
        if (this.mAutoBrightnessManager == null) {
            VSlog.d(TAG, "no AutoBrightnessManager");
            return;
        }
        int currentState = this.mPowerState.getScreenState();
        if (currentState == 2) {
            VSlog.w(TAG, "current state isn't off.");
            return;
        }
        float currentBrightness = this.mPowerState.getScreenBrightness();
        if (currentBrightness > 0.0f) {
            VSlog.d(TAG, "current brightness is light");
            return;
        }
        DisplayManagerInternal.DisplayPowerRequest displayPowerRequest = this.mPowerRequest;
        if (displayPowerRequest == null || !displayPowerRequest.useAutoBrightness) {
            VSlog.d(TAG, "auto brightness is disabled!");
            return;
        }
        VSlog.d(TAG, "setAutoBrightnessEnable: " + enable);
        if (enable) {
            this.mAutoBrightnessManager.notifyStateChanged(10001);
        } else {
            this.mAutoBrightnessManager.notifyStateChanged(10005);
        }
    }

    public void faceBlockScreenOn(boolean block) {
        this.mPowerState.faceBlockScreenOn(block);
    }

    public void onKeyguardLockChanged() {
        if (this.mWindowManagerInternal == null) {
            this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        }
        WindowManagerInternal windowManagerInternal = this.mWindowManagerInternal;
        if (windowManagerInternal != null && !windowManagerInternal.isKeyguardShowingAndNotOccluded()) {
            this.mIsKeyguardHide = true;
            handleSettingsChange(true);
        }
    }

    public void setOccluded(boolean occluded) {
        if (this.mKeyguardOccluded != occluded) {
            this.mKeyguardOccluded = occluded;
            handleSettingsChange(true);
        }
    }

    public void setKeyguardHide(boolean keyguardHide) {
        if (this.mIsKeyguardHide != keyguardHide) {
            this.mIsKeyguardHide = keyguardHide;
            handleSettingsChange(true);
            DisplayPowerController displayPowerController = this.mDisplayPowerController;
            if (displayPowerController != null && displayPowerController.mPowerState != null && this.mDisplayPowerController.mPowerState.mColorFade != null) {
                this.mDisplayPowerController.mPowerState.mColorFade.setKeyguardHide(keyguardHide);
            }
        }
    }

    public void initialize() {
        initPowerSaveType();
        if (isSupportRealtimeBlurSwitch(this.mContext)) {
            this.mBlurSwitchName = "realtime_blur_state";
        }
        handleSettingsChange(true);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("global_animation_color_fade_style"), false, this.mDisplayPowerController.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(this.mBlurSwitchName), false, this.mDisplayPowerController.mSettingsObserver, -1);
    }

    private void initPowerSaveType() {
        PowerAssistant powerAssistant = this.mPowerAssistant;
        if (powerAssistant != null) {
            powerAssistant.getPowerSaveTypeCallback().notifyPowerSaveTypeInitialized();
        }
    }

    public boolean isDozeOrSuspendMode() {
        return this.mDisplayManagerInternal.isDozeOrSuspendMode(0);
    }

    public void setIsScreenOnAnimation(boolean screenOnAnimation) {
        this.mPowerState.setIsScreenOnAnimation(screenOnAnimation);
    }

    public void dismissColorFadeForDoze() {
        VSlog.d(TAG, "animateScreenStateChange dismiss colorFade because of DOZE mode! ");
        this.mPowerState.dismissColorFade();
    }

    public void handleSettingsChange(boolean forceUpdate) {
        int colorFadeStyle = getColorFadeStyleSetting();
        boolean dynamicEffectsOn = getDynamicEffectsSetting() == 1;
        boolean colorFadeStyleChanged = colorFadeStyle != this.mLastColorFadeStyle;
        boolean dynamicEffectsChanged = dynamicEffectsOn != this.mLastDynamicEffectsOn;
        if (this.mDisplayPowerController.mColorFadeEnabled) {
            if (dynamicEffectsChanged || colorFadeStyleChanged || forceUpdate) {
                this.mIsPowered = this.mBatteryManagerInternal.isPowered(7);
                this.mLastColorFadeStyle = colorFadeStyle;
                this.mLastDynamicEffectsOn = dynamicEffectsOn;
                if (colorFadeStyle == -1) {
                    DisplayPowerController.COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 0;
                    DisplayPowerController.COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 0;
                } else {
                    switch (colorFadeStyle) {
                        case COLOR_FADE_ANIMATION_GLOBAL /* 4000 */:
                            if (!VivoColorFadeImpl.USE_SYSTEMUI_FADE || this.mIsKeyguardHide || this.mKeyguardOccluded || ((!dynamicEffectsOn || !this.mSupportRtblur) && this.mIsPowered)) {
                                DisplayPowerController.COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 350;
                                DisplayPowerController.COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 350;
                            } else {
                                DisplayPowerController.COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 0;
                                DisplayPowerController.COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 500;
                            }
                            this.mDisplayPowerController.mColorFadeOnAnimator.setInterpolator(new PathInterpolator(0.17f, 0.17f, 0.67f, 1.0f));
                            this.mDisplayPowerController.mColorFadeOffAnimator.setInterpolator(new PathInterpolator(0.17f, 0.17f, 0.5f, 1.0f));
                            break;
                        case 4001:
                            DisplayPowerController.COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 500;
                            DisplayPowerController.COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 500;
                            this.mDisplayPowerController.mColorFadeOnAnimator.setInterpolator(new LinearInterpolator());
                            this.mDisplayPowerController.mColorFadeOffAnimator.setInterpolator(new LinearInterpolator());
                            break;
                        case COLOR_FADE_ANIMATION_VERTICAL /* 4002 */:
                            DisplayPowerController.COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 500;
                            DisplayPowerController.COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 350;
                            this.mDisplayPowerController.mColorFadeOnAnimator.setInterpolator(new LinearInterpolator());
                            this.mDisplayPowerController.mColorFadeOffAnimator.setInterpolator(new LinearInterpolator());
                            break;
                    }
                }
                this.mDisplayPowerController.mColorFadeOnAnimator.setDuration(DisplayPowerController.COLOR_FADE_ON_ANIMATION_DURATION_MILLIS);
                this.mDisplayPowerController.mColorFadeOffAnimator.setDuration(DisplayPowerController.COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS);
                this.mPowerState.setColorFadeStyle(colorFadeStyle);
                this.mPowerState.setDynamicEffectsOn(dynamicEffectsOn);
                VSlog.d(TAG, "handleSettingsChange colorFadeStyle = " + colorFadeStyle + ", USE_SYSTEMUI_FADE = " + VivoColorFadeImpl.USE_SYSTEMUI_FADE + ", mIsKeyguardHide = " + this.mIsKeyguardHide + ", mKeyguardOccluded = " + this.mKeyguardOccluded + ", dynamicEffects = " + dynamicEffectsOn + ", mSupportRtblur = " + this.mSupportRtblur + ", mIsPowered = " + this.mIsPowered + ", mBlurSwitchName = " + this.mBlurSwitchName);
            }
        }
    }

    private int getColorFadeStyleSetting() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), "global_animation_color_fade_style", -1, -2);
    }

    private int getDynamicEffectsSetting() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), this.mBlurSwitchName, 1, -2);
    }

    public void startColorFadeOnAnimator() {
        this.mColorFadeHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayPowerControllerImpl.3
            @Override // java.lang.Runnable
            public void run() {
                VivoDisplayPowerControllerImpl.this.mDisplayPowerController.mColorFadeOnAnimator.start();
            }
        });
    }

    public void resetColorFadeForOn() {
        this.mDisplayPowerController.mPowerState.setColorFadeLevel(1.0f);
        this.colorFadeOffFlag = false;
    }

    public void setColorFadeOffFlag(boolean flag) {
        this.colorFadeOffFlag = flag;
    }

    public boolean getColorFadeOffFlag() {
        return this.colorFadeOffFlag;
    }

    private boolean isSupportRealtimeBlurSwitch(Context context) {
        boolean support = false;
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, 128);
            if (appInfo == null || appInfo.metaData == null) {
                VSlog.d(TAG, "getMetaData not initialized !");
            } else {
                support = appInfo.metaData.getBoolean("realtime_blur_switch_support", false);
                VSlog.d(TAG, "getMetaData realtime_blur_switch_support: " + support);
            }
        } catch (Exception e) {
            VSlog.d(TAG, "isSupportRealtimeBlurSwitch ", e);
        }
        return support;
    }

    public void dummy() {
    }

    public int notifyStateChanged(int state) {
        return this.mAutoBrightnessManager.notifyStateChanged(state);
    }

    public void startVivoDebounceHandler() {
        HandlerThread handlerThread = new HandlerThread("VivoAlsDebounce");
        this.mVivoDebounceThread = handlerThread;
        handlerThread.start();
        Handler handler = new Handler(this.mVivoDebounceThread.getLooper());
        this.mVivoDebounceHandler = handler;
        handler.post(this.mVivoGetFtColorManagerTask);
    }

    public void InitializeSensorDPC(Context context, SensorManager sensorManager, DisplayManagerInternal.DisplayPowerCallbacks callbacks, Handler handler) {
        this.mAnimateBrightnessHandler = new Handler(handler.getLooper());
        AutoBrightnessManagerImpl autoBrightnessManagerImpl = AutoBrightnessManagerImpl.getInstance(context);
        this.mAutoBrightnessManager = autoBrightnessManagerImpl;
        autoBrightnessManagerImpl.initAutoBrightnessManager(context, sensorManager, this.mAutoBrightnessCallback);
        this.mPowerAssistant = new PowerAssistant(context, this.mVivoDebounceHandler, this.mPowerAssistantCallback);
        this.mHandler = handler;
        this.mCallbacks = callbacks;
        this.mSensorManager = sensorManager;
        AutoBrightnessManagerImpl autoBrightnessManagerImpl2 = this.mAutoBrightnessManager;
        if (autoBrightnessManagerImpl2 != null) {
            autoBrightnessManagerImpl2.setUnderDisplayLightCallback(this.mUDLightCallback);
            PowerAssistant powerAssistant = this.mPowerAssistant;
            if (powerAssistant != null) {
                this.mAutoBrightnessManager.setAppBrightnessCallback(powerAssistant.getAppBrightnessCallback());
            }
        }
        try {
            this.mPositiveDelaytime = Integer.parseInt(SystemProperties.get(POSITIVE_DEBOUNCE_TIME, "0"));
        } catch (Exception e) {
            VSlog.d(TAG, "error on " + e.toString());
        }
    }

    public boolean isPowerKeyWakeUp(boolean mPowerKeyWakeUp, boolean mScreenOffBecauseOfProximity) {
        if (mPowerKeyWakeUp) {
            this.isPowerKeyWakeUp = true;
            return false;
        }
        return mScreenOffBecauseOfProximity;
    }

    public void requestPowerState(DisplayManagerInternal.DisplayPowerRequest request) {
        this.mAutoBrightnessManager.setUseAutoBrightness(request.useAutoBrightness);
        this.mAutoBrightnessManager.onGetSettings(request.settingBrightness, request.settingScreenBrightnessMode, request.brightnessModeOffBy, request.settingBrightnessChangeBy);
        boolean isUpSlide = isUpSlide(request);
        if ((request.useAutoBrightness && isUpSlide) || (request.settingBrightnessChangeBy.equals(VivoPermissionUtils.OS_PKG) && this.mAutoBrightnessManager.getBrightnessRestoreStatus())) {
            this.mAutoBrightnessManager.setBrightnessRestoreStatus(false);
            pendingUpdateAutoBright(request);
        }
        if (request.useAutoBrightness) {
            this.mSettingBrightness = request.settingBrightness;
        }
        if (request.useAutoBrightness && this.mScreenBrightnessMode != request.useAutoBrightness) {
            this.mBeingAckBrightnessModeChanging = true;
        }
        this.mScreenBrightnessMode = request.useAutoBrightness;
        this.bIsProximitySensorTag = request.bIsProximitySensorTag;
        this.mAutoBrightnessManager.setWindowBrightnessExitInfo(request.windowBrightnessExit, request.windowBrightnessExitTime);
    }

    public void getOldRequest() {
        oldPolicy = this.mPowerRequest.policy;
        this.oldUseAutoBrightness = this.mPowerRequest.useAutoBrightness;
        this.policyChanged = false;
    }

    public void notifyPolicyStateToAuto() {
        if (oldPolicy != this.mPowerRequest.policy) {
            int i = this.mPowerRequest.policy;
            if (i == 0) {
                notifyStateChanged(5);
                this.mWaitingApplyFirstLight = false;
            } else if (i == 1) {
                notifyStateChanged(29);
            } else if (i == 2) {
                notifyStateChanged(4);
            } else if (i == 3) {
                notifyStateChanged(3);
            }
            if (this.mPowerRequest.policy == 3) {
                this.policyChanged = true;
            }
            if ((oldPolicy == 3 && this.mPowerRequest.policy == 2) || (oldPolicy == 2 && this.mPowerRequest.policy == 3)) {
                this.dimStateChanged = true;
            }
        }
    }

    public void PowerModeChanged(DisplayPowerState mPowerState) {
        if (this.oldUseAutoBrightness != this.mPowerRequest.useAutoBrightness) {
            VSlog.d(TAG, "oldUseAutoBrightness " + this.oldUseAutoBrightness + " mPowerRequest.useAutoBrightness " + this.mPowerRequest.useAutoBrightness + " mPowerSaveType " + this.mPowerSaveType);
            if (this.oldUseAutoBrightness && this.mPowerSaveType == 1) {
                this.mBrightnessSettingChangedWhenPowerSaveEnable = true;
            }
            if (mPowerState != null && this.mBrightnessSettingChangedWhenPowerSaveEnable) {
                mPowerState.onPowerAssistantModeChanged(this.mPowerSaveType);
                mPowerState.onPowerAssistantModeChangedNotify();
            }
        }
    }

    public void updatePowerState(int state) {
        if (this.mPowerRequest.lightUpNow && !this.mPowerRequest.useProximitySensor) {
            this.mDisplayPowerController.setProximitySensorEnabled(false);
        }
        if (this.mPowerRequest.useProximitySensor && state != 1) {
            this.mDisplayPowerController.setProximitySensorEnabled(true);
            if (!this.mDisplayPowerController.mScreenOffBecauseOfProximity && this.mDisplayPowerController.mProximity == 1 && !this.isPowerKeyWakeUp) {
                this.mDisplayPowerController.mScreenOffBecauseOfProximity = true;
                if (this.mPowerRequest.callState != 0) {
                    this.mPowerRequest.policy = 0;
                }
                this.mDisplayPowerController.sendOnProximityPositiveWithWakelock();
            } else if (this.mDisplayPowerController.mScreenOffBecauseOfProximity && this.mDisplayPowerController.mProximity == 1) {
                if (this.mPowerRequest.callState != 0) {
                    this.mDisplayPowerController.mProximity = -1;
                }
            } else if (!this.mDisplayPowerController.mScreenOffBecauseOfProximity && this.mDisplayPowerController.mProximity == 0 && state == 2 && 1 == this.mPowerRequest.callState) {
                this.mDisplayPowerController.mScreenOffBecauseOfProximity = true;
            }
        } else if (this.mDisplayPowerController.mWaitingForNegativeProximity && (this.mDisplayPowerController.mProximity == 1 || (this.mDisplayPowerController.mProximity == -1 && this.mDisplayPowerController.mPendingProximity == 1))) {
            this.mDisplayPowerController.setProximitySensorEnabled(true);
        } else {
            if (this.mPowerRequest.useProximitySensor) {
                if (state == 1 && this.mDisplayPowerController.mProximity == 1) {
                    this.mDisplayPowerController.mScreenOffBecauseOfProximity = true;
                } else if (this.mDisplayPowerController.mScreenOffBecauseOfProximity) {
                    this.mDisplayPowerController.mProximity = -1;
                }
                this.mDisplayPowerController.setProximitySensorEnabled(true);
            } else {
                this.mDisplayPowerController.setProximitySensorEnabled(false);
            }
            this.mDisplayPowerController.mWaitingForNegativeProximity = false;
        }
        if (this.mDisplayPowerController.mScreenOffBecauseOfProximity && this.mDisplayPowerController.mProximity != 1) {
            this.mAnimateToDirectly = true;
            this.mDisplayPowerController.mScreenOffBecauseOfProximity = false;
            this.mDisplayPowerController.sendOnProximityNegativeWithWakelock();
            this.mNeedChangeStateBecauseProximity = true;
        }
    }

    public boolean getNeedChangeStateBecauseProximity() {
        return this.mNeedChangeStateBecauseProximity;
    }

    public void setNeedChangeStateBecauseProximity(boolean needChange) {
        this.mNeedChangeStateBecauseProximity = needChange;
    }

    public boolean ConvertState() {
        return this.mPowerRequest.callState == 0 || this.mPowerRequest.useProximitySensor;
    }

    public int changeProxWhenWakekey(DisplayPowerState mPowerState, int mProximity) {
        if (this.isPowerKeyWakeUp && mPowerState.getScreenBrightness() > 0.0f) {
            if (this.mPowerRequest.callState == 0 && mProximity == 1) {
                mProximity = -1;
            }
            this.isPowerKeyWakeUp = false;
        }
        return mProximity;
    }

    public boolean setLightSensorEnabled(int state, float brightness) {
        boolean autoBrightnessEnabled = false;
        VSlog.i(TAG, "useAutoBrightness = " + this.mPowerRequest.useAutoBrightness + "; state = " + state + "; brightness = " + brightness);
        if (this.mAutoBrightnessManager != null) {
            VSlog.i(TAG, "mLightSensorEnabled = " + this.mLightSensorEnabled);
            autoBrightnessEnabled = this.mPowerRequest.useAutoBrightness && state == 2 && brightness <= 0.0f;
            if (this.mLightSensorEnabled != autoBrightnessEnabled) {
                VSlog.d(TAG, "setLightSensorEnabled to " + autoBrightnessEnabled);
                if (autoBrightnessEnabled) {
                    this.mAnimateToDirectly = true;
                    this.mWatingFirstLightSensorValue = true;
                    this.mWaitingApplyFirstLight = false;
                } else {
                    this.mWatingFirstLightSensorValue = false;
                }
                this.mLightSensorEnabled = autoBrightnessEnabled;
                this.mAutoBrightnessManager.setLightSensorEnabled(autoBrightnessEnabled);
            } else {
                VSlog.d(TAG, "updatePowerState use=" + this.mPowerRequest.useAutoBrightness + " enabled=" + this.mLightSensorEnabled + "  state=" + Display.stateToString(state));
            }
            updateUseAlsBeforeScreenOffState(state, this.mPowerRequest);
        }
        return autoBrightnessEnabled;
    }

    public void animateScreenBrightnessImpl(int state, float brightness, DisplayPowerState mPowerState, boolean slowChange) {
        if (state == 2) {
            if (this.mAnimateToDirectly) {
                animateDirect(brightness);
                return;
            } else if (isFirstLux(brightness)) {
                animateFirstLux(brightness);
                return;
            } else if (this.mIsNeedQuickSetBrightness) {
                VSlog.d(TAG, "mIsNeedQuickSetBrightness= " + this.mIsNeedQuickSetBrightness);
                this.mDisplayPowerController.animateScreenBrightness(brightness, 0.0f);
                this.mIsNeedQuickSetBrightness = false;
                return;
            } else if (isChangeManuallyWhenAuto(brightness, this.mPowerRequest)) {
                changeManually(brightness, this.mPowerRequest);
                return;
            } else if (isBrightnessBar(brightness, this.mPowerRequest)) {
                changeBrightnessBar(brightness);
                return;
            } else if (stillWaitFirstLux(this.mPowerRequest.useAutoBrightness)) {
                VSlog.d(TAG, "still wait for first light sensor value. use=" + this.mPowerRequest.useAutoBrightness + " before=" + this.mUseAlsBeforeScreenOff + " wFirst=" + this.mWatingFirstLightSensorValue + " aFirst=" + this.mWaitingApplyFirstLight);
                return;
            } else {
                if (DEBUG) {
                    VSlog.d(TAG, "not wait for first light sensor value. use=" + this.mPowerRequest.useAutoBrightness + "; before=" + this.mUseAlsBeforeScreenOff + "; wFirst=" + this.mWatingFirstLightSensorValue + "; aFirst=" + this.mWaitingApplyFirstLight + "; screenBrightness=" + mPowerState.getScreenBrightness() + "; policyChanged =" + this.policyChanged + "; slowChange =" + slowChange);
                }
                if (quiklySwitchAutoMode(this.mPowerRequest.useAutoBrightness)) {
                    removeAnimateBrightnessTask(this.mPowerRequest.useAutoBrightness);
                }
                if (this.policyChanged) {
                    this.mDisplayPowerController.animateScreenBrightness(brightness, 0.0f);
                    this.policyChanged = false;
                    return;
                }
                if (!SensorConfig.floatEquals(this.mPowerRequest.screenBrightnessOverrideFromPem, this.mLastScreenBrightnessOverrideFromPem)) {
                    DisplayPowerController displayPowerController = this.mDisplayPowerController;
                    displayPowerController.animateScreenBrightness(displayPowerController.clampScreenBrightness(brightness), 500.0f);
                } else if (isNotAutoMode(this.mPowerRequest.useAutoBrightness, this.mPowerRequest.policy)) {
                    animateNotAutoMdoe(brightness, mPowerState);
                } else {
                    if (!this.mPowerRequest.lowPowerMode && this.mPowerRequest.policy == 2) {
                        slowChange = true;
                    }
                    if (!this.mBlockScreenFlag) {
                        if (this.mPowerRequest.policy == 2 && slowChange) {
                            DisplayPowerController displayPowerController2 = this.mDisplayPowerController;
                            displayPowerController2.animateScreenBrightness(displayPowerController2.clampScreenBrightness(brightness), 600.0f);
                        } else if (slowChange) {
                            if (this.mAutoBrightnessManager.needQuickUpdate()) {
                                DisplayPowerController displayPowerController3 = this.mDisplayPowerController;
                                displayPowerController3.animateScreenBrightness(displayPowerController3.clampScreenBrightness(brightness), 200.0f);
                            } else {
                                DisplayPowerController displayPowerController4 = this.mDisplayPowerController;
                                displayPowerController4.animateScreenBrightness(displayPowerController4.clampScreenBrightness(brightness), slowChange ? 40.0f : 200.0f);
                            }
                        } else if (brightness == 0.0f) {
                            VSlog.d(TAG, "fpblock no need to handle zero");
                        } else {
                            DisplayPowerController displayPowerController5 = this.mDisplayPowerController;
                            displayPowerController5.animateScreenBrightness(displayPowerController5.clampScreenBrightness(brightness), 0.0f);
                        }
                    } else {
                        this.mDisplayPowerController.animateScreenBrightness(brightness, 0.0f);
                    }
                }
                this.mLastScreenBrightnessOverrideFromPem = this.mPowerRequest.screenBrightnessOverrideFromPem;
                return;
            }
        }
        this.mDisplayPowerController.animateScreenBrightness(brightness, 0.0f);
    }

    private void animateDirect(float brightness) {
        int i = 0;
        this.mAnimateToDirectly = false;
        if (this.mLightSensorEnabled) {
            int delayScreenOnTime = SensorConfig.delayScreenOnTime();
            AutoBrightnessManagerImpl autoBrightnessManagerImpl = this.mAutoBrightnessManager;
            if (autoBrightnessManagerImpl != null && autoBrightnessManagerImpl.getSkipLuxFlag()) {
                i = 100;
            }
            int delayTime = delayScreenOnTime + i;
            this.mAnimateBrightnessHandler.removeCallbacks(this.mAnimateBrightnessTask);
            this.mAnimateBrightnessHandler.postDelayed(this.mAnimateBrightnessTask, delayTime);
            this.mAnimateBrightnessTaskStarted = true;
            VSlog.d(TAG, "dddd animateScreenBrightness(" + this.mDisplayPowerController.clampScreenBrightness(brightness) + ",0) in " + delayTime + " ms");
            return;
        }
        this.mDisplayPowerController.animateScreenBrightness(brightness, 0.0f);
        VSlog.d(TAG, "dddd animateScreenBrightness(" + this.mDisplayPowerController.clampScreenBrightness(brightness) + ",0)");
    }

    private boolean isFirstLux(float brightness) {
        return this.mLightSensorEnabled && brightness == this.mVivoWaitingBrightness && this.mWaitingApplyFirstLight && (this.mUseAlsBeforeScreenOff || this.mAutoBrightnessManager.getAnimateFlagForSuperPwrSaveMode());
    }

    private void animateFirstLux(float brightness) {
        this.mAnimateBrightnessHandler.removeCallbacks(this.mAnimateBrightnessTask);
        this.mAnimateBrightnessTaskStarted = false;
        if (this.mAutoBrightnessManager.getAnimateFlagForSuperPwrSaveMode()) {
            this.mAutoBrightnessManager.setAnimateFlagForSuperPwrSaveMode();
        }
        VSlog.d(TAG, "as 111 mWaitingApplyFirstLight animateScreenBrightness(" + this.mDisplayPowerController.clampScreenBrightness(brightness) + ",0)");
        DisplayPowerController displayPowerController = this.mDisplayPowerController;
        displayPowerController.animateScreenBrightness(displayPowerController.clampScreenBrightness(brightness), 0.0f);
        this.mWaitingApplyFirstLight = false;
        this.mUseAlsBeforeScreenOff = false;
    }

    private boolean isChangeManuallyWhenAuto(float brightness, DisplayManagerInternal.DisplayPowerRequest mPowerRequest) {
        boolean reOpenAuto = mPowerRequest.useAutoBrightness && brightness == this.mVivoWaitingBrightness && this.mBeingAckBrightnessModeChanging && this.mWaitingApplyFirstLight;
        if (!reOpenAuto) {
            this.mAutoBrightnessManager.setReOpenAutoBrightnessInfo(reOpenAuto, SystemClock.elapsedRealtime());
        }
        return reOpenAuto;
    }

    private void changeManually(float brightness, DisplayManagerInternal.DisplayPowerRequest mPowerRequest) {
        this.mAnimateBrightnessHandler.removeCallbacks(this.mAnimateBrightnessTask);
        this.mAnimateBrightnessTaskStarted = false;
        VSlog.d(TAG, "as 222 mBeingAckBrightnessModeChanging animateScreenBrightness(" + this.mDisplayPowerController.clampScreenBrightness(brightness) + ",0) mWaitingApplyFirstLight " + this.mWaitingApplyFirstLight);
        DisplayPowerController displayPowerController = this.mDisplayPowerController;
        displayPowerController.animateScreenBrightness(displayPowerController.clampScreenBrightness(brightness), 200.0f);
        this.mBeingAckBrightnessModeChanging = false;
        this.mWaitingApplyFirstLight = false;
    }

    private boolean isBrightnessBar(float brightness, DisplayManagerInternal.DisplayPowerRequest mPowerRequest) {
        boolean isEqual = ((double) Math.abs(brightness - this.mVivoWaitingBrightness)) < 1.0E-6d;
        return mPowerRequest.useAutoBrightness && isEqual && this.mBeingAckSettingChanging;
    }

    private void changeBrightnessBar(float brightness) {
        VSlog.d(TAG, "Ack the screen brightness change " + brightness + " when autobrightness mode when mAnimatedBrightness = " + this.mAnimatedBrightness);
        if (this.mAnimatedBrightness != brightness) {
            DisplayPowerController displayPowerController = this.mDisplayPowerController;
            displayPowerController.animateScreenBrightness(displayPowerController.clampScreenBrightness(brightness), 0.0f);
        }
        this.mBeingAckSettingChanging = false;
    }

    private boolean stillWaitFirstLux(boolean useAutoBrightness) {
        return useAutoBrightness && this.mUseAlsBeforeScreenOff && (this.mWatingFirstLightSensorValue || this.mWaitingApplyFirstLight);
    }

    private boolean quiklySwitchAutoMode(boolean useAutoBrightness) {
        return this.mAnimateBrightnessTaskStarted && (this.mLightSensorEnabled || !useAutoBrightness);
    }

    private void removeAnimateBrightnessTask(boolean useAutoBrightness) {
        VSlog.d(TAG, "mLightSensorEnabled " + this.mLightSensorEnabled + " useAuto " + useAutoBrightness + " so stop mAnimateBrightnessTask");
        this.mAnimateBrightnessHandler.removeCallbacks(this.mAnimateBrightnessTask);
        this.mAnimateBrightnessTaskStarted = false;
    }

    private boolean isNotAutoMode(boolean useAutoBrightness, int policy) {
        boolean isBright = policy == 3;
        return !useAutoBrightness && this.mBeingAckSettingScreenBrightnessChange && isBright;
    }

    private void animateNotAutoMdoe(float brightness, DisplayPowerState mPowerState) {
        if (!SensorConfig.floatEquals(this.mAnimatedBrightness, brightness)) {
            VSlog.d(TAG, "Ack the screen brightness change when non-autobrightness mode");
            DisplayPowerController displayPowerController = this.mDisplayPowerController;
            displayPowerController.animateScreenBrightness(displayPowerController.clampScreenBrightness(brightness), 0.0f);
        }
        this.mBeingAckSettingScreenBrightnessChange = false;
    }

    public void notifyToSensorService(int type) {
        VivoSensorImpl vivoSensorImpl = this.mVivoSensorImpl;
        if (vivoSensorImpl != null) {
            vivoSensorImpl.notifyDPCState(this.mAutobrightInfo.mLightLux, type, this.mPowerRequest.useAutoBrightness, this.mPowerRequest.policy);
        }
    }

    public void blockScreenOn() {
        this.mBlockScreenFlag = true;
    }

    public void unblockScreenOn() {
        this.mBlockScreenFlag = false;
    }

    public void setScreenState(int state, DisplayPowerState mPowerState) {
        boolean wasOn = mPowerState.getScreenState() != 1;
        boolean isOn = state != 1;
        if (isOn != wasOn) {
            AutoBrightnessManagerImpl autoBrightnessManagerImpl = this.mAutoBrightnessManager;
            if (autoBrightnessManagerImpl != null) {
                autoBrightnessManagerImpl.setScreenOn(isOn);
            }
            if (!isOn) {
                this.mAnimateToDirectly = true;
            }
        }
        updateUseAlsBeforeScreenOffState(state, this.mPowerRequest);
    }

    public void disableUnderProximity(int target, int rate) {
    }

    public void putAutoBrightnessSetting() {
        if (this.mPowerRequest.useAutoBrightness && !SensorConfig.floatEquals(this.mLastScreenAutoBrightness, this.mScreenAutoBrightness)) {
            Settings.System.putFloat(this.mContext.getContentResolver(), "vivo_screen_auto_brightness", this.mScreenAutoBrightness);
            this.mLastScreenAutoBrightness = this.mScreenAutoBrightness;
        }
    }

    public void setPositiveDelayTime(DisplayPowerState mPowerState, int mProximity, long time) {
        int mTempPositiveDelaytime;
        if (mPowerState.getScreenState() == 1) {
            mTempPositiveDelaytime = 0;
        } else {
            mTempPositiveDelaytime = this.mPositiveDelaytime;
        }
        if (this.mPowerRequest.callState != 0) {
            this.mProximityPositiveTime = mTempPositiveDelaytime + time;
            this.mDisplayPowerController.setPendingProximityDebounceTime(mTempPositiveDelaytime + time);
            return;
        }
        this.mDisplayPowerController.setPendingProximityDebounceTime(0 + time);
    }

    public void setNegtiveDelayTime(int mProximity, long time) {
        long now = SystemClock.uptimeMillis();
        if (this.mPowerRequest.callState != 0 && mProximity == 1) {
            long j = this.mProximityPositiveTime;
            if (now >= j && now - j < 100) {
                this.mDisplayPowerController.setPendingProximityDebounceTime(100 + time);
                VSlog.d(TAG, "PROXIMITY_NEGATIVE delay 100ms ");
                return;
            }
        }
        this.mDisplayPowerController.setPendingProximityDebounceTime(0 + time);
    }

    public int getAnimatedBrightness() {
        return (int) (this.mAnimatedBrightness * 255.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void isSettingPackage(String pkg) {
        if (!this.mLastUdPackage.equals(pkg)) {
            this.mLastUdPackage = pkg;
            if (pkg.equals(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS)) {
                this.mDisplayPowerController.mScreenBrightnessRampAnimator.needStopRampAnimator(true);
            } else {
                this.mDisplayPowerController.mScreenBrightnessRampAnimator.needStopRampAnimator(false);
            }
        }
    }

    private boolean isUpSlide(DisplayManagerInternal.DisplayPowerRequest request) {
        if (request.settingBrightness != this.mSettingBrightness && !VivoPermissionUtils.OS_PKG.equals(request.settingBrightnessChangeBy)) {
            this.mBeingAckSettingScreenBrightnessChange = true;
            VSlog.d(TAG, "mSettingBrightness = " + this.mSettingBrightness + " as request " + request.settingBrightness);
            return true;
        }
        return false;
    }

    private void pendingUpdateAutoBright(DisplayManagerInternal.DisplayPowerRequest request) {
        if (this.mSettingBrightness != -1.0f && this.mAutoBrightnessManager != null) {
            float brightness = request.settingBrightness;
            if (DEBUG_LIGHT_SENSOR) {
                VSlog.d(TAG, "notifyScreenBrightness " + brightness);
            }
            if (brightness > 0.0f) {
                this.mVivoDebounceHandler.removeCallbacks(this.mVivoAutoBrightnessTask);
                this.mAutobrightInfo.mBrightnessFloat = request.settingBrightness;
                this.mAutobrightInfo.mBrightnessAlgo = AutoBrightnessAlgoLcmBrightnessMap.float2AlgoBrightness(request.settingBrightness);
                VSlog.d(TAG, "AckSettingBrightnessChanged change mVivoWaitingBrightness from " + this.mVivoWaitingBrightness + " to " + brightness);
                if (brightness != this.mVivoWaitingBrightness && this.mPowerRequest != null) {
                    this.mVivoWaitingBrightness = brightness;
                    this.mBeingAckSettingChanging = true;
                    vivoUpdateAutoBrightness();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void vivoUpdateAutoBrightness() {
        AutoBrightnessManagerImpl autoBrightnessManagerImpl;
        if ((this.mVivoWaitingBrightness >= 0.0f || this.mBeingAckSettingChanging) && this.mPowerRequest.policy == 3) {
            VSlog.d(TAG, "ALS update mScreenAutoBrightness from " + this.mScreenAutoBrightness + " to " + this.mVivoWaitingBrightness);
            StringBuilder sb = new StringBuilder();
            sb.append("UpdateAutoBrightness mAutobrightInfo:");
            AutobrightInfo autobrightInfo = this.mAutobrightInfo;
            sb.append(autobrightInfo == null ? "null" : autobrightInfo.toString());
            VSlog.d(TAG, sb.toString());
            this.mScreenAutoBrightness = this.mVivoWaitingBrightness;
            if (this.mLightSensorEnabled && !this.mBeingAckSettingChanging && (autoBrightnessManagerImpl = this.mAutoBrightnessManager) != null) {
                autoBrightnessManagerImpl.brightnessBeenApplied(this.mAutobrightInfo);
                VSlog.d(TAG, "brightnessBeenApplied = " + this.mAutobrightInfo.mLightLevel);
            }
            if (this.mPowerRequest.useAutoBrightness && this.mUseAlsBeforeScreenOff && this.mWaitingApplyFirstLight) {
                if (this.mPowerRequest.policy == 3) {
                    this.mDisplayPowerController.animateScreenBrightness(this.mVivoWaitingBrightness, 0.0f);
                    if (DEBUG) {
                        VSlog.d(TAG, "call first brightness (" + this.mVivoWaitingBrightness + ")");
                    }
                } else if (DEBUG) {
                    VSlog.d(TAG, "NOT call first brightnesss(" + this.mVivoWaitingBrightness + ") policy=" + this.mPowerRequest.policy);
                }
            }
            if (this.mPowerRequest.policy == 3) {
                this.mDisplayPowerController.sendUpdatePowerState();
            }
        } else if (this.mAutoBrightnessManager != null) {
            VSlog.d(TAG, "ALS not executed as mVivoWaitingBrightness=" + this.mVivoWaitingBrightness + " mScreenAutoBrightness=" + this.mScreenAutoBrightness + " mVivoWaitingBrightness=" + this.mVivoWaitingBrightness + " mLightSensorEnabled=" + this.mLightSensorEnabled + " policy=" + this.mPowerRequest.policy + " isLuxValid=" + this.mAutoBrightnessManager.isLuxValid(this.mScreenAutoBrightness, this.mVivoWaitingBrightness));
        }
    }

    public void updateUseAlsBeforeScreenOffState(int state, DisplayManagerInternal.DisplayPowerRequest mPowerRequest) {
        if (state != 2 && mPowerRequest != null && mPowerRequest.useAutoBrightness) {
            this.mUseAlsBeforeScreenOff = true;
        } else if (!mPowerRequest.useAutoBrightness) {
            this.mUseAlsBeforeScreenOff = false;
        }
    }

    public float getAutoBrightness() {
        float brightness;
        if (SensorConfig.floatEquals(this.mScreenAutoBrightness, -1.0f)) {
            brightness = this.mSettingBrightness;
        } else {
            brightness = this.mScreenAutoBrightness;
        }
        if (SensorConfig.isGameOptimizeBrightness() && this.mPowerRequest.gameFrameRateMode == 1) {
            double gameOptimizeBrightness = SensorConfig.getGameOptimizeBrightness();
            if (brightness < gameOptimizeBrightness) {
                brightness = (float) gameOptimizeBrightness;
                VSlog.d(TAG, "getAutoBrightness GameFrameRateMode is on, gameOptimizeBrightness = " + gameOptimizeBrightness);
            }
        }
        if (!SensorConfig.floatEquals(this.mPowerRequest.screenBrightnessOverrideFromPem, -1.0f)) {
            if (this.mPowerRequest.screenBrightnessOverrideFromPem < this.mVivoWaitingBrightness) {
                float brightness2 = this.mPowerRequest.screenBrightnessOverrideFromPem;
                return brightness2;
            }
            float brightness3 = this.mVivoWaitingBrightness;
            return brightness3;
        }
        return brightness;
    }
}