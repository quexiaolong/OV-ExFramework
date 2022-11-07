package com.vivo.services.popupcamera;

import android.content.Context;
import android.os.SystemProperties;
import android.os.Vibrator;
import com.vivo.common.utils.VLog;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class PopupCameraVibrateManager {
    private static final String PROP_POPUPCAMERA_VIBRATE_ENABLE = "persist.vivo.popupcamera.vibrate.enable";
    private static final String TAG = "PopupCameraVibrate";
    private static Method mCancelMethod;
    private static Class mClazz;
    private static boolean mIsVibEnable = false;
    private static Vibrator mVibrator;
    private static Method mVibtatorMethod;
    private static Context sContext;

    public static void init(Context context) {
        int vibrateEnable = SystemProperties.getInt(PROP_POPUPCAMERA_VIBRATE_ENABLE, 0);
        if (1 == vibrateEnable) {
            mIsVibEnable = true;
            sContext = context;
            Vibrator vibrator = (Vibrator) context.getSystemService(Vibrator.class);
            mVibrator = vibrator;
            if (vibrator != null) {
                mClazz = vibrator.getClass();
            }
            try {
                mVibtatorMethod = mClazz.getDeclaredMethod("vibratorPro", Integer.TYPE, Long.TYPE, Integer.TYPE);
                mCancelMethod = mClazz.getDeclaredMethod("cancelVibPro", new Class[0]);
            } catch (Exception e) {
                VLog.d(TAG, "call method exception" + e);
            }
        }
    }

    public static synchronized void startCameraVibrate(int mode, boolean isPopup) {
        synchronized (PopupCameraVibrateManager.class) {
            if (mIsVibEnable) {
                VLog.d(TAG, "start popup vibrate");
                vibrate(mode, isPopup);
            }
        }
    }

    public static synchronized void stopCameraVibrate() {
        synchronized (PopupCameraVibrateManager.class) {
            if (mIsVibEnable) {
                VLog.d(TAG, "stop vibrate");
                cancelVibrate();
            }
        }
    }

    private static int obtainVibratorEffectID(int mode, boolean isPopup) {
        if (mode == 1) {
            if (isPopup) {
                return 805;
            }
            return 806;
        } else if (mode == 2) {
            if (isPopup) {
                return 801;
            }
            return 802;
        } else if (mode != 3) {
            return 0;
        } else {
            if (isPopup) {
                return 803;
            }
            return 804;
        }
    }

    private static void vibrate(int mode, boolean isPopup) {
        if (mVibrator != null) {
            try {
                if (mVibtatorMethod != null) {
                    int effectID = obtainVibratorEffectID(mode, isPopup);
                    if (effectID == 0) {
                        VLog.d(TAG, "get PopupCameraVibrate error,mode:" + mode);
                    }
                    long playMills = ((Long) mVibtatorMethod.invoke(mVibrator, Integer.valueOf(effectID), -1, -1)).longValue();
                    VLog.d(TAG, "PopupCameraWillVibrate: " + playMills);
                    return;
                }
                VLog.d(TAG, "get vibratorPro method failed");
                return;
            } catch (Exception e) {
                VLog.d(TAG, "call vibratorPro exception" + e);
                return;
            }
        }
        VLog.d(TAG, "Vibrator is null");
    }

    private static void cancelVibrate() {
        Vibrator vibrator = mVibrator;
        if (vibrator != null) {
            try {
                if (mCancelMethod != null) {
                    mCancelMethod.invoke(vibrator, new Object[0]);
                    VLog.d(TAG, "cancel vibrate");
                } else {
                    VLog.d(TAG, "get cancelVibPro method failed");
                }
                return;
            } catch (Exception e) {
                VLog.d(TAG, "call cancelVibPro exception");
                return;
            }
        }
        VLog.d(TAG, "Vibrator is null");
    }
}