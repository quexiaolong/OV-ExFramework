package com.android.server.autofill;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutoFillManagerClient;
import java.util.List;

/* loaded from: classes.dex */
public interface IVivoAutofillService {
    boolean isVivoAutofillService(ComponentName componentName);

    void updateAutoFill(Bundle bundle, String str, IAutoFillManagerClient iAutoFillManagerClient, int i, List<AutofillId> list, List<AutofillValue> list2, boolean z);
}