package com.android.server.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.INetd;
import android.net.IVivoNetworkStatsEntry;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.VivoNetworkStats;
import android.net.VivoNetworkStatsImpl;
import android.net.util.NetdService;
import android.os.Binder;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.SubscriptionPlan;
import android.util.ArrayMap;
import android.util.FtFeature;
import android.util.IntArray;
import com.android.internal.net.IOemNetd;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FileRotator;
import com.android.internal.util.ProcFileReader;
import com.android.server.NetworkManagementSocketTagger;
import com.android.server.net.NetworkStatsService;
import com.vivo.services.proxy.ProxyConfigs;
import java.io.File;
import java.io.FileInputStream;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import libcore.io.IoUtils;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoNetworkStatsServiceImpl implements IVivoNetworkStatsService {
    public static final int FEATURE_CALLED = 2;
    public static final int FEATURE_ERR = 1;
    public static final int FEATURE_IND = 0;
    private static final int FLAG_PERSIST_FORCE = 256;
    private static final String PREFIX_PACK = "pack";
    private static final String PREFIX_PACK_TAG = "pack_tag";
    private static final String PREFIX_RAT = "rat";
    private static final String PREFIX_RAT_TAG = "rat_tag";
    public static final int PROCESS_DETAIL = 4;
    public static final int PROCESS_SIGN = 3;
    private static final String SUPPORT_FEATURE = "vivo.software.datatrafficsubdivision";
    public static final String TAG = "VivoNetworkStatsService";
    private List<ApplicationInfo> installedAPPS;
    private final File mBaseDir;
    private final Clock mClock;
    private final Context mContext;
    private final boolean mIsTrafficMegSupport;
    private final boolean mIsTrafficRatSupport;
    private final boolean mIsTrafficSubSupport;
    private INetd mNetdService;
    private IOemNetd mOemNetdService;
    private VivoNetworkStatsRecorder mPackInfoRecorder;
    private VivoNetworkStatsRecorder mPackInfoTagRecorder;
    private final PackageManager mPackageManager;
    private VivoNetworkStatsRecorder mRatInfoRecorder;
    private VivoNetworkStatsRecorder mRatInfoTagRecorder;
    private final NetworkStatsService mService;
    private final NetworkStatsService.NetworkStatsSettings mSettings;
    private static final int LOGABLE_LEVEL = SystemProperties.getInt("persist.vivo.net_stats_debug_level", 0);
    private static final String[] mSeparPackages = {"com.vivo.hybrid"};
    private static final String[] mMergedPackages = {"com.vivo.abe", ProxyConfigs.CTRL_MODULE_PEM, "com.vivo.aiengine", "com.vivo.daemonService", "com.vivo.sdkplugin", "com.mobile.cos.iroaming", "com.android.phone", "com.android.wifisettings", "com.android.packageinstaller", "com.vivo.sos", "com.vivo.agent", "com.vivo.vtouch", "com.vivo.voicewakeup", "com.vivo.networkimprove"};
    private final Object mVivoStatsLock = new Object();
    private final ConcurrentHashMap<String, String> mStackedIfaces = new ConcurrentHashMap<>();
    private long mPersistThreshold = 2097152;
    private Set<Integer> mSeparUids = new HashSet();
    private Object mSeparUidsLock = new Object();
    private int mDisplayUid = 1000;
    private Set<Integer> mMergedUids = new HashSet();
    private boolean mMergedUidsInited = false;
    private BroadcastReceiver mPackageChangeReceiver = new BroadcastReceiver() { // from class: com.android.server.net.VivoNetworkStatsServiceImpl.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            VivoNetworkStatsServiceImpl.log(3, "package changed, update install apps");
            VivoNetworkStatsServiceImpl vivoNetworkStatsServiceImpl = VivoNetworkStatsServiceImpl.this;
            vivoNetworkStatsServiceImpl.installedAPPS = vivoNetworkStatsServiceImpl.mPackageManager.getInstalledApplications(0);
        }
    };
    private final File mStatsXtPid = new File("/proc/net/xt_qtaguid/stats_pid");
    private final boolean mUseBpfStats = new File("/sys/fs/bpf/map_netd_app_uid_stats_map").exists();

    private native int nativeReadPackinfoNetworkStatsDetail(VivoNetworkStats vivoNetworkStats, String str, int i, String[] strArr, String[] strArr2, int i2, boolean z);

    private native int nativeReadRatinfoNetworkStatsDetail(VivoNetworkStats vivoNetworkStats, String str, int i, String[] strArr, int i2, int i3, boolean z);

    public VivoNetworkStatsServiceImpl(NetworkStatsService networkStatsService) {
        this.mService = networkStatsService;
        this.mContext = networkStatsService.getContext();
        this.mSettings = this.mService.getNetworkStatsSettings();
        this.mClock = this.mService.getClock();
        this.mBaseDir = this.mService.getBaseDir();
        try {
            INetd iNetd = NetdService.get();
            this.mNetdService = iNetd;
            this.mOemNetdService = IOemNetd.Stub.asInterface(iNetd.getOemNetd());
        } catch (Exception e) {
            e.printStackTrace();
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        this.mPackageManager = packageManager;
        this.installedAPPS = packageManager.getInstalledApplications(0);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        this.mContext.registerReceiver(this.mPackageChangeReceiver, filter);
        this.mIsTrafficSubSupport = FtFeature.isFeatureSupport(SUPPORT_FEATURE);
        log(0, "is traffic sub support = " + this.mIsTrafficSubSupport);
        this.mIsTrafficRatSupport = FtFeature.isFeatureSupport("vivo.software.datatrafficratdivision");
        log(0, "is traffic rat support = " + this.mIsTrafficRatSupport);
        this.mIsTrafficMegSupport = FtFeature.isFeatureSupport("vivo.software.datatrafficmerged");
        log(0, "is traffic meg support = " + this.mIsTrafficMegSupport);
        if (this.mIsTrafficSubSupport) {
            updateSeparateStatsUids();
        }
    }

    public void noteStackedIface(String stackedIface, String baseIface) {
        if (stackedIface != null && baseIface != null) {
            try {
                this.mStackedIfaces.put(stackedIface, baseIface);
            } catch (Exception e) {
                log(1, "problem during noteStackedIface " + e);
            }
        }
    }

    public void initVivoRecorder() {
        try {
            synchronized (this.mVivoStatsLock) {
                this.mPackInfoRecorder = buildVivoRecorder(PREFIX_PACK, this.mSettings.getUidConfig(), false, 1);
                this.mPackInfoTagRecorder = buildVivoRecorder(PREFIX_PACK_TAG, this.mSettings.getUidTagConfig(), true, 1);
                this.mRatInfoRecorder = buildVivoRecorder(PREFIX_RAT, this.mSettings.getUidConfig(), false, 2);
                this.mRatInfoTagRecorder = buildVivoRecorder(PREFIX_RAT_TAG, this.mSettings.getUidTagConfig(), true, 2);
            }
        } catch (Exception e) {
            log(1, "problem during initVivoRecorder " + e);
        }
    }

    public void shutdownVivoRecorder() {
        try {
            synchronized (this.mVivoStatsLock) {
                long currentTime = this.mClock.millis();
                this.mPackInfoRecorder.forcePersistLocked(currentTime);
                this.mPackInfoTagRecorder.forcePersistLocked(currentTime);
                this.mRatInfoRecorder.forcePersistLocked(currentTime);
                this.mRatInfoTagRecorder.forcePersistLocked(currentTime);
            }
        } catch (Exception e) {
            log(1, "problem during shutdownVivoRecorder " + e);
        }
    }

    public void updateVivoPersistThresholds() {
        try {
            synchronized (this.mVivoStatsLock) {
                this.mPackInfoRecorder.setPersistThreshold(this.mSettings.getUidPersistBytes(this.mPersistThreshold));
                this.mPackInfoTagRecorder.setPersistThreshold(this.mSettings.getUidPersistBytes(this.mPersistThreshold));
                this.mRatInfoRecorder.setPersistThreshold(this.mSettings.getUidPersistBytes(this.mPersistThreshold));
                this.mRatInfoTagRecorder.setPersistThreshold(this.mSettings.getUidPersistBytes(this.mPersistThreshold));
            }
        } catch (Exception e) {
            log(1, "problem during updateVivoPersistThresholds " + e);
        }
    }

    public void recordVivoSnapshot(ArrayMap<String, NetworkIdentitySet> activeUidIfaces, long currentTime) {
        try {
            synchronized (this.mVivoStatsLock) {
                if (this.mIsTrafficSubSupport) {
                    recordPackinfoSnapshotLocked(activeUidIfaces, currentTime);
                }
                if (this.mIsTrafficRatSupport) {
                    recordRatinfoSnapshotLocked(activeUidIfaces, currentTime);
                }
            }
        } catch (Exception e) {
            log(1, "problem during recordVivoSnapshot " + e);
        }
    }

    public void performPollVivo(int flags) {
        try {
            synchronized (this.mVivoStatsLock) {
                boolean persistForce = (flags & 256) != 0;
                long currentTime = this.mClock.millis();
                if (persistForce) {
                    if (this.mIsTrafficSubSupport) {
                        this.mPackInfoRecorder.forcePersistLocked(currentTime);
                        this.mPackInfoTagRecorder.forcePersistLocked(currentTime);
                    }
                    if (this.mIsTrafficRatSupport) {
                        this.mRatInfoRecorder.forcePersistLocked(currentTime);
                        this.mRatInfoTagRecorder.forcePersistLocked(currentTime);
                    }
                } else {
                    if (this.mIsTrafficSubSupport) {
                        this.mPackInfoRecorder.maybePersistLocked(currentTime);
                        this.mPackInfoTagRecorder.maybePersistLocked(currentTime);
                    }
                    if (this.mIsTrafficRatSupport) {
                        this.mRatInfoRecorder.maybePersistLocked(currentTime);
                        this.mRatInfoTagRecorder.maybePersistLocked(currentTime);
                    }
                }
            }
        } catch (Exception e) {
            log(1, "problem during performPollVivo " + e);
        }
    }

    private void recordPackinfoSnapshotLocked(ArrayMap<String, NetworkIdentitySet> activeUidIfaces, long currentTime) {
        VivoNetworkStats uidSnapshot = readPackInfoNetworkStatsDetail(-1, null, -1);
        if (uidSnapshot == null) {
            log(3, "null packinfo snapshot");
            return;
        }
        VivoNetworkStats detailSnapshot = new VivoNetworkStats(uidSnapshot.getElapsedRealtime(), uidSnapshot.internalSize());
        detailSnapshot.syncVivoInfoIndicatorFrom(uidSnapshot);
        if (this.installedAPPS == null) {
            this.installedAPPS = this.mPackageManager.getInstalledApplications(0);
        }
        VivoNetworkStats.Entry entry = null;
        for (int i = 0; i < uidSnapshot.size(); i++) {
            entry = uidSnapshot.getValues(i, entry);
            if (entry.mVivoEntry != null) {
                VivoNetworkStatsImpl.VivoEntryImpl vivoEntryImpl = entry.mVivoEntry;
                if (vivoEntryImpl instanceof VivoNetworkStatsImpl.VivoEntryImpl) {
                    VivoNetworkStatsImpl.VivoEntryImpl entryImpl = vivoEntryImpl;
                    entryImpl.packInfo = getFullPackInfo(this.installedAPPS, entryImpl.packInfo, 1000 == entry.uid);
                }
            }
            detailSnapshot.combineValues(entry);
        }
        logNetworkStats(3, "full packinfo Snapshot", detailSnapshot);
        this.mPackInfoRecorder.recordSnapshotLocked(detailSnapshot, activeUidIfaces, currentTime);
        this.mPackInfoTagRecorder.recordSnapshotLocked(detailSnapshot, activeUidIfaces, currentTime);
    }

    private void recordRatinfoSnapshotLocked(ArrayMap<String, NetworkIdentitySet> activeUidIfaces, long currentTime) {
        VivoNetworkStats uidSnapshot = readRatInfoNetworkStatsDetail(-1, null, -1, -1);
        if (uidSnapshot == null) {
            log(3, "null ratinfo snapshot");
            return;
        }
        logNetworkStats(3, "full ratinfo Snapshot", uidSnapshot);
        this.mRatInfoRecorder.recordSnapshotLocked(uidSnapshot, activeUidIfaces, currentTime);
        this.mRatInfoTagRecorder.recordSnapshotLocked(uidSnapshot, activeUidIfaces, currentTime);
    }

    private VivoNetworkStatsRecorder buildVivoRecorder(String prefix, NetworkStatsService.NetworkStatsSettings.Config config, boolean includeTags, int vivoInfoIndicator) {
        return new VivoNetworkStatsRecorder(new FileRotator(this.mBaseDir, prefix, config.rotateAgeMillis, config.deleteAgeMillis), null, null, prefix, config.bucketDuration, includeTags, vivoInfoIndicator);
    }

    private int checkAccessLevel(String callingPackage) {
        return NetworkStatsAccess.checkAccessLevel(this.mContext, Binder.getCallingUid(), callingPackage);
    }

    private VivoNetworkStats readPackInfoNetworkStatsDetail(int limitUid, String[] limitIfaces, int limitTag) {
        VivoNetworkStats stats = null;
        try {
            stats = javaReadPackInfoNetworkStatsDetail(this.mStatsXtPid, limitUid, limitIfaces, limitTag);
            stats.apply464xlatAdjustments(this.mStackedIfaces);
            return stats;
        } catch (Exception e) {
            return stats;
        }
    }

    private VivoNetworkStats javaReadPackInfoNetworkStatsDetail(File detailPath, int limitUid, String[] limitIfaces, int limitTag) {
        VivoNetworkStats stats = new VivoNetworkStats(SystemClock.elapsedRealtime(), 24);
        stats.setVivoInfoIndicator(1);
        if (this.mUseBpfStats) {
            nativeReadPackinfoNetworkStatsDetail(stats, null, limitUid, null, limitIfaces, limitTag, true);
        } else {
            javaReadPackInfoNetworkStatsDetailFromProcFile(stats, detailPath, limitUid, limitIfaces, limitTag);
        }
        logNetworkStats(4, "raw packinfo Snapshot", stats);
        return stats;
    }

    private void javaReadPackInfoNetworkStatsDetailFromProcFile(VivoNetworkStats stats, File detailPath, int limitUid, String[] limitIfaces, int limitTag) {
        VivoNetworkStats.Entry entry = new VivoNetworkStats.Entry();
        stats.syncVivoInfoIndicatorTo(entry);
        VivoNetworkStatsImpl.VivoEntryImpl entryImpl = null;
        if (entry.mVivoEntry != null) {
            IVivoNetworkStatsEntry vivoEntry = entry.mVivoEntry;
            if (vivoEntry instanceof VivoNetworkStatsImpl.VivoEntryImpl) {
                entryImpl = (VivoNetworkStatsImpl.VivoEntryImpl) vivoEntry;
            }
        }
        if (entryImpl == null) {
            return;
        }
        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        ProcFileReader reader = null;
        try {
            try {
                reader = new ProcFileReader(new FileInputStream(detailPath));
                reader.finishLine();
                int lastIdx = 1;
                while (reader.hasMoreData()) {
                    int idx = reader.nextInt();
                    if (idx != lastIdx + 1) {
                        throw new Exception("inconsistent idx=" + idx + " after lastIdx=" + lastIdx);
                    }
                    lastIdx = idx;
                    entry.iface = reader.nextString();
                    entryImpl.packInfo = reader.nextString();
                    entryImpl.ratInfo = 0;
                    entry.tag = NetworkManagementSocketTagger.kernelToTag(reader.nextString());
                    entry.uid = reader.nextInt();
                    entry.set = reader.nextInt();
                    entry.rxBytes = reader.nextLong();
                    entry.rxPackets = reader.nextLong();
                    entry.txBytes = reader.nextLong();
                    entry.txPackets = reader.nextLong();
                    if ((limitIfaces == null || ArrayUtils.contains(limitIfaces, entry.iface)) && ((limitUid == -1 || limitUid == entry.uid) && (limitTag == -1 || limitTag == entry.tag))) {
                        stats.insertEntry(entry);
                    }
                    reader.finishLine();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            IoUtils.closeQuietly(reader);
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    private VivoNetworkStats readRatInfoNetworkStatsDetail(int limitUid, String[] limitIfaces, int limitTag, int limitRat) {
        VivoNetworkStats stats = null;
        try {
            stats = javaReadRatInfoNetworkStatsDetail(this.mStatsXtPid, limitUid, limitIfaces, limitTag, limitRat);
            stats.apply464xlatAdjustments(this.mStackedIfaces);
            return stats;
        } catch (Exception e) {
            return stats;
        }
    }

    private VivoNetworkStats javaReadRatInfoNetworkStatsDetail(File detailPath, int limitUid, String[] limitIfaces, int limitTag, int limitRat) {
        VivoNetworkStats stats = new VivoNetworkStats(SystemClock.elapsedRealtime(), 24);
        stats.setVivoInfoIndicator(2);
        nativeReadRatinfoNetworkStatsDetail(stats, null, limitUid, limitIfaces, limitTag, limitRat, true);
        logNetworkStats(4, "raw ratinfo Snapshot", stats);
        return stats;
    }

    private String getFullPackInfo(List<ApplicationInfo> source, String targetInfo, boolean needMerge) {
        if (source != null && targetInfo != null) {
            for (ApplicationInfo current : source) {
                if ((current.flags & 1) > 0) {
                    if (needMerge && targetInfo.contains(":")) {
                        String targetTmp = targetInfo.substring(0, targetInfo.indexOf(":"));
                        if (current.packageName != null && current.packageName.contains(targetTmp)) {
                            return current.packageName;
                        }
                    } else if (current.packageName != null && current.packageName.endsWith(targetInfo)) {
                        return current.packageName;
                    }
                }
            }
        }
        return targetInfo;
    }

    public void updateMergedDisplayUids() {
        if (this.mIsTrafficMegSupport) {
            try {
                synchronized (this.mMergedUids) {
                    if (this.mMergedUidsInited) {
                        log(3, "updateMergedDisplayUids Mergeduids inited");
                        return;
                    }
                    this.mMergedUids.add(Integer.valueOf(this.mDisplayUid));
                    if (this.installedAPPS == null) {
                        this.installedAPPS = this.mPackageManager.getInstalledApplications(0);
                    }
                    StringBuilder mergedUidsSB = new StringBuilder("updateMergedDisplayUids Mergeduids = [");
                    for (ApplicationInfo current : this.installedAPPS) {
                        if (current.packageName != null) {
                            String[] strArr = mMergedPackages;
                            int length = strArr.length;
                            int i = 0;
                            while (true) {
                                if (i < length) {
                                    String target = strArr[i];
                                    if (!current.packageName.equals(target)) {
                                        i++;
                                    } else {
                                        this.mMergedUids.add(Integer.valueOf(current.uid));
                                        mergedUidsSB.append(target);
                                        mergedUidsSB.append(" = ");
                                        mergedUidsSB.append(current.uid);
                                        mergedUidsSB.append(";");
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    mergedUidsSB.append("]");
                    log(3, mergedUidsSB.toString());
                    this.mMergedUidsInited = true;
                }
            } catch (Exception e) {
                log(1, "problem during updateMergedDisplayUids " + e);
            }
        }
    }

    public int[] getSeparateUids() {
        int[] array;
        synchronized (this.mSeparUidsLock) {
            IntArray uids = new IntArray();
            if (this.mIsTrafficSubSupport) {
                for (Integer uid : this.mSeparUids) {
                    uids.add(uid.intValue());
                }
            }
            array = uids.toArray();
        }
        return array;
    }

    public void updateIfaceRatinfo(String ifaceName, int rat) {
        if (this.mIsTrafficRatSupport) {
            if (ifaceName != null && !ifaceName.isEmpty()) {
                try {
                    this.mOemNetdService.updateIfaceRatinfo(ifaceName, rat);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
            log(1, "updateIfaceRatinfo empty ifacename");
        }
    }

    public long getUidStats(int uid, int type) {
        Set<Integer> set;
        if (this.mIsTrafficMegSupport) {
            updateMergedDisplayUids();
            int i = this.mDisplayUid;
            if (-1 != i && (set = this.mMergedUids) != null) {
                if (uid == i) {
                    long displayStats = this.mService.getRawUidStat(i, type);
                    for (Integer num : this.mMergedUids) {
                        int mergedUid = num.intValue();
                        if (mergedUid != this.mDisplayUid) {
                            long mergedStats = this.mService.getRawUidStat(mergedUid, type);
                            displayStats += mergedStats;
                        }
                    }
                    return displayStats;
                } else if (set.contains(Integer.valueOf(uid))) {
                    return 0L;
                }
            }
        }
        return this.mService.getRawUidStat(uid, type);
    }

    public NetworkStatsHistory getHistoryForPack(String callingPackage, int callingUid, NetworkTemplate template, String packInfo, int uid, int set, int tag, int fields) {
        NetworkStatsHistory result = null;
        if (this.mIsTrafficSubSupport) {
            int accessLevel = checkAccessLevel(callingPackage);
            VivoNetworkStatsImpl.VivoEntryImpl vivoEntry = new VivoNetworkStatsImpl.VivoEntryImpl((VivoNetworkStats.Entry) null);
            vivoEntry.mVivoInfoIndicator = 1;
            vivoEntry.packInfo = packInfo;
            vivoEntry.ratInfo = -1;
            if (tag == 0) {
                result = getPackComplete().getHistory(template, null, vivoEntry, uid, set, tag, fields, Long.MIN_VALUE, Long.MAX_VALUE, accessLevel, callingUid);
            } else {
                result = getPackTagComplete().getHistory(template, null, vivoEntry, uid, set, tag, fields, Long.MIN_VALUE, Long.MAX_VALUE, accessLevel, callingUid);
            }
        }
        log(2, "getHistoryForPack: cUid[" + callingUid + "] cPack[" + callingPackage + "] tUid[" + uid + "] tPack[" + packInfo + "]" + result);
        return result;
    }

    public VivoNetworkStats getSummaryForAllPack(String callingPackage, int callingUid, NetworkTemplate template, long start, long end, boolean includeTags) {
        VivoNetworkStats stats = null;
        if (this.mIsTrafficSubSupport) {
            int accessLevel = checkAccessLevel(callingPackage);
            stats = getPackComplete().getSummary(template, start, end, accessLevel, callingUid);
            if (includeTags) {
                VivoNetworkStats tagStats = getPackTagComplete().getSummary(template, start, end, accessLevel, callingUid);
                stats.combineAllValues(tagStats);
            }
        }
        logNetworkStats(2, "getSummaryForAllPack : cUid[" + callingUid + "] cPack[" + callingPackage + "]", stats);
        return stats;
    }

    public VivoNetworkStats getSummaryForAllRat(String callingPackage, int callingUid, NetworkTemplate template, long start, long end, boolean includeTags) {
        VivoNetworkStats stats = null;
        if (this.mIsTrafficRatSupport) {
            int accessLevel = checkAccessLevel(callingPackage);
            stats = getRatComplete().getSummary(template, start, end, accessLevel, callingUid);
            if (includeTags) {
                VivoNetworkStats tagStats = getRatTagComplete().getSummary(template, start, end, accessLevel, callingUid);
                stats.combineAllValues(tagStats);
            }
        }
        logNetworkStats(2, "getSummaryForAllRat : cUid[" + callingUid + "] cPack[" + callingPackage + "]", stats);
        return stats;
    }

    public NetworkStats changeToMergedStats(String callingPackage, int callingUid, NetworkStats target) {
        NetworkStats mergedStats;
        if (target == null) {
            return null;
        }
        if (this.mIsTrafficMegSupport) {
            try {
                mergedStats = new NetworkStats(target.getElapsedRealtime(), target.size());
                NetworkStats.Entry mergedRecycle = new NetworkStats.Entry();
                for (int i = 0; i < target.size(); i++) {
                    mergedRecycle = target.getValues(i, mergedRecycle);
                    log(3, "changeToMergedStats: target stats [" + i + "] = " + mergedRecycle);
                    if (this.mMergedUids.contains(Integer.valueOf(mergedRecycle.uid))) {
                        mergedRecycle.uid = this.mDisplayUid;
                        log(3, "changeToMergedStats: merged stats [" + i + "] = " + mergedRecycle);
                    }
                    mergedStats.combineValues(mergedRecycle);
                }
            } catch (Exception e) {
                log(1, "problem during changeToMergedStats " + e);
                return null;
            }
        } else {
            mergedStats = target;
        }
        logNetworkStats(2, "changeToMergedStats : cUid[" + callingUid + "] cPack[" + callingPackage + "]", mergedStats);
        return mergedStats;
    }

    public NetworkStatsHistory getMergedHistory(String callingPackage, int callingUid, NetworkStatsCollection targetCollection, NetworkTemplate template, SubscriptionPlan augmentPlan, int uid, int set, int tag, int fields, long start, long end, int accessLevel) {
        NetworkStatsHistory displayHistory;
        if (targetCollection == null) {
            return null;
        }
        if (this.mIsTrafficMegSupport) {
            int i = this.mDisplayUid;
            if (uid == i) {
                displayHistory = targetCollection.getHistory(template, (SubscriptionPlan) null, i, set, tag, fields, start, end, accessLevel, callingUid);
                try {
                    for (Integer num : this.mMergedUids) {
                        int mergedUid = num.intValue();
                        if (mergedUid != this.mDisplayUid) {
                            NetworkStatsHistory mergedHistory = targetCollection.getHistory(template, (SubscriptionPlan) null, mergedUid, set, tag, fields, start, end, accessLevel, callingUid);
                            displayHistory.recordHistory(mergedHistory, start, end);
                        }
                    }
                } catch (Exception e) {
                    log(1, "problem during getMergedHistory " + e);
                    return null;
                }
            } else if (this.mMergedUids.contains(Integer.valueOf(uid))) {
                displayHistory = null;
            } else {
                displayHistory = targetCollection.getHistory(template, (SubscriptionPlan) null, uid, set, tag, fields, start, end, accessLevel, callingUid);
            }
        } else {
            displayHistory = targetCollection.getHistory(template, (SubscriptionPlan) null, uid, set, tag, fields, start, end, accessLevel, callingUid);
        }
        log(2, "getMergedHistory: tUid[" + uid + "] cUid[" + callingUid + "] cPack[" + callingPackage + "]" + displayHistory);
        return displayHistory;
    }

    public NetworkStats getSummaryForMergedUid(String callingPackage, int callingUid, NetworkTemplate template, long start, long end, boolean includeTags, boolean onlyMerged) {
        NetworkStats mergedStats;
        if (this.mIsTrafficMegSupport) {
            int accessLevel = checkAccessLevel(callingPackage);
            NetworkStats stats = getUidComplete().getSummary(template, start, end, accessLevel, callingUid);
            if (includeTags) {
                NetworkStats tagStats = getUidTagComplete().getSummary(template, start, end, accessLevel, callingUid);
                stats.combineAllValues(tagStats);
            }
            if (onlyMerged) {
                try {
                    mergedStats = new NetworkStats(stats.getElapsedRealtime(), stats.size());
                    NetworkStats.Entry mergedRecycle = new NetworkStats.Entry();
                    for (int i = 0; i < stats.size(); i++) {
                        mergedRecycle = stats.getValues(i, mergedRecycle);
                        log(3, "getSummaryForMergedUid: target entry [" + i + "] = " + mergedRecycle);
                        if (this.mMergedUids.contains(Integer.valueOf(mergedRecycle.uid))) {
                            mergedStats.combineValues(mergedRecycle);
                            log(3, "getSummaryForMergedUid: merged entry [" + i + "] = " + mergedRecycle);
                        }
                    }
                } catch (Exception e) {
                    log(1, "problem during getSummaryForMergedUid " + e);
                    return null;
                }
            } else {
                mergedStats = stats;
            }
        } else {
            mergedStats = null;
        }
        logNetworkStats(2, "getSummaryForMergedUid : onlyMerged[" + onlyMerged + "] cUid[" + callingUid + "] cPack[" + callingPackage + "]", mergedStats);
        return mergedStats;
    }

    private VivoNetworkStatsCollection getPackComplete() {
        VivoNetworkStatsCollection orLoadCompleteLocked;
        synchronized (this.mVivoStatsLock) {
            orLoadCompleteLocked = this.mPackInfoRecorder.getOrLoadCompleteLocked();
        }
        return orLoadCompleteLocked;
    }

    private VivoNetworkStatsCollection getPackTagComplete() {
        VivoNetworkStatsCollection orLoadCompleteLocked;
        synchronized (this.mVivoStatsLock) {
            orLoadCompleteLocked = this.mPackInfoTagRecorder.getOrLoadCompleteLocked();
        }
        return orLoadCompleteLocked;
    }

    private VivoNetworkStatsCollection getRatComplete() {
        VivoNetworkStatsCollection orLoadCompleteLocked;
        synchronized (this.mVivoStatsLock) {
            orLoadCompleteLocked = this.mRatInfoRecorder.getOrLoadCompleteLocked();
        }
        return orLoadCompleteLocked;
    }

    private VivoNetworkStatsCollection getRatTagComplete() {
        VivoNetworkStatsCollection orLoadCompleteLocked;
        synchronized (this.mVivoStatsLock) {
            orLoadCompleteLocked = this.mRatInfoTagRecorder.getOrLoadCompleteLocked();
        }
        return orLoadCompleteLocked;
    }

    private NetworkStatsCollection getUidComplete() {
        synchronized (this.mVivoStatsLock) {
            NetworkStatsRecorder uidRecorder = this.mService.getUidRecorder();
            if (uidRecorder != null) {
                return uidRecorder.getOrLoadCompleteLocked();
            }
            return null;
        }
    }

    private NetworkStatsCollection getUidTagComplete() {
        synchronized (this.mVivoStatsLock) {
            NetworkStatsRecorder uidTagRecorder = this.mService.getUidTagRecorder();
            if (uidTagRecorder != null) {
                return uidTagRecorder.getOrLoadCompleteLocked();
            }
            return null;
        }
    }

    private void updateSeparateStatsUids() {
        String[] strArr;
        synchronized (this.mSeparUidsLock) {
            try {
                this.mSeparUids.clear();
                this.mSeparUids.add(1000);
                this.mOemNetdService.setUidPackSeparate(1000, true);
                PackageManager pm = this.mContext.getPackageManager();
                for (String pack : mSeparPackages) {
                    ApplicationInfo ai = pm.getApplicationInfo(pack, 128);
                    this.mSeparUids.add(Integer.valueOf(ai.uid));
                    this.mOemNetdService.setUidPackSeparate(ai.uid, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void log(int level, String logStr) {
        if (level <= LOGABLE_LEVEL) {
            VSlog.d(TAG, logStr);
        }
    }

    public static void logNetworkStats(int level, String logStr, NetworkStats stats) {
        if (level <= LOGABLE_LEVEL) {
            if (stats == null) {
                VSlog.d(TAG, logStr + " : stats = null ");
                return;
            }
            NetworkStats.Entry printRecycle = new NetworkStats.Entry();
            VSlog.d(TAG, logStr + " : stats = " + stats.getElapsedRealtime());
            for (int i = 0; i < stats.size(); i++) {
                VSlog.d(TAG, logStr + " : stats = [" + i + "] " + stats.getValues(i, printRecycle));
            }
        }
    }

    public static void logNetworkStats(int level, String logStr, VivoNetworkStats stats) {
        if (level <= LOGABLE_LEVEL) {
            if (stats == null) {
                VSlog.d(TAG, logStr + " : stats = null ");
                return;
            }
            VivoNetworkStats.Entry printRecycle = new VivoNetworkStats.Entry();
            VSlog.d(TAG, logStr + " : stats = " + stats.getElapsedRealtime());
            for (int i = 0; i < stats.size(); i++) {
                VSlog.d(TAG, logStr + " : stats = [" + i + "] " + stats.getValues(i, printRecycle));
            }
        }
    }

    public void dummy() {
    }
}