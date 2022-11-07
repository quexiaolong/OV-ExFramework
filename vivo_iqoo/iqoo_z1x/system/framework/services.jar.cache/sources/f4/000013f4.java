package com.android.server.oemlock;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.persistentdata.PersistentDataBlockManager;
import android.util.Slog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class PersistentDataBlockLock extends OemLock {
    private static final String TAG = "OemLock";
    private Context mContext;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PersistentDataBlockLock(Context context) {
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.oemlock.OemLock
    public String getLockName() {
        return "";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.oemlock.OemLock
    public void setOemUnlockAllowedByCarrier(boolean allowed, byte[] signature) {
        if (signature != null) {
            Slog.w(TAG, "Signature provided but is not being used");
        }
        UserManager.get(this.mContext).setUserRestriction("no_oem_unlock", !allowed, UserHandle.SYSTEM);
        if (!allowed) {
            disallowUnlockIfNotUnlocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.oemlock.OemLock
    public boolean isOemUnlockAllowedByCarrier() {
        return !UserManager.get(this.mContext).hasUserRestriction("no_oem_unlock", UserHandle.SYSTEM);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.oemlock.OemLock
    public void setOemUnlockAllowedByDevice(boolean allowedByDevice) {
        PersistentDataBlockManager pdbm = (PersistentDataBlockManager) this.mContext.getSystemService("persistent_data_block");
        if (pdbm == null) {
            Slog.w(TAG, "PersistentDataBlock is not supported on this device");
        } else {
            pdbm.setOemUnlockEnabled(allowedByDevice);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.oemlock.OemLock
    public boolean isOemUnlockAllowedByDevice() {
        PersistentDataBlockManager pdbm = (PersistentDataBlockManager) this.mContext.getSystemService("persistent_data_block");
        if (pdbm == null) {
            Slog.w(TAG, "PersistentDataBlock is not supported on this device");
            return false;
        }
        return pdbm.getOemUnlockEnabled();
    }

    private void disallowUnlockIfNotUnlocked() {
        PersistentDataBlockManager pdbm = (PersistentDataBlockManager) this.mContext.getSystemService("persistent_data_block");
        if (pdbm == null) {
            Slog.w(TAG, "PersistentDataBlock is not supported on this device");
        } else if (pdbm.getFlashLockState() != 0) {
            pdbm.setOemUnlockEnabled(false);
        }
    }
}