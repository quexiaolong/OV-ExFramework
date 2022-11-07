package com.android.server.backup;

/* loaded from: classes.dex */
public class JobIdManager {
    public static int getJobIdForUserId(int minJobId, int maxJobId, int userId) {
        if (minJobId + userId > maxJobId) {
            throw new RuntimeException("No job IDs available in the given range");
        }
        return minJobId + userId;
    }
}