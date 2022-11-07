package com.android.server.pm;

import android.content.Context;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class ProtectedPackages {
    private final Context mContext;
    private String mDeviceOwnerPackage;
    private List<String> mDeviceOwnerProtectedPackages;
    private int mDeviceOwnerUserId;
    private final String mDeviceProvisioningPackage;
    private SparseArray<String> mProfileOwnerPackages;

    public ProtectedPackages(Context context) {
        this.mContext = context;
        this.mDeviceProvisioningPackage = context.getResources().getString(17039899);
    }

    public synchronized void setDeviceAndProfileOwnerPackages(int deviceOwnerUserId, String deviceOwnerPackage, SparseArray<String> profileOwnerPackages) {
        this.mDeviceOwnerUserId = deviceOwnerUserId;
        SparseArray<String> sparseArray = null;
        this.mDeviceOwnerPackage = deviceOwnerUserId == -10000 ? null : deviceOwnerPackage;
        if (profileOwnerPackages != null) {
            sparseArray = profileOwnerPackages.clone();
        }
        this.mProfileOwnerPackages = sparseArray;
    }

    public synchronized void setDeviceOwnerProtectedPackages(List<String> packageNames) {
        this.mDeviceOwnerProtectedPackages = new ArrayList(packageNames);
    }

    private synchronized boolean hasDeviceOwnerOrProfileOwner(int userId, String packageName) {
        if (packageName == null) {
            return false;
        }
        if (this.mDeviceOwnerPackage != null && this.mDeviceOwnerUserId == userId && packageName.equals(this.mDeviceOwnerPackage)) {
            return true;
        }
        if (this.mProfileOwnerPackages != null) {
            if (packageName.equals(this.mProfileOwnerPackages.get(userId))) {
                return true;
            }
        }
        return false;
    }

    public synchronized String getDeviceOwnerOrProfileOwnerPackage(int userId) {
        if (this.mDeviceOwnerUserId == userId) {
            return this.mDeviceOwnerPackage;
        } else if (this.mProfileOwnerPackages == null) {
            return null;
        } else {
            return this.mProfileOwnerPackages.get(userId);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:7:0x0011, code lost:
        if (com.android.internal.util.ArrayUtils.contains(r1.mDeviceOwnerProtectedPackages, r2) != false) goto L13;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private synchronized boolean isProtectedPackage(java.lang.String r2) {
        /*
            r1 = this;
            monitor-enter(r1)
            if (r2 == 0) goto L18
            java.lang.String r0 = r1.mDeviceProvisioningPackage     // Catch: java.lang.Throwable -> L15
            boolean r0 = r2.equals(r0)     // Catch: java.lang.Throwable -> L15
            if (r0 != 0) goto L13
            java.util.List<java.lang.String> r0 = r1.mDeviceOwnerProtectedPackages     // Catch: java.lang.Throwable -> L15
            boolean r0 = com.android.internal.util.ArrayUtils.contains(r0, r2)     // Catch: java.lang.Throwable -> L15
            if (r0 == 0) goto L18
        L13:
            r0 = 1
            goto L19
        L15:
            r2 = move-exception
            monitor-exit(r1)
            throw r2
        L18:
            r0 = 0
        L19:
            monitor-exit(r1)
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.ProtectedPackages.isProtectedPackage(java.lang.String):boolean");
    }

    public boolean isPackageStateProtected(int userId, String packageName) {
        return hasDeviceOwnerOrProfileOwner(userId, packageName) || isProtectedPackage(packageName);
    }

    public boolean isPackageDataProtected(int userId, String packageName) {
        return hasDeviceOwnerOrProfileOwner(userId, packageName) || isProtectedPackage(packageName);
    }
}