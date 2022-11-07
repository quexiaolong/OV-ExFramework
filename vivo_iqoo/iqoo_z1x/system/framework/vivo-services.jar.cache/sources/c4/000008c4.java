package com.vivo.services.vgc.cbs.carrier;

import com.vivo.services.vgc.cbs.CbsSimInfo;
import java.util.List;

/* loaded from: classes.dex */
public abstract class CbsCarrierManager {
    protected List<CbsSimInfo> mSimInfoList;

    public abstract CbsSimInfo getMapSimInfo(CbsSimInfo cbsSimInfo);

    public abstract int getSimCardFlag(CbsSimInfo cbsSimInfo);

    public CbsCarrierManager(List<CbsSimInfo> simInfoList) {
        this.mSimInfoList = simInfoList;
    }
}