package com.android.server.policy;

import android.app.ActivityManagerNative;
import android.app.ActivityTaskManager;
import android.app.AlertDialog;
import android.app.IActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.SQLException;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.session.MediaSessionLegacyHelper;
import android.multidisplay.MultiDisplayManager;
import android.os.FtBuild;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.view.Display;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Toast;
import com.android.internal.policy.KeyInterceptionInfo;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.telecom.ITelecomService;
import com.android.internal.telephony.VivoTelephonyApiParams;
import com.android.server.LocalServices;
import com.android.server.accessibility.AccessibilityWaterMark;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.input.ThreeFingerConfigManager;
import com.android.server.pm.VivoPKMSLocManager;
import com.android.server.policy.VivoWMPHook;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.key.VDC_KEY_J_1;
import com.android.server.policy.key.VivoAIKeyExtend;
import com.android.server.policy.keyguard.KeyguardServiceDelegate;
import com.android.server.wm.WindowState;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.framework.systemdefence.SystemDefenceManager;
import com.vivo.services.autorecover.SystemAutoRecoverManagerInternal;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.rms.sdk.Consts;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoInputPolicy implements IVivoAdjustmentPolicy {
    private static final String ACTION_AI_KEY_AUTO_TEST = "vivo.intent.action.AI_KEY_AUTO_TEST";
    private static final String ACTION_GAME_PAD_LEFT_KEY_TEST = "vivo.intent.action.GAME_PAD_LEFT_KEY_TEST";
    private static final String ACTION_GAME_PAD_RIGHT_KEY_TEST = "vivo.intent.action.GAME_PAD_RIGHT_KEY_TEST";
    private static final String ACTION_MENU_KEY_AUTO_TEST = "vivo.intent.action..MENU_KEY_AUTO_TEST";
    private static final String ACTION_POWER_KEY_AUTO_TEST = "vivo.intent.action.POWER_KEY_AUTO_TEST";
    private static final String ACTION_TOP_POWER_KEY_TEST = "vivo.intent.action.TOP_POWER_KEY_TEST";
    private static final String ACTION_VOLUME_DOWN_SIDE_KEY_TEST = "vivo.intent.action.VOLUME_DOWN_SIDE_KEY_TEST";
    private static final String ACTION_VOLUME_UP_SIDE_KEY_TEST = "vivo.intent.action.VOLUME_UP_SIDE_KEY_TEST";
    private static final int JOVI_VOICE_TRIGGER_TIME_OUT = 500;
    private static final String KEY_VIB_MODEL = "EXP1933";
    private static final int MSG_AI_KEY_LONG_PRESS = 104;
    private static final int MSG_NOTIFY_ORITATION_CHANGE = 101;
    private static final int MSG_NOTIFY_SCREEN_OFF = 102;
    private static final int MSG_START_JOVI_VOICE = 103;
    private static final String TAG = "VivoInputPolicy";
    private static final long VIVO_SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS = 500;
    private static final long VIVO_SCREENSHOT_CHORD_LONGPRESS_DELAY_MILLIS = 50;
    private static final long VIVO_SCREENSHOT_CHORD_VOLUME_DELAY_MILLIS = 150;
    AccessibilityWaterMark mA11WaterMark;
    private AudioManager mAudioManager;
    private Context mContext;
    private boolean mFingerKeyPressed;
    private ForceBackManager mForceBackManager;
    private boolean mGlobalAiKeyHandled;
    private boolean mGlobalAiKeyTriggered;
    private long mGlobalAiKeyTriggeredTime;
    private boolean mGlobalPowerKeyTriggered;
    private boolean mGlobalVolumeDownKeyTriggered;
    private boolean mGlobalVolumeUpKeyTriggered;
    private Handler mHandler;
    private boolean mHasMultiDisplay;
    private boolean mHasPressurePowerkey;
    private boolean mHomeKeyConsumedByScreenshotChord;
    private boolean mHomeKeyScreenshotPending;
    private long mHomeKeyTime;
    private boolean mHomeKeyTriggered;
    private boolean mIsExport;
    private boolean mIsPhysiscalHomeKey;
    private boolean mIsSupportAiKey;
    private boolean mIsSupportFingerKey;
    private boolean mJoviGuideShown;
    private long mMonsterAiKeyDownTime;
    private long mMonsterGamePadKeyDownTime;
    private PhoneWindowManager mPhoneWindowManager;
    private volatile boolean mPowerKeyHandledByJovi;
    private boolean mPowerKeyTriggerJoviVoiceEnabled;
    private long mScreenshotChordAIKeyTime;
    private boolean mScreenshotChordVolumeUpKeyConsumed;
    private long mScreenshotChordVolumeUpKeyTime;
    private boolean mShouldShowJoviGuide;
    private boolean mSupportFeedbackLra;
    private ThreeFingerConfigManager mThreeFingerConfigManager;
    private boolean mTopPowerKeyHandled;
    Vibrator mVibrator;
    private VivoPolicyHelper mVivoPolicyHelper;
    private boolean mVivoScreenshotChordEnabled;
    private VivoWMPHook mVivoWMPHook;
    private static final long SCREENSHOT_HANDLER_DELAY_MILLIS = SystemProperties.getInt("persist.vivo.volumekey.delay", 1000);
    private static final int[] WINDOW_TYPES_WHERE_HOME_DOESNT_WORK = {2003, 2010};
    private static int POWER_INTERVAL_DELAY_FOR_SOS = VivoPKMSLocManager.MAX_LOCATION_WAIT_TIME;
    private boolean mScreenshotChordDelay = true;
    private AlertDialog mAlertDialog = null;
    private byte[] mLock = new byte[0];
    private boolean mIsInterceptByCall = false;
    private boolean mInterceptWhenAlarm = false;
    private int mLastDoubleKey = 0;
    private int mKeyDoubleCount = 0;
    private boolean mShouldSnoozeAlarm = true;
    private boolean mSupportPowerStartVivoPay = false;
    private int mDebugCount = 0;
    private MultiDisplayManager mMultiDisplayManager = null;
    private boolean mAIKeyHandled = false;
    private boolean mScreenshotChordAIKeyTriggered = false;
    private boolean mIsFlashLightOn = false;
    private boolean mDisplayChangeAnimating = false;
    private boolean mIsGameRunning = false;
    private long mLastTime = -1;
    private boolean mComboKeyScreenshotEnabled = true;
    private boolean mIsAiKeyHandled = false;
    private boolean mMonsterAiKeyTriggered = false;
    private boolean mMonsterGamePadKeyTriggered = false;
    private boolean mMonsterKeyEnabled = false;
    private boolean mMonsterModeEnabled = false;
    private int mLastKeyCode = 0;
    private boolean mSupportKeyVibrate = false;
    private boolean mInterceptInputKey = false;
    private boolean mIsFamilyCare = false;
    private int mSosCount = 0;
    private int mLastKeyCodeForSosAI = 0;
    private Runnable mKeyDoubleClick = new Runnable() { // from class: com.android.server.policy.VivoInputPolicy.1
        @Override // java.lang.Runnable
        public void run() {
            VivoInputPolicy.this.mKeyDoubleCount = 0;
            long now = SystemClock.elapsedRealtime();
            if (VivoInputPolicy.this.mLastTime != -1 && now - VivoInputPolicy.this.mLastTime < 2000) {
                VLog.d(VivoInputPolicy.TAG, "Cancel showtoast!");
                return;
            }
            VLog.d(VivoInputPolicy.TAG, "showtoast:" + (SystemClock.elapsedRealtime() - VivoInputPolicy.this.mLastTime));
            VivoInputPolicy.this.mLastTime = now;
            Toast.makeText(VivoInputPolicy.this.mContext, VivoInputPolicy.this.mContext.getResources().getString(51249805), 0).show();
        }
    };
    private final Runnable mScreenshotTimeoutRunnable = new Runnable() { // from class: com.android.server.policy.VivoInputPolicy.2
        @Override // java.lang.Runnable
        public void run() {
            VLog.d(VivoInputPolicy.TAG, "ScreenshotCord delay reset.");
            VivoInputPolicy.this.mScreenshotChordDelay = true;
        }
    };
    private final StartJoviRunnable mStartJoviRunnable = new StartJoviRunnable();
    private Runnable mAiKeyLongPressRunnable = new Runnable() { // from class: com.android.server.policy.VivoInputPolicy.3
        @Override // java.lang.Runnable
        public void run() {
            VivoInputPolicy.this.aiKeyLongPress();
        }
    };
    private BroadcastReceiver mScreenshotReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.VivoInputPolicy.10
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "vivo.intent.action.SCREEN_SHOT".equals(intent.getAction())) {
                VLog.d(VivoInputPolicy.TAG, "take screen shot.");
                VivoInputPolicy.this.mHandler.removeCallbacks(VivoInputPolicy.this.mPhoneWindowManager.mScreenshotRunnable);
                VivoInputPolicy.this.mPhoneWindowManager.setScreenshotType(1);
                VivoInputPolicy.this.mPhoneWindowManager.setScreenshotSource(6);
                VivoInputPolicy.this.mHandler.post(VivoInputPolicy.this.mPhoneWindowManager.mScreenshotRunnable);
            }
        }
    };
    private Runnable mTriggerMonsterModeRunnable = new Runnable() { // from class: com.android.server.policy.VivoInputPolicy.11
        @Override // java.lang.Runnable
        public void run() {
            VivoInputPolicy.this.changeMonsterStatus();
        }
    };
    private Runnable mResetSosCountRunnable = new Runnable() { // from class: com.android.server.policy.VivoInputPolicy.12
        @Override // java.lang.Runnable
        public void run() {
            VLog.d(VivoInputPolicy.TAG, "PowerKey reset sos click times because overtime.");
            VivoInputPolicy.this.mSosCount = 0;
        }
    };

    public VivoInputPolicy(PhoneWindowManager phoneWindowManager, Context context, Handler handler) {
        this.mPhoneWindowManager = phoneWindowManager;
        this.mContext = context;
        this.mHandler = handler;
    }

    public void initVivoInputPolicy(Context context, IWindowManager windowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        boolean z;
        boolean z2;
        VSlog.i(TAG, "initForVivo");
        boolean z3 = true;
        this.mVivoScreenshotChordEnabled = true;
        this.mHomeKeyScreenshotPending = false;
        this.mFingerKeyPressed = false;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mVivoWMPHook = VivoWMPHookCreator.createInstance(context, windowManager, windowManagerFuncs, this);
        this.mIsPhysiscalHomeKey = SystemProperties.getBoolean("persist.vivo.fingerprint.front", false);
        this.mIsSupportFingerKey = SystemProperties.getBoolean("persist.vivo.support.fingerkey", false);
        this.mIsExport = "yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no"));
        if (SystemProperties.getInt("persist.vivo.support_press_key", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        this.mHasPressurePowerkey = z;
        if (SystemProperties.getInt("persist.vivo.support.aikey", 0) != 0) {
            z2 = true;
        } else {
            z2 = false;
        }
        this.mIsSupportAiKey = z2;
        boolean isPowerStartVivoPaySupport = isPowerStartVivoPaySupport();
        this.mSupportPowerStartVivoPay = isPowerStartVivoPaySupport;
        if (isPowerStartVivoPaySupport) {
            VivoPayObserver vivoPayObserver = new VivoPayObserver(this.mHandler);
            vivoPayObserver.observe();
        }
        PowerKeyTriggerJoviVoiceObserver powerKeyTriggerJoviVoiceObserver = new PowerKeyTriggerJoviVoiceObserver(this.mHandler);
        powerKeyTriggerJoviVoiceObserver.observe();
        NavBarLandScapePositionObserver navBarLandScapePositionObserver = new NavBarLandScapePositionObserver(this.mHandler);
        navBarLandScapePositionObserver.observe();
        this.mVivoPolicyHelper = new VivoPolicyHelper(context, this);
        if (!hasNavigationBar() && SystemProperties.getInt("persist.vivo.game_no_disturb", 0) == 1) {
            GameModeObserver gameModeObserver = new GameModeObserver(this.mHandler);
            gameModeObserver.observe();
        }
        if (this.mHasPressurePowerkey) {
            ComboKeyStatusObserver comboKeyStatusObserver = new ComboKeyStatusObserver(this.mHandler);
            comboKeyStatusObserver.observe();
        }
        MonsterModeObserver monsterModeObserver = new MonsterModeObserver(this.mHandler);
        monsterModeObserver.observe();
        IntentFilter filter = new IntentFilter();
        filter.addAction("vivo.intent.action.SCREEN_SHOT");
        context.registerReceiver(this.mScreenshotReceiver, filter);
        boolean hasMultiDisplay = hasMultiDisplay();
        this.mHasMultiDisplay = hasMultiDisplay;
        if (hasMultiDisplay) {
            this.mMultiDisplayManager = (MultiDisplayManager) this.mContext.getSystemService("multidisplay");
            DisplayChangeAnimatingObserver displayChangeAnimatingObserver = new DisplayChangeAnimatingObserver(this.mHandler);
            displayChangeAnimatingObserver.observe();
            FlashLightObserver flashLightObserver = new FlashLightObserver(this.mHandler);
            flashLightObserver.observe();
        }
        this.mForceBackManager = new ForceBackManager(this.mContext, this.mHandler, windowManagerFuncs);
        this.mThreeFingerConfigManager = ThreeFingerConfigManager.getInstance(this.mContext);
        String product = SystemProperties.get("ro.vivo.product.model", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        if (!product.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && KEY_VIB_MODEL.contains(product)) {
            this.mSupportKeyVibrate = true;
            VLog.d(TAG, "interceptKeyBeforeQueueingForVivo key vibrate product = " + product + "mSupportKeyVibrate = " + this.mSupportKeyVibrate);
        } else {
            this.mSupportKeyVibrate = false;
        }
        if (SystemProperties.getInt("persist.vivo.support.lra", 0) != 1) {
            z3 = false;
        }
        this.mSupportFeedbackLra = z3;
        FamilyCareObserver familyCareObserver = new FamilyCareObserver(this.mHandler);
        familyCareObserver.observe();
        this.mA11WaterMark = new AccessibilityWaterMark(this.mContext, this.mHandler);
    }

    public boolean isMusicActive() {
        AudioManager audioManager = this.mAudioManager;
        if (audioManager != null) {
            return audioManager.isMusicActive() || this.mAudioManager.isMusicActiveRemotely() || AudioSystem.isStreamActive(0, 50);
        }
        VLog.w("WindowManager", "isMusicActive: couldn't get AudioManager reference");
        return false;
    }

    private boolean isAlarmActive() {
        return AudioSystem.isStreamActive(4, 0);
    }

    private void cancelPendingScreenshotKey() {
        VLog.d(TAG, "cancelPendingScreenshotKey.");
        this.mHomeKeyScreenshotPending = false;
        this.mPhoneWindowManager.mScreenshotChordVolumeDownKeyTriggered = false;
    }

    private boolean waitForVivo(final WindowManagerPolicy.ScreenOnListener screenOnListener) {
        if (screenOnListener != null) {
            boolean hasWindow = this.mVivoWMPHook.onScreenTurnedOn(new VivoWMPHook.ShowListener() { // from class: com.android.server.policy.VivoInputPolicy.4
                @Override // com.android.server.policy.VivoWMPHook.ShowListener
                public void onShown(IBinder windowToken) {
                    VivoInputPolicy.this.waitForWindowDrawn(windowToken, screenOnListener);
                }
            });
            return hasWindow;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void waitForWindowDrawn(IBinder windowToken, WindowManagerPolicy.ScreenOnListener screenOnListener) {
    }

    public void finishScreenTurningOn() {
        this.mVivoWMPHook.onScreenTurnedOn(new VivoWMPHook.ShowListener() { // from class: com.android.server.policy.VivoInputPolicy.5
            @Override // com.android.server.policy.VivoWMPHook.ShowListener
            public void onShown(IBinder windowToken) {
                if (windowToken != null) {
                    VivoInputPolicy.this.mVivoWMPHook.finishScreenTurningOn(windowToken);
                }
            }
        });
    }

    private void notifyWindowsDrawn(IBinder appToken) {
        this.mVivoWMPHook.notifyWindowsDrawn(appToken);
    }

    public boolean isPhysiscalHomeKey(int keyCode) {
        return (keyCode == 3 || keyCode == 305 || keyCode == 304) && this.mIsPhysiscalHomeKey;
    }

    private void performAudioFeedback() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        if (audioManager == null) {
            VLog.w(TAG, "Couldn't get audio manager");
        } else {
            audioManager.playSoundEffect(0);
        }
    }

    private ITelecomService getITelecom() {
        return ITelecomService.Stub.asInterface(ServiceManager.getService("telecom"));
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:288:0x05d5
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    public int interceptKeyBeforeQueueing(android.view.KeyEvent r29, int r30, boolean r31, boolean r32) {
        /*
            Method dump skipped, instructions count: 2163
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.VivoInputPolicy.interceptKeyBeforeQueueing(android.view.KeyEvent, int, boolean, boolean):int");
    }

    public int interceptKeyBeforeDispatching(KeyInterceptionInfo keyInterceptionInfo, KeyEvent event, int policyFlags, boolean keyguardOn) {
        int metaState;
        int i;
        AlertDialog alertDialog;
        int keyCode = event.getKeyCode();
        int repeatCount = event.getRepeatCount();
        event.getMetaState();
        int flags = event.getFlags();
        boolean down = event.getAction() == 0;
        boolean canceled = event.isCanceled();
        boolean isLongPress = event.isLongPress();
        VLog.d(TAG, "interceptKeyTi keyCode=" + keyCode + " down=" + down + " repeatCount=" + repeatCount + " keyguardOn=" + keyguardOn + " canceled=" + canceled);
        StringBuilder sb = new StringBuilder();
        sb.append("intercept win = ");
        sb.append(keyInterceptionInfo);
        VLog.d(TAG, sb.toString());
        ForceBackManager forceBackManager = this.mForceBackManager;
        if (forceBackManager != null && keyInterceptionInfo != null) {
            forceBackManager.countingBackEvent(keyInterceptionInfo, event);
        }
        if (4 == event.getKeyCode()) {
            try {
                ((SystemAutoRecoverManagerInternal) LocalServices.getService(SystemAutoRecoverManagerInternal.class)).reportBackKey(event, keyInterceptionInfo);
            } catch (Exception e) {
                VLog.d(TAG, "reportBackKeyWhenNoFocusedWindow cause exception: " + e);
            }
        }
        synchronized (this.mLock) {
            try {
                IVivoKeyBeforeDispatchingListener listener = this.mVivoWMPHook.getKeyBeforeDispatchingListener().get(keyCode);
                if (listener != null) {
                    try {
                        int indeResult = listener.onInterceptKeyBeforeDispatching(keyInterceptionInfo, event, policyFlags, keyguardOn);
                        if (indeResult != -100) {
                            try {
                                VLog.i(TAG, listener + " handled keyCode=" + keyCode);
                                return indeResult;
                            } catch (Throwable th) {
                                th = th;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                }
                                throw th;
                            }
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                try {
                    if (keyCode == 3) {
                        metaState = 1;
                        if (this.mFingerKeyPressed) {
                            VLog.d(TAG, "FingerkeyPressed is true,break here!");
                            if (!this.mVivoScreenshotChordEnabled || (flags & Consts.ProcessStates.FOCUS) != 0) {
                                i = 0;
                            } else if (this.mHomeKeyConsumedByScreenshotChord) {
                                VLog.d(TAG, "interceptKeyBeforeDispatchingForVivo HomeKeyConsumedByScreenshotChord ,return -1.");
                                return -1;
                            } else {
                                i = 0;
                            }
                        } else if (!this.mVivoScreenshotChordEnabled || (flags & Consts.ProcessStates.FOCUS) != 0) {
                            i = 0;
                        } else if (this.mHomeKeyConsumedByScreenshotChord) {
                            if (!down) {
                                this.mHomeKeyConsumedByScreenshotChord = false;
                            }
                            VLog.d(TAG, "interceptKeyBeforeDispatchingForVivo HomeKeyConsumedByScreenshotChord ,return -1.");
                            return -1;
                        } else {
                            i = 0;
                        }
                    } else if (keyCode == 24 || keyCode == 25) {
                        if (this.mScreenshotChordDelay && this.mPhoneWindowManager.mA11yShortcutChordVolumeUpKeyTriggered && !this.mPhoneWindowManager.mScreenshotChordPowerKeyTriggered) {
                            long now = SystemClock.uptimeMillis();
                            long timeoutTime = this.mScreenshotChordVolumeUpKeyTime + PhoneWindowManager.SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS;
                            if (now < timeoutTime) {
                                this.mScreenshotChordDelay = false;
                                long delay = (timeoutTime - now) + SCREENSHOT_HANDLER_DELAY_MILLIS;
                                this.mHandler.removeCallbacks(this.mScreenshotTimeoutRunnable);
                                this.mHandler.postDelayed(this.mScreenshotTimeoutRunnable, delay);
                                VLog.d(TAG, "interceptKeyTi delaytime = " + delay);
                                return (int) (timeoutTime - now);
                            }
                        }
                        if (keyCode == 24 && this.mScreenshotChordVolumeUpKeyConsumed) {
                            if (!down) {
                                this.mScreenshotChordVolumeUpKeyConsumed = false;
                            }
                            VLog.i(TAG, "VolumeUp Key ConsumedByDismissPinning, discard here!");
                            return -1;
                        } else if (!this.mPhoneWindowManager.isScreenOn() && isMusicActive()) {
                            VLog.d(TAG, "sendVolumeKeyEvent.");
                            MediaSessionLegacyHelper.getHelper(this.mContext).sendVolumeKeyEvent(event, Integer.MIN_VALUE, true);
                            return -1;
                        } else {
                            metaState = 1;
                            i = 0;
                        }
                    } else if (keyCode != 305 && keyCode != 306) {
                        i = 0;
                        metaState = 1;
                    } else if (!this.mVivoScreenshotChordEnabled || (flags & Consts.ProcessStates.FOCUS) != 0) {
                        i = 0;
                        metaState = 1;
                    } else if (this.mHomeKeyConsumedByScreenshotChord) {
                        if (!down) {
                            this.mHomeKeyConsumedByScreenshotChord = false;
                        }
                        VLog.d(TAG, "interceptKeyBeforeDispatchingForVivo HomeKeyConsumedByScreenshotChord ,return -1.");
                        return -1;
                    } else {
                        i = 0;
                        metaState = 1;
                    }
                    if (!down && ((82 == keyCode || 3 == keyCode || 4 == keyCode || 305 == keyCode || 306 == keyCode) && (alertDialog = this.mAlertDialog) != null)) {
                        alertDialog.dismiss();
                        this.mAlertDialog = null;
                    }
                    if ((keyCode == 3 || keyCode == 305 || keyCode == 306) && isLongPress) {
                        int privateFlags = keyInterceptionInfo != null ? keyInterceptionInfo.layoutParamsPrivateFlags : 0;
                        int isDisableLongPress = (VivoPolicyConstant.PRIVATE_FLAG_DISABLE_HOMEKEY_LONGPRESS & privateFlags) != 0 ? metaState : i;
                        if (isDisableLongPress == 0) {
                            if (VivoPolicyUtil.isSPSMode() || VivoPolicyUtil.isDrivingMode()) {
                                i = metaState;
                            }
                            isDisableLongPress = i;
                        }
                        if (isDisableLongPress != 0) {
                            VLog.i("WindowManager", "Home key long press is disabled.");
                            return -1;
                        }
                    }
                    if (hasNavigationBar() || !isPhysiscalHomeKey(keyCode)) {
                        return -100;
                    }
                    return this.mVivoPolicyHelper.interceptHomeKeyForVivo(keyInterceptionInfo, event, policyFlags, keyguardOn);
                } catch (Throwable th4) {
                    th = th4;
                    while (true) {
                        break;
                        break;
                    }
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleShow() {
        View textEntryView;
        LayoutInflater factory = LayoutInflater.from(this.mContext);
        if (this.mHasPressurePowerkey) {
            textEntryView = factory.inflate(50528397, (ViewGroup) null);
        } else {
            textEntryView = factory.inflate(50528396, (ViewGroup) null);
        }
        AlertDialog create = new AlertDialog.Builder(this.mContext, 51314692).setMessage(51249908).setTitle(51249909).setView(textEntryView).setPositiveButton(51249907, new DialogInterface.OnClickListener() { // from class: com.android.server.policy.VivoInputPolicy.6
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        }).setCancelable(false).create();
        this.mAlertDialog = create;
        create.getWindow().setType(2003);
        this.mAlertDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showWarmingDialog() {
        VLog.d(TAG, "showWarmingDialog : " + this.mAlertDialog);
        AlertDialog alertDialog = this.mAlertDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
            this.mAlertDialog = null;
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.VivoInputPolicy.7
                @Override // java.lang.Runnable
                public void run() {
                    VivoInputPolicy.this.handleShow();
                }
            });
            return;
        }
        handleShow();
    }

    private boolean interceptDismissPinningChord() {
        IActivityManager activityManager = ActivityManagerNative.asInterface(ServiceManager.checkService(VivoFirewall.TYPE_ACTIVITY));
        if (activityManager == null) {
            VLog.w(TAG, "ActivityManager is null ,return.");
            return false;
        }
        try {
        } catch (RemoteException e) {
            VLog.e(TAG, "RemoteException when stopLockTaskModeOnCurrent", e);
        }
        if (!activityManager.isInLockTaskMode()) {
            VLog.w(TAG, "Not in LockTaskMode!");
            return false;
        }
        ActivityTaskManager.getService().stopSystemLockTaskMode();
        performHapticFeedback(0, false, true);
        return true;
    }

    public void interceptScreenshotChord() {
        VLog.d(TAG, "interceptScreenshotChord downKeyTriggered=" + this.mPhoneWindowManager.mScreenshotChordVolumeDownKeyTriggered + " ,upKeyTriggered=" + this.mPhoneWindowManager.mA11yShortcutChordVolumeUpKeyTriggered + " ,powerKeyTriggered=" + this.mPhoneWindowManager.mScreenshotChordPowerKeyTriggered + " ,homeKeyTriggered=" + this.mHomeKeyTriggered);
        if (this.mVivoScreenshotChordEnabled && this.mPhoneWindowManager.mScreenshotChordVolumeDownKeyTriggered && this.mPhoneWindowManager.mScreenshotChordPowerKeyTriggered) {
            long now = SystemClock.uptimeMillis();
            if (now <= this.mPhoneWindowManager.mScreenshotChordVolumeDownKeyTime + VIVO_SCREENSHOT_CHORD_VOLUME_DELAY_MILLIS && now <= this.mPhoneWindowManager.mScreenshotChordPowerKeyTime + VIVO_SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
                this.mPhoneWindowManager.mScreenshotChordVolumeDownKeyConsumed = true;
                this.mPhoneWindowManager.cancelPendingPowerKeyAction();
                if (this.mComboKeyScreenshotEnabled) {
                    this.mPhoneWindowManager.setScreenshotType(1);
                    this.mPhoneWindowManager.setScreenshotSource(1);
                    VLog.d(TAG, "interceptScreenshotChord postScreenshot.");
                    WindowState focusedWindow = this.mPhoneWindowManager.mDefaultDisplayPolicy.getFocusedWindow();
                    String wins = focusedWindow != null ? focusedWindow.toString() : " ";
                    if (wins != null && wins.contains("com.ndt.sidekey.hwtest.core.HwTestActivity")) {
                        VLog.w(TAG, "interceptScreenshotChordForVivo HwTestActivity skip Screenshot. ");
                        return;
                    } else {
                        this.mHandler.postDelayed(this.mPhoneWindowManager.mScreenshotRunnable, VIVO_SCREENSHOT_CHORD_LONGPRESS_DELAY_MILLIS);
                        triggerDumpStacktraceToDebug();
                    }
                }
            }
            this.mShouldSnoozeAlarm = false;
        } else {
            this.mShouldSnoozeAlarm = true;
        }
        if (this.mPhoneWindowManager.mA11yShortcutChordVolumeUpKeyTriggered && this.mPhoneWindowManager.mScreenshotChordPowerKeyTriggered) {
            long now2 = SystemClock.uptimeMillis();
            if (now2 <= this.mScreenshotChordVolumeUpKeyTime + VIVO_SCREENSHOT_CHORD_VOLUME_DELAY_MILLIS && now2 <= this.mPhoneWindowManager.mScreenshotChordPowerKeyTime + VIVO_SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
                this.mScreenshotChordVolumeUpKeyConsumed = true;
                this.mPhoneWindowManager.cancelPendingPowerKeyAction();
            }
        }
        if (this.mVivoScreenshotChordEnabled && this.mHomeKeyTriggered && this.mPhoneWindowManager.mScreenshotChordPowerKeyTriggered) {
            long now3 = SystemClock.uptimeMillis();
            if (now3 <= this.mHomeKeyTime + VIVO_SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS && now3 <= this.mPhoneWindowManager.mScreenshotChordPowerKeyTime + VIVO_SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS && this.mHomeKeyTime - this.mPhoneWindowManager.mScreenshotChordPowerKeyTime < ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout()) {
                this.mHomeKeyConsumedByScreenshotChord = true;
                this.mPhoneWindowManager.cancelPendingPowerKeyAction();
                this.mHandler.post(new Runnable() { // from class: com.android.server.policy.VivoInputPolicy.8
                    @Override // java.lang.Runnable
                    public void run() {
                        VivoInputPolicy.this.showWarmingDialog();
                    }
                });
            }
        }
        if (this.mHasMultiDisplay && this.mScreenshotChordAIKeyTriggered && this.mPhoneWindowManager.mScreenshotChordPowerKeyTriggered) {
            long now4 = SystemClock.uptimeMillis();
            if (now4 <= this.mScreenshotChordAIKeyTime + VIVO_SCREENSHOT_CHORD_VOLUME_DELAY_MILLIS && now4 <= this.mPhoneWindowManager.mScreenshotChordPowerKeyTime + VIVO_SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
                this.mPhoneWindowManager.cancelPendingPowerKeyAction();
                cancelPendingAIKeyAction();
                triggerSwitchAwake();
            }
        }
    }

    private void changeUpslideState(boolean down, boolean canceled) {
        try {
            IStatusBarService sbs = this.mPhoneWindowManager.getStatusBarService();
            if (sbs != null) {
                VivoPolicyUtil.invokeMethod(sbs, "changeUpslideState", new Class[]{Boolean.TYPE, Boolean.TYPE}, new Object[]{Boolean.valueOf(down), Boolean.valueOf(canceled)});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkBaseRequirement(KeyEvent event, boolean down, int keyCode, int policyFlags) {
        boolean isInjected = (16777216 & policyFlags) != 0 && (policyFlags & 2) == 0;
        VLog.d(TAG, "checkBaseRequirement inJected : " + isInjected);
        if (!isInjected && 301 != keyCode && down && event.getRepeatCount() == 0) {
            if ((policyFlags & 2) != 0 || 82 == keyCode || 3 == keyCode || 4 == keyCode) {
                return true;
            }
            return false;
        }
        return false;
    }

    private void performVirtualKeyHapticFeedback(KeyEvent event, int policyFlags, boolean isScreenOn, boolean keyguardActive) {
        boolean down = event.getAction() == 0;
        int keyCode = event.getKeyCode();
        boolean alwaysWhenEnable = 3 == keyCode;
        if ((!keyguardActive || alwaysWhenEnable) && checkBaseRequirement(event, down, keyCode, policyFlags)) {
            int effectId = 1;
            if (this.mSupportFeedbackLra && (82 == keyCode || 3 == keyCode || 4 == keyCode)) {
                effectId = ProcessList.CACHED_APP_MAX_ADJ;
            }
            performHapticFeedback(effectId, false, alwaysWhenEnable);
        }
    }

    private void performVirtualKeyAudioFeedback(KeyEvent event, int policyFlags, boolean isScreenOn, boolean keyguardActive) {
        boolean isInjected = (16777216 & policyFlags) != 0 && (policyFlags & 2) == 0;
        int keyCode = event.getKeyCode();
        boolean down = event.getAction() == 0;
        if (!down || event.getRepeatCount() != 0 || isInjected || keyguardActive) {
            return;
        }
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        if (audioManager == null) {
            VLog.w(TAG, "Couldn't get audio manager");
        } else if (keyCode == 3 || keyCode == 4 || keyCode == 82) {
            audioManager.playSoundEffect(0);
        }
    }

    public boolean checkDisableGlobalActionsDialog() {
        boolean disabled = this.mVivoWMPHook.checkDisableGlobalActionsDialog();
        if (disabled) {
            VLog.w(TAG, "checkDisableGlobalActionsDialog is true, return here!");
        }
        return disabled;
    }

    public void screenTurningOff() {
        try {
            ((SystemAutoRecoverManagerInternal) LocalServices.getService(SystemAutoRecoverManagerInternal.class)).onScreenTurnOff();
        } catch (Exception e) {
            VLog.d(TAG, "SystemAutoRecoverManagerInternal onScreenTurnOff cause exception: " + e);
        }
        this.mVivoWMPHook.onScreenTurnedOff();
    }

    @Override // com.android.server.policy.IVivoAdjustmentPolicy
    public void sendMediaKeyEvent(KeyEvent event) {
        this.mPhoneWindowManager.sendMediaKeyEvent(event);
    }

    @Override // com.android.server.policy.IVivoAdjustmentPolicy
    public boolean performHapticFeedback(int effectId, boolean always, boolean alwaysWhenEnable) {
        if (!always) {
            boolean hapticsDisabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 0, -2) == 0;
            if (!hapticsDisabled && alwaysWhenEnable) {
                always = true;
            }
        }
        return this.mPhoneWindowManager.performHapticFeedback(Process.myUid(), this.mContext.getOpPackageName(), effectId, always, "Vivo Policy");
    }

    @Override // com.android.server.policy.IVivoAdjustmentPolicy
    public void requestScreenShot() {
        this.mHandler.removeCallbacks(this.mPhoneWindowManager.mScreenshotRunnable);
        this.mHandler.post(this.mPhoneWindowManager.mScreenshotRunnable);
    }

    @Override // com.android.server.policy.IVivoAdjustmentPolicy
    public void doCustomKeyHandler(int keyCode, int action) {
        if (action == 0) {
            this.mPhoneWindowManager.handleShortPressOnHome(0);
        } else if (action == 1) {
            this.mPhoneWindowManager.preloadRecentApps();
        } else if (action == 2) {
            this.mPhoneWindowManager.handleLongPressOnHome(0);
        } else {
            VLog.d(TAG, "No any handler action.");
        }
    }

    @Override // com.android.server.policy.IVivoAdjustmentPolicy
    public void handleMetaKeyEvent() {
        this.mPhoneWindowManager.handleMetaKeyEvent();
    }

    @Override // com.android.server.policy.IVivoAdjustmentPolicy
    public void keyguardDone(boolean authenticated, boolean wakeup) {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mPhoneWindowManager.mKeyguardDelegate;
    }

    private void printf(String msg) {
        VivoWMPHook.printf(msg);
    }

    @Override // com.android.server.policy.IVivoAdjustmentPolicy
    public Display getDisplay() {
        return this.mPhoneWindowManager.mDefaultDisplay;
    }

    @Override // com.android.server.policy.IVivoAdjustmentPolicy
    public boolean isAIKeyHandled() {
        return this.mGlobalAiKeyHandled || this.mIsAiKeyHandled || this.mMonsterGamePadKeyTriggered;
    }

    public void onUserSwitched() {
        this.mVivoWMPHook.onUserSwitched();
        updateMonsterModeSettings();
        updateComboKeyStatus();
        updateGameSetting();
        updateVivoPaySettings();
        updatePowerKeyTriggerJoviVoiceSettings();
        this.mA11WaterMark.onUserSwitched();
    }

    public void onKeyGuardChange(boolean isShowing) {
        this.mA11WaterMark.onKeyGuardChange(isShowing);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateGameSetting() {
        try {
            ContentResolver resolver = this.mContext.getContentResolver();
            boolean isGameModeOn = Settings.System.getIntForUser(resolver, VivoAIKeyExtend.GAME_DISTURB_ENABLED, 0, -2) == 1;
            String curActiveGame = Settings.System.getStringForUser(resolver, VivoAIKeyExtend.GAME_CURRENT_PACKAGE, -2);
            String gameList = Settings.System.getStringForUser(resolver, "enabled_shield_bottom_button", -2);
            if (!isGameModeOn) {
                this.mIsGameRunning = false;
            } else if (Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(curActiveGame)) {
                this.mIsGameRunning = false;
            } else if (gameList != null && curActiveGame != null && gameList.contains(curActiveGame)) {
                this.mIsGameRunning = true;
            } else {
                this.mIsGameRunning = false;
            }
            VLog.d(TAG, "updateGameSetting gameModeOn = " + isGameModeOn + " ,activeGame = [" + curActiveGame + "] ,gameList = [" + gameList + "] ,isGameRunning = " + this.mIsGameRunning);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isPowerStartVivoPaySupport() {
        boolean supportEse = SystemProperties.getBoolean("persist.vendor.vivo.support.ese", false);
        if (!supportEse) {
            VLog.d(TAG, "VIVO PAY: not support ese");
            return false;
        }
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            VLog.d(TAG, "VIVO PAY: PackageManager null");
            return false;
        }
        List<PackageInfo> pakageinfos = pm.getInstalledPackages(8320);
        for (PackageInfo item : pakageinfos) {
            String pkg = item.packageName;
            if (pkg.startsWith("com.vivo.wallet")) {
                if (item.applicationInfo.metaData == null) {
                    VLog.d(TAG, "VIVO PAY: installed but no metadata");
                    return false;
                }
                return item.applicationInfo.metaData.getBoolean("com.vivo.wallet.support.nfc");
            }
        }
        VLog.d(TAG, "VIVO PAY: not installed");
        return false;
    }

    public void startVivoPay() {
        VLog.d(TAG, "Starting vivo pay");
        try {
            Intent intent = new Intent();
            intent.setAction("vivo.intent.action.NFC_BUS_SWINGSPLASH");
            intent.putExtra("lb_from", "2");
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            VLog.d(TAG, "VIVO PAY: start vivo pay cause exception: " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVivoPaySettings() {
        if (this.mSupportPowerStartVivoPay) {
            ContentResolver resolver = this.mContext.getContentResolver();
            boolean doublePressOnPowerStartVivoPayEnable = Settings.System.getIntForUser(resolver, "com_vivo_wallet_nfc_support_double_click", 0, -2) == 1;
            if (doublePressOnPowerStartVivoPayEnable) {
                this.mPhoneWindowManager.mDoublePressOnPowerBehavior = 3;
            } else {
                this.mPhoneWindowManager.mDoublePressOnPowerBehavior = 0;
            }
        }
    }

    private TelecomManager getTelecommService() {
        return (TelecomManager) this.mContext.getSystemService("telecom");
    }

    private void sendStartJoviVoiceMessage(int keyCode) {
        this.mStartJoviRunnable.setKeyCode(keyCode);
        this.mHandler.postDelayed(this.mStartJoviRunnable, VIVO_SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS);
    }

    public void cancelJoviVoice() {
        VLog.d(TAG, "cancelJoviVoice mPowerKeyHandledByJovi = " + this.mPowerKeyHandledByJovi);
        if (!this.mPowerKeyHandledByJovi) {
            this.mPowerKeyHandledByJovi = true;
            this.mHandler.removeCallbacks(this.mStartJoviRunnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startJoviVoice(int keyCode) {
        if (this.mIsSupportAiKey) {
            VLog.d(TAG, "device has AI key");
        } else if (!this.mPhoneWindowManager.isUserSetupComplete()) {
            VLog.e(TAG, "device not setup!");
        } else if (!this.mPowerKeyTriggerJoviVoiceEnabled && this.mShouldShowJoviGuide && !this.mJoviGuideShown) {
            boolean isStartGuide = startJoviVoiceGudie();
            if (isStartGuide) {
                if (keyCode == 26) {
                    this.mPowerKeyHandledByJovi = true;
                    return;
                } else if (keyCode == 308) {
                    this.mAIKeyHandled = true;
                    return;
                } else {
                    return;
                }
            }
            VLog.d(TAG, "to start jovi guide failed.");
        } else {
            boolean isStartGuide2 = this.mPowerKeyTriggerJoviVoiceEnabled;
            if (!isStartGuide2) {
                VLog.d(TAG, "power key trigger jovi voice not enable");
                return;
            }
            if (keyCode == 26) {
                this.mPowerKeyHandledByJovi = true;
            } else if (keyCode == 308) {
                this.mAIKeyHandled = true;
            }
            Intent intent = new Intent("com.vivo.intent.action.WAKEUP_AGENT_BY_POWER");
            intent.setPackage("com.vivo.agent");
            try {
                VLog.d(TAG, "startJoviVoice");
                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
            } catch (Exception e) {
                VLog.e(TAG, "startJoviVoice failed : " + e);
            }
        }
    }

    private boolean startJoviVoiceGudie() {
        Intent intent = new Intent("com.vivo.intent.action.POWER_KEY_GUIDE");
        intent.setPackage("com.vivo.agent");
        try {
            VLog.d(TAG, "startJoviVoiceGudie");
            ComponentName componentName = this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
            if (componentName != null) {
                return true;
            }
            return false;
        } catch (Exception e) {
            VLog.e(TAG, "startJoviVoiceGudie failed : " + e);
            return false;
        }
    }

    public boolean getScreenshotChordDelay() {
        return this.mScreenshotChordDelay;
    }

    public void postScreenShotTimeOutRunnable(long delta) {
        this.mScreenshotChordDelay = false;
        long delay = SCREENSHOT_HANDLER_DELAY_MILLIS + delta;
        this.mHandler.removeCallbacks(this.mScreenshotTimeoutRunnable);
        this.mHandler.postDelayed(this.mScreenshotTimeoutRunnable, delay);
        VLog.d(TAG, "interceptKeyTi delaytime = " + delay);
    }

    public void postDelayScreenShotTimeOutRunnable() {
        if (!this.mScreenshotChordDelay) {
            this.mHandler.removeCallbacks(this.mScreenshotTimeoutRunnable);
            this.mHandler.postDelayed(this.mScreenshotTimeoutRunnable, SCREENSHOT_HANDLER_DELAY_MILLIS);
        }
    }

    public void triggerScreenshotChordVolumeUp(long time) {
        this.mScreenshotChordVolumeUpKeyTime = time;
        interceptScreenshotChord();
        postDelayScreenShotTimeOutRunnable();
    }

    private void processRotation(int rotation) {
        if (PhoneWindowManager.DEBUG_INPUT) {
            VSlog.v(TAG, "processRotation, rotation=" + rotation);
        }
    }

    public boolean getInterceptWhenAlarm() {
        return this.mInterceptWhenAlarm;
    }

    public void setInterceptWhenAlarm(boolean intercept) {
        this.mInterceptWhenAlarm = intercept;
    }

    public void notifyScreenOff() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.policy.VivoInputPolicy.9
            @Override // java.lang.Runnable
            public void run() {
                VLog.d(VivoInputPolicy.TAG, "sendPowerKeyScreenOffBroadcast.");
                VivoPolicyUtil.sendPowerKeyScreenOffBroadcast(VivoInputPolicy.this.mContext);
                if (VivoInputPolicy.this.mAlertDialog != null) {
                    VivoInputPolicy.this.mAlertDialog.dismiss();
                    VivoInputPolicy.this.mAlertDialog = null;
                }
            }
        });
    }

    public boolean isSupportForLongPressHome() {
        if (!this.mIsExport) {
            VLog.w(TAG, "Not support longpress home");
        }
        return this.mIsExport;
    }

    public boolean interceptHomeKey() {
        if (VivoPolicyUtil.isSPSMode()) {
            VivoPolicyUtil.launchSPS(this.mContext);
            VLog.d(TAG, "launchSPS.");
            return true;
        } else if (VivoPolicyUtil.isDrivingMode()) {
            VivoPolicyUtil.launchDrivingMode(this.mContext);
            VLog.d(TAG, "launchDriving.");
            return true;
        } else if (VivoPolicyUtil.isMotorMode(this.mContext)) {
            VivoPolicyUtil.launchMotorMode(this.mContext);
            VLog.d(TAG, "launchMotorMode.");
            return true;
        } else if (this.mIsFamilyCare) {
            VivoPolicyUtil.launchFamilyCareRemindActivity(this.mContext);
            VLog.d(TAG, "launchFamilyCareRemindActivity.");
            return true;
        } else {
            return false;
        }
    }

    public int interceptPowerKeyDown(KeyEvent event, boolean interactive, boolean hungUp, boolean screenshotChordVolumeDownKeyTriggered, boolean a11yShortcutChordVolumeUpKeyTriggered, boolean gesturedServiceIntercepted) {
        long now = SystemClock.uptimeMillis();
        boolean z = true;
        boolean maybeScreenShot = now <= this.mHomeKeyTime + VIVO_SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS;
        this.mPhoneWindowManager.mPowerKeyHandled = hungUp || screenshotChordVolumeDownKeyTriggered || a11yShortcutChordVolumeUpKeyTriggered || (this.mHomeKeyScreenshotPending && maybeScreenShot) || this.mIsInterceptByCall || (this.mHasMultiDisplay && this.mScreenshotChordAIKeyTriggered);
        VLog.d(TAG, "powerKeyHandled=" + this.mPhoneWindowManager.mPowerKeyHandled + ", hungUp=" + hungUp + ", volumeDownTriggered=" + screenshotChordVolumeDownKeyTriggered + ", volumeUpTriggered = " + a11yShortcutChordVolumeUpKeyTriggered + ", mHomeKeyPending = " + this.mHomeKeyScreenshotPending + ", maybeScreenShot = " + maybeScreenShot + ", interceptByCall = " + this.mIsInterceptByCall + ", mHasMultiDisplay= " + this.mHasMultiDisplay + ", mAIKeyTriggered = " + this.mScreenshotChordAIKeyTriggered);
        this.mPowerKeyHandledByJovi = false;
        if (!interactive || this.mPhoneWindowManager.isKeyguardLocked() || this.mPhoneWindowManager.mPowerKeyHandled) {
            z = false;
        }
        this.mShouldShowJoviGuide = z;
        if (!this.mHasMultiDisplay || !interactive || this.mPhoneWindowManager.mPowerKeyHandled || getFocusDisplayId() == 0) {
            if (!this.mPhoneWindowManager.mPowerKeyHandled) {
                if (this.mHasMultiDisplay && !interactive && getFocusDisplayId() != 0) {
                    requestFocusDisplay(0);
                }
                sendStartJoviVoiceMessage(26);
                return -100;
            }
            return -100;
        }
        return 0;
    }

    public boolean interceptPowerKeyUp(KeyEvent event, boolean interactive, boolean canceled) {
        boolean handled = canceled || this.mPhoneWindowManager.mPowerKeyHandled || this.mPowerKeyHandledByJovi;
        VLog.d(TAG, "interceptPowerKeyUp handled=" + handled);
        return handled;
    }

    public int interceptPowerKeyUpForMultiDisplay(boolean interactive) {
        if (this.mHasMultiDisplay) {
            boolean isAllDisplayInteractive = isAllDisplayInterative();
            int focusedDisplayId = getFocusDisplayId();
            VLog.d(TAG, "interceptPowerKeyUp mDisplayChangeAnimating = " + this.mDisplayChangeAnimating + " isAllDisplayInteractive = " + isAllDisplayInteractive + " focusedDisplayId = " + focusedDisplayId);
            if (interactive) {
                if ((isAllDisplayInteractive && this.mDisplayChangeAnimating && focusedDisplayId == 4096) || (!isAllDisplayInteractive && focusedDisplayId == 4096)) {
                    if (focusedDisplayId == 4096) {
                        long now = SystemClock.uptimeMillis();
                        if (now - this.mPhoneWindowManager.mScreenshotChordPowerKeyTime <= VIVO_SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
                            requestMultiDisplayState();
                        }
                    }
                    this.mPhoneWindowManager.finishPowerKeyPress();
                    VLog.d(TAG, "interceptPowerKeyUp return");
                    return 0;
                }
                return -100;
            }
            return -100;
        }
        return -100;
    }

    public int handleHomeButton(KeyInterceptionInfo keyInterceptionInfo) {
        if (keyInterceptionInfo != null) {
            if (!"vos".equals(FtBuild.getOsName())) {
                int typeCount = WINDOW_TYPES_WHERE_HOME_DOESNT_WORK.length;
                int type = keyInterceptionInfo.layoutParamsType;
                for (int i = 0; i < typeCount; i++) {
                    if (type == WINDOW_TYPES_WHERE_HOME_DOESNT_WORK[i]) {
                        VLog.i(TAG, "Do nothing, dropping home key event.");
                        return -1;
                    }
                }
            }
            int typeCount2 = keyInterceptionInfo.layoutParamsPrivateFlags;
            if ((typeCount2 & 536870912) != 0) {
                VLog.i(TAG, "Dispatching home key to app window:" + keyInterceptionInfo);
                return 0;
            }
            return 1;
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePowerKeyTriggerJoviVoiceSettings() {
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mPowerKeyTriggerJoviVoiceEnabled = Settings.System.getIntForUser(resolver, "vivo_jovi_power_wakeup_switch", 0, -2) == 1;
        this.mJoviGuideShown = Settings.System.getIntForUser(resolver, "vivo_jovi_power_wakeup_guide_shown", 0, -2) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class GameModeObserver extends ContentObserver {
        public GameModeObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = VivoInputPolicy.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(VivoAIKeyExtend.GAME_DISTURB_ENABLED), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor(VivoAIKeyExtend.GAME_CURRENT_PACKAGE), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("enabled_shield_bottom_button"), false, this, -1);
            VivoInputPolicy.this.updateGameSetting();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VivoInputPolicy.this.updateGameSetting();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class VivoPayObserver extends ContentObserver {
        public VivoPayObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = VivoInputPolicy.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("com_vivo_wallet_nfc_support_double_click"), false, this, -1);
            VivoInputPolicy.this.updateVivoPaySettings();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VivoInputPolicy.this.updateVivoPaySettings();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class PowerKeyTriggerJoviVoiceObserver extends ContentObserver {
        public PowerKeyTriggerJoviVoiceObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = VivoInputPolicy.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("vivo_jovi_power_wakeup_switch"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("vivo_jovi_power_wakeup_guide_shown"), false, this, -1);
            VivoInputPolicy.this.updatePowerKeyTriggerJoviVoiceSettings();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VivoInputPolicy.this.updatePowerKeyTriggerJoviVoiceSettings();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class NavBarLandScapePositionObserver extends ContentObserver {
        public NavBarLandScapePositionObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = VivoInputPolicy.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor("nav_bar_landscape_position"), false, this, -1);
            VivoInputPolicy.this.updateNavBarLandScapePosition();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VivoInputPolicy.this.updateNavBarLandScapePosition();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNavBarLandScapePosition() {
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean changed = Settings.Secure.getIntForUser(resolver, "nav_bar_landscape_position", 1, -2) == 1;
        if (this.mPhoneWindowManager.mDefaultDisplayPolicy != null) {
            this.mPhoneWindowManager.mDefaultDisplayPolicy.changeNavigationBarPosition(changed);
        }
        VLog.d(TAG, "update vivoChangeNavPosition = " + changed);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ComboKeyStatusObserver extends ContentObserver {
        public ComboKeyStatusObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = VivoInputPolicy.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("combo_keys_press_shot_switch"), false, this, -1);
            VivoInputPolicy.this.updateComboKeyStatus();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VivoInputPolicy.this.updateComboKeyStatus();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateComboKeyStatus() {
        this.mComboKeyScreenshotEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "combo_keys_press_shot_switch", 1, -2) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DisplayChangeAnimatingObserver extends ContentObserver {
        public DisplayChangeAnimatingObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = VivoInputPolicy.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("multidisplay_transition_showing"), false, this, -1);
            VivoInputPolicy.this.updateDisplayChangeAnimatingStatus();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VivoInputPolicy.this.updateDisplayChangeAnimatingStatus();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDisplayChangeAnimatingStatus() {
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mDisplayChangeAnimating = Settings.System.getIntForUser(resolver, "multidisplay_transition_showing", 0, -2) == 1;
        VLog.d(TAG, "update mDisplayChangeAnimating = " + this.mDisplayChangeAnimating);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class FlashLightObserver extends ContentObserver {
        public FlashLightObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = VivoInputPolicy.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("vivoFlash_state"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("FlashState"), false, this, -1);
            VivoInputPolicy.this.updateFlashLightStatus();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VivoInputPolicy.this.updateFlashLightStatus();
        }
    }

    private boolean hasNavigationBar() {
        return this.mPhoneWindowManager.mDefaultDisplayPolicy != null && this.mPhoneWindowManager.mDefaultDisplayPolicy.hasNavigationBar();
    }

    private void triggerDumpStacktraceToDebug() {
        if (SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes")) {
            VSlog.d(TAG, "start triggerDumpStacktraceToDebug.");
            SystemDefenceManager.getInstance().dumpStacktraceToDebug();
        }
    }

    private boolean isTopPowerKey(KeyEvent event) {
        if (this.mHasPressurePowerkey && event.getKeyCode() == 26 && event.getScanCode() == 699) {
            return true;
        }
        return false;
    }

    private void interceptTopPowerKeyDown(KeyEvent event, boolean interactive) {
        if (!this.mPhoneWindowManager.mPowerKeyWakeLock.isHeld()) {
            this.mPhoneWindowManager.mPowerKeyWakeLock.acquire(10000L);
        }
        if (!interactive) {
            this.mPhoneWindowManager.wakeUpFromPowerKey(event.getDownTime());
            this.mPhoneWindowManager.mBeganFromNonInteractive = true;
            VDC_KEY_J_1.VDC_Key_F_2(this.mContext, true, true);
        }
    }

    private void interceptTopPowerKeyUp(KeyEvent event, boolean interactive, boolean canceled) {
        if (!this.mTopPowerKeyHandled) {
            this.mPhoneWindowManager.powerPress(event.getEventTime(), interactive, 0);
            if (interactive && !this.mPhoneWindowManager.mBeganFromNonInteractive) {
                VDC_KEY_J_1.VDC_Key_F_2(this.mContext, true, false);
            }
            Message msg = this.mHandler.obtainMessage(102);
            msg.setAsynchronous(true);
            msg.sendToTarget();
        }
        this.mPhoneWindowManager.finishPowerKeyPress();
    }

    private boolean shouldDispatchToKeyGuide(KeyEvent event, String windowName, boolean interactive) {
        int keycode = event.getKeyCode();
        if (this.mHasPressurePowerkey && interactive && windowName != null && windowName.contains("com.vivo.capacitykeysettings.NewerGuideActivity")) {
            if (keycode == 26 || keycode == 25 || keycode == 24) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override // com.android.server.policy.IVivoAdjustmentPolicy
    public boolean isAIKeyTriggered() {
        long now = SystemClock.uptimeMillis();
        return this.mGlobalAiKeyTriggered && now - this.mGlobalAiKeyTriggeredTime < 60000;
    }

    @Override // com.android.server.policy.IVivoAdjustmentPolicy
    public boolean isRightSideKeyTriggered() {
        return this.mGlobalPowerKeyTriggered || this.mGlobalVolumeUpKeyTriggered || this.mGlobalVolumeDownKeyTriggered;
    }

    private boolean isRightSideKey(KeyEvent event) {
        int keycode = event.getKeyCode();
        if ((keycode == 26 && !isTopPowerKey(event)) || keycode == 24 || keycode == 25) {
            return true;
        }
        return false;
    }

    @Override // com.android.server.policy.IVivoAdjustmentPolicy
    public int getLastKeyCode() {
        return this.mLastKeyCode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MonsterModeObserver extends ContentObserver {
        public MonsterModeObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = VivoInputPolicy.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("monster_key_start"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("power_save_type"), false, this, -1);
            VivoInputPolicy.this.updateMonsterModeSettings();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VivoInputPolicy.this.updateMonsterModeSettings();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMonsterModeSettings() {
        this.mMonsterKeyEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "monster_key_start", 0, -2) == 1;
        this.mMonsterModeEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "power_save_type", 1, -2) == 5;
    }

    private void triggerMonsterMode() {
        VLog.d(TAG, "mMonsterAiKeyTriggered = " + this.mMonsterAiKeyTriggered + " mMonsterGamePadKeyTriggered = " + this.mMonsterGamePadKeyTriggered);
        if (this.mMonsterAiKeyTriggered && this.mMonsterGamePadKeyTriggered) {
            long now = SystemClock.uptimeMillis();
            boolean aiKeyLongPressed = now - this.mMonsterAiKeyDownTime > VIVO_SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS;
            this.mHandler.postDelayed(this.mTriggerMonsterModeRunnable, aiKeyLongPressed ? 1200L : 200L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void changeMonsterStatus() {
        if (!this.mPhoneWindowManager.isUserSetupComplete()) {
            VLog.d(TAG, "ignored change monster mode when user setup not complete");
        } else if (VivoPolicyUtil.isSPSMode()) {
            VLog.d(TAG, "ignore change monster mode when in super power save mode");
        } else {
            try {
                Intent intent = new Intent("intent.action.POWER_MODE_CHANGE_SERVICE");
                intent.setPackage("com.iqoo.powersaving");
                intent.putExtra("command", this.mMonsterModeEnabled ? 1013 : Constants.CMD.CMD_HIDL_OPEN_IR);
                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
                this.mIsAiKeyHandled = true;
                VLog.d(TAG, "changeMonsterStatus");
            } catch (Exception e) {
                VLog.d(TAG, "change monster mode cause exception: " + e.fillInStackTrace());
            }
        }
    }

    private boolean sosCountDownShown(WindowManagerPolicy.WindowState focusedWindow) {
        if (focusedWindow != null && "com.vivo.sos".equals(focusedWindow.getOwningPackage()) && focusedWindow.getAttrs() != null && focusedWindow.getAttrs().getTitle().toString().contains("com.vivo.sos.sosmode.SosCountdownActivity")) {
            return true;
        }
        return false;
    }

    private boolean hasMultiDisplay() {
        DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
        Display secondDisplay = displayManager.getDisplay(4096);
        return secondDisplay != null;
    }

    private void requestFocusDisplay(int mode) {
        if (this.mMultiDisplayManager != null) {
            VLog.d(TAG, "requestFocusedDisplay " + mode);
            this.mMultiDisplayManager.requestMultiDisplayState(mode);
        }
    }

    private int getFocusDisplayId() {
        MultiDisplayManager multiDisplayManager = this.mMultiDisplayManager;
        if (multiDisplayManager != null) {
            return multiDisplayManager.getFocusedDisplayId();
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void aiKeyLongPress() {
        this.mAIKeyHandled = true;
        boolean hapticsDisabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 0, -2) == 0;
        this.mPhoneWindowManager.performHapticFeedback(Process.myUid(), this.mContext.getOpPackageName(), 0, !hapticsDisabled, "AI - Long Press - Global Actions");
        this.mPhoneWindowManager.showGlobalActionsInternal();
    }

    private boolean isAllDisplayInterative() {
        DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
        Display secondDisplay = displayManager.getDisplay(4096);
        if (secondDisplay != null && secondDisplay.getState() == 2 && this.mPhoneWindowManager.mDefaultDisplay.getState() == 2) {
            return true;
        }
        return false;
    }

    private boolean isAlarmOn() {
        return "on".equals(SystemProperties.get("persist.vivo.clock.alarm_status", "off"));
    }

    private void snoozeAlarm() {
        Intent intent = new Intent(VivoPolicyConstant.ACTION_HW_KEY_ALARM_CHANGE);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFlashLightStatus() {
        int flash = Settings.System.getInt(this.mContext.getContentResolver(), "vivoFlash_state", 0);
        int light = Settings.System.getInt(this.mContext.getContentResolver(), "FlashState", 0);
        if (flash == 0 && light == 1) {
            this.mIsFlashLightOn = true;
        } else {
            this.mIsFlashLightOn = false;
        }
    }

    private void turnOffFlashLight() {
        Settings.System.putInt(this.mContext.getContentResolver(), "FlashState", 0);
    }

    private void sendShowRebootMenuBroadcast() {
        Intent intent = new Intent("com.vivo.intent.action.SHOW_REBOOT_MENU");
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean interceptAIkeyByCall() {
        if (getFocusDisplayId() == 4096) {
            boolean isPowerHangUp = SystemProperties.getBoolean("persist.radio.power_hang_up", false);
            TelecomManager telecomManager = getTelecommService();
            if (isPowerHangUp && telecomManager != null && telecomManager.isInCall()) {
                try {
                    VivoTelephonyApiParams param = new VivoTelephonyApiParams("API_TAG_triggerPowerHangUp");
                    VivoTelephonyApiParams ret = getITelecom().vivoTelephonyApi(param);
                    boolean isTrigger = ret.getAsBoolean("isTrigger").booleanValue();
                    VLog.d(TAG, "vivoTelephonyApi ret = " + isTrigger);
                    return isTrigger;
                } catch (RemoteException e) {
                    VLog.w(TAG, "vivoTelephonyApi failed " + e);
                }
            } else if (!isPowerHangUp && telecomManager != null && telecomManager.isRinging()) {
                try {
                    VivoTelephonyApiParams param2 = new VivoTelephonyApiParams("API_TAG_isTriggerSilence");
                    VivoTelephonyApiParams ret2 = getITelecom().vivoTelephonyApi(param2);
                    boolean isTriggerSilence = ret2.getAsBoolean("isTriggerSilence").booleanValue();
                    VLog.d(TAG, "API_TAG_isTriggerSilence ret = " + isTriggerSilence);
                    if (!isTriggerSilence) {
                        telecomManager.silenceRinger();
                        return true;
                    }
                } catch (RemoteException e2) {
                    VLog.w(TAG, "vivoTelephonyApi failed " + e2);
                }
            }
        }
        return false;
    }

    private void goToSleepByAiKey(KeyEvent event) {
        if (this.mPhoneWindowManager.mDefaultDisplayPolicy.isScreenOnEarly() && !this.mPhoneWindowManager.mDefaultDisplayPolicy.isScreenOnFully()) {
            VSlog.i(TAG, "Suppressed redundant power key press while already in the process of turning the screen on.");
        } else {
            this.mPhoneWindowManager.goToSleep(event.getEventTime(), 102, 0);
        }
    }

    private int interceptAiKey(KeyEvent event, boolean isScreenOn) {
        if (this.mHasMultiDisplay) {
            int keyCode = event.getKeyCode();
            boolean down = event.getAction() == 0;
            if (keyCode == 308) {
                WindowState focusedWindow = this.mPhoneWindowManager.mDefaultDisplayPolicy.getFocusedWindow();
                String wins = focusedWindow != null ? focusedWindow.toString() : " ";
                if (wins != null && (wins.contains("com.iqoo.engineermode.keycode") || (isScreenOn && wins.contains("com.vivo.bsptest")))) {
                    if (down && event.getRepeatCount() == 0) {
                        VLog.w(TAG, "interceptKeyBeforeQueueingForVivo skip KEYCODE_AI here.");
                        Intent intent = new Intent(ACTION_AI_KEY_AUTO_TEST);
                        this.mContext.sendBroadcast(intent);
                    }
                    return 0;
                } else if (VivoPolicyUtil.isSPSMode()) {
                    VLog.d(TAG, "SPSMode , ai key not work");
                    return 0;
                } else if (down) {
                    if (isAlarmOn() && isAlarmActive()) {
                        snoozeAlarm();
                        this.mAIKeyHandled = true;
                        VLog.d(TAG, "ai key snooze alarm");
                        return 0;
                    } else if (!this.mPhoneWindowManager.mScreenshotChordPowerKeyTriggered && interceptAIkeyByCall()) {
                        this.mAIKeyHandled = true;
                        VLog.d(TAG, "ai key intercept by call");
                        return 0;
                    } else if (isScreenOn) {
                        this.mAIKeyHandled = this.mPhoneWindowManager.mScreenshotChordPowerKeyTriggered;
                        VLog.d(TAG, "mAIKeyHandled=" + this.mAIKeyHandled + " ,mScreenshotChordPowerKeyTriggered = " + this.mPhoneWindowManager.mScreenshotChordPowerKeyTriggered);
                        this.mScreenshotChordAIKeyTriggered = true;
                        this.mScreenshotChordAIKeyTime = event.getDownTime();
                        interceptScreenshotChord();
                        if (!this.mAIKeyHandled && getFocusDisplayId() != 0) {
                            sendStartJoviVoiceMessage(308);
                            this.mHandler.postDelayed(this.mAiKeyLongPressRunnable, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                        }
                        sosCountByAIKey();
                        return 0;
                    } else if (this.mIsFlashLightOn) {
                        turnOffFlashLight();
                        VLog.d(TAG, "ai key turn off flash light");
                        this.mAIKeyHandled = true;
                        return 0;
                    } else {
                        requestFocusDisplay(1);
                        sendStartJoviVoiceMessage(308);
                        wakeUpFromAiKey(event.getDownTime());
                        this.mAIKeyHandled = true;
                        sosCountByAIKey();
                        return 0;
                    }
                } else {
                    this.mScreenshotChordAIKeyTriggered = false;
                    VLog.d(TAG, "mAIKeyHandled=" + this.mAIKeyHandled);
                    if (!this.mAIKeyHandled && isScreenOn) {
                        boolean isAllDisplayInteractive = isAllDisplayInterative();
                        int focusedDisplayId = getFocusDisplayId();
                        VLog.d(TAG, "ai key up isAllDisplayInteractive = " + isAllDisplayInteractive + " mDisplayChangeAnimating = " + this.mDisplayChangeAnimating + " focusedDisplayId = " + focusedDisplayId);
                        if ((isAllDisplayInteractive && (!this.mDisplayChangeAnimating || focusedDisplayId == 4096)) || (!isAllDisplayInteractive && focusedDisplayId == 4096)) {
                            goToSleepByAiKey(event);
                        } else if (focusedDisplayId == 0) {
                            long now = SystemClock.uptimeMillis();
                            if (now - this.mScreenshotChordAIKeyTime <= VIVO_SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
                                requestMultiDisplayState();
                            }
                        }
                    }
                    cancelPendingAIKeyAction();
                    return 0;
                }
            }
            return -100;
        }
        return -100;
    }

    private void wakeUpFromAiKey(long eventTime) {
        this.mPhoneWindowManager.wakeUp(eventTime, true, 1, "AiKey");
    }

    private void triggerSwitchAwake() {
        VLog.d(TAG, "triggerSwitchAwake");
        try {
            Intent intent = new Intent("com.vivo.backtoolbar.multidisplay.ROTATE_WITH_KEY");
            intent.setComponent(new ComponentName("com.vivo.backtoolbar", "com.vivo.backtoolbar.service.ToolbarService"));
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestMultiDisplayState() {
        VLog.d(TAG, "requestMultiDisplayState");
        try {
            Intent intent = new Intent("com.vivo.backtoolbar.multidisplay.TURN_ON_SCREEN");
            intent.setComponent(new ComponentName("com.vivo.backtoolbar", "com.vivo.backtoolbar.service.ToolbarService"));
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelPendingAIKeyAction() {
        synchronized (this.mLock) {
            IVivoKeyBeforeQueueingListener listener = this.mVivoWMPHook.getKeyBeforeQueueingListener().get(308);
            if (listener != null) {
                listener.cancelPendingKeyAction(308);
            }
        }
        this.mAIKeyHandled = true;
        this.mHandler.removeCallbacks(this.mStartJoviRunnable);
        this.mHandler.removeCallbacks(this.mAiKeyLongPressRunnable);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class StartJoviRunnable implements Runnable {
        int mFromKeyCode = 26;

        public StartJoviRunnable() {
        }

        public void setKeyCode(int keyCode) {
            this.mFromKeyCode = keyCode;
        }

        @Override // java.lang.Runnable
        public void run() {
            VivoInputPolicy.this.startJoviVoice(this.mFromKeyCode);
        }
    }

    public void systemReady() {
        ForceBackManager forceBackManager = this.mForceBackManager;
        if (forceBackManager != null) {
            forceBackManager.systemReady();
        }
        ThreeFingerConfigManager threeFingerConfigManager = this.mThreeFingerConfigManager;
        if (threeFingerConfigManager != null) {
            threeFingerConfigManager.systemReady();
        }
        VivoWMPHook vivoWMPHook = this.mVivoWMPHook;
        if (vivoWMPHook != null) {
            vivoWMPHook.systemReady();
        }
        AccessibilityWaterMark accessibilityWaterMark = this.mA11WaterMark;
        if (accessibilityWaterMark != null) {
            accessibilityWaterMark.onSystemReady();
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        ForceBackManager forceBackManager = this.mForceBackManager;
        if (forceBackManager != null) {
            forceBackManager.dump(prefix, pw);
        }
        ThreeFingerConfigManager threeFingerConfigManager = this.mThreeFingerConfigManager;
        if (threeFingerConfigManager != null) {
            threeFingerConfigManager.dump(prefix, pw);
        }
    }

    @Override // com.android.server.policy.IVivoAdjustmentPolicy
    public WindowState getFocusedWindow() {
        if (this.mHasMultiDisplay) {
            if (getFocusDisplayId() == 0) {
                return this.mPhoneWindowManager.mDefaultDisplayPolicy.getFocusedWindow();
            }
            return this.mPhoneWindowManager.mSecondDisplayPolicy.getFocusedWindow();
        }
        return this.mPhoneWindowManager.mDefaultDisplayPolicy.getFocusedWindow();
    }

    public boolean shouldDisablePilferPointers(String opPackageName) {
        return this.mThreeFingerConfigManager.shouldDisablePilferPointers(opPackageName, getFocusedWindow());
    }

    private void sosCountByAIKey() {
        if (this.mLastKeyCodeForSosAI != 308) {
            this.mSosCount = 0;
            VLog.d(TAG, "recet sos count because other key trigger");
        }
        if (this.mSosCount == 0) {
            this.mHandler.removeCallbacks(this.mResetSosCountRunnable);
            this.mHandler.postDelayed(this.mResetSosCountRunnable, POWER_INTERVAL_DELAY_FOR_SOS);
        }
        int i = this.mSosCount + 1;
        this.mSosCount = i;
        if (i == 5) {
            sosCall();
        }
    }

    private void sosCall() {
        if (!isUserSetupComplete()) {
            VLog.d(TAG, "Ignoring because device not setup.");
            return;
        }
        VLog.d(TAG, "notify sos call");
        try {
            VivoTelephonyApiParams args = new VivoTelephonyApiParams("API_TAG_SosModePlaceCall");
            getITelecom().vivoTelephonyApi(args);
        } catch (RemoteException e) {
            VLog.w(TAG, "call vivoTelephonyApi failed " + e);
        }
    }

    private boolean isUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    public void vibratorPro(int effectId) {
        try {
            Vibrator vibrator = (Vibrator) this.mContext.getSystemService("vibrator");
            Class clazz = vibrator.getClass();
            Method method = clazz.getDeclaredMethod("vibratorPro", Integer.TYPE, Long.TYPE, Integer.TYPE);
            if (method != null) {
                long playMillis = ((Long) method.invoke(vibrator, Integer.valueOf(effectId), -1, 1)).longValue();
                VLog.i(TAG, "vibratorPro effect will play millis:" + playMillis);
            }
        } catch (Exception e) {
            VLog.e(TAG, "Exception:", e);
        }
    }

    public void setInterceptInputKeyStatus(boolean enable) {
        if (this.mInterceptInputKey != enable) {
            this.mInterceptInputKey = enable;
            VSlog.d(TAG, "setInterceptInputKeyStatus =" + this.mInterceptInputKey);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class FamilyCareObserver extends ContentObserver {
        public FamilyCareObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = VivoInputPolicy.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor("family_care_remind_type"), false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VivoInputPolicy vivoInputPolicy = VivoInputPolicy.this;
            vivoInputPolicy.mIsFamilyCare = Settings.Secure.getInt(vivoInputPolicy.mContext.getContentResolver(), "family_care_remind_type", 0) == 1;
        }
    }
}