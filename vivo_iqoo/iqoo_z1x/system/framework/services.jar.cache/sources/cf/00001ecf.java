package com.android.server.wm;

import android.os.Process;
import android.os.SystemClock;
import android.util.Slog;
import com.android.server.wm.PersisterQueue;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class PersisterQueue {
    private static final boolean DEBUG = false;
    static final WriteQueueItem EMPTY_ITEM = $$Lambda$PersisterQueue$HOTPBvinkMOqT3zxV3gRm6Y9Wi4.INSTANCE;
    private static final long FLUSH_QUEUE = -1;
    private static final long INTER_WRITE_DELAY_MS = 500;
    private static final int MAX_WRITE_QUEUE_LENGTH = 6;
    private static final long PRE_TASK_DELAY_MS = 3000;
    private static final String TAG = "PersisterQueue";
    private final long mInterWriteDelayMs;
    private final LazyTaskWriterThread mLazyTaskWriterThread;
    private final ArrayList<Listener> mListeners;
    private long mNextWriteTime;
    private final long mPreTaskDelayMs;
    private final ArrayList<WriteQueueItem> mWriteQueue;

    /* loaded from: classes2.dex */
    interface Listener {
        void onPreProcessItem(boolean z);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$static$0() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PersisterQueue() {
        this(500L, 3000L);
    }

    PersisterQueue(long interWriteDelayMs, long preTaskDelayMs) {
        this.mWriteQueue = new ArrayList<>();
        this.mListeners = new ArrayList<>();
        this.mNextWriteTime = 0L;
        if (interWriteDelayMs < 0 || preTaskDelayMs < 0) {
            throw new IllegalArgumentException("Both inter-write delay and pre-task delay need tobe non-negative. inter-write delay: " + interWriteDelayMs + "ms pre-task delay: " + preTaskDelayMs);
        }
        this.mInterWriteDelayMs = interWriteDelayMs;
        this.mPreTaskDelayMs = preTaskDelayMs;
        this.mLazyTaskWriterThread = new LazyTaskWriterThread("LazyTaskWriterThread");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void startPersisting() {
        if (!this.mLazyTaskWriterThread.isAlive()) {
            this.mLazyTaskWriterThread.start();
        }
    }

    void stopPersisting() throws InterruptedException {
        if (!this.mLazyTaskWriterThread.isAlive()) {
            return;
        }
        synchronized (this) {
            this.mLazyTaskWriterThread.interrupt();
        }
        this.mLazyTaskWriterThread.join();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void addItem(WriteQueueItem item, boolean flush) {
        this.mWriteQueue.add(item);
        if (!flush && this.mWriteQueue.size() <= 6) {
            if (this.mNextWriteTime == 0) {
                this.mNextWriteTime = SystemClock.uptimeMillis() + this.mPreTaskDelayMs;
            }
            notify();
        }
        this.mNextWriteTime = -1L;
        notify();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized <T extends WriteQueueItem> T findLastItem(Predicate<T> predicate, Class<T> clazz) {
        for (int i = this.mWriteQueue.size() - 1; i >= 0; i--) {
            WriteQueueItem writeQueueItem = this.mWriteQueue.get(i);
            if (clazz.isInstance(writeQueueItem)) {
                T item = clazz.cast(writeQueueItem);
                if (predicate.test(item)) {
                    return item;
                }
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized <T extends WriteQueueItem> void updateLastOrAddItem(final T item, boolean flush) {
        Objects.requireNonNull(item);
        WriteQueueItem findLastItem = findLastItem(new Predicate() { // from class: com.android.server.wm.-$$Lambda$pAuPvwUqsKCejIrAPrx0ARZSqeY
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return PersisterQueue.WriteQueueItem.this.matches((PersisterQueue.WriteQueueItem) obj);
            }
        }, item.getClass());
        if (findLastItem == null) {
            addItem(item, flush);
        } else {
            findLastItem.updateFrom(item);
        }
        yieldIfQueueTooDeep();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized <T extends WriteQueueItem> void removeItems(Predicate<T> predicate, Class<T> clazz) {
        for (int i = this.mWriteQueue.size() - 1; i >= 0; i--) {
            WriteQueueItem writeQueueItem = this.mWriteQueue.get(i);
            if (clazz.isInstance(writeQueueItem)) {
                T item = clazz.cast(writeQueueItem);
                if (predicate.test(item)) {
                    this.mWriteQueue.remove(i);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void flush() {
        this.mNextWriteTime = -1L;
        notifyAll();
        do {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        } while (this.mNextWriteTime == -1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void yieldIfQueueTooDeep() {
        boolean stall = false;
        synchronized (this) {
            if (this.mNextWriteTime == -1) {
                stall = true;
            }
        }
        if (stall) {
            Thread.yield();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    boolean removeListener(Listener listener) {
        return this.mListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processNextItem() throws InterruptedException {
        WriteQueueItem item;
        synchronized (this) {
            if (this.mNextWriteTime != -1) {
                this.mNextWriteTime = SystemClock.uptimeMillis() + this.mInterWriteDelayMs;
            }
            while (this.mWriteQueue.isEmpty()) {
                if (this.mNextWriteTime != 0) {
                    this.mNextWriteTime = 0L;
                    notify();
                }
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                wait();
            }
            item = this.mWriteQueue.remove(0);
            for (long now = SystemClock.uptimeMillis(); now < this.mNextWriteTime; now = SystemClock.uptimeMillis()) {
                wait(this.mNextWriteTime - now);
            }
        }
        item.process();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface WriteQueueItem<T extends WriteQueueItem<T>> {
        void process();

        default void updateFrom(T item) {
        }

        default boolean matches(T item) {
            return false;
        }
    }

    /* loaded from: classes2.dex */
    private class LazyTaskWriterThread extends Thread {
        private LazyTaskWriterThread(String name) {
            super(name);
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            boolean probablyDone;
            Process.setThreadPriority(10);
            while (true) {
                try {
                    synchronized (PersisterQueue.this) {
                        probablyDone = PersisterQueue.this.mWriteQueue.isEmpty();
                    }
                    for (int i = PersisterQueue.this.mListeners.size() - 1; i >= 0; i--) {
                        ((Listener) PersisterQueue.this.mListeners.get(i)).onPreProcessItem(probablyDone);
                    }
                    PersisterQueue.this.processNextItem();
                } catch (InterruptedException e) {
                    Slog.e(PersisterQueue.TAG, "Persister thread is exiting. Should never happen in prod, butit's OK in tests.");
                    return;
                }
            }
        }
    }
}