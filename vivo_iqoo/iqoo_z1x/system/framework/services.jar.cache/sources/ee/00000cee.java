package com.android.server.devicepolicy;

import android.app.admin.IVivoCallStateCallback;
import android.app.admin.IVivoPolicyManagerCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import java.util.List;

/* loaded from: classes.dex */
public interface IVivoCustomDpms {
    void checkCallingEmmPermission(ComponentName componentName, String str);

    int getAdminDeviceOwnerPolicy(ComponentName componentName, int i);

    int getAdminProfileOwnerPolicy(ComponentName componentName, int i);

    List<String> getCustomPkgs();

    String getCustomShortName();

    int getCustomType();

    List<String> getEmmBlackList(int i);

    List<String> getEmmDisabledList(int i);

    String getEmmFromCota();

    List<String> getEmmPackage(int i);

    String getEmmShortName();

    List<String> getExceptionInfo(int i);

    Bundle getInfoDeviceTransaction(ComponentName componentName, int i, Bundle bundle, int i2);

    List<String> getRestrictionInfoList(ComponentName componentName, int i, int i2);

    int getRestrictionPolicy(ComponentName componentName, int i, int i2);

    ComponentName getVivoAdmin(int i);

    ComponentName getVivoAdminUncheckedLocked(int i);

    boolean hasVivoActiveAdmin(int i);

    boolean invokeDeviceTransaction(ComponentName componentName, int i, Bundle bundle, int i2);

    boolean isAllowSlientInstall(int i);

    boolean isEmmAPI();

    boolean isVivoActiveAdmin(ComponentName componentName, int i);

    void onBootCompleted();

    void onBroadcastReceive(Context context, Intent intent);

    void registerCallStateCallback(IVivoCallStateCallback iVivoCallStateCallback);

    void registerPolicyCallback(IVivoPolicyManagerCallback iVivoPolicyManagerCallback);

    void removeVivoAdmin(ComponentName componentName, int i);

    void reportExceptionInfo(int i, Bundle bundle, int i2);

    void reportInfo(int i, Bundle bundle, int i2);

    void setEmmBlackList(List<String> list, boolean z, int i);

    boolean setEmmDisabledList(List<String> list, boolean z, int i);

    void setEmmPackage(String str, Bundle bundle, boolean z, int i);

    boolean setRestrictionInfoList(ComponentName componentName, int i, List<String> list, boolean z, int i2);

    boolean setRestrictionPolicy(ComponentName componentName, int i, int i2, int i3);

    boolean setVivoAdmin(ComponentName componentName, boolean z, int i);

    void unregisterCallStateCallback(IVivoCallStateCallback iVivoCallStateCallback);

    void unregisterPolicyCallback(IVivoPolicyManagerCallback iVivoPolicyManagerCallback);

    void updateProjectInfo();
}