package com.android.server.power;

import android.content.Context;

/* loaded from: classes2.dex */
public interface IVivoShutdownThread {
    void dummy();

    void fingerprintRecordReboot(Context context, String str);

    void initializeDBHandler();

    boolean isMdmModemType(Context context);

    void notifyShutdown();

    void pollBacklightOff(Context context);

    void runPre();

    void setBacklightOff(Context context);

    void showShutdownPic(String str, Context context);

    boolean shutdownInnerPre();

    void skipShowShutdownDialog();

    void waitForBroadcastAndAnimationDoneLocked(long j);

    /* loaded from: classes2.dex */
    public interface IVivoShutdownThreadExport {
        IVivoShutdownThread getVivoInjectInstance();

        default void dummyExport() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }
    }
}