package com.android.server.backup;

import android.content.pm.ApplicationInfo;

/* loaded from: classes.dex */
public interface IVivoTarBackupReader {
    boolean enableDualPackage(String str);

    int getDualUserIdByPkg(int i, String str);

    Object getPolicyByPkg(String str);

    boolean isDualUserByPkg(String str);

    boolean isRunFromVivo();

    void pulseAndPostProgress(String str);

    void setDualUserByPkg(String str, boolean z);

    void setUp(Object obj, ApplicationInfo applicationInfo);

    void tearDownAgent();

    /* loaded from: classes.dex */
    public interface IVivoTarBackupReaderExport {
        IVivoTarBackupReader getVivoInjectInstance();

        default void setUp(Object fullRestoreEngine, ApplicationInfo targetApp) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setUp(fullRestoreEngine, targetApp);
            }
        }
    }
}