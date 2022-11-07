package com.vivo.services.backup;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;

/* loaded from: classes.dex */
public class VivoBackupManagerServiceProxy {
    public static final int VIVO_USER_OWNER = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void fullBackup(ParcelFileDescriptor writeFile, String[] pkgNames, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, boolean doAllApps, boolean includeSystem, boolean compress, boolean doKeyValue) throws RemoteException {
        ServiceManager.getService("backup").adbBackup(0, writeFile, includeApks, includeObbs, includeShared, doWidgets, doAllApps, includeSystem, compress, doKeyValue, pkgNames);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void fullRestore(ParcelFileDescriptor readFile) throws RemoteException {
        ServiceManager.getService("backup").adbRestore(0, readFile);
    }
}