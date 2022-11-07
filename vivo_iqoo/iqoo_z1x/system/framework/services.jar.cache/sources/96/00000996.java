package com.android.server.backup;

import android.content.ContentResolver;
import android.os.Handler;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.KeyValueSettingObserver;

/* loaded from: classes.dex */
public class BackupAgentTimeoutParameters extends KeyValueSettingObserver {
    public static final long DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS = 300000;
    public static final long DEFAULT_KV_BACKUP_AGENT_TIMEOUT_MILLIS = 30000;
    public static final long DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS = 3000;
    public static final long DEFAULT_RESTORE_AGENT_FINISHED_TIMEOUT_MILLIS = 30000;
    public static final long DEFAULT_RESTORE_AGENT_TIMEOUT_MILLIS = 60000;
    public static final long DEFAULT_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS = 1800000;
    public static final String SETTING = "backup_agent_timeout_parameters";
    public static final String SETTING_FULL_BACKUP_AGENT_TIMEOUT_MILLIS = "full_backup_agent_timeout_millis";
    public static final String SETTING_KV_BACKUP_AGENT_TIMEOUT_MILLIS = "kv_backup_agent_timeout_millis";
    public static final String SETTING_QUOTA_EXCEEDED_TIMEOUT_MILLIS = "quota_exceeded_timeout_millis";
    public static final String SETTING_RESTORE_AGENT_FINISHED_TIMEOUT_MILLIS = "restore_agent_finished_timeout_millis";
    public static final String SETTING_RESTORE_AGENT_TIMEOUT_MILLIS = "restore_agent_timeout_millis";
    public static final String SETTING_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS = "shared_backup_agent_timeout_millis";
    private long mFullBackupAgentTimeoutMillis;
    private long mKvBackupAgentTimeoutMillis;
    private final Object mLock;
    private long mQuotaExceededTimeoutMillis;
    private long mRestoreAgentFinishedTimeoutMillis;
    private long mRestoreAgentTimeoutMillis;
    private long mSharedBackupAgentTimeoutMillis;

    public BackupAgentTimeoutParameters(Handler handler, ContentResolver resolver) {
        super(handler, resolver, Settings.Global.getUriFor(SETTING));
        this.mLock = new Object();
    }

    public String getSettingValue(ContentResolver resolver) {
        return Settings.Global.getString(resolver, SETTING);
    }

    public void update(KeyValueListParser parser) {
        synchronized (this.mLock) {
            this.mKvBackupAgentTimeoutMillis = parser.getLong(SETTING_KV_BACKUP_AGENT_TIMEOUT_MILLIS, 30000L);
            this.mFullBackupAgentTimeoutMillis = parser.getLong(SETTING_FULL_BACKUP_AGENT_TIMEOUT_MILLIS, (long) DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
            this.mSharedBackupAgentTimeoutMillis = parser.getLong(SETTING_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS, 1800000L);
            this.mRestoreAgentTimeoutMillis = parser.getLong(SETTING_RESTORE_AGENT_TIMEOUT_MILLIS, 60000L);
            this.mRestoreAgentFinishedTimeoutMillis = parser.getLong(SETTING_RESTORE_AGENT_FINISHED_TIMEOUT_MILLIS, 30000L);
            this.mQuotaExceededTimeoutMillis = parser.getLong(SETTING_QUOTA_EXCEEDED_TIMEOUT_MILLIS, (long) DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS);
        }
    }

    public long getKvBackupAgentTimeoutMillis() {
        long j;
        synchronized (this.mLock) {
            j = this.mKvBackupAgentTimeoutMillis;
        }
        return j;
    }

    public long getFullBackupAgentTimeoutMillis() {
        long j;
        synchronized (this.mLock) {
            j = this.mFullBackupAgentTimeoutMillis;
        }
        return j;
    }

    public long getSharedBackupAgentTimeoutMillis() {
        long j;
        synchronized (this.mLock) {
            j = this.mSharedBackupAgentTimeoutMillis;
        }
        return j;
    }

    public long getRestoreAgentTimeoutMillis() {
        long j;
        synchronized (this.mLock) {
            j = this.mRestoreAgentTimeoutMillis;
        }
        return j;
    }

    public long getRestoreAgentFinishedTimeoutMillis() {
        long j;
        synchronized (this.mLock) {
            j = this.mRestoreAgentFinishedTimeoutMillis;
        }
        return j;
    }

    public long getQuotaExceededTimeoutMillis() {
        long j;
        synchronized (this.mLock) {
            j = this.mQuotaExceededTimeoutMillis;
        }
        return j;
    }
}