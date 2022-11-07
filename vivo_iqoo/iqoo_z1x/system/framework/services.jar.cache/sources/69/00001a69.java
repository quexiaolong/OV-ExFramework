package com.android.server.updates;

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Slog;

/* loaded from: classes2.dex */
public class EmergencyNumberDbInstallReceiver extends ConfigUpdateInstallReceiver {
    private static final String TAG = "EmergencyNumberDbInstallReceiver";

    public EmergencyNumberDbInstallReceiver() {
        super("/data/misc/emergencynumberdb", "emergency_number_db", "metadata/", "version");
    }

    @Override // com.android.server.updates.ConfigUpdateInstallReceiver
    protected void postInstall(Context context, Intent intent) {
        Slog.i(TAG, "Emergency number database is updated in file partition");
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        telephonyManager.notifyOtaEmergencyNumberDbInstalled();
    }
}