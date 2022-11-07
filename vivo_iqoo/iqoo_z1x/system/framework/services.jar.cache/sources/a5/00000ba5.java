package com.android.server.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetdEventCallback;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.metrics.ConnectStats;
import android.net.metrics.DnsEvent;
import android.net.metrics.INetdEventListener;
import android.net.metrics.NetworkMetrics;
import android.net.metrics.WakeupEvent;
import android.net.metrics.WakeupStats;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.BitUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.RingBuffer;
import com.android.internal.util.TokenBucket;
import com.android.server.connectivity.metrics.nano.IpConnectivityLogClass;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/* loaded from: classes.dex */
public class NetdEventListenerService extends INetdEventListener.Stub {
    private static final int CONNECT_LATENCY_BURST_LIMIT = 5000;
    private static final int CONNECT_LATENCY_FILL_RATE = 15000;
    private static final boolean DBG = false;
    private static final int METRICS_SNAPSHOT_BUFFER_SIZE = 48;
    private static final long METRICS_SNAPSHOT_SPAN_MS = 300000;
    public static final String SERVICE_NAME = "netd_listener";
    static final int WAKEUP_EVENT_BUFFER_LENGTH = 1024;
    static final String WAKEUP_EVENT_IFACE_PREFIX = "iface:";
    private final ConnectivityManager mCm;
    private final TokenBucket mConnectTb;
    private long mLastSnapshot;
    private INetdEventCallback[] mNetdEventCallbackList;
    private final SparseArray<NetworkMetrics> mNetworkMetrics;
    private final RingBuffer<NetworkMetricsSnapshot> mNetworkMetricsSnapshots;
    private final RingBuffer<WakeupEvent> mWakeupEvents;
    private final ArrayMap<String, WakeupStats> mWakeupStats;
    private static final String TAG = NetdEventListenerService.class.getSimpleName();
    private static final int[] ALLOWED_CALLBACK_TYPES = {0, 1, 2, 3, 4};

    public synchronized boolean addNetdEventCallback(int callerType, INetdEventCallback callback) {
        if (!isValidCallerType(callerType)) {
            String str = TAG;
            Log.e(str, "Invalid caller type: " + callerType);
            return false;
        }
        this.mNetdEventCallbackList[callerType] = callback;
        return true;
    }

    public synchronized boolean removeNetdEventCallback(int callerType) {
        if (!isValidCallerType(callerType)) {
            String str = TAG;
            Log.e(str, "Invalid caller type: " + callerType);
            return false;
        }
        this.mNetdEventCallbackList[callerType] = null;
        return true;
    }

    private static boolean isValidCallerType(int callerType) {
        int i = 0;
        while (true) {
            int[] iArr = ALLOWED_CALLBACK_TYPES;
            if (i < iArr.length) {
                if (callerType != iArr[i]) {
                    i++;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    public NetdEventListenerService(Context context) {
        this((ConnectivityManager) context.getSystemService(ConnectivityManager.class));
    }

    public NetdEventListenerService(ConnectivityManager cm) {
        this.mNetworkMetrics = new SparseArray<>();
        this.mNetworkMetricsSnapshots = new RingBuffer<>(NetworkMetricsSnapshot.class, 48);
        this.mLastSnapshot = 0L;
        this.mWakeupStats = new ArrayMap<>();
        this.mWakeupEvents = new RingBuffer<>(WakeupEvent.class, 1024);
        this.mConnectTb = new TokenBucket(15000, 5000);
        this.mNetdEventCallbackList = new INetdEventCallback[ALLOWED_CALLBACK_TYPES.length];
        this.mCm = cm;
    }

    private static long projectSnapshotTime(long timeMs) {
        return (timeMs / 300000) * 300000;
    }

    private NetworkMetrics getMetricsForNetwork(long timeMs, int netId) {
        collectPendingMetricsSnapshot(timeMs);
        NetworkMetrics metrics = this.mNetworkMetrics.get(netId);
        if (metrics == null) {
            NetworkMetrics metrics2 = new NetworkMetrics(netId, getTransports(netId), this.mConnectTb);
            this.mNetworkMetrics.put(netId, metrics2);
            return metrics2;
        }
        return metrics;
    }

    private NetworkMetricsSnapshot[] getNetworkMetricsSnapshots() {
        collectPendingMetricsSnapshot(System.currentTimeMillis());
        return (NetworkMetricsSnapshot[]) this.mNetworkMetricsSnapshots.toArray();
    }

    private void collectPendingMetricsSnapshot(long timeMs) {
        if (Math.abs(timeMs - this.mLastSnapshot) <= 300000) {
            return;
        }
        long projectSnapshotTime = projectSnapshotTime(timeMs);
        this.mLastSnapshot = projectSnapshotTime;
        NetworkMetricsSnapshot snapshot = NetworkMetricsSnapshot.collect(projectSnapshotTime, this.mNetworkMetrics);
        if (snapshot.stats.isEmpty()) {
            return;
        }
        this.mNetworkMetricsSnapshots.append(snapshot);
    }

    @Override // android.net.metrics.INetdEventListener
    public synchronized void onDnsEvent(int netId, int eventType, int returnCode, int latencyMs, String hostname, String[] ipAddresses, int ipAddressesCount, int uid) throws RemoteException {
        int i;
        INetdEventCallback[] iNetdEventCallbackArr;
        int i2;
        long timestamp = System.currentTimeMillis();
        getMetricsForNetwork(timestamp, netId).addDnsResult(eventType, returnCode, latencyMs);
        INetdEventCallback[] iNetdEventCallbackArr2 = this.mNetdEventCallbackList;
        int length = iNetdEventCallbackArr2.length;
        int i3 = 0;
        while (i3 < length) {
            INetdEventCallback callback = iNetdEventCallbackArr2[i3];
            if (callback == null) {
                i = i3;
                iNetdEventCallbackArr = iNetdEventCallbackArr2;
                i2 = length;
            } else {
                i = i3;
                iNetdEventCallbackArr = iNetdEventCallbackArr2;
                i2 = length;
                callback.onDnsEvent(netId, eventType, returnCode, hostname, ipAddresses, ipAddressesCount, timestamp, uid);
            }
            i3 = i + 1;
            iNetdEventCallbackArr2 = iNetdEventCallbackArr;
            length = i2;
        }
    }

    @Override // android.net.metrics.INetdEventListener
    public synchronized void onNat64PrefixEvent(int netId, boolean added, String prefixString, int prefixLength) throws RemoteException {
        INetdEventCallback[] iNetdEventCallbackArr;
        for (INetdEventCallback callback : this.mNetdEventCallbackList) {
            if (callback != null) {
                callback.onNat64PrefixEvent(netId, added, prefixString, prefixLength);
            }
        }
    }

    @Override // android.net.metrics.INetdEventListener
    public synchronized void onPrivateDnsValidationEvent(int netId, String ipAddress, String hostname, boolean validated) throws RemoteException {
        INetdEventCallback[] iNetdEventCallbackArr;
        for (INetdEventCallback callback : this.mNetdEventCallbackList) {
            if (callback != null) {
                callback.onPrivateDnsValidationEvent(netId, ipAddress, hostname, validated);
            }
        }
    }

    @Override // android.net.metrics.INetdEventListener
    public synchronized void onConnectEvent(int netId, int error, int latencyMs, String ipAddr, int port, int uid) throws RemoteException {
        INetdEventCallback[] iNetdEventCallbackArr;
        long timestamp = System.currentTimeMillis();
        getMetricsForNetwork(timestamp, netId).addConnectResult(error, latencyMs, ipAddr);
        for (INetdEventCallback callback : this.mNetdEventCallbackList) {
            if (callback != null) {
                callback.onConnectEvent(ipAddr, port, timestamp, uid);
            }
        }
    }

    @Override // android.net.metrics.INetdEventListener
    public synchronized void onDnsStatsInfo(int successes, int errors, int timeouts, int internal_errors, int rtt_avg, int success_threshold, int min_samples) throws RemoteException {
        INetdEventCallback[] iNetdEventCallbackArr;
        System.currentTimeMillis();
        for (INetdEventCallback callback : this.mNetdEventCallbackList) {
            if (callback != null) {
                callback.onDnsStatsInfo(successes, errors, timeouts, internal_errors, rtt_avg, success_threshold, min_samples);
            }
        }
    }

    @Override // android.net.metrics.INetdEventListener
    public synchronized void onWakeupEvent(String prefix, int uid, int ethertype, int ipNextHeader, byte[] dstHw, String srcIp, String dstIp, int srcPort, int dstPort, long timestampNs) {
        long timestampMs;
        String iface = prefix.replaceFirst(WAKEUP_EVENT_IFACE_PREFIX, "");
        if (timestampNs > 0) {
            timestampMs = timestampNs / 1000000;
        } else {
            long timestampMs2 = System.currentTimeMillis();
            timestampMs = timestampMs2;
        }
        WakeupEvent event = new WakeupEvent();
        event.iface = iface;
        event.timestampMs = timestampMs;
        event.uid = uid;
        event.ethertype = ethertype;
        event.dstHwAddr = MacAddress.fromBytes(dstHw);
        event.srcIp = srcIp;
        event.dstIp = dstIp;
        event.ipNextHeader = ipNextHeader;
        event.srcPort = srcPort;
        event.dstPort = dstPort;
        addWakeupEvent(event);
        String dstMac = event.dstHwAddr.toString();
        FrameworkStatsLog.write(44, uid, iface, ethertype, dstMac, srcIp, dstIp, ipNextHeader, srcPort, dstPort);
    }

    @Override // android.net.metrics.INetdEventListener
    public synchronized void onTcpSocketStatsEvent(int[] networkIds, int[] sentPackets, int[] lostPackets, int[] rttsUs, int[] sentAckDiffsMs) {
        if (networkIds.length == sentPackets.length && networkIds.length == lostPackets.length && networkIds.length == rttsUs.length && networkIds.length == sentAckDiffsMs.length) {
            long timestamp = System.currentTimeMillis();
            for (int i = 0; i < networkIds.length; i++) {
                int netId = networkIds[i];
                int sent = sentPackets[i];
                int lost = lostPackets[i];
                int rttUs = rttsUs[i];
                int sentAckDiffMs = sentAckDiffsMs[i];
                getMetricsForNetwork(timestamp, netId).addTcpStatsResult(sent, lost, rttUs, sentAckDiffMs);
            }
            return;
        }
        Log.e(TAG, "Mismatched lengths of TCP socket stats data arrays");
    }

    @Override // android.net.metrics.INetdEventListener
    public int getInterfaceVersion() throws RemoteException {
        return 1;
    }

    @Override // android.net.metrics.INetdEventListener
    public String getInterfaceHash() {
        return INetdEventListener.HASH;
    }

    private void addWakeupEvent(WakeupEvent event) {
        String iface = event.iface;
        this.mWakeupEvents.append(event);
        WakeupStats stats = this.mWakeupStats.get(iface);
        if (stats == null) {
            stats = new WakeupStats(iface);
            this.mWakeupStats.put(iface, stats);
        }
        stats.countEvent(event);
    }

    public synchronized void flushStatistics(List<IpConnectivityLogClass.IpConnectivityEvent> events) {
        for (int i = 0; i < this.mNetworkMetrics.size(); i++) {
            ConnectStats stats = this.mNetworkMetrics.valueAt(i).connectMetrics;
            if (stats.eventCount != 0) {
                events.add(IpConnectivityEventBuilder.toProto(stats));
            }
        }
        for (int i2 = 0; i2 < this.mNetworkMetrics.size(); i2++) {
            DnsEvent ev = this.mNetworkMetrics.valueAt(i2).dnsMetrics;
            if (ev.eventCount != 0) {
                events.add(IpConnectivityEventBuilder.toProto(ev));
            }
        }
        for (int i3 = 0; i3 < this.mWakeupStats.size(); i3++) {
            events.add(IpConnectivityEventBuilder.toProto(this.mWakeupStats.valueAt(i3)));
        }
        this.mNetworkMetrics.clear();
        this.mWakeupStats.clear();
    }

    public synchronized void list(PrintWriter pw) {
        NetworkMetricsSnapshot[] networkMetricsSnapshots;
        WakeupEvent[] wakeupEventArr;
        pw.println("dns/connect events:");
        for (int i = 0; i < this.mNetworkMetrics.size(); i++) {
            pw.println(this.mNetworkMetrics.valueAt(i).connectMetrics);
        }
        for (int i2 = 0; i2 < this.mNetworkMetrics.size(); i2++) {
            pw.println(this.mNetworkMetrics.valueAt(i2).dnsMetrics);
        }
        pw.println("");
        pw.println("network statistics:");
        for (NetworkMetricsSnapshot s : getNetworkMetricsSnapshots()) {
            pw.println(s);
        }
        pw.println("");
        pw.println("packet wakeup events:");
        for (int i3 = 0; i3 < this.mWakeupStats.size(); i3++) {
            pw.println(this.mWakeupStats.valueAt(i3));
        }
        for (WakeupEvent wakeup : (WakeupEvent[]) this.mWakeupEvents.toArray()) {
            pw.println(wakeup);
        }
    }

    public synchronized List<IpConnectivityLogClass.IpConnectivityEvent> listAsProtos() {
        List<IpConnectivityLogClass.IpConnectivityEvent> list;
        list = new ArrayList<>();
        for (int i = 0; i < this.mNetworkMetrics.size(); i++) {
            list.add(IpConnectivityEventBuilder.toProto(this.mNetworkMetrics.valueAt(i).connectMetrics));
        }
        for (int i2 = 0; i2 < this.mNetworkMetrics.size(); i2++) {
            list.add(IpConnectivityEventBuilder.toProto(this.mNetworkMetrics.valueAt(i2).dnsMetrics));
        }
        for (int i3 = 0; i3 < this.mWakeupStats.size(); i3++) {
            list.add(IpConnectivityEventBuilder.toProto(this.mWakeupStats.valueAt(i3)));
        }
        return list;
    }

    private long getTransports(int netId) {
        NetworkCapabilities nc = this.mCm.getNetworkCapabilities(new Network(netId));
        if (nc == null) {
            return 0L;
        }
        return BitUtils.packBits(nc.getTransportTypes());
    }

    private static void maybeLog(String s, Object... args) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class NetworkMetricsSnapshot {
        public List<NetworkMetrics.Summary> stats = new ArrayList();
        public long timeMs;

        NetworkMetricsSnapshot() {
        }

        static NetworkMetricsSnapshot collect(long timeMs, SparseArray<NetworkMetrics> networkMetrics) {
            NetworkMetricsSnapshot snapshot = new NetworkMetricsSnapshot();
            snapshot.timeMs = timeMs;
            for (int i = 0; i < networkMetrics.size(); i++) {
                NetworkMetrics.Summary s = networkMetrics.valueAt(i).getPendingStats();
                if (s != null) {
                    snapshot.stats.add(s);
                }
            }
            return snapshot;
        }

        public String toString() {
            StringJoiner j = new StringJoiner(", ");
            for (NetworkMetrics.Summary s : this.stats) {
                j.add(s.toString());
            }
            return String.format("%tT.%tL: %s", Long.valueOf(this.timeMs), Long.valueOf(this.timeMs), j.toString());
        }
    }
}