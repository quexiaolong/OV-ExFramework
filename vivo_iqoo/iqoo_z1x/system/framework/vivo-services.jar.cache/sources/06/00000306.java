package com.android.server.locksettings;

import android.content.Context;
import android.content.pm.UserInfo;
import android.hardware.biometrics.fingerprint.FingerprintKeyguardInternal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.service.gatekeeper.IGateKeeperService;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.LocalServices;
import com.android.server.locksettings.LockSettingsStorage;
import com.android.server.locksettings.SyntheticPasswordManager;
import com.vivo.face.common.data.Constants;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import com.vivo.services.rms.ProcessList;
import com.vivo.vcodetransbase.EventTransfer;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.Switch;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoSyntheticPasswordManagerImpl implements IVivoSyntheticPasswordManager {
    private static final String SWITCH_FILE = "/data/bbkcore/LockSettings_spblob_1.0.xml";
    private static final String SWITCH_NAME = "spblob_swtich";
    private static final String TAG = "VivoSyntheticPasswordManagerImpl";
    private AbsConfigurationManager mConfigurationManager;
    private Handler mHandler;
    private LockSettingsStorage mLockSettingsStorage;
    private AbsVivoPerfManager mPerfUnlock;
    private SyntheticPasswordManager mSpManager;
    private Switch mSwitch;
    private UserManager mUserManager;
    private static final String SP_BLOB_NAME = "spblob";
    private static final String SECDISCARDABLE_NAME = "secdis";
    private static final String PASSWORD_DATA_NAME = "pwd";
    private static final String PASSWORD_METRICS_NAME = "metrics";
    private static final String SP_HANDLE_NAME = "handle";
    static final String[] STATE_NAMES = {SP_BLOB_NAME, SECDISCARDABLE_NAME, PASSWORD_DATA_NAME, PASSWORD_METRICS_NAME, SP_HANDLE_NAME};
    private HandlerThread mThread = new HandlerThread("SpManager_Thread");
    private ConfigurationObserver onConfigChangeListener = new ConfigurationObserver() { // from class: com.android.server.locksettings.VivoSyntheticPasswordManagerImpl.1
        public void onConfigChange(String file, String name) {
            try {
                if (VivoSyntheticPasswordManagerImpl.this.mConfigurationManager != null) {
                    VivoSyntheticPasswordManagerImpl.this.mHandler.post(new Runnable() { // from class: com.android.server.locksettings.VivoSyntheticPasswordManagerImpl.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            VivoSyntheticPasswordManagerImpl.this.mSwitch = VivoSyntheticPasswordManagerImpl.this.mConfigurationManager.getSwitch(VivoSyntheticPasswordManagerImpl.SWITCH_FILE, VivoSyntheticPasswordManagerImpl.SWITCH_NAME);
                            VivoSyntheticPasswordManagerImpl.this.setSpblobProperty(VivoSyntheticPasswordManagerImpl.this.mSwitch);
                        }
                    });
                } else {
                    VSlog.i(VivoSyntheticPasswordManagerImpl.TAG, "ConfigurationManager is null");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public VivoSyntheticPasswordManagerImpl(LockSettingsStorage storage, UserManager um, SyntheticPasswordManager spManager) {
        this.mPerfUnlock = null;
        this.mLockSettingsStorage = storage;
        this.mUserManager = um;
        this.mSpManager = spManager;
        this.mThread.start();
        this.mHandler = new Handler(this.mThread.getLooper());
        initBackupSpblob();
        initSwitch();
        reportCorruptedFiles();
        this.mPerfUnlock = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
    }

    public byte[] loadState(String stateName, long handle, int userId) {
        UserInfo userinfo;
        boolean suffixRight = stateName.equals(PASSWORD_DATA_NAME);
        byte[] result = this.mLockSettingsStorage.readSyntheticPasswordState(userId, handle, stateName);
        if (suffixRight && result == null && (userinfo = this.mUserManager.getUserInfo(userId)) != null && userinfo.isManagedProfile()) {
            VSlog.d(TAG, "loadState file: " + handle + "." + stateName + ",userId " + userId);
            StringBuilder sb = new StringBuilder();
            sb.append(handle);
            sb.append(".");
            sb.append(stateName);
            sb.append(" miss,userId ");
            sb.append(userId);
            SystemProperties.set("persist.vivo.LockSettings.reboot", sb.toString());
            this.mLockSettingsStorage.writeKeyValue("sp-handle", Long.toString(0L), userId);
            this.mLockSettingsStorage.removeChildProfileLock(userId);
            this.mLockSettingsStorage.writeCredentialHash(LockSettingsStorage.CredentialHash.createEmptyHash(), userId);
            SystemProperties.set("sys.powerctl", "reboot," + stateName + "Miss");
            try {
                Thread.sleep(20000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            VSlog.wtf(TAG, "Unexpected return from " + stateName + "MissReboot!");
        }
        return result;
    }

    public void beginUnwrapSpBlob(int userId, long handle) {
        VSlog.d(TAG, "begin unwrapSyntheticPasswordBlob. userId: " + userId + "handle: " + handle);
    }

    public VerifyCredentialResponse handleAuthTokenResult(SyntheticPasswordManager.AuthenticationResult result, IGateKeeperService gatekeeper, int userId) {
        if (result.authToken == null) {
            VSlog.w(TAG, "result.authToken == null");
            return VerifyCredentialResponse.ERROR;
        }
        return this.mSpManager.verifyChallenge(gatekeeper, result.authToken, 0L, userId);
    }

    public void onCredentialVerified() {
        FingerprintKeyguardInternal fingerprintKeyguard = (FingerprintKeyguardInternal) LocalServices.getService(FingerprintKeyguardInternal.class);
        if (fingerprintKeyguard != null) {
            fingerprintKeyguard.onCredentialVerified(1);
        }
    }

    public void saveBackupAndHash(final String stateName, final byte[] data, final long handle, final int userId) {
        if (userId != 0 && userId != 999) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.locksettings.-$$Lambda$VivoSyntheticPasswordManagerImpl$dZYfxEQN0MeKtED5emIEkrqBD04
            @Override // java.lang.Runnable
            public final void run() {
                VivoSyntheticPasswordManagerImpl.this.lambda$saveBackupAndHash$0$VivoSyntheticPasswordManagerImpl(stateName, data, handle, userId);
            }
        });
    }

    public void destroyBackupAndHash(final String stateName, final long handle, final int userId) {
        if (userId != 0 && userId != 999) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.locksettings.-$$Lambda$VivoSyntheticPasswordManagerImpl$9_P6oYr5jIICP70Exm9zUea_YFg
            @Override // java.lang.Runnable
            public final void run() {
                VivoSyntheticPasswordManagerImpl.this.lambda$destroyBackupAndHash$1$VivoSyntheticPasswordManagerImpl(stateName, handle, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: saveBackupAndHashInternal */
    public void lambda$saveBackupAndHash$0$VivoSyntheticPasswordManagerImpl(String stateName, byte[] data, long handle, int userId) {
        try {
            String fileName = String.format("%016x.%s", Long.valueOf(handle), stateName);
            File hashDir = LockSettingsStorage.getSpblobHashDir(userId);
            hashDir.mkdir();
            File stateHash = new File(hashDir, fileName);
            if (!stateHash.exists()) {
                byte[] hashContent = SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALISATION_SPBLOBHASH, new byte[][]{data});
                LockSettingsStorage.writeNoBufferedFile(stateHash, hashContent);
                if (!Arrays.equals(LockSettingsStorage.readNoBufferedFile(stateHash), hashContent)) {
                    LockSettingsStorage.writeNoBufferedFile(stateHash, hashContent);
                }
            }
            File backupDir = LockSettingsStorage.getSpblobBackupDir(userId);
            backupDir.mkdir();
            File stateBackup = new File(backupDir, fileName);
            if (!stateBackup.exists()) {
                LockSettingsStorage.writeNoBufferedFile(stateBackup, data);
                if (!Arrays.equals(LockSettingsStorage.readNoBufferedFile(stateBackup), data)) {
                    LockSettingsStorage.writeNoBufferedFile(stateBackup, data);
                }
            }
        } catch (Exception e) {
            VSlog.e(TAG, "Catch exception when saveStateHash", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: destroyBackupAndHashInternal */
    public void lambda$destroyBackupAndHash$1$VivoSyntheticPasswordManagerImpl(String stateName, long handle, int userId) {
        File hashDir = LockSettingsStorage.getSpblobHashDir(userId);
        File backupDir = LockSettingsStorage.getSpblobBackupDir(userId);
        String fileName = String.format("%016x.%s", Long.valueOf(handle), stateName);
        File hashFile = new File(hashDir, fileName);
        File backupFile = new File(backupDir, fileName);
        try {
            hashFile.delete();
        } catch (Exception e) {
            VSlog.e(TAG, "Catch exception when delete: " + hashFile.getAbsolutePath());
        }
        try {
            backupFile.delete();
        } catch (Exception e2) {
            VSlog.e(TAG, "Catch exception when delete: " + backupFile.getAbsolutePath());
        }
    }

    private void initBackupSpblob() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.locksettings.-$$Lambda$VivoSyntheticPasswordManagerImpl$g1TQ4ASwZbU2oQOMfRMEJ7Bekh0
            @Override // java.lang.Runnable
            public final void run() {
                VivoSyntheticPasswordManagerImpl.this.lambda$initBackupSpblob$2$VivoSyntheticPasswordManagerImpl();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: initBackupSpblobInternal */
    public void lambda$initBackupSpblob$2$VivoSyntheticPasswordManagerImpl() {
        initBackupSpblobPerUser(0);
        initBackupSpblobPerUser(ProcessList.CACHED_APP_MAX_ADJ);
    }

    private void initBackupSpblobPerUser(int userId) {
        long handle;
        int i;
        int i2;
        VSlog.d(TAG, "Begin to initBackupSpblob, userId: " + userId);
        File spblobDir = LockSettingsStorage.getSpblobDir(userId);
        File hashDir = LockSettingsStorage.getSpblobHashDir(userId);
        File backupDir = LockSettingsStorage.getSpblobBackupDir(userId);
        try {
            handle = Long.parseLong(this.mLockSettingsStorage.readKeyValue("sp-handle", "0", userId));
        } catch (Exception e) {
            VSlog.e(TAG, "Catch exception when initializing hash file.", e);
        }
        if (!spblobDir.exists()) {
            return;
        }
        long j = 0;
        if (handle == 0) {
            VSlog.w(TAG, "spblob dir exists, but handle is 0.");
            return;
        }
        backupDir.mkdir();
        hashDir.mkdir();
        String[] strArr = STATE_NAMES;
        int length = strArr.length;
        int i3 = 0;
        while (i3 < length) {
            String stateName = strArr[i3];
            long tempHandle = SP_HANDLE_NAME.equals(stateName) ? j : handle;
            String fileName = String.format("%016x.%s", Long.valueOf(tempHandle), stateName);
            String[] strArr2 = strArr;
            File oldHash = new File(spblobDir, fileName + "hash");
            if (oldHash.exists()) {
                oldHash.delete();
            }
            File stateFile = new File(spblobDir, fileName);
            if (!stateFile.exists()) {
                i = i3;
                i2 = length;
            } else {
                i = i3;
                i2 = length;
                lambda$saveBackupAndHash$0$VivoSyntheticPasswordManagerImpl(stateName, LockSettingsStorage.readNoBufferedFile(stateFile), tempHandle, userId);
            }
            i3 = i + 1;
            strArr = strArr2;
            length = i2;
            j = 0;
        }
        VSlog.d(TAG, "End to initBackupSpblob, userId:" + userId);
    }

    private void initSwitch() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.locksettings.-$$Lambda$VivoSyntheticPasswordManagerImpl$7yfyycH5yNlfCmmVapgD_xMb9Ak
            @Override // java.lang.Runnable
            public final void run() {
                VivoSyntheticPasswordManagerImpl.this.lambda$initSwitch$3$VivoSyntheticPasswordManagerImpl();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: initSwitchInternal */
    public void lambda$initSwitch$3$VivoSyntheticPasswordManagerImpl() {
        VivoFrameworkFactory vivoFrameworkFactory = VivoFrameworkFactory.getFrameworkFactoryImpl();
        if (vivoFrameworkFactory != null) {
            AbsConfigurationManager configurationManager = vivoFrameworkFactory.getConfigurationManager();
            this.mConfigurationManager = configurationManager;
            Switch r1 = configurationManager.getSwitch(SWITCH_FILE, SWITCH_NAME);
            this.mSwitch = r1;
            setSpblobProperty(r1);
            this.mConfigurationManager.registerObserver(this.mSwitch, this.onConfigChangeListener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSpblobProperty(Switch s) {
        if (s != null && !s.isUninitialized() && s.isOn()) {
            SystemProperties.set("persist.vivo.spblobrestore.enable", "true");
        } else if (s != null && !s.isUninitialized() && !s.isOn()) {
            SystemProperties.set("persist.vivo.spblobrestore.enable", "false");
        }
    }

    private void reportCorruptedFiles() {
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.locksettings.-$$Lambda$VivoSyntheticPasswordManagerImpl$TMRpX4nvgAR5Umek66cl3fKrlG0
            @Override // java.lang.Runnable
            public final void run() {
                VivoSyntheticPasswordManagerImpl.this.lambda$reportCorruptedFiles$4$VivoSyntheticPasswordManagerImpl();
            }
        }, VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: reportCorruptedFilesInternal */
    public void lambda$reportCorruptedFiles$4$VivoSyntheticPasswordManagerImpl() {
        if (LockSettingsService.sCorruptedFiles.isEmpty()) {
            return;
        }
        HashMap<String, String> params = new HashMap<>();
        params.put("verify_result", LockSettingsService.sCorruptedFiles.toString());
        try {
            EventTransfer.getInstance().singleEvent("F323", "F323|10001", System.currentTimeMillis(), 0L, params);
        } catch (Exception e) {
            VSlog.w(TAG, "catch exception when transfer event.", e);
        }
    }

    public void perfBoost(int userId) {
        AbsVivoPerfManager absVivoPerfManager;
        if (userId == 0 && StorageManager.isFileEncryptedNativeOrEmulated() && !StorageManager.isUserKeyUnlocked(0) && (absVivoPerfManager = this.mPerfUnlock) != null) {
            absVivoPerfManager.perfHint(4229, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, -1, 10);
        }
    }
}