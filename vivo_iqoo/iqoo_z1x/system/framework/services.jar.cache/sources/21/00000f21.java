package com.android.server.infra;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import com.android.server.infra.ServiceNameResolver;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public final class FrameworkResourcesServiceNameResolver implements ServiceNameResolver {
    private static final int MSG_RESET_TEMPORARY_SERVICE = 0;
    private static final String TAG = FrameworkResourcesServiceNameResolver.class.getSimpleName();
    private final Context mContext;
    private ServiceNameResolver.NameResolverListener mOnSetCallback;
    private final int mResourceId;
    private Handler mTemporaryHandler;
    private long mTemporaryServiceExpiration;
    private final Object mLock = new Object();
    private final SparseArray<String> mTemporaryServiceNames = new SparseArray<>();
    private final SparseBooleanArray mDefaultServicesDisabled = new SparseBooleanArray();

    public FrameworkResourcesServiceNameResolver(Context context, int resourceId) {
        this.mContext = context;
        this.mResourceId = resourceId;
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public void setOnTemporaryServiceNameChangedCallback(ServiceNameResolver.NameResolverListener callback) {
        synchronized (this.mLock) {
            this.mOnSetCallback = callback;
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public String getDefaultServiceName(int userId) {
        String str;
        synchronized (this.mLock) {
            String name = this.mContext.getString(this.mResourceId);
            str = TextUtils.isEmpty(name) ? null : name;
        }
        return str;
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public String getServiceName(int userId) {
        synchronized (this.mLock) {
            String temporaryName = this.mTemporaryServiceNames.get(userId);
            if (temporaryName != null) {
                String str = TAG;
                Slog.w(str, "getServiceName(): using temporary name " + temporaryName + " for user " + userId);
                return temporaryName;
            }
            boolean disabled = this.mDefaultServicesDisabled.get(userId);
            if (disabled) {
                String str2 = TAG;
                Slog.w(str2, "getServiceName(): temporary name not set and default disabled for user " + userId);
                return null;
            }
            return getDefaultServiceName(userId);
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public boolean isTemporary(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mTemporaryServiceNames.get(userId) != null;
        }
        return z;
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public void setTemporaryService(final int userId, String componentName, int durationMs) {
        synchronized (this.mLock) {
            this.mTemporaryServiceNames.put(userId, componentName);
            if (this.mTemporaryHandler == null) {
                this.mTemporaryHandler = new Handler(Looper.getMainLooper(), null, true) { // from class: com.android.server.infra.FrameworkResourcesServiceNameResolver.1
                    @Override // android.os.Handler
                    public void handleMessage(Message msg) {
                        if (msg.what == 0) {
                            synchronized (FrameworkResourcesServiceNameResolver.this.mLock) {
                                FrameworkResourcesServiceNameResolver.this.resetTemporaryService(userId);
                            }
                            return;
                        }
                        String str = FrameworkResourcesServiceNameResolver.TAG;
                        Slog.wtf(str, "invalid handler msg: " + msg);
                    }
                };
            } else {
                this.mTemporaryHandler.removeMessages(0);
            }
            this.mTemporaryServiceExpiration = SystemClock.elapsedRealtime() + durationMs;
            this.mTemporaryHandler.sendEmptyMessageDelayed(0, durationMs);
            notifyTemporaryServiceNameChangedLocked(userId, componentName, true);
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public void resetTemporaryService(int userId) {
        synchronized (this.mLock) {
            String str = TAG;
            Slog.i(str, "resetting temporary service for user " + userId + " from " + this.mTemporaryServiceNames.get(userId));
            this.mTemporaryServiceNames.remove(userId);
            if (this.mTemporaryHandler != null) {
                this.mTemporaryHandler.removeMessages(0);
                this.mTemporaryHandler = null;
            }
            notifyTemporaryServiceNameChangedLocked(userId, null, false);
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public boolean setDefaultServiceEnabled(int userId, boolean enabled) {
        synchronized (this.mLock) {
            boolean currentlyEnabled = isDefaultServiceEnabledLocked(userId);
            if (currentlyEnabled == enabled) {
                String str = TAG;
                Slog.i(str, "setDefaultServiceEnabled(" + userId + "): already " + enabled);
                return false;
            }
            if (enabled) {
                String str2 = TAG;
                Slog.i(str2, "disabling default service for user " + userId);
                this.mDefaultServicesDisabled.removeAt(userId);
            } else {
                String str3 = TAG;
                Slog.i(str3, "enabling default service for user " + userId);
                this.mDefaultServicesDisabled.put(userId, true);
            }
            return true;
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public boolean isDefaultServiceEnabled(int userId) {
        boolean isDefaultServiceEnabledLocked;
        synchronized (this.mLock) {
            isDefaultServiceEnabledLocked = isDefaultServiceEnabledLocked(userId);
        }
        return isDefaultServiceEnabledLocked;
    }

    private boolean isDefaultServiceEnabledLocked(int userId) {
        return !this.mDefaultServicesDisabled.get(userId);
    }

    public String toString() {
        return "FrameworkResourcesServiceNamer[temps=" + this.mTemporaryServiceNames + "]";
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public void dumpShort(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.print("FrameworkResourcesServiceNamer: resId=");
            pw.print(this.mResourceId);
            pw.print(", numberTemps=");
            pw.print(this.mTemporaryServiceNames.size());
            pw.print(", enabledDefaults=");
            pw.print(this.mDefaultServicesDisabled.size());
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public void dumpShort(PrintWriter pw, int userId) {
        synchronized (this.mLock) {
            String temporaryName = this.mTemporaryServiceNames.get(userId);
            if (temporaryName != null) {
                pw.print("tmpName=");
                pw.print(temporaryName);
                long ttl = this.mTemporaryServiceExpiration - SystemClock.elapsedRealtime();
                pw.print(" (expires in ");
                TimeUtils.formatDuration(ttl, pw);
                pw.print("), ");
            }
            pw.print("defaultName=");
            pw.print(getDefaultServiceName(userId));
            boolean disabled = this.mDefaultServicesDisabled.get(userId);
            pw.println(disabled ? " (disabled)" : " (enabled)");
        }
    }

    private void notifyTemporaryServiceNameChangedLocked(int userId, String newTemporaryName, boolean isTemporary) {
        ServiceNameResolver.NameResolverListener nameResolverListener = this.mOnSetCallback;
        if (nameResolverListener != null) {
            nameResolverListener.onNameResolved(userId, newTemporaryName, isTemporary);
        }
    }
}