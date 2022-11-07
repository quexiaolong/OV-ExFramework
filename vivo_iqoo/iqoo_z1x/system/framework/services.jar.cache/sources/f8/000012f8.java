package com.android.server.net.watchlist;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/* loaded from: classes.dex */
public class DigestUtils {
    private static final int FILE_READ_BUFFER_SIZE = 16384;

    private DigestUtils() {
    }

    public static byte[] getSha256Hash(File apkFile) throws IOException, NoSuchAlgorithmException {
        InputStream stream = new FileInputStream(apkFile);
        try {
            byte[] sha256Hash = getSha256Hash(stream);
            stream.close();
            return sha256Hash;
        } catch (Throwable th) {
            try {
                stream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public static byte[] getSha256Hash(InputStream stream) throws IOException, NoSuchAlgorithmException {
        MessageDigest digester = MessageDigest.getInstance("SHA256");
        byte[] buf = new byte[16384];
        while (true) {
            int bytesRead = stream.read(buf);
            if (bytesRead >= 0) {
                digester.update(buf, 0, bytesRead);
            } else {
                return digester.digest();
            }
        }
    }
}