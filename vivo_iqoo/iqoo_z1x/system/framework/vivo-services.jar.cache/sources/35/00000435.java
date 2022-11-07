package com.android.server.policy.key;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioSystem;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.policy.VivoPolicyConstant;
import com.vivo.common.utils.VLog;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class VivoAIKeyExtend {
    public static final String AIKEY_DISABLE = "aikey_disable_setting";
    private static final String DISABLE_SMART_KEY_FOR_GAME_MODE = "disable_smart_key_on";
    public static final String ENABLED_SHIELD_SMART_KEY = "enabled_shield_smart_key";
    public static final String GAME_CURRENT_PACKAGE = "current_game_package";
    public static final String GAME_DISTURB_ENABLED = "game_do_not_disturb";
    private static final String KEY_COMPETITION_MODE_ENABLED_UPDATE = "enabled_competition_mode";
    private Context mContext;
    private boolean mIsSupportGameMode_8_1;
    private ArrayList<String> mList;
    private volatile boolean isAIKeyDisable = false;
    private boolean mIsSetupWizard = false;
    private volatile boolean isGameDisturb = false;
    private String mCurrentGamePackage = null;
    private boolean mIsCompetitionModeEnabled = false;
    private boolean mDisableByGameMode = false;
    private final BroadcastReceiver mPackageReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.key.VivoAIKeyExtend.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals("android.intent.action.PACKAGE_ADDED")) {
                String pkgName = intent.getData().getSchemeSpecificPart();
                if ("com.vivo.gamecube".equals(pkgName)) {
                    VivoAIKeyExtend vivoAIKeyExtend = VivoAIKeyExtend.this;
                    boolean isSupport = vivoAIKeyExtend.isSupportGameMode_8_1(vivoAIKeyExtend.mContext);
                    if (isSupport != VivoAIKeyExtend.this.mIsSupportGameMode_8_1) {
                        VivoAIKeyExtend.this.mIsSupportGameMode_8_1 = isSupport;
                        VLog.d("VivoAIKeyHandler", "game mode is support 8.1 " + VivoAIKeyExtend.this.mIsSupportGameMode_8_1);
                    }
                }
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public VivoAIKeyExtend(Context context, Handler handler) {
        this.mIsSupportGameMode_8_1 = false;
        this.mContext = context;
        ContentObserver contentObserver = new ContentObserver(handler) { // from class: com.android.server.policy.key.VivoAIKeyExtend.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                VivoAIKeyExtend.this.updateSettings();
            }
        };
        updateSettings();
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(Settings.System.CONTENT_URI, AIKEY_DISABLE);
        contentResolver.registerContentObserver(uri, true, contentObserver, -1);
        Uri uri2 = Uri.withAppendedPath(Settings.Secure.CONTENT_URI, "user_setup_complete");
        contentResolver.registerContentObserver(uri2, true, contentObserver, -1);
        registerForGameMode(this.mContext, contentObserver);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mPackageReceiver, UserHandle.ALL, filter, null, null);
        this.mIsSupportGameMode_8_1 = isSupportGameMode_8_1(this.mContext);
        Uri uri3 = Uri.withAppendedPath(Settings.System.CONTENT_URI, DISABLE_SMART_KEY_FOR_GAME_MODE);
        contentResolver.registerContentObserver(uri3, true, contentObserver, -1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSettings() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mList = userModifiedGameModeSettings(this.mContext, ENABLED_SHIELD_SMART_KEY);
        this.isGameDisturb = Settings.System.getIntForUser(contentResolver, GAME_DISTURB_ENABLED, 0, -2) == 1;
        String stringForUser = Settings.System.getStringForUser(contentResolver, GAME_CURRENT_PACKAGE, -2);
        this.mCurrentGamePackage = stringForUser;
        this.mIsCompetitionModeEnabled = isCompetitionModeEnabled(contentResolver, stringForUser);
        this.mDisableByGameMode = Settings.System.getIntForUser(contentResolver, DISABLE_SMART_KEY_FOR_GAME_MODE, 0, -2) == 1;
        this.isAIKeyDisable = Settings.System.getIntForUser(contentResolver, AIKEY_DISABLE, 0, -2) == 1;
        this.mIsSetupWizard = Settings.Secure.getIntForUser(contentResolver, "user_setup_complete", 1, -2) == 0;
        VLog.d("VivoAIKeyHandler", "ai key isGameDisturb = " + this.isGameDisturb + ", isAIKeyDisable=" + this.isAIKeyDisable + ", mIsCompetitionModeEnabled = " + this.mIsCompetitionModeEnabled + ", mIsSupportGameMode_8_1 = " + this.mIsSupportGameMode_8_1 + ", mDisableByGameMode = " + this.mDisableByGameMode);
    }

    public ArrayList<String> userModifiedGameModeSettings(Context context, String type) {
        ArrayList<String> list = new ArrayList<>();
        String pkgs = Settings.System.getStringForUser(context.getContentResolver(), type, -2);
        if (!TextUtils.isEmpty(pkgs)) {
            String[] names = pkgs.split(":");
            if (names == null) {
                return list;
            }
            for (String str : names) {
                list.add(str);
            }
        }
        return list;
    }

    public boolean isNeedDisableAIKey() {
        ArrayList<String> arrayList;
        if (!this.mIsSupportGameMode_8_1 && this.isGameDisturb && !TextUtils.isEmpty(this.mCurrentGamePackage) && (((arrayList = this.mList) != null && arrayList.contains(this.mCurrentGamePackage)) || this.mIsCompetitionModeEnabled)) {
            VLog.d("VivoAIKeyHandler", "disable ai key for game disturb " + this.mCurrentGamePackage + ", mIsCompetitionModeEnabled = " + this.mIsCompetitionModeEnabled);
            return true;
        } else if (this.mIsSupportGameMode_8_1 && this.mDisableByGameMode) {
            VLog.d("VivoAIKeyHandler", "disable ai key for game mode 8.1");
            return true;
        } else if (this.isAIKeyDisable) {
            Log.d("VivoAIKeyHandler", "ai key disable");
            return true;
        } else if (this.mIsSetupWizard) {
            VLog.d("VivoAIKeyHandler", "steup running");
            return true;
        } else {
            return false;
        }
    }

    public boolean isAlarm() {
        boolean isAlarm = "on".equals(SystemProperties.get("persist.vivo.clock.alarm_status", "off"));
        return isAlarm && AudioSystem.isStreamActive(4, 0);
    }

    public void stopAlarm() {
        Intent intent = new Intent(VivoPolicyConstant.ACTION_HW_KEY_ALARM_CHANGE);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
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

    private void registerForGameMode(Context context, ContentObserver contentObserver) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(Settings.System.CONTENT_URI, GAME_DISTURB_ENABLED);
        contentResolver.registerContentObserver(uri, true, contentObserver, -1);
        Uri uri2 = Uri.withAppendedPath(Settings.System.CONTENT_URI, GAME_CURRENT_PACKAGE);
        contentResolver.registerContentObserver(uri2, true, contentObserver, -1);
        Uri uri3 = Uri.withAppendedPath(Settings.System.CONTENT_URI, ENABLED_SHIELD_SMART_KEY);
        contentResolver.registerContentObserver(uri3, true, contentObserver, -1);
        Uri uri4 = Uri.withAppendedPath(Settings.System.CONTENT_URI, KEY_COMPETITION_MODE_ENABLED_UPDATE);
        contentResolver.registerContentObserver(uri4, true, contentObserver, -1);
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
            VLog.d("VivoAIKeyHandler", e.toString());
            return false;
        }
    }
}