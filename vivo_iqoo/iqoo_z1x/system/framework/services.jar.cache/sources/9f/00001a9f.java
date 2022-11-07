package com.android.server.usage;

import android.content.pm.PackageStats;

/* loaded from: classes2.dex */
public abstract class StorageStatsManagerInternal {

    /* loaded from: classes2.dex */
    public interface StorageStatsAugmenter {
        void augmentStatsForPackage(PackageStats packageStats, String str, int i, boolean z);

        void augmentStatsForUid(PackageStats packageStats, int i, boolean z);
    }

    public abstract void registerStorageStatsAugmenter(StorageStatsAugmenter storageStatsAugmenter, String str);
}