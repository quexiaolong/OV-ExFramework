package com.android.server.tv;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.server.pm.Settings;
import com.android.server.slice.SliceClientPermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class TvRemoteProviderWatcher {
    private final Context mContext;
    private final Handler mHandler;
    private final Object mLock;
    private final PackageManager mPackageManager;
    private final ArrayList<TvRemoteProviderProxy> mProviderProxies;
    private boolean mRunning;
    private final BroadcastReceiver mScanPackagesReceiver;
    private final Runnable mScanPackagesRunnable;
    private final Set<String> mUnbundledServicePackages;
    private final int mUserId;
    private static final String TAG = "TvRemoteProviderWatcher";
    private static final boolean DEBUG = Log.isLoggable(TAG, 2);

    TvRemoteProviderWatcher(Context context, Object lock, Handler handler) {
        this.mProviderProxies = new ArrayList<>();
        this.mUnbundledServicePackages = new HashSet();
        this.mScanPackagesReceiver = new BroadcastReceiver() { // from class: com.android.server.tv.TvRemoteProviderWatcher.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (TvRemoteProviderWatcher.DEBUG) {
                    Slog.d(TvRemoteProviderWatcher.TAG, "Received package manager broadcast: " + intent);
                }
                TvRemoteProviderWatcher.this.mHandler.post(TvRemoteProviderWatcher.this.mScanPackagesRunnable);
            }
        };
        this.mScanPackagesRunnable = new Runnable() { // from class: com.android.server.tv.TvRemoteProviderWatcher.2
            @Override // java.lang.Runnable
            public void run() {
                TvRemoteProviderWatcher.this.scanPackages();
            }
        };
        this.mContext = context;
        this.mHandler = handler;
        this.mUserId = UserHandle.myUserId();
        this.mPackageManager = context.getPackageManager();
        this.mLock = lock;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
        splitter.setString(context.getString(17039974));
        splitter.forEach(new Consumer() { // from class: com.android.server.tv.-$$Lambda$TvRemoteProviderWatcher$dDERmcw8SCyoq7X1l50jggUVY28
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TvRemoteProviderWatcher.this.lambda$new$0$TvRemoteProviderWatcher((String) obj);
            }
        });
    }

    public /* synthetic */ void lambda$new$0$TvRemoteProviderWatcher(String packageName) {
        String packageName2 = packageName.trim();
        if (!packageName2.isEmpty()) {
            this.mUnbundledServicePackages.add(packageName2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TvRemoteProviderWatcher(Context context, Object lock) {
        this(context, lock, new Handler(true));
    }

    public void start() {
        if (DEBUG) {
            Slog.d(TAG, "start()");
        }
        if (!this.mRunning) {
            this.mRunning = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_ADDED");
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addAction("android.intent.action.PACKAGE_REPLACED");
            filter.addAction("android.intent.action.PACKAGE_RESTARTED");
            filter.addDataScheme(Settings.ATTR_PACKAGE);
            this.mContext.registerReceiverAsUser(this.mScanPackagesReceiver, new UserHandle(this.mUserId), filter, null, this.mHandler);
            this.mHandler.post(this.mScanPackagesRunnable);
        }
    }

    public void stop() {
        if (this.mRunning) {
            this.mRunning = false;
            this.mContext.unregisterReceiver(this.mScanPackagesReceiver);
            this.mHandler.removeCallbacks(this.mScanPackagesRunnable);
            for (int i = this.mProviderProxies.size() - 1; i >= 0; i--) {
                this.mProviderProxies.get(i).stop();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scanPackages() {
        if (!this.mRunning) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "scanPackages()");
        }
        int targetIndex = 0;
        Intent intent = new Intent("com.android.media.tv.remoteprovider.TvRemoteProvider");
        for (ResolveInfo resolveInfo : this.mPackageManager.queryIntentServicesAsUser(intent, 0, this.mUserId)) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (serviceInfo != null && verifyServiceTrusted(serviceInfo)) {
                int sourceIndex = findProvider(serviceInfo.packageName, serviceInfo.name);
                if (sourceIndex < 0) {
                    TvRemoteProviderProxy providerProxy = new TvRemoteProviderProxy(this.mContext, this.mLock, new ComponentName(serviceInfo.packageName, serviceInfo.name), this.mUserId, serviceInfo.applicationInfo.uid);
                    providerProxy.start();
                    this.mProviderProxies.add(targetIndex, providerProxy);
                    targetIndex++;
                } else if (sourceIndex >= targetIndex) {
                    TvRemoteProviderProxy provider = this.mProviderProxies.get(sourceIndex);
                    provider.start();
                    provider.rebindIfDisconnected();
                    Collections.swap(this.mProviderProxies, sourceIndex, targetIndex);
                    targetIndex++;
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "scanPackages() targetIndex " + targetIndex);
        }
        if (targetIndex < this.mProviderProxies.size()) {
            for (int i = this.mProviderProxies.size() - 1; i >= targetIndex; i--) {
                TvRemoteProviderProxy providerProxy2 = this.mProviderProxies.get(i);
                this.mProviderProxies.remove(providerProxy2);
                providerProxy2.stop();
            }
        }
    }

    boolean verifyServiceTrusted(ServiceInfo serviceInfo) {
        if (serviceInfo.permission == null || !serviceInfo.permission.equals("android.permission.BIND_TV_REMOTE_SERVICE")) {
            Slog.w(TAG, "Ignoring atv remote provider service because it did not require the BIND_TV_REMOTE_SERVICE permission in its manifest: " + serviceInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + serviceInfo.name);
            return false;
        } else if (!this.mUnbundledServicePackages.contains(serviceInfo.packageName)) {
            Slog.w(TAG, "Ignoring atv remote provider service because the package has not been set and/or whitelisted: " + serviceInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + serviceInfo.name);
            return false;
        } else if (!hasNecessaryPermissions(serviceInfo.packageName)) {
            Slog.w(TAG, "Ignoring atv remote provider service because its package does not have TV_VIRTUAL_REMOTE_CONTROLLER permission: " + serviceInfo.packageName);
            return false;
        } else {
            return true;
        }
    }

    private boolean hasNecessaryPermissions(String packageName) {
        if (this.mPackageManager.checkPermission("android.permission.TV_VIRTUAL_REMOTE_CONTROLLER", packageName) == 0) {
            return true;
        }
        return false;
    }

    private int findProvider(String packageName, String className) {
        int count = this.mProviderProxies.size();
        for (int i = 0; i < count; i++) {
            TvRemoteProviderProxy provider = this.mProviderProxies.get(i);
            if (provider.hasComponentName(packageName, className)) {
                return i;
            }
        }
        return -1;
    }
}