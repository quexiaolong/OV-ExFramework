package com.android.server.location;

import android.os.IBinder;
import android.os.RemoteException;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class LocationManagerServiceUtils {

    /* loaded from: classes.dex */
    public static class LinkedListener<TRequest, TListener> extends LinkedListenerBase {
        private final Consumer<TListener> mBinderDeathCallback;
        private final TListener mListener;
        protected final TRequest mRequest;

        public LinkedListener(TRequest request, TListener listener, CallerIdentity callerIdentity, Consumer<TListener> binderDeathCallback) {
            super(callerIdentity);
            this.mListener = listener;
            this.mRequest = request;
            this.mBinderDeathCallback = binderDeathCallback;
        }

        public TRequest getRequest() {
            return this.mRequest;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            this.mBinderDeathCallback.accept(this.mListener);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class LinkedListenerBase implements IBinder.DeathRecipient {
        public final CallerIdentity mCallerIdentity;

        /* JADX INFO: Access modifiers changed from: package-private */
        public LinkedListenerBase(CallerIdentity callerIdentity) {
            this.mCallerIdentity = callerIdentity;
        }

        public String toString() {
            return this.mCallerIdentity.toString();
        }

        public CallerIdentity getCallerIdentity() {
            return this.mCallerIdentity;
        }

        public boolean linkToListenerDeathNotificationLocked(IBinder binder) {
            try {
                binder.linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                return false;
            }
        }

        public void unlinkFromListenerDeathNotificationLocked(IBinder binder) {
            try {
                binder.unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
            }
        }
    }
}