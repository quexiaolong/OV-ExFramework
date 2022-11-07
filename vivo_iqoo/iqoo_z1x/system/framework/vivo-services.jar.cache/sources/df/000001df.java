package com.android.server.display;

import android.hardware.biometrics.fingerprint.FingerprintUIManagerInternal;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.graphics.common.V1_0.BufferUsage;
import android.os.IBinder;
import android.os.Trace;
import android.view.Display;
import android.view.SurfaceControl;
import com.android.internal.BrightnessSynchronizer;
import com.android.server.LocalServices;
import com.android.server.display.LocalDisplayAdapter;
import com.android.server.display.VivoDisplayModuleController;
import com.vivo.fingerprint.analysis.AnalysisManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLocalDisplayDeviceImpl implements IVivoLocalDisplayDevice {
    static final String TAG = "VivoLocalDisplayDeviceImpl";
    private float mActualBrightness;
    private int mActualState;
    private DisplayManagerInternal mDisplayManagerInternal;
    private FingerprintUIManagerInternal mFingerprintUIManager;
    private LocalDisplayAdapter.LocalDisplayDevice mLocalDisplayDevice;
    private final long mPhysicalDiaplayIdMain;
    private int mLastState = -1;
    private boolean mForceStateChanged = false;

    public VivoLocalDisplayDeviceImpl(LocalDisplayAdapter.LocalDisplayDevice localDisplayDevice) {
        this.mLocalDisplayDevice = localDisplayDevice;
        long[] displayIds = SurfaceControl.getPhysicalDisplayIds();
        this.mPhysicalDiaplayIdMain = (displayIds == null || displayIds.length <= 0) ? 0L : displayIds[0];
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
    }

    public void setPowerModeForState(int state, long physicalDisplayId) {
        DisplayManagerInternal displayManagerInternal;
        IBinder token = this.mLocalDisplayDevice.getDisplayTokenLocked();
        int lastState = this.mLastState;
        if (this.mLocalDisplayDevice.mForceStateOn) {
            if (state == 2) {
                this.mLocalDisplayDevice.mForceStateOn = false;
                this.mLocalDisplayDevice.mLastForceState = false;
                VSlog.d("shuangping1025", "The real state has been changed to ON !");
            } else {
                state = 2;
            }
        }
        if (this.mLastState == state) {
            VSlog.d(TAG, "setPowerModeForState return");
            return;
        }
        this.mLastState = state;
        boolean isMainDisplay = physicalDisplayId == this.mPhysicalDiaplayIdMain;
        VSlog.d(TAG, Display.stateToString(lastState) + " setPowerModeForState:" + Display.stateToString(state) + ", isMainDisplay=" + isMainDisplay + ", " + this.mForceStateChanged);
        if (this.mForceStateChanged && isMainDisplay) {
            if (this.mDisplayManagerInternal == null) {
                this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
            }
            DisplayManagerInternal displayManagerInternal2 = this.mDisplayManagerInternal;
            if (displayManagerInternal2 != null) {
                if (state == 2 && lastState == 1) {
                    displayManagerInternal2.requestDraw(true);
                } else {
                    this.mDisplayManagerInternal.requestDraw(false);
                }
            }
        } else if (isMainDisplay && state == 1 && (displayManagerInternal = this.mDisplayManagerInternal) != null) {
            displayManagerInternal.requestDraw(false);
        }
        if (isMainDisplay && this.mActualState != state) {
            if (state == 2) {
                AnalysisManager.trace("displayInit");
            }
            onDisplayStateChangeStarted(state);
        }
        Trace.traceBegin(BufferUsage.CAMERA_OUTPUT, "setDisplayState(id=" + physicalDisplayId + ", state=" + Display.stateToString(state) + ")");
        int mode = getPowerModeForState(state);
        try {
            SurfaceControl.setDisplayPowerMode(token, mode);
            Trace.traceCounter(BufferUsage.CAMERA_OUTPUT, "DisplayPowerMode", mode);
            if (isMainDisplay && this.mActualState != state) {
                if (state == 2) {
                    AnalysisManager.trace("displayInit");
                }
                this.mActualState = state;
                onDisplayStateChangeFinished();
            }
        } finally {
            Trace.traceEnd(BufferUsage.CAMERA_OUTPUT);
        }
    }

    public Runnable whetherForceStateChanged(final int state, final boolean forceStateOn, boolean lastForceState, final long physicalDisplayId, final float brightness, final boolean forceOnNoChangeVDSS) {
        if (!this.mForceStateChanged) {
            if (state == 2 && forceStateOn) {
                VSlog.d(TAG, "The real state already been on ! Because called force_true twice.");
                this.mLocalDisplayDevice.mForceStateOn = false;
                this.mLocalDisplayDevice.mLastForceState = false;
                return null;
            }
            return null;
        }
        VSlog.d("shuangping1025", "mLastForceState = " + lastForceState + " mForceStateOn = " + forceStateOn);
        LocalDisplayAdapter.LocalDisplayDevice localDisplayDevice = this.mLocalDisplayDevice;
        localDisplayDevice.mLastForceState = localDisplayDevice.mForceStateOn;
        return new Runnable() { // from class: com.android.server.display.VivoLocalDisplayDeviceImpl.1
            @Override // java.lang.Runnable
            public void run() {
                VivoLocalDisplayDeviceImpl.this.mLocalDisplayDevice.notifyDisplayStateBC(state, brightness, physicalDisplayId, forceStateOn, forceOnNoChangeVDSS);
                VivoLocalDisplayDeviceImpl.this.setPowerModeForState(state, physicalDisplayId);
                VivoLocalDisplayDeviceImpl.this.mLocalDisplayDevice.notifyDisplayStateC(state, brightness, physicalDisplayId, forceStateOn);
            }
        };
    }

    public void setForceStateChanged(boolean forceStateOn, boolean lastForceState) {
        this.mForceStateChanged = forceStateOn != lastForceState;
    }

    static int getPowerModeForState(int state) {
        if (state != 1) {
            if (state != 6) {
                if (state != 3) {
                    return state != 4 ? 2 : 3;
                }
                return 1;
            }
            return 4;
        }
        return 0;
    }

    private FingerprintUIManagerInternal checkFingerprintUI() {
        if (this.mFingerprintUIManager == null) {
            this.mFingerprintUIManager = (FingerprintUIManagerInternal) LocalServices.getService(FingerprintUIManagerInternal.class);
        }
        return this.mFingerprintUIManager;
    }

    private void onDisplayStateChangeStarted(int state) {
        FingerprintUIManagerInternal fui = checkFingerprintUI();
        if (fui != null) {
            fui.onDisplayStateChangeStarted(state, this.mActualBrightness);
        }
    }

    private void onDisplayStateChangeFinished() {
        FingerprintUIManagerInternal fui = checkFingerprintUI();
        if (fui != null) {
            fui.onDisplayStateChangeFinished(this.mActualState, this.mActualBrightness);
        }
    }

    public void onSetBrightnessStarted(long physicalDisplayId, float brightness) {
        if (physicalDisplayId == this.mPhysicalDiaplayIdMain) {
            if (isBrightnessOff(this.mActualBrightness) != isBrightnessOff(brightness) && isBrightnessOff(this.mActualBrightness)) {
                AnalysisManager.trace(VivoDisplayModuleController.VivoDisplayModuleConfig.STR_MODULE_LIGHT);
            }
            FingerprintUIManagerInternal fui = checkFingerprintUI();
            if (fui != null) {
                fui.onDisplayStateChangeStarted(this.mActualState, brightness);
            }
        }
    }

    public void onSetBrightnessFinished(long physicalDisplayId, float brightness) {
        if (physicalDisplayId == this.mPhysicalDiaplayIdMain) {
            float f = this.mActualBrightness;
            if (f != brightness) {
                if (isBrightnessOff(f) && !isBrightnessOff(brightness)) {
                    AnalysisManager.trace(VivoDisplayModuleController.VivoDisplayModuleConfig.STR_MODULE_LIGHT);
                }
                this.mActualBrightness = brightness;
                onDisplayStateChangeFinished();
            }
        }
    }

    private boolean isBrightnessOff(float brightnessState) {
        return BrightnessSynchronizer.floatEquals(brightnessState, -1.0f) || BrightnessSynchronizer.floatEquals(brightnessState, Float.NaN) || brightnessState <= 0.0f;
    }
}