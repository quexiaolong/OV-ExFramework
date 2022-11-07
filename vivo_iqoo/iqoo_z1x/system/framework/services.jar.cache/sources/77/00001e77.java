package com.android.server.wm;

import android.os.SystemProperties;

/* loaded from: classes2.dex */
public interface IVivoMultiWindowConfig {
    public static final boolean DEBUG_ALL_SPLIT_PRIV_LOG = SystemProperties.getBoolean("persist.vivo.multiwindow_fmk_debug_all_private_info", false);
    public static final String HOME_ACTIVITY_CLASS_NAME = " com.bbk.launcher2.Launcher";
    public static final String RECENTS_ACTIVITY_CLASS_NAME = "com.vivo.upslide.recents.RecentsActivity";
    public static final String SPLIT_APPLIST_PKG = "com.vivo.smartmultiwindow";
    public static final String VIVOMULTIWINDOWAPPLIST_CLASS_NAME = "com.vivo.smartmultiwindow.minilauncher2.Launcher";
    public static final String VIVOMULTIWINDOW_PASSWRD_CLASS_NAME = "com.vivo.settings.secret.PasswordActivityMultiWindow";
}