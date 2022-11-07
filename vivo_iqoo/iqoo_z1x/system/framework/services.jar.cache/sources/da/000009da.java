package com.android.server.backup.fullbackup;

import android.app.IBackupAgent;
import android.content.pm.PackageInfo;

/* loaded from: classes.dex */
public interface FullBackupPreflight {
    long getExpectedSizeOrErrorCode();

    int preflightFullBackup(PackageInfo packageInfo, IBackupAgent iBackupAgent);
}