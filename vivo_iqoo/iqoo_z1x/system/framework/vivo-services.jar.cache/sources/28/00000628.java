package com.vivo.services.backup.util;

import vivo.app.VivoFrameworkFactory;
import vivo.app.backup.AbsVivoBackupManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoBackupCommonUtil {
    public static final String COMMON_TAG = "VIVO_BACKUP_";
    public static final int ERROR_AGENT = 5;
    public static final long POST_PROGRESS_INTERVAL = 1000;
    public static final long RETRIES_BIND_AGENT = 3;
    public static final long RETRY_BIND_INTERVAL = 5000;
    public static final String TAG = "VIVO_BACKUP_VivoBackupCommonUtil";
    public static final long TIMEOUT_CLEAR_APPDATA = 60000;
    public static final long TIMEOUT_MONITOR_INTERVAL = 30000;

    public static AbsVivoBackupManager getVivoBackupManager() {
        VivoFrameworkFactory frameworkFactoryImpl = VivoFrameworkFactory.getFrameworkFactoryImpl();
        if (frameworkFactoryImpl == null) {
            VSlog.e(TAG, "Fatal error: getFrameworkFactoryImpl = null");
            return null;
        }
        AbsVivoBackupManager vivoBackupManager = frameworkFactoryImpl.getVivoBackupManager();
        if (vivoBackupManager == null) {
            VSlog.e(TAG, "Fatal error: getVivoBackupManager = null");
            return null;
        }
        return vivoBackupManager;
    }
}