package com.android.server.policy;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.SQLException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FtBuild;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.WindowManagerPolicyConstants;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.key.VDC_KEY_J_1;
import com.android.server.policy.motion.ThreeFingerGesture;
import com.android.server.wm.VivoMultiWindowConfig;
import com.vivo.common.VivoCollectData;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import vivo.util.FtFindPhoneLockUtil;

/* loaded from: classes.dex */
public final class VivoWMPHook {
    public static final String ACTION_VIVO_EMM_VOLUMEUP_LONGPRESS = "vivo.app.action.VIVO_EMM_VOLUMEUP_LONGPRESS";
    private static final String ACTION_VIVO_POLICY_MANAGER_STATE_CHANGED = "vivo.app.action.POLICY_MANAGER_STATE_CHANGED";
    private static final String DEBUG_FOLDER = "/data/vivodump";
    private static final String DUMP_FOLDER = "/data/vivo_dumpsys";
    private static final int MSG_LOOP = 0;
    public static final String NIGHT_PEARL_FUNCTION = "screen_off_remind";
    public static final String ONEKEY_START_FUNCTION = "smartkey_primary_switch";
    private static final float PROXIMITY_THRESHOLD = 5.0f;
    private static final String SMART_KEY_EVENT_FAILED_REASON = "reason";
    private static final int SMART_KEY_EVENT_FAILED_REASON_CALL = 4;
    private static final int SMART_KEY_EVENT_FAILED_REASON_MUSIC = 3;
    private static final int SMART_KEY_EVENT_FAILED_REASON_POWER_SAVE = 2;
    private static final int SMART_KEY_EVENT_FAILED_REASON_SENSOR = 1;
    private static final int SMART_KEY_EVENT_FAILED_REASON_SHORT_PRESS = 5;
    private static final String SMART_KEY_EVENT_ID = "1073";
    private static final String SMART_KEY_EVENT_LABEL_FAILED = "107351";
    private static final String SMART_KEY_EVENT_LABEL_SUCCEED = "107350";
    private static final String SMART_KEY_EVENT_STATUS = "status";
    private static final String SMART_KEY_EVENT_STATUS_FAILED = "fail";
    private static final String SMART_KEY_EVENT_STATUS_SUCCEED = "success";
    public static final String TAG = "VivoWMPHook";
    private static final int VIVO_TRANSACTION_OPERATION_VOLUME_LONGPRESS = 3207;
    private static final long VOLUME_LONG_PRESS_DELAY = ViewConfiguration.getKeyRepeatTimeout() * 2;
    private static final int VOLUME_UP_LONG_PRESS_TIMEOUT = 200;
    private boolean isSupportSmartMultiWindow;
    private boolean isVOS_1;
    public Context mContext;
    private int mCount;
    private AlertDialog mDialog;
    private boolean mIsFbeProject;
    private SparseArray<IVivoKeyBeforeDispatchingListener> mKeyBeforeDispatchingListeners;
    private SparseArray<IVivoKeyBeforeQueueingListener> mKeyBeforeQueueingListeners;
    private PowerManager.WakeLock mLongPressLock;
    private PowerManager mPowerManager;
    private Sensor mProximitySensor;
    private SensorManager mSensorManager;
    private SmartClickManager mSmartClickManager;
    private boolean mSmartKeyDisable;
    private ThreeFingerGesture mThreeFingerGesture;
    private UserManager mUserManager;
    private VDC_KEY_J_1 mVDC_KEY_J_1;
    public IVivoAdjustmentPolicy mVivoPolicy;
    private VivoPowerKeyOLPListener mVivoPowerKeyOLPListener;
    private long mVolumeDownKeyTime;
    private Handler mWMPHookHandler;
    private ArrayList<IVivoWindowListener> mWindowListeners;
    public IWindowManager mWindowManager;
    public WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;
    private final HandlerThread mWMPHookThread = new HandlerThread("vivo_wmp_hook");
    private final Object mLock = new Object();
    private Handler mHandler = new Handler();
    private File mDumpFolder = null;
    private boolean mVolumeKeyDown = false;
    private boolean mOneKeySwitchOn = false;
    private boolean mProximitySwitchOn = false;
    private boolean mProximiteRegistered = false;
    private boolean mIsNightpearlOpen = false;
    private SensorEventListener mProximityListener = new SensorEventListener() { // from class: com.android.server.policy.VivoWMPHook.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            float distance = event.values[0];
            if (distance < 0.0d || distance >= VivoWMPHook.PROXIMITY_THRESHOLD || distance >= VivoWMPHook.this.mProximitySensor.getMaximumRange()) {
                VivoWMPHook.this.mProximitySwitchOn = false;
            } else {
                VivoWMPHook.this.mProximitySwitchOn = true;
            }
            VLog.d(VivoWMPHook.TAG, "proxiity sensor change keySwitchOn is " + VivoWMPHook.this.mProximitySwitchOn);
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private Runnable mVolumeLongPressRunnable = new Runnable() { // from class: com.android.server.policy.VivoWMPHook.2
        @Override // java.lang.Runnable
        public void run() {
            VLog.d(VivoWMPHook.TAG, "volume key is long press, do something here.");
            if (!VivoWMPHook.this.mVivoPolicy.isAIKeyTriggered()) {
                boolean userKeyUnlocked = VivoWMPHook.this.mUserManager.isUserUnlocked();
                VLog.d(VivoWMPHook.TAG, "mIsFbeProject = " + VivoWMPHook.this.mIsFbeProject + " userKeyUnlocked = " + userKeyUnlocked);
                if (!VivoWMPHook.this.mIsFbeProject || userKeyUnlocked) {
                    try {
                        VivoWMPHook.this.mLongPressLock.acquire(1000L);
                        VivoWMPHook.this.mVivoPolicy.performHapticFeedback(0, true, true);
                        Intent oneKeyIntent = new Intent();
                        oneKeyIntent.setAction("vivo.action.ACTION_THE_KEY_TO_START_FUNCTIONS");
                        oneKeyIntent.setPackage("com.vivo.SmartKey");
                        VivoWMPHook.this.mContext.startServiceAsUser(oneKeyIntent, UserHandle.CURRENT);
                        VivoWMPHook.this.VDC_SmartKey_F_1(true, 0);
                        return;
                    } catch (Exception e) {
                        VLog.e(VivoWMPHook.TAG, "Fail to start smartkey service" + e);
                        return;
                    }
                }
                try {
                    VivoWMPHook.this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "SmartKey");
                    VivoWMPHook.this.mWindowManager.dismissKeyguard((IKeyguardDismissCallback) null, (CharSequence) null);
                    VLog.d(VivoWMPHook.TAG, "Fbe project and unlocked, request dismiss keyguard and return.");
                    return;
                } catch (RemoteException e2) {
                    VLog.d(VivoWMPHook.TAG, "Fbe project and unlocked, dismiss keyguard cause exception: " + e2);
                    return;
                }
            }
            VLog.d(VivoWMPHook.TAG, "return when ai key isTriggered!");
        }
    };
    private int mTimeIntval = 0;
    private int mFirstTime = 0;
    private boolean mShellEnabled = false;
    private boolean sRun = false;
    private File mDebugFolder = null;
    private KeyEvent mVolumeUpEventDown = null;
    private boolean mVolumeUpLongPressConsumed = false;
    private boolean mVolumeUpInject = false;
    private Runnable mVolumeUpLongPressRunnable = new Runnable() { // from class: com.android.server.policy.VivoWMPHook.7
        @Override // java.lang.Runnable
        public void run() {
            VivoWMPHook.this.mVolumeUpLongPressConsumed = true;
            VivoWMPHook vivoWMPHook = VivoWMPHook.this;
            vivoWMPHook.triggerVolumeUpLongPress(vivoWMPHook.mVolumeUpEventDown);
        }
    };
    private String mPackageForVolumeUpLongPress = null;
    private BroadcastReceiver mDPMReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.VivoWMPHook.8
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VLog.d(VivoWMPHook.TAG, "action=" + action + " package=" + VivoWMPHook.this.mPackageForVolumeUpLongPress);
            if (VivoWMPHook.ACTION_VIVO_POLICY_MANAGER_STATE_CHANGED.equals(action)) {
                int poId = intent.getIntExtra("poId", 0);
                if (poId == 0 || poId == VivoWMPHook.VIVO_TRANSACTION_OPERATION_VOLUME_LONGPRESS) {
                    VivoWMPHook vivoWMPHook = VivoWMPHook.this;
                    vivoWMPHook.mPackageForVolumeUpLongPress = vivoWMPHook.getPackageForVolumeUpLongPress();
                }
            }
        }
    };

    /* loaded from: classes.dex */
    public interface ShowListener {
        void onShown(IBinder iBinder);
    }

    public VivoWMPHook(Context context, IWindowManager windowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs, IVivoAdjustmentPolicy vivoPolicy) {
        boolean z = false;
        this.mSmartKeyDisable = false;
        this.mIsFbeProject = false;
        this.isVOS_1 = false;
        this.mContext = context;
        this.mWindowManager = windowManager;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mVivoPolicy = vivoPolicy;
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mProximitySensor = sensorManager.getDefaultSensor(8);
        this.mSmartKeyDisable = checkSmartKeyDisable();
        registerOnekeySettingObserver();
        registerProximityListener(true);
        VivoPolicyUtil.createInstance(context);
        this.mWindowListeners = new ArrayList<>();
        this.mVivoPowerKeyOLPListener = new VivoPowerKeyOLPListener(context, this.mHandler);
        PowerManager pm = (PowerManager) context.getSystemService("power");
        PowerManager.WakeLock newWakeLock = pm.newWakeLock(1, "VivoWMPHook.mVolumeKeyWakeLock");
        this.mLongPressLock = newWakeLock;
        newWakeLock.setReferenceCounted(true);
        this.mKeyBeforeQueueingListeners = new SparseArray<>();
        this.mKeyBeforeDispatchingListeners = new SparseArray<>();
        VivoWMPHookCreator.createInterceptKeyHandler(this);
        VivoWMPHookCreator.createPointerEventListener(this);
        startDumpThread();
        if ("yes".equals(SystemProperties.get("persist.vivo.abc.debug", "no"))) {
            SystemProperties.set("persist.vivo.abc.debug", "no");
        }
        this.mWMPHookThread.start();
        this.mWMPHookHandler = new Handler(this.mWMPHookThread.getLooper());
        this.mVDC_KEY_J_1 = new VDC_KEY_J_1(this.mContext);
        this.mIsFbeProject = "file".equals(SystemProperties.get("ro.crypto.type", "unknow"));
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        if ("vos".equals(FtBuild.getOsName()) && "1.0".equals(FtBuild.getOsVersion())) {
            z = true;
        }
        this.isVOS_1 = z;
        this.mSmartClickManager = new SmartClickManager();
        this.isSupportSmartMultiWindow = isSupportSmartMultiWindow(this.mContext);
        VLog.d(TAG, "is support smart multi window:" + this.isSupportSmartMultiWindow);
        if (!this.isSupportSmartMultiWindow) {
            ThreeFingerGesture threeFingerGesture = new ThreeFingerGesture(this.mContext, this);
            this.mThreeFingerGesture = threeFingerGesture;
            threeFingerGesture.register();
        }
    }

    public void systemReady() {
        ThreeFingerGesture threeFingerGesture;
        this.mSmartClickManager.onSystemReady();
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.policy.VivoWMPHook.3
            @Override // java.lang.Runnable
            public void run() {
                DevicePolicyManager mDpm = (DevicePolicyManager) VivoWMPHook.this.mContext.getSystemService("device_policy");
                int type = mDpm.getCustomType();
                if (type > 0) {
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(VivoWMPHook.ACTION_VIVO_POLICY_MANAGER_STATE_CHANGED);
                    VivoWMPHook.this.mContext.registerReceiverAsUser(VivoWMPHook.this.mDPMReceiver, UserHandle.ALL, filter, null, null);
                    VivoWMPHook vivoWMPHook = VivoWMPHook.this;
                    vivoWMPHook.mPackageForVolumeUpLongPress = vivoWMPHook.getPackageForVolumeUpLongPress();
                }
            }
        }, 1000L);
        if (!this.isSupportSmartMultiWindow && (threeFingerGesture = this.mThreeFingerGesture) != null) {
            threeFingerGesture.systemReady();
        }
    }

    public void registerPointerEventListener(WindowManagerPolicyConstants.PointerEventListener listener, int displayId) {
        this.mWindowManagerFuncs.registerPointerEventListener(listener, displayId);
    }

    public static void printf(String msg) {
        VivoPolicyUtil.printf(TAG, msg);
    }

    private void registerOnekeySettingObserver() {
        ContentResolver resolver = this.mContext.getContentResolver();
        try {
            boolean z = true;
            this.mOneKeySwitchOn = Settings.System.getIntForUser(this.mContext.getContentResolver(), ONEKEY_START_FUNCTION, 1, -2) > 0;
            if (Settings.System.getIntForUser(this.mContext.getContentResolver(), NIGHT_PEARL_FUNCTION, 0, -2) != 1) {
                z = false;
            }
            this.mIsNightpearlOpen = z;
            VLog.d(TAG, "registerSettingObserver oneKeySwitchOn = " + this.mOneKeySwitchOn + " mIsNightpearlOpen = " + this.mIsNightpearlOpen);
            resolver.registerContentObserver(Settings.System.getUriFor(ONEKEY_START_FUNCTION), false, new OnekeySettingsObserver(this.mHandler), -1);
            resolver.registerContentObserver(Settings.System.getUriFor(NIGHT_PEARL_FUNCTION), false, new NightpearlSettingsObserver(this.mHandler), -1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerProximityListener(boolean reg) {
        boolean entry = SystemProperties.get("ro.vivo.op.entry", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).contains("CMCC_RW");
        if (this.mSmartKeyDisable || entry) {
            return;
        }
        VLog.d(TAG, "The proximiteRegistered is " + this.mProximiteRegistered + "register is " + reg);
        if (reg && !this.mProximiteRegistered) {
            this.mProximiteRegistered = true;
            this.mSensorManager.registerListener(this.mProximityListener, this.mProximitySensor, 1, this.mHandler);
        } else if (!reg && this.mProximiteRegistered) {
            this.mProximiteRegistered = false;
            this.mSensorManager.unregisterListener(this.mProximityListener);
        }
    }

    private boolean checkSmartKeyDisable() {
        return !SystemProperties.getBoolean("persist.vivo.smartkey.enable", false);
    }

    public boolean onScreenTurnedOn(ShowListener showListener) {
        this.mWMPHookHandler.post(new Runnable() { // from class: com.android.server.policy.VivoWMPHook.4
            @Override // java.lang.Runnable
            public void run() {
                VivoWMPHook.this.registerProximityListener(false);
            }
        });
        int size = this.mWindowListeners.size();
        for (int i = 0; i < size; i++) {
            IVivoWindowListener listener = this.mWindowListeners.get(i);
            IBinder windowToken = listener.getWindowToken();
            if (windowToken != null) {
                showListener.onShown(windowToken);
                return true;
            }
        }
        return false;
    }

    public void finishScreenTurningOn(IBinder windowToken) {
        int size = this.mWindowListeners.size();
        for (int i = 0; i < size; i++) {
            IVivoWindowListener listener = this.mWindowListeners.get(i);
            IBinder token = listener.getWindowToken();
            if (windowToken == token) {
                listener.onFinishScreenTurningOn();
                return;
            }
        }
    }

    public void notifyWindowsDrawn(IBinder appToken) {
        int size = this.mWindowListeners.size();
        for (int i = 0; i < size; i++) {
            IVivoWindowListener listener = this.mWindowListeners.get(i);
            IBinder windowToken = listener.getWindowToken();
            if (windowToken != null) {
                listener.notifyWindowsDrawn(appToken);
                return;
            }
        }
    }

    public void onScreenTurnedOff() {
        this.mWMPHookHandler.post(new Runnable() { // from class: com.android.server.policy.VivoWMPHook.5
            @Override // java.lang.Runnable
            public void run() {
                VivoWMPHook.this.registerProximityListener(true);
            }
        });
    }

    public void registerWaitingForDrawnWindowListener(IVivoWindowListener listener) {
        if (this.mWindowListeners.contains(listener)) {
            return;
        }
        this.mWindowListeners.add(listener);
    }

    public void unregisterWaitingForDrawnWindowListener(IVivoWindowListener listener) {
        this.mWindowListeners.remove(listener);
    }

    public SparseArray<IVivoKeyBeforeQueueingListener> getKeyBeforeQueueingListener() {
        return this.mKeyBeforeQueueingListeners;
    }

    public void registerKeyBeforeQueueingListener(int keyCode, IVivoKeyBeforeQueueingListener listener) {
        if (keyCode <= 0) {
            VLog.e(TAG, "Invalid keyCode:" + keyCode);
        } else if (this.mKeyBeforeQueueingListeners.get(keyCode) != null) {
            VLog.e(TAG, "registerKeyBeforeQueueingListener: KeyCode=" + keyCode + " has already been registered, please contact the manager.");
        } else {
            this.mKeyBeforeQueueingListeners.put(keyCode, listener);
        }
    }

    public void unregisterKeyBeforeQueueingListener(int keyCode) {
        this.mKeyBeforeQueueingListeners.remove(keyCode);
    }

    public SparseArray<IVivoKeyBeforeDispatchingListener> getKeyBeforeDispatchingListener() {
        return this.mKeyBeforeDispatchingListeners;
    }

    public void registerKeyBeforeDispatchingListener(int keyCode, IVivoKeyBeforeDispatchingListener listener) {
        if (keyCode <= 0) {
            VLog.e(TAG, "Invalid keyCode:" + keyCode);
        } else if (this.mKeyBeforeDispatchingListeners.get(keyCode) != null) {
            VLog.e(TAG, "registerKeyBeforeDispatchingListener: KeyCode=" + keyCode + " has already been registered, please contact the manager.");
        } else {
            this.mKeyBeforeDispatchingListeners.put(keyCode, listener);
        }
    }

    public void unregisterKeyBeforeDispatchingListener(int keyCode) {
        this.mKeyBeforeDispatchingListeners.remove(keyCode);
    }

    boolean isCallActive() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        if (tm != null) {
            return tm.getCallState() != 0;
        }
        VLog.w(TAG, "isCallActive: couldn't get TelephonyManager reference");
        return false;
    }

    boolean isMusicActive() {
        AudioManager am = (AudioManager) this.mContext.getSystemService("audio");
        if (am != null) {
            return am.isMusicActive() || am.isMusicActiveRemotely() || AudioSystem.isStreamActive(0, 50);
        }
        VLog.w(TAG, "isMusicActive: couldn't get AudioManager reference");
        return false;
    }

    boolean isAlarmActive() {
        boolean z = false;
        boolean isAlarmActive = (AudioSystem.isStreamActiveRemotely(4, 0) || AudioSystem.isStreamActive(4, 0)) ? true : true;
        return isAlarmActive;
    }

    public boolean checkDropKeyBeforeQueueing(KeyEvent event, int policyFlags, boolean isScreenOn, boolean keyguardActive) {
        boolean alarmBoot = SystemProperties.getBoolean(VivoPolicyConstant.KEY_BOOT_REASON, false);
        boolean isDrop = alarmBoot;
        if (isScreenOn) {
            this.mHandler.removeCallbacks(this.mVolumeLongPressRunnable);
        }
        if (event.getAction() == 0) {
            this.mVDC_KEY_J_1.VDC_Key_F_1(event.getKeyCode());
        }
        if (event.getKeyCode() == 25) {
            boolean spsave = SystemProperties.getBoolean("sys.super_power_save", false);
            boolean smartEnable = (this.isVOS_1 || !this.mOneKeySwitchOn || this.mSmartKeyDisable || this.mProximitySwitchOn || spsave || isScreenOn) ? false : true;
            VLog.d(TAG, "checkDropKeyBeforeQueueing keyguardActive=" + keyguardActive + " ,isScreenOn=" + isScreenOn + " ,oneKeySwitchOn=" + this.mOneKeySwitchOn + " ,smartKeyDisable=" + this.mSmartKeyDisable + " ,proximityOn=" + this.mProximitySwitchOn + " ,volumeKeyDown=" + this.mVolumeKeyDown);
            boolean isMusicActive = isMusicActive();
            boolean isCallActive = isCallActive();
            if (event.getAction() == 0) {
                if (spsave) {
                    VDC_SmartKey_F_1(false, 2);
                } else if (this.mProximitySwitchOn) {
                    VDC_SmartKey_F_1(false, 1);
                } else if (isMusicActive) {
                    VDC_SmartKey_F_1(false, 3);
                } else if (isCallActive) {
                    VDC_SmartKey_F_1(false, 4);
                }
            }
            VLog.d(TAG, "checkDropKeyBeforeQueueing isMusicActive=" + isMusicActive + " ,isCallActive=" + isCallActive);
            if (!isMusicActive && !isCallActive && this.mVivoPolicy.getDisplay() != null && ((this.mVivoPolicy.getDisplay().getState() == 4 || this.mVivoPolicy.getDisplay().getState() == 3) && !isScreenOn)) {
                isDrop = true;
                VLog.d(TAG, "Drop volume key during DOZE mode");
            }
            if (smartEnable || this.mVolumeKeyDown) {
                if (this.mVolumeKeyDown) {
                    isDrop = true;
                    VLog.d(TAG, "checkDropKeyBeforeQueueing volumekey last down,drop true.");
                }
                if (this.mSmartClickManager.isStartWhenInMusic(isMusicActive) && !isCallActive) {
                    if (event.getAction() != 0) {
                        if (SystemClock.uptimeMillis() - this.mVolumeDownKeyTime <= VOLUME_LONG_PRESS_DELAY) {
                            VDC_SmartKey_F_1(false, 5);
                        }
                        VLog.d(TAG, "VolumeKey up remove callback.");
                        if (this.mLongPressLock.isHeld()) {
                            VLog.d(TAG, "VolumeKey up release lock.");
                            this.mLongPressLock.release();
                        }
                        this.mHandler.removeCallbacks(this.mVolumeLongPressRunnable);
                    } else if (event.getRepeatCount() == 0) {
                        this.mVolumeDownKeyTime = event.getDownTime();
                        VLog.d(TAG, "VolumeKey down post long press runnable.");
                        this.mVolumeKeyDown = true;
                        isDrop = true;
                        if (!this.mLongPressLock.isHeld()) {
                            this.mLongPressLock.acquire(VOLUME_LONG_PRESS_DELAY * 2);
                        }
                        this.mHandler.removeCallbacks(this.mVolumeLongPressRunnable);
                        this.mHandler.postDelayed(this.mVolumeLongPressRunnable, VOLUME_LONG_PRESS_DELAY);
                    }
                }
            } else {
                VLog.d(TAG, "smartquick not open.");
            }
            if (event.getKeyCode() == 25 && event.getAction() != 0) {
                this.mVolumeKeyDown = false;
            }
        }
        if (event.getKeyCode() == 24 && event.getAction() == 0 && !isMusicActive() && !isCallActive() && this.mVivoPolicy.getDisplay() != null && ((this.mVivoPolicy.getDisplay().getState() == 4 || this.mVivoPolicy.getDisplay().getState() == 3) && !isScreenOn)) {
            isDrop = true;
            VLog.d(TAG, "Drop volume key during DOZE mode");
        }
        if (event.getKeyCode() == 24 && isSupportVolumeUpLongPress()) {
            if (interceptVolumeUpKey(event, isScreenOn)) {
                return true;
            }
            return isDrop;
        }
        return isDrop;
    }

    public boolean checkDisableGlobalActionsDialog() {
        return FtFindPhoneLockUtil.isFindPhoneLocked(this.mContext) || ActivityManager.isUserAMonkey();
    }

    public VivoPowerKeyOLPListener getVivoPowerKeyOLPListener() {
        return this.mVivoPowerKeyOLPListener;
    }

    private void startDumpThread() {
        this.mFirstTime = SystemProperties.getInt("persist.vivo.shell.first", 2) * 60 * 1000;
        this.mTimeIntval = SystemProperties.getInt("persist.vivo.shell.interval", 10) * 60 * 1000;
        boolean z = SystemProperties.getBoolean("persist.vivo.shell.enable", false);
        this.mShellEnabled = z;
        if (!z) {
            return;
        }
        VLog.d(TAG, "startDumpThread timeintval = " + this.mTimeIntval + " ,firstTime = " + this.mFirstTime + " ,enabled = " + this.mShellEnabled);
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.policy.VivoWMPHook.6
            @Override // java.lang.Runnable
            public void run() {
                new SystemDebugPoker().execute(new Void[0]);
            }
        }, (long) this.mFirstTime);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void excuteShell() {
        VLog.d(TAG, "excuteShell timeintval = " + this.mTimeIntval + " ,enabled = " + this.mShellEnabled);
        if (!this.mShellEnabled) {
            return;
        }
        File file = this.mDebugFolder;
        if (file == null || !file.exists()) {
            boolean done = false;
            try {
                File file2 = new File(DEBUG_FOLDER);
                this.mDebugFolder = file2;
                if (!file2.exists()) {
                    done = this.mDebugFolder.mkdirs();
                }
                VLog.d(TAG, "creat vivodump done = " + done + " ,isExit = " + this.mDebugFolder.exists());
            } catch (Exception e) {
                VLog.w(TAG, "Unable to prepare vivodump file!");
            }
        }
        SimpleDateFormat sDateFormat = new SimpleDateFormat("MM-dd");
        SimpleDateFormat sTimeFormat = new SimpleDateFormat("hh:mm:ss");
        String date = sDateFormat.format(new Date());
        String time = sTimeFormat.format(new Date());
        String str = "/data/vivodump/result_" + date + "_" + time.replaceAll(":", "-") + ".txt";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void VDC_SmartKey_F_1(boolean succeed, int failedReason) {
        String eventLabel;
        if (FtBuild.isOverSeas()) {
            return;
        }
        HashMap<String, String> params = new HashMap<>();
        if (succeed) {
            params.put(SMART_KEY_EVENT_STATUS, SMART_KEY_EVENT_STATUS_SUCCEED);
            eventLabel = SMART_KEY_EVENT_LABEL_SUCCEED;
        } else {
            params.put(SMART_KEY_EVENT_STATUS, SMART_KEY_EVENT_STATUS_FAILED);
            params.put(SMART_KEY_EVENT_FAILED_REASON, failedReason + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            eventLabel = SMART_KEY_EVENT_LABEL_FAILED;
        }
        VivoCollectData collectData = VivoCollectData.getInstance(this.mContext);
        collectData.writeData(SMART_KEY_EVENT_ID, eventLabel, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params);
    }

    public void onUserSwitched() {
        ThreeFingerGesture threeFingerGesture;
        this.mOneKeySwitchOn = Settings.System.getIntForUser(this.mContext.getContentResolver(), ONEKEY_START_FUNCTION, 1, -2) > 0;
        if (!this.isSupportSmartMultiWindow && (threeFingerGesture = this.mThreeFingerGesture) != null) {
            threeFingerGesture.updateSettings();
        }
        VLog.d(TAG, "onUserSwitched oneKeySwitchOn = " + this.mOneKeySwitchOn);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class OnekeySettingsObserver extends ContentObserver {
        public OnekeySettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            try {
                VivoWMPHook vivoWMPHook = VivoWMPHook.this;
                boolean z = true;
                if (Settings.System.getIntForUser(VivoWMPHook.this.mContext.getContentResolver(), VivoWMPHook.ONEKEY_START_FUNCTION, 1, -2) <= 0) {
                    z = false;
                }
                vivoWMPHook.mOneKeySwitchOn = z;
                VLog.d(VivoWMPHook.TAG, "OnekeySettingsObserver oneKeySwitchOn = " + VivoWMPHook.this.mOneKeySwitchOn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class NightpearlSettingsObserver extends ContentObserver {
        public NightpearlSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            try {
                VivoWMPHook.this.mIsNightpearlOpen = Settings.System.getIntForUser(VivoWMPHook.this.mContext.getContentResolver(), VivoWMPHook.NIGHT_PEARL_FUNCTION, 0, -2) == 1;
                VLog.d(VivoWMPHook.TAG, "OnekeySettingsObserver mIsNightpearlOpen = " + VivoWMPHook.this.mIsNightpearlOpen);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /* loaded from: classes.dex */
    class SystemDebugPoker extends AsyncTask<Void, Void, Void> {
        private boolean mExit = false;

        SystemDebugPoker() {
        }

        public void exit() {
            this.mExit = true;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Void doInBackground(Void... params) {
            while (!this.mExit) {
                VivoWMPHook.this.excuteShell();
                try {
                    Thread.sleep(VivoWMPHook.this.mTimeIntval);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private boolean interceptVolumeUpKey(KeyEvent event, boolean isScreenOn) {
        boolean down = event.getAction() == 0;
        if (down) {
            KeyEvent keyEvent = this.mVolumeUpEventDown;
            if (keyEvent == null || keyEvent.getDownTime() != event.getDownTime()) {
                this.mVolumeUpEventDown = KeyEvent.obtain(event);
                this.mHandler.removeCallbacks(this.mVolumeUpLongPressRunnable);
                this.mHandler.postDelayed(this.mVolumeUpLongPressRunnable, 200L);
                if (!isScreenOn && !this.mLongPressLock.isHeld()) {
                    VLog.d(TAG, "volume up long press acquire wakelock");
                    this.mLongPressLock.acquire(400L);
                }
                return true;
            }
            return false;
        }
        KeyEvent keyEvent2 = this.mVolumeUpEventDown;
        if (keyEvent2 == null || keyEvent2.getDownTime() != event.getDownTime()) {
            return false;
        }
        if (this.mVolumeUpLongPressConsumed) {
            VLog.d(TAG, "volume up long press up");
            this.mVolumeUpEventDown.recycle();
            this.mVolumeUpEventDown = null;
            this.mVolumeUpLongPressConsumed = false;
            triggerVolumeUpLongPress(event);
            return true;
        }
        this.mHandler.removeCallbacks(this.mVolumeUpLongPressRunnable);
        if (!this.mVolumeUpInject) {
            VLog.d(TAG, "volume up short press, inject event");
            this.mVolumeUpInject = true;
            InputManager.getInstance().injectInputEvent(this.mVolumeUpEventDown, 0);
            InputManager.getInstance().injectInputEvent(event, 0);
            if (this.mLongPressLock.isHeld()) {
                VLog.d(TAG, "volume up long press remove wakelock");
                this.mLongPressLock.release();
            }
            return true;
        }
        VLog.d(TAG, "volume up short press up");
        this.mVolumeUpEventDown.recycle();
        this.mVolumeUpEventDown = null;
        this.mVolumeUpLongPressConsumed = false;
        this.mVolumeUpInject = false;
        return false;
    }

    private boolean isSupportVolumeUpLongPress() {
        boolean isSupport = false;
        String str = this.mPackageForVolumeUpLongPress;
        if (str != null && !(isSupport = isPackageValid(str))) {
            VLog.d(TAG, "cannot find package or corresponding service:" + this.mPackageForVolumeUpLongPress);
        }
        return isSupport;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void triggerVolumeUpLongPress(KeyEvent event) {
        boolean userKeyUnlocked = this.mUserManager.isUserUnlocked();
        if (this.mIsFbeProject && !userKeyUnlocked) {
            try {
                this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "VolumeUpLongPressKey");
                this.mWindowManager.dismissKeyguard((IKeyguardDismissCallback) null, (CharSequence) null);
                VLog.d(TAG, "Fbe project and unlocked, request dismiss keyguard and return.");
                return;
            } catch (RemoteException e) {
                VLog.d(TAG, "Fbe project and unlocked, dismiss keyguard cause exception: " + e);
                return;
            }
        }
        boolean down = event.getAction() == 0;
        VLog.d(TAG, "triggerVolumeUpLongPress keycode=" + event.getKeyCode() + " down=" + down + " mPackageForVolumeUpLongPress=" + this.mPackageForVolumeUpLongPress);
        if (this.mPackageForVolumeUpLongPress == null) {
            VLog.d(TAG, "volume up long press package is null");
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setAction(ACTION_VIVO_EMM_VOLUMEUP_LONGPRESS);
            intent.setPackage(this.mPackageForVolumeUpLongPress);
            intent.putExtra("action", down ? 0 : 1);
            this.mContext.startForegroundServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e2) {
            VLog.e(TAG, "Fail to start service" + e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getPackageForVolumeUpLongPress() {
        DevicePolicyManager mDpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        Bundle bundle = mDpm.getInfoDeviceTransaction(null, VIVO_TRANSACTION_OPERATION_VOLUME_LONGPRESS, null);
        if (bundle == null) {
            return null;
        }
        return bundle.getString("package_name");
    }

    private boolean isPackageValid(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return false;
        }
        Intent intent = new Intent(ACTION_VIVO_EMM_VOLUMEUP_LONGPRESS);
        PackageManager pms = this.mContext.getPackageManager();
        List<ResolveInfo> list = pms.queryIntentServices(intent, 786496);
        if (list == null) {
            return false;
        }
        for (ResolveInfo resolveInfo : list) {
            if (pkg.equals(resolveInfo.serviceInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSupportSmartMultiWindow(Context context) {
        List<PackageInfo> packageInfos = context.getPackageManager().getInstalledPackages(0);
        for (PackageInfo item : packageInfos) {
            String pkg = item.packageName;
            if (VivoMultiWindowConfig.SMART_MULTIWINDOW_NAME.equals(pkg)) {
                return true;
            }
        }
        return false;
    }
}