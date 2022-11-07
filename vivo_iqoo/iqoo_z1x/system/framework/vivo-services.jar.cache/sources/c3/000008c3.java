package com.vivo.services.vgc.cbs.carrier;

import com.vivo.services.vgc.cbs.CbsSimInfo;
import java.util.List;

/* loaded from: classes.dex */
public class CarrierManagerFactory {
    private static final CarrierManagerFactory factory = new CarrierManagerFactory();

    private CarrierManagerFactory() {
    }

    public static CarrierManagerFactory getInstance() {
        return factory;
    }

    public CbsCarrierManager getCarrierManager(List<CbsSimInfo> simInfoList) {
        return new GeneralManager(simInfoList);
    }
}