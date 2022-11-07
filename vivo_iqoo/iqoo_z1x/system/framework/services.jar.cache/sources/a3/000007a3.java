package com.android.server.am;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.util.TimeUtils;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public abstract class PersistentConnection<T> {
    private static final boolean DEBUG = false;
    private boolean mBound;
    private final ComponentName mComponentName;
    private final Context mContext;
    private final Handler mHandler;
    private boolean mIsConnected;
    private long mLastConnectedTime;
    private long mNextBackoffMs;
    private int mNumBindingDied;
    private int mNumConnected;
    private int mNumDisconnected;
    private final double mRebindBackoffIncrease;
    private final long mRebindBackoffMs;
    private final long mRebindMaxBackoffMs;
    private boolean mRebindScheduled;
    private long mReconnectTime;
    private final long mResetBackoffDelay;
    private T mService;
    private boolean mShouldBeBound;
    private final String mTag;
    private final int mUserId;
    private final Object mLock = new Object();
    private final ServiceConnection mServiceConnection = new ServiceConnection() { // from class: com.android.server.am.PersistentConnection.1
        {
            PersistentConnection.this = this;
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (PersistentConnection.this.mLock) {
                if (!PersistentConnection.this.mBound) {
                    String str = PersistentConnection.this.mTag;
                    Log.w(str, "Connected: " + PersistentConnection.this.mComponentName.flattenToShortString() + " u" + PersistentConnection.this.mUserId + " but not bound, ignore.");
                    return;
                }
                String str2 = PersistentConnection.this.mTag;
                Log.i(str2, "Connected: " + PersistentConnection.this.mComponentName.flattenToShortString() + " u" + PersistentConnection.this.mUserId);
                PersistentConnection.access$508(PersistentConnection.this);
                PersistentConnection.this.mIsConnected = true;
                PersistentConnection.this.mLastConnectedTime = PersistentConnection.this.injectUptimeMillis();
                PersistentConnection.this.mService = PersistentConnection.this.asInterface(service);
                PersistentConnection.this.scheduleStableCheckLocked();
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            synchronized (PersistentConnection.this.mLock) {
                String str = PersistentConnection.this.mTag;
                Log.i(str, "Disconnected: " + PersistentConnection.this.mComponentName.flattenToShortString() + " u" + PersistentConnection.this.mUserId);
                PersistentConnection.access$1008(PersistentConnection.this);
                PersistentConnection.this.cleanUpConnectionLocked();
            }
        }

        @Override // android.content.ServiceConnection
        public void onBindingDied(ComponentName name) {
            synchronized (PersistentConnection.this.mLock) {
                if (!PersistentConnection.this.mBound) {
                    String str = PersistentConnection.this.mTag;
                    Log.w(str, "Binding died: " + PersistentConnection.this.mComponentName.flattenToShortString() + " u" + PersistentConnection.this.mUserId + " but not bound, ignore.");
                    return;
                }
                String str2 = PersistentConnection.this.mTag;
                Log.w(str2, "Binding died: " + PersistentConnection.this.mComponentName.flattenToShortString() + " u" + PersistentConnection.this.mUserId);
                PersistentConnection.access$1208(PersistentConnection.this);
                PersistentConnection.this.scheduleRebindLocked();
            }
        }
    };
    private final Runnable mBindForBackoffRunnable = new Runnable() { // from class: com.android.server.am.-$$Lambda$PersistentConnection$xTW-hnA2hSnEFuF87mUe85RYnfE
        @Override // java.lang.Runnable
        public final void run() {
            PersistentConnection.this.lambda$new$0$PersistentConnection();
        }
    };
    private final Runnable mStableCheck = new Runnable() { // from class: com.android.server.am.-$$Lambda$PersistentConnection$rkvbuN0FQdQUv1hqSwDvmwwh6Uk
        @Override // java.lang.Runnable
        public final void run() {
            PersistentConnection.lambda$rkvbuN0FQdQUv1hqSwDvmwwh6Uk(PersistentConnection.this);
        }
    };

    public static /* synthetic */ void lambda$rkvbuN0FQdQUv1hqSwDvmwwh6Uk(PersistentConnection persistentConnection) {
        persistentConnection.stableConnectionCheck();
    }

    protected abstract T asInterface(IBinder iBinder);

    protected abstract int getBindFlags();

    static /* synthetic */ int access$1008(PersistentConnection x0) {
        int i = x0.mNumDisconnected;
        x0.mNumDisconnected = i + 1;
        return i;
    }

    static /* synthetic */ int access$1208(PersistentConnection x0) {
        int i = x0.mNumBindingDied;
        x0.mNumBindingDied = i + 1;
        return i;
    }

    static /* synthetic */ int access$508(PersistentConnection x0) {
        int i = x0.mNumConnected;
        x0.mNumConnected = i + 1;
        return i;
    }

    public PersistentConnection(String tag, Context context, Handler handler, int userId, ComponentName componentName, long rebindBackoffSeconds, double rebindBackoffIncrease, long rebindMaxBackoffSeconds, long resetBackoffDelay) {
        this.mTag = tag;
        this.mContext = context;
        this.mHandler = handler;
        this.mUserId = userId;
        this.mComponentName = componentName;
        long j = rebindBackoffSeconds * 1000;
        this.mRebindBackoffMs = j;
        this.mRebindBackoffIncrease = rebindBackoffIncrease;
        this.mRebindMaxBackoffMs = rebindMaxBackoffSeconds * 1000;
        this.mResetBackoffDelay = 1000 * resetBackoffDelay;
        this.mNextBackoffMs = j;
    }

    public final ComponentName getComponentName() {
        return this.mComponentName;
    }

    public final int getUserId() {
        return this.mUserId;
    }

    public final boolean isBound() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mBound;
        }
        return z;
    }

    public final boolean isRebindScheduled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mRebindScheduled;
        }
        return z;
    }

    public final boolean isConnected() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsConnected;
        }
        return z;
    }

    public final T getServiceBinder() {
        T t;
        synchronized (this.mLock) {
            t = this.mService;
        }
        return t;
    }

    public final void bind() {
        synchronized (this.mLock) {
            this.mShouldBeBound = true;
            bindInnerLocked(true);
        }
    }

    public long getNextBackoffMs() {
        long j;
        synchronized (this.mLock) {
            j = this.mNextBackoffMs;
        }
        return j;
    }

    public int getNumConnected() {
        int i;
        synchronized (this.mLock) {
            i = this.mNumConnected;
        }
        return i;
    }

    public int getNumDisconnected() {
        int i;
        synchronized (this.mLock) {
            i = this.mNumDisconnected;
        }
        return i;
    }

    public int getNumBindingDied() {
        int i;
        synchronized (this.mLock) {
            i = this.mNumBindingDied;
        }
        return i;
    }

    private void resetBackoffLocked() {
        long j = this.mNextBackoffMs;
        long j2 = this.mRebindBackoffMs;
        if (j != j2) {
            this.mNextBackoffMs = j2;
            String str = this.mTag;
            Log.i(str, "Backoff reset to " + this.mNextBackoffMs);
        }
    }

    public final void bindInnerLocked(boolean resetBackoff) {
        unscheduleRebindLocked();
        if (this.mBound) {
            return;
        }
        this.mBound = true;
        unscheduleStableCheckLocked();
        if (resetBackoff) {
            resetBackoffLocked();
        }
        Intent service = new Intent().setComponent(this.mComponentName);
        boolean success = this.mContext.bindServiceAsUser(service, this.mServiceConnection, getBindFlags() | 1, this.mHandler, UserHandle.of(this.mUserId));
        if (!success) {
            String str = this.mTag;
            Log.e(str, "Binding: " + service.getComponent() + " u" + this.mUserId + " failed.");
        }
    }

    /* renamed from: bindForBackoff */
    public final void lambda$new$0$PersistentConnection() {
        synchronized (this.mLock) {
            if (this.mShouldBeBound) {
                bindInnerLocked(false);
            }
        }
    }

    public void cleanUpConnectionLocked() {
        this.mIsConnected = false;
        this.mService = null;
        unscheduleStableCheckLocked();
    }

    public final void unbind() {
        synchronized (this.mLock) {
            this.mShouldBeBound = false;
            unbindLocked();
            unscheduleStableCheckLocked();
        }
    }

    private final void unbindLocked() {
        unscheduleRebindLocked();
        if (!this.mBound) {
            return;
        }
        String str = this.mTag;
        Log.i(str, "Stopping: " + this.mComponentName.flattenToShortString() + " u" + this.mUserId);
        this.mBound = false;
        this.mContext.unbindService(this.mServiceConnection);
        cleanUpConnectionLocked();
    }

    void unscheduleRebindLocked() {
        injectRemoveCallbacks(this.mBindForBackoffRunnable);
        this.mRebindScheduled = false;
    }

    void scheduleRebindLocked() {
        unbindLocked();
        if (!this.mRebindScheduled) {
            String str = this.mTag;
            Log.i(str, "Scheduling to reconnect in " + this.mNextBackoffMs + " ms (uptime)");
            long injectUptimeMillis = injectUptimeMillis() + this.mNextBackoffMs;
            this.mReconnectTime = injectUptimeMillis;
            injectPostAtTime(this.mBindForBackoffRunnable, injectUptimeMillis);
            this.mNextBackoffMs = Math.min(this.mRebindMaxBackoffMs, (long) (((double) this.mNextBackoffMs) * this.mRebindBackoffIncrease));
            this.mRebindScheduled = true;
        }
    }

    public void stableConnectionCheck() {
        synchronized (this.mLock) {
            long now = injectUptimeMillis();
            long timeRemaining = (this.mLastConnectedTime + this.mResetBackoffDelay) - now;
            if (this.mBound && this.mIsConnected && timeRemaining <= 0) {
                resetBackoffLocked();
            }
        }
    }

    private void unscheduleStableCheckLocked() {
        injectRemoveCallbacks(this.mStableCheck);
    }

    public void scheduleStableCheckLocked() {
        unscheduleStableCheckLocked();
        injectPostAtTime(this.mStableCheck, injectUptimeMillis() + this.mResetBackoffDelay);
    }

    public void dump(String prefix, PrintWriter pw) {
        synchronized (this.mLock) {
            pw.print(prefix);
            pw.print(this.mComponentName.flattenToShortString());
            pw.print(" u");
            pw.print(this.mUserId);
            pw.print(this.mBound ? " [bound]" : " [not bound]");
            pw.print(this.mIsConnected ? " [connected]" : " [not connected]");
            if (this.mRebindScheduled) {
                pw.print(" reconnect in ");
                TimeUtils.formatDuration(this.mReconnectTime - injectUptimeMillis(), pw);
            }
            pw.println();
            pw.print(prefix);
            pw.print("  Next backoff(sec): ");
            pw.print(this.mNextBackoffMs / 1000);
            pw.println();
            pw.print(prefix);
            pw.print("  Connected: ");
            pw.print(this.mNumConnected);
            pw.print("  Disconnected: ");
            pw.print(this.mNumDisconnected);
            pw.print("  Died: ");
            pw.print(this.mNumBindingDied);
            if (this.mIsConnected) {
                pw.print("  Duration: ");
                TimeUtils.formatDuration(injectUptimeMillis() - this.mLastConnectedTime, pw);
            }
            pw.println();
        }
    }

    void injectRemoveCallbacks(Runnable r) {
        this.mHandler.removeCallbacks(r);
    }

    void injectPostAtTime(Runnable r, long uptimeMillis) {
        this.mHandler.postAtTime(r, uptimeMillis);
    }

    long injectUptimeMillis() {
        return SystemClock.uptimeMillis();
    }

    long getNextBackoffMsForTest() {
        return this.mNextBackoffMs;
    }

    long getReconnectTimeForTest() {
        return this.mReconnectTime;
    }

    ServiceConnection getServiceConnectionForTest() {
        return this.mServiceConnection;
    }

    Runnable getBindForBackoffRunnableForTest() {
        return this.mBindForBackoffRunnable;
    }

    Runnable getStableCheckRunnableForTest() {
        return this.mStableCheck;
    }

    boolean shouldBeBoundForTest() {
        return this.mShouldBeBound;
    }
}