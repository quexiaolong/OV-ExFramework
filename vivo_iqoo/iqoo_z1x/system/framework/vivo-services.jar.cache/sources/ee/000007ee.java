package com.vivo.services.sarpower;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import com.vivo.sensor.autobrightness.config.AblConfig;
import com.vivo.sensor.autobrightness.utils.SElog;
import com.vivo.sensor.sarpower.VivoSarConfig;
import dalvik.system.PathClassLoader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class VivoQcomSarPowerStateController extends VivoSarConfig {
    private static final String ACTION_CLOSE_CAMERA = "com.android.camera.ACTION_CLOSE_CAMERA";
    private static final String ACTION_OPEN_CAMERA = "com.android.camera.ACTION_OPEN_CAMERA";
    private static final int ACTION_SAR_POWER_MAX = 8;
    private static final int ACTION_SAR_POWER_MIN = 0;
    private static final String CAMERA_ID = "mCameraId";
    private static final byte DEFAULT_POWER_STATE = 0;
    private static final int MSG_SAR_POWER_CHANGE = 0;
    private static final String NotifySarDsiChangeToWifiAction = "android.net.sar.SENSOR_TO_WIFI";
    private static final String PROP_MANUAL_DSI = "persist.sys.vivo.fixeddsi";
    private static final String QcRilHookClassName = "com.qualcomm.qcrilhook.QcRilHook";
    private static final String SetSarPowerMethodName = "qcRilSetSarPower";
    private static final String TAG = "SarPowerStateService";
    private static Context mContext;
    private static HandlerThread mThread;
    private Class<?> QcRilHook;
    private Constructor<?>[] cons;
    private final BroadcastReceiver mCameraEventReceiver;
    private byte mLastReportSarPowerStateToWifi;
    private Looper mMainLooper;
    private int mManualDsi;
    private PowerChangeHandler mPowerChangeHandler;
    private Object mSarQcRilHook;
    private Method qcRilSetSarPower;
    private static final String model = SystemProperties.get("ro.vivo.product.model", "unkown").toLowerCase();
    private static final boolean eu = SystemProperties.get("persist.vivo.sar.aera", "unkown").toLowerCase().equals("eu");

    public VivoQcomSarPowerStateController(VivoSarPowerStateService service, Context contxt) {
        super(service);
        this.QcRilHook = null;
        this.cons = null;
        this.mSarQcRilHook = null;
        this.qcRilSetSarPower = null;
        this.mLastReportSarPowerStateToWifi = (byte) 0;
        this.mManualDsi = -1;
        this.mCameraEventReceiver = new BroadcastReceiver() { // from class: com.vivo.services.sarpower.VivoQcomSarPowerStateController.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                SElog.d(VivoQcomSarPowerStateController.TAG, "mCameraEventReceiver action:" + action);
                if (VivoQcomSarPowerStateController.ACTION_OPEN_CAMERA.equals(action)) {
                    try {
                        String cameraIDStr = intent.getStringExtra(VivoQcomSarPowerStateController.CAMERA_ID);
                        SElog.d(VivoQcomSarPowerStateController.TAG, "mCameraEventReceiver:  cameraStr " + cameraIDStr);
                        int cameraID = Integer.parseInt(cameraIDStr);
                        if (cameraID == 1) {
                            VivoQcomSarPowerStateController.this.isOpenFrontCamera = true;
                            VivoQcomSarPowerStateController.this.mPowerChangeHandler.removeMessages(0);
                            VivoQcomSarPowerStateController.this.mPowerChangeHandler.sendMessageDelayed(VivoQcomSarPowerStateController.this.mPowerChangeHandler.obtainMessage(0), 0L);
                        }
                    } catch (Exception e) {
                        SElog.d(VivoQcomSarPowerStateController.TAG, "ACTION_OPEN_CAMERA exp");
                    }
                } else if (VivoQcomSarPowerStateController.ACTION_CLOSE_CAMERA.equals(action)) {
                    try {
                        String cameraIDStr2 = intent.getStringExtra(VivoQcomSarPowerStateController.CAMERA_ID);
                        SElog.d(VivoQcomSarPowerStateController.TAG, "mCameraEventReceiver:  cameraStr " + cameraIDStr2);
                        int cameraID2 = Integer.parseInt(cameraIDStr2);
                        if (cameraID2 == 1) {
                            VivoQcomSarPowerStateController.this.isOpenFrontCamera = false;
                            VivoQcomSarPowerStateController.this.mPowerChangeHandler.removeMessages(0);
                            VivoQcomSarPowerStateController.this.mPowerChangeHandler.sendMessageDelayed(VivoQcomSarPowerStateController.this.mPowerChangeHandler.obtainMessage(0), 0L);
                        }
                    } catch (Exception e2) {
                        SElog.d(VivoQcomSarPowerStateController.TAG, "ACTION_OPEN_CAMERA exp");
                    }
                }
            }
        };
        mContext = contxt;
        HandlerThread handlerThread = new HandlerThread("SarPowerStateService_Qcom");
        mThread = handlerThread;
        handlerThread.start();
        this.mMainLooper = mThread.getLooper();
        this.mPowerChangeHandler = new PowerChangeHandler(this.mMainLooper);
        if (model.equals("pd1730c") || model.equals("pd1730cf_ex") || model.equals("pd1730g")) {
            IntentFilter filterOpenCamera = new IntentFilter();
            filterOpenCamera.addAction(ACTION_OPEN_CAMERA);
            mContext.registerReceiver(this.mCameraEventReceiver, filterOpenCamera);
            IntentFilter filterCloseCamera = new IntentFilter();
            filterCloseCamera.addAction(ACTION_CLOSE_CAMERA);
            mContext.registerReceiver(this.mCameraEventReceiver, filterCloseCamera);
        }
        try {
            this.mManualDsi = SystemProperties.getInt(PROP_MANUAL_DSI, -1);
        } catch (Exception e) {
            SElog.e(TAG, "get mManualDsi fail!");
            e.printStackTrace();
        }
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public boolean initialPowerState() {
        boolean reflectRet = sarQcRilHookReflect();
        if (!reflectRet) {
            SElog.e(TAG, "sarQcRilHookReflect init fail");
            return false;
        }
        return true;
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void handleSarMessage(int sarMsg, int delayTimes) {
        if (sarMsg == 0) {
            this.mPowerChangeHandler.removeMessages(0);
            PowerChangeHandler powerChangeHandler = this.mPowerChangeHandler;
            powerChangeHandler.sendMessageDelayed(powerChangeHandler.obtainMessage(0), delayTimes);
        }
    }

    private boolean sarQcRilHookReflect() {
        PathClassLoader classLoader = new PathClassLoader("/system_ext/framework/qti-telephony-common.jar:/system_ext/framework/qti-telephony-utils.jar", ClassLoader.getSystemClassLoader());
        try {
            Class<?> cls = Class.forName(QcRilHookClassName, false, classLoader);
            this.QcRilHook = cls;
            if (cls == null) {
                SElog.e(TAG, "QcRilHook class get fail");
                return false;
            }
            Constructor<?>[] constructors = cls.getConstructors();
            this.cons = constructors;
            if (constructors == null) {
                SElog.e(TAG, "Constructors get fail");
                return false;
            }
            Object newInstance = constructors[0].newInstance(mContext);
            this.mSarQcRilHook = newInstance;
            if (newInstance == null) {
                SElog.e(TAG, "mSarQcRilHook Object get fail");
                return false;
            }
            Method method = this.QcRilHook.getMethod(SetSarPowerMethodName, Byte.TYPE);
            this.qcRilSetSarPower = method;
            if (method == null) {
                SElog.e(TAG, "mSarQcRilHook Method get fail");
                return false;
            }
            return true;
        } catch (Exception e) {
            SElog.e(TAG, "sarQcRilHookReflect throws exception ");
            e.printStackTrace();
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setQcRiSarPowerState(byte powerState) {
        if (this.qcRilSetSarPower == null) {
            SElog.d(TAG, "qcRilSetSarPower is null return");
            return;
        }
        try {
            powerState = changeDsiOnFactoryMode(powerState);
            notifyDSItosystem(powerState);
            boolean sarTestResult = ((Boolean) this.qcRilSetSarPower.invoke(this.mSarQcRilHook, new Byte(powerState))).booleanValue();
            if (!sarTestResult) {
                SElog.e(TAG, "writeSarTestMode return fail");
            }
        } catch (Exception e) {
            SElog.e(TAG, "setQcRiSarPowerState throws exception ");
            e.printStackTrace();
        }
        SElog.d(TAG, "setQcRiSarPowerState powerState = " + ((int) powerState));
    }

    private void notifyDSItosystem(byte powerState) {
        try {
            SystemProperties.set("sys.sar.dsi", String.valueOf((int) powerState));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte changeDsiOnFactoryMode(byte powerState) {
        if (rejectReduceSar()) {
            SElog.d(TAG, "Pretend to setQcRiSarPowerState powerState = " + ((int) powerState));
            powerState = 0;
        }
        int i = this.mManualDsi;
        if (i >= 0) {
            return (byte) i;
        }
        return powerState;
    }

    private boolean rejectReduceSar() {
        return this.isUnderFactoryMode && AblConfig.EngineModeRejectReduceSar() && this.mSAR_A_state != 1;
    }

    /* loaded from: classes.dex */
    private final class PowerChangeHandler extends Handler {
        public PowerChangeHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                byte powerState = VivoQcomSarPowerStateController.this.processStateChange();
                SElog.d(VivoQcomSarPowerStateController.TAG, "PowerChangeHandler power change, powerState = " + ((int) powerState) + ", mLastSarPowerState = " + VivoQcomSarPowerStateController.this.mLastSarPowerState + ", mProxState = " + VivoQcomSarPowerStateController.this.mProximityState + ", A = " + VivoQcomSarPowerStateController.this.mAudioState + ", W = " + VivoQcomSarPowerStateController.this.mWIFIState + ", mENDCState=" + VivoQcomSarPowerStateController.this.mENDCState + ", mScreenState = " + VivoQcomSarPowerStateController.this.mScreenState + ", mCardOneState = " + VivoQcomSarPowerStateController.this.mCardOneState + ", mCardTwoState = " + VivoQcomSarPowerStateController.this.mCardTwoState + ", mLastCardState = " + VivoQcomSarPowerStateController.this.mLastCardState + ", mSAR_A_state=" + VivoQcomSarPowerStateController.this.mSAR_A_state + ", mSAR_A_CS0_state=" + VivoQcomSarPowerStateController.this.mSAR_A_CS0_state + ", mSAR_A_CS1_state=" + VivoQcomSarPowerStateController.this.mSAR_A_CS1_state + ", mSAR_A_CS2_state=" + VivoQcomSarPowerStateController.this.mSAR_A_CS2_state + ", mSAR_B_CS0_state=" + VivoQcomSarPowerStateController.this.mSAR_B_CS0_state + ", mSAR_B_state=" + VivoQcomSarPowerStateController.this.mSAR_B_state + ", mSAR_B_CS1_state=" + VivoQcomSarPowerStateController.this.mSAR_B_CS1_state + ", mSAR_B_CS2_state=" + VivoQcomSarPowerStateController.this.mSAR_B_CS2_state + ", mSarPowerRfDetectState = " + VivoQcomSarPowerStateController.this.mSarPowerRfDetectState + ", mFrontProxState= " + VivoQcomSarPowerStateController.this.mFrontProximityState + ", mBackProxState= " + VivoQcomSarPowerStateController.this.mBackProximityState + ", mHPState=" + VivoQcomSarPowerStateController.this.mHeadsetPlugState);
                if (VivoQcomSarPowerStateController.this.mLastSarPowerState != powerState || VivoQcomSarPowerStateController.this.mForceUpdateState) {
                    if (VivoQcomSarPowerStateController.this.mForceUpdateState) {
                        SElog.d(VivoQcomSarPowerStateController.TAG, "force update");
                        VivoQcomSarPowerStateController.this.mForceUpdateState = false;
                    }
                    if (VivoQcomSarPowerStateController.this.mNeedReset) {
                        SElog.d(VivoQcomSarPowerStateController.TAG, "reseting PowerState");
                        VivoQcomSarPowerStateController.this.setQcRiSarPowerState((byte) 0);
                        VivoQcomSarPowerStateController.this.mNeedReset = false;
                    }
                    VivoQcomSarPowerStateController.this.mLastSarPowerState = powerState;
                    VivoQcomSarPowerStateController.this.setQcRiSarPowerState(powerState);
                    return;
                }
                return;
            }
            SElog.d(VivoQcomSarPowerStateController.TAG, "PowerChangeHandler default, the mProximityState is" + VivoQcomSarPowerStateController.this.mProximityState);
        }
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void notifyDsiToWifi(byte SarPowerStateToWifi) {
        if (this.mLastReportSarPowerStateToWifi != SarPowerStateToWifi) {
            SElog.d(TAG, "notifyDsiToWifi : " + ((int) SarPowerStateToWifi));
            Intent intent = new Intent(NotifySarDsiChangeToWifiAction);
            intent.addFlags(67108864);
            intent.putExtra("SarDsi", Integer.valueOf(SarPowerStateToWifi));
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            this.mLastReportSarPowerStateToWifi = SarPowerStateToWifi;
        }
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void notifySarPowerTest(int powerStateValue) {
        if (powerStateValue <= 8 && powerStateValue >= 0) {
            byte powerState = (byte) powerStateValue;
            setQcRiSarPowerState(powerState);
        }
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void dump(PrintWriter pw) {
        pw.println(String.format("---- %s ----", TAG));
    }
}