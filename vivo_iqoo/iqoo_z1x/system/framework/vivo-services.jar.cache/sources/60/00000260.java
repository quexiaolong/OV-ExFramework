package com.android.server.inputmethod;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.view.inputmethod.InputMethodInfo;
import com.android.server.wm.VCD_FF_1;
import com.vivo.face.common.data.Constants;
import com.vivo.vcodetransbase.EventTransfer;
import java.util.HashMap;
import java.util.UUID;

/* loaded from: classes.dex */
public class DefaultImeJobService extends JobService {
    public static final long ONE_DAY_MILLIS = 86400000;
    public static final int REPORT_IME_DATA_JOB_ID = 777777;
    private static InputMethodManagerService mImms;

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        collectDefaultImeData();
        return false;
    }

    private void collectDefaultImeData() {
        InputMethodInfo defaultImeInfo = null;
        synchronized (InputMethodManagerService.mLock) {
            String defaultImiId = mImms.mSettings.getSelectedInputMethod();
            int N = mImms.mMethodList.size();
            if (defaultImiId != null) {
                for (int i = 0; i < N; i++) {
                    InputMethodInfo imi = (InputMethodInfo) mImms.mMethodList.get(i);
                    String imiId = imi.getId();
                    if (imiId.equals(defaultImiId)) {
                        defaultImeInfo = imi;
                    }
                }
            }
        }
        if (defaultImeInfo != null) {
            String packageName = defaultImeInfo.getPackageName();
            PackageInfo packageInfo = mImms.mPackageManagerInternal.getPackageInfo(packageName, 0, 1000, mImms.mSettings.getCurrentUserId());
            String versionName = packageInfo == null ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : packageInfo.versionName;
            HashMap<String, String> params = new HashMap<>();
            params.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
            params.put("def_input", packageName);
            params.put("def_input_version", versionName + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            EventTransfer.getInstance().singleEvent("F296", "F296|10003", System.currentTimeMillis(), 0L, params);
        }
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public static void scheduleJob(InputMethodManagerService imms, Context context) {
        mImms = imms;
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        if (jobScheduler == null) {
            return;
        }
        JobInfo pendingJob = jobScheduler.getPendingJob(REPORT_IME_DATA_JOB_ID);
        if (pendingJob != null) {
            return;
        }
        ComponentName componentName = new ComponentName(context, DefaultImeJobService.class);
        jobScheduler.schedule(new JobInfo.Builder(REPORT_IME_DATA_JOB_ID, componentName).setPeriodic(86400000L).setRequiredNetworkType(2).build());
    }
}