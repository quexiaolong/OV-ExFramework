package com.vivo.services.motion;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import com.vivo.common.utils.VLog;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import vivo.app.motion.IMotionManager;

/* loaded from: classes.dex */
public class MotionManagerService extends IMotionManager.Stub {
    private static final String TAG = "MotionManagerService";
    private static final Object mObjectLock = new Object();
    private final Stack<ClientStackEntry> mClientStack = new Stack<>();

    public MotionManagerService() {
    }

    public MotionManagerService(Context context) {
    }

    public List getClients() {
        Stack<ClientStackEntry> stack = this.mClientStack;
        if (stack == null || stack.empty()) {
            return null;
        }
        List clients = new ArrayList();
        clients.add(this.mClientStack.peek().mClientId);
        return clients;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ClientStackEntry {
        private String mCallingPackageName;
        private IBinder mCb;
        private String mClientId;
        private DeathHandler mDh;
        private String mType;

        public ClientStackEntry(String clientId, String callingPackageName, String type, IBinder cb, DeathHandler dh) {
            this.mClientId = clientId;
            this.mCallingPackageName = callingPackageName;
            this.mType = type;
            this.mCb = cb;
            this.mDh = dh;
        }

        public IBinder getBinder() {
            return this.mCb;
        }

        public DeathHandler getDeathHandler() {
            return this.mDh;
        }

        public void unlinkToDeath() {
            try {
                if (this.mCb != null && this.mDh != null) {
                    this.mCb.unlinkToDeath(this.mDh, 0);
                    this.mDh = null;
                }
            } catch (NoSuchElementException e) {
                VLog.d(MotionManagerService.TAG, "Encountered " + e + " in ClientStackEntry.unlinkToDeath()");
            }
        }

        protected void finalize() throws Throwable {
            unlinkToDeath();
            super.finalize();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DeathHandler implements IBinder.DeathRecipient {
        private String mClientId;

        DeathHandler(String clientId) {
            this.mClientId = clientId;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (MotionManagerService.mObjectLock) {
                VLog.d(MotionManagerService.TAG, "[binderDied] mClientId:" + this.mClientId);
                MotionManagerService.this.removeClientStackEntry(this.mClientId, false);
            }
        }
    }

    public int register(String clientId, String callingPackageName, String type, IBinder cb) {
        if (type.equals("1")) {
            VLog.d(TAG, "[register] type:" + type + ",clientId:" + clientId + ",pkg:" + callingPackageName);
            if (!cb.pingBinder()) {
                VLog.d(TAG, "!cb.pingBinder()");
                return -1;
            }
            synchronized (mObjectLock) {
                DeathHandler dh = new DeathHandler(clientId);
                try {
                    cb.linkToDeath(dh, 0);
                    if (!this.mClientStack.empty() && this.mClientStack.peek().mClientId.equals(clientId)) {
                        VLog.d(TAG, "the current top of the client stack:" + clientId);
                        cb.unlinkToDeath(dh, 0);
                        return -1;
                    }
                    removeClientStackEntry(clientId, false);
                    this.mClientStack.push(new ClientStackEntry(clientId, callingPackageName, type, cb, dh));
                    return 0;
                } catch (RemoteException e) {
                    VLog.d(TAG, "Could not link to " + cb + " binder death.");
                    return -1;
                }
            }
        }
        return -1;
    }

    public int unregister(String clientId) {
        try {
            synchronized (mObjectLock) {
                removeClientStackEntry(clientId, true);
            }
            return 0;
        } catch (Exception e) {
            VLog.d(TAG, "FATAL EXCEPTION unregister caused " + e);
            e.printStackTrace();
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeClientStackEntry(String clientToRemove, boolean signal) {
        if (!this.mClientStack.empty() && this.mClientStack.peek().mClientId.equals(clientToRemove)) {
            ClientStackEntry Cse = this.mClientStack.pop();
            Cse.unlinkToDeath();
            VLog.d(TAG, "Removed entry for " + Cse.mClientId);
            return;
        }
        Iterator<ClientStackEntry> stackIterator = this.mClientStack.iterator();
        while (stackIterator.hasNext()) {
            ClientStackEntry Cse2 = stackIterator.next();
            if (Cse2.mClientId.equals(clientToRemove)) {
                VLog.d(TAG, "Removing entry for " + Cse2.mClientId);
                stackIterator.remove();
                Cse2.unlinkToDeath();
            }
        }
    }
}