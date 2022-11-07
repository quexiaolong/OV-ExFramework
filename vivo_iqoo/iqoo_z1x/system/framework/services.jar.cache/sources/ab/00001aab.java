package com.android.server.usage;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import com.android.server.LocalServices;
import java.util.concurrent.TimeUnit;

/* loaded from: classes2.dex */
public class UsageStatsIdleService extends JobService {
    private static final int PRUNE_JOB_ID = 546357475;
    private static final int UPDATE_MAPPINGS_JOB_ID = 546378950;
    private static final String USER_ID_KEY = "user_id";

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void scheduleJob(Context context, int userId) {
        int userJobId = PRUNE_JOB_ID + userId;
        ComponentName component = new ComponentName(context.getPackageName(), UsageStatsIdleService.class.getName());
        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt(USER_ID_KEY, userId);
        JobInfo pruneJob = new JobInfo.Builder(userJobId, component).setRequiresDeviceIdle(true).setExtras(bundle).setPersisted(true).build();
        scheduleJobInternal(context, pruneJob, userJobId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void scheduleUpdateMappingsJob(Context context) {
        ComponentName component = new ComponentName(context.getPackageName(), UsageStatsIdleService.class.getName());
        JobInfo updateMappingsJob = new JobInfo.Builder(UPDATE_MAPPINGS_JOB_ID, component).setPersisted(true).setMinimumLatency(TimeUnit.DAYS.toMillis(1L)).setOverrideDeadline(TimeUnit.DAYS.toMillis(2L)).build();
        scheduleJobInternal(context, updateMappingsJob, UPDATE_MAPPINGS_JOB_ID);
    }

    private static void scheduleJobInternal(Context context, JobInfo pruneJob, int jobId) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        JobInfo pendingPruneJob = jobScheduler.getPendingJob(jobId);
        if (!pruneJob.equals(pendingPruneJob)) {
            jobScheduler.cancel(jobId);
            jobScheduler.schedule(pruneJob);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void cancelJob(Context context, int userId) {
        cancelJobInternal(context, PRUNE_JOB_ID + userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void cancelUpdateMappingsJob(Context context) {
        cancelJobInternal(context, UPDATE_MAPPINGS_JOB_ID);
    }

    private static void cancelJobInternal(Context context, int jobId) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        if (jobScheduler != null) {
            jobScheduler.cancel(jobId);
        }
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(final JobParameters params) {
        PersistableBundle bundle = params.getExtras();
        final int userId = bundle.getInt(USER_ID_KEY, -1);
        if (userId == -1 && params.getJobId() != UPDATE_MAPPINGS_JOB_ID) {
            return false;
        }
        AsyncTask.execute(new Runnable() { // from class: com.android.server.usage.-$$Lambda$UsageStatsIdleService$yCu7ZLOsLXVx3Ko_5w14oCiO5p4
            @Override // java.lang.Runnable
            public final void run() {
                UsageStatsIdleService.this.lambda$onStartJob$0$UsageStatsIdleService(params, userId);
            }
        });
        return true;
    }

    public /* synthetic */ void lambda$onStartJob$0$UsageStatsIdleService(JobParameters params, int userId) {
        UsageStatsManagerInternal usageStatsManagerInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        if (params.getJobId() == UPDATE_MAPPINGS_JOB_ID) {
            boolean jobFinished = usageStatsManagerInternal.updatePackageMappingsData();
            jobFinished(params, !jobFinished);
            return;
        }
        boolean jobFinished2 = usageStatsManagerInternal.pruneUninstalledPackagesData(userId);
        jobFinished(params, !jobFinished2);
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}