package com.android.server.backup.keyvalue;

import java.util.Objects;

/* loaded from: classes.dex */
public class BackupRequest {
    public String packageName;

    public BackupRequest(String pkgName) {
        this.packageName = pkgName;
    }

    public String toString() {
        return "BackupRequest{pkg=" + this.packageName + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BackupRequest)) {
            return false;
        }
        BackupRequest that = (BackupRequest) o;
        return Objects.equals(this.packageName, that.packageName);
    }

    public int hashCode() {
        return Objects.hash(this.packageName);
    }
}