package com.android.server.locksettings;

import android.content.pm.UserInfo;
import android.os.UserManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.widget.LockscreenCredential;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/* loaded from: classes.dex */
public class ManagedProfilePasswordCache {
    private static final int CACHE_TIMEOUT_SECONDS = (int) TimeUnit.DAYS.toSeconds(7);
    private static final int KEY_LENGTH = 256;
    private static final String TAG = "ManagedProfilePasswordCache";
    private final SparseArray<byte[]> mEncryptedPasswords = new SparseArray<>();
    private final KeyStore mKeyStore;
    private final UserManager mUserManager;

    public ManagedProfilePasswordCache(KeyStore keyStore, UserManager userManager) {
        this.mKeyStore = keyStore;
        this.mUserManager = userManager;
    }

    public void storePassword(int userId, LockscreenCredential password) {
        synchronized (this.mEncryptedPasswords) {
            if (this.mEncryptedPasswords.contains(userId)) {
                return;
            }
            UserInfo parent = this.mUserManager.getProfileParent(userId);
            if (parent != null && parent.id == 0) {
                String keyName = getEncryptionKeyName(userId);
                try {
                    KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
                    generator.init(new KeyGenParameterSpec.Builder(keyName, 3).setKeySize(256).setBlockModes("GCM").setEncryptionPaddings("NoPadding").setUserAuthenticationRequired(true).setUserAuthenticationValidityDurationSeconds(CACHE_TIMEOUT_SECONDS).build());
                    SecretKey key = generator.generateKey();
                    try {
                        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                        cipher.init(1, key);
                        byte[] ciphertext = cipher.doFinal(password.getCredential());
                        byte[] iv = cipher.getIV();
                        byte[] block = Arrays.copyOf(iv, ciphertext.length + iv.length);
                        System.arraycopy(ciphertext, 0, block, iv.length, ciphertext.length);
                        this.mEncryptedPasswords.put(userId, block);
                    } catch (GeneralSecurityException e) {
                        Slog.d(TAG, "Cannot encrypt", e);
                    }
                } catch (GeneralSecurityException | ProviderException e2) {
                    Slog.e(TAG, "Cannot generate key", e2);
                }
            }
        }
    }

    public LockscreenCredential retrievePassword(int userId) {
        synchronized (this.mEncryptedPasswords) {
            byte[] block = this.mEncryptedPasswords.get(userId);
            if (block == null) {
                return null;
            }
            try {
                Key key = this.mKeyStore.getKey(getEncryptionKeyName(userId), null);
                if (key == null) {
                    return null;
                }
                byte[] iv = Arrays.copyOf(block, 12);
                byte[] ciphertext = Arrays.copyOfRange(block, 12, block.length);
                try {
                    try {
                        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                        cipher.init(2, key, new GCMParameterSpec(128, iv));
                        byte[] credential = cipher.doFinal(ciphertext);
                        LockscreenCredential result = LockscreenCredential.createManagedPassword(credential);
                        Arrays.fill(credential, (byte) 0);
                        return result;
                    } catch (UserNotAuthenticatedException e) {
                        Slog.i(TAG, "Device not unlocked for more than 7 days");
                        return null;
                    }
                } catch (GeneralSecurityException e2) {
                    Slog.d(TAG, "Cannot decrypt", e2);
                    return null;
                }
            } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e3) {
                Slog.d(TAG, "Cannot get key", e3);
                return null;
            }
        }
    }

    public void removePassword(int userId) {
        synchronized (this.mEncryptedPasswords) {
            String keyName = getEncryptionKeyName(userId);
            String legacyKeyName = getLegacyEncryptionKeyName(userId);
            try {
                if (this.mKeyStore.containsAlias(keyName)) {
                    this.mKeyStore.deleteEntry(keyName);
                }
                if (this.mKeyStore.containsAlias(legacyKeyName)) {
                    this.mKeyStore.deleteEntry(legacyKeyName);
                }
            } catch (KeyStoreException e) {
                Slog.d(TAG, "Cannot delete key", e);
            }
            if (this.mEncryptedPasswords.contains(userId)) {
                Arrays.fill(this.mEncryptedPasswords.get(userId), (byte) 0);
                this.mEncryptedPasswords.remove(userId);
            }
        }
    }

    private static String getEncryptionKeyName(int userId) {
        return "com.android.server.locksettings.unified_profile_cache_v2_" + userId;
    }

    private static String getLegacyEncryptionKeyName(int userId) {
        return "com.android.server.locksettings.unified_profile_cache_" + userId;
    }
}