package com.vivo.services.rms;

import android.os.Bundle;
import android.os.SystemClock;
import com.android.server.am.ProcessRecord;
import com.android.server.am.RMProcHelper;
import com.android.server.am.RMProcInfo;
import com.android.server.am.frozen.FrozenAppRecord;
import com.android.server.wm.WindowState;
import com.vivo.services.rms.appmng.WorkingState;
import com.vivo.services.rms.cgrp.CgrpPolicy;
import com.vivo.services.rms.display.RefreshRateAdjuster;
import com.vivo.services.rms.sdk.Consts;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class ProcessInfo extends RMProcInfo {
    private static int ACTIVE_MASKS = 3087;
    public ProcessInfo mOomProc;
    public PackageInfo mOwner;
    public FrozenAppRecord mRecord;
    private final ArrayList<String> mRefreshRateHandles = new ArrayList<>();
    private final ArrayList<StateChangeListener> mStateChangeListeners = new ArrayList<>();

    /* loaded from: classes.dex */
    public interface StateChangeListener {
        default void onStateChanged(int state, boolean hasState, ProcessInfo processInfo) {
        }
    }

    public ProcessInfo(ProcessRecord parent, int uid, String pkgName, int pkgFlags, String process) {
        this.mParent = parent;
        this.mUid = uid;
        this.mPkgName = pkgName;
        this.mPkgFlags = pkgFlags;
        this.mProcName = process;
    }

    public void makeActive(PackageInfo owner) {
        this.mLastActiveElapsedTime = SystemClock.elapsedRealtime();
        this.mLastActiveTime = SystemClock.uptimeMillis();
        this.mLastInvisibleTime = this.mLastActiveTime;
        this.mPkgList.clear();
        this.mDepPkgList.clear();
        this.mPkgList.add(this.mPkgName);
        this.mKillReason = null;
        this.mOwner = owner;
        this.mStates = owner.mStates;
        this.mAlive = true;
    }

    private void reset() {
        this.mPkgList.clear();
        this.mDepPkgList.clear();
        this.mRefreshRateHandles.clear();
        this.mStateChangeListeners.clear();
        this.needKeepQuiet = false;
        this.rmsPreloaded = false;
        this.mAdj = 1001;
        this.mSchedGroup = -1;
        this.mOriginGroup = -1;
        this.mBinderGroup = 0;
        this.mBinderSetGroup = 0;
        this.mOom = -1;
        this.mOomProc = null;
        this.mStates = 0;
        this.mWindows.clear();
    }

    public void onDied() {
        clearRefreshRateHandles();
        synchronized (this.mStateChangeListeners) {
            this.mStateChangeListeners.clear();
        }
        reset();
    }

    public void addStateChangedListener(StateChangeListener l) {
        synchronized (this.mStateChangeListeners) {
            if (!this.mStateChangeListeners.contains(l)) {
                this.mStateChangeListeners.add(l);
            }
        }
    }

    public void removeStateChangedListener(StateChangeListener l) {
        synchronized (this.mStateChangeListeners) {
            this.mStateChangeListeners.remove(l);
        }
    }

    private void notifyStateChanged(int mask, boolean hasState) {
        synchronized (this.mStateChangeListeners) {
            if (this.mStateChangeListeners.isEmpty()) {
                return;
            }
            Iterator<StateChangeListener> it = this.mStateChangeListeners.iterator();
            while (it.hasNext()) {
                StateChangeListener l = it.next();
                l.onStateChanged(mask, hasState, this);
            }
        }
    }

    public void addRefreshRateHandle(String handle) {
        synchronized (this.mRefreshRateHandles) {
            this.mRefreshRateHandles.add(handle);
        }
    }

    public void removeRefreshRateHandle(String handle) {
        synchronized (this.mRefreshRateHandles) {
            this.mRefreshRateHandles.remove(handle);
        }
    }

    public int sizeOfRefreshRateHandle() {
        int size;
        synchronized (this.mRefreshRateHandles) {
            size = this.mRefreshRateHandles.size();
        }
        return size;
    }

    private void clearRefreshRateHandles() {
        ArrayList<String> handles = null;
        synchronized (this.mRefreshRateHandles) {
            if (!this.mRefreshRateHandles.isEmpty()) {
                handles = new ArrayList<>(this.mRefreshRateHandles);
                this.mRefreshRateHandles.clear();
            }
        }
        if (handles != null) {
            RefreshRateAdjuster.getInstance().clearDeathHandles(this.mPid, handles);
        }
    }

    public void setState(int state, int mask) {
        this.mStates = (this.mStates & (~mask)) | (state & mask);
        if ((ACTIVE_MASKS & mask) != 0) {
            this.mLastActiveElapsedTime = SystemClock.elapsedRealtime();
            this.mLastActiveTime = SystemClock.uptimeMillis();
        }
        if ((mask & 1065) != 0) {
            notifyStateChanged(mask, state == 0);
        }
    }

    public boolean isVisible() {
        return (this.mStates & 8) != 0;
    }

    public boolean isWorking() {
        PackageInfo packageInfo = this.mOwner;
        return (packageInfo == null || packageInfo.mWorkingState == 0) ? false : true;
    }

    public boolean isWorkingExceptDownload() {
        PackageInfo packageInfo = this.mOwner;
        return (packageInfo == null || (packageInfo.mWorkingState & (-5)) == 0) ? false : true;
    }

    public boolean isDownload() {
        PackageInfo packageInfo = this.mOwner;
        return (packageInfo == null || (packageInfo.mWorkingState & 4) == 0) ? false : true;
    }

    public boolean isMedia() {
        PackageInfo packageInfo = this.mOwner;
        return (packageInfo == null || (packageInfo.mWorkingState & 3) == 0) ? false : true;
    }

    public boolean isAudio() {
        PackageInfo packageInfo = this.mOwner;
        return (packageInfo == null || (packageInfo.mWorkingState & 2) == 0) ? false : true;
    }

    public boolean hasFocus() {
        return (this.mStates & Consts.ProcessStates.FOCUS) != 0;
    }

    public boolean hasFgService() {
        return (this.mStates & 4) != 0;
    }

    public boolean hasShownUi() {
        return (this.mStates & 32) != 0;
    }

    public boolean isFgActivity() {
        return (this.mStates & 1) != 0;
    }

    public boolean hasVirualDisplay() {
        return (this.mStates & Consts.ProcessStates.VIRTUAL_DISPLAY) != 0;
    }

    public boolean inBuildInDisplayLocked() {
        if (this.mWindows.isEmpty()) {
            return true;
        }
        Iterator<WindowState> it = this.mWindows.iterator();
        return !it.hasNext() || it.next().getDisplayId() == 0;
    }

    public boolean isAlive() {
        return this.mAlive && this.mPid > 0;
    }

    public boolean isSystemApp() {
        return (this.mPkgFlags & 1) != 0;
    }

    public boolean isBackUpApp() {
        return "backup".equals(this.mAdjType);
    }

    public boolean isHeavyApp() {
        return this.mAdj <= 401;
    }

    public boolean isOomByFg() {
        ProcessInfo processInfo = this.mOomProc;
        return processInfo != null && processInfo.hasFocus() && this.mOomProc.isVisible();
    }

    public boolean isOomByPersistent() {
        ProcessInfo processInfo = this.mOomProc;
        return processInfo != null && processInfo.mAdj < 0;
    }

    public boolean hasStates(int states) {
        return (this.mStates & states) == states;
    }

    public boolean isRunningRemoteAnimation() {
        return RMProcHelper.isRunningRemoteAnimation(this.mParent);
    }

    /* JADX WARN: Code restructure failed: missing block: B:15:?, code lost:
        return r1;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public com.vivo.services.rms.ProcessInfo getRootOom() {
        /*
            r4 = this;
            com.vivo.services.rms.ProcessInfo r0 = r4.mOomProc
            r1 = r0
            r2 = 0
        L4:
            if (r0 == 0) goto L12
            if (r0 == r4) goto L12
            r3 = 10
            if (r2 > r3) goto L12
            r1 = r0
            com.vivo.services.rms.ProcessInfo r0 = r0.mOomProc
            int r2 = r2 + 1
            goto L4
        L12:
            if (r0 == r4) goto L16
            r3 = r1
            goto L17
        L16:
            r3 = 0
        L17:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.rms.ProcessInfo.getRootOom():com.vivo.services.rms.ProcessInfo");
    }

    public long getInvisibleTime() {
        if (isVisible()) {
            return 0L;
        }
        return SystemClock.uptimeMillis() - this.mLastInvisibleTime;
    }

    public long getInactiveTime() {
        if (isVisible()) {
            return 0L;
        }
        return SystemClock.elapsedRealtime() - this.mLastActiveElapsedTime;
    }

    public boolean hasWindow(WindowState win) {
        return this.mWindows.contains(win);
    }

    public void addDepPkg(String pkg) {
        this.mDepPkgList.add(pkg);
    }

    public void addPkg(String pkg) {
        this.mPkgList.add(pkg);
    }

    public String[] getPackageList() {
        int size = this.mPkgList.size();
        if (size == 0) {
            return new String[]{this.mPkgName};
        }
        String[] list = new String[size];
        for (int i = 0; i < this.mPkgList.size(); i++) {
            list[i] = this.mPkgList.get(i);
        }
        return list;
    }

    public Bundle toBundleLocked() {
        Bundle data = new Bundle();
        data.putInt("uid", this.mUid);
        data.putString("pkg", this.mPkgName);
        data.putInt("pkgFlags", this.mPkgFlags);
        data.putString("name", this.mProcName);
        data.putInt("pid", this.mPid);
        data.putString("createReason", this.mCreateReason);
        data.putLong("lastActiveElapsedTime", this.mLastActiveElapsedTime);
        data.putLong("lastInvisibleTime", this.mLastInvisibleTime);
        data.putLong("lastActiveTime", this.mLastActiveTime);
        data.putInt("adj", this.mAdj);
        data.putInt("schedGroup", this.mSchedGroup);
        data.putInt("oom", this.mOom);
        data.putInt("states", this.mStates);
        data.putStringArrayList("depPkgList", this.mDepPkgList);
        data.putStringArrayList("pkgList", this.mPkgList);
        return data;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(128);
        stringBuilder(builder);
        return builder.toString();
    }

    public String makeSchedString() {
        return String.format("%s->%s->%s", CgrpPolicy.group2String(this.mSchedGroup), CgrpPolicy.group2String(this.mOriginGroup), CgrpPolicy.policy2String(this.mCgrpGroup));
    }

    public void stringBuilder(StringBuilder builder) {
        builder.append("*");
        builder.append(this.mProcName);
        builder.append(" uid:" + this.mUid);
        builder.append(" pid:" + this.mPid);
        builder.append(" adj:" + this.mAdj);
        builder.append(" adjType:" + this.mAdjType);
        builder.append(String.format(" binder[%d %d]", Integer.valueOf(this.mBinderGroup), Integer.valueOf(this.mBinderSetGroup)));
        builder.append(" sched:" + makeSchedString());
        builder.append(" invisible:" + (getInvisibleTime() / 1000));
        builder.append("s");
        builder.append(" inactive:" + (getInactiveTime() / 1000));
        builder.append("s");
        ProcessInfo processInfo = this.mOomProc;
        if (processInfo != null) {
            builder.append(String.format("\n\t      oom:%s[%d, %d]", processInfo.mProcName, Integer.valueOf(this.mOomProc.mPid), Integer.valueOf(this.mOomProc.mAdj)));
        }
        ProcessInfo rootOom = getRootOom();
        if (rootOom != null && rootOom != this.mOomProc) {
            builder.append(String.format("\n\t      rootOom:%s[%d, %d]", rootOom.mProcName, Integer.valueOf(rootOom.mPid), Integer.valueOf(rootOom.mAdj)));
        }
        builder.append("\n\t      pkgList:" + this.mPkgList.toString());
        if (this.mStates != 0) {
            builder.append(String.format("\n\t      states:[%s]", Consts.ProcessStates.getName(this.mStates)));
        }
        PackageInfo packageInfo = this.mOwner;
        if (packageInfo != null && packageInfo.mWorkingState != 0) {
            builder.append(String.format("\n\t      workingStates:[%s]", WorkingState.getName(this.mOwner.mWorkingState)));
        }
        if (this.mCreateReason != null) {
            builder.append("\n\t      create reason:" + this.mCreateReason);
        }
        if (!this.mDepPkgList.isEmpty()) {
            builder.append("\n\t      depPkgList:" + this.mDepPkgList.toString());
        }
        synchronized (this.mRefreshRateHandles) {
            if (!this.mRefreshRateHandles.isEmpty()) {
                builder.append("\n\t      refreshHandles:" + this.mRefreshRateHandles);
            }
        }
    }
}