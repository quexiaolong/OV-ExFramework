package com.android.server.am;

import android.bluetooth.BluetoothActivityEnergyInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Process;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SynchronousResultReceiver;
import android.os.SystemClock;
import android.os.ThreadLocalWorkSource;
import android.os.connectivity.WifiActivityEnergyInfo;
import android.telephony.ModemActivityInfo;
import android.telephony.TelephonyManager;
import android.util.IntArray;
import android.util.Slog;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.function.pooled.PooledLambda;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import libcore.util.EmptyArray;

/* loaded from: classes.dex */
public class BatteryExternalStatsWorker implements BatteryStatsImpl.ExternalStatsSync {
    private static final boolean DEBUG = false;
    private static final long EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS = 2000;
    private static final long MAX_WIFI_STATS_SAMPLE_ERROR_MILLIS = 750;
    private static final String TAG = "BatteryExternalStatsWorker";
    private Future<?> mBatteryLevelSync;
    private final Context mContext;
    private long mLastCollectionTimeStamp;
    private boolean mOnBattery;
    private boolean mOnBatteryScreenOff;
    private final BatteryStatsImpl mStats;
    private Future<?> mWakelockChangesUpdate;
    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor($$Lambda$BatteryExternalStatsWorker$ML8sXrbYk0MflPvsY2cfCYlcU0w.INSTANCE);
    private int mUpdateFlags = 0;
    private Future<?> mCurrentFuture = null;
    private String mCurrentReason = null;
    private boolean mUseLatestStates = true;
    private final IntArray mUidsToRemove = new IntArray();
    private final Object mWorkerLock = new Object();
    private WifiManager mWifiManager = null;
    private TelephonyManager mTelephony = null;
    private WifiActivityEnergyInfo mLastInfo = new WifiActivityEnergyInfo(0, 0, 0, 0, 0, 0);
    private final Runnable mSyncTask = new Runnable() { // from class: com.android.server.am.BatteryExternalStatsWorker.1
        {
            BatteryExternalStatsWorker.this = this;
        }

        @Override // java.lang.Runnable
        public void run() {
            int updateFlags;
            String reason;
            int[] uidsToRemove;
            boolean onBattery;
            boolean onBatteryScreenOff;
            boolean useLatestStates;
            synchronized (BatteryExternalStatsWorker.this) {
                updateFlags = BatteryExternalStatsWorker.this.mUpdateFlags;
                reason = BatteryExternalStatsWorker.this.mCurrentReason;
                uidsToRemove = BatteryExternalStatsWorker.this.mUidsToRemove.size() > 0 ? BatteryExternalStatsWorker.this.mUidsToRemove.toArray() : EmptyArray.INT;
                onBattery = BatteryExternalStatsWorker.this.mOnBattery;
                onBatteryScreenOff = BatteryExternalStatsWorker.this.mOnBatteryScreenOff;
                useLatestStates = BatteryExternalStatsWorker.this.mUseLatestStates;
                BatteryExternalStatsWorker.this.mUpdateFlags = 0;
                BatteryExternalStatsWorker.this.mCurrentReason = null;
                BatteryExternalStatsWorker.this.mUidsToRemove.clear();
                BatteryExternalStatsWorker.this.mCurrentFuture = null;
                BatteryExternalStatsWorker.this.mUseLatestStates = true;
                if ((updateFlags & 31) != 0) {
                    BatteryExternalStatsWorker.this.cancelSyncDueToBatteryLevelChangeLocked();
                }
                if ((updateFlags & 1) != 0) {
                    BatteryExternalStatsWorker.this.cancelCpuSyncDueToWakelockChange();
                }
            }
            try {
                synchronized (BatteryExternalStatsWorker.this.mWorkerLock) {
                    BatteryExternalStatsWorker.this.updateExternalStatsLocked(reason, updateFlags, onBattery, onBatteryScreenOff, useLatestStates);
                }
                if ((updateFlags & 1) != 0) {
                    BatteryExternalStatsWorker.this.mStats.copyFromAllUidsCpuTimes();
                }
                synchronized (BatteryExternalStatsWorker.this.mStats) {
                    for (int uid : uidsToRemove) {
                        FrameworkStatsLog.write(43, -1, uid, 0);
                        BatteryExternalStatsWorker.this.mStats.removeIsolatedUidLocked(uid);
                    }
                    BatteryExternalStatsWorker.this.mStats.clearPendingRemovedUids();
                }
            } catch (Exception e) {
                Slog.wtf(BatteryExternalStatsWorker.TAG, "Error updating external stats: ", e);
            }
            synchronized (BatteryExternalStatsWorker.this) {
                BatteryExternalStatsWorker.this.mLastCollectionTimeStamp = SystemClock.elapsedRealtime();
            }
        }
    };
    private final Runnable mWriteTask = new Runnable() { // from class: com.android.server.am.BatteryExternalStatsWorker.2
        {
            BatteryExternalStatsWorker.this = this;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (BatteryExternalStatsWorker.this.mStats) {
                BatteryExternalStatsWorker.this.mStats.writeAsyncLocked();
            }
        }
    };

    public static /* synthetic */ Thread lambda$new$1(final Runnable r) {
        Thread t = new Thread(new Runnable() { // from class: com.android.server.am.-$$Lambda$BatteryExternalStatsWorker$ddVY5lmqswnSjXppAxPTOHbuzzQ
            @Override // java.lang.Runnable
            public final void run() {
                BatteryExternalStatsWorker.lambda$new$0(r);
            }
        }, "batterystats-worker");
        t.setPriority(5);
        return t;
    }

    public static /* synthetic */ void lambda$new$0(Runnable r) {
        ThreadLocalWorkSource.setUid(Process.myUid());
        r.run();
    }

    public BatteryExternalStatsWorker(Context context, BatteryStatsImpl stats) {
        this.mContext = context;
        this.mStats = stats;
    }

    public synchronized Future<?> scheduleSync(String reason, int flags) {
        return scheduleSyncLocked(reason, flags);
    }

    public synchronized Future<?> scheduleCpuSyncDueToRemovedUid(int uid) {
        this.mUidsToRemove.add(uid);
        return scheduleSyncLocked("remove-uid", 1);
    }

    public synchronized Future<?> scheduleCpuSyncDueToSettingChange() {
        return scheduleSyncLocked("setting-change", 1);
    }

    public Future<?> scheduleReadProcStateCpuTimes(boolean onBattery, boolean onBatteryScreenOff, long delayMillis) {
        synchronized (this.mStats) {
            if (this.mStats.trackPerProcStateCpuTimes()) {
                synchronized (this) {
                    if (this.mExecutorService.isShutdown()) {
                        return null;
                    }
                    return this.mExecutorService.schedule((Runnable) PooledLambda.obtainRunnable($$Lambda$cC4f0pNQX9_D9f8AXLmKk2sArGY.INSTANCE, this.mStats, Boolean.valueOf(onBattery), Boolean.valueOf(onBatteryScreenOff)).recycleOnUse(), delayMillis, TimeUnit.MILLISECONDS);
                }
            }
            return null;
        }
    }

    public Future<?> scheduleCopyFromAllUidsCpuTimes(boolean onBattery, boolean onBatteryScreenOff) {
        synchronized (this.mStats) {
            if (this.mStats.trackPerProcStateCpuTimes()) {
                synchronized (this) {
                    if (this.mExecutorService.isShutdown()) {
                        return null;
                    }
                    return this.mExecutorService.submit((Runnable) PooledLambda.obtainRunnable($$Lambda$7toxTvZDSEytL0rCkoEfGilPDWM.INSTANCE, this.mStats, Boolean.valueOf(onBattery), Boolean.valueOf(onBatteryScreenOff)).recycleOnUse());
                }
            }
            return null;
        }
    }

    public Future<?> scheduleCpuSyncDueToScreenStateChange(boolean onBattery, boolean onBatteryScreenOff) {
        Future<?> scheduleSyncLocked;
        synchronized (this) {
            if (this.mCurrentFuture == null || (this.mUpdateFlags & 1) == 0) {
                this.mOnBattery = onBattery;
                this.mOnBatteryScreenOff = onBatteryScreenOff;
                this.mUseLatestStates = false;
            }
            scheduleSyncLocked = scheduleSyncLocked("screen-state", 1);
        }
        return scheduleSyncLocked;
    }

    public Future<?> scheduleCpuSyncDueToWakelockChange(long delayMillis) {
        Future<?> scheduleDelayedSyncLocked;
        synchronized (this) {
            scheduleDelayedSyncLocked = scheduleDelayedSyncLocked(this.mWakelockChangesUpdate, new Runnable() { // from class: com.android.server.am.-$$Lambda$BatteryExternalStatsWorker$r3x3xYmhrLG8kgeNVPXl5EILHwU
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryExternalStatsWorker.this.lambda$scheduleCpuSyncDueToWakelockChange$3$BatteryExternalStatsWorker();
                }
            }, delayMillis);
            this.mWakelockChangesUpdate = scheduleDelayedSyncLocked;
        }
        return scheduleDelayedSyncLocked;
    }

    public /* synthetic */ void lambda$scheduleCpuSyncDueToWakelockChange$3$BatteryExternalStatsWorker() {
        scheduleSync("wakelock-change", 1);
        scheduleRunnable(new Runnable() { // from class: com.android.server.am.-$$Lambda$BatteryExternalStatsWorker$PpNEY15dspg9oLlkg1OsyjrPTqw
            @Override // java.lang.Runnable
            public final void run() {
                BatteryExternalStatsWorker.this.lambda$scheduleCpuSyncDueToWakelockChange$2$BatteryExternalStatsWorker();
            }
        });
    }

    public /* synthetic */ void lambda$scheduleCpuSyncDueToWakelockChange$2$BatteryExternalStatsWorker() {
        this.mStats.postBatteryNeedsCpuUpdateMsg();
    }

    public void cancelCpuSyncDueToWakelockChange() {
        synchronized (this) {
            if (this.mWakelockChangesUpdate != null) {
                this.mWakelockChangesUpdate.cancel(false);
                this.mWakelockChangesUpdate = null;
            }
        }
    }

    public Future<?> scheduleSyncDueToBatteryLevelChange(long delayMillis) {
        Future<?> scheduleDelayedSyncLocked;
        synchronized (this) {
            scheduleDelayedSyncLocked = scheduleDelayedSyncLocked(this.mBatteryLevelSync, new Runnable() { // from class: com.android.server.am.-$$Lambda$BatteryExternalStatsWorker$xR3yCbbVfCo3oq_xPiH7j5l5uac
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryExternalStatsWorker.this.lambda$scheduleSyncDueToBatteryLevelChange$4$BatteryExternalStatsWorker();
                }
            }, delayMillis);
            this.mBatteryLevelSync = scheduleDelayedSyncLocked;
        }
        return scheduleDelayedSyncLocked;
    }

    public /* synthetic */ void lambda$scheduleSyncDueToBatteryLevelChange$4$BatteryExternalStatsWorker() {
        scheduleSync("battery-level", 31);
    }

    public void cancelSyncDueToBatteryLevelChangeLocked() {
        Future<?> future = this.mBatteryLevelSync;
        if (future != null) {
            future.cancel(false);
            this.mBatteryLevelSync = null;
        }
    }

    private Future<?> scheduleDelayedSyncLocked(Future<?> lastScheduledSync, Runnable syncRunnable, long delayMillis) {
        if (this.mExecutorService.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("worker shutdown"));
        }
        if (lastScheduledSync != null) {
            if (delayMillis == 0) {
                lastScheduledSync.cancel(false);
            } else {
                return lastScheduledSync;
            }
        }
        return this.mExecutorService.schedule(syncRunnable, delayMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized Future<?> scheduleWrite() {
        if (this.mExecutorService.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("worker shutdown"));
        }
        scheduleSyncLocked("write", 31);
        return this.mExecutorService.submit(this.mWriteTask);
    }

    public synchronized void scheduleRunnable(Runnable runnable) {
        if (!this.mExecutorService.isShutdown()) {
            this.mExecutorService.submit(runnable);
        }
    }

    public void shutdown() {
        this.mExecutorService.shutdownNow();
    }

    private Future<?> scheduleSyncLocked(String reason, int flags) {
        if (this.mExecutorService.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("worker shutdown"));
        }
        if (this.mCurrentFuture == null) {
            this.mUpdateFlags = flags;
            this.mCurrentReason = reason;
            this.mCurrentFuture = this.mExecutorService.submit(this.mSyncTask);
        }
        this.mUpdateFlags |= flags;
        return this.mCurrentFuture;
    }

    public long getLastCollectionTimeStamp() {
        long j;
        synchronized (this) {
            j = this.mLastCollectionTimeStamp;
        }
        return j;
    }

    public void updateExternalStatsLocked(String reason, int updateFlags, boolean onBattery, boolean onBatteryScreenOff, boolean useLatestStates) {
        final SynchronousResultReceiver tempWifiReceiver;
        boolean onBattery2;
        boolean onBatteryScreenOff2;
        BluetoothAdapter adapter;
        ResultReceiver resultReceiver = null;
        ResultReceiver resultReceiver2 = null;
        boolean railUpdated = false;
        if ((updateFlags & 2) == 0) {
            tempWifiReceiver = null;
        } else {
            if (this.mWifiManager == null && ServiceManager.getService("wifi") != null) {
                this.mWifiManager = (WifiManager) this.mContext.getSystemService(WifiManager.class);
            }
            WifiManager wifiManager = this.mWifiManager;
            if (wifiManager != null && wifiManager.isEnhancedPowerReportingSupported()) {
                tempWifiReceiver = new SynchronousResultReceiver("wifi");
                this.mWifiManager.getWifiActivityEnergyInfoAsync(new Executor() { // from class: com.android.server.am.BatteryExternalStatsWorker.3
                    {
                        BatteryExternalStatsWorker.this = this;
                    }

                    @Override // java.util.concurrent.Executor
                    public void execute(Runnable runnable) {
                        runnable.run();
                    }
                }, new WifiManager.OnWifiActivityEnergyInfoListener() { // from class: com.android.server.am.-$$Lambda$BatteryExternalStatsWorker$MJXTdtPzBwRCdTjCDCE77VXPHBk
                    public final void onWifiActivityEnergyInfo(WifiActivityEnergyInfo wifiActivityEnergyInfo) {
                        BatteryExternalStatsWorker.lambda$updateExternalStatsLocked$5(tempWifiReceiver, wifiActivityEnergyInfo);
                    }
                });
            } else {
                tempWifiReceiver = null;
            }
            synchronized (this.mStats) {
                this.mStats.updateRailStatsLocked();
            }
            railUpdated = true;
        }
        if ((updateFlags & 8) != 0 && (adapter = BluetoothAdapter.getDefaultAdapter()) != null) {
            resultReceiver = new SynchronousResultReceiver("bluetooth");
            adapter.requestControllerActivityEnergyInfo(resultReceiver);
        }
        if ((updateFlags & 4) != 0) {
            if (this.mTelephony == null) {
                this.mTelephony = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
            }
            if (this.mTelephony != null) {
                resultReceiver2 = new SynchronousResultReceiver("telephony");
                this.mTelephony.requestModemActivityInfo(resultReceiver2);
            }
            if (!railUpdated) {
                synchronized (this.mStats) {
                    this.mStats.updateRailStatsLocked();
                }
            }
        }
        WifiActivityEnergyInfo wifiInfo = (WifiActivityEnergyInfo) awaitControllerInfo(tempWifiReceiver);
        BluetoothActivityEnergyInfo bluetoothInfo = awaitControllerInfo(resultReceiver);
        ModemActivityInfo modemInfo = awaitControllerInfo(resultReceiver2);
        synchronized (this.mStats) {
            try {
                try {
                    this.mStats.addHistoryEventLocked(SystemClock.elapsedRealtime(), SystemClock.uptimeMillis(), 14, reason, 0);
                    if ((updateFlags & 1) != 0) {
                        if (!useLatestStates) {
                            onBattery2 = onBattery;
                            onBatteryScreenOff2 = onBatteryScreenOff;
                        } else {
                            onBattery2 = this.mStats.isOnBatteryLocked();
                            try {
                                onBatteryScreenOff2 = this.mStats.isOnBatteryScreenOffLocked();
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                        this.mStats.updateCpuTimeLocked(onBattery2, onBatteryScreenOff2);
                    }
                    if ((updateFlags & 31) != 0) {
                        this.mStats.updateKernelWakelocksLocked();
                        this.mStats.updateKernelMemoryBandwidthLocked();
                    }
                    if ((updateFlags & 16) != 0) {
                        this.mStats.updateRpmStatsLocked();
                    }
                    if (bluetoothInfo != null) {
                        if (bluetoothInfo.isValid()) {
                            this.mStats.updateBluetoothStateLocked(bluetoothInfo);
                        } else {
                            Slog.w(TAG, "bluetooth info is invalid: " + bluetoothInfo);
                        }
                    }
                    if (wifiInfo != null) {
                        if (wifiInfo.isValid()) {
                            this.mStats.updateWifiState(extractDeltaLocked(wifiInfo));
                        } else {
                            Slog.w(TAG, "wifi info is invalid: " + wifiInfo);
                        }
                    }
                    if (modemInfo != null) {
                        if (modemInfo.isValid()) {
                            this.mStats.updateMobileRadioState(modemInfo);
                            return;
                        }
                        Slog.w(TAG, "modem info is invalid: " + modemInfo);
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public static /* synthetic */ void lambda$updateExternalStatsLocked$5(SynchronousResultReceiver tempWifiReceiver, WifiActivityEnergyInfo info) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("controller_activity", info);
        tempWifiReceiver.send(0, bundle);
    }

    private static <T extends Parcelable> T awaitControllerInfo(SynchronousResultReceiver receiver) {
        if (receiver == null) {
            return null;
        }
        try {
            SynchronousResultReceiver.Result result = receiver.awaitResult((long) EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS);
            if (result.bundle != null) {
                result.bundle.setDefusable(true);
                T data = (T) result.bundle.getParcelable("controller_activity");
                if (data != null) {
                    return data;
                }
            }
        } catch (TimeoutException e) {
            Slog.w(TAG, "timeout reading " + receiver.getName() + " stats");
        }
        return null;
    }

    /* JADX WARN: Removed duplicated region for block: B:58:0x019f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private android.os.connectivity.WifiActivityEnergyInfo extractDeltaLocked(android.os.connectivity.WifiActivityEnergyInfo r52) {
        /*
            Method dump skipped, instructions count: 436
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.BatteryExternalStatsWorker.extractDeltaLocked(android.os.connectivity.WifiActivityEnergyInfo):android.os.connectivity.WifiActivityEnergyInfo");
    }
}