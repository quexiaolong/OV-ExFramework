package com.android.server.wm;

import android.app.IActivityController;
import android.os.Binder;
import android.os.IBinder;
import com.android.server.am.firewall.VivoAppIsolationController;
import com.android.server.uri.NeededUriGrants;
import com.vivo.face.common.data.Constants;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoSoftwareLock {
    private static final String TAG = "VivoSoftwareLock";
    public static boolean isSecureController = false;
    public static boolean isInDaemon = false;
    private static int sControllerPid = 0;
    private static int sVivoControllerPid = 0;
    private static int sControllerUid = 0;
    private static int sVivoControllerUid = 0;
    IBinder mInterceptedResultTo = null;
    String mInterceptedResultWho = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    int mInterceptedRequestCode = 0;
    ActivityRecord mInterceptedSourceRecord = null;
    int mInterceptedCallingUid = -1;
    NeededUriGrants mNeededUriGrants = null;
    String mInterceptedCallingPackage = null;

    public ActivityRecord getInterceptedSourceRecord() {
        return this.mInterceptedSourceRecord;
    }

    public IBinder getInterceptedResultTo() {
        return this.mInterceptedResultTo;
    }

    public int getInterceptedRequestCode() {
        return this.mInterceptedRequestCode;
    }

    public int getInterceptedCallingUid() {
        return this.mInterceptedCallingUid;
    }

    public NeededUriGrants getInterceptedNeededUriGrants() {
        return this.mNeededUriGrants;
    }

    public void setInterceptedCallingUid(int uid) {
        this.mInterceptedCallingUid = uid;
    }

    public String getInterceptedCallingPackage() {
        return this.mInterceptedCallingPackage;
    }

    public void setInterceptedCallingPackage(String callingPackage) {
        this.mInterceptedCallingPackage = callingPackage;
    }

    public void setInterceptedNeededUriGrants(NeededUriGrants neededUriGrants) {
        this.mNeededUriGrants = neededUriGrants;
    }

    public void setInterceptedResultValue(ActivityRecord resultRecord, IBinder resultTo, int requestcode) {
        this.mInterceptedSourceRecord = resultRecord;
        this.mInterceptedResultTo = resultTo;
        this.mInterceptedRequestCode = requestcode;
    }

    public static void restoreControllerValue() {
        sControllerPid = 0;
        sControllerUid = 0;
    }

    public static int getControllerPid() {
        return sControllerPid;
    }

    public static int getControllerUid() {
        return sControllerUid;
    }

    public static void restoreVivoControllerValue() {
        sVivoControllerPid = 0;
        sVivoControllerUid = 0;
    }

    public static int getVivoControllerPid() {
        return sVivoControllerPid;
    }

    public static int getVivoControllerUid() {
        return sVivoControllerUid;
    }

    public static void setVivoControllerPid(int pid) {
        sVivoControllerPid = pid;
    }

    public static void setVivoControllerUid(int uid) {
        sVivoControllerUid = uid;
    }

    public static void setSecureController(IActivityController controller, boolean imAMonkey, WindowProcessControllerMap mProcessMap) {
        sControllerPid = controller != null ? Binder.getCallingPid() : 0;
        sControllerUid = controller != null ? Binder.getCallingUid() : 0;
        VSlog.i(TAG, "Controller Pid : " + sControllerPid + " amonkey:" + imAMonkey);
        WindowProcessController processController = mProcessMap.getProcess(sControllerPid);
        isInDaemon = false;
        isSecureController = false;
        if (processController != null && (processController.mName.startsWith(VivoAppIsolationController.NOTIFY_IQOO_SECURE_PACKAGE) || processController.mName.startsWith("com.vivo.daemonService"))) {
            isSecureController = true;
            if (processController.mName.startsWith("com.vivo.daemonService")) {
                isInDaemon = true;
            }
        } else {
            isSecureController = false;
        }
        VSlog.i(TAG, "isSecureController : " + isSecureController + "isInDaemon:" + isInDaemon);
    }
}