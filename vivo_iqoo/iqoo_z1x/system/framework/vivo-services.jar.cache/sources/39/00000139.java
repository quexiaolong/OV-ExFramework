package com.android.server.autofill;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutoFillManagerClient;
import com.vivo.common.utils.VLog;
import java.util.List;

/* loaded from: classes.dex */
public class VivoAutofillServiceImpl implements IVivoAutofillService {
    private static final String TAG = "VivoAutofillServiceImpl";
    private static final String VIVO_CIP_CHAIN_SERVICE = "com.vivo.cipherchain.service.CipChainService";
    private Context mContext;
    private VivoAutofillHelper mVivoAutofillHelper = new VivoAutofillHelper();

    public VivoAutofillServiceImpl(Context context) {
        this.mContext = null;
        this.mContext = context;
    }

    public boolean isVivoAutofillService(ComponentName packageService) {
        VLog.d(TAG, "isVivoCipChain packageService:" + packageService);
        if (packageService != null && packageService.getClassName().equals(VIVO_CIP_CHAIN_SERVICE)) {
            return true;
        }
        return false;
    }

    public void updateAutoFill(Bundle clientState, String serviceName, IAutoFillManagerClient client, int sessionId, List<AutofillId> ids, List<AutofillValue> values, boolean hideHighlight) {
        this.mVivoAutofillHelper.updateAutoFillManagerClient(clientState, serviceName, client, sessionId, ids, values, hideHighlight);
    }
}