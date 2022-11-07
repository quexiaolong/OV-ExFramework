package com.android.server.people.data;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.CancellationSignal;
import com.android.server.LocalServices;
import com.android.server.people.PeopleServiceInternal;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public class DataMaintenanceService extends JobService {
    private static final int BASE_JOB_ID = 204561367;
    private static final long JOB_RUN_INTERVAL = TimeUnit.HOURS.toMillis(24);
    private CancellationSignal mSignal;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void scheduleJob(Context context, int userId) {
        int jobId = getJobId(userId);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        if (jobScheduler.getPendingJob(jobId) == null) {
            ComponentName component = new ComponentName(context, DataMaintenanceService.class);
            JobInfo newJob = new JobInfo.Builder(jobId, component).setRequiresDeviceIdle(true).setPeriodic(JOB_RUN_INTERVAL).build();
            jobScheduler.schedule(newJob);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void cancelJob(Context context, int userId) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        jobScheduler.cancel(getJobId(userId));
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(final JobParameters params) {
        final int userId = getUserId(params.getJobId());
        this.mSignal = new CancellationSignal();
        new Thread(new Runnable() { // from class: com.android.server.people.data.-$$Lambda$DataMaintenanceService$pZUzfdXzCXsv1D-xTvqArhV-TxI
            @Override // java.lang.Runnable
            public final void run() {
                DataMaintenanceService.this.lambda$onStartJob$0$DataMaintenanceService(userId, params);
            }
        }).start();
        return true;
    }

    public /* synthetic */ void lambda$onStartJob$0$DataMaintenanceService(int userId, JobParameters params) {
        PeopleServiceInternal peopleServiceInternal = (PeopleServiceInternal) LocalServices.getService(PeopleServiceInternal.class);
        peopleServiceInternal.pruneDataForUser(userId, this.mSignal);
        jobFinished(params, this.mSignal.isCanceled());
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        CancellationSignal cancellationSignal = this.mSignal;
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
            return false;
        }
        return false;
    }

    private static int getJobId(int userId) {
        return BASE_JOB_ID + userId;
    }

    private static int getUserId(int jobId) {
        return jobId - BASE_JOB_ID;
    }
}