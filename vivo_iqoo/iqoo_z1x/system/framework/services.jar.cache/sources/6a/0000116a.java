package com.android.server.location.gnss;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Telephony;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseCallState;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.location.GpsNetInitiatedHandler;
import com.vivo.common.utils.VLog;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class GnssNetworkConnectivityHandler {
    private static final int AGNSS_NET_CAPABILITY_NOT_METERED = 1;
    private static final int AGNSS_NET_CAPABILITY_NOT_ROAMING = 2;
    private static final int AGPS_DATA_CONNECTION_CLOSED = 0;
    private static final int AGPS_DATA_CONNECTION_OPEN = 2;
    private static final int AGPS_DATA_CONNECTION_OPENING = 1;
    public static final int AGPS_TYPE_C2K = 2;
    private static final int AGPS_TYPE_EIMS = 3;
    private static final int AGPS_TYPE_IMS = 4;
    public static final int AGPS_TYPE_SUPL = 1;
    private static final int APN_INVALID = 0;
    private static final int APN_IPV4 = 1;
    private static final int APN_IPV4V6 = 3;
    private static final int APN_IPV6 = 2;
    private static final int GPS_AGPS_DATA_CONNECTED = 3;
    private static final int GPS_AGPS_DATA_CONN_DONE = 4;
    private static final int GPS_AGPS_DATA_CONN_FAILED = 5;
    private static final int GPS_RELEASE_AGPS_DATA_CONN = 2;
    private static final int GPS_REQUEST_AGPS_DATA_CONN = 1;
    private static final int HASH_MAP_INITIAL_CAPACITY_TO_TRACK_CONNECTED_NETWORKS = 5;
    private static final int SUPL_NETWORK_REQUEST_TIMEOUT_MILLIS = 10000;
    static final String TAG = "GnssNetworkConnectivityHandler";
    private static final String WAKELOCK_KEY = "GnssNetworkConnectivityHandler";
    private static final long WAKELOCK_TIMEOUT_MILLIS = 60000;
    private InetAddress mAGpsDataConnectionIpAddr;
    private int mAGpsDataConnectionState;
    private int mAGpsType;
    private final ConnectivityManager mConnMgr;
    private final Context mContext;
    private final GnssNetworkListener mGnssNetworkListener;
    private final Handler mHandler;
    private ConnectivityManager.NetworkCallback mNetworkConnectivityCallback;
    private final GpsNetInitiatedHandler mNiHandler;
    private HashMap<Integer, SubIdPhoneStateListener> mPhoneStateListeners;
    private ConnectivityManager.NetworkCallback mSuplConnectivityCallback;
    private final PowerManager.WakeLock mWakeLock;
    private static final boolean DEBUG = Log.isLoggable("GnssNetworkConnectivityHandler", 3);
    private static final boolean VERBOSE = Log.isLoggable("GnssNetworkConnectivityHandler", 2);
    private HashMap<Network, NetworkAttributes> mAvailableNetworkAttributes = new HashMap<>(5);
    private int mActiveSubId = -1;
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener = new SubscriptionManager.OnSubscriptionsChangedListener() { // from class: com.android.server.location.gnss.GnssNetworkConnectivityHandler.1
        @Override // android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
        public void onSubscriptionsChanged() {
            TelephonyManager subIdTelManager;
            if (GnssNetworkConnectivityHandler.this.mPhoneStateListeners == null) {
                GnssNetworkConnectivityHandler.this.mPhoneStateListeners = new HashMap(2, 1.0f);
            }
            SubscriptionManager subManager = (SubscriptionManager) GnssNetworkConnectivityHandler.this.mContext.getSystemService(SubscriptionManager.class);
            TelephonyManager telManager = (TelephonyManager) GnssNetworkConnectivityHandler.this.mContext.getSystemService(TelephonyManager.class);
            if (subManager != null && telManager != null) {
                List<SubscriptionInfo> subscriptionInfoList = subManager.getActiveSubscriptionInfoList();
                HashSet<Integer> activeSubIds = new HashSet<>();
                if (subscriptionInfoList != null) {
                    if (GnssNetworkConnectivityHandler.DEBUG) {
                        Log.d("GnssNetworkConnectivityHandler", "Active Sub List size: " + subscriptionInfoList.size());
                    }
                    for (SubscriptionInfo subInfo : subscriptionInfoList) {
                        activeSubIds.add(Integer.valueOf(subInfo.getSubscriptionId()));
                        if (!GnssNetworkConnectivityHandler.this.mPhoneStateListeners.containsKey(Integer.valueOf(subInfo.getSubscriptionId())) && (subIdTelManager = telManager.createForSubscriptionId(subInfo.getSubscriptionId())) != null) {
                            if (GnssNetworkConnectivityHandler.DEBUG) {
                                Log.d("GnssNetworkConnectivityHandler", "Listener sub" + subInfo.getSubscriptionId());
                            }
                            SubIdPhoneStateListener subIdPhoneStateListener = new SubIdPhoneStateListener(Integer.valueOf(subInfo.getSubscriptionId()));
                            GnssNetworkConnectivityHandler.this.mPhoneStateListeners.put(Integer.valueOf(subInfo.getSubscriptionId()), subIdPhoneStateListener);
                            subIdTelManager.listen(subIdPhoneStateListener, 2048);
                        }
                    }
                }
                Iterator<Map.Entry<Integer, SubIdPhoneStateListener>> iterator = GnssNetworkConnectivityHandler.this.mPhoneStateListeners.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, SubIdPhoneStateListener> element = iterator.next();
                    if (!activeSubIds.contains(element.getKey())) {
                        TelephonyManager subIdTelManager2 = telManager.createForSubscriptionId(element.getKey().intValue());
                        if (subIdTelManager2 != null) {
                            if (GnssNetworkConnectivityHandler.DEBUG) {
                                Log.d("GnssNetworkConnectivityHandler", "unregister listener sub " + element.getKey());
                            }
                            subIdTelManager2.listen(element.getValue(), 0);
                            iterator.remove();
                        } else {
                            Log.e("GnssNetworkConnectivityHandler", "Telephony Manager for Sub " + element.getKey() + " null");
                        }
                    }
                }
                if (!activeSubIds.contains(Integer.valueOf(GnssNetworkConnectivityHandler.this.mActiveSubId))) {
                    GnssNetworkConnectivityHandler.this.mActiveSubId = -1;
                }
            }
        }
    };

    /* loaded from: classes.dex */
    interface GnssNetworkListener {
        void onNetworkAvailable();
    }

    private native void native_agps_data_conn_closed();

    private native void native_agps_data_conn_failed();

    private native void native_agps_data_conn_open(long j, String str, int i);

    private static native void native_agps_set_supl_host_ip(String str);

    private static native boolean native_is_agps_ril_supported();

    private native void native_update_network_state(boolean z, int i, boolean z2, boolean z3, String str, long j, short s);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class NetworkAttributes {
        private String mApn;
        private NetworkCapabilities mCapabilities;
        private int mType;

        private NetworkAttributes() {
            this.mType = -1;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static boolean hasCapabilitiesChanged(NetworkCapabilities curCapabilities, NetworkCapabilities newCapabilities) {
            return curCapabilities == null || newCapabilities == null || hasCapabilityChanged(curCapabilities, newCapabilities, 18) || hasCapabilityChanged(curCapabilities, newCapabilities, 11);
        }

        private static boolean hasCapabilityChanged(NetworkCapabilities curCapabilities, NetworkCapabilities newCapabilities, int capability) {
            return curCapabilities.hasCapability(capability) != newCapabilities.hasCapability(capability);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static short getCapabilityFlags(NetworkCapabilities capabilities) {
            short capabilityFlags = 0;
            if (capabilities.hasCapability(18)) {
                capabilityFlags = (short) (0 | 2);
            }
            if (capabilities.hasCapability(11)) {
                return (short) (capabilityFlags | 1);
            }
            return capabilityFlags;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GnssNetworkConnectivityHandler(Context context, GnssNetworkListener gnssNetworkListener, Looper looper, GpsNetInitiatedHandler niHandler) {
        this.mContext = context;
        this.mGnssNetworkListener = gnssNetworkListener;
        SubscriptionManager subManager = (SubscriptionManager) context.getSystemService(SubscriptionManager.class);
        if (subManager != null) {
            subManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        }
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, "GnssNetworkConnectivityHandler");
        this.mHandler = new Handler(looper);
        this.mNiHandler = niHandler;
        this.mConnMgr = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mSuplConnectivityCallback = createSuplConnectivityCallback();
    }

    /* loaded from: classes.dex */
    private final class SubIdPhoneStateListener extends PhoneStateListener {
        private Integer mSubId;

        SubIdPhoneStateListener(Integer subId) {
            this.mSubId = subId;
        }

        public void onPreciseCallStateChanged(PreciseCallState state) {
            if (1 == state.getForegroundCallState()) {
                GnssNetworkConnectivityHandler.this.mActiveSubId = this.mSubId.intValue();
                if (GnssNetworkConnectivityHandler.DEBUG) {
                    Log.d("GnssNetworkConnectivityHandler", "mActiveSubId: " + GnssNetworkConnectivityHandler.this.mActiveSubId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerNetworkCallbacks() {
        NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
        networkRequestBuilder.addCapability(12);
        networkRequestBuilder.addCapability(16);
        networkRequestBuilder.removeCapability(15);
        NetworkRequest networkRequest = networkRequestBuilder.build();
        ConnectivityManager.NetworkCallback createNetworkConnectivityCallback = createNetworkConnectivityCallback();
        this.mNetworkConnectivityCallback = createNetworkConnectivityCallback;
        this.mConnMgr.registerNetworkCallback(networkRequest, createNetworkConnectivityCallback, this.mHandler);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDataNetworkConnected() {
        NetworkInfo activeNetworkInfo = this.mConnMgr.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onReportAGpsStatus(final int agpsType, int agpsStatus, final byte[] suplIpAddr) {
        if (DEBUG) {
            Log.d("GnssNetworkConnectivityHandler", "AGPS_DATA_CONNECTION: " + agpsDataConnStatusAsString(agpsStatus));
        }
        if (agpsStatus == 1) {
            runOnHandler(new Runnable() { // from class: com.android.server.location.gnss.-$$Lambda$GnssNetworkConnectivityHandler$bnc6RM72T8jpSxM08ugCgEMySwo
                @Override // java.lang.Runnable
                public final void run() {
                    GnssNetworkConnectivityHandler.this.lambda$onReportAGpsStatus$0$GnssNetworkConnectivityHandler(agpsType, suplIpAddr);
                }
            });
        } else if (agpsStatus == 2) {
            runOnHandler(new Runnable() { // from class: com.android.server.location.gnss.-$$Lambda$GnssNetworkConnectivityHandler$M5xHE3b_460ydxe6w6OcvDX9Kx8
                @Override // java.lang.Runnable
                public final void run() {
                    GnssNetworkConnectivityHandler.this.lambda$onReportAGpsStatus$1$GnssNetworkConnectivityHandler();
                }
            });
        } else if (agpsStatus != 3 && agpsStatus != 4 && agpsStatus != 5) {
            Log.w("GnssNetworkConnectivityHandler", "Received unknown AGPS status: " + agpsStatus);
        }
    }

    public /* synthetic */ void lambda$onReportAGpsStatus$1$GnssNetworkConnectivityHandler() {
        handleReleaseSuplConnection(2);
    }

    private ConnectivityManager.NetworkCallback createNetworkConnectivityCallback() {
        return new ConnectivityManager.NetworkCallback() { // from class: com.android.server.location.gnss.GnssNetworkConnectivityHandler.2
            private HashMap<Network, NetworkCapabilities> mAvailableNetworkCapabilities = new HashMap<>(5);

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                if (!NetworkAttributes.hasCapabilitiesChanged(this.mAvailableNetworkCapabilities.get(network), capabilities)) {
                    if (GnssNetworkConnectivityHandler.VERBOSE) {
                        Log.v("GnssNetworkConnectivityHandler", "Relevant network capabilities unchanged. Capabilities: " + capabilities);
                        return;
                    }
                    return;
                }
                this.mAvailableNetworkCapabilities.put(network, capabilities);
                if (GnssNetworkConnectivityHandler.DEBUG) {
                    Log.d("GnssNetworkConnectivityHandler", "Network connected/capabilities updated. Available networks count: " + this.mAvailableNetworkCapabilities.size());
                }
                GnssNetworkConnectivityHandler.this.mGnssNetworkListener.onNetworkAvailable();
                GnssNetworkConnectivityHandler.this.handleUpdateNetworkState(network, true, capabilities, false, null);
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                if (this.mAvailableNetworkCapabilities.remove(network) == null) {
                    Log.w("GnssNetworkConnectivityHandler", "Incorrectly received network callback onLost() before onCapabilitiesChanged() for network: " + network);
                    return;
                }
                Log.i("GnssNetworkConnectivityHandler", "Network connection lost. Available networks count: " + this.mAvailableNetworkCapabilities.size());
                if (this.mAvailableNetworkCapabilities.size() <= 0) {
                    GnssNetworkConnectivityHandler.this.handleUpdateNetworkState(network, false, null, false, null);
                    return;
                }
                Iterator<Network> it = this.mAvailableNetworkCapabilities.keySet().iterator();
                if (it.hasNext()) {
                    Network network_available = it.next();
                    GnssNetworkConnectivityHandler.this.handleUpdateNetworkState(network_available, true, this.mAvailableNetworkCapabilities.get(network_available), true, network);
                }
            }
        };
    }

    private ConnectivityManager.NetworkCallback createSuplConnectivityCallback() {
        return new ConnectivityManager.NetworkCallback() { // from class: com.android.server.location.gnss.GnssNetworkConnectivityHandler.3
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onAvailable(Network network) {
                if (GnssNetworkConnectivityHandler.DEBUG) {
                    Log.d("GnssNetworkConnectivityHandler", "SUPL network connection available.");
                }
                GnssNetworkConnectivityHandler.this.handleSuplConnectionAvailable(network);
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                Log.i("GnssNetworkConnectivityHandler", "SUPL network connection lost.");
                GnssNetworkConnectivityHandler.this.handleReleaseSuplConnection(2);
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onUnavailable() {
                Log.i("GnssNetworkConnectivityHandler", "SUPL network connection request timed out.");
                GnssNetworkConnectivityHandler.this.handleReleaseSuplConnection(5);
            }
        };
    }

    public void getSuplInetAddrByFqdn(String fqdn) {
        Log.d("GnssNetworkConnectivityHandler", "getSuplInetAddrByFqdn is not supported on Android R yet.");
    }

    private void runOnHandler(Runnable event) {
        this.mWakeLock.acquire(60000L);
        if (!this.mHandler.post(runEventAndReleaseWakeLock(event))) {
            this.mWakeLock.release();
        }
    }

    private Runnable runEventAndReleaseWakeLock(final Runnable event) {
        return new Runnable() { // from class: com.android.server.location.gnss.-$$Lambda$GnssNetworkConnectivityHandler$ezDFQHbzZ9WnxJSpYWB6YP4YDQM
            @Override // java.lang.Runnable
            public final void run() {
                GnssNetworkConnectivityHandler.this.lambda$runEventAndReleaseWakeLock$2$GnssNetworkConnectivityHandler(event);
            }
        };
    }

    public /* synthetic */ void lambda$runEventAndReleaseWakeLock$2$GnssNetworkConnectivityHandler(Runnable event) {
        try {
            event.run();
        } finally {
            this.mWakeLock.release();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUpdateNetworkState(Network network, boolean isConnected, NetworkCapabilities capabilities, boolean isOnLost, Network lostNetwork) {
        boolean networkAvailable;
        NetworkAttributes networkAttributes;
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
        if (telephonyManager == null) {
            networkAvailable = false;
        } else {
            boolean networkAvailable2 = isConnected && telephonyManager.getDataEnabled();
            networkAvailable = networkAvailable2;
        }
        VLog.d("GnssNetworkConnectivityHandler", "network = " + network + ", lostNetwork = " + lostNetwork);
        if (isOnLost) {
            NetworkAttributes networkAttributes2 = updateTrackedNetworksState(false, lostNetwork, null);
            networkAttributes = networkAttributes2;
        } else {
            NetworkAttributes networkAttributes3 = updateTrackedNetworksState(isConnected, network, capabilities);
            networkAttributes = networkAttributes3;
        }
        String apn = networkAttributes.mApn;
        int type = networkAttributes.mType;
        NetworkCapabilities capabilities2 = networkAttributes.mCapabilities;
        Log.i("GnssNetworkConnectivityHandler", String.format("updateNetworkState, state=%s, connected=%s, network=%s, capabilities=%s, apn: %s, availableNetworkCount: %d", agpsDataConnStateAsString(), Boolean.valueOf(isConnected), network, capabilities2, apn, Integer.valueOf(this.mAvailableNetworkAttributes.size())));
        if (native_is_agps_ril_supported()) {
            native_update_network_state(isConnected, type, !capabilities2.hasTransport(18), networkAvailable, apn != null ? apn : "", network.getNetworkHandle(), NetworkAttributes.getCapabilityFlags(capabilities2));
        } else if (DEBUG) {
            Log.d("GnssNetworkConnectivityHandler", "Skipped network state update because GPS HAL AGPS-RIL is not  supported");
        }
    }

    private NetworkAttributes updateTrackedNetworksState(boolean isConnected, Network network, NetworkCapabilities capabilities) {
        if (!isConnected) {
            return this.mAvailableNetworkAttributes.remove(network);
        }
        NetworkAttributes networkAttributes = this.mAvailableNetworkAttributes.get(network);
        if (networkAttributes != null) {
            networkAttributes.mCapabilities = capabilities;
            return networkAttributes;
        }
        NetworkAttributes networkAttributes2 = new NetworkAttributes();
        networkAttributes2.mCapabilities = capabilities;
        NetworkInfo info = this.mConnMgr.getNetworkInfo(network);
        if (info != null) {
            networkAttributes2.mApn = info.getExtraInfo();
            networkAttributes2.mType = info.getType();
        }
        this.mAvailableNetworkAttributes.put(network, networkAttributes2);
        return networkAttributes2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSuplConnectionAvailable(Network network) {
        NetworkInfo info = this.mConnMgr.getNetworkInfo(network);
        String apn = null;
        if (info != null) {
            apn = info.getExtraInfo();
        }
        if (DEBUG) {
            String message = String.format("handleSuplConnectionAvailable: state=%s, suplNetwork=%s, info=%s", agpsDataConnStateAsString(), network, info);
            Log.d("GnssNetworkConnectivityHandler", message);
        }
        if (this.mAGpsDataConnectionState == 1) {
            if (apn == null) {
                apn = "dummy-apn";
            }
            if (this.mAGpsDataConnectionIpAddr != null) {
                setRouting();
            }
            int apnIpType = getApnIpType(apn);
            if (DEBUG) {
                String message2 = String.format("native_agps_data_conn_open: mAgpsApn=%s, mApnIpType=%s", apn, Integer.valueOf(apnIpType));
                Log.d("GnssNetworkConnectivityHandler", message2);
            }
            native_agps_data_conn_open(network.getNetworkHandle(), apn, apnIpType);
            this.mAGpsDataConnectionState = 2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleRequestSuplConnection */
    public void lambda$onReportAGpsStatus$0$GnssNetworkConnectivityHandler(int agpsType, byte[] suplIpAddr) {
        this.mAGpsDataConnectionIpAddr = null;
        this.mAGpsType = agpsType;
        if (suplIpAddr != null) {
            if (VERBOSE) {
                Log.v("GnssNetworkConnectivityHandler", "Received SUPL IP addr[]: " + Arrays.toString(suplIpAddr));
            }
            try {
                this.mAGpsDataConnectionIpAddr = InetAddress.getByAddress(suplIpAddr);
                if (DEBUG) {
                    Log.d("GnssNetworkConnectivityHandler", "IP address converted to: " + this.mAGpsDataConnectionIpAddr);
                }
            } catch (UnknownHostException e) {
                Log.e("GnssNetworkConnectivityHandler", "Bad IP Address: " + suplIpAddr, e);
            }
        }
        if (DEBUG) {
            String message = String.format("requestSuplConnection, state=%s, agpsType=%s, address=%s", agpsDataConnStateAsString(), agpsTypeAsString(agpsType), this.mAGpsDataConnectionIpAddr);
            Log.d("GnssNetworkConnectivityHandler", message);
        }
        if (this.mAGpsDataConnectionState != 0) {
            return;
        }
        this.mAGpsDataConnectionState = 1;
        NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
        networkRequestBuilder.addCapability(getNetworkCapability(this.mAGpsType));
        networkRequestBuilder.addTransportType(0);
        if (this.mNiHandler.getInEmergency() && this.mActiveSubId >= 0) {
            if (DEBUG) {
                Log.d("GnssNetworkConnectivityHandler", "Adding Network Specifier: " + Integer.toString(this.mActiveSubId));
            }
            networkRequestBuilder.setNetworkSpecifier(Integer.toString(this.mActiveSubId));
        }
        NetworkRequest networkRequest = networkRequestBuilder.build();
        this.mConnMgr.requestNetwork(networkRequest, this.mSuplConnectivityCallback, this.mHandler, 10000);
    }

    private int getNetworkCapability(int agpsType) {
        if (agpsType == 1 || agpsType == 2) {
            return 1;
        }
        if (agpsType != 3) {
            if (agpsType == 4) {
                return 4;
            }
            throw new IllegalArgumentException("agpsType: " + agpsType);
        }
        return 10;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleReleaseSuplConnection(int agpsDataConnStatus) {
        if (DEBUG) {
            String message = String.format("releaseSuplConnection, state=%s, status=%s", agpsDataConnStateAsString(), agpsDataConnStatusAsString(agpsDataConnStatus));
            Log.d("GnssNetworkConnectivityHandler", message);
        }
        if (this.mAGpsDataConnectionState == 0) {
            return;
        }
        this.mAGpsDataConnectionState = 0;
        this.mConnMgr.unregisterNetworkCallback(this.mSuplConnectivityCallback);
        if (agpsDataConnStatus == 2) {
            native_agps_data_conn_closed();
        } else if (agpsDataConnStatus == 5) {
            native_agps_data_conn_failed();
        } else {
            Log.e("GnssNetworkConnectivityHandler", "Invalid status to release SUPL connection: " + agpsDataConnStatus);
        }
    }

    private void setRouting() {
        boolean result = this.mConnMgr.requestRouteToHostAddress(3, this.mAGpsDataConnectionIpAddr);
        if (!result) {
            Log.e("GnssNetworkConnectivityHandler", "Error requesting route to host: " + this.mAGpsDataConnectionIpAddr);
        } else if (DEBUG) {
            Log.d("GnssNetworkConnectivityHandler", "Successfully requested route to host: " + this.mAGpsDataConnectionIpAddr);
        }
    }

    private void ensureInHandlerThread() {
        if (this.mHandler != null && Looper.myLooper() == this.mHandler.getLooper()) {
            return;
        }
        throw new IllegalStateException("This method must run on the Handler thread.");
    }

    private String agpsDataConnStateAsString() {
        int i = this.mAGpsDataConnectionState;
        if (i != 0) {
            if (i != 1) {
                if (i == 2) {
                    return "OPEN";
                }
                return "<Unknown>(" + this.mAGpsDataConnectionState + ")";
            }
            return "OPENING";
        }
        return "CLOSED";
    }

    private String agpsDataConnStatusAsString(int agpsDataConnStatus) {
        if (agpsDataConnStatus != 1) {
            if (agpsDataConnStatus != 2) {
                if (agpsDataConnStatus != 3) {
                    if (agpsDataConnStatus != 4) {
                        if (agpsDataConnStatus == 5) {
                            return "FAILED";
                        }
                        return "<Unknown>(" + agpsDataConnStatus + ")";
                    }
                    return "DONE";
                }
                return "CONNECTED";
            }
            return "RELEASE";
        }
        return "REQUEST";
    }

    private String agpsTypeAsString(int agpsType) {
        if (agpsType != 1) {
            if (agpsType != 2) {
                if (agpsType != 3) {
                    if (agpsType == 4) {
                        return "IMS";
                    }
                    return "<Unknown>(" + agpsType + ")";
                }
                return "EIMS";
            }
            return "C2K";
        }
        return "SUPL";
    }

    private int getApnIpType(String apn) {
        String projection;
        Cursor cursor;
        int i;
        TelephonyManager subIdTelManager;
        ensureInHandlerThread();
        if (apn == null) {
            return 0;
        }
        TelephonyManager phone = (TelephonyManager) this.mContext.getSystemService("phone");
        if (this.mNiHandler.getInEmergency() && (i = this.mActiveSubId) >= 0 && (subIdTelManager = phone.createForSubscriptionId(i)) != null) {
            phone = subIdTelManager;
        }
        ServiceState serviceState = phone.getServiceState();
        if (serviceState != null && serviceState.getDataRoamingFromRegistration()) {
            projection = "roaming_protocol";
        } else {
            projection = "protocol";
        }
        String selection = (phone.getNetworkType() == 0 && 3 == this.mAGpsType) ? String.format("type like '%%emergency%%' and apn = '%s' and carrier_enabled = 1", apn) : String.format("current = 1 and apn = '%s' and carrier_enabled = 1", apn);
        try {
            cursor = this.mContext.getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{projection}, selection, null, "name ASC");
        } catch (Exception e) {
            Log.e("GnssNetworkConnectivityHandler", "Error encountered on APN query for: " + apn, e);
        }
        if (cursor != null && cursor.moveToFirst()) {
            int translateToApnIpType = translateToApnIpType(cursor.getString(0), apn);
            if (cursor != null) {
                cursor.close();
            }
            return translateToApnIpType;
        }
        Log.e("GnssNetworkConnectivityHandler", "No entry found in query for APN: " + apn);
        if (cursor != null) {
            cursor.close();
        }
        return 3;
    }

    private int translateToApnIpType(String ipProtocol, String apn) {
        if ("IP".equals(ipProtocol)) {
            return 1;
        }
        if ("IPV6".equals(ipProtocol)) {
            return 2;
        }
        if ("IPV4V6".equals(ipProtocol)) {
            return 3;
        }
        String message = String.format("Unknown IP Protocol: %s, for APN: %s", ipProtocol, apn);
        Log.e("GnssNetworkConnectivityHandler", message);
        return 3;
    }
}