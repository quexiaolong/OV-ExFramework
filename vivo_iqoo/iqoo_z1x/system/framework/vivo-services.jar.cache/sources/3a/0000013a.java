package com.android.server.backup;

import android.util.StringBuilderPrinter;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import vivo.app.backup.AbsVivoBackupManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoAppMetadataBackupWriterImpl implements IVivoAppMetadataBackupWriter {
    private static final String TAG = "VIVO_BACKUP_VivoAppMetadataBackupWriterImpl";

    public void writeBackupDualFlags(StringBuilderPrinter printer, String pkg) {
        int fd;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (fd = vivoBackupManager.getFdByPkg(pkg)) != -1 && vivoBackupManager.isRunningFromVivoBackup(fd)) {
            boolean isDualUser = vivoBackupManager.isDualUserByPkg(pkg);
            VSlog.i(TAG, " write backup dual flag: pkg = " + pkg + " isDualUser = " + isDualUser);
            printer.println(isDualUser ? "1" : "0");
        }
    }
}