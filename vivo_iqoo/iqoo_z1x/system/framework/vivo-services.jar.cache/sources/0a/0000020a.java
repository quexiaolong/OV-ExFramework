package com.android.server.display.color;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.FtBuild;
import android.os.IBinder;
import android.os.ServiceManager;
import com.android.server.display.color.VivoColorManagerService;
import com.android.server.display.color.VivoLcmSre;
import com.android.server.display.color.displayenhance.VivoDisplayEnhanceManagerService;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoColorDisplayServiceImpl implements IVivoColorDisplayService {
    static final String TAG = "VivoColorDisplayServiceImpl";
    private VivoAutoColorTempController mAutoColorTempController;
    private final Context mContext;
    private VivoLcmEsd mEsd;
    private ExynosDisplaySolutionManagerService mExynosDisplay;
    private VivoLightColorMatrixControl mLightColorMatrixControl;
    private boolean mScreenOn = true;
    private ScreenStatusReceiver mScreenStatusReceiver;
    private VivoLcmSre mSre;
    public VivoColorManagerService mVivoColorManagerService;
    private VivoDisplayEnhanceManagerService mVivoDisplayEnhanceService;

    public VivoColorDisplayServiceImpl(ColorDisplayService colorDisplayService, Context context) {
        this.mVivoColorManagerService = null;
        this.mVivoDisplayEnhanceService = null;
        this.mSre = null;
        this.mEsd = null;
        this.mExynosDisplay = null;
        this.mAutoColorTempController = null;
        this.mContext = context;
        VivoColorManagerService vivoColorManagerService = VivoColorManagerService.getInstance(colorDisplayService, context);
        this.mVivoColorManagerService = vivoColorManagerService;
        this.mVivoDisplayEnhanceService = VivoDisplayEnhanceManagerService.getInstance(vivoColorManagerService, context);
        if (FtBuild.isSamsungPlatform()) {
            this.mExynosDisplay = new ExynosDisplaySolutionManagerService(context);
        }
        registScreenStatusReceiver();
        this.mSre = new VivoLcmSre(context, VivoLcmSre.SensorType.AMBLIENT_SENSOR);
        this.mEsd = new VivoLcmEsd(context);
        this.mLightColorMatrixControl = VivoLightColorMatrixControl.getInstance(this.mVivoColorManagerService, colorDisplayService, context);
        this.mAutoColorTempController = VivoAutoColorTempController.getInstance(colorDisplayService, context);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ScreenStatusReceiver extends BroadcastReceiver {
        ScreenStatusReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                VSlog.d(VivoColorDisplayServiceImpl.TAG, "receive: " + action + " mScreenOn:" + VivoColorDisplayServiceImpl.this.mScreenOn);
                if (!VivoColorDisplayServiceImpl.this.mScreenOn) {
                    VivoColorDisplayServiceImpl.this.mScreenOn = true;
                    if (VivoColorDisplayServiceImpl.this.mEsd != null) {
                        VivoColorDisplayServiceImpl.this.mEsd.onPowerOn();
                    }
                    if (VivoColorDisplayServiceImpl.this.mSre != null) {
                        VivoColorDisplayServiceImpl.this.mSre.onPowerOn();
                    }
                    if (VivoColorDisplayServiceImpl.this.mLightColorMatrixControl != null) {
                        VivoColorDisplayServiceImpl.this.mLightColorMatrixControl.onPowerOn();
                    }
                    VSlog.d(VivoColorDisplayServiceImpl.TAG, "receive: mScreenOn=" + VivoColorDisplayServiceImpl.this.mScreenOn);
                }
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                VSlog.d(VivoColorDisplayServiceImpl.TAG, "receive: " + action + " mScreenOn:" + VivoColorDisplayServiceImpl.this.mScreenOn);
                if (VivoColorDisplayServiceImpl.this.mScreenOn) {
                    VivoColorDisplayServiceImpl.this.mScreenOn = false;
                    if (VivoColorDisplayServiceImpl.this.mEsd != null) {
                        VivoColorDisplayServiceImpl.this.mEsd.onPowerOff();
                    }
                    if (VivoColorDisplayServiceImpl.this.mSre != null) {
                        VivoColorDisplayServiceImpl.this.mSre.onPowerOff();
                    }
                    if (VivoColorDisplayServiceImpl.this.mLightColorMatrixControl != null) {
                        VivoColorDisplayServiceImpl.this.mLightColorMatrixControl.onPowerOff();
                    }
                    VSlog.d(VivoColorDisplayServiceImpl.TAG, "receive: mScreenOn=" + VivoColorDisplayServiceImpl.this.mScreenOn);
                }
            }
            VivoColorDisplayServiceImpl.this.mVivoDisplayEnhanceService.onScreenStatusChanged(context, intent);
            if (VivoColorDisplayServiceImpl.this.mExynosDisplay != null) {
                VivoColorDisplayServiceImpl.this.mExynosDisplay.onScreenStatusChanged(context, intent);
            }
        }
    }

    private void registScreenStatusReceiver() {
        this.mScreenStatusReceiver = new ScreenStatusReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        this.mContext.registerReceiver(this.mScreenStatusReceiver, intentFilter);
    }

    private void unregistScreenStatusReceiver() {
        ScreenStatusReceiver screenStatusReceiver = this.mScreenStatusReceiver;
        if (screenStatusReceiver != null) {
            this.mContext.unregisterReceiver(screenStatusReceiver);
        }
    }

    private void registerBinderService(String name, IBinder service) {
        if (service != null) {
            try {
                ServiceManager.addService(name, service);
                VSlog.d(TAG, "register " + name + " service ok");
            } catch (Throwable e) {
                VSlog.e(TAG, "Failure add " + name + " service:" + e);
            }
        }
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            VivoColorManagerService.BinderService binderService = this.mVivoColorManagerService.mBinderService;
            registerBinderService(VivoColorManagerService.BinderService.serviceName, this.mVivoColorManagerService.mBinderService);
            VivoDisplayEnhanceManagerService.BinderService binderService2 = this.mVivoDisplayEnhanceService.mBinderService;
            registerBinderService(VivoDisplayEnhanceManagerService.BinderService.serviceName, this.mVivoDisplayEnhanceService.mBinderService);
            if (FtBuild.isSamsungPlatform()) {
                registerBinderService(ExynosDisplaySolutionManagerService.serviceName, this.mExynosDisplay);
            }
        }
    }

    public void setUp(int userHandle) {
        this.mVivoColorManagerService.setUp(userHandle);
        this.mVivoDisplayEnhanceService.setUp(userHandle);
        VivoLightColorMatrixControl vivoLightColorMatrixControl = this.mLightColorMatrixControl;
        if (vivoLightColorMatrixControl != null) {
            vivoLightColorMatrixControl.setUp(userHandle);
        }
        VivoAutoColorTempController vivoAutoColorTempController = this.mAutoColorTempController;
        if (vivoAutoColorTempController != null) {
            vivoAutoColorTempController.onSetUser(userHandle);
            this.mAutoColorTempController.setUp(this.mContext, false);
        }
    }

    public void tearDown() {
        this.mVivoColorManagerService.tearDown();
        this.mVivoDisplayEnhanceService.tearDown();
        VivoLightColorMatrixControl vivoLightColorMatrixControl = this.mLightColorMatrixControl;
        if (vivoLightColorMatrixControl != null) {
            vivoLightColorMatrixControl.tearDown();
        }
        VivoAutoColorTempController vivoAutoColorTempController = this.mAutoColorTempController;
        if (vivoAutoColorTempController != null) {
            vivoAutoColorTempController.tearDown();
        }
    }

    public void onStartUser(int userHandle, DisplayTransformManager dtm) {
        VSlog.d(TAG, "onStartUser: userHandle=" + userHandle);
        if (userHandle == 999) {
            return;
        }
        this.mVivoColorManagerService.onStartUser(userHandle, dtm);
        this.mVivoDisplayEnhanceService.onStartUser(userHandle);
        VivoLightColorMatrixControl vivoLightColorMatrixControl = this.mLightColorMatrixControl;
        if (vivoLightColorMatrixControl != null) {
            vivoLightColorMatrixControl.onStartUser(userHandle);
        }
        VivoAutoColorTempController vivoAutoColorTempController = this.mAutoColorTempController;
        if (vivoAutoColorTempController != null) {
            vivoAutoColorTempController.onStartUser(userHandle);
        }
    }

    public TintController getLightTintController(int level) {
        VivoLightColorMatrixControl vivoLightColorMatrixControl = this.mLightColorMatrixControl;
        if (vivoLightColorMatrixControl != null) {
            return vivoLightColorMatrixControl.getLightTintController(level);
        }
        VSlog.e(TAG, "getLightTintController: mLightColorMatrixControl is null");
        return null;
    }

    public boolean onInversionChanged() {
        VivoLightColorMatrixControl vivoLightColorMatrixControl = this.mLightColorMatrixControl;
        if (vivoLightColorMatrixControl != null) {
            return vivoLightColorMatrixControl.onInversionChanged();
        }
        return false;
    }

    public TintController getAutoColorTempTintController() {
        return this.mAutoColorTempController;
    }

    public boolean setColorMode(int colorMode) {
        return this.mVivoColorManagerService.setColorMode(colorMode);
    }

    public int getColorMode() {
        return this.mVivoColorManagerService.getColorMode();
    }

    public void setNightDisplayColorMatrix(int cct) {
        this.mVivoColorManagerService.setNightDisplayColorMatrix(cct);
    }

    public float[] getNightDisplayColorMatrix() {
        return this.mVivoColorManagerService.getNightDisplayColorMatrix();
    }

    public int getColorTemperature() {
        return this.mVivoColorManagerService.getColorTemperature();
    }

    public boolean setColorTemperature(int temperature) {
        return this.mVivoColorManagerService.setColorTemperature(temperature);
    }

    public void setNightDisplayNotificationEnable(Boolean enable) {
        this.mVivoColorManagerService.setNightDisplayNotificationEnable(enable);
    }
}