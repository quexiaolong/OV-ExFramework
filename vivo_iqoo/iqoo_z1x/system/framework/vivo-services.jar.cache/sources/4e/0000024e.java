package com.android.server.display.color.displayenhance;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.FtBuild;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.FtDeviceInfo;
import android.util.FtFeature;
import com.android.internal.util.DumpUtils;
import com.android.server.FgThread;
import com.android.server.display.color.VivoColorManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import vivo.common.IVivoDisplayEnhanceManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoDisplayEnhanceManagerService implements PackageActivityListener {
    public static final int MSG_FOREGROUND_ACTIVITY_UPDATE = 1;
    public static final int MSG_FOREGROUND_PACKAGE_UPDATE = 0;
    public static final int MSG_SCHEDULE_HIGH_TEMP_CHECK = 2;
    public static final String POWER_SAVE_TYPE = "power_save_type";
    public static final int SETTING_LOW_POWER = 2;
    static final String TAG = "VivoDisplayEnhanceManagerService";
    private ApplicationPackageObserver mAppObserver;
    public BinderService mBinderService;
    private Context mContext;
    private DisplayEnhanceIrisConfig mDisplayEnhanceConfig;
    private Handler mHandler;
    private HawkeyeDisplayEnhanceController mHawkeyeController;
    private final ContentObserver mLowPowerObserver;
    private MemcDisplayEnhanceController mMemcController;
    private Sdr2HdrHwDisplayEnhanceController mSdr2HdrHwController;
    private Sdr2HdrSwDisplayEnhanceController mSdr2HdrSwController;
    private VivoColorManagerService mVivoColorManager;
    public static int TEMPERATURE_MAX = 47;
    private static int CHECK_TEMPERATURE_TIME = 30000;
    public static int SCREEN_OFF = 0;
    public static int USER_PRESENT = 1;
    private static boolean mHightTempTest = SystemProperties.getBoolean("persist.vivo.hight.temperature.test", false);
    private static final ArrayList<DisplayEnhanceListener> mDisplayEnhanceListeners = new ArrayList<>();
    private static final ArrayList<PowerTemperatureListener> mPowerTempListeners = new ArrayList<>();
    private static volatile VivoDisplayEnhanceManagerService mVivoDisplayEnhanceManager = null;
    private static final boolean mSupportVideoHdrSw = FtFeature.isFeatureSupport("vivo.software.video.sdr2hdr");
    private int mScreenState = -1;
    private boolean isHighTemp = false;
    private Runnable mCheckTempRunnable = new Runnable() { // from class: com.android.server.display.color.displayenhance.VivoDisplayEnhanceManagerService.3
        @Override // java.lang.Runnable
        public void run() {
            int temperature = VivoDisplayEnhanceManagerService.this.getCurrentTemperature();
            synchronized (VivoDisplayEnhanceManagerService.mPowerTempListeners) {
                for (int i = VivoDisplayEnhanceManagerService.mPowerTempListeners.size() - 1; i >= 0; i--) {
                    PowerTemperatureListener listener = (PowerTemperatureListener) VivoDisplayEnhanceManagerService.mPowerTempListeners.get(i);
                    VivoDisplayEnhanceManagerService.access$776(VivoDisplayEnhanceManagerService.this, listener.onTemperatureChanged(temperature) ? 1 : 0);
                }
            }
            VivoDisplayEnhanceManagerService.this.mHandler.postDelayed(VivoDisplayEnhanceManagerService.this.mCheckTempRunnable, VivoDisplayEnhanceManagerService.CHECK_TEMPERATURE_TIME);
        }
    };

    /* JADX WARN: Type inference failed for: r0v2, types: [byte, boolean] */
    static /* synthetic */ boolean access$776(VivoDisplayEnhanceManagerService x0, int x1) {
        ?? r0 = (byte) ((x0.isHighTemp ? 1 : 0) | x1);
        x0.isHighTemp = r0;
        return r0;
    }

    private VivoDisplayEnhanceManagerService(VivoColorManagerService colorManager, Context context) {
        this.mAppObserver = null;
        this.mSdr2HdrSwController = null;
        this.mSdr2HdrHwController = null;
        this.mMemcController = null;
        this.mVivoColorManager = null;
        this.mHawkeyeController = null;
        this.mDisplayEnhanceConfig = null;
        this.mBinderService = null;
        this.mLowPowerObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.display.color.displayenhance.VivoDisplayEnhanceManagerService.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                try {
                    int lowPowerLevel = Settings.System.getIntForUser(VivoDisplayEnhanceManagerService.this.mContext.getContentResolver(), "power_save_type", 0, -2);
                    VSlog.d(VivoDisplayEnhanceManagerService.TAG, "onChange:  lowPowerLevel = " + lowPowerLevel);
                    VivoDisplayEnhanceManagerService.this.updatePowerState(lowPowerLevel);
                } catch (Exception e) {
                    VSlog.e(VivoDisplayEnhanceManagerService.TAG, "onChange: mLowPowerObserver SettingNotFoundException:" + e.getMessage());
                }
            }
        };
        this.mContext = context;
        this.mVivoColorManager = colorManager;
        VSlog.d(TAG, "Feature support:  Sdr2HdrSw=" + mSupportVideoHdrSw + ", Sdr2HdrHw=" + isIrisVideoSdr2HdrAvailable() + ", Memc=" + isIrisVideoMemcAvailable());
        if (mSupportVideoHdrSw) {
            this.mSdr2HdrSwController = Sdr2HdrSwDisplayEnhanceController.getInstance(colorManager, context);
        }
        if (isIrisVideoSdr2HdrAvailable()) {
            this.mSdr2HdrHwController = Sdr2HdrHwDisplayEnhanceController.getInstance(context);
        }
        if (isIrisVideoMemcAvailable()) {
            this.mMemcController = MemcDisplayEnhanceController.getInstance(context);
        }
        this.mHawkeyeController = HawkeyeDisplayEnhanceController.getInstance(colorManager, context);
        if (isIrisAvailable()) {
            this.mDisplayEnhanceConfig = DisplayEnhanceIrisConfig.getInstance(context);
        }
        this.mHandler = new DisplayEnhanceHandler(FgThread.get().getLooper());
        this.mBinderService = new BinderService();
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.color.displayenhance.VivoDisplayEnhanceManagerService.1
            @Override // java.lang.Runnable
            public void run() {
                VivoDisplayEnhanceManagerService.this.initSettings();
            }
        });
        ApplicationPackageObserver applicationPackageObserver = ApplicationPackageObserver.getInstance(context);
        this.mAppObserver = applicationPackageObserver;
        applicationPackageObserver.registerListener(this);
    }

    public static VivoDisplayEnhanceManagerService getInstance(VivoColorManagerService colorManager, Context context) {
        if (mVivoDisplayEnhanceManager == null) {
            synchronized (VivoDisplayEnhanceManagerService.class) {
                if (mVivoDisplayEnhanceManager == null) {
                    mVivoDisplayEnhanceManager = new VivoDisplayEnhanceManagerService(colorManager, context);
                }
            }
        }
        return mVivoDisplayEnhanceManager;
    }

    public void setUp(int userHandle) {
        this.mHawkeyeController.setUp(userHandle);
    }

    public void tearDown() {
        this.mHawkeyeController.tearDown();
    }

    public void onStartUser(int userHandle) {
        this.mHawkeyeController.onStartUser(userHandle);
    }

    private boolean isIrisVideoMemcAvailable() {
        if (FtFeature.isFeatureSupport("vivo.hardware.video.memc") && "iris".equals(FtFeature.getFeatureAttribute("vivo.hardware.video.memc", "version", "iris"))) {
            return true;
        }
        return false;
    }

    private boolean isIrisVideoSdr2HdrAvailable() {
        if (FtFeature.isFeatureSupport("vivo.hardware.video.sdr2hdr") && "iris".equals(FtFeature.getFeatureAttribute("vivo.hardware.video.sdr2hdr", "version", "iris"))) {
            return true;
        }
        return false;
    }

    private boolean isIrisGameMemcAvailable() {
        if (FtFeature.isFeatureSupport("vivo.hardware.game.memc") && "iris".equals(FtFeature.getFeatureAttribute("vivo.hardware.game.memc", "version", "iris"))) {
            return true;
        }
        return false;
    }

    private boolean isIrisGameSdr2HdrAvailable() {
        if (FtFeature.isFeatureSupport("vivo.hardware.game.sdr2hdr") && "iris".equals(FtFeature.getFeatureAttribute("vivo.hardware.game.sdr2hdr", "version", "iris"))) {
            return true;
        }
        return false;
    }

    private boolean isNovaVideoMemcAvailable() {
        if (FtFeature.isFeatureSupport("vivo.hardware.video.memc") && "nova".equals(FtFeature.getFeatureAttribute("vivo.hardware.video.memc", "version", "iris"))) {
            return true;
        }
        return false;
    }

    private boolean isNovaVideoSdr2HdrAvailable() {
        if (FtFeature.isFeatureSupport("vivo.hardware.video.sdr2hdr") && "nova".equals(FtFeature.getFeatureAttribute("vivo.hardware.video.sdr2hdr", "version", "iris"))) {
            return true;
        }
        return false;
    }

    private boolean isNovaGameMemcAvailable() {
        if (FtFeature.isFeatureSupport("vivo.hardware.game.memc") && "nova".equals(FtFeature.getFeatureAttribute("vivo.hardware.game.memc", "version", "iris"))) {
            return true;
        }
        return false;
    }

    private boolean isNovaGameSdr2HdrAvailable() {
        if (FtFeature.isFeatureSupport("vivo.hardware.game.sdr2hdr") && "nova".equals(FtFeature.getFeatureAttribute("vivo.hardware.game.sdr2hdr", "version", "iris"))) {
            return true;
        }
        return false;
    }

    /* loaded from: classes.dex */
    private final class DisplayEnhanceHandler extends Handler {
        public DisplayEnhanceHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 2) {
                VivoDisplayEnhanceManagerService.this.scheduleHighTempCheck();
            }
        }
    }

    public void onScreenStatusChanged(Context context, Intent intent) {
        KeyguardManager keyguard = (KeyguardManager) context.getSystemService("keyguard");
        String action = intent.getAction();
        int screenState = -1;
        if ("android.intent.action.SCREEN_OFF".equals(action)) {
            if (keyguard != null && keyguard.isKeyguardLocked()) {
                screenState = SCREEN_OFF;
                this.mScreenState = screenState;
                scheduleHighTempCheck();
            }
        } else if ("android.intent.action.USER_PRESENT".equals(action) && keyguard != null && !keyguard.isKeyguardLocked()) {
            screenState = USER_PRESENT;
            this.mScreenState = screenState;
            scheduleHighTempCheck();
        }
        if (screenState != -1) {
            synchronized (mDisplayEnhanceListeners) {
                for (int i = mDisplayEnhanceListeners.size() - 1; i >= 0; i--) {
                    DisplayEnhanceListener listener = mDisplayEnhanceListeners.get(i);
                    listener.onScreenStateChanged(screenState);
                }
            }
        }
    }

    public static void registerDisplayEnhanceListener(DisplayEnhanceListener listener) {
        synchronized (mDisplayEnhanceListeners) {
            if (!mDisplayEnhanceListeners.contains(listener)) {
                mDisplayEnhanceListeners.add(listener);
                VSlog.d(TAG, "registerDisplayEnhanceListener:  " + listener);
            }
        }
    }

    public static void unregisterDisplayEnhanceListener(DisplayEnhanceListener listener) {
        synchronized (mDisplayEnhanceListeners) {
            if (mDisplayEnhanceListeners.contains(listener)) {
                mDisplayEnhanceListeners.remove(listener);
                VSlog.d(TAG, "unregisterDisplayEnhanceListener:  " + listener);
            }
        }
    }

    public static void registerPowerTempListener(PowerTemperatureListener listener) {
        synchronized (mPowerTempListeners) {
            if (!mPowerTempListeners.contains(listener)) {
                mPowerTempListeners.add(listener);
                VSlog.d(TAG, "registerPowerTempListener:  " + listener);
            }
        }
    }

    public static void unregisterPowerTempListener(PowerTemperatureListener listener) {
        synchronized (mPowerTempListeners) {
            if (mPowerTempListeners.contains(listener)) {
                mPowerTempListeners.remove(listener);
                VSlog.d(TAG, "unregisterPowerTempListener:  " + listener);
            }
        }
    }

    @Override // com.android.server.display.color.displayenhance.PackageActivityListener
    public void onForegroundPackageChanged(String name) {
        synchronized (mDisplayEnhanceListeners) {
            for (int i = mDisplayEnhanceListeners.size() - 1; i >= 0; i--) {
                DisplayEnhanceListener listener = mDisplayEnhanceListeners.get(i);
                listener.onForegroundPackageChanged(name);
            }
        }
    }

    @Override // com.android.server.display.color.displayenhance.PackageActivityListener
    public void onForegroundActivityChanged(String name, int state) {
        synchronized (mDisplayEnhanceListeners) {
            for (int i = mDisplayEnhanceListeners.size() - 1; i >= 0; i--) {
                DisplayEnhanceListener listener = mDisplayEnhanceListeners.get(i);
                listener.onForegroundActivityChanged(name, state);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isIrisAvailable() {
        if (isIrisVideoSdr2HdrAvailable() || isIrisVideoMemcAvailable() || isIrisGameMemcAvailable() || isIrisGameSdr2HdrAvailable()) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initSettings() {
        if ("vos".equals(FtBuild.getOsName())) {
            DisplayEnhanceReceiver displayEnhanceReceiver = new DisplayEnhanceReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
            this.mContext.registerReceiver(displayEnhanceReceiver, filter);
        } else {
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("power_save_type"), false, this.mLowPowerObserver, -1);
            int lowPowerLevel = Settings.System.getIntForUser(this.mContext.getContentResolver(), "power_save_type", 0, -2);
            if (lowPowerLevel == 2) {
                this.mLowPowerObserver.onChange(false, null);
            }
        }
        this.mHandler.sendEmptyMessage(2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DisplayEnhanceReceiver extends BroadcastReceiver {
        private DisplayEnhanceReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.os.action.POWER_SAVE_MODE_CHANGED".equals(action)) {
                try {
                    PowerManager pm = (PowerManager) VivoDisplayEnhanceManagerService.this.mContext.getSystemService("power");
                    boolean isPowerSaveMode = pm.isPowerSaveMode();
                    VSlog.d(VivoDisplayEnhanceManagerService.TAG, "onReceive: now is in save mode " + pm.isPowerSaveMode());
                    int lowPowerLevel = isPowerSaveMode ? 2 : 0;
                    VivoDisplayEnhanceManagerService.this.updatePowerState(lowPowerLevel);
                } catch (Exception e) {
                    VSlog.e(VivoDisplayEnhanceManagerService.TAG, " exception = " + e.getMessage());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePowerState(int state) {
        boolean mainSwitchChange = false;
        synchronized (mPowerTempListeners) {
            for (int i = mPowerTempListeners.size() - 1; i >= 0; i--) {
                PowerTemperatureListener listener = mPowerTempListeners.get(i);
                mainSwitchChange |= listener.onPowerStateChanged(state);
            }
        }
        if (mainSwitchChange) {
            this.mHandler.sendEmptyMessage(2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getCurrentTemperature() {
        if (!mHightTempTest) {
            int temperature = FtDeviceInfo.getBoardTempure();
            return temperature;
        }
        int temperature2 = Settings.Global.getInt(this.mContext.getContentResolver(), "setting_for_tempure_limit", 28);
        return temperature2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkIsHighTemperature() {
        int temperature = getCurrentTemperature();
        if (temperature >= TEMPERATURE_MAX) {
            VSlog.d(TAG, "checkIsHighTemp: temperature is to high");
            this.isHighTemp = true;
            return true;
        }
        return false;
    }

    private boolean hasDisplayEnhanceOpen() {
        Sdr2HdrSwDisplayEnhanceController sdr2HdrSwDisplayEnhanceController;
        Sdr2HdrHwDisplayEnhanceController sdr2HdrHwDisplayEnhanceController;
        MemcDisplayEnhanceController memcDisplayEnhanceController = this.mMemcController;
        return (memcDisplayEnhanceController != null && memcDisplayEnhanceController.getUserSwitchSetting() == 1) || ((sdr2HdrSwDisplayEnhanceController = this.mSdr2HdrSwController) != null && sdr2HdrSwDisplayEnhanceController.getUserSwitchSetting() == 1) || ((sdr2HdrHwDisplayEnhanceController = this.mSdr2HdrHwController) != null && sdr2HdrHwDisplayEnhanceController.getUserSwitchSetting() == 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleHighTempCheck() {
        if (hasDisplayEnhanceOpen() && this.mScreenState == USER_PRESENT) {
            if (this.mHandler.hasCallbacks(this.mCheckTempRunnable)) {
                this.mHandler.removeCallbacks(this.mCheckTempRunnable);
            }
            this.mHandler.post(this.mCheckTempRunnable);
        } else if ((!this.isHighTemp || this.mScreenState == SCREEN_OFF) && this.mHandler.hasCallbacks(this.mCheckTempRunnable)) {
            this.mHandler.removeCallbacks(this.mCheckTempRunnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public DisplayEnhanceController getDisplayEnhanceController(int enhanceType) {
        if (enhanceType == 0) {
            DisplayEnhanceController controller = this.mMemcController;
            return controller;
        } else if (enhanceType == 1) {
            DisplayEnhanceController controller2 = this.mSdr2HdrHwController;
            return controller2;
        } else if (enhanceType != 2) {
            return null;
        } else {
            DisplayEnhanceController controller3 = this.mSdr2HdrSwController;
            return controller3;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(PrintWriter pw) {
        pw.println("VIVO DISPLAY ENHANCE MANAGER dumpsys (vivo_display_enhance)");
        synchronized (mDisplayEnhanceListeners) {
            for (int i = mDisplayEnhanceListeners.size() - 1; i >= 0; i--) {
                DisplayEnhanceListener listener = mDisplayEnhanceListeners.get(i);
                listener.dump(pw);
            }
        }
    }

    /* loaded from: classes.dex */
    public class BinderService extends IVivoDisplayEnhanceManager.Stub {
        public static final String serviceName = "vivo_display_enhance";

        public BinderService() {
        }

        public void setDisplayEnhanceState(int type, int state) {
            long token = Binder.clearCallingIdentity();
            try {
                DisplayEnhanceController controller = VivoDisplayEnhanceManagerService.this.getDisplayEnhanceController(type);
                if (controller == null) {
                    VSlog.e(VivoDisplayEnhanceManagerService.TAG, "setDisplayEnhanceState fail, type=" + type);
                    return;
                }
                VSlog.d(VivoDisplayEnhanceManagerService.TAG, "setDisplayEnhanceState: type=" + type + ", state=" + state);
                if (!controller.isMainSwitchOpen() && state == 1 && VivoDisplayEnhanceManagerService.this.checkIsHighTemperature()) {
                    return;
                }
                controller.setDisplayEnhanceState(state);
                VivoDisplayEnhanceManagerService.this.mHandler.sendEmptyMessage(2);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getDisplayEnhanceState(int type) {
            long token = Binder.clearCallingIdentity();
            try {
                DisplayEnhanceController controller = VivoDisplayEnhanceManagerService.this.getDisplayEnhanceController(type);
                if (controller == null) {
                    VSlog.e(VivoDisplayEnhanceManagerService.TAG, "getDisplayEnhanceState fail, type=" + type);
                    return 0;
                }
                return controller.getDisplayEnhanceState();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public List<String> getSupportAppList(int type) {
            long token = Binder.clearCallingIdentity();
            try {
                DisplayEnhanceController controller = VivoDisplayEnhanceManagerService.this.getDisplayEnhanceController(type);
                if (controller == null) {
                    VSlog.e(VivoDisplayEnhanceManagerService.TAG, "getSupportAppList fail, type=" + type);
                    return null;
                }
                return controller.getSupportAppList();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getPackageSettingState(int type, String name) {
            long token = Binder.clearCallingIdentity();
            try {
                DisplayEnhanceController controller = VivoDisplayEnhanceManagerService.this.getDisplayEnhanceController(type);
                if (controller == null) {
                    VSlog.e(VivoDisplayEnhanceManagerService.TAG, "getPackageSettingState fail, type=" + type);
                    return 0;
                }
                return controller.getPackageSettingState(name);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setPackageSettingState(int type, String name, int state) {
            long token = Binder.clearCallingIdentity();
            try {
                DisplayEnhanceController controller = VivoDisplayEnhanceManagerService.this.getDisplayEnhanceController(type);
                if (controller == null) {
                    VSlog.e(VivoDisplayEnhanceManagerService.TAG, "setPackageSettingState fail, type=" + type);
                    return;
                }
                controller.setPackageSettingState(name, state);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int setDisplayEnhanceConfigure(int type, int[] values, int count) {
            long token = Binder.clearCallingIdentity();
            try {
                if (VivoDisplayEnhanceManagerService.this.isIrisAvailable()) {
                    return VivoDisplayEnhanceManagerService.this.mDisplayEnhanceConfig.irisConfigureSet(type, values, count);
                }
                VSlog.e(VivoDisplayEnhanceManagerService.TAG, "Not support IIris");
                return -1;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getDisplayEnhanceConfigure(int type, int[] values, int count) {
            long token = Binder.clearCallingIdentity();
            try {
                if (!VivoDisplayEnhanceManagerService.this.isIrisAvailable()) {
                    return VivoDisplayEnhanceManagerService.this.mDisplayEnhanceConfig.irisConfigureGet(type, values, count);
                }
                VSlog.e(VivoDisplayEnhanceManagerService.TAG, "Not support IIris");
                return -1;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(VivoDisplayEnhanceManagerService.this.mContext, VivoDisplayEnhanceManagerService.TAG, pw)) {
                return;
            }
            long token = Binder.clearCallingIdentity();
            try {
                VivoDisplayEnhanceManagerService.this.dumpInternal(pw);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }
}