package com.android.server.timezone;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* loaded from: classes2.dex */
final class PackageStatus {
    static final int CHECK_COMPLETED_FAILURE = 3;
    static final int CHECK_COMPLETED_SUCCESS = 2;
    static final int CHECK_STARTED = 1;
    final int mCheckStatus;
    final PackageVersions mVersions;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface CheckStatus {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageStatus(int checkStatus, PackageVersions versions) {
        this.mCheckStatus = checkStatus;
        if (checkStatus < 1 || checkStatus > 3) {
            throw new IllegalArgumentException("Unknown checkStatus " + checkStatus);
        } else if (versions == null) {
            throw new NullPointerException("versions == null");
        } else {
            this.mVersions = versions;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PackageStatus that = (PackageStatus) o;
        if (this.mCheckStatus != that.mCheckStatus) {
            return false;
        }
        return this.mVersions.equals(that.mVersions);
    }

    public int hashCode() {
        int result = this.mCheckStatus;
        return (result * 31) + this.mVersions.hashCode();
    }

    public String toString() {
        return "PackageStatus{mCheckStatus=" + this.mCheckStatus + ", mVersions=" + this.mVersions + '}';
    }
}