package com.android.server;

import android.os.IBinder;
import android.os.VibrationEffect;

/* loaded from: classes.dex */
public interface IVivoVibratorService {
    void cancelVibPro(int i, String str, int i2);

    void doCancelIncreasingWaveformVibration();

    void doIncreaseWaveformVibration();

    long gameVibrateVibPro(int i, String str, int i2, boolean z, VibrationEffect vibrationEffect, IBinder iBinder, int i3);

    int getIncreasingAmplitude();

    boolean isEffectIdSupported(int i);

    boolean isSupportAmplitudeControlVibPro(int i);

    boolean isVibrateDisableWhenInCamera(String str);

    boolean isVibrateProForIME(String str, VibrationEffect vibrationEffect, boolean z, VibrationEffect vibrationEffect2, IBinder iBinder);

    void notifyAppSharePackageChanged(String str, int i);

    long ringVibPro(int i, String str, String str2, boolean z, boolean z2, boolean z3, VibrationEffect vibrationEffect, IBinder iBinder, int i2);

    void setAmplitudeVibPro(int i, int i2);

    void setIncreasingAmplitudeRange(int i);

    void setIncreasingAmplitudeVibPro(int i);

    boolean shouldBlockVibratorByAppShare(String str, int i, int i2);

    void systemToSendMessage(String str, long j, VibrationEffect.Prebaked prebaked, int i);

    void updateAppShareInputHandle(boolean z);

    long vibrateVibPro(int i, String str, int i2, long j, int i3, boolean z, VibrationEffect vibrationEffect, IBinder iBinder, int i4);

    boolean whetherToContinueThisVibrator(VibrationEffect vibrationEffect);
}