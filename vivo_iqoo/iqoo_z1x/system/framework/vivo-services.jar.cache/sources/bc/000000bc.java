package com.android.server.am;

import android.util.SparseArray;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class SystemDefenceHelper {
    private static final String TAG = "SDS";
    private static SystemDefenceHelper _instance;
    private ActivityManagerService mAms;

    public static SystemDefenceHelper getInstance() {
        if (_instance == null) {
            _instance = new SystemDefenceHelper();
        }
        return _instance;
    }

    public void init(ActivityManagerService ams) {
        this.mAms = ams;
    }

    public ActivityManagerService getAms() {
        return this.mAms;
    }

    public boolean checkDelayUpdate(String resumePackage, String delayPackage) {
        synchronized (this.mAms) {
            int NP = this.mAms.mProcessList.mProcessNames.getMap().size();
            for (int ip = 0; ip < NP; ip++) {
                SparseArray<ProcessRecord> apps = (SparseArray) this.mAms.mProcessList.mProcessNames.getMap().valueAt(ip);
                int NA = apps.size();
                for (int ia = 0; ia < NA; ia++) {
                    ProcessRecord app = apps.valueAt(ia);
                    if (app.info != null && app.info.packageName != null && app.info.packageName.equals(resumePackage) && app != null && app.pkgDeps != null && app.pkgDeps.contains(delayPackage)) {
                        VSlog.i(TAG, "resumePackage = " + resumePackage + ",delayPackage=" + delayPackage);
                        return true;
                    }
                }
            }
            return false;
        }
    }
}