package com.android.server.tv.tunerresourcemanager;

/* loaded from: classes2.dex */
public class TunerResourceBasic {
    final int mId;
    boolean mIsInUse;
    int mOwnerClientId = -1;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TunerResourceBasic(Builder builder) {
        this.mId = builder.mId;
    }

    public int getId() {
        return this.mId;
    }

    public boolean isInUse() {
        return this.mIsInUse;
    }

    public int getOwnerClientId() {
        return this.mOwnerClientId;
    }

    public void setOwner(int ownerClientId) {
        this.mIsInUse = true;
        this.mOwnerClientId = ownerClientId;
    }

    public void removeOwner() {
        this.mIsInUse = false;
        this.mOwnerClientId = -1;
    }

    /* loaded from: classes2.dex */
    public static class Builder {
        private final int mId;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(int id) {
            this.mId = id;
        }

        public TunerResourceBasic build() {
            TunerResourceBasic resource = new TunerResourceBasic(this);
            return resource;
        }
    }
}