package com.android.server.display;

/* loaded from: classes.dex */
public interface IVivoRampAnimator {
    Runnable getAnimationCallback();

    float getPemBrightness(float f);

    void needStopRampAnimator(boolean z);

    void notifyGlobalTargetChanged(float f);

    void postVivoAnimationCallback(float f);

    void setAnimating(boolean z);

    void setChangeTime(float f);

    void setPemBrightnessScale(float f);

    void syncBrightnessSettings(float f);
}