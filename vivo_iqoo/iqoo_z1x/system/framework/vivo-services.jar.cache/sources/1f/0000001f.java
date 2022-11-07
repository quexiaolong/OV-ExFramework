package com.android.server;

/* loaded from: classes.dex */
public abstract class RmsKeepQuietListener {
    public final String owner;
    public final int ownerFlag;

    public abstract void onQuietStateChanged(String str, int i, boolean z);

    public RmsKeepQuietListener(String owner, int ownerFlag) {
        this.owner = owner;
        this.ownerFlag = ownerFlag;
    }
}