package com.android.server.backup;

import android.app.IBackupAgent;
import android.content.pm.ApplicationInfo;
import android.os.IBinder;
import java.io.InputStream;
import vivo.app.backup.BRTimeoutMonitor;

/* loaded from: classes.dex */
public interface IVivoFullRestoreEngine {
    void cancelRestoreTimeoutMonitor();

    void closeInputStream(InputStream inputStream);

    Object getAdbRestoreFinishedLatch();

    int getDualUserIdByPkg(int i, String str);

    int getFd();

    boolean getRestoreResult();

    int getRestoreToken();

    void handleRestoreResult(long j);

    boolean isRunFromVivo();

    boolean isTearDown();

    void pulseAndPostProgress(String str, long j);

    void putFdByToken(int i);

    void removeToken(int i);

    IBackupAgent retryBindIfNull(IBackupAgent iBackupAgent, ApplicationInfo applicationInfo, Object obj);

    void setAdbRestoreFinishedLatch(Object obj);

    void setDeathRecipient(IBinder.DeathRecipient deathRecipient);

    void setMonitor(BRTimeoutMonitor bRTimeoutMonitor);

    void setRestoreResult(boolean z);

    void setTearDown(boolean z);

    void setUp(int i, boolean z, int i2);

    void startRestoreTimeoutMonitor();

    /* loaded from: classes.dex */
    public interface IVivoFullRestoreEngineExport {
        IVivoFullRestoreEngine getVivoInjectInstance();

        default void setUp(int fd, boolean isRunFromVivo, int token) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setUp(fd, isRunFromVivo, token);
            }
        }

        default int getFd() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().getFd();
            }
            return -1;
        }

        default boolean isRunFromVivo() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().isRunFromVivo();
            }
            return false;
        }

        default void handleRestoreResult(long totalBytes) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().handleRestoreResult(totalBytes);
            }
        }

        default void setMonitor(BRTimeoutMonitor monitor) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setMonitor(monitor);
            }
        }

        default void setRestoreResult(boolean result) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setRestoreResult(result);
            }
        }

        default void pulseAndPostProgress(String msg, long bytes) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().pulseAndPostProgress(msg, bytes);
            }
        }

        default void setDeathRecipient(IBinder.DeathRecipient agentDeathRecipient) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setDeathRecipient(agentDeathRecipient);
            }
        }

        default void setTearDown(boolean isTearDown) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setTearDown(isTearDown);
            }
        }

        default boolean isTearDown() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().isTearDown();
            }
            return false;
        }

        default void setAdbRestoreFinishedLatch(Object latch) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setAdbRestoreFinishedLatch(latch);
            }
        }

        default Object getAdbRestoreFinishedLatch() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().getAdbRestoreFinishedLatch();
            }
            return null;
        }
    }
}