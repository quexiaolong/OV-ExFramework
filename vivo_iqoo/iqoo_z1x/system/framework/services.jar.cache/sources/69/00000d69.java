package com.android.server.display;

import android.content.Context;

/* loaded from: classes.dex */
public interface IVivoColorFade {
    void captureScreenshotTextureAndSetViewportMod();

    void destroyScreenshotTextureMod();

    void dismissResources();

    void draw(float f);

    void drawFadedMod1();

    void drawFadedMod2();

    void drawThreeFrames(int i);

    void dummy();

    void initGLBuffers();

    void initGLShadersParms(int i);

    int initGLfShader(Context context);

    void onKeyguardLockChanged();

    void setColorFadeStyle(int i);

    void setDynamicEffectsOn(boolean z);

    void setIsScreenOnAnimation(boolean z);

    void setKeyguardHide(boolean z);

    void setOccluded(boolean z);

    void setOffReason(int i);

    void setRot(int i);

    void setWakeReason(String str);

    /* loaded from: classes.dex */
    public interface IVivoColorFadeExport {
        IVivoColorFade getVivoInjectInstance();

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

        default void setOccluded(boolean occluded) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setOccluded(occluded);
            }
        }

        default void onKeyguardLockChanged() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().onKeyguardLockChanged();
            }
        }

        default void setKeyguardHide(boolean keyguardHide) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setKeyguardHide(keyguardHide);
            }
        }

        default void setDynamicEffectsOn(boolean dynamicEffectsOn) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setDynamicEffectsOn(dynamicEffectsOn);
            }
        }
    }
}