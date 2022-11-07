package com.android.server.backup;

import android.app.IBackupAgent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import vivo.app.backup.AbsVivoBackupManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoTrampolineImpl implements IVivoTrampoline {
    public static final String TAG = "VIVO_BACKUP_VivoTrampolineImpl";
    private BackupManagerService mService;

    public void setService(Object backupManagerService) {
        this.mService = (BackupManagerService) backupManagerService;
    }

    public int getUserIdByPkg(int userId, String pkg) {
        int fd;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (fd = vivoBackupManager.getFdByPkg(pkg)) != -1 && vivoBackupManager.isRunningFromVivoBackup(fd) && userId == vivoBackupManager.getDualUserId()) {
            VSlog.d(TAG, "fd = " + fd + ", current userId of pkg " + pkg + " is: " + userId + ", treat as userId 0");
            return 0;
        }
        return userId;
    }

    public int getUserIdByToken(int userId, int token) {
        int fd;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (fd = vivoBackupManager.getFdByToken(token)) != -1 && vivoBackupManager.isRunningFromVivoBackup(fd) && userId == vivoBackupManager.getDualUserId()) {
            return 0;
        }
        return userId;
    }

    public void removeToken(int token) {
        int fd;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (fd = vivoBackupManager.getFdByToken(token)) != -1 && vivoBackupManager.isRunningFromVivoBackup(fd)) {
            VSlog.d(TAG, "fd = " + fd + ", current token is " + token + ", remove token");
            vivoBackupManager.removeToken(token);
        }
    }

    public IBackupAgent bindToAgentSynchronousFromVivo(String packageName, int mode) {
        BackupManagerService backupManagerService;
        UserBackupManagerService ubms;
        ApplicationInfo appInfo;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (backupManagerService = this.mService) != null && (ubms = backupManagerService.getServiceForUserIfCallerHasPermission(0, "bindToAgentSynchronousFromVivo()")) != null && (appInfo = getApplicationInfoFromVivo(packageName)) != null) {
            IBackupAgent agent = ubms.bindToAgentSynchronous(appInfo, mode);
            int retryTimes = 0;
            while (agent == null && retryTimes < 3) {
                retryTimes++;
                VSlog.i(TAG, "======== get agent null, try again time [" + retryTimes + "] in 5000 ========");
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                agent = ubms.bindToAgentSynchronous(appInfo, mode);
            }
            return agent;
        }
        return null;
    }

    public void tearDownAgentAndKillFromVivo(String packageName) {
        BackupManagerService backupManagerService;
        UserBackupManagerService ubms;
        ApplicationInfo appInfo;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (backupManagerService = this.mService) != null && (ubms = backupManagerService.getServiceForUserIfCallerHasPermission(0, "tearDownAgentAndKillFromVivo()")) != null && (appInfo = getApplicationInfoFromVivo(packageName)) != null) {
            ubms.tearDownAgentAndKill(appInfo);
        }
    }

    public void clearApplicationDataBeforeRestoreFromVivo(String packageName) {
        BackupManagerService backupManagerService;
        UserBackupManagerService ubms;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (backupManagerService = this.mService) != null && (ubms = backupManagerService.getServiceForUserIfCallerHasPermission(0, "tearDownAgentAndKillFromVivo()")) != null) {
            ubms.clearApplicationDataBeforeRestore(packageName);
        }
    }

    public ApplicationInfo getApplicationInfoFromVivo(String packageName) {
        BackupManagerService backupManagerService;
        UserBackupManagerService ubms;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (backupManagerService = this.mService) != null && (ubms = backupManagerService.getServiceForUserIfCallerHasPermission(0, "getApplicationInfoFromVivo()")) != null) {
            int userId = 0;
            if (vivoBackupManager.isDualUserByPkg(packageName)) {
                userId = vivoBackupManager.getDualUserId();
            }
            try {
                VSlog.i(TAG, "getApplicationInfoFromVivo pkgName = " + packageName + " userId = " + userId);
                return ubms.getContext().getPackageManager().getApplicationInfoAsUser(packageName, 0, userId);
            } catch (PackageManager.NameNotFoundException e) {
                VSlog.e(TAG, "getApplicationInfoFromVivo name not found pkg = " + packageName, e);
                return null;
            }
        }
        return null;
    }
}