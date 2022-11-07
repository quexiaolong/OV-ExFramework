package com.android.server;

import android.common.OplusFeatureCache;
import android.content.Context;
import com.android.server.content.IOplusFeatureConfigManagerInternal;
import com.android.server.display.DisplayManagerService;

/* loaded from: classes.dex */
public final class OplusSystemServerHelper {
    private static final String TAG = "OplusSystemServerHelper";
    private final Context mContext;
    private final IOplusJobSchedulerSystemServerEx mOplusJssSystemServerEx;
    private final IOplusSystemServerEx mOplusSystemServerEx;

    public OplusSystemServerHelper(Context context) {
        this.mContext = context;
        OplusFeatureCache.addFactory(OplusServiceFactory.getInstance());
        OplusFeatureCache.addFactory(OplusServiceFactory.getInstance());
        this.mOplusSystemServerEx = (IOplusSystemServerEx) OplusServiceFactory.getInstance().getFeature(IOplusSystemServerEx.DEFAULT, new Object[]{context});
        this.mOplusJssSystemServerEx = (IOplusJobSchedulerSystemServerEx) OplusJobSchedulerServiceFactory.getInstance().getFeature(IOplusJobSchedulerSystemServerEx.DEFAULT, new Object[]{context});
    }

    public void loadOplusFeaturesAsync() {
        ((IOplusFeatureConfigManagerInternal) OplusFeatureCache.getOrCreate(IOplusFeatureConfigManagerInternal.DEFAULT, new Object[0])).loadOplusFeatures();
    }

    public void startBootstrapServices() {
        IOplusSystemServerEx iOplusSystemServerEx = this.mOplusSystemServerEx;
        if (iOplusSystemServerEx != null) {
            iOplusSystemServerEx.startBootstrapServices();
        }
    }

    public void startCoreServices() {
        IOplusSystemServerEx iOplusSystemServerEx = this.mOplusSystemServerEx;
        if (iOplusSystemServerEx != null) {
            iOplusSystemServerEx.startCoreServices();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startOtherServices() {
        IOplusSystemServerEx iOplusSystemServerEx = this.mOplusSystemServerEx;
        if (iOplusSystemServerEx != null) {
            iOplusSystemServerEx.startOtherServices();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemReady() {
        IOplusSystemServerEx iOplusSystemServerEx = this.mOplusSystemServerEx;
        if (iOplusSystemServerEx != null) {
            iOplusSystemServerEx.systemReady();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemRunning() {
        IOplusSystemServerEx iOplusSystemServerEx = this.mOplusSystemServerEx;
        if (iOplusSystemServerEx != null) {
            iOplusSystemServerEx.systemRunning();
        }
    }

    boolean startOplusLightService() {
        IOplusSystemServerEx iOplusSystemServerEx = this.mOplusSystemServerEx;
        if (iOplusSystemServerEx != null) {
            return iOplusSystemServerEx.startOplusLightService();
        }
        return false;
    }

    boolean startOplusAccessibilityService() {
        IOplusSystemServerEx iOplusSystemServerEx = this.mOplusSystemServerEx;
        if (iOplusSystemServerEx != null) {
            return iOplusSystemServerEx.startOplusAccessibilityService();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startJobSchedulerService() {
        IOplusJobSchedulerSystemServerEx iOplusJobSchedulerSystemServerEx = this.mOplusJssSystemServerEx;
        if (iOplusJobSchedulerSystemServerEx != null) {
            return iOplusJobSchedulerSystemServerEx.startJobSchedulerService();
        }
        return false;
    }

    DisplayManagerService startColorDisplayManagerService() {
        IOplusSystemServerEx iOplusSystemServerEx = this.mOplusSystemServerEx;
        if (iOplusSystemServerEx != null) {
            return iOplusSystemServerEx.startColorDisplayManagerService();
        }
        return null;
    }

    public void addOplusDevicePolicyService() {
        IOplusSystemServerEx iOplusSystemServerEx = this.mOplusSystemServerEx;
        if (iOplusSystemServerEx != null) {
            iOplusSystemServerEx.addOplusDevicePolicyService();
        }
    }
}