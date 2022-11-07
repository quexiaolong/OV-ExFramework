package com.android.server;

import android.content.Context;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.INetworkManagementService;
import com.android.server.connectivity.NetworkAgentInfo;

/* loaded from: classes.dex */
public class VivoConnectivityServiceImplPriv extends VivoConnectivityServiceImpl {
    public VivoConnectivityServiceImplPriv(Context context, ConnectivityService service, INetworkManagementService managementService, Handler handler) {
        super(context, service, managementService, handler);
    }

    @Override // com.android.server.VivoConnectivityServiceImpl
    public void addPolicyRoute(NetworkAgentInfo networkAgent) {
        LinkProperties lp = new LinkProperties(networkAgent.linkProperties);
        NetworkInfo ni = networkAgent.networkInfo;
        if (ni == null || ni.getType() != 1) {
            lp = this.mConnectivityService.getLinkPropertiesForType(1);
        }
        String dstNetAddr = getNetAddress(lp);
        String tableName = this.mConnectivityService.getInterfaceNameForType(0);
        String devName = this.mConnectivityService.getInterfaceNameForType(1);
        if (dstNetAddr == null || tableName == null || devName == null) {
            log("dstNetAddr OR tableName OR devName is null, return directly");
            return;
        }
        try {
            this.mNetworkManagementService.addPolicyRoute(tableName, dstNetAddr, devName);
            if (!hasAddPolicyRoute) {
                hasAddPolicyRoute = true;
                sendNetCoexistAvailableBroadcast();
            }
        } catch (Exception ex) {
            log("addPolicyRoute exception: " + ex);
        }
    }
}