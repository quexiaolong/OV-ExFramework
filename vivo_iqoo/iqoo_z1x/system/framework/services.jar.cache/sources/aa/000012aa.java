package com.android.server.net;

import android.os.INetworkManagementService;
import com.android.internal.util.IndentingPrintWriter;
import java.util.List;

/* loaded from: classes.dex */
public interface IVivoNetworkPolicyManagerService {
    void closeSockets(List list, List list2, INetworkManagementService iNetworkManagementService);

    void dummy();

    void dump(IndentingPrintWriter indentingPrintWriter);

    boolean isAllowUnPrivilegedAppAddAsDefault();

    void onUidDeletedUL(int i);

    void resetUidFirewallRules(int i, INetworkManagementService iNetworkManagementService);

    boolean setMobileUidFirewall(List list, INetworkManagementService iNetworkManagementService);

    boolean setUidFirewallForUserChain(int i, String str, List list, INetworkManagementService iNetworkManagementService);

    boolean setWifiUidFirewall(List list, INetworkManagementService iNetworkManagementService);

    void showOrCancelNotify(boolean z);

    /* loaded from: classes.dex */
    public interface IVivoNpmsExport {
        IVivoNetworkPolicyManagerService getVivoInjectInstance();

        default void dummyExport() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }
    }
}