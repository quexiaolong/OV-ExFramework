package com.android.server.am.frozen;

import android.util.SparseArray;

/* loaded from: classes.dex */
public class FrozenDataInfo {
    private static final SparseArray<String> UNFROZEN_REASONS = new SparseArray<String>() { // from class: com.android.server.am.frozen.FrozenDataInfo.1
        {
            put(0, "audio focus");
            put(1, "Start activity");
            put(2, "add widget");
            put(3, "media resource");
            put(4, "process died");
            put(5, "bind service");
            put(6, "phone state");
            put(7, "media key code");
            put(8, "push service");
            put(9, "notification");
            put(10, "dump anr");
            put(11, "window drawn timeout");
            put(12, "sync binder");
            put(14, "stop bg service");
            put(13, "kill service");
            put(15, "unbind service");
            put(99, "unknown reason");
        }
    };
    public final int caller;
    public int failReason;
    public final String pkgName;
    public final int state;
    public final int uid;
    public int unfrozenReason;

    public FrozenDataInfo(int uid, String pkgName, int caller, int frozen) {
        this.uid = uid;
        this.pkgName = pkgName;
        this.caller = caller;
        this.state = frozen;
    }

    public static String convertCaller(int caller) {
        if (caller == 0) {
            return "pem";
        }
        if (caller == 1) {
            return "rms";
        }
        if (caller == 2) {
            return "quickfrozen";
        }
        return "unknown";
    }

    public static int indexOfReason(int reason) {
        return UNFROZEN_REASONS.indexOfKey(reason);
    }

    public static String convertUnfrozenReason(int reason) {
        return UNFROZEN_REASONS.get(reason, "unknown");
    }

    public static int getUnfrozenReasonSize() {
        return UNFROZEN_REASONS.size();
    }

    public static String convertState(int state) {
        return state == 1 ? "frozen" : "unfrozen";
    }

    public void setUnfrozenReason(int unfrozenReason) {
        this.unfrozenReason = unfrozenReason;
    }

    public void setFailReason(int failReason) {
        this.failReason = failReason;
    }

    public String toString() {
        StringBuilder outVal = new StringBuilder();
        outVal.append("uid=");
        outVal.append(this.uid);
        outVal.append(" ");
        outVal.append("pkg=");
        outVal.append(this.pkgName);
        outVal.append(" ");
        outVal.append("caller=");
        outVal.append(convertCaller(this.caller));
        outVal.append(" ");
        outVal.append("state=");
        outVal.append(convertState(this.state));
        outVal.append(" ");
        if (this.state == 1) {
            outVal.append("failReason=");
            outVal.append(this.failReason);
        } else {
            outVal.append("unfrozenReason=");
            outVal.append(convertUnfrozenReason(this.unfrozenReason));
        }
        return outVal.toString();
    }
}