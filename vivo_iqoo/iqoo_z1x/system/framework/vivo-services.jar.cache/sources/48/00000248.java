package com.android.server.display.color.displayenhance;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import com.android.server.FgThread;
import com.android.server.display.color.displayenhance.MemcHdrWhiteList;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class Sdr2HdrHwDisplayEnhanceController extends DisplayEnhanceController implements DisplayEnhanceListener, PowerTemperatureListener {
    private static final String APPS_SDR2HDR_HARDWARE_TOAST_STATUS = "apps_sdr2hdr_hardware_toast_status";
    private static final int MSG_INIT_APP_LIST = 0;
    private static final int MSG_UPDATE_APP_LIST = 1;
    private static final String SDR2HDR_HARDWARE_APPS_SWITCH_SETTING = "sdr2hdr_hardware_apps_switch_setting";
    private static final String SDR2HDR_HARDWARE_MAIN_SWITCH_SETTING = "sdr2hdr_hardware_main_switch_setting";
    private static final String SDR2HDR_HARDWARE_USER_SWITCH_SETTING = "sdr2hdr_hardware_user_switch_setting";
    static final String TAG = "Sdr2HdrHwDisplayEnhanceController";
    private static Sdr2HdrHwDisplayEnhanceController mSdr2HdrHwController = null;
    private ApplicationPackageObserver mAppObserver;
    private Context mContext;
    private DisplayEnhanceIrisConfig mDisplayEnhanceConfig;
    private DisplayEnhanceToast mDisplayEnhanceToast;
    private Handler mHandler;
    private MemcHdrWhiteList mMemcHdrWhiteList;

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

    private Sdr2HdrHwDisplayEnhanceController(Context context) {
        super(context);
        this.mAppObserver = null;
        this.mDisplayEnhanceConfig = null;
        this.mDisplayEnhanceToast = null;
        this.mMemcHdrWhiteList = null;
        this.mContext = context;
        this.mHandler = new Sdr2HdrHwHandler(FgThread.get().getLooper());
        this.mAppObserver = ApplicationPackageObserver.getInstance(context);
        this.mDisplayEnhanceConfig = DisplayEnhanceIrisConfig.getInstance(context);
        this.mDisplayEnhanceToast = DisplayEnhanceToast.getInstance(context);
        MemcHdrWhiteList memcHdrWhiteList = MemcHdrWhiteList.getInstance(context);
        this.mMemcHdrWhiteList = memcHdrWhiteList;
        memcHdrWhiteList.setWhiteListUpdataeListener(new MemcHdrWhiteList.WhiteListUpdataeListener() { // from class: com.android.server.display.color.displayenhance.Sdr2HdrHwDisplayEnhanceController.1
            @Override // com.android.server.display.color.displayenhance.MemcHdrWhiteList.WhiteListUpdataeListener
            public void updateWhiteList(HashMap<String, HashMap<String, String>> hashMap) {
                Message msg = Sdr2HdrHwDisplayEnhanceController.this.mHandler.obtainMessage();
                msg.what = 1;
                msg.obj = hashMap;
                Sdr2HdrHwDisplayEnhanceController.this.mHandler.sendMessage(msg);
            }
        });
        this.mHandler.sendEmptyMessage(0);
        VivoDisplayEnhanceManagerService.registerPowerTempListener(this);
        this.isUserMainSwitchOpen = getUserSwitchSetting() == 1;
        this.isMainSwitchOpen = getMainSwitchSetting() == 1;
        if (this.isMainSwitchOpen) {
            VivoDisplayEnhanceManagerService.registerDisplayEnhanceListener(this);
        }
    }

    public static Sdr2HdrHwDisplayEnhanceController getInstance(Context context) {
        if (mSdr2HdrHwController == null) {
            synchronized (Sdr2HdrHwDisplayEnhanceController.class) {
                if (mSdr2HdrHwController == null) {
                    mSdr2HdrHwController = new Sdr2HdrHwDisplayEnhanceController(context);
                }
            }
        }
        return mSdr2HdrHwController;
    }

    /* loaded from: classes.dex */
    private final class Sdr2HdrHwHandler extends Handler {
        public Sdr2HdrHwHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            HashMap packageMap = null;
            int i = msg.what;
            if (i == 0) {
                packageMap = Sdr2HdrHwDisplayEnhanceController.this.mMemcHdrWhiteList.parserAppInfo();
            } else if (i != 1) {
                return;
            }
            if (msg.obj != null) {
                packageMap = (HashMap) msg.obj;
            }
            if (packageMap != null) {
                Sdr2HdrHwDisplayEnhanceController.this.initAppSwitch(packageMap);
                Sdr2HdrHwDisplayEnhanceController.this.mDisplayEnhanceToast.initToastStatus(packageMap);
            }
        }
    }

    private void putMainSwitchSetting(int value) {
        VSlog.d(TAG, "putMainSwitchSetting: " + value);
        if (value == 1) {
            VivoDisplayEnhanceManagerService.registerDisplayEnhanceListener(this);
        } else {
            VivoDisplayEnhanceManagerService.unregisterDisplayEnhanceListener(this);
        }
        Settings.System.putIntForUser(this.mContext.getContentResolver(), SDR2HDR_HARDWARE_MAIN_SWITCH_SETTING, value, -2);
    }

    private int getMainSwitchSetting() {
        int value = Settings.System.getIntForUser(this.mContext.getContentResolver(), SDR2HDR_HARDWARE_MAIN_SWITCH_SETTING, 0, -2);
        return value;
    }

    private void putUserSwitchSetting(int value) {
        VSlog.d(TAG, "putUserSwitchSetting: " + value);
        Settings.System.putIntForUser(this.mContext.getContentResolver(), SDR2HDR_HARDWARE_USER_SWITCH_SETTING, value, -2);
    }

    public int getUserSwitchSetting() {
        int value = Settings.System.getIntForUser(this.mContext.getContentResolver(), SDR2HDR_HARDWARE_USER_SWITCH_SETTING, 0, -2);
        return value;
    }

    private void putAppSwitchSetting() {
        try {
            synchronized (this.mAppSwitchLock) {
                JSONObject object = new JSONObject(this.mAppSwitch);
                VSlog.d(TAG, "putAppSwitchSetting:  json = " + object.toString());
                Settings.System.putStringForUser(this.mContext.getContentResolver(), SDR2HDR_HARDWARE_APPS_SWITCH_SETTING, object.toString(), -2);
            }
        } catch (Exception e) {
            VSlog.e(TAG, "putAppSwitchSetting: json exception ");
        }
    }

    private String getAppSwitchSetting() {
        return Settings.System.getStringForUser(this.mContext.getContentResolver(), SDR2HDR_HARDWARE_APPS_SWITCH_SETTING, -2);
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void onScreenStateChanged(int state) {
        String packageName = this.mAppObserver.getForegroundAppPackageName();
        if (getPackageSettingState(packageName) == 1) {
            if (state == VivoDisplayEnhanceManagerService.USER_PRESENT) {
                this.mDisplayEnhanceConfig.setIrisStateMachine(2, 2);
            } else if (state == VivoDisplayEnhanceManagerService.SCREEN_OFF) {
                this.mDisplayEnhanceConfig.setIrisStateMachine(2, 0);
            }
        }
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void onForegroundPackageChanged(String name) {
        if (this.mAppObserver.isLauncher(name) || name == null) {
            this.mDisplayEnhanceConfig.setIrisStateMachine(2, 0);
        } else if (getPackageSettingState(name) == 1) {
            this.mDisplayEnhanceConfig.setIrisStateMachine(2, 2);
            if (this.mDisplayEnhanceToast.getPackageToastStatus(name) == 0) {
                this.mDisplayEnhanceToast.showCommonActivatedToast();
                this.mDisplayEnhanceToast.setPackageToastStatus(name, 1);
            }
        }
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void onForegroundActivityChanged(String name, int state) {
    }

    @Override // com.android.server.display.color.displayenhance.PowerTemperatureListener
    public boolean onTemperatureChanged(int temperature) {
        synchronized (this.mMainSwitchLock) {
            boolean needUpdate = super.checkTemperature(temperature);
            if (needUpdate) {
                boolean hdrEnable = true;
                putMainSwitchSetting(this.isMainSwitchOpen ? 1 : 0);
                String packageName = this.mAppObserver.getForegroundAppPackageName();
                if (getPackageSettingState(packageName) == 1) {
                    if (this.mDisplayEnhanceConfig.getSdr2HdrMode() == 0) {
                        hdrEnable = false;
                    }
                    if (!this.isMainSwitchOpen && hdrEnable) {
                        this.mDisplayEnhanceConfig.setIrisStateMachine(2, 0);
                    } else if (this.isMainSwitchOpen && !hdrEnable) {
                        this.mDisplayEnhanceConfig.setIrisStateMachine(2, 2);
                    }
                }
            }
        }
        return this.isHighTemp;
    }

    @Override // com.android.server.display.color.displayenhance.PowerTemperatureListener
    public boolean onPowerStateChanged(int state) {
        boolean needUpdate;
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
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initAppSwitch(HashMap<String, HashMap<String, String>> packageMap) {
        synchronized (this.mAppSwitchLock) {
            for (String key : packageMap.keySet()) {
                this.mAppSwitch.put(key, 1);
                this.mAppPackageList.add(key);
            }
        }
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