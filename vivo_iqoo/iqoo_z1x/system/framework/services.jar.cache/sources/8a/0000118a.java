package com.android.server.locksettings;

import android.app.admin.DevicePolicyManager;
import android.os.Handler;

/* loaded from: classes.dex */
public interface IVivoLockSettingsStrongAuth {
    long getBBKNonStrongBiometricTimeout(long j, int i);

    long getBBKStrongAuthTimeout(DevicePolicyManager devicePolicyManager, int i);

    void handleScheduleConvenienceLevelStrongAuthTimeout(int i, Handler handler);

    void notifyStrongAuthTrackers(int i, int i2);

    void requireStrongAuth(int i, int i2);
}