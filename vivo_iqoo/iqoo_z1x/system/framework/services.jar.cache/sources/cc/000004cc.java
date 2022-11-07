package com.android.server;

import android.util.SparseBooleanArray;

/* loaded from: classes.dex */
public class NetIdManager {
    public static final int MAX_NET_ID = 64511;
    public static final int MIN_NET_ID = 100;
    private int mLastNetId;
    private final int mMaxNetId;
    private final SparseBooleanArray mNetIdInUse;

    public NetIdManager() {
        this(MAX_NET_ID);
    }

    NetIdManager(int maxNetId) {
        this.mNetIdInUse = new SparseBooleanArray();
        this.mLastNetId = 99;
        this.mMaxNetId = maxNetId;
    }

    private int getNextAvailableNetIdLocked(int lastId, SparseBooleanArray netIdInUse) {
        int netId = lastId;
        int i = 100;
        while (true) {
            int i2 = this.mMaxNetId;
            if (i <= i2) {
                netId = netId < i2 ? netId + 1 : 100;
                if (netIdInUse.get(netId)) {
                    i++;
                } else {
                    return netId;
                }
            } else {
                throw new IllegalStateException("No free netIds");
            }
        }
    }

    public int reserveNetId() {
        int i;
        synchronized (this.mNetIdInUse) {
            int nextAvailableNetIdLocked = getNextAvailableNetIdLocked(this.mLastNetId, this.mNetIdInUse);
            this.mLastNetId = nextAvailableNetIdLocked;
            this.mNetIdInUse.put(nextAvailableNetIdLocked, true);
            i = this.mLastNetId;
        }
        return i;
    }

    public void releaseNetId(int id) {
        synchronized (this.mNetIdInUse) {
            this.mNetIdInUse.delete(id);
        }
    }
}