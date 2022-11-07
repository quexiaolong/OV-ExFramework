package com.android.server.backup;

import android.os.Environment;
import java.io.File;

/* loaded from: classes.dex */
final class UserBackupManagerFiles {
    private static final String BACKUP_PERSISTENT_DIR = "backup";
    private static final String BACKUP_STAGING_DIR = "backup_stage";

    UserBackupManagerFiles() {
    }

    private static File getBaseDir(int userId) {
        return Environment.getDataSystemCeDirectory(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static File getBaseStateDir(int userId) {
        if (userId != 0) {
            return new File(getBaseDir(userId), "backup");
        }
        return new File(Environment.getDataDirectory(), "backup");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static File getDataDir(int userId) {
        if (userId != 0) {
            return new File(getBaseDir(userId), BACKUP_STAGING_DIR);
        }
        return new File(Environment.getDownloadCacheDirectory(), BACKUP_STAGING_DIR);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static File getStateDirInSystemDir(int userId) {
        File baseStateDir = getBaseStateDir(0);
        return new File(baseStateDir, "" + userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static File getStateFileInSystemDir(String filename, int userId) {
        return new File(getStateDirInSystemDir(userId), filename);
    }
}