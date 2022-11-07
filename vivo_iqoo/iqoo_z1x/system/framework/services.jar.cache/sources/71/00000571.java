package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IUpdateLock;
import android.os.RemoteException;
import android.os.TokenWatcher;
import android.os.UserHandle;
import com.android.internal.util.DumpUtils;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import java.io.FileDescriptor;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class UpdateLockService extends IUpdateLock.Stub {
    static final boolean DEBUG = false;
    static final String PERMISSION = "android.permission.UPDATE_LOCK";
    static final String TAG = "UpdateLockService";
    Context mContext;
    LockWatcher mLocks = new LockWatcher(new Handler(), "UpdateLocks");

    /* loaded from: classes.dex */
    class LockWatcher extends TokenWatcher {
        LockWatcher(Handler h, String tag) {
            super(h, tag);
        }

        @Override // android.os.TokenWatcher
        public void acquired() {
            UpdateLockService.this.sendLockChangedBroadcast(false);
        }

        @Override // android.os.TokenWatcher
        public void released() {
            UpdateLockService.this.sendLockChangedBroadcast(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UpdateLockService(Context context) {
        this.mContext = context;
        sendLockChangedBroadcast(true);
    }

    void sendLockChangedBroadcast(boolean state) {
        long oldIdent = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.os.UpdateLock.UPDATE_LOCK_CHANGED").putExtra("nowisconvenient", state).putExtra(WatchlistLoggingHandler.WatchlistEventKeys.TIMESTAMP, System.currentTimeMillis()).addFlags(67108864);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(oldIdent);
        }
    }

    public void acquireUpdateLock(IBinder token, String tag) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "acquireUpdateLock");
        this.mLocks.acquire(token, makeTag(tag));
    }

    public void releaseUpdateLock(IBinder token) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "releaseUpdateLock");
        this.mLocks.release(token);
    }

    private String makeTag(String tag) {
        return "{tag=" + tag + " uid=" + Binder.getCallingUid() + " pid=" + Binder.getCallingPid() + '}';
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            this.mLocks.dump(pw);
        }
    }
}