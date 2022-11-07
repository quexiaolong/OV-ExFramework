package com.android.server.job;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Binder;
import java.util.List;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoJobSchedulerServiceImpl implements IVivoJobSchedulerService {
    static final String TAG = "VivoJobSchedulerServiceImpl";
    private final Context mContext;
    private JobScheduler mJobScheduler;

    public VivoJobSchedulerServiceImpl(Context context) {
        this.mContext = context;
        if (context == null) {
            VSlog.i(TAG, "container is " + context);
            return;
        }
        this.mJobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
    }

    private void ensureJobSchedulerNonNull() {
        Context context;
        if (this.mJobScheduler == null && (context = this.mContext) != null) {
            this.mJobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        }
    }

    public List<JobInfo> getPendingJobsByUid(int uid) {
        int callingUid = Binder.getCallingUid();
        ensureJobSchedulerNonNull();
        if (callingUid != 1000 || this.mJobScheduler == null) {
            VSlog.v(TAG, "getPendingJobsByUid falied for Frozen calling uid: " + callingUid + ",mJobScheduler: " + this.mJobScheduler);
            return null;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            VSlog.v(TAG, "getPendingJobsFrozen or Frozen calling uid: " + callingUid);
            return this.mJobScheduler.getPendingJobsFrozen(uid);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void cancelByUid(int uid, int jobId) {
        int callingUid = Binder.getCallingUid();
        ensureJobSchedulerNonNull();
        if (callingUid != 1000 || this.mJobScheduler == null) {
            VSlog.v(TAG, "cancelByUid falied or Frozen calling uid: " + callingUid);
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            VSlog.v(TAG, "cancelJobFrozen  or Frozen calling uid: " + callingUid);
            this.mJobScheduler.cancelJobFrozen(uid, jobId, callingUid);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }
}