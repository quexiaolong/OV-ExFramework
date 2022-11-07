package com.android.server.location;

import android.content.Context;
import android.os.Binder;
import android.os.UserHandle;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.server.slice.SliceClientPermissions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/* loaded from: classes.dex */
public final class CallerIdentity {
    public static final int PERMISSION_COARSE = 1;
    public static final int PERMISSION_FINE = 2;
    public static final int PERMISSION_NONE = 0;
    public final String featureId;
    public final String listenerId;
    public final String packageName;
    public final int permissionLevel;
    public final int pid;
    public final int uid;
    public final int userId;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface PermissionLevel {
    }

    public static String asPermission(int permissionLevel) {
        if (permissionLevel != 1) {
            if (permissionLevel == 2) {
                return "android.permission.ACCESS_FINE_LOCATION";
            }
            throw new IllegalArgumentException();
        }
        return "android.permission.ACCESS_COARSE_LOCATION";
    }

    public static int asAppOp(int permissionLevel) {
        if (permissionLevel != 1) {
            if (permissionLevel == 2) {
                return 1;
            }
            throw new IllegalArgumentException();
        }
        return 0;
    }

    public static CallerIdentity fromBinder(Context context, String packageName, String featureId) {
        return fromBinder(context, packageName, featureId, null);
    }

    public static CallerIdentity fromBinder(Context context, String packageName, String featureId, String listenerId) {
        int uid = Binder.getCallingUid();
        if (!ArrayUtils.contains(context.getPackageManager().getPackagesForUid(uid), packageName)) {
            throw new SecurityException("invalid package \"" + packageName + "\" for uid " + uid);
        }
        return fromBinderUnsafe(context, packageName, featureId, listenerId);
    }

    public static CallerIdentity fromBinderUnsafe(Context context, String packageName, String featureId) {
        return fromBinderUnsafe(context, packageName, featureId, null);
    }

    public static CallerIdentity fromBinderUnsafe(Context context, String packageName, String featureId, String listenerId) {
        return new CallerIdentity(Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId(), packageName, featureId, listenerId, getBinderPermissionLevel(context));
    }

    public static void enforceCallingOrSelfLocationPermission(Context context) {
        enforceLocationPermission(Binder.getCallingUid(), getBinderPermissionLevel(context));
    }

    public static boolean checkCallingOrSelfLocationPermission(Context context) {
        return checkLocationPermission(getBinderPermissionLevel(context));
    }

    private static void enforceLocationPermission(int uid, int permissionLevel) {
        if (checkLocationPermission(permissionLevel)) {
            return;
        }
        throw new SecurityException("uid " + uid + " does not have android.permission.ACCESS_COARSE_LOCATION or android.permission.ACCESS_FINE_LOCATION.");
    }

    private static boolean checkLocationPermission(int permissionLevel) {
        return permissionLevel >= 1;
    }

    private static int getBinderPermissionLevel(Context context) {
        if (context.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0) {
            return 2;
        }
        if (context.checkCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == 0) {
            return 1;
        }
        return 0;
    }

    public CallerIdentity(int uid, int pid, int userId, String packageName, String featureId, int permissionLevel) {
        this(uid, pid, userId, packageName, featureId, null, permissionLevel);
    }

    private CallerIdentity(int uid, int pid, int userId, String packageName, String featureId, String listenerId, int permissionLevel) {
        this.uid = uid;
        this.pid = pid;
        this.userId = userId;
        Objects.requireNonNull(packageName);
        this.packageName = packageName;
        this.featureId = featureId;
        this.listenerId = listenerId;
        this.permissionLevel = Preconditions.checkArgumentInRange(permissionLevel, 0, 2, "permissionLevel");
    }

    public void enforceLocationPermission() {
        enforceLocationPermission(this.uid, this.permissionLevel);
    }

    public String toString() {
        int length = this.packageName.length() + 10;
        String str = this.featureId;
        if (str != null) {
            length += str.length();
        }
        StringBuilder builder = new StringBuilder(length);
        builder.append(this.pid);
        builder.append(SliceClientPermissions.SliceAuthority.DELIMITER);
        builder.append(this.packageName);
        if (this.featureId != null) {
            builder.append("[");
            if (this.featureId.startsWith(this.packageName)) {
                builder.append(this.featureId.substring(this.packageName.length()));
            } else {
                builder.append(this.featureId);
            }
            builder.append("]");
        }
        return builder.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof CallerIdentity) {
            CallerIdentity that = (CallerIdentity) o;
            return this.uid == that.uid && this.pid == that.pid && this.packageName.equals(that.packageName) && Objects.equals(this.featureId, that.featureId) && Objects.equals(this.listenerId, that.listenerId);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.uid), Integer.valueOf(this.pid), this.packageName, this.featureId);
    }
}