package com.vivo.face.common.data;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.FtFeature;
import com.vivo.face.common.utils.FaceLog;

/* loaded from: classes.dex */
public final class Config {
    private static final int SENSOR_TYPE_OPTICAL = 2;
    private static final String TAG = "Config";
    private static final String PRODUCT = SystemProperties.get("ro.vivo.product.model", "unknown").toLowerCase();
    private static final boolean SUPPORT_POPUPCAMERA = FtFeature.isFeatureSupport(64);
    private static final boolean SUPPORT_FACE_ASSISTANT = SystemProperties.get("persist.vivo.face.assistant", "0").equals("1");

    private static boolean isPD1832F_EX() {
        return TextUtils.equals(PRODUCT, "pd1832f_ex");
    }

    public static boolean isPD1821() {
        return PRODUCT.startsWith("pd1821");
    }

    public static void intProperties(Context context) {
        FaceLog.i(TAG, "init properties");
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService("fingerprint");
        boolean isUDFingerprint = false;
        if (fingerprintManager != null) {
            int fingerprintType = fingerprintManager.getFingerprintSensorType();
            isUDFingerprint = fingerprintType == 2;
        }
        if (isPD1832F_EX()) {
            SystemProperties.set("persist.vivo.face.finger.combine", "1");
        }
        if (isUDFingerprint && !SUPPORT_POPUPCAMERA && context.getPackageManager().hasSystemFeature("android.hardware.biometrics.face") && !isPD1821()) {
            SystemProperties.set("persist.vivo.face.finger.combine", "1");
        }
        if (isPD1821()) {
            SystemProperties.set("persist.vivo.face.no.effect", "1");
        }
    }
}