package com.vivo.services.rms.cgrp;

/* loaded from: classes.dex */
public class CgrpPolicy {
    public static final int BACKGROUND = 2;
    public static final int FOREGROUND = 6;
    public static final int H_BACKGROUND = 4;
    public static final int L_BACKGROUND = 1;
    public static final int L_FOREGROUND = 5;
    public static final int RESTRICTED = 8;
    public static final int SYSTEM_BACKGROUND = 3;
    public static final int TOP_APP = 7;

    public static String group2String(int originGroup) {
        if (originGroup != 0) {
            if (originGroup != 1) {
                if (originGroup != 2) {
                    if (originGroup == 3 || originGroup == 4) {
                        return "T";
                    }
                    String schedString = String.valueOf(originGroup);
                    return schedString;
                }
                return "F";
            }
            return "R";
        }
        return "B";
    }

    public static String policy2String(int cgrpGroup) {
        switch (cgrpGroup) {
            case 1:
                return "LB";
            case 2:
                return "B";
            case 3:
                return "SB";
            case 4:
                return "HB";
            case 5:
                return "LF";
            case 6:
                return "F";
            case 7:
                return "T";
            case 8:
                return "R";
            default:
                return String.valueOf(cgrpGroup);
        }
    }

    public static boolean isSame(int policy, int group) {
        return group != 0 ? group != 1 ? (group == 3 || group == 4) ? policy == 7 : policy == 6 : policy == 8 : policy == 2;
    }

    public static int group2Policy(int group) {
        if (group != 0) {
            if (group != 1) {
                if (group == 3 || group == 4) {
                    return 7;
                }
                return 6;
            }
            return 8;
        }
        return 2;
    }
}