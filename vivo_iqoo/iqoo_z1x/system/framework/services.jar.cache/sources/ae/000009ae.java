package com.android.server.backup;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import java.util.concurrent.atomic.AtomicBoolean;

/* loaded from: classes.dex */
public interface IVivoPerformAdbBackupTask {
    boolean isBackupTimeout(Object obj);

    boolean isRunFromVivo();

    void prepareBackup(Object obj, Object obj2, Object obj3, Object obj4, AtomicBoolean atomicBoolean, ParcelFileDescriptor parcelFileDescriptor);

    void startDualUserBackup(Object obj, Object obj2, String str) throws RemoteException;
}