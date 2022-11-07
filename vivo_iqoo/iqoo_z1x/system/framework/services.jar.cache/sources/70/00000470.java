package com.android.server;

import android.util.EventLog;

/* loaded from: classes.dex */
public class EventLogTags {
    public static final int AUTO_BRIGHTNESS_ADJ = 35000;
    public static final int BACKUP_AGENT_FAILURE = 2823;
    public static final int BACKUP_DATA_CHANGED = 2820;
    public static final int BACKUP_INITIALIZE = 2827;
    public static final int BACKUP_PACKAGE = 2824;
    public static final int BACKUP_QUOTA_EXCEEDED = 2829;
    public static final int BACKUP_REQUESTED = 2828;
    public static final int BACKUP_RESET = 2826;
    public static final int BACKUP_START = 2821;
    public static final int BACKUP_SUCCESS = 2825;
    public static final int BACKUP_TRANSPORT_CONNECTION = 2851;
    public static final int BACKUP_TRANSPORT_FAILURE = 2822;
    public static final int BACKUP_TRANSPORT_LIFECYCLE = 2850;
    public static final int BATTERY_DISCHARGE = 2730;
    public static final int BATTERY_LEVEL = 2722;
    public static final int BATTERY_SAVER_MODE = 2739;
    public static final int BATTERY_SAVER_SETTING = 27392;
    public static final int BATTERY_SAVING_STATS = 27390;
    public static final int BATTERY_STATUS = 2723;
    public static final int BOOT_PROGRESS_PMS_DATA_SCAN_START = 3080;
    public static final int BOOT_PROGRESS_PMS_READY = 3100;
    public static final int BOOT_PROGRESS_PMS_SCAN_END = 3090;
    public static final int BOOT_PROGRESS_PMS_START = 3060;
    public static final int BOOT_PROGRESS_PMS_SYSTEM_SCAN_START = 3070;
    public static final int BOOT_PROGRESS_SYSTEM_RUN = 3010;
    public static final int CACHE_FILE_DELETED = 2748;
    public static final int CAMERA_GESTURE_TRIGGERED = 40100;
    public static final int CONFIG_INSTALL_FAILED = 51300;
    public static final int CONNECTIVITY_STATE_CHANGED = 50020;
    public static final int DEVICE_IDLE = 34000;
    public static final int DEVICE_IDLE_LIGHT = 34009;
    public static final int DEVICE_IDLE_LIGHT_STEP = 34010;
    public static final int DEVICE_IDLE_OFF_COMPLETE = 34008;
    public static final int DEVICE_IDLE_OFF_PHASE = 34007;
    public static final int DEVICE_IDLE_OFF_START = 34006;
    public static final int DEVICE_IDLE_ON_COMPLETE = 34005;
    public static final int DEVICE_IDLE_ON_PHASE = 34004;
    public static final int DEVICE_IDLE_ON_START = 34003;
    public static final int DEVICE_IDLE_STEP = 34001;
    public static final int DEVICE_IDLE_WAKE_FROM_IDLE = 34002;
    public static final int FSTRIM_FINISH = 2756;
    public static final int FSTRIM_START = 2755;
    public static final int FULL_BACKUP_AGENT_FAILURE = 2841;
    public static final int FULL_BACKUP_CANCELLED = 2846;
    public static final int FULL_BACKUP_PACKAGE = 2840;
    public static final int FULL_BACKUP_QUOTA_EXCEEDED = 2845;
    public static final int FULL_BACKUP_SUCCESS = 2843;
    public static final int FULL_BACKUP_TRANSPORT_FAILURE = 2842;
    public static final int FULL_RESTORE_PACKAGE = 2844;
    public static final int IDLE_MAINTENANCE_WINDOW_FINISH = 51501;
    public static final int IDLE_MAINTENANCE_WINDOW_START = 51500;
    public static final int IFW_INTENT_MATCHED = 51400;
    public static final int IMF_FORCE_RECONNECT_IME = 32000;
    public static final int JOB_DEFERRED_EXECUTION = 8000;
    public static final int LOCKDOWN_VPN_CONNECTED = 51201;
    public static final int LOCKDOWN_VPN_CONNECTING = 51200;
    public static final int LOCKDOWN_VPN_ERROR = 51202;
    public static final int NETSTATS_MOBILE_SAMPLE = 51100;
    public static final int NETSTATS_WIFI_SAMPLE = 51101;
    public static final int NOTIFICATION_ACTION_CLICKED = 27521;
    public static final int NOTIFICATION_ALERT = 27532;
    public static final int NOTIFICATION_AUTOGROUPED = 27533;
    public static final int NOTIFICATION_CANCEL = 2751;
    public static final int NOTIFICATION_CANCELED = 27530;
    public static final int NOTIFICATION_CANCEL_ALL = 2752;
    public static final int NOTIFICATION_CLICKED = 27520;
    public static final int NOTIFICATION_ENQUEUE = 2750;
    public static final int NOTIFICATION_EXPANSION = 27511;
    public static final int NOTIFICATION_PANEL_HIDDEN = 27501;
    public static final int NOTIFICATION_PANEL_REVEALED = 27500;
    public static final int NOTIFICATION_UNAUTOGROUPED = 275534;
    public static final int NOTIFICATION_VISIBILITY = 27531;
    public static final int NOTIFICATION_VISIBILITY_CHANGED = 27510;
    public static final int PM_CRITICAL_INFO = 3120;
    public static final int PM_PACKAGE_STATS = 3121;
    public static final int POWER_PARTIAL_WAKE_STATE = 2729;
    public static final int POWER_SCREEN_BROADCAST_DONE = 2726;
    public static final int POWER_SCREEN_BROADCAST_SEND = 2725;
    public static final int POWER_SCREEN_BROADCAST_STOP = 2727;
    public static final int POWER_SCREEN_STATE = 2728;
    public static final int POWER_SLEEP_REQUESTED = 2724;
    public static final int POWER_SOFT_SLEEP_REQUESTED = 2731;
    public static final int REQUEST_LOCATION_REMOVE = 40201;
    public static final int REQUEST_LOCATION_UPDATE = 40200;
    public static final int RESCUE_FAILURE = 2903;
    public static final int RESCUE_LEVEL = 2901;
    public static final int RESCUE_NOTE = 2900;
    public static final int RESCUE_SUCCESS = 2902;
    public static final int RESTORE_AGENT_FAILURE = 2832;
    public static final int RESTORE_PACKAGE = 2833;
    public static final int RESTORE_START = 2830;
    public static final int RESTORE_SUCCESS = 2834;
    public static final int RESTORE_TRANSPORT_FAILURE = 2831;
    public static final int START_NAVIGATING = 40202;
    public static final int STORAGE_STATE = 2749;
    public static final int STREAM_DEVICES_CHANGED = 40001;
    public static final int SYSTEM_SERVER_START = 3011;
    public static final int THERMAL_CHANGED = 2737;
    public static final int TIMEZONE_INSTALL_COMPLETE = 51612;
    public static final int TIMEZONE_INSTALL_STARTED = 51611;
    public static final int TIMEZONE_NOTHING_COMPLETE = 51631;
    public static final int TIMEZONE_REQUEST_INSTALL = 51610;
    public static final int TIMEZONE_REQUEST_NOTHING = 51630;
    public static final int TIMEZONE_REQUEST_UNINSTALL = 51620;
    public static final int TIMEZONE_TRIGGER_CHECK = 51600;
    public static final int TIMEZONE_UNINSTALL_COMPLETE = 51622;
    public static final int TIMEZONE_UNINSTALL_STARTED = 51621;
    public static final int UNKNOWN_SOURCES_ENABLED = 3110;
    public static final int USER_ACTIVITY_TIMEOUT_OVERRIDE = 27391;
    public static final int VOLUME_CHANGED = 40000;
    public static final int WATCHDOG = 2802;
    public static final int WATCHDOG_HARD_RESET = 2805;
    public static final int WATCHDOG_MEMINFO = 2809;
    public static final int WATCHDOG_PROC_PSS = 2803;
    public static final int WATCHDOG_PROC_STATS = 2807;
    public static final int WATCHDOG_PSS_STATS = 2806;
    public static final int WATCHDOG_REQUESTED_REBOOT = 2811;
    public static final int WATCHDOG_SCHEDULED_REBOOT = 2808;
    public static final int WATCHDOG_SOFT_RESET = 2804;
    public static final int WATCHDOG_VMSTAT = 2810;
    public static final int WP_WALLPAPER_CRASHED = 33000;

    private EventLogTags() {
    }

    public static void writeBatteryLevel(int level, int voltage, int temperature) {
        EventLog.writeEvent((int) BATTERY_LEVEL, Integer.valueOf(level), Integer.valueOf(voltage), Integer.valueOf(temperature));
    }

    public static void writeBatteryStatus(int status, int health, int present, int plugged, String technology) {
        EventLog.writeEvent((int) BATTERY_STATUS, Integer.valueOf(status), Integer.valueOf(health), Integer.valueOf(present), Integer.valueOf(plugged), technology);
    }

    public static void writeBatteryDischarge(long duration, int minlevel, int maxlevel) {
        EventLog.writeEvent((int) BATTERY_DISCHARGE, Long.valueOf(duration), Integer.valueOf(minlevel), Integer.valueOf(maxlevel));
    }

    public static void writePowerSleepRequested(int wakelockscleared) {
        EventLog.writeEvent((int) POWER_SLEEP_REQUESTED, wakelockscleared);
    }

    public static void writePowerScreenBroadcastSend(int wakelockcount) {
        EventLog.writeEvent((int) POWER_SCREEN_BROADCAST_SEND, wakelockcount);
    }

    public static void writePowerScreenBroadcastDone(int on, long broadcastduration, int wakelockcount) {
        EventLog.writeEvent((int) POWER_SCREEN_BROADCAST_DONE, Integer.valueOf(on), Long.valueOf(broadcastduration), Integer.valueOf(wakelockcount));
    }

    public static void writePowerScreenBroadcastStop(int which, int wakelockcount) {
        EventLog.writeEvent((int) POWER_SCREEN_BROADCAST_STOP, Integer.valueOf(which), Integer.valueOf(wakelockcount));
    }

    public static void writePowerScreenState(int offoron, int becauseofuser, long totaltouchdowntime, int touchcycles, int latency) {
        EventLog.writeEvent((int) POWER_SCREEN_STATE, Integer.valueOf(offoron), Integer.valueOf(becauseofuser), Long.valueOf(totaltouchdowntime), Integer.valueOf(touchcycles), Integer.valueOf(latency));
    }

    public static void writePowerPartialWakeState(int releasedoracquired, String tag) {
        EventLog.writeEvent((int) POWER_PARTIAL_WAKE_STATE, Integer.valueOf(releasedoracquired), tag);
    }

    public static void writePowerSoftSleepRequested(long savedwaketimems) {
        EventLog.writeEvent((int) POWER_SOFT_SLEEP_REQUESTED, savedwaketimems);
    }

    public static void writeBatterySaverMode(int fullprevofforon, int adaptiveprevofforon, int fullnowofforon, int adaptivenowofforon, int interactive, String features, int reason) {
        EventLog.writeEvent((int) BATTERY_SAVER_MODE, Integer.valueOf(fullprevofforon), Integer.valueOf(adaptiveprevofforon), Integer.valueOf(fullnowofforon), Integer.valueOf(adaptivenowofforon), Integer.valueOf(interactive), features, Integer.valueOf(reason));
    }

    public static void writeBatterySavingStats(int batterysaver, int interactive, int doze, long deltaDuration, int deltaBatteryDrain, int deltaBatteryDrainPercent, long totalDuration, int totalBatteryDrain, int totalBatteryDrainPercent) {
        EventLog.writeEvent((int) BATTERY_SAVING_STATS, Integer.valueOf(batterysaver), Integer.valueOf(interactive), Integer.valueOf(doze), Long.valueOf(deltaDuration), Integer.valueOf(deltaBatteryDrain), Integer.valueOf(deltaBatteryDrainPercent), Long.valueOf(totalDuration), Integer.valueOf(totalBatteryDrain), Integer.valueOf(totalBatteryDrainPercent));
    }

    public static void writeUserActivityTimeoutOverride(long override) {
        EventLog.writeEvent((int) USER_ACTIVITY_TIMEOUT_OVERRIDE, override);
    }

    public static void writeBatterySaverSetting(int threshold) {
        EventLog.writeEvent((int) BATTERY_SAVER_SETTING, threshold);
    }

    public static void writeThermalChanged(String name, int type, float temperature, int sensorStatus, int previousSystemStatus) {
        EventLog.writeEvent((int) THERMAL_CHANGED, name, Integer.valueOf(type), Float.valueOf(temperature), Integer.valueOf(sensorStatus), Integer.valueOf(previousSystemStatus));
    }

    public static void writeCacheFileDeleted(String path) {
        EventLog.writeEvent((int) CACHE_FILE_DELETED, path);
    }

    public static void writeStorageState(String uuid, int oldState, int newState, long usable, long total) {
        EventLog.writeEvent((int) STORAGE_STATE, uuid, Integer.valueOf(oldState), Integer.valueOf(newState), Long.valueOf(usable), Long.valueOf(total));
    }

    public static void writeNotificationEnqueue(int uid, int pid, String pkg, int id, String tag, int userid, String notification, int status) {
        EventLog.writeEvent((int) NOTIFICATION_ENQUEUE, Integer.valueOf(uid), Integer.valueOf(pid), pkg, Integer.valueOf(id), tag, Integer.valueOf(userid), notification, Integer.valueOf(status));
    }

    public static void writeNotificationCancel(int uid, int pid, String pkg, int id, String tag, int userid, int requiredFlags, int forbiddenFlags, int reason, String listener) {
        EventLog.writeEvent((int) NOTIFICATION_CANCEL, Integer.valueOf(uid), Integer.valueOf(pid), pkg, Integer.valueOf(id), tag, Integer.valueOf(userid), Integer.valueOf(requiredFlags), Integer.valueOf(forbiddenFlags), Integer.valueOf(reason), listener);
    }

    public static void writeNotificationCancelAll(int uid, int pid, String pkg, int userid, int requiredFlags, int forbiddenFlags, int reason, String listener) {
        EventLog.writeEvent((int) NOTIFICATION_CANCEL_ALL, Integer.valueOf(uid), Integer.valueOf(pid), pkg, Integer.valueOf(userid), Integer.valueOf(requiredFlags), Integer.valueOf(forbiddenFlags), Integer.valueOf(reason), listener);
    }

    public static void writeNotificationPanelRevealed(int items) {
        EventLog.writeEvent((int) NOTIFICATION_PANEL_REVEALED, items);
    }

    public static void writeNotificationPanelHidden() {
        EventLog.writeEvent((int) NOTIFICATION_PANEL_HIDDEN, new Object[0]);
    }

    public static void writeNotificationVisibilityChanged(String newlyvisiblekeys, String nolongervisiblekeys) {
        EventLog.writeEvent((int) NOTIFICATION_VISIBILITY_CHANGED, newlyvisiblekeys, nolongervisiblekeys);
    }

    public static void writeNotificationExpansion(String key, int userAction, int expanded, int lifespan, int freshness, int exposure) {
        EventLog.writeEvent((int) NOTIFICATION_EXPANSION, key, Integer.valueOf(userAction), Integer.valueOf(expanded), Integer.valueOf(lifespan), Integer.valueOf(freshness), Integer.valueOf(exposure));
    }

    public static void writeNotificationClicked(String key, int lifespan, int freshness, int exposure, int rank, int count) {
        EventLog.writeEvent((int) NOTIFICATION_CLICKED, key, Integer.valueOf(lifespan), Integer.valueOf(freshness), Integer.valueOf(exposure), Integer.valueOf(rank), Integer.valueOf(count));
    }

    public static void writeNotificationActionClicked(String key, int actionIndex, int lifespan, int freshness, int exposure, int rank, int count) {
        EventLog.writeEvent((int) NOTIFICATION_ACTION_CLICKED, key, Integer.valueOf(actionIndex), Integer.valueOf(lifespan), Integer.valueOf(freshness), Integer.valueOf(exposure), Integer.valueOf(rank), Integer.valueOf(count));
    }

    public static void writeNotificationCanceled(String key, int reason, int lifespan, int freshness, int exposure, int rank, int count, String listener) {
        EventLog.writeEvent((int) NOTIFICATION_CANCELED, key, Integer.valueOf(reason), Integer.valueOf(lifespan), Integer.valueOf(freshness), Integer.valueOf(exposure), Integer.valueOf(rank), Integer.valueOf(count), listener);
    }

    public static void writeNotificationVisibility(String key, int visibile, int lifespan, int freshness, int exposure, int rank) {
        EventLog.writeEvent((int) NOTIFICATION_VISIBILITY, key, Integer.valueOf(visibile), Integer.valueOf(lifespan), Integer.valueOf(freshness), Integer.valueOf(exposure), Integer.valueOf(rank));
    }

    public static void writeNotificationAlert(String key, int buzz, int beep, int blink) {
        EventLog.writeEvent((int) NOTIFICATION_ALERT, key, Integer.valueOf(buzz), Integer.valueOf(beep), Integer.valueOf(blink));
    }

    public static void writeNotificationAutogrouped(String key) {
        EventLog.writeEvent((int) NOTIFICATION_AUTOGROUPED, key);
    }

    public static void writeNotificationUnautogrouped(String key) {
        EventLog.writeEvent((int) NOTIFICATION_UNAUTOGROUPED, key);
    }

    public static void writeWatchdog(String service) {
        EventLog.writeEvent((int) WATCHDOG, service);
    }

    public static void writeWatchdogProcPss(String process, int pid, int pss) {
        EventLog.writeEvent((int) WATCHDOG_PROC_PSS, process, Integer.valueOf(pid), Integer.valueOf(pss));
    }

    public static void writeWatchdogSoftReset(String process, int pid, int maxpss, int pss, String skip) {
        EventLog.writeEvent((int) WATCHDOG_SOFT_RESET, process, Integer.valueOf(pid), Integer.valueOf(maxpss), Integer.valueOf(pss), skip);
    }

    public static void writeWatchdogHardReset(String process, int pid, int maxpss, int pss) {
        EventLog.writeEvent((int) WATCHDOG_HARD_RESET, process, Integer.valueOf(pid), Integer.valueOf(maxpss), Integer.valueOf(pss));
    }

    public static void writeWatchdogPssStats(int emptypss, int emptycount, int backgroundpss, int backgroundcount, int servicepss, int servicecount, int visiblepss, int visiblecount, int foregroundpss, int foregroundcount, int nopsscount) {
        EventLog.writeEvent((int) WATCHDOG_PSS_STATS, Integer.valueOf(emptypss), Integer.valueOf(emptycount), Integer.valueOf(backgroundpss), Integer.valueOf(backgroundcount), Integer.valueOf(servicepss), Integer.valueOf(servicecount), Integer.valueOf(visiblepss), Integer.valueOf(visiblecount), Integer.valueOf(foregroundpss), Integer.valueOf(foregroundcount), Integer.valueOf(nopsscount));
    }

    public static void writeWatchdogProcStats(int deathsinone, int deathsintwo, int deathsinthree, int deathsinfour, int deathsinfive) {
        EventLog.writeEvent((int) WATCHDOG_PROC_STATS, Integer.valueOf(deathsinone), Integer.valueOf(deathsintwo), Integer.valueOf(deathsinthree), Integer.valueOf(deathsinfour), Integer.valueOf(deathsinfive));
    }

    public static void writeWatchdogScheduledReboot(long now, int interval, int starttime, int window, String skip) {
        EventLog.writeEvent((int) WATCHDOG_SCHEDULED_REBOOT, Long.valueOf(now), Integer.valueOf(interval), Integer.valueOf(starttime), Integer.valueOf(window), skip);
    }

    public static void writeWatchdogMeminfo(int memfree, int buffers, int cached, int active, int inactive, int anonpages, int mapped, int slab, int sreclaimable, int sunreclaim, int pagetables) {
        EventLog.writeEvent((int) WATCHDOG_MEMINFO, Integer.valueOf(memfree), Integer.valueOf(buffers), Integer.valueOf(cached), Integer.valueOf(active), Integer.valueOf(inactive), Integer.valueOf(anonpages), Integer.valueOf(mapped), Integer.valueOf(slab), Integer.valueOf(sreclaimable), Integer.valueOf(sunreclaim), Integer.valueOf(pagetables));
    }

    public static void writeWatchdogVmstat(long runtime, int pgfree, int pgactivate, int pgdeactivate, int pgfault, int pgmajfault) {
        EventLog.writeEvent((int) WATCHDOG_VMSTAT, Long.valueOf(runtime), Integer.valueOf(pgfree), Integer.valueOf(pgactivate), Integer.valueOf(pgdeactivate), Integer.valueOf(pgfault), Integer.valueOf(pgmajfault));
    }

    public static void writeWatchdogRequestedReboot(int nowait, int scheduleinterval, int recheckinterval, int starttime, int window, int minscreenoff, int minnextalarm) {
        EventLog.writeEvent((int) WATCHDOG_REQUESTED_REBOOT, Integer.valueOf(nowait), Integer.valueOf(scheduleinterval), Integer.valueOf(recheckinterval), Integer.valueOf(starttime), Integer.valueOf(window), Integer.valueOf(minscreenoff), Integer.valueOf(minnextalarm));
    }

    public static void writeRescueNote(int uid, int count, long window) {
        EventLog.writeEvent((int) RESCUE_NOTE, Integer.valueOf(uid), Integer.valueOf(count), Long.valueOf(window));
    }

    public static void writeRescueLevel(int level, int triggerUid) {
        EventLog.writeEvent((int) RESCUE_LEVEL, Integer.valueOf(level), Integer.valueOf(triggerUid));
    }

    public static void writeRescueSuccess(int level) {
        EventLog.writeEvent((int) RESCUE_SUCCESS, level);
    }

    public static void writeRescueFailure(int level, String msg) {
        EventLog.writeEvent((int) RESCUE_FAILURE, Integer.valueOf(level), msg);
    }

    public static void writeBackupDataChanged(String package_) {
        EventLog.writeEvent((int) BACKUP_DATA_CHANGED, package_);
    }

    public static void writeBackupStart(String transport) {
        EventLog.writeEvent((int) BACKUP_START, transport);
    }

    public static void writeBackupTransportFailure(String package_) {
        EventLog.writeEvent((int) BACKUP_TRANSPORT_FAILURE, package_);
    }

    public static void writeBackupAgentFailure(String package_, String message) {
        EventLog.writeEvent((int) BACKUP_AGENT_FAILURE, package_, message);
    }

    public static void writeBackupPackage(String package_, int size) {
        EventLog.writeEvent((int) BACKUP_PACKAGE, package_, Integer.valueOf(size));
    }

    public static void writeBackupSuccess(int packages, int time) {
        EventLog.writeEvent((int) BACKUP_SUCCESS, Integer.valueOf(packages), Integer.valueOf(time));
    }

    public static void writeBackupReset(String transport) {
        EventLog.writeEvent((int) BACKUP_RESET, transport);
    }

    public static void writeBackupInitialize() {
        EventLog.writeEvent((int) BACKUP_INITIALIZE, new Object[0]);
    }

    public static void writeBackupRequested(int total, int keyValue, int full) {
        EventLog.writeEvent((int) BACKUP_REQUESTED, Integer.valueOf(total), Integer.valueOf(keyValue), Integer.valueOf(full));
    }

    public static void writeBackupQuotaExceeded(String package_) {
        EventLog.writeEvent((int) BACKUP_QUOTA_EXCEEDED, package_);
    }

    public static void writeRestoreStart(String transport, long source) {
        EventLog.writeEvent((int) RESTORE_START, transport, Long.valueOf(source));
    }

    public static void writeRestoreTransportFailure() {
        EventLog.writeEvent((int) RESTORE_TRANSPORT_FAILURE, new Object[0]);
    }

    public static void writeRestoreAgentFailure(String package_, String message) {
        EventLog.writeEvent((int) RESTORE_AGENT_FAILURE, package_, message);
    }

    public static void writeRestorePackage(String package_, int size) {
        EventLog.writeEvent((int) RESTORE_PACKAGE, package_, Integer.valueOf(size));
    }

    public static void writeRestoreSuccess(int packages, int time) {
        EventLog.writeEvent((int) RESTORE_SUCCESS, Integer.valueOf(packages), Integer.valueOf(time));
    }

    public static void writeFullBackupPackage(String package_) {
        EventLog.writeEvent((int) FULL_BACKUP_PACKAGE, package_);
    }

    public static void writeFullBackupAgentFailure(String package_, String message) {
        EventLog.writeEvent((int) FULL_BACKUP_AGENT_FAILURE, package_, message);
    }

    public static void writeFullBackupTransportFailure() {
        EventLog.writeEvent((int) FULL_BACKUP_TRANSPORT_FAILURE, new Object[0]);
    }

    public static void writeFullBackupSuccess(String package_) {
        EventLog.writeEvent((int) FULL_BACKUP_SUCCESS, package_);
    }

    public static void writeFullRestorePackage(String package_) {
        EventLog.writeEvent((int) FULL_RESTORE_PACKAGE, package_);
    }

    public static void writeFullBackupQuotaExceeded(String package_) {
        EventLog.writeEvent((int) FULL_BACKUP_QUOTA_EXCEEDED, package_);
    }

    public static void writeFullBackupCancelled(String package_, String message) {
        EventLog.writeEvent((int) FULL_BACKUP_CANCELLED, package_, message);
    }

    public static void writeBackupTransportLifecycle(String transport, int bound) {
        EventLog.writeEvent((int) BACKUP_TRANSPORT_LIFECYCLE, transport, Integer.valueOf(bound));
    }

    public static void writeBackupTransportConnection(String transport, int connected) {
        EventLog.writeEvent((int) BACKUP_TRANSPORT_CONNECTION, transport, Integer.valueOf(connected));
    }

    public static void writeBootProgressSystemRun(long time) {
        EventLog.writeEvent((int) BOOT_PROGRESS_SYSTEM_RUN, time);
    }

    public static void writeSystemServerStart(int startCount, long uptime, long elapseTime) {
        EventLog.writeEvent((int) SYSTEM_SERVER_START, Integer.valueOf(startCount), Long.valueOf(uptime), Long.valueOf(elapseTime));
    }

    public static void writeBootProgressPmsStart(long time) {
        EventLog.writeEvent((int) BOOT_PROGRESS_PMS_START, time);
    }

    public static void writeBootProgressPmsSystemScanStart(long time) {
        EventLog.writeEvent((int) BOOT_PROGRESS_PMS_SYSTEM_SCAN_START, time);
    }

    public static void writeBootProgressPmsDataScanStart(long time) {
        EventLog.writeEvent((int) BOOT_PROGRESS_PMS_DATA_SCAN_START, time);
    }

    public static void writeBootProgressPmsScanEnd(long time) {
        EventLog.writeEvent((int) BOOT_PROGRESS_PMS_SCAN_END, time);
    }

    public static void writeBootProgressPmsReady(long time) {
        EventLog.writeEvent((int) BOOT_PROGRESS_PMS_READY, time);
    }

    public static void writeUnknownSourcesEnabled(int value) {
        EventLog.writeEvent((int) UNKNOWN_SOURCES_ENABLED, value);
    }

    public static void writePmCriticalInfo(String msg) {
        EventLog.writeEvent((int) PM_CRITICAL_INFO, msg);
    }

    public static void writePmPackageStats(long manualTime, long quotaTime, long manualData, long quotaData, long manualCache, long quotaCache) {
        EventLog.writeEvent((int) PM_PACKAGE_STATS, Long.valueOf(manualTime), Long.valueOf(quotaTime), Long.valueOf(manualData), Long.valueOf(quotaData), Long.valueOf(manualCache), Long.valueOf(quotaCache));
    }

    public static void writeImfForceReconnectIme(Object[] ime, long timeSinceConnect, int showing) {
        EventLog.writeEvent((int) IMF_FORCE_RECONNECT_IME, ime, Long.valueOf(timeSinceConnect), Integer.valueOf(showing));
    }

    public static void writeWpWallpaperCrashed(String component) {
        EventLog.writeEvent((int) WP_WALLPAPER_CRASHED, component);
    }

    public static void writeDeviceIdle(int state, String reason) {
        EventLog.writeEvent((int) DEVICE_IDLE, Integer.valueOf(state), reason);
    }

    public static void writeDeviceIdleStep() {
        EventLog.writeEvent((int) DEVICE_IDLE_STEP, new Object[0]);
    }

    public static void writeDeviceIdleWakeFromIdle(int isIdle, String reason) {
        EventLog.writeEvent((int) DEVICE_IDLE_WAKE_FROM_IDLE, Integer.valueOf(isIdle), reason);
    }

    public static void writeDeviceIdleOnStart() {
        EventLog.writeEvent((int) DEVICE_IDLE_ON_START, new Object[0]);
    }

    public static void writeDeviceIdleOnPhase(String what) {
        EventLog.writeEvent((int) DEVICE_IDLE_ON_PHASE, what);
    }

    public static void writeDeviceIdleOnComplete() {
        EventLog.writeEvent((int) DEVICE_IDLE_ON_COMPLETE, new Object[0]);
    }

    public static void writeDeviceIdleOffStart(String reason) {
        EventLog.writeEvent((int) DEVICE_IDLE_OFF_START, reason);
    }

    public static void writeDeviceIdleOffPhase(String what) {
        EventLog.writeEvent((int) DEVICE_IDLE_OFF_PHASE, what);
    }

    public static void writeDeviceIdleOffComplete() {
        EventLog.writeEvent((int) DEVICE_IDLE_OFF_COMPLETE, new Object[0]);
    }

    public static void writeDeviceIdleLight(int state, String reason) {
        EventLog.writeEvent((int) DEVICE_IDLE_LIGHT, Integer.valueOf(state), reason);
    }

    public static void writeDeviceIdleLightStep() {
        EventLog.writeEvent((int) DEVICE_IDLE_LIGHT_STEP, new Object[0]);
    }

    public static void writeAutoBrightnessAdj(float oldLux, float oldBrightness, float newLux, float newBrightness) {
        EventLog.writeEvent((int) AUTO_BRIGHTNESS_ADJ, Float.valueOf(oldLux), Float.valueOf(oldBrightness), Float.valueOf(newLux), Float.valueOf(newBrightness));
    }

    public static void writeConnectivityStateChanged(int type, int subtype, int state) {
        EventLog.writeEvent((int) CONNECTIVITY_STATE_CHANGED, Integer.valueOf(type), Integer.valueOf(subtype), Integer.valueOf(state));
    }

    public static void writeNetstatsMobileSample(long devRxBytes, long devTxBytes, long devRxPkts, long devTxPkts, long xtRxBytes, long xtTxBytes, long xtRxPkts, long xtTxPkts, long uidRxBytes, long uidTxBytes, long uidRxPkts, long uidTxPkts, long trustedTime) {
        EventLog.writeEvent((int) NETSTATS_MOBILE_SAMPLE, Long.valueOf(devRxBytes), Long.valueOf(devTxBytes), Long.valueOf(devRxPkts), Long.valueOf(devTxPkts), Long.valueOf(xtRxBytes), Long.valueOf(xtTxBytes), Long.valueOf(xtRxPkts), Long.valueOf(xtTxPkts), Long.valueOf(uidRxBytes), Long.valueOf(uidTxBytes), Long.valueOf(uidRxPkts), Long.valueOf(uidTxPkts), Long.valueOf(trustedTime));
    }

    public static void writeNetstatsWifiSample(long devRxBytes, long devTxBytes, long devRxPkts, long devTxPkts, long xtRxBytes, long xtTxBytes, long xtRxPkts, long xtTxPkts, long uidRxBytes, long uidTxBytes, long uidRxPkts, long uidTxPkts, long trustedTime) {
        EventLog.writeEvent((int) NETSTATS_WIFI_SAMPLE, Long.valueOf(devRxBytes), Long.valueOf(devTxBytes), Long.valueOf(devRxPkts), Long.valueOf(devTxPkts), Long.valueOf(xtRxBytes), Long.valueOf(xtTxBytes), Long.valueOf(xtRxPkts), Long.valueOf(xtTxPkts), Long.valueOf(uidRxBytes), Long.valueOf(uidTxBytes), Long.valueOf(uidRxPkts), Long.valueOf(uidTxPkts), Long.valueOf(trustedTime));
    }

    public static void writeLockdownVpnConnecting(int egressNet) {
        EventLog.writeEvent((int) LOCKDOWN_VPN_CONNECTING, egressNet);
    }

    public static void writeLockdownVpnConnected(int egressNet) {
        EventLog.writeEvent((int) LOCKDOWN_VPN_CONNECTED, egressNet);
    }

    public static void writeLockdownVpnError(int egressNet) {
        EventLog.writeEvent((int) LOCKDOWN_VPN_ERROR, egressNet);
    }

    public static void writeConfigInstallFailed(String dir) {
        EventLog.writeEvent((int) CONFIG_INSTALL_FAILED, dir);
    }

    public static void writeIfwIntentMatched(int intentType, String componentName, int callerUid, int callerPkgCount, String callerPkgs, String action, String mimeType, String uri, int flags) {
        EventLog.writeEvent((int) IFW_INTENT_MATCHED, Integer.valueOf(intentType), componentName, Integer.valueOf(callerUid), Integer.valueOf(callerPkgCount), callerPkgs, action, mimeType, uri, Integer.valueOf(flags));
    }

    public static void writeIdleMaintenanceWindowStart(long time, long lastuseractivity, int batterylevel, int batterycharging) {
        EventLog.writeEvent((int) IDLE_MAINTENANCE_WINDOW_START, Long.valueOf(time), Long.valueOf(lastuseractivity), Integer.valueOf(batterylevel), Integer.valueOf(batterycharging));
    }

    public static void writeIdleMaintenanceWindowFinish(long time, long lastuseractivity, int batterylevel, int batterycharging) {
        EventLog.writeEvent((int) IDLE_MAINTENANCE_WINDOW_FINISH, Long.valueOf(time), Long.valueOf(lastuseractivity), Integer.valueOf(batterylevel), Integer.valueOf(batterycharging));
    }

    public static void writeFstrimStart(long time) {
        EventLog.writeEvent((int) FSTRIM_START, time);
    }

    public static void writeFstrimFinish(long time) {
        EventLog.writeEvent((int) FSTRIM_FINISH, time);
    }

    public static void writeJobDeferredExecution(long time) {
        EventLog.writeEvent((int) JOB_DEFERRED_EXECUTION, time);
    }

    public static void writeVolumeChanged(int stream, int prevLevel, int level, int maxLevel, String caller) {
        EventLog.writeEvent((int) VOLUME_CHANGED, Integer.valueOf(stream), Integer.valueOf(prevLevel), Integer.valueOf(level), Integer.valueOf(maxLevel), caller);
    }

    public static void writeStreamDevicesChanged(int stream, int prevDevices, int devices) {
        EventLog.writeEvent((int) STREAM_DEVICES_CHANGED, Integer.valueOf(stream), Integer.valueOf(prevDevices), Integer.valueOf(devices));
    }

    public static void writeCameraGestureTriggered(long gestureOnTime, long sensor1OnTime, long sensor2OnTime, int eventExtra) {
        EventLog.writeEvent((int) CAMERA_GESTURE_TRIGGERED, Long.valueOf(gestureOnTime), Long.valueOf(sensor1OnTime), Long.valueOf(sensor2OnTime), Integer.valueOf(eventExtra));
    }

    public static void writeTimezoneTriggerCheck(String token) {
        EventLog.writeEvent((int) TIMEZONE_TRIGGER_CHECK, token);
    }

    public static void writeTimezoneRequestInstall(String token) {
        EventLog.writeEvent((int) TIMEZONE_REQUEST_INSTALL, token);
    }

    public static void writeTimezoneInstallStarted(String token) {
        EventLog.writeEvent((int) TIMEZONE_INSTALL_STARTED, token);
    }

    public static void writeTimezoneInstallComplete(String token, int result) {
        EventLog.writeEvent((int) TIMEZONE_INSTALL_COMPLETE, token, Integer.valueOf(result));
    }

    public static void writeTimezoneRequestUninstall(String token) {
        EventLog.writeEvent((int) TIMEZONE_REQUEST_UNINSTALL, token);
    }

    public static void writeTimezoneUninstallStarted(String token) {
        EventLog.writeEvent((int) TIMEZONE_UNINSTALL_STARTED, token);
    }

    public static void writeTimezoneUninstallComplete(String token, int result) {
        EventLog.writeEvent((int) TIMEZONE_UNINSTALL_COMPLETE, token, Integer.valueOf(result));
    }

    public static void writeTimezoneRequestNothing(String token) {
        EventLog.writeEvent((int) TIMEZONE_REQUEST_NOTHING, token);
    }

    public static void writeTimezoneNothingComplete(String token) {
        EventLog.writeEvent((int) TIMEZONE_NOTHING_COMPLETE, token);
    }

    public static void writeRequestLocationUpdate(String name, String idHash, int uid, String pkg) {
        EventLog.writeEvent((int) REQUEST_LOCATION_UPDATE, name, idHash, Integer.valueOf(uid), pkg);
    }

    public static void writeRequestLocationRemove(String idHash, int uid, int pid, String pkg) {
        EventLog.writeEvent((int) REQUEST_LOCATION_REMOVE, idHash, Integer.valueOf(uid), Integer.valueOf(pid), pkg);
    }

    public static void writeStartNavigating(int start) {
        EventLog.writeEvent((int) START_NAVIGATING, start);
    }
}