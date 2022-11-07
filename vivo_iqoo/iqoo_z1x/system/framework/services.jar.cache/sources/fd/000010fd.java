package com.android.server.location;

import android.app.ActivityManagerInternal;
import android.content.Context;
import android.os.Binder;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/* loaded from: classes.dex */
public abstract class UserInfoHelper {
    private ActivityManagerInternal mActivityManagerInternal;
    private final Context mContext;
    private final CopyOnWriteArrayList<UserListener> mListeners = new CopyOnWriteArrayList<>();
    private UserManager mUserManager;

    /* loaded from: classes.dex */
    public interface UserListener {
        public static final int CURRENT_USER_CHANGED = 1;
        public static final int USER_STARTED = 2;
        public static final int USER_STOPPED = 3;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        public @interface UserChange {
        }

        void onUserChanged(int i, int i2);
    }

    public UserInfoHelper(Context context) {
        this.mContext = context;
    }

    public synchronized void onSystemReady() {
        if (this.mActivityManagerInternal != null) {
            return;
        }
        ActivityManagerInternal activityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        Objects.requireNonNull(activityManagerInternal);
        this.mActivityManagerInternal = activityManagerInternal;
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
    }

    public final void addListener(UserListener listener) {
        this.mListeners.add(listener);
    }

    public final void removeListener(UserListener listener) {
        this.mListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dispatchOnUserStarted(int userId) {
        if (LocationManagerService.D) {
            Log.d(LocationManagerService.TAG, "u" + userId + " started");
        }
        Iterator<UserListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            UserListener listener = it.next();
            listener.onUserChanged(userId, 2);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dispatchOnUserStopped(int userId) {
        if (LocationManagerService.D) {
            Log.d(LocationManagerService.TAG, "u" + userId + " stopped");
        }
        Iterator<UserListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            UserListener listener = it.next();
            listener.onUserChanged(userId, 3);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dispatchOnCurrentUserChanged(int fromUserId, int toUserId) {
        int[] fromUserIds = getProfileIds(fromUserId);
        int[] toUserIds = getProfileIds(toUserId);
        if (LocationManagerService.D) {
            Log.d(LocationManagerService.TAG, "current user changed from u" + Arrays.toString(fromUserIds) + " to u" + Arrays.toString(toUserIds));
        }
        Iterator<UserListener> it = this.mListeners.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            UserListener listener = it.next();
            for (int userId : fromUserIds) {
                listener.onUserChanged(userId, 1);
            }
        }
        Iterator<UserListener> it2 = this.mListeners.iterator();
        while (it2.hasNext()) {
            UserListener listener2 = it2.next();
            for (int userId2 : toUserIds) {
                listener2.onUserChanged(userId2, 1);
            }
        }
    }

    public int[] getCurrentUserIds() {
        synchronized (this) {
            if (this.mActivityManagerInternal == null) {
                return new int[0];
            }
            long identity = Binder.clearCallingIdentity();
            try {
                return this.mActivityManagerInternal.getCurrentProfileIds();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public boolean isCurrentUserId(int userId) {
        synchronized (this) {
            if (this.mActivityManagerInternal == null) {
                return false;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                return this.mActivityManagerInternal.isCurrentProfile(userId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private int[] getProfileIds(int userId) {
        synchronized (this) {
            Preconditions.checkState(this.mUserManager != null);
        }
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mUserManager.getEnabledProfileIds(userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        int[] currentUserProfiles = getCurrentUserIds();
        pw.println("current users: " + Arrays.toString(currentUserProfiles));
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        if (userManager != null) {
            for (int userId : currentUserProfiles) {
                if (userManager.hasUserRestrictionForUser("no_share_location", UserHandle.of(userId))) {
                    pw.println("  u" + userId + " restricted");
                }
            }
        }
    }
}