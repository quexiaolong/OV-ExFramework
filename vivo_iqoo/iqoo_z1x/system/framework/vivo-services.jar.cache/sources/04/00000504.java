package com.android.server.wm;

import android.content.Context;
import com.vivo.common.VivoCollectData;
import com.vivo.vcodetransbase.EventTransfer;
import java.util.HashMap;
import java.util.UUID;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class VCD_FF_1 {
    private static final String EVENT_ID_FREEFORM_CLOSE_BUTTON_CLICKED = "F339|10007";
    private static final String EVENT_ID_FREEFORM_CLOSE_BUTTON_CLICKED_COMBINE = "F339|10012";
    private static final String EVENT_ID_FREEFORM_DRAG_TO_MOVE = "F339|10006";
    private static final String EVENT_ID_FREEFORM_DRAG_TO_MOVE_COMBINE = "F339|10013";
    private static final String EVENT_ID_FREEFORM_FLIP_BUTTON_CLICKED = "F339|10010";
    private static final String EVENT_ID_FREEFORM_MAXIMIZE_BUTTON_CLICKED = "F339|10004";
    private static final String EVENT_ID_FREEFORM_MAXIMIZE_BUTTON_CLICKED_COMBINE = "F339|10011";
    private static final String EVENT_ID_GAME_PIP_CLOSE_BUTTON_CLICKED = "F339|10008";
    private static final String EVENT_ID_GAME_PIP_DRAG_TO_MOVE = "F339|10005";
    private static final String EVENT_ID_GAME_PIP_FLIP_BUTTON_CLICKED = "F339|10009";
    private static final String EVENT_ID_GAME_PIP_MAXIMIZE_BUTTON_CLICKED = "F339|10003";
    private static final String EVENT_ID_MULTIWINDOW = "1072";
    private static final String KEY_FREEFORM_CLOSE = "close";
    private static final String KEY_FREEFORM_DRAG = "drag";
    private static final String KEY_FREEFORM_DRAG_TO_MOVE = "move";
    private static final String KEY_FREEFORM_HIDE = "hide";
    private static final String KEY_FREEFORM_MAX = "max";
    private static final String KEY_FREEFORM_PKG = "pkgname";
    public static final String MODULE_ID = "F339";
    private static final String SUB_EVENT_ID_FREEFORM_CLOSE_BUTTON_CLICKED = "107273";
    private static final String SUB_EVENT_ID_FREEFORM_CLOSE_BUTTON_CLICKED_FLOATBALL = "107274";
    private static final String SUB_EVENT_ID_FREEFORM_DRAG_TO_MOVE = "107233";
    private static final String SUB_EVENT_ID_FREEFORM_DRAG_TO_MOVE_FLOATBALL = "107279";
    private static final String SUB_EVENT_ID_FREEFORM_FLIP_BUTTON_CLICKED = "107275";
    private static final String SUB_EVENT_ID_FREEFORM_FLIP_BUTTON_CLICKED_FLOATbALL = "107276";
    private static final String SUB_EVENT_ID_FREEFORM_MAXIMIZE_BUTTON_CLICKED = "107232";
    private static final String SUB_EVENT_ID_FREEFORM_MAXIMIZE_BUTTON_CLICKED_FLOATBALL = "107272";
    private static final String TAG = "VCD_FF_1";
    public static final String UUID_STR = "uuid";
    private static boolean mIsFreeFormVCDEnable = false;

    VCD_FF_1() {
    }

    static boolean isFreeFormVCDEnable() {
        return mIsFreeFormVCDEnable;
    }

    static void setFreeFormVCDEnable(boolean isFreeFormVCDEnable) {
        mIsFreeFormVCDEnable = isFreeFormVCDEnable;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void VCD_FF_2(Context context) {
        if (context == null) {
            VSlog.e(TAG, "VCD_FF_2 failed ! Context is null !");
            return;
        }
        try {
            VivoCollectData vivoCollectData = VivoCollectData.getInstance(context);
            mIsFreeFormVCDEnable = vivoCollectData.getControlInfo(EVENT_ID_MULTIWINDOW);
            VSlog.i(TAG, "attachStack init VCD_FF_2 = " + mIsFreeFormVCDEnable);
        } catch (Exception e) {
            VSlog.e(TAG, "attachStack VCD_FF_2 e = " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void VCD_FF_3(Context context, boolean isDirectFreeform, String pkg) {
        try {
            HashMap<String, String> params = new HashMap<>(3);
            params.put(UUID_STR, UUID.randomUUID().toString());
            params.put(KEY_FREEFORM_MAX, "1");
            try {
                params.put(KEY_FREEFORM_PKG, pkg);
                EventTransfer.getInstance().singleEvent("F339", EVENT_ID_FREEFORM_MAXIMIZE_BUTTON_CLICKED_COMBINE, System.currentTimeMillis(), 0L, params);
            } catch (Exception e) {
                e = e;
                VSlog.e(TAG, "VCD_FF_3 vcode failed ! e = " + e);
                if (context != null) {
                }
                VSlog.e(TAG, "VCD_FF_3 failed ! context:" + context + " mIsFreeFormVCDEnable:" + mIsFreeFormVCDEnable);
                return;
            }
        } catch (Exception e2) {
            e = e2;
        }
        if (context != null || !mIsFreeFormVCDEnable) {
            VSlog.e(TAG, "VCD_FF_3 failed ! context:" + context + " mIsFreeFormVCDEnable:" + mIsFreeFormVCDEnable);
            return;
        }
        try {
            VivoCollectData vivoCollectData = VivoCollectData.getInstance(context);
            HashMap<String, String> params2 = new HashMap<>();
            params2.put(KEY_FREEFORM_MAX, "1");
            if (isDirectFreeform) {
                vivoCollectData.writeData(EVENT_ID_MULTIWINDOW, SUB_EVENT_ID_FREEFORM_MAXIMIZE_BUTTON_CLICKED_FLOATBALL, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params2);
            } else {
                vivoCollectData.writeData(EVENT_ID_MULTIWINDOW, SUB_EVENT_ID_FREEFORM_MAXIMIZE_BUTTON_CLICKED, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params2);
            }
        } catch (Exception e3) {
            VSlog.e(TAG, "VCD_FF_3 failed ! e = " + e3);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void VCD_FF_4(Context context, boolean isDirectFreeform, String pkg) {
        try {
            HashMap<String, String> params = new HashMap<>(3);
            params.put(UUID_STR, UUID.randomUUID().toString());
            params.put(KEY_FREEFORM_CLOSE, "1");
            try {
                params.put(KEY_FREEFORM_PKG, pkg);
                EventTransfer.getInstance().singleEvent("F339", EVENT_ID_FREEFORM_CLOSE_BUTTON_CLICKED_COMBINE, System.currentTimeMillis(), 0L, params);
            } catch (Exception e) {
                e = e;
                VSlog.e(TAG, "VCD_FF_4 vcode failed! e = " + e);
                if (context != null) {
                }
                VSlog.e(TAG, "VCD_FF_4 failed ! context:" + context + " mIsFreeFormVCDEnable:" + mIsFreeFormVCDEnable);
                return;
            }
        } catch (Exception e2) {
            e = e2;
        }
        if (context != null || !mIsFreeFormVCDEnable) {
            VSlog.e(TAG, "VCD_FF_4 failed ! context:" + context + " mIsFreeFormVCDEnable:" + mIsFreeFormVCDEnable);
            return;
        }
        try {
            VivoCollectData vivoCollectData = VivoCollectData.getInstance(context);
            HashMap<String, String> params2 = new HashMap<>();
            params2.put(KEY_FREEFORM_CLOSE, "1");
            if (isDirectFreeform) {
                vivoCollectData.writeData(EVENT_ID_MULTIWINDOW, SUB_EVENT_ID_FREEFORM_CLOSE_BUTTON_CLICKED_FLOATBALL, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params2);
            } else {
                vivoCollectData.writeData(EVENT_ID_MULTIWINDOW, SUB_EVENT_ID_FREEFORM_CLOSE_BUTTON_CLICKED, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params2);
            }
        } catch (Exception e3) {
            VSlog.e(TAG, "VCD_FF_4 failed! e = " + e3);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void VCD_FF_5(Context context, WindowState windowState, boolean isDirectFreeform) {
        if (windowState == null || !windowState.inFreeformWindowingMode()) {
            VSlog.e(TAG, "VCD_FF_5 failed !  windowState:" + windowState);
            return;
        }
        try {
            HashMap<String, String> params = new HashMap<>(3);
            params.put(UUID_STR, UUID.randomUUID().toString());
            params.put(KEY_FREEFORM_DRAG_TO_MOVE, "1");
            params.put(KEY_FREEFORM_PKG, windowState.getOwningPackage());
            EventTransfer.getInstance().singleEvent("F339", EVENT_ID_FREEFORM_DRAG_TO_MOVE_COMBINE, System.currentTimeMillis(), 0L, params);
        } catch (Exception e) {
            VSlog.e(TAG, "VCD_FF_5 vcode failed! e = " + e);
        }
        if (context == null || !mIsFreeFormVCDEnable) {
            VSlog.e(TAG, "VCD_FF_5 failed ! mIsFreeFormVCDEnable:" + mIsFreeFormVCDEnable);
            return;
        }
        try {
            VivoCollectData vivoCollectData = VivoCollectData.getInstance(context);
            HashMap<String, String> params2 = new HashMap<>();
            params2.put(KEY_FREEFORM_DRAG, "1");
            if (isDirectFreeform) {
                vivoCollectData.writeData(EVENT_ID_MULTIWINDOW, SUB_EVENT_ID_FREEFORM_DRAG_TO_MOVE_FLOATBALL, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params2);
            } else {
                vivoCollectData.writeData(EVENT_ID_MULTIWINDOW, SUB_EVENT_ID_FREEFORM_DRAG_TO_MOVE, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params2);
            }
        } catch (Exception exception) {
            VSlog.e(TAG, "VCD_FF_5 Error e = " + exception);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void VCD_FF_6(Context context, boolean isDirectFreeform) {
        try {
            HashMap<String, String> params = new HashMap<>(2);
            params.put(UUID_STR, UUID.randomUUID().toString());
            params.put(KEY_FREEFORM_HIDE, "1");
            if (isDirectFreeform) {
                EventTransfer.getInstance().singleEvent("F339", EVENT_ID_FREEFORM_FLIP_BUTTON_CLICKED, System.currentTimeMillis(), 0L, params);
            } else {
                EventTransfer.getInstance().singleEvent("F339", EVENT_ID_GAME_PIP_FLIP_BUTTON_CLICKED, System.currentTimeMillis(), 0L, params);
            }
        } catch (Exception e) {
            VSlog.e(TAG, "VCD_FF_6 vcode failed! e = " + e);
        }
        if (context == null || !mIsFreeFormVCDEnable) {
            VSlog.e(TAG, "VCD_FF_6 failed ! context:" + context + " mIsFreeFormVCDEnable:" + mIsFreeFormVCDEnable);
            return;
        }
        try {
            VivoCollectData vivoCollectData = VivoCollectData.getInstance(context);
            HashMap<String, String> params2 = new HashMap<>();
            params2.put(KEY_FREEFORM_HIDE, "1");
            if (isDirectFreeform) {
                vivoCollectData.writeData(EVENT_ID_MULTIWINDOW, SUB_EVENT_ID_FREEFORM_FLIP_BUTTON_CLICKED_FLOATbALL, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params2);
            } else {
                vivoCollectData.writeData(EVENT_ID_MULTIWINDOW, SUB_EVENT_ID_FREEFORM_FLIP_BUTTON_CLICKED, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params2);
            }
        } catch (Exception exception) {
            VSlog.e(TAG, "VCD_FF_6 Error e = " + exception);
        }
    }
}