package com.android.server.net;

import android.net.NetworkIdentity;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.os.Binder;
import android.telephony.SubscriptionPlan;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.IntArray;
import android.util.MathUtils;
import android.util.Range;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FileRotator;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.controllers.JobStatus;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ProtocolException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import libcore.io.IoUtils;

/* loaded from: classes.dex */
public class NetworkStatsCollection implements FileRotator.Reader {
    private static final int FILE_MAGIC = 1095648596;
    private static final int VERSION_NETWORK_INIT = 1;
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

    public NetworkStatsCollection(long bucketDuration) {
        this.mBucketDuration = bucketDuration;
        reset();
    }

    public void clear() {
        reset();
    }

    public void reset() {
        this.mStats.clear();
        this.mStartMillis = JobStatus.NO_LATEST_RUNTIME;
        this.mEndMillis = Long.MIN_VALUE;
        this.mTotalBytes = 0L;
        this.mDirty = false;
    }

    public long getStartMillis() {
        return this.mStartMillis;
    }

    public long getFirstAtomicBucketMillis() {
        long j = this.mStartMillis;
        return j == JobStatus.NO_LATEST_RUNTIME ? JobStatus.NO_LATEST_RUNTIME : j + this.mBucketDuration;
    }

    public long getEndMillis() {
        return this.mEndMillis;
    }

    public long getTotalBytes() {
        return this.mTotalBytes;
    }

    public boolean isDirty() {
        return this.mDirty;
    }

    public void clearDirty() {
        this.mDirty = false;
    }

    public boolean isEmpty() {
        return this.mStartMillis == JobStatus.NO_LATEST_RUNTIME && this.mEndMillis == Long.MIN_VALUE;
    }

    public long roundUp(long time) {
        if (time == Long.MIN_VALUE || time == JobStatus.NO_LATEST_RUNTIME || time == -1) {
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
        if (time == Long.MIN_VALUE || time == JobStatus.NO_LATEST_RUNTIME || time == -1) {
            return time;
        }
        long mod = time % this.mBucketDuration;
        if (mod > 0) {
            return time - mod;
        }
        return time;
    }

    public int[] getRelevantUids(int accessLevel) {
        return getRelevantUids(accessLevel, Binder.getCallingUid());
    }

    public int[] getRelevantUids(int accessLevel, int callerUid) {
        int j;
        IntArray uids = new IntArray();
        for (int i = 0; i < this.mStats.size(); i++) {
            Key key = this.mStats.keyAt(i);
            if (NetworkStatsAccess.isAccessibleToUser(key.uid, callerUid, accessLevel) && (j = uids.binarySearch(key.uid)) < 0) {
                uids.add(~j, key.uid);
            }
        }
        return uids.toArray();
    }

    public NetworkStatsHistory getHistory(NetworkTemplate template, SubscriptionPlan augmentPlan, int uid, int set, int tag, int fields, long start, long end, int accessLevel, int callerUid) {
        long collectEnd;
        long augmentStart;
        long collectEnd2;
        long augmentEnd;
        long collectStart;
        if (NetworkStatsAccess.isAccessibleToUser(uid, callerUid, accessLevel)) {
            long j = this.mBucketDuration;
            int bucketEstimate = (int) MathUtils.constrain((end - start) / j, 0L, 15552000000L / j);
            NetworkStatsHistory combined = new NetworkStatsHistory(this.mBucketDuration, bucketEstimate, fields);
            if (start == end) {
                return combined;
            }
            long augmentStart2 = -1;
            long augmentEnd2 = augmentPlan != null ? augmentPlan.getDataUsageTime() : -1L;
            long collectStart2 = start;
            long collectEnd3 = end;
            if (augmentEnd2 == -1) {
                collectEnd = collectEnd3;
            } else {
                Iterator<Range<ZonedDateTime>> it = augmentPlan.cycleIterator();
                while (true) {
                    if (!it.hasNext()) {
                        collectEnd = collectEnd3;
                        break;
                    }
                    Range<ZonedDateTime> cycle = it.next();
                    long cycleStart = cycle.getLower().toInstant().toEpochMilli();
                    long cycleEnd = cycle.getUpper().toInstant().toEpochMilli();
                    if (cycleStart <= augmentEnd2 && augmentEnd2 < cycleEnd) {
                        augmentStart2 = cycleStart;
                        collectStart2 = Long.min(collectStart2, augmentStart2);
                        collectEnd = Long.max(collectEnd3, augmentEnd2);
                        break;
                    }
                    collectEnd3 = collectEnd3;
                }
            }
            if (augmentStart2 == -1) {
                augmentStart = augmentStart2;
                collectEnd2 = collectEnd;
                augmentEnd = augmentEnd2;
                collectStart = collectStart2;
            } else {
                long augmentStart3 = roundUp(augmentStart2);
                long augmentEnd3 = roundDown(augmentEnd2);
                long collectStart3 = roundDown(collectStart2);
                augmentStart = augmentStart3;
                collectEnd2 = roundUp(collectEnd);
                augmentEnd = augmentEnd3;
                collectStart = collectStart3;
            }
            for (int i = 0; i < this.mStats.size(); i++) {
                Key key = this.mStats.keyAt(i);
                if (key.uid == uid && NetworkStats.setMatches(set, key.set)) {
                    if (key.tag == tag && templateMatches(template, key.ident)) {
                        NetworkStatsHistory value = this.mStats.valueAt(i);
                        combined.recordHistory(value, collectStart, collectEnd2);
                    }
                }
            }
            if (augmentStart != -1) {
                NetworkStatsHistory.Entry entry = combined.getValues(augmentStart, augmentEnd, (NetworkStatsHistory.Entry) null);
                if (entry.rxBytes == 0 || entry.txBytes == 0) {
                    long j2 = augmentStart;
                    long j3 = augmentEnd;
                    combined.recordData(j2, j3, new NetworkStats.Entry(1L, 0L, 1L, 0L, 0L));
                    combined.getValues(j2, j3, entry);
                }
                long rawBytes = entry.rxBytes + entry.txBytes;
                long rawRxBytes = entry.rxBytes;
                long rawTxBytes = entry.txBytes;
                long targetBytes = augmentPlan.getDataUsageBytes();
                long targetRxBytes = NetworkStatsHistory.multiplySafe(targetBytes, rawRxBytes, rawBytes);
                long targetTxBytes = NetworkStatsHistory.multiplySafe(targetBytes, rawTxBytes, rawBytes);
                long beforeTotal = combined.getTotalBytes();
                int i2 = 0;
                while (i2 < combined.size()) {
                    combined.getValues(i2, entry);
                    long rawBytes2 = rawBytes;
                    if (entry.bucketStart >= augmentStart && entry.bucketStart + entry.bucketDuration <= augmentEnd) {
                        entry.rxBytes = NetworkStatsHistory.multiplySafe(targetRxBytes, entry.rxBytes, rawRxBytes);
                        entry.txBytes = NetworkStatsHistory.multiplySafe(targetTxBytes, entry.txBytes, rawTxBytes);
                        entry.rxPackets = 0L;
                        entry.txPackets = 0L;
                        combined.setValues(i2, entry);
                    }
                    i2++;
                    rawBytes = rawBytes2;
                }
                long deltaTotal = combined.getTotalBytes() - beforeTotal;
                if (deltaTotal != 0) {
                    Slog.d("NetworkStats", "Augmented network usage by " + deltaTotal + " bytes");
                }
                NetworkStatsHistory sliced = new NetworkStatsHistory(this.mBucketDuration, bucketEstimate, fields);
                sliced.recordHistory(combined, start, end);
                return sliced;
            }
            return combined;
        }
        throw new SecurityException("Network stats history of uid " + uid + " is forbidden for caller " + callerUid);
    }

    public NetworkStats getSummary(NetworkTemplate template, long start, long end, int accessLevel, int callerUid) {
        long now = System.currentTimeMillis();
        NetworkStats stats = new NetworkStats(end - start, 24);
        if (start == end) {
            return stats;
        }
        NetworkStats.Entry entry = new NetworkStats.Entry();
        NetworkStatsHistory.Entry historyEntry = null;
        for (int i = 0; i < this.mStats.size(); i++) {
            Key key = this.mStats.keyAt(i);
            if (templateMatches(template, key.ident) && NetworkStatsAccess.isAccessibleToUser(key.uid, callerUid, accessLevel) && key.set < 1000) {
                NetworkStatsHistory value = this.mStats.valueAt(i);
                NetworkStatsHistory.Entry historyEntry2 = value.getValues(start, end, now, historyEntry);
                entry.iface = NetworkStats.IFACE_ALL;
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

    public void recordData(NetworkIdentitySet ident, int uid, int set, int tag, long start, long end, NetworkStats.Entry entry) {
        NetworkStatsHistory history = findOrCreateHistory(ident, uid, set, tag);
        history.recordData(start, end, entry);
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

    public void recordCollection(NetworkStatsCollection another) {
        for (int i = 0; i < another.mStats.size(); i++) {
            Key key = another.mStats.keyAt(i);
            NetworkStatsHistory value = another.mStats.valueAt(i);
            recordHistory(key, value);
        }
    }

    private NetworkStatsHistory findOrCreateHistory(NetworkIdentitySet ident, int uid, int set, int tag) {
        Key key = new Key(ident, uid, set, tag);
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

    public void read(InputStream in) throws IOException {
        read(new DataInputStream(in));
    }

    public void read(DataInputStream in) throws IOException {
        int magic = in.readInt();
        if (magic != FILE_MAGIC) {
            throw new ProtocolException("unexpected magic: " + magic);
        }
        int version = in.readInt();
        if (version == 16) {
            int identSize = in.readInt();
            for (int i = 0; i < identSize; i++) {
                NetworkIdentitySet ident = new NetworkIdentitySet(in);
                int size = in.readInt();
                for (int j = 0; j < size; j++) {
                    int uid = in.readInt();
                    int set = in.readInt();
                    int tag = in.readInt();
                    Key key = new Key(ident, uid, set, tag);
                    NetworkStatsHistory history = new NetworkStatsHistory(in);
                    recordHistory(key, history);
                }
            }
            return;
        }
        throw new ProtocolException("unexpected version: " + version);
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
        out.writeInt(16);
        out.writeInt(keysByIdent.size());
        for (NetworkIdentitySet ident : keysByIdent.keySet()) {
            ArrayList<Key> keys2 = keysByIdent.get(ident);
            ident.writeToStream(out);
            out.writeInt(keys2.size());
            Iterator<Key> it = keys2.iterator();
            while (it.hasNext()) {
                Key key2 = it.next();
                NetworkStatsHistory history = this.mStats.get(key2);
                out.writeInt(key2.uid);
                out.writeInt(key2.set);
                out.writeInt(key2.tag);
                history.writeToStream(out);
            }
        }
        out.flush();
    }

    @Deprecated
    public void readLegacyNetwork(File file) throws IOException {
        int magic;
        AtomicFile inputFile = new AtomicFile(file);
        DataInputStream in = null;
        try {
            in = new DataInputStream(new BufferedInputStream(inputFile.openRead()));
            magic = in.readInt();
        } catch (FileNotFoundException e) {
        } catch (Throwable th) {
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
        if (magic != FILE_MAGIC) {
            throw new ProtocolException("unexpected magic: " + magic);
        }
        int version = in.readInt();
        if (version == 1) {
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                NetworkIdentitySet ident = new NetworkIdentitySet(in);
                NetworkStatsHistory history = new NetworkStatsHistory(in);
                Key key = new Key(ident, -1, -1, 0);
                recordHistory(key, history);
            }
            IoUtils.closeQuietly(in);
            return;
        }
        throw new ProtocolException("unexpected version: " + version);
    }

    @Deprecated
    public void readLegacyUid(File file, boolean onlyTags) throws IOException {
        int magic;
        AtomicFile inputFile = new AtomicFile(file);
        DataInputStream in = null;
        try {
            in = new DataInputStream(new BufferedInputStream(inputFile.openRead()));
            magic = in.readInt();
        } catch (FileNotFoundException e) {
        } catch (Throwable th) {
            th = th;
        }
        try {
        } catch (FileNotFoundException e2) {
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly(in);
            throw th;
        }
        if (magic != FILE_MAGIC) {
            throw new ProtocolException("unexpected magic: " + magic);
        }
        int version = in.readInt();
        if (version != 1 && version != 2) {
            int i = 4;
            if (version != 3 && version != 4) {
                throw new ProtocolException("unexpected version: " + version);
            }
            int identSize = in.readInt();
            int i2 = 0;
            while (i2 < identSize) {
                NetworkIdentitySet ident = new NetworkIdentitySet(in);
                int size = in.readInt();
                int j = 0;
                while (j < size) {
                    int uid = in.readInt();
                    int set = version >= i ? in.readInt() : 0;
                    int tag = in.readInt();
                    Key key = new Key(ident, uid, set, tag);
                    NetworkStatsHistory history = new NetworkStatsHistory(in);
                    if ((tag == 0) != onlyTags) {
                        recordHistory(key, history);
                    }
                    j++;
                    i = 4;
                }
                i2++;
                i = 4;
            }
        }
        IoUtils.closeQuietly(in);
    }

    public void removeUids(int[] uids) {
        ArrayList<Key> knownKeys = Lists.newArrayList();
        knownKeys.addAll(this.mStats.keySet());
        Iterator<Key> it = knownKeys.iterator();
        while (it.hasNext()) {
            Key key = it.next();
            if (ArrayUtils.contains(uids, key.uid)) {
                if (key.tag == 0) {
                    NetworkStatsHistory uidHistory = this.mStats.get(key);
                    NetworkStatsHistory removedHistory = findOrCreateHistory(key.ident, -4, 0, 0);
                    removedHistory.recordEntireHistory(uidHistory);
                }
                this.mStats.remove(key);
                this.mDirty = true;
            }
        }
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

    private int estimateBuckets() {
        return (int) (Math.min(this.mEndMillis - this.mStartMillis, 3024000000L) / this.mBucketDuration);
    }

    private ArrayList<Key> getSortedKeys() {
        ArrayList<Key> keys = Lists.newArrayList();
        keys.addAll(this.mStats.keySet());
        Collections.sort(keys);
        return keys;
    }

    public void dump(IndentingPrintWriter pw) {
        Iterator<Key> it = getSortedKeys().iterator();
        while (it.hasNext()) {
            Key key = it.next();
            pw.print("ident=");
            pw.print(key.ident.toString());
            pw.print(" uid=");
            pw.print(key.uid);
            pw.print(" set=");
            pw.print(NetworkStats.setToString(key.set));
            pw.print(" tag=");
            pw.println(NetworkStats.tagToString(key.tag));
            NetworkStatsHistory history = this.mStats.get(key);
            pw.increaseIndent();
            history.dump(pw, true);
            pw.decreaseIndent();
        }
    }

    public void dumpDebug(ProtoOutputStream proto, long tag) {
        long start = proto.start(tag);
        Iterator<Key> it = getSortedKeys().iterator();
        while (it.hasNext()) {
            Key key = it.next();
            long startStats = proto.start(2246267895809L);
            long startKey = proto.start(1146756268033L);
            key.ident.dumpDebug(proto, 1146756268033L);
            proto.write(1120986464258L, key.uid);
            proto.write(1120986464259L, key.set);
            proto.write(1120986464260L, key.tag);
            proto.end(startKey);
            NetworkStatsHistory history = this.mStats.get(key);
            history.dumpDebug(proto, 1146756268034L);
            proto.end(startStats);
        }
        proto.end(start);
    }

    public void dumpCheckin(PrintWriter pw, long start, long end) {
        dumpCheckin(pw, start, end, NetworkTemplate.buildTemplateMobileWildcard(), "cell");
        dumpCheckin(pw, start, end, NetworkTemplate.buildTemplateWifiWildcard(), "wifi");
        dumpCheckin(pw, start, end, NetworkTemplate.buildTemplateEthernet(), "eth");
        dumpCheckin(pw, start, end, NetworkTemplate.buildTemplateBluetooth(), "bt");
    }

    private void dumpCheckin(PrintWriter pw, long start, long end, NetworkTemplate groupTemplate, String groupPrefix) {
        NetworkStatsHistory groupHistory;
        ArrayMap<Key, NetworkStatsHistory> grouped = new ArrayMap<>();
        for (int i = 0; i < this.mStats.size(); i++) {
            Key key = this.mStats.keyAt(i);
            NetworkStatsHistory value = this.mStats.valueAt(i);
            if (templateMatches(groupTemplate, key.ident) && key.set < 1000) {
                Key groupKey = new Key(null, key.uid, key.set, key.tag);
                NetworkStatsHistory groupHistory2 = grouped.get(groupKey);
                if (groupHistory2 != null) {
                    groupHistory = groupHistory2;
                } else {
                    NetworkStatsHistory groupHistory3 = new NetworkStatsHistory(value.getBucketDuration());
                    grouped.put(groupKey, groupHistory3);
                    groupHistory = groupHistory3;
                }
                groupHistory.recordHistory(value, start, end);
            }
        }
        for (int i2 = 0; i2 < grouped.size(); i2++) {
            Key key2 = grouped.keyAt(i2);
            NetworkStatsHistory value2 = grouped.valueAt(i2);
            if (value2.size() != 0) {
                pw.print("c,");
                pw.print(groupPrefix);
                pw.print(',');
                pw.print(key2.uid);
                pw.print(',');
                pw.print(NetworkStats.setToCheckinString(key2.set));
                pw.print(',');
                pw.print(key2.tag);
                pw.println();
                value2.dumpCheckin(pw);
            }
        }
    }

    private static boolean templateMatches(NetworkTemplate template, NetworkIdentitySet identSet) {
        Iterator<NetworkIdentity> it = identSet.iterator();
        while (it.hasNext()) {
            NetworkIdentity ident = it.next();
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

        public Key(NetworkIdentitySet ident, int uid, int set, int tag) {
            this.ident = ident;
            this.uid = uid;
            this.set = set;
            this.tag = tag;
            this.hashCode = Objects.hash(ident, Integer.valueOf(uid), Integer.valueOf(set), Integer.valueOf(tag));
        }

        public int hashCode() {
            return this.hashCode;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Key) {
                Key key = (Key) obj;
                return this.uid == key.uid && this.set == key.set && this.tag == key.tag && Objects.equals(this.ident, key.ident);
            }
            return false;
        }

        @Override // java.lang.Comparable
        public int compareTo(Key another) {
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
                return Integer.compare(this.tag, another.tag);
            }
            return res;
        }
    }
}