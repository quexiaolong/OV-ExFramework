package com.android.server.backup.fullbackup;

/* loaded from: classes.dex */
public class FullBackupEntry implements Comparable<FullBackupEntry> {
    public long lastBackup;
    public String packageName;

    public FullBackupEntry(String pkg, long when) {
        this.packageName = pkg;
        this.lastBackup = when;
    }

    @Override // java.lang.Comparable
    public int compareTo(FullBackupEntry other) {
        long j = this.lastBackup;
        long j2 = other.lastBackup;
        if (j < j2) {
            return -1;
        }
        if (j > j2) {
            return 1;
        }
        return 0;
    }
}