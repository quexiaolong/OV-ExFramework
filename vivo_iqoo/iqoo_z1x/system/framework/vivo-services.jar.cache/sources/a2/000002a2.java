package com.android.server.location;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.ILocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import com.android.server.location.gnss.GnssLocationProvider;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class VivoGpsPowerMonitor {
    private static final boolean DBG = true;
    private static final int ENTRY_POWER_SAVE_DELAY = 60000;
    public static final String GPM = "GPM";
    public static final int GPS_STRONG_SIGNAL = 10;
    public static final int GPS_WEAK_SIGNAL = 11;
    private static final int MSG_ENTRY_POWER_SAVE_MODE = 3;
    private static final int MSG_EXIT_POWER_SAVE_MODE = 4;
    private static final int MSG_GPS_SIGNAL_CHANGED = 2;
    private static final int MSG_SENSOR_STATUS_CHANGED = 1;
    public static final int SENSOR_MOTIONLESS = 1;
    public static final int SENSOR_MOVING = 2;
    private static final int SENSOR_TYPE_AMD = 33171006;
    private static final String TAG = "VivoGpsPowerMonitor";
    public static final boolean vivoGpsPowerMonitorEnabled = true;
    private Sensor mAmdSensor;
    private Context mContext;
    private GnssLocationProvider mGnssLocationProvider;
    private VivoMonitorHandler mHandler;
    private SensorManager mSensorManager;
    private boolean mIsRegistered = false;
    private boolean mGpsDisableByGPM = false;
    private ILocationManager mILocationManager = null;
    private int mSensorStatus = 2;
    private int mGpsSignalStatus = 10;
    SensorEventListener mAmdListener = new SensorEventListener() { // from class: com.android.server.location.VivoGpsPowerMonitor.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            try {
                float result = event.values[0];
                int status = 2;
                if (result == 1.0f) {
                    status = 1;
                }
                VivoGpsPowerMonitor.LogD_GPM(VivoGpsPowerMonitor.TAG, "Sensor lister get event " + VivoGpsPowerMonitor.eventToString(status));
                VivoGpsPowerMonitor.this.mHandler.sendMessage(VivoGpsPowerMonitor.this.mHandler.obtainMessage(1, status, 0));
            } catch (Exception ex) {
                VivoGpsPowerMonitor.LogE_GPM(VivoGpsPowerMonitor.TAG, VLog.getStackTraceString(ex));
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public VivoGpsPowerMonitor(Context context, GnssLocationProvider provider, Looper looper) {
        this.mContext = context;
        this.mGnssLocationProvider = provider;
        this.mHandler = new VivoMonitorHandler(looper);
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mAmdSensor = sensorManager.getDefaultSensor(SENSOR_TYPE_AMD);
    }

    public void startGpsPowerMonitor() {
        try {
            clearUp();
            if (!this.mIsRegistered) {
                LogD_GPM(TAG, "register Sensor listener");
                if (this.mAmdSensor == null) {
                    LogW_GPM(TAG, "AmdSensor is not available.");
                } else {
                    this.mSensorManager.registerListener(this.mAmdListener, this.mAmdSensor, 3);
                    this.mIsRegistered = true;
                }
            }
        } catch (Exception ex) {
            LogE_GPM(TAG, VLog.getStackTraceString(ex));
        }
    }

    public boolean isRegistered() {
        return this.mIsRegistered;
    }

    public void stopGpsPowerMonitor() {
        try {
            if (this.mIsRegistered) {
                LogD_GPM(TAG, "unregister Sensor listener");
                clearUp();
                if (this.mAmdSensor != null) {
                    this.mSensorManager.unregisterListener(this.mAmdListener, this.mAmdSensor);
                }
                this.mIsRegistered = false;
            }
        } catch (Exception ex) {
            LogE_GPM(TAG, VLog.getStackTraceString(ex));
        }
    }

    public void reportSvStatus(int svCount, float[] cn0s) {
        LogD_GPM(TAG, "reportSvStatus");
        if (this.mIsRegistered) {
            int status = isStrongSignal(svCount, cn0s);
            VivoMonitorHandler vivoMonitorHandler = this.mHandler;
            vivoMonitorHandler.sendMessage(vivoMonitorHandler.obtainMessage(2, status, 0));
        }
    }

    private int isStrongSignal(int svCount, float[] cn0s) {
        int ret = 10;
        int count = 0;
        for (int i = 0; i < svCount; i++) {
            try {
                if (cn0s[i] > 8.0d) {
                    count++;
                }
            } catch (Exception ex) {
                LogE_GPM(TAG, VLog.getStackTraceString(ex));
            }
        }
        if (count < 4) {
            ret = 11;
        }
        LogD_GPM(TAG, "isStrongSignal:" + eventToString(ret) + ", count:" + count + ", svCount:" + svCount);
        return ret;
    }

    public static String eventToString(int event) {
        if (event != 1) {
            if (event != 2) {
                if (event != 10) {
                    if (event == 11) {
                        return "GPS_WEAK_SIGNAL";
                    }
                    return "UNKNOWN";
                }
                return "GPS_STRONG_SIGNAL";
            }
            return "SENSOR_MOVING";
        }
        return "SENSOR_MOTIONLESS";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void exitPowerSaveMode() {
        GnssLocationProvider gnssLocationProvider = this.mGnssLocationProvider;
        if (gnssLocationProvider != null) {
            this.mGpsDisableByGPM = false;
            gnssLocationProvider.enableGps(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void entryPowerSaveMode() {
        if ("1".equals(SystemProperties.get("sys.bsptest.gps", "0"))) {
            LogD_GPM(TAG, "In BSP Test, do not entry power save mode");
            return;
        }
        GnssLocationProvider gnssLocationProvider = this.mGnssLocationProvider;
        if (gnssLocationProvider != null) {
            this.mGpsDisableByGPM = true;
            gnssLocationProvider.enableGps(false);
        }
    }

    public boolean resistStartGps() {
        LogD_GPM(TAG, "resistStartGps " + this.mGpsDisableByGPM + " " + this.mIsRegistered);
        return this.mIsRegistered && this.mGpsDisableByGPM;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class VivoMonitorHandler extends Handler {
        public VivoMonitorHandler(Looper looper) {
            super(looper);
        }

        public String msgToString(int msg) {
            if (msg != 1) {
                if (msg != 2) {
                    if (msg != 3) {
                        if (msg == 4) {
                            return "MSG_EXIT_POWER_SAVE_MODE";
                        }
                        return "UNKNOWN";
                    }
                    return "MSG_ENTRY_POWER_SAVE_MODE";
                }
                return "MSG_GPS_SIGNAL_CHANGED";
            }
            return "MSG_SENSOR_STATUS_CHANGED";
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            try {
                if (msg == null) {
                    VivoGpsPowerMonitor.LogE_GPM(VivoGpsPowerMonitor.TAG, "msg error");
                    return;
                }
                VivoGpsPowerMonitor.LogD_GPM(VivoGpsPowerMonitor.TAG, "handleMessage " + msgToString(msg.what));
                int i = msg.what;
                if (i == 1) {
                    int signalStatus = msg.arg1;
                    if (signalStatus != VivoGpsPowerMonitor.this.mSensorStatus) {
                        VivoGpsPowerMonitor.LogD_GPM(VivoGpsPowerMonitor.TAG, "MSG_SENSOR_STATUS_CHANGED new:" + VivoGpsPowerMonitor.eventToString(signalStatus) + ", old:" + VivoGpsPowerMonitor.eventToString(VivoGpsPowerMonitor.this.mSensorStatus) + ", mGpsSignalStatus:" + VivoGpsPowerMonitor.eventToString(VivoGpsPowerMonitor.this.mGpsSignalStatus));
                        VivoGpsPowerMonitor.this.mSensorStatus = signalStatus;
                        VivoGpsPowerMonitor.this.mHandler.removeMessages(3);
                        if (VivoGpsPowerMonitor.this.mSensorStatus == 1 && VivoGpsPowerMonitor.this.mGpsSignalStatus == 11) {
                            VivoGpsPowerMonitor.this.mHandler.sendEmptyMessageDelayed(3, 60000L);
                        }
                    }
                    if (VivoGpsPowerMonitor.this.mSensorStatus == 2) {
                        VivoGpsPowerMonitor.this.mHandler.sendEmptyMessage(4);
                    }
                } else if (i != 2) {
                    if (i != 3) {
                        if (i == 4 && VivoGpsPowerMonitor.this.mGpsDisableByGPM) {
                            VivoGpsPowerMonitor.this.exitPowerSaveMode();
                            return;
                        }
                        return;
                    }
                    VivoGpsPowerMonitor.this.entryPowerSaveMode();
                } else {
                    int signalStatus2 = msg.arg1;
                    if (signalStatus2 != VivoGpsPowerMonitor.this.mGpsSignalStatus) {
                        VivoGpsPowerMonitor.LogD_GPM(VivoGpsPowerMonitor.TAG, "MSG_GPS_SIGNAL_CHANGED new:" + VivoGpsPowerMonitor.eventToString(signalStatus2) + ", old:" + VivoGpsPowerMonitor.eventToString(VivoGpsPowerMonitor.this.mGpsSignalStatus) + ", mSensorStatus:" + VivoGpsPowerMonitor.eventToString(VivoGpsPowerMonitor.this.mSensorStatus));
                        VivoGpsPowerMonitor.this.mGpsSignalStatus = signalStatus2;
                        VivoGpsPowerMonitor.this.mHandler.removeMessages(3);
                        if (VivoGpsPowerMonitor.this.mGpsSignalStatus == 11 && VivoGpsPowerMonitor.this.mSensorStatus == 1) {
                            VivoGpsPowerMonitor.this.mHandler.sendEmptyMessageDelayed(3, 60000L);
                        }
                    }
                    if (VivoGpsPowerMonitor.this.mGpsSignalStatus == 10) {
                        VivoGpsPowerMonitor.this.mHandler.sendEmptyMessage(4);
                    }
                }
            } catch (Exception ex) {
                VivoGpsPowerMonitor.LogE_GPM(VivoGpsPowerMonitor.TAG, VLog.getStackTraceString(ex));
            }
        }
    }

    private void removeAllMessages() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(4);
    }

    private void clearUp() {
        LogD_GPM(TAG, "clearUp");
        try {
            removeAllMessages();
            this.mGpsDisableByGPM = false;
            this.mSensorStatus = 2;
            this.mGpsSignalStatus = 10;
        } catch (Exception ex) {
            LogE_GPM(TAG, VLog.getStackTraceString(ex));
        }
    }

    public static void LogD_GPM(String tag, String info) {
        VLog.d(tag, "GPM " + info);
    }

    public static void LogW_GPM(String tag, String info) {
        VLog.w(tag, "GPM " + info);
    }

    public static void LogE_GPM(String tag, String info) {
        VLog.e(tag, "GPM " + info);
    }
}