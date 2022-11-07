package com.android.server.am;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AppErrorResult {
    boolean mHasResult = false;
    int mResult;

    public void set(int res) {
        synchronized (this) {
            this.mHasResult = true;
            this.mResult = res;
            notifyAll();
        }
    }

    public int get() {
        synchronized (this) {
            while (!this.mHasResult) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return this.mResult;
    }
}