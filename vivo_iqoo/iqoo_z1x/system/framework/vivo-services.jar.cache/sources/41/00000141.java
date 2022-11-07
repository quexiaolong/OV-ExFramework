package com.android.server.backup;

import android.app.backup.IFullBackupRestoreObserver;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.android.server.backup.fullbackup.FullBackupEngine;
import com.android.server.backup.fullbackup.FullBackupObbConnection;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import vivo.app.backup.AbsVivoBackupManager;
import vivo.app.backup.BRTimeoutMonitor;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoPerformAdbBackupTaskImpl implements IVivoPerformAdbBackupTask {
    private static final String TAG = "VIVO_BACKUP_VivoPerformAdbBackupTaskImpl";
    private BRTimeoutMonitor mBackupTimeoutMonitor;
    private AtomicBoolean mCloseLatch = new AtomicBoolean(false);
    private int mFd;
    private IFullBackupRestoreObserver mObserver;
    private boolean mRunFromVivo;
    private int mToken;

    public VivoPerformAdbBackupTaskImpl(int token, int fd, IFullBackupRestoreObserver observer) {
        this.mToken = token;
        this.mBackupTimeoutMonitor = new BRTimeoutMonitor(TAG, this.mToken);
        this.mFd = fd;
        this.mRunFromVivo = isRunningFromVivoBackup(fd);
        this.mObserver = observer;
    }

    public boolean isRunFromVivo() {
        return this.mRunFromVivo;
    }

    public boolean isRunningFromVivoBackup(int fd) {
        AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
        if (vivoBackupManager != null) {
            return vivoBackupManager.isRunningFromVivoBackup(fd);
        }
        return false;
    }

    public void prepareBackup(Object peformAdbBackupTask, Object userBackupManagerService, Object fullBackupEngine, Object fullBackupObbConnection, final AtomicBoolean latchObject, final ParcelFileDescriptor outputFile) {
        if (this.mRunFromVivo) {
            AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
            if (vivoBackupManager != null) {
                final UserBackupManagerService ubms = (UserBackupManagerService) userBackupManagerService;
                final FullBackupEngine fbe = (FullBackupEngine) fullBackupEngine;
                final FullBackupObbConnection fboc = (FullBackupObbConnection) fullBackupObbConnection;
                fbe.setUp(this.mFd, this.mRunFromVivo);
                this.mBackupTimeoutMonitor.setOnTimeoutListener(new BRTimeoutMonitor.OnTimeoutListener() { // from class: com.android.server.backup.VivoPerformAdbBackupTaskImpl.1
                    public void onTimeout(int token, String lastFlag, String tag) {
                        if (!VivoPerformAdbBackupTaskImpl.this.mCloseLatch.compareAndSet(false, true)) {
                            return;
                        }
                        VivoPerformAdbBackupTaskImpl.this.sendTimeout();
                        fbe.tearDownPipes();
                        fboc.tearDown();
                        VivoPerformAdbBackupTaskImpl.this.sendEndBackup();
                        synchronized (latchObject) {
                            latchObject.set(true);
                            latchObject.notifyAll();
                        }
                        try {
                            outputFile.close();
                        } catch (IOException e) {
                            VSlog.e(VivoPerformAdbBackupTaskImpl.TAG, "IO error closing outputFile ", e);
                        }
                        ubms.removeToken(token);
                        ubms.getWakelock().release();
                    }
                });
                this.mBackupTimeoutMonitor.start((long) VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
                fbe.setMonitor(this.mBackupTimeoutMonitor);
            }
        }
    }

    public void startDualUserBackup(Object userBackupManagerService, Object fullBackupEngine, String pkg) throws RemoteException {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            UserBackupManagerService ubms = (UserBackupManagerService) userBackupManagerService;
            FullBackupEngine fbe = (FullBackupEngine) fullBackupEngine;
            int backupResult = fbe.getBackupResult();
            if (backupResult != 0) {
                VSlog.i(TAG, "backup " + pkg + " error, result is: [" + backupResult + "]");
                ubms.removeToken(this.mToken);
                ubms.onError(5, this.mFd);
            } else if (vivoBackupManager.isDualPackageEnabled(pkg)) {
                VSlog.i(TAG, "--- start backup dual package [" + pkg + "] ---");
                fbe.setDualUserByPkg(pkg, true);
                int dualUserBackupResult = fbe.backupOnePackage();
                if (dualUserBackupResult != 0) {
                    VSlog.i(TAG, " backup " + pkg + " error, result is: [" + dualUserBackupResult + "]");
                    ubms.removeToken(this.mToken);
                    ubms.onError(5, this.mFd);
                }
            }
        }
    }

    public boolean isBackupTimeout(Object userBackupManagerService) {
        if (this.mRunFromVivo) {
            AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
            if (vivoBackupManager != null) {
                if (!this.mCloseLatch.compareAndSet(false, true)) {
                    return true;
                }
                this.mBackupTimeoutMonitor.cancel();
                ((UserBackupManagerService) userBackupManagerService).removeToken(this.mToken);
            }
        }
        return false;
    }

    public void removeToken(int token) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            VSlog.d(TAG, "fd = " + this.mFd + ", current token is " + token + ", remove token");
            vivoBackupManager.removeToken(token);
        }
    }

    public void sendTimeout() {
        IFullBackupRestoreObserver iFullBackupRestoreObserver = this.mObserver;
        if (iFullBackupRestoreObserver != null) {
            try {
                iFullBackupRestoreObserver.onTimeout();
            } catch (RemoteException e) {
                VSlog.w(TAG, "full backup observer went away: timeout");
                this.mObserver = null;
            }
        }
    }

    public void sendEndBackup() {
        IFullBackupRestoreObserver iFullBackupRestoreObserver = this.mObserver;
        if (iFullBackupRestoreObserver != null) {
            try {
                iFullBackupRestoreObserver.onEndBackup();
            } catch (RemoteException e) {
                VSlog.w(TAG, "full backup observer went away: endBackup");
                this.mObserver = null;
            }
        }
    }
}