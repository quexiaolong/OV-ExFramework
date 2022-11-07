package com.android.server.display.color;

/* loaded from: classes.dex */
public interface IVivoColorDisplayService {
    TintController getAutoColorTempTintController();

    int getColorMode();

    int getColorTemperature();

    TintController getLightTintController(int i);

    float[] getNightDisplayColorMatrix();

    void onBootPhase(int i);

    boolean onInversionChanged();

    void onStartUser(int i, DisplayTransformManager displayTransformManager);

    boolean setColorMode(int i);

    boolean setColorTemperature(int i);

    void setNightDisplayColorMatrix(int i);

    void setNightDisplayNotificationEnable(Boolean bool);

    void setUp(int i);

    void tearDown();
}