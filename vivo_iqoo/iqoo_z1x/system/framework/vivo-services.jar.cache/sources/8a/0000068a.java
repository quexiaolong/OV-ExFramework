package com.vivo.services.popupcamera;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.FtFeature;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import com.android.server.UiThread;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.sensor.NativeNotification.NativeNotificationImpl;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import com.vivo.services.popupcamera.ApplicationProcessStateHelper;
import com.vivo.services.popupcamera.PopupFrontCameraPermissionHelper;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.rms.display.SceneManager;
import com.vivo.services.security.server.VivoPermissionUtils;
import com.vivo.services.superresolution.Constant;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import vivo.app.popupcamera.IPopupCameraManager;
import vivo.util.VivoThemeUtil;

/* loaded from: classes.dex */
public class PopupCameraManagerService extends IPopupCameraManager.Stub {
    private static final int BACK_CAMERA_CLOSED = 4;
    private static final int BACK_CAMERA_OPENED = 2;
    private static final int CAMERA_FINISHED_POP = 2;
    private static final String CAMERA_SERVICE_BINDER_NAME = "media.camera";
    private static final int CAMERA_START_POP = 1;
    private static final int CHECK_DELAY_TIME_AFTER_HOME_KEY = 3000;
    private static final int CHECK_SCREEN_ON_INTERVAL = 400;
    private static final boolean DBG = true;
    private static final boolean EMULATE_HOME_KEY_ENALBE = true;
    private static final String FORBIDDEN_BACKGROUND_POPUP_FRONT_CAMERA_CONFIG_FILE = "/data/bbkcore/forbidden_background_popup_front_camera.xml";
    private static final String FRONT_CAMERA_AUDIO_NOTIFICATION_BASE_DIR = "/system/media/audio/ui/";
    private static final int FRONT_CAMERA_CLOSED = 1;
    private static final boolean FRONT_CAMERA_NOTIFICATION_ENABLE = true;
    private static final int FRONT_CAMERA_OPENED = 0;
    private static final int FRONT_CAMERA_POPUP_FOR_FACEUNLOCK = 5;
    private static final String FRONT_CAMERA_PRESSED_ACTION = "vivo.intent.action.FRONT_CAMERA_PRESSED";
    private static final String FRONT_CAMERA_PRESSED_EXTRA_TIME = "press-time";
    private static final int FRONT_CAMERA_PUSHED_FOR_FACEUNLOCK = 6;
    private static final int FRONT_CAMERA_TEMPERATURE_OVERHEAD_WARNING_TIMES_IN_MILLIS = 5000;
    private static final int MAX_FRONT_CAMERA_USE_TIMES_FOR_30_SECONDS = 10;
    static final int MSG_BACK_CAMERA_CLOSED = 10;
    static final int MSG_BACK_CAMERA_OPENED = 9;
    static final int MSG_CAMERASERVER_DIED = 14;
    static final int MSG_CHECK_FRONT_CAMERA_IS_CLOSED = 15;
    static final int MSG_CHECK_SCREEN_ON = 24;
    static final int MSG_DELAY_TAKE_BACK_FRONT_CAMERA_AGAIN = 16;
    static final int MSG_FRONT_CAMERA_CLOSED = 7;
    static final int MSG_FRONT_CAMERA_CLOSED_POLL = 11;
    static final int MSG_FRONT_CAMERA_OPENED = 8;
    static final int MSG_HANDLE_DATA_COLLECT = 17;
    static final int MSG_POLL_STATUS_CANCELED = 0;
    static final int MSG_POLL_STATUS_INVALID = -1;
    static final int MSG_POLL_STATUS_POPUP_JAMMED = 4;
    static final int MSG_POLL_STATUS_POPUP_OK = 2;
    static final int MSG_POLL_STATUS_PRESSED = 5;
    static final int MSG_POLL_STATUS_PUSH_JAMMED = 3;
    static final int MSG_POLL_STATUS_PUSH_OK = 1;
    static final int MSG_POP_UP_FRONT_CAMERA = 13;
    static final int MSG_PUSH_FOR_FACEUNLOCK = 22;
    static final int MSG_SCREEN_OFF = 20;
    static final int MSG_SCREEN_ON = 19;
    static final int MSG_SHORT_TAKEBACK = 21;
    static final int MSG_STOP_POPUP_LIGHT = 23;
    static final int MSG_STOP_POPUP_VIBRATE = 26;
    static final int MSG_TAKE_BACK_FRONT_CAMERA = 12;
    static final int MSG_TAKE_BACK_FRONT_CAMERA_FOR_DROP = 18;
    static final int MSG_UI_FRONT_CAMERA_DROP_PROTECT = 4;
    static final int MSG_UI_FRONT_CAMERA_POPUP_JAMMED = 0;
    static final int MSG_UI_FRONT_CAMERA_PRESSED = 3;
    static final int MSG_UI_FRONT_CAMERA_PUSH_JAMMED = 1;
    static final int MSG_UI_FRONT_CAMERA_TOO_FREQUENT = 2;
    private static final int SHORT_TAKEBACK_TIME = 400;
    private static final int SHORT_TAKE_BACK_FRONT_CAMERA_TIMEOUT_IN_MILLIS = 2000;
    private static final int STOP_POPUP_LIGHT_DELAY_IN_MILLIS = 3000;
    static final String TAG = "PopupCameraManagerService";
    private static final int TAKE_BACK_FRONT_CAMERA_AGAIN_AFTER_JAMMED_INTERVALS = 20000;
    private static final int TAKE_BACK_FRONT_CAMERA_MAX_RETRY_TIMES = 5;
    private static final int TAKE_BACK_FRONT_CAMERA_TIMEOUT_IN_MILLIS = 5000;
    private static final int TEMPERATURE_MONITOR_INTERVAL_IN_MILLIS = 30000;
    private static PopupCameraManagerService sInstance;
    private boolean isDropDownDetectedSupported;
    private AudioManager mAudioManager;
    private CameraStatus mBackCameraStatus;
    private IBinder mCameraServiceBinder;
    private CameraServerDeathRecipient mCameraServiceDeatchRecipient;
    private Context mContext;
    private DataAnalysisHelper mDataAnalysisHelper;
    private MySensorEventListener mDropDetectSensorEventListener;
    private int mDropDetectSensorType;
    private Sensor mDropDownSensor;
    private MySensorEventListener mDropDownSensorEventListener;
    private int mDropDownSensorType;
    private FrontCameraDropProtectDialog mDropProtectDialog;
    private FileObserver mFileObserver;
    private volatile boolean mForceDelayTakebackFrontCamera;
    private int mFrontCameraPopupStreamId;
    private int mFrontCameraPushStreamId;
    private CameraStatus mFrontCameraStatus;
    private PowerManager.WakeLock mHallWakeLock;
    private Handler mMainHandler;
    private HandlerThread mMainHandlerThread;
    private NativeNotificationImpl mNativeNotificationImpl;
    private CameraStatus mPendingFrontCameraCloseStatus;
    private CameraPopupTakebackRecord mPendingRecordAddToQueue;
    private FrontCameraErrorDialog mPopupJammedDialog;
    private PowerManager mPowerManager;
    private FrontCameraPressedDialog mPressedDialog;
    private FrontCameraJammedConfirmDialog mPushJammedDialog;
    private ScreenStatusReceiver mScreenStatusReceiver;
    private SensorManager mSensorManager;
    private SoundPool mSoundPool;
    private FrontCameraTemperatureProtectDialog mTemperatureProtectDialog;
    private Handler mUIHandler;
    private CameraPopupPermissionCheckDialog permissionCheckDialog;
    private static ArrayList<String> SYSTEM_CAMERA_APP_PACKAGE_LIST = new ArrayList<String>() { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.1
        {
            add("com.android.camera");
        }
    };
    private static ArrayList<String> SHORT_TAKEBACK_PACKAGE_LIST = new ArrayList<String>() { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.2
        {
            add(Constant.APP_WEIXIN);
            add("com.eg.android.AlipayGphone");
            add("com.tencent.mobileqq");
            add("com.alibaba.android.rimet");
            add("im.yixin");
            add("jp.naver.line.android");
            add("com.skype.raider");
            add("com.whatsapp");
            add("com.facebook.katana");
            add("com.kugou.fanxing");
            add(Constant.APP_HUYA);
            add(Constant.APP_DOUYU);
            add("com.meelive.ingkee");
            add("com.kascend.chushou");
            add("tv.xiaoka.live");
            add("com.huajiao");
            add("com.panda.videoliveplatform");
            add("com.tencent.now");
            add("com.smile.gifmaker");
            add(Constant.APP_DOUYIN);
            add("com.meitu.meiyancamera");
            add("com.mt.mtxx.mtxx");
            add("com.meitu.meipaimv");
            add("com.meitu.wheecam");
            add("com.vivo.faceunlock");
            add("com.facebook.orca");
            add("com.instagram.android");
            add("com.vivo.bsptest");
            add("com.iqoo.engineermode");
            add("com.vivo.wallet");
        }
    };
    private static ArrayList<String> WARN_POPUP_CAMERA_PACKAGE_LIST = new ArrayList<String>() { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.3
        {
            add("cn.net.cyberway");
        }
    };
    private static ArrayList<String> BACKGROUND_USE_FRONT_CAMERA_PACKAGE_WHITE_LIST = new ArrayList<String>() { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.4
        {
            add("com.sohu.inputmethod.sogou.vivo");
            add("org.codeaurora.ims");
            add("android.camera.cts");
            add("android.camera.cts.api25test");
            add("com.android.cts.verifier");
            add("com.vivo.findphone");
            add(VivoPermissionUtils.OS_PKG);
            add("com.facebook.orca");
        }
    };
    private static ArrayList<String> FREQUENT_USE_FRONT_CAMERA_PACKAGE_WHITE_LIST = new ArrayList<String>() { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.5
        {
            add(".cts");
        }
    };
    private static final HashMap<Integer, AudioNotification> mAudioNotificationMap = new HashMap<>();
    private volatile boolean mHavePendingPopupTask = false;
    private PendingPopupTask mPendingPopupTask = null;
    private volatile boolean isFrontCameraOpened = false;
    private LimitQueue<CameraPopupTakebackRecord> cameraPopupAndTakebackRecords = new LimitQueue<>(10);
    private Object mFrontCameraStatusLock = new Object();
    private boolean isHavingPendingTakeBackTask = false;
    private long mTakeBackTaskStartTime = 0;
    private int mLastFrontCameraStatus = -1;
    private int mCurrentFrontCameraStatus = 1;
    private volatile boolean isFrontCameraPopup = false;
    private volatile boolean isSystemShutdowning = false;
    private volatile boolean mHavaPendingOpenVibStepTask = false;
    private volatile boolean mHavePendingCloseVibStepTask = false;
    private volatile boolean isLastInGalleryActivity = false;
    private volatile boolean isTodoFirstClosedPoll = false;
    private volatile int mCurrentRetryTimes = 0;
    private volatile boolean isFaceUnlocking = false;
    private Object mForbiddenListLock = new Object();
    private ArrayList<ForbiddenItem> mForbiddenList = new ArrayList<>();
    private volatile int mImmediatelyTakebackTime = ProcessList.HEAVY_WEIGHT_APP_ADJ;
    private Runnable mConfigFileObserveRunnable = new Runnable() { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.6
        @Override // java.lang.Runnable
        public void run() {
            VLog.d(PopupCameraManagerService.TAG, "mConfigFileObserveRunnable");
            PopupCameraManagerService.this.parseConfigFile(PopupCameraManagerService.FORBIDDEN_BACKGROUND_POPUP_FRONT_CAMERA_CONFIG_FILE);
            PopupCameraManagerService.this.observeConfigFileChange();
        }
    };
    private Object mCameraServiceBinderLock = new Object();
    private int mLastOpenVibHallCookie = 0;
    private int mLastCloseVibHallCookie = SceneManager.ANIMATION_PRIORITY;
    private volatile boolean isLastPupupJammed = false;
    private volatile boolean isLastTakebackJammed = false;
    private volatile boolean isDropDownListenerRegisted = false;
    private Object mPermissionCheckDialogLock = new Object();
    private volatile boolean isCheckingPopupCameraPermission = false;

    static /* synthetic */ int access$2208(PopupCameraManagerService x0) {
        int i = x0.mCurrentRetryTimes;
        x0.mCurrentRetryTimes = i + 1;
        return i;
    }

    private static boolean isInFrequentUseWhitelist(String packageName) {
        Iterator<String> it = FREQUENT_USE_FRONT_CAMERA_PACKAGE_WHITE_LIST.iterator();
        while (it.hasNext()) {
            String pattern = it.next();
            if (packageName != null && packageName.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class PendingPopupTask {
        public boolean isShowCheckbox;
        public CameraStatus mPendingPopupCameraStatus;
        public boolean mPendingPopupTaskNeedPopupPermissionDialog;
        public PopupFrontCameraPermissionHelper.PopupFrontCameraPermissionState mPermissionState;

        public PendingPopupTask(boolean showDialog, boolean showCheckBox, CameraStatus status, PopupFrontCameraPermissionHelper.PopupFrontCameraPermissionState state) {
            this.mPendingPopupTaskNeedPopupPermissionDialog = showDialog;
            this.isShowCheckbox = showCheckBox;
            this.mPendingPopupCameraStatus = status;
            this.mPermissionState = state;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetPendingPopupTask() {
        this.mHavePendingPopupTask = false;
        this.mPendingPopupTask = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPendingPopupTask(boolean showDialog, boolean showCheckBox, CameraStatus status, PopupFrontCameraPermissionHelper.PopupFrontCameraPermissionState state) {
        this.mHavePendingPopupTask = true;
        this.mPendingPopupTask = new PendingPopupTask(showDialog, showCheckBox, status, state);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class CameraPopupTakebackRecord {
        long popupTimeInMillis;
        long takebackTimeInMillis;

        private CameraPopupTakebackRecord() {
        }

        public String toString() {
            return "front camera record{ popupTimeInMillis=" + timeMillisToString(this.popupTimeInMillis) + " takebackTimeInMillis=" + timeMillisToString(this.takebackTimeInMillis) + " used_time_millis=" + (this.takebackTimeInMillis - this.popupTimeInMillis) + " }";
        }

        public boolean isValid() {
            long j = this.popupTimeInMillis;
            if (j > 0) {
                long j2 = this.takebackTimeInMillis;
                if (j2 > 0 && j2 - j > 0) {
                    return true;
                }
            }
            return false;
        }

        private String timeMillisToString(long timeMillis) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date(timeMillis));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AudioNotification {
        public String popupAudioFile;
        public int popupStreamId;
        public String pushedAudioFile;
        public int pushedStreamId;

        public AudioNotification(String popupFile, String pushFile, SoundPool soundPool) {
            this.popupAudioFile = popupFile;
            this.pushedAudioFile = pushFile;
            if (popupFile != null && soundPool != null) {
                this.popupStreamId = soundPool.load(popupFile, 1);
            } else {
                this.popupStreamId = -1;
            }
            String str = this.pushedAudioFile;
            if (str != null && soundPool != null) {
                this.pushedStreamId = soundPool.load(str, 1);
            } else {
                this.pushedStreamId = -1;
            }
        }
    }

    private PopupCameraManagerService(Context context) {
        SensorManager sensorManager;
        Sensor dropDetectSensor;
        this.mDropDetectSensorType = 66548;
        this.mDropDownSensorType = 66556;
        this.isDropDownDetectedSupported = false;
        this.mDataAnalysisHelper = null;
        VLog.d(TAG, "PopupCameraManagerService construct");
        this.mContext = context;
        PopupCameraLightManager.initLightManager(context);
        this.mFrontCameraStatus = new CameraStatus(1);
        this.mBackCameraStatus = new CameraStatus(0);
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mMainHandlerThread = handlerThread;
        handlerThread.start();
        this.mMainHandler = new MainHandler(this.mMainHandlerThread.getLooper());
        this.mUIHandler = new UIHandler();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_SHUTDOWN");
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        filter.setPriority(1000);
        this.mContext.registerReceiver(new ShutDownBootReceiver(), filter, null, this.mMainHandler);
        registSreenStatusReceiver();
        this.mCameraServiceDeatchRecipient = new CameraServerDeathRecipient();
        connectCameraServiceLocked();
        VibHallWrapper.initVibHallWrapper(this.mMainHandler);
        initAudioNotificationMap();
        Handler handler = this.mUIHandler;
        if (handler != null) {
            handler.post(new Runnable() { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.7
                @Override // java.lang.Runnable
                public void run() {
                    PopupCameraManagerService.this.mPopupJammedDialog = new FrontCameraErrorDialog(PopupCameraManagerService.this.mContext, true, PopupCameraManagerService.this.mContext.getString(51249511));
                    PopupCameraManagerService.this.mPushJammedDialog = new FrontCameraJammedConfirmDialog(PopupCameraManagerService.this.mContext);
                    PopupCameraManagerService.this.mTemperatureProtectDialog = new FrontCameraTemperatureProtectDialog(PopupCameraManagerService.this.mContext, true, PopupCameraManagerService.this.mContext.getString(51249509), PopupCameraManagerService.this.mContext.getString(51249518));
                    PopupCameraManagerService.this.mPressedDialog = new FrontCameraPressedDialog(PopupCameraManagerService.this.mContext, true, PopupCameraManagerService.this.mContext.getString(51249513), PopupCameraManagerService.this);
                    PopupCameraManagerService.this.mDropProtectDialog = new FrontCameraDropProtectDialog(PopupCameraManagerService.this.mContext, PopupCameraManagerService.this);
                }
            });
        }
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mPowerManager = powerManager;
        PowerManager.WakeLock newWakeLock = powerManager.newWakeLock(1, "PopupCameraManagerService-Hall-WakeLock");
        this.mHallWakeLock = newWakeLock;
        newWakeLock.setReferenceCounted(false);
        readForbiddenBackgroundPopupFrontCameraConfig();
        this.mDropDetectSensorType = getDropDetectSensorTypeByReflect();
        this.isDropDownDetectedSupported = "1".equals(FtFeature.getFeatureAttribute("vivo.hardware.popupcamera", "is_drop_down_detect_support", "0"));
        this.mDropDownSensorType = getDropDownSensorTypeByReflect();
        SensorManager sensorManager2 = (SensorManager) this.mContext.getSystemService("sensor");
        this.mSensorManager = sensorManager2;
        if (sensorManager2 != null) {
            this.mDropDownSensor = sensorManager2.getDefaultSensor(this.mDropDownSensorType);
        }
        this.mDropDetectSensorEventListener = new MySensorEventListener();
        this.mDropDownSensorEventListener = new MySensorEventListener();
        int i = this.mDropDetectSensorType;
        if (i != -1 && (sensorManager = this.mSensorManager) != null && (dropDetectSensor = sensorManager.getDefaultSensor(i)) != null) {
            VLog.d(TAG, "registerListener for drop detect");
            this.mSensorManager.registerListener(this.mDropDetectSensorEventListener, dropDetectSensor, 500000);
        }
        this.mDataAnalysisHelper = DataAnalysisHelper.getInstance(this.mContext);
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        PopupCameraVibrateManager.init(context);
        this.mNativeNotificationImpl = NativeNotificationImpl.getInstance(this.mContext);
    }

    private int getDropDetectSensorTypeByReflect() {
        try {
            Class<?> sensorClass = Class.forName("android.hardware.Sensor");
            Field field = sensorClass.getDeclaredField("TYPE_DROP_DET");
            return field.getInt(sensorClass);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void registDropDownSensorEventListener() {
        VLog.d(TAG, "registDropDownSensorEventListener");
        if (this.isDropDownDetectedSupported && !this.isDropDownListenerRegisted && this.mDropDownSensorType != -1 && this.mSensorManager != null && this.mDropDownSensorEventListener != null && this.mDropDownSensor != null) {
            VLog.d(TAG, "registerListener for drop down");
            this.mSensorManager.registerListener(this.mDropDownSensorEventListener, this.mDropDownSensor, 500000);
            this.isDropDownListenerRegisted = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void unregistDropDownSensorEventListener() {
        VLog.d(TAG, "unregistDropDownSensorEventListener");
        if (this.isDropDownDetectedSupported && this.isDropDownListenerRegisted && this.mDropDownSensorType != -1 && this.mSensorManager != null && this.mDropDownSensorEventListener != null) {
            VLog.d(TAG, "unregisterListener for drop down");
            this.mSensorManager.unregisterListener(this.mDropDownSensorEventListener);
            this.isDropDownListenerRegisted = false;
        }
    }

    private int getDropDownSensorTypeByReflect() {
        try {
            Class<?> sensorClass = Class.forName("android.hardware.Sensor");
            Field field = sensorClass.getDeclaredField("TYPE_DROP_DOWN");
            return field.getInt(sensorClass);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void registSreenStatusReceiver() {
        this.mScreenStatusReceiver = new ScreenStatusReceiver();
        IntentFilter screenStatusIF = new IntentFilter();
        screenStatusIF.addAction("android.intent.action.SCREEN_ON");
        screenStatusIF.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(this.mScreenStatusReceiver, screenStatusIF, null, this.mUIHandler);
    }

    private void readForbiddenBackgroundPopupFrontCameraConfig() {
        parseConfigFile(FORBIDDEN_BACKGROUND_POPUP_FRONT_CAMERA_CONFIG_FILE);
        observeConfigFileChange();
    }

    private ArrayList<ForbiddenItem> parseConfigFromXml(InputStream is) {
        ArrayList<ForbiddenItem> tmp = new ArrayList<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            try {
                parser.setInput(new InputStreamReader(is));
                while (parser.getEventType() != 1) {
                    try {
                        if (parser.getEventType() == 2) {
                            if (!"item".equalsIgnoreCase(parser.getName()) || !"package".equalsIgnoreCase(parser.getAttributeName(0)) || !"mode".equalsIgnoreCase(parser.getAttributeName(1))) {
                                if ("item".equalsIgnoreCase(parser.getName()) && "mode".equalsIgnoreCase(parser.getAttributeName(0)) && "time".equalsIgnoreCase(parser.getAttributeName(1))) {
                                    String mode = parser.getAttributeValue(0);
                                    String time = parser.getAttributeValue(1);
                                    try {
                                        int m = Integer.parseInt(mode);
                                        if (m == 7) {
                                            this.mImmediatelyTakebackTime = Integer.parseInt(time);
                                        }
                                    } catch (Exception e) {
                                        this.mImmediatelyTakebackTime = ProcessList.HEAVY_WEIGHT_APP_ADJ;
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                String packageName = parser.getAttributeValue(0);
                                String mode2 = parser.getAttributeValue(1);
                                tmp.add(new ForbiddenItem(packageName, Integer.parseInt(mode2)));
                            }
                        }
                        parser.next();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                        return tmp;
                    }
                }
                return tmp;
            } catch (Exception e3) {
                e3.printStackTrace();
                return tmp;
            }
        } catch (Exception e4) {
            e4.printStackTrace();
            return tmp;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseConfigFile(String filePath) {
        try {
            File file = new File(filePath);
            String result = FileUtils.readTextFile(file, 0, null);
            if (result != null) {
                VLog.d(TAG, "result = " + result);
                synchronized (this.mForbiddenListLock) {
                    this.mForbiddenList = parseConfigFromXml(new ByteArrayInputStream(result.getBytes()));
                }
            }
        } catch (Exception e) {
            this.mForbiddenList = null;
            VLog.e(TAG, "parseConfigFile error! " + e.fillInStackTrace());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isShouldForbiddenPopupFrontCamera(String packageName, PopupFrontCameraPermissionHelper.PopupFrontCameraPermissionState ps) {
        return ps != null && !ps.isPopupFrontCameraPermissionGranted() && ps.isAlwaysDeny() && ps.isPermissionStateValid();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isShouldTakebackImmediately(CameraStatus status) {
        if (status == null || status.currentStatusPackageName == null) {
            return false;
        }
        synchronized (this.mForbiddenListLock) {
            if (this.mForbiddenList != null) {
                Iterator<ForbiddenItem> it = this.mForbiddenList.iterator();
                while (it.hasNext()) {
                    ForbiddenItem item = it.next();
                    if (item != null && item.packageName != null && item.packageName.equalsIgnoreCase(status.currentStatusPackageName)) {
                        if (item.mode == 4) {
                            return true;
                        }
                        if (item.mode == 6) {
                            return false;
                        }
                    }
                }
            }
            return isInShortTakebackPackageList(status);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isShouldWarnUserPopupCameraPermissionDialog(CameraStatus status, PopupFrontCameraPermissionHelper.PopupFrontCameraPermissionState ps) {
        if (status == null || status.currentStatusPackageName == null || ps == null || !ps.isPermissionStateValid() || ps.isPopupFrontCameraPermissionGranted() || ps.isAlwaysDeny()) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isInBackgroundUseExemptionList(CameraStatus status) {
        if (status == null || status.currentStatusPackageName == null) {
            return false;
        }
        if (isSystemApp(status.currentStatusPackageName)) {
            VLog.d(TAG, "system app " + status.currentStatusPackageName + " is in background exemption list");
            return true;
        } else if (BACKGROUND_USE_FRONT_CAMERA_PACKAGE_WHITE_LIST.contains(status.currentStatusPackageName)) {
            return true;
        } else {
            synchronized (this.mForbiddenListLock) {
                if (this.mForbiddenList == null) {
                    return false;
                }
                Iterator<ForbiddenItem> it = this.mForbiddenList.iterator();
                while (it.hasNext()) {
                    ForbiddenItem item = it.next();
                    if (item != null && item.packageName != null && item.packageName.equalsIgnoreCase(status.currentStatusPackageName) && item.mode == 5) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void observeConfigFileChange() {
        VLog.d(TAG, "observeConfigFileChange");
        FileObserver fileObserver = this.mFileObserver;
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
        File file = new File(FORBIDDEN_BACKGROUND_POPUP_FRONT_CAMERA_CONFIG_FILE);
        try {
            if (!file.exists()) {
                VLog.d(TAG, "/data/bbkcore/forbidden_background_popup_front_camera.xml file not exist ,create new one");
                file.createNewFile();
            }
        } catch (Exception e) {
            VLog.e(TAG, "observeConfigFileChange create file error");
        }
        FileObserver fileObserver2 = new FileObserver(FORBIDDEN_BACKGROUND_POPUP_FRONT_CAMERA_CONFIG_FILE, 1544) { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.8
            @Override // android.os.FileObserver
            public void onEvent(int event, String path) {
                VLog.d(PopupCameraManagerService.TAG, "onEvent=" + event + " path=" + path);
                if (8 == event) {
                    VLog.d(PopupCameraManagerService.TAG, "get CLOSE_WRITE event, parse config again");
                    PopupCameraManagerService.this.parseConfigFile(PopupCameraManagerService.FORBIDDEN_BACKGROUND_POPUP_FRONT_CAMERA_CONFIG_FILE);
                }
                if (event == 1024 || event == 512) {
                    VLog.d(PopupCameraManagerService.TAG, "get DELETE event, delay parse config & watch file again");
                    PopupCameraManagerService.this.mMainHandler.removeCallbacks(PopupCameraManagerService.this.mConfigFileObserveRunnable);
                    PopupCameraManagerService.this.mMainHandler.postDelayed(PopupCameraManagerService.this.mConfigFileObserveRunnable, 5000L);
                }
            }
        };
        this.mFileObserver = fileObserver2;
        fileObserver2.startWatching();
    }

    private void initAudioNotificationMap() {
        this.mSoundPool = new SoundPool(10, 7, 0);
        mAudioNotificationMap.put(0, new AudioNotification(null, null, this.mSoundPool));
        mAudioNotificationMap.put(1, new AudioNotification("/system/media/audio/ui/front_camera_sciencefiction_popuped.ogg", "/system/media/audio/ui/front_camera_sciencefiction_pushed.ogg", this.mSoundPool));
        mAudioNotificationMap.put(2, new AudioNotification("/system/media/audio/ui/front_camera_mechanical_popuped.ogg", "/system/media/audio/ui/front_camera_mechanical_pushed.ogg", this.mSoundPool));
        mAudioNotificationMap.put(3, new AudioNotification("/system/media/audio/ui/front_camera_rhythm_popuped.ogg", "/system/media/audio/ui/front_camera_rhythm_pushed.ogg", this.mSoundPool));
    }

    private int getFrontCameraNotificationModeFromSettings() {
        int userid = ActivityManager.getCurrentUser();
        int mode = Settings.System.getIntForUser(this.mContext.getContentResolver(), "telescopic_camera_sound", 1, userid);
        VLog.d(TAG, "getFrontCameraNotificationModeFromSettings mode = " + mode + "  userid = " + userid);
        return mode;
    }

    private void playFrontCameraPopupAudio() {
        int mode = getFrontCameraNotificationModeFromSettings();
        AudioNotification notify = mAudioNotificationMap.get(Integer.valueOf(mode));
        if (this.mSoundPool != null && notify != null && notify.popupStreamId > 0) {
            this.mSoundPool.play(notify.popupStreamId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    private void playFrontCameraPushAudio() {
        int mode = getFrontCameraNotificationModeFromSettings();
        AudioNotification notify = mAudioNotificationMap.get(Integer.valueOf(mode));
        if (this.mSoundPool != null && notify != null && notify.pushedStreamId > 0) {
            this.mSoundPool.play(notify.pushedStreamId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    private void connectCameraServiceLocked() {
        synchronized (this.mCameraServiceBinderLock) {
            if (this.mCameraServiceBinder == null) {
                IBinder service = ServiceManager.getService(CAMERA_SERVICE_BINDER_NAME);
                this.mCameraServiceBinder = service;
                if (service == null) {
                    VLog.d(TAG, "mCameraServiceBinder is null");
                } else {
                    try {
                        service.linkToDeath(this.mCameraServiceDeatchRecipient, 0);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        VLog.d(TAG, "we can not linkToDeath to cameraserver");
                    }
                }
            }
        }
    }

    public static synchronized PopupCameraManagerService getInstance(Context context) {
        PopupCameraManagerService popupCameraManagerService;
        synchronized (PopupCameraManagerService.class) {
            if (sInstance == null) {
                sInstance = new PopupCameraManagerService(context);
            }
            popupCameraManagerService = sInstance;
        }
        return popupCameraManagerService;
    }

    public boolean popupFrontCamera() {
        VLog.d(TAG, "popupFrontCamera");
        Handler handler = this.mMainHandler;
        if (handler != null) {
            handler.sendEmptyMessage(13);
            return true;
        }
        return false;
    }

    public boolean takeupFrontCamera() {
        VLog.d(TAG, "takeupFrontCamera");
        Handler handler = this.mMainHandler;
        if (handler != null) {
            handler.sendEmptyMessage(12);
            return true;
        }
        return false;
    }

    private int calculateOpenCookie() {
        int i = this.mLastOpenVibHallCookie + 1;
        this.mLastOpenVibHallCookie = i;
        return i;
    }

    private int calculateCloseCookie() {
        int i = this.mLastCloseVibHallCookie + 1;
        this.mLastCloseVibHallCookie = i;
        return i;
    }

    private void recordPopupFrontCamera(CameraStatus cs) {
        long openingTime = SystemClock.elapsedRealtime();
        LimitQueue<CameraPopupTakebackRecord> limitQueue = this.cameraPopupAndTakebackRecords;
        if (limitQueue != null && limitQueue.size() == 10 && openingTime - this.cameraPopupAndTakebackRecords.getFirst().popupTimeInMillis < VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL) {
            if (cs != null && isInFrequentUseWhitelist(cs.currentStatusPackageName)) {
                VLog.d(TAG, "ignore frequent popup for package " + cs.currentStatusPackageName);
            } else {
                VLog.d(TAG, "current popup trigger temperature monitor");
                this.mForceDelayTakebackFrontCamera = true;
                this.mUIHandler.sendEmptyMessage(2);
            }
        } else {
            VLog.d(TAG, "current popup does not trigger temperature monitor");
        }
        if (this.mPendingRecordAddToQueue != null) {
            VLog.d(TAG, "there is maybe something wrong, mPendingRecordAddToQueue is non-null, but open the front camera again,we just skip the last");
            this.mPendingRecordAddToQueue = null;
        }
        VLog.d(TAG, "record front camera opened time");
        CameraPopupTakebackRecord cameraPopupTakebackRecord = new CameraPopupTakebackRecord();
        this.mPendingRecordAddToQueue = cameraPopupTakebackRecord;
        cameraPopupTakebackRecord.popupTimeInMillis = openingTime;
    }

    private void recordTakebackFrontCamera() {
        CameraPopupTakebackRecord cameraPopupTakebackRecord = this.mPendingRecordAddToQueue;
        if (cameraPopupTakebackRecord != null) {
            cameraPopupTakebackRecord.takebackTimeInMillis = SystemClock.elapsedRealtime();
            if (this.mPendingRecordAddToQueue.isValid()) {
                VLog.d(TAG, "put CameraPopupTakebackRecord " + this.mPendingRecordAddToQueue + "to queue");
                this.cameraPopupAndTakebackRecords.offer(this.mPendingRecordAddToQueue);
                this.mPendingRecordAddToQueue = null;
                return;
            }
            return;
        }
        VLog.d(TAG, "there is maybe something wrong, we get takeback camera event , but mPendingRecordAddToQueue is null ,don't record this close event");
    }

    public boolean popupFrontCameraInternal(boolean silent, CameraStatus cs) {
        VLog.d(TAG, "popupFrontCameraInternal isFrontCameraPopup=" + this.isFrontCameraPopup);
        if (this.isFrontCameraPopup) {
            return false;
        }
        this.isFrontCameraPopup = true;
        if (!silent) {
            playFrontCameraPopupAudio();
        }
        if (this.mMainHandler.hasMessages(23)) {
            this.mMainHandler.removeMessages(23);
            VLog.d(TAG, "popup front camera, we remove MSG_STOP_POPUP_LIGHT");
        }
        this.mHavaPendingOpenVibStepTask = true;
        this.isLastPupupJammed = false;
        this.isLastTakebackJammed = false;
        this.mCurrentRetryTimes = 0;
        recordPopupFrontCamera(cs);
        PopupCameraLightManager.startCameraLightForPopup();
        if (this.mMainHandler.hasMessages(26)) {
            this.mMainHandler.removeMessages(26);
        }
        int mode = getFrontCameraNotificationModeFromSettings();
        PopupCameraVibrateManager.startCameraVibrate(mode, true);
        AudioManager audioManager = this.mAudioManager;
        if (audioManager != null) {
            audioManager.setParameters("FrontCameraRising=true");
        }
        this.mNativeNotificationImpl.notifyCameraState(1);
        return VibHallWrapper.openStepVibrator(calculateOpenCookie()) != -1;
    }

    public boolean takeupFrontCameraInternal(boolean isForce, boolean silent) {
        VLog.d(TAG, "takeupFrontCameraInternal isFrontCameraPopup=" + this.isFrontCameraPopup);
        resetCloseFrontCameraStatus();
        if (this.isFrontCameraPopup || isForce) {
            acquireWakeLock(5000L);
            this.isFrontCameraPopup = false;
            if (!silent) {
                playFrontCameraPushAudio();
            }
            this.mHavePendingCloseVibStepTask = true;
            this.isLastTakebackJammed = false;
            this.isLastPupupJammed = false;
            recordTakebackFrontCamera();
            PopupCameraLightManager.startCameraLightForPush();
            int mode = getFrontCameraNotificationModeFromSettings();
            PopupCameraVibrateManager.startCameraVibrate(mode, false);
            AudioManager audioManager = this.mAudioManager;
            if (audioManager != null) {
                audioManager.setParameters("FrontCameraLower=true");
            }
            this.mNativeNotificationImpl.notifyCameraState(1);
            return VibHallWrapper.closeStepVibrator(calculateCloseCookie()) != -1;
        }
        return false;
    }

    public boolean fakeTakeupFrontCameraInternal() {
        VLog.d(TAG, "takeupFrontCameraInternal isFrontCameraPopup=" + this.isFrontCameraPopup);
        resetCloseFrontCameraStatus();
        this.isLastTakebackJammed = false;
        this.isLastPupupJammed = false;
        return false;
    }

    public boolean takeupFrontCameraInternalAfterFalling() {
        VLog.d(TAG, "takeupFrontCameraInternalAfterFalling isFrontCameraPopup=" + this.isFrontCameraPopup);
        if (this.isFrontCameraPopup) {
            return false;
        }
        acquireWakeLock(5000L);
        return VibHallWrapper.closeStepVibratorAfterFalling() != -1;
    }

    public int getFrontCameraStatus() {
        VLog.d(TAG, "getFrontCameraStatus");
        CameraStatus cameraStatus = this.mFrontCameraStatus;
        if (cameraStatus != null) {
            return cameraStatus.getCameraStatus();
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetCloseFrontCameraStatus() {
        this.mPendingFrontCameraCloseStatus = null;
        this.mTakeBackTaskStartTime = 0L;
        this.isHavingPendingTakeBackTask = false;
    }

    private void handleFrontCameraOpened(CameraStatus status) {
        VLog.d(TAG, "handleFrontCameraOpened");
        VLog.d(TAG, "isFrontCameraOpened7=" + this.isFrontCameraOpened);
        if (this.mMainHandler.hasMessages(7)) {
            VLog.d(TAG, "becuaseof open again, we remove message MSG_FRONT_CAMERA_CLOSED");
            this.mMainHandler.removeMessages(7);
        }
        if (this.mMainHandler.hasMessages(21)) {
            VLog.d(TAG, "becuaseof open again, we remove message MSG_SHORT_TAKEBACK");
            this.mMainHandler.removeMessages(21);
        }
        if (this.mMainHandler.hasMessages(11)) {
            VLog.d(TAG, "becuaseof open again, we remove message MSG_FRONT_CAMERA_CLOSED_POLL");
            this.mMainHandler.removeMessages(11);
        }
        if (this.mMainHandler.hasMessages(16)) {
            VLog.d(TAG, "becuaseof open again, we remove message MSG_DELAY_TAKE_BACK_FRONT_CAMERA_AGAIN");
            this.mMainHandler.removeMessages(16);
        }
        Message msg = this.mMainHandler.obtainMessage(8);
        msg.obj = status;
        this.mMainHandler.sendMessage(msg);
        VLog.d(TAG, "isFrontCameraOpened8=" + this.isFrontCameraOpened);
    }

    private void handleFrontCameraClosed(CameraStatus status) {
        VLog.d(TAG, "handleFrontCameraClosed");
        if (this.isCheckingPopupCameraPermission && this.permissionCheckDialog != null) {
            VLog.d(TAG, "get front camera closed event, but the last open event for check popup permission is doing,we will cancel the check task");
            this.permissionCheckDialog.cancelPermissionCheck();
        }
        Message msg = this.mMainHandler.obtainMessage(7);
        msg.obj = status;
        this.mMainHandler.sendMessage(msg);
    }

    private void handleBackCameraOpened(CameraStatus status) {
        VLog.d(TAG, "handleBackCameraOpened");
        Message msg = this.mMainHandler.obtainMessage(9);
        msg.obj = status;
        this.mMainHandler.sendMessage(msg);
    }

    private void handleFaceUnlockPush() {
        VLog.d(TAG, "handleFaceUnlockPush");
        Message msg = this.mMainHandler.obtainMessage(22);
        this.mMainHandler.sendMessage(msg);
    }

    private void handleBackCameraClosed(CameraStatus status) {
        VLog.d(TAG, "handleBackCameraClosed");
    }

    private String cameraStatsFromIntToString(int status) {
        if (status != 0) {
            if (status != 1) {
                if (status != 2) {
                    if (status != 4) {
                        if (status == 6) {
                            return "pushed-for-faceunlock";
                        }
                        return "invalid";
                    }
                    return "back-camera-closed";
                }
                return "back-camera-opened";
            }
            return "front-camera-closed";
        }
        return "front-camera-opened";
    }

    private boolean isFrontCameraFromStatus(int status) {
        return status == 1 || status == 0;
    }

    private boolean isBackCameraFromStatus(int status) {
        return status == 4 || status == 2;
    }

    private boolean isCameraOpendFromStatus(int status) {
        return status == 0 || status == 2;
    }

    private boolean isCameraClosedFromStatus(int status) {
        return status == 1 || status == 4;
    }

    private boolean isFaceUnlockPushStatusNotify(int status) {
        return status == 6;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isFaceUnlock(CameraStatus cs) {
        return VivoPermissionUtils.OS_PKG.equalsIgnoreCase(cs.currentStatusPackageName) || FaceUIState.PKG_FACEUI.equalsIgnoreCase(cs.currentStatusPackageName);
    }

    public boolean notifyCameraStatus(int cameraId, int status, String packageName) {
        VLog.d(TAG, "notifyCameraStatus cameraId=" + cameraId + " packageName=" + packageName + " status=" + cameraStatsFromIntToString(status));
        StringBuilder sb = new StringBuilder();
        sb.append("isFrontCameraOpened1=");
        sb.append(this.isFrontCameraOpened);
        VLog.d(TAG, sb.toString());
        if (this.isSystemShutdowning) {
            VLog.d(TAG, "because system is shutdown, we ignore any notifyCameraStatus!!!");
            return true;
        }
        connectCameraServiceLocked();
        synchronized (this.mFrontCameraStatusLock) {
            if (isFaceUnlockPushStatusNotify(status)) {
                if (this.isFaceUnlocking) {
                    this.isFaceUnlocking = false;
                    handleFaceUnlockPush();
                } else {
                    VLog.d(TAG, "faceunlock notify push camera, but isFaceUnlocking is false, we just ignore it");
                }
                return true;
            }
            CameraStatus tmpFrontStatus = new CameraStatus();
            CameraStatus tmpBackStatus = new CameraStatus();
            if (isFrontCameraFromStatus(status)) {
                CameraStatus.updateCameraStatus(cameraId, isCameraOpendFromStatus(status), packageName, this.mFrontCameraStatus);
                CameraStatus.updateCameraStatus(cameraId, isCameraOpendFromStatus(status), packageName, tmpFrontStatus);
            }
            if (isBackCameraFromStatus(status)) {
                CameraStatus.updateCameraStatus(cameraId, isCameraOpendFromStatus(status), packageName, this.mBackCameraStatus);
                CameraStatus.updateCameraStatus(cameraId, isCameraOpendFromStatus(status), packageName, tmpBackStatus);
            }
            boolean lastFaceUnlcking = this.isFaceUnlocking;
            VLog.d(TAG, "isFrontCameraOpened2=" + this.isFrontCameraOpened);
            if (isFrontCameraFromStatus(status)) {
                this.isFaceUnlocking = isFaceUnlock(tmpFrontStatus);
                if (lastFaceUnlcking && !this.isFaceUnlocking) {
                    VLog.d(TAG, "last is faceunlocking , now other apps use front camera , we clear faceunlocking flag");
                }
                if (status == 1) {
                    VLog.d(TAG, "isFrontCameraOpened3=" + this.isFrontCameraOpened);
                    this.isFrontCameraOpened = false;
                    VLog.d(TAG, "isFrontCameraOpened4=" + this.isFrontCameraOpened);
                    handleFrontCameraClosed(tmpFrontStatus);
                } else if (status == 0) {
                    VLog.d(TAG, "isFrontCameraOpened5=" + this.isFrontCameraOpened);
                    this.isFrontCameraOpened = true;
                    VLog.d(TAG, "isFrontCameraOpened6=" + this.isFrontCameraOpened);
                    if (this.isFaceUnlocking) {
                        if (!lastFaceUnlcking) {
                            VLog.d(TAG, "front camera opened for faceunlock");
                        } else {
                            VLog.d(TAG, "front camera opened again for faceunlock");
                        }
                    }
                    handleFrontCameraOpened(tmpFrontStatus);
                }
            } else if (isBackCameraFromStatus(status)) {
                if (status == 4) {
                    handleBackCameraClosed(tmpBackStatus);
                } else if (status == 2) {
                    handleBackCameraOpened(tmpBackStatus);
                }
            }
            return true;
        }
    }

    private boolean isInWarnPopupCameraPackageList(CameraStatus status) {
        if (status == null) {
            return false;
        }
        return WARN_POPUP_CAMERA_PACKAGE_LIST.contains(status.currentStatusPackageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSystemCameraApp(CameraStatus status) {
        if (status == null) {
            return false;
        }
        return SYSTEM_CAMERA_APP_PACKAGE_LIST.contains(status.currentStatusPackageName);
    }

    private boolean isInShortTakebackPackageList(CameraStatus status) {
        if (status == null) {
            return false;
        }
        return SHORT_TAKEBACK_PACKAGE_LIST.contains(status.currentStatusPackageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getAppName(String packageName) {
        try {
            PackageInfo pi = this.mContext.getPackageManager().getPackageInfo(packageName, 64);
            return (pi == null || pi.applicationInfo == null) ? packageName : pi.applicationInfo.loadLabel(this.mContext.getPackageManager()).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return packageName;
        }
    }

    private boolean isSystemApp(String packageName) {
        if (packageName == null) {
            return false;
        }
        try {
            PackageInfo pi = this.mContext.getPackageManager().getPackageInfo(packageName, 64);
            if (pi == null || pi.applicationInfo == null) {
                return false;
            }
            if ((pi.applicationInfo.flags & 1) == 0 && (pi.applicationInfo.flags & 128) == 0) {
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void sendFrontCameraPressedBroadcast() {
        Intent intent = new Intent(FRONT_CAMERA_PRESSED_ACTION);
        Bundle extras = new Bundle();
        extras.putLong(FRONT_CAMERA_PRESSED_EXTRA_TIME, System.currentTimeMillis());
        intent.putExtras(extras);
        intent.setPackage("com.android.camera");
        VLog.d(TAG, " broadcast vivo.intent.action.FRONT_CAMERA_PRESSED");
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean isStatusJammed(int status) {
        return status == 3 || status == 4;
    }

    private boolean isStatusPressed(int status) {
        return status == 5;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelPendingCloseOrOpenVibStebTask(int status, int msgCookie) {
        String str = "by pressed";
        if (msgCookie == this.mLastCloseVibHallCookie) {
            StringBuilder sb = new StringBuilder();
            sb.append("closeVibStep with cookie ");
            sb.append(msgCookie);
            sb.append(" get response ");
            sb.append(isStatusJammed(status) ? "by jammed status" : isStatusPressed(status) ? "by pressed" : "by push-ok");
            VLog.d(TAG, sb.toString());
            this.mHavePendingCloseVibStepTask = false;
            if (isStatusJammed(status)) {
                this.isLastTakebackJammed = true;
            }
        }
        if (msgCookie == this.mLastOpenVibHallCookie) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("openVibStep with cookie ");
            sb2.append(msgCookie);
            sb2.append(" get response ");
            if (isStatusJammed(status)) {
                str = "by jammed status";
            } else if (!isStatusPressed(status)) {
                str = "by popup-ok";
            }
            sb2.append(str);
            VLog.d(TAG, sb2.toString());
            this.mHavaPendingOpenVibStepTask = false;
            if (isStatusJammed(status)) {
                this.isLastPupupJammed = true;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isInKeyguardRestrictedInputMode() {
        KeyguardManager km = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (km != null) {
            return km.inKeyguardRestrictedInputMode();
        }
        return false;
    }

    private void sendEvent(int keyCode, int action) {
        KeyEvent ev = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), action, keyCode, 0, 0, -1, 0, 72, 257);
        InputManager.getInstance().injectInputEvent(ev, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void emulatePressHomeKey() {
        VLog.d(TAG, "emulatePressHomeKey");
        sendEvent(3, 0);
        sendEvent(3, 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isScreenOn() {
        PowerManager powerManager = this.mPowerManager;
        if (powerManager != null) {
            return powerManager.isScreenOn();
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCurrentFrontCameraOpened() {
        return this.isFrontCameraOpened;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void acquireWakeLock(long timeout) {
        PowerManager.WakeLock wakeLock = this.mHallWakeLock;
        if (wakeLock != null && !wakeLock.isHeld()) {
            VLog.d(TAG, "wakelock is not held, acquire it again");
            this.mHallWakeLock.acquire(timeout);
            return;
        }
        VLog.d(TAG, "the wakelock is held, don't need acquire it again");
    }

    private void releaseWakeLock() {
        PowerManager.WakeLock wakeLock = this.mHallWakeLock;
        if (wakeLock != null && wakeLock.isHeld()) {
            VLog.d(TAG, "wakelock is held, release it");
            this.mHallWakeLock.release();
            return;
        }
        VLog.d(TAG, "wakelock is not held, no need to release it");
    }

    /* loaded from: classes.dex */
    private final class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            boolean z = true;
            switch (msg.what) {
                case 0:
                    int msgCookie = msg.arg2;
                    VLog.d(PopupCameraManagerService.TAG, "MSG_POLL_STATUS_CANCELED cookie=" + msgCookie);
                    if (msgCookie >= 100000) {
                        PopupCameraLightManager.stopCameraLightForPush();
                        return;
                    } else {
                        PopupCameraLightManager.stopCameraLightForPopup();
                        return;
                    }
                case 1:
                    VLog.d(PopupCameraManagerService.TAG, "front camera pushed ok");
                    if (PopupCameraManagerService.this.mAudioManager != null) {
                        PopupCameraManagerService.this.mAudioManager.setParameters("FrontCameraLower=false");
                    }
                    PopupCameraLightManager.stopCameraLightForPush();
                    PopupCameraManagerService.this.cancelPendingCloseOrOpenVibStebTask(msg.what, msg.arg2);
                    PopupCameraManagerService.this.unregistDropDownSensorEventListener();
                    if (PopupCameraManagerService.this.mDataAnalysisHelper != null) {
                        PopupCameraManagerService.this.mDataAnalysisHelper.gatherCounts(msg.what);
                    }
                    if (PopupCameraManagerService.this.mPushJammedDialog != null && PopupCameraManagerService.this.mUIHandler != null) {
                        PopupCameraManagerService.this.mUIHandler.post(new Runnable() { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.MainHandler.1
                            @Override // java.lang.Runnable
                            public void run() {
                                if (PopupCameraManagerService.this.mPushJammedDialog.isShowing()) {
                                    PopupCameraManagerService.this.mPushJammedDialog.dismiss();
                                }
                            }
                        });
                    }
                    PopupCameraManagerService.this.mNativeNotificationImpl.notifyCameraState(2);
                    return;
                case 2:
                    VLog.d(PopupCameraManagerService.TAG, "front camera popup ok");
                    if (PopupCameraManagerService.this.mAudioManager != null) {
                        PopupCameraManagerService.this.mAudioManager.setParameters("FrontCameraRising=false");
                    }
                    sendEmptyMessageDelayed(23, 3000L);
                    sendEmptyMessage(26);
                    PopupCameraManagerService.this.cancelPendingCloseOrOpenVibStebTask(msg.what, msg.arg2);
                    PopupCameraManagerService.this.registDropDownSensorEventListener();
                    if (PopupCameraManagerService.this.mDataAnalysisHelper != null) {
                        PopupCameraManagerService.this.mDataAnalysisHelper.gatherCounts(msg.what);
                    }
                    if (PopupCameraManagerService.this.mPopupJammedDialog != null && PopupCameraManagerService.this.mUIHandler != null) {
                        PopupCameraManagerService.this.mUIHandler.post(new Runnable() { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.MainHandler.2
                            @Override // java.lang.Runnable
                            public void run() {
                                if (PopupCameraManagerService.this.mPopupJammedDialog.isShowing()) {
                                    PopupCameraManagerService.this.mPopupJammedDialog.dismiss();
                                }
                            }
                        });
                    }
                    PopupCameraManagerService.this.mNativeNotificationImpl.notifyCameraState(2);
                    return;
                case 3:
                    VLog.d(PopupCameraManagerService.TAG, "front camera push jammed & add MSG_DELAY_TAKE_BACK_FRONT_CAMERA_AGAIN for try again");
                    PopupCameraManagerService.this.unregistDropDownSensorEventListener();
                    PopupCameraLightManager.stopCameraLightForPush();
                    PopupCameraManagerService.this.cancelPendingCloseOrOpenVibStebTask(msg.what, msg.arg2);
                    if (hasMessages(16)) {
                        removeMessages(16);
                    }
                    if (PopupCameraManagerService.this.mCurrentRetryTimes < 5) {
                        sendEmptyMessageDelayed(16, 20000L);
                    }
                    PopupCameraManagerService.this.mUIHandler.sendMessage(PopupCameraManagerService.this.mUIHandler.obtainMessage(1));
                    if (PopupCameraManagerService.this.mDataAnalysisHelper != null) {
                        PopupCameraManagerService.this.mDataAnalysisHelper.gatherCounts(msg.what);
                    }
                    EPMReporter.getInstance().reportPushJammed(PopupCameraManagerService.this.mFrontCameraStatus);
                    return;
                case 4:
                    VLog.d(PopupCameraManagerService.TAG, "front camera popup jammed");
                    PopupCameraLightManager.stopCameraLightForPopup();
                    PopupCameraManagerService.this.cancelPendingCloseOrOpenVibStebTask(msg.what, msg.arg2);
                    PopupCameraManagerService.this.mUIHandler.sendMessage(PopupCameraManagerService.this.mUIHandler.obtainMessage(0));
                    VLog.d(PopupCameraManagerService.TAG, "because popup front camera jammed, we take back front camera immediately");
                    PopupCameraManagerService.this.takeupFrontCameraInternal(true, false);
                    if (PopupCameraManagerService.this.mDataAnalysisHelper != null) {
                        PopupCameraManagerService.this.mDataAnalysisHelper.gatherCounts(msg.what);
                    }
                    EPMReporter.getInstance().reportPopupJammed(PopupCameraManagerService.this.mFrontCameraStatus);
                    return;
                case 5:
                    VLog.d(PopupCameraManagerService.TAG, "front camera pressed, take back front camera immediately");
                    PopupCameraManagerService.this.cancelPendingCloseOrOpenVibStebTask(msg.what, msg.arg2);
                    PopupCameraManagerService.this.takeupFrontCameraInternal(true, false);
                    PopupCameraManagerService.this.emulatePressHomeKey();
                    if (PopupCameraManagerService.this.isFrontCameraOpened) {
                        sendEmptyMessageDelayed(15, 3000L);
                    }
                    if (PopupCameraManagerService.this.mDataAnalysisHelper != null) {
                        PopupCameraManagerService.this.mDataAnalysisHelper.gatherCounts(msg.what);
                        return;
                    }
                    return;
                case 6:
                case 10:
                case 17:
                case 19:
                case 20:
                case 25:
                default:
                    return;
                case 7:
                    VLog.d(PopupCameraManagerService.TAG, "MSG_FRONT_CAMERA_CLOSED");
                    PopupCameraManagerService.this.acquireWakeLock(6000L);
                    PopupCameraManagerService.this.resetPendingPopupTask();
                    if (hasMessages(15)) {
                        removeMessages(15);
                        VLog.d(PopupCameraManagerService.TAG, "front camera is close, we remove MSG_CHECK_FRONT_CAMERA_IS_CLOSED");
                    }
                    if (hasMessages(24)) {
                        removeMessages(24);
                        VLog.d(PopupCameraManagerService.TAG, "front camera is close, we remove MSG_CHECK_SCREEN_ON");
                    }
                    if (PopupCameraManagerService.this.mPressedDialog != null && PopupCameraManagerService.this.mUIHandler != null) {
                        PopupCameraManagerService.this.mUIHandler.post(new Runnable() { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.MainHandler.3
                            @Override // java.lang.Runnable
                            public void run() {
                                if (PopupCameraManagerService.this.mPressedDialog.isShowing()) {
                                    PopupCameraManagerService.this.mPressedDialog.dismiss();
                                }
                            }
                        });
                    }
                    if (PopupCameraManagerService.this.mDropProtectDialog != null && PopupCameraManagerService.this.mUIHandler != null) {
                        PopupCameraManagerService.this.mUIHandler.post(new Runnable() { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.MainHandler.4
                            @Override // java.lang.Runnable
                            public void run() {
                                if (PopupCameraManagerService.this.mDropProtectDialog.isShowing()) {
                                    PopupCameraManagerService.this.mDropProtectDialog.dismiss();
                                }
                            }
                        });
                    }
                    CameraStatus tmp = (CameraStatus) msg.obj;
                    boolean isFaceUnlock = PopupCameraManagerService.this.isFaceUnlock(tmp);
                    boolean isAppForground = ApplicationProcessStateHelper.isApplicationProcessForeground(tmp.currentStatusPackageName).isAppForeground;
                    if (!PopupCameraManagerService.this.isFrontCameraPopup || isFaceUnlock) {
                        if (PopupCameraManagerService.this.isFrontCameraPopup && isFaceUnlock) {
                            PopupCameraManagerService.this.fakeTakeupFrontCameraInternal();
                            VLog.d(PopupCameraManagerService.TAG, "we don't takeback front camera when faceunlock isFrontCameraPopup=" + PopupCameraManagerService.this.isFrontCameraPopup);
                            return;
                        }
                        return;
                    }
                    PopupCameraManagerService.this.mTakeBackTaskStartTime = System.currentTimeMillis();
                    PopupCameraManagerService.this.mPendingFrontCameraCloseStatus = tmp;
                    PopupCameraManagerService.this.isHavingPendingTakeBackTask = true;
                    boolean isInKeyguardMode = PopupCameraManagerService.this.isInKeyguardRestrictedInputMode();
                    boolean isShortTakeback = PopupCameraManagerService.this.isShouldTakebackImmediately(tmp);
                    VLog.d(PopupCameraManagerService.TAG, "the app isInShortTakebackList=" + isShortTakeback);
                    VLog.d(PopupCameraManagerService.TAG, "isAppForground=" + isAppForground);
                    VLog.d(PopupCameraManagerService.TAG, "isInKeyguardMode=" + isInKeyguardMode);
                    VLog.d(PopupCameraManagerService.TAG, "isScreenOn=" + PopupCameraManagerService.this.isScreenOn());
                    if ((!PopupCameraManagerService.this.isSystemCameraApp(tmp) && !isAppForground) || isInKeyguardMode || isShortTakeback || isFaceUnlock || !PopupCameraManagerService.this.isScreenOn()) {
                        VLog.d(PopupCameraManagerService.TAG, "the third parth app is in background,take back front camera isInKeyguardMode=" + isInKeyguardMode);
                        if (isShortTakeback) {
                            VLog.d(PopupCameraManagerService.TAG, "in short takeback list, delay " + PopupCameraManagerService.this.mImmediatelyTakebackTime + "ms");
                            Message shortTakeMsg = obtainMessage(21);
                            shortTakeMsg.obj = tmp;
                            sendMessageDelayed(shortTakeMsg, (long) PopupCameraManagerService.this.mImmediatelyTakebackTime);
                            return;
                        }
                        PopupCameraManagerService.this.takeupFrontCameraInternal(false, isFaceUnlock);
                        return;
                    }
                    Message tmpMsg = obtainMessage(11);
                    PopupCameraManagerService.this.isTodoFirstClosedPoll = true;
                    PopupCameraManagerService.this.isLastInGalleryActivity = false;
                    tmpMsg.obj = tmp;
                    sendMessage(tmpMsg);
                    return;
                case 8:
                    VLog.d(PopupCameraManagerService.TAG, "isFrontCameraOpened9=" + PopupCameraManagerService.this.isFrontCameraOpened);
                    if (hasMessages(11)) {
                        removeMessages(11);
                        VLog.d(PopupCameraManagerService.TAG, "MSG_FRONT_CAMERA_OPENED, we remove MSG_FRONT_CAMERA_CLOSED_POLL");
                    }
                    if (hasMessages(21)) {
                        removeMessages(21);
                        VLog.d(PopupCameraManagerService.TAG, "MSG_FRONT_CAMERA_OPENED, we remove MSG_SHORT_TAKEBACK");
                    }
                    if (PopupCameraManagerService.this.isFrontCameraPopup) {
                        VLog.d(PopupCameraManagerService.TAG, "front camera is popuped, we just return ,need do nothing for MSG_FRONT_CAMERA_OPENED");
                        return;
                    }
                    CameraStatus tmp3 = (CameraStatus) msg.obj;
                    ApplicationProcessStateHelper.ApplicationProcessStatus aps = ApplicationProcessStateHelper.isApplicationProcessForeground(tmp3.currentStatusPackageName);
                    boolean isAppForground3 = aps.isAppForeground;
                    VLog.d(PopupCameraManagerService.TAG, "MSG_FRONT_CAMERA_OPENED isAppForground=" + isAppForground3 + " isScreenOn=" + PopupCameraManagerService.this.isScreenOn());
                    PopupFrontCameraPermissionHelper.PopupFrontCameraPermissionState ps = PopupFrontCameraPermissionHelper.getFrontCameraPermissionStateFromSettings(PopupCameraManagerService.this.mContext, tmp3.currentStatusPackageName);
                    StringBuilder sb = new StringBuilder();
                    sb.append("getFrontCameraPermissionStateFromSettings");
                    sb.append(ps);
                    VLog.d(PopupCameraManagerService.TAG, sb.toString());
                    if (!PopupCameraManagerService.this.isShouldForbiddenPopupFrontCamera(tmp3.currentStatusPackageName, ps)) {
                        boolean isFaceUnlock2 = PopupCameraManagerService.this.isFaceUnlock(tmp3);
                        boolean isBackgroundUseExempted = PopupCameraManagerService.this.isInBackgroundUseExemptionList(tmp3);
                        if (!isAppForground3 && !isBackgroundUseExempted) {
                            int i = 1;
                            boolean isAppForground32 = isAppForground3;
                            ApplicationProcessStateHelper.ApplicationProcessStatus aps2 = aps;
                            while (true) {
                                if (i <= 5) {
                                    try {
                                        Thread.sleep(200L);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    aps2 = ApplicationProcessStateHelper.isApplicationProcessForeground(tmp3.currentStatusPackageName);
                                    isAppForground32 = aps2.isAppForeground;
                                    VLog.d(PopupCameraManagerService.TAG, "check again after " + (i * 200) + "ms isAppForground3=" + isAppForground32);
                                    if (isAppForground32) {
                                        isAppForground3 = isAppForground32;
                                    } else {
                                        i++;
                                    }
                                } else {
                                    isAppForground3 = isAppForground32;
                                }
                            }
                        }
                        VLog.d(PopupCameraManagerService.TAG, "after finally check , isAppForground3=" + isAppForground3 + " isFrontCameraOpened=" + PopupCameraManagerService.this.isFrontCameraOpened);
                        if (!PopupCameraManagerService.this.isFrontCameraOpened) {
                            VLog.d(PopupCameraManagerService.TAG, "after finally check , the front camera is closed, we just return...");
                            return;
                        } else if (isAppForground3) {
                            boolean isShouldPopupPermissionDialog = PopupCameraManagerService.this.isShouldWarnUserPopupCameraPermissionDialog(tmp3, ps);
                            if (PopupCameraManagerService.this.isScreenOn() || isFaceUnlock2) {
                                if (!isShouldPopupPermissionDialog) {
                                    VLog.d(PopupCameraManagerService.TAG, "screen is on, just popup front camera now");
                                    PopupCameraManagerService.this.resetPendingPopupTask();
                                    PopupCameraManagerService.this.popupFrontCameraInternal(isFaceUnlock2, tmp3);
                                    PopupCameraManagerService.this.resetCloseFrontCameraStatus();
                                    return;
                                }
                                VLog.d(PopupCameraManagerService.TAG, tmp3.currentStatusPackageName + " is in warn list for popup camera");
                                PopupCameraManagerService.this.popupPermissionConfirmDialog(tmp3, ps, true);
                                return;
                            }
                            VLog.d(PopupCameraManagerService.TAG, "screen is off, don't popup front camera now, we will popup when screen on");
                            PopupCameraManagerService.this.setPendingPopupTask(isShouldPopupPermissionDialog, true, tmp3, ps);
                            PopupCameraManagerService.this.startCheckScreenOn();
                            return;
                        } else if (isBackgroundUseExempted) {
                            if (PopupCameraManagerService.this.isScreenOn() || isFaceUnlock2) {
                                VLog.d(PopupCameraManagerService.TAG, "screen is on, just popup front camera now");
                                PopupCameraManagerService.this.resetPendingPopupTask();
                                PopupCameraManagerService.this.popupFrontCameraInternal(isFaceUnlock2, tmp3);
                                PopupCameraManagerService.this.resetCloseFrontCameraStatus();
                                return;
                            }
                            VLog.d(PopupCameraManagerService.TAG, "screen is off, don't popup front camera now, we will popup when screen on");
                            PopupCameraManagerService.this.setPendingPopupTask(false, false, tmp3, ps);
                            PopupCameraManagerService.this.startCheckScreenOn();
                            return;
                        } else {
                            PopupCameraManagerService.this.popupPermissionConfirmDialog(tmp3, ps, false);
                            return;
                        }
                    }
                    VLog.d(PopupCameraManagerService.TAG, "the app " + tmp3.currentStatusPackageName + "is not allowed to popup front camera, ignore the popup");
                    EPMReporter.getInstance().reportPermissionDeny(PopupCameraManagerService.this.mFrontCameraStatus);
                    return;
                case 9:
                    if (PopupCameraManagerService.this.mPendingFrontCameraCloseStatus != null) {
                        if (hasMessages(7)) {
                            removeMessages(7);
                        }
                        if (hasMessages(11)) {
                            removeMessages(11);
                        }
                        if (hasMessages(21)) {
                            removeMessages(21);
                        }
                        VLog.d(PopupCameraManagerService.TAG, "becuase back camera opened , we take back front camera immediately");
                        PopupCameraManagerService.this.takeupFrontCameraInternal(false, false);
                    }
                    if (PopupCameraManagerService.this.isLastTakebackJammed) {
                        VLog.d(PopupCameraManagerService.TAG, "the back camera is opened and the last takeback front camera is jammed, we try take back again");
                        PopupCameraManagerService.this.takeupFrontCameraInternal(true, true);
                        return;
                    }
                    return;
                case 11:
                    VLog.d(PopupCameraManagerService.TAG, "MSG_FRONT_CAMERA_CLOSED_POLL again");
                    if (PopupCameraManagerService.this.isFrontCameraPopup) {
                        CameraStatus tmp2 = (CameraStatus) msg.obj;
                        ApplicationProcessStateHelper.ApplicationProcessStatus aps3 = ApplicationProcessStateHelper.isApplicationProcessForeground(tmp2.currentStatusPackageName);
                        boolean isAppForground2 = aps3.isAppForeground;
                        boolean isInSystemGallery = aps3.isInGalleryActivity;
                        if ((!isInSystemGallery || !PopupCameraManagerService.this.isLastInGalleryActivity) && (!isInSystemGallery || PopupCameraManagerService.this.isLastInGalleryActivity || !PopupCameraManagerService.this.isTodoFirstClosedPoll)) {
                            z = false;
                        }
                        boolean isStillInGallery = z;
                        PopupCameraManagerService.this.isLastInGalleryActivity = isInSystemGallery;
                        PopupCameraManagerService.this.isTodoFirstClosedPoll = false;
                        boolean isInKeyguardMode2 = PopupCameraManagerService.this.isInKeyguardRestrictedInputMode();
                        VLog.d(PopupCameraManagerService.TAG, "isAppForground2=" + isAppForground2);
                        VLog.d(PopupCameraManagerService.TAG, "isInKeyguardMode2=" + isInKeyguardMode2);
                        VLog.d(PopupCameraManagerService.TAG, "isStillInGallery=" + isStillInGallery);
                        VLog.d(PopupCameraManagerService.TAG, "isScreenOn=" + PopupCameraManagerService.this.isScreenOn());
                        if ((!isAppForground2 && (!PopupCameraManagerService.this.isSystemCameraApp(tmp2) || !isStillInGallery)) || isInKeyguardMode2 || !PopupCameraManagerService.this.isScreenOn()) {
                            VLog.d(PopupCameraManagerService.TAG, "the app is in background,take back front camera immediately isInKeyguardMode=" + isInKeyguardMode2);
                            PopupCameraManagerService.this.takeupFrontCameraInternal(false, false);
                            return;
                        }
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - PopupCameraManagerService.this.mTakeBackTaskStartTime > 5000) {
                            VLog.d(PopupCameraManagerService.TAG, "we have poll more than  5000ms , we take back front camera immediately");
                            PopupCameraManagerService.this.takeupFrontCameraInternal(false, false);
                            return;
                        }
                        Message pollAgainMsg = obtainMessage(11);
                        pollAgainMsg.obj = tmp2;
                        sendMessageDelayed(pollAgainMsg, 500L);
                        return;
                    }
                    return;
                case 12:
                    PopupCameraManagerService.this.takeupFrontCameraInternal(false, false);
                    return;
                case 13:
                    PopupCameraManagerService.this.popupFrontCameraInternal(false, null);
                    return;
                case 14:
                    if (PopupCameraManagerService.this.mPendingFrontCameraCloseStatus == null) {
                        if (PopupCameraManagerService.this.isFrontCameraPopup) {
                            VLog.d(PopupCameraManagerService.TAG, "take back front camera even through the front camera is opened");
                            PopupCameraManagerService.this.takeupFrontCameraInternal(false, false);
                            return;
                        }
                        return;
                    }
                    if (hasMessages(7)) {
                        removeMessages(7);
                    }
                    if (hasMessages(11)) {
                        removeMessages(11);
                    }
                    VLog.d(PopupCameraManagerService.TAG, "have pending close task,becuase cameraserver died , we take back front camera immediately");
                    PopupCameraManagerService.this.takeupFrontCameraInternal(false, false);
                    return;
                case 15:
                    if (PopupCameraManagerService.this.isFrontCameraOpened) {
                        VLog.d(PopupCameraManagerService.TAG, "after emulate home key , the front camera is opened, notify user");
                        if (PopupCameraManagerService.this.mUIHandler != null) {
                            PopupCameraManagerService.this.mUIHandler.sendMessage(PopupCameraManagerService.this.mUIHandler.obtainMessage(3));
                            return;
                        }
                        return;
                    }
                    VLog.d(PopupCameraManagerService.TAG, "after emulate home key , the front camera is closed, do nothing");
                    return;
                case 16:
                    VLog.d(PopupCameraManagerService.TAG, "after 20s, try takeback front camera again time " + (PopupCameraManagerService.this.mCurrentRetryTimes + 1));
                    if (!PopupCameraManagerService.this.isFrontCameraOpened && PopupCameraManagerService.this.isLastTakebackJammed) {
                        PopupCameraManagerService.access$2208(PopupCameraManagerService.this);
                        PopupCameraManagerService.this.takeupFrontCameraInternal(true, true);
                        return;
                    }
                    return;
                case 18:
                    VLog.d(PopupCameraManagerService.TAG, "MSG_TAKE_BACK_FRONT_CAMERA_FOR_DROP");
                    PopupCameraManagerService.this.takeupFrontCameraInternalAfterFalling();
                    if (!PopupCameraManagerService.this.isFrontCameraPopup && PopupCameraManagerService.this.mDataAnalysisHelper != null) {
                        PopupCameraManagerService.this.mDataAnalysisHelper.gatherCounts(msg.what);
                        return;
                    }
                    return;
                case 21:
                    VLog.d(PopupCameraManagerService.TAG, "MSG_SHORT_TAKEBACK");
                    if (PopupCameraManagerService.this.isFrontCameraPopup) {
                        VLog.d(PopupCameraManagerService.TAG, "after delay " + PopupCameraManagerService.this.mImmediatelyTakebackTime + "ms, takeback front camera");
                        PopupCameraManagerService.this.takeupFrontCameraInternal(false, false);
                        return;
                    }
                    return;
                case 22:
                    if (PopupCameraManagerService.this.isFrontCameraPopup) {
                        PopupCameraManagerService.this.takeupFrontCameraInternal(false, true);
                        return;
                    }
                    return;
                case 23:
                    PopupCameraLightManager.stopCameraLightForPopup();
                    return;
                case 24:
                    VLog.d(PopupCameraManagerService.TAG, "MSG_CHECK_SCREEN_ON");
                    if (PopupCameraManagerService.this.isScreenOn() || !PopupCameraManagerService.this.isFrontCameraOpened || !PopupCameraManagerService.this.mHavePendingPopupTask || PopupCameraManagerService.this.mPendingPopupTask == null) {
                        if (PopupCameraManagerService.this.isScreenOn() && PopupCameraManagerService.this.isFrontCameraOpened && !PopupCameraManagerService.this.isFrontCameraPopup && PopupCameraManagerService.this.mHavePendingPopupTask && PopupCameraManagerService.this.mPendingPopupTask != null) {
                            if (PopupCameraManagerService.this.mPendingPopupTask.mPendingPopupTaskNeedPopupPermissionDialog) {
                                VLog.d(PopupCameraManagerService.TAG, "have pending popup task,need check permission");
                                PopupCameraManagerService popupCameraManagerService = PopupCameraManagerService.this;
                                popupCameraManagerService.popupPermissionConfirmDialog(popupCameraManagerService.mPendingPopupTask.mPendingPopupCameraStatus, PopupCameraManagerService.this.mPendingPopupTask.mPermissionState, PopupCameraManagerService.this.mPendingPopupTask.isShowCheckbox);
                                return;
                            }
                            VLog.d(PopupCameraManagerService.TAG, "have pending popup task, no need check permission ,just popup now");
                            try {
                                Thread.sleep(1000L);
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                            PopupCameraManagerService popupCameraManagerService2 = PopupCameraManagerService.this;
                            popupCameraManagerService2.popupFrontCameraInternal(false, popupCameraManagerService2.mPendingPopupTask.mPendingPopupCameraStatus);
                            PopupCameraManagerService.this.resetPendingPopupTask();
                            PopupCameraManagerService.this.resetCloseFrontCameraStatus();
                            return;
                        }
                        return;
                    }
                    VLog.d(PopupCameraManagerService.TAG, "screen is off , we will post MSG_CHECK_SCREEN_ON again");
                    sendEmptyMessageDelayed(24, 400L);
                    return;
                case 26:
                    PopupCameraVibrateManager.stopCameraVibrate();
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyCalibrationResult(boolean done) {
        if (done) {
            VLog.d(TAG, "notifyCalibrationResult, remove MSG_DELAY_TAKE_BACK_FRONT_CAMERA_AGAIN");
            if (this.mMainHandler.hasMessages(16)) {
                this.mMainHandler.removeMessages(16);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void popupPermissionConfirmDialog(final CameraStatus tmp3, final PopupFrontCameraPermissionHelper.PopupFrontCameraPermissionState ps, final boolean isShowCheckbox) {
        boolean isAllowToPopup;
        this.isCheckingPopupCameraPermission = true;
        this.permissionCheckDialog = null;
        Handler handler = this.mUIHandler;
        if (handler != null) {
            handler.post(new Runnable() { // from class: com.vivo.services.popupcamera.PopupCameraManagerService.9
                @Override // java.lang.Runnable
                public void run() {
                    Context context;
                    int i;
                    PopupCameraManagerService popupCameraManagerService = PopupCameraManagerService.this;
                    ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(PopupCameraManagerService.this.mContext, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT));
                    String str = "\"" + PopupCameraManagerService.this.getAppName(tmp3.currentStatusPackageName) + "\" " + PopupCameraManagerService.this.mContext.getString(51249514);
                    if (isShowCheckbox) {
                        context = PopupCameraManagerService.this.mContext;
                        i = 51249782;
                    } else {
                        context = PopupCameraManagerService.this.mContext;
                        i = 51249415;
                    }
                    popupCameraManagerService.permissionCheckDialog = new CameraPopupPermissionCheckDialog(contextThemeWrapper, true, str, context.getString(i), PopupCameraManagerService.this.mContext.getString(51249411), isShowCheckbox, ps);
                    PopupCameraManagerService.this.permissionCheckDialog.show();
                    synchronized (PopupCameraManagerService.this.mPermissionCheckDialogLock) {
                        PopupCameraManagerService.this.mPermissionCheckDialogLock.notifyAll();
                    }
                }
            });
        }
        synchronized (this.mPermissionCheckDialogLock) {
            while (this.permissionCheckDialog == null) {
                VLog.d(TAG, "wait for create CameraPopupPermissionCheckDialog");
                try {
                    this.mPermissionCheckDialogLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        synchronized (this.permissionCheckDialog) {
            if (this.permissionCheckDialog.isPermissionConfirmed()) {
                isAllowToPopup = this.permissionCheckDialog.isPermissionGranted();
            } else {
                VLog.d(TAG, "wait 20s for user confim popup permission");
                try {
                    this.permissionCheckDialog.wait(25000L);
                    this.isCheckingPopupCameraPermission = false;
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                    this.isCheckingPopupCameraPermission = false;
                }
                if (!this.permissionCheckDialog.isPermissionConfirmed() && !this.permissionCheckDialog.isPermissionCheckCanceled()) {
                    VLog.d(TAG, "after 25 seconds, the user doesn't confirm, we just deny it");
                    isAllowToPopup = false;
                } else if (this.permissionCheckDialog.isPermissionCheckCanceled()) {
                    VLog.d(TAG, "popup front camera is canceled after check, we don't popup");
                    isAllowToPopup = false;
                } else {
                    isAllowToPopup = this.permissionCheckDialog.isPermissionGranted();
                    VLog.d(TAG, "user confirm popucamera isAllowToPopup=" + isAllowToPopup);
                }
            }
            if (!isAllowToPopup) {
                VLog.d(TAG, "popup front camera is denied by user!!!, not popup front camera");
            } else if (isScreenOn() && this.isFrontCameraOpened) {
                VLog.d(TAG, "screen is on & front camera is opened, just popup front camera after user choose");
                resetPendingPopupTask();
                popupFrontCameraInternal(false, tmp3);
                resetCloseFrontCameraStatus();
            } else if (!isScreenOn() && this.isFrontCameraOpened) {
                VLog.d(TAG, "screen is off , but front camera is close after user choose, will popup when screen on");
                setPendingPopupTask(false, false, tmp3, ps);
                startCheckScreenOn();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startCheckScreenOn() {
        if (this.mMainHandler.hasMessages(24)) {
            VLog.d(TAG, "screen is off, don't popup front camera now, clear the last MSG_CHECK_SCREEN_ON msg");
            this.mMainHandler.removeMessages(24);
        }
        VLog.d(TAG, "screen is off, don't popup front camera now, post MSG_CHECK_SCREEN_ON ");
        this.mMainHandler.sendEmptyMessageDelayed(24, 400L);
    }

    /* loaded from: classes.dex */
    private final class UIHandler extends Handler {
        public UIHandler() {
            super(UiThread.get().getLooper(), null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                if (PopupCameraManagerService.this.mPopupJammedDialog == null || (PopupCameraManagerService.this.mPopupJammedDialog != null && !PopupCameraManagerService.this.mPopupJammedDialog.isShowing())) {
                    PopupCameraManagerService.this.mPopupJammedDialog = new FrontCameraErrorDialog(PopupCameraManagerService.this.mContext, true, PopupCameraManagerService.this.mContext.getString(51249511));
                    PopupCameraManagerService.this.mPopupJammedDialog.show();
                }
            } else if (i == 1) {
                if (PopupCameraManagerService.this.mPushJammedDialog == null || (PopupCameraManagerService.this.mPushJammedDialog != null && !PopupCameraManagerService.this.mPushJammedDialog.isShowing())) {
                    PopupCameraManagerService.this.mPushJammedDialog = new FrontCameraJammedConfirmDialog(PopupCameraManagerService.this.mContext);
                    PopupCameraManagerService.this.mPushJammedDialog.show();
                }
            } else if (i == 2) {
                if (PopupCameraManagerService.this.mTemperatureProtectDialog == null || (PopupCameraManagerService.this.mTemperatureProtectDialog != null && !PopupCameraManagerService.this.mTemperatureProtectDialog.isShowing())) {
                    PopupCameraManagerService.this.mTemperatureProtectDialog = new FrontCameraTemperatureProtectDialog(PopupCameraManagerService.this.mContext, true, PopupCameraManagerService.this.mContext.getString(51249509), PopupCameraManagerService.this.mContext.getString(51249518));
                    PopupCameraManagerService.this.mTemperatureProtectDialog.show();
                }
            } else if (i == 3) {
                if (PopupCameraManagerService.this.mPressedDialog == null || (PopupCameraManagerService.this.mPressedDialog != null && !PopupCameraManagerService.this.mPressedDialog.isShowing())) {
                    PopupCameraManagerService.this.mPressedDialog = new FrontCameraPressedDialog(PopupCameraManagerService.this.mContext, true, PopupCameraManagerService.this.mContext.getString(51249513), PopupCameraManagerService.this);
                    PopupCameraManagerService.this.mPressedDialog.show();
                }
            } else if (i == 4) {
                if (PopupCameraManagerService.this.mDropProtectDialog == null || (PopupCameraManagerService.this.mDropProtectDialog != null && !PopupCameraManagerService.this.mDropProtectDialog.isShowing())) {
                    PopupCameraManagerService.this.mDropProtectDialog = new FrontCameraDropProtectDialog(PopupCameraManagerService.this.mContext, PopupCameraManagerService.this);
                    PopupCameraManagerService.this.mDropProtectDialog.show();
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private final class ShutDownBootReceiver extends BroadcastReceiver {
        private ShutDownBootReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                VLog.d(PopupCameraManagerService.TAG, "receive shutdown broadcast");
                PopupCameraManagerService.this.isSystemShutdowning = true;
                if (PopupCameraManagerService.this.mPendingFrontCameraCloseStatus != null) {
                    if (PopupCameraManagerService.this.mMainHandler.hasMessages(7)) {
                        PopupCameraManagerService.this.mMainHandler.removeMessages(7);
                    }
                    if (PopupCameraManagerService.this.mMainHandler.hasMessages(11)) {
                        PopupCameraManagerService.this.mMainHandler.removeMessages(11);
                    }
                    if (PopupCameraManagerService.this.mMainHandler.hasMessages(16)) {
                        PopupCameraManagerService.this.mMainHandler.removeMessages(16);
                    }
                    if (PopupCameraManagerService.this.mMainHandler.hasMessages(21)) {
                        PopupCameraManagerService.this.mMainHandler.removeMessages(21);
                    }
                    VLog.d(PopupCameraManagerService.TAG, "becuase system shutdown, we take back front camera immediately");
                    PopupCameraManagerService.this.takeupFrontCameraInternal(true, false);
                } else if (PopupCameraManagerService.this.isFrontCameraPopup) {
                    VLog.d(PopupCameraManagerService.TAG, "becuase system shutdown, we take back front camera immediately even if the front camera is opened");
                    PopupCameraManagerService.this.takeupFrontCameraInternal(true, false);
                }
                if (PopupCameraManagerService.this.mDataAnalysisHelper != null) {
                    PopupCameraManagerService.this.mDataAnalysisHelper.notifyShutdownBroadcast();
                }
            } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                VLog.d(PopupCameraManagerService.TAG, "receive bootcomplected broadcast");
                if (PopupCameraManagerService.this.mDataAnalysisHelper != null) {
                    PopupCameraManagerService.this.mDataAnalysisHelper.notifyBootCompletedBroadcast();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ScreenStatusReceiver extends BroadcastReceiver {
        private static final String SCREEN_OFF = "android.intent.action.SCREEN_OFF";
        private static final String SCREEN_ON = "android.intent.action.SCREEN_ON";

        private ScreenStatusReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (!PopupCameraManagerService.this.isFrontCameraOpened && PopupCameraManagerService.this.isCheckingPopupCameraPermission && PopupCameraManagerService.this.permissionCheckDialog != null && PopupCameraManagerService.this.permissionCheckDialog.isShowing()) {
                VLog.d(PopupCameraManagerService.TAG, "dismiss the last permission check dialog");
                PopupCameraManagerService.this.permissionCheckDialog.dismiss();
            }
            if (SCREEN_ON.equals(intent.getAction())) {
                VLog.d(PopupCameraManagerService.TAG, "receive screen on broadcast isFrontCameraOpened=" + PopupCameraManagerService.this.isFrontCameraOpened + " isFrontCameraPopup=" + PopupCameraManagerService.this.isFrontCameraPopup + " mHavePendingPopupTask=" + PopupCameraManagerService.this.mHavePendingPopupTask);
            } else if (SCREEN_OFF.equals(intent.getAction())) {
                VLog.d(PopupCameraManagerService.TAG, "receive screen off broadcast");
            }
            dismissTemperatureProtectDialog();
        }

        private void dismissTemperatureProtectDialog() {
            try {
                if (PopupCameraManagerService.this.mTemperatureProtectDialog != null && PopupCameraManagerService.this.mTemperatureProtectDialog.isAllowToDismiss()) {
                    VLog.d(PopupCameraManagerService.TAG, "try to dismissTemperatureProtectDialog");
                    PopupCameraManagerService.this.mTemperatureProtectDialog.dismiss();
                }
            } catch (Exception e) {
                VLog.d(PopupCameraManagerService.TAG, "dismissTemperatureProtectDialog error");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class MySensorEventListener implements SensorEventListener {
        private static final int MIN_DROP_INTERNAL = 2000;
        private long lastDropEventTime;

        private MySensorEventListener() {
            this.lastDropEventTime = 0L;
        }

        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent sensorEvent) {
            int type = sensorEvent.sensor.getType();
            VLog.d(PopupCameraManagerService.TAG, "onSensorChanged type=" + type);
            if (type == PopupCameraManagerService.this.mDropDetectSensorType) {
                if (PopupCameraManagerService.this.mMainHandler != null) {
                    PopupCameraManagerService.this.mMainHandler.sendEmptyMessage(18);
                }
            } else if (type == PopupCameraManagerService.this.mDropDownSensorType && sensorEvent.values[0] == 1.0f && PopupCameraManagerService.this.mUIHandler != null && PopupCameraManagerService.this.isFrontCameraPopup && PopupCameraManagerService.this.isFrontCameraOpened) {
                PopupCameraManagerService.this.takeupFrontCameraInternal(true, false);
                PopupCameraManagerService.this.mUIHandler.sendMessage(PopupCameraManagerService.this.mUIHandler.obtainMessage(4));
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int i) {
            VLog.d(PopupCameraManagerService.TAG, "onAccuracyChanged");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class CameraServerDeathRecipient implements IBinder.DeathRecipient {
        private CameraServerDeathRecipient() {
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            VLog.d(PopupCameraManagerService.TAG, "we got cameraserver death recipient");
            synchronized (PopupCameraManagerService.this.mCameraServiceBinderLock) {
                PopupCameraManagerService.this.mCameraServiceBinder = null;
            }
            if (PopupCameraManagerService.this.mMainHandler != null) {
                PopupCameraManagerService.this.mMainHandler.sendEmptyMessage(14);
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:33:0x0202, code lost:
        popupFrontCamera();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    protected void dump(java.io.FileDescriptor r7, java.io.PrintWriter r8, java.lang.String[] r9) {
        /*
            Method dump skipped, instructions count: 523
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.popupcamera.PopupCameraManagerService.dump(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[]):void");
    }
}