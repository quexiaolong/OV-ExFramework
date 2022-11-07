package com.android.server.media;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioRoutesInfo;
import android.media.IAudioRoutesObserver;
import android.media.IAudioService;
import android.media.MediaRoute2Info;
import android.media.MediaRoute2ProviderInfo;
import android.media.RouteDiscoveryPreference;
import android.media.RoutingSessionInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.server.media.BluetoothRouteProvider;
import com.android.server.media.MediaRoute2Provider;
import com.android.server.media.SystemMediaRoute2Provider;
import java.util.List;
import java.util.Objects;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class SystemMediaRoute2Provider extends MediaRoute2Provider {
    static final String DEFAULT_ROUTE_ID = "DEFAULT_ROUTE";
    static final String DEVICE_ROUTE_ID = "DEVICE_ROUTE";
    static final String SYSTEM_SESSION_ID = "SYSTEM_SESSION";
    private final AudioManager mAudioManager;
    final IAudioRoutesObserver.Stub mAudioRoutesObserver;
    private final IAudioService mAudioService;
    private final BluetoothRouteProvider mBtRouteProvider;
    private final Context mContext;
    final AudioRoutesInfo mCurAudioRoutesInfo;
    MediaRoute2Info mDefaultRoute;
    RoutingSessionInfo mDefaultSessionInfo;
    MediaRoute2Info mDeviceRoute;
    int mDeviceVolume;
    private final Handler mHandler;
    private volatile SessionCreationRequest mPendingSessionCreationRequest;
    private final Object mRequestLock;
    private String mSelectedRouteId;
    private static final String TAG = "MR2SystemProvider";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static ComponentName sComponentName = new ComponentName(SystemMediaRoute2Provider.class.getPackage().getName(), SystemMediaRoute2Provider.class.getName());

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.media.SystemMediaRoute2Provider$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends IAudioRoutesObserver.Stub {
        AnonymousClass1() {
        }

        public void dispatchAudioRoutesChanged(final AudioRoutesInfo newRoutes) {
            SystemMediaRoute2Provider.this.mHandler.post(new Runnable() { // from class: com.android.server.media.-$$Lambda$SystemMediaRoute2Provider$1$ebcdsGsKcvePyBmWcsYxnmypK0U
                @Override // java.lang.Runnable
                public final void run() {
                    SystemMediaRoute2Provider.AnonymousClass1.this.lambda$dispatchAudioRoutesChanged$0$SystemMediaRoute2Provider$1(newRoutes);
                }
            });
        }

        public /* synthetic */ void lambda$dispatchAudioRoutesChanged$0$SystemMediaRoute2Provider$1(AudioRoutesInfo newRoutes) {
            SystemMediaRoute2Provider.this.updateDeviceRoute(newRoutes);
            SystemMediaRoute2Provider.this.notifyProviderState();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemMediaRoute2Provider(Context context) {
        super(sComponentName);
        this.mCurAudioRoutesInfo = new AudioRoutesInfo();
        this.mRequestLock = new Object();
        this.mAudioRoutesObserver = new AnonymousClass1();
        this.mIsSystemRouteProvider = true;
        this.mContext = context;
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        IAudioService asInterface = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        this.mAudioService = asInterface;
        AudioRoutesInfo newAudioRoutes = null;
        try {
            newAudioRoutes = asInterface.startWatchingRoutes(this.mAudioRoutesObserver);
        } catch (RemoteException e) {
        }
        updateDeviceRoute(newAudioRoutes);
        this.mBtRouteProvider = BluetoothRouteProvider.getInstance(context, new BluetoothRouteProvider.BluetoothRoutesUpdatedListener() { // from class: com.android.server.media.-$$Lambda$SystemMediaRoute2Provider$aOlVIsBkXTnw1voyl2-9vhrVhMY
            @Override // com.android.server.media.BluetoothRouteProvider.BluetoothRoutesUpdatedListener
            public final void onBluetoothRoutesUpdated(List list) {
                SystemMediaRoute2Provider.this.lambda$new$0$SystemMediaRoute2Provider(list);
            }
        });
        updateSessionInfosIfNeeded();
        IntentFilter intentFilter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        intentFilter.addAction("android.media.STREAM_DEVICES_CHANGED_ACTION");
        this.mContext.registerReceiver(new AudioManagerBroadcastReceiver(this, null), intentFilter);
        if (this.mBtRouteProvider != null) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.media.-$$Lambda$SystemMediaRoute2Provider$AB-PWlKU2NOApQQQov7CqgW5RnQ
                @Override // java.lang.Runnable
                public final void run() {
                    SystemMediaRoute2Provider.this.lambda$new$1$SystemMediaRoute2Provider();
                }
            });
        }
        updateVolume();
    }

    public /* synthetic */ void lambda$new$0$SystemMediaRoute2Provider(List routes) {
        publishProviderState();
        boolean sessionInfoChanged = updateSessionInfosIfNeeded();
        if (sessionInfoChanged) {
            notifySessionInfoUpdated();
        }
    }

    public /* synthetic */ void lambda$new$1$SystemMediaRoute2Provider() {
        this.mBtRouteProvider.start();
        notifyProviderState();
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void setCallback(MediaRoute2Provider.Callback callback) {
        super.setCallback(callback);
        notifyProviderState();
        notifySessionInfoUpdated();
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void requestCreateSession(long requestId, String packageName, String routeId, Bundle sessionHints) {
        if (TextUtils.equals(routeId, DEFAULT_ROUTE_ID)) {
            this.mCallback.onSessionCreated(this, requestId, this.mDefaultSessionInfo);
        } else if (TextUtils.equals(routeId, this.mSelectedRouteId)) {
            this.mCallback.onSessionCreated(this, requestId, this.mSessionInfos.get(0));
        } else {
            synchronized (this.mRequestLock) {
                if (this.mPendingSessionCreationRequest != null) {
                    this.mCallback.onRequestFailed(this, this.mPendingSessionCreationRequest.mRequestId, 0);
                }
                this.mPendingSessionCreationRequest = new SessionCreationRequest(requestId, routeId);
            }
            transferToRoute(requestId, SYSTEM_SESSION_ID, routeId);
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void releaseSession(long requestId, String sessionId) {
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void updateDiscoveryPreference(RouteDiscoveryPreference discoveryPreference) {
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void selectRoute(long requestId, String sessionId, String routeId) {
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void deselectRoute(long requestId, String sessionId, String routeId) {
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void transferToRoute(long requestId, String sessionId, String routeId) {
        if (!TextUtils.equals(routeId, DEFAULT_ROUTE_ID) && this.mBtRouteProvider != null) {
            if (TextUtils.equals(routeId, this.mDeviceRoute.getId())) {
                this.mBtRouteProvider.transferTo(null);
            } else {
                this.mBtRouteProvider.transferTo(routeId);
            }
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void setRouteVolume(long requestId, String routeId, int volume) {
        if (!TextUtils.equals(routeId, this.mSelectedRouteId)) {
            return;
        }
        this.mAudioManager.setStreamVolume(3, volume, 0);
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void setSessionVolume(long requestId, String sessionId, int volume) {
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void prepareReleaseSession(String sessionId) {
    }

    public MediaRoute2Info getDefaultRoute() {
        return this.mDefaultRoute;
    }

    public RoutingSessionInfo getDefaultSessionInfo() {
        return this.mDefaultSessionInfo;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDeviceRoute(AudioRoutesInfo newRoutes) {
        int name = 17040071;
        int type = 2;
        int i = 1;
        if (newRoutes != null) {
            this.mCurAudioRoutesInfo.mainType = newRoutes.mainType;
            if ((newRoutes.mainType & 2) == 0) {
                if ((newRoutes.mainType & 1) != 0) {
                    type = 3;
                    name = 17040074;
                } else if ((newRoutes.mainType & 4) != 0) {
                    type = 13;
                    name = 17040072;
                } else if ((newRoutes.mainType & 8) != 0) {
                    type = 9;
                    name = 17040073;
                } else if ((newRoutes.mainType & 16) != 0) {
                    type = 11;
                    name = 17040075;
                }
            } else {
                type = 4;
                name = 17040074;
            }
        }
        MediaRoute2Info.Builder builder = new MediaRoute2Info.Builder(DEVICE_ROUTE_ID, this.mContext.getResources().getText(name).toString());
        if (this.mAudioManager.isVolumeFixed()) {
            i = 0;
        }
        this.mDeviceRoute = builder.setVolumeHandling(i).setVolume(this.mDeviceVolume).setVolumeMax(this.mAudioManager.getStreamMaxVolume(3)).setType(type).addFeature("android.media.route.feature.LIVE_AUDIO").addFeature("android.media.route.feature.LIVE_VIDEO").addFeature("android.media.route.feature.LOCAL_PLAYBACK").setConnectionState(2).build();
        updateProviderState();
    }

    private void updateProviderState() {
        MediaRoute2ProviderInfo.Builder builder = new MediaRoute2ProviderInfo.Builder();
        builder.addRoute(this.mDeviceRoute);
        BluetoothRouteProvider bluetoothRouteProvider = this.mBtRouteProvider;
        if (bluetoothRouteProvider != null) {
            for (MediaRoute2Info route : bluetoothRouteProvider.getAllBluetoothRoutes()) {
                builder.addRoute(route);
            }
        }
        MediaRoute2ProviderInfo providerInfo = builder.build();
        setProviderState(providerInfo);
        if (DEBUG) {
            Slog.d(TAG, "Updating system provider info : " + providerInfo);
        }
    }

    boolean updateSessionInfosIfNeeded() {
        SessionCreationRequest sessionCreationRequest;
        MediaRoute2Info selectedBtRoute;
        synchronized (this.mLock) {
            RoutingSessionInfo oldSessionInfo = this.mSessionInfos.isEmpty() ? null : this.mSessionInfos.get(0);
            RoutingSessionInfo.Builder builder = new RoutingSessionInfo.Builder(SYSTEM_SESSION_ID, "").setSystemSession(true);
            MediaRoute2Info selectedRoute = this.mDeviceRoute;
            if (this.mBtRouteProvider != null && (selectedBtRoute = this.mBtRouteProvider.getSelectedRoute()) != null) {
                selectedRoute = selectedBtRoute;
                builder.addTransferableRoute(this.mDeviceRoute.getId());
            }
            this.mSelectedRouteId = selectedRoute.getId();
            this.mDefaultRoute = new MediaRoute2Info.Builder(DEFAULT_ROUTE_ID, selectedRoute).setSystemRoute(true).setProviderId(this.mUniqueId).build();
            builder.addSelectedRoute(this.mSelectedRouteId);
            if (this.mBtRouteProvider != null) {
                for (MediaRoute2Info route : this.mBtRouteProvider.getTransferableRoutes()) {
                    builder.addTransferableRoute(route.getId());
                }
            }
            RoutingSessionInfo newSessionInfo = builder.setProviderId(this.mUniqueId).build();
            if (this.mPendingSessionCreationRequest != null) {
                synchronized (this.mRequestLock) {
                    sessionCreationRequest = this.mPendingSessionCreationRequest;
                    this.mPendingSessionCreationRequest = null;
                }
                if (sessionCreationRequest != null) {
                    if (TextUtils.equals(this.mSelectedRouteId, sessionCreationRequest.mRouteId)) {
                        this.mCallback.onSessionCreated(this, sessionCreationRequest.mRequestId, newSessionInfo);
                    } else {
                        this.mCallback.onRequestFailed(this, sessionCreationRequest.mRequestId, 0);
                    }
                }
            }
            if (Objects.equals(oldSessionInfo, newSessionInfo)) {
                return false;
            }
            if (DEBUG) {
                Slog.d(TAG, "Updating system routing session info : " + newSessionInfo);
            }
            this.mSessionInfos.clear();
            this.mSessionInfos.add(newSessionInfo);
            this.mDefaultSessionInfo = new RoutingSessionInfo.Builder(SYSTEM_SESSION_ID, "").setProviderId(this.mUniqueId).setSystemSession(true).addSelectedRoute(DEFAULT_ROUTE_ID).build();
            return true;
        }
    }

    void publishProviderState() {
        updateProviderState();
        notifyProviderState();
    }

    void notifySessionInfoUpdated() {
        RoutingSessionInfo sessionInfo;
        if (this.mCallback == null) {
            return;
        }
        synchronized (this.mLock) {
            sessionInfo = this.mSessionInfos.get(0);
        }
        this.mCallback.onSessionUpdated(this, sessionInfo);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class SessionCreationRequest {
        final long mRequestId;
        final String mRouteId;

        SessionCreationRequest(long requestId, String routeId) {
            this.mRequestId = requestId;
            this.mRouteId = routeId;
        }
    }

    void updateVolume() {
        int devices = this.mAudioManager.getDevicesForStream(3);
        int volume = this.mAudioManager.getStreamVolume(3);
        if (this.mDefaultRoute.getVolume() != volume) {
            this.mDefaultRoute = new MediaRoute2Info.Builder(this.mDefaultRoute).setVolume(volume).build();
        }
        BluetoothRouteProvider bluetoothRouteProvider = this.mBtRouteProvider;
        if (bluetoothRouteProvider != null && bluetoothRouteProvider.updateVolumeForDevices(devices, volume)) {
            return;
        }
        if (this.mDeviceVolume != volume) {
            this.mDeviceVolume = volume;
            this.mDeviceRoute = new MediaRoute2Info.Builder(this.mDeviceRoute).setVolume(volume).build();
        }
        publishProviderState();
    }

    /* loaded from: classes.dex */
    private class AudioManagerBroadcastReceiver extends BroadcastReceiver {
        private AudioManagerBroadcastReceiver() {
        }

        /* synthetic */ AudioManagerBroadcastReceiver(SystemMediaRoute2Provider x0, AnonymousClass1 x1) {
            this();
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION") && !intent.getAction().equals("android.media.STREAM_DEVICES_CHANGED_ACTION")) {
                return;
            }
            int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
            if (streamType != 3) {
                return;
            }
            SystemMediaRoute2Provider.this.updateVolume();
        }
    }
}