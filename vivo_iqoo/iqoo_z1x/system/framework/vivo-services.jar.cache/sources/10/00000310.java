package com.android.server.net;

import android.net.VivoNetworkStats;
import android.net.VivoNetworkStatsImpl;
import android.os.DropBoxManager;
import android.util.MathUtils;
import com.android.internal.util.FileRotator;
import com.android.internal.util.Preconditions;
import com.google.android.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import libcore.io.IoUtils;

/* loaded from: classes.dex */
public class VivoNetworkStatsRecorder {
    private static final boolean DUMP_BEFORE_DELETE = true;
    private final long mBucketDuration;
    private WeakReference<VivoNetworkStatsCollection> mComplete;
    private final String mCookie;
    private VivoNetworkStats mLastSnapshot;
    private final boolean mOnlyTags;
    private final VivoNetworkStatsCollection mPending;
    private final CombiningRewriter mPendingRewriter;
    private long mPersistThresholdBytes;
    private final FileRotator mRotator;
    private final VivoNetworkStatsCollection mSinceBoot;
    public int mVivoInfoIndicator;

    public VivoNetworkStatsRecorder(int vivoInfoIndicator) {
        this.mPersistThresholdBytes = 2097152L;
        this.mVivoInfoIndicator = 0;
        this.mRotator = null;
        this.mCookie = null;
        this.mBucketDuration = 31449600000L;
        this.mOnlyTags = false;
        this.mVivoInfoIndicator = vivoInfoIndicator;
        this.mPending = null;
        this.mSinceBoot = new VivoNetworkStatsCollection(31449600000L, vivoInfoIndicator);
        this.mPendingRewriter = null;
    }

    public VivoNetworkStatsRecorder(FileRotator rotator, VivoNetworkStats.NonMonotonicObserver<String> observer, DropBoxManager dropBox, String cookie, long bucketDuration, boolean onlyTags, int vivoInfoIndicator) {
        this.mPersistThresholdBytes = 2097152L;
        this.mVivoInfoIndicator = 0;
        this.mRotator = (FileRotator) Preconditions.checkNotNull(rotator, "missing FileRotator");
        this.mCookie = cookie;
        this.mBucketDuration = bucketDuration;
        this.mOnlyTags = onlyTags;
        this.mVivoInfoIndicator = vivoInfoIndicator;
        this.mPending = new VivoNetworkStatsCollection(bucketDuration, vivoInfoIndicator);
        this.mSinceBoot = new VivoNetworkStatsCollection(bucketDuration, this.mVivoInfoIndicator);
        this.mPendingRewriter = new CombiningRewriter(this.mPending);
    }

    public void setPersistThreshold(long thresholdBytes) {
        this.mPersistThresholdBytes = MathUtils.constrain(thresholdBytes, 1024L, 104857600L);
    }

    public VivoNetworkStatsCollection getOrLoadCompleteLocked() {
        Preconditions.checkNotNull(this.mRotator, "missing FileRotator");
        WeakReference<VivoNetworkStatsCollection> weakReference = this.mComplete;
        VivoNetworkStatsCollection res = weakReference != null ? weakReference.get() : null;
        if (res == null) {
            VivoNetworkStatsCollection res2 = loadLocked(Long.MIN_VALUE, Long.MAX_VALUE);
            this.mComplete = new WeakReference<>(res2);
            return res2;
        }
        return res;
    }

    public VivoNetworkStatsCollection getOrLoadPartialLocked(long start, long end) {
        Preconditions.checkNotNull(this.mRotator, "missing FileRotator");
        WeakReference<VivoNetworkStatsCollection> weakReference = this.mComplete;
        VivoNetworkStatsCollection res = weakReference != null ? weakReference.get() : null;
        if (res == null) {
            return loadLocked(start, end);
        }
        return res;
    }

    private VivoNetworkStatsCollection loadLocked(long start, long end) {
        VivoNetworkStatsCollection res = new VivoNetworkStatsCollection(this.mBucketDuration, this.mVivoInfoIndicator);
        try {
            this.mRotator.readMatching(res, start, end);
            res.recordCollection(this.mPending);
        } catch (IOException e) {
            VivoNetworkStatsServiceImpl.log(1, "tag[" + this.mOnlyTags + "] problem completely reading network stats " + e);
            recoverFromWtf();
        } catch (OutOfMemoryError e2) {
            VivoNetworkStatsServiceImpl.log(1, "tag[" + this.mOnlyTags + "] problem completely reading network stats " + e2);
            recoverFromWtf();
        }
        return res;
    }

    public void recordSnapshotLocked(VivoNetworkStats snapshot, Map<String, NetworkIdentitySet> ifaceIdent, long currentTimeMillis) {
        String str;
        String str2;
        VivoNetworkStatsCollection complete;
        int i;
        long start;
        long end;
        String str3;
        String str4;
        char c;
        HashSet<String> unknownIfaces;
        VivoNetworkStatsImpl.VivoEntryImpl vivoEntryImpl;
        String str5;
        if (snapshot == null) {
            return;
        }
        if (this.mLastSnapshot == null) {
            this.mLastSnapshot = snapshot;
            VivoNetworkStatsServiceImpl.logNetworkStats(3, "tag[" + this.mOnlyTags + "] first Snapshot", this.mLastSnapshot);
            return;
        }
        HashSet<String> unknownIfaces2 = Sets.newHashSet();
        WeakReference<VivoNetworkStatsCollection> weakReference = this.mComplete;
        VivoNetworkStatsCollection complete2 = weakReference != null ? weakReference.get() : null;
        VivoNetworkStatsServiceImpl.logNetworkStats(4, "tag[" + this.mOnlyTags + "] lastestSnapshot", this.mLastSnapshot);
        VivoNetworkStatsServiceImpl.logNetworkStats(4, "tag[" + this.mOnlyTags + "] currentSnapshot", snapshot);
        VivoNetworkStats delta = VivoNetworkStats.subtract(snapshot, this.mLastSnapshot, (VivoNetworkStats.NonMonotonicObserver) null, (Object) null);
        long end2 = currentTimeMillis;
        long start2 = end2 - delta.getElapsedRealtime();
        StringBuilder sb = new StringBuilder();
        sb.append("tag[");
        sb.append(this.mOnlyTags);
        sb.append("] begin: record delta : start[");
        sb.append(start2);
        String str6 = "], end[";
        sb.append("], end[");
        sb.append(end2);
        String str7 = "]";
        sb.append("]");
        VivoNetworkStatsServiceImpl.log(3, sb.toString());
        VivoNetworkStatsServiceImpl.logNetworkStats(3, "tag[" + this.mOnlyTags + "] record delta : start[" + start2 + "], end[" + end2 + "]", delta);
        VivoNetworkStats.Entry entry = null;
        int i2 = 0;
        while (i2 < delta.size()) {
            VivoNetworkStats.Entry entry2 = delta.getValues(i2, entry);
            StringBuilder sb2 = new StringBuilder();
            sb2.append("tag[");
            sb2.append(this.mOnlyTags);
            sb2.append("] record [");
            sb2.append(i2);
            VivoNetworkStats delta2 = delta;
            sb2.append("] = ");
            sb2.append(entry2);
            VivoNetworkStatsServiceImpl.log(4, sb2.toString());
            if (!entry2.isNegative()) {
                str = str7;
                str2 = str6;
            } else {
                str = str7;
                str2 = str6;
                entry2.rxBytes = Math.max(entry2.rxBytes, 0L);
                entry2.rxPackets = Math.max(entry2.rxPackets, 0L);
                entry2.txBytes = Math.max(entry2.txBytes, 0L);
                entry2.txPackets = Math.max(entry2.txPackets, 0L);
                entry2.operations = Math.max(entry2.operations, 0L);
            }
            NetworkIdentitySet ident = ifaceIdent.get(entry2.iface);
            if (ident == null) {
                VivoNetworkStatsServiceImpl.log(3, "tag[" + this.mOnlyTags + "] record [" + i2 + "] unknown iface = " + entry2.iface);
                unknownIfaces2.add(entry2.iface);
                complete = complete2;
                i = i2;
                start = start2;
                end = end2;
                str3 = str2;
                str4 = str;
                c = 4;
                unknownIfaces = unknownIfaces2;
            } else if (entry2.isEmpty()) {
                VivoNetworkStatsServiceImpl.log(3, "tag[" + this.mOnlyTags + "] record [" + i2 + "] empty entry");
                complete = complete2;
                i = i2;
                start = start2;
                end = end2;
                str3 = str2;
                str4 = str;
                c = 4;
                unknownIfaces = unknownIfaces2;
            } else if (entry2.mVivoEntry == null) {
                complete = complete2;
                i = i2;
                start = start2;
                end = end2;
                str3 = str2;
                str4 = str;
                c = 4;
                unknownIfaces = unknownIfaces2;
            } else {
                VivoNetworkStatsImpl.VivoEntryImpl vivoEntryImpl2 = entry2.mVivoEntry;
                if (!(vivoEntryImpl2 instanceof VivoNetworkStatsImpl.VivoEntryImpl)) {
                    complete = complete2;
                    i = i2;
                    start = start2;
                    end = end2;
                    str3 = str2;
                    str4 = str;
                    c = 4;
                    unknownIfaces = unknownIfaces2;
                } else {
                    VivoNetworkStatsImpl.VivoEntryImpl entryImpl = vivoEntryImpl2;
                    if ((entry2.tag == 0) == this.mOnlyTags) {
                        complete = complete2;
                        i = i2;
                        start = start2;
                        end = end2;
                        str3 = str2;
                        str4 = str;
                        c = 4;
                        unknownIfaces = unknownIfaces2;
                    } else {
                        VivoNetworkStatsCollection vivoNetworkStatsCollection = this.mPending;
                        if (vivoNetworkStatsCollection == null) {
                            vivoEntryImpl = vivoEntryImpl2;
                            str5 = str;
                        } else {
                            vivoEntryImpl = vivoEntryImpl2;
                            vivoNetworkStatsCollection.recordData(ident, entryImpl, entry2.uid, entry2.set, entry2.tag, start2, end2, entry2);
                            StringBuilder sb3 = new StringBuilder();
                            sb3.append("tag[");
                            sb3.append(this.mOnlyTags);
                            sb3.append("] record pending [");
                            sb3.append(i2);
                            str5 = str;
                            sb3.append(str5);
                            VivoNetworkStatsServiceImpl.log(4, sb3.toString());
                        }
                        VivoNetworkStatsCollection vivoNetworkStatsCollection2 = this.mSinceBoot;
                        if (vivoNetworkStatsCollection2 == null) {
                            unknownIfaces = unknownIfaces2;
                        } else {
                            unknownIfaces = unknownIfaces2;
                            vivoNetworkStatsCollection2.recordData(ident, entryImpl, entry2.uid, entry2.set, entry2.tag, start2, end2, entry2);
                            VivoNetworkStatsServiceImpl.log(4, "tag[" + this.mOnlyTags + "] record sinceboot [" + i2 + str5);
                        }
                        if (complete2 == null) {
                            complete = complete2;
                            i = i2;
                            str4 = str5;
                            start = start2;
                            end = end2;
                            str3 = str2;
                            c = 4;
                        } else {
                            VivoNetworkStatsServiceImpl.log(4, "tag[" + this.mOnlyTags + "] record complete [" + i2 + str5);
                            VivoNetworkStatsCollection vivoNetworkStatsCollection3 = complete2;
                            i = i2;
                            complete = complete2;
                            str4 = str5;
                            str3 = str2;
                            start = start2;
                            end = end2;
                            c = 4;
                            vivoNetworkStatsCollection3.recordData(ident, entryImpl, entry2.uid, entry2.set, entry2.tag, start, end, entry2);
                        }
                    }
                }
            }
            i2 = i + 1;
            entry = entry2;
            str7 = str4;
            str6 = str3;
            complete2 = complete;
            start2 = start;
            end2 = end;
            delta = delta2;
            unknownIfaces2 = unknownIfaces;
        }
        HashSet<String> unknownIfaces3 = unknownIfaces2;
        String str8 = str7;
        String str9 = str6;
        long start3 = start2;
        long end3 = end2;
        this.mLastSnapshot = snapshot;
        if (unknownIfaces3.size() > 0) {
            VivoNetworkStatsServiceImpl.log(1, "tag[" + this.mOnlyTags + "] ignoring unknown interfaces " + unknownIfaces3);
        }
        VivoNetworkStatsServiceImpl.log(3, "tag[" + this.mOnlyTags + "] end: record delta : start[" + start3 + str9 + end3 + str8);
    }

    public void maybePersistLocked(long currentTimeMillis) {
        Preconditions.checkNotNull(this.mRotator, "missing FileRotator");
        long pendingBytes = this.mPending.getTotalBytes();
        if (pendingBytes >= this.mPersistThresholdBytes) {
            forcePersistLocked(currentTimeMillis);
        } else {
            this.mRotator.maybeRotate(currentTimeMillis);
        }
    }

    public void forcePersistLocked(long currentTimeMillis) {
        Preconditions.checkNotNull(this.mRotator, "missing FileRotator");
        if (this.mPending.isDirty()) {
            try {
                this.mRotator.rewriteActive(this.mPendingRewriter, currentTimeMillis);
                this.mRotator.maybeRotate(currentTimeMillis);
                this.mPending.reset();
            } catch (IOException e) {
                VivoNetworkStatsServiceImpl.log(1, "tag[" + this.mOnlyTags + "] problem persisting pending stats " + e);
                recoverFromWtf();
            } catch (OutOfMemoryError e2) {
                VivoNetworkStatsServiceImpl.log(1, "tag[" + this.mOnlyTags + "] problem persisting pending stats " + e2);
                recoverFromWtf();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class CombiningRewriter implements FileRotator.Rewriter {
        private final VivoNetworkStatsCollection mCollection;

        public CombiningRewriter(VivoNetworkStatsCollection collection) {
            this.mCollection = (VivoNetworkStatsCollection) Preconditions.checkNotNull(collection, "missing NetworkStatsCollection");
        }

        public void reset() {
        }

        public void read(InputStream in) throws IOException {
            this.mCollection.read(in);
        }

        public boolean shouldWrite() {
            return true;
        }

        public void write(OutputStream out) throws IOException {
            this.mCollection.write(new DataOutputStream(out));
            this.mCollection.reset();
        }
    }

    private void recoverFromWtf() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            try {
                this.mRotator.dumpAll(os);
            } catch (IOException e) {
                os.reset();
            }
            this.mRotator.deleteAll();
        } finally {
            IoUtils.closeQuietly(os);
        }
    }
}