package com.vivo.services.proxy.game;

import android.util.SparseArray;
import com.android.server.am.BroadcastProxyManager;
import com.android.server.am.VivoActiveServiceImpl;
import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.proxy.broadcast.BroadcastConfigs;
import com.vivo.services.rms.GameOptManager;
import com.vivo.services.rms.Platform;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/* loaded from: classes.dex */
public class GameSceneProxyManager {
    public static final int SERVICE_MAX_DELYEED_TIME = 3600000;
    private static VivoActiveServiceImpl sActiveServiceImpl;
    private static final SparseArray<HashSet<String>> PROXY_PROCESSES = new SparseArray<>();
    private static int sProxySize = 0;

    public static void initialize(VivoActiveServiceImpl vas) {
        sActiveServiceImpl = vas;
    }

    public static void addProcessBroadcastProxy(String process, int userId) {
        BroadcastProxyManager.proxyProcess(process, userId, true, ProxyConfigs.CTRL_MODULE_GAME);
        synchronized (PROXY_PROCESSES) {
            HashSet<String> processes = PROXY_PROCESSES.get(userId);
            if (processes == null) {
                processes = new HashSet<>();
                PROXY_PROCESSES.put(userId, processes);
            }
            if (!processes.contains(process)) {
                processes.add(process);
                sProxySize++;
            }
        }
    }

    public static void onProcessAttached(String process, int userId) {
        rescheduleBroadcast(process, userId);
    }

    public static void onGameExit() {
        rescheduleBroadcast();
        VivoActiveServiceImpl vivoActiveServiceImpl = sActiveServiceImpl;
        if (vivoActiveServiceImpl != null) {
            vivoActiveServiceImpl.rescheduleServiceProxyedByGameScene();
        }
    }

    private static boolean isEnabled(String pkgName) {
        return BroadcastProxyManager.isProxyProcessEnable() && !Platform.isOverSeas() && Platform.isTwoBigcoreDevice() && GameOptManager.isGamePlaying() && pkgName != null && pkgName.equals(GameOptManager.getGameName());
    }

    public static boolean isBroadcastAllowRestartLocked(String pkgName, String process) {
        return !isEnabled(pkgName) || BroadcastConfigs.PROCESS_ALLOW_RESTART_IN_GAME.contains(process);
    }

    public static boolean isServiceAllowRestartLocked(String pkgName, String process, String componentName) {
        return !isEnabled(pkgName) || BroadcastConfigs.PROCESS_ALLOW_RESTART_IN_GAME.contains(process) || BroadcastConfigs.COMPONENT_ALLOW_RESTART_IN_GAME.contains(componentName);
    }

    private static void rescheduleBroadcast() {
        SparseArray<ArrayList<String>> processes = null;
        synchronized (PROXY_PROCESSES) {
            if (sProxySize == 0) {
                return;
            }
            for (int i = 0; i < PROXY_PROCESSES.size(); i++) {
                if (!PROXY_PROCESSES.valueAt(i).isEmpty()) {
                    if (processes == null) {
                        processes = new SparseArray<>();
                    }
                    processes.put(PROXY_PROCESSES.keyAt(i), new ArrayList<>(PROXY_PROCESSES.valueAt(i)));
                }
                PROXY_PROCESSES.valueAt(i).clear();
            }
            sProxySize = 0;
            if (processes != null && processes.size() > 0) {
                for (int i2 = 0; i2 < processes.size(); i2++) {
                    BroadcastProxyManager.proxyProcess((List<String>) processes.valueAt(i2), processes.keyAt(i2), false, ProxyConfigs.CTRL_MODULE_GAME);
                }
            }
        }
    }

    private static void rescheduleBroadcast(String process, int userId) {
        synchronized (PROXY_PROCESSES) {
            HashSet<String> processes = PROXY_PROCESSES.get(userId);
            if (processes != null && processes.contains(process)) {
                sProxySize--;
                processes.remove(process);
                if (1 != 0) {
                    BroadcastProxyManager.proxyProcess(process, userId, false, ProxyConfigs.CTRL_MODULE_GAME);
                }
            }
        }
    }

    public static void dump(PrintWriter pw) {
        pw.println("GameSceneProxyManager:");
        dumpBroadcast(pw);
    }

    public static void dumpBroadcast(PrintWriter pw) {
        SparseArray<HashSet<String>> processes = null;
        synchronized (PROXY_PROCESSES) {
            if (sProxySize == 0) {
                return;
            }
            for (int i = 0; i < PROXY_PROCESSES.size(); i++) {
                if (!PROXY_PROCESSES.valueAt(i).isEmpty()) {
                    if (processes == null) {
                        processes = new SparseArray<>();
                    }
                    processes.put(PROXY_PROCESSES.keyAt(i), new HashSet<>(PROXY_PROCESSES.valueAt(i)));
                }
            }
            if (processes != null && processes.size() > 0) {
                for (int i2 = 0; i2 < processes.size(); i2++) {
                    BroadcastProxyManager.dumpBroadcastByProcesses(pw, processes.valueAt(i2), processes.keyAt(i2));
                }
            }
        }
    }
}