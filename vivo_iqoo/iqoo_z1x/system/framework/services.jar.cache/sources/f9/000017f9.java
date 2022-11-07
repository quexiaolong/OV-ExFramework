package com.android.server.power.batterysaver;

import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.slice.SliceClientPermissions;
import java.util.Map;

/* loaded from: classes2.dex */
public class CpuFrequencies {
    private static final String TAG = "CpuFrequencies";
    private final Object mLock = new Object();
    private final ArrayMap<Integer, Long> mCoreAndFrequencies = new ArrayMap<>();

    public CpuFrequencies parseString(String cpuNumberAndFrequencies) {
        synchronized (this.mLock) {
            this.mCoreAndFrequencies.clear();
            try {
                for (String pair : cpuNumberAndFrequencies.split(SliceClientPermissions.SliceAuthority.DELIMITER)) {
                    String pair2 = pair.trim();
                    if (pair2.length() != 0) {
                        String[] coreAndFreq = pair2.split(":", 2);
                        if (coreAndFreq.length != 2) {
                            throw new IllegalArgumentException("Wrong format");
                        }
                        int core = Integer.parseInt(coreAndFreq[0]);
                        long freq = Long.parseLong(coreAndFreq[1]);
                        this.mCoreAndFrequencies.put(Integer.valueOf(core), Long.valueOf(freq));
                    }
                }
            } catch (IllegalArgumentException e) {
                Slog.wtf(TAG, "Invalid configuration: '" + cpuNumberAndFrequencies + "'");
            }
        }
        return this;
    }

    public ArrayMap<String, String> toSysFileMap() {
        ArrayMap<String, String> map = new ArrayMap<>();
        addToSysFileMap(map);
        return map;
    }

    public void addToSysFileMap(Map<String, String> map) {
        synchronized (this.mLock) {
            int size = this.mCoreAndFrequencies.size();
            for (int i = 0; i < size; i++) {
                int core = this.mCoreAndFrequencies.keyAt(i).intValue();
                long freq = this.mCoreAndFrequencies.valueAt(i).longValue();
                String file = "/sys/devices/system/cpu/cpu" + Integer.toString(core) + "/cpufreq/scaling_max_freq";
                map.put(file, Long.toString(freq));
            }
        }
    }
}