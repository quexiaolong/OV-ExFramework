package com.vivo.services.rms;

import android.os.SystemProperties;
import android.util.ArrayMap;

/* loaded from: classes.dex */
public class Platform {
    public static final int CPU_CLUSTER_4_0 = 0;
    public static final int CPU_CLUSTER_4_4 = 1;
    public static final int CPU_CLUSTER_4_4_INVERSE = 3;
    public static final int CPU_CLUSTER_6_2 = 2;
    public static final int CPU_CLUSTER_6_2_INVERSE = 4;
    public static final int CPU_CLUSTER_UNKNOW = -1;
    private static final int PERFORMANCE_LEVEL_HIGH = 3;
    private static final int PERFORMANCE_LEVEL_LOW = 0;
    private static final int PERFORMANCE_LEVEL_MIDDLE = 2;
    private static final int PERFORMANCE_LEVEL_NORMAL = 1;
    private static final int PERFORMANCE_LEVEL_UNKNOWN = -1;
    private static SysInfo SYS_INFO = null;
    public static final String VIVO_PLATFORM_INFO_PROP = "persist.sys.vivo.platform_info";
    public static final String PLATFORM = SystemProperties.get("ro.vivo.product.platform");
    private static final boolean OVERSEAS = SystemProperties.get("ro.vivo.product.overseas", "no").equals("yes");

    static {
        ArrayMap<String, SysInfo> infos = new ArrayMap<>();
        infos.put("SM8250", new SysInfo(1, 3));
        infos.put("SM8150", new SysInfo(1, 3));
        infos.put("SDM845", new SysInfo(1, 3));
        infos.put("MTK6885", new SysInfo(1, 3));
        infos.put("SM8350", new SysInfo(1, 3));
        infos.put("MTK6891", new SysInfo(1, 3));
        infos.put("ERD9815", new SysInfo(1, 2));
        infos.put("SM7250", new SysInfo(2, 2));
        infos.put("MTK6875", new SysInfo(1, 2));
        infos.put("SDM710", new SysInfo(2, 2));
        infos.put("ERD880", new SysInfo(2, 2));
        infos.put("SM7150", new SysInfo(2, 2));
        infos.put("SM7125", new SysInfo(2, 2));
        infos.put("ERD9630", new SysInfo(2, 2));
        infos.put("SDM660", new SysInfo(1, 1));
        infos.put("SM6150", new SysInfo(2, 1));
        infos.put("MTK6771", new SysInfo(1, 1));
        infos.put("MTK6771T", new SysInfo(1, 1));
        infos.put("MTK6768", new SysInfo(2, 0));
        infos.put("MTK6765", new SysInfo(3, 0));
        infos.put("MTK6853", new SysInfo(2, 0));
        infos.put("SM6125", new SysInfo(1, 0));
        infos.put("SM4250", new SysInfo(1, 0));
        infos.put("SM4350", new SysInfo(2, 0));
        infos.put("SDM439", new SysInfo(3, 0));
        infos.put("MTK6769", new SysInfo(2, 0));
        infos.put("MTK6833", new SysInfo(2, 0));
        SYS_INFO = infos.getOrDefault(PLATFORM, new SysInfo(-1, -1));
        SystemProperties.set(VIVO_PLATFORM_INFO_PROP, SYS_INFO.cluster + "/" + SYS_INFO.perf);
    }

    public static boolean isOverSeas() {
        return OVERSEAS;
    }

    public static int getCpuCluster() {
        return SYS_INFO.cluster;
    }

    public static boolean isTwoBigcoreDevice() {
        return SYS_INFO.cluster == 2 || SYS_INFO.cluster == 4;
    }

    public static boolean isHighPerfDevice() {
        return SYS_INFO.perf >= 3;
    }

    public static boolean isMiddlePerfDevice() {
        return SYS_INFO.perf >= 2;
    }

    public static boolean isLowPerfDevice() {
        return SYS_INFO.perf == 0;
    }

    /* loaded from: classes.dex */
    private static class SysInfo {
        public int cluster;
        public int perf;

        public SysInfo(int inCluster, int inPerf) {
            this.cluster = inCluster;
            this.perf = inPerf;
        }
    }
}