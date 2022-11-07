package com.android.server.am;

import android.content.pm.ComponentInfo;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import com.vivo.common.utils.VLog;
import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.proxy.broadcast.BroadcastConfigs;
import com.vivo.services.proxy.game.GameSceneProxyManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public class BroadcastProxyQueue {
    private static final int BR_MATCH_TYPE_NULL = 0;
    private static final int BR_MATCH_TYPE_SAME = 1;
    private static final String TAG = "RMBroadcastQueue";
    final BroadcastQueue mParent;
    private final ArrayList<BroadcastProxyRecord> mPendingOrderedBroadcasts = new ArrayList<>();
    private final ArrayList<BroadcastProxyRecord> mPendingParallelBroadcasts = new ArrayList<>();
    final VivoBroadcastQueueImpl mVivoBroadcastQueue;

    public BroadcastProxyQueue(BroadcastQueue queue, VivoBroadcastQueueImpl vivoBroadcastQueue) {
        this.mParent = queue;
        this.mVivoBroadcastQueue = vivoBroadcastQueue;
        BroadcastProxyManager.register(this);
    }

    public boolean enqueuePendingBroadcastLocked(BroadcastRecord r, Object target, boolean ordered) {
        if (target == null || !BroadcastProxyManager.isProxyEnable()) {
            return false;
        }
        BroadcastProxyRecord brProxy = new BroadcastProxyRecord(this, r, target);
        if (brProxy.mProcess == null || brProxy.mUid == -1 || brProxy.mPkgName == null || (brProxy.mType == 1 && brProxy.mPid <= 0)) {
            VLog.e(TAG, "enqueuePendingBroadcast error:" + brProxy.mAction);
            return false;
        }
        boolean proxyPackageMode = BroadcastProxyManager.hasRefsLocked(brProxy.mPkgName, brProxy.mUserId, false);
        boolean proxyProcessMode = BroadcastProxyManager.hasRefsLocked(brProxy.mProcess, brProxy.mUserId, true);
        boolean proxyByGameScene = false;
        if (!proxyPackageMode && !proxyProcessMode) {
            boolean isProxyInGameScene = isProxyInGameScene(brProxy);
            proxyByGameScene = isProxyInGameScene;
            if (!isProxyInGameScene) {
                return false;
            }
        }
        brProxy.initTargetProcessLocked();
        if (proxyByGameScene) {
            GameSceneProxyManager.addProcessBroadcastProxy(brProxy.mProcess, brProxy.mUserId);
        }
        brProxy.initBroadcastRecordLocked(r, target);
        if (proxyPackageMode && brProxy.mPid > 0) {
            unfreezeIfNeeded(brProxy);
        }
        boolean unmergerable = isUnmergerable(brProxy);
        int optCount = 0;
        int count = BroadcastConfigs.getBrProxyCountLocked(brProxy.mProcess, brProxy.mUserId);
        ArrayList<BroadcastProxyRecord> pendingBroadcasts = ordered ? this.mPendingOrderedBroadcasts : this.mPendingParallelBroadcasts;
        Iterator<BroadcastProxyRecord> it = pendingBroadcasts.iterator();
        if (!unmergerable) {
            while (it.hasNext()) {
                if (match(brProxy, it.next()) == 1) {
                    optCount++;
                    count--;
                    it.remove();
                }
            }
        }
        int count2 = count + 1;
        pendingBroadcasts.add(brProxy);
        BroadcastConfigs.setBrProxyCountLocked(brProxy.mProcess, brProxy.mUserId, count2);
        BroadcastConfigs.updateBrProxyOptCountLocked(brProxy.mProcess, brProxy.mUserId, optCount);
        if (count2 > 0 && count2 % BroadcastConfigs.PROXY_BR_ABNORMAL_SIZE == 0) {
            BroadcastProxyManager.handleBrProxyAbnormalLocked(brProxy.mPkgName, brProxy.mProcess, brProxy.mUid, brProxy.mPid, count2);
        }
        if (ProxyConfigs.DEBUG_BQ) {
            VLog.i(TAG, "enqueuePendingBroadcast " + brProxy.toString());
            return true;
        }
        return true;
    }

    private boolean isProxyInGameScene(BroadcastProxyRecord brProxy) {
        if (brProxy.mType == 1 || GameSceneProxyManager.isBroadcastAllowRestartLocked(brProxy.mPkgName, brProxy.mProcess)) {
            return false;
        }
        brProxy.initTargetProcessLocked();
        return brProxy.mPid <= 0;
    }

    public void onStartProcessLocked(BroadcastRecord r, Object target, String targetProcess) {
        if (ProxyConfigs.isDumpAllowed()) {
            BroadcastConfigs.addProcStartedHistoryLocked(targetProcess, r.intent.getAction());
        }
        if (ProxyConfigs.DEBUG_BQ) {
            VLog.i(TAG, String.format("startProcess %s by %s", targetProcess, r.intent.getAction()));
        }
    }

    public void unproxyBroadcastLocked(List<String> pkgList, int userId, boolean processMode) {
        BroadcastProxyRecord br;
        boolean handleOrderNow = false;
        ArrayList<BroadcastProxyRecord> parallelBroadcasts = getAndRemoveBroadcastLocked(pkgList, userId, false, processMode);
        ArrayList<BroadcastProxyRecord> orderredBroadcasts = getAndRemoveBroadcastLocked(pkgList, userId, true, processMode);
        for (int i = 0; i < parallelBroadcasts.size(); i++) {
            this.mParent.mParallelBroadcasts.add(i, parallelBroadcasts.get(i).mBr);
            if (ProxyConfigs.DEBUG_BQ) {
                VLog.v(TAG, "unproxyBroadcast" + br.toString());
            }
        }
        if (this.mParent.mPendingBroadcastTimeoutMessage && !this.mParent.mDispatcher.mOrderedBroadcasts.isEmpty()) {
            handleOrderNow = true;
        }
        for (int i2 = 0; i2 < orderredBroadcasts.size(); i2++) {
            BroadcastProxyRecord br2 = orderredBroadcasts.get(i2);
            if (handleOrderNow) {
                this.mParent.mDispatcher.mOrderedBroadcasts.add(i2 + 1, br2.mBr);
            } else {
                this.mParent.mDispatcher.mOrderedBroadcasts.add(i2, br2.mBr);
            }
            if (ProxyConfigs.DEBUG_BQ) {
                VLog.v(TAG, "unproxyBroadcast" + br2.toString());
            }
        }
        if (!parallelBroadcasts.isEmpty() || !orderredBroadcasts.isEmpty()) {
            this.mParent.scheduleBroadcastsLocked();
        }
    }

    private ArrayList<BroadcastProxyRecord> getAndRemoveBroadcastLocked(List<String> pkgList, int userId, boolean isOrdered, boolean processMode) {
        ArrayList<BroadcastProxyRecord> broadcasts = new ArrayList<>();
        ArrayList<BroadcastProxyRecord> pendingBroadcasts = isOrdered ? this.mPendingOrderedBroadcasts : this.mPendingParallelBroadcasts;
        if (pkgList == null) {
            broadcasts.addAll(pendingBroadcasts);
            pendingBroadcasts.clear();
            return broadcasts;
        }
        Iterator<BroadcastProxyRecord> it = pendingBroadcasts.iterator();
        while (it.hasNext()) {
            BroadcastProxyRecord br = it.next();
            if (userId == -1 || br.mUserId == userId) {
                boolean matched = pkgList.contains(processMode ? br.mProcess : br.mPkgName);
                if (matched) {
                    if (!isDiscardable(br)) {
                        broadcasts.add(br);
                    }
                    it.remove();
                    BroadcastConfigs.incBrProxyCountsLocked(br.mProcess, br.mUserId, -1);
                }
            }
        }
        return broadcasts;
    }

    public void collectProcessActionsLocked(String process, int userId, HashMap<String, Integer> actions) {
        ArrayList<BroadcastProxyRecord>[] lists = {this.mPendingOrderedBroadcasts, this.mPendingParallelBroadcasts};
        for (ArrayList<BroadcastProxyRecord> pendingList : lists) {
            Iterator<BroadcastProxyRecord> it = pendingList.iterator();
            while (it.hasNext()) {
                BroadcastProxyRecord br = it.next();
                if (br.mUserId == userId && br.mProcess.equals(process)) {
                    actions.put(br.mAction, Integer.valueOf(actions.getOrDefault(br.mAction, 0).intValue() + 1));
                }
            }
        }
    }

    public void cleanupDisabledPackageReceiversLocked(String packageName, Set<String> filterByClasses, int userId, boolean doit) {
        if (BroadcastProxyManager.isProxyEnable()) {
            cleanupPackageLocked(packageName, userId, false);
            cleanupPackageLocked(packageName, userId, true);
        }
    }

    public void cleanupAllLocked() {
        if (!this.mPendingOrderedBroadcasts.isEmpty()) {
            VLog.e(TAG, "cleanupAllLocked mPendingOrderedBroadcasts is not empty,maybe a bug.");
            VLog.e(TAG, "mPendingOrderedBroadcasts:" + this.mPendingOrderedBroadcasts.toString());
            this.mPendingOrderedBroadcasts.clear();
        }
        if (!this.mPendingParallelBroadcasts.isEmpty()) {
            VLog.e(TAG, "cleanupAllLocked mPendingParallelBroadcasts is not empty,maybe a bug.");
            VLog.e(TAG, "mPendingParallelBroadcasts:" + this.mPendingParallelBroadcasts.toString());
            this.mPendingParallelBroadcasts.clear();
        }
    }

    public void cleanupReceiverLocked(ReceiverList list) {
        cleanupByReceiverListLocked(list, false);
        cleanupByReceiverListLocked(list, true);
    }

    public void cleanupProcessRecordLocked(ProcessRecord app) {
        cleanupByProcessNameLocked(app.processName, app.userId, false);
        cleanupByProcessNameLocked(app.processName, app.userId, true);
        cleanupSendToSelfBroadcastLocked(app);
    }

    private void cleanupSendToSelfBroadcastLocked(ProcessRecord app) {
        if (!isThirdPartyApp(app) || BroadcastConfigs.APP_RESTART_SELF_SET.contains(app.info.packageName)) {
            return;
        }
        int size = this.mParent.mDispatcher.mOrderedBroadcasts.size();
        if (size > 100) {
            VLog.e(TAG, String.format("cleanupSendToSelfBroadcastLocked error queue=%s size=%d, too many ordered broadcasts", this.mParent.mQueueName, Integer.valueOf(size)));
            return;
        }
        boolean remove = false;
        boolean scheduleNext = false;
        for (int i = this.mParent.mDispatcher.mOrderedBroadcasts.size() - 1; i >= 0; i--) {
            BroadcastRecord r = (BroadcastRecord) this.mParent.mDispatcher.mOrderedBroadcasts.get(i);
            if (r.callerApp == app || r.callingUid == app.uid) {
                int N = r.receivers != null ? r.receivers.size() : 0;
                int j = 0;
                while (true) {
                    if (j >= N) {
                        break;
                    }
                    Object target = r.receivers.get(j);
                    if (!(target instanceof ResolveInfo) || app.processName == null || !app.processName.equals(getTargetProcessName(target))) {
                        j++;
                    } else {
                        remove = true;
                        break;
                    }
                }
                if (remove) {
                    if (i == 0) {
                        this.mParent.skipPendingBroadcastLocked(app.pid);
                        this.mParent.cancelBroadcastTimeoutLocked();
                        if (r == this.mParent.mPendingBroadcast) {
                            this.mParent.mPendingBroadcast = null;
                        }
                    }
                    this.mParent.mDispatcher.mOrderedBroadcasts.remove(r);
                    scheduleNext = true;
                    if (ProxyConfigs.DEBUG_BQ) {
                        VLog.i(TAG, String.format("cleanupSendToSelfBroadcast queue=%s action=%s name=%s uid=%d", this.mParent.mQueueName, r.intent.getAction(), app.processName, Integer.valueOf(app.uid)));
                    }
                }
            }
        }
        if (scheduleNext) {
            this.mParent.scheduleBroadcastsLocked();
        }
    }

    private void cleanupPackageLocked(String packageName, int userId, boolean isOrdered) {
        ArrayList<BroadcastProxyRecord> pendingBroadcasts = isOrdered ? this.mPendingOrderedBroadcasts : this.mPendingParallelBroadcasts;
        Iterator<BroadcastProxyRecord> it = pendingBroadcasts.iterator();
        while (it.hasNext()) {
            BroadcastProxyRecord br = it.next();
            if (userId == -1 || br.mUserId == userId) {
                if (br.mPkgName.equals(packageName)) {
                    it.remove();
                    BroadcastConfigs.incBrProxyCountsLocked(br.mProcess, br.mUserId, -1);
                    if (ProxyConfigs.DEBUG_BQ) {
                        VLog.i(TAG, "cleanupBroadcastsByPackage" + br.toString());
                    }
                }
            }
        }
    }

    private void cleanupByProcessNameLocked(String processName, int userId, boolean isOrdered) {
        ArrayList<BroadcastProxyRecord> pendingBroadcasts = isOrdered ? this.mPendingOrderedBroadcasts : this.mPendingParallelBroadcasts;
        Iterator<BroadcastProxyRecord> it = pendingBroadcasts.iterator();
        while (it.hasNext()) {
            BroadcastProxyRecord br = it.next();
            if (userId == -1 || UserHandle.getUserId(br.mUid) == userId) {
                if (processName.equals(br.mProcess)) {
                    it.remove();
                    BroadcastConfigs.incBrProxyCountsLocked(processName, userId, -1);
                    if (ProxyConfigs.DEBUG_BQ) {
                        VLog.i(TAG, "cleanupBroadcastsByProcessName " + br.toString());
                    }
                }
            }
        }
    }

    private boolean cleanupByReceiverListLocked(ReceiverList receiverList, boolean isOrdered) {
        ArrayList<BroadcastProxyRecord> pendingBroadcasts = isOrdered ? this.mPendingOrderedBroadcasts : this.mPendingParallelBroadcasts;
        boolean didSomething = false;
        Iterator<BroadcastProxyRecord> it = pendingBroadcasts.iterator();
        while (it.hasNext()) {
            BroadcastProxyRecord br = it.next();
            if (br.mType == 1) {
                BroadcastFilter filter = (BroadcastFilter) br.mTarget;
                if (filter.receiverList == receiverList) {
                    it.remove();
                    BroadcastConfigs.incBrProxyCountsLocked(br.mProcess, br.mUserId, -1);
                    didSomething = true;
                    if (ProxyConfigs.DEBUG_BQ) {
                        VLog.i(TAG, "cleanupBroadcastsByReceiverList " + br.toString());
                    }
                }
            }
        }
        return didSomething;
    }

    private boolean isTargetEqual(BroadcastProxyRecord proxy1, BroadcastProxyRecord proxy2) {
        if (proxy1.mTarget == proxy2.mTarget) {
            return true;
        }
        if (proxy1.mType == proxy2.mType) {
            if (proxy1.mType == 1) {
                BroadcastFilter filter1 = (BroadcastFilter) proxy1.mTarget;
                BroadcastFilter filter2 = (BroadcastFilter) proxy2.mTarget;
                return (filter1.receiverList == null || filter2.receiverList == null || filter1.receiverList.receiver != filter2.receiverList.receiver) ? false : true;
            } else if (proxy1.mType == 2) {
                ResolveInfo info1 = (ResolveInfo) proxy1.mTarget;
                ResolveInfo info2 = (ResolveInfo) proxy2.mTarget;
                ComponentInfo component1 = info1.getComponentInfo();
                ComponentInfo component2 = info2.getComponentInfo();
                return component1.packageName.equals(component2.packageName) && component1.name.equals(component2.name);
            } else {
                return false;
            }
        }
        return false;
    }

    private int match(BroadcastProxyRecord proxy1, BroadcastProxyRecord proxy2) {
        if (proxy1.mUid == -1 || proxy2.mUid == -1 || proxy1.mUid != proxy2.mUid || !isTargetEqual(proxy1, proxy2)) {
            return 0;
        }
        if (proxy1.mAction.equals(proxy2.mAction)) {
            if (ProxyConfigs.DEBUG_BQ) {
                VLog.i(TAG, "Broadcast is matched for same: " + proxy2.toString());
            }
            return 1;
        }
        String action = BroadcastConfigs.PAIR_ACTIONS.getOther(proxy2.mAction);
        if (action == null || !proxy1.mAction.equals(action)) {
            return 0;
        }
        if (ProxyConfigs.DEBUG_BQ) {
            VLog.i(TAG, "Broadcast is matched for pair: " + proxy2.toString());
        }
        return 1;
    }

    private boolean isDiscardable(BroadcastProxyRecord br) {
        HashSet<String> discardActions = BroadcastConfigs.APP_DISCARD_ACTIONS.get(br.mPkgName);
        if (discardActions == null) {
            discardActions = BroadcastConfigs.DEFAULT_DISCARD_ACTIONS;
        }
        if (discardActions.contains(br.mAction)) {
            if (ProxyConfigs.DEBUG_BQ) {
                VLog.i(TAG, "Discard " + br.toString());
                return true;
            }
            return true;
        }
        return false;
    }

    private boolean isUnmergerable(BroadcastProxyRecord br) {
        HashSet<String> removeActions = BroadcastConfigs.APP_REMOVE_ACTIONS.get(br.mPkgName);
        if (removeActions != null && removeActions.contains(br.mAction)) {
            return true;
        }
        return BroadcastConfigs.DEFAULT_REMOVE_ACTIONS.contains(br.mAction);
    }

    private String getTargetProcessName(Object target) {
        if (target instanceof BroadcastFilter) {
            BroadcastFilter filter = (BroadcastFilter) target;
            if (filter.receiverList == null || filter.receiverList.app == null) {
                return null;
            }
            return filter.receiverList.app.processName;
        } else if (target instanceof ResolveInfo) {
            ResolveInfo info = (ResolveInfo) target;
            if (info.activityInfo == null || info.activityInfo.applicationInfo == null) {
                return null;
            }
            return info.activityInfo.processName;
        } else {
            return null;
        }
    }

    public ProcessRecord getProcessRecordLocked(String process, int uid) {
        return this.mParent.mService.getProcessRecordLocked(process, uid, true);
    }

    private void unfreezeIfNeeded(BroadcastProxyRecord brProxy) {
        this.mVivoBroadcastQueue.isKeepFrozenBroadcastPrcocess(brProxy.mBr, brProxy.mCurApp, false, "deliver Broadcast");
    }

    private static boolean isThirdPartyApp(ProcessRecord app) {
        return (app == null || app.pid == ActivityManagerService.MY_PID || (app.info.flags & 1) != 0) ? false : true;
    }

    public void dumpLocked(PrintWriter pw) {
        if (!this.mPendingOrderedBroadcasts.isEmpty() || !this.mPendingParallelBroadcasts.isEmpty()) {
            if (!this.mPendingOrderedBroadcasts.isEmpty()) {
                pw.println("PendingOrderedBroadcasts:");
                Iterator<BroadcastProxyRecord> it = this.mPendingOrderedBroadcasts.iterator();
                while (it.hasNext()) {
                    BroadcastProxyRecord br = it.next();
                    pw.print('\t');
                    pw.println(br.toString());
                }
            }
            if (!this.mPendingParallelBroadcasts.isEmpty()) {
                pw.println("PendingParallelBroadcasts:");
                Iterator<BroadcastProxyRecord> it2 = this.mPendingParallelBroadcasts.iterator();
                while (it2.hasNext()) {
                    BroadcastProxyRecord br2 = it2.next();
                    pw.print('\t');
                    pw.println(br2.toString());
                }
            }
        }
    }

    public void dumpByProcessesLocked(PrintWriter pw, HashSet<String> processes, int userId) {
        if (!this.mPendingOrderedBroadcasts.isEmpty() || !this.mPendingParallelBroadcasts.isEmpty()) {
            if (!this.mPendingOrderedBroadcasts.isEmpty()) {
                pw.println("\tPendingOrderedBroadcasts:");
                Iterator<BroadcastProxyRecord> it = this.mPendingOrderedBroadcasts.iterator();
                while (it.hasNext()) {
                    BroadcastProxyRecord br = it.next();
                    if (processes.contains(br.mProcess) && br.mUserId == userId) {
                        pw.print("\t\t");
                        pw.println(br.toString());
                    }
                }
            }
            if (!this.mPendingParallelBroadcasts.isEmpty()) {
                pw.println("\tPendingParallelBroadcasts:");
                Iterator<BroadcastProxyRecord> it2 = this.mPendingParallelBroadcasts.iterator();
                while (it2.hasNext()) {
                    BroadcastProxyRecord br2 = it2.next();
                    if (processes.contains(br2.mProcess) && br2.mUserId == userId) {
                        pw.print("\t\t");
                        pw.println(br2.toString());
                    }
                }
            }
        }
    }
}