package com.android.server.appprediction;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.service.appprediction.IPredictionService;
import com.android.internal.infra.AbstractMultiplePendingRequestsRemoteService;
import com.android.internal.infra.AbstractRemoteService;

/* loaded from: classes.dex */
public class RemoteAppPredictionService extends AbstractMultiplePendingRequestsRemoteService<RemoteAppPredictionService, IPredictionService> {
    private static final String TAG = "RemoteAppPredictionService";
    private static final long TIMEOUT_REMOTE_REQUEST_MILLIS = 2000;
    private final RemoteAppPredictionServiceCallbacks mCallback;

    /* loaded from: classes.dex */
    public interface RemoteAppPredictionServiceCallbacks extends AbstractRemoteService.VultureCallback<RemoteAppPredictionService> {
        void onConnectedStateChanged(boolean z);

        void onFailureOrTimeout(boolean z);
    }

    public RemoteAppPredictionService(Context context, String serviceInterface, ComponentName componentName, int userId, RemoteAppPredictionServiceCallbacks callback, boolean bindInstantServiceAllowed, boolean verbose) {
        super(context, serviceInterface, componentName, userId, callback, context.getMainThreadHandler(), bindInstantServiceAllowed ? 4194304 : 0, verbose, 1);
        this.mCallback = callback;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public IPredictionService getServiceInterface(IBinder service) {
        return IPredictionService.Stub.asInterface(service);
    }

    protected long getTimeoutIdleBindMillis() {
        return 0L;
    }

    protected long getRemoteRequestMillis() {
        return TIMEOUT_REMOTE_REQUEST_MILLIS;
    }

    public void reconnect() {
        super.scheduleBind();
    }

    public void scheduleOnResolvedService(AbstractRemoteService.AsyncRequest<IPredictionService> request) {
        scheduleAsyncRequest(request);
    }

    public void executeOnResolvedService(AbstractRemoteService.AsyncRequest<IPredictionService> request) {
        executeAsyncRequest(request);
    }

    protected void handleOnConnectedStateChanged(boolean connected) {
        RemoteAppPredictionServiceCallbacks remoteAppPredictionServiceCallbacks = this.mCallback;
        if (remoteAppPredictionServiceCallbacks != null) {
            remoteAppPredictionServiceCallbacks.onConnectedStateChanged(connected);
        }
    }
}