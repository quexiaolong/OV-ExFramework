package com.android.server.wifi;

import android.annotation.SystemApi;
import android.os.SystemService;

@SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
/* loaded from: classes2.dex */
public class SupplicantManager {
    private static final String WPA_SUPPLICANT_DAEMON_NAME = "wpa_supplicant";

    private SupplicantManager() {
    }

    public static void start() {
        SystemService.start(WPA_SUPPLICANT_DAEMON_NAME);
    }

    public static void stop() {
        SystemService.stop(WPA_SUPPLICANT_DAEMON_NAME);
    }
}