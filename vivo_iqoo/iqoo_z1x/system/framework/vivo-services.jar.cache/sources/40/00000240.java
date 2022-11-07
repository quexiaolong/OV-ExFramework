package com.android.server.display.color.displayenhance;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.Display;
import com.android.server.FgThread;
import com.android.server.display.color.displayenhance.MemcHdrWhiteList;
import com.vivo.services.rms.display.RefreshRateAdjuster;
import com.vivo.services.rms.display.SceneManager;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class MemcDisplayEnhanceController extends DisplayEnhanceController implements DisplayEnhanceListener, PowerTemperatureListener {
    private static final String APPS_MEMC_TOAST_STATUS = "apps_memc_toast_status";
    public static final int DUAL_CHANNEL = 2;
    private static final String MEMC_APPS_SWITCH_SETTING = "memc_apps_switch_setting";
    private static final String MEMC_MAIN_SWITCH_SETTING = "memc_main_switch_setting";
    private static final String MEMC_USER_SWITCH_SETTING = "memc_user_switch_setting";
    private static final int MSG_ACQUIRE_REFRESH_RATE = 3;
    private static final int MSG_INIT_APP_LIST = 0;
    private static final int MSG_SET_MEMC_STATE = 2;
    private static final int MSG_UPDATE_APP_LIST = 1;
    public static final int SINGLE_CHANNEL = 1;
    static final String TAG = "MemcDisplayEnhanceController";
    private static MemcDisplayEnhanceController mMemcController = null;
    private boolean DBG;
    private ApplicationPackageObserver mAppObserver;
    private Context mContext;
    private DisplayEnhanceIrisConfig mDisplayEnhanceConfig;
    private DisplayEnhanceToast mDisplayEnhanceToast;
    private int mHandle;
    private Handler mHandler;
    private MemcHdrWhiteList mMemcHdrWhiteList;
    private boolean mMemcWindowEnable;
    private final Object mPackageWhiteListLock;
    private volatile HashMap<String, HashMap<String, String>> mPackageWhiteListMap;

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

    private MemcDisplayEnhanceController(Context context) {
        super(context);
        this.mHandle = -1;
        this.DBG = SystemProperties.getBoolean("persist.vivo.display.enhance.debug", false);
        this.mMemcWindowEnable = SystemProperties.getBoolean("persist.vivo.memc.window.enable", false);
        this.mPackageWhiteListMap = null;
        this.mPackageWhiteListLock = new Object();
        this.mAppObserver = null;
        this.mDisplayEnhanceConfig = null;
        this.mDisplayEnhanceToast = null;
        this.mMemcHdrWhiteList = null;
        this.mContext = context;
        this.mHandler = new MemcHandler(FgThread.get().getLooper());
        this.mAppObserver = ApplicationPackageObserver.getInstance(context);
        this.mDisplayEnhanceConfig = DisplayEnhanceIrisConfig.getInstance(context);
        this.mDisplayEnhanceToast = DisplayEnhanceToast.getInstance(context);
        MemcHdrWhiteList memcHdrWhiteList = MemcHdrWhiteList.getInstance(context);
        this.mMemcHdrWhiteList = memcHdrWhiteList;
        memcHdrWhiteList.setWhiteListUpdataeListener(new MemcHdrWhiteList.WhiteListUpdataeListener() { // from class: com.android.server.display.color.displayenhance.MemcDisplayEnhanceController.1
            @Override // com.android.server.display.color.displayenhance.MemcHdrWhiteList.WhiteListUpdataeListener
            public void updateWhiteList(HashMap<String, HashMap<String, String>> hashMap) {
                Message msg = MemcDisplayEnhanceController.this.mHandler.obtainMessage();
                msg.what = 1;
                msg.obj = hashMap;
                MemcDisplayEnhanceController.this.mHandler.sendMessage(msg);
            }
        });
        this.mHandler.sendEmptyMessage(0);
        VivoDisplayEnhanceManagerService.registerPowerTempListener(this);
        this.isUserMainSwitchOpen = getUserSwitchSetting() == 1;
        this.isMainSwitchOpen = getMainSwitchSetting() == 1;
        if (this.isMainSwitchOpen) {
            VivoDisplayEnhanceManagerService.registerDisplayEnhanceListener(this);
        }
        if (this.mMemcWindowEnable) {
            this.mDisplayEnhanceConfig.setMemcWindowEnable(true);
        }
    }

    public static MemcDisplayEnhanceController getInstance(Context context) {
        if (mMemcController == null) {
            synchronized (MemcDisplayEnhanceController.class) {
                if (mMemcController == null) {
                    mMemcController = new MemcDisplayEnhanceController(context);
                }
            }
        }
        return mMemcController;
    }

    /* loaded from: classes.dex */
    private final class MemcHandler extends Handler {
        public MemcHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                synchronized (MemcDisplayEnhanceController.this.mPackageWhiteListLock) {
                    MemcDisplayEnhanceController.this.mPackageWhiteListMap = MemcDisplayEnhanceController.this.mMemcHdrWhiteList.parserAppInfo();
                }
            } else if (i != 1) {
                if (i != 2) {
                    if (i == 3) {
                        MemcDisplayEnhanceController.this.acquireRefreshRate(msg.arg1);
                        MemcDisplayEnhanceController.this.sendSetMemcStateMsg(msg.arg2, 100);
                        return;
                    }
                    return;
                } else if (msg.arg1 != 0) {
                    if (MemcDisplayEnhanceController.this.getRefreshRate() == 60) {
                        MemcDisplayEnhanceController.this.mDisplayEnhanceConfig.setIrisStateMachine(1, msg.arg1);
                        String packageName = MemcDisplayEnhanceController.this.mAppObserver.getForegroundAppPackageName();
                        if (MemcDisplayEnhanceController.this.mDisplayEnhanceToast.getPackageToastStatus(packageName) == 0) {
                            MemcDisplayEnhanceController.this.mDisplayEnhanceToast.showCommonActivatedToast();
                            MemcDisplayEnhanceController.this.mDisplayEnhanceToast.setPackageToastStatus(packageName, 1);
                            return;
                        }
                        return;
                    }
                    VSlog.e(MemcDisplayEnhanceController.TAG, "memc fail: current refresh rate is " + MemcDisplayEnhanceController.this.getRefreshRate() + " , not 60!");
                    return;
                } else {
                    MemcDisplayEnhanceController.this.releaseRefreshRate();
                    MemcDisplayEnhanceController.this.mDisplayEnhanceConfig.setIrisStateMachine(1, 0);
                    return;
                }
            }
            if (msg.obj != null) {
                synchronized (MemcDisplayEnhanceController.this.mPackageWhiteListLock) {
                    MemcDisplayEnhanceController.this.mPackageWhiteListMap = (HashMap) msg.obj;
                }
            }
            MemcDisplayEnhanceController memcDisplayEnhanceController = MemcDisplayEnhanceController.this;
            memcDisplayEnhanceController.initAppSwitch(memcDisplayEnhanceController.mPackageWhiteListMap);
            MemcDisplayEnhanceController.this.mDisplayEnhanceToast.initToastStatus(MemcDisplayEnhanceController.this.mPackageWhiteListMap);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void acquireRefreshRate(int rate) {
        if (this.mHandle < 0) {
            int clientPid = Process.myPid();
            int acquireRefreshRate = (int) RefreshRateAdjuster.getInstance().acquireRefreshRate(SceneManager.FIX_RATE_SCENE, "MemcDisplayEnhance", rate, 10, 0, 0, clientPid, 0, false);
            this.mHandle = acquireRefreshRate;
            if (acquireRefreshRate < 0) {
                VSlog.e(TAG, "acquireRefreshRate fail");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseRefreshRate() {
        if (this.mHandle > 0) {
            RefreshRateAdjuster.getInstance().releaseRefreshRate(this.mHandle);
            this.mHandle = -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getRefreshRate() {
        DisplayManager dm = (DisplayManager) this.mContext.getSystemService("display");
        Display display = dm.getDisplay(0);
        return (int) display.getRefreshRate();
    }

    private void sendAcquireRefreshRateMsg(int rate, int memcChanel, int delay) {
        Message msg = this.mHandler.obtainMessage();
        msg.what = 3;
        msg.arg1 = rate;
        msg.arg2 = memcChanel;
        if (this.mHandler.hasMessages(3)) {
            VSlog.d(TAG, "Has msg acquire refresh rate,remove it!");
            this.mHandler.removeMessages(3);
        }
        if (this.mHandler.hasMessages(2)) {
            VSlog.d(TAG, "Has msg set memc state,remove it!");
            this.mHandler.removeMessages(2);
        }
        this.mHandler.sendMessageDelayed(msg, delay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendSetMemcStateMsg(int state, int delay) {
        Message msg = this.mHandler.obtainMessage();
        msg.what = 2;
        msg.arg1 = state;
        if (this.mHandler.hasMessages(3)) {
            VSlog.d(TAG, "Has msg acquire refresh rate,remove it!");
            this.mHandler.removeMessages(3);
        }
        if (this.mHandler.hasMessages(2)) {
            VSlog.d(TAG, "Has msg set memc state,remove it!");
            this.mHandler.removeMessages(2);
        }
        this.mHandler.sendMessageDelayed(msg, delay);
    }

    private void setMemcState(int state, int delay) {
        VSlog.d(TAG, "setMemcState state=" + state + " delay=" + delay);
        if (state == 0) {
            sendSetMemcStateMsg(0, 0);
        } else {
            sendAcquireRefreshRateMsg(60, state, delay);
        }
    }

    private void putMainSwitchSetting(int value) {
        VSlog.d(TAG, "putMainSwitchSetting: " + value);
        if (value == 1) {
            VivoDisplayEnhanceManagerService.registerDisplayEnhanceListener(this);
        } else {
            VivoDisplayEnhanceManagerService.unregisterDisplayEnhanceListener(this);
        }
        Settings.System.putIntForUser(this.mContext.getContentResolver(), MEMC_MAIN_SWITCH_SETTING, value, -2);
    }

    private int getMainSwitchSetting() {
        int value = Settings.System.getIntForUser(this.mContext.getContentResolver(), MEMC_MAIN_SWITCH_SETTING, 0, -2);
        return value;
    }

    private void putUserSwitchSetting(int value) {
        VSlog.d(TAG, "putUserSwitchSetting: " + value);
        Settings.System.putIntForUser(this.mContext.getContentResolver(), MEMC_USER_SWITCH_SETTING, value, -2);
    }

    public int getUserSwitchSetting() {
        int value = Settings.System.getIntForUser(this.mContext.getContentResolver(), MEMC_USER_SWITCH_SETTING, 0, -2);
        return value;
    }

    private void putAppSwitchSetting() {
        try {
            synchronized (this.mAppSwitchLock) {
                JSONObject object = new JSONObject(this.mAppSwitch);
                VSlog.d(TAG, "putAppSwitchSetting:  json = " + object.toString());
                Settings.System.putStringForUser(this.mContext.getContentResolver(), MEMC_APPS_SWITCH_SETTING, object.toString(), -2);
            }
        } catch (Exception e) {
            VSlog.e(TAG, "putAppSwitchSetting: json exception ");
        }
    }

    private String getAppSwitchSetting() {
        return Settings.System.getStringForUser(this.mContext.getContentResolver(), MEMC_APPS_SWITCH_SETTING, -2);
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void onScreenStateChanged(int state) {
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void onForegroundPackageChanged(String name) {
        HashMap<String, String> activityWhiteListMap;
        String memcChannel;
        synchronized (this.mPackageWhiteListLock) {
            activityWhiteListMap = this.mPackageWhiteListMap.get(name);
        }
        if (this.mAppObserver.isLauncher(name) || name == null) {
            setMemcState(0, 0);
        } else if (activityWhiteListMap != null && getPackageSettingState(name) == 1 && (memcChannel = activityWhiteListMap.get(this.mAppObserver.getForegroundActivityName())) != null) {
            setMemcState(getMemcMode(memcChannel), 1000);
        }
    }

    private int getMemcMode(String name) {
        if ("dual-channel".equals(name)) {
            return 2;
        }
        if ("single-channel".equals(name)) {
            return 1;
        }
        return -1;
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void onForegroundActivityChanged(String name, int state) {
        HashMap<String, String> activityWhiteListMap;
        String packageName = this.mAppObserver.getForegroundAppPackageName();
        synchronized (this.mPackageWhiteListLock) {
            activityWhiteListMap = this.mPackageWhiteListMap.get(packageName);
        }
        if (activityWhiteListMap != null && getPackageSettingState(packageName) == 1 && activityWhiteListMap.containsKey(name)) {
            if (state == 1) {
                String memcChannel = activityWhiteListMap.get(name);
                if (memcChannel != null) {
                    String preActivityName = this.mAppObserver.getForegroundActivityName();
                    if (preActivityName == null || !preActivityName.contains("Launcher")) {
                        setMemcState(getMemcMode(memcChannel), 0);
                        return;
                    } else {
                        setMemcState(getMemcMode(memcChannel), 1000);
                        return;
                    }
                }
                return;
            }
            setMemcState(0, 0);
        }
    }

    @Override // com.android.server.display.color.displayenhance.PowerTemperatureListener
    public boolean onTemperatureChanged(int temperature) {
        HashMap<String, String> activityWhiteListMap;
        String memcChannel;
        synchronized (this.mMainSwitchLock) {
            boolean needUpdate = super.checkTemperature(temperature);
            if (needUpdate) {
                boolean memcEnable = true;
                putMainSwitchSetting(this.isMainSwitchOpen ? 1 : 0);
                String packageName = this.mAppObserver.getForegroundAppPackageName();
                String activityName = this.mAppObserver.getForegroundActivityName();
                if (getPackageSettingState(packageName) == 1 && activityName != null) {
                    synchronized (this.mPackageWhiteListLock) {
                        activityWhiteListMap = this.mPackageWhiteListMap.get(packageName);
                    }
                    if (activityWhiteListMap != null && (memcChannel = activityWhiteListMap.get(activityName)) != null) {
                        if (this.mDisplayEnhanceConfig.getMemcMode() == 0) {
                            memcEnable = false;
                        }
                        if (!this.isMainSwitchOpen && memcEnable) {
                            setMemcState(0, 0);
                        } else if (this.isMainSwitchOpen && !memcEnable) {
                            setMemcState(getMemcMode(memcChannel), 0);
                        }
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
        HashMap<String, String> activityWhiteListMap;
        pw.println("Memc display enhance:");
        for (String key : this.mPackageWhiteListMap.keySet()) {
            synchronized (this.mPackageWhiteListLock) {
                activityWhiteListMap = this.mPackageWhiteListMap.get(key);
            }
            Integer state = this.mAppSwitch.get(key);
            int appSwitchState = state != null ? state.intValue() : 0;
            pw.println("    Package: " + key + ", " + appSwitchState);
            if (activityWhiteListMap != null && !activityWhiteListMap.isEmpty()) {
                for (Map.Entry<String, String> entry : activityWhiteListMap.entrySet()) {
                    pw.println("        Activity: " + entry.getKey() + ", " + entry.getValue());
                }
            }
        }
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