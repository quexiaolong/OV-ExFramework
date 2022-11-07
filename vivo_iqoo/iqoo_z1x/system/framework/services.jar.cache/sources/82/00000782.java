package com.android.server.am;

import android.content.Context;
import android.content.Intent;

/* loaded from: classes.dex */
public interface IVivoPcbaCotrol {
    boolean isIllegalPackage(Intent intent, Context context);

    boolean isIllegalPermisson(String str, int i);

    boolean isIllegalPermisson(String str, String str2);

    void showToast(Context context);
}