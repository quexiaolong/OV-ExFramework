package com.android.server.backup;

import android.os.IBinder;
import com.android.server.backup.restore.AdbRestoreFinishedLatch;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import java.io.IOException;
import java.io.InputStream;
import vivo.app.backup.AbsVivoBackupManager;
import vivo.app.backup.BRTimeoutMonitor;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoFullRestoreEngineImpl implements IVivoFullRestoreEngine {
    private static final String TAG = "VIVO_BACKUP_VivoFullRestoreEngineImpl";
    private IBinder.DeathRecipient mAgentDeathRecipient;
    private AdbRestoreFinishedLatch mLatch;
    private boolean mRunFromVivo = false;
    private boolean mAsDualUser = false;
    private int mFd = -1;
    private BRTimeoutMonitor mRestoreTimeoutMonitor = null;
    private long mLastPostTime = 0;
    private boolean isRestoreOk = true;
    private int mRestoreToken = -1;
    private boolean mIsTearDownAgent = false;

    public void setUp(int fd, boolean isRunFromVivo, int token) {
        this.mFd = fd;
        this.mRunFromVivo = isRunFromVivo;
        this.mRestoreToken = token;
    }

    public int getFd() {
        return this.mFd;
    }

    public boolean getRestoreResult() {
        return this.isRestoreOk;
    }

    public void setRestoreResult(boolean result) {
        this.isRestoreOk = result;
    }

    public void setMonitor(BRTimeoutMonitor monitor) {
        this.mRestoreTimeoutMonitor = monitor;
    }

    public int getRestoreToken() {
        return this.mRestoreToken;
    }

    public boolean isRunFromVivo() {
        return this.mRunFromVivo;
    }

    public void setDeathRecipient(IBinder.DeathRecipient agentDeathRecipient) {
        this.mAgentDeathRecipient = agentDeathRecipient;
    }

    public void setTearDown(boolean isTearDown) {
        this.mIsTearDownAgent = isTearDown;
    }

    public boolean isTearDown() {
        return this.mIsTearDownAgent;
    }

    public void cancelRestoreTimeoutMonitor() {
        this.mRestoreTimeoutMonitor.cancel();
    }

    public void pulseAndPostProgress(String msg, long bytes) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            this.mRestoreTimeoutMonitor.pulse(msg);
            if (System.currentTimeMillis() - this.mLastPostTime > 1000) {
                vivoBackupManager.postRestoreCompleteSize(bytes, this.mFd);
                this.mLastPostTime = System.currentTimeMillis();
            }
        }
    }

    public void startRestoreTimeoutMonitor() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        VSlog.d(TAG, "installation complete , restart monitor");
        this.mRestoreTimeoutMonitor.start((long) VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
    }

    public int getDualUserIdByPkg(int userId, String pkg) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null && vivoBackupManager.isDualUserByPkg(pkg)) {
            VSlog.d(TAG, "fd = " + this.mFd + ", current userId of pkg " + pkg + " is: " + userId);
            return vivoBackupManager.getDualUserId();
        }
        return userId;
    }

    public void closeInputStream(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
                VSlog.i(TAG, "close inputStream");
            } catch (IOException e) {
                VSlog.e(TAG, "close inputStream error");
            }
        }
    }

    public void putFdByToken(int token) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            VSlog.d(TAG, "putFdByToken token = " + token + " fd = " + this.mFd);
            vivoBackupManager.putFdByToken(token, this.mFd);
        }
    }

    public void removeToken(int token) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            VSlog.d(TAG, "fd = " + this.mFd + ", current token is " + token + ", remove token");
            vivoBackupManager.removeToken(token);
        }
    }

    public void handleRestoreResult(long totalBytes) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            if (this.isRestoreOk) {
                vivoBackupManager.postRestoreCompleteSize(totalBytes, this.mFd);
                this.mLastPostTime = System.currentTimeMillis();
                VSlog.d(TAG, "Done consuming input tarfile, total bytes = " + totalBytes);
                return;
            }
            removeToken(this.mRestoreToken);
            vivoBackupManager.onError(5, this.mFd);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:16:0x0048, code lost:
        if (r7.mAgentDeathRecipient == null) goto L25;
     */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x004a, code lost:
        r8.asBinder().linkToDeath(r7.mAgentDeathRecipient, 0);
        vivo.util.VSlog.i(com.android.server.backup.VivoFullRestoreEngineImpl.TAG, "agent linkToDeath, pkg:[" + r9.packageName + "], uid:[" + r9.uid + "]");
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x007b, code lost:
        vivo.util.VSlog.e(com.android.server.backup.VivoFullRestoreEngineImpl.TAG, "Agent crashed, unable to linkToDeath");
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public android.app.IBackupAgent retryBindIfNull(android.app.IBackupAgent r8, android.content.pm.ApplicationInfo r9, java.lang.Object r10) {
        /*
            r7 = this;
            r0 = 0
            r1 = r10
            com.android.server.backup.UserBackupManagerService r1 = (com.android.server.backup.UserBackupManagerService) r1
        L4:
            java.lang.String r2 = "VIVO_BACKUP_VivoFullRestoreEngineImpl"
            if (r8 != 0) goto L44
            if (r1 == 0) goto L44
            long r3 = (long) r0
            r5 = 3
            int r3 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1))
            if (r3 >= 0) goto L44
            int r0 = r0 + 1
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "======== get agent null, try again time ["
            r3.append(r4)
            r3.append(r0)
            java.lang.String r4 = "] in "
            r3.append(r4)
            r4 = 5000(0x1388, double:2.4703E-320)
            r3.append(r4)
            java.lang.String r6 = " ========"
            r3.append(r6)
            java.lang.String r3 = r3.toString()
            vivo.util.VSlog.i(r2, r3)
            java.lang.Thread.sleep(r4)     // Catch: java.lang.InterruptedException -> L3a
            goto L3e
        L3a:
            r2 = move-exception
            r2.printStackTrace()
        L3e:
            r2 = 3
            android.app.IBackupAgent r8 = r1.bindToAgentSynchronous(r9, r2)
            goto L4
        L44:
            if (r8 == 0) goto L80
            android.os.IBinder$DeathRecipient r3 = r7.mAgentDeathRecipient
            if (r3 == 0) goto L80
            android.os.IBinder r3 = r8.asBinder()     // Catch: android.os.RemoteException -> L7a
            android.os.IBinder$DeathRecipient r4 = r7.mAgentDeathRecipient     // Catch: android.os.RemoteException -> L7a
            r5 = 0
            r3.linkToDeath(r4, r5)     // Catch: android.os.RemoteException -> L7a
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: android.os.RemoteException -> L7a
            r3.<init>()     // Catch: android.os.RemoteException -> L7a
            java.lang.String r4 = "agent linkToDeath, pkg:["
            r3.append(r4)     // Catch: android.os.RemoteException -> L7a
            java.lang.String r4 = r9.packageName     // Catch: android.os.RemoteException -> L7a
            r3.append(r4)     // Catch: android.os.RemoteException -> L7a
            java.lang.String r4 = "], uid:["
            r3.append(r4)     // Catch: android.os.RemoteException -> L7a
            int r4 = r9.uid     // Catch: android.os.RemoteException -> L7a
            r3.append(r4)     // Catch: android.os.RemoteException -> L7a
            java.lang.String r4 = "]"
            r3.append(r4)     // Catch: android.os.RemoteException -> L7a
            java.lang.String r3 = r3.toString()     // Catch: android.os.RemoteException -> L7a
            vivo.util.VSlog.i(r2, r3)     // Catch: android.os.RemoteException -> L7a
            goto L80
        L7a:
            r3 = move-exception
            java.lang.String r4 = "Agent crashed, unable to linkToDeath"
            vivo.util.VSlog.e(r2, r4)
        L80:
            return r8
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.backup.VivoFullRestoreEngineImpl.retryBindIfNull(android.app.IBackupAgent, android.content.pm.ApplicationInfo, java.lang.Object):android.app.IBackupAgent");
    }

    public void setAdbRestoreFinishedLatch(Object latch) {
        this.mLatch = (AdbRestoreFinishedLatch) latch;
    }

    public Object getAdbRestoreFinishedLatch() {
        return this.mLatch;
    }
}