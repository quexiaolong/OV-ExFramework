package com.android.server.timezone;

/* loaded from: classes2.dex */
final class PackageVersions {
    final long mDataAppVersion;
    final long mUpdateAppVersion;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageVersions(long updateAppVersion, long dataAppVersion) {
        this.mUpdateAppVersion = updateAppVersion;
        this.mDataAppVersion = dataAppVersion;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PackageVersions that = (PackageVersions) o;
        if (this.mUpdateAppVersion == that.mUpdateAppVersion && this.mDataAppVersion == that.mDataAppVersion) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = Long.hashCode(this.mUpdateAppVersion);
        return (result * 31) + Long.hashCode(this.mDataAppVersion);
    }

    public String toString() {
        return "PackageVersions{mUpdateAppVersion=" + this.mUpdateAppVersion + ", mDataAppVersion=" + this.mDataAppVersion + '}';
    }
}