package com.android.server.policy.key;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.media.AudioManager;
import android.multidisplay.MultiDisplayManager;
import android.net.Uri;
import android.os.FtBuild;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.view.Display;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.WindowManager;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.server.am.EmergencyBroadcastManager;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.android.server.policy.IVivoAdjustmentPolicy;
import com.android.server.policy.IVivoWindowListener;
import com.android.server.policy.key.VivoSmartwakeCharContainer;
import com.vivo.common.utils.VLog;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/* loaded from: classes.dex */
public final class VivoSmartwakeKeyHandler extends AVivoInterceptKeyCallback implements IVivoWindowListener {
    private static final String ACTION_SMART_WAKE_DISMISS = "com.vivo.smartwake.dismiss";
    private static final String ACTION_SUPER_POWER_SAVE_MODE = "intent.action.super_power_save_send";
    private static final String BBK_SCREEN_DISABLE_CARD_SLIDE_SETTING = "bbk_screen_disable_card_slide_setting";
    private static final String CHILED_ENABLE = "vivo_children_mode_enable";
    private static final String EXTRA_USER_PRESENT_DELAY = "user_present_delay";
    public static final int FORWARD = -100;
    public static final int HANDLED = 0;
    public static final int HANDLED_PASS_TO_USER = 1;
    private static final String INPUT_DEVICE_NAME_PRIMARY = "vivo_ts";
    private static final String INPUT_DEVICE_NAME_SECONDARY = "vivo_ts_second";
    private static final String KEY_SUPER_POWER_SAVE = "sps_action";
    private static final int LOW_BATTERY_MODE_CAPACITY = 10;
    private static final int MSG_START_ALPHA_ANIMATION = 9;
    private static final int MSG_VSW_CAMERA_VIEW_DISABLE = 12;
    private static final int MSG_VSW_DISPLAY_SCREEN_WAKE_UP = 10;
    private static final int MSG_VSW_MEDIA_WAKELOCK = 5;
    private static final int MSG_VSW_VIEW_DISABLE = 4;
    private static final int MSG_VSW_VIEW_ENABLE = 1;
    private static final int MSG_VSW_VIEW_LIGHT = 2;
    private static final int MSG_VSW_VIEW_START = 3;
    private static final int MSG_VSW_VIEW_START_TIMEOUT = 11;
    private static final String PKG_BACK_PIC = "com.vivo.lockscreen.play";
    private static final String PKG_BROWSER = "com.vivo.browser";
    private static final String PKG_CAMERA = "com.android.camera";
    private static final String PKG_CARD_PACKAGE = "com.vivo.upslide";
    private static final String PKG_DIALER = "com.android.dialer";
    private static final String PKG_EMAIL = "com.vivo.email";
    private static final String PKG_FACEBOOK = "com.facebook.katana";
    private static final String PKG_GOOGLE_DIALER = "com.google.android.dialer";
    private static final String PKG_MOBILEQQ = "com.tencent.mobileqq";
    private static final String PKG_PAIR_SCRAWL = "com.vivo.scrawl.pair";
    private static final String PKG_WECHAT = "com.tencent.mm";
    private static final String PKG_WHATSAPP = "com.whatsapp";
    static final String PROP_DISMISS = "sys.smartwake.dismiss";
    private static final float PROXIMITY_THRESHOLD = 5.0f;
    private static final int SENSOR_CHANGED_COUNT = 3;
    private static final String START_SERVICE_ACTION = "intent.action.custom.gesture";
    static final String TAG = "smartwake ";
    private static final String VALUE_SUPER_POWER_SAVE_VALUE_ENTER = "entered";
    private static final String VALUE_SUPER_POWER_SAVE_VALUE_EXIT = "exited";
    static final String VCD_EID_DCLICK = "1008";
    static final String VCD_EID_MOOD_TRANSMIT = "518";
    static final String VCD_EID_SMARTWAKE = "1006";
    static final String VCD_ELABEL_A = "100621";
    static final String VCD_ELABEL_C = "10064";
    static final String VCD_ELABEL_DCLICK = "10082";
    static final String VCD_ELABEL_DOWN = "10063";
    static final String VCD_ELABEL_E = "10067";
    static final String VCD_ELABEL_F = "100622";
    static final String VCD_ELABEL_H = "5185";
    static final String VCD_ELABEL_LEFT = "10066";
    static final String VCD_ELABEL_M = "10065";
    static final String VCD_ELABEL_O = "10068";
    static final String VCD_ELABEL_RIGHT = "10066";
    static final String VCD_ELABEL_UP = "10062";
    static final String VCD_ELABEL_V = "5187";
    static final String VCD_ELABEL_W = "10069";
    private static final int WORK_SCREEN_BOTH = 2;
    private static final int WORK_SCREEN_PRIMARY_ONLY = 0;
    private static final int WORK_SCREEN_SECONDARY_ONLY = 1;
    private Sensor mACCSensor;
    private AudioManager mAudioManager;
    private Context mContext;
    private DisplayManager mDisplayManager;
    private KeyguardManager mKeyguardManager;
    private MultiDisplayManager mMultiDisplayManager;
    private Intent mPendingIntent;
    private PowerManager mPowerManager;
    private Sensor mProximitySensorPrimary;
    private Sensor mProximitySensorSecondary;
    private SensorManager mSensorManager;
    private VivoSmartwakeView mSmartWakeView;
    private TelecomManager mTelecomManager;
    private IVivoAdjustmentPolicy mVivoPolicy;
    private IWindowManager mWMS;
    private PowerManager.WakeLock mWakeLock;
    private Method mWakeUpByDoubleTap;
    private static final String SETTING_KEY_PAIR_SCRAWL = "vivo_scrawl_gesture_mode";
    private static final Uri URI_PAIR_SCRAWL = Settings.System.getUriFor(SETTING_KEY_PAIR_SCRAWL);
    private static final String SETTING_KEY_BACK_PIC = "vivo_mood_picture_gesture_mode";
    private static final Uri URI_BACK_PIC = Settings.System.getUriFor(SETTING_KEY_BACK_PIC);
    private int mPairScrawlWorkScreen = 0;
    private int mBackPicWorkScreen = 0;
    private int mDisplayMode = -1;
    private int mDisplayID = -1;
    private ContentObserver mContentObserver = null;
    private SensorEventListener mACCListener = new SensorEventListener() { // from class: com.android.server.policy.key.VivoSmartwakeKeyHandler.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (VivoSmartwakeKeyHandler.this.mGDataCount < 3) {
                VivoSmartwakeKeyHandler.access$608(VivoSmartwakeKeyHandler.this);
            }
            if (!VivoSmartwakeKeyHandler.this.mHasGdataRefresh && VivoSmartwakeKeyHandler.this.mGDataCount == 3) {
                VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler = VivoSmartwakeKeyHandler.this;
                vivoSmartwakeKeyHandler.printf("smartwake x is " + x + " y is " + y + " z is " + z);
                if (y < -6.0f) {
                    VivoSmartwakeKeyHandler.this.mIsPhoneHeadStand = true;
                } else {
                    VivoSmartwakeKeyHandler.this.mIsPhoneHeadStand = false;
                }
                VivoSmartwakeKeyHandler.this.mHasGdataRefresh = true;
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler = VivoSmartwakeKeyHandler.this;
            vivoSmartwakeKeyHandler.printf("smartwake accuracy changed " + accuracy);
        }
    };
    private boolean mIsSensorRegistered = false;
    private volatile boolean mHasGdataRefresh = false;
    private volatile int mKeyCode = 0;
    private int mCameraTimeoutRecord = 0;
    private Handler mHandler = new Handler() { // from class: com.android.server.policy.key.VivoSmartwakeKeyHandler.2
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    boolean isSecure = VivoSmartwakeKeyHandler.this.getKeyguardManager().isKeyguardSecure();
                    boolean isLocked = VivoSmartwakeKeyHandler.this.getKeyguardManager().isKeyguardLocked();
                    VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler = VivoSmartwakeKeyHandler.this;
                    vivoSmartwakeKeyHandler.printf("smartwake enable " + msg.arg1 + "," + msg.arg2 + "," + isSecure + "," + isLocked);
                    VivoSmartwakeKeyHandler.this.mKeyCode = msg.arg1;
                    if (msg.arg2 != -1) {
                        VivoSmartwakeKeyHandler.this.mHandler.removeMessages(4);
                        Message lightMsg = VivoSmartwakeKeyHandler.this.mHandler.obtainMessage(4);
                        VivoSmartwakeKeyHandler.this.mHandler.sendMessageDelayed(lightMsg, 3000L);
                        if (VivoSmartwakeKeyHandler.this.mHandler.hasMessages(2)) {
                            VivoSmartwakeKeyHandler.this.mHandler.removeMessages(2);
                            VivoSmartwakeKeyHandler.this.printf("smartwake prepare remove. before enable.");
                            VivoSmartwakeKeyHandler.this.mWakeLock.release();
                        }
                        VivoSmartwakeKeyHandler.this.viewEnable(msg.arg1, true, true);
                        if (50 != VivoSmartwakeKeyHandler.this.mKeyCode && !VivoSmartwakeKeyHandler.this.isFbeLocked()) {
                            VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler2 = VivoSmartwakeKeyHandler.this;
                            vivoSmartwakeKeyHandler2.launchApp(vivoSmartwakeKeyHandler2.mKeyCode, isSecure, isLocked);
                        }
                    } else {
                        VivoSmartwakeKeyHandler.this.launchApp(msg.arg1, isSecure, isLocked);
                        VivoSmartwakeKeyHandler.this.mKeyCode = 0;
                    }
                    VivoSmartwakeKeyHandler.this.mWakeLock.release();
                    return;
                case 2:
                    VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler3 = VivoSmartwakeKeyHandler.this;
                    vivoSmartwakeKeyHandler3.printf("smartwake prepare " + msg.arg1);
                    VivoSmartwakeKeyHandler.this.wakeupByPM();
                    VivoSmartwakeKeyHandler.this.mWakeLock.release();
                    return;
                case 3:
                    boolean isSecure2 = VivoSmartwakeKeyHandler.this.getKeyguardManager().isKeyguardSecure();
                    boolean isLocked2 = VivoSmartwakeKeyHandler.this.getKeyguardManager().isKeyguardLocked();
                    VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler4 = VivoSmartwakeKeyHandler.this;
                    vivoSmartwakeKeyHandler4.printf("smartwake start " + isSecure2 + "," + isLocked2);
                    VivoSmartwakeKeyHandler.this.mHandler.removeMessages(4);
                    Message disableMsg = VivoSmartwakeKeyHandler.this.mHandler.obtainMessage(4);
                    VivoSmartwakeKeyHandler.this.mHandler.sendMessageDelayed(disableMsg, 6000L);
                    VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler5 = VivoSmartwakeKeyHandler.this;
                    vivoSmartwakeKeyHandler5.viewUpdateDisappearTime(vivoSmartwakeKeyHandler5.mKeyCode, isSecure2);
                    VivoSmartwakeKeyHandler.this.viewStart(isSecure2);
                    return;
                case 4:
                    if (VivoSmartwakeKeyHandler.PKG_CAMERA.equals(VivoSmartwakeKeyHandler.this.mLaunchPkgName)) {
                        boolean isForeground = VivoSmartwakeKeyHandler.this.isRunningForeground(VivoSmartwakeKeyHandler.PKG_CAMERA);
                        if (!isForeground && VivoSmartwakeKeyHandler.this.mCameraTimeoutRecord < 1500) {
                            VivoSmartwakeKeyHandler.access$2312(VivoSmartwakeKeyHandler.this, 100);
                            VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler6 = VivoSmartwakeKeyHandler.this;
                            vivoSmartwakeKeyHandler6.printf("smartwake  isForeground : " + isForeground + ", wait for another 100ms");
                            VivoSmartwakeKeyHandler.this.sendViewDisable(100L);
                            return;
                        }
                        VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler7 = VivoSmartwakeKeyHandler.this;
                        vivoSmartwakeKeyHandler7.printf("smartwake  isForeground : " + isForeground + ", showDuration : " + VivoSmartwakeKeyHandler.this.mCameraTimeoutRecord + ", will wait 500ms");
                        VivoSmartwakeKeyHandler.this.mCameraTimeoutRecord = 0;
                        VivoSmartwakeKeyHandler.this.sendCameraViewDisable(1200L);
                        return;
                    }
                    VivoSmartwakeKeyHandler.this.viewTimeoutDisable();
                    return;
                case 5:
                    VivoSmartwakeKeyHandler.this.mWakeLock.release();
                    VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler8 = VivoSmartwakeKeyHandler.this;
                    vivoSmartwakeKeyHandler8.printf("smartwake media wakelock timeout " + VivoSmartwakeKeyHandler.this.mWakeLock.toString());
                    return;
                case 6:
                case 7:
                case 8:
                default:
                    return;
                case 9:
                    VivoSmartwakeKeyHandler.this.startAlphaAnimation();
                    return;
                case 10:
                    VivoSmartwakeKeyHandler.this.wakeupByPM();
                    return;
                case 11:
                    VivoSmartwakeKeyHandler.this.printf("smartwake view start timeout");
                    VivoSmartwakeKeyHandler.this.mWaitOffDisplayID = -1;
                    VivoSmartwakeKeyHandler.this.sendViewStartMsg(0L);
                    return;
                case 12:
                    VivoSmartwakeKeyHandler.this.viewTimeoutDisable();
                    return;
            }
        }
    };
    private ScreenTurningOnListener mScreenTurningOnListener = null;
    private int mWaitOffDisplayID = -1;
    private boolean isWaitWindowsDrawn = true;
    private boolean mIsCharAnimationPlaying = false;
    private String mLaunchPkgName = null;
    private boolean mPrimaryProximityNearby = false;
    private boolean mSecondaryProximityNearby = false;
    private SensorEventListener mPrimaryProximityListener = new SensorEventListener() { // from class: com.android.server.policy.key.VivoSmartwakeKeyHandler.4
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            float distance = event.values[0];
            if (distance < 0.0d || distance >= VivoSmartwakeKeyHandler.PROXIMITY_THRESHOLD || VivoSmartwakeKeyHandler.this.mProximitySensorPrimary == null || distance >= VivoSmartwakeKeyHandler.this.mProximitySensorPrimary.getMaximumRange()) {
                VivoSmartwakeKeyHandler.this.mPrimaryProximityNearby = false;
            } else {
                VivoSmartwakeKeyHandler.this.mPrimaryProximityNearby = true;
            }
            VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler = VivoSmartwakeKeyHandler.this;
            vivoSmartwakeKeyHandler.printf("smartwake proximity sensor mPrimaryProximityNearby is " + VivoSmartwakeKeyHandler.this.mPrimaryProximityNearby);
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private SensorEventListener mSecondaryProximityListener = new SensorEventListener() { // from class: com.android.server.policy.key.VivoSmartwakeKeyHandler.5
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            float distance = event.values[0];
            if (distance < 0.0d || distance >= VivoSmartwakeKeyHandler.PROXIMITY_THRESHOLD || VivoSmartwakeKeyHandler.this.mProximitySensorSecondary == null || distance >= VivoSmartwakeKeyHandler.this.mProximitySensorSecondary.getMaximumRange()) {
                VivoSmartwakeKeyHandler.this.mSecondaryProximityNearby = false;
            } else {
                VivoSmartwakeKeyHandler.this.mSecondaryProximityNearby = true;
            }
            VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler = VivoSmartwakeKeyHandler.this;
            vivoSmartwakeKeyHandler.printf("smartwake proximity sensor mSecondaryProximityNearby is " + VivoSmartwakeKeyHandler.this.mSecondaryProximityNearby);
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private boolean mDismissKeyguard = false;
    private Runnable mUserPresentRunnable = new Runnable() { // from class: com.android.server.policy.key.VivoSmartwakeKeyHandler.6
        @Override // java.lang.Runnable
        public void run() {
            if (VivoSmartwakeKeyHandler.this.mPendingIntent == null) {
                return;
            }
            int visitMode = Settings.System.getInt(VivoSmartwakeKeyHandler.this.mContext.getContentResolver(), "visit_mode", 0);
            VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler = VivoSmartwakeKeyHandler.this;
            vivoSmartwakeKeyHandler.printf("smartwake user event present trigger " + visitMode);
            if (visitMode == 0 && !VivoSmartwakeKeyHandler.this.isFbeLocked()) {
                VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler2 = VivoSmartwakeKeyHandler.this;
                vivoSmartwakeKeyHandler2.startActivity(vivoSmartwakeKeyHandler2.mPendingIntent);
            }
            VivoSmartwakeKeyHandler.this.mPendingIntent = null;
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.key.VivoSmartwakeKeyHandler.7
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                VivoSmartwakeKeyHandler.this.printf("smartwake screen off...");
                VivoSmartwakeKeyHandler.this.mHasReportedSmartwakeKey = false;
                VivoSmartwakeKeyHandler.this.mPendingIntent = null;
                VivoSmartwakeKeyHandler.this.mHandler.removeCallbacks(VivoSmartwakeKeyHandler.this.mUserPresentRunnable);
            } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                if (VivoSmartwakeKeyHandler.this.mHandler.hasMessages(2)) {
                    VivoSmartwakeKeyHandler.this.mHandler.removeMessages(2);
                    VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler = VivoSmartwakeKeyHandler.this;
                    vivoSmartwakeKeyHandler.printf("smartwake prepare removed... " + VivoSmartwakeKeyHandler.this.mKeyCode);
                    VivoSmartwakeKeyHandler.this.mWakeLock.release();
                    return;
                }
                VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler2 = VivoSmartwakeKeyHandler.this;
                vivoSmartwakeKeyHandler2.printf("smartwake screen on... " + VivoSmartwakeKeyHandler.this.mKeyCode);
            } else {
                VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler3 = VivoSmartwakeKeyHandler.this;
                vivoSmartwakeKeyHandler3.printf("smartwake wrong action !" + action);
            }
        }
    };
    private BroadcastReceiver mUserPresentReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.key.VivoSmartwakeKeyHandler.8
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_PRESENT".equals(action)) {
                VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler = VivoSmartwakeKeyHandler.this;
                vivoSmartwakeKeyHandler.printf("smartwake user present " + VivoSmartwakeKeyHandler.this.mDismissKeyguard);
                if (VivoSmartwakeKeyHandler.this.mPendingIntent != null) {
                    int delay = VivoSmartwakeKeyHandler.this.mPendingIntent.getIntExtra(VivoSmartwakeKeyHandler.EXTRA_USER_PRESENT_DELAY, 0);
                    VivoSmartwakeKeyHandler.this.mHandler.removeCallbacks(VivoSmartwakeKeyHandler.this.mUserPresentRunnable);
                    VivoSmartwakeKeyHandler.this.mHandler.postDelayed(VivoSmartwakeKeyHandler.this.mUserPresentRunnable, delay);
                }
                if (VivoSmartwakeKeyHandler.this.mDismissKeyguard) {
                    VivoSmartwakeKeyHandler.this.mDismissKeyguard = false;
                    SystemProperties.set(VivoSmartwakeKeyHandler.PROP_DISMISS, "0");
                }
            }
        }
    };
    private BroadcastReceiver mReceivers = new BroadcastReceiver() { // from class: com.android.server.policy.key.VivoSmartwakeKeyHandler.9
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (((action.hashCode() == 798292259 && action.equals("android.intent.action.BOOT_COMPLETED")) ? (char) 0 : (char) 65535) == 0) {
                VivoSmartwakeKeyHandler.this.registerContentObserver();
                VivoSmartwakeKeyHandler.this.registerProximityListener();
                VivoSmartwakeKeyHandler.this.updateScrawlStatus();
                VivoSmartwakeKeyHandler.this.registerScreenDisplayListener();
            }
        }
    };
    private boolean mIsPhoneHeadStand = false;
    private volatile int mGDataCount = 0;
    private volatile boolean mHasReportedSmartwakeKey = false;
    private boolean mIsSmartkeyDownReported = false;
    private boolean mIsFbeProject = StorageManager.isFileEncryptedNativeOrEmulated();

    static /* synthetic */ int access$2312(VivoSmartwakeKeyHandler x0, int x1) {
        int i = x0.mCameraTimeoutRecord + x1;
        x0.mCameraTimeoutRecord = i;
        return i;
    }

    static /* synthetic */ int access$608(VivoSmartwakeKeyHandler x0) {
        int i = x0.mGDataCount;
        x0.mGDataCount = i + 1;
        return i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver() {
            super(new Handler());
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler = VivoSmartwakeKeyHandler.this;
            vivoSmartwakeKeyHandler.printf("onChange: selfChange = " + selfChange + ", uri = " + uri);
            if (VivoSmartwakeKeyHandler.URI_PAIR_SCRAWL.equals(uri)) {
                VivoSmartwakeKeyHandler.this.printf("pair scrawl changed.");
                VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler2 = VivoSmartwakeKeyHandler.this;
                vivoSmartwakeKeyHandler2.mPairScrawlWorkScreen = Settings.System.getIntForUser(vivoSmartwakeKeyHandler2.mContext.getContentResolver(), VivoSmartwakeKeyHandler.SETTING_KEY_PAIR_SCRAWL, 0, -2);
                VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler3 = VivoSmartwakeKeyHandler.this;
                vivoSmartwakeKeyHandler3.printf("pair scrawl changed,now: " + VivoSmartwakeKeyHandler.this.mPairScrawlWorkScreen);
            } else if (VivoSmartwakeKeyHandler.URI_BACK_PIC.equals(uri)) {
                VivoSmartwakeKeyHandler.this.printf("back pic changed.");
                VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler4 = VivoSmartwakeKeyHandler.this;
                vivoSmartwakeKeyHandler4.mBackPicWorkScreen = Settings.System.getIntForUser(vivoSmartwakeKeyHandler4.mContext.getContentResolver(), VivoSmartwakeKeyHandler.SETTING_KEY_BACK_PIC, 0, -2);
                VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler5 = VivoSmartwakeKeyHandler.this;
                vivoSmartwakeKeyHandler5.printf("back pic changed,now: " + VivoSmartwakeKeyHandler.this.mBackPicWorkScreen);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerContentObserver() {
        ContentResolver cr = this.mContext.getContentResolver();
        SettingsObserver settingsObserver = new SettingsObserver();
        this.mContentObserver = settingsObserver;
        cr.registerContentObserver(URI_PAIR_SCRAWL, true, settingsObserver, -2);
        cr.registerContentObserver(URI_BACK_PIC, true, this.mContentObserver, -2);
        printf("register content observer complete");
    }

    public VivoSmartwakeKeyHandler(Context context, IVivoAdjustmentPolicy vivoPolicy) {
        this.mContext = context;
        this.mVivoPolicy = vivoPolicy;
        printf("smartwake is fbe project : " + this.mIsFbeProject);
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mWakeLock = powerManager.newWakeLock(1, "VivoSmartwakeKeyHandler");
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mMultiDisplayManager = (MultiDisplayManager) this.mContext.getSystemService("multidisplay");
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mTelecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        registerReceiver();
    }

    private static int getMainProximityType() {
        try {
            Class<?> sensorClass = Class.forName("android.hardware.Sensor");
            Field field = sensorClass.getDeclaredField("TYPE_PROXIMITYFRONT");
            return field.getInt(sensorClass);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private int getBackProximityType() {
        try {
            Class<?> sensorClass = Class.forName("android.hardware.Sensor");
            Field field = sensorClass.getDeclaredField("TYPE_PROXIMITY_B");
            return field.getInt(sensorClass);
        } catch (Exception e) {
            printf("get secondary proximity sensor type exception: " + e.toString());
            e.printStackTrace();
            return -1;
        }
    }

    private boolean isDoubleScreenProject() {
        Display display = this.mDisplayManager.getDisplay(4096);
        return display != null;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        if (this.mState != 0) {
            return false;
        }
        boolean isDrop = checkForwardDeforeQueueing(keyCode, this.mPolicyFlags, event);
        return isDrop;
    }

    private boolean checkForwardDeforeQueueing(int keyCode, int policyFlags, KeyEvent event) {
        if (this.mPowerManager.isInteractive() && 224 == keyCode) {
            return true;
        }
        String inputDeviceName = getInputDeviceName(event);
        if (!INPUT_DEVICE_NAME_PRIMARY.equals(inputDeviceName) && !INPUT_DEVICE_NAME_SECONDARY.equals(inputDeviceName)) {
            printf("smartwake input device name: " + inputDeviceName + ", not intercept");
            return true;
        } else if ((keyCode != 19 && keyCode != 29 && keyCode != 31 && keyCode != 36 && keyCode != 41 && keyCode != 43 && keyCode != 21 && keyCode != 22 && keyCode != 33 && keyCode != 34 && keyCode != 50 && keyCode != 51) || (33554432 & policyFlags) != 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        int i = this.mState;
        if (i == 0) {
            getDisplayInfor(event);
            int result = interceptKeyBeforeQueueing(true, keyCode, event, this.mIsScreenOn);
            printf("smartwake onKeyDown " + result);
            return result;
        } else if (i != 1 || keyCode != 224) {
            return -100;
        } else {
            if (this.mKeyInterceptionInfo != null && "com.iqoo.engineermode".equals(this.mKeyInterceptionInfo.mOwningPackage) && this.mKeyInterceptionInfo.mLayoutTitle.contains("com.iqoo.engineermode.WakeupTest")) {
                printf("mOwningPackage = " + this.mKeyInterceptionInfo.mOwningPackage + " mLayoutTitle= " + this.mKeyInterceptionInfo.mLayoutTitle);
                return -100;
            }
            printf("not in factory mode ,need drop wake up key ");
            return -1;
        }
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        if (this.mState != 0) {
            return -100;
        }
        int result = interceptKeyBeforeQueueing(false, keyCode, event, this.mIsScreenOn);
        printf("smartwake onKeyUp " + result);
        return result;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public void onKeyLongPress(int keyCode, KeyEvent event) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void printf(String msg) {
        VLog.d("VivoSmartwakeKeyHandler", msg);
    }

    private void registerAccSensor() {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null) {
            return;
        }
        Sensor accSensor = sensorManager.getDefaultSensor(1);
        if (accSensor == null) {
            printf("no accelerometer sensor");
            return;
        }
        this.mIsSensorRegistered = true;
        this.mGDataCount = 0;
        printf("smartwake registerAccSensor");
        this.mSensorManager.registerListener(this.mACCListener, accSensor, 0);
    }

    private void unregisterAccSensor() {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null || !this.mIsSensorRegistered) {
            printf("no sensor registerred.");
            return;
        }
        sensorManager.unregisterListener(this.mACCListener);
        this.mIsSensorRegistered = false;
    }

    private boolean responseForKey(int keyCode) {
        boolean response = false;
        if (!this.mPowerManager.isInteractive()) {
            if (50 != keyCode && 36 != keyCode) {
                response = true;
            } else if (50 == keyCode) {
                response = isWorkScreen(this.mBackPicWorkScreen);
            } else if (36 == keyCode) {
                response = isWorkScreen(this.mPairScrawlWorkScreen);
            }
            long start = System.currentTimeMillis();
            if (2 == this.mTelecomManager.getCallState()) {
                int deviceType = this.mAudioManager.getDevicesForStream(0);
                printf("smartwake current device type : " + deviceType);
                if (deviceType == 1) {
                    response = false;
                }
            }
            long end = System.currentTimeMillis();
            if (end - start > 50) {
                VLog.d(TAG, "get device type and call state cost more than 50ms : " + (end - start));
            }
        } else {
            printf("smartwake PowerManager isInteractive");
        }
        printf("smartwake keycode : " + keyCode + " ,response: " + response);
        return response;
    }

    private boolean isWorkScreen(int workScreen) {
        printf("smartwake work screen: " + workScreen);
        boolean z = true;
        if (2 == workScreen) {
            return true;
        }
        int displayID = getDisplayID();
        boolean isWorkScreen = false;
        if (workScreen != 0) {
            if (workScreen == 1) {
                if (4096 != displayID) {
                    z = false;
                }
                isWorkScreen = z;
            }
        } else {
            if (displayID != 0) {
                z = false;
            }
            isWorkScreen = z;
        }
        printf("smartwake isWorkScreen: " + isWorkScreen + " ,displayID: " + displayID);
        return isWorkScreen;
    }

    private int interceptKeyBeforeQueueing(boolean down, int keyCode, KeyEvent event, boolean isScreenOn) {
        boolean vibra;
        int repeatCount = event.getRepeatCount();
        printf("smartwake interceptKeyBeforeQueueing " + keyCode + "," + down + "," + repeatCount);
        if (!responseForKey(keyCode)) {
            printf("smartwake just shield the key");
            return 0;
        }
        String os = getOsType();
        if ("vos".equals(os) && ((keyCode == 51 && !isPackageExist(PKG_WHATSAPP)) || (keyCode == 34 && !isPackageExist(PKG_FACEBOOK)))) {
            printf("smartwake Package not exist: " + getPackageName(keyCode));
            return 0;
        } else if (this.mSmartWakeView != null) {
            printf("smartwake smartwake view showing, not response.");
            return 0;
        } else if (this.mDisplayManager.getDisplay(getDisplayID()) == null) {
            printf("smartwake event screen not exists, do nothing.");
            return 0;
        } else {
            if (repeatCount == 0) {
                if (!this.mHasReportedSmartwakeKey) {
                    if (down) {
                        this.mHasGdataRefresh = false;
                        this.mWakeLock.acquire();
                        registerAccSensor();
                        int waitCount = 0;
                        while (!this.mHasGdataRefresh && waitCount < 40) {
                            waitCount++;
                            SystemClock.sleep(5L);
                        }
                        this.mWakeLock.release();
                        unregisterAccSensor();
                        if (waitCount == 40) {
                            printf("smartwake wait gsensor data timeout!!");
                        }
                        if (this.mIsPhoneHeadStand) {
                            printf("smartwake Phone is headstand, drop ts wakeup key, down");
                            return 0;
                        }
                        this.mIsSmartkeyDownReported = true;
                        printf("smartwake Phone direction is normal down");
                    } else if (!this.mIsSmartkeyDownReported || this.mIsPhoneHeadStand) {
                        printf("smartwake Phone is headstand, drop ts wakeup key, up");
                        return 0;
                    } else {
                        printf("smartwake Phone direction is normal up");
                        this.mHasReportedSmartwakeKey = true;
                        this.mIsSmartkeyDownReported = false;
                    }
                } else {
                    printf("smartwake Has reported smartwake key " + down);
                }
            }
            if (keyCode != 19 && keyCode != 29 && keyCode != 31 && keyCode != 36) {
                if (keyCode != 41) {
                    if (keyCode != 43) {
                        if (keyCode == 224) {
                            if (down && repeatCount == 0) {
                                if (getDisplayID() == 4096) {
                                    notifyProcessByDoubleTap();
                                }
                                performHapticFeedback(keyCode);
                                wakeupByWho(event.getEventTime());
                            }
                            return 1;
                        } else if (keyCode != 21 && keyCode != 22) {
                            if (keyCode != 33 && keyCode != 34 && keyCode != 50 && keyCode != 51 && keyCode != 302) {
                                if (keyCode == 303) {
                                    VLog.i("CUSTOM_SmartwakeHandler", "KEYCODE_CUSTOM_GESTURE");
                                    if (!down || repeatCount != 0) {
                                        return 0;
                                    }
                                    startCustomGestureService();
                                    return 0;
                                }
                                return -100;
                            }
                        }
                    }
                }
                if (isFbeLocked()) {
                    if (keyCode == 22 || keyCode == 21) {
                        return 0;
                    }
                    if (keyCode == 41) {
                        sendDisplayScreenWakeUp(200L);
                        return 0;
                    }
                }
                if (down && repeatCount == 0) {
                    boolean isIdle = false;
                    TelecomManager telecomManager = this.mTelecomManager;
                    if (telecomManager != null) {
                        isIdle = telecomManager.getCallState() == 0;
                    }
                    if (isIdle) {
                        int mediaKeyCode = 0;
                        boolean vibra2 = false;
                        int cardSwitch = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "bbk_screen_disable_card_slide_setting", 0, -2);
                        int leftSwitch = Settings.System.getIntForUser(this.mContext.getContentResolver(), "bbk_screen_disable_change_music_setting", 0, -2);
                        printf("smartwake  cardSwitch is: " + cardSwitch + " leftSwitch is: " + leftSwitch);
                        if (keyCode == 21) {
                            if (cardSwitch == 1 && leftSwitch == 0) {
                                boolean isSecure = getKeyguardManager().isKeyguardSecure();
                                boolean isLocked = getKeyguardManager().isKeyguardLocked();
                                printf("smartwake card package keycode: " + keyCode + " isSecure: " + isSecure + " isLocked:" + isLocked);
                                if (!isFbeLocked()) {
                                    sendDisplayScreenWakeUp(0L);
                                    launchApp(keyCode, isSecure, isLocked);
                                    return 0;
                                }
                                return 0;
                            } else if (isMusicActive()) {
                                mediaKeyCode = 88;
                            }
                        } else if (keyCode == 22) {
                            if (cardSwitch == 1 && leftSwitch == 0) {
                                return 0;
                            }
                            if (isMusicActive()) {
                                mediaKeyCode = 87;
                            }
                        } else {
                            mediaKeyCode = 85;
                            vibra2 = true;
                        }
                        sendSimulatedMediaButtonEvent(mediaKeyCode);
                        if (vibra2) {
                            performHapticFeedback(keyCode);
                        }
                    }
                }
                this.mHasReportedSmartwakeKey = false;
                return 0;
            }
            if (isFbeLocked() && keyCode == 302) {
                sendDisplayScreenWakeUp(200L);
                return 0;
            } else if (!down || repeatCount != 0) {
                return 0;
            } else {
                boolean isIdle2 = false;
                boolean isRinging = false;
                TelecomManager telecomManager2 = this.mTelecomManager;
                if (telecomManager2 != null) {
                    isRinging = telecomManager2.isRinging();
                    isIdle2 = this.mTelecomManager.getCallState() == 0;
                }
                if (isIdle2) {
                    this.mHandler.removeMessages(4);
                    if (this.mHandler.hasMessages(2)) {
                        printf("smartwake prepare removed. hasMessages...");
                    }
                    this.mWakeLock.acquire();
                    Message msg = this.mHandler.obtainMessage(1);
                    msg.arg1 = keyCode;
                    msg.arg2 = 0;
                    this.mHandler.sendMessageDelayed(msg, 0L);
                    vibra = true;
                } else if (isRinging) {
                    wakeupByPM(event.getEventTime());
                    vibra = true;
                } else {
                    this.mWakeLock.acquire();
                    Message msg2 = this.mHandler.obtainMessage(1);
                    msg2.arg1 = keyCode;
                    msg2.arg2 = -1;
                    this.mHandler.sendMessageDelayed(msg2, 0L);
                    wakeupByPM(event.getEventTime());
                    vibra = true;
                }
                if (!vibra) {
                    return 0;
                }
                performHapticFeedback(keyCode);
                return 0;
            }
        }
    }

    public boolean isPackageExist(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            pm.getApplicationInfo(packageName, EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override // com.android.server.policy.IVivoWindowListener
    public IBinder getWindowToken() {
        IBinder windowToken = null;
        VivoSmartwakeView vivoSmartwakeView = this.mSmartWakeView;
        if (vivoSmartwakeView != null && (windowToken = vivoSmartwakeView.getWindowToken()) == null) {
            printf("smartwake window failed...");
        }
        return windowToken;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ScreenTurningOnListener implements DisplayManager.DisplayListener {
        private ScreenTurningOnListener() {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            if (VivoSmartwakeKeyHandler.this.getDisplayID() == displayId && VivoSmartwakeKeyHandler.this.isScreenOn(displayId)) {
                if (VivoSmartwakeKeyHandler.this.mHandler.hasMessages(2)) {
                    VivoSmartwakeKeyHandler.this.mHandler.removeMessages(2);
                    VivoSmartwakeKeyHandler.this.printf("smartwake remove light msg");
                    VivoSmartwakeKeyHandler.this.mWakeLock.release();
                } else {
                    VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler = VivoSmartwakeKeyHandler.this;
                    vivoSmartwakeKeyHandler.printf("smartwake screen on: " + displayId);
                }
                VivoSmartwakeKeyHandler.this.onFinishScreenTurningOn();
                VivoSmartwakeKeyHandler.this.unregisterScreenTurningOnListener();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isScreenOn(int displayID) {
        return checkScreenStatus(displayID, 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkScreenStatus(int displayID, int displayState) {
        Display display = this.mDisplayManager.getDisplay(displayID);
        return display != null && displayState == display.getState();
    }

    private void registerScreenTurningOnListener() {
        if (this.mScreenTurningOnListener == null) {
            ScreenTurningOnListener screenTurningOnListener = new ScreenTurningOnListener();
            this.mScreenTurningOnListener = screenTurningOnListener;
            this.mDisplayManager.registerDisplayListener(screenTurningOnListener, this.mHandler);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterScreenTurningOnListener() {
        ScreenTurningOnListener screenTurningOnListener = this.mScreenTurningOnListener;
        if (screenTurningOnListener != null) {
            this.mDisplayManager.unregisterDisplayListener(screenTurningOnListener);
            this.mScreenTurningOnListener = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerScreenDisplayListener() {
        this.mDisplayManager.registerDisplayListener(new ScreenDisplayListener(), this.mHandler);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ScreenDisplayListener implements DisplayManager.DisplayListener {
        private ScreenDisplayListener() {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            if (VivoSmartwakeKeyHandler.this.mWaitOffDisplayID == displayId) {
                if (VivoSmartwakeKeyHandler.this.checkScreenStatus(displayId, 1) || VivoSmartwakeKeyHandler.this.checkScreenStatus(displayId, 2)) {
                    VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler = VivoSmartwakeKeyHandler.this;
                    vivoSmartwakeKeyHandler.printf("smartwake Screen " + displayId + " changed to OFF/ON, start smartwake anim.");
                    VivoSmartwakeKeyHandler.this.mWaitOffDisplayID = -1;
                    VivoSmartwakeKeyHandler.this.mHandler.removeMessages(11);
                    VivoSmartwakeKeyHandler.this.sendViewStartMsg(0L);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendViewStartMsg(long delayMillis) {
        this.mHandler.removeMessages(4);
        Message msg = this.mHandler.obtainMessage(3);
        this.mHandler.sendMessageDelayed(msg, delayMillis);
    }

    @Override // com.android.server.policy.IVivoWindowListener
    public void onFinishScreenTurningOn() {
        printf("smartwake onFinishScreenTurningOn " + this.mKeyCode);
        if (this.mKeyCode != 0) {
            int backDisplayID = getAnotherScreenDisplayID(getDisplayID());
            if (!isDoubleScreenProject() || checkScreenStatus(backDisplayID, 1) || checkScreenStatus(backDisplayID, 2)) {
                sendViewStartMsg(0L);
            } else {
                printf("smartwake double screen project and screen is not in OFF/ON state, wait back screen to OFF/ON.");
                Message msg = this.mHandler.obtainMessage(11);
                this.mHandler.sendMessageDelayed(msg, 1000L);
                this.mWaitOffDisplayID = backDisplayID;
            }
            if (this.mKeyCode != 50) {
                this.mKeyCode = 0;
                return;
            }
            return;
        }
        sendViewDisable(0L);
    }

    @Override // com.android.server.policy.IVivoWindowListener
    public void notifyWindowsDrawn(IBinder appToken) {
        String str;
        printf("smartwake notifyWindowsDrawn " + appToken);
        String tokenStr = appToken.toString();
        if (this.mSmartWakeView != null && tokenStr != null && (str = this.mLaunchPkgName) != null && tokenStr.contains(str) && this.isWaitWindowsDrawn) {
            this.isWaitWindowsDrawn = false;
            if (!this.mIsCharAnimationPlaying) {
                printf("smartwake WindowsDrawn startAlphaAnimation...");
                sendAlphaAnimation(0L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendCameraViewDisable(long delay) {
        this.mHandler.removeMessages(12);
        Message msg = this.mHandler.obtainMessage(12);
        this.mHandler.sendMessageDelayed(msg, delay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendViewDisable(long delay) {
        this.mHandler.removeMessages(4);
        Message msg = this.mHandler.obtainMessage(4);
        this.mHandler.sendMessageDelayed(msg, delay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendAlphaAnimation(long delay) {
        this.mHandler.removeMessages(9);
        Message msg = this.mHandler.obtainMessage(9);
        this.mHandler.sendMessageDelayed(msg, delay);
    }

    private void sendDisplayScreenWakeUp(long delay) {
        this.mHandler.removeMessages(10);
        Message msg = this.mHandler.obtainMessage(10);
        this.mHandler.sendMessageDelayed(msg, delay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startAlphaAnimation() {
        VivoSmartwakeView vivoSmartwakeView = this.mSmartWakeView;
        if (vivoSmartwakeView != null) {
            vivoSmartwakeView.startAlphaAnimation();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void viewEnable(int keyCode, boolean visible, boolean wakeup) {
        VivoSmartwakeView vivoSmartwakeView = this.mSmartWakeView;
        if (vivoSmartwakeView == null) {
            if (this.mDisplayManager.getDisplay(getDisplayID()) == null) {
                printf("smartwake show smartwake view screen not exists, do nothing.");
                return;
            }
            if (this.mPowerManager.isInteractive() && isDoubleScreenProject()) {
                registerScreenTurningOnListener();
            }
            logoutAntiMisoperation();
            VivoSmartwakeView vivoSmartwakeView2 = new VivoSmartwakeView(this.mContext);
            this.mSmartWakeView = vivoSmartwakeView2;
            vivoSmartwakeView2.setmKeyCode(keyCode);
            this.mSmartWakeView.setAnimEndlistener(new VivoSmartwakeCharContainer.SmartWakeCallback() { // from class: com.android.server.policy.key.VivoSmartwakeKeyHandler.3
                @Override // com.android.server.policy.key.VivoSmartwakeCharContainer.SmartWakeCallback
                public void AllAnimationEnd() {
                    VivoSmartwakeKeyHandler.this.printf("smartwake AllAnimationEnd...");
                    VivoSmartwakeKeyHandler.this.mHandler.removeMessages(4);
                    VivoSmartwakeKeyHandler.this.viewDisable();
                }

                @Override // com.android.server.policy.key.VivoSmartwakeCharContainer.SmartWakeCallback
                public void charAnimationEnd() {
                    VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler = VivoSmartwakeKeyHandler.this;
                    vivoSmartwakeKeyHandler.printf("smartwake charAnimationEnd, waitWindowDrawn: " + VivoSmartwakeKeyHandler.this.isWaitWindowsDrawn);
                    VivoSmartwakeKeyHandler.this.mIsCharAnimationPlaying = false;
                    if (VivoSmartwakeKeyHandler.this.isWaitWindowsDrawn) {
                        if (VivoSmartwakeKeyHandler.PKG_CAMERA.equals(VivoSmartwakeKeyHandler.this.mLaunchPkgName)) {
                            VivoSmartwakeKeyHandler.this.sendViewDisable(0L);
                        } else {
                            VivoSmartwakeKeyHandler.this.sendAlphaAnimation(500L);
                        }
                    } else {
                        VivoSmartwakeKeyHandler.this.sendAlphaAnimation(0L);
                    }
                    if (50 == VivoSmartwakeKeyHandler.this.mKeyCode) {
                        if (!VivoSmartwakeKeyHandler.this.isFbeLocked()) {
                            VivoSmartwakeKeyHandler.this.offScreen();
                            boolean isSecure = VivoSmartwakeKeyHandler.this.getKeyguardManager().isKeyguardSecure();
                            boolean isLocked = VivoSmartwakeKeyHandler.this.getKeyguardManager().isKeyguardLocked();
                            VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler2 = VivoSmartwakeKeyHandler.this;
                            vivoSmartwakeKeyHandler2.printf("smartwake start back screen pic: " + isSecure + "," + isLocked);
                            VivoSmartwakeKeyHandler vivoSmartwakeKeyHandler3 = VivoSmartwakeKeyHandler.this;
                            vivoSmartwakeKeyHandler3.launchApp(vivoSmartwakeKeyHandler3.mKeyCode, isSecure, isLocked);
                        }
                        VivoSmartwakeKeyHandler.this.mKeyCode = 0;
                    }
                }
            });
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1);
            lp.type = 2021;
            lp.flags = 525592;
            if (ActivityManager.isHighEndGfx()) {
                lp.flags |= Dataspace.TRANSFER_GAMMA2_2;
                lp.privateFlags |= 2;
            }
            if (4096 == getDisplayID()) {
                lp.privateFlags |= Dataspace.TRANSFER_HLG;
            }
            lp.screenOrientation = 1;
            lp.format = 1;
            lp.preferedDisplay = getWMPreferedDisplay();
            printf("smartwake add view display id: " + lp.preferedDisplay);
            lp.layoutInDisplayCutoutMode = 3;
            lp.setTitle("VivoSmartwakeView");
            WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
            lp.inputFeatures = 2 | lp.inputFeatures;
            lp.setFitInsetsTypes(0);
            if (!isScreenOn(0) && !isScreenOn(4096)) {
                sendDisplayScreenWakeUp(400L);
            }
            wm.addView(this.mSmartWakeView, lp);
            wm.updateViewLayout(this.mSmartWakeView, lp);
            this.mSmartWakeView.setVisibility(visible ? 0 : 8);
            this.mSmartWakeView.invalidate();
            return;
        }
        vivoSmartwakeView.setmKeyCode(keyCode);
        this.mSmartWakeView.setVisibility(visible ? 0 : 8);
        if (wakeup) {
            wakeupByPM();
            if (this.mHandler.hasMessages(2)) {
                this.mHandler.removeMessages(2);
                printf("smartwake enabled wakeup... removed...");
                this.mWakeLock.release();
                return;
            }
            printf("smartwake enabled wakeup...");
            return;
        }
        printf("smartwake enabled not wakeup...");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void viewStart(boolean isSecure) {
        VivoSmartwakeView vivoSmartwakeView = this.mSmartWakeView;
        if (vivoSmartwakeView != null) {
            vivoSmartwakeView.startTrackAnimation(isSecure);
            this.mIsCharAnimationPlaying = true;
            return;
        }
        printf("smartwake disabled...");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void viewTimeoutDisable() {
        printf("smartwake disable timeout.");
        unregisterScreenTurningOnListener();
        viewDisable();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void viewDisable() {
        if (this.mSmartWakeView != null) {
            WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
            wm.removeView(this.mSmartWakeView);
            this.mSmartWakeView = null;
            printf("smartwake disable " + this.mWakeLock.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void viewUpdateDisappearTime(int keyCode, boolean isSecure) {
        VivoSmartwakeView vivoSmartwakeView = this.mSmartWakeView;
        if (vivoSmartwakeView != null) {
            vivoSmartwakeView.updateDisappearTime(keyCode, isSecure);
        }
    }

    private void performHapticFeedback(int keyCode) {
        this.mVivoPolicy.performHapticFeedback(1, false, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public KeyguardManager getKeyguardManager() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        }
        return this.mKeyguardManager;
    }

    private boolean isMusicActive() {
        AudioManager am = (AudioManager) this.mContext.getSystemService("audio");
        if (am == null) {
            printf("smartwake isMusicActive: couldn't get AudioManager reference");
            return false;
        }
        return am.isMusicActive();
    }

    private void sendSimulatedMediaButtonEvent(int keyCode) {
        if (keyCode == 0) {
            return;
        }
        printf("smartwake simulated music " + keyCode);
        long time = SystemClock.uptimeMillis();
        KeyEvent keyEvent = new KeyEvent(time, time, 0, keyCode, 0);
        this.mVivoPolicy.sendMediaKeyEvent(keyEvent);
        KeyEvent keyEvent2 = new KeyEvent(time, time, 1, keyCode, 0);
        this.mVivoPolicy.sendMediaKeyEvent(keyEvent2);
        if (this.mHandler.hasMessages(5)) {
            this.mHandler.removeMessages(5);
            this.mWakeLock.release();
            printf("smartwake media wakelock removed.");
        }
        this.mWakeLock.acquire();
        Message msg = this.mHandler.obtainMessage(5);
        this.mHandler.sendMessageDelayed(msg, 2000L);
    }

    private IWindowManager getWMS() {
        if (this.mWMS == null) {
            this.mWMS = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        }
        return this.mWMS;
    }

    private void dismissKeyguard() {
        SystemProperties.set(PROP_DISMISS, "1");
        this.mDismissKeyguard = true;
        try {
            printf("smartwake dismissKeyguard ");
            getWMS().dismissKeyguard((IKeyguardDismissCallback) null, "smartwake");
        } catch (RemoteException e) {
            printf("smartwake dismissKeyguard " + e.toString());
        }
    }

    private void getDisplayInfor(KeyEvent event) {
        this.mDisplayMode = getDisplayMode(event);
        this.mDisplayID = getDisplayID(event);
        printf("smartwake display mode: " + this.mDisplayMode + " ,display id:" + this.mDisplayID);
    }

    private int getDisplayMode() {
        return this.mDisplayMode;
    }

    private int getAnotherDisplayMode(int mode) {
        return mode == 0 ? 1 : 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getDisplayID() {
        return this.mDisplayID;
    }

    private int getAnotherScreenDisplayID(int displayID) {
        return displayID == 0 ? 4096 : 0;
    }

    private String getInputDeviceName(KeyEvent event) {
        InputDevice inputDevice = InputDevice.getDevice(event.getDeviceId());
        if (inputDevice == null) {
            return null;
        }
        String inputDeviceName = inputDevice.getName();
        return inputDeviceName;
    }

    private int getDisplayMode(KeyEvent event) {
        char c;
        String inputDeviceName = getInputDeviceName(event);
        printf("smartwake display mode input device name: " + inputDeviceName);
        int hashCode = inputDeviceName.hashCode();
        if (hashCode != -153483679) {
            if (hashCode == 469690098 && inputDeviceName.equals(INPUT_DEVICE_NAME_PRIMARY)) {
                c = 0;
            }
            c = 65535;
        } else {
            if (inputDeviceName.equals(INPUT_DEVICE_NAME_SECONDARY)) {
                c = 1;
            }
            c = 65535;
        }
        if (c != 0) {
            if (c == 1) {
                return 1;
            }
            printf("smartwake  unknown input device name: " + inputDeviceName);
            return -1;
        }
        return 0;
    }

    private int getDisplayID(KeyEvent event) {
        InputDevice inputDevice = InputDevice.getDevice(event.getDeviceId());
        if (inputDevice != null) {
            String inputDeviceName = inputDevice.getName();
            printf("smartwake display id input device name: " + inputDeviceName);
            char c = 65535;
            int hashCode = inputDeviceName.hashCode();
            if (hashCode != -153483679) {
                if (hashCode == 469690098 && inputDeviceName.equals(INPUT_DEVICE_NAME_PRIMARY)) {
                    c = 0;
                }
            } else if (inputDeviceName.equals(INPUT_DEVICE_NAME_SECONDARY)) {
                c = 1;
            }
            if (c != 0) {
                if (c == 1) {
                    return 4096;
                }
                printf("smartwake unknown input device name: " + inputDeviceName);
                return -1;
            }
            return 0;
        }
        printf("smartwake inputdevice is null");
        return -1;
    }

    private int getWMPreferedDisplay() {
        int wmPreferedDisplay = -1;
        int displayID = getDisplayID();
        if (displayID == 0) {
            wmPreferedDisplay = 1;
        } else if (displayID == 4096) {
            wmPreferedDisplay = 2;
        } else {
            printf("smartwake unknown display id: " + displayID);
        }
        printf("smartwake getWMPreferedDisplay ,wmPreferedDisplay: " + wmPreferedDisplay + " ,displayID: " + displayID);
        return wmPreferedDisplay;
    }

    private void requestMultiDisplayState() {
        int mode = getDisplayMode();
        requestMultiDisplayState(mode);
    }

    private void requestMultiDisplayState(int mode) {
        if (isDoubleScreenProject() && this.mDisplayManager.getDisplay(getDisplayID()) != null) {
            printf("smartwake requestMultiDisplayState mode: " + mode);
            if (mode >= 0) {
                try {
                    this.mMultiDisplayManager.requestMultiDisplayState(mode);
                    return;
                } catch (Exception e) {
                    printf("smartwake request multidisplay state exception: " + e.toString());
                    return;
                }
            }
            printf("smartwake display mode error: " + mode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void offScreen() {
        offScreen(getDisplayMode());
    }

    private void offScreen(int mode) {
        printf("smartwake offscreen mode: " + mode);
        requestMultiDisplayState(mode);
        this.mPowerManager.goToSleep(SystemClock.uptimeMillis());
    }

    private void wakeupByPM(long time) {
        requestMultiDisplayState();
        this.mPowerManager.wakeUp(time);
    }

    private void wakeupByWho(long time) {
        printf("smartwake wakeupByWho is called");
        requestMultiDisplayState();
        if (!isDoubleScreenProject() || getDisplayID() == 4096) {
            this.mPowerManager.wakeUp(time, 0, "double-tap");
        } else {
            this.mPowerManager.wakeUp(time, 0, "double-tap-primary");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void wakeupByPM() {
        printf("smartwake wakeupByPM is called");
        requestMultiDisplayState();
        PowerManager.WakeLock pmWakeLock = this.mPowerManager.newWakeLock(268435466, "SmartWake");
        pmWakeLock.acquire(5000L);
    }

    private void logoutAntiMisoperation() {
        PowerManager.WakeLock pmWakeLock = this.mPowerManager.newWakeLock(1, "SmartWakeLogoutAntiMisoperation");
        pmWakeLock.acquire(1000L);
    }

    private void keyguardGoingAway() {
        try {
            ActivityTaskManager.getService().keyguardGoingAway(4);
        } catch (RemoteException e) {
            printf("smartwake keyguardGoingAway " + e.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void launchApp(int keyCode, boolean isSecure, boolean isLocked) {
        this.mPendingIntent = null;
        this.mHandler.removeCallbacks(this.mUserPresentRunnable);
        this.mLaunchPkgName = getPackageName(keyCode);
        this.isWaitWindowsDrawn = true;
        if (keyCode == 0) {
            printf("smartwake an unknown key...");
            return;
        }
        if (keyCode != 19 && keyCode != 21 && keyCode != 29 && keyCode != 31 && keyCode != 36 && keyCode != 43) {
            if (keyCode == 302) {
                launchCamera(isSecure, isLocked);
                return;
            } else if (keyCode != 33 && keyCode != 34 && keyCode != 50 && keyCode != 51) {
                return;
            }
        }
        launchAppInternal(keyCode, isSecure, isLocked);
    }

    private void launchAppInternal(int keyCode, boolean isSecure, boolean isLocked) {
        boolean isSetLss = isRunningForeground(keyCode);
        ComponentName cn = getComponentName(keyCode);
        printf("smartwake launchAppInternal keyCode: " + keyCode + ",isSecure: " + isSecure + ",isLocked: " + isLocked + ",isSetLss: " + isSetLss);
        if (isSetLss || isSecure || cn == null) {
            this.isWaitWindowsDrawn = false;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addFlags(807469056);
        intent.setComponent(cn);
        intent.addCategory("android.intent.category.LAUNCHER");
        if (keyCode == 33) {
            intent.putExtra("start-mode", "smartwake");
        }
        if (cn == null) {
            if (!isSecure) {
                dismissKeyguard();
            } else if (isLocked) {
                broadcastKeyguardDismiss();
            }
        } else if (50 == this.mKeyCode) {
            startActivityInInverseScreen(intent);
        } else {
            if (keyCode != 21) {
                if (isSecure && isLocked) {
                    this.mPendingIntent = intent;
                    broadcastKeyguardDismiss();
                } else if (isLocked) {
                    dismissKeyguard();
                    keyguardGoingAway();
                }
            }
            printf("smartwake launchapp: " + this.mLaunchPkgName);
            startActivity(intent);
            if (keyCode == 43) {
                this.mPendingIntent = intent;
                intent.putExtra(EXTRA_USER_PRESENT_DELAY, 200);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerProximityListener() {
        if (isDoubleScreenProject()) {
            Sensor sensor = this.mProximitySensorPrimary;
            if (sensor != null) {
                this.mSensorManager.registerListener(this.mPrimaryProximityListener, sensor, 1, this.mHandler);
            }
            Sensor sensor2 = this.mProximitySensorSecondary;
            if (sensor2 != null) {
                this.mSensorManager.registerListener(this.mSecondaryProximityListener, sensor2, 1, this.mHandler);
            }
        }
    }

    private boolean isProximityNearby(int displayID) {
        if (displayID == 0) {
            boolean nearby = this.mPrimaryProximityNearby;
            return nearby;
        } else if (displayID == 4096) {
            boolean nearby2 = this.mSecondaryProximityNearby;
            return nearby2;
        } else {
            printf("smartwake is proximity nearby, unknown displayID: " + displayID);
            return false;
        }
    }

    private void launchCamera(boolean isSecure, boolean isLocked) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addFlags(807469056);
        intent.putExtra("SlideToCamera", "wakeup");
        boolean isForeground = isRunningForeground(PKG_CAMERA);
        boolean childrenMode = isInChildrenMode();
        printf("smartwake launchCamera isForeground: " + isForeground + ", childrenMode: " + childrenMode);
        if ((isForeground && !isSecure) || childrenMode) {
            this.isWaitWindowsDrawn = false;
        }
        if (isForeground) {
            if (isSecure && isLocked) {
                intent.setFlags(268533760);
            } else {
                intent.addFlags(67108864);
            }
        } else {
            intent.setFlags(268533760);
        }
        if (!isSecure || !isLocked) {
            intent.setClassName(PKG_CAMERA, "com.android.camera.CameraActivity");
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.putExtra("wave_open_application", "1");
        } else {
            intent.addFlags(Dataspace.TRANSFER_SRGB);
            intent.setAction(VivoCameraKeyHandler.ACTION_START_CAMERA_SECURE);
            intent.putExtra("wave_open_application", "0");
        }
        if (isLocked && !isSecure) {
            dismissKeyguard();
            keyguardGoingAway();
        }
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isFbeLocked() {
        ((UserManager) this.mContext.getSystemService(UserManager.class)).isUserUnlocked(Process.myUserHandle());
        boolean isSecure = getKeyguardManager().isKeyguardSecure();
        return isFbeLocked(isSecure);
    }

    private boolean isFbeLocked(boolean isSecure) {
        boolean isFbeUserUnloced = ((UserManager) this.mContext.getSystemService(UserManager.class)).isUserUnlocked(Process.myUserHandle());
        printf("smartwake isFbeUserUnloced: " + isFbeUserUnloced + ", isSecure: " + isSecure + ", mIsFbeProject: " + this.mIsFbeProject);
        return this.mIsFbeProject && isSecure && !isFbeUserUnloced;
    }

    private boolean isInChildrenMode() {
        return "true".equals(Settings.System.getString(this.mContext.getContentResolver(), CHILED_ENABLE));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startActivity(Intent intent) {
        startActivity(intent, getDisplayID());
    }

    private void startActivityInInverseScreen(Intent intent) {
        startActivity(intent, getAnotherScreenDisplayID(getDisplayID()));
    }

    private void startActivity(Intent intent, int displayID) {
        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            if (isDoubleScreenProject()) {
                options.setLaunchDisplayId(displayID);
            }
            this.mContext.startActivityAsUser(intent, options.toBundle(), UserHandle.CURRENT);
            printf("smartwake start carema activity ");
        } catch (Exception e) {
            printf("smartwake startActivity " + e.toString());
        }
    }

    private boolean isRunningForeground(int keyCode) {
        String os = getOsType();
        String version = getOsVersion();
        String country = SystemProperties.get("ro.product.customize.bbk");
        if (keyCode == 29) {
            boolean foreground = isRunningForeground(PKG_EMAIL);
            return foreground;
        } else if (keyCode == 31) {
            if (!"ID".equals(country) && !"RU".equals(country) && "2.1".equals(version) && isPackageExist(PKG_GOOGLE_DIALER)) {
                boolean foreground2 = isRunningForeground(PKG_GOOGLE_DIALER);
                return foreground2;
            }
            boolean foreground3 = isRunningForeground(PKG_DIALER);
            return foreground3;
        } else if (keyCode == 36) {
            boolean foreground4 = isRunningForeground(PKG_PAIR_SCRAWL);
            return foreground4;
        } else if (keyCode == 43) {
            boolean foreground5 = isRunningForeground(PKG_MOBILEQQ);
            return foreground5;
        } else if (keyCode == 33) {
            boolean foreground6 = isRunningForeground(PKG_BROWSER);
            return foreground6;
        } else if (keyCode == 34) {
            boolean foreground7 = isRunningForeground(PKG_FACEBOOK);
            return foreground7;
        } else if (keyCode == 50) {
            boolean foreground8 = isRunningForeground(PKG_BACK_PIC);
            return foreground8;
        } else if (keyCode != 51) {
            return false;
        } else {
            if ("IN".equals(country) || "ID".equals(country) || "MY".equals(country) || "vos".equals(os)) {
                boolean foreground9 = isRunningForeground(PKG_WHATSAPP);
                return foreground9;
            }
            boolean foreground10 = isRunningForeground("com.tencent.mm");
            return foreground10;
        }
    }

    private ComponentName getComponentName(int keyCode) {
        String pkgName = null;
        String clsName = null;
        String os = getOsType();
        String version = getOsVersion();
        String country = SystemProperties.get("ro.product.customize.bbk");
        if (keyCode == 21) {
            pkgName = PKG_CARD_PACKAGE;
            clsName = "com.vivo.card.ui.TransparentForCardActivity";
        } else if (keyCode == 29) {
            pkgName = PKG_EMAIL;
            clsName = getMainActivityClass(PKG_EMAIL);
        } else if (keyCode == 31) {
            if (!"ID".equals(country) && !"RU".equals(country) && "2.1".equals(version) && isPackageExist(PKG_GOOGLE_DIALER)) {
                pkgName = PKG_GOOGLE_DIALER;
            } else {
                pkgName = PKG_DIALER;
            }
            clsName = getMainActivityClass(pkgName);
        } else if (keyCode == 36) {
            pkgName = PKG_PAIR_SCRAWL;
            clsName = "com.vivo.scrawl.pair.MainActivity";
        } else if (keyCode == 43) {
            pkgName = PKG_MOBILEQQ;
            clsName = getMainActivityClass(PKG_MOBILEQQ);
        } else if (keyCode == 33) {
            pkgName = PKG_BROWSER;
            clsName = "com.vivo.browser.BrowserActivity";
        } else if (keyCode == 34) {
            pkgName = PKG_FACEBOOK;
            clsName = getMainActivityClass(PKG_FACEBOOK);
        } else if (keyCode == 50) {
            pkgName = PKG_BACK_PIC;
            clsName = "com.vivo.lockscreen.play.ui.lockActicity.LockScreenActivity";
        } else if (keyCode == 51) {
            if ("IN".equals(country) || "ID".equals(country) || "MY".equals(country) || "vos".equals(os)) {
                pkgName = PKG_WHATSAPP;
            } else {
                pkgName = "com.tencent.mm";
            }
            clsName = getMainActivityClass(pkgName);
        }
        printf("smartwake getComponentName clsName: " + clsName);
        if (clsName != null && !clsName.isEmpty()) {
            return new ComponentName(pkgName, clsName);
        }
        return null;
    }

    private String getPackageName(int keyCode) {
        String os = getOsType();
        String version = getOsVersion();
        String country = SystemProperties.get("ro.product.customize.bbk");
        if (keyCode != 21) {
            if (keyCode != 29) {
                if (keyCode == 31) {
                    if (!"ID".equals(country) && !"RU".equals(country) && "2.1".equals(version) && isPackageExist(PKG_GOOGLE_DIALER)) {
                        return PKG_GOOGLE_DIALER;
                    }
                    return PKG_DIALER;
                } else if (keyCode != 36) {
                    if (keyCode != 43) {
                        if (keyCode != 302) {
                            if (keyCode != 33) {
                                if (keyCode != 34) {
                                    if (keyCode != 50) {
                                        if (keyCode != 51) {
                                            return null;
                                        }
                                        if ("IN".equals(country) || "ID".equals(country) || "MY".equals(country) || "vos".equals(os)) {
                                            return PKG_WHATSAPP;
                                        }
                                        return "com.tencent.mm";
                                    }
                                    return PKG_BACK_PIC;
                                }
                                return PKG_FACEBOOK;
                            }
                            return PKG_BROWSER;
                        }
                        return PKG_CAMERA;
                    }
                    return PKG_MOBILEQQ;
                } else {
                    return PKG_PAIR_SCRAWL;
                }
            }
            return PKG_EMAIL;
        }
        return PKG_CARD_PACKAGE;
    }

    private String getMainActivityClass(String pkg) {
        String pkgName;
        int userID = ActivityManager.getCurrentUser();
        PackageManager pm = this.mContext.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> resolveInfos = pm.queryIntentActivitiesAsUser(intent, 0, userID);
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo != null && (pkgName = resolveInfo.activityInfo.packageName) != null && pkgName.equals(pkg)) {
                String clsName = resolveInfo.activityInfo.name;
                return clsName;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isRunningForeground(String packageName) {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
        try {
            ComponentName componentName = am.getRunningTasks(1).get(0).topActivity;
            if (componentName != null) {
                String currentPackageName = componentName.getPackageName();
                if (!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            printf(TAG + packageName + " is running foreground exception...");
            e.printStackTrace();
            return false;
        }
    }

    private void broadcastKeyguardDismiss() {
        Intent intent = new Intent(ACTION_SMART_WAKE_DISMISS);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateScrawlStatus() {
        try {
            this.mPairScrawlWorkScreen = Settings.System.getIntForUser(this.mContext.getContentResolver(), SETTING_KEY_PAIR_SCRAWL, 0, -2);
            this.mBackPicWorkScreen = Settings.System.getIntForUser(this.mContext.getContentResolver(), SETTING_KEY_BACK_PIC, 0, -2);
            printf("init scrawl : " + this.mPairScrawlWorkScreen + ", mood : " + this.mBackPicWorkScreen);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        this.mContext.registerReceiver(this.mReceiver, intentFilter, null, this.mHandler);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.USER_PRESENT");
        intentFilter2.setPriority(-999);
        this.mContext.registerReceiver(this.mUserPresentReceiver, intentFilter2, null, this.mHandler);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mReceivers, intentFilter3, null, this.mHandler);
    }

    private void startCustomGestureService() {
        Intent intent = new Intent(START_SERVICE_ACTION).setPackage("com.vivo.smartwakecustomgesture");
        this.mContext.startService(intent);
    }

    private void notifyProcessByDoubleTap() {
        try {
            if (this.mWakeUpByDoubleTap == null) {
                this.mWakeUpByDoubleTap = IVivoAdjustmentPolicy.class.getDeclaredMethod("wakeupByDoubleTap", new Class[0]);
            }
            if (this.mWakeUpByDoubleTap != null) {
                this.mWakeUpByDoubleTap.invoke(this.mVivoPolicy, new Object[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            VLog.e(TAG, "notifyProcessByDoubleTap: ", e);
        }
    }

    public String getOsType() {
        try {
            Method method = FtBuild.class.getDeclaredMethod("getOsName", new Class[0]);
            method.setAccessible(true);
            return (String) method.invoke(null, new Object[0]);
        } catch (Exception e) {
            VLog.e(TAG, "getOsName error:", e);
            return null;
        }
    }

    public String getOsVersion() {
        try {
            Method method = FtBuild.class.getDeclaredMethod("getOsVersion", new Class[0]);
            method.setAccessible(true);
            return (String) method.invoke(null, new Object[0]);
        } catch (Exception e) {
            VLog.e(TAG, "getOsVersion error:", e);
            return null;
        }
    }
}