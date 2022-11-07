package com.android.server.am.frozen;

import android.os.SystemClock;
import java.util.Arrays;
import java.util.HashMap;
import vivo.util.VSlog;

/* loaded from: classes.dex */
class FrozenDataBean {
    private static final String TAG = "FrozenDataBean";
    private static final long UNKNOWN_TIME = -1;
    private long bgBeginTime;
    private long bgDur;
    private int caller;
    private long frozenDur;
    private long frozenTime;
    private boolean isUnfrozen;
    String pkg;
    private int successCnt;
    int uid;
    private int unfrozenCnt;
    private int[] unfrozenReason;

    FrozenDataBean(int uid, String pkg, int caller) {
        this.frozenTime = SystemClock.elapsedRealtime();
        this.isUnfrozen = false;
        this.successCnt = 1;
        this.unfrozenReason = new int[FrozenDataInfo.getUnfrozenReasonSize()];
        this.uid = uid;
        this.pkg = pkg;
        this.caller = caller;
    }

    public void updateFrozenTime() {
        this.frozenTime = SystemClock.uptimeMillis();
        this.isUnfrozen = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FrozenDataBean(int uid, String pkg, int caller, long bgBeginTime) {
        this.frozenTime = SystemClock.elapsedRealtime();
        this.isUnfrozen = false;
        this.successCnt = 1;
        this.unfrozenReason = new int[FrozenDataInfo.getUnfrozenReasonSize()];
        this.uid = uid;
        this.pkg = pkg;
        this.caller = caller;
        this.bgBeginTime = bgBeginTime;
    }

    public void updateFrozenTime(long bgBeginTime) {
        this.frozenTime = SystemClock.elapsedRealtime();
        this.isUnfrozen = false;
        this.bgBeginTime = bgBeginTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unfrozen(int reason) {
        int index = FrozenDataInfo.indexOfReason(reason);
        if (this.bgBeginTime == 0 || index < 0) {
            VSlog.e(TAG, "Unfrozen reason " + FrozenDataInfo.convertUnfrozenReason(reason));
            return;
        }
        int[] iArr = this.unfrozenReason;
        iArr[index] = iArr[index] + 1;
        this.unfrozenCnt++;
        long unfrozenTime = SystemClock.elapsedRealtime();
        long j = this.bgBeginTime;
        if (unfrozenTime > j) {
            this.bgDur += unfrozenTime - j;
        }
        long j2 = this.frozenTime;
        if (unfrozenTime > j2 && j2 != UNKNOWN_TIME) {
            this.frozenDur += unfrozenTime - j2;
        }
        VSlog.d(TAG, "After unfrozen: " + toString());
        this.frozenTime = UNKNOWN_TIME;
        this.isUnfrozen = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canUpload() {
        return this.isUnfrozen && this.frozenDur > 0;
    }

    public String toJSONString() {
        HashMap<String, String> outVal = new HashMap<>();
        outVal.put("pkg", this.pkg);
        outVal.put("frozen_caller", Integer.toString(this.caller));
        outVal.put("frozen_total_dur", Long.toString(this.frozenDur));
        outVal.put("bg_total_dur", Long.toString(this.bgDur));
        outVal.put("success_cnt", Integer.toString(this.successCnt));
        outVal.put("unfrozen_cnt", Integer.toString(this.unfrozenCnt));
        outVal.put("unfrozen_reason", Arrays.toString(this.unfrozenReason));
        return outVal.toString();
    }

    public String toString() {
        HashMap<String, String> outVal = new HashMap<>();
        outVal.put("uid", Integer.toString(this.uid));
        outVal.put("pkg", this.pkg);
        outVal.put("frozenTime", Long.toString(this.frozenTime));
        outVal.put("isUnfrozen", Boolean.toString(this.isUnfrozen));
        outVal.put("caller", FrozenDataInfo.convertCaller(this.caller));
        outVal.put("frozenDur", Long.toString(this.frozenDur));
        outVal.put("bgDur", Long.toString(this.bgDur));
        outVal.put("bgBeginTime", Long.toString(this.bgBeginTime));
        outVal.put("successCnt", Integer.toString(this.successCnt));
        outVal.put("unfreezeCnt", Integer.toString(this.unfrozenCnt));
        outVal.put("unfronzeReason", Arrays.toString(this.unfrozenReason));
        return outVal.toString();
    }
}