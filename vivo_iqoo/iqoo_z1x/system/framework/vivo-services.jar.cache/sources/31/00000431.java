package com.android.server.policy;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.android.server.policy.WindowOrientationListener;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoWindowOrientationListenerImpl implements IVivoWindowOrientationListener {
    private static final int DEFAULT_BATCH_LATENCY = 100000;
    private static final int DISABLE_WINDOW_ORIETATION_SENSOR = 0;
    private static final int ENABLE_WINDOW_ORIETATION_SENSOR = 1;
    private static final String TAG = "WindowOrientationListenerImpl";
    private static int lastChangedRotation = -2;
    private WindowOrientationListener.AccelSensorJudge mAccelSensorJudge;
    private boolean mEnabled;
    private Sensor mSensor;
    private SensorHandler mSensorHandler;
    private HandlerThread mSensorHandlerThread;
    private SensorManager mSensorManager;
    private WindowOrientationListener mWindowOrientationListener;

    public VivoWindowOrientationListenerImpl(WindowOrientationListener windoworientationlistener, SensorManager sensormanager) {
        this.mSensorHandler = null;
        this.mSensorHandlerThread = null;
        this.mWindowOrientationListener = windoworientationlistener;
        this.mSensorManager = sensormanager;
        this.mSensor = windoworientationlistener.mSensor;
        HandlerThread handlerThread = new HandlerThread("WindowOrietationImplEnableSensor");
        this.mSensorHandlerThread = handlerThread;
        handlerThread.start();
        this.mSensorHandler = new SensorHandler(this.mSensorHandlerThread.getLooper());
    }

    public void setAccelSensorJudge(WindowOrientationListener.AccelSensorJudge accelsensorjudge) {
        this.mAccelSensorJudge = accelsensorjudge;
    }

    public void updateRotation(float x, float y, float z, long now) {
        WindowOrientationListener.AccelSensorJudge accelSensorJudge = this.mAccelSensorJudge;
        accelSensorJudge.oldProposedRotation = accelSensorJudge.mProposedRotation;
        this.mAccelSensorJudge.mProposedRotation = (int) x;
        if (this.mAccelSensorJudge.mProposedRotation < 0 || this.mAccelSensorJudge.mProposedRotation > 3) {
            this.mAccelSensorJudge.mProposedRotation = -1;
        }
        WindowOrientationListener.AccelSensorJudge accelSensorJudge2 = this.mAccelSensorJudge;
        accelSensorJudge2.proposedRotation = accelSensorJudge2.mProposedRotation;
        WindowOrientationListener.AccelSensorJudge accelSensorJudge3 = this.mAccelSensorJudge;
        accelSensorJudge3.updatePredictedRotationLocked(now, accelSensorJudge3.mProposedRotation);
    }

    public void changeWhenNotTouch() {
        if (this.mAccelSensorJudge.proposedRotation != this.mAccelSensorJudge.oldProposedRotation && this.mAccelSensorJudge.proposedRotation >= 0 && !this.mAccelSensorJudge.mTouched) {
            VSlog.i(TAG, "Proposed rotation changed!  proposedRotation=" + this.mAccelSensorJudge.proposedRotation + ", oldProposedRotation=" + this.mAccelSensorJudge.oldProposedRotation);
            lastChangedRotation = this.mAccelSensorJudge.proposedRotation;
            this.mWindowOrientationListener.onProposedRotationChanged(this.mAccelSensorJudge.proposedRotation);
        } else if (this.mAccelSensorJudge.mTouched) {
            VSlog.i(TAG, "mTouching! not update !!!  proposedRotation=" + this.mAccelSensorJudge.proposedRotation + ", oldProposedRotation=" + this.mAccelSensorJudge.oldProposedRotation + "; lastChangedRotation=" + lastChangedRotation);
        }
    }

    public void updateWhenTouchEnd(boolean LOG) {
        if (lastChangedRotation == this.mAccelSensorJudge.mProposedRotation) {
            return;
        }
        if (LOG) {
            VSlog.i(TAG, "onTouchEndLocked: mTouching is false !  proposedRotation=" + this.mAccelSensorJudge.proposedRotation + ", oldProposedRotation=" + this.mAccelSensorJudge.oldProposedRotation);
        }
        if (this.mAccelSensorJudge.proposedRotation >= 0) {
            lastChangedRotation = this.mAccelSensorJudge.proposedRotation;
            this.mWindowOrientationListener.onProposedRotationChanged(this.mAccelSensorJudge.proposedRotation);
            VSlog.v(TAG, "in onTouchEndLocked. proposedRotation=" + this.mAccelSensorJudge.proposedRotation + ", oldProposedRotation=" + this.mAccelSensorJudge.oldProposedRotation + ", lastChangedRotation=" + lastChangedRotation);
        }
    }

    public void enableSensor(boolean enable) {
        if (enable) {
            this.mSensorHandler.removeMessages(1);
            this.mSensorHandler.sendEmptyMessage(1);
            return;
        }
        this.mSensorHandler.removeMessages(0);
        this.mSensorHandler.sendEmptyMessage(0);
    }

    /* loaded from: classes.dex */
    private class SensorHandler extends Handler {
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 0) {
                if (i == 1 && !VivoWindowOrientationListenerImpl.this.mEnabled) {
                    if (1 != VivoWindowOrientationListenerImpl.this.mSensor.getType()) {
                        VivoWindowOrientationListenerImpl.this.mSensorManager.registerListener((SensorEventListener) VivoWindowOrientationListenerImpl.this.mAccelSensorJudge, VivoWindowOrientationListenerImpl.this.mSensor, VivoWindowOrientationListenerImpl.this.mWindowOrientationListener.mRate, VivoWindowOrientationListenerImpl.this.mWindowOrientationListener.mHandler);
                    } else {
                        VivoWindowOrientationListenerImpl.this.mSensorManager.registerListener(VivoWindowOrientationListenerImpl.this.mAccelSensorJudge, VivoWindowOrientationListenerImpl.this.mSensor, VivoWindowOrientationListenerImpl.this.mWindowOrientationListener.mRate, 100000, VivoWindowOrientationListenerImpl.this.mWindowOrientationListener.mHandler);
                    }
                    VivoWindowOrientationListenerImpl.this.mEnabled = true;
                }
            } else if (VivoWindowOrientationListenerImpl.this.mEnabled) {
                VivoWindowOrientationListenerImpl.this.mSensorManager.unregisterListener((SensorEventListener) VivoWindowOrientationListenerImpl.this.mAccelSensorJudge);
                VivoWindowOrientationListenerImpl.this.mEnabled = false;
            }
        }

        public SensorHandler(Looper looper) {
            super(looper);
        }
    }
}