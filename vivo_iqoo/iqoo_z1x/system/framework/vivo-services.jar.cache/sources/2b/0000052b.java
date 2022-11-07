package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.IAppShareController;
import android.app.WindowConfiguration;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.input.InputManagerInternal;
import android.media.AudioManagerInternal;
import android.multidisplay.MultiDisplayManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IVibratorService;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.MergedConfiguration;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.WindowManager;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.notification.NotificationManagerInternal;
import com.android.server.pm.VivoPKMSLocManager;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerService;
import com.vivo.appshare.AppShareConfig;
import com.vivo.appshare.UnifiedConfig;
import com.vivo.face.common.data.Constants;
import com.vivo.services.daemon.VivoDmServiceProxy;
import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.superresolution.Constant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoAppShareManager {
    private static boolean DEBUG = false;
    private static final boolean IS_ENG = Build.TYPE.equals("eng");
    private static final boolean IS_LOG_CTRL_OPEN;
    private static final boolean IS_MTK;
    private static final int MAX_FREEZE_TIMEOUT = 10000;
    private static final String MODEL;
    private static final int MSG_APP_SHARE_CONNECTED_PEM = 10091;
    private static final int MSG_APP_SHARE_CREATE_SLEEP_TOKEN = 10120;
    private static final int MSG_APP_SHARE_DISCONNECTED_PEM = 10092;
    private static final int MSG_APP_SHARE_FINISH_SHARING = 10101;
    private static final int MSG_APP_SHARE_FORBID_APP_START = 10098;
    private static final int MSG_APP_SHARE_FORBID_ENTER_MULTIWINDOW_MODE = 10097;
    private static final int MSG_APP_SHARE_FORBID_IME_SWITCH = 10088;
    private static final int MSG_APP_SHARE_FORBID_USE_HARDWARE = 10086;
    private static final int MSG_APP_SHARE_FORBID_USE_HARDWARE_BY_BROADCAST = 10126;
    private static final int MSG_APP_SHARE_MOVE_TASK_TO_BACK = 10100;
    private static final int MSG_APP_SHARE_NOTIFY_ABOUT_FREE_FORM_MODE = 10124;
    private static final int MSG_APP_SHARE_NOTIFY_ABOUT_PIP_MODE = 10123;
    private static final int MSG_APP_SHARE_NOTIFY_FREEZE = 10096;
    private static final int MSG_APP_SHARE_NOTIFY_IME_STATUS_VIRTUAL_DISPLAY = 10128;
    private static final int MSG_APP_SHARE_NOTIFY_INPUT_MANAGER_STATE = 10127;
    private static final int MSG_APP_SHARE_NOTIFY_KEEP_ROTATION = 10125;
    private static final int MSG_APP_SHARE_NOTIFY_KEEP_SCREEN_ON = 10090;
    private static final int MSG_APP_SHARE_NOTIFY_PACKAGE_CHANGED = 10122;
    private static final int MSG_APP_SHARE_NOTIFY_TASK_SECURE = 10089;
    private static final int MSG_APP_SHARE_PROTECT_APP_SHARE_PACKAGE = 10093;
    private static final int MSG_APP_SHARE_REMOVE_SLEEP_TOKEN = 10121;
    private static final int MSG_APP_SHARE_REPORT_IME_SHOWN = 10087;
    private static final int MSG_APP_SHARE_REQUEST_ORIENTATION = 10094;
    private static final int MSG_APP_SHARE_SET_LOCK_SCREEN_SHOWN = 10129;
    private static final int MSG_APP_SHARE_START_ACTIVITY_DELAYED = 10095;
    private static final int MSG_APP_SHARE_START_ACTIVITY_FROM_RECENTS = 10099;
    private static final int MSG_APP_SHARE_TASK_SET = 10118;
    private static final int MSG_APP_SHARE_UPDATE_CONFIG = 10119;
    private static final int MSG_APP_SHARE_UPDATE_INPUTMETHOD_CONFIG = 10102;
    private static final int MSG_IME_MENU_DESTORY = 10111;
    private static final int MSG_IME_MOVE_NEEDED = 10112;
    private static final int MSG_IME_READYTOSHOW_TIMEOUT = 10109;
    private static final int MSG_IME_STATUS_CHANGED = 10115;
    private static final int MSG_IME_SWITCH_CLIENT_TIMEOUT = 10110;
    private static final int MSG_IME_SWITCH_TIMEOUT = 10116;
    private static final String PKG_IFLY_INPUTMETHOD = "com.iflytek.inputmethod";
    private static final String PKG_VIDEO_PLAYER = "com.android.VideoPlayer";
    private static final String PLATFORM;
    private static final int PROTECTED_INTERVAL = 1800000;
    private static final int REPORT_APPSHARE_DISPLAY_REMOVE_COMPLETED = 10104;
    private static final int REPORT_APPSHARE_DISPLAY_REMOVE_START = 10108;
    private static final int REPORT_APPSHARE_DISPLAY_SHOW_IME = 10114;
    private static final int REPORT_APPSHARE_KEEP_WINDOW_SIZE = 10105;
    private static final int REPORT_IME_MOVE_DISPLAY_COMPLETED = 10106;
    private static final int REPORT_INJECT_POINT = 10113;
    private static final int REPORT_REMOTE_CONTROL_CHANGED = 10117;
    private static final int REPORT_UPDATE_DISPLAY_ROTATION = 10107;
    private static final int REPORT_WINDOW_IME_STATUS = 10103;
    private static final String SOLUTION;
    private static final String TAG = "AppShare-VivoAppShareManager";
    private static final int TIME_KEEP_ACTIVITY_REQ = 600;
    private static final ArrayList<String> mWhiteImePkg;
    private int WINDOW_FREEZE_TIMEOUT_DURATION_APP_SHARED;
    private ActivityManagerService mActivityManagerService;
    private ActivityTaskManagerService mActivityTaskManagerService;
    private String mAppShareCallingPackage;
    private IAppShareController mAppShareController;
    private Handler mAppShareHandler;
    public String mAppSharePackageName;
    private boolean mAppShareRequestedOrientation;
    private boolean mAppShareRotateFreeze;
    private AppShareSettingsObserver mAppShareSettingsObserver;
    private ActivityTaskManagerInternal.SleepToken mAppShareSleepToken;
    public int mAppShareUserId;
    private WindowState mAppSharedKeepWindow;
    private AudioManagerInternal mAudioManagerInternal;
    private String mCameraExitReason;
    private Context mContext;
    private final IBinder.DeathRecipient mDeathRecipient;
    private DisplayManagerInternal mDisplayManagerInternal;
    private boolean mHasBound;
    private int mImeLastShownDisplayId;
    private boolean mImeReadyShow;
    private int mImeReadyShownDisplayId;
    private boolean mImeShown;
    private int mImeShownDisplayId;
    private int mImeSwitchToDisplayId;
    private boolean mImeSwitchToOther;
    private InputMethodManagerInternal mInputMethodManagerInternal;
    private InputMethodStatusChangedEvent mInputMethodNotify;
    InputMethodStatusChangedEvent mInputMethodStatusListener;
    private boolean mInputShowDialogInAppShared;
    private boolean mIsAppResumed;
    private boolean mIsAppShareForeground;
    private boolean mIsControlledByRemote;
    private boolean mIsVideoPlayerPortrait;
    private boolean mKeepRotation;
    private int mLastImeMovedDisplayId;
    private int mMainDeviceId;
    private boolean mMainSecure;
    VivoMultiWindowTransManager mMultiWindowWmsInstance;
    private boolean mNormalImeShown;
    private NotificationManagerInternal mNotificationManagerInternal;
    private IBinder mPEMService;
    private PowerManagerInternal mPowerManagerInternal;
    private int mRetryTimes;
    private boolean mSecImeShown;
    private final ServiceConnection mServiceConnection;
    private boolean mShareDisplayRemoving;
    private boolean mShareDisplayRemovingImeShown;
    private boolean mShouldBlockInjectMotionEventIgnoreRegion;
    private int mShowImeMenuDisplayId;
    private int mStatusBarsHeight;
    private int mSwitchMethodDisplayId;
    private boolean mSwitchingMethod;
    private IVibratorService mVibratorService;
    private VivoRatioControllerUtilsImpl mVivoRatioControllerUtilsImpl;
    private WindowManagerService mWMService;
    private int mWakefulness;

    /* loaded from: classes.dex */
    public interface InputMethodStatusChangedEvent {
        void onInputMethodStatusChanged(boolean z);
    }

    static {
        boolean equals = "yes".equals(SystemProperties.get("persist.sys.log.ctrl", "no"));
        IS_LOG_CTRL_OPEN = equals;
        DEBUG = equals || IS_ENG;
        MODEL = SystemProperties.get("ro.vivo.product.model", "unknown");
        PLATFORM = SystemProperties.get("ro.vivo.product.platform", "unknown");
        String str = SystemProperties.get("ro.vivo.product.solution", "unknown");
        SOLUTION = str;
        IS_MTK = "MTK".equals(str);
        ArrayList<String> arrayList = new ArrayList<>();
        mWhiteImePkg = arrayList;
        arrayList.add("com.sohu.inputmethod.sogou.vivo");
        mWhiteImePkg.add("com.sohu.inputmethod.sogou");
        mWhiteImePkg.add("com.baidu.input_vivo");
        mWhiteImePkg.add("com.baidu.input");
        mWhiteImePkg.add("com.tencent.qqpinyin");
    }

    private VivoAppShareManager() {
        this.mActivityManagerService = null;
        this.mActivityTaskManagerService = null;
        this.mWMService = null;
        this.mAppShareController = null;
        this.mAppShareHandler = null;
        this.mAppShareRequestedOrientation = false;
        this.mAppShareRotateFreeze = false;
        this.mMainSecure = false;
        this.mShareDisplayRemoving = false;
        this.mShareDisplayRemovingImeShown = false;
        this.mAppSharedKeepWindow = null;
        this.mMainDeviceId = 0;
        this.mStatusBarsHeight = 0;
        this.mContext = null;
        this.mIsControlledByRemote = false;
        this.mAppSharePackageName = null;
        this.mAppShareUserId = -1;
        this.mIsAppResumed = true;
        this.mShouldBlockInjectMotionEventIgnoreRegion = false;
        this.mIsVideoPlayerPortrait = true;
        this.mIsAppShareForeground = false;
        this.mCameraExitReason = null;
        this.mKeepRotation = false;
        this.mInputShowDialogInAppShared = false;
        this.WINDOW_FREEZE_TIMEOUT_DURATION_APP_SHARED = VivoPKMSLocManager.MAX_LOCATION_WAIT_TIME;
        this.mMultiWindowWmsInstance = VivoMultiWindowTransManager.getInstance();
        this.mPEMService = null;
        this.mHasBound = false;
        this.mServiceConnection = new ServiceConnection() { // from class: com.android.server.wm.VivoAppShareManager.1
            {
                VivoAppShareManager.this = this;
            }

            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                VSlog.i(VivoAppShareManager.TAG, "onServiceConnected: name = " + name + ", service = " + service);
                VivoAppShareManager.this.mAppShareHandler.removeMessages(VivoAppShareManager.MSG_APP_SHARE_DISCONNECTED_PEM);
                VivoAppShareManager.this.mAppShareHandler.removeMessages(VivoAppShareManager.MSG_APP_SHARE_CONNECTED_PEM);
                VivoAppShareManager.this.mAppShareHandler.sendMessage(Message.obtain(VivoAppShareManager.this.mAppShareHandler, VivoAppShareManager.MSG_APP_SHARE_CONNECTED_PEM, service));
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                VSlog.i(VivoAppShareManager.TAG, "onServiceDisconnected: name = " + name);
                VivoAppShareManager.this.mAppShareHandler.removeMessages(VivoAppShareManager.MSG_APP_SHARE_DISCONNECTED_PEM);
                VivoAppShareManager.this.mAppShareHandler.removeMessages(VivoAppShareManager.MSG_APP_SHARE_CONNECTED_PEM);
                VivoAppShareManager.this.mAppShareHandler.sendEmptyMessage(VivoAppShareManager.MSG_APP_SHARE_DISCONNECTED_PEM);
            }
        };
        this.mDeathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.wm.VivoAppShareManager.2
            {
                VivoAppShareManager.this = this;
            }

            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                VSlog.i(VivoAppShareManager.TAG, "binderDied(), " + AppShareConfig.APP_SHARE_PKG_NAME);
                synchronized (VivoAppShareManager.this.mActivityTaskManagerService.getGlobalLock()) {
                    VivoAppShareManager.this.mAppShareController = null;
                }
            }
        };
        this.mInputMethodStatusListener = new InputMethodStatusChangedEvent() { // from class: com.android.server.wm.VivoAppShareManager.4
            {
                VivoAppShareManager.this = this;
            }

            @Override // com.android.server.wm.VivoAppShareManager.InputMethodStatusChangedEvent
            public void onInputMethodStatusChanged(boolean immedately) {
                VivoAppShareManager.this.updateInputMethodStatus(immedately);
            }
        };
        this.mInputMethodNotify = null;
        this.mImeShown = false;
        this.mImeShownDisplayId = 0;
        this.mImeLastShownDisplayId = 0;
        this.mNormalImeShown = false;
        this.mSecImeShown = false;
        this.mImeReadyShow = false;
        this.mImeReadyShownDisplayId = 0;
        this.mImeSwitchToOther = false;
        this.mImeSwitchToDisplayId = 0;
        this.mSwitchingMethod = false;
        this.mSwitchMethodDisplayId = -1;
        this.mShowImeMenuDisplayId = 0;
        this.mWakefulness = 0;
        this.mRetryTimes = 0;
        this.mLastImeMovedDisplayId = 0;
        this.mVivoRatioControllerUtilsImpl = VivoRatioControllerUtilsImpl.getInstance();
    }

    /* loaded from: classes.dex */
    public static class VivoAppShareManagerHolder {
        private static final VivoAppShareManager sVivoAppShareManager = new VivoAppShareManager();

        private VivoAppShareManagerHolder() {
        }
    }

    public static VivoAppShareManager getInstance() {
        return VivoAppShareManagerHolder.sVivoAppShareManager;
    }

    public void initAms(ActivityManagerService activityManagerService) {
        this.mActivityManagerService = activityManagerService;
    }

    public void initAtms(ActivityTaskManagerService atm) {
        this.mActivityTaskManagerService = atm;
        this.mContext = atm.mContext;
        HandlerThread appShareThread = new HandlerThread("AppShare");
        appShareThread.start();
        this.mAppShareHandler = new AppShareHandler(appShareThread.getLooper());
    }

    public void initWms(WindowManagerService wms) {
        this.mWMService = wms;
    }

    public void registerAppShareObserver() {
        if (AppShareConfig.SUPPROT_APPSHARE && this.mAppShareSettingsObserver == null) {
            this.mAppShareSettingsObserver = new AppShareSettingsObserver();
        }
    }

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    /* loaded from: classes.dex */
    public final class AppShareHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public AppShareHandler(Looper looper) {
            super(looper);
            VivoAppShareManager.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case VivoAppShareManager.MSG_APP_SHARE_FORBID_USE_HARDWARE /* 10086 */:
                    VivoAppShareManager.this.handleForbidUseHardware(msg.arg1);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_REPORT_IME_SHOWN /* 10087 */:
                    VivoAppShareManager.this.reportImeShownToAppShare(msg.arg1);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_FORBID_IME_SWITCH /* 10088 */:
                    VivoAppShareManager.this.forbidIMESwitch(msg.arg1);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_NOTIFY_TASK_SECURE /* 10089 */:
                    VivoAppShareManager.this.notifyTaskSecure(((Boolean) msg.obj).booleanValue());
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_NOTIFY_KEEP_SCREEN_ON /* 10090 */:
                    VivoAppShareManager.this.notifyKeepScreenOn(((Boolean) msg.obj).booleanValue());
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_CONNECTED_PEM /* 10091 */:
                    VivoAppShareManager.this.handleServiceConnected((IBinder) msg.obj);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_DISCONNECTED_PEM /* 10092 */:
                    VivoAppShareManager.this.handleServiceDisConnected();
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_PROTECT_APP_SHARE_PACKAGE /* 10093 */:
                    VivoAppShareManager.this.handleProtectAppSharePackage();
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_REQUEST_ORIENTATION /* 10094 */:
                    VivoAppShareManager.this.handleSharedDisplayRequestOrientation(msg.arg1);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_START_ACTIVITY_DELAYED /* 10095 */:
                    VivoAppShareManager.this.startActivityDelayed(((Long) msg.obj).longValue());
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_NOTIFY_FREEZE /* 10096 */:
                    VivoAppShareManager.this.notifyAppSharedShouldFreeze();
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_FORBID_ENTER_MULTIWINDOW_MODE /* 10097 */:
                    VivoAppShareManager.this.forbidEnterMultiWindowMode(((Boolean) msg.obj).booleanValue());
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_FORBID_APP_START /* 10098 */:
                    VivoAppShareManager.this.forbidAppStart();
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_START_ACTIVITY_FROM_RECENTS /* 10099 */:
                    VivoAppShareManager.this.startActivityFromRecents(((Integer) msg.obj).intValue());
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_MOVE_TASK_TO_BACK /* 10100 */:
                    VivoAppShareManager.this.notifyAppShareMoveTaskToBack();
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_FINISH_SHARING /* 10101 */:
                    VivoAppShareManager.this.notifyAppShareFinishSharing();
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_UPDATE_INPUTMETHOD_CONFIG /* 10102 */:
                    VivoAppShareManager.this.handleUpdateConfigurationForInputMethod(msg.arg1, msg.arg2 == 1);
                    return;
                case VivoAppShareManager.REPORT_WINDOW_IME_STATUS /* 10103 */:
                    VivoAppShareManager.this.handleUpdateWindowInputMethodStatus((WindowState) msg.obj);
                    return;
                case VivoAppShareManager.REPORT_APPSHARE_DISPLAY_REMOVE_COMPLETED /* 10104 */:
                    VivoAppShareManager.this.handleAppShareDisplayRemoveCompleted(msg.arg1);
                    return;
                case VivoAppShareManager.REPORT_APPSHARE_KEEP_WINDOW_SIZE /* 10105 */:
                    VivoAppShareManager.this.handleAppShareKeepSizeCompleted();
                    return;
                case VivoAppShareManager.REPORT_IME_MOVE_DISPLAY_COMPLETED /* 10106 */:
                    VivoAppShareManager.this.handleInputMethodMoveDisplayCompleted((WindowState) msg.obj, msg.arg1);
                    return;
                case VivoAppShareManager.REPORT_UPDATE_DISPLAY_ROTATION /* 10107 */:
                    VivoAppShareManager.this.handleUpdateDisplayOrientation(msg.arg1);
                    return;
                case VivoAppShareManager.REPORT_APPSHARE_DISPLAY_REMOVE_START /* 10108 */:
                    VivoAppShareManager.this.handleDisplayRemoveStart(msg.arg1);
                    return;
                case VivoAppShareManager.MSG_IME_READYTOSHOW_TIMEOUT /* 10109 */:
                    VivoAppShareManager.this.handleImeReadyToShowTimeOut();
                    return;
                case VivoAppShareManager.MSG_IME_SWITCH_CLIENT_TIMEOUT /* 10110 */:
                    VivoAppShareManager.this.handleImeSwitchClientTimeOut();
                    return;
                case VivoAppShareManager.MSG_IME_MENU_DESTORY /* 10111 */:
                    VivoAppShareManager.this.updateImeMenuShowDisplay(0);
                    return;
                case VivoAppShareManager.MSG_IME_MOVE_NEEDED /* 10112 */:
                    VivoAppShareManager.this.handleImeMoveNeeded(msg.arg1 == 1);
                    return;
                case VivoAppShareManager.REPORT_INJECT_POINT /* 10113 */:
                    VivoAppShareManager.this.handleInjectPoint(msg.arg1, msg.arg2, ((Integer) msg.obj).intValue());
                    return;
                case VivoAppShareManager.REPORT_APPSHARE_DISPLAY_SHOW_IME /* 10114 */:
                    VivoAppShareManager.this.handleUpdateAppShareDisplayShowInputMethod(msg.arg1);
                    return;
                case VivoAppShareManager.MSG_IME_STATUS_CHANGED /* 10115 */:
                    VivoAppShareManager.this.handleImeStatusChanged(msg.arg1 == 1);
                    return;
                case VivoAppShareManager.MSG_IME_SWITCH_TIMEOUT /* 10116 */:
                    VivoAppShareManager.this.handleImeSwitchingTimeOut();
                    return;
                case VivoAppShareManager.REPORT_REMOTE_CONTROL_CHANGED /* 10117 */:
                    VivoAppShareManager.this.handleRemoveControllerChanged();
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_TASK_SET /* 10118 */:
                    VivoAppShareManager.this.appShareTaskSetLimit();
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_UPDATE_CONFIG /* 10119 */:
                    VivoAppShareManager.this.updateAppShareConfigInternal(msg.arg1);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_CREATE_SLEEP_TOKEN /* 10120 */:
                    VivoAppShareManager.this.handleCreateSleepTokenLocked();
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_REMOVE_SLEEP_TOKEN /* 10121 */:
                    VivoAppShareManager.this.handleRemoveSleepTokenLocked();
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_NOTIFY_PACKAGE_CHANGED /* 10122 */:
                    VivoAppShareManager.this.notifyAppSharePackageChangedInternal((String) msg.obj, msg.arg1);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_NOTIFY_ABOUT_PIP_MODE /* 10123 */:
                    VivoAppShareManager.this.notifyAppShareAboutPipModeInternal(msg.arg1 == 1);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_NOTIFY_ABOUT_FREE_FORM_MODE /* 10124 */:
                    VivoAppShareManager.this.notifyAppShareAboutFreeFormModeInternal(msg.arg1 == 1);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_NOTIFY_KEEP_ROTATION /* 10125 */:
                    VivoAppShareManager.this.handleAppShareKeepRotation(msg.arg1 == 1);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_FORBID_USE_HARDWARE_BY_BROADCAST /* 10126 */:
                    VivoAppShareManager.this.forbidUseHardwareByBroadcast(msg.arg1);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_NOTIFY_INPUT_MANAGER_STATE /* 10127 */:
                    VivoAppShareManager.this.updateAppShareStateForInputManagerInternal((String) msg.obj, true);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_NOTIFY_IME_STATUS_VIRTUAL_DISPLAY /* 10128 */:
                    VivoAppShareManager.this.handleNotifyImeStatusInShareDisplay(msg.arg1 == 1);
                    return;
                case VivoAppShareManager.MSG_APP_SHARE_SET_LOCK_SCREEN_SHOWN /* 10129 */:
                    VivoAppShareManager.this.handleLockScreenShown(((Boolean) msg.obj).booleanValue());
                    return;
                default:
                    return;
            }
        }
    }

    public void handleForbidUseHardware(int type) {
        VSlog.i(TAG, "handleForbidUseHardware: type = " + type);
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.forbidUseHardware(type);
            }
        }
    }

    public void reportImeShownToAppShare(int displayId) {
        VSlog.i(TAG, "reportImeShownToAppShare: displayId = " + displayId);
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.reportImeShownToAppShare((int) MSG_APP_SHARE_FORBID_USE_HARDWARE);
            }
        }
    }

    public void forbidIMESwitch(int displayId) {
        VSlog.i(TAG, "forbidIMESwitch: displayId = " + displayId);
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.forbidIMESwitch(displayId);
            }
        }
    }

    public void notifyTaskSecure(boolean secure) {
        VSlog.d(TAG, "notifyTaskSecure secure = " + secure);
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.notifyTaskSecure(secure);
            }
        }
    }

    public void notifyKeepScreenOn(boolean keepScreenOn) {
        VSlog.i(TAG, "notifyKeepScreenOn, keepScreenOn = " + keepScreenOn);
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.notifyKeepScreenOn(keepScreenOn);
            }
        }
    }

    public void handleServiceConnected(IBinder service) {
        this.mPEMService = service;
        this.mAppShareHandler.sendEmptyMessageDelayed(MSG_APP_SHARE_PROTECT_APP_SHARE_PACKAGE, 500L);
    }

    public void handleServiceDisConnected() {
        this.mPEMService = null;
        this.mAppShareHandler.removeMessages(MSG_APP_SHARE_PROTECT_APP_SHARE_PACKAGE);
    }

    public void handleProtectAppSharePackage() {
        String appSharePackageName;
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            appSharePackageName = this.mAppSharePackageName;
        }
        notifyPEM(appSharePackageName, true);
        notifyPEM(AppShareConfig.APP_SHARE_PKG_NAME, true);
        this.mAppShareHandler.sendEmptyMessageDelayed(MSG_APP_SHARE_PROTECT_APP_SHARE_PACKAGE, 1800000L);
    }

    public void handleSharedDisplayRequestOrientation(int requestedOrientation) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
            VSlog.d(TAG, "handleSharedDisplayRequestOrientation requestedOrientation : " + requestedOrientation);
        }
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.notifyAppShareRequestOrientation(requestedOrientation);
            }
        }
    }

    public void notifyAppSharedShouldFreeze() {
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.notifyAppSharedShouldFreeze();
            }
        }
    }

    public void startActivityDelayed(long delayMillis) {
        VSlog.i(TAG, "startActivityDelayed: delayMillis = " + delayMillis);
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.startActivityDelayed(delayMillis);
            }
        }
    }

    public void forbidEnterMultiWindowMode(boolean showOnAppShareDisplay) {
        VSlog.i(TAG, "forbidEnterMultiWindowMode: showOnAppShareDisplay = " + showOnAppShareDisplay);
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.forbidEnterMultiWindowMode(showOnAppShareDisplay);
            }
        }
    }

    public void forbidAppStart() {
        VSlog.i(TAG, "forbidAppStart: ");
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.forbidAppStart();
            }
        }
    }

    public void startActivityFromRecents(int taskId) {
        VSlog.i(TAG, "startActivityFromRecents: taskId = " + taskId);
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.startActivityFromRecents(taskId);
            }
        }
    }

    public void notifyAppShareMoveTaskToBack() {
        VSlog.i(TAG, "notifyAppShareMoveTaskToBack: ");
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.notifyAppShareMoveTaskToBack();
            }
        }
    }

    public void notifyAppShareFinishSharing() {
        VSlog.i(TAG, "notifyAppShareFinishSharing: ");
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.notifyAppShareFinishSharing();
            }
        }
    }

    private void notifyAppFinishResumed(String packageName, int userId) {
        VSlog.i(TAG, "notifyAppFinishResumed: " + packageName + ", userId = " + userId);
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppSharePackageName == null || this.mAppShareUserId == -1) {
                if (this.mAppShareController != null) {
                    this.mAppShareController.notifyAppFinishResumed(packageName, userId);
                }
            }
        }
    }

    public void handleUpdateConfigurationForInputMethod(int displayId, boolean force) {
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (displayId == -1) {
                return;
            }
            DisplayContent display = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent(displayId);
            if (display != null || force) {
                Configuration overrideConfig = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayOverrideConfiguration(displayId);
                Configuration globalConfig = new Configuration(this.mActivityTaskManagerService.getGlobalConfiguration());
                globalConfig.updateFrom(overrideConfig);
                globalConfig.seq = 0;
                SparseArray<WindowProcessController> pidMap = this.mActivityTaskManagerService.mProcessMap.getPidMap();
                for (int i = pidMap.size() - 1; i >= 0; i--) {
                    int pid = pidMap.keyAt(i);
                    boolean shouldInform = false;
                    if (pid != 0 && this.mVivoRatioControllerUtilsImpl.isCurrentInputMethodDisplayId(pid, displayId)) {
                        shouldInform = true;
                    }
                    if (shouldInform) {
                        WindowProcessController app = pidMap.get(pid);
                        app.onConfigurationChanged(globalConfig);
                    }
                }
            }
        }
    }

    public void handleUpdateWindowInputMethodStatus(WindowState target) {
        if (target == null) {
            return;
        }
        VSlog.d(TAG, "updateWindowInputMethodStatus for focused win : " + target);
        int[] status = getInputMethodStatus();
        if (status == null) {
            return;
        }
        target.reportImeAllStatus(status);
    }

    public void handleAppShareDisplayRemoveCompleted(int displayId) {
        this.mShareDisplayRemoving = false;
        this.mShareDisplayRemovingImeShown = false;
        synchronized (this.mWMService.mGlobalLock) {
            this.mWMService.mRoot.getDisplayContent(0).getVivoInjectInstance().moveImeToDisplay(displayId, true, true);
            WindowManagerService.H h = this.mWMService.mH;
            WindowManagerService.H h2 = this.mWMService.mH;
            h.sendEmptyMessage(61);
            if (this.mAppSharedKeepWindow != null) {
                this.mAppShareHandler.sendEmptyMessageDelayed(REPORT_APPSHARE_KEEP_WINDOW_SIZE, 500L);
            }
        }
        updateInputMethodDefaultShownValues();
    }

    public void handleAppShareKeepSizeCompleted() {
        this.mAppSharedKeepWindow = null;
        updateImeShowDisplayId(0);
    }

    public void handleInputMethodMoveDisplayCompleted(WindowState win, int displayId) {
        if (win == null) {
            return;
        }
        win.reportMoveToDisplayCompleted(displayId);
    }

    public void handleUpdateDisplayOrientation(int displayId) {
        synchronized (this.mWMService.mGlobalLock) {
            DisplayContent displayContent = this.mWMService.mRoot.getDisplayContent(displayId);
            if (displayContent == null) {
                return;
            }
            displayContent.relayoutForDisplayUpdate();
        }
    }

    public void handleDisplayRemoveStart(int displayId) {
        updateAppShareKeepRoation(false);
        updateShowSelectedDialog(false);
        synchronized (this.mWMService.mGlobalLock) {
            this.mWMService.mRoot.getDisplayContent(0).getVivoInjectInstance().moveImeToDisplay(displayId, true, true);
        }
        handleInputMethodProcessMoved(displayId);
        updateTaskSecure(false);
        setInputMethodStateChangeListener(null);
    }

    private void updateAppShareKeepRoation(boolean keep) {
        if (!keep) {
            this.mAppShareHandler.removeMessages(MSG_APP_SHARE_NOTIFY_KEEP_ROTATION);
        }
        this.mKeepRotation = keep;
    }

    public void handleImeReadyToShowTimeOut() {
        if (!isImeShown() && isImeReadyToShow() && getRetryTimes() <= 0) {
            VSlog.d(TAG, "handleImeReadyToShowTimeOut but ime do not shown one time. then wait.");
            this.mAppShareHandler.removeMessages(MSG_IME_READYTOSHOW_TIMEOUT);
            this.mAppShareHandler.sendEmptyMessageDelayed(MSG_IME_READYTOSHOW_TIMEOUT, 1500L);
            return;
        }
        if (DEBUG) {
            VSlog.d(TAG, "handleImeReadyToShowTimeOut");
        }
        onImeReadyToShowTimeOut();
    }

    public void handleImeSwitchClientTimeOut() {
        if (!isImeShown() && isImeSwitchClient() && getRetryTimes() <= 0) {
            if (DEBUG) {
                VSlog.d(TAG, "handleImeSwitchClientTimeOut but ime do not shown once time. then wait.");
            }
            this.mAppShareHandler.removeMessages(MSG_IME_SWITCH_CLIENT_TIMEOUT);
            this.mAppShareHandler.sendEmptyMessageDelayed(MSG_IME_SWITCH_CLIENT_TIMEOUT, 1500L);
            return;
        }
        if (DEBUG) {
            VSlog.d(TAG, "handleImeSwitchClientTimeOut");
        }
        onImeSwitchClientTimeOut();
    }

    public void handleImeMoveNeeded(boolean updateConfig) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        synchronized (this.mWMService.mGlobalLock) {
            if (isAppsharedMode()) {
                int imeDstDisplayId = getInputMethodDstDisplayId();
                VSlog.d(TAG, "handleImeMoveNeeded input method dst display : " + imeDstDisplayId);
                WindowState imeWindow = findInputMethodWindowLocked();
                if (imeWindow == null) {
                    return;
                }
                int curImeDisplayId = imeWindow.getDisplayId();
                VSlog.d(TAG, "handleInputMethodMoveNeeded input method dst display : " + imeDstDisplayId + ", cur ime display: " + curImeDisplayId);
                if (imeDstDisplayId == curImeDisplayId) {
                    return;
                }
                this.mWMService.mRoot.getDisplayContent(imeDstDisplayId).getVivoInjectInstance().moveImeToDisplay(curImeDisplayId, updateConfig, false);
            }
        }
    }

    public void updateImeMenuShowDisplay(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE || this.mShowImeMenuDisplayId == displayId) {
            return;
        }
        this.mShowImeMenuDisplayId = displayId;
        if (DEBUG) {
            VSlog.d(TAG, "updateImeMenuShowDisplay displayId: " + this.mShowImeMenuDisplayId);
        }
    }

    public void handleInjectPoint(int displayId, int pointX, int pointY) {
        synchronized (this.mWMService.mGlobalLock) {
            try {
                try {
                    DisplayContent displayContent = this.mWMService.mRoot.getDisplayContent(displayId);
                    if (displayContent == null) {
                        return;
                    }
                    InputManagerInternal mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
                    if (mInputManagerInternal == null || this.mMainDeviceId <= 0 || pointX <= 0 || pointY <= 0) {
                        return;
                    }
                    int deviceId = this.mMainDeviceId;
                    try {
                        try {
                            mInputManagerInternal.injectInputEvent(generationMotionEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 0, pointX, pointY, deviceId, displayId), 0);
                            mInputManagerInternal.injectInputEvent(generationMotionEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 1, pointX, pointY, deviceId, displayId), 0);
                        } catch (Exception e) {
                            e = e;
                            e.printStackTrace();
                        }
                    } catch (Exception e2) {
                        e = e2;
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public void handleUpdateAppShareDisplayShowInputMethod(int displayId) {
        this.mWMService.setShouldShowIme(displayId, true);
        this.mWMService.setShouldShowSystemDecors(displayId, false);
    }

    public void handleImeStatusChanged(boolean shown) {
        InputMethodManagerInternal inputMethodManagerInternal;
        if (!shown) {
            return;
        }
        this.mAppShareHandler.removeMessages(MSG_IME_READYTOSHOW_TIMEOUT);
        this.mAppShareHandler.removeMessages(MSG_IME_SWITCH_CLIENT_TIMEOUT);
        int displayId = getImeShownDisplayId();
        int mWakefulness = getWakefulness();
        VSlog.d(TAG, "ime status changed displayId: " + displayId + ", wwakefulness: " + mWakefulness);
        if (mWakefulness != 1 && isAppsharedDisplayId(displayId) && (inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class)) != null) {
            inputMethodManagerInternal.setInteractive(true, mWakefulness);
        }
    }

    public void handleImeSwitchingTimeOut() {
        updateImeSwitching(false);
    }

    public void handleRemoveControllerChanged() {
        boolean imeShow = isImeShown();
        int imeShowDisplayId = getImeShownDisplayId();
        VSlog.d(TAG, "handleRemoveControllerChanged ime shown : " + imeShow + ", ime show DisplayId : " + imeShowDisplayId);
        if (!imeShow || !MultiDisplayManager.isAppShareDisplayId(imeShowDisplayId) || isAppShareForeground()) {
            return;
        }
        synchronized (this.mWMService.mGlobalLock) {
            WindowState win = getFocusedWindowForDisplay(imeShowDisplayId);
            updateImeShowDisplayId(0);
            if (win != null) {
                win.reportImeHide(0);
            }
        }
    }

    public boolean needSkipAssignRotation(DisplayContent displayContext) {
        if (AppShareConfig.SUPPROT_APPSHARE && displayContext != null) {
            return isAppsharedDisplayId(displayContext.getDisplayId()) || displayContext.isAppSharedRecordDisplay();
        }
        return false;
    }

    public void appShareTaskSetLimit() {
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            ActivityRecord r = getAppShareDisplayTopRunningActivityLocked();
            if (r != null && r.packageName != null) {
                if (!this.mIsAppResumed) {
                    appShareTaskSet("0f", r);
                } else {
                    appShareTaskSetWithPackageName(r.packageName, r);
                }
            }
        }
    }

    private void appShareTaskSetWithPackageName(String packageName, ActivityRecord r) {
        VSlog.i(TAG, "appShareTaskSetWithPackageName: " + packageName + ", PLATFORM = " + PLATFORM);
        if (PLATFORM.equals("SDM845") || PLATFORM.equals("SM8250") || PLATFORM.equals("SM8150") || PLATFORM.equals("MTK6885")) {
            appShareTaskSet("3f", r);
        } else if (PLATFORM.equals("SM7250") || PLATFORM.equals("ERD9630")) {
            if (AppShareConfig.getInstance().isGameApp(packageName)) {
                appShareTaskSet("7f", r);
            } else {
                appShareTaskSet("3f", r);
            }
        }
    }

    private void appShareTaskSet(String mask, ActivityRecord r) {
        if (r == null || r.app == null) {
            VSlog.e(TAG, "r or r.app == null.");
            return;
        }
        VSlog.i(TAG, "appShareTaskSet package = " + r.packageName);
        String var = mask + " " + r.app.mPid;
        this.mAppShareHandler.post(new TaskSetRunnable(var));
    }

    public void appShareTaskSetInternal(String var) {
        try {
            VivoDmServiceProxy vivoDmSrvProxy = VivoDmServiceProxy.asInterface(ServiceManager.getService("vivo_daemon.service"));
            if (vivoDmSrvProxy != null) {
                if (!Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(SystemProperties.get("persist.vivo.vivo_daemon", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK))) {
                    String cmd_cpu_task_set = "kMEiqYA8Od8G6x5TSG/9v00/t0KkuO+ra0Eup9aLdntONa+LhYG+sEw8ylhNorq3VAAMndAvsaDRIlkA9Z0gMJm2/m7vDyTmB2om8R1owF/Ju4x7/tVo+3Exs40q3ZafZF7MLs+q4l6JpktVoLrTp0r4aT0P4dSFvQFCGRtaiZRnfNJh/gbOcQEQz/cNfQluj1rPaNwWXnarGjR42Jpz8JeGTrH9bXjvZN2NPh9hlMqaBZpFp74N43mrbuwX1Njr4wjW+fPkRzfJZge2b2i6iTQSVYqsa1Xeei2iQ/aeG3NqUIxqz4O3VRi2IJhG6T4lpmzKqQPLDCydTimmKsi+2A==?" + var;
                    VSlog.w(TAG, "appShareTaskSet runShell taskset -p " + var);
                    vivoDmSrvProxy.runShell(cmd_cpu_task_set);
                } else {
                    vivoDmSrvProxy.runShell("taskset -p " + var);
                }
            }
        } catch (Exception e) {
            VSlog.e(TAG, "get vivo_daemon.service failed");
        }
    }

    /* loaded from: classes.dex */
    public final class TaskSetRunnable implements Runnable {
        final String var;

        TaskSetRunnable(String var) {
            VivoAppShareManager.this = r1;
            this.var = var;
        }

        @Override // java.lang.Runnable
        public void run() {
            VivoAppShareManager.this.appShareTaskSetInternal(this.var);
        }
    }

    public void updateAppShareConfigInternal(int type) {
        VSlog.i(TAG, "updateAppShareConfigInternal: type = " + type);
        boolean updateStartInAppShareDisplayBlackList = false;
        boolean updateAll = type == 0;
        boolean updateForceStartToAppShareDisplayWhiteList = updateAll || type == 1;
        boolean updateContentProviderBlackList = updateAll || type == 2;
        if (updateAll || type == 3) {
            updateStartInAppShareDisplayBlackList = true;
        }
        List<String> forceStartToAppShareDisplayWhiteList = null;
        if (updateForceStartToAppShareDisplayWhiteList) {
            forceStartToAppShareDisplayWhiteList = UnifiedConfig.getInstance().getWhiteList();
        }
        List<String> contentProviderBlackList = null;
        if (updateContentProviderBlackList) {
            contentProviderBlackList = UnifiedConfig.getInstance().getContentProviderBlackList();
        }
        List<String> startInAppShareDisplayBlackList = null;
        if (updateStartInAppShareDisplayBlackList) {
            startInAppShareDisplayBlackList = UnifiedConfig.getInstance().getActivityBlackList();
        }
        if (updateAll) {
            AppShareConfig.getInstance().setConfigChangeListener();
        }
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (updateForceStartToAppShareDisplayWhiteList) {
                try {
                    AppShareConfig.getInstance().updateForceStartToAppShareDisplayWhiteList(forceStartToAppShareDisplayWhiteList);
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (updateContentProviderBlackList) {
                AppShareConfig.getInstance().updateContentProviderBlackList(contentProviderBlackList);
            }
            if (updateStartInAppShareDisplayBlackList) {
                AppShareConfig.getInstance().updateStartInAppShareDisplayBlackList(startInAppShareDisplayBlackList);
            }
        }
    }

    public void handleCreateSleepTokenLocked() {
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareSleepToken == null) {
                this.mAppShareSleepToken = this.mActivityTaskManagerService.acquireSleepToken("Display-off", (int) MSG_APP_SHARE_FORBID_USE_HARDWARE);
            }
        }
    }

    public void handleRemoveSleepTokenLocked() {
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareSleepToken != null) {
                this.mAppShareSleepToken.release();
                this.mAppShareSleepToken = null;
            }
        }
    }

    public void notifyAppSharePackageChangedInternal(String packageName, int userId) {
        String lastAppSharePackageName;
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            lastAppSharePackageName = this.mAppSharePackageName;
            this.mAppSharePackageName = packageName;
            this.mAppShareUserId = userId;
        }
        VSlog.i(TAG, "notifyAppSharePackageChanged: packageName = " + packageName + ", userId = " + userId + ", lastAppSharePackageName = " + lastAppSharePackageName);
        boolean start = (TextUtils.isEmpty(packageName) || userId == -1) ? false : true;
        if (start) {
            bindPEMService();
        } else {
            notifyPEM(lastAppSharePackageName, false);
            notifyPEM(AppShareConfig.APP_SHARE_PKG_NAME, false);
            unbindPEMService();
        }
        notifyOtherServiceAppSharePackageChanged(packageName, userId);
        this.mWMService.mInputManager.setGameMode(isGameApp(packageName));
        updateAppShareStateForInputManagerInternal(start ? packageName : lastAppSharePackageName, start);
    }

    private void bindPEMService() {
        VSlog.i(TAG, "bindPEMService: mHasBound = " + this.mHasBound);
        if (this.mHasBound) {
            return;
        }
        try {
            Intent intent = new Intent("com.vivo.pem.PemService");
            intent.setPackage(ProxyConfigs.CTRL_MODULE_PEM);
            this.mHasBound = this.mContext.bindService(intent, this.mServiceConnection, 1);
            VSlog.i(TAG, "bindPEMService, mHasBound = " + this.mHasBound);
        } catch (Exception e) {
            VSlog.e(TAG, "bindPEMService: " + e);
        }
    }

    private void unbindPEMService() {
        VSlog.i(TAG, "unbindPEMService: mHasBound = " + this.mHasBound);
        if (!this.mHasBound) {
            return;
        }
        try {
            this.mContext.unbindService(this.mServiceConnection);
            this.mHasBound = false;
            this.mAppShareHandler.removeMessages(MSG_APP_SHARE_DISCONNECTED_PEM);
            this.mAppShareHandler.removeMessages(MSG_APP_SHARE_CONNECTED_PEM);
            this.mAppShareHandler.sendEmptyMessage(MSG_APP_SHARE_DISCONNECTED_PEM);
            VSlog.i(TAG, "unbindPEMService: success.");
        } catch (Exception e) {
            VSlog.e(TAG, "unbindPEMService: " + e);
        }
    }

    private boolean notifyPEM(String pkgName, boolean protect) {
        VSlog.i(TAG, "notifyPEM: pkgName = " + pkgName + ", protect = " + protect);
        boolean result = false;
        if (TextUtils.isEmpty(pkgName)) {
            VSlog.e(TAG, "notifyPEM: pkgName is null.");
            return false;
        } else if (this.mPEMService == null) {
            VSlog.e(TAG, "notifyPEM: mPEMService is null.");
            return false;
        } else {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                try {
                    data.writeInterfaceToken("com.vivo.pem.IPemr");
                    data.writeInt(106);
                    data.writeInt(protect ? 30 : 0);
                    data.writeString(pkgName);
                    this.mPEMService.transact(7, data, reply, 0);
                    reply.readException();
                    result = reply.readInt() != 0;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                reply.recycle();
                data.recycle();
                VSlog.i(TAG, "notifyPEM: result = " + result);
                return result;
            } catch (Throwable th) {
                reply.recycle();
                data.recycle();
                throw th;
            }
        }
    }

    private void notifyOtherServiceAppSharePackageChanged(String packageName, int userId) {
        this.mActivityManagerService.notifyAppSharePackageChanged(packageName, userId);
        if (this.mPowerManagerInternal == null) {
            this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        }
        PowerManagerInternal powerManagerInternal = this.mPowerManagerInternal;
        if (powerManagerInternal != null) {
            powerManagerInternal.notifyAppSharePackageChanged(packageName, userId);
        }
        if (this.mDisplayManagerInternal == null) {
            this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        }
        DisplayManagerInternal displayManagerInternal = this.mDisplayManagerInternal;
        if (displayManagerInternal != null) {
            displayManagerInternal.notifyAppSharePackageChanged(packageName, userId);
        }
        if (this.mNotificationManagerInternal == null) {
            this.mNotificationManagerInternal = (NotificationManagerInternal) LocalServices.getService(NotificationManagerInternal.class);
        }
        NotificationManagerInternal notificationManagerInternal = this.mNotificationManagerInternal;
        if (notificationManagerInternal != null) {
            notificationManagerInternal.notifyAppSharePackageChanged(packageName, userId);
        }
        if (this.mVibratorService == null) {
            this.mVibratorService = IVibratorService.Stub.asInterface(ServiceManager.getService("vibrator"));
        }
        IVibratorService iVibratorService = this.mVibratorService;
        if (iVibratorService != null) {
            try {
                iVibratorService.notifyAppSharePackageChanged(packageName, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (this.mInputMethodManagerInternal == null) {
            this.mInputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
        }
        InputMethodManagerInternal inputMethodManagerInternal = this.mInputMethodManagerInternal;
        if (inputMethodManagerInternal != null) {
            inputMethodManagerInternal.notifyAppSharePackageChanged(packageName, userId);
        }
        if (this.mAudioManagerInternal == null) {
            this.mAudioManagerInternal = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
        }
        AudioManagerInternal audioManagerInternal = this.mAudioManagerInternal;
        if (audioManagerInternal != null) {
            audioManagerInternal.notifyAppSharePackageChanged(packageName, userId);
        }
    }

    private boolean isGameApp(String packageName) {
        return packageName != null && packageName.equals("com.tencent.tmgp.sgame");
    }

    public void notifyAppShareAboutPipModeInternal(boolean enter) {
        VSlog.i(TAG, "notifyAppShareAboutPipModeInternal: enter = " + enter);
        Intent intent = new Intent("vivo.intent.action.PIP_MODE");
        intent.setPackage(AppShareConfig.APP_SHARE_PKG_NAME);
        intent.putExtra("enter", enter);
        this.mContext.sendBroadcast(intent);
    }

    public void notifyAppShareAboutFreeFormModeInternal(boolean enter) {
        VSlog.i(TAG, "notifyAppShareAboutFreeFormModeInternal: enter = " + enter);
        Intent intent = new Intent("vivo.intent.action.FREE_FORM_MODE");
        intent.setPackage(AppShareConfig.APP_SHARE_PKG_NAME);
        intent.putExtra("enter", enter);
        this.mContext.sendBroadcast(intent);
    }

    public void handleAppShareKeepRotation(boolean keep) {
        updateAppShareKeepRoation(keep);
        if (keep) {
            return;
        }
        synchronized (this.mWMService.mGlobalLock) {
            if (isAppsharedMode() && !isAppSharedRemoving()) {
                VSlog.d(TAG, "reset keep rotation need relayout");
                DisplayContent displayContent = this.mWMService.mRoot.getDisplayContent((int) MSG_APP_SHARE_FORBID_USE_HARDWARE);
                if (displayContent == null) {
                    return;
                }
                displayContent.setLayoutNeeded();
                this.mWMService.mWindowPlacerLocked.performSurfacePlacement();
            }
        }
    }

    public void forbidUseHardwareByBroadcast(int hardwareType) {
        AppShareConfig.getInstance().sendInterceptHardwareBroadCast(hardwareType);
    }

    public void updateAppShareStateForInputManagerInternal(String packageName, boolean start) {
        ActivityStack stack;
        ActivityRecord r;
        if (!PKG_VIDEO_PLAYER.equals(packageName)) {
            return;
        }
        boolean block = false;
        boolean portrait = true;
        if (start) {
            synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                DisplayContent display = getAppShareDisplay();
                if (display != null) {
                    boolean z = true;
                    if (display.getChildCount() == 1 && (stack = display.getFocusedStack()) != null && stack.getChildCount() == 1 && (r = stack.topRunningActivityLocked()) != null && r.mActivityComponent != null && "com.android.VideoPlayer/.MovieViewPublicActivity".equals(r.mActivityComponent.flattenToShortString())) {
                        block = true;
                        int orientation = r.getOrientation();
                        if (orientation == 0 || orientation == 8 || orientation == 6 || orientation == 11) {
                            z = false;
                        }
                        portrait = z;
                    }
                }
            }
        }
        this.mShouldBlockInjectMotionEventIgnoreRegion = block;
        this.mIsVideoPlayerPortrait = portrait;
        VSlog.i(TAG, "updateAppShareStateForInputManagerInternal: block = " + block + ", portrait = " + portrait);
    }

    public void handleNotifyImeStatusInShareDisplay(boolean shown) {
        VSlog.d(TAG, "notify to app share ime shown in virtual display: " + shown);
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mAppShareController != null) {
                this.mAppShareController.notifyImeStatusInShareDisplay(shown);
            }
        }
    }

    public void handleLockScreenShown(boolean shown) {
        Settings.System.putInt(this.mContext.getContentResolver(), "APP_SHARE_KEYGUARD_STATE", shown ? 1 : 0);
    }

    /* loaded from: classes.dex */
    public final class AppShareSettingsObserver extends ContentObserver {
        private final Uri appShareUri;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public AppShareSettingsObserver() {
            super(r3.mAppShareHandler);
            VivoAppShareManager.this = r3;
            this.appShareUri = Settings.System.getUriFor("appshare_input_handle");
            ContentResolver resolver = r3.mContext.getContentResolver();
            resolver.registerContentObserver(this.appShareUri, false, this);
            updateAppShareInputHandle(resolver);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            updateAppShareInputHandle(VivoAppShareManager.this.mContext.getContentResolver());
        }

        private void updateAppShareInputHandle(ContentResolver resolver) {
            boolean controlledByRemote;
            synchronized (VivoAppShareManager.this.mActivityTaskManagerService.getGlobalLock()) {
                String appShareInputHandle = Settings.System.getString(resolver, "appshare_input_handle");
                if (TextUtils.isEmpty(appShareInputHandle)) {
                    appShareInputHandle = "local";
                }
                VivoAppShareManager vivoAppShareManager = VivoAppShareManager.this;
                controlledByRemote = "remote".equals(appShareInputHandle);
                vivoAppShareManager.mIsControlledByRemote = controlledByRemote;
                if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                    VSlog.i(VivoAppShareManager.TAG, "updateAppShareInputHandle, mIsControlledByRemote = " + VivoAppShareManager.this.mIsControlledByRemote);
                }
            }
            VivoAppShareManager.this.updateAppShareInputHandleToOtherService(controlledByRemote);
        }
    }

    public void updateAppShareInputHandleToOtherService(boolean controlledByRemote) {
        if (this.mVibratorService == null) {
            this.mVibratorService = IVibratorService.Stub.asInterface(ServiceManager.getService("vibrator"));
        }
        IVibratorService iVibratorService = this.mVibratorService;
        if (iVibratorService != null) {
            try {
                iVibratorService.updateAppShareInputHandle(controlledByRemote);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (this.mAudioManagerInternal == null) {
            this.mAudioManagerInternal = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
        }
        AudioManagerInternal audioManagerInternal = this.mAudioManagerInternal;
        if (audioManagerInternal != null) {
            audioManagerInternal.updateAppShareInputHandle(controlledByRemote);
        }
    }

    public boolean blockContentProviderByAppShare(ApplicationInfo info, String name) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            if (info != null) {
                String callingPackage = info.packageName;
                int callingUserId = UserHandle.getUserId(info.uid);
                if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                    VSlog.d(TAG, "blockContentProviderByAppShare, callingPackage = " + callingPackage + ", callingUserId = " + callingUserId + ", name = " + name);
                }
                synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                    if (isOnAppShareDisplay(callingPackage, callingUserId) && isControlledByRemote() && AppShareConfig.getInstance().isContentProviderBlackList(name)) {
                        DisplayContent appShareDisplay = getAppShareDisplay();
                        int numActivities = 0;
                        for (int tdaNdx = appShareDisplay.getTaskDisplayAreaCount() - 1; tdaNdx >= 0; tdaNdx--) {
                            TaskDisplayArea taskDisplayArea = appShareDisplay.getTaskDisplayAreaAt(tdaNdx);
                            for (int stackNdx = taskDisplayArea.getStackCount() - 1; stackNdx >= 0; stackNdx--) {
                                ActivityStack stack = taskDisplayArea.getStackAt(stackNdx);
                                numActivities += getAllActivities(stack).size();
                            }
                        }
                        VSlog.i(TAG, "This content provider will start activity of other app when app sharing, so we block it!");
                        if (numActivities > 1) {
                            this.mAppShareHandler.sendEmptyMessage(MSG_APP_SHARE_FORBID_APP_START);
                        }
                        return true;
                    }
                }
            } else {
                VSlog.e(TAG, "blockContentProviderByAppShare, info is null!");
            }
            return false;
        }
        return false;
    }

    private ArrayList<ActivityRecord> getAllActivities(ActivityStack stack) {
        final ArrayList<ActivityRecord> activityRecords = new ArrayList<>();
        stack.forAllActivities(new Consumer() { // from class: com.android.server.wm.-$$Lambda$VivoAppShareManager$xvdjFm9BCgtF5qUG4cEP6QP5Fmo
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                VivoAppShareManager.lambda$getAllActivities$0(activityRecords, (ActivityRecord) obj);
            }
        });
        return activityRecords;
    }

    public static /* synthetic */ void lambda$getAllActivities$0(ArrayList activityRecords, ActivityRecord r) {
        activityRecords.add(r);
    }

    private ArrayList<Task> getAllTasks(ActivityStack stack) {
        final ArrayList<Task> tasks = new ArrayList<>();
        stack.forAllTasks(new Consumer() { // from class: com.android.server.wm.-$$Lambda$VivoAppShareManager$toiNshFowKZbe5yb2n7Sih0WW9Q
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                VivoAppShareManager.lambda$getAllTasks$1(tasks, (Task) obj);
            }
        });
        return tasks;
    }

    public static /* synthetic */ void lambda$getAllTasks$1(ArrayList tasks, Task t) {
        tasks.add(t);
    }

    public boolean isControlledByRemote() {
        boolean z;
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return false;
        }
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            z = this.mIsControlledByRemote;
        }
        return z;
    }

    public String getForegroundPackage() {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                    ActivityStack topStack = getDefaultDisplay().getTopStack();
                    if (topStack == null) {
                        VSlog.e(TAG, "getForegroundPackage, topStack is null.");
                        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    }
                    ActivityRecord r = adjustTopActivityForStackLocked(topStack, topStack.topRunningActivityLocked());
                    if (r == null) {
                        VSlog.e(TAG, "getForegroundPackage, r is null.");
                        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    } else if (r.mActivityComponent == null) {
                        VSlog.e(TAG, "getForegroundPackage, r.realActivity is null.");
                        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    } else if (r.app == null) {
                        VSlog.e(TAG, "getForegroundPackage, r.app is null.");
                        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    } else {
                        WindowProcessController app = r.app;
                        int pid = app.mPid;
                        int uid = app.mUid;
                        String packageName = r.mActivityComponent.getPackageName();
                        if (this.mAppSharePackageName == null && this.mAppShareUserId == -1 && AppShareConfig.getInstance().isPackageInWhiteList(packageName) && !TextUtils.equals(app.mName, packageName)) {
                            pid = getMainProcessPidLocked(uid, pid);
                            VSlog.i(TAG, "getForegroundPackage, change pid from " + r.app.mPid + " to " + pid + ", uid = " + uid + ", packageName = " + packageName + ", processName = " + app.mName);
                        }
                        return packageName + "," + r.mActivityComponent.getClassName() + "," + r.mUserId + "," + pid + "," + hasMultiPackageLocked(topStack);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    private ActivityRecord adjustTopActivityForStackLocked(ActivityStack topStack, ActivityRecord r) {
        ActivityRecord r2;
        if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
            VSlog.i(TAG, "adjustTopActivityForStackLocked, topStack = " + topStack + ", r = " + r);
        }
        if (!isGrantPermissionsLocked(r)) {
            return r;
        }
        int numActivities = getAllActivities(topStack).size();
        if (numActivities == 1) {
            r2 = null;
        } else {
            r2 = null;
            final ArrayList<ActivityRecord> activityRecords = new ArrayList<>();
            topStack.forAllActivities(new Consumer() { // from class: com.android.server.wm.-$$Lambda$VivoAppShareManager$FnH9wsFtOgJUYtwXuHJsDG8c9FM
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    VivoAppShareManager.lambda$adjustTopActivityForStackLocked$2(activityRecords, (ActivityRecord) obj);
                }
            });
            int size = activityRecords.size();
            if (size > 1) {
                int activityNdx = 0;
                while (true) {
                    if (activityNdx >= size) {
                        break;
                    } else if (activityRecords.get(activityNdx) != r) {
                        activityNdx++;
                    } else if (activityNdx + 1 < size) {
                        r2 = activityRecords.get(activityNdx + 1);
                    }
                }
            }
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
            VSlog.i(TAG, "adjustTopActivityForStackLocked, numActivities = " + numActivities + ", r = " + r2);
        }
        return r2;
    }

    public static /* synthetic */ void lambda$adjustTopActivityForStackLocked$2(ArrayList activityRecords, ActivityRecord record) {
        if (!record.finishing && record.okToShowLocked() && record.visibleIgnoringKeyguard) {
            activityRecords.add(record);
        }
    }

    private int getMainProcessPidLocked(int uid, int candidatePid) {
        SparseArray<WindowProcessController> pidMap = this.mActivityTaskManagerService.mProcessMap.getPidMap();
        for (int i = pidMap.size() - 1; i >= 0; i--) {
            int pid = pidMap.keyAt(i);
            WindowProcessController app = pidMap.get(pid);
            if (app != null && app.mUid == uid && app.mInfo != null && TextUtils.equals(app.mName, app.mInfo.packageName)) {
                return app.mPid;
            }
        }
        return candidatePid;
    }

    public boolean isOnAppShareDisplay(String packageName, int userId) {
        if (AppShareConfig.SUPPROT_APPSHARE && !TextUtils.isEmpty(packageName)) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                    DisplayContent activityDisplay = getAppShareDisplay();
                    if (activityDisplay == null) {
                        return false;
                    }
                    for (int tdaNdx = activityDisplay.getTaskDisplayAreaCount() - 1; tdaNdx >= 0; tdaNdx--) {
                        TaskDisplayArea taskDisplayArea = activityDisplay.getTaskDisplayAreaAt(tdaNdx);
                        for (int stackNdx = taskDisplayArea.getStackCount() - 1; stackNdx >= 0; stackNdx--) {
                            ActivityStack stack = taskDisplayArea.getStackAt(stackNdx);
                            ActivityRecord top = adjustTopActivityForStackLocked(stack, stack.topRunningActivityLocked());
                            String sharingPackageName = (top == null || top.mActivityComponent == null) ? null : top.mActivityComponent.getPackageName();
                            if (TextUtils.equals(packageName, sharingPackageName) && top.mUserId == userId) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return false;
    }

    public boolean moveAppToDisplay(String packageName, int userId, int displayId) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            this.mActivityTaskManagerService.mAmInternal.enforceCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "moveAppToDisplay()");
            synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                long ident = Binder.clearCallingIdentity();
                if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                    VSlog.d(TAG, "moveAppToDisplay: moving packageName=" + packageName + ", userId=" + userId + " to displayId=" + displayId);
                }
                if (!TextUtils.isEmpty(packageName) && !"com.bbk.launcher2".equals(packageName)) {
                    if (!MultiDisplayManager.isAppShareDisplayId(displayId)) {
                        VSlog.e(TAG, "moveAppToDisplay, displayId " + displayId + " error.");
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                    DisplayContent appShareDisplay = getAppShareDisplay();
                    if (appShareDisplay == null) {
                        VSlog.e(TAG, "moveAppToDisplay, displayId " + displayId + " not exist.");
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                    DisplayContent defaultDisplay = getDefaultDisplay();
                    List<ActivityStack> targetStacks = new ArrayList<>();
                    int mainStackCount = defaultDisplay.getChildCount();
                    int i = 1;
                    if (mainStackCount < 1) {
                        VSlog.e(TAG, "moveAppToDisplay, mainStackCount = " + mainStackCount);
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                    int tdaNdx = defaultDisplay.getTaskDisplayAreaCount() - 1;
                    while (tdaNdx >= 0) {
                        TaskDisplayArea taskDisplayArea = defaultDisplay.getTaskDisplayAreaAt(tdaNdx);
                        int stackNdx = taskDisplayArea.getStackCount() - i;
                        while (stackNdx >= 0) {
                            ActivityStack stack = taskDisplayArea.getStackAt(stackNdx);
                            if (isMultiWindowStack(stack)) {
                                VSlog.e(TAG, "moveAppToDisplay, windowingMode = " + stack.getWindowingMode());
                                Binder.restoreCallingIdentity(ident);
                                return false;
                            }
                            DisplayContent defaultDisplay2 = defaultDisplay;
                            int mainStackCount2 = mainStackCount;
                            if (!hasMultiPackageLocked(stack)) {
                                ActivityRecord top = adjustTopActivityForStackLocked(stack, stack.topRunningActivityLocked());
                                String currentPackageName = (top == null || top.mActivityComponent == null) ? null : top.mActivityComponent.getPackageName();
                                if (TextUtils.equals(packageName, currentPackageName) && top.mUserId == userId) {
                                    targetStacks.add(stack);
                                }
                            }
                            stackNdx--;
                            defaultDisplay = defaultDisplay2;
                            mainStackCount = mainStackCount2;
                        }
                        tdaNdx--;
                        i = 1;
                    }
                    if (targetStacks.size() == 0) {
                        VSlog.e(TAG, "moveAppToDisplay, can not find stack of " + packageName + "," + userId);
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                    notifySkipHiddenPkg(packageName, userId);
                    decideMoveInputMethod();
                    this.mActivityTaskManagerService.deferWindowLayout();
                    boolean hasMovedMainStack = false;
                    for (int stackNdx2 = 0; stackNdx2 < targetStacks.size(); stackNdx2++) {
                        ActivityStack stack2 = targetStacks.get(stackNdx2);
                        if (hasMovedMainStack) {
                            stack2.finishAllActivitiesImmediately();
                        } else {
                            stack2.reparent(appShareDisplay.getDefaultTaskDisplayArea(), false);
                        }
                        if (!hasMovedMainStack && isMainStack(stack2)) {
                            hasMovedMainStack = true;
                        }
                    }
                    this.mActivityTaskManagerService.continueWindowLayout();
                    notifyAppSharePackageChanged(packageName, userId);
                    Binder.restoreCallingIdentity(ident);
                    return true;
                }
                VSlog.e(TAG, "moveAppToDisplay, " + packageName + " can not be moved to display " + displayId);
                Binder.restoreCallingIdentity(ident);
                return false;
            }
        }
        return false;
    }

    private void notifySkipHiddenPkg(String packageName, int userId) {
    }

    private void notifyAppSharePackageChanged(String packageName, int userId) {
        Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_NOTIFY_PACKAGE_CHANGED, userId, 0, packageName).sendToTarget();
    }

    private boolean hasMultiPackageLocked(ActivityStack stack) {
        Task tr = stack.getTopMostTask();
        ActivityRecord top = stack.getTopMostActivity();
        return (isGrantPermissionsLocked(top) || tr == null || tr.realActivity == null || top == null || top.mActivityComponent == null || tr.realActivity.flattenToShortString().contains(top.mActivityComponent.getPackageName())) ? false : true;
    }

    private boolean isGrantPermissionsLocked(ActivityRecord r) {
        return (r == null || r.mActivityComponent == null || !"com.android.permissioncontroller/.permission.ui.GrantPermissionsActivity".equals(r.mActivityComponent.flattenToShortString())) ? false : true;
    }

    private boolean isMultiWindowStack(ActivityStack stack) {
        return this.mWMService.isInVivoMultiWindowIgnoreVisibility() || stack.getWindowingMode() == 2 || stack.getWindowingMode() == 5;
    }

    private int getAppShareMainDisplayOrientationLocked(String packageName) {
        if (packageName == null) {
            return -1;
        }
        int numDisplays = getDisplayCount();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            DisplayContent display = getDisplayAt(displayNdx);
            if (display != null) {
                for (int tdaNdx = display.getTaskDisplayAreaCount() - 1; tdaNdx >= 0; tdaNdx--) {
                    TaskDisplayArea taskDisplayArea = display.getTaskDisplayAreaAt(tdaNdx);
                    for (int stackNdx = taskDisplayArea.getStackCount() - 1; stackNdx >= 0; stackNdx--) {
                        ActivityStack stack = taskDisplayArea.getStackAt(stackNdx);
                        if (stack != null && (display.getDefaultTaskDisplayArea().getRootHomeTask() == null || stack.getRootTaskId() != display.getDefaultTaskDisplayArea().getRootHomeTask().getRootTaskId())) {
                            ArrayList<ActivityRecord> activities = getAllActivities(stack);
                            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                                ActivityRecord r = activities.get(activityNdx);
                                if (r != null && !r.finishing && r.mActivityComponent != null && r.mActivityComponent.getPackageName().equals(packageName)) {
                                    int req = r.getOrientation();
                                    VSlog.d(TAG, "find package: " + packageName + ", in displayId: " + display.mDisplayId + ", orientation: " + req);
                                    return req;
                                }
                            }
                            continue;
                        }
                    }
                }
                continue;
            }
        }
        VSlog.e(TAG, "cannot find package: " + packageName + ", in all display");
        return -1;
    }

    public int getAppShareMainDisplayOrientation(String packageName) {
        int appShareMainDisplayOrientationLocked;
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            VSlog.e(TAG, "get display orientation not support : " + packageName);
            return -1;
        } else if (packageName == null) {
            VSlog.e(TAG, "get display orientation not support : " + packageName);
            return -1;
        } else {
            synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                appShareMainDisplayOrientationLocked = getAppShareMainDisplayOrientationLocked(packageName);
            }
            return appShareMainDisplayOrientationLocked;
        }
    }

    public int getActivityNumOnAppShareDisplay() {
        int activityNum;
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return 0;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                activityNum = 0;
                DisplayContent activityDisplay = getAppShareDisplay();
                if (activityDisplay != null) {
                    for (int tdaNdx = activityDisplay.getTaskDisplayAreaCount() - 1; tdaNdx >= 0; tdaNdx--) {
                        TaskDisplayArea taskDisplayArea = activityDisplay.getTaskDisplayAreaAt(tdaNdx);
                        for (int stackNdx = taskDisplayArea.getStackCount() - 1; stackNdx >= 0; stackNdx--) {
                            ActivityStack stack = taskDisplayArea.getStackAt(stackNdx);
                            if (stack != null) {
                                activityNum += numAllRunningActivitiesLocked(stack);
                            }
                        }
                    }
                }
            }
            return activityNum;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private int numAllRunningActivitiesLocked(ActivityStack stack) {
        final ArrayList<ActivityRecord> activityRecords = new ArrayList<>();
        stack.forAllActivities(new Consumer() { // from class: com.android.server.wm.-$$Lambda$VivoAppShareManager$ppNrHpu6n5FC9fNWVltEoH6pzIM
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                VivoAppShareManager.lambda$numAllRunningActivitiesLocked$3(activityRecords, (ActivityRecord) obj);
            }
        });
        return activityRecords.size();
    }

    public static /* synthetic */ void lambda$numAllRunningActivitiesLocked$3(ArrayList activityRecords, ActivityRecord r) {
        if (r != null && !r.finishing) {
            activityRecords.add(r);
        }
    }

    public ArrayList<String> getAppSharePackagesWithUserId() {
        long token = Binder.clearCallingIdentity();
        ArrayList<String> packages = new ArrayList<>();
        if (AppShareConfig.SUPPROT_APPSHARE) {
            try {
                synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                    if (!this.mIsAppResumed) {
                        VSlog.i(TAG, "getAppSharePackagesWithUserId, mIsAppResumed = " + this.mIsAppResumed);
                        return packages;
                    }
                    DisplayContent appShareDisplay = getAppShareDisplay();
                    if (appShareDisplay == null) {
                        VSlog.i(TAG, "getAppSharePackagesWithUserId, appShareDisplay is null");
                        return packages;
                    }
                    for (int tdaNdx = appShareDisplay.getTaskDisplayAreaCount() - 1; tdaNdx >= 0; tdaNdx--) {
                        TaskDisplayArea taskDisplayArea = appShareDisplay.getTaskDisplayAreaAt(tdaNdx);
                        for (int stackNdx = taskDisplayArea.getStackCount() - 1; stackNdx >= 0; stackNdx--) {
                            ActivityStack stack = taskDisplayArea.getStackAt(stackNdx);
                            ActivityRecord top = adjustTopActivityForStackLocked(stack, stack.topRunningActivityLocked());
                            String sharingPackageName = (top == null || top.mActivityComponent == null) ? null : top.mActivityComponent.getPackageName();
                            if (!TextUtils.isEmpty(sharingPackageName)) {
                                String sharingPackageNameWithUserId = sharingPackageName + "," + top.mUserId;
                                if (!packages.contains(sharingPackageNameWithUserId)) {
                                    packages.add(sharingPackageNameWithUserId);
                                }
                            }
                        }
                    }
                    int tdaNdx2 = packages.size();
                    if (tdaNdx2 > 0) {
                        packages.add(AppShareConfig.APP_SHARE_PKG_NAME + ",0");
                    }
                    VSlog.i(TAG, "getAppSharePackagesWithUserId, packages = " + packages);
                    return packages;
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return packages;
    }

    public void setAppShareController(IAppShareController controller) {
        IBinder binder;
        VSlog.i(TAG, "setAppShareController: controller = " + controller);
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (controller != null) {
                IBinder binder2 = controller.asBinder();
                if (binder2 != null) {
                    try {
                        binder2.linkToDeath(this.mDeathRecipient, 0);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } else if (this.mAppShareController != null && (binder = this.mAppShareController.asBinder()) != null) {
                binder.unlinkToDeath(this.mDeathRecipient, 0);
            }
            this.mAppShareController = controller;
        }
    }

    public void setIsAppResumed(boolean isAppResumed) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            VSlog.i(TAG, "setIsAppResumed: isAppResumed = " + isAppResumed + ", mIsAppResumed = " + this.mIsAppResumed);
            if (this.mIsAppResumed != isAppResumed) {
                this.mIsAppResumed = isAppResumed;
                if (!isAppResumed) {
                    appShareTaskSet("0f", getAppShareDisplayTopRunningActivityLocked());
                    this.mAppShareHandler.sendEmptyMessage(MSG_APP_SHARE_CREATE_SLEEP_TOKEN);
                } else {
                    this.mAppShareHandler.sendEmptyMessage(MSG_APP_SHARE_REMOVE_SLEEP_TOKEN);
                }
                notifyAppShareActivityStateChanged(isAppResumed);
            }
        }
    }

    private void notifyAppShareActivityStateChanged(boolean isAppResumed) {
        if (this.mDisplayManagerInternal == null) {
            this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        }
        DisplayManagerInternal displayManagerInternal = this.mDisplayManagerInternal;
        if (displayManagerInternal != null) {
            displayManagerInternal.notifyAppShareActivityStateChanged(isAppResumed);
        }
    }

    public String getAppShareDisplayTopActivity() {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                    DisplayContent activityDisplay = getAppShareDisplay();
                    if (activityDisplay == null) {
                        VSlog.e(TAG, "getAppShareDisplayTopActivity, app share display not exists.");
                        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    }
                    ActivityStack stack = activityDisplay.getTopStack();
                    if (stack == null) {
                        VSlog.e(TAG, "getAppShareDisplayTopActivity, app share display has no stacks.");
                        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    }
                    ActivityRecord r = stack.topRunningActivityLocked();
                    if (r != null && r.mActivityComponent != null) {
                        return r.mActivityComponent.flattenToShortString();
                    }
                    VSlog.e(TAG, "getAppShareDisplayTopActivity, app share display has no activities.");
                    return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public String getProcessInfo(String packageName, int userId) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                    if (TextUtils.isEmpty(packageName)) {
                        VSlog.e(TAG, "getProcessInfo: invalid packageName!");
                        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    } else if (userId == -1) {
                        VSlog.e(TAG, "getProcessInfo: invalid userId!");
                        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    } else {
                        SparseArray<WindowProcessController> pidMap = this.mActivityTaskManagerService.mProcessMap.getPidMap();
                        for (int i = pidMap.size() - 1; i >= 0; i--) {
                            int pid = pidMap.keyAt(i);
                            WindowProcessController app = pidMap.get(pid);
                            if (app != null && packageName.equals(app.mName) && userId == app.mUserId) {
                                return packageName + "," + userId + "," + app.mPid;
                            }
                        }
                        VSlog.e(TAG, "getProcessInfo: " + packageName + "," + userId + " do not exists!");
                        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public void setCameraExitReason(String reason) {
        VSlog.i(TAG, "setCameraExitReason: reason = " + reason + ", mCameraExitReason = " + this.mCameraExitReason);
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            this.mCameraExitReason = reason;
        }
    }

    public void updateAppShareConfig(int type) {
        VSlog.i(TAG, "updateAppShareConfig: type = " + type);
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        Message msg = Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_UPDATE_CONFIG, type, 0);
        msg.sendToTarget();
    }

    public void setAppShareModeFreezeTime(int time) {
        if (!AppShareConfig.SUPPROT_APPSHARE || time <= 0 || time > 10000) {
            return;
        }
        VSlog.d(TAG, "update freeze old: " + this.WINDOW_FREEZE_TIMEOUT_DURATION_APP_SHARED + ", time: " + time);
        this.WINDOW_FREEZE_TIMEOUT_DURATION_APP_SHARED = time;
    }

    public boolean decideFullScreenForAppShareLocked(ActivityRecord top, ActivityOptions options, boolean fullscreen) {
        int displayId;
        VivoAppShareManager vivoAppShareManager = this;
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return fullscreen;
        }
        if (!isAppsharedMode()) {
            return fullscreen;
        }
        if (fullscreen) {
            return fullscreen;
        }
        String str = vivoAppShareManager.mAppSharePackageName;
        if (str == null || !"com.tencent.tmgp.sgame".equals(str)) {
            return fullscreen;
        }
        if (top.mActivityComponent == null || !"com.tencent.mm.plugin.wallet_index.ui.OrderHandlerUI".equals(top.mActivityComponent.getClassName())) {
            return fullscreen;
        }
        int displayId2 = options != null ? options.getLaunchDisplayId() : top.getDisplayId();
        if (displayId2 == -1) {
            displayId2 = 0;
        }
        if (displayId2 != 0) {
            return fullscreen;
        }
        DisplayContent display = vivoAppShareManager.getDisplay(displayId2);
        if (display == null) {
            return fullscreen;
        }
        boolean findShareActivity = false;
        boolean hasOtherMainActivity = false;
        boolean end = false;
        int i = 1;
        int tdaNdx = display.getTaskDisplayAreaCount() - 1;
        while (tdaNdx >= 0) {
            TaskDisplayArea taskDisplayArea = display.getTaskDisplayAreaAt(tdaNdx);
            int stackNdx = taskDisplayArea.getStackCount() - i;
            while (true) {
                if (stackNdx < 0) {
                    displayId = displayId2;
                    break;
                }
                ActivityStack stack = taskDisplayArea.getStackAt(stackNdx);
                if (stack == null || stack.getChildCount() == 0) {
                    displayId = displayId2;
                } else {
                    ArrayList<ActivityRecord> activities = vivoAppShareManager.getAllActivities(stack);
                    int activityNdx = activities.size() - i;
                    while (true) {
                        if (activityNdx < 0) {
                            displayId = displayId2;
                            break;
                        }
                        ActivityRecord r = activities.get(activityNdx);
                        if (r == null || r.finishing) {
                            displayId = displayId2;
                        } else if (r.mActivityComponent == null) {
                            displayId = displayId2;
                        } else {
                            if (r.mActivityComponent.getPackageName().equals(AppShareConfig.APP_SHARE_PKG_NAME)) {
                                displayId = displayId2;
                                if (r.mActivityComponent.getClassName().equals("com.vivo.appshare.ShareDisplayActivity")) {
                                    findShareActivity = true;
                                    VSlog.d(TAG, "decideFullScreenForAppShareLocked find findShareActivity");
                                    end = true;
                                    break;
                                }
                            } else {
                                displayId = displayId2;
                            }
                            if (ActivityRecord.isMainIntent(r.intent)) {
                                VSlog.d(TAG, "decideFullScreenForAppShareLocked find hasOtherMainActivity: " + r + ", intent: " + r.intent);
                                hasOtherMainActivity = true;
                                end = true;
                                break;
                            } else if (r != top && !"com.tencent.tmgp.sgame".equals(r.mActivityComponent.getPackageName()) && !Constant.APP_WEIXIN.equals(r.mActivityComponent.getPackageName())) {
                                VSlog.d(TAG, "decideFullScreenForAppShareLocked find trd activity hasOtherMainActivity: " + r);
                                return fullscreen;
                            }
                        }
                        activityNdx--;
                        displayId2 = displayId;
                    }
                    if (end) {
                        break;
                    }
                }
                stackNdx--;
                i = 1;
                vivoAppShareManager = this;
                displayId2 = displayId;
            }
            if (end) {
                break;
            }
            tdaNdx--;
            i = 1;
            vivoAppShareManager = this;
            displayId2 = displayId;
        }
        return findShareActivity && !hasOtherMainActivity;
    }

    public void updateOverrideConfigurationLockedForAppShare(ActivityRecord r, Configuration mergedConfiguration) {
        int displayId;
        if (!AppShareConfig.SUPPROT_APPSHARE || !isAppsharedMode() || r == null || mergedConfiguration == null || (displayId = r.getDisplayId()) < 0 || !isAppsharedDisplayId(displayId)) {
            return;
        }
        Configuration overrideConfig = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayOverrideConfiguration(displayId);
        mergedConfiguration.updateFrom(overrideConfig);
    }

    public boolean needInterceptAudioRecord() {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                if (isAppShareDisplayExist()) {
                    int callingPid = Binder.getCallingPid();
                    int callingUid = Binder.getCallingUid();
                    String packageName = this.mContext.getPackageManager().getNameForUid(callingUid);
                    VSlog.i(TAG, "needInterceptAudioRecord: callingPid = " + callingPid + ", callingUid = " + callingUid + ", packageName = " + packageName);
                    if ("com.tencent.tmgp.sgame".equals(packageName) && isOnAppShareDisplay(packageName, UserHandle.getUserId(callingUid)) && isControlledByRemote()) {
                        Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_FORBID_USE_HARDWARE_BY_BROADCAST, 2, 0).sendToTarget();
                        return true;
                    }
                    return false;
                }
                return false;
            }
        }
        return false;
    }

    public boolean shouldBlockRequestedOrientationLocked(ActivityRecord r, int requestedOrientation) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            int lastRequestedOrientation = r.getOrientation();
            if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                VSlog.d(TAG, "shouldBlockRequestedOrientationLocked: r = " + r + ", requestedOrientation = " + requestedOrientation + ", lastRequestedOrientation = " + lastRequestedOrientation + ", displayId = " + r.getDisplayId());
            }
            if (isAppShareDisplayExist()) {
                String packageName = r.mActivityComponent != null ? r.mActivityComponent.getPackageName() : null;
                if (isAppShareForeground() && isOnAppShareDisplay(packageName, r.mUserId) && isAppSharedRotateFreeze()) {
                    updateAppSharedRotateFreeze(false);
                    updateAppSharedRequestedOrientation(false);
                    VSlog.d(TAG, "setRequested orientation: " + requestedOrientation + ", for activity: " + r + ", then reset freeze status.");
                }
                if (AppShareConfig.getInstance().isBlockRequestedOrientationBlackList(packageName) && isOnAppShareDisplay(packageName, r.mUserId) && ((isControlledByRemote() || !isAppShareForeground()) && ((lastRequestedOrientation == 0 && requestedOrientation == 8) || (lastRequestedOrientation == 8 && requestedOrientation == 0)))) {
                    VSlog.i(TAG, "block r = " + r + ", requestedOrientation = " + requestedOrientation);
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    public Task changeTaskForAppShareIfNeededLocked(Task task) {
        Task appShareTask;
        if (AppShareConfig.SUPPROT_APPSHARE) {
            ActivityStack stack = task.getStack();
            boolean useAppShareTask = false;
            if (stack != null && MultiDisplayManager.isAppShareDisplayId(stack.getDisplayId())) {
                useAppShareTask = true;
            } else if (isAppShareDisplayExist() && stack == null && task.realActivity != null && isOnAppShareDisplay(task.realActivity.getPackageName(), task.mUserId) && AppShareConfig.getInstance().isStartInAppShareDisplayBlackList(task.realActivity.flattenToShortString())) {
                useAppShareTask = true;
            }
            if (!useAppShareTask || (appShareTask = findAppShareTaskByDisplayIdLocked(0)) == null) {
                return null;
            }
            VSlog.i(TAG, "changeTaskForAppShareIfNeededLocked: change task from " + task + " to " + appShareTask);
            return appShareTask;
        }
        return null;
    }

    private Task findAppShareTaskByDisplayIdLocked(int displayId) {
        DisplayContent display;
        if (AppShareConfig.SUPPROT_APPSHARE && (display = getDisplay(displayId)) != null) {
            for (int tdaNdx = display.getTaskDisplayAreaCount() - 1; tdaNdx >= 0; tdaNdx--) {
                TaskDisplayArea taskDisplayArea = display.getTaskDisplayAreaAt(tdaNdx);
                for (int stackNdx = taskDisplayArea.getStackCount() - 1; stackNdx >= 0; stackNdx--) {
                    ActivityStack stack = taskDisplayArea.getStackAt(stackNdx);
                    if (stack != null && stack.getActivityType() != 4 && stack.getActivityType() != 2 && stack.getActivityType() != 3) {
                        ArrayList<Task> tasks = getAllTasks(stack);
                        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                            Task task = tasks.get(taskNdx);
                            if (task.realActivity != null && AppShareConfig.APP_SHARE_ACTIVITY_SHORT_STRING.equals(task.realActivity.flattenToShortString())) {
                                return task;
                            }
                        }
                        continue;
                    }
                }
            }
            return null;
        }
        return null;
    }

    public boolean shouldBlockVivoCameraFinishLocked(ActivityRecord r) {
        String str;
        int i;
        if (AppShareConfig.SUPPROT_APPSHARE) {
            if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                VSlog.i(TAG, "shouldBlockVivoCameraFinishLocked: r = " + r + ", mCameraExitReason = " + this.mCameraExitReason);
            }
            if (MultiDisplayManager.isAppShareDisplayId(r.getDisplayId()) && r.mActivityComponent != null && (str = this.mAppSharePackageName) != null && str.equals(r.mActivityComponent.getPackageName()) && (i = this.mAppShareUserId) != -1 && i == r.mUserId && "com.android.camera/.CameraActivity".equals(r.mActivityComponent.flattenToShortString())) {
                String reason = this.mCameraExitReason;
                this.mCameraExitReason = null;
                if ("onBackPressed".equals(reason)) {
                    this.mAppShareHandler.sendEmptyMessage(MSG_APP_SHARE_MOVE_TASK_TO_BACK);
                    VSlog.i(TAG, "finishActivity, block vivo camera finish because it is sharing!");
                    return true;
                }
                this.mAppShareHandler.sendEmptyMessage(MSG_APP_SHARE_FINISH_SHARING);
            }
            return false;
        }
        return false;
    }

    public void notifyAppShareLockScreenShown(boolean shown) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_SET_LOCK_SCREEN_SHOWN, Boolean.valueOf(shown)).sendToTarget();
    }

    public boolean shouldBlockEnterPictureInPictureModeLocked(ActivityRecord r) {
        if (r != null && MultiDisplayManager.isAppShareDisplayId(r.getDisplayId())) {
            VSlog.i(TAG, "enterPictureInPictureMode failed, app share can't enter pip!");
            this.mAppShareHandler.obtainMessage(MSG_APP_SHARE_FORBID_ENTER_MULTIWINDOW_MODE, true).sendToTarget();
            return true;
        }
        return false;
    }

    public void startAppShareForShortcutIfNeeded(String packageName, int userId) {
        if (AppShareConfig.SUPPROT_APPSHARE && isOnAppShareDisplay(packageName, userId)) {
            Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_START_ACTIVITY_DELAYED, 0L).sendToTarget();
        }
    }

    public void setResumedActivityForAppShareLocked(ActivityRecord r) {
        if (!AppShareConfig.SUPPROT_APPSHARE || r == null) {
            return;
        }
        if (r.getDisplayId() == 0) {
            notifyAppFinishResumed(r.packageName, r.mUserId);
            updateAppShareForegroundStateLocked(isAppShareForeground());
        } else if (MultiDisplayManager.isAppShareDisplayId(r.getDisplayId())) {
            if (isAppShareForeground()) {
                appShareTaskSet("ff", getAppShareDisplayTopRunningActivityLocked());
            } else {
                appShareTaskSetLimit();
            }
            String packageName = r.mActivityComponent != null ? r.mActivityComponent.getPackageName() : null;
            updateAppShareStateForInputManager(packageName);
        }
    }

    private void updateAppShareStateForInputManager(String packageName) {
        if (!PKG_VIDEO_PLAYER.equals(packageName)) {
            return;
        }
        Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_NOTIFY_INPUT_MANAGER_STATE, packageName).sendToTarget();
    }

    public void updateConfigurationForInputMethod(int displayId, boolean force) {
        if (AppShareConfig.SUPPROT_APPSHARE && !isAppsharedMode()) {
        }
    }

    public void adjustAppShareActivityInfoWhenStartLocked(int callingPid, int callingUid, String callingPackage, Intent intent, ActivityInfo aInfo) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("start activity calling pkg: ");
        sb.append(callingPackage);
        sb.append(", intent: ");
        sb.append(intent);
        sb.append(", ainfo: ");
        sb.append(aInfo != null ? Integer.valueOf(aInfo.screenOrientation) : "none");
        VSlog.d(TAG, sb.toString());
        if (callingPackage == null || intent == null || aInfo == null || this.mAppSharePackageName == null || intent.getComponent() == null || !intent.getComponent().getPackageName().equals(AppShareConfig.APP_SHARE_PKG_NAME) || !intent.getComponent().getClassName().equals("com.vivo.appshare.ShareDisplayActivity")) {
            return;
        }
        int req = getAppShareMainDisplayOrientationLocked(this.mAppSharePackageName);
        VSlog.d(TAG, "start activity get launch pkg: " + this.mAppSharePackageName + ", req: " + req);
        if (!isOrientationDefinite(req)) {
            return;
        }
        boolean landscape = isLandscapeOrientation(req);
        aInfo.screenOrientation = !landscape;
    }

    public void decideKeepAppShareRotationLocked(int callingPid, int callingUid, String callingPackage, Intent intent, ActivityInfo aInfo, ActivityOptions options) {
        if (!AppShareConfig.SUPPROT_APPSHARE || options == null) {
            return;
        }
        int launchDisplayId = options.getLaunchDisplayId();
        if (launchDisplayId != MSG_APP_SHARE_FORBID_USE_HARDWARE || intent == null || intent.getComponent() == null || aInfo == null) {
            return;
        }
        int req = aInfo.screenOrientation;
        VSlog.d(TAG, "decide keep rotation intent: " + intent + ", req: " + req);
        if (isOrientationDefinite(req)) {
            updateAppShareKeepRoation(false);
        } else if (isSensorOrientation(req)) {
            this.mAppShareHandler.removeMessages(MSG_APP_SHARE_NOTIFY_KEEP_ROTATION);
            updateAppShareKeepRoation(true);
            sendMyMessage(this.mAppShareHandler, MSG_APP_SHARE_NOTIFY_KEEP_ROTATION, 0, 0, 600, null);
        } else {
            updateAppShareKeepRoation(false);
        }
    }

    private boolean isSensorOrientation(int req) {
        return req == -1 || req == 4 || req == 10;
    }

    public void updateConfigurationAppShareLocked(WindowProcessController app, Configuration configuration) {
        if (!AppShareConfig.SUPPROT_APPSHARE || app == null || configuration == null || this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent((int) MSG_APP_SHARE_FORBID_USE_HARDWARE) == null) {
            return;
        }
        VSlog.d(TAG, "update configuration pid: " + app.mPid);
        if (this.mVivoRatioControllerUtilsImpl.isRunningInputMethodAll(app.mPid)) {
            int imeDisplayId = getInputMethodDstDisplayId();
            VSlog.d(TAG, "imeDisplayId : " + imeDisplayId);
            if (imeDisplayId != MSG_APP_SHARE_FORBID_USE_HARDWARE) {
                return;
            }
            Configuration overrideConfig = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayOverrideConfiguration(imeDisplayId);
            Configuration globalConfig = this.mActivityTaskManagerService.getGlobalConfiguration();
            VSlog.d(TAG, "updateConfigurationAppShareLocked globalConfig: " + globalConfig + ", override: " + overrideConfig);
            configuration.updateFrom(globalConfig);
            configuration.updateFrom(overrideConfig);
            VSlog.d(TAG, "updateConfigurationAppShareLocked final configuration: " + configuration);
        }
    }

    public void adjustConfigurationForAppShareMode(WindowProcessController app, Configuration configCopy, Configuration mTempConfig) {
        if (!AppShareConfig.SUPPROT_APPSHARE || app == null || !isAppShareDisplayExist()) {
            return;
        }
        boolean assigned = false;
        if (this.mVivoRatioControllerUtilsImpl.isRunningInputMethodAll(app.mPid)) {
            int imeDisplayId = getInputMethodDstDisplayId();
            VSlog.d(TAG, "imeDisplayId : " + imeDisplayId);
            if (imeDisplayId != MSG_APP_SHARE_FORBID_USE_HARDWARE) {
                return;
            }
            Configuration overrideConfig = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayOverrideConfiguration(imeDisplayId);
            configCopy.setTo(mTempConfig);
            configCopy.updateFrom(overrideConfig);
            assigned = true;
        }
        if (assigned || app.mActivities.isEmpty()) {
            return;
        }
        for (int k = 0; k < app.mActivities.size(); k++) {
            ActivityRecord activityRecord = (ActivityRecord) app.mActivities.get(k);
            if (activityRecord != null) {
                int displayId = activityRecord.getDisplayId();
                if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                    VSlog.d(TAG, "app share app displayId: " + displayId + ", app: " + app.mName + ", activity: " + activityRecord);
                }
                if (displayId != MSG_APP_SHARE_FORBID_USE_HARDWARE) {
                    return;
                }
                Configuration overrideConfig2 = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayOverrideConfiguration(activityRecord.getDisplayId());
                configCopy.setTo(mTempConfig);
                configCopy.updateFrom(overrideConfig2);
                VSlog.d(TAG, "app share app: " + app.mName + ", pid: " + app.mPid + ", configuration: " + configCopy);
            }
        }
    }

    public void updateDisplayOverrideConfigurationForAppShareLocked(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE || !MultiDisplayManager.isAppShareDisplayId(displayId)) {
            return;
        }
        Configuration overrideConfig = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayOverrideConfiguration(displayId);
        Configuration mTmpConfig = new Configuration();
        Configuration globalConfig = this.mActivityTaskManagerService.getGlobalConfiguration();
        mTmpConfig.setTo(globalConfig);
        mTmpConfig.updateFrom(overrideConfig);
        mTmpConfig.seq = 0;
        updateConfigurationForAppInShareDisplay(mTmpConfig, displayId);
        notifyAppSharedRequestOrientation();
    }

    private void notifyAppSharedRequestOrientation() {
        int requestedOrientation;
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        ActivityRecord r = getAppShareRunningActivity();
        if (r != null) {
            requestedOrientation = r.getOrientation();
        } else {
            requestedOrientation = getDefinityRequestedOrientation(MSG_APP_SHARE_FORBID_USE_HARDWARE);
        }
        int finalOrientation = convertRequestOrientation(MSG_APP_SHARE_FORBID_USE_HARDWARE, requestedOrientation);
        if (finalOrientation == 6) {
            finalOrientation = 0;
        }
        VSlog.d(TAG, "notifyAppSharedRequestOrientation finalOrientation : " + finalOrientation + ", requestedOrientation : " + requestedOrientation + ", r : " + r);
        Message msg = this.mAppShareHandler.obtainMessage(MSG_APP_SHARE_REQUEST_ORIENTATION);
        msg.arg1 = finalOrientation;
        this.mAppShareHandler.sendMessage(msg);
    }

    private ActivityRecord getAppShareRunningActivity() {
        ActivityStack otherStack;
        DisplayContent otherDisplay = getAppShareDisplay();
        if (otherDisplay == null || (otherStack = otherDisplay.getTopStack()) == null) {
            return null;
        }
        return otherStack.topRunningActivityLocked();
    }

    private int getDefinityRequestedOrientation(int displayId) {
        DisplayContent displayContent = this.mWMService.mRoot.getDisplayContent(displayId);
        if (displayContent == null) {
            return -1;
        }
        return getRequestedOrientation(displayContent.getLastOrientation(), displayContent.getRotation());
    }

    public boolean skipFreezeActivity(ActivityRecord r) {
        if (!AppShareConfig.SUPPROT_APPSHARE || r == null || this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent((int) MSG_APP_SHARE_FORBID_USE_HARDWARE) == null) {
            return false;
        }
        if (isAppShareForeground() || isShareAppInPipMode()) {
            return !this.mWMService.mDisplayFrozen;
        }
        return false;
    }

    public void setRequestedOrientationAppShared(ActivityRecord r, int requestedOrientation) {
        if (!AppShareConfig.SUPPROT_APPSHARE || r == null || !isAppsharedMode()) {
            return;
        }
        restartAppShareContainerIfNeededLocked(r);
        int displayId = r.getDisplayId();
        if (!isAppsharedDisplayId(displayId)) {
            return;
        }
        DisplayContent display = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent(displayId);
        ActivityStack topStack = display.getTopStack();
        if (topStack == null) {
            return;
        }
        ActivityRecord topR = topStack.topRunningActivityLocked();
        if (topR == null || topR != r) {
            VSlog.d(TAG, "setRequestedOrientationAppShared r: " + r + ", running r: " + topR);
            return;
        }
        String packageName = r.mActivityComponent != null ? r.mActivityComponent.getPackageName() : null;
        updateAppShareStateForInputManager(packageName);
        int finalOrientation = convertRequestOrientation(displayId, requestedOrientation);
        if (finalOrientation == 6) {
            finalOrientation = 0;
        }
        Message msg = this.mAppShareHandler.obtainMessage(MSG_APP_SHARE_REQUEST_ORIENTATION);
        msg.arg1 = finalOrientation;
        this.mAppShareHandler.sendMessage(msg);
    }

    private void restartAppShareContainerIfNeededLocked(ActivityRecord r) {
        VSlog.i(TAG, "restartAppShareContainerIfNeededLocked: r = " + r);
        if (!IS_MTK || !isAppShareContainerLocked(r)) {
            return;
        }
        ActivityStack stack = r.getStack();
        if (stack == null) {
            VSlog.e(TAG, "restartAppShareContainerIfNeededLocked: stack is null!");
            return;
        }
        stack.startPausingLocked(false, false, (ActivityRecord) null);
        stack.goToSleep();
    }

    private int convertRequestOrientation(int displayId, int requestedOrientation) {
        if (isOrientationDefinite(requestedOrientation)) {
            return requestedOrientation;
        }
        DisplayContent displayContent = this.mWMService.mRoot.getDisplayContent(displayId);
        if (displayContent == null) {
            return requestedOrientation;
        }
        return getRequestedOrientation(displayContent.getLastOrientation(), displayContent.getRotation());
    }

    private boolean isOrientationDefinite(int orientation) {
        return orientation == 0 || orientation == 6 || orientation == 8 || orientation == 11 || orientation == 1 || orientation == 7 || orientation == 9 || orientation == 12;
    }

    private int getRequestedOrientation(int orientation, int rotation) {
        if (isOrientationDefinite(orientation)) {
            return orientation;
        }
        return OrientationByRotation(rotation);
    }

    private int OrientationByRotation(int rotation) {
        if (rotation != 1 && rotation != 3) {
            return 1;
        }
        return 0;
    }

    private boolean isLandscapeOrientation(int orientation) {
        return orientation == 0 || orientation == 6 || orientation == 8 || orientation == 11;
    }

    public boolean shouldBlockedByAppShareLocked(Task task) {
        Task appShareTask;
        if (AppShareConfig.SUPPROT_APPSHARE) {
            if (isAppShareDisplayExist() && task.realActivity != null && isOnAppShareDisplay(task.realActivity.getPackageName(), task.mUserId) && AppShareConfig.getInstance().isStartInAppShareDisplayBlackList(task.realActivity.flattenToShortString())) {
                ActivityStack appShareStack = findAppShareStackLocked();
                if (task.getStack() == null || (appShareStack != null && task.getRootTaskId() != appShareStack.getRootTaskId())) {
                    DisplayContent appShareDisplay = getAppShareDisplay();
                    ActivityStack topStack = appShareDisplay.getTopStack();
                    if (topStack != null && (appShareTask = topStack.getTopMostTask()) != null) {
                        VSlog.i(TAG, "shouldBlockedByAppShareLocked: change task from " + task + " to " + appShareTask);
                        if (task.getStack() != null && !task.getStack().hasActivity()) {
                            getDefaultDisplay().getDisplayArea().removeChild(task.getStack());
                        }
                        task = appShareTask;
                    }
                }
            }
            if (task.getStack() == null || !MultiDisplayManager.isAppShareDisplayId(task.getStack().getDisplayId())) {
                return false;
            }
            boolean inSplitScreenMode = this.mWMService.isInVivoMultiWindowIgnoreVisibility();
            if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                VSlog.i(TAG, "shouldBlockedByAppShareLocked, mService.mAppShareController = " + this.mAppShareController + ", inSplitScreenMode = " + inSplitScreenMode);
            }
            if (inSplitScreenMode) {
                this.mAppShareHandler.obtainMessage(MSG_APP_SHARE_FORBID_ENTER_MULTIWINDOW_MODE, false).sendToTarget();
                return true;
            }
            this.mAppShareHandler.obtainMessage(MSG_APP_SHARE_START_ACTIVITY_DELAYED, 0L).sendToTarget();
            return true;
        }
        return false;
    }

    private ActivityStack findAppShareStackLocked() {
        DisplayContent defaultDisplay = getDefaultDisplay();
        for (int tdaNdx = defaultDisplay.getTaskDisplayAreaCount() - 1; tdaNdx >= 0; tdaNdx--) {
            TaskDisplayArea taskDisplayArea = defaultDisplay.getTaskDisplayAreaAt(tdaNdx);
            for (int stackNdx = taskDisplayArea.getStackCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = taskDisplayArea.getStackAt(stackNdx);
                WindowConfiguration windowConfiguration = stack.getWindowConfiguration();
                if (windowConfiguration.getActivityType() == 1 && windowConfiguration.getWindowingMode() == 1) {
                    ArrayList<ActivityRecord> activities = getAllActivities(stack);
                    for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                        ActivityRecord r = activities.get(activityNdx);
                        if (!r.finishing && r.mActivityComponent != null && AppShareConfig.APP_SHARE_ACTIVITY_SHORT_STRING.equals(r.mActivityComponent.flattenToShortString())) {
                            return r.getStack();
                        }
                    }
                    continue;
                }
            }
        }
        return null;
    }

    public void notifyAppShareDisplayRemovedLocked(DisplayContent display) {
        if (MultiDisplayManager.isAppShareDisplayId(display.getDisplayId())) {
            notifyAppSharePackageChanged(null, -1);
            removeEmptyStackIfNeededLocked();
        }
    }

    private void removeEmptyStackIfNeededLocked() {
        DisplayContent defaultDisplay = getDefaultDisplay();
        ArrayList<ActivityStack> pendingRemoveStacks = new ArrayList<>();
        for (int tdaNdx = defaultDisplay.getTaskDisplayAreaCount() - 1; tdaNdx >= 0; tdaNdx--) {
            TaskDisplayArea taskDisplayArea = defaultDisplay.getTaskDisplayAreaAt(tdaNdx);
            for (int stackNdx = taskDisplayArea.getStackCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = taskDisplayArea.getStackAt(stackNdx);
                if (!stack.hasActivity() && stack.getActivityType() == 1 && stack.getWindowingMode() == 1) {
                    pendingRemoveStacks.add(stack);
                }
            }
            for (int stackNdx2 = 0; stackNdx2 < pendingRemoveStacks.size(); stackNdx2++) {
                ActivityStack stack2 = pendingRemoveStacks.get(stackNdx2);
                taskDisplayArea.removeChild(stack2);
                VSlog.i(TAG, "removeEmptyStack: stackNdx = " + stackNdx2 + ", stack = " + stack2);
            }
        }
        pendingRemoveStacks.clear();
    }

    public void moveAllStacksToDefaultDisplayForAppShareLocked(final TaskDisplayArea currentDisplay, final TaskDisplayArea toDisplay) {
        if (MultiDisplayManager.isAppShareDisplayId(currentDisplay.getDisplayId())) {
            final boolean isAppShareForeground = isAppShareForeground();
            this.mWMService.inSurfaceTransaction(new Runnable() { // from class: com.android.server.wm.VivoAppShareManager.3
                {
                    VivoAppShareManager.this = this;
                }

                @Override // java.lang.Runnable
                public void run() {
                    while (currentDisplay.getChildCount() > 0) {
                        ActivityStack stack = currentDisplay.getStackAt(0);
                        stack.reparent(toDisplay, isAppShareForeground);
                        VivoAppShareManager.this.pauseStackIfNeededLocked(stack, isAppShareForeground);
                    }
                }
            });
            updateAppShareForegroundStateLocked(false);
        }
    }

    public void pauseStackIfNeededLocked(ActivityStack stack, boolean onTop) {
        VSlog.i(TAG, "pauseStackIfNeededLocked: stackId = " + stack.getRootTaskId() + ", onTop = " + onTop);
        if (onTop) {
            boolean isKeyguardLocked = this.mWMService.isKeyguardLocked();
            VSlog.i(TAG, "pauseStackIfNeededLocked: isKeyguardLocked = " + isKeyguardLocked);
            if (isKeyguardLocked) {
                stack.startPausingLocked(false, false, (ActivityRecord) null);
                return;
            }
            return;
        }
        stack.startPausingLocked(false, false, (ActivityRecord) null);
    }

    private void updateAppShareForegroundStateLocked(boolean foreground) {
        if (AppShareConfig.SUPPROT_APPSHARE && this.mIsAppShareForeground != foreground) {
            Settings.System.putInt(this.mContext.getContentResolver(), "app_share_container_state", foreground ? 1 : 0);
            this.mIsAppShareForeground = foreground;
            if (foreground) {
                appShareTaskSet("ff", getAppShareDisplayTopRunningActivityLocked());
            } else {
                appShareTaskSetLimit();
            }
        }
    }

    public boolean isAppShareForeground() {
        boolean isAppShareForegroundLocked;
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return false;
        }
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            isAppShareForegroundLocked = isAppShareForegroundLocked();
        }
        return isAppShareForegroundLocked;
    }

    boolean isShareAppInPipMode() {
        boolean isShareAppInPipModeLocked;
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return false;
        }
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            isShareAppInPipModeLocked = isShareAppInPipModeLocked();
        }
        return isShareAppInPipModeLocked;
    }

    private boolean isShareAppInPipModeLocked() {
        DisplayContent defaultDisplay;
        if (AppShareConfig.SUPPROT_APPSHARE && !isAppSharedRemoving() && isAppShareDisplayExist() && (defaultDisplay = getDefaultDisplay()) != null) {
            boolean hasSplitScreenPrimaryStack = defaultDisplay.getDefaultTaskDisplayArea().isSplitScreenModeActivated();
            if (hasSplitScreenPrimaryStack) {
                if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                    VSlog.i(TAG, "isShareAppInPipModeLocked: hasSplitScreenPrimaryStack");
                }
                return false;
            }
            for (int tdaNdx = defaultDisplay.getTaskDisplayAreaCount() - 1; tdaNdx >= 0; tdaNdx--) {
                TaskDisplayArea taskDisplayArea = defaultDisplay.getTaskDisplayAreaAt(tdaNdx);
                for (int stackNdx = taskDisplayArea.getStackCount() - 1; stackNdx >= 0; stackNdx--) {
                    ActivityStack stack = taskDisplayArea.getStackAt(stackNdx);
                    if (stack != null) {
                        if (stack.getWindowingMode() != 2) {
                            VSlog.i(TAG, "isShareAppInPipModeLocked: skip stack not in pinned mode");
                        } else {
                            ArrayList<ActivityRecord> activities = getAllActivities(stack);
                            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                                ActivityRecord r = activities.get(activityNdx);
                                if (r != null && !r.finishing && r.mActivityComponent != null && isAppShareContainerLocked(r)) {
                                    return true;
                                }
                            }
                            continue;
                        }
                    }
                }
            }
            return false;
        }
        return false;
    }

    private boolean isAppShareForegroundLocked() {
        if (AppShareConfig.SUPPROT_APPSHARE && isAppShareDisplayExist()) {
            DisplayContent defaultDisplay = getDefaultDisplay();
            boolean hasSplitScreenPrimaryStack = defaultDisplay.getDefaultTaskDisplayArea().isSplitScreenModeActivated();
            if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                VSlog.i(TAG, "isAppShareForeground: hasSplitScreenPrimaryStack = " + hasSplitScreenPrimaryStack);
            }
            if (hasSplitScreenPrimaryStack) {
                return false;
            }
            ActivityStack targetStack = null;
            ActivityRecord targetActivity = null;
            for (int tdaNdx = defaultDisplay.getTaskDisplayAreaCount() - 1; tdaNdx >= 0; tdaNdx--) {
                TaskDisplayArea taskDisplayArea = defaultDisplay.getTaskDisplayAreaAt(tdaNdx);
                int stackNdx = taskDisplayArea.getStackCount() - 1;
                while (true) {
                    if (stackNdx >= 0) {
                        ActivityStack stack = taskDisplayArea.getStackAt(stackNdx);
                        if (stack.getWindowingMode() != 5 && stack.getWindowingMode() != 2) {
                            ActivityRecord topRunningActivity = stack.topRunningActivityLocked();
                            if (topRunningActivity == null) {
                                VSlog.i(TAG, "isAppShareForeground: topRunningActivity is null.");
                            } else {
                                targetStack = stack;
                                targetActivity = topRunningActivity;
                                break;
                            }
                        } else {
                            VSlog.i(TAG, "isAppShareForeground: skip ActivityStack " + WindowConfiguration.windowingModeToString(stack.getWindowingMode()));
                        }
                        stackNdx--;
                    }
                }
            }
            boolean isAppShareForeground = isAppShareContainerLocked(targetActivity);
            if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                VSlog.i(TAG, "isAppShareForeground: targetStack = " + targetStack + ", targetActivity = " + targetActivity + ", isAppShareForeground = " + isAppShareForeground);
            }
            return isAppShareForeground;
        }
        return false;
    }

    private boolean isAppShareContainerLocked(ActivityRecord r) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                VSlog.d(TAG, "isAppShareContainerLocked, r = " + r);
            }
            return (r == null || r.mActivityComponent == null || !AppShareConfig.APP_SHARE_ACTIVITY_SHORT_STRING.equals(r.mActivityComponent.flattenToShortString())) ? false : true;
        }
        return false;
    }

    public void notifyAppShareAboutFreeFormMode(int displayId, int windowingMode) {
        if (displayId == 0 && windowingMode == 5) {
            notifyAppShareAboutFreeFormMode(true);
        }
    }

    private void notifyAppShareAboutFreeFormMode(boolean enter) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_NOTIFY_ABOUT_FREE_FORM_MODE, enter ? 1 : 0, 0).sendToTarget();
    }

    public void notifyAppShareIfNeededLocked(ActivityStack stack, int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
            VSlog.d(TAG, "notifyAppShareIfNeededLocked, displayId = " + displayId + ", windowingMode = " + stack.getWindowingMode());
        }
        if (displayId == 0) {
            int windowingMode = stack.getWindowingMode();
            if (windowingMode == 2) {
                notifyAppShareAboutPipMode(false);
            } else if (windowingMode == 5) {
                notifyAppShareAboutFreeFormMode(false);
            }
        }
    }

    public void notifyAppShareAboutPipMode(boolean enter) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_NOTIFY_ABOUT_PIP_MODE, enter ? 1 : 0, 0).sendToTarget();
    }

    private DisplayContent getDisplay(int displayId) {
        if (displayId == 0) {
            return getDefaultDisplay();
        }
        DisplayContent display = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent(displayId);
        if (display != null && display.getDisplayId() == displayId) {
            return display;
        }
        return null;
    }

    private DisplayContent getDefaultDisplay() {
        return this.mActivityTaskManagerService.mRootWindowContainer.getDefaultDisplay();
    }

    public DisplayContent getAppShareDisplay() {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            return getDisplay(MSG_APP_SHARE_FORBID_USE_HARDWARE);
        }
        return null;
    }

    public boolean isAppShareDisplayExist() {
        return getAppShareDisplay() != null;
    }

    private int getDisplayCount() {
        return this.mActivityTaskManagerService.mRootWindowContainer.getChildCount();
    }

    private DisplayContent getDisplayAt(int displayNdx) {
        return this.mActivityTaskManagerService.mRootWindowContainer.getChildAt(displayNdx);
    }

    public boolean isMainStack(ActivityStack stack) {
        ArrayList<Task> tasks = getAllTasks(stack);
        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
            Task task = tasks.get(taskNdx);
            if (task.intent != null && task.intent.getCategories() != null && "android.intent.action.MAIN".equals(task.intent.getAction()) && task.intent.getCategories().contains("android.intent.category.LAUNCHER")) {
                return true;
            }
        }
        return false;
    }

    private ActivityRecord getAppShareDisplayTopRunningActivityLocked() {
        ActivityStack focusedStack;
        DisplayContent appShareDisplay = getAppShareDisplay();
        if (appShareDisplay == null || (focusedStack = appShareDisplay.getFocusedStack()) == null) {
            return null;
        }
        return focusedStack.topRunningActivityLocked();
    }

    public boolean shouldBlockedByAppShareLocked(ActivityRecord sourceRecord, String callingPackage, int realCallingPid, int realCallingUid, ActivityInfo aInfo, Intent intent) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return false;
        }
        String flattenToShortString = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        String targetPackage = (aInfo == null || aInfo.applicationInfo == null) ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : aInfo.applicationInfo.packageName;
        if (intent != null && intent.getComponent() != null) {
            flattenToShortString = intent.getComponent().flattenToShortString();
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
            VSlog.i(TAG, "shouldBlockedByAppShareLocked, mAppShareController = " + this.mAppShareController + ", sourceRecord = " + sourceRecord + ", callingPackage = " + callingPackage + ", realCallingPid = " + realCallingPid + ", realCallingUid = " + realCallingUid + ", targetPackage = " + targetPackage + ", flattenToShortString = " + flattenToShortString);
        }
        boolean isAppShareDisplayExist = isAppShareDisplayExist();
        boolean shouldBlock = false;
        if (isAppShareDisplayExist && !TextUtils.isEmpty(callingPackage) && !TextUtils.isEmpty(targetPackage) && isOnAppShareDisplay(callingPackage, UserHandle.getUserId(realCallingUid)) && this.mIsControlledByRemote && ((!TextUtils.equals(callingPackage, targetPackage) && !AppShareConfig.getInstance().isForceStartToAppShareDisplayWhiteList(flattenToShortString)) || AppShareConfig.getInstance().isStartInAppShareDisplayBlackList(flattenToShortString))) {
            shouldBlock = true;
        }
        if (isAppShareDisplayExist && !TextUtils.isEmpty(callingPackage) && !TextUtils.isEmpty(targetPackage) && sourceRecord == null && this.mIsControlledByRemote && this.mVivoRatioControllerUtilsImpl.isImeApplication(this.mContext, realCallingPid, realCallingUid) && MultiDisplayManager.isAppShareDisplayId(this.mVivoRatioControllerUtilsImpl.getCurrentInputMethodDisplayId())) {
            shouldBlock = true;
        }
        if (shouldBlock) {
            this.mAppShareHandler.sendEmptyMessage(MSG_APP_SHARE_FORBID_APP_START);
        }
        return shouldBlock;
    }

    private boolean shouldAssignDisplayIdLocked(String callingPackage, int realCallingUid) {
        String str;
        int i;
        return AppShareConfig.SUPPROT_APPSHARE && isAppShareDisplayExist() && (str = this.mAppSharePackageName) != null && str.equals(callingPackage) && (i = this.mAppShareUserId) != -1 && i == UserHandle.getUserId(realCallingUid);
    }

    public int startAppShareOrBlockStartLocked(ActivityRecord sourceRecord, ActivityRecord r, ActivityRecord reusedActivity, String lastStartReason) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            String targetPackage = (r == null || r.mActivityComponent == null) ? null : r.mActivityComponent.getPackageName();
            if (isOnAppShareDisplay(targetPackage, r.mUserId)) {
                boolean callFromSelfOnAppShareDisplay = sourceRecord != null && MultiDisplayManager.isAppShareDisplayId(sourceRecord.getDisplayId()) && targetPackage.equals(r.launchedFromPackage);
                boolean startDeliveredToTop = !(callFromSelfOnAppShareDisplay || reusedActivity == null || reusedActivity.getTask() == null || reusedActivity.getTask().realActivity == null || !reusedActivity.getTask().realActivity.equals(r.mActivityComponent) || !MultiDisplayManager.isAppShareDisplayId(reusedActivity.getDisplayId())) || (!targetPackage.equals(r.launchedFromPackage) && reusedActivity == null && r.intent != null && "android.intent.action.MAIN".equals(r.intent.getAction()) && r.intent.getCategories() != null && r.intent.getCategories().contains("android.intent.category.LAUNCHER"));
                boolean inSplitScreenMode = this.mWMService.isInVivoMultiWindowIgnoreVisibility();
                boolean sendToAppShare = (r.intent == null || r.intent.getAction() == null || !"android.intent.action.SEND".equals(r.intent.getAction())) ? false : true;
                if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                    VSlog.i(TAG, "startAppShareOrBlockStartLocked, mService.mAppShareController = " + this.mAppShareController + ", callFromSelfOnAppShareDisplay = " + callFromSelfOnAppShareDisplay + ", startDeliveredToTop = " + startDeliveredToTop + ", mLastStartReason = " + lastStartReason + ", r.launchedFromPackage = " + r.launchedFromPackage + ", inSplitScreenMode = " + inSplitScreenMode + ", mIntent = " + r.intent + ", sendToAppShare = " + sendToAppShare);
                }
                if ((startDeliveredToTop && VivoMultiWindowConfig.SMART_MULTIWINDOW_NAME.equals(r.launchedFromPackage)) || ((startDeliveredToTop && inSplitScreenMode && "com.vivo.SmartKey".equals(r.launchedFromPackage)) || (inSplitScreenMode && "PendingIntentRecord".equals(lastStartReason)))) {
                    this.mAppShareHandler.obtainMessage(MSG_APP_SHARE_FORBID_ENTER_MULTIWINDOW_MODE, false).sendToTarget();
                    return 102;
                }
                boolean notifyAppShare = false;
                long delayMillis = 0;
                if (startDeliveredToTop) {
                    notifyAppShare = true;
                    delayMillis = 0;
                } else if ("PendingIntentRecord".equals(lastStartReason) || sendToAppShare) {
                    notifyAppShare = true;
                    delayMillis = 600;
                }
                if (notifyAppShare) {
                    boolean startFromRecents = false;
                    int taskId = -1;
                    ActivityStack appShareStack = findAppShareStackLocked();
                    if (startDeliveredToTop && appShareStack != null && appShareStack.getChildCount() > 1) {
                        Task topTask = appShareStack.getTopMostTask();
                        ActivityRecord topActivity = appShareStack.topRunningActivityLocked();
                        if (topActivity != null && !isAppShareContainerLocked(topActivity) && topTask.realActivity != null && AppShareConfig.getInstance().isStartInAppShareDisplayBlackList(topTask.realActivity.flattenToShortString())) {
                            startFromRecents = true;
                            taskId = topTask.mTaskId;
                        }
                    }
                    if (startFromRecents) {
                        this.mAppShareHandler.obtainMessage(MSG_APP_SHARE_START_ACTIVITY_FROM_RECENTS, Integer.valueOf(taskId)).sendToTarget();
                    } else {
                        this.mAppShareHandler.obtainMessage(MSG_APP_SHARE_START_ACTIVITY_DELAYED, Long.valueOf(delayMillis)).sendToTarget();
                    }
                }
                if (startDeliveredToTop) {
                    return 3;
                }
                return 0;
            }
            return 0;
        }
        return 0;
    }

    public boolean shouldIgnoreChildrenModeByAppShareLocked(String callingPackage, int realCallingUid, Intent intent) {
        if (AppShareConfig.SUPPROT_APPSHARE && isAppShareDisplayExist()) {
            ComponentName name = intent != null ? intent.getComponent() : null;
            if (name == null && intent != null && intent.getSelector() != null) {
                name = intent.getSelector().getComponent();
            }
            if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                VSlog.i(TAG, "shouldIgnoreChildrenModeByAppShareLocked, callingPackage = " + callingPackage + ", realCallingUid = " + realCallingUid + ", name = " + name);
            }
            int userId = UserHandle.getUserId(realCallingUid);
            boolean isOnAppShareDisplay = isOnAppShareDisplay(callingPackage, userId);
            String str = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            if (isOnAppShareDisplay) {
                if (name != null) {
                    str = name.getPackageName();
                }
                String targetPackage = str;
                if (TextUtils.equals(callingPackage, targetPackage)) {
                    return true;
                }
                return this.mIsControlledByRemote;
            }
            if (name != null) {
                str = name.flattenToShortString();
            }
            String flattenToShortString = str;
            return AppShareConfig.APP_SHARE_ACTIVITY_SHORT_STRING.equals(flattenToShortString);
        }
        return false;
    }

    public void resetAppShareCallingPackage() {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        this.mAppShareCallingPackage = null;
    }

    public ActivityOptions setLaunchDisplayIdForAppShareIfNeededLocked(ActivityInfo aInfo, Intent intent, ActivityRecord r, ActivityRecord sourceRecord, String callingPackage, int realCallingUid, ActivityOptions checkedOptions) {
        int launchDisplayId;
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return checkedOptions;
        }
        resetAppShareCallingPackage();
        boolean sendToAppShare = false;
        int targetUid = (aInfo == null || aInfo.applicationInfo == null) ? 0 : aInfo.applicationInfo.uid;
        String flattenToShortString = null;
        String targetPackage = (aInfo == null || aInfo.applicationInfo == null) ? null : aInfo.applicationInfo.packageName;
        if (intent != null && intent.getComponent() != null) {
            flattenToShortString = intent.getComponent().flattenToShortString();
        }
        if (r.intent != null && r.intent.getAction() != null && "android.intent.action.SEND".equals(r.intent.getAction())) {
            sendToAppShare = true;
        }
        if (shouldAssignDisplayIdLocked(callingPackage, realCallingUid)) {
            VSlog.i(TAG, "assign displayId for " + callingPackage + ", uid = " + realCallingUid + ", targetPackage = " + targetPackage);
            if (TextUtils.equals(callingPackage, targetPackage) && !AppShareConfig.getInstance().isStartInAppShareDisplayBlackList(r.mActivityComponent.flattenToShortString())) {
                launchDisplayId = MSG_APP_SHARE_FORBID_USE_HARDWARE;
            } else if (AppShareConfig.getInstance().isForceStartToAppShareDisplayWhiteList(flattenToShortString)) {
                launchDisplayId = MSG_APP_SHARE_FORBID_USE_HARDWARE;
            } else {
                launchDisplayId = 0;
                if (sourceRecord != null && MultiDisplayManager.isAppShareDisplayId(sourceRecord.getDisplayId())) {
                    this.mAppShareCallingPackage = callingPackage;
                }
            }
            if (checkedOptions == null) {
                checkedOptions = ActivityOptions.makeBasic();
            }
            checkedOptions.setLaunchDisplayId(launchDisplayId);
        } else if (isOnAppShareDisplay(targetPackage, UserHandle.getUserId(targetUid)) && sendToAppShare) {
            VSlog.i(TAG, "assign appShare-displayId for targetPackage: " + targetPackage);
            if (checkedOptions == null) {
                checkedOptions = ActivityOptions.makeBasic();
            }
            checkedOptions.setLaunchDisplayId(MSG_APP_SHARE_FORBID_USE_HARDWARE);
        }
        return checkedOptions;
    }

    public boolean createNewTaskForAppShareLocked(ActivityRecord r) {
        if (this.mAppShareCallingPackage == null) {
            return false;
        }
        String flattenToShortString = (r == null || r.mActivityComponent == null) ? null : r.mActivityComponent.flattenToShortString();
        if (AppShareConfig.getInstance().isForceStartToAppShareDisplayWhiteList(flattenToShortString)) {
            return false;
        }
        VSlog.d(TAG, "startInMainDisplay : " + flattenToShortString);
        return true;
    }

    public boolean needKeepRotationByAppShare(int displayId, int orientation, int lastRotation) {
        if (AppShareConfig.SUPPROT_APPSHARE && isAppsharedDisplayId(displayId) && isSensorOrientation(orientation)) {
            VSlog.d(TAG, "needKeepRotationByAppShare displayId: " + displayId + ", orientation: " + orientation + ", lastRotation: " + lastRotation + ", keepRotation: " + this.mKeepRotation);
            return this.mKeepRotation;
        }
        return false;
    }

    public boolean isNeedFindFocusedWindow(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return false;
        }
        return isAppsharedDisplayId(displayId);
    }

    public boolean isAppsharedMode() {
        return AppShareConfig.SUPPROT_APPSHARE && this.mWMService.mRoot.getDisplayContent((int) MSG_APP_SHARE_FORBID_USE_HARDWARE) != null;
    }

    public boolean isAppsharedDisplayId(int displayId) {
        return AppShareConfig.SUPPROT_APPSHARE && isAppsharedMode() && MultiDisplayManager.isAppShareDisplayId(displayId);
    }

    public boolean handleInputMethodNeedMovedWithDisplay(WindowState win, WindowManager.LayoutParams attrs, int displayId, boolean updateConfig) {
        DisplayContent mImeSrcDc;
        if (AppShareConfig.SUPPROT_APPSHARE) {
            int imeDstDisplayId = getInputMethodDstDisplayId();
            if (attrs == null || !isAppsharedMode() || displayId == imeDstDisplayId || !isWindowImeType(win, attrs) || (mImeSrcDc = this.mWMService.mRoot.getDisplayContent(win.getDisplayId())) == null) {
                return false;
            }
            DisplayContent mImeDstDc = this.mWMService.mRoot.getDisplayContent(imeDstDisplayId);
            if (mImeDstDc == null || mImeSrcDc.mInputMethodWindow == null) {
                return false;
            }
            this.mWMService.mRoot.getDisplayContent(imeDstDisplayId).getVivoInjectInstance().moveImeToDisplay(mImeSrcDc.getDisplayId(), updateConfig, false);
            return true;
        }
        return false;
    }

    private boolean isImeSelectDialog(WindowState win, WindowManager.LayoutParams attrs) {
        if (win == null) {
            return false;
        }
        int type = attrs != null ? attrs.type : win.mAttrs.type;
        String title = win.getWindowTag() != null ? win.getWindowTag().toString() : null;
        return type == 2012 && title != null && title.equals("Select input method");
    }

    private boolean isWindowImeType(WindowState win, WindowManager.LayoutParams attrs) {
        if (win == null) {
            return false;
        }
        int type = attrs != null ? attrs.type : win.mAttrs.type;
        return type == 2011 || type == 2012 || win.isInputMethodWindow();
    }

    public boolean isInputMethodSelectDialogToken(WindowToken wtoken) {
        int size;
        if (AppShareConfig.SUPPROT_APPSHARE && wtoken != null && wtoken.windowType == 2012 && (size = wtoken.mChildren.size()) > 0) {
            for (int i = 0; i < size; i++) {
                WindowState win = (WindowState) wtoken.mChildren.get(i);
                if (win != null && isImeSelectDialog(win, win.mAttrs)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean handleInputMethodNeedMoved(WindowState win, WindowManager.LayoutParams attrs, int viewVisibility, boolean updateConfig) {
        if (AppShareConfig.SUPPROT_APPSHARE && viewVisibility == 0) {
            if (win.isInputMethodWindow()) {
                updateInputMethodShow(win, true);
            }
            if (isAppsharedMode() && isWindowImeType(win, attrs)) {
                if (!isImeSelectDialog(win, attrs) || isAppShareForeground()) {
                    int imeDstDisplayId = getInputMethodDstDisplayId();
                    if ((isAppShareForeground() || isShareAppInPipMode()) && imeDstDisplayId == 0) {
                        imeDstDisplayId = MSG_APP_SHARE_FORBID_USE_HARDWARE;
                        updateImeShowDisplayId(MSG_APP_SHARE_FORBID_USE_HARDWARE);
                    }
                    int displayId = win.getDisplayId();
                    VSlog.d(TAG, "handle ime need moved cur ime displayId : " + imeDstDisplayId + ", win displayId : " + displayId);
                    if (imeDstDisplayId == displayId) {
                        return false;
                    }
                    this.mWMService.mRoot.getDisplayContent(imeDstDisplayId).getVivoInjectInstance().moveImeToDisplay(displayId, updateConfig, false);
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    private boolean isInputMethodMainWindow(WindowState win) {
        if (win == null || !win.isInputMethodWindow()) {
            return false;
        }
        String title = win.getWindowTag() != null ? win.getWindowTag().toString() : null;
        if (title == null) {
            return false;
        }
        return title.equals("InputMethod") || title.equals("SecureInputMethod");
    }

    public void updateInputMethodShow(WindowState win, boolean show) {
        if (!AppShareConfig.SUPPROT_APPSHARE || win == null || win.mAttrs.type == 1003) {
            return;
        }
        if (isImeSelectDialog(win, null)) {
            VSlog.d(TAG, "updateImeSelectDialog Shown : " + show);
            return;
        }
        String title = win.getWindowTag() != null ? win.getWindowTag().toString() : null;
        if (title == null) {
            return;
        }
        boolean lastShown = isImeShown();
        int lastShowDisplayId = getImeShownDisplayId();
        if (title.equals("SecureInputMethod")) {
            updateImeShowStatusChanged(show, true, win.getDisplayId());
        } else if (!title.equals("InputMethod")) {
            VSlog.w(TAG, "updateInputMethodShow shown is not ime main window, then return.");
            return;
        } else {
            updateImeShowStatusChanged(show, false, win.getDisplayId());
        }
        VSlog.d(TAG, "updateInputMethodShow shown : " + show + ", win displayId : " + win.getDisplayId() + ", win: " + win + ", req w: " + win.mRequestedWidth + ", h: " + win.mRequestedHeight);
        if (!isAppsharedMode()) {
            return;
        }
        if (isRealyShown()) {
            updateImeLastShownDisplayId(win.getDisplayId());
            reportImeShownStatus();
        }
        if (!lastShown || lastShowDisplayId != MSG_APP_SHARE_FORBID_USE_HARDWARE || !isImeShown() || getImeShownDisplayId() == MSG_APP_SHARE_FORBID_USE_HARDWARE) {
            if (win.getDisplayId() == MSG_APP_SHARE_FORBID_USE_HARDWARE) {
                notifyImeStatusInShareDisplay(isImeShown());
                return;
            }
            return;
        }
        notifyImeStatusInShareDisplay(false);
    }

    private void notifyImeStatusInShareDisplay(boolean shown) {
        sendMyMessage(this.mAppShareHandler, MSG_APP_SHARE_NOTIFY_IME_STATUS_VIRTUAL_DISPLAY, shown ? 1 : 0, 0, 0, null);
    }

    public void reportImeShownStatus() {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        sendMyMessage(this.mAppShareHandler, MSG_IME_STATUS_CHANGED, isImeShown() ? 1 : 0, 0, 0, null);
    }

    public void updateInputMethodAnimationDone(WindowState win, boolean exitAnim) {
        if (!AppShareConfig.SUPPROT_APPSHARE || win == null) {
            return;
        }
        if (!isInputMethodMainWindow(win)) {
            VSlog.w(TAG, "update input status but is not inputmethod main window win: " + win + ", req w: " + win.mRequestedWidth + ", h: " + win.mRequestedHeight);
            return;
        }
        updateInputMethodShow(win, !exitAnim);
        if (!exitAnim) {
            updateImeLastShownDisplayId(getInputMethodDstDisplayId());
        }
    }

    public void updateWindowAnimationDone(WindowState win, boolean exitAnim) {
        if (!AppShareConfig.SUPPROT_APPSHARE || win == null) {
            return;
        }
        if (win.isInputMethodWindow()) {
            if (exitAnim) {
                updateInputMethodAnimationDone(win, exitAnim);
            }
        } else if (!isAppsharedMode() || win.mAttrs.type != 2) {
        } else {
            String title = win.getWindowTag() != null ? win.getWindowTag().toString() : null;
            if (title == null || !title.contains("com.smile.gifmaker") || !title.contains("PhotoDetailActivity")) {
                return;
            }
            int displayId = win.getDisplayId();
            WindowState focused = getFocusedWindowForDisplay(displayId);
            VSlog.d(TAG, "current focused win : " + focused);
            if (focused == null || focused.mAttrs.type != 1) {
                return;
            }
            String title2 = win.getWindowTag() != null ? win.getWindowTag().toString() : null;
            if (title2 == null || !title2.contains("com.smile.gifmaker") || !title2.contains("PhotoDetailActivity")) {
                return;
            }
            focused.reportSpecialDialogRemoveOk();
        }
    }

    public boolean needUpdateWindowConfiguration(WindowState win, int displayId, MergedConfiguration configuration) {
        Configuration currentDisplayConfig;
        if (AppShareConfig.SUPPROT_APPSHARE && win != null && win.isInputMethodWindow() && win.mViewVisibility == 0 && isAppsharedMode()) {
            if ((isImeSelectDialog(win, null) && !isAppShareForeground()) || configuration == null || configuration.getGlobalConfiguration() == null) {
                return false;
            }
            int dstDisplayId = getInputMethodDstDisplayId();
            if (this.mWMService.mRoot.getDisplayContent(dstDisplayId) == null || (currentDisplayConfig = this.mWMService.mRoot.getDisplayContent(dstDisplayId).getConfiguration()) == null) {
                return false;
            }
            VSlog.d(TAG, "relayout ime window current prefer display: " + displayId + ", dst display : " + dstDisplayId + ", current config : " + currentDisplayConfig + ", global configuration : " + configuration.getGlobalConfiguration());
            return currentDisplayConfig.orientation != configuration.getGlobalConfiguration().orientation;
        }
        return false;
    }

    public void reportImeConfigUpdate() {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        updateConfigurationForInputMethod(getInputMethodDstDisplayId(), false);
    }

    public DisplayContent getDesiredImeFocusStackLocked(DisplayContent preferDisplayContent) {
        if (!AppShareConfig.SUPPROT_APPSHARE || !isAppsharedMode() || isAppSharedRemoving() || !MultiDisplayManager.isAppShareDisplayId(getInputMethodDstDisplayId())) {
            return preferDisplayContent;
        }
        DisplayContent finalDisplayContent = this.mWMService.mRoot.getDisplayContent((int) MSG_APP_SHARE_FORBID_USE_HARDWARE);
        return finalDisplayContent;
    }

    public DisplayContent getDesiredImeDisplayContent(DisplayContent preferDisplayContent, int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE || !isAppsharedMode()) {
            return preferDisplayContent;
        }
        int imeDisplayId = getInputMethodDstDisplayId();
        VSlog.d(TAG, "getDesiredImeDisplayContent old displayId: " + displayId + ", imeDisplayId: " + imeDisplayId);
        DisplayContent finalDisplayContent = this.mWMService.mRoot.getDisplayContent(imeDisplayId);
        return finalDisplayContent;
    }

    public void onInputMethodMoved(int pid, int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE || this.mVivoRatioControllerUtilsImpl == null) {
            return;
        }
        VSlog.d(TAG, "onInputMethodMoved pid: " + pid + ", displayId: " + displayId);
        this.mVivoRatioControllerUtilsImpl.addDisplayId(pid, displayId, "moveIME");
    }

    public void updateMergedConfigurationForAppShared(WindowState win, MergedConfiguration mergedConfiguration) {
        int displayId;
        DisplayContent displayContent;
        Configuration configuration;
        if (!AppShareConfig.SUPPROT_APPSHARE || win == null || mergedConfiguration == null || !isAppsharedMode() || (displayId = win.getDisplayId()) != MSG_APP_SHARE_FORBID_USE_HARDWARE || (displayContent = this.mWMService.mRoot.getDisplayContent(displayId)) == null || (configuration = displayContent.getConfiguration()) == null) {
            return;
        }
        Configuration overrideConfiguration = mergedConfiguration.getOverrideConfiguration();
        VSlog.d(TAG, "updateMergedConfiguration for win:" + win + ", global: " + configuration + ", override: " + overrideConfiguration);
        mergedConfiguration.setConfiguration(configuration, mergedConfiguration.getOverrideConfiguration());
    }

    private void updateConfigurationForAppInShareDisplay(Configuration values, int displayId) {
        Configuration configCopy = new Configuration(values);
        ArrayList<Integer> mUpdateUid = new ArrayList<>();
        SparseArray<WindowProcessController> pidMap = this.mActivityTaskManagerService.mProcessMap.getPidMap();
        for (int i = pidMap.size() - 1; i >= 0; i--) {
            boolean shouldInform = false;
            int pid = pidMap.keyAt(i);
            WindowProcessController app = pidMap.get(pid);
            if (!app.mActivities.isEmpty()) {
                int k = 0;
                while (true) {
                    if (k >= app.mActivities.size()) {
                        break;
                    }
                    ActivityRecord activityRecord = (ActivityRecord) app.mActivities.get(k);
                    if (activityRecord.getDisplayId() != displayId) {
                        k++;
                    } else {
                        shouldInform = true;
                        break;
                    }
                }
            }
            if (shouldInform && !mUpdateUid.contains(Integer.valueOf(app.mUid))) {
                mUpdateUid.add(Integer.valueOf(app.mUid));
                VSlog.v(TAG, "Sending to proc " + app.mName + ", pid: " + app.mPid + ", new config " + configCopy);
                for (int k2 = pidMap.size() + (-1); k2 >= 0; k2--) {
                    int subpid = pidMap.keyAt(k2);
                    WindowProcessController subapp = pidMap.get(subpid);
                    if (subapp.mUid == app.mUid) {
                        subapp.onConfigurationChanged(configCopy);
                    }
                }
            }
        }
    }

    public boolean isLandscapeOrientation(int orientation, int rotation) {
        boolean landscape = isLandscapeOrientation(orientation);
        return landscape || orientation == 0 || orientation == 6 || orientation == 8 || orientation == 11 || rotation == 1 || rotation == 3;
    }

    public boolean isNeedSkipFreeze(int displayId) {
        if (AppShareConfig.SUPPROT_APPSHARE && isAppsharedDisplayId(displayId)) {
            return (isAppShareForeground() || isShareAppInPipMode()) && !this.mWMService.mPolicy.isKeyguardLocked();
        }
        return false;
    }

    public boolean canResizeDisplay(int displayId) {
        return AppShareConfig.SUPPROT_APPSHARE && isAppsharedDisplayId(displayId);
    }

    public int adjustDisplayFreezeTime(int displayId, int preferTime) {
        int time = preferTime;
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return time;
        }
        if (displayId < 0) {
            if ((isAppShareForeground() || isShareAppInPipMode()) && isAppSharedRotateFreeze()) {
                time = this.WINDOW_FREEZE_TIMEOUT_DURATION_APP_SHARED;
            }
        } else if ((isAppShareForeground() || isShareAppInPipMode()) && displayId == 0 && isAppSharedRequestedOrientation()) {
            updateAppSharedRotateFreeze(true);
            updateAppSharedRequestedOrientation(false);
            time = this.WINDOW_FREEZE_TIMEOUT_DURATION_APP_SHARED;
        }
        VSlog.d(TAG, "adjust display freeze time: " + time);
        return time;
    }

    private boolean isWhiteInputMethod(String packageName) {
        if (packageName == null) {
            return false;
        }
        Iterator<String> it = mWhiteImePkg.iterator();
        while (it.hasNext()) {
            String item = it.next();
            if (item.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public int adjustDisplayIdForWindowInAppShare(WindowManager.LayoutParams attrs, int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return displayId;
        }
        if (attrs == null) {
            return displayId;
        }
        if (!isAppsharedMode()) {
            return displayId;
        }
        if (displayId != 0 && displayId != MSG_APP_SHARE_FORBID_USE_HARDWARE) {
            return displayId;
        }
        if (!isWhiteInputMethod(attrs.packageName)) {
            return displayId;
        }
        if (attrs.type != 1000) {
            return displayId;
        }
        WindowState parentWindow = this.mWMService.windowForClientLocked((Session) null, attrs.token, false);
        if (parentWindow == null) {
            return displayId;
        }
        if (!parentWindow.isInputMethodWindow() || parentWindow.mAttrs.type != 2011) {
            return displayId;
        }
        WindowToken token = null;
        int count = this.mWMService.mRoot.mChildren.size();
        int j = 0;
        while (true) {
            if (j >= count) {
                break;
            }
            DisplayContent dc = (DisplayContent) this.mWMService.mRoot.mChildren.get(j);
            token = dc.getWindowToken(parentWindow != null ? parentWindow.mAttrs.token : attrs.token);
            if (token == null) {
                j++;
            } else {
                VSlog.d(TAG, "addWindow : find token in " + displayId);
                break;
            }
        }
        if (token != null) {
            return token.getDisplayContent().getDisplayId();
        }
        return getInputMethodDstDisplayId();
    }

    public DisplayContent adjustDisplayForWindowTokenAppShare(int type, int displayId, DisplayContent dc) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return dc;
        }
        if (!isAppsharedMode()) {
            return dc;
        }
        return dc;
    }

    public boolean needSkipDisplayEffectService(int displayId) {
        DisplayContent displayContent;
        if (AppShareConfig.SUPPROT_APPSHARE && (displayContent = this.mWMService.mRoot.getDisplayContent(displayId)) != null) {
            return displayContent.isAppSharedRecordDisplay();
        }
        return false;
    }

    public void prepareSharedScreenShot(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        if (!isNeedSkipFreeze(displayId)) {
            if (needTellAppShareFreeze(displayId)) {
                tellAppShareFreeze();
                return;
            }
            return;
        }
        prepareScreenShotAppShared();
    }

    private boolean needTellAppShareFreeze(int displayId) {
        return AppShareConfig.SUPPROT_APPSHARE && displayId == MSG_APP_SHARE_FORBID_USE_HARDWARE && isAppsharedMode();
    }

    private void prepareScreenShotAppShared() {
        Task appShareTask;
        if (!AppShareConfig.SUPPROT_APPSHARE || (appShareTask = findAppShareTaskByDisplayIdLocked(0)) == null) {
            return;
        }
        ActivityRecord r = appShareTask.getTopMostActivity();
        VSlog.d(TAG, "prepareScreenShotAppShared r : " + r);
        if (r == null || r.appToken == null) {
            return;
        }
        updateAppShareKeepRoation(false);
        notifyAppShareFreezeLocked();
        this.mMultiWindowWmsInstance.prepareLongTransFreezeWindowFrame();
        this.mMultiWindowWmsInstance.waitExitMultiWindowFreezeAnimation(r.appToken);
    }

    private void notifyAppShareFreezeLocked() {
        this.mAppShareHandler.sendEmptyMessage(MSG_APP_SHARE_NOTIFY_FREEZE);
    }

    private void tellAppShareFreeze() {
        Task appShareTask;
        if (!AppShareConfig.SUPPROT_APPSHARE || (appShareTask = findAppShareTaskByDisplayIdLocked(0)) == null) {
            return;
        }
        ActivityRecord r = appShareTask.getTopMostActivity();
        VSlog.d(TAG, "notifyAppShareFreezeLocked r : " + r);
        if (r == null || r.appToken == null) {
            return;
        }
        this.mAppShareHandler.sendEmptyMessage(MSG_APP_SHARE_NOTIFY_FREEZE);
    }

    public boolean isKeepScreenOffPrcocessAppShare(String action, String packageName) {
        if (AppShareConfig.SUPPROT_APPSHARE && packageName != null && action != null && action.equals("android.intent.action.SCREEN_OFF")) {
            synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                if (isAppShareDisplayExist()) {
                    if (isImeShown()) {
                        String imePackageName = this.mVivoRatioControllerUtilsImpl.getCurrentInputMethodPackageName();
                        String imeNonePackageName = this.mVivoRatioControllerUtilsImpl.getCurrentNoneSecureInputMethodPackageName();
                        VSlog.d(TAG, "skip boardcast imePackageName : " + imePackageName + ", info packagename : " + packageName);
                        if ((imePackageName != null && imePackageName.equals(packageName)) || (imeNonePackageName != null && imeNonePackageName.equals(packageName))) {
                            int displayId = getInputMethodDstDisplayId();
                            if (MultiDisplayManager.isAppShareDisplayId(displayId)) {
                                return true;
                            }
                        }
                        String shareRunningPackageName = getAppShareRunningPackageName();
                        VSlog.d(TAG, "skip boardcast shareRunningPackageName : " + shareRunningPackageName + ", info packagename : " + packageName);
                        return shareRunningPackageName != null && shareRunningPackageName.equals(packageName);
                    }
                    return false;
                }
                return false;
            }
        }
        return false;
    }

    private String getAppShareRunningPackageName() {
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            DisplayContent otherDisplay = getAppShareDisplay();
            if (otherDisplay == null) {
                return null;
            }
            ActivityStack otherStack = otherDisplay.getTopStack();
            if (otherStack == null) {
                return null;
            }
            ActivityRecord otherR = otherStack.topRunningActivityLocked();
            if (otherR == null) {
                return null;
            }
            return otherR.mActivityComponent != null ? otherR.mActivityComponent.getPackageName() : null;
        }
    }

    public boolean isRequestOrientationByShareDisplay(int displayId) {
        return AppShareConfig.SUPPROT_APPSHARE && displayId == MSG_APP_SHARE_FORBID_USE_HARDWARE && isAppsharedMode() && isAppShareForeground() && !this.mWMService.mPolicy.isKeyguardLocked();
    }

    public void decideFreezeDisplay(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        if ((isAppShareForeground() || isShareAppInPipMode()) && displayId == 0 && isAppSharedRequestedOrientation()) {
            updateAppSharedRotateFreeze(true);
            updateAppSharedRequestedOrientation(false);
            VSlog.d(TAG, "freeze displayId : " + displayId + ", freeze : " + isAppSharedRotateFreeze());
        }
    }

    public void updateDisplaySecureLockedForAppShare(DisplayContent displayContent) {
        boolean secure;
        if (!AppShareConfig.SUPPROT_APPSHARE || displayContent == null || !isAppsharedMode() || !MultiDisplayManager.isAppShareDisplayId(displayContent.getDisplayId()) || (secure = displayContent.isWindowSecureForAppShare()) == this.mMainSecure) {
            return;
        }
        this.mMainSecure = secure;
        VSlog.i(TAG, "task is secure : " + secure);
        Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_NOTIFY_TASK_SECURE, Boolean.valueOf(secure)).sendToTarget();
    }

    public static void sendMyMessage(Handler handler, int what, int arg1, int arg2, int delay, Object obj) {
        if (handler == null) {
            return;
        }
        Message msg = Message.obtain();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        if (delay <= 0) {
            handler.sendMessage(msg);
        } else {
            handler.sendMessageDelayed(msg, delay);
        }
    }

    public boolean needUpdateInputMethodClientForce() {
        return AppShareConfig.SUPPROT_APPSHARE && isAppsharedMode() && isImeSwitchClient();
    }

    public void updateAppSharedRequestedOrientation(boolean requested) {
        this.mAppShareRequestedOrientation = requested;
        VSlog.d(TAG, "updateAppSharedRequestedOrientation requested : " + requested);
    }

    public boolean isAppSharedRequestedOrientation() {
        return this.mAppShareRequestedOrientation;
    }

    public void updateAppSharedRotateFreeze(boolean freeze) {
        this.mAppShareRotateFreeze = freeze;
        VSlog.d(TAG, "updateAppSharedRotateFreeze freeze : " + freeze);
    }

    public boolean isAppSharedRotateFreeze() {
        return this.mAppShareRotateFreeze;
    }

    public boolean isAppSharedRemoving() {
        return this.mShareDisplayRemoving;
    }

    public void reportFocusedWindowInputMethodStatus(int displayId, WindowState target) {
        if (!AppShareConfig.SUPPROT_APPSHARE || !isAppsharedMode()) {
            return;
        }
        sendMyMessage(this.mAppShareHandler, REPORT_WINDOW_IME_STATUS, 0, 0, 0, target);
    }

    public boolean computeImeTarget(int displayId, WindowState w) {
        if (AppShareConfig.SUPPROT_APPSHARE && w != null) {
            if ((isAppsharedMode() || isAppSharedRemoving()) && !isAppShareWindow(w)) {
                if (isInputMethodShownEvent() && getInputMethodShowEventDisplayId() >= 0 && w.getDisplayId() == getInputMethodShowEventDisplayId()) {
                    return true;
                }
                if (getImeLastShownDisplayId() < 0 || w.getDisplayId() != getImeLastShownDisplayId()) {
                    DisplayContent displayContent = this.mWMService.mRoot.getDisplayContent(displayId);
                    return displayContent != null && displayContent.mInputMethodWindow != null && displayContent.mInputMethodWindow.isVisible() && w.getDisplayId() == displayContent.mInputMethodWindow.getDisplayId();
                }
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean isAppShareWindow(WindowState win) {
        if (win == null || win.mActivityRecord == null || win.getWindowTag() == null) {
            return false;
        }
        String string = win.getWindowTag().toString();
        return string.contains(AppShareConfig.APP_SHARE_PKG_NAME) && string.contains("ShareDisplayActivity");
    }

    public boolean isImeShowInShareDisplay() {
        return AppShareConfig.SUPPROT_APPSHARE && isAppsharedMode() && isImeShown() && MultiDisplayManager.isAppShareDisplayId(getInputMethodDstDisplayId());
    }

    public void updatePointEvent(int displayId, int deviceId) {
        if (!AppShareConfig.SUPPROT_APPSHARE || displayId != 0 || deviceId <= 0) {
            return;
        }
        this.mMainDeviceId = deviceId;
    }

    public void adjustWindowAppShared(WindowState win) {
        if (!AppShareConfig.SUPPROT_APPSHARE || win == null) {
            return;
        }
        if (this.mStatusBarsHeight <= 0) {
            this.mStatusBarsHeight = this.mContext.getResources().getDimensionPixelSize(17105488);
        }
        if (isAppsharedMode() && !MultiDisplayManager.isAppShareDisplayId(win.getDisplayId())) {
        }
    }

    public boolean isAppShareKeepWindow(WindowState win) {
        return AppShareConfig.SUPPROT_APPSHARE && win != null && this.mAppSharedKeepWindow == win;
    }

    public boolean checkAlertOpsWhenAddInAppSharedMode(int res, int appOp, String owningPackageName, int displayId) {
        return AppShareConfig.SUPPROT_APPSHARE && res != 0 && appOp == 24 && owningPackageName != null && isAppsharedDisplayId(displayId) && this.mWMService.mRoot.getDisplayContent(displayId) != null;
    }

    public boolean checkAlertOpsWhenAddInAppSharedMode(WindowState win, int mAppOp, String packageName) {
        if (AppShareConfig.SUPPROT_APPSHARE && win != null && packageName != null && mAppOp == 24) {
            int displayId = win.getDisplayId();
            return isAppsharedDisplayId(displayId) && this.mWMService.mRoot.getDisplayContent(displayId) != null;
        }
        return false;
    }

    public void reportNewFocusForDisplay(int displayId, WindowState newFocus) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        reportFocusedWindowInputMethodStatus(displayId, newFocus);
        if (displayId == 0 && newFocus != null && newFocus != this.mAppSharedKeepWindow) {
            this.mAppSharedKeepWindow = null;
        }
    }

    public void onDisplayAdded(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        VSlog.d(TAG, "onDisplayAdded displayId: " + displayId);
        if (MultiDisplayManager.isAppShareDisplayId(displayId)) {
            setInputMethodStateChangeListener(this.mInputMethodStatusListener);
            sendMyMessage(this.mAppShareHandler, REPORT_APPSHARE_DISPLAY_SHOW_IME, displayId, 1, 0, null);
        }
    }

    public void onDisplayRemoved(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        VSlog.d(TAG, "onDisplayRemoved displayId: " + displayId);
        if (!MultiDisplayManager.isAppShareDisplayId(displayId)) {
            return;
        }
        reportImeFocusOut(displayId);
        sendMyMessage(this.mAppShareHandler, REPORT_APPSHARE_DISPLAY_REMOVE_START, displayId, 0, 0, null);
    }

    private void reportImeFocusOut(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        VSlog.d(TAG, "reportImeFocusOut displayId : " + displayId + ", ime shown : " + isImeShown());
        if (displayId == MSG_APP_SHARE_FORBID_USE_HARDWARE) {
            this.mShareDisplayRemoving = true;
        }
        if (!this.mShareDisplayRemoving) {
            return;
        }
        this.mShareDisplayRemovingImeShown = isImeShown();
        int imeDstDisplayId = getInputMethodDstDisplayId();
        if (isImeShown() && imeDstDisplayId == displayId) {
            synchronized (this.mWMService.mGlobalLock) {
                WindowState win = getFocusedWindowForDisplay(displayId);
                if (win != null) {
                    win.reportImeUnbind();
                }
                WindowState win2 = getFocusedWindowForDisplay(0);
                if (win2 != null) {
                    win2.reportImeUnbind();
                }
                this.mAppSharedKeepWindow = win2;
            }
        }
    }

    private void updateTaskSecure(boolean secure) {
        this.mMainSecure = secure;
    }

    private void handleInputMethodProcessMoved(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE || !MultiDisplayManager.isAppShareDisplayId(displayId)) {
            return;
        }
        VSlog.d(TAG, "handleInputMethodProcessMoved last ime move displayId : " + displayId + ", ime last focus displayId : " + getInputMethodDstDisplayId());
        if (this.mShareDisplayRemovingImeShown) {
            sendMyMessage(this.mAppShareHandler, REPORT_APPSHARE_DISPLAY_REMOVE_COMPLETED, displayId, 0, 0, null);
        } else {
            handleAppShareDisplayRemoveCompleted(displayId);
        }
    }

    private void updateInputMethodDefaultShownValues() {
        updateImeShowDisplayId(0);
        reportLastShownDisplayId(0);
    }

    public void updateInputMethodStatus(boolean immediately) {
        synchronized (this.mWMService.mGlobalLock) {
            if (isAppsharedMode()) {
                if (isInputMethodNeedMoved() && !isImeShown()) {
                    this.mWMService.mRoot.getDisplayContent(getInputMethodDstDisplayId()).getVivoInjectInstance().moveImeToDisplay(getInputMethodDstDisplayId(), true, false);
                    reportMoveToDisplayCompleted(getInputMethodDstDisplayId());
                }
                WindowState win = getFocusedWindowForDisplay(0);
                if (win != null) {
                    if (immediately) {
                        handleUpdateWindowInputMethodStatus(win);
                    } else {
                        reportFocusedWindowInputMethodStatus(0, win);
                    }
                }
                WindowState win2 = getFocusedWindowForDisplay(MSG_APP_SHARE_FORBID_USE_HARDWARE);
                if (win2 != null) {
                    if (immediately) {
                        handleUpdateWindowInputMethodStatus(win2);
                    } else {
                        reportFocusedWindowInputMethodStatus(MSG_APP_SHARE_FORBID_USE_HARDWARE, win2);
                    }
                }
            }
        }
    }

    private WindowState getFocusedWindowForDisplay(int displayId) {
        DisplayContent displayContent = this.mWMService.mRoot.getDisplayContent(displayId);
        if (displayContent == null) {
            return null;
        }
        return displayContent.mCurrentFocus;
    }

    private WindowState getAppShareDisplayFocusedWindow() {
        return getFocusedWindowForDisplay(MSG_APP_SHARE_FORBID_USE_HARDWARE);
    }

    public void reportMoveToDisplayCompleted(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        VSlog.d(TAG, "report ime move to display completed displayId : " + displayId);
        if (isImeShown()) {
            updateImeShowDisplayId(displayId);
            updateImeLastShownDisplayId(displayId);
        }
        WindowState curFocus = getFocusedWindowForDisplay(0);
        WindowState appShareFocus = getFocusedWindowForDisplay(MSG_APP_SHARE_FORBID_USE_HARDWARE);
        if (isAppsharedDisplayId(displayId)) {
            sendMyMessage(this.mAppShareHandler, REPORT_IME_MOVE_DISPLAY_COMPLETED, displayId, 0, 0, appShareFocus);
            sendMyMessage(this.mAppShareHandler, REPORT_IME_MOVE_DISPLAY_COMPLETED, displayId, 0, 0, curFocus);
            return;
        }
        sendMyMessage(this.mAppShareHandler, REPORT_IME_MOVE_DISPLAY_COMPLETED, displayId, 0, 0, curFocus);
        sendMyMessage(this.mAppShareHandler, REPORT_IME_MOVE_DISPLAY_COMPLETED, displayId, 0, 0, appShareFocus);
    }

    public void updateDisplayOrientation(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        sendMyMessage(this.mAppShareHandler, REPORT_UPDATE_DISPLAY_ROTATION, displayId, 0, 0, null);
    }

    private void decideMoveInputMethod() {
        if (!isImeShown()) {
            return;
        }
        VSlog.d(TAG, "input method is showning, then move to shared display");
        InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
        if (inputMethodManagerInternal != null) {
            inputMethodManagerInternal.hideCurrentInputMethod(3);
        }
    }

    public void handleAddWindowForAppShare(WindowState win, boolean shown) {
        WindowState parentWindow;
        if (!AppShareConfig.SUPPROT_APPSHARE || !isAppsharedMode() || isAppSharedRemoving() || win == null || !win.isInputMethodWindow() || win.mAttrs.type != 1003 || win.getDisplayId() != MSG_APP_SHARE_FORBID_USE_HARDWARE || (parentWindow = win.getParentWindow()) == null || parentWindow.mAttrs.type != 2011 || win.mAttrs.packageName == null || !win.mAttrs.packageName.equals(PKG_IFLY_INPUTMETHOD)) {
            return;
        }
        this.mInputShowDialogInAppShared = shown;
        VSlog.d(TAG, "inputmethod selected dialog in virtual display show: " + shown);
    }

    private void updateShowSelectedDialog(boolean shown) {
        this.mInputShowDialogInAppShared = shown;
    }

    private boolean isShowSelectedDialogInAppShare() {
        return this.mInputShowDialogInAppShared;
    }

    public boolean isInputMethodProcess(int uid, int pid) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            return this.mVivoRatioControllerUtilsImpl.isInputMethodProcess(pid) || this.mVivoRatioControllerUtilsImpl.isRunningInputMethodAll(pid);
        }
        return false;
    }

    public boolean shouldBlockImeSetInputMethod(int pid, int uid, String id) {
        if (AppShareConfig.SUPPROT_APPSHARE && !isAppSharedRemoving()) {
            boolean isRunningPidCall = this.mVivoRatioControllerUtilsImpl.isRunningInputMethodAll(pid);
            String runningPackageName = this.mVivoRatioControllerUtilsImpl.getCurrentNoneSecureInputMethodPackageName();
            VSlog.d(TAG, "should ignore set inputmethod pid: " + pid + ", show dialog in app share: " + this.mInputShowDialogInAppShared + ", is running pid: " + this.mVivoRatioControllerUtilsImpl.isRunningInputMethodAll(pid));
            if (this.mInputShowDialogInAppShared && isControlledByRemote() && isRunningPidCall && runningPackageName != null && runningPackageName.equals(PKG_IFLY_INPUTMETHOD)) {
                forbidIMESwitch();
                return true;
            }
            return false;
        }
        return false;
    }

    private void forbidIMESwitch() {
        Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_FORBID_IME_SWITCH, MSG_APP_SHARE_FORBID_USE_HARDWARE, 0).sendToTarget();
    }

    public void updateImeSwitchingStart(boolean switching) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        updateImeSwitching(switching);
        if (!switching) {
            return;
        }
        this.mAppShareHandler.removeMessages(MSG_IME_SWITCH_TIMEOUT);
        this.mAppShareHandler.sendEmptyMessageDelayed(MSG_IME_SWITCH_TIMEOUT, 5000L);
    }

    public boolean isCannotShowImeMenu(int displayId) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            int menuShowDisplayId = getImeMenuShowDisplay();
            updateSwitchMenuShowDisplayId(menuShowDisplayId);
            boolean res = isAppsharedDisplayId(menuShowDisplayId) && isControlledByRemote();
            if (DEBUG) {
                VSlog.d(TAG, "can not show ime menu : " + res + ", show displayId: " + displayId + ", menu show displayId: " + menuShowDisplayId);
            }
            if (res) {
                sendMyMessage(this.mAppShareHandler, MSG_APP_SHARE_FORBID_IME_SWITCH, menuShowDisplayId, 0, 0, null);
            }
            this.mAppShareHandler.removeMessages(MSG_IME_MENU_DESTORY);
            updateImeMenuShowDisplay(0);
            return res;
        }
        return false;
    }

    public int adjustShowMethodMenu(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return displayId;
        }
        if (!isAppsharedMode() || isAppSharedRemoving()) {
            return displayId;
        }
        if (isAppsharedDisplayId(displayId) && isControlledByRemote()) {
            VSlog.d(TAG, "adjust show method menu from displayId: " + displayId + ", to: 0");
            return 0;
        }
        return displayId;
    }

    public void onRemovedWindowLocked(WindowState win) {
        if (!AppShareConfig.SUPPROT_APPSHARE || win == null || !win.isInputMethodWindow()) {
            return;
        }
        if (win.mAttrs.type == 1003) {
            handleAddWindowForAppShare(win, false);
        } else {
            updateInputMethodShow(win, false);
        }
    }

    public void tryInjectMotionEvent(int displayId, int pointX, int pointY) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        sendMyMessage(this.mAppShareHandler, REPORT_INJECT_POINT, displayId, pointX, 0, Integer.valueOf(pointY));
    }

    private MotionEvent generationMotionEvent(long startTime, long endTime, int event, float x, float y, int deviceId, int displayId) {
        MotionEvent.PointerProperties pp1 = new MotionEvent.PointerProperties();
        pp1.id = 0;
        pp1.toolType = 1;
        MotionEvent.PointerProperties[] properties = {pp1};
        MotionEvent.PointerCoords pc1 = new MotionEvent.PointerCoords();
        pc1.x = x;
        pc1.y = y;
        pc1.pressure = 1.0f;
        pc1.size = 1.0f;
        MotionEvent.PointerCoords[] pointerCoords = {pc1};
        return MotionEvent.obtain(startTime, endTime, event, 1, properties, pointerCoords, 0, 0, 1.0f, 1.0f, deviceId, 0, 4098, displayId, 0);
    }

    private WindowState findInputMethodWindowLocked() {
        for (int i = this.mWMService.mRoot.getChildCount() - 1; i >= 0; i--) {
            DisplayContent dc = this.mWMService.mRoot.getChildAt(i);
            if (dc != null && dc.mInputMethodWindow != null) {
                return dc.mInputMethodWindow;
            }
        }
        return null;
    }

    public void updateImeReadyShown(int displayId, boolean ready) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        VSlog.i(TAG, "updateImeReadyShown: displayId = " + displayId + ", ready: " + ready);
        updateImeReadyShownForDisplay(displayId, ready);
        if (ready) {
            this.mAppShareHandler.removeMessages(MSG_IME_READYTOSHOW_TIMEOUT);
            sendMyMessage(this.mAppShareHandler, MSG_IME_MOVE_NEEDED, 1, 0, 0, null);
            this.mAppShareHandler.sendEmptyMessageDelayed(MSG_IME_READYTOSHOW_TIMEOUT, 2000L);
            return;
        }
        this.mAppShareHandler.removeMessages(MSG_IME_READYTOSHOW_TIMEOUT);
    }

    private void onImeReadyToShowTimeOut() {
        if (isImeShown() && !isImeReadyToShow()) {
            return;
        }
        updateImeReadyShown(false);
    }

    public void readyToSwitchDisplay(int displayId, boolean hidecurrent) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        VSlog.i(TAG, "readyToSwitchDisplay: displayId = " + displayId + ", hidecurrent: " + hidecurrent);
        if (hidecurrent) {
            updateImeSwitchClientForDisplay(displayId, hidecurrent);
            sendMyMessage(this.mAppShareHandler, MSG_IME_MOVE_NEEDED, 0, 0, 0, null);
            this.mAppShareHandler.removeMessages(MSG_IME_SWITCH_CLIENT_TIMEOUT);
            this.mAppShareHandler.sendEmptyMessageDelayed(MSG_IME_SWITCH_CLIENT_TIMEOUT, 2000L);
        }
        Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_REPORT_IME_SHOWN, displayId, 0).sendToTarget();
    }

    private void onImeSwitchClientTimeOut() {
        if (isImeShown() && !isImeSwitchClient()) {
            return;
        }
        updateImeSwitchClient(false);
    }

    public void setInputMethodStateChangeListener(InputMethodStatusChangedEvent listener) {
        this.mInputMethodNotify = listener;
    }

    private void notifyInputMethodStatusChanged(boolean immediately) {
        if (this.mInputMethodNotify == null) {
            return;
        }
        if (DEBUG) {
            VSlog.d(TAG, "notifyInputMethodStatusChanged: " + getImeStatusString() + ", immedately : " + immediately);
        }
        this.mInputMethodNotify.onInputMethodStatusChanged(immediately);
    }

    private void updateImeShowStatusChanged(boolean shown, boolean sec, int displayId) {
        int i;
        int i2;
        if (DEBUG) {
            VSlog.d(TAG, "updateImeShowStatusChanged new shown : " + shown + ", sec : " + sec + ", window displayId : " + displayId + ", last shown : " + this.mImeShown + ", display : " + this.mImeShownDisplayId);
        }
        if (sec) {
            this.mSecImeShown = shown;
        } else {
            this.mNormalImeShown = shown;
        }
        boolean lastshown = this.mImeShown;
        boolean changed = false;
        boolean z = this.mSecImeShown | this.mNormalImeShown;
        this.mImeShown = z;
        if (z) {
            if (DEBUG) {
                VSlog.d(TAG, "updateImeShowStatusChanged show check switch : " + this.mImeSwitchToOther + ", display: " + this.mImeSwitchToDisplayId + ", ready : " + this.mImeReadyShow + ", displayId : " + this.mImeReadyShownDisplayId);
            }
            if (this.mImeSwitchToOther && displayId == (i2 = this.mImeSwitchToDisplayId)) {
                this.mImeShownDisplayId = i2;
                this.mImeSwitchToOther = false;
                changed = true;
            } else if (this.mImeReadyShow && displayId == (i = this.mImeReadyShownDisplayId)) {
                this.mImeShownDisplayId = i;
                this.mImeReadyShow = false;
                changed = true;
            } else if (this.mImeShownDisplayId != displayId) {
                this.mImeShownDisplayId = displayId;
                changed = true;
            }
        }
        if (lastshown != this.mImeShown || changed) {
            notifyInputMethodStatusChanged(false);
        }
    }

    private boolean isRealyShown() {
        return (!this.mImeShown || this.mImeSwitchToOther || this.mImeReadyShow) ? false : true;
    }

    public void updateImeReadyShown(boolean ready) {
        if (this.mImeReadyShow == ready) {
            return;
        }
        this.mImeReadyShow = ready;
        if (DEBUG) {
            VSlog.d(TAG, "ime ready to show time out, mImeReadyShow : " + this.mImeReadyShow + ", ready show displayId : " + this.mImeReadyShownDisplayId + ", then ime showing : " + this.mImeShown + ", displayId : " + this.mImeShownDisplayId);
        }
        notifyInputMethodStatusChanged(false);
    }

    private void updateImeReadyShownForDisplay(int displayId, boolean ready) {
        this.mImeReadyShownDisplayId = displayId;
        this.mImeReadyShow = ready;
        if (ready) {
            this.mRetryTimes = 0;
        }
        if (DEBUG) {
            VSlog.d(TAG, "updateImeReadyShownForDisplay ready show displayId : " + this.mImeReadyShownDisplayId + ", ready : " + ready);
        }
        notifyInputMethodStatusChanged(ready);
    }

    public int[] getInputMethodStatus() {
        int[] status = {this.mImeShown ? 1 : 0, this.mImeShownDisplayId, this.mImeReadyShow ? 1 : 0, this.mImeReadyShownDisplayId, this.mImeSwitchToOther ? 1 : 0, this.mImeSwitchToDisplayId, this.mSwitchingMethod ? 1 : 0, this.mSwitchMethodDisplayId, this.mImeLastShownDisplayId};
        return status;
    }

    public void updateImeSwitchClient(boolean switchclient) {
        if (this.mImeSwitchToOther == switchclient) {
            return;
        }
        this.mImeSwitchToOther = switchclient;
        if (DEBUG) {
            VSlog.d(TAG, "ime switch client update switch : " + this.mImeSwitchToOther + ", switch displayId : " + this.mImeSwitchToDisplayId + ", ime shown : " + this.mImeShown + ", displayId : " + this.mImeShownDisplayId);
        }
        notifyInputMethodStatusChanged(false);
    }

    private void updateImeSwitchClientForDisplay(int displayId, boolean switchclient) {
        this.mImeSwitchToOther = switchclient;
        this.mImeSwitchToDisplayId = displayId;
        if (switchclient) {
            this.mRetryTimes = 0;
        }
        if (DEBUG) {
            VSlog.d(TAG, "ime switch client update switch : " + this.mImeSwitchToOther + ", switch displayId : " + this.mImeSwitchToDisplayId + ", ime shown : " + this.mImeShown + ", displayId : " + this.mImeShownDisplayId);
        }
        notifyInputMethodStatusChanged(switchclient);
    }

    public int getRetryTimes() {
        int i = this.mRetryTimes;
        this.mRetryTimes = i + 1;
        return i;
    }

    public boolean isImeShown() {
        return this.mImeShown;
    }

    public boolean isImeReadyToShow() {
        return this.mImeReadyShow;
    }

    public boolean isImeSwitchClient() {
        return this.mImeSwitchToOther;
    }

    public void updateImeSwitching(boolean switching) {
        if (!AppShareConfig.SUPPROT_APPSHARE || this.mSwitchingMethod == switching) {
            return;
        }
        if (DEBUG) {
            VSlog.d(TAG, "updateImeSwitching switching: " + switching + ", ime shown: " + this.mImeShown + ", displayId: " + this.mImeShownDisplayId + ", cur switch displayId: " + this.mSwitchMethodDisplayId);
        }
        this.mSwitchingMethod = switching;
        if (switching && this.mImeShown) {
            this.mSwitchMethodDisplayId = this.mImeShownDisplayId;
        }
        notifyInputMethodStatusChanged(switching);
    }

    private void updateSwitchMenuShowDisplayId(int displayId) {
        this.mSwitchMethodDisplayId = displayId;
        if (DEBUG) {
            VSlog.d(TAG, "update inputmethod switch displayId: " + displayId);
        }
    }

    public int getImeShownDisplayId() {
        return this.mImeShownDisplayId;
    }

    public int getWakefulness() {
        return this.mWakefulness;
    }

    public void updateWakefulness(int wakefulness) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        this.mWakefulness = wakefulness;
    }

    public void onImeMenuDestory() {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        if (DEBUG) {
            VSlog.d(TAG, "Ime menu destoryed");
        }
        this.mAppShareHandler.sendEmptyMessageDelayed(MSG_IME_MENU_DESTORY, 1000L);
    }

    public int getImeMenuShowDisplay() {
        return this.mShowImeMenuDisplayId;
    }

    public void reportLastShownDisplayId(int displayId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        this.mImeLastShownDisplayId = displayId;
        if (DEBUG) {
            VSlog.d(TAG, "reportLastShownDisplayId displayId : " + this.mImeLastShownDisplayId);
        }
        notifyInputMethodStatusChanged(true);
    }

    public int getInputMethodDstDisplayId() {
        VSlog.d(TAG, "---------------------------------------------------------------------------------------------------------");
        VSlog.d(TAG, "HLX  mImeSwitchToOther : " + this.mImeSwitchToOther + " mImeSwitchToDisplayId :" + this.mImeSwitchToDisplayId);
        VSlog.d(TAG, "HLX  mImeReadyShow : " + this.mImeReadyShow + " mImeReadyShownDisplayId :" + this.mImeReadyShownDisplayId);
        VSlog.d(TAG, "HLX  mImeShown : " + this.mImeShown + " mImeShownDisplayId :" + this.mImeShownDisplayId);
        StringBuilder sb = new StringBuilder();
        sb.append("HLX  mImeLastShownDisplayId : ");
        sb.append(this.mImeLastShownDisplayId);
        VSlog.d(TAG, sb.toString());
        VSlog.d(TAG, "HLX  getInputMethodDstDisplayId trace: ", new Throwable());
        VSlog.d(TAG, "---------------------------------------------------------------------------------------------------------");
        if (this.mImeSwitchToOther) {
            return this.mImeSwitchToDisplayId;
        }
        if (this.mImeReadyShow) {
            return this.mImeReadyShownDisplayId;
        }
        if (this.mImeShown) {
            return this.mImeShownDisplayId;
        }
        return this.mImeLastShownDisplayId;
    }

    public boolean isInputMethodShownEvent() {
        return this.mImeSwitchToOther || this.mImeReadyShow || this.mImeShown || this.mSwitchingMethod;
    }

    public boolean isInputMethodNeedMoved() {
        return this.mImeSwitchToOther;
    }

    public int getInputMethodShowEventDisplayId() {
        if (this.mImeSwitchToOther) {
            return this.mImeSwitchToDisplayId;
        }
        if (this.mImeReadyShow) {
            return this.mImeReadyShownDisplayId;
        }
        if (this.mImeShown) {
            return this.mImeShownDisplayId;
        }
        if (this.mSwitchingMethod) {
            return this.mSwitchMethodDisplayId;
        }
        return -1;
    }

    public int getImeLastShownDisplayId() {
        return this.mImeLastShownDisplayId;
    }

    public void updateImeShowDisplayId(int displayId) {
        this.mImeShownDisplayId = displayId;
        if (DEBUG) {
            VSlog.d(TAG, "updateImeShowDisplayId displayId : " + this.mImeShownDisplayId);
        }
        notifyInputMethodStatusChanged(false);
    }

    public void updateImeLastShownDisplayId(int displayId) {
        this.mImeLastShownDisplayId = displayId;
        if (DEBUG) {
            VSlog.d(TAG, "updateImeLastShownDisplayId displayId : " + this.mImeLastShownDisplayId);
        }
    }

    public void updateLastInputMethodMove(int displayId) {
        this.mLastImeMovedDisplayId = displayId;
        if (DEBUG) {
            VSlog.d(TAG, "update inputmethod last moved: " + displayId);
        }
    }

    public int getLastInputMethodMove() {
        return this.mLastImeMovedDisplayId;
    }

    public int getAppShareDisplayFocusWindowType() {
        synchronized (this.mWMService.mGlobalLock) {
            if (!isAppsharedMode()) {
                VSlog.e(TAG, "getAppShareDisplayFocusWindowType, app share display not exists!");
                return -1;
            }
            WindowState currentFocus = getAppShareDisplayFocusedWindow();
            int type = -1;
            if (currentFocus != null) {
                type = currentFocus.mAttrs.type;
            }
            VSlog.d(TAG, "getAppShareDisplayFocusWindowType: type = " + type + ", currentFocus = " + currentFocus);
            return type;
        }
    }

    public boolean isOnAppShareDisplay(String packageName, int uid, int pid) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            synchronized (this.mWMService.mGlobalLock) {
                if (isAppsharedMode()) {
                    if (isOnAppShareDisplay(packageName, UserHandle.getUserId(uid))) {
                        return true;
                    }
                    return this.mVivoRatioControllerUtilsImpl.isImeApplication(this.mContext, pid, uid) && MultiDisplayManager.isAppShareDisplayId(this.mVivoRatioControllerUtilsImpl.getCurrentInputMethodDisplayId());
                }
                return false;
            }
        }
        return false;
    }

    public void updateMergedConfigurationForAppShareLocked(WindowState win, MergedConfiguration mergedConfiguration) {
        if (!AppShareConfig.SUPPROT_APPSHARE || win == null || win.isInputMethodWindow()) {
            return;
        }
        updateMergedConfigurationForAppShared(win, mergedConfiguration);
    }

    public boolean isNotComputeLoseFocusWindowLocked(DisplayContent displayContent, WindowState loseWin, boolean block) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return false;
        }
        if (block) {
            return isNotComputeLoseFocusWindowLocked(displayContent, loseWin);
        }
        return isNotComputeLoseFocusWindowNoneLock(displayContent, loseWin);
    }

    private boolean isNotComputeLoseFocusWindowNoneLock(DisplayContent displayContent, WindowState loseWin) {
        boolean isNotComputeLoseFocusWindowLocked;
        synchronized (this.mWMService.mGlobalLock) {
            isNotComputeLoseFocusWindowLocked = isNotComputeLoseFocusWindowLocked(displayContent, loseWin);
        }
        return isNotComputeLoseFocusWindowLocked;
    }

    private boolean isNotComputeLoseFocusWindowLocked(DisplayContent displayContent, WindowState loseWin) {
        int loseDisplayId;
        int curDisplayId;
        DisplayContent loseDisplayContent;
        if (displayContent == null || loseWin == null || (loseDisplayId = loseWin.getDisplayId()) == (curDisplayId = displayContent.getDisplayId())) {
            return false;
        }
        if ((loseDisplayId == 0 || MultiDisplayManager.isAppShareDisplayId(loseDisplayId)) && ((curDisplayId == 0 || MultiDisplayManager.isAppShareDisplayId(curDisplayId)) && (loseDisplayContent = this.mWMService.mRoot.getDisplayContent(loseDisplayId)) != null)) {
            return loseDisplayContent.mCurrentFocus == loseWin || displayContent.mCurrentFocus == loseWin;
        }
        return false;
    }

    private String getImeStatusString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ime status : {");
        sb.append("imeShown: ");
        sb.append(this.mImeShown);
        sb.append(", displayId: ");
        sb.append(this.mImeShownDisplayId);
        sb.append(", last shown displayId: ");
        sb.append(this.mImeLastShownDisplayId);
        sb.append(", readyShown: ");
        sb.append(this.mImeReadyShow);
        sb.append(", displayId: ");
        sb.append(this.mImeReadyShownDisplayId);
        sb.append(", switchToApp: ");
        sb.append(this.mImeSwitchToOther);
        sb.append(", displayId: ");
        sb.append(this.mImeSwitchToDisplayId);
        sb.append(", switch Method: ");
        sb.append(this.mSwitchingMethod);
        sb.append(", siwtch Method DisplayId: ");
        sb.append(this.mSwitchMethodDisplayId);
        sb.append("}");
        return sb.toString();
    }

    public void forbidUseHardware(int type) {
        Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_FORBID_USE_HARDWARE, type, 0).sendToTarget();
    }

    public void notifyKeepScreenOnFromPowerManager(boolean keepScreenOn) {
        Message.obtain(this.mAppShareHandler, MSG_APP_SHARE_NOTIFY_KEEP_SCREEN_ON, Boolean.valueOf(keepScreenOn)).sendToTarget();
    }

    public void notifyAppShareMoveTaskToBackFromInputManager() {
        this.mAppShareHandler.sendEmptyMessage(MSG_APP_SHARE_MOVE_TASK_TO_BACK);
    }

    public boolean isLeftButtonRegion(float x, float y) {
        if (!this.mShouldBlockInjectMotionEventIgnoreRegion) {
            return false;
        }
        if (this.mIsVideoPlayerPortrait) {
            if (x <= 0.0f || x >= 150.0f || y <= 0.0f || y >= 210.0f) {
                return false;
            }
            return true;
        } else if (x <= 0.0f || x >= 200.0f || y <= 0.0f || y >= 180.0f) {
            return false;
        } else {
            return true;
        }
    }
}