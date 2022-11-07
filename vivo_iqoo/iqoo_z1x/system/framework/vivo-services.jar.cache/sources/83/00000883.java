package com.vivo.services.touchscreen;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VivoDisplayModule;
import android.hardware.display.VivoDisplayStateManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.FtFeature;
import android.util.Xml;
import android.view.Display;
import com.android.server.LocalServices;
import com.android.server.wm.VivoEasyShareManager;
import com.vivo.face.common.data.Constants;
import com.vivo.fingerprint.FingerprintConfig;
import com.vivo.services.autorecover.SystemAutoRecoverService;
import com.vivo.services.superresolution.Constant;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.app.touchscreen.ITouchScreen;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class TouchScreenService extends ITouchScreen.Stub {
    static final String ACTION_CLOSE_CAMERA = "com.android.camera.ACTION_CLOSE_CAMERA";
    static final String ACTION_FM_RECORDING_STATUS = "codeaurora.intent.action.FM";
    static final String ACTION_INPUT_METHOD_STATUS = "android.vivo.inputmethod.status";
    static final String ACTION_OPEN_CAMERA = "com.android.camera.ACTION_OPEN_CAMERA";
    static final String ACTION_VIRTUAL_KEY_STATUS = "com.intent.action.VIRTUAL_KEY";
    static final String ACTION_WHITELIST_CLOSE_CAMERA = "com.android.camera.WHITELIST_CLOSE";
    public static final String BBK_SCREEN_DISABLE_CARD_SLIDE_SETTING = "bbk_screen_disable_card_slide_setting";
    static final int BBK_TP_GESTURE_DCLICK_SWITCH = 256;
    static final int BBK_TP_GESTURE_FACE_SWITCH = 8192;
    static final int BBK_TP_GESTURE_FINGERPRINT_QUICK_SWITCH = 2048;
    static final int BBK_TP_GESTURE_LETTER_C_SWITCH = 64;
    static final int BBK_TP_GESTURE_LETTER_E_SWITCH = 32;
    static final int BBK_TP_GESTURE_LETTER_H_SWITCH = 32768;
    static final int BBK_TP_GESTURE_LETTER_M_SWITCH = 16;
    static final int BBK_TP_GESTURE_LETTER_O_SWITCH = 4;
    static final int BBK_TP_GESTURE_LETTER_V_SWITCH = 16384;
    static final int BBK_TP_GESTURE_LETTER_W_SWITCH = 8;
    static final int BBK_TP_GESTURE_PN_FINGER_SWITCH = 4096;
    static final int BBK_TP_GESTURE_SWIPE_DOWN_SWITCH = 128;
    static final int BBK_TP_GESTURE_SWIPE_LEFT_RIGHT_SWITCH = 1;
    static final int BBK_TP_GESTURE_SWIPE_UP_SWITCH = 2;
    static final int BBK_TP_GESTURE_WAKE_EMAIL = 512;
    static final int BBK_TP_GESTURE_WAKE_FACEBOOK = 1024;
    private static final int DISPALY_ID_BACK = 4096;
    private static final int DISPALY_ID_MAIN = 0;
    private static final String EASY_SHARE_FORCE_BRIGHTNESS_OFF = "easy_share_force_brightness_off";
    private static final String FINGERPRINT_UNLOCK_SWITCH = "finger_unlock_open";
    private static final String FINGERPRINT_VISIABLE = "fingerprint_touch_gesture";
    static final String HALL_LOCK_BROADCAST_ACTION = "com.android.service.hallobserver.lock";
    static final String HALL_UNLOCK_BROADCAST_ACTION = "com.android.service.hallobserver.unlock";
    public static final String INPUT_METHOD_STATE = "input_method_state";
    public static final String KEY_GAME_MODE = "is_game_mode";
    public static final String KEY_GAME_SPACE = "is_game_space";
    private static final String PROP_VALUE_PREFIX_UDFP = "udfp_";
    private static final float PROXIMITY_THRESHOLD = 5.0f;
    public static final String SCREEN_CLOCK_REPORT_ABS_SWITCH = "aod_tp_report_switch";
    public static final String SCREEN_CLOCK_SWITCH = "aod_tp_support_switch";
    private static final int SENSORTYPE_PRXOIMITYBACK = 66550;
    private static final int SENSORTYPE_PRXOIMITYFRONT = 66558;
    private static final int SENSOR_TYPE_EDGEREJECTION_DETECT = 66578;
    static final int SET_FINGER_GESTURE_SRATE = 2;
    static final int SET_NATIVE_LCD_STATE_OFF = 0;
    static final int SET_NATIVE_LCD_STATE_ON = 1;
    static final int SET_NATIVE_ROTATION_STATE = 3;
    static final String SUPER_POWER_SAVE_BROADCAST_ACTION = "intent.action.super_power_save_send";
    private static final String TAG = "BBKTouchScreenServiceService";
    public static final String VIRTUAL_GAMEKEY = "game_inputfilter_enabled";
    public static final String VIRTUAL_KEY = "virtual_button_switch";
    public static final String VIRTUAL_KEY_GESTURE = "virtual_button_gesture";
    public static final String VK_LONGPRESS = "virtual_can_long_press";
    private static IActivityManager mIActivityManager;
    private BatteryManager mBatteryManager;
    private Context mContext;
    private EdgeRejectionConfigure mEdgeRejectionConfigure;
    private Sensor mEdgeRejectionSensor;
    private boolean mFactorySwitch;
    private boolean mFingerModeSupport;
    private boolean mForceDisable;
    private Handler mHandler;
    private DisplayMonitor mMainDisplayMonitor;
    private MultiTouchController mMultiTouchController;
    private boolean mNeedDownSensitivity;
    private Handler mNewHandler;
    private Sensor mProximitySensor;
    private RadioBandSwitch mRadioBandSwitch;
    private DisplayMonitor mSecondDisplayMonitor;
    private SensorManager mSensorManager;
    private SettingsObserver mSettingsObserver;
    private boolean mSpsSwitch;
    private Handler mTsLcdStateHandler;
    private BroadcastReceiver mVivoBroadcastReceiver;
    private IntentFilter mVivoIntentFilter;
    private static final boolean DBG = SystemProperties.get("persist.sys.touch.debug", "no").equals("yes");
    static final boolean EdgeRejectionConfigureSupport = SystemProperties.get("persist.vivo.support.edge_rejection_configure", "false").equals("true");
    static final String BBKProductName = SystemProperties.get("ro.vivo.product.model", "null");
    static boolean PCshareTouchSupport = false;
    private static final String[] mDoubleScreenProjects = {"pd1820", "pd1821"};
    static final String[] SystemGesturesSettings = {"bbk_screen_disable_change_music_setting", "bbk_screen_disable_to_unlock_setting", "bbk_screen_disable_wake_qq_setting", "bbk_screen_disable_wake_wechat_setting", "bbk_screen_disable_wake_music_setting", "bbk_screen_disable_wake_browser_setting", "bbk_screen_disable_wake_dial_setting", "bbk_quick_open_camera_setting", "bbk_smart_wakeup", "bbk_screen_disable_wake_email_setting", "bbk_screen_disable_wake_facebook_setting", "quick_launch_app_primary_switch", "finger_unlock_open", Constants.Setting.ARD9_FACE_UNLOCK_SCREEN_OFF, "vivo_mood_picture_setting_enable", "vivo_scrawl_setting_enable", "bbk_cover_screen_mute_setting"};
    private static final Uri urlFingerUnlock = Settings.System.getUriFor("finger_unlock_open");
    private HallLockReceiver mHallLockReceiver = new HallLockReceiver();
    private CamereaReceiver mCamereaReceiver = new CamereaReceiver();
    private int mProximited = -1;
    private int mProximiteListenerRegistered = 0;
    private boolean mIsScreenOn = true;
    private int mGesturesSetting = 0;
    private int mGesturesSettingSave = 256;
    private int mUdgesturesSetting = 0;
    private int mTemplateValid = 0;
    private boolean mHasGesturesEnabled = false;
    private boolean mHallLockEnabled = false;
    private boolean isLcdBacklightCalled = false;
    private boolean isSupportUDFingerprint = false;
    private int mScreenclockSetting = 0;
    private String PACKAGE_NAME = null;
    private String PACKAGE_NAME_BACK = null;
    private int package_num = 0;
    String[] package_name = {Constant.APP_DOUYIN, "com.qiyi.video", "com.tencent.qqlive", "com.ss.android.article.video", "com.youku.phone", "com.hunantv.imgo.activity", "com.babycloud.hanju"};
    private boolean mIsInEasyShareForceBrightnessOffState = false;
    private final Object mLock = new Object();
    SensorEventListener mEdgeRejectionSensorListner = new SensorEventListener() { // from class: com.vivo.services.touchscreen.TouchScreenService.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            VSlog.d(TouchScreenService.TAG, "recieve event = " + event.values[0]);
            int currentState = (int) event.values[0];
            if (currentState == 0) {
                TouchScreenService.this.mEdgeRejectionConfigure.mTimerHandler.removeCallbacks(TouchScreenService.this.mEdgeRejectionConfigure.mTimerRunnable);
                TouchScreenService.this.mEdgeRejectionConfigure.mTimerHandler.postDelayed(TouchScreenService.this.mEdgeRejectionConfigure.mTimerRunnable, 500L);
            } else if (TouchScreenService.this.mEdgeRejectionConfigure.lastState == 0 || TouchScreenService.this.mEdgeRejectionConfigure.flat_state) {
                TouchScreenService.this.mEdgeRejectionConfigure.mTimerHandler.removeCallbacks(TouchScreenService.this.mEdgeRejectionConfigure.mTimerRunnable);
                TouchScreenService.this.mEdgeRejectionConfigure.mTimerHandler.postDelayed(TouchScreenService.this.mEdgeRejectionConfigure.mTimerRunnable, 1500L);
                TouchScreenService.this.mEdgeRejectionConfigure.flat_state = false;
            }
            TouchScreenService.this.mEdgeRejectionConfigure.lastState = currentState;
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    SensorEventListener mProximityListener = new SensorEventListener() { // from class: com.vivo.services.touchscreen.TouchScreenService.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            int ProximityState;
            synchronized (TouchScreenService.this.mLock) {
                float distance = event.values[0];
                if (distance >= 0.0d && distance < TouchScreenService.PROXIMITY_THRESHOLD && distance < TouchScreenService.this.mProximitySensor.getMaximumRange()) {
                    ProximityState = 1;
                } else {
                    ProximityState = 0;
                }
                if (!TouchScreenService.this.mNeedDownSensitivity) {
                    if (TouchScreenService.this.mProximiteListenerRegistered == 1 && TouchScreenService.this.mProximited != ProximityState) {
                        TouchScreenService.this.mProximited = ProximityState;
                        if (ProximityState == 1) {
                            if (TouchScreenService.DBG) {
                                VSlog.d(TouchScreenService.TAG, "Proximity Sensor proximited");
                            }
                            TouchScreenService.nativeTouchScreenDclickEnable(0);
                            TouchScreenService.this.mHasGesturesEnabled = false;
                        } else {
                            if (TouchScreenService.DBG) {
                                VSlog.d(TouchScreenService.TAG, "Proximity Sensor check move away");
                            }
                            TouchScreenService.nativeTouchScreenDclickEnable(1);
                            TouchScreenService.this.mHasGesturesEnabled = true;
                        }
                    }
                } else if (!TouchScreenService.this.mIsScreenOn) {
                    if (TouchScreenService.this.mProximiteListenerRegistered == 1 && TouchScreenService.this.mProximited != ProximityState) {
                        TouchScreenService.this.mProximited = ProximityState;
                        if (ProximityState == 1) {
                            if (TouchScreenService.DBG) {
                                VSlog.d(TouchScreenService.TAG, "Proximity Sensor proximited");
                            }
                            TouchScreenService.nativeTouchScreenDclickEnable(0);
                            TouchScreenService.this.mHasGesturesEnabled = false;
                        } else {
                            if (TouchScreenService.DBG) {
                                VSlog.d(TouchScreenService.TAG, "Proximity Sensor check move away");
                            }
                            TouchScreenService.nativeTouchScreenDclickEnable(1);
                            TouchScreenService.this.mHasGesturesEnabled = true;
                        }
                    }
                } else if (TouchScreenService.this.mProximiteListenerRegistered == 1 && TouchScreenService.this.mProximited != ProximityState) {
                    TouchScreenService.this.mProximited = ProximityState;
                    if (ProximityState == 1) {
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "NeedDownSensitivity Proximity Sensor proximited");
                        }
                        TouchScreenService.nativeTouchScreenGlovesModeSwitch(1);
                    } else {
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "NeedDownSensitivity Proximity Sensor check move away");
                        }
                        TouchScreenService.nativeTouchScreenGlovesModeSwitch(0);
                    }
                }
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final ContentObserver mSmartWakeObserver = new ContentObserver(new Handler()) { // from class: com.vivo.services.touchscreen.TouchScreenService.3
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            for (int i = 0; i < TouchScreenService.SystemGesturesSettings.length; i++) {
                if (Settings.System.getUriFor(TouchScreenService.SystemGesturesSettings[i]).equals(uri)) {
                    VSlog.d(TouchScreenService.TAG, "SmartWake settings " + TouchScreenService.SystemGesturesSettings[i] + " have changed");
                    TouchScreenService.this.TouchscreenSetGestureBitmap();
                }
            }
        }
    };
    private final ContentObserver mGameModeObserver = new ContentObserver(new Handler()) { // from class: com.vivo.services.touchscreen.TouchScreenService.4
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor("is_game_mode").equals(uri)) {
                VSlog.d(TouchScreenService.TAG, "gameMode have changed");
                if (TouchScreenService.isInGameMode(TouchScreenService.this.mContext)) {
                    VSlog.d(TouchScreenService.TAG, "enter game mode");
                    TouchScreenService.this.SetAppName("GAME_IN".getBytes());
                    EdgeRejectionConfigure edgeRejectionConfigure = TouchScreenService.this.mEdgeRejectionConfigure;
                    EdgeRejectionConfigure unused = TouchScreenService.this.mEdgeRejectionConfigure;
                    EdgeRejectionConfigure unused2 = TouchScreenService.this.mEdgeRejectionConfigure;
                    edgeRejectionConfigure.setEdgeRejectionPara(0, 2, 1);
                    TouchScreenService.this.mMultiTouchController.setStateForAll(13, 1);
                } else {
                    VSlog.d(TouchScreenService.TAG, "out game mode");
                    TouchScreenService.this.SetAppName("GAME_OUT".getBytes());
                    EdgeRejectionConfigure edgeRejectionConfigure2 = TouchScreenService.this.mEdgeRejectionConfigure;
                    EdgeRejectionConfigure unused3 = TouchScreenService.this.mEdgeRejectionConfigure;
                    EdgeRejectionConfigure unused4 = TouchScreenService.this.mEdgeRejectionConfigure;
                    edgeRejectionConfigure2.setEdgeRejectionPara(0, 2, 0);
                    TouchScreenService.this.mMultiTouchController.setStateForAll(13, 0);
                }
            }
            if (Settings.Global.getUriFor("is_game_space").equals(uri)) {
                VSlog.d(TouchScreenService.TAG, "gameSpace state have changed");
                if (TouchScreenService.isInGameSpace(TouchScreenService.this.mContext)) {
                    VSlog.d(TouchScreenService.TAG, "enter game space");
                    TouchScreenService.this.SetAppName("GAME_SPACE_IN".getBytes());
                    return;
                }
                VSlog.d(TouchScreenService.TAG, "out game space");
                TouchScreenService.this.SetAppName("GAME_SPACE_OUT".getBytes());
            }
        }
    };
    private final ContentObserver mCardSlideObserver = new ContentObserver(new Handler()) { // from class: com.vivo.services.touchscreen.TouchScreenService.5
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.Secure.getUriFor(TouchScreenService.BBK_SCREEN_DISABLE_CARD_SLIDE_SETTING).equals(uri)) {
                TouchScreenService.this.TouchscreenSetGestureBitmap();
            }
        }
    };
    private final ContentObserver mScreenClockSupportObserver = new ContentObserver(new Handler()) { // from class: com.vivo.services.touchscreen.TouchScreenService.6
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor(TouchScreenService.SCREEN_CLOCK_SWITCH).equals(uri)) {
                int enable = Settings.System.getIntForUser(TouchScreenService.this.mContext.getContentResolver(), TouchScreenService.SCREEN_CLOCK_SWITCH, 0, -2);
                TouchScreenService.this.mMultiTouchController.setState(TouchScreenService.this.mMainDisplayMonitor.getDisplayId(), 19, enable);
                if (1 == enable) {
                    TouchScreenService.this.mScreenclockSetting = 1;
                    TouchScreenService.this.SetAppName("SCREEN_CLOCK_SWITCH_ON".getBytes());
                } else {
                    TouchScreenService.this.mScreenclockSetting = 0;
                    TouchScreenService.this.SetAppName("SCREEN_CLOCK_SWITCH_OFF".getBytes());
                }
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "System Screen clock switch is " + enable);
                }
            }
        }
    };
    private final ContentObserver mScreenClockReportObserver = new ContentObserver(new Handler()) { // from class: com.vivo.services.touchscreen.TouchScreenService.7
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor(TouchScreenService.SCREEN_CLOCK_REPORT_ABS_SWITCH).equals(uri)) {
                int enable = Settings.System.getIntForUser(TouchScreenService.this.mContext.getContentResolver(), TouchScreenService.SCREEN_CLOCK_REPORT_ABS_SWITCH, 0, -2);
                TouchScreenService.this.mMultiTouchController.setState(TouchScreenService.this.mMainDisplayMonitor.getDisplayId(), 20, enable);
                if (1 == enable) {
                    TouchScreenService.this.mScreenclockSetting = 1;
                    TouchScreenService.this.SetAppName("SCREEN_CLOCK_REPORT_SWITCH_ON".getBytes());
                } else {
                    TouchScreenService.this.mScreenclockSetting = 0;
                    TouchScreenService.this.SetAppName("SCREEN_CLOCK_REPORT_SWITCH_OFF".getBytes());
                }
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "System Screen report abs switch is " + enable);
                }
            }
        }
    };
    private final ContentObserver mVirtualKeyObserver = new ContentObserver(new Handler()) { // from class: com.vivo.services.touchscreen.TouchScreenService.8
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor(TouchScreenService.VIRTUAL_KEY_GESTURE).equals(uri)) {
                int enable = Settings.System.getIntForUser(TouchScreenService.this.mContext.getContentResolver(), TouchScreenService.VIRTUAL_KEY_GESTURE, 0, -2);
                TouchScreenService.this.mMultiTouchController.setState(TouchScreenService.this.mMainDisplayMonitor.getDisplayId(), 14, enable);
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "System Virtual key is " + enable);
                }
                if (enable == 1) {
                    TouchScreenService.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(TouchScreenService.VK_LONGPRESS), false, TouchScreenService.this.mVkLongpressObserver, -1);
                    TouchScreenService.this.mMultiTouchController.setState(TouchScreenService.this.mMainDisplayMonitor.getDisplayId(), 15, Settings.System.getIntForUser(TouchScreenService.this.mContext.getContentResolver(), TouchScreenService.VK_LONGPRESS, 0, -2));
                    if (TouchScreenService.DBG) {
                        VSlog.d(TouchScreenService.TAG, "System Virtual key longpress is " + enable);
                        return;
                    }
                    return;
                }
                TouchScreenService.this.mContext.getContentResolver().unregisterContentObserver(TouchScreenService.this.mVkLongpressObserver);
                TouchScreenService.this.mMultiTouchController.setState(TouchScreenService.this.mMainDisplayMonitor.getDisplayId(), 15, 0);
            }
        }
    };
    private final ContentObserver mVkLongpressObserver = new ContentObserver(new Handler()) { // from class: com.vivo.services.touchscreen.TouchScreenService.9
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor(TouchScreenService.VK_LONGPRESS).equals(uri)) {
                int enable = Settings.System.getIntForUser(TouchScreenService.this.mContext.getContentResolver(), TouchScreenService.VK_LONGPRESS, 0, -2);
                TouchScreenService.this.mMultiTouchController.setState(TouchScreenService.this.mMainDisplayMonitor.getDisplayId(), 15, enable);
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "System Virtual key longpress is " + enable);
                }
            }
        }
    };
    private final ContentObserver mVirtualGameKeyObserver = new ContentObserver(new Handler()) { // from class: com.vivo.services.touchscreen.TouchScreenService.10
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor(TouchScreenService.VIRTUAL_GAMEKEY).equals(uri)) {
                int enable = Settings.System.getIntForUser(TouchScreenService.this.mContext.getContentResolver(), TouchScreenService.VIRTUAL_GAMEKEY, 0, -2);
                TouchScreenService.this.mMultiTouchController.setState(TouchScreenService.this.mMainDisplayMonitor.getDisplayId(), 23, enable);
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "System Virtual Gamekey is " + enable);
                }
            }
        }
    };
    private final ContentObserver mInputMethodObserver = new ContentObserver(new Handler()) { // from class: com.vivo.services.touchscreen.TouchScreenService.11
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor(TouchScreenService.INPUT_METHOD_STATE).equals(uri)) {
                int enable = Settings.System.getInt(TouchScreenService.this.mContext.getContentResolver(), TouchScreenService.INPUT_METHOD_STATE, 0);
                TouchScreenService.this.mMultiTouchController.setState(TouchScreenService.this.mMainDisplayMonitor.getDisplayId(), 25, enable);
                EdgeRejectionConfigure edgeRejectionConfigure = TouchScreenService.this.mEdgeRejectionConfigure;
                EdgeRejectionConfigure unused = TouchScreenService.this.mEdgeRejectionConfigure;
                EdgeRejectionConfigure unused2 = TouchScreenService.this.mEdgeRejectionConfigure;
                edgeRejectionConfigure.setEdgeRejectionPara(0, 4, enable);
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "System input method is " + enable);
                }
            }
        }
    };
    PhoneStateListener mPhoneStateListener = new PhoneStateListener() { // from class: com.vivo.services.touchscreen.TouchScreenService.12
        @Override // android.telephony.PhoneStateListener
        public void onCallStateChanged(int state, String incomingNumber) {
            if (TouchScreenService.DBG) {
                VSlog.d(TouchScreenService.TAG, "onCallStateChanged state is" + state);
            }
            if (state == 0) {
                TouchScreenService.this.SetAppName("VivoPhoneState:0".getBytes());
                TouchScreenService.this.mMultiTouchController.setStateForAll(9, 0);
                TouchScreenService.this.mMultiTouchController.setStateForAll(17, 0);
            } else if (state == 1) {
                TouchScreenService.this.SetAppName("VivoPhoneState:1".getBytes());
                TouchScreenService.this.mMultiTouchController.setStateForAll(9, 1);
            } else if (state == 2) {
                TouchScreenService.this.SetAppName("VivoPhoneState:2".getBytes());
                TouchScreenService.this.mMultiTouchController.setStateForAll(17, 1);
            }
        }
    };
    private boolean wzrySwitch = false;
    private final String vivoTsUpdateAction = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_VTSG";
    private final String vivoTsUri = "content://com.vivo.daemonservice.unifiedconfigprovider/configs";
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.vivo.services.touchscreen.TouchScreenService.16
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            try {
                if (foregroundActivities) {
                    TouchScreenService.this.PACKAGE_NAME = TouchScreenService.this.getAppNameFromUid(uid);
                    VSlog.d(TouchScreenService.TAG, "foreground: " + TouchScreenService.this.PACKAGE_NAME);
                    TouchScreenService.access$4008(TouchScreenService.this);
                } else {
                    TouchScreenService.this.PACKAGE_NAME_BACK = TouchScreenService.this.getAppNameFromUid(uid);
                    VSlog.d(TouchScreenService.TAG, "background: " + TouchScreenService.this.PACKAGE_NAME_BACK);
                    TouchScreenService.access$4008(TouchScreenService.this);
                }
                if (TouchScreenService.this.package_num >= 2 && TouchScreenService.this.PACKAGE_NAME != TouchScreenService.this.PACKAGE_NAME_BACK) {
                    TouchScreenService.this.package_num = 0;
                    int i = 0;
                    while (true) {
                        if (i >= 7) {
                            break;
                        } else if (TouchScreenService.this.PACKAGE_NAME != TouchScreenService.this.package_name[i]) {
                            i++;
                        } else {
                            VSlog.d(TouchScreenService.TAG, "PACKAGE_NAME" + TouchScreenService.this.PACKAGE_NAME);
                            EdgeRejectionConfigure edgeRejectionConfigure = TouchScreenService.this.mEdgeRejectionConfigure;
                            EdgeRejectionConfigure unused = TouchScreenService.this.mEdgeRejectionConfigure;
                            EdgeRejectionConfigure unused2 = TouchScreenService.this.mEdgeRejectionConfigure;
                            edgeRejectionConfigure.setEdgeRejectionPara(0, 3, 1);
                            break;
                        }
                    }
                    if (i == 7) {
                        EdgeRejectionConfigure edgeRejectionConfigure2 = TouchScreenService.this.mEdgeRejectionConfigure;
                        EdgeRejectionConfigure unused3 = TouchScreenService.this.mEdgeRejectionConfigure;
                        EdgeRejectionConfigure unused4 = TouchScreenService.this.mEdgeRejectionConfigure;
                        edgeRejectionConfigure2.setEdgeRejectionPara(0, 3, 0);
                    }
                }
            } catch (Exception e) {
                VSlog.d(TouchScreenService.TAG, "Failed in TouchscreenSetAppCode");
            }
        }

        public void onProcessStateChanged(int pid, int uid, int importance) {
        }

        public void onProcessDied(int pid, int uid) {
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    };
    private final ContentObserver mEasyShareForceBrightnessOffStateObserver = new ContentObserver(new Handler()) { // from class: com.vivo.services.touchscreen.TouchScreenService.18
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            TouchScreenService touchScreenService = TouchScreenService.this;
            touchScreenService.mIsInEasyShareForceBrightnessOffState = Settings.System.getInt(touchScreenService.mContext.getContentResolver(), TouchScreenService.EASY_SHARE_FORCE_BRIGHTNESS_OFF, 0) == 1;
            if (TouchScreenService.DBG) {
                VSlog.d(TouchScreenService.TAG, "easy share force brightness off state is " + TouchScreenService.this.mIsInEasyShareForceBrightnessOffState);
            }
        }
    };

    private static native int nativeCallingSwitch(int i);

    private static native int nativeGetDriverICName();

    private static native void nativeInit();

    private static native int nativeSensorRxTx();

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeSetAppName(byte[] bArr);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeSetFingerGestureSwitch(int i);

    private static native int nativeTouchDclickSwitch(int i);

    private static native int nativeTouchGestureLetterSwitch(int i);

    private static native int nativeTouchGestureSignSwitch(int i);

    private static native int nativeTouchGestureSwitch(int i);

    private static native int nativeTouchGestureSwitchexport(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeTouchScreenDclickEnable(int i);

    private static native int nativeTouchScreenDclickSimulateSwitch(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeTouchScreenEdgeSuppressSwitch(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeTouchScreenGlovesModeSwitch(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeTouchScreenLcdStateSet(int i);

    private static native int nativeTouchSwipeWakeupSwitch(int i);

    private static native int nativeUdgClearTemplate();

    private static native int nativeUdgGetAttn();

    private static native int nativeUdgGetCoordinates(byte[] bArr);

    private static native int nativeUdgGetDetectionScore();

    private static native int nativeUdgGetGesturePoints(byte[] bArr);

    private static native int nativeUdgGetGesturePointsLength();

    private static native int nativeUdgGetMatchScore(byte[] bArr, byte[] bArr2);

    private static native int nativeUdgGetMaxNumberSigs();

    private static native int nativeUdgGetMaxSigLength();

    private static native int nativeUdgGetRegistrationStatus();

    private static native int nativeUdgGetScore();

    private static native int nativeUdgGetTemplateData(float[] fArr, float[] fArr2, byte[] bArr);

    private static native int nativeUdgGetTemplateSize();

    private static native int nativeUdgGetThreshold();

    private static native int nativeUdgGetTraceData(int[] iArr, int[] iArr2, byte[] bArr);

    private static native int nativeUdgGetTraceSize();

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeUdgGetgestureEnable();

    private static native byte nativeUdgReadDetection();

    private static native int nativeUdgReadDetectionIndex();

    private static native int nativeUdgReadIndex();

    private static native int nativeUdgReadSignature(byte[] bArr);

    private static native int nativeUdgReadTemplateDetection();

    private static native int nativeUdgReadTemplateMaxIndex();

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeUdgReadTemplateValid();

    private static native int nativeUdgSetDetectionEnable(int i);

    private static native int nativeUdgSetEnable(int i);

    private static native int nativeUdgSetEngineEnable(int i);

    private static native int nativeUdgSetEnroll(int i);

    private static native int nativeUdgSetMode(int i);

    private static native int nativeUdgSetRegistrationBegin(int i);

    private static native int nativeUdgSetRegistrationEnable(int i);

    private static native int nativeUdgSetTemplateValid(int i);

    private static native int nativeUdgSetThreshold(int i);

    private static native int nativeUdgSetgestureEnable(int i);

    private static native int nativeUdgWriteIndex(int i);

    private static native int nativeUdgWriteSignature(byte[] bArr);

    private static native int nativeUdgWriteTemplateData(float[] fArr, float[] fArr2, byte[] bArr);

    private static native int nativeUdgWriteTemplateIndex(char c);

    static /* synthetic */ int access$4008(TouchScreenService x0) {
        int i = x0.package_num;
        x0.package_num = i + 1;
        return i;
    }

    public void touchScreenSetFingerprint(boolean isSupport, int fingerUnlock) {
        this.isSupportUDFingerprint = isSupport;
        VSlog.d(TAG, " set isSupportUDFingerprint:" + isSupport + " fingerUnlock:" + fingerUnlock);
        if (this.isSupportUDFingerprint) {
            if (isDoubleScreenProject()) {
                this.mMultiTouchController.setStateForAll(11, fingerUnlock);
            } else {
                this.mMultiTouchController.setState(0, 11, fingerUnlock);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isDoubleScreenProject() {
        String model = SystemProperties.get("ro.product.model.bbk", "unkown").toLowerCase();
        if (mDoubleScreenProjects.length > 0) {
            int i = 0;
            while (true) {
                String[] strArr = mDoubleScreenProjects;
                if (i < strArr.length) {
                    if (!model.startsWith(strArr[i])) {
                        i++;
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    /* loaded from: classes.dex */
    private class ProximitySensorMonitor implements SensorEventListener {
        private int mDisplayId;
        private Sensor mSensor;
        private boolean mSensorNear = false;
        private int mSensorType;

        private boolean isSensorNear(float distance) {
            if (distance >= 0.0d && distance < TouchScreenService.PROXIMITY_THRESHOLD && distance < this.mSensor.getMaximumRange()) {
                return true;
            }
            return false;
        }

        public ProximitySensorMonitor(SensorManager sensorManager, int sensorType, int displayId) {
            HandlerThread ht = new HandlerThread("dual prox handler");
            ht.start();
            Sensor defaultSensor = sensorManager.getDefaultSensor(sensorType);
            this.mSensor = defaultSensor;
            this.mDisplayId = displayId;
            this.mSensorType = sensorType;
            sensorManager.registerListener(this, defaultSensor, 3, new Handler(ht.getLooper()));
        }

        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            boolean sensorNear = isSensorNear(event.values[0]);
            if (TouchScreenService.DBG) {
                VSlog.i(TouchScreenService.TAG, " mSensorNear:" + this.mSensorNear + " sensorNear:" + sensorNear + " SensorType:" + this.mSensorType + " mDisplayId:" + this.mDisplayId);
            }
            if (this.mSensorNear != sensorNear) {
                if (sensorNear) {
                    TouchScreenService.this.mMultiTouchController.setState(this.mDisplayId, 1, 0);
                } else {
                    TouchScreenService.this.mMultiTouchController.setState(this.mDisplayId, 1, 1);
                }
                this.mSensorNear = sensorNear;
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    public static boolean isInGameMode(Context context) {
        ContentResolver resolver = context.getContentResolver();
        int gameModeValue = Settings.System.getIntForUser(resolver, "is_game_mode", 0, -2);
        return 1 == gameModeValue;
    }

    public static boolean isInGameSpace(Context context) {
        ContentResolver resolver = context.getContentResolver();
        int gameSpaceValue = Settings.Global.getInt(resolver, "is_game_space", 0);
        return 1 == gameSpaceValue;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class CamereaReceiver extends BroadcastReceiver {
        CamereaReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VSlog.e(TouchScreenService.TAG, "packageName:" + intent.getStringExtra("packageName"));
            if (action.equals(TouchScreenService.ACTION_OPEN_CAMERA) && "com.android.camera".equals(intent.getStringExtra("packageName"))) {
                VSlog.d(TouchScreenService.TAG, "OPEN_CAMERA broadcast received");
                TouchScreenService.this.mMultiTouchController.setState(0, 10, 1);
                VSlog.d(TouchScreenService.TAG, "camera is opened, support double tap.");
            } else if (action.equals(TouchScreenService.ACTION_CLOSE_CAMERA)) {
                VSlog.d(TouchScreenService.TAG, "CLOSE_CAMERA broadcast received");
                TouchScreenService.this.mMultiTouchController.setState(0, 10, 0);
                VSlog.d(TouchScreenService.TAG, "camera is closed, not support double tap.");
            } else if (action.equals(TouchScreenService.ACTION_WHITELIST_CLOSE_CAMERA)) {
                VSlog.d(TouchScreenService.TAG, "WHITELIST_CLOSE_CAMERA broadcast received");
                TouchScreenService.this.mMultiTouchController.setState(0, 10, 0);
                VSlog.d(TouchScreenService.TAG, "camera is crashed, not support double tap.");
            }
        }
    }

    /* loaded from: classes.dex */
    private final class SettingsObserver extends ContentObserver {
        private Context mContext;
        int mFingerUnlock;

        public SettingsObserver(Context context) {
            super(new Handler());
            this.mFingerUnlock = 0;
            this.mContext = context;
            ContentResolver cr = context.getContentResolver();
            cr.registerContentObserver(TouchScreenService.urlFingerUnlock, true, this, -1);
            this.mFingerUnlock = Settings.System.getIntForUser(this.mContext.getContentResolver(), "finger_unlock_open", 0, -2);
            VSlog.d(TouchScreenService.TAG, "fingerUnlock:" + this.mFingerUnlock);
            if (TouchScreenService.this.isSupportUDFingerprint) {
                if (TouchScreenService.isDoubleScreenProject()) {
                    TouchScreenService.this.mMultiTouchController.setStateForAll(11, this.mFingerUnlock);
                } else {
                    TouchScreenService.this.mMultiTouchController.setState(0, 11, this.mFingerUnlock);
                }
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (TouchScreenService.urlFingerUnlock.equals(uri)) {
                this.mFingerUnlock = Settings.System.getIntForUser(this.mContext.getContentResolver(), "finger_unlock_open", 0, -2);
                VSlog.d(TouchScreenService.TAG, "fingerUnlock:" + this.mFingerUnlock);
                if (TouchScreenService.this.isSupportUDFingerprint) {
                    if (TouchScreenService.isDoubleScreenProject()) {
                        TouchScreenService.this.mMultiTouchController.setStateForAll(11, this.mFingerUnlock);
                    } else {
                        TouchScreenService.this.mMultiTouchController.setState(0, 11, this.mFingerUnlock);
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    class HallLockReceiver extends BroadcastReceiver {
        HallLockReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            synchronized (TouchScreenService.this.mLock) {
                if (action.equals(TouchScreenService.HALL_LOCK_BROADCAST_ACTION)) {
                    if (TouchScreenService.DBG) {
                        VSlog.d(TouchScreenService.TAG, "Hall lock recive");
                    }
                    TouchScreenService.this.RegisterProximityListener(false);
                    TouchScreenService.this.mHallLockEnabled = true;
                    if (TouchScreenService.this.mHasGesturesEnabled) {
                        TouchScreenService.nativeTouchScreenDclickEnable(0);
                        TouchScreenService.this.mHasGesturesEnabled = false;
                    }
                } else if (action.equals(TouchScreenService.HALL_UNLOCK_BROADCAST_ACTION)) {
                    if (TouchScreenService.DBG) {
                        VSlog.d(TouchScreenService.TAG, "Hall unlock recive");
                    }
                    TouchScreenService.this.mHallLockEnabled = false;
                } else if (action.equals(TouchScreenService.SUPER_POWER_SAVE_BROADCAST_ACTION)) {
                    if ("exited".equals(intent.getStringExtra("sps_action"))) {
                        VSlog.d(TouchScreenService.TAG, "exited: mIsScreenOn = " + TouchScreenService.this.mIsScreenOn + " mHasGesturesEnabled = " + TouchScreenService.this.mHasGesturesEnabled);
                        PowerManager powerManager = (PowerManager) TouchScreenService.this.mContext.getSystemService("power");
                        if (!TouchScreenService.this.mIsScreenOn) {
                            VSlog.d(TouchScreenService.TAG, "exited: isScreen off");
                            TouchScreenService.this.TouchscreenLcdBacklightStateSet(false);
                        } else {
                            VSlog.d(TouchScreenService.TAG, "exited: isScreen on");
                        }
                        TouchScreenService.this.mMultiTouchController.setStateForAll(27, 0);
                    } else if ("entered".equals(intent.getStringExtra("sps_action"))) {
                        VSlog.d(TouchScreenService.TAG, "entered: super power save");
                        TouchScreenService.this.mMultiTouchController.setStateForAll(27, 1);
                    }
                } else if (action.equals(TouchScreenService.ACTION_FM_RECORDING_STATUS)) {
                    VSlog.d(TouchScreenService.TAG, "FMRedioReceiver action \n");
                    int state = intent.getIntExtra("state", 0);
                    if (state == 1) {
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "FMRedioReceiver on \n");
                        }
                        TouchScreenService.this.SetAppName("FM_ON".getBytes());
                    } else if (state == 0) {
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "FMRedioReceiver off \n");
                        }
                        TouchScreenService.this.SetAppName("FM_OFF".getBytes());
                    }
                } else if (action.equals("android.intent.action.HEADSET_PLUG")) {
                    if (intent.getIntExtra("state", 0) == 0) {
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "Headset disconnect, FM off");
                        }
                        TouchScreenService.this.SetAppName("FM_OFF".getBytes());
                    }
                } else if (action.equals("vivo.intent.action.VIVO_SCREEN_SHOT")) {
                    int screenshotType = intent.getIntExtra("screen_shot_type", 0);
                    int triggerType = intent.getIntExtra("trigger_type", 0);
                    VSlog.d(TouchScreenService.TAG, "screen shot! shotType: " + screenshotType + "triggerType:" + triggerType);
                    if (screenshotType == 1 && triggerType == 4) {
                        TouchScreenService.this.mMultiTouchController.setStateForAll(12, 1);
                    }
                } else if (action.equals(TouchScreenService.ACTION_INPUT_METHOD_STATUS)) {
                    VSlog.d(TouchScreenService.TAG, "input method action \n");
                    int inputmethodTpye = intent.getIntExtra("status", 0);
                    if (inputmethodTpye == 1) {
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "input method on\n");
                        }
                        TouchScreenService.this.SetAppName("INPUT_METHOD_ON".getBytes());
                    } else if (inputmethodTpye == 0) {
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "input method off\n");
                        }
                        TouchScreenService.this.SetAppName("INPUT_METHOD_OFF".getBytes());
                    }
                }
            }
        }
    }

    int GetGesturesSwitchState() {
        int GesturesSettings = 0;
        int i = 0;
        while (true) {
            String[] strArr = SystemGesturesSettings;
            if (i < strArr.length) {
                if (this.isSupportUDFingerprint || !strArr[i].equals("finger_unlock_open")) {
                    int TempSetting = Settings.System.getIntForUser(this.mContext.getContentResolver(), SystemGesturesSettings[i], 0, -2);
                    if (i == 0 && Settings.Secure.getIntForUser(this.mContext.getContentResolver(), BBK_SCREEN_DISABLE_CARD_SLIDE_SETTING, 0, -2) == 1) {
                        TempSetting = 1;
                    }
                    if (DBG) {
                        VSlog.d(TAG, "System Setting " + SystemGesturesSettings[i] + " is " + TempSetting);
                    }
                    GesturesSettings |= TempSetting << i;
                }
                i++;
            } else {
                return GesturesSettings & (-16385) & (-32769);
            }
        }
    }

    int GetGesturesSwitchState(int display_id) {
        boolean scrawlEnable = SystemProperties.getInt("ro.vivo.scrawl", 0) == 1;
        boolean moodEnable = SystemProperties.getInt("ro.vivo.mood.trans", 0) == 1;
        int GesturesSettings = GetGesturesSwitchState();
        if (moodEnable && 1 == Settings.System.getIntForUser(this.mContext.getContentResolver(), "vivo_mood_picture_setting_enable", 1, -2)) {
            int TempSetting = Settings.System.getIntForUser(this.mContext.getContentResolver(), "vivo_mood_picture_gesture_mode", 0, -2);
            if (TempSetting != 0) {
                if (TempSetting != 1) {
                    if (TempSetting == 2) {
                        GesturesSettings |= BBK_TP_GESTURE_LETTER_V_SWITCH;
                    }
                } else if (display_id != 0) {
                    GesturesSettings |= BBK_TP_GESTURE_LETTER_V_SWITCH;
                }
            } else if (display_id == 0) {
                GesturesSettings |= BBK_TP_GESTURE_LETTER_V_SWITCH;
            }
        }
        if (scrawlEnable && 1 == Settings.System.getIntForUser(this.mContext.getContentResolver(), "vivo_scrawl_setting_enable", 1, -2)) {
            int TempSetting2 = Settings.System.getIntForUser(this.mContext.getContentResolver(), "vivo_scrawl_gesture_mode", 0, -2);
            if (TempSetting2 == 0) {
                if (display_id == 0) {
                    return GesturesSettings | BBK_TP_GESTURE_LETTER_H_SWITCH;
                }
                return GesturesSettings;
            } else if (TempSetting2 != 1) {
                if (TempSetting2 == 2) {
                    return GesturesSettings | BBK_TP_GESTURE_LETTER_H_SWITCH;
                }
                return GesturesSettings;
            } else if (display_id != 0) {
                return GesturesSettings | BBK_TP_GESTURE_LETTER_H_SWITCH;
            } else {
                return GesturesSettings;
            }
        }
        return GesturesSettings;
    }

    void RegisterProximityListener(boolean on) {
        if (DBG) {
            VSlog.d(TAG, "The mProximiteListenerRegistered is " + this.mProximiteListenerRegistered + " on is " + on);
        }
        this.mProximited = -1;
        if (on) {
            if (this.mProximiteListenerRegistered != 1) {
                this.mProximiteListenerRegistered = 1;
                this.mSensorManager.registerListener(this.mProximityListener, this.mProximitySensor, 3, this.mHandler);
            }
        } else if (this.mProximiteListenerRegistered != 0) {
            this.mProximiteListenerRegistered = 0;
            this.mSensorManager.unregisterListener(this.mProximityListener);
        }
    }

    void SetNativeGesturesSwitchState(int GesturesSettings) {
        int GestureSignSetting = 0;
        int SettingBitmap = this.mGesturesSettingSave ^ GesturesSettings;
        if ((SettingBitmap & 256) != 0) {
            if ((GesturesSettings & 256) > 0) {
                nativeTouchDclickSwitch(1);
            } else {
                nativeTouchDclickSwitch(0);
            }
        }
        if ((SettingBitmap & 128) != 0) {
            if ((GesturesSettings & 128) > 0) {
                nativeTouchSwipeWakeupSwitch(1);
            } else {
                nativeTouchSwipeWakeupSwitch(0);
            }
        }
        if ((SettingBitmap & 1536) != 0 || (SettingBitmap & 4096) != 0) {
            int tempsetting = GesturesSettings & 1536;
            if (DBG) {
                VSlog.d(TAG, "before add finger switch,tempsetting=" + tempsetting);
            }
            int tempsetting2 = tempsetting | ((GesturesSettings & 4096) >> 4);
            if (DBG) {
                VSlog.d(TAG, "after add finger switch,tempsetting=" + tempsetting2);
            }
            nativeTouchGestureSwitchexport(tempsetting2);
        }
        if (DBG) {
            VSlog.d(TAG, "SettingBitmap:" + SettingBitmap + "mGesturesSettingSave:" + this.mGesturesSettingSave);
        }
        if ((SettingBitmap & 2175) != 0) {
            int tempsetting3 = (GesturesSettings & KernelConfig.FRC_LOW_LATENCY) | ((GesturesSettings & 2048) >> 4);
            if (DBG) {
                VSlog.d(TAG, "set gesture_switch:" + tempsetting3);
            }
            nativeTouchGestureSwitch(tempsetting3);
        }
        if ((SettingBitmap & 1) != 0 && (GesturesSettings & 1) > 0) {
            GestureSignSetting = 0 + 3;
        }
        if ((SettingBitmap & 2) != 0 && (GesturesSettings & 2) > 0) {
            GestureSignSetting += 4;
        }
        nativeTouchGestureSignSwitch(GestureSignSetting);
        if (((SettingBitmap >> 2) & 31) != 0) {
            nativeTouchGestureLetterSwitch((GesturesSettings >> 2) & 31);
        }
        this.mGesturesSettingSave = GesturesSettings;
    }

    public void TouchscreenAccStateSet(int isLandscape) {
        if (DBG) {
            VSlog.d(TAG, "set isLandscape  " + isLandscape);
        }
        this.mNewHandler.obtainMessage(3, isLandscape, 0).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void TouchscreenAccStateSet(int displayId, int rotation) {
        this.mMultiTouchController.setState(displayId, 3, rotation);
    }

    public int TouchscreenSetFingerGestureSwitch(int state) {
        this.mTsLcdStateHandler.obtainMessage(2, state, 0).sendToTarget();
        if (state == 6) {
            this.mMultiTouchController.setState(0, 5, 1);
        } else if (state == 7) {
            this.mMultiTouchController.setState(0, 5, 0);
        } else if (state == 8) {
            this.mMultiTouchController.setState(0, 24, 1);
        } else if (state == 9) {
            this.mMultiTouchController.setState(0, 24, 0);
        }
        return 0;
    }

    public void TouchscreenLcdBacklightStateSet(boolean isScreenOn) {
        String lcdState = isScreenOn ? "On" : "Off";
        if (DBG) {
            VSlog.d(TAG, "Get lcd status " + lcdState);
        }
        Message msg = this.mTsLcdStateHandler.obtainMessage();
        msg.what = isScreenOn ? 1 : 0;
        this.mTsLcdStateHandler.sendMessage(msg);
    }

    public void TouchscreenForceNormalEnable(int displayId, int enable) {
        this.mMultiTouchController.setState(displayId, 8, enable);
    }

    public void TouchscreenLcdBacklightStateSetEx(int displayId, int state, int brightness) {
        if (DBG) {
            VSlog.d(TAG, "displayId:" + displayId + " state:" + state + " brightness: " + brightness);
        }
        DisplayMonitor displayMonitor = this.mMainDisplayMonitor;
        if (displayMonitor != null && displayMonitor.getDisplayId() == displayId) {
            this.mMainDisplayMonitor.setBrightness(brightness);
            return;
        }
        DisplayMonitor displayMonitor2 = this.mSecondDisplayMonitor;
        if (displayMonitor2 != null) {
            displayMonitor2.setBrightness(brightness);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void TouchscreenLcdBacklightStateSet(int displayId, boolean isScreenOn) {
        this.mMultiTouchController.setState(displayId, 0, isScreenOn ? 1 : 0);
        if (isScreenOn) {
            this.mMultiTouchController.setState(displayId, 10, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void TouchscreenSetGestureBitmap() {
        DisplayMonitor displayMonitor = this.mMainDisplayMonitor;
        if (displayMonitor != null) {
            this.mMultiTouchController.setState(displayMonitor.getDisplayId(), 2, GetGesturesSwitchState(this.mMainDisplayMonitor.getDisplayId()));
        }
        DisplayMonitor displayMonitor2 = this.mSecondDisplayMonitor;
        if (displayMonitor2 != null) {
            this.mMultiTouchController.setState(displayMonitor2.getDisplayId(), 2, GetGesturesSwitchState(this.mSecondDisplayMonitor.getDisplayId()));
        }
    }

    private void TouchscreenSetScreenClock() {
        int enable = Settings.System.getIntForUser(this.mContext.getContentResolver(), SCREEN_CLOCK_SWITCH, 0, -2);
        this.mMultiTouchController.setState(this.mMainDisplayMonitor.getDisplayId(), 19, enable);
        if (1 == enable) {
            this.mScreenclockSetting = 1;
            SetAppName("SCREEN_CLOCK_SWITCH_ON".getBytes());
        } else {
            this.mScreenclockSetting = 0;
            SetAppName("SCREEN_CLOCK_SWITCH_OFF".getBytes());
        }
        if (DBG) {
            VSlog.d(TAG, "power on System Screen clock switch is " + enable);
        }
    }

    public int TouchScreenDclickSimulateSwitch(int on) {
        return nativeTouchScreenDclickSimulateSwitch(on);
    }

    public int TouchScreenGlovesModeSwitch(int on) {
        return nativeTouchScreenGlovesModeSwitch(on);
    }

    public int TouchScreenCallingSwitch(int on) {
        return nativeCallingSwitch(on);
    }

    public int TouchScreenUserDefineGestureSetThreshold(int setting) {
        return nativeUdgSetThreshold(setting);
    }

    public int TouchScreenUserDefineGestureWriteIndex(int setting) {
        return nativeUdgWriteIndex(setting);
    }

    public int TouchScreenUserDefineGestureWriteSignature(byte[] signature) {
        return nativeUdgWriteSignature(signature);
    }

    public int TouchScreenUserDefineGestureSetgestureEnable(int setting) {
        return nativeUdgSetgestureEnable(setting);
    }

    public int TouchScreenUserDefineGestureSetEnroll(int setting) {
        return nativeUdgSetEnroll(setting);
    }

    public int TouchScreenUserDefineGestureSetEnable(int setting) {
        return nativeUdgSetEnable(setting);
    }

    public int TouchScreenUserDefineGestureSetMode(int setting) {
        return nativeUdgSetMode(setting);
    }

    public int TouchScreenUserDefineGestureGetAttn() {
        return nativeUdgGetAttn();
    }

    public int TouchScreenUserDefineGestureGetThreshold() {
        return nativeUdgGetThreshold();
    }

    public int TouchScreenUserDefineGestureGetScore() {
        return nativeUdgGetScore();
    }

    public int TouchScreenUserDefineReadIndex() {
        return nativeUdgReadIndex();
    }

    public int TouchScreenUserDefineReadSignature(byte[] signature) {
        return nativeUdgReadSignature(signature);
    }

    public byte TouchScreenUserDefineReadDetection() {
        return nativeUdgReadDetection();
    }

    public int TouchScreenUserDefineGetMaxSigLength() {
        return nativeUdgGetMaxSigLength();
    }

    public int TouchScreenUserDefineGetMaxNumberSigs() {
        return nativeUdgGetMaxNumberSigs();
    }

    public int TouchScreenUserDefineGetgestureEnable() {
        return nativeUdgGetgestureEnable();
    }

    public int TouchScreenUserDefineGetCoordinates(byte[] coordinates) {
        return nativeUdgGetCoordinates(coordinates);
    }

    public int TouchScreenUserDefineGetGesturePoints(byte[] points) {
        return nativeUdgGetGesturePoints(points);
    }

    public int TouchScreenUserDefineGetGesturePointsLength() {
        return nativeUdgGetGesturePointsLength();
    }

    public int TouchScreenUserDefineGetMatchScore(byte[] signature1, byte[] signature2) {
        return nativeUdgGetMatchScore(signature1, signature2);
    }

    public int TouchscreenUserDefineGestureSetEngineEnable(int setting) {
        return nativeUdgSetEngineEnable(setting);
    }

    public int TouchscreenUserDefineGestureSetDetectionEnable(int setting) {
        return nativeUdgSetDetectionEnable(setting);
    }

    public int TouchscreenUserDefineGestureSetRegistrationEnable(int setting) {
        return nativeUdgSetRegistrationEnable(setting);
    }

    public int TouchscreenUserDefineGestureSetRegistrationBegin(int setting) {
        return nativeUdgSetRegistrationBegin(setting);
    }

    public int TouchscreenUserDefineGestureWriteTemplateIndex(char index) {
        return nativeUdgWriteTemplateIndex(index);
    }

    public int TouchscreenUserDefineGestureSetTemplateValid(int setting) {
        return nativeUdgSetTemplateValid(setting);
    }

    public int TouchscreenUserDefineGestureClearTemplate() {
        return nativeUdgClearTemplate();
    }

    public int TouchscreenUserDefineGestureReadDetectionIndex() {
        return nativeUdgReadDetectionIndex();
    }

    public int TouchscreenUserDefineGestureGetDetectionScore() {
        return nativeUdgGetDetectionScore();
    }

    public int TouchscreenUserDefineGestureGetRegistrationStatus() {
        return nativeUdgGetRegistrationStatus();
    }

    public int TouchscreenUserDefineGestureGetTemplateSize() {
        return nativeUdgGetTemplateSize();
    }

    public int TouchscreenUserDefineGestureReadTemplateMaxIndex() {
        return nativeUdgReadTemplateMaxIndex();
    }

    public int TouchscreenUserDefineGestureReadTemplateDetection() {
        return nativeUdgReadTemplateDetection();
    }

    public int TouchscreenUserDefineGestureReadTemplateValid() {
        return nativeUdgReadTemplateValid();
    }

    public int TouchscreenUserDefineGestureGetTraceSize() {
        return nativeUdgGetTraceSize();
    }

    public int TouchscreenUserDefineGestureGetTraceData(int[] x_trace, int[] y_trace, byte[] segments) {
        return nativeUdgGetTraceData(x_trace, y_trace, segments);
    }

    public int TouchscreenUserDefineGestureGetTemplateData(float[] data, float[] scalefac, byte[] segments) {
        return nativeUdgGetTemplateData(data, scalefac, segments);
    }

    public int TouchscreenUserDefineGestureWriteTemplateData(float[] data, float[] scalefac, byte[] segments) {
        return nativeUdgWriteTemplateData(data, scalefac, segments);
    }

    public int TouchSensorRxTx() {
        return nativeSensorRxTx();
    }

    public int TouchScreenGetDriverICName() {
        return nativeGetDriverICName();
    }

    public int touchScreenClockZoneSet(int[] coordinate) {
        if (coordinate.length != 4) {
            VSlog.e(TAG, "invalid coordinate, length is error, expect length is 4");
            return 0;
        }
        if (DBG) {
            VSlog.d(TAG, "coordinate is" + coordinate[0] + coordinate[1] + coordinate[2] + coordinate[3]);
        }
        StringBuffer sb = new StringBuffer();
        sb.append("vts_clock_area_cmd:");
        for (int i = 0; i < coordinate.length - 1; i++) {
            sb.append(coordinate[i]);
            sb.append(",");
        }
        int i2 = coordinate.length;
        sb.append(coordinate[i2 - 1]);
        return SetAppName(sb.toString().getBytes());
    }

    public int touchScreenEdgeRejectZoneSet(int index, int flag, int[] coordinate, boolean enable) {
        if (coordinate.length != 4) {
            VSlog.e(TAG, "invalid coordinate, length is error");
            return 0;
        }
        if (DBG) {
            VSlog.d(TAG, "index is " + index + "flag is " + flag + "coordinate is " + coordinate[0] + coordinate[1] + coordinate[2] + coordinate[3] + enable);
        }
        if (index <= 0) {
            VSlog.e(TAG, "invalid index, forbid to set edge reject zone");
            return 0;
        } else if (index == 101) {
            VSlog.d(TAG, "edge rejection set, index is " + index + "flag is " + flag + "coordinate is " + coordinate[0] + coordinate[1] + coordinate[2] + coordinate[3] + enable);
            this.mEdgeRejectionConfigure.EdgeRjectionParaChanged(flag, coordinate, enable);
            return 0;
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("vts_edge_area_cmd:");
            for (int i : coordinate) {
                sb.append(i);
                sb.append(",");
            }
            sb.append(flag);
            sb.append(",");
            if (enable) {
                sb.append("1");
                sb.append(",");
                sb.append(index);
            } else {
                sb.append("0");
                sb.append(",");
                sb.append(index);
            }
            return SetAppName(sb.toString().getBytes());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DisplayMonitor implements DisplayManager.DisplayListener, VivoDisplayStateManager.DisplayContentListener {
        private static final String DISPLAY_EVENT_ADD = "display added";
        private static final String DISPLAY_EVENT_CHANGED = "display changed";
        private static final String DISPLAY_EVENT_REMOVE = " display removed";
        private static final String FACE_MODULE_NAME = "FaceDozeIcon";
        private static final String FINGER_MODULE_NAME = "UDFingerprint";
        private static final String NOTICEUP_MODULE_NAME = "turn_screen_on_notice";
        private int mDisplayId;
        private DisplayManager mDisplayManager;
        private PowerManagerInternal mPowerManagerInternal;
        private VivoDisplayStateManager mVivoDisplayStateManager;
        private int mDisplayOn = -1;
        private int mDisplayRotation = -1;
        private int mBrightness = 1;
        private boolean mFingerIconVisable = false;
        private boolean mFaceIconVisable = false;
        private boolean mNoticeUpIconVisable = false;

        private void setDisplayOff(int displayId) {
            if (displayId != this.mDisplayId) {
                return;
            }
            this.mDisplayOn = 0;
            TouchScreenService.this.TouchscreenSetGestureBitmap();
            TouchScreenService.this.TouchscreenLcdBacklightStateSet(this.mDisplayId, false);
        }

        private void updateDisplayState(Display display) {
            int displayOn;
            int screenState = display.getState();
            int rotation = display.getRotation();
            int displayId = display.getDisplayId();
            if (TouchScreenService.DBG) {
                VSlog.d(TouchScreenService.TAG, "mDisplayId =" + this.mDisplayId + " displayId = " + displayId + " screenState = " + screenState + " rotation = " + rotation + " mBrightness =" + this.mBrightness + " mDisplayOn =" + this.mDisplayOn);
            }
            if (displayId != this.mDisplayId) {
                return;
            }
            if (TouchScreenService.PCshareTouchSupport) {
                PowerManagerInternal powerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
                this.mPowerManagerInternal = powerManagerInternal;
                if (powerManagerInternal != null && powerManagerInternal.handleSmartMirrorOnIfNeeded(false)) {
                    displayOn = screenState == 2 ? 1 : 0;
                } else if (VivoEasyShareManager.SUPPORT_PCSHARE && TouchScreenService.this.mIsInEasyShareForceBrightnessOffState) {
                    displayOn = screenState == 2 ? 1 : 0;
                } else {
                    displayOn = (screenState != 2 || this.mBrightness == 0) ? 0 : 1;
                }
            } else {
                displayOn = (screenState != 2 || this.mBrightness == 0) ? 0 : 1;
            }
            if (this.mDisplayRotation != rotation) {
                if (rotation == 1 || rotation == 3) {
                    TouchScreenService.this.TouchscreenAccStateSet(this.mDisplayId, rotation - 1);
                    TouchScreenService.this.TouchscreenAccStateSet(rotation - 1);
                    EdgeRejectionConfigure edgeRejectionConfigure = TouchScreenService.this.mEdgeRejectionConfigure;
                    EdgeRejectionConfigure unused = TouchScreenService.this.mEdgeRejectionConfigure;
                    EdgeRejectionConfigure unused2 = TouchScreenService.this.mEdgeRejectionConfigure;
                    edgeRejectionConfigure.setEdgeRejectionPara(2, 0, 0);
                    EdgeRejectionConfigure edgeRejectionConfigure2 = TouchScreenService.this.mEdgeRejectionConfigure;
                    EdgeRejectionConfigure unused3 = TouchScreenService.this.mEdgeRejectionConfigure;
                    EdgeRejectionConfigure unused4 = TouchScreenService.this.mEdgeRejectionConfigure;
                    edgeRejectionConfigure2.setEdgeSpecialPara(2, 0);
                } else {
                    TouchScreenService.this.TouchscreenAccStateSet(this.mDisplayId, 1);
                    TouchScreenService.this.TouchscreenAccStateSet(1);
                    EdgeRejectionConfigure edgeRejectionConfigure3 = TouchScreenService.this.mEdgeRejectionConfigure;
                    EdgeRejectionConfigure unused5 = TouchScreenService.this.mEdgeRejectionConfigure;
                    EdgeRejectionConfigure unused6 = TouchScreenService.this.mEdgeRejectionConfigure;
                    edgeRejectionConfigure3.setEdgeRejectionPara(1, 0, 0);
                    EdgeRejectionConfigure edgeRejectionConfigure4 = TouchScreenService.this.mEdgeRejectionConfigure;
                    EdgeRejectionConfigure unused7 = TouchScreenService.this.mEdgeRejectionConfigure;
                    EdgeRejectionConfigure unused8 = TouchScreenService.this.mEdgeRejectionConfigure;
                    edgeRejectionConfigure4.setEdgeSpecialPara(1, 0);
                }
                this.mDisplayRotation = rotation;
            }
            if (TouchScreenService.DBG) {
                VSlog.d(TouchScreenService.TAG, "mDisplayId = " + this.mDisplayId + " mDisplayOn = " + this.mDisplayOn + " displayOn = " + displayOn);
            }
            if (this.mDisplayOn != displayOn) {
                if (displayOn == 1) {
                    TouchScreenService.this.TouchscreenLcdBacklightStateSet(this.mDisplayId, true);
                    if (TouchScreenService.EdgeRejectionConfigureSupport && TouchScreenService.this.mEdgeRejectionSensor != null) {
                        TouchScreenService.this.mSensorManager.registerListener(TouchScreenService.this.mEdgeRejectionSensorListner, TouchScreenService.this.mEdgeRejectionSensor, 3, TouchScreenService.this.mHandler);
                    }
                } else {
                    TouchScreenService.this.TouchscreenSetGestureBitmap();
                    TouchScreenService.this.TouchscreenLcdBacklightStateSet(this.mDisplayId, false);
                    if (TouchScreenService.EdgeRejectionConfigureSupport && TouchScreenService.this.mEdgeRejectionSensor != null) {
                        TouchScreenService.this.mSensorManager.unregisterListener(TouchScreenService.this.mEdgeRejectionSensorListner);
                        TouchScreenService.this.mEdgeRejectionConfigure.closeLcdHandler();
                    }
                }
                this.mDisplayOn = displayOn;
            }
        }

        public void setBrightness(int brightness) {
            Display display = this.mDisplayManager.getDisplay(this.mDisplayId);
            this.mBrightness = brightness;
            if (display != null) {
                updateDisplayState(display);
            }
        }

        public int getDisplayId() {
            return this.mDisplayId;
        }

        public DisplayMonitor(Context context, int displayId) {
            this.mDisplayManager = (DisplayManager) context.getSystemService("display");
            VivoDisplayStateManager vivoDisplayStateManager = (VivoDisplayStateManager) context.getSystemService("vivo_display_state");
            this.mVivoDisplayStateManager = vivoDisplayStateManager;
            this.mDisplayId = displayId;
            DisplayManager displayManager = this.mDisplayManager;
            if (displayManager == null || vivoDisplayStateManager == null) {
                return;
            }
            Display display = displayManager.getDisplay(displayId);
            if (display != null) {
                updateDisplayState(display);
            } else {
                setDisplayOff(displayId);
            }
            HandlerThread ht = new HandlerThread("DisplayMonitor handler");
            ht.start();
            this.mDisplayManager.registerDisplayListener(this, new Handler(ht.getLooper()));
            this.mVivoDisplayStateManager.registerDisplayContentListener(this);
        }

        private void onProcessDisplayEvent(String event, int displayId) {
            Display display = this.mDisplayManager.getDisplay(displayId);
            if (display == null) {
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "display is null, event:" + event + " displayId:" + displayId + " mDisplayId:" + this.mDisplayId);
                }
                setDisplayOff(displayId);
                return;
            }
            updateDisplayState(display);
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
            onProcessDisplayEvent(DISPLAY_EVENT_REMOVE, displayId);
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
            onProcessDisplayEvent(DISPLAY_EVENT_ADD, displayId);
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            onProcessDisplayEvent(DISPLAY_EVENT_CHANGED, displayId);
        }

        public void onDisplayContentChanged(int displayId, boolean globalVisible, String module, boolean moduleVisible) {
            if (TouchScreenService.DBG) {
                VSlog.d(TouchScreenService.TAG, "display :" + displayId + " globalVisible:" + globalVisible + " module:" + module + " moduleVisible:" + moduleVisible);
            }
            if (displayId == this.mDisplayId) {
                if (!TouchScreenService.this.isSupportUDFingerprint && module.equals("UDFingerprint") && this.mFingerIconVisable != moduleVisible) {
                    TouchScreenService.this.mMultiTouchController.setState(displayId, 5, moduleVisible ? 1 : 0);
                    this.mFingerIconVisable = moduleVisible;
                }
                if (module.equals("FaceDozeIcon") && this.mFaceIconVisable != moduleVisible) {
                    TouchScreenService.this.mMultiTouchController.setState(displayId, 6, moduleVisible ? 1 : 0);
                    this.mFaceIconVisable = moduleVisible;
                }
                if (module.equals(NOTICEUP_MODULE_NAME) && this.mNoticeUpIconVisable != moduleVisible) {
                    TouchScreenService.this.mMultiTouchController.setState(displayId, 7, moduleVisible ? 1 : 0);
                    this.mNoticeUpIconVisable = moduleVisible;
                }
            }
        }

        public void onListenerRegistered(List<VivoDisplayModule> primaryDisplayContent, List<VivoDisplayModule> secondaryDisplayContent) {
        }
    }

    public TouchScreenService(Context context) {
        boolean z;
        this.mSpsSwitch = false;
        this.mNeedDownSensitivity = false;
        this.mFactorySwitch = false;
        this.mForceDisable = false;
        this.mFingerModeSupport = false;
        VSlog.i(TAG, "BBKTouchScreenService Service");
        this.mContext = context;
        this.mBatteryManager = (BatteryManager) context.getSystemService("batterymanager");
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mProximitySensor = sensorManager.getDefaultSensor(8);
        VSlog.d(TAG, "TelephonyManager init !!!!!!!!!!!!!!!!!!!!!");
        ((TelephonyManager) context.getSystemService("phone")).listen(this.mPhoneStateListener, 32);
        VSlog.d(TAG, "vivo touch common code v2");
        PCshareTouchSupport = supportPCshareTouch();
        nativeInit();
        VSlog.d(TAG, "new hander thread !!!!!!!!!!!!!!!!!!!!!");
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mNewHandler = new TouchScreenServiceHandler(thread.getLooper());
        this.mMultiTouchController = new MultiTouchController();
        this.mRadioBandSwitch = new RadioBandSwitch();
        this.mEdgeRejectionConfigure = new EdgeRejectionConfigure();
        this.mMainDisplayMonitor = new DisplayMonitor(context, 0);
        if (isDoubleScreenProject()) {
            new ProximitySensorMonitor(this.mSensorManager, SENSORTYPE_PRXOIMITYFRONT, 0);
            new ProximitySensorMonitor(this.mSensorManager, 66550, 4096);
            this.mSecondDisplayMonitor = new DisplayMonitor(context, 4096);
        } else {
            new ProximitySensorMonitor(this.mSensorManager, 8, 0);
        }
        this.mFactorySwitch = SystemProperties.get("persist.sys.factory.mode", "no").equals("yes");
        VSlog.d(TAG, "mFactorySwitch:" + this.mFactorySwitch);
        if (this.mFactorySwitch) {
            this.mMultiTouchController.setStateForAll(18, 1);
        }
        this.mForceDisable = SystemProperties.get("ro.vivo.secscreen", "enable").equalsIgnoreCase("disable");
        VSlog.d(TAG, "mForceDisable:" + this.mForceDisable);
        if (this.mForceDisable) {
            this.mMultiTouchController.setState(4096, 22, 1);
        }
        if (FingerprintConfig.isOpticalFingerprint()) {
            String fingerModeSupport = FtFeature.getFeatureAttribute("vivo.software.fingerprint", "support_unlock_no_dialog", "1");
            this.mFingerModeSupport = fingerModeSupport.equals("1");
            VSlog.d(TAG, "mFingerModeSupport:" + this.mFingerModeSupport);
            if (this.mFingerModeSupport) {
                this.mMultiTouchController.setState(0, 26, 1);
            }
        }
        TouchscreenSetGestureBitmap();
        TouchscreenSetScreenClock();
        HandlerThread handlerThread = new HandlerThread("TS_Service");
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
        HandlerThread handlerThread2 = new HandlerThread("TSLcdState");
        handlerThread2.start();
        this.mTsLcdStateHandler = new Handler(handlerThread2.getLooper()) { // from class: com.vivo.services.touchscreen.TouchScreenService.13
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                int i = msg.what;
                if (i != 0 && i != 1) {
                    if (i == 2) {
                        int state = msg.arg1;
                        TouchScreenService.nativeSetFingerGestureSwitch(state);
                        return;
                    }
                    return;
                }
                int state2 = msg.what;
                Boolean isScreenOn = Boolean.valueOf(state2 != 0);
                TouchScreenService.this.mSpsSwitch = SystemProperties.getBoolean("sys.super_power_save", false);
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "Super power save property is " + TouchScreenService.this.mSpsSwitch + ". msg:" + msg.what);
                }
                synchronized (TouchScreenService.this.mLock) {
                    if (isScreenOn.booleanValue() == TouchScreenService.this.mIsScreenOn && TouchScreenService.this.isLcdBacklightCalled) {
                        return;
                    }
                    if (!TouchScreenService.this.isLcdBacklightCalled) {
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "first called by LcdBacklight after BBKTouchScreenService is create.");
                        }
                        TouchScreenService.this.isLcdBacklightCalled = true;
                    }
                    if (isScreenOn.booleanValue()) {
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "Set LCD backlight state ON");
                        }
                        TouchScreenService.nativeTouchScreenLcdStateSet(1);
                        if (TouchScreenService.this.mNeedDownSensitivity) {
                            if (TouchScreenService.DBG) {
                                VSlog.d(TouchScreenService.TAG, "Need register listener for sensitivity change");
                            }
                            TouchScreenService.this.RegisterProximityListener(false);
                            TouchScreenService.this.RegisterProximityListener(true);
                        } else {
                            TouchScreenService.this.RegisterProximityListener(false);
                        }
                    } else if (TouchScreenService.this.mHallLockEnabled) {
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "Hall lock is enabled");
                        }
                        TouchScreenService.this.RegisterProximityListener(false);
                        TouchScreenService.this.mIsScreenOn = isScreenOn.booleanValue();
                        return;
                    } else {
                        TouchScreenService.this.mGesturesSetting = TouchScreenService.this.GetGesturesSwitchState();
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "mGesturesSetting is " + TouchScreenService.this.mGesturesSetting);
                        }
                        TouchScreenService.this.mUdgesturesSetting = TouchScreenService.nativeUdgGetgestureEnable();
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "mUdgesturesSetting is " + TouchScreenService.this.mUdgesturesSetting);
                        }
                        TouchScreenService.this.mTemplateValid = TouchScreenService.nativeUdgReadTemplateValid();
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "mTemplateValid is " + TouchScreenService.this.mTemplateValid);
                        }
                        if (TouchScreenService.this.mTemplateValid < 0) {
                            TouchScreenService.this.mTemplateValid = 0;
                        }
                        TouchScreenService.this.SetNativeGesturesSwitchState(TouchScreenService.this.mGesturesSetting);
                        if (TouchScreenService.DBG) {
                            VSlog.d(TouchScreenService.TAG, "Set LCD backlight state OFF");
                        }
                        TouchScreenService.nativeTouchScreenLcdStateSet(0);
                        if (!TouchScreenService.this.mNeedDownSensitivity) {
                            if (!TouchScreenService.this.mSpsSwitch && (TouchScreenService.this.mGesturesSetting != 0 || TouchScreenService.this.mUdgesturesSetting == 1 || (TouchScreenService.this.mTemplateValid & 31) != 0 || TouchScreenService.this.mScreenclockSetting == 1)) {
                                TouchScreenService.this.RegisterProximityListener(true);
                            }
                        } else if (!TouchScreenService.this.mSpsSwitch && (TouchScreenService.this.mGesturesSetting != 0 || TouchScreenService.this.mUdgesturesSetting == 1 || (TouchScreenService.this.mTemplateValid & 31) != 0 || TouchScreenService.this.mScreenclockSetting == 1)) {
                            TouchScreenService.this.RegisterProximityListener(false);
                            TouchScreenService.this.RegisterProximityListener(true);
                        } else {
                            TouchScreenService.this.RegisterProximityListener(false);
                        }
                    }
                    TouchScreenService.this.mIsScreenOn = isScreenOn.booleanValue();
                }
            }
        };
        this.mSettingsObserver = new SettingsObserver(context);
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        if (DBG) {
            VSlog.d(TAG, "end !!!!!!!!!!!!!!!!!!!!!");
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(HALL_LOCK_BROADCAST_ACTION);
        filter.addAction(HALL_UNLOCK_BROADCAST_ACTION);
        filter.addAction(SUPER_POWER_SAVE_BROADCAST_ACTION);
        filter.addAction(ACTION_FM_RECORDING_STATUS);
        filter.addAction(ACTION_INPUT_METHOD_STATUS);
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.addAction("vivo.intent.action.VIVO_SCREEN_SHOT");
        this.mContext.registerReceiver(this.mHallLockReceiver, filter);
        if (!SystemProperties.get("ro.product.model.bbk", "other").equalsIgnoreCase("PD1227T") && !SystemProperties.get("ro.product.model.bbk", "other").equalsIgnoreCase("PD1227TG3") && !SystemProperties.get("ro.product.model.bbk", "other").equalsIgnoreCase("PD1227B")) {
            z = false;
        } else {
            z = true;
        }
        this.mNeedDownSensitivity = z;
        this.mSpsSwitch = SystemProperties.getBoolean("sys.super_power_save", false);
        if (this.mNeedDownSensitivity) {
            RegisterProximityListener(true);
        }
        if (DBG) {
            VSlog.d(TAG, "construct function called !!!!!!!!!!!!!!!!!!!!!");
        }
        mIActivityManager = ActivityManagerNative.getDefault();
        if (EdgeRejectionConfigureSupport) {
            registerProcessObserver();
        }
        if (!this.mBatteryManager.isCharging()) {
            this.mMultiTouchController.setStateForAll(4, 0);
        } else {
            this.mMultiTouchController.setStateForAll(4, 1);
        }
        this.mMultiTouchController.setStateForAll(10, 0);
        if (this.mMainDisplayMonitor != null) {
            int enable = Settings.System.getIntForUser(this.mContext.getContentResolver(), VIRTUAL_KEY_GESTURE, 0, -2);
            this.mMultiTouchController.setState(this.mMainDisplayMonitor.getDisplayId(), 14, enable);
            if (enable == 1) {
                this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(VK_LONGPRESS), false, this.mVkLongpressObserver, -1);
                this.mMultiTouchController.setState(this.mMainDisplayMonitor.getDisplayId(), 15, Settings.System.getIntForUser(this.mContext.getContentResolver(), VK_LONGPRESS, 0, -2));
            } else {
                this.mContext.getContentResolver().unregisterContentObserver(this.mVkLongpressObserver);
                this.mMultiTouchController.setState(this.mMainDisplayMonitor.getDisplayId(), 15, 0);
            }
        }
        setupVivoReceiver();
    }

    /* loaded from: classes.dex */
    final class TouchScreenServiceHandler extends Handler {
        public TouchScreenServiceHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 3) {
                int state = msg.arg1;
                TouchScreenService.nativeTouchScreenEdgeSuppressSwitch(state);
            }
        }
    }

    private void setupVivoReceiver() {
        this.mVivoBroadcastReceiver = new BroadcastReceiver() { // from class: com.vivo.services.touchscreen.TouchScreenService.14
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                try {
                    String action = intent.getAction();
                    if (TouchScreenService.DBG) {
                        VSlog.d(TouchScreenService.TAG, "setupVivoReceiver action:" + action);
                    }
                    if (!action.equals("com.vivo.daemonService.unifiedconfig.update_finish_broadcast_VTSG")) {
                        if (action.equals("com.vivo.battlemode.touchscreen")) {
                            int state = ((Integer) intent.getExtra("state", 0)).intValue();
                            if (state == 0) {
                                if (TouchScreenService.DBG) {
                                    VSlog.d(TouchScreenService.TAG, "Exit game mode");
                                }
                                TouchScreenService.this.SetAppName("VivoGameMode:0".getBytes());
                            } else if (state == 1) {
                                if (TouchScreenService.DBG) {
                                    VSlog.d(TouchScreenService.TAG, "Enter game mode");
                                }
                                TouchScreenService.this.SetAppName("VivoGameMode:1".getBytes());
                            }
                        } else if (action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
                            TouchScreenService.this.mMultiTouchController.setStateForAll(4, 0);
                        } else if (action.equals("android.intent.action.ACTION_POWER_CONNECTED")) {
                            TouchScreenService.this.mMultiTouchController.setStateForAll(4, 1);
                        } else if (action.equals(TouchScreenService.ACTION_VIRTUAL_KEY_STATUS)) {
                            VSlog.d(TouchScreenService.TAG, "virtual key to active action \n");
                            int virtualkeyStatus = intent.getIntExtra("status", 0);
                            if (virtualkeyStatus == 1) {
                                if (TouchScreenService.DBG) {
                                    VSlog.d(TouchScreenService.TAG, "virtual key on\n");
                                }
                                TouchScreenService.this.mMultiTouchController.setStateForAll(21, 1);
                            } else if (virtualkeyStatus == 0) {
                                if (TouchScreenService.DBG) {
                                    VSlog.d(TouchScreenService.TAG, "virtual key off\n");
                                }
                                TouchScreenService.this.mMultiTouchController.setStateForAll(21, 0);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        this.mVivoIntentFilter = intentFilter;
        intentFilter.addAction("com.vivo.daemonService.unifiedconfig.update_finish_broadcast_VTSG");
        this.mVivoIntentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mVivoIntentFilter.addAction("com.vivo.battlemode.touchscreen");
        this.mVivoIntentFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        this.mVivoIntentFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        this.mVivoIntentFilter.addAction(ACTION_OPEN_CAMERA);
        this.mVivoIntentFilter.addAction(ACTION_CLOSE_CAMERA);
        this.mVivoIntentFilter.addAction(ACTION_WHITELIST_CLOSE_CAMERA);
        this.mVivoIntentFilter.addAction(ACTION_VIRTUAL_KEY_STATUS);
        this.mContext.registerReceiver(this.mVivoBroadcastReceiver, this.mVivoIntentFilter);
        this.mContext.registerReceiver(this.mCamereaReceiver, this.mVivoIntentFilter);
        for (int i = 0; i < SystemGesturesSettings.length; i++) {
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SystemGesturesSettings[i]), false, this.mSmartWakeObserver, -1);
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("is_game_mode"), false, this.mGameModeObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("is_game_space"), false, this.mGameModeObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(VIRTUAL_KEY_GESTURE), false, this.mVirtualKeyObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SCREEN_CLOCK_SWITCH), false, this.mScreenClockSupportObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SCREEN_CLOCK_REPORT_ABS_SWITCH), false, this.mScreenClockReportObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(VIRTUAL_GAMEKEY), false, this.mVirtualGameKeyObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(BBK_SCREEN_DISABLE_CARD_SLIDE_SETTING), false, this.mCardSlideObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(INPUT_METHOD_STATE), false, this.mInputMethodObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(EASY_SHARE_FORCE_BRIGHTNESS_OFF), false, this.mEasyShareForceBrightnessOffStateObserver, -1);
    }

    private void readXml() {
        new Thread() { // from class: com.vivo.services.touchscreen.TouchScreenService.15
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                try {
                    TouchScreenService.this.getConfig("content://com.vivo.daemonservice.unifiedconfigprovider/configs", "VTSG", "1", "v1.0", "vivotsgwzryswitch");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getConfig(String uri, String moduleName, String type, String version, String identifier) {
        ContentResolver resolver = this.mContext.getContentResolver();
        String[] selectionArgs = {moduleName, type, version, identifier};
        Cursor cursor = null;
        try {
            try {
                cursor = resolver.query(Uri.parse(uri), null, null, selectionArgs, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    if (cursor.getCount() > 0) {
                        while (!cursor.isAfterLast()) {
                            cursor.getString(cursor.getColumnIndex("id"));
                            cursor.getString(cursor.getColumnIndex("identifier"));
                            cursor.getString(cursor.getColumnIndex("fileversion"));
                            byte[] filecontent = cursor.getBlob(cursor.getColumnIndex("filecontent"));
                            String applists = new String(filecontent, "UTF-8");
                            if (DBG) {
                                VSlog.d(TAG, "getConfig VivoFakeWifiState.xml:\n  " + applists);
                            }
                            StringReader reader = new StringReader(applists);
                            updateWzrySwitch(reader);
                            cursor.moveToNext();
                        }
                    } else if (DBG) {
                        VSlog.d(TAG, "getConfig nodata");
                    }
                }
                if (cursor == null) {
                    return;
                }
            } catch (Exception e) {
                VSlog.d(TAG, "getConfig error:" + e);
                if (0 == 0) {
                    return;
                }
            }
            cursor.close();
        } catch (Throwable th) {
            if (0 != 0) {
                cursor.close();
            }
            throw th;
        }
    }

    private void updateWzrySwitch(StringReader reader) {
        if (DBG) {
            VSlog.d(TAG, "updateWzrySwitch start");
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(reader);
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                if (eventType == 0) {
                    this.wzrySwitch = false;
                } else if (eventType == 2) {
                    String name = parser.getName();
                    if (name.equalsIgnoreCase("item")) {
                        String wzrySetting = parser.nextText();
                        if (wzrySetting.contains("1")) {
                            this.wzrySwitch = true;
                            SetAppName("twzrySwitch".getBytes());
                            if (DBG) {
                                VSlog.d(TAG, "wzrySwitch is seteing to true");
                            }
                        } else {
                            this.wzrySwitch = false;
                            SetAppName("fwzrySwitch".getBytes());
                            if (DBG) {
                                VSlog.d(TAG, "wzrySwitch is seteing to false");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (DBG) {
            VSlog.d(TAG, "updateWzrySwitch end");
        }
    }

    private void registerProcessObserver() {
        try {
            if (mIActivityManager != null) {
                mIActivityManager.registerProcessObserver(this.mProcessObserver);
            }
        } catch (RemoteException e) {
            VSlog.d(TAG, "registerProcessObserver failed.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getAppNameFromUid(int uid) {
        if (DBG) {
            VSlog.d(TAG, "pakage name is " + this.mContext.getPackageManager().getNameForUid(uid) + "with :" + uid);
        }
        return this.mContext.getPackageManager().getNameForUid(uid);
    }

    public int SetAppName(final byte[] appName) {
        if (DBG) {
            VSlog.d(TAG, "called and appName is " + appName);
        }
        Thread thread = new Thread(new Runnable() { // from class: com.vivo.services.touchscreen.TouchScreenService.17
            @Override // java.lang.Runnable
            public void run() {
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "app name Thread run");
                }
                TouchScreenService.nativeSetAppName(appName);
            }
        });
        thread.start();
        return 0;
    }

    private boolean supportPCshareTouch() {
        if (BBKProductName.equals("null")) {
            return false;
        }
        String productStr = new String(BBKProductName);
        String[] strings = productStr.split("_");
        VSlog.d(TAG, "strings length =" + strings.length);
        if (strings.length > 0) {
            String str = strings[0];
            Pattern p = Pattern.compile("[^0-9]");
            Matcher m = p.matcher(str);
            String str2 = m.replaceAll(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).trim();
            if (str2.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                VSlog.d(TAG, "product str is empty!");
                return false;
            }
            int productNum = Integer.parseInt(str2);
            VSlog.d(TAG, "productnumber =" + productNum);
            return productNum < 2100;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class MultiTouchController {
        public static final int TYPE_BAND_STATE = 16;
        public static final int TYPE_CALLING = 9;
        public static final int TYPE_CHARGING = 4;
        public static final int TYPE_COUNT = 27;
        public static final int TYPE_FACE_VISABLE = 6;
        public static final int TYPE_FACTORY_SWITCH = 18;
        public static final int TYPE_FINGER_MODE = 24;
        public static final int TYPE_FINGER_MODE_SUPPORT = 26;
        public static final int TYPE_FINGER_SWITCH = 11;
        public static final int TYPE_FINGER_VISABLE = 5;
        public static final int TYPE_FORCE_DISABLE = 22;
        public static final int TYPE_FORCE_DOUBLE = 10;
        public static final int TYPE_FORCE_NORMAL = 8;
        public static final int TYPE_GAME_MODE = 13;
        public static final int TYPE_GESTURE = 2;
        public static final int TYPE_INPUT_METHOD = 25;
        public static final int TYPE_IN_CALL = 17;
        public static final int TYPE_NOTICEUP_VISABLE = 7;
        public static final int TYPE_PROXIMITY = 1;
        public static final int TYPE_ROTATION = 3;
        public static final int TYPE_SAVE_POWER = 27;
        public static final int TYPE_SCREEN = 0;
        public static final int TYPE_SCREENSHOT = 12;
        public static final int TYPE_SCREEN_CLOCK_SUPPORT = 19;
        public static final int TYPE_SCREEN_REPORT_ABS = 20;
        public static final int TYPE_VIRTUAL_GAMEKEY = 23;
        public static final int TYPE_VIRTUAL_KEY = 14;
        public static final int TYPE_VK_ACTIVE_MODE = 21;
        public static final int TYPE_VK_LONGPRESS = 15;
        private static final String mSeparator = ":";
        private Handler mTouchCntlHandler;

        public MultiTouchController() {
            HandlerThread ht = new HandlerThread("TouchCntlThread");
            ht.start();
            this.mTouchCntlHandler = new Handler(ht.getLooper()) { // from class: com.vivo.services.touchscreen.TouchScreenService.MultiTouchController.1
                @Override // android.os.Handler
                public void handleMessage(Message msg) {
                    int type = msg.what;
                    int displayId = msg.arg1;
                    int state = msg.arg2;
                    StringBuffer sb = new StringBuffer();
                    sb.append(MultiTouchController.this.getCmd(type));
                    sb.append(MultiTouchController.mSeparator);
                    sb.append(MultiTouchController.this.getTouchName(displayId));
                    sb.append(MultiTouchController.mSeparator);
                    sb.append(state);
                    TouchScreenService.nativeSetAppName(sb.toString().getBytes());
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String getCmd(int type) {
            switch (type) {
                case 0:
                    return "vts_lcd";
                case 1:
                    return "vts_proximity";
                case 2:
                    return "vts_gs";
                case 3:
                    return "vts_rotation";
                case 4:
                    return "vts_charge";
                case 5:
                    return "vts_finger_highlight";
                case 6:
                    return "vts_face_highlight";
                case 7:
                    return "vts_notice_up";
                case 8:
                    return "vts_force_normal";
                case 9:
                    return "vts_calling";
                case 10:
                    return "vts_force_double";
                case 11:
                    return "vts_finger_unlock";
                case 12:
                    return "vts_screenshot";
                case 13:
                    return "vts_game_mode";
                case 14:
                    return "vts_virtual_key";
                case 15:
                    return "vts_vk_longpress";
                case 16:
                    return "vts_band_state";
                case 17:
                    return "vts_in_call";
                case 18:
                    return "vts_factory_switch";
                case 19:
                    return "vts_screen_clock";
                case 20:
                    return "vts_clock_report_abs";
                case 21:
                    return "vts_vk_activemode";
                case 22:
                    return "vts_force_disable";
                case 23:
                    return "vts_virtual_gamekey";
                case 24:
                    return "vts_finger_mode";
                case 25:
                    return "vts_input_method";
                case 26:
                    return "vts_finger_mode_support";
                case 27:
                    return "vts_save_power";
                default:
                    return null;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String getTouchName(int displayId) {
            if (displayId != 0) {
                if (displayId != 1) {
                    if (displayId == 2) {
                        return SystemAutoRecoverService.WindowItem.ALL_WINDOW_TAG;
                    }
                    return null;
                }
                return "vivo_ts_second";
            }
            return "vivo_ts";
        }

        public void setStateForAll(int type, int state) {
            setState(2, type, state);
        }

        public void setState(int displayId, int type, int state) {
            if (TouchScreenService.DBG) {
                VSlog.d(TouchScreenService.TAG, "displayId = " + displayId + " type = " + type + "state = " + state);
            }
            if (displayId == 0) {
                displayId = 0;
            }
            if (displayId == 4096) {
                displayId = 1;
            }
            if (getTouchName(displayId) == null) {
                VSlog.d(TouchScreenService.TAG, "invalid display id, display id = " + displayId);
            } else if (getCmd(type) == null) {
                VSlog.e(TouchScreenService.TAG, "invalid type : " + type);
            } else {
                this.mTouchCntlHandler.obtainMessage(type, displayId, state).sendToTarget();
            }
        }
    }

    /* loaded from: classes.dex */
    private final class RadioBandSwitch {
        private static final String ACTION_FCN_CHANGED = "vivo.intent.action.FCN_CHANGE";
        private Handler mTimerHandler;
        private boolean setBandTimerEnable = true;
        private boolean needToSwitch = false;
        private int fcn = 0;
        private final BroadcastReceiver mFcnChangeReceiver = new BroadcastReceiver() { // from class: com.vivo.services.touchscreen.TouchScreenService.RadioBandSwitch.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "Touch mFcnChangeReceiver action:" + action);
                }
                if (action.equals(RadioBandSwitch.ACTION_FCN_CHANGED)) {
                    RadioBandSwitch.this.fcn = intent.getIntExtra("isTpBand", 0);
                    if (TouchScreenService.DBG) {
                        VSlog.d(TouchScreenService.TAG, "get fcn is:" + RadioBandSwitch.this.fcn + ", needToSwitch:" + RadioBandSwitch.this.needToSwitch + ", setBandTimerEnable:" + RadioBandSwitch.this.setBandTimerEnable);
                    }
                    if (RadioBandSwitch.this.setBandTimerEnable) {
                        RadioBandSwitch.this.setBandTimerEnable = false;
                        RadioBandSwitch.this.mTimerHandler.postDelayed(RadioBandSwitch.this.mTimerRunnable, 60000L);
                        RadioBandSwitch.this.SetRadioBand();
                        RadioBandSwitch.this.needToSwitch = false;
                        return;
                    }
                    RadioBandSwitch.this.needToSwitch = true;
                }
            }
        };
        Runnable mTimerRunnable = new Runnable() { // from class: com.vivo.services.touchscreen.TouchScreenService.RadioBandSwitch.2
            @Override // java.lang.Runnable
            public void run() {
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "RadioTimerReach, needToSwitch:" + RadioBandSwitch.this.needToSwitch + ", setBandTimerEnable:" + RadioBandSwitch.this.setBandTimerEnable);
                }
                RadioBandSwitch.this.setBandTimerEnable = true;
                if (RadioBandSwitch.this.needToSwitch) {
                    RadioBandSwitch.this.setBandTimerEnable = false;
                    RadioBandSwitch.this.needToSwitch = false;
                    RadioBandSwitch.this.mTimerHandler.postDelayed(RadioBandSwitch.this.mTimerRunnable, 60000L);
                    RadioBandSwitch.this.SetRadioBand();
                }
            }
        };

        public RadioBandSwitch() {
            this.mTimerHandler = null;
            if (this.mTimerHandler == null) {
                this.mTimerHandler = new Handler();
            }
            IntentFilter mFcnChangeFilter = new IntentFilter();
            mFcnChangeFilter.addAction(ACTION_FCN_CHANGED);
            TouchScreenService.this.mContext.registerReceiver(this.mFcnChangeReceiver, mFcnChangeFilter);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public final void SetRadioBand() {
            if (TouchScreenService.DBG) {
                VSlog.d(TouchScreenService.TAG, "Touch fcn:" + this.fcn);
            }
            if (this.fcn == 0) {
                TouchScreenService.this.mMultiTouchController.setState(0, 16, 0);
            } else {
                TouchScreenService.this.mMultiTouchController.setState(0, 16, 1);
            }
        }
    }

    public int touchScreenEdgeRejectionSceneGet() {
        return this.mEdgeRejectionConfigure.current_scene;
    }

    public int touchScreenEdgeRejectionLyingGet() {
        return this.mEdgeRejectionConfigure.current_lying;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class EdgeRejectionConfigure {
        public static final int DEADZONE_LANDSCAPE = 18;
        public static final int DEADZONE_PORTRAIT = 16;
        public static final int EDGE_REJECTION_APP = 101;
        public static final int GAME_LANDSCAPE = 81;
        public static final int GAME_MODE = 2;
        public static final int GAME_PORTRAIT = 2;
        public static final int INPUT_METHOD_MODE = 4;
        public static final int INPUT_METHOD_PORTRAIT = 1;
        public static final int LANDSCAPE_STATE = 2;
        public static final int LONGPRESS_LANDSCAPE = 19;
        public static final int LONGPRESS_PORTRAIT = 17;
        public static final int NORMAL_LANDSCAPE = 80;
        public static final int NORMAL_MODE = 1;
        public static final int NORMAL_PORTRAIT = 0;
        public static final int NULL_MODE = 0;
        public static final int NULL_STATE = 0;
        public static final int PORTRAIT_STATE = 1;
        public static final int SPECIAL_FLAT_MODE = 2;
        public static final int SPECIAL_LEFT_MODE = 3;
        public static final int SPECIAL_LYING = 1;
        public static final int SPECIAL_NORMAL = 0;
        public static final int SPECIAL_NORMAL_MODE = 1;
        public static final int SPECIAL_RIGHT_MODE = 4;
        public static final int VIDEO_LANDSCAPE = 82;
        public static final int VIDEO_MODE = 3;
        private String EdgeRejectionConfigured;
        private Handler mExceptionHandler;
        public Handler mTimerHandler;
        private int screen_state = 1;
        private int game_mode = 0;
        private int video_mode = 0;
        private int input_method_mode = 0;
        private int last_special_mode = 1;
        private int special_screen_state = 1;
        private int current_scene = 0;
        private int current_lying = 0;
        private boolean configValue = false;
        public boolean flat_state = false;
        public int lastState = 0;
        private final String[] EdgeRejectionPortraitPara = {"0,0,0,10,0,10,0", "0,0,1,30,200,100,30", "1,0,0,45,0,45,0", "1,0,1,65,300,65,300", "256,0,0,80,0,0,0", "0,0,0,10,0,10,0", "0,0,1,45,200,200,45", "1,0,0,45,0,45,0", "1,0,1,65,300,65,300", "256,0,0,80,0,0,0", "0,0,0,10,0,10,0", "0,0,1,30,200,100,30", "1,0,0,45,0,45,0", "1,0,1,65,300,65,300", "256,0,0,50,0,0,0"};
        private final String[] EdgeRejectionLandscapePara = {"0,1,0,10,0,10,0", "0,1,2,30,200,100,30", "0,1,3,30,200,100,30", "1,1,0,30,50,30,50", "1,1,2,80,80,80,80", "1,1,3,100,100,100,100", "256,0,0,70,0,0,0", "0,1,0,10,0,10,0", "0,1,2,0,0,0,0", "0,1,3,30,200,100,30", "1,1,0,30,50,30,50", "1,1,2,0,0,0,0", "1,1,3,50,50,50,50", "256,0,0,50,0,0,0", "0,1,0,10,10,10,10", "0,1,2,50,200,200,50", "0,1,3,50,200,200,50", "1,1,0,30,70,30,50", "1,1,2,100,200,100,200", "1,1,3,100,200,100,200", "256,0,0,70,0,0,0"};
        Runnable mExceptionTimerRunnable = new Runnable() { // from class: com.vivo.services.touchscreen.TouchScreenService.EdgeRejectionConfigure.1
            @Override // java.lang.Runnable
            public void run() {
                VSlog.d(TouchScreenService.TAG, "lying for 8 hours and auto set normal mode");
                EdgeRejectionConfigure.this.setEdgeSpecialPara(0, 1);
            }
        };
        Runnable mTimerRunnable = new Runnable() { // from class: com.vivo.services.touchscreen.TouchScreenService.EdgeRejectionConfigure.2
            @Override // java.lang.Runnable
            public void run() {
                int result;
                if (EdgeRejectionConfigure.this.lastState != 0) {
                    if (TouchScreenService.DBG) {
                        VSlog.d(TouchScreenService.TAG, "mACCListener updatePhoneState : 1");
                    }
                    if (EdgeRejectionConfigure.this.lastState == 2) {
                        result = 3;
                    } else if (EdgeRejectionConfigure.this.lastState == 3) {
                        result = 4;
                    } else {
                        result = 2;
                    }
                    EdgeRejectionConfigure.this.setEdgeSpecialPara(0, result);
                    EdgeRejectionConfigure.this.mExceptionHandler.postDelayed(EdgeRejectionConfigure.this.mExceptionTimerRunnable, 28800000L);
                    EdgeRejectionConfigure.this.flat_state = true;
                    return;
                }
                EdgeRejectionConfigure.this.setEdgeSpecialPara(0, 1);
                EdgeRejectionConfigure.this.mExceptionHandler.removeCallbacks(EdgeRejectionConfigure.this.mExceptionTimerRunnable);
            }
        };

        public void EdgeRjectionParaChanged(int flag, int[] coordinate, boolean enable) {
            StringBuffer EdgeRejectionBuffer = new StringBuffer();
            if (flag == 17) {
                if (enable) {
                    this.EdgeRejectionConfigured = "1,0,0," + coordinate[2] + ",0," + coordinate[2] + ",0";
                } else {
                    this.EdgeRejectionConfigured = "1,0,0,45,0,45,0";
                }
                EdgeRejectionBuffer.append("vts_rejection_zone:");
                EdgeRejectionBuffer.append(this.EdgeRejectionConfigured);
                TouchScreenService.this.SetAppName(EdgeRejectionBuffer.toString().getBytes());
                this.configValue = enable;
            }
        }

        public EdgeRejectionConfigure() {
            this.mTimerHandler = null;
            this.mExceptionHandler = null;
            if (!TouchScreenService.EdgeRejectionConfigureSupport) {
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "edge rejection configure not support");
                    return;
                }
                return;
            }
            if (this.mTimerHandler == null) {
                this.mTimerHandler = new Handler();
            }
            if (this.mExceptionHandler == null) {
                this.mExceptionHandler = new Handler();
            }
            TouchScreenService.this.mEdgeRejectionSensor = TouchScreenService.this.mSensorManager.getDefaultSensor(TouchScreenService.SENSOR_TYPE_EDGEREJECTION_DETECT);
            VSlog.d(TouchScreenService.TAG, "mEdgeRejectionSensor : " + TouchScreenService.this.mEdgeRejectionSensor);
            if (TouchScreenService.this.mEdgeRejectionSensor != null) {
                TouchScreenService.this.mSensorManager.registerListener(TouchScreenService.this.mEdgeRejectionSensorListner, TouchScreenService.this.mEdgeRejectionSensor, 3, TouchScreenService.this.mHandler);
            }
        }

        public void closeLcdHandler() {
            this.mExceptionHandler.removeCallbacks(this.mExceptionTimerRunnable);
            this.mTimerHandler.removeCallbacks(this.mTimerRunnable);
            setEdgeSpecialPara(0, 1);
            this.flat_state = false;
            this.lastState = 0;
        }

        private boolean set_edge_cmd(int status) {
            StringBuffer DeadZoneSide = new StringBuffer();
            StringBuffer DeadZoneCorner = new StringBuffer();
            StringBuffer DeadZoneBottomCorner = new StringBuffer();
            StringBuffer LongPressZoneSide = new StringBuffer();
            StringBuffer LongPressZoneCorner = new StringBuffer();
            StringBuffer LongPressZoneBottomCorner = new StringBuffer();
            StringBuffer SpecialZone = new StringBuffer();
            if (this.current_scene == status) {
                VSlog.d(TouchScreenService.TAG, "do not set_edge_cmd, status: " + status);
                return false;
            }
            this.current_scene = status;
            if (status < 80) {
                DeadZoneSide.append("vts_rejection_zone:");
                DeadZoneSide.append(this.EdgeRejectionPortraitPara[(status * 5) + 0]);
                TouchScreenService.this.SetAppName(DeadZoneSide.toString().getBytes());
                DeadZoneCorner.append("vts_rejection_zone:");
                DeadZoneCorner.append(this.EdgeRejectionPortraitPara[(status * 5) + 1]);
                TouchScreenService.this.SetAppName(DeadZoneCorner.toString().getBytes());
                if (this.configValue) {
                    LongPressZoneSide.append("vts_rejection_zone:");
                    LongPressZoneSide.append(this.EdgeRejectionConfigured);
                } else {
                    LongPressZoneSide.append("vts_rejection_zone:");
                    LongPressZoneSide.append(this.EdgeRejectionPortraitPara[(status * 5) + 2]);
                }
                TouchScreenService.this.SetAppName(LongPressZoneSide.toString().getBytes());
                LongPressZoneCorner.append("vts_rejection_zone:");
                LongPressZoneCorner.append(this.EdgeRejectionPortraitPara[(status * 5) + 3]);
                TouchScreenService.this.SetAppName(LongPressZoneCorner.toString().getBytes());
                if (this.current_lying == 1) {
                    SpecialZone.append("vts_rejection_zone:");
                    SpecialZone.append("256,0,0,115,0,0,0");
                } else {
                    SpecialZone.append("vts_rejection_zone:");
                    SpecialZone.append(this.EdgeRejectionPortraitPara[(status * 5) + 4]);
                }
                TouchScreenService.this.SetAppName(SpecialZone.toString().getBytes());
            } else {
                DeadZoneSide.append("vts_rejection_zone:");
                DeadZoneSide.append(this.EdgeRejectionLandscapePara[((status - 80) * 7) + 0]);
                TouchScreenService.this.SetAppName(DeadZoneSide.toString().getBytes());
                DeadZoneCorner.append("vts_rejection_zone:");
                DeadZoneCorner.append(this.EdgeRejectionLandscapePara[((status - 80) * 7) + 1]);
                TouchScreenService.this.SetAppName(DeadZoneCorner.toString().getBytes());
                DeadZoneBottomCorner.append("vts_rejection_zone:");
                DeadZoneBottomCorner.append(this.EdgeRejectionLandscapePara[((status - 80) * 7) + 2]);
                TouchScreenService.this.SetAppName(DeadZoneBottomCorner.toString().getBytes());
                LongPressZoneSide.append("vts_rejection_zone:");
                LongPressZoneSide.append(this.EdgeRejectionLandscapePara[((status - 80) * 7) + 3]);
                TouchScreenService.this.SetAppName(LongPressZoneSide.toString().getBytes());
                LongPressZoneCorner.append("vts_rejection_zone:");
                LongPressZoneCorner.append(this.EdgeRejectionLandscapePara[((status - 80) * 7) + 4]);
                TouchScreenService.this.SetAppName(LongPressZoneCorner.toString().getBytes());
                LongPressZoneBottomCorner.append("vts_rejection_zone:");
                LongPressZoneBottomCorner.append(this.EdgeRejectionLandscapePara[((status - 80) * 7) + 5]);
                TouchScreenService.this.SetAppName(LongPressZoneBottomCorner.toString().getBytes());
                SpecialZone.append("vts_rejection_zone:");
                SpecialZone.append(this.EdgeRejectionLandscapePara[((status - 80) * 7) + 6]);
                TouchScreenService.this.SetAppName(SpecialZone.toString().getBytes());
            }
            VSlog.d(TouchScreenService.TAG, "set_edge_cmd, status: " + status);
            return true;
        }

        private boolean set_edge_special_cmd(int status) {
            StringBuffer NormalSet = new StringBuffer();
            StringBuffer SpecialSet = new StringBuffer();
            if (this.current_lying == status) {
                VSlog.d(TouchScreenService.TAG, "do not set_edge_special_cmd, status: " + status);
                return false;
            }
            this.current_lying = status;
            if (status == 1) {
                NormalSet.append("vts_rejection_zone:");
                NormalSet.append("256,0,0,115,0,0,0");
                TouchScreenService.this.SetAppName(NormalSet.toString().getBytes());
            } else {
                SpecialSet.append("vts_rejection_zone:");
                SpecialSet.append("256,0,0,80,0,0,0");
                TouchScreenService.this.SetAppName(SpecialSet.toString().getBytes());
            }
            VSlog.d(TouchScreenService.TAG, "set_edge_special_cmd, status: " + status);
            return true;
        }

        private int setEdgeRejectionRotation(int rotation) {
            if (rotation != 0) {
                this.screen_state = rotation;
                return 0;
            }
            return 0;
        }

        private int setEdgeRejectionMode(int mode, int state) {
            if (mode == 2) {
                this.game_mode = state;
                return 0;
            } else if (mode == 3) {
                this.video_mode = state;
                return 0;
            } else if (mode == 4) {
                this.input_method_mode = state;
                return 0;
            } else {
                return 0;
            }
        }

        public boolean setEdgeRejectionPara(int rotation, int mode, int state) {
            if (!TouchScreenService.EdgeRejectionConfigureSupport) {
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "edge rejection configure not support");
                }
                return false;
            }
            VSlog.d(TouchScreenService.TAG, "rotation: " + rotation + " mode: " + mode + " state: " + state);
            if ((rotation == 0 && mode == 0) || rotation == this.screen_state) {
                return false;
            }
            setEdgeRejectionRotation(rotation);
            setEdgeRejectionMode(mode, state);
            if (this.screen_state == 1 && this.input_method_mode == 1) {
                return set_edge_cmd(1);
            }
            if (this.screen_state == 1 && this.game_mode == 1) {
                return set_edge_cmd(2);
            }
            int i = this.screen_state;
            if (i == 1) {
                return set_edge_cmd(0);
            }
            if (i == 2 && this.game_mode == 1) {
                return set_edge_cmd(81);
            }
            if (this.screen_state == 2 && this.video_mode == 1) {
                return set_edge_cmd(82);
            }
            if (this.screen_state == 2) {
                return set_edge_cmd(80);
            }
            VSlog.d(TouchScreenService.TAG, "no need to set state or mode");
            return false;
        }

        public boolean setEdgeSpecialPara(int rotation, int mode) {
            if (!TouchScreenService.EdgeRejectionConfigureSupport) {
                if (TouchScreenService.DBG) {
                    VSlog.d(TouchScreenService.TAG, "edge special configure not support");
                }
                return false;
            } else if ((rotation == 0 && mode == 0) || rotation == this.special_screen_state || mode == this.last_special_mode) {
                return false;
            } else {
                if (rotation != 0) {
                    this.special_screen_state = rotation;
                }
                if (mode != 0) {
                    this.last_special_mode = mode;
                }
                if (this.special_screen_state == 1 && (mode == 2 || mode == 3 || mode == 4)) {
                    return set_edge_special_cmd(1);
                }
                return set_edge_special_cmd(0);
            }
        }
    }
}