package com.android.server.biometrics.face;

import android.os.IBinder;
import java.io.FileDescriptor;

/* loaded from: classes.dex */
public interface IVivoFaceService {
    boolean ardVersionUpdated(boolean z);

    void doFaceAndroidUpdate();

    IBinder getFaceUIBinder();

    boolean getLockoutState();

    boolean getResultType(int i);

    FileDescriptor getShareMemoryFd(int i, String str);

    void onAcquired(int i, int i2);

    void onAuthenticationFailed();

    void onAuthenticationSucceeded(int i, int i2);

    void onEnrollmentStart();

    void onEnrollmentStop();

    void onError(int i, int i2);

    void onFaceAlgorithmResult(int i, int i2, int i3, String str);

    void onHidlServiceDied();

    void onRemoved();

    void onSystemTime(long j, int i);

    void processAuthenticationOnError(IBinder iBinder, String str);

    void processAuthenticationOnStart(IBinder iBinder, String str);

    void processAuthenticationOnStop(IBinder iBinder, String str);

    void sendCommand(int i, int i2, String str);
}