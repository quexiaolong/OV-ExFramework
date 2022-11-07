package com.android.server.backup;

import android.app.IBackupAgent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.ParcelFileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import vivo.app.backup.BRTimeoutMonitor;

/* loaded from: classes.dex */
public interface IVivoFullBackupEngine {
    int getBackupResult();

    int getFd();

    PackageInfo getPkgInfoIfDualUser(PackageInfo packageInfo, Object obj);

    void handleOneResult(int i, int i2);

    boolean isDualUserByPkg(String str);

    boolean isRunFromVivo();

    void putFdByToken(int i);

    void removeToken(int i);

    IBackupAgent retryBindIfNull(IBackupAgent iBackupAgent, ApplicationInfo applicationInfo, Object obj);

    void routeSocketDataToOutputFromVivo(ParcelFileDescriptor parcelFileDescriptor, OutputStream outputStream) throws IOException;

    void setBackupResult(int i);

    void setDualUserByPkg(String str, boolean z);

    void setMonitor(BRTimeoutMonitor bRTimeoutMonitor);

    void setPipes(ParcelFileDescriptor[] parcelFileDescriptorArr);

    void setUp(int i, boolean z);

    void tearDownPipes();

    /* loaded from: classes.dex */
    public interface IVivoFullBackupEngineExport {
        IVivoFullBackupEngine getVivoInjectInstance();

        default void setUp(int fd, boolean isRunFromVivo) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setUp(fd, isRunFromVivo);
            }
        }

        default boolean isRunFromVivo() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().isRunFromVivo();
            }
            return false;
        }

        default void setMonitor(BRTimeoutMonitor monitor) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setMonitor(monitor);
            }
        }

        default void tearDownPipes() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().tearDownPipes();
            }
        }

        default int getBackupResult() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().getBackupResult();
            }
            return -1;
        }

        default void setDualUserByPkg(String pkg, boolean isDualUser) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setDualUserByPkg(pkg, isDualUser);
            }
        }
    }
}