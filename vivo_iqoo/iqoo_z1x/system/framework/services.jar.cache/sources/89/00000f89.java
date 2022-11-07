package com.android.server.inputmethod;

import android.content.ComponentName;
import android.os.Build;
import android.os.SystemProperties;

/* loaded from: classes.dex */
public class InputMethodSystemProperty {
    public static final boolean MULTI_CLIENT_IME_ENABLED;
    private static final String PROP_DEBUG_MULTI_CLIENT_IME = "persist.debug.multi_client_ime";
    private static final String PROP_PROD_MULTI_CLIENT_IME = "ro.sys.multi_client_ime";
    static final ComponentName sMultiClientImeComponentName;

    private static ComponentName getMultiClientImeComponentName() {
        ComponentName debugIme;
        if (Build.IS_DEBUGGABLE && (debugIme = ComponentName.unflattenFromString(SystemProperties.get(PROP_DEBUG_MULTI_CLIENT_IME, ""))) != null) {
            return debugIme;
        }
        return ComponentName.unflattenFromString(SystemProperties.get(PROP_PROD_MULTI_CLIENT_IME, ""));
    }

    static {
        ComponentName multiClientImeComponentName = getMultiClientImeComponentName();
        sMultiClientImeComponentName = multiClientImeComponentName;
        MULTI_CLIENT_IME_ENABLED = multiClientImeComponentName != null;
    }
}