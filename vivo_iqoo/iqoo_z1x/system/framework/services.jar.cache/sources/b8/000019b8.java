package com.android.server.textclassifier;

import android.net.Uri;
import android.util.ArrayMap;
import android.util.Log;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/* loaded from: classes2.dex */
public final class IconsUriHelper {
    public static final String AUTHORITY = "com.android.textclassifier.icons";
    private static final String TAG = "IconsUriHelper";
    private final Supplier<String> mIdSupplier;
    private final Map<String, String> mPackageIds = new ArrayMap();
    private static final Supplier<String> DEFAULT_ID_SUPPLIER = $$Lambda$IconsUriHelper$xs4gzwHiyi5MNRelcf1JWo71zo.INSTANCE;
    private static final IconsUriHelper sSingleton = new IconsUriHelper(null);

    private IconsUriHelper(Supplier<String> idSupplier) {
        this.mIdSupplier = idSupplier != null ? idSupplier : DEFAULT_ID_SUPPLIER;
        this.mPackageIds.put(PackageManagerService.PLATFORM_PACKAGE_NAME, PackageManagerService.PLATFORM_PACKAGE_NAME);
    }

    public static IconsUriHelper newInstanceForTesting(Supplier<String> idSupplier) {
        return new IconsUriHelper(idSupplier);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static IconsUriHelper getInstance() {
        return sSingleton;
    }

    public Uri getContentUri(String packageName, int resId) {
        Uri build;
        Objects.requireNonNull(packageName);
        synchronized (this.mPackageIds) {
            if (!this.mPackageIds.containsKey(packageName)) {
                this.mPackageIds.put(packageName, this.mIdSupplier.get());
            }
            build = new Uri.Builder().scheme(ActivityTaskManagerInternal.ASSIST_KEY_CONTENT).authority(AUTHORITY).path(this.mPackageIds.get(packageName)).appendPath(Integer.toString(resId)).build();
        }
        return build;
    }

    public ResourceInfo getResourceInfo(Uri uri) {
        if (ActivityTaskManagerInternal.ASSIST_KEY_CONTENT.equals(uri.getScheme()) && AUTHORITY.equals(uri.getAuthority())) {
            List<String> pathItems = uri.getPathSegments();
            try {
            } catch (Exception e) {
                Log.v(TAG, "Could not get resource info. Reason: " + e.getMessage());
            }
            synchronized (this.mPackageIds) {
                String packageId = pathItems.get(0);
                int resId = Integer.parseInt(pathItems.get(1));
                for (String packageName : this.mPackageIds.keySet()) {
                    if (packageId.equals(this.mPackageIds.get(packageName))) {
                        return new ResourceInfo(packageName, resId);
                    }
                }
                return null;
            }
        }
        return null;
    }

    /* loaded from: classes2.dex */
    public static final class ResourceInfo {
        public final int id;
        public final String packageName;

        private ResourceInfo(String packageName, int id) {
            Objects.requireNonNull(packageName);
            this.packageName = packageName;
            this.id = id;
        }
    }
}