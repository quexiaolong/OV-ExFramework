package com.vivo.services.proxy.transact;

import android.os.BinderProxy;
import android.os.IBinder;
import android.os.ITransactProxy;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.SparseArray;
import com.vivo.common.utils.VLog;
import com.vivo.services.proxy.DualInt;
import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.proxy.RefValue;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.appmng.AppManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class TransactProxyManager {
    public static final String TAG = "TransactProxyManager";
    public static boolean CODE_READY = true;
    private static boolean FEATURE_ENABLED = SystemProperties.getBoolean("persist.sys.proxy_transact", true);
    private static final SparseArray<HashMap<String, HashMap<String, SparseArray<DualInt>>>> TRANSACT_PROXY_OPT_COUNT = new SparseArray<>();
    private static final SparseArray<HashMap<String, RefValue>> PROXY_REFS = new SparseArray<>();
    private static final HashMap<IBinder, BpTransactProxy> BINBDERS = new HashMap<>();
    private static final ArrayList<String> WHITE_LIST = new ArrayList<>();
    public static final int MY_PID = Process.myPid();
    private static final Object LOCK = new Object();
    private static final DeatCallback DEATH_CALLBACK = new DeatCallback();

    public static void addBinderProxy(String descriptor, IBinder bpBinder, int pid, int uid, boolean autoRemove) {
        ProcessInfo app;
        if (!CODE_READY || bpBinder == null || pid == MY_PID || pid <= 0 || descriptor == null) {
            return;
        }
        if ((uid <= 0 || uid >= 10000) && (bpBinder instanceof BinderProxy) && (app = AppManager.getInstance().getProcessInfo(pid)) != null && app.isAlive() && app.mUid >= 10000) {
            if (app.isSystemApp() && !isWhiteList(app.mPkgName)) {
                return;
            }
            if (autoRemove && !linkToDeath(bpBinder)) {
                return;
            }
            synchronized (LOCK) {
                if (BINBDERS.containsKey(bpBinder)) {
                    return;
                }
                BpTransactProxy proxy = TransactProxyFactory.create(descriptor, bpBinder, app.mPkgName, app.mProcName, app.mUid, pid);
                BINBDERS.put(bpBinder, proxy);
                ((BinderProxy) bpBinder).setTransactProxy(proxy);
                if (hasRefsLocked(proxy.mPkgName, proxy.mUserId)) {
                    proxy.proxy(true);
                }
            }
        }
    }

    public static void removeBinderProxy(IBinder bpBinder) {
        if (!CODE_READY || bpBinder == null || !(bpBinder instanceof BinderProxy)) {
            return;
        }
        synchronized (LOCK) {
            BinderProxy binderProxy = (BinderProxy) bpBinder;
            if (binderProxy.getTransactProxy() == null) {
                return;
            }
            BpTransactProxy proxy = BINBDERS.get(bpBinder);
            if (proxy != null) {
                binderProxy.setTransactProxy((ITransactProxy) null);
                proxy.clearTransactions();
                BINBDERS.remove(bpBinder);
            }
        }
    }

    private static boolean linkToDeath(IBinder binder) {
        try {
            binder.linkToDeath(DEATH_CALLBACK, 0);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DeatCallback implements IBinder.DeathRecipient {
        private DeatCallback() {
        }

        public void binderDied(IBinder binder) {
            TransactProxyManager.removeBinderProxy(binder);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
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

    public static void proxyPackage(List<String> pkgList, int userId, boolean proxy, String module) {
        if (!isEnable() || pkgList == null || userId == -1) {
            return;
        }
        if (!ProxyConfigs.CTRL_MODULE_SET.contains(module)) {
            VLog.e(TAG, "proxyPackage from a invalid module:" + module);
            return;
        }
        if (ProxyConfigs.DEBUG_BQ) {
            VLog.i(TAG, String.format("proxyPackage proxy=%s module=%s pkgList=%s", String.valueOf(proxy), module, pkgList.toString()));
        }
        synchronized (LOCK) {
            ArrayList<String> results = updateRefsLocked(pkgList, userId, proxy, module);
            if (results.isEmpty()) {
                return;
            }
            if (proxy) {
                for (BpTransactProxy bpBinder : BINBDERS.values()) {
                    if (bpBinder.mUserId == userId && results.contains(bpBinder.mPkgName)) {
                        bpBinder.proxy(true);
                        resetTransactOptCount(bpBinder.mDescriptor, bpBinder.mProcess, bpBinder.mUserId);
                    }
                }
            } else {
                for (BpTransactProxy bpBinder2 : BINBDERS.values()) {
                    if (bpBinder2.mUserId == userId && results.contains(bpBinder2.mPkgName)) {
                        bpBinder2.proxy(false);
                    }
                }
            }
        }
    }

    public static void setEnable(boolean enable) {
        if (FEATURE_ENABLED != enable) {
            boolean oldEnabled = isEnable();
            FEATURE_ENABLED = enable;
            boolean nowEnabled = isEnable();
            if (oldEnabled != nowEnabled) {
                synchronized (LOCK) {
                    if (!nowEnabled) {
                        PROXY_REFS.clear();
                        TRANSACT_PROXY_OPT_COUNT.clear();
                    }
                    for (BpTransactProxy bpBinder : BINBDERS.values()) {
                        if (bpBinder.mProxy) {
                            bpBinder.proxy(false);
                        }
                    }
                }
            }
        }
    }

    private static boolean isEnable() {
        return FEATURE_ENABLED && CODE_READY;
    }

    private static ArrayList<String> updateRefsLocked(List<String> pkgList, int userId, boolean proxy, String module) {
        HashMap<String, RefValue> refs = refsForUserIdLocked(userId);
        ArrayList<String> result = new ArrayList<>(pkgList.size());
        if (proxy) {
            for (String pkg : pkgList) {
                RefValue value = refs.get(pkg);
                if (value == null) {
                    value = new RefValue();
                    refs.put(pkg, value);
                }
                if (value.ref(module)) {
                    result.add(pkg);
                }
            }
        } else {
            for (String pkg2 : pkgList) {
                RefValue value2 = refs.get(pkg2);
                if (value2 != null) {
                    value2.unref(module);
                    if (value2.refCount() == 0) {
                        result.add(pkg2);
                    }
                }
            }
            Iterator<String> it = result.iterator();
            while (it.hasNext()) {
                refs.remove(it.next());
            }
        }
        return result;
    }

    private static HashMap<String, RefValue> refsForUserIdLocked(int userId) {
        HashMap<String, RefValue> refMap = PROXY_REFS.get(userId);
        if (refMap == null) {
            HashMap<String, RefValue> refMap2 = new HashMap<>();
            PROXY_REFS.put(userId, refMap2);
            return refMap2;
        }
        return refMap;
    }

    public static boolean hasRefsLocked(String pkg, int userId) {
        HashMap<String, RefValue> refMap = refsForUserIdLocked(userId);
        return refMap.containsKey(pkg);
    }

    public static void updateTransactOptCount(String descriptor, String process, int userId, int code, int incCount) {
        synchronized (TRANSACT_PROXY_OPT_COUNT) {
            HashMap<String, HashMap<String, SparseArray<DualInt>>> descriptorsForUserId = TRANSACT_PROXY_OPT_COUNT.get(userId);
            if (descriptorsForUserId == null) {
                descriptorsForUserId = new HashMap<>();
                TRANSACT_PROXY_OPT_COUNT.put(userId, descriptorsForUserId);
            }
            HashMap<String, SparseArray<DualInt>> procInfos = descriptorsForUserId.get(descriptor);
            if (procInfos == null) {
                procInfos = new HashMap<>();
                descriptorsForUserId.put(descriptor, procInfos);
            }
            SparseArray<DualInt> codeInfos = procInfos.get(process);
            if (codeInfos == null) {
                codeInfos = new SparseArray<>();
                procInfos.put(process, codeInfos);
            }
            DualInt dint = codeInfos.get(code);
            if (dint == null) {
                dint = new DualInt();
                codeInfos.put(code, dint);
            }
            dint.mInt1 += incCount;
            dint.mInt2++;
        }
    }

    public static void resetTransactOptCount(String descriptor, String process, int userId) {
        synchronized (TRANSACT_PROXY_OPT_COUNT) {
            HashMap<String, HashMap<String, SparseArray<DualInt>>> descriptorsForUserId = TRANSACT_PROXY_OPT_COUNT.get(userId);
            if (descriptorsForUserId == null) {
                return;
            }
            HashMap<String, SparseArray<DualInt>> procInfos = descriptorsForUserId.get(descriptor);
            if (procInfos != null) {
                procInfos.remove(process);
            }
        }
    }

    public static void setWhiteList(ArrayList<String> list) {
        synchronized (WHITE_LIST) {
            WHITE_LIST.clear();
            WHITE_LIST.addAll(list);
        }
    }

    public static ArrayList<String> getWhiteList() {
        ArrayList<String> arrayList;
        synchronized (WHITE_LIST) {
            arrayList = WHITE_LIST;
        }
        return arrayList;
    }

    public static boolean isWhiteList(String pkg) {
        boolean contains;
        synchronized (WHITE_LIST) {
            contains = WHITE_LIST.contains(pkg);
        }
        return contains;
    }

    public static void dump(PrintWriter pw, String[] args, int startIndex) {
        int argsLen = args.length - startIndex;
        if (argsLen >= 1 && args[startIndex].equals("--transactions")) {
            dumpTransactions(pw);
        } else if (argsLen >= 1 && args[startIndex].equals("--binders")) {
            dumpBinders(pw);
        }
    }

    public static void dumpTransactions(PrintWriter pw) {
        synchronized (LOCK) {
            HashMap<String, ArrayList<BpTransactProxy>> dumpBinders = new HashMap<>();
            for (BpTransactProxy binder : BINBDERS.values()) {
                synchronized (binder) {
                    if (binder.mProxy && !binder.mTransactions.isEmpty()) {
                        ArrayList<BpTransactProxy> list = dumpBinders.get(binder.mDescriptor);
                        if (list == null) {
                            list = new ArrayList<>();
                            dumpBinders.put(binder.mDescriptor, list);
                        }
                        list.add(binder);
                    }
                }
            }
            if (!dumpBinders.isEmpty()) {
                for (String key : dumpBinders.keySet()) {
                    pw.print(key);
                    pw.println(":");
                    Iterator<BpTransactProxy> it = dumpBinders.get(key).iterator();
                    while (it.hasNext()) {
                        BpTransactProxy binder2 = it.next();
                        synchronized (binder2) {
                            if (binder2.mProxy && !binder2.mTransactions.isEmpty()) {
                                pw.print('\t');
                                pw.print(binder2.getID());
                                pw.println(":");
                                Iterator<Transaction> it2 = binder2.mTransactions.iterator();
                                while (it2.hasNext()) {
                                    Transaction t = it2.next();
                                    pw.print('\t');
                                    pw.print('\t');
                                    pw.println(t.toString());
                                }
                            }
                        }
                    }
                }
            }
            if (PROXY_REFS.size() > 0) {
                for (int i = 0; i < PROXY_REFS.size(); i++) {
                    int userId = PROXY_REFS.keyAt(i);
                    HashMap<String, RefValue> refs = PROXY_REFS.valueAt(i);
                    if (!refs.isEmpty()) {
                        pw.print("PROXY_REFS userId=");
                        pw.print(userId);
                        pw.println("->");
                        for (String pkg : refs.keySet()) {
                            pw.print('\t');
                            pw.print(pkg);
                            pw.print(" ");
                            pw.println(refs.get(pkg).toString());
                        }
                    }
                }
            }
        }
        synchronized (TRANSACT_PROXY_OPT_COUNT) {
            if (TRANSACT_PROXY_OPT_COUNT.size() > 0) {
                pw.println("TRANSACT_PROXY_OPT_COUNT:");
                for (int i2 = 0; i2 < TRANSACT_PROXY_OPT_COUNT.size(); i2++) {
                    int userId2 = TRANSACT_PROXY_OPT_COUNT.keyAt(i2);
                    HashMap<String, HashMap<String, SparseArray<DualInt>>> descriptors = TRANSACT_PROXY_OPT_COUNT.valueAt(i2);
                    if (!descriptors.isEmpty()) {
                        for (String descriptor : descriptors.keySet()) {
                            HashMap<String, SparseArray<DualInt>> processes = descriptors.get(descriptor);
                            if (!processes.isEmpty()) {
                                pw.print(descriptor);
                                pw.print(" userId=");
                                pw.print(userId2);
                                pw.println(":");
                                for (String process : processes.keySet()) {
                                    pw.print('\t');
                                    pw.println(process);
                                    SparseArray<DualInt> codeInfos = processes.get(process);
                                    for (int j = 0; j < codeInfos.size(); j++) {
                                        int code = codeInfos.keyAt(j);
                                        DualInt dint = codeInfos.valueAt(j);
                                        pw.print("\t\tcode=");
                                        pw.print(code);
                                        pw.print("->");
                                        pw.println(dint.toString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void dumpBinders(PrintWriter pw) {
        synchronized (LOCK) {
            for (BpTransactProxy binder : BINBDERS.values()) {
                pw.print('\t');
                pw.println(binder.toString());
            }
            pw.println("isEnable:" + String.valueOf(isEnable()));
            pw.println("FEATURE_ENABLED:" + String.valueOf(FEATURE_ENABLED));
            pw.println("CODE_READY:" + String.valueOf(CODE_READY));
        }
    }
}