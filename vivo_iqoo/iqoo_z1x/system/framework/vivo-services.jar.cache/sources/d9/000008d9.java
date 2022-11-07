package com.vivo.services.vivolight;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import com.vivo.common.utils.VLog;
import java.text.SimpleDateFormat;
import java.util.Date;

/* loaded from: classes.dex */
public class SettingMonitorUtil {
    public static final String ACTION_VIVO_LIGHT_LIMIT_TIME_CHANGE_ACTION = "android.vivo.action.VIVO_LIGHT_LIMIT_TIME_CHANGE";
    public static final String KEY_BEYOND_LIGHT_TIME_LIMIT = "beyond_light_time_limit";
    public static final String KEY_LIGHT_END_TIME = "light_end_time";
    public static final String KEY_LIGHT_START_TIME = "light_start_time";
    public static final String KEY_USER_DEFINED_TIME = "user_defined_time";
    ContentObserver mChangeContentObserver;
    private final Context mContext;
    private VivoLightManagerService mService;

    public SettingMonitorUtil(Context context, VivoLightManagerService service) {
        this.mContext = context;
        this.mService = service;
    }

    public void register(Handler handler) {
        UserChangeReceiver userChangeReceiver = new UserChangeReceiver();
        IntentFilter filterChange = new IntentFilter();
        filterChange.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(userChangeReceiver, filterChange);
        if (this.mChangeContentObserver == null) {
            this.mChangeContentObserver = new ContentObserver(handler) { // from class: com.vivo.services.vivolight.SettingMonitorUtil.1
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    VLog.d(VivoLightManagerService.TAG, "Setting change");
                    SettingMonitorUtil.this.resetLimitLight();
                }
            };
        }
        registerObserver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_VIVO_LIGHT_LIMIT_TIME_CHANGE_ACTION);
        SettingMonitorBroadcastReceiver mReceiver = new SettingMonitorBroadcastReceiver();
        this.mContext.registerReceiver(mReceiver, filter, null, handler);
        resetLimitLight();
    }

    public void registerObserver() {
        VLog.d(VivoLightManagerService.TAG, "registerObserver");
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_USER_DEFINED_TIME), false, this.mChangeContentObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_LIGHT_START_TIME), false, this.mChangeContentObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_LIGHT_END_TIME), false, this.mChangeContentObserver, -1);
    }

    public void unRegisterObserver() {
        VLog.d(VivoLightManagerService.TAG, "unRegisterObserver");
        this.mContext.getContentResolver().unregisterContentObserver(this.mChangeContentObserver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetLimitLight() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        int anInt = Settings.System.getIntForUser(contentResolver, KEY_USER_DEFINED_TIME, 0, ActivityManager.getCurrentUser());
        VLog.d(VivoLightManagerService.TAG, "user_defined_time:" + anInt);
        if (anInt == 0) {
            setLimitLight(false);
            return;
        }
        String startTimeStr = Settings.System.getStringForUser(contentResolver, KEY_LIGHT_START_TIME, ActivityManager.getCurrentUser());
        if (startTimeStr == null) {
            startTimeStr = "06:00";
        }
        String endTimeStr = Settings.System.getStringForUser(contentResolver, KEY_LIGHT_END_TIME, ActivityManager.getCurrentUser());
        if (endTimeStr == null) {
            endTimeStr = "23:00";
        }
        int compareTo = startTimeStr.compareTo(endTimeStr);
        String currentTimeStr = parseCurrentTime();
        VLog.d(VivoLightManagerService.TAG, "startTimeStr:" + startTimeStr + ", endTimeStr:" + endTimeStr + ", currentTimeStr:" + currentTimeStr);
        if (compareTo == 0) {
            setLimitLight(false);
        } else if (compareTo > 0) {
            boolean isBetween = isBetween(endTimeStr, currentTimeStr, startTimeStr);
            setLimitLight(isBetween);
        } else {
            boolean isBetween2 = isBetween(startTimeStr, currentTimeStr, endTimeStr);
            setLimitLight(!isBetween2);
        }
    }

    private boolean isBetween(String start, String mid, String end) {
        return mid.compareTo(start) >= 0 && mid.compareTo(end) < 0;
    }

    private void setLimitLight(boolean limit) {
        boolean limitLightPre = this.mService.isLimitLight();
        if (limitLightPre != limit) {
            this.mService.setLimitLight(limit);
            this.mService.notifyUpdateLight();
        }
    }

    private void unRegister() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SettingMonitorBroadcastReceiver extends BroadcastReceiver {
        private SettingMonitorBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            VLog.d(VivoLightManagerService.TAG, "vivo_light_limit_time_change");
            SettingMonitorUtil.this.resetLimitLight();
        }
    }

    public static String parseCurrentTime() {
        SimpleDateFormat dateFm = new SimpleDateFormat("HH:mm");
        return dateFm.format(new Date());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class UserChangeReceiver extends BroadcastReceiver {
        private UserChangeReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VLog.d(VivoLightManagerService.TAG, "userChange receive = " + action);
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                SettingMonitorUtil.this.resetLimitLight();
            }
        }
    }
}