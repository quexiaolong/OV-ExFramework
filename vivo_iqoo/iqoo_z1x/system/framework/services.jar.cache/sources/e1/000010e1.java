package com.android.server.location;

import android.os.SystemClock;
import android.util.Log;
import android.util.TimeUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

/* loaded from: classes.dex */
public class LocationRequestStatistics {
    private static final String TAG = "LocationStats";
    public final HashMap<PackageProviderKey, PackageStatistics> statistics = new HashMap<>();
    public final RequestSummaryLimitedHistory history = new RequestSummaryLimitedHistory();

    public void startRequesting(String packageName, String featureId, String providerName, long intervalMs, boolean isForeground) {
        PackageProviderKey key = new PackageProviderKey(packageName, featureId, providerName);
        PackageStatistics stats = this.statistics.get(key);
        if (stats == null) {
            stats = new PackageStatistics();
            this.statistics.put(key, stats);
        }
        stats.startRequesting(intervalMs);
        stats.updateForeground(isForeground);
        this.history.addRequest(packageName, featureId, providerName, intervalMs);
    }

    public void stopRequesting(String packageName, String featureId, String providerName) {
        PackageProviderKey key = new PackageProviderKey(packageName, featureId, providerName);
        PackageStatistics stats = this.statistics.get(key);
        if (stats != null) {
            stats.stopRequesting();
        }
        this.history.removeRequest(packageName, featureId, providerName);
    }

    public void updateForeground(String packageName, String featureId, String providerName, boolean isForeground) {
        PackageProviderKey key = new PackageProviderKey(packageName, featureId, providerName);
        PackageStatistics stats = this.statistics.get(key);
        if (stats == null) {
            return;
        }
        stats.updateForeground(isForeground);
    }

    /* loaded from: classes.dex */
    public static class PackageProviderKey implements Comparable<PackageProviderKey> {
        public final String mFeatureId;
        public final String mPackageName;
        public final String mProviderName;

        PackageProviderKey(String packageName, String featureId, String providerName) {
            this.mPackageName = packageName;
            this.mFeatureId = featureId;
            this.mProviderName = providerName;
        }

        public String toString() {
            String str;
            StringBuilder sb = new StringBuilder();
            sb.append(this.mProviderName);
            sb.append(": ");
            sb.append(this.mPackageName);
            if (this.mFeatureId == null) {
                str = "";
            } else {
                str = ": " + this.mFeatureId;
            }
            sb.append(str);
            return sb.toString();
        }

        @Override // java.lang.Comparable
        public int compareTo(PackageProviderKey other) {
            int providerCompare = this.mProviderName.compareTo(other.mProviderName);
            if (providerCompare != 0) {
                return providerCompare;
            }
            int packageCompare = this.mPackageName.compareTo(other.mPackageName);
            if (packageCompare != 0) {
                return packageCompare;
            }
            return Objects.compare(this.mFeatureId, other.mFeatureId, Comparator.nullsFirst($$Lambda$TEfSBt3hRUlBSSARfPEHsJesTtE.INSTANCE));
        }

        public boolean equals(Object other) {
            if (other instanceof PackageProviderKey) {
                PackageProviderKey otherKey = (PackageProviderKey) other;
                return this.mPackageName.equals(otherKey.mPackageName) && this.mProviderName.equals(otherKey.mProviderName) && Objects.equals(this.mFeatureId, otherKey.mFeatureId);
            }
            return false;
        }

        public int hashCode() {
            int hash = this.mPackageName.hashCode() + (this.mProviderName.hashCode() * 31);
            String str = this.mFeatureId;
            if (str != null) {
                return hash + str.hashCode() + (hash * 31);
            }
            return hash;
        }
    }

    /* loaded from: classes.dex */
    public static class RequestSummaryLimitedHistory {
        static final int MAX_SIZE = 100;
        final ArrayList<RequestSummary> mList = new ArrayList<>(100);

        void addRequest(String packageName, String featureId, String providerName, long intervalMs) {
            addRequestSummary(new RequestSummary(packageName, featureId, providerName, intervalMs));
        }

        void removeRequest(String packageName, String featureId, String providerName) {
            addRequestSummary(new RequestSummary(packageName, featureId, providerName, -1L));
        }

        private void addRequestSummary(RequestSummary summary) {
            while (this.mList.size() >= 100) {
                this.mList.remove(0);
            }
            this.mList.add(summary);
        }

        public void dump(IndentingPrintWriter ipw) {
            long systemElapsedOffsetMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime();
            ipw.println("Last Several Location Requests:");
            ipw.increaseIndent();
            Iterator<RequestSummary> it = this.mList.iterator();
            while (it.hasNext()) {
                RequestSummary requestSummary = it.next();
                requestSummary.dump(ipw, systemElapsedOffsetMillis);
            }
            ipw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class RequestSummary {
        static final long REQUEST_ENDED_INTERVAL = -1;
        private final long mElapsedRealtimeMillis = SystemClock.elapsedRealtime();
        private final String mFeatureId;
        private final long mIntervalMillis;
        private final String mPackageName;
        private final String mProviderName;

        RequestSummary(String packageName, String featureId, String providerName, long intervalMillis) {
            this.mPackageName = packageName;
            this.mFeatureId = featureId;
            this.mProviderName = providerName;
            this.mIntervalMillis = intervalMillis;
        }

        void dump(IndentingPrintWriter ipw, long systemElapsedOffsetMillis) {
            StringBuilder s = new StringBuilder();
            long systemTimeMillis = this.mElapsedRealtimeMillis + systemElapsedOffsetMillis;
            s.append("At ");
            s.append(TimeUtils.logTimeOfDay(systemTimeMillis));
            s.append(": ");
            s.append(this.mIntervalMillis == -1 ? "- " : "+ ");
            s.append(String.format("%7s", this.mProviderName));
            s.append(" request from ");
            s.append(this.mPackageName);
            if (this.mFeatureId != null) {
                s.append(" with feature ");
                s.append(this.mFeatureId);
            }
            if (this.mIntervalMillis != -1) {
                s.append(" at interval ");
                s.append(this.mIntervalMillis / 1000);
                s.append(" seconds");
            }
            ipw.println(s);
        }
    }

    /* loaded from: classes.dex */
    public static class PackageStatistics {
        private long mFastestIntervalMs;
        private long mForegroundDurationMs;
        private final long mInitialElapsedTimeMs;
        private long mLastActivitationElapsedTimeMs;
        private long mLastForegroundElapsedTimeMs;
        private long mLastStopElapsedTimeMs;
        private int mNumActiveRequests;
        private long mSlowestIntervalMs;
        private long mTotalDurationMs;

        private PackageStatistics() {
            this.mInitialElapsedTimeMs = SystemClock.elapsedRealtime();
            this.mNumActiveRequests = 0;
            this.mTotalDurationMs = 0L;
            this.mFastestIntervalMs = JobStatus.NO_LATEST_RUNTIME;
            this.mSlowestIntervalMs = 0L;
            this.mForegroundDurationMs = 0L;
            this.mLastForegroundElapsedTimeMs = 0L;
            this.mLastStopElapsedTimeMs = 0L;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void startRequesting(long intervalMs) {
            if (this.mNumActiveRequests == 0) {
                this.mLastActivitationElapsedTimeMs = SystemClock.elapsedRealtime();
            }
            if (intervalMs < this.mFastestIntervalMs) {
                this.mFastestIntervalMs = intervalMs;
            }
            if (intervalMs > this.mSlowestIntervalMs) {
                this.mSlowestIntervalMs = intervalMs;
            }
            this.mNumActiveRequests++;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateForeground(boolean isForeground) {
            long nowElapsedTimeMs = SystemClock.elapsedRealtime();
            long j = this.mLastForegroundElapsedTimeMs;
            if (j != 0) {
                this.mForegroundDurationMs += nowElapsedTimeMs - j;
            }
            this.mLastForegroundElapsedTimeMs = isForeground ? nowElapsedTimeMs : 0L;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void stopRequesting() {
            int i = this.mNumActiveRequests;
            if (i <= 0) {
                Log.e(LocationRequestStatistics.TAG, "Reference counting corrupted in usage statistics.");
                return;
            }
            int i2 = i - 1;
            this.mNumActiveRequests = i2;
            if (i2 == 0) {
                long elapsedRealtime = SystemClock.elapsedRealtime();
                this.mLastStopElapsedTimeMs = elapsedRealtime;
                long lastDurationMs = elapsedRealtime - this.mLastActivitationElapsedTimeMs;
                this.mTotalDurationMs += lastDurationMs;
                updateForeground(false);
            }
        }

        public long getDurationMs() {
            long currentDurationMs = this.mTotalDurationMs;
            if (this.mNumActiveRequests > 0) {
                return currentDurationMs + (SystemClock.elapsedRealtime() - this.mLastActivitationElapsedTimeMs);
            }
            return currentDurationMs;
        }

        public long getForegroundDurationMs() {
            long currentDurationMs = this.mForegroundDurationMs;
            if (this.mLastForegroundElapsedTimeMs != 0) {
                return currentDurationMs + (SystemClock.elapsedRealtime() - this.mLastForegroundElapsedTimeMs);
            }
            return currentDurationMs;
        }

        public long getTimeSinceFirstRequestMs() {
            return SystemClock.elapsedRealtime() - this.mInitialElapsedTimeMs;
        }

        public long getTimeSinceLastRequestStoppedMs() {
            return SystemClock.elapsedRealtime() - this.mLastStopElapsedTimeMs;
        }

        public long getFastestIntervalMs() {
            return this.mFastestIntervalMs;
        }

        public long getSlowestIntervalMs() {
            return this.mSlowestIntervalMs;
        }

        public boolean isActive() {
            return this.mNumActiveRequests > 0;
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            if (this.mFastestIntervalMs == this.mSlowestIntervalMs) {
                s.append("Interval ");
                s.append(this.mFastestIntervalMs / 1000);
                s.append(" seconds");
            } else {
                s.append("Min interval ");
                s.append(this.mFastestIntervalMs / 1000);
                s.append(" seconds");
                s.append(": Max interval ");
                s.append(this.mSlowestIntervalMs / 1000);
                s.append(" seconds");
            }
            s.append(": Duration requested ");
            s.append((getDurationMs() / 1000) / 60);
            s.append(" total, ");
            s.append((getForegroundDurationMs() / 1000) / 60);
            s.append(" foreground, out of the last ");
            s.append((getTimeSinceFirstRequestMs() / 1000) / 60);
            s.append(" minutes");
            if (isActive()) {
                s.append(": Currently active");
            } else {
                s.append(": Last active ");
                s.append((getTimeSinceLastRequestStoppedMs() / 1000) / 60);
                s.append(" minutes ago");
            }
            return s.toString();
        }
    }
}