package com.android.server.backup;

import android.content.pm.ApplicationInfo;
import com.android.server.backup.restore.FullRestoreEngine;
import com.android.server.backup.restore.RestorePolicy;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import vivo.app.backup.AbsVivoBackupManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoTarBackupReaderImpl implements IVivoTarBackupReader {
    private static final String TAG = "VIVO_BACKUP_VivoTarBackupReaderImpl";
    private int mFd;
    private FullRestoreEngine mRestoreEngine;
    private boolean mRunFromVivo;
    private ApplicationInfo mTargetApp;

    public void setUp(Object fullRestoreEngine, ApplicationInfo targetApp) {
        FullRestoreEngine fullRestoreEngine2 = (FullRestoreEngine) fullRestoreEngine;
        this.mRestoreEngine = fullRestoreEngine2;
        this.mFd = fullRestoreEngine2.getFd();
        this.mRunFromVivo = this.mRestoreEngine.isRunFromVivo();
        this.mTargetApp = targetApp;
    }

    public boolean isRunFromVivo() {
        return this.mRunFromVivo;
    }

    public void setDualUserByPkg(String pkg, boolean isDualUser) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            VSlog.i(TAG, "setDualUserByPkg fd = " + this.mFd + ", pkg = " + pkg + ", isDualUser = " + isDualUser);
            vivoBackupManager.setDualUserByPkg(pkg, isDualUser);
        }
    }

    public boolean isDualUserByPkg(String pkg) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            return vivoBackupManager.isDualUserByPkg(pkg);
        }
        return false;
    }

    public int getDualUserIdByPkg(int userId, String pkg) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null && vivoBackupManager.isDualUserByPkg(pkg)) {
            return vivoBackupManager.getDualUserId();
        }
        return userId;
    }

    public boolean enableDualPackage(String pkg) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null && vivoBackupManager.isDualUserByPkg(pkg)) {
            VSlog.i(TAG, "enable dual user package for: " + pkg + " fd = " + this.mFd);
            return vivoBackupManager.enableDualPackage(pkg);
        }
        return false;
    }

    public void pulseAndPostProgress(String msg) {
        FullRestoreEngine fullRestoreEngine;
        if (this.mRunFromVivo) {
            AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
            if (vivoBackupManager != null && (fullRestoreEngine = this.mRestoreEngine) != null) {
                fullRestoreEngine.pulseAndPostProgress(msg, fullRestoreEngine.getTotalBytes());
            }
        }
    }

    public void tearDownAgent() {
        FullRestoreEngine fullRestoreEngine;
        ApplicationInfo applicationInfo;
        if (this.mRunFromVivo) {
            AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
            if (vivoBackupManager != null && (fullRestoreEngine = this.mRestoreEngine) != null && (applicationInfo = this.mTargetApp) != null) {
                fullRestoreEngine.tearDownAgentExt(applicationInfo, true);
            }
        }
    }

    public Object getPolicyByPkg(String pkg) {
        if (this.mRunFromVivo) {
            AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
            if (vivoBackupManager != null && enableDualPackage(pkg)) {
                tearDownAgent();
                return RestorePolicy.ACCEPT;
            }
        }
        return RestorePolicy.IGNORE;
    }
}