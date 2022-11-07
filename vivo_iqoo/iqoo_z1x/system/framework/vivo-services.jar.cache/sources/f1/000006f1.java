package com.vivo.services.rms.appmng;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.SparseArray;
import com.android.server.am.RMProcHelper;
import com.android.server.am.frozen.FrozenInjectorImpl;
import com.android.server.wm.WindowState;
import com.vivo.common.utils.VLog;
import com.vivo.services.proxy.game.GameSceneProxyManager;
import com.vivo.services.rms.EventDispatcher;
import com.vivo.services.rms.EventNotifier;
import com.vivo.services.rms.GameOptManager;
import com.vivo.services.rms.PackageInfo;
import com.vivo.services.rms.PreloadedAppRecordMgr;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.RMServer;
import com.vivo.services.rms.RMWms;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.appmng.namelist.OomPreviousList;
import com.vivo.services.rms.cgrp.AnimationManager;
import com.vivo.services.rms.cgrp.CgrpController;
import com.vivo.services.rms.proxy.VivoBinderProxy;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.rms.sdk.IntArrayFactory;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class AppManager {
    private static final String TAG = "AppManager";
    private final SparseArray<ArrayList<PackageInfo>> mApps;
    private int mHomeProcessPid;
    private OomHandler mOomHandler;
    private final IntValueChangeItem mPendingADJChange;
    private final IntValueChangeItem mPendingSchedChange;
    private final StateChangeItem mPendingStateChange;
    private final SparseArray<ProcessInfo> mProcs;
    private final RecentTask mRecentTask;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final AppManager INSTANCE = new AppManager();

        private Instance() {
        }
    }

    public static AppManager getInstance() {
        return Instance.INSTANCE;
    }

    private AppManager() {
        this.mProcs = new SparseArray<>(128);
        this.mApps = new SparseArray<>(128);
        this.mPendingADJChange = new IntValueChangeItem(0);
        this.mPendingSchedChange = new IntValueChangeItem(2);
        this.mPendingStateChange = new StateChangeItem();
        this.mRecentTask = new RecentTask();
        this.mHomeProcessPid = -1;
    }

    public void initialize(Looper looper) {
        this.mOomHandler = new OomHandler(looper);
    }

    public void doInitLocked(Bundle dest) {
        Bundle procs = new Bundle();
        for (int i = 0; i < this.mProcs.size(); i++) {
            procs.putBundle(String.valueOf(this.mProcs.keyAt(i)), this.mProcs.valueAt(i).toBundleLocked());
        }
        if (!procs.keySet().isEmpty()) {
            dest.putBundle("_pids", procs);
        }
    }

    public ProcessInfo getProcessInfo(int pid) {
        ProcessInfo processInfo;
        if (pid <= 0) {
            return null;
        }
        synchronized (this) {
            processInfo = this.mProcs.get(pid);
        }
        return processInfo;
    }

    public ProcessInfo getProcessInfo(int uid, String procName) {
        synchronized (this) {
            ArrayList<PackageInfo> apps = this.mApps.get(uid);
            if (apps != null) {
                Iterator<PackageInfo> it = apps.iterator();
                while (it.hasNext()) {
                    PackageInfo app = it.next();
                    ProcessInfo proc = app.findProc(procName);
                    if (proc != null) {
                        return proc;
                    }
                }
            }
            return null;
        }
    }

    public List<ProcessInfo> getProcessInfoList(int uid, String pkgName) {
        List<ProcessInfo> result = new ArrayList<>();
        synchronized (this) {
            ArrayList<PackageInfo> apps = this.mApps.get(uid);
            if (apps != null) {
                Iterator<PackageInfo> it = apps.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    PackageInfo app = it.next();
                    if (app.mPkgName.equals(pkgName)) {
                        result.addAll(app.getProcs());
                        break;
                    }
                }
            }
        }
        return result;
    }

    public List<ProcessInfo> getProcessInfoList(int uid) {
        List<ProcessInfo> result = new ArrayList<>();
        synchronized (this) {
            ArrayList<PackageInfo> apps = this.mApps.get(uid);
            if (apps != null) {
                Iterator<PackageInfo> it = apps.iterator();
                while (it.hasNext()) {
                    PackageInfo app = it.next();
                    result.addAll(app.getProcs());
                }
            }
        }
        return result;
    }

    public List<ProcessInfo> getProcessInfoListForUid(int uid) {
        return getProcessInfoList(uid);
    }

    public ProcessInfo add(ProcessInfo pi) {
        synchronized (this) {
            EventDispatcher.getInstance().add(pi.mUid, pi.mPkgName, pi.mPkgFlags, pi.mPid, pi.mProcName, pi.mCreateReason);
            addAppLocked(pi);
        }
        PreloadedAppRecordMgr.getInstance().add(pi);
        GameSceneProxyManager.onProcessAttached(pi.mProcName, UserHandle.getUserId(pi.mUid));
        FrozenInjectorImpl.getInstance().addProcess(pi);
        return pi;
    }

    public void remove(ProcessInfo pi) {
        synchronized (this) {
            EventDispatcher.getInstance().remove(pi.mPid, pi.mKillReason);
            removeAppLocked(pi);
            pi.onDied();
        }
        AnimationManager.getInstance().removeHotStartDelayed(pi);
        PreloadedAppRecordMgr.getInstance().remove(pi);
        if (GameOptManager.isGamePlaying()) {
            GameOptManager.onProcDeath(pi.mPkgList);
        }
        if (EventNotifier.sUsingProcessName.equals(pi.mProcName)) {
            RMServer.getInstance().getEventNotifier().setDeathReason(pi.mKillReason);
            WorkingState.resetWorkingState();
        }
        FrozenInjectorImpl.getInstance().removeProcess(pi);
        VivoBinderProxy.getInstance().reportAppDied(pi.mPid);
    }

    public void setOomLocked(ProcessInfo pi, int oom) {
        if (pi.mOom != oom) {
            EventDispatcher.getInstance().setOom(pi.mPid, oom);
            pi.mOom = oom;
            pi.mOomProc = this.mProcs.get(oom);
        }
    }

    public void setAdjLocked(ProcessInfo pi, int adj) {
        if (pi.mAdj != adj) {
            pi.mAdj = adj;
            this.mPendingADJChange.put(pi.mPid, adj);
        }
    }

    public void setAdjTypeLocked(ProcessInfo pi, String adjType) {
        pi.mAdjType = adjType;
    }

    public void setSchedGroupLocked(ProcessInfo pi, int schedGroup) {
        if (pi.mSchedGroup != schedGroup) {
            pi.mSchedGroup = schedGroup;
            this.mPendingSchedChange.put(pi.mPid, schedGroup);
        }
    }

    public void addDepPkg(ProcessInfo pi, String pkg) {
        synchronized (this) {
            if (!pi.mDepPkgList.contains(pkg)) {
                EventDispatcher.getInstance().addDepPkg(pi.mPid, pkg);
                pi.addDepPkg(pkg);
            }
        }
    }

    public void addPkg(ProcessInfo pi, String pkg) {
        synchronized (this) {
            if (!pi.mPkgList.contains(pkg)) {
                EventDispatcher.getInstance().addPkg(pi.mPid, pkg);
                pi.addPkg(pkg);
            }
        }
    }

    public String[] getPackageList(ProcessInfo pi) {
        String[] packageList;
        synchronized (this) {
            packageList = pi.getPackageList();
        }
        return packageList;
    }

    public RecentTask getRecentTask() {
        return this.mRecentTask;
    }

    public void setFgActivityLocked(ProcessInfo pi, boolean has) {
        setStateLocked(pi, has, 1);
    }

    public void setFgServiceLocked(ProcessInfo pi, boolean has) {
        setStateLocked(pi, has, 4);
    }

    public void setFgForceLocked(ProcessInfo pi, boolean has) {
        setStateLocked(pi, has, 2);
    }

    public void setHasActivityLocked(ProcessInfo pi, boolean has) {
        setStateLocked(pi, has, 64);
    }

    public void setHasServiceLocked(ProcessInfo pi, boolean has) {
        setStateLocked(pi, has, 128);
    }

    public void setHasNotificationLocked(ProcessInfo pi, boolean has) {
        setStateLocked(pi, has, 256);
    }

    public void setPausing(ProcessInfo pi, boolean has) {
        setStateLocked(pi, has, 512);
    }

    private void setStateLocked(ProcessInfo pi, boolean has, int mask) {
        int state = has ? mask : 0;
        if ((pi.mStates & mask) != state) {
            pi.setState(state, mask);
            this.mPendingStateChange.putState(pi.mPid, state, mask);
            if ((mask & 1) != 0) {
                if ((state & 1) != 0) {
                    this.mRecentTask.remove(pi.mProcName, pi.mUid);
                } else if (pi.mAdj >= 0 && pi.mParent != null && pi.mPid != this.mHomeProcessPid && !OomPreviousList.excluded(pi.mProcName) && RMProcHelper.getBoolean(pi.mParent, 8) && !RmsInjectorImpl.getInstance().isRmsPreload(pi.mPkgName, pi.mUid)) {
                    this.mRecentTask.put(pi.mProcName, pi.mUid);
                }
            }
        }
    }

    public void addVirtualDisplay(String pkgName, int uid) {
        synchronized (this) {
            PackageInfo app = getAppLocked(pkgName, uid);
            if (app != null) {
                int i = app.mVDSize;
                app.mVDSize = i + 1;
                if (i == 0) {
                    app.setState(Consts.ProcessStates.VIRTUAL_DISPLAY, Consts.ProcessStates.VIRTUAL_DISPLAY);
                    Iterator<ProcessInfo> it = app.mProcs.iterator();
                    while (it.hasNext()) {
                        ProcessInfo pi = it.next();
                        setStateLocked(pi, true, Consts.ProcessStates.VIRTUAL_DISPLAY);
                        if (RmsInjectorImpl.getInstance().isCgrpFeatureEnable()) {
                            requestUpdateCgrp(pi);
                        }
                    }
                }
            }
        }
        FrozenInjectorImpl.getInstance().addVirtualDisplay(pkgName, uid);
    }

    public void removeVirtualDisplay(String pkgName, int uid) {
        synchronized (this) {
            PackageInfo app = getAppLocked(pkgName, uid);
            if (app != null) {
                int i = app.mVDSize - 1;
                app.mVDSize = i;
                if (i == 0) {
                    app.setState(0, Consts.ProcessStates.VIRTUAL_DISPLAY);
                    Iterator<ProcessInfo> it = app.mProcs.iterator();
                    while (it.hasNext()) {
                        ProcessInfo pi = it.next();
                        setStateLocked(pi, false, Consts.ProcessStates.VIRTUAL_DISPLAY);
                        if (RmsInjectorImpl.getInstance().isCgrpFeatureEnable()) {
                            requestUpdateCgrp(pi);
                        }
                    }
                }
            }
        }
        FrozenInjectorImpl.getInstance().removeVirtualDisplay(pkgName, uid);
    }

    public void updateWorkingState(String pkgName, int uid, int mask, boolean state) {
        synchronized (this) {
            PackageInfo app = null;
            if (pkgName == null) {
                ArrayList<PackageInfo> appList = this.mApps.get(uid);
                if (appList != null && appList.size() == 1) {
                    app = appList.get(0);
                }
            } else {
                app = getAppLocked(pkgName, uid);
            }
            if (app != null) {
                app.setWorkingState(state ? mask : 0, mask);
                if (RmsInjectorImpl.getInstance().isCgrpFeatureEnable()) {
                    Iterator<ProcessInfo> it = app.mProcs.iterator();
                    while (it.hasNext()) {
                        ProcessInfo pi = it.next();
                        if (!state || pi.mCgrpGroup < 4) {
                            requestUpdateCgrp(pi);
                        }
                    }
                }
                VLog.d(TAG, String.format("updateWorkingState %s [%s]->%s ", pkgName, WorkingState.getName(mask), String.valueOf(state)));
            }
        }
    }

    public void resetWorkingState() {
        synchronized (this) {
            for (int i = 0; i < this.mApps.size(); i++) {
                Iterator<PackageInfo> it = this.mApps.valueAt(i).iterator();
                while (it.hasNext()) {
                    PackageInfo app = it.next();
                    app.resetWorkingState();
                }
            }
        }
    }

    public void updateWindowFocus(WindowState newFocus, WindowState oldFocus) {
        synchronized (this) {
            int newPid = -1;
            int oldPid = -1;
            if (newFocus != null) {
                try {
                    newPid = RMWms.getInstance().getOwnerPid(newFocus);
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (oldFocus != null) {
                oldPid = RMWms.getInstance().getOwnerPid(oldFocus);
            }
            if (newPid != oldPid) {
                setFocusLocked(newPid, true);
                setFocusLocked(oldPid, false);
            }
        }
    }

    private void setFocusLocked(int pid, boolean focus) {
        ProcessInfo newProc;
        if (pid > 0 && (newProc = this.mProcs.get(pid)) != null) {
            setStateLocked(newProc, focus, Consts.ProcessStates.FOCUS);
            if (RmsInjectorImpl.getInstance().isCgrpFeatureEnable()) {
                requestUpdateCgrp(newProc);
            }
        }
    }

    public ProcessInfo addWindow(int pid, WindowState win) {
        ProcessInfo pi;
        synchronized (this) {
            pi = this.mProcs.get(pid);
            if (pi != null && !pi.hasWindow(win)) {
                pi.mWindows.add(win);
                if ((pi.mStates & 8) == 0) {
                    setStateLocked(pi, true, 8);
                    setStateLocked(pi, true, 32);
                    this.mPendingStateChange.apply();
                }
            }
        }
        return pi;
    }

    public ProcessInfo removeWindow(int pid, WindowState win) {
        ProcessInfo pi;
        synchronized (this) {
            pi = this.mProcs.get(pid);
            if (pi != null && pi.hasWindow(win)) {
                pi.mWindows.remove(win);
                if (pi.mWindows.isEmpty() && (pi.mStates & 8) != 0) {
                    setStateLocked(pi, false, 8);
                    pi.mLastInvisibleTime = SystemClock.uptimeMillis();
                    this.mPendingStateChange.apply();
                }
            }
        }
        return pi;
    }

    public boolean inBuildInDisplay(ProcessInfo pi) {
        boolean inBuildInDisplayLocked;
        synchronized (this) {
            inBuildInDisplayLocked = pi.inBuildInDisplayLocked();
        }
        return inBuildInDisplayLocked;
    }

    private void addAppLocked(ProcessInfo pi) {
        ArrayList<PackageInfo> appList = this.mApps.get(pi.mUid);
        if (appList == null) {
            appList = new ArrayList<>();
            this.mApps.put(pi.mUid, appList);
        }
        PackageInfo app = null;
        Iterator<PackageInfo> it = appList.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PackageInfo p = it.next();
            if (p.mPkgName.equals(pi.mPkgName)) {
                app = p;
                break;
            }
        }
        if (app == null) {
            app = new PackageInfo(pi.mPkgName, pi.mUid);
            appList.add(app);
        }
        app.addProc(pi);
        this.mProcs.put(pi.mPid, pi);
        pi.makeActive(app);
    }

    private void removeAppLocked(ProcessInfo pi) {
        pi.mAlive = false;
        this.mProcs.remove(pi.mPid);
        ArrayList<PackageInfo> appList = this.mApps.get(pi.mUid);
        if (appList == null) {
            VLog.e(TAG, "removeApp error,proc can't find in mApps");
            return;
        }
        PackageInfo app = null;
        Iterator<PackageInfo> it = appList.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PackageInfo p = it.next();
            if (p.mPkgName.equals(pi.mPkgName)) {
                app = p;
                break;
            }
        }
        if (app == null) {
            VLog.e(TAG, "removeApp error,proc can't find in appList");
            return;
        }
        app.removeProc(pi);
        if (app.isEmpty()) {
            appList.remove(app);
            if (appList.isEmpty()) {
                this.mApps.remove(pi.mUid);
            }
        }
    }

    private PackageInfo getAppLocked(String pkgName, int uid) {
        ArrayList<PackageInfo> appList = this.mApps.get(uid);
        if (appList == null) {
            return null;
        }
        Iterator<PackageInfo> it = appList.iterator();
        while (it.hasNext()) {
            PackageInfo p = it.next();
            if (p.mPkgName.equals(pkgName)) {
                return p;
            }
        }
        return null;
    }

    public void applyPenddings() {
        synchronized (this) {
            this.mPendingADJChange.apply();
            this.mPendingStateChange.apply();
            this.mPendingSchedChange.apply();
        }
    }

    public void setHomeProcessPid(int pid) {
        this.mHomeProcessPid = pid;
    }

    public int getHomeProcessPid() {
        return this.mHomeProcessPid;
    }

    public void dump(PrintWriter pw) {
        synchronized (this) {
            StringBuilder builder = new StringBuilder(128);
            for (int i = 0; i < this.mApps.size(); i++) {
                Iterator<PackageInfo> it = this.mApps.valueAt(i).iterator();
                while (it.hasNext()) {
                    PackageInfo app = it.next();
                    builder.setLength(0);
                    app.stringBuilder(builder);
                    pw.println(builder.toString());
                }
            }
            this.mRecentTask.dump(pw);
            pw.println("*Total Size=" + this.mProcs.size());
        }
    }

    public final void requestUpdateCgrp(ProcessInfo pi) {
        OomHandler oomHandler;
        if (pi != null && pi.isAlive() && (oomHandler = this.mOomHandler) != null) {
            oomHandler.requestUpdateCgrp(pi);
        }
    }

    public final void requestUpdateCgrp(String reason) {
        OomHandler oomHandler = this.mOomHandler;
        if (oomHandler != null) {
            oomHandler.requestUpdateCgrp(reason);
        }
    }

    public SparseArray<ProcessInfo> getProcsLocked() {
        return this.mProcs;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class OomHandler extends Handler {
        private static final int UPDATE_ALL = 1;
        private static final int UPDATE_PROCESS = 0;

        private OomHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.arg1 == 0) {
                ProcessInfo pi = (ProcessInfo) msg.obj;
                CgrpController.getInstance().applyCgrpPolicy(pi);
            } else if (msg.arg1 == 1) {
                String reason = (String) msg.obj;
                CgrpController.getInstance().applyCgrpPolicy(reason);
            }
        }

        public final void requestUpdateCgrp(ProcessInfo pi) {
            if (pi.mPid != 0) {
                removeMessages(pi.mPid);
                sendMessage(obtainMessage(pi.mPid, 0, 0, pi));
            }
        }

        public final void requestUpdateCgrp(String reason) {
            removeMessages(0);
            sendMessage(obtainMessage(0, 1, 0, reason));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class IntValueChangeItem {
        private final int mEventType;
        private final SparseArray<Integer> mValues = new SparseArray<>(12);

        public IntValueChangeItem(int type) {
            this.mEventType = type;
        }

        public void put(int pid, int value) {
            this.mValues.put(pid, Integer.valueOf(value));
        }

        public void apply() {
            int length = this.mValues.size();
            if (length == 0) {
                return;
            }
            int[] pids = IntArrayFactory.create(length);
            int[] values = IntArrayFactory.create(length);
            for (int i = 0; i < length; i++) {
                pids[i] = this.mValues.keyAt(i);
                values[i] = this.mValues.valueAt(i).intValue();
            }
            this.mValues.clear();
            int i2 = this.mEventType;
            if (i2 == 0) {
                EventDispatcher.getInstance().setAdj(pids, values);
            } else if (i2 == 2) {
                EventDispatcher.getInstance().setSchedGroup(pids, values);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class StateChangeItem {
        private final SparseArray<Integer> mMasks;
        private final SparseArray<Integer> mStates;

        private StateChangeItem() {
            this.mStates = new SparseArray<>(5);
            this.mMasks = new SparseArray<>(5);
        }

        public void putState(int pid, int state, int mask) {
            int masks;
            int states;
            int states2 = this.mStates.get(pid, 0).intValue();
            int masks2 = this.mMasks.get(pid, 0).intValue();
            if ((masks2 & mask) != 0) {
                states = states2 & (~mask);
                masks = masks2 & (~mask);
            } else {
                masks = masks2 | mask;
                states = ((~mask) & states2) | (state & mask);
            }
            this.mStates.put(pid, Integer.valueOf(states));
            this.mMasks.put(pid, Integer.valueOf(masks));
        }

        public void apply() {
            int length = this.mStates.size();
            if (length == 0) {
                return;
            }
            int[] pids = IntArrayFactory.create(length);
            int[] states = IntArrayFactory.create(length);
            int[] masks = IntArrayFactory.create(length);
            for (int i = 0; i < length; i++) {
                pids[i] = this.mStates.keyAt(i);
                states[i] = this.mStates.valueAt(i).intValue();
                masks[i] = this.mMasks.valueAt(i).intValue();
            }
            this.mStates.clear();
            this.mMasks.clear();
            EventDispatcher.getInstance().setStates(pids, states, masks);
        }
    }
}