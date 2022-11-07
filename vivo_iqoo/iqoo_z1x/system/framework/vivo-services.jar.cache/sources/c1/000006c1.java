package com.vivo.services.proxy.broadcast;

import android.os.Bundle;
import android.util.Pair;
import android.util.SparseArray;
import com.vivo.services.proxy.DualInt;
import com.vivo.services.proxy.ProxyConfigs;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/* loaded from: classes.dex */
public class BroadcastConfigs {
    public static final String CONFIG_APP_DISCARD_ACTIONS_NAME = "bq_discard_actions";
    public static final String CONFIG_APP_REMOVE_ACTIONS_NAME = "bq_remove_actions";
    public static final String CONFIG_APP_RESTART_SELF_SET_NAME = "bq_app_restart_self_set";
    public static final String CONFIG_COMPONENT_ALLOW_RESTART_IN_GAME = "bq_component_allow_restart_in_game";
    public static final String CONFIG_DEFAULT_DISCARD_ACTIONS_NAME = "bq_defualt_discard_actions";
    public static final String CONFIG_DEFAULT_REMOVE_ACTIONSS_NAME = "bq_default_remove_actions";
    public static final String CONFIG_PAIR_ACTIONS_NAME = "bq_pair_actions";
    public static final String CONFIG_PROCESS_ALLOW_RESTART_IN_GAME = "bq_process_allow_restart_in_game";
    private static final int MAX_PROC_START_BY_BR_HISTORY_SIZE = 32;
    public static final String PROXY_BR_ABNORMAL = "proxy_br_abnormal";
    public static final int PROXY_BR_ABNORMAL_SIZE = 240;
    public static boolean DEBUG_BQ = false;
    public static final PairList<String> PAIR_ACTIONS = new PairList<>();
    public static final HashSet<String> DEFAULT_REMOVE_ACTIONS = new HashSet<>();
    public static final HashSet<String> DEFAULT_DISCARD_ACTIONS = new HashSet<>();
    public static final HashMap<String, HashSet<String>> APP_REMOVE_ACTIONS = new HashMap<>();
    public static final HashMap<String, HashSet<String>> APP_DISCARD_ACTIONS = new HashMap<>();
    public static final HashSet<String> APP_RESTART_SELF_SET = new HashSet<>();
    public static final HashSet<String> PROCESS_ALLOW_RESTART_IN_GAME = new HashSet<>();
    public static final HashSet<String> COMPONENT_ALLOW_RESTART_IN_GAME = new HashSet<>();
    public static final LinkedList<Pair<String, String>> PROC_START_BY_BR_HISTORY = new LinkedList<>();
    public static final SparseArray<HashMap<String, Integer>> BR_PROXY_COUNT = new SparseArray<>();
    public static final SparseArray<HashMap<String, DualInt>> BR_PROXY_OPT_COUNT = new SparseArray<>();

    static {
        PAIR_ACTIONS.put("android.intent.action.SCREEN_ON", "android.intent.action.SCREEN_OFF");
        PAIR_ACTIONS.put("android.intent.action.MEDIA_MOUNTED", "android.intent.action.MEDIA_UNMOUNTED");
        PAIR_ACTIONS.put("android.intent.action.ACTION_POWER_CONNECTED", "android.intent.action.ACTION_POWER_DISCONNECTED");
        PAIR_ACTIONS.put("android.bluetooth.device.action.ACL_DISCONNECTED", "android.bluetooth.device.action.ACL_CONNECTED");
        DEFAULT_DISCARD_ACTIONS.add("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        addAction("com.bbk.appstore", "android.intent.action.PACKAGE_ADDED", APP_REMOVE_ACTIONS);
        addAction("com.bbk.appstore", "android.intent.action.PACKAGE_REMOVED", APP_REMOVE_ACTIONS);
        addAction("com.bbk.appstore", "android.intent.action.PACKAGE_CHANGED", APP_REMOVE_ACTIONS);
        addAction("com.bbk.appstore", "android.intent.action.PACKAGE_REPLACED", APP_REMOVE_ACTIONS);
        PROCESS_ALLOW_RESTART_IN_GAME.add("com.android.BBKClock");
        PROCESS_ALLOW_RESTART_IN_GAME.add("com.bbk.calendar");
    }

    public static int getBrProxyCountLocked(String process, int userId) {
        HashMap<String, Integer> mapForUserId = BR_PROXY_COUNT.get(userId);
        if (mapForUserId == null) {
            return 0;
        }
        return mapForUserId.getOrDefault(process, 0).intValue();
    }

    public static void setBrProxyCountLocked(String process, int userId, int count) {
        HashMap<String, Integer> mapForUserId = BR_PROXY_COUNT.get(userId);
        if (mapForUserId == null) {
            mapForUserId = new HashMap<>();
            BR_PROXY_COUNT.put(userId, mapForUserId);
        }
        if (count == 0) {
            mapForUserId.remove(process);
        } else {
            mapForUserId.put(process, Integer.valueOf(count));
        }
    }

    public static void incBrProxyCountsLocked(String process, int userId, int incCount) {
        HashMap<String, Integer> mapForUserId = BR_PROXY_COUNT.get(userId);
        if (mapForUserId == null) {
            mapForUserId = new HashMap<>();
            BR_PROXY_COUNT.put(userId, mapForUserId);
        }
        int count = mapForUserId.getOrDefault(process, 0).intValue() + incCount;
        if (count == 0) {
            mapForUserId.remove(process);
        } else {
            mapForUserId.put(process, Integer.valueOf(count));
        }
    }

    public static void updateBrProxyOptCountLocked(String process, int userId, int incCount) {
        HashMap<String, DualInt> mapForUserId = BR_PROXY_OPT_COUNT.get(userId);
        if (mapForUserId == null) {
            mapForUserId = new HashMap<>();
            BR_PROXY_OPT_COUNT.put(userId, mapForUserId);
        }
        DualInt dint = mapForUserId.get(process);
        if (dint == null) {
            dint = new DualInt();
            mapForUserId.put(process, dint);
        }
        dint.mInt1 += incCount;
        dint.mInt2++;
    }

    public static void resetBrProxyOptCountLocked(String pkg, int userId, boolean byProcess) {
        HashMap<String, DualInt> mapForUserId = BR_PROXY_OPT_COUNT.get(userId);
        if (mapForUserId != null) {
            if (byProcess) {
                mapForUserId.remove(pkg);
                return;
            }
            Iterator<String> it = mapForUserId.keySet().iterator();
            while (it.hasNext()) {
                String process = it.next();
                if (process != null && process.startsWith(pkg)) {
                    it.remove();
                }
            }
        }
    }

    public static void addProcStartedHistoryLocked(String process, String action) {
        Pair<String, String> pair = Pair.create(process, action);
        if (!PROC_START_BY_BR_HISTORY.contains(pair)) {
            PROC_START_BY_BR_HISTORY.add(pair);
            int size = PROC_START_BY_BR_HISTORY.size();
            if (size > 32) {
                PROC_START_BY_BR_HISTORY.remove(0);
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static boolean setConfigLocked(String name, Bundle bundle) {
        char c;
        switch (name.hashCode()) {
            case -1354596814:
                if (name.equals(CONFIG_APP_REMOVE_ACTIONS_NAME)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -924017299:
                if (name.equals(CONFIG_APP_RESTART_SELF_SET_NAME)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -778543508:
                if (name.equals(CONFIG_APP_DISCARD_ACTIONS_NAME)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -503647034:
                if (name.equals(CONFIG_DEFAULT_DISCARD_ACTIONS_NAME)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -473108856:
                if (name.equals(CONFIG_PAIR_ACTIONS_NAME)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -273461786:
                if (name.equals(CONFIG_PROCESS_ALLOW_RESTART_IN_GAME)) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 1149503988:
                if (name.equals(CONFIG_COMPONENT_ALLOW_RESTART_IN_GAME)) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 1836891120:
                if (name.equals(CONFIG_DEFAULT_REMOVE_ACTIONSS_NAME)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                updatePairActionsLocked(bundle);
                return true;
            case 1:
                ProxyConfigs.updateCollectionLocked(DEFAULT_REMOVE_ACTIONS, bundle);
                return true;
            case 2:
                updateAppReplaceableActionsLocked(APP_REMOVE_ACTIONS, bundle);
                return true;
            case 3:
                ProxyConfigs.updateCollectionLocked(DEFAULT_DISCARD_ACTIONS, bundle);
                return true;
            case 4:
                updateAppReplaceableActionsLocked(APP_DISCARD_ACTIONS, bundle);
                return true;
            case 5:
                ProxyConfigs.updateCollectionLocked(APP_RESTART_SELF_SET, bundle);
                return true;
            case 6:
                ProxyConfigs.updateCollectionLocked(PROCESS_ALLOW_RESTART_IN_GAME, bundle);
                return true;
            case 7:
                ProxyConfigs.updateCollectionLocked(COMPONENT_ALLOW_RESTART_IN_GAME, bundle);
                return true;
            default:
                return false;
        }
    }

    private static void updatePairActionsLocked(Bundle bundle) {
        ArrayList<String> values = bundle.getStringArrayList(ProxyConfigs.CONFIG_VALUES_KEY);
        int op = bundle.getInt(ProxyConfigs.CONFIG_OP_KEY, 0);
        if (op == 0) {
            PAIR_ACTIONS.clear();
        }
        if (values != null && !values.isEmpty() && (values.size() + 1) % 2 == 0) {
            for (int i = 0; i < values.size(); i += 2) {
                if (op == 2) {
                    PAIR_ACTIONS.removeKeyorValue(values.get(i));
                } else {
                    PAIR_ACTIONS.put(values.get(i), values.get(i + 1));
                }
            }
        }
    }

    private static void updateAppReplaceableActionsLocked(HashMap<String, HashSet<String>> configs, Bundle bundle) {
        Set<String> pkgSet = bundle.keySet();
        if (pkgSet.isEmpty()) {
            return;
        }
        for (String pkg : pkgSet) {
            Bundle tmpBundle = bundle.getBundle(pkg);
            int op = tmpBundle.getInt(ProxyConfigs.CONFIG_OP_KEY, 0);
            ArrayList<String> values = tmpBundle.getStringArrayList(ProxyConfigs.CONFIG_VALUES_KEY);
            if (values == null || values.isEmpty()) {
                if (op == 0) {
                    configs.remove(pkg);
                }
            } else {
                HashSet<String> configValues = configs.get(pkg);
                if (configValues == null) {
                    configValues = new HashSet<>();
                    configs.put(pkg, configValues);
                }
                if (op == 0) {
                    configValues.clear();
                }
                if (op == 2) {
                    configValues.removeAll(values);
                    if (configValues.isEmpty()) {
                        configs.remove(pkg);
                    }
                } else {
                    configValues.addAll(values);
                }
            }
        }
    }

    private static void addAction(String pkg, String action, HashMap<String, HashSet<String>> container) {
        HashSet<String> actions = container.get(pkg);
        if (actions == null) {
            actions = new HashSet<>();
            container.put(pkg, actions);
        }
        actions.add(action);
    }

    public static int sum(Collection<Integer> collection) {
        int sum = 0;
        for (Integer i : collection) {
            sum += i.intValue();
        }
        return sum;
    }

    public static void dumpConfigs(PrintWriter pw) {
        if (!ProxyConfigs.CTRL_MODULE_SET.isEmpty()) {
            pw.println("CTRL_MODULE_SET:");
            Iterator<String> it = ProxyConfigs.CTRL_MODULE_SET.iterator();
            while (it.hasNext()) {
                String action = it.next();
                pw.print('\t');
                pw.println(action);
            }
        }
        if (!PAIR_ACTIONS.isEmpty()) {
            pw.println("PAIR_ACTIONS:");
            Iterator<Pair<K, K>> it2 = PAIR_ACTIONS.iterator();
            while (it2.hasNext()) {
                Pair<String, String> pair = (Pair) it2.next();
                pw.print('\t');
                pw.print((String) pair.first);
                pw.print("<->");
                pw.println((String) pair.second);
            }
        }
        if (!DEFAULT_REMOVE_ACTIONS.isEmpty()) {
            pw.println("DEFAULT_REMOVE_ACTIONS:");
            Iterator<String> it3 = DEFAULT_REMOVE_ACTIONS.iterator();
            while (it3.hasNext()) {
                String action2 = it3.next();
                pw.print('\t');
                pw.println(action2);
            }
        }
        if (!APP_REMOVE_ACTIONS.isEmpty()) {
            pw.println("APP_REMOVE_ACTIONS:");
            for (String key : APP_REMOVE_ACTIONS.keySet()) {
                pw.print('\t');
                pw.print(key);
                pw.println(":");
                Iterator<String> it4 = APP_REMOVE_ACTIONS.get(key).iterator();
                while (it4.hasNext()) {
                    String action3 = it4.next();
                    pw.print("\t\t");
                    pw.println(action3);
                }
            }
        }
        if (!DEFAULT_DISCARD_ACTIONS.isEmpty()) {
            pw.println("DEFAULT_DISCARD_ACTIONS:");
            Iterator<String> it5 = DEFAULT_DISCARD_ACTIONS.iterator();
            while (it5.hasNext()) {
                String action4 = it5.next();
                pw.print('\t');
                pw.println(action4);
            }
        }
        if (!APP_DISCARD_ACTIONS.isEmpty()) {
            pw.println("APP_DISCARD_ACTIONS:");
            for (String key2 : APP_DISCARD_ACTIONS.keySet()) {
                pw.print('\t');
                pw.print(key2);
                pw.println(":");
                Iterator<String> it6 = APP_DISCARD_ACTIONS.get(key2).iterator();
                while (it6.hasNext()) {
                    String action5 = it6.next();
                    pw.print("\t\t");
                    pw.println(action5);
                }
            }
        }
        if (!APP_RESTART_SELF_SET.isEmpty()) {
            pw.println("APP_RESTART_SELF_SET:");
            pw.print('\t');
            pw.println(APP_RESTART_SELF_SET.toString());
        }
        if (!PROCESS_ALLOW_RESTART_IN_GAME.isEmpty()) {
            pw.println("PROCESS_ALLOW_RESTART_IN_GAME:");
            pw.print('\t');
            pw.println(PROCESS_ALLOW_RESTART_IN_GAME.toString());
        }
        if (!COMPONENT_ALLOW_RESTART_IN_GAME.isEmpty()) {
            pw.println("COMPONENT_ALLOW_RESTART_IN_GAME:");
            pw.print('\t');
            pw.println(COMPONENT_ALLOW_RESTART_IN_GAME.toString());
        }
    }
}