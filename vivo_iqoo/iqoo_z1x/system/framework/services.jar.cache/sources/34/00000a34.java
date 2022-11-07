package com.android.server.backup.utils;

import android.app.backup.IFullBackupRestoreObserver;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;

/* loaded from: classes.dex */
public class FullBackupRestoreObserverUtils {
    public static IFullBackupRestoreObserver sendStartRestore(IFullBackupRestoreObserver observer) {
        if (observer != null) {
            try {
                observer.onStartRestore();
                return observer;
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full restore observer went away: startRestore");
                return null;
            }
        }
        return observer;
    }

    public static IFullBackupRestoreObserver sendOnRestorePackage(IFullBackupRestoreObserver observer, String name) {
        if (observer != null) {
            try {
                observer.onRestorePackage(name);
                return observer;
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full restore observer went away: restorePackage");
                return null;
            }
        }
        return observer;
    }

    public static IFullBackupRestoreObserver sendEndRestore(IFullBackupRestoreObserver observer) {
        if (observer != null) {
            try {
                observer.onEndRestore();
                return observer;
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full restore observer went away: endRestore");
                return null;
            }
        }
        return observer;
    }
}