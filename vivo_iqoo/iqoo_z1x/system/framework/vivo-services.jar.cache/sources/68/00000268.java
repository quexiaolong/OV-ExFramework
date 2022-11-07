package com.android.server.lights;

import android.content.Context;
import com.android.server.display.color.VivoLightColorMatrixControl;
import com.android.server.lights.LightsService;
import com.android.server.power.PowerDataReport;
import com.vivo.sensor.implement.SensorConfig;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLightImplImpl implements IVivoLightImpl {
    static final String TAG = "VivoLightsService";
    private Context mContext;
    private LightsService.LightImpl mLightImpl;
    private PowerDataReport mPowerDataReport;
    private VivoLightColorMatrixControl mVivoLmControl = null;

    public VivoLightImplImpl(Context context, LightsService.LightImpl lightImpl) {
        this.mContext = null;
        this.mLightImpl = lightImpl;
        this.mContext = context;
    }

    public void setHardwareBrightness(int display, float bright, int brightnessMode) {
        float brightness = bright > 0.0f ? bright : 0.0f;
        int HardwareBrightness = SensorConfig.float2LcmBrightnessAfterDPC(bright);
        int color = 65535 & HardwareBrightness;
        if (this.mVivoLmControl == null) {
            this.mVivoLmControl = VivoLightColorMatrixControl.getExistInstance();
        }
        VivoLightColorMatrixControl vivoLightColorMatrixControl = this.mVivoLmControl;
        if (vivoLightColorMatrixControl != null) {
            vivoLightColorMatrixControl.onBrightnessChanged(display, color);
        }
        VSlog.w(TAG, "algo brightness " + bright + " brightness=" + brightness + "; setBrightness=" + color);
        try {
            this.mLightImpl.setLightLocked(color, 0, 0, 0, brightnessMode);
        } catch (Exception e) {
            VSlog.w(TAG, "Failed setLightLocked.");
        }
    }

    public void sendPowerReport(int color, int lastColor) {
        if (this.mPowerDataReport == null) {
            this.mPowerDataReport = PowerDataReport.getInstance();
        }
        if (color > 0 && lastColor == 0) {
            this.mPowerDataReport.sendPowerReport();
        }
        this.mPowerDataReport.clearReasonOfScreenOn();
    }
}