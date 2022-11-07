package com.android.server.stats.pull.netstats;

import java.util.Objects;

/* loaded from: classes2.dex */
public final class SubInfo {
    public final int carrierId;
    public final boolean isOpportunistic;
    public final String mcc;
    public final String mnc;
    public final int subId;
    public final String subscriberId;

    public SubInfo(int subId, int carrierId, String mcc, String mnc, String subscriberId, boolean isOpportunistic) {
        this.subId = subId;
        this.carrierId = carrierId;
        this.mcc = mcc;
        this.mnc = mnc;
        this.subscriberId = subscriberId;
        this.isOpportunistic = isOpportunistic;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubInfo other = (SubInfo) o;
        if (this.subId == other.subId && this.carrierId == other.carrierId && this.isOpportunistic == other.isOpportunistic && this.mcc.equals(other.mcc) && this.mnc.equals(other.mnc) && this.subscriberId.equals(other.subscriberId)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.subId), this.mcc, this.mnc, Integer.valueOf(this.carrierId), this.subscriberId, Boolean.valueOf(this.isOpportunistic));
    }
}