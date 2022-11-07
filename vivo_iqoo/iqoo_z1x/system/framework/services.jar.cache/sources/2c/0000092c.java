package com.android.server.autofill;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.hardware.tv.cec.V1_0.CecMessageType;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.service.autofill.IInlineSuggestionRenderService;
import android.service.autofill.IInlineSuggestionUiCallback;
import android.service.autofill.InlinePresentation;
import android.util.Slog;
import com.android.internal.infra.AbstractMultiplePendingRequestsRemoteService;
import com.android.internal.infra.AbstractRemoteService;

/* loaded from: classes.dex */
public final class RemoteInlineSuggestionRenderService extends AbstractMultiplePendingRequestsRemoteService<RemoteInlineSuggestionRenderService, IInlineSuggestionRenderService> {
    private static final String TAG = "RemoteInlineSuggestionRenderService";
    private final long mIdleUnbindTimeoutMs;

    /* loaded from: classes.dex */
    interface InlineSuggestionRenderCallbacks extends AbstractRemoteService.VultureCallback<RemoteInlineSuggestionRenderService> {
    }

    public RemoteInlineSuggestionRenderService(Context context, ComponentName componentName, String serviceInterface, int userId, InlineSuggestionRenderCallbacks callback, boolean bindInstantServiceAllowed, boolean verbose) {
        super(context, serviceInterface, componentName, userId, callback, context.getMainThreadHandler(), bindInstantServiceAllowed ? 4194304 : 0, verbose, 2);
        this.mIdleUnbindTimeoutMs = 0L;
        ensureBound();
    }

    public IInlineSuggestionRenderService getServiceInterface(IBinder service) {
        return IInlineSuggestionRenderService.Stub.asInterface(service);
    }

    protected long getTimeoutIdleBindMillis() {
        return 0L;
    }

    protected void handleOnConnectedStateChanged(boolean connected) {
        if (connected && getTimeoutIdleBindMillis() != 0) {
            scheduleUnbind();
        }
        super.handleOnConnectedStateChanged(connected);
    }

    public void ensureBound() {
        scheduleBind();
    }

    public static /* synthetic */ void lambda$renderSuggestion$0(IInlineSuggestionUiCallback callback, InlinePresentation presentation, int width, int height, IBinder hostInputToken, int displayId, int userId, int sessionId, IInlineSuggestionRenderService s) throws RemoteException {
        s.renderSuggestion(callback, presentation, width, height, hostInputToken, displayId, userId, sessionId);
    }

    public void renderSuggestion(final IInlineSuggestionUiCallback callback, final InlinePresentation presentation, final int width, final int height, final IBinder hostInputToken, final int displayId, final int userId, final int sessionId) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.autofill.-$$Lambda$RemoteInlineSuggestionRenderService$Ynu9LYMZF_i1OFnFcaANpQNOYfo
            public final void run(IInterface iInterface) {
                RemoteInlineSuggestionRenderService.lambda$renderSuggestion$0(callback, presentation, width, height, hostInputToken, displayId, userId, sessionId, (IInlineSuggestionRenderService) iInterface);
            }
        });
    }

    public static /* synthetic */ void lambda$getInlineSuggestionsRendererInfo$1(RemoteCallback callback, IInlineSuggestionRenderService s) throws RemoteException {
        s.getInlineSuggestionsRendererInfo(callback);
    }

    public void getInlineSuggestionsRendererInfo(final RemoteCallback callback) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.autofill.-$$Lambda$RemoteInlineSuggestionRenderService$FqcxltVlZ48okYD3kwsYbGd36eo
            public final void run(IInterface iInterface) {
                RemoteInlineSuggestionRenderService.lambda$getInlineSuggestionsRendererInfo$1(callback, (IInlineSuggestionRenderService) iInterface);
            }
        });
    }

    public static /* synthetic */ void lambda$destroySuggestionViews$2(int userId, int sessionId, IInlineSuggestionRenderService s) throws RemoteException {
        s.destroySuggestionViews(userId, sessionId);
    }

    public void destroySuggestionViews(final int userId, final int sessionId) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.autofill.-$$Lambda$RemoteInlineSuggestionRenderService$TuHcQ-1NgaycY4boDHeJGU4PhnA
            public final void run(IInterface iInterface) {
                RemoteInlineSuggestionRenderService.lambda$destroySuggestionViews$2(userId, sessionId, (IInlineSuggestionRenderService) iInterface);
            }
        });
    }

    private static ServiceInfo getServiceInfo(Context context, int userId) {
        String packageName = context.getPackageManager().getServicesSystemSharedLibraryPackageName();
        if (packageName == null) {
            Slog.w(TAG, "no external services package!");
            return null;
        }
        Intent intent = new Intent("android.service.autofill.InlineSuggestionRenderService");
        intent.setPackage(packageName);
        ResolveInfo resolveInfo = context.getPackageManager().resolveServiceAsUser(intent, CecMessageType.REPORT_PHYSICAL_ADDRESS, userId);
        ServiceInfo serviceInfo = resolveInfo == null ? null : resolveInfo.serviceInfo;
        if (resolveInfo == null || serviceInfo == null) {
            Slog.w(TAG, "No valid components found.");
            return null;
        } else if (!"android.permission.BIND_INLINE_SUGGESTION_RENDER_SERVICE".equals(serviceInfo.permission)) {
            Slog.w(TAG, serviceInfo.name + " does not require permission android.permission.BIND_INLINE_SUGGESTION_RENDER_SERVICE");
            return null;
        } else {
            return serviceInfo;
        }
    }

    public static ComponentName getServiceComponentName(Context context, int userId) {
        ServiceInfo serviceInfo = getServiceInfo(context, userId);
        if (serviceInfo == null) {
            return null;
        }
        ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        if (Helper.sVerbose) {
            Slog.v(TAG, "getServiceComponentName(): " + componentName);
        }
        return componentName;
    }
}