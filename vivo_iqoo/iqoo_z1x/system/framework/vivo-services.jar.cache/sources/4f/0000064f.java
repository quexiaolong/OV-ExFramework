package com.vivo.services.memc;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.multidisplay.MultiDisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.FtDeviceInfo;
import android.widget.Toast;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerService;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.configurationManager.DecryptUtils;
import com.vivo.services.proxy.game.GameSceneProxyManager;
import com.vivo.services.superresolution.Constant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.StringList;
import vivo.app.memc.IMemcManager;

/* loaded from: classes.dex */
public class MemcManagerService extends IMemcManager.Stub {
    private static final String ACTION_CHANGE_MEMC_STATE = "action_change_memc_state";
    private static final long CHECK_TEMPURE_TIME = 30000;
    private static final String MEMC_CONFIG_FILE_PATH = "/data/bbkcore/DynamicCompensation_UniConfigR_1.0.xml";
    private static final String MEMC_CONFIG_SUPPORT_APPS = "support_app";
    private static final String MEMC_STATE = "memc_state";
    private static final String MEMC_SUPPORT_SAVE_1080P = "memc_support_save_1080p";
    private static final int MEMC_SUPPORT_SAVE_1080P_CLOSE = 0;
    private static final int MEMC_SUPPORT_SAVE_1080P_OPEN = 1;
    private static final int MILLILS_OF_WEEK = 604800000;
    private static final int MSG_CHECK_TEMPURE = 1;
    private static final int MSG_REPORT_DATA = 2;
    private static final int MSG_REPORT_DATA_INIT = 3;
    private static final int MSG_STATUS_CHANGED = 4;
    private static final String PLATFORM_SDM8250 = "kona";
    private static final String PLATFORM_SDM8350 = "lahaina";
    private static final String PROP_MEMC_ENABLE = "persist.sys.memc.enable";
    private static final int REPORT_SPACE = 604;
    private static final String SETTING_VALUE_MAIN = "memc_main";
    private static final String SETTING_VALUE_TEMPERATURE = "temperature_main";
    public static final int STATUS_HIGH_TEMPERATURE = 0;
    public static final int STATUS_MEMC_CLOSE = 0;
    public static final int STATUS_MEMC_OPEN = 1;
    public static final int STATUS_NORMAL_TEMPERATURE = 1;
    public static final String TAG = "MemcManagerService";
    private static volatile MemcManagerService sInstance;
    private final boolean deviceSupportMemc;
    private IActivityManager mActivityManager;
    private StringList mConfigSupportApps;
    private AbsConfigurationManager mConfigurationManager;
    private ContentResolver mContentResolver;
    private Context mContext;
    private DisplayManager mDisplayManager;
    private String mImei;
    private ActivityRecord mLastResumedActivity;
    private HandlerThread mMemcThread;
    private MultiDisplayManager mMultiDisplayManager;
    private ProcessObserver mProcessObserver;
    private ContentObserver mSettingsObserver;
    private Handler mWorkHandler;
    private static final String[] SUPPORT_DEVICES = {"PD1981", "PD1955", "PD2011", "PD2024", "PD2025", "PD2049"};
    private static List<String> SUPPORT_APP = Arrays.asList(Constant.APP_GALLERY);
    private static final Uri URI_FOR_MAIN = Settings.Global.getUriFor("memc_main");
    private static final String SETTING_FOR_TEMPURE_LIMIT = "setting_for_tempure_limit";
    private static final Uri URI_FOR_HIGH_TEMPURE = Settings.Global.getUriFor(SETTING_FOR_TEMPURE_LIMIT);
    private int HIGH_TEMPURE = 47;
    private volatile boolean isPowerSaveMode = false;
    private boolean isMemcEnable = false;
    private boolean isHighTempure = false;
    private boolean isMainSwitchOpen = false;
    private boolean isAppShareOpen = false;
    private boolean isSupportApp = false;
    private Handler mMainHandler = new Handler();
    private ConfigurationObserver mSupportAppConfigObserver = new ConfigurationObserver() { // from class: com.vivo.services.memc.MemcManagerService.2
        public void onConfigChange(String file, String name) {
            VLog.d(MemcManagerService.TAG, "getUnifiedConfig: support_apps change file = " + file + "   name = " + name);
            MemcManagerService.this.updateUnifiedConfig();
        }
    };
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() { // from class: com.vivo.services.memc.MemcManagerService.5
        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
            if (displayId == 10086) {
                VLog.i(MemcManagerService.TAG, "onDisplayAdded, displayId = " + displayId);
                MemcManagerService.this.isAppShareOpen = true;
                MemcManagerService.this.notifyStatusChange();
            }
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
            if (displayId == 10086) {
                VLog.i(MemcManagerService.TAG, "onDisplayRemoved, displayId = " + displayId);
                MemcManagerService.this.isAppShareOpen = false;
                MemcManagerService.this.notifyStatusChange();
            }
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
        }
    };

    public static MemcManagerService getInstance(Context context) {
        if (sInstance == null) {
            synchronized (MemcManagerService.class) {
                if (sInstance == null) {
                    sInstance = new MemcManagerService(context);
                }
            }
        }
        return sInstance;
    }

    private MemcManagerService(Context context) {
        setProp(false, true);
        boolean checkDeviceSupportMemc = checkDeviceSupportMemc();
        this.deviceSupportMemc = checkDeviceSupportMemc;
        if (!checkDeviceSupportMemc) {
            return;
        }
        try {
            this.mContext = context;
            HandlerThread handlerThread = new HandlerThread("memc");
            this.mMemcThread = handlerThread;
            handlerThread.start();
            WorkHandler workHandler = new WorkHandler(this.mMemcThread.getLooper());
            this.mWorkHandler = workHandler;
            workHandler.sendEmptyMessageDelayed(3, 30000L);
            this.mProcessObserver = new ProcessObserver(this.mWorkHandler);
            this.mActivityManager = ActivityManager.getService();
            this.mDisplayManager = (DisplayManager) this.mContext.getSystemService(DisplayManager.class);
            this.mMultiDisplayManager = (MultiDisplayManager) this.mContext.getSystemService(MultiDisplayManager.class);
            initSettingObserver();
            initUnifiedConfig();
            init();
        } catch (Exception e) {
            VLog.e(TAG, " exception = " + e.getMessage());
        }
    }

    private void setProp(boolean enable) {
        setProp(enable, false);
    }

    private void setProp(boolean enable, boolean isFirst) {
        VLog.d(TAG, "enable=" + enable);
        if (!isFirst && this.isMemcEnable == enable) {
            return;
        }
        this.isMemcEnable = enable;
        if (!isFirst) {
            Intent intent = new Intent();
            intent.setAction(ACTION_CHANGE_MEMC_STATE);
            intent.putExtra(MEMC_STATE, this.isMemcEnable ? 1 : 0);
            this.mContext.sendBroadcast(intent);
        }
        SystemProperties.set(PROP_MEMC_ENABLE, (enable ? 1 : 0) + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
    }

    private void init() throws Settings.SettingNotFoundException {
        VLog.d(TAG, "init memc");
        MemcReceiver memcReceiver = new MemcReceiver();
        IntentFilter stopSceneIntentFilter = new IntentFilter();
        stopSceneIntentFilter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        this.mContext.registerReceiver(memcReceiver, stopSceneIntentFilter);
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mContentResolver = contentResolver;
        contentResolver.registerContentObserver(URI_FOR_MAIN, false, this.mSettingsObserver);
        this.mContentResolver.registerContentObserver(URI_FOR_HIGH_TEMPURE, false, this.mSettingsObserver);
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        this.isPowerSaveMode = pm.isPowerSaveMode();
        this.isMainSwitchOpen = Settings.Global.getInt(this.mContentResolver, "memc_main", 0) == 1;
        this.HIGH_TEMPURE = Settings.Global.getInt(this.mContext.getContentResolver(), SETTING_FOR_TEMPURE_LIMIT, this.HIGH_TEMPURE);
        this.isHighTempure = false;
        Settings.Global.putInt(this.mContext.getContentResolver(), SETTING_VALUE_TEMPERATURE, 1);
        setObserver();
        this.mWorkHandler.sendEmptyMessageDelayed(1, 10000L);
        this.mWorkHandler.sendEmptyMessage(4);
        if (PLATFORM_SDM8250.equals(Build.BOARD)) {
            VLog.d(TAG, "Settings.Global.putInt =kona 0");
            Settings.Global.putInt(this.mContext.getContentResolver(), MEMC_SUPPORT_SAVE_1080P, 0);
        }
        if (PLATFORM_SDM8350.equals(Build.BOARD)) {
            VLog.d(TAG, "Settings.Global.putInt =lahaina 0");
            Settings.Global.putInt(this.mContext.getContentResolver(), MEMC_SUPPORT_SAVE_1080P, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setObserver() {
        try {
            this.mWorkHandler.removeMessages(1);
            if (this.isMainSwitchOpen) {
                this.mWorkHandler.sendEmptyMessageDelayed(1, 30000L);
                this.mActivityManager.registerProcessObserver(this.mProcessObserver);
                this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mWorkHandler);
                this.isAppShareOpen = this.mMultiDisplayManager.isAppShareDisplayExist();
            } else {
                this.mActivityManager.unregisterProcessObserver(this.mProcessObserver);
                this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
            }
        } catch (Exception e) {
            VLog.e(TAG, "startCheckTempure error " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyStatusChange() {
        boolean b = (!this.isMainSwitchOpen || this.isPowerSaveMode || this.isHighTempure || !this.isSupportApp || this.isAppShareOpen) ? false : true;
        setProp(b);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getDeviceId() {
        try {
            if (!TextUtils.isEmpty(this.mImei)) {
                return;
            }
            TelephonyManager mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            this.mImei = mTelephonyManager.getImei();
        } catch (Exception e) {
            VLog.e(TAG, " exception = " + e.getMessage());
            this.mImei = "1234567890123456";
        }
    }

    private void initSettingObserver() {
        this.mSettingsObserver = new ContentObserver(this.mWorkHandler) { // from class: com.vivo.services.memc.MemcManagerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (MemcManagerService.this.mImei == null) {
                    MemcManagerService.this.getDeviceId();
                }
                VLog.d(MemcManagerService.TAG, "onChange");
                if (uri != null) {
                    if (!uri.equals(MemcManagerService.URI_FOR_MAIN)) {
                        if (uri.equals(MemcManagerService.URI_FOR_HIGH_TEMPURE)) {
                            try {
                                MemcManagerService.this.HIGH_TEMPURE = Settings.Global.getInt(MemcManagerService.this.mContext.getContentResolver(), MemcManagerService.SETTING_FOR_TEMPURE_LIMIT);
                                VLog.d(MemcManagerService.TAG, "URI_FOR_HIGH_TEMPURE " + MemcManagerService.this.HIGH_TEMPURE);
                                MemcManagerService.this.checkTempure();
                            } catch (Settings.SettingNotFoundException e) {
                                VLog.e(MemcManagerService.TAG, " exception = " + e.getMessage());
                            }
                        }
                    } else {
                        MemcManagerService memcManagerService = MemcManagerService.this;
                        boolean z = true;
                        memcManagerService.isMainSwitchOpen = Settings.Global.getInt(memcManagerService.mContentResolver, "memc_main", 0) == 1;
                        if (MemcManagerService.this.isMainSwitchOpen) {
                            int tempure = FtDeviceInfo.getBoardTempure();
                            Settings.Global.putInt(MemcManagerService.this.mContext.getContentResolver(), MemcManagerService.SETTING_VALUE_TEMPERATURE, 1);
                            MemcManagerService memcManagerService2 = MemcManagerService.this;
                            if (!memcManagerService2.isHighTempure ? tempure < MemcManagerService.this.HIGH_TEMPURE : tempure < MemcManagerService.this.HIGH_TEMPURE - 2) {
                                z = false;
                            }
                            memcManagerService2.isHighTempure = z;
                            if (MemcManagerService.this.isHighTempure) {
                                VLog.d(MemcManagerService.TAG, "isHighTemperature");
                                MemcManagerService memcManagerService3 = MemcManagerService.this;
                                memcManagerService3.toast(memcManagerService3.mContext.getString(51249633));
                                Settings.Global.putInt(MemcManagerService.this.mContext.getContentResolver(), MemcManagerService.SETTING_VALUE_TEMPERATURE, 0);
                            }
                        }
                        VLog.d(MemcManagerService.TAG, "URI_FOR_MAIN " + MemcManagerService.this.isMainSwitchOpen);
                        MemcManagerService.this.setObserver();
                    }
                    MemcManagerService.this.notifyStatusChange();
                    return;
                }
                VLog.e(MemcManagerService.TAG, "uri = " + uri);
            }
        };
    }

    void initUnifiedConfig() {
        VLog.d(TAG, "getUnifiedConfig: start");
        VivoFrameworkFactory vivoFrameworkFactory = VivoFrameworkFactory.getFrameworkFactoryImpl();
        if (vivoFrameworkFactory != null) {
            AbsConfigurationManager configurationManager = vivoFrameworkFactory.getConfigurationManager();
            this.mConfigurationManager = configurationManager;
            if (configurationManager == null) {
                VLog.e(TAG, "getUnifiedConfig: registerObserver error: mConfigurationManager is null");
                return;
            }
            updateUnifiedConfig();
            this.mConfigurationManager.registerObserver(this.mConfigSupportApps, this.mSupportAppConfigObserver);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateUnifiedConfig() {
        if (this.mConfigurationManager != null) {
            boolean isSupport = DecryptUtils.isAbeSupportDecryptV2();
            VLog.d(TAG, "getUnifiedConfig:  isAbeSupportDecryptV2=" + isSupport);
            StringList stringList = this.mConfigurationManager.getStringList(MEMC_CONFIG_FILE_PATH, MEMC_CONFIG_SUPPORT_APPS);
            this.mConfigSupportApps = stringList;
            if (stringList != null && stringList.getValues() != null && this.mConfigSupportApps.getValues().size() > 0) {
                VLog.d(TAG, "getUnifiedConfig: support_apps = " + this.mConfigSupportApps.toString());
                List<String> configSupportApps = this.mConfigSupportApps.getValues();
                if (configSupportApps.size() > 0) {
                    if (configSupportApps.get(0).equals("CLOSE_ALL")) {
                        SUPPORT_APP.clear();
                    } else {
                        SUPPORT_APP = configSupportApps;
                    }
                    notifyStatusChange();
                    return;
                }
                return;
            }
            return;
        }
        VLog.e(TAG, "getUnifiedConfig: update error: mConfigurationManager is null");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toast(final String toast) {
        this.mMainHandler.post(new Runnable() { // from class: com.vivo.services.memc.MemcManagerService.3
            @Override // java.lang.Runnable
            public void run() {
                Toast.makeText(MemcManagerService.this.mContext, toast, 0).show();
            }
        });
    }

    private void toastT(String toast) {
        final String text = "t:" + toast;
        this.mMainHandler.post(new Runnable() { // from class: com.vivo.services.memc.MemcManagerService.4
            @Override // java.lang.Runnable
            public void run() {
                Toast.makeText(MemcManagerService.this.mContext, text, 0).show();
            }
        });
    }

    public boolean isDeviceSupportMemc() {
        return this.deviceSupportMemc;
    }

    public List<String> getMemcSupportAppList() {
        return SUPPORT_APP;
    }

    private boolean checkDeviceSupportMemc() {
        String[] strArr;
        String deviceName = Build.DEVICE;
        VLog.d(TAG, "isDeviceSupportMemc deviceName = " + deviceName);
        if (TextUtils.isEmpty(deviceName)) {
            return false;
        }
        for (String supportDevice : SUPPORT_DEVICES) {
            if (deviceName.contains(supportDevice)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MemcReceiver extends BroadcastReceiver {
        MemcReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                VLog.d(MemcManagerService.TAG, "onReceive: intent = null");
                return;
            }
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }
            VLog.d(MemcManagerService.TAG, "onReceive: action : " + action);
            if ("android.os.action.POWER_SAVE_MODE_CHANGED".equals(action)) {
                try {
                    PowerManager pm = (PowerManager) MemcManagerService.this.mContext.getSystemService("power");
                    MemcManagerService.this.isPowerSaveMode = pm.isPowerSaveMode();
                    VLog.d(MemcManagerService.TAG, "onReceive: now is in save mode " + pm.isPowerSaveMode());
                } catch (Exception e) {
                    VLog.e(MemcManagerService.TAG, " exception = " + e.getMessage());
                }
            }
            MemcManagerService.this.mWorkHandler.sendEmptyMessage(4);
        }
    }

    /* loaded from: classes.dex */
    class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
            if (i == 1) {
                MemcManagerService.this.checkTempure();
                MemcManagerService.this.notifyStatusChange();
                if (MemcManagerService.this.isMainSwitchOpen) {
                    sendEmptyMessageDelayed(1, 30000L);
                }
            } else if (i == 2) {
                MemcManagerService.this.reportData();
                sendEmptyMessageDelayed(2, 604800000L);
            } else if (i == 3) {
                MemcManagerService.this.reportDataInit();
            } else if (i == 4) {
                MemcManagerService.this.notifyStatusChange();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportData() {
        HashMap<String, String> map = new HashMap<>();
        map.put(DataReport.REPORT_MAIN_SWITCH_STATE_ID_KEY, Settings.Global.getInt(this.mContentResolver, "memc_main", 0) == 1 ? "1" : "0");
        DataReport.reportMainSwitchState(map);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportDataInit() {
        if (TextUtils.isEmpty(this.mImei)) {
            getDeviceId();
        }
        VLog.d(TAG, "reportDataInit");
        long cur = System.currentTimeMillis() % 604800000;
        long set = getMillils().longValue();
        long delay = cur < set ? set - cur : (604800000 + set) - cur;
        int day = (int) (delay / 86400000);
        int hour = (int) ((delay - (day * 86400000)) / 3600000);
        int second = (int) ((((delay - (86400000 * day)) - (GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME * hour)) / 1000) / 60);
        VLog.w(TAG, "reportDataInit: delay = " + day + "d" + hour + "h" + second + "m");
        this.mWorkHandler.sendEmptyMessageDelayed(2, delay);
    }

    private Long getMillils() {
        try {
            long set = Integer.valueOf(this.mImei.substring(8, 14)).intValue() * 604;
            return Long.valueOf(set);
        } catch (Exception e) {
            VLog.e(TAG, "getMillils: strinig transform fail");
            return -1L;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkTempure() {
        int tempure = FtDeviceInfo.getBoardTempure();
        boolean originParam = this.isHighTempure;
        int i = this.HIGH_TEMPURE;
        if (tempure >= i) {
            this.isHighTempure = true;
            if (this.isMemcEnable) {
                toast(this.mContext.getString(51249633));
            }
        } else if (tempure < i - 2) {
            this.isHighTempure = false;
        }
        boolean z = this.isHighTempure;
        if (originParam != z) {
            Settings.Global.putInt(this.mContentResolver, SETTING_VALUE_TEMPERATURE, !z);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ProcessObserver extends IProcessObserver.Stub {
        private final Handler handler;

        public ProcessObserver(Handler handler) {
            this.handler = handler;
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean hasForegroundActivities) {
            MemcManagerService.this.onActivityChange();
        }

        public void onProcessDied(int pid, int uid) {
            MemcManagerService.this.onActivityChange();
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onActivityChange() {
        ActivityTaskManagerService service = ActivityTaskManager.getService();
        if (service instanceof ActivityTaskManagerService) {
            ActivityRecord lastResumedActivity = service.getLastResumedActivity();
            if (this.mLastResumedActivity != lastResumedActivity) {
                this.mLastResumedActivity = lastResumedActivity;
                if (lastResumedActivity == null) {
                    this.isSupportApp = false;
                } else {
                    this.isSupportApp = SUPPORT_APP.contains(lastResumedActivity.packageName);
                }
                notifyStatusChange();
            }
            VLog.d(TAG, "onActivityChange " + this.mLastResumedActivity);
        }
    }
}