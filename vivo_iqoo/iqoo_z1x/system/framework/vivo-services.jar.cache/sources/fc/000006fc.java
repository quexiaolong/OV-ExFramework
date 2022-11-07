package com.vivo.services.rms.appmng.namelist;

import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.superresolution.Constant;
import java.util.ArrayList;
import java.util.HashMap;

/* loaded from: classes.dex */
public class OomPreloadList {
    private static final String ADD = "add";
    private static final String CLEAR = "clear";
    private static final String REMOVE = "remove";
    private static final String UPDATE = "update";
    private static final HashMap<String, OomNode> MAP = new HashMap<>();
    private static boolean sUpdated = false;
    private static int sMinAdj = 1001;

    static {
        restore();
    }

    public static void restore() {
        synchronized (MAP) {
            if (sUpdated || MAP.isEmpty()) {
                sUpdated = false;
                sMinAdj = 1001;
                MAP.clear();
            }
        }
    }

    public static OomNode getNode(ProcessInfo pi, int curAdj) {
        OomNode oomNode;
        if (MAP.isEmpty() || curAdj <= sMinAdj || Constant.APP_WEIXIN.equals(pi.mPkgName)) {
            return null;
        }
        synchronized (MAP) {
            oomNode = MAP.get(pi.mPkgName);
        }
        return oomNode;
    }

    private static void put(String pkg, int adj, int state, int sched) {
        if (adj == -1) {
            adj = 800;
        }
        if (state == -1) {
            state = 15;
        }
        if (sched == -1) {
            sched = 0;
        }
        if (adj < sMinAdj) {
            sMinAdj = adj;
        }
        MAP.put(pkg, new OomNode(adj, state, sched));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static void apply(String policy, ArrayList<String> pkgs, ArrayList<Integer> adjs, ArrayList<Integer> states, ArrayList<Integer> scheds) {
        synchronized (MAP) {
            sUpdated = true;
            char c = 65535;
            switch (policy.hashCode()) {
                case -934610812:
                    if (policy.equals(REMOVE)) {
                        c = 1;
                        break;
                    }
                    break;
                case -838846263:
                    if (policy.equals(UPDATE)) {
                        c = 2;
                        break;
                    }
                    break;
                case 96417:
                    if (policy.equals(ADD)) {
                        c = 0;
                        break;
                    }
                    break;
                case 94746189:
                    if (policy.equals(CLEAR)) {
                        c = 3;
                        break;
                    }
                    break;
            }
            if (c != 0) {
                if (c == 1) {
                    remove(pkgs);
                } else if (c == 2) {
                    MAP.clear();
                    sMinAdj = 1001;
                    add(pkgs, adjs, states, scheds);
                } else if (c == 3) {
                    MAP.clear();
                    sMinAdj = 1001;
                }
            } else {
                add(pkgs, adjs, states, scheds);
            }
        }
    }

    public static ArrayList<String> getList() {
        ArrayList<String> result;
        synchronized (MAP) {
            result = new ArrayList<>(MAP.size());
            for (String pkg : MAP.keySet()) {
                result.add(pkg);
            }
        }
        return result;
    }

    private static void add(ArrayList<String> pkgs, ArrayList<Integer> adjs, ArrayList<Integer> states, ArrayList<Integer> scheds) {
        if (pkgs == null || adjs == null || states == null || scheds == null) {
            return;
        }
        for (int i = 0; i < pkgs.size(); i++) {
            put(pkgs.get(i), adjs.get(i).intValue(), states.get(i).intValue(), scheds.get(i).intValue());
        }
    }

    private static void remove(ArrayList<String> pkgs) {
        if (pkgs == null) {
            return;
        }
        for (int i = 0; i < pkgs.size(); i++) {
            MAP.remove(pkgs.get(i));
        }
    }
}