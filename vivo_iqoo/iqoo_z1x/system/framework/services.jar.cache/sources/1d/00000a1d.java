package com.android.server.backup.transport;

import android.app.backup.RestoreDescription;
import android.app.backup.RestoreSet;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.android.internal.backup.IBackupTransport;

/* loaded from: classes.dex */
public abstract class DelegatingTransport extends IBackupTransport.Stub {
    protected abstract IBackupTransport getDelegate() throws RemoteException;

    public String name() throws RemoteException {
        return getDelegate().name();
    }

    public Intent configurationIntent() throws RemoteException {
        return getDelegate().configurationIntent();
    }

    public String currentDestinationString() throws RemoteException {
        return getDelegate().currentDestinationString();
    }

    public Intent dataManagementIntent() throws RemoteException {
        return getDelegate().dataManagementIntent();
    }

    public CharSequence dataManagementIntentLabel() throws RemoteException {
        return getDelegate().dataManagementIntentLabel();
    }

    public String transportDirName() throws RemoteException {
        return getDelegate().transportDirName();
    }

    public long requestBackupTime() throws RemoteException {
        return getDelegate().requestBackupTime();
    }

    public int initializeDevice() throws RemoteException {
        return getDelegate().initializeDevice();
    }

    public int performBackup(PackageInfo packageInfo, ParcelFileDescriptor inFd, int flags) throws RemoteException {
        return getDelegate().performBackup(packageInfo, inFd, flags);
    }

    public int clearBackupData(PackageInfo packageInfo) throws RemoteException {
        return getDelegate().clearBackupData(packageInfo);
    }

    public int finishBackup() throws RemoteException {
        return getDelegate().finishBackup();
    }

    public RestoreSet[] getAvailableRestoreSets() throws RemoteException {
        return getDelegate().getAvailableRestoreSets();
    }

    public long getCurrentRestoreSet() throws RemoteException {
        return getDelegate().getCurrentRestoreSet();
    }

    public int startRestore(long token, PackageInfo[] packages) throws RemoteException {
        return getDelegate().startRestore(token, packages);
    }

    public RestoreDescription nextRestorePackage() throws RemoteException {
        return getDelegate().nextRestorePackage();
    }

    public int getRestoreData(ParcelFileDescriptor outFd) throws RemoteException {
        return getDelegate().getRestoreData(outFd);
    }

    public void finishRestore() throws RemoteException {
        getDelegate().finishRestore();
    }

    public long requestFullBackupTime() throws RemoteException {
        return getDelegate().requestFullBackupTime();
    }

    public int performFullBackup(PackageInfo targetPackage, ParcelFileDescriptor socket, int flags) throws RemoteException {
        return getDelegate().performFullBackup(targetPackage, socket, flags);
    }

    public int checkFullBackupSize(long size) throws RemoteException {
        return getDelegate().checkFullBackupSize(size);
    }

    public int sendBackupData(int numBytes) throws RemoteException {
        return getDelegate().sendBackupData(numBytes);
    }

    public void cancelFullBackup() throws RemoteException {
        getDelegate().cancelFullBackup();
    }

    public boolean isAppEligibleForBackup(PackageInfo targetPackage, boolean isFullBackup) throws RemoteException {
        return getDelegate().isAppEligibleForBackup(targetPackage, isFullBackup);
    }

    public long getBackupQuota(String packageName, boolean isFullBackup) throws RemoteException {
        return getDelegate().getBackupQuota(packageName, isFullBackup);
    }

    public int getNextFullRestoreDataChunk(ParcelFileDescriptor socket) throws RemoteException {
        return getDelegate().getNextFullRestoreDataChunk(socket);
    }

    public int abortFullRestore() throws RemoteException {
        return getDelegate().abortFullRestore();
    }

    public int getTransportFlags() throws RemoteException {
        return getDelegate().getTransportFlags();
    }
}