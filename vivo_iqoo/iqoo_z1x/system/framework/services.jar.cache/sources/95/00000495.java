package com.android.server;

import android.content.Context;
import com.android.server.am.ActivityManagerService;

/* loaded from: classes.dex */
public interface IVivoSystemServer {
    void makeDisplayStateServiceReady();

    void startANRManagerService(Context context, ActivityManagerService activityManagerService);

    void startAutoRecoverService(ActivityManagerService activityManagerService);

    void startConfigurationManagerService(Context context);

    void startCoreServices();

    void startDisplayStateService();

    void startFaceUi();

    void startFingerprintAnalysisService(int i);

    void startFingerprintUIManagerService();

    void startFingerprintUi();

    void startHangVivoConfigService();

    void startOtherServices();

    void startSecInputMethodManagerService();

    void startVgcManagerService(Context context);
}