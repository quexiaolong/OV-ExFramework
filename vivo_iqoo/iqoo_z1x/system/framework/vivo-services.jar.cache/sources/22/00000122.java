package com.android.server.am.frozen;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.multidisplay.MultiDisplayManager;
import android.os.BatteryManagerInternal;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Display;
import com.android.server.IVivoStats;
import com.android.server.IVivoWorkingState;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.am.VivoFrozenPackageSupervisor;
import com.android.server.am.firewall.VivoAppIsolationController;
import com.android.server.am.frozen.WorkingStateManager;
import com.android.server.wm.VivoStatsInServerImpl;
import com.vivo.face.common.data.Constants;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.services.proxy.VivoProxyImpl;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.superresolution.Constant;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class FrozenQuicker implements Observer, WorkingStateManager.StateChangeListener, IVivoWorkingState {
    private static final String ACTION_FREEZE_ALL = "vivo.intent.action.FREEZE_ALL";
    private static final String ACTION_UNFREEZE_SNS = "vivo.intent.action.UNFREEZE_SNS";
    public static final int ALLOW_FROZEN = 0;
    public static final int AUDIO = 1;
    public static final int DELAY_CHECK_TIME = 10000;
    public static final int FREEZE_STATUS_CHECK_MS = 5000;
    public static final int FREEZE_TIMEOUT_MS = 500;
    private static final int FROZEN_STATE_MASK = 16703;
    public static final int HAVE_FROZEN = 2;
    private static final String KEY_PKG_NAME = "pkgName";
    private static final String KEY_UID = "uid";
    private static final int MSG_ID_ADD_VIRTUAL_DISPLAY = 13;
    private static final int MSG_ID_ADD_WINDOW = 18;
    private static final int MSG_ID_BLACK_LIST_CHANGE = 11;
    private static final int MSG_ID_CHECK_DOWNLOAD_STATUS = 10;
    private static final int MSG_ID_DELAY_CHECK_STATUS = 7;
    private static final int MSG_ID_DO_FROZEN = 2;
    private static final int MSG_ID_FROZEN_SUCCESS = 4;
    private static final int MSG_ID_PROC_ADD = 0;
    private static final int MSG_ID_PROC_REMOVE = 1;
    private static final int MSG_ID_PROC_STATE_CHANGE = 6;
    private static final int MSG_ID_REMOVE_VIRTUAL_DISPLAY = 14;
    private static final int MSG_ID_SCREEN_OFF_FROZEN = 8;
    private static final int MSG_ID_SCREEN_ON_FROZEN = 9;
    private static final int MSG_ID_STOP_CHECK_DOWNLOAD_STATUS = 12;
    private static final int MSG_ID_UNFREEZE_FOR_ADD_WINDOW = 19;
    private static final int MSG_ID_UNFREEZE_PERIOD_LIST_CHANGE = 16;
    private static final int MSG_ID_UNFREEZE_SNS = 17;
    private static final int MSG_ID_UNFROZEN = 5;
    private static final int MSG_ID_WHITE_LIST_CHANGE = 15;
    private static final int MSG_ID_WORKING_STATE_CHANGE = 3;
    public static final int NAVIGATION = 2;
    public static final int NOT_ALLOW_FROZEN = 1;
    public static final int ONE_MIN = 60000;
    private static final String PACKAGE_LIST_TYPE_BINDER_PROXY = "binder_proxy";
    private static final String PACKAGE_LIST_TYPE_NO_NOTI_DOWNLOAD = "no_noti_download";
    private static final String PACKAGE_LIST_TYPE_POWER_CONNECTED = "power_connected";
    private static final String PACKAGE_LIST_TYPE_UNFREEZE_PERIOD = "unfreeze_period";
    private static final String PEM_IN_SLEEP_MODE = "pem_in_sleepmode";
    public static final int RECORD = 4;
    public static final int REPORT_UNFREEZE_MSG = 4;
    private static final long SCREEN_OFF_FREEZE_ALL_DELAYED_TIME = 20000;
    private static final String SCREEN_OFF_FROZEN_ENABLE = "screen_off_frozen_enable";
    public static final int SET_FROZEN_PROCESS_MSG = 3;
    private static final String TAG = "quickfrozen";
    public static final int TWO_10S = 20000;
    private static final String UNFREEZE_REASON_ADD_WINDOW = "AddWindow";
    private static final String UNFREEZE_REASON_SCREEN_ON = "ScreenOn";
    private static final String UNFREEZE_REASON_UNFREEZE_SNS = "UnfreezeSNS";
    private static final int UP_TIME_PERCENT_THRESHOLD = 95;
    public static final int WINDOW = 8;
    private final FrozenAppRecord COMMON_KEY;
    private boolean isIncall;
    private boolean isScreenOffFrozen;
    private AlarmManager mAlarmManager;
    private final HashMap<String, FrozenAppRecord> mApps;
    private BatteryManagerInternal mBatteryManagerInternal;
    private boolean mBootPhasePass;
    private long mBootTime;
    private Context mContext;
    private String mCurrentInputMethod;
    private DisplayManager mDisplayManager;
    private long mElapsedRealtime;
    private PendingIntent mFreezeAllIntent;
    private final FrozenHandler mFrozenHandler;
    private int mHomePid;
    private FrozenAppRecord mLastFrozen;
    private Looper mLooper;
    private NotificationManager mNotificationManager;
    private PackageManager mPackageManager;
    private final SparseArray<PendingIntent> mPendingIntents;
    private PowerManager mPowerManager;
    private final SparseArray<ProcessInfo> mProcs;
    private int mScreenState;
    private long mUpTimeMillis;
    private VivoFrozenPackageSupervisor mVfps;
    private IVivoStats mVivoStats;
    private Map<Integer, String> mWaitingFrozenForegroundApp;
    private PowerManager.WakeLock mWakeLock;
    private ComponentName mWallPaper;
    private WorkingStateManager mWms;
    public static final HashSet<String> UNFREEZE_APP_LIST = new HashSet<String>() { // from class: com.android.server.am.frozen.FrozenQuicker.1
        {
            add("com.vivo.abe");
            add("com.vivo.epm");
            add("com.vivo.assistant");
            add("com.vivo.gamewatch");
            add("com.vivo.aiengine");
            add("com.vivo.pushservice");
            add("com.android.smspush");
            add("com.vivo.hiboard");
            add("com.vivo.aiservice");
            add("com.vivo.weather.provider");
            add("android.ext.services");
            add(VivoAppIsolationController.NOTIFY_IQOO_SECURE_PACKAGE);
            add("com.vivo.safecenter");
            add("com.vivo.systemblur.server");
            add("com.vivo.deformer");
            add("com.vivo.globalsearch");
            add("com.vivo.vivo3rdalgoservice");
            add("com.google.android.gsf");
            add("com.vivo.hybrid");
            add("com.bbk.theme");
            add("com.vivo.magazine");
            add("com.android.providers.media.module");
            add("com.baidu.map.location");
            add(FaceUIState.PKG_SYSTEMUI);
            add("com.vivo.doubletimezoneclock");
            add("com.bbk.launcher2");
            add("com.google.android.gms");
            add("com.google.process.gservices");
            add("com.google.android.gms.persistent");
            add("com.android.providers.media");
            add("com.android.providers.downloads");
            add("com.vivo.space");
            add("com.sy.ydcs.vivo");
            add("com.tencent.tmgp.p2y9y.xuanlongji.vivo");
            add("com.vivo.sdkplugin");
            add(Constant.APP_WEIXIN);
            add("com.vivo.livewallpaper.behavior");
            add("com.android.providers.calendar");
            add("com.android.providers.contacts");
        }
    };
    public static final HashSet<String> mBlackListFromPem = new HashSet<>(UNFREEZE_APP_LIST);
    public static final HashSet<String> mWhiteListFromPem = new HashSet<>();
    private static final HashMap<String, double[]> mUnfreezePeriodMap = new HashMap<>();
    private static final HashSet<String> sNoNotiDownloadAppList = new HashSet<>();
    private static final boolean DEBUG = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
    private static final boolean FROZEN_DISABLE = SystemProperties.get("persist.vivo.frozen.disable", "no").equals("yes");
    public static boolean isFeatureSupport = false;
    public static boolean isBlackListFromPem = false;
    public static boolean isQuickFrozenPause = false;
    public static boolean isWhiteListFromPem = false;
    private static boolean screenOffFrozenEnable = true;

    private FrozenQuicker() {
        this.COMMON_KEY = new FrozenAppRecord(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, 0);
        this.mProcs = new SparseArray<>(128);
        this.mApps = new HashMap<>();
        this.isScreenOffFrozen = false;
        this.mScreenState = 2;
        this.mBootPhasePass = false;
        this.mNotificationManager = null;
        this.mDisplayManager = null;
        this.mPackageManager = null;
        this.mPowerManager = null;
        this.mAlarmManager = null;
        this.mPendingIntents = new SparseArray<>();
        this.mVivoStats = null;
        this.mWaitingFrozenForegroundApp = null;
        this.mWakeLock = null;
        ServiceThread thread = new ServiceThread("quick_frozen", 0, false);
        thread.start();
        this.mLooper = thread.getLooper();
        this.mFrozenHandler = new FrozenHandler(this.mLooper);
        this.mVfps = VivoFrozenPackageSupervisor.getInstance();
        WorkingStateManager workingStateManager = WorkingStateManager.getInstance();
        this.mWms = workingStateManager;
        workingStateManager.registerListener(this);
        this.mBootTime = SystemClock.elapsedRealtime();
        this.mBootPhasePass = false;
        this.mVivoStats = VivoStatsInServerImpl.getInstance();
        VSlog.e(TAG, "FrozenQuicker create", new Throwable("debug FrozenQuicker"));
    }

    public static FrozenQuicker getInstance() {
        return Instance.INSTANCE;
    }

    public static Looper getLooper() {
        return getInstance().mLooper;
    }

    public void initialize(Context context) {
        this.mContext = context;
        this.mVfps.addObserver(this);
    }

    public void systemReady() {
        registerDisplayListener();
        registerContentObserver();
        registerReceiver();
    }

    private void registerContentObserver() {
        try {
            boolean screenOffFrozenEnable2 = getScreenOffFrozenEnable();
            screenOffFrozenEnable = screenOffFrozenEnable2;
            this.mVfps.setFreezeForegroundAppEnable(screenOffFrozenEnable2);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(SCREEN_OFF_FROZEN_ENABLE), false, new SettingsObserver(this.mFrozenHandler));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            boolean enable = FrozenQuicker.this.getScreenOffFrozenEnable();
            VSlog.i(FrozenQuicker.TAG, "onChange, enable = " + enable + ", mScreenOffFrozenEnable = " + FrozenQuicker.screenOffFrozenEnable);
            if (enable != FrozenQuicker.screenOffFrozenEnable) {
                boolean unused = FrozenQuicker.screenOffFrozenEnable = enable;
                FrozenQuicker.this.mVfps.setFreezeForegroundAppEnable(FrozenQuicker.screenOffFrozenEnable);
                if (!enable) {
                    if (FrozenQuicker.this.mScreenState == 1 || Display.isDozeState(FrozenQuicker.this.mScreenState)) {
                        FrozenQuicker.this.unfreezeInputMethod();
                        FrozenQuicker.this.unfreezeWallpaper();
                        FrozenQuicker.this.unfreezeForegroundAppLocked();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean getScreenOffFrozenEnable() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), SCREEN_OFF_FROZEN_ENABLE, 1) > 0;
    }

    private void registerReceiver() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_UNFREEZE_SNS);
            intentFilter.addAction(ACTION_FREEZE_ALL);
            this.mContext.registerReceiver(new UnfreezeReceiver(), intentFilter, null, this.mFrozenHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class UnfreezeReceiver extends BroadcastReceiver {
        private UnfreezeReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            VSlog.i(FrozenQuicker.TAG, "onReceive: action = " + action);
            if (FrozenQuicker.ACTION_UNFREEZE_SNS.equals(action)) {
                FrozenQuicker.this.handleUnfreezeSNS(intent);
            } else if (FrozenQuicker.ACTION_FREEZE_ALL.equals(action)) {
                FrozenQuicker.this.handleFreezeAll();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUnfreezeSNS(Intent intent) {
        String pkgName = intent.getStringExtra(KEY_PKG_NAME);
        int uid = intent.getIntExtra(KEY_UID, -1);
        VSlog.i(TAG, "handleUnfreezeSNS: pkgName = " + pkgName + ", uid = " + uid);
        FrozenHandler frozenHandler = this.mFrozenHandler;
        frozenHandler.sendMessageDelayed(Message.obtain(frozenHandler, 17, uid, 0, pkgName), isInteractive() ? 0L : 3000L);
    }

    private boolean isInteractive() {
        ensurePowerManagerNonNull();
        PowerManager powerManager = this.mPowerManager;
        return powerManager != null && powerManager.isInteractive();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleFreezeAll() {
        long elapsedRealtimeDuringScreenOff = SystemClock.elapsedRealtime() - this.mElapsedRealtime;
        if (elapsedRealtimeDuringScreenOff == 0) {
            elapsedRealtimeDuringScreenOff = 20000;
        }
        long upTimeMillisDuringScreenOff = SystemClock.uptimeMillis() - this.mUpTimeMillis;
        double percent = (upTimeMillisDuringScreenOff * 100.0d) / elapsedRealtimeDuringScreenOff;
        VSlog.i(TAG, "elapsedRealtimeDuringScreenOff = " + elapsedRealtimeDuringScreenOff + ", upTimeMillisDuringScreenOff = " + upTimeMillisDuringScreenOff + ", percent = " + new DecimalFormat("#.00").format(percent));
        boolean freezeAll = true;
        if (percent >= 95.0d) {
            freezeAll = false;
        }
        acquireWakeLock();
        HashSet<FrozenAppRecord> records = new HashSet<>();
        synchronized (this) {
            records.addAll(this.mApps.values());
        }
        Iterator<FrozenAppRecord> it = records.iterator();
        while (it.hasNext()) {
            FrozenAppRecord appRecord = it.next();
            if (!appRecord.frozen && (freezeAll || (!isPerceptible(appRecord) && !inNoNotiDownloadAppList(appRecord.pkgName)))) {
                appRecord.setWorkingState(0, 8);
                appRecord.stopCheckDownloadStatus();
                this.mFrozenHandler.removeMessages(2, appRecord);
                FrozenHandler frozenHandler = this.mFrozenHandler;
                frozenHandler.sendMessage(frozenHandler.obtainMessage(2, appRecord));
            }
        }
    }

    private void ensurePowerManagerNonNull() {
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        }
    }

    private void acquireWakeLock() {
        ensurePowerManagerNonNull();
        PowerManager powerManager = this.mPowerManager;
        if (powerManager != null && this.mWakeLock == null) {
            this.mWakeLock = powerManager.newWakeLock(1, TAG);
        }
        PowerManager.WakeLock wakeLock = this.mWakeLock;
        if (wakeLock != null && !wakeLock.isHeld()) {
            this.mWakeLock.acquire(500L);
        }
    }

    private void releaseWakeLock() {
        PowerManager.WakeLock wakeLock = this.mWakeLock;
        if (wakeLock != null && wakeLock.isHeld()) {
            this.mWakeLock.release();
        }
    }

    @Override // java.util.Observer
    public void update(Observable observable, Object data) {
        if (data instanceof FrozenDataInfo) {
            FrozenDataInfo notifyData = (FrozenDataInfo) data;
            if (notifyData.state == 1) {
                if (DEBUG) {
                    VSlog.d(TAG, "FROZEN: " + notifyData.toString());
                }
                synchronized (this) {
                    FrozenAppRecord appRecord = this.mApps.get(createKey(notifyData.uid, notifyData.pkgName));
                    if (appRecord != null) {
                        appRecord.caller = notifyData.caller;
                        reportFrozenSuccess(notifyData.pkgName, notifyData.uid);
                        return;
                    }
                    return;
                }
            }
            synchronized (this) {
                FrozenAppRecord appRecord2 = this.mApps.get(createKey(notifyData.uid, notifyData.pkgName));
                if (appRecord2 != null) {
                    appRecord2.caller = notifyData.caller;
                    appRecord2.unfreezeReason = notifyData.unfrozenReason;
                    VSlog.d(TAG, "unfreeze: " + notifyData.toString());
                    reportAppUnfreeze(notifyData.pkgName, notifyData.uid);
                }
            }
        }
    }

    @Override // com.android.server.am.frozen.WorkingStateManager.StateChangeListener
    public void onStateChanged(int model, int state, int uid) {
        if (!isFeatureSupport || (model & FROZEN_STATE_MASK) == 0 || uid < 10000) {
            return;
        }
        synchronized (this) {
            int count = 0;
            for (Map.Entry<String, FrozenAppRecord> map : this.mApps.entrySet()) {
                FrozenAppRecord value = map.getValue();
                if (value.uid == uid) {
                    if (!value.isSystemApp && ((!isBlackListFromPem || !value.isInPemBlackList) && (!isWhiteListFromPem || value.isInPemWhiteList))) {
                        value.setWorkingState(state == 1 ? model : 0, model);
                        count++;
                        VSlog.d(TAG, "onStateChanged 3: uid " + uid + " model:" + FrozenAppRecord.STATES_NAMES.get(model) + " state:" + FrozenAppRecord.ONOFF_NAMES.get(state) + " count= " + count + " app:" + value.pkgName);
                        this.mFrozenHandler.removeMessages(3, value);
                        this.mFrozenHandler.sendMessageDelayed(this.mFrozenHandler.obtainMessage(3, value), 5000L);
                    }
                    return;
                }
            }
        }
    }

    public boolean isPerceptible(int uid) {
        String pkgName = null;
        synchronized (this) {
            Iterator<FrozenAppRecord> it = this.mApps.values().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                FrozenAppRecord appRecord = it.next();
                if (appRecord.uid == uid) {
                    if (appRecord.hasFgService()) {
                        return true;
                    }
                    pkgName = appRecord.pkgName;
                }
            }
            if (!TextUtils.isEmpty(pkgName)) {
                return hasOngoingNotification(pkgName, uid);
            }
            return false;
        }
    }

    public boolean isPerceptible(FrozenAppRecord appRecord) {
        if (appRecord == null) {
            return false;
        }
        if (!appRecord.hasFgService() && !hasOngoingNotification(appRecord.pkgName, appRecord.uid)) {
            return false;
        }
        return true;
    }

    public boolean hasOngoingNotification(String pkgName, int uid) {
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        }
        NotificationManager notificationManager = this.mNotificationManager;
        if (notificationManager != null) {
            return notificationManager.hasOngoingNotification(pkgName, uid);
        }
        return false;
    }

    @Override // com.android.server.am.frozen.WorkingStateManager.StateChangeListener
    public void onStateChanged(int model, int state, int uid, int pid) {
        ProcessInfo proc;
        if (!isFeatureSupport) {
            return;
        }
        if ((model != 16384 && ((model & FROZEN_STATE_MASK) == 0 || uid < 10000)) || pid <= 0) {
            return;
        }
        synchronized (this.mProcs) {
            proc = this.mProcs.get(pid);
        }
        if (proc == null || proc.mRecord == null || proc.mRecord.isSystemApp) {
            return;
        }
        if (isBlackListFromPem && proc.mRecord.isInPemBlackList) {
            return;
        }
        if (isWhiteListFromPem && !proc.mRecord.isInPemWhiteList) {
            return;
        }
        if (DEBUG) {
            VSlog.d(TAG, "onStateChanged : uid " + uid + " model:" + FrozenAppRecord.STATES_NAMES.get(model) + " state: " + FrozenAppRecord.ONOFF_NAMES.get(state) + " pid:" + pid + " proc:" + proc.mProcName);
        }
        proc.mRecord.setWorkingState(state == 1 ? model : 0, model);
        this.mFrozenHandler.removeMessages(3, proc.mRecord);
        FrozenHandler frozenHandler = this.mFrozenHandler;
        frozenHandler.sendMessageDelayed(frozenHandler.obtainMessage(3, proc.mRecord), 5000L);
    }

    @Override // com.android.server.am.frozen.WorkingStateManager.StateChangeListener
    public void onStateChanged(int model, int state, int uid, String pkgName) {
        if (!isFeatureSupport || (model & FROZEN_STATE_MASK) == 0 || uid < 10000) {
            return;
        }
        if (DEBUG) {
            VSlog.d(TAG, "onStateChanged  : pkgName " + pkgName + " uid:" + uid + " model:" + FrozenAppRecord.STATES_NAMES.get(model) + " state:" + FrozenAppRecord.ONOFF_NAMES.get(state));
        }
        if (!isPemAllowFrozen(pkgName)) {
            return;
        }
        synchronized (this) {
            FrozenAppRecord appRecord = this.mApps.get(createKey(uid, pkgName));
            if (appRecord == null) {
                VSlog.d(TAG, "onStateChanged not found app error :" + pkgName + uid + " model:" + model + " state:" + FrozenAppRecord.ONOFF_NAMES.get(state));
            } else if (appRecord.isSystemApp) {
            } else {
                if (isBlackListFromPem && appRecord.isInPemBlackList) {
                    return;
                }
                if (isWhiteListFromPem && !appRecord.isInPemWhiteList) {
                    return;
                }
                appRecord.setWorkingState(state == 1 ? model : 0, model);
                this.mFrozenHandler.removeMessages(3, appRecord);
                FrozenHandler frozenHandler = this.mFrozenHandler;
                frozenHandler.sendMessageDelayed(frozenHandler.obtainMessage(3, appRecord), 5000L);
            }
        }
    }

    public void updateInputMethod(String inputMethod) {
        synchronized (mBlackListFromPem) {
            if (this.mCurrentInputMethod != null) {
                mBlackListFromPem.remove(this.mCurrentInputMethod);
            }
            mBlackListFromPem.add(inputMethod);
            this.mCurrentInputMethod = inputMethod;
        }
    }

    public boolean isCurrentInputMethod(String pkgName) {
        return pkgName != null && pkgName.equals(this.mCurrentInputMethod);
    }

    private boolean isPemAllowFrozen(String pkgName) {
        if (isWhiteListFromPem || isBlackListFromPem) {
            if (isBlackListFromPem) {
                synchronized (mBlackListFromPem) {
                    return !mBlackListFromPem.contains(pkgName);
                }
            } else if (isWhiteListFromPem) {
                synchronized (mWhiteListFromPem) {
                    return mWhiteListFromPem.contains(pkgName);
                }
            } else {
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DisplayListenerImpl implements DisplayManager.DisplayListener {
        private DisplayListenerImpl() {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            Display display;
            int state;
            if (FrozenQuicker.DEBUG) {
                VSlog.i(FrozenQuicker.TAG, "onDisplayChanged, displayId = " + displayId);
            }
            if (displayId == 0 && (display = FrozenQuicker.this.mDisplayManager.getDisplay(displayId)) != null && FrozenQuicker.this.mScreenState != (state = display.getState())) {
                FrozenQuicker frozenQuicker = FrozenQuicker.this;
                frozenQuicker.noteScreenState(state, frozenQuicker.mScreenState);
                FrozenQuicker.this.mScreenState = state;
            }
        }
    }

    private void registerDisplayListener() {
        try {
            if (this.mDisplayManager == null) {
                this.mDisplayManager = (DisplayManager) this.mContext.getSystemService(DisplayManager.class);
            }
            this.mDisplayManager.registerDisplayListener(new DisplayListenerImpl(), this.mFrozenHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void noteScreenState(int newState, int oldState) {
        if (!isFeatureSupport || !screenOffFrozenEnable) {
            return;
        }
        if (DEBUG) {
            VSlog.d(TAG, "noteScreenState : newState " + Display.stateToString(newState) + ", oldState = " + Display.stateToString(oldState));
        }
        if (oldState == 2 && newState == 1) {
            this.mFrozenHandler.removeMessages(9);
            this.mFrozenHandler.removeMessages(8);
            this.mFrozenHandler.removeMessages(19);
            FrozenHandler frozenHandler = this.mFrozenHandler;
            frozenHandler.sendMessageDelayed(frozenHandler.obtainMessage(8), 5000L);
            setFreezeAllAlarm();
        } else if (newState == 2) {
            this.mFrozenHandler.removeMessages(9);
            this.mFrozenHandler.removeMessages(8);
            FrozenHandler frozenHandler2 = this.mFrozenHandler;
            frozenHandler2.sendMessage(frozenHandler2.obtainMessage(9));
            cancelFreezeAllAlarm();
        }
    }

    private void setFreezeAllAlarm() {
        ensureAlarmManagerNonNull();
        cancelFreezeAllAlarm();
        if (this.mAlarmManager != null) {
            Intent intent = new Intent(ACTION_FREEZE_ALL);
            this.mFreezeAllIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
            this.mAlarmManager.setExactAndAllowWhileIdle(0, System.currentTimeMillis() + 20000, this.mFreezeAllIntent);
        }
        this.mElapsedRealtime = SystemClock.elapsedRealtime();
        this.mUpTimeMillis = SystemClock.uptimeMillis();
    }

    private void cancelFreezeAllAlarm() {
        PendingIntent pendingIntent;
        AlarmManager alarmManager = this.mAlarmManager;
        if (alarmManager != null && (pendingIntent = this.mFreezeAllIntent) != null) {
            alarmManager.cancel(pendingIntent);
            this.mFreezeAllIntent = null;
        }
    }

    public void reportWallPaperService(ComponentName wallpaper) {
        synchronized (mBlackListFromPem) {
            if (this.mWallPaper != null) {
                mBlackListFromPem.remove(this.mWallPaper.getPackageName());
            }
            mBlackListFromPem.add(wallpaper.getPackageName());
            this.mWallPaper = wallpaper;
        }
    }

    public boolean isWallpaperService(String pkgName) {
        ComponentName currentWallpaper = this.mWallPaper;
        return (pkgName == null || currentWallpaper == null || !pkgName.equals(currentWallpaper.getPackageName())) ? false : true;
    }

    public void addProcess(ProcessInfo proc) {
        if (proc.mUid < 10000 || proc.mPkgName.startsWith("com.vivo") || proc.mPkgName.startsWith("com.bbk") || proc.isSystemApp()) {
            return;
        }
        Message msg = this.mFrozenHandler.obtainMessage(0, proc);
        this.mFrozenHandler.sendMessage(msg);
    }

    public void addWindow(String pkgName, int uid) {
        if (DEBUG) {
            VSlog.i(TAG, "addWindow: pkgName = " + pkgName + ", uid = " + uid);
        }
        if (TextUtils.isEmpty(pkgName) || !UserHandle.isApp(uid)) {
            return;
        }
        FrozenHandler frozenHandler = this.mFrozenHandler;
        frozenHandler.sendMessage(Message.obtain(frozenHandler, 18, uid, 0, pkgName));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAddWindowLocked(String pkgName, int uid) {
        FrozenAppRecord record = this.mApps.get(createKey(uid, pkgName));
        if (record == null || !record.isAppInFrozenList() || record.isSystemApp) {
            return;
        }
        this.mFrozenHandler.removeMessages(19, record);
        FrozenHandler frozenHandler = this.mFrozenHandler;
        frozenHandler.sendMessageDelayed(Message.obtain(frozenHandler, 19, record), 5000L);
    }

    public void setQuickFrozenEnable(boolean frozenEnable, long threshold) {
        VSlog.d(TAG, "setQuickFrozenEnable: frozenEnable " + frozenEnable + " threshold " + threshold);
        if (isFeatureSupport != frozenEnable) {
            if (!frozenEnable) {
                synchronized (this) {
                    for (Map.Entry<String, FrozenAppRecord> map : this.mApps.entrySet()) {
                        FrozenAppRecord value = map.getValue();
                        value.removeStateChangedListener();
                    }
                    this.mApps.clear();
                }
                synchronized (this.mProcs) {
                    this.mProcs.clear();
                }
            }
            isFeatureSupport = frozenEnable;
        }
        WorkingStateManager workingStateManager = this.mWms;
        if (workingStateManager != null) {
            workingStateManager.setNetflowThreshold(threshold);
        }
    }

    public void notifyQuickFrozenPause(boolean pause) {
        isQuickFrozenPause = pause;
        VSlog.d(TAG, "notifyQuickFrozenPause: isQuickFrozenPause " + pause);
    }

    public long getBeginTime(int uid, String pkgName) {
        if (isFeatureSupport) {
            FrozenAppRecord frozenAppRecord = this.mLastFrozen;
            if (frozenAppRecord != null && frozenAppRecord.uid == uid && this.mLastFrozen.pkgName != null && this.mLastFrozen.pkgName.equals(pkgName)) {
                return this.mLastFrozen.enterBgTime;
            }
            synchronized (this) {
                FrozenAppRecord record = this.mApps.get(createKey(uid, pkgName));
                if (record == null) {
                    return 0L;
                }
                return record.enterBgTime;
            }
        }
        return 0L;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addAppLocked(ProcessInfo pi) {
        synchronized (this.mProcs) {
            this.mProcs.put(pi.mPid, pi);
        }
        if (UserHandle.isIsolated(pi.mUid)) {
            for (Map.Entry<String, FrozenAppRecord> map : this.mApps.entrySet()) {
                FrozenAppRecord value = map.getValue();
                if (value.pkgName.equals(pi.mPkgName) && UserHandle.getUserId(value.uid) == UserHandle.getUserId(pi.mUid)) {
                    value.addProc(pi);
                    pi.mRecord = value;
                    VSlog.d(TAG, "isIsolated: found " + pi.mProcName + " uid " + pi.mUid);
                    return;
                }
            }
            return;
        }
        if (DEBUG) {
            VSlog.d(TAG, "addAppLocked: " + pi.mProcName);
        }
        String key = createKey(pi.mUid, pi.mPkgName);
        FrozenAppRecord appRecord = this.mApps.get(key);
        if (appRecord == null) {
            appRecord = new FrozenAppRecord(pi.mPkgName, pi.mUid);
            this.mApps.put(key, appRecord);
            setPeriodsIfNeeded(appRecord);
            this.mWms.uidRunning(pi.mUid, pi.mPkgName);
            if (isBlackListFromPem && isInBlackList(appRecord.pkgName)) {
                appRecord.isInPemBlackList = true;
            } else if (isWhiteListFromPem && isInWhiteList(appRecord.pkgName)) {
                appRecord.isInPemWhiteList = true;
            }
        }
        pi.mRecord = appRecord;
        appRecord.addProc(pi);
        this.mFrozenHandler.removeMessages(7, appRecord);
        long delayMillis = 5000;
        delayMillis = (isInteractive() || isPlugged()) ? 10000L : 10000L;
        FrozenHandler frozenHandler = this.mFrozenHandler;
        frozenHandler.sendMessageDelayed(frozenHandler.obtainMessage(7, appRecord), delayMillis);
    }

    public void removeProcess(ProcessInfo proc) {
        if (proc.mRecord == null || proc.mUid < 10000 || proc.mPkgName.startsWith("com.vivo") || proc.mPkgName.startsWith("com.bbk")) {
            return;
        }
        synchronized (this.mProcs) {
            this.mProcs.remove(proc.mPid);
        }
        FrozenHandler frozenHandler = this.mFrozenHandler;
        frozenHandler.sendMessage(frozenHandler.obtainMessage(1, proc));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeAppLocked(ProcessInfo pi) {
        FrozenAppRecord appRecord = pi.mRecord;
        if (appRecord != null) {
            appRecord.removeProc(pi);
            if (appRecord.isEmpty()) {
                this.mApps.remove(appRecord.mKey);
                this.mWms.uidStopped(appRecord.uid);
                this.mFrozenHandler.removeMessages(7, appRecord);
                return;
            }
            this.mFrozenHandler.removeMessages(7, appRecord);
            FrozenHandler frozenHandler = this.mFrozenHandler;
            frozenHandler.sendMessageDelayed(frozenHandler.obtainMessage(7, appRecord), 10000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void frozenApplication(FrozenAppRecord appRecord) {
        if (!isFeatureSupport) {
            return;
        }
        if ((!isBlackListFromPem && !isWhiteListFromPem) || !this.mVfps.isEnableFunction() || isQuickFrozenPause || FROZEN_DISABLE) {
            return;
        }
        if (checkIsBootPhase()) {
            Message msg = this.mFrozenHandler.obtainMessage(6, appRecord);
            this.mFrozenHandler.sendMessageDelayed(msg, 60000L);
        } else if (isBlackListFromPem && appRecord.isInPemBlackList) {
            if (DEBUG) {
                VSlog.d(TAG, "frozenAppLocked: return because in blacklist " + appRecord.pkgName);
            }
        } else if (isWhiteListFromPem && !appRecord.isInPemWhiteList) {
            if (DEBUG) {
                VSlog.d(TAG, "frozenAppLocked: return because not  in whitelist " + appRecord.pkgName);
            }
        } else if (appRecord.isSystemApp) {
            if (DEBUG) {
                VSlog.d(TAG, "frozenAppLocked: return because in isSystemApp " + appRecord.pkgName);
            }
        } else if (appRecord.isCheckDownload) {
            if (DEBUG) {
                VSlog.d(TAG, "isCheckDownload return " + appRecord.pkgName + " " + appRecord.allowFreeze + " " + appRecord.mWorkingState + " " + appRecord.frozen);
            }
        } else {
            appRecord.computeFreezeStatus();
            if (DEBUG) {
                VSlog.d(TAG, "frozenAppLocked: " + appRecord.pkgName + " " + appRecord.allowFreeze + " " + appRecord.mWorkingState + " " + appRecord.frozen);
            }
            if (appRecord.isAllowFrozen()) {
                this.mVfps.requestFrozen(appRecord.uid, appRecord.pkgName, true, 2, appRecord.getProcessList());
                appRecord.requestFrozenTime = System.currentTimeMillis();
                appRecord.retryFznCnt++;
                this.mLastFrozen = appRecord;
                if (DEBUG) {
                    VSlog.d(TAG, "requestFrozen :" + appRecord.pkgName);
                }
            }
        }
    }

    private boolean checkIsBootPhase() {
        if (!this.mBootPhasePass) {
            if (this.mBootTime + 60000 > SystemClock.elapsedRealtime()) {
                return true;
            }
            VSlog.d(TAG, "bootPhase Pass");
            this.mBootPhasePass = true;
            return false;
        }
        return false;
    }

    public HashSet<String> getDefaultBackList() {
        HashSet<String> list = new HashSet<>();
        list.addAll(UNFREEZE_APP_LIST);
        return list;
    }

    public boolean isInBlackList(String packageName) {
        boolean contains;
        synchronized (mBlackListFromPem) {
            contains = mBlackListFromPem.contains(packageName);
        }
        return contains;
    }

    public boolean isInWhiteList(String packageName) {
        boolean contains;
        synchronized (mWhiteListFromPem) {
            contains = mWhiteListFromPem.contains(packageName);
        }
        return contains;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBlackInformationLocked() {
        for (Map.Entry<String, FrozenAppRecord> map : this.mApps.entrySet()) {
            FrozenAppRecord value = map.getValue();
            if (mBlackListFromPem.contains(value.pkgName)) {
                value.isInPemBlackList = true;
                if (value.frozen && value.caller == 2 && !isCurrentInputMethod(value.pkgName)) {
                    this.mVfps.isKeepFrozen(value.pkgName, value.uid, TAG, 1000, 0, true, "changeblacklist");
                }
            } else {
                value.isInPemBlackList = false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateWhiteListInformationLocked() {
        for (Map.Entry<String, FrozenAppRecord> map : this.mApps.entrySet()) {
            FrozenAppRecord value = map.getValue();
            if (mWhiteListFromPem.contains(value.pkgName)) {
                value.isInPemWhiteList = true;
            } else {
                if (value.frozen && value.caller == 2 && !isCurrentInputMethod(value.pkgName)) {
                    this.mVfps.isKeepFrozen(value.pkgName, value.uid, TAG, 1000, 0, true, "changewhitelist");
                }
                value.isInPemWhiteList = false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateUnfreezePeriodListLocked() {
        Iterator<Map.Entry<String, FrozenAppRecord>> it = this.mApps.entrySet().iterator();
        if (it != null) {
            while (it.hasNext()) {
                Map.Entry<String, FrozenAppRecord> entry = it.next();
                if (entry != null) {
                    setPeriodsIfNeeded(entry.getValue());
                }
            }
        }
    }

    private void setPeriodsIfNeeded(FrozenAppRecord appRecord) {
        if (appRecord == null) {
            return;
        }
        synchronized (mUnfreezePeriodMap) {
            if (mUnfreezePeriodMap.containsKey(appRecord.pkgName)) {
                appRecord.setPeriods(mUnfreezePeriodMap.get(appRecord.pkgName));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void appFrozenSuccessLocked(int uid, String pkgName) {
        FrozenAppRecord appRecord;
        if (UserHandle.isApp(uid) && (appRecord = this.mApps.get(createKey(uid, pkgName))) != null) {
            appRecord.setFrozenInfo();
            setUnfreezeAlarm(appRecord);
        }
    }

    private void setUnfreezeAlarm(FrozenAppRecord appRecord) {
        if (!isInUnfreezePeriodList(appRecord.pkgName)) {
            return;
        }
        if (!checkCaller(appRecord.caller)) {
            VSlog.i(TAG, "caller is " + FrozenDataInfo.convertCaller(appRecord.caller));
        } else if (!checkState()) {
            VSlog.i(TAG, "in sleep mode.");
        } else {
            ensureAlarmManagerNonNull();
            if (this.mAlarmManager != null) {
                long triggerAtMillis = System.currentTimeMillis() + appRecord.getNextPeriod();
                this.mAlarmManager.setExact(1, triggerAtMillis, getPendingIntent(appRecord.pkgName, appRecord.uid));
            }
        }
    }

    private boolean checkCaller(int caller) {
        return caller == 2 || caller == 0;
    }

    private boolean checkState() {
        return Settings.System.getInt(this.mContext.getContentResolver(), PEM_IN_SLEEP_MODE, 0) == 0;
    }

    private void ensureAlarmManagerNonNull() {
        if (this.mAlarmManager == null) {
            this.mAlarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        }
    }

    private PendingIntent getPendingIntent(String pkgName, int uid) {
        Intent intent = new Intent(ACTION_UNFREEZE_SNS).putExtra(KEY_PKG_NAME, pkgName).putExtra(KEY_UID, uid);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, uid, intent, 0);
        this.mPendingIntents.put(uid, pendingIntent);
        return pendingIntent;
    }

    public void reportAppUnfreeze(String pkgName, int uid) {
        if (!UserHandle.isApp(uid)) {
            return;
        }
        FrozenHandler frozenHandler = this.mFrozenHandler;
        frozenHandler.sendMessage(frozenHandler.obtainMessage(5, this.COMMON_KEY.obtain(pkgName, uid)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unFreezeAppLocked(int uid, String pkgName) {
        FrozenAppRecord appRecord;
        if (UserHandle.isApp(uid) && (appRecord = this.mApps.get(createKey(uid, pkgName))) != null) {
            appRecord.setUnfreezeInfo();
            if (isAllowFreezeStatus(appRecord)) {
                this.mFrozenHandler.removeMessages(6, appRecord);
                long delayMillis = 0;
                delayMillis = (isInteractive() || isPlugged() || isInUnfreezePeriodList(appRecord.pkgName)) ? 5000L : 5000L;
                FrozenHandler frozenHandler = this.mFrozenHandler;
                frozenHandler.sendMessageDelayed(frozenHandler.obtainMessage(6, 0, 0, appRecord), delayMillis);
            }
            removeUnfreezeAlarm(appRecord);
        }
    }

    private boolean isPlugged() {
        if (this.mBatteryManagerInternal == null) {
            this.mBatteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
        }
        return this.mBatteryManagerInternal.getPlugType() != 0;
    }

    private void removeUnfreezeAlarm(FrozenAppRecord appRecord) {
        PendingIntent pendingIntent = this.mPendingIntents.get(appRecord.uid);
        if (pendingIntent == null) {
            return;
        }
        this.mPendingIntents.remove(appRecord.uid);
        ensureAlarmManagerNonNull();
        AlarmManager alarmManager = this.mAlarmManager;
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private boolean isAllowFreezeStatus(FrozenAppRecord appRecord) {
        if (appRecord.allowFreeze && appRecord.mWorkingState == 0 && appRecord.virtualDisplaySize == 0) {
            return true;
        }
        return false;
    }

    public void setHomeProcess(int pid) {
        this.mHomePid = pid;
    }

    public void setPhoneState(int state) {
        this.isIncall = state == 1;
    }

    public void addVirtualDisplay(String pkgName, int uid) {
        FrozenAppRecord appRecord = new FrozenAppRecord(pkgName, uid);
        FrozenHandler frozenHandler = this.mFrozenHandler;
        frozenHandler.sendMessage(frozenHandler.obtainMessage(13, 0, 0, appRecord));
        if (DEBUG) {
            VSlog.d(TAG, "addVirtualDisplay :" + pkgName);
        }
    }

    public void removeVirtualDisplay(String pkgName, int uid) {
        FrozenAppRecord appRecord = new FrozenAppRecord(pkgName, uid);
        FrozenHandler frozenHandler = this.mFrozenHandler;
        frozenHandler.sendMessage(frozenHandler.obtainMessage(14, 0, 0, appRecord));
        if (DEBUG) {
            VSlog.d(TAG, "removeVirtualDisplay :" + pkgName);
        }
    }

    public void addVirtualDisplayLocked(FrozenAppRecord app) {
        FrozenAppRecord appRecord = this.mApps.get(createKey(app.uid, app.pkgName));
        if (appRecord != null) {
            appRecord.virtualDisplaySize++;
            if (DEBUG) {
                VSlog.d(TAG, "addVirtualDisplayLocked :" + appRecord.pkgName + " size " + appRecord.virtualDisplaySize);
            }
            if (appRecord.virtualDisplaySize == 1) {
                appRecord.setWorkingState(Dataspace.STANDARD_BT709, Dataspace.STANDARD_BT709);
            }
        }
    }

    public void removeVirtualDisplayLocked(FrozenAppRecord app) {
        FrozenAppRecord appRecord = this.mApps.get(createKey(app.uid, app.pkgName));
        if (appRecord != null && appRecord.virtualDisplaySize > 0) {
            appRecord.virtualDisplaySize--;
            if (DEBUG) {
                VSlog.d(TAG, "removeVirtualDisplayLocked :" + appRecord.pkgName + " size " + appRecord.virtualDisplaySize);
            }
            if (appRecord.virtualDisplaySize == 0) {
                appRecord.setWorkingState(0, Dataspace.STANDARD_BT709);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleWorkingStateLocked(FrozenAppRecord app) {
        FrozenAppRecord appRecord = this.mApps.get(createKey(app.uid, app.pkgName));
        if (appRecord != null) {
            if (DEBUG) {
                VSlog.d(TAG, "handleWorkingStateLocked :" + app.pkgName + " allowFreeze " + appRecord.allowFreeze + " mWorkingState " + appRecord.getName(appRecord.mWorkingState));
            }
            if (appRecord.allowFreeze && appRecord.mWorkingState == 0 && appRecord.virtualDisplaySize == 0 && !appRecord.frozen) {
                this.mFrozenHandler.removeMessages(2, appRecord);
                FrozenHandler frozenHandler = this.mFrozenHandler;
                frozenHandler.sendMessageDelayed(frozenHandler.obtainMessage(2, appRecord), 500L);
                return;
            }
            this.mFrozenHandler.removeMessages(2, appRecord);
        }
    }

    public void reportFreezeStatusAsync(int uid, String procName, int pid, boolean allowFreeze, int procState) {
        ProcessInfo proc;
        if (!isFeatureSupport) {
            return;
        }
        synchronized (this.mProcs) {
            proc = this.mProcs.get(pid);
        }
        if (proc == null || proc.mRecord == null || !procName.equals(proc.mProcName) || proc.mRecord.isSystemApp) {
            return;
        }
        if (isBlackListFromPem && proc.mRecord.isInPemBlackList) {
            return;
        }
        if (isWhiteListFromPem && !proc.mRecord.isInPemWhiteList) {
            return;
        }
        if (DEBUG) {
            VSlog.d(TAG, "reportFreezeStatusAsync :" + procName + " " + pid + " " + allowFreeze + " state " + procState);
        }
        proc.allowFreeze = allowFreeze;
        proc.mProcState = procState;
        proc.mRecord.computeFreezeStatus();
        if (proc.mRecord.allowFreeze) {
            this.mFrozenHandler.removeMessages(6, proc.mRecord);
            long delayMillis = 0;
            delayMillis = (!proc.mRecord.allowSkipDelay() || isInteractive() || isPlugged()) ? 5000L : 5000L;
            FrozenHandler frozenHandler = this.mFrozenHandler;
            frozenHandler.sendMessageDelayed(frozenHandler.obtainMessage(6, 0, 0, proc.mRecord), delayMillis);
            return;
        }
        this.mFrozenHandler.removeMessages(6, proc.mRecord);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleProcStateLoked(FrozenAppRecord app) {
        FrozenAppRecord appRecord = this.mApps.get(createKey(app.uid, app.pkgName));
        if (appRecord != null) {
            appRecord.computeFreezeStatus();
            if (DEBUG) {
                VSlog.d(TAG, "handleProcState pkg:" + appRecord.pkgName + " allowFreezeStatus " + appRecord.allowFreeze + " workingState " + appRecord.getName(appRecord.mWorkingState));
            }
            if (appRecord.allowFreeze && appRecord.mWorkingState == 0 && appRecord.virtualDisplaySize == 0 && !appRecord.frozen) {
                FrozenHandler frozenHandler = this.mFrozenHandler;
                frozenHandler.sendMessageDelayed(frozenHandler.obtainMessage(2, appRecord), 500L);
                return;
            }
            this.mFrozenHandler.removeMessages(2, appRecord);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCheckStatusLoked(FrozenAppRecord app) {
        FrozenAppRecord appRecord = this.mApps.get(app.mKey);
        if (appRecord != null) {
            appRecord.computeFreezeStatus();
            if (appRecord.allowFreeze && appRecord.mWorkingState == 0 && appRecord.virtualDisplaySize == 0 && !appRecord.frozen) {
                this.mFrozenHandler.removeMessages(2, appRecord);
                FrozenHandler frozenHandler = this.mFrozenHandler;
                frozenHandler.sendMessageDelayed(frozenHandler.obtainMessage(2, appRecord), 500L);
                return;
            }
            this.mFrozenHandler.removeMessages(2, appRecord);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScreenOffLocked() {
        int i = this.mScreenState;
        if (i == 2 || i == 6) {
            return;
        }
        if (DEBUG) {
            VSlog.d(TAG, "handleScreenOffLocked intput " + this.mCurrentInputMethod);
        }
        freezeInputMethod();
        freezeWallpaper();
        freezeForegroundAppLocked();
        this.isScreenOffFrozen = true;
    }

    private void freezeInputMethod() {
        if (MultiDisplayManager.isVCarDisplayRunning()) {
            VSlog.i(TAG, "carnetworking is running ,skip!");
            return;
        }
        String str = this.mCurrentInputMethod;
        if (str != null) {
            this.mVfps.requestFrozen(str, true, 2);
        }
    }

    private void freezeWallpaper() {
        ComponentName componentName = this.mWallPaper;
        if (componentName != null && !isSystemApp(componentName.getPackageName(), -1)) {
            this.mVfps.requestFrozen(this.mWallPaper.getPackageName(), true, 2);
        }
    }

    private void freezeForegroundAppLocked() {
        Map<Integer, String> foregroundApp = this.mVivoStats.getForegroundApp();
        this.mWaitingFrozenForegroundApp = foregroundApp;
        if (foregroundApp == null || foregroundApp.size() <= 0) {
            VSlog.i(TAG, "can't get foreground app!");
            return;
        }
        for (Integer num : this.mWaitingFrozenForegroundApp.keySet()) {
            int uid = num.intValue();
            String pkgName = this.mWaitingFrozenForegroundApp.get(Integer.valueOf(uid));
            String appInfo = "pkgName[" + pkgName + "], uid[" + uid + "]";
            VSlog.i(TAG, "foreground app is " + appInfo);
            if (!isPemAllowFrozen(pkgName)) {
                VSlog.i(TAG, appInfo + " isn't in freeze white list!");
            } else {
                FrozenAppRecord appRecord = this.mApps.get(createKey(uid, pkgName));
                if (appRecord == null) {
                    VSlog.i(TAG, "appRecord is null!");
                } else {
                    FrozenHandler frozenHandler = this.mFrozenHandler;
                    frozenHandler.sendMessage(frozenHandler.obtainMessage(6, appRecord));
                }
            }
        }
    }

    private boolean isSystemApp(String packageName, int uid) {
        if (this.mPackageManager == null) {
            this.mPackageManager = this.mContext.getPackageManager();
        }
        if (this.mPackageManager != null) {
            try {
                int userId = uid != -1 ? UserHandle.getUserId(uid) : ActivityManager.getCurrentUser();
                ApplicationInfo info = this.mPackageManager.getApplicationInfoAsUser(packageName, 0, userId);
                if (info != null) {
                    if (info.isSystemApp()) {
                        return true;
                    }
                }
                return false;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScreenOnLocked() {
        int i = this.mScreenState;
        if ((i != 2 && i != 6) || !this.isScreenOffFrozen) {
            return;
        }
        VSlog.d(TAG, "handleScreenOnLocked intput " + this.mCurrentInputMethod);
        unfreezeInputMethod();
        unfreezeWallpaper();
        unfreezeForegroundAppLocked();
        this.isScreenOffFrozen = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unfreezeInputMethod() {
        String str = this.mCurrentInputMethod;
        if (str != null) {
            this.mVfps.isKeepFrozen(str, -1, TAG, 1000, 0, true, UNFREEZE_REASON_SCREEN_ON);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unfreezeWallpaper() {
        ComponentName componentName = this.mWallPaper;
        if (componentName != null) {
            this.mVfps.isKeepFrozen(componentName.getPackageName(), -1, TAG, 1000, 0, true, UNFREEZE_REASON_SCREEN_ON);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unfreezeForegroundAppLocked() {
        Map<Integer, String> map = this.mWaitingFrozenForegroundApp;
        if (map == null || map.size() <= 0) {
            return;
        }
        for (Integer num : this.mWaitingFrozenForegroundApp.keySet()) {
            int uid = num.intValue();
            if (this.mVfps.isFrozenPackage(uid)) {
                String pkgName = this.mWaitingFrozenForegroundApp.get(Integer.valueOf(uid));
                this.mVfps.isKeepFrozen(pkgName, uid, TAG, 1000, 0, true, UNFREEZE_REASON_SCREEN_ON);
            }
        }
        this.mWaitingFrozenForegroundApp.clear();
    }

    public boolean setFrozenPkgBlacklist(List<String> pkgNames, int len) {
        synchronized (mBlackListFromPem) {
            mBlackListFromPem.clear();
            mWhiteListFromPem.clear();
            mBlackListFromPem.addAll(pkgNames);
            mBlackListFromPem.addAll(UNFREEZE_APP_LIST);
            if (this.mCurrentInputMethod != null) {
                mBlackListFromPem.add(this.mCurrentInputMethod);
            }
            VSlog.d(TAG, "get frozen black pkglist size" + mBlackListFromPem.size());
            Message msg = this.mFrozenHandler.obtainMessage(11);
            this.mFrozenHandler.sendMessage(msg);
            isBlackListFromPem = true;
            isWhiteListFromPem = false;
        }
        return true;
    }

    public boolean setFrozenPkgWhitelist(List<String> pkgNames, int len) {
        synchronized (mWhiteListFromPem) {
            mWhiteListFromPem.clear();
            mBlackListFromPem.clear();
            mWhiteListFromPem.addAll(pkgNames);
            VSlog.d(TAG, "get frozen white pkglist size" + mWhiteListFromPem.size());
            Message msg = this.mFrozenHandler.obtainMessage(15);
            this.mFrozenHandler.sendMessage(msg);
            isWhiteListFromPem = true;
            isBlackListFromPem = false;
        }
        return true;
    }

    public boolean isAudioOn(int uid, String pkgName) {
        FrozenAppRecord appRecord;
        synchronized (this) {
            appRecord = this.mApps.get(createKey(uid, pkgName));
        }
        if (appRecord != null) {
            appRecord.isAudioOn();
            VSlog.d("DEBUG_FROZEN", "appRecord.isAudioOn pkgName " + pkgName + " audio " + appRecord.isAudioOn());
            return appRecord.isAudioOn();
        }
        return false;
    }

    public boolean setPackageList(String type, Bundle data) {
        if (PACKAGE_LIST_TYPE_BINDER_PROXY.equals(type)) {
            return setBinderProxyList(data);
        }
        if (PACKAGE_LIST_TYPE_UNFREEZE_PERIOD.equals(type)) {
            return setUnfreezePeriodList(data);
        }
        if (PACKAGE_LIST_TYPE_POWER_CONNECTED.equals(type)) {
            return this.mVfps.setPowerConnectedUnfreezeList(data);
        }
        if (PACKAGE_LIST_TYPE_NO_NOTI_DOWNLOAD.equals(type)) {
            return setNoNotiDownloadAppList(data);
        }
        return this.mWms.setPackageList(type, data);
    }

    private boolean setBinderProxyList(Bundle data) {
        ArrayList<String> list;
        if (data == null || (list = data.getStringArrayList("list")) == null) {
            return false;
        }
        VivoProxyImpl.getInstance().setWhiteList(list);
        return true;
    }

    private boolean setUnfreezePeriodList(Bundle data) {
        ArrayList<String> list;
        double[] periods;
        if (data == null || (list = data.getStringArrayList("list")) == null) {
            return false;
        }
        synchronized (mUnfreezePeriodMap) {
            mUnfreezePeriodMap.clear();
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                String pkgName = it.next();
                if (!TextUtils.isEmpty(pkgName) && (periods = data.getDoubleArray(pkgName)) != null && periods.length != 0) {
                    mUnfreezePeriodMap.put(pkgName, periods);
                }
            }
        }
        this.mFrozenHandler.removeMessages(16);
        this.mFrozenHandler.sendEmptyMessage(16);
        return true;
    }

    private boolean isInUnfreezePeriodList(String pkgName) {
        boolean containsKey;
        synchronized (mUnfreezePeriodMap) {
            containsKey = mUnfreezePeriodMap.containsKey(pkgName);
        }
        return containsKey;
    }

    private boolean setNoNotiDownloadAppList(Bundle data) {
        ArrayList<String> list;
        if (data == null || (list = data.getStringArrayList("list")) == null) {
            return false;
        }
        synchronized (sNoNotiDownloadAppList) {
            sNoNotiDownloadAppList.clear();
            sNoNotiDownloadAppList.addAll(list);
        }
        return true;
    }

    private boolean inNoNotiDownloadAppList(String pkgName) {
        boolean contains;
        synchronized (sNoNotiDownloadAppList) {
            contains = sNoNotiDownloadAppList.contains(pkgName);
        }
        return contains;
    }

    public void beginToCheckDownloadStatus(FrozenAppRecord app) {
        if (!isFeatureSupport) {
            return;
        }
        FrozenHandler frozenHandler = this.mFrozenHandler;
        frozenHandler.sendMessage(frozenHandler.obtainMessage(10, 0, 0, app));
        if (DEBUG) {
            VSlog.i(TAG, "beginToCheckDownloadStatus : " + app.pkgName + " uid = " + app.uid);
        }
    }

    public void stopToCheckDownloadStatus(FrozenAppRecord app) {
        if (!isFeatureSupport) {
            return;
        }
        FrozenHandler frozenHandler = this.mFrozenHandler;
        frozenHandler.sendMessage(frozenHandler.obtainMessage(12, 0, 0, app));
    }

    public void stopCheckDownloadStatus(FrozenAppRecord app) {
        if (!isFeatureSupport) {
            return;
        }
        this.mFrozenHandler.removeMessages(10, app);
        if (DEBUG) {
            VSlog.i(TAG, "stopToCheckDownloadStatus : " + app.pkgName + " uid = " + app.uid);
        }
        this.mWms.stopCheckDownloadStatus(app.uid, app.pkgName);
    }

    public void reportFrozenSuccess(String packageName, int uid) {
        if (!isFeatureSupport) {
            return;
        }
        FrozenHandler frozenHandler = this.mFrozenHandler;
        frozenHandler.sendMessage(frozenHandler.obtainMessage(4, this.COMMON_KEY.obtain(packageName, uid)));
        if (DEBUG) {
            VSlog.i(TAG, "notifyFrozenSuccess : " + packageName + " uid = " + uid);
        }
    }

    public String createKey(int uid, String pkgName) {
        return String.format(Locale.US, "%d_%s", Integer.valueOf(uid), pkgName);
    }

    public void dumpQuickFrozenInformation(PrintWriter pw, String[] args, int opti) {
        if (!SystemProperties.get("persist.vivo.frozen.allow_dump", "no").equals("yes")) {
            return;
        }
        pw.println("-----quick-frozen inforamtion------");
        synchronized (this) {
            StringBuilder builderaAllow = new StringBuilder((int) Consts.ProcessStates.FOCUS);
            StringBuilder builderNotAllow = new StringBuilder((int) Consts.ProcessStates.FOCUS);
            StringBuilder builderFrozen = new StringBuilder((int) Consts.ProcessStates.FOCUS);
            StringBuilder builderInBlackList = new StringBuilder((int) Consts.ProcessStates.FOCUS);
            int allowSize = 0;
            int notAllowSize = 0;
            int frozenSize = 0;
            int inBlackList = 0;
            for (Map.Entry<String, FrozenAppRecord> map : this.mApps.entrySet()) {
                FrozenAppRecord pkgInfo = map.getValue();
                if (pkgInfo.frozen) {
                    frozenSize++;
                    builderFrozen.append("#frozen ");
                    builderFrozen.append(frozenSize);
                    builderFrozen.append(" ");
                    builderFrozen.append(pkgInfo.toString());
                } else if (pkgInfo.isInPemBlackList && isBlackListFromPem) {
                    inBlackList++;
                    builderInBlackList.append("#BlackList ");
                    builderInBlackList.append(inBlackList);
                    builderInBlackList.append(" ");
                    builderInBlackList.append(pkgInfo.toString());
                } else if (pkgInfo.isAllowFrozen() && !pkgInfo.isSystemApp && ((!pkgInfo.isInPemBlackList && isBlackListFromPem) || (isWhiteListFromPem && pkgInfo.isInPemWhiteList))) {
                    allowSize++;
                    builderaAllow.append("#allow ");
                    builderaAllow.append(allowSize);
                    builderaAllow.append(" ");
                    builderaAllow.append(pkgInfo.toString());
                } else {
                    notAllowSize++;
                    builderNotAllow.append("#unallow ");
                    builderNotAllow.append(notAllowSize);
                    builderNotAllow.append(" ");
                    builderNotAllow.append(pkgInfo.toString());
                }
            }
            pw.println("#frozen -- size " + frozenSize);
            pw.println(builderFrozen.toString());
            pw.println("------------------------------------------\n");
            pw.println("#allow -- size " + allowSize);
            pw.println(builderaAllow.toString());
            pw.println("------------------------------------------\n");
            pw.println("#unallow -- size " + notAllowSize);
            pw.println(builderNotAllow.toString());
            if (isBlackListFromPem) {
                pw.println("------------------------------------------\n");
                pw.println("#blacklist -- size " + inBlackList);
                pw.println(builderInBlackList.toString());
                pw.println("------------------------");
                pw.println("------pem black list, length = " + mBlackListFromPem.size());
                Iterator<String> it = mBlackListFromPem.iterator();
                while (it.hasNext()) {
                    String set = it.next();
                    pw.println(set);
                }
                pw.println("------------------------");
            } else if (isWhiteListFromPem) {
                pw.println("------------------------");
                pw.println("------pem white list, length = " + mWhiteListFromPem.size());
                Iterator<String> it2 = mWhiteListFromPem.iterator();
                while (it2.hasNext()) {
                    String set2 = it2.next();
                    pw.println(set2);
                }
                pw.println("------------------------");
            }
            HashSet<String> uncheckAudioList = this.mWms.getPackageList("audio");
            if (uncheckAudioList != null) {
                pw.println("------uncheck audio list, length = " + uncheckAudioList.size());
                Iterator<String> it3 = uncheckAudioList.iterator();
                while (it3.hasNext()) {
                    String pkgName = it3.next();
                    pw.println(pkgName);
                }
                pw.println("------------------------");
            }
            HashSet<String> uncheckDownloadList = this.mWms.getPackageList("download");
            if (uncheckDownloadList != null) {
                pw.println("------uncheck download list, length = " + uncheckDownloadList.size());
                Iterator<String> it4 = uncheckDownloadList.iterator();
                while (it4.hasNext()) {
                    String pkgName2 = it4.next();
                    pw.println(pkgName2);
                }
                pw.println("------------------------");
            }
            HashSet<String> checkGPSList = this.mWms.getPackageList("navigation");
            if (checkGPSList != null) {
                pw.println("------check GPS list, length = " + checkGPSList.size());
                Iterator<String> it5 = checkGPSList.iterator();
                while (it5.hasNext()) {
                    String pkgName3 = it5.next();
                    pw.println(pkgName3);
                }
                pw.println("------------------------");
            }
            ArrayList<String> addBinderProxyWhiteList = VivoProxyImpl.getInstance().getWhiteList();
            if (addBinderProxyWhiteList != null) {
                pw.println("------add binder proxy(System App) list, length = " + addBinderProxyWhiteList.size());
                Iterator<String> it6 = addBinderProxyWhiteList.iterator();
                while (it6.hasNext()) {
                    String pkgName4 = it6.next();
                    pw.println(pkgName4);
                }
                pw.println("------------------------");
            }
            pw.println("------unfreeze period list, length = " + mUnfreezePeriodMap.size());
            for (String pkgName5 : mUnfreezePeriodMap.keySet()) {
                pw.println(pkgName5);
            }
            pw.println("------No-Noti download list, length = " + sNoNotiDownloadAppList.size());
            Iterator<String> it7 = sNoNotiDownloadAppList.iterator();
            while (it7.hasNext()) {
                String pkgName6 = it7.next();
                pw.println(pkgName6);
            }
            pw.println("------------------------");
            pw.println("------mCurrentInputMethod " + this.mCurrentInputMethod);
            if (this.mWallPaper != null) {
                pw.println("------mWallPaper " + this.mWallPaper.getPackageName());
            } else {
                pw.println("------mWallPaper null ");
            }
            pw.println("---isQuickFrozenSupport " + isFeatureSupport);
            pw.println("---isBlackListFromPem " + isBlackListFromPem);
            pw.println("---isQuickFrozenPause " + isQuickFrozenPause);
            pw.println("---isWhiteListFromPem " + isWhiteListFromPem);
            pw.println("---screenOffFrozenEnable " + screenOffFrozenEnable);
            pw.println("-------finish-----------------");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final FrozenQuicker INSTANCE = new FrozenQuicker();

        private Instance() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class FrozenHandler extends Handler {
        private FrozenHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    synchronized (FrozenQuicker.this) {
                        ProcessInfo pi = (ProcessInfo) msg.obj;
                        FrozenQuicker.this.addAppLocked(pi);
                    }
                    return;
                case 1:
                    ProcessInfo pi2 = (ProcessInfo) msg.obj;
                    synchronized (FrozenQuicker.this) {
                        FrozenQuicker.this.removeAppLocked(pi2);
                    }
                    return;
                case 2:
                    FrozenAppRecord pi3 = (FrozenAppRecord) msg.obj;
                    FrozenQuicker.this.frozenApplication(pi3);
                    return;
                case 3:
                    synchronized (FrozenQuicker.this) {
                        FrozenAppRecord pi4 = (FrozenAppRecord) msg.obj;
                        FrozenQuicker.this.handleWorkingStateLocked(pi4);
                    }
                    return;
                case 4:
                    synchronized (FrozenQuicker.this) {
                        FrozenAppRecord info = (FrozenAppRecord) msg.obj;
                        FrozenQuicker.this.appFrozenSuccessLocked(info.uid, info.pkgName);
                    }
                    return;
                case 5:
                    FrozenAppRecord info2 = (FrozenAppRecord) msg.obj;
                    synchronized (FrozenQuicker.this) {
                        FrozenQuicker.this.unFreezeAppLocked(info2.uid, info2.pkgName);
                    }
                    return;
                case 6:
                    FrozenAppRecord info3 = (FrozenAppRecord) msg.obj;
                    synchronized (FrozenQuicker.this) {
                        FrozenQuicker.this.handleProcStateLoked(info3);
                    }
                    return;
                case 7:
                    FrozenAppRecord info4 = (FrozenAppRecord) msg.obj;
                    synchronized (FrozenQuicker.this) {
                        FrozenQuicker.this.handleCheckStatusLoked(info4);
                    }
                    return;
                case 8:
                    synchronized (FrozenQuicker.this) {
                        FrozenQuicker.this.handleScreenOffLocked();
                    }
                    return;
                case 9:
                    synchronized (FrozenQuicker.this) {
                        FrozenQuicker.this.handleScreenOnLocked();
                    }
                    return;
                case 10:
                    FrozenAppRecord appRecord = (FrozenAppRecord) msg.obj;
                    if (appRecord != null) {
                        FrozenQuicker.this.mWms.beginCheckDownloadStatus(appRecord.uid, appRecord.pkgName, FrozenQuicker.this.isPerceptible(appRecord));
                        return;
                    }
                    return;
                case 11:
                    synchronized (FrozenQuicker.this) {
                        FrozenQuicker.this.updateBlackInformationLocked();
                    }
                    return;
                case 12:
                    FrozenQuicker.this.stopCheckDownloadStatus((FrozenAppRecord) msg.obj);
                    return;
                case 13:
                    FrozenAppRecord appRecord2 = (FrozenAppRecord) msg.obj;
                    synchronized (FrozenQuicker.this) {
                        FrozenQuicker.this.addVirtualDisplayLocked(appRecord2);
                    }
                    return;
                case 14:
                    FrozenAppRecord appRecord3 = (FrozenAppRecord) msg.obj;
                    synchronized (FrozenQuicker.this) {
                        FrozenQuicker.this.removeVirtualDisplayLocked(appRecord3);
                    }
                    return;
                case 15:
                    synchronized (FrozenQuicker.this) {
                        FrozenQuicker.this.updateWhiteListInformationLocked();
                    }
                    return;
                case 16:
                    synchronized (FrozenQuicker.this) {
                        FrozenQuicker.this.updateUnfreezePeriodListLocked();
                    }
                    return;
                case 17:
                    String pkgName = (String) msg.obj;
                    int uid = msg.arg1;
                    FrozenQuicker.this.mVfps.isKeepFrozen(pkgName, uid, FrozenQuicker.TAG, 1000, 0, true, FrozenQuicker.UNFREEZE_REASON_UNFREEZE_SNS);
                    return;
                case 18:
                    String pkgName2 = (String) msg.obj;
                    int uid2 = msg.arg1;
                    synchronized (FrozenQuicker.this) {
                        FrozenQuicker.this.handleAddWindowLocked(pkgName2, uid2);
                    }
                    return;
                case 19:
                    FrozenAppRecord appRecord4 = (FrozenAppRecord) msg.obj;
                    if (appRecord4.frozen) {
                        FrozenQuicker.this.mVfps.isKeepFrozen(appRecord4.pkgName, appRecord4.uid, FrozenQuicker.TAG, 1000, 0, true, FrozenQuicker.UNFREEZE_REASON_ADD_WINDOW);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }
}