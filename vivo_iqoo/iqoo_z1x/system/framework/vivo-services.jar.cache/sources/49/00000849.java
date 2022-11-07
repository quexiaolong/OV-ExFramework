package com.vivo.services.superresolution;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.FtDeviceInfo;
import android.view.Display;
import com.android.server.am.frozen.FrozenQuicker;
import com.vivo.face.common.data.Constants;
import com.vivo.services.configurationManager.DecryptUtils;
import com.vivo.services.proxy.game.GameSceneProxyManager;
import java.io.FileDescriptor;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONObject;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.StringList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class SuperResolutionManagerServiceHelper {
    private static final String TAG = "SuperResolutionManagerService";
    private Timer accuTimer;
    private ReportAccuTimeTask accuTimerTask;
    private UpdateAlgorithmTimeTask algorithmTimeTask;
    private Timer algorithmTimer;
    private AbsConfigurationManager mConfigurationManager;
    private ContentResolver mContentResolver;
    private Context mContext;
    private StringList mMaxTemperature;
    private SuperResolutionManagerService mService;
    private StringList mSupportApps;
    private ReportTask mTask;
    private Timer mTimer;
    private VivoFrameworkFactory mVivoFrameworkFactory;
    private boolean mIsOpenAppShare = false;
    private final Object mAppSwitchLock = new Object();
    private Map<String, Integer> mAppSwitch = Constant.sAppSwitch;
    List<String> mInstalledPackages = Constant.sInstalledPackages;
    private String mImei = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private boolean isMainSwitchOpen = false;
    private boolean isHighTempure = false;
    private int isOpenHighTempureForbid = 1;
    private Handler mSRHandler;
    private final ContentObserver mMainSettingsObserver = new ContentObserver(this.mSRHandler) { // from class: com.vivo.services.superresolution.SuperResolutionManagerServiceHelper.1
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            try {
                int switchMain = Settings.Global.getInt(SuperResolutionManagerServiceHelper.this.mContentResolver, Constant.SETTING_VALUE_MAIN);
                VSlog.i("SuperResolutionManagerService", "switchMain = " + switchMain);
                SuperResolutionManagerServiceHelper.this.isMainSwitchOpen = switchMain > 0;
                SuperResolutionManagerServiceHelper.this.doMainSwitchChange();
                SuperResolutionManagerServiceHelper.this.notifyNativeSwitchChange(switchMain, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, 0);
            } catch (Exception e) {
                VSlog.e("SuperResolutionManagerService", "onChange:  SettingNotFoundException : " + e.getMessage() + " need set value");
                boolean set = Settings.Global.putInt(SuperResolutionManagerServiceHelper.this.mContentResolver, Constant.SETTING_VALUE_MAIN, 0);
                SuperResolutionManagerServiceHelper.this.notifyNativeSwitchChange(0, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, 0);
                StringBuilder sb = new StringBuilder();
                sb.append("set setting value super_resolution_main ");
                sb.append(set ? "success" : "fail");
                VSlog.d("SuperResolutionManagerService", sb.toString());
            }
        }
    };
    private final ContentObserver mTemperatureObserver = new ContentObserver(this.mSRHandler) { // from class: com.vivo.services.superresolution.SuperResolutionManagerServiceHelper.2
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            try {
                int temperature = Settings.Global.getInt(SuperResolutionManagerServiceHelper.this.mContentResolver, Constant.CUSTOM_TEMPURE);
                Constant.TEMPERATURE_MAX = temperature;
                VSlog.d("SuperResolutionManagerService", "onChange: modify TEMPERATURE_MAX=" + temperature);
                SuperResolutionManagerServiceHelper.this.checkTemperature();
            } catch (Exception e) {
                VSlog.d("SuperResolutionManagerService", "onChange: set temperature success");
            }
        }
    };
    private boolean isFirst = true;
    private final ContentObserver mAppsSettingsObserver = new ContentObserver(this.mSRHandler) { // from class: com.vivo.services.superresolution.SuperResolutionManagerServiceHelper.3
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            try {
                String switchApps = Settings.Global.getString(SuperResolutionManagerServiceHelper.this.mContentResolver, Constant.SETTING_VALUE_APPS);
                if (SuperResolutionManagerServiceHelper.this.isFirst) {
                    SuperResolutionManagerServiceHelper.this.doAppSwitchChange(switchApps);
                    SuperResolutionManagerServiceHelper.this.isFirst = false;
                }
                int switchMain = Settings.Global.getInt(SuperResolutionManagerServiceHelper.this.mContentResolver, Constant.SETTING_VALUE_MAIN, -1);
                if (switchMain == 1) {
                    if (switchApps != null) {
                        SuperResolutionManagerServiceHelper.this.doAppSwitchChange(switchApps);
                        SuperResolutionManagerServiceHelper.this.notifyNativeSwitchChange(0, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, 1);
                        return;
                    }
                    SuperResolutionManagerServiceHelper.this.updateSettingAppSwitches();
                    SuperResolutionManagerServiceHelper.this.notifyNativeSwitchChange(0, null, 1);
                }
            } catch (Exception e) {
                VSlog.e("SuperResolutionManagerService", "onChange: mAppsSettingsObserver exception=" + e.getMessage());
            }
        }
    };
    private final ContentObserver mLowPowerObserver = new ContentObserver(this.mSRHandler) { // from class: com.vivo.services.superresolution.SuperResolutionManagerServiceHelper.4
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            try {
                int lowPowerLevel = Settings.System.getInt(SuperResolutionManagerServiceHelper.this.mContentResolver, "power_save_type");
                VSlog.d("SuperResolutionManagerService", "onChange:  lowPowerLevel = " + lowPowerLevel);
                SuperResolutionManagerServiceHelper.this.mService.setSuperResolutionStopState(3, lowPowerLevel == 2);
            } catch (Exception e) {
                VSlog.e("SuperResolutionManagerService", "onChange: mLowPowerObserver SettingNotFoundException:" + e.getMessage());
            }
        }
    };
    private final ContentObserver mHighTemperatureObserver = new ContentObserver(this.mSRHandler) { // from class: com.vivo.services.superresolution.SuperResolutionManagerServiceHelper.5
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            try {
                SuperResolutionManagerServiceHelper.this.isOpenHighTempureForbid = Settings.Global.getInt(SuperResolutionManagerServiceHelper.this.mContentResolver, Constant.OPEN_HIGH_TEMPURE_FORBID);
                if (SuperResolutionManagerServiceHelper.this.isOpenHighTempureForbid == 0 && SuperResolutionManagerServiceHelper.this.isHighTempure) {
                    SuperResolutionManagerServiceHelper.this.mService.setSuperResolutionStopState(1, false);
                }
                VSlog.d("SuperResolutionManagerService", "onChange:  open_high_tempure_forbid = " + SuperResolutionManagerServiceHelper.this.isOpenHighTempureForbid);
            } catch (Exception e) {
                VSlog.e("SuperResolutionManagerService", "onChange:  SettingNotFoundException : " + e.getMessage());
            }
        }
    };
    private ConfigurationObserver mSupportAppConfigObserver = new ConfigurationObserver() { // from class: com.vivo.services.superresolution.SuperResolutionManagerServiceHelper.6
        public void onConfigChange(String file, String name) {
            String[] strArr;
            VSlog.d("SuperResolutionManagerService", "getUnifiedConfig: support_apps change file = " + file + "   name = " + name);
            if (SuperResolutionManagerServiceHelper.this.mConfigurationManager == null) {
                VSlog.e("SuperResolutionManagerService", "getUnifiedConfig: mConfigurationManager is null");
                return;
            }
            SuperResolutionManagerServiceHelper superResolutionManagerServiceHelper = SuperResolutionManagerServiceHelper.this;
            superResolutionManagerServiceHelper.mSupportApps = superResolutionManagerServiceHelper.mConfigurationManager.getStringList(Constant.SR_CONFIG_FILE_PATH, Constant.SR_CONFIG_SUPPORT_APPS);
            VSlog.d("SuperResolutionManagerService", "getUnifiedConfig: support_apps change file = " + file + "   name = " + name);
            if (SuperResolutionManagerServiceHelper.this.mSupportApps != null && SuperResolutionManagerServiceHelper.this.mSupportApps.getValues() != null && SuperResolutionManagerServiceHelper.this.mSupportApps.getValues().size() > 0) {
                Constant.SUPPORT_APP = (String[]) SuperResolutionManagerServiceHelper.this.mSupportApps.getValues().toArray(new String[0]);
                VSlog.d("SuperResolutionManagerService", "getUnifiedConfig: support_apps = " + Arrays.toString(Constant.SUPPORT_APP));
                String[] configSupportApps = (String[]) SuperResolutionManagerServiceHelper.this.mSupportApps.getValues().toArray(new String[0]);
                if (configSupportApps.length > 0) {
                    Constant.SUPPORT_APP = configSupportApps;
                } else {
                    VSlog.d("SuperResolutionManagerService", "getUnifiedConfig: configSupportApps has no app");
                    Constant.SUPPORT_APP = Constant.DEFAULT_SUPPORT_APP;
                }
            }
            synchronized (SuperResolutionManagerServiceHelper.this.mAppSwitchLock) {
                for (String appName : Constant.DEFAULT_SUPPORT_APP) {
                    if (Arrays.asList(Constant.SUPPORT_APP).contains(appName)) {
                        SuperResolutionManagerServiceHelper.this.mAppSwitch.put(appName, 1);
                    } else {
                        SuperResolutionManagerServiceHelper.this.mAppSwitch.put(appName, 0);
                    }
                }
            }
            SuperResolutionManagerServiceHelper.this.updateSettingAppSwitches();
        }
    };
    private ConfigurationObserver mTemperatureConfigObserver = new ConfigurationObserver() { // from class: com.vivo.services.superresolution.SuperResolutionManagerServiceHelper.7
        public void onConfigChange(String file, String name) {
            VSlog.d("SuperResolutionManagerService", "getUnifiedConfig: TEMPERATURE_MAX change file = " + file + "   name = " + name);
            if (SuperResolutionManagerServiceHelper.this.mConfigurationManager == null) {
                VSlog.e("SuperResolutionManagerService", "getUnifiedConfig: mConfigurationManager is null");
                return;
            }
            SuperResolutionManagerServiceHelper superResolutionManagerServiceHelper = SuperResolutionManagerServiceHelper.this;
            superResolutionManagerServiceHelper.mMaxTemperature = superResolutionManagerServiceHelper.mConfigurationManager.getStringList(Constant.SR_CONFIG_FILE_PATH, Constant.SR_CONFIG_MAX_TEMPERATURE);
            if (SuperResolutionManagerServiceHelper.this.mMaxTemperature != null && SuperResolutionManagerServiceHelper.this.mMaxTemperature.getValues() != null && SuperResolutionManagerServiceHelper.this.mMaxTemperature.getValues().size() > 0) {
                VSlog.d("SuperResolutionManagerService", "getUnifiedConfig: mMaxTemperature = " + SuperResolutionManagerServiceHelper.this.mMaxTemperature.toString());
                Constant.TEMPERATURE_MAX = Integer.parseInt((String) SuperResolutionManagerServiceHelper.this.mMaxTemperature.getValues().get(0));
                VSlog.d("SuperResolutionManagerService", "getUnifiedConfig: update TEMPERATURE_MAX = " + Constant.TEMPERATURE_MAX);
                SuperResolutionManagerServiceHelper.this.checkTemperature();
            }
        }
    };
    private DisplayManager mDisplayManager = null;
    private int mDisplayState = 2;
    private IBinder mNativeService = null;
    private PowerManager.WakeLock mPartialWakeLock = null;
    private PowerManager mPowerManager = null;
    private final Runnable mReleaser = new Runnable() { // from class: com.vivo.services.superresolution.SuperResolutionManagerServiceHelper.8
        @Override // java.lang.Runnable
        public void run() {
            SuperResolutionManagerServiceHelper.this.mNativeService = null;
        }
    };
    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() { // from class: com.vivo.services.superresolution.SuperResolutionManagerServiceHelper.9
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            VSlog.e("SuperResolutionManagerService", "binderDied: super resolution native died.");
            SuperResolutionManagerServiceHelper.this.mSRHandler.post(SuperResolutionManagerServiceHelper.this.mReleaser);
        }
    };
    private final Runnable mClearHandlesRunnable = new Runnable() { // from class: com.vivo.services.superresolution.SuperResolutionManagerServiceHelper.11
        @Override // java.lang.Runnable
        public void run() {
            SuperResolutionManagerServiceHelper.this.clearAllHandlesIfNeeded();
        }
    };
    private Runnable mCheckTempRunnable = new Runnable() { // from class: com.vivo.services.superresolution.SuperResolutionManagerServiceHelper.12
        @Override // java.lang.Runnable
        public void run() {
            VSlog.d("SuperResolutionManagerService", "mCheckTempRunnable: run: ");
            SuperResolutionManagerServiceHelper.this.checkTemperature();
            SuperResolutionManagerServiceHelper.this.mSRHandler.postDelayed(SuperResolutionManagerServiceHelper.this.mCheckTempRunnable, Constant.CHECK_TEMPERATURE_TIME);
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyNativeSwitchChange(int value, String activityName, int mode) {
        String[] strArr;
        VSlog.d("SuperResolutionManagerService", "notifyNativeSwitchChange: mode = " + mode);
        if (mode == 3) {
            if (value == 1) {
                VSlog.d("SuperResolutionManagerService", "notifyNativeSwitchChange app share start, main switch set to 0");
                SystemProperties.set(Constant.SYSTEM_SUPER_RESOLUTION_SWITCH, "0");
            } else if (value == 0) {
                VSlog.d("SuperResolutionManagerService", "notifyNativeSwitchChange app share end, main switch set to 0");
                notifyNativeSwitchChange(0, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, 1);
            }
        }
        if (this.mIsOpenAppShare) {
            VSlog.d("SuperResolutionManagerService", "notifyNativeSwitchChange app share is running, main switch set to 0");
            return;
        }
        String nativeSwitch = SystemProperties.get(Constant.SYSTEM_SUPER_RESOLUTION_SWITCH);
        if (mode == 0 && value == 0) {
            SystemProperties.set(Constant.SYSTEM_SUPER_RESOLUTION_SWITCH, "0");
            VSlog.d("SuperResolutionManagerService", "notifyNativeSwitchChange switch = 0");
        } else if ((mode == 0 && value == 1) || mode == 1) {
            int wechatVideoCallSwitch = -1;
            int wechatShareVideoSwitch = -1;
            if (!TextUtils.isEmpty(nativeSwitch)) {
                int mainSwitch = Integer.parseInt(nativeSwitch);
                wechatVideoCallSwitch = mainSwitch & Constant.SWITCH_WECHAT_VIDEO_CALL;
                wechatShareVideoSwitch = mainSwitch & Constant.SWITCH_WECHAT_SHARE_VIDEO;
            }
            int appSwitch = 1;
            int mainSwitch2 = 0;
            synchronized (this.mAppSwitchLock) {
                for (String app : Constant.THIRD_PARTY_APP) {
                    if (this.mAppSwitch.get(app) != null && this.mAppSwitch.get(app).intValue() == 1) {
                        mainSwitch2 = openSwitch(mainSwitch2, appSwitch);
                    }
                    appSwitch <<= 1;
                }
                if (wechatShareVideoSwitch != -1 && wechatVideoCallSwitch != -1) {
                    mainSwitch2 = mainSwitch2 | wechatVideoCallSwitch | wechatShareVideoSwitch;
                }
                if (this.mAppSwitch.get(Constant.APP_WEIXIN) != null && this.mAppSwitch.get(Constant.APP_WEIXIN).intValue() == 1 && mode == 1) {
                    VSlog.d("SuperResolutionManagerService", "notifyNativeSwitchChange value = " + value + "  activity=" + activityName);
                    StringBuilder sb = new StringBuilder();
                    sb.append("notifyNativeSwitchChange before mainSwitch = ");
                    sb.append(intToBin32(mainSwitch2));
                    VSlog.d("SuperResolutionManagerService", sb.toString());
                    if (Constant.ACTIVITY_WEIXIN_VIDEO.equals(activityName)) {
                        if (value == 1) {
                            mainSwitch2 = closeSwitch(mainSwitch2, Constant.SWITCH_WECHAT_VIDEO_CALL);
                        } else if (value == 0) {
                            mainSwitch2 = openSwitch(mainSwitch2, Constant.SWITCH_WECHAT_VIDEO_CALL);
                        }
                        VSlog.d("SuperResolutionManagerService", "notifyNativeSwitchChange WECHAT_VIDEO_CALL value = " + value + "  mainSwitch=" + intToBin32(mainSwitch2));
                    }
                    if (Constant.ACTIVITY_WEIXIN_NO_SR.equals(activityName)) {
                        if (value == 1) {
                            mainSwitch2 = closeSwitch(mainSwitch2, Constant.SWITCH_APP_WECHAT);
                        } else if (value == 0) {
                            mainSwitch2 = openSwitch(mainSwitch2, Constant.SWITCH_APP_WECHAT);
                        }
                        VSlog.d("SuperResolutionManagerService", "notifyNativeSwitchChange ACTIVITY_WEIXIN_NO_SR value = " + value + "  mainSwitch=" + intToBin32(mainSwitch2));
                    }
                }
            }
            int mainSwitch3 = closeSwitch(mainSwitch2, Constant.SWITCH_APP_WECHAT);
            VSlog.d("SuperResolutionManagerService", "notifyNativeSwitchChange 屏蔽微信 mainSwitch=" + intToBin32(mainSwitch3));
            if (this.isMainSwitchOpen) {
                VSlog.d("SuperResolutionManagerService", "notifyNativeSwitchChange final mainSwitch = " + intToBin32(mainSwitch3));
                SystemProperties.set(Constant.SYSTEM_SUPER_RESOLUTION_SWITCH, String.valueOf(mainSwitch3));
                return;
            }
            VSlog.d("SuperResolutionManagerService", "mainswitch is close, notifyNativeSwitchChange mainSwitch = 0");
            SystemProperties.set(Constant.SYSTEM_SUPER_RESOLUTION_SWITCH, "0");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initHelper(SuperResolutionManagerService service, Context context) {
        this.mService = service;
        this.mContext = context;
        this.mSRHandler = service.getSRHandler();
        registerBroadcastReceiver();
        registerObserver();
        settingsInit();
        registerDisplayListener();
        this.mSRHandler.sendEmptyMessageDelayed(14, 1000L);
        registerUpdateAlgorithmTimer();
        registerReportAccuTimer();
    }

    private void registerBroadcastReceiver() {
        InstallApkReceiver installReceiver = new InstallApkReceiver();
        IntentFilter installIntentFilter = new IntentFilter();
        installIntentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        installIntentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        installIntentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        installIntentFilter.addDataScheme("package");
        this.mContext.registerReceiver(installReceiver, installIntentFilter, null, this.mSRHandler);
        TimeChangedReceiver timeChangedReceiver = new TimeChangedReceiver();
        IntentFilter timeChangedIntentFilter = new IntentFilter();
        timeChangedIntentFilter.addAction("android.intent.action.TIME_SET");
        this.mContext.registerReceiver(timeChangedReceiver, timeChangedIntentFilter, null, this.mSRHandler);
        VSlog.d("SuperResolutionManagerService", "registerReceiver: success");
    }

    private void registerObserver() {
        this.mContentResolver = this.mContext.getContentResolver();
        this.mSRHandler.post(new Runnable() { // from class: com.vivo.services.superresolution.-$$Lambda$SuperResolutionManagerServiceHelper$NSxMFGap6yvfZB01szOGteZgYPo
            @Override // java.lang.Runnable
            public final void run() {
                SuperResolutionManagerServiceHelper.this.lambda$registerObserver$0$SuperResolutionManagerServiceHelper();
            }
        });
    }

    public /* synthetic */ void lambda$registerObserver$0$SuperResolutionManagerServiceHelper() {
        this.mContentResolver.registerContentObserver(Settings.Global.getUriFor(Constant.SETTING_VALUE_MAIN), false, this.mMainSettingsObserver, 0);
        this.mContentResolver.registerContentObserver(Settings.Global.getUriFor(Constant.SETTING_VALUE_APPS), false, this.mAppsSettingsObserver, 0);
        this.mContentResolver.registerContentObserver(Settings.System.getUriFor("power_save_type"), false, this.mLowPowerObserver, -1);
        this.mContentResolver.registerContentObserver(Settings.Global.getUriFor(Constant.OPEN_HIGH_TEMPURE_FORBID), false, this.mHighTemperatureObserver, -1);
        this.mContentResolver.registerContentObserver(Settings.Global.getUriFor(Constant.CUSTOM_TEMPURE), false, this.mTemperatureObserver, -1);
        VSlog.d("SuperResolutionManagerService", "init: registerContentObserver ");
        getUnifiedConfig();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getUnifiedConfig() {
        VSlog.d("SuperResolutionManagerService", "getUnifiedConfig: start");
        VivoFrameworkFactory frameworkFactoryImpl = VivoFrameworkFactory.getFrameworkFactoryImpl();
        this.mVivoFrameworkFactory = frameworkFactoryImpl;
        if (frameworkFactoryImpl != null) {
            this.mConfigurationManager = frameworkFactoryImpl.getConfigurationManager();
        } else {
            this.mSRHandler.sendEmptyMessageDelayed(11, 1000L);
        }
        if (this.mConfigurationManager == null) {
            VSlog.e("SuperResolutionManagerService", "getUnifiedConfig error : VivoFrameworkFactory  getFrameworkFactoryImpl is null");
            return;
        }
        boolean isSupport = DecryptUtils.isAbeSupportDecryptV2();
        VSlog.d("SuperResolutionManagerService", "getUnifiedConfig:  isAbeSupportDecryptV2=" + isSupport);
        StringList stringList = this.mConfigurationManager.getStringList(Constant.SR_CONFIG_FILE_PATH, Constant.SR_CONFIG_SUPPORT_APPS);
        this.mSupportApps = stringList;
        if (stringList != null && stringList.getValues() != null && this.mSupportApps.getValues().size() > 0) {
            VSlog.d("SuperResolutionManagerService", "getUnifiedConfig: support_apps = " + this.mSupportApps.toString());
            String[] configSupportApps = (String[]) this.mSupportApps.getValues().toArray(new String[0]);
            if (configSupportApps.length <= 0) {
                VSlog.d("SuperResolutionManagerService", "getUnifiedConfig: configSupportApps has no app");
                Constant.SUPPORT_APP = Constant.DEFAULT_SUPPORT_APP;
            } else {
                Constant.SUPPORT_APP = configSupportApps;
            }
            notifyNativeSwitchChange(0, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, 1);
        }
        StringList stringList2 = this.mConfigurationManager.getStringList(Constant.SR_CONFIG_FILE_PATH, Constant.SR_CONFIG_MAX_TEMPERATURE);
        this.mMaxTemperature = stringList2;
        if (stringList2 != null && stringList2.getValues() != null && this.mMaxTemperature.getValues().size() > 0) {
            VSlog.d("SuperResolutionManagerService", "getUnifiedConfig: mMaxTemperature = " + this.mMaxTemperature.toString());
            Constant.TEMPERATURE_MAX = Integer.parseInt((String) this.mMaxTemperature.getValues().get(0));
            VSlog.d("SuperResolutionManagerService", "getUnifiedConfig: update TEMPERATURE_MAX=" + Constant.TEMPERATURE_MAX);
        }
        this.mConfigurationManager.registerObserver(this.mSupportApps, this.mSupportAppConfigObserver);
        this.mConfigurationManager.registerObserver(this.mMaxTemperature, this.mTemperatureConfigObserver);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSettingAppSwitches() {
        try {
            synchronized (this.mAppSwitchLock) {
                JSONObject object = new JSONObject(this.mAppSwitch);
                VSlog.d("SuperResolutionManagerService", "updateSettingAppSwitches:  json = " + object.toString());
                Settings.Global.putString(this.mContentResolver, Constant.SETTING_VALUE_APPS, object.toString());
            }
        } catch (Exception e) {
            VSlog.e("SuperResolutionManagerService", "updateSettingAppSwitches error : " + e.getMessage());
        }
    }

    private void settingsInit() {
        VSlog.d("SuperResolutionManagerService", "init: switch init first ");
        this.mAppsSettingsObserver.onChange(false, null);
        this.mMainSettingsObserver.onChange(false, null);
        Settings.Global.putInt(this.mContentResolver, Constant.OPEN_HIGH_TEMPURE_FORBID, 1);
    }

    private void registerDisplayListener() {
        DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService(DisplayManager.class);
        this.mDisplayManager = displayManager;
        displayManager.registerDisplayListener(new DisplayManager.DisplayListener() { // from class: com.vivo.services.superresolution.SuperResolutionManagerServiceHelper.10
            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayAdded(int displayId) {
                VSlog.d("SuperResolutionManagerService", "onDisplayAdded, displayId = " + displayId);
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayRemoved(int displayId) {
                VSlog.d("SuperResolutionManagerService", "onDisplayRemoved, displayId = " + displayId);
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayChanged(int displayId) {
                VSlog.d("SuperResolutionManagerService", "onDisplayChanged, displayId = " + displayId);
                if (displayId == 0) {
                    Display display = SuperResolutionManagerServiceHelper.this.mDisplayManager.getDisplay(displayId);
                    int newState = display.getState();
                    VSlog.i("SuperResolutionManagerService", "onDisplayChanged, mDisplayState = " + Display.stateToString(SuperResolutionManagerServiceHelper.this.mDisplayState) + ", newState = " + Display.stateToString(newState));
                    if (SuperResolutionManagerServiceHelper.this.mDisplayState != 2 || newState != 1) {
                        if (SuperResolutionManagerServiceHelper.this.mDisplayState != 2 && newState == 2) {
                            SuperResolutionManagerServiceHelper.this.setIsScreenOn(true);
                        }
                    } else {
                        SuperResolutionManagerServiceHelper.this.setIsScreenOn(false);
                        SuperResolutionManagerServiceHelper.this.clearAllHandlesIfNeeded();
                    }
                    SuperResolutionManagerServiceHelper.this.mDisplayState = newState;
                }
            }
        }, this.mSRHandler);
    }

    private void ensureBinderAlive() {
        IBinder iBinder = this.mNativeService;
        if (iBinder == null || !iBinder.isBinderAlive()) {
            IBinder service = ServiceManager.getService("superreolution");
            this.mNativeService = service;
            if (service != null) {
                try {
                    service.linkToDeath(this.mDeathRecipient, 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        VSlog.i("SuperResolutionManagerService", "ensureBinderAlive: mNativeService = " + this.mNativeService);
    }

    private int clearAllHandles() {
        ensureBinderAlive();
        if (this.mNativeService != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                this.mNativeService.transact(7, data, reply, 0);
                int ret = reply.readInt();
                VSlog.i("SuperResolutionManagerService", "clearAllHandles: ret = " + ret);
                return ret;
            } catch (RemoteException e) {
                e.printStackTrace();
                return -1;
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int setIsScreenOn(boolean isScreenOn) {
        ensureBinderAlive();
        if (this.mNativeService != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeBoolean(isScreenOn);
                this.mNativeService.transact(8, data, reply, 0);
                int ret = reply.readInt();
                VSlog.i("SuperResolutionManagerService", "setIsScreenOn: isScreenOn = " + isScreenOn + ", ret = " + ret);
                return ret;
            } catch (RemoteException e) {
                e.printStackTrace();
                return -1;
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
        return -1;
    }

    private void acquireWakeLock() {
        PowerManager powerManager;
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        }
        if (this.mPartialWakeLock == null && (powerManager = this.mPowerManager) != null) {
            this.mPartialWakeLock = powerManager.newWakeLock(1, "SuperResolution");
        }
        PowerManager.WakeLock wakeLock = this.mPartialWakeLock;
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                this.mPartialWakeLock.release();
            }
            this.mPartialWakeLock.acquire(1500L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearAllHandlesIfNeeded() {
        boolean isOpen = Settings.Global.getInt(this.mContentResolver, Constant.SETTING_VALUE_MAIN, -1) == 1;
        VSlog.i("SuperResolutionManagerService", "clearAllHandlesIfNeeded, isOpen = " + isOpen);
        if (isOpen) {
            acquireWakeLock();
            clearAllHandles();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postClearAllHandlesRunnable() {
        this.mSRHandler.removeCallbacks(this.mClearHandlesRunnable);
        this.mSRHandler.post(this.mClearHandlesRunnable);
    }

    public boolean isOpenAppShare() {
        return this.mIsOpenAppShare;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doAppSwitchChange(String value) {
        this.mService.appSwitchChangeCallBack(value);
        VSlog.d("SuperResolutionManagerService", "doAppSwitchChange: value = " + value);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doMainSwitchChange() {
        this.mService.mainSwitchChangeCallBack();
        if (this.isMainSwitchOpen) {
            if (this.mSRHandler.hasCallbacks(this.mCheckTempRunnable)) {
                this.mSRHandler.removeCallbacks(this.mCheckTempRunnable);
            }
            this.mSRHandler.post(this.mCheckTempRunnable);
        } else if (!this.isHighTempure && this.mSRHandler.hasCallbacks(this.mCheckTempRunnable)) {
            this.mSRHandler.removeCallbacks(this.mCheckTempRunnable);
        }
        VSlog.d("SuperResolutionManagerService", "doMainSwitchChange: isMainSwitchOpen = " + this.isMainSwitchOpen);
    }

    public void initStopScene(int stopSceneType) {
        VSlog.d("SuperResolutionManagerService", "initStopScene: stopSceneType=" + stopSceneType);
        if (stopSceneType == 1 || stopSceneType == 4) {
            this.isHighTempure = true;
            this.isMainSwitchOpen = true;
            doMainSwitchChange();
            VSlog.d("SuperResolutionManagerService", "initStopScene: init high temp scene");
        }
    }

    public void checkTemperature() {
        if (this.isMainSwitchOpen || this.isHighTempure) {
            int temperature = FtDeviceInfo.getBoardTempure();
            VSlog.d("SuperResolutionManagerService", "checkTemperature: temperature=" + temperature + "  isHighTempure = " + this.isHighTempure + " TEMPERATURE_MAX = " + Constant.TEMPERATURE_MAX);
            if (temperature > Constant.TEMPERATURE_MAX && !this.isHighTempure) {
                VSlog.d("SuperResolutionManagerService", "checkTemperature: temperature is to high");
                this.isHighTempure = true;
                this.mService.setSuperResolutionStopState(1, true);
            } else if (temperature <= Constant.TEMPERATURE_MAX - 1 && this.isHighTempure) {
                VSlog.d("SuperResolutionManagerService", "checkTemperature: temperature is down");
                this.isHighTempure = false;
                this.mService.setSuperResolutionStopState(1, false);
            }
        }
    }

    public boolean checkIsHighTemperature() {
        int temperature = FtDeviceInfo.getBoardTempure();
        VSlog.d("SuperResolutionManagerService", "checkIsHighTemp: temperature=" + temperature + "  isHighTempure = " + this.isHighTempure + " TEMPERATURE_MAX = " + Constant.TEMPERATURE_MAX);
        if (temperature > Constant.TEMPERATURE_MAX) {
            VSlog.d("SuperResolutionManagerService", "checkIsHighTemp: temperature is to high");
            this.isHighTempure = true;
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void registerReportTimer() {
        if (this.mTimer != null) {
            return;
        }
        getDeviceId();
        if (TextUtils.isEmpty(this.mImei)) {
            this.mSRHandler.sendEmptyMessageDelayed(14, 2000L);
            return;
        }
        PackageManager pm = this.mContext.getPackageManager();
        if (this.mInstalledPackages.size() == 0) {
            List<PackageInfo> packageInfoList = pm.getInstalledPackages(1);
            for (PackageInfo info : packageInfoList) {
                this.mInstalledPackages.add(info.packageName);
            }
        }
        this.mTimer = new Timer();
        ReportTask reportTask = new ReportTask();
        this.mTask = reportTask;
        this.mTimer.schedule(reportTask, getReportDelayTime(), 604800000L);
        VSlog.d("SuperResolutionManagerService", "initReportTimer: success");
    }

    public void resetReportTimer() {
        Timer timer = this.mTimer;
        if (timer != null) {
            timer.cancel();
            this.mTimer = null;
            this.mTask.cancel();
            this.mTask = null;
            this.mTimer = new Timer();
            ReportTask reportTask = new ReportTask();
            this.mTask = reportTask;
            this.mTimer.schedule(reportTask, getReportDelayTime(), 604800000L);
            VSlog.d("SuperResolutionManagerService", "resetReportTimer");
        }
    }

    private long getReportDelayTime() {
        long cur = System.currentTimeMillis() % 604800000;
        long set = getMillis().longValue();
        long delay = cur < set ? set - cur : (604800000 + set) - cur;
        int day = (int) (delay / 86400000);
        int hour = (int) ((delay - (day * 86400000)) / 3600000);
        int minute = ((int) ((delay - (day * 86400000)) - (hour * GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME))) / FrozenQuicker.ONE_MIN;
        int second = (int) ((((delay - (86400000 * day)) - (GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME * hour)) - (FrozenQuicker.ONE_MIN * minute)) / 1000);
        VSlog.d("SuperResolutionManagerService", "cur->" + cur + ",set->" + set + ",delay->" + delay);
        VSlog.d("SuperResolutionManagerService", "getReportDelayTime: delay = " + day + "天" + hour + "小时" + minute + "分" + second + "秒");
        return delay;
    }

    private Long getMillis() {
        try {
            return Long.valueOf(Long.parseLong(this.mImei.substring(8, 14)) * 604);
        } catch (Exception e) {
            VSlog.e("SuperResolutionManagerService", "getMillis: string transform fail");
            return -1L;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ReportTask extends TimerTask {
        ReportTask() {
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            String[] strArr;
            Map<String, String> map = new HashMap<>();
            map.put("high-quality_sw", SuperResolutionManagerServiceHelper.this.isMainSwitchOpen ? "1" : "0");
            StringBuilder highBuilder = new StringBuilder();
            StringBuilder nonHighBuilder = new StringBuilder();
            for (String s : Constant.SUPPORT_APP) {
                if (SuperResolutionManagerServiceHelper.this.containsApp(s)) {
                    synchronized (SuperResolutionManagerServiceHelper.this.mAppSwitchLock) {
                        if (SuperResolutionManagerServiceHelper.this.mAppSwitch.get(s) != null && ((Integer) SuperResolutionManagerServiceHelper.this.mAppSwitch.get(s)).intValue() == 1) {
                            highBuilder.append(s);
                            highBuilder.append("|");
                        } else {
                            nonHighBuilder.append(s);
                            nonHighBuilder.append("|");
                        }
                    }
                }
            }
            String high = highBuilder.toString();
            String nonHigh = nonHighBuilder.toString();
            if (high.length() > 0) {
                high = high.substring(0, high.length() - 1);
            }
            if (nonHigh.length() > 0) {
                nonHigh = nonHigh.substring(0, nonHigh.length() - 1);
            }
            map.put("high_quality_app", high);
            map.put("nonhigh_quality_app", nonHigh);
            DataReport.reportState(map);
        }
    }

    protected void registerReportAccuTimer() {
        if (this.accuTimer != null) {
            return;
        }
        this.accuTimer = new Timer();
        ReportAccuTimeTask reportAccuTimeTask = new ReportAccuTimeTask();
        this.accuTimerTask = reportAccuTimeTask;
        this.accuTimer.schedule(reportAccuTimeTask, getReportAccuTimeDelayTime(), 86400000L);
        VSlog.d("SuperResolutionManagerService", "registerReportAccuTimer: success");
    }

    public void resetReportAccuTimer() {
        Timer timer = this.accuTimer;
        if (timer != null) {
            timer.cancel();
            this.accuTimer = null;
            this.accuTimerTask.cancel();
            this.accuTimerTask = null;
            this.accuTimer = new Timer();
            ReportAccuTimeTask reportAccuTimeTask = new ReportAccuTimeTask();
            this.accuTimerTask = reportAccuTimeTask;
            this.accuTimer.schedule(reportAccuTimeTask, getReportAccuTimeDelayTime(), 86400000L);
            VSlog.d("SuperResolutionManagerService", "resetReportAccuTimer");
        }
    }

    private long getReportAccuTimeDelayTime() {
        long cur = System.currentTimeMillis() % 86400000;
        long set = getSetTime();
        long delay = cur < set ? set - cur : (86400000 + set) - cur;
        int hour = (int) (delay / 3600000);
        int minute = ((int) (delay - (hour * GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME))) / FrozenQuicker.ONE_MIN;
        int second = (int) (((delay - (GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME * hour)) - (FrozenQuicker.ONE_MIN * minute)) / 1000);
        VSlog.d("SuperResolutionManagerService", "getReportAccuTimeDelayTime:cur->" + cur + ",set->" + set + ",delay->" + delay);
        VSlog.d("SuperResolutionManagerService", "getReportAccuTimeDelayTime: delay = " + hour + "小时" + minute + "分" + second + "秒");
        return delay;
    }

    private long getSetTime() {
        int seed = Settings.Global.getInt(this.mContentResolver, Constant.SETTING_VALUE_SEED, 0);
        if (seed == 0) {
            Random rand = new Random();
            seed = rand.nextInt(8999999) + 1000000;
            Settings.Global.putInt(this.mContentResolver, Constant.SETTING_VALUE_SEED, seed);
        }
        VSlog.d("SuperResolutionManagerService", "seed:" + seed);
        long setTime = (((long) seed) * 86400000) / 10000000;
        return setTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ReportAccuTimeTask extends TimerTask {
        ReportAccuTimeTask() {
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            Map<String, String> map = new HashMap<>();
            long accuTime = SystemProperties.getLong("persist.sys.sr.last_day_time", 0L);
            map.put("app_duration", "all_app:" + String.valueOf(accuTime));
            VSlog.d("SuperResolutionManagerService", "report accumulate time");
            DataReport.reportAccuTime(map);
        }
    }

    public void registerUpdateAlgorithmTimer() {
        if (this.algorithmTimer != null) {
            return;
        }
        this.algorithmTimer = new Timer();
        UpdateAlgorithmTimeTask updateAlgorithmTimeTask = new UpdateAlgorithmTimeTask();
        this.algorithmTimeTask = updateAlgorithmTimeTask;
        this.algorithmTimer.schedule(updateAlgorithmTimeTask, getDelayTime(), 86400000L);
        VSlog.d("SuperResolutionManagerService", "registerUpdateAlgorithmTimer: success");
    }

    public void resetUpdateAlgorithmTimer() {
        Timer timer = this.algorithmTimer;
        if (timer != null) {
            timer.cancel();
            this.algorithmTimer = null;
            this.algorithmTimeTask.cancel();
            this.algorithmTimeTask = null;
            this.algorithmTimer = new Timer();
            UpdateAlgorithmTimeTask updateAlgorithmTimeTask = new UpdateAlgorithmTimeTask();
            this.algorithmTimeTask = updateAlgorithmTimeTask;
            this.algorithmTimer.schedule(updateAlgorithmTimeTask, getDelayTime(), 86400000L);
            VSlog.d("SuperResolutionManagerService", "resetUpdateAlgorithmTimer");
        }
    }

    private long getDelayTime() {
        try {
            long now = System.currentTimeMillis();
            SimpleDateFormat sdfOne = new SimpleDateFormat("yyyy-MM-dd");
            long overTime = now - sdfOne.parse(sdfOne.format(Long.valueOf(now))).getTime();
            int passHour = (int) (overTime / 3600000);
            int passMinute = ((int) (overTime - (passHour * GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME))) / FrozenQuicker.ONE_MIN;
            int passSecond = (int) (((overTime - (passHour * GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME)) - (passMinute * FrozenQuicker.ONE_MIN)) / 1000);
            VSlog.d("SuperResolutionManagerService", "initTimer: pass = " + passHour + "小时" + passMinute + "分" + passSecond + "秒");
            long delayTime = 86400000 - overTime;
            StringBuilder sb = new StringBuilder();
            sb.append("initTimer: overTime = ");
            sb.append(overTime);
            sb.append(",delayTime=");
            sb.append(delayTime);
            VSlog.d("SuperResolutionManagerService", sb.toString());
            int hour = (int) (delayTime / 3600000);
            int minute = ((int) (delayTime - (hour * GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME))) / FrozenQuicker.ONE_MIN;
            int second = (int) (((delayTime - (GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME * hour)) - (FrozenQuicker.ONE_MIN * minute)) / 1000);
            VSlog.d("SuperResolutionManagerService", "initTimer: delay = " + hour + "小时" + minute + "分" + second + "秒");
            return delayTime;
        } catch (ParseException e) {
            VSlog.d("SuperResolutionManagerService", "ParseException: e->" + e);
            return 0L;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class UpdateAlgorithmTimeTask extends TimerTask {
        UpdateAlgorithmTimeTask() {
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            VSlog.d("SuperResolutionManagerService", "--------------UpdateAlgorithmTimeTask-----------------");
            long todayTime = SystemProperties.getLong("persist.sys.sr.today_time", 0L);
            SystemProperties.set("persist.sys.sr.last_day_time", String.valueOf(todayTime));
            SystemProperties.set("persist.sys.sr.today_time", "0");
        }
    }

    /* loaded from: classes.dex */
    public class TimeChangedReceiver extends BroadcastReceiver {
        public TimeChangedReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                VSlog.d("SuperResolutionManagerService", "onReceive: intent is null");
                return;
            }
            VSlog.d("SuperResolutionManagerService", "onReceive: " + intent.getAction());
            if ("android.intent.action.TIME_SET".equals(intent.getAction()) || "android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) {
                SuperResolutionManagerServiceHelper.this.mSRHandler.sendEmptyMessage(20);
            }
        }
    }

    private void getDeviceId() {
        try {
            if (!TextUtils.isEmpty(this.mImei)) {
                return;
            }
            TelephonyManager mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            this.mImei = mTelephonyManager.getImei();
        } catch (Exception e) {
            VSlog.e("SuperResolutionManagerService", "getDeviceId: exception = " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class InstallApkReceiver extends BroadcastReceiver {
        InstallApkReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            VSlog.e("SuperResolutionManagerService", "onReceive: action=" + intent.getAction() + "  packagename=" + intent.getDataString());
            if (intent.getDataString() == null || intent.getAction() == null) {
                return;
            }
            String packageName = intent.getDataString().split(":")[1];
            Message message = Message.obtain();
            message.obj = packageName;
            String action = intent.getAction();
            char c = 65535;
            int hashCode = action.hashCode();
            if (hashCode != -810471698) {
                if (hashCode != 525384130) {
                    if (hashCode == 1544582882 && action.equals("android.intent.action.PACKAGE_ADDED")) {
                        c = 0;
                    }
                } else if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                    c = 2;
                }
            } else if (action.equals("android.intent.action.PACKAGE_REPLACED")) {
                c = 1;
            }
            if (c != 0) {
                if (c == 1) {
                    VSlog.d("SuperResolutionManagerService", "onReceive: replace package = " + packageName);
                    return;
                } else if (c == 2) {
                    if (SuperResolutionManagerServiceHelper.this.mSRHandler.hasMessages(13)) {
                        SuperResolutionManagerServiceHelper.this.mSRHandler.removeMessages(13);
                    }
                    message.what = 13;
                    SuperResolutionManagerServiceHelper.this.mSRHandler.sendMessage(message);
                    return;
                } else {
                    return;
                }
            }
            VSlog.d("SuperResolutionManagerService", "onReceive: install package = " + packageName);
            if (SuperResolutionManagerServiceHelper.this.mInstalledPackages != null && !SuperResolutionManagerServiceHelper.this.mInstalledPackages.contains(packageName)) {
                SuperResolutionManagerServiceHelper.this.mInstalledPackages.add(packageName);
                if (SuperResolutionManagerServiceHelper.this.mSRHandler.hasMessages(12)) {
                    SuperResolutionManagerServiceHelper.this.mSRHandler.removeMessages(12);
                }
                message.what = 12;
                SuperResolutionManagerServiceHelper.this.mSRHandler.sendMessage(message);
            } else if (SuperResolutionManagerServiceHelper.this.mInstalledPackages != null && SuperResolutionManagerServiceHelper.this.mInstalledPackages.contains(packageName)) {
                VSlog.d("SuperResolutionManagerService", "onReceive: replace(update) package = " + packageName);
            }
        }
    }

    /* loaded from: classes.dex */
    class UnifiedConfigurationReceiver extends BroadcastReceiver {
        UnifiedConfigurationReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Bundle bundle;
            String[] identifiers;
            if (!Constant.SR_CONFIG_BROADCAST.equals(intent.getAction()) || (bundle = intent.getExtras()) == null || (identifiers = (String[]) bundle.get("identifiers")) == null || identifiers.length == 0) {
                return;
            }
            VSlog.d("SuperResolutionManagerService", "onReceive: UnifiedConfigurationReceiver identifiers=" + Arrays.toString(identifiers));
            if (Constant.SR_CONFIG_NAME.equals(identifiers[0])) {
                VSlog.d("SuperResolutionManagerService", "onReceive: UnifiedConfigurationReceiver identifiers=" + bundle);
                boolean isSupport = DecryptUtils.isAbeSupportDecryptV2();
                VSlog.d("SuperResolutionManagerService", "getUnifiedConfig:  isAbeSupportDecryptV2=" + isSupport);
                SuperResolutionManagerServiceHelper.this.mVivoFrameworkFactory = VivoFrameworkFactory.getFrameworkFactoryImpl();
                if (SuperResolutionManagerServiceHelper.this.mVivoFrameworkFactory != null) {
                    SuperResolutionManagerServiceHelper superResolutionManagerServiceHelper = SuperResolutionManagerServiceHelper.this;
                    superResolutionManagerServiceHelper.mConfigurationManager = superResolutionManagerServiceHelper.mVivoFrameworkFactory.getConfigurationManager();
                    SuperResolutionManagerServiceHelper superResolutionManagerServiceHelper2 = SuperResolutionManagerServiceHelper.this;
                    superResolutionManagerServiceHelper2.mSupportApps = superResolutionManagerServiceHelper2.mConfigurationManager.getStringList(Constant.SR_CONFIG_FILE_PATH, Constant.SR_CONFIG_SUPPORT_APPS);
                    VSlog.d("SuperResolutionManagerService", "onReceive: UnifiedConfigurationReceiver: mSupportApps = " + SuperResolutionManagerServiceHelper.this.mSupportApps.toString());
                    String[] array = (String[]) SuperResolutionManagerServiceHelper.this.mSupportApps.getValues().toArray(new String[0]);
                    VSlog.d("SuperResolutionManagerService", "onReceive: UnifiedConfigurationReceiver: SUPPORT_APP = " + Arrays.toString(array));
                    return;
                }
                VSlog.d("SuperResolutionManagerService", "onReceive: UnifiedConfigurationReceiver: mVivoFrameworkFactory is null");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean containsApp(String name) {
        VSlog.d("SuperResolutionManagerService", "containsApp:  mInstalledPackages size = " + this.mInstalledPackages.size());
        if (this.mInstalledPackages.size() == 0) {
            return false;
        }
        for (String info : this.mInstalledPackages) {
            if (info.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMainSwitchOpen() {
        return this.isMainSwitchOpen;
    }

    public int isOpenHighTempureForbid() {
        return this.isOpenHighTempureForbid;
    }

    public static String intToBin32(int i) {
        StringBuilder binaryStr = new StringBuilder(Integer.toBinaryString(i));
        while (binaryStr.length() < 32) {
            binaryStr.insert(0, "0");
        }
        return binaryStr.toString();
    }

    public static int openSwitch(int mainSwitch, int singleSwitch) {
        return mainSwitch | singleSwitch;
    }

    public static int closeSwitch(int mainSwitch, int singleSwitch) {
        return (~singleSwitch) & mainSwitch;
    }

    public int[] getOutputSize(int inWidth, int inHeight) {
        VSlog.d("SuperResolutionManagerService", "SuperResolutionService getOutputSize");
        ensureBinderAlive();
        if (this.mNativeService != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInt(inWidth);
                data.writeInt(inHeight);
                this.mNativeService.transact(2, data, reply, 0);
                int[] outSize = {reply.readInt(), reply.readInt()};
                int ret = reply.readInt();
                VSlog.i("SuperResolutionManagerService", "getOutputSize: ret = " + ret);
                return ret != 0 ? new int[0] : outSize;
            } catch (RemoteException e) {
                e.printStackTrace();
                return new int[0];
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
        return new int[0];
    }

    public long initSuperResolution(int FrameRate, int BitPerSecond, int[] inputSize, int[] inStride, int[] outStride, int format, String appInfo) {
        ensureBinderAlive();
        char[] appData = appInfo.toCharArray();
        VSlog.i("SuperResolutionManagerService", "initSuperResolution: appData = " + ((Object) appData) + ",appInfo = " + appInfo);
        if (this.mNativeService != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInt(FrameRate);
            } catch (RemoteException e) {
                e = e;
            } catch (Throwable th) {
                e = th;
            }
            try {
                data.writeInt(BitPerSecond);
            } catch (RemoteException e2) {
                e = e2;
                e.printStackTrace();
                reply.recycle();
                data.recycle();
                return -1L;
            } catch (Throwable th2) {
                e = th2;
                reply.recycle();
                data.recycle();
                throw e;
            }
            try {
                try {
                    data.writeInt(format);
                    data.writeInt(inputSize[0]);
                    data.writeInt(inputSize[1]);
                    data.writeInt(inStride[0]);
                    data.writeInt(inStride[1]);
                    data.writeInt(outStride[0]);
                    data.writeInt(outStride[1]);
                    this.mNativeService.transact(10, data, reply, 0);
                    long ret = reply.readLong();
                    VSlog.i("SuperResolutionManagerService", "initSuperResolution: ret = " + ret);
                    reply.recycle();
                    data.recycle();
                    return ret;
                } catch (Throwable th3) {
                    e = th3;
                    reply.recycle();
                    data.recycle();
                    throw e;
                }
            } catch (RemoteException e3) {
                e = e3;
                e.printStackTrace();
                reply.recycle();
                data.recycle();
                return -1L;
            }
        }
        return -1L;
    }

    public int runSuperResolution(long handler, FileDescriptor fd, long size, int dataPos, boolean isNewVideo) {
        ensureBinderAlive();
        if (this.mNativeService != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeLong(handler);
                data.writeFileDescriptor(fd);
                data.writeLong(size);
                data.writeInt(dataPos);
                data.writeBoolean(isNewVideo);
                this.mNativeService.transact(3, data, reply, 0);
                int ret = reply.readInt();
                VSlog.i("SuperResolutionManagerService", "runSuperResolution: ret = " + ret);
                return ret;
            } catch (RemoteException e) {
                e.printStackTrace();
                return -1;
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
        return -1;
    }

    public int releaseSuperResolution(long handler) {
        ensureBinderAlive();
        if (this.mNativeService != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeLong(handler);
                this.mNativeService.transact(4, data, reply, 0);
                int ret = reply.readInt();
                VSlog.i("SuperResolutionManagerService", "releaseSuperResolution: ret = " + ret);
                return ret;
            } catch (RemoteException e) {
                e.printStackTrace();
                return -1;
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
        return -1;
    }

    public int resizeBilinearCommon(FileDescriptor fd, long size, int imageFormat, int imageLocation, int inWidth, int inHeight, int inWidthStride, int inHeightStride, int outWidth, int outHeight, int outWidthStride, int outHeightStride) {
        ensureBinderAlive();
        if (this.mNativeService != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                try {
                    data.writeFileDescriptor(fd);
                } catch (RemoteException e) {
                    e = e;
                } catch (Throwable th) {
                    e = th;
                }
                try {
                    data.writeLong(size);
                } catch (RemoteException e2) {
                    e = e2;
                    e.printStackTrace();
                    reply.recycle();
                    data.recycle();
                    return -1;
                } catch (Throwable th2) {
                    e = th2;
                    reply.recycle();
                    data.recycle();
                    throw e;
                }
                try {
                    data.writeInt(imageFormat);
                } catch (RemoteException e3) {
                    e = e3;
                    e.printStackTrace();
                    reply.recycle();
                    data.recycle();
                    return -1;
                } catch (Throwable th3) {
                    e = th3;
                    reply.recycle();
                    data.recycle();
                    throw e;
                }
                try {
                    data.writeInt(imageLocation);
                } catch (RemoteException e4) {
                    e = e4;
                    e.printStackTrace();
                    reply.recycle();
                    data.recycle();
                    return -1;
                } catch (Throwable th4) {
                    e = th4;
                    reply.recycle();
                    data.recycle();
                    throw e;
                }
                try {
                    data.writeInt(inWidth);
                } catch (RemoteException e5) {
                    e = e5;
                    e.printStackTrace();
                    reply.recycle();
                    data.recycle();
                    return -1;
                } catch (Throwable th5) {
                    e = th5;
                    reply.recycle();
                    data.recycle();
                    throw e;
                }
                try {
                    data.writeInt(inHeight);
                } catch (RemoteException e6) {
                    e = e6;
                    e.printStackTrace();
                    reply.recycle();
                    data.recycle();
                    return -1;
                } catch (Throwable th6) {
                    e = th6;
                    reply.recycle();
                    data.recycle();
                    throw e;
                }
                try {
                    data.writeInt(inWidthStride);
                    try {
                        data.writeInt(inHeightStride);
                        try {
                            data.writeInt(outWidth);
                        } catch (RemoteException e7) {
                            e = e7;
                            e.printStackTrace();
                            reply.recycle();
                            data.recycle();
                            return -1;
                        } catch (Throwable th7) {
                            e = th7;
                            reply.recycle();
                            data.recycle();
                            throw e;
                        }
                    } catch (RemoteException e8) {
                        e = e8;
                        e.printStackTrace();
                        reply.recycle();
                        data.recycle();
                        return -1;
                    } catch (Throwable th8) {
                        e = th8;
                        reply.recycle();
                        data.recycle();
                        throw e;
                    }
                } catch (RemoteException e9) {
                    e = e9;
                    e.printStackTrace();
                    reply.recycle();
                    data.recycle();
                    return -1;
                } catch (Throwable th9) {
                    e = th9;
                    reply.recycle();
                    data.recycle();
                    throw e;
                }
                try {
                    data.writeInt(outHeight);
                    data.writeInt(outWidthStride);
                    data.writeInt(outHeightStride);
                    this.mNativeService.transact(5, data, reply, 0);
                    int ret = reply.readInt();
                    VSlog.i("SuperResolutionManagerService", "resizeBilinearCommon: ret = " + ret);
                    reply.recycle();
                    data.recycle();
                    return ret;
                } catch (RemoteException e10) {
                    e = e10;
                    e.printStackTrace();
                    reply.recycle();
                    data.recycle();
                    return -1;
                }
            } catch (Throwable th10) {
                e = th10;
                reply.recycle();
                data.recycle();
                throw e;
            }
        }
        return -1;
    }
}