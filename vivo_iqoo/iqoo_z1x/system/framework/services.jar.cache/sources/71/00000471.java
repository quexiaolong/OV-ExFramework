package com.android.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.hardware.tv.cec.V1_0.CecMessageType;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.watchdog.ExplicitHealthCheckService;
import android.service.watchdog.IExplicitHealthCheckService;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class ExplicitHealthCheckController {
    private static final String TAG = "ExplicitHealthCheckController";
    private ServiceConnection mConnection;
    private final Context mContext;
    private boolean mEnabled;
    private final Object mLock = new Object();
    private Runnable mNotifySyncRunnable;
    private Consumer<String> mPassedConsumer;
    private IExplicitHealthCheckService mRemoteService;
    private Consumer<List<ExplicitHealthCheckService.PackageConfig>> mSupportedConsumer;

    public ExplicitHealthCheckController(Context context) {
        this.mContext = context;
    }

    public void setEnabled(boolean enabled) {
        synchronized (this.mLock) {
            StringBuilder sb = new StringBuilder();
            sb.append("Explicit health checks ");
            sb.append(enabled ? "enabled." : "disabled.");
            Slog.i(TAG, sb.toString());
            this.mEnabled = enabled;
        }
    }

    public void setCallbacks(Consumer<String> passedConsumer, Consumer<List<ExplicitHealthCheckService.PackageConfig>> supportedConsumer, Runnable notifySyncRunnable) {
        synchronized (this.mLock) {
            if (this.mPassedConsumer != null || this.mSupportedConsumer != null || this.mNotifySyncRunnable != null) {
                Slog.wtf(TAG, "Resetting health check controller callbacks");
            }
            Objects.requireNonNull(passedConsumer);
            this.mPassedConsumer = passedConsumer;
            Objects.requireNonNull(supportedConsumer);
            this.mSupportedConsumer = supportedConsumer;
            Objects.requireNonNull(notifySyncRunnable);
            this.mNotifySyncRunnable = notifySyncRunnable;
        }
    }

    public void syncRequests(final Set<String> newRequestedPackages) {
        boolean enabled;
        synchronized (this.mLock) {
            enabled = this.mEnabled;
        }
        if (!enabled) {
            Slog.i(TAG, "Health checks disabled, no supported packages");
            this.mSupportedConsumer.accept(Collections.emptyList());
            return;
        }
        getSupportedPackages(new Consumer() { // from class: com.android.server.-$$Lambda$ExplicitHealthCheckController$x4g41SYVR_nHQxV-RQY6VIfh1zs
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ExplicitHealthCheckController.this.lambda$syncRequests$3$ExplicitHealthCheckController(newRequestedPackages, (List) obj);
            }
        });
    }

    public /* synthetic */ void lambda$syncRequests$3$ExplicitHealthCheckController(final Set newRequestedPackages, final List supportedPackageConfigs) {
        this.mSupportedConsumer.accept(supportedPackageConfigs);
        getRequestedPackages(new Consumer() { // from class: com.android.server.-$$Lambda$ExplicitHealthCheckController$NCzfilqDrFIbp6BuyCJrDsdAk5I
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ExplicitHealthCheckController.this.lambda$syncRequests$2$ExplicitHealthCheckController(supportedPackageConfigs, newRequestedPackages, (List) obj);
            }
        });
    }

    public /* synthetic */ void lambda$syncRequests$2$ExplicitHealthCheckController(List supportedPackageConfigs, Set newRequestedPackages, List previousRequestedPackages) {
        synchronized (this.mLock) {
            Set<String> supportedPackages = new ArraySet<>();
            Iterator it = supportedPackageConfigs.iterator();
            while (it.hasNext()) {
                ExplicitHealthCheckService.PackageConfig config = (ExplicitHealthCheckService.PackageConfig) it.next();
                supportedPackages.add(config.getPackageName());
            }
            newRequestedPackages.retainAll(supportedPackages);
            actOnDifference(previousRequestedPackages, newRequestedPackages, new Consumer() { // from class: com.android.server.-$$Lambda$ExplicitHealthCheckController$fE2pZ6ZhwFEJPuOl0ochqPnSmyI
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ExplicitHealthCheckController.this.lambda$syncRequests$0$ExplicitHealthCheckController((String) obj);
                }
            });
            actOnDifference(newRequestedPackages, previousRequestedPackages, new Consumer() { // from class: com.android.server.-$$Lambda$ExplicitHealthCheckController$ucIBQc_IW2iYt6j4dngAncLT6nQ
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ExplicitHealthCheckController.this.lambda$syncRequests$1$ExplicitHealthCheckController((String) obj);
                }
            });
            if (newRequestedPackages.isEmpty()) {
                Slog.i(TAG, "No more health check requests, unbinding...");
                unbindService();
            }
        }
    }

    private void actOnDifference(Collection<String> collection1, Collection<String> collection2, Consumer<String> action) {
        for (String packageName : collection1) {
            if (!collection2.contains(packageName)) {
                action.accept(packageName);
            }
        }
    }

    /* renamed from: request */
    public void lambda$syncRequests$1$ExplicitHealthCheckController(String packageName) {
        synchronized (this.mLock) {
            if (prepareServiceLocked("request health check for " + packageName)) {
                Slog.i(TAG, "Requesting health check for package " + packageName);
                try {
                    this.mRemoteService.request(packageName);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to request health check for package " + packageName, e);
                }
            }
        }
    }

    /* renamed from: cancel */
    public void lambda$syncRequests$0$ExplicitHealthCheckController(String packageName) {
        synchronized (this.mLock) {
            if (prepareServiceLocked("cancel health check for " + packageName)) {
                Slog.i(TAG, "Cancelling health check for package " + packageName);
                try {
                    this.mRemoteService.cancel(packageName);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to cancel health check for package " + packageName, e);
                }
            }
        }
    }

    private void getSupportedPackages(final Consumer<List<ExplicitHealthCheckService.PackageConfig>> consumer) {
        synchronized (this.mLock) {
            if (prepareServiceLocked("get health check supported packages")) {
                Slog.d(TAG, "Getting health check supported packages");
                try {
                    this.mRemoteService.getSupportedPackages(new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.-$$Lambda$ExplicitHealthCheckController$_PgTaUvckhKQczm_86P6Mowec48
                        public final void onResult(Bundle bundle) {
                            ExplicitHealthCheckController.lambda$getSupportedPackages$4(consumer, bundle);
                        }
                    }));
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to get health check supported packages", e);
                }
            }
        }
    }

    public static /* synthetic */ void lambda$getSupportedPackages$4(Consumer consumer, Bundle result) {
        ArrayList parcelableArrayList = result.getParcelableArrayList("android.service.watchdog.extra.supported_packages");
        Slog.i(TAG, "Explicit health check supported packages " + parcelableArrayList);
        consumer.accept(parcelableArrayList);
    }

    private void getRequestedPackages(final Consumer<List<String>> consumer) {
        synchronized (this.mLock) {
            if (prepareServiceLocked("get health check requested packages")) {
                Slog.d(TAG, "Getting health check requested packages");
                try {
                    this.mRemoteService.getRequestedPackages(new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.-$$Lambda$ExplicitHealthCheckController$MJhpX-SveTcXQEYQTQa3k6RpjzU
                        public final void onResult(Bundle bundle) {
                            ExplicitHealthCheckController.lambda$getRequestedPackages$5(consumer, bundle);
                        }
                    }));
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to get health check requested packages", e);
                }
            }
        }
    }

    public static /* synthetic */ void lambda$getRequestedPackages$5(Consumer consumer, Bundle result) {
        ArrayList<String> stringArrayList = result.getStringArrayList("android.service.watchdog.extra.requested_packages");
        Slog.i(TAG, "Explicit health check requested packages " + stringArrayList);
        consumer.accept(stringArrayList);
    }

    public void bindService() {
        synchronized (this.mLock) {
            if (this.mEnabled && this.mConnection == null && this.mRemoteService == null) {
                ComponentName component = getServiceComponentNameLocked();
                if (component == null) {
                    Slog.wtf(TAG, "Explicit health check service not found");
                    return;
                }
                Intent intent = new Intent();
                intent.setComponent(component);
                ServiceConnection serviceConnection = new ServiceConnection() { // from class: com.android.server.ExplicitHealthCheckController.1
                    {
                        ExplicitHealthCheckController.this = this;
                    }

                    @Override // android.content.ServiceConnection
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Slog.i(ExplicitHealthCheckController.TAG, "Explicit health check service is connected " + name);
                        ExplicitHealthCheckController.this.initState(service);
                    }

                    @Override // android.content.ServiceConnection
                    public void onServiceDisconnected(ComponentName name) {
                        Slog.i(ExplicitHealthCheckController.TAG, "Explicit health check service is disconnected " + name);
                        synchronized (ExplicitHealthCheckController.this.mLock) {
                            ExplicitHealthCheckController.this.mRemoteService = null;
                        }
                    }

                    @Override // android.content.ServiceConnection
                    public void onBindingDied(ComponentName name) {
                        Slog.i(ExplicitHealthCheckController.TAG, "Explicit health check service binding is dead. Rebind: " + name);
                        ExplicitHealthCheckController.this.unbindService();
                        ExplicitHealthCheckController.this.bindService();
                    }

                    @Override // android.content.ServiceConnection
                    public void onNullBinding(ComponentName name) {
                        Slog.wtf(ExplicitHealthCheckController.TAG, "Explicit health check service binding is null?? " + name);
                    }
                };
                this.mConnection = serviceConnection;
                this.mContext.bindServiceAsUser(intent, serviceConnection, 1, UserHandle.of(0));
                Slog.i(TAG, "Explicit health check service is bound");
                return;
            }
            if (!this.mEnabled) {
                Slog.i(TAG, "Not binding to service, service disabled");
            } else if (this.mRemoteService != null) {
                Slog.i(TAG, "Not binding to service, service already connected");
            } else {
                Slog.i(TAG, "Not binding to service, service already connecting");
            }
        }
    }

    public void unbindService() {
        synchronized (this.mLock) {
            if (this.mRemoteService != null) {
                this.mContext.unbindService(this.mConnection);
                this.mRemoteService = null;
                this.mConnection = null;
            }
            Slog.i(TAG, "Explicit health check service is unbound");
        }
    }

    private ServiceInfo getServiceInfoLocked() {
        String packageName = this.mContext.getPackageManager().getServicesSystemSharedLibraryPackageName();
        if (packageName == null) {
            Slog.w(TAG, "no external services package!");
            return null;
        }
        Intent intent = new Intent("android.service.watchdog.ExplicitHealthCheckService");
        intent.setPackage(packageName);
        ResolveInfo resolveInfo = this.mContext.getPackageManager().resolveService(intent, CecMessageType.REPORT_PHYSICAL_ADDRESS);
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            Slog.w(TAG, "No valid components found.");
            return null;
        }
        return resolveInfo.serviceInfo;
    }

    private ComponentName getServiceComponentNameLocked() {
        ServiceInfo serviceInfo = getServiceInfoLocked();
        if (serviceInfo == null) {
            return null;
        }
        ComponentName name = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        if (!"android.permission.BIND_EXPLICIT_HEALTH_CHECK_SERVICE".equals(serviceInfo.permission)) {
            Slog.w(TAG, name.flattenToShortString() + " does not require permission android.permission.BIND_EXPLICIT_HEALTH_CHECK_SERVICE");
            return null;
        }
        return name;
    }

    public void initState(IBinder service) {
        synchronized (this.mLock) {
            if (!this.mEnabled) {
                Slog.w(TAG, "Attempting to connect disabled service?? Unbinding...");
                unbindService();
                return;
            }
            IExplicitHealthCheckService asInterface = IExplicitHealthCheckService.Stub.asInterface(service);
            this.mRemoteService = asInterface;
            try {
                asInterface.setCallback(new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.-$$Lambda$ExplicitHealthCheckController$6YGiVtgCnlJ0hMIeX5TzlFUaNrY
                    public final void onResult(Bundle bundle) {
                        ExplicitHealthCheckController.this.lambda$initState$6$ExplicitHealthCheckController(bundle);
                    }
                }));
                Slog.i(TAG, "Service initialized, syncing requests");
            } catch (RemoteException e) {
                Slog.wtf(TAG, "Could not setCallback on explicit health check service");
            }
            this.mNotifySyncRunnable.run();
        }
    }

    public /* synthetic */ void lambda$initState$6$ExplicitHealthCheckController(Bundle result) {
        String packageName = result.getString("android.service.watchdog.extra.health_check_passed_package");
        if (!TextUtils.isEmpty(packageName)) {
            Consumer<String> consumer = this.mPassedConsumer;
            if (consumer == null) {
                Slog.wtf(TAG, "Health check passed for package " + packageName + "but no consumer registered.");
                return;
            }
            consumer.accept(packageName);
            return;
        }
        Slog.wtf(TAG, "Empty package passed explicit health check?");
    }

    private boolean prepareServiceLocked(String action) {
        if (this.mRemoteService != null && this.mEnabled) {
            return true;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Service not ready to ");
        sb.append(action);
        sb.append(this.mEnabled ? ". Binding..." : ". Disabled");
        Slog.i(TAG, sb.toString());
        if (this.mEnabled) {
            bindService();
            return false;
        }
        return false;
    }
}