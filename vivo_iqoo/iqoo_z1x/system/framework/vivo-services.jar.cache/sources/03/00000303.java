package com.android.server.locksettings;

import android.app.AlarmManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.SparseLongArray;
import com.android.server.am.EmergencyBroadcastManager;
import com.android.server.policy.VivoPolicyConstant;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLockSettingsStrongAuthImpl implements IVivoLockSettingsStrongAuth {
    public static final long BBK_NON_STRONG_BIOMETRIC_TIMEOUT_DEFAULT = -999;
    public static final long BBK_NON_STRONG_BIOMETRIC_TIMEOUT_MIN = 10000;
    public static final long BBK_STRONGAUTH_TIMEOUT_DEFAULT = -999;
    public static final long BBK_STRONGAUTH_TIMEOUT_MIN = 10000;
    private static final String CONVENIENCE_LEVEL_STRONGAUTH_TIMEOUT_ALARM_TAG = "LockSettingsStrongAuth.conLevelStrongauthtimeoutForUser";
    public static final long DEFAULT_CONVENIENCE_LEVEL_STRONGAUTH_TIMEOUT_MS = 86400000;
    private static final String TAG = "VivoLockSettingsStrongAuthImpl";
    private AlarmManager mAlarmManager;
    private Context mContext;
    private final ArrayMap<Integer, ConvenienceLevelStrongauthTimeoutAlarmListener> mConvenienceLevelStrongauthTimeoutAlarmListenerForUser = new ArrayMap<>();
    private LockSettingsStrongAuth mStrongAuth;
    public static long sBBKStrongauthTimeout = -999;
    public static SparseLongArray sBBKStrongauthTimeoutArray = new SparseLongArray();
    public static long sBBKNonStrongBiometricTimeout = -999;
    public static SparseLongArray sBBKNonStrongNiometricTimeoutArray = new SparseLongArray();
    public static SparseLongArray sBBKConLevelStrongauthTimeoutArray = new SparseLongArray();

    public VivoLockSettingsStrongAuthImpl(LockSettingsStrongAuth strongauth, Context context, AlarmManager alarm) {
        this.mStrongAuth = strongauth;
        this.mContext = context;
        this.mAlarmManager = alarm;
        registerLogBroadcast(context);
    }

    public void notifyStrongAuthTrackers(int strongAuthReason, int userId) {
        VSlog.w(TAG, "notifyStrongAuthTrackers strongAuthReason == " + strongAuthReason + " ,userId ==" + userId);
    }

    public void requireStrongAuth(int strongAuthReason, int userId) {
        VSlog.d(TAG, "requireStrongAuth strongAuthReason == " + strongAuthReason + " ,userId ==" + userId);
    }

    public long getBBKStrongAuthTimeout(DevicePolicyManager dpm, int userId) {
        long tempValue = sBBKStrongauthTimeoutArray.get(userId, -999L);
        if (tempValue != -999) {
            return tempValue;
        }
        return dpm.getRequiredStrongAuthTimeout(null, userId);
    }

    public long getBBKNonStrongBiometricTimeout(long originTimeout, int userId) {
        long tempValue = sBBKNonStrongNiometricTimeoutArray.get(userId, -999L);
        if (tempValue != -999) {
            return tempValue;
        }
        return originTimeout;
    }

    /* loaded from: classes.dex */
    private class ConvenienceLevelStrongauthTimeoutAlarmListener implements AlarmManager.OnAlarmListener {
        private final int mUserId;

        public ConvenienceLevelStrongauthTimeoutAlarmListener(int userId) {
            this.mUserId = userId;
        }

        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            VivoLockSettingsStrongAuthImpl.this.mStrongAuth.requireStrongAuth((int) EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP, this.mUserId);
        }
    }

    private long getBBKConvenienceLevelStrongauthTimeout(int userId) {
        long tempValue = sBBKConLevelStrongauthTimeoutArray.get(userId, -999L);
        if (tempValue != -999) {
            return tempValue;
        }
        return 86400000L;
    }

    public void handleScheduleConvenienceLevelStrongAuthTimeout(int userId, Handler handler) {
        ConvenienceLevelStrongauthTimeoutAlarmListener alarm;
        long currentTimeoutValue = getBBKConvenienceLevelStrongauthTimeout(userId);
        long when = SystemClock.elapsedRealtime() + currentTimeoutValue;
        VSlog.w(TAG, "RequiredConvenienceLevelStrongauthTimeout == " + currentTimeoutValue + ", userId == " + userId);
        ConvenienceLevelStrongauthTimeoutAlarmListener alarm2 = this.mConvenienceLevelStrongauthTimeoutAlarmListenerForUser.get(Integer.valueOf(userId));
        if (alarm2 != null) {
            this.mAlarmManager.cancel(alarm2);
            alarm = alarm2;
        } else {
            ConvenienceLevelStrongauthTimeoutAlarmListener alarm3 = new ConvenienceLevelStrongauthTimeoutAlarmListener(userId);
            this.mConvenienceLevelStrongauthTimeoutAlarmListenerForUser.put(Integer.valueOf(userId), alarm3);
            alarm = alarm3;
        }
        this.mAlarmManager.set(2, when, CONVENIENCE_LEVEL_STRONGAUTH_TIMEOUT_ALARM_TAG, alarm, handler);
    }

    private void registerLogBroadcast(Context context) {
        if (context == null) {
            VSlog.i(TAG, "registerLogBroadcast failed, context is null");
            return;
        }
        LockSettingsStrongAuth.DEBUG = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
        IntentFilter bbklogFilter = new IntentFilter();
        bbklogFilter.addAction(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED);
        context.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.locksettings.VivoLockSettingsStrongAuthImpl.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                boolean status = "on".equals(intent.getStringExtra("adblog_status"));
                VSlog.i(VivoLockSettingsStrongAuthImpl.TAG, "bbklog status: " + status);
                LockSettingsStrongAuth.DEBUG = status;
            }
        }, bbklogFilter);
    }
}