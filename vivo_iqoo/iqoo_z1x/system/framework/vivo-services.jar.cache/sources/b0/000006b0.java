package com.vivo.services.proxcali;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VivoDisplayModule;
import android.hardware.display.VivoDisplayStateManager;
import android.multidisplay.MultiDisplayManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.FtFeature;
import android.view.Display;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.sensor.SDC.ExceptionController;
import com.vivo.sensor.autobrightness.config.AblConfig;
import com.vivo.sensor.implement.SensorConfig;
import com.vivo.sensor.implement.VivoSensorImpl;
import com.vivo.sensor.sensoroperate.DeviceParaProvideService;
import com.vivo.sensor.sensoroperate.SensorTestResult;
import com.vivo.sensor.sensoroperate.VivoSensorOperationResult;
import com.vivo.sensor.sensoroperate.VivoSensorOperationUtils;
import com.vivo.sensor.sensoroperate.VivoSensorTest;
import com.vivo.services.rms.ProcessList;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import vendor.vivo.hardware.vibrator_hall.V1_0.IVib_Hall;
import vivo.app.proxcali.IVivoProxCali;

/* loaded from: classes.dex */
public class VivoProxCaliService extends IVivoProxCali.Stub {
    private static final String ACTION_DAY_START_CLOCK_TIMER = "android.intent.action.auto_bright.day.start";
    private static final String ACTION_PHONE_STATE_CHANGE = "android.intent.action.PHONE_STATE";
    private static final String ACTION_WECHAT_OPEN = "android.intent.action.open";
    private static final String ACTION_WECHAT_QUIT = "android.intent.action.quit";
    private static final int APDS9960_PROXIMITY_RAW_MARGIN = 10;
    private static final String BASE_THRESHOLD_SENSOR = "persist.sys.base_threshold_prox";
    private static final String BASE_THRESHOLD_SENSOR_B = "persist.sys.base_threshold_prox_b";
    private static final int CALL_PHONE_CALLING = 9;
    private static final int CALL_PHONE_OFFHOOK = 10;
    private static final int CALL_STATE_DIALING = 2;
    private static final int CALL_STATE_IDLE = 0;
    private static final int CALL_STATE_OFFHOOK = 3;
    private static final int CALL_STATE_RINGING = 1;
    private static final int EM_ACC_BIAS_FILE_CREATE = 1285;
    private static final int ENHANCE_PROXIMITY_POWER = 1;
    private static final String FINGER_MODULE_NAME = "UDFingerprint";
    private static final int GP2AP052A_PROXIMITY_RAW_MARGIN = 400;
    private static final int LIGHT_TIMES_LIMIT = 12;
    private static final int MAX_CALI_RETRY_TIMES = 5;
    private static final int MSG_CREATE_ACC_FILE_ON_BOOT = 4102;
    private static final int MSG_SHOW_DIALOG_BY_ACC = 4103;
    private static final int MSG_VIVONOTIFY_PHONESTATE = 4100;
    private static final int MSG_VIVONOTIFY_WECHAT_OPEN = 4104;
    private static final int MSG_VIVONOTIFY_WECHAT_QUIT = 4105;
    private static final int MSG_VIVOSENSORTEST_ONBOOT_EVENT = 4096;
    private static final int MSG_VIVOSENSORTEST_ONPOWERCONNECT_CHANGE_EVENT = 4101;
    private static final int MSG_VIVOSENSORTEST_ONSCREENON_EVENT = 4097;
    private static final String NEED_CHANGE_PROXIMITY_PULSE = "persist.sys.need_change_pulse";
    private static final int NOT_SUPPORT_SAR = 3;
    private static final String PROP_ACC_CALI_DATA = "persist.sys.gsensor_cal_xyz";
    private static final String PROP_SENSOR_DUMP_KEY = "debug.sensor.dump";
    public static final int PROXIMITY_BOOTUP_CALI = 0;
    public static final int PROXIMITY_PHONE_CALI = 2;
    public static final int PROXIMITY_POWERKEY_CALI = 1;
    private static final int PROXIMITY_TIMES_LIMIT = 12;
    public static final int PROXIMITY_UNKNOWN_CALI = -1;
    private static final int PROX_B_SET_CALI_OFFSET_DATA = 837;
    private static final String PS_CALI_FLAG = "persist.sys.ps_cali_flag";
    private static final String PS_CALI_OFFSET_DATA_B = "persist.sys.ps_cali_offset_data_b";
    private static final String PS_CALI_OFFSET_FLAG = "persist.sys.ps_offset";
    private static final String PS_CALI_OFFSET_FLAG_B = "persist.sys.ps_offset_b";
    private static final int PS_DRIVER_TEMP_CALI = 527;
    private static final int PS_DRIVER_TEMP_CALI_BACK = 839;
    private static final int PS_SET_CALI_OFFSET_DATA = 524;
    private static final int PS_SET_ENG_CALI_DATA = 520;
    private static final int PS_SET_ENG_CALI_DATA_BACK = 834;
    private static final int PS_SET_ENG_CALI_DATA_FRONT_LOW = 529;
    private static final int SAR_A_COMPENSATION_CALL = 1030;
    private static final int SAR_B_COMPENSATION_CALL = 1039;
    private static final int SENSOR_COMMAND_SET_PS_CALI_DATA = 22;
    private static final int SENSOR_COMMAND_SET_PS_CALI_OFFSET_DATA = 24;
    private static final int SENSOR_COMMAND_SET_PS_DRIVER_TEMP_CALI = 25;
    private static final int SENSOR_COMMAND_SET_PS_PARA_INDEX = 9;
    private static final String SENSOR_DUMP_DISABLED = "disabled";
    private static final String SENSOR_DUMP_ENABLED = "enabled";
    private static final int STATE_PHONE_CALLING = 32;
    private static final int STATE_PHONE_UNDER_FLAT = 64;
    private static final int STATE_PROXIMITY_SCREEN_OFF_WAKE_LOCK = 16;
    private static final int SUPPORT_SAR_A = 0;
    private static final int SUPPORT_SAR_A_AND_B = 1;
    private static final String SUPPORT_SAR_A_COMPENSATION_CALI = "persist.sys.sar_a_cmpensation";
    private static final int SUPPORT_SAR_B = 2;
    private static final String SUPPORT_SAR_B_COMPENSATION_CALI = "persist.sys.sar_b_cmpensation";
    private static final String TAG = "ProxCaliService";
    private static final int TMD2772_PROXIMITY_RAW_MARGIN = 50;
    private static final String TMP_BASE_THRESHOLD_SENSOR = "persist.sys.tmp_base_thres_prox";
    private static final String Under_TEMP_BASE_THRESHOLD_SENSOR = "persist.sys.tmp_base_thres_prox_under";
    private static final int VIVO_MAIN_SCREEN_ID = 0;
    private static final int VIVO_SECOND_SCREEN_ID = 4096;
    private boolean CaliSaronUserUnlock;
    private int UserUnlockDelay;
    private Sensor mAccSensor;
    private SensorEventListener mAccSensorListener;
    private int mBackDisplayState;
    private final IntentFilter mBootCompleteFilter;
    private final BroadcastReceiver mBootPsCaliReceiver;
    private final IntentFilter mBrightDayFilter;
    private int mCallNewState;
    private int mCallOldeState;
    private Context mContext;
    private DeviceParaProvideService mDeviceParaProvideService;
    private long mDirectCallTime;
    private Display mDisplayBack;
    private VivoDisplayStateManager.DisplayContentListener mDisplayContentListener;
    private Display mDisplayFront;
    private DisplayManager mDisplayManager;
    private ExceptionController mExceptionController;
    private boolean mFingerIconVisable;
    private int mFrontDisplayState;
    private boolean mIsCrystalAnim;
    private int mLastBackDisplayState;
    private int mLastFrontDisplayState;
    private float mLightCaliValue;
    private int mLightCounts;
    private Sensor mLightSensor;
    private SensorEventListener mLightSensorListener;
    private float mLightThreshould;
    private final IntentFilter mPhoneFilter;
    private int mPhoneFlag;
    private long mPhoneStateOffHookTs;
    private final IntentFilter mPowerConnect;
    private final IntentFilter mPowerDisConnect;
    private MultiDisplayManager mProiximityDisplayManager;
    private HandlerThread mProxCaliThread;
    private int mProximityDisplayId;
    private MultiDisplayManager.FocusDisplayListener mProximityFocusDisplayListener;
    private Sensor mProximitySensor;
    private SensorEventListener mProximitySensorListenerVST;
    private int mResetSensorHubCommand;
    private final BroadcastReceiver mSarSensorCaliReceiver;
    private Sensor mSarSensor_A;
    private Sensor mSarSensor_B;
    private final IntentFilter mScreenOffFilter;
    private final IntentFilter mScreenOnFilter;
    private SensorErrorDialog mSensorDialog;
    private SensorDump mSensorDump;
    private SensorManager mSensorManager;
    private Runnable mStartCaliRunnable;
    private Handler mStartHandler;
    private Runnable mStopCaliRunnable;
    private Handler mStopHandler;
    private boolean mSupportSarA_ComPensation_cail;
    private boolean mSupportSarB_ComPensation_cail;
    private int[] mTestArg;
    private SensorTestResult mTestResult;
    private final IntentFilter mUserPresent;
    private String mVideoPhoneCallId;
    private int mVideoState;
    private DisplayManager.DisplayListener mVivoDisplayListener;
    private VivoDisplayStateManager mVivoDisplayStateManager;
    private VivoSensorImpl mVivoSensorImpl;
    private VivoSensorOperationUtils mVivoSensorOperationUtils;
    private VivoSensorTest mVivoSensorTest;
    private VivoSensorTestHandler mVivoSensorTestHandler;
    private HandlerThread mVivoSensortThread;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager pm;
    private int powerConnectDelay;
    private int powerDisConnDelay;
    private static boolean mIsCalibrationing = false;
    private static boolean mNeedStopCali = false;
    private static boolean mIsVerifying = false;
    private static boolean mIsUseVST = true;
    private static boolean isNormalProx = SensorConfig.isNormalProx();
    private static boolean isUnderProx = SensorConfig.isUnderProx();
    private static boolean isDoubleScreen = SensorConfig.isDoubleScreen();
    private static boolean mScreenOnProxTempCali = AblConfig.isScreenOnProxTempCali();
    private static boolean mIsDriverProxTempCali = AblConfig.isDriverProxTempCali();
    private static boolean mChangeProxParam = AblConfig.motionChangeProxParam();
    private static boolean mIsSupportCreateAccFile = AblConfig.isSupportCreateAccFile();
    private static boolean mLightLeak = AblConfig.isLightLeak();
    private static boolean mDecreaseProxBaseValue = AblConfig.decreaseProxBaseValue();
    private static int mCaliStartBy = -1;
    private static int mLastTmpThreshHold = -1;
    private static int mCurrentTmpThreshHold = -1;
    private static final String mOpEntry = SystemProperties.get("ro.vivo.op.entry", "no");
    private static IVib_Hall sVibHallInstance = null;

    static /* synthetic */ int access$1512(VivoProxCaliService x0, int x1) {
        int i = x0.mLightCounts + x1;
        x0.mLightCounts = i;
        return i;
    }

    static /* synthetic */ float access$1716(VivoProxCaliService x0, float x1) {
        float f = x0.mLightCaliValue + x1;
        x0.mLightCaliValue = f;
        return f;
    }

    private static boolean isOpEntry() {
        return mOpEntry.equals("CMCC") || mOpEntry.contains("_RW");
    }

    public int camera_record() {
        if (!FtFeature.isFeatureSupport("vivo.hardware.popupcamera")) {
            VLog.e(TAG, "camera not support FEATURE_POPUP_FRONT_CAMERA!!");
            return -1;
        }
        VLog.e(TAG, "camera support FEATURE_POPUP_FRONT_CAMERA!!");
        try {
            IVib_Hall.getService();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void doPsCaliUseVST(SensorTestResult mRes) {
        VivoSensorTest vivoSensorTest = this.mVivoSensorTest;
        if (vivoSensorTest != null) {
            vivoSensorTest.vivoSensorTest(36, mRes, this.mTestArg, 0);
            if (this.mLightCaliValue > 5000.0f) {
                mRes.mSuccess = 0;
                VLog.d(TAG, "mLightCaliValue =" + this.mLightCaliValue);
            }
            if (mRes.mSuccess == 1) {
                String strVal = Integer.toString(Math.round(mRes.mTestVal[0]));
                int defBase = Math.round(mRes.mDefBase[0]);
                if (mLightLeak) {
                    int round = Math.round(mRes.mTestVal[0]);
                    mCurrentTmpThreshHold = round;
                    if (mLastTmpThreshHold == -1) {
                        mLastTmpThreshHold = round;
                    }
                    TelephonyManager tpManager = (TelephonyManager) this.mContext.getSystemService("phone");
                    if (tpManager != null) {
                        VLog.d(TAG, "mLastTmpThreshHold = " + mLastTmpThreshHold + " mCurrentTmpThreshHold = " + mCurrentTmpThreshHold + " mCaliStartBy = " + mCaliStartBy + " isRinging = " + tpManager.isRinging());
                    }
                    if (tpManager != null && tpManager.isRinging() && mCaliStartBy == 2) {
                        if (mCurrentTmpThreshHold < mLastTmpThreshHold + 1000) {
                            VLog.d(TAG, "Allow Prox temp cali when Ringing within the range");
                        } else {
                            VLog.d(TAG, "Forbit Prox temp cali when Ringing out of the range");
                            return;
                        }
                    }
                    mLastTmpThreshHold = mCurrentTmpThreshHold;
                }
                try {
                    SystemProperties.set(TMP_BASE_THRESHOLD_SENSOR, strVal);
                } catch (Exception e) {
                    VLog.d(TAG, "setprop persist.sys.tmp_base_thres_prox failed");
                }
                VLog.d(TAG, "setprop persist.sys.tmp_base_thres_prox " + strVal);
                VLog.d(TAG, "doPsCaliUseVST success: " + mRes.dumpString());
                this.mTestArg[0] = Math.min(255, SystemProperties.getInt(BASE_THRESHOLD_SENSOR, 0));
                this.mTestArg[1] = 0;
                if (SystemProperties.getInt(BASE_THRESHOLD_SENSOR, defBase) >= 65535) {
                    this.mTestArg[2] = Math.round(mRes.mTestVal[0]);
                } else if (mDecreaseProxBaseValue) {
                    this.mTestArg[2] = Math.max(Math.round(mRes.mTestVal[0]), (int) mRes.mMinBase[0]);
                    VLog.d(TAG, "delta temp cali vla[0]:" + mRes.mTestVal[0] + "-----Arg[2]:" + this.mTestArg[2] + ";  minbase:" + mRes.mMinBase[0] + "maxbase:" + mRes.mMaxBase[0] + "\n");
                } else {
                    this.mTestArg[2] = Math.max(Math.round(mRes.mTestVal[0]), SystemProperties.getInt(BASE_THRESHOLD_SENSOR, defBase));
                }
                VivoSensorTest vivoSensorTest2 = this.mVivoSensorTest;
                int[] iArr = this.mTestArg;
                vivoSensorTest2.vivoSensorTest(34, mRes, iArr, iArr.length);
                return;
            }
            DeviceParaProvideService dpService = (DeviceParaProvideService) this.mContext.getSystemService("device_para_provide_service");
            this.mTestArg[0] = Math.min(255, SystemProperties.getInt(BASE_THRESHOLD_SENSOR, 0));
            if (mCaliStartBy == 2) {
                this.mTestArg[1] = 1;
            } else {
                this.mTestArg[1] = 0;
            }
            if (dpService != null) {
                this.mTestArg[2] = Math.max(SystemProperties.getInt(TMP_BASE_THRESHOLD_SENSOR, dpService.getPsBaseValue()), SystemProperties.getInt(BASE_THRESHOLD_SENSOR, dpService.getPsBaseValue()));
            } else {
                this.mTestArg[2] = Math.max(SystemProperties.getInt(TMP_BASE_THRESHOLD_SENSOR, 500), SystemProperties.getInt(BASE_THRESHOLD_SENSOR, 500));
            }
            VivoSensorTest vivoSensorTest3 = this.mVivoSensorTest;
            int[] iArr2 = this.mTestArg;
            vivoSensorTest3.vivoSensorTest(34, mRes, iArr2, iArr2.length);
            VLog.d(TAG, "doPsCaliUseVST fail: " + mRes.dumpString() + " set caldata as" + this.mTestArg[2]);
        }
    }

    public void setPhoneVideoCallState(String callId, int videoState, int callOldState, int callNewState) {
        this.mVideoPhoneCallId = callId;
        this.mVideoState = videoState;
        this.mCallOldeState = callOldState;
        this.mCallNewState = callNewState;
    }

    private void enableBackProximityDriverCali(boolean enable) {
        if (enable) {
            SensorTestResult mTempRes = new SensorTestResult();
            int mBaseValue = SystemProperties.getInt(BASE_THRESHOLD_SENSOR_B, -1);
            mIsCalibrationing = true;
            mNeedStopCali = false;
            int[] mTempTestArg = {25, mBaseValue};
            if (AblConfig.getDoubleScreenScreenId() == 4096) {
                this.mVivoSensorTest.vivoSensorTest((int) PS_DRIVER_TEMP_CALI_BACK, mTempRes, mTempTestArg, mTempTestArg.length);
                VLog.d(TAG, "enabledrivercali  back proximity cali_data: data[0]" + mTempTestArg[0] + " cali_data " + mTempTestArg[1]);
            }
            mIsCalibrationing = false;
            mNeedStopCali = false;
        } else if (!mNeedStopCali) {
            mNeedStopCali = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enabledrivercali(boolean enable) {
        if (mIsCalibrationing && enable) {
            VLog.d(TAG, "enabledrivercali,return,as mIsCalibrationing=" + mIsCalibrationing + " enable=" + enable);
        } else if (AblConfig.supportDoubleScreenOrNot()) {
            enableBackProximityDriverCali(enable);
        } else if (enable) {
            SensorTestResult mTempRes = new SensorTestResult();
            int mBaseValue = SystemProperties.getInt(BASE_THRESHOLD_SENSOR, -1);
            mIsCalibrationing = true;
            mNeedStopCali = false;
            int[] mTempTestArg = {25, mBaseValue};
            VivoSensorTest vivoSensorTest = this.mVivoSensorTest;
            if (vivoSensorTest != null) {
                vivoSensorTest.vivoSensorTest((int) PS_DRIVER_TEMP_CALI, mTempRes, mTempTestArg, mTempTestArg.length);
                VLog.d(TAG, "enabledrivercali cali_data: data[0]" + mTempTestArg[0] + " cali_data " + mTempTestArg[1]);
            }
            mIsCalibrationing = false;
            mNeedStopCali = false;
        } else if (!mNeedStopCali) {
            mNeedStopCali = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableCalibrationUseVST(boolean enable) {
        int i;
        if (mIsCalibrationing && enable) {
            VLog.d(TAG, "WIRED,return,as mIsCalibrationing=" + mIsCalibrationing + " enable=" + enable);
        } else if (enable) {
            mIsCalibrationing = true;
            mNeedStopCali = false;
            this.mSensorManager.registerListener(this.mProximitySensorListenerVST, this.mProximitySensor, 1);
            this.mLightCounts = 0;
            this.mLightCaliValue = 0.0f;
            this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, 1);
            doPsCaliUseVST(this.mTestResult);
            if (this.mTestResult.mSuccess == 1 || mCaliStartBy != 2) {
                if (this.mTestResult.mSuccess != 1 && ((i = mCaliStartBy) == 0 || i == 1)) {
                    for (int i2 = 0; i2 < 5 && !mNeedStopCali; i2++) {
                        doPsCaliUseVST(this.mTestResult);
                        if (this.mTestResult.mSuccess == 1) {
                            break;
                        }
                    }
                } else {
                    VLog.d(TAG, "finish prox temp cali");
                }
            } else {
                TelephonyManager tpManager = (TelephonyManager) this.mContext.getSystemService("phone");
                if (tpManager != null) {
                    while (this.mTestResult.mSuccess != 1 && tpManager.isRinging() && !mNeedStopCali) {
                        doPsCaliUseVST(this.mTestResult);
                        if (this.mTestResult.mSuccess == 1) {
                            break;
                        }
                    }
                }
            }
            new SensorTestResult();
            int[] iArr = new int[3];
            this.mSensorManager.unregisterListener(this.mProximitySensorListenerVST);
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
            mIsCalibrationing = false;
            mNeedStopCali = false;
        } else {
            if (!mNeedStopCali) {
                mNeedStopCali = true;
            }
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
        }
    }

    public static String sss(String myString) {
        Pattern CRLF = Pattern.compile("(\r\n|\r|\n|\n\r)");
        Matcher m = CRLF.matcher(myString);
        if (!m.find()) {
            return null;
        }
        String newString = m.replaceAll(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        return newString;
    }

    /* loaded from: classes.dex */
    class VivoSensorTestHandler extends Handler {
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 4096:
                    VivoProxCaliService.this.handlerOnBootVivoSensorTestEvent();
                    return;
                case 4097:
                    VivoProxCaliService.this.handlerVivoSensorTestOnScreenOnEvent();
                    return;
                case 4098:
                case 4099:
                default:
                    return;
                case VivoProxCaliService.MSG_VIVONOTIFY_PHONESTATE /* 4100 */:
                    int PhoneState = VivoProxCaliService.this.getPhoneCallState();
                    if (PhoneState == 0) {
                        VivoProxCaliService.this.mPhoneStateOffHookTs = System.currentTimeMillis();
                    }
                    if (PhoneState == 1 || PhoneState == 2) {
                        VivoProxCaliService.this.notifyProxmitySwitchThres(9);
                        return;
                    } else {
                        VivoProxCaliService.this.notifyProxmitySwitchThres(10);
                        return;
                    }
                case VivoProxCaliService.MSG_VIVOSENSORTEST_ONPOWERCONNECT_CHANGE_EVENT /* 4101 */:
                    if (VivoProxCaliService.this.mSarSensor_A != null) {
                        VivoProxCaliService.this.handlerSarA_Compensation_cali();
                    }
                    if (VivoProxCaliService.this.mSarSensor_B != null) {
                        VivoProxCaliService.this.handlerSarB_Compensation_cali();
                        return;
                    }
                    return;
                case VivoProxCaliService.MSG_CREATE_ACC_FILE_ON_BOOT /* 4102 */:
                    VivoProxCaliService.this.handleCreateAccFile();
                    return;
                case VivoProxCaliService.MSG_SHOW_DIALOG_BY_ACC /* 4103 */:
                    VivoProxCaliService.this.showDialogforI2CException();
                    return;
                case VivoProxCaliService.MSG_VIVONOTIFY_WECHAT_OPEN /* 4104 */:
                    VivoProxCaliService.this.notifyProxmitySwitchThres(9);
                    return;
                case VivoProxCaliService.MSG_VIVONOTIFY_WECHAT_QUIT /* 4105 */:
                    VivoProxCaliService.this.notifyProxmitySwitchThres(10);
                    return;
            }
        }

        VivoSensorTestHandler(Looper looper) {
            super(looper);
        }
    }

    public VivoProxCaliService() {
        this.mResetSensorHubCommand = 201;
        this.mLightCounts = 0;
        this.mLightCaliValue = 0.0f;
        this.mLightThreshould = 500.0f;
        this.mSupportSarA_ComPensation_cail = false;
        this.mSupportSarB_ComPensation_cail = false;
        this.mExceptionController = null;
        this.mBrightDayFilter = new IntentFilter(ACTION_DAY_START_CLOCK_TIMER);
        this.mProximitySensor = null;
        this.mAccSensor = null;
        this.mLightSensor = null;
        this.pm = null;
        this.mWakeLock = null;
        this.mStartHandler = null;
        this.mStopHandler = null;
        this.mVivoSensorTestHandler = null;
        this.mProximitySensorListenerVST = null;
        this.mLightSensorListener = null;
        this.mStartCaliRunnable = null;
        this.mStopCaliRunnable = null;
        this.mDeviceParaProvideService = null;
        this.mBootPsCaliReceiver = new BootcompleteReceiverForPsCali();
        this.mScreenOnFilter = new IntentFilter("android.intent.action.SCREEN_ON");
        this.mScreenOffFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
        this.mPhoneFilter = new IntentFilter();
        this.mSarSensorCaliReceiver = new SarSensorCaliReceiver();
        this.mBootCompleteFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
        this.mPowerConnect = new IntentFilter("android.intent.action.ACTION_POWER_CONNECTED");
        this.mPowerDisConnect = new IntentFilter("android.intent.action.ACTION_POWER_DISCONNECTED");
        this.mUserPresent = new IntentFilter("android.intent.action.USER_PRESENT");
        this.mVivoSensorTest = VivoSensorTest.getInstance();
        this.mTestResult = new SensorTestResult();
        this.mTestArg = new int[3];
        this.mSensorDump = null;
        this.mIsCrystalAnim = false;
        this.mDirectCallTime = 0L;
        this.mProiximityDisplayManager = null;
        this.mProximityDisplayId = -1;
        this.mVivoSensorImpl = null;
        this.mFingerIconVisable = false;
        this.mDisplayManager = null;
        this.mFrontDisplayState = 2;
        this.mBackDisplayState = 1;
        this.mLastFrontDisplayState = 2;
        this.mLastBackDisplayState = 1;
        this.mPhoneFlag = 0;
        this.mDisplayFront = null;
        this.mDisplayBack = null;
        this.mVideoPhoneCallId = null;
        this.mVideoState = -1;
        this.mCallOldeState = -1;
        this.mCallNewState = -1;
        this.mVivoSensorOperationUtils = null;
        this.mPhoneStateOffHookTs = -1L;
        this.powerConnectDelay = ProcessList.BACKUP_APP_ADJ;
        this.powerDisConnDelay = ProcessList.BACKUP_APP_ADJ;
        this.UserUnlockDelay = ProcessList.BACKUP_APP_ADJ;
        this.CaliSaronUserUnlock = false;
        this.mSarSensor_A = null;
        this.mSarSensor_B = null;
        this.mDisplayContentListener = new VivoDisplayStateManager.DisplayContentListener() { // from class: com.vivo.services.proxcali.VivoProxCaliService.1
            public void onDisplayContentChanged(int displayId, boolean globalVisible, String module, boolean moduleVisible) {
                if (AblConfig.isBbkLog()) {
                    VLog.d(VivoProxCaliService.TAG, "display :" + displayId + " globalVisible:" + globalVisible + " module:" + module + " moduleVisible:" + moduleVisible);
                }
                if (displayId == 0 && "UDFingerprint".equals(module)) {
                    VivoProxCaliService.this.mFingerIconVisable = moduleVisible;
                    VivoProxCaliService.this.mVivoSensorImpl.notifyFingerIconChange(VivoProxCaliService.this.mFingerIconVisable);
                }
            }

            public void onListenerRegistered(List<VivoDisplayModule> primaryDisplayContent, List<VivoDisplayModule> secondaryDisplayContent) {
            }
        };
        this.mAccSensorListener = new SensorEventListener() { // from class: com.vivo.services.proxcali.VivoProxCaliService.6
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                if (event.values[0] != 100.0f || event.values[1] != 100.0f || event.values[2] != 100.0f || VivoProxCaliService.this.mSensorDialog.isShowing()) {
                    VivoProxCaliService.this.mSensorManager.unregisterListener(VivoProxCaliService.this.mAccSensorListener);
                    return;
                }
                VLog.d(VivoProxCaliService.TAG, " I2C exception x = 100,y = 100,z = 100! ");
                VivoProxCaliService.this.showDialogforI2CException();
                VivoProxCaliService.this.mSensorManager.unregisterListener(VivoProxCaliService.this.mAccSensorListener);
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mVivoDisplayListener = new DisplayManager.DisplayListener() { // from class: com.vivo.services.proxcali.VivoProxCaliService.7
            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayAdded(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayRemoved(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayChanged(int displayId) {
                if (VivoProxCaliService.this.mDisplayFront != null) {
                    VivoProxCaliService vivoProxCaliService = VivoProxCaliService.this;
                    vivoProxCaliService.mFrontDisplayState = vivoProxCaliService.mDisplayFront.getState();
                }
                if (VivoProxCaliService.this.mDisplayBack != null) {
                    VivoProxCaliService vivoProxCaliService2 = VivoProxCaliService.this;
                    vivoProxCaliService2.mBackDisplayState = vivoProxCaliService2.mDisplayBack.getState();
                }
                if (VivoProxCaliService.this.mFrontDisplayState == VivoProxCaliService.this.mLastFrontDisplayState && VivoProxCaliService.this.mBackDisplayState == VivoProxCaliService.this.mLastBackDisplayState) {
                    return;
                }
                VivoProxCaliService vivoProxCaliService3 = VivoProxCaliService.this;
                vivoProxCaliService3.mLastFrontDisplayState = vivoProxCaliService3.mFrontDisplayState;
                VivoProxCaliService vivoProxCaliService4 = VivoProxCaliService.this;
                vivoProxCaliService4.mLastBackDisplayState = vivoProxCaliService4.mBackDisplayState;
                VLog.d(VivoProxCaliService.TAG, "onDisplayChanged display = " + displayId + "; Frontstate = " + VivoProxCaliService.this.mFrontDisplayState);
                VivoProxCaliService.this.mVivoSensorImpl.notifyDisplayState(VivoProxCaliService.this.mFrontDisplayState, VivoProxCaliService.this.mBackDisplayState);
                AblConfig.setDoubleScreenState(VivoProxCaliService.this.mFrontDisplayState, VivoProxCaliService.this.mBackDisplayState, VivoProxCaliService.this.mProximityDisplayId);
            }
        };
        this.mProximityFocusDisplayListener = new MultiDisplayManager.FocusDisplayListener() { // from class: com.vivo.services.proxcali.VivoProxCaliService.8
            public void onFocusDisplayChanged(int displayId) {
                if (AblConfig.isBbkLog()) {
                    VLog.d(VivoProxCaliService.TAG, "onFocusDisplayChanged display=" + displayId);
                }
                VivoProxCaliService.this.mProximityDisplayId = displayId;
                if (VivoProxCaliService.this.mExceptionController != null) {
                    VivoProxCaliService.this.mExceptionController.onFocusDisplayChanged(displayId);
                }
                VivoProxCaliService.this.mVivoSensorImpl.notifyScreenChange(VivoProxCaliService.this.mProximityDisplayId);
                AblConfig.setDoubleScreenState(VivoProxCaliService.this.mFrontDisplayState, VivoProxCaliService.this.mBackDisplayState, VivoProxCaliService.this.mProximityDisplayId);
            }
        };
    }

    public VivoProxCaliService(Context ctx) {
        this.mResetSensorHubCommand = 201;
        this.mLightCounts = 0;
        this.mLightCaliValue = 0.0f;
        this.mLightThreshould = 500.0f;
        this.mSupportSarA_ComPensation_cail = false;
        this.mSupportSarB_ComPensation_cail = false;
        this.mExceptionController = null;
        this.mBrightDayFilter = new IntentFilter(ACTION_DAY_START_CLOCK_TIMER);
        this.mProximitySensor = null;
        this.mAccSensor = null;
        this.mLightSensor = null;
        this.pm = null;
        this.mWakeLock = null;
        this.mStartHandler = null;
        this.mStopHandler = null;
        this.mVivoSensorTestHandler = null;
        this.mProximitySensorListenerVST = null;
        this.mLightSensorListener = null;
        this.mStartCaliRunnable = null;
        this.mStopCaliRunnable = null;
        this.mDeviceParaProvideService = null;
        this.mBootPsCaliReceiver = new BootcompleteReceiverForPsCali();
        this.mScreenOnFilter = new IntentFilter("android.intent.action.SCREEN_ON");
        this.mScreenOffFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
        this.mPhoneFilter = new IntentFilter();
        this.mSarSensorCaliReceiver = new SarSensorCaliReceiver();
        this.mBootCompleteFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
        this.mPowerConnect = new IntentFilter("android.intent.action.ACTION_POWER_CONNECTED");
        this.mPowerDisConnect = new IntentFilter("android.intent.action.ACTION_POWER_DISCONNECTED");
        this.mUserPresent = new IntentFilter("android.intent.action.USER_PRESENT");
        this.mVivoSensorTest = VivoSensorTest.getInstance();
        this.mTestResult = new SensorTestResult();
        this.mTestArg = new int[3];
        this.mSensorDump = null;
        this.mIsCrystalAnim = false;
        this.mDirectCallTime = 0L;
        this.mProiximityDisplayManager = null;
        this.mProximityDisplayId = -1;
        this.mVivoSensorImpl = null;
        this.mFingerIconVisable = false;
        this.mDisplayManager = null;
        this.mFrontDisplayState = 2;
        this.mBackDisplayState = 1;
        this.mLastFrontDisplayState = 2;
        this.mLastBackDisplayState = 1;
        this.mPhoneFlag = 0;
        this.mDisplayFront = null;
        this.mDisplayBack = null;
        this.mVideoPhoneCallId = null;
        this.mVideoState = -1;
        this.mCallOldeState = -1;
        this.mCallNewState = -1;
        this.mVivoSensorOperationUtils = null;
        this.mPhoneStateOffHookTs = -1L;
        this.powerConnectDelay = ProcessList.BACKUP_APP_ADJ;
        this.powerDisConnDelay = ProcessList.BACKUP_APP_ADJ;
        this.UserUnlockDelay = ProcessList.BACKUP_APP_ADJ;
        this.CaliSaronUserUnlock = false;
        this.mSarSensor_A = null;
        this.mSarSensor_B = null;
        this.mDisplayContentListener = new VivoDisplayStateManager.DisplayContentListener() { // from class: com.vivo.services.proxcali.VivoProxCaliService.1
            public void onDisplayContentChanged(int displayId, boolean globalVisible, String module, boolean moduleVisible) {
                if (AblConfig.isBbkLog()) {
                    VLog.d(VivoProxCaliService.TAG, "display :" + displayId + " globalVisible:" + globalVisible + " module:" + module + " moduleVisible:" + moduleVisible);
                }
                if (displayId == 0 && "UDFingerprint".equals(module)) {
                    VivoProxCaliService.this.mFingerIconVisable = moduleVisible;
                    VivoProxCaliService.this.mVivoSensorImpl.notifyFingerIconChange(VivoProxCaliService.this.mFingerIconVisable);
                }
            }

            public void onListenerRegistered(List<VivoDisplayModule> primaryDisplayContent, List<VivoDisplayModule> secondaryDisplayContent) {
            }
        };
        this.mAccSensorListener = new SensorEventListener() { // from class: com.vivo.services.proxcali.VivoProxCaliService.6
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                if (event.values[0] != 100.0f || event.values[1] != 100.0f || event.values[2] != 100.0f || VivoProxCaliService.this.mSensorDialog.isShowing()) {
                    VivoProxCaliService.this.mSensorManager.unregisterListener(VivoProxCaliService.this.mAccSensorListener);
                    return;
                }
                VLog.d(VivoProxCaliService.TAG, " I2C exception x = 100,y = 100,z = 100! ");
                VivoProxCaliService.this.showDialogforI2CException();
                VivoProxCaliService.this.mSensorManager.unregisterListener(VivoProxCaliService.this.mAccSensorListener);
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mVivoDisplayListener = new DisplayManager.DisplayListener() { // from class: com.vivo.services.proxcali.VivoProxCaliService.7
            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayAdded(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayRemoved(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayChanged(int displayId) {
                if (VivoProxCaliService.this.mDisplayFront != null) {
                    VivoProxCaliService vivoProxCaliService = VivoProxCaliService.this;
                    vivoProxCaliService.mFrontDisplayState = vivoProxCaliService.mDisplayFront.getState();
                }
                if (VivoProxCaliService.this.mDisplayBack != null) {
                    VivoProxCaliService vivoProxCaliService2 = VivoProxCaliService.this;
                    vivoProxCaliService2.mBackDisplayState = vivoProxCaliService2.mDisplayBack.getState();
                }
                if (VivoProxCaliService.this.mFrontDisplayState == VivoProxCaliService.this.mLastFrontDisplayState && VivoProxCaliService.this.mBackDisplayState == VivoProxCaliService.this.mLastBackDisplayState) {
                    return;
                }
                VivoProxCaliService vivoProxCaliService3 = VivoProxCaliService.this;
                vivoProxCaliService3.mLastFrontDisplayState = vivoProxCaliService3.mFrontDisplayState;
                VivoProxCaliService vivoProxCaliService4 = VivoProxCaliService.this;
                vivoProxCaliService4.mLastBackDisplayState = vivoProxCaliService4.mBackDisplayState;
                VLog.d(VivoProxCaliService.TAG, "onDisplayChanged display = " + displayId + "; Frontstate = " + VivoProxCaliService.this.mFrontDisplayState);
                VivoProxCaliService.this.mVivoSensorImpl.notifyDisplayState(VivoProxCaliService.this.mFrontDisplayState, VivoProxCaliService.this.mBackDisplayState);
                AblConfig.setDoubleScreenState(VivoProxCaliService.this.mFrontDisplayState, VivoProxCaliService.this.mBackDisplayState, VivoProxCaliService.this.mProximityDisplayId);
            }
        };
        this.mProximityFocusDisplayListener = new MultiDisplayManager.FocusDisplayListener() { // from class: com.vivo.services.proxcali.VivoProxCaliService.8
            public void onFocusDisplayChanged(int displayId) {
                if (AblConfig.isBbkLog()) {
                    VLog.d(VivoProxCaliService.TAG, "onFocusDisplayChanged display=" + displayId);
                }
                VivoProxCaliService.this.mProximityDisplayId = displayId;
                if (VivoProxCaliService.this.mExceptionController != null) {
                    VivoProxCaliService.this.mExceptionController.onFocusDisplayChanged(displayId);
                }
                VivoProxCaliService.this.mVivoSensorImpl.notifyScreenChange(VivoProxCaliService.this.mProximityDisplayId);
                AblConfig.setDoubleScreenState(VivoProxCaliService.this.mFrontDisplayState, VivoProxCaliService.this.mBackDisplayState, VivoProxCaliService.this.mProximityDisplayId);
            }
        };
        this.mContext = ctx;
        HandlerThread handlerThread = new HandlerThread("ProxCaliThread");
        this.mProxCaliThread = handlerThread;
        handlerThread.start();
        VLog.d(TAG, "call VivoProxCaliService constructor.");
        this.mStartHandler = new Handler(this.mProxCaliThread.getLooper());
        this.mStopHandler = new Handler(this.mProxCaliThread.getLooper());
        HandlerThread handlerThread2 = new HandlerThread("VivoSensorTestThread");
        this.mVivoSensortThread = handlerThread2;
        if (handlerThread2 != null) {
            handlerThread2.start();
            this.mVivoSensorTestHandler = new VivoSensorTestHandler(this.mVivoSensortThread.getLooper());
        }
        this.pm = (PowerManager) this.mContext.getSystemService("power");
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mProximitySensor = sensorManager.getDefaultSensor(8);
        this.mAccSensor = this.mSensorManager.getDefaultSensor(1);
        this.mSarSensor_A = this.mSensorManager.getDefaultSensor(66559, true);
        this.mSarSensor_B = this.mSensorManager.getDefaultSensor(66560, true);
        this.mVivoSensorImpl = VivoSensorImpl.getInstance(this.mContext);
        if (AblConfig.supportDoubleScreenOrNot()) {
            MultiDisplayManager multiDisplayManager = (MultiDisplayManager) this.mContext.getSystemService("multidisplay");
            this.mProiximityDisplayManager = multiDisplayManager;
            if (multiDisplayManager != null) {
                multiDisplayManager.registerFocusDisplayListener(this.mProximityFocusDisplayListener);
                this.mProximityDisplayId = this.mProiximityDisplayManager.getFocusedDisplayId();
            }
            this.mVivoSensorImpl.notifyScreenChange(this.mProximityDisplayId);
        }
        if (AblConfig.getStateOfI2cError() == 2) {
            this.mSensorDialog = new SensorErrorDialog(this.mContext, true, "Sensor I2C failed,Please contact shencheng!");
            this.mSensorManager.registerListener(this.mAccSensorListener, this.mAccSensor, 2);
        }
        DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
        this.mDisplayManager = displayManager;
        if (displayManager != null) {
            displayManager.registerDisplayListener(this.mVivoDisplayListener, this.mStartHandler);
            this.mDisplayFront = this.mDisplayManager.getDisplay(0);
            Display display = this.mDisplayManager.getDisplay(4096);
            this.mDisplayBack = display;
            if (display != null) {
                int state = display.getState();
                this.mBackDisplayState = state;
                this.mLastBackDisplayState = state;
            }
            Display display2 = this.mDisplayFront;
            if (display2 != null) {
                int state2 = display2.getState();
                this.mFrontDisplayState = state2;
                this.mLastFrontDisplayState = state2;
            }
            this.mVivoSensorImpl.notifyDisplayState(this.mFrontDisplayState, this.mBackDisplayState);
        }
        if (AblConfig.isUnderProximity()) {
            VivoDisplayStateManager vivoDisplayStateManager = (VivoDisplayStateManager) this.mContext.getSystemService("vivo_display_state");
            this.mVivoDisplayStateManager = vivoDisplayStateManager;
            vivoDisplayStateManager.registerDisplayContentListener(this.mDisplayContentListener);
        }
        Sensor sensor = this.mProximitySensor;
        if (sensor != null) {
            sensor.getName();
        }
        if (AblConfig.getProductSolution() == 2) {
            VLog.d(TAG, "SENSOR_OPERATION_GET_ALS_RAWDATA_MTK");
            this.mResetSensorHubCommand = 17;
        } else {
            this.mResetSensorHubCommand = 201;
        }
        this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
        this.mWakeLock = this.pm.newWakeLock(1, TAG);
        this.mExceptionController = ExceptionController.getInstance(this.mContext);
        this.mSensorDump = new SensorDump(this.mContext);
        this.mProximitySensorListenerVST = new SensorEventListener() { // from class: com.vivo.services.proxcali.VivoProxCaliService.2
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                VLog.d(VivoProxCaliService.TAG, "mProximitySensorListenerVST get ps data" + event.values[0]);
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor2, int accuracy) {
            }
        };
        this.mLightSensorListener = new SensorEventListener() { // from class: com.vivo.services.proxcali.VivoProxCaliService.3
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                if (VivoProxCaliService.this.mLightCounts < 12) {
                    VivoProxCaliService.access$1512(VivoProxCaliService.this, 1);
                    float lightValue = (event.values[0] * 500.0f) / VivoProxCaliService.this.mLightThreshould;
                    VivoProxCaliService.access$1716(VivoProxCaliService.this, lightValue / 12.0f);
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor2, int accuracy) {
            }
        };
        this.mStartCaliRunnable = new Runnable() { // from class: com.vivo.services.proxcali.VivoProxCaliService.4
            @Override // java.lang.Runnable
            public void run() {
                VLog.d(VivoProxCaliService.TAG, "Start calibration");
                if (VivoProxCaliService.mIsDriverProxTempCali) {
                    VivoProxCaliService.this.enabledrivercali(true);
                } else {
                    VivoProxCaliService.this.enableCalibrationUseVST(true);
                }
            }
        };
        this.mStopCaliRunnable = new Runnable() { // from class: com.vivo.services.proxcali.VivoProxCaliService.5
            @Override // java.lang.Runnable
            public void run() {
                VLog.d(VivoProxCaliService.TAG, "Stop calibration");
                if (VivoProxCaliService.mIsDriverProxTempCali) {
                    VivoProxCaliService.this.enabledrivercali(false);
                } else {
                    VivoProxCaliService.this.enableCalibrationUseVST(false);
                }
            }
        };
        this.powerConnectDelay = Integer.parseInt(FtFeature.getFeatureAttribute("vivo.hardware.sensorconfig", "powerConnectDelay", "300"));
        this.powerDisConnDelay = Integer.parseInt(FtFeature.getFeatureAttribute("vivo.hardware.sensorconfig", "powerDisConnDelay", "300"));
        this.UserUnlockDelay = Integer.parseInt(FtFeature.getFeatureAttribute("vivo.hardware.sensorconfig", "UserUnlockDelay", "300"));
        this.CaliSaronUserUnlock = "1".equals(FtFeature.getFeatureAttribute("vivo.hardware.sensorconfig", "CaliSaronUserUnlock", "0"));
        this.mPhoneFilter.addAction(ACTION_PHONE_STATE_CHANGE);
        this.mPhoneFilter.addAction(ACTION_WECHAT_OPEN);
        this.mPhoneFilter.addAction(ACTION_WECHAT_QUIT);
        this.mContext.registerReceiver(this.mBootPsCaliReceiver, this.mScreenOnFilter);
        this.mContext.registerReceiver(this.mBootPsCaliReceiver, this.mScreenOffFilter);
        this.mContext.registerReceiver(this.mBootPsCaliReceiver, this.mPhoneFilter);
        this.mContext.registerReceiver(this.mSarSensorCaliReceiver, this.mPowerConnect);
        this.mContext.registerReceiver(this.mSarSensorCaliReceiver, this.mPowerDisConnect);
        this.mContext.registerReceiver(this.mSarSensorCaliReceiver, this.mBootCompleteFilter);
        this.mContext.registerReceiver(this.mSarSensorCaliReceiver, this.mBrightDayFilter);
        if (this.CaliSaronUserUnlock) {
            this.mContext.registerReceiver(this.mSarSensorCaliReceiver, this.mUserPresent);
        }
        VivoSensorTest vivoSensorTest = this.mVivoSensorTest;
        SensorTestResult sensorTestResult = this.mTestResult;
        int[] iArr = this.mTestArg;
        vivoSensorTest.vivoSensorTest(47, sensorTestResult, iArr, iArr.length);
        VLog.d(TAG, "get temp para " + this.mTestResult.dumpString());
        this.mVivoSensorOperationUtils = VivoSensorOperationUtils.getInstance();
        Message msg1 = Message.obtain();
        msg1.what = 4096;
        this.mVivoSensorTestHandler.sendMessageDelayed(msg1, 4000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showDialogforI2CException() {
        try {
            this.mSensorDialog.show();
        } catch (Exception e) {
            VLog.i(TAG, "Fail to show the mSensorDialog");
        }
    }

    public void startCalibration(int type) {
        if (isUnderProx && !isDoubleScreen) {
            VLog.d(TAG, "underdisplay prox don't need to do calibration");
        } else if (mIsCalibrationing) {
            VLog.d(TAG, "Being Calibrationing, return");
        } else if (this.mProximitySensor == null) {
            VLog.d(TAG, "Proximity sensor is null,return");
        } else if (isNormalProx && type != 2 && type != 0 && !mScreenOnProxTempCali) {
            VLog.d(TAG, "Mikoto Int cali, do not need to do temp cali except phone call or boot complete cali");
        } else {
            long now = System.currentTimeMillis();
            if (type == 2 && now - this.mDirectCallTime < 2000) {
                VLog.d(TAG, "direct calling, not cali. now:" + now + " call:" + this.mDirectCallTime);
            } else if (type == 2 && now - this.mPhoneStateOffHookTs < 5000) {
                VLog.d(TAG, "short after idle, not cali. now:" + now + " call:" + this.mPhoneStateOffHookTs);
            } else {
                VLog.d(TAG, "Start Calibration...., type: " + type);
                mCaliStartBy = type;
                this.mStartHandler.removeCallbacks(this.mStartCaliRunnable);
                this.mStartHandler.post(this.mStartCaliRunnable);
                this.mStopHandler.removeCallbacks(this.mStopCaliRunnable);
                this.mStopHandler.postDelayed(this.mStopCaliRunnable, 10000L);
            }
        }
    }

    public void changeProximityParam(boolean change, int state) {
        if (mChangeProxParam) {
            SensorTestResult mTempRes = new SensorTestResult();
            int[] mTempTestArg = new int[3];
            mTempTestArg[0] = 9;
            if (change) {
                mTempTestArg[1] = 1;
            } else {
                mTempTestArg[1] = 0;
            }
            mTempTestArg[2] = state;
            if (this.mVivoSensorTest != null) {
                VLog.d(TAG, "proximity status = " + change + " ps_para: data[0]" + mTempTestArg[0] + " data[1]=" + mTempTestArg[1] + "data[2]=" + mTempTestArg[2]);
                this.mVivoSensorTest.vivoSensorTest(45, mTempRes, mTempTestArg, mTempTestArg.length);
            }
            this.mExceptionController.notifyChangeProximityParam(0, change);
            VLog.d(TAG, "0ops change param in proxcaliservice");
        }
    }

    public void onDirectCall(long timestamp) {
        this.mDirectCallTime = timestamp;
    }

    public void setCrystalAnimStatus(boolean isCrystalAnim) {
        VLog.d(TAG, "set crystal animation : " + isCrystalAnim);
        this.mIsCrystalAnim = isCrystalAnim;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getPhoneCallState() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        if (tm == null) {
            VLog.e(TAG, "TelephonyManager is Null");
            return 0;
        }
        VLog.d(TAG, "getCallState: " + tm.getCallState());
        return tm.getCallState();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlerOnBootVivoSensorTestEvent() {
        startCalibration(0);
        Sensor sensor = this.mProximitySensor;
        if (sensor != null) {
            sensor.getName();
        }
        VLog.d(TAG, "startCalibration type 0 when bootcompleted...");
        String prop = SystemProperties.get("ro.product.model.bbk", (String) null);
        int psCaliFlag = SystemProperties.getInt(PS_CALI_FLAG, 0);
        SensorTestResult mTempRes = new SensorTestResult();
        int[] mTempTestArg = new int[3];
        if (psCaliFlag == 1 && prop != null && prop.toLowerCase().startsWith("pd1913")) {
            int offsetFlag = SystemProperties.getInt(PS_CALI_OFFSET_FLAG, 0);
            if (offsetFlag == 1 || offsetFlag == 2) {
                mTempTestArg[0] = 24;
                mTempTestArg[1] = offsetFlag;
                mTempTestArg[2] = 0;
                VivoSensorTest vivoSensorTest = this.mVivoSensorTest;
                if (vivoSensorTest != null) {
                    vivoSensorTest.vivoSensorTest((int) PS_SET_CALI_OFFSET_DATA, mTempRes, mTempTestArg, mTempTestArg.length);
                    VLog.d(TAG, "proximity cali_off_data: data[0]" + mTempTestArg[0] + " offsetFlag " + mTempTestArg[1]);
                    return;
                }
                return;
            }
            VLog.d(TAG, "proximity no need to send cali type to driver");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlerSarA_Compensation_cali() {
        int[] mTempTestArg = {0, 0, 0};
        SensorTestResult result = new SensorTestResult();
        if (this.mVivoSensorTest != null) {
            if (AblConfig.isBbkLog()) {
                VLog.d(TAG, "handlerSarA_Compensation_cali start");
            }
            this.mVivoSensorTest.vivoSensorTest((int) SAR_A_COMPENSATION_CALL, result, mTempTestArg, mTempTestArg.length);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlerSarB_Compensation_cali() {
        int[] mTempTestArg = {0, 0, 0};
        SensorTestResult result = new SensorTestResult();
        if (this.mVivoSensorTest != null) {
            if (AblConfig.isBbkLog()) {
                VLog.d(TAG, "handlerSarB_Compensation_cali start");
            }
            this.mVivoSensorTest.vivoSensorTest((int) SAR_B_COMPENSATION_CALL, result, mTempTestArg, mTempTestArg.length);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCreateAccFile() {
        int[] mTempTestArg = {0, 0, 0};
        String caliOffsetDataStr = SystemProperties.get(PROP_ACC_CALI_DATA, "unknown");
        if (caliOffsetDataStr != null && !caliOffsetDataStr.equals("unknown")) {
            String[] strs = caliOffsetDataStr.split(",");
            if (strs.length == 3) {
                mTempTestArg[0] = Integer.parseInt(strs[0].trim());
                mTempTestArg[1] = Integer.parseInt(strs[1].trim());
                mTempTestArg[2] = Integer.parseInt(strs[2].trim());
            }
        }
        SensorTestResult result = new SensorTestResult();
        if (this.mVivoSensorTest != null) {
            if (AblConfig.isBbkLog()) {
                VLog.d(TAG, "handle create acc file on boot, data_x: " + mTempTestArg[0] + ", data_y: " + mTempTestArg[1] + ", data_z: " + mTempTestArg[2]);
            }
            this.mVivoSensorTest.vivoSensorTest((int) EM_ACC_BIAS_FILE_CREATE, result, mTempTestArg, mTempTestArg.length);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlerVivoSensorTestOnScreenOnEvent() {
        if (this.mIsCrystalAnim || getPhoneCallState() != 0) {
            VLog.d(TAG, "use proximity lock or cystal animation in screen on,return");
            return;
        }
        int[] mTempTestArg = {0, 0, 0};
        SensorTestResult result = new SensorTestResult();
        VivoSensorTest vivoSensorTest = this.mVivoSensorTest;
        if (vivoSensorTest != null) {
            vivoSensorTest.vivoSensorTest(513, result, mTempTestArg, mTempTestArg.length);
        }
        if (result.mSuccess == 1) {
            VLog.d(TAG, "startCalibration when screen on...");
            startCalibration(1);
            return;
        }
        VLog.d(TAG, "not configured ps tolerance,not do calibration when screen on...");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean notifyProxmitySwitchThres(int thresState) {
        if (this.mProximitySensor == null) {
            return false;
        }
        VivoSensorOperationResult operationRes = new VivoSensorOperationResult();
        int[] mOperationArgs = {200, thresState};
        VivoSensorOperationUtils vivoSensorOperationUtils = this.mVivoSensorOperationUtils;
        if (vivoSensorOperationUtils != null) {
            try {
                vivoSensorOperationUtils.executeCommand(mOperationArgs[0], operationRes, mOperationArgs, mOperationArgs.length);
            } catch (Exception e) {
                VLog.i(TAG, "Fail to notify the sensors");
            }
        }
        VLog.i(TAG, "notifyProxmitySwitchThres, thresState = " + thresState);
        return true;
    }

    /* loaded from: classes.dex */
    private final class BootcompleteReceiverForPsCali extends BroadcastReceiver {
        private BootcompleteReceiverForPsCali() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            VLog.d(VivoProxCaliService.TAG, "receive action = " + action);
            if (action.equals("android.intent.action.SCREEN_ON")) {
                VivoProxCaliService.this.mVivoSensorTestHandler.removeMessages(4097);
                VivoProxCaliService.this.mVivoSensorTestHandler.sendEmptyMessage(4097);
                if (AblConfig.getStateOfI2cError() == 2) {
                    VivoProxCaliService.this.mSensorManager.registerListener(VivoProxCaliService.this.mAccSensorListener, VivoProxCaliService.this.mAccSensor, 2);
                }
            } else if (action.equals("android.intent.action.SCREEN_OFF") || VivoProxCaliService.this.mIsCrystalAnim) {
                if (!VivoProxCaliService.mNeedStopCali) {
                    VLog.d(VivoProxCaliService.TAG, "Stop calibration coz screen off broadcast");
                    VivoProxCaliService.this.mStopHandler.removeCallbacks(VivoProxCaliService.this.mStopCaliRunnable);
                    VivoProxCaliService.this.mStopHandler.post(VivoProxCaliService.this.mStopCaliRunnable);
                }
            } else if (action.equals(VivoProxCaliService.ACTION_PHONE_STATE_CHANGE)) {
                VivoProxCaliService.this.mVivoSensorTestHandler.removeMessages(VivoProxCaliService.MSG_VIVONOTIFY_PHONESTATE);
                VivoProxCaliService.this.mVivoSensorTestHandler.sendEmptyMessage(VivoProxCaliService.MSG_VIVONOTIFY_PHONESTATE);
            } else if (action.equals(VivoProxCaliService.ACTION_WECHAT_OPEN) && !AblConfig.isUnderProximity()) {
                VivoProxCaliService.this.mVivoSensorTestHandler.removeMessages(VivoProxCaliService.MSG_VIVONOTIFY_WECHAT_OPEN);
                VivoProxCaliService.this.mVivoSensorTestHandler.sendEmptyMessage(VivoProxCaliService.MSG_VIVONOTIFY_WECHAT_OPEN);
            } else if (action.equals(VivoProxCaliService.ACTION_WECHAT_QUIT) && !AblConfig.isUnderProximity()) {
                VivoProxCaliService.this.mVivoSensorTestHandler.removeMessages(VivoProxCaliService.MSG_VIVONOTIFY_WECHAT_QUIT);
                VivoProxCaliService.this.mVivoSensorTestHandler.sendEmptyMessage(VivoProxCaliService.MSG_VIVONOTIFY_WECHAT_QUIT);
            }
        }
    }

    /* loaded from: classes.dex */
    private final class SarSensorCaliReceiver extends BroadcastReceiver {
        private SarSensorCaliReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            int delaytime = 0;
            String action = intent.getAction();
            VLog.d(VivoProxCaliService.TAG, "sar sensor receive action = " + action);
            if (action.equals("android.intent.action.ACTION_POWER_CONNECTED") || action.equals("android.intent.action.ACTION_POWER_DISCONNECTED") || action.equals("android.intent.action.USER_PRESENT") || action.equals("android.intent.action.BOOT_COMPLETED")) {
                if (action.equals("android.intent.action.ACTION_POWER_CONNECTED")) {
                    delaytime = VivoProxCaliService.this.powerConnectDelay;
                }
                if (action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
                    delaytime = VivoProxCaliService.this.powerDisConnDelay;
                }
                if (action.equals("android.intent.action.USER_PRESENT")) {
                    delaytime = VivoProxCaliService.this.UserUnlockDelay;
                    if (AblConfig.engineModeForbidSarCali()) {
                        try {
                            String ForbidSarCali = SystemProperties.get("sys.vivo.sar_tested", "0");
                            if (Integer.valueOf(ForbidSarCali).intValue() == 1) {
                                VLog.d(VivoProxCaliService.TAG, "Forbid Sar Calibration!");
                                return;
                            }
                        } catch (Exception e) {
                            VLog.e(VivoProxCaliService.TAG, "errors: " + e);
                            e.printStackTrace();
                        }
                    }
                }
                if (VivoProxCaliService.this.mSarSensor_A != null || VivoProxCaliService.this.mSarSensor_B != null) {
                    VivoProxCaliService.this.mVivoSensorTestHandler.removeMessages(VivoProxCaliService.MSG_VIVOSENSORTEST_ONPOWERCONNECT_CHANGE_EVENT);
                    VivoProxCaliService.this.mVivoSensorTestHandler.sendEmptyMessageDelayed(VivoProxCaliService.MSG_VIVOSENSORTEST_ONPOWERCONNECT_CHANGE_EVENT, delaytime);
                }
                if (VivoProxCaliService.mIsSupportCreateAccFile) {
                    VivoProxCaliService.this.mVivoSensorTestHandler.removeMessages(VivoProxCaliService.MSG_CREATE_ACC_FILE_ON_BOOT);
                    VivoProxCaliService.this.mVivoSensorTestHandler.sendEmptyMessage(VivoProxCaliService.MSG_CREATE_ACC_FILE_ON_BOOT);
                }
            } else if (action.equals(VivoProxCaliService.ACTION_DAY_START_CLOCK_TIMER)) {
                VivoProxCaliService.this.camera_record();
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        String enableDump = SystemProperties.get(PROP_SENSOR_DUMP_KEY, SENSOR_DUMP_DISABLED);
        if (SENSOR_DUMP_ENABLED.contentEquals(enableDump)) {
            SensorDump sensorDump = this.mSensorDump;
            if (sensorDump != null) {
                sensorDump.handleCommand(fd, pw, args);
                return;
            } else {
                pw.println("Error__NULL");
                return;
            }
        }
        pw.print("Permission Denial, android.permission.DUMP\r\n");
    }
}