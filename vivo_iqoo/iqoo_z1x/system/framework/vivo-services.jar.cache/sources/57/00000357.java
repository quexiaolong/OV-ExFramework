package com.android.server.notification;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.FtBuild;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.SparseArray;
import com.vivo.common.VivoCollectData;
import com.vivo.face.common.data.Constants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoZenModeHelperImpl implements IVivoZenModeHelper {
    private static final String KEY_BLOCK_NOTIFICATION_FOR_GAME_MODE = "block_notification_sound_vibration";
    private static final String KEY_COMPETITION_MODE_ENABLED_UPDATE = "enabled_competition_mode";
    private static final String KEY_CURRENT_GAME_PACKAGE = "current_game_package";
    static final String TAG = "ZenModeHelper";
    private static final int ZEN_MODE_NOTIFICATION_ID = 999;
    private static final String ZEN_MODE_SWITCH_EVENT_ID = "1023";
    private static final String ZEN_MODE_SWITCH_EVENT_LABEL = "102370";
    private Context mContext;
    private boolean mIsSupportGameMode_8_1;
    private ZenModeHelper mZenModeHelper;
    private String recordReasonOfEvaluateZenMode;
    static final boolean DEBUG = ZenModeHelper.DEBUG;
    private static final String KEY_ALLOW_INTERCEPTION = "priority_interruptions_state";
    private static final Uri ALLOW_INTERCEPTION = Settings.System.getUriFor(KEY_ALLOW_INTERCEPTION);
    private final Uri ZEN_GROUP = Settings.Global.getUriFor("zen_mode_contacts_group");
    private final Uri DRIVE_MODE = Settings.System.getUriFor("drive_mode_enabled");
    private final Uri SHIELD_NOTIFICATION = Settings.System.getUriFor("shield_notification_reminder_enabled");
    private final Uri ZEN_MESSAGE_GROUP = Settings.Global.getUriFor("zen_mode_messages_group");
    private final Uri COMPETITION_MODE = Settings.System.getUriFor(KEY_COMPETITION_MODE_ENABLED_UPDATE);
    private final Uri CURRENT_GAME_PACKAGE = Settings.System.getUriFor("current_game_package");
    private final Uri BLOCK_NOTIFICATION_FOR_GAME_MODE = Settings.System.getUriFor(KEY_BLOCK_NOTIFICATION_FOR_GAME_MODE);
    private boolean mNotInterruptDuringDrive = false;
    private boolean mNotInterruptDuringGame = false;
    private boolean mZenModeEnabled = false;
    private final BroadcastReceiver mPackageReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.VivoZenModeHelperImpl.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals("android.intent.action.PACKAGE_ADDED")) {
                String pkgName = intent.getData().getSchemeSpecificPart();
                if ("com.vivo.gamecube".equals(pkgName)) {
                    VivoZenModeHelperImpl vivoZenModeHelperImpl = VivoZenModeHelperImpl.this;
                    boolean isSupport = vivoZenModeHelperImpl.isSupportGameMode_8_1(vivoZenModeHelperImpl.mContext);
                    if (isSupport != VivoZenModeHelperImpl.this.mIsSupportGameMode_8_1) {
                        VivoZenModeHelperImpl.this.mIsSupportGameMode_8_1 = isSupport;
                        VSlog.d(VivoZenModeHelperImpl.TAG, "game mode is support 8.1 " + VivoZenModeHelperImpl.this.mIsSupportGameMode_8_1);
                    }
                }
            }
        }
    };
    boolean mIsCtsVInstalled = false;

    public VivoZenModeHelperImpl(ZenModeHelper zenModeHelper, Context context) {
        this.mIsSupportGameMode_8_1 = false;
        this.mZenModeHelper = zenModeHelper;
        this.mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mPackageReceiver, UserHandle.ALL, filter, null, null);
        this.mIsSupportGameMode_8_1 = isSupportGameMode_8_1(this.mContext);
    }

    private void collectZenmodeData(boolean zenModeEnabled) {
        VivoCollectData collectData = VivoCollectData.getInstance(this.mContext);
        HashMap<String, String> params = new HashMap<>();
        String str = zenModeEnabled ? "start_time" : "end_time";
        params.put(str, System.currentTimeMillis() + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        collectData.writeData(ZEN_MODE_SWITCH_EVENT_ID, ZEN_MODE_SWITCH_EVENT_LABEL, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params);
    }

    public void onUserSwitched() {
        this.mNotInterruptDuringDrive = computeDriveMode();
        synchronized (this.mZenModeHelper.mConfig) {
            NotificationManager.Policy policy = this.mZenModeHelper.mConsolidatedPolicy;
            boolean z = true;
            if (Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_ALLOW_INTERCEPTION, 1, -2) != 0) {
                z = false;
            }
            policy.mNotAllowInterception = z;
        }
    }

    public void evaluateZenMode(String reason, boolean setRingerMode, int zen, int zenBefore) {
        this.mNotInterruptDuringDrive = computeDriveMode();
        this.mNotInterruptDuringGame = computeGameMode();
        if (DEBUG) {
            VSlog.d(TAG, "DEBUG_ZENMODE: new zen=" + zen + " old zen=" + zenBefore + ", drive mode = " + this.mNotInterruptDuringDrive + ", game mode = " + this.mNotInterruptDuringGame);
        }
        boolean zenModeEnabled = zen != 0;
        VSlog.d(TAG, "reason = " + reason + " zenModeEnabled = " + zenModeEnabled + " mZenModeEnabled = " + this.mZenModeEnabled);
        if (!"init".equals(reason) && ("onSystemReady".equals(reason) || this.mZenModeEnabled != zenModeEnabled)) {
            updateZenModeNotification(zenModeEnabled);
        }
        this.mZenModeEnabled = zenModeEnabled;
        this.recordReasonOfEvaluateZenMode = reason;
    }

    private void updateZenModeNotification(boolean zenModeEnabled) {
        collectZenmodeData(zenModeEnabled);
    }

    public void observe(ContentResolver resolver, ContentObserver contentObserver) {
        resolver.registerContentObserver(this.ZEN_GROUP, false, contentObserver);
        resolver.registerContentObserver(this.DRIVE_MODE, false, contentObserver, -1);
        resolver.registerContentObserver(this.SHIELD_NOTIFICATION, false, contentObserver, -1);
        resolver.registerContentObserver(this.ZEN_MESSAGE_GROUP, false, contentObserver);
        resolver.registerContentObserver(this.COMPETITION_MODE, false, contentObserver, -1);
        resolver.registerContentObserver(this.CURRENT_GAME_PACKAGE, false, contentObserver, -1);
        resolver.registerContentObserver(this.BLOCK_NOTIFICATION_FOR_GAME_MODE, false, contentObserver, -1);
        resolver.registerContentObserver(ALLOW_INTERCEPTION, false, contentObserver, -1);
    }

    public void update(Uri uri, ZenModeConfig mConfig, SparseArray<ZenModeConfig> mConfigs) {
        if (this.ZEN_GROUP.equals(uri)) {
            long Groupid = Settings.Global.getLong(this.mContext.getContentResolver(), "zen_mode_contacts_group", -1L);
            if (mConfig.groupid != Groupid) {
                VSlog.d(TAG, "DEBUG_ZENMODE_GROUP:update groupid from " + mConfig.groupid + " to " + Groupid);
                mConfig.groupid = Groupid;
                mConfigs.put(mConfig.user, mConfig);
                this.mZenModeHelper.dispatchOnConfigChanged();
            }
        }
        if (this.DRIVE_MODE.equals(uri) || this.SHIELD_NOTIFICATION.equals(uri)) {
            this.mNotInterruptDuringDrive = computeDriveMode();
        }
        if (this.ZEN_MESSAGE_GROUP.equals(uri)) {
            long messageGroupId = Settings.Global.getLong(this.mContext.getContentResolver(), "zen_mode_messages_group", -1L);
            if (mConfig.messageGroupId != messageGroupId) {
                VSlog.d(TAG, "update messageGroupId from " + mConfig.messageGroupId + " to " + messageGroupId);
                mConfig.messageGroupId = messageGroupId;
                mConfigs.put(mConfig.user, mConfig);
                this.mZenModeHelper.dispatchOnConfigChanged();
            }
        }
        if (this.COMPETITION_MODE.equals(uri) || this.CURRENT_GAME_PACKAGE.equals(uri) || this.BLOCK_NOTIFICATION_FOR_GAME_MODE.equals(uri)) {
            this.mNotInterruptDuringGame = computeGameMode();
        }
        if (ALLOW_INTERCEPTION.equals(uri)) {
            this.mZenModeHelper.mConsolidatedPolicy.mNotAllowInterception = Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_ALLOW_INTERCEPTION, 1, -2) == 0;
        }
    }

    public boolean shouldIntercept() {
        if (this.mNotInterruptDuringDrive) {
            VSlog.d(TAG, "Do not intercept during drive ");
            return true;
        }
        return false;
    }

    public boolean shouldInterceptByGameMode() {
        if (this.mNotInterruptDuringGame) {
            VSlog.d(TAG, "Do not intercept during game mode");
            return true;
        }
        return false;
    }

    private boolean computeDriveMode() {
        boolean driveModeEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "drive_mode_enabled", 0, -2) == 1;
        boolean shieldNotificationEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "shield_notification_reminder_enabled", 0, -2) == 1;
        return driveModeEnabled && shieldNotificationEnabled;
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mNotInterruptDuringDrive=");
        pw.print(this.mNotInterruptDuringDrive);
    }

    public boolean isAlarmAndMusicAffectedByRingerMode() {
        String str = this.recordReasonOfEvaluateZenMode;
        return str != null && str.equals("setInterruptionFilterC");
    }

    public void readXmlForZenModeConfigUpgrade(ZenModeConfig config, ZenModeConfig mDefaultConfig) {
        if (config.version < 8) {
            ZenModeConfig defaultConfig = mDefaultConfig.copy();
            defaultConfig.user = config.user;
            defaultConfig.areChannelsBypassingDnd = config.areChannelsBypassingDnd;
            defaultConfig.groupid = config.groupid;
            defaultConfig.messageGroupId = config.messageGroupId;
            defaultConfig.manualRule = config.manualRule;
            defaultConfig.automaticRules = config.automaticRules;
        }
    }

    private boolean computeGameMode() {
        ContentResolver resolver = this.mContext.getContentResolver();
        if (this.mIsSupportGameMode_8_1) {
            boolean res = Settings.System.getIntForUser(resolver, KEY_BLOCK_NOTIFICATION_FOR_GAME_MODE, 0, -2) == 1;
            return res;
        }
        String currentRunningGame = Settings.System.getStringForUser(resolver, "current_game_package", -2);
        return !TextUtils.isEmpty(currentRunningGame) && isCompetitionModeEnabled(resolver, currentRunningGame);
    }

    private static boolean isCompetitionModeEnabled(ContentResolver resolver, String pkgName) {
        String competitionGames = Settings.System.getString(resolver, KEY_COMPETITION_MODE_ENABLED_UPDATE);
        List<String> competitionList = getArrayListFromLongString(competitionGames);
        return competitionList.contains(pkgName);
    }

    private static List<String> getArrayListFromLongString(String longString) {
        String[] strTemp;
        List<String> result = new ArrayList<>();
        if (longString != null && (strTemp = longString.split(":")) != null && strTemp.length > 0) {
            for (String str : strTemp) {
                if (!result.contains(str)) {
                    result.add(str);
                }
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSupportGameMode_8_1(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo("com.vivo.gamecube", 128);
            if (applicationInfo == null || applicationInfo.metaData == null) {
                return false;
            }
            boolean isSupport = applicationInfo.metaData.getBoolean("is_support_gamecube_8.1", false);
            return isSupport;
        } catch (Exception e) {
            VSlog.d(TAG, e.toString());
            return false;
        }
    }

    public boolean hasCtsVInstall() {
        return this.mIsCtsVInstalled;
    }

    public void updateCtsVInstallState(boolean installed) {
        this.mIsCtsVInstalled = installed;
    }

    public void resetOsFlag() {
        ZenModeHelper.IS_NOT_TIER_VOS = FtBuild.getTierLevel() == 0 || !FtBuild.getOsName().equalsIgnoreCase("vos");
    }
}