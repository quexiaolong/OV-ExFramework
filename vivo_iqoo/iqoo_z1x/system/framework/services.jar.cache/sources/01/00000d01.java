package com.android.server.devicepolicy;

import android.app.admin.SecurityLog;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import com.android.server.job.controllers.JobStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class SecurityLogMonitor implements Runnable {
    private static final int BUFFER_ENTRIES_CRITICAL_LEVEL = 9216;
    private static final int BUFFER_ENTRIES_MAXIMUM_LEVEL = 10240;
    static final int BUFFER_ENTRIES_NOTIFICATION_LEVEL = 1024;
    private static final boolean DEBUG = false;
    private static final String TAG = "SecurityLogMonitor";
    private boolean mAllowedToRetrieve;
    private boolean mCriticalLevelLogged;
    private int mEnabledUser;
    private final Semaphore mForceSemaphore;
    private long mId;
    private long mLastEventNanos;
    private final ArrayList<SecurityLog.SecurityEvent> mLastEvents;
    private long mLastForceNanos;
    private final Lock mLock;
    private Thread mMonitorThread;
    private long mNextAllowedRetrievalTimeMillis;
    private boolean mPaused;
    private ArrayList<SecurityLog.SecurityEvent> mPendingLogs;
    private final DevicePolicyManagerService mService;
    private static final long RATE_LIMIT_INTERVAL_MS = TimeUnit.HOURS.toMillis(2);
    private static final long BROADCAST_RETRY_INTERVAL_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long POLLING_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long OVERLAP_NS = TimeUnit.SECONDS.toNanos(3);
    private static final long FORCE_FETCH_THROTTLE_NS = TimeUnit.SECONDS.toNanos(10);

    /* JADX INFO: Access modifiers changed from: package-private */
    public SecurityLogMonitor(DevicePolicyManagerService service) {
        this(service, 0L);
    }

    SecurityLogMonitor(DevicePolicyManagerService service, long id) {
        this.mLock = new ReentrantLock();
        this.mMonitorThread = null;
        this.mPendingLogs = new ArrayList<>();
        this.mAllowedToRetrieve = false;
        this.mCriticalLevelLogged = false;
        this.mLastEvents = new ArrayList<>();
        this.mLastEventNanos = -1L;
        this.mNextAllowedRetrievalTimeMillis = -1L;
        this.mPaused = false;
        this.mForceSemaphore = new Semaphore(0);
        this.mLastForceNanos = 0L;
        this.mService = service;
        this.mId = id;
        this.mLastForceNanos = System.nanoTime();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void start(int enabledUser) {
        Slog.i(TAG, "Starting security logging for user " + enabledUser);
        this.mEnabledUser = enabledUser;
        SecurityLog.writeEvent(210011, new Object[0]);
        this.mLock.lock();
        try {
            if (this.mMonitorThread == null) {
                this.mPendingLogs = new ArrayList<>();
                this.mCriticalLevelLogged = false;
                this.mId = 0L;
                this.mAllowedToRetrieve = false;
                this.mNextAllowedRetrievalTimeMillis = -1L;
                this.mPaused = false;
                Thread thread = new Thread(this);
                this.mMonitorThread = thread;
                thread.start();
            }
        } finally {
            this.mLock.unlock();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stop() {
        Slog.i(TAG, "Stopping security logging.");
        SecurityLog.writeEvent(210012, new Object[0]);
        this.mLock.lock();
        try {
            if (this.mMonitorThread != null) {
                this.mMonitorThread.interrupt();
                try {
                    this.mMonitorThread.join(TimeUnit.SECONDS.toMillis(5L));
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting for thread to stop", e);
                }
                this.mPendingLogs = new ArrayList<>();
                this.mId = 0L;
                this.mAllowedToRetrieve = false;
                this.mNextAllowedRetrievalTimeMillis = -1L;
                this.mPaused = false;
                this.mMonitorThread = null;
            }
        } finally {
            this.mLock.unlock();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pause() {
        Slog.i(TAG, "Paused.");
        this.mLock.lock();
        this.mPaused = true;
        this.mAllowedToRetrieve = false;
        this.mLock.unlock();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resume() {
        this.mLock.lock();
        try {
            if (!this.mPaused) {
                Log.d(TAG, "Attempted to resume, but logging is not paused.");
                return;
            }
            this.mPaused = false;
            this.mAllowedToRetrieve = false;
            this.mLock.unlock();
            Slog.i(TAG, "Resumed.");
            try {
                notifyDeviceOwnerIfNeeded(false);
            } catch (InterruptedException e) {
                Log.w(TAG, "Thread interrupted.", e);
            }
        } finally {
            this.mLock.unlock();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void discardLogs() {
        this.mLock.lock();
        this.mAllowedToRetrieve = false;
        this.mPendingLogs = new ArrayList<>();
        this.mCriticalLevelLogged = false;
        this.mLock.unlock();
        Slog.i(TAG, "Discarded all logs.");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<SecurityLog.SecurityEvent> retrieveLogs() {
        this.mLock.lock();
        try {
            if (this.mAllowedToRetrieve) {
                this.mAllowedToRetrieve = false;
                this.mNextAllowedRetrievalTimeMillis = SystemClock.elapsedRealtime() + RATE_LIMIT_INTERVAL_MS;
                List<SecurityLog.SecurityEvent> result = this.mPendingLogs;
                this.mPendingLogs = new ArrayList<>();
                this.mCriticalLevelLogged = false;
                return result;
            }
            return null;
        } finally {
            this.mLock.unlock();
        }
    }

    private void getNextBatch(ArrayList<SecurityLog.SecurityEvent> newLogs) throws IOException {
        if (this.mLastEventNanos < 0) {
            SecurityLog.readEvents(newLogs);
        } else {
            long startNanos = this.mLastEvents.isEmpty() ? this.mLastEventNanos : Math.max(0L, this.mLastEventNanos - OVERLAP_NS);
            SecurityLog.readEventsSince(startNanos, newLogs);
        }
        int i = 0;
        while (true) {
            if (i >= newLogs.size() - 1) {
                break;
            } else if (newLogs.get(i).getTimeNanos() <= newLogs.get(i + 1).getTimeNanos()) {
                i++;
            } else {
                newLogs.sort($$Lambda$SecurityLogMonitor$y5Q3dMmmJ8bk5nBh8WR2MUroKrI.INSTANCE);
                break;
            }
        }
        int i2 = this.mEnabledUser;
        SecurityLog.redactEvents(newLogs, i2);
    }

    private void saveLastEvents(ArrayList<SecurityLog.SecurityEvent> newLogs) {
        this.mLastEvents.clear();
        if (newLogs.isEmpty()) {
            return;
        }
        this.mLastEventNanos = newLogs.get(newLogs.size() - 1).getTimeNanos();
        int pos = newLogs.size() - 2;
        while (pos >= 0 && this.mLastEventNanos - newLogs.get(pos).getTimeNanos() < OVERLAP_NS) {
            pos--;
        }
        this.mLastEvents.addAll(newLogs.subList(pos + 1, newLogs.size()));
    }

    private void mergeBatchLocked(ArrayList<SecurityLog.SecurityEvent> newLogs) {
        ArrayList<SecurityLog.SecurityEvent> arrayList = this.mPendingLogs;
        arrayList.ensureCapacity(arrayList.size() + newLogs.size());
        int curPos = 0;
        int lastPos = 0;
        while (lastPos < this.mLastEvents.size() && curPos < newLogs.size()) {
            SecurityLog.SecurityEvent curEvent = newLogs.get(curPos);
            long currentNanos = curEvent.getTimeNanos();
            if (currentNanos > this.mLastEventNanos) {
                break;
            }
            SecurityLog.SecurityEvent lastEvent = this.mLastEvents.get(lastPos);
            long lastNanos = lastEvent.getTimeNanos();
            if (lastNanos > currentNanos) {
                assignLogId(curEvent);
                this.mPendingLogs.add(curEvent);
                curPos++;
            } else if (lastNanos < currentNanos) {
                lastPos++;
            } else {
                if (!lastEvent.eventEquals(curEvent)) {
                    assignLogId(curEvent);
                    this.mPendingLogs.add(curEvent);
                }
                lastPos++;
                curPos++;
            }
        }
        List<SecurityLog.SecurityEvent> idLogs = newLogs.subList(curPos, newLogs.size());
        for (SecurityLog.SecurityEvent event : idLogs) {
            assignLogId(event);
        }
        this.mPendingLogs.addAll(idLogs);
        checkCriticalLevel();
        if (this.mPendingLogs.size() > BUFFER_ENTRIES_MAXIMUM_LEVEL) {
            ArrayList<SecurityLog.SecurityEvent> arrayList2 = this.mPendingLogs;
            this.mPendingLogs = new ArrayList<>(arrayList2.subList(arrayList2.size() - 5120, this.mPendingLogs.size()));
            this.mCriticalLevelLogged = false;
            Slog.i(TAG, "Pending logs buffer full. Discarding old logs.");
        }
    }

    private void checkCriticalLevel() {
        if (SecurityLog.isLoggingEnabled() && this.mPendingLogs.size() >= BUFFER_ENTRIES_CRITICAL_LEVEL && !this.mCriticalLevelLogged) {
            this.mCriticalLevelLogged = true;
            SecurityLog.writeEvent(210015, new Object[0]);
        }
    }

    private void assignLogId(SecurityLog.SecurityEvent event) {
        event.setId(this.mId);
        long j = this.mId;
        if (j == JobStatus.NO_LATEST_RUNTIME) {
            Slog.i(TAG, "Reached maximum id value; wrapping around.");
            this.mId = 0L;
            return;
        }
        this.mId = j + 1;
    }

    @Override // java.lang.Runnable
    public void run() {
        boolean force;
        Process.setThreadPriority(10);
        ArrayList<SecurityLog.SecurityEvent> newLogs = new ArrayList<>();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                force = this.mForceSemaphore.tryAcquire(POLLING_INTERVAL_MS, TimeUnit.MILLISECONDS);
                getNextBatch(newLogs);
                this.mLock.lockInterruptibly();
            } catch (IOException e) {
                Log.e(TAG, "Failed to read security log", e);
            } catch (InterruptedException e2) {
                Log.i(TAG, "Thread interrupted, exiting.", e2);
            }
            try {
                mergeBatchLocked(newLogs);
                this.mLock.unlock();
                saveLastEvents(newLogs);
                newLogs.clear();
                notifyDeviceOwnerIfNeeded(force);
            } catch (Throwable th) {
                this.mLock.unlock();
                throw th;
                break;
            }
        }
        this.mLastEvents.clear();
        long j = this.mLastEventNanos;
        if (j != -1) {
            this.mLastEventNanos = j + 1;
        }
        Slog.i(TAG, "MonitorThread exit.");
    }

    private void notifyDeviceOwnerIfNeeded(boolean force) throws InterruptedException {
        boolean allowRetrievalAndNotifyDO = false;
        this.mLock.lockInterruptibly();
        try {
            if (this.mPaused) {
                return;
            }
            int logSize = this.mPendingLogs.size();
            if ((logSize >= 1024 || (force && logSize > 0)) && !this.mAllowedToRetrieve) {
                allowRetrievalAndNotifyDO = true;
            }
            if (logSize > 0 && SystemClock.elapsedRealtime() >= this.mNextAllowedRetrievalTimeMillis) {
                allowRetrievalAndNotifyDO = true;
            }
            if (allowRetrievalAndNotifyDO) {
                this.mAllowedToRetrieve = true;
                this.mNextAllowedRetrievalTimeMillis = SystemClock.elapsedRealtime() + BROADCAST_RETRY_INTERVAL_MS;
            }
            if (allowRetrievalAndNotifyDO) {
                Slog.i(TAG, "notify DO");
                this.mService.sendDeviceOwnerCommand("android.app.action.SECURITY_LOGS_AVAILABLE", null);
            }
        } finally {
            this.mLock.unlock();
        }
    }

    public long forceLogs() {
        long nowNanos = System.nanoTime();
        synchronized (this.mForceSemaphore) {
            long toWaitNanos = (this.mLastForceNanos + FORCE_FETCH_THROTTLE_NS) - nowNanos;
            if (toWaitNanos > 0) {
                return TimeUnit.NANOSECONDS.toMillis(toWaitNanos) + 1;
            }
            this.mLastForceNanos = nowNanos;
            if (this.mForceSemaphore.availablePermits() == 0) {
                this.mForceSemaphore.release();
            }
            return 0L;
        }
    }
}