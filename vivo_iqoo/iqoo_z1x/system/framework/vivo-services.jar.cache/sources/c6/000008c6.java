package com.vivo.services.vivo4dgamevibrator;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.server.am.firewall.VivoFirewall;
import com.vivo.face.common.data.Constants;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class Vivo4DGameVibratorMonitor {
    private static final int DEFAULT_GAME_MODE_SWITCH = 1;
    private static final String ENABLED_GAME_4D_SHOCK = "game_4d_shock_enabled";
    private static final String KEY_GAME_DO_NOT_DISTURB = "game_do_not_disturb";
    private static final int MSG_FOREGROUND = 1;
    private static final int MSG_PROCESS_DIED = 2;
    private static final int MSG_SETCLICK_MONITOR_APP_INFOR = 4;
    private static final int MSG_UPDATE_SETTING_INFOR = 3;
    private static final String SKILL_SHOCK_STATE = "skill_shock_state";
    public static final String TAG = "gamevibrator";
    private int doubleLra;
    private final IActivityManager mActivityManager;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final MonitorHandler mMonitorHandler;
    private final HandlerThread mMonitorHandlerThrd;
    private final ProcessObserver mProcessObserver;
    private final SettingsValueChangeContentObserver mSettingsValueChangeContentObserver;
    private final HashMap<String, Integer> mSupport4DGameVibPkgsTypeMap;
    private final Vivo4DGameVibratorService mVivo4DGameVibratorService;
    private int mClickMonitorAppPid = -1;
    private int mClickMonitorAppUid = -1;
    private final HashMap<String, Integer> mGamePidCacheMap = new HashMap<>(5);
    private String enableShockPkgsCache = null;
    private int userCache = 0;
    private String[] mSupportVibrator4DPkgs = null;
    private final Intent mClickMonitorAppServiceIntent = createClickMonitorAppServiceIntent();

    public Vivo4DGameVibratorMonitor(Context context, Vivo4DGameVibratorService vivo4DGameVibratorService) {
        this.mContext = context;
        this.mVivo4DGameVibratorService = vivo4DGameVibratorService;
        this.mSupport4DGameVibPkgsTypeMap = vivo4DGameVibratorService.m5get4DGameVibSupportPkgs();
        HandlerThread handlerThread = new HandlerThread("GameVibratorMonitorThrd");
        this.mMonitorHandlerThrd = handlerThread;
        handlerThread.start();
        this.mMonitorHandler = new MonitorHandler(this.mMonitorHandlerThrd.getLooper());
        this.doubleLra = SystemProperties.getInt("persist.sys.vivo.support.double.lra", 0);
        this.mActivityManager = ActivityManagerNative.getDefault();
        ProcessObserver processObserver = new ProcessObserver(this.mMonitorHandler);
        this.mProcessObserver = processObserver;
        try {
            this.mActivityManager.registerProcessObserver(processObserver);
        } catch (Exception e) {
            VSlog.e(TAG, "register process observer failed", e);
        }
        this.mContentResolver = this.mContext.getContentResolver();
        this.mSettingsValueChangeContentObserver = new SettingsValueChangeContentObserver(this.mMonitorHandler);
        registerObserver();
        updateSettings();
        UserChangeReceiver userChangeReceiver = new UserChangeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(userChangeReceiver, filter);
    }

    public void registerObserver() {
        VSlog.d(TAG, "registerObserver");
        this.mContentResolver.registerContentObserver(Settings.System.getUriFor(ENABLED_GAME_4D_SHOCK), true, this.mSettingsValueChangeContentObserver, -1);
        this.mContentResolver.registerContentObserver(Settings.System.getUriFor(SKILL_SHOCK_STATE), true, this.mSettingsValueChangeContentObserver, -1);
        this.mContentResolver.registerContentObserver(Settings.System.getUriFor("game_do_not_disturb"), true, this.mSettingsValueChangeContentObserver, -1);
    }

    public void unRegisterObserver() {
        VSlog.d(TAG, "unRegisterObserver");
        this.mContentResolver.unregisterContentObserver(this.mSettingsValueChangeContentObserver);
    }

    public void setClickMonitorAppInfor(int pid, int uid) {
        sendMsg(4, pid, uid);
    }

    /* loaded from: classes.dex */
    private class ProcessObserver extends IProcessObserver.Stub {
        private final Handler mHandler;

        public ProcessObserver(Handler handler) {
            this.mHandler = handler;
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean hasForegroundActivities) {
            VSlog.d(Vivo4DGameVibratorMonitor.TAG, "onForegroundActivitiesChanged pid:" + pid + ", uid:" + uid + ", hasFore:" + hasForegroundActivities);
            if (hasForegroundActivities) {
                Vivo4DGameVibratorMonitor.this.mVivo4DGameVibratorService.setPkgName(null);
                Vivo4DGameVibratorMonitor.this.sendMsg(1, pid, uid);
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }

        public void onProcessDied(int pid, int uid) {
            VSlog.d(Vivo4DGameVibratorMonitor.TAG, "onProcessDied pid:" + pid + ", uid:" + uid);
            Vivo4DGameVibratorMonitor.this.sendMsg(2, pid, -1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendMsg(int what, int arg1, int arg2) {
        sendMsg(this.mMonitorHandler, what, arg1, arg2);
    }

    private void sendMsg(Handler handler, int what, int arg1, int arg2) {
        if (handler != null) {
            Message message = handler.obtainMessage();
            message.what = what;
            message.arg1 = arg1;
            message.arg2 = arg2;
            handler.sendMessage(message);
        }
    }

    private Intent createClickMonitorAppServiceIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.vivo.vibrator4d", "com.vivo.vibrator4d.Vibrator4DService"));
        intent.setAction("action_start");
        return intent;
    }

    private void stopClickMonitorAppService() {
        VSlog.d(TAG, "stop click monitor app service");
        this.mContext.stopService(this.mClickMonitorAppServiceIntent);
    }

    private void clickMonitorAppServiceStartOrStop(boolean start) {
        VSlog.d(TAG, "start or stop click monitor app service, start: " + start);
        this.mClickMonitorAppServiceIntent.setAction(start ? "action_start" : "action_stop");
        this.mContext.startService(this.mClickMonitorAppServiceIntent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MonitorHandler extends Handler {
        MonitorHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
            if (i == 1) {
                Vivo4DGameVibratorMonitor.this.foregroundChange(msg.arg1, msg.arg2);
            } else if (i == 2) {
                Vivo4DGameVibratorMonitor.this.processDied(msg.arg1);
            } else if (i == 3) {
                Vivo4DGameVibratorMonitor.this.settingChange();
            } else if (i == 4) {
                Vivo4DGameVibratorMonitor.this.mClickMonitorAppPid = msg.arg1;
                Vivo4DGameVibratorMonitor.this.mClickMonitorAppUid = msg.arg2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void settingChange() {
        updateSettings();
        updateSupportGamePidCache();
        if (this.mGamePidCacheMap.size() > 0) {
            clickMonitorAppServiceStartOrStop(true);
        } else {
            clickMonitorAppServiceStartOrStop(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processDied(int pid) {
        if (this.mClickMonitorAppPid == pid) {
            if (this.mGamePidCacheMap.size() > 0) {
                VSlog.d(TAG, "click monitor service app died, but game is running, start is again");
                clickMonitorAppServiceStartOrStop(true);
                return;
            }
            return;
        }
        String gamePkg = getGamePkg(pid);
        VSlog.d(TAG, "game pkg: " + gamePkg);
        if (gamePkg != null) {
            this.mGamePidCacheMap.remove(gamePkg);
            boolean isClickMonitorAppRunning = isPidRunning(this.mContext, this.mClickMonitorAppPid);
            VSlog.d(TAG, "isClickMonitorAppRunning: " + isClickMonitorAppRunning);
            if (isClickMonitorAppRunning) {
                clickMonitorAppServiceStartOrStop(false);
            }
        }
    }

    private String getGamePkg(int pid) {
        for (Map.Entry<String, Integer> entry : this.mGamePidCacheMap.entrySet()) {
            String pkg = entry.getKey();
            String pkg2 = pkg;
            Integer objPid = entry.getValue();
            if (objPid != null && objPid.intValue() == pid) {
                return pkg2;
            }
        }
        return null;
    }

    private boolean isPkgSupportAudioSolution(String pkg) {
        HashMap<String, Integer> hashMap;
        if (pkg == null || pkg.length() <= 0 || (hashMap = this.mSupport4DGameVibPkgsTypeMap) == null || hashMap.size() <= 0) {
            return false;
        }
        Integer type = this.mSupport4DGameVibPkgsTypeMap.get(pkg);
        if (type != null && (1 == type.intValue() || 4 == type.intValue())) {
            return true;
        }
        String projectName = SystemProperties.get("ro.vivo.product.model");
        if (!Arrays.asList(Vivo4DGameVibratorService.SUPPORT_SKILL_VIBRATOR_PROJECT).contains(projectName) || !Arrays.asList(Vivo4DGameVibratorService.SUPPORT_SKILL_VIBRATOR_GAMES).contains(pkg)) {
            return false;
        }
        String skillShockPkgs = Settings.System.getStringForUser(this.mContext.getContentResolver(), SKILL_SHOCK_STATE, ActivityManager.getCurrentUser());
        if (this.doubleLra != 1 || TextUtils.isEmpty(skillShockPkgs)) {
            if (this.doubleLra != 0) {
                return false;
            }
            return true;
        }
        String[] split = skillShockPkgs.split(":");
        for (String s : split) {
            if (s.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void foregroundChange(int pid, int uid) {
        try {
            String pkgName = this.mContext.getPackageManager().getNameForUid(uid);
            if (isSupportGame(pkgName)) {
                if (isPkgSupportAudioSolution(pkgName)) {
                    VSlog.d(TAG, "start click monitor app service, foreground game pkg: " + pkgName + " ,pid: " + pid);
                    this.mVivo4DGameVibratorService.setPkgName(pkgName);
                    clickMonitorAppServiceStartOrStop(true);
                    this.mGamePidCacheMap.put(pkgName, Integer.valueOf(pid));
                } else {
                    VSlog.d(TAG, "pkg: " + pkgName + " not support audio solution");
                }
            }
        } catch (Exception e) {
            VSlog.e(TAG, "start click monitor app failed", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SettingsValueChangeContentObserver extends ContentObserver {
        private SettingsValueChangeContentObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VSlog.d(Vivo4DGameVibratorMonitor.TAG, "setting value changed");
            Vivo4DGameVibratorMonitor.this.sendMsg(3, -1, -1);
        }
    }

    private void updateSettings() {
        int currentUser = ActivityManager.getCurrentUser();
        VSlog.d(TAG, "currentUser = " + currentUser + " userCache = " + this.userCache);
        String enableShockPkgs = Settings.System.getStringForUser(this.mContext.getContentResolver(), ENABLED_GAME_4D_SHOCK, currentUser);
        if (currentUser != this.userCache) {
            if (currentUser != 0) {
                Settings.System.putString(this.mContext.getContentResolver(), ENABLED_GAME_4D_SHOCK, null);
            } else {
                Settings.System.putString(this.mContext.getContentResolver(), ENABLED_GAME_4D_SHOCK, this.enableShockPkgsCache);
            }
            this.userCache = currentUser;
        }
        if (currentUser == 0) {
            this.enableShockPkgsCache = enableShockPkgs;
        }
        int gameMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), "game_do_not_disturb", 1, currentUser);
        VSlog.d(TAG, "updateSettings gameMode = " + gameMode);
        if (gameMode == 1) {
            VSlog.d(TAG, "pkgs: " + enableShockPkgs);
            if (!TextUtils.isEmpty(enableShockPkgs)) {
                this.mSupportVibrator4DPkgs = enableShockPkgs.split(":");
                StringBuilder sb = new StringBuilder();
                sb.append("support games: ");
                String[] strArr = this.mSupportVibrator4DPkgs;
                sb.append(strArr == null ? 0 : strArr.length);
                VSlog.d(TAG, sb.toString());
                return;
            }
            this.mSupportVibrator4DPkgs = null;
            return;
        }
        this.mSupportVibrator4DPkgs = null;
    }

    private boolean isSupportGame(String pkg) {
        String[] strArr = this.mSupportVibrator4DPkgs;
        if (strArr == null) {
            return false;
        }
        for (String s : strArr) {
            if (pkg != null && pkg.equals(s)) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    private String getPkgNameFromPid(Context context, int pid) {
        ActivityManager am;
        List<ActivityManager.RunningAppProcessInfo> list;
        if (context != null && (am = (ActivityManager) context.getSystemService(VivoFirewall.TYPE_ACTIVITY)) != null && (list = am.getRunningAppProcesses()) != null) {
            for (ActivityManager.RunningAppProcessInfo info : list) {
                if (info.pid == pid) {
                    return info.processName;
                }
            }
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    @Deprecated
    public String getTopAppPackageName(Context context) {
        if (context == null) {
            return null;
        }
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(VivoFirewall.TYPE_ACTIVITY);
        try {
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = mActivityManager.getRunningTasks(1);
            if (runningTaskInfos == null || runningTaskInfos.get(0) == null) {
                return null;
            }
            ComponentName topActivity = runningTaskInfos.get(0).topActivity;
            return topActivity.getPackageName();
        } catch (SecurityException e) {
            VSlog.i(TAG, "getRunningTask fail");
            return null;
        }
    }

    private void updateSupportGamePidCache() {
        ActivityManager am;
        List<ActivityManager.RunningAppProcessInfo> list;
        String[] strArr;
        this.mGamePidCacheMap.clear();
        String[] strArr2 = this.mSupportVibrator4DPkgs;
        if (strArr2 != null && strArr2.length > 0 && this.mSupport4DGameVibPkgsTypeMap != null && (am = (ActivityManager) this.mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY)) != null && (list = am.getRunningAppProcesses()) != null) {
            for (ActivityManager.RunningAppProcessInfo info : list) {
                for (String tempPkg : this.mSupportVibrator4DPkgs) {
                    if (info.processName != null && info.processName.equals(tempPkg) && isPkgSupportAudioSolution(tempPkg)) {
                        VSlog.i(TAG, "support game running, pkg: " + tempPkg + " , pid: " + info.pid);
                        this.mGamePidCacheMap.put(tempPkg, Integer.valueOf(info.pid));
                    }
                }
            }
        }
    }

    @Deprecated
    private boolean isThereAtLeastOnePkgRunning(Context context, String[] pkgs) {
        ActivityManager am;
        List<ActivityManager.RunningAppProcessInfo> list;
        boolean running = false;
        if (context != null && pkgs != null && pkgs.length > 0 && (am = (ActivityManager) context.getSystemService(VivoFirewall.TYPE_ACTIVITY)) != null && (list = am.getRunningAppProcesses()) != null) {
            for (ActivityManager.RunningAppProcessInfo info : list) {
                int length = pkgs.length;
                int i = 0;
                while (true) {
                    if (i < length) {
                        String tempPkg = pkgs[i];
                        if (info.processName == null || !info.processName.equals(tempPkg)) {
                            i++;
                        } else {
                            VSlog.d(TAG, "running pkg process name: " + info.processName + " ,param pkg: " + tempPkg);
                            running = true;
                            break;
                        }
                    }
                }
            }
        }
        return running;
    }

    private boolean isPidRunning(Context context, int pid) {
        ActivityManager am;
        List<ActivityManager.RunningAppProcessInfo> list;
        if (context == null || (am = (ActivityManager) context.getSystemService(VivoFirewall.TYPE_ACTIVITY)) == null || (list = am.getRunningAppProcesses()) == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo info : list) {
            if (pid == info.pid) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    private int getPidFromAppName(Context context, String pkgName) {
        ActivityManager am;
        List<ActivityManager.RunningAppProcessInfo> list;
        if (pkgName != null && context != null && (am = (ActivityManager) context.getSystemService(VivoFirewall.TYPE_ACTIVITY)) != null && (list = am.getRunningAppProcesses()) != null) {
            for (ActivityManager.RunningAppProcessInfo info : list) {
                if (pkgName.equals(info.processName)) {
                    return info.pid;
                }
            }
        }
        return -1;
    }

    /* loaded from: classes.dex */
    private class UserChangeReceiver extends BroadcastReceiver {
        private UserChangeReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VSlog.d(Vivo4DGameVibratorMonitor.TAG, "userChange receive = " + action);
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                Vivo4DGameVibratorMonitor.this.sendMsg(3, -1, -1);
            }
        }
    }
}