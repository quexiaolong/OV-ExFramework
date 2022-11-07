package com.android.server.am;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import com.android.server.am.PendingIntentRecord;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoPendingIntentControllerImpl implements IVivoPendingIntentController {
    private static final String TAG = "PendingIntentControllerImpl";
    final ArrayMap<String, Integer> byPackageMap = new ArrayMap<>();
    private KillGmsRunnable mRunnable = new KillGmsRunnable();

    public void collectRecordByPackage(PendingIntentRecord.Key key) {
        if (key != null) {
            Integer count = this.byPackageMap.get(key.packageName);
            if (count == null) {
                count = 0;
                this.byPackageMap.put(key.packageName, count);
            }
            Integer count2 = Integer.valueOf(count.intValue() + 1);
            this.byPackageMap.replace(key.packageName, count2);
            if (count2.intValue() > 0 && count2.intValue() % 100 == 0) {
                VSlog.w(TAG, "Collect PendingIntent  " + key + " count = " + count2);
            }
        }
    }

    public void eliminateRecordByPackage(PendingIntentRecord.Key key) {
        Integer count;
        if (key != null && (count = this.byPackageMap.get(key.packageName)) != null) {
            this.byPackageMap.replace(key.packageName, Integer.valueOf(count.intValue() - 1));
        }
    }

    /* loaded from: classes.dex */
    final class KillGmsRunnable implements Runnable {
        String packageName;
        int uid;

        KillGmsRunnable() {
        }

        public void setInfo(String pkgName, int id) {
            this.packageName = pkgName;
            this.uid = id;
        }

        @Override // java.lang.Runnable
        public void run() {
            long btoken = Binder.clearCallingIdentity();
            try {
                IActivityManager am = ActivityManager.getService();
                if (am != null) {
                    try {
                        VSlog.w(VivoPendingIntentControllerImpl.TAG, "force-stop " + this.packageName + " for pendingintent bomb!!!");
                        am.forceStopPackage(this.packageName, UserHandle.getUserId(this.uid));
                    } catch (RemoteException e) {
                        VSlog.w(VivoPendingIntentControllerImpl.TAG, "killApplication " + this.packageName + " failed!", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(btoken);
            }
        }
    }

    public void forceStopGms(String packageName, int uid, Handler handler) {
        if (handler.hasCallbacks(this.mRunnable)) {
            handler.removeCallbacks(this.mRunnable);
        }
        this.mRunnable.setInfo(packageName, uid);
        handler.post(this.mRunnable);
    }
}