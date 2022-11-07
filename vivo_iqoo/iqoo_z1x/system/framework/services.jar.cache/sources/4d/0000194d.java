package com.android.server.stats.pull;

import android.os.FileUtils;
import android.util.Slog;
import android.util.SparseArray;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes2.dex */
public final class IonMemoryUtil {
    private static final String DEBUG_SYSTEM_ION_HEAP_FILE = "/sys/kernel/debug/ion/heaps/system";
    private static final Pattern ION_HEAP_SIZE_IN_BYTES = Pattern.compile("\n\\s*total\\s*(\\d+)\\s*\n");
    private static final Pattern PROCESS_ION_HEAP_SIZE_IN_BYTES = Pattern.compile("\n\\s+\\S+\\s+(\\d+)\\s+(\\d+)");
    private static final String TAG = "IonMemoryUtil";

    private IonMemoryUtil() {
    }

    public static long readSystemIonHeapSizeFromDebugfs() {
        return parseIonHeapSizeFromDebugfs(readFile(DEBUG_SYSTEM_ION_HEAP_FILE));
    }

    static long parseIonHeapSizeFromDebugfs(String contents) {
        if (contents.isEmpty()) {
            return 0L;
        }
        Matcher matcher = ION_HEAP_SIZE_IN_BYTES.matcher(contents);
        try {
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
            return 0L;
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Failed to parse value", e);
            return 0L;
        }
    }

    public static List<IonAllocations> readProcessSystemIonHeapSizesFromDebugfs() {
        return parseProcessIonHeapSizesFromDebugfs(readFile(DEBUG_SYSTEM_ION_HEAP_FILE));
    }

    static List<IonAllocations> parseProcessIonHeapSizesFromDebugfs(String contents) {
        if (contents.isEmpty()) {
            return Collections.emptyList();
        }
        Matcher m = PROCESS_ION_HEAP_SIZE_IN_BYTES.matcher(contents);
        SparseArray<IonAllocations> entries = new SparseArray<>();
        while (m.find()) {
            try {
                int pid = Integer.parseInt(m.group(1));
                long sizeInBytes = Long.parseLong(m.group(2));
                IonAllocations allocations = entries.get(pid);
                if (allocations == null) {
                    allocations = new IonAllocations();
                    entries.put(pid, allocations);
                }
                allocations.pid = pid;
                allocations.totalSizeInBytes += sizeInBytes;
                allocations.count++;
                allocations.maxSizeInBytes = Math.max(allocations.maxSizeInBytes, sizeInBytes);
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Failed to parse value", e);
            }
        }
        List<IonAllocations> result = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            result.add(entries.valueAt(i));
        }
        return result;
    }

    private static String readFile(String path) {
        try {
            File file = new File(path);
            return FileUtils.readTextFile(file, 0, null);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read file", e);
            return "";
        }
    }

    /* loaded from: classes2.dex */
    public static final class IonAllocations {
        public int count;
        public long maxSizeInBytes;
        public int pid;
        public long totalSizeInBytes;

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IonAllocations that = (IonAllocations) o;
            if (this.pid == that.pid && this.totalSizeInBytes == that.totalSizeInBytes && this.count == that.count && this.maxSizeInBytes == that.maxSizeInBytes) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.pid), Long.valueOf(this.totalSizeInBytes), Integer.valueOf(this.count), Long.valueOf(this.maxSizeInBytes));
        }

        public String toString() {
            return "IonAllocations{pid=" + this.pid + ", totalSizeInBytes=" + this.totalSizeInBytes + ", count=" + this.count + ", maxSizeInBytes=" + this.maxSizeInBytes + '}';
        }
    }
}