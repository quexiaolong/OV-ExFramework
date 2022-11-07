package com.android.server.pm;

import android.os.Binder;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class KeySetHandle extends Binder {
    private final long mId;
    private int mRefCount;

    /* JADX INFO: Access modifiers changed from: protected */
    public KeySetHandle(long id) {
        this.mId = id;
        this.mRefCount = 1;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public KeySetHandle(long id, int refCount) {
        this.mId = id;
        this.mRefCount = refCount;
    }

    public long getId() {
        return this.mId;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getRefCountLPr() {
        return this.mRefCount;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setRefCountLPw(int newCount) {
        this.mRefCount = newCount;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void incrRefCountLPw() {
        this.mRefCount++;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int decrRefCountLPw() {
        int i = this.mRefCount - 1;
        this.mRefCount = i;
        return i;
    }
}