package com.android.server.power;

import android.content.ContentValues;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.ArrayMap;
import com.android.internal.os.BackgroundThread;
import com.android.server.power.PowerManagerService;
import com.android.server.wm.VCD_FF_1;
import com.android.server.wm.WindowState;
import com.vivo.vcodetransbase.EventTransfer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import vivo.app.epm.ExceptionPolicyManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class PowerDataReport {
    private static final int EVENT_TYPE = 44;
    private static final String KEYGUARD_TIMEOUT = "KeyguardDrawnTimeout";
    private static final int LEVEL_MAY_CAUSE_COMPLAINT = 2;
    private static final int LEVEL_MAY_CAUSE_DISCOMFORT = 3;
    private static final int MSG_DISPLAY_LOCK_EXCEPTION = 1;
    private static final int MSG_DOUBLE_TAP_SCREEN_ON_TIME = 6;
    private static final int MSG_KEYGUARD_DRAWN_TIMEOUT = 3;
    private static final int MSG_LONG_WAKE_LOCK_EXCEPTION = 2;
    private static final int MSG_POWER_KEY_SCREEN_ON_TIME = 5;
    private static final int MSG_WINDOW_DRAWN_TIMEOUT = 4;
    private static final String SEPARATOR = "_";
    private static final int SUB_EVENT_BRIGHT_WAKE_LOCK_EXCEPTION = 3;
    private static final int SUB_EVENT_DISPLAY_LOCK_EXCEPTION = 1;
    private static final int SUB_EVENT_KEYGUARD_DRAWN_TIMEOUT = 4;
    private static final int SUB_EVENT_PARTIAL_WAKE_LOCK_EXCEPTION = 2;
    private static final int SUB_EVENT_WINDOW_DRAWN_TIMEOUT = 5;
    private static final String TAG = "PowerDataReport";
    private static final String WINDOW_TIMEOUT = "WindowDrawnTimeout";
    private static PowerDataReport sInstance = null;
    private int mKeyguardDrawnTimeoutTimes;
    private Handler mPowerReportHandler;
    private int mWindowDrawnTimeoutTimes;
    private ArrayMap<String, Integer> mDrawnTimeoutWindows = new ArrayMap<>();
    private long mStartTimeOfScrenOn = 0;
    private String mReasonOfScreenOn = null;

    private PowerDataReport() {
        initHandler();
    }

    public static synchronized PowerDataReport getInstance() {
        PowerDataReport powerDataReport;
        synchronized (PowerDataReport.class) {
            if (sInstance == null) {
                sInstance = new PowerDataReport();
            }
            powerDataReport = sInstance;
        }
        return powerDataReport;
    }

    private void initHandler() {
        this.mPowerReportHandler = new Handler(BackgroundThread.getHandler().getLooper()) { // from class: com.android.server.power.PowerDataReport.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        PowerDataReport.this.reportEventToEpm((String) msg.obj, "44_" + msg.arg1, msg.arg2);
                        return;
                    case 5:
                        try {
                            PowerDataReport.this.reportEventToVcode("F632", "F632|10001", "TimeOfScreenOnFromPowerKey", Long.valueOf(((Long) msg.obj).longValue()));
                            return;
                        } catch (Exception ex) {
                            VSlog.e(PowerDataReport.TAG, "failed send to vcode");
                            ex.printStackTrace();
                            return;
                        }
                    case 6:
                        try {
                            PowerDataReport.this.reportEventToVcode("F632", "F632|10002", "TimeOfScreenOnFromDoubleTap", Long.valueOf(((Long) msg.obj).longValue()));
                            return;
                        } catch (Exception ex2) {
                            VSlog.e(PowerDataReport.TAG, "failed send to vcode");
                            ex2.printStackTrace();
                            return;
                        }
                    default:
                        return;
                }
            }
        };
    }

    public void reportDisplayLockException(boolean proximityPositive, int count) {
        Message msg = this.mPowerReportHandler.obtainMessage(1);
        msg.obj = "proximityPositive_" + Boolean.toString(proximityPositive) + SEPARATOR + Integer.toString(count);
        msg.arg1 = 1;
        msg.arg2 = 3;
        this.mPowerReportHandler.sendMessage(msg);
    }

    public void reportLongWakeLockException(PowerManagerService.WakeLock wakeLock, long acquiredTime, boolean isPartial) {
        Message msg = this.mPowerReportHandler.obtainMessage(2);
        msg.obj = wakeLock.mPackageName + SEPARATOR + wakeLock.mTag + SEPARATOR + Long.toString(acquiredTime);
        msg.arg1 = isPartial ? 2 : 3;
        msg.arg2 = 3;
        this.mPowerReportHandler.sendMessage(msg);
    }

    public void reportKeyguardDrawnTimeout() {
        this.mKeyguardDrawnTimeoutTimes++;
        Message msg = this.mPowerReportHandler.obtainMessage(3);
        msg.obj = "KeyguardDrawnTimeout_" + this.mKeyguardDrawnTimeoutTimes;
        msg.arg1 = 4;
        msg.arg2 = 2;
        this.mPowerReportHandler.sendMessage(msg);
    }

    public void reportWindowDrawnTimeout(ArrayList<WindowState> waitingForDrawn) {
        ArrayList<String> timeoutWindows = new ArrayList<>();
        Iterator<WindowState> it = waitingForDrawn.iterator();
        while (it.hasNext()) {
            WindowState windowState = it.next();
            String windowPackageName = windowState.getAttrs().packageName;
            if (!this.mDrawnTimeoutWindows.containsKey(windowPackageName)) {
                this.mDrawnTimeoutWindows.put(windowPackageName, 1);
            } else {
                Integer integer = this.mDrawnTimeoutWindows.get(windowPackageName);
                if (integer != null) {
                    int times = integer.intValue();
                    this.mDrawnTimeoutWindows.put(windowPackageName, Integer.valueOf(times + 1));
                } else {
                    VSlog.d(TAG, "ERROR with " + windowPackageName + ", can't find index!");
                    this.mDrawnTimeoutWindows.remove(windowPackageName);
                }
            }
            timeoutWindows.add(windowState.toString() + SEPARATOR + this.mDrawnTimeoutWindows.get(windowPackageName));
        }
        Message msg = this.mPowerReportHandler.obtainMessage(4);
        msg.obj = "WindowDrawnTimeout_" + timeoutWindows;
        msg.arg1 = 5;
        msg.arg2 = 2;
        this.mPowerReportHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportEventToEpm(String expsrc, String subtype, int level) {
        VSlog.e(TAG, "power exception report:" + expsrc + " subtype:" + subtype);
        ContentValues cv = new ContentValues();
        cv.put("expsrc", expsrc);
        cv.put("subtype", subtype);
        cv.put("level", Integer.toString(level));
        cv.put("trouble", "1");
        ExceptionPolicyManager.getInstance().recordEvent(44, System.currentTimeMillis(), cv);
    }

    public void sendPowerReport() {
        String str = this.mReasonOfScreenOn;
        if (str == null) {
            return;
        }
        Message msg = null;
        char c = 65535;
        int hashCode = str.hashCode();
        if (hashCode != 923182906) {
            if (hashCode == 1197805042 && str.equals("DoubleTap")) {
                c = 1;
            }
        } else if (str.equals("PowerKey")) {
            c = 0;
        }
        if (c == 0) {
            msg = this.mPowerReportHandler.obtainMessage(5);
            msg.obj = Long.valueOf(SystemClock.uptimeMillis() - this.mStartTimeOfScrenOn);
        } else if (c == 1) {
            msg = this.mPowerReportHandler.obtainMessage(6);
            msg.obj = Long.valueOf(SystemClock.uptimeMillis() - this.mStartTimeOfScrenOn);
        }
        if (msg != null) {
            this.mPowerReportHandler.sendMessage(msg);
        }
    }

    public void reportEventToVcode(String mode_id, String event_id, String reason, Long time) {
        VSlog.d(TAG, "The reason of Waking up is " + reason + ", spend time " + time);
        HashMap<String, String> params = new HashMap<>(3);
        params.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
        params.put(reason, time.toString());
        EventTransfer.getInstance().singleEvent(mode_id, event_id, System.currentTimeMillis(), 0L, params);
    }

    public void clearReasonOfScreenOn() {
        this.mReasonOfScreenOn = null;
    }

    public void setStartTimeOfScrenOn(long startTimeOfScrenOn) {
        this.mStartTimeOfScrenOn = startTimeOfScrenOn;
    }

    public void setReasonOfScreenOn(String reasonOfScreenOn) {
        this.mReasonOfScreenOn = reasonOfScreenOn;
    }

    public String getReasonOfScreenOn() {
        return this.mReasonOfScreenOn;
    }
}