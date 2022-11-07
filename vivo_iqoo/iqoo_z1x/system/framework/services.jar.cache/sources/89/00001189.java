package com.android.server.locksettings;

/* loaded from: classes.dex */
public interface IVivoLockSettingsStorage {
    String readKeyValue(String str, String str2, int i);

    void writeKeyValue(String str, String str2, int i);
}