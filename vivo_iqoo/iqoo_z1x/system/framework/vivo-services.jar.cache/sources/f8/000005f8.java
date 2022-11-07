package com.vivo.sensor.sarpower;

import android.os.SystemProperties;
import java.io.PrintWriter;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public abstract class VivoSarPowerStateController {
    protected static final byte ACTION_SAR_POWER_FALL_DSI_0 = 0;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_1 = 1;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_10 = 10;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_11 = 11;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_12 = 12;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_13 = 13;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_14 = 14;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_2 = 2;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_3 = 3;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_4 = 4;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_5 = 5;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_6 = 6;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_7 = 7;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_8 = 8;
    protected static final byte ACTION_SAR_POWER_FALL_DSI_9 = 9;
    private static final int AUDIO = 3;
    protected static final int BOARD_VERSION_VALUE = 30;
    private static final int ENDC = 13;
    private static final int GSM_CALLING_STATE = 34;
    private static final int HEADSET_PLUG_STATE = 33;
    private static final int PROMIMITY = 0;
    private static final int PROMIMITY_BACK = 2;
    private static final int PROMIMITY_FRONT = 1;
    private static final int SAR_A = 4;
    private static final int SAR_A_CS0 = 5;
    private static final int SAR_A_CS1 = 6;
    private static final int SAR_A_CS2 = 7;
    private static final int SAR_B = 8;
    private static final int SAR_B_CS0 = 9;
    private static final int SAR_B_CS1 = 10;
    private static final int SAR_B_CS2 = 11;
    private static final int SAR_REGISTER_FAILED_VALUE = 31;
    private static final int SAR_SENSOR_EXIST = 32;
    private static final int SETSARINFO = 88;
    protected static final String TAG = "SarPowerStateService";
    protected static final byte USE_SAR_CODE = 0;
    protected static final byte USE_SAR_SENSOR_A = 1;
    protected static final byte USE_SAR_SENSOR_AB = 3;
    protected static final byte USE_SAR_SENSOR_B = 2;
    private static final int WIFI = 12;
    private boolean isDisplaySarValue;
    protected boolean isUnderFactoryMode;
    protected int mAudioState;
    protected int mBackProximityState;
    protected int mBoardVersionValueState;
    protected int mCardOneState;
    protected int mCardTwoState;
    protected int mENDCState;
    protected boolean mForceUpdateState;
    protected int mFrequencyNSA;
    protected int mFrequencySA;
    protected int mFrequencySoftAp;
    protected int mFrequencyWifi;
    protected int mFrontProximityState;
    protected int mGsmCallingState;
    protected boolean mHasOneCard;
    protected boolean mHasTwoCard;
    protected int mHeadsetPlugState;
    protected boolean mIgnoreRfdetect;
    protected int mLastCardState;
    protected int mLastSarPowerState;
    protected boolean mNeedReset;
    protected int mProximityState;
    protected int mSAR_A_CS0_state;
    protected int mSAR_A_CS1_state;
    protected int mSAR_A_CS2_state;
    protected int mSAR_A_state;
    protected int mSAR_B_CS0_state;
    protected int mSAR_B_CS1_state;
    protected int mSAR_B_CS2_state;
    protected int mSAR_B_state;
    protected int mSarPowerRfDetectState;
    protected int mSarRegisterFailedState;
    protected int mSarSensorExist;
    protected int mScreenState;
    protected int mSetSarInfo;
    protected int mWIFIState;
    protected static int SAR_ForceNear = 1;
    protected static int SAR_ForceFar = 2;
    protected static int SAR_UnKonwnStatus = -1;
    protected static int SAR_I2CError = 255;
    protected static int Audio_handsetwork = 1;
    protected static int Audio_speakerwork = 0;
    protected static int Prox_Use_1 = 0;
    protected static int Prox_NoUse_0 = 5;
    protected static final String model = SystemProperties.get("ro.vivo.product.model", "unkown").toLowerCase();
    protected static final boolean eu = SystemProperties.get("persist.vivo.sar.aera", "unkown").toLowerCase().equals("eu");
    protected boolean isOpenFrontCamera = false;
    private String PROP_FACTORY_MODE = "persist.sys.factory.mode";
    private String PROP_FACTORY_MODE_DISPLAY_SAR_VALUE = "persist.sys.vivo.displaydsi";
    private boolean isFactoryMode = SystemProperties.get("persist.sys.factory.mode", "no").equals("yes");

    public abstract void dump(PrintWriter printWriter);

    public abstract void handleSarMessage(int i, int i2);

    public abstract boolean initialPowerState();

    public abstract void notifyDsiToWifi(byte b);

    public abstract void notifySarPowerTest(int i);

    public abstract void registerResource();

    public VivoSarPowerStateController() {
        boolean equals = SystemProperties.get(this.PROP_FACTORY_MODE_DISPLAY_SAR_VALUE, "no").equals("yes");
        this.isDisplaySarValue = equals;
        this.isUnderFactoryMode = this.isFactoryMode && !equals;
        this.mSarPowerRfDetectState = -1;
        this.mProximityState = -1;
        this.mSAR_A_state = -1;
        this.mSAR_A_CS0_state = -1;
        this.mSAR_A_CS1_state = -1;
        this.mSAR_A_CS2_state = -1;
        this.mSAR_B_state = -1;
        this.mSAR_B_CS0_state = -1;
        this.mSAR_B_CS1_state = -1;
        this.mSAR_B_CS2_state = -1;
        this.mAudioState = 0;
        this.mWIFIState = 0;
        this.mENDCState = -1;
        this.mBoardVersionValueState = -1;
        this.mSarRegisterFailedState = 0;
        this.mSarSensorExist = -1;
        this.mHeadsetPlugState = 0;
        this.mGsmCallingState = -1;
        this.mSetSarInfo = 0;
        this.mFrontProximityState = -1;
        this.mBackProximityState = -1;
        this.mLastSarPowerState = -1;
        this.mLastCardState = -1;
        this.mScreenState = -1;
        this.mCardOneState = -1;
        this.mCardTwoState = -1;
        this.mFrequencyWifi = -1;
        this.mFrequencySoftAp = -1;
        this.mFrequencySA = -1;
        this.mFrequencyNSA = -1;
        this.mForceUpdateState = false;
        this.mIgnoreRfdetect = false;
        this.mNeedReset = false;
        this.mHasOneCard = false;
        this.mHasTwoCard = false;
    }

    public void notifyStateChange(int state, int index) {
        if (index != SETSARINFO) {
            switch (index) {
                case 0:
                    this.mProximityState = state;
                    setSarInfo(this.mSetSarInfo);
                    return;
                case 1:
                    this.mFrontProximityState = state;
                    return;
                case 2:
                    this.mBackProximityState = state;
                    return;
                case 3:
                    this.mAudioState = state;
                    setSarInfo(this.mSetSarInfo);
                    return;
                case 4:
                    this.mSAR_A_state = state;
                    setSarInfo(this.mSetSarInfo);
                    return;
                case 5:
                    this.mSAR_A_CS0_state = state;
                    setSarInfo(this.mSetSarInfo);
                    return;
                case 6:
                    this.mSAR_A_CS1_state = state;
                    setSarInfo(this.mSetSarInfo);
                    return;
                case 7:
                    this.mSAR_A_CS2_state = state;
                    setSarInfo(this.mSetSarInfo);
                    return;
                case 8:
                    this.mSAR_B_state = state;
                    setSarInfo(this.mSetSarInfo);
                    return;
                case 9:
                    this.mSAR_B_CS0_state = state;
                    setSarInfo(this.mSetSarInfo);
                    return;
                case 10:
                    this.mSAR_B_CS1_state = state;
                    setSarInfo(this.mSetSarInfo);
                    return;
                case 11:
                    this.mSAR_B_CS2_state = state;
                    setSarInfo(this.mSetSarInfo);
                    return;
                case 12:
                    if (state == 0) {
                        this.mWIFIState = 0;
                    } else {
                        this.mWIFIState = 1;
                    }
                    setSarInfo(this.mSetSarInfo);
                    return;
                case 13:
                    this.mENDCState = state;
                    setSarInfo(this.mSetSarInfo);
                    return;
                default:
                    switch (index) {
                        case 30:
                            this.mBoardVersionValueState = state;
                            return;
                        case 31:
                            this.mSarRegisterFailedState = state;
                            return;
                        case 32:
                            this.mSarSensorExist = state;
                            return;
                        case 33:
                            this.mHeadsetPlugState = state;
                            return;
                        case 34:
                            this.mGsmCallingState = state;
                            return;
                        default:
                            return;
                    }
            }
        }
        this.mSetSarInfo = state;
        setSarInfo(state);
        VSlog.d(TAG, "setSarInfo for Apk state change to " + state);
    }

    public void notifySarPowerRfDetectState(int sarRfDetectState) {
        if (!this.mIgnoreRfdetect) {
            VSlog.d(TAG, "mIgnoreRfdetect false");
            this.mSarPowerRfDetectState = sarRfDetectState;
            return;
        }
        VSlog.d(TAG, "Ignore sarRfDetectState");
    }

    public void notifyScreenState(int screenState) {
        this.mScreenState = screenState;
    }

    public void notifyCardState(boolean isCardOne, int state) {
        if (isCardOne) {
            this.mCardOneState = state;
        } else {
            this.mCardTwoState = state;
        }
        setSarInfo(this.mSetSarInfo);
    }

    public void notifyBootCompleted() {
        this.mLastSarPowerState = -1;
    }

    public void notifyForceUpdateState() {
        this.mForceUpdateState = true;
    }

    public void notifyNeedReset() {
        this.mNeedReset = true;
    }

    public void notifyCardInsert(boolean isCardOne, boolean state) {
        if (isCardOne) {
            this.mHasOneCard = state;
        } else {
            this.mHasTwoCard = state;
        }
    }

    public void setSarInfo(int flag) {
        if (flag == 1) {
            try {
                SystemProperties.set("sys.sar.zx.mSarInfo", String.valueOf(this.mWIFIState) + " " + String.valueOf(this.mAudioState) + " " + String.valueOf(this.mProximityState) + " " + String.valueOf(this.mENDCState) + " " + String.valueOf(this.mSAR_A_state) + " " + String.valueOf(this.mSAR_A_CS0_state) + " " + String.valueOf(this.mSAR_A_CS1_state) + " " + String.valueOf(this.mSAR_A_CS2_state) + " " + String.valueOf(this.mSAR_B_state) + " " + String.valueOf(this.mSAR_B_CS0_state) + " " + String.valueOf(this.mSAR_B_CS1_state) + " " + String.valueOf(this.mSAR_B_CS2_state) + " " + String.valueOf(this.mCardOneState) + " " + String.valueOf(this.mCardTwoState));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void notifyServiceStateChanged() {
    }

    public void notifyWifiFrequency(int frequency) {
        this.mFrequencyWifi = frequency;
    }

    public void notifySoftApFrequency(int frequency) {
        this.mFrequencySoftAp = frequency;
    }

    public void notifyNetworkSANrarfcn(int frequencySA) {
        this.mFrequencySA = frequencySA;
    }

    public void notifyNetworkNSANrarfcn(int frequencyNSA) {
        this.mFrequencyNSA = frequencyNSA;
    }

    public boolean isSupportWifiFrequency() {
        return false;
    }
}