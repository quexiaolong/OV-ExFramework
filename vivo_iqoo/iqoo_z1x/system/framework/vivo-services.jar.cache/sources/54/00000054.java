package com.android.server;

import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.IProcessObserver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.FtFeature;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.File;
import vivo.app.nightmode.IVivoNightModeManager;
import vivo.app.nightmode.NightModeController;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoUiModeMgrServiceImpl implements IVivoUiModeMgrService {
    private static final String TAG = "VivoUiModeMgrServiceImpl";
    private boolean isSupportVivoNightMode;
    private String mThemeId;
    private UiModeManagerService mUiModeManagerService;
    private int setNightModePid;
    private int vivo_ui_mode;
    private boolean globalThemeChange = false;
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.android.server.VivoUiModeMgrServiceImpl.1
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            VLog.d(VivoUiModeMgrServiceImpl.TAG, "onForegroundActivitiesChanged: pid=" + pid + ", uid=" + uid + ", foregroundActivities=" + foregroundActivities);
            if (VivoUiModeMgrServiceImpl.this.isSupportVivoNightMode) {
                VivoUiModeMgrServiceImpl.this.handleForgroundAppChanged(pid, uid, foregroundActivities);
            }
        }

        public void onProcessDied(int pid, int uid) {
            if (VivoUiModeMgrServiceImpl.this.isSupportVivoNightMode) {
                VivoUiModeMgrServiceImpl.this.handleForgroundAppChanged(pid, uid, false);
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    };

    public VivoUiModeMgrServiceImpl(UiModeManagerService uiModeManagerService) {
        boolean z = false;
        this.isSupportVivoNightMode = false;
        this.mUiModeManagerService = uiModeManagerService;
        if (FtFeature.isFeatureSupport("vivo.software.nightmode") && !"vos".equals(SystemProperties.get("ro.vivo.os.name", "unknown").toLowerCase())) {
            z = true;
        }
        this.isSupportVivoNightMode = z;
    }

    private boolean renameNightFile(int vivo_ui_mode) {
        boolean isRename = false;
        String themePath = getGlobaleThemePath();
        File file = new File(themePath);
        File[] themeFiles = file.listFiles();
        if (!new File(themePath + "vivo").exists()) {
            if (!new File(themePath + "vivo_nightmode").exists()) {
                return false;
            }
        }
        for (File themeFile : themeFiles) {
            if (vivo_ui_mode == 32) {
                if (!themeFile.getName().endsWith("_nightmode") && (themeFile.getName().startsWith("com") || "vivo".equals(themeFile.getName()))) {
                    File tmpFile = new File(themeFile.getPath() + "_nightmode");
                    themeFile.renameTo(tmpFile);
                    isRename = true;
                }
            } else if (vivo_ui_mode == 16 && themeFile.getName().endsWith("_nightmode")) {
                File tmpFile2 = new File(themeFile.getPath().substring(0, themeFile.getPath().indexOf("_nightmode")));
                themeFile.renameTo(tmpFile2);
                isRename = true;
            }
        }
        return isRename;
    }

    public void sendConfigurationLockedBefore(Context context, Configuration configuration) {
        try {
            int i = configuration.uiMode & 48;
            this.vivo_ui_mode = i;
            this.mThemeId = "2";
            this.globalThemeChange = false;
            if (i == 32 || i == 16) {
                this.mThemeId = Settings.System.getStringForUser(context.getContentResolver(), "theme_id", UserHandle.myUserId());
                this.globalThemeChange = renameNightFile(this.vivo_ui_mode);
                VLog.v(TAG, "mConfiguration.getThemeId():" + String.valueOf(configuration.getThemeId()) + " mThemeId:" + String.valueOf(this.mThemeId) + " globalThemeChange:" + String.valueOf(this.globalThemeChange));
                if (this.vivo_ui_mode == 32 && this.globalThemeChange) {
                    this.mThemeId = "-1";
                }
                if (this.mThemeId == null || Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(this.mThemeId)) {
                    this.mThemeId = "2";
                }
                int themeId = 1000;
                try {
                    themeId = Integer.parseInt(this.mThemeId);
                } catch (NumberFormatException e) {
                    VLog.e(TAG, "sendConfigurationLockedBefore, themeId exceeds the maximum value of int");
                    e.printStackTrace();
                }
                configuration.setThemeId(themeId);
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public void sendConfigurationLockedAfter(Context context) {
        try {
            String themePath = getGlobaleThemePath();
            if (this.globalThemeChange) {
                if (this.vivo_ui_mode == 32 || this.vivo_ui_mode == 16) {
                    Intent intent = new Intent("intent.action.theme.changed");
                    intent.putExtra("pkg", VivoPermissionUtils.OS_PKG);
                    intent.putExtra("themeId", this.mThemeId);
                    intent.putExtra("reason", "switchNightMode");
                    if (this.vivo_ui_mode == 32) {
                        intent.putExtra("style", "general");
                        Settings.System.putStringForUser(context.getContentResolver(), "theme_style", "general", getCurrentSystemUser());
                    } else {
                        if (new File(themePath + "vivo").exists()) {
                            intent.putExtra("style", "whole");
                            Settings.System.putStringForUser(context.getContentResolver(), "theme_style", "whole", getCurrentSystemUser());
                        }
                    }
                    context.sendBroadcast(intent);
                    if (Build.VERSION.SDK_INT >= 26) {
                        ComponentName componentName = new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.vivo.settings.ClearAppIconCacheReceiver");
                        intent.setComponent(componentName);
                        context.sendBroadcast(intent);
                    }
                    VLog.v(TAG, "send broadcast: intent.action.theme.changed");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleForgroundAppChanged(int pid, int uid, boolean foregroundActivities) {
        try {
            if (pid == this.setNightModePid && !foregroundActivities) {
                ContentResolver cr = ActivityThread.currentApplication().getContentResolver();
                int userId = getCurrentSystemUser();
                if (userId == 999) {
                    userId = 0;
                }
                int nightMode = Settings.System.getIntForUser(cr, "vivo_nightmode_used", -2, userId) == 1 ? 2 : 1;
                this.mUiModeManagerService.setNightMode(nightMode);
                this.mUiModeManagerService.nightModeConfigurationLocked();
            }
        } catch (Exception e) {
            VLog.e(TAG, "error handleForgroundAppChanged");
        }
    }

    public void registerProcessObserver() {
        try {
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (Exception e) {
            VLog.e(TAG, "error registerProcessObserver " + e);
        }
    }

    public int updateNightModeFromSettings(Context context, int nightMode) {
        ContentResolver cr = context.getContentResolver();
        int userId = getCurrentSystemUser();
        if (userId == 999) {
            userId = 0;
        }
        if (this.isSupportVivoNightMode) {
            int nightMode2 = Settings.System.getIntForUser(cr, "vivo_nightmode_used", -2, userId) == 1 ? 2 : 1;
            return nightMode2;
        }
        Settings.System.putIntForUser(cr, "vivo_nightmode_used", -2, userId);
        return 1;
    }

    public void setNightMode(int mode) {
        IVivoNightModeManager iNm;
        try {
            if (this.isSupportVivoNightMode && (iNm = NightModeController.getInstance().getNightModeService()) != null && !iNm.isNightModeSwitching()) {
                this.setNightModePid = Binder.getCallingPid();
                VSlog.d(TAG, "setNightMode:" + mode + "  setNightModePid:" + this.setNightModePid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getCurrentSystemUser() {
        try {
            IVivoNightModeManager iNm = NightModeController.getInstance().getNightModeService();
            if (iNm != null) {
                return iNm.getCurrentSystemUser();
            }
            return 0;
        } catch (Exception e) {
            VLog.e(TAG, "VivoUiModeMgrServiceImpl::getCurrentSystemUser() failed, " + e);
            return 0;
        }
    }

    public String getGlobaleThemePath() {
        int userid = getCurrentSystemUser();
        if (userid == 0) {
            return "/data/bbkcore/theme/";
        }
        String themePath = "/data/bbkcore/theme/" + userid + "/";
        return themePath;
    }
}