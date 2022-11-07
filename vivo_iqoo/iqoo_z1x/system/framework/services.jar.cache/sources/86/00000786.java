package com.android.server.am;

/* loaded from: classes.dex */
public interface IVivoProcessRecord {
    void addDepPkg(String str);

    void addPkg(String str);

    void addProcess();

    boolean checkSkipKilledByRemoveTask(String str, String str2);

    void checkUploadStabilityData(String str, String str2);

    boolean getColdStartAlreadyNotify();

    HostingRecord getHostingRecord();

    Object getRMProcInfo();

    boolean isCheckingPermission(int i, String str);

    void onProcAnr(String str, String str2, String str3, String str4);

    void onStartActivity();

    void processRecordkill(String str, int i, boolean z, boolean z2);

    void removeProcess();

    void setColdStartAlreadyNotify(boolean z);

    void setCreateReason(String str);

    void setDeathResson(String str);

    void setNeedKeepQuiet(boolean z);

    void setPid(int i);

    void setRmsPreloaded(boolean z);

    /* loaded from: classes.dex */
    public interface IVivoProcessRecordExport {
        IVivoProcessRecord getVivoInjectInstance();

        default HostingRecord getHostingRecord() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().getHostingRecord();
                return null;
            }
            return null;
        }

        default boolean getColdStartAlreadyNotify() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().getColdStartAlreadyNotify();
                return false;
            }
            return false;
        }

        default void setColdStartAlreadyNotify(boolean notified) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setColdStartAlreadyNotify(notified);
            }
        }
    }
}