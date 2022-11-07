package com.android.server.devicepolicy;

import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class BaseIDevicePolicyManager extends IDevicePolicyManager.Stub {
    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void handleStartUser(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void handleStopUser(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void handleUnlockUser(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void systemReady(int i);

    public void clearSystemUpdatePolicyFreezePeriodRecord() {
    }

    public boolean setKeyGrantForApp(ComponentName admin, String callerPackage, String alias, String packageName, boolean hasGrant) {
        return false;
    }

    public void setLocationEnabled(ComponentName who, boolean locationEnabled) {
    }

    public boolean isOrganizationOwnedDeviceWithManagedProfile() {
        return false;
    }

    public int getPersonalAppsSuspendedReasons(ComponentName admin) {
        return 0;
    }

    public void setPersonalAppsSuspended(ComponentName admin, boolean suspended) {
    }

    public void setManagedProfileMaximumTimeOff(ComponentName admin, long timeoutMs) {
    }

    public long getManagedProfileMaximumTimeOff(ComponentName admin) {
        return 0L;
    }

    public boolean canProfileOwnerResetPasswordWhenLocked(int userId) {
        return false;
    }
}