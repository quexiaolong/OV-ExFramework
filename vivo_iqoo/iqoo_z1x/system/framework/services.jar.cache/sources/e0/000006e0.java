package com.android.server.am;

import android.os.SystemProperties;

/* loaded from: classes.dex */
public class ActivityManagerDebugConfig {
    public static boolean APPEND_CATEGORY_NAME = false;
    public static boolean DEBUG_ALL = false;
    public static boolean DEBUG_ANR = false;
    public static boolean DEBUG_BACKGROUND_CHECK = false;
    public static boolean DEBUG_BACKUP = false;
    public static boolean DEBUG_BROADCAST = false;
    public static boolean DEBUG_BROADCAST_BACKGROUND = false;
    public static boolean DEBUG_BROADCAST_DEFERRAL = false;
    public static boolean DEBUG_BROADCAST_LIGHT = false;
    public static boolean DEBUG_COMPACTION = false;
    public static boolean DEBUG_FOREGROUND_SERVICE = false;
    public static boolean DEBUG_FREEZER = false;
    public static boolean DEBUG_LRU = false;
    public static boolean DEBUG_MU = false;
    public static boolean DEBUG_NETWORK = false;
    public static boolean DEBUG_OOM_ADJ = false;
    public static boolean DEBUG_OOM_ADJ_REASON = false;
    public static boolean DEBUG_OTHER = false;
    public static boolean DEBUG_PERMISSIONS_REVIEW = false;
    public static boolean DEBUG_POWER = false;
    public static boolean DEBUG_POWER_QUICK = false;
    public static boolean DEBUG_PROCESSES = false;
    public static boolean DEBUG_PROCESS_OBSERVERS = false;
    public static boolean DEBUG_PROVIDER = false;
    public static boolean DEBUG_PSS = false;
    public static boolean DEBUG_SERVICE = false;
    public static boolean DEBUG_SERVICE_EXECUTING = false;
    public static boolean DEBUG_SERVICE_VIVO = false;
    public static boolean DEBUG_UID_OBSERVERS = false;
    public static boolean DEBUG_USAGE_STATS = false;
    public static boolean DEBUG_WHITELISTS = false;
    static final String POSTFIX_BACKUP;
    static final String POSTFIX_BROADCAST;
    static final String POSTFIX_CLEANUP;
    static final String POSTFIX_LRU;
    static final String POSTFIX_MU = "_MU";
    static final String POSTFIX_NETWORK = "_Network";
    static final String POSTFIX_OOM_ADJ;
    static final String POSTFIX_POWER;
    static final String POSTFIX_PROCESSES;
    static final String POSTFIX_PROCESS_OBSERVERS;
    static final String POSTFIX_PROVIDER;
    static final String POSTFIX_PSS;
    static final String POSTFIX_SERVICE;
    static final String POSTFIX_SERVICE_EXECUTING;
    static final String POSTFIX_UID_OBSERVERS;
    static final String TAG_AM = "ActivityManager";
    static final boolean TAG_WITH_CLASS_NAME = false;

    static {
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        boolean z7;
        boolean z8;
        boolean z9;
        boolean z10;
        boolean z11;
        boolean z12;
        boolean z13;
        boolean z14;
        boolean z15;
        boolean z16;
        boolean z17;
        boolean z18;
        boolean z19;
        boolean z20;
        boolean z21;
        boolean z22;
        boolean z23;
        boolean z24;
        boolean z25;
        boolean z26 = true;
        if (0 == 0) {
            z = false;
        } else {
            z = true;
        }
        DEBUG_BACKGROUND_CHECK = z;
        if (!DEBUG_ALL) {
            z2 = false;
        } else {
            z2 = true;
        }
        DEBUG_BACKUP = z2;
        if (!DEBUG_ALL) {
            z3 = false;
        } else {
            z3 = true;
        }
        DEBUG_BROADCAST = z3;
        if (!z3) {
            z4 = false;
        } else {
            z4 = true;
        }
        DEBUG_BROADCAST_BACKGROUND = z4;
        if (!DEBUG_BROADCAST) {
            z5 = false;
        } else {
            z5 = true;
        }
        DEBUG_BROADCAST_LIGHT = z5;
        if (!DEBUG_BROADCAST) {
            z6 = false;
        } else {
            z6 = true;
        }
        DEBUG_BROADCAST_DEFERRAL = z6;
        if (!DEBUG_ALL) {
            z7 = false;
        } else {
            z7 = true;
        }
        DEBUG_COMPACTION = z7;
        boolean z27 = DEBUG_ALL;
        DEBUG_FREEZER = true;
        if (!z27) {
            z8 = false;
        } else {
            z8 = true;
        }
        DEBUG_LRU = z8;
        if (!DEBUG_ALL) {
            z9 = false;
        } else {
            z9 = true;
        }
        DEBUG_MU = z9;
        if (!DEBUG_ALL) {
            z10 = false;
        } else {
            z10 = true;
        }
        DEBUG_NETWORK = z10;
        if (!DEBUG_ALL) {
            z11 = false;
        } else {
            z11 = true;
        }
        DEBUG_OOM_ADJ = z11;
        if (!DEBUG_ALL) {
            z12 = false;
        } else {
            z12 = true;
        }
        DEBUG_OOM_ADJ_REASON = z12;
        if (!DEBUG_ALL) {
            z13 = false;
        } else {
            z13 = true;
        }
        DEBUG_POWER = z13;
        if (!z13) {
            z14 = false;
        } else {
            z14 = true;
        }
        DEBUG_POWER_QUICK = z14;
        if (!DEBUG_ALL) {
            z15 = false;
        } else {
            z15 = true;
        }
        DEBUG_PROCESS_OBSERVERS = z15;
        if (!DEBUG_ALL) {
            z16 = false;
        } else {
            z16 = true;
        }
        DEBUG_PROCESSES = z16;
        if (!DEBUG_ALL) {
            z17 = false;
        } else {
            z17 = true;
        }
        DEBUG_PROVIDER = z17;
        if (!DEBUG_ALL) {
            z18 = false;
        } else {
            z18 = true;
        }
        DEBUG_PSS = z18;
        if (!DEBUG_ALL) {
            z19 = false;
        } else {
            z19 = true;
        }
        DEBUG_SERVICE = z19;
        if (!DEBUG_ALL) {
            z20 = false;
        } else {
            z20 = true;
        }
        DEBUG_FOREGROUND_SERVICE = z20;
        if (!DEBUG_ALL) {
            z21 = false;
        } else {
            z21 = true;
        }
        DEBUG_SERVICE_EXECUTING = z21;
        if (!DEBUG_ALL) {
            z22 = false;
        } else {
            z22 = true;
        }
        DEBUG_UID_OBSERVERS = z22;
        if (!DEBUG_ALL) {
            z23 = false;
        } else {
            z23 = true;
        }
        DEBUG_USAGE_STATS = z23;
        if (!DEBUG_ALL) {
            z24 = false;
        } else {
            z24 = true;
        }
        DEBUG_PERMISSIONS_REVIEW = z24;
        if (!DEBUG_ALL) {
            z25 = false;
        } else {
            z25 = true;
        }
        DEBUG_WHITELISTS = z25;
        if (!DEBUG_ALL) {
            z26 = false;
        }
        DEBUG_SERVICE_VIVO = z26;
        DEBUG_OTHER = SystemProperties.getBoolean("persist.sys.amslog.debug", false);
        POSTFIX_BACKUP = APPEND_CATEGORY_NAME ? "_Backup" : "";
        POSTFIX_BROADCAST = APPEND_CATEGORY_NAME ? "_Broadcast" : "";
        POSTFIX_CLEANUP = APPEND_CATEGORY_NAME ? "_Cleanup" : "";
        POSTFIX_LRU = APPEND_CATEGORY_NAME ? "_LRU" : "";
        POSTFIX_OOM_ADJ = APPEND_CATEGORY_NAME ? "_OomAdj" : "";
        POSTFIX_POWER = APPEND_CATEGORY_NAME ? "_Power" : "";
        POSTFIX_PROCESS_OBSERVERS = APPEND_CATEGORY_NAME ? "_ProcessObservers" : "";
        POSTFIX_PROCESSES = APPEND_CATEGORY_NAME ? "_Processes" : "";
        POSTFIX_PROVIDER = APPEND_CATEGORY_NAME ? "_Provider" : "";
        POSTFIX_PSS = APPEND_CATEGORY_NAME ? "_Pss" : "";
        POSTFIX_SERVICE = APPEND_CATEGORY_NAME ? "_Service" : "";
        POSTFIX_SERVICE_EXECUTING = APPEND_CATEGORY_NAME ? "_ServiceExecuting" : "";
        POSTFIX_UID_OBSERVERS = APPEND_CATEGORY_NAME ? "_UidObservers" : "";
    }
}