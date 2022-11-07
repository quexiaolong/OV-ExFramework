package com.android.server.job;

import android.app.job.JobInfo;
import java.util.List;

/* loaded from: classes.dex */
public interface IVivoJobSchedulerService {
    void cancelByUid(int i, int i2);

    List<JobInfo> getPendingJobsByUid(int i);
}