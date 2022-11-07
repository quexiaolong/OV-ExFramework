package com.android.server.locksettings;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.biometrics.fingerprint.FingerprintKeyguardInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.FtBuild;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.security.keystore.KeyProtection;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.internal.widget.ICheckCredentialProgressCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.LocalServices;
import com.android.server.location.VivoNlpPowerMonitor;
import com.android.server.locksettings.LockSettingsService;
import com.android.server.locksettings.VivoLockSettingsServiceImpl;
import com.vivo.face.common.data.Constants;
import com.vivo.services.cipher.utils.SecurityStringOperateUtil;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLockSettingsServiceImpl implements IVivoLockSettingsService {
    private static final int AES_KEY_LENGTH = 32;
    private static final int DOUBLE_APP_USER = 999;
    private static boolean PERFORM_ONCE_FLAG = false;
    private static final String TAG = "VivoLockSettingsServiceImpl";
    private static final boolean TEMPDEBUG = false;
    private static final String VIVO_LOCK_REBOOT_SPLOG_PROPERTY = "persist.vivo.reboot.splog";
    private static final String VP_ENCRYPT_KEY = "vp_encrypt_key";
    private static HandlerThread mDatabaseTempHandlerThread;
    private BBKUpdaterObserver mBBKUpdaterObserver;
    private Context mContext;
    private Handler mDatabaseTempHandler;
    private LockSettingsStorage mLockSettingsStorage;
    private LockSettingsService mLocksettingsservice;
    private UserManager mUsermanager;
    private SparseArray<byte[]> mVivoSavePasswordCache = new SparseArray<>();

    public VivoLockSettingsServiceImpl(LockSettingsService lss, Context context, LockSettingsStorage storage, UserManager um) {
        this.mLocksettingsservice = lss;
        this.mUsermanager = um;
        this.mContext = context;
        this.mLockSettingsStorage = storage;
    }

    public void initialization() {
        this.mDatabaseTempHandler = LockSettingsService.sHandlerThread.getThreadHandler();
        this.mBBKUpdaterObserver = new BBKUpdaterObserver();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BBKUpdaterObserver extends ContentObserver {
        private final Uri isBBKUpdaterUri;

        public BBKUpdaterObserver() {
            super(VivoLockSettingsServiceImpl.this.mDatabaseTempHandler);
            this.isBBKUpdaterUri = Settings.Global.getUriFor("com.bbk.reboot.notify.lock");
            ContentResolver resolver = VivoLockSettingsServiceImpl.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.isBBKUpdaterUri, false, this, -1);
        }

        public /* synthetic */ void lambda$onChange$0$VivoLockSettingsServiceImpl$BBKUpdaterObserver(int userId) {
            VivoLockSettingsServiceImpl.this.saveVPassword(userId);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, final int userId) {
            VivoLockSettingsServiceImpl.this.mDatabaseTempHandler.post(new Runnable() { // from class: com.android.server.locksettings.-$$Lambda$VivoLockSettingsServiceImpl$BBKUpdaterObserver$UIphX3he1Xs0V_Uohz5elSkrByo
                @Override // java.lang.Runnable
                public final void run() {
                    VivoLockSettingsServiceImpl.BBKUpdaterObserver.this.lambda$onChange$0$VivoLockSettingsServiceImpl$BBKUpdaterObserver(userId);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveVPassword(int userId) {
        int currentUserId = ActivityManager.getCurrentUser();
        VSlog.i(TAG, "BBKUpdaterObserver onChange, userId: " + userId + ", current userId: " + currentUserId);
        if (userId != 0) {
            return;
        }
        int notifyValue = getIntFromSettingsGlobal("com.bbk.reboot.notify.lock", 0);
        if (notifyValue == 0) {
            VSlog.i(TAG, "BBK_REBOOT_NOTIFY_LOCK_VALUE == 0,return");
        } else if (StorageManager.isFileEncryptedNativeOrEmulated() && !StorageManager.isUserKeyUnlocked(0)) {
            VSlog.w(TAG, "user 0 still locked, return.");
            putIntFromSettingsGlobal("com.bbk.reboot.notify.lock", 0);
        } else {
            String rebootReason = SystemProperties.get("persist.vivo.lock.reboot", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            VSlog.i(TAG, "LOCK_REBOOT_PROPERTY is " + rebootReason);
            if (!rebootReason.equals("silent") && !rebootReason.equals("silent_update")) {
                VSlog.e(TAG, "LOCK_REBOOT_PROPERTY is wrong, return.");
                putIntFromSettingsGlobal("com.bbk.reboot.notify.lock", 0);
                return;
            }
            boolean openFaceUnlock = getIntFromSettingsSecure(Constants.Setting.FACE_UNLOCK_KEYGUARD_ENABLED, 0) == 1;
            boolean openFingerprintUnlock = getIntFromSettingsSystem(VivoNlpPowerMonitor.FINGER_UNLOCK, 0) == 1;
            VSlog.i(TAG, "openFaceUnlock == " + openFaceUnlock);
            VSlog.i(TAG, "openFingerprintUnlock == " + openFingerprintUnlock);
            if (!openFaceUnlock && !openFingerprintUnlock && !StorageManager.isFileEncryptedNativeOrEmulated()) {
                VSlog.i(TAG, "Conditions are not satisfied,put db 0");
                putIntFromSettingsGlobal("com.bbk.reboot.notify.lock", 0);
            } else if (notifyValue == 1) {
                boolean result = SecurityStringOperateUtil.getInstance().writeString(getVivoSavePassword(0));
                VSlog.i(TAG, "k Write == " + result);
                if (!result) {
                    VSlog.e(TAG, "k write fail,put db 0");
                    putIntFromSettingsGlobal("com.bbk.reboot.notify.lock", 0);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: getVPassword */
    public void lambda$BinderService_handleSpecialReboot$0$VivoLockSettingsServiceImpl() {
        int callingUid = Binder.getCallingUid();
        int currentUserId = UserHandle.getCallingUserId();
        if (callingUid != 1000) {
            VSlog.w(TAG, "callingUid is " + callingUid);
            resetSpecialRebootValue();
            return;
        }
        int notifyLockValue = getIntFromSettingsGlobal("com.bbk.reboot.notify.lock", -1);
        int updateSilentValue = getIntFromSettingsGlobal("com.bbk.updater.silent", 0);
        VSlog.i(TAG, "notifyLockValue:" + notifyLockValue + ", updateSilentValue: " + updateSilentValue);
        if (notifyLockValue == 0) {
            VSlog.i(TAG, "get db is 0,return");
            SecurityStringOperateUtil.getInstance().removeString();
            SystemProperties.set("persist.vivo.lock.reboot", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            putIntFromSettingsGlobal("com.bbk.updater.silent", 0);
            return;
        }
        if (notifyLockValue == -1 && updateSilentValue == 1) {
            VSlog.w(TAG, "silent up from old plan version.");
            saveSpecialRebootLog("silentUp_old2new");
        } else if (notifyLockValue == 1) {
            String rebootReason = SystemProperties.get("persist.vivo.lock.reboot", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            VSlog.i(TAG, "lock_reboot_property is " + rebootReason);
            saveSpecialRebootLog("lock_reboot_prop_" + rebootReason);
            if (rebootReason.equals("silent") || rebootReason.equals("silent_update")) {
                VSlog.i(TAG, "get db is 1, reason is " + rebootReason);
            } else {
                resetSpecialRebootValue();
                return;
            }
        } else {
            return;
        }
        String tempPwd = SecurityStringOperateUtil.getInstance().getString();
        if (TextUtils.isEmpty(tempPwd)) {
            VSlog.e(TAG, "vp empty.");
        }
        if (!TextUtils.isEmpty(tempPwd)) {
            try {
                VSlog.i(TAG, "currentUserId == " + currentUserId);
                LockscreenCredential credential = getCredential(currentUserId, tempPwd);
                VerifyCredentialResponse response = this.mLocksettingsservice.checkCredential(credential, currentUserId, (ICheckCredentialProgressCallback) null);
                if (response != null && response.getResponseCode() == 0) {
                    saveSpecialRebootLog("auLock_ok");
                    this.mLocksettingsservice.userPresent(currentUserId);
                    VSlog.i(TAG, "report unlock now.");
                    retainVivoSavePassword(tempPwd, currentUserId);
                }
            } catch (Exception e) {
                VSlog.e(TAG, "clear failed in LSS.", e.fillInStackTrace());
            }
        }
        resetSpecialRebootValue();
        boolean result = SecurityStringOperateUtil.getInstance().removeString();
        if (!result) {
            VSlog.w(TAG, "k remove fail,try again");
            SecurityStringOperateUtil.getInstance().removeString();
            return;
        }
        VSlog.i(TAG, "k remove success");
    }

    public void BinderService_handleSpecialReboot() {
        this.mDatabaseTempHandler.post(new Runnable() { // from class: com.android.server.locksettings.-$$Lambda$VivoLockSettingsServiceImpl$s83ul-C31hhRgio0uKv-RxsWKYI
            @Override // java.lang.Runnable
            public final void run() {
                VivoLockSettingsServiceImpl.this.lambda$BinderService_handleSpecialReboot$0$VivoLockSettingsServiceImpl();
            }
        });
    }

    private void resetSpecialRebootValue() {
        putIntFromSettingsGlobal("com.bbk.reboot.notify.lock", 0);
        SystemProperties.set("persist.vivo.lock.reboot", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        putIntFromSettingsGlobal("com.bbk.updater.silent", 0);
        VSlog.w(TAG, "resetSpecialRebootValue");
    }

    private void saveSpecialRebootLog(String log) {
        String specialLog = System.currentTimeMillis() + "_u" + UserHandle.getCallingUserId() + "_" + log;
        SystemProperties.set(VIVO_LOCK_REBOOT_SPLOG_PROPERTY, specialLog);
    }

    private int getIntFromSettingsGlobal(String field, int def) {
        try {
            int tempValue = Settings.Global.getInt(this.mContext.getContentResolver(), field, def);
            return tempValue;
        } catch (Exception e) {
            VSlog.e(TAG, "catch exception in getIntFromSettingsGlobal: " + e.toString());
            return def;
        }
    }

    private boolean putIntFromSettingsGlobal(String field, int value) {
        try {
            boolean tempValue = Settings.Global.putInt(this.mContext.getContentResolver(), field, value);
            return tempValue;
        } catch (Exception e) {
            VSlog.e(TAG, "catch exception in putIntFromSettingsGlobal: " + e.toString());
            return false;
        }
    }

    private int getIntFromSettingsSystem(String field, int def) {
        try {
            int tempValue = Settings.System.getInt(this.mContext.getContentResolver(), field, def);
            return tempValue;
        } catch (Exception e) {
            VSlog.e(TAG, "catch exception in getIntFromSettingsSystem: " + e.toString());
            return def;
        }
    }

    private int getIntFromSettingsSecure(String field, int def) {
        try {
            int tempValue = Settings.Secure.getInt(this.mContext.getContentResolver(), field, def);
            return tempValue;
        } catch (Exception e) {
            VSlog.e(TAG, "catch exception in getIntFromSettingsSecure: " + e.toString());
            return def;
        }
    }

    private LockscreenCredential getCredential(int userId, String pwd) {
        if (TextUtils.isEmpty(pwd)) {
            return LockscreenCredential.createNone();
        }
        int credentialType = this.mLocksettingsservice.getCredentialTypeInternal(userId);
        if (credentialType == 1) {
            LockscreenCredential credential = LockscreenCredential.createPattern(LockPatternUtils.byteArrayToPattern(pwd.getBytes()));
            return credential;
        } else if (credentialType == 3) {
            LockscreenCredential credential2 = LockscreenCredential.createPin(pwd);
            return credential2;
        } else if (credentialType == 4) {
            LockscreenCredential credential3 = LockscreenCredential.createPassword(pwd);
            return credential3;
        } else {
            VSlog.w(TAG, String.format("Failed to get credential, user: %d, credentialType: %d", Integer.valueOf(userId), Integer.valueOf(credentialType)));
            LockscreenCredential credential4 = LockscreenCredential.createNone();
            return credential4;
        }
    }

    public void systemReadyJob() {
        this.mDatabaseTempHandler.post(new Runnable() { // from class: com.android.server.locksettings.VivoLockSettingsServiceImpl.1
            @Override // java.lang.Runnable
            public void run() {
                String lockSettingsReboot = SystemProperties.get("persist.vivo.LockSettings.reboot", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                if (!TextUtils.isEmpty(lockSettingsReboot)) {
                    VSlog.d(VivoLockSettingsServiceImpl.TAG, lockSettingsReboot + ",spblob has been recreated and rebooted before.");
                    SystemProperties.set("persist.vivo.LockSettings.reboot", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                }
            }
        });
    }

    public void retainVivoSavePassword(String password, int userId) {
        if (userId != 0) {
            VSlog.w(TAG, "won't retain vp, non system userId: " + userId);
            return;
        }
        SecretKey secretKey = getSecretKey();
        if (secretKey == null || TextUtils.isEmpty(password)) {
            this.mVivoSavePasswordCache.delete(userId);
            Object[] objArr = new Object[2];
            objArr[0] = Boolean.valueOf(secretKey == null);
            objArr[1] = Boolean.valueOf(TextUtils.isEmpty(password));
            VSlog.w(TAG, String.format("won't retain vp, null secretKey: %b, null pwd: %b", objArr));
            return;
        }
        byte[] result = null;
        try {
            result = SyntheticPasswordCrypto.encrypt(secretKey, password.getBytes());
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | InvalidParameterSpecException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            VSlog.e(TAG, "Failed to encrypt vp", e);
        }
        if (result == null) {
            this.mVivoSavePasswordCache.delete(userId);
        } else {
            this.mVivoSavePasswordCache.put(userId, result);
        }
    }

    private String getVivoSavePassword(int userId) {
        byte[] encryptedVP = this.mVivoSavePasswordCache.get(userId);
        if (encryptedVP == null) {
            VSlog.w(TAG, "encryptedVP is empty, just return.");
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        SecretKey secretKey = getSecretKey();
        if (secretKey == null) {
            this.mVivoSavePasswordCache.delete(userId);
            VSlog.w(TAG, "secret key is null, just return.");
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        byte[] decryptedVP = null;
        try {
            decryptedVP = SyntheticPasswordCrypto.decrypt(secretKey, encryptedVP);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            this.mVivoSavePasswordCache.delete(userId);
            VSlog.e(TAG, "Failed to decrypt VP", e);
        }
        if (decryptedVP == null) {
            this.mVivoSavePasswordCache.delete(userId);
            VSlog.e(TAG, "decryptedVP is null");
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        String result = new String(decryptedVP);
        return result;
    }

    public void sanitizeVivoSavePassword(int userId) {
        this.mVivoSavePasswordCache.delete(userId);
    }

    private SecretKey getSecretKey() {
        SecretKey secretKey = null;
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey2 = (SecretKey) keyStore.getKey(VP_ENCRYPT_KEY, null);
            if (secretKey2 != null) {
                return secretKey2;
            }
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256, new SecureRandom());
            secretKey = keyGenerator.generateKey();
            keyStore.load(null);
            KeyProtection.Builder builder = new KeyProtection.Builder(3).setBlockModes("GCM").setEncryptionPaddings("NoPadding").setCriticalToDeviceEncryption(true);
            keyStore.setEntry(VP_ENCRYPT_KEY, new KeyStore.SecretKeyEntry(secretKey), builder.build());
            return secretKey;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException e) {
            deleteSecretKey(keyStore, VP_ENCRYPT_KEY);
            VSlog.e(TAG, "Failed to get secret key", e);
            return secretKey;
        }
    }

    private void deleteSecretKey(KeyStore keyStore, String keyAlias) {
        if (keyStore == null) {
            return;
        }
        try {
            keyStore.deleteEntry(keyAlias);
            VSlog.i(TAG, "VP key deleted: " + keyAlias);
        } catch (KeyStoreException e) {
            VSlog.e(TAG, "Failed to delete VP key", e);
        }
    }

    public void unlockManagedProfile(int userId, int challengeType, long challenge, ArrayList<LockSettingsService.PendingResetLockout> resetLockouts) {
        if (!PERFORM_ONCE_FLAG && userId == 999 && this.mLocksettingsservice.hasUnifiedChallenge(userId) && !this.mUsermanager.isUserUnlockingOrUnlocked(userId)) {
            int retryTime = 0;
            while (true) {
                if (retryTime >= 3) {
                    break;
                } else if (this.mUsermanager.isUserRunning(userId)) {
                    this.mLocksettingsservice.unlockChildProfile(userId, false, challengeType, challenge, resetLockouts);
                    VSlog.w(TAG, "user " + userId + "unlock successful. retryTime ==  " + retryTime);
                    break;
                } else {
                    try {
                        VSlog.w(TAG, "user " + userId + " is not running yet, sleep for 1s. retryTime ==  " + retryTime);
                        Thread.sleep(1000L);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    retryTime++;
                }
            }
            PERFORM_ONCE_FLAG = true;
        }
    }

    public void retainPwdInCheckCredential(byte[] credential, VerifyCredentialResponse response, int userId, ICheckCredentialProgressCallback progressCallback) {
        FingerprintKeyguardInternal fingerprintKeyguard;
        if (response.getResponseCode() == 0) {
            if (FtBuild.isQCOMPlatform() && userId == 0) {
                this.mLocksettingsservice.retainPassword(credential == null ? null : new String(credential));
            }
            retainVivoSavePassword(credential != null ? new String(credential) : null, userId);
            if (progressCallback != null && (fingerprintKeyguard = (FingerprintKeyguardInternal) LocalServices.getService(FingerprintKeyguardInternal.class)) != null) {
                fingerprintKeyguard.onCredentialVerified(0);
            }
        }
    }

    public void onCredentialVerified() {
        FingerprintKeyguardInternal fingerprintKeyguard = (FingerprintKeyguardInternal) LocalServices.getService(FingerprintKeyguardInternal.class);
        if (fingerprintKeyguard != null) {
            fingerprintKeyguard.onCredentialVerified(1);
        }
    }
}