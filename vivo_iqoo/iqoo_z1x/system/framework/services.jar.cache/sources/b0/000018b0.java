package com.android.server.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.security.keymaster.IKeyAttestationApplicationIdProvider;
import android.security.keymaster.KeyAttestationApplicationId;
import android.security.keymaster.KeyAttestationPackageInfo;

/* loaded from: classes2.dex */
public class KeyAttestationApplicationIdProviderService extends IKeyAttestationApplicationIdProvider.Stub {
    private PackageManager mPackageManager;

    public KeyAttestationApplicationIdProviderService(Context context) {
        this.mPackageManager = context.getPackageManager();
    }

    public KeyAttestationApplicationId getKeyAttestationApplicationId(int uid) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1017 && callingUid != 1076) {
            throw new SecurityException("This service can only be used by Keystore or Credstore");
        }
        long token = Binder.clearCallingIdentity();
        try {
            try {
                String[] packageNames = this.mPackageManager.getPackagesForUid(uid);
                if (packageNames == null) {
                    throw new RemoteException("No packages for uid");
                }
                int userId = UserHandle.getUserId(uid);
                KeyAttestationPackageInfo[] keyAttestationPackageInfos = new KeyAttestationPackageInfo[packageNames.length];
                for (int i = 0; i < packageNames.length; i++) {
                    PackageInfo packageInfo = this.mPackageManager.getPackageInfoAsUser(packageNames[i], 64, userId);
                    keyAttestationPackageInfos[i] = new KeyAttestationPackageInfo(packageNames[i], packageInfo.getLongVersionCode(), packageInfo.signatures);
                }
                Binder.restoreCallingIdentity(token);
                return new KeyAttestationApplicationId(keyAttestationPackageInfos);
            } catch (PackageManager.NameNotFoundException nnfe) {
                throw new RemoteException(nnfe.getMessage());
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }
}