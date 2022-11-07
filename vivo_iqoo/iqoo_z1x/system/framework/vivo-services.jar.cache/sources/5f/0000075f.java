package com.vivo.services.rms.sp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import com.vivo.services.rms.sp.config.Helpers;
import com.vivo.services.rms.sp.sdk.ISpClient;
import com.vivo.services.rms.sp.sdk.SpClientStub;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class SpClientNotifier implements ServiceConnection {
    private static final int MAX_TRY_REBIND_COUNT = 370;
    private static final String PACKAGE_NAME = "com.vivo.sps";
    public static final String PROCESS_NAME = "com.vivo.sps";
    private static final String SERVICE_NAME = "com.vivo.sp.service.SpClientService";
    public static final String TAG = "SpManager";
    private int mConnectedTimes;
    private Context mContext;
    private Handler mHandler;
    private volatile long mLastConnectedTime;
    private ISpClient mNotifier;
    private volatile int mPid;
    private int mTryRebindTimes;

    public void initialize(Context context) {
        this.mContext = context;
    }

    public void startSps() {
        VSlog.i("SpManager", "startOtherServices");
        bindServiceFirstTime();
    }

    public void systemReady() {
    }

    public static SpClientNotifier getInstance() {
        return Instance.INSTANCE;
    }

    private SpClientNotifier() {
        this.mPid = -1;
        this.mLastConnectedTime = 0L;
        HandlerThread thread = new HandlerThread("sp_client");
        thread.start();
        this.mHandler = new H(thread.getLooper());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static SpClientNotifier INSTANCE = new SpClientNotifier();

        private Instance() {
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceConnected(ComponentName name, IBinder listener) {
        synchronized (this) {
            if (!isServiceConnected()) {
                ISpClient l = SpClientStub.asInterface(listener);
                try {
                    l.doInit(new Bundle());
                    this.mHandler.removeMessages(0);
                    this.mNotifier = l;
                    this.mPid = l.myPid();
                    this.mConnectedTimes++;
                    this.mTryRebindTimes = 0;
                    this.mLastConnectedTime = SystemClock.uptimeMillis();
                    VSlog.i("SpManager", String.format("SpClientService is connected pid=%d, times=%d", Integer.valueOf(this.mPid), Integer.valueOf(this.mConnectedTimes)));
                } catch (Exception e) {
                    VSlog.e("SpManager", "onServiceConnected exeption : " + e.getMessage());
                }
            }
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceDisconnected(ComponentName name) {
        synchronized (this) {
            if (isServiceConnected()) {
                this.mPid = -1;
                this.mNotifier = null;
                this.mHandler.removeMessages(0);
                bindServiceIfNeeded(SystemClock.uptimeMillis() - this.mLastConnectedTime > 1000 ? 0L : 1000L);
                VSlog.d("SpManager", "SpClientService is disconnected");
            }
        }
    }

    private void bindServiceFirstTime() {
        Handler handler = this.mHandler;
        handler.sendMessageAtFrontOfQueue(handler.obtainMessage(0));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bindServiceIfNeeded() {
        bindServiceIfNeeded(1000L);
    }

    private void bindServiceIfNeeded(long delay) {
        if (!isServiceConnected()) {
            if (!this.mHandler.hasMessages(0)) {
                int i = this.mTryRebindTimes + 1;
                this.mTryRebindTimes = i;
                if (i < MAX_TRY_REBIND_COUNT) {
                    Handler handler = this.mHandler;
                    handler.sendMessageDelayed(handler.obtainMessage(0), delay);
                    return;
                }
            }
            if (this.mTryRebindTimes >= MAX_TRY_REBIND_COUNT) {
                VSlog.e("SpManager", "Failed to connect to sps");
                long version = Helpers.getVersion("com.vivo.sps", this.mContext);
                if (version <= 0) {
                    version = 1;
                    VSlog.e("SpManager", "SUPER PROCESS APPLICATION IS NOT EXIST!");
                }
                BadPackageManager.getInstance().addBadPackage("com.vivo.sps", version, 4096);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void realBindService() {
        synchronized (this) {
            if (!isServiceConnected()) {
                Intent intent = new Intent();
                intent.putExtra("version", "1.0");
                intent.putExtra("caller", this.mContext.getPackageName());
                intent.setComponent(new ComponentName("com.vivo.sps", SERVICE_NAME));
                try {
                    this.mContext.bindServiceAsUser(intent, this, 1, Process.myUserHandle());
                } catch (Exception e) {
                    VSlog.e("SpManager", e.getMessage());
                }
            }
        }
    }

    public boolean isServiceConnected() {
        return this.mPid > 0;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        try {
            if (this.mNotifier != null && SystemProperties.getBoolean("persist.rms.allow_dump", false)) {
                this.mNotifier.dumpData(fd, args);
            }
        } catch (Exception e) {
            pw.println("dumpData exeption : " + e.getMessage());
        }
    }

    public boolean setBundle(String name, Bundle bundle) {
        if (TextUtils.isEmpty(name) || bundle == null) {
            return false;
        }
        try {
            if (this.mNotifier == null) {
                return false;
            }
            boolean result = this.mNotifier.setBundle(name, bundle);
            return result;
        } catch (Exception e) {
            VSlog.e("SpManager", "setBundle failed.", e);
            return false;
        }
    }

    public Bundle getBundle(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        try {
            if (this.mNotifier != null) {
                return this.mNotifier.getBundle(name);
            }
        } catch (Exception e) {
            VSlog.e("SpManager", "getBundle failed.", e);
        }
        return null;
    }

    public void notifyErrorPackage(String pkgName, int uid, long versionCode, int flag) {
        if (TextUtils.isEmpty(pkgName)) {
            return;
        }
        try {
            if (this.mNotifier != null) {
                this.mNotifier.notifyErrorPackage(pkgName, uid, versionCode, flag);
            }
        } catch (Exception e) {
            VSlog.e("SpManager", "notifyErrorPackage failed.", e);
        }
    }

    /* loaded from: classes.dex */
    private class H extends Handler {
        private static final int MSG_BINDSERVICE = 0;

        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                SpClientNotifier.this.realBindService();
                SpClientNotifier.this.bindServiceIfNeeded();
            }
        }
    }
}