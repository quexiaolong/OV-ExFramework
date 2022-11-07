package com.android.server.tv.tunerresourcemanager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/* loaded from: classes2.dex */
public final class CasResource {
    private int mAvailableSessionNum;
    private int mMaxSessionNum;
    private Map<Integer, Integer> mOwnerClientIdsToSessionNum;
    private final int mSystemId;

    private CasResource(Builder builder) {
        this.mOwnerClientIdsToSessionNum = new HashMap();
        this.mSystemId = builder.mSystemId;
        this.mMaxSessionNum = builder.mMaxSessionNum;
        this.mAvailableSessionNum = builder.mMaxSessionNum;
    }

    public int getSystemId() {
        return this.mSystemId;
    }

    public int getMaxSessionNum() {
        return this.mMaxSessionNum;
    }

    public int getUsedSessionNum() {
        return this.mMaxSessionNum - this.mAvailableSessionNum;
    }

    public boolean isFullyUsed() {
        return this.mAvailableSessionNum == 0;
    }

    public void updateMaxSessionNum(int maxSessionNum) {
        this.mAvailableSessionNum = Math.max(0, this.mAvailableSessionNum + (maxSessionNum - this.mMaxSessionNum));
        this.mMaxSessionNum = maxSessionNum;
    }

    public void setOwner(int ownerId) {
        int sessionNum = this.mOwnerClientIdsToSessionNum.get(Integer.valueOf(ownerId)) == null ? 1 : this.mOwnerClientIdsToSessionNum.get(Integer.valueOf(ownerId)).intValue() + 1;
        this.mOwnerClientIdsToSessionNum.put(Integer.valueOf(ownerId), Integer.valueOf(sessionNum));
        this.mAvailableSessionNum--;
    }

    public void removeOwner(int ownerId) {
        this.mAvailableSessionNum += this.mOwnerClientIdsToSessionNum.get(Integer.valueOf(ownerId)).intValue();
        this.mOwnerClientIdsToSessionNum.remove(Integer.valueOf(ownerId));
    }

    public Set<Integer> getOwnerClientIds() {
        return this.mOwnerClientIdsToSessionNum.keySet();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CasResource[systemId=");
        sb.append(this.mSystemId);
        sb.append(", isFullyUsed=");
        sb.append(this.mAvailableSessionNum == 0);
        sb.append(", maxSessionNum=");
        sb.append(this.mMaxSessionNum);
        sb.append(", ownerClients=");
        sb.append(ownersMapToString());
        sb.append("]");
        return sb.toString();
    }

    /* loaded from: classes2.dex */
    public static class Builder {
        private int mMaxSessionNum;
        private int mSystemId;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(int systemId) {
            this.mSystemId = systemId;
        }

        public Builder maxSessionNum(int maxSessionNum) {
            this.mMaxSessionNum = maxSessionNum;
            return this;
        }

        public CasResource build() {
            CasResource cas = new CasResource(this);
            return cas;
        }
    }

    private String ownersMapToString() {
        StringBuilder string = new StringBuilder("{");
        for (Integer num : this.mOwnerClientIdsToSessionNum.keySet()) {
            int clienId = num.intValue();
            string.append(" clientId=");
            string.append(clienId);
            string.append(", owns session num=");
            string.append(this.mOwnerClientIdsToSessionNum.get(Integer.valueOf(clienId)));
            string.append(",");
        }
        string.append("}");
        return string.toString();
    }
}