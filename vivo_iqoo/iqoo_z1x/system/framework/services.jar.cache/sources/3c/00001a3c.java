package com.android.server.tv;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.media.tv.ITvRemoteProvider;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import java.io.PrintWriter;

/* loaded from: classes2.dex */
final class TvRemoteProviderProxy implements ServiceConnection {
    protected static final String SERVICE_INTERFACE = "com.android.media.tv.remoteprovider.TvRemoteProvider";
    private boolean mBound;
    private final ComponentName mComponentName;
    private boolean mConnected;
    private final Context mContext;
    private final Object mLock;
    private boolean mRunning;
    private final int mUid;
    private final int mUserId;
    private static final String TAG = "TvRemoteProviderProxy";
    private static final boolean DEBUG = Log.isLoggable(TAG, 2);

    /* JADX INFO: Access modifiers changed from: package-private */
    public TvRemoteProviderProxy(Context context, Object lock, ComponentName componentName, int userId, int uid) {
        this.mContext = context;
        this.mLock = lock;
        this.mComponentName = componentName;
        this.mUserId = userId;
        this.mUid = uid;
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "Proxy");
        pw.println(prefix + "  mUserId=" + this.mUserId);
        pw.println(prefix + "  mRunning=" + this.mRunning);
        pw.println(prefix + "  mBound=" + this.mBound);
        pw.println(prefix + "  mConnected=" + this.mConnected);
    }

    public boolean hasComponentName(String packageName, String className) {
        return this.mComponentName.getPackageName().equals(packageName) && this.mComponentName.getClassName().equals(className);
    }

    public void start() {
        if (!this.mRunning) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Starting");
            }
            this.mRunning = true;
            bind();
        }
    }

    public void stop() {
        if (this.mRunning) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Stopping");
            }
            this.mRunning = false;
            unbind();
        }
    }

    public void rebindIfDisconnected() {
        if (this.mRunning && !this.mConnected) {
            unbind();
            bind();
        }
    }

    private void bind() {
        if (!this.mBound) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Binding");
            }
            Intent service = new Intent(SERVICE_INTERFACE);
            service.setComponent(this.mComponentName);
            try {
                boolean bindServiceAsUser = this.mContext.bindServiceAsUser(service, this, AudioFormat.AAC_MAIN, new UserHandle(this.mUserId));
                this.mBound = bindServiceAsUser;
                if (DEBUG && !bindServiceAsUser) {
                    Slog.d(TAG, this + ": Bind failed");
                }
            } catch (SecurityException ex) {
                if (DEBUG) {
                    Slog.d(TAG, this + ": Bind failed", ex);
                }
            }
        }
    }

    private void unbind() {
        if (this.mBound) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Unbinding");
            }
            this.mBound = false;
            this.mContext.unbindService(this);
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (DEBUG) {
            Slog.d(TAG, this + ": onServiceConnected()");
        }
        this.mConnected = true;
        ITvRemoteProvider provider = ITvRemoteProvider.Stub.asInterface(service);
        if (provider == null) {
            Slog.e(TAG, this + ": Invalid binder");
            return;
        }
        try {
            provider.setRemoteServiceInputSink(new TvRemoteServiceInput(this.mLock, provider));
        } catch (RemoteException e) {
            Slog.e(TAG, this + ": Failed remote call to setRemoteServiceInputSink");
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceDisconnected(ComponentName name) {
        this.mConnected = false;
        if (DEBUG) {
            Slog.d(TAG, this + ": onServiceDisconnected()");
        }
    }
}