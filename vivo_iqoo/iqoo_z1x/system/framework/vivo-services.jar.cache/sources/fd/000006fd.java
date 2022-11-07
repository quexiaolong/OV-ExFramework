package com.vivo.services.rms.appmng.namelist;

import android.os.Process;
import android.os.SystemClock;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.rms.appmng.AppManager;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class OomPreviousList {
    private static final ArrayList<OomNode> LIST = new ArrayList<>();
    private static boolean sUpdated = false;
    private static long TIME_OUT = 86400000;
    private static int sMinAdj = 1001;
    private static final ArrayList<String> EXCLUDED_LIST = new ArrayList<>();
    private static final long sTotalMem = Process.getTotalMemory();

    static {
        restore();
    }

    public static void restore() {
        synchronized (LIST) {
            if (sUpdated || LIST.isEmpty()) {
                sUpdated = false;
                LIST.clear();
                sMinAdj = 1001;
                put(ProcessList.VERY_LASTEST_PREVIOUS_APP_ADJ, 15);
                put(ProcessList.LASTEST_PREVIOUS_APP_ADJ, 15);
                put(ProcessList.LASTEST_PREVIOUS_APP_MAX, 15);
                put(ProcessList.PREVIOUS_APP_ADJ, 15);
                put(701, 15);
                if (sTotalMem > 5368709120L) {
                    put(702, 15);
                    put(703, 15);
                    put(704, 15);
                }
                if (sTotalMem > 6442450944L) {
                    put(705, 15);
                    put(706, 15);
                }
                if (sTotalMem > 7516192768L) {
                    put(707, 15);
                    put(708, 15);
                }
            }
        }
    }

    public static OomNode getNode(ProcessInfo pi, int curAdj) {
        int taskId;
        OomNode oomNode;
        long now = SystemClock.uptimeMillis();
        if (curAdj <= sMinAdj || now - pi.mLastInvisibleTime > TIME_OUT || (taskId = AppManager.getInstance().getRecentTask().taskId(pi.mProcName, pi.mUid)) == -1 || taskId > LIST.size() - 1) {
            return null;
        }
        synchronized (LIST) {
            oomNode = LIST.get(taskId);
        }
        return oomNode;
    }

    private static void put(int adj, int state) {
        if (adj < sMinAdj) {
            sMinAdj = adj;
        }
        LIST.add(new OomNode(adj, state, 0));
    }

    public static void apply(ArrayList<Integer> adjs, ArrayList<Integer> states) {
        if (adjs == null || states == null || adjs.size() != states.size()) {
            return;
        }
        synchronized (LIST) {
            sUpdated = true;
            LIST.clear();
            sMinAdj = 1001;
            for (int i = 0; i < adjs.size(); i++) {
                put(adjs.get(i).intValue(), states.get(i).intValue());
            }
            AppManager.getInstance().getRecentTask().setSize(LIST.size());
        }
    }

    public static void updateExcludedList(ArrayList<String> lists) {
        synchronized (EXCLUDED_LIST) {
            EXCLUDED_LIST.clear();
            EXCLUDED_LIST.addAll(lists);
        }
    }

    public static boolean excluded(String process) {
        boolean contains;
        if (process == null) {
            return false;
        }
        synchronized (EXCLUDED_LIST) {
            contains = EXCLUDED_LIST.contains(process);
        }
        return contains;
    }

    public static void restoreExcludedList() {
        synchronized (EXCLUDED_LIST) {
            EXCLUDED_LIST.clear();
        }
    }
}