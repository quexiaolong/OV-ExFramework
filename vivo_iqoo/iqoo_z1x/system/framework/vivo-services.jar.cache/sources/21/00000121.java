package com.android.server.am.frozen;

import android.content.ComponentName;
import android.content.Context;
import android.hardware.graphics.common.V1_0.BufferUsage;
import android.os.Bundle;
import android.os.Process;
import com.android.server.IVivoFrozenInjector;
import com.vivo.services.rms.ProcessInfo;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

/* loaded from: classes.dex */
public class FrozenInjectorImpl implements IVivoFrozenInjector {
    private static final String TAG = "Quick-Frozen";
    private Context mContext;
    public static final int MY_PID = Process.myPid();
    private static FrozenInjectorImpl INSTANCE = null;
    private WorkingStateManager mWorkingStateManager = WorkingStateManager.getInstance();
    private FrozenQuicker mFrozenQuicker = FrozenQuicker.getInstance();
    private boolean mMonkeyState = false;
    private boolean mInited = false;

    private FrozenInjectorImpl() {
    }

    public static synchronized FrozenInjectorImpl getInstance() {
        FrozenInjectorImpl frozenInjectorImpl;
        synchronized (FrozenInjectorImpl.class) {
            if (INSTANCE == null) {
                INSTANCE = new FrozenInjectorImpl();
            }
            frozenInjectorImpl = INSTANCE;
        }
        return frozenInjectorImpl;
    }

    public void initialize(Context context) {
        this.mContext = context;
        this.mFrozenQuicker.initialize(context);
        this.mInited = true;
    }

    public void setHomeProcess(int pid) {
        if (!this.mInited) {
            return;
        }
        this.mFrozenQuicker.setHomeProcess(pid);
    }

    public void removeProcess(ProcessInfo proc) {
        this.mFrozenQuicker.removeProcess(proc);
    }

    public void addProcess(ProcessInfo proc) {
        this.mFrozenQuicker.addProcess(proc);
    }

    public void addVirtualDisplay(String pkgName, int uid) {
        this.mFrozenQuicker.addVirtualDisplay(pkgName, uid);
    }

    public void removeVirtualDisplay(String pkgName, int uid) {
        this.mFrozenQuicker.removeVirtualDisplay(pkgName, uid);
    }

    public void notifyFrozenSpecial(int type) {
        this.mFrozenQuicker.setQuickFrozenEnable(false, BufferUsage.COMPOSER_OVERLAY);
    }

    public void reportFreezeStatus(int uid, String procName, int pid, boolean allowFreeze, int procState) {
        if (!this.mInited) {
            return;
        }
        this.mFrozenQuicker.reportFreezeStatusAsync(uid, procName, pid, allowFreeze, procState);
    }

    public void setWorkingState(int model, int state, int uid) {
        this.mWorkingStateManager.setState(model, state, uid);
    }

    public void setWorkingState(int model, int state, int uid, int pid) {
        this.mWorkingStateManager.setState(model, state, uid, pid);
    }

    public void reportWallPaperService(ComponentName wallpaper) {
        this.mFrozenQuicker.reportWallPaperService(wallpaper);
    }

    public boolean isWallpaperService(String pkgName) {
        return this.mFrozenQuicker.isWallpaperService(pkgName);
    }

    public void setPhoneState(int state) {
        this.mFrozenQuicker.setPhoneState(state);
    }

    public void noteScreenState(int state) {
    }

    public void updateInputMethod(String inputMethod) {
        this.mFrozenQuicker.updateInputMethod(inputMethod);
    }

    public boolean isAudioOn(int uid, String pkgName) {
        return this.mFrozenQuicker.isAudioOn(uid, pkgName);
    }

    public boolean isCurrentInputMethod(String pkgName) {
        return this.mFrozenQuicker.isCurrentInputMethod(pkgName);
    }

    public boolean isInBlackList(String packageName) {
        return this.mFrozenQuicker.isInBlackList(packageName);
    }

    public void setQuickFrozenEnable(boolean frozenEnable, long downloadThd) {
        this.mFrozenQuicker.setQuickFrozenEnable(frozenEnable, downloadThd);
    }

    public void notifyQuickFrozenPause(boolean pause) {
        this.mFrozenQuicker.notifyQuickFrozenPause(pause);
    }

    public HashSet<String> getDefaultBackList() {
        return this.mFrozenQuicker.getDefaultBackList();
    }

    public void dumpQuickFrozenInformation(PrintWriter pw, String[] args, int opti) {
        if (!this.mInited) {
            return;
        }
        this.mFrozenQuicker.dumpQuickFrozenInformation(pw, args, opti);
    }

    public boolean setFrozenPkgBlacklist(List<String> pkgNames, int len) {
        return this.mFrozenQuicker.setFrozenPkgBlacklist(pkgNames, len);
    }

    public boolean setFrozenPkgWhitelist(List<String> pkgNames, int len) {
        return this.mFrozenQuicker.setFrozenPkgWhitelist(pkgNames, len);
    }

    public boolean setPackageList(String type, Bundle data) {
        return this.mFrozenQuicker.setPackageList(type, data);
    }

    public void systemReady() {
        this.mFrozenQuicker.systemReady();
    }

    public void addWindow(String pkgName, int uid) {
        this.mFrozenQuicker.addWindow(pkgName, uid);
    }
}