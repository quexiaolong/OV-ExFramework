package com.android.server.am;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.BroadcastOptions;
import android.app.IApplicationThread;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.INetworkPolicyManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.Watchdog;
import com.android.server.am.FrozenPackageRecord;
import com.android.server.am.frozen.FrozenDataInfo;
import com.android.server.am.frozen.FrozenDataManager;
import com.android.server.display.VirtualDisplayAdapter;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.WindowProcessController;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.proxy.VivoBinderProxy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/* loaded from: classes.dex */
public final class VivoFrozenPackageSupervisor {
    private static final String ACTION_PACKAGE_FREEZE = "vivo.intent.action.PACKAGE_FREEZE";
    private static final String ACTION_PACKAGE_UNFREEZE = "vivo.intent.action.PACKAGE_UNFREEZE";
    private static final long DELAY_CHECK_FROZEN_MS = 3600000;
    public static final int FLAG_FROZEN_CANCEL_JOB = 4;
    public static final int FLAG_FROZEN_CANCEL_NOTIFICATION = 2;
    public static final int FLAG_FROZEN_DEFAULT = 253;
    public static final int FLAG_FROZEN_RELEASE_ALARM = 128;
    public static final int FLAG_FROZEN_RELEASE_AUDIO = 16;
    public static final int FLAG_FROZEN_RELEASE_LOCATION = 32;
    public static final int FLAG_FROZEN_RELEASE_SENSOR = 8;
    public static final int FLAG_FROZEN_RELEASE_SOCKET = 64;
    public static final int FLAG_FROZEN_RELEASE_WAKELOCK = 1;
    private static final long MAX_FROZEN_TIME_MS = 259200000;
    private static final int MSG_CHECK_FROZEN_TIME = 8;
    private static final int MSG_FINISH_FROZEN = 6;
    private static final int MSG_FROZEN = 2;
    private static final int MSG_FROZEN_DELAY = 3;
    private static final int MSG_INIT = 1;
    private static final int MSG_UNFROZEN = 4;
    private static final int MSG_UNFROZEN_RESTORE = 5;
    private static final int MSG_VMR = 7;
    private static final int SIGCONT = 18;
    private static final int SIGSTOP = 19;
    public static final String TAG = "VFPS";
    public static final int UNFREEZE_TYPE_ACTIVITY = 1;
    public static final int UNFREEZE_TYPE_BROADCAST = 3;
    public static final int UNFREEZE_TYPE_CONFIG_CHANGE = 5;
    public static final int UNFREEZE_TYPE_MEDIA = 6;
    public static final int UNFREEZE_TYPE_PROVIDER = 4;
    public static final int UNFREEZE_TYPE_SERVICE = 2;
    private static VivoFrozenPackageSupervisor _instance = new VivoFrozenPackageSupervisor();
    private Method cancelAllNotificationsForFrozen;
    private Method cancelJobByUid;
    private Method getPendingJobsByUid;
    private Method hasVisibleActivitiesMethod;
    private PowerManager mPowerManager;
    private Method onFrozenPackageAlarmManager;
    private Method onFrozenPackageAudioManager;
    private Method onFrozenPackageDisplayManager;
    private Method onFrozenPackageLocationManager;
    private INetworkPolicyManager onFrozenPackageNetworkManager;
    private Method onFrozenPackagePowerManager;
    private Method onFrozenPackageSensorManager;
    private FrozenHandler mHandler = null;
    private Handler mAudioHandler = null;
    private Context mContext = null;
    private ActivityManagerService mService = null;
    private String mPlatformName = null;
    private String mPlatformAndroid = null;
    private String mPlatformDisplay = null;
    private boolean mSystemReady = false;
    private boolean mInitSuccess = false;
    private Map<Integer, String> mFrozenFromSrc = new HashMap();
    private Map<String, FrozenPackageRecord> mFrozenPackages = new HashMap();
    private List<FrozenPackageRecord> mWaitingUnfrozenPackages = new ArrayList();
    private List<String> mBlackFrozenApp = new ArrayList();
    private final Object mLock = new Object();
    private List<String> mPowerConnectedUnfreezeList = new ArrayList();
    private List<String> mCallerWhiteList = new ArrayList();
    private List<String> mBeCalledWhiteList = new ArrayList();
    private List<String> mInterfaceWhiteList = new ArrayList<String>() { // from class: com.android.server.am.VivoFrozenPackageSupervisor.1
        {
            add("com.vivo.pem.test");
            add("com.vivo.test.dut");
        }
    };
    final ArrayList<String> FROZEN_WHITELIST = new ArrayList<String>() { // from class: com.android.server.am.VivoFrozenPackageSupervisor.2
        {
            add("android.intent.action.MEDIA_BUTTON");
            add("android.intent.action.PHONE_STATE");
            add("android.appwidget.action.APPWIDGET_UPDATE");
        }
    };
    private boolean frozenEnableFromPem = false;
    private boolean mEnableFunction = false;
    private boolean mVmrEnable = false;
    private boolean mFreezeForegroundAppEnable = true;
    private FrozenHelper mFrozenHelper = new FrozenHelper();
    public FrozenStateObservable mFrozenStateObservable = null;
    private int mAudioResult = 0;
    private boolean mGetAudioResult = false;

    public static VivoFrozenPackageSupervisor getInstance() {
        return _instance;
    }

    private VivoFrozenPackageSupervisor() {
    }

    public void init(ActivityManagerService ams, Context context) {
        this.mService = ams;
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread("VivoFrozenPackageSupervisor");
        handlerThread.start();
        this.mHandler = new FrozenHandler(handlerThread.getLooper());
        HandlerThread audioThread = new HandlerThread("VFPS_audio");
        audioThread.start();
        this.mAudioHandler = new Handler(audioThread.getLooper());
        Watchdog.getInstance().addThread(this.mHandler);
        this.mHandler.sendEmptyMessageDelayed(1, 2000L);
        this.mFrozenStateObservable = new FrozenStateObservable();
        FrozenDataManager.getInstance().init(this.mContext, handlerThread.getLooper());
    }

    public void deleteObserver(Observer observer) {
        FrozenStateObservable frozenStateObservable = this.mFrozenStateObservable;
        if (frozenStateObservable != null) {
            frozenStateObservable.deleteObserver(observer);
        }
    }

    public void addObserver(Observer observer) {
        FrozenStateObservable frozenStateObservable = this.mFrozenStateObservable;
        if (frozenStateObservable != null) {
            frozenStateObservable.addObserver(observer);
        }
    }

    public void noteProcessAdd(int pid, int uid, String packageName, String processName, String reason) {
        if (!checkSystem() || packageName == null || packageName.trim().isEmpty()) {
            return;
        }
        synchronized (this.mLock) {
            String key = createKey(packageName, UserHandle.getUserId(uid));
            if (this.mFrozenPackages.containsKey(key)) {
                VLog.d(TAG, "noteProcessAdd for [pid:" + pid + " packageName: " + packageName + " processName: " + processName + " uid: " + uid + " reason:" + reason + "]");
                FrozenPackageRecord fpr = this.mFrozenPackages.get(key);
                this.mHandler.sendMessage(this.mHandler.obtainMessage(4, fpr));
            }
        }
    }

    public boolean isKeepFrozenService(ServiceRecord r, boolean forceUnfreeze, String reason) {
        if (r != null && r.appInfo != null && r.appInfo.packageName != null) {
            try {
                if (isKeepFrozen(r.appInfo.packageName, r.appInfo.uid, null, -1, 2, forceUnfreeze, reason)) {
                    return true;
                }
                return false;
            } catch (Exception e) {
                Slog.w(TAG, "Exception: " + e);
                return false;
            }
        }
        return false;
    }

    public boolean isKeepFrozenService(ServiceRecord r, ProcessRecord callerApp, boolean forceUnfreeze, String reason) {
        if (r != null && r.appInfo != null && r.appInfo.packageName != null) {
            if (callerApp != null) {
                try {
                    if (callerApp.hasForegroundActivities()) {
                        forceUnfreeze = true;
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Exception: " + e);
                    return false;
                }
            }
            if (callerApp != null && callerApp.info != null && callerApp.info.packageName != null) {
                if (isKeepFrozen(r.appInfo.packageName, r.appInfo.uid, callerApp.info.packageName, callerApp.uid, 2, forceUnfreeze, reason)) {
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    public boolean isKeepFrozenService(ActiveServices activeService, ServiceRecord r, IApplicationThread caller, String callingPackage, int callingUid, boolean forceUnfreeze, String reason) {
        boolean forceUnfreeze2;
        if (r != null) {
            try {
                if (r.appInfo != null && r.appInfo.uid >= 10000) {
                    if (r.appInfo.packageName != null) {
                        try {
                            if (caller != null) {
                                ProcessRecord callerApp = activeService.mAm.getRecordForAppLocked(caller);
                                if (callerApp != null && callerApp.hasForegroundActivities()) {
                                    forceUnfreeze2 = true;
                                } else {
                                    forceUnfreeze2 = forceUnfreeze;
                                }
                                if (callerApp != null) {
                                    try {
                                        if (callerApp.info != null && callerApp.info.packageName != null) {
                                            if (isKeepFrozen(r.appInfo.packageName, r.appInfo.uid, callerApp.info.packageName, callerApp.uid, 2, forceUnfreeze2, reason)) {
                                                return true;
                                            }
                                            return false;
                                        }
                                        return false;
                                    } catch (Exception e) {
                                        e = e;
                                        Slog.w(TAG, "Exception: " + e);
                                        return false;
                                    }
                                }
                                return false;
                            }
                            if (isKeepFrozen(r.appInfo.packageName, r.appInfo.uid, callingPackage, callingUid, 2, forceUnfreeze, reason)) {
                                return true;
                            }
                            return false;
                        } catch (Exception e2) {
                            e = e2;
                            Slog.w(TAG, "Exception: " + e);
                            return false;
                        }
                    }
                }
            } catch (Exception e3) {
                e = e3;
            }
        }
        return false;
    }

    public boolean isKeepFrozenBroadcastPrcocess(Object bro, ProcessRecord app, boolean forceUnfreeze, String reason) {
        if (app != null && app.info != null && app.info.packageName != null) {
            BroadcastRecord br = (BroadcastRecord) bro;
            if (br != null && br.intent != null && br.intent.getAction() != null) {
                String action = br.intent.getAction();
                if (action != null && (this.FROZEN_WHITELIST.contains(action) || VivoFcmGoldPassSupervisor.getInstance().allowDeliverFcmBroadcast(br.intent, app.info.packageName))) {
                    forceUnfreeze = true;
                }
                if ("android.intent.action.ACTION_POWER_CONNECTED".equals(action) && isInPowerConnectedUnfreezeList(app.info.packageName)) {
                    forceUnfreeze = true;
                }
            }
            if (isKeepFrozen(app.info.packageName, app.info.uid, null, -1, 3, forceUnfreeze, reason)) {
                VLog.d(TAG, "Skipping " + reason + ": " + app.processName + " because of process is frozen");
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean isKeepFrozenProcess(ProcessRecord app, boolean forceUnfreeze, String reason) {
        if (app != null && app.info != null && app.info.packageName != null) {
            return isKeepFrozen(app.info.packageName, app.info.uid, null, -1, 1, forceUnfreeze, reason);
        }
        return false;
    }

    public boolean isFrozenProcess(ProcessRecord app) {
        if (app != null && app.info != null && app.info.packageName != null) {
            return isFrozenPackage(app.info.packageName, app.info.uid);
        }
        return false;
    }

    public boolean isFrozenProcess(ProcessRecord app, PrintWriter pw) {
        if (app != null && app.info != null && app.info.packageName != null && isFrozenPackage(app.info.packageName, app.info.uid)) {
            if (pw != null) {
                pw.println("\n** this package: " + app.info.packageName + " process: " + app.processName + " has been frozen **");
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean isKeepFrozenProvider(Object cpro, ProcessRecord r) {
        boolean hasForegroundActivities;
        try {
            ContentProviderRecord cpr = (ContentProviderRecord) cpro;
            if (cpr != null && cpr.appInfo != null && cpr.appInfo.packageName != null) {
                if (r == null || !r.hasForegroundActivities()) {
                    hasForegroundActivities = false;
                } else {
                    hasForegroundActivities = true;
                }
                if (r != null && r.info != null && r.info.packageName != null) {
                    if (isKeepFrozen(cpr.appInfo.packageName, cpr.appInfo.uid, r.info.packageName, r.uid, 4, hasForegroundActivities, "Provider")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            VLog.w(TAG, "Exception: " + e);
        }
        return false;
    }

    public boolean hasVirtualDisplayLocked(VirtualDisplayAdapter virtualDisplayAdapter, String packageName) {
        if (packageName == null || packageName.isEmpty() || virtualDisplayAdapter == null || virtualDisplayAdapter.mVirtualDisplayDevices == null || virtualDisplayAdapter.mVirtualDisplayDevices.isEmpty()) {
            return false;
        }
        for (VirtualDisplayAdapter.VirtualDisplayDevice device : virtualDisplayAdapter.mVirtualDisplayDevices.values()) {
            if (packageName.equals(device.mOwnerPackageName)) {
                return true;
            }
        }
        return false;
    }

    public void noteProcessDied(int pid, int uid, ProcessRecord app, String reason) {
        if (app == null || app.info == null) {
            return;
        }
        noteProcessDied(pid, uid, app.info.packageName, app.processName, "process died.");
    }

    public void noteProcessDied(int pid, int uid, String packageName, String processName, String reason) {
        if (!checkSystem() || packageName == null || packageName.trim().isEmpty()) {
            return;
        }
        synchronized (this.mLock) {
            String key = createKey(packageName, UserHandle.getUserId(uid));
            if (this.mFrozenPackages.containsKey(key)) {
                FrozenPackageRecord fpr = this.mFrozenPackages.get(key);
                ProcessRecord normalProcPid = fpr.getNormalProcessFromPid(pid);
                FrozenPackageRecord.NativeProcessRecord nativeProcPid = fpr.getNativeProcessFromPid(pid);
                if (normalProcPid != null) {
                    fpr.rmNormalProcs(normalProcPid);
                }
                if (nativeProcPid != null) {
                    fpr.rmNativeProcs(nativeProcPid);
                }
                ArrayList<ProcessRecord> normalProcs = fpr.getNormalProcesses();
                ArrayList<FrozenPackageRecord.NativeProcessRecord> nativeProcs = fpr.getNativeProcesses();
                if ((normalProcs != null && normalProcs.size() > 0) || (nativeProcs != null && nativeProcs.size() > 0)) {
                    VLog.d(TAG, "noteProcessDied for packageName: " + packageName + " pid:" + pid + " processName: " + processName);
                    return;
                }
                VLog.d(TAG, "noteProcessDied unfreeze  packageName: " + packageName + " uid: " + uid);
                fpr.unfrozenReason = reason;
                this.mHandler.sendMessage(this.mHandler.obtainMessage(4, fpr));
            }
        }
    }

    public void systemReady() {
        VLog.d(TAG, "system ready.");
        this.mSystemReady = true;
        unfrozenAllPackages();
    }

    public void setFreezeForegroundAppEnable(boolean enable) {
        this.mFreezeForegroundAppEnable = enable;
    }

    private boolean filterActivityVisible(ArrayList<ProcessRecord> procs) {
        WindowProcessController wpc;
        if ((!this.mFreezeForegroundAppEnable || isInteractive()) && procs != null) {
            try {
                if (procs.size() > 0) {
                    Iterator<ProcessRecord> it = procs.iterator();
                    while (it.hasNext()) {
                        ProcessRecord proc = it.next();
                        if (proc != null && (wpc = proc.getWindowProcessController()) != null && hasVisibleActivitiesIgnoringKeyguard(wpc)) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                VLog.e(TAG, "filterActivityVisible error: ", e);
            }
        }
        return false;
    }

    private boolean isInteractive() {
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        }
        PowerManager powerManager = this.mPowerManager;
        return powerManager != null && powerManager.isInteractive();
    }

    private boolean hasVisibleActivitiesIgnoringKeyguard(WindowProcessController wpc) {
        synchronized (wpc.mAtm.mGlobalLockWithoutBoost) {
            for (int i = wpc.mActivities.size() - 1; i >= 0; i--) {
                ActivityRecord r = (ActivityRecord) wpc.mActivities.get(i);
                if (r.visibleIgnoringKeyguard) {
                    return true;
                }
            }
            return false;
        }
    }

    private String getRequestFromWhich(int reqFrom) {
        int callingPid;
        String callingPkg;
        if (reqFrom == 2) {
            callingPid = Process.myPid();
        } else {
            Binder.getCallingUid();
            callingPid = Binder.getCallingPid();
        }
        try {
            if (this.mFrozenFromSrc.containsKey(Integer.valueOf(callingPid))) {
                String callingPkg2 = this.mFrozenFromSrc.get(Integer.valueOf(callingPid));
                return callingPkg2;
            }
            if (reqFrom == 2) {
                callingPkg = "quickfrozen";
            } else {
                callingPkg = getProcNameByPid(callingPid);
            }
            Map<Integer, String> tmpMap = new HashMap<>();
            if (this.mFrozenFromSrc.size() > 7) {
                for (Map.Entry<Integer, String> entry : this.mFrozenFromSrc.entrySet()) {
                    if (callingPkg.equals(entry.getValue())) {
                        tmpMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            if (tmpMap.size() > 0) {
                for (Map.Entry<Integer, String> entry2 : tmpMap.entrySet()) {
                    this.mFrozenFromSrc.remove(entry2.getKey());
                }
            }
            this.mFrozenFromSrc.put(Integer.valueOf(callingPid), callingPkg);
            return callingPkg;
        } catch (Exception e) {
            e.printStackTrace();
            return ProxyConfigs.CTRL_MODULE_PEM;
        }
    }

    public void requestFrozen(String packageName, boolean isFrozen, int reqFrom) {
        requestFrozen(-1, packageName, isFrozen, FLAG_FROZEN_DEFAULT, reqFrom, null);
    }

    public void requestFrozenWithFlag(String packageName, boolean isFrozen, int flag, int reqFrom) {
        requestFrozen(-1, packageName, isFrozen, flag, reqFrom, null);
    }

    public void requestFrozenWithUid(String packageName, boolean isFrozen, int flag, int uid, int reqFrom) {
        requestFrozen(uid, packageName, isFrozen, flag, reqFrom, null);
    }

    public void requestFrozen(String packageName, boolean isFrozen, int flag, int uid) {
        requestFrozen(uid, packageName, isFrozen, flag, -1, null);
    }

    public void requestFrozen(int uid, String packageName, boolean isFrozen, int reqFrom, List<ProcessRecord> list) {
        requestFrozen(uid, packageName, isFrozen, FLAG_FROZEN_DEFAULT, reqFrom, list);
    }

    private void requestFrozen(int desUid, String packageName, boolean isFrozen, int flag, int reqFrom, List<ProcessRecord> list) {
        int uid;
        if (!checkSystem() || !checkInterfaceCaller()) {
            return;
        }
        if (packageName == null || packageName.trim().isEmpty()) {
            VLog.d(TAG, "packageName unavailable.");
            return;
        }
        try {
            String callingPkg = getRequestFromWhich(reqFrom);
            if (desUid > 0) {
                uid = desUid;
            } else {
                uid = AppGlobals.getPackageManager().getPackageUid(packageName, (int) Dataspace.STANDARD_BT601_625, ActivityManager.getCurrentUser());
            }
            if (flag == -1) {
                flag = FLAG_FROZEN_DEFAULT;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("request ");
            sb.append(isFrozen ? "freeze " : "unfreeze ");
            sb.append("pkgName[");
            sb.append(packageName);
            sb.append("], uid[");
            sb.append(uid);
            sb.append("] from ");
            sb.append(callingPkg);
            sb.append(", flag = ");
            sb.append(flag);
            VLog.i(TAG, sb.toString());
            if (uid < 10000) {
                VLog.d(TAG, "Only thridpart App can be freezed!");
            } else if (isFrozen && this.mService.getUidState(uid) <= 2) {
                VLog.d(TAG, "foreground app can not be freezed!");
            } else {
                synchronized (this.mLock) {
                    String key = createKey(packageName, UserHandle.getUserId(uid));
                    if (this.mFrozenPackages.containsKey(key)) {
                        FrozenPackageRecord fpr = this.mFrozenPackages.get(key);
                        fpr.frozenReason = "from " + callingPkg;
                        if (isFrozen) {
                            VLog.d(TAG, packageName + "can not frozen , because status =" + fpr.status);
                            return;
                        }
                        doUnfrozenLocked(fpr);
                    } else {
                        FrozenPackageRecord fpr2 = new FrozenPackageRecord(uid, packageName, "from " + callingPkg, flag);
                        this.mFrozenPackages.put(key, fpr2);
                        if (isFrozen) {
                            if (list != null && list.size() > 0) {
                                fpr2.addNormalProcs((ArrayList) list);
                            }
                            doFrozenLocked(fpr2);
                        } else {
                            doUnfrozenLocked(fpr2);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setFrozenEnable(boolean frozenEnable) {
        this.frozenEnableFromPem = frozenEnable;
        readyFrozenEnvironment();
    }

    public void readyFrozenEnvironment() {
        if (!this.mSystemReady || !this.mInitSuccess || !checkInterfaceCaller() || this.mEnableFunction == this.frozenEnableFromPem) {
            return;
        }
        if (!SystemProperties.getBoolean("persist.sys.enable_frozen", true)) {
            this.mEnableFunction = false;
            return;
        }
        if (!this.mEnableFunction) {
            unfrozenAllPackages();
        }
        this.mEnableFunction = this.frozenEnableFromPem;
    }

    public boolean isEnableFunction() {
        return this.mEnableFunction;
    }

    public void setVmrEnable(boolean enable) {
        if (!checkSystem() || !checkInterfaceCaller() || this.mVmrEnable == enable) {
            return;
        }
        this.mVmrEnable = enable;
    }

    public boolean setCallerWhiteList(List<String> pkgNames) {
        if (checkSystem() && checkInterfaceCaller() && pkgNames != null && pkgNames.size() != 0) {
            synchronized (this.mCallerWhiteList) {
                this.mCallerWhiteList.clear();
                this.mCallerWhiteList.addAll(pkgNames);
            }
            return true;
        }
        return false;
    }

    private boolean isInCallerWhiteList(String packageName) {
        boolean z = false;
        if (packageName == null || packageName.trim().length() <= 0) {
            return false;
        }
        synchronized (this.mCallerWhiteList) {
            z = (this.mCallerWhiteList.contains(packageName) || isSystemUI(packageName)) ? true : true;
        }
        return z;
    }

    private boolean isSystemUI(String packageName) {
        return packageName != null && packageName.startsWith("android.uid.systemui");
    }

    public boolean setBeCalledWhiteList(List<String> pkgNames) {
        if (checkSystem() && checkInterfaceCaller() && pkgNames != null && pkgNames.size() != 0) {
            synchronized (this.mBeCalledWhiteList) {
                this.mBeCalledWhiteList.clear();
                this.mBeCalledWhiteList.addAll(pkgNames);
            }
            return true;
        }
        return false;
    }

    public boolean setInterfaceWhiteList(List<String> pkgNames) {
        if (checkSystem() && checkInterfaceCaller() && pkgNames != null && pkgNames.size() != 0) {
            synchronized (this.mInterfaceWhiteList) {
                this.mInterfaceWhiteList.addAll(pkgNames);
            }
            return true;
        }
        return false;
    }

    private boolean isInBeCalledWhiteList(String packageName) {
        boolean contains;
        if (packageName == null || packageName.trim().length() <= 0) {
            return false;
        }
        synchronized (this.mBeCalledWhiteList) {
            contains = this.mBeCalledWhiteList.contains(packageName);
        }
        return contains;
    }

    public boolean setPowerConnectedUnfreezeList(Bundle data) {
        ArrayList<String> list;
        if (data == null || (list = data.getStringArrayList("list")) == null) {
            return false;
        }
        synchronized (this.mPowerConnectedUnfreezeList) {
            this.mPowerConnectedUnfreezeList.clear();
            this.mPowerConnectedUnfreezeList.addAll(list);
            VLog.i(TAG, "setPowerConnectedUnfreezeList, size = " + list.size());
        }
        return true;
    }

    private boolean isInPowerConnectedUnfreezeList(String pkgName) {
        boolean contains;
        if (TextUtils.isEmpty(pkgName)) {
            return false;
        }
        synchronized (this.mPowerConnectedUnfreezeList) {
            contains = this.mPowerConnectedUnfreezeList.contains(pkgName);
        }
        return contains;
    }

    public boolean setFrozenPkgBlacklist(List<String> appList, int size, int add, int fromWhich) {
        if (appList == null || appList.size() == 0 || size <= 0 || size != appList.size()) {
            VLog.d(TAG, "get frozen black pkglist failed!");
            return false;
        }
        synchronized (this.mBlackFrozenApp) {
            this.mBlackFrozenApp.clear();
            this.mBlackFrozenApp.addAll(appList);
        }
        for (int i = 0; i < appList.size(); i++) {
            VLog.d(TAG, "get frozen black pkglist: " + appList.get(i));
        }
        return true;
    }

    /* JADX WARN: Removed duplicated region for block: B:35:0x003d A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean isFrozenPackage(java.lang.String r7, int r8) {
        /*
            r6 = this;
            boolean r0 = r6.checkSystem()
            r1 = 0
            if (r0 != 0) goto L8
            return r1
        L8:
            if (r7 == 0) goto L55
            boolean r0 = r7.isEmpty()
            if (r0 == 0) goto L11
            goto L55
        L11:
            r0 = -1
            if (r8 != r0) goto L39
            long r2 = android.os.Binder.clearCallingIdentity()
            android.content.pm.IPackageManager r0 = android.app.AppGlobals.getPackageManager()     // Catch: java.lang.Throwable -> L2b java.lang.Exception -> L2d
            r4 = 131072(0x20000, float:1.83671E-40)
            int r5 = android.app.ActivityManager.getCurrentUser()     // Catch: java.lang.Throwable -> L2b java.lang.Exception -> L2d
            int r0 = r0.getPackageUid(r7, r4, r5)     // Catch: java.lang.Throwable -> L2b java.lang.Exception -> L2d
            r8 = r0
            android.os.Binder.restoreCallingIdentity(r2)
            goto L3a
        L2b:
            r0 = move-exception
            goto L35
        L2d:
            r0 = move-exception
            r0.printStackTrace()     // Catch: java.lang.Throwable -> L2b
            android.os.Binder.restoreCallingIdentity(r2)
            goto L39
        L35:
            android.os.Binder.restoreCallingIdentity(r2)
            throw r0
        L39:
            r0 = r8
        L3a:
            java.lang.Object r2 = r6.mLock
            monitor-enter(r2)
            int r8 = android.os.UserHandle.getUserId(r0)     // Catch: java.lang.Throwable -> L52
            java.lang.String r8 = createKey(r7, r8)     // Catch: java.lang.Throwable -> L52
            java.util.Map<java.lang.String, com.android.server.am.FrozenPackageRecord> r3 = r6.mFrozenPackages     // Catch: java.lang.Throwable -> L52
            boolean r3 = r3.containsKey(r8)     // Catch: java.lang.Throwable -> L52
            if (r3 == 0) goto L50
            r1 = 1
            monitor-exit(r2)     // Catch: java.lang.Throwable -> L52
            return r1
        L50:
            monitor-exit(r2)     // Catch: java.lang.Throwable -> L52
            return r1
        L52:
            r8 = move-exception
            monitor-exit(r2)     // Catch: java.lang.Throwable -> L52
            throw r8
        L55:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.VivoFrozenPackageSupervisor.isFrozenPackage(java.lang.String, int):boolean");
    }

    public boolean isFrozenPackage(int uid) {
        if (checkSystem() && uid >= 10000) {
            synchronized (this.mLock) {
                for (FrozenPackageRecord fpr : this.mFrozenPackages.values()) {
                    if (fpr.uid == uid) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:84:0x003e A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean isKeepFrozen(java.lang.String r8, int r9, java.lang.String r10, int r11, int r12, boolean r13, java.lang.String r14) {
        /*
            Method dump skipped, instructions count: 467
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.VivoFrozenPackageSupervisor.isKeepFrozen(java.lang.String, int, java.lang.String, int, int, boolean, java.lang.String):boolean");
    }

    public void unfrozenAllPackages() {
        if (!this.mSystemReady || !this.mInitSuccess) {
            return;
        }
        synchronized (this.mLock) {
            List<FrozenPackageRecord> tmpList = new ArrayList<>(this.mFrozenPackages.values());
            for (FrozenPackageRecord fpr : tmpList) {
                doUnfrozenLocked(fpr);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCheckFrozenTime() {
        if (!this.mSystemReady || !this.mInitSuccess) {
            return;
        }
        VLog.d(TAG, "check frozen time !");
        synchronized (this.mLock) {
            List<FrozenPackageRecord> tmpList = new ArrayList<>(this.mFrozenPackages.values());
            for (FrozenPackageRecord fpr : tmpList) {
                if (fpr.getFrozenUptimeInterval() > MAX_FROZEN_TIME_MS && fpr.frozenReason.endsWith(ProxyConfigs.CTRL_MODULE_PEM)) {
                    VLog.d(TAG, fpr.packageName + " frozen too long, need unfrozen.");
                    doUnfrozenLocked(fpr);
                }
            }
        }
    }

    private boolean checkSystem() {
        return this.mSystemReady && this.mInitSuccess && this.mEnableFunction;
    }

    private boolean checkInterfaceCaller() {
        int callingUid = Binder.getCallingUid();
        String[] callingPkgList = null;
        try {
            callingPkgList = AppGlobals.getPackageManager().getPackagesForUid(callingUid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (callingUid != 1000) {
            if (callingPkgList == null || callingPkgList.length <= 0 || !this.mInterfaceWhiteList.contains(callingPkgList[0])) {
                VLog.d(TAG, "Only system app can call vmr enable.");
                return false;
            }
            return true;
        }
        return true;
    }

    private void doFrozenLocked(FrozenPackageRecord fpr) {
        fpr.status = 1;
        fpr.tryFrozenCnt = 0;
        fpr.startFrozenTime = SystemClock.elapsedRealtime();
        FrozenHandler frozenHandler = this.mHandler;
        frozenHandler.sendMessage(frozenHandler.obtainMessage(2, fpr));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doUnfrozenLocked(FrozenPackageRecord fpr) {
        if (fpr.status == 2) {
            VLog.d(TAG, fpr.packageName + "is unfrozening.");
        } else if (fpr.status == 1) {
            if (!this.mWaitingUnfrozenPackages.contains(fpr)) {
                this.mWaitingUnfrozenPackages.add(fpr);
            }
            VLog.d(TAG, fpr.packageName + " can not unfrozen , because need wait." + fpr.status);
        } else {
            fpr.status = 2;
            fpr.startUnfrozenTime = SystemClock.elapsedRealtime();
            ArrayList<FrozenPackageRecord.NativeProcessRecord> nativeProcs = fpr.getNativeProcesses();
            Iterator<FrozenPackageRecord.NativeProcessRecord> it = nativeProcs.iterator();
            while (it.hasNext()) {
                FrozenPackageRecord.NativeProcessRecord proc = it.next();
                this.mFrozenHelper.unfrozen(proc.pid);
                VLog.d(TAG, "This native process " + proc.pid + " - " + proc.processName + " be unfreezed.");
            }
            ArrayList<ProcessRecord> normalProcs = fpr.getNormalProcesses();
            Iterator<ProcessRecord> it2 = normalProcs.iterator();
            while (it2.hasNext()) {
                ProcessRecord proc2 = it2.next();
                if (proc2.killedByAm || proc2.killed || proc2.pid <= 0) {
                    VLog.d(TAG, "This normal process " + proc2.pid + " - " + proc2.processName + " has been killed! ");
                } else {
                    proc2.setFrozenStatus(false);
                    this.mFrozenHelper.unfrozen(proc2.pid);
                    VLog.d(TAG, "This normal process " + proc2.pid + " - " + proc2.processName + " be unfreezed.");
                }
            }
            fpr.status = 0;
            this.mFrozenPackages.remove(createKey(fpr.packageName, fpr.userId));
            this.mWaitingUnfrozenPackages.remove(fpr);
            VLog.d(TAG, "package " + fpr.packageName + " has been unfrozen for " + fpr.getFrozenInterval() + " in " + fpr.getSpendUnfrozenTime() + "ms");
            VivoBinderProxy.getInstance().reportUnfreezeApp(fpr.getNormalProcesses());
            FrozenHandler frozenHandler = this.mHandler;
            frozenHandler.sendMessage(frozenHandler.obtainMessage(5, fpr));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finishUnfrozen(FrozenPackageRecord fpr) {
        int audioResult = onFrozenPackageForAudio(fpr.uid, false);
        int sensorResult = onFrozenPackageForSensor(fpr.packageName, fpr.uid, false);
        int displayResult = onFrozenPackageForDisplayManager(fpr.packageName, false);
        int gpsResult = onFrozenPackageForGPS(fpr.packageName, fpr.uid, false);
        int alarmResult = onFrozenPackageForAlarm(fpr.uid, fpr.packageName, false);
        VLog.d(TAG, fpr.packageName + " finish unfrozen gpsResult=" + gpsResult + ",sensorResult=" + sensorResult + ",audioResult=" + audioResult + ",displayResult=" + displayResult + ",alarmResult=" + alarmResult);
        Intent intent = new Intent(ACTION_PACKAGE_UNFREEZE);
        intent.putExtra("android.intent.extra.PACKAGE_NAME", fpr.packageName);
        intent.putExtra("android.intent.extra.UID", fpr.uid);
        this.mContext.sendBroadcast(intent);
        vmr(fpr, false);
        FrozenNotifier.unfreeze(fpr.packageName, fpr.uid, fpr.userId);
        FrozenStateObservable frozenStateObservable = this.mFrozenStateObservable;
        if (frozenStateObservable != null) {
            frozenStateObservable.updateAppUnfrozenRecord(fpr.uid, fpr.packageName, fpr.frozenFromWhich(fpr.frozenReason), fpr.unfrozenByWhich(fpr.unfrozenReason));
        }
        EventLog.writeEvent(getIntFieldFromEventTags("AM_APP_UNFROZEN"), Integer.valueOf(UserHandle.getUserId(fpr.uid)), Integer.valueOf(fpr.uid), fpr.packageName, fpr.unfrozenReason);
        if (needSendBroadcast(fpr.unfrozenReason)) {
            synchronized (this.mService) {
                sendConnectivityChangedBroadcastLocked(fpr.packageName, fpr.uid);
            }
        }
    }

    private boolean needSendBroadcast(String reason) {
        return "Start activity".equals(reason) || "ScreenOn".equals(reason) || "resume top activity".equals(reason);
    }

    private void sendConnectivityChangedBroadcastLocked(String packageName, int uid) {
        ArrayList<Intent> intents;
        if (!TextUtils.isEmpty(packageName) && UserHandle.isApp(uid)) {
            Intent sticky = null;
            ArrayMap<String, ArrayList<Intent>> stickies = (ArrayMap) this.mService.mStickyBroadcasts.get(-1);
            if (stickies != null && (intents = stickies.get("android.net.conn.CONNECTIVITY_CHANGE")) != null && !intents.isEmpty()) {
                sticky = intents.get(0);
            }
            if (sticky != null) {
                Intent queryIntent = new Intent(sticky).setPackage(packageName);
                List registeredReceivers = this.mService.mReceiverResolver.queryIntent(queryIntent, (String) null, false, UserHandle.getUserId(uid));
                if (registeredReceivers != null && registeredReceivers.size() > 0) {
                    BroadcastQueue queue = this.mService.broadcastQueueForIntent(sticky);
                    BroadcastRecord r = new BroadcastRecord(queue, sticky, (ProcessRecord) null, (String) null, (String) null, -1, -1, false, (String) null, (String[]) null, -1, (BroadcastOptions) null, registeredReceivers, (IIntentReceiver) null, 0, (String) null, (Bundle) null, false, true, true, -1, false, false);
                    queue.enqueueParallelBroadcastLocked(r);
                    queue.scheduleBroadcastsLocked();
                    VLog.i(TAG, "sendBroadcast, CONNECTIVITY_CHANGE, pkg = " + packageName + ", uid = " + uid);
                }
            }
        }
    }

    private boolean hasExecutingService(ArrayList<ProcessRecord> normalProcs) {
        synchronized (this.mService) {
            Iterator<ProcessRecord> it = normalProcs.iterator();
            while (it.hasNext()) {
                ProcessRecord app = it.next();
                int size = app.executingServices.size();
                if (size > 0) {
                    VLog.v(TAG, "There has " + size + " executing services: " + app.processName);
                    return true;
                }
            }
            return false;
        }
    }

    private boolean hasComponentExecuting(ArrayList<ProcessRecord> normalProcs) {
        Iterator<ProcessRecord> it = normalProcs.iterator();
        while (it.hasNext()) {
            ProcessRecord app = it.next();
            if ((app.mFreezeFlags & 3) != 0) {
                VLog.v(TAG, "There has ComponentExecuting: " + app.processName + " mFreezeFlags " + app.mFreezeFlags + " app.setProcState " + app.setProcState);
                return true;
            }
        }
        return false;
    }

    private void setNativeProcessName(ArrayList<FrozenPackageRecord.NativeProcessRecord> nativeProcs) {
        if (nativeProcs == null) {
            return;
        }
        Iterator<FrozenPackageRecord.NativeProcessRecord> it = nativeProcs.iterator();
        while (it.hasNext()) {
            FrozenPackageRecord.NativeProcessRecord proc = it.next();
            if (proc != null) {
                String procName = getProcNameByPid(proc.pid);
                proc.setProcessName(procName);
            }
        }
    }

    private boolean hasDex2oatProcess(ArrayList<FrozenPackageRecord.NativeProcessRecord> nativeProcs) {
        if (nativeProcs == null) {
            return false;
        }
        Iterator<FrozenPackageRecord.NativeProcessRecord> it = nativeProcs.iterator();
        while (it.hasNext()) {
            FrozenPackageRecord.NativeProcessRecord proc = it.next();
            if (proc != null && "dex2oat".equals(proc.processName)) {
                VLog.d(TAG, "There are dex2oat process running in: " + proc.pid);
                return true;
            }
        }
        return false;
    }

    private boolean disconnectServices(FrozenPackageRecord fpr, ArrayList<ProcessRecord> normalProcs) {
        ArrayList<ConnectionRecord> services;
        boolean result = false;
        Iterator<ProcessRecord> it = normalProcs.iterator();
        while (it.hasNext()) {
            ProcessRecord app = it.next();
            if (app != null && (services = disconnectClientServiceLocked(app)) != null && services.size() > 0) {
                fpr.addServices(services);
                result = true;
            }
        }
        return result;
    }

    private boolean cancelJob(int uid) {
        boolean result = false;
        try {
            JobScheduler jobScheduler = (JobScheduler) this.mContext.getSystemService("jobscheduler");
            if (this.getPendingJobsByUid == null || this.cancelJobByUid == null) {
                VLog.d(TAG, "failed to get job-related method");
            } else {
                List<JobInfo> jbs = (List) this.getPendingJobsByUid.invoke(jobScheduler, Integer.valueOf(uid));
                if (jbs != null && jbs.size() > 0) {
                    for (JobInfo jb : jbs) {
                        if (jb != null) {
                            this.cancelJobByUid.invoke(jobScheduler, Integer.valueOf(uid), Integer.valueOf(jb.getId()));
                            VLog.d(TAG, "Cancel frozen package " + uid + " JobScheduler " + jb);
                        }
                    }
                    result = true;
                }
            }
        } catch (Exception e) {
            VLog.w(TAG, "Perform JobScheduler failed! " + e);
        }
        return result;
    }

    private ArrayList<ConnectionRecord> disconnectClientServiceLocked(ProcessRecord app) {
        if (app == null || app.info == null || app.connections == null || app.connections.size() <= 0) {
            return null;
        }
        ArrayList<ConnectionRecord> services = null;
        Iterator it = app.connections.iterator();
        while (it.hasNext()) {
            ConnectionRecord cr = (ConnectionRecord) it.next();
            if (cr != null && cr.binding != null && cr.binding.client != null && cr.binding.client.info != null && cr.binding.client.info.packageName != null && !app.info.packageName.equals(cr.binding.client.info.packageName)) {
                try {
                    VLog.d(TAG, "Disconnect client that not belong to this package:" + app.info.packageName);
                    if (cr != null && cr.conn != null) {
                        cr.conn.connected(cr.binding.service.name, (IBinder) null, true);
                    }
                    if (services == null) {
                        services = new ArrayList<>();
                    }
                    services.add(cr);
                } catch (Exception e) {
                    VLog.w(TAG, "Failure disconnecting service: " + cr.binding.service.name);
                }
            }
        }
        return services;
    }

    private void reconnectClientServiceLocked(ArrayList<ConnectionRecord> services) {
        if (services == null) {
            return;
        }
        Iterator<ConnectionRecord> it = services.iterator();
        while (it.hasNext()) {
            ConnectionRecord cr = it.next();
            try {
                VLog.d(TAG, "Reconnect client: " + cr.toString());
                if (cr.binding != null && cr.binding.intent != null && cr.binding.intent.binder != null && cr.binding.service != null && cr.binding.service.name != null) {
                    cr.conn.connected(cr.binding.service.name, cr.binding.intent.binder, false);
                }
            } catch (Exception e) {
                VLog.w(TAG, "Failure reconnecting service: ", e);
            }
        }
    }

    private final ArrayList<ProcessRecord> collectNormalProcess(int uid, String packageName, int appId, int userId) {
        ArrayList<ProcessRecord> procs = new ArrayList<>();
        synchronized (this.mService) {
            if (!UserHandle.isApp(uid)) {
                VLog.d(TAG, "is isIsolated uid: " + uid + ", pkgname: " + packageName + ", userHandle appid: " + UserHandle.getAppId(uid));
            }
            int NP = this.mService.mProcessList.mProcessNames.getMap().size();
            for (int ip = 0; ip < NP; ip++) {
                SparseArray<ProcessRecord> apps = (SparseArray) this.mService.mProcessList.mProcessNames.getMap().valueAt(ip);
                int NA = apps.size();
                for (int ia = 0; ia < NA; ia++) {
                    ProcessRecord app = apps.valueAt(ia);
                    if (app.info != null && app.info.packageName != null && app.info.packageName.equals(packageName) && UserHandle.isApp(app.info.uid) && app.info.uid == uid) {
                        if (app.uid == Process.getUidForPid(app.pid)) {
                            procs.add(app);
                        }
                    } else {
                        boolean isDep = app.pkgDeps != null && app.pkgDeps.contains(packageName);
                        if ((isDep || UserHandle.getAppId(app.uid) == appId) && ((userId == -1 || app.userId == userId) && ((app.pkgList.containsKey(packageName) || isDep) && app.uid == Process.getUidForPid(app.pid)))) {
                            procs.add(app);
                        }
                    }
                }
            }
        }
        return procs;
    }

    private final ArrayList<FrozenPackageRecord.NativeProcessRecord> collectNativeProcessLocked(FrozenPackageRecord fpr, ArrayList<ProcessRecord> parents) {
        int pid;
        ArrayList<FrozenPackageRecord.NativeProcessRecord> childs = new ArrayList<>();
        ArrayList<FrozenPackageRecord.NativeProcessRecord> result = new ArrayList<>();
        if (fpr != null && parents != null && parents.size() > 0) {
            Iterator<ProcessRecord> it = parents.iterator();
            BufferedReader reader = null;
            while (it.hasNext()) {
                ProcessRecord proc = it.next();
                try {
                    try {
                        String path = "/acct/uid_" + fpr.uid + "/pid_" + proc.pid + "/cgroup.procs";
                        File file = new File(path);
                        if (file.exists()) {
                            reader = new BufferedReader(new FileReader(file));
                            while (true) {
                                String line = reader.readLine();
                                if (line == null) {
                                    break;
                                } else if (line != null && !line.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && proc.pid != (pid = Integer.parseInt(line))) {
                                    FrozenPackageRecord.NativeProcessRecord nativeProc = new FrozenPackageRecord.NativeProcessRecord(fpr.packageName, pid, proc.pid, fpr.uid, fpr.userId);
                                    childs.add(nativeProc);
                                }
                            }
                        }
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Exception e) {
                            }
                        }
                    } catch (Exception e2) {
                        VLog.e(TAG, "Exception=" + e2);
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Exception e3) {
                            }
                        }
                        return null;
                    }
                } catch (Throwable th) {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e4) {
                        }
                    }
                    throw th;
                }
            }
        }
        for (int i = childs.size(); i > 0; i--) {
            FrozenPackageRecord.NativeProcessRecord proc2 = childs.get(i - 1);
            if (proc2 != null) {
                result.add(proc2);
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class FrozenHandler extends Handler {
        public FrozenHandler(Looper loop) {
            super(loop);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    VivoFrozenPackageSupervisor vivoFrozenPackageSupervisor = VivoFrozenPackageSupervisor.this;
                    vivoFrozenPackageSupervisor.mInitSuccess = vivoFrozenPackageSupervisor.handleMsgInit();
                    VivoFrozenPackageSupervisor.this.mHandler.sendEmptyMessageDelayed(8, VivoFrozenPackageSupervisor.DELAY_CHECK_FROZEN_MS);
                    return;
                case 2:
                    FrozenPackageRecord fpr = (FrozenPackageRecord) msg.obj;
                    boolean success = VivoFrozenPackageSupervisor.this.handleFrozen(fpr);
                    synchronized (VivoFrozenPackageSupervisor.this.mLock) {
                        if (!success) {
                            fpr.status = 0;
                            VivoFrozenPackageSupervisor.this.mFrozenPackages.remove(VivoFrozenPackageSupervisor.createKey(fpr.packageName, fpr.userId));
                            VivoFrozenPackageSupervisor.this.mWaitingUnfrozenPackages.remove(fpr);
                        }
                    }
                    return;
                case 3:
                    synchronized (VivoFrozenPackageSupervisor.this.mLock) {
                        VivoFrozenPackageSupervisor.this.realDoFrozenLocked((FrozenPackageRecord) msg.obj);
                    }
                    return;
                case 4:
                    synchronized (VivoFrozenPackageSupervisor.this.mLock) {
                        VivoFrozenPackageSupervisor.this.doUnfrozenLocked((FrozenPackageRecord) msg.obj);
                    }
                    return;
                case 5:
                    VivoFrozenPackageSupervisor.this.finishUnfrozen((FrozenPackageRecord) msg.obj);
                    return;
                case 6:
                    VivoFrozenPackageSupervisor.this.finishFrozen((FrozenPackageRecord) msg.obj);
                    return;
                case 7:
                default:
                    return;
                case 8:
                    VivoFrozenPackageSupervisor.this.handleCheckFrozenTime();
                    VivoFrozenPackageSupervisor.this.mHandler.sendEmptyMessageDelayed(8, VivoFrozenPackageSupervisor.DELAY_CHECK_FROZEN_MS);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean handleMsgInit() {
        initMethods();
        readyFrozenEnvironment();
        this.mPlatformName = SystemProperties.get("ro.vivo.product.platform");
        this.mPlatformAndroid = SystemProperties.get("ro.build.version.release");
        this.mPlatformDisplay = SystemProperties.get("ro.vivo.lcm.xhd");
        SystemProperties.set("persist.vivo.support.freeze_fgapp", String.valueOf(true));
        VLog.i(TAG, "Platform info:[name: " + this.mPlatformName + " android: " + this.mPlatformAndroid + " display: " + this.mPlatformDisplay + "]");
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean handleFrozen(FrozenPackageRecord fpr) {
        ArrayList<ProcessRecord> normalProcs;
        if (fpr.getNormalProcesses().isEmpty()) {
            normalProcs = collectNormalProcess(fpr.uid, fpr.packageName, fpr.appId, fpr.userId);
            if (normalProcs.size() == 0) {
                VLog.w(TAG, "There no processes of package " + fpr.packageName + " running.");
                return false;
            }
        } else {
            normalProcs = new ArrayList<>();
            normalProcs.addAll(fpr.getNormalProcesses());
        }
        if (filterActivityVisible(normalProcs)) {
            if (RmsInjectorImpl.getInstance().isRmsPreload(fpr.packageName, fpr.uid)) {
                VLog.d(TAG, "packageName: " + fpr.packageName + " has visible activities is called by rms continue be freezed!");
            } else {
                VLog.d(TAG, "packageName: " + fpr.packageName + " has visible activities ,cannot be frozen.");
                return false;
            }
        }
        if (fpr.getNormalProcesses().isEmpty()) {
            fpr.addNormalProcs(normalProcs);
        }
        ArrayList<FrozenPackageRecord.NativeProcessRecord> nativeProcs = collectNativeProcessLocked(fpr, normalProcs);
        fpr.addNativeProcs(nativeProcs);
        if (hasComponentExecuting(normalProcs)) {
            if (fpr.tryFrozenCnt < 2) {
                fpr.tryFrozenCnt++;
                FrozenHandler frozenHandler = this.mHandler;
                frozenHandler.sendMessageDelayed(frozenHandler.obtainMessage(2, fpr), 2000L);
                return true;
            }
            return false;
        }
        setNativeProcessName(nativeProcs);
        boolean hasJobs = cancelJob(fpr.uid);
        if (hasJobs) {
            FrozenHandler frozenHandler2 = this.mHandler;
            frozenHandler2.sendMessageDelayed(frozenHandler2.obtainMessage(3, fpr), 100L);
        } else {
            synchronized (this.mLock) {
                realDoFrozenLocked(fpr);
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void realDoFrozenLocked(FrozenPackageRecord fpr) {
        ArrayList<ProcessRecord> normalProcs = fpr.getNormalProcesses();
        ArrayList<FrozenPackageRecord.NativeProcessRecord> nativeProcs = fpr.getNativeProcesses();
        Iterator<FrozenPackageRecord.NativeProcessRecord> it = nativeProcs.iterator();
        while (it.hasNext()) {
            FrozenPackageRecord.NativeProcessRecord proc = it.next();
            if (proc.pid > 1) {
                this.mFrozenHelper.frozen(proc.pid);
                VLog.d(TAG, "This native process " + proc.pid + " - " + proc.processName + " be freezed.");
            }
        }
        Iterator<ProcessRecord> it2 = normalProcs.iterator();
        while (it2.hasNext()) {
            ProcessRecord proc2 = it2.next();
            if (proc2.pid > 1) {
                proc2.setFrozenStatus(true);
                this.mFrozenHelper.frozen(proc2.pid);
                VLog.d(TAG, "This normal process " + proc2.pid + " - " + proc2.processName + " be freezed.");
            }
        }
        fpr.frozenTime = System.currentTimeMillis();
        fpr.frozenRealTime = SystemClock.elapsedRealtime();
        fpr.frozenUptime = SystemClock.uptimeMillis();
        fpr.status = 3;
        EventLog.writeEvent(getIntFieldFromEventTags("AM_APP_FROZEN"), Integer.valueOf(UserHandle.getUserId(fpr.uid)), Integer.valueOf(fpr.uid), fpr.packageName, fpr.frozenReason);
        VLog.d(TAG, "package " + fpr.packageName + " has been frozen in " + fpr.getSpendFrozenTime() + "ms");
        if (this.mWaitingUnfrozenPackages.contains(fpr)) {
            VLog.d(TAG, "now " + fpr.packageName + " go to unfrozen because waiting.");
            doUnfrozenLocked(fpr);
            return;
        }
        FrozenHandler frozenHandler = this.mHandler;
        frozenHandler.sendMessage(frozenHandler.obtainMessage(6, fpr));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finishFrozen(FrozenPackageRecord fpr) {
        fpr.getNativeProcesses();
        synchronized (this.mService) {
            Iterator<ProcessRecord> it = fpr.getNormalProcesses().iterator();
            while (it.hasNext()) {
                ProcessRecord processRecord = it.next();
                this.mService.skipCurrentReceiverLocked(processRecord);
                this.mService.skipPendingBroadcastLocked(processRecord.pid);
            }
        }
        VLog.d(TAG, "getPackageFrozenFlag flag: " + fpr.flag);
        if ((fpr.flag & 2) != 0) {
            VLog.d(TAG, "not remove notification!");
        }
        int gpsResult = 0;
        if ((fpr.flag & 32) != 0) {
            gpsResult = onFrozenPackageForGPS(fpr.packageName, fpr.uid, true);
        }
        int wakelockCount = 0;
        if ((fpr.flag & 1) != 0) {
            wakelockCount = removeWakelock(fpr.packageName, fpr.uid);
        }
        int sensorResult = 0;
        if ((fpr.flag & 8) != 0) {
            sensorResult = onFrozenPackageForSensor(fpr.packageName, fpr.uid, true);
        }
        int audioResult = 0;
        if ((fpr.flag & 16) != 0) {
            audioResult = onFrozenPackageForAudio(fpr.uid, true);
        }
        int alarmResult = 0;
        if ((fpr.flag & 128) != 0) {
            alarmResult = onFrozenPackageForAlarm(fpr.uid, fpr.packageName, true);
        }
        if ((fpr.flag & 64) != 0) {
            onFrozenPackageForNetwork(fpr.uid, fpr.packageName, true);
        }
        VLog.d(TAG, fpr.packageName + " finish frozen gpsResult=" + gpsResult + ",sensorResult=" + sensorResult + ",audioResult=" + audioResult + ",wakelockCount=" + wakelockCount + ",alarm: " + alarmResult);
        Intent intent = new Intent(ACTION_PACKAGE_FREEZE);
        intent.putExtra("android.intent.extra.PACKAGE_NAME", fpr.packageName);
        intent.putExtra("android.intent.extra.UID", fpr.uid);
        this.mContext.sendBroadcast(intent);
        vmr(fpr, true);
        FrozenNotifier.freeze(fpr.packageName, fpr.uid, fpr.userId);
        VivoBinderProxy.getInstance().reportFreezeApp(fpr.getNormalProcesses());
        FrozenStateObservable frozenStateObservable = this.mFrozenStateObservable;
        if (frozenStateObservable != null) {
            frozenStateObservable.updateAppFrozenRecord(fpr.uid, fpr.packageName, fpr.frozenFromWhich(fpr.frozenReason), 0);
        }
    }

    private void vmr(FrozenPackageRecord fpr, boolean isFrozen) {
        String echoPath;
        if (this.mVmrEnable) {
            ArrayList<Integer> pids = new ArrayList<>();
            ArrayList<FrozenPackageRecord.NativeProcessRecord> nativeProcs = fpr.getNativeProcesses();
            Iterator<FrozenPackageRecord.NativeProcessRecord> it = nativeProcs.iterator();
            while (it.hasNext()) {
                FrozenPackageRecord.NativeProcessRecord proc = it.next();
                if (proc.pid > 1) {
                    pids.add(Integer.valueOf(proc.pid));
                }
            }
            ArrayList<ProcessRecord> normalProcs = fpr.getNormalProcesses();
            Iterator<ProcessRecord> it2 = normalProcs.iterator();
            while (it2.hasNext()) {
                ProcessRecord proc2 = it2.next();
                if (proc2.pid > 1) {
                    pids.add(Integer.valueOf(proc2.pid));
                }
            }
            if (pids.size() < 1) {
                return;
            }
            if (isFrozen) {
                echoPath = " > /sys/kernel/mm/vmr/bg_task";
            } else {
                echoPath = " > /sys/kernel/mm/vmr/fg_task";
            }
            StringBuilder cmd = new StringBuilder();
            for (int i = 0; i < pids.size(); i++) {
                int pid = pids.get(i).intValue();
                cmd.append("echo ");
                cmd.append(pid);
                cmd.append(echoPath);
                if (i != pids.size() - 1) {
                    cmd.append(";");
                }
            }
            VLog.d(TAG, "run frozen vmrCmds [" + ((Object) cmd) + "],result = nodo");
        }
    }

    private int removeWakelock(String packageName, int uid) {
        try {
            PowerManager pm = (PowerManager) this.mService.mContext.getSystemService("power");
            return onFrozenPackage(pm, packageName, uid);
        } catch (Exception e) {
            VLog.w(TAG, "PowerManager onFrozenPackage " + packageName + " failed! " + e);
            return 0;
        }
    }

    private void removeNotification(String packageName) {
        try {
            NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            if (this.cancelAllNotificationsForFrozen != null) {
                this.cancelAllNotificationsForFrozen.invoke(notificationManager, packageName, 0);
            }
        } catch (Exception e) {
            VLog.w(TAG, "Cancel all notification for " + packageName + " failed cause: " + e);
        }
    }

    private String getProcNameByPid(int pid) {
        String result = null;
        BufferedReader reader = null;
        try {
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            try {
                String path = "/proc/" + pid + "/comm";
                File file = new File(path);
                if (file.exists()) {
                    reader = new BufferedReader(new FileReader(file));
                    result = reader.readLine();
                }
            } catch (Exception e2) {
                VLog.w(TAG, "getProcNameByPid failed! " + e2);
                if (reader != null) {
                    reader.close();
                }
            }
            if (reader != null) {
                reader.close();
            }
            return result;
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            throw th;
        }
    }

    public void dumpFrozenPackageLocked(PrintWriter pw) {
        pw.println("Plateform:");
        pw.println("    PlatformName: " + this.mPlatformName);
        pw.println("    PlatformAndroid: " + this.mPlatformAndroid);
        pw.println("    PlatformDisplay: " + this.mPlatformDisplay);
        pw.println("  ");
        pw.println("Configure:");
        pw.println("    EnableFunction: " + this.mEnableFunction);
        pw.println("    VmrEnable: " + this.mVmrEnable);
        pw.println("  ");
        pw.println("State:");
        pw.println("    Count: " + this.mFrozenPackages.size());
        pw.println("  ");
        if (this.mFrozenPackages.size() > 0) {
            pw.println("User " + UserHandle.myUserId() + " frozen packages:");
            int i = 0;
            for (String key : this.mFrozenPackages.keySet()) {
                i++;
                FrozenPackageRecord fpr = this.mFrozenPackages.get(key);
                pw.println(" #" + i + " " + fpr + " freeze at (" + fpr.getFrozenTime() + "), totalTime (" + fpr.getFrozenInterval() + ") " + fpr.frozenReason);
                ArrayList<ProcessRecord> procs = fpr.getNormalProcesses();
                Iterator<ProcessRecord> it = procs.iterator();
                while (it.hasNext()) {
                    ProcessRecord proc = it.next();
                    StringBuilder sb = new StringBuilder();
                    sb.append("    -> ");
                    sb.append(proc.toShortString());
                    sb.append(" curAdj:");
                    sb.append(proc.curAdj);
                    sb.append(proc.pid <= 0 ? " (killed)" : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    pw.println(sb.toString());
                }
                ArrayList<FrozenPackageRecord.NativeProcessRecord> nativeProcs = fpr.getNativeProcesses();
                if (nativeProcs != null && nativeProcs.size() > 0) {
                    Iterator<FrozenPackageRecord.NativeProcessRecord> it2 = nativeProcs.iterator();
                    while (it2.hasNext()) {
                        pw.println("    -> " + it2.next().toShortString() + " (child process)");
                    }
                }
            }
        }
    }

    private void initMethods() {
        try {
            this.onFrozenPackagePowerManager = PowerManager.class.getDeclaredMethod("onFrozenPackage", String.class, Integer.TYPE);
        } catch (Exception e) {
            VLog.d(TAG, "failed to find onFrozenPackagePowerManager method");
        }
        try {
            this.cancelAllNotificationsForFrozen = NotificationManager.class.getDeclaredMethod("cancelAllForFrozen", String.class, Integer.TYPE);
        } catch (Exception e2) {
            VLog.d(TAG, "failed to find cancelAllNotificationsForFrozen method");
        }
        try {
            this.getPendingJobsByUid = JobScheduler.class.getDeclaredMethod("getPendingJobsByUid", Integer.TYPE);
        } catch (Exception e3) {
            VLog.d(TAG, "failed to find getPendingJobsByUid method");
        }
        try {
            this.cancelJobByUid = JobScheduler.class.getDeclaredMethod("cancelByUid", Integer.TYPE, Integer.TYPE);
        } catch (Exception e4) {
            VLog.d(TAG, "failed to find cancelJobByUid method");
        }
        try {
            this.onFrozenPackageSensorManager = SensorManager.class.getDeclaredMethod("onFrozenPackage", String.class, Integer.TYPE, Boolean.TYPE);
        } catch (Exception e5) {
            VLog.d(TAG, "failed to find onFrozenPackageSensorManager method");
        }
        try {
            this.onFrozenPackageLocationManager = LocationManager.class.getDeclaredMethod("onFrozenPackage", String.class, Integer.TYPE, Boolean.TYPE);
        } catch (Exception e6) {
            VLog.d(TAG, "failed to find onFrozenPackageLocationManager method");
        }
        try {
            this.onFrozenPackageAudioManager = AudioManager.class.getDeclaredMethod("onFrozenPackage", Integer.TYPE, Boolean.TYPE);
        } catch (Exception e7) {
            VLog.d(TAG, "failed to find onFrozenPackageAudioManager method");
        }
        try {
            this.onFrozenPackageNetworkManager = INetworkPolicyManager.Stub.asInterface(ServiceManager.getService("netpolicy"));
        } catch (Exception e8) {
            VLog.d(TAG, "failed to find onFrozenPackageNetworkManagers method");
        }
        try {
            this.onFrozenPackageDisplayManager = DisplayManager.class.getDeclaredMethod("onFrozenPackage", String.class, Boolean.TYPE);
        } catch (Exception e9) {
            VLog.d(TAG, "failed to find onFrozenPackageDisplayManager method");
        }
        try {
            this.onFrozenPackageAlarmManager = AlarmManager.class.getDeclaredMethod("onFrozenPackage", Integer.TYPE, String.class, Boolean.TYPE);
        } catch (Exception e10) {
            VLog.d(TAG, "failed to find onFrozenPackageAlarmManager method");
        }
        try {
            this.onFrozenPackageNetworkManager = INetworkPolicyManager.Stub.asInterface(ServiceManager.getService("netpolicy"));
        } catch (Exception e11) {
            VLog.d(TAG, "failed to find onFrozenPackageNetworkManager method");
        }
    }

    private int onFrozenPackage(PowerManager pm, String pkgName, int uid) {
        Method method = this.onFrozenPackagePowerManager;
        if (method != null) {
            try {
                int result = ((Integer) method.invoke(pm, pkgName, Integer.valueOf(uid))).intValue();
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private int onFrozenPackageForGPS(String packageName, int uid, boolean isFrozen) {
        if (this.onFrozenPackageLocationManager != null) {
            try {
                LocationManager lm = (LocationManager) this.mService.mContext.getSystemService("location");
                int result = ((Integer) this.onFrozenPackageLocationManager.invoke(lm, packageName, Integer.valueOf(uid), Boolean.valueOf(isFrozen))).intValue();
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private int onFrozenPackageForSensor(String packageName, int uid, boolean isFrozen) {
        if (this.onFrozenPackageSensorManager != null) {
            try {
                SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
                int result = ((Integer) this.onFrozenPackageSensorManager.invoke(sensorManager, packageName, Integer.valueOf(uid), Boolean.valueOf(isFrozen))).intValue();
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private int onFrozenPackageForDisplayManager(String packageName, boolean isFrozen) {
        if (this.onFrozenPackageDisplayManager != null) {
            try {
                DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
                int result = ((Integer) this.onFrozenPackageDisplayManager.invoke(displayManager, packageName, Boolean.valueOf(isFrozen))).intValue();
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private int onFrozenPackageForAudio(final int uid, final boolean isFrozen) {
        this.mAudioResult = 0;
        this.mGetAudioResult = false;
        this.mAudioHandler.runWithScissors(new Runnable() { // from class: com.android.server.am.VivoFrozenPackageSupervisor.3
            @Override // java.lang.Runnable
            public void run() {
                if (VivoFrozenPackageSupervisor.this.onFrozenPackageAudioManager != null) {
                    try {
                        AudioManager audioManager = (AudioManager) VivoFrozenPackageSupervisor.this.mContext.getSystemService("audio");
                        long start = System.currentTimeMillis();
                        VivoFrozenPackageSupervisor.this.mAudioResult = ((Integer) VivoFrozenPackageSupervisor.this.onFrozenPackageAudioManager.invoke(audioManager, Integer.valueOf(uid), Boolean.valueOf(isFrozen))).intValue();
                        VivoFrozenPackageSupervisor.this.mGetAudioResult = true;
                        long cost = System.currentTimeMillis() - start;
                        VLog.i(VivoFrozenPackageSupervisor.TAG, "onFrozenPackageForAudio, cost = " + cost + " ms, uid = " + uid + ", isFrozen = " + isFrozen);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 5000L);
        if (!this.mGetAudioResult) {
            VLog.e(TAG, "onFrozenPackageForAudio, timeout! uid = " + uid + ", isFrozen = " + isFrozen);
        }
        return this.mAudioResult;
    }

    private int onFrozenPackageForNetwork(int uid, String packageName, boolean isFrozen) {
        if (this.onFrozenPackageNetworkManager == null) {
            this.onFrozenPackageNetworkManager = INetworkPolicyManager.Stub.asInterface(ServiceManager.getService("netpolicy"));
        }
        if (this.onFrozenPackageNetworkManager != null) {
            try {
                List<Integer> uids = new ArrayList<>();
                uids.add(Integer.valueOf(uid));
                this.onFrozenPackageNetworkManager.closeSockets(uids, (List) null);
                VLog.d(TAG, "onFrozenPackageNetworkManagers close socket " + packageName + ", uid = " + uid);
                return 0;
            } catch (Exception e) {
                VLog.d(TAG, "failed to find onFrozenPackageNetworkManagers method" + e);
                return 0;
            }
        }
        return 0;
    }

    private int onFrozenPackageForAlarm(int uid, String packageName, boolean isFrozen) {
        if (this.onFrozenPackageAlarmManager != null) {
            try {
                AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
                int result = ((Integer) this.onFrozenPackageAlarmManager.invoke(alarmManager, Integer.valueOf(uid), packageName, Boolean.valueOf(isFrozen))).intValue();
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private int getIntFieldFromEventTags(String fieldName) {
        try {
            Field field = EventLogTags.class.getField(fieldName);
            return field.getInt(EventLogTags.class);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private boolean hasVisibleActivities(ProcessRecord pr) {
        try {
            if (this.hasVisibleActivitiesMethod != null) {
                boolean result = ((Boolean) this.hasVisibleActivitiesMethod.invoke(pr, new Object[0])).booleanValue();
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String createKey(String packageName, int userId) {
        if (TextUtils.isEmpty(packageName)) {
            VLog.e(TAG, "packageName is empty!");
        }
        if (userId < 0) {
            userId = 0;
        }
        return createKeyInternal(packageName, userId);
    }

    private static String createKeyInternal(String packageName, int userId) {
        return packageName + "_" + userId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class FrozenHelper {
        private static final String CGROUP_FROZEN_PATH = "/dev/freezer/frozen/cgroup.procs";
        private static final String CGROUP_UNFROZEN_PATH = "/dev/freezer/unfrozen/cgroup.procs";
        private static final int SIGCONT = 18;
        private static final int SIGSTOP = 19;
        private boolean mEnable;
        private PrintWriter mFrozenWriter;
        private PrintWriter mUnFrozenWriter;

        private FrozenHelper() {
            this.mEnable = false;
            VLog.d(VivoFrozenPackageSupervisor.TAG, "FrozenHelper frozen ");
            if (SystemProperties.getBoolean("persist.sys.enable_cgroup_freezer", false)) {
                File frozenFile = new File(CGROUP_FROZEN_PATH);
                File unfrozenFile = new File(CGROUP_UNFROZEN_PATH);
                if (frozenFile.exists() && frozenFile.canWrite() && unfrozenFile.exists() && unfrozenFile.canWrite()) {
                    try {
                        this.mFrozenWriter = new PrintWriter(new FileOutputStream(CGROUP_FROZEN_PATH));
                        this.mUnFrozenWriter = new PrintWriter(new FileOutputStream(CGROUP_UNFROZEN_PATH));
                        this.mEnable = true;
                    } catch (FileNotFoundException e) {
                        VLog.d(VivoFrozenPackageSupervisor.TAG, "FrozenHelper error=" + e.toString());
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void frozen(int pid) {
            if (this.mEnable) {
                this.mFrozenWriter.write(String.valueOf(pid));
                this.mFrozenWriter.flush();
                return;
            }
            Process.sendSignal(pid, 19);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void unfrozen(int pid) {
            if (this.mEnable) {
                this.mUnFrozenWriter.write(String.valueOf(pid));
                this.mUnFrozenWriter.flush();
                return;
            }
            Process.sendSignal(pid, 18);
        }
    }

    /* loaded from: classes.dex */
    public final class FrozenStateObservable extends Observable {
        public FrozenStateObservable() {
            VLog.d(VivoFrozenPackageSupervisor.TAG, "FrozenStateObservable be created");
        }

        public void updateAppFrozenRecord(int uid, String pkgName, int caller, int failReason) {
            VLog.d(VivoFrozenPackageSupervisor.TAG, "updateAppFrozenRocord " + pkgName);
            setChanged();
            FrozenDataInfo info = new FrozenDataInfo(uid, pkgName, caller, 1);
            info.setFailReason(failReason);
            notifyObservers(info);
        }

        public void updateAppUnfrozenRecord(int uid, String pkgName, int caller, int unfrozenReason) {
            VLog.d(VivoFrozenPackageSupervisor.TAG, "updateAppUnfrozenRocord " + pkgName);
            setChanged();
            FrozenDataInfo info = new FrozenDataInfo(uid, pkgName, caller, 0);
            info.setUnfrozenReason(unfrozenReason);
            notifyObservers(info);
        }
    }
}