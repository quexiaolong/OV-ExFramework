package com.android.server.display.color.displayenhance;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityTaskManager;
import android.app.IActivityManager;
import android.app.IActivityTaskManager;
import android.app.IProcessObserver;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.IVivoWindowFocusListener;
import android.view.WindowManagerGlobal;
import com.android.server.FgThread;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.notification.VivoNotificationManagerServiceImpl;
import com.vivo.face.common.data.Constants;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.appmng.AppManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import vivo.contentcatcher.IActivityObserver;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class ApplicationPackageObserver {
    public static final int ACTIVITY_PAUSED = 0;
    public static final int ACTIVITY_RESUMED = 1;
    private static final int CUR_ENTER_SPLIT_MODE = 1;
    private static final int CUR_EXIT_SPLIT_MODE = 0;
    private static final String SPLIT_PANEL_MODE_CHANGE_URI = "in_multi_window";
    static final String TAG = "DisplayEnhanceAppPackageObserver";
    private Context mContext;
    private String mCurForegroundActivityName;
    private String mCurForegroundAppPackageName;
    private String mForegroundAppActivityObserver;
    private String mForegroundAppProcessObserver;
    private static boolean DBG = SystemProperties.getBoolean("persist.vivo.display.enhance.debug", false);
    private static ApplicationPackageObserver mAppObserver = null;
    private boolean mIsMultWindow = false;
    private ContentResolver mContentResolver = null;
    private final ArrayList<PackageActivityListener> mListeners = new ArrayList<>();
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.android.server.display.color.displayenhance.ApplicationPackageObserver.3
        public void onForegroundActivitiesChanged(int pid, int UID, boolean foregroundActivities) {
            long uid = (pid * 100000) + UID;
            PowerManager powerManager = (PowerManager) ApplicationPackageObserver.this.mContext.getSystemService("power");
            boolean isScreenOn = powerManager.isInteractive();
            String packageName = ApplicationPackageObserver.this.getPkgNameByPid(pid);
            if (ApplicationPackageObserver.DBG) {
                VSlog.d(ApplicationPackageObserver.TAG, "onForegroundActivitiesChanged: pid=" + pid + ", uid=" + uid + ", isScreenOn = " + isScreenOn + ", foregroundActivities=" + foregroundActivities + ", name=" + packageName);
            }
            if (isScreenOn && foregroundActivities) {
                if (ApplicationPackageObserver.this.isLauncher(packageName)) {
                    ApplicationPackageObserver.this.setForegroundAppPackageName(packageName);
                    ApplicationPackageObserver.this.foregroundAppUpdate(packageName);
                }
                ApplicationPackageObserver.this.mForegroundAppProcessObserver = packageName;
            } else if (isScreenOn && !foregroundActivities && ApplicationPackageObserver.this.isLauncher(packageName) && ApplicationPackageObserver.this.mForegroundAppActivityObserver != null) {
                ApplicationPackageObserver applicationPackageObserver = ApplicationPackageObserver.this;
                applicationPackageObserver.setForegroundAppPackageName(applicationPackageObserver.mForegroundAppActivityObserver);
                ApplicationPackageObserver applicationPackageObserver2 = ApplicationPackageObserver.this;
                applicationPackageObserver2.foregroundAppUpdate(applicationPackageObserver2.mForegroundAppActivityObserver);
            }
        }

        public void onProcessDied(int pid, int UID) {
            long uid = (pid * 100000) + UID;
            if (ApplicationPackageObserver.DBG) {
                VSlog.d(ApplicationPackageObserver.TAG, "onProcessDied: pid=" + pid + ", uid=" + uid);
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    };
    IActivityObserver mActivityObserver = new IActivityObserver.Stub() { // from class: com.android.server.display.color.displayenhance.ApplicationPackageObserver.4
        public void activityResumed(int pid, int uid, ComponentName componentName) throws RemoteException {
            long j = (pid * 100000) + uid;
            if (componentName != null) {
                String resumedActivityName = componentName.getClassName();
                if (ApplicationPackageObserver.DBG) {
                    VSlog.d(ApplicationPackageObserver.TAG, "activityResumed: pid=" + pid + ", uid=" + uid + ", resumedActivityName=" + resumedActivityName);
                }
                ApplicationPackageObserver.this.foregroundActivityUpdate(resumedActivityName, 1);
                ApplicationPackageObserver.this.mCurForegroundActivityName = resumedActivityName;
            }
        }

        public void activityPaused(int pid, int uid, ComponentName componentName) throws RemoteException {
            long j = (pid * 100000) + uid;
            if (componentName != null) {
                String pausedActivityName = componentName.getClassName();
                if (ApplicationPackageObserver.DBG) {
                    VSlog.d(ApplicationPackageObserver.TAG, "activityPaused: pid=" + pid + ", uid=" + uid + ", pausedActivityName=" + pausedActivityName);
                }
                ApplicationPackageObserver.this.foregroundActivityUpdate(pausedActivityName, 0);
            }
        }
    };
    private Handler mHandler = new Handler(FgThread.get().getLooper());
    private IActivityTaskManager mActivityTaskManager = ActivityTaskManager.getService();

    private ApplicationPackageObserver(Context context) {
        this.mContext = context;
        registerForegroundAppUpdater();
        registerProcessObserver();
        lambda$registerActivityObserver$0$ApplicationPackageObserver(context);
        registerVivoWindowFocusListener();
        registerSplitScreenModeObserver();
    }

    public static ApplicationPackageObserver getInstance(Context context) {
        if (mAppObserver == null) {
            synchronized (ApplicationPackageObserver.class) {
                if (mAppObserver == null) {
                    mAppObserver = new ApplicationPackageObserver(context);
                }
            }
        }
        return mAppObserver;
    }

    public void registerListener(PackageActivityListener listener) {
        synchronized (this.mListeners) {
            if (!this.mListeners.contains(listener)) {
                this.mListeners.add(listener);
                VSlog.d(TAG, "registerListener:  " + listener);
            }
        }
    }

    public void unregisterListener(PackageActivityListener listener) {
        synchronized (this.mListeners) {
            if (this.mListeners.contains(listener)) {
                this.mListeners.remove(listener);
                VSlog.d(TAG, "unregisterListener:  " + listener);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setForegroundAppPackageName(String packageName) {
        this.mCurForegroundAppPackageName = packageName;
    }

    public String getForegroundAppPackageName() {
        return this.mCurForegroundAppPackageName;
    }

    public String getForegroundActivityName() {
        return this.mCurForegroundActivityName;
    }

    public boolean isLauncher(String packageName) {
        if ("com.bbk.launcher2".equals(packageName) || VivoNotificationManagerServiceImpl.PKG_LAUNCHER.equals(packageName)) {
            return true;
        }
        return false;
    }

    private void registerVivoWindowFocusListener() {
        try {
            WindowManagerGlobal.getWindowManagerService().registerVivoWindowFocusListener(new IVivoWindowFocusListener.Stub() { // from class: com.android.server.display.color.displayenhance.ApplicationPackageObserver.1
                public void onWindowFocusChanged(int focusSide) {
                    if (ApplicationPackageObserver.this.mIsMultWindow) {
                        ApplicationPackageObserver.this.updateForegroundApp();
                    }
                }
            });
        } catch (RemoteException e) {
            VSlog.e(TAG, "Failed registering window focus listener:" + e);
        }
    }

    private void registerSplitScreenModeObserver() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mContentResolver = contentResolver;
        if (contentResolver != null) {
            SplitScreenModeObserver splitScreenModeObserver = new SplitScreenModeObserver(this.mHandler);
            this.mContentResolver.registerContentObserver(Settings.System.getUriFor(SPLIT_PANEL_MODE_CHANGE_URI), true, splitScreenModeObserver);
            return;
        }
        VSlog.e(TAG, "ContentResolver is null.");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SplitScreenModeObserver extends ContentObserver {
        public SplitScreenModeObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            try {
                boolean isSplitMode = Settings.System.getInt(ApplicationPackageObserver.this.mContentResolver, ApplicationPackageObserver.SPLIT_PANEL_MODE_CHANGE_URI, 0) == 1;
                ApplicationPackageObserver.this.mIsMultWindow = isSplitMode;
            } catch (Exception e) {
                VSlog.e(ApplicationPackageObserver.TAG, "Split Mode Observer error cause:" + e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class TaskStackListenerImpl extends TaskStackListener {
        TaskStackListenerImpl() {
        }

        public void onTaskStackChanged() throws RemoteException {
            ApplicationPackageObserver.this.updateForegroundApp();
        }

        public void onActivityPinned(String packageName, int userId, int taskId, int stackId) throws RemoteException {
        }

        public void onActivityUnpinned() throws RemoteException {
        }
    }

    private void registerForegroundAppUpdater() {
        try {
            this.mActivityTaskManager.registerTaskStackListener(new TaskStackListenerImpl());
            updateForegroundApp();
        } catch (RemoteException e) {
            VSlog.e(TAG, "Failed to register foreground app updater: " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void foregroundActivityUpdate(String name, int activated) {
        synchronized (this.mListeners) {
            for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                PackageActivityListener listener = this.mListeners.get(i);
                listener.onForegroundActivityChanged(name, activated);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void foregroundAppUpdate(String name) {
        synchronized (this.mListeners) {
            for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                PackageActivityListener listener = this.mListeners.get(i);
                listener.onForegroundPackageChanged(name);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForegroundApp() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.color.displayenhance.ApplicationPackageObserver.2
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ActivityManager.StackInfo info = ApplicationPackageObserver.this.mActivityTaskManager.getFocusedStackInfo();
                    if (info != null && info.topActivity != null) {
                        String packageName = info.topActivity.getPackageName();
                        if (ApplicationPackageObserver.this.mForegroundAppActivityObserver == null || !ApplicationPackageObserver.this.mForegroundAppActivityObserver.equals(packageName)) {
                            if (ApplicationPackageObserver.DBG) {
                                VSlog.d(ApplicationPackageObserver.TAG, "Updating foreground app: packageName=" + packageName);
                            }
                            ApplicationPackageObserver.this.mForegroundAppActivityObserver = packageName;
                            ApplicationPackageObserver.this.setForegroundAppPackageName(ApplicationPackageObserver.this.mForegroundAppActivityObserver);
                            ApplicationPackageObserver.this.foregroundAppUpdate(packageName);
                        }
                    }
                } catch (RemoteException e) {
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getPkgNameByPid(int pid) {
        ProcessInfo info = AppManager.getInstance().getProcessInfo(pid);
        if (info != null) {
            return info.mPkgName;
        }
        return String.valueOf(pid);
    }

    private boolean registerProcessObserver() {
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            if (am == null) {
                return false;
            }
            am.registerProcessObserver(this.mProcessObserver);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            VSlog.e(TAG, "onProcessDied: registerProcessObserver!");
            return false;
        }
    }

    private static String getAppName(int pid, int uid, Context mContext) {
        String processName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        String packageName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        ActivityManager.RunningAppProcessInfo processInfo = null;
        try {
            ActivityManager am = (ActivityManager) mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
            List<ActivityManager.RunningAppProcessInfo> runTasks = am.getRunningAppProcesses();
            if (runTasks != null) {
                Iterator<ActivityManager.RunningAppProcessInfo> it = runTasks.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    ActivityManager.RunningAppProcessInfo runAppInfo = it.next();
                    if (pid == runAppInfo.pid && uid == runAppInfo.uid) {
                        processName = runAppInfo.processName;
                        processInfo = runAppInfo;
                        break;
                    }
                }
            }
            if (processInfo != null) {
                String[] pkgList = processInfo.pkgList;
                if (pkgList.length == 1) {
                    packageName = pkgList[0];
                } else if (pkgList.length == 0) {
                    if (DBG) {
                        VSlog.d(TAG, "processName = " + processName + ", the packageName is null");
                    }
                } else if (pkgList.length > 1) {
                    for (int i = 0; i < pkgList.length; i++) {
                        if (DBG) {
                            VSlog.d(TAG, "pkgList[" + i + "] = " + pkgList[i]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (DBG) {
            VSlog.d(TAG, "processName = " + processName + ", the packageName is " + packageName);
        }
        return packageName;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: registerActivityObserver */
    public void lambda$registerActivityObserver$0$ApplicationPackageObserver(final Context mContext) {
        try {
            ActivityManager.getService().registerActivityObserver(this.mActivityObserver);
        } catch (Exception e) {
            VSlog.d(TAG, "Failure regiester observer", e);
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.display.color.displayenhance.-$$Lambda$ApplicationPackageObserver$YrrwvIx8yjT9j_dXq5H1ycSUn1A
                @Override // java.lang.Runnable
                public final void run() {
                    ApplicationPackageObserver.this.lambda$registerActivityObserver$0$ApplicationPackageObserver(mContext);
                }
            }, VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
        }
    }
}