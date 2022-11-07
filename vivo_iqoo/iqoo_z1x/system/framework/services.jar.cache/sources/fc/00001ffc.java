package com.vivo.server.adapter;

import android.content.Context;
import com.android.server.NetworkManagementService;
import com.android.server.SystemServiceManager;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.utils.TimingsTraceAndSlog;

/* loaded from: classes2.dex */
public abstract class AbsSystemServerAdapter {
    public void setPrameters(TimingsTraceAndSlog btt, SystemServiceManager ssm, Context context) {
    }

    public void startMtkBootstrapServices() {
    }

    public void startMtkCoreServices() {
    }

    public boolean startMtkAlarmManagerService() {
        return false;
    }

    public void startMtkOtherServices() {
    }

    public boolean startMtkStorageManagerService() {
        return false;
    }

    public Object getMtkConnectivityService(NetworkManagementService networkManagement, NetworkStatsService networkStats, NetworkPolicyManagerService networkPolicy) {
        return null;
    }

    public void addBootEvent(String bootevent) {
    }

    public void startNetworkDataControllerService(Context context) {
    }
}