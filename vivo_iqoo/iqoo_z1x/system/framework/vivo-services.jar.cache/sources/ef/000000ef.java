package com.android.server.am;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import com.vivo.framework.systemdefence.SystemDefenceManager;
import com.vivo.services.perf.bigdata.PerfBigdata;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.proxy.VivoBinderProxy;
import com.vivo.services.rms.sp.SpManagerImpl;
import com.vivo.services.security.client.VivoPermissionManager;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoProcessRecordImpl implements IVivoProcessRecord {
    boolean mColdStartAlreadyNotify = false;
    ProcessRecord mProcessRecord;
    RMProcInfo rmProc;

    public VivoProcessRecordImpl(ProcessRecord _processRecord, ApplicationInfo _info, String _processName, int _uid) {
        this.mProcessRecord = _processRecord;
        this.rmProc = RmsInjectorImpl.getInstance().newProcInfo(_processRecord, _uid, _info.packageName, _info.flags, _processName);
    }

    public HostingRecord getHostingRecord() {
        ProcessRecord processRecord = this.mProcessRecord;
        if (processRecord != null) {
            return processRecord.hostingRecord;
        }
        return null;
    }

    public boolean getColdStartAlreadyNotify() {
        return this.mColdStartAlreadyNotify;
    }

    public void setColdStartAlreadyNotify(boolean notified) {
        this.mColdStartAlreadyNotify = notified;
    }

    public void addProcess() {
        if (this.mProcessRecord.thread != null) {
            RmsInjectorImpl.getInstance().addProcess(this.rmProc);
            SpManagerImpl.getInstance().identifySuperProcessIfNeed(this.mProcessRecord, this.rmProc);
        }
    }

    public void removeProcess() {
        if (this.mProcessRecord.thread != null) {
            RmsInjectorImpl.getInstance().removeProcess(this.rmProc);
            SpManagerImpl.getInstance().clearSuperProcessIfNeed(this.mProcessRecord, this.rmProc);
        }
    }

    public void addDepPkg(String pkg) {
        RmsInjectorImpl.getInstance().addDepPkg(this.rmProc, pkg);
    }

    public void addPkg(String pkg) {
        RmsInjectorImpl.getInstance().addPkg(this.rmProc, pkg);
    }

    public void setDeathResson(String reason) {
        this.rmProc.mKillReason = reason;
    }

    public void setCreateReason(String reason) {
        this.rmProc.mCreateReason = reason;
    }

    public void setPid(int pid) {
        this.rmProc.mPid = pid;
    }

    public void setNeedKeepQuiet(boolean quiet) {
        this.rmProc.needKeepQuiet = quiet;
    }

    public void setRmsPreloaded(boolean preloaded) {
        this.rmProc.rmsPreloaded = preloaded;
    }

    public Object getRMProcInfo() {
        return this.rmProc;
    }

    public void onStartActivity() {
        if (!this.mProcessRecord.hasShownUi && this.mProcessRecord.pid > 0) {
            VivoBinderProxy.getInstance().reportHasShownUi(this.mProcessRecord.pid);
        }
    }

    public void onProcAnr(String packageName, String processName, String activity, String reason) {
        PerfBigdata.onProcAnr(packageName, processName, activity, reason);
    }

    public void processRecordkill(String processName, int pid, boolean mNotResponding, boolean mCrashing) {
        AbsVivoPerfManager ux_perf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        if (ux_perf != null && !VivoAmsImpl.mForceStopKill && !mNotResponding && !mCrashing) {
            ux_perf.perfUXEngine_events(4, 0, processName, 0);
        } else {
            VivoAmsImpl.mForceStopKill = false;
        }
        if (ux_perf != null) {
            ux_perf.perfHintAsync(4243, processName, pid, 0);
        }
    }

    public boolean checkSkipKilledByRemoveTask(String reason, String processName) {
        return SystemDefenceManager.getInstance().checkSkipKilledByRemoveTask(reason, processName);
    }

    public void checkUploadStabilityData(String processName, String reason) {
        SystemDefenceManager.getInstance().checkUploadStabilityData(processName, reason);
    }

    public boolean isCheckingPermission(int pid, String processName) {
        if (VivoPermissionManager.isCheckingPermission(pid)) {
            VSlog.d("VPS", "ANR is skiped! processName=" + processName + ";pid=" + pid);
            return true;
        }
        return false;
    }
}