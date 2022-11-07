package com.android.server.backup;

import android.app.backup.IFullBackupRestoreObserver;
import android.os.ParcelFileDescriptor;
import java.util.concurrent.atomic.AtomicBoolean;

/* loaded from: classes.dex */
public interface IVivoPerformAdbRestoreTask {
    boolean isRestoreTimeout(Object obj);

    boolean isRunFromVivo();

    IFullBackupRestoreObserver prepareRestore(IFullBackupRestoreObserver iFullBackupRestoreObserver, Object obj, Object obj2, Object obj3, AtomicBoolean atomicBoolean, ParcelFileDescriptor parcelFileDescriptor);
}