package com.android.server.am;

import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Pair;
import android.util.SparseArray;
import com.vivo.common.utils.VLog;
import com.vivo.services.proxy.DualInt;
import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.proxy.RefValue;
import com.vivo.services.proxy.broadcast.BroadcastConfigs;
import com.vivo.services.rms.Platform;
import com.vivo.statistics.sdk.GatherManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class BroadcastProxyManager {
    private static final String TAG = "RMBroadcastQueue";
    private static ActivityManagerService sService;
    private static final ArrayList<BroadcastProxyQueue> BQ_CORES = new ArrayList<>();
    private static final SparseArray<HashMap<String, RefValue>> PROXY_REFS_FOR_PACKAGE_MODE = new SparseArray<>();
    private static final SparseArray<HashMap<String, RefValue>> PROXY_REFS_FOR_PROCESS_MODE = new SparseArray<>();
    private static boolean sProxyPackageEnabled = SystemProperties.getBoolean("persist.sys.bq.proxy_package", false);
    private static boolean sProxyProcessEnabled = SystemProperties.getBoolean("persist.sys.bq.proxy_process", Platform.isMiddlePerfDevice());

    public static void proxyPackage(List<String> pkgList, int userId, boolean proxy, String module) {
        if (sService == null || pkgList == null || !isProxyPackageEnable()) {
            return;
        }
        if (!ProxyConfigs.CTRL_MODULE_SET.contains(module)) {
            VLog.e(TAG, "proxyPackage from a invalid module:" + module);
            return;
        }
        if (ProxyConfigs.DEBUG_BQ) {
            VLog.i(TAG, String.format("proxyPackage proxy=%s module=%s pkgList=%s", String.valueOf(proxy), module, pkgList.toString()));
        }
        synchronized (sService) {
            ArrayList<String> unrefs = updateRefsLocked(pkgList, userId, proxy, module, false);
            if (unrefs.isEmpty()) {
                return;
            }
            if (!proxy) {
                Iterator<BroadcastProxyQueue> it = BQ_CORES.iterator();
                while (it.hasNext()) {
                    BroadcastProxyQueue queue = it.next();
                    queue.unproxyBroadcastLocked(unrefs, userId, false);
                }
                if (ProxyConfigs.DEBUG_BQ) {
                    VLog.i(TAG, String.format("proxyPackage unrefs=%s", unrefs.toString()));
                }
            } else {
                Iterator<String> it2 = unrefs.iterator();
                while (it2.hasNext()) {
                    String pkg = it2.next();
                    BroadcastConfigs.resetBrProxyOptCountLocked(pkg, userId, false);
                }
            }
        }
    }

    public static void proxyProcess(List<String> pkgList, int userId, boolean proxy, String module) {
        if (sService == null || pkgList == null || !isProxyProcessEnable()) {
            return;
        }
        if (!ProxyConfigs.CTRL_MODULE_SET.contains(module)) {
            VLog.e(TAG, "proxyProcess from a invalid module:" + module);
            return;
        }
        if (ProxyConfigs.DEBUG_BQ) {
            VLog.i(TAG, String.format("proxyProcess proxy=%s module=%s pkgList=%s", String.valueOf(proxy), module, pkgList.toString()));
        }
        synchronized (sService) {
            ArrayList<String> unrefs = updateRefsLocked(pkgList, userId, proxy, module, true);
            if (unrefs.isEmpty()) {
                return;
            }
            if (!proxy) {
                Iterator<BroadcastProxyQueue> it = BQ_CORES.iterator();
                while (it.hasNext()) {
                    BroadcastProxyQueue queue = it.next();
                    queue.unproxyBroadcastLocked(unrefs, userId, true);
                }
                if (ProxyConfigs.DEBUG_BQ) {
                    VLog.i(TAG, String.format("proxyProcess unrefs=%s", unrefs.toString()));
                }
            } else {
                Iterator<String> it2 = unrefs.iterator();
                while (it2.hasNext()) {
                    String pkg = it2.next();
                    BroadcastConfigs.resetBrProxyOptCountLocked(pkg, userId, true);
                }
            }
        }
    }

    public static void proxyPackage(String pkg, int userId, boolean proxy, String module) {
        if (pkg == null) {
            return;
        }
        List<String> pkgList = new ArrayList<>();
        pkgList.add(pkg);
        proxyPackage(pkgList, userId, proxy, module);
    }

    public static void proxyProcess(String pkg, int userId, boolean proxy, String module) {
        if (pkg == null) {
            return;
        }
        List<String> pkgList = new ArrayList<>();
        pkgList.add(pkg);
        proxyProcess(pkgList, userId, proxy, module);
    }

    private static ArrayList<String> updateRefsLocked(List<String> pkgList, int userId, boolean proxy, String module, boolean processMode) {
        HashMap<String, RefValue> refs = refsForUserIdLocked(userId, processMode);
        ArrayList<String> results = new ArrayList<>(pkgList.size());
        if (proxy) {
            for (String pkg : pkgList) {
                RefValue value = refs.get(pkg);
                if (value == null) {
                    value = new RefValue();
                    refs.put(pkg, value);
                }
                if (value.ref(module)) {
                    results.add(pkg);
                }
            }
        } else {
            for (String pkg2 : pkgList) {
                RefValue value2 = refs.get(pkg2);
                if (value2 != null) {
                    value2.unref(module);
                    if (value2.refCount() == 0) {
                        results.add(pkg2);
                        refs.remove(pkg2);
                    }
                }
            }
        }
        return results;
    }

    public static void register(BroadcastProxyQueue queue) {
        synchronized (BQ_CORES) {
            if (!BQ_CORES.contains(queue)) {
                BQ_CORES.add(queue);
                if (sService == null) {
                    sService = queue.mParent.mService;
                }
            }
        }
    }

    public static void setProxyPackageEnable(boolean enable) {
        ActivityManagerService activityManagerService = sService;
        if (activityManagerService == null) {
            return;
        }
        synchronized (activityManagerService) {
            if (sProxyPackageEnabled != enable) {
                sProxyPackageEnabled = enable;
                VLog.d(TAG, "setProxyPackageEnable = " + enable);
                updateEnableLocked(enable, false);
            }
        }
    }

    public static boolean isProxyPackageEnable() {
        return sProxyPackageEnabled;
    }

    public static void setProxyProcessEnable(boolean enable) {
        ActivityManagerService activityManagerService = sService;
        if (activityManagerService == null) {
            return;
        }
        synchronized (activityManagerService) {
            if (sProxyProcessEnabled != enable) {
                sProxyProcessEnabled = enable;
                VLog.d(TAG, "setProxyProcessEnable = " + enable);
                updateEnableLocked(enable, true);
            }
        }
    }

    public static boolean isProxyProcessEnable() {
        return sProxyProcessEnabled;
    }

    public static boolean isProxyEnable() {
        return sProxyPackageEnabled || sProxyProcessEnabled;
    }

    private static void updateEnableLocked(boolean enable, boolean processMode) {
        if (!enable) {
            SparseArray<HashMap<String, RefValue>> refs = processMode ? PROXY_REFS_FOR_PROCESS_MODE : PROXY_REFS_FOR_PACKAGE_MODE;
            for (int i = 0; i < refs.size(); i++) {
                ArrayList<String> proxyByPackage = new ArrayList<>();
                HashMap<String, RefValue> refsForPackage = refs.valueAt(i);
                int userId = refs.keyAt(i);
                for (String pkg : refsForPackage.keySet()) {
                    proxyByPackage.add(pkg);
                }
                if (!proxyByPackage.isEmpty()) {
                    VLog.d(TAG, "updateEnableLocked :" + proxyByPackage.toString());
                    Iterator<BroadcastProxyQueue> it = BQ_CORES.iterator();
                    while (it.hasNext()) {
                        BroadcastProxyQueue queue = it.next();
                        queue.unproxyBroadcastLocked(proxyByPackage, userId, processMode);
                    }
                }
            }
            refs.clear();
            if (!isProxyEnable()) {
                Iterator<BroadcastProxyQueue> it2 = BQ_CORES.iterator();
                while (it2.hasNext()) {
                    BroadcastProxyQueue queue2 = it2.next();
                    queue2.cleanupAllLocked();
                }
                BroadcastConfigs.BR_PROXY_COUNT.clear();
                BroadcastConfigs.BR_PROXY_OPT_COUNT.clear();
                BroadcastConfigs.PROC_START_BY_BR_HISTORY.clear();
            }
        }
    }

    public static void cleanupReceiverLocked(ReceiverList list) {
        if (isProxyEnable()) {
            Iterator<BroadcastProxyQueue> it = BQ_CORES.iterator();
            while (it.hasNext()) {
                BroadcastProxyQueue queue = it.next();
                queue.cleanupReceiverLocked(list);
            }
        }
    }

    public static void cleanupProcessRecordLocked(ProcessRecord app) {
        if (isProxyEnable()) {
            Iterator<BroadcastProxyQueue> it = BQ_CORES.iterator();
            while (it.hasNext()) {
                BroadcastProxyQueue queue = it.next();
                queue.cleanupProcessRecordLocked(app);
            }
            cleanupCtrlMoudleLocked(app);
        }
    }

    public static void handleBrProxyAbnormalLocked(String pkg, String process, int uid, int pid, int count) {
        HashMap<String, Integer> actions = new HashMap<>();
        Iterator<BroadcastProxyQueue> it = BQ_CORES.iterator();
        while (it.hasNext()) {
            BroadcastProxyQueue queue = it.next();
            queue.collectProcessActionsLocked(process, UserHandle.getUserId(uid), actions);
        }
        if (!actions.isEmpty()) {
            ArrayList<String> actionList = new ArrayList<>(actions.keySet());
            ArrayList<String> countList = new ArrayList<>(actions.size());
            for (Integer i : actions.values()) {
                countList.add(String.valueOf(i));
            }
            GatherManager.getInstance().gather(BroadcastConfigs.PROXY_BR_ABNORMAL, new Object[]{pkg, process, Integer.valueOf(uid), Integer.valueOf(pid), actionList, countList});
        }
    }

    public static HashMap<String, RefValue> refsForUserIdLocked(int userId, boolean processMode) {
        SparseArray<HashMap<String, RefValue>> arrayMap = processMode ? PROXY_REFS_FOR_PROCESS_MODE : PROXY_REFS_FOR_PACKAGE_MODE;
        HashMap<String, RefValue> refMap = arrayMap.get(userId);
        if (refMap == null) {
            HashMap<String, RefValue> refMap2 = new HashMap<>();
            arrayMap.put(userId, refMap2);
            return refMap2;
        }
        return refMap;
    }

    public static boolean hasRefsLocked(String pkg, int userId, boolean processMode) {
        HashMap<String, RefValue> refMap = refsForUserIdLocked(userId, processMode);
        return refMap.containsKey(pkg);
    }

    private static void cleanupCtrlMoudleLocked(ProcessRecord app) {
        String module = app.processName;
        if (module != null && ProxyConfigs.CTRL_MODULE_SET.contains(module)) {
            for (int i = 0; i < PROXY_REFS_FOR_PACKAGE_MODE.size(); i++) {
                ArrayList<String> proxyByPackage = new ArrayList<>();
                HashMap<String, RefValue> refsForPackage = PROXY_REFS_FOR_PACKAGE_MODE.valueAt(i);
                int userId = PROXY_REFS_FOR_PACKAGE_MODE.keyAt(i);
                for (String pkg : refsForPackage.keySet()) {
                    if (refsForPackage.get(pkg).contains(module)) {
                        proxyByPackage.add(pkg);
                    }
                }
                if (!proxyByPackage.isEmpty()) {
                    proxyByPackage = updateRefsLocked(proxyByPackage, app.userId, false, module, false);
                }
                if (!proxyByPackage.isEmpty()) {
                    VLog.d(TAG, "cleanupCtrlMoudleLocked :" + proxyByPackage.toString());
                    Iterator<BroadcastProxyQueue> it = BQ_CORES.iterator();
                    while (it.hasNext()) {
                        BroadcastProxyQueue queue = it.next();
                        queue.unproxyBroadcastLocked(proxyByPackage, userId, false);
                    }
                }
            }
            for (int i2 = 0; i2 < PROXY_REFS_FOR_PROCESS_MODE.size(); i2++) {
                ArrayList<String> proxyProcess = new ArrayList<>();
                HashMap<String, RefValue> refsForProcess = PROXY_REFS_FOR_PROCESS_MODE.valueAt(i2);
                int userId2 = PROXY_REFS_FOR_PROCESS_MODE.keyAt(i2);
                for (String process : refsForProcess.keySet()) {
                    if (refsForProcess.get(process).contains(module)) {
                        proxyProcess.add(process);
                    }
                }
                if (!proxyProcess.isEmpty()) {
                    proxyProcess = updateRefsLocked(proxyProcess, userId2, false, module, true);
                }
                if (!proxyProcess.isEmpty()) {
                    VLog.d(TAG, "cleanupCtrlMoudleLocked :" + proxyProcess.toString());
                    Iterator<BroadcastProxyQueue> it2 = BQ_CORES.iterator();
                    while (it2.hasNext()) {
                        BroadcastProxyQueue queue2 = it2.next();
                        queue2.unproxyBroadcastLocked(proxyProcess, userId2, true);
                    }
                }
            }
        }
    }

    public static boolean setBundle(String name, Bundle bundle) {
        boolean configLocked;
        ActivityManagerService activityManagerService = sService;
        if (activityManagerService == null) {
            return false;
        }
        synchronized (activityManagerService) {
            configLocked = BroadcastConfigs.setConfigLocked(name, bundle);
        }
        return configLocked;
    }

    private static void dumpStatsLocked(PrintWriter pw) {
        pw.print("PROXY_ENABLED:");
        pw.println(String.valueOf(isProxyEnable()));
        pw.print("PROXY_PACKAGE_ENABLED:");
        pw.println(String.valueOf(isProxyPackageEnable()));
        pw.print("PROXY_PROCESS_ENABLED:");
        pw.println(String.valueOf(isProxyProcessEnable()));
        BroadcastConfigs.dumpConfigs(pw);
        Iterator<BroadcastProxyQueue> it = BQ_CORES.iterator();
        while (it.hasNext()) {
            BroadcastProxyQueue queue = it.next();
            queue.dumpLocked(pw);
        }
        if (PROXY_REFS_FOR_PACKAGE_MODE.size() > 0) {
            for (int i = 0; i < PROXY_REFS_FOR_PACKAGE_MODE.size(); i++) {
                int userId = PROXY_REFS_FOR_PACKAGE_MODE.keyAt(i);
                HashMap<String, RefValue> refs = PROXY_REFS_FOR_PACKAGE_MODE.valueAt(i);
                if (!refs.isEmpty()) {
                    pw.print("PROXY_REFS_FOR_PACKAGE_MODE userId=");
                    pw.print(userId);
                    pw.println(":");
                    for (String pkg : refs.keySet()) {
                        pw.print('\t');
                        pw.print(pkg);
                        pw.print(" ");
                        pw.println(refs.get(pkg).toString());
                    }
                }
            }
        }
        if (PROXY_REFS_FOR_PROCESS_MODE.size() > 0) {
            for (int i2 = 0; i2 < PROXY_REFS_FOR_PROCESS_MODE.size(); i2++) {
                int userId2 = PROXY_REFS_FOR_PROCESS_MODE.keyAt(i2);
                HashMap<String, RefValue> refs2 = PROXY_REFS_FOR_PROCESS_MODE.valueAt(i2);
                if (!refs2.isEmpty()) {
                    pw.print("PROXY_REFS_FOR_PROCESS_MODE userId=");
                    pw.print(userId2);
                    pw.println(":");
                    for (String pkg2 : refs2.keySet()) {
                        pw.print('\t');
                        pw.print(pkg2);
                        pw.print(" ");
                        pw.println(refs2.get(pkg2).toString());
                    }
                }
            }
        }
        if (BroadcastConfigs.BR_PROXY_COUNT.size() > 0) {
            for (int i3 = 0; i3 < BroadcastConfigs.BR_PROXY_COUNT.size(); i3++) {
                int userId3 = BroadcastConfigs.BR_PROXY_COUNT.keyAt(i3);
                HashMap<String, Integer> map = BroadcastConfigs.BR_PROXY_COUNT.valueAt(i3);
                if (!map.isEmpty()) {
                    pw.print("BR_PROXY_COUNT userId=");
                    pw.print(userId3);
                    pw.println(":");
                    for (String process : map.keySet()) {
                        pw.print('\t');
                        pw.print(process);
                        pw.print("->");
                        pw.println(map.get(process).toString());
                    }
                }
            }
        }
        if (BroadcastConfigs.BR_PROXY_OPT_COUNT.size() > 0) {
            for (int i4 = 0; i4 < BroadcastConfigs.BR_PROXY_OPT_COUNT.size(); i4++) {
                int userId4 = BroadcastConfigs.BR_PROXY_OPT_COUNT.keyAt(i4);
                HashMap<String, DualInt> map2 = BroadcastConfigs.BR_PROXY_OPT_COUNT.valueAt(i4);
                if (!map2.isEmpty()) {
                    pw.print("BR_PROXY_OPT_COUNT userId=");
                    pw.print(userId4);
                    pw.println(":");
                    for (String pkg3 : map2.keySet()) {
                        pw.print('\t');
                        pw.print(pkg3);
                        pw.print("->");
                        pw.println(map2.get(pkg3).toString());
                    }
                }
            }
        }
        if (!BroadcastConfigs.PROC_START_BY_BR_HISTORY.isEmpty()) {
            pw.println("PROC_START_BY_BR_HISTORY:");
            Iterator<Pair<String, String>> it2 = BroadcastConfigs.PROC_START_BY_BR_HISTORY.iterator();
            while (it2.hasNext()) {
                Pair<String, String> pair = it2.next();
                pw.print('\t');
                pw.print((String) pair.first);
                pw.print(" by ");
                pw.println((String) pair.second);
            }
        }
    }

    public static void dumpBroadcastByProcesses(PrintWriter pw, HashSet<String> processes, int userId) {
        ActivityManagerService activityManagerService = sService;
        if (activityManagerService == null) {
            return;
        }
        synchronized (activityManagerService) {
            Iterator<BroadcastProxyQueue> it = BQ_CORES.iterator();
            while (it.hasNext()) {
                BroadcastProxyQueue queue = it.next();
                queue.dumpByProcessesLocked(pw, processes, userId);
            }
        }
    }

    public static void dump(PrintWriter pw, String[] args, int startIndex) {
        if (sService == null) {
            pw.println("sService == null");
            return;
        }
        int argsLen = args.length - startIndex;
        if (argsLen >= 1 && args[startIndex].equals("--configs")) {
            synchronized (sService) {
                dumpStatsLocked(pw);
            }
        }
    }
}