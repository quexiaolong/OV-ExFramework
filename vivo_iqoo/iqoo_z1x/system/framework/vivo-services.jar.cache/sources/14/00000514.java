package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityController;
import android.app.IAppShareController;
import android.app.IApplicationThread;
import android.app.IEasyShareController;
import android.app.IGameModeController;
import android.app.IVivoActivityController;
import android.app.IVivoKeyguardOccludeCallback;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IClipboard;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.SQLException;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.multidisplay.MultiDisplayManager;
import android.net.Uri;
import android.os.BatteryManagerInternal;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.server.LocalServices;
import com.android.server.UnifiedConfigThread;
import com.android.server.am.AmsDataManager;
import com.android.server.am.ProcessRecord;
import com.android.server.am.VivoAmsUtils;
import com.android.server.am.firewall.VivoAppIsolationController;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.policy.key.VivoAIKeyExtend;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.LaunchParamsController;
import com.google.android.collect.Sets;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.autorecover.SystemAutoRecoverManagerInternal;
import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.display.SceneManager;
import com.vivo.services.rms.proxy.VivoBinderProxy;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.security.client.VivoPermissionManager;
import com.vivo.services.superresolution.Constant;
import com.vivo.vcodetransbase.EventTransfer;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.app.IVivoActivityObserver;
import vivo.contentcatcher.IActivityObserver;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoActivityTaskManagerServiceImpl implements IVivoActivityTaskManagerService {
    static final String BLACKLIST_COMPONENT_NAME = "com.outfit7.mytalkingtom.vivo/com.outfit7.mytalkingtom.SplashActivity";
    static final int CMD_CRASH_ACTION = 8;
    static final int CMD_ENTER_ACTION = 1;
    static final int CMD_EXIT_ACTION = 2;
    static final int CMD_FRONT_ACTION = 4;
    static final int CMD_SWITCH_ACTION = 16;
    static final int DISPLAY_WAIT_TIMEOUT_MS = 100;
    static final String ENABLED_SUSPEND_MODE_GAMES = "enabled_framework_suspend_mode_games";
    static final String GAMEMODE_CMD_KEY = "cmd";
    static final String GAMEMODE_FRONT_KEY = "front";
    static final String GAMEMODE_PKG_KEY = "pkg";
    static final String GAMEMODE_PROC_KEY = "proc";
    static final String GAMEMODE_TIME_KEY = "time";
    static final int POSITION_BOTTOM = Integer.MIN_VALUE;
    static final int POSITION_TOP = Integer.MAX_VALUE;
    static final String TAG = "VivoActivityTaskManagerServiceImpl";
    public static final int VIVO_OCCLUDE_KEYGUARD_APP_DEAD = 1;
    public static final int VIVO_OCCLUDE_KEYGUARD_OTHER_APP = 3;
    public static final int VIVO_OCCLUDE_KEYGUARD_SCREEN_OFF_TIME_OUT = 2;
    public static final int VIVO_OCCLUDE_KEYGUARD_SUCCEE = 0;
    private Bundle mActivityStateInfo;
    private ActivityTaskManagerService mActivityTaskManagerService;
    private AudioManager mAudioManager;
    BatteryManagerInternal mBatteryManagerInternal;
    private GameModeSettingObserver mGameModeSettingsObserver;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    public ThirdPartyIncomingManager mThirdPartyIncomingManager;
    VivoAppShareManager mVivoAppShareManager;
    private VivoEasyShareManager mVivoEasyShareManager;
    private VivoFreeformActivityManager mVivoFreeformActivityManager;
    static final boolean PRI_AMS_SPLIT_DEBUG = VivoMultiWindowConfig.isDebugAllPrivateInfo();
    static final int DUBRESUME_MINUTE_TIMEOUT = SystemProperties.getInt("persist.vivo.dubresume.time", 30);
    static final Object mLock = new Object();
    static String[] vComponentList = null;
    static int sGameModeDisplayId = -1;
    static boolean sInNormalGameMode = false;
    static boolean sInSleepGameMode = false;
    private static IBinder mRemote = null;
    private RemoteCallbackList<IActivityObserver> mActivityObserver = new RemoteCallbackList<>();
    boolean mIsMonkey = false;
    HashMap<IBinder, OccludeKeyguardDeathRecipient> mVivoOccludeDeathRecipients = new HashMap<>();
    UserSetupObserver mUserSetupObserver = null;
    private boolean mSplittingScreenByVivo = false;
    private boolean mSplittingState = false;
    final Runnable mResetSplittingStateRunnable = new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.1
        @Override // java.lang.Runnable
        public void run() {
            VivoActivityTaskManagerServiceImpl.this.mSplittingState = false;
        }
    };
    private boolean mIsResizingJustAfterSplit = false;
    private VivoMultiWindowConfig mVivoMultiWindowConfig = VivoMultiWindowConfig.getInstance();
    boolean mSkipRelaunchActivity = false;
    String mNotifiySkipRelaunchActivity = null;
    String mNotifiySkipRelaunchApp = null;
    final int mMaxSkipRelaunchActivityDuration = SystemProperties.getInt("persist.vivo.skip_relaunch.timeout", 200);
    final Runnable mSkipRelaunchActivityRunnable = new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.2
        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.getGlobalLock()) {
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.d(VivoActivityTaskManagerServiceImpl.TAG, "vivo_multiwindow_fmk may skipActivityRelaunch name " + VivoActivityTaskManagerServiceImpl.this.mNotifiySkipRelaunchActivity + " to null, because timeout!");
                }
                VivoActivityTaskManagerServiceImpl.this.mSkipRelaunchActivity = false;
                VivoActivityTaskManagerServiceImpl.this.mNotifiySkipRelaunchActivity = null;
                VivoActivityTaskManagerServiceImpl.this.mNotifiySkipRelaunchApp = null;
            }
        }
    };
    boolean mVivoFloatMessageFlag = false;
    boolean mVivoIgnoreSetVisableWhenFloatMessage = false;
    boolean mVivoFloatPackageColdStartingFlag = false;
    String mVivoCurrentPackageInColdStarting = null;
    final Runnable mColdStartingRunnable = new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.3
        @Override // java.lang.Runnable
        public void run() {
            VivoActivityTaskManagerServiceImpl.this.setVivoCurrentPackageInColdStarting(null);
        }
    };
    String mVivoReortPendingSplitPackage = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    String mNotifiySwapDockActivity1 = null;
    String mNotifiySwapDockActivity2 = null;
    int mNotifiySwapDockActivityFlag = 0;
    long mSwapDockActivityFreezingScreenStartTime = SystemClock.uptimeMillis();
    long mSwapDockTime = 0;
    boolean mSwapDockInFreezeProcess = false;
    final int mMaxSwapDockActivityFreezingScreenDuration = SystemProperties.getInt("persist.vivo.swapdock_freeze.timeout", 100);
    final Runnable mSwapDockActivityFreezingScreenRunnable = new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.4
        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.getGlobalLock()) {
                int notifiySwapDockActivityFlag = VivoActivityTaskManagerServiceImpl.this.mNotifiySwapDockActivityFlag;
                VivoActivityTaskManagerServiceImpl.this.mNotifiySwapDockActivity1 = null;
                VivoActivityTaskManagerServiceImpl.this.mNotifiySwapDockActivity2 = null;
                VivoActivityTaskManagerServiceImpl.this.mNotifiySwapDockActivityFlag = 0;
                VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mWindowManager.maskForceFreezingScreenInSplit();
                VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mWindowManager.stopFreezingScreen();
                VivoActivityTaskManagerServiceImpl.this.mSwapDockInFreezeProcess = false;
                VSlog.d(VivoActivityTaskManagerServiceImpl.TAG, "swapDockedAndFullscreenStack:stopFreezingScreen");
                long totalTime = SystemClock.uptimeMillis() - VivoActivityTaskManagerServiceImpl.this.mSwapDockActivityFreezingScreenStartTime;
                VSlog.d(VivoActivityTaskManagerServiceImpl.TAG, "swapDockedAndFullscreenStack:stopFreezingScreen, mNotifiySwapDockActivityFlag=" + notifiySwapDockActivityFlag + " totalTime=" + totalTime + " mSwapDockTime=" + VivoActivityTaskManagerServiceImpl.this.mSwapDockTime);
            }
        }
    };
    private Field mStackAboveHomeVarName = null;
    int smwpid = -1;
    private String vivoEnterSplitWay = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private int mTencentPid = -1;
    private boolean isGameModeOpen = false;
    private boolean isInGame = false;
    IGameModeController mGameModeController = null;
    final RemoteCallbackList<IVivoActivityObserver> mVivoActivityObserver = new RemoteCallbackList<>();
    final Map<Integer, ActivityTaskManagerInternal.SleepToken> mVirtualSleepTokens = new HashMap();
    String mMirraPackage = null;
    final Object mMirraLock = new Object();
    long mLastGameModeTime = 0;
    ActivityStack mBgStack = null;
    WindowProcessController mBgProc = null;
    String mBgPkgName = null;
    boolean mExitFromClient = false;
    boolean mSecondDisplayAdded = false;
    IRemoteCallback mVivoGameCallback = null;
    VirtualDisplay mVirtualDisplay = null;
    private ServiceConnection mPEMConn = new ServiceConnection() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.8
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            VSlog.d("VivoGameMode", "onServiceConnected name = " + name.toString());
            IBinder unused = VivoActivityTaskManagerServiceImpl.mRemote = service;
            if ((VivoActivityTaskManagerServiceImpl.sInNormalGameMode || VivoActivityTaskManagerServiceImpl.sInSleepGameMode) && VivoActivityTaskManagerServiceImpl.this.mBgPkgName != null) {
                VivoActivityTaskManagerServiceImpl vivoActivityTaskManagerServiceImpl = VivoActivityTaskManagerServiceImpl.this;
                vivoActivityTaskManagerServiceImpl.notifyGameState(true, vivoActivityTaskManagerServiceImpl.mBgPkgName, VivoActivityTaskManagerServiceImpl.this.mBgProc != null ? VivoActivityTaskManagerServiceImpl.this.mBgProc.mUid : 0);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            VSlog.d("VivoGameMode", "onServiceDisconnected name = " + name.toString());
            IBinder unused = VivoActivityTaskManagerServiceImpl.mRemote = null;
        }
    };
    private final Runnable mMuteGMRunnable = new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.11
        @Override // java.lang.Runnable
        public void run() {
            VSlog.d("VivoGameMode", "restorePidGM!");
            AudioSystem.setParameters("mutePidGM=0");
        }
    };

    /* loaded from: classes.dex */
    private static class AudioFocusListener implements AudioManager.OnAudioFocusChangeListener {
        @Override // android.media.AudioManager.OnAudioFocusChangeListener
        public void onAudioFocusChange(int focusChange) {
        }
    }

    public VivoActivityTaskManagerServiceImpl(ActivityTaskManagerService atm) {
        this.mVivoEasyShareManager = null;
        this.mVivoAppShareManager = null;
        if (atm == null) {
            VSlog.i(TAG, "container is " + atm);
        }
        this.mActivityTaskManagerService = atm;
        this.mAudioManager = (AudioManager) atm.mContext.getSystemService("audio");
        VivoFreeformActivityManager vivoFreeformActivityManager = VivoFreeformActivityManager.getInstance();
        this.mVivoFreeformActivityManager = vivoFreeformActivityManager;
        if (vivoFreeformActivityManager != null) {
            vivoFreeformActivityManager.init(atm);
        }
        this.mThirdPartyIncomingManager = new ThirdPartyIncomingManager(atm);
        VivoEasyShareManager vivoEasyShareManager = VivoEasyShareManager.getInstance();
        this.mVivoEasyShareManager = vivoEasyShareManager;
        if (vivoEasyShareManager != null) {
            vivoEasyShareManager.initAtms(atm);
        }
        VivoAppShareManager vivoAppShareManager = VivoAppShareManager.getInstance();
        this.mVivoAppShareManager = vivoAppShareManager;
        if (vivoAppShareManager != null) {
            vivoAppShareManager.initAtms(atm);
        }
        VivoBinderProxy.getInstance().initialize();
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public void initSplitInterfaceCaller(int callingUid, int callingPid, int taskId, boolean toTop) {
        this.mSplittingScreenByVivo = false;
        if (isMultiWindowSupport()) {
            if (UserHandle.getAppId(callingUid) == 1000) {
                this.mSplittingScreenByVivo = true;
            } else {
                this.mSplittingScreenByVivo = false;
            }
            this.mSplittingState = true;
            if (this.mActivityTaskManagerService.mH.hasCallbacks(this.mResetSplittingStateRunnable)) {
                this.mActivityTaskManagerService.mH.removeCallbacks(this.mResetSplittingStateRunnable);
            }
            this.mActivityTaskManagerService.mH.postDelayed(this.mResetSplittingStateRunnable, 300L);
            VSlog.d(TAG, "setTaskWindowingModeSplitScreenPrimary: moving task=" + taskId + " mSplittingScreenByVivo=" + this.mSplittingScreenByVivo + " toTop=" + toTop);
        }
    }

    public boolean isSplittingScreenByVivo() {
        return this.mSplittingScreenByVivo;
    }

    public boolean isInSplittingState() {
        return this.mSplittingState;
    }

    public void resetSplittingState() {
        this.mSplittingState = false;
    }

    public void resetSplitInterfaceCaller() {
        this.mSplittingScreenByVivo = false;
        VSlog.d(TAG, "setTaskWindowingModeSplitScreenPrimary: resetSplitInterfaceCaller mSplittingScreenByVivo=" + this.mSplittingScreenByVivo);
    }

    public boolean judgeDisplaySleepingStateInSplit(int displayId, ActivityRecord record) {
        return false;
    }

    public void setResizingJustAfterSplit(boolean resizingJustSplit) {
        this.mIsResizingJustAfterSplit = resizingJustSplit;
    }

    public boolean isResizingJustAfterSplit() {
        return this.mIsResizingJustAfterSplit;
    }

    public void hideSoftIfNeededBeforSplit(boolean isForce) {
    }

    public boolean isNeededHideSoftBeforeSplitActivity(String fullActivityName) {
        return this.mVivoMultiWindowConfig.isNeededHideSoftBeforeSplitActivity(fullActivityName);
    }

    public boolean isForceResizeApp(String packageName) {
        if (VivoMultiWindowConfig.IS_VIVO_SUPPORT_MULTIWINDOW_DEBUG_PROPERTY) {
            String forceResizeApp = SystemProperties.get("persist.vivo.forceresize_app", "null");
            if (packageName != null && packageName.contains(forceResizeApp)) {
                return true;
            }
        }
        return isAllowSplitApp(packageName);
    }

    public boolean isForceUnResizeApp(String packageName) {
        if (VivoMultiWindowConfig.IS_VIVO_SUPPORT_MULTIWINDOW_DEBUG_PROPERTY) {
            String forceUnResizeApp = SystemProperties.get("persist.vivo.forceunresize_app", "null");
            if (packageName != null && packageName.contains(forceUnResizeApp)) {
                return true;
            }
        }
        return isNotAllowSplitApp(packageName);
    }

    public boolean isDoubleResumeApp(String packageName) {
        if (VivoMultiWindowConfig.IS_VIVO_SUPPORT_MULTIWINDOW_DEBUG_PROPERTY) {
            String forceDoubleResumeApp = SystemProperties.get("persist.vivo.forcedoubleresume_app", "null");
            if (packageName != null && packageName.contains(forceDoubleResumeApp)) {
                return true;
            }
        }
        return this.mVivoMultiWindowConfig.isDoubleResumeApp(packageName);
    }

    public boolean isForceFullscreenActivity(String fullActivityName) {
        if (VivoMultiWindowConfig.IS_VIVO_SUPPORT_MULTIWINDOW_DEBUG_PROPERTY) {
            String forceFullscreenActivity = SystemProperties.get("persist.vivo.forcefullscreen_activity", "null");
            if (fullActivityName != null && fullActivityName.contains(forceFullscreenActivity)) {
                return true;
            }
        }
        return this.mVivoMultiWindowConfig.isForceFullscreenActivity(fullActivityName);
    }

    public boolean isForceResizableActivity(String fullActivityName) {
        if (VivoMultiWindowConfig.IS_VIVO_SUPPORT_MULTIWINDOW_DEBUG_PROPERTY) {
            String forceResizableActivity = SystemProperties.get("persist.vivo.forceresizable_activity", "null");
            if (fullActivityName != null && fullActivityName.contains(forceResizableActivity)) {
                return true;
            }
        }
        return this.mVivoMultiWindowConfig.isForceResizableActivity(fullActivityName);
    }

    public boolean isAllowSplitApp(String packageName) {
        return this.mVivoMultiWindowConfig.isAllowSplitApp(packageName);
    }

    public boolean isNotAllowSplitApp(String packageName) {
        return this.mVivoMultiWindowConfig.isNotAllowSplitApp(packageName);
    }

    public boolean isNeedRelaunchApp(String packageName) {
        return this.mVivoMultiWindowConfig.isNeedRelaunchApp(packageName);
    }

    public boolean isNeedRelaunchActivity(String fullActivityName) {
        return this.mVivoMultiWindowConfig.isNeedRelaunchActivity(fullActivityName);
    }

    public boolean isIgnoreRelaunchApp(String packageName) {
        return this.mVivoMultiWindowConfig.isIgnoreRelaunchApp(packageName);
    }

    public boolean isIgnoreRelaunchActivity(String fullActivityName) {
        return this.mVivoMultiWindowConfig.isIgnoreRelaunchActivity(fullActivityName);
    }

    public boolean isIgnoreRelaunchActivityAlreadySplit(String fullActivityName) {
        return this.mVivoMultiWindowConfig.isIgnoreRelaunchActivityAlreadySplit(fullActivityName);
    }

    public boolean isNeedRelaunchOrientationActivity(String fullActivityName) {
        return this.mVivoMultiWindowConfig.isNeedRelaunchOrientationActivity(fullActivityName);
    }

    public boolean isIgnoreRelaunchOrientationActivity(String fullActivityName) {
        return this.mVivoMultiWindowConfig.isIgnoreRelaunchOrientationActivity(fullActivityName);
    }

    public boolean isNotIgnoreRelaunchActivityWhenFloatMsg(String fullActivityName) {
        return this.mVivoMultiWindowConfig.isNotIgnoreRelaunchActivityWhenFloatMsg(fullActivityName);
    }

    public void updateMultiWindowConfig(Map configsMap) {
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            VSlog.i(TAG, "updateMultiWindowConfig new configs");
            this.mVivoMultiWindowConfig.setMultiWindowConfig(configsMap);
        }
    }

    public boolean isVivoMultiWindowSupport() {
        if (VivoMultiWindowConfig.IS_VIVO_SUPPORT_MULTIWINDOW_DEBUG_PROPERTY) {
            String tmp = SystemProperties.get("persist.vivo.multiwindow_force", "none");
            if ("true".equals(tmp) || "false".equals(tmp)) {
                return "true".equals(tmp);
            }
        }
        return this.mVivoMultiWindowConfig.isVivoMultiWindowSupport();
    }

    public boolean isVivoVosMultiWindowSupport() {
        if (VivoMultiWindowConfig.IS_VIVO_SUPPORT_MULTIWINDOW_DEBUG_PROPERTY) {
            String tmp = SystemProperties.get("persist.vivo.multiwindow_force", "none");
            if ("true".equals(tmp) || "false".equals(tmp)) {
                return "true".equals(tmp);
            }
        }
        return this.mVivoMultiWindowConfig.isVivoVosMultiWindowSupport();
    }

    public boolean isVivoOverVos2MultiWindowSupport() {
        if (VivoMultiWindowConfig.IS_VIVO_SUPPORT_MULTIWINDOW_DEBUG_PROPERTY) {
            String tmp = SystemProperties.get("persist.vivo.multiwindow_force", "none");
            if ("true".equals(tmp) || "false".equals(tmp)) {
                return "true".equals(tmp);
            }
        }
        return this.mVivoMultiWindowConfig.isVivoOverVos2MultiWindowSupport();
    }

    public boolean isMultiWindowSupport() {
        if (VivoMultiWindowConfig.IS_VIVO_SUPPORT_MULTIWINDOW_DEBUG_PROPERTY) {
            String tmp = SystemProperties.get("persist.vivo.multiwindow_force", "none");
            if ("true".equals(tmp) || "false".equals(tmp)) {
                return "true".equals(tmp);
            }
        }
        return this.mVivoMultiWindowConfig.isMultiWindowSupport();
    }

    public boolean isVivoResizeableApp(String packageName) {
        if (packageName == null) {
            return false;
        }
        boolean forceUnResizeApp = isForceUnResizeApp(packageName);
        boolean forceResizeableApp = isForceResizeApp(packageName);
        if (VivoMultiWindowConfig.IS_VIVO_AM_RESIZABLE_PROPERTY) {
            if (forceUnResizeApp) {
                return false;
            }
            if (forceResizeableApp) {
                return true;
            }
        }
        try {
            ApplicationInfo info = this.mActivityTaskManagerService.mContext.getPackageManager().getApplicationInfo(packageName, Consts.ProcessStates.FOCUS);
            return (info.privateFlags & Consts.ProcessStates.VIRTUAL_DISPLAY) != 0;
        } catch (Exception e) {
            VSlog.e(TAG, "getApplicationInfo Exception in AMS");
            e.printStackTrace();
            return true;
        }
    }

    public boolean isVivoResizeableActivity(String shortComponentName) {
        if (shortComponentName == null) {
            return false;
        }
        try {
            int callingUid = Binder.getCallingUid();
            ComponentName comp = ComponentName.unflattenFromString(shortComponentName);
            String packageName = comp.getPackageName();
            boolean forceFullscreenActivity = isForceFullscreenActivity(shortComponentName);
            boolean forceResizeableActivity = isForceResizableActivity(shortComponentName);
            boolean forceUnResizeApp = isForceUnResizeApp(packageName);
            boolean forceResizeableApp = isForceResizeApp(packageName);
            ActivityInfo info = AppGlobals.getPackageManager().getActivityInfo(comp, (int) Consts.ProcessStates.FOCUS, UserHandle.getUserId(callingUid));
            if (VivoMultiWindowConfig.IS_VIVO_AM_RESIZABLE_PROPERTY) {
                if (forceFullscreenActivity) {
                    return false;
                }
                if (forceResizeableActivity) {
                    return true;
                }
                if (forceUnResizeApp) {
                    return false;
                }
                if (forceResizeableApp) {
                    return true;
                }
            }
            if (info != null) {
                return ActivityInfo.isResizeableMode(info.resizeMode);
            }
        } catch (Exception e) {
            VSlog.e(TAG, "getActivityInfo Exception in AMS");
            e.printStackTrace();
        }
        return false;
    }

    public boolean isInMultiWindowDefaultDisplay() {
        return this.mActivityTaskManagerService.mWindowManager.mRoot.getDefaultTaskDisplayArea().isSplitScreenModeActivated();
    }

    public boolean isInMultiWindowFocusedDisplay() {
        return this.mActivityTaskManagerService.mWindowManager.mRoot.getDefaultTaskDisplayArea().isSplitScreenModeActivated();
    }

    public int getFocusedDisplayId() {
        return 0;
    }

    public boolean isSplitLogDebug() {
        return VivoMultiWindowConfig.DEBUG;
    }

    public boolean isSupportDoubleResume() {
        return false;
    }

    public void notifyActivityDrawn(ActivityRecord r) {
        notifyVivoSwapDockedAndFullscreenStack(r);
        notifyVivoColdStarting(r);
    }

    void notifyVivoSkipActivityRelaunch(ActivityRecord r) {
        if (this.mSkipRelaunchActivity && r != null && r.mActivityComponent != null && r.mActivityComponent.flattenToShortString().equals(this.mNotifiySkipRelaunchActivity)) {
            if (this.mActivityTaskManagerService.mH.hasCallbacks(this.mSkipRelaunchActivityRunnable)) {
                this.mActivityTaskManagerService.mH.removeCallbacks(this.mSkipRelaunchActivityRunnable);
            }
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.d(TAG, "vivo_multiwindow_fmk may skipActivityRelaunch name " + this.mNotifiySkipRelaunchActivity + " to null, because drawn!");
            }
            this.mSkipRelaunchActivity = false;
            this.mNotifiySkipRelaunchActivity = null;
            this.mNotifiySkipRelaunchApp = null;
        }
    }

    public void setSkipActivityRelaunch(ActivityRecord r) {
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (r != null) {
                if (r.mActivityComponent != null) {
                    this.mSkipRelaunchActivity = true;
                    this.mNotifiySkipRelaunchActivity = r.mActivityComponent.flattenToShortString();
                    this.mNotifiySkipRelaunchApp = r.mActivityComponent.getPackageName();
                    if (this.mActivityTaskManagerService.mH.hasCallbacks(this.mSkipRelaunchActivityRunnable)) {
                        this.mActivityTaskManagerService.mH.removeCallbacks(this.mSkipRelaunchActivityRunnable);
                    }
                    if (VivoMultiWindowConfig.DEBUG) {
                        VSlog.d(TAG, "vivo_multiwindow_fmk may skipActivityRelaunch name " + this.mNotifiySkipRelaunchActivity + " app: " + this.mNotifiySkipRelaunchApp);
                    }
                    this.mActivityTaskManagerService.mH.postDelayed(this.mSkipRelaunchActivityRunnable, this.mMaxSkipRelaunchActivityDuration);
                }
            }
        }
    }

    public boolean skipActivityRelaunchIfNeed(String activityname) {
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mSkipRelaunchActivity && activityname != null && (activityname.equals(this.mNotifiySkipRelaunchActivity) || this.mVivoMultiWindowConfig.isForceIgnoreRelaunchAfterBackActivity(activityname))) {
                return true;
            }
            return false;
        }
    }

    public boolean skipAppRelaunchIfNeed(String packageName) {
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            if (this.mSkipRelaunchActivity && packageName != null && packageName.equals(this.mNotifiySkipRelaunchApp)) {
                return true;
            }
            return false;
        }
    }

    public void setVivoFloatMessageFlag(boolean enter) {
        if (!isMultiWindowSupport()) {
            return;
        }
        VSlog.d(TAG, "setVivoFloatMessageFlag:" + enter);
        this.mVivoFloatMessageFlag = enter;
        this.mVivoIgnoreSetVisableWhenFloatMessage = enter;
        if (!enter && this.mVivoFloatPackageColdStartingFlag) {
            setVivoFloatPackageColdStarting(false);
        }
        this.mActivityTaskManagerService.mWindowManager.setVivoFloatMessageFlag(enter);
    }

    public boolean getVivoFloatMessageFlag() {
        return this.mVivoFloatMessageFlag;
    }

    public void setVivoIgnoreSetVisableFlag(boolean flag) {
        this.mVivoIgnoreSetVisableWhenFloatMessage = flag;
    }

    public boolean getVivoIgnoreSetVisableFlag() {
        return this.mVivoIgnoreSetVisableWhenFloatMessage;
    }

    public void setVivoFloatPackageColdStarting(boolean flag) {
        if (VivoMultiWindowConfig.DEBUG) {
            VSlog.d(TAG, "setVivoFloatPackageColdStarting:" + flag);
        }
        this.mVivoFloatPackageColdStartingFlag = flag;
    }

    public boolean getVivoFloatPackageColdStarting() {
        return this.mVivoFloatPackageColdStartingFlag;
    }

    void notifyVivoColdStarting(ActivityRecord r) {
        String str;
        if (r != null && (str = this.mVivoCurrentPackageInColdStarting) != null && str.equals(r.packageName)) {
            if (this.mActivityTaskManagerService.mH.hasCallbacks(this.mColdStartingRunnable)) {
                this.mActivityTaskManagerService.mH.removeCallbacks(this.mColdStartingRunnable);
            }
            setVivoCurrentPackageInColdStarting(null);
        }
    }

    public void setVivoCurrentPackageInColdStarting(String packageName) {
        if (VivoMultiWindowConfig.DEBUG) {
            VSlog.d(TAG, "setVivoCurrentPackageInColdStarting:" + packageName);
        }
        this.mVivoCurrentPackageInColdStarting = packageName;
        if (packageName != null) {
            if (this.mActivityTaskManagerService.mH.hasCallbacks(this.mColdStartingRunnable)) {
                this.mActivityTaskManagerService.mH.removeCallbacks(this.mColdStartingRunnable);
            }
            this.mActivityTaskManagerService.mH.postDelayed(this.mColdStartingRunnable, 500L);
        }
    }

    public String getVivoCurrentPackageInColdStarting() {
        return this.mVivoCurrentPackageInColdStarting;
    }

    public void setVivoPendingPackageInSplit(String packageName) {
        if (isMultiWindowSupport() && packageName != null) {
            this.mVivoReortPendingSplitPackage = packageName;
            this.mActivityTaskManagerService.mWindowManager.setVivoPendingPackageInSplit(packageName);
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.d(TAG, "setVivoPendingPackageInSplit is " + packageName);
            }
        }
    }

    public String getVivoPendingPackageInSplit() {
        return this.mVivoReortPendingSplitPackage;
    }

    public void startPauseActivitiesOfPipInSplitScreen(boolean isToTop, ActivityStack stack) {
    }

    public void cancelPauseTopActivitiesOfPipIfNeeded(ActivityStack stack, ActivityRecord target) {
    }

    void notifyVivoSwapDockedAndFullscreenStack(ActivityRecord r) {
        String str;
        if (r != null && r.mActivityComponent != null && (str = this.mNotifiySwapDockActivity1) != null && this.mNotifiySwapDockActivity2 != null) {
            if (str.equals(r.mActivityComponent.flattenToShortString())) {
                this.mNotifiySwapDockActivityFlag |= 1;
            }
            if (this.mNotifiySwapDockActivity2.equals(r.mActivityComponent.flattenToShortString())) {
                this.mNotifiySwapDockActivityFlag |= 2;
            }
            if (3 == (this.mNotifiySwapDockActivityFlag & 3)) {
                if (this.mActivityTaskManagerService.mH.hasCallbacks(this.mSwapDockActivityFreezingScreenRunnable)) {
                    this.mActivityTaskManagerService.mH.removeCallbacks(this.mSwapDockActivityFreezingScreenRunnable);
                }
                this.mSwapDockActivityFreezingScreenRunnable.run();
            }
        }
    }

    public void swapDockedAndFullscreenStack(String reason) {
        swapDockedStacksInSplit(reason, 0);
    }

    /* JADX WARN: Removed duplicated region for block: B:64:0x0157 A[Catch: all -> 0x0223, TryCatch #1 {all -> 0x0223, blocks: (B:7:0x0015, B:12:0x0020, B:17:0x002b, B:19:0x0031, B:88:0x0217, B:22:0x003b, B:24:0x0041, B:28:0x004c, B:83:0x0209, B:31:0x0057, B:33:0x005e, B:35:0x0064, B:78:0x01fa, B:39:0x0077, B:41:0x007d, B:45:0x0089, B:47:0x00cd, B:48:0x00d5, B:50:0x00d9, B:51:0x00e1, B:53:0x00e5, B:58:0x00f1, B:60:0x0147, B:62:0x014f, B:64:0x0157, B:68:0x0160, B:69:0x0174, B:70:0x0197, B:72:0x01b8, B:73:0x01c1, B:57:0x00ec), top: B:105:0x0015 }] */
    /* JADX WARN: Removed duplicated region for block: B:65:0x015c  */
    /* JADX WARN: Removed duplicated region for block: B:68:0x0160 A[Catch: all -> 0x0223, TryCatch #1 {all -> 0x0223, blocks: (B:7:0x0015, B:12:0x0020, B:17:0x002b, B:19:0x0031, B:88:0x0217, B:22:0x003b, B:24:0x0041, B:28:0x004c, B:83:0x0209, B:31:0x0057, B:33:0x005e, B:35:0x0064, B:78:0x01fa, B:39:0x0077, B:41:0x007d, B:45:0x0089, B:47:0x00cd, B:48:0x00d5, B:50:0x00d9, B:51:0x00e1, B:53:0x00e5, B:58:0x00f1, B:60:0x0147, B:62:0x014f, B:64:0x0157, B:68:0x0160, B:69:0x0174, B:70:0x0197, B:72:0x01b8, B:73:0x01c1, B:57:0x00ec), top: B:105:0x0015 }] */
    /* JADX WARN: Removed duplicated region for block: B:69:0x0174 A[Catch: all -> 0x0223, TryCatch #1 {all -> 0x0223, blocks: (B:7:0x0015, B:12:0x0020, B:17:0x002b, B:19:0x0031, B:88:0x0217, B:22:0x003b, B:24:0x0041, B:28:0x004c, B:83:0x0209, B:31:0x0057, B:33:0x005e, B:35:0x0064, B:78:0x01fa, B:39:0x0077, B:41:0x007d, B:45:0x0089, B:47:0x00cd, B:48:0x00d5, B:50:0x00d9, B:51:0x00e1, B:53:0x00e5, B:58:0x00f1, B:60:0x0147, B:62:0x014f, B:64:0x0157, B:68:0x0160, B:69:0x0174, B:70:0x0197, B:72:0x01b8, B:73:0x01c1, B:57:0x00ec), top: B:105:0x0015 }] */
    /* JADX WARN: Removed duplicated region for block: B:72:0x01b8 A[Catch: all -> 0x0223, TryCatch #1 {all -> 0x0223, blocks: (B:7:0x0015, B:12:0x0020, B:17:0x002b, B:19:0x0031, B:88:0x0217, B:22:0x003b, B:24:0x0041, B:28:0x004c, B:83:0x0209, B:31:0x0057, B:33:0x005e, B:35:0x0064, B:78:0x01fa, B:39:0x0077, B:41:0x007d, B:45:0x0089, B:47:0x00cd, B:48:0x00d5, B:50:0x00d9, B:51:0x00e1, B:53:0x00e5, B:58:0x00f1, B:60:0x0147, B:62:0x014f, B:64:0x0157, B:68:0x0160, B:69:0x0174, B:70:0x0197, B:72:0x01b8, B:73:0x01c1, B:57:0x00ec), top: B:105:0x0015 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void swapDockedStacksInSplit(java.lang.String r32, int r33) {
        /*
            Method dump skipped, instructions count: 564
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoActivityTaskManagerServiceImpl.swapDockedStacksInSplit(java.lang.String, int):void");
    }

    public boolean getVivoSwapDockInFreezeProcess() {
        return this.mSwapDockInFreezeProcess;
    }

    public void reparentTaskForSwap(Task task, ActivityStack targetStack, Task exclusionTask) {
        if (task == null || targetStack == null) {
            return;
        }
        if (exclusionTask != null && (exclusionTask == task || exclusionTask.mTaskId == task.mTaskId)) {
            return;
        }
        task.reparent(targetStack, true, 0, true, true, "swapDockedStacksInSplit-toSecnodaryStack");
    }

    public boolean isLayoutIncludeNavApp(String runningApp) {
        return this.mVivoMultiWindowConfig.isLayoutIncludeNavApp(runningApp);
    }

    public void debugDismissSplitScreen(boolean toTop) {
        ActivityStack focusStack = this.mActivityTaskManagerService.mStackSupervisor.mRootWindowContainer.getDefaultDisplay().getFocusedStack();
        ActivityRecord topRunningActivity = null;
        if (focusStack != null) {
            topRunningActivity = focusStack.topRunningActivityLocked();
        }
        if (VivoMultiWindowConfig.DEBUG) {
            VSlog.d(TAG, "dismissSplitScreenMode: topRunningActivity=" + topRunningActivity + " focusStack=" + focusStack + " toTop=" + toTop);
        }
    }

    public void resizeDockedStackLocked(Rect dockedBounds, Rect tempDockedTaskBounds, Rect tempDockedTaskInsetBounds, Rect tempOtherTaskBounds, Rect tempOtherTaskInsetBounds, boolean preserveWindows) {
        if (this.mActivityTaskManagerService.mWindowManager.getVivoMultiWindowRotationFlag() && !this.mActivityTaskManagerService.mWindowManager.getVivoPauseRotationFlag()) {
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.d(TAG, "AMS.resizeDockedStack: getVivoMultiWindowRotationFlag=true");
                return;
            }
            return;
        }
        this.mActivityTaskManagerService.mStackSupervisor.resizeDockedStackLockedFromAms(dockedBounds, tempDockedTaskBounds, tempDockedTaskInsetBounds, tempOtherTaskBounds, tempOtherTaskInsetBounds, true, false);
    }

    public void moveStackBehindPasswrdStackAfterExitSplit(String securePkgName) {
    }

    ActivityStack getJustStackBehindTop(boolean isStandard, ArrayList<ActivityStack> stackList, ActivityStack topFullStack) {
        if (stackList == null || stackList.isEmpty()) {
            return null;
        }
        for (int i = stackList.size() - 1; i >= 0; i--) {
            ActivityStack stackItem = stackList.get(i);
            if (1 == stackItem.getWindowingMode() && ((!isStandard || (3 != stackItem.getActivityType() && 2 != stackItem.getActivityType())) && topFullStack != stackItem)) {
                return stackItem;
            }
        }
        return null;
    }

    public void adjustStackInQuitSplitMultiwindow(ActivityRecord pendingRecord, int windowingMode, int activityType, int mDisplayId) {
    }

    ActivityStack getStackBehindTop(boolean isStandard, ArrayList<ActivityStack> stackList, ActivityStack topFullStack, String pkgName) {
        String stackcomp;
        if (stackList == null || stackList.isEmpty()) {
            return null;
        }
        for (int i = stackList.size() - 1; i >= 0; i--) {
            ActivityStack stackItem = stackList.get(i);
            if (1 == stackItem.getWindowingMode() && ((!isStandard || (3 != stackItem.getActivityType() && 2 != stackItem.getActivityType())) && topFullStack != stackItem && stackItem.topRunningActivityLocked() != null && stackItem.topRunningActivityLocked().mActivityComponent != null && (stackcomp = stackItem.topRunningActivityLocked().mActivityComponent.flattenToShortString()) != null && stackcomp.contains(pkgName))) {
                return stackItem;
            }
        }
        return null;
    }

    public void vivoResetRecentStackForSplit(ActivityRecord r, int displayId, boolean immediately) {
    }

    public boolean skipEnterPipIfNeeded(ActivityRecord r) {
        boolean isLandscape;
        if (r != null && r.isVisible() && r.inMultiWindowMode() && r.getDisplayId() == 0 && r.getDisplay() != null && isMultiWindowSupport() && isInMultiWindowDefaultDisplay() && isMultiWindowEnterJustDefaultDisplay()) {
            DisplayContent displayContent = r.getDisplay();
            int dw = displayContent.getDisplayInfo().logicalWidth;
            int dh = displayContent.getDisplayInfo().logicalHeight;
            if (dw <= dh) {
                isLandscape = false;
            } else {
                isLandscape = true;
            }
            VivoMultiWindowConfig vivoMultiWindowConfig = VivoMultiWindowConfig.getInstance();
            if (vivoMultiWindowConfig != null && ((vivoMultiWindowConfig.isVideoAppRunning(r.toString(), r.toString()) && isLandscape) || vivoMultiWindowConfig.skipPipIfNeededInSplit(r.toString()))) {
                if (isSplitLogDebug()) {
                    VSlog.i(TAG, " skipEnterPipIfNeeded in split of " + r);
                }
                return true;
            }
        }
        return false;
    }

    private void moveDumpStacks(ArrayList<ActivityStack> stackList) {
        if (stackList == null || stackList.isEmpty()) {
            VSlog.i(TAG, "vivo_multiwindow_fmk moveStackBehindPasswrdStackAfterExitSplit-STACK: null or empty");
            return;
        }
        for (int i = stackList.size() - 1; i >= 0; i += -1) {
            VSlog.i(TAG, "vivo_multiwindow_fmk moveStackBehindPasswrdStackAfterExitSplit-STACK:" + stackList.get(i).getRootTaskId() + " " + stackList.get(i));
        }
    }

    private void moveDumpTasks(ArrayList<Task> taskList) {
        if (taskList == null || taskList.isEmpty()) {
            VSlog.i(TAG, "vivo_multiwindow_fmk moveStackBehindPasswrdStackAfterExitSplit-TASK: null or empty");
            return;
        }
        for (int i = taskList.size() - 1; i >= 0; i += -1) {
            VSlog.i(TAG, "vivo_multiwindow_fmk moveStackBehindPasswrdStackAfterExitSplit-TASK:" + taskList.get(i).mTaskId + " " + taskList.get(i));
        }
    }

    public boolean splitKeyguardTransitionTimeOutIfNeeded(boolean specialPause, ActivityRecord top, ActivityRecord prev, boolean doTimeOut) {
        return false;
    }

    public void removeKeyguardTransitionTimeOutForLoopSplitPauseIfNeeded() {
    }

    public void registerActivityObserver(IActivityObserver observer) {
        VSlog.i(TAG, "register activity observer");
        this.mActivityObserver.register(observer);
    }

    public void unregisterActivityObserver(IActivityObserver observer) {
        VSlog.i(TAG, "unregister activity observer");
        this.mActivityObserver.unregister(observer);
    }

    public void onActivityStateChanged(IBinder token, int state) {
        ActivityRecord record;
        ComponentName name;
        WindowProcessController process;
        VSlog.i(TAG, "activity state: " + state);
        if (this.mActivityObserver == null || (record = ActivityRecord.forTokenLocked(token)) == null || (name = record.mActivityComponent) == null || (process = record.app) == null) {
            return;
        }
        int pid = process.getPid();
        int uid = process.mUid;
        traceActivityState(record, state);
        int count = this.mActivityObserver.beginBroadcast();
        for (int i = 0; i < count; i++) {
            IActivityObserver observer = this.mActivityObserver.getBroadcastItem(i);
            if (observer != null) {
                if (state == 0) {
                    observer.activityResumed(pid, uid, name);
                } else if (state == 1) {
                    try {
                        observer.activityPaused(pid, uid, name);
                    } catch (RemoteException e) {
                        VSlog.e(TAG, "onActivityStateChanged RemoteException:", e);
                        unregisterActivityObserver(observer);
                    }
                }
            }
        }
        this.mActivityObserver.finishBroadcast();
    }

    private ActivityRecord checkCurrentActivity(ComponentName name, ActivityRecord activity) {
        if (name.equals(activity.mActivityComponent)) {
            return activity;
        }
        return null;
    }

    private ActivityRecord checkLastResumedActivity(ComponentName name) {
        if (this.mActivityTaskManagerService.mLastResumedActivity != null && name.equals(this.mActivityTaskManagerService.mLastResumedActivity.mActivityComponent)) {
            return this.mActivityTaskManagerService.mLastResumedActivity;
        }
        return null;
    }

    private ActivityRecord checkLastActivity(ComponentName name) {
        if (this.mActivityTaskManagerService.mLastActivityRecord != null && name.equals(this.mActivityTaskManagerService.mLastActivityRecord.mActivityComponent)) {
            return this.mActivityTaskManagerService.mLastActivityRecord;
        }
        return null;
    }

    private ComponentName getComponentName(IBinder token) {
        ActivityRecord record = ActivityRecord.forTokenLocked(token);
        if (record == null) {
            return null;
        }
        return record.mActivityComponent;
    }

    private WindowProcessController getWindowProcess(IBinder token) {
        ActivityRecord record = ActivityRecord.forTokenLocked(token);
        if (record == null) {
            return null;
        }
        return record.app;
    }

    private ActivityRecord checkActivityRecord(ComponentName name) {
        ActivityRecord activity;
        if (name == null || (activity = this.mActivityTaskManagerService.getTopDisplayFocusedStack().getTopActivity(false, false)) == null) {
            return null;
        }
        ActivityRecord activity2 = checkCurrentActivity(name, activity);
        if (activity2 != null) {
            return activity2;
        }
        ActivityRecord activity3 = checkLastResumedActivity(name);
        if (activity3 != null) {
            return activity3;
        }
        ActivityRecord activity4 = checkLastActivity(name);
        if (activity4 == null) {
            return null;
        }
        return activity4;
    }

    private IApplicationThread getActivityThread(ActivityRecord activity) {
        if (activity == null || activity.app == null) {
            return null;
        }
        return activity.app.getThread();
    }

    public boolean analyzeActivity(ComponentName name, Bundle data) {
        IApplicationThread mThread;
        ActivityRecord mActivityRecord = checkActivityRecord(name);
        if (mActivityRecord == null || (mThread = getActivityThread(mActivityRecord)) == null) {
            return false;
        }
        try {
            mThread.analyzeActivity(data, mActivityRecord.appToken);
            return true;
        } catch (RemoteException e) {
            VSlog.e(TAG, "analyzeActivity RemoteException:", e);
            return false;
        }
    }

    public void debugProcessMappForSplit(int pid, String procName, String func) {
        int i;
        if (this.mActivityTaskManagerService.isDoublePhoneForSplit()) {
            if (VivoMultiWindowConfig.SMART_MULTIWINDOW_NAME.equals(procName) && "onProcessMapped".equals(func)) {
                this.smwpid = pid;
                VSlog.d(TAG, "onProcessMapped smartmultiwindow pid:" + pid);
            } else if ("onProcessUnMapped".equals(func) && (i = this.smwpid) != -1 && i == pid) {
                VSlog.d(TAG, "onProcessUnMapped smartmultiwindow", new Throwable("onProcessUnMapped"));
            }
        }
    }

    public boolean isMultiWindowEnterJustDefaultDisplay() {
        TaskDisplayArea taskDisplayArea;
        DisplayContent displayContent = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent(0);
        if (displayContent == null || (taskDisplayArea = displayContent.getDefaultTaskDisplayArea()) == null) {
            return false;
        }
        VSlog.d(TAG, "isMultiWindowEnterJustDefaultDisplay of " + taskDisplayArea.isMultiWindowEnterJustWithDisplay());
        return taskDisplayArea.isMultiWindowEnterJustWithDisplay();
    }

    public boolean isVivoEnteringMultiWindowDefaultDisplay() {
        if (isMultiWindowSupport() && isMultiWindowEnterJustDefaultDisplay()) {
            DisplayContent defaultDisplay = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent(0);
            TaskDisplayArea taskDisplayArea = defaultDisplay != null ? defaultDisplay.getDefaultTaskDisplayArea() : null;
            if (taskDisplayArea != null && taskDisplayArea.isSplitScreenModeActivated()) {
                ActivityStack secondaryStack = taskDisplayArea.getTopStackInWindowingMode(4);
                return secondaryStack == null || !(secondaryStack == null || !secondaryStack.mCreatedByOrganizer || secondaryStack.hasChild());
            }
        }
        return false;
    }

    public void setVivoEnterSplitWay(String way) {
        this.vivoEnterSplitWay = way;
    }

    public String getVivoEnterSplitWay() {
        return this.vivoEnterSplitWay;
    }

    public boolean isMultiWindowExitedJustDefaultDisplay() {
        DisplayContent displayContent = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent(0);
        if (displayContent == null || displayContent == null) {
            return false;
        }
        VSlog.d(TAG, "isMultiWindowExitedJustDefaultDisplay of " + displayContent.isVivoMultiWindowExitedJustWithDisplay());
        return displayContent.isVivoMultiWindowExitedJustWithDisplay();
    }

    public List<ActivityManager.RunningTaskInfo> getFilteredTasksOnDisplay(int maxNum, int ignoreActivityType, int ignoreWindowingMode, int displayId) {
        int callingUid = Binder.getCallingUid();
        ArrayList<ActivityManager.RunningTaskInfo> list = new ArrayList<>();
        synchronized (this.mActivityTaskManagerService.mGlobalLock) {
            if (ActivityTaskManagerDebugConfig.DEBUG_ALL) {
                VSlog.v(TAG, "getTasks: max=" + maxNum);
            }
            this.mActivityTaskManagerService.isGetTasksAllowed("getTasks", Binder.getCallingPid(), callingUid);
        }
        return list;
    }

    public boolean isDoublePhoneForSplit() {
        return false;
    }

    public boolean skipUpdateMultiWindowAppConfig(String name) {
        if (isVivoMultiWindowSupport() && isDoublePhoneForSplit() && VivoMultiWindowConfig.SMART_MULTIWINDOW_NAME.equals(name) && this.mActivityTaskManagerService.getFocusedDisplayId() != 0) {
            if (ActivityTaskManagerDebugConfig.DEBUG_ALL) {
                VSlog.d(TAG, "skip Update process config of " + name + " id:" + this.mActivityTaskManagerService.getFocusedDisplayId());
                return true;
            }
            return true;
        }
        return false;
    }

    public void recordTencentPidForSplit(int pid, WindowProcessController proc) {
        if (proc != null && Constant.APP_WEIXIN.equals(proc.mName)) {
            this.mTencentPid = pid;
        }
    }

    public void updateTencentConfigurationForSplit() {
        if (!VivoMultiWindowConfig.IS_VIVO_MODIFY_TENCENT_TOP || !isVivoMultiWindowSupport() || -1 == this.mTencentPid) {
            return;
        }
        synchronized (this.mActivityTaskManagerService.mGlobalLock) {
            WindowProcessController proc = this.mActivityTaskManagerService.mProcessMap.getProcess(this.mTencentPid);
            if (proc != null) {
                proc.onConfigurationChanged(proc.getLastReportedConfiguration());
            }
        }
    }

    private void traceActivityState(ActivityRecord record, int state) {
        Bundle bundle = this.mActivityStateInfo;
        if (bundle == null) {
            this.mActivityStateInfo = new Bundle();
        } else {
            bundle.clear();
        }
        this.mActivityStateInfo.putInt("state", state);
        this.mActivityStateInfo.putInt("pid", record.app.getPid());
        this.mActivityStateInfo.putInt("uid", record.app.mUid);
        this.mActivityStateInfo.putParcelable("component", record.mActivityComponent);
        this.mActivityStateInfo.putParcelable("intent", record.intent);
        this.mActivityStateInfo.putString("callingPackage", record.launchedFromPackage);
        this.mActivityStateInfo.putInt("callingPid", record.launchedFromPid);
        this.mActivityStateInfo.putInt("userId", record.mUserId);
        EventTransfer.getInstance().traceActivityState(this.mActivityStateInfo);
    }

    public void setActivityControllerTimeout() {
        VivoAmsUtils.setActivityControllerTimeout();
    }

    public void cancelActivityControllerTimeout() {
        VivoAmsUtils.cancelActivityControllerTimeout();
    }

    public void onCheckClipboard() {
        try {
            IClipboard clipboard = IClipboard.Stub.asInterface(ServiceManager.getService("clipboard"));
            if (clipboard != null && clipboard.isClipboardDialogShowing()) {
                clipboard.hideClipboardDialog();
            }
        } catch (RemoteException e) {
        }
    }

    public void miniMizeWindowVivoFreeformMode(IBinder token, boolean mini) {
        this.mVivoFreeformActivityManager.miniMizeWindowVivoFreeformMode(token, mini);
    }

    public void enterResizeVivoFreeformMode(IBinder token, boolean enter) {
        this.mVivoFreeformActivityManager.enterResizeVivoFreeformMode(token, enter);
    }

    public boolean isInVivoFreeformMode(IBinder token) {
        return this.mVivoFreeformActivityManager.isInVivoFreeformMode(token);
    }

    public void moveFreeformTaskToSecondDisplay(IBinder token) {
        this.mVivoFreeformActivityManager.moveFreeformTaskToSecondDisplay(token);
    }

    public ActivityManager.StackInfo getVivoFreeformStackInfo() {
        return this.mVivoFreeformActivityManager.getVivoFreeformStackInfo();
    }

    public void updateVivoFreeFormConfig(Map configsMap) {
        this.mVivoFreeformActivityManager.updateVivoFreeFormConfig(configsMap);
    }

    public void enableVivoFreeFormRuntime(boolean enable, boolean inDirectFreeformState) {
        this.mVivoFreeformActivityManager.enableVivoFreeFormRuntime(enable, inDirectFreeformState);
    }

    public boolean isVivoFreeFormValid() {
        return this.mVivoFreeformActivityManager.isVivoFreeFormValid();
    }

    public boolean isInDirectFreeformState() {
        return this.mVivoFreeformActivityManager.isInDirectFreeformState();
    }

    public boolean isInVivoFreeform() {
        return this.mVivoFreeformActivityManager.isInVivoFreeform();
    }

    public boolean isResizeTaskFreeform() {
        return this.mVivoFreeformActivityManager.isResizeTaskFreeform();
    }

    public void setResizeTaskFreeform(boolean resizeTaskFreeform) {
        this.mVivoFreeformActivityManager.setResizeTaskFreeform(resizeTaskFreeform);
    }

    public boolean isFreeFormMin() {
        return this.mVivoFreeformActivityManager.isFreeFormMin();
    }

    public void setFreeFormMin(boolean freeFormMin) {
        this.mVivoFreeformActivityManager.setFreeFormMin(freeFormMin);
    }

    public boolean isFreeFormStackMax() {
        return this.mVivoFreeformActivityManager.isFreeFormStackMax();
    }

    public void notifyFreeFormStackMaxChanged(boolean fullScreen) {
        this.mVivoFreeformActivityManager.notifyFreeFormStackMaxChanged(fullScreen);
    }

    public boolean ignoreAlwaysCreateStackForVivoFreeform(int windowingMode, int activityType) {
        return this.mVivoFreeformActivityManager.ignoreAlwaysCreateStackForVivoFreeform(windowingMode, activityType);
    }

    public boolean isLastExitFromDirectFreeform() {
        return this.mVivoFreeformActivityManager.isLastExitFromDirectFreeform();
    }

    public void setLastExitFromDirectFreeform(boolean lastExitFromDirectFreeform) {
        this.mVivoFreeformActivityManager.setLastExitFromDirectFreeform(lastExitFromDirectFreeform);
    }

    public boolean isMultiDisplyPhone() {
        return this.mVivoFreeformActivityManager.isMultiDisplyPhone();
    }

    public ArrayList<String> getFreeFormEnabledApp() {
        return this.mVivoFreeformActivityManager.getFreeFormEnabledApp();
    }

    public ArrayList<String> getFreeFormEmergentActivity() {
        return this.mVivoFreeformActivityManager.getFreeFormEmergentActivity();
    }

    public ArrayList<String> getFreeFormFullScreenApp() {
        return this.mVivoFreeformActivityManager.getFreeFormFullScreenApp();
    }

    public ArrayList<String> getForceFullScreenActivitylistFreeform() {
        return this.mVivoFreeformActivityManager.getForceFullScreenActivitylistFreeform();
    }

    public void registerFreeformMultiWindowObserver() {
        this.mActivityTaskManagerService.mH.post(new Runnable() { // from class: com.android.server.wm.-$$Lambda$VivoActivityTaskManagerServiceImpl$s3h4-CT4wS0o15I-USldSRteS1I
            @Override // java.lang.Runnable
            public final void run() {
                VivoActivityTaskManagerServiceImpl.this.lambda$registerFreeformMultiWindowObserver$1$VivoActivityTaskManagerServiceImpl();
            }
        });
    }

    public /* synthetic */ void lambda$registerFreeformMultiWindowObserver$1$VivoActivityTaskManagerServiceImpl() {
        VivoFreeformActivityManager vivoFreeformActivityManager = this.mVivoFreeformActivityManager;
        if (vivoFreeformActivityManager != null) {
            vivoFreeformActivityManager.registerFreeformMultiWindowObserver();
        }
    }

    public void getMultiWindowConnection() {
        this.mVivoFreeformActivityManager.getMultiWindowConnection();
    }

    public void setNormalFreezingAnimaiton(IBinder token) {
        this.mVivoFreeformActivityManager.setNormalFreezingAnimaiton(token);
    }

    public void setShortFreezingAnimaiton(IBinder token) {
        this.mVivoFreeformActivityManager.setShortFreezingAnimaiton(token);
    }

    public boolean isUnlockingToFreeform() {
        return this.mVivoFreeformActivityManager.isUnlockingToFreeform();
    }

    public void setIsUnlockingToFreeform(boolean isUnlockingToFreeform) {
        this.mVivoFreeformActivityManager.setIsUnlockingToFreeform(isUnlockingToFreeform);
    }

    public boolean moveTaskToBackWhenFinishActivityForWechat(ActivityRecord r, IBinder token) {
        return this.mVivoFreeformActivityManager.moveTaskToBackWhenFinishActivityForWechat(r, token);
    }

    public void sendProcessChangeForGame() {
        this.mVivoFreeformActivityManager.sendProcessChangeForGame();
    }

    public void ensureFocusForVivoFreeform(ActivityRecord r) {
        this.mVivoFreeformActivityManager.ensureFocusForVivoFreeform(r);
    }

    public void moveFreeformWindowToTopWhenSetFocusTask(ActivityRecord r) {
        this.mVivoFreeformActivityManager.moveFreeformWindowToTopWhenSetFocusTask(r);
    }

    public boolean ignoreThrowExceptionWhenResizeTask(Task task) {
        return this.mVivoFreeformActivityManager.ignoreThrowExceptionWhenResizeTask(task);
    }

    public boolean toggleVivoFreeformWindowingMode(ActivityStack stack, ActivityRecord r) {
        return this.mVivoFreeformActivityManager.toggleVivoFreeformWindowingMode(stack, r);
    }

    public void updateFreeformTaskUseGlobalConfig(Configuration tempGlobalConfig) {
        this.mVivoFreeformActivityManager.updateFreeformTaskUseGlobalConfig(tempGlobalConfig);
    }

    public void getFreeformPkg() {
        this.mVivoFreeformActivityManager.getFreeformPkg();
    }

    public boolean updateFreeformAppConfig(WindowProcessController app, Configuration tempConfig) {
        return this.mVivoFreeformActivityManager.updateFreeformAppConfig(app, tempConfig);
    }

    public void checkAndExitFreeformWhenSlideToHome(int targetActivityType, ActivityRecord launchedTargetActivity, int reorderMode) {
        this.mVivoFreeformActivityManager.checkAndExitFreeformWhenSlideToHome(targetActivityType, launchedTargetActivity, reorderMode);
    }

    public void exitFreeformWhenLockTask() {
        this.mVivoFreeformActivityManager.exitFreeformWhenLockTask();
    }

    public boolean isFirstTimeUnlock() {
        return this.mVivoFreeformActivityManager.isFirstTimeUnlock();
    }

    public void setIsFirstTimeUnlock(boolean isFirstTimeUnlock) {
        this.mVivoFreeformActivityManager.setIsFirstTimeUnlock(isFirstTimeUnlock);
    }

    public boolean isStartingEmergent() {
        return this.mVivoFreeformActivityManager.isStartingEmergent();
    }

    public void setIsStartingEmergent(boolean isStartingEmergent) {
        this.mVivoFreeformActivityManager.setIsStartingEmergent(isStartingEmergent);
    }

    public boolean isStartingRecentOnHome() {
        return this.mVivoFreeformActivityManager.isStartingRecentOnHome();
    }

    public void setIsStartingRecentOnHome(boolean isStartingRecentOnHome) {
        this.mVivoFreeformActivityManager.setIsStartingRecentOnHome(isStartingRecentOnHome);
    }

    public boolean isStartingPasswdOnHome() {
        return this.mVivoFreeformActivityManager.isStartingPasswdOnHome();
    }

    public void setIsStartingPasswdOnHome(boolean isStartingPasswdOnHome) {
        this.mVivoFreeformActivityManager.setIsStartingPasswdOnHome(isStartingPasswdOnHome);
    }

    public boolean isStartingPassword() {
        return this.mVivoFreeformActivityManager.isStartingPassword();
    }

    public void setIsStartingPassword(boolean isStartingPassword) {
        this.mVivoFreeformActivityManager.setIsStartingPassword(isStartingPassword);
    }

    public void setFreeformKeepR(ActivityRecord freeformKeepR) {
        this.mVivoFreeformActivityManager.setFreeformKeepR(freeformKeepR);
    }

    public ActivityRecord getFreeformKeepR() {
        return this.mVivoFreeformActivityManager.getFreeformKeepR();
    }

    public int getDisplayRotation(DisplayContent display) {
        return this.mVivoFreeformActivityManager.getDisplayRotation(display);
    }

    public Task getPrevVivoFreeformTask() {
        return this.mVivoFreeformActivityManager.getPrevVivoFreeformTask();
    }

    public void setPrevVivoFreeformTask(Task prevVivoFreeformTask) {
        this.mVivoFreeformActivityManager.setPrevVivoFreeformTask(prevVivoFreeformTask);
    }

    public void exitCurrentFreeformTaskForTransitToNext(ActivityRecord source, Task next, LaunchParamsController.LaunchParams launchParams) {
        this.mVivoFreeformActivityManager.exitCurrentFreeformTaskForTransitToNext(source, next, launchParams);
    }

    public void setFreeformResizedInCurrentRotationIfNeed(Task task) {
        this.mVivoFreeformActivityManager.setFreeformResizedInCurrentRotationIfNeed(task);
    }

    public boolean isCurrentRotationHasResized() {
        return this.mVivoFreeformActivityManager.isCurrentRotationHasResized();
    }

    public void setCurrentRotationHasResized(boolean currentRotationHasResized) {
        this.mVivoFreeformActivityManager.setCurrentRotationHasResized(currentRotationHasResized);
    }

    /* loaded from: classes.dex */
    private final class GameModeSettingObserver extends ContentObserver {
        private final Uri isGameModeOpenUri;
        private final Uri isGameStateUri;

        public GameModeSettingObserver(Handler handler) {
            super(handler);
            this.isGameModeOpenUri = Settings.System.getUriFor(VivoAIKeyExtend.GAME_DISTURB_ENABLED);
            this.isGameStateUri = Settings.System.getUriFor("is_game_mode");
            ContentResolver resolver = VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mContext.getContentResolver();
            resolver.registerContentObserver(this.isGameModeOpenUri, false, this, -1);
            int value = Settings.System.getIntForUser(VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mContext.getContentResolver(), VivoAIKeyExtend.GAME_DISTURB_ENABLED, 1, -2);
            VivoActivityTaskManagerServiceImpl.this.isGameModeOpen = value == 1;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.isGameModeOpenUri.equals(uri)) {
                int value = Settings.System.getIntForUser(VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mContext.getContentResolver(), VivoAIKeyExtend.GAME_DISTURB_ENABLED, 1, -2);
                VivoActivityTaskManagerServiceImpl.this.isGameModeOpen = value == 1;
            }
        }
    }

    public boolean isGameModeOpen() {
        return this.isGameModeOpen;
    }

    public boolean notifyActivityColdStarting(ActivityRecord next) {
        ProcessRecord processRecord;
        boolean isGameModeOpen = this.mActivityTaskManagerService.isGameModeOpen();
        if (isGameModeOpen) {
            boolean exist = false;
            boolean isActivityType = false;
            boolean needNotify = false;
            WindowProcessController wpc = this.mActivityTaskManagerService.getProcessController(next.processName, next.info.applicationInfo.uid);
            if (wpc != null && wpc.hasThread()) {
                exist = true;
                ProcessRecord processRecord2 = (ProcessRecord) wpc.mOwner;
                if (processRecord2 != null && processRecord2.getVivoProcessRecord().getHostingRecord() != null && processRecord2.getVivoProcessRecord().getHostingRecord().getType() != null && processRecord2.getVivoProcessRecord().getHostingRecord().getType().contains(VivoFirewall.TYPE_ACTIVITY) && (next.getPreloadFlags() & 1) == 0) {
                    isActivityType = true;
                }
            }
            if (!this.mActivityTaskManagerService.mControllerIsAMonkey && this.mGameModeController != null) {
                if (!exist) {
                    needNotify = true;
                } else if (exist && !isActivityType && (processRecord = (ProcessRecord) wpc.mOwner) != null && processRecord.getVivoProcessRecord() != null && !processRecord.getVivoProcessRecord().getColdStartAlreadyNotify()) {
                    processRecord.getVivoProcessRecord().setColdStartAlreadyNotify(true);
                    needNotify = true;
                }
                if (needNotify) {
                    try {
                        if (ActivityTaskManagerDebugConfig.DEBUG_STATES) {
                            VSlog.d(TAG, "activityColdStarting processName=" + next.processName);
                        }
                        this.mGameModeController.activityColdStarting((Intent) null, next.processName, (Bundle) null);
                    } catch (RemoteException e) {
                    }
                }
            }
            return needNotify;
        }
        return false;
    }

    public void notifyActivityScreenshot(ActivityRecord prev) {
        if (!this.mActivityTaskManagerService.mControllerIsAMonkey && this.mGameModeController != null) {
            try {
                if (ActivityTaskManagerDebugConfig.DEBUG_STATES) {
                    VSlog.d(TAG, "activityScreenshot activity=" + prev.mActivityComponent + " " + Debug.getCaller());
                }
                this.mGameModeController.activityScreenshot(prev.mActivityComponent);
            } catch (RemoteException e) {
            }
        }
    }

    public void setGameModeController(IGameModeController controller) {
        this.mActivityTaskManagerService.mAmInternal.enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "setGameModeController()");
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            this.mGameModeController = controller;
            int controllerPid = controller != null ? Binder.getCallingPid() : 0;
            VSlog.i(TAG, "GameModeController pid : " + controllerPid);
        }
    }

    public IGameModeController getGameModeController() {
        return this.mGameModeController;
    }

    public void onSystemReady() {
        HandlerThread handlerThread = new HandlerThread("GameMode");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        Handler handler = new Handler(this.mHandlerThread.getLooper());
        this.mHandler = handler;
        handler.post(new Runnable() { // from class: com.android.server.wm.-$$Lambda$VivoActivityTaskManagerServiceImpl$iXpWr5Ui0hfFmy1loA4lImTWeuA
            @Override // java.lang.Runnable
            public final void run() {
                VivoActivityTaskManagerServiceImpl.this.lambda$onSystemReady$2$VivoActivityTaskManagerServiceImpl();
            }
        });
        this.mThirdPartyIncomingManager.systemReady(this.mHandlerThread.getLooper());
    }

    public /* synthetic */ void lambda$onSystemReady$2$VivoActivityTaskManagerServiceImpl() {
        this.mGameModeSettingsObserver = new GameModeSettingObserver(this.mHandler);
    }

    public void setMonkeyState(boolean isMonkey) {
        if (isMonkey != this.mIsMonkey) {
            RmsInjectorImpl.getInstance().setMonkeyState(isMonkey);
        }
        this.mIsMonkey = isMonkey;
    }

    public boolean vivoCanOccludeKeyguard(String packageName, boolean lastOcclude) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        for (OccludeKeyguardDeathRecipient deathRecipient : this.mVivoOccludeDeathRecipients.values()) {
            if (deathRecipient != null && deathRecipient.mPkgName.equals(packageName)) {
                if (deathRecipient.mCanOcclude) {
                    return true;
                }
                if (lastOcclude && !deathRecipient.mOccludeTimeOut) {
                    return true;
                }
            }
        }
        return false;
    }

    /* loaded from: classes.dex */
    private final class OccludeKeyguardDeathRecipient implements IBinder.DeathRecipient {
        IVivoKeyguardOccludeCallback mCallback;
        IBinder mOccludePkgToken;
        String mPkgName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        boolean mOccludeTimeOut = false;
        boolean mCanOcclude = false;
        boolean mOccluded = false;

        OccludeKeyguardDeathRecipient(IBinder token) {
            Slog.i(VivoActivityTaskManagerServiceImpl.TAG, "New OccludeKeyguardDeathRecipient " + this + ",token = " + token);
            this.mOccludePkgToken = token;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.getGlobalLock()) {
                Slog.i(VivoActivityTaskManagerServiceImpl.TAG, "OccludeKeyguardDeathRecipient dead " + this + ",token = " + this.mOccludePkgToken);
                VivoActivityTaskManagerServiceImpl.this.mVivoOccludeDeathRecipients.remove(this.mOccludePkgToken);
                VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mRootWindowContainer.resumeFocusedStacksTopActivities();
            }
        }

        public String toString() {
            return "OccludeKeyguardDeathRecipient{mOccludePkgToken=" + this.mOccludePkgToken + ", mPkgName='" + this.mPkgName + "', mCallback=" + this.mCallback + ", mOccludeTimeOut=" + this.mOccludeTimeOut + ", mCanOcclude=" + this.mCanOcclude + ", mOccluded=" + this.mOccluded + '}';
        }
    }

    public boolean enableVivoOccludeKeyguardPackage(IBinder token, String packageName, IVivoKeyguardOccludeCallback callback) {
        this.mActivityTaskManagerService.mAmInternal.enforceCallingPermission("com.vivo.permission.START_ACTIVITY_OCCLUDE_KEYGUARD", "enableVivoOccludeKeyguardPackage");
        if (token == null) {
            return false;
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                OccludeKeyguardDeathRecipient deathRecipient = this.mVivoOccludeDeathRecipients.get(token);
                if (deathRecipient == null) {
                    deathRecipient = new OccludeKeyguardDeathRecipient(token);
                    try {
                        token.linkToDeath(deathRecipient, 0);
                        this.mVivoOccludeDeathRecipients.put(token, deathRecipient);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "enableVivoOccludeKeyguardPackage fail add token,token is already dead.");
                        return false;
                    }
                }
                deathRecipient.mCallback = callback;
                deathRecipient.mPkgName = packageName;
                deathRecipient.mCanOcclude = true;
                deathRecipient.mOccludeTimeOut = false;
                Slog.i(TAG, "enableVivoOccludeKeyguardPackage packageName = " + packageName + " token = " + token);
            }
            return true;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public boolean disableVivoOccludeKeyguardPackage(IBinder token, String packageName, IVivoKeyguardOccludeCallback callback) {
        this.mActivityTaskManagerService.mAmInternal.enforceCallingPermission("com.vivo.permission.START_ACTIVITY_OCCLUDE_KEYGUARD", "disableVivoOccludeKeyguardPackage");
        if (token == null) {
            return false;
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
                OccludeKeyguardDeathRecipient deathRecipient = this.mVivoOccludeDeathRecipients.get(token);
                if (deathRecipient != null) {
                    token.unlinkToDeath(deathRecipient, 0);
                    this.mVivoOccludeDeathRecipients.remove(token);
                    Slog.i(TAG, "disableVivoOccludeKeyguardPackage token = " + token);
                    return true;
                }
                Slog.i(TAG, "disableVivoOccludeKeyguardPackage fail disable token packageName = " + deathRecipient.mPkgName);
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void notifyVivoOccludeChange(String pkgName, int reason) {
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            for (IBinder token : this.mVivoOccludeDeathRecipients.keySet()) {
                OccludeKeyguardDeathRecipient deathRecipient = this.mVivoOccludeDeathRecipients.get(token);
                if (deathRecipient != null) {
                    IVivoKeyguardOccludeCallback callback = deathRecipient.mCallback;
                    if (reason != 0) {
                        if (reason != 1) {
                            if (reason == 2) {
                                if (deathRecipient.mOccluded) {
                                    deathRecipient.mOccludeTimeOut = true;
                                    if (callback != null) {
                                        Slog.i(TAG, "notifyVivoOccludeChange time out token = " + token + " packageName = " + deathRecipient.mPkgName);
                                        callback.onOccludeChange(deathRecipient.mPkgName, false, reason);
                                    }
                                }
                            } else if (reason == 3 && pkgName != null) {
                                try {
                                    if (pkgName.equals(deathRecipient.mPkgName)) {
                                        deathRecipient.mOccluded = false;
                                        if (callback != null) {
                                            Slog.i(TAG, "notifyVivoOccludeChange other app token = " + token + " packageName = " + deathRecipient.mPkgName);
                                            callback.onOccludeChange(deathRecipient.mPkgName, false, reason);
                                        }
                                    }
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (pkgName != null && pkgName.equals(deathRecipient.mPkgName)) {
                            deathRecipient.mOccludeTimeOut = true;
                            deathRecipient.mCanOcclude = false;
                            deathRecipient.mOccluded = false;
                            if (callback != null) {
                                Slog.i(TAG, "notifyVivoOccludeChange dead token = " + token + " packageName = " + deathRecipient.mPkgName);
                                callback.onOccludeChange(deathRecipient.mPkgName, true, reason);
                            }
                        }
                    } else if (pkgName != null && pkgName.equals(deathRecipient.mPkgName)) {
                        deathRecipient.mOccluded = true;
                        if (callback != null) {
                            Slog.i(TAG, "notifyVivoOccludeChange succee token = " + token + " packageName = " + deathRecipient.mPkgName);
                            callback.onOccludeChange(deathRecipient.mPkgName, true, reason);
                        }
                    }
                }
            }
        }
    }

    public boolean shouldStartIncoming(Intent intent, int userId, ActivityRecord sourceRecord) {
        if (!this.mThirdPartyIncomingManager.shouldStartIncoming(intent, userId, sourceRecord)) {
            return false;
        }
        return true;
    }

    public void setSecureController(IActivityController controller, boolean imAMonkey, WindowProcessControllerMap mProcessMap) {
        VivoSoftwareLock.setSecureController(controller, imAMonkey, mProcessMap);
        try {
            ((SystemAutoRecoverManagerInternal) LocalServices.getService(SystemAutoRecoverManagerInternal.class)).setActivityController(controller, imAMonkey);
        } catch (Exception e) {
            VLog.d(TAG, "setActivityController cause exception: " + e);
        }
    }

    public void reportAmsTimeoutException(String callerPackage, String ComponentName, String reason, Intent intent) {
        AmsDataManager.getInstance().reportAmsTimeoutException(callerPackage, ComponentName, reason, intent);
    }

    public void setVivoActivityController(IVivoActivityController controller, boolean imAMonkey, WindowProcessControllerMap mProcessMap) {
        this.mActivityTaskManagerService.mAmInternal.enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "setVivoActivityController()");
        synchronized (this.mActivityTaskManagerService.mGlobalLock) {
            this.mActivityTaskManagerService.mVivoController = controller;
            VivoSoftwareLock.setVivoControllerPid(controller != null ? Binder.getCallingPid() : 0);
            VivoSoftwareLock.setVivoControllerUid(controller != null ? Binder.getCallingUid() : 0);
            VSlog.i(TAG, "VivoController Pid : " + VivoSoftwareLock.getVivoControllerPid() + " amonkey:" + imAMonkey);
            WindowProcessController processController = mProcessMap.getProcess(VivoSoftwareLock.getVivoControllerPid());
            this.mActivityTaskManagerService.isSecureControllerForMultiWindow = false;
            if (processController != null && processController.mName.startsWith(VivoAppIsolationController.NOTIFY_IQOO_SECURE_PACKAGE)) {
                this.mActivityTaskManagerService.isSecureControllerForMultiWindow = true;
            }
        }
    }

    public boolean checkVivoPermission(Intent intent) {
        String action = intent.getAction();
        if ("android.media.action.VIDEO_CAPTURE".equals(action)) {
            return VivoPermissionManager.checkCallingVivoPermission("android.permission.CAMERA");
        }
        if ("android.media.action.IMAGE_CAPTURE".equals(action)) {
            return VivoPermissionManager.checkCallingVivoPermission("android.permission.CAMERA");
        }
        if ("android.intent.action.CALL".equals(action)) {
            return VivoPermissionManager.checkCallingVivoPermission("android.permission.CALL_PHONE");
        }
        return true;
    }

    public void setCallingPackageForChooser(Intent intent, String callingPackage) {
        if (intent.getAction() != null && "android.intent.action.CHOOSER".equals(intent.getAction())) {
            intent.putExtra("vivo.calling.package", callingPackage);
        }
    }

    public void setEasyShareController(IEasyShareController controller) {
        this.mVivoEasyShareManager.setEasyShareController(controller);
    }

    public void notifyDragResult(int action, boolean result, String packageName) {
        this.mVivoEasyShareManager.notifyDragResult(action, result, packageName);
    }

    public boolean isInPcSharing() {
        return this.mVivoEasyShareManager.isInPcSharing();
    }

    public void clearSuperResolutionFirstApp() {
        long ident = Binder.clearCallingIdentity();
        try {
            Settings.Global.putInt(this.mActivityTaskManagerService.mContext.getContentResolver(), Constant.SUPER_RESOLUTION_FIRST_APP, 0);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void clearSuperResolutionAppsAlphaAnimator(String pkgName) {
        long ident = Binder.clearCallingIdentity();
        try {
            ContentResolver resolver = this.mActivityTaskManagerService.mContext.getContentResolver();
            String superResolutionAppsAlphaAnimator = Settings.Global.getString(resolver, Constant.SUPER_RESOLUTION_APPS_ALPHA_ANIMATOR);
            if (superResolutionAppsAlphaAnimator != null) {
                try {
                    JSONObject object = new JSONObject(superResolutionAppsAlphaAnimator);
                    object.put(pkgName, 0);
                    Settings.Global.putString(resolver, Constant.SUPER_RESOLUTION_APPS_ALPHA_ANIMATOR, object.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public ActivityTaskManagerInternal.SleepToken getSpecTokenForVirtualDisplay(int displayId) {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return null;
        }
        return this.mVirtualSleepTokens.get(Integer.valueOf(displayId));
    }

    public void updateSpecTokenForVirtualDisplay(final int displayId, final boolean acquire) {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return;
        }
        this.mActivityTaskManagerService.mH.post(new Runnable() { // from class: com.android.server.wm.-$$Lambda$VivoActivityTaskManagerServiceImpl$MZ0NvR-uoMB80FJJRdPiVeWn25g
            @Override // java.lang.Runnable
            public final void run() {
                VivoActivityTaskManagerServiceImpl.this.lambda$updateSpecTokenForVirtualDisplay$3$VivoActivityTaskManagerServiceImpl(acquire, displayId);
            }
        });
    }

    public /* synthetic */ void lambda$updateSpecTokenForVirtualDisplay$3$VivoActivityTaskManagerServiceImpl(boolean acquire, int displayId) {
        synchronized (this.mActivityTaskManagerService.mGlobalLock) {
            if (MultiDisplayManager.DEBUG) {
                VSlog.d("VivoStack", "updateSpecToken acquire: " + acquire + " ,displayId: " + displayId + " ,token: " + this.mVirtualSleepTokens.get(Integer.valueOf(displayId)));
            }
            try {
                if (acquire) {
                    if (!MultiDisplayManager.isVirtualDisplayRunning(displayId)) {
                        return;
                    }
                    if (this.mVirtualSleepTokens.get(Integer.valueOf(displayId)) == null) {
                        ActivityTaskManagerInternal.SleepToken token = this.mActivityTaskManagerService.mRootWindowContainer.createSleepToken(displayId == 95555 ? "Vivo-RMS" : "Vivo-CAR", displayId);
                        if (token != null) {
                            this.mVirtualSleepTokens.put(Integer.valueOf(displayId), token);
                        }
                    }
                } else if (this.mVirtualSleepTokens.get(Integer.valueOf(displayId)) != null) {
                    this.mVirtualSleepTokens.get(Integer.valueOf(displayId)).release();
                    this.mVirtualSleepTokens.remove(Integer.valueOf(displayId));
                }
                this.mActivityTaskManagerService.updateSleepIfNeededLocked();
            } catch (Exception ex) {
                VSlog.w("VivoStack", "Exception thrown during updateSpecToken", ex);
            }
        }
    }

    public ComponentName getTopActivityForDisplay(int displayId) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            synchronized (this.mActivityTaskManagerService.mGlobalLock) {
                DisplayContent display = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent(displayId);
                if (display != null) {
                    ActivityRecord activity = display.topRunningActivity();
                    return activity != null ? activity.mActivityComponent : null;
                }
                return null;
            }
        }
        return null;
    }

    public void notifyStackDisplayChanged(ActivityStack stack, boolean add) {
        if (stack != null && MultiDisplayManager.isVivoDisplay(stack.getDisplayId())) {
            updateVivoActivityState(stack.topRunningActivityLocked(), add);
        }
    }

    public void notifyAppDiedForVirtualDisplay(ProcessRecord app) {
        if (app != null) {
            VSlog.d("VivoObserver", "notifyAppDiedForVirtualDisplay app: " + app);
            updateVivoActivityState(app);
            synchronized (this.mMirraLock) {
                if (this.mMirraPackage != null && this.mMirraPackage.equals(app.processName)) {
                    updateCastStates(null);
                }
            }
            if (MultiDisplayManager.SUPPORT_BG_GAME) {
                handleAppCrash4GameMode(app);
            }
        }
    }

    public void updateCastStates(final String packageName) {
        synchronized (this.mMirraLock) {
            this.mMirraPackage = packageName;
            this.mActivityTaskManagerService.mH.post(new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.5
                @Override // java.lang.Runnable
                public void run() {
                    VSlog.d("VivoCar", "updateCastStates pkg = " + packageName);
                    StringBuilder sb = new StringBuilder();
                    sb.append("APPSharePck=");
                    String str = packageName;
                    if (str == null) {
                        str = "NULL";
                    }
                    sb.append(str);
                    AudioSystem.setParameters(sb.toString());
                    SystemProperties.set("sys.vivo.bg.mirroring", packageName != null ? "1" : "0");
                    Settings.Secure.putInt(VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mContext.getContentResolver(), "vscreen_bg_mirroring", packageName != null ? 1 : 0);
                    if (packageName != null) {
                        VivoActivityTaskManagerServiceImpl.this.mAudioManager.requestAudioFocus(new AudioFocusListener(), 3, 1);
                        return;
                    }
                    VivoActivityTaskManagerServiceImpl.this.mAudioManager.abandonAudioFocus(null);
                    DisplayContent castDisplay = VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent((int) SceneManager.POWER_PRIORITY);
                    if (castDisplay != null) {
                        castDisplay.setCastOrientation(0);
                    }
                }
            });
        }
    }

    public void updateVivoActivityState(IBinder token, int state) {
        updateVivoActivityState(token, null, null, state, false);
    }

    public void updateVivoActivityState(ActivityRecord activity, int state) {
        updateVivoActivityState(null, activity, null, state, false);
    }

    private void updateVivoActivityState(ActivityRecord activity, boolean add) {
        updateVivoActivityState(null, activity, null, 4, add);
    }

    private void updateVivoActivityState(ProcessRecord app) {
        updateVivoActivityState(null, null, app, 3, false);
    }

    private void updateVivoActivityState(IBinder token, ActivityRecord activity, ProcessRecord app, int state, boolean add) {
        ComponentName name;
        String processName;
        int displayId;
        int displayId2;
        int uid;
        int i;
        synchronized (this.mVivoActivityObserver) {
            try {
                if (app != null) {
                    int pid = app.pid;
                    String processName2 = app.processName;
                    name = null;
                    processName = processName2;
                    displayId = 0;
                    displayId2 = 0;
                    uid = pid;
                } else {
                    ActivityRecord record = token != null ? ActivityRecord.forTokenLocked(token) : activity;
                    if (record != null && record.app != null) {
                        WindowProcessController process = record.app;
                        int pid2 = process.getPid();
                        int uid2 = process.mUid;
                        int displayId3 = record.getDisplayId();
                        ComponentName name2 = record.mActivityComponent;
                        name = name2;
                        processName = null;
                        displayId = displayId3;
                        displayId2 = uid2;
                        uid = pid2;
                    }
                    VSlog.w("VivoObserver", "updateVivoActivityState return " + record);
                    return;
                }
                int count = this.mVivoActivityObserver.beginBroadcast();
                int i2 = 0;
                while (i2 < count) {
                    IVivoActivityObserver observer = this.mVivoActivityObserver.getBroadcastItem(i2);
                    if (observer == null) {
                        i = i2;
                    } else if (state == 0) {
                        i = i2;
                        observer.activityResumed(uid, displayId2, name, displayId);
                    } else if (state == 1) {
                        i = i2;
                        observer.activityPaused(uid, displayId2, name, displayId);
                    } else if (state == 2) {
                        i = i2;
                        observer.activityFinished(uid, displayId2, name, displayId);
                    } else if (state == 3) {
                        i = i2;
                        observer.activityDied(uid, displayId2, processName);
                    } else if (state != 4) {
                        i = i2;
                    } else {
                        i = i2;
                        int i3 = displayId;
                        try {
                            observer.activityDisplayChanged(uid, displayId2, name, i3, add);
                        } catch (RemoteException e) {
                            VSlog.w("VivoObserver", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, e);
                            unregisterVivoActivityObserver(observer);
                        }
                    }
                    i2 = i + 1;
                }
                this.mVivoActivityObserver.finishBroadcast();
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void registerVivoActivityObserver(IVivoActivityObserver observer) {
        VSlog.i("VivoObserver", "register activity observer");
        this.mVivoActivityObserver.register(observer);
    }

    public void unregisterVivoActivityObserver(IVivoActivityObserver observer) {
        VSlog.i("VivoObserver", "unregister activity observer");
        this.mVivoActivityObserver.unregister(observer);
    }

    public boolean adjustVDConfiguration(WindowProcessController app, Configuration tmpConfig) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            for (int i = 0; i < app.mActivities.size(); i++) {
                ActivityRecord activityRecord = (ActivityRecord) app.mActivities.get(i);
                if (activityRecord != null) {
                    int displayId = activityRecord.getDisplayId();
                    if (MultiDisplayManager.isVivoDisplay(displayId)) {
                        Configuration configCopy = new Configuration(tmpConfig);
                        Configuration overrideConfig = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayOverrideConfiguration(displayId);
                        configCopy.updateFrom(overrideConfig);
                        app.onConfigurationChanged(configCopy);
                        if (MultiDisplayManager.DEBUG) {
                            Slog.d("VivoCar", "Adjust app: " + app.mName + " with new config " + app.getConfiguration());
                            return true;
                        }
                        return true;
                    }
                    return false;
                }
            }
            return false;
        }
        return false;
    }

    public boolean moveToDisplayForVirtualDisplay(int displayId, boolean toDesktop, boolean onTop) {
        int historyDisplayId = displayId == 0 ? SceneManager.POWER_PRIORITY : 0;
        return moveToDisplayForVirtualDisplay(displayId, historyDisplayId, toDesktop, onTop);
    }

    public boolean moveToDisplayForVirtualDisplay(int displayId, int historyDisplayId, boolean onTop) {
        return moveToDisplayForVirtualDisplay(displayId, historyDisplayId, true, onTop);
    }

    public boolean moveToDisplayForVirtualDisplay(int displayId, int historyDisplayId, boolean toDesktop, boolean onTop) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            synchronized (this.mActivityTaskManagerService.mGlobalLock) {
                try {
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    RootWindowContainer rootWindowContainer = this.mActivityTaskManagerService.mRootWindowContainer;
                    DisplayContent targetDisplay = rootWindowContainer.getDisplayContent(displayId);
                    DisplayContent sourceDisplay = rootWindowContainer.getDisplayContent(historyDisplayId);
                    if (targetDisplay != null && sourceDisplay != null) {
                        ActivityStack topStack = sourceDisplay.getTopStack();
                        if (topStack == null) {
                            VSlog.d("VivoCar", "Couldn't find any stack of display : " + sourceDisplay.getDisplayId());
                            return false;
                        }
                        ActivityRecord currentTop = topStack.getTopNonFinishingActivity();
                        if (currentTop == null) {
                            VSlog.d("VivoCar", "Couldn't find running of stack : " + topStack);
                            return false;
                        }
                        long ident = Binder.clearCallingIdentity();
                        this.mActivityTaskManagerService.deferWindowLayout();
                        if (displayId == 90000) {
                            try {
                                targetDisplay.setCastOrientation(-2);
                                if (sourceDisplay.getRotation() == 1 || sourceDisplay.getRotation() == 3) {
                                    targetDisplay.setCastOrientation(0);
                                }
                                snapshotForCast(currentTop.getTask());
                            } catch (Exception e) {
                                VSlog.w("VivoCar", " moveToDisplay expt!", e);
                                this.mActivityTaskManagerService.continueWindowLayout();
                            }
                        }
                        topStack.toDesktop = toDesktop;
                        rootWindowContainer.moveStackToDisplay(topStack.getRootTaskId(), displayId, onTop);
                        rootWindowContainer.ensureVisibilityAndConfig(currentTop, currentTop.getDisplayId(), false, false);
                        VSlog.d("VivoCar", "moveToDisplay  : " + displayId + " ,onTop: " + onTop + " ,toDestop: " + toDesktop + " ,topActivity: " + currentTop);
                        if (onTop && displayId == 0 && MultiDisplayManager.isVivoDisplay(historyDisplayId) && topStack.getDisplayId() == displayId) {
                            VSlog.d("VivoCar", "Move from resume-to-resume " + currentTop);
                            updateVivoActivityState(currentTop, 0);
                        }
                        this.mActivityTaskManagerService.continueWindowLayout();
                        Binder.restoreCallingIdentity(ident);
                        return true;
                    }
                    VSlog.d("VivoCar", "Couldn't find display : " + targetDisplay + "  ||  " + sourceDisplay);
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
        return false;
    }

    public String mirracastPackage() {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            synchronized (this.mActivityTaskManagerService.mGlobalLock) {
                DisplayContent castDisplay = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent((int) SceneManager.POWER_PRIORITY);
                if (castDisplay == null) {
                    VSlog.d("VivoCar", "Couldn't find cast display");
                    return null;
                }
                ActivityRecord top = castDisplay.topRunningActivity();
                return top != null ? top.packageName : null;
            }
        }
        return null;
    }

    public void updateCastDisplayOrientation(ActivityRecord r) {
        DisplayContent castDisplay;
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && r.getDisplayId() == 90000 && (castDisplay = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent((int) SceneManager.POWER_PRIORITY)) != null) {
            castDisplay.setCastOrientation(-2);
        }
    }

    private void snapshotForCast(Task task) {
        TaskSnapshotController snapshotController = this.mActivityTaskManagerService.mWindowManager.mTaskSnapshotController;
        ArraySet<Task> tasks = Sets.newArraySet(new Task[]{task});
        VSlog.i("VivoCar", "snapshotForCast task=" + task);
        snapshotController.snapshotTasks(tasks);
    }

    public boolean moveGameToBackground() {
        boolean checkIfTrigBackground;
        if (!MultiDisplayManager.SUPPORT_BG_GAME) {
            return false;
        }
        synchronized (this.mActivityTaskManagerService.mGlobalLock) {
            long ident = Binder.clearCallingIdentity();
            VSlog.d("VivoGameMode", "moveGameToBackground");
            checkIfTrigBackground = checkIfTrigBackground();
            Binder.restoreCallingIdentity(ident);
        }
        return checkIfTrigBackground;
    }

    public boolean setGameModeCallback(IRemoteCallback callback) {
        if (MultiDisplayManager.SUPPORT_BG_GAME) {
            VSlog.d("VivoGameMode", "setVivoGameController callback : " + callback);
            if (callback == null) {
                return false;
            }
            this.mVivoGameCallback = callback;
            IBinder.DeathRecipient death = new IBinder.DeathRecipient() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.6
                @Override // android.os.IBinder.DeathRecipient
                public void binderDied() {
                    VSlog.d("VivoGameMode", "hang: given caller IBinder is already dead!");
                    VivoActivityTaskManagerServiceImpl.this.mVivoGameCallback = null;
                }
            };
            try {
                callback.asBinder().linkToDeath(death, 0);
                return true;
            } catch (RemoteException e) {
                VSlog.w("VivoGameMode", "link exception.");
                this.mVivoGameCallback = null;
                return false;
            }
        }
        return false;
    }

    public boolean sendGameModeCmd(int cmd, Bundle options) {
        if (MultiDisplayManager.SUPPORT_BG_GAME) {
            synchronized (this.mActivityTaskManagerService.mGlobalLock) {
                long ident = Binder.clearCallingIdentity();
                VSlog.d("VivoGameMode", "sendGameModeCmd cmd: " + cmd);
                this.mExitFromClient = true;
                if ((cmd & 2) != 0) {
                    if (sInNormalGameMode) {
                        exitGameMode(false);
                    } else if (sInSleepGameMode) {
                        exitSleepMode();
                        this.mActivityTaskManagerService.updateSleepIfNeededLocked();
                    }
                } else if ((cmd & 4) != 0) {
                    if (cmd == 5) {
                        this.mExitFromClient = false;
                    }
                    exitGameMode(true);
                }
                Binder.restoreCallingIdentity(ident);
            }
            return true;
        }
        return false;
    }

    public Bundle getGameModeState(int requestType) {
        if (MultiDisplayManager.SUPPORT_BG_GAME) {
            VSlog.d("VivoGameMode", "getGameModeState pinProc : " + this.mBgProc);
            synchronized (this.mActivityTaskManagerService.mGlobalLock) {
                if (this.mBgProc != null) {
                    Bundle bundle = new Bundle();
                    bundle.putInt(GAMEMODE_PROC_KEY, this.mBgProc.mPid);
                    bundle.putString(GAMEMODE_PKG_KEY, this.mBgProc.mInfo != null ? this.mBgProc.mInfo.packageName : null);
                    bundle.putLong(GAMEMODE_TIME_KEY, this.mLastGameModeTime);
                    return bundle;
                }
                return null;
            }
        }
        return null;
    }

    private static boolean isSupportRunInBackground(String packageName) {
        String[] strArr;
        synchronized (mLock) {
            if (packageName != null) {
                if (vComponentList != null && MultiDisplayManager.SUPPORT_BG_GAME) {
                    for (String name : vComponentList) {
                        VSlog.d("VivoGameMode", "isSupportRunInBackground name : " + name);
                        if (packageName.equals(name)) {
                            VSlog.d("VivoGameMode", "isSupportRunInBackground return true.");
                            return true;
                        }
                    }
                    VSlog.d("VivoGameMode", "isSupportRunInBackground return false.");
                    return false;
                }
            }
            return false;
        }
    }

    private void doCupSetRq(int pid, boolean foreground) {
        String path = foreground ? "/dev/vivo_rsc/fg/cgroup.procs" : "/dev/vivo_rsc/bg/cgroup.procs";
        VSlog.d("VivoGameMode", "doCupSetRq: " + Integer.toString(pid));
        try {
            FileWriter sysrq_trigger = new FileWriter(path);
            sysrq_trigger.write(Integer.toString(pid));
            sysrq_trigger.close();
        } catch (IOException e) {
            VSlog.w("VivoGameMode", "Failed to write cgroup: " + e.getMessage());
        }
    }

    private void enableNormalGameMode(boolean enable) {
        VSlog.d("VivoGameMode", "enableNormalGameMode : " + enable);
        sInNormalGameMode = enable;
    }

    private void enableSleepGameMode(boolean enable) {
        VSlog.d("VivoGameMode", "enableSleepGameMode : " + enable);
        sInSleepGameMode = enable;
    }

    public boolean isGameModeValid() {
        return sInNormalGameMode;
    }

    public boolean isSleepGameModeValid() {
        return sInSleepGameMode;
    }

    public void autoExitGameMode() {
        autoExitGameMode(false);
    }

    public void autoExitGameMode(boolean toTop) {
        if (!MultiDisplayManager.SUPPORT_BG_GAME) {
            return;
        }
        VSlog.d("VivoGameMode", "autoExitGameMode: " + this.mExitFromClient + " ,inNormal: " + sInNormalGameMode + " ,inSleep: " + sInSleepGameMode + " ,toTop: " + toTop);
        if (sInNormalGameMode || sInSleepGameMode) {
            if (!this.mExitFromClient) {
                sendCmdMessage(2, toTop);
            }
            if (toTop && this.mBgStack != null && this.mBgProc != null) {
                this.mActivityTaskManagerService.mAmInternal.sendProcessActivityChangeMessage4GameMode(this.mBgProc.mPid, this.mBgProc.mInfo.uid, true);
                VSlog.d("VivoGameMode", "autoExitGameMode sendProcMsg.");
            }
            if (sInNormalGameMode && toTop && this.mBgStack != null && this.mBgProc != null) {
                VSlog.d("VivoGameMode", "setProcessGroup FG: " + this.mBgProc.mPid);
                Process.setProcessGroup(this.mBgProc.mPid, 5);
                doCupSetRq(this.mBgProc.mPid, true);
            }
            if (sInSleepGameMode && this.mBgStack != null && this.mBgProc != null) {
                VSlog.d("VivoGameMode", "setProcessGroup2 FG: " + this.mBgProc.mPid);
                Process.setProcessGroup(this.mBgProc.mPid, 5);
                doCupSetRq(this.mBgProc.mPid, true);
            }
            clearGameMode();
        }
    }

    public void exitSleepMode() {
        if (sInSleepGameMode && this.mBgStack != null && this.mBgProc != null) {
            VSlog.d("VivoGameMode", "setProcessGroup3 FG: " + this.mBgProc.mPid);
            Process.setProcessGroup(this.mBgProc.mPid, 5);
            doCupSetRq(this.mBgProc.mPid, true);
        }
        clearGameMode();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class GameToastRunnable implements Runnable {
        String mText;
        Toast mToast;

        public GameToastRunnable(String txt) {
            this.mText = txt;
        }

        @Override // java.lang.Runnable
        public void run() {
            Toast makeText = Toast.makeText(VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mContext, this.mText, 1);
            this.mToast = makeText;
            makeText.show();
        }
    }

    /* loaded from: classes.dex */
    private final class GameModeObserver extends ContentObserver {
        public GameModeObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            try {
                String pkgs = Settings.System.getString(VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mContext.getContentResolver(), VivoActivityTaskManagerServiceImpl.ENABLED_SUSPEND_MODE_GAMES);
                synchronized (VivoActivityTaskManagerServiceImpl.mLock) {
                    VSlog.d("VivoGameMode", "onChange pkgs : " + pkgs);
                    if (pkgs != null && !Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(pkgs)) {
                        VivoActivityTaskManagerServiceImpl.vComponentList = pkgs.split(":");
                        VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mH.post(new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.GameModeObserver.1
                            @Override // java.lang.Runnable
                            public void run() {
                                synchronized (VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mGlobalLock) {
                                    VivoActivityTaskManagerServiceImpl.this.prepareSecondaryDisplayDevice(null);
                                }
                            }
                        });
                        return;
                    }
                    VivoActivityTaskManagerServiceImpl.vComponentList = null;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void registerGameModeObserver() {
        if (!MultiDisplayManager.SUPPORT_BG_GAME) {
            return;
        }
        ContentResolver resolver = this.mActivityTaskManagerService.mContext.getContentResolver();
        try {
            String pkgs = Settings.System.getString(resolver, ENABLED_SUSPEND_MODE_GAMES);
            synchronized (mLock) {
                VSlog.d("VivoGameMode", "registerGameModeObserver pkgs : " + pkgs);
                if (pkgs != null && !Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(pkgs)) {
                    vComponentList = pkgs.split(":");
                }
                vComponentList = null;
            }
            resolver.registerContentObserver(Settings.System.getUriFor(ENABLED_SUSPEND_MODE_GAMES), false, new GameModeObserver(this.mActivityTaskManagerService.mH));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    BatteryManagerInternal getBatteryManagerInternal() {
        if (this.mBatteryManagerInternal == null) {
            this.mBatteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
        }
        return this.mBatteryManagerInternal;
    }

    public boolean handleIncomingRecentActivity(ActivityRecord r) {
        if (MultiDisplayManager.SUPPORT_BG_GAME && r != null && "com.vivo.upslide/.recents.RecentsActivity".equals(r.shortComponentName)) {
            return checkIfTrigBackground();
        }
        return false;
    }

    public void abortRecentGameMode() {
        if (!MultiDisplayManager.SUPPORT_BG_GAME) {
            return;
        }
        VSlog.d("VivoGameMode", "abortRecentGameMode inNormal : " + sInNormalGameMode);
        if (sInNormalGameMode) {
            updateSecondaryDisplayDevice(false);
            clearGameMode();
        }
    }

    public void handleAppCrash4GameMode(ProcessRecord app) {
        String str;
        if (MultiDisplayManager.SUPPORT_BG_GAME && (str = this.mBgPkgName) != null && str.equals(app.processName)) {
            VSlog.d("VivoGameMode", "handleAppCrash4GameMode st : " + app + " ,callers=" + Debug.getCallers(3));
            sendCmdMessage(8);
            if (sInNormalGameMode) {
                updateSecondaryDisplayDevice(false);
            }
            clearGameMode();
        }
    }

    private void sendCmdMessage(int cmd) {
        sendCmdMessage(cmd, false);
    }

    private void sendCmdMessage(int cmd, boolean front) {
        VSlog.d("VivoGameMode", "sendCmdMessage: " + cmd + " ,front: " + front + " ,callers=" + Debug.getCallers(5));
        if (this.mVivoGameCallback != null) {
            Bundle bundle = new Bundle();
            bundle.putInt(GAMEMODE_CMD_KEY, cmd);
            if (cmd == 1) {
                WindowProcessController windowProcessController = this.mBgProc;
                bundle.putInt(GAMEMODE_PROC_KEY, windowProcessController != null ? windowProcessController.mPid : 0);
                WindowProcessController windowProcessController2 = this.mBgProc;
                bundle.putString(GAMEMODE_PKG_KEY, (windowProcessController2 == null || windowProcessController2.mInfo == null) ? null : this.mBgProc.mInfo.packageName);
            } else if (cmd == 2) {
                bundle.putBoolean(GAMEMODE_FRONT_KEY, front);
            }
            try {
                this.mVivoGameCallback.sendResult(bundle);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendBroadcastToApp(final boolean enable, final String packageName) {
        VSlog.d("VivoGameMode", "sendBroadcastToApp : " + packageName + " ,enable: " + enable);
        this.mActivityTaskManagerService.mH.post(new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.7
            @Override // java.lang.Runnable
            public void run() {
                Intent intent = new Intent("vivo.intent.action.GAMEMODE_STATE_CHANGED");
                intent.putExtra("enable", enable);
                intent.putExtra(VivoActivityTaskManagerServiceImpl.GAMEMODE_PKG_KEY, packageName);
                VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            }
        });
    }

    private void clearGameMode() {
        ActivityStack activityStack = this.mBgStack;
        if (activityStack != null) {
            activityStack.mKeepAwake = false;
            this.mBgStack = null;
            sendBroadcastToApp(false, this.mBgPkgName);
            String str = this.mBgPkgName;
            WindowProcessController windowProcessController = this.mBgProc;
            notifyGameState(false, str, windowProcessController != null ? windowProcessController.mUid : 0);
            this.mBgPkgName = null;
        }
        WindowProcessController windowProcessController2 = this.mBgProc;
        if (windowProcessController2 != null) {
            windowProcessController2.isInBgStack = false;
            this.mBgProc = null;
        }
        sGameModeDisplayId = -1;
        this.mLastGameModeTime = 0L;
        this.mExitFromClient = false;
        this.mActivityTaskManagerService.mH.removeCallbacks(this.mMuteGMRunnable);
        this.mActivityTaskManagerService.mH.postDelayed(this.mMuteGMRunnable, 800L);
        enableNormalGameMode(false);
        enableSleepGameMode(false);
    }

    public void exitGameMode(boolean onTop) {
        VSlog.d("VivoGameMode", "exitGameMode top: " + onTop + " ,inNormal: " + sInNormalGameMode + " ,inSleep: " + sInSleepGameMode + " ,bg: " + this.mBgStack + " callers=" + Debug.getCallers(3));
        ActivityRecord ar = this.mBgStack.topRunningActivityLocked();
        boolean needdoubleResume = false;
        if (sInNormalGameMode) {
            VSlog.d("VivoGameMode", "moveStackToDisplay mBgStack: " + this.mBgStack + " to DEFAULT_DISPLAY ");
            if (!onTop) {
                this.mBgStack.startPausingLocked(false, false, (ActivityRecord) null);
            }
            if (ar != null && onTop) {
                ar.setVisibility(false);
                needdoubleResume = true;
            }
            this.mActivityTaskManagerService.mRootWindowContainer.moveStackToDisplay(this.mBgStack.getRootTaskId(), 0, onTop);
            if (needdoubleResume) {
                ar.setVisibility(true);
            }
            if (!onTop) {
                updateSecondaryDisplayDevice(false);
            }
        }
        clearGameMode();
    }

    private CharSequence getPackageName(String packageName) {
        if (packageName == null) {
            return "null";
        }
        try {
            Context context = this.mActivityTaskManagerService.mContext.createPackageContext(packageName, 0);
            CharSequence name = context.getApplicationInfo().loadLabel(context.getPackageManager());
            VSlog.d("VivoGameMode", "getPackageName name : " + ((Object) name));
            return name;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Unable to parse package name", e);
            return "null";
        }
    }

    private void connectPEM() {
        VSlog.d("VivoGameMode", "connectPEM.");
        this.mActivityTaskManagerService.mH.post(new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.9
            @Override // java.lang.Runnable
            public void run() {
                try {
                    Intent intent = new Intent("com.vivo.pem.PemService");
                    intent.setPackage(ProxyConfigs.CTRL_MODULE_PEM);
                    boolean result = VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mContext.bindService(intent, VivoActivityTaskManagerServiceImpl.this.mPEMConn, 1);
                    VSlog.d("VivoGameMode", "connectPEM ret: " + result);
                } catch (Exception e) {
                    VSlog.w("VivoGameMode", "bindService exp:", e);
                }
            }
        });
    }

    private void disconnectPEM() {
        try {
            this.mActivityTaskManagerService.mContext.unbindService(this.mPEMConn);
            mRemote = null;
        } catch (Exception e) {
            VSlog.w("VivoGameMode", "unbindService exp:", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyGameState(final boolean enter, final String pkgName, final int uid) {
        if (mRemote == null) {
            connectPEM();
        } else if (pkgName == null) {
            VSlog.d("VivoGameMode", "notifyGameState pkgName is null!");
        } else {
            this.mActivityTaskManagerService.mH.post(new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.10
                @Override // java.lang.Runnable
                public void run() {
                    VSlog.d("VivoGameMode", "notifyGameState packageName: " + pkgName + " ,uid: " + uid + " ,enter: " + enter);
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    boolean z = true;
                    try {
                        try {
                            data.writeInterfaceToken("com.vivo.pem.IPemr");
                            data.writeInt(106);
                            data.writeInt(enter ? VivoActivityTaskManagerServiceImpl.DUBRESUME_MINUTE_TIMEOUT : 0);
                            data.writeString(pkgName);
                            VivoActivityTaskManagerServiceImpl.mRemote.transact(7, data, reply, 0);
                            reply.readException();
                            boolean result = reply.readInt() != 0;
                            VSlog.d("VivoGameMode", "notifyGameState ret: " + result);
                        } catch (RemoteException e) {
                            VSlog.w("VivoGameMode", "notifyGameState exp:", e);
                        }
                        reply.recycle();
                        data.recycle();
                        data = Parcel.obtain();
                        reply = Parcel.obtain();
                        try {
                            try {
                                data.writeInterfaceToken("com.vivo.pem.IPemr");
                                data.writeInt(KernelConfig.N2M_ENABLE);
                                data.writeInt(enter ? 1 : 0);
                                data.writeInt(uid);
                                VivoActivityTaskManagerServiceImpl.mRemote.transact(7, data, reply, 0);
                                reply.readException();
                                if (reply.readInt() == 0) {
                                    z = false;
                                }
                                boolean result2 = z;
                                VSlog.d("VivoGameMode", "noteCpuLimit ret: " + result2);
                            } catch (RemoteException e2) {
                                VSlog.w("VivoGameMode", "noteCpuLimit exp:", e2);
                            }
                        } finally {
                        }
                    } finally {
                    }
                }
            });
        }
    }

    public void prepareSecondaryDisplayDevice(IBinder token) {
        ActivityRecord runActivity;
        if (!MultiDisplayManager.SUPPORT_BG_GAME) {
            return;
        }
        if (isGameModeOpen()) {
            if (token == null) {
                runActivity = ensureBottomActivity();
            } else {
                runActivity = ActivityRecord.forTokenLocked(token);
            }
            if (runActivity != null && "com.vivo.sdkplugin".equals(runActivity.packageName)) {
                runActivity = ensureBottomActivity();
            }
            if (runActivity == null) {
                VSlog.d("VivoGameMode", "prepareDisplayDevice skip token: " + token);
            } else if (isSupportRunInBackground(runActivity.packageName)) {
                if (!this.mSecondDisplayAdded) {
                    updateSecondaryDisplayDevice(true);
                }
            } else if (this.mSecondDisplayAdded && !sInNormalGameMode) {
                updateSecondaryDisplayDevice(false);
            } else {
                VSlog.d("VivoGameMode", "prepareDisplayDevice ac: " + runActivity + " ,isAdded: " + this.mSecondDisplayAdded + " ,inNormal: " + sInNormalGameMode + " callers=" + Debug.getCallers(2));
            }
        } else if (sInNormalGameMode || sInSleepGameMode) {
            sendCmdMessage(2);
            exitGameMode(false);
        } else if (this.mSecondDisplayAdded) {
            updateSecondaryDisplayDevice(false);
        }
    }

    private void updateSecondaryDisplayDevice(boolean open) {
        VSlog.d("VivoGameMode", "updateSecondaryDisplayDevice opt : " + open + " callers=" + Debug.getCallers(2));
        this.mSecondDisplayAdded = open;
        if (open) {
            WindowManager windowManager = (WindowManager) this.mActivityTaskManagerService.mContext.getSystemService(WindowManager.class);
            DisplayMetrics realMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getRealMetrics(realMetrics);
            DisplayManager mDisplayManager = (DisplayManager) this.mActivityTaskManagerService.mContext.getSystemService(DisplayManager.class);
            VirtualDisplay createVirtualDisplay = mDisplayManager.createVirtualDisplay("VivoGameMode", realMetrics.widthPixels, realMetrics.heightPixels, realMetrics.densityDpi, null, Consts.ProcessStates.FOCUS);
            this.mVirtualDisplay = createVirtualDisplay;
            createVirtualDisplay.setDisplayState(true);
            return;
        }
        VirtualDisplay virtualDisplay = this.mVirtualDisplay;
        if (virtualDisplay != null) {
            virtualDisplay.release();
            this.mVirtualDisplay = null;
        }
    }

    private void clearSecondDisplayDevice() {
        ActivityStack activityStack;
        if (sInNormalGameMode && (activityStack = this.mBgStack) != null && activityStack.getDisplayId() != -1 && this.mBgStack.getDisplayId() != 0) {
            sInNormalGameMode = false;
            this.mBgStack.startPausingLocked(false, false, (ActivityRecord) null);
            this.mActivityTaskManagerService.mRootWindowContainer.moveStackToDisplay(this.mBgStack.getRootTaskId(), 0, false);
            ActivityRecord ar = this.mBgStack.topRunningActivityLocked();
            if (ar != null) {
                VSlog.i("VivoGameMode", "move exit vis:" + ar.isVisible());
                ar.setVisible(false);
            }
        }
        clearGameMode();
    }

    private ActivityRecord ensureBottomActivity() {
        ActivityRecord bottomActivity = null;
        DisplayContent display = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent(0);
        if (display != null) {
            bottomActivity = display.topRunningActivity();
        }
        VSlog.d("VivoGameMode", "ensureBottomActivity : " + bottomActivity);
        if (bottomActivity == null || bottomActivity.app == null || bottomActivity.packageName == null) {
            VSlog.d("VivoGameMode", "ensureBottomActivity skip topTask: " + bottomActivity);
            return null;
        } else if (bottomActivity.mActivityComponent != null && BLACKLIST_COMPONENT_NAME.equals(bottomActivity.mActivityComponent.flattenToShortString())) {
            VSlog.d("VivoGameMode", "ensureBottomActivity skip topActivity: " + bottomActivity.mActivityComponent);
            return null;
        } else {
            return bottomActivity;
        }
    }

    public boolean checkIfKeepAwake() {
        if (MultiDisplayManager.SUPPORT_BG_GAME) {
            if (!isGameModeOpen() || sInNormalGameMode || sInSleepGameMode) {
                VSlog.d("VivoGameMode", "checkIfKeepAwake donothing: " + sInNormalGameMode + " ," + sInSleepGameMode);
                return false;
            }
            ActivityRecord bActivity = ensureBottomActivity();
            if (bActivity == null || !isSupportRunInBackground(bActivity.packageName)) {
                return false;
            }
            int plug = getBatteryManagerInternal().getPlugType();
            int level = getBatteryManagerInternal().getBatteryLevel();
            VSlog.d("VivoGameMode", "checkIfKeepAwake battery : " + plug + " , " + level);
            if (plug != 0 || level >= 10) {
                clearGameMode();
                this.mBgStack = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent(0).getFocusedStack();
                VSlog.d("VivoGameMode", "checkIfKeepAwake mBgStack : " + this.mBgStack);
                this.mBgStack.mKeepAwake = true;
                WindowProcessController windowProcessController = bActivity.app;
                this.mBgProc = windowProcessController;
                windowProcessController.isInBgStack = true;
                this.mBgPkgName = bActivity.packageName;
                VSlog.d("VivoGameMode", "checkIfKeepAwake muteProc : " + this.mBgProc);
                if (this.mBgProc.mPid > 0 && SystemProperties.getBoolean("persist.vivo.bgame.volume", true)) {
                    final int pid = this.mBgProc.mPid;
                    this.mActivityTaskManagerService.mH.removeCallbacks(this.mMuteGMRunnable);
                    this.mActivityTaskManagerService.mH.post(new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.12
                        @Override // java.lang.Runnable
                        public void run() {
                            VSlog.d("VivoGameMode", "Immedially mutePid: " + pid);
                            AudioSystem.setParameters("mutePidGM=" + pid);
                        }
                    });
                    VSlog.d("VivoGameMode", "setProcessGroup2 BG : " + pid);
                    Process.setProcessGroup(pid, 0);
                    doCupSetRq(pid, false);
                }
                enableSleepGameMode(true);
                notifyGameState(true, this.mBgPkgName, this.mBgProc.mUid);
                sendBroadcastToApp(true, this.mBgPkgName);
                this.mLastGameModeTime = SystemClock.elapsedRealtime();
                sendCmdMessage(1);
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean checkIfTrigBackground() {
        return checkIfTrigBackground(false);
    }

    public boolean checkIfTrigBackground(boolean fromRecent) {
        if (MultiDisplayManager.SUPPORT_BG_GAME) {
            if (!isGameModeOpen()) {
                VSlog.d("VivoGameMode", "checkIfTrigBackground not open!");
                return false;
            } else if (this.mActivityTaskManagerService.mWindowManager.isSplitScreenModeActivated()) {
                VSlog.d("VivoGameMode", "checkIfTrigBackground SplitScreenMode, skip!");
                return false;
            } else {
                ActivityRecord bActivity = ensureBottomActivity();
                if (bActivity != null && isSupportRunInBackground(bActivity.packageName)) {
                    int plug = getBatteryManagerInternal().getPlugType();
                    int level = getBatteryManagerInternal().getBatteryLevel();
                    if (plug == 0 && level < 10) {
                        VSlog.d("VivoGameMode", "checkIfTrigBackground skip battery : " + plug + " , " + level);
                        this.mActivityTaskManagerService.mH.post(new GameToastRunnable(this.mActivityTaskManagerService.mContext.getString(51249665)));
                        return false;
                    }
                    DisplayContent display = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent(85000);
                    VSlog.d("VivoGameMode", "checkIfTrigBackground display : " + display);
                    if (display != null) {
                        clearSecondDisplayDevice();
                        sGameModeDisplayId = 85000;
                        ActivityStack focusedStack = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent(0).getFocusedStack();
                        this.mBgStack = focusedStack;
                        focusedStack.mKeepAwake = true;
                        WindowProcessController windowProcessController = bActivity.app;
                        this.mBgProc = windowProcessController;
                        windowProcessController.isInBgStack = true;
                        this.mBgPkgName = bActivity.packageName;
                        VSlog.d("VivoGameMode", "checkIfTrigBackground muteProc : " + this.mBgProc);
                        if (this.mBgProc.mPid > 0 && SystemProperties.getBoolean("persist.vivo.bgame.volume", true)) {
                            final int pid = this.mBgProc.mPid;
                            this.mActivityTaskManagerService.mH.removeCallbacks(this.mMuteGMRunnable);
                            this.mActivityTaskManagerService.mH.post(new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.13
                                @Override // java.lang.Runnable
                                public void run() {
                                    VSlog.d("VivoGameMode", "Immedially mutePid: " + pid);
                                    AudioSystem.setParameters("mutePidGM=" + pid);
                                }
                            });
                            VSlog.d("VivoGameMode", "setProcessGroup BG: " + pid);
                            Process.setProcessGroup(pid, 0);
                            doCupSetRq(pid, false);
                        }
                        enableNormalGameMode(true);
                        notifyGameState(true, this.mBgPkgName, this.mBgProc.mUid);
                        sendBroadcastToApp(true, this.mBgPkgName);
                        this.mLastGameModeTime = SystemClock.elapsedRealtime();
                        sendCmdMessage(1);
                        this.mActivityTaskManagerService.moveStackToDisplay(this.mBgStack.getRootTaskId(), 85000);
                        this.mActivityTaskManagerService.mAmInternal.sendProcessActivityChangeMessage4GameMode(this.mBgProc.mPid, this.mBgProc.mInfo.uid, false);
                        VSlog.d("VivoGameMode", "checkIfTrigBackground sendProcMsg.");
                        return true;
                    }
                    VSlog.d("VivoGameMode", "checkIfTrigBackground failed.");
                    return false;
                }
                VSlog.d("VivoGameMode", "Not in whitelist.");
                return false;
            }
        }
        return false;
    }

    public boolean isControlledByRemote() {
        return this.mVivoAppShareManager.isControlledByRemote();
    }

    public String getForegroundPackage() {
        return this.mVivoAppShareManager.getForegroundPackage();
    }

    public boolean isOnAppShareDisplay(String packageName, int userId) {
        return this.mVivoAppShareManager.isOnAppShareDisplay(packageName, userId);
    }

    public boolean moveAppToDisplay(String packageName, int userId, int displayId) {
        return this.mVivoAppShareManager.moveAppToDisplay(packageName, userId, displayId);
    }

    public int getAppShareMainDisplayOrientation(String packageName) {
        return this.mVivoAppShareManager.getAppShareMainDisplayOrientation(packageName);
    }

    public int getActivityNumOnAppShareDisplay() {
        return this.mVivoAppShareManager.getActivityNumOnAppShareDisplay();
    }

    public ArrayList<String> getAppSharePackagesWithUserId() {
        return this.mVivoAppShareManager.getAppSharePackagesWithUserId();
    }

    public void setAppShareController(IAppShareController controller) {
        this.mVivoAppShareManager.setAppShareController(controller);
    }

    public void setIsAppResumed(boolean isAppResumed) {
        this.mVivoAppShareManager.setIsAppResumed(isAppResumed);
    }

    public String getAppShareDisplayTopActivity() {
        return this.mVivoAppShareManager.getAppShareDisplayTopActivity();
    }

    public String getProcessInfo(String packageName, int userId) {
        return this.mVivoAppShareManager.getProcessInfo(packageName, userId);
    }

    public void setCameraExitReason(String reason) {
        this.mVivoAppShareManager.setCameraExitReason(reason);
    }

    public void updateAppShareConfig(int type) {
        this.mVivoAppShareManager.updateAppShareConfig(type);
    }

    public boolean isAppShareForeground() {
        return this.mVivoAppShareManager.isAppShareForeground();
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [int] */
    /* JADX WARN: Type inference failed for: r2v2 */
    /* JADX WARN: Type inference failed for: r2v3 */
    /* JADX WARN: Type inference failed for: r2v6 */
    public List<ActivityManager.RunningTaskInfo> getFilteredTasksWithPackage(int maxNum, boolean filterOnlyVisibleRecents, String packageName) {
        ?? length;
        WindowManagerGlobalLock windowManagerGlobalLock;
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        boolean crossUser = this.mActivityTaskManagerService.isCrossUserAllowed(callingPid, callingUid);
        int[] profileIds = this.mActivityTaskManagerService.getUserManager().getProfileIds(UserHandle.getUserId(callingUid), true);
        ArraySet<Integer> callingProfileIds = new ArraySet<>();
        int i = 0;
        while (true) {
            length = profileIds.length;
            if (i >= length) {
                break;
            }
            callingProfileIds.add(Integer.valueOf(profileIds[i]));
            i++;
        }
        ArrayList<ActivityManager.RunningTaskInfo> list = new ArrayList<>();
        WindowManagerGlobalLock windowManagerGlobalLock2 = this.mActivityTaskManagerService.mGlobalLock;
        synchronized (windowManagerGlobalLock2) {
            try {
                try {
                    if (ActivityTaskManagerDebugConfig.DEBUG_ALL) {
                        try {
                            StringBuilder sb = new StringBuilder();
                            sb.append("getTasks: max=");
                            try {
                                sb.append(maxNum);
                                Slog.v(TAG, sb.toString());
                            } catch (Throwable th) {
                                th = th;
                                length = windowManagerGlobalLock2;
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            length = windowManagerGlobalLock2;
                            throw th;
                        }
                    }
                    boolean allowed = this.mActivityTaskManagerService.isGetTasksAllowed("getTasks", callingPid, callingUid);
                    if (this.mVivoAppShareManager.isOnAppShareDisplay(packageName, UserHandle.getCallingUserId())) {
                        this.mActivityTaskManagerService.mRootWindowContainer.getRunningTasksWithDisplayId(maxNum, list, filterOnlyVisibleRecents, callingUid, allowed, crossUser, callingProfileIds, 10086);
                        windowManagerGlobalLock = windowManagerGlobalLock2;
                    } else {
                        windowManagerGlobalLock = windowManagerGlobalLock2;
                        this.mActivityTaskManagerService.mRootWindowContainer.getRunningTasks(maxNum, list, filterOnlyVisibleRecents, callingUid, allowed, crossUser, callingProfileIds);
                    }
                    return list;
                } catch (Throwable th3) {
                    th = th3;
                    length = windowManagerGlobalLock2;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    public boolean shouldBlockVivoCameraFinish(ActivityRecord r) {
        return this.mVivoAppShareManager.shouldBlockVivoCameraFinishLocked(r);
    }

    public void setResumedActivityForAppShareLocked(ActivityRecord r) {
        this.mVivoAppShareManager.setResumedActivityForAppShareLocked(r);
    }

    public boolean shouldBlockRequestedOrientation(ActivityRecord r, int requestedOrientation) {
        return this.mVivoAppShareManager.shouldBlockRequestedOrientationLocked(r, requestedOrientation);
    }

    public boolean shouldBlockEnterPictureInPictureMode(ActivityRecord r) {
        return this.mVivoAppShareManager.shouldBlockEnterPictureInPictureModeLocked(r);
    }

    public void notifyAppShareAboutPipMode(boolean enter) {
        this.mVivoAppShareManager.notifyAppShareAboutPipMode(enter);
    }

    public Task changeTaskForAppShareIfNeededLocked(Task task) {
        return this.mVivoAppShareManager.changeTaskForAppShareIfNeededLocked(task);
    }

    public boolean needInterceptAudioRecord() {
        return this.mVivoAppShareManager.needInterceptAudioRecord();
    }

    public void startAppShareForShortcutIfNeeded(String packageName, int userId) {
        this.mVivoAppShareManager.startAppShareForShortcutIfNeeded(packageName, userId);
    }

    public void notifyAppShareLockScreenShown(boolean shown) {
        this.mVivoAppShareManager.notifyAppShareLockScreenShown(shown);
    }

    public void updateConfigurationForInputMethod(int displayId, boolean force) {
        this.mVivoAppShareManager.updateConfigurationForInputMethod(displayId, force);
    }

    public void setRequestedOrientationAppShared(ActivityRecord r, int requestedOrientation) {
        this.mVivoAppShareManager.setRequestedOrientationAppShared(r, requestedOrientation);
    }

    public void adjustConfigurationForAppShareMode(WindowProcessController app, Configuration currentConfig, Configuration tempConfig) {
        this.mVivoAppShareManager.adjustConfigurationForAppShareMode(app, currentConfig, tempConfig);
    }

    public void updateDisplayOverrideConfigurationForAppShareLocked(int displayId) {
        this.mVivoAppShareManager.updateDisplayOverrideConfigurationForAppShareLocked(displayId);
    }

    public boolean skipFreezeActivity(ActivityRecord r) {
        return this.mVivoAppShareManager.skipFreezeActivity(r);
    }

    public void updateConfigurationAppShareLocked(WindowProcessController app, Configuration configuration) {
        this.mVivoAppShareManager.updateConfigurationAppShareLocked(app, configuration);
    }

    public void onInputMethodMoved(int pid, int displayId) {
        this.mVivoAppShareManager.onInputMethodMoved(pid, displayId);
    }

    public void adjustAppShareActivityInfoWhenStartLocked(int callingPid, int callingUid, String callingPackage, Intent intent, ActivityInfo aInfo) {
        this.mVivoAppShareManager.adjustAppShareActivityInfoWhenStartLocked(callingPid, callingUid, callingPackage, intent, aInfo);
    }

    public void decideKeepAppShareRotationLocked(int callingPid, int callingUid, String callingPackage, Intent intent, ActivityInfo aInfo, ActivityOptions options) {
        this.mVivoAppShareManager.decideKeepAppShareRotationLocked(callingPid, callingUid, callingPackage, intent, aInfo, options);
    }

    public void setAppShareModeFreezeTime(int time) {
        this.mVivoAppShareManager.setAppShareModeFreezeTime(time);
    }

    public boolean decideFullScreenForAppShareLocked(ActivityRecord r, ActivityOptions options, boolean fullscreen) {
        return this.mVivoAppShareManager.decideFullScreenForAppShareLocked(r, options, fullscreen);
    }

    public void updateOverrideConfigurationLockedForAppShare(ActivityRecord r, Configuration mergedConfiguration) {
        this.mVivoAppShareManager.updateOverrideConfigurationLockedForAppShare(r, mergedConfiguration);
    }

    public boolean removeTaskIfKillNeeded(int taskId, boolean killProcess) {
        boolean removeTaskById;
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            int callingPid = Binder.getCallingPid();
            if (ActivityTaskManagerDebugConfig.DEBUG_TASKS && callingPid != Process.myPid()) {
                VSlog.d(TAG, "removeTask from pid " + callingPid + " killProcess " + killProcess);
            }
            long ident = Binder.clearCallingIdentity();
            removeTaskById = this.mActivityTaskManagerService.mStackSupervisor.removeTaskById(taskId, killProcess, true, "remove-task-if-kill");
            Binder.restoreCallingIdentity(ident);
        }
        return removeTaskById;
    }

    public boolean checkActivityInfo(ActivityRecord r, int type) {
        if (type == 0) {
            return blockedByUserSetup(r);
        }
        return false;
    }

    private boolean blockedByUserSetup(ActivityRecord r) {
        UserSetupObserver userSetupObserver;
        if (r.intent == null || r.intent.getComponent() == null || (userSetupObserver = this.mUserSetupObserver) == null) {
            return false;
        }
        boolean block = userSetupObserver.shouldBlocked(r.intent.getComponent().flattenToShortString());
        if (block) {
            VSlog.i(TAG, "blockedByUserSetup.");
            if (this.mActivityTaskManagerService.mUiHandler != null) {
                this.mActivityTaskManagerService.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.14
                    @Override // java.lang.Runnable
                    public void run() {
                        Toast.makeText(VivoActivityTaskManagerServiceImpl.this.mActivityTaskManagerService.mContext, 51249654, 0).show();
                    }
                });
            }
        }
        return block;
    }

    public void initUserSetup() {
        UserSetupObserver userSetupObserver = new UserSetupObserver(UnifiedConfigThread.getHandler(), this.mActivityTaskManagerService.mContext);
        this.mUserSetupObserver = userSetupObserver;
        userSetupObserver.init();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class UserSetupObserver extends ContentObserver {
        private static final String TAG = "UserSetup";
        private boolean mBlacklistDisabled;
        private List<String> mBlockedList;
        private final Context mContext;

        public UserSetupObserver(Handler handler, Context context) {
            super(handler);
            this.mBlockedList = Collections.emptyList();
            this.mContext = context;
        }

        public void init() {
            UnifiedConfigThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.VivoActivityTaskManagerServiceImpl.UserSetupObserver.1
                @Override // java.lang.Runnable
                public void run() {
                    UserSetupObserver.this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("vivo_user_setup_inactive"), false, UserSetupObserver.this);
                    UserSetupObserver.this.update();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void update() {
            String blockedStr = Settings.Secure.getString(this.mContext.getContentResolver(), "vivo_user_setup_blacklist");
            this.mBlockedList = TextUtils.isEmpty(blockedStr) ? Collections.emptyList() : Arrays.asList(blockedStr.split(","));
            this.mBlacklistDisabled = Settings.Secure.getInt(this.mContext.getContentResolver(), "vivo_user_setup_inactive", 0) != 0;
        }

        private boolean isUserSetupComplete() {
            return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
        }

        public boolean shouldBlocked(String component) {
            if (this.mBlacklistDisabled || this.mBlockedList == null || isUserSetupComplete()) {
                return false;
            }
            return this.mBlockedList.contains(component);
        }

        public boolean isDisabled() {
            return this.mBlacklistDisabled;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            update();
        }
    }
}