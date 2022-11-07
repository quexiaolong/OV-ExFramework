package com.android.server.backup.transport;

import android.content.ComponentName;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/* loaded from: classes.dex */
public class TransportStats {
    private final Object mStatsLock = new Object();
    private final Map<ComponentName, Stats> mTransportStats = new HashMap();

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerConnectionTime(ComponentName transportComponent, long timeMs) {
        synchronized (this.mStatsLock) {
            Stats stats = this.mTransportStats.get(transportComponent);
            if (stats == null) {
                stats = new Stats();
                this.mTransportStats.put(transportComponent, stats);
            }
            stats.register(timeMs);
        }
    }

    public Stats getStatsForTransport(ComponentName transportComponent) {
        synchronized (this.mStatsLock) {
            Stats stats = this.mTransportStats.get(transportComponent);
            if (stats == null) {
                return null;
            }
            return new Stats(stats);
        }
    }

    public void dump(PrintWriter pw) {
        synchronized (this.mStatsLock) {
            Optional<Stats> aggregatedStats = this.mTransportStats.values().stream().reduce($$Lambda$bnpJn6l0a4iWMupJTDnTAfwT1eA.INSTANCE);
            if (aggregatedStats.isPresent()) {
                dumpStats(pw, "", aggregatedStats.get());
            }
            if (!this.mTransportStats.isEmpty()) {
                pw.println("Per transport:");
                for (ComponentName transportComponent : this.mTransportStats.keySet()) {
                    Stats stats = this.mTransportStats.get(transportComponent);
                    pw.println("    " + transportComponent.flattenToShortString());
                    dumpStats(pw, "        ", stats);
                }
            }
        }
    }

    private static void dumpStats(PrintWriter pw, String prefix, Stats stats) {
        pw.println(String.format(Locale.US, "%sAverage connection time: %.2f ms", prefix, Double.valueOf(stats.average)));
        pw.println(String.format(Locale.US, "%sMax connection time: %d ms", prefix, Long.valueOf(stats.max)));
        pw.println(String.format(Locale.US, "%sMin connection time: %d ms", prefix, Long.valueOf(stats.min)));
        pw.println(String.format(Locale.US, "%sNumber of connections: %d ", prefix, Integer.valueOf(stats.n)));
    }

    /* loaded from: classes.dex */
    public static final class Stats {
        public double average;
        public long max;
        public long min;
        public int n;

        public static Stats merge(Stats a, Stats b) {
            int i = a.n;
            int i2 = b.n;
            return new Stats(i + i2, ((a.average * i) + (b.average * i2)) / (i + i2), Math.max(a.max, b.max), Math.min(a.min, b.min));
        }

        public Stats() {
            this.n = 0;
            this.average = 0.0d;
            this.max = 0L;
            this.min = JobStatus.NO_LATEST_RUNTIME;
        }

        private Stats(int n, double average, long max, long min) {
            this.n = n;
            this.average = average;
            this.max = max;
            this.min = min;
        }

        private Stats(Stats original) {
            this(original.n, original.average, original.max, original.min);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void register(long sample) {
            double d = this.average;
            int i = this.n;
            this.average = ((d * i) + sample) / (i + 1);
            this.n = i + 1;
            this.max = Math.max(this.max, sample);
            this.min = Math.min(this.min, sample);
        }
    }
}