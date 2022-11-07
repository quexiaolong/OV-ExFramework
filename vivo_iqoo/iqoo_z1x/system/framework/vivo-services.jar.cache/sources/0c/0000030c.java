package com.android.server.net;

import com.android.server.VivoDoubleInstanceServiceImpl;
import com.vivo.services.rms.ProcessList;

/* loaded from: classes.dex */
public class VivoNetworkStatsAccessImpl implements IVivoNetworkStatsAccess {
    static final String TAG = "VivoNetworkStatsAccessImpl";
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();

    public boolean isDoubleAppUserExist() {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable()) {
            return this.mVivoDoubleInstanceService.isDoubleAppUserExist();
        }
        return false;
    }

    public int getDoubleAppUserId() {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable()) {
            return this.mVivoDoubleInstanceService.getDoubleAppUserId();
        }
        return ProcessList.INVALID_ADJ;
    }
}