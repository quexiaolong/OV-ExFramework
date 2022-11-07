package com.android.server.backup;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.StringBuilderPrinter;

/* loaded from: classes.dex */
public interface IVivoUserBackupManagerService {
    boolean enableDualPackage(String str, int i);

    ApplicationInfo getApplicationInfoByPkg(ApplicationInfo applicationInfo, int i, PackageManager packageManager, int i2);

    int getDualUserId(int i);

    int getDualUserIdByPkg(int i, String str);

    boolean isDualPackageEnabled(String str, int i);

    boolean isRunningFromVivoBackup(int i);

    boolean isRunningFromVivoBackupByPkg(String str);

    boolean isRunningFromVivoBackupByToken(int i);

    void onError(int i, int i2);

    void postRestoreCompleteSize(long j, int i);

    void putFdByToken(int i, int i2);

    void removeToken(int i);

    boolean startConfirmationUiFromVivoByToken(Object obj, int i, String str);

    void writeBackupDualFlags(StringBuilderPrinter stringBuilderPrinter, boolean z, int i);

    /* loaded from: classes.dex */
    public interface IVivoUserBackupManagerServiceExport {
        IVivoUserBackupManagerService getVivoInjectInstance();

        default void writeBackupDualFlags(StringBuilderPrinter printer, boolean asDual, int fd) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().writeBackupDualFlags(printer, asDual, fd);
            }
        }

        default boolean isRunningFromVivoBackup(int fd) {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().isRunningFromVivoBackup(fd);
            }
            return false;
        }

        default boolean isDualPackageEnabled(String pkgName, int fd) {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().isDualPackageEnabled(pkgName, fd);
            }
            return false;
        }

        default boolean enableDualPackage(String pkgName, int fd) {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().enableDualPackage(pkgName, fd);
            }
            return false;
        }

        default int getDualUserId(int fd) {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().getDualUserId(fd);
            }
            return -1;
        }

        default void postRestoreCompleteSize(long size, int fd) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().postRestoreCompleteSize(size, fd);
            }
        }

        default void onError(int error, int fd) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().onError(error, fd);
            }
        }

        default void putFdByToken(int token, int fd) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().putFdByToken(token, fd);
            }
        }

        default void removeToken(int token) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().removeToken(token);
            }
        }

        default boolean isRunningFromVivoBackupByToken(int token) {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().isRunningFromVivoBackupByToken(token);
            }
            return false;
        }

        default ApplicationInfo getApplicationInfoByPkg(ApplicationInfo app, int userId, PackageManager mPM, int flags) throws PackageManager.NameNotFoundException {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().getApplicationInfoByPkg(app, userId, mPM, flags);
            }
            return app;
        }
    }
}