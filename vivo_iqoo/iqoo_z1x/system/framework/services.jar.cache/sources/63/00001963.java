package com.android.server.statusbar;

import android.os.Bundle;

/* loaded from: classes2.dex */
public interface IVivoStatusBarManagerService {
    void changeUpslideState(boolean z, boolean z2);

    void dummy();

    void notifyInfo(int i, Bundle bundle);

    void onClearAllNotificationsIgnoreFlags();

    void onClearNotificationsIgnoreFlags(String str);

    void setNetworkSpeed(String str);

    void setSimcardFlow(String str, String str2);

    void setSimcardFlowExtension(Bundle bundle);

    void setStatusBarIconColor(boolean z);

    /* loaded from: classes2.dex */
    public interface IVivoStatusBarManagerServiceExport {
        IVivoStatusBarManagerService getVivoInjectInstance();

        default void dummyExport() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }
    }
}