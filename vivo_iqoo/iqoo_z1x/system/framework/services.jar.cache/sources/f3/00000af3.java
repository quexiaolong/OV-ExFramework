package com.android.server.blob;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.text.format.TimeMigrationUtils;
import android.util.Slog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class BlobStoreUtils {
    private static final String DESC_RES_TYPE_STRING = "string";

    BlobStoreUtils() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Resources getPackageResources(Context context, String packageName, int userId) {
        try {
            return context.getPackageManager().getResourcesForApplicationAsUser(packageName, userId);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.d(BlobStoreConfig.TAG, "Unknown package in user " + userId + ": " + packageName, e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getDescriptionResourceId(Resources resources, String resourceEntryName, String packageName) {
        return resources.getIdentifier(resourceEntryName, DESC_RES_TYPE_STRING, packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getDescriptionResourceId(Context context, String resourceEntryName, String packageName, int userId) {
        Resources resources = getPackageResources(context, packageName, userId);
        if (resources == null) {
            return 0;
        }
        return getDescriptionResourceId(resources, resourceEntryName, packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String formatTime(long timeMs) {
        return TimeMigrationUtils.formatMillisWithFixedFormat(timeMs);
    }
}