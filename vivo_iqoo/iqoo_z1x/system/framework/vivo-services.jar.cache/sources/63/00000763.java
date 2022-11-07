package com.vivo.services.rms.sp;

import android.content.Context;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.server.IVivoSpManager;
import com.android.server.am.EmergencyBroadcastManager;
import com.android.server.am.ProcessRecord;
import com.android.server.am.RMProcInfo;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.rms.RMAms;
import com.vivo.services.rms.sp.BadPackageManager;
import com.vivo.services.rms.sp.config.ConfigManager;
import com.vivo.services.rms.sp.config.Helpers;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class SpManagerImpl implements IVivoSpManager {
    public static final String SYSTEM_PROCESS_PERSISTENT_SERVICE = "com.vivo.sps.FirstLaunchService";
    public static final String SYSTEM_PROCESS_PKG_NAME = "com.vivo.sps";
    public static final String SYSTEM_PROCESS_PROCESS_NAME = "com.vivo.sps";
    private static final int SYSTEM_PROCESS_UID = 1000;
    public static final String TAG = "SpManager";
    private volatile boolean isSpsExist;
    private volatile boolean isSpsPublished;
    private final HashSet<String> mAllowPackages;
    private final BadPackageManager mBadPkgManager;
    private final ComponentLifeCycleMgr mComponentManager;
    private final ConfigManager mConfigManager;
    private Context mContext;
    private volatile int mLastReportedErrorPid;
    private ProcessRecord mSystemProcess;
    public static final int SUPER_PROCESS_FRAMEWORK_VERSION = SystemProperties.getInt("persist.rms.fwk_version", 10);
    private static final Object mProcessLock = new Object();
    private static volatile boolean sSpsEnable = SystemProperties.getBoolean("persist.debug.sps.enable", true);
    public static String DEVICE_NAME = Helpers.determineModel();

    public void initialize(Context context) {
        this.mContext = context;
        SpServer.getInstance().initialize(context);
        SpClientNotifier.getInstance().initialize(context);
        this.mConfigManager.initialize(context);
        sSpsEnable = sSpsEnable && this.mConfigManager.getIsEnabled();
        SystemProperties.set("sys.sps.enable", sSpsEnable ? "true" : "false");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void disableSps(String reason) {
        if (sSpsEnable) {
            sSpsEnable = false;
            VSlog.i("SpManager", "SuperProcess turns disabled for " + reason);
            SystemProperties.set("sys.sps.enable", "false");
        }
    }

    public void publish() {
        Context context = this.mContext;
        if (context == null) {
            VSlog.e("SpManager", "Publish Sps but context is null");
            disableSps("null context");
            return;
        }
        this.isSpsExist = Helpers.getVersion("com.vivo.sps", context) > 0;
        if (!this.isSpsExist) {
            VSlog.e("SpManager", "CAN NOT FIND SUPER PROCESS APPLICATION.");
            disableSps("Sps app is not exist.");
        }
        if (sSpsEnable) {
            long start = SystemClock.uptimeMillis();
            this.mBadPkgManager.initialize(this.mContext);
            this.mBadPkgManager.checkPackageChangeSync();
            loadAllPackages();
            if (this.mBadPkgManager.isBadPackage("com.vivo.sps")) {
                disableSps("Sps app is bad package.");
            }
            if (sSpsEnable) {
                setBadPackageChangeListener();
                setConfigManagerListener();
            }
            long end = SystemClock.uptimeMillis();
            VSlog.i("SpManager", "Start sps took " + (end - start) + " ms");
        }
        this.isSpsPublished = true;
    }

    public boolean startSps() {
        if (this.isSpsExist) {
            SpClientNotifier.getInstance().startSps();
        }
        return this.isSpsExist;
    }

    public boolean isSpsExist() {
        return this.isSpsExist;
    }

    public void systemReady() {
    }

    private void setBadPackageChangeListener() {
        this.mBadPkgManager.setPackageStateChange(new BadPackageManager.PackageStateChange() { // from class: com.vivo.services.rms.sp.SpManagerImpl.1
            @Override // com.vivo.services.rms.sp.BadPackageManager.PackageStateChange
            public void onPackageTurnBad(String pkg) {
                if ("com.vivo.sps".equals(pkg)) {
                    SpManagerImpl.this.disableSps("Sps app turns bad.");
                }
            }
        });
    }

    private void loadAllPackages() {
        HashSet<String> allPkgs = this.mConfigManager.copyPackageList();
        synchronized (this.mAllowPackages) {
            new HashSet();
            Iterator<String> it = allPkgs.iterator();
            while (it.hasNext()) {
                String pkg = it.next();
                if (this.mConfigManager.isPackageEnabled(pkg, Helpers.getVersion(pkg, this.mContext))) {
                    this.mAllowPackages.add(pkg);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public HashSet<String> updateAllowPackages() {
        HashSet<String> becomeNotAllowed;
        HashSet<String> allPkgs = this.mConfigManager.copyPackageList();
        synchronized (this.mAllowPackages) {
            HashSet<String> newAllowed = new HashSet<>();
            Iterator<String> it = allPkgs.iterator();
            while (it.hasNext()) {
                String pkg = it.next();
                if (this.mConfigManager.isPackageEnabled(pkg, Helpers.getVersion(pkg, this.mContext))) {
                    newAllowed.add(pkg);
                }
            }
            becomeNotAllowed = new HashSet<>();
            Iterator<String> it2 = this.mAllowPackages.iterator();
            while (it2.hasNext()) {
                String pkg2 = it2.next();
                if (!newAllowed.contains(pkg2)) {
                    becomeNotAllowed.add(pkg2);
                    it2.remove();
                }
            }
        }
        return becomeNotAllowed;
    }

    private void setConfigManagerListener() {
        this.mConfigManager.setConfigChangedCallback(new ConfigManager.ConfigChangedCallback() { // from class: com.vivo.services.rms.sp.SpManagerImpl.2
            @Override // com.vivo.services.rms.sp.config.ConfigManager.ConfigChangedCallback
            public void callback(int reason) {
                if (reason == 2) {
                    boolean oldState = SpManagerImpl.sSpsEnable;
                    boolean newState = SpManagerImpl.this.mConfigManager.getIsEnabled();
                    if (oldState && !newState) {
                        SpManagerImpl.this.disableSps("Config change");
                        SpClientNotifier.getInstance().notifyErrorPackage("com.vivo.sps", 1000, -1L, EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP);
                    }
                    HashSet<String> becomeNotAllowedPkgs = SpManagerImpl.this.updateAllowPackages();
                    Iterator<String> it = becomeNotAllowedPkgs.iterator();
                    while (it.hasNext()) {
                        String pkg = it.next();
                        SpClientNotifier.getInstance().notifyErrorPackage(pkg, 1000, -1L, EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP);
                    }
                }
            }
        });
    }

    public boolean isSuperSystemProcessEnable() {
        return sSpsEnable;
    }

    public boolean isSuperAppProcessEnable() {
        return false;
    }

    public boolean canStartOnSuperProcess(String pkgName, int uid) {
        if (uid == 1000 && !this.isSpsPublished) {
            VSlog.e("SpManager", "Check canStartOnSuperProcess for " + pkgName + " before sps published.", new RuntimeException("NEED CHECK"));
        }
        return isSuperSystemProcessEnable() && uid == 1000 && inAllowList(pkgName);
    }

    private boolean inAllowList(String pkgName) {
        boolean z;
        synchronized (this.mAllowPackages) {
            z = this.mAllowPackages.contains(pkgName) && !this.mBadPkgManager.isBadPackage(pkgName);
        }
        return z;
    }

    public int getSuperSystemProcessPid() {
        ProcessRecord processRecord = this.mSystemProcess;
        if (processRecord != null) {
            return processRecord.pid;
        }
        return -1;
    }

    public int getSuperSystemProcessUid() {
        return 1000;
    }

    public int getSuperAppProcessPid() {
        return 0;
    }

    public int getSuperAppProcessUid() {
        return 0;
    }

    public ProcessRecord getSuperSystemProcessRecordLocked() {
        ProcessRecord processRecord;
        synchronized (mProcessLock) {
            processRecord = this.mSystemProcess;
        }
        return processRecord;
    }

    public ProcessRecord getSuperAppProcessRecordLocked() {
        return null;
    }

    public void setSuperSystemProcessRecordLocked(ProcessRecord process) {
        synchronized (mProcessLock) {
            VLog.i("SpManager", "set super system process:" + process);
            this.mSystemProcess = process;
        }
    }

    public void setSuperAppProcessRecordLocked(ProcessRecord process) {
    }

    public void notifyPackageChanged(int type, String pkgName, long versionCode, int uid) {
        VSlog.d("SpManager", "package changed: " + type + " pkg:" + pkgName + " versionCode:" + versionCode + " uid:" + uid);
        if (uid == 1000) {
            if (type == 1 || type == 2) {
                this.mBadPkgManager.onPackageUpdate(pkgName, versionCode);
            }
        }
    }

    public void reportErrorPackage(String packageName, int uid, long versionCode, int errorType) {
        if (isSuperSystemProcessEnable()) {
            this.mBadPkgManager.reportErrorPackage(packageName, versionCode, errorType);
            SpClientNotifier.getInstance().notifyErrorPackage(packageName, uid, versionCode, errorType);
        }
    }

    public void componentStartPoint(int spsPid, int type, String componentPkgName, String componentClassName) {
        if (isSuperSystemProcessEnable()) {
            this.mComponentManager.componentStartPoint(spsPid, type, componentPkgName, componentClassName);
        }
    }

    public void componentFinishPoint(int spsPid, int type, String componentPkgName, String componentClassName) {
        if (isSuperSystemProcessEnable()) {
            this.mComponentManager.componentFinishPoint(spsPid, type, componentPkgName, componentClassName);
        }
    }

    public void componentTimeOut(int spsPid, int type, String componentPkgName, String componentClassName) {
        if (isSuperSystemProcessEnable() && spsPid != this.mLastReportedErrorPid) {
            this.mLastReportedErrorPid = spsPid;
            String blamePkg = this.mComponentManager.whoShouldBlame(spsPid, type, componentPkgName, componentClassName);
            VSlog.i("SpManager", "find blame package:" + blamePkg + " while component pkg:" + componentPkgName);
            if (!TextUtils.isEmpty(blamePkg)) {
                reportErrorPackage(blamePkg, 1000, -1L, componentTypeToErrorType(type));
            } else {
                reportErrorPackage(componentPkgName, 1000, -1L, 32);
            }
        }
    }

    public void identifySuperProcessIfNeed(ProcessRecord process, RMProcInfo rmProc) {
        if (isSuperSystemProcess(rmProc)) {
            VLog.d("SpManager", "identify super system process:" + process);
            synchronized (mProcessLock) {
                this.mSystemProcess = process;
            }
        }
    }

    public void clearSuperProcessIfNeed(ProcessRecord process, RMProcInfo rmProc) {
        if (isSuperSystemProcess(rmProc)) {
            VLog.d("SpManager", "clear super system process:" + process);
            synchronized (mProcessLock) {
                this.mSystemProcess = null;
            }
            this.mComponentManager.onSuperProcessDied(rmProc.mPid);
        }
    }

    private boolean isSuperSystemProcess(RMProcInfo rmProc) {
        return rmProc.mUid == 1000 && "com.vivo.sps".equals(rmProc.mPkgName) && "com.vivo.sps".equals(rmProc.mProcName);
    }

    private boolean isSuperAppProcess(RMProcInfo rmProc) {
        return false;
    }

    public void dump(PrintWriter pw, String[] args) {
        if (args.length >= 1 && args[0].equals("add_bad_package")) {
            if (args.length < 2) {
                pw.append("Usage: dumpsys rms --sps-native add_bad_package [package name] {version code} {error flags}\n");
                return;
            }
            this.mBadPkgManager.addBadPackage(args[1], args.length >= 3 ? Long.parseLong(args[2]) : -1L, args.length >= 4 ? Integer.parseInt(args[3]) : 16384);
            int pid = getSuperSystemProcessPid();
            if (pid > 0) {
                RMAms.getInstance().killProcess(new int[]{pid}, new int[]{ProcessList.PERSISTENT_PROC_ADJ}, "SPS DEBUG", true);
            }
        } else if (args.length >= 1 && args[0].equals("del_bad_package")) {
            if (args.length < 2) {
                pw.append("Usage: dumpsys rms --sps-native del_bad_package [package name]\n");
            } else {
                this.mBadPkgManager.removeBadPackage(args[1]);
            }
        } else if (args.length >= 1 && args[0].equals("report_error_package")) {
            if (args.length < 2) {
                pw.append("Usage: dumpsys rms --sps-native report_error_package [package name] {version code} {error flags}\n");
                return;
            }
            this.mBadPkgManager.reportErrorPackage(args[1], args.length >= 3 ? Long.parseLong(args[2]) : -1L, args.length >= 4 ? Integer.parseInt(args[3]) : 16384);
            int pid2 = getSuperSystemProcessPid();
            if (pid2 > 0) {
                RMAms.getInstance().killProcess(new int[]{pid2}, new int[]{ProcessList.PERSISTENT_PROC_ADJ}, "SPS DEBUG", true);
            }
        } else if (args.length >= 1 && args[0].equals("show_bad_package")) {
            this.mBadPkgManager.dumpBadPackages(pw);
        } else if (args.length >= 1 && args[0].equals("show_config")) {
            this.mConfigManager.dump(pw);
        } else if (args.length >= 1 && args[0].equals("show_anr")) {
            this.mComponentManager.dump(pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final SpManagerImpl INSTANCE = new SpManagerImpl();

        private Instance() {
        }
    }

    private SpManagerImpl() {
        this.mConfigManager = ConfigManager.getInstance();
        this.mBadPkgManager = BadPackageManager.getInstance();
        this.mComponentManager = ComponentLifeCycleMgr.getInstance();
        this.mAllowPackages = new HashSet<>();
        this.isSpsPublished = false;
        this.isSpsExist = false;
    }

    public static SpManagerImpl getInstance() {
        return Instance.INSTANCE;
    }
}