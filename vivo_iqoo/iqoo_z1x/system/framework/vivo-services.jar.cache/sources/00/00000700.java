package com.vivo.services.rms.appmng.namelist;

import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.ProcessList;
import java.util.ArrayList;
import java.util.HashMap;

/* loaded from: classes.dex */
public class OomStaticList {
    private static final String ADD = "add";
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
                put(ProxyConfigs.CTRL_MODULE_RMS, 0, 10, 2);
                put("com.vivo.sps:rms", 0, 10, 2);
                put("com.vivo.permissionmanager", 100, 2, 2);
                put("com.iqoo.secure:remote", ProcessList.BACKUP_APP_ADJ, 10, 0);
                put("com.vivo.safecenter", ProcessList.BACKUP_APP_ADJ, 10, 0);
                put("com.bbk.updater:remote", ProcessList.BACKUP_APP_ADJ, 10, 0);
                put("com.vivo.sim.contacts", ProcessList.BACKUP_APP_ADJ, 10, 0);
                put("com.android.vendors.bridge.softsim", ProcessList.BACKUP_APP_ADJ, 10, 0);
                put("com.redteamobile.virtual.softsim", ProcessList.BACKUP_APP_ADJ, 10, 0);
                put("com.android.providers.calendar", ProcessList.BACKUP_APP_ADJ, 10, 0);
                put("com.vivo.weather.provider", ProcessList.BACKUP_APP_ADJ, 10, 0);
                put("android.process.media", ProcessList.BACKUP_APP_ADJ, 10, 0);
                put("android.process.acore", ProcessList.BACKUP_APP_ADJ, 10, 0);
                put("com.vivo.doubletimezoneclock", ProcessList.BACKUP_APP_ADJ, 10, 0);
                put("com.vivo.bsptest", 0, 10, 2);
                put("com.vivo.devicereg", ProcessList.BACKUP_APP_ADJ, 10, 0);
            }
        }
    }

    public static OomNode getNode(ProcessInfo pi, int curAdj) {
        OomNode oomNode;
        if (MAP.isEmpty() || curAdj <= sMinAdj) {
            return null;
        }
        synchronized (MAP) {
            oomNode = MAP.get(pi.mProcName);
        }
        return oomNode;
    }

    private static void put(String procName, int adj, int state, int sched) {
        if (adj < sMinAdj) {
            sMinAdj = adj;
        }
        MAP.put(procName, new OomNode(adj, state, sched));
    }

    /* JADX WARN: Removed duplicated region for block: B:22:0x003b  */
    /* JADX WARN: Removed duplicated region for block: B:27:0x0051 A[Catch: all -> 0x0057, TryCatch #0 {, blocks: (B:5:0x0004, B:28:0x0055, B:25:0x0040, B:26:0x004d, B:27:0x0051, B:12:0x001c, B:15:0x0026, B:18:0x0030), top: B:33:0x0004 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static void apply(java.lang.String r6, java.util.ArrayList<java.lang.String> r7, java.util.ArrayList<java.lang.Integer> r8, java.util.ArrayList<java.lang.Integer> r9, java.util.ArrayList<java.lang.Integer> r10) {
        /*
            java.util.HashMap<java.lang.String, com.vivo.services.rms.appmng.namelist.OomNode> r0 = com.vivo.services.rms.appmng.namelist.OomStaticList.MAP
            monitor-enter(r0)
            r1 = 1
            com.vivo.services.rms.appmng.namelist.OomStaticList.sUpdated = r1     // Catch: java.lang.Throwable -> L57
            r2 = -1
            int r3 = r6.hashCode()     // Catch: java.lang.Throwable -> L57
            r4 = -934610812(0xffffffffc84af884, float:-207842.06)
            r5 = 2
            if (r3 == r4) goto L30
            r4 = -838846263(0xffffffffce0038c9, float:-5.3780128E8)
            if (r3 == r4) goto L26
            r4 = 96417(0x178a1, float:1.35109E-40)
            if (r3 == r4) goto L1c
        L1b:
            goto L39
        L1c:
            java.lang.String r3 = "add"
            boolean r3 = r6.equals(r3)     // Catch: java.lang.Throwable -> L57
            if (r3 == 0) goto L1b
            r2 = 0
            goto L39
        L26:
            java.lang.String r3 = "update"
            boolean r3 = r6.equals(r3)     // Catch: java.lang.Throwable -> L57
            if (r3 == 0) goto L1b
            r2 = r5
            goto L39
        L30:
            java.lang.String r3 = "remove"
            boolean r3 = r6.equals(r3)     // Catch: java.lang.Throwable -> L57
            if (r3 == 0) goto L1b
            r2 = r1
        L39:
            if (r2 == 0) goto L51
            if (r2 == r1) goto L4d
            if (r2 == r5) goto L40
            goto L55
        L40:
            java.util.HashMap<java.lang.String, com.vivo.services.rms.appmng.namelist.OomNode> r1 = com.vivo.services.rms.appmng.namelist.OomStaticList.MAP     // Catch: java.lang.Throwable -> L57
            r1.clear()     // Catch: java.lang.Throwable -> L57
            r1 = 1001(0x3e9, float:1.403E-42)
            com.vivo.services.rms.appmng.namelist.OomStaticList.sMinAdj = r1     // Catch: java.lang.Throwable -> L57
            add(r7, r8, r9, r10)     // Catch: java.lang.Throwable -> L57
            goto L55
        L4d:
            remove(r7)     // Catch: java.lang.Throwable -> L57
            goto L55
        L51:
            add(r7, r8, r9, r10)     // Catch: java.lang.Throwable -> L57
        L55:
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L57
            return
        L57:
            r1 = move-exception
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L57
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.rms.appmng.namelist.OomStaticList.apply(java.lang.String, java.util.ArrayList, java.util.ArrayList, java.util.ArrayList, java.util.ArrayList):void");
    }

    private static void add(ArrayList<String> procs, ArrayList<Integer> adjs, ArrayList<Integer> states, ArrayList<Integer> scheds) {
        if (procs == null || adjs == null || states == null || scheds == null) {
            return;
        }
        for (int i = 0; i < procs.size(); i++) {
            put(procs.get(i), adjs.get(i).intValue(), states.get(i).intValue(), scheds.get(i).intValue());
        }
    }

    private static void remove(ArrayList<String> procs) {
        if (procs == null) {
            return;
        }
        for (int i = 0; i < procs.size(); i++) {
            MAP.remove(procs.get(i));
        }
    }
}