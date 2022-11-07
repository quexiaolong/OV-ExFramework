package com.android.server.wm;

import android.text.TextUtils;
import android.util.Base64;
import com.vivo.face.common.data.Constants;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/* loaded from: classes.dex */
public class AlertWindowWhiteListCryUtils {
    private static final String ALGORITHM = "AES";
    private static String PASSWORD_CRYPT_KEY = null;

    private static String parseByte2HexStr(byte[] buf) {
        StringBuffer sb = new StringBuffer();
        for (byte b : buf) {
            String hex = Integer.toHexString(b & 255);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    private static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        }
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, (i * 2) + 1), 16);
            int low = Integer.parseInt(hexStr.substring((i * 2) + 1, (i * 2) + 2), 16);
            result[i] = (byte) ((high * 16) + low);
        }
        return result;
    }

    public static void setCryptKey(String key) {
        PASSWORD_CRYPT_KEY = key;
    }

    private static void getCryptKey() {
        if (PASSWORD_CRYPT_KEY == null) {
            PASSWORD_CRYPT_KEY = "R.string.0x7f050001";
        }
    }

    public static String encrypt(String resource) {
        if (!TextUtils.isEmpty(resource)) {
            byte[] secretArr = encryptMode(resource.getBytes());
            byte[] secret = Base64.encode(secretArr, 0);
            String secretString = new String(secret);
            return secretString;
        }
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public static String encrypt(String resource, String keyString) {
        if (!TextUtils.isEmpty(resource)) {
            if (!TextUtils.isEmpty(keyString)) {
                byte[] secretArr = encryptMode(resource.getBytes(), keyString);
                byte[] secret = Base64.encode(secretArr, 0);
                String secretString = new String(secret);
                return secretString;
            }
            return encrypt(resource);
        }
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public static String decrypt(String encrptedStr) {
        if (TextUtils.isEmpty(encrptedStr)) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        try {
            byte[] decryptFrom = Base64.decode(encrptedStr, 0);
            byte[] secreArr2 = decryptMode(decryptFrom);
            if (secreArr2 != null) {
                return new String(secreArr2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public static String decrypt(String encrptedStr, String keyString) {
        if (TextUtils.isEmpty(encrptedStr)) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        if (!TextUtils.isEmpty(keyString)) {
            try {
                byte[] decryptFrom = Base64.decode(encrptedStr, 0);
                byte[] secreArr2 = decryptMode(decryptFrom, keyString);
                if (secreArr2 != null) {
                    return new String(secreArr2);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        return decrypt(encrptedStr);
    }

    private static byte[] encryptMode(byte[] src) {
        if (PASSWORD_CRYPT_KEY == null) {
            getCryptKey();
        }
        try {
            SecretKey deskey = new SecretKeySpec(build3Deskey(PASSWORD_CRYPT_KEY), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(1, deskey);
            return cipher.doFinal(src);
        } catch (Exception var3) {
            var3.printStackTrace();
            return null;
        }
    }

    private static byte[] encryptMode(byte[] src, String keyString) {
        if (PASSWORD_CRYPT_KEY == null) {
            getCryptKey();
        }
        if (TextUtils.isEmpty(keyString)) {
            keyString = PASSWORD_CRYPT_KEY;
        }
        try {
            SecretKey deskey = new SecretKeySpec(build3Deskey(keyString), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(1, deskey);
            return cipher.doFinal(src);
        } catch (Exception var4) {
            var4.printStackTrace();
            return null;
        }
    }

    private static byte[] decryptMode(byte[] src) {
        if (PASSWORD_CRYPT_KEY == null) {
            getCryptKey();
        }
        try {
            SecretKey deskey = new SecretKeySpec(build3Deskey(PASSWORD_CRYPT_KEY), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(2, deskey);
            return cipher.doFinal(src);
        } catch (Exception var3) {
            var3.printStackTrace();
            return null;
        }
    }

    private static byte[] decryptMode(byte[] src, String keyString) {
        if (PASSWORD_CRYPT_KEY == null) {
            getCryptKey();
        }
        if (TextUtils.isEmpty(keyString)) {
            keyString = PASSWORD_CRYPT_KEY;
        }
        try {
            SecretKey deskey = new SecretKeySpec(build3Deskey(keyString), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(2, deskey);
            return cipher.doFinal(src);
        } catch (Exception var4) {
            var4.printStackTrace();
            return null;
        }
    }

    private static byte[] build3Deskey(String keyStr) throws Exception {
        byte[] key = new byte[24];
        byte[] temp = keyStr.getBytes();
        if (key.length > temp.length) {
            System.arraycopy(temp, 0, key, 0, temp.length);
        } else {
            System.arraycopy(temp, 0, key, 0, key.length);
        }
        return key;
    }
}