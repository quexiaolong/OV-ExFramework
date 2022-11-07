package com.android.server.pm.permission;

import android.content.pm.PackageManagerInternal;
import android.os.FtBuild;
import android.provider.DeviceConfig;
import com.android.server.LocalServices;

/* loaded from: classes.dex */
public class VivoOneTimePermissionUserManagerImpl implements IVivoOneTimePermissionUserManager {
    private static final long DEFAULT_KILLED_DELAY_MILLIS = 5000;
    private static final long DEFAULT_KILLED_DELAY_MILLIS_TAIER = 0;
    public static final String PROPERTY_KILLED_DELAY_CONFIG_KEY = "one_time_permissions_killed_delay_millis";
    static final String TAG = "VivoOneTimePermissionUserManagerImpl";
    private PackageManagerInternal mPackageManagerInternal;

    public VivoOneTimePermissionUserManagerImpl() {
        this.mPackageManagerInternal = null;
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
    }

    public long getKilledDelayMillis(String pkg) {
        try {
            if (!FtBuild.isOverSeas() && !this.mPackageManagerInternal.isTestApp(pkg)) {
                return DeviceConfig.getLong("permissions", PROPERTY_KILLED_DELAY_CONFIG_KEY, 0L);
            }
        } catch (Exception e) {
        }
        return DeviceConfig.getLong("permissions", PROPERTY_KILLED_DELAY_CONFIG_KEY, 5000L);
    }
}