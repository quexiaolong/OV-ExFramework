package com.vivo.services.configurationManager;

import android.content.Context;
import android.os.SELinux;
import android.util.Base64;
import com.vivo.services.cipher.SecurityKeyCipher;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class DecryptUtils {
    private static final String ABE_KEY_FILE_PATH = "/data/bbkcore/cms_1_100.txt";
    private static final String ABE_KEY_FILE_PATH2 = "/data/bbkcore/cms_2_100.txt";
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final String KEY_ALGORITHM = "AES";
    private static final String TAG = "ConfigurationManager";
    private static String abeCipherAlgorithm = CommonUtils.getCipherAlgorithm();

    public static boolean isAbeSupportDecryptV2() {
        File file = new File(ABE_KEY_FILE_PATH2);
        CommonUtils.log("/data/bbkcore/cms_2_100.txt length=" + file.length());
        if (file.exists() && file.isFile() && file.canRead() && file.length() > 10) {
            return true;
        }
        return false;
    }

    public static void prepareDecryptV2KeyFile() {
        File file = new File(ABE_KEY_FILE_PATH2);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                VSlog.d(TAG, "prepareDecryptV2KeyFile failed");
                return;
            }
        }
        SELinux.restorecon(file);
    }

    public static byte[] getKeyFromABE(Context context) {
        byte[] abeKey = null;
        try {
            String encryptABEKey = readFile(ABE_KEY_FILE_PATH2, false);
            CommonUtils.log("getKeyFromABE encryptABEKey=" + encryptABEKey);
            if (encryptABEKey == null) {
                return null;
            }
            byte[] encryptedBytes = Base64.decode(encryptABEKey, 0);
            SecurityKeyCipher cipher = SecurityKeyCipher.getInstance(context, "cms_decrypt");
            if (cipher != null) {
                cipher.setCipherMode(3);
                byte[] abeKey2 = cipher.aesDecrypt(encryptedBytes);
                cipher.setCipherMode(1);
                abeKey = Base64.decode(new String(abeKey2), 0);
            } else {
                VSlog.d(TAG, "SecurityKeyCipher is null");
            }
            StringBuilder sb = new StringBuilder();
            sb.append("getKeyFromABE abeKey=");
            sb.append(abeKey != null ? new String(abeKey) : "null");
            CommonUtils.log(sb.toString());
            return abeKey;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isPlainText(String path) {
        String content = readFile(path, true);
        if (content != null && content.contains("xml")) {
            return true;
        }
        return false;
    }

    public static String decryptFile(String path, Context context) {
        String plaintext = readFile(path, true);
        if (plaintext == null || plaintext.length() == 0) {
            VSlog.d(TAG, path + " is null,just return file content, no need decrypt file");
            return plaintext;
        } else if (plaintext != null && plaintext.contains("xml")) {
            VSlog.d(TAG, path + "is plaintext,just return file content, no need decrypt file");
            return plaintext;
        } else {
            InputStream inputStream = null;
            String content = null;
            try {
                try {
                    InputStream inputStream2 = new FileInputStream(new File(path));
                    byte[] fileData = toByteArray(inputStream2);
                    byte[] keyFromAbe = getKeyFromABE(context);
                    if (keyFromAbe != null) {
                        VSlog.d(TAG, "get key from abe");
                        Key key = new SecretKeySpec(keyFromAbe, KEY_ALGORITHM);
                        String abeCipherAlgorithm2 = CommonUtils.getCipherAlgorithm();
                        VSlog.d(TAG, "effectcipher : " + CommonUtils.isEffectCipher(abeCipherAlgorithm2) + " " + CommonUtils.isCBC(abeCipherAlgorithm2));
                        Cipher cipher = Cipher.getInstance(abeCipherAlgorithm2);
                        if (!CommonUtils.isEffectCipher(abeCipherAlgorithm2)) {
                            VSlog.e(TAG, "Error abe not set algorithm");
                        } else if (CommonUtils.isCBC(abeCipherAlgorithm2)) {
                            cipher.init(2, key, CommonUtils.generateIV(keyFromAbe));
                        } else {
                            cipher.init(2, key);
                        }
                        byte[] decryptedBytes = cipher.doFinal(fileData);
                        content = new String(decryptedBytes);
                    } else {
                        VSlog.d(TAG, "cms cann't get key from abe, just return null");
                    }
                    try {
                        inputStream2.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return content;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (0 != 0) {
                        try {
                            inputStream.close();
                        } catch (Exception e3) {
                            e3.printStackTrace();
                        }
                    }
                    return null;
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        inputStream.close();
                    } catch (Exception e4) {
                        e4.printStackTrace();
                    }
                }
                return null;
            }
        }
    }

    public static String readFile(String path, boolean appendLineWithEnter) {
        if (path == null || path.trim().length() == 0 || !new File(path).exists()) {
            return null;
        }
        BufferedReader br = null;
        try {
            try {
                try {
                    File file = new File(path);
                    FileInputStream fis = new FileInputStream(file);
                    br = new BufferedReader(new InputStreamReader(fis));
                    StringBuffer sb = new StringBuffer();
                    while (true) {
                        String line = br.readLine();
                        if (line == null) {
                            break;
                        }
                        sb.append(line);
                        if (appendLineWithEnter) {
                            sb.append('\n');
                        }
                    }
                    String stringBuffer = sb.toString();
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return stringBuffer;
                } catch (FileNotFoundException e2) {
                    e2.printStackTrace();
                    if (br != null) {
                        br.close();
                    }
                    return null;
                } catch (IOException e3) {
                    e3.printStackTrace();
                    if (br != null) {
                        br.close();
                    }
                    return null;
                }
            } catch (IOException e4) {
                e4.printStackTrace();
                return null;
            }
        } catch (Throwable th) {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e5) {
                    e5.printStackTrace();
                }
            }
            throw th;
        }
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    public static int copy(InputStream input, OutputStream output) throws IOException {
        long count = copyLarge(input, output);
        if (count > 2147483647L) {
            return -1;
        }
        return (int) count;
    }

    public static long copyLarge(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        long count = 0;
        while (true) {
            int n = input.read(buffer);
            if (-1 != n) {
                output.write(buffer, 0, n);
                count += n;
            } else {
                return count;
            }
        }
    }
}