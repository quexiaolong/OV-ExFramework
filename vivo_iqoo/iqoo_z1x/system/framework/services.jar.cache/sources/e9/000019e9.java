package com.android.server.timezone;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.UserHandle;
import android.util.Slog;
import java.util.List;

/* loaded from: classes2.dex */
final class PackageTrackerHelperImpl implements ConfigHelper, PackageManagerHelper {
    private static final String TAG = "PackageTrackerHelperImpl";
    private final Context mContext;
    private final PackageManager mPackageManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageTrackerHelperImpl(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
    }

    @Override // com.android.server.timezone.ConfigHelper
    public boolean isTrackingEnabled() {
        return this.mContext.getResources().getBoolean(17891565);
    }

    @Override // com.android.server.timezone.ConfigHelper
    public String getUpdateAppPackageName() {
        return this.mContext.getResources().getString(17039973);
    }

    @Override // com.android.server.timezone.ConfigHelper
    public String getDataAppPackageName() {
        Resources resources = this.mContext.getResources();
        return resources.getString(17039972);
    }

    @Override // com.android.server.timezone.ConfigHelper
    public int getCheckTimeAllowedMillis() {
        return this.mContext.getResources().getInteger(17694912);
    }

    @Override // com.android.server.timezone.ConfigHelper
    public int getFailedCheckRetryCount() {
        return this.mContext.getResources().getInteger(17694911);
    }

    @Override // com.android.server.timezone.PackageManagerHelper
    public long getInstalledPackageVersion(String packageName) throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 32768);
        return packageInfo.getLongVersionCode();
    }

    @Override // com.android.server.timezone.PackageManagerHelper
    public boolean isPrivilegedApp(String packageName) throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 32768);
        return packageInfo.applicationInfo.isPrivilegedApp();
    }

    @Override // com.android.server.timezone.PackageManagerHelper
    public boolean usesPermission(String packageName, String requiredPermissionName) throws PackageManager.NameNotFoundException {
        String[] strArr;
        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 36864);
        if (packageInfo.requestedPermissions == null) {
            return false;
        }
        for (String requestedPermission : packageInfo.requestedPermissions) {
            if (requiredPermissionName.equals(requestedPermission)) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.timezone.PackageManagerHelper
    public boolean contentProviderRegistered(String authority, String requiredPackageName) {
        ProviderInfo providerInfo = this.mPackageManager.resolveContentProviderAsUser(authority, 32768, UserHandle.SYSTEM.getIdentifier());
        if (providerInfo == null) {
            Slog.i(TAG, "contentProviderRegistered: No content provider registered with authority=" + authority);
            return false;
        }
        boolean packageMatches = requiredPackageName.equals(providerInfo.applicationInfo.packageName);
        if (!packageMatches) {
            Slog.i(TAG, "contentProviderRegistered: App with packageName=" + requiredPackageName + " does not expose the a content provider with authority=" + authority);
            return false;
        }
        return true;
    }

    @Override // com.android.server.timezone.PackageManagerHelper
    public boolean receiverRegistered(Intent intent, String requiredPermissionName) throws PackageManager.NameNotFoundException {
        List<ResolveInfo> resolveInfo = this.mPackageManager.queryBroadcastReceiversAsUser(intent, 32768, UserHandle.SYSTEM);
        if (resolveInfo.size() != 1) {
            Slog.i(TAG, "receiverRegistered: Zero or multiple broadcast receiver registered for intent=" + intent + ", found=" + resolveInfo);
            return false;
        }
        ResolveInfo matched = resolveInfo.get(0);
        boolean requiresPermission = requiredPermissionName.equals(matched.activityInfo.permission);
        if (!requiresPermission) {
            Slog.i(TAG, "receiverRegistered: Broadcast receiver registered for intent=" + intent + " must require permission " + requiredPermissionName);
        }
        return requiresPermission;
    }
}