package com.android.server.pm.pkg;

import android.annotation.NonNull;
import android.content.pm.SharedLibraryInfo;
import com.android.internal.util.AnnotationValidations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* loaded from: classes.dex */
public class PackageStateUnserialized {
    private boolean hiddenUntilInstalled;
    private volatile long[] lastPackageUsageTimeInMills;
    private String overrideSeInfo;
    private boolean updatedSystemApp;
    private List<SharedLibraryInfo> usesLibraryInfos = Collections.emptyList();
    private List<String> usesLibraryFiles = Collections.emptyList();

    private long[] lazyInitLastPackageUsageTimeInMills() {
        return new long[8];
    }

    public PackageStateUnserialized setLastPackageUsageTimeInMills(int reason, long time) {
        if (reason < 0) {
            return this;
        }
        if (reason >= 8) {
            return this;
        }
        getLastPackageUsageTimeInMills()[reason] = time;
        return this;
    }

    public long getLatestPackageUseTimeInMills() {
        long[] lastPackageUsageTimeInMills;
        long latestUse = 0;
        for (long use : getLastPackageUsageTimeInMills()) {
            latestUse = Math.max(latestUse, use);
        }
        return latestUse;
    }

    public long getLatestForegroundPackageUseTimeInMills() {
        int[] foregroundReasons = {0, 2};
        long latestUse = 0;
        for (int reason : foregroundReasons) {
            latestUse = Math.max(latestUse, getLastPackageUsageTimeInMills()[reason]);
        }
        return latestUse;
    }

    public void updateFrom(PackageStateUnserialized other) {
        this.hiddenUntilInstalled = other.hiddenUntilInstalled;
        if (!other.usesLibraryInfos.isEmpty()) {
            this.usesLibraryInfos = new ArrayList(other.usesLibraryInfos);
        }
        if (!other.usesLibraryFiles.isEmpty()) {
            this.usesLibraryFiles = new ArrayList(other.usesLibraryFiles);
        }
        this.updatedSystemApp = other.updatedSystemApp;
        this.lastPackageUsageTimeInMills = other.lastPackageUsageTimeInMills;
        this.overrideSeInfo = other.overrideSeInfo;
    }

    public boolean isHiddenUntilInstalled() {
        return this.hiddenUntilInstalled;
    }

    public List<SharedLibraryInfo> getUsesLibraryInfos() {
        return this.usesLibraryInfos;
    }

    public List<String> getUsesLibraryFiles() {
        return this.usesLibraryFiles;
    }

    public boolean isUpdatedSystemApp() {
        return this.updatedSystemApp;
    }

    public long[] getLastPackageUsageTimeInMills() {
        long[] _lastPackageUsageTimeInMills = this.lastPackageUsageTimeInMills;
        if (_lastPackageUsageTimeInMills == null) {
            synchronized (this) {
                _lastPackageUsageTimeInMills = this.lastPackageUsageTimeInMills;
                if (_lastPackageUsageTimeInMills == null) {
                    long[] lazyInitLastPackageUsageTimeInMills = lazyInitLastPackageUsageTimeInMills();
                    this.lastPackageUsageTimeInMills = lazyInitLastPackageUsageTimeInMills;
                    _lastPackageUsageTimeInMills = lazyInitLastPackageUsageTimeInMills;
                }
            }
        }
        return _lastPackageUsageTimeInMills;
    }

    public String getOverrideSeInfo() {
        return this.overrideSeInfo;
    }

    public PackageStateUnserialized setHiddenUntilInstalled(boolean value) {
        this.hiddenUntilInstalled = value;
        return this;
    }

    public PackageStateUnserialized setUsesLibraryInfos(List<SharedLibraryInfo> value) {
        this.usesLibraryInfos = value;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, value);
        return this;
    }

    public PackageStateUnserialized setUsesLibraryFiles(List<String> value) {
        this.usesLibraryFiles = value;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, value);
        return this;
    }

    public PackageStateUnserialized setUpdatedSystemApp(boolean value) {
        this.updatedSystemApp = value;
        return this;
    }

    public PackageStateUnserialized setLastPackageUsageTimeInMills(long... value) {
        this.lastPackageUsageTimeInMills = value;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, this.lastPackageUsageTimeInMills);
        return this;
    }

    public PackageStateUnserialized setOverrideSeInfo(String value) {
        this.overrideSeInfo = value;
        return this;
    }

    @Deprecated
    private void __metadata() {
    }
}