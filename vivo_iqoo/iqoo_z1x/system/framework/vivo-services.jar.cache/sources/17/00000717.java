package com.vivo.services.rms.display;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.DisplayEventReceiver;
import android.view.IWindowManager;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import java.util.Iterator;

/* loaded from: classes.dex */
public class GlobalConfigs {
    private static final int BRIGHTNESS_MODE_ALWAYS = 3;
    private static final int BRIGHTNESS_MODE_OLED = 1;
    private static final int BRIGHTNESS_MODE_OLED_AND_HIGH_RATE = 2;
    private static final int DEFAULT_LOW_BRINGHTNESS_VALUE = 44;
    private static final int DEFAULT_MIN_DFPS = 10;
    private static final int DEFAULT_TOUCH_BOOST_DURATION = 3000;
    private static final int DEFAULT_TOUCH_MIN_FPS = 60;
    private static final int FPS_COM_EPSILON = 4;
    private static final float FPS_EPSILON = 0.2f;
    private static final boolean HAS_PRIVATE_FPS;
    public static final boolean IS_OLED_PANEL = "amoled".equals(SystemProperties.get("persist.vivo.phone.panel_type", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).toLowerCase());
    public static final String KEY_ALWAYS_AUTO_TOUCH = "key_always_auto_touch";
    public static final String KEY_ANIMATION_ALWAYS_MAX_REFRESH_RATE = "key_animation_always_max_refresh_rate";
    public static final String KEY_ANIMATION_RELEASE_DELAYED_TIME = "key_animation_release_delayed_time";
    public static final String KEY_APP_REQUEST_REFRESH_RATE_LIMITED = "key_app_request_refresh_rate_limited";
    public static final String KEY_AUTO_TOUCH_IN_LOW_POWER_MODE = "key_auto_touch_in_low_power_mode";
    public static final String KEY_CONFIG_BITS_ALLOWED_BRIGHTNESS = "key_config_bits_allowed_brightness";
    public static final String KEY_DFPS_ENABLE = "key_dfps_enable";
    public static final String KEY_FEATURE_ENABLED = "key_feature_enabled";
    public static final String KEY_LOW_BRIGHTNESS_VALUE = "key_oled_low_brightness_value";
    public static final String KEY_MIN_DFPS = "key_min_dfps";
    public static final String KEY_NIGHT_MODE_VALUE = "key_night_mode_value";
    public static final String KEY_SCENE_CONFIGS = "key_scene_configs";
    public static final String KEY_SCENE_DISABLE_REASONS = "key_scene_disable_reasons";
    public static final String KEY_SCENE_ENABLE = "key_scene_enable";
    public static final String KEY_TOUCH_BOOST_DURATION = "key_touch_boost_duration";
    public static final String KEY_TOUCH_MIN_FPS = "key_touch_min_fps";
    public static final String KEY_USE_BRIGHTNESS_MODE = "key_use_brightness_mode";
    public static final String KEY_USE_MAX_FPS_WHEN_IGNORE_INPUT = "key_use_max_fps_when_ignore_input";
    public static final String KEY_VERSION = "key_version";
    public static final String KEY_WINDOW_FLAGS = "key_window_flags";
    private static final int MSG_UPDATE_BRINGHTNESS = 1;
    private static final int NIGHT_MODE_VALUE = 1;
    private static final String TAG = "RefreshRateAdjuster";
    private static final int USER_AUTO_REFRESH_RATE_MODE = 1;
    private static DisplayObserver sDisplayObserver;
    private static MainHandler sHandler;
    private static MyInputEventReceiver sInputEventReceiver;
    private static UsetSettingObserver sUserSettingObserver;
    private static final String PRIVATE_FPS_LIST = SystemProperties.get("persist.sys.private_fps", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
    private static final DisplayConfigsManager DISPLAY_CONFIGS = DisplayConfigsManager.getInstance();
    private static int sUseBrightnessMode = SystemProperties.getInt("persist.sys.use_brightness_mode", 2);
    private static int sLowBrightnessValue = 44;
    private static boolean sUseMaxFpsWhenIgnoreInput = true;
    private static int sNightModeValue = 1;
    private static int sTouchBoostDuration = 3000;
    private static boolean sAnimationAlwaysMaxRefreshRate = false;
    private static boolean sAppRequestRefreshRateLimited = true;
    private static boolean sAlwaysAutoTouch = false;
    private static boolean sAutoTouchInLowPowerMode = true;
    private static boolean sDfpsEnable = true;
    private static int sMinDFps = 10;
    private static int sTouchMinFps = 60;
    private static int sWindowInteractionMinFps = -1;
    private static int sWindowInteractionMaxFps = -1;
    private static int sWindowBrightnessValue = 0;
    private static int sWindowConfigBits = 0;
    private static int sScreenBrightness = 100;
    private static int sScreenState = 0;
    private static int sAnimationReleaseDelayedTime = 50;
    public static boolean sFeatureSupported = false;
    private static boolean sFeatureEnabled = false;
    private static boolean sBbkLogOn = false;
    private static boolean sDebug = false;
    private static boolean sTmpUpdateRefreshRateModeRequest = false;

    static {
        String str;
        HAS_PRIVATE_FPS = !TextUtils.isEmpty(str);
    }

    public static void initialize(Context context, Looper looper) {
        boolean z = SystemProperties.getBoolean("persist.sys.vivo.refresh_rate_adjuster", true);
        sFeatureSupported = z;
        sFeatureEnabled = z;
        if (isFeatureSupported()) {
            sHandler = new MainHandler(looper);
            UsetSettingObserver usetSettingObserver = new UsetSettingObserver(sHandler);
            sUserSettingObserver = usetSettingObserver;
            usetSettingObserver.observe(context);
            sDisplayObserver = new DisplayObserver(sHandler);
            try {
                IWindowManager wms = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
                InputChannel inputChannel = wms.monitorInput("input_hook");
                sInputEventReceiver = new MyInputEventReceiver(inputChannel, looper);
            } catch (RemoteException e) {
            }
            PowerManager pm = (PowerManager) context.getSystemService("power");
            if (sScreenState == 0) {
                sScreenState = pm.isInteractive() ? 2 : 1;
            }
        }
    }

    public static void setConfigs(Bundle bundle) {
        synchronized (GlobalConfigs.class) {
            sTmpUpdateRefreshRateModeRequest = false;
            try {
                String value = bundle.getString(KEY_LOW_BRIGHTNESS_VALUE, null);
                if (value != null) {
                    setLowBrightnessValue(Integer.parseInt(value));
                }
            } catch (NumberFormatException e) {
            }
            try {
                String value2 = bundle.getString(KEY_NIGHT_MODE_VALUE, null);
                if (value2 != null) {
                    setNightModeValue(Integer.parseInt(value2));
                }
            } catch (NumberFormatException e2) {
            }
            try {
                String value3 = bundle.getString(KEY_TOUCH_BOOST_DURATION, null);
                if (value3 != null) {
                    setTouchBoostDuration(Integer.parseInt(value3));
                }
            } catch (NumberFormatException e3) {
            }
            try {
                String value4 = bundle.getString(KEY_ANIMATION_RELEASE_DELAYED_TIME, null);
                if (value4 != null) {
                    setAnimationReleaseDelayedTime(Integer.parseInt(value4));
                }
            } catch (NumberFormatException e4) {
            }
            try {
                String value5 = bundle.getString(KEY_MIN_DFPS, null);
                if (value5 != null) {
                    setMinDFps(Integer.parseInt(value5));
                }
            } catch (NumberFormatException e5) {
            }
            try {
                String value6 = bundle.getString(KEY_TOUCH_MIN_FPS, null);
                if (value6 != null) {
                    setTouchMinFps(Integer.parseInt(value6));
                }
            } catch (NumberFormatException e6) {
            }
            String value7 = bundle.getString(KEY_USE_BRIGHTNESS_MODE, null);
            if (value7 != null) {
                setUseBrightnessMode(Integer.parseInt(value7));
            }
            String value8 = bundle.getString(KEY_CONFIG_BITS_ALLOWED_BRIGHTNESS, null);
            if (value8 != null) {
                setConfigBitsAllowedBrightness(value8);
            }
            String value9 = bundle.getString(KEY_USE_MAX_FPS_WHEN_IGNORE_INPUT, null);
            if (value9 != null) {
                setUseMaxFpsWhenIgnoreInput(Boolean.parseBoolean(value9));
            }
            String value10 = bundle.getString(KEY_DFPS_ENABLE, null);
            if (value10 != null) {
                setDfpsEnable(Boolean.parseBoolean(value10));
            }
            String value11 = bundle.getString(KEY_ANIMATION_ALWAYS_MAX_REFRESH_RATE, null);
            if (value11 != null) {
                setAnimationAlwaysMaxRefreshRate(Boolean.parseBoolean(value11));
            }
            String value12 = bundle.getString(KEY_APP_REQUEST_REFRESH_RATE_LIMITED, null);
            if (value12 != null) {
                setAppRequestRefreshRateLimited(Boolean.parseBoolean(value12));
            }
            String value13 = bundle.getString(KEY_ALWAYS_AUTO_TOUCH, null);
            if (value13 != null) {
                setAlwaysAutoTouch(Boolean.parseBoolean(value13));
            }
            String value14 = bundle.getString(KEY_AUTO_TOUCH_IN_LOW_POWER_MODE, null);
            if (value14 != null) {
                setAutoTouchInLowPowerMode(Boolean.parseBoolean(value14));
            }
            try {
                String value15 = bundle.getString(KEY_WINDOW_FLAGS, null);
                if (value15 != null) {
                    WindowRequestManager.getInstance().setFlags(parseInteger(value15));
                }
            } catch (NumberFormatException e7) {
            }
            if (sTmpUpdateRefreshRateModeRequest && sUserSettingObserver != null) {
                sUserSettingObserver.updateRefreshRateModeRequest();
            }
            RefreshRateAdjuster.getInstance().requestSetActiveMode();
            sTmpUpdateRefreshRateModeRequest = false;
            String value16 = bundle.getString(KEY_FEATURE_ENABLED, null);
            if (value16 != null) {
                setFeatureEnabled(Boolean.parseBoolean(value16));
            }
        }
    }

    public static boolean isFeatureSupported() {
        return sFeatureSupported;
    }

    public static boolean isFeatureEnabled() {
        return sFeatureSupported && sFeatureEnabled;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void setFeatureEnabled(boolean enable) {
        if (isFeatureSupported() && enable != sFeatureEnabled) {
            boolean isEnableOld = isFeatureEnabled();
            sFeatureEnabled = enable;
            boolean isEnableNow = isFeatureEnabled();
            if (isEnableOld != isEnableNow) {
                RefreshRateAdjuster.getInstance().notifyFeatureSwitch(isEnableNow);
            }
        }
    }

    public static void setAnimationAlwaysMaxRefreshRate(boolean alwaysMaxRefreshRate) {
        sAnimationAlwaysMaxRefreshRate = alwaysMaxRefreshRate;
    }

    public static int getAnimationRefreshRate() {
        if (sAnimationAlwaysMaxRefreshRate) {
            return getUserSettingMaxRefreshRate();
        }
        return WindowRequestManager.getInstance().getAnimationRefreshRate();
    }

    public static boolean isAnimationHighRate() {
        return WindowRequestManager.getInstance().isAnimationHighRate();
    }

    private static void setAppRequestRefreshRateLimited(boolean limited) {
        if (sAppRequestRefreshRateLimited != limited) {
            sAppRequestRefreshRateLimited = limited;
            sTmpUpdateRefreshRateModeRequest = true;
        }
    }

    public static boolean isAppRequestRefreshRateLimited() {
        return sAppRequestRefreshRateLimited;
    }

    public static int getAppRequestRefreshRate(int reqFps) {
        if (sAppRequestRefreshRateLimited) {
            return Math.min(reqFps, getUserSettingMaxRefreshRate());
        }
        return reqFps;
    }

    public static boolean isUseBrightness() {
        int i = sUseBrightnessMode;
        if (i != 1) {
            return i != 2 ? i == 3 : IS_OLED_PANEL || getUserSettingRefreshRateMode() > 85;
        }
        return IS_OLED_PANEL;
    }

    private static void setUseBrightnessMode(int mode) {
        sUseBrightnessMode = mode;
    }

    public static void setNightModeValue(int value) {
        sNightModeValue = value;
    }

    public static void setLowBrightnessValue(int threshold) {
        sLowBrightnessValue = threshold;
    }

    public static int getLowBrightnessValue() {
        int i = sWindowBrightnessValue;
        return i > 0 ? i : sLowBrightnessValue;
    }

    public static void setDfpsEnable(boolean enable) {
        if (sDfpsEnable != enable) {
            sDfpsEnable = enable;
        }
    }

    public static boolean isConfigBitsAllowed(int configBits) {
        return DISPLAY_CONFIGS.isConfigBitsAllowed(configBits, sScreenBrightness);
    }

    private static void setConfigBitsAllowedBrightness(String brightnessStr) {
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter('|');
        splitter.setString(brightnessStr);
        try {
            Iterator<String> it = splitter.iterator();
            while (it.hasNext()) {
                String entry = it.next();
                int index = entry.indexOf("=");
                if (index != -1) {
                    DISPLAY_CONFIGS.setAllowedBrightness(parseInteger(entry.substring(0, index)), parseInteger(entry.substring(index + 1)));
                }
            }
        } catch (NumberFormatException e) {
            VLog.d("RefreshRateAdjuster", "setConfigBitsAllowed error:" + e.getMessage());
        }
    }

    public static boolean isDfpsEnable() {
        return sDfpsEnable;
    }

    public static boolean isLowBrightness() {
        return sScreenBrightness <= getLowBrightnessValue();
    }

    public static int getScreenBrightness() {
        return sScreenBrightness;
    }

    public static int getDisplayState() {
        return sScreenState;
    }

    public static boolean isScreenOn() {
        return sScreenState == 2;
    }

    public static void setAnimationReleaseDelayedTime(int time) {
        sAnimationReleaseDelayedTime = time;
    }

    public static int animationReleaseDelayedTime() {
        return sAnimationReleaseDelayedTime;
    }

    public static int getMinDFps() {
        return sMinDFps;
    }

    public static void setMinDFps(int fps) {
        sMinDFps = fps;
    }

    public static int getTouchMinDFps() {
        return sTouchMinFps;
    }

    public static void setTouchMinFps(int fps) {
        if (sTouchMinFps != fps) {
            sTmpUpdateRefreshRateModeRequest = true;
            sTouchMinFps = fps;
        }
    }

    public static int getVsyncRate() {
        int vsyncPeriod = SfUtils.getVsyncPeriod();
        if (vsyncPeriod > 0) {
            return 1000000000 / vsyncPeriod;
        }
        return 0;
    }

    private static boolean updateScreenBrightness(int screenBrightness) {
        if (isUseBrightness()) {
            int threshold = getLowBrightnessValue();
            return (sScreenBrightness < threshold) != (screenBrightness < threshold);
        }
        return false;
    }

    public static void onScreenBrightnessChanged(int screenBrightness, int screenState) {
        MainHandler mainHandler;
        int i = 0;
        int i2 = 0;
        if (sScreenBrightness != screenBrightness) {
            i = (updateScreenBrightness(screenBrightness) || DISPLAY_CONFIGS.updateAllowdBrightness(screenBrightness, sScreenBrightness)) ? 1 : 1;
            sScreenBrightness = screenBrightness;
        }
        if (sScreenState != screenState) {
            sScreenState = screenState;
            i2 = 1;
        }
        if ((i2 != 0 || i != 0) && isFeatureSupported() && (mainHandler = sHandler) != null) {
            mainHandler.obtainMessage(1, i, i2).sendToTarget();
        }
    }

    public static boolean isNightMode() {
        UsetSettingObserver usetSettingObserver = sUserSettingObserver;
        if (usetSettingObserver != null) {
            return usetSettingObserver.isNightMode();
        }
        return false;
    }

    public static boolean isLowPowerMode() {
        UsetSettingObserver usetSettingObserver = sUserSettingObserver;
        if (usetSettingObserver != null) {
            return usetSettingObserver.isLowPowerMode();
        }
        return false;
    }

    public static int clipFps(int reqFps) {
        int fps = Math.min(DISPLAY_CONFIGS.toRealFps(reqFps), DISPLAY_CONFIGS.getMaxRefreshRate());
        if (fps > 0) {
            return Math.max(fps, getMinDFps());
        }
        return fps;
    }

    public static boolean isModeSupported(String modeStr) {
        if ("*".equals(modeStr)) {
            return true;
        }
        try {
            return isModeSupported(Integer.parseInt(modeStr));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isModeSupported(int mode) {
        if (mode == 1) {
            return true;
        }
        return DISPLAY_CONFIGS.isRefreshRateSupported(mode);
    }

    public static int convertRefreshRate(float refreshRate) {
        return (int) (FPS_EPSILON + refreshRate);
    }

    public static boolean isSameFps(int fps1, int fps2) {
        return Math.abs(fps1 - fps2) <= 4;
    }

    public static int parseInteger(String value) {
        if (value.startsWith("0x") || value.startsWith("0X")) {
            return Integer.parseInt(value.substring(2), 16);
        }
        return Integer.parseInt(value);
    }

    public static boolean isTouchValid() {
        if (isIngoreInput()) {
            return false;
        }
        int min = getInteractionMinRefreshRate();
        int max = getInteractionMaxRefreshRate();
        return max > min;
    }

    private static void setUseMaxFpsWhenIgnoreInput(boolean value) {
        if (sUseMaxFpsWhenIgnoreInput != value) {
            sUseMaxFpsWhenIgnoreInput = value;
            sTmpUpdateRefreshRateModeRequest = true;
        }
    }

    public static boolean isUseMaxFpsWhenIgnoreInput() {
        return sUseMaxFpsWhenIgnoreInput;
    }

    public static boolean isIngoreInput() {
        if (isUseBrightness()) {
            if (isLowBrightness()) {
                return true;
            }
            if (IS_OLED_PANEL && isNightMode()) {
                return true;
            }
            return false;
        }
        return false;
    }

    public static boolean isAllowDump() {
        return SystemProperties.getBoolean("persist.rms.allow_dump", false) || SystemProperties.getBoolean("persist.sys.rms.allow_dump", false);
    }

    public static void setWindowInteractionFps(int min, int max, int configBits) {
        boolean update = false;
        if (sWindowInteractionMaxFps != max || sWindowInteractionMinFps != min) {
            sWindowInteractionMinFps = min;
            sWindowInteractionMaxFps = max;
            update = true;
        }
        if (sWindowConfigBits != configBits) {
            sWindowConfigBits = configBits;
            update = true;
        }
        if (update) {
            RefreshRateAdjuster.getInstance().updateRequest();
        }
    }

    public static void setWindowBrightnessValue(int brightness) {
        if (sWindowBrightnessValue != brightness) {
            sWindowBrightnessValue = brightness;
            RefreshRateAdjuster.getInstance().updateRequest();
        }
    }

    public static int getInteractionConfigBits() {
        return sWindowConfigBits;
    }

    public static int getInteractionMinRefreshRate() {
        int i = sWindowInteractionMinFps;
        return i > 0 ? i : getUserSettingMinRefreshRate();
    }

    public static int getInteractionMaxRefreshRate() {
        int i = sWindowInteractionMaxFps;
        return i > 0 ? i : getUserSettingMaxRefreshRate();
    }

    public static int getTouchBoostDuration() {
        return sTouchBoostDuration;
    }

    public static void setTouchBoostDuration(int duration) {
        sTouchBoostDuration = duration;
    }

    public static boolean hasPrivateFps() {
        return HAS_PRIVATE_FPS;
    }

    public static boolean isPrivateFps(int fps) {
        if (!HAS_PRIVATE_FPS) {
            return false;
        }
        return PRIVATE_FPS_LIST.contains(String.valueOf(fps));
    }

    public static void setAlwaysAutoTouch(boolean enable) {
        if (sAlwaysAutoTouch != enable) {
            sAlwaysAutoTouch = enable;
            sTmpUpdateRefreshRateModeRequest = true;
        }
    }

    public static boolean isAutoTouchMode() {
        return sAlwaysAutoTouch || (sAutoTouchInLowPowerMode && isLowPowerMode());
    }

    public static void setAutoTouchInLowPowerMode(boolean enable) {
        if (sAutoTouchInLowPowerMode != enable) {
            sAutoTouchInLowPowerMode = enable;
            sTmpUpdateRefreshRateModeRequest = true;
        }
    }

    public static boolean isAutoTouchInLowPowerMode() {
        return sAutoTouchInLowPowerMode;
    }

    public static int getUserSettingRefreshRateMode() {
        UsetSettingObserver usetSettingObserver = sUserSettingObserver;
        if (usetSettingObserver != null) {
            return usetSettingObserver.getUserRefreshRateMode();
        }
        return -1;
    }

    public static int getUserSettingMaxRefreshRate() {
        UsetSettingObserver usetSettingObserver = sUserSettingObserver;
        if (usetSettingObserver != null) {
            return usetSettingObserver.getUserMaxRefreshRate();
        }
        return -1;
    }

    public static int getUserSettingMinRefreshRate() {
        UsetSettingObserver usetSettingObserver = sUserSettingObserver;
        if (usetSettingObserver != null) {
            return usetSettingObserver.getUserMinRefreshRate();
        }
        return -1;
    }

    public static InputEventReceiver getInputEventReceiver() {
        return sInputEventReceiver;
    }

    public static DisplayObserver getDisplayObserver() {
        return sDisplayObserver;
    }

    public static void updateBbkLogStatus() {
        sBbkLogOn = "yes".equals(SystemProperties.get("persist.sys.log.ctrl", "no"));
    }

    public static void setDebug(boolean enable) {
        sDebug = enable;
    }

    public static boolean isDebug() {
        return sDebug || sBbkLogOn;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class UsetSettingObserver extends ContentObserver {
        private static final String KEY_POWER_SAVE_TYPE = "power_save_type";
        private static final int LOW_POWER_MODE_TYPE = 2;
        private static final int NORMAL_POWER_MODE_TYPE = 1;
        private Context mContext;
        private boolean mIsLowPowerMode;
        private int mNightMode;
        int mUserMaxRefreshRate;
        int mUserMinRefreshRate;
        private long mUserRefreshRateHandle;
        int mUserRefreshRateMode;
        private static final String KEY_USER_SETTING_RESOLUTION = "vivo_screen_resolution";
        private static final Uri URI_USER_SETTING_RESOLUTION = Settings.System.getUriFor(KEY_USER_SETTING_RESOLUTION);
        private static final String KEY_USER_SETTING_REFRESH_RATE = "vivo_screen_refresh_rate_mode";
        private static final Uri URI_USER_SETTING_REFRESH_RATE = Settings.Global.getUriFor(KEY_USER_SETTING_REFRESH_RATE);
        private static final String KEY_NIGHT_MODE = "vivo_nightmode_used";
        private static final Uri URI_KEY_NIGHT_MODE = Settings.System.getUriFor(KEY_NIGHT_MODE);
        private static final Uri URI_POWER_SAVE_TYPE = Settings.System.getUriFor("power_save_type");

        public UsetSettingObserver(Handler handler) {
            super(handler);
            this.mUserRefreshRateHandle = -1L;
            this.mUserRefreshRateMode = -1;
            this.mUserMinRefreshRate = -1;
            this.mUserMaxRefreshRate = -1;
            this.mNightMode = -2;
            this.mIsLowPowerMode = false;
        }

        public void observe(Context context) {
            this.mContext = context;
            ContentResolver cr = context.getContentResolver();
            cr.registerContentObserver(URI_USER_SETTING_RESOLUTION, false, this, 0);
            cr.registerContentObserver(URI_USER_SETTING_REFRESH_RATE, false, this, 0);
            cr.registerContentObserver(URI_POWER_SAVE_TYPE, false, this, 0);
            cr.registerContentObserver(URI_KEY_NIGHT_MODE, false, this, 0);
            updateResolution();
            updateNightMode();
            updateRefreshRateMode();
            updateLowPowerMode();
        }

        public int getUserRefreshRateMode() {
            return this.mUserRefreshRateMode;
        }

        public boolean isAutoRefreshRateMode() {
            return this.mUserRefreshRateMode == 1 || GlobalConfigs.isAutoTouchMode();
        }

        public int getUserMinRefreshRate() {
            return this.mUserMinRefreshRate;
        }

        public int getUserMaxRefreshRate() {
            return this.mUserMaxRefreshRate;
        }

        public boolean isNightMode() {
            return this.mNightMode == GlobalConfigs.sNightModeValue;
        }

        public boolean isLowPowerMode() {
            return this.mIsLowPowerMode;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (URI_USER_SETTING_REFRESH_RATE.equals(uri)) {
                updateRefreshRateMode();
            } else if (URI_KEY_NIGHT_MODE.equals(uri)) {
                updateNightMode();
                if (GlobalConfigs.isUseBrightness()) {
                    RefreshRateAdjuster.getInstance().updateRequest();
                }
            } else if (URI_POWER_SAVE_TYPE.equals(uri)) {
                updateLowPowerMode();
            } else if (URI_USER_SETTING_RESOLUTION.equals(uri)) {
                updateResolution();
            }
        }

        private void updateResolution() {
            String resolution = Settings.System.getString(this.mContext.getContentResolver(), KEY_USER_SETTING_RESOLUTION);
            if (resolution != null) {
                VLog.d("RefreshRateAdjuster", "updateResolution resolution=" + resolution);
                int index = resolution.indexOf("x");
                if (index != -1) {
                    try {
                        int width = Integer.valueOf(resolution.substring(0, index)).intValue();
                        int height = Integer.valueOf(resolution.substring(index + 1)).intValue();
                        RefreshRateAdjuster.getInstance().setResolution(width, height);
                    } catch (Exception e) {
                    }
                }
            }
        }

        private void updateNightMode() {
            this.mNightMode = Settings.System.getInt(this.mContext.getContentResolver(), KEY_NIGHT_MODE, -2);
            VLog.d("RefreshRateAdjuster", "updateNightMode mode=" + this.mNightMode);
        }

        private int getMinRefreshRate() {
            if (isAutoRefreshRateMode()) {
                int refreshRate = GlobalConfigs.DISPLAY_CONFIGS.getMinRefreshRate(GlobalConfigs.sTouchMinFps, Integer.MAX_VALUE);
                return refreshRate;
            }
            int refreshRate2 = Math.min(this.mUserRefreshRateMode, GlobalConfigs.DISPLAY_CONFIGS.getMaxRefreshRate());
            return refreshRate2;
        }

        private int getMaxRefreshRate() {
            int i = this.mUserRefreshRateMode;
            if (i == 1) {
                int refreshRate = GlobalConfigs.DISPLAY_CONFIGS.getMaxRefreshRate();
                return refreshRate;
            }
            int refreshRate2 = Math.min(i, GlobalConfigs.DISPLAY_CONFIGS.getMaxRefreshRate());
            return refreshRate2;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean updateRefreshRateModeRequest() {
            boolean update;
            synchronized (this) {
                update = false;
                int min = getMinRefreshRate();
                int max = getMaxRefreshRate();
                if (max != this.mUserMaxRefreshRate) {
                    this.mUserMaxRefreshRate = max;
                    update = true;
                }
                if (min != this.mUserMinRefreshRate) {
                    update = true;
                    this.mUserMinRefreshRate = min;
                    RefreshRateAdjuster.getInstance().releaseRefreshRate(this.mUserRefreshRateHandle);
                    this.mUserRefreshRateHandle = RefreshRateAdjuster.getInstance().acquireRefreshRate(SceneManager.INTERACTION_SCENE, "UserSetting", this.mUserMinRefreshRate, 0, 0, 0, 0, 0, false, 1);
                }
                RefreshRateAdjuster.getInstance().updateRequest();
            }
            return update;
        }

        private void updateRefreshRateMode() {
            int mode = Settings.Global.getInt(this.mContext.getContentResolver(), KEY_USER_SETTING_REFRESH_RATE, 1);
            if (mode != this.mUserRefreshRateMode) {
                this.mUserRefreshRateMode = mode;
                if (!GlobalConfigs.sFeatureEnabled) {
                    GlobalConfigs.setFeatureEnabled(true);
                }
                RefreshRateAdjuster.getInstance().reqeustResetActiveMode();
                updateRefreshRateModeRequest();
                WindowRequestManager.getInstance().updateRequest();
                VLog.d("RefreshRateAdjuster", "updateRefreshRateMode mode=" + this.mUserRefreshRateMode);
            }
        }

        private void updateLowPowerMode() {
            boolean lowPowerMode = Settings.System.getInt(this.mContext.getContentResolver(), "power_save_type", 1) == 2;
            if (lowPowerMode != this.mIsLowPowerMode) {
                this.mIsLowPowerMode = lowPowerMode;
                RefreshRateAdjuster.getInstance().updateRequest();
                VLog.d("RefreshRateAdjuster", "updateLowPowerMode mode=" + lowPowerMode);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                boolean updateRequest = msg.arg1 == 1;
                boolean screenStateChanged = msg.arg2 == 1;
                boolean isScreenOn = GlobalConfigs.isScreenOn();
                if (screenStateChanged && isScreenOn) {
                    RefreshRateAdjuster.getInstance().notifyInputEvent(null);
                }
                if (updateRequest) {
                    RefreshRateAdjuster.getInstance().updateRequest(8, 0);
                }
                if (screenStateChanged) {
                    RefreshRateAdjuster.getInstance().onScreenStateChanged(isScreenOn);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class DisplayObserver extends DisplayEventReceiver {
        public DisplayObserver(Handler handler) {
            super(handler.getLooper(), 0, 1);
        }

        public void onConfigChanged(long timestampNanos, long physicalDisplayId, int configId) {
            RefreshRateAdjuster.getInstance().onDisplayActiveModeChanged(physicalDisplayId, configId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MyInputEventReceiver extends InputEventReceiver {
        public MyInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        public void onInputEvent(InputEvent event) {
            try {
                if (event.getSource() != 4098) {
                    return;
                }
                RefreshRateAdjuster.getInstance().notifyInputEvent(event);
            } finally {
                finishInputEvent(event, true);
            }
        }
    }
}