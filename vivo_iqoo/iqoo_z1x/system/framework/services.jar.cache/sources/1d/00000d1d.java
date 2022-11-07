package com.android.server.display;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.display.DisplayManagerInternal;
import com.android.server.LocalServices;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public class BrightnessIdleJob extends JobService {
    private static final int JOB_ID = 3923512;

    public static void scheduleJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        JobInfo pending = jobScheduler.getPendingJob(JOB_ID);
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, BrightnessIdleJob.class)).setRequiresDeviceIdle(true).setRequiresCharging(true).setPeriodic(TimeUnit.HOURS.toMillis(24L)).build();
        if (pending != null && !pending.equals(jobInfo)) {
            jobScheduler.cancel(JOB_ID);
            pending = null;
        }
        if (pending == null) {
            jobScheduler.schedule(jobInfo);
        }
    }

    public static void cancelJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        jobScheduler.cancel(JOB_ID);
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        DisplayManagerInternal dmi = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        dmi.persistBrightnessTrackerState();
        return false;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}