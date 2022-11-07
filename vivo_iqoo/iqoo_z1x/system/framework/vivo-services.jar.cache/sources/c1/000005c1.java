package com.vivo.face.common.data;

import android.net.Uri;
import android.provider.Settings;
import com.vivo.services.superresolution.Constant;

/* loaded from: classes.dex */
public final class Constants {
    public static final String[] PKG_FACE_PAY = {Constant.APP_WEIXIN, "com.eg.android.AlipayGphone", "com.tencent.mobileqq", "com.taobao.taobao", "com.tmall.wireless", "com.taobao.idlefish", "com.taobao.movie.android", "com.taobao.trip", "com.taobao.litetao", "com.citic21.user"};
    public static final int VIVO_SWITCH_OFF = 0;
    public static final int VIVO_SWITCH_ON = 1;

    /* loaded from: classes.dex */
    public static final class ArdUpdate {
        public static final int ARD_ALREADY_UPDATE_STATE = 2;
        public static final int ARD_UPDATE_NONE_STATE = 0;
        public static final int ARD_UPDATE_STATE = 1;
        public static final int ARD_VERSION10 = 10;
        public static final int ARD_VERSION9 = 9;
        public static final String PROP_ARD_UPDATE = "persist.sys.face.ard_updated";
    }

    /* loaded from: classes.dex */
    public static class CMD {
        private static final int CMD_HIDL_BASE = 1000;
        public static final int CMD_HIDL_ENTER_PRIVACY = 1007;
        public static final int CMD_HIDL_EXIT_PRIVACY = 1008;
        public static final int CMD_HIDL_FACE_AUTO_TEST = 1005;
        public static final int CMD_HIDL_GET_PRE_UPDATE_ARD_VERSION = 1004;
        public static final int CMD_HIDL_INITIALIZATION = 1006;
        public static final int CMD_HIDL_KEYGUARD_CLOSED = 1010;
        public static final int CMD_HIDL_OPEN_CAMERA_ERROR = 1003;
        public static final int CMD_HIDL_OPEN_IR = 1012;
        public static final int CMD_HIDL_PAYMENT_AUTH = 1009;
        public static final int CMD_HIDL_RELEASE_MEMORY = 1011;
        public static final int CMD_HIDL_SET_SHARE_MEMORY_FD = 1001;
        public static final int CMD_HIDL_WRITE_FACE_DATA = 1002;
    }

    /* loaded from: classes.dex */
    public static class Setting {
        public static final String ARD9_FACE_FINGER_COMBINE = "finger_face_combine";
        public static final String ARD9_FACE_UNLOCK_ADJUST_SCREEN_BRIGHTNESS = "faceunlock_adjust_screen_brightness";
        public static final String ARD9_FACE_UNLOCK_ANIMATION_STYLE = "vivo_face_animation_style";
        public static final String ARD9_FACE_UNLOCK_ASSISSTANT_ENABLED = "faceunlock_assisstant_enabled";
        public static final String ARD9_FACE_UNLOCK_ATTENTION_FOCUS = "faceunlock_attention_focus";
        public static final String ARD9_FACE_UNLOCK_FAST_UNLOCK = "";
        public static final String ARD9_FACE_UNLOCK_FORBID_EYE_CLOSE = "faceunlock_forbid_eye_close";
        public static final String ARD9_FACE_UNLOCK_KEYGUARD_ENABLED = "faceunlock_enabled";
        public static final String ARD9_FACE_UNLOCK_KEYGUARD_KEEP = "faceunlock_keyguard_keep";
        public static final String ARD9_FACE_UNLOCK_POPUP_CAMERA_BY_POWER = "faceunlock_popup_camera_unlock_by";
        public static final String ARD9_FACE_UNLOCK_PRIVACY_ENABLED_1ST = "faceunlock_secure_open";
        public static final String ARD9_FACE_UNLOCK_PRIVACY_ENABLED_2ND = "faceunlock_privacy_user_enabled";
        public static final String ARD9_FACE_UNLOCK_SCREEN_OFF = "faceunlock_screen_doze";
        public static final String ARD9_FACE_UNLOCK_START_WHEN_SCREENON = "faceunlock_start_when_screenon";
        public static final String FACE_FINGER_COMBINE = "finger_face_combine";
        public static final String FACE_UNLOCK_ADJUST_SCREEN_BRIGHTNESS = "faceunlock_adjust_screen_brightness";
        public static final String FACE_UNLOCK_ANIMATION_STYLE = "faceunlock_animation_style";
        public static final String FACE_UNLOCK_ASSISSTANT_ENABLED = "faceunlock_assisstant_enabled";
        public static final String FACE_UNLOCK_ATTENTION_FOCUS = "faceunlock_attention_focus";
        public static final String FACE_UNLOCK_FAST_UNLOCK = "faceunlock_fast_unlock";
        public static final String FACE_UNLOCK_FORBID_EYE_CLOSE = "faceunlock_forbid_eye_close";
        public static final String FACE_UNLOCK_KEYGUARD_ENABLED = "face_unlock_keyguard_enabled";
        public static final String FACE_UNLOCK_KEYGUARD_KEEP = "faceunlock_keyguard_keep";
        public static final String FACE_UNLOCK_POPUP_CAMERA_BY_POWER = "faceunlock_popup_camera_unlock_by";
        public static final String FACE_UNLOCK_PRIVACY_ENABLED = "faceunlock_privacy_enabled";
        public static final String FACE_UNLOCK_SCREEN_OFF = "faceunlock_screen_off";
        public static final String FACE_UNLOCK_START_WHEN_SCREENON = "faceunlock_start_when_screenon";
    }

    /* loaded from: classes.dex */
    public static class URI {
        public static final Uri URI_UNLOCK_KEYGUARD_ENABLED = Settings.Secure.getUriFor(Setting.FACE_UNLOCK_KEYGUARD_ENABLED);
        public static final Uri URI_UNLOCK_SCREEN_OFF_ENABLED = Settings.Secure.getUriFor(Setting.FACE_UNLOCK_SCREEN_OFF);
        public static final Uri URI_UNLOCK_KEYGUARD_KEEP_ENABLED = Settings.Secure.getUriFor("faceunlock_keyguard_keep");
    }
}