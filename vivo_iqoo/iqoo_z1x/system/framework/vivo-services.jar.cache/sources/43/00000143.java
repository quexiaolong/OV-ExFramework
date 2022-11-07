package com.android.server.backup;

import android.app.backup.IFullBackupRestoreObserver;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.android.server.backup.fullbackup.FullBackupObbConnection;
import com.android.server.backup.restore.AdbRestoreFinishedLatch;
import com.android.server.backup.restore.FullRestoreEngine;
import com.android.server.backup.utils.FullBackupRestoreObserverUtils;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import vivo.app.backup.AbsVivoBackupManager;
import vivo.app.backup.BRTimeoutMonitor;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoPerformAdbRestoreTaskImpl implements IVivoPerformAdbRestoreTask {
    private static final String TAG = "VIVO_BACKUP_VivoPerformAdbRestoreTaskImpl";
    private AtomicBoolean mCloseLatch = new AtomicBoolean(false);
    private int mFd;
    private IFullBackupRestoreObserver mObserver;
    private BRTimeoutMonitor mRestoreTimeoutMonitor;
    private final boolean mRunFromVivo;
    private int mToken;

    public VivoPerformAdbRestoreTaskImpl(int token, int fd) {
        this.mToken = token;
        this.mRestoreTimeoutMonitor = new BRTimeoutMonitor(TAG, this.mToken);
        this.mFd = fd;
        this.mRunFromVivo = isRunningFromVivoBackup(fd);
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

    public IFullBackupRestoreObserver prepareRestore(IFullBackupRestoreObserver observer, Object userBackupManagerService, Object fullRestoreEngine, Object fullBackupObbConnection, final AtomicBoolean latchObject, final ParcelFileDescriptor inputFile) {
        final AbsVivoBackupManager vivoBackupManager;
        this.mObserver = observer;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            final UserBackupManagerService ubms = (UserBackupManagerService) userBackupManagerService;
            final FullRestoreEngine fre = (FullRestoreEngine) fullRestoreEngine;
            final FullBackupObbConnection fboc = (FullBackupObbConnection) fullBackupObbConnection;
            fre.setUp(this.mFd, this.mRunFromVivo, this.mToken);
            vivoBackupManager.putFdByToken(this.mToken, this.mFd);
            this.mRestoreTimeoutMonitor.setOnTimeoutListener(new BRTimeoutMonitor.OnTimeoutListener() { // from class: com.android.server.backup.VivoPerformAdbRestoreTaskImpl.1
                public void onTimeout(int token, String lastFlag, String tag) {
                    if (!VivoPerformAdbRestoreTaskImpl.this.mCloseLatch.compareAndSet(false, true)) {
                        return;
                    }
                    fre.setRestoreResult(false);
                    fre.tearDownPipesExt();
                    FullRestoreEngine fullRestoreEngine2 = fre;
                    fullRestoreEngine2.tearDownAgentExt(fullRestoreEngine2.getTargetApp(), false);
                    fboc.tearDown();
                    VivoPerformAdbRestoreTaskImpl vivoPerformAdbRestoreTaskImpl = VivoPerformAdbRestoreTaskImpl.this;
                    vivoPerformAdbRestoreTaskImpl.mObserver = vivoPerformAdbRestoreTaskImpl.sendTimeout(vivoPerformAdbRestoreTaskImpl.mObserver);
                    VivoPerformAdbRestoreTaskImpl vivoPerformAdbRestoreTaskImpl2 = VivoPerformAdbRestoreTaskImpl.this;
                    vivoPerformAdbRestoreTaskImpl2.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(vivoPerformAdbRestoreTaskImpl2.mObserver);
                    synchronized (latchObject) {
                        latchObject.set(true);
                        latchObject.notifyAll();
                    }
                    try {
                        inputFile.close();
                    } catch (IOException e) {
                        VSlog.e(VivoPerformAdbRestoreTaskImpl.TAG, "IO error closing InputFile ", e);
                    }
                    ubms.removeToken(token);
                    ubms.getWakelock().release();
                }
            });
            this.mRestoreTimeoutMonitor.start((long) VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
            fre.setMonitor(this.mRestoreTimeoutMonitor);
            IBinder.DeathRecipient agentDeathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.backup.VivoPerformAdbRestoreTaskImpl.2
                @Override // android.os.IBinder.DeathRecipient
                public void binderDied() {
                    if (!fre.isTearDown()) {
                        if (!VivoPerformAdbRestoreTaskImpl.this.mCloseLatch.compareAndSet(false, true)) {
                            return;
                        }
                        fre.setRestoreResult(false);
                        fre.tearDownPipesExt();
                        FullRestoreEngine fullRestoreEngine2 = fre;
                        fullRestoreEngine2.tearDownAgentExt(fullRestoreEngine2.getTargetApp(), false);
                        AdbRestoreFinishedLatch latch = (AdbRestoreFinishedLatch) fre.getAdbRestoreFinishedLatch();
                        if (latch != null) {
                            VSlog.e(VivoPerformAdbRestoreTaskImpl.TAG, "agent was killed unnormal, so op complete.");
                            latch.operationComplete(0L);
                        }
                        fboc.tearDown();
                        vivoBackupManager.onError(5, VivoPerformAdbRestoreTaskImpl.this.mFd);
                        VivoPerformAdbRestoreTaskImpl vivoPerformAdbRestoreTaskImpl = VivoPerformAdbRestoreTaskImpl.this;
                        vivoPerformAdbRestoreTaskImpl.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(vivoPerformAdbRestoreTaskImpl.mObserver);
                        synchronized (latchObject) {
                            latchObject.set(true);
                            latchObject.notifyAll();
                        }
                        try {
                            inputFile.close();
                        } catch (IOException e) {
                            VSlog.e(VivoPerformAdbRestoreTaskImpl.TAG, "IO error closing InputFile ", e);
                        }
                        VivoPerformAdbRestoreTaskImpl.this.mRestoreTimeoutMonitor.cancel();
                        VSlog.i(VivoPerformAdbRestoreTaskImpl.TAG, "agent was killed unnormal");
                        ubms.removeToken(VivoPerformAdbRestoreTaskImpl.this.mToken);
                        ubms.getWakelock().release();
                        return;
                    }
                    fre.setTearDown(false);
                }
            };
            fre.setDeathRecipient(agentDeathRecipient);
        }
        return this.mObserver;
    }

    public boolean isRestoreTimeout(Object userBackupManagerService) {
        if (this.mRunFromVivo) {
            AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
            if (vivoBackupManager != null) {
                if (!this.mCloseLatch.compareAndSet(false, true)) {
                    return true;
                }
                this.mRestoreTimeoutMonitor.cancel();
                ((UserBackupManagerService) userBackupManagerService).removeToken(this.mToken);
            }
        }
        return false;
    }

    public IFullBackupRestoreObserver sendTimeout(IFullBackupRestoreObserver observer) {
        if (observer != null) {
            try {
                observer.onTimeout();
                return observer;
            } catch (RemoteException e) {
                VSlog.w(TAG, "full restore observer went away: timeout");
                return null;
            }
        }
        return observer;
    }
}