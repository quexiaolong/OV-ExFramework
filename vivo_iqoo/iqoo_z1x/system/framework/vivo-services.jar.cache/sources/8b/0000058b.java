package com.android.server.wm;

import android.app.IApplicationThread;
import android.app.servertransaction.ConfigurationChangeItem;
import android.content.res.Configuration;
import android.os.SystemProperties;
import com.android.server.am.ProcessRecord;
import com.android.server.am.VivoFrozenPackageSupervisor;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.proxy.ProxyUtils;
import com.vivo.services.rms.proxy.VivoBinderProxy;
import java.util.ArrayList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoWindowProcessControllerImpl implements IVivoWindowProcessController {
    private static boolean DEBUG = SystemProperties.getBoolean("persist.vivo.wpc.debug", false);
    static final String TAG = "VivoWindowProcessControllerImpl";
    public boolean hasShownUi;
    private ArrayList<ActivityRecord> mActivities;
    private final ActivityTaskManagerService mAtm;
    private boolean mIsPackageUpdating = false;
    private VivoFrozenPackageSupervisor mVivoFrozenPackageSupervisor;
    private WindowProcessController mWindowProcessController;

    public VivoWindowProcessControllerImpl(WindowProcessController windowProcessController, ActivityTaskManagerService atm, ArrayList<ActivityRecord> activities) {
        this.mActivities = new ArrayList<>();
        if (windowProcessController == null) {
            VSlog.i(TAG, "container is " + windowProcessController);
        }
        this.mWindowProcessController = windowProcessController;
        this.mAtm = atm;
        this.mActivities = activities;
        this.mVivoFrozenPackageSupervisor = VivoFrozenPackageSupervisor.getInstance();
    }

    public boolean hasVisibleActivitiesIgnoringKeyguard() {
        synchronized (this.mAtm.mGlobalLockWithoutBoost) {
            for (int i = this.mActivities.size() - 1; i >= 0; i--) {
                ActivityRecord r = this.mActivities.get(i);
                if (r.visibleIgnoringKeyguard) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isPackageUpdating() {
        return this.mIsPackageUpdating;
    }

    public void setPackageUpdate(boolean isUpdate) {
        this.mIsPackageUpdating = isUpdate;
    }

    public boolean isFrozenProcess() {
        return (this.mWindowProcessController.mInfo == null || this.mWindowProcessController.mInfo.packageName == null || !this.mVivoFrozenPackageSupervisor.isFrozenPackage(this.mWindowProcessController.mInfo.packageName, this.mWindowProcessController.mInfo.uid)) ? false : true;
    }

    private void handleFrozenProcessConfig(int pid, final IApplicationThread thread, Configuration config) {
        final Configuration newConfig = new Configuration(config);
        Runnable callback = new Runnable() { // from class: com.android.server.wm.VivoWindowProcessControllerImpl.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    VivoWindowProcessControllerImpl.this.mAtm.getLifecycleManager().scheduleTransaction(thread, ConfigurationChangeItem.obtain(newConfig));
                    if (ActivityTaskManagerDebugConfig.DEBUG_CONFIGURATION) {
                        VSlog.v("configchange", "Sending to proc " + VivoWindowProcessControllerImpl.this.mWindowProcessController.mName + " new config " + newConfig);
                    }
                } catch (Exception e) {
                    VLog.e(VivoWindowProcessControllerImpl.TAG, "Failed to schedule configuration change", e);
                }
            }
        };
        VivoBinderProxy.getInstance().saveToFrozenList((ProcessRecord) this.mWindowProcessController.mOwner, callback);
    }

    public void setHasShowUi() {
        if (!this.hasShownUi || isFrozenProcess()) {
            VivoBinderProxy.getInstance().reportShowUiSyncHandle(this.mWindowProcessController.getPid());
            this.hasShownUi = true;
        }
    }

    public boolean handleTransaction(IApplicationThread thread, Configuration config, int change) {
        if (ProxyUtils.isFeatureSupport()) {
            ProcessRecord pr = (ProcessRecord) this.mWindowProcessController.mOwner;
            if (change == 536872064) {
                if (this.hasShownUi || VivoBinderProxy.getInstance().hasShownUi(pr)) {
                    if (isFrozenProcess()) {
                        if (ActivityTaskManagerDebugConfig.DEBUG_CONFIGURATION) {
                            VLog.i("configchange", "frozen process: " + pr.processName + "  to save");
                        }
                        handleFrozenProcessConfig(pr.pid, thread, config);
                        return true;
                    }
                    VivoBinderProxy.getInstance().removeFromFrozenList(pr.pid);
                    return false;
                }
                if (ActivityTaskManagerDebugConfig.DEBUG_CONFIGURATION) {
                    VLog.i("configchange", "orientation change, process has no ui: " + pr.processName + "  to save");
                }
                handleFrozenProcessConfig(pr.pid, thread, config);
                return true;
            } else if (isFrozenProcess()) {
                if (ActivityTaskManagerDebugConfig.DEBUG_CONFIGURATION) {
                    VLog.i("configchange", "frozen process: " + pr.processName + "  to save");
                }
                handleFrozenProcessConfig(pr.pid, thread, config);
                return true;
            } else {
                VivoBinderProxy.getInstance().removeFromFrozenList(pr.pid);
                return false;
            }
        }
        return false;
    }
}