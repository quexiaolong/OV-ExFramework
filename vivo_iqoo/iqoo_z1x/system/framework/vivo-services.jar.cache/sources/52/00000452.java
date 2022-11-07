package com.android.server.policy.key;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.server.notification.VivoNotificationManagerServiceImpl;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.android.server.policy.IVivoAdjustmentPolicy;
import com.android.server.policy.VivoPolicyConstant;
import com.android.server.policy.WindowManagerPolicy;
import com.vivo.common.utils.VLog;
import com.vivo.services.superresolution.Constant;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class VivoGamepadKeyHandler extends AVivoInterceptKeyCallback {
    private static final boolean IS_SUPPORT_LRA;
    private static final boolean SUPPORT_DEEP_PRESS;
    private static final String TAG = "VivoGamepadKeyHandler";
    private Context mContext;
    private GameStatusObserver mGameStatusObserver;
    private PowerManager.WakeLock mGamepadKeyLock;
    private boolean mIsFbeProject;
    private boolean mIsSupportCamera;
    private int mPointerCount;
    private PowerManager mPowerManager;
    private UserManager mUserManager;
    private Vibrator mVibrator;
    private IVivoAdjustmentPolicy mVivoPolicy;
    private IWindowManager mWindowManager;
    private WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;
    private static int GAME_ZONE_TRIGGER_THREHOLD = 500;
    private static int GAME_ZONE_TRIGGER_PRESS_THREHOLD = 200;
    private static int CAMERA_TRIGGER_INTERVAL = 1000;
    private boolean mGamepadLeftDown = false;
    private boolean mGamepadRightDown = false;
    private long mGamepadLeftDownTime = 0;
    private long mGamepadRightDownTime = 0;
    private long mLastTriggerCameraTime = 0;
    private boolean mIsGameMode = false;
    private boolean mIsStartCamera = false;
    private Handler mHandler = new Handler();
    private Runnable mStartGameZoneRunnable = new Runnable() { // from class: com.android.server.policy.key.VivoGamepadKeyHandler.1
        @Override // java.lang.Runnable
        public void run() {
            VivoGamepadKeyHandler.this.startGameZone();
        }
    };
    private Runnable mStartCameraRunnable = new Runnable() { // from class: com.android.server.policy.key.VivoGamepadKeyHandler.3
        @Override // java.lang.Runnable
        public void run() {
            long now = SystemClock.uptimeMillis();
            if (now - VivoGamepadKeyHandler.this.mLastTriggerCameraTime >= VivoGamepadKeyHandler.CAMERA_TRIGGER_INTERVAL) {
                VivoGamepadKeyHandler.this.mLastTriggerCameraTime = now;
                boolean userKeyUnlocked = VivoGamepadKeyHandler.this.mUserManager.isUserUnlocked();
                VLog.d(VivoGamepadKeyHandler.TAG, "start camera. mIstFbeProject = " + VivoGamepadKeyHandler.this.mIsFbeProject + " userKeyUnlocked = " + userKeyUnlocked);
                if (!VivoGamepadKeyHandler.this.mIsFbeProject || userKeyUnlocked) {
                    VivoGamepadKeyHandler.this.mGamepadKeyLock.acquire(2000L);
                    try {
                        VivoGamepadKeyHandler.this.performHapticFeedback();
                        Intent intent = VivoGamepadKeyHandler.this.getCameraIntent(VivoGamepadKeyHandler.this.isKeyguardSecureLocked());
                        VivoGamepadKeyHandler.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                        return;
                    } catch (Exception e) {
                        VLog.e(VivoGamepadKeyHandler.TAG, "start camera activity cause exception:" + e);
                        return;
                    }
                }
                try {
                    PowerManager.WakeLock wakeLock = VivoGamepadKeyHandler.this.mPowerManager.newWakeLock(268435466, "GamepadKey");
                    wakeLock.acquire(5000L);
                    VivoGamepadKeyHandler.this.mWindowManager.dismissKeyguard((IKeyguardDismissCallback) null, (CharSequence) null);
                    return;
                } catch (RemoteException e2) {
                    VLog.d(VivoGamepadKeyHandler.TAG, "Fbe project an unlocked, dismiss keyguard cause exception: " + e2);
                    return;
                }
            }
            VLog.d(VivoGamepadKeyHandler.TAG, "prevent a short continuous trigger, drop it.");
        }
    };

    static {
        IS_SUPPORT_LRA = SystemProperties.getInt("persist.vivo.support.lra", 0) == 1;
        SUPPORT_DEEP_PRESS = SystemProperties.getInt("persist.vivo.support.deeppress", 0) == 1;
    }

    public VivoGamepadKeyHandler(Context context, IVivoAdjustmentPolicy vivoPolicy, IWindowManager windowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        this.mIsSupportCamera = false;
        this.mIsFbeProject = false;
        this.mContext = context;
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManager = powerManager;
        PowerManager.WakeLock newWakeLock = powerManager.newWakeLock(1, "VivoGamepadKeyHandler.mGamepadKeyLock");
        this.mGamepadKeyLock = newWakeLock;
        newWakeLock.setReferenceCounted(true);
        this.mWindowManager = windowManager;
        this.mVivoPolicy = vivoPolicy;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mIsSupportCamera = SystemProperties.getInt("ro.vivo.camera.pressgamekey", 0) == 1;
        this.mIsFbeProject = "file".equals(SystemProperties.get("ro.crypto.type", "unknow"));
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mVibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        if (SUPPORT_DEEP_PRESS) {
            this.mWindowManagerFuncs.registerPointerEventListener(new WindowManagerPolicyConstants.PointerEventListener() { // from class: com.android.server.policy.key.VivoGamepadKeyHandler.2
                public void onPointerEvent(MotionEvent motionEvent) {
                    VivoGamepadKeyHandler.this.mPointerCount = motionEvent.getPointerCount();
                }
            }, 0);
        }
        if (this.mIsSupportCamera) {
            GameStatusObserver gameStatusObserver = new GameStatusObserver(this.mHandler);
            this.mGameStatusObserver = gameStatusObserver;
            gameStatusObserver.register(this.mContext.getContentResolver());
            this.mGameStatusObserver.onChange(false);
        }
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        if (this.mIsScreenOn && this.mKeyInterceptionInfo != null && this.mKeyInterceptionInfo.receiveGamepadKey) {
            if (this.mState == 1) {
                VLog.d(TAG, "window: " + this.mKeyInterceptionInfo + " need to receive gamepad key.");
            }
            return true;
        } else if (this.mKeyInterceptionInfo != null && "com.android.camera".equals(this.mKeyInterceptionInfo.mOwningPackage) && this.mKeyInterceptionInfo.mLayoutTitle.contains("com.android.camera.CameraActivity") && this.mIsScreenOn && this.mState == 1) {
            VLog.d(TAG, "in camera, drop the event.");
            return true;
        } else {
            return false;
        }
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        if (this.mState == 1) {
            boolean isScreenLocked = isScreenLocked();
            if (event.getRepeatCount() == 0) {
                this.mGamepadKeyLock.acquire(GAME_ZONE_TRIGGER_PRESS_THREHOLD);
            }
            if (VivoPolicyConstant.KEYCODE_GAME_PAD_LEFT == keyCode) {
                if (event.getRepeatCount() == 0) {
                    this.mGamepadLeftDown = true;
                    this.mGamepadLeftDownTime = event.getDownTime();
                    if (this.mIsSupportCamera && (!this.mIsScreenOn || isScreenLocked)) {
                        triggerCamera();
                    } else {
                        triggerGameZone();
                    }
                }
            } else if (VivoPolicyConstant.KEYCODE_GAME_PAD_RIGHT == keyCode && event.getRepeatCount() == 0) {
                this.mGamepadRightDown = true;
                this.mGamepadRightDownTime = event.getDownTime();
                if (this.mIsSupportCamera && (!this.mIsScreenOn || isScreenLocked)) {
                    triggerCamera();
                } else {
                    triggerGameZone();
                }
            }
        }
        return -1;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        if (this.mState == 1) {
            if (this.mGamepadKeyLock.isHeld()) {
                this.mGamepadKeyLock.release();
            }
            if (VivoPolicyConstant.KEYCODE_GAME_PAD_LEFT == keyCode) {
                this.mGamepadLeftDown = false;
                this.mHandler.removeCallbacks(this.mStartGameZoneRunnable);
                this.mHandler.removeCallbacks(this.mStartCameraRunnable);
            } else if (VivoPolicyConstant.KEYCODE_GAME_PAD_RIGHT == keyCode) {
                this.mGamepadRightDown = false;
                this.mHandler.removeCallbacks(this.mStartGameZoneRunnable);
                this.mHandler.removeCallbacks(this.mStartCameraRunnable);
            }
        }
        return -1;
    }

    private void triggerGameZone() {
        VLog.d(TAG, "triggerGameZone mGamepadLeftDown = " + this.mGamepadLeftDown + " mGamepadRightDown = " + this.mGamepadRightDown + " mIsGameMode = " + this.mIsGameMode);
        long now = SystemClock.uptimeMillis();
        if (!this.mIsGameMode && this.mGamepadLeftDown && this.mGamepadRightDown) {
            int i = GAME_ZONE_TRIGGER_THREHOLD;
            if (now - this.mGamepadLeftDownTime <= i && now - this.mGamepadRightDownTime <= i) {
                if (this.mKeyInterceptionInfo != null && this.mKeyInterceptionInfo.mLayoutTitle.contains("com.ndt.sidekey.hwtest.core.HwTestActivity")) {
                    VLog.d(TAG, "triggerGameZone skip HwTestActivity");
                } else {
                    this.mHandler.postDelayed(this.mStartGameZoneRunnable, GAME_ZONE_TRIGGER_PRESS_THREHOLD);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startGameZone() {
        if (SUPPORT_DEEP_PRESS && (!inLauncher() || this.mPointerCount != 2)) {
            VLog.d(TAG, "KeyInterceptionInfo: " + this.mKeyInterceptionInfo + " current pointer count = " + this.mPointerCount);
            return;
        }
        Intent intent = new Intent(VivoAIKeyShortPress.ACTION);
        intent.setPackage("com.vivo.game");
        intent.putExtra("keyEvent", SUPPORT_DEEP_PRESS ? "double_finger_press" : "touch_key_up");
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
            VLog.d(TAG, "start game zone");
        } catch (Exception e) {
            VLog.d(TAG, "start game zone cause exception: " + e);
        }
    }

    /* loaded from: classes.dex */
    class GameStatusObserver extends ContentObserver {
        private final Uri mGameStatusUri;
        private final Uri mIsStartCameraUri;

        public GameStatusObserver(Handler handler) {
            super(handler);
            this.mGameStatusUri = Settings.System.getUriFor("is_game_mode");
            this.mIsStartCameraUri = Settings.System.getUriFor("cam_press_gameKey_open_when_screen_off");
        }

        public void register(ContentResolver contentObserver) {
            contentObserver.registerContentObserver(this.mGameStatusUri, false, this, -1);
            contentObserver.registerContentObserver(this.mIsStartCameraUri, false, this, -1);
        }

        public void unregister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            try {
                VivoGamepadKeyHandler.this.mIsGameMode = "1".equals(Settings.System.getStringForUser(VivoGamepadKeyHandler.this.mContext.getContentResolver(), "is_game_mode", -2));
                VivoGamepadKeyHandler.this.mIsStartCamera = Settings.System.getInt(VivoGamepadKeyHandler.this.mContext.getContentResolver(), "cam_press_gameKey_open_when_screen_off", 0) == 1;
            } catch (Exception e) {
                VLog.d(VivoGamepadKeyHandler.TAG, "read game status cause exception: " + e);
            }
        }
    }

    private void triggerCamera() {
        VLog.d(TAG, "triggerCamera mGamepadLeftDown = " + this.mGamepadLeftDown + " mGamepadRightDown = " + this.mGamepadRightDown + " mIsStartCamera = " + this.mIsStartCamera);
        long now = SystemClock.uptimeMillis();
        if (this.mIsStartCamera && this.mGamepadLeftDown && this.mGamepadRightDown) {
            int i = GAME_ZONE_TRIGGER_THREHOLD;
            if (now - this.mGamepadLeftDownTime <= i && now - this.mGamepadRightDownTime <= i) {
                this.mHandler.postDelayed(this.mStartCameraRunnable, GAME_ZONE_TRIGGER_PRESS_THREHOLD);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Intent getCameraIntent(boolean isSecureLocked) {
        Intent intent = new Intent();
        if (isSecureLocked) {
            intent.addFlags(270647296);
            intent.setAction(VivoCameraKeyHandler.ACTION_START_CAMERA_SECURE);
            intent.putExtra("presskey_open_application", 0);
        } else {
            intent.setAction("android.intent.action.MAIN");
            intent.addFlags(807419904);
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.setComponent(new ComponentName("com.android.camera", "com.android.camera.CameraActivity"));
            intent.putExtra("presskey_open_application", 1);
        }
        intent.putExtra("SlideToCamera", "wakeup");
        return intent;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isKeyguardSecureLocked() {
        KeyguardManager mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (mKeyguardManager.isKeyguardSecure() && mKeyguardManager.isKeyguardLocked()) {
            return true;
        }
        return false;
    }

    private boolean isScreenLocked() {
        KeyguardManager mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (mKeyguardManager.isKeyguardLocked()) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void performHapticFeedback() {
        AudioManager mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        if (mAudioManager.getRingerMode() == 0) {
            VLog.d(TAG, "in silent mode.");
        } else if (IS_SUPPORT_LRA) {
            Class clazz = this.mVibrator.getClass();
            try {
                Method method = clazz.getDeclaredMethod("vibratorPro", Integer.TYPE, Long.TYPE, Integer.TYPE);
                if (method != null) {
                    ((Long) method.invoke(this.mVibrator, 0, -1, -1)).longValue();
                }
            } catch (Exception e) {
                VLog.e(TAG, "vibrate cause exception:", e);
            }
        } else {
            this.mVivoPolicy.performHapticFeedback(0, true, true);
        }
    }

    private boolean inLauncher() {
        if (this.mKeyInterceptionInfo != null) {
            if (!"com.bbk.launcher2".equals(this.mKeyInterceptionInfo.mOwningPackage) || !this.mKeyInterceptionInfo.mLayoutTitle.contains(Constant.ACTIVITY_LAUNCHER)) {
                if (VivoNotificationManagerServiceImpl.PKG_LAUNCHER.equals(this.mKeyInterceptionInfo.mOwningPackage) && this.mKeyInterceptionInfo.mLayoutTitle.contains("com.android.launcher3.Launcher")) {
                    return true;
                }
                return false;
            }
            return true;
        }
        return false;
    }
}