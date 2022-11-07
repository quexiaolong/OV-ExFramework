package com.android.server.display;

/* loaded from: classes.dex */
public interface IVivoDisplayPowerState {
    void dummy();

    int getForceOffBrightness();

    boolean getGameFrameRateModeChanged();

    boolean getPowerAssistantModeChanged();

    boolean isForceBrightnessOff();

    boolean isNeedBlockBrightness();

    int judgeBrightness(int i, float f, int i2);

    void notifyScreenBrightness(float f);

    void onPowerAssistantModeChanged(int i);

    void onPowerAssistantModeChangedNotify();

    void onSetScreenBrightness(int i, float f);

    float setBrightAccordingPowerAssistantMode(float f, boolean z, int i);

    void setColorFadeStyle(int i);

    void setDebug();

    void setDynamicEffectsOn(boolean z);

    void setForceDisplayBrightnessOff(boolean z, String str);

    void setForceDisplayState(Object obj, int i, int i2);

    void setForceDisplayStateIfNeed(int i);

    void setGameFrameRateMode(int i);

    void setIsScreenOnAnimation(boolean z);

    void setOffReason(int i);

    void setWakeReason(String str);

    void updateForceScreenState(int i);

    /* loaded from: classes.dex */
    public interface IVivoDisplayPowerStateExport {
        IVivoDisplayPowerState getVivoInjectInstance();

        default void dummy() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }

        default void setIsScreenOnAnimation(boolean screenOnAnimation) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setIsScreenOnAnimation(screenOnAnimation);
            }
        }

        default void setOffReason(int offReason) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setOffReason(offReason);
            }
        }

        default void setColorFadeStyle(int colorFadeStyle) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setColorFadeStyle(colorFadeStyle);
            }
        }

        default void setWakeReason(String reason) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setWakeReason(reason);
            }
        }

        default void setDynamicEffectsOn(boolean dynamicEffectsOn) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setDynamicEffectsOn(dynamicEffectsOn);
            }
        }
    }

    default void onScreenBrightnessChanged(float screenBrightness, int screenState) {
    }
}