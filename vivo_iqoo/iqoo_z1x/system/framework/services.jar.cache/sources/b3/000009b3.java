package com.android.server.backup;

import android.app.IBackupAgent;

/* loaded from: classes.dex */
public interface IVivoTrampoline {
    IBackupAgent bindToAgentSynchronousFromVivo(String str, int i);

    void clearApplicationDataBeforeRestoreFromVivo(String str);

    int getUserIdByPkg(int i, String str);

    int getUserIdByToken(int i, int i2);

    void removeToken(int i);

    void setService(Object obj);

    void tearDownAgentAndKillFromVivo(String str);

    /* loaded from: classes.dex */
    public interface IVivoTrampolineExport {
        IVivoTrampoline getVivoInjectInstance();

        default IBackupAgent bindToAgentSynchronousFromVivo(String packageName, int mode) {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().bindToAgentSynchronousFromVivo(packageName, mode);
            }
            return null;
        }

        default void tearDownAgentAndKillFromVivo(String packageName) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().tearDownAgentAndKillFromVivo(packageName);
            }
        }

        default void clearApplicationDataBeforeRestoreFromVivo(String packageName) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().clearApplicationDataBeforeRestoreFromVivo(packageName);
            }
        }
    }
}