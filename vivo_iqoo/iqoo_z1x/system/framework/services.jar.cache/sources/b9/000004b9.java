package com.android.server;

import android.os.StrictMode;
import android.util.Slog;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import libcore.io.IoUtils;

/* loaded from: classes.dex */
public final class MemoryPressureUtil {
    private static final String FILE = "/proc/pressure/memory";
    private static final String FILE_CPU0_online = "/sys/devices/system/cpu/cpu0/online";
    private static final String FILE_CPU1_online = "/sys/devices/system/cpu/cpu1/online";
    private static final String FILE_CPU2_online = "/sys/devices/system/cpu/cpu2/online";
    private static final String FILE_CPU3_online = "/sys/devices/system/cpu/cpu3/online";
    private static final String FILE_CPU4_online = "/sys/devices/system/cpu/cpu4/online";
    private static final String FILE_CPU5_online = "/sys/devices/system/cpu/cpu5/online";
    private static final String FILE_CPU6_online = "/sys/devices/system/cpu/cpu6/online";
    private static final String FILE_CPU7_online = "/sys/devices/system/cpu/cpu7/online";
    private static final String TAG = "MemoryPressure";

    public static String currentPsiState() {
        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        StringWriter contents = new StringWriter();
        try {
            try {
                if (new File(FILE).exists()) {
                    contents.append((CharSequence) "----- Output from /proc/pressure/memory -----\n");
                    contents.append((CharSequence) IoUtils.readFileAsString(FILE));
                    contents.append((CharSequence) "----- End output from /proc/pressure/memory -----\n\n");
                }
                if (new File(FILE_CPU7_online).exists()) {
                    contents.append((CharSequence) "----- Output from cpu online -----\n");
                    contents.append((CharSequence) IoUtils.readFileAsString(FILE_CPU0_online));
                    contents.append((CharSequence) IoUtils.readFileAsString(FILE_CPU1_online));
                    contents.append((CharSequence) IoUtils.readFileAsString(FILE_CPU2_online));
                    contents.append((CharSequence) IoUtils.readFileAsString(FILE_CPU3_online));
                    contents.append((CharSequence) IoUtils.readFileAsString(FILE_CPU4_online));
                    contents.append((CharSequence) IoUtils.readFileAsString(FILE_CPU5_online));
                    contents.append((CharSequence) IoUtils.readFileAsString(FILE_CPU6_online));
                    contents.append((CharSequence) IoUtils.readFileAsString(FILE_CPU7_online));
                    contents.append((CharSequence) "----- End output from cpu online -----\n\n");
                }
            } catch (IOException e) {
                Slog.e(TAG, "Could not read /proc/pressure/memory", e);
            }
            StrictMode.setThreadPolicy(savedPolicy);
            return contents.toString();
        } catch (Throwable th) {
            StrictMode.setThreadPolicy(savedPolicy);
            throw th;
        }
    }

    private MemoryPressureUtil() {
    }
}