package com.vivo.services.configurationManager;

import android.security.keystore.KeyGenParameterSpec;
import android.util.Base64;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class Utils {
    private static final String ABE_KEYSTORE_ALIAS = "KEYSTORE_ABE_KEY";
    private static final String ABE_KEY_FILE_PATH = "/data/bbkcore/cms_1_100.txt";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALGORITHM = "AES";
    private static final String RSA_MODE = "RSA/ECB/PKCS1Padding";
    private static final String TAG = "ConfigurationManager";

    public static void saveABEKey(byte[] abeKey) {
        try {
            String encryptABEKey = encryptRSA(abeKey);
            writeFile(ABE_KEY_FILE_PATH, encryptABEKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] getABEKey() {
        byte[] abeKey = null;
        try {
            if (new File(ABE_KEY_FILE_PATH).exists()) {
                String encryptABEKey = readFile(ABE_KEY_FILE_PATH, false);
                String tmp = new String(decryptRSA(encryptABEKey));
                abeKey = Base64.decode(tmp, 0);
            } else {
                VSlog.d(TAG, "/data/bbkcore/cms_1_100.txt not exists, have no key");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return abeKey;
    }

    private static KeyStore getABEKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        if (!keyStore.containsAlias(ABE_KEYSTORE_ALIAS)) {
            generateRSAKey();
        }
        return keyStore;
    }

    private static void generateRSAKey() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", KEYSTORE_PROVIDER);
        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(ABE_KEYSTORE_ALIAS, 3).setDigests("SHA-256", "SHA-512").setEncryptionPaddings("PKCS1Padding").build();
        keyPairGenerator.initialize(keyGenParameterSpec);
        keyPairGenerator.generateKeyPair();
    }

    private static String encryptRSA(byte[] plainText) throws Exception {
        KeyStore keyStore = getABEKeyStore();
        PublicKey publicKey = keyStore.getCertificate(ABE_KEYSTORE_ALIAS).getPublicKey();
        Cipher cipher = Cipher.getInstance(RSA_MODE);
        cipher.init(1, publicKey);
        byte[] encryptedByte = cipher.doFinal(plainText);
        return Base64.encodeToString(encryptedByte, 0);
    }

    private static byte[] decryptRSA(String encryptedText) throws Exception {
        KeyStore keyStore = getABEKeyStore();
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(ABE_KEYSTORE_ALIAS, null);
        Cipher cipher = Cipher.getInstance(RSA_MODE);
        cipher.init(2, privateKey);
        byte[] encryptedBytes = Base64.decode(encryptedText, 0);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return decryptedBytes;
    }

    public static String decryptFile(String path) {
        StringBuilder sb;
        InputStream inputStream = null;
        String content = null;
        try {
            try {
                InputStream inputStream2 = new FileInputStream(new File(path));
                byte[] fileData = toByteArray(inputStream2);
                byte[] abeKeys = getABEKey();
                if (abeKeys == null) {
                    VSlog.d(TAG, "cms cann't get key from abe, just return null");
                } else {
                    Key key = new SecretKeySpec(abeKeys, KEY_ALGORITHM);
                    String abeCipherAlgorithm = CommonUtils.getCipherAlgorithm();
                    VSlog.d(TAG, "effectcipher : " + CommonUtils.isEffectCipher(abeCipherAlgorithm) + " " + CommonUtils.isCBC(abeCipherAlgorithm));
                    Cipher cipher = Cipher.getInstance(abeCipherAlgorithm);
                    if (!CommonUtils.isEffectCipher(abeCipherAlgorithm)) {
                        VSlog.e(TAG, "Error abe not set algorithm");
                    } else if (CommonUtils.isCBC(abeCipherAlgorithm)) {
                        cipher.init(2, key, CommonUtils.generateIV(abeKeys));
                    } else {
                        cipher.init(2, key);
                    }
                    byte[] decryptedBytes = cipher.doFinal(fileData);
                    content = new String(decryptedBytes);
                }
                try {
                    inputStream2.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sb = new StringBuilder();
            } catch (Exception e2) {
                e2.printStackTrace();
                if (0 != 0) {
                    try {
                        inputStream.close();
                    } catch (Exception e3) {
                        e3.printStackTrace();
                    }
                }
                sb = new StringBuilder();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (Exception e4) {
                    e4.printStackTrace();
                }
            }
            sb = new StringBuilder();
        }
        sb.append("decryptFile ");
        sb.append(path);
        sb.append(" content={ ");
        sb.append(content);
        sb.append(" }");
        CommonUtils.log(sb.toString());
        return content;
    }

    public static void encryptFile(String originPath, String encryptedPath) {
        InputStream inputStream = null;
        try {
            try {
                try {
                    inputStream = new FileInputStream(new File(originPath));
                    Key key = new SecretKeySpec(getABEKey(), KEY_ALGORITHM);
                    Cipher cipher = Cipher.getInstance(CommonUtils.getCipherAlgorithm());
                    cipher.init(1, key);
                    byte[] encryptedBytes = cipher.doFinal(toByteArray(inputStream));
                    String fileData = new String(Base64.encode(encryptedBytes, 1));
                    writeFile(encryptedPath, fileData);
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } catch (Throwable th) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (Exception e3) {
            e3.printStackTrace();
        }
    }

    public static String readFile(String path, boolean appendLineWithEnter) {
        if (path == null || path.trim().length() == 0) {
            return null;
        }
        BufferedReader br = null;
        try {
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
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        if (br != null) {
                            br.close();
                        }
                        return null;
                    }
                } catch (FileNotFoundException e3) {
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
            if (0 != 0) {
                try {
                    br.close();
                } catch (IOException e5) {
                    e5.printStackTrace();
                }
            }
            throw th;
        }
    }

    public static void writeFile(String path, String content) {
        File outFile = new File(path);
        BufferedWriter bw = null;
        try {
            try {
                try {
                    bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile)));
                    bw.write(content);
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (bw != null) {
                        bw.close();
                    }
                }
            } catch (Throwable th) {
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
            e3.printStackTrace();
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