package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseSetArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.StatLogger;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.usage.AppStandbyInternal;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class AppStateTracker {
    private static final boolean DEBUG = false;
    private static final String TAG = "AppStateTracker";
    static final int TARGET_OP = 70;
    ActivityManagerInternal mActivityManagerInternal;
    AppOpsManager mAppOpsManager;
    IAppOpsService mAppOpsService;
    AppStandbyInternal mAppStandbyInternal;
    boolean mBatterySaverEnabled;
    private final Context mContext;
    FeatureFlagsObserver mFlagsObserver;
    boolean mForceAllAppStandbyForSmallBattery;
    boolean mForceAllAppsStandby;
    boolean mForcedAppStandbyEnabled;
    private final MyHandler mHandler;
    IActivityManager mIActivityManager;
    boolean mIsPluggedIn;
    PowerManagerInternal mPowerManagerInternal;
    private int[] mPowerWhitelistedAllAppIds;
    StandbyTracker mStandbyTracker;
    boolean mStarted;
    private int[] mTempWhitelistedAppIds;
    private final Object mLock = new Object();
    final ArraySet<Pair<Integer, String>> mRunAnyRestrictedPackages = new ArraySet<>();
    final SparseBooleanArray mActiveUids = new SparseBooleanArray();
    final SparseBooleanArray mForegroundUids = new SparseBooleanArray();
    private int[] mPowerWhitelistedUserAppIds = new int[0];
    private final SparseSetArray<String> mExemptedPackages = new SparseSetArray<>();
    final ArraySet<Listener> mListeners = new ArraySet<>();
    private final StatLogger mStatLogger = new StatLogger(new String[]{"UID_FG_STATE_CHANGED", "UID_ACTIVE_STATE_CHANGED", "RUN_ANY_CHANGED", "ALL_UNWHITELISTED", "ALL_WHITELIST_CHANGED", "TEMP_WHITELIST_CHANGED", "EXEMPT_CHANGED", "FORCE_ALL_CHANGED", "FORCE_APP_STANDBY_FEATURE_FLAG_CHANGED", "IS_UID_ACTIVE_CACHED", "IS_UID_ACTIVE_RAW"});

    /* loaded from: classes.dex */
    interface Stats {
        public static final int ALL_UNWHITELISTED = 3;
        public static final int ALL_WHITELIST_CHANGED = 4;
        public static final int EXEMPT_CHANGED = 6;
        public static final int FORCE_ALL_CHANGED = 7;
        public static final int FORCE_APP_STANDBY_FEATURE_FLAG_CHANGED = 8;
        public static final int IS_UID_ACTIVE_CACHED = 9;
        public static final int IS_UID_ACTIVE_RAW = 10;
        public static final int RUN_ANY_CHANGED = 2;
        public static final int TEMP_WHITELIST_CHANGED = 5;
        public static final int UID_ACTIVE_STATE_CHANGED = 1;
        public static final int UID_FG_STATE_CHANGED = 0;
    }

    /* loaded from: classes.dex */
    class FeatureFlagsObserver extends ContentObserver {
        FeatureFlagsObserver() {
            super(null);
        }

        void register() {
            AppStateTracker.this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("forced_app_standby_enabled"), false, this);
            AppStateTracker.this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("forced_app_standby_for_small_battery_enabled"), false, this);
        }

        boolean isForcedAppStandbyEnabled() {
            return AppStateTracker.this.injectGetGlobalSettingInt("forced_app_standby_enabled", 1) == 1;
        }

        boolean isForcedAppStandbyForSmallBatteryEnabled() {
            return AppStateTracker.this.injectGetGlobalSettingInt("forced_app_standby_for_small_battery_enabled", 0) == 1;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.Global.getUriFor("forced_app_standby_enabled").equals(uri)) {
                boolean enabled = isForcedAppStandbyEnabled();
                synchronized (AppStateTracker.this.mLock) {
                    if (AppStateTracker.this.mForcedAppStandbyEnabled == enabled) {
                        return;
                    }
                    AppStateTracker.this.mForcedAppStandbyEnabled = enabled;
                    AppStateTracker.this.mHandler.notifyForcedAppStandbyFeatureFlagChanged();
                }
            } else if (Settings.Global.getUriFor("forced_app_standby_for_small_battery_enabled").equals(uri)) {
                boolean enabled2 = isForcedAppStandbyForSmallBatteryEnabled();
                synchronized (AppStateTracker.this.mLock) {
                    if (AppStateTracker.this.mForceAllAppStandbyForSmallBattery == enabled2) {
                        return;
                    }
                    AppStateTracker.this.mForceAllAppStandbyForSmallBattery = enabled2;
                    AppStateTracker.this.updateForceAllAppStandbyState();
                }
            } else {
                Slog.w(AppStateTracker.TAG, "Unexpected feature flag uri encountered: " + uri);
            }
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Listener {
        /* JADX INFO: Access modifiers changed from: private */
        public void onRunAnyAppOpsChanged(AppStateTracker sender, int uid, String packageName) {
            updateJobsForUidPackage(uid, packageName, sender.isUidActive(uid));
            if (!sender.areAlarmsRestricted(uid, packageName, false)) {
                unblockAlarmsForUidPackage(uid, packageName);
            } else if (!sender.areAlarmsRestricted(uid, packageName, true)) {
                unblockAllUnrestrictedAlarms();
            }
            if (!sender.isRunAnyInBackgroundAppOpsAllowed(uid, packageName)) {
                Slog.v(AppStateTracker.TAG, "Package " + packageName + SliceClientPermissions.SliceAuthority.DELIMITER + uid + " toggled into fg service restriction");
                stopForegroundServicesForUidPackage(uid, packageName);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onUidForegroundStateChanged(AppStateTracker sender, int uid) {
            onUidForeground(uid, sender.isUidInForeground(uid));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onUidActiveStateChanged(AppStateTracker sender, int uid) {
            boolean isActive = sender.isUidActive(uid);
            updateJobsForUid(uid, isActive);
            if (isActive) {
                unblockAlarmsForUid(uid);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onPowerSaveUnwhitelisted(AppStateTracker sender) {
            updateAllJobs();
            unblockAllUnrestrictedAlarms();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onPowerSaveWhitelistedChanged(AppStateTracker sender) {
            updateAllJobs();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onTempPowerSaveWhitelistChanged(AppStateTracker sender) {
            updateAllJobs();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onExemptChanged(AppStateTracker sender) {
            updateAllJobs();
            unblockAllUnrestrictedAlarms();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onForceAllAppsStandbyChanged(AppStateTracker sender) {
            updateAllJobs();
            if (!sender.isForceAllAppsStandbyEnabled()) {
                unblockAllUnrestrictedAlarms();
            }
        }

        public void updateAllJobs() {
        }

        public void updateJobsForUid(int uid, boolean isNowActive) {
        }

        public void updateJobsForUidPackage(int uid, String packageName, boolean isNowActive) {
        }

        public void stopForegroundServicesForUidPackage(int uid, String packageName) {
        }

        public void unblockAllUnrestrictedAlarms() {
        }

        public void unblockAlarmsForUid(int uid) {
        }

        public void unblockAlarmsForUidPackage(int uid, String packageName) {
        }

        public void onUidForeground(int uid, boolean foreground) {
        }
    }

    public AppStateTracker(Context context, Looper looper) {
        int[] iArr = new int[0];
        this.mPowerWhitelistedAllAppIds = iArr;
        this.mTempWhitelistedAppIds = iArr;
        this.mContext = context;
        this.mHandler = new MyHandler(looper);
    }

    public void onSystemServicesReady() {
        synchronized (this.mLock) {
            if (this.mStarted) {
                return;
            }
            this.mStarted = true;
            IActivityManager injectIActivityManager = injectIActivityManager();
            Objects.requireNonNull(injectIActivityManager);
            this.mIActivityManager = injectIActivityManager;
            ActivityManagerInternal injectActivityManagerInternal = injectActivityManagerInternal();
            Objects.requireNonNull(injectActivityManagerInternal);
            this.mActivityManagerInternal = injectActivityManagerInternal;
            AppOpsManager injectAppOpsManager = injectAppOpsManager();
            Objects.requireNonNull(injectAppOpsManager);
            this.mAppOpsManager = injectAppOpsManager;
            IAppOpsService injectIAppOpsService = injectIAppOpsService();
            Objects.requireNonNull(injectIAppOpsService);
            this.mAppOpsService = injectIAppOpsService;
            PowerManagerInternal injectPowerManagerInternal = injectPowerManagerInternal();
            Objects.requireNonNull(injectPowerManagerInternal);
            this.mPowerManagerInternal = injectPowerManagerInternal;
            AppStandbyInternal injectAppStandbyInternal = injectAppStandbyInternal();
            Objects.requireNonNull(injectAppStandbyInternal);
            this.mAppStandbyInternal = injectAppStandbyInternal;
            FeatureFlagsObserver featureFlagsObserver = new FeatureFlagsObserver();
            this.mFlagsObserver = featureFlagsObserver;
            featureFlagsObserver.register();
            this.mForcedAppStandbyEnabled = this.mFlagsObserver.isForcedAppStandbyEnabled();
            this.mForceAllAppStandbyForSmallBattery = this.mFlagsObserver.isForcedAppStandbyForSmallBatteryEnabled();
            StandbyTracker standbyTracker = new StandbyTracker();
            this.mStandbyTracker = standbyTracker;
            this.mAppStandbyInternal.addListener(standbyTracker);
            try {
                this.mIActivityManager.registerUidObserver(new UidObserver(), 15, -1, (String) null);
                this.mAppOpsService.startWatchingMode(70, (String) null, new AppOpsWatcher());
            } catch (RemoteException e) {
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_REMOVED");
            filter.addAction("android.intent.action.BATTERY_CHANGED");
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            this.mContext.registerReceiver(new MyReceiver(), filter);
            refreshForcedAppStandbyUidPackagesLocked();
            this.mPowerManagerInternal.registerLowPowerModeObserver(11, new Consumer() { // from class: com.android.server.-$$Lambda$AppStateTracker$zzioY8jvEm-1GnJ13CUiQGauPEE
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AppStateTracker.this.lambda$onSystemServicesReady$0$AppStateTracker((PowerSaveState) obj);
                }
            });
            this.mBatterySaverEnabled = this.mPowerManagerInternal.getLowPowerState(11).batterySaverEnabled;
            updateForceAllAppStandbyState();
        }
    }

    public /* synthetic */ void lambda$onSystemServicesReady$0$AppStateTracker(PowerSaveState state) {
        synchronized (this.mLock) {
            this.mBatterySaverEnabled = state.batterySaverEnabled;
            updateForceAllAppStandbyState();
        }
    }

    AppOpsManager injectAppOpsManager() {
        return (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
    }

    IAppOpsService injectIAppOpsService() {
        return IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
    }

    IActivityManager injectIActivityManager() {
        return ActivityManager.getService();
    }

    ActivityManagerInternal injectActivityManagerInternal() {
        return (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
    }

    PowerManagerInternal injectPowerManagerInternal() {
        return (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
    }

    AppStandbyInternal injectAppStandbyInternal() {
        return (AppStandbyInternal) LocalServices.getService(AppStandbyInternal.class);
    }

    boolean isSmallBatteryDevice() {
        return ActivityManager.isSmallBatteryDevice();
    }

    int injectGetGlobalSettingInt(String key, int def) {
        return Settings.Global.getInt(this.mContext.getContentResolver(), key, def);
    }

    private void refreshForcedAppStandbyUidPackagesLocked() {
        this.mRunAnyRestrictedPackages.clear();
        List<AppOpsManager.PackageOps> ops = this.mAppOpsManager.getPackagesForOps(new int[]{70});
        if (ops == null) {
            return;
        }
        int size = ops.size();
        for (int i = 0; i < size; i++) {
            AppOpsManager.PackageOps pkg = ops.get(i);
            List<AppOpsManager.OpEntry> entries = ops.get(i).getOps();
            for (int j = 0; j < entries.size(); j++) {
                AppOpsManager.OpEntry ent = entries.get(j);
                if (ent.getOp() == 70 && ent.getMode() != 0) {
                    this.mRunAnyRestrictedPackages.add(Pair.create(Integer.valueOf(pkg.getUid()), pkg.getPackageName()));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForceAllAppStandbyState() {
        synchronized (this.mLock) {
            if (this.mForceAllAppStandbyForSmallBattery && isSmallBatteryDevice()) {
                toggleForceAllAppsStandbyLocked(!this.mIsPluggedIn);
            } else {
                toggleForceAllAppsStandbyLocked(this.mBatterySaverEnabled);
            }
        }
    }

    private void toggleForceAllAppsStandbyLocked(boolean enable) {
        if (enable == this.mForceAllAppsStandby) {
            return;
        }
        this.mForceAllAppsStandby = enable;
        this.mHandler.notifyForceAllAppsStandbyChanged();
    }

    private int findForcedAppStandbyUidPackageIndexLocked(int uid, String packageName) {
        int size = this.mRunAnyRestrictedPackages.size();
        if (size > 8) {
            return this.mRunAnyRestrictedPackages.indexOf(Pair.create(Integer.valueOf(uid), packageName));
        }
        for (int i = 0; i < size; i++) {
            Pair<Integer, String> pair = this.mRunAnyRestrictedPackages.valueAt(i);
            if (((Integer) pair.first).intValue() == uid && packageName.equals(pair.second)) {
                return i;
            }
        }
        return -1;
    }

    boolean isRunAnyRestrictedLocked(int uid, String packageName) {
        return findForcedAppStandbyUidPackageIndexLocked(uid, packageName) >= 0;
    }

    boolean updateForcedAppStandbyUidPackageLocked(int uid, String packageName, boolean restricted) {
        int index = findForcedAppStandbyUidPackageIndexLocked(uid, packageName);
        boolean wasRestricted = index >= 0;
        if (wasRestricted == restricted) {
            return false;
        }
        if (restricted) {
            this.mRunAnyRestrictedPackages.add(Pair.create(Integer.valueOf(uid), packageName));
        } else {
            this.mRunAnyRestrictedPackages.removeAt(index);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean addUidToArray(SparseBooleanArray array, int uid) {
        if (UserHandle.isCore(uid) || array.get(uid)) {
            return false;
        }
        array.put(uid, true);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean removeUidFromArray(SparseBooleanArray array, int uid, boolean remove) {
        if (!UserHandle.isCore(uid) && array.get(uid)) {
            if (remove) {
                array.delete(uid);
                return true;
            }
            array.put(uid, false);
            return true;
        }
        return false;
    }

    /* loaded from: classes.dex */
    private final class UidObserver extends IUidObserver.Stub {
        private UidObserver() {
        }

        public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
            AppStateTracker.this.mHandler.onUidStateChanged(uid, procState);
        }

        public void onUidActive(int uid) {
            AppStateTracker.this.mHandler.onUidActive(uid);
        }

        public void onUidGone(int uid, boolean disabled) {
            AppStateTracker.this.mHandler.onUidGone(uid, disabled);
        }

        public void onUidIdle(int uid, boolean disabled) {
            AppStateTracker.this.mHandler.onUidIdle(uid, disabled);
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }
    }

    /* loaded from: classes.dex */
    private final class AppOpsWatcher extends IAppOpsCallback.Stub {
        private AppOpsWatcher() {
        }

        public void opChanged(int op, int uid, String packageName) throws RemoteException {
            boolean restricted = false;
            try {
                restricted = AppStateTracker.this.mAppOpsService.checkOperation(70, uid, packageName) != 0;
            } catch (RemoteException e) {
            }
            synchronized (AppStateTracker.this.mLock) {
                if (AppStateTracker.this.updateForcedAppStandbyUidPackageLocked(uid, packageName, restricted)) {
                    AppStateTracker.this.mHandler.notifyRunAnyAppOpsChanged(uid, packageName);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private final class MyReceiver extends BroadcastReceiver {
        private MyReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_REMOVED".equals(intent.getAction())) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (userId > 0) {
                    AppStateTracker.this.mHandler.doUserRemoved(userId);
                    return;
                }
                return;
            }
            if ("android.intent.action.BATTERY_CHANGED".equals(intent.getAction())) {
                synchronized (AppStateTracker.this.mLock) {
                    AppStateTracker.this.mIsPluggedIn = intent.getIntExtra("plugged", 0) != 0;
                }
                AppStateTracker.this.updateForceAllAppStandbyState();
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(intent.getAction()) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                int userId2 = intent.getIntExtra("android.intent.extra.user_handle", -1);
                String pkgName = intent.getData().getSchemeSpecificPart();
                if (AppStateTracker.this.mExemptedPackages.remove(userId2, pkgName)) {
                    AppStateTracker.this.mHandler.notifyExemptChanged();
                }
            }
        }
    }

    /* loaded from: classes.dex */
    final class StandbyTracker extends AppStandbyInternal.AppIdleStateChangeListener {
        StandbyTracker() {
        }

        public void onAppIdleStateChanged(String packageName, int userId, boolean idle, int bucket, int reason) {
            synchronized (AppStateTracker.this.mLock) {
                boolean changed = bucket == 5 ? AppStateTracker.this.mExemptedPackages.add(userId, packageName) : AppStateTracker.this.mExemptedPackages.remove(userId, packageName);
                if (changed) {
                    AppStateTracker.this.mHandler.notifyExemptChanged();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Listener[] cloneListeners() {
        Listener[] listenerArr;
        synchronized (this.mLock) {
            listenerArr = (Listener[]) this.mListeners.toArray(new Listener[this.mListeners.size()]);
        }
        return listenerArr;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        private static final int MSG_ALL_UNWHITELISTED = 4;
        private static final int MSG_ALL_WHITELIST_CHANGED = 5;
        private static final int MSG_EXEMPT_CHANGED = 10;
        private static final int MSG_FORCE_ALL_CHANGED = 7;
        private static final int MSG_FORCE_APP_STANDBY_FEATURE_FLAG_CHANGED = 9;
        private static final int MSG_ON_UID_ACTIVE = 12;
        private static final int MSG_ON_UID_GONE = 13;
        private static final int MSG_ON_UID_IDLE = 14;
        private static final int MSG_ON_UID_STATE_CHANGED = 11;
        private static final int MSG_RUN_ANY_CHANGED = 3;
        private static final int MSG_TEMP_WHITELIST_CHANGED = 6;
        private static final int MSG_UID_ACTIVE_STATE_CHANGED = 0;
        private static final int MSG_UID_FG_STATE_CHANGED = 1;
        private static final int MSG_USER_REMOVED = 8;

        public MyHandler(Looper looper) {
            super(looper);
        }

        public void notifyUidActiveStateChanged(int uid) {
            obtainMessage(0, uid, 0).sendToTarget();
        }

        public void notifyUidForegroundStateChanged(int uid) {
            obtainMessage(1, uid, 0).sendToTarget();
        }

        public void notifyRunAnyAppOpsChanged(int uid, String packageName) {
            obtainMessage(3, uid, 0, packageName).sendToTarget();
        }

        public void notifyAllUnwhitelisted() {
            removeMessages(4);
            obtainMessage(4).sendToTarget();
        }

        public void notifyAllWhitelistChanged() {
            removeMessages(5);
            obtainMessage(5).sendToTarget();
        }

        public void notifyTempWhitelistChanged() {
            removeMessages(6);
            obtainMessage(6).sendToTarget();
        }

        public void notifyForceAllAppsStandbyChanged() {
            removeMessages(7);
            obtainMessage(7).sendToTarget();
        }

        public void notifyForcedAppStandbyFeatureFlagChanged() {
            removeMessages(9);
            obtainMessage(9).sendToTarget();
        }

        public void notifyExemptChanged() {
            removeMessages(10);
            obtainMessage(10).sendToTarget();
        }

        public void doUserRemoved(int userId) {
            obtainMessage(8, userId, 0).sendToTarget();
        }

        public void onUidStateChanged(int uid, int procState) {
            obtainMessage(11, uid, procState).sendToTarget();
        }

        public void onUidActive(int uid) {
            obtainMessage(12, uid, 0).sendToTarget();
        }

        public void onUidGone(int uid, boolean disabled) {
            obtainMessage(13, uid, disabled ? 1 : 0).sendToTarget();
        }

        public void onUidIdle(int uid, boolean disabled) {
            obtainMessage(14, uid, disabled ? 1 : 0).sendToTarget();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            Listener[] cloneListeners;
            if (msg.what != 8) {
                synchronized (AppStateTracker.this.mLock) {
                    if (AppStateTracker.this.mStarted) {
                        AppStateTracker sender = AppStateTracker.this;
                        long start = AppStateTracker.this.mStatLogger.getTime();
                        int i = 0;
                        switch (msg.what) {
                            case 0:
                                Listener[] cloneListeners2 = AppStateTracker.this.cloneListeners();
                                int length = cloneListeners2.length;
                                while (i < length) {
                                    Listener l = cloneListeners2[i];
                                    l.onUidActiveStateChanged(sender, msg.arg1);
                                    i++;
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(1, start);
                                return;
                            case 1:
                                for (Listener l2 : AppStateTracker.this.cloneListeners()) {
                                    l2.onUidForegroundStateChanged(sender, msg.arg1);
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(0, start);
                                return;
                            case 2:
                            default:
                                return;
                            case 3:
                                Listener[] cloneListeners3 = AppStateTracker.this.cloneListeners();
                                int length2 = cloneListeners3.length;
                                while (i < length2) {
                                    Listener l3 = cloneListeners3[i];
                                    l3.onRunAnyAppOpsChanged(sender, msg.arg1, (String) msg.obj);
                                    i++;
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(2, start);
                                return;
                            case 4:
                                Listener[] cloneListeners4 = AppStateTracker.this.cloneListeners();
                                int length3 = cloneListeners4.length;
                                while (i < length3) {
                                    Listener l4 = cloneListeners4[i];
                                    l4.onPowerSaveUnwhitelisted(sender);
                                    i++;
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(3, start);
                                return;
                            case 5:
                                Listener[] cloneListeners5 = AppStateTracker.this.cloneListeners();
                                int length4 = cloneListeners5.length;
                                while (i < length4) {
                                    Listener l5 = cloneListeners5[i];
                                    l5.onPowerSaveWhitelistedChanged(sender);
                                    i++;
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(4, start);
                                return;
                            case 6:
                                Listener[] cloneListeners6 = AppStateTracker.this.cloneListeners();
                                int length5 = cloneListeners6.length;
                                while (i < length5) {
                                    Listener l6 = cloneListeners6[i];
                                    l6.onTempPowerSaveWhitelistChanged(sender);
                                    i++;
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(5, start);
                                return;
                            case 7:
                                Listener[] cloneListeners7 = AppStateTracker.this.cloneListeners();
                                int length6 = cloneListeners7.length;
                                while (i < length6) {
                                    Listener l7 = cloneListeners7[i];
                                    l7.onForceAllAppsStandbyChanged(sender);
                                    i++;
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(7, start);
                                return;
                            case 8:
                                AppStateTracker.this.handleUserRemoved(msg.arg1);
                                return;
                            case 9:
                                synchronized (AppStateTracker.this.mLock) {
                                    if (AppStateTracker.this.mForcedAppStandbyEnabled || AppStateTracker.this.mForceAllAppsStandby) {
                                        unblockAlarms = false;
                                    }
                                }
                                Listener[] cloneListeners8 = AppStateTracker.this.cloneListeners();
                                int length7 = cloneListeners8.length;
                                while (i < length7) {
                                    Listener l8 = cloneListeners8[i];
                                    l8.updateAllJobs();
                                    if (unblockAlarms) {
                                        l8.unblockAllUnrestrictedAlarms();
                                    }
                                    i++;
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(8, start);
                                return;
                            case 10:
                                Listener[] cloneListeners9 = AppStateTracker.this.cloneListeners();
                                int length8 = cloneListeners9.length;
                                while (i < length8) {
                                    Listener l9 = cloneListeners9[i];
                                    l9.onExemptChanged(sender);
                                    i++;
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(6, start);
                                return;
                            case 11:
                                handleUidStateChanged(msg.arg1, msg.arg2);
                                return;
                            case 12:
                                handleUidActive(msg.arg1);
                                return;
                            case 13:
                                int i2 = msg.arg1;
                                unblockAlarms = msg.arg1 != 0;
                                handleUidGone(i2, unblockAlarms);
                                return;
                            case 14:
                                int i3 = msg.arg1;
                                unblockAlarms = msg.arg1 != 0;
                                handleUidIdle(i3, unblockAlarms);
                                return;
                        }
                    }
                    return;
                }
            }
            AppStateTracker.this.handleUserRemoved(msg.arg1);
        }

        public void handleUidStateChanged(int uid, int procState) {
            synchronized (AppStateTracker.this.mLock) {
                if (procState > 6) {
                    if (AppStateTracker.removeUidFromArray(AppStateTracker.this.mForegroundUids, uid, false)) {
                        AppStateTracker.this.mHandler.notifyUidForegroundStateChanged(uid);
                    }
                } else if (AppStateTracker.addUidToArray(AppStateTracker.this.mForegroundUids, uid)) {
                    AppStateTracker.this.mHandler.notifyUidForegroundStateChanged(uid);
                }
            }
        }

        public void handleUidActive(int uid) {
            synchronized (AppStateTracker.this.mLock) {
                if (AppStateTracker.addUidToArray(AppStateTracker.this.mActiveUids, uid)) {
                    AppStateTracker.this.mHandler.notifyUidActiveStateChanged(uid);
                }
            }
        }

        public void handleUidGone(int uid, boolean disabled) {
            removeUid(uid, true);
        }

        public void handleUidIdle(int uid, boolean disabled) {
            removeUid(uid, false);
        }

        private void removeUid(int uid, boolean remove) {
            synchronized (AppStateTracker.this.mLock) {
                if (AppStateTracker.removeUidFromArray(AppStateTracker.this.mActiveUids, uid, remove)) {
                    AppStateTracker.this.mHandler.notifyUidActiveStateChanged(uid);
                }
                if (AppStateTracker.removeUidFromArray(AppStateTracker.this.mForegroundUids, uid, remove)) {
                    AppStateTracker.this.mHandler.notifyUidForegroundStateChanged(uid);
                }
            }
        }
    }

    void handleUserRemoved(int removedUserId) {
        synchronized (this.mLock) {
            for (int i = this.mRunAnyRestrictedPackages.size() - 1; i >= 0; i--) {
                Pair<Integer, String> pair = this.mRunAnyRestrictedPackages.valueAt(i);
                int uid = ((Integer) pair.first).intValue();
                int userId = UserHandle.getUserId(uid);
                if (userId == removedUserId) {
                    this.mRunAnyRestrictedPackages.removeAt(i);
                }
            }
            cleanUpArrayForUser(this.mActiveUids, removedUserId);
            cleanUpArrayForUser(this.mForegroundUids, removedUserId);
            this.mExemptedPackages.remove(removedUserId);
        }
    }

    private void cleanUpArrayForUser(SparseBooleanArray array, int removedUserId) {
        for (int i = array.size() - 1; i >= 0; i--) {
            int uid = array.keyAt(i);
            int userId = UserHandle.getUserId(uid);
            if (userId == removedUserId) {
                array.removeAt(i);
            }
        }
    }

    public void setPowerSaveWhitelistAppIds(int[] powerSaveWhitelistExceptIdleAppIdArray, int[] powerSaveWhitelistUserAppIdArray, int[] tempWhitelistAppIdArray) {
        synchronized (this.mLock) {
            int[] previousWhitelist = this.mPowerWhitelistedAllAppIds;
            int[] previousTempWhitelist = this.mTempWhitelistedAppIds;
            this.mPowerWhitelistedAllAppIds = powerSaveWhitelistExceptIdleAppIdArray;
            this.mTempWhitelistedAppIds = tempWhitelistAppIdArray;
            this.mPowerWhitelistedUserAppIds = powerSaveWhitelistUserAppIdArray;
            if (isAnyAppIdUnwhitelisted(previousWhitelist, powerSaveWhitelistExceptIdleAppIdArray)) {
                this.mHandler.notifyAllUnwhitelisted();
            } else if (!Arrays.equals(previousWhitelist, this.mPowerWhitelistedAllAppIds)) {
                this.mHandler.notifyAllWhitelistChanged();
            }
            if (!Arrays.equals(previousTempWhitelist, this.mTempWhitelistedAppIds)) {
                this.mHandler.notifyTempWhitelistChanged();
            }
        }
    }

    static boolean isAnyAppIdUnwhitelisted(int[] prevArray, int[] newArray) {
        boolean prevFinished;
        boolean newFinished;
        int i1 = 0;
        int i2 = 0;
        while (true) {
            prevFinished = i1 >= prevArray.length;
            newFinished = i2 >= newArray.length;
            if (prevFinished || newFinished) {
                break;
            }
            int a1 = prevArray[i1];
            int a2 = newArray[i2];
            if (a1 == a2) {
                i1++;
                i2++;
            } else if (a1 < a2) {
                return true;
            } else {
                i2++;
            }
        }
        if (prevFinished) {
            return false;
        }
        return newFinished;
    }

    public void addListener(Listener listener) {
        synchronized (this.mLock) {
            this.mListeners.add(listener);
        }
    }

    public boolean areAlarmsRestricted(int uid, String packageName, boolean isExemptOnBatterySaver) {
        return isRestricted(uid, packageName, false, isExemptOnBatterySaver);
    }

    public boolean areJobsRestricted(int uid, String packageName, boolean hasForegroundExemption) {
        return isRestricted(uid, packageName, true, hasForegroundExemption);
    }

    public boolean areForegroundServicesRestricted(int uid, String packageName) {
        boolean isRunAnyRestrictedLocked;
        synchronized (this.mLock) {
            isRunAnyRestrictedLocked = isRunAnyRestrictedLocked(uid, packageName);
        }
        return isRunAnyRestrictedLocked;
    }

    private boolean isRestricted(int uid, String packageName, boolean useTempWhitelistToo, boolean exemptOnBatterySaver) {
        if (isUidActive(uid)) {
            return false;
        }
        synchronized (this.mLock) {
            int appId = UserHandle.getAppId(uid);
            if (ArrayUtils.contains(this.mPowerWhitelistedAllAppIds, appId)) {
                return false;
            }
            if (useTempWhitelistToo && ArrayUtils.contains(this.mTempWhitelistedAppIds, appId)) {
                return false;
            }
            if (this.mForcedAppStandbyEnabled && isRunAnyRestrictedLocked(uid, packageName)) {
                return true;
            }
            if (exemptOnBatterySaver) {
                return false;
            }
            int userId = UserHandle.getUserId(uid);
            if (this.mAppStandbyInternal.isAppIdleEnabled() && !this.mAppStandbyInternal.isInParole() && this.mExemptedPackages.contains(userId, packageName)) {
                return false;
            }
            return this.mForceAllAppsStandby;
        }
    }

    public boolean isUidActive(int uid) {
        boolean z;
        if (UserHandle.isCore(uid)) {
            return true;
        }
        synchronized (this.mLock) {
            z = this.mActiveUids.get(uid);
        }
        return z;
    }

    public boolean isUidActiveSynced(int uid) {
        if (isUidActive(uid)) {
            return true;
        }
        long start = this.mStatLogger.getTime();
        boolean ret = this.mActivityManagerInternal.isUidActive(uid);
        this.mStatLogger.logDurationStat(10, start);
        return ret;
    }

    public boolean isUidInForeground(int uid) {
        boolean z;
        if (UserHandle.isCore(uid)) {
            return true;
        }
        synchronized (this.mLock) {
            z = this.mForegroundUids.get(uid);
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isForceAllAppsStandbyEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mForceAllAppsStandby;
        }
        return z;
    }

    public boolean isRunAnyInBackgroundAppOpsAllowed(int uid, String packageName) {
        boolean z;
        synchronized (this.mLock) {
            z = !isRunAnyRestrictedLocked(uid, packageName);
        }
        return z;
    }

    public boolean isUidPowerSaveWhitelisted(int uid) {
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mPowerWhitelistedAllAppIds, UserHandle.getAppId(uid));
        }
        return contains;
    }

    public boolean isUidPowerSaveUserWhitelisted(int uid) {
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mPowerWhitelistedUserAppIds, UserHandle.getAppId(uid));
        }
        return contains;
    }

    public boolean isUidTempPowerSaveWhitelisted(int uid) {
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mTempWhitelistedAppIds, UserHandle.getAppId(uid));
        }
        return contains;
    }

    @Deprecated
    public void dump(PrintWriter pw, String prefix) {
        dump(new IndentingPrintWriter(pw, "  ").setIndent(prefix));
    }

    public void dump(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("Forced App Standby Feature enabled: " + this.mForcedAppStandbyEnabled);
            pw.print("Force all apps standby: ");
            pw.println(isForceAllAppsStandbyEnabled());
            pw.print("Small Battery Device: ");
            pw.println(isSmallBatteryDevice());
            pw.print("Force all apps standby for small battery device: ");
            pw.println(this.mForceAllAppStandbyForSmallBattery);
            pw.print("Plugged In: ");
            pw.println(this.mIsPluggedIn);
            pw.print("Active uids: ");
            dumpUids(pw, this.mActiveUids);
            pw.print("Foreground uids: ");
            dumpUids(pw, this.mForegroundUids);
            pw.print("Except-idle + user whitelist appids: ");
            pw.println(Arrays.toString(this.mPowerWhitelistedAllAppIds));
            pw.print("User whitelist appids: ");
            pw.println(Arrays.toString(this.mPowerWhitelistedUserAppIds));
            pw.print("Temp whitelist appids: ");
            pw.println(Arrays.toString(this.mTempWhitelistedAppIds));
            pw.println("Exempted packages:");
            pw.increaseIndent();
            for (int i = 0; i < this.mExemptedPackages.size(); i++) {
                pw.print("User ");
                pw.print(this.mExemptedPackages.keyAt(i));
                pw.println();
                pw.increaseIndent();
                for (int j = 0; j < this.mExemptedPackages.sizeAt(i); j++) {
                    pw.print((String) this.mExemptedPackages.valueAt(i, j));
                    pw.println();
                }
                pw.decreaseIndent();
            }
            pw.decreaseIndent();
            pw.println();
            pw.println("Restricted packages:");
            pw.increaseIndent();
            Iterator<Pair<Integer, String>> it = this.mRunAnyRestrictedPackages.iterator();
            while (it.hasNext()) {
                Pair<Integer, String> uidAndPackage = it.next();
                pw.print(UserHandle.formatUid(((Integer) uidAndPackage.first).intValue()));
                pw.print(" ");
                pw.print((String) uidAndPackage.second);
                pw.println();
            }
            pw.decreaseIndent();
            this.mStatLogger.dump(pw);
        }
    }

    private void dumpUids(PrintWriter pw, SparseBooleanArray array) {
        pw.print("[");
        String sep = "";
        for (int i = 0; i < array.size(); i++) {
            if (array.valueAt(i)) {
                pw.print(sep);
                pw.print(UserHandle.formatUid(array.keyAt(i)));
                sep = " ";
            }
        }
        pw.println("]");
    }

    public void dumpProto(ProtoOutputStream proto, long fieldId) {
        int[] iArr;
        int[] iArr2;
        int[] iArr3;
        synchronized (this.mLock) {
            long token = proto.start(fieldId);
            proto.write(1133871366157L, this.mForcedAppStandbyEnabled);
            proto.write(1133871366145L, isForceAllAppsStandbyEnabled());
            proto.write(1133871366150L, isSmallBatteryDevice());
            proto.write(1133871366151L, this.mForceAllAppStandbyForSmallBattery);
            proto.write(1133871366152L, this.mIsPluggedIn);
            for (int i = 0; i < this.mActiveUids.size(); i++) {
                if (this.mActiveUids.valueAt(i)) {
                    proto.write(2220498092034L, this.mActiveUids.keyAt(i));
                }
            }
            for (int i2 = 0; i2 < this.mForegroundUids.size(); i2++) {
                if (this.mForegroundUids.valueAt(i2)) {
                    proto.write(2220498092043L, this.mForegroundUids.keyAt(i2));
                }
            }
            for (int appId : this.mPowerWhitelistedAllAppIds) {
                proto.write(2220498092035L, appId);
            }
            for (int appId2 : this.mPowerWhitelistedUserAppIds) {
                proto.write(2220498092044L, appId2);
            }
            for (int appId3 : this.mTempWhitelistedAppIds) {
                proto.write(2220498092036L, appId3);
            }
            for (int i3 = 0; i3 < this.mExemptedPackages.size(); i3++) {
                for (int j = 0; j < this.mExemptedPackages.sizeAt(i3); j++) {
                    long token2 = proto.start(2246267895818L);
                    proto.write(1120986464257L, this.mExemptedPackages.keyAt(i3));
                    proto.write(1138166333442L, (String) this.mExemptedPackages.valueAt(i3, j));
                    proto.end(token2);
                }
            }
            Iterator<Pair<Integer, String>> it = this.mRunAnyRestrictedPackages.iterator();
            while (it.hasNext()) {
                Pair<Integer, String> uidAndPackage = it.next();
                long token22 = proto.start(2246267895813L);
                proto.write(1120986464257L, ((Integer) uidAndPackage.first).intValue());
                proto.write(1138166333442L, (String) uidAndPackage.second);
                proto.end(token22);
            }
            this.mStatLogger.dumpProto(proto, 1146756268041L);
            proto.end(token);
        }
    }
}