package com.android.server.backup;

import android.util.Slog;
import java.text.SimpleDateFormat;
import java.util.Date;

/* loaded from: classes.dex */
public class FileMetadata {
    public String domain;
    public boolean hasApk;
    public String installerPackageName;
    public long mode;
    public long mtime;
    public String packageName;
    public String path;
    public long size;
    public int type;
    public long version;

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("FileMetadata{");
        sb.append(this.packageName);
        sb.append(',');
        sb.append(this.type);
        sb.append(',');
        sb.append(this.domain);
        sb.append(':');
        sb.append(this.path);
        sb.append(',');
        sb.append(this.size);
        sb.append('}');
        return sb.toString();
    }

    public void dump() {
        StringBuilder b = new StringBuilder(128);
        b.append(this.type == 2 ? 'd' : '-');
        b.append((this.mode & 256) != 0 ? 'r' : '-');
        b.append((this.mode & 128) != 0 ? 'w' : '-');
        b.append((this.mode & 64) != 0 ? 'x' : '-');
        b.append((this.mode & 32) != 0 ? 'r' : '-');
        b.append((this.mode & 16) != 0 ? 'w' : '-');
        b.append((this.mode & 8) != 0 ? 'x' : '-');
        b.append((this.mode & 4) == 0 ? '-' : 'r');
        b.append((this.mode & 2) == 0 ? '-' : 'w');
        b.append((this.mode & 1) != 0 ? 'x' : '-');
        b.append(String.format(" %9d ", Long.valueOf(this.size)));
        Date stamp = new Date(this.mtime);
        b.append(new SimpleDateFormat("MMM dd HH:mm:ss ").format(stamp));
        b.append(this.packageName);
        b.append(" :: ");
        b.append(this.domain);
        b.append(" :: ");
        b.append(this.path);
        Slog.i(BackupManagerService.TAG, b.toString());
    }
}