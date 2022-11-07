package com.android.server.wm;

import android.os.SystemProperties;

/* loaded from: classes2.dex */
public class ActivityTaskManagerDebugConfig {
    public static boolean DEBUG_ACTIVITY_STARTS = false;
    public static boolean DEBUG_ADD_REMOVE = false;
    public static boolean DEBUG_ALL_ACTIVITIES = false;
    public static boolean DEBUG_APP = false;
    public static boolean DEBUG_APP_SHARE = false;
    public static boolean DEBUG_CLEANUP = false;
    public static boolean DEBUG_CONFIGURATION = false;
    public static boolean DEBUG_CONTAINERS = false;
    public static boolean DEBUG_FOCUS = false;
    public static boolean DEBUG_IDLE = false;
    public static boolean DEBUG_IMMERSIVE = false;
    public static boolean DEBUG_LOCKTASK = false;
    public static boolean DEBUG_METRICS = false;
    public static boolean DEBUG_OTHER = false;
    public static boolean DEBUG_PAUSE = false;
    public static boolean DEBUG_PERMISSIONS_REVIEW = false;
    public static boolean DEBUG_RECENTS = false;
    public static boolean DEBUG_RECENTS_TRIM_TASKS = false;
    public static boolean DEBUG_RELEASE = false;
    public static boolean DEBUG_RESULTS = false;
    public static boolean DEBUG_SAVED_STATE = false;
    public static boolean DEBUG_STACK = false;
    public static boolean DEBUG_STATES = false;
    public static boolean DEBUG_SWITCH = false;
    public static boolean DEBUG_TASKS = false;
    public static boolean DEBUG_TRANSITION = false;
    public static boolean DEBUG_USER_LEAVING = false;
    public static boolean DEBUG_VISIBILITY = false;
    static final String POSTFIX_ADD_REMOVE;
    static final String POSTFIX_APP;
    static final String POSTFIX_CLEANUP;
    public static final String POSTFIX_CONFIGURATION;
    static final String POSTFIX_CONTAINERS;
    static final String POSTFIX_FOCUS;
    static final String POSTFIX_IDLE;
    static final String POSTFIX_IMMERSIVE;
    public static final String POSTFIX_LOCKTASK;
    static final String POSTFIX_PAUSE;
    static final String POSTFIX_RECENTS;
    static final String POSTFIX_RELEASE;
    static final String POSTFIX_RESULTS;
    static final String POSTFIX_SAVED_STATE;
    static final String POSTFIX_STACK;
    static final String POSTFIX_STATES;
    public static final String POSTFIX_SWITCH;
    static final String POSTFIX_TASKS;
    static final String POSTFIX_TRANSITION;
    static final String POSTFIX_USER_LEAVING;
    static final String POSTFIX_VISIBILITY;
    static final String TAG_ATM = "ActivityTaskManager";
    static final boolean TAG_WITH_CLASS_NAME = false;
    public static boolean APPEND_CATEGORY_NAME = false;
    public static boolean DEBUG_ALL = false;

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
        boolean z26 = false;
        if (0 == 0) {
            z = false;
        } else {
            z = true;
        }
        DEBUG_ALL_ACTIVITIES = z;
        if (!z) {
            z2 = false;
        } else {
            z2 = true;
        }
        DEBUG_ADD_REMOVE = z2;
        if (!DEBUG_ALL) {
            z3 = false;
        } else {
            z3 = true;
        }
        DEBUG_CONFIGURATION = z3;
        if (!DEBUG_ALL_ACTIVITIES) {
            z4 = false;
        } else {
            z4 = true;
        }
        DEBUG_CONTAINERS = z4;
        DEBUG_FOCUS = false;
        if (!DEBUG_ALL) {
            z5 = false;
        } else {
            z5 = true;
        }
        DEBUG_IMMERSIVE = z5;
        if (!DEBUG_ALL) {
            z6 = false;
        } else {
            z6 = true;
        }
        DEBUG_LOCKTASK = z6;
        if (!DEBUG_ALL) {
            z7 = false;
        } else {
            z7 = true;
        }
        DEBUG_PAUSE = z7;
        if (!DEBUG_ALL) {
            z8 = false;
        } else {
            z8 = true;
        }
        DEBUG_RECENTS = z8;
        if (!z8) {
            z9 = false;
        } else {
            z9 = true;
        }
        DEBUG_RECENTS_TRIM_TASKS = z9;
        if (!DEBUG_ALL_ACTIVITIES) {
            z10 = false;
        } else {
            z10 = true;
        }
        DEBUG_SAVED_STATE = z10;
        if (!DEBUG_ALL) {
            z11 = false;
        } else {
            z11 = true;
        }
        DEBUG_STACK = z11;
        if (!DEBUG_ALL_ACTIVITIES) {
            z12 = false;
        } else {
            z12 = true;
        }
        DEBUG_STATES = z12;
        if (!DEBUG_ALL) {
            z13 = false;
        } else {
            z13 = true;
        }
        DEBUG_SWITCH = z13;
        if (!DEBUG_ALL) {
            z14 = false;
        } else {
            z14 = true;
        }
        DEBUG_TASKS = z14;
        if (!DEBUG_ALL) {
            z15 = false;
        } else {
            z15 = true;
        }
        DEBUG_TRANSITION = z15;
        if (!DEBUG_ALL) {
            z16 = false;
        } else {
            z16 = true;
        }
        DEBUG_VISIBILITY = z16;
        if (!DEBUG_ALL_ACTIVITIES) {
            z17 = false;
        } else {
            z17 = true;
        }
        DEBUG_APP = z17;
        if (!DEBUG_ALL_ACTIVITIES) {
            z18 = false;
        } else {
            z18 = true;
        }
        DEBUG_IDLE = z18;
        if (!DEBUG_ALL_ACTIVITIES) {
            z19 = false;
        } else {
            z19 = true;
        }
        DEBUG_RELEASE = z19;
        if (!DEBUG_ALL) {
            z20 = false;
        } else {
            z20 = true;
        }
        DEBUG_USER_LEAVING = z20;
        if (!DEBUG_ALL) {
            z21 = false;
        } else {
            z21 = true;
        }
        DEBUG_PERMISSIONS_REVIEW = z21;
        if (!DEBUG_ALL) {
            z22 = false;
        } else {
            z22 = true;
        }
        DEBUG_RESULTS = z22;
        if (!DEBUG_ALL) {
            z23 = false;
        } else {
            z23 = true;
        }
        DEBUG_ACTIVITY_STARTS = z23;
        if (!DEBUG_ALL) {
            z24 = false;
        } else {
            z24 = true;
        }
        DEBUG_CLEANUP = z24;
        if (!DEBUG_ALL) {
            z25 = false;
        } else {
            z25 = true;
        }
        DEBUG_METRICS = z25;
        DEBUG_OTHER = SystemProperties.getBoolean("persist.sys.amslog.debug", false);
        if (DEBUG_ALL) {
            z26 = true;
        }
        DEBUG_APP_SHARE = z26;
        POSTFIX_APP = APPEND_CATEGORY_NAME ? "_App" : "";
        POSTFIX_CLEANUP = APPEND_CATEGORY_NAME ? "_Cleanup" : "";
        POSTFIX_IDLE = APPEND_CATEGORY_NAME ? "_Idle" : "";
        POSTFIX_RELEASE = APPEND_CATEGORY_NAME ? "_Release" : "";
        POSTFIX_USER_LEAVING = APPEND_CATEGORY_NAME ? "_UserLeaving" : "";
        POSTFIX_ADD_REMOVE = APPEND_CATEGORY_NAME ? "_AddRemove" : "";
        POSTFIX_CONFIGURATION = APPEND_CATEGORY_NAME ? "_Configuration" : "";
        POSTFIX_CONTAINERS = APPEND_CATEGORY_NAME ? "_Containers" : "";
        POSTFIX_FOCUS = APPEND_CATEGORY_NAME ? "_Focus" : "";
        POSTFIX_IMMERSIVE = APPEND_CATEGORY_NAME ? "_Immersive" : "";
        POSTFIX_LOCKTASK = APPEND_CATEGORY_NAME ? "_LockTask" : "";
        POSTFIX_PAUSE = APPEND_CATEGORY_NAME ? "_Pause" : "";
        POSTFIX_RECENTS = APPEND_CATEGORY_NAME ? "_Recents" : "";
        POSTFIX_SAVED_STATE = APPEND_CATEGORY_NAME ? "_SavedState" : "";
        POSTFIX_STACK = APPEND_CATEGORY_NAME ? "_Stack" : "";
        POSTFIX_STATES = APPEND_CATEGORY_NAME ? "_States" : "";
        POSTFIX_SWITCH = APPEND_CATEGORY_NAME ? "_Switch" : "";
        POSTFIX_TASKS = APPEND_CATEGORY_NAME ? "_Tasks" : "";
        POSTFIX_TRANSITION = APPEND_CATEGORY_NAME ? "_Transition" : "";
        POSTFIX_VISIBILITY = APPEND_CATEGORY_NAME ? "_Visibility" : "";
        POSTFIX_RESULTS = APPEND_CATEGORY_NAME ? "_Results" : "";
    }
}