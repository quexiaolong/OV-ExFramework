package com.vivo.services.proxy.transact;

import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.ITransactProxy;
import android.os.Parcel;
import android.os.UserHandle;
import com.vivo.common.utils.VLog;
import java.util.ArrayList;
import java.util.Iterator;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class BpTransactProxy implements ITransactProxy {
    public IBinder mBinder;
    public String mDescriptor;
    public boolean mLinkToDeathRecipient;
    public int mPid;
    public String mPkgName;
    public String mProcess;
    public int mUid;
    public int mUserId;
    public volatile boolean mProxy = false;
    public boolean mIsAlive = true;
    public final ArrayList<Transaction> mTransactions = new ArrayList<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public BpTransactProxy(String descriptor, IBinder bpBinder, String pkg, String process, int uid, int pid) {
        this.mDescriptor = descriptor;
        this.mBinder = bpBinder;
        this.mPkgName = pkg;
        this.mProcess = process;
        this.mUid = uid;
        this.mPid = pid;
        this.mUserId = UserHandle.getUserId(uid);
    }

    public boolean transact(IBinder binder, int code, Parcel data, Parcel reply, int flags) {
        if (this.mProxy && this.mIsAlive && data.dataSize() > 0) {
            if ((flags & 1) == 0) {
                VLog.e(TransactProxyManager.TAG, "flags is not oneway:" + this.mBinder.toString());
                return false;
            } else if (binder == this.mBinder && !isSkip(code)) {
                Transaction t = createTransaction(code, data);
                if (t == null) {
                    return true;
                }
                if (!onTansactionPreAdded(t)) {
                    t.recycleData();
                    return true;
                } else if (onTransactionAdded(t)) {
                    TransactProxyManager.updateTransactOptCount(this.mDescriptor, this.mProcess, this.mUserId, code, 1);
                    return true;
                } else {
                    t.recycleData();
                    return false;
                }
            } else {
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void proxy(boolean isProxy) {
        synchronized (this) {
            this.mProxy = isProxy;
            VLog.d(TransactProxyManager.TAG, "proxy " + toString());
            if (!isProxy) {
                onUnProxyLocked();
            }
        }
    }

    private void onUnProxyLocked() {
        Iterator<Transaction> it = this.mTransactions.iterator();
        while (it.hasNext()) {
            Transaction t = it.next();
            it.remove();
            try {
                try {
                    if (isAlive()) {
                        t.send2Target(this.mBinder);
                    }
                } catch (Exception e) {
                    if (this.mIsAlive && (e instanceof DeadObjectException)) {
                        this.mIsAlive = false;
                    }
                    VLog.e(TransactProxyManager.TAG, String.format("reTransaction fail transactionSize=%d e=%s this=%s", Integer.valueOf(this.mTransactions.size()), e.toString(), toString()));
                }
            } finally {
                t.recycleData();
            }
        }
    }

    protected boolean onTansactionPreAdded(Transaction t) {
        return true;
    }

    protected boolean onTransactionAdded(Transaction current) {
        synchronized (this) {
            Iterator<Transaction> it = this.mTransactions.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Transaction t = it.next();
                if (current.isEqual(t)) {
                    it.remove();
                    t.recycleData();
                    break;
                }
            }
            this.mTransactions.add(current);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearTransactions() {
        synchronized (this) {
            if (!this.mTransactions.isEmpty()) {
                Iterator<Transaction> it = this.mTransactions.iterator();
                while (it.hasNext()) {
                    Transaction t = it.next();
                    t.recycleData();
                }
                this.mTransactions.clear();
            }
        }
    }

    protected Transaction onTransactionCreated(int code, Parcel data) {
        return new Transaction(code, data);
    }

    private Transaction createTransaction(int code, Parcel data) {
        Transaction t = null;
        int pos = data.dataPosition();
        try {
            try {
                t = onTransactionCreated(code, data);
            } catch (Exception e) {
                VLog.e(TransactProxyManager.TAG, String.format("createTransaction fail, pos=%d e=%s this=%s", Integer.valueOf(pos), e.toString(), toString()));
            }
            return t;
        } finally {
            data.setDataPosition(pos);
        }
    }

    protected boolean isSkip(int code) {
        return false;
    }

    private boolean isAlive() {
        return this.mIsAlive && this.mBinder.isBinderAlive();
    }

    private int size() {
        int size;
        synchronized (this) {
            size = this.mTransactions.size();
        }
        return size;
    }

    public String toString() {
        return String.format("BpTransactProxy{descriptor=%s pkg=%s proc=%s uid=%d pid=%d proxy=%s size=%d binder=%s}", this.mDescriptor, this.mPkgName, this.mProcess, Integer.valueOf(this.mUid), Integer.valueOf(this.mPid), String.valueOf(this.mProxy), Integer.valueOf(size()), this.mBinder.toString());
    }

    public String getID() {
        return String.format("pkg=%s proc=%s uid=%d pid=%d", this.mPkgName, this.mProcess, Integer.valueOf(this.mUid), Integer.valueOf(this.mPid));
    }
}