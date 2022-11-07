package com.android.server.tv.tunerresourcemanager;

import com.android.server.tv.tunerresourcemanager.TunerResourceBasic;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/* loaded from: classes2.dex */
public final class FrontendResource extends TunerResourceBasic {
    private final int mExclusiveGroupId;
    private Set<Integer> mExclusiveGroupMemberFeIds;
    private final int mType;

    private FrontendResource(Builder builder) {
        super(builder);
        this.mExclusiveGroupMemberFeIds = new HashSet();
        this.mType = builder.mType;
        this.mExclusiveGroupId = builder.mExclusiveGroupId;
    }

    public int getType() {
        return this.mType;
    }

    public int getExclusiveGroupId() {
        return this.mExclusiveGroupId;
    }

    public Set<Integer> getExclusiveGroupMemberFeIds() {
        return this.mExclusiveGroupMemberFeIds;
    }

    public void addExclusiveGroupMemberFeId(int id) {
        this.mExclusiveGroupMemberFeIds.add(Integer.valueOf(id));
    }

    public void addExclusiveGroupMemberFeIds(Collection<Integer> ids) {
        this.mExclusiveGroupMemberFeIds.addAll(ids);
    }

    public void removeExclusiveGroupMemberFeId(int id) {
        this.mExclusiveGroupMemberFeIds.remove(Integer.valueOf(id));
    }

    public String toString() {
        return "FrontendResource[id=" + this.mId + ", type=" + this.mType + ", exclusiveGId=" + this.mExclusiveGroupId + ", exclusiveGMemeberIds=" + this.mExclusiveGroupMemberFeIds + ", isInUse=" + this.mIsInUse + ", ownerClientId=" + this.mOwnerClientId + "]";
    }

    /* loaded from: classes2.dex */
    public static class Builder extends TunerResourceBasic.Builder {
        private int mExclusiveGroupId;
        private int mType;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(int id) {
            super(id);
        }

        public Builder type(int type) {
            this.mType = type;
            return this;
        }

        public Builder exclusiveGroupId(int exclusiveGroupId) {
            this.mExclusiveGroupId = exclusiveGroupId;
            return this;
        }

        @Override // com.android.server.tv.tunerresourcemanager.TunerResourceBasic.Builder
        public FrontendResource build() {
            FrontendResource frontendResource = new FrontendResource(this);
            return frontendResource;
        }
    }
}