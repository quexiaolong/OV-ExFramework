package com.android.server.display.whitebalance;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Slog;
import com.android.server.display.utils.History;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Objects;

/* loaded from: classes.dex */
abstract class AmbientSensor {
    private static final int HISTORY_SIZE = 50;
    private boolean mEnabled;
    private int mEventsCount;
    private History mEventsHistory;
    private final Handler mHandler;
    private SensorEventListener mListener = new SensorEventListener() { // from class: com.android.server.display.whitebalance.AmbientSensor.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            float value = event.values[0];
            AmbientSensor.this.handleNewEvent(value);
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    protected boolean mLoggingEnabled;
    private int mRate;
    protected Sensor mSensor;
    protected final SensorManager mSensorManager;
    protected String mTag;

    protected abstract void update(float f);

    AmbientSensor(String tag, Handler handler, SensorManager sensorManager, int rate) {
        validateArguments(handler, sensorManager, rate);
        this.mTag = tag;
        this.mLoggingEnabled = false;
        this.mHandler = handler;
        this.mSensorManager = sensorManager;
        this.mEnabled = false;
        this.mRate = rate;
        this.mEventsCount = 0;
        this.mEventsHistory = new History(50);
    }

    public boolean setEnabled(boolean enabled) {
        if (enabled) {
            return enable();
        }
        return disable();
    }

    public boolean setLoggingEnabled(boolean loggingEnabled) {
        if (this.mLoggingEnabled == loggingEnabled) {
            return false;
        }
        this.mLoggingEnabled = loggingEnabled;
        return true;
    }

    public void dump(PrintWriter writer) {
        writer.println("  " + this.mTag);
        writer.println("    mLoggingEnabled=" + this.mLoggingEnabled);
        writer.println("    mHandler=" + this.mHandler);
        writer.println("    mSensorManager=" + this.mSensorManager);
        writer.println("    mSensor=" + this.mSensor);
        writer.println("    mEnabled=" + this.mEnabled);
        writer.println("    mRate=" + this.mRate);
        writer.println("    mEventsCount=" + this.mEventsCount);
        writer.println("    mEventsHistory=" + this.mEventsHistory);
    }

    private static void validateArguments(Handler handler, SensorManager sensorManager, int rate) {
        Objects.requireNonNull(handler, "handler cannot be null");
        Objects.requireNonNull(sensorManager, "sensorManager cannot be null");
        if (rate <= 0) {
            throw new IllegalArgumentException("rate must be positive");
        }
    }

    private boolean enable() {
        if (this.mEnabled) {
            return false;
        }
        if (this.mLoggingEnabled) {
            Slog.d(this.mTag, "enabling");
        }
        this.mEnabled = true;
        startListening();
        return true;
    }

    private boolean disable() {
        if (this.mEnabled) {
            if (this.mLoggingEnabled) {
                Slog.d(this.mTag, "disabling");
            }
            this.mEnabled = false;
            this.mEventsCount = 0;
            stopListening();
            return true;
        }
        return false;
    }

    private void startListening() {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null) {
            return;
        }
        sensorManager.registerListener(this.mListener, this.mSensor, this.mRate * 1000, this.mHandler);
    }

    private void stopListening() {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null) {
            return;
        }
        sensorManager.unregisterListener(this.mListener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNewEvent(float value) {
        if (!this.mEnabled) {
            return;
        }
        if (this.mLoggingEnabled) {
            Slog.d(this.mTag, "handle new event: " + value);
        }
        this.mEventsCount++;
        this.mEventsHistory.add(value);
        update(value);
    }

    /* loaded from: classes.dex */
    static class AmbientBrightnessSensor extends AmbientSensor {
        private static final String TAG = "AmbientBrightnessSensor";
        private Callbacks mCallbacks;

        /* loaded from: classes.dex */
        interface Callbacks {
            void onAmbientBrightnessChanged(float f);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public AmbientBrightnessSensor(Handler handler, SensorManager sensorManager, int rate) {
            super(TAG, handler, sensorManager, rate);
            this.mSensor = this.mSensorManager.getDefaultSensor(5);
            if (this.mSensor == null) {
                throw new IllegalStateException("cannot find light sensor");
            }
            this.mCallbacks = null;
        }

        public boolean setCallbacks(Callbacks callbacks) {
            if (this.mCallbacks == callbacks) {
                return false;
            }
            this.mCallbacks = callbacks;
            return true;
        }

        @Override // com.android.server.display.whitebalance.AmbientSensor
        public void dump(PrintWriter writer) {
            super.dump(writer);
            writer.println("    mCallbacks=" + this.mCallbacks);
        }

        @Override // com.android.server.display.whitebalance.AmbientSensor
        protected void update(float value) {
            Callbacks callbacks = this.mCallbacks;
            if (callbacks != null) {
                callbacks.onAmbientBrightnessChanged(value);
            }
        }
    }

    /* loaded from: classes.dex */
    static class AmbientColorTemperatureSensor extends AmbientSensor {
        private static final String TAG = "AmbientColorTemperatureSensor";
        private Callbacks mCallbacks;

        /* loaded from: classes.dex */
        interface Callbacks {
            void onAmbientColorTemperatureChanged(float f);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public AmbientColorTemperatureSensor(Handler handler, SensorManager sensorManager, String name, int rate) {
            super(TAG, handler, sensorManager, rate);
            this.mSensor = null;
            Iterator<Sensor> it = this.mSensorManager.getSensorList(-1).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Sensor sensor = it.next();
                if (sensor.getStringType().equals(name)) {
                    this.mSensor = sensor;
                    break;
                }
            }
            if (this.mSensor == null) {
                throw new IllegalStateException("cannot find sensor " + name);
            }
            this.mCallbacks = null;
        }

        public boolean setCallbacks(Callbacks callbacks) {
            if (this.mCallbacks == callbacks) {
                return false;
            }
            this.mCallbacks = callbacks;
            return true;
        }

        @Override // com.android.server.display.whitebalance.AmbientSensor
        public void dump(PrintWriter writer) {
            super.dump(writer);
            writer.println("    mCallbacks=" + this.mCallbacks);
        }

        @Override // com.android.server.display.whitebalance.AmbientSensor
        protected void update(float value) {
            Callbacks callbacks = this.mCallbacks;
            if (callbacks != null) {
                callbacks.onAmbientColorTemperatureChanged(value);
            }
        }
    }
}