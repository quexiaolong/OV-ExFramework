package com.android.server.am;

import android.util.EventLog;

/* loaded from: classes.dex */
public class EventLogTags {
    public static final int AM_ANR = 30008;
    public static final int AM_APP_FROZEN = 30082;
    public static final int AM_APP_UNFROZEN = 30083;
    public static final int AM_BROADCAST_DISCARD_APP = 30025;
    public static final int AM_BROADCAST_DISCARD_FILTER = 30024;
    public static final int AM_COMPACT = 30063;
    public static final int AM_CRASH = 30039;
    public static final int AM_CREATE_SERVICE = 30030;
    public static final int AM_DESTROY_SERVICE = 30031;
    public static final int AM_DROP_PROCESS = 30033;
    public static final int AM_FREEZE = 30068;
    public static final int AM_KILL = 30023;
    public static final int AM_LOW_MEMORY = 30017;
    public static final int AM_MEMINFO = 30046;
    public static final int AM_MEM_FACTOR = 30050;
    public static final int AM_PRE_BOOT = 30045;
    public static final int AM_PROCESS_CRASHED_TOO_MUCH = 30032;
    public static final int AM_PROCESS_START_TIMEOUT = 30037;
    public static final int AM_PROC_BAD = 30015;
    public static final int AM_PROC_BOUND = 30010;
    public static final int AM_PROC_DIED = 30011;
    public static final int AM_PROC_GOOD = 30016;
    public static final int AM_PROC_START = 30014;
    public static final int AM_PROVIDER_LOST_PROCESS = 30036;
    public static final int AM_PSS = 30047;
    public static final int AM_SCHEDULE_SERVICE_RESTART = 30035;
    public static final int AM_SERVICE_CRASHED_TOO_MUCH = 30034;
    public static final int AM_STOP_IDLE_SERVICE = 30056;
    public static final int AM_SWITCH_USER = 30041;
    public static final int AM_UID_ACTIVE = 30054;
    public static final int AM_UID_IDLE = 30055;
    public static final int AM_UID_RUNNING = 30052;
    public static final int AM_UID_STOPPED = 30053;
    public static final int AM_UNFREEZE = 30069;
    public static final int AM_USER_STATE_CHANGED = 30051;
    public static final int AM_WTF = 30040;
    public static final int BOOT_PROGRESS_AMS_READY = 3040;
    public static final int BOOT_PROGRESS_ENABLE_SCREEN = 3050;
    public static final int CONFIGURATION_CHANGED = 2719;
    public static final int CPU = 2721;
    public static final int UC_CONTINUE_USER_SWITCH = 30080;
    public static final int UC_DISPATCH_USER_SWITCH = 30079;
    public static final int UC_FINISH_USER_BOOT = 30078;
    public static final int UC_FINISH_USER_STOPPED = 30074;
    public static final int UC_FINISH_USER_STOPPING = 30073;
    public static final int UC_FINISH_USER_UNLOCKED = 30071;
    public static final int UC_FINISH_USER_UNLOCKED_COMPLETED = 30072;
    public static final int UC_FINISH_USER_UNLOCKING = 30070;
    public static final int UC_SEND_USER_BROADCAST = 30081;
    public static final int UC_START_USER_INTERNAL = 30076;
    public static final int UC_SWITCH_USER = 30075;
    public static final int UC_UNLOCK_USER = 30077;

    private EventLogTags() {
    }

    public static void writeConfigurationChanged(int configMask) {
        EventLog.writeEvent((int) CONFIGURATION_CHANGED, configMask);
    }

    public static void writeCpu(int total, int user, int system, int iowait, int irq, int softirq) {
        EventLog.writeEvent((int) CPU, Integer.valueOf(total), Integer.valueOf(user), Integer.valueOf(system), Integer.valueOf(iowait), Integer.valueOf(irq), Integer.valueOf(softirq));
    }

    public static void writeBootProgressAmsReady(long time) {
        EventLog.writeEvent((int) BOOT_PROGRESS_AMS_READY, time);
    }

    public static void writeBootProgressEnableScreen(long time) {
        EventLog.writeEvent((int) BOOT_PROGRESS_ENABLE_SCREEN, time);
    }

    public static void writeAmAnr(int user, int pid, String packageName, int flags, String reason) {
        EventLog.writeEvent((int) AM_ANR, Integer.valueOf(user), Integer.valueOf(pid), packageName, Integer.valueOf(flags), reason);
    }

    public static void writeAmProcBound(int user, int pid, String processName) {
        EventLog.writeEvent((int) AM_PROC_BOUND, Integer.valueOf(user), Integer.valueOf(pid), processName);
    }

    public static void writeAmProcDied(int user, int pid, String processName, int oomadj, int procstate) {
        EventLog.writeEvent((int) AM_PROC_DIED, Integer.valueOf(user), Integer.valueOf(pid), processName, Integer.valueOf(oomadj), Integer.valueOf(procstate));
    }

    public static void writeAmProcStart(int user, int pid, int uid, String processName, String type, String component) {
        EventLog.writeEvent((int) AM_PROC_START, Integer.valueOf(user), Integer.valueOf(pid), Integer.valueOf(uid), processName, type, component);
    }

    public static void writeAmProcBad(int user, int uid, String processName) {
        EventLog.writeEvent((int) AM_PROC_BAD, Integer.valueOf(user), Integer.valueOf(uid), processName);
    }

    public static void writeAmProcGood(int user, int uid, String processName) {
        EventLog.writeEvent((int) AM_PROC_GOOD, Integer.valueOf(user), Integer.valueOf(uid), processName);
    }

    public static void writeAmLowMemory(int numProcesses) {
        EventLog.writeEvent((int) AM_LOW_MEMORY, numProcesses);
    }

    public static void writeAmKill(int user, int pid, String processName, int oomadj, String reason) {
        EventLog.writeEvent((int) AM_KILL, Integer.valueOf(user), Integer.valueOf(pid), processName, Integer.valueOf(oomadj), reason);
    }

    public static void writeAmBroadcastDiscardFilter(int user, int broadcast, String action, int receiverNumber, int broadcastfilter) {
        EventLog.writeEvent((int) AM_BROADCAST_DISCARD_FILTER, Integer.valueOf(user), Integer.valueOf(broadcast), action, Integer.valueOf(receiverNumber), Integer.valueOf(broadcastfilter));
    }

    public static void writeAmBroadcastDiscardApp(int user, int broadcast, String action, int receiverNumber, String app) {
        EventLog.writeEvent((int) AM_BROADCAST_DISCARD_APP, Integer.valueOf(user), Integer.valueOf(broadcast), action, Integer.valueOf(receiverNumber), app);
    }

    public static void writeAmCreateService(int user, int serviceRecord, String name, int uid, int pid) {
        EventLog.writeEvent((int) AM_CREATE_SERVICE, Integer.valueOf(user), Integer.valueOf(serviceRecord), name, Integer.valueOf(uid), Integer.valueOf(pid));
    }

    public static void writeAmDestroyService(int user, int serviceRecord, int pid) {
        EventLog.writeEvent((int) AM_DESTROY_SERVICE, Integer.valueOf(user), Integer.valueOf(serviceRecord), Integer.valueOf(pid));
    }

    public static void writeAmProcessCrashedTooMuch(int user, String name, int pid) {
        EventLog.writeEvent((int) AM_PROCESS_CRASHED_TOO_MUCH, Integer.valueOf(user), name, Integer.valueOf(pid));
    }

    public static void writeAmDropProcess(int pid) {
        EventLog.writeEvent((int) AM_DROP_PROCESS, pid);
    }

    public static void writeAmServiceCrashedTooMuch(int user, int crashCount, String componentName, int pid) {
        EventLog.writeEvent((int) AM_SERVICE_CRASHED_TOO_MUCH, Integer.valueOf(user), Integer.valueOf(crashCount), componentName, Integer.valueOf(pid));
    }

    public static void writeAmScheduleServiceRestart(int user, String componentName, long time) {
        EventLog.writeEvent((int) AM_SCHEDULE_SERVICE_RESTART, Integer.valueOf(user), componentName, Long.valueOf(time));
    }

    public static void writeAmProviderLostProcess(int user, String packageName, int uid, String name) {
        EventLog.writeEvent((int) AM_PROVIDER_LOST_PROCESS, Integer.valueOf(user), packageName, Integer.valueOf(uid), name);
    }

    public static void writeAmProcessStartTimeout(int user, int pid, int uid, String processName) {
        EventLog.writeEvent((int) AM_PROCESS_START_TIMEOUT, Integer.valueOf(user), Integer.valueOf(pid), Integer.valueOf(uid), processName);
    }

    public static void writeAmCrash(int user, int pid, String processName, int flags, String exception, String message, String file, int line) {
        EventLog.writeEvent((int) AM_CRASH, Integer.valueOf(user), Integer.valueOf(pid), processName, Integer.valueOf(flags), exception, message, file, Integer.valueOf(line));
    }

    public static void writeAmWtf(int user, int pid, String processName, int flags, String tag, String message) {
        EventLog.writeEvent((int) AM_WTF, Integer.valueOf(user), Integer.valueOf(pid), processName, Integer.valueOf(flags), tag, message);
    }

    public static void writeAmSwitchUser(int id) {
        EventLog.writeEvent((int) AM_SWITCH_USER, id);
    }

    public static void writeAmPreBoot(int user, String package_) {
        EventLog.writeEvent((int) AM_PRE_BOOT, Integer.valueOf(user), package_);
    }

    public static void writeAmMeminfo(long cached, long free, long zram, long kernel, long native_) {
        EventLog.writeEvent((int) AM_MEMINFO, Long.valueOf(cached), Long.valueOf(free), Long.valueOf(zram), Long.valueOf(kernel), Long.valueOf(native_));
    }

    public static void writeAmPss(int pid, int uid, String processName, long pss, long uss, long swappss, long rss, int stattype, int procstate, long timetocollect) {
        EventLog.writeEvent((int) AM_PSS, Integer.valueOf(pid), Integer.valueOf(uid), processName, Long.valueOf(pss), Long.valueOf(uss), Long.valueOf(swappss), Long.valueOf(rss), Integer.valueOf(stattype), Integer.valueOf(procstate), Long.valueOf(timetocollect));
    }

    public static void writeAmMemFactor(int current, int previous) {
        EventLog.writeEvent((int) AM_MEM_FACTOR, Integer.valueOf(current), Integer.valueOf(previous));
    }

    public static void writeAmUserStateChanged(int id, int state) {
        EventLog.writeEvent((int) AM_USER_STATE_CHANGED, Integer.valueOf(id), Integer.valueOf(state));
    }

    public static void writeAmUidRunning(int uid) {
        EventLog.writeEvent((int) AM_UID_RUNNING, uid);
    }

    public static void writeAmUidStopped(int uid) {
        EventLog.writeEvent((int) AM_UID_STOPPED, uid);
    }

    public static void writeAmUidActive(int uid) {
        EventLog.writeEvent((int) AM_UID_ACTIVE, uid);
    }

    public static void writeAmUidIdle(int uid) {
        EventLog.writeEvent((int) AM_UID_IDLE, uid);
    }

    public static void writeAmStopIdleService(int uid, String componentName) {
        EventLog.writeEvent((int) AM_STOP_IDLE_SERVICE, Integer.valueOf(uid), componentName);
    }

    public static void writeAmCompact(int pid, String processName, String action, long beforersstotal, long beforerssfile, long beforerssanon, long beforerssswap, long deltarsstotal, long deltarssfile, long deltarssanon, long deltarssswap, long time, int lastaction, long lastactiontimestamp, int setadj, int procstate, long beforezramfree, long deltazramfree) {
        EventLog.writeEvent((int) AM_COMPACT, Integer.valueOf(pid), processName, action, Long.valueOf(beforersstotal), Long.valueOf(beforerssfile), Long.valueOf(beforerssanon), Long.valueOf(beforerssswap), Long.valueOf(deltarsstotal), Long.valueOf(deltarssfile), Long.valueOf(deltarssanon), Long.valueOf(deltarssswap), Long.valueOf(time), Integer.valueOf(lastaction), Long.valueOf(lastactiontimestamp), Integer.valueOf(setadj), Integer.valueOf(procstate), Long.valueOf(beforezramfree), Long.valueOf(deltazramfree));
    }

    public static void writeAmFreeze(int pid, String processName) {
        EventLog.writeEvent((int) AM_FREEZE, Integer.valueOf(pid), processName);
    }

    public static void writeAmUnfreeze(int pid, String processName) {
        EventLog.writeEvent((int) AM_UNFREEZE, Integer.valueOf(pid), processName);
    }

    public static void writeUcFinishUserUnlocking(int userid) {
        EventLog.writeEvent((int) UC_FINISH_USER_UNLOCKING, userid);
    }

    public static void writeUcFinishUserUnlocked(int userid) {
        EventLog.writeEvent((int) UC_FINISH_USER_UNLOCKED, userid);
    }

    public static void writeUcFinishUserUnlockedCompleted(int userid) {
        EventLog.writeEvent((int) UC_FINISH_USER_UNLOCKED_COMPLETED, userid);
    }

    public static void writeUcFinishUserStopping(int userid) {
        EventLog.writeEvent((int) UC_FINISH_USER_STOPPING, userid);
    }

    public static void writeUcFinishUserStopped(int userid) {
        EventLog.writeEvent((int) UC_FINISH_USER_STOPPED, userid);
    }

    public static void writeUcSwitchUser(int userid) {
        EventLog.writeEvent((int) UC_SWITCH_USER, userid);
    }

    public static void writeUcStartUserInternal(int userid) {
        EventLog.writeEvent((int) UC_START_USER_INTERNAL, userid);
    }

    public static void writeUcUnlockUser(int userid) {
        EventLog.writeEvent((int) UC_UNLOCK_USER, userid);
    }

    public static void writeUcFinishUserBoot(int userid) {
        EventLog.writeEvent((int) UC_FINISH_USER_BOOT, userid);
    }

    public static void writeUcDispatchUserSwitch(int olduserid) {
        EventLog.writeEvent((int) UC_DISPATCH_USER_SWITCH, olduserid);
    }

    public static void writeUcContinueUserSwitch(int olduserid) {
        EventLog.writeEvent((int) UC_CONTINUE_USER_SWITCH, olduserid);
    }

    public static void writeUcSendUserBroadcast(int userid, String intentaction) {
        EventLog.writeEvent((int) UC_SEND_USER_BROADCAST, Integer.valueOf(userid), intentaction);
    }

    public static void writeAmAppFrozen(int user, int uid, String packageName, String reason) {
        EventLog.writeEvent((int) AM_APP_FROZEN, Integer.valueOf(user), Integer.valueOf(uid), packageName, reason);
    }

    public static void writeAmAppUnfrozen(int user, int uid, String packageName, String reason) {
        EventLog.writeEvent((int) AM_APP_UNFROZEN, Integer.valueOf(user), Integer.valueOf(uid), packageName, reason);
    }
}