package com.android.server;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.notification.SnoozeHelper;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public class PreloadsFileCacheExpirationJobService extends JobService {
    private static final boolean DEBUG = false;
    private static final int JOB_ID = 100500;
    private static final String PERSIST_SYS_PRELOADS_FILE_CACHE_EXPIRED = "persist.sys.preloads.file_cache_expired";
    private static final String TAG = "PreloadsFileCacheExpirationJobService";

    public static void schedule(Context context) {
        int keepPreloadsMinDays = Resources.getSystem().getInteger(17694821);
        long keepPreloadsMinTimeoutMs = TimeUnit.DAYS.toMillis(keepPreloadsMinDays);
        long keepPreloadsMaxTimeoutMs = TimeUnit.DAYS.toMillis(keepPreloadsMinDays + 1);
        JobInfo expirationJob = new JobInfo.Builder(JOB_ID, new ComponentName(context, PreloadsFileCacheExpirationJobService.class)).setPersisted(true).setMinimumLatency(keepPreloadsMinTimeoutMs).setOverrideDeadline(keepPreloadsMaxTimeoutMs).build();
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(expirationJob);
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        SystemProperties.set(PERSIST_SYS_PRELOADS_FILE_CACHE_EXPIRED, SnoozeHelper.XML_SNOOZED_NOTIFICATION_VERSION);
        Slog.i(TAG, "Set persist.sys.preloads.file_cache_expired=1");
        return false;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}