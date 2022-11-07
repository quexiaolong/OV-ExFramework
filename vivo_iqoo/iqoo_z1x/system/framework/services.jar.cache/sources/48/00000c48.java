package com.android.server.contentsuggestions;

import android.app.contentsuggestions.ClassificationsRequest;
import android.app.contentsuggestions.IClassificationsCallback;
import android.app.contentsuggestions.ISelectionsCallback;
import android.app.contentsuggestions.SelectionsRequest;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.GraphicBuffer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.service.contentsuggestions.IContentSuggestionsService;
import com.android.internal.infra.AbstractMultiplePendingRequestsRemoteService;
import com.android.internal.infra.AbstractRemoteService;

/* loaded from: classes.dex */
public class RemoteContentSuggestionsService extends AbstractMultiplePendingRequestsRemoteService<RemoteContentSuggestionsService, IContentSuggestionsService> {
    private static final long TIMEOUT_REMOTE_REQUEST_MILLIS = 2000;

    /* loaded from: classes.dex */
    interface Callbacks extends AbstractRemoteService.VultureCallback<RemoteContentSuggestionsService> {
    }

    public RemoteContentSuggestionsService(Context context, ComponentName serviceName, int userId, Callbacks callbacks, boolean bindInstantServiceAllowed, boolean verbose) {
        super(context, "android.service.contentsuggestions.ContentSuggestionsService", serviceName, userId, callbacks, context.getMainThreadHandler(), bindInstantServiceAllowed ? 4194304 : 0, verbose, 1);
    }

    public IContentSuggestionsService getServiceInterface(IBinder service) {
        return IContentSuggestionsService.Stub.asInterface(service);
    }

    protected long getTimeoutIdleBindMillis() {
        return 0L;
    }

    protected long getRemoteRequestMillis() {
        return TIMEOUT_REMOTE_REQUEST_MILLIS;
    }

    public void provideContextImage(final int taskId, final GraphicBuffer contextImage, final int colorSpaceId, final Bundle imageContextRequestExtras) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentsuggestions.-$$Lambda$RemoteContentSuggestionsService$VKh1DoMPNSPjPfnVGdsInmxuqzc
            public final void run(IInterface iInterface) {
                ((IContentSuggestionsService) iInterface).provideContextImage(taskId, contextImage, colorSpaceId, imageContextRequestExtras);
            }
        });
    }

    public void suggestContentSelections(final SelectionsRequest selectionsRequest, final ISelectionsCallback selectionsCallback) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentsuggestions.-$$Lambda$RemoteContentSuggestionsService$yUTbcaYlZCYTmagCkNJ3i2VCkY4
            public final void run(IInterface iInterface) {
                RemoteContentSuggestionsService.lambda$suggestContentSelections$1(selectionsRequest, selectionsCallback, (IContentSuggestionsService) iInterface);
            }
        });
    }

    public static /* synthetic */ void lambda$suggestContentSelections$1(SelectionsRequest selectionsRequest, ISelectionsCallback selectionsCallback, IContentSuggestionsService s) throws RemoteException {
        s.suggestContentSelections(selectionsRequest, selectionsCallback);
    }

    public static /* synthetic */ void lambda$classifyContentSelections$2(ClassificationsRequest classificationsRequest, IClassificationsCallback callback, IContentSuggestionsService s) throws RemoteException {
        s.classifyContentSelections(classificationsRequest, callback);
    }

    public void classifyContentSelections(final ClassificationsRequest classificationsRequest, final IClassificationsCallback callback) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentsuggestions.-$$Lambda$RemoteContentSuggestionsService$eoGnQ2MDLLnW1UBX6wxNE1VBLAk
            public final void run(IInterface iInterface) {
                RemoteContentSuggestionsService.lambda$classifyContentSelections$2(classificationsRequest, callback, (IContentSuggestionsService) iInterface);
            }
        });
    }

    public static /* synthetic */ void lambda$notifyInteraction$3(String requestId, Bundle bundle, IContentSuggestionsService s) throws RemoteException {
        s.notifyInteraction(requestId, bundle);
    }

    public void notifyInteraction(final String requestId, final Bundle bundle) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentsuggestions.-$$Lambda$RemoteContentSuggestionsService$Enqw46SYVKFK9F2xX4qUcIu5_3I
            public final void run(IInterface iInterface) {
                RemoteContentSuggestionsService.lambda$notifyInteraction$3(requestId, bundle, (IContentSuggestionsService) iInterface);
            }
        });
    }
}