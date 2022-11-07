package com.android.server.stats.pull;

import android.os.Process;
import android.util.SparseArray;

/* loaded from: classes2.dex */
public final class ProcfsMemoryUtil {
    private static final int[] CMDLINE_OUT = {4096};
    private static final String[] STATUS_KEYS = {"Uid:", "VmHWM:", "VmRSS:", "RssAnon:", "VmSwap:"};

    /* loaded from: classes2.dex */
    public static final class MemorySnapshot {
        public int anonRssInKilobytes;
        public int rssHighWaterMarkInKilobytes;
        public int rssInKilobytes;
        public int swapInKilobytes;
        public int uid;
    }

    private ProcfsMemoryUtil() {
    }

    public static MemorySnapshot readMemorySnapshotFromProcfs(int pid) {
        long[] output = new long[STATUS_KEYS.length];
        output[0] = -1;
        output[3] = -1;
        output[4] = -1;
        Process.readProcLines("/proc/" + pid + "/status", STATUS_KEYS, output);
        if (output[0] == -1 || output[3] == -1 || output[4] == -1) {
            return null;
        }
        MemorySnapshot snapshot = new MemorySnapshot();
        snapshot.uid = (int) output[0];
        snapshot.rssHighWaterMarkInKilobytes = (int) output[1];
        snapshot.rssInKilobytes = (int) output[2];
        snapshot.anonRssInKilobytes = (int) output[3];
        snapshot.swapInKilobytes = (int) output[4];
        return snapshot;
    }

    public static String readCmdlineFromProcfs(int pid) {
        String[] cmdline = new String[1];
        if (!Process.readProcFile("/proc/" + pid + "/cmdline", CMDLINE_OUT, cmdline, null, null)) {
            return "";
        }
        return cmdline[0];
    }

    public static SparseArray<String> getProcessCmdlines() {
        int[] pids = Process.getPids("/proc", new int[1024]);
        SparseArray<String> cmdlines = new SparseArray<>(pids.length);
        for (int pid : pids) {
            if (pid < 0) {
                break;
            }
            String cmdline = readCmdlineFromProcfs(pid);
            if (!cmdline.isEmpty()) {
                cmdlines.append(pid, cmdline);
            }
        }
        return cmdlines;
    }
}