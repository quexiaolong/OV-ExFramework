package com.vivo.services.sarpower;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.media.AudioFeatures;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Display;
import com.vivo.face.common.data.Constants;
import com.vivo.sensor.autobrightness.config.AblConfig;
import com.vivo.sensor.autobrightness.utils.SElog;
import com.vivo.sensor.sarpower.VivoSarPowerStateController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import vivo.app.sarpower.IVivoSarPowerState;

/* loaded from: classes.dex */
public class VivoSarPowerStateService extends IVivoSarPowerState.Stub {
    private static final String ACTION_BOOT_COMPLETE = "android.intent.action.BOOT_COMPLETED";
    private static final String ACTION_CARD_CHANGE = "android.intent.action.SIM_STATE_CHANGED";
    private static final String ACTION_HEADSET_PLUG_CHANEGD = "android.intent.action.HEADSET_PLUG";
    private static final String ACTION_SAR_POWER_TEST = "android.intent.action.sar_test";
    private static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
    private static final String ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON";
    private static final String ACTION_SERVICE_STATE_CHANGED = "android.intent.action.SERVICE_STATE";
    private static final String ACTION_SETSARINFO_OFF = "android.intent.action.SETSARINFO_OFF";
    private static final String ACTION_SETSARINFO_ON = "android.intent.action.SETSARINFO_ON";
    private static final int AUDIO = 3;
    public static final int AUDIO_STATE_OFF_FAR = 5;
    public static final int AUDIO_STATE_ON_NEAR = 0;
    private static final int BOARD_VERSION_VALUE = 30;
    public static final int DOUBLE_SCREEN_BOTH_OFF = 3;
    public static final int DOUBLE_SCREEN_BOTH_ON = 5;
    public static final int DOUBLE_SCREEN_FRONT_OFF_BACK_ON = 6;
    public static final int DOUBLE_SCREEN_FRONT_ON_BACK_OFF = 4;
    private static final String DUMP_MAGIC_WORD = "vivo_sar_dump";
    private static final int ENDC = 13;
    public static final int ENDC_STATE_OFF = 0;
    public static final int ENDC_STATE_ON = 1;
    private static final int HEADSET_PLUG_STATE = 33;
    private static final int HOTSPOT = 14;
    private static final int MSG_ENABLE_SENSOR = 1;
    private static final int MSG_NOTIFY_SERVICE_STATE_CHANGED = 2;
    protected static final int MSG_SAR_BOOT_COMPLETED = 1;
    protected static final int MSG_SAR_CAMERA_ISSUE_DELAY = 3;
    protected static final int MSG_SAR_CAMERA_ISSUE_START = 4;
    private static final int MSG_SAR_EVENT_UPDATE_CARD = 0;
    private static final int MSG_SAR_EVENT_UPDATE_NETWORK_FREQUENCY = 4;
    private static final int MSG_SAR_EVENT_UPDATE_NETWORK_STATE = 3;
    private static final int MSG_SAR_EVENT_UPDATE_WIFI_FREQUENCY = 5;
    protected static final int MSG_SAR_POWER_CHANGE = 0;
    protected static final int MSG_SAR_UPDATE_PARAM = 2;
    public static final int NR_STATE_CONNECTED = 3;
    public static final int NR_STATE_DEFAULT = -1;
    private static final int PLATFORM_MTK = 2;
    private static final int PLATFORM_NEW_MTK = 3;
    private static final int PLATFORM_QCOM = 1;
    private static final int PLATFORM_SAMSUNG = 4;
    private static final int PLATFORM_UNKNOWN = 0;
    private static final int PROMIMITY = 0;
    private static final int PROMIMITY_BACK = 2;
    private static final int PROMIMITY_FRONT = 1;
    public static final int RIL_STATE_5G = 20;
    private static final int SAR_A = 4;
    private static final int SAR_A_CS0 = 5;
    private static final int SAR_A_CS1 = 6;
    private static final int SAR_A_CS2 = 7;
    private static final int SAR_B = 8;
    private static final int SAR_B_CS0 = 9;
    private static final int SAR_B_CS1 = 10;
    private static final int SAR_B_CS2 = 11;
    private static final String SAR_POWER_STATE_TEST_PARMNAME = "powerState";
    private static final String SAR_POWER_UEVENT_MATCH = "DEVPATH=/devices/virtual/sarpower/sar-power";
    private static final int SAR_REGISTER_FAILED_VALUE = 31;
    private static final int SAR_SENSOR_EXIST = 32;
    public static final int SAR_STATE_FAR = 5;
    public static final int SAR_STATE_NEAR = 0;
    public static final int SCREEN_STATE_OFF = 0;
    public static final int SCREEN_STATE_ON = 1;
    private static final int SENSORTYPE_PRXOIMITYBACK = 66550;
    private static final int SENSORTYPE_PRXOIMITYFRONT = 66558;
    private static final int SETSARINFO = 88;
    private static final int SIM_CARD_ONE_ID = 0;
    private static final int SIM_CARD_TWO_ID = 1;
    private static final String TAG = "SarPowerStateService";
    private static final int VIVO_MAIN_SCREEN_ID = 0;
    private static final int VIVO_SECOND_SCREEN_ID = 4096;
    private static final int WIFI = 12;
    public static final int WIFI_STATE_OFF = 0;
    public static final int WIFI_STATE_OFF_AIRPLAY_MODE = 3;
    public static final int WIFI_STATE_ON = 1;
    public static final int WIFI_STATE_ON_AIRPLAY_MODE = 2;
    private static Context mContext = null;
    private static final int mSarRegisterFailedValue = -1;
    private static final int mSarSensorExist = 1;
    private static HandlerThread mThread;
    private String ARG;
    private AudioFeatureCallback mAudioFeatureCallback;
    private AudioFeatures mAudioFeatures;
    private Display mBackDisplay;
    private int mBackDisplayState;
    private ContentResolver mContentResolver;
    private DisplayManager mDisplayManager;
    private Display mFrontDisplay;
    private int mFrontDisplayState;
    private int mHOTSPOTState;
    private int mHeadsetPlugState;
    private int mLastBackProximityState;
    private int mLastEnDcState;
    private int mLastFrontProximityState;
    private Looper mMainLooper;
    private int mPhoneCardId;
    private int mPhoneReceverState;
    private Sensor mProximityBackSensor;
    private SensorEventListener mProximityBackSensorListener;
    private Sensor mProximityFrontSensor;
    private SensorEventListener mProximityFrontSensorListener;
    private Sensor mProximitySensor;
    private SensorEventListener mProximitySensorListener;
    private SarPowerObserver mSarPowerObserver;
    private final BroadcastReceiver mSarPowerTestReceiver;
    private SensorEventListener mSarSensorListener_A;
    private SensorEventListener mSarSensorListener_B;
    private Sensor mSarSensor_A;
    private Sensor mSarSensor_B;
    private SensorManager mSensorManager;
    private final BroadcastReceiver mStateChangeReceiver;
    private int mTargetPlatformInfo;
    private TelephonyManager mTelephonyManager;
    private DisplayManager.DisplayListener mVivoDisplayListener;
    private VivoGsmStateListener mVivoGsmStateListener;
    private VivoSarPowerHandler mVivoSarPowerHandler;
    private VivoSarPowerStateController mVivoSarPowerStateController;
    private VivoWifiFrequencyController mVivoWifiFrequencyController;
    private int mWIFIState;
    private static final boolean SUPPORT_SAR_POWER = SystemProperties.get("persist.vivo.phone.sarpower", "no").equals("Have_sarpower");
    private static final String platform = SystemProperties.get("ro.vivo.product.solution", "unkown").toLowerCase();
    private static final String model = SystemProperties.get("ro.vivo.product.model", "unkown").toLowerCase();
    private static final boolean eu = SystemProperties.get("persist.vivo.sar.aera", "unkown").toLowerCase().equals("eu");
    private static boolean mtk_support_dsi_command = false;

    private static native int nativeHandleSarPowerEnable(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeInitSarPowerState();

    /* loaded from: classes.dex */
    private class SarPowerObserver extends UEventObserver {
        private static final String TAG = "SarPowerStateService";

        public SarPowerObserver() {
            startObserving(VivoSarPowerStateService.SAR_POWER_UEVENT_MATCH);
            VivoSarPowerStateService.this.sarPowerSwitchEnable(1);
            int mSarPowerRfDetectState = VivoSarPowerStateService.nativeInitSarPowerState();
            SElog.d(TAG, "init mSarPowerRfDetectState = " + mSarPowerRfDetectState);
            VivoSarPowerStateService.this.mVivoSarPowerStateController.notifySarPowerRfDetectState(mSarPowerRfDetectState);
        }

        public void onUEvent(UEventObserver.UEvent event) {
            try {
                int mSarPowerRfDetectState = Integer.parseInt(event.get("SWITCH_STATE"));
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifySarPowerRfDetectState(mSarPowerRfDetectState);
                VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                SElog.d(TAG, "onUEvent mSarPowerRfDetectState = " + mSarPowerRfDetectState);
            } catch (NumberFormatException e) {
                SElog.d(TAG, "onUEvent e:" + e);
            }
        }
    }

    /* loaded from: classes.dex */
    private class AudioFeatureCallback extends AudioFeatures.AudioFeatureCallback {
        AudioFeatureCallback(Context context, String arg0, Object obj) {
            super(context, arg0, obj);
        }

        public String onCallback(String ReceiveAudioState, Object obj) {
            SElog.d(VivoSarPowerStateService.TAG, "onCallback: " + ReceiveAudioState);
            String state = ReceiveAudioState.substring(24, 25);
            String device = ReceiveAudioState.substring(33, 34);
            if (!state.equals("1") || !device.equals("1")) {
                VivoSarPowerStateService.this.mPhoneReceverState = 0;
            } else {
                VivoSarPowerStateService.this.mPhoneReceverState = 1;
            }
            VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(VivoSarPowerStateService.this.mPhoneReceverState, 3);
            VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
            return null;
        }
    }

    /* loaded from: classes.dex */
    public final class VivoSarPowerHandler extends Handler {
        public VivoSarPowerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 0) {
                if (i == 1) {
                    VivoSarPowerStateService.this.registerSarSensors();
                    return;
                } else if (i == 2) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyServiceStateChanged();
                    return;
                } else if (i == 3) {
                    VivoSarPowerStateService.this.getNetworkFrequencyNSA();
                    return;
                } else if (i == 4) {
                    VivoSarPowerStateService.this.getNetworkFrequencyNSA();
                    return;
                } else if (i == 5) {
                    VivoSarPowerStateService.this.getWifiFrequency();
                    return;
                } else {
                    SElog.d(VivoSarPowerStateService.TAG, "VivoSarPowerHandler default, msg:" + msg.what);
                    return;
                }
            }
            Bundle bundle = msg.getData();
            int slotNum = bundle.getInt("slotNum");
            String iccStateExtra = bundle.getString("iccState");
            SElog.d(VivoSarPowerStateService.TAG, "VivoSarPowerHandler: slotNum " + slotNum + ",icc:" + iccStateExtra);
            if (slotNum == 0) {
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyCardInsert(true, VivoSarPowerStateService.this.mTelephonyManager.hasIccCard(slotNum));
            } else if (1 == slotNum) {
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyCardInsert(false, VivoSarPowerStateService.this.mTelephonyManager.hasIccCard(slotNum));
            }
            if ("LOADED".equals(iccStateExtra)) {
                VivoSarPowerStateService.this.mPhoneCardId = slotNum;
            }
            if ((VivoSarPowerStateService.this.mTargetPlatformInfo != 1 && VivoSarPowerStateService.this.mTargetPlatformInfo != 4) || !"LOADED".equals(iccStateExtra)) {
                if (VivoSarPowerStateService.this.mTargetPlatformInfo != 2 || !"LOADED".equals(iccStateExtra)) {
                    if (!VivoSarPowerStateService.this.mTelephonyManager.hasIccCard(slotNum)) {
                        if (slotNum == 0) {
                            VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyCardState(true, 0);
                        } else if (1 == slotNum) {
                            VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyCardState(false, 0);
                        }
                    }
                } else {
                    String iccid = VivoSarPowerStateService.this.getSimIccid(slotNum);
                    boolean isWhiteCardInsert = VivoSarPowerStateService.this.isWhiteCard(slotNum) && !VivoSarPowerStateService.isVSim(iccid);
                    if (isWhiteCardInsert) {
                        if (slotNum == 0) {
                            VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyCardState(true, 1);
                        } else if (1 == slotNum) {
                            VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyCardState(false, 1);
                        }
                    } else if (slotNum == 0) {
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyCardState(true, 0);
                    } else if (1 == slotNum) {
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyCardState(false, 0);
                    }
                }
            } else {
                boolean isWhiteCardInsert2 = VivoSarPowerStateService.this.isWhiteCard(slotNum) && !VivoSarPowerStateService.this.isVSim(slotNum);
                if (isWhiteCardInsert2) {
                    if (slotNum == 0) {
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyCardState(true, 1);
                    } else if (1 == slotNum) {
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyCardState(false, 1);
                    }
                } else if (slotNum == 0) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyCardState(true, 0);
                } else if (1 == slotNum) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyCardState(false, 0);
                }
            }
            if ("LOADED".equals(iccStateExtra) && VivoSarPowerStateService.this.mTargetPlatformInfo == 2) {
                SElog.d(VivoSarPowerStateService.TAG, "notifyForceUpdateState");
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyForceUpdateState();
            }
            VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 500);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerSarSensors() {
        VivoWifiFrequencyController vivoWifiFrequencyController;
        this.mVivoSarPowerStateController.registerResource();
        if (this.mVivoSarPowerStateController.isSupportWifiFrequency() && (vivoWifiFrequencyController = this.mVivoWifiFrequencyController) != null) {
            vivoWifiFrequencyController.registerSoftAp();
        }
    }

    private boolean isAirPlaneModeOn() {
        int mode = 0;
        try {
            mode = Settings.Global.getInt(mContext.getContentResolver(), "airplane_mode_on");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mode == 1;
    }

    public void registerSarSensorSarA() {
        Sensor defaultSensor = this.mSensorManager.getDefaultSensor(66559, true);
        this.mSarSensor_A = defaultSensor;
        if (defaultSensor != null) {
            SElog.d(TAG, "register SarSensor_A");
            this.mSensorManager.registerListener(this.mSarSensorListener_A, this.mSarSensor_A, 3);
            this.mVivoSarPowerStateController.notifyStateChange(1, 32);
            return;
        }
        SElog.d(TAG, "SarSensor_A == null");
        this.mVivoSarPowerStateController.notifyStateChange(-1, 31);
        this.mVivoSarPowerStateController.handleSarMessage(0, 0);
    }

    public void registerSarSensorSarB() {
        Sensor defaultSensor = this.mSensorManager.getDefaultSensor(66560, true);
        this.mSarSensor_B = defaultSensor;
        if (defaultSensor != null) {
            SElog.d(TAG, "register SarSensor_B");
            this.mSensorManager.registerListener(this.mSarSensorListener_B, this.mSarSensor_B, 3);
            this.mVivoSarPowerStateController.notifyStateChange(1, 32);
            return;
        }
        SElog.d(TAG, "SarSensor_B == null");
        this.mVivoSarPowerStateController.notifyStateChange(-1, 31);
        this.mVivoSarPowerStateController.handleSarMessage(0, 0);
    }

    public void registerSarSensor() {
        this.mSarSensor_A = this.mSensorManager.getDefaultSensor(66559, true);
        this.mSarSensor_B = this.mSensorManager.getDefaultSensor(66560, true);
        if (this.mSarSensor_A != null) {
            SElog.d(TAG, "register SarSensor_A");
            this.mSensorManager.registerListener(this.mSarSensorListener_A, this.mSarSensor_A, 3);
            this.mVivoSarPowerStateController.notifyStateChange(1, 32);
        } else {
            SElog.d(TAG, "SarSensor_A == null");
            this.mVivoSarPowerStateController.notifyStateChange(-1, 31);
            this.mVivoSarPowerStateController.handleSarMessage(0, 0);
        }
        if (this.mSarSensor_B != null) {
            SElog.d(TAG, "register SarSensor_B");
            this.mSensorManager.registerListener(this.mSarSensorListener_B, this.mSarSensor_B, 3);
            this.mVivoSarPowerStateController.notifyStateChange(1, 32);
            return;
        }
        SElog.d(TAG, "SarSensor_B == null");
        this.mVivoSarPowerStateController.notifyStateChange(-1, 31);
        this.mVivoSarPowerStateController.handleSarMessage(0, 0);
    }

    void unregisterSarSensor() {
        if (this.mSarSensor_A != null) {
            SElog.d(TAG, "unregister SarSensor_A");
            this.mSensorManager.unregisterListener(this.mSarSensorListener_A);
        }
        if (this.mSarSensor_B != null) {
            SElog.d(TAG, "unregister SarSensor_B");
            this.mSensorManager.unregisterListener(this.mSarSensorListener_B);
        }
    }

    public void setAudioCallBack() {
        try {
            this.mAudioFeatureCallback = new AudioFeatureCallback(mContext, this.ARG, new Object());
            AudioFeatures audioFeatures = new AudioFeatures(mContext, this.ARG, (Object) null);
            this.mAudioFeatures = audioFeatures;
            audioFeatures.registerAudioFeatureCallback(this.mAudioFeatureCallback, this.ARG, (Object) null);
            SElog.d(TAG, "mAudioFeatures registerAudioFeatureCallback successed!");
        } catch (Exception e) {
            SElog.e(TAG, "construct error--Audio!");
        }
    }

    public void registerDoubleScreenProx() {
        this.mProximityFrontSensor = this.mSensorManager.getDefaultSensor(SENSORTYPE_PRXOIMITYFRONT);
        this.mProximityBackSensor = this.mSensorManager.getDefaultSensor(66550);
        Sensor sensor = this.mProximityFrontSensor;
        if (sensor != null) {
            this.mSensorManager.registerListener(this.mProximityFrontSensorListener, sensor, 3);
        }
        Sensor sensor2 = this.mProximityBackSensor;
        if (sensor2 != null) {
            this.mSensorManager.registerListener(this.mProximityBackSensorListener, sensor2, 3);
        }
        SElog.i(TAG, "registerDoubleScreenProx");
    }

    public void registerProximity() {
        this.mSensorManager.registerListener(this.mProximitySensorListener, this.mProximitySensor, 3);
        SElog.i(TAG, "registerProximity");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getSimIccid(int slotNum) {
        ArrayList<SubscriptionInfo> activeSubInfoList = (ArrayList) SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
        String[] strArr = {Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK};
        if (activeSubInfoList == null) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        Iterator<SubscriptionInfo> it = activeSubInfoList.iterator();
        while (it.hasNext()) {
            SubscriptionInfo subInfo = it.next();
            if (subInfo != null && subInfo.getSimSlotIndex() == slotNum) {
                String iccid = subInfo.getIccId();
                return iccid;
            }
        }
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isVSim(String iccid) {
        if (iccid == null) {
            return false;
        }
        if (!iccid.startsWith("89860000000000000001") && !iccid.startsWith("89886920556000843550") && !iccid.startsWith("89862320100000000131")) {
            return false;
        }
        SElog.d(TAG, "Is virtul S");
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isVSim(int slotNum) {
        if (slotNum == 0 && virtualSIMFlagIsTrue()) {
            SElog.d(TAG, "Is virtul");
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isWhiteCard(int slotNum) {
        if (SubscriptionManager.getSubId(slotNum) != null) {
            int subid = SubscriptionManager.getSubId(slotNum)[0];
            String imsi = this.mTelephonyManager.createForSubscriptionId(subid).getSubscriberId();
            if (TextUtils.isEmpty(imsi)) {
                return false;
            }
            if (!TextUtils.isEmpty(imsi) && isTestImsi(imsi)) {
                SElog.d(TAG, "is white S" + slotNum);
                return true;
            }
            SElog.d(TAG, "is not white S" + slotNum);
            return false;
        }
        SElog.d(TAG, "Sub is null");
        return false;
    }

    private static boolean isTestImsi(String imsi) {
        return imsi.startsWith("44201") || imsi.startsWith("46099") || imsi.startsWith("001") || imsi.startsWith("002") || imsi.startsWith("003") || imsi.startsWith("004") || imsi.startsWith("005") || imsi.startsWith("006") || imsi.startsWith("007") || imsi.startsWith("008") || imsi.startsWith("009") || imsi.startsWith("010") || imsi.startsWith("011") || imsi.startsWith("012");
    }

    private boolean virtualSIMFlagIsTrue() {
        try {
            int readFlag = SystemProperties.getInt("sys.vivo.factory.virtualsim", 9);
            SElog.d(TAG, "flag = " + readFlag);
            return readFlag == 1;
        } catch (Exception e) {
            SElog.e(TAG, "vs judge throws exception");
            e.printStackTrace();
            return false;
        }
    }

    public VivoSarPowerStateService() {
        this.mProximitySensor = null;
        this.mProximityFrontSensor = null;
        this.mProximityBackSensor = null;
        this.mSarSensor_A = null;
        this.mSarSensor_B = null;
        this.ARG = "ReceiveAudioState";
        this.mPhoneReceverState = -1;
        this.mVivoSarPowerStateController = null;
        this.mVivoGsmStateListener = null;
        this.mVivoWifiFrequencyController = null;
        this.mTargetPlatformInfo = 0;
        this.mDisplayManager = null;
        this.mFrontDisplay = null;
        this.mBackDisplay = null;
        this.mFrontDisplayState = 0;
        this.mBackDisplayState = 0;
        this.mLastBackProximityState = -1;
        this.mLastFrontProximityState = -1;
        this.mLastEnDcState = -1;
        this.mContentResolver = null;
        this.mWIFIState = 0;
        this.mHOTSPOTState = 0;
        this.mHeadsetPlugState = 0;
        this.mPhoneCardId = -1;
        this.mProximitySensorListener = new SensorEventListener() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.1
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                int proximityState = (int) event.values[0];
                SElog.d(VivoSarPowerStateService.TAG, "prox event: " + proximityState);
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(proximityState, 0);
                VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mSarSensorListener_A = new SensorEventListener() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.2
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                int sarAState = (int) event.values[0];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarAState, 4);
                int sarAState_CS0 = (int) event.values[1];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarAState_CS0, 5);
                int sarAState_CS1 = (int) event.values[2];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarAState_CS1, 6);
                int sarAState_CS2 = (int) event.values[3];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarAState_CS2, 7);
                SElog.d(VivoSarPowerStateService.TAG, "sar_A event: " + sarAState + " " + sarAState_CS0 + " " + sarAState_CS1 + " " + sarAState_CS2);
                VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mSarSensorListener_B = new SensorEventListener() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.3
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                int sarBState = (int) event.values[0];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarBState, 8);
                int sarBState_CS0 = (int) event.values[1];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarBState_CS0, 9);
                int sarBState_CS1 = (int) event.values[2];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarBState_CS1, 10);
                int sarBState_CS2 = (int) event.values[3];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarBState_CS2, 11);
                SElog.d(VivoSarPowerStateService.TAG, "sar_B event: " + sarBState + " " + sarBState_CS0 + " " + sarBState_CS1 + " " + sarBState_CS2);
                VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mProximityFrontSensorListener = new SensorEventListener() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.4
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                int frontproximitystate = (int) event.values[0];
                if (VivoSarPowerStateService.this.mLastFrontProximityState != frontproximitystate) {
                    VivoSarPowerStateService.this.mLastFrontProximityState = frontproximitystate;
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(frontproximitystate, 1);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mProximityBackSensorListener = new SensorEventListener() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.5
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                int backproximitystate = (int) event.values[0];
                if (backproximitystate != VivoSarPowerStateService.this.mLastBackProximityState) {
                    VivoSarPowerStateService.this.mLastBackProximityState = backproximitystate;
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(backproximitystate, 2);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mStateChangeReceiver = new BroadcastReceiver() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.6
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                SElog.d(VivoSarPowerStateService.TAG, "SAR mStateChangeReceiver action:" + action);
                if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyForceUpdateState();
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                } else if (VivoSarPowerStateService.ACTION_CARD_CHANGE.equals(action)) {
                    int slotNum = intent.getIntExtra("phone", -1);
                    String iccStateExtra = intent.getStringExtra("ss");
                    SElog.d(VivoSarPowerStateService.TAG, "mCardChangeReceiver: slotNum " + slotNum + ",icc:" + iccStateExtra);
                    Message msg = VivoSarPowerStateService.this.mVivoSarPowerHandler.obtainMessage();
                    msg.what = 0;
                    Bundle bundle = new Bundle();
                    bundle.putInt("slotNum", slotNum);
                    bundle.putString("iccState", iccStateExtra);
                    msg.setData(bundle);
                    VivoSarPowerStateService.this.mVivoSarPowerHandler.sendMessage(msg);
                } else if (VivoSarPowerStateService.ACTION_SCREEN_OFF.equals(action)) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyScreenState(0);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                } else if (VivoSarPowerStateService.ACTION_SCREEN_ON.equals(action)) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyScreenState(1);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                } else if (VivoSarPowerStateService.ACTION_BOOT_COMPLETE.equals(action)) {
                    if (!VivoSarPowerStateService.platform.equals("samsung")) {
                        VivoSarPowerStateService.this.mSarPowerObserver = new SarPowerObserver();
                    }
                    VivoSarPowerStateService.this.initialPowerState();
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyBootCompleted();
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 1500);
                } else if (VivoSarPowerStateService.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
                    ServiceState ss = ServiceState.newFromBundle(intent.getExtras());
                    int currentRilState = ss.getNrState();
                    VivoSarPowerStateService.this.mVivoSarPowerHandler.sendEmptyMessage(2);
                    if (VivoSarPowerStateService.this.mVivoSarPowerStateController.isSupportWifiFrequency() && VivoSarPowerStateService.this.mVivoWifiFrequencyController != null) {
                        VivoSarPowerStateService.this.mVivoWifiFrequencyController.getNetworkFrequencySA(ss);
                    }
                    SElog.d(VivoSarPowerStateService.TAG, "received service state changed, currentRil State: " + currentRilState + " mLastEnDcState=" + VivoSarPowerStateService.this.mLastEnDcState);
                    if (currentRilState == 3 && VivoSarPowerStateService.this.mLastEnDcState != 3) {
                        SElog.d(VivoSarPowerStateService.TAG, "current endc state: 1");
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyForceUpdateState();
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyNeedReset();
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(1, 13);
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                        VivoSarPowerStateService.this.mVivoSarPowerHandler.sendEmptyMessageDelayed(3, 0L);
                    } else if (currentRilState != 3 && VivoSarPowerStateService.this.mLastEnDcState == 3) {
                        SElog.d(VivoSarPowerStateService.TAG, "current endc state: 0");
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyForceUpdateState();
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyNeedReset();
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(0, 13);
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                    }
                    if (currentRilState == 3) {
                        VivoSarPowerStateService.this.mVivoSarPowerHandler.removeMessages(4);
                        VivoSarPowerStateService.this.mVivoSarPowerHandler.sendEmptyMessageDelayed(4, 5000L);
                    }
                    VivoSarPowerStateService.this.mLastEnDcState = currentRilState;
                } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                    NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (VivoSarPowerStateService.this.mVivoSarPowerStateController.isSupportWifiFrequency()) {
                        VivoSarPowerStateService.this.mVivoSarPowerHandler.sendEmptyMessage(5);
                    }
                    if (info == null || info.isConnected()) {
                        VivoSarPowerStateService.this.mWIFIState = 1;
                    } else {
                        VivoSarPowerStateService.this.mWIFIState = 0;
                    }
                    SElog.d(VivoSarPowerStateService.TAG, "mWS change to : " + VivoSarPowerStateService.this.mWIFIState);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(VivoSarPowerStateService.this.mWIFIState + VivoSarPowerStateService.this.mHOTSPOTState, 12);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                } else if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
                    int state = intent.getIntExtra("wifi_state", 14);
                    if (state == 13) {
                        VivoSarPowerStateService.this.mHOTSPOTState = 1;
                    } else {
                        SElog.e(VivoSarPowerStateService.TAG, "SoftAP start failed");
                        VivoSarPowerStateService.this.mHOTSPOTState = 0;
                    }
                    SElog.d(VivoSarPowerStateService.TAG, "mHOTSS change to : " + VivoSarPowerStateService.this.mHOTSPOTState);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(VivoSarPowerStateService.this.mHOTSPOTState + VivoSarPowerStateService.this.mWIFIState, 12);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                } else if (VivoSarPowerStateService.ACTION_SETSARINFO_ON.equals(action)) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(1, VivoSarPowerStateService.SETSARINFO);
                } else if (VivoSarPowerStateService.ACTION_SETSARINFO_OFF.equals(action)) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(0, VivoSarPowerStateService.SETSARINFO);
                } else if (VivoSarPowerStateService.ACTION_HEADSET_PLUG_CHANEGD.equals(action)) {
                    SElog.d(VivoSarPowerStateService.TAG, "intent.hasExtra : " + intent.hasExtra("state"));
                    if (intent.hasExtra("state")) {
                        int headsetState = intent.getIntExtra("state", 0);
                        SElog.d(VivoSarPowerStateService.TAG, "headsetState = " + headsetState);
                        if (headsetState == 1) {
                            VivoSarPowerStateService.this.mHeadsetPlugState = 1;
                        } else {
                            VivoSarPowerStateService.this.mHeadsetPlugState = 0;
                        }
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(VivoSarPowerStateService.this.mHeadsetPlugState, 33);
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                    }
                }
            }
        };
        this.mSarPowerTestReceiver = new BroadcastReceiver() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.7
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                SElog.d(VivoSarPowerStateService.TAG, "mSarPowerTestReceiver action:" + action);
                if (VivoSarPowerStateService.ACTION_SAR_POWER_TEST.equals(action)) {
                    int tmpPowerState = intent.getIntExtra(VivoSarPowerStateService.SAR_POWER_STATE_TEST_PARMNAME, -1);
                    SElog.d(VivoSarPowerStateService.TAG, "receiver the powerState:" + tmpPowerState);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifySarPowerTest(tmpPowerState);
                }
            }
        };
        this.mVivoDisplayListener = new DisplayManager.DisplayListener() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.8
            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayAdded(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayRemoved(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayChanged(int displayId) {
                if (displayId == 0) {
                    if (VivoSarPowerStateService.this.mFrontDisplay != null && VivoSarPowerStateService.this.mFrontDisplayState != VivoSarPowerStateService.this.mFrontDisplay.getState()) {
                        VivoSarPowerStateService vivoSarPowerStateService = VivoSarPowerStateService.this;
                        vivoSarPowerStateService.mFrontDisplayState = vivoSarPowerStateService.mFrontDisplay.getState();
                    }
                } else if (displayId == 4096 && VivoSarPowerStateService.this.mBackDisplay != null && VivoSarPowerStateService.this.mBackDisplayState != VivoSarPowerStateService.this.mBackDisplay.getState()) {
                    VivoSarPowerStateService vivoSarPowerStateService2 = VivoSarPowerStateService.this;
                    vivoSarPowerStateService2.mBackDisplayState = vivoSarPowerStateService2.mBackDisplay.getState();
                }
                VivoSarPowerStateService vivoSarPowerStateService3 = VivoSarPowerStateService.this;
                vivoSarPowerStateService3.getDoubleScreenState(vivoSarPowerStateService3.mFrontDisplayState, VivoSarPowerStateService.this.mBackDisplayState);
            }
        };
    }

    public VivoSarPowerStateService(Context context) {
        this.mProximitySensor = null;
        this.mProximityFrontSensor = null;
        this.mProximityBackSensor = null;
        this.mSarSensor_A = null;
        this.mSarSensor_B = null;
        this.ARG = "ReceiveAudioState";
        this.mPhoneReceverState = -1;
        this.mVivoSarPowerStateController = null;
        this.mVivoGsmStateListener = null;
        this.mVivoWifiFrequencyController = null;
        this.mTargetPlatformInfo = 0;
        this.mDisplayManager = null;
        this.mFrontDisplay = null;
        this.mBackDisplay = null;
        this.mFrontDisplayState = 0;
        this.mBackDisplayState = 0;
        this.mLastBackProximityState = -1;
        this.mLastFrontProximityState = -1;
        this.mLastEnDcState = -1;
        this.mContentResolver = null;
        this.mWIFIState = 0;
        this.mHOTSPOTState = 0;
        this.mHeadsetPlugState = 0;
        this.mPhoneCardId = -1;
        this.mProximitySensorListener = new SensorEventListener() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.1
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                int proximityState = (int) event.values[0];
                SElog.d(VivoSarPowerStateService.TAG, "prox event: " + proximityState);
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(proximityState, 0);
                VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mSarSensorListener_A = new SensorEventListener() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.2
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                int sarAState = (int) event.values[0];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarAState, 4);
                int sarAState_CS0 = (int) event.values[1];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarAState_CS0, 5);
                int sarAState_CS1 = (int) event.values[2];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarAState_CS1, 6);
                int sarAState_CS2 = (int) event.values[3];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarAState_CS2, 7);
                SElog.d(VivoSarPowerStateService.TAG, "sar_A event: " + sarAState + " " + sarAState_CS0 + " " + sarAState_CS1 + " " + sarAState_CS2);
                VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mSarSensorListener_B = new SensorEventListener() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.3
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                int sarBState = (int) event.values[0];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarBState, 8);
                int sarBState_CS0 = (int) event.values[1];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarBState_CS0, 9);
                int sarBState_CS1 = (int) event.values[2];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarBState_CS1, 10);
                int sarBState_CS2 = (int) event.values[3];
                VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(sarBState_CS2, 11);
                SElog.d(VivoSarPowerStateService.TAG, "sar_B event: " + sarBState + " " + sarBState_CS0 + " " + sarBState_CS1 + " " + sarBState_CS2);
                VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mProximityFrontSensorListener = new SensorEventListener() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.4
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                int frontproximitystate = (int) event.values[0];
                if (VivoSarPowerStateService.this.mLastFrontProximityState != frontproximitystate) {
                    VivoSarPowerStateService.this.mLastFrontProximityState = frontproximitystate;
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(frontproximitystate, 1);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mProximityBackSensorListener = new SensorEventListener() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.5
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                int backproximitystate = (int) event.values[0];
                if (backproximitystate != VivoSarPowerStateService.this.mLastBackProximityState) {
                    VivoSarPowerStateService.this.mLastBackProximityState = backproximitystate;
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(backproximitystate, 2);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mStateChangeReceiver = new BroadcastReceiver() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.6
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                SElog.d(VivoSarPowerStateService.TAG, "SAR mStateChangeReceiver action:" + action);
                if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyForceUpdateState();
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                } else if (VivoSarPowerStateService.ACTION_CARD_CHANGE.equals(action)) {
                    int slotNum = intent.getIntExtra("phone", -1);
                    String iccStateExtra = intent.getStringExtra("ss");
                    SElog.d(VivoSarPowerStateService.TAG, "mCardChangeReceiver: slotNum " + slotNum + ",icc:" + iccStateExtra);
                    Message msg = VivoSarPowerStateService.this.mVivoSarPowerHandler.obtainMessage();
                    msg.what = 0;
                    Bundle bundle = new Bundle();
                    bundle.putInt("slotNum", slotNum);
                    bundle.putString("iccState", iccStateExtra);
                    msg.setData(bundle);
                    VivoSarPowerStateService.this.mVivoSarPowerHandler.sendMessage(msg);
                } else if (VivoSarPowerStateService.ACTION_SCREEN_OFF.equals(action)) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyScreenState(0);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                } else if (VivoSarPowerStateService.ACTION_SCREEN_ON.equals(action)) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyScreenState(1);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                } else if (VivoSarPowerStateService.ACTION_BOOT_COMPLETE.equals(action)) {
                    if (!VivoSarPowerStateService.platform.equals("samsung")) {
                        VivoSarPowerStateService.this.mSarPowerObserver = new SarPowerObserver();
                    }
                    VivoSarPowerStateService.this.initialPowerState();
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyBootCompleted();
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 1500);
                } else if (VivoSarPowerStateService.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
                    ServiceState ss = ServiceState.newFromBundle(intent.getExtras());
                    int currentRilState = ss.getNrState();
                    VivoSarPowerStateService.this.mVivoSarPowerHandler.sendEmptyMessage(2);
                    if (VivoSarPowerStateService.this.mVivoSarPowerStateController.isSupportWifiFrequency() && VivoSarPowerStateService.this.mVivoWifiFrequencyController != null) {
                        VivoSarPowerStateService.this.mVivoWifiFrequencyController.getNetworkFrequencySA(ss);
                    }
                    SElog.d(VivoSarPowerStateService.TAG, "received service state changed, currentRil State: " + currentRilState + " mLastEnDcState=" + VivoSarPowerStateService.this.mLastEnDcState);
                    if (currentRilState == 3 && VivoSarPowerStateService.this.mLastEnDcState != 3) {
                        SElog.d(VivoSarPowerStateService.TAG, "current endc state: 1");
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyForceUpdateState();
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyNeedReset();
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(1, 13);
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                        VivoSarPowerStateService.this.mVivoSarPowerHandler.sendEmptyMessageDelayed(3, 0L);
                    } else if (currentRilState != 3 && VivoSarPowerStateService.this.mLastEnDcState == 3) {
                        SElog.d(VivoSarPowerStateService.TAG, "current endc state: 0");
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyForceUpdateState();
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyNeedReset();
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(0, 13);
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                    }
                    if (currentRilState == 3) {
                        VivoSarPowerStateService.this.mVivoSarPowerHandler.removeMessages(4);
                        VivoSarPowerStateService.this.mVivoSarPowerHandler.sendEmptyMessageDelayed(4, 5000L);
                    }
                    VivoSarPowerStateService.this.mLastEnDcState = currentRilState;
                } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                    NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (VivoSarPowerStateService.this.mVivoSarPowerStateController.isSupportWifiFrequency()) {
                        VivoSarPowerStateService.this.mVivoSarPowerHandler.sendEmptyMessage(5);
                    }
                    if (info == null || info.isConnected()) {
                        VivoSarPowerStateService.this.mWIFIState = 1;
                    } else {
                        VivoSarPowerStateService.this.mWIFIState = 0;
                    }
                    SElog.d(VivoSarPowerStateService.TAG, "mWS change to : " + VivoSarPowerStateService.this.mWIFIState);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(VivoSarPowerStateService.this.mWIFIState + VivoSarPowerStateService.this.mHOTSPOTState, 12);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                } else if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
                    int state = intent.getIntExtra("wifi_state", 14);
                    if (state == 13) {
                        VivoSarPowerStateService.this.mHOTSPOTState = 1;
                    } else {
                        SElog.e(VivoSarPowerStateService.TAG, "SoftAP start failed");
                        VivoSarPowerStateService.this.mHOTSPOTState = 0;
                    }
                    SElog.d(VivoSarPowerStateService.TAG, "mHOTSS change to : " + VivoSarPowerStateService.this.mHOTSPOTState);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(VivoSarPowerStateService.this.mHOTSPOTState + VivoSarPowerStateService.this.mWIFIState, 12);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                } else if (VivoSarPowerStateService.ACTION_SETSARINFO_ON.equals(action)) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(1, VivoSarPowerStateService.SETSARINFO);
                } else if (VivoSarPowerStateService.ACTION_SETSARINFO_OFF.equals(action)) {
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(0, VivoSarPowerStateService.SETSARINFO);
                } else if (VivoSarPowerStateService.ACTION_HEADSET_PLUG_CHANEGD.equals(action)) {
                    SElog.d(VivoSarPowerStateService.TAG, "intent.hasExtra : " + intent.hasExtra("state"));
                    if (intent.hasExtra("state")) {
                        int headsetState = intent.getIntExtra("state", 0);
                        SElog.d(VivoSarPowerStateService.TAG, "headsetState = " + headsetState);
                        if (headsetState == 1) {
                            VivoSarPowerStateService.this.mHeadsetPlugState = 1;
                        } else {
                            VivoSarPowerStateService.this.mHeadsetPlugState = 0;
                        }
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.notifyStateChange(VivoSarPowerStateService.this.mHeadsetPlugState, 33);
                        VivoSarPowerStateService.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                    }
                }
            }
        };
        this.mSarPowerTestReceiver = new BroadcastReceiver() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.7
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                SElog.d(VivoSarPowerStateService.TAG, "mSarPowerTestReceiver action:" + action);
                if (VivoSarPowerStateService.ACTION_SAR_POWER_TEST.equals(action)) {
                    int tmpPowerState = intent.getIntExtra(VivoSarPowerStateService.SAR_POWER_STATE_TEST_PARMNAME, -1);
                    SElog.d(VivoSarPowerStateService.TAG, "receiver the powerState:" + tmpPowerState);
                    VivoSarPowerStateService.this.mVivoSarPowerStateController.notifySarPowerTest(tmpPowerState);
                }
            }
        };
        this.mVivoDisplayListener = new DisplayManager.DisplayListener() { // from class: com.vivo.services.sarpower.VivoSarPowerStateService.8
            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayAdded(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayRemoved(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayChanged(int displayId) {
                if (displayId == 0) {
                    if (VivoSarPowerStateService.this.mFrontDisplay != null && VivoSarPowerStateService.this.mFrontDisplayState != VivoSarPowerStateService.this.mFrontDisplay.getState()) {
                        VivoSarPowerStateService vivoSarPowerStateService = VivoSarPowerStateService.this;
                        vivoSarPowerStateService.mFrontDisplayState = vivoSarPowerStateService.mFrontDisplay.getState();
                    }
                } else if (displayId == 4096 && VivoSarPowerStateService.this.mBackDisplay != null && VivoSarPowerStateService.this.mBackDisplayState != VivoSarPowerStateService.this.mBackDisplay.getState()) {
                    VivoSarPowerStateService vivoSarPowerStateService2 = VivoSarPowerStateService.this;
                    vivoSarPowerStateService2.mBackDisplayState = vivoSarPowerStateService2.mBackDisplay.getState();
                }
                VivoSarPowerStateService vivoSarPowerStateService3 = VivoSarPowerStateService.this;
                vivoSarPowerStateService3.getDoubleScreenState(vivoSarPowerStateService3.mFrontDisplayState, VivoSarPowerStateService.this.mBackDisplayState);
            }
        };
        mContext = context;
        if (SUPPORT_SAR_POWER) {
            SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
            this.mSensorManager = sensorManager;
            this.mProximitySensor = sensorManager.getDefaultSensor(8);
            this.mTelephonyManager = (TelephonyManager) mContext.getSystemService(TelephonyManager.class);
            SystemProperties.set("sys.sar.dsi", String.valueOf(0));
            if (platform.equals("qcom")) {
                this.mTargetPlatformInfo = 1;
                this.mVivoSarPowerStateController = new VivoQcomSarPowerStateController(this, context);
            } else if (platform.equals("samsung")) {
                this.mTargetPlatformInfo = 4;
                this.mVivoSarPowerStateController = new VivoSamsungSarPowerStateController(this, context);
            } else if (AblConfig.isMtkSupportDsiCommand()) {
                this.mTargetPlatformInfo = 2;
                this.mVivoSarPowerStateController = new VivoMtkNewSarPowerStateController(this, context);
            } else {
                this.mTargetPlatformInfo = 2;
                this.mVivoSarPowerStateController = new VivoMtkSarPowerStateController(this, context);
            }
            if (AblConfig.isUseGsm900()) {
                this.mVivoGsmStateListener = new VivoGsmStateListener(context, this.mVivoSarPowerStateController);
            }
            IntentFilter mStateChangeFilter = new IntentFilter();
            mStateChangeFilter.addAction(ACTION_BOOT_COMPLETE);
            mStateChangeFilter.addAction("android.intent.action.AIRPLANE_MODE");
            if (!AblConfig.supportDoubleScreenOrNot()) {
                mStateChangeFilter.addAction(ACTION_SCREEN_ON);
                mStateChangeFilter.addAction(ACTION_SCREEN_OFF);
            }
            mStateChangeFilter.addAction(ACTION_CARD_CHANGE);
            mStateChangeFilter.addAction(ACTION_SERVICE_STATE_CHANGED);
            mStateChangeFilter.addAction("android.net.wifi.STATE_CHANGE");
            mStateChangeFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
            HandlerThread handlerThread = new HandlerThread(TAG);
            mThread = handlerThread;
            handlerThread.start();
            this.mMainLooper = mThread.getLooper();
            VivoSarPowerHandler vivoSarPowerHandler = new VivoSarPowerHandler(this.mMainLooper);
            this.mVivoSarPowerHandler = vivoSarPowerHandler;
            vivoSarPowerHandler.sendEmptyMessage(1);
            if (AblConfig.supportDoubleScreenOrNot()) {
                DisplayManager displayManager = (DisplayManager) mContext.getSystemService("display");
                this.mDisplayManager = displayManager;
                if (displayManager != null) {
                    this.mFrontDisplay = displayManager.getDisplay(0);
                    this.mBackDisplay = this.mDisplayManager.getDisplay(4096);
                    Display display = this.mFrontDisplay;
                    if (display != null) {
                        this.mFrontDisplayState = display.getState();
                    }
                    Display display2 = this.mBackDisplay;
                    if (display2 != null) {
                        this.mBackDisplayState = display2.getState();
                    }
                    this.mDisplayManager.registerDisplayListener(this.mVivoDisplayListener, this.mVivoSarPowerHandler);
                }
            }
            if (AblConfig.isMtkSupportDsiCommand()) {
                mStateChangeFilter.addAction("android.intent.action.AIRPLANE_MODE");
            }
            mStateChangeFilter.addAction(ACTION_SETSARINFO_ON);
            mStateChangeFilter.addAction(ACTION_SETSARINFO_OFF);
            mStateChangeFilter.addAction(ACTION_HEADSET_PLUG_CHANEGD);
            mContext.registerReceiver(this.mStateChangeReceiver, mStateChangeFilter);
            try {
                ServiceState servicestate = new ServiceState();
                int currentRilState = servicestate.getNrState();
                SElog.d(TAG, "received service state changed, currentRil State: " + currentRilState);
                if (currentRilState == 3) {
                    SElog.d(TAG, "mLastEnDcState: " + this.mLastEnDcState + ", force sar and reset");
                    this.mVivoSarPowerStateController.notifyForceUpdateState();
                    this.mVivoSarPowerStateController.notifyNeedReset();
                    this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                    this.mVivoSarPowerStateController.notifyStateChange(1, 13);
                    this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                } else if (currentRilState != 3) {
                    SElog.d(TAG, "mLastEnDcState: " + this.mLastEnDcState + ", force sar and reset");
                    this.mVivoSarPowerStateController.notifyForceUpdateState();
                    this.mVivoSarPowerStateController.notifyNeedReset();
                    this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                    this.mVivoSarPowerStateController.notifyStateChange(0, 13);
                    this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                }
                this.mLastEnDcState = currentRilState;
            } catch (Exception e) {
                SElog.e(TAG, "servicestate has some error");
            }
            if (this.mVivoSarPowerStateController.isSupportWifiFrequency()) {
                this.mVivoWifiFrequencyController = new VivoWifiFrequencyController(mContext, this.mVivoSarPowerStateController, this.mVivoSarPowerHandler);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getDoubleScreenState(int front_state, int back_state) {
        int doublescreenstate = -1;
        if (front_state != 1) {
            if (front_state != 2) {
                return 0;
            }
            if (back_state == 2) {
                doublescreenstate = 5;
            } else if (back_state == 1) {
                doublescreenstate = 4;
            }
        } else if (back_state == 2) {
            doublescreenstate = 5;
        } else if (back_state == 1) {
            doublescreenstate = 3;
        }
        this.mVivoSarPowerStateController.notifyScreenState(doublescreenstate);
        this.mVivoSarPowerStateController.handleSarMessage(0, 0);
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initialPowerState() {
        if (this.mVivoSarPowerStateController.initialPowerState()) {
            IntentFilter filterSarPowerTest = new IntentFilter();
            filterSarPowerTest.addAction(ACTION_SAR_POWER_TEST);
            mContext.registerReceiver(this.mSarPowerTestReceiver, filterSarPowerTest);
        } else if (!platform.equals("samsung")) {
            sarPowerSwitchEnable(0);
        }
    }

    public int sarPowerSwitchEnable(int enable) {
        int ret = nativeHandleSarPowerEnable(enable);
        if (ret < 0) {
            SElog.d(TAG, "SarPowerSwitchEnable write fail : " + ret);
        }
        SElog.d(TAG, "SarPowerSwitchEnable write enable : " + enable);
        return 0;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args != null && args.length > 0 && DUMP_MAGIC_WORD.equals(args[0])) {
            this.mVivoSarPowerStateController.dump(pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getNetworkFrequencyNSA() {
        VivoWifiFrequencyController vivoWifiFrequencyController;
        if (this.mVivoSarPowerStateController.isSupportWifiFrequency() && (vivoWifiFrequencyController = this.mVivoWifiFrequencyController) != null) {
            vivoWifiFrequencyController.getNetworkFrequencyNSA(this.mPhoneCardId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getWifiFrequency() {
        VivoWifiFrequencyController vivoWifiFrequencyController;
        if (this.mVivoSarPowerStateController.isSupportWifiFrequency() && (vivoWifiFrequencyController = this.mVivoWifiFrequencyController) != null) {
            vivoWifiFrequencyController.getWifiFrequency();
        }
    }
}