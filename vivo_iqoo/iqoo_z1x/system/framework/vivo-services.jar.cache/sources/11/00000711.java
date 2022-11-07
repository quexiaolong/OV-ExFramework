package com.vivo.services.rms.cgrp;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.FtFeature;
import com.android.internal.os.BackgroundThread;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.GameOptManager;
import com.vivo.services.rms.Platform;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.appmng.AppManager;
import com.vivo.services.rms.util.JniTool;
import com.vivo.services.superresolution.Constant;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class CgrpUtils {
    private static final String KEY_FEATURE_ENABLE = "key_feature_enable";
    private static final String KEY_GAME_SCENE_OPT_ENABLE = "key_game_scene_opt_enable";
    private static final String KEY_HOT_START_DELAYED_ENABLE = "key_hot_start_delayed_enable";
    private static final String KEY_HOT_START_FORBID_DELAYED = "key_hot_start_forbid_delayed";
    private static final String KEY_IGNORE_FIFO_IN_GAME = "key_ignore_fifo_in_game";
    private static final String KEY_MAX_CG_FG_RECENT_TIME = "key_max_cg_fg_recent_time";
    private static final String KEY_MOVE_BG_LITTLECORE = "key_move_bg_littlecore";
    private static final String KEY_SET_TID_GROUP_POLICY = "key_set_tid_group_policy";
    private static final String KEY_SYSTEM_BG_LIST = "key_system_bg_list";
    private static final String KEY_SYSTEM_FG_LIST = "key_system_fg_list";
    private static final String KEY_SYSTEM_SERVER_BG_IN_GAMESCENE = "key_system_server_bg_in_game_scene";
    private static final String KEY_TOP_SCHED_APP_LIST = "key_top_sched_app_list";
    private static final String KEY_TOP_SLEEPING_LIST = "key_top_sleeping_list";
    private static final String KEY_TOP_USED_APP_LIST = "key_top_app_list";
    static final String TAG = "CgrpController";
    private static RealtimeBlurObserver sRealtimeBlurObserver;
    private static final List<String> SYSTEM_FG_LIST = new ArrayList();
    private static final List<String> SYSTEM_BG_LIST = new ArrayList();
    private static final List<String> TOP_SLEEPING_LIST = new ArrayList();
    private static final List<String> TOP_USED_APP_LIST = new ArrayList();
    private static final List<String> TOP_SCHED_APP_LIST = new ArrayList();
    private static final List<String> HOT_START_FORBID_DELAYED_LIST = new ArrayList();
    private static final List<String> SET_TID_GROUP_POLICY_LIST = new ArrayList();
    private static final boolean PID_GROUP_POLICY_ENABLE = SystemProperties.getBoolean("persist.sys.cgroup.pid_group_policy", true);
    private static int sMaxCgFgRecentTime = 30000;
    private static boolean sGameSceneOptEnabled = SystemProperties.getBoolean("persist.sys.cgroup.game_opt", Platform.isTwoBigcoreDevice());
    private static boolean sSystemServerBgInGameScene = SystemProperties.getBoolean("persist.sys.cgroup.ss_bg_in_game", false);
    private static boolean sHotStartDelayedEnable = SystemProperties.getBoolean("persist.sys.cgroup.hot_start_delayed", true);
    private static boolean sPlatformSupported = false;
    private static boolean sFeatureEnabled = false;
    private static boolean sMoveBg2LittleCore = true;
    private static boolean sIgnoreFifoInGame = true;
    private static boolean sSupportRtblur = false;
    private static String sBlurSwitchName = "enhanced_dynamic_effects";
    static boolean DEBUG = false;

    static {
        TOP_USED_APP_LIST.add(Constant.APP_WEIXIN);
        TOP_SCHED_APP_LIST.add("com.vivo.systemblur.server");
        TOP_SLEEPING_LIST.add("com.vivo.wallet");
        HOT_START_FORBID_DELAYED_LIST.add("com.android.camera");
        SET_TID_GROUP_POLICY_LIST.add("*");
        SYSTEM_FG_LIST.add("com.vivo.voicewakeup");
        SYSTEM_FG_LIST.add("com.google.android.providers.media.module");
        SYSTEM_FG_LIST.add("com.android.providers.media.module");
    }

    public static void initialize() {
        boolean initCgrpConfigs = initCgrpConfigs();
        sPlatformSupported = initCgrpConfigs;
        boolean z = false;
        if (initCgrpConfigs && SystemProperties.getBoolean("persist.sys.enable_cgroup", false)) {
            z = true;
        }
        sFeatureEnabled = z;
        VSlog.i(TAG, "initialize isSupported=" + sPlatformSupported + " cluster=" + Platform.getCpuCluster());
    }

    public static void initRealtimeBlurObserver(Context context) {
        sSupportRtblur = FtFeature.isFeatureSupport("vivo.software.rtblur");
        VSlog.i(TAG, "initialize isSupportRtblur=" + sSupportRtblur);
        if (isSupportRealtimeBlurSwitch(context)) {
            sBlurSwitchName = "realtime_blur_state";
        }
        RealtimeBlurObserver realtimeBlurObserver = new RealtimeBlurObserver(BackgroundThread.getHandler());
        sRealtimeBlurObserver = realtimeBlurObserver;
        realtimeBlurObserver.observe(context);
    }

    public static boolean initCgrpConfigs() {
        boolean r2;
        boolean r1;
        boolean r0;
        boolean r3;
        boolean r4;
        int cluster = Platform.getCpuCluster();
        if (cluster == -1) {
            return false;
        }
        File file = new File("/dev/cpuset/l-background");
        if (!file.exists()) {
            return false;
        }
        boolean r5 = false;
        boolean r6 = false;
        boolean r7 = false;
        if (cluster == 0) {
            return false;
        }
        if (cluster == 1) {
            boolean r02 = writeCpusetConfigs("/dev/cpuset/l-background/cpus", "2-3");
            r1 = writeCpusetConfigs("/dev/cpuset/h-background/cpus", "0-4");
            boolean r22 = writeCpusetConfigs("/dev/cpuset/l-foreground/cpus", "0-5");
            boolean r32 = writeCpusetConfigs("/dev/cpuset/foreground/cpus", "0-7");
            r4 = writeCpusetConfigs("/dev/cpuset/restricted/cpus", "0-7");
            r5 = writeCpusetConfigs("/dev/cpuset/background/cpus", "0-3");
            r6 = writeCpusetConfigs("/dev/cpuset/top-app/cpus", "0-7");
            r7 = writeCpusetConfigs("/dev/cpuset/system-background/cpus", "0-3");
            r3 = r32;
            r0 = r22;
            r2 = r02;
        } else if (cluster == 2) {
            boolean r03 = writeCpusetConfigs("/dev/cpuset/l-background/cpus", "2-4");
            r1 = writeCpusetConfigs("/dev/cpuset/h-background/cpus", "0-5");
            boolean r23 = writeCpusetConfigs("/dev/cpuset/l-foreground/cpus", "0-6");
            boolean r33 = writeCpusetConfigs("/dev/cpuset/foreground/cpus", "0-7");
            r4 = writeCpusetConfigs("/dev/cpuset/restricted/cpus", "0-7");
            r5 = writeCpusetConfigs("/dev/cpuset/background/cpus", "0-3");
            r6 = writeCpusetConfigs("/dev/cpuset/top-app/cpus", "0-7");
            r7 = writeCpusetConfigs("/dev/cpuset/system-background/cpus", "0-3");
            r3 = r33;
            r0 = r23;
            r2 = r03;
        } else if (cluster == 3) {
            boolean r04 = writeCpusetConfigs("/dev/cpuset/l-background/cpus", "4-5");
            r1 = writeCpusetConfigs("/dev/cpuset/h-background/cpus", "3-7");
            boolean r24 = writeCpusetConfigs("/dev/cpuset/l-foreground/cpus", "2-7");
            boolean r34 = writeCpusetConfigs("/dev/cpuset/foreground/cpus", "0-7");
            r4 = writeCpusetConfigs("/dev/cpuset/restricted/cpus", "0-7");
            r5 = writeCpusetConfigs("/dev/cpuset/background/cpus", "4-7");
            r6 = writeCpusetConfigs("/dev/cpuset/top-app/cpus", "0-7");
            r7 = writeCpusetConfigs("/dev/cpuset/system-background/cpus", "4-7");
            r3 = r34;
            r0 = r24;
            r2 = r04;
        } else if (cluster != 4) {
            r2 = false;
            r1 = false;
            r0 = false;
            r3 = false;
            r4 = false;
        } else {
            boolean r05 = writeCpusetConfigs("/dev/cpuset/l-background/cpus", "3-5");
            r1 = writeCpusetConfigs("/dev/cpuset/h-background/cpus", "2-7");
            boolean r25 = writeCpusetConfigs("/dev/cpuset/l-foreground/cpus", "1-7");
            boolean r35 = writeCpusetConfigs("/dev/cpuset/foreground/cpus", "0-7");
            r4 = writeCpusetConfigs("/dev/cpuset/restricted/cpus", "0-7");
            r5 = writeCpusetConfigs("/dev/cpuset/background/cpus", "4-7");
            r6 = writeCpusetConfigs("/dev/cpuset/top-app/cpus", "0-7");
            r7 = writeCpusetConfigs("/dev/cpuset/system-background/cpus", "4-7");
            r3 = r35;
            r0 = r25;
            r2 = r05;
        }
        return r2 && r1 && r0 && r3 && r4 && r5 && r6 && r7;
    }

    public static void setConfigs(Bundle data) {
        if (data == null) {
            return;
        }
        synchronized (AppManager.getInstance()) {
            initList(TOP_SLEEPING_LIST, data.getStringArrayList(KEY_TOP_SLEEPING_LIST));
            List<String> list = data.getStringArrayList(KEY_SYSTEM_FG_LIST);
            if (list != null) {
                initList(SYSTEM_FG_LIST, list);
                SYSTEM_FG_LIST.add("com.google.android.providers.media.module");
                SYSTEM_FG_LIST.add("com.android.providers.media.module");
            }
            initList(SYSTEM_BG_LIST, data.getStringArrayList(KEY_SYSTEM_BG_LIST));
            initList(TOP_USED_APP_LIST, data.getStringArrayList(KEY_TOP_USED_APP_LIST));
            initList(TOP_SCHED_APP_LIST, data.getStringArrayList(KEY_TOP_SCHED_APP_LIST));
            initList(SET_TID_GROUP_POLICY_LIST, data.getStringArrayList(KEY_SET_TID_GROUP_POLICY));
            initList(HOT_START_FORBID_DELAYED_LIST, data.getStringArrayList(KEY_HOT_START_FORBID_DELAYED));
            sMaxCgFgRecentTime = data.getInt(KEY_MAX_CG_FG_RECENT_TIME, sMaxCgFgRecentTime);
            sSystemServerBgInGameScene = data.getBoolean(KEY_SYSTEM_SERVER_BG_IN_GAMESCENE, sSystemServerBgInGameScene);
            sGameSceneOptEnabled = data.getBoolean(KEY_GAME_SCENE_OPT_ENABLE, sGameSceneOptEnabled);
            sHotStartDelayedEnable = data.getBoolean(KEY_HOT_START_DELAYED_ENABLE, sHotStartDelayedEnable);
            sIgnoreFifoInGame = data.getBoolean(KEY_IGNORE_FIFO_IN_GAME, sIgnoreFifoInGame);
            sMoveBg2LittleCore = data.getBoolean(KEY_MOVE_BG_LITTLECORE, sMoveBg2LittleCore);
            setFeatureEnale(data.getBoolean(KEY_FEATURE_ENABLE, sFeatureEnabled));
        }
        BinderGroupController.getInstance().setConfigs(data);
    }

    private static void setFeatureEnale(boolean enable) {
        boolean featureEnable = sPlatformSupported && enable;
        if (featureEnable != sFeatureEnabled) {
            sFeatureEnabled = featureEnable;
            AppManager.getInstance().requestUpdateCgrp("update-cgrp-configs");
        }
    }

    public static boolean isAllowDump() {
        return SystemProperties.getBoolean("persist.rms.allow_dump", false);
    }

    public static void dump(PrintWriter pw, String[] args) {
        if (!isAllowDump()) {
            return;
        }
        synchronized (AppManager.getInstance()) {
            if (args.length >= 2 && "debug".equals(args[0])) {
                DEBUG = Boolean.valueOf(args[1]).booleanValue();
                pw.println("DEBUG = " + DEBUG);
            } else if (args.length >= 2 && "enable".equals(args[0])) {
                boolean enable = Boolean.valueOf(args[1]).booleanValue();
                setFeatureEnale(enable);
                pw.println("FeatureEnabled = " + sFeatureEnabled);
            } else {
                pw.println("FEATURE_ENABLED = " + sFeatureEnabled);
                pw.println("PLATFORM_SUPPORTED = " + sPlatformSupported);
                pw.println("DEBUG = " + DEBUG);
                if (sFeatureEnabled) {
                    dumpList(pw, TOP_SLEEPING_LIST, "TOP_SLEEPING_LIST");
                    dumpList(pw, SYSTEM_FG_LIST, "SYSTEM_FG_LIST");
                    dumpList(pw, SYSTEM_BG_LIST, "SYSTEM_BG_LIST");
                    dumpList(pw, TOP_USED_APP_LIST, "TOP_USED_APP_LIST");
                    dumpList(pw, TOP_SCHED_APP_LIST, "TOP_SCHED_APP_LIST");
                    dumpList(pw, SET_TID_GROUP_POLICY_LIST, "SET_TID_GROUP_POLICY_LIST");
                    dumpList(pw, HOT_START_FORBID_DELAYED_LIST, "HOT_START_FORBID_DELAYED_LIST");
                    pw.println("GameSceneOptEnabled = " + sGameSceneOptEnabled);
                    pw.println("SystemServerBgInGameScene = " + sSystemServerBgInGameScene);
                    pw.println("GameSceneOpt = " + isGameSceneOpt());
                    pw.println("IgnoreFifoInGame = " + sIgnoreFifoInGame);
                    pw.println("sMoveBg2LittleCore = " + sMoveBg2LittleCore);
                    pw.println("PidGroupPolicyEnable = " + PID_GROUP_POLICY_ENABLE);
                    pw.println("HotStartDelayedEnable = " + sHotStartDelayedEnable);
                    pw.print("/dev/cpuset/l-background/cpus->" + readCpusetConfigs("/dev/cpuset/l-background/cpus"));
                    pw.print("/dev/cpuset/background/cpus->" + readCpusetConfigs("/dev/cpuset/background/cpus"));
                    pw.print("/dev/cpuset/system-background/cpus->" + readCpusetConfigs("/dev/cpuset/system-background/cpus"));
                    pw.print("/dev/cpuset/h-background/cpus->" + readCpusetConfigs("/dev/cpuset/h-background/cpus"));
                    pw.print("/dev/cpuset/l-foreground/cpus->" + readCpusetConfigs("/dev/cpuset/l-foreground/cpus"));
                }
            }
        }
    }

    public static boolean isIgnoreFifoInGame() {
        return isGameSceneOpt() && sIgnoreFifoInGame;
    }

    public static boolean isMoveBg2LittleCore() {
        return sMoveBg2LittleCore;
    }

    private static String readCpusetConfigs(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.canRead()) {
            byte[] buffer = new byte[64];
            return readLine(file, buffer);
        }
        return "?";
    }

    public static boolean isSystemFg(String pkgName) {
        return SYSTEM_FG_LIST.contains(pkgName);
    }

    public static boolean isTopUsedApp(String pkgName) {
        return TOP_USED_APP_LIST.contains(pkgName);
    }

    public static boolean isTopSchedApp(String pkgName) {
        return TOP_SCHED_APP_LIST.contains(pkgName);
    }

    public static boolean isSystemBg(String pkgName) {
        return SYSTEM_BG_LIST.contains(pkgName);
    }

    public static boolean isTopSleeping(String pkgName) {
        return TOP_SLEEPING_LIST.contains(pkgName);
    }

    public static boolean isHotStartForbidDelayed(String procName) {
        return HOT_START_FORBID_DELAYED_LIST.contains(procName);
    }

    public static boolean isTidGroupPolicyList(String procName) {
        if (PID_GROUP_POLICY_ENABLE) {
            synchronized (SET_TID_GROUP_POLICY_LIST) {
                if (SET_TID_GROUP_POLICY_LIST.contains("*")) {
                    return true;
                }
                for (String list : SET_TID_GROUP_POLICY_LIST) {
                    if (procName.startsWith(list)) {
                        return true;
                    }
                }
                return false;
            }
        }
        return true;
    }

    public static void initList(List<String> targetList, List<String> list) {
        if (list == null) {
            return;
        }
        synchronized (targetList) {
            targetList.clear();
            targetList.addAll(list);
        }
    }

    public static void dumpList(PrintWriter pw, List<String> list, String tag) {
        if (list == null) {
            return;
        }
        synchronized (list) {
            pw.print(tag);
            pw.print("=");
            pw.println(list);
        }
    }

    public static boolean systemServerBgInGameScene() {
        return sSystemServerBgInGameScene;
    }

    public static int maxCgFgRecentTime() {
        return sMaxCgFgRecentTime;
    }

    public static boolean isGameSceneOpt() {
        return GameOptManager.isGamePlaying() && isGameSceneEnable();
    }

    public static boolean isFeatureEnable() {
        return sFeatureEnabled;
    }

    public static boolean isGameSceneEnable() {
        return sGameSceneOptEnabled;
    }

    public static boolean isHotStartDelayedEnable() {
        return sHotStartDelayedEnable;
    }

    public static boolean isRealtimeBlurEnable() {
        RealtimeBlurObserver realtimeBlurObserver;
        return sSupportRtblur && (realtimeBlurObserver = sRealtimeBlurObserver) != null && realtimeBlurObserver.isRealtimeBlurEnable();
    }

    private static boolean isSupportRealtimeBlurSwitch(Context context) {
        boolean support = false;
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, 128);
            if (appInfo == null || appInfo.metaData == null) {
                return false;
            }
            support = appInfo.metaData.getBoolean("realtime_blur_switch_support", false);
            VLog.i(TAG, "getMetaData realtime_blur_switch_support: " + support);
            return support;
        } catch (Exception e) {
            VLog.e(TAG, "isSupportRealtimeBlurSwitch", e);
            return support;
        }
    }

    public static int setProcessChildCgrp(ProcessInfo pi, int group) {
        String pkgName = pi.mPkgName;
        int pid = pi.mPid;
        int uid = pi.mUid;
        int tSize = 0;
        File file = new File(String.format("/acct/uid_%d/pid_%d/cgroup.procs", Integer.valueOf(uid), Integer.valueOf(pid)));
        if (file.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file), 512);
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    int child = Integer.parseInt(line);
                    if (child != pid) {
                        if (DEBUG) {
                            VSlog.i(TAG, String.format("setProcessChildCgrp %s child=%d", pkgName, Integer.valueOf(child)));
                        }
                        try {
                            try {
                                tSize += JniTool.setProcessGroup(child, group, 0);
                            } catch (Throwable th) {
                                th = th;
                                IoUtils.closeQuietly(reader);
                                throw th;
                            }
                        } catch (Exception e) {
                            e = e;
                            VSlog.e(TAG, "setProcessChildCgrp error" + e);
                            IoUtils.closeQuietly(reader);
                            return tSize;
                        }
                    }
                }
            } catch (Exception e2) {
                e = e2;
            } catch (Throwable th2) {
                th = th2;
                IoUtils.closeQuietly(reader);
                throw th;
            }
            IoUtils.closeQuietly(reader);
            return tSize;
        }
        return 0;
    }

    private static String readLine(File file, byte[] buffer) {
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            int len = is.read(buffer);
            if (len > 0) {
                int i = 0;
                while (true) {
                    if (i >= len) {
                        break;
                    } else if (buffer[i] != 0) {
                        i++;
                    } else {
                        i--;
                        break;
                    }
                }
                if (i >= 0) {
                    String str = new String(buffer, 0, i);
                    IoUtils.closeQuietly(is);
                    return str;
                }
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
        } catch (Throwable th) {
            IoUtils.closeQuietly(is);
            throw th;
        }
        IoUtils.closeQuietly(is);
        return null;
    }

    private static boolean writeCpusetConfigs(String path, String value) {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileOutputStream(path));
            pw.write(value);
            pw.flush();
            return true;
        } catch (Exception e) {
            VSlog.d(TAG, String.format("writeCpuset %s %s error=%s", path, value, e.getMessage()));
            return false;
        } finally {
            IoUtils.closeQuietly(pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class RealtimeBlurObserver extends ContentObserver {
        private Context mContext;
        private int mRealtimeBlurState;

        public RealtimeBlurObserver(Handler handler) {
            super(handler);
            this.mRealtimeBlurState = 1;
        }

        public void observe(Context context) {
            this.mContext = context;
            ContentResolver cr = context.getContentResolver();
            cr.registerContentObserver(Settings.System.getUriFor(CgrpUtils.sBlurSwitchName), false, this, 0);
            updateRealtimeBlurState();
        }

        public boolean isRealtimeBlurEnable() {
            return this.mRealtimeBlurState == 1;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            updateRealtimeBlurState();
        }

        private void updateRealtimeBlurState() {
            this.mRealtimeBlurState = Settings.System.getInt(this.mContext.getContentResolver(), CgrpUtils.sBlurSwitchName, 1);
            VLog.d(CgrpUtils.TAG, "updateRealtimeBlurState state=" + this.mRealtimeBlurState);
        }
    }
}