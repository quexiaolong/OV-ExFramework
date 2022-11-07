package com.android.server.pm;

import android.os.Environment;
import android.os.SystemClock;
import android.util.AtomicFile;
import com.android.server.am.ProcessList;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/* loaded from: classes.dex */
public abstract class AbstractStatsBase<T> {
    private static final int WRITE_INTERVAL_MS;
    private final String mBackgroundThreadName;
    private final String mFileName;
    private final boolean mLock;
    private final Object mFileLock = new Object();
    private final AtomicLong mLastTimeWritten = new AtomicLong(0);
    private final AtomicBoolean mBackgroundWriteRunning = new AtomicBoolean(false);

    protected abstract void readInternal(T t);

    protected abstract void writeInternal(T t);

    static {
        WRITE_INTERVAL_MS = PackageManagerService.DEBUG_DEXOPT ? 0 : ProcessList.PSS_ALL_INTERVAL;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public AbstractStatsBase(String fileName, String threadName, boolean lock) {
        this.mFileName = fileName;
        this.mBackgroundThreadName = threadName;
        this.mLock = lock;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public AtomicFile getFile() {
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, "system");
        File fname = new File(systemDir, this.mFileName);
        return new AtomicFile(fname);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void writeNow(T data) {
        writeImpl(data);
        this.mLastTimeWritten.set(SystemClock.elapsedRealtime());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Type inference failed for: r0v5, types: [com.android.server.pm.AbstractStatsBase$1] */
    public boolean maybeWriteAsync(final T data) {
        if ((SystemClock.elapsedRealtime() - this.mLastTimeWritten.get() >= WRITE_INTERVAL_MS || PackageManagerService.DEBUG_DEXOPT) && this.mBackgroundWriteRunning.compareAndSet(false, true)) {
            new Thread(this.mBackgroundThreadName) { // from class: com.android.server.pm.AbstractStatsBase.1
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    try {
                        AbstractStatsBase.this.writeImpl(data);
                        AbstractStatsBase.this.mLastTimeWritten.set(SystemClock.elapsedRealtime());
                    } finally {
                        AbstractStatsBase.this.mBackgroundWriteRunning.set(false);
                    }
                }
            }.start();
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeImpl(T data) {
        if (this.mLock) {
            synchronized (data) {
                synchronized (this.mFileLock) {
                    writeInternal(data);
                }
            }
            return;
        }
        synchronized (this.mFileLock) {
            writeInternal(data);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void read(T data) {
        if (this.mLock) {
            synchronized (data) {
                synchronized (this.mFileLock) {
                    readInternal(data);
                }
            }
        } else {
            synchronized (this.mFileLock) {
                readInternal(data);
            }
        }
        this.mLastTimeWritten.set(SystemClock.elapsedRealtime());
    }
}