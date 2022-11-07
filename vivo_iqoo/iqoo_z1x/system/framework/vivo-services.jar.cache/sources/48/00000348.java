package com.android.server.notification;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.AutomaticZenRule;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.media.AudioAttributes;
import android.media.IRingtonePlayer;
import android.media.RingtoneManager;
import android.multidisplay.MultiDisplayManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.FtBuild;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.notification.Adjustment;
import android.service.notification.INotificationListener;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.internal.util.Preconditions;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.notification.ManagedServices;
import com.android.server.notification.NotificationManagerService;
import com.android.server.policy.VivoPolicyConstant;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.vivo.appshare.AppShareConfig;
import com.vivo.common.utils.VibrationUtils;
import com.vivo.face.common.data.Constants;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.Switch;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoNotificationManagerServiceImpl implements IVivoNotificationManagerService {
    static final String ABE_DATA_COLLECT_ACTION = "com.vivo.abe.thirdapp.lifecontrol_framework_data_collect";
    static final String ABE_DATA_COLLECT_EVENT_ID = "110";
    static final String ABE_DATA_COLLECT_LABEL_ID = "1103";
    private static final int FLAG_NOTICE = 2;
    private static final int FLAG_RECORD = 1;
    private static final int HASTE_DEGREE_MAX = 100;
    private static final int HASTE_DEGREE_MIN = 0;
    private static final int HASTE_DEGREE_TRIGGER = 50;
    private static final int INTERNAL_NOTIFICATION_TYPE_NORMAL = 0;
    private static final int INTERNAL_NOTIFICATION_TYPE_PUSH = 1;
    public static boolean IS_VOS = false;
    static final int MAX_RETRY_TIMES = 10;
    private static final String MMS_SLOT0_CHANNEL_ID = "mms_slot0_channel";
    private static final String MMS_SLOT1_CHANNEL_ID = "mms_slot1_channel";
    static final int NEED_PREVENT_FORM_NOTIFICATION = 1;
    private static final String NOTIFICATION_EXCEPTION_FILE = "data/bbkcore/NotificationService_foreground_channel_exception_resolver_1.0.xml";
    private static final String NOTIFICATION_EXCEPTION_KEY = "catch_delete_fgs_channel_exception";
    private static final int NO_HASTE_MODE_DELAY_MS = 15;
    private static final int NO_HASTE_MODE_RESET_MS = 1000;
    public static final String PKG_LAUNCHER = "com.android.launcher3";
    static final int RETRY_INTERVAL = 10000;
    private static final boolean SUPPORTED_DYNAMIC_VIBRATION;
    private static final int SYSTEM_NOTIFICATION_CODE = 1;
    private static final int SYSTEM_SOUND_CODE = 2;
    private static final String TAG = "NotificationService";
    private static final boolean USE_TRACK_TITLE_SOUND;
    private static final int VIBRATE_DYNAMIC = 2;
    private static final int VIBRATE_NONE = 0;
    private static final int VIBRATE_STANDARD = 1;
    AbeProviderObserver mAbeProviderObserver;
    private AlarmManager mAlarmManager;
    private AbsConfigurationManager mConfigurationManager;
    private int mDefaultNotificationLedOff;
    private int mDefaultNotificationLedOn;
    private Handler mHandler;
    private Handler mInternalAbeHandler;
    private PendingIntent mInternalAbePendingIntent;
    private HandlerThread mInternalAbeThread;
    private NotificationRecord mLedNotification;
    private NotificationManagerService.NotificationListeners mListeners;
    private NotificationBadBehaviorManager mNotificationBadBehaviorManager;
    private NotificationBlackListManager mNotificationBlackListManager;
    private NotificationClassifyManager mNotificationClassifyManager;
    private Switch mNotificationExceptionCatchSwitch;
    private NotificationManagerService mNotificationManagerService;
    private boolean mNotificationNoticeStates;
    private boolean mNotificationVideoStates;
    private PowerManager.WakeLock mNotificationWakeLock;
    NotificationWhiteListManager mNotificationWhiteListManager;
    private VivoSettingObserver mObserver;
    private IPackageManager mPackageManager;
    private PreferencesHelper mPreferencesHelper;
    private Handler mSoundTitleHandler;
    private VivoRatioControllerUtilsImpl mVivoRatioControllerUtils;
    private static final int MAX_NOTIFICATIONS = SystemProperties.getInt("persist.sys.notification.max", 100);
    private static final String[] GRANTED_APPS = {FaceUIState.PKG_SYSTEMUI, "com.vivo.pushservice", "com.vivo.abe"};
    private static final int MY_UID = Process.myUid();
    private static final int MY_PID = Process.myPid();
    private static final int DEBUG_VIVO_NMSLEDS = SystemProperties.getInt("persist.sys.debug.nmsleds", 1);
    public static String INDICATOR_CONFIG = SystemProperties.get("persist.vivo.phone.indicator", "No_indicator");
    final Object mBindFailedLock = new Object();
    BindFailedRunnable mBindFailedRunnable = null;
    boolean mIsBindFailedRunnableEnqueue = false;
    private Runnable mStopNotificationSoundRunnable = new Runnable() { // from class: com.android.server.notification.VivoNotificationManagerServiceImpl.1
        @Override // java.lang.Runnable
        public void run() {
            VivoNotificationManagerServiceImpl.this.stopNotificationSound();
        }
    };
    private int mHasteDegree = 0;
    private long mLastPostTime = 0;
    private Object mHasteLock = new Object();
    HashMap<String, Integer> mAbeNotificationMap = new HashMap<>();
    HashMap<String, String> mAbeNotificationCountsMap = new HashMap<>();
    private final Uri THIRD_APP = Uri.parse("content://com.vivo.abe.thirdapp.lifecontrol.provider/ThirdApp");
    String[] appProjection = {"id", "pkg", "bgLifeCycleSwitchOn", "bgNotificationSwitchOn", "bgLifeCycleInterval", "sysCallerList", "rxData", "txData", "notifyState"};
    boolean hasSetAlarm = false;
    private int mNotificationVibrate = 2;
    private int mMessageVibrateSim1 = 2;
    private int mMessageVibrateSim2 = 2;
    private int mCalendarVibrate = 2;
    private final ArrayMap<Uri, String> mCacheSoundTitle = new ArrayMap<>(20);
    private String mAppSharePackageName = null;
    private int mAppShareUserId = -1;
    private MultiDisplayManager mMultiDisplayManager = null;
    private int mNotificationVibrateIntensity = 2;
    private final List<String> mRequestedTimeoutNotificationKeys = new ArrayList();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.VivoNotificationManagerServiceImpl.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(VivoNotificationManagerServiceImpl.ABE_DATA_COLLECT_ACTION)) {
                VSlog.d(VivoNotificationManagerServiceImpl.TAG, "receive ABE_DATA_COLLECT_ACTION");
                VivoNotificationManagerServiceImpl.this.mInternalAbeHandler.post(VivoNotificationManagerServiceImpl.this.mAbeInternalTask);
            }
        }
    };
    private boolean mNotificationExceptionCatchEnabled = false;
    private Runnable mAbeInternalTask = new Runnable() { // from class: com.android.server.notification.VivoNotificationManagerServiceImpl.7
        @Override // java.lang.Runnable
        public void run() {
            if (NotificationManagerService.DBG) {
                VSlog.d(VivoNotificationManagerServiceImpl.TAG, "Its time to run task...");
            }
            VivoNotificationManagerServiceImpl.this.mAlarmManager.set(0, System.currentTimeMillis() + 86400000, VivoNotificationManagerServiceImpl.this.mInternalAbePendingIntent);
        }
    };
    private final HandlerThread mGetSoundTitleThread = new HandlerThread("notification_sound_title");
    private String mAudioTitleForVibrate = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private boolean mBlockHappened = false;
    private UriMatcher mSoundUriMatcher = new UriMatcher(-1);
    private boolean mIsCtsVInstalled = false;

    static {
        boolean z = true;
        if (SystemProperties.getInt("persist.vivo.support.lra", 0) != 1 || SystemProperties.getInt("persist.vivo.support.sound.vibrate", 0) != 1) {
            z = false;
        }
        SUPPORTED_DYNAMIC_VIBRATION = z;
        USE_TRACK_TITLE_SOUND = SystemProperties.getBoolean("ro.config.fos_use_tracktitle_sound", false);
        IS_VOS = FtBuild.getOsName().equals("vos");
    }

    public VivoNotificationManagerServiceImpl(NotificationManagerService notificationManagerService, Handler handler, PreferencesHelper preferencesHelper, PackageManager packageManagerClient, IPackageManager packageManager, NotificationManagerService.NotificationListeners listeners, AlarmManager alarmManager, RankingHelper rankingHelper) {
        this.mVivoRatioControllerUtils = null;
        this.mNotificationManagerService = notificationManagerService;
        this.mHandler = handler;
        this.mPreferencesHelper = preferencesHelper;
        this.mListeners = listeners;
        this.mAlarmManager = alarmManager;
        this.mPackageManager = packageManager;
        this.mNotificationWhiteListManager = new NotificationWhiteListManager(this.mNotificationManagerService.getContext(), this.mNotificationManagerService, preferencesHelper);
        this.mNotificationBlackListManager = new NotificationBlackListManager(this.mNotificationManagerService.getContext(), this.mNotificationManagerService);
        this.mNotificationBadBehaviorManager = new NotificationBadBehaviorManager(this.mNotificationManagerService.getContext(), this.mNotificationManagerService);
        this.mNotificationClassifyManager = new NotificationClassifyManager(packageManagerClient, this.mNotificationManagerService.getContext(), this.mHandler, this.mNotificationManagerService);
        VivoRankingHelperImpl impl = (VivoRankingHelperImpl) rankingHelper.getVivoInjectInstance();
        impl.setClassifyManager(this.mNotificationClassifyManager);
        Resources resources = this.mNotificationManagerService.getContext().getResources();
        this.mDefaultNotificationLedOn = resources.getInteger(17694781);
        this.mDefaultNotificationLedOff = resources.getInteger(17694780);
        this.mObserver = new VivoSettingObserver(this.mHandler);
        this.mVivoRatioControllerUtils = VivoRatioControllerUtilsImpl.getInstance();
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    public void onStart() {
        regiterBroadcastReceiver();
        this.mNotificationWhiteListManager.onStart();
        this.mNotificationBlackListManager.onStart();
        this.mNotificationBadBehaviorManager.onStart();
        this.mNotificationClassifyManager.onStart();
        readSystemVibrationMode();
        this.mGetSoundTitleThread.start();
        this.mSoundTitleHandler = this.mGetSoundTitleThread.getThreadHandler();
        this.mSoundUriMatcher.addURI("media", "internal/audio/media/#", 2);
        boolean z = true;
        this.mSoundUriMatcher.addURI("settings", "system/notification_sound", 1);
        initWakeLock();
        if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
            AbsConfigurationManager configurationManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getConfigurationManager();
            this.mConfigurationManager = configurationManager;
            if (configurationManager != null) {
                Switch r0 = configurationManager.getSwitch(NOTIFICATION_EXCEPTION_FILE, NOTIFICATION_EXCEPTION_KEY);
                this.mNotificationExceptionCatchSwitch = r0;
                if (r0 != null && !r0.isUninitialized() && !this.mNotificationExceptionCatchSwitch.isOn()) {
                    z = false;
                }
                this.mNotificationExceptionCatchEnabled = z;
                if (NotificationManagerService.DBG) {
                    VSlog.d(TAG, "ignore delete notification crash, isOn = " + this.mNotificationExceptionCatchEnabled);
                }
                this.mConfigurationManager.registerObserver(this.mNotificationExceptionCatchSwitch, new ConfigurationObserver() { // from class: com.android.server.notification.VivoNotificationManagerServiceImpl.3
                    public void onConfigChange(String file, String name) {
                        VivoNotificationManagerServiceImpl vivoNotificationManagerServiceImpl = VivoNotificationManagerServiceImpl.this;
                        vivoNotificationManagerServiceImpl.mNotificationExceptionCatchSwitch = vivoNotificationManagerServiceImpl.mConfigurationManager.getSwitch(VivoNotificationManagerServiceImpl.NOTIFICATION_EXCEPTION_FILE, VivoNotificationManagerServiceImpl.NOTIFICATION_EXCEPTION_KEY);
                        VivoNotificationManagerServiceImpl vivoNotificationManagerServiceImpl2 = VivoNotificationManagerServiceImpl.this;
                        vivoNotificationManagerServiceImpl2.mNotificationExceptionCatchEnabled = vivoNotificationManagerServiceImpl2.mNotificationExceptionCatchSwitch == null || VivoNotificationManagerServiceImpl.this.mNotificationExceptionCatchSwitch.isUninitialized() || VivoNotificationManagerServiceImpl.this.mNotificationExceptionCatchSwitch.isOn();
                        if (NotificationManagerService.DBG) {
                            VSlog.d(VivoNotificationManagerServiceImpl.TAG, "notification exception switch onConfigChange, isOn = " + VivoNotificationManagerServiceImpl.this.mNotificationExceptionCatchEnabled);
                        }
                    }
                });
            }
        }
    }

    private void regiterBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ABE_DATA_COLLECT_ACTION);
        IntentFilter bbklogFilter = new IntentFilter();
        this.mNotificationManagerService.getContext().registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter, null, null);
        bbklogFilter.addAction(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED);
        this.mNotificationManagerService.getContext().registerReceiver(new BroadcastReceiver() { // from class: com.android.server.notification.VivoNotificationManagerServiceImpl.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                boolean status = "on".equals(intent.getStringExtra("adblog_status"));
                NotificationManagerService.DBG = status;
            }
        }, bbklogFilter);
        IntentFilter crashedFilter = new IntentFilter();
        crashedFilter.addAction("com.vivo.internal.broadcast.fix_channel_crash");
        this.mNotificationManagerService.getContext().registerReceiver(new BroadcastReceiver() { // from class: com.android.server.notification.VivoNotificationManagerServiceImpl.5
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    String crashedPkg = intent.getStringExtra("crashed_package");
                    if (!TextUtils.isEmpty(crashedPkg)) {
                        if (NotificationManagerService.DBG) {
                            VSlog.d(VivoNotificationManagerServiceImpl.TAG, "received ignoring crash broadcast for " + crashedPkg);
                        }
                        if (VivoNotificationManagerServiceImpl.this.mPreferencesHelper.markApplicationCrashIgnored(crashedPkg)) {
                            VivoNotificationManagerServiceImpl.this.mNotificationManagerService.handleSavePolicyFile();
                        }
                    }
                }
            }
        }, crashedFilter, "com.vivo.permission.NOTIFY_SYSTEM_IGNORE_NOTIFICATION_CRASH", null);
    }

    public void checkStopNotificationSound(Uri soundUri, boolean looping) {
        List<String> path;
        String soundScheme = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (soundUri != null && (path = soundUri.getPathSegments()) != null && path.size() > 0) {
            soundScheme = path.get(0);
        }
        if (!looping && soundScheme.equals("external")) {
            this.mHandler.removeCallbacks(this.mStopNotificationSoundRunnable);
            this.mHandler.postDelayed(this.mStopNotificationSoundRunnable, 10000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopNotificationSound() {
        this.mNotificationManagerService.mSoundNotificationKey = null;
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                IRingtonePlayer player = this.mNotificationManagerService.mAudioManager.getRingtonePlayer();
                if (player != null) {
                    player.stopAsync();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void cancelEarliestNotificationLocked(NotificationRecord goingToEnqueue) {
        synchronized (this.mNotificationManagerService.mNotificationLock) {
            cancelEarliestNotification(goingToEnqueue);
        }
    }

    private void cancelEarliestNotification(NotificationRecord goingToEnqueue) {
        NotificationRecord goingToCancel;
        int count = this.mNotificationManagerService.mNotificationList.size() + this.mNotificationManagerService.mEnqueuedNotifications.size();
        if (count >= MAX_NOTIFICATIONS) {
            NotificationRecord earliestNotification = null;
            NotificationRecord earliestPushNotification = null;
            int N = this.mNotificationManagerService.mNotificationList.size();
            for (int i = 0; i < N; i++) {
                NotificationRecord record = (NotificationRecord) this.mNotificationManagerService.mNotificationList.get(i);
                if (!shouldNotCancel(record, goingToEnqueue)) {
                    earliestNotification = getEarlierNotification(earliestNotification, record);
                    if (isPush(record.getNotification())) {
                        earliestPushNotification = getEarlierNotification(earliestPushNotification, record);
                    }
                }
            }
            if (earliestPushNotification != null) {
                goingToCancel = earliestPushNotification;
            } else {
                goingToCancel = earliestNotification;
            }
            if (goingToCancel != null && !isGroupHasOnlyOneChild(goingToCancel.getSbn())) {
                cancelEarliestNotificationInternal(goingToCancel);
            }
        }
    }

    private NotificationRecord getEarlierNotification(NotificationRecord current, NotificationRecord next) {
        if (current == null || next.getRankingTimeMs() < current.getRankingTimeMs()) {
            return next;
        }
        return current;
    }

    private boolean shouldNotCancel(NotificationRecord r, NotificationRecord goingToEnqueue) {
        boolean isOngoingOrForeGroundService = (r.getNotification().flags & 66) != 0;
        if (isOngoingOrForeGroundService) {
            VSlog.d(TAG, "Skip notification because it has FLAG_ONGOING_EVENT or FLAG_FOREGROUND_SERVICE flag . record: " + r);
        }
        return isOngoingOrForeGroundService || isGroupSummaryAndHasChildren(r) || isSummaryOfGoiongToEnqueue(r, goingToEnqueue);
    }

    private boolean isGroupSummaryAndHasChildren(NotificationRecord r) {
        if (r.getSbn().getNotification().isGroupSummary()) {
            List<NotificationRecord> groupNotifications = this.mNotificationManagerService.findGroupNotificationsLocked(r.getSbn().getPackageName(), r.getSbn().getGroupKey(), r.getSbn().getUserId());
            if (groupNotifications.size() > 1) {
                VSlog.d(TAG, "Skip notification because it is summary and has children. record: " + r);
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean isSummaryOfGoiongToEnqueue(NotificationRecord r, NotificationRecord goingToEnqueue) {
        if (r.getNotification().isGroupSummary() && goingToEnqueue.getNotification().isGroupChild() && r.getSbn().getGroupKey().equals(goingToEnqueue.getSbn().getGroupKey())) {
            VSlog.d(TAG, "Skip notification because it is summary of notification going to enqueue. record: " + r);
            return true;
        }
        return false;
    }

    private boolean isGroupHasOnlyOneChild(StatusBarNotification sbn) {
        if (sbn.getNotification().isGroupChild() && !sbn.getGroupKey().contains("ranker_group")) {
            List<NotificationRecord> groupNotifications = this.mNotificationManagerService.findGroupNotificationsLocked(sbn.getPackageName(), sbn.getGroupKey(), sbn.getUserId());
            if (groupNotifications.size() == 2) {
                for (int i = 0; i < groupNotifications.size(); i++) {
                    if (groupNotifications.get(i).getSbn().getNotification().isGroupSummary()) {
                        VSlog.e(TAG, "Notification is group and it's summary has only one child. We remove both!");
                        cancelEarliestNotificationInternal(groupNotifications.get(0));
                        cancelEarliestNotificationInternal(groupNotifications.get(1));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void cancelEarliestNotificationInternal(final NotificationRecord goingToCancel) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.notification.-$$Lambda$VivoNotificationManagerServiceImpl$Fgv0o1fjBIoFBLaTuXJ1rSVEVnM
            @Override // java.lang.Runnable
            public final void run() {
                VivoNotificationManagerServiceImpl.this.lambda$cancelEarliestNotificationInternal$0$VivoNotificationManagerServiceImpl(goingToCancel);
            }
        });
    }

    public /* synthetic */ void lambda$cancelEarliestNotificationInternal$0$VivoNotificationManagerServiceImpl(NotificationRecord goingToCancel) {
        synchronized (this.mNotificationManagerService.mNotificationLock) {
            this.mNotificationManagerService.mNotificationList.remove(goingToCancel);
            this.mNotificationManagerService.mNotificationsByKey.remove(goingToCancel.getSbn().getKey());
            this.mNotificationManagerService.cancelNotificationLocked(goingToCancel, true, 2, true, (String) null);
            VSlog.e(TAG, "All notifications have reached the limit:" + MAX_NOTIFICATIONS + ". So we need cancel the earliest notification: " + goingToCancel);
        }
    }

    private boolean isPush(Notification notification) {
        return notification.internalType == 1;
    }

    public boolean isPushSummary(StatusBarNotification sbn) {
        return isPush(sbn.getNotification()) && sbn.getNotification().isGroupSummary() && sbn.getGroupKey().contains("om.vivo.pushservice.notification.lessimportant");
    }

    private void enqueueClassified(final StatusBarNotification statusBarNotification) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.notification.VivoNotificationManagerServiceImpl.6
            @Override // java.lang.Runnable
            public void run() {
                boolean isNeedClassify = true;
                synchronized (VivoNotificationManagerServiceImpl.this.mNotificationManagerService.mNotificationLock) {
                    NotificationRecord old = (NotificationRecord) VivoNotificationManagerServiceImpl.this.mNotificationManagerService.mNotificationsByKey.get(statusBarNotification.getKey());
                    if (old != null) {
                        isNeedClassify = false;
                    }
                }
                if (isNeedClassify) {
                    VivoNotificationManagerServiceImpl.this.mNotificationClassifyManager.enqueueClassified(statusBarNotification);
                    return;
                }
                VSlog.d(VivoNotificationManagerServiceImpl.TAG, "already existed. key=" + statusBarNotification.getKey());
            }
        });
    }

    public void beforeEnqueue(int callingUid, String pkg, StatusBarNotification statusBarNotification) {
        checkHaste(callingUid, pkg);
        enqueueClassified(statusBarNotification);
    }

    private void checkHaste(int callingUid, String pkg) {
        boolean isSystemNotification = this.mNotificationManagerService.isUidSystemOrPhone(callingUid) || VivoPermissionUtils.OS_PKG.equals(pkg);
        if (!isSystemNotification || pkg.equals("com.android.providers.downloads")) {
            checkHasteInternal();
        }
    }

    private void checkHasteInternal() {
        synchronized (this.mHasteLock) {
            long now = System.currentTimeMillis();
            long freshness = now - this.mLastPostTime;
            this.mLastPostTime = now;
            if (freshness <= 20) {
                this.mHasteDegree += 2;
            } else if (freshness <= 40) {
                this.mHasteDegree++;
            }
            if (freshness >= 1000) {
                this.mHasteDegree = 0;
            } else if (freshness >= 200) {
                this.mHasteDegree -= 10;
            }
            if (this.mHasteDegree > 100) {
                this.mHasteDegree = 100;
            }
            if (this.mHasteDegree < 0) {
                this.mHasteDegree = 0;
            }
        }
        if (this.mHasteDegree >= 50) {
            VSlog.d(TAG, "no haste mode!");
            try {
                Thread.sleep(15L);
            } catch (Exception e) {
            }
        }
    }

    public boolean isCallerSystemOrGrantedApp() {
        if (this.mNotificationManagerService.isCallerSystemOrPhone()) {
            return true;
        }
        return isCallerSpecialGrantedApp();
    }

    public boolean isCallerSpecialGrantedApp() {
        int uid = Binder.getCallingUid();
        for (int i = 0; i < GRANTED_APPS.length; i++) {
            try {
                ApplicationInfo grantedApp = AppGlobals.getPackageManager().getApplicationInfo(GRANTED_APPS[i], 0, UserHandle.getCallingUserId());
                if (grantedApp != null && grantedApp.uid == uid) {
                    return true;
                }
            } catch (Exception e) {
                VSlog.d(TAG, "getApplication cause exception: " + e);
            }
        }
        return false;
    }

    public void enqueueNotificationWithTag(String pkg, String opPkg, String tag, int id, Notification notification, int userId) {
        VSlog.d(TAG, "DEBUG_OVERRIDE_NOTIFY:enqueueNotificationWithTag pkg=" + pkg + " opPkg=" + opPkg + "tag=" + tag + " notification=" + notification + " userId =" + userId);
        notification.internalType = 0;
        if (("com.vivo.abe".equals(pkg) || "com.vivo.abetest".equals(pkg) || "com.vivo.pushservice".equals(pkg)) && isCallerSystemOrGrantedApp()) {
            enqueueOverrideNotification(pkg, opPkg, tag, id, notification, userId);
        } else {
            this.mNotificationManagerService.enqueueNotificationInternal(pkg, opPkg, Binder.getCallingUid(), Binder.getCallingPid(), tag, id, notification, userId);
        }
    }

    private void enqueueOverrideNotification(String pkg, String opPkg, String tag, int id, Notification notification, int userId) {
        int overrideUid;
        boolean isOverrideNotification;
        int overrideUid2 = -1;
        PackageManager pm = this.mNotificationManagerService.getContext().getPackageManager();
        String overridePkg = notification.extras.getCharSequence("override_pkgname", pkg).toString();
        VSlog.d(TAG, "DEBUG_OVERRIDE_NOTIFY: OVERRIDE notification:" + notification + " BY " + pkg + " got overridePkg=" + overridePkg);
        if (pkg.equals(overridePkg)) {
            overrideUid = -1;
            isOverrideNotification = false;
        } else {
            try {
                overrideUid2 = pm.getPackageUidAsUser(overridePkg, userId);
                notification.internalType = 1;
                String overrideChannelId = notification.extras.getCharSequence("channel_id", notification.getChannelId()).toString();
                if (!overrideChannelId.equals(notification.getChannelId())) {
                    notification.setChannel(overrideChannelId);
                }
                VSlog.d(TAG, "DEBUG_OVERRIDE_NOTIFY: OVERRIDE notification:" + notification + " BY " + pkg + " got overrideUid=" + overrideUid2);
                overrideUid = overrideUid2;
                isOverrideNotification = true;
            } catch (Exception e) {
                VSlog.d(TAG, "DEBUG_OVERRIDE_NOTIFY: e= " + e);
                overrideUid = overrideUid2;
                isOverrideNotification = false;
            }
        }
        this.mNotificationManagerService.enqueueNotificationInternal(isOverrideNotification ? overridePkg : pkg, isOverrideNotification ? overridePkg : opPkg, isOverrideNotification ? overrideUid : Binder.getCallingUid(), Binder.getCallingPid(), tag, id, notification, userId, false, isOverrideNotification, isOverrideNotification ? pkg : null, isOverrideNotification ? Binder.getCallingUid() : -1);
    }

    public void onClearAllIgnoreFlags() {
        synchronized (this.mNotificationManagerService.mNotificationList) {
            int N = this.mNotificationManagerService.mNotificationList.size();
            for (int i = N - 1; i >= 0; i--) {
                NotificationRecord r = (NotificationRecord) this.mNotificationManagerService.mNotificationList.get(i);
                if (!amnesty(r.getSbn().getPackageName(), r.getSbn().getId())) {
                    this.mNotificationManagerService.mNotificationList.remove(i);
                    this.mNotificationManagerService.cancelNotificationLocked(r, true, 2, true, (String) null);
                }
            }
            this.mNotificationManagerService.updateLightsLocked();
        }
    }

    private boolean amnesty(String pkg, int id) {
        if ("com.android.incallui".equals(pkg) && 10002 == id) {
            return true;
        }
        if (("com.android.phone".equals(pkg) && 2 == id) || "com.android.bbksoundrecorder".equals(pkg) || "com.android.bbkmusic".equals(pkg)) {
            return true;
        }
        return "com.vivo.daemonService".equals(pkg) && 10100 == id;
    }

    public void onClearIgnoreFlags(String pkg) {
        synchronized (this.mNotificationManagerService.mNotificationList) {
            int N = this.mNotificationManagerService.mNotificationList.size();
            for (int i = N - 1; i >= 0; i--) {
                NotificationRecord r = (NotificationRecord) this.mNotificationManagerService.mNotificationList.get(i);
                if (r.getSbn().getPackageName().equals(pkg)) {
                    this.mNotificationManagerService.mNotificationList.remove(i);
                    this.mNotificationManagerService.cancelNotificationLocked(r, true, 2, true, (String) null);
                }
            }
            this.mNotificationManagerService.updateLightsLocked();
        }
    }

    public void checkWhenPackageChanged(Intent intent, int changeUserId, boolean removingPackage) {
        this.mNotificationWhiteListManager.checkWhenAppInstalled(intent, changeUserId);
        if (removingPackage) {
            this.mNotificationWhiteListManager.checkWhenAppUninstalled(intent);
            this.mNotificationClassifyManager.checkWhenAppUnInstalled(intent, changeUserId);
        }
    }

    public int checkWhenPackageChangedReason(Intent intent, int reason) {
        String action = intent.getAction();
        if (action == null) {
            return reason;
        }
        if (action.equals("android.intent.action.PACKAGE_RESTARTED") && !intent.getBooleanExtra("vivo.intent.extra.cancel_notification", true)) {
            return 21;
        }
        return reason;
    }

    public void saveWhiteManualList(String pkg, int uid) {
        this.mNotificationWhiteListManager.saveSwitchedManualItem(pkg, uid);
    }

    public void joinVivoPush(String pkg) {
        if (!isCallerSystemOrGrantedApp()) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationWhiteListManager.joinVivoPushInternal(pkg);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean hasJoinVivoPush(String pkg, int uid) {
        this.mNotificationManagerService.checkCallerIsSystemOrSameApp(pkg);
        return this.mNotificationWhiteListManager.hasJoinVivoPush(pkg);
    }

    public boolean areMoreNotificationsEnabledForPackage(String pkg, int uid) {
        this.mNotificationManagerService.checkCallerIsSystemOrSameApp(pkg);
        return this.mPreferencesHelper.areMoreNotificationsEnabled(pkg, uid);
    }

    public void setMoreNotificationsEnabledForPackage(String pkg, int uid, boolean enabled) {
        this.mNotificationManagerService.checkCallerIsSystem();
        this.mNotificationWhiteListManager.saveAllowMoreManualItem(pkg, uid);
        this.mPreferencesHelper.setMoreNotificationEnabled(pkg, uid, enabled, false);
        if (!enabled) {
            this.mNotificationManagerService.cancelAllNotificationsInt(MY_UID, MY_PID, pkg, "miscellaneous", 0, 0, true, UserHandle.getUserId(uid), 7, (ManagedServices.ManagedServiceInfo) null);
        }
        this.mNotificationManagerService.handleSavePolicyFile();
    }

    public boolean enqueueNotificationInternal(String pkg, String opPkg, int callingUid, int callingPid, String tag, int id, Notification notification, int incomingUserId, boolean isOverrideNotification, String pushServicePkg, int pushServiceUid) {
        if (this.mNotificationBlackListManager.isIntercept(pkg)) {
            if (NotificationManagerService.DBG) {
                VSlog.v(TAG, "notification intercepted: pkg=" + pkg + " id=" + id + " notification=" + notification);
            }
            return true;
        }
        if (this.mNotificationBadBehaviorManager.isNeed2Modify(pkg)) {
            if (NotificationManagerService.DBG) {
                VSlog.v(TAG, "notification is bad in badBehaviorList: pkg=" + pkg + " id=" + id + " notification=" + notification);
            }
            this.mNotificationBadBehaviorManager.modifyNotification(notification);
            Bundle extras = notification.extras;
            if (notification.internalType != 1 && !notification.isCustomNotification() && extras != null && TextUtils.isEmpty(extras.getCharSequence("android.title")) && TextUtils.isEmpty(extras.getCharSequence("android.text"))) {
                if (NotificationManagerService.DBG) {
                    VSlog.v(TAG, "notification is bad behavior and is blank notification.pkg=" + pkg + " id=" + id);
                }
                return true;
            }
        }
        NotificationChannel nc = this.mPreferencesHelper.getNotificationChannel(pkg, callingUid, notification.getChannelId(), false);
        return nc != null && nc.isRequestDeleteByAppForFgsNotification();
    }

    public boolean areVivoCustomNotificationEnabled() {
        this.mNotificationManagerService.checkCallerIsSystem();
        return this.mNotificationClassifyManager.areVivoCustomNotificationEnabled();
    }

    public boolean areVivoCustomNotificationEnabledForPackage(String pkg, int uid) {
        this.mNotificationManagerService.checkCallerIsSystem();
        return this.mNotificationClassifyManager.areVivoCustomNotificationEnabledForPackage(pkg, uid);
    }

    public void setVivoCustomNotificationEnabled(boolean enabled) {
        this.mNotificationManagerService.checkCallerIsSystem();
        this.mNotificationClassifyManager.setVivoCustomNotificationEnabled(enabled);
    }

    public void setVivoCustomNotificationEnabledForPackage(String pkg, int uid, boolean enabled) {
        this.mNotificationManagerService.checkCallerIsSystem();
        this.mNotificationClassifyManager.setVivoCustomNotificationEnabledForPackage(pkg, uid, enabled);
    }

    public int getNotificationClassifiedResult(String key, long postTime) {
        return this.mNotificationClassifyManager.getNotificationClassifiedResult(key, postTime);
    }

    public void removeNotificationClassifiedResult(String key, long postTime) {
        this.mNotificationClassifyManager.removeNotificationClassifiedResult(key, postTime);
    }

    public boolean shouldChannelDisableByDefault(String pkg, int uid) {
        boolean disabledByDefault = false;
        if (this.mNotificationWhiteListManager.hasJoinVivoPush(pkg) && !this.mPreferencesHelper.areMoreNotificationsEnabled(pkg, uid)) {
            disabledByDefault = true;
        }
        if (this.mNotificationWhiteListManager.hasJoinVivoPush(pkg)) {
            int pkgUid = findPkgUidAsUser(pkg, UserHandle.getUserId(uid));
            if (!this.mPreferencesHelper.areMoreNotificationsEnabled(pkg, pkgUid)) {
                return true;
            }
            return disabledByDefault;
        }
        return disabledByDefault;
    }

    public boolean isBlocked(NotificationRecord r, String pkg, int callingUid, boolean isBlocked) {
        if (!isBlocked && this.mNotificationWhiteListManager.hasJoinVivoPush(pkg) && "miscellaneous".equals(r.getChannel().getId()) && !this.mPreferencesHelper.areMoreNotificationsEnabled(pkg, callingUid)) {
            VSlog.d(TAG, "Blocked when more notifiations disabled for join VPush apps");
            return true;
        }
        Notification no = r.getNotification();
        if (no != null && !isPush(no)) {
            if ((r.getSbn().getOverrideGroupKey() == null || !no.isGroupSummary()) && RmsInjectorImpl.getInstance().needKeepQuiet(pkg, UserHandle.getUserId(callingUid), callingUid, 1)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean shouldIgnoredSnooze(NotificationRecord r) {
        if (r.getNotification().isGroupSummary() && r.getSbn().getGroupKey().contains("ranker_group")) {
            if (NotificationManagerService.DBG) {
                VSlog.d(TAG, "snooze skip " + r);
                return true;
            }
            return true;
        }
        return false;
    }

    public void toNotifyBindFailed(ManagedServices.ManagedServiceInfo info, String operation) {
        synchronized (this.mBindFailedLock) {
            if (FaceUIState.PKG_SYSTEMUI.equals(info.component.getPackageName())) {
                VSlog.d(TAG, "notify failed operation " + operation + " isInEnqueue " + this.mIsBindFailedRunnableEnqueue);
                if (this.mBindFailedRunnable == null) {
                    this.mBindFailedRunnable = new BindFailedRunnable(0, operation);
                }
                if (!this.mIsBindFailedRunnableEnqueue) {
                    this.mBindFailedRunnable.index = 0;
                    this.mBindFailedRunnable.operation = operation;
                    this.mHandler.postDelayed(this.mBindFailedRunnable, 10000L);
                    this.mIsBindFailedRunnableEnqueue = true;
                }
            }
        }
    }

    public void resetBindFailed(ComponentName component) {
        synchronized (this.mBindFailedLock) {
            if (FaceUIState.PKG_SYSTEMUI.equals(component.getPackageName()) && this.mBindFailedRunnable != null) {
                VSlog.d(TAG, "register listener pkg = " + component.getPackageName());
                this.mHandler.removeCallbacks(this.mBindFailedRunnable);
                this.mIsBindFailedRunnableEnqueue = false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean notifyBindFailed() {
        boolean result = false;
        for (ManagedServices.ManagedServiceInfo info : this.mListeners.getServices()) {
            if (FaceUIState.PKG_SYSTEMUI.equals(info.component.getPackageName())) {
                INotificationListener listener = info.service;
                try {
                    listener.onNotificationNotifyBindFailed();
                    result = true;
                } catch (RemoteException ex) {
                    VSlog.e(TAG, "notify bind failed:" + listener, ex);
                    result = false;
                }
            }
        }
        return result;
    }

    public void updateModifiedAdjustment(NotificationRecord r, boolean modified) {
        if (r.getSbn().modified != modified) {
            Bundle signals = new Bundle();
            signals.putBoolean("key_modified", modified);
            Adjustment adjustment = new Adjustment(r.getSbn().getPackageName(), r.getKey(), signals, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, r.getSbn().getUserId());
            r.setModifiedAdjustment(adjustment);
        }
    }

    public void updateAutoGroupSummary(int userId, String pkg, boolean modified) {
        int N = this.mNotificationManagerService.mNotificationList.size();
        int i = 0;
        while (true) {
            if (i >= N) {
                break;
            }
            NotificationRecord r = (NotificationRecord) this.mNotificationManagerService.mNotificationList.get(i);
            StatusBarNotification sbn = r.getSbn();
            if (sbn == null || !sbn.getPackageName().equals(pkg) || sbn.getUserId() != userId || !sbn.getNotification().isGroupSummary() || !sbn.getGroupKey().contains("ranker_group")) {
                i++;
            } else {
                r.getSbn().modified = modified;
                this.mNotificationManagerService.mNotificationList.set(i, r);
                break;
            }
        }
        int M = this.mNotificationManagerService.mEnqueuedNotifications.size();
        for (int i2 = 0; i2 < M; i2++) {
            NotificationRecord r2 = (NotificationRecord) this.mNotificationManagerService.mEnqueuedNotifications.get(i2);
            StatusBarNotification sbn2 = r2.getSbn();
            if (sbn2 != null && sbn2.getPackageName().equals(pkg) && sbn2.getUserId() == userId && sbn2.getNotification().isGroupSummary() && sbn2.getGroupKey().contains("ranker_group")) {
                r2.getSbn().modified = modified;
                this.mNotificationManagerService.mEnqueuedNotifications.set(i2, r2);
                return;
            }
        }
    }

    public boolean autoGroupSummaryModified(StatusBarNotification sbn) {
        String key;
        NotificationRecord record;
        ArrayMap<String, String> summaries = (ArrayMap) this.mNotificationManagerService.mAutobundledSummaries.get(Integer.valueOf(sbn.getUserId()));
        if (summaries != null && (key = summaries.get(sbn.getPackageName())) != null && (record = (NotificationRecord) this.mNotificationManagerService.mNotificationsByKey.get(key)) != null) {
            return record.getSbn().modified;
        }
        return true;
    }

    public boolean shouldMuteNotificationByClassify(NotificationRecord record) {
        boolean result = false;
        if (record == null) {
            return false;
        }
        StatusBarNotification sbn = record.getSbn();
        int classifyImportance = sbn.getClassifyImportance();
        if (1 == classifyImportance) {
            if (NotificationManagerService.DBG) {
                VSlog.v(TAG, "mute notification because of notification classify.");
            }
            result = true;
        }
        if (sbn.getNotification().internalType == 1 && record.getChannel() != null && "vivo_push_channel".equals(record.getChannel().getId()) && sbn.getNotification().internalPriority < 0) {
            if (NotificationManagerService.DBG) {
                VSlog.v(TAG, "mute notification because of low priority push.");
            }
            return true;
        }
        return result;
    }

    public void setInterruptionFilter(ZenModeHelper zenModeHelper, String pkg, int zen) {
        if (isCallerSystemApp(pkg)) {
            zenModeHelper.setManualZenMode(zen, (Uri) null, pkg, "setInterruptionFilter");
            return;
        }
        zenModeHelper.setManualZenMode(zen, (Uri) null, pkg, "setInterruptionFilterC");
        checkForCtsTest(pkg);
    }

    private void checkForCtsTest(String pkg) {
        if (isGMSApk(pkg)) {
            int mSleepMs = SystemProperties.getInt("debug.zenmode.sleep.ms", (int) ProcessList.HEAVY_WEIGHT_APP_ADJ);
            if (NotificationManagerService.DBG) {
                VSlog.v(TAG, "setInterruptionFilter+++ pkg:" + pkg + " mSleepMs:" + mSleepMs);
            }
            try {
                Thread.sleep(mSleepMs);
            } catch (InterruptedException e) {
            }
            if (NotificationManagerService.DBG) {
                VSlog.v(TAG, "setInterruptionFilter--- pkg:" + pkg + " mSleepMs:" + mSleepMs);
            }
        }
    }

    private boolean isGMSApk(String packageName) {
        if ((packageName != null && ((packageName.contains("cts") || packageName.contains("gts") || packageName.contains("com.android.frameworks")) && packageName.contains(VivoPermissionUtils.OS_PKG))) || "android.app.stubs".equals(packageName)) {
            return true;
        }
        return false;
    }

    private boolean isCallerSystemApp(String pkg) {
        try {
            ApplicationInfo ai = this.mPackageManager.getApplicationInfo(pkg, 0, UserHandle.getCallingUserId());
            if (ai == null) {
                throw new SecurityException("Unknown package " + pkg);
            } else if ((ai.flags & KernelConfig.AP_TE) != 0) {
                return true;
            } else {
                return false;
            }
        } catch (RemoteException e) {
            throw new SecurityException("Unknown package " + pkg);
        }
    }

    public boolean isVibrateSettingOnForFos(boolean originRingerModeState) {
        if (IS_VOS) {
            return !originRingerModeState;
        }
        return this.mNotificationManagerService.mAudioManager.getVibrateSetting(0) == 1;
    }

    public boolean shouldIgnoreCancel(String channelId, NotificationRecord r, int reason) {
        if (channelId != null && r.getNotification().isGroupSummary() && r.getGroupKey().contains("ranker_group")) {
            return true;
        }
        if (reason == 21 && r.getNotification().internalType == 1) {
            return true;
        }
        return false;
    }

    public boolean isUidSystemUI(int uid) {
        int systemuiUid = 0;
        try {
            systemuiUid = AppGlobals.getPackageManager().getApplicationInfo(FaceUIState.PKG_SYSTEMUI, 0, UserHandle.getCallingUserId()).uid;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uid == systemuiUid;
    }

    public boolean isSpeciallyAllowCancel(int reason, int callingUid) {
        if (reason == 2 && isUidSystemUI(callingUid)) {
            return true;
        }
        return false;
    }

    /* loaded from: classes.dex */
    protected class BindFailedRunnable implements Runnable {
        int index;
        String operation;

        public BindFailedRunnable(int index, String operation) {
            this.index = 0;
            this.index = index;
            this.operation = operation;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoNotificationManagerServiceImpl.this.mBindFailedLock) {
                VivoNotificationManagerServiceImpl.this.mIsBindFailedRunnableEnqueue = false;
                if (VivoNotificationManagerServiceImpl.this.mBindFailedRunnable == null) {
                    VivoNotificationManagerServiceImpl.this.mBindFailedRunnable = this;
                    VSlog.e(VivoNotificationManagerServiceImpl.TAG, "mBindFailedRunnable is null");
                }
                boolean isSuccess = VivoNotificationManagerServiceImpl.this.notifyBindFailed();
                if (isSuccess) {
                    VivoNotificationManagerServiceImpl.this.mHandler.removeCallbacks(VivoNotificationManagerServiceImpl.this.mBindFailedRunnable);
                    VivoNotificationManagerServiceImpl.this.mIsBindFailedRunnableEnqueue = false;
                    VSlog.d(VivoNotificationManagerServiceImpl.TAG, "notify failed event success, index=" + this.index);
                } else {
                    VivoNotificationManagerServiceImpl.this.mBindFailedRunnable.index = this.index + 1;
                    VSlog.d(VivoNotificationManagerServiceImpl.TAG, "notify failed event to be retried, index=" + VivoNotificationManagerServiceImpl.this.mBindFailedRunnable.index);
                    if (VivoNotificationManagerServiceImpl.this.mBindFailedRunnable.index < 10) {
                        VivoNotificationManagerServiceImpl.this.mHandler.postDelayed(VivoNotificationManagerServiceImpl.this.mBindFailedRunnable, 10000L);
                        VivoNotificationManagerServiceImpl.this.mIsBindFailedRunnableEnqueue = true;
                    }
                }
            }
        }
    }

    public void updateLightsLocked() {
        if (this.mNotificationManagerService.mNotificationLight == null) {
            return;
        }
        int ledARGB = 0;
        int flag_red = 0;
        int flag_green = 0;
        int n = this.mNotificationManagerService.mLights.size();
        getLightMenuSet();
        for (int i = 0; i < n; i++) {
            NotificationRecord notificationRecord = (NotificationRecord) this.mNotificationManagerService.mNotificationsByKey.get(this.mNotificationManagerService.mLights.get(i));
            this.mLedNotification = notificationRecord;
            if (notificationRecord != null) {
                if (notificationRecord.getNotification().ledARGB == -65536 && ((String) this.mNotificationManagerService.mLights.get(i)).contains("bbksoundrecorder")) {
                    flag_red = 1;
                } else if ((this.mLedNotification.getNotification().defaults & 4) != 0 || (this.mLedNotification.getNotification().ledARGB & (-1)) != 0) {
                    flag_green = 1;
                }
                printfLedInfoLevel3("=== defaults=" + Integer.toHexString(this.mLedNotification.getNotification().defaults) + " ledARGB=" + Integer.toHexString(this.mLedNotification.getNotification().ledARGB) + " flag_red=" + flag_red + " flag_green=" + flag_green);
            }
        }
        if (flag_red == 1) {
            ledARGB = 0 | (-65536);
        }
        if (flag_green == 1) {
            ledARGB |= -16711936;
        }
        printfLedInfoLevel1("===VNOILNMS n=" + n + " VideoStates=" + this.mNotificationVideoStates + " NoticeStates=" + this.mNotificationNoticeStates + " flag_red=" + flag_red + " flag_green=" + flag_green + " mScreenOn=" + this.mNotificationManagerService.mScreenOn + " mNotificationPulseEnabled=" + this.mNotificationManagerService.mNotificationPulseEnabled);
        if (this.mLedNotification == null || this.mNotificationManagerService.isInCall() || this.mNotificationManagerService.mScreenOn || ledARGB == 0) {
            printfLedInfoLevel2("===turnOff");
            this.mNotificationManagerService.mNotificationLight.turnOff();
            return;
        }
        int ledOnMS = this.mDefaultNotificationLedOn;
        int ledOffMS = this.mDefaultNotificationLedOff;
        if ((65280 & ledARGB) != 0 && this.mNotificationNoticeStates && this.mNotificationManagerService.mNotificationPulseEnabled) {
            printfLedInfoLevel2("===setFlashing notification green");
            this.mNotificationManagerService.mNotificationLight.setFlashing(-16711936, 1, ledOnMS, ledOffMS);
        } else if ((16711680 & ledARGB) != 0 && this.mNotificationVideoStates && this.mNotificationManagerService.mNotificationPulseEnabled) {
            printfLedInfoLevel2("===setColor Sound recording red");
            this.mNotificationManagerService.mNotificationLight.setColor(-65536);
        }
    }

    public void clearLightsLocked() {
        int noticeCount = this.mNotificationManagerService.mLights.size();
        printfLedInfoLevel3("noticeCount:" + noticeCount);
        String soundRecorderNotification = null;
        int i = 0;
        while (true) {
            if (i >= noticeCount) {
                break;
            }
            printfLedInfoLevel3("find a light: " + ((String) this.mNotificationManagerService.mLights.get(i)));
            if (!((String) this.mNotificationManagerService.mLights.get(i)).contains("bbksoundrecorder")) {
                i++;
            } else {
                printfLedInfoLevel3("find goal:" + ((String) this.mNotificationManagerService.mLights.get(i)));
                soundRecorderNotification = (String) this.mNotificationManagerService.mLights.get(i);
                break;
            }
        }
        this.mNotificationManagerService.mLights.clear();
        if (soundRecorderNotification != null) {
            ActivityManager activityManager = (ActivityManager) this.mNotificationManagerService.getContext().getSystemService(VivoFirewall.TYPE_ACTIVITY);
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
            int processNumber = runningProcesses.size();
            boolean recorderIsOn = false;
            for (int j = 0; j < processNumber; j++) {
                String[] pkgList = runningProcesses.get(j).pkgList;
                int packageNumber = pkgList.length;
                int k = 0;
                while (true) {
                    if (k >= packageNumber) {
                        break;
                    }
                    String packageName = pkgList[k];
                    if (!packageName.contains("bbksoundrecorder")) {
                        k++;
                    } else {
                        recorderIsOn = true;
                        this.mNotificationManagerService.mLights.add(soundRecorderNotification);
                        printfLedInfoLevel3("add SoundRecorder to notification list");
                        break;
                    }
                }
                if (recorderIsOn) {
                    break;
                }
            }
        }
        updateLightsLocked();
    }

    private void getLightMenuSet() {
        if ("No_indicator".equals(INDICATOR_CONFIG)) {
            this.mNotificationVideoStates = false;
            this.mNotificationNoticeStates = false;
        } else if ("Have_indicator_green".equals(INDICATOR_CONFIG)) {
            int notification_light_on = Settings.System.getInt(this.mNotificationManagerService.getContext().getContentResolver(), "notification_light", 15);
            this.mNotificationVideoStates = false;
            if ((notification_light_on & 1) == 1) {
                this.mNotificationNoticeStates = true;
            } else {
                this.mNotificationNoticeStates = false;
            }
        } else {
            int notification_light_on2 = Settings.System.getInt(this.mNotificationManagerService.getContext().getContentResolver(), "notification_light", 14);
            if ((notification_light_on2 & 1) == 1) {
                this.mNotificationVideoStates = true;
            } else {
                this.mNotificationVideoStates = false;
            }
            if ((notification_light_on2 & 2) == 2) {
                this.mNotificationNoticeStates = true;
            } else {
                this.mNotificationNoticeStates = false;
            }
        }
    }

    public void printfLedInfoLevel1(String msg) {
        int i = DEBUG_VIVO_NMSLEDS;
        if (i == 1 || i > 5) {
            VSlog.i("VNOILNMS", msg);
        }
    }

    public void printfLedInfoLevel2(String msg) {
        int i = DEBUG_VIVO_NMSLEDS;
        if (i == 2 || i > 5) {
            VSlog.i("VNOILNMS", msg);
        }
    }

    public void printfLedInfoLevel3(String msg) {
        int i = DEBUG_VIVO_NMSLEDS;
        if (i == 3 || i > 7) {
            VSlog.i("VNOILNMS", msg);
        }
    }

    public void cancelAllNotificationsForFrozen(String pkg, int userId) {
        this.mNotificationManagerService.checkCallerIsSystemOrSameApp(pkg);
        this.mNotificationManagerService.cancelAllNotificationsInt(Binder.getCallingUid(), Binder.getCallingPid(), pkg, (String) null, 0, 0, true, ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, false, "cancelAllNotifications", pkg), 21, (ManagedServices.ManagedServiceInfo) null);
        if (NotificationManagerService.DBG) {
            VSlog.d(TAG, "cancelAllNotificationsForFrozen pkg = " + pkg);
        }
    }

    public void dumpImpl(PrintWriter pw) {
        this.mNotificationWhiteListManager.dump(pw);
    }

    /* loaded from: classes.dex */
    private final class AbeProviderObserver extends ContentObserver {
        public AbeProviderObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (NotificationManagerService.DBG) {
                VSlog.d(VivoNotificationManagerServiceImpl.TAG, "AbeProviderObserver onChange...");
            }
            VivoNotificationManagerServiceImpl.this.updateAbeNotificationInfo();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x010b, code lost:
        if (r11 == null) goto L23;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x010e, code lost:
        if (r12 != false) goto L30;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x0114, code lost:
        if (r0.size() <= 0) goto L29;
     */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x0116, code lost:
        r17.mAbeNotificationMap = (java.util.HashMap) r0.clone();
     */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x011e, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:?, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:?, code lost:
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void updateAbeNotificationInfo() {
        /*
            Method dump skipped, instructions count: 293
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.notification.VivoNotificationManagerServiceImpl.updateAbeNotificationInfo():void");
    }

    private void thirdLifeControlOnStart() {
        HandlerThread handlerThread = new HandlerThread("InternalAbe");
        this.mInternalAbeThread = handlerThread;
        handlerThread.start();
        this.mInternalAbeHandler = new Handler(this.mInternalAbeThread.getLooper());
        this.mAbeProviderObserver = new AbeProviderObserver(this.mInternalAbeHandler);
        ContentResolver cr = this.mNotificationManagerService.getContext().getContentResolver();
        cr.registerContentObserver(this.THIRD_APP, true, this.mAbeProviderObserver);
        Intent tmp = new Intent(ABE_DATA_COLLECT_ACTION);
        tmp.setPackage(this.mNotificationManagerService.getContext().getPackageName());
        this.mInternalAbePendingIntent = PendingIntent.getBroadcast(this.mNotificationManagerService.getContext(), 0, tmp, 0);
    }

    private boolean enqueueNotificationForThirdLifeControl(String pkg) {
        Integer notNeedNotify;
        HashMap<String, Integer> hashMap = this.mAbeNotificationMap;
        if (hashMap == null || hashMap.size() <= 0 || (notNeedNotify = this.mAbeNotificationMap.get(pkg)) == null || notNeedNotify.intValue() != 1) {
            return false;
        }
        VSlog.d(TAG, "return because of thirdlife control.");
        int count = 1;
        String countStr = this.mAbeNotificationCountsMap.get(pkg);
        if (countStr != null) {
            int count2 = Integer.parseInt(countStr);
            count = count2 + 1;
        }
        this.mAbeNotificationCountsMap.put(pkg, String.valueOf(count));
        if (!this.hasSetAlarm) {
            if (NotificationManagerService.DBG) {
                VSlog.d(TAG, "mAlarmManager.setRepeating.");
            }
            this.hasSetAlarm = true;
            long origId = Binder.clearCallingIdentity();
            this.mAlarmManager.set(0, System.currentTimeMillis() + 86400000, this.mInternalAbePendingIntent);
            Binder.restoreCallingIdentity(origId);
        }
        return true;
    }

    public void copyingRecordClassifyResult(NotificationRecord newOne, NotificationRecord oldOne) {
        NotificationChannel channel = newOne.getChannel();
        boolean isSystemClassifyEnabled = this.mNotificationClassifyManager.areVivoCustomNotificationEnabled();
        boolean isPkgClassifyEnabled = this.mNotificationClassifyManager.areVivoCustomNotificationEnabledForPackage(newOne.getSbn().getPackageName(), newOne.getUid());
        boolean isChannelClassifyEnabled = channel.isAcceptNotificationClassifyManage();
        if (isSystemClassifyEnabled && isPkgClassifyEnabled && isChannelClassifyEnabled) {
            newOne.getSbn().setClassifyImportance(oldOne.getSbn().getClassifyImportance());
        } else {
            newOne.getSbn().setClassifyImportance(Integer.MIN_VALUE);
        }
    }

    public boolean isActionRemoveOrHide(Intent intent, boolean removingPackage) {
        if ("android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
            if (intent.getBooleanExtra("android.intent.extra.REMOVED_FOR_ALL_USERS", false)) {
                return intent.getBooleanExtra("android.intent.extra.DATA_REMOVED", false) && !intent.hasExtra("android.intent.extra.REPLACING");
            }
            String packageName = null;
            Uri packageData = intent.getData();
            if (packageData != null) {
                packageName = packageData.getSchemeSpecificPart();
            }
            if (!TextUtils.isEmpty(packageName)) {
                int userHandle = intent.getIntExtra("android.intent.extra.user_handle", this.mNotificationManagerService.getContext().getUserId());
                try {
                    ApplicationInfo ai = this.mPackageManager.getApplicationInfo(packageName, 256, userHandle);
                    if (ai != null) {
                        return false;
                    }
                    return removingPackage;
                } catch (RemoteException e) {
                    return removingPackage;
                }
            }
        }
        return removingPackage;
    }

    public int fixMustNotHaveFlagsWhenCancelingAllLocked(String pkg, int userId) {
        List<NotificationRecord> autoGroupNotificationRecord;
        String autoSummaryKey;
        int mustNotHaveFlags = 64;
        ArrayMap<String, String> summaries = (ArrayMap) this.mNotificationManagerService.mAutobundledSummaries.get(Integer.valueOf(userId));
        String autoSummaryGroupKey = null;
        if (summaries != null && (autoSummaryKey = summaries.get(pkg)) != null) {
            NotificationRecord autoSummaryRecord = this.mNotificationManagerService.findNotificationByKeyLocked(autoSummaryKey);
            autoSummaryGroupKey = autoSummaryRecord != null ? autoSummaryRecord.getGroupKey() : null;
        }
        if (autoSummaryGroupKey != null && (autoGroupNotificationRecord = this.mNotificationManagerService.findGroupNotificationsLocked(pkg, autoSummaryGroupKey, userId)) != null) {
            for (NotificationRecord r : autoGroupNotificationRecord) {
                if ((r.getSbn().getNotification().flags & 64) == 64) {
                    mustNotHaveFlags |= Consts.ProcessStates.FOCUS;
                }
            }
        }
        return mustNotHaveFlags;
    }

    public boolean vivoPlayVibration(NotificationRecord record, long[] vibration, boolean hasValidSound, Uri soundUri) {
        String pkg = record.getSbn().getPackageName();
        if ("com.android.mms.service".equals(pkg)) {
            String channelId = record.getChannel().getId();
            if (MMS_SLOT0_CHANNEL_ID.equals(channelId)) {
                return vibrateByState(record, vibration, hasValidSound, soundUri, this.mMessageVibrateSim1);
            }
            if (MMS_SLOT1_CHANNEL_ID.equals(channelId)) {
                return vibrateByState(record, vibration, hasValidSound, soundUri, this.mMessageVibrateSim2);
            }
        }
        if ("com.bbk.calendar".equals(pkg)) {
            return vibrateByState(record, vibration, hasValidSound, soundUri, this.mCalendarVibrate);
        }
        return vibrateByState(record, vibration, hasValidSound, soundUri, this.mNotificationVibrate);
    }

    private boolean vibrateByState(NotificationRecord record, long[] vibration, boolean hasValidSound, Uri soundUri, int vibrateState) {
        if (vibrateState != 0) {
            boolean isVibrateWithSoundSuccess = false;
            if (vibrateState == 2 && SUPPORTED_DYNAMIC_VIBRATION && !this.mNotificationManagerService.mAudioManager.isAudioFocusExclusive()) {
                isVibrateWithSoundSuccess = isVibrateWithTheSound(soundUri, record);
            } else if (vibrateState != 1) {
                isVibrateWithSoundSuccess = playVibrationFromInternalSetting(vibrateState);
            }
            if (!isVibrateWithSoundSuccess) {
                boolean buzz = this.mNotificationManagerService.playVibration(record, vibration, hasValidSound);
                return buzz;
            }
            return true;
        }
        VSlog.d(TAG, "none vibration");
        return false;
    }

    private boolean playVibrationFromInternalSetting(int vibrateState) {
        boolean playSuccessful = false;
        long[] vibrationTime = VibrationUtils.getVibrationTime(vibrateState);
        int[] vibrationAmplitude = VibrationUtils.getVibrationAmplitude(vibrateState);
        if (vibrationTime != null && vibrationAmplitude != null && vibrationTime.length > 0 && vibrationAmplitude.length > 0) {
            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(vibrationTime, vibrationAmplitude, -1);
            Vibrator vibrator = (Vibrator) this.mNotificationManagerService.getContext().getSystemService(Vibrator.class);
            if (vibrator != null) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder().setContentType(4).setUsage(5).build();
                vibrator.vibrate(vibrationEffect, audioAttributes);
                playSuccessful = true;
                if (NotificationManagerService.DBG) {
                    VSlog.d(TAG, "playing vibration from internal setting with state =" + vibrateState);
                }
            }
        }
        if (!playSuccessful) {
            VSlog.d(TAG, "play vibration from internal setting error with time or amplitude or vibrator is empty, vibrateState = " + vibrateState);
        }
        return playSuccessful;
    }

    private boolean isVibrateWithTheSound(final Uri soundUri, NotificationRecord record) {
        boolean isValidSound = (soundUri == null || Uri.EMPTY.equals(soundUri) || !isSystemSoundUri(soundUri)) ? false : true;
        boolean isCustomVibration = isRecordCustomVibration(record);
        boolean noVibrate = this.mNotificationVibrateIntensity == 0;
        if (isValidSound && !isCustomVibration && !noVibrate) {
            this.mAudioTitleForVibrate = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            if (this.mBlockHappened && !this.mSoundTitleHandler.getLooper().getQueue().isPolling()) {
                VSlog.d(TAG, "getting sound title already blocked");
                return false;
            }
            final CountDownLatch countDown = new CountDownLatch(1);
            Runnable findTitle = new Runnable() { // from class: com.android.server.notification.-$$Lambda$VivoNotificationManagerServiceImpl$vPpRIBDJoLrAo8D9CbefeMVowuw
                @Override // java.lang.Runnable
                public final void run() {
                    VivoNotificationManagerServiceImpl.this.lambda$isVibrateWithTheSound$1$VivoNotificationManagerServiceImpl(soundUri, countDown);
                }
            };
            this.mSoundTitleHandler.post(findTitle);
            boolean inTime = false;
            try {
                inTime = countDown.await(1L, TimeUnit.SECONDS);
            } catch (Exception e) {
                VSlog.e(TAG, "error awaiting");
            }
            if (!inTime) {
                VSlog.d(TAG, "getting sound title overtime");
                this.mSoundTitleHandler.removeCallbacks(findTitle);
                this.mBlockHappened = true;
                return false;
            }
            String soundTitle = this.mAudioTitleForVibrate;
            this.mBlockHappened = false;
            if (soundTitle == null || (soundTitle != null && soundTitle.isEmpty())) {
                if (NotificationManagerService.DBG) {
                    VSlog.d(TAG, "sound title is null or empty");
                    return false;
                }
                return false;
            }
            long vibrateMillis = -1;
            Vibrator vibrator = (Vibrator) this.mNotificationManagerService.getContext().getSystemService(Vibrator.class);
            Class clazz = vibrator.getClass();
            try {
                Method method = clazz.getDeclaredMethod("ringVibrate", String.class, Boolean.TYPE);
                if (method != null) {
                    vibrateMillis = ((Long) method.invoke(vibrator, soundTitle, false)).longValue();
                }
            } catch (Exception e2) {
                VSlog.d(TAG, "vibrate with sound failed" + e2.fillInStackTrace());
                vibrateMillis = -1;
            }
            if (vibrateMillis < 0) {
                if (NotificationManagerService.DBG) {
                    VSlog.d(TAG, "dynamic vibration fail");
                    return false;
                }
                return false;
            }
            VSlog.d(TAG, "vibrate with sound " + vibrateMillis + " ms");
            return true;
        } else if (NotificationManagerService.DBG) {
            VSlog.d(TAG, "valid sound or custom vibration.");
            return false;
        } else {
            return false;
        }
    }

    public /* synthetic */ void lambda$isVibrateWithTheSound$1$VivoNotificationManagerServiceImpl(Uri soundUri, CountDownLatch countDown) {
        long startTime = System.currentTimeMillis();
        Uri actualSoundUri = soundUri;
        if (RingtoneManager.isDefault(soundUri)) {
            actualSoundUri = RingtoneManager.getActualDefaultRingtoneUri(this.mNotificationManagerService.getContext(), RingtoneManager.getDefaultType(soundUri));
        }
        String title = getSoundTitleFromRingtone(actualSoundUri);
        if (System.currentTimeMillis() - startTime < 1000) {
            this.mAudioTitleForVibrate = title;
            this.mBlockHappened = false;
            countDown.countDown();
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:43:0x00d1, code lost:
        if (r4.isClosed() == false) goto L39;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private java.lang.String getSoundTitleFromRingtone(android.net.Uri r15) {
        /*
            r14 = this;
            java.lang.String r0 = "_display_name"
            r1 = 0
            java.lang.String r2 = "NotificationService"
            if (r15 == 0) goto L2f
            java.lang.String r3 = r15.getAuthority()
            boolean r4 = android.text.TextUtils.isEmpty(r3)
            if (r4 != 0) goto L2f
            java.lang.String r4 = "settings"
            boolean r4 = r4.equals(r3)
            if (r4 == 0) goto L2f
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r4 = "illegal uri with settings authority, uri is "
            r0.append(r4)
            r0.append(r15)
            java.lang.String r0 = r0.toString()
            vivo.util.VSlog.e(r2, r0)
            r0 = 0
            return r0
        L2f:
            boolean r3 = com.android.server.notification.VivoNotificationManagerServiceImpl.USE_TRACK_TITLE_SOUND
            r4 = 0
            r5 = 1
            if (r3 != 0) goto L39
            boolean r3 = com.android.server.notification.VivoNotificationManagerServiceImpl.IS_VOS
            if (r3 == 0) goto Le1
        L39:
            if (r15 == 0) goto Le1
            android.util.ArrayMap<android.net.Uri, java.lang.String> r3 = r14.mCacheSoundTitle
            boolean r3 = r3.containsKey(r15)
            if (r3 == 0) goto L53
            android.util.ArrayMap<android.net.Uri, java.lang.String> r3 = r14.mCacheSoundTitle
            java.lang.Object r3 = r3.get(r15)
            java.lang.CharSequence r3 = (java.lang.CharSequence) r3
            boolean r3 = android.text.TextUtils.isEmpty(r3)
            if (r3 != 0) goto L53
            r4 = r5
            goto L54
        L53:
        L54:
            r3 = r4
            r4 = 0
            if (r3 == 0) goto L62
            android.util.ArrayMap<android.net.Uri, java.lang.String> r0 = r14.mCacheSoundTitle
            java.lang.Object r0 = r0.get(r15)
            java.lang.String r0 = (java.lang.String) r0
            goto Ld4
        L62:
            com.android.server.notification.NotificationManagerService r6 = r14.mNotificationManagerService
            android.content.Context r6 = r6.getContext()
            android.content.ContentResolver r6 = r6.getContentResolver()
            java.lang.String[] r9 = new java.lang.String[]{r0}     // Catch: java.lang.Throwable -> La7 java.lang.IllegalArgumentException -> La9
            r10 = 0
            r11 = 0
            r12 = 0
            r13 = 0
            r7 = r6
            r8 = r15
            android.database.Cursor r7 = r7.query(r8, r9, r10, r11, r12, r13)     // Catch: java.lang.Throwable -> La7 java.lang.IllegalArgumentException -> La9
            r4 = r7
            if (r4 == 0) goto L9a
            int r7 = r4.getCount()     // Catch: java.lang.Throwable -> La7 java.lang.IllegalArgumentException -> La9
            if (r7 != r5) goto L9a
            r4.moveToFirst()     // Catch: java.lang.Throwable -> La7 java.lang.IllegalArgumentException -> La9
            int r0 = r4.getColumnIndex(r0)     // Catch: java.lang.Throwable -> La7 java.lang.IllegalArgumentException -> La9
            java.lang.String r5 = r4.getString(r0)     // Catch: java.lang.Throwable -> La7 java.lang.IllegalArgumentException -> La9
            r1 = r5
            boolean r5 = android.text.TextUtils.isEmpty(r1)     // Catch: java.lang.Throwable -> La7 java.lang.IllegalArgumentException -> La9
            if (r5 != 0) goto L9a
            android.util.ArrayMap<android.net.Uri, java.lang.String> r5 = r14.mCacheSoundTitle     // Catch: java.lang.Throwable -> La7 java.lang.IllegalArgumentException -> La9
            r5.put(r15, r1)     // Catch: java.lang.Throwable -> La7 java.lang.IllegalArgumentException -> La9
        L9a:
            if (r4 == 0) goto La5
            boolean r0 = r4.isClosed()
            if (r0 != 0) goto La5
        La2:
            r4.close()
        La5:
            r0 = r1
            goto Ld4
        La7:
            r0 = move-exception
            goto Ld5
        La9:
            r0 = move-exception
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> La7
            r5.<init>()     // Catch: java.lang.Throwable -> La7
            java.lang.String r7 = "failed to get sound title, source uri = "
            r5.append(r7)     // Catch: java.lang.Throwable -> La7
            r5.append(r15)     // Catch: java.lang.Throwable -> La7
            java.lang.String r7 = " e: "
            r5.append(r7)     // Catch: java.lang.Throwable -> La7
            java.lang.String r7 = r0.toString()     // Catch: java.lang.Throwable -> La7
            r5.append(r7)     // Catch: java.lang.Throwable -> La7
            java.lang.String r5 = r5.toString()     // Catch: java.lang.Throwable -> La7
            vivo.util.VSlog.e(r2, r5)     // Catch: java.lang.Throwable -> La7
            if (r4 == 0) goto La5
            boolean r0 = r4.isClosed()
            if (r0 != 0) goto La5
            goto La2
        Ld4:
            goto Leb
        Ld5:
            if (r4 == 0) goto Le0
            boolean r2 = r4.isClosed()
            if (r2 != 0) goto Le0
            r4.close()
        Le0:
            throw r0
        Le1:
            com.android.server.notification.NotificationManagerService r0 = r14.mNotificationManagerService
            android.content.Context r0 = r0.getContext()
            java.lang.String r0 = android.media.Ringtone.getTitle(r0, r15, r4, r5)
        Leb:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.notification.VivoNotificationManagerServiceImpl.getSoundTitleFromRingtone(android.net.Uri):java.lang.String");
    }

    private boolean isSystemSoundUri(Uri soundUri) {
        if (soundUri.getScheme() == null || !soundUri.getScheme().equals("content")) {
            return false;
        }
        int match = this.mSoundUriMatcher.match(soundUri);
        return match == 1 || match == 2;
    }

    private boolean isRecordCustomVibration(NotificationRecord record) {
        boolean isPreChannelsNotification = "miscellaneous".equals(record.getChannel().getId()) && record.mTargetSdkVersion < 26;
        if (isPreChannelsNotification) {
            boolean useDefaultVibrate = (record.getNotification().defaults & 2) != 0;
            if (useDefaultVibrate) {
                return false;
            }
            boolean isCustom = record.getNotification().vibrate != null;
            return isCustom;
        }
        boolean isCustom2 = record.getChannel().getVibrationPattern() != null;
        return isCustom2;
    }

    public void observeSetting() {
        this.mObserver.observe();
    }

    /* loaded from: classes.dex */
    private class VivoSettingObserver extends ContentObserver {
        private final Uri CALENDAR_VIBRATE_STATE_URI;
        private final Uri MESSAGE_SIM1_VIBRATE_STATE_URI;
        private final Uri MESSAGE_SIM2_VIBRATE_STATE_URI;
        private final Uri NOTIFICATION_VIBRATE_INTENSITY;
        private final Uri NOTIFICATION_VIBRATE_STATE_URI;
        private final Uri USER_SETUP_COMPLETE_URI;

        public VivoSettingObserver(Handler handler) {
            super(handler);
            this.NOTIFICATION_VIBRATE_STATE_URI = Settings.System.getUriFor("vibration_mode_notification");
            this.MESSAGE_SIM1_VIBRATE_STATE_URI = Settings.System.getUriFor("vibration_mode_message_sim1");
            this.MESSAGE_SIM2_VIBRATE_STATE_URI = Settings.System.getUriFor("vibration_mode_message_sim2");
            this.CALENDAR_VIBRATE_STATE_URI = Settings.System.getUriFor("vibration_mode_calendar");
            this.USER_SETUP_COMPLETE_URI = Settings.System.getUriFor("user_setup_complete");
            this.NOTIFICATION_VIBRATE_INTENSITY = Settings.System.getUriFor("notification_vibration_intensity");
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            ContentResolver resolver = VivoNotificationManagerServiceImpl.this.mNotificationManagerService.getContext().getContentResolver();
            if (uri == null || this.NOTIFICATION_VIBRATE_STATE_URI.equals(uri)) {
                VivoNotificationManagerServiceImpl vivoNotificationManagerServiceImpl = VivoNotificationManagerServiceImpl.this;
                vivoNotificationManagerServiceImpl.mNotificationVibrate = Settings.System.getInt(resolver, "vibration_mode_notification", vivoNotificationManagerServiceImpl.mNotificationVibrate);
            }
            if (uri == null || this.MESSAGE_SIM1_VIBRATE_STATE_URI.equals(uri)) {
                VivoNotificationManagerServiceImpl vivoNotificationManagerServiceImpl2 = VivoNotificationManagerServiceImpl.this;
                vivoNotificationManagerServiceImpl2.mMessageVibrateSim1 = Settings.System.getInt(resolver, "vibration_mode_message_sim1", vivoNotificationManagerServiceImpl2.mMessageVibrateSim1);
            }
            if (uri == null || this.MESSAGE_SIM2_VIBRATE_STATE_URI.equals(uri)) {
                VivoNotificationManagerServiceImpl vivoNotificationManagerServiceImpl3 = VivoNotificationManagerServiceImpl.this;
                vivoNotificationManagerServiceImpl3.mMessageVibrateSim2 = Settings.System.getInt(resolver, "vibration_mode_message_sim2", vivoNotificationManagerServiceImpl3.mMessageVibrateSim2);
            }
            if (uri == null || this.CALENDAR_VIBRATE_STATE_URI.equals(uri)) {
                VivoNotificationManagerServiceImpl vivoNotificationManagerServiceImpl4 = VivoNotificationManagerServiceImpl.this;
                vivoNotificationManagerServiceImpl4.mCalendarVibrate = Settings.System.getInt(resolver, "vibration_mode_calendar", vivoNotificationManagerServiceImpl4.mCalendarVibrate);
            }
            if (uri == null || this.USER_SETUP_COMPLETE_URI.equals(uri)) {
                VivoNotificationManagerServiceImpl.this.mNotificationWhiteListManager.updateUserSetupState(Settings.Secure.getIntForUser(resolver, "user_setup_complete", 0, -2));
            }
            if (uri == null || this.NOTIFICATION_VIBRATE_INTENSITY.equals(uri)) {
                VivoNotificationManagerServiceImpl.this.mNotificationVibrateIntensity = Settings.System.getInt(resolver, "notification_vibration_intensity", 2);
            }
        }

        public void observe() {
            ContentResolver resolver = VivoNotificationManagerServiceImpl.this.mNotificationManagerService.getContext().getContentResolver();
            resolver.registerContentObserver(this.NOTIFICATION_VIBRATE_STATE_URI, false, VivoNotificationManagerServiceImpl.this.mObserver, -1);
            resolver.registerContentObserver(this.MESSAGE_SIM1_VIBRATE_STATE_URI, false, VivoNotificationManagerServiceImpl.this.mObserver, -1);
            resolver.registerContentObserver(this.MESSAGE_SIM2_VIBRATE_STATE_URI, false, VivoNotificationManagerServiceImpl.this.mObserver, -1);
            resolver.registerContentObserver(this.CALENDAR_VIBRATE_STATE_URI, false, VivoNotificationManagerServiceImpl.this.mObserver, -1);
            resolver.registerContentObserver(this.NOTIFICATION_VIBRATE_INTENSITY, false, VivoNotificationManagerServiceImpl.this.mObserver, -1);
        }
    }

    public boolean isTwoSbnCustom(StatusBarNotification oldSbn, StatusBarNotification newSbn) {
        return oldSbn != null && oldSbn.getNotification().isCustomNotification() && oldSbn.getOverrideGroupKey() == null && newSbn.getNotification().isCustomNotification();
    }

    private void readSystemVibrationMode() {
        ContentResolver resolver = this.mNotificationManagerService.getContext().getContentResolver();
        this.mNotificationVibrate = Settings.System.getInt(resolver, "vibration_mode_notification", this.mNotificationVibrate);
        this.mMessageVibrateSim1 = Settings.System.getInt(resolver, "vibration_mode_message_sim1", this.mMessageVibrateSim1);
        this.mMessageVibrateSim2 = Settings.System.getInt(resolver, "vibration_mode_message_sim2", this.mMessageVibrateSim2);
        this.mCalendarVibrate = Settings.System.getInt(resolver, "vibration_mode_calendar", this.mCalendarVibrate);
    }

    public boolean shouldCancelForeground(NotificationRecord r, int callingUid) {
        return r.getSbn().getOverrideGroupKey() != null && r.getNotification().isCustomNotification() && isUidSystemUI(callingUid);
    }

    public boolean isNeedCancelGroupSummary(int reason, NotificationRecord r) {
        if (reason != 21 || !r.getNotification().isGroupSummary() || !r.getGroupKey().contains("ranker_group")) {
            return true;
        }
        List<NotificationRecord> autoGroupList = this.mNotificationManagerService.findGroupNotificationsLocked(r.getSbn().getPackageName(), r.getSbn().getGroupKey(), r.getSbn().getUserId());
        for (NotificationRecord temp : autoGroupList) {
            if (temp.getNotification().internalType == 1) {
                return false;
            }
        }
        return true;
    }

    public void maybeUpdateGroupTime(NotificationRecord child) {
        NotificationRecord summary;
        if (child.getNotification().isGroupSummary()) {
            return;
        }
        if (child.getSbn().isAppGroup()) {
            List<NotificationRecord> groupNotifications = this.mNotificationManagerService.findGroupNotificationsLocked(child.getSbn().getPackageName(), child.getGroupKey(), child.getUserId());
            for (NotificationRecord groupedNo : groupNotifications) {
                if (groupedNo.getNotification().isGroupSummary() && TextUtils.equals(groupedNo.getNotification().getGroup(), child.getNotification().getGroup())) {
                    updateGroupSummaryWhenChildComing(groupedNo, child);
                    return;
                }
            }
        } else if (this.mNotificationManagerService.mAutobundledSummaries.get(Integer.valueOf(child.getSbn().getUserId())) != null && ((ArrayMap) this.mNotificationManagerService.mAutobundledSummaries.get(Integer.valueOf(child.getSbn().getUserId()))).containsKey(child.getSbn().getPackageName())) {
            ArrayMap<String, String> summaries = (ArrayMap) this.mNotificationManagerService.mAutobundledSummaries.get(Integer.valueOf(child.getSbn().getUserId()));
            String key = summaries.get(child.getSbn().getPackageName());
            if (key != null && (summary = findNotificationByKey(key)) != null) {
                updateGroupSummaryWhenChildComing(summary, child);
            }
        }
    }

    private NotificationRecord findNotificationByKey(String key) {
        int N = this.mNotificationManagerService.mNotificationList.size();
        for (int i = 0; i < N; i++) {
            if (key.equals(((NotificationRecord) this.mNotificationManagerService.mNotificationList.get(i)).getKey())) {
                return (NotificationRecord) this.mNotificationManagerService.mNotificationList.get(i);
            }
        }
        int M = this.mNotificationManagerService.mEnqueuedNotifications.size();
        for (int i2 = 0; i2 < M; i2++) {
            if (key.equals(((NotificationRecord) this.mNotificationManagerService.mEnqueuedNotifications.get(i2)).getKey())) {
                return (NotificationRecord) this.mNotificationManagerService.mEnqueuedNotifications.get(i2);
            }
        }
        return null;
    }

    private void updateGroupSummaryWhenChildComing(final NotificationRecord groupSummary, NotificationRecord child) {
        final long newTime = child.getRankingTimeMs();
        if (newTime > groupSummary.getRankingTimeMs()) {
            groupSummary.resetRakingTimeMs(newTime);
            groupSummary.getSbn().getNotification().when = newTime;
            if (NotificationManagerService.DBG) {
                VSlog.d(TAG, "updating summary time pkg=" + groupSummary.getSbn().getPackageName() + " time = " + (System.currentTimeMillis() - newTime));
            }
            for (final ManagedServices.ManagedServiceInfo info : this.mListeners.getServices()) {
                if (info.component.getPackageName().equals(FaceUIState.PKG_SYSTEMUI)) {
                    this.mHandler.post(new Runnable() { // from class: com.android.server.notification.-$$Lambda$VivoNotificationManagerServiceImpl$lFdfTQ-xRztr75nI8aYt7nhKDYA
                        @Override // java.lang.Runnable
                        public final void run() {
                            VivoNotificationManagerServiceImpl.lambda$updateGroupSummaryWhenChildComing$2(info, groupSummary, newTime);
                        }
                    });
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$updateGroupSummaryWhenChildComing$2(ManagedServices.ManagedServiceInfo info, NotificationRecord groupSummary, long newTime) {
        INotificationListener listener = info.service;
        try {
            listener.onNotificationGroupSummaryTimeUpdate(groupSummary.getKey(), newTime);
        } catch (RemoteException ex) {
            VSlog.e(TAG, "unable to notify listener (updateGroupSummaryWhenChildComing): " + listener, ex);
        }
    }

    public void setProperTimeForSummary(StatusBarNotification summary, List<NotificationRecord> notificationRecords) {
        long properTime = 0;
        for (NotificationRecord r : notificationRecords) {
            if (summary.getPackageName().equals(r.getSbn().getPackageName()) && r.getRankingTimeMs() > properTime) {
                properTime = r.getRankingTimeMs();
            }
        }
        summary.getNotification().when = properTime;
    }

    public boolean isPushServiceCalling(int uid, String pkg) {
        int pushServiceUid = findPkgUidAsUser("com.vivo.pushservice", UserHandle.getUserId(uid));
        return (pushServiceUid == 0 || uid != pushServiceUid || "com.vivo.pushservice".equals(pkg)) ? false : true;
    }

    public int findPkgUidAsUser(String pkg, int userId) {
        try {
            int uid = this.mPackageManager.getPackageUid(pkg, 0, userId);
            return uid;
        } catch (Exception e) {
            VSlog.e(TAG, "find uidAsUser error :" + e.fillInStackTrace());
            return Integer.MIN_VALUE;
        }
    }

    public void notifyBadgeStateChange(String pkg, boolean enabled, int uid, boolean fromBadgeMenu) {
        if (IS_VOS) {
            int i = 1;
            if (!fromBadgeMenu) {
                enabled = enabled && this.mPreferencesHelper.canShowBadge(pkg, uid);
            }
            try {
                Intent launcherIntent = this.mNotificationManagerService.getContext().getPackageManager().getLaunchIntentForPackage(pkg);
                String mainClass = launcherIntent != null ? launcherIntent.getComponent().getClassName() : null;
                if (NotificationManagerService.DBG) {
                    VSlog.d(TAG, "notify badge state change");
                }
                Context context = this.mNotificationManagerService.getContext();
                Intent intent = new Intent("android.app.action.APP_BADGE_STATE_CHANGE");
                if (!enabled) {
                    i = 0;
                }
                context.sendBroadcastAsUser(intent.putExtra("APP_BADGE_ENABLE_STATE", i).putExtra("APP_BADGE_ENABLE_PKG", pkg).putExtra("APP_BADGE_ENABLE_CLASS", mainClass).putExtra("APP_BADGE_ENABLE_UID", UserHandle.of(UserHandle.getUserId(uid))).setPackage(PKG_LAUNCHER), UserHandle.of(UserHandle.getUserId(uid)), null);
            } catch (Exception e) {
                VSlog.e(TAG, "notify badge state change error");
            }
        }
    }

    public void recordDismissalState(int dismissalSurface, int dismissalSentiment, NotificationRecord r, int reason) {
        if (reason == 2) {
            r.recordDismissalSurface(dismissalSurface);
            r.recordDismissalSentiment(dismissalSentiment);
        }
    }

    public void requestNotificationPermission(String pkg) {
        int uid = Binder.getCallingUid();
        int userid = UserHandle.getUserId(uid);
        long identity = Binder.clearCallingIdentity();
        PackageManager pm = this.mNotificationManagerService.getContext().getPackageManager();
        try {
            try {
                String appName = pm.getApplicationLabel(pm.getApplicationInfoAsUser(pkg, 0, userid)).toString();
                Intent requestPermissionIntent = new Intent("com.android.notification.permission.action.FRONT");
                requestPermissionIntent.putExtra("pkg", pkg).putExtra("label", appName).putExtra("uid", uid).setFlags(268435456);
                this.mNotificationManagerService.getContext().startActivity(requestPermissionIntent);
            } catch (PackageManager.NameNotFoundException e) {
                VSlog.e(TAG, "can not find this application info", e);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public AudioAttributes fixAttributesInZenMode(int currentZenMode, NotificationRecord record) {
        if (currentZenMode != 0 && !record.isIntercepted()) {
            if (NotificationManagerService.DBG) {
                VSlog.d(TAG, "fixing bypass dnd flag for attributes");
            }
            AudioAttributes.Builder builder = new AudioAttributes.Builder(record.getAudioAttributes());
            builder.setFlags(64);
            return builder.build();
        }
        return record.getAudioAttributes();
    }

    public void setZenMode(ZenModeHelper zenModeHelper, int mode, Uri conditionId, String reason) {
        if (this.mIsCtsVInstalled) {
            zenModeHelper.setManualZenMode(mode, conditionId, (String) null, "setInterruptionFilterC");
        } else {
            zenModeHelper.setManualZenMode(mode, conditionId, (String) null, reason);
        }
    }

    public void updateCtsVInstallState(ZenModeHelper zenModeHelper, Intent intent, boolean removingPackage) {
        String action = intent.getAction();
        Uri uri = intent.getData();
        if (uri != null) {
            String pkgName = uri.getSchemeSpecificPart();
            if (!TextUtils.isEmpty(pkgName) && "com.android.cts.verifier".equals(pkgName)) {
                if (removingPackage) {
                    this.mIsCtsVInstalled = false;
                } else if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                    this.mIsCtsVInstalled = true;
                }
            }
        }
        zenModeHelper.getVivoInjectInstance().updateCtsVInstallState(this.mIsCtsVInstalled);
    }

    public boolean doNotDeleteVPushChannel(String channelId) {
        if ("vivo_push_channel".equals(channelId) || (channelId != null && channelId.startsWith("VPushChannel_"))) {
            int callingUid = Binder.getCallingUid();
            int pushServiceUid = findPkgUidAsUser("com.vivo.pushservice", UserHandle.getUserId(callingUid));
            if (callingUid != pushServiceUid) {
                if (NotificationManagerService.DBG) {
                    VSlog.d(TAG, "do not delete vPush channel");
                    return true;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public void onUserSwitched(int userId) {
        this.mNotificationWhiteListManager.onUserSwitched(userId);
    }

    public void onUserChanged(int userId, boolean added) {
        this.mNotificationWhiteListManager.onUserChanged(userId, added);
    }

    public void notifyAppSharePackageChanged(String packageName, int userId) {
        synchronized (this.mNotificationManagerService.mToastQueue) {
            this.mAppSharePackageName = packageName;
            this.mAppShareUserId = userId;
        }
    }

    public int adjustDisplayIdForAppShareLocked(int candidateDisplayId, String packageName, int callingPid, int callingUid) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            return MultiDisplayManager.isAppShareDisplayId(candidateDisplayId) ? candidateDisplayId : getDisplayIdForApp(packageName, UserHandle.getUserId(callingUid), callingPid, callingUid, candidateDisplayId);
        }
        return candidateDisplayId;
    }

    private int getDisplayIdForApp(String pkgName, int userId, int pid, int uid, int candidateDisplayId) {
        VSlog.i(TAG, "getDisplayIdForApp: pkgName = " + pkgName + ", userId = " + userId + ", pid = " + pid + ", uid = " + uid + ", mAppSharePackageName = " + this.mAppSharePackageName + ", mAppShareUserId = " + this.mAppShareUserId + ", candidateDisplayId = " + candidateDisplayId);
        if (TextUtils.isEmpty(this.mAppSharePackageName) || this.mAppShareUserId == -1) {
            return candidateDisplayId;
        }
        if (this.mAppSharePackageName.equals(pkgName) && this.mAppShareUserId == userId) {
            return 10086;
        }
        if (this.mVivoRatioControllerUtils.isImeApplication(this.mNotificationManagerService.getContext(), pid, uid) && MultiDisplayManager.isAppShareDisplayId(this.mVivoRatioControllerUtils.getCurrentInputMethodDisplayId())) {
            return 10086;
        }
        return candidateDisplayId;
    }

    public boolean targetDisplayNotExist(int displayId) {
        if (MultiDisplayManager.isAppShareDisplayId(displayId)) {
            if (this.mMultiDisplayManager == null) {
                this.mMultiDisplayManager = (MultiDisplayManager) this.mNotificationManagerService.getContext().getSystemService(MultiDisplayManager.class);
            }
            MultiDisplayManager multiDisplayManager = this.mMultiDisplayManager;
            if (multiDisplayManager != null && !multiDisplayManager.isAppShareDisplayExist()) {
                return true;
            }
            return false;
        }
        return false;
    }

    public int resolveTargetUidForVivoAre(PendingIntent pi, int userId, int defaultUid, String notificationPkg, boolean isOverrideNotification) {
        if (isOverrideNotification && "com.vivo.are".equals(notificationPkg) && pi != null) {
            try {
                String targetPkg = pi.getIntent().getPackage();
                if (!TextUtils.isEmpty(targetPkg)) {
                    return this.mPackageManager.getPackageUid(targetPkg, 0, userId);
                }
            } catch (Exception e) {
                VSlog.d(TAG, "can not find pkg");
            }
        }
        return defaultUid;
    }

    public boolean canNotificationBeep(NotificationRecord r) {
        return (this.mNotificationManagerService.mAudioManager.isAudioFocusExclusive() || this.mNotificationManagerService.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(r.getAudioAttributes())) == 0) ? false : true;
    }

    private void initWakeLock() {
        PowerManager powerManager = (PowerManager) this.mNotificationManagerService.getContext().getSystemService("power");
        if (powerManager != null) {
            this.mNotificationWakeLock = powerManager.newWakeLock(1, "notification_wake_lock");
        }
    }

    public void acquireWake(boolean assistantEnabled) {
        PowerManager.WakeLock wakeLock = this.mNotificationWakeLock;
        if (wakeLock != null) {
            wakeLock.acquire(assistantEnabled ? 300L : 200L);
        } else {
            initWakeLock();
        }
    }

    public boolean checkNotificationHasCanceled(NotificationRecord r) {
        return ((r.getSbn().getNotification().flags & Consts.ProcessStates.FOCUS) == 0 || r.getStats().getDismissalSurface() == -1 || r.getStats().getDismissalSentiment() == -1000) ? false : true;
    }

    public void executeDelayedCancelationsIfNeedLocked(boolean wasPosted, NotificationRecord r) {
        if (!wasPosted && r != null && this.mNotificationManagerService.mDelayedCancelations.get(r) != null) {
            Iterator it = ((ArrayList) this.mNotificationManagerService.mDelayedCancelations.get(r)).iterator();
            while (it.hasNext()) {
                this.mHandler.post((NotificationManagerService.CancelNotificationRunnable) it.next());
            }
            this.mNotificationManagerService.mDelayedCancelations.remove(r);
        }
    }

    public void mayUpdateTimeOutAlarmLocked(String key, boolean removed, int reason) {
        if (!TextUtils.isEmpty(key)) {
            if (removed && this.mRequestedTimeoutNotificationKeys.contains(key)) {
                if (reason != 19) {
                    PendingIntent pi = PendingIntent.getBroadcast(this.mNotificationManagerService.getContext(), 1, new Intent(NotificationManagerService.ACTION_NOTIFICATION_TIMEOUT).setPackage(VivoPermissionUtils.OS_PKG).setData(new Uri.Builder().scheme("timeout").appendPath(key).build()).addFlags(268435456).putExtra("key", key), Dataspace.RANGE_FULL);
                    this.mAlarmManager.cancel(pi);
                }
                this.mRequestedTimeoutNotificationKeys.remove(key);
                return;
            }
            this.mRequestedTimeoutNotificationKeys.add(key);
        }
    }

    public String addAutomaticZenRuleGranted(ZenModeHelper helper, AutomaticZenRule zenRule) {
        Preconditions.checkNotNull(zenRule, "automaticZenRule is null");
        Preconditions.checkNotNull(zenRule.getName(), "Name is null");
        if (zenRule.getOwner() == null && zenRule.getConfigurationActivity() == null) {
            throw new NullPointerException("Rule must have a conditionproviderservice and/or configuration activity");
        }
        Preconditions.checkNotNull(zenRule.getConditionId(), "ConditionId is null");
        if (zenRule.getZenPolicy() != null && zenRule.getInterruptionFilter() != 2) {
            throw new IllegalArgumentException("ZenPolicy is only applicable to INTERRUPTION_FILTER_PRIORITY filters");
        }
        if (!enforceHasZenModePermission(Binder.getCallingUid())) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            String id = helper.addAutomaticZenRule(zenRule, "addAutomaticZenRule");
            return id;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean updateAutomaticZenRuleGranted(ZenModeHelper helper, String id, AutomaticZenRule zenRule) {
        Preconditions.checkNotNull(zenRule, "automaticZenRule is null");
        Preconditions.checkNotNull(zenRule.getName(), "Name is null");
        if (zenRule.getOwner() == null && zenRule.getConfigurationActivity() == null) {
            throw new NullPointerException("Rule must have a conditionproviderservice and/or configuration activity");
        }
        Preconditions.checkNotNull(zenRule.getConditionId(), "ConditionId is null");
        if (!enforceHasZenModePermission(Binder.getCallingUid())) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            boolean result = helper.updateAutomaticZenRule(id, zenRule, "updateAutomaticZenRule");
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean removeAutomaticZenRuleGranted(ZenModeHelper helper, String id) {
        Preconditions.checkNotNull(id, "Id is null");
        if (!enforceHasZenModePermission(Binder.getCallingUid())) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            boolean result = helper.removeAutomaticZenRule(id, "removeAutomaticZenRule");
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public AutomaticZenRule getAutomaticZenRuleGranted(ZenModeHelper helper, String id) {
        Preconditions.checkNotNull(id, "Id is null");
        if (!enforceHasZenModePermission(Binder.getCallingUid())) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            AutomaticZenRule result = helper.getAutomaticZenRule(id);
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public List<AutomaticZenRule> getZenRulesGranted(ZenModeHelper mZenModeHelper) {
        if (!enforceHasZenModePermission(Binder.getCallingUid())) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            List<AutomaticZenRule> result = mZenModeHelper.getAllAutomaticZenRules();
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean enforceHasZenModePermission(int callingUid) {
        int res = this.mNotificationManagerService.getContext().checkPermission("com.vivo.notification.MANAGE_ZEN_POLICY", Binder.getCallingPid(), callingUid);
        return res == 0;
    }

    public boolean hasOngoingNotification(String pkgName, int uid) {
        Notification n;
        if (!UserHandle.isApp(uid) || TextUtils.isEmpty(pkgName)) {
            return false;
        }
        synchronized (this.mNotificationManagerService.mNotificationLock) {
            int N = this.mNotificationManagerService.mNotificationList.size();
            for (int i = 0; i < N; i++) {
                NotificationRecord nr = (NotificationRecord) this.mNotificationManagerService.mNotificationList.get(i);
                StatusBarNotification sbn = nr.getSbn();
                if (sbn.getUid() == uid && pkgName.equals(sbn.getPackageName()) && (n = sbn.getNotification()) != null && (n.flags & 2) != 0) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean shouldIgnoreCrash(String pkg, int crashedUid, String crashedChannelId, int userId, int callingPid) {
        NotificationRecord r;
        if (this.mNotificationExceptionCatchEnabled && (r = findFgsNotificationByChannel(pkg, crashedChannelId, userId)) != null) {
            long identity = Binder.clearCallingIdentity();
            try {
                Notification replacedNotification = generateDefaultForegroundNotification(r.getNotification(), pkg, userId);
                this.mNotificationManagerService.enqueueNotificationInternal(pkg, pkg, crashedUid, callingPid, r.getSbn().getTag(), r.getSbn().getId(), replacedNotification, userId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        this.mPreferencesHelper.getVivoInjectInstance().markApplicationCrashedByDeletingFgsChannel(pkg, crashedUid, crashedChannelId);
        VSlog.e(TAG, "Cannot delete channel with a foreground service notification");
        return this.mNotificationExceptionCatchEnabled;
    }

    private NotificationRecord findFgsNotificationByChannel(String pkg, String crashedChannelId, int userId) {
        synchronized (this.mNotificationManagerService.mNotificationLock) {
            Iterator it = this.mNotificationManagerService.mNotificationList.iterator();
            while (it.hasNext()) {
                NotificationRecord r = (NotificationRecord) it.next();
                if (r.getSbn().getPackageName().equals(pkg) && TextUtils.equals(r.getChannel().getId(), crashedChannelId) && r.getSbn().getUserId() == userId && (r.getNotification().flags & 64) != 0) {
                    return r;
                }
            }
            Iterator it2 = this.mNotificationManagerService.mEnqueuedNotifications.iterator();
            while (it2.hasNext()) {
                NotificationRecord r2 = (NotificationRecord) it2.next();
                if (r2.getSbn().getPackageName().equals(pkg) && TextUtils.equals(r2.getChannel().getId(), crashedChannelId) && r2.getSbn().getUserId() == userId && (r2.getNotification().flags & 64) != 0) {
                    return r2;
                }
            }
            return null;
        }
    }

    public Notification generateDefaultForegroundNotification(Notification originNotification, String pkg, int userId) {
        Context ctx;
        ApplicationInfo applicationInfo;
        Context systemContext = this.mNotificationManagerService.getContext();
        Context ctx2 = null;
        try {
            ctx2 = systemContext.createPackageContextAsUser(pkg, 0, new UserHandle(userId));
            ApplicationInfo applicationInfo2 = this.mPackageManager.getApplicationInfo(pkg, 0, userId);
            ctx = ctx2;
            applicationInfo = applicationInfo2;
        } catch (PackageManager.NameNotFoundException | RemoteException e) {
            VSlog.e(TAG, "failed to get package info e:" + e.getMessage());
            ctx = ctx2;
            applicationInfo = null;
        }
        if (ctx == null || applicationInfo == null) {
            return originNotification;
        }
        CharSequence appName = applicationInfo.loadLabel(ctx.getPackageManager());
        Notification.Builder builder = new Notification.Builder(ctx, originNotification.getChannelId());
        builder.setFlag(64, true);
        Intent runningIntent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        runningIntent.setData(Uri.fromParts("package", pkg, null));
        PendingIntent pi = PendingIntent.getActivityAsUser(systemContext, 0, runningIntent, Dataspace.RANGE_FULL, null, UserHandle.of(userId));
        builder.setSmallIcon(originNotification.getSmallIcon());
        builder.setContentTitle(systemContext.getString(17039670, appName));
        builder.setContentText(systemContext.getString(17039669, appName));
        builder.setContentIntent(pi);
        builder.setOnlyAlertOnce(true);
        if (originNotification.getSmallIcon() == null) {
            if (applicationInfo.icon == 0) {
                return null;
            }
            builder.setSmallIcon(applicationInfo.icon);
        }
        return builder.build();
    }
}