package com.android.server.print;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.printservice.recommendation.IRecommendationService;
import android.printservice.recommendation.IRecommendationServiceCallbacks;
import android.printservice.recommendation.RecommendationInfo;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.List;

/* loaded from: classes2.dex */
class RemotePrintServiceRecommendationService {
    private static final String LOG_TAG = "RemotePrintServiceRecS";
    private final Connection mConnection;
    private final Context mContext;
    private boolean mIsBound;
    private final Object mLock = new Object();
    private IRecommendationService mService;

    /* loaded from: classes2.dex */
    public interface RemotePrintServiceRecommendationServiceCallbacks {
        void onPrintServiceRecommendationsUpdated(List<RecommendationInfo> list);
    }

    private Intent getServiceIntent(UserHandle userHandle) throws Exception {
        List<ResolveInfo> installedServices = this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.printservice.recommendation.RecommendationService"), 268435588, userHandle.getIdentifier());
        if (installedServices.size() != 1) {
            throw new Exception(installedServices.size() + " instead of exactly one service found");
        }
        ResolveInfo installedService = installedServices.get(0);
        ComponentName serviceName = new ComponentName(installedService.serviceInfo.packageName, installedService.serviceInfo.name);
        ApplicationInfo appInfo = this.mContext.getPackageManager().getApplicationInfo(installedService.serviceInfo.packageName, 0);
        if (appInfo == null) {
            throw new Exception("Cannot read appInfo for service");
        }
        if ((1 & appInfo.flags) == 0) {
            throw new Exception("Service is not part of the system");
        }
        if (!"android.permission.BIND_PRINT_RECOMMENDATION_SERVICE".equals(installedService.serviceInfo.permission)) {
            throw new Exception("Service " + serviceName.flattenToShortString() + " does not require permission android.permission.BIND_PRINT_RECOMMENDATION_SERVICE");
        }
        Intent serviceIntent = new Intent();
        serviceIntent.setComponent(serviceName);
        return serviceIntent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemotePrintServiceRecommendationService(Context context, UserHandle userHandle, RemotePrintServiceRecommendationServiceCallbacks callbacks) {
        this.mContext = context;
        this.mConnection = new Connection(callbacks);
        try {
            Intent serviceIntent = getServiceIntent(userHandle);
            synchronized (this.mLock) {
                boolean bindServiceAsUser = this.mContext.bindServiceAsUser(serviceIntent, this.mConnection, AudioFormat.AAC_MAIN, userHandle);
                this.mIsBound = bindServiceAsUser;
                if (!bindServiceAsUser) {
                    throw new Exception("Failed to bind to service " + serviceIntent);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not connect to print service recommendation service", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void close() {
        synchronized (this.mLock) {
            if (this.mService != null) {
                try {
                    this.mService.registerCallbacks((IRecommendationServiceCallbacks) null);
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Could not unregister callbacks", e);
                }
                this.mService = null;
            }
            if (this.mIsBound) {
                this.mContext.unbindService(this.mConnection);
                this.mIsBound = false;
            }
        }
    }

    protected void finalize() throws Throwable {
        if (this.mIsBound || this.mService != null) {
            Log.w(LOG_TAG, "Service still connected on finalize()");
            close();
        }
        super.finalize();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Connection implements ServiceConnection {
        private final RemotePrintServiceRecommendationServiceCallbacks mCallbacks;

        public Connection(RemotePrintServiceRecommendationServiceCallbacks callbacks) {
            this.mCallbacks = callbacks;
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (RemotePrintServiceRecommendationService.this.mLock) {
                RemotePrintServiceRecommendationService.this.mService = IRecommendationService.Stub.asInterface(service);
                try {
                    RemotePrintServiceRecommendationService.this.mService.registerCallbacks(new IRecommendationServiceCallbacks.Stub() { // from class: com.android.server.print.RemotePrintServiceRecommendationService.Connection.1
                        public void onRecommendationsUpdated(List<RecommendationInfo> recommendations) {
                            synchronized (RemotePrintServiceRecommendationService.this.mLock) {
                                if (RemotePrintServiceRecommendationService.this.mIsBound && RemotePrintServiceRecommendationService.this.mService != null) {
                                    if (recommendations != null) {
                                        Preconditions.checkCollectionElementsNotNull(recommendations, "recommendation");
                                    }
                                    Connection.this.mCallbacks.onPrintServiceRecommendationsUpdated(recommendations);
                                }
                            }
                        }
                    });
                } catch (RemoteException e) {
                    Log.e(RemotePrintServiceRecommendationService.LOG_TAG, "Could not register callbacks", e);
                }
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            Log.w(RemotePrintServiceRecommendationService.LOG_TAG, "Unexpected termination of connection");
            synchronized (RemotePrintServiceRecommendationService.this.mLock) {
                RemotePrintServiceRecommendationService.this.mService = null;
            }
        }
    }
}