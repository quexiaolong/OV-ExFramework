package com.android.server.location;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Binder;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/* loaded from: classes.dex */
public class AppForegroundHelper {
    private static final int FOREGROUND_IMPORTANCE_CUTOFF = 125;
    private ActivityManager mActivityManager;
    private final Context mContext;
    private final CopyOnWriteArrayList<AppForegroundListener> mListeners = new CopyOnWriteArrayList<>();

    /* loaded from: classes.dex */
    public interface AppForegroundListener {
        void onAppForegroundChanged(int i, boolean z);
    }

    public static /* synthetic */ void lambda$gltDhiWDJwfMNZ8gJdumXZH8_Hg(AppForegroundHelper appForegroundHelper, int i, int i2) {
        appForegroundHelper.onAppForegroundChanged(i, i2);
    }

    private static boolean isForeground(int importance) {
        return importance <= 125;
    }

    public AppForegroundHelper(Context context) {
        this.mContext = context;
    }

    public synchronized void onSystemReady() {
        if (this.mActivityManager != null) {
            return;
        }
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService(ActivityManager.class);
        Objects.requireNonNull(activityManager);
        ActivityManager activityManager2 = activityManager;
        this.mActivityManager = activityManager2;
        activityManager2.addOnUidImportanceListener(new ActivityManager.OnUidImportanceListener() { // from class: com.android.server.location.-$$Lambda$AppForegroundHelper$gltDhiWDJwfMNZ8gJdumXZH8_Hg
            public final void onUidImportance(int i, int i2) {
                AppForegroundHelper.lambda$gltDhiWDJwfMNZ8gJdumXZH8_Hg(AppForegroundHelper.this, i, i2);
            }
        }, 125);
    }

    public void addListener(AppForegroundListener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(AppForegroundListener listener) {
        this.mListeners.remove(listener);
    }

    public void onAppForegroundChanged(final int uid, int importance) {
        final boolean foreground = isForeground(importance);
        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.-$$Lambda$AppForegroundHelper$7asxY_maANt1D_AUTchqbCjktH0
            @Override // java.lang.Runnable
            public final void run() {
                AppForegroundHelper.this.lambda$onAppForegroundChanged$0$AppForegroundHelper(uid, foreground);
            }
        });
    }

    public /* synthetic */ void lambda$onAppForegroundChanged$0$AppForegroundHelper(int uid, boolean foreground) {
        Iterator<AppForegroundListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            AppForegroundListener listener = it.next();
            listener.onAppForegroundChanged(uid, foreground);
        }
    }

    public boolean isAppForeground(int uid) {
        return isForeground(getImportance(uid));
    }

    @Deprecated
    public int getImportance(int uid) {
        synchronized (this) {
            Preconditions.checkState(this.mActivityManager != null);
        }
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mActivityManager.getUidImportance(uid);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}