package com.vivo.services.proxcali;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import com.vivo.sensor.autobrightness.config.AblConfig;
import com.vivo.services.proxy.ProxyConfigs;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class SensorDump {
    private static final String CMD_DISABLE_SENSOR = "--disable";
    private static final String CMD_DUMP_SENSOR_LIST = "--sensorlist";
    private static final String CMD_ENABLE_SENSOR = "--enable";
    private static final String CMD_GET_SENSOR_EVENT = "--getevent";
    public static final String ERROR = "Error_";
    private static final String ERROR_DISABLE = "Error_Disable";
    private static final String ERROR_ENABLE = "Error_Enable";
    private static final String ERROR_GETEVENT = "Error_Getevent";
    private static final String ERROR_SENSOR_LIST = "Error_SensorList";
    private static final String ERROR_WRONG_ARG = "Error_Arg";
    private static final int MSG_DISABLE_SENSOR = 2;
    private static final int MSG_ENABLE_SENSOR = 1;
    private static final String OK = "OK";
    private static final String TAG = "SensorDump";
    private Handler mHandler;
    private SensorManager mManager;
    private HandlerThread mThread;
    private HashMap<Integer, SensorMonitor> mMap = new HashMap<>();
    private Object mLockMap = new Object();

    public SensorDump(Context context) {
        this.mManager = null;
        this.mManager = (SensorManager) context.getSystemService("sensor");
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mThread = handlerThread;
        handlerThread.start();
        this.mHandler = new MyHandler(this.mThread.getLooper());
    }

    public void handleCommand(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args == null || args.length < 1) {
            pw.println(ERROR_WRONG_ARG);
        } else if (CMD_DUMP_SENSOR_LIST.equals(args[0])) {
            handleSensorList(pw, args);
        } else if (CMD_ENABLE_SENSOR.equals(args[0])) {
            handleEnableSensor(pw, args);
        } else if (CMD_DISABLE_SENSOR.equals(args[0])) {
            handleDisableSensor(pw, args);
        } else if (CMD_GET_SENSOR_EVENT.equals(args[0])) {
            handleGetEvent(pw, args);
        } else {
            pw.println(ERROR_WRONG_ARG);
        }
    }

    private JSONObject sensorToJsonObj(Sensor sensor) {
        JSONObject obj = new JSONObject();
        if (sensor == null) {
            return null;
        }
        try {
            obj.put("t", sensor.getType());
            obj.put("ts", sensor.getStringType());
            obj.put("r", sensor.getResolution());
            obj.put("m", sensor.getMaximumRange());
            obj.put("v", sensor.getVendor());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    private void handleSensorList(PrintWriter pw, String[] args) {
        List<Sensor> list = this.mManager.getSensorList(-1);
        if (list == null) {
            pw.println(ERROR_SENSOR_LIST);
            return;
        }
        JSONArray arr = new JSONArray();
        for (Sensor sensor : list) {
            if (sensor != null) {
                arr.put(sensorToJsonObj(sensor));
            }
        }
        pw.println(arr.toString());
    }

    private void handleEnableSensor(PrintWriter pw, String[] args) {
        if (args.length < 5) {
            pw.println(ERROR_ENABLE);
            return;
        }
        int type = -1;
        int delay = -1;
        try {
            if (args[1].equals("-t")) {
                type = Integer.valueOf(args[2]).intValue();
            } else if (args[3].equals("-t")) {
                type = Integer.valueOf(args[4]).intValue();
            }
            if (!args[1].equals("-d")) {
                if (args[3].equals("-d")) {
                    delay = Integer.valueOf(args[4]).intValue();
                }
            } else {
                delay = Integer.valueOf(args[2]).intValue();
            }
        } catch (NumberFormatException e) {
            pw.println(ERROR_ENABLE);
        }
        if (type != -1 && delay != -1) {
            Message msg = this.mHandler.obtainMessage(1);
            msg.arg1 = type;
            msg.arg2 = delay;
            this.mHandler.sendMessage(msg);
            pw.println(OK);
            return;
        }
        pw.println(ERROR_ENABLE);
    }

    private void handleDisableSensor(PrintWriter pw, String[] args) {
        int type;
        if (args.length < 2) {
            pw.println(ERROR_DISABLE);
            return;
        }
        try {
            if (args[1].equals("-t") && args.length == 3) {
                type = Integer.valueOf(args[2]).intValue();
            } else if (args[1].equals("-a")) {
                type = -1;
            } else if (args[1].equals("-l")) {
                String ret = "[";
                synchronized (this.mLockMap) {
                    for (Map.Entry<Integer, SensorMonitor> entry : this.mMap.entrySet()) {
                        if (entry.getValue() != null) {
                            ret = ret + entry.getKey() + ",";
                        }
                    }
                }
                pw.println((ret + "]").replace(",]", "]"));
                return;
            } else {
                pw.println(ERROR_DISABLE);
                return;
            }
            if (type == -1) {
                synchronized (this.mLockMap) {
                    for (Map.Entry<Integer, SensorMonitor> entry2 : this.mMap.entrySet()) {
                        if (entry2.getValue() != null) {
                            Message msg = this.mHandler.obtainMessage(2);
                            msg.arg1 = entry2.getKey().intValue();
                            this.mHandler.sendMessage(msg);
                        }
                    }
                }
                this.mHandler.removeMessages(1);
                pw.println(OK);
                return;
            }
            Message msg2 = this.mHandler.obtainMessage(2);
            msg2.arg1 = type;
            this.mHandler.sendMessage(msg2);
            pw.println(OK);
        } catch (NumberFormatException e) {
            pw.println(ERROR_DISABLE);
        }
    }

    private void handleGetEvent(PrintWriter pw, String[] args) {
        if (args.length < 3) {
            pw.println(ERROR_GETEVENT);
            return;
        }
        int type = -1;
        try {
            if (args[1].equals("-t")) {
                type = Integer.valueOf(args[2]).intValue();
            }
        } catch (NumberFormatException e) {
            pw.println(ERROR_GETEVENT);
        }
        if (type == -1) {
            pw.println(ERROR_GETEVENT);
            return;
        }
        synchronized (this.mLockMap) {
            SensorMonitor monitor = this.mMap.get(Integer.valueOf(type));
            if (monitor == null) {
                pw.println(ERROR_GETEVENT);
            } else {
                pw.println(monitor.getEvent());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SensorMonitor {
        public int mDelay;
        public SensorEvent mEvent;
        public Sensor mSensor;
        public int mType;
        public long timeStamp = -1;
        public boolean mRegistered = false;
        public Object mLock = new Object();
        private SensorEventListener mListener = new SensorEventListener() { // from class: com.vivo.services.proxcali.SensorDump.SensorMonitor.1
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == 5) {
                    event.values[0] = AblConfig.getRectifiedLux(event.values[0], event.sensor.getName());
                }
                synchronized (SensorMonitor.this.mLock) {
                    SensorMonitor.this.mEvent = event;
                    SensorMonitor.this.timeStamp = SystemClock.currentTimeMicro();
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        public SensorMonitor(int type, int delay) {
            this.mType = -1;
            this.mSensor = null;
            this.mDelay = 3;
            this.mType = type;
            this.mDelay = delay;
            this.mSensor = SensorDump.this.mManager.getDefaultSensor(this.mType);
        }

        public boolean enableSensor(boolean enable) {
            if (enable != this.mRegistered) {
                if (enable) {
                    this.mRegistered = SensorDump.this.mManager.registerListener(this.mListener, this.mSensor, this.mDelay);
                } else {
                    SensorDump.this.mManager.unregisterListener(this.mListener);
                    this.mRegistered = false;
                }
            }
            return this.mRegistered;
        }

        public String getEvent() {
            float[] fArr;
            synchronized (this.mLock) {
                if (this.mEvent == null) {
                    return "{}";
                }
                JSONObject obj = new JSONObject();
                JSONArray arr = new JSONArray();
                try {
                    obj.put("type", this.mEvent.sensor.getType());
                    obj.put("time", this.timeStamp);
                    boolean z = false;
                    for (float v : this.mEvent.values) {
                        arr.put(v);
                    }
                    obj.put(ProxyConfigs.CONFIG_VALUES_KEY, arr);
                    if (this.mEvent.sensor.getType() == 8) {
                        if (this.mEvent.values[0] >= 0.0f && this.mEvent.values[0] < this.mEvent.sensor.getMaximumRange()) {
                            z = true;
                        }
                        boolean positive = z;
                        obj.put("postive", positive);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return obj.toString();
            }
        }
    }

    /* loaded from: classes.dex */
    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                int type = msg.arg1;
                int delay = msg.arg2;
                synchronized (SensorDump.this.mLockMap) {
                    SensorMonitor monitor = (SensorMonitor) SensorDump.this.mMap.get(Integer.valueOf(type));
                    if (monitor == null) {
                        monitor = new SensorMonitor(type, delay);
                        SensorDump.this.mMap.put(Integer.valueOf(type), monitor);
                    }
                    monitor.enableSensor(true);
                }
            } else if (i == 2) {
                int type2 = msg.arg1;
                int i2 = msg.arg2;
                synchronized (SensorDump.this.mLockMap) {
                    SensorMonitor monitor2 = (SensorMonitor) SensorDump.this.mMap.get(Integer.valueOf(type2));
                    if (monitor2 == null) {
                        return;
                    }
                    monitor2.enableSensor(false);
                    SensorDump.this.mMap.remove(Integer.valueOf(type2));
                }
            }
        }
    }
}