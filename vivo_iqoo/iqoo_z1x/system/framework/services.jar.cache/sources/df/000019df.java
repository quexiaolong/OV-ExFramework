package com.android.server.timedetector;

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.timedetector.TimeDetectorStrategy;
import java.util.Objects;

/* loaded from: classes2.dex */
public final class TimeDetectorStrategyCallbackImpl implements TimeDetectorStrategy.Callback {
    private static final int SYSTEM_CLOCK_UPDATE_THRESHOLD_MILLIS_DEFAULT = 2000;
    private static final String TAG = "timedetector.TimeDetectorStrategyCallbackImpl";
    private final AlarmManager mAlarmManager;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final int mSystemClockUpdateThresholdMillis;
    private final PowerManager.WakeLock mWakeLock;

    public TimeDetectorStrategyCallbackImpl(Context context) {
        Objects.requireNonNull(context);
        this.mContext = context;
        ContentResolver contentResolver = context.getContentResolver();
        Objects.requireNonNull(contentResolver);
        this.mContentResolver = contentResolver;
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        PowerManager.WakeLock newWakeLock = powerManager.newWakeLock(1, TAG);
        Objects.requireNonNull(newWakeLock);
        this.mWakeLock = newWakeLock;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        Objects.requireNonNull(alarmManager);
        this.mAlarmManager = alarmManager;
        this.mSystemClockUpdateThresholdMillis = SystemProperties.getInt("ro.sys.time_detector_update_diff", (int) SYSTEM_CLOCK_UPDATE_THRESHOLD_MILLIS_DEFAULT);
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy.Callback
    public int systemClockUpdateThresholdMillis() {
        return this.mSystemClockUpdateThresholdMillis;
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy.Callback
    public boolean isAutoTimeDetectionEnabled() {
        try {
            return Settings.Global.getInt(this.mContentResolver, "auto_time") != 0;
        } catch (Settings.SettingNotFoundException e) {
            return true;
        }
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy.Callback
    public void acquireWakeLock() {
        if (this.mWakeLock.isHeld()) {
            Slog.wtf(TAG, "WakeLock " + this.mWakeLock + " already held");
        }
        this.mWakeLock.acquire();
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy.Callback
    public long elapsedRealtimeMillis() {
        return SystemClock.elapsedRealtime();
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy.Callback
    public long systemClockMillis() {
        return System.currentTimeMillis();
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy.Callback
    public void setSystemClock(long newTimeMillis) {
        checkWakeLockHeld();
        this.mAlarmManager.setTime(newTimeMillis);
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy.Callback
    public void releaseWakeLock() {
        checkWakeLockHeld();
        this.mWakeLock.release();
    }

    private void checkWakeLockHeld() {
        if (!this.mWakeLock.isHeld()) {
            Slog.wtf(TAG, "WakeLock " + this.mWakeLock + " not held");
        }
    }
}