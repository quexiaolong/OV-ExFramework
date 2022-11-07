package com.vivo.services.configurationManager;

import android.os.SystemProperties;
import java.security.AlgorithmParameters;
import javax.crypto.spec.IvParameterSpec;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class CommonUtils {
    private static final String ABE_KEYSTORE_FLAG = "abe_keystore_flag";
    private static final String TAG = "ConfigurationManager";
    private static final boolean bIsPropSet = SystemProperties.getBoolean("persist.vivo.configalgorithm_prop_set", false);
    public static final boolean DEBUG = SystemProperties.getBoolean("persist.vivo.configuration_service_debuggable", false);

    public static String getDefaultAlgorithm() {
        return "AES/ECB/PKCS5Padding";
    }

    public static String getCipherAlgorithm() {
        if (bIsPropSet) {
            return SystemProperties.get("persist.vivo.configuration_cipher_algorithm", getDefaultAlgorithm());
        }
        return getDefaultAlgorithm();
    }

    public static boolean isEffectCipher(String algorithm) {
        return algorithm != null && algorithm.contains("AES");
    }

    public static boolean isCBC(String algorithm) {
        String[] thms;
        if (algorithm != null && !algorithm.isEmpty() && (thms = algorithm.split("/")) != null && thms.length >= 2) {
            return thms[1].equals("CBC");
        }
        return false;
    }

    public static AlgorithmParameters generateIV(byte[] iv) throws Exception {
        AlgorithmParameters params = AlgorithmParameters.getInstance("AES");
        params.init(new IvParameterSpec(iv));
        return params;
    }

    public static void log(String msg) {
        if (DEBUG) {
            VSlog.d(TAG, msg);
        }
    }
}