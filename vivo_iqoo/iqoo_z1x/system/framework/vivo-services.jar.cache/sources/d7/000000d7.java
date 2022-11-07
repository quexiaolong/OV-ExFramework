package com.android.server.am;

import android.app.AppGlobals;
import android.app.PendingIntent;
import android.os.Binder;
import android.os.Message;
import com.android.server.wm.ActivityTaskManagerDebugConfig;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoAmsUtils {
    private static final String TAG = "VivoAmsUtils";

    public static boolean isPendingIntentCanceled(PendingIntent pendingIntent) {
        PendingIntentRecord target;
        if (pendingIntent == null || (target = pendingIntent.getTarget()) == null || !(target instanceof PendingIntentRecord)) {
            return false;
        }
        boolean canceled = target.canceled;
        return canceled;
    }

    public static void setActivityControllerTimeout() {
        VSlog.i(TAG, "send message.");
        ProcessList.sKillHandler.sendEmptyMessageDelayed(200, (long) VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
    }

    public static void cancelActivityControllerTimeout() {
        VSlog.i(TAG, "remove message.");
        ProcessList.sKillHandler.removeMessages(200);
    }

    public static void setVivoActivityControllerTimeout() {
        VSlog.i(TAG, "send vivo message.");
        ProcessList.sKillHandler.sendEmptyMessageDelayed(201, (long) VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
    }

    public static void cancelVivoActivityControllerTimeout() {
        VSlog.i(TAG, "remove vivo message.");
        ProcessList.sKillHandler.removeMessages(201);
    }

    public static void setAmsDumpTimeout() {
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        String packageName = getNameFromUid(callingUid);
        VSlog.i(TAG, "begin ams dump.");
        if (ActivityTaskManagerDebugConfig.DEBUG_STATES) {
            VSlog.d(TAG, "AMS dump callingPid=" + callingPid + " ,callingUid=" + callingUid + " ,callerPkgName=" + packageName);
        }
        Message msg = ProcessList.sKillHandler.obtainMessage(202);
        msg.arg1 = callingPid;
        msg.arg2 = callingUid;
        msg.obj = Thread.currentThread().getName();
        ProcessList.sKillHandler.sendMessageDelayed(msg, (long) VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
    }

    public static void cancelAmsDumpTimeout() {
        VSlog.i(TAG, "end ams dump.");
        ProcessList.sKillHandler.removeMessages(202, Thread.currentThread().getName());
    }

    public static String getNameFromUid(int callingUid) {
        try {
            String callerPkgName = AppGlobals.getPackageManager().getNameForUid(callingUid);
            return callerPkgName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}