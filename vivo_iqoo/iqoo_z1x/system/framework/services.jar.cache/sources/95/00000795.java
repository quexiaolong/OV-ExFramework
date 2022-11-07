package com.android.server.am;

import android.os.FileUtils;
import android.os.SystemProperties;
import android.system.Os;
import android.system.OsConstants;
import android.util.Slog;
import com.android.server.wm.ActivityTaskManagerDebugConfig;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public final class MemoryStatUtil {
    private static final String MEMORY_STAT_FILE_FMT = "/dev/memcg/apps/uid_%d/pid_%d/memory.stat";
    private static final int PGFAULT_INDEX = 9;
    private static final int PGMAJFAULT_INDEX = 11;
    private static final String PROC_STAT_FILE_FMT = "/proc/%d/stat";
    private static final int RSS_IN_PAGES_INDEX = 23;
    private static final String TAG = "ActivityManager";
    static final int PAGE_SIZE = (int) Os.sysconf(OsConstants._SC_PAGESIZE);
    private static final boolean DEVICE_HAS_PER_APP_MEMCG = SystemProperties.getBoolean("ro.config.per_app_memcg", false);
    private static final Pattern PGFAULT = Pattern.compile("total_pgfault (\\d+)");
    private static final Pattern PGMAJFAULT = Pattern.compile("total_pgmajfault (\\d+)");
    private static final Pattern RSS_IN_BYTES = Pattern.compile("total_rss (\\d+)");
    private static final Pattern CACHE_IN_BYTES = Pattern.compile("total_cache (\\d+)");
    private static final Pattern SWAP_IN_BYTES = Pattern.compile("total_swap (\\d+)");

    /* loaded from: classes.dex */
    public static final class MemoryStat {
        public long cacheInBytes;
        public long pgfault;
        public long pgmajfault;
        public long rssInBytes;
        public long swapInBytes;
    }

    private MemoryStatUtil() {
    }

    public static MemoryStat readMemoryStatFromFilesystem(int uid, int pid) {
        return hasMemcg() ? readMemoryStatFromMemcg(uid, pid) : readMemoryStatFromProcfs(pid);
    }

    static MemoryStat readMemoryStatFromMemcg(int uid, int pid) {
        String statPath = String.format(Locale.US, MEMORY_STAT_FILE_FMT, Integer.valueOf(uid), Integer.valueOf(pid));
        return parseMemoryStatFromMemcg(readFileContents(statPath));
    }

    public static MemoryStat readMemoryStatFromProcfs(int pid) {
        String statPath = String.format(Locale.US, PROC_STAT_FILE_FMT, Integer.valueOf(pid));
        return parseMemoryStatFromProcfs(readFileContents(statPath));
    }

    private static String readFileContents(String path) {
        File file = new File(path);
        if (!file.exists()) {
            if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
                Slog.i(TAG, path + " not found");
            }
            return null;
        }
        try {
            return FileUtils.readTextFile(file, 0, null);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read file:", e);
            return null;
        }
    }

    static MemoryStat parseMemoryStatFromMemcg(String memoryStatContents) {
        if (memoryStatContents == null || memoryStatContents.isEmpty()) {
            return null;
        }
        MemoryStat memoryStat = new MemoryStat();
        memoryStat.pgfault = tryParseLong(PGFAULT, memoryStatContents);
        memoryStat.pgmajfault = tryParseLong(PGMAJFAULT, memoryStatContents);
        memoryStat.rssInBytes = tryParseLong(RSS_IN_BYTES, memoryStatContents);
        memoryStat.cacheInBytes = tryParseLong(CACHE_IN_BYTES, memoryStatContents);
        memoryStat.swapInBytes = tryParseLong(SWAP_IN_BYTES, memoryStatContents);
        return memoryStat;
    }

    static MemoryStat parseMemoryStatFromProcfs(String procStatContents) {
        if (procStatContents == null || procStatContents.isEmpty()) {
            return null;
        }
        String[] splits = procStatContents.split(" ");
        if (splits.length < 24) {
            return null;
        }
        try {
            MemoryStat memoryStat = new MemoryStat();
            memoryStat.pgfault = Long.parseLong(splits[9]);
            memoryStat.pgmajfault = Long.parseLong(splits[11]);
            memoryStat.rssInBytes = Long.parseLong(splits[23]) * PAGE_SIZE;
            return memoryStat;
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Failed to parse value", e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean hasMemcg() {
        return DEVICE_HAS_PER_APP_MEMCG;
    }

    private static long tryParseLong(Pattern pattern, String input) {
        Matcher m = pattern.matcher(input);
        try {
            if (m.find()) {
                return Long.parseLong(m.group(1));
            }
            return 0L;
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Failed to parse value", e);
            return 0L;
        }
    }
}