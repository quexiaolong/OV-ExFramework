package com.android.server.net;

import android.net.NetworkIdentity;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.VivoNetworkStats;
import android.net.VivoNetworkStatsImpl;
import android.telephony.SubscriptionPlan;
import android.util.ArrayMap;
import android.util.MathUtils;
import android.util.Range;
import com.android.internal.util.FileRotator;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

/* loaded from: classes.dex */
public class VivoNetworkStatsCollection implements FileRotator.Reader {
    private static final int FILE_MAGIC = 1095648596;
    private static final int VERSION_NETWORK_INIT = 1;
    private static final int VERSION_RAT_INFO = 17;
    private static final int VERSION_UID_INIT = 1;
    private static final int VERSION_UID_WITH_IDENT = 2;
    private static final int VERSION_UID_WITH_SET = 4;
    private static final int VERSION_UID_WITH_TAG = 3;
    private static final int VERSION_UNIFIED_INIT = 16;
    private final long mBucketDuration;
    private boolean mDirty;
    private long mEndMillis;
    private long mStartMillis;
    private ArrayMap<Key, NetworkStatsHistory> mStats = new ArrayMap<>();
    private long mTotalBytes;
    public int mVivoInfoIndicator;

    public VivoNetworkStatsCollection(long bucketDuration, int vivoInfoIndicator) {
        this.mVivoInfoIndicator = 0;
        this.mBucketDuration = bucketDuration;
        this.mVivoInfoIndicator = vivoInfoIndicator;
        reset();
    }

    public void reset() {
        this.mStats.clear();
        this.mStartMillis = Long.MAX_VALUE;
        this.mEndMillis = Long.MIN_VALUE;
        this.mTotalBytes = 0L;
        this.mDirty = false;
    }

    public long getTotalBytes() {
        return this.mTotalBytes;
    }

    public boolean isDirty() {
        return this.mDirty;
    }

    public NetworkStatsHistory getHistory(NetworkTemplate template, SubscriptionPlan augmentPlan, VivoNetworkStatsImpl.VivoEntryImpl vivoEntry, int uid, int set, int tag, int fields, long start, long end, int accessLevel, int callerUid) {
        long augmentStart;
        long collectEnd;
        long collectEnd2;
        long collectEnd3;
        long augmentEnd;
        long collectEnd4;
        long augmentStart2;
        if (NetworkStatsAccess.isAccessibleToUser(uid, callerUid, accessLevel)) {
            long j = this.mBucketDuration;
            int bucketEstimate = (int) MathUtils.constrain((end - start) / j, 0L, 15552000000L / j);
            NetworkStatsHistory combined = new NetworkStatsHistory(this.mBucketDuration, bucketEstimate, fields);
            if (start == end) {
                return combined;
            }
            long collectEnd5 = -1;
            long augmentEnd2 = augmentPlan != null ? augmentPlan.getDataUsageTime() : -1L;
            long collectStart = start;
            long augmentStart3 = end;
            if (augmentEnd2 == -1) {
                augmentStart = augmentStart3;
                collectEnd = -1;
            } else {
                Iterator<Range<ZonedDateTime>> it = augmentPlan.cycleIterator();
                while (it.hasNext()) {
                    Range<ZonedDateTime> cycle = it.next();
                    long cycleStart = cycle.getLower().toInstant().toEpochMilli();
                    long cycleEnd = cycle.getUpper().toInstant().toEpochMilli();
                    if (cycleStart > augmentEnd2 || augmentEnd2 >= cycleEnd) {
                        collectEnd5 = collectEnd5;
                        augmentStart3 = augmentStart3;
                    } else {
                        collectStart = Long.min(collectStart, cycleStart);
                        long collectEnd6 = Long.max(augmentStart3, augmentEnd2);
                        collectEnd2 = collectEnd6;
                        collectEnd3 = cycleStart;
                        break;
                    }
                }
                long j2 = collectEnd5;
                augmentStart = augmentStart3;
                collectEnd = j2;
            }
            collectEnd2 = augmentStart;
            collectEnd3 = collectEnd;
            if (collectEnd3 == -1) {
                augmentEnd = augmentEnd2;
                collectEnd4 = collectEnd3;
            } else {
                long augmentStart4 = roundUp(collectEnd3);
                long augmentEnd3 = roundDown(augmentEnd2);
                roundDown(collectStart);
                roundUp(collectEnd2);
                augmentEnd = augmentEnd3;
                collectEnd4 = augmentStart4;
            }
            for (int i = 0; i < this.mStats.size(); i++) {
                Key key = this.mStats.keyAt(i);
                Key.VivoKey vivoKey = new Key.VivoKey();
                vivoKey.vivoInfoIndicator = vivoEntry.mVivoInfoIndicator;
                vivoKey.packInfo = vivoEntry.packInfo;
                vivoKey.ratInfo = vivoEntry.ratInfo;
                if (key.uid == uid && key.tag == tag) {
                    if (VivoNetworkStats.setMatches(set, key.set) && templateMatches(template, key.ident) && key.vivoKey.equals(vivoKey)) {
                        NetworkStatsHistory value = this.mStats.valueAt(i);
                        combined.recordHistory(value, start, end);
                    }
                }
            }
            if (collectEnd4 != -1) {
                NetworkStatsHistory.Entry entry = combined.getValues(collectEnd4, augmentEnd, (NetworkStatsHistory.Entry) null);
                if (entry.rxBytes == 0 || entry.txBytes == 0) {
                    long j3 = collectEnd4;
                    long j4 = augmentEnd;
                    combined.recordData(j3, j4, new NetworkStats.Entry(1L, 0L, 1L, 0L, 0L));
                    combined.getValues(j3, j4, entry);
                }
                long rawBytes = entry.txBytes + entry.rxBytes;
                long rawRxBytes = entry.rxBytes;
                long rawTxBytes = entry.txBytes;
                long targetBytes = augmentPlan.getDataUsageBytes();
                long targetRxBytes = multiplySafe(targetBytes, rawRxBytes, rawBytes);
                long targetTxBytes = multiplySafe(targetBytes, rawTxBytes, rawBytes);
                long beforeTotal = combined.getTotalBytes();
                int i2 = 0;
                while (i2 < combined.size()) {
                    combined.getValues(i2, entry);
                    long rawBytes2 = rawBytes;
                    if (entry.bucketStart >= collectEnd4) {
                        augmentStart2 = collectEnd4;
                        if (entry.bucketStart + entry.bucketDuration <= augmentEnd) {
                            entry.rxBytes = multiplySafe(targetRxBytes, entry.rxBytes, rawRxBytes);
                            entry.txBytes = multiplySafe(targetTxBytes, entry.txBytes, rawTxBytes);
                            entry.rxPackets = 0L;
                            entry.txPackets = 0L;
                            combined.setValues(i2, entry);
                        }
                    } else {
                        augmentStart2 = collectEnd4;
                    }
                    i2++;
                    rawBytes = rawBytes2;
                    collectEnd4 = augmentStart2;
                }
                long deltaTotal = combined.getTotalBytes() - beforeTotal;
                if (deltaTotal != 0) {
                    VivoNetworkStatsServiceImpl.log(1, "Augmented network usage by " + deltaTotal + " bytes");
                }
                NetworkStatsHistory sliced = new NetworkStatsHistory(this.mBucketDuration, bucketEstimate, fields);
                sliced.recordHistory(combined, start, end);
                return sliced;
            }
            return combined;
        }
        throw new SecurityException("Network stats history of uid " + uid + " is forbidden for caller " + callerUid);
    }

    public VivoNetworkStats getSummary(NetworkTemplate template, long start, long end, int accessLevel, int callerUid) {
        long now = System.currentTimeMillis();
        VivoNetworkStats stats = new VivoNetworkStats(end - start, 24);
        VivoNetworkStats.Entry entry = new VivoNetworkStats.Entry();
        stats.setVivoInfoIndicator(this.mVivoInfoIndicator);
        stats.syncVivoInfoIndicatorTo(entry);
        if (start == end) {
            return stats;
        }
        NetworkStatsHistory.Entry historyEntry = null;
        for (int i = 0; i < this.mStats.size(); i++) {
            Key key = this.mStats.keyAt(i);
            if (templateMatches(template, key.ident) && NetworkStatsAccess.isAccessibleToUser(key.uid, callerUid, accessLevel) && key.set < 1000) {
                NetworkStatsHistory value = this.mStats.valueAt(i);
                NetworkStatsHistory.Entry historyEntry2 = value.getValues(start, end, now, historyEntry);
                if (entry.mVivoEntry != null) {
                    VivoNetworkStatsImpl.VivoEntryImpl vivoEntryImpl = entry.mVivoEntry;
                    if (vivoEntryImpl instanceof VivoNetworkStatsImpl.VivoEntryImpl) {
                        VivoNetworkStatsImpl.VivoEntryImpl entryImpl = vivoEntryImpl;
                        entryImpl.mVivoInfoIndicator = key.vivoKey.vivoInfoIndicator;
                        entryImpl.packInfo = key.vivoKey.packInfo;
                        entryImpl.ratInfo = key.vivoKey.ratInfo;
                    }
                }
                entry.iface = VivoNetworkStats.IFACE_ALL;
                entry.uid = key.uid;
                entry.set = key.set;
                entry.tag = key.tag;
                entry.defaultNetwork = key.ident.areAllMembersOnDefaultNetwork() ? 1 : 0;
                entry.metered = key.ident.isAnyMemberMetered() ? 1 : 0;
                entry.roaming = key.ident.isAnyMemberRoaming() ? 1 : 0;
                entry.rxBytes = historyEntry2.rxBytes;
                entry.rxPackets = historyEntry2.rxPackets;
                entry.txBytes = historyEntry2.txBytes;
                entry.txPackets = historyEntry2.txPackets;
                entry.operations = historyEntry2.operations;
                if (!entry.isEmpty()) {
                    stats.combineValues(entry);
                }
                historyEntry = historyEntry2;
            }
        }
        return stats;
    }

    public void recordData(NetworkIdentitySet ident, VivoNetworkStatsImpl.VivoEntryImpl vivoEntry, int uid, int set, int tag, long start, long end, VivoNetworkStats.Entry entry) {
        NetworkStatsHistory history = findOrCreateHistory(ident, vivoEntry, uid, set, tag);
        VivoNetworkStatsServiceImpl.log(4, "before record = " + history);
        NetworkStats.Entry recordEntry = new NetworkStats.Entry(entry.iface, entry.uid, entry.set, entry.tag, entry.metered, entry.roaming, entry.defaultNetwork, entry.rxBytes, entry.rxPackets, entry.txBytes, entry.txPackets, entry.operations);
        history.recordData(start, end, recordEntry);
        VivoNetworkStatsServiceImpl.log(4, "after record = " + history);
        noteRecordedHistory(history.getStart(), history.getEnd(), entry.rxBytes + entry.txBytes);
    }

    private void recordHistory(Key key, NetworkStatsHistory history) {
        if (history.size() == 0) {
            return;
        }
        noteRecordedHistory(history.getStart(), history.getEnd(), history.getTotalBytes());
        NetworkStatsHistory target = this.mStats.get(key);
        if (target == null) {
            target = new NetworkStatsHistory(history.getBucketDuration());
            this.mStats.put(key, target);
        }
        target.recordEntireHistory(history);
    }

    public void recordCollection(VivoNetworkStatsCollection another) {
        for (int i = 0; i < another.mStats.size(); i++) {
            Key key = another.mStats.keyAt(i);
            NetworkStatsHistory value = another.mStats.valueAt(i);
            recordHistory(key, value);
        }
    }

    private NetworkStatsHistory findOrCreateHistory(NetworkIdentitySet ident, VivoNetworkStatsImpl.VivoEntryImpl vivoEntry, int uid, int set, int tag) {
        Key.VivoKey vivoKey = new Key.VivoKey();
        vivoKey.vivoInfoIndicator = vivoEntry.mVivoInfoIndicator;
        vivoKey.packInfo = vivoEntry.packInfo;
        vivoKey.ratInfo = vivoEntry.ratInfo;
        Key key = new Key(ident, vivoKey, uid, set, tag);
        NetworkStatsHistory existing = this.mStats.get(key);
        NetworkStatsHistory updated = null;
        if (existing == null) {
            updated = new NetworkStatsHistory(this.mBucketDuration, 10);
        } else if (existing.getBucketDuration() != this.mBucketDuration) {
            updated = new NetworkStatsHistory(existing, this.mBucketDuration);
        }
        if (updated != null) {
            this.mStats.put(key, updated);
            return updated;
        }
        return existing;
    }

    public long roundUp(long time) {
        if (time == Long.MIN_VALUE || time == Long.MAX_VALUE || time == -1) {
            return time;
        }
        long j = this.mBucketDuration;
        long mod = time % j;
        if (mod > 0) {
            return (time - mod) + j;
        }
        return time;
    }

    public long roundDown(long time) {
        if (time == Long.MIN_VALUE || time == Long.MAX_VALUE || time == -1) {
            return time;
        }
        long mod = time % this.mBucketDuration;
        if (mod > 0) {
            return time - mod;
        }
        return time;
    }

    public static long multiplySafe(long value, long num, long den) {
        long den2 = den == 0 ? 1L : den;
        long r = value * num;
        long ax = Math.abs(value);
        long ay = Math.abs(num);
        if (((ax | ay) >>> 31) != 0) {
            if ((num != 0 && r / num != value) || (value == Long.MIN_VALUE && num == -1)) {
                return (long) ((num / den2) * value);
            }
        }
        long x = r / den2;
        return x;
    }

    public void read(InputStream in) throws IOException {
        read(new DataInputStream(in));
    }

    public void read(DataInputStream in) throws IOException {
        int magic = in.readInt();
        if (magic != FILE_MAGIC) {
            throw new ProtocolException("unexpected magic: " + magic);
        }
        int version = in.readInt();
        VivoNetworkStatsServiceImpl.log(3, "read version = " + version);
        int i = 1;
        if (version == 16) {
            int identSize = in.readInt();
            int i2 = 0;
            while (i2 < identSize) {
                NetworkIdentitySet ident = new NetworkIdentitySet(in);
                VivoNetworkStatsServiceImpl.log(3, "read ident = " + ident);
                int size = in.readInt();
                int j = 0;
                while (j < size) {
                    String packInfo = readOptionalString(in);
                    int uid = in.readInt();
                    int set = in.readInt();
                    int tag = in.readInt();
                    Key.VivoKey vivoKey = new Key.VivoKey();
                    vivoKey.vivoInfoIndicator = i;
                    vivoKey.packInfo = packInfo;
                    vivoKey.ratInfo = -1;
                    Key key = new Key(ident, vivoKey, uid, set, tag);
                    NetworkStatsHistory history = new NetworkStatsHistory(in);
                    VivoNetworkStatsServiceImpl.log(3, "read key = " + key + ", history = " + history);
                    recordHistory(key, history);
                    j++;
                    size = size;
                    i = 1;
                }
                i2++;
                i = 1;
            }
        } else if (version == 17) {
            int identSize2 = in.readInt();
            for (int i3 = 0; i3 < identSize2; i3++) {
                NetworkIdentitySet ident2 = new NetworkIdentitySet(in);
                VivoNetworkStatsServiceImpl.log(3, "read ident = " + ident2);
                int j2 = 0;
                for (int size2 = in.readInt(); j2 < size2; size2 = size2) {
                    int uid2 = in.readInt();
                    int set2 = in.readInt();
                    int tag2 = in.readInt();
                    Key.VivoKey vivoKey2 = new Key.VivoKey();
                    vivoKey2.vivoInfoIndicator = in.readInt();
                    if ((vivoKey2.vivoInfoIndicator & 1) > 0) {
                        vivoKey2.packInfo = readOptionalString(in);
                    }
                    if ((vivoKey2.vivoInfoIndicator & 2) > 0) {
                        vivoKey2.ratInfo = in.readInt();
                    }
                    int j3 = j2;
                    Key key2 = new Key(ident2, vivoKey2, uid2, set2, tag2);
                    NetworkStatsHistory history2 = new NetworkStatsHistory(in);
                    VivoNetworkStatsServiceImpl.log(3, "read key = " + key2 + ", history = " + history2);
                    recordHistory(key2, history2);
                    j2 = j3 + 1;
                }
            }
        } else {
            throw new ProtocolException("unexpected version: " + version);
        }
    }

    public void write(DataOutputStream out) throws IOException {
        HashMap<NetworkIdentitySet, ArrayList<Key>> keysByIdent = Maps.newHashMap();
        for (Key key : this.mStats.keySet()) {
            ArrayList<Key> keys = keysByIdent.get(key.ident);
            if (keys == null) {
                keys = Lists.newArrayList();
                keysByIdent.put(key.ident, keys);
            }
            keys.add(key);
        }
        out.writeInt(FILE_MAGIC);
        out.writeInt(17);
        out.writeInt(keysByIdent.size());
        for (NetworkIdentitySet ident : keysByIdent.keySet()) {
            VivoNetworkStatsServiceImpl.log(3, "write ident = " + ident);
            ArrayList<Key> keys2 = keysByIdent.get(ident);
            ident.writeToStream(out);
            out.writeInt(keys2.size());
            Iterator<Key> it = keys2.iterator();
            while (it.hasNext()) {
                Key key2 = it.next();
                out.writeInt(key2.uid);
                out.writeInt(key2.set);
                out.writeInt(key2.tag);
                out.writeInt(key2.vivoKey.vivoInfoIndicator);
                if ((key2.vivoKey.vivoInfoIndicator & 1) > 0) {
                    writeOptionalString(out, key2.vivoKey.packInfo);
                }
                if ((key2.vivoKey.vivoInfoIndicator & 2) > 0) {
                    out.writeInt(key2.vivoKey.ratInfo);
                }
                NetworkStatsHistory history = this.mStats.get(key2);
                history.writeToStream(out);
                VivoNetworkStatsServiceImpl.log(3, "write key = " + key2 + ", history = " + history);
            }
        }
        out.flush();
    }

    private void noteRecordedHistory(long startMillis, long endMillis, long totalBytes) {
        if (startMillis < this.mStartMillis) {
            this.mStartMillis = startMillis;
        }
        if (endMillis > this.mEndMillis) {
            this.mEndMillis = endMillis;
        }
        this.mTotalBytes += totalBytes;
        this.mDirty = true;
    }

    private static boolean templateMatches(NetworkTemplate template, NetworkIdentitySet identSet) {
        Iterator it = identSet.iterator();
        while (it.hasNext()) {
            NetworkIdentity ident = (NetworkIdentity) it.next();
            if (template.matches(ident)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Key implements Comparable<Key> {
        private final int hashCode;
        public final NetworkIdentitySet ident;
        public final int set;
        public final int tag;
        public final int uid;
        public final VivoKey vivoKey;

        /* loaded from: classes.dex */
        public static class VivoKey {
            public String packInfo = "UnsetPack";
            public int ratInfo = -1;
            public int vivoInfoIndicator;

            public int hashCode() {
                int result = (1 * 31) + Objects.hash(Integer.valueOf(this.vivoInfoIndicator));
                return (((result * 31) + Objects.hash(this.packInfo)) * 31) + Objects.hash(Integer.valueOf(this.ratInfo));
            }

            public boolean equals(Object obj) {
                if (obj == null || !(obj instanceof VivoKey)) {
                    return false;
                }
                VivoKey e = (VivoKey) obj;
                int i = this.vivoInfoIndicator;
                if (i != e.vivoInfoIndicator) {
                    return false;
                }
                if ((i & 1) > 0 && !Objects.equals(this.packInfo, e.packInfo)) {
                    return false;
                }
                if ((this.vivoInfoIndicator & 2) > 0 && this.ratInfo != e.ratInfo) {
                    return false;
                }
                return true;
            }
        }

        public Key(NetworkIdentitySet ident, VivoKey vivoKey, int uid, int set, int tag) {
            this.ident = ident;
            this.vivoKey = vivoKey;
            this.uid = uid;
            this.set = set;
            this.tag = tag;
            this.hashCode = Objects.hash(ident, vivoKey, Integer.valueOf(uid), Integer.valueOf(set), Integer.valueOf(tag));
        }

        public int hashCode() {
            return this.hashCode;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Key) {
                Key key = (Key) obj;
                if (this.vivoKey.vivoInfoIndicator != key.vivoKey.vivoInfoIndicator) {
                    return false;
                }
                if ((this.vivoKey.vivoInfoIndicator & 1) <= 0 || Objects.equals(this.vivoKey.packInfo, key.vivoKey.packInfo)) {
                    return ((this.vivoKey.vivoInfoIndicator & 2) <= 0 || this.vivoKey.ratInfo == key.vivoKey.ratInfo) && this.uid == key.uid && this.set == key.set && this.tag == key.tag && Objects.equals(this.ident, key.ident);
                }
                return false;
            }
            return false;
        }

        @Override // java.lang.Comparable
        public int compareTo(Key another) {
            VivoKey vivoKey;
            NetworkIdentitySet networkIdentitySet;
            int res = 0;
            NetworkIdentitySet networkIdentitySet2 = this.ident;
            if (networkIdentitySet2 != null && (networkIdentitySet = another.ident) != null) {
                res = networkIdentitySet2.compareTo(networkIdentitySet);
            }
            if (res == 0) {
                res = Integer.compare(this.uid, another.uid);
            }
            if (res == 0) {
                res = Integer.compare(this.set, another.set);
            }
            if (res == 0) {
                res = Integer.compare(this.tag, another.tag);
            }
            if (res == 0 && (vivoKey = this.vivoKey) != null && another.vivoKey != null) {
                int res2 = Integer.compare(vivoKey.vivoInfoIndicator, another.vivoKey.vivoInfoIndicator);
                if (res2 == 0 && this.vivoKey.packInfo != null && another.vivoKey.packInfo != null) {
                    res2 = this.vivoKey.packInfo.compareTo(another.vivoKey.packInfo);
                }
                if (res2 == 0) {
                    return Integer.compare(this.vivoKey.ratInfo, another.vivoKey.ratInfo);
                }
                return res2;
            }
            return res;
        }

        public String toString() {
            return "ident=" + this.ident + " uid=" + this.uid + " set=" + this.set + " tag=" + this.tag + " vivo=" + this.vivoKey.vivoInfoIndicator + " packinfo=" + this.vivoKey.packInfo + " ratinfo=" + this.vivoKey.ratInfo;
        }
    }

    private static String readOptionalString(DataInputStream in) throws IOException {
        if (in.readByte() != 0) {
            return in.readUTF();
        }
        return null;
    }

    private static void writeOptionalString(DataOutputStream out, String value) throws IOException {
        if (value != null) {
            out.writeByte(1);
            out.writeUTF(value);
            return;
        }
        out.writeByte(0);
    }
}