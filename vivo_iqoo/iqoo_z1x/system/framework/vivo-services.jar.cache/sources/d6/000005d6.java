package com.vivo.face.common.wake;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.VivoDisplayModule;
import android.hardware.display.VivoDisplayStateInternal;
import android.hardware.display.VivoDisplayStateManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import com.android.server.LocalServices;
import com.android.server.display.VivoDisplayModuleController;
import com.vivo.face.common.data.Config;
import com.vivo.face.common.data.Constants;
import com.vivo.face.common.utils.FaceLog;
import com.vivo.face.common.utils.SettingUtils;

/* loaded from: classes.dex */
public final class FaceWakeController {
    private static final int DELAY_FOR_RETRY = 200;
    private static final int DELAY_FOR_TIMEOUT = 4000;
    private static final int MSG_AUTH_FAILED = 1005;
    private static final int MSG_AUTH_SUCCEED = 1004;
    private static final int MSG_KEYGUARD_HIDED = 1006;
    private static final int MSG_START_WAKEUP = 1000;
    private static final int MSG_STOP_WAKEUP = 1001;
    private static final int MSG_WAKEUP_INTERCEPT = 1002;
    private static final int MSG_WAKEUP_TIMEOUT = 1003;
    private static final String TAG = "WakeController";
    private static final String WAKE_REASON_FACE_KEY = "Face";
    private static final String WAKE_REASON_FACE_LOGOUT = "Logout";
    private static boolean mScreenOffUnlockEnabled;
    private static boolean mUnlockEnabled;
    private static boolean mUnlockKeyguardKeepEnabled;
    private Handler mAsyncHandler;
    private ContentObserver mContentObserver;
    private ContentResolver mContentResolver;
    private Context mContext;
    private boolean mDialogVisible;
    private VivoDisplayStateInternal mDisplayStateInternal;
    private VivoDisplayStateManager mDisplayStateManager;
    private boolean mFaceWakeProcessing;
    private Handler mHandler;
    private boolean mHasFaceWaked;
    private PowerManager mPowerManager;
    private FaceWakeCallback mWakeCallback;
    private PowerManager.WakeLock mWakeLock;
    private boolean mWakeUpFinished;
    private boolean mWakeUpFinishedByFace;
    public static final boolean DIALOG_ENABLED = Config.isPD1821();
    private static FaceWakeController sInstance = new FaceWakeController();
    private int mDisplayBacklight = -1;
    private int mDisplayState = 0;
    private int mKeyguardState = 0;
    private boolean mScreenOn = true;

    private FaceWakeController() {
        HandlerThread thread = new HandlerThread("FaceWakeAsync", -4);
        thread.start();
        this.mAsyncHandler = new Handler(thread.getLooper());
    }

    public static FaceWakeController getInstance() {
        return sInstance;
    }

    public void init(Context context, Handler handler) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mHandler = new WakeHandler(handler.getLooper());
        initSetting(handler);
    }

    private void initSetting(Handler handler) {
        if (!DIALOG_ENABLED) {
            return;
        }
        int state = SettingUtils.getSecureSettingInt(this.mContext, Constants.Setting.FACE_UNLOCK_KEYGUARD_ENABLED, 0);
        mUnlockEnabled = 1 == state;
        int state2 = SettingUtils.getSecureSettingInt(this.mContext, Constants.Setting.FACE_UNLOCK_SCREEN_OFF, 0);
        mScreenOffUnlockEnabled = 1 == state2;
        int state3 = SettingUtils.getSecureSettingInt(this.mContext, "faceunlock_keyguard_keep", 0);
        mUnlockKeyguardKeepEnabled = 1 == state3;
        this.mContentObserver = new SettingObserver(handler);
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mContentResolver = contentResolver;
        contentResolver.registerContentObserver(Constants.URI.URI_UNLOCK_KEYGUARD_ENABLED, true, this.mContentObserver);
        this.mContentResolver.registerContentObserver(Constants.URI.URI_UNLOCK_SCREEN_OFF_ENABLED, true, this.mContentObserver);
        this.mContentResolver.registerContentObserver(Constants.URI.URI_UNLOCK_KEYGUARD_KEEP_ENABLED, true, this.mContentObserver);
    }

    private static boolean dialogEnabled() {
        return DIALOG_ENABLED && mUnlockEnabled && mScreenOffUnlockEnabled;
    }

    public void setWakeCallback(FaceWakeCallback callback) {
        this.mWakeCallback = callback;
    }

    public static boolean isFaceKey(String key) {
        if (!dialogEnabled()) {
            return false;
        }
        return TextUtils.equals(WAKE_REASON_FACE_KEY, key);
    }

    public void setDisplayBacklight(int backlight) {
        this.mDisplayBacklight = backlight;
    }

    public void setDisplayState(int state) {
        this.mDisplayState = state;
    }

    private boolean isDisplayOn() {
        return this.mDisplayState == 2;
    }

    private boolean isScreenOff() {
        if (this.mWakeCallback == null) {
            return false;
        }
        return !this.mScreenOn;
    }

    private void acquireWakeLock() {
        PowerManager.WakeLock wakeLock = this.mWakeLock;
        if (wakeLock != null && wakeLock.isHeld()) {
            FaceLog.d(TAG, "wakelock already acquired");
            return;
        }
        FaceLog.d(TAG, "acquire wakelock");
        PowerManager.WakeLock newWakeLock = this.mPowerManager.newWakeLock(1, "FaceWakeLock");
        this.mWakeLock = newWakeLock;
        newWakeLock.acquire();
    }

    private void releaseWakeLock() {
        PowerManager.WakeLock wakeLock = this.mWakeLock;
        if (wakeLock == null || !wakeLock.isHeld()) {
            FaceLog.d(TAG, "wakelock already released");
            return;
        }
        FaceLog.d(TAG, "release wakelock");
        this.mWakeLock.release();
        this.mWakeLock = null;
    }

    public void onAuthenticationResult(boolean succeed) {
        if (!dialogEnabled()) {
            return;
        }
        if (this.mDisplayBacklight > 0) {
            FaceLog.d(TAG, "backlight is turned on");
        } else if (succeed) {
            this.mHandler.obtainMessage(1004).sendToTarget();
        } else {
            this.mHandler.obtainMessage(1005).sendToTarget();
        }
    }

    public void onDialogVisibleStateChanged(boolean visible) {
        if (!dialogEnabled()) {
            return;
        }
        FaceLog.d(TAG, "dialog visible state changed " + this.mDialogVisible + ":" + visible);
        this.mDialogVisible = visible;
    }

    public void onWakeUpByWho(long eventTime, String who, int uid, long ident) {
        if (!dialogEnabled()) {
            FaceLog.d(TAG, "dialog not enabled");
        } else if (TextUtils.equals(WAKE_REASON_FACE_KEY, who)) {
            if (!this.mDialogVisible) {
                FaceLog.d(TAG, "icon not visible");
            } else {
                onWakeUpByFace();
            }
        } else {
            onWakeUpByOthers(who);
        }
    }

    private void onWakeUpByFace() {
        if (this.mWakeUpFinished) {
            FaceLog.d(TAG, "display is already waking up");
        } else if (!isScreenOff()) {
            FaceLog.d(TAG, "screen is turned on");
        } else {
            this.mFaceWakeProcessing = true;
            this.mHandler.obtainMessage(1000).sendToTarget();
        }
    }

    private void onWakeUpByOthers(String who) {
        this.mFaceWakeProcessing = false;
        if (this.mWakeUpFinishedByFace) {
            FaceLog.w(TAG, "face wakeup is running");
        } else {
            this.mHandler.obtainMessage(1002, who).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void wakeUpIntercept(String who) {
        this.mWakeUpFinishedByFace = false;
        if (this.mHandler.hasMessages(1003)) {
            this.mHandler.removeMessages(1003);
        }
        if (this.mHasFaceWaked) {
            postShowKeyguardAndWait();
        }
        if (TextUtils.equals(WAKE_REASON_FACE_LOGOUT, who)) {
            this.mWakeUpFinished = true;
        }
        setHasFaceWaked(false);
        this.mHandler.removeMessages(1000);
        releaseWakeLock();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startWakeUp() {
        if (!isScreenOff()) {
            FaceLog.w(TAG, "screen is turned on");
            return;
        }
        FaceLog.d(TAG, "start wakeup");
        setHasFaceWaked(true);
        acquireWakeLock();
        postHideKeyguard();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopWakeUp() {
        FaceLog.d(TAG, "stop wakeup");
        this.mFaceWakeProcessing = false;
        postShowKeyguardAndWait();
        clearWakeState();
        releaseWakeLock();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAuthenticationResult(boolean succeed) {
        StringBuilder sb = new StringBuilder();
        sb.append("authResult: ");
        sb.append(succeed ? "succeed" : "failed");
        sb.append(" hasFaceWaked: ");
        sb.append(this.mHasFaceWaked);
        sb.append(" wakeUpFinished: ");
        sb.append(this.mWakeUpFinished);
        FaceLog.d(TAG, sb.toString());
        this.mHandler.removeMessages(1003);
        if (!succeed) {
            if (this.mHasFaceWaked && !this.mWakeUpFinished) {
                this.mHandler.sendEmptyMessageDelayed(1003, 200L);
            } else {
                this.mFaceWakeProcessing = false;
                releaseWakeLock();
            }
        } else {
            this.mFaceWakeProcessing = false;
            if (this.mHasFaceWaked) {
                onAuthenticationSucceed();
            } else {
                onAuthenticationSucceedWithoutWakeUp();
            }
            releaseWakeLock();
        }
        this.mHandler.removeMessages(1000);
    }

    private void onAuthenticationSucceed() {
        FaceLog.d(TAG, "authentication succeed");
        this.mWakeUpFinished = true;
        this.mWakeUpFinishedByFace = true;
        boolean waitDisplayReady = false;
        if (!isScreenOff()) {
            FaceLog.d(TAG, "keyguard should not be hidden");
            postShowKeyguardAndWait();
            clearWakeState();
            this.mWakeUpFinishedByFace = false;
        } else {
            if (this.mKeyguardState != 1) {
                waitDisplayReady();
                waitDisplayReady = true;
            }
            removeDisplayOverlay();
        }
        wakeUp();
        if (!waitDisplayReady) {
            onKeyguardHide();
        }
    }

    private void onAuthenticationSucceedWithoutWakeUp() {
        FaceLog.d(TAG, "authentication succeed without wakeup");
        boolean needWakeup = false;
        boolean multiNeedWakeup = DIALOG_ENABLED && !isDisplayOn();
        if (isScreenOff() || this.mWakeUpFinished || multiNeedWakeup) {
            if (isScreenOff() || multiNeedWakeup) {
                postHideKeyguardAndWait();
                wakeUp();
            } else {
                needWakeup = true;
            }
        }
        clearWakeState();
        if (needWakeup) {
            this.mWakeUpFinished = true;
            this.mWakeUpFinishedByFace = true;
            wakeUp();
        }
    }

    private void setHasFaceWaked(boolean has) {
        FaceLog.d(TAG, "set hasFaceWaked " + has);
        this.mHasFaceWaked = has;
        this.mHandler.removeMessages(1003);
        if (has) {
            this.mHandler.sendEmptyMessageDelayed(1003, 4000L);
        }
    }

    private void clearWakeState() {
        FaceLog.d(TAG, "clear wake state");
        setHasFaceWaked(false);
        this.mKeyguardState = 0;
    }

    private void postHideKeyguard() {
        if (mUnlockKeyguardKeepEnabled) {
            FaceLog.d(TAG, "keyguard keep enabled");
            return;
        }
        FaceLog.d(TAG, "keyguard will be hidden");
        this.mAsyncHandler.post(new Runnable() { // from class: com.vivo.face.common.wake.FaceWakeController.1
            @Override // java.lang.Runnable
            public void run() {
                FaceWakeController.this.hideKeyguard();
            }
        });
    }

    private void postHideKeyguardAndWait() {
        if (mUnlockKeyguardKeepEnabled) {
            FaceLog.d(TAG, "keyguard keep enabled");
        } else {
            this.mAsyncHandler.runWithScissors(new Runnable() { // from class: com.vivo.face.common.wake.FaceWakeController.2
                @Override // java.lang.Runnable
                public void run() {
                    FaceWakeController.this.hideKeyguard();
                }
            }, 1000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideKeyguard() {
        int i = this.mKeyguardState;
        if (i == 3 || i == 1) {
            FaceLog.w(TAG, "keyguard is hidden");
            return;
        }
        FaceLog.d(TAG, "hide keyguard");
        this.mKeyguardState = 3;
        this.mWakeCallback.hideKeyguard(1);
        this.mKeyguardState = 1;
    }

    private void postShowKeyguardAndWait() {
        if (mUnlockKeyguardKeepEnabled) {
            FaceLog.d(TAG, "keyguard keep enabled");
            return;
        }
        FaceLog.d(TAG, "keyguard will be shown");
        this.mAsyncHandler.runWithScissors(new Runnable() { // from class: com.vivo.face.common.wake.FaceWakeController.3
            @Override // java.lang.Runnable
            public void run() {
                FaceWakeController.this.showKeyguard();
            }
        }, 1000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showKeyguard() {
        if (this.mKeyguardState == 2) {
            FaceLog.w(TAG, "keyguard is shown");
            return;
        }
        FaceLog.d(TAG, "show keyguard");
        this.mKeyguardState = 2;
        this.mWakeCallback.hideKeyguard(2);
    }

    public void setScreenOn(boolean on) {
        if (!dialogEnabled()) {
            FaceLog.d(TAG, "dialog not enabled");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("screen turned ");
        sb.append(on ? "on" : "off");
        FaceLog.d(TAG, sb.toString());
        this.mScreenOn = on;
        if (on && this.mFaceWakeProcessing) {
            this.mHandler.obtainMessage(1002, WAKE_REASON_FACE_LOGOUT).sendToTarget();
        }
    }

    public void onWakeUpFinished() {
        if (!dialogEnabled()) {
            FaceLog.d(TAG, "dialog not enabled");
            return;
        }
        this.mWakeUpFinished = false;
        this.mWakeUpFinishedByFace = false;
    }

    private void wakeUp() {
        FaceLog.d(TAG, "wakeup");
        this.mWakeCallback.wakeUp();
    }

    private void waitDisplayReady() {
        FaceLog.d(TAG, "wait display ready");
        this.mAsyncHandler.post(new Runnable() { // from class: com.vivo.face.common.wake.FaceWakeController.4
            @Override // java.lang.Runnable
            public void run() {
                FaceLog.d(FaceWakeController.TAG, "display ready");
                FaceWakeController.this.mHandler.obtainMessage(1006).sendToTarget();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onKeyguardHide() {
        FaceLog.d(TAG, "update display");
        clearWakeState();
        this.mHandler.removeMessages(1000);
    }

    private void removeDisplayOverlay() {
        if (this.mDisplayStateManager == null) {
            this.mDisplayStateManager = (VivoDisplayStateManager) this.mContext.getSystemService("vivo_display_state");
        }
        if (this.mDisplayStateInternal == null) {
            this.mDisplayStateInternal = (VivoDisplayStateInternal) LocalServices.getService(VivoDisplayStateInternal.class);
        }
        VivoDisplayModule vivoDisplayModule = this.mDisplayStateManager.getVivoDisplayModuleInfo(4096, -1, VivoDisplayModuleController.VivoDisplayModuleConfig.STR_MODULE_FACEDETECT);
        if (vivoDisplayModule != null) {
            this.mDisplayStateInternal.requestOverlayDismiss(4096, vivoDisplayModule.getModuleId());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SettingObserver extends ContentObserver {
        public SettingObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Constants.URI.URI_UNLOCK_KEYGUARD_ENABLED)) {
                int state = SettingUtils.getSecureSettingInt(FaceWakeController.this.mContext, Constants.Setting.FACE_UNLOCK_KEYGUARD_ENABLED, 0);
                boolean unused = FaceWakeController.mUnlockEnabled = 1 == state;
                StringBuilder sb = new StringBuilder();
                sb.append("keyguard unlock ");
                sb.append(FaceWakeController.mUnlockEnabled ? "enabled" : "disabled");
                FaceLog.d(FaceWakeController.TAG, sb.toString());
            } else if (uri.equals(Constants.URI.URI_UNLOCK_SCREEN_OFF_ENABLED)) {
                int state2 = SettingUtils.getSecureSettingInt(FaceWakeController.this.mContext, Constants.Setting.FACE_UNLOCK_SCREEN_OFF, 0);
                boolean unused2 = FaceWakeController.mScreenOffUnlockEnabled = 1 == state2;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("screen off keyguard unlock ");
                sb2.append(FaceWakeController.mScreenOffUnlockEnabled ? "enabled" : "disabled");
                FaceLog.d(FaceWakeController.TAG, sb2.toString());
            } else if (uri.equals(Constants.URI.URI_UNLOCK_KEYGUARD_KEEP_ENABLED)) {
                int state3 = SettingUtils.getSecureSettingInt(FaceWakeController.this.mContext, "faceunlock_keyguard_keep", 0);
                boolean unused3 = FaceWakeController.mUnlockKeyguardKeepEnabled = 1 == state3;
                StringBuilder sb3 = new StringBuilder();
                sb3.append("unlock keyguard keep ");
                sb3.append(FaceWakeController.mUnlockKeyguardKeepEnabled ? "enabled" : "disabled");
                FaceLog.d(FaceWakeController.TAG, sb3.toString());
            }
        }
    }

    /* loaded from: classes.dex */
    private class WakeHandler extends Handler {
        public WakeHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1000:
                    FaceLog.d(FaceWakeController.TAG, "MSG_START_WAKEUP");
                    FaceWakeController.this.startWakeUp();
                    return;
                case 1001:
                    FaceLog.d(FaceWakeController.TAG, "MSG_STOP_WAKEUP");
                    FaceWakeController.this.stopWakeUp();
                    return;
                case 1002:
                    FaceLog.d(FaceWakeController.TAG, "MSG_WAKEUP_INTERCEPT " + ((String) msg.obj));
                    FaceWakeController.this.wakeUpIntercept((String) msg.obj);
                    return;
                case 1003:
                    FaceLog.d(FaceWakeController.TAG, "MSG_WAKEUP_TIMEOUT");
                    FaceWakeController.this.stopWakeUp();
                    return;
                case 1004:
                    FaceLog.d(FaceWakeController.TAG, "MSG_AUTH_SUCCEED");
                    FaceWakeController.this.handleAuthenticationResult(true);
                    return;
                case 1005:
                    FaceLog.d(FaceWakeController.TAG, "MSG_AUTH_FAILED");
                    FaceWakeController.this.handleAuthenticationResult(false);
                    return;
                case 1006:
                    FaceLog.d(FaceWakeController.TAG, "MSG_KEYGUARD_HIDED");
                    FaceWakeController.this.onKeyguardHide();
                    return;
                default:
                    return;
            }
        }
    }
}