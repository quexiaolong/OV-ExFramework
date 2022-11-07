package com.android.server.locksettings;

import com.android.internal.widget.ICheckCredentialProgressCallback;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.locksettings.LockSettingsService;
import java.util.ArrayList;

/* loaded from: classes.dex */
public interface IVivoLockSettingsService {
    public static final String BBK_REBOOT_NOTIFY_LOCK_VALUE = "com.bbk.reboot.notify.lock";
    public static final String BBK_UPDATE_SILENT_VALUE = "com.bbk.updater.silent";
    public static final String VIVO_LOCK_REBOOT_PROPERTY = "persist.vivo.lock.reboot";

    /* loaded from: classes.dex */
    public interface IVivoLSSExport {
        IVivoLockSettingsService getVivoInjectInstance();
    }

    void BinderService_handleSpecialReboot();

    void initialization();

    void onCredentialVerified();

    void retainPwdInCheckCredential(byte[] bArr, VerifyCredentialResponse verifyCredentialResponse, int i, ICheckCredentialProgressCallback iCheckCredentialProgressCallback);

    void retainVivoSavePassword(String str, int i);

    void sanitizeVivoSavePassword(int i);

    void systemReadyJob();

    void unlockManagedProfile(int i, int i2, long j, ArrayList<LockSettingsService.PendingResetLockout> arrayList);
}