package com.vivo.services.sarpower;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import com.iqoo.engineermode.PhoneInterface;
import com.vivo.sensor.autobrightness.config.AblConfig;
import com.vivo.sensor.autobrightness.utils.SElog;
import com.vivo.sensor.sarpower.VivoSarConfig;
import java.io.PrintWriter;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class VivoMtkNewSarPowerStateController extends VivoSarConfig {
    private static final String ACTION_SAR_POWER_0_CMD = "AT+ERFIDX=1,-1";
    private static final String ACTION_SAR_POWER_10_CMD = "AT+ERFIDX=1,10";
    private static final String ACTION_SAR_POWER_11_CMD = "AT+ERFIDX=1,11";
    private static final String ACTION_SAR_POWER_12_CMD = "AT+ERFIDX=1,12";
    private static final String ACTION_SAR_POWER_1_CMD = "AT+ERFIDX=1,1";
    private static final String ACTION_SAR_POWER_2_CMD = "AT+ERFIDX=1,2";
    private static final String ACTION_SAR_POWER_3_CMD = "AT+ERFIDX=1,3";
    private static final String ACTION_SAR_POWER_4_CMD = "AT+ERFIDX=1,4";
    private static final String ACTION_SAR_POWER_5_CMD = "AT+ERFIDX=1,5";
    private static final String ACTION_SAR_POWER_6_CMD = "AT+ERFIDX=1,6";
    private static final String ACTION_SAR_POWER_7_CMD = "AT+ERFIDX=1,7";
    private static final String ACTION_SAR_POWER_8_CMD = "AT+ERFIDX=1,8";
    private static final String ACTION_SAR_POWER_9_CMD = "AT+ERFIDX=1,9";
    private static final String KEY_FACTORY_MODE = "persist_factory_mode";
    private static final int MSG_SAR_POWER_CHANGE = 0;
    private static final int MSG_SAR_UPDATE_PARAM = 2;
    private static final String NotifySarDsiChangeToWifiAction = "android.net.sar.SENSOR_TO_WIFI";
    private static final int PHONE_ID_GSM = 0;
    private static final String TAG = "SarPowerStateService";
    private static Context mContext;
    private static HandlerThread mThread;
    private boolean isBootCompleted;
    private ContentResolver mContentResolver;
    private byte mLastReportSarPowerStateToWifi;
    private Looper mMainLooper;
    private PhoneInterface mPhoneInterface;
    private PhoneServiceConnection mPhoneServiceConnection;
    private PowerChangeHandler mPowerChangeHandler;
    private Runnable mRegisterRunnable;
    private SarSettingsObserver mSarSettingsObserver;
    private static final String model = SystemProperties.get("ro.vivo.product.model", "unkown").toLowerCase();
    private static final String mBoardVersion = SystemProperties.get("ro.vivo.board_version", "unkown").toLowerCase();

    public VivoMtkNewSarPowerStateController(VivoSarPowerStateService service, Context contxt) {
        super(service);
        this.mPhoneInterface = null;
        this.mPhoneServiceConnection = null;
        this.mLastReportSarPowerStateToWifi = (byte) 0;
        this.isBootCompleted = false;
        this.mRegisterRunnable = new Runnable() { // from class: com.vivo.services.sarpower.VivoMtkNewSarPowerStateController.1
            @Override // java.lang.Runnable
            public void run() {
                VivoMtkNewSarPowerStateController.this.registerObserver();
            }
        };
        mContext = contxt;
        this.mContentResolver = contxt.getContentResolver();
        HandlerThread handlerThread = new HandlerThread("SarPowerStateService_MtkNew");
        mThread = handlerThread;
        handlerThread.start();
        this.mMainLooper = mThread.getLooper();
        this.mPowerChangeHandler = new PowerChangeHandler(this.mMainLooper);
        this.mSarSettingsObserver = new SarSettingsObserver(this.mPowerChangeHandler);
        this.mPowerChangeHandler.post(this.mRegisterRunnable);
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public boolean initialPowerState() {
        this.isBootCompleted = true;
        startPhoneService();
        return true;
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void handleSarMessage(int sarMsg, int deleyTimes) {
        if (sarMsg == 0) {
            this.mPowerChangeHandler.removeMessages(0);
            PowerChangeHandler powerChangeHandler = this.mPowerChangeHandler;
            powerChangeHandler.sendMessageDelayed(powerChangeHandler.obtainMessage(0), deleyTimes);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class PowerChangeHandler extends Handler {
        public PowerChangeHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 0) {
                if (i == 2) {
                    VivoMtkNewSarPowerStateController.this.notifyForceUpdateState();
                    VivoMtkNewSarPowerStateController.this.handleSarMessage(0, 500);
                    SElog.i(VivoMtkNewSarPowerStateController.TAG, "MSG_SAR_UPDATE_PARAM update params");
                    return;
                }
                SElog.d(VivoMtkNewSarPowerStateController.TAG, "PowerChangeHandler default, the mProximityState is" + VivoMtkNewSarPowerStateController.this.mProximityState);
                return;
            }
            byte powerState = VivoMtkNewSarPowerStateController.this.processStateChange();
            SElog.d(VivoMtkNewSarPowerStateController.TAG, "PowerChangeHandler power change,newMTK powerState = " + ((int) powerState) + ", mLastSarPowerState = " + VivoMtkNewSarPowerStateController.this.mLastSarPowerState + ", mProximityState = " + VivoMtkNewSarPowerStateController.this.mProximityState + ", mScreenState = " + VivoMtkNewSarPowerStateController.this.mScreenState + ", mSarPowerRfDetectState = " + VivoMtkNewSarPowerStateController.this.mSarPowerRfDetectState + ", mCardOneState = " + VivoMtkNewSarPowerStateController.this.mCardOneState + ", mCardTwoState = " + VivoMtkNewSarPowerStateController.this.mCardTwoState + ", mLastCardState = " + VivoMtkNewSarPowerStateController.this.mLastCardState + ", mAudioState = " + VivoMtkNewSarPowerStateController.this.mAudioState + ", mWIFIState = " + VivoMtkNewSarPowerStateController.this.mWIFIState + ", mSAR_A_state = " + VivoMtkNewSarPowerStateController.this.mSAR_A_state + ", mSAR_A_CS0_state=" + VivoMtkNewSarPowerStateController.this.mSAR_A_CS0_state + ", mSAR_A_CS1_state=" + VivoMtkNewSarPowerStateController.this.mSAR_A_CS1_state + ", mSAR_A_CS2_state=" + VivoMtkNewSarPowerStateController.this.mSAR_A_CS2_state + ", mForceUpdateState = " + VivoMtkNewSarPowerStateController.this.mForceUpdateState + ", changeDsiOnFactoryMode = " + ((int) VivoMtkNewSarPowerStateController.this.changeDsiOnFactoryMode(powerState)));
            byte powerState2 = VivoMtkNewSarPowerStateController.this.changeDsiOnFactoryMode(powerState);
            VivoMtkNewSarPowerStateController.this.notifyDSItosystem(powerState2);
            if (VivoMtkNewSarPowerStateController.this.mLastSarPowerState != powerState2 || VivoMtkNewSarPowerStateController.this.mForceUpdateState) {
                VivoMtkNewSarPowerStateController.this.mLastSarPowerState = powerState2;
                if (VivoMtkNewSarPowerStateController.this.isBootCompleted) {
                    if (VivoMtkNewSarPowerStateController.this.mForceUpdateState) {
                        VivoMtkNewSarPowerStateController.this.mForceUpdateState = false;
                    }
                    VivoMtkNewSarPowerStateController.this.handleSarPowerReduction(powerState2);
                    return;
                }
                SElog.d(VivoMtkNewSarPowerStateController.TAG, "not boot completed, so do not react");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyDSItosystem(byte powerState) {
        try {
            SystemProperties.set("sys.sar.dsi", String.valueOf((int) powerState));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public byte changeDsiOnFactoryMode(byte powerState) {
        int manualDsi = SystemProperties.getInt("persist.sys.vivo.fixeddsi", -1);
        if (rejectReduceSar()) {
            SElog.d(TAG, "Pretend to setQcRiSarPowerState powerState = " + ((int) powerState));
            powerState = 0;
        }
        if (manualDsi >= 0) {
            return (byte) manualDsi;
        }
        return powerState;
    }

    private boolean rejectReduceSar() {
        return this.isUnderFactoryMode && AblConfig.EngineModeRejectReduceSar() && this.mSAR_A_state != 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSarPowerReduction(int state) {
        switch (state) {
            case 0:
                sendCommand(ACTION_SAR_POWER_0_CMD, 0);
                return;
            case 1:
                sendCommand(ACTION_SAR_POWER_1_CMD, 0);
                return;
            case 2:
                sendCommand(ACTION_SAR_POWER_2_CMD, 0);
                return;
            case 3:
                sendCommand(ACTION_SAR_POWER_3_CMD, 0);
                return;
            case 4:
                sendCommand(ACTION_SAR_POWER_4_CMD, 0);
                return;
            case 5:
                sendCommand(ACTION_SAR_POWER_5_CMD, 0);
                return;
            case 6:
                sendCommand(ACTION_SAR_POWER_6_CMD, 0);
                return;
            case 7:
                sendCommand(ACTION_SAR_POWER_7_CMD, 0);
                return;
            case 8:
                sendCommand(ACTION_SAR_POWER_8_CMD, 0);
                return;
            case 9:
                sendCommand(ACTION_SAR_POWER_9_CMD, 0);
                return;
            case 10:
                sendCommand(ACTION_SAR_POWER_10_CMD, 0);
                return;
            case 11:
                sendCommand(ACTION_SAR_POWER_11_CMD, 0);
                return;
            case 12:
                sendCommand(ACTION_SAR_POWER_12_CMD, 0);
                return;
            default:
                sendCommand(ACTION_SAR_POWER_0_CMD, 0);
                return;
        }
    }

    private void startPhoneService() {
        Intent intent = new Intent();
        intent.setClassName("com.iqoo.engineermode", "com.iqoo.engineermode.PhoneService");
        this.mPhoneServiceConnection = new PhoneServiceConnection();
        SElog.d(TAG, "startPhoneService");
        mContext.bindService(intent, this.mPhoneServiceConnection, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class PhoneServiceConnection implements ServiceConnection {
        PhoneServiceConnection() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            SElog.d(VivoMtkNewSarPowerStateController.TAG, "onServiceConnected:" + name + " service " + service);
            try {
                VivoMtkNewSarPowerStateController.this.mPhoneInterface = PhoneInterface.Stub.asInterface(service);
            } catch (Exception e) {
                SElog.e(VivoMtkNewSarPowerStateController.TAG, "Exception", e);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            SElog.d(VivoMtkNewSarPowerStateController.TAG, "onServiceDisconnected:" + name);
        }
    }

    private void stopPhoneService() {
        try {
            if (this.mPhoneInterface != null) {
                SElog.d(TAG, "stopPhoneService");
                mContext.unbindService(this.mPhoneServiceConnection);
                this.mPhoneServiceConnection = null;
                this.mPhoneInterface = null;
            }
        } catch (Exception e) {
            SElog.e(TAG, "Exception", e);
        }
    }

    private void sendCommand(String cmd, int id) {
        SElog.d(TAG, "command:" + cmd + ", i:" + id);
        try {
            if (this.mPhoneInterface == null) {
                SElog.e(TAG, "mPhoneInterface is null, cannot send command");
                this.mForceUpdateState = true;
            } else {
                this.mPhoneInterface.sendATCommand(cmd, id);
            }
        } catch (Exception ex) {
            SElog.e(TAG, "sendCommand exception:" + ex);
        }
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void notifySarPowerTest(int powerStateValue) {
    }

    private int getCurrentFactoryMode(int oldMode) {
        int mode = oldMode;
        try {
            mode = Settings.Global.getInt(this.mContentResolver, KEY_FACTORY_MODE, 0);
            SElog.i(TAG, "getCurrentFactoryMode:" + mode);
            return mode;
        } catch (Exception e) {
            SElog.e(TAG, "getCurrentFactoryMode failed.");
            return mode;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSettingsChanged() {
        boolean z = this.isUnderFactoryMode;
        int oldMode = z ? 1 : 0;
        int newMode = getCurrentFactoryMode(oldMode);
        if (z != newMode) {
            PowerChangeHandler powerChangeHandler = this.mPowerChangeHandler;
            powerChangeHandler.sendMessage(powerChangeHandler.obtainMessage(2));
            SElog.i(TAG, "mode change. update the params");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SarSettingsObserver extends ContentObserver {
        public SarSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            VivoMtkNewSarPowerStateController.this.handleSettingsChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerObserver() {
        ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(Settings.Global.getUriFor(KEY_FACTORY_MODE), false, this.mSarSettingsObserver, -1);
        SElog.d(TAG, "registerObserver");
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
    public void dump(PrintWriter pw) {
        pw.println(String.format("---- %s ----", TAG));
    }
}