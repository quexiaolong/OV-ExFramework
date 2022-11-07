package com.android.server.pm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageParser;
import android.os.FtBuild;
import android.os.Handler;
import android.text.TextUtils;
import com.android.server.pm.VivoPKMSReportMgr;
import com.vivo.face.common.data.Constants;
import com.vivo.framework.systemdefence.SystemDefenceManager;
import java.io.File;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoPackageInstallerSessionImpl implements IVivoPackageInstallerSession {
    static final String TAG = "VivoPackageInstallerSessionImpl";
    private Context mContext;
    private Handler mHandler;
    private String mPackageName;
    private AbsVivoPerfManager mPerf;
    private PackageInstallerSession mPis;
    private PackageManagerService mPms;
    private VivoPKMSCommonUtils mVivoPKMSCommonUtils;
    private Boolean LOGD = false;
    private long mVersionCode = -1;
    private String mVersionName = null;
    private boolean mIsPerfLockAcquired = false;
    private final int MAX_INSTALL_DURATION = 20000;

    public VivoPackageInstallerSessionImpl(PackageManagerService pms, PackageInstallerSession pis, Context context, Handler handler) {
        this.mPerf = null;
        if (pms == null) {
            VSlog.i(TAG, "container is " + pms);
        }
        this.mPms = pms;
        this.mPis = pis;
        this.mContext = context;
        this.mHandler = handler;
        if (this.mPerf == null) {
            this.mPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        }
    }

    public void reportInstallFailedException(int returnCode, String msg, String installerPackageName, String packageName, long packageVersion, int installerUid, PackageInstaller.SessionParams params, String versionName) {
        if (FtBuild.getTierLevel() != 0 || returnCode >= 0) {
            return;
        }
        if (returnCode == -115) {
            VSlog.w(TAG, "skip " + returnCode + " install_failed.");
            return;
        }
        try {
            reportInstallFailedExceptionInner(returnCode, msg, installerPackageName, packageName, packageVersion, installerUid, params, versionName);
        } catch (Exception e) {
            VSlog.w(TAG, "reportInstallFailedExp, " + e.toString());
        }
    }

    private void reportInstallFailedExceptionInner(int returnCode, String msg, String installerPackageName, String packageName, long packageVersion, int installerUid, PackageInstaller.SessionParams params, String versionName) {
        String pNVN;
        String originInstallCallerPkgName = VivoPKMSCommonUtils.getOriginalInstallCallerPkgName(this.LOGD.booleanValue(), installerPackageName, installerUid, params.originatingUid, params.installFlags, this.mContext);
        String originInstallCallerPkgLabelName = VivoPKMSCommonUtils.getPackageLabelName(this.LOGD.booleanValue(), originInstallCallerPkgName, this.mContext);
        String originInstallVersion = VivoPKMSCommonUtils.getPackageVersionName(originInstallCallerPkgName, this.mContext);
        String pN = "unknow";
        if (!TextUtils.isEmpty(packageName)) {
            pN = packageName;
        } else if (!TextUtils.isEmpty(params.appPackageName)) {
            pN = params.appPackageName;
        } else {
            VSlog.w(TAG, "can not get app pkg name.");
        }
        if (packageVersion == -1) {
            pNVN = VivoPKMSCommonUtils.getPackageVersionName(pN, this.mContext);
        } else {
            pNVN = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + packageVersion;
        }
        if ("unknow".equals(pNVN)) {
            pNVN = this.mVersionCode + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        int state = VivoPKMSCommonUtils.getPackageInstallState(pN, this.mContext);
        if ("unknow".equals(pN) && "unknow".equals(originInstallCallerPkgName) && TextUtils.isEmpty(msg)) {
            VSlog.w(TAG, "unknow who install who, skip this record.");
            return;
        }
        VivoPKMSReportMgr.InstallReportInfo rf = new VivoPKMSReportMgr.InstallReportInfo(originInstallCallerPkgName, originInstallCallerPkgLabelName, originInstallVersion, pN, pNVN, state, 1, returnCode, msg);
        rf.versionName = versionName != null ? versionName : this.mVersionName;
        VivoPKMSReportMgr.getInstance().scheduleReportInstallFailedException(rf);
    }

    public void parserApkNotCheckSignature(File file) {
        PackageParser.ApkLite apkLite = VivoPKMSCommonUtils.parserApkNotCheckSignature(file);
        if (apkLite != null) {
            this.mVersionCode = apkLite.versionCode;
            this.mVersionName = apkLite.versionName;
        }
    }

    public void dummy() {
    }

    public void openWriteBoost() {
        AbsVivoPerfManager absVivoPerfManager = this.mPerf;
        if (absVivoPerfManager != null && !this.mIsPerfLockAcquired) {
            absVivoPerfManager.perfHint(4232, (String) null, 20000, -1);
            this.mIsPerfLockAcquired = true;
        }
    }

    public void openWriteBoostRelease() {
        AbsVivoPerfManager absVivoPerfManager;
        if (this.mIsPerfLockAcquired && (absVivoPerfManager = this.mPerf) != null) {
            absVivoPerfManager.perfLockRelease();
            this.mIsPerfLockAcquired = false;
        }
    }

    public boolean checkDelayUpdate(String packageName) {
        this.mPackageName = packageName;
        registReceiverIfNeed(packageName);
        return SystemDefenceManager.getInstance().checkDelayUpdate(packageName);
    }

    private void registReceiverIfNeed(String packageName) {
        if (SystemDefenceManager.getInstance().checkReinstallPacakge(packageName)) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SCREEN_OFF");
            BroadcastReceiver receiver = new BroadcastReceiver() { // from class: com.android.server.pm.VivoPackageInstallerSessionImpl.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    if (VivoPackageInstallerSessionImpl.this.mPackageName != null && SystemDefenceManager.getInstance().checkReinstallPacakge(VivoPackageInstallerSessionImpl.this.mPackageName) && "android.intent.action.SCREEN_OFF".equals(intent.getAction()) && !SystemDefenceManager.getInstance().checkDelayUpdate(VivoPackageInstallerSessionImpl.this.mPackageName)) {
                        VSlog.d("SDS", "continue to update =" + VivoPackageInstallerSessionImpl.this.mPackageName);
                        VivoPackageInstallerSessionImpl.this.mPackageName = null;
                        VivoPackageInstallerSessionImpl.this.mHandler.obtainMessage(1).sendToTarget();
                        VivoPackageInstallerSessionImpl.this.mContext.unregisterReceiver(this);
                    }
                }
            };
            this.mContext.registerReceiver(receiver, filter);
        }
    }
}