package com.android.server.am;

import android.util.ArrayMap;
import android.util.ArraySet;

/* loaded from: classes.dex */
public class RMProcHelper {
    public static final int ID_arrayset_services = 9;
    public static final int ID_boolean_cached = 7;
    public static final int ID_boolean_foregroundActivities = 5;
    public static final int ID_boolean_foregroundServices = 6;
    public static final int ID_boolean_hasActivities = 8;
    public static final int ID_boolean_hasShownUi = 4;
    public static final int ID_int_adjSource = 3;
    public static final int ID_int_appInfo_Flag = 16;
    public static final int ID_int_curAdj = 0;
    public static final int ID_int_curProcState = 2;
    public static final int ID_int_curRawAdj = 13;
    public static final int ID_int_curSchedGroup = 1;
    public static final int ID_int_freezeFlag = 15;
    public static final int ID_int_setSchedGroup = 14;
    public static final int ID_int_trimMemoryLevel = 11;
    public static final int ID_long_lastProviderTime = 12;
    public static final int ID_string_adjType = 10;
    public static final String TAG = "rms";

    public static void initialize() {
    }

    public static int getInt(Object pr, int id) {
        ProcessRecord app = (ProcessRecord) pr;
        if (id != 0) {
            if (id != 1) {
                if (id != 2) {
                    if (id != 3) {
                        if (id != 11) {
                            switch (id) {
                                case 13:
                                    return app.getCurRawAdj();
                                case 14:
                                    return app.setSchedGroup;
                                case 15:
                                    return app.mFreezeFlags;
                                case 16:
                                    if (app.info != null) {
                                        return app.info.flags;
                                    }
                                    return 0;
                                default:
                                    return 0;
                            }
                        }
                        return app.trimMemoryLevel;
                    }
                    return getAdjSourcePid(app.adjSource);
                }
                return app.getCurProcState();
            }
            return app.getCurrentSchedulingGroup();
        }
        return app.curAdj;
    }

    public static void setInt(Object pr, int id, int value) {
        ProcessRecord app = (ProcessRecord) pr;
        if (id == 0) {
            app.curAdj = value;
        } else if (id == 1) {
            app.setCurrentSchedulingGroup(value);
        } else if (id == 2) {
            app.setCurProcState(value);
        } else if (id == 11) {
            app.trimMemoryLevel = value;
        } else if (id == 13) {
            app.setCurRawAdj(value);
        }
    }

    public static long getLong(Object pr, int id) {
        ProcessRecord app = (ProcessRecord) pr;
        if (id == 12) {
            return app.lastProviderTime;
        }
        return 0L;
    }

    public static void setBoolean(Object pr, int id, boolean value) {
        ProcessRecord app = (ProcessRecord) pr;
        if (id == 7) {
            app.setCached(value);
        }
    }

    public static boolean getBoolean(Object pr, int id) {
        ProcessRecord app = (ProcessRecord) pr;
        switch (id) {
            case 4:
                return app.hasShownUi;
            case 5:
                return app.hasForegroundActivities();
            case 6:
                return app.hasForegroundServices();
            case 7:
                return app.isCached();
            case 8:
                return app.getActivitySizeLockFree() > 0;
            default:
                return false;
        }
    }

    public static void setString(Object pr, int id, String value) {
        ProcessRecord app = (ProcessRecord) pr;
        if (id == 10) {
            app.adjType = value;
        }
    }

    public static String getString(Object pr, int id) {
        ProcessRecord app = (ProcessRecord) pr;
        if (id == 10) {
            return app.adjType;
        }
        return null;
    }

    public static ArraySet<?> getArraySet(Object pr, int id) {
        ProcessRecord app = (ProcessRecord) pr;
        if (id == 9) {
            return app.getServices();
        }
        return null;
    }

    public static boolean isRunningRemoteAnimation(ProcessRecord app) {
        return app.runningRemoteAnimation;
    }

    public static RMProcInfo getInfo(ProcessRecord app) {
        return (RMProcInfo) app.getVivoInjectInstance().getRMProcInfo();
    }

    private static int getAdjSourcePid(Object adjSource) {
        if (adjSource == null || !(adjSource instanceof ProcessRecord)) {
            return 0;
        }
        return ((ProcessRecord) adjSource).pid;
    }

    public static void setLong(Object pr, int id, long value) {
    }

    public static ArrayMap<String, ?> getArrayMap(Object pr, int id) {
        return null;
    }

    public static Object getObject(Object pr, int id) {
        return null;
    }

    public static void setOject(Object pr, int id, Object value) {
    }
}