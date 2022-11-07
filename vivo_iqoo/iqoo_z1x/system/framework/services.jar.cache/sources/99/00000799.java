package com.android.server.am;

import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.ProcessCpuTracker;
import com.android.internal.util.RingBuffer;
import com.android.internal.util.function.pooled.PooledLambda;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class OomAdjProfiler {
    private static final int MSG_UPDATE_CPU_TIME = 42;
    private boolean mLastScheduledOnBattery;
    private boolean mLastScheduledScreenOff;
    private long mLastSystemServerCpuTimeMs;
    private boolean mOnBattery;
    private long mOomAdjStartTimeUs;
    private boolean mOomAdjStarted;
    private boolean mScreenOff;
    private boolean mSystemServerCpuTimeUpdateScheduled;
    private int mTotalOomAdjCalls;
    private long mTotalOomAdjRunTimeUs;
    private CpuTimes mOomAdjRunTime = new CpuTimes();
    private CpuTimes mSystemServerCpuTime = new CpuTimes();
    private final ProcessCpuTracker mProcessCpuTracker = new ProcessCpuTracker(false);
    final RingBuffer<CpuTimes> mOomAdjRunTimesHist = new RingBuffer<>(CpuTimes.class, 10);
    final RingBuffer<CpuTimes> mSystemServerCpuTimesHist = new RingBuffer<>(CpuTimes.class, 10);

    /* JADX INFO: Access modifiers changed from: package-private */
    public void batteryPowerChanged(boolean onBattery) {
        synchronized (this) {
            scheduleSystemServerCpuTimeUpdate();
            this.mOnBattery = onBattery;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onWakefulnessChanged(int wakefulness) {
        synchronized (this) {
            scheduleSystemServerCpuTimeUpdate();
            boolean z = true;
            if (wakefulness == 1) {
                z = false;
            }
            this.mScreenOff = z;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void oomAdjStarted() {
        synchronized (this) {
            this.mOomAdjStartTimeUs = SystemClock.currentThreadTimeMicro();
            this.mOomAdjStarted = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void oomAdjEnded() {
        synchronized (this) {
            if (this.mOomAdjStarted) {
                long elapsedUs = SystemClock.currentThreadTimeMicro() - this.mOomAdjStartTimeUs;
                this.mOomAdjRunTime.addCpuTimeUs(elapsedUs);
                this.mTotalOomAdjRunTimeUs += elapsedUs;
                this.mTotalOomAdjCalls++;
            }
        }
    }

    private void scheduleSystemServerCpuTimeUpdate() {
        synchronized (this) {
            if (this.mSystemServerCpuTimeUpdateScheduled) {
                return;
            }
            this.mLastScheduledOnBattery = this.mOnBattery;
            this.mLastScheduledScreenOff = this.mScreenOff;
            this.mSystemServerCpuTimeUpdateScheduled = true;
            Message scheduledMessage = PooledLambda.obtainMessage($$Lambda$OomAdjProfiler$oLbVP84ACmxo_1QlnwlSuhi91W4.INSTANCE, this, Boolean.valueOf(this.mLastScheduledOnBattery), Boolean.valueOf(this.mLastScheduledScreenOff), true);
            scheduledMessage.setWhat(42);
            BackgroundThread.getHandler().sendMessage(scheduledMessage);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSystemServerCpuTime(boolean onBattery, boolean screenOff, boolean onlyIfScheduled) {
        long cpuTimeMs = this.mProcessCpuTracker.getCpuTimeForPid(Process.myPid());
        synchronized (this) {
            if (onlyIfScheduled) {
                if (!this.mSystemServerCpuTimeUpdateScheduled) {
                    return;
                }
            }
            this.mSystemServerCpuTime.addCpuTimeMs(cpuTimeMs - this.mLastSystemServerCpuTimeMs, onBattery, screenOff);
            this.mLastSystemServerCpuTimeMs = cpuTimeMs;
            this.mSystemServerCpuTimeUpdateScheduled = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reset() {
        synchronized (this) {
            if (this.mSystemServerCpuTime.isEmpty()) {
                return;
            }
            this.mOomAdjRunTimesHist.append(this.mOomAdjRunTime);
            this.mSystemServerCpuTimesHist.append(this.mSystemServerCpuTime);
            this.mOomAdjRunTime = new CpuTimes();
            this.mSystemServerCpuTime = new CpuTimes();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        synchronized (this) {
            if (this.mSystemServerCpuTimeUpdateScheduled) {
                BackgroundThread.getHandler().removeMessages(42);
                updateSystemServerCpuTime(this.mLastScheduledOnBattery, this.mLastScheduledScreenOff, false);
            } else {
                updateSystemServerCpuTime(this.mOnBattery, this.mScreenOff, false);
            }
            pw.println("System server and oomAdj runtimes (ms) in recent battery sessions (most recent first):");
            if (!this.mSystemServerCpuTime.isEmpty()) {
                pw.print("  ");
                pw.print("system_server=");
                pw.print(this.mSystemServerCpuTime);
                pw.print("  ");
                pw.print("oom_adj=");
                pw.println(this.mOomAdjRunTime);
            }
            CpuTimes[] systemServerCpuTimes = (CpuTimes[]) this.mSystemServerCpuTimesHist.toArray();
            CpuTimes[] oomAdjRunTimes = (CpuTimes[]) this.mOomAdjRunTimesHist.toArray();
            for (int i = oomAdjRunTimes.length - 1; i >= 0; i--) {
                pw.print("  ");
                pw.print("system_server=");
                pw.print(systemServerCpuTimes[i]);
                pw.print("  ");
                pw.print("oom_adj=");
                pw.println(oomAdjRunTimes[i]);
            }
            int i2 = this.mTotalOomAdjCalls;
            if (i2 != 0) {
                pw.println("System server total oomAdj runtimes (us) since boot:");
                pw.print("  cpu time spent=");
                pw.print(this.mTotalOomAdjRunTimeUs);
                pw.print("  number of calls=");
                pw.print(this.mTotalOomAdjCalls);
                pw.print("  average=");
                pw.println(this.mTotalOomAdjRunTimeUs / this.mTotalOomAdjCalls);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class CpuTimes {
        private long mOnBatteryScreenOffTimeUs;
        private long mOnBatteryTimeUs;

        private CpuTimes() {
        }

        public void addCpuTimeMs(long cpuTimeMs) {
            addCpuTimeUs(1000 * cpuTimeMs, OomAdjProfiler.this.mOnBattery, OomAdjProfiler.this.mScreenOff);
        }

        public void addCpuTimeMs(long cpuTimeMs, boolean onBattery, boolean screenOff) {
            addCpuTimeUs(1000 * cpuTimeMs, onBattery, screenOff);
        }

        public void addCpuTimeUs(long cpuTimeUs) {
            addCpuTimeUs(cpuTimeUs, OomAdjProfiler.this.mOnBattery, OomAdjProfiler.this.mScreenOff);
        }

        public void addCpuTimeUs(long cpuTimeUs, boolean onBattery, boolean screenOff) {
            if (onBattery) {
                this.mOnBatteryTimeUs += cpuTimeUs;
                if (screenOff) {
                    this.mOnBatteryScreenOffTimeUs += cpuTimeUs;
                }
            }
        }

        public boolean isEmpty() {
            return this.mOnBatteryTimeUs == 0 && this.mOnBatteryScreenOffTimeUs == 0;
        }

        public String toString() {
            return "[" + (this.mOnBatteryTimeUs / 1000) + "," + (this.mOnBatteryScreenOffTimeUs / 1000) + "]";
        }
    }
}