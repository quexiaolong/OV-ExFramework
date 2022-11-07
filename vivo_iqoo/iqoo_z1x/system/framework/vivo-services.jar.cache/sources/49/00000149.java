package com.android.server.backup;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.StringBuilderPrinter;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import vivo.app.backup.AbsVivoBackupManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoUserBackupManagerServiceImpl implements IVivoUserBackupManagerService {
    private static final String TAG = "VIVO_BACKUP_VivoUserBackupManagerServiceImpl";
    private UserBackupManagerService mUserBackupManagerService;

    public VivoUserBackupManagerServiceImpl(UserBackupManagerService userBackupManagerService) {
        if (userBackupManagerService == null) {
            VSlog.i(TAG, "container is " + userBackupManagerService);
        }
        this.mUserBackupManagerService = userBackupManagerService;
    }

    public void writeBackupDualFlags(StringBuilderPrinter printer, boolean asDual, int fd) {
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && vivoBackupManager.isRunningFromVivoBackup(fd)) {
            printer.println(asDual ? "1" : "0");
        }
    }

    public boolean isRunningFromVivoBackup(int fd) {
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null) {
            return vivoBackupManager.isRunningFromVivoBackup(fd);
        }
        return false;
    }

    public boolean isDualPackageEnabled(String pkgName, int fd) {
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && vivoBackupManager.isRunningFromVivoBackup(fd)) {
            return vivoBackupManager.isDualPackageEnabled(pkgName);
        }
        return false;
    }

    public boolean enableDualPackage(String pkgName, int fd) {
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && vivoBackupManager.isRunningFromVivoBackup(fd)) {
            return vivoBackupManager.enableDualPackage(pkgName);
        }
        return false;
    }

    public int getDualUserId(int fd) {
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && vivoBackupManager.isRunningFromVivoBackup(fd)) {
            return vivoBackupManager.getDualUserId();
        }
        return -1;
    }

    public void postRestoreCompleteSize(long size, int fd) {
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && vivoBackupManager.isRunningFromVivoBackup(fd)) {
            vivoBackupManager.postRestoreCompleteSize(size, fd);
        }
    }

    public void onError(int error, int fd) {
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && vivoBackupManager.isRunningFromVivoBackup(fd)) {
            vivoBackupManager.onError(error, fd);
        }
    }

    public void putFdByToken(int token, int fd) {
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && vivoBackupManager.isRunningFromVivoBackup(fd)) {
            VSlog.d(TAG, "putFdByToken token = " + token + " fd = " + fd);
            vivoBackupManager.putFdByToken(token, fd);
        }
    }

    public void removeToken(int token) {
        int fd;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (fd = vivoBackupManager.getFdByToken(token)) != -1 && vivoBackupManager.isRunningFromVivoBackup(fd)) {
            VSlog.d(TAG, "fd = " + fd + ", current token is " + token + ", remove token");
            vivoBackupManager.removeToken(token);
        }
    }

    public boolean isRunningFromVivoBackupByToken(int token) {
        int fd;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (fd = vivoBackupManager.getFdByToken(token)) != -1) {
            return vivoBackupManager.isRunningFromVivoBackup(fd);
        }
        return false;
    }

    public boolean startConfirmationUiFromVivoByToken(Object service, int token, String action) {
        int fd;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (fd = vivoBackupManager.getFdByToken(token)) != -1 && vivoBackupManager.isRunningFromVivoBackup(fd)) {
            VSlog.d(TAG, "startConfirmationUiFromVivoByToken fd = " + fd + ", token = " + token);
            return vivoBackupManager.startConfirmationUi(token, action, fd);
        }
        return false;
    }

    public int getDualUserIdByPkg(int userId, String pkg) {
        int fd;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null && (fd = vivoBackupManager.getFdByPkg(pkg)) != -1 && vivoBackupManager.isRunningFromVivoBackup(fd) && vivoBackupManager.isDualUserByPkg(pkg)) {
            VSlog.d(TAG, "fd = " + fd + ", current userId of pkg " + pkg + " is: " + userId);
            return vivoBackupManager.getDualUserId();
        }
        return userId;
    }

    public ApplicationInfo getApplicationInfoByPkg(ApplicationInfo app, int userId, PackageManager mPM, int flags) {
        int myUserId = userId;
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null) {
            int fd = vivoBackupManager.getFdByPkg(app.packageName);
            if (fd != -1 && vivoBackupManager.isRunningFromVivoBackup(fd) && vivoBackupManager.isDualUserByPkg(app.packageName)) {
                myUserId = vivoBackupManager.getDualUserId();
            }
            try {
                VSlog.i(TAG, "getApplicationInfoByPkg pkgName = " + app.packageName + " myUserId = " + myUserId);
                ApplicationInfo ai = mPM.getApplicationInfoAsUser(app.packageName, flags, myUserId);
                if (ai != null) {
                    return ai;
                }
            } catch (PackageManager.NameNotFoundException e) {
                VSlog.e(TAG, "connected PackageManagerService failed!");
            }
        }
        return app;
    }

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