package com.android.server.blob;

import android.content.Context;
import android.os.Environment;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.DataUnit;
import android.util.Log;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import com.android.server.blob.BlobStoreConfig;
import com.android.server.display.color.DisplayTransformManager;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/* loaded from: classes.dex */
class BlobStoreConfig {
    private static final String BLOBS_DIR_NAME = "blobs";
    private static final String BLOBS_INDEX_FILE_NAME = "blobs_index.xml";
    public static final int IDLE_JOB_ID = 191934935;
    public static final long INVALID_BLOB_ID = 0;
    public static final long INVALID_BLOB_SIZE = 0;
    private static final String ROOT_DIR_NAME = "blobstore";
    private static final String SESSIONS_INDEX_FILE_NAME = "sessions_index.xml";
    public static final int XML_VERSION_ADD_COMMIT_TIME = 4;
    public static final int XML_VERSION_ADD_DESC_RES_NAME = 3;
    public static final int XML_VERSION_ADD_SESSION_CREATION_TIME = 5;
    public static final int XML_VERSION_ADD_STRING_DESC = 2;
    public static final int XML_VERSION_CURRENT = 5;
    public static final int XML_VERSION_INIT = 1;
    public static final String TAG = "BlobStore";
    public static final boolean LOGV = Log.isLoggable(TAG, 2);

    BlobStoreConfig() {
    }

    /* loaded from: classes.dex */
    public static class DeviceConfigProperties {
        public static long COMMIT_COOL_OFF_DURATION_MS = 0;
        public static final long DEFAULT_COMMIT_COOL_OFF_DURATION_MS;
        public static final long DEFAULT_DELETE_ON_LAST_LEASE_DELAY_MS;
        public static final long DEFAULT_IDLE_JOB_PERIOD_MS;
        public static final long DEFAULT_LEASE_ACQUISITION_WAIT_DURATION_MS;
        public static int DEFAULT_LEASE_DESC_CHAR_LIMIT = 0;
        public static int DEFAULT_MAX_ACTIVE_SESSIONS = 0;
        public static int DEFAULT_MAX_BLOB_ACCESS_PERMITTED_PACKAGES = 0;
        public static int DEFAULT_MAX_COMMITTED_BLOBS = 0;
        public static int DEFAULT_MAX_LEASED_BLOBS = 0;
        public static final long DEFAULT_SESSION_EXPIRY_TIMEOUT_MS;
        public static final long DEFAULT_TOTAL_BYTES_PER_APP_LIMIT_FLOOR;
        public static final float DEFAULT_TOTAL_BYTES_PER_APP_LIMIT_FRACTION = 0.01f;
        public static final boolean DEFAULT_USE_REVOCABLE_FD_FOR_READS = true;
        public static long DELETE_ON_LAST_LEASE_DELAY_MS = 0;
        public static long IDLE_JOB_PERIOD_MS = 0;
        public static final String KEY_COMMIT_COOL_OFF_DURATION_MS = "commit_cool_off_duration_ms";
        public static final String KEY_DELETE_ON_LAST_LEASE_DELAY_MS = "delete_on_last_lease_delay_ms";
        public static final String KEY_IDLE_JOB_PERIOD_MS = "idle_job_period_ms";
        public static final String KEY_LEASE_ACQUISITION_WAIT_DURATION_MS = "lease_acquisition_wait_time_ms";
        public static final String KEY_LEASE_DESC_CHAR_LIMIT = "lease_desc_char_limit";
        public static final String KEY_MAX_ACTIVE_SESSIONS = "max_active_sessions";
        public static final String KEY_MAX_BLOB_ACCESS_PERMITTED_PACKAGES = "max_permitted_pks";
        public static final String KEY_MAX_COMMITTED_BLOBS = "max_committed_blobs";
        public static final String KEY_MAX_LEASED_BLOBS = "max_leased_blobs";
        public static final String KEY_SESSION_EXPIRY_TIMEOUT_MS = "session_expiry_timeout_ms";
        public static final String KEY_TOTAL_BYTES_PER_APP_LIMIT_FLOOR = "total_bytes_per_app_limit_floor";
        public static final String KEY_TOTAL_BYTES_PER_APP_LIMIT_FRACTION = "total_bytes_per_app_limit_fraction";
        public static final String KEY_USE_REVOCABLE_FD_FOR_READS = "use_revocable_fd_for_reads";
        public static long LEASE_ACQUISITION_WAIT_DURATION_MS;
        public static int LEASE_DESC_CHAR_LIMIT;
        public static int MAX_ACTIVE_SESSIONS;
        public static int MAX_BLOB_ACCESS_PERMITTED_PACKAGES;
        public static int MAX_COMMITTED_BLOBS;
        public static int MAX_LEASED_BLOBS;
        public static long SESSION_EXPIRY_TIMEOUT_MS;
        public static long TOTAL_BYTES_PER_APP_LIMIT_FLOOR;
        public static float TOTAL_BYTES_PER_APP_LIMIT_FRACTION;
        public static boolean USE_REVOCABLE_FD_FOR_READS;

        static {
            long millis = TimeUnit.DAYS.toMillis(1L);
            DEFAULT_IDLE_JOB_PERIOD_MS = millis;
            IDLE_JOB_PERIOD_MS = millis;
            long millis2 = TimeUnit.DAYS.toMillis(7L);
            DEFAULT_SESSION_EXPIRY_TIMEOUT_MS = millis2;
            SESSION_EXPIRY_TIMEOUT_MS = millis2;
            long bytes = DataUnit.MEBIBYTES.toBytes(300L);
            DEFAULT_TOTAL_BYTES_PER_APP_LIMIT_FLOOR = bytes;
            TOTAL_BYTES_PER_APP_LIMIT_FLOOR = bytes;
            TOTAL_BYTES_PER_APP_LIMIT_FRACTION = 0.01f;
            long millis3 = TimeUnit.HOURS.toMillis(6L);
            DEFAULT_LEASE_ACQUISITION_WAIT_DURATION_MS = millis3;
            LEASE_ACQUISITION_WAIT_DURATION_MS = millis3;
            long millis4 = TimeUnit.HOURS.toMillis(48L);
            DEFAULT_COMMIT_COOL_OFF_DURATION_MS = millis4;
            COMMIT_COOL_OFF_DURATION_MS = millis4;
            USE_REVOCABLE_FD_FOR_READS = true;
            long millis5 = TimeUnit.HOURS.toMillis(6L);
            DEFAULT_DELETE_ON_LAST_LEASE_DELAY_MS = millis5;
            DELETE_ON_LAST_LEASE_DELAY_MS = millis5;
            DEFAULT_MAX_ACTIVE_SESSIONS = 250;
            MAX_ACTIVE_SESSIONS = 250;
            DEFAULT_MAX_COMMITTED_BLOBS = 1000;
            MAX_COMMITTED_BLOBS = 1000;
            DEFAULT_MAX_LEASED_BLOBS = SystemService.PHASE_SYSTEM_SERVICES_READY;
            MAX_LEASED_BLOBS = SystemService.PHASE_SYSTEM_SERVICES_READY;
            DEFAULT_MAX_BLOB_ACCESS_PERMITTED_PACKAGES = DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR;
            MAX_BLOB_ACCESS_PERMITTED_PACKAGES = DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR;
            DEFAULT_LEASE_DESC_CHAR_LIMIT = DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR;
            LEASE_DESC_CHAR_LIMIT = DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR;
        }

        public static void refresh(final DeviceConfig.Properties properties) {
            if (!BlobStoreConfig.ROOT_DIR_NAME.equals(properties.getNamespace())) {
                return;
            }
            properties.getKeyset().forEach(new Consumer() { // from class: com.android.server.blob.-$$Lambda$BlobStoreConfig$DeviceConfigProperties$7FeT9Nj22YRJdnAt_b-xbcQB1wI
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BlobStoreConfig.DeviceConfigProperties.lambda$refresh$0(properties, (String) obj);
                }
            });
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public static /* synthetic */ void lambda$refresh$0(DeviceConfig.Properties properties, String key) {
            char c;
            switch (key.hashCode()) {
                case -1925137189:
                    if (key.equals(KEY_MAX_ACTIVE_SESSIONS)) {
                        c = '\b';
                        break;
                    }
                    c = 65535;
                    break;
                case -1390984180:
                    if (key.equals(KEY_USE_REVOCABLE_FD_FOR_READS)) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case -409761467:
                    if (key.equals(KEY_LEASE_ACQUISITION_WAIT_DURATION_MS)) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case -284382148:
                    if (key.equals(KEY_MAX_LEASED_BLOBS)) {
                        c = '\n';
                        break;
                    }
                    c = 65535;
                    break;
                case 12210070:
                    if (key.equals(KEY_MAX_BLOB_ACCESS_PERMITTED_PACKAGES)) {
                        c = 11;
                        break;
                    }
                    c = 65535;
                    break;
                case 271099507:
                    if (key.equals(KEY_COMMIT_COOL_OFF_DURATION_MS)) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case 964101945:
                    if (key.equals(KEY_TOTAL_BYTES_PER_APP_LIMIT_FLOOR)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1401805851:
                    if (key.equals(KEY_LEASE_DESC_CHAR_LIMIT)) {
                        c = '\f';
                        break;
                    }
                    c = 65535;
                    break;
                case 1419134232:
                    if (key.equals(KEY_MAX_COMMITTED_BLOBS)) {
                        c = '\t';
                        break;
                    }
                    c = 65535;
                    break;
                case 1733063605:
                    if (key.equals(KEY_TOTAL_BYTES_PER_APP_LIMIT_FRACTION)) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 1838729636:
                    if (key.equals(KEY_DELETE_ON_LAST_LEASE_DELAY_MS)) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case 1903375799:
                    if (key.equals(KEY_IDLE_JOB_PERIOD_MS)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 2022996583:
                    if (key.equals(KEY_SESSION_EXPIRY_TIMEOUT_MS)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    IDLE_JOB_PERIOD_MS = properties.getLong(key, DEFAULT_IDLE_JOB_PERIOD_MS);
                    return;
                case 1:
                    SESSION_EXPIRY_TIMEOUT_MS = properties.getLong(key, DEFAULT_SESSION_EXPIRY_TIMEOUT_MS);
                    return;
                case 2:
                    TOTAL_BYTES_PER_APP_LIMIT_FLOOR = properties.getLong(key, DEFAULT_TOTAL_BYTES_PER_APP_LIMIT_FLOOR);
                    return;
                case 3:
                    TOTAL_BYTES_PER_APP_LIMIT_FRACTION = properties.getFloat(key, 0.01f);
                    return;
                case 4:
                    LEASE_ACQUISITION_WAIT_DURATION_MS = properties.getLong(key, DEFAULT_LEASE_ACQUISITION_WAIT_DURATION_MS);
                    return;
                case 5:
                    COMMIT_COOL_OFF_DURATION_MS = properties.getLong(key, DEFAULT_COMMIT_COOL_OFF_DURATION_MS);
                    return;
                case 6:
                    USE_REVOCABLE_FD_FOR_READS = properties.getBoolean(key, true);
                    return;
                case 7:
                    DELETE_ON_LAST_LEASE_DELAY_MS = properties.getLong(key, DEFAULT_DELETE_ON_LAST_LEASE_DELAY_MS);
                    return;
                case '\b':
                    MAX_ACTIVE_SESSIONS = properties.getInt(key, DEFAULT_MAX_ACTIVE_SESSIONS);
                    return;
                case '\t':
                    MAX_COMMITTED_BLOBS = properties.getInt(key, DEFAULT_MAX_COMMITTED_BLOBS);
                    return;
                case '\n':
                    MAX_LEASED_BLOBS = properties.getInt(key, DEFAULT_MAX_LEASED_BLOBS);
                    return;
                case 11:
                    MAX_BLOB_ACCESS_PERMITTED_PACKAGES = properties.getInt(key, DEFAULT_MAX_BLOB_ACCESS_PERMITTED_PACKAGES);
                    return;
                case '\f':
                    LEASE_DESC_CHAR_LIMIT = properties.getInt(key, DEFAULT_LEASE_DESC_CHAR_LIMIT);
                    return;
                default:
                    Slog.wtf(BlobStoreConfig.TAG, "Unknown key in device config properties: " + key);
                    return;
            }
        }

        static void dump(IndentingPrintWriter fout, Context context) {
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_IDLE_JOB_PERIOD_MS, TimeUtils.formatDuration(IDLE_JOB_PERIOD_MS), TimeUtils.formatDuration(DEFAULT_IDLE_JOB_PERIOD_MS)));
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_SESSION_EXPIRY_TIMEOUT_MS, TimeUtils.formatDuration(SESSION_EXPIRY_TIMEOUT_MS), TimeUtils.formatDuration(DEFAULT_SESSION_EXPIRY_TIMEOUT_MS)));
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_TOTAL_BYTES_PER_APP_LIMIT_FLOOR, Formatter.formatFileSize(context, TOTAL_BYTES_PER_APP_LIMIT_FLOOR, 8), Formatter.formatFileSize(context, DEFAULT_TOTAL_BYTES_PER_APP_LIMIT_FLOOR, 8)));
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_TOTAL_BYTES_PER_APP_LIMIT_FRACTION, Float.valueOf(TOTAL_BYTES_PER_APP_LIMIT_FRACTION), Float.valueOf(0.01f)));
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_LEASE_ACQUISITION_WAIT_DURATION_MS, TimeUtils.formatDuration(LEASE_ACQUISITION_WAIT_DURATION_MS), TimeUtils.formatDuration(DEFAULT_LEASE_ACQUISITION_WAIT_DURATION_MS)));
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_COMMIT_COOL_OFF_DURATION_MS, TimeUtils.formatDuration(COMMIT_COOL_OFF_DURATION_MS), TimeUtils.formatDuration(DEFAULT_COMMIT_COOL_OFF_DURATION_MS)));
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_USE_REVOCABLE_FD_FOR_READS, Boolean.valueOf(USE_REVOCABLE_FD_FOR_READS), true));
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_DELETE_ON_LAST_LEASE_DELAY_MS, TimeUtils.formatDuration(DELETE_ON_LAST_LEASE_DELAY_MS), TimeUtils.formatDuration(DEFAULT_DELETE_ON_LAST_LEASE_DELAY_MS)));
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_MAX_ACTIVE_SESSIONS, Integer.valueOf(MAX_ACTIVE_SESSIONS), Integer.valueOf(DEFAULT_MAX_ACTIVE_SESSIONS)));
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_MAX_COMMITTED_BLOBS, Integer.valueOf(MAX_COMMITTED_BLOBS), Integer.valueOf(DEFAULT_MAX_COMMITTED_BLOBS)));
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_MAX_LEASED_BLOBS, Integer.valueOf(MAX_LEASED_BLOBS), Integer.valueOf(DEFAULT_MAX_LEASED_BLOBS)));
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_MAX_BLOB_ACCESS_PERMITTED_PACKAGES, Integer.valueOf(MAX_BLOB_ACCESS_PERMITTED_PACKAGES), Integer.valueOf(DEFAULT_MAX_BLOB_ACCESS_PERMITTED_PACKAGES)));
            fout.println(String.format("%s: [cur: %s, def: %s]", KEY_LEASE_DESC_CHAR_LIMIT, Integer.valueOf(LEASE_DESC_CHAR_LIMIT), Integer.valueOf(DEFAULT_LEASE_DESC_CHAR_LIMIT)));
        }
    }

    public static void initialize(Context context) {
        DeviceConfig.addOnPropertiesChangedListener(ROOT_DIR_NAME, context.getMainExecutor(), $$Lambda$BlobStoreConfig$puwdUOAux6q8DMSxBpGh5jGtgZA.INSTANCE);
        DeviceConfigProperties.refresh(DeviceConfig.getProperties(ROOT_DIR_NAME, new String[0]));
    }

    public static long getIdleJobPeriodMs() {
        return DeviceConfigProperties.IDLE_JOB_PERIOD_MS;
    }

    public static boolean hasSessionExpired(long sessionLastModifiedMs) {
        return sessionLastModifiedMs < System.currentTimeMillis() - DeviceConfigProperties.SESSION_EXPIRY_TIMEOUT_MS;
    }

    public static long getAppDataBytesLimit() {
        long totalBytesLimit = ((float) Environment.getDataSystemDirectory().getTotalSpace()) * DeviceConfigProperties.TOTAL_BYTES_PER_APP_LIMIT_FRACTION;
        return Math.max(DeviceConfigProperties.TOTAL_BYTES_PER_APP_LIMIT_FLOOR, totalBytesLimit);
    }

    public static boolean hasLeaseWaitTimeElapsed(long commitTimeMs) {
        return DeviceConfigProperties.LEASE_ACQUISITION_WAIT_DURATION_MS + commitTimeMs < System.currentTimeMillis();
    }

    public static long getAdjustedCommitTimeMs(long oldCommitTimeMs, long newCommitTimeMs) {
        if (oldCommitTimeMs == 0 || hasCommitCoolOffPeriodElapsed(oldCommitTimeMs)) {
            return newCommitTimeMs;
        }
        return oldCommitTimeMs;
    }

    private static boolean hasCommitCoolOffPeriodElapsed(long commitTimeMs) {
        return DeviceConfigProperties.COMMIT_COOL_OFF_DURATION_MS + commitTimeMs < System.currentTimeMillis();
    }

    public static boolean shouldUseRevocableFdForReads() {
        return DeviceConfigProperties.USE_REVOCABLE_FD_FOR_READS;
    }

    public static long getDeletionOnLastLeaseDelayMs() {
        return DeviceConfigProperties.DELETE_ON_LAST_LEASE_DELAY_MS;
    }

    public static int getMaxActiveSessions() {
        return DeviceConfigProperties.MAX_ACTIVE_SESSIONS;
    }

    public static int getMaxCommittedBlobs() {
        return DeviceConfigProperties.MAX_COMMITTED_BLOBS;
    }

    public static int getMaxLeasedBlobs() {
        return DeviceConfigProperties.MAX_LEASED_BLOBS;
    }

    public static int getMaxPermittedPackages() {
        return DeviceConfigProperties.MAX_BLOB_ACCESS_PERMITTED_PACKAGES;
    }

    public static CharSequence getTruncatedLeaseDescription(CharSequence description) {
        if (TextUtils.isEmpty(description)) {
            return description;
        }
        return TextUtils.trimToLengthWithEllipsis(description, DeviceConfigProperties.LEASE_DESC_CHAR_LIMIT);
    }

    public static File prepareBlobFile(long sessionId) {
        File blobsDir = prepareBlobsDir();
        if (blobsDir == null) {
            return null;
        }
        return getBlobFile(blobsDir, sessionId);
    }

    public static File getBlobFile(long sessionId) {
        return getBlobFile(getBlobsDir(), sessionId);
    }

    private static File getBlobFile(File blobsDir, long sessionId) {
        return new File(blobsDir, String.valueOf(sessionId));
    }

    public static File prepareBlobsDir() {
        File blobsDir = getBlobsDir(prepareBlobStoreRootDir());
        if (!blobsDir.exists() && !blobsDir.mkdir()) {
            Slog.e(TAG, "Failed to mkdir(): " + blobsDir);
            return null;
        }
        return blobsDir;
    }

    public static File getBlobsDir() {
        return getBlobsDir(getBlobStoreRootDir());
    }

    private static File getBlobsDir(File blobsRootDir) {
        return new File(blobsRootDir, BLOBS_DIR_NAME);
    }

    public static File prepareSessionIndexFile() {
        File blobStoreRootDir = prepareBlobStoreRootDir();
        if (blobStoreRootDir == null) {
            return null;
        }
        return new File(blobStoreRootDir, SESSIONS_INDEX_FILE_NAME);
    }

    public static File prepareBlobsIndexFile() {
        File blobsStoreRootDir = prepareBlobStoreRootDir();
        if (blobsStoreRootDir == null) {
            return null;
        }
        return new File(blobsStoreRootDir, BLOBS_INDEX_FILE_NAME);
    }

    public static File prepareBlobStoreRootDir() {
        File blobStoreRootDir = getBlobStoreRootDir();
        if (!blobStoreRootDir.exists() && !blobStoreRootDir.mkdir()) {
            Slog.e(TAG, "Failed to mkdir(): " + blobStoreRootDir);
            return null;
        }
        return blobStoreRootDir;
    }

    public static File getBlobStoreRootDir() {
        return new File(Environment.getDataSystemDirectory(), ROOT_DIR_NAME);
    }

    public static void dump(IndentingPrintWriter fout, Context context) {
        fout.println("XML current version: 5");
        fout.println("Idle job ID: 191934935");
        fout.println("Total bytes per app limit: " + Formatter.formatFileSize(context, getAppDataBytesLimit(), 8));
        fout.println("Device config properties:");
        fout.increaseIndent();
        DeviceConfigProperties.dump(fout, context);
        fout.decreaseIndent();
    }
}