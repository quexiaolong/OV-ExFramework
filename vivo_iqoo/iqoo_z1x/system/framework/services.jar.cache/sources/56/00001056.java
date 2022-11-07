package com.android.server.lights;

/* loaded from: classes.dex */
public abstract class LogicalLight {
    public static final int BRIGHTNESS_MODE_LOW_PERSISTENCE = 2;
    public static final int BRIGHTNESS_MODE_SENSOR = 1;
    public static final int BRIGHTNESS_MODE_USER = 0;
    public static final int LIGHT_FLASH_HARDWARE = 2;
    public static final int LIGHT_FLASH_NONE = 0;
    public static final int LIGHT_FLASH_TIMED = 1;

    public abstract void pulse();

    public abstract void pulse(int i, int i2);

    public abstract void setBrightness(float f);

    public abstract void setBrightness(float f, int i);

    public abstract void setBrightnessForFingerprintCalibration(float f);

    public abstract void setColor(int i);

    public abstract void setFingerprintCalibrationState(boolean z);

    public abstract void setFlashing(int i, int i2, int i3, int i4);

    public abstract void setVrMode(boolean z);

    public abstract void turnOff();
}