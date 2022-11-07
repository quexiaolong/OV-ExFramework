package com.vivo.services.rms;

import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class PackageInfo {
    public String mPkgName;
    public final ArrayList<ProcessInfo> mProcs = new ArrayList<>();
    public int mStates;
    public int mUid;
    public int mVDSize;
    public int mWorkingState;

    public PackageInfo(String pkg, int uid) {
        this.mPkgName = pkg;
        this.mUid = uid;
    }

    public void setState(int state, int mask) {
        this.mStates = (this.mStates & (~mask)) | (state & mask);
    }

    public void setWorkingState(int state, int mask) {
        this.mWorkingState = (this.mWorkingState & (~mask)) | (state & mask);
    }

    public void resetWorkingState() {
        this.mWorkingState = 0;
    }

    public void addProc(ProcessInfo proc) {
        this.mProcs.add(proc);
    }

    public void removeProc(ProcessInfo proc) {
        this.mProcs.remove(proc);
    }

    public ProcessInfo findProcess(int pid) {
        Iterator<ProcessInfo> it = this.mProcs.iterator();
        while (it.hasNext()) {
            ProcessInfo r = it.next();
            if (r.mPid == pid) {
                return r;
            }
        }
        return null;
    }

    public boolean isTopApp() {
        Iterator<ProcessInfo> it = this.mProcs.iterator();
        while (it.hasNext()) {
            ProcessInfo r = it.next();
            if (r.isFgActivity() && r.isVisible()) {
                return true;
            }
        }
        return false;
    }

    public ProcessInfo findProc(String procName) {
        Iterator<ProcessInfo> it = this.mProcs.iterator();
        while (it.hasNext()) {
            ProcessInfo r = it.next();
            if (r.mProcName.equals(procName)) {
                return r;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return this.mProcs.isEmpty();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(256);
        stringBuilder(builder);
        return builder.toString();
    }

    public void stringBuilder(StringBuilder builder) {
        for (int i = 0; i < this.mProcs.size(); i++) {
            this.mProcs.get(i).stringBuilder(builder);
            builder.append('\n');
        }
        int i2 = builder.length();
        builder.setLength(i2 - 1);
    }

    public ArrayList<ProcessInfo> getProcs() {
        return this.mProcs;
    }
}