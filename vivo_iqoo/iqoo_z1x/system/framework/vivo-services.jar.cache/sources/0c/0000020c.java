package com.android.server.display.color;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.Binder;
import android.os.Bundle;
import android.os.FtBuild;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.FtFeature;
import android.view.Display;
import com.android.server.LocalServices;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.android.server.twilight.TwilightListener;
import com.android.server.twilight.TwilightManager;
import com.android.server.twilight.TwilightState;
import com.vivo.services.proxy.broadcast.BroadcastConfigs;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.rms.display.DisplayConfigsManager;
import java.time.LocalDateTime;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.common.IVivoColorManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoColorManagerService {
    private static final String CHILD_MODE_EYE_PROTECTION_KEY = "children_mode_eyeprotection_key";
    private static final int COLOR_MODE_COLORTEMP = 4;
    private static final int COLOR_MODE_EYE_CARE = 1;
    private static final int NOT_SET = -1;
    private static final String SURFACE_FLINGER = "SurfaceFlinger";
    private static final int SURFACE_FLINGER_TRANSACTION_GET_COLOR_MODE = 31012;
    private static final int SURFACE_FLINGER_TRANSACTION_SET_COLOR_MODE = 31013;
    static final String TAG = "VivoColorManagerService";
    private static final String VIVO_AUTO_EYE_CCT_ENABLE = "vivo_auto_eye_cct_enable";
    public static final int VIVO_COLOR_MODE_AOD = 508;
    public static final int VIVO_COLOR_MODE_FINGERPRINT = 509;
    private static final String VIVO_COLOR_TEMPERATURE_CHANGE = "vivo_color_temperature_change";
    private static final String VIVO_DISPLAY_COLOR_MODE = "vivo_display_color_mode";
    private static final String VIVO_DISPLAY_COLOR_MODE_SETTING = "vivo_display_color_mode_setting";
    private static final String VIVO_NIGHT_DISPLAY_CHILD_MODE = "vivo_children_mode_enable";
    private static final String VIVO_NIGHT_DISPLAY_COLOR_TEMPERATURE = "vivo_night_display_color_temperature";
    private static final String VIVO_NIGHT_DISPLAY_EYECARE = "vivo_night_display_eyecare";
    public static final String VIVO_NIGHT_DISPLAY_EYEPROTECTION_RATIO = "vivo_night_display_eyeprotection_ratio";
    private static final String VIVO_NIGHT_DISPLAY_GAME_MODE = "is_game_mode";
    private static final String VIVO_NIGHT_DISPLAY_USER_AUTO_MODE = "vivo_night_display_user_auto_mode";
    private int mAutoMode;
    private ColorDisplayService mColorDisplayService;
    private ContentObserver mContentObserver;
    private final Context mContext;
    private DisplayManager mDisplayManager;
    private int mDisplayState;
    private Display mMainDisplay;
    private VivoNightDisplayNotification mVivoNightDisplayNotification;
    private static volatile VivoColorManagerService mVivoColorManager = null;
    private static final boolean mSupportColorManager = FtFeature.isFeatureSupport("vivo.hardware.color.management");
    private static final boolean mSupportSuperLcd = SystemProperties.getBoolean("persist.vivo.phone.super_lcd", false);
    private static String mPanelType = SystemProperties.get("persist.vivo.phone.panel_type", "unknown").toLowerCase();
    private int mColorTemp = -1;
    private int mColorEyePro = -1;
    private int mColorMode = -1;
    private int mBackupForAod = -1;
    private boolean mEyeProEnable = true;
    private boolean mNotificationEnable = false;
    public DisplayTransformManager mDtm = null;
    private int mCurrentUser = ProcessList.INVALID_ADJ;
    private final float[] mMatrix = new float[16];
    public BinderService mBinderService = new BinderService();
    private DisplayManager.DisplayListener mVivoDisplayListener = new DisplayManager.DisplayListener() { // from class: com.android.server.display.color.VivoColorManagerService.2
        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            int curDisplayState;
            if (displayId == 0 && VivoColorManagerService.this.mMainDisplay != null && (curDisplayState = VivoColorManagerService.this.mMainDisplay.getState()) != VivoColorManagerService.this.mDisplayState) {
                if ((curDisplayState != 3 && curDisplayState != 4) || VivoColorManagerService.this.mDisplayState == 3 || VivoColorManagerService.this.mDisplayState == 4) {
                    if ((VivoColorManagerService.this.mDisplayState == 3 || VivoColorManagerService.this.mDisplayState == 4) && curDisplayState != 3 && curDisplayState != 4 && VivoColorManagerService.this.getActualColorModeSetting() == 508) {
                        VivoColorManagerService vivoColorManagerService = VivoColorManagerService.this;
                        vivoColorManagerService.setColorModeInternal(vivoColorManagerService.mBackupForAod);
                    }
                } else if (VivoColorManagerService.this.mDtm != null && VivoColorManagerService.this.mDtm.isDisplayColorSupport((int) VivoColorManagerService.VIVO_COLOR_MODE_AOD)) {
                    VivoColorManagerService vivoColorManagerService2 = VivoColorManagerService.this;
                    vivoColorManagerService2.mBackupForAod = vivoColorManagerService2.getActualColorModeSetting();
                    VivoColorManagerService.this.setColorModeInternal(VivoColorManagerService.VIVO_COLOR_MODE_AOD);
                }
                VivoColorManagerService.this.mDisplayState = curDisplayState;
            }
        }
    };

    private VivoColorManagerService(ColorDisplayService colorDisplayService, Context context) {
        this.mDisplayState = 0;
        this.mDisplayManager = null;
        this.mMainDisplay = null;
        this.mColorDisplayService = null;
        this.mVivoNightDisplayNotification = null;
        this.mContext = context;
        this.mColorDisplayService = colorDisplayService;
        this.mVivoNightDisplayNotification = new VivoNightDisplayNotification();
        if (mSupportColorManager) {
            DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
            this.mDisplayManager = displayManager;
            if (displayManager != null) {
                Display display = displayManager.getDisplay(0);
                this.mMainDisplay = display;
                if (display != null) {
                    this.mDisplayState = display.getState();
                }
                this.mDisplayManager.registerDisplayListener(this.mVivoDisplayListener, colorDisplayService.mHandler);
            }
        }
    }

    public static VivoColorManagerService getInstance(ColorDisplayService colorDisplayService, Context context) {
        if (mVivoColorManager == null) {
            synchronized (VivoColorManagerService.class) {
                if (mVivoColorManager == null) {
                    mVivoColorManager = new VivoColorManagerService(colorDisplayService, context);
                }
            }
        }
        return mVivoColorManager;
    }

    public void setUp(int userHandle) {
        boolean userChanged = false;
        if (this.mCurrentUser != userHandle) {
            userChanged = true;
        }
        this.mCurrentUser = userHandle;
        this.mAutoMode = getNightDisplayAutoModeSetting();
        VSlog.d(TAG, "setUp: currentUser=" + this.mCurrentUser + " mUserActivated=" + isActivatedSetting() + " mAutoMode=" + this.mAutoMode);
        if (this.mContentObserver == null) {
            this.mContentObserver = new ContentObserver(this.mColorDisplayService.mHandler) { // from class: com.android.server.display.color.VivoColorManagerService.1
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    String setting = uri == null ? null : uri.getLastPathSegment();
                    VSlog.d(VivoColorManagerService.TAG, "onChange: setting=" + setting);
                    if (setting != null) {
                        char c = 65535;
                        switch (setting.hashCode()) {
                            case -1906220853:
                                if (setting.equals(VivoColorManagerService.VIVO_NIGHT_DISPLAY_EYECARE)) {
                                    c = 2;
                                    break;
                                }
                                break;
                            case -1773754913:
                                if (setting.equals(VivoColorManagerService.VIVO_NIGHT_DISPLAY_EYEPROTECTION_RATIO)) {
                                    c = 6;
                                    break;
                                }
                                break;
                            case -782730399:
                                if (setting.equals(VivoColorManagerService.VIVO_NIGHT_DISPLAY_COLOR_TEMPERATURE)) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case -310155013:
                                if (setting.equals("is_game_mode")) {
                                    c = 5;
                                    break;
                                }
                                break;
                            case 1286802191:
                                if (setting.equals(VivoColorManagerService.VIVO_DISPLAY_COLOR_MODE)) {
                                    c = 7;
                                    break;
                                }
                                break;
                            case 1399082858:
                                if (setting.equals(VivoColorManagerService.VIVO_COLOR_TEMPERATURE_CHANGE)) {
                                    c = 1;
                                    break;
                                }
                                break;
                            case 1554286350:
                                if (setting.equals(VivoColorManagerService.CHILD_MODE_EYE_PROTECTION_KEY)) {
                                    c = 4;
                                    break;
                                }
                                break;
                            case 1728631570:
                                if (setting.equals(VivoColorManagerService.VIVO_NIGHT_DISPLAY_CHILD_MODE)) {
                                    c = 3;
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                int colorTempSetting = VivoColorManagerService.this.getColorTemperatureSetting();
                                if (VivoColorManagerService.this.mColorTemp != colorTempSetting) {
                                    VivoColorManagerService.this.mColorTemp = colorTempSetting;
                                    VivoColorManagerService vivoColorManagerService = VivoColorManagerService.this;
                                    vivoColorManagerService.onColorTemperatureChanged(vivoColorManagerService.mColorTemp);
                                    return;
                                }
                                return;
                            case 1:
                                VivoColorManagerService vivoColorManagerService2 = VivoColorManagerService.this;
                                vivoColorManagerService2.onNightDisplayChangedAnimation(vivoColorManagerService2.getColorTemperatureSetting());
                                return;
                            case 2:
                                int eyeProSetting = VivoColorManagerService.this.getEyeProtectionSetting();
                                if (VivoColorManagerService.this.mColorEyePro != eyeProSetting) {
                                    VivoColorManagerService.this.mColorEyePro = eyeProSetting;
                                    VivoColorManagerService vivoColorManagerService3 = VivoColorManagerService.this;
                                    vivoColorManagerService3.onEyeProtectionChanged(vivoColorManagerService3.mColorEyePro);
                                    return;
                                }
                                return;
                            case 3:
                                if (!VivoColorManagerService.this.isChildrenMode()) {
                                    boolean activated = VivoColorManagerService.this.isActivatedSetting();
                                    if (activated != VivoColorManagerService.this.isChildrenModeEyeProtectionKeyOn() && (VivoColorManagerService.this.mColorMode != 509 || !activated)) {
                                        VivoColorManagerService vivoColorManagerService4 = VivoColorManagerService.this;
                                        vivoColorManagerService4.onNightDisplayChangedAnimation(vivoColorManagerService4.getColorTemperature());
                                    }
                                    VivoColorManagerService vivoColorManagerService5 = VivoColorManagerService.this;
                                    vivoColorManagerService5.mAutoMode = vivoColorManagerService5.getNightDisplayUserAutoModeSetting();
                                    if (VivoColorManagerService.this.mAutoMode != 0) {
                                        VivoColorManagerService vivoColorManagerService6 = VivoColorManagerService.this;
                                        vivoColorManagerService6.setNightDisplayAutoMode(vivoColorManagerService6.mAutoMode);
                                        return;
                                    }
                                    return;
                                }
                                VivoColorManagerService vivoColorManagerService7 = VivoColorManagerService.this;
                                vivoColorManagerService7.mAutoMode = vivoColorManagerService7.getNightDisplayAutoModeSetting();
                                if (VivoColorManagerService.this.mAutoMode != 0) {
                                    VivoColorManagerService.this.setNightDisplayAutoMode(0);
                                }
                                VivoColorManagerService vivoColorManagerService8 = VivoColorManagerService.this;
                                vivoColorManagerService8.saveNightDisplayUserAutoModeSetting(vivoColorManagerService8.mAutoMode);
                                boolean activated2 = VivoColorManagerService.this.isActivatedSetting();
                                if ((!activated2 && VivoColorManagerService.this.isChildrenModeEyeProtectionKeyOn()) || (activated2 && !VivoColorManagerService.this.isChildrenModeEyeProtectionKeyOn())) {
                                    VivoColorManagerService vivoColorManagerService9 = VivoColorManagerService.this;
                                    vivoColorManagerService9.onNightDisplayChangedAnimation(vivoColorManagerService9.getColorTemperature());
                                    return;
                                }
                                return;
                            case 4:
                                if (VivoColorManagerService.this.isChildrenMode()) {
                                    VivoColorManagerService vivoColorManagerService10 = VivoColorManagerService.this;
                                    vivoColorManagerService10.onNightDisplayChangedAnimation(vivoColorManagerService10.getColorTemperature());
                                    return;
                                }
                                return;
                            case 5:
                                if (VivoColorManagerService.this.mVivoNightDisplayNotification != null && VivoColorManagerService.this.mVivoNightDisplayNotification.isNotificationPending()) {
                                    VivoColorManagerService.this.mVivoNightDisplayNotification.updateNotification(VivoColorManagerService.this.mVivoNightDisplayNotification.getTwilightState());
                                }
                                if (VivoColorManagerService.this.isActivatedSetting()) {
                                    VivoColorManagerService vivoColorManagerService11 = VivoColorManagerService.this;
                                    vivoColorManagerService11.onNightDisplayChangedAnimation(vivoColorManagerService11.getEyeProtectionSetting());
                                    return;
                                }
                                return;
                            case 6:
                                if (VivoColorManagerService.this.isActivatedSetting()) {
                                    VivoColorManagerService vivoColorManagerService12 = VivoColorManagerService.this;
                                    vivoColorManagerService12.onNightDisplayChangedAnimation(vivoColorManagerService12.getEyeProtectionSetting());
                                    return;
                                }
                                return;
                            case 7:
                                int colorMode = VivoColorManagerService.this.getActualColorModeSetting();
                                if (VivoColorManagerService.this.mColorMode != colorMode) {
                                    VivoColorManagerService.this.mColorMode = colorMode;
                                    VivoColorManagerService.this.onVivoDisplayColorModeChanged(colorMode);
                                    return;
                                }
                                return;
                            default:
                                return;
                        }
                    }
                }
            };
        }
        ContentResolver cr = this.mContext.getContentResolver();
        cr.registerContentObserver(Settings.Secure.getUriFor(VIVO_NIGHT_DISPLAY_COLOR_TEMPERATURE), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor(VIVO_NIGHT_DISPLAY_EYECARE), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor(VIVO_COLOR_TEMPERATURE_CHANGE), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.System.getUriFor(VIVO_NIGHT_DISPLAY_CHILD_MODE), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.System.getUriFor(CHILD_MODE_EYE_PROTECTION_KEY), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.System.getUriFor("is_game_mode"), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor(VIVO_NIGHT_DISPLAY_EYEPROTECTION_RATIO), false, this.mContentObserver, this.mCurrentUser);
        if (mSupportSuperLcd || !mPanelType.startsWith("tft")) {
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_DISPLAY_COLOR_MODE), false, this.mContentObserver, this.mCurrentUser);
            if (getActualColorModeSetting() <= 2 && userChanged) {
                onVivoDisplayColorModeChanged(getUserColorModeSetting());
            }
        }
        VivoNightDisplayNotification vivoNightDisplayNotification = this.mVivoNightDisplayNotification;
        if (vivoNightDisplayNotification != null && vivoNightDisplayNotification.isNotificationEnable() && userHandle == 0) {
            setNightDisplayNotificationEnable(true);
        }
        if (isChildrenMode()) {
            VSlog.i(TAG, "SetUp Finished isChildrenMode");
            onNightDisplayChangedAnimation(getColorTemperature());
        }
    }

    public void tearDown() {
        VSlog.d(TAG, "tearDown: currentUser=" + this.mCurrentUser);
        if (this.mContentObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
        }
        VivoNightDisplayNotification vivoNightDisplayNotification = this.mVivoNightDisplayNotification;
        if (vivoNightDisplayNotification != null && this.mCurrentUser == 0) {
            vivoNightDisplayNotification.onStop();
        }
    }

    public void onStartUser(int userHandle, DisplayTransformManager dtm) {
        this.mCurrentUser = userHandle;
        this.mDtm = dtm;
        int activated = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "night_display_activated", -1, -2);
        if (activated == -1) {
            int colortempMode = SystemProperties.getInt("persist.sys.colortemp.en.vivo", 4);
            int bluelightMode = SystemProperties.getInt("persist.sys.bluelight.en.vivo", 0);
            int mtkBluelightMode = SystemProperties.getInt("persist.sys.mtkbluelight.en.vivo", 0);
            int mode = colortempMode | bluelightMode | mtkBluelightMode;
            int activated2 = mode == 1 ? 1 : 0;
            VSlog.d(TAG, "vivo night display init, activated=" + activated2);
            setActivated(Boolean.valueOf(mode == 1));
        }
        int factor = getEyeProtectionSetting();
        if (factor == -1) {
            int factor2 = SystemProperties.getInt("persist.sys.bluelight.ui.vivo", (int) KernelConfig.FRC_LOW_LATENCY);
            int mtkFactor = SystemProperties.getInt("persist.sys.mtkbluelight.ui.vivo", (int) KernelConfig.FRC_LOW_LATENCY);
            if (mtkFactor != 127) {
                factor2 = mtkFactor;
            }
            this.mColorEyePro = factor2;
            setEyeProtectionSetting(factor2);
            VSlog.d(TAG, "vivo night display first init, mColorEyePro=" + this.mColorEyePro);
        }
        int factor3 = getColorTemperatureSetting();
        if (factor3 == -1) {
            int factor4 = SystemProperties.getInt("persist.sys.colortemp.ui.vivo", (int) KernelConfig.FRC_LOW_LATENCY);
            this.mColorTemp = factor4;
            setColorTemperatureSetting(factor4);
            VSlog.d(TAG, "vivo night display first init, mColorTemp=" + this.mColorTemp);
        }
        if (mSupportSuperLcd || !mPanelType.startsWith("tft")) {
            int colorMode = getUserColorModeSetting();
            if (colorMode == -1) {
                colorMode = SystemProperties.getInt("persist.sys.colourgamut.en.vivo", 0);
                VSlog.d(TAG, "vivo display color mode first init, colorMode=" + colorMode);
                setUserColorModeSetting(colorMode);
            }
            setColorModeInternal(colorMode);
        } else if (!isChildrenMode()) {
            onNightDisplayChangedAnimation(getColorTemperature());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onVivoDisplayColorModeChanged(int colorMode) {
        VSlog.d(TAG, "onVivoDisplayColorModeChanged  colorMode=" + colorMode);
        setColorModeNative(colorMode);
        if ((colorMode <= 2 || colorMode == 509) && this.mEyeProEnable) {
            onNightDisplayChangedImmediate(getColorTemperature());
        }
    }

    public boolean setColorMode(int colorMode) {
        VSlog.d(TAG, "setColorMode ---mode=" + colorMode);
        int vivoColorMode = colorMode;
        if (vivoColorMode >= 2) {
            vivoColorMode = 2;
        }
        setUserColorModeSetting(vivoColorMode);
        setColorModeInternal(vivoColorMode);
        return true;
    }

    public int getColorMode() {
        int colorMode = getUserColorModeSetting();
        VSlog.d(TAG, "getColorMode ---mode=" + colorMode);
        return colorMode;
    }

    private double[] getBlueLightRGBFactor(int factor) {
        double factor_b;
        int min_color_r;
        int def_color_r;
        int min_color_r2;
        int def_color_r2;
        int min_color_b;
        int min_color_g;
        long j;
        long j2;
        double[] retVal = {0.0d, 0.0d, 0.0d};
        double ratio_r = 1.0d;
        double ratio_g = 1.0d;
        double ratio_b = 1.0d;
        double factor_r = 1.0d;
        int colorMode = getActualColorModeSetting();
        double factor_g = 1.0d;
        int eye_ratio = SystemProperties.getInt("persist.system.vivo.bluelight.ratio", 255);
        double factor_b2 = factor;
        int factor2 = (int) (factor_b2 * (eye_ratio / 255.0d));
        if (mSupportColorManager && !mPanelType.startsWith("tft")) {
            if (colorMode == 1) {
                int min_color_r3 = SystemProperties.getInt("persist.sys.bluelight.manage.minred", 255);
                int min_color_g2 = SystemProperties.getInt("persist.sys.bluelight.manage.mingreen", 177);
                int min_color_b2 = SystemProperties.getInt("persist.sys.bluelight.manage.minblue", 100);
                int def_color_r3 = SystemProperties.getInt("persist.sys.bluelight.manage.defred", 233);
                int def_color_g = SystemProperties.getInt("persist.sys.bluelight.manage.defgreen", 210);
                min_color_r = min_color_r3;
                def_color_r = def_color_r3;
                min_color_r2 = def_color_g;
                def_color_r2 = min_color_b2;
                min_color_b = SystemProperties.getInt("persist.sys.bluelight.manage.defblue", 170);
                min_color_g = min_color_g2;
            } else if (colorMode == 509) {
                int min_color_r4 = SystemProperties.getInt("persist.sys.bluelight.fingerprint.minred", 255);
                int min_color_g3 = SystemProperties.getInt("persist.sys.bluelight.fingerprint.mingreen", 218);
                int min_color_b3 = SystemProperties.getInt("persist.sys.bluelight.fingerprint.minblue", 157);
                def_color_r = SystemProperties.getInt("persist.sys.bluelight.fingerprint.defred", 255);
                min_color_r = min_color_r4;
                int def_color_g2 = SystemProperties.getInt("persist.sys.bluelight.fingerprint.defgreen", 235);
                min_color_r2 = def_color_g2;
                def_color_r2 = min_color_b3;
                min_color_b = SystemProperties.getInt("persist.sys.bluelight.fingerprint.defblue", 198);
                min_color_g = min_color_g3;
            } else {
                int min_color_r5 = SystemProperties.getInt("persist.sys.bluelight.standard.minred", 245);
                int min_color_g4 = SystemProperties.getInt("persist.sys.bluelight.standard.mingreen", 165);
                int min_color_b4 = SystemProperties.getInt("persist.sys.bluelight.standard.minblue", 75);
                int def_color_r4 = SystemProperties.getInt("persist.sys.bluelight.standard.defred", (int) BroadcastConfigs.PROXY_BR_ABNORMAL_SIZE);
                int def_color_g3 = SystemProperties.getInt("persist.sys.bluelight.standard.defgreen", 200);
                min_color_r = min_color_r5;
                def_color_r = def_color_r4;
                min_color_r2 = def_color_g3;
                def_color_r2 = min_color_b4;
                min_color_b = SystemProperties.getInt("persist.sys.bluelight.standard.defblue", 145);
                min_color_g = min_color_g4;
            }
            if (factor2 < 0 || factor2 > 127) {
                j = 0;
                if (factor2 > 127 && factor2 <= 255) {
                    double d = def_color_r;
                    int def_color_r5 = factor2 - 128;
                    j2 = 0;
                    double factor_r2 = (d - (((def_color_r - min_color_r) / 127.0d) * def_color_r5)) / 255.0d;
                    factor_r = factor_r2;
                    double factor_r3 = factor2 - 128;
                    double factor_g2 = (min_color_r2 - (((min_color_r2 - min_color_g) / 127.0d) * factor_r3)) / 255.0d;
                    factor_g = factor_g2;
                    factor_b = (min_color_b - (((min_color_b - def_color_r2) / 127.0d) * (factor2 - 128))) / 255.0d;
                } else {
                    j2 = 0;
                    factor_b = 1.0d;
                }
            } else {
                j = 0;
                double factor_r4 = (255.0d - (((255.0d - def_color_r) / 127.0d) * factor2)) / 255.0d;
                factor_r = factor_r4;
                double factor_r5 = factor2;
                double factor_g3 = (255.0d - (((255.0d - min_color_r2) / 127.0d) * factor_r5)) / 255.0d;
                factor_g = factor_g3;
                double factor_g4 = factor2;
                factor_b = (255.0d - (((255.0d - min_color_b) / 127.0d) * factor_g4)) / 255.0d;
                j2 = 0;
            }
        } else {
            int min_color_r6 = SystemProperties.getInt("persist.sys.bluelight.minred", 248);
            int min_color_g5 = SystemProperties.getInt("persist.sys.bluelight.mingreen", 214);
            int min_color_b5 = SystemProperties.getInt("persist.sys.bluelight.minblue", 153);
            if (!mPanelType.startsWith("tft") && mSupportSuperLcd && colorMode != 509) {
                min_color_r6 = SystemProperties.getInt("persist.sys.bluelight.e1pro.minred", 255);
                min_color_g5 = SystemProperties.getInt("persist.sys.bluelight.e1pro.mingreen", 175);
                min_color_b5 = SystemProperties.getInt("persist.sys.bluelight.e1pro.minblue", 85);
            }
            int factor3 = 255 - factor2;
            double temp_r = min_color_r6;
            double temp_g = min_color_g5;
            double temp_b = min_color_b5;
            if (factor3 == 255) {
                factor_r = 1.0d;
            } else {
                factor_r = temp_r / 255.0d;
            }
            factor_g = (temp_g + (((255.0d - temp_g) * factor3) / 255.0d)) / 255.0d;
            factor_b = ((((255.0d - temp_b) * factor3) / 255.0d) + temp_b) / 255.0d;
        }
        if (FtBuild.isMTKPlatform() && mSupportSuperLcd && colorMode == 1) {
            ratio_r = SystemProperties.getInt("persist.sys.rgbgain.ratiored", 255) / 255.0d;
            ratio_g = SystemProperties.getInt("persist.sys.rgbgain.ratiogreen", 252) / 255.0d;
            ratio_b = SystemProperties.getInt("persist.sys.rgbgain.ratioblue", 203) / 255.0d;
        }
        retVal[0] = factor_r * ratio_r;
        retVal[1] = factor_g * ratio_g;
        retVal[2] = factor_b * ratio_b;
        return retVal;
    }

    private double[] getColorTempRGBFactor(int factor) {
        int min_colortemp_b;
        int min_colortemp_b2;
        int max_colortemp_r;
        int min_colortemp_g;
        double factor_b;
        int max_colortemp_b;
        int max_colortemp_b2;
        double ratio_r;
        double ratio_g;
        double ratio_b;
        double factor_r;
        double factor_r2;
        double ratio_r2;
        double ratio_g2;
        double[] retVal = {0.0d, 0.0d, 0.0d};
        int colorMode = getActualColorModeSetting();
        int min_colortemp_r = SystemProperties.getInt("persist.sys.colortemp.minred", 255);
        int min_colortemp_g2 = SystemProperties.getInt("persist.sys.colortemp.mingreen", 241);
        int min_colortemp_b3 = SystemProperties.getInt("persist.sys.colortemp.minblue", 198);
        int max_colortemp_r2 = SystemProperties.getInt("persist.sys.colortemp.maxred", 238);
        int max_colortemp_g = SystemProperties.getInt("persist.sys.colortemp.maxgreen", 243);
        int max_colortemp_b3 = SystemProperties.getInt("persist.sys.colortemp.maxblue", 255);
        if (mSupportColorManager && !mPanelType.startsWith("tft") && colorMode != 509) {
            if (colorMode == 1) {
                int min_colortemp_r2 = SystemProperties.getInt("persist.sys.colortemp.manage.minred", 255);
                int min_colortemp_g3 = SystemProperties.getInt("persist.sys.colortemp.manage.mingreen", (int) DisplayConfigsManager.DisplayMode.BIT_AUTO_MODE_1HZ_BRIGHTNESS);
                int min_colortemp_b4 = SystemProperties.getInt("persist.sys.colortemp.manage.minblue", 177);
                int max_colortemp_r3 = SystemProperties.getInt("persist.sys.colortemp.manage.maxred", 208);
                int max_colortemp_g2 = SystemProperties.getInt("persist.sys.colortemp.manage.maxgreen", 220);
                int max_colortemp_b4 = SystemProperties.getInt("persist.sys.colortemp.manage.maxblue", 255);
                min_colortemp_b2 = min_colortemp_b4;
                max_colortemp_r = max_colortemp_r3;
                min_colortemp_b = min_colortemp_g3;
                min_colortemp_g = max_colortemp_g2;
                factor_b = 1.0d;
                max_colortemp_b = max_colortemp_b4;
                max_colortemp_b2 = min_colortemp_r2;
            } else {
                int min_colortemp_r3 = SystemProperties.getInt("persist.sys.colortemp.standard.minred", 255);
                int min_colortemp_g4 = SystemProperties.getInt("persist.sys.colortemp.standard.mingreen", 230);
                int min_colortemp_b5 = SystemProperties.getInt("persist.sys.colortemp.standard.minblue", 145);
                int max_colortemp_r4 = SystemProperties.getInt("persist.sys.colortemp.standard.maxred", 216);
                int max_colortemp_g3 = SystemProperties.getInt("persist.sys.colortemp.standard.maxgreen", 233);
                int max_colortemp_b5 = SystemProperties.getInt("persist.sys.colortemp.standard.maxblue", 255);
                min_colortemp_b2 = min_colortemp_b5;
                min_colortemp_b = min_colortemp_g4;
                max_colortemp_r = max_colortemp_r4;
                factor_b = 1.0d;
                max_colortemp_b = max_colortemp_b5;
                max_colortemp_b2 = min_colortemp_r3;
                min_colortemp_g = max_colortemp_g3;
            }
        } else if (!mPanelType.startsWith("tft") && mSupportSuperLcd && colorMode != 509) {
            int min_colortemp_r4 = SystemProperties.getInt("persist.sys.colortemp.e1pro.minred", 255);
            int min_colortemp_g5 = SystemProperties.getInt("persist.sys.colortemp.e1pro.mingreen", 220);
            int min_colortemp_b6 = SystemProperties.getInt("persist.sys.colortemp.e1pro.minblue", 145);
            int max_colortemp_r5 = SystemProperties.getInt("persist.sys.colortemp.e1pro.maxred", 230);
            int max_colortemp_g4 = SystemProperties.getInt("persist.sys.colortemp.e1pro.maxgreen", 233);
            int max_colortemp_b6 = SystemProperties.getInt("persist.sys.colortemp.e1pro.maxblue", 255);
            min_colortemp_b2 = min_colortemp_b6;
            max_colortemp_r = max_colortemp_r5;
            min_colortemp_b = min_colortemp_g5;
            min_colortemp_g = max_colortemp_g4;
            factor_b = 1.0d;
            max_colortemp_b = max_colortemp_b6;
            max_colortemp_b2 = min_colortemp_r4;
        } else {
            min_colortemp_b = min_colortemp_g2;
            min_colortemp_b2 = min_colortemp_b3;
            max_colortemp_r = max_colortemp_r2;
            min_colortemp_g = max_colortemp_g;
            factor_b = 1.0d;
            max_colortemp_b = max_colortemp_b3;
            max_colortemp_b2 = min_colortemp_r;
        }
        if (factor < 0 || factor > 127) {
            ratio_r = 1.0d;
            ratio_g = 1.0d;
            ratio_b = 1.0d;
            if (factor > 127 && factor <= 255) {
                factor_r = (255.0d - (((255.0d - max_colortemp_b2) / 127.0d) * (factor - 128))) / 255.0d;
                double factor_g = (255.0d - (((255.0d - min_colortemp_b) / 127.0d) * (factor - 128))) / 255.0d;
                factor_r2 = factor_g;
                factor_b = (255.0d - (((255.0d - min_colortemp_b2) / 127.0d) * (factor - 128))) / 255.0d;
            } else {
                factor_r = 1.0d;
                factor_r2 = 1.0d;
            }
        } else {
            ratio_r = 1.0d;
            ratio_g = 1.0d;
            ratio_b = 1.0d;
            double factor_r3 = (max_colortemp_r + (((255.0d - max_colortemp_r) / 127.0d) * factor)) / 255.0d;
            double factor_g2 = (min_colortemp_g + (((255.0d - min_colortemp_g) / 127.0d) * factor)) / 255.0d;
            factor_r2 = factor_g2;
            double factor_g3 = factor;
            factor_b = (max_colortemp_b + (((255.0d - max_colortemp_b) / 127.0d) * factor_g3)) / 255.0d;
            factor_r = factor_r3;
        }
        if (FtBuild.isMTKPlatform() && mSupportSuperLcd && colorMode == 1) {
            ratio_r2 = SystemProperties.getInt("persist.sys.rgbgain.ratiored", 255) / 255.0d;
            ratio_g2 = SystemProperties.getInt("persist.sys.rgbgain.ratiogreen", 252) / 255.0d;
            ratio_b = SystemProperties.getInt("persist.sys.rgbgain.ratioblue", 203) / 255.0d;
        } else {
            ratio_r2 = ratio_r;
            ratio_g2 = ratio_g;
        }
        retVal[0] = factor_r * ratio_r2;
        retVal[1] = factor_r2 * ratio_g2;
        retVal[2] = factor_b * ratio_b;
        return retVal;
    }

    private void setColorTemperatureMatrix(int temperature) {
        double[] dArr = {0.0d, 0.0d, 0.0d};
        if (isAccessiblityAutoEyeCctEnabled()) {
            VSlog.d(TAG, "ignore setColorTemperatureMatrix when enable auto cct.");
            Matrix.setIdentityM(this.mMatrix, 0);
            return;
        }
        double[] RGB = getColorTempRGBFactor(temperature);
        Matrix.setIdentityM(this.mMatrix, 0);
        float[] fArr = this.mMatrix;
        fArr[0] = (float) RGB[0];
        fArr[5] = (float) RGB[1];
        fArr[10] = (float) RGB[2];
        VSlog.d(TAG, "setColorTemperatureMatrix factor=" + temperature + " R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
    }

    private void setEyeProtectionMatrix(int temperature) {
        double[] dArr = {0.0d, 0.0d, 0.0d};
        float ratio = getEyeProtectionRatio();
        if (ratio == 1.0d) {
            ratio = isGameMode() ? 0.7f : 1.0f;
        }
        double[] RGB = getBlueLightRGBFactor((int) (temperature * ratio));
        Matrix.setIdentityM(this.mMatrix, 0);
        float[] fArr = this.mMatrix;
        fArr[0] = (float) RGB[0];
        fArr[5] = (float) RGB[1];
        fArr[10] = (float) RGB[2];
        VSlog.d(TAG, "setEyecareMatrix gameMode=" + isGameMode() + " factor=" + temperature + " R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
    }

    public void setNightDisplayColorMatrix(int cct) {
        if (isChildrenMode()) {
            if (isChildrenModeEyeProtectionKeyOn()) {
                setEyeProtectionMatrix(cct);
            } else {
                setColorTemperatureMatrix(cct);
            }
        } else if (isActivatedSetting()) {
            setEyeProtectionMatrix(cct);
        } else {
            setColorTemperatureMatrix(cct);
        }
    }

    public float[] getNightDisplayColorMatrix() {
        return this.mMatrix;
    }

    public int getColorTemperature() {
        return isChildrenMode() ? isChildrenModeEyeProtectionKeyOn() ? getEyeProtectionSetting() : getColorTemperatureSetting() : isActivatedSetting() ? getEyeProtectionSetting() : getColorTemperatureSetting();
    }

    public boolean setColorTemperature(int temperature) {
        return isActivatedSetting() ? setEyeProtectionSetting(temperature) : setColorTemperatureSetting(temperature);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isChildrenMode() {
        String result = Settings.System.getStringForUser(this.mContext.getContentResolver(), VIVO_NIGHT_DISPLAY_CHILD_MODE, -2);
        VSlog.d(TAG, "isChildrenMode: " + result);
        return "true".equals(result);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isChildrenModeEyeProtectionKeyOn() {
        int key = Settings.System.getIntForUser(this.mContext.getContentResolver(), CHILD_MODE_EYE_PROTECTION_KEY, 1, -2);
        VSlog.d(TAG, "isChildrenModeEyeProtectionKeyOn: " + key);
        return key == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isGameMode() {
        int result = Settings.System.getIntForUser(this.mContext.getContentResolver(), "is_game_mode", 0, -2);
        return 1 == result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isActivatedSetting() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "night_display_activated", 0, -2) == 1;
    }

    private void setActivated(Boolean activated) {
        VSlog.d(TAG, "setActivated: " + activated);
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "night_display_activated", activated.booleanValue() ? 1 : 0, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onColorTemperatureChanged(int temperature) {
        setColorTemperatureMatrix(temperature);
        this.mColorDisplayService.mHandler.sendEmptyMessage(2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onEyeProtectionChanged(int temperature) {
        setNightDisplayNotificationEnable(false);
        setEyeProtectionMatrix(temperature);
        this.mColorDisplayService.mHandler.sendEmptyMessage(2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onNightDisplayChangedAnimation(int temperature) {
        setNightDisplayColorMatrix(temperature);
        this.mColorDisplayService.mHandler.sendEmptyMessage(3);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onNightDisplayChangedImmediate(int temperature) {
        setNightDisplayColorMatrix(temperature);
        DisplayTransformManager displayTransformManager = this.mDtm;
        if (displayTransformManager != null) {
            displayTransformManager.setColorMatrix(100, getNightDisplayColorMatrix());
        } else {
            this.mColorDisplayService.mHandler.sendEmptyMessage(2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setNightDisplayAutoMode(int autoMode) {
        VSlog.d(TAG, "setNightDisplayAutoMode: " + autoMode);
        return Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "night_display_auto_mode", autoMode, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getNightDisplayAutoModeSetting() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "night_display_auto_mode", 0, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean saveNightDisplayUserAutoModeSetting(int autoMode) {
        return Settings.Secure.putIntForUser(this.mContext.getContentResolver(), VIVO_NIGHT_DISPLAY_USER_AUTO_MODE, autoMode, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getNightDisplayUserAutoModeSetting() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), VIVO_NIGHT_DISPLAY_USER_AUTO_MODE, 0, -2);
    }

    public void setNightDisplayNotificationEnable(Boolean enable) {
        if (this.mVivoNightDisplayNotification == null || this.mNotificationEnable == enable.booleanValue()) {
            return;
        }
        this.mNotificationEnable = enable.booleanValue();
        VSlog.d(TAG, "setNightDisplayNotificationEnable: " + enable);
        if (enable.booleanValue()) {
            this.mVivoNightDisplayNotification.onStart();
            return;
        }
        this.mVivoNightDisplayNotification.setNotificationCount(0);
        this.mVivoNightDisplayNotification.removeNotification();
        this.mVivoNightDisplayNotification.onStop();
    }

    public float getEyeProtectionRatio() {
        return Settings.Secure.getFloatForUser(this.mContext.getContentResolver(), VIVO_NIGHT_DISPLAY_EYEPROTECTION_RATIO, 1.0f, -2);
    }

    public void setEyeProtectionRatio(float ratio) {
        VSlog.d(TAG, "setEyeProtectionRatio: " + ratio);
        Settings.Secure.putFloatForUser(this.mContext.getContentResolver(), VIVO_NIGHT_DISPLAY_EYEPROTECTION_RATIO, ratio, -2);
    }

    private boolean isAccessiblityAutoEyeCctEnabled() {
        if (this.mCurrentUser == -10000) {
            this.mCurrentUser = -2;
        }
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_AUTO_EYE_CCT_ENABLE, 0, this.mCurrentUser) != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class VivoNightDisplayNotification implements TwilightListener {
        private static final String CHANNEL_ID = "Eye";
        private static final int NOTIFICATION_MAX_COUNT = 3;
        private static final int NOTIFICATION_STATE_INIT = 0;
        private static final int NOTIFICATION_STATE_PEND_SEND = 2;
        private static final int NOTIFICATION_STATE_REMOVED = 3;
        private static final int NOTIFICATION_STATE_SENDED = 1;
        private static final String VIVO_ACTION_NOTIFICATION_CLICK = "vivo.night.display.action.notification.click";
        private static final String VIVO_ACTION_NOTIFICATION_DELETED = "vivo.night.display.action.notification.deleted";
        private static final int VIVO_NIGHT_DISPLAY_NOTIFICATION_ID = 1;
        private static final String VIVO_NOTIFICATION_COUNT = "persist.sys.notification.count";
        private int mNotificationCount;
        private NotificationManager mNotificationManager;
        private boolean mNotificationStart;
        private int mNotificationState;
        private Resources mResources;
        private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.display.color.VivoColorManagerService.VivoNightDisplayNotification.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (intent == null || context == null) {
                    return;
                }
                VSlog.d(VivoColorManagerService.TAG, "onReceive: " + intent);
                if (intent.getAction().equals(VivoNightDisplayNotification.VIVO_ACTION_NOTIFICATION_DELETED)) {
                    VivoColorManagerService.this.setNightDisplayNotificationEnable(false);
                } else if (intent.getAction().equals(VivoNightDisplayNotification.VIVO_ACTION_NOTIFICATION_CLICK)) {
                    VivoColorManagerService.this.setNightDisplayNotificationEnable(false);
                    Intent startActivityIntent = new Intent();
                    startActivityIntent.setPackage(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS);
                    startActivityIntent.setAction("vivo.intent.action.NightMode");
                    startActivityIntent.putExtra("comefrom", "push");
                    context.startActivityAsUser(startActivityIntent, UserHandle.CURRENT);
                }
            }
        };
        private final TwilightManager mTwilightManager = (TwilightManager) LocalServices.getService(TwilightManager.class);

        public VivoNightDisplayNotification() {
            this.mResources = VivoColorManagerService.this.mContext.getResources();
            this.mNotificationManager = (NotificationManager) VivoColorManagerService.this.mContext.getSystemService("notification");
            registerNotificationChannel();
        }

        public TwilightState getTwilightState() {
            return this.mTwilightManager.getLastTwilightState();
        }

        public void updateNotification(TwilightState state) {
            if (state == null) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime sunrise = state.sunrise();
            LocalDateTime sunset = state.sunset();
            boolean isSunset = state.isNight();
            VSlog.d(VivoColorManagerService.TAG, " updateNotification now=" + now + " sunrise=" + sunrise + " sunset=" + sunset + " isSunset=" + isSunset);
            if (isSunset && this.mNotificationCount > 0) {
                if (!VivoColorManagerService.this.isGameMode()) {
                    VSlog.d(VivoColorManagerService.TAG, "SystemClock.elapsedRealtime() = " + SystemClock.elapsedRealtime());
                    if (SystemClock.elapsedRealtime() > 10800000) {
                        sendNotification();
                        return;
                    }
                    return;
                }
                VSlog.d(VivoColorManagerService.TAG, "Foregound is game, pend send notification!");
                this.mNotificationState = 2;
            } else if (this.mNotificationState == 2) {
                this.mNotificationState = 0;
            } else {
                removeNotification();
            }
        }

        public void onStart() {
            this.mTwilightManager.registerListener(this, VivoColorManagerService.this.mColorDisplayService.mHandler);
            this.mNotificationStart = true;
            IntentFilter filter = new IntentFilter(VIVO_ACTION_NOTIFICATION_DELETED);
            filter.addAction(VIVO_ACTION_NOTIFICATION_CLICK);
            VivoColorManagerService.this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
            this.mNotificationCount = getNotificationCount();
            VSlog.d(VivoColorManagerService.TAG, "onStart: notification remaining count=" + this.mNotificationCount);
        }

        public void onStop() {
            if (this.mNotificationStart) {
                VSlog.d(VivoColorManagerService.TAG, "onStop");
                if (this.mBroadcastReceiver != null) {
                    VivoColorManagerService.this.mContext.unregisterReceiver(this.mBroadcastReceiver);
                    this.mBroadcastReceiver = null;
                }
                this.mTwilightManager.unregisterListener(this);
                this.mNotificationStart = false;
            }
        }

        public void onTwilightStateChanged(TwilightState state) {
            StringBuilder sb = new StringBuilder();
            sb.append("onTwilightStateChanged: isNight=");
            sb.append(state == null ? null : Boolean.valueOf(state.isNight()));
            VSlog.d(VivoColorManagerService.TAG, sb.toString());
            updateNotification(state);
        }

        private PendingIntent pendingBroadcast(String action) {
            return PendingIntent.getBroadcastAsUser(VivoColorManagerService.this.mContext, 0, new Intent(action).setPackage(VivoColorManagerService.this.mContext.getPackageName()).setFlags(268435456), 0, UserHandle.CURRENT);
        }

        private void sendNotification() {
            Bundle bundle = new Bundle();
            bundle.putInt("vivo.summaryIconRes", 50463065);
            bundle.putBoolean("custom_force_show_notification_on_keyguard", true);
            Notification.Builder builder = new Notification.Builder(VivoColorManagerService.this.mContext, CHANNEL_ID).setSmallIcon(50463064).setContentText(this.mResources.getString(51249839)).setStyle(new Notification.BigTextStyle()).setAutoCancel(true).setDeleteIntent(pendingBroadcast(VIVO_ACTION_NOTIFICATION_DELETED)).setContentIntent(pendingBroadcast(VIVO_ACTION_NOTIFICATION_CLICK)).setExtras(bundle).setVisibility(1);
            if (("vos".equals(FtBuild.getOsName()) && this.mResources.getConfiguration().locale.getCountry().equals("CN")) || FtBuild.getRomVersion() >= 12.0f) {
                bundle.putString("android.substName", this.mResources.getString(51249841));
            } else {
                bundle.putString("android.substName", this.mResources.getString(51249840));
            }
            Notification notification = builder.build();
            this.mNotificationManager.notify(1, notification);
            this.mNotificationState = 1;
            int i = this.mNotificationCount - 1;
            this.mNotificationCount = i;
            setNotificationCount(i);
            VSlog.d(VivoColorManagerService.TAG, "sendNotification: notification send count= " + (3 - this.mNotificationCount));
        }

        public void removeNotification() {
            if (this.mNotificationState == 1) {
                VSlog.d(VivoColorManagerService.TAG, "removeNotification");
                this.mNotificationManager.cancel(1);
                this.mNotificationState = 3;
                if (this.mNotificationCount == 0) {
                    onStop();
                }
            }
        }

        public boolean isNotificationPending() {
            return this.mNotificationState == 2;
        }

        public boolean isNotificationEnable() {
            int notificationCount = getNotificationCount();
            StringBuilder sb = new StringBuilder();
            sb.append("isNotificationEnable:");
            sb.append(notificationCount > 0);
            sb.append(" notificationCount=");
            sb.append(notificationCount);
            VSlog.d(VivoColorManagerService.TAG, sb.toString());
            return notificationCount > 0;
        }

        public void setNotificationCount(int count) {
            SystemProperties.set(VIVO_NOTIFICATION_COUNT, Integer.toString(count));
        }

        public int getNotificationCount() {
            return SystemProperties.getInt(VIVO_NOTIFICATION_COUNT, 3);
        }

        private void registerNotificationChannel() {
            NotificationChannel notificationChannel = this.mNotificationManager.getNotificationChannel(CHANNEL_ID);
            if (notificationChannel == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, 4);
                channel.enableLights(false);
                this.mNotificationManager.createNotificationChannel(channel);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setEyeProtectionSetting(int temperature) {
        boolean success = Settings.Secure.putIntForUser(this.mContext.getContentResolver(), VIVO_NIGHT_DISPLAY_EYECARE, temperature, -2);
        return success;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getEyeProtectionSetting() {
        int temperature = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), VIVO_NIGHT_DISPLAY_EYECARE, -1, -2);
        return temperature;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setColorTemperatureSetting(int temperature) {
        boolean success = Settings.Secure.putIntForUser(this.mContext.getContentResolver(), VIVO_NIGHT_DISPLAY_COLOR_TEMPERATURE, temperature, -2);
        return success;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getColorTemperatureSetting() {
        int temperature = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), VIVO_NIGHT_DISPLAY_COLOR_TEMPERATURE, -1, -2);
        return temperature;
    }

    public boolean setUserColorModeSetting(int colorMode) {
        boolean success = Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_DISPLAY_COLOR_MODE_SETTING, colorMode, -2);
        return success;
    }

    public int getUserColorModeSetting() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_DISPLAY_COLOR_MODE_SETTING, -1, -2);
    }

    public int getActualColorModeSetting() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_DISPLAY_COLOR_MODE, -1, -2);
    }

    public boolean setActualColorModeSetting(int colorMode) {
        boolean success = Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_DISPLAY_COLOR_MODE, colorMode, -2);
        return success;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setColorModeInternal(int colorMode) {
        this.mColorMode = colorMode;
        setActualColorModeSetting(colorMode);
        onVivoDisplayColorModeChanged(colorMode);
        return true;
    }

    private void setColorModeNative(int colorMode) {
        IBinder flinger = ServiceManager.getService(SURFACE_FLINGER);
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(colorMode);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_SET_COLOR_MODE, data, null, 0);
                } catch (RemoteException e) {
                    VSlog.e(TAG, "Failed to setColorModeNative");
                }
            } finally {
                data.recycle();
            }
        }
    }

    private int getColorModeNative() {
        IBinder flinger = ServiceManager.getService(SURFACE_FLINGER);
        int colorMode = -1;
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_GET_COLOR_MODE, data, reply, 0);
                    colorMode = reply.readInt();
                } catch (RemoteException e) {
                    VSlog.e(TAG, "Failed to setColorModeNative");
                }
            } finally {
                data.recycle();
            }
        }
        return colorMode;
    }

    /* loaded from: classes.dex */
    final class BinderService extends IVivoColorManager.Stub {
        public static final String serviceName = "vivo_color_manager_service";

        BinderService() {
        }

        public boolean setEyeProtection(int temperature) {
            long token = Binder.clearCallingIdentity();
            try {
                VivoColorManagerService.this.mColorEyePro = temperature;
                VivoColorManagerService.this.onEyeProtectionChanged(temperature);
                return VivoColorManagerService.this.setEyeProtectionSetting(temperature);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getEyeProtection() {
            long token = Binder.clearCallingIdentity();
            try {
                return VivoColorManagerService.this.getEyeProtectionSetting();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setColorTemperature(int temperature) {
            long token = Binder.clearCallingIdentity();
            try {
                VivoColorManagerService.this.mColorTemp = temperature;
                VivoColorManagerService.this.onColorTemperatureChanged(temperature);
                return VivoColorManagerService.this.setColorTemperatureSetting(temperature);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getColorTemperature() {
            long token = Binder.clearCallingIdentity();
            try {
                return VivoColorManagerService.this.getColorTemperatureSetting();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setEyeProtectionPreview(int temperature, boolean previewing) {
            long token = Binder.clearCallingIdentity();
            try {
                if (!previewing) {
                    VivoColorManagerService.this.onNightDisplayChangedAnimation(VivoColorManagerService.this.getColorTemperature());
                    return true;
                }
                return setEyeProtection(temperature);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setEyeProtectionEnabled(boolean enable) {
            long token = Binder.clearCallingIdentity();
            try {
                VivoColorManagerService.this.mEyeProEnable = enable;
                if (!enable) {
                    VivoColorManagerService.this.onColorTemperatureChanged(KernelConfig.FRC_LOW_LATENCY);
                } else {
                    VivoColorManagerService.this.onNightDisplayChangedImmediate(VivoColorManagerService.this.getColorTemperature());
                }
                return true;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setColorMode(int colorMode, int from) {
            long token = Binder.clearCallingIdentity();
            int targetMode = 0;
            try {
                int settingMode = VivoColorManagerService.this.getUserColorModeSetting();
                if (from == 0) {
                    VivoColorManagerService.this.setUserColorModeSetting(colorMode);
                    targetMode = colorMode;
                } else if (from == 1) {
                    if (colorMode == 4096) {
                        if (!VivoColorManagerService.mSupportColorManager && !VivoColorManagerService.mSupportSuperLcd) {
                            if (settingMode != 0) {
                                targetMode = 0;
                            }
                        }
                        targetMode = VivoColorManagerService.VIVO_COLOR_MODE_FINGERPRINT;
                    } else if (colorMode == 8192) {
                        targetMode = settingMode;
                    }
                }
                VSlog.d(VivoColorManagerService.TAG, "setColorMode: settingMode=" + settingMode + ", expectedMode=" + colorMode + ", targetMode=" + targetMode);
                return VivoColorManagerService.this.setColorModeInternal(targetMode);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getColorMode() {
            long token = Binder.clearCallingIdentity();
            try {
                return VivoColorManagerService.this.getUserColorModeSetting();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setDisplayColor(int color, int compositionColorMode) {
            long token = Binder.clearCallingIdentity();
            try {
                if (VivoColorManagerService.this.mDtm != null) {
                    VivoColorManagerService.this.mDtm.setDisplayColor(color, compositionColorMode);
                }
                return true;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }
}