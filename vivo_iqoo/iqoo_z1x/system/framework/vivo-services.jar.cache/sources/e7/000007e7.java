package com.vivo.services.sarpower;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import com.iqoo.engineermode.PhoneInterface;
import com.vivo.face.common.data.Constants;
import com.vivo.sensor.autobrightness.config.AblConfig;
import com.vivo.sensor.autobrightness.utils.SElog;
import com.vivo.sensor.sarpower.VivoSarConfig;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Arrays;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class VivoMtkSarPowerStateController extends VivoSarConfig {
    private static final String ACTION_CLOSE_CAMERA = "com.android.camera.ACTION_CLOSE_CAMERA";
    private static final String ACTION_OPEN_CAMERA = "com.android.camera.ACTION_OPEN_CAMERA";
    private static final byte ACTION_SAR_POWER_CAMERA_CLOSE = 4;
    private static final byte ACTION_SAR_POWER_CAMERA_OPEN = 3;
    private static final byte ACTION_SAR_POWER_FAR = 0;
    private static final byte ACTION_SAR_POWER_NEAR_BODY = 2;
    private static final byte ACTION_SAR_POWER_NEAR_HEAD = 1;
    private static final String BOARD_VERSION = "/sys/devs_list/board_version";
    private static final String CAMERA_ID = "mCameraId";
    private static final String KEY_FACTORY_MODE = "persist_factory_mode";
    private static final int MSG_SAR_CAMERA_ISSUE_DELAY = 3;
    private static final int MSG_SAR_CAMERA_ISSUE_START = 4;
    private static final int MSG_SAR_POWER_CHANGE = 0;
    private static final int MSG_SAR_UPDATE_PARAM = 2;
    private static final int PHONE_ID_CDMA = 1;
    private static final int PHONE_ID_GSM = 0;
    private static final String ReductionCommand_2G = "AT+ERFTX=10,1\r\n";
    private static final String ReductionCommand_3G = "AT+ERFTX=10,2\r\n";
    private static final String ReductionCommand_4G = "AT+ERFTX=10,3,1,32,16,0\r\n";
    private static final String ReductionCommand_All = "AT+ERFTX=9,8,8,8,16,16,16\r\n";
    private static final String ReductionCommand_All_For_White_Card = "AT+ERFTX=9,16,16,16,32,32,32\r\n";
    private static final String ReductionCommand_All_For_White_Card_On_C2K = "AT+ERFTX=4,8,0,16,32\r\n";
    private static final String ReductionCommand_All_On_C2K = "AT+ERFTX=4,8,0,8,16\r\n";
    private static final String ResetCommand_2G = "AT+ERFTX=10,1\r\n";
    private static final String ResetCommand_3G = "AT+ERFTX=10,2\r\n";
    private static final String ResetCommand_4G = "AT+ERFTX=10,3,1,0,0,0\r\n";
    private static final String ResetCommand_All = "AT+ERFTX=9,0,0,0,0,0,0";
    private static final String ResetCommand_All_On_C2K = "AT+ERFTX=4,8,0,0,0";
    private static final String ResetCommand_All_On_C2K_NEW = "AT+ERFTX=13,7,0,0";
    private static final String TAG = "SarPowerStateService";
    private static final String VIVO_MTK_SAR_CONFIG_PARSED = "com.vivo.services.sarpower";
    private static Context mContext;
    private static HandlerThread mThread;
    private static final String model = SystemProperties.get("ro.vivo.product.model", "unkown").toLowerCase();
    private String[] CameraReductionCommandsBody;
    private String[] CameraReductionCommandsHead;
    private String[] CameraReductionCommandsOnC2K;
    private String[] CameraReductionCommandsOnC2KWhite;
    private String[] CameraReductionCommandsWhiteBody;
    private String[] CameraReductionCommandsWhiteHead;
    private String[] ReductionCommandsBody;
    private String[] ReductionCommandsHead;
    private String[] ReductionCommandsOnC2K;
    private String[] ReductionCommandsOnC2KWhite;
    private String[] ReductionCommandsWhiteBody;
    private String[] ReductionCommandsWhiteHead;
    private boolean isBootCompleted;
    private boolean isOpenFrontCamera;
    private CommandConfig mCommandConfig;
    private ContentResolver mContentResolver;
    private Looper mMainLooper;
    private final BroadcastReceiver mPhoneEventReceiver;
    private PhoneInterface mPhoneInterface;
    private PhoneServiceConnection mPhoneServiceConnection;
    private PowerChangeHandler mPowerChangeHandler;
    private Runnable mRegisterRunnable;
    private final BroadcastReceiver mSarCongigParsedReceiver;
    private SarSettingsObserver mSarSettingsObserver;

    private void ignoreRfDetect() {
        String model2 = SystemProperties.get("ro.vivo.product.model").toLowerCase();
        this.mIgnoreRfdetect = false;
        if (model2.startsWith("pd1913")) {
            try {
                FileInputStream mInputStream = new FileInputStream(BOARD_VERSION);
                byte[] buf = new byte[100];
                int len = mInputStream.read(buf);
                String board_version = new String(buf, 0, len);
                char[] temp = board_version.toCharArray();
                mInputStream.close();
                if (temp[3] == '1' && temp[4] == '1' && temp[5] == '0') {
                    this.mIgnoreRfdetect = true;
                } else if (temp[3] == '0' && temp[4] == '1' && temp[5] == '0') {
                    this.mIgnoreRfdetect = true;
                }
                SElog.e(TAG, "borad version: " + board_version + " mIgnoreRfdetect " + this.mIgnoreRfdetect);
            } catch (Exception e) {
                System.out.println(e);
                this.mIgnoreRfdetect = false;
            }
        }
    }

    public VivoMtkSarPowerStateController(VivoSarPowerStateService service, Context contxt) {
        super(service);
        this.ReductionCommandsHead = new String[]{"AT+ERFTX=9,16,16,16,32,32,32"};
        this.ReductionCommandsBody = new String[]{"AT+ERFTX=9,16,16,16,32,32,32"};
        this.ReductionCommandsWhiteHead = new String[]{"AT+ERFTX=9,16,16,16,32,32,32"};
        this.ReductionCommandsWhiteBody = new String[]{"AT+ERFTX=9,16,16,16,32,32,32"};
        this.ReductionCommandsOnC2K = new String[]{"AT+ERFTX=4,8,0,8,16"};
        this.ReductionCommandsOnC2KWhite = new String[]{"AT+ERFTX=4,8,0,8,16"};
        this.CameraReductionCommandsHead = new String[]{"AT+ERFTX=9,16,16,16,32,32,32"};
        this.CameraReductionCommandsBody = new String[]{"AT+ERFTX=9,16,16,16,32,32,32"};
        this.CameraReductionCommandsWhiteHead = new String[]{"AT+ERFTX=9,16,16,16,32,32,32"};
        this.CameraReductionCommandsWhiteBody = new String[]{"AT+ERFTX=9,16,16,16,32,32,32"};
        this.CameraReductionCommandsOnC2K = new String[]{"AT+ERFTX=4,8,0,8,16"};
        this.CameraReductionCommandsOnC2KWhite = new String[]{"AT+ERFTX=4,8,0,8,16"};
        this.mPhoneInterface = null;
        this.mPhoneServiceConnection = null;
        this.isBootCompleted = false;
        this.mRegisterRunnable = new Runnable() { // from class: com.vivo.services.sarpower.VivoMtkSarPowerStateController.1
            @Override // java.lang.Runnable
            public void run() {
                VivoMtkSarPowerStateController.this.registerObserver();
            }
        };
        this.mSarCongigParsedReceiver = new BroadcastReceiver() { // from class: com.vivo.services.sarpower.VivoMtkSarPowerStateController.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (VivoMtkSarPowerStateController.VIVO_MTK_SAR_CONFIG_PARSED.equals(action)) {
                    SElog.d(VivoMtkSarPowerStateController.TAG, "mSarCongigParsedReceiver action:" + action);
                    VivoMtkSarPowerStateController.this.updateSarCommandConfig();
                }
            }
        };
        this.isOpenFrontCamera = false;
        this.mPhoneEventReceiver = new BroadcastReceiver() { // from class: com.vivo.services.sarpower.VivoMtkSarPowerStateController.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                SElog.d(VivoMtkSarPowerStateController.TAG, "mPhoneEventReceiver action:" + action);
                if (VivoMtkSarPowerStateController.ACTION_OPEN_CAMERA.equals(action)) {
                    try {
                        String cameraIDStr = intent.getStringExtra(VivoMtkSarPowerStateController.CAMERA_ID);
                        String facewake = intent.getStringExtra("packageName");
                        SElog.d(VivoMtkSarPowerStateController.TAG, "mPhoneEventReceiver:  cameraIDStr " + cameraIDStr + " packageName " + facewake);
                        int cameraID = Integer.parseInt(cameraIDStr);
                        if (cameraID == 1 && !facewake.equals("com.vivo.faceunlock")) {
                            SElog.d(VivoMtkSarPowerStateController.TAG, "mPhoneEventReceiver: start sar power for camera");
                            VivoMtkSarPowerStateController.this.mPowerChangeHandler.removeMessages(3);
                            VivoMtkSarPowerStateController.this.mPowerChangeHandler.removeMessages(4);
                            VivoMtkSarPowerStateController.this.mPowerChangeHandler.sendMessageDelayed(VivoMtkSarPowerStateController.this.mPowerChangeHandler.obtainMessage(4), 0L);
                            VivoMtkSarPowerStateController.this.mPowerChangeHandler.removeMessages(3);
                            VivoMtkSarPowerStateController.this.mPowerChangeHandler.sendMessageDelayed(VivoMtkSarPowerStateController.this.mPowerChangeHandler.obtainMessage(3), 1000L);
                        }
                    } catch (Exception e) {
                        SElog.d(VivoMtkSarPowerStateController.TAG, "ACTION_OPEN_CAMERA exp");
                    }
                } else if (VivoMtkSarPowerStateController.ACTION_CLOSE_CAMERA.equals(action)) {
                    try {
                        String cameraIDStr2 = intent.getStringExtra(VivoMtkSarPowerStateController.CAMERA_ID);
                        SElog.d(VivoMtkSarPowerStateController.TAG, "mPhoneEventReceiver:  cameraIDStr " + cameraIDStr2);
                        Integer.parseInt(cameraIDStr2);
                    } catch (Exception e2) {
                        SElog.d(VivoMtkSarPowerStateController.TAG, "ACTION_OPEN_CAMERA exp");
                    }
                } else if (VivoMtkSarPowerStateController.VIVO_MTK_SAR_CONFIG_PARSED.equals(action)) {
                    try {
                        VivoMtkSarPowerStateController.this.updateSarCommandConfig();
                    } catch (Exception e3) {
                        SElog.d(VivoMtkSarPowerStateController.TAG, "VIVO_MTK_SAR_CONFIG_PARSED exp");
                    }
                }
            }
        };
        mContext = contxt;
        this.mContentResolver = contxt.getContentResolver();
        HandlerThread handlerThread = new HandlerThread("SarPowerStateService_Mtk");
        mThread = handlerThread;
        handlerThread.start();
        this.mMainLooper = mThread.getLooper();
        this.mCommandConfig = new CommandConfig(this.mMainLooper, contxt);
        this.mPowerChangeHandler = new PowerChangeHandler(this.mMainLooper);
        this.mSarSettingsObserver = new SarSettingsObserver(this.mPowerChangeHandler);
        ignoreRfDetect();
        updateSarCommandConfig();
        this.mPowerChangeHandler.post(this.mRegisterRunnable);
        IntentFilter filterSarConfigParsed = new IntentFilter();
        filterSarConfigParsed.addAction(VIVO_MTK_SAR_CONFIG_PARSED);
        if (model.equals("pd1813f_ex") || model.equals("pd1813bf_ex")) {
            filterSarConfigParsed.addAction(ACTION_OPEN_CAMERA);
            filterSarConfigParsed.addAction(ACTION_CLOSE_CAMERA);
        }
        mContext.registerReceiver(this.mPhoneEventReceiver, filterSarConfigParsed);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSarCommandConfig() {
        this.mCommandConfig.updateSarCommands();
        this.ReductionCommandsHead = this.mCommandConfig.mSarCommandsHead;
        this.ReductionCommandsBody = this.mCommandConfig.mSarCommandsBody;
        this.ReductionCommandsWhiteHead = this.mCommandConfig.mSarCommandsWhiteHead;
        this.ReductionCommandsWhiteBody = this.mCommandConfig.mSarCommandsWhiteBody;
        this.ReductionCommandsOnC2K = this.mCommandConfig.mSarCommandsOnC2K;
        this.ReductionCommandsOnC2KWhite = this.mCommandConfig.mSarCommandsOnC2KWhite;
        this.CameraReductionCommandsHead = this.mCommandConfig.mCameraSarCommandsHead;
        this.CameraReductionCommandsBody = this.mCommandConfig.mCameraSarCommandsBody;
        this.CameraReductionCommandsWhiteHead = this.mCommandConfig.mCameraSarCommandsWhiteHead;
        this.CameraReductionCommandsWhiteBody = this.mCommandConfig.mCameraSarCommandsWhiteBody;
        this.CameraReductionCommandsOnC2K = this.mCommandConfig.mCameraSarCommandsOnC2K;
        this.CameraReductionCommandsOnC2KWhite = this.mCommandConfig.mCameraSarCommandsOnC2KWhite;
        SElog.d(TAG, "updateSarCommandConfig");
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
            byte powerState;
            int i = msg.what;
            if (i != 0) {
                if (i == 2) {
                    VivoMtkSarPowerStateController.this.updateSarCommandConfig();
                    VivoMtkSarPowerStateController.this.notifyForceUpdateState();
                    VivoMtkSarPowerStateController.this.handleSarMessage(0, 500);
                    SElog.i(VivoMtkSarPowerStateController.TAG, "MSG_SAR_UPDATE_PARAM update params");
                    return;
                } else if (i == 3) {
                    VivoMtkSarPowerStateController.this.isOpenFrontCamera = false;
                    VivoMtkSarPowerStateController.this.mForceUpdateState = true;
                    VivoMtkSarPowerStateController.this.handleSarMessage(0, 0);
                    return;
                } else if (i == 4) {
                    VivoMtkSarPowerStateController.this.isOpenFrontCamera = true;
                    VivoMtkSarPowerStateController.this.mForceUpdateState = true;
                    VivoMtkSarPowerStateController.this.handleSarMessage(0, 0);
                    return;
                } else {
                    SElog.d(VivoMtkSarPowerStateController.TAG, "PowerChangeHandler default, the mProximityState is" + VivoMtkSarPowerStateController.this.mProximityState);
                    return;
                }
            }
            if (VivoMtkSarPowerStateController.this.mSAR_A_state == 1) {
                VivoMtkSarPowerStateController.this.mForceUpdateState = true;
                powerState = 2;
            } else if (VivoMtkSarPowerStateController.this.mSAR_A_state == 2) {
                VivoMtkSarPowerStateController.this.mForceUpdateState = true;
                powerState = 0;
            } else if (!VivoMtkSarPowerStateController.this.mIgnoreRfdetect) {
                if (VivoMtkSarPowerStateController.this.mSarPowerRfDetectState != 1) {
                    if (VivoMtkSarPowerStateController.this.mCardOneState == 1 || VivoMtkSarPowerStateController.this.mCardTwoState == 1) {
                        if (VivoMtkSarPowerStateController.this.mProximityState == 0) {
                            powerState = 1;
                        } else {
                            powerState = 2;
                        }
                    } else if (VivoMtkSarPowerStateController.this.mProximityState == 0 && VivoMtkSarPowerStateController.this.mScreenState == 0) {
                        powerState = 1;
                    } else {
                        powerState = 2;
                    }
                } else {
                    powerState = 0;
                }
            } else if (VivoMtkSarPowerStateController.this.mSAR_A_state != 0 || VivoMtkSarPowerStateController.this.mProximityState != 5) {
                if (VivoMtkSarPowerStateController.this.mSAR_A_state == 5 && VivoMtkSarPowerStateController.this.mProximityState == 0) {
                    powerState = 1;
                } else {
                    powerState = 0;
                }
            } else {
                powerState = 2;
            }
            SElog.d(VivoMtkSarPowerStateController.TAG, "PowerChangeHandler power change, mProximityState = " + VivoMtkSarPowerStateController.this.mProximityState + ", mScreenState = " + VivoMtkSarPowerStateController.this.mScreenState + ", mSAR_A_state = " + VivoMtkSarPowerStateController.this.mSAR_A_state + ", mSarPowerRfDetectState = " + VivoMtkSarPowerStateController.this.mSarPowerRfDetectState + ", mCardOneState = " + VivoMtkSarPowerStateController.this.mCardOneState + ", mCardTwoState = " + VivoMtkSarPowerStateController.this.mCardTwoState + ", mLastCardState = " + VivoMtkSarPowerStateController.this.mLastCardState + ", powerState = " + ((int) powerState) + ", mLastSarPowerState = " + VivoMtkSarPowerStateController.this.mLastSarPowerState + ", mForceUpdateState = " + VivoMtkSarPowerStateController.this.mForceUpdateState);
            if (VivoMtkSarPowerStateController.this.mLastSarPowerState != powerState || VivoMtkSarPowerStateController.this.mForceUpdateState) {
                VivoMtkSarPowerStateController.this.mLastSarPowerState = powerState;
                if (VivoMtkSarPowerStateController.this.isBootCompleted) {
                    if (VivoMtkSarPowerStateController.this.mForceUpdateState) {
                        VivoMtkSarPowerStateController.this.mForceUpdateState = false;
                    }
                    VivoMtkSarPowerStateController.this.handleSarPowerReduction(powerState);
                    return;
                }
                SElog.d(VivoMtkSarPowerStateController.TAG, "not boot completed, so do not react");
            }
        }
    }

    private void startPhoneService() {
        Intent intent = new Intent();
        intent.setClassName("com.iqoo.engineermode", "com.iqoo.engineermode.PhoneService");
        this.mPhoneServiceConnection = new PhoneServiceConnection();
        SElog.d(TAG, "startPhoneService");
        mContext.bindService(intent, this.mPhoneServiceConnection, 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSarPowerReduction(byte enable) {
        String[] ReductionCommands;
        int powerState;
        String[] ReductionCommands2;
        String[] ReductionCommands3;
        int powerState2;
        String str = ResetCommand_All_On_C2K_NEW;
        String str2 = ResetCommand_All;
        if (enable != 0) {
            try {
                if (!rejectReduceSar()) {
                    if (enable == 1) {
                        if (this.isOpenFrontCamera) {
                            if (this.mCardOneState != 1 && this.mCardTwoState != 1) {
                                ReductionCommands2 = this.CameraReductionCommandsHead;
                            }
                            ReductionCommands2 = this.CameraReductionCommandsWhiteHead;
                        } else {
                            if (this.mCardOneState != 1 && this.mCardTwoState != 1) {
                                ReductionCommands3 = this.ReductionCommandsHead;
                                if (this.mCardOneState != 1 && this.mCardTwoState != 1) {
                                    powerState2 = 1;
                                    SystemProperties.set("sys.sar.dsi", String.valueOf(powerState2));
                                    ReductionCommands2 = ReductionCommands3;
                                }
                                powerState2 = 3;
                                SystemProperties.set("sys.sar.dsi", String.valueOf(powerState2));
                                ReductionCommands2 = ReductionCommands3;
                            }
                            ReductionCommands3 = this.ReductionCommandsWhiteHead;
                            if (this.mCardOneState != 1) {
                                powerState2 = 1;
                                SystemProperties.set("sys.sar.dsi", String.valueOf(powerState2));
                                ReductionCommands2 = ReductionCommands3;
                            }
                            powerState2 = 3;
                            SystemProperties.set("sys.sar.dsi", String.valueOf(powerState2));
                            ReductionCommands2 = ReductionCommands3;
                        }
                    } else if (this.isOpenFrontCamera) {
                        if (this.mCardOneState != 1 && this.mCardTwoState != 1) {
                            ReductionCommands2 = this.CameraReductionCommandsBody;
                        }
                        ReductionCommands2 = this.CameraReductionCommandsWhiteBody;
                    } else {
                        if (this.mCardOneState != 1 && this.mCardTwoState != 1) {
                            ReductionCommands = this.ReductionCommandsBody;
                            if (this.mCardOneState != 1 && this.mCardTwoState != 1) {
                                powerState = 2;
                                SystemProperties.set("sys.sar.dsi", String.valueOf(powerState));
                                ReductionCommands2 = ReductionCommands;
                            }
                            powerState = 4;
                            SystemProperties.set("sys.sar.dsi", String.valueOf(powerState));
                            ReductionCommands2 = ReductionCommands;
                        }
                        ReductionCommands = this.ReductionCommandsWhiteBody;
                        if (this.mCardOneState != 1) {
                            powerState = 2;
                            SystemProperties.set("sys.sar.dsi", String.valueOf(powerState));
                            ReductionCommands2 = ReductionCommands;
                        }
                        powerState = 4;
                        SystemProperties.set("sys.sar.dsi", String.valueOf(powerState));
                        ReductionCommands2 = ReductionCommands;
                    }
                    if (this.mCardOneState != 1 && this.mCardTwoState != 1) {
                        SElog.e(TAG, "reduce power");
                        if (this.mCommandConfig.mResetGSM != null) {
                            str2 = this.mCommandConfig.mResetGSM;
                        }
                        sendCommand(str2, 0);
                        if (this.mCommandConfig.mResetC2K != null) {
                            str = this.mCommandConfig.mResetC2K;
                        }
                        sendCommand(str, 0);
                        Thread.sleep(100L);
                        if (ReductionCommands2.length != 0) {
                            if (!ReductionCommands2[0].equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                                for (String str3 : ReductionCommands2) {
                                    sendCommand(str3, 0);
                                    Thread.sleep(100L);
                                }
                            } else {
                                SElog.e(TAG, "common command is empty, no need to reduce power.");
                            }
                        } else {
                            SElog.e(TAG, "common command not config, please check it.");
                        }
                        if (this.ReductionCommandsOnC2K.length != 0) {
                            if (!this.ReductionCommandsOnC2K[0].equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                                for (int i = 0; i < this.ReductionCommandsOnC2K.length; i++) {
                                    sendCommand(this.ReductionCommandsOnC2K[i], 1);
                                    Thread.sleep(100L);
                                }
                            } else {
                                SElog.e(TAG, "c2k command is empty, no need to reduce power.");
                            }
                        } else {
                            SElog.e(TAG, "c2k command not config, please check it.");
                        }
                        return;
                    }
                    SElog.e(TAG, "reduce power for white card");
                    if (this.mCommandConfig.mResetGSM != null) {
                        str2 = this.mCommandConfig.mResetGSM;
                    }
                    sendCommand(str2, 0);
                    if (this.mCommandConfig.mResetC2K != null) {
                        str = this.mCommandConfig.mResetC2K;
                    }
                    sendCommand(str, 0);
                    Thread.sleep(100L);
                    if (ReductionCommands2.length != 0) {
                        if (!ReductionCommands2[0].equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                            for (String str4 : ReductionCommands2) {
                                sendCommand(str4, 0);
                                Thread.sleep(100L);
                            }
                        } else {
                            SElog.e(TAG, "common command is empty, no need to reduce power.");
                        }
                    } else {
                        SElog.e(TAG, "common command not config, please check it.");
                    }
                    if (this.ReductionCommandsOnC2KWhite.length != 0) {
                        if (!this.ReductionCommandsOnC2KWhite[0].equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                            for (int i2 = 0; i2 < this.ReductionCommandsOnC2KWhite.length; i2++) {
                                sendCommand(this.ReductionCommandsOnC2KWhite[i2], 1);
                                Thread.sleep(100L);
                            }
                        } else {
                            SElog.e(TAG, "c2k command is empty, no need to reduce power.");
                        }
                    } else {
                        SElog.e(TAG, "c2k command not config, please check it.");
                    }
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
        }
        SElog.e(TAG, "reset power");
        SystemProperties.set("sys.sar.dsi", String.valueOf(0));
        if (this.mCommandConfig.mResetGSM != null) {
            str2 = this.mCommandConfig.mResetGSM;
        }
        sendCommand(str2, 0);
        if (this.mCommandConfig.mResetC2K != null) {
            str = this.mCommandConfig.mResetC2K;
        }
        sendCommand(str, 0);
        if (this.ReductionCommandsOnC2K.length == 0 && this.ReductionCommandsOnC2KWhite.length == 0) {
            SElog.e(TAG, "c2k command not config, please check it.");
            return;
        }
        if (this.ReductionCommandsOnC2K[0].equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && this.ReductionCommandsOnC2KWhite[0].equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
            SElog.e(TAG, "c2k command is empty, no need to reset power.");
            return;
        }
        sendCommand(ResetCommand_All_On_C2K, 1);
    }

    private boolean rejectReduceSar() {
        return this.isUnderFactoryMode && AblConfig.EngineModeRejectReduceSar();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class PhoneServiceConnection implements ServiceConnection {
        PhoneServiceConnection() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            SElog.d(VivoMtkSarPowerStateController.TAG, "onServiceConnected:" + name);
            SElog.d(VivoMtkSarPowerStateController.TAG, "onServiceConnected:" + service);
            try {
                VivoMtkSarPowerStateController.this.mPhoneInterface = PhoneInterface.Stub.asInterface(service);
            } catch (Exception e) {
                SElog.e(VivoMtkSarPowerStateController.TAG, "Exception", e);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            SElog.d(VivoMtkSarPowerStateController.TAG, "onServiceDisconnected:" + name);
        }
    }

    private void stopPhoneService() {
        try {
            if (this.mPhoneInterface != null) {
                SElog.d(TAG, "stopPservice");
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
            VivoMtkSarPowerStateController.this.handleSettingsChanged();
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
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void dump(PrintWriter pw) {
        pw.println(String.format("---- %s using config----", TAG));
        pw.println("Head:" + Arrays.toString(this.ReductionCommandsHead));
        pw.println("Body:" + Arrays.toString(this.ReductionCommandsBody));
        pw.println("WhiteHead:" + Arrays.toString(this.ReductionCommandsWhiteHead));
        pw.println("WhiteBody:" + Arrays.toString(this.ReductionCommandsWhiteBody));
        pw.println("OnC2K:" + Arrays.toString(this.ReductionCommandsOnC2K));
        pw.println("OnC2KWhite:" + Arrays.toString(this.ReductionCommandsOnC2KWhite));
        pw.println("CameraHead:" + Arrays.toString(this.CameraReductionCommandsHead));
        pw.println("CameraBody:" + Arrays.toString(this.CameraReductionCommandsBody));
        pw.println("CameraWhiteHead:" + Arrays.toString(this.CameraReductionCommandsWhiteHead));
        pw.println("CameraWhiteBody:" + Arrays.toString(this.CameraReductionCommandsWhiteBody));
        pw.println("CameraOnC2K:" + Arrays.toString(this.CameraReductionCommandsOnC2K));
        pw.println("CameraOnC2KWhite:" + Arrays.toString(this.CameraReductionCommandsOnC2KWhite));
        pw.println("--------------- end of using config ----------------------");
        pw.println("------------- full config info--------------");
        this.mCommandConfig.dump(pw);
        pw.println("---- end ----");
    }
}