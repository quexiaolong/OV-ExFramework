package com.android.server.connectivity;

import android.os.SystemProperties;

/* loaded from: classes.dex */
public class MockableSystemProperties {
    public String get(String key) {
        return SystemProperties.get(key);
    }

    public int getInt(String key, int def) {
        return SystemProperties.getInt(key, def);
    }

    public boolean getBoolean(String key, boolean def) {
        return SystemProperties.getBoolean(key, def);
    }

    public void set(String key, String value) {
        SystemProperties.set(key, value);
    }
}