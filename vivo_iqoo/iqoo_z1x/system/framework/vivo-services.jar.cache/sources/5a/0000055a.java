package com.android.server.wm;

import android.os.FtBuild;

/* loaded from: classes.dex */
public class VivoFreeformUtils {
    public static final int FREEFORM_TRANSACTION_ENTERING = 106;
    public static final int FREEFORM_TRANSACTION_EXITING = 104;
    static final int FREEZING_SCREEN_FOR_MULTIWINDOW_TIMEOUT = 1000;
    static final int FREEZING_SCREEN_FOR_MULTIWINDOW_TIMEOUT_MSG = 108;
    public static final int RESIZE_TASK_FOR_FREEFORM = 105;
    public static final String VIVO_FREEFORM_IME_SETTINGS_STATE = "ime_freeform";
    static final String VIVO_FREEFORM_INPUT_POS = "game_show_ime_pos";
    static final String VIVO_FREEFORM_SETTINGS_STATE = "smartmultiwindow_freeform";
    public static final String VIVO_SETTINGS_FREEFORM_RESIZED_IN_CURRENT_ROTATION = "freeform_resized_in_current_rotation";
    public static final String VIVO_SETTINGS_IME_FREEFORM_REMIND_TIME = "ime_noti_time";
    public static final String VIVO_SETTINGS_IN_FREEFORM_TRANSIT = "in_freeform_transit";
    public static final float ratioFreeformLandscape = 0.6926f;
    public static final float ratioFreeformPortrait = 1.924f;
    public static boolean sIsVosProduct = "vos".equals(FtBuild.getOsName());
    public static boolean inFullscreenMode = false;
}