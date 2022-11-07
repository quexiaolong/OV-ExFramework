package com.vivo.services.rms.cgrp;

import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.Trace;
import android.util.SparseArray;
import com.android.server.ServiceThread;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.GameOptManager;
import com.vivo.services.rms.Platform;
import com.vivo.services.rms.PreloadedAppRecordMgr;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.RMAms;
import com.vivo.services.rms.RMServer;
import com.vivo.services.rms.appmng.AppManager;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.rms.util.JniTool;

/* loaded from: classes.dex */
public final class CgrpController {
    private static final int FLAG_MOVE_BG_LITTLE_CORE = 2;
    private static final int FLAG_SET_BY_TID = 32;
    private static final int FLAG_SKIP_BG = 4;
    private static final int FLAG_SKIP_FG = 8;
    private static final int FLAG_SKIP_FIFO = 16;
    private static final int FLAG_SYSTEM_APP = 1;
    static final int MY_PID = Process.myPid();
    static final String TAG = "CgrpController";
    private SetNativeGroupHandler mSetNativeGroupHandler;
    private SetProcessGroupHandler mSetProcessGroupHandler;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final CgrpController INSTANCE = new CgrpController();

        private Instance() {
        }
    }

    public static CgrpController getInstance() {
        return Instance.INSTANCE;
    }

    public void initialize() {
        CgrpUtils.initialize();
        if (Platform.getCpuCluster() != -1) {
            ServiceThread setProcessGroupThead = new ServiceThread("OomAdjuster", Platform.isTwoBigcoreDevice() ? -4 : -10, false);
            setProcessGroupThead.start();
            this.mSetProcessGroupHandler = new SetProcessGroupHandler(setProcessGroupThead.getLooper());
            AnimationManager.getInstance().initialize(setProcessGroupThead.getLooper());
            JniTool.initCgrp();
            JniTool.setThreadGroup(setProcessGroupThead.getThreadId(), 7);
            ServiceThread setNativeGroupHandler = new ServiceThread("NativeOom", 10, false);
            setNativeGroupHandler.start();
            this.mSetNativeGroupHandler = new SetNativeGroupHandler(setNativeGroupHandler.getLooper());
            JniTool.setThreadGroup(setNativeGroupHandler.getThreadId(), 4);
        }
    }

    public void applyCgrpPolicy(ProcessInfo pi) {
        if (pi == null || !pi.isAlive()) {
            return;
        }
        if (!CgrpUtils.isFeatureEnable()) {
            pi.mCgrpGroup = -1;
            pi.mOriginGroup = -1;
            return;
        }
        Message msg = this.mSetProcessGroupHandler.obtainMessage(pi.mPid, pi);
        if (this.mSetProcessGroupHandler.hasMessages(pi.mPid)) {
            return;
        }
        if (pi.mSchedGroup == 3 || isVisibleActivity(pi)) {
            this.mSetProcessGroupHandler.sendMessageAtFrontOfQueue(msg);
        } else {
            msg.sendToTarget();
        }
    }

    public void applyCgrpPolicy(String reason) {
        Trace.traceBegin(64L, "applyCgrpPolicy_" + reason);
        synchronized (AppManager.getInstance()) {
            SparseArray<ProcessInfo> procs = AppManager.getInstance().getProcsLocked();
            for (int i = 0; i < procs.size(); i++) {
                ProcessInfo pi = procs.valueAt(i);
                if (!CgrpUtils.isFeatureEnable() && "update-cgrp-configs".equals(reason)) {
                    if (pi.mCgrpGroup != -1) {
                        int group = pi.mSchedGroup;
                        if (group == 3 && !pi.isVisible()) {
                            group = 2;
                        }
                        pi.mSchedGroup = group;
                        pi.mCgrpGroup = -1;
                        pi.mOriginGroup = -1;
                        if (!CgrpPolicy.isSame(pi.mCgrpGroup, group)) {
                            if (this.mSetProcessGroupHandler != null) {
                                this.mSetProcessGroupHandler.removeMessages(pi.mPid);
                                this.mSetProcessGroupHandler.obtainMessage(pi.mPid, CgrpPolicy.group2Policy(group), 0).sendToTarget();
                            }
                        }
                    }
                } else {
                    applyCgrpPolicy(pi);
                }
            }
        }
        Trace.traceEnd(64L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int calculateCgrpPolicyLocked(ProcessInfo pi, boolean gameScene, boolean isHotStarted) {
        if (MY_PID == pi.mPid) {
            pi.mOriginGroup = pi.mSchedGroup;
            return (gameScene && forceSSBgInGameScene() && !pi.hasFocus()) ? 3 : 6;
        }
        computeOriginGroupLocked(pi);
        int curCgrpGroup = 1;
        int originGroup = pi.mOriginGroup;
        boolean isTopSleeping = isTopSleepingActivity(pi);
        boolean isFgService = pi.hasFgService();
        boolean isVisible = pi.isVisible() && !isTopSleeping;
        boolean isFocus = pi.hasFocus() && !isTopSleeping;
        if (originGroup == 0) {
            if (gameScene) {
                if (isFocus || (isFgService && isVisible)) {
                    curCgrpGroup = 5;
                } else if (pi.isMedia() || pi.hasVirualDisplay()) {
                    curCgrpGroup = 4;
                } else if (pi.isSystemApp() || isVisible || pi.isWorking() || isTopUsedApp(pi.mPkgName) || pi.mPkgName.equals(GameOptManager.getGameName())) {
                    curCgrpGroup = 2;
                } else {
                    curCgrpGroup = 1;
                }
            } else {
                boolean perceptible = isVisible || pi.hasVirualDisplay() || pi.isWorking();
                if (isFocus || (isFgService && (isVisible || isSystemFg(pi)))) {
                    curCgrpGroup = 5;
                } else if (pi.isBackUpApp() || pi.isAudio() || (isFgService && (perceptible || isTopUsedApp(pi.mProcName) || isRecentTask(pi)))) {
                    curCgrpGroup = 4;
                } else if (perceptible || pi.isSystemApp() || isTopUsedApp(pi.mPkgName) || pi.isHeavyApp() || pi.isFgActivity()) {
                    curCgrpGroup = 2;
                } else {
                    curCgrpGroup = 1;
                }
                ProcessInfo oom = pi.getRootOom();
                if (oom != null) {
                    int oomCgrpGroup = Math.min(Math.min(CgrpPolicy.group2Policy(pi.mSchedGroup), 5), oom.mCgrpGroup);
                    curCgrpGroup = Math.max(oomCgrpGroup, curCgrpGroup);
                }
            }
        } else if (originGroup == 2) {
            if (gameScene) {
                if (isFocus) {
                    curCgrpGroup = 7;
                } else if (pi.isMedia() || pi.hasVirualDisplay() || isVisible) {
                    curCgrpGroup = 4;
                } else if (!pi.isSystemApp() && !pi.isWorking() && !pi.isOomByFg()) {
                    if (isTopUsedApp(pi.mPkgName)) {
                        curCgrpGroup = 2;
                    } else {
                        curCgrpGroup = 1;
                    }
                } else {
                    curCgrpGroup = 3;
                }
            } else if (isFocus) {
                curCgrpGroup = 7;
            } else if (isVisible || pi.isOomByFg() || isSystemFg(pi)) {
                curCgrpGroup = 6;
            } else if (isSystemBg(pi)) {
                curCgrpGroup = 3;
            } else {
                curCgrpGroup = 5;
            }
        } else if (originGroup == 3 || originGroup == 4) {
            if (PreloadedAppRecordMgr.getInstance().isRmsPreload(pi.mPkgName, pi.mUid)) {
                curCgrpGroup = 5;
            } else {
                curCgrpGroup = 7;
            }
        } else if (originGroup == 1) {
            curCgrpGroup = 8;
        }
        if (isHotStarted && curCgrpGroup >= 4 && !AnimationManager.getInstance().isAnimationTimeout(pi)) {
            int oldCgrpGroup = curCgrpGroup;
            int curCgrpGroup2 = Math.min(pi.mCgrpGroup, pi.isAudio() ? 4 : 2);
            VLog.d(TAG, String.format("Animation optimize %s from %s to %s", pi.mProcName, CgrpPolicy.policy2String(oldCgrpGroup), CgrpPolicy.policy2String(curCgrpGroup2)));
            return curCgrpGroup2;
        }
        return curCgrpGroup;
    }

    private void computeOriginGroupLocked(ProcessInfo pi) {
        pi.mOriginGroup = pi.mSchedGroup;
        if (pi.mSchedGroup != 3 && pi.isFgActivity() && (pi.isRunningRemoteAnimation() || isVisibleActivity(pi) || isTopActivity(pi) || CgrpUtils.isTopSleeping(pi.mProcName))) {
            pi.mOriginGroup = 3;
        } else if (pi.mSchedGroup == 0 && isSystemFg(pi)) {
            pi.mOriginGroup = 2;
        } else if (pi.mSchedGroup == 2 && isSystemTopSched(pi)) {
            pi.mOriginGroup = 3;
        } else if (pi.mSchedGroup == 2 && RMServer.getInstance().isReady() && RMAms.getInstance().getWakefulness() == 1) {
            if (pi.mAdj >= 200 || (pi.hasFgService() && pi.mAdj == 50)) {
                pi.mOriginGroup = 0;
            } else if (!pi.isSystemApp() && pi.mAdj >= 50 && pi.mAdj < 200 && !pi.isOomByFg() && !pi.isOomByPersistent()) {
                pi.mOriginGroup = 0;
            }
        }
    }

    public void onAttachedApplication(int pid, String processName, ApplicationInfo info) {
        if (CgrpUtils.isFeatureEnable() && CgrpUtils.isGameSceneOpt()) {
            int group = 2;
            if (info.packageName.equals(GameOptManager.getGameName()) || (info.isSystemApp() && CgrpUtils.isSystemFg(processName))) {
                group = 5;
            }
            if (CgrpUtils.DEBUG) {
                VLog.d(TAG, String.format("move %s bindApplication to bg %d", processName, Integer.valueOf(group)));
            }
            this.mSetProcessGroupHandler.obtainMessage(pid, group, 0).sendToTarget();
        }
    }

    private boolean isSystemTopSched(ProcessInfo pi) {
        return pi.isSystemApp() && CgrpUtils.isTopSchedApp(pi.mProcName);
    }

    private boolean isSystemFg(ProcessInfo pi) {
        return pi.isSystemApp() && CgrpUtils.isSystemFg(pi.mProcName);
    }

    private boolean isSystemBg(ProcessInfo pi) {
        return pi.isSystemApp() && CgrpUtils.isSystemBg(pi.mProcName);
    }

    private boolean isTopActivity(ProcessInfo pi) {
        return "top-activity".equals(pi.mAdjType);
    }

    private boolean isTopSleepingActivity(ProcessInfo pi) {
        return "top-sleeping".equals(pi.mAdjType);
    }

    private boolean isVisibleActivity(ProcessInfo pi) {
        return "vis-activity".equals(pi.mAdjType);
    }

    private boolean isTopUsedApp(String pkgName) {
        return CgrpUtils.isTopUsedApp(pkgName);
    }

    private boolean forceSSBgInGameScene() {
        return CgrpUtils.systemServerBgInGameScene();
    }

    private boolean isRecentTask(ProcessInfo pi) {
        long time = AppManager.getInstance().getRecentTask().getRecentTime(pi.mPkgName, pi.mUid);
        return time != -1 && SystemClock.uptimeMillis() - time < ((long) CgrpUtils.maxCgFgRecentTime());
    }

    public void resetProcessGroupHandler(ProcessInfo pi) {
        SetProcessGroupHandler setProcessGroupHandler = this.mSetProcessGroupHandler;
        if (setProcessGroupHandler != null) {
            setProcessGroupHandler.removeMessages(pi.mPid);
            SetProcessGroupHandler setProcessGroupHandler2 = this.mSetProcessGroupHandler;
            setProcessGroupHandler2.sendMessageAtFrontOfQueue(setProcessGroupHandler2.obtainMessage(pi.mPid, pi));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SetProcessGroupHandler extends Handler {
        private SetProcessGroupHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            ProcessInfo pi;
            boolean isHotStarted;
            int pid = msg.what;
            if (msg.obj != null) {
                ProcessInfo pi2 = (ProcessInfo) msg.obj;
                if (!pi2.isAlive() || !CgrpUtils.isFeatureEnable()) {
                    pi2.mCgrpGroup = -1;
                    pi2.mOriginGroup = -1;
                    return;
                }
                int group = -1;
                if (!pi2.isRunningRemoteAnimation() && AnimationManager.getInstance().isHotStartDelayed(pi2)) {
                    isHotStarted = true;
                } else {
                    isHotStarted = false;
                }
                Trace.traceBegin(64L, "setProcessGroupWithFlags");
                synchronized (AppManager.getInstance()) {
                    int curCgrpGroup = CgrpController.this.calculateCgrpPolicyLocked(pi2, CgrpUtils.isGameSceneOpt(), isHotStarted);
                    if (pi2.mCgrpGroup != curCgrpGroup) {
                        pi2.mCgrpGroup = curCgrpGroup;
                        group = curCgrpGroup;
                    }
                }
                if (group != -1) {
                    int threadSize = CgrpController.setProcessGroupWithFlags(pi2, group);
                    if (!pi2.isSystemApp()) {
                        CgrpController.this.mSetNativeGroupHandler.removeMessages(pi2.mPid);
                        CgrpController.this.mSetNativeGroupHandler.sendMessage(CgrpController.this.mSetNativeGroupHandler.obtainMessage(pi2.mPid, group, 0, pi2));
                    }
                    if (CgrpUtils.DEBUG || threadSize >= 200) {
                        VLog.d(CgrpController.TAG, String.format("setProcessGroupWithFlags %s,pid=%d,adjType=%s,states=[%s],group=%s,threadSize=%d", pi2.mProcName, Integer.valueOf(pid), pi2.mAdjType, Consts.ProcessStates.getName(pi2.mStates), pi2.makeSchedString(), Integer.valueOf(threadSize)));
                    }
                }
            } else {
                int group2 = msg.arg1;
                Trace.traceBegin(64L, "setProcessGroup");
                int threadSize2 = CgrpController.setProcessGroup(pid, group2);
                if (CgrpUtils.DEBUG && (pi = AppManager.getInstance().getProcessInfo(pid)) != null) {
                    VLog.d(CgrpController.TAG, String.format("restoreProcessGroup %s pid=%d group=%d threadSize=%d", pi.mProcName, Integer.valueOf(pid), Integer.valueOf(group2), Integer.valueOf(threadSize2)));
                }
            }
            Trace.traceEnd(64L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SetNativeGroupHandler extends Handler {
        private SetNativeGroupHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            ProcessInfo pi = (ProcessInfo) msg.obj;
            int group = msg.arg1;
            if (!pi.isAlive()) {
                return;
            }
            CgrpUtils.setProcessChildCgrp(pi, group);
        }
    }

    public static int setProcessGroupWithFlags(ProcessInfo pi, int policy) {
        int flags = isIgnoreFifoInGame(pi) ? 0 : 16;
        if (pi.mPid == MY_PID) {
            flags |= 12;
        }
        if (pi.isSystemApp()) {
            flags |= 1;
        }
        if (CgrpUtils.isTidGroupPolicyList(pi.mProcName) || pi.mPid == MY_PID) {
            flags |= 32;
        }
        if ((policy <= 5 || pi.isSystemApp()) && CgrpUtils.isMoveBg2LittleCore()) {
            flags |= 2;
        }
        return JniTool.setProcessGroup(pi.mPid, policy, flags);
    }

    public static int setProcessGroup(int pid, int policy) {
        int flags = 16;
        if (pid == MY_PID) {
            flags = 16 | 12;
        }
        return JniTool.setProcessGroup(pid, policy, flags);
    }

    private static boolean isIgnoreFifoInGame(ProcessInfo pi) {
        return CgrpUtils.isIgnoreFifoInGame() && !pi.isMedia();
    }
}