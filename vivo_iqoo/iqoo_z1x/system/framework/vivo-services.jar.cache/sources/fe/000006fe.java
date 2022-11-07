package com.vivo.services.rms.appmng.namelist;

import com.vivo.services.proxy.game.GameSceneProxyManager;
import com.vivo.services.rms.Platform;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.superresolution.Constant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/* loaded from: classes.dex */
public class OomProtectList {
    private static final String ADD = "add";
    private static final String REMOVE = "remove";
    private static final String UPDATE = "update";
    private static final HashMap<String, MyNode> MAP = new HashMap<>();
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
                if (!Platform.isOverSeas()) {
                    put("com.tencent.mm:push", ProcessList.PROTECT_SERVICE_ADJ, 43200000);
                    put("com.tencent.mobileqq:MSF", ProcessList.PROTECT_SERVICE_ADJ, 43200000);
                    put(Constant.APP_WEIXIN, ProcessList.PROTECT_ACTIVITY_ADJ, GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME);
                    put("com.tencent.mm:tools", ProcessList.PROTECT_ACTIVITY_ADJ, 86400000);
                    put("com.tencent.mobileqq", ProcessList.PROTECT_ACTIVITY_ADJ, GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME);
                } else {
                    put("com.facebook.katana", ProcessList.PROTECT_ACTIVITY_ADJ, GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME);
                    put("com.facebook.orca", ProcessList.PROTECT_ACTIVITY_ADJ, GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME);
                    put("com.whatsapp", ProcessList.PROTECT_ACTIVITY_ADJ, GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME);
                }
            }
        }
    }

    public static OomNode getNode(ProcessInfo pi, int curAdj) {
        if (curAdj <= sMinAdj) {
            return null;
        }
        synchronized (MAP) {
            MyNode node = MAP.get(pi.mProcName);
            if (node == null || node.adj >= curAdj || pi.getInvisibleTime() >= node.duration) {
                return null;
            }
            return node;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:22:0x0039  */
    /* JADX WARN: Removed duplicated region for block: B:27:0x0046 A[Catch: all -> 0x0057, TryCatch #0 {, blocks: (B:5:0x0004, B:28:0x0055, B:25:0x003e, B:26:0x0042, B:27:0x0046, B:12:0x001a, B:15:0x0024, B:18:0x002e), top: B:33:0x0004 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static void apply(java.lang.String r6, java.util.ArrayList<java.lang.String> r7, java.util.ArrayList<java.lang.String> r8, java.util.ArrayList<java.lang.String> r9) {
        /*
            java.util.HashMap<java.lang.String, com.vivo.services.rms.appmng.namelist.OomProtectList$MyNode> r0 = com.vivo.services.rms.appmng.namelist.OomProtectList.MAP
            monitor-enter(r0)
            r1 = -1
            int r2 = r6.hashCode()     // Catch: java.lang.Throwable -> L57
            r3 = -934610812(0xffffffffc84af884, float:-207842.06)
            r4 = 2
            r5 = 1
            if (r2 == r3) goto L2e
            r3 = -838846263(0xffffffffce0038c9, float:-5.3780128E8)
            if (r2 == r3) goto L24
            r3 = 96417(0x178a1, float:1.35109E-40)
            if (r2 == r3) goto L1a
        L19:
            goto L37
        L1a:
            java.lang.String r2 = "add"
            boolean r2 = r6.equals(r2)     // Catch: java.lang.Throwable -> L57
            if (r2 == 0) goto L19
            r1 = r5
            goto L37
        L24:
            java.lang.String r2 = "update"
            boolean r2 = r6.equals(r2)     // Catch: java.lang.Throwable -> L57
            if (r2 == 0) goto L19
            r1 = 0
            goto L37
        L2e:
            java.lang.String r2 = "remove"
            boolean r2 = r6.equals(r2)     // Catch: java.lang.Throwable -> L57
            if (r2 == 0) goto L19
            r1 = r4
        L37:
            if (r1 == 0) goto L46
            if (r1 == r5) goto L42
            if (r1 == r4) goto L3e
            goto L55
        L3e:
            doRemove(r7, r8, r9)     // Catch: java.lang.Throwable -> L57
            goto L55
        L42:
            doUpdate(r7, r8, r9)     // Catch: java.lang.Throwable -> L57
            goto L55
        L46:
            com.vivo.services.rms.appmng.namelist.OomProtectList.sUpdated = r5     // Catch: java.lang.Throwable -> L57
            java.util.HashMap<java.lang.String, com.vivo.services.rms.appmng.namelist.OomProtectList$MyNode> r1 = com.vivo.services.rms.appmng.namelist.OomProtectList.MAP     // Catch: java.lang.Throwable -> L57
            r1.clear()     // Catch: java.lang.Throwable -> L57
            r1 = 1001(0x3e9, float:1.403E-42)
            com.vivo.services.rms.appmng.namelist.OomProtectList.sMinAdj = r1     // Catch: java.lang.Throwable -> L57
            doUpdate(r7, r8, r9)     // Catch: java.lang.Throwable -> L57
        L55:
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L57
            return
        L57:
            r1 = move-exception
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L57
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.rms.appmng.namelist.OomProtectList.apply(java.lang.String, java.util.ArrayList, java.util.ArrayList, java.util.ArrayList):void");
    }

    private static void doUpdate(ArrayList<String> activities, ArrayList<String> services, ArrayList<String> games) {
        if (services != null) {
            Iterator<String> it = services.iterator();
            while (it.hasNext()) {
                put(it.next(), ProcessList.PROTECT_SERVICE_ADJ, 43200000);
            }
        }
        if (activities != null) {
            Iterator<String> it2 = activities.iterator();
            while (it2.hasNext()) {
                String proc = it2.next();
                if ("com.tencent.mm:tools".equals(proc)) {
                    put(proc, ProcessList.PROTECT_ACTIVITY_ADJ, 86400000);
                } else {
                    put(proc, ProcessList.PROTECT_ACTIVITY_ADJ, GameSceneProxyManager.SERVICE_MAX_DELYEED_TIME);
                }
            }
        }
        if (games != null) {
            Iterator<String> it3 = games.iterator();
            while (it3.hasNext()) {
                put(it3.next(), ProcessList.PROTECT_GAME_ADJ, 1800000);
            }
        }
    }

    private static void doRemove(ArrayList<String> activities, ArrayList<String> services, ArrayList<String> games) {
        if (services != null) {
            Iterator<String> it = services.iterator();
            while (it.hasNext()) {
                String proc = it.next();
                remove(proc);
            }
        }
        if (activities != null) {
            Iterator<String> it2 = activities.iterator();
            while (it2.hasNext()) {
                String proc2 = it2.next();
                remove(proc2);
            }
        }
        if (games != null) {
            Iterator<String> it3 = games.iterator();
            while (it3.hasNext()) {
                String proc3 = it3.next();
                remove(proc3);
            }
        }
    }

    private static void put(String procName, int adj, int duration) {
        if (adj < sMinAdj) {
            sMinAdj = adj;
        }
        MAP.put(procName, new MyNode(adj, duration));
    }

    private static void remove(String procName) {
        MAP.remove(procName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MyNode extends OomNode {
        public int duration;

        public MyNode(int adj, int dt) {
            super(adj, 10, 0);
            this.adj = adj;
            this.duration = dt;
        }
    }
}