package com.android.server;

import android.app.anr.ANRManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.vivo.services.daemon.VivoDmServiceProxy;
import java.util.ArrayList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoWatchdogImpl implements IVivoWatchdog {
    private static final String TAG = "VivoWatchdogImpl";
    ActivityManagerService mActivity;
    int mCount = 0;

    public VivoWatchdogImpl(ActivityManagerService mAms) {
        this.mActivity = mAms;
    }

    public void checkBlockedAndWarningLocked(ArrayList<Watchdog.HandlerChecker> mHandlerCheckers) {
        ArrayList<Watchdog.HandlerChecker> arrayList = mHandlerCheckers;
        long nowTime = SystemClock.uptimeMillis();
        boolean shouldPrintTrace = false;
        int i = 0;
        while (i < mHandlerCheckers.size()) {
            Watchdog.HandlerChecker checker = arrayList.get(i);
            Thread thread = arrayList.get(i).getThread();
            if (checker.getCompletionStateLocked() != 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(thread.getName() + "(tid=" + thread.getId() + ") blocked for " + (nowTime - checker.mStartTime) + "ms\n");
                sb.append("    now stack trace:\n");
                StackTraceElement[] stackTrace = checker.getThread().getStackTrace();
                for (StackTraceElement element : stackTrace) {
                    sb.append("      at " + element + "\n");
                }
                sb.append("    start stack trace:\n");
                String msg = sb.toString();
                if (nowTime - checker.mStartTime > 5000) {
                    shouldPrintTrace = true;
                }
                VSlog.w(TAG, msg);
                if (this.mActivity.getANRManager() instanceof ANRManager) {
                    ANRManager mANRManager = (ANRManager) this.mActivity.getANRManager();
                    mANRManager.appLooperBlocked(msg, (int) (nowTime - checker.mStartTime), (String) null);
                } else {
                    VSlog.e(TAG, "Get ANRManager error");
                }
            }
            i++;
            arrayList = mHandlerCheckers;
        }
        if (shouldPrintTrace && this.mCount < 3) {
            try {
                VivoDmServiceProxy vivoDmSrvProxy = VivoDmServiceProxy.asInterface(ServiceManager.getService("vivo_daemon.service"));
                if (vivoDmSrvProxy != null) {
                    VSlog.d(TAG, "collect blocked for");
                    vivoDmSrvProxy.collectSystemInfo("2");
                    this.mCount++;
                }
            } catch (Exception e) {
                VSlog.e(TAG, "collect blocked for", e);
            }
        }
    }

    public void completionLocked() {
        this.mCount = 0;
    }
}