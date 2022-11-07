package com.android.server.autofill;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutoFillManagerClient;
import com.vivo.common.utils.VLog;
import java.util.LinkedList;
import java.util.List;

/* loaded from: classes.dex */
public class VivoAutofillHelper {
    private static final String AUTOFILL_PACKAGE_VALUE_VIVO = "com.vivo.cipherchain";
    private static final String FILL_TYPE_KEY = "fillType";
    private static final String TAG = "VivoAutofillHelper";

    private boolean isVivoAutofillService(String packageName) {
        return AUTOFILL_PACKAGE_VALUE_VIVO.equals(packageName);
    }

    private int getFillType(Bundle clientState) {
        if (clientState != null) {
            return clientState.getInt(FILL_TYPE_KEY, 0);
        }
        return 0;
    }

    public void updateAutoFillManagerClient(Bundle clientState, String serviceName, IAutoFillManagerClient client, int sessionId, List<AutofillId> ids, List<AutofillValue> values, boolean hideHighlight) {
        int fillType;
        if (client == null || ids == null || values == null || !isVivoAutofillService(serviceName) || (fillType = getFillType(clientState)) == 0) {
            return;
        }
        if (fillType == 1) {
            try {
                client.autofill(sessionId, ids, values, hideHighlight);
            } catch (RemoteException e) {
                VLog.e(TAG, "exception in updateAutoFillManagerClient:" + e.getMessage());
            }
        } else if (fillType == 2 && ids.size() == 2 && values.size() == 2) {
            List<AutofillId> newIds = new LinkedList<>();
            newIds.add(ids.get(1));
            List<AutofillValue> newValues = new LinkedList<>();
            newValues.add(values.get(1));
            try {
                client.autofill(sessionId, newIds, newValues, hideHighlight);
            } catch (RemoteException e2) {
                VLog.e(TAG, "exception in updateAutoFillManagerClient:" + e2.getMessage());
            }
        }
    }
}