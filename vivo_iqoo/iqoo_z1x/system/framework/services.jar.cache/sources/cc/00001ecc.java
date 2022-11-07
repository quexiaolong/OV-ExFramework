package com.android.server.wm;

import android.util.SparseIntArray;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class MirrorActiveUids {
    private SparseIntArray mUidStates = new SparseIntArray();

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onUidActive(int uid, int procState) {
        this.mUidStates.put(uid, procState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onUidInactive(int uid) {
        this.mUidStates.delete(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onActiveUidsCleared() {
        this.mUidStates.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onUidProcStateChanged(int uid, int procState) {
        int index = this.mUidStates.indexOfKey(uid);
        if (index >= 0) {
            this.mUidStates.setValueAt(index, procState);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized int getUidState(int uid) {
        return this.mUidStates.get(uid, 20);
    }
}