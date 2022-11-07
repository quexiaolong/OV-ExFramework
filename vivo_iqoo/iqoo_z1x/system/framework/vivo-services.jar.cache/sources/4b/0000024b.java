package com.android.server.display.color.displayenhance;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import com.android.server.FgThread;
import com.android.server.display.color.VivoColorManagerService;
import com.android.server.display.color.VivoLtmController;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class Sdr2HdrSwDisplayEnhanceController extends DisplayEnhanceController implements DisplayEnhanceListener, PowerTemperatureListener {
    private static final String SDR2HDR_SOFTWARE_APPS_SWITCH_SETTING = "sdr2hdr_software_apps_switch_setting";
    private static final String SDR2HDR_SOFTWARE_MAIN_SWITCH_SETTING = "sdr2hdr_software_main_switch_setting";
    private static final String SDR2HDR_SOFTWARE_USER_SWITCH_SETTING = "sdr2hdr_software_user_switch_setting";
    static final String TAG = "Sdr2HdrSwDisplayEnhanceController";
    private ApplicationPackageObserver mAppObserver;
    private Context mContext;
    private Handler mHandler;
    private final HashMap<String, PackageSdr2HdrSwInfo> mPackageSdr2HdrSwMap;
    private VivoColorManagerService mVivoColorManager;
    private VivoLtmController mVivoLtmController;
    private static boolean DBG = SystemProperties.getBoolean("persist.vivo.display.enhance.debug", false);
    private static Sdr2HdrSwDisplayEnhanceController mSdr2HdrSwController = null;

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceController
    public /* bridge */ /* synthetic */ boolean checkLowPowerStatus(int i) {
        return super.checkLowPowerStatus(i);
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceController
    public /* bridge */ /* synthetic */ boolean checkTemperature(int i) {
        return super.checkTemperature(i);
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceController
    public /* bridge */ /* synthetic */ int getPackageSettingState(String str) {
        return super.getPackageSettingState(str);
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceController
    public /* bridge */ /* synthetic */ List getSupportAppList() {
        return super.getSupportAppList();
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceController
    public /* bridge */ /* synthetic */ void initAppSwitch(String str) {
        super.initAppSwitch(str);
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceController
    public /* bridge */ /* synthetic */ boolean isMainSwitchOpen() {
        return super.isMainSwitchOpen();
    }

    private Sdr2HdrSwDisplayEnhanceController(VivoColorManagerService colorManager, Context context) {
        super(context);
        this.mPackageSdr2HdrSwMap = new HashMap<>();
        this.mVivoLtmController = null;
        this.mAppObserver = null;
        this.mVivoColorManager = null;
        this.mContext = context;
        this.mVivoColorManager = colorManager;
        this.mHandler = new Handler(FgThread.get().getLooper());
        this.mVivoLtmController = VivoLtmController.getInstance(context);
        this.mAppObserver = ApplicationPackageObserver.getInstance(context);
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.color.displayenhance.Sdr2HdrSwDisplayEnhanceController.1
            @Override // java.lang.Runnable
            public void run() {
                Sdr2HdrSwDisplayEnhanceController.this.parserAppInfo();
            }
        });
        VivoDisplayEnhanceManagerService.registerPowerTempListener(this);
        this.isUserMainSwitchOpen = getUserSwitchSetting() == 1;
        this.isMainSwitchOpen = getMainSwitchSetting() == 1;
        if (this.isMainSwitchOpen) {
            VivoDisplayEnhanceManagerService.registerDisplayEnhanceListener(this);
        }
    }

    public static Sdr2HdrSwDisplayEnhanceController getInstance(VivoColorManagerService colorManager, Context context) {
        if (mSdr2HdrSwController == null) {
            synchronized (Sdr2HdrSwDisplayEnhanceController.class) {
                if (mSdr2HdrSwController == null) {
                    mSdr2HdrSwController = new Sdr2HdrSwDisplayEnhanceController(colorManager, context);
                }
            }
        }
        return mSdr2HdrSwController;
    }

    private void putMainSwitchSetting(int value) {
        VSlog.d(TAG, "putMainSwitchSetting: " + value);
        if (value == 1) {
            VivoDisplayEnhanceManagerService.registerDisplayEnhanceListener(this);
        } else {
            VivoDisplayEnhanceManagerService.unregisterDisplayEnhanceListener(this);
        }
        Settings.System.putIntForUser(this.mContext.getContentResolver(), SDR2HDR_SOFTWARE_MAIN_SWITCH_SETTING, value, -2);
    }

    private int getMainSwitchSetting() {
        int value = Settings.System.getIntForUser(this.mContext.getContentResolver(), SDR2HDR_SOFTWARE_MAIN_SWITCH_SETTING, 0, -2);
        return value;
    }

    private void putAppSwitchSetting() {
        try {
            JSONObject object = new JSONObject(this.mAppSwitch);
            VSlog.d(TAG, "putAppSwitchSetting:  json = " + object.toString());
            Settings.System.putStringForUser(this.mContext.getContentResolver(), SDR2HDR_SOFTWARE_APPS_SWITCH_SETTING, object.toString(), -2);
        } catch (Exception e) {
            VSlog.e(TAG, "putAppSwitchSetting: json exception ");
        }
    }

    private String getAppSwitchSetting() {
        return Settings.System.getStringForUser(this.mContext.getContentResolver(), SDR2HDR_SOFTWARE_APPS_SWITCH_SETTING, -2);
    }

    private void putUserSwitchSetting(int value) {
        VSlog.d(TAG, "putUserSwitchSetting: " + value);
        Settings.System.putIntForUser(this.mContext.getContentResolver(), SDR2HDR_SOFTWARE_USER_SWITCH_SETTING, value, -2);
    }

    public int getUserSwitchSetting() {
        int value = Settings.System.getIntForUser(this.mContext.getContentResolver(), SDR2HDR_SOFTWARE_USER_SWITCH_SETTING, 0, -2);
        return value;
    }

    private int getForegroundAppColorMode(String packageName) {
        PackageSdr2HdrSwInfo info = this.mPackageSdr2HdrSwMap.get(packageName);
        if (info == null || !info.colorModeConfig) {
            return -1;
        }
        int colorMode = info.colorMode;
        return colorMode;
    }

    private boolean isInForeground(String packageName) {
        PackageSdr2HdrSwInfo info = this.mPackageSdr2HdrSwMap.get(packageName);
        if (info != null) {
            return true;
        }
        return false;
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void onScreenStateChanged(int state) {
        boolean activated = state == VivoDisplayEnhanceManagerService.USER_PRESENT;
        int targetColorMode = -1;
        String packageName = this.mAppObserver.getForegroundAppPackageName();
        if (packageName == null || !isInForeground(packageName) || getPackageSettingState(packageName) != 1) {
            return;
        }
        this.mVivoLtmController.setLtmOn(activated, activated ? 2 : 0);
        if (activated) {
            targetColorMode = getForegroundAppColorMode(packageName);
            if (this.mVivoColorManager.mDtm != null && !this.mVivoColorManager.mDtm.isDisplayColorSupport(targetColorMode)) {
                return;
            }
        } else {
            int currentColorMode = this.mVivoColorManager.getActualColorModeSetting();
            if (currentColorMode == 509) {
                targetColorMode = currentColorMode;
            } else if (currentColorMode >= 256 && currentColorMode <= 511) {
                targetColorMode = this.mVivoColorManager.getUserColorModeSetting();
            }
        }
        VSlog.d(TAG, "onScreenStateChanged: activated=" + activated + ", targetColorMode=" + targetColorMode);
        if (targetColorMode >= 0) {
            this.mVivoColorManager.setActualColorModeSetting(targetColorMode);
        }
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void onForegroundPackageChanged(String name) {
        int colorModeSetting = this.mVivoColorManager.getUserColorModeSetting();
        int actualColorMode = this.mVivoColorManager.getActualColorModeSetting();
        int colorModeApp = getForegroundAppColorMode(name);
        if (this.mAppObserver.isLauncher(name) || name == null) {
            if (colorModeSetting != actualColorMode && actualColorMode != 509) {
                this.mVivoColorManager.setActualColorModeSetting(colorModeSetting);
                this.mVivoLtmController.setLtmOn(false, 0);
            }
            if (this.mVivoColorManager.getEyeProtectionRatio() != 1.0f) {
                this.mVivoColorManager.setEyeProtectionRatio(1.0f);
            }
        } else if (isInForeground(name) && getPackageSettingState(name) == 1 && colorModeApp != -1 && actualColorMode != colorModeApp) {
            if (this.mVivoColorManager.mDtm != null && !this.mVivoColorManager.mDtm.isDisplayColorSupport(colorModeApp)) {
                return;
            }
            this.mVivoColorManager.setActualColorModeSetting(colorModeApp);
            this.mVivoLtmController.setLtmOn(true, 2);
        }
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void onForegroundActivityChanged(String name, int state) {
    }

    @Override // com.android.server.display.color.displayenhance.PowerTemperatureListener
    public boolean onTemperatureChanged(int temperature) {
        if (DBG) {
            VSlog.d(TAG, "onTemperatureChanged: " + temperature);
        }
        synchronized (this.mMainSwitchLock) {
            boolean needUpdate = super.checkTemperature(temperature);
            if (needUpdate) {
                putMainSwitchSetting(this.isMainSwitchOpen ? 1 : 0);
            }
        }
        return this.isHighTemp;
    }

    @Override // com.android.server.display.color.displayenhance.PowerTemperatureListener
    public boolean onPowerStateChanged(int state) {
        boolean needUpdate;
        if (DBG) {
            VSlog.d(TAG, "onPowerStateChanged: " + state);
        }
        synchronized (this.mMainSwitchLock) {
            needUpdate = super.checkLowPowerStatus(state);
            if (needUpdate) {
                putMainSwitchSetting(this.isMainSwitchOpen ? 1 : 0);
            }
        }
        return needUpdate;
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void dump(PrintWriter pw) {
        pw.println("Sdr2HdrSw display enhance:");
        for (String key : this.mPackageSdr2HdrSwMap.keySet()) {
            PackageSdr2HdrSwInfo info = this.mPackageSdr2HdrSwMap.get(key);
            Integer state = this.mAppSwitch.get(key);
            int appSwitchState = state != null ? state.intValue() : 0;
            pw.println("    Package: " + key + ", " + appSwitchState);
            StringBuilder sb = new StringBuilder();
            sb.append("        color mode: ");
            sb.append(info.colorMode);
            pw.println(sb.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PackageSdr2HdrSwInfo {
        private int colorMode;
        boolean colorModeConfig;

        private PackageSdr2HdrSwInfo() {
            this.colorModeConfig = false;
            this.colorMode = 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parserAppInfo() {
        Resources res = this.mContext.getResources();
        if (res == null) {
            return;
        }
        try {
            XmlResourceParser xmlParser = res.getXml(51576836);
            try {
                PackageSdr2HdrSwInfo info = null;
                if (DBG) {
                    VSlog.d(TAG, "paser PackageSdr2HdrSwInfo start");
                }
                for (int event = xmlParser.getEventType(); event != 1; event = xmlParser.next()) {
                    if (event == 2) {
                        if ("package".equals(xmlParser.getName())) {
                            String pkg = xmlParser.getAttributeValue(0);
                            if (DBG) {
                                VSlog.d(TAG, "<<<== package : " + pkg + " ==>>>");
                            }
                            info = new PackageSdr2HdrSwInfo();
                            this.mPackageSdr2HdrSwMap.put(pkg, info);
                            this.mAppPackageList.add(pkg);
                            this.mAppSwitch.put(pkg, 1);
                        } else if ("feature".equals(xmlParser.getName())) {
                            String feature = xmlParser.getAttributeValue(0);
                            if (info != null && "colorMode".equals(feature)) {
                                String featureState = xmlParser.nextText();
                                try {
                                    info.colorMode = Integer.parseInt(featureState);
                                    info.colorModeConfig = true;
                                    if (DBG) {
                                        VSlog.d(TAG, "feature : " + feature + ", colorMode : " + info.colorMode);
                                    }
                                } catch (NumberFormatException e) {
                                }
                            }
                        }
                    }
                }
                if (DBG) {
                    VSlog.d(TAG, "paser PackageSdr2HdrSwInfo end");
                }
                initAppSwitch();
            } catch (XmlPullParserException e2) {
                e2.printStackTrace();
            }
        } catch (Resources.NotFoundException e3) {
            e3.printStackTrace();
        } catch (IOException e4) {
            e4.printStackTrace();
        }
    }

    private void initAppSwitch() {
        String appSwitch = getAppSwitchSetting();
        if (appSwitch != null) {
            super.initAppSwitch(appSwitch);
        }
        putAppSwitchSetting();
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceController
    public void setDisplayEnhanceState(int state) {
        synchronized (this.mMainSwitchLock) {
            putMainSwitchSetting(state);
            putUserSwitchSetting(state);
            super.setDisplayEnhanceState(state);
        }
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceController
    public int getDisplayEnhanceState() {
        return getMainSwitchSetting();
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceController
    public void setPackageSettingState(String name, int state) {
        super.setPackageSettingState(name, state);
        putAppSwitchSetting();
    }
}