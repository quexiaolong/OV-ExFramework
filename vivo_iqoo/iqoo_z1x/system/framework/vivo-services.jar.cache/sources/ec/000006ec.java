package com.vivo.services.rms;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import com.android.server.IVivoRmsInjector;
import com.android.server.RmsKeepQuietListener;
import com.android.server.ServiceThread;
import com.android.server.am.ProcessRecord;
import com.android.server.am.RMProcHelper;
import com.android.server.am.RMProcInfo;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.wm.WindowState;
import com.vivo.services.proxy.game.GameSceneProxyManager;
import com.vivo.services.rms.appmng.AppManager;
import com.vivo.services.rms.appmng.DeathReason;
import com.vivo.services.rms.appmng.namelist.OomNode;
import com.vivo.services.rms.appmng.namelist.OomPreloadList;
import com.vivo.services.rms.appmng.namelist.OomPreviousList;
import com.vivo.services.rms.appmng.namelist.OomProtectList;
import com.vivo.services.rms.appmng.namelist.OomStaticList;
import com.vivo.services.rms.appmng.namelist.WidgetList;
import com.vivo.services.rms.cgrp.AnimationManager;
import com.vivo.services.rms.cgrp.BinderGroupController;
import com.vivo.services.rms.cgrp.CgrpController;
import com.vivo.services.rms.cgrp.CgrpUtils;
import com.vivo.services.rms.display.GlobalConfigs;
import com.vivo.services.rms.display.WindowRequestManager;
import com.vivo.services.rms.proxy.VivoBinderProxy;
import com.vivo.services.rms.sp.SpManagerImpl;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class RmsInjectorImpl implements IVivoRmsInjector {
    public static final int MY_PID = Process.myPid();
    String TAG;
    private Context mContext;
    private Handler mHandler;
    private boolean mInited;
    private Looper mLooper;
    private boolean mMonkeyState;
    private OomNode mOomNode;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final RmsInjectorImpl INSTANCE = new RmsInjectorImpl();

        private Instance() {
        }
    }

    private RmsInjectorImpl() {
        this.mMonkeyState = false;
        this.mOomNode = new OomNode();
        this.mInited = false;
        this.TAG = "RMS-Preload";
        ServiceThread thread = new ServiceThread("rms_main", -8, false);
        thread.start();
        this.mLooper = thread.getLooper();
        this.mHandler = new Handler(this.mLooper);
    }

    public static RmsInjectorImpl getInstance() {
        return Instance.INSTANCE;
    }

    public void initialize(Context context) {
        SpManagerImpl.getInstance().initialize(context);
        RMProcHelper.initialize();
        RMServer.publish(context);
        CgrpController.getInstance().initialize();
        AppManager.getInstance().initialize(this.mLooper);
        this.mContext = context;
        this.mInited = true;
    }

    public void startCoreServices() {
        SpManagerImpl.getInstance().publish();
    }

    public void startOtherServices() {
        if (SpManagerImpl.getInstance().isSpsExist()) {
            boolean isSpsExist = SpManagerImpl.getInstance().startSps();
            RMServer.getInstance().startRms(isSpsExist);
        }
    }

    public void systemReady() {
        if (!SpManagerImpl.getInstance().isSpsExist()) {
            RMServer.getInstance().startRms(false);
        }
        CgrpUtils.initRealtimeBlurObserver(this.mContext);
        SpManagerImpl.getInstance().systemReady();
        BinderGroupController.getInstance().onSystemReady();
    }

    public static RmsInjectorImpl self() {
        return getInstance();
    }

    public void notifyRmsActivityStart(String pkgName, String processName, int pid, int uid, int started) {
        String str = this.TAG;
        VSlog.d(str, "notifyRmsActivityStart :" + pkgName + " started " + started);
        if (started == 1) {
            ProcessInfo pi = AppManager.getInstance().getProcessInfo(uid, processName);
            if (pi != null) {
                BinderGroupController.getInstance().setProcessGroup(pi, 2);
            }
            if (pi != null && pid != -1) {
                AnimationManager.getInstance().onActivityStart(pi, pid);
            }
        }
        EventDispatcher.getInstance().startActivity(pkgName, processName, pid, uid, started);
    }

    public void notifyRmsActivityOnVdStart(String pkgName, String processName, String className, int pid, int uid, int started) {
        ProcessInfo pi;
        String str = this.TAG;
        VSlog.d(str, "notifyRmsActivityOnVdStart :" + pkgName + " started " + started);
        if (started == 1 && (pi = AppManager.getInstance().getProcessInfo(uid, processName)) != null) {
            BinderGroupController.getInstance().setProcessGroup(pi, 2);
        }
        EventDispatcher.getInstance().startActivityOnVD(pkgName, processName, className, pid, uid, started);
    }

    public RMProcInfo newProcInfo(ProcessRecord parent, int uid, String pkgName, int flags, String procName) {
        return new ProcessInfo(parent, uid, pkgName, flags, procName);
    }

    public void addProcess(RMProcInfo pi) {
        if (pi.mPid > 0) {
            AppManager.getInstance().add((ProcessInfo) pi);
        }
    }

    public void removeProcess(RMProcInfo pi) {
        if (pi.mPid > 0) {
            DeathReason.fillReason((ProcessInfo) pi);
            AppManager.getInstance().remove((ProcessInfo) pi);
        }
    }

    public void addDepPkg(RMProcInfo pi, String pkg) {
        AppManager.getInstance().addDepPkg((ProcessInfo) pi, pkg);
    }

    public void addPkg(RMProcInfo pi, String pkg) {
        AppManager.getInstance().addPkg((ProcessInfo) pi, pkg);
    }

    public void modifyOomAdj(ProcessRecord app) {
        OomNode node;
        ProcessInfo pi = (ProcessInfo) RMProcHelper.getInfo(app);
        if (pi == null) {
            return;
        }
        int rawAdj = RMProcHelper.getInt(app, 13);
        int curProcState = RMProcHelper.getInt(app, 2);
        int curSchedGroup = RMProcHelper.getInt(app, 1);
        RMProcHelper.getString(app, 10);
        boolean hasActivity = RMProcHelper.getBoolean(app, 8);
        if (rawAdj > 0) {
            long now = SystemClock.uptimeMillis();
            String setAdjType = null;
            this.mOomNode.adj = rawAdj;
            this.mOomNode.procState = curProcState;
            this.mOomNode.schedGroup = curSchedGroup;
            OomNode node2 = OomPreloadList.getNode(pi, this.mOomNode.adj);
            if (node2 != null) {
                applyOomNode(node2);
                setAdjType = "rms-preload";
                if (rawAdj <= 100 && pi.hasShownUi() && isRmsPreload(pi.mPkgName, pi.mUid)) {
                    RMProcHelper.setInt(app, 13, node2.adj);
                    RMProcHelper.setInt(app, 0, node2.adj);
                    RMProcHelper.setInt(app, 1, node2.schedGroup);
                }
            }
            OomNode node3 = OomStaticList.getNode(pi, this.mOomNode.adj);
            if (node3 != null) {
                applyOomNode(node3);
                setAdjType = "static";
            }
            OomNode node4 = OomProtectList.getNode(pi, this.mOomNode.adj);
            if (node4 != null) {
                applyOomNode(node4);
                setAdjType = "protect";
            }
            if (hasActivity && (node = OomPreviousList.getNode(pi, this.mOomNode.adj)) != null) {
                applyOomNode(node);
                setAdjType = "previous";
            }
            long lastProviderTime = RMProcHelper.getLong(app, 12);
            if (lastProviderTime > 0 && lastProviderTime + 20000 > now && this.mOomNode.adj > 700) {
                this.mOomNode.adj = ProcessList.PREVIOUS_APP_ADJ;
                setAdjType = VivoFirewall.TYPE_PROVIDER;
                if (this.mOomNode.procState > 15) {
                    this.mOomNode.procState = 15;
                }
            }
            if (rawAdj > this.mOomNode.adj) {
                int rawAdj2 = this.mOomNode.adj;
                RMProcHelper.setInt(app, 13, rawAdj2);
                RMProcHelper.setInt(app, 0, rawAdj2);
                RMProcHelper.setBoolean(app, 7, false);
                if (curProcState > this.mOomNode.procState) {
                    RMProcHelper.setInt(app, 2, this.mOomNode.procState);
                }
                if (curSchedGroup != this.mOomNode.schedGroup) {
                    RMProcHelper.setInt(app, 1, this.mOomNode.schedGroup);
                }
                if (setAdjType != null) {
                    String curAdjType = setAdjType;
                    RMProcHelper.setString(app, 10, curAdjType);
                }
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:17:0x0059  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void applyOomAdjLocked(com.android.server.am.ProcessRecord r15) {
        /*
            r14 = this;
            com.android.server.am.RMProcInfo r0 = com.android.server.am.RMProcHelper.getInfo(r15)
            com.vivo.services.rms.ProcessInfo r0 = (com.vivo.services.rms.ProcessInfo) r0
            if (r0 != 0) goto L9
            return
        L9:
            r1 = 0
            int r2 = com.android.server.am.RMProcHelper.getInt(r15, r1)
            r3 = 1
            int r4 = com.android.server.am.RMProcHelper.getInt(r15, r3)
            r5 = 10
            java.lang.String r5 = com.android.server.am.RMProcHelper.getString(r15, r5)
            r6 = 5
            boolean r6 = com.android.server.am.RMProcHelper.getBoolean(r15, r6)
            r7 = 6
            boolean r7 = com.android.server.am.RMProcHelper.getBoolean(r15, r7)
            r8 = 3
            int r8 = com.android.server.am.RMProcHelper.getInt(r15, r8)
            r9 = 8
            boolean r9 = com.android.server.am.RMProcHelper.getBoolean(r15, r9)
            r10 = 9
            android.util.ArraySet r10 = com.android.server.am.RMProcHelper.getArraySet(r15, r10)
            com.vivo.services.rms.appmng.AppManager r11 = com.vivo.services.rms.appmng.AppManager.getInstance()
            r12 = 0
            monitor-enter(r11)
            if (r6 != 0) goto L49
            java.lang.String r13 = "pers-top-activity"
            boolean r13 = r13.equals(r5)     // Catch: java.lang.Throwable -> L47
            if (r13 == 0) goto L45
            goto L49
        L45:
            r13 = r1
            goto L4a
        L47:
            r1 = move-exception
            goto L8b
        L49:
            r13 = r3
        L4a:
            r11.setFgActivityLocked(r0, r13)     // Catch: java.lang.Throwable -> L47
            r11.setFgServiceLocked(r0, r7)     // Catch: java.lang.Throwable -> L47
            r11.setHasActivityLocked(r0, r9)     // Catch: java.lang.Throwable -> L47
            int r13 = r10.size()     // Catch: java.lang.Throwable -> L47
            if (r13 <= 0) goto L5a
            r1 = r3
        L5a:
            r11.setHasServiceLocked(r0, r1)     // Catch: java.lang.Throwable -> L47
            java.lang.String r1 = "pausing"
            boolean r1 = r1.equals(r5)     // Catch: java.lang.Throwable -> L47
            r11.setPausing(r0, r1)     // Catch: java.lang.Throwable -> L47
            r11.setOomLocked(r0, r8)     // Catch: java.lang.Throwable -> L47
            r11.setAdjLocked(r0, r2)     // Catch: java.lang.Throwable -> L47
            r11.setAdjTypeLocked(r0, r5)     // Catch: java.lang.Throwable -> L47
            r11.setSchedGroupLocked(r0, r4)     // Catch: java.lang.Throwable -> L47
            com.vivo.services.rms.cgrp.BinderGroupController r1 = com.vivo.services.rms.cgrp.BinderGroupController.getInstance()     // Catch: java.lang.Throwable -> L47
            int r1 = r1.computeGroupLocked(r0)     // Catch: java.lang.Throwable -> L47
            r12 = r1
            monitor-exit(r11)     // Catch: java.lang.Throwable -> L47
            com.vivo.services.rms.cgrp.BinderGroupController r1 = com.vivo.services.rms.cgrp.BinderGroupController.getInstance()
            r1.setProcessGroup(r0, r12)
            com.vivo.services.rms.cgrp.CgrpController r1 = com.vivo.services.rms.cgrp.CgrpController.getInstance()
            r1.applyCgrpPolicy(r0)
            return
        L8b:
            monitor-exit(r11)     // Catch: java.lang.Throwable -> L47
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.rms.RmsInjectorImpl.applyOomAdjLocked(com.android.server.am.ProcessRecord):void");
    }

    private void applyOomNode(OomNode node) {
        if (this.mOomNode.adj > node.adj) {
            this.mOomNode.adj = node.adj;
            if (this.mOomNode.procState > node.procState) {
                this.mOomNode.procState = node.procState;
            }
            this.mOomNode.schedGroup = node.schedGroup;
        }
    }

    public void updateOomAdjLocked() {
        AppManager.getInstance().applyPenddings();
    }

    public boolean isMonkey() {
        return this.mMonkeyState;
    }

    public String getPkgNameByPid(int pid) {
        ProcessInfo info = AppManager.getInstance().getProcessInfo(pid);
        if (info != null) {
            return info.mPkgName;
        }
        return String.valueOf(pid);
    }

    public RMProcInfo getProcByPid(int pid) {
        return AppManager.getInstance().getProcessInfo(pid);
    }

    public boolean isRefreshRateAdjusterSupported() {
        return GlobalConfigs.isFeatureEnabled();
    }

    public boolean isCgrpFeatureEnable() {
        return CgrpUtils.isFeatureEnable();
    }

    public String wrapReason(String reason, int callingPid) {
        if (callingPid <= 0 || callingPid == MY_PID) {
            return reason;
        }
        String pkgName = getPkgNameByPid(callingPid);
        return reason + " by " + pkgName;
    }

    public void addWidget(String pkg) {
        if (this.mInited) {
            WidgetList.addWidget(pkg);
        }
    }

    public void removeWidget(String pkg) {
        if (this.mInited) {
            WidgetList.removeWidget(pkg);
        }
    }

    public void addVirtualDisplay(String pkgName, int uid) {
        if (this.mInited) {
            AppManager.getInstance().addVirtualDisplay(pkgName, uid);
        }
    }

    public void removeVirtualDisplay(String pkgName, int uid, int displayId) {
        if (this.mInited) {
            AppManager.getInstance().removeVirtualDisplay(pkgName, uid);
            if (displayId == 95555) {
                PreloadedAppRecordMgr.getInstance().clearUiPreloadMute();
            }
        }
    }

    public void updateWindowFocus(int displayId, WindowState newFocus, WindowState oldFocus) {
        if (this.mInited) {
            AppManager.getInstance().updateWindowFocus(newFocus, oldFocus);
            WindowRequestManager.getInstance().updateFocus(displayId, newFocus, oldFocus);
        }
    }

    public void applyWindowRefreshRate(int displayId) {
        if (this.mInited) {
            WindowRequestManager.getInstance().applyWindowRefreshRate(displayId);
        }
    }

    public void setRmsPreload(String pkgName, int uid, boolean isRmsPreload, boolean keepQuiet) {
        if (this.mInited) {
            PreloadedAppRecordMgr.getInstance().setRmsPreload(pkgName, uid, isRmsPreload, keepQuiet);
        }
    }

    public void notifyRmsForSpecial(int type) {
        if (this.mInited) {
            EventDispatcher.getInstance().notifyRmsForSpecial(type);
        }
    }

    public boolean isRmsPreload(String pkgName, int uid) {
        if (!this.mInited) {
            return false;
        }
        return PreloadedAppRecordMgr.getInstance().isRmsPreload(pkgName, uid);
    }

    public boolean isRmsPreload(int pid, int uid) {
        RMProcInfo rmp;
        if (this.mInited && (rmp = getProcByPid(pid)) != null) {
            return PreloadedAppRecordMgr.getInstance().isRmsPreload(rmp.mPkgName, uid);
        }
        return false;
    }

    public boolean isRmsUIPreload(String pkgName, int uid) {
        if (!this.mInited) {
            return false;
        }
        return PreloadedAppRecordMgr.getInstance().isRmsUIPreload(pkgName, uid);
    }

    public void onScreenBrightnessChanged(int screenBrightness, int screenState) {
        GlobalConfigs.onScreenBrightnessChanged(screenBrightness, screenState);
    }

    public void updateGameScene(String pkg, int pid, boolean state) {
        if (this.mInited && isCgrpFeatureEnable() && CgrpUtils.isGameSceneEnable()) {
            AppManager.getInstance().requestUpdateCgrp("game-scene");
        }
        if (!state) {
            GameSceneProxyManager.onGameExit();
        }
    }

    public long acquireRefreshRate(String sceneName, String reason, int fps, int priority, int duration, int forWho) {
        if (reason.equals("RecentsAnimationStart")) {
            AnimationManager.getInstance().onAnimationStart();
        }
        if (GlobalConfigs.isFeatureSupported()) {
            return WindowRequestManager.getInstance().acquireRefreshRate(sceneName, reason, fps, priority, duration, forWho);
        }
        return -1L;
    }

    public void releaseRefreshRate(long handle) {
        AnimationManager.getInstance().onAnimationEnd();
        if (GlobalConfigs.isFeatureSupported()) {
            WindowRequestManager.getInstance().releaseRefreshRate(handle);
        }
    }

    public void showWindow(int pid, WindowState win) {
        if (this.mInited) {
            ProcessInfo pi = AppManager.getInstance().addWindow(pid, win);
            WindowRequestManager.getInstance().addWindow(pi, win);
        }
    }

    public void hideWindow(int pid, WindowState win) {
        if (this.mInited) {
            ProcessInfo pi = AppManager.getInstance().removeWindow(pid, win);
            WindowRequestManager.getInstance().removeWindow(pi, win);
        }
    }

    public void commitFinishDrawing(int pid, int state, WindowState win) {
        if (this.mInited) {
            AnimationManager.getInstance().onFirstDrawingFinish(pid, state);
        }
    }

    public void addWindow(int pid, WindowState win) {
        ProcessInfo pi;
        if (this.mInited && (pi = AppManager.getInstance().getProcessInfo(pid)) != null && !pi.hasShownUi()) {
            VivoBinderProxy.getInstance().reportHasShownUi(pid);
        }
    }

    public boolean needKeepQuiet(int pid, int uid, int flag) {
        if (!this.mInited) {
            return false;
        }
        return PreloadedAppRecordMgr.getInstance().needKeepQuiet(pid, uid, flag);
    }

    public boolean needKeepQuiet(String pkgName, int userId, int uid, int flag) {
        if (!this.mInited) {
            return false;
        }
        return PreloadedAppRecordMgr.getInstance().needKeepQuiet(pkgName, userId, uid, flag);
    }

    public void setNeedKeepQuiet(int pid, int uid, boolean keepQuiet) {
        if (this.mInited) {
            PreloadedAppRecordMgr.getInstance().setNeedKeepQuiet(pid, uid, keepQuiet);
        }
    }

    public void setNeedKeepQuiet(String pkgName, int userId, int uid, boolean keepQuiet) {
        if (this.mInited) {
            PreloadedAppRecordMgr.getInstance().setNeedKeepQuiet(pkgName, userId, uid, keepQuiet);
        }
    }

    public void registerListener(RmsKeepQuietListener listener) {
        if (this.mInited) {
            PreloadedAppRecordMgr.getInstance().registerListener(listener);
        }
    }

    public void unRegisterListener(RmsKeepQuietListener listener) {
        if (this.mInited) {
            PreloadedAppRecordMgr.getInstance().unRegisterListener(listener);
        }
    }

    public void setMonkeyState(boolean isMonkeyUser) {
        if (this.mInited && this.mMonkeyState != isMonkeyUser) {
            EventDispatcher.getInstance().setMonkeyState(isMonkeyUser ? 1 : 0);
            this.mMonkeyState = isMonkeyUser;
        }
    }

    public void setHomeProcess(int homeProcessPid) {
        AppManager.getInstance().setHomeProcessPid(homeProcessPid);
    }

    public Bundle getAppCoreSettings(Bundle coreSettings, ApplicationInfo info, String procName) {
        VspaConfigs.initAppCoreSettings(coreSettings, info, procName);
        return coreSettings;
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public Context getContext() {
        return this.mContext;
    }

    public boolean isGamePlayingNow() {
        return GameOptManager.isGamePlaying();
    }

    public Looper getLooper() {
        return this.mLooper;
    }
}