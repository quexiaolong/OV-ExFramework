package com.android.server.backup;

import com.vivo.services.backup.util.VivoBackupCommonUtil;
import vivo.app.backup.AbsVivoBackupManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoRestoreUtilsImpl implements IVivoRestoreUtils {
    private static final String TAG = "VIVO_BACKUP_VivoRestoreUtilsImpl";

    public boolean isRunningFromVivoBackupByPkg(String pkg) {
        int fd;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (fd = vivoBackupManager.getFdByPkg(pkg)) != -1) {
            VSlog.d(TAG, "isRunningFromVivoBackupByPkg fd = " + fd + ", pkg = " + pkg);
            return vivoBackupManager.isRunningFromVivoBackup(fd);
        }
        return false;
    }
}