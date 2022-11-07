package com.android.server.tv.tunerresourcemanager;

import java.util.HashSet;
import java.util.Set;

/* loaded from: classes2.dex */
public final class ClientProfile {
    public static final int INVALID_GROUP_ID = -1;
    public static final int INVALID_RESOURCE_ID = -1;
    private int mGroupId;
    private final int mId;
    private int mNiceValue;
    private int mPriority;
    private final int mProcessId;
    private final String mTvInputSessionId;
    private final int mUseCase;
    private int mUsingCasSystemId;
    private Set<Integer> mUsingFrontendIds;
    private Set<Integer> mUsingLnbIds;

    private ClientProfile(Builder builder) {
        this.mGroupId = -1;
        this.mUsingFrontendIds = new HashSet();
        this.mUsingLnbIds = new HashSet();
        this.mUsingCasSystemId = -1;
        this.mId = builder.mId;
        this.mTvInputSessionId = builder.mTvInputSessionId;
        this.mUseCase = builder.mUseCase;
        this.mProcessId = builder.mProcessId;
    }

    public int getId() {
        return this.mId;
    }

    public String getTvInputSessionId() {
        return this.mTvInputSessionId;
    }

    public int getUseCase() {
        return this.mUseCase;
    }

    public int getProcessId() {
        return this.mProcessId;
    }

    public int getGroupId() {
        return this.mGroupId;
    }

    public int getPriority() {
        return this.mPriority;
    }

    public int getNiceValue() {
        return this.mNiceValue;
    }

    public void setGroupId(int groupId) {
        this.mGroupId = groupId;
    }

    public void setPriority(int priority) {
        this.mPriority = priority;
    }

    public void setNiceValue(int niceValue) {
        this.mNiceValue = niceValue;
    }

    public void useFrontend(int frontendId) {
        this.mUsingFrontendIds.add(Integer.valueOf(frontendId));
    }

    public Set<Integer> getInUseFrontendIds() {
        return this.mUsingFrontendIds;
    }

    public void releaseFrontend(int frontendId) {
        this.mUsingFrontendIds.remove(Integer.valueOf(frontendId));
    }

    public void useLnb(int lnbId) {
        this.mUsingLnbIds.add(Integer.valueOf(lnbId));
    }

    public Set<Integer> getInUseLnbIds() {
        return this.mUsingLnbIds;
    }

    public void releaseLnb(int lnbId) {
        this.mUsingLnbIds.remove(Integer.valueOf(lnbId));
    }

    public void useCas(int casSystemId) {
        this.mUsingCasSystemId = casSystemId;
    }

    public int getInUseCasSystemId() {
        return this.mUsingCasSystemId;
    }

    public void releaseCas() {
        this.mUsingCasSystemId = -1;
    }

    public void reclaimAllResources() {
        this.mUsingFrontendIds.clear();
        this.mUsingLnbIds.clear();
        this.mUsingCasSystemId = -1;
    }

    public String toString() {
        return "ClientProfile[id=" + this.mId + ", tvInputSessionId=" + this.mTvInputSessionId + ", useCase=" + this.mUseCase + ", processId=" + this.mProcessId + "]";
    }

    /* loaded from: classes2.dex */
    public static class Builder {
        private final int mId;
        private int mProcessId;
        private String mTvInputSessionId;
        private int mUseCase;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(int id) {
            this.mId = id;
        }

        public Builder useCase(int useCase) {
            this.mUseCase = useCase;
            return this;
        }

        public Builder tvInputSessionId(String tvInputSessionId) {
            this.mTvInputSessionId = tvInputSessionId;
            return this;
        }

        public Builder processId(int processId) {
            this.mProcessId = processId;
            return this;
        }

        public ClientProfile build() {
            ClientProfile clientProfile = new ClientProfile(this);
            return clientProfile;
        }
    }
}