package com.android.server.display;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.biometrics.fingerprint.FingerprintKeyguardInternal;
import android.hardware.biometrics.fingerprint.FingerprintUIManagerInternal;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import com.android.server.LocalServices;
import com.android.server.wm.VivoEasyShareManager;
import com.vivo.sensor.autobrightness.AutoBrightnessManagerImpl;
import com.vivo.sensor.autobrightness.ScreenBrightnessModeRestore;
import com.vivo.sensor.implement.SensorConfig;
import com.vivo.services.rms.RmsInjectorImpl;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoDisplayPowerStateImpl implements IVivoDisplayPowerState {
    private static boolean DEBUG = false;
    private static final String DOWN_BRIGHTNESS_PERCENT = "pem_down_brightness_percent";
    private static final boolean IS_ENG = Build.TYPE.equals("eng");
    private static final boolean IS_LOG_CTRL_OPEN;
    private static final String KEY_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    private static final String TAG = "VivoDisplayPowerState";
    private boolean hasRegistedBriPer;
    private AutoBrightnessManagerImpl mAutoBrightnessManagerImpl;
    private Context mContext;
    private DisplayManager mDisplayManager;
    private DisplayManagerInternal mDisplayManagerInternal;
    DisplayPowerState mDisplayPowerState;
    private FingerprintKeyguardInternal mFingerprintKeyguard;
    private FingerprintUIManagerInternal mFingerprintUIManager;
    private Handler mHandler;
    private ScreenBrightnessModeRestore mScreenBrightnessModeRestore;
    private int mFingerprintForceScreenState = 1;
    private int mFingerprintPendingForceScreenState = 1;
    private float mFirstBrightWhenCamOpt = -1.0f;
    private float mCamOptThres = -1.0f;
    private boolean isNeedKeepWhenCamOpt = true;
    private int mFirstBrightWhenAppBright = -1;
    private int mAppBrightThres = -1;
    private boolean isNeedKeepWhenAppBright = true;
    private float[] aFloatValues = {2.0f, 0.0f};
    public int mPowerAssistantMode = 0;
    private boolean mPowerAssistantModeChanged = false;
    private int mGameFrameRateMode = -1;
    private boolean mGameFrameRateModeChanged = false;
    private float downBrightnessPercent = 0.8f;
    private boolean mForceBrightnessOff = false;

    static {
        boolean equals = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
        IS_LOG_CTRL_OPEN = equals;
        DEBUG = equals || IS_ENG;
    }

    public VivoDisplayPowerStateImpl(DisplayPowerState displayPowerState, Context context, Handler handler) {
        this.mAutoBrightnessManagerImpl = null;
        this.hasRegistedBriPer = false;
        this.mContext = context;
        this.mHandler = handler;
        this.mDisplayPowerState = displayPowerState;
        this.mDisplayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
        checkFingerprintUI();
        checkFingerprintKeyguard();
        this.mScreenBrightnessModeRestore = ScreenBrightnessModeRestore.getInstance(this.mContext, this.mHandler.getLooper());
        this.mAutoBrightnessManagerImpl = AutoBrightnessManagerImpl.getInstance(this.mContext);
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        if (!this.hasRegistedBriPer) {
            registerObserver();
            this.hasRegistedBriPer = true;
        }
    }

    private void registerObserver() {
        ContentResolver mContentResolver = this.mContext.getContentResolver();
        if (mContentResolver == null) {
            VSlog.d(TAG, "register BrightPercentObserver fail");
            return;
        }
        BrightPercentObserver mBrightPercentObserver = new BrightPercentObserver(this.mHandler);
        mContentResolver.registerContentObserver(Settings.System.getUriFor(DOWN_BRIGHTNESS_PERCENT), true, mBrightPercentObserver);
        VSlog.d(TAG, "register BrightPercentObserver success");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getDownBrightPercent() {
        return Settings.System.getFloat(this.mContext.getContentResolver(), DOWN_BRIGHTNESS_PERCENT, this.downBrightnessPercent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BrightPercentObserver extends ContentObserver {
        public BrightPercentObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            float newData = VivoDisplayPowerStateImpl.this.getDownBrightPercent();
            VSlog.d(VivoDisplayPowerStateImpl.TAG, "down brightness percent, old:" + VivoDisplayPowerStateImpl.this.downBrightnessPercent + ", new:" + newData);
            VivoDisplayPowerStateImpl.this.downBrightnessPercent = newData;
        }
    }

    private FingerprintUIManagerInternal checkFingerprintUI() {
        if (this.mFingerprintUIManager == null) {
            this.mFingerprintUIManager = (FingerprintUIManagerInternal) LocalServices.getService(FingerprintUIManagerInternal.class);
        }
        return this.mFingerprintUIManager;
    }

    private FingerprintKeyguardInternal checkFingerprintKeyguard() {
        if (this.mFingerprintKeyguard == null) {
            this.mFingerprintKeyguard = (FingerprintKeyguardInternal) LocalServices.getService(FingerprintKeyguardInternal.class);
        }
        return this.mFingerprintKeyguard;
    }

    public boolean isNeedBlockBrightness() {
        FingerprintKeyguardInternal fk = checkFingerprintKeyguard();
        return fk != null && fk.isNeedBlockBrightness();
    }

    public void setForceDisplayStateIfNeed(int actualState) {
        if (actualState == 1) {
            int i = this.mFingerprintForceScreenState;
            int i2 = this.mFingerprintPendingForceScreenState;
            if (i != i2 && this.mDisplayManager != null) {
                this.mFingerprintForceScreenState = i2;
                if (i2 == 2) {
                    VSlog.d(TAG, "forcedisplay true");
                    this.mDisplayManager.setForceDisplayStateOn(0, true, "fingerprint");
                } else if (i2 == 1) {
                    VSlog.d(TAG, "forcedisplay false");
                    this.mDisplayManager.setForceDisplayStateOn(0, false, "fingerprint");
                } else {
                    VSlog.d(TAG, "forcedisplay invalid, " + this.mFingerprintForceScreenState);
                }
            }
        }
    }

    public void updateForceScreenState(int actualState) {
        this.mFingerprintForceScreenState = actualState;
        this.mFingerprintPendingForceScreenState = actualState;
    }

    public void setForceDisplayState(Object lock, int actualState, int pendingState) {
        synchronized (lock) {
            if (actualState == 1) {
                this.mFingerprintPendingForceScreenState = pendingState;
                VSlog.d(TAG, "setForceDisplayState state=" + pendingState);
                lock.notifyAll();
                return;
            }
            VSlog.d(TAG, "setForceDisplayState failed, act=" + actualState + " state=" + pendingState);
        }
    }

    public void onSetScreenBrightness(int state, float brightnessState) {
        checkFingerprintUI();
    }

    public void setDebug() {
        DisplayPowerState.DEBUG = DEBUG;
    }

    public void setIsScreenOnAnimation(boolean screenOnAnimation) {
        if (this.mDisplayPowerState.mColorFade != null) {
            this.mDisplayPowerState.mColorFade.setIsScreenOnAnimation(screenOnAnimation);
        }
    }

    public void setColorFadeStyle(int colorFadeStyle) {
        if (this.mDisplayPowerState.mColorFade != null) {
            this.mDisplayPowerState.mColorFade.setColorFadeStyle(colorFadeStyle);
        }
    }

    public void setOffReason(int offReason) {
        if (this.mDisplayPowerState.mColorFade != null) {
            this.mDisplayPowerState.mColorFade.setOffReason(offReason);
        }
    }

    public int judgeBrightness(int screenState, float colorFadeLevel, int screenBrightness) {
        if (this.mDisplayPowerState.mScreenState == 1 || this.mDisplayPowerState.mColorFadeLevel <= 0.05f) {
            return 0;
        }
        int brightness = (int) this.mDisplayPowerState.mScreenBrightness;
        return brightness;
    }

    public void setWakeReason(String reason) {
        if (this.mDisplayPowerState.mColorFade != null) {
            this.mDisplayPowerState.mColorFade.setWakeReason(reason);
        }
    }

    public void setDynamicEffectsOn(boolean dynamicEffectsOn) {
        if (this.mDisplayPowerState.mColorFade != null) {
            this.mDisplayPowerState.mColorFade.setDynamicEffectsOn(dynamicEffectsOn);
        }
    }

    public void dummy() {
    }

    public void onScreenBrightnessChanged(float screenBrightness, int screenState) {
        int brightness = SensorConfig.algoMapTo255br(SensorConfig.lcm2AlgoBrightness(SensorConfig.float2LcmBrightnessAfterDPC(screenBrightness)));
        if (brightness <= 0) {
            brightness = 0;
        }
        RmsInjectorImpl.getInstance().onScreenBrightnessChanged(brightness, screenState);
    }

    /* JADX WARN: Removed duplicated region for block: B:52:0x00cf  */
    /* JADX WARN: Removed duplicated region for block: B:59:0x00fd  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public float setBrightAccordingPowerAssistantMode(float r11, boolean r12, int r13) {
        /*
            Method dump skipped, instructions count: 402
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.VivoDisplayPowerStateImpl.setBrightAccordingPowerAssistantMode(float, boolean, int):float");
    }

    public void notifyScreenBrightness(float backlightState) {
    }

    public void onPowerAssistantModeChanged(int mode) {
        if (mode != this.mPowerAssistantMode || mode == 3) {
            if (SensorConfig.isBbkLog()) {
                VSlog.d(TAG, "onPowerAssistantModeChanged change mode from " + this.mPowerAssistantMode + " to " + mode);
            }
            this.mPowerAssistantMode = mode;
            if (mode != 2) {
                this.mFirstBrightWhenCamOpt = -1.0f;
                this.isNeedKeepWhenCamOpt = true;
                this.mCamOptThres = -1.0f;
            }
            if (this.mPowerAssistantMode != 3) {
                this.mFirstBrightWhenAppBright = -1;
                this.isNeedKeepWhenAppBright = true;
                this.mAppBrightThres = -1;
            }
        }
    }

    public void onPowerAssistantModeChangedNotify() {
        this.mPowerAssistantModeChanged = true;
        if (SensorConfig.isBbkLog()) {
            VSlog.d(TAG, "onPowerAssistantModeChangedNotify set mPowerAssistantModeChanged=true");
        }
    }

    public boolean getPowerAssistantModeChanged() {
        return this.mPowerAssistantModeChanged;
    }

    public void setForceDisplayBrightnessOff(boolean brightnessOff, String reason) {
        DisplayManagerInternal displayManagerInternal;
        VSlog.d(TAG, "brightnessOff set to " + brightnessOff + " because of " + reason);
        this.mForceBrightnessOff = brightnessOff;
        boolean bInvalidStatus = true;
        if (this.mDisplayPowerState.mScreenState != 1) {
            this.mDisplayPowerState.mScreenReady = false;
            this.mDisplayPowerState.scheduleScreenUpdate();
        }
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (!brightnessOff || this.mDisplayPowerState.mScreenState != 1) {
            bInvalidStatus = false;
        }
        if (flinger != null && (displayManagerInternal = this.mDisplayManagerInternal) != null && !bInvalidStatus) {
            long physicalDeviceId = displayManagerInternal.getPhysicalDeviceId(0);
            VSlog.d(TAG, "setForceDisplayBrightnessOff: physical device id=" + physicalDeviceId);
            if (physicalDeviceId != -1) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeLong(physicalDeviceId);
                data.writeInt(brightnessOff ? 0 : 2);
                try {
                    try {
                        flinger.transact(20000, data, null, 0);
                        VSlog.w(TAG, "setForceDisplayBrightnessOff: execute flinger 20000");
                    } catch (Exception e) {
                        VSlog.e(TAG, "setForceDisplayBrightnessOff: transact data error", e);
                    }
                } finally {
                    data.recycle();
                }
            }
        }
        VivoEasyShareManager.getInstance().notifyForceBrightnessOffStateChanged(this.mForceBrightnessOff);
    }

    public boolean isForceBrightnessOff() {
        return this.mForceBrightnessOff;
    }

    public int getForceOffBrightness() {
        VSlog.d(TAG, "mForceBrightnessOff is true, force set brightness off");
        return 0;
    }

    public void setGameFrameRateMode(int gameFrameRateMode) {
        if (this.mGameFrameRateMode != gameFrameRateMode) {
            this.mGameFrameRateModeChanged = true;
            VSlog.d(TAG, "gameFrameRateMode changed, setGameFrameRateMode = " + gameFrameRateMode);
        }
        this.mGameFrameRateMode = gameFrameRateMode;
    }

    public boolean getGameFrameRateModeChanged() {
        return this.mGameFrameRateModeChanged;
    }
}