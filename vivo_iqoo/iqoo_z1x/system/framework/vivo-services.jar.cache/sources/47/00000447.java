package com.android.server.policy.key;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.android.server.policy.VivoPolicyUtil;
import com.android.server.policy.VivoProximitySensorListener;
import com.android.server.policy.VivoWMPHook;

/* loaded from: classes.dex */
public final class VivoCameraKeyHandler extends AVivoInterceptKeyCallback implements VivoProximitySensorListener.Callbacks {
    public static final String ACTION_START_CAMERA = "android.media.action.VIVO_STILL_IMAGE_CAMERA";
    public static final String ACTION_START_CAMERA_SECURE = "android.media.action.VIVO_STILL_IMAGE_CAMERA_SECURE";
    private static final long BRIGHT_SCREEN_DELAY = 200;
    private static final boolean ENABLE_SENSOR_CTRL = false;
    public static final int FINISH_HANDLED = -101;
    public static final int FINISH_NOT_HANDLED = -102;
    public static final int FORWARD = -100;
    public static final String KEY_START_MODE = "START_MODE";
    private static final long SPLASH_ANIMATOR_TIMEOUT = 500;
    private static final long SPLASH_DISMISS_DELAY = 1000;
    private static final String TAG_CAMERA_KEY = "CAMERA_KEY";
    public static final String VALUE_DOUBLE_CLICK = "DOUBLE_CLICK";
    public static final String VALUE_LONG_PRESS = "LONG_PRESS";
    private static final long WAKE_LOCK_TIMEOUT = 5000;
    private Context mContext;
    private MessageHandler mMessageHandler;
    private PowerManager mPowerManager;
    private IWindowManager mWMS = null;
    private KeyguardManager mKeyguardManager = null;
    private WindowManager mWindowManager = null;
    private Vibrator mVibrator = null;
    private CameraSplashView mCameraSplashView = null;
    private PowerManager.WakeLock mBrightWakeLock = null;
    private VivoProximitySensorListener mProximitySensorListener = null;
    private boolean mNeedDiscard = false;
    private IntentFilter mIntentFilter = null;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.key.VivoCameraKeyHandler.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VivoCameraKeyHandler vivoCameraKeyHandler = VivoCameraKeyHandler.this;
            vivoCameraKeyHandler.printf("action=" + action);
            if (!"android.intent.action.SCREEN_ON".equals(action)) {
                "android.intent.action.SCREEN_OFF".equals(action);
            }
        }
    };

    public VivoCameraKeyHandler(Context context) {
        this.mContext = null;
        this.mPowerManager = null;
        this.mMessageHandler = null;
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mMessageHandler = new MessageHandler();
    }

    private void initBrightWakeLock() {
        if (this.mBrightWakeLock == null) {
            PowerManager.WakeLock newWakeLock = this.mPowerManager.newWakeLock(268435466, TAG_CAMERA_KEY);
            this.mBrightWakeLock = newWakeLock;
            newWakeLock.setReferenceCounted(false);
        }
    }

    private void initSensorListener() {
        if (this.mProximitySensorListener == null) {
            this.mProximitySensorListener = new VivoProximitySensorListener(this.mContext, this.mMessageHandler.getLooper(), this);
        }
    }

    public void registerReceiver() {
        if (this.mIntentFilter == null) {
            IntentFilter intentFilter = new IntentFilter();
            this.mIntentFilter = intentFilter;
            intentFilter.addAction("android.intent.action.SCREEN_ON");
            this.mIntentFilter.addAction("android.intent.action.SCREEN_OFF");
            this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
        }
    }

    public void unregisterReceiver() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        this.mIntentFilter = null;
    }

    public Vibrator getVibrator() {
        if (this.mVibrator == null) {
            this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        }
        return this.mVibrator;
    }

    public WindowManager getWindowManager() {
        if (this.mWindowManager == null) {
            this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        }
        return this.mWindowManager;
    }

    public IWindowManager getWMS() {
        if (this.mWMS == null) {
            this.mWMS = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        }
        return this.mWMS;
    }

    public KeyguardManager getKeyguardManager() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        }
        return this.mKeyguardManager;
    }

    public boolean isSuperPowerSaverMode() {
        return VivoPolicyUtil.isSPSMode();
    }

    public void startCameraKeyLongPressAction(MessageParam msgParam) {
        if (isSuperPowerSaverMode()) {
            printf("It's in super-power-saver mode, just discard it!");
            return;
        }
        printf("It's long press action. startCameraKeyLongPressAction");
        vibrate(msgParam);
        startCameraExt(VALUE_LONG_PRESS, msgParam);
    }

    public void startCameraKeyDoubleClickAction(MessageParam msgParam) {
        if (isSuperPowerSaverMode()) {
            printf("It's in super-power-saver mode, just discard it!");
            return;
        }
        printf("It's double click action. startCameraKeyDoubleClickAction");
        startCameraExt(VALUE_DOUBLE_CLICK, msgParam);
    }

    public void startCameraExt(String mode, MessageParam msgParam) {
        if (msgParam.mNeedWakeUp) {
            wakeUp();
        }
        startCamera(mode, msgParam);
        if (msgParam.mIsShowSplash) {
            showSplashView(msgParam);
        }
    }

    public void startCamera(String mode, MessageParam msgParam) {
        boolean isScreenOn = msgParam.mIsScreenOn;
        boolean isKeyguardActive = msgParam.mIsKeyguardActive;
        boolean isKeyguardSecure = false;
        try {
            IWindowManager wms = getWMS();
            if (wms != null) {
                isKeyguardSecure = wms.isKeyguardSecure(this.mContext.getUserId());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent();
        intent.addFlags(874512384);
        if (!isScreenOn || isKeyguardActive) {
            intent.addFlags(Dataspace.STANDARD_BT709);
        }
        intent.putExtra(KEY_START_MODE, mode);
        if (isKeyguardSecure && (isKeyguardActive || !isScreenOn)) {
            intent.setAction(ACTION_START_CAMERA_SECURE);
        } else {
            intent.setAction(ACTION_START_CAMERA);
        }
        try {
            this.mContext.startActivity(intent);
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public MessageParam newMessageParam() {
        return new MessageParam();
    }

    private View getCameraSplashView(MessageParam msgParam) {
        if (this.mCameraSplashView == null) {
            this.mCameraSplashView = new CameraSplashView(this, this.mContext);
        }
        this.mCameraSplashView.setMessageParam(msgParam);
        return this.mCameraSplashView;
    }

    private void showSplashView(MessageParam msgParam) {
        printf("showSplashView");
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1);
        layoutParams.type = 2015;
        layoutParams.flags = 525592;
        layoutParams.screenOrientation = 1;
        layoutParams.format = 1;
        layoutParams.inputFeatures |= 2;
        layoutParams.setTitle("CameraSplashView");
        if (ActivityManager.isHighEndGfx()) {
            layoutParams.flags |= Dataspace.TRANSFER_GAMMA2_2;
            layoutParams.privateFlags |= 2;
        }
        View cameraSplashView = getCameraSplashView(msgParam);
        WindowManager windowManager = getWindowManager();
        windowManager.addView(cameraSplashView, layoutParams);
        cameraSplashView.setVisibility(0);
        cameraSplashView.invalidate();
        postDismissSplashView();
    }

    private void postDismissSplashView() {
        this.mMessageHandler.postDelayed(new Runnable() { // from class: com.android.server.policy.key.VivoCameraKeyHandler.2
            @Override // java.lang.Runnable
            public void run() {
                VivoCameraKeyHandler.this.dismissSplashView();
            }
        }, 1000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dismissSplashView() {
        CameraSplashView cameraSplashView;
        if (this.mWindowManager == null || (cameraSplashView = this.mCameraSplashView) == null) {
            return;
        }
        ObjectAnimator alphAnimator = ObjectAnimator.ofFloat(cameraSplashView, "alpha", 1.0f, 0.1f);
        alphAnimator.setDuration(SPLASH_ANIMATOR_TIMEOUT);
        alphAnimator.addListener(new Animator.AnimatorListener() { // from class: com.android.server.policy.key.VivoCameraKeyHandler.3
            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator paramAnimator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator paramAnimator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator paramAnimator) {
                if (VivoCameraKeyHandler.this.mWindowManager != null && VivoCameraKeyHandler.this.mCameraSplashView != null) {
                    try {
                        VivoCameraKeyHandler.this.mWindowManager.removeViewImmediate(VivoCameraKeyHandler.this.mCameraSplashView);
                        VivoCameraKeyHandler.this.mCameraSplashView = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator paramAnimator) {
            }
        });
        alphAnimator.start();
    }

    private void wakeUp() {
    }

    private void vibrate(MessageParam msgParam) {
        View view = msgParam != null ? msgParam.mView : null;
        try {
            if (view != null) {
                boolean result = view.performHapticFeedback(0, 3);
                printf("performHapticFeedback: result=" + result);
            } else {
                getVibrator().vibrate(new long[]{1, 100}, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void postBrightScreen() {
        this.mMessageHandler.postDelayed(new Runnable() { // from class: com.android.server.policy.key.VivoCameraKeyHandler.4
            @Override // java.lang.Runnable
            public void run() {
                VivoCameraKeyHandler.this.acquireBrightLock();
            }
        }, BRIGHT_SCREEN_DELAY);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void acquireBrightLock() {
        printf("acquire mBrightWakeLock, timeout=5000");
        initBrightWakeLock();
        this.mBrightWakeLock.acquire(5000L);
    }

    private void releaseBrightLock() {
        if (this.mBrightWakeLock != null) {
            printf("release mBrightWakeLock");
            this.mBrightWakeLock.release();
        }
    }

    private void enableSensorListener() {
        initSensorListener();
        this.mProximitySensorListener.enable();
    }

    private void disableSensorListener() {
        VivoProximitySensorListener vivoProximitySensorListener = this.mProximitySensorListener;
        if (vivoProximitySensorListener != null) {
            vivoProximitySensorListener.disable();
        }
        this.mNeedDiscard = false;
    }

    @Override // com.android.server.policy.VivoProximitySensorListener.Callbacks
    public void onProximityPositive() {
        this.mNeedDiscard = true;
    }

    @Override // com.android.server.policy.VivoProximitySensorListener.Callbacks
    public void onProximityNegative() {
        this.mNeedDiscard = false;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        boolean isDrop = false;
        if (this.mIsScreenOn && !this.mIsKeyguardActive) {
            isDrop = true;
        }
        if (this.mNeedDiscard) {
            return true;
        }
        return isDrop;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback
    public boolean onCheckNeedWakeLockWhenScreenOff(int keyCode, KeyEvent event) {
        return true;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckDoubleClickEnabled(int keyCode, KeyEvent event) {
        return true;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public void onKeyLongPress(int keyCode, KeyEvent event) {
        MessageParam msgParam = new MessageParam();
        msgParam.mKeyEvent = event;
        msgParam.mIsKeyguardActive = this.mIsKeyguardActive;
        msgParam.mIsScreenOn = this.mIsScreenOn;
        msgParam.mIsShowSplash = false;
        msgParam.mNeedWakeUp = true;
        int cameraPhysicKeySupported = SystemProperties.getInt("persist.vivo.camera.physic_key", 0);
        printf("onKeyLongPress cameraPhysicKeySupported " + cameraPhysicKeySupported);
        if (cameraPhysicKeySupported == 1) {
            startCameraKeyLongPressAction(msgParam);
        }
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public void onKeyDoubleClick(int keyCode, KeyEvent event) {
        MessageParam msgParam = new MessageParam();
        msgParam.mKeyEvent = event;
        msgParam.mIsKeyguardActive = this.mIsKeyguardActive;
        msgParam.mIsScreenOn = this.mIsScreenOn;
        msgParam.mIsShowSplash = false;
        msgParam.mNeedWakeUp = true;
        int cameraPhysicKeySupported = SystemProperties.getInt("persist.vivo.camera.physic_key", 0);
        printf("onKeyDoubleClick cameraPhysicKeySupported " + cameraPhysicKeySupported);
        if (cameraPhysicKeySupported == 1) {
            startCameraKeyDoubleClickAction(msgParam);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void printf(String msg) {
        VivoWMPHook.printf(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MessageHandler extends Handler {
        private MessageHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
        }
    }

    /* loaded from: classes.dex */
    public final class MessageParam {
        public KeyEvent mKeyEvent = null;
        public View mView = null;
        public boolean mIsKeyguardActive = false;
        public boolean mIsScreenOn = true;
        public boolean mIsShowSplash = false;
        public boolean mNeedWakeUp = false;

        public MessageParam() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class CameraSplashView extends FrameLayout {
        public MessageParam mMsgParam;

        public CameraSplashView(VivoCameraKeyHandler vivoCameraKeyHandler, Context context) {
            this(vivoCameraKeyHandler, context, null);
        }

        public CameraSplashView(VivoCameraKeyHandler vivoCameraKeyHandler, Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public CameraSplashView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            this.mMsgParam = null;
            setBackgroundColor(-16777216);
        }

        public void setMessageParam(MessageParam msgParam) {
            this.mMsgParam = msgParam;
        }

        @Override // android.view.ViewGroup, android.view.View
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
        }

        @Override // android.view.ViewGroup, android.view.View
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
        }
    }
}