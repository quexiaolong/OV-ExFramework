package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.multidisplay.MultiDisplayManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Debug;
import android.os.FtBuild;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.FtFeature;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.IDisplayWindowRotationController;
import android.view.IVivoProposedRotationChangeListener;
import android.view.IVivoWindowFocusListener;
import android.view.IWindow;
import android.view.InputWindowHandle;
import android.view.KeyEvent;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import com.android.internal.policy.NavigationBarPolicy;
import com.android.server.LocalServices;
import com.android.server.am.VivoFrozenPackageSupervisor;
import com.android.server.am.frozen.FrozenQuicker;
import com.android.server.input.InputManagerService;
import com.android.server.policy.VivoKeyguardOverlayController;
import com.android.server.policy.key.VivoAIKeyExtend;
import com.android.server.wm.WindowManagerInternal;
import com.vivo.appshare.AppShareConfig;
import com.vivo.face.common.data.Constants;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.services.autorecover.SystemAutoRecoverManagerInternal;
import com.vivo.services.rms.RMWms;
import com.vivo.services.superresolution.Constant;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import vivo.app.VivoFrameworkFactory;
import vivo.app.car.ICarWindowObserver;
import vivo.app.vivoscreenshot.IVivoScreenshotManager;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoWmsImpl implements IVivoWms {
    private static final int DURATION_COMPOSITION_DELAY = 16;
    public static final int MULTIWINDOW_TRANSACTION_FINISHED = 102;
    public static final int MULTIWINDOW_TRANSACTION_TIMEOUT = 101;
    public static final int NOTIFY_SPLIT_BAR_LAYOUT = 301;
    public static final int SPLIT_CLEAR_TEMP_COLOR_STATE = 205;
    public static final int SPLIT_MINI_LAUNCHER_ANIM_CHANGE_TIMEOUT = 206;
    static final String TAG = "VivoWmsImpl";
    public static final int UPDATA_GAME_MODE = 204;
    public static final int UPDATA_SYSTEMUI_COLOR = 201;
    public static final int UPDATA_SYSTEMUI_GESTURE_STYLE = 207;
    private static final int WINDOW_FOCUSED_STATE_CHANGED = 0;
    private static boolean sIsVosProduct = "vos".equals(FtBuild.getOsName());
    private final ArrayList<WeakReference<WindowState>> incomingFloatWindows;
    private AlertWindowNotificationController mAlertWindowNotificationController;
    private Session mAppShareHoldingScreenOn;
    private PowerManager.WakeLock mAppShareHoldingScreenWakeLock;
    private Context mContext;
    private final ArrayMap<IBinder, IBinder.DeathRecipient> mForceWpInVisibleTokens;
    private GraphicsConfigController mGraphicsConfigController;
    boolean mHideShowWallpaper;
    private final boolean mIsSupportIncomingFloat;
    VivoKeyguardOverlayController mKeyguardOverlayController;
    private VivoLayerRecorderManager mLayerRecorderManager;
    int mNavGestureMode;
    int mNavGestureStyle;
    boolean mPrivateKeyguardApplyed;
    private SettingsObserver mSettingsObserver;
    SnapshotWindow mSnapshotWindow;
    private VivoAppShareManager mVivoAppShareManager;
    private VivoEasyShareManager mVivoEasyShareManager;
    private VivoFreeformWindowManager mVivoFreeformWindowManager;
    private VivoFrozenPackageSupervisor mVivoFrozenPackageSupervisor;
    VivoWindowPolicyControllerImpl mVivoWindowPolicyController;
    private WindowManagerService mWms;
    String mStatusbarSettingColor = "#ffededed";
    String mNavigationSettingColor = "#fff5f5f5";
    private AbsVivoPerfManager mPerf = null;
    final RemoteCallbackList<ICarWindowObserver> mCarWindowObserver = new RemoteCallbackList<>();
    private WindowState mAppShareWindow = null;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.wm.VivoWmsImpl.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (((action.hashCode() == -438567294 && action.equals("com.vivo.daemonService.unifiedconfig.update_finish_broadcast_WindowPolicy")) ? (char) 0 : (char) 65535) == 0 && VivoWmsImpl.this.mVivoWindowPolicyController != null) {
                VivoWmsImpl.this.mVivoWindowPolicyController.postRetriveFile();
            }
        }
    };
    private boolean DEBUG_FETCH = false;
    WindowState mKeyguardWindow = null;
    private boolean mKeyguardWindowHidden = false;
    boolean mKeyguardTimeout = true;
    public Runnable mHideKeyguardTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.VivoWmsImpl.2
        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoWmsImpl.this.mWms.mGlobalLock) {
                Slog.d(VivoWmsImpl.TAG, "restore hideKeyguardByFingerprint");
                VivoWmsImpl.this.hideKeyguardLocked(false, VivoWmsImpl.this.mKeyguardTimeout);
                VivoWmsImpl.this.mWms.mWindowPlacerLocked.performSurfacePlacement();
                if (!VivoWmsImpl.this.mKeyguardTimeout) {
                    VivoWmsImpl.this.mKeyguardTimeout = true;
                }
            }
        }
    };
    int mFocusDisplay = 0;
    private Handler mHandler = new Handler() { // from class: com.android.server.wm.VivoWmsImpl.3
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 101) {
                if ((VivoWmsImpl.this.isVivoMultiWindowSupport() || VivoWmsImpl.this.isVivoFreeformFeatureSupport()) && VivoMultiWindowTransManager.getInstance() != null) {
                    VivoMultiWindowTransManager.getInstance().multiWindowTransAnimationTimeOut();
                }
            } else if (i == 102) {
                synchronized (VivoWmsImpl.this.mWms.mGlobalLock) {
                    if (VivoWmsImpl.this.mWms.mAtmService.isMultiWindowSupport() && VivoMultiWindowTransManager.getInstance() != null && VivoMultiWindowTransManager.getInstance().hasSetAnimation()) {
                        VivoMultiWindowTransManager.getInstance().closeMultiWindowTransLocked(VivoWmsImpl.this.mWms.getDefaultDisplayContentLocked());
                    }
                }
            } else if (i == 201) {
                VivoWmsImpl vivoWmsImpl = VivoWmsImpl.this;
                vivoWmsImpl.mStatusbarSettingColor = Settings.Secure.getStringForUser(vivoWmsImpl.mContext.getContentResolver(), "statusbar_color", -2);
                VivoWmsImpl vivoWmsImpl2 = VivoWmsImpl.this;
                vivoWmsImpl2.mNavigationSettingColor = Settings.Secure.getStringForUser(vivoWmsImpl2.mContext.getContentResolver(), "navigation_color", -2);
            } else if (i == 301) {
                synchronized (VivoWmsImpl.this.mWms.mGlobalLock) {
                    VivoWmsImpl.this.layoutSplitNavBar((IBinder) msg.obj);
                }
            } else {
                switch (i) {
                    case 104:
                        if (VivoWmsImpl.this.isVivoFreeformFeatureSupport()) {
                            VivoWmsImpl.this.setExitingFreeForm(false);
                            if (VivoWmsImpl.this.mWms.mDisplayFrozen) {
                                VivoWmsImpl.this.mWms.stopFreezingDisplayLocked();
                                return;
                            }
                            return;
                        }
                        return;
                    case 105:
                        DisplayContent topFocusDisplay = VivoWmsImpl.this.mWms.mRoot.getTopFocusedDisplayContent();
                        if (VivoWmsImpl.this.isVivoFreeFormValid() && !((Rect) msg.obj).isEmpty() && VivoWmsImpl.this.isInVivoFreeform() && !VivoWmsImpl.this.isScreenRotating(topFocusDisplay) && !VivoWmsImpl.this.isVivoFreeFormStackMax()) {
                            if (VivoWmsImpl.this.isDirectFreeformFinishingResizing()) {
                                VivoWmsImpl.this.setIsDirectFreeformFinishingResizing(false);
                            } else if (VivoWmsImpl.this.isStartingRecent()) {
                                VivoWmsImpl.this.setStartingRecent(false);
                                VivoWmsImpl.this.setStartingRecentBreakAdjust(true);
                                return;
                            } else {
                                if (VivoWmsImpl.this.isFreeFormResizing() && !VivoWmsImpl.this.isVivoFreeFormStackMax()) {
                                    VivoWmsImpl.this.setFreeformAdjustForImeWhenResize();
                                }
                                try {
                                    VSlog.d(VivoWmsImpl.TAG, "resize freeform task : " + msg.arg1);
                                    VivoWmsImpl.this.mWms.mActivityManager.resizeTask(msg.arg1, (Rect) msg.obj, 1);
                                } catch (RemoteException e) {
                                }
                                VSlog.d(VivoWmsImpl.TAG, "resize freeform task finish");
                            }
                        }
                        VivoWmsImpl.this.setStartingRecentBreakAdjust(false);
                        boolean move = msg.arg2 == 1;
                        if (move) {
                            if (VivoWmsImpl.this.getFreeformPosition() != null) {
                                VivoWmsImpl.this.getFreeformPosition().setEmpty();
                            }
                            VivoWmsImpl.this.setFreeformStackMove(false);
                            return;
                        }
                        VivoWmsImpl.this.setFreeformStackMove(true);
                        return;
                    case 106:
                        if (VivoWmsImpl.this.isVivoFreeFormValid() && VivoWmsImpl.this.isInVivoFreeform()) {
                            VivoWmsImpl.this.setEnteringFreeForm(false);
                            return;
                        }
                        return;
                    default:
                        switch (i) {
                            case VivoWmsImpl.UPDATA_GAME_MODE /* 204 */:
                                if (VivoWmsImpl.this.mIsSupportIncomingFloat) {
                                    boolean isGameMode = 1 == Settings.System.getIntForUser(VivoWmsImpl.this.mContext.getContentResolver(), VivoAIKeyExtend.GAME_DISTURB_ENABLED, 1, -2) && "1".equals(Settings.System.getStringForUser(VivoWmsImpl.this.mContext.getContentResolver(), "is_game_mode", -2));
                                    synchronized (VivoWmsImpl.this.mWms.mGlobalLock) {
                                        Iterator<WeakReference<WindowState>> it = VivoWmsImpl.this.incomingFloatWindows.iterator();
                                        WindowState curFocus = VivoWmsImpl.this.mWms.getCurrentFocusedWindowLocked();
                                        while (it.hasNext()) {
                                            WindowState w = it.next().get();
                                            if (w == null) {
                                                it.remove();
                                            } else {
                                                VSlog.v(VivoWmsImpl.TAG, "change state " + w + " " + isGameMode + " mCurrentFocus = " + curFocus);
                                                if (isGameMode && (curFocus == null || !w.mAttrs.packageName.equals(curFocus.mAttrs.packageName))) {
                                                    w.setForceHide(true);
                                                } else {
                                                    w.setForceHide(false);
                                                }
                                            }
                                        }
                                    }
                                    return;
                                }
                                return;
                            case VivoWmsImpl.SPLIT_CLEAR_TEMP_COLOR_STATE /* 205 */:
                                VivoWmsImpl.this.tempSetMiniNavColorState(true);
                                return;
                            case VivoWmsImpl.SPLIT_MINI_LAUNCHER_ANIM_CHANGE_TIMEOUT /* 206 */:
                                VivoWmsImpl.this.setMiniLauncherAnimChangeState(null, true);
                                return;
                            case VivoWmsImpl.UPDATA_SYSTEMUI_GESTURE_STYLE /* 207 */:
                                VivoWmsImpl vivoWmsImpl3 = VivoWmsImpl.this;
                                vivoWmsImpl3.mNavGestureStyle = Settings.Secure.getIntForUser(vivoWmsImpl3.mContext.getContentResolver(), "navigation_home_indicator_icon_style", 10, -2);
                                VivoWmsImpl vivoWmsImpl4 = VivoWmsImpl.this;
                                vivoWmsImpl4.mNavGestureMode = Settings.Secure.getIntForUser(vivoWmsImpl4.mContext.getContentResolver(), "navigation_gesture_mode", 0, -2);
                                return;
                            default:
                                return;
                        }
                }
            }
        }
    };
    private final RemoteCallbackList<IVivoWindowFocusListener> mVivoWindowFocusListeners = new RemoteCallbackList<>();
    boolean mVivoDockedDividerResizing = false;
    private final Object mLock = new Object();
    boolean mVivoFloatMessageFlag = false;
    String mVivoReortPendingSplitPackage = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    boolean mChangeMiniLauncherAnimState = true;
    String mNotifiyStartActivity = null;
    private final String LAUNCH_PACKAGE = "com.bbk.launcher2";
    boolean mVivoPauseRotationFlag = false;
    boolean mVivoHasPendingDockedBounds = false;
    boolean mVivoPauseRotationFlagPending = false;
    final int mMaxForceUpdateVivoPauseRotationFlagDuration = SystemProperties.getInt("persist.vivo.force_update_rotation_flag.timeout", 200);
    final Runnable mForceUpdateVivoPauseRotationFlagRunnable = new Runnable() { // from class: com.android.server.wm.VivoWmsImpl.4
        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoWmsImpl.this.mWms.mGlobalLock) {
                if (VivoWmsImpl.this.mVivoPauseRotationFlag) {
                    VSlog.e(VivoWmsImpl.TAG, "mForceUpdateVivoPauseRotationFlagRunnable:resumeRotationLocked,  mDeferredRotationPauseCount=" + VivoWmsImpl.this.mWms.getDefaultDisplayContentLocked().getDisplayRotation().getDeferredRotationPauseCount());
                    VivoWmsImpl.this.mVivoPauseRotationFlag = false;
                    VivoWmsImpl.this.mWms.getDefaultDisplayContentLocked().getDisplayRotation().resume();
                }
            }
        }
    };
    final int mMaxForceUpdateVivoPauseRotationFlagDurationAvoidErr = SystemProperties.getInt("persist.vivo.force_update_rotation_flag.timeout", (int) FrozenQuicker.FREEZE_STATUS_CHECK_MS);
    final Runnable mForceUpdateVivoPauseRotationFlagRunnableAvoidErr = new Runnable() { // from class: com.android.server.wm.VivoWmsImpl.5
        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoWmsImpl.this.mWms.mGlobalLock) {
                if (VivoWmsImpl.this.mVivoPauseRotationFlag) {
                    VSlog.e(VivoWmsImpl.TAG, "mForceUpdateVivoPauseRotationFlagRunnableAvoidErr:resumeRotationLocked,  mDeferredRotationPauseCount=" + VivoWmsImpl.this.mWms.getDefaultDisplayContentLocked().getDisplayRotation().getDeferredRotationPauseCount());
                    VivoWmsImpl.this.mVivoPauseRotationFlag = false;
                    VivoWmsImpl.this.mWms.getDefaultDisplayContentLocked().getDisplayRotation().resume();
                }
            }
        }
    };
    boolean mVivoMultiWindowRotationFlag = false;
    boolean bMiniNavColorState = true;
    IDisplayWindowRotationController mDisplayRotationControllerForMultiWindow = null;
    private final IBinder.DeathRecipient mDisplayRotationControllerDeathForMultiWindow = new IBinder.DeathRecipient() { // from class: com.android.server.wm.-$$Lambda$VivoWmsImpl$9x0xUFERgZcHvxjZS1_2GP8xG68
        @Override // android.os.IBinder.DeathRecipient
        public final void binderDied() {
            VivoWmsImpl.this.lambda$new$0$VivoWmsImpl();
        }
    };
    private long forceScreenTime = 0;
    private final int TIMEOUT_SWAP = 200;
    WindowState mTopRunningAppWinForSreenshot = null;
    boolean mHasFreeformWinCover = false;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        private final Uri mIsHomeIndicatorBlankUri;
        private final Uri mIsModeSideSlideUri;
        private final Uri mNavigationbarColorSettingUri;
        private final Uri mStatusbarColorSettingUri;

        public SettingsObserver() {
            super(new Handler());
            this.mStatusbarColorSettingUri = Settings.Secure.getUriFor("statusbar_color");
            this.mNavigationbarColorSettingUri = Settings.Secure.getUriFor("navigation_color");
            this.mIsHomeIndicatorBlankUri = Settings.Secure.getUriFor("navigation_home_indicator_icon_style");
            this.mIsModeSideSlideUri = Settings.Secure.getUriFor("navigation_gesture_mode");
            ContentResolver resolver = VivoWmsImpl.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.mStatusbarColorSettingUri, false, this, -1);
            resolver.registerContentObserver(this.mNavigationbarColorSettingUri, false, this, -1);
            resolver.registerContentObserver(this.mIsHomeIndicatorBlankUri, false, this, -1);
            resolver.registerContentObserver(this.mIsModeSideSlideUri, false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (uri == null) {
                return;
            }
            if (this.mStatusbarColorSettingUri.equals(uri) || this.mNavigationbarColorSettingUri.equals(uri)) {
                Message msg = VivoWmsImpl.this.mHandler.obtainMessage(201);
                VivoWmsImpl.this.mHandler.sendMessage(msg);
            } else if (this.mIsHomeIndicatorBlankUri.equals(uri) || this.mIsModeSideSlideUri.equals(uri)) {
                Message msg2 = VivoWmsImpl.this.mHandler.obtainMessage(VivoWmsImpl.UPDATA_SYSTEMUI_GESTURE_STYLE);
                VivoWmsImpl.this.mHandler.sendMessage(msg2);
            }
        }
    }

    private void init(Context context) {
        this.mStatusbarSettingColor = Settings.Secure.getStringForUser(context.getContentResolver(), "statusbar_color", -2);
        this.mNavigationSettingColor = Settings.Secure.getStringForUser(context.getContentResolver(), "navigation_color", -2);
        this.mNavGestureStyle = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "navigation_home_indicator_icon_style", 10, -2);
        this.mNavGestureMode = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "navigation_gesture_mode", 0, -2);
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.vivo.daemonService.unifiedconfig.update_finish_broadcast_WindowPolicy");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        this.mSettingsObserver = new SettingsObserver();
        VivoWindowPolicyControllerImpl vivoWindowPolicyControllerImpl = VivoWindowPolicyControllerImpl.getInstance(context);
        this.mVivoWindowPolicyController = vivoWindowPolicyControllerImpl;
        if (vivoWindowPolicyControllerImpl != null) {
            vivoWindowPolicyControllerImpl.init();
        }
        SnapshotWindow snapshotWindow = SnapshotWindow.getInstance(this.mContext);
        this.mSnapshotWindow = snapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.setWindowManagerService(this.mWms);
        }
        RMWms.getInstance().initialize(this);
        this.mKeyguardOverlayController = new VivoKeyguardOverlayController(context);
        this.mLayerRecorderManager = new VivoLayerRecorderManager(this.mContext);
    }

    public void onUserSwitched() {
        this.mStatusbarSettingColor = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "statusbar_color", -2);
        this.mNavigationSettingColor = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "navigation_color", -2);
        this.mNavGestureStyle = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "navigation_home_indicator_icon_style", 10, -2);
        this.mNavGestureMode = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "navigation_gesture_mode", 0, -2);
    }

    public VivoWmsImpl(WindowManagerService wms) {
        this.mVivoAppShareManager = null;
        boolean z = false;
        if (FtFeature.isFeatureSupport("vivo.software.tpincomingfloat") && !"vos".equals(FtBuild.getOsName())) {
            z = true;
        }
        this.mIsSupportIncomingFloat = z;
        this.incomingFloatWindows = new ArrayList<>();
        this.mForceWpInVisibleTokens = new ArrayMap<>();
        if (wms == null) {
            Slog.i(TAG, "container is " + wms);
        }
        Context context = wms.mContext;
        this.mContext = context;
        this.mWms = wms;
        init(context);
        VivoMultiWindowTransManager.getInstance().setWindowManagerService(wms);
        this.mKeyguardOverlayController = new VivoKeyguardOverlayController(this.mContext);
        VivoFreeformWindowManager vivoFreeformWindowManager = VivoFreeformWindowManager.getInstance();
        this.mVivoFreeformWindowManager = vivoFreeformWindowManager;
        if (vivoFreeformWindowManager != null) {
            vivoFreeformWindowManager.init(wms);
        }
        this.mVivoEasyShareManager = VivoEasyShareManager.getInstance();
        this.mVivoFrozenPackageSupervisor = VivoFrozenPackageSupervisor.getInstance();
        this.mAlertWindowNotificationController = new AlertWindowNotificationController(this.mContext);
        this.mGraphicsConfigController = new GraphicsConfigController(this.mContext);
        VivoAppShareManager vivoAppShareManager = VivoAppShareManager.getInstance();
        this.mVivoAppShareManager = vivoAppShareManager;
        if (vivoAppShareManager != null) {
            vivoAppShareManager.initWms(this.mWms);
        }
    }

    public boolean allowOverlayKeyguard(String packageName) {
        VivoKeyguardOverlayController vivoKeyguardOverlayController = this.mKeyguardOverlayController;
        if (vivoKeyguardOverlayController == null) {
            return true;
        }
        return vivoKeyguardOverlayController.allowOverlayKeyguard(packageName);
    }

    public boolean allowOverlayKeyguard(String packageName, String componentName, int callerUid) {
        VivoKeyguardOverlayController vivoKeyguardOverlayController = this.mKeyguardOverlayController;
        if (vivoKeyguardOverlayController == null) {
            return true;
        }
        return vivoKeyguardOverlayController.allowOverlayKeyguard(packageName, componentName, callerUid);
    }

    public boolean allowDisableKeyguard(int callingUid, String tag, IBinder token) {
        VSlog.d(TAG, "callingUid = " + callingUid + ", tag = " + tag + ", token = " + token);
        StringBuilder sb = new StringBuilder();
        sb.append("disableKeyguard Callers : ");
        sb.append(Debug.getCallers(10));
        VSlog.d(TAG, sb.toString());
        if (callingUid >= 10000 && callingUid <= 19999) {
            String[] pkgName = this.mContext.getPackageManager().getPackagesForUid(callingUid);
            VSlog.d(TAG, "pkgName = " + pkgName);
            if (pkgName != null && pkgName.length > 0) {
                synchronized (this.mWms.mGlobalLock) {
                    if (!allowOverlayKeyguard(pkgName[0])) {
                        VSlog.i(TAG, "DEBUG_KOC:Forbid overlay keyguard: package = " + pkgName[0]);
                        return false;
                    }
                    return true;
                }
            }
            return true;
        }
        return true;
    }

    public void dummy() {
        Slog.i(TAG, "dummy, this=" + this);
    }

    public String getNavigationBarPolicy(IWindow window) {
        String res;
        String navColorPolicy = getNavigationBarPolicyInner(window);
        synchronized (this.mWms.mGlobalLock) {
            String paddingColorPolicy = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            String navFixColor = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            WindowState win = this.mWms.windowForClientLocked((Session) null, window, false);
            if (win != null) {
                String title = win.getWindowTag().toString();
                String navFixColorfetchString = "WindowPolicy-FIXCOLOR," + win.mAttrs.packageName + "," + title;
                navFixColor = fetchSystemSetting(navFixColorfetchString);
                String paddingColorfetchString = "WindowPolicy-PADDINGCOLOR," + win.mAttrs.packageName + "," + title;
                paddingColorPolicy = fetchSystemSetting(paddingColorfetchString);
            }
            res = navColorPolicy + "," + navFixColor + "," + paddingColorPolicy;
            if (WindowManagerDebugConfig.DEBUG) {
                VSlog.i(TAG, "getNavigationBarPolicy, win = " + win + ", result = " + res);
            }
        }
        return res;
    }

    private String getNavigationBarPolicyInner(IWindow window) {
        WindowState mainWindow;
        synchronized (this.mWms.mGlobalLock) {
            WindowState win = this.mWms.windowForClientLocked((Session) null, window, false);
            if (win != null && NavigationBarPolicy.hasNavigationBarWindow(win, win.mAttrs)) {
                if (win.mAttrs.type == 2012) {
                    return "#00000000";
                }
                if (NavigationBarPolicy.forceImmersive(win.mAttrs)) {
                    return "NONE";
                }
                if (win.mActivityRecord != null && (mainWindow = findMainWindow(win.mActivityRecord)) != null) {
                    win = mainWindow;
                }
                String title = win.getWindowTag().toString();
                String navColorfetchString = "WindowPolicy-NAVCOLOR," + win.mAttrs.packageName + "," + title;
                String navColorPolicy = fetchSystemSetting(navColorfetchString);
                if (TextUtils.isEmpty(navColorPolicy)) {
                    try {
                        ApplicationInfo appInfo = this.mWms.mContext.getPackageManager().getApplicationInfoAsUser(win.mAttrs.packageName, 0, UserHandle.getUserId(Binder.getCallingUid()));
                        if ((appInfo.flags & 1) != 0) {
                            return "IMMERSE";
                        }
                    } catch (Exception e) {
                    }
                }
                return navColorPolicy;
            }
            if (WindowManagerDebugConfig.DEBUG) {
                VSlog.i(TAG, "getNavigationBarPolicy, win = " + win);
            }
            return "NONE";
        }
    }

    private WindowState findMainWindow(ActivityRecord activityRecord) {
        int j = activityRecord.mChildren.size();
        while (j > 0) {
            j--;
            WindowState win = (WindowState) activityRecord.mChildren.get(j);
            if (win.mAttrs.type == 1) {
                return win;
            }
        }
        return null;
    }

    public void applyNavColorForWindow(IWindow window, int color) {
        synchronized (this.mWms.mGlobalLock) {
            WindowState win = this.mWms.windowForClientLocked((Session) null, window, false);
            if (win == null) {
                VSlog.w(TAG, "applyNavColorForWindow, win = " + win);
                return;
            }
            win.applyNavColor(color);
            this.mWms.mWindowPlacerLocked.requestTraversal();
        }
    }

    public boolean isAspectRestricted(IWindow window) {
        boolean z;
        synchronized (this.mWms.mGlobalLock) {
            z = false;
            WindowState win = this.mWms.windowForClientLocked((Session) null, window, false);
            if (win != null) {
                z = win.isAspectRestricted();
            }
        }
        return z;
    }

    /* JADX WARN: Removed duplicated region for block: B:110:0x01f8  */
    /* JADX WARN: Removed duplicated region for block: B:125:0x0252 A[Catch: Exception -> 0x0289, TryCatch #4 {Exception -> 0x0289, blocks: (B:108:0x01f4, B:111:0x01fa, B:113:0x0200, B:115:0x020a, B:118:0x0225, B:120:0x022b, B:122:0x0236, B:123:0x024e, B:125:0x0252, B:128:0x0279, B:132:0x0282), top: B:291:0x01f4 }] */
    /* JADX WARN: Removed duplicated region for block: B:126:0x0275  */
    /* JADX WARN: Removed duplicated region for block: B:140:0x0293  */
    /* JADX WARN: Removed duplicated region for block: B:144:0x029f  */
    /* JADX WARN: Removed duplicated region for block: B:157:0x02d5  */
    /* JADX WARN: Removed duplicated region for block: B:172:0x032f A[Catch: Exception -> 0x0346, TRY_LEAVE, TryCatch #10 {Exception -> 0x0346, blocks: (B:155:0x02d1, B:158:0x02d7, B:160:0x02dd, B:162:0x02e7, B:165:0x0302, B:167:0x0308, B:169:0x0313, B:170:0x032b, B:172:0x032f), top: B:303:0x02d1 }] */
    /* JADX WARN: Removed duplicated region for block: B:175:0x0344  */
    /* JADX WARN: Removed duplicated region for block: B:183:0x0350  */
    /* JADX WARN: Removed duplicated region for block: B:187:0x035c  */
    /* JADX WARN: Removed duplicated region for block: B:200:0x0392  */
    /* JADX WARN: Removed duplicated region for block: B:215:0x03ec A[Catch: Exception -> 0x0403, TRY_LEAVE, TryCatch #13 {Exception -> 0x0403, blocks: (B:198:0x038e, B:201:0x0394, B:203:0x039a, B:205:0x03a4, B:208:0x03bf, B:210:0x03c5, B:212:0x03d0, B:213:0x03e8, B:215:0x03ec), top: B:309:0x038e }] */
    /* JADX WARN: Removed duplicated region for block: B:218:0x0401  */
    /* JADX WARN: Removed duplicated region for block: B:226:0x040d  */
    /* JADX WARN: Removed duplicated region for block: B:230:0x0419  */
    /* JADX WARN: Removed duplicated region for block: B:234:0x0426 A[Catch: Exception -> 0x04b7, TryCatch #5 {Exception -> 0x04b7, blocks: (B:232:0x0422, B:234:0x0426, B:235:0x043b, B:238:0x0448, B:240:0x044e, B:242:0x0458, B:245:0x0473, B:247:0x0479, B:249:0x0484, B:250:0x049c, B:252:0x04a0), top: B:293:0x0422 }] */
    /* JADX WARN: Removed duplicated region for block: B:237:0x0446  */
    /* JADX WARN: Removed duplicated region for block: B:252:0x04a0 A[Catch: Exception -> 0x04b7, TRY_LEAVE, TryCatch #5 {Exception -> 0x04b7, blocks: (B:232:0x0422, B:234:0x0426, B:235:0x043b, B:238:0x0448, B:240:0x044e, B:242:0x0458, B:245:0x0473, B:247:0x0479, B:249:0x0484, B:250:0x049c, B:252:0x04a0), top: B:293:0x0422 }] */
    /* JADX WARN: Removed duplicated region for block: B:255:0x04b5  */
    /* JADX WARN: Removed duplicated region for block: B:261:0x04c3  */
    /* JADX WARN: Removed duplicated region for block: B:263:0x04c6  */
    /* JADX WARN: Removed duplicated region for block: B:297:0x0369 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:301:0x0112 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:307:0x01cf A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:311:0x02ac A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:316:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:318:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:319:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:320:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:50:0x00f9  */
    /* JADX WARN: Removed duplicated region for block: B:54:0x0105  */
    /* JADX WARN: Removed duplicated region for block: B:67:0x013b  */
    /* JADX WARN: Removed duplicated region for block: B:82:0x0195 A[Catch: Exception -> 0x01ac, TRY_LEAVE, TryCatch #2 {Exception -> 0x01ac, blocks: (B:65:0x0137, B:68:0x013d, B:70:0x0143, B:72:0x014d, B:75:0x0168, B:77:0x016e, B:79:0x0179, B:80:0x0191, B:82:0x0195), top: B:287:0x0137 }] */
    /* JADX WARN: Removed duplicated region for block: B:85:0x01aa  */
    /* JADX WARN: Removed duplicated region for block: B:93:0x01b6  */
    /* JADX WARN: Removed duplicated region for block: B:97:0x01c2  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public java.lang.String fetchSystemSetting(java.lang.String r19) {
        /*
            Method dump skipped, instructions count: 1300
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoWmsImpl.fetchSystemSetting(java.lang.String):java.lang.String");
    }

    private boolean isHomeIndicatorOn() {
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys", "1");
        if (WindowManagerDebugConfig.DEBUG) {
            VSlog.d(TAG, "DEBUG_ALIENSCREEN:isHomeIndicatorOn=" + navBarOverride);
        }
        return !"0".equals(navBarOverride);
    }

    public boolean shouldAnimAffectWin(WindowState win) {
        DisplayContent dc;
        boolean shouldAnimAffectWin = true;
        ActivityRecord animatingContainer = win.getAnimatingContainer(3, -1);
        if (animatingContainer == null || !(animatingContainer instanceof ActivityRecord)) {
            return true;
        }
        ActivityRecord animAR = animatingContainer;
        if (animAR.isSelfAnimating(0, -1) || (dc = animAR.getDisplayContent()) == null) {
            return true;
        }
        if (dc.mOpeningApps.contains(animAR) && !animAR.shouldApplyAnimation(true)) {
            VSlog.d(TAG, "No shouldAnimAffectWin, mOpeningApps for " + win);
            shouldAnimAffectWin = false;
        }
        if (dc.mClosingApps.contains(animAR) && !animAR.shouldApplyAnimation(false)) {
            VSlog.d(TAG, "No shouldAnimAffectWin, mClosingApps for " + win);
            return false;
        }
        return shouldAnimAffectWin;
    }

    public void hideKeyguardByFingerprint(int hide) {
        Slog.i(TAG, "hideKeyguardByFingerprint waiting WindowMap lock...");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideKeyguardLocked(boolean hide, boolean addWallpaper) {
        if (this.mKeyguardWindowHidden == hide || this.mKeyguardWindow == null) {
            return;
        }
        System.nanoTime();
        this.mWms.mH.removeCallbacks(this.mHideKeyguardTimeoutRunnable);
        if (hide) {
            if (!this.mPrivateKeyguardApplyed) {
                this.mWms.mH.postDelayed(this.mHideKeyguardTimeoutRunnable, 1000L);
            } else {
                this.mWms.mH.postDelayed(this.mHideKeyguardTimeoutRunnable, 2000L);
            }
        }
        Slog.i(TAG, "hideKeyguardByFingerprint, hide = " + hide + ", childCount = " + this.mKeyguardWindow.mChildren.size());
        this.mWms.mPolicy.setHideByFingerPrint(hide);
        this.mKeyguardWindow.hideByFingerPrint(hide);
        this.mWms.getDefaultDisplayContentLocked().performLayout(true, false);
        if (hide && (this.mKeyguardWindow.mAttrs.flags & 1048576) != 0) {
            this.mHideShowWallpaper = true;
            this.mKeyguardWindow.mAttrs.flags &= -1048577;
            this.mWms.getDefaultDisplayContentLocked().pendingLayoutChanges |= 4;
            this.mWms.mWindowPlacerLocked.performSurfacePlacement();
        } else if (!hide && addWallpaper && this.mHideShowWallpaper) {
            WindowManager.LayoutParams layoutParams = this.mKeyguardWindow.mAttrs;
            layoutParams.flags = 1048576 | layoutParams.flags;
            this.mWms.getDefaultDisplayContentLocked().pendingLayoutChanges |= 4;
            Slog.w(TAG, "Finger verify fail and show wallpaper.");
            this.mWms.mWindowPlacerLocked.performSurfacePlacement();
        }
        if (!hide && this.mHideShowWallpaper) {
            this.mHideShowWallpaper = false;
        }
        int childCount = this.mKeyguardWindow.mChildren.size();
        for (int i = 0; i < childCount; i++) {
            WindowState child = (WindowState) this.mKeyguardWindow.mChildren.get(i);
            child.hideByFingerPrint(hide);
        }
        this.mKeyguardWindowHidden = hide;
    }

    private void updateSurfaceSyncLocked(long time) {
        this.mWms.mAnimator.animate(time);
        if (this.mKeyguardWindowHidden && this.mWms.mPolicy.isScreenOn()) {
            Slog.i(TAG, "hideKeyguardByFingerprint, doCompositionSync");
            try {
                Thread.sleep(16L);
            } catch (Exception e) {
            }
        }
    }

    public void trySetKeyguardWindow(WindowState win) {
        if (this.mWms.mPolicy.isKeyguardHostWindow(win.mAttrs)) {
            this.mKeyguardWindow = win;
            this.mPrivateKeyguardApplyed = win.mAttrs.isFullscreen();
        }
    }

    public void checkPrivateKeyguardApplyed(WindowState win) {
        if (this.mWms.mPolicy.isKeyguardHostWindow(win.mAttrs)) {
            DisplayContent defaultDisplay = this.mWms.getDefaultDisplayContentLocked();
            DisplayInfo defaultInfo = defaultDisplay.getDisplayInfo();
            int defaultDw = defaultInfo.logicalWidth;
            int defaultDh = defaultInfo.logicalHeight;
            this.mPrivateKeyguardApplyed = win.mAttrs.isFullscreen() && (win.mAttrs.height == -1 || win.mAttrs.height == Math.max(defaultDw, defaultDh));
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.i(TAG, "Relayout keyguard window, mPrivateKeyguardApplyed = " + this.mPrivateKeyguardApplyed + ", attrs.height = " + win.mAttrs.height + ", hasSurface = " + win.mHasSurface + ", has showwallpaper flag = " + (win.mAttrs.flags & 1048576) + ", mHideShowWallpaper = " + this.mHideShowWallpaper);
            }
            if (this.mHideShowWallpaper && (win.mAttrs.flags & 1048576) != 0) {
                this.mKeyguardTimeout = false;
                this.mWms.mH.removeCallbacks(this.mHideKeyguardTimeoutRunnable);
                hideKeyguardLocked(false, this.mKeyguardTimeout);
            }
        }
    }

    public void resizeForHideKeyguard(WindowState win) {
        if (win.mAttrs.isFullscreen() && this.mPrivateKeyguardApplyed && win.mWinAnimator.mSurfaceResized) {
            Slog.i(TAG, "force DRAW_PENDING for keyguard, w = " + this);
            win.mWinAnimator.mDrawState = 1;
        }
        if (this.mWms.mPolicy.isKeyguardHostWindow(win.mAttrs) && !this.mPrivateKeyguardApplyed && win.mWinAnimator.mSurfaceResized) {
            this.mKeyguardTimeout = false;
            this.mWms.mH.removeCallbacks(this.mHideKeyguardTimeoutRunnable);
            this.mWms.mH.post(this.mHideKeyguardTimeoutRunnable);
        }
    }

    public void startFreezingSplitWindowDirect(int during, String freezingReason) {
    }

    public int getPreferredModeId(WindowState w) {
        int preferredModeId;
        synchronized (this.mWms.mGlobalLock) {
            preferredModeId = w.getDisplayContent().getDisplayPolicy().getRefreshRatePolicy().getPreferredModeId(w);
        }
        return preferredModeId;
    }

    public void registerAppTransitionListener(WindowManagerInternal.AppTransitionListener listener) {
        synchronized (this.mWms.mGlobalLock) {
            this.mWms.getDefaultDisplayContentLocked().mAppTransition.registerListenerLocked(listener);
        }
    }

    public int getRotation(WindowState w) {
        DisplayContent dc = w.getDisplayContent();
        if (dc == null) {
            return 0;
        }
        return dc.getRotation();
    }

    public WindowManager.LayoutParams getAttrs(WindowState w) {
        return w.mAttrs;
    }

    public int getOwnerPid(WindowState w) {
        return w.mSession.mPid;
    }

    public int getWidth(WindowState w) {
        WindowSurfaceController controller = w.mWinAnimator.mSurfaceController;
        return controller != null ? controller.getWidth() : w.mRequestedWidth;
    }

    public int getHeight(WindowState w) {
        WindowSurfaceController controller = w.mWinAnimator.mSurfaceController;
        return controller != null ? controller.getHeight() : w.mRequestedWidth;
    }

    public boolean isAnimating(WindowState w) {
        boolean isAnimating;
        synchronized (this.mWms.mGlobalLock) {
            isAnimating = w.isAnimating(3, -1);
        }
        return isAnimating;
    }

    public int getLayer(WindowState w) {
        return w.mBaseLayer + w.mSubLayer;
    }

    public boolean isVivoMultiWindowSupport() {
        if (this.mWms.mAtmService != null) {
            return this.mWms.mAtmService.isVivoMultiWindowSupport();
        }
        VSlog.e(TAG, "vivo_multiwindow support judge too early");
        return false;
    }

    public boolean isSplitLogDebug() {
        return VivoMultiWindowConfig.DEBUG;
    }

    public boolean isSupportRotateFree() {
        return VivoMultiWindowConfig.IS_VIVO_ROTATE_FREE;
    }

    public boolean isInVivoMultiWindowIgnoreVisibility() {
        return this.mWms.mRoot.getDefaultTaskDisplayArea().isSplitScreenModeActivated();
    }

    public boolean isInVivoMultiWindowConsiderVisibility() {
        return this.mWms.mRoot.getDefaultTaskDisplayArea().isSplitScreenModeActivated();
    }

    public boolean isInVivoMultiWindowIgnoreVisibilityFocusedDisplay() {
        if (this.mFocusDisplay != 0) {
            return false;
        }
        return isInVivoMultiWindowIgnoreVisibility();
    }

    public boolean isInVivoMultiWindowConsiderVisibilityFocusedDisplay() {
        if (this.mFocusDisplay != 0) {
            return false;
        }
        return isInVivoMultiWindowConsiderVisibility();
    }

    public void notifyVivoWindowFocusChanged() {
        if ((!isVivoMultiWindowSupport() && !this.mWms.mAtmService.isVivoOverVos2MultiWindowSupport()) || this.mFocusDisplay != 0) {
            return;
        }
        WindowState mCurrentFocus = this.mWms.getCurrentFocusedWindowLocked();
        ActivityStack stack = mCurrentFocus != null ? mCurrentFocus.getRootTask() : null;
        if (stack == null) {
            return;
        }
        int focusTaskId = stack.mTaskId;
        synchronized (this.mLock) {
            int size = this.mVivoWindowFocusListeners.beginBroadcast();
            for (int i = 0; i < size; i++) {
                IVivoWindowFocusListener listener = this.mVivoWindowFocusListeners.getBroadcastItem(i);
                try {
                    listener.onWindowFocusChanged(focusTaskId);
                } catch (RemoteException e) {
                    VSlog.e(TAG, "Error delivering divider visibility changed event.", e);
                }
            }
            this.mVivoWindowFocusListeners.finishBroadcast();
        }
    }

    public void registerVivoWindowFocusListener(IVivoWindowFocusListener listener) {
        if (!this.mWms.checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerWindowFocusListener()")) {
            return;
        }
        synchronized (this.mWms.mGlobalLock) {
            this.mVivoWindowFocusListeners.register(listener);
            notifyVivoWindowFocusChanged();
        }
    }

    public void unregisterVivoWindowFocusListener(IVivoWindowFocusListener listener) {
        if (!this.mWms.checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "unregisterVivoWindowFocusListener()")) {
            return;
        }
        synchronized (this.mWms.mGlobalLock) {
            this.mVivoWindowFocusListeners.unregister(listener);
        }
    }

    public int getVivoFocusTaskId() {
        WindowState mCurrentFocus = this.mWms.getCurrentFocusedWindowLocked();
        ActivityStack stack = mCurrentFocus != null ? mCurrentFocus.getRootTask() : null;
        if (stack == null) {
            return -1;
        }
        return stack.mTaskId;
    }

    public void setVivoDockedDividerResizing(boolean resizing) {
        VSlog.e(TAG, "setResizing:" + resizing);
        this.mVivoDockedDividerResizing = resizing;
        if (isVivoMultiWindowSupport()) {
            SystemProperties.set("sys.vivo.multiwindow.divider_resizing", resizing ? "true" : "false");
        }
    }

    public boolean isVivoDockedDividerResizing() {
        return this.mVivoDockedDividerResizing && this.mFocusDisplay == 0;
    }

    public void setVivoFloatMessageFlag(boolean enter) {
        this.mVivoFloatMessageFlag = enter;
    }

    public boolean getVivoFloatMessageFlag() {
        return this.mVivoFloatMessageFlag;
    }

    public void setVivoPendingPackageInSplit(String packageName) {
        this.mVivoReortPendingSplitPackage = packageName;
    }

    public String getVivoPendingPackageInSplit() {
        return this.mVivoReortPendingSplitPackage;
    }

    public void disableMiniLauncherAnimChange(String aName, int timeout) {
        if (!VivoMultiWindowConfig.IS_VIVO_SPLIT_SWITCH_ANIM || !isVivoMultiWindowSupport()) {
            return;
        }
        setMiniLauncherAnimChangeState(aName, false);
        this.mHandler.sendEmptyMessageDelayed(SPLIT_MINI_LAUNCHER_ANIM_CHANGE_TIMEOUT, timeout);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setMiniLauncherAnimChangeState(String aName, boolean state) {
        if (!isVivoMultiWindowSupport()) {
            return;
        }
        synchronized (this.mWms.mGlobalLock) {
            this.mChangeMiniLauncherAnimState = state;
            this.mNotifiyStartActivity = aName;
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.i(TAG, "vivo_multiwindow_fmk aName" + aName + ", can change minianim: " + state);
            }
        }
    }

    private void notifyActivityDrawnForMultiWindow(IBinder obj) {
        ActivityRecord ar = this.mWms.mRoot.isInAnyStack(obj);
        String str = this.mNotifiyStartActivity;
        if (str != null && ar != null && !this.mChangeMiniLauncherAnimState && str.equals(ar.packageName)) {
            this.mHandler.removeMessages(SPLIT_MINI_LAUNCHER_ANIM_CHANGE_TIMEOUT);
            setMiniLauncherAnimChangeState(null, true);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:109:? A[ADDED_TO_REGION, RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:97:0x01d7 A[ADDED_TO_REGION] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public android.view.animation.Animation changeAnimationForSplitIfNeed(com.android.server.wm.WindowContainer r16, android.view.animation.Animation r17, int r18, boolean r19, android.graphics.Rect r20, android.graphics.Rect r21) {
        /*
            Method dump skipped, instructions count: 520
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoWmsImpl.changeAnimationForSplitIfNeed(com.android.server.wm.WindowContainer, android.view.animation.Animation, int, boolean, android.graphics.Rect, android.graphics.Rect):android.view.animation.Animation");
    }

    private Animation getAlphaAnimation(Animation a, boolean enter, Rect frame, Rect displayFrame, long mDuration) {
        if (frame == null) {
            return null;
        }
        Animation mAnimation = enter ? new AlphaAnimation(1.0f, 1.0f) : new AlphaAnimation(0.5f, 0.0f);
        mAnimation.setDuration(mDuration);
        return mAnimation;
    }

    private Animation getAlphaAnimation(boolean enter, Rect frame, long mDuration) {
        if (frame == null) {
            return null;
        }
        Animation mAnimation = enter ? new AlphaAnimation(1.0f, 1.0f) : new AlphaAnimation(0.25f, 0.0f);
        mAnimation.setDuration(mDuration);
        return mAnimation;
    }

    public void hideSplitNarBarIfNeeded() {
        if (isVivoMultiWindowSupport() && !isInVivoMultiWindowConsiderVisibilityFocusedDisplay() && VivoMultiWindowConfig.IS_VIVO_SPLIT_NAV_COLOR) {
            DisplayContent dc = this.mWms.getDefaultDisplayContentLocked();
            if (dc != null) {
                dc.hideSplitScreenNavBar();
            }
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.d(TAG, "vivo_multiwindow_fmk hideSplitNarBarIfNeeded timeout");
            }
        }
    }

    private void notifyHideSplitNarBarIfNeeded(IBinder _obj) {
        WindowState mMain;
        if (isVivoMultiWindowSupport() && !isInVivoMultiWindowConsiderVisibilityFocusedDisplay() && VivoMultiWindowConfig.IS_VIVO_SPLIT_NAV_COLOR && _obj != null) {
            ActivityRecord ar = this.mWms.mRoot.isInAnyStack(_obj);
            DisplayContent dc = ar != null ? ar.getDisplay() : null;
            if (ar != null && dc != null && dc.isVivoMultiWindowExitedJustWithDisplay() && (mMain = ar.findMainWindow()) != null && dc.mCurrentFocus == mMain) {
                this.mHandler.removeMessages(NOTIFY_SPLIT_BAR_LAYOUT);
                Message msg = this.mHandler.obtainMessage(NOTIFY_SPLIT_BAR_LAYOUT);
                msg.obj = _obj;
                this.mHandler.sendMessageDelayed(msg, 250L);
                if (VivoMultiWindowConfig.DEBUG) {
                    VSlog.d(TAG, " draw of notifyHideSplitNarBarIfNeeded post layout split nar bar");
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void layoutSplitNavBar(IBinder obj) {
        ActivityRecord ar;
        if (isVivoMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_NAV_COLOR && obj != null && !isInVivoMultiWindowConsiderVisibilityFocusedDisplay() && (ar = this.mWms.mRoot.isInAnyStack(obj)) != null && ar.isVisible()) {
            ar.vivoLayoutSplitNav();
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.d(TAG, "layoutSplitNavBar again of " + ar);
            }
        }
    }

    public void setDeferMinimizeRotation(boolean isDefer) {
        synchronized (this.mWms.mGlobalLock) {
        }
    }

    public boolean isDeferRotationMinimizeChange() {
        return false;
    }

    public boolean isMinimizedDock() {
        DisplayContent dc = this.mWms.getDefaultDisplayContentLocked();
        return (dc == null || dc.getDockedDividerController() == null || !dc.getDockedDividerController().isMinimizedDock()) ? false : true;
    }

    public void checkSplitScreenMinimizedChanged(boolean animate) {
        synchronized (this.mWms.mGlobalLock) {
            DisplayContent displayContent = this.mWms.getDefaultDisplayContentLocked();
            if (displayContent != null) {
                displayContent.getDockedDividerController().checkMinimizeChanged(animate);
            }
        }
    }

    public int freezeRotationWhenSplit(int rotation, String reason) {
        if (!VivoMultiWindowConfig.IS_VIVO_ROTATE_SUGGESTION || !"default".equals(reason)) {
            return rotation;
        }
        int rt = this.mWms.getDefaultDisplayRotation();
        VSlog.d(TAG, "freezeRotation reason:default, Rotation:" + rt);
        return rt;
    }

    public void sendProposedRotationChangeToDockedDivider(int rotation, boolean isValid) {
        synchronized (this.mWms.mGlobalLock) {
            this.mWms.getDefaultDisplayContentLocked().getDockedDividerController().notifyVivoProposedRotationChange(rotation, isValid);
        }
    }

    public void registerVivoProposedRotationChangeListener(IVivoProposedRotationChangeListener listener) {
        if (!this.mWms.checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerVivoProposedRotationChangeListener()")) {
            return;
        }
        synchronized (this.mWms.mGlobalLock) {
            this.mWms.getDefaultDisplayContentLocked().mDividerControllerLocked.registerVivoProposedRotationChangeListener(listener);
        }
    }

    public void unregisterVivoProposedRotationChangeListener(IVivoProposedRotationChangeListener listener) {
        if (!this.mWms.checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "unregisterVivoProposedRotationChangeListener()")) {
            return;
        }
        synchronized (this.mWms.mGlobalLock) {
            this.mWms.getDefaultDisplayContentLocked().mDividerControllerLocked.unregisterVivoProposedRotationChangeListener(listener);
        }
    }

    public void notifyActivityDrawn(Message msg) {
        synchronized (this.mWms.mGlobalLock) {
            notifyActivityDrawnMultiWindow(msg);
            if (VivoMultiWindowConfig.IS_VIVO_SPLIT_SWITCH_ANIM && isVivoMultiWindowSupport() && !this.mChangeMiniLauncherAnimState) {
                notifyActivityDrawnForMultiWindow((IBinder) msg.obj);
            }
            notifyHideSplitNarBarIfNeeded((IBinder) msg.obj);
        }
    }

    public void vivoRemoveMultiWindowAnimTimeOut() {
        this.mHandler.removeMessages(101);
    }

    public void vivoResendMultiWindowAnimTimeOut(long timeOut) {
        this.mHandler.removeMessages(101);
        this.mHandler.sendEmptyMessageDelayed(101, timeOut);
    }

    private void notifyActivityDrawnMultiWindow(Message msg) {
        if (this.mWms.mAtmService.isMultiWindowSupport() && VivoMultiWindowTransManager.getInstance() != null && VivoMultiWindowTransManager.getInstance().hasSetAnimation() && msg != null && msg.obj != null) {
            VivoMultiWindowTransManager.getInstance().notifyAppTokenDrawnForMultiWindow((IBinder) msg.obj);
        }
    }

    public boolean hasSetMultiWindowAnimation() {
        return VivoMultiWindowTransManager.getInstance().hasSetAnimation();
    }

    public void setVivoPauseRotationFlag(boolean pause) {
        if (this.mWms.mAtmService.isMultiWindowSupport()) {
            VSlog.e(TAG, "setVivoPauseRotationFlag:pause=" + pause + " mVivoPauseRotationFlag=" + this.mVivoPauseRotationFlag + " mDeferredRotationPauseCount=" + this.mWms.getDefaultDisplayContentLocked().getDisplayRotation().getDeferredRotationPauseCount());
            if (pause) {
                if (this.mVivoPauseRotationFlag) {
                    VSlog.e(TAG, "setVivoPauseRotationFlag:mVivoPauseRotationFlag already true");
                    this.mVivoPauseRotationFlagPending = false;
                    this.mVivoHasPendingDockedBounds = false;
                    return;
                }
                this.mVivoPauseRotationFlagPending = false;
                this.mVivoPauseRotationFlag = true;
                VSlog.e(TAG, "setVivoPauseRotationFlag:pauseRotationLocked");
                this.mWms.getDefaultDisplayContentLocked().getDisplayRotation().pause();
                if (this.mHandler.hasCallbacks(this.mForceUpdateVivoPauseRotationFlagRunnableAvoidErr)) {
                    this.mHandler.removeCallbacks(this.mForceUpdateVivoPauseRotationFlagRunnableAvoidErr);
                }
                this.mHandler.postDelayed(this.mForceUpdateVivoPauseRotationFlagRunnableAvoidErr, this.mMaxForceUpdateVivoPauseRotationFlagDurationAvoidErr);
            } else if (this.mVivoHasPendingDockedBounds && this.mVivoPauseRotationFlag) {
                this.mVivoPauseRotationFlagPending = true;
                if (this.mHandler.hasCallbacks(this.mForceUpdateVivoPauseRotationFlagRunnable)) {
                    this.mHandler.removeCallbacks(this.mForceUpdateVivoPauseRotationFlagRunnable);
                }
                this.mHandler.postDelayed(this.mForceUpdateVivoPauseRotationFlagRunnable, this.mMaxForceUpdateVivoPauseRotationFlagDuration);
            } else if (this.mVivoPauseRotationFlag) {
                this.mVivoPauseRotationFlagPending = false;
                this.mVivoPauseRotationFlag = false;
                VSlog.e(TAG, "setVivoPauseRotationFlag:resumeRotationLocked,  mDeferredRotationPauseCount=" + this.mWms.getDefaultDisplayContentLocked().getDisplayRotation().getDeferredRotationPauseCount());
                this.mWms.getDefaultDisplayContentLocked().getDisplayRotation().resume();
            }
        }
    }

    public void updateVivoSplitScreenResizingPendignStatus(boolean bHasPendingDockedBounds) {
        if (this.mWms.mAtmService.isMultiWindowSupport()) {
            VSlog.e(TAG, "updateVivoSplitScreenResizingStatus:pause=" + bHasPendingDockedBounds + " mVivoPauseRotationFlagPending=" + this.mVivoPauseRotationFlagPending + " mVivoPauseRotationFlag=" + this.mVivoPauseRotationFlag);
            this.mVivoHasPendingDockedBounds = bHasPendingDockedBounds;
            if (this.mVivoPauseRotationFlag && this.mVivoPauseRotationFlagPending && !bHasPendingDockedBounds) {
                VSlog.e(TAG, "updateVivoSplitScreenResizingPendignStatus:resumeRotationLocked,  mDeferredRotationPauseCount=" + this.mWms.getDefaultDisplayContentLocked().getDisplayRotation().getDeferredRotationPauseCount());
                this.mVivoPauseRotationFlagPending = false;
                this.mVivoPauseRotationFlag = false;
                this.mWms.getDefaultDisplayContentLocked().getDisplayRotation().resume();
            }
        }
    }

    public boolean getVivoPauseRotationFlag() {
        return this.mVivoPauseRotationFlag;
    }

    public boolean getVivoMultiWindowRotationFlag() {
        return this.mVivoMultiWindowRotationFlag;
    }

    public boolean setVivoMultiWindowRotationFlag(boolean flag) {
        VSlog.w(TAG, "setVivoMultiWindowRotationFlag:change from " + this.mVivoMultiWindowRotationFlag + " to " + flag);
        this.mVivoMultiWindowRotationFlag = flag;
        return flag;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tempSetMiniNavColorState(boolean colorState) {
        this.bMiniNavColorState = colorState;
        if (VivoMultiWindowConfig.DEBUG) {
            VSlog.i(TAG, "vivo_multiwindow_fmk set mini nav color state " + colorState);
        }
    }

    public boolean getMiniNavColorState() {
        return this.bMiniNavColorState;
    }

    public void disableMiniLauncherNavColorState(int timeout) {
        if (!VivoMultiWindowConfig.IS_VIVO_SPLIT_NAV_COLOR || !this.mWms.isVivoMultiWindowSupport()) {
            return;
        }
        this.mHandler.removeMessages(SPLIT_CLEAR_TEMP_COLOR_STATE);
        this.mHandler.sendEmptyMessageDelayed(SPLIT_CLEAR_TEMP_COLOR_STATE, timeout);
        tempSetMiniNavColorState(false);
    }

    public void enableMiniLauncherNavColorState() {
        if (!VivoMultiWindowConfig.IS_VIVO_SPLIT_NAV_COLOR || !this.mWms.isVivoMultiWindowSupport()) {
            return;
        }
        this.mHandler.removeMessages(SPLIT_CLEAR_TEMP_COLOR_STATE);
        tempSetMiniNavColorState(true);
    }

    public boolean isLayoutIncludeNavApp(String packageName) {
        if (VivoMultiWindowConfig.IS_VIVO_LAYOUT_INCLUDE_NAVBAR && isVivoMultiWindowSupport() && isInVivoMultiWindowIgnoreVisibility() && VivoMultiWindowConfig.getInstance().isLayoutIncludeNavApp(packageName)) {
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.d(TAG, "Not Add FLAG_ALWAYS_CONSUME_NAV_BAR for: " + packageName);
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean vivoSurfacePlacerNotThrow(String reason) {
        WindowManagerService windowManagerService;
        if (reason.contains("recursivecall") && (windowManagerService = this.mWms) != null && windowManagerService.isVivoMultiWindowSupport() && this.mWms.isInVivoMultiWindowIgnoreVisibilityFocusedDisplay()) {
            RuntimeException stack = new RuntimeException();
            stack.fillInStackTrace();
            VSlog.w(TAG, "performSurfacePlacementLoop Recursive call!", stack);
            return true;
        }
        return false;
    }

    public void vivoDebugSplitScreenReloadProp() {
        VivoMultiWindowConfig.reloadSplitSystemProperties();
    }

    public boolean splitNotPointerCallBack(WindowState windowState) {
        if (windowState != null && windowState.getOwningPackage() != null && windowState.getOwningPackage().contains(VivoMultiWindowConfig.SMART_MULTIWINDOW_NAME) && !this.mWms.isInVivoMultiWindowIgnoreVisibilityFocusedDisplay()) {
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.i(TAG, "vivo_split_full splitNotPointerCallBack not pointer to minilauncher not in split");
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean isDoublePhoneForSplit() {
        return false;
    }

    public /* synthetic */ void lambda$new$0$VivoWmsImpl() {
        this.mDisplayRotationControllerForMultiWindow = null;
    }

    public void setDisplayWindowRotationControllerForMultiWindow(int callingUid, int callingPid, IDisplayWindowRotationController controller) {
        try {
            synchronized (this.mWms.mGlobalLock) {
                if (this.mDisplayRotationControllerForMultiWindow != null) {
                    this.mDisplayRotationControllerForMultiWindow.asBinder().unlinkToDeath(this.mDisplayRotationControllerDeathForMultiWindow, 0);
                    this.mDisplayRotationControllerForMultiWindow = null;
                }
                VSlog.i(TAG, "setDisplayWindowRotationControllerForMultiWindow callingUid:" + callingUid + " callingPid:" + callingPid + " controller:" + controller);
                controller.asBinder().linkToDeath(this.mDisplayRotationControllerDeathForMultiWindow, 0);
                this.mDisplayRotationControllerForMultiWindow = controller;
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Unable to set multiWindow rotation controller");
        }
    }

    public IDisplayWindowRotationController getMultiWindowRotationController() {
        return this.mDisplayRotationControllerForMultiWindow;
    }

    public void finishMultiWindowTransaction() {
        if (!VivoMultiWindowConfig.IS_VIVO_SPLIT_SPLITFULL_ANIM) {
            return;
        }
        this.mHandler.removeMessages(102);
        this.mHandler.sendEmptyMessage(102);
    }

    public void resetMainWindowSizeChangeTransaction(IBinder token) {
        WindowContainer wc;
        Task task;
        if (!this.mWms.mAtmService.isMultiWindowSupport() || (wc = WindowContainer.fromBinder(token)) == null || (task = wc.asTask()) == null) {
            return;
        }
        task.setMainWindowSizeChangeTransaction((SurfaceControl.Transaction) null);
    }

    public void debugInvalidConfigUpdate(WindowContainer container, Configuration lastFullConfig, Configuration tempConfig) {
        if (container != null && lastFullConfig != null) {
            int windowingMode = lastFullConfig.windowConfiguration.getWindowingMode();
            Configuration config = container.getConfiguration();
            Configuration requestConfig = container.getRequestedOverrideConfiguration();
            if (windowingMode != 3 && windowingMode != 4 && container.inSplitScreenWindowingMode()) {
                VSlog.w(TAG, "debugInvalidConfigUpdate to invalidconfig to " + container + ",tempConfig is " + tempConfig + ",lastFullConfig is " + lastFullConfig + ",currentConfig is " + config + ",requesetConfig is " + requestConfig + ",callback form:" + Debug.getCallers(15));
            }
        }
    }

    public void checkDragResizingState(IBinder token) {
        WindowContainer wc;
        Task task;
        if (!this.mWms.mAtmService.isMultiWindowSupport() || (wc = WindowContainer.fromBinder(token)) == null || (task = wc.asTask()) == null) {
            return;
        }
        ActivityRecord ar = task.getTopMostActivity();
        WindowState ws = ar != null ? ar.findMainWindow() : null;
        if (ws != null && ws.isDragResizing() && ws.isDragResizeChanged()) {
            VSlog.i(TAG, "checkDragResizingState ws:" + ws);
            ws.setDragResizing();
        }
    }

    public void moveMiniLauncherToFront(IBinder token) {
        Task task;
        WindowContainer wc = WindowContainer.fromBinder(token);
        if (wc == null || (task = wc.asTask()) == null) {
            return;
        }
        Task miniLauncherTask = this.mWms.mRoot.getDefaultTaskDisplayArea().getMiniLauncherTask();
        StringBuilder sb = new StringBuilder();
        sb.append("moveMiniLauncherToFront task:");
        sb.append(task);
        sb.append(" miniLauncherTask:");
        sb.append(miniLauncherTask);
        sb.append(" Parent:");
        sb.append(miniLauncherTask != null ? miniLauncherTask.getParent() : "Null");
        VSlog.d(TAG, sb.toString());
        if (miniLauncherTask != null) {
            miniLauncherTask.getParent().positionChildAt(Integer.MAX_VALUE, miniLauncherTask, false);
        }
    }

    public boolean isPinnedStackExistImpl() {
        return false;
    }

    public void unFreezeWaitingToDraw(WindowContainer container) {
        try {
            ArrayList<String> vfpsUnfrozenList = new ArrayList<>();
            ArrayList<Integer> vfpsUnfrozenUidList = new ArrayList<>();
            synchronized (this.mWms.mGlobalLock) {
                for (int j = container.mWaitingForDrawn.size() - 1; j >= 0; j--) {
                    WindowState w = (WindowState) container.mWaitingForDrawn.get(j);
                    String pkgName = null;
                    if (w != null) {
                        pkgName = this.mContext.getPackageManager().getNameForUid(w.mOwnerUid);
                    }
                    if (pkgName != null) {
                        vfpsUnfrozenList.add(pkgName);
                        vfpsUnfrozenUidList.add(Integer.valueOf(w.mOwnerUid));
                    }
                }
            }
            for (int j2 = vfpsUnfrozenList.size() - 1; j2 >= 0; j2--) {
                String packageName = vfpsUnfrozenList.get(j2);
                int uid = vfpsUnfrozenUidList.get(j2).intValue();
                if (packageName != null) {
                    this.mVivoFrozenPackageSupervisor.isKeepFrozen(packageName, uid, null, -1, 1, true, "to finish win draw");
                    VSlog.w(TAG, "WAITING_FOR_DRAWN_TIMEOUT: for unfrozen  " + packageName);
                }
            }
        } catch (Exception e) {
            VSlog.w(TAG, "WAITING_FOR_DRAWN_TIMEOUT: for unfrozen  ", e);
        }
    }

    public boolean isWmInVivoFreeForm() {
        return this.mVivoFreeformWindowManager.isWmInVivoFreeForm();
    }

    public boolean isVivoFreeFormStackMax() {
        return this.mVivoFreeformWindowManager.isVivoFreeFormStackMax();
    }

    public void notifyFreeFormStackMaxChanged(boolean fullScreen) {
        this.mVivoFreeformWindowManager.notifyFreeFormStackMaxChanged(fullScreen);
    }

    public boolean isFreeFormResizing() {
        return this.mVivoFreeformWindowManager.isFreeFormResizing();
    }

    public void setFreeFormResizing(boolean resize) {
        this.mVivoFreeformWindowManager.setFreeFormResizing(resize);
    }

    public boolean isResizingTask() {
        return this.mVivoFreeformWindowManager.isResizingTask();
    }

    public void setResizingTask(boolean resizingTask) {
        this.mVivoFreeformWindowManager.setResizingTask(resizingTask);
    }

    public boolean isEnteringFreeForm() {
        return this.mVivoFreeformWindowManager.isEnteringFreeForm();
    }

    public void setEnteringFreeForm(boolean enteringFreeForm) {
        this.mVivoFreeformWindowManager.setEnteringFreeForm(enteringFreeForm);
    }

    public boolean isExitingFreeForm() {
        return this.mVivoFreeformWindowManager.isExitingFreeForm();
    }

    public void setExitingFreeForm(boolean exitingFreeForm) {
        this.mVivoFreeformWindowManager.setExitingFreeForm(exitingFreeForm);
    }

    public boolean isInDirectFreeformState() {
        return this.mVivoFreeformWindowManager.isInDirectFreeformState();
    }

    public boolean isClosingFreeForm() {
        return this.mVivoFreeformWindowManager.isClosingFreeForm();
    }

    public void setClosingFreeForm(boolean closingFreeForm) {
        this.mVivoFreeformWindowManager.setClosingFreeForm(closingFreeForm);
    }

    public boolean isVivoFreeformFeatureSupport() {
        return this.mVivoFreeformWindowManager.isVivoFreeformFeatureSupport();
    }

    public Rect getCurFocusWindowVisibleFrame() {
        return this.mVivoFreeformWindowManager.getCurFocusWindowVisibleFrame();
    }

    public boolean isAdjustedForLeftNavBar() {
        return this.mVivoFreeformWindowManager.getIsAdjustedForLeftNavBar();
    }

    public boolean isNavigationbarVisible() {
        return this.mVivoFreeformWindowManager.isNavigationbarVisible();
    }

    public boolean isInVivoFreeform() {
        return this.mVivoFreeformWindowManager.isInVivoFreeform();
    }

    public boolean isVivoFreeFormValid() {
        return this.mVivoFreeformWindowManager.isVivoFreeFormValid();
    }

    public void enableVivoFreeFormRuntime(boolean enable, boolean inDirectFreeformState) {
        this.mVivoFreeformWindowManager.enableVivoFreeFormRuntime(enable, inDirectFreeformState);
    }

    public boolean isFreeFormMin() {
        return this.mVivoFreeformWindowManager.isFreeFormMin();
    }

    public void setFreeFormMin(boolean freeFormMin) {
        this.mVivoFreeformWindowManager.setFreeFormMin(freeFormMin);
    }

    public boolean isRemovingFreeformStack() {
        return this.mVivoFreeformWindowManager.isRemovingFreeformStack();
    }

    public void setIsRemovingFreeformStack(boolean isRemovingFreeformStack) {
        this.mVivoFreeformWindowManager.setIsRemovingFreeformStack(isRemovingFreeformStack);
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public boolean isFreeformStackMove() {
        return this.mVivoFreeformWindowManager.isFreeformStackMove();
    }

    public void setFreeformStackMove(boolean freeformStackMove) {
        this.mVivoFreeformWindowManager.setFreeformStackMove(freeformStackMove);
    }

    public Rect getFreeformPosition() {
        return this.mVivoFreeformWindowManager.getFreeformPosition();
    }

    public boolean isTopFullscreenIsTranslucency() {
        return this.mVivoFreeformWindowManager.isTopFullscreenIsTranslucency();
    }

    public void setTopFullscreenIsTranslucency(boolean topFullscreenIsTranslucency) {
        this.mVivoFreeformWindowManager.setTopFullscreenIsTranslucency(topFullscreenIsTranslucency);
    }

    public boolean isStartingRecentBreakAdjust() {
        return this.mVivoFreeformWindowManager.isStartingRecentBreakAdjust();
    }

    public void setStartingRecentBreakAdjust(boolean startingRecentBreakAdjust) {
        this.mVivoFreeformWindowManager.setStartingRecentBreakAdjust(startingRecentBreakAdjust);
    }

    public void setFreeformAdjustForImeWhenResize() {
        this.mVivoFreeformWindowManager.setFreeformAdjustForImeWhenResize();
    }

    public boolean isStartingRecent() {
        return this.mVivoFreeformWindowManager.isStartingRecent();
    }

    public void setStartingRecent(boolean startingRecent) {
        this.mVivoFreeformWindowManager.setStartingRecent(startingRecent);
    }

    public boolean isDirectFreeformFinishingResizing() {
        return this.mVivoFreeformWindowManager.isDirectFreeformFinishingResizing();
    }

    public void setIsDirectFreeformFinishingResizing(boolean isDirectFreeformFinishingResizing) {
        this.mVivoFreeformWindowManager.setIsDirectFreeformFinishingResizing(isDirectFreeformFinishingResizing);
    }

    public boolean isScreenRotating(DisplayContent displayContent) {
        return this.mVivoFreeformWindowManager.isScreenRotating(displayContent);
    }

    public void updateFreeformForceList(ArrayList<String> forceFullScreenActivitylist) {
        this.mVivoFreeformWindowManager.updateFreeformForceList(forceFullScreenActivitylist);
    }

    public void setVivoFreeformWhiteListSwitchValue(boolean switchValue) {
        this.mVivoFreeformWindowManager.setVivoFreeformWhiteListSwitchValue(switchValue);
    }

    public boolean isImeTarget(IBinder appToken, DisplayContent displayContent) {
        return this.mVivoFreeformWindowManager.isImeTarget(appToken, displayContent);
    }

    public boolean ignoreAddOpeningAppsInVivoFreeform() {
        if (isVivoFreeFormValid()) {
            if (isInVivoFreeform() || isExitingFreeForm()) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void changeSystemUIVisibilityInFreeform(WindowState win) {
        if (isVivoFreeFormValid() && win.getTask() != null && win.inFreeformWindowingMode() && "com.tencent.mm/com.tencent.mm.ui.LauncherUI".equals(win.getWindowTag().toString())) {
            win.mSystemUiVisibility &= -257;
        }
    }

    public boolean ignoreReturnWhenStopFreezingInFreeform(int numOpeningApps) {
        if (isVivoFreeFormValid()) {
            if ((isInVivoFreeform() || isExitingFreeForm()) && numOpeningApps != 0 && this.mWms.mRoot.mOrientationChangeComplete) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void maskForceFreezingScreenInSplit() {
        this.forceScreenTime = SystemClock.uptimeMillis();
    }

    public boolean ignoreReturnWhenStopFreezingInSplit(int mWindowsFreezingScreen, int mAppsFreezingScreen) {
        if (this.mWms.mAtmService.isMultiWindowSupport() && isInVivoMultiWindowConsiderVisibilityFocusedDisplay()) {
            int duration = (int) (SystemClock.uptimeMillis() - this.forceScreenTime);
            DisplayContent dc = this.mWms.mRoot.getDisplayContent(0);
            boolean isRotation = dc != null ? dc.getDisplayRotation().isRotationChanging() : false;
            if (!isRotation && duration < 200 && duration >= 0 && (mAppsFreezingScreen > 0 || mWindowsFreezingScreen == 1)) {
                if (isSplitLogDebug()) {
                    VSlog.d(TAG, "ignoreReturnWhenStopFreezingInSplit of mAppsFreezingScreen:" + mAppsFreezingScreen + ",mWindowsFreezingScreen:" + mWindowsFreezingScreen);
                }
                return true;
            }
        }
        return false;
    }

    public void setIsFreeformExitToTop(boolean toTop) {
        this.mVivoFreeformWindowManager.setIsFreeformExitToTop(toTop);
    }

    public boolean ignoreStopFreezingWhenHaveChangingApps(DisplayContent displayContent) {
        return this.mVivoFreeformWindowManager.ignoreStopFreezingWhenHaveChangingApps(displayContent);
    }

    public void setFreeformMovedToSecondDisplay() {
        this.mVivoFreeformWindowManager.setFreeformMovedToSecondDisplay();
    }

    public void freezingMultiWindow(int during) {
        this.mVivoFreeformWindowManager.freezingMultiWindow(during);
    }

    public boolean isFreeformAnimating() {
        return this.mVivoFreeformWindowManager.isFreeformAnimating();
    }

    public void setFreeformAnimating(boolean freeformAnimating) {
        this.mVivoFreeformWindowManager.setFreeformAnimating(freeformAnimating);
    }

    public boolean isSettingFreeformTaskFocused() {
        return this.mVivoFreeformWindowManager.isSettingFreeformTaskFocused();
    }

    public void setSettingFreeformTaskFocused(boolean settingFreeformTaskFocused) {
        this.mVivoFreeformWindowManager.setSettingFreeformTaskFocused(settingFreeformTaskFocused);
    }

    public void getCurLogicalDisplayRect(Rect out, int displayId) {
        this.mVivoFreeformWindowManager.getCurLogicalDisplayRect(out, displayId);
    }

    public int getSensorOrientation(int displayId) {
        return this.mVivoFreeformWindowManager.getSensorOrientation(displayId);
    }

    public void setStackUnderFreeformFocusedIfNeeded() {
        this.mVivoFreeformWindowManager.setStackUnderFreeformFocusedIfNeeded();
    }

    public void sendResizeFreeformTaskForIme(Rect rect, int taskId) {
        this.mVivoFreeformWindowManager.sendResizeFreeformTaskForIme(rect, taskId);
    }

    public void setFreeformRotateWhenResize() {
        this.mVivoFreeformWindowManager.setFreeformRotateWhenResize();
    }

    public boolean isNavigationBarGestureOff() {
        return this.mVivoFreeformWindowManager.isNavigationBarGestureOff();
    }

    public int getPortraitLimitedTop() {
        return this.mVivoFreeformWindowManager.getPortraitLimitedTop();
    }

    public int getLandLimitedLeft(int rotation) {
        return this.mVivoFreeformWindowManager.getLandLimitedLeft(rotation);
    }

    public void updateFreeformTaskSnapshot(Task freeformTaskRecord) {
        this.mVivoFreeformWindowManager.updateFreeformTaskSnapshot(freeformTaskRecord);
    }

    public void limitWindowDragBoundsForVivoFreeform(Task task, Rect windowDragBounds, InputWindowHandle dragWindowHandle, int minWidth, int minHeight) {
        this.mVivoFreeformWindowManager.limitWindowDragBoundsForVivoFreeform(task, windowDragBounds, dragWindowHandle, minWidth, minHeight);
    }

    public void VCD_FF_MOVE(WindowState windowState) {
        this.mVivoFreeformWindowManager.VCD_FF_MOVE(windowState);
    }

    public boolean isFreeformMiniStateChanged() {
        return this.mVivoFreeformWindowManager.isFreeformMiniStateChanged();
    }

    public void setFreeformMiniStateChanged(boolean freeformMiniStateChanged) {
        this.mVivoFreeformWindowManager.setFreeformMiniStateChanged(freeformMiniStateChanged);
    }

    public void miniMizeFreeformWhenShowSoftInputIfNeed(IBinder focusWin) {
        this.mVivoFreeformWindowManager.miniMizeFreeformWhenShowSoftInputIfNeed(focusWin);
    }

    public float getFreeformScale() {
        return this.mVivoFreeformWindowManager.getFreeformScale();
    }

    public void setFreeformScale(float freeformScale) {
        this.mVivoFreeformWindowManager.setFreeformScale(freeformScale);
    }

    public void scaleFreeformBack(Rect frame) {
        this.mVivoFreeformWindowManager.scaleFreeformBack(frame);
    }

    public void scaleFreeformToReal(Rect frame) {
        this.mVivoFreeformWindowManager.scaleFreeformToReal(frame);
    }

    public float getLastFreeformScale() {
        return this.mVivoFreeformWindowManager.getLastFreeformScale();
    }

    public boolean getLayoutHintForVivoFreeformLw(WindowToken windowToken, Rect outFrame, Rect outContentInsets, Rect outStableInsets, DisplayCutout.ParcelableWrapper outDisplayCutout) {
        ActivityRecord activity = windowToken != null ? windowToken.asActivityRecord() : null;
        Task task = activity != null ? activity.getTask() : null;
        if (task == null || !task.inFreeformWindowingMode() || !isVivoFreeFormValid() || !isInVivoFreeform()) {
            return false;
        }
        outFrame.set(task.getBounds());
        outContentInsets.setEmpty();
        outStableInsets.setEmpty();
        outDisplayCutout.set(DisplayCutout.NO_CUTOUT);
        return true;
    }

    public boolean ignoreRemoveChangingContainersForVivoFreeform(WindowContainer windowContainer, DisplayContent dc) {
        return this.mVivoFreeformWindowManager.ignoreRemoveChangingContainersForVivoFreeform(windowContainer, dc);
    }

    public void registerFreeformPointEventListener(DisplayContent displayContent) {
        this.mVivoFreeformWindowManager.registerFreeformPointEventListener(displayContent);
    }

    public void unRegisterFreeformPointEventListener(DisplayContent displayContent) {
        this.mVivoFreeformWindowManager.unRegisterFreeformPointEventListener(displayContent);
    }

    public boolean getIsVosProduct() {
        return sIsVosProduct;
    }

    public void requestCheckForInputException(IWindow window, boolean isOpaque) {
        synchronized (this.mWms.mGlobalLock) {
            WindowState win = this.mWms.windowForClientLocked((Session) null, window, false);
            if (win == null) {
                VSlog.w(TAG, "requestCheckForInputException, win = null");
                return;
            }
            try {
                ((SystemAutoRecoverManagerInternal) LocalServices.getService(SystemAutoRecoverManagerInternal.class)).requestCheckForInputException(win, isOpaque);
            } catch (Exception e) {
                VSlog.d(TAG, "requestCheckForInputException cause exception: " + e);
            }
        }
    }

    public List<ComponentName> getVisbileActivities() {
        final ArrayList<ComponentName> visibleActivties;
        synchronized (this.mWms.mGlobalLock) {
            visibleActivties = new ArrayList<>();
            this.mWms.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.-$$Lambda$VivoWmsImpl$Qx46FcCwe9eAxBkQxNQRFnH0Pl0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    VivoWmsImpl.lambda$getVisbileActivities$1(visibleActivties, (WindowState) obj);
                }
            }, true);
        }
        return visibleActivties;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getVisbileActivities$1(ArrayList visibleActivties, WindowState w) {
        if (w.mActivityRecord != null && w.mAttrs.type == 1 && w.mActivityRecord.mActivityComponent != null && w.isVisibleLw()) {
            visibleActivties.add(w.mActivityRecord.mActivityComponent);
        }
    }

    public InputManagerService getInputManager() {
        return this.mWms.mInputManager;
    }

    public IVivoScreenshotManager getForegroundAppScreeshotManager() {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) != 1000) {
            VSlog.w(TAG, "getScreenshotManager calling not system uid");
            return null;
        }
        synchronized (this.mWms.mGlobalLock) {
            String topPkg = null;
            ActivityStack focusedStack = this.mWms.mRoot.getTopDisplayFocusedStack();
            ActivityRecord r = focusedStack != null ? focusedStack.topRunningActivityLocked() : null;
            if (r != null && r.app != null) {
                topPkg = r.packageName;
            }
            VSlog.d(TAG, "getForegroundAppScreeshotManager topRunningPkg : " + topPkg);
            if (topPkg == null) {
                return null;
            }
            final String topRunningPkg = topPkg;
            int focusDc = this.mFocusDisplay;
            DisplayContent dc = this.mWms.mRoot.getDisplayContent(focusDc);
            if (dc.mInputMethodWindow != null && dc.mInputMethodWindow.isVisibleLw()) {
                VSlog.w(TAG, "getScreenshotManager not support as ime window is showing");
                return null;
            }
            this.mTopRunningAppWinForSreenshot = null;
            this.mHasFreeformWinCover = false;
            dc.forAllWindows(new Consumer() { // from class: com.android.server.wm.-$$Lambda$VivoWmsImpl$us0dFa5MhcwRkUuIhTxJz2vucjw
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    VivoWmsImpl.this.lambda$getForegroundAppScreeshotManager$2$VivoWmsImpl(topRunningPkg, (WindowState) obj);
                }
            }, true);
            VSlog.d(TAG, "mTopRunningAppWinForSreenshot = " + this.mTopRunningAppWinForSreenshot + " mHasFreeformWinCover = " + this.mHasFreeformWinCover);
            if (this.mTopRunningAppWinForSreenshot == null || this.mTopRunningAppWinForSreenshot.mSession == null) {
                return null;
            }
            return this.mTopRunningAppWinForSreenshot.mSession.getVivoScreenshotManager();
        }
    }

    public /* synthetic */ void lambda$getForegroundAppScreeshotManager$2$VivoWmsImpl(String topRunningPkg, WindowState w) {
        if (this.mTopRunningAppWinForSreenshot != null || this.mHasFreeformWinCover) {
            return;
        }
        if (w.mActivityRecord != null && topRunningPkg.equals(w.mActivityRecord.packageName) && w.mActivityRecord.mVisibleRequested && w.isVisible() && !w.inSplitScreenWindowingMode() && isWinVisibleRectSuitable(w)) {
            this.mTopRunningAppWinForSreenshot = w;
        }
        if (w.isVisible() && w.inFreeformWindowingMode()) {
            this.mHasFreeformWinCover = true;
        }
    }

    private boolean isWinVisibleRectSuitable(WindowState win) {
        int dw = win.getDisplayInfo().logicalWidth;
        int dh = win.getDisplayInfo().logicalHeight;
        return !win.getFrameLw().isEmpty() && win.getFrameLw().intersect(0, 0, dw, dh);
    }

    public void systemReady() {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.systemReady();
        }
        this.mAlertWindowNotificationController.systemReady();
        this.mGraphicsConfigController.systemReady();
    }

    public void updateHasSurfaceView(Session session, IWindow window, boolean visible) {
        synchronized (this.mWms.mGlobalLock) {
            WindowState windowState = this.mWms.windowForClientLocked(session, window, false);
            if (windowState != null && windowState.mActivityRecord != null) {
                VSlog.i(TAG, "updateHasSurfaceView: visible = " + visible + " appToken = " + windowState.mActivityRecord.mActivityComponent.flattenToShortString());
                windowState.mActivityRecord.mHasSurfaceView = visible;
            }
        }
    }

    public int useSnapshotState(int module) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow == null) {
            return 0;
        }
        return snapshotWindow.useSnapshotState(module);
    }

    public void snapshotUnlockCloseAnim(boolean closeAnim) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.snapshotUnlockCloseAnim(closeAnim);
        }
    }

    public boolean transitionAnimIsClosed() {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        return snapshotWindow != null && snapshotWindow.transitionAnimIsClosed();
    }

    public boolean snapshotWinIsVisible() {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        return snapshotWindow != null && snapshotWindow.snapshotWinIsVisible();
    }

    public boolean needCancleAnim() {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        return snapshotWindow != null && snapshotWindow.needCancleAnim();
    }

    public void onFreezingDisplay(boolean start, int frozenDisplayId, long startFreezeTime) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.onFreezingDisplay(start, frozenDisplayId, startFreezeTime);
        }
    }

    public float showDimLayerGetForceAlpha(WindowState win, WindowContainer host, float alpha) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            return snapshotWindow.showDimLayerGetForceAlpha(win, host, alpha);
        }
        return alpha;
    }

    public boolean fastUnlockForceShowDimLayer(WindowState win) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        return snapshotWindow != null && snapshotWindow.fastUnlockForceShowDimLayer(win);
    }

    public boolean isSnapshotWindow(WindowState wallpaperTargetWindow) {
        if (wallpaperTargetWindow != null && wallpaperTargetWindow.mAttrs != null) {
            String title = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + ((Object) wallpaperTargetWindow.mAttrs.getTitle());
            if ("SnapshotWindow".equals(title)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void createSnapshotWindow(int module) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.createSnapshotWindow(module);
        }
    }

    public void setSnapshotVisibility(int module, boolean show) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.setSnapshotVisibility(module, show);
        }
    }

    public void notifyRotationChanged(int rotation) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.notifyRotationChanged(rotation);
        }
    }

    public void addSnapshotWinLocked(WindowState win) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.addSnapshotWinLocked(win);
        }
    }

    public void removeSnapshotWinLocked(WindowState win) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.removeSnapshotWinLocked(win);
        }
    }

    public void finishDrawingLocked(WindowState win) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.finishDrawingLocked(win);
        }
    }

    public boolean isGoogleUnlock() {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        return snapshotWindow != null && snapshotWindow.isGoogleUnlock();
    }

    public boolean needWaitingForDrawn(WindowState win) {
        try {
            if (!isGoogleUnlock() || win == null || win.mRemoved || win.mActivityRecord == null) {
                return false;
            }
            if (!win.mActivityRecord.isTopRunningActivity()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            VSlog.w(TAG, "Exception: ", e);
            return false;
        }
    }

    public void vivoOnWindowFocusChanged(int displayId, WindowState oldFocus, WindowState newFocus) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.vivoOnWindowFocusChanged(displayId, oldFocus, newFocus);
        }
    }

    public void focusAppHasDrawn(WindowState win) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.focusAppHasDrawn(win);
        }
    }

    public void recentsAnimHideFocusActivity(ActivityStack restoreTargetBehindStack) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            snapshotWindow.recentsAnimHideFocusActivity(restoreTargetBehindStack);
        }
    }

    public void recordIncomingFloatWin(WindowState win) {
        if (!this.mIsSupportIncomingFloat) {
            return;
        }
        if (Constant.APP_WEIXIN.equals(win.mAttrs.packageName) || "jp.naver.line.android".equals(win.mAttrs.packageName) || "com.facebook.orca".equals(win.mAttrs.packageName)) {
            if (win.mAttrs.type == 2002 || win.mAttrs.type == 2038) {
                this.incomingFloatWindows.add(new WeakReference<>(win));
                VSlog.v(TAG, "record third-party incoming float window " + win);
                Message msg = this.mHandler.obtainMessage(UPDATA_GAME_MODE);
                this.mHandler.sendMessage(msg);
            }
        }
    }

    public void removeIncomingFloatWinRecord(WindowState win) {
        if (!this.mIsSupportIncomingFloat) {
            return;
        }
        Iterator<WeakReference<WindowState>> it = this.incomingFloatWindows.iterator();
        while (it.hasNext()) {
            WindowState w = it.next().get();
            if (w == null) {
                it.remove();
            } else if (w == win) {
                VSlog.v(TAG, "remove record third-party incoming float window " + win);
                it.remove();
            }
        }
    }

    public void updateIncomingFloatWinRecord() {
        this.mHandler.sendEmptyMessage(UPDATA_GAME_MODE);
    }

    public boolean hasForegroundWindow(final String packageName) {
        WindowState currentFocusedWindow = this.mWms.getCurrentFocusedWindowLocked();
        if (currentFocusedWindow != null && currentFocusedWindow.mAttrs != null) {
            WindowManager.LayoutParams attrs = currentFocusedWindow.mAttrs;
            CharSequence title = attrs.getTitle();
            VSlog.d(TAG, "hasForegroundWindow, currentFocusedView: " + ((Object) title) + ", pkgName:" + attrs.packageName);
            if (("UpSlideTransparentView".equals(title) && "com.vivo.upslide".equals(attrs.packageName)) || (("StatusBar".equals(title) || "QSCenter".equals(title) || "NotificationShade".equals(title)) && FaceUIState.PKG_SYSTEMUI.equals(attrs.packageName))) {
                VSlog.d(TAG, "check " + packageName + " and mCurrentFocus is " + currentFocusedWindow);
                return true;
            }
        }
        final boolean[] hasForegroundWindow = new boolean[1];
        this.mWms.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.-$$Lambda$VivoWmsImpl$TPK6Uanpdqnb3rj-vfj0Vuxb8AU
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                VivoWmsImpl.lambda$hasForegroundWindow$3(packageName, hasForegroundWindow, (WindowState) obj);
            }
        }, true);
        return hasForegroundWindow[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$hasForegroundWindow$3(String packageName, boolean[] hasForegroundWindow, WindowState w) {
        if (packageName.equals(w.mAttrs.packageName) && w.isWinVisibleLw()) {
            if (w.mAttrs.type != 2005 || !"Toast".equals(w.mAttrs.getTitle())) {
                hasForegroundWindow[0] = true;
                VSlog.d(TAG, packageName + "has foreground window " + w);
            }
        }
    }

    public void startFreezingDisplayLockedBoost() {
        if (this.mPerf == null) {
            this.mPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        }
        AbsVivoPerfManager absVivoPerfManager = this.mPerf;
        if (absVivoPerfManager != null) {
            absVivoPerfManager.perfHint(4233, (String) null);
        }
    }

    public void stopFreezingDisplayLockedBoostRelease() {
        AbsVivoPerfManager absVivoPerfManager = this.mPerf;
        if (absVivoPerfManager != null) {
            absVivoPerfManager.perfLockRelease();
        }
    }

    public IBinder createLayerRecorder(String pkg, String name, IBinder deathToken) {
        return this.mLayerRecorderManager.createLayerRecorder(pkg, name, deathToken);
    }

    public void destroyLayerRecorder(String pkg, IBinder deathToken) {
        this.mLayerRecorderManager.destroyLayerRecorder(pkg, deathToken);
    }

    public void updateWallpaperClientVisibility(IBinder token, boolean visible) {
        if (token == null) {
            VSlog.w(TAG, "updateWallpaperClientVisibility with token is null");
            return;
        }
        int pid = Binder.getCallingPid();
        VSlog.d(TAG, "updateWallpaperClientVisibility " + visible + " form pid = " + pid + " token = " + token);
        updateMapAndWpVis(token, visible);
    }

    public void clearWallpaperClientForcedVisibility(String reason) {
        VSlog.d(TAG, "clearWallpaperClientForcedVisibility, reason:" + reason);
        for (int i = this.mForceWpInVisibleTokens.size() + (-1); i >= 0; i--) {
            IBinder token = this.mForceWpInVisibleTokens.keyAt(i);
            IBinder.DeathRecipient deathRecipient = this.mForceWpInVisibleTokens.valueAt(i);
            token.unlinkToDeath(deathRecipient, 0);
        }
        this.mForceWpInVisibleTokens.clear();
        updateWallpaperClientVisibility();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMapAndWpVis(final IBinder token, boolean visible) {
        synchronized (this.mWms.mGlobalLock) {
            boolean newToken = !this.mForceWpInVisibleTokens.containsKey(token);
            if (visible && !newToken) {
                token.unlinkToDeath(this.mForceWpInVisibleTokens.get(token), 0);
                this.mForceWpInVisibleTokens.remove(token);
                updateWallpaperClientVisibility();
            } else if (!visible && newToken) {
                try {
                    IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.wm.VivoWmsImpl.6
                        @Override // android.os.IBinder.DeathRecipient
                        public void binderDied() {
                            VivoWmsImpl.this.updateMapAndWpVis(token, true);
                        }
                    };
                    token.linkToDeath(deathRecipient, 0);
                    this.mForceWpInVisibleTokens.put(token, deathRecipient);
                    updateWallpaperClientVisibility();
                } catch (RemoteException e) {
                    VSlog.w(TAG, "updateWallpaperClientVisibility: given caller IBinder is already dead.");
                }
            }
        }
    }

    private void updateWallpaperClientVisibility() {
        int size = this.mForceWpInVisibleTokens.size();
        VSlog.v(TAG, "updateWallpaperClientVisibility mForceWpInVisibleTokens size = " + size);
        DisplayContent dc = this.mWms.mRoot.getDisplayContent(this.mFocusDisplay);
        if (dc == null) {
            dc = this.mWms.getDefaultDisplayContentLocked();
        }
        dc.updateWallpaperClientVisibility(size == 0);
    }

    public void updateDisplaySecureLocked(DisplayContent displayContent) {
        this.mVivoEasyShareManager.notifyTaskSecure(displayContent);
    }

    /* JADX WARN: Code restructure failed: missing block: B:128:0x02f6, code lost:
        if (android.os.Process.myPid() != r15.mSession.mPid) goto L114;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void dispatchDragEvent(int r39, int r40, int r41, android.content.ClipDescription r42, android.content.ClipData r43, boolean r44, java.lang.String r45) {
        /*
            Method dump skipped, instructions count: 833
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoWmsImpl.dispatchDragEvent(int, int, int, android.content.ClipDescription, android.content.ClipData, boolean, java.lang.String):void");
    }

    public String getTouchableWindowTitleAtPoint(int x, int y) {
        synchronized (this.mWms.mGlobalLock) {
            if (this.mWms.mRoot == null) {
                VSlog.d(TAG, "getTouchableWindowTitleAtPoint: mRoot is null");
                return null;
            }
            DisplayContent displayContent = this.mWms.mRoot.getDisplayContent(this.mFocusDisplay);
            if (displayContent == null) {
                VSlog.d(TAG, "getTouchableWindowTitleAtPoint: display content is null");
                return null;
            }
            WindowState targetWindow = displayContent.getTouchableWinAtPointLocked(x, y);
            if (targetWindow == null) {
                VSlog.d(TAG, "getTouchableWindowTitleAtPoint: touched window is null");
                return null;
            }
            return targetWindow.toString();
        }
    }

    public void notifyPrimaryClip(ClipData data) {
        this.mVivoEasyShareManager.notifyPrimaryClip(data);
    }

    public boolean shouldShowNotification(String pkg) {
        return this.mAlertWindowNotificationController.shouldShowNotification(pkg);
    }

    public void setInterceptInputKeyStatus(boolean enable) {
        this.mWms.mPolicy.setInterceptInputKeyStatus(enable);
    }

    public boolean interceptKeyeventToLauncherLocked(KeyEvent keyEvent) {
        if (this.mWms.getRecentsAnimationController() != null) {
            return this.mWms.getRecentsAnimationController().interceptKeyeventToLauncherLocked(keyEvent);
        }
        return false;
    }

    public void updateMergedConfigurationForVirtualDisplay(int displayId, MergedConfiguration outConfig) {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING || !MultiDisplayManager.isVivoDisplay(displayId) || this.mWms.mRoot.getDisplayContent(displayId) == null) {
            return;
        }
        outConfig.setGlobalConfiguration(this.mWms.mRoot.getDisplayContent(displayId).getConfiguration());
    }

    public int getFocusedWindowForVCar() {
        WindowState win;
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && (win = this.mWms.mRoot.getTopFocusedDisplayContent().mCurrentFocus) != null) {
            if (win.getName().contains("VivoCarToolBar")) {
                return 1;
            }
            if (win.getDisplayId() == 80000) {
                return 0;
            }
            return 2;
        }
        return -1;
    }

    public void notifyFocusedCarWindowChanged() {
        notifyCarWindowState(0, getFocusedWindowForVCar());
    }

    private void notifyCarWindowState(int state, int windowId) {
        synchronized (this.mCarWindowObserver) {
            int count = this.mCarWindowObserver.beginBroadcast();
            for (int i = 0; i < count; i++) {
                ICarWindowObserver observer = this.mCarWindowObserver.getBroadcastItem(i);
                if (observer != null && state == 0) {
                    try {
                        observer.windowFocusedStateChanged(windowId);
                    } catch (RemoteException e) {
                        VSlog.w("VivoObserver", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, e);
                        unregisterCarWindowObserver(observer);
                    }
                }
            }
            this.mCarWindowObserver.finishBroadcast();
        }
    }

    public void registerCarWindowObserver(ICarWindowObserver observer) {
        VSlog.i("VivoObserver", "register carWindow observer");
        this.mCarWindowObserver.register(observer);
    }

    public void unregisterCarWindowObserver(ICarWindowObserver observer) {
        VSlog.i("VivoObserver", "unregister carWindow observer");
        this.mCarWindowObserver.unregister(observer);
    }

    public WindowState getVisibleStartingWindow() {
        try {
            WindowState windowState = this.mWms.mRoot.getWindow($$Lambda$VivoWmsImpl$iYURkSFG0WWIrEOJIHak6jSEdWA.INSTANCE);
            return windowState;
        } catch (Exception e) {
            VSlog.d(TAG, "getVisibleStartingWindow cause exception: " + e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getVisibleStartingWindow$4(WindowState w) {
        return w.mAttrs.type == 3 && w.isVisibleLw() && w.getAttrs().getTitle().toString().contains("Splash Screen") && w.mToken.asActivityRecord() != null && "com.bbk.launcher2".equals(w.mToken.asActivityRecord().launchedFromPackage);
    }

    public void getStableInsetsWithoutCutout(int displayId, Rect outInsets) {
        synchronized (this.mWms.mGlobalLock) {
            outInsets.setEmpty();
            DisplayContent dc = this.mWms.mRoot.getDisplayContent(displayId);
            if (dc != null) {
                DisplayInfo di = dc.getDisplayInfo();
                dc.getDisplayPolicy().getStableInsetsLw(di.rotation, di.logicalWidth, di.logicalHeight, (DisplayCutout) null, outInsets);
            }
        }
    }

    public boolean isNeedFindFocusedWindow(int displayId) {
        return this.mVivoAppShareManager.isAppsharedDisplayId(displayId);
    }

    public boolean isAppsharedMode() {
        return this.mVivoAppShareManager.isAppsharedMode();
    }

    public boolean isAppsharedDisplayId(int displayId) {
        return this.mVivoAppShareManager.isAppsharedDisplayId(displayId);
    }

    public boolean handleInputMethodNeedMovedWithDisplay(WindowState win, WindowManager.LayoutParams attrs, int displayId, boolean updateConfig) {
        return this.mVivoAppShareManager.handleInputMethodNeedMovedWithDisplay(win, attrs, displayId, updateConfig);
    }

    public int getImeShownDisplayId() {
        return this.mVivoAppShareManager.getImeShownDisplayId();
    }

    public boolean isInputMethodSelectDialogToken(WindowToken wtoken) {
        return this.mVivoAppShareManager.isInputMethodSelectDialogToken(wtoken);
    }

    public boolean isAppShareForeground() {
        return this.mVivoAppShareManager.isAppShareForeground();
    }

    public boolean handleInputMethodNeedMoved(WindowState win, WindowManager.LayoutParams attrs, int viewVisibility, boolean updateConfig) {
        return this.mVivoAppShareManager.handleInputMethodNeedMoved(win, attrs, viewVisibility, updateConfig);
    }

    public void updateInputMethodShow(WindowState win, boolean show) {
        this.mVivoAppShareManager.updateInputMethodShow(win, show);
    }

    public void updateImeShowDisplayId(int displayId) {
        this.mVivoAppShareManager.updateImeShowDisplayId(displayId);
    }

    public void reportImeShownStatus() {
        this.mVivoAppShareManager.reportImeShownStatus();
    }

    public void updateInputMethodAnimationDone(WindowState win, boolean exitAnim) {
        this.mVivoAppShareManager.updateInputMethodAnimationDone(win, exitAnim);
    }

    public void updateWindowAnimationDone(WindowState win, boolean exitAnim) {
        this.mVivoAppShareManager.updateWindowAnimationDone(win, exitAnim);
    }

    public boolean needUpdateWindowConfiguration(WindowState win, int displayId, MergedConfiguration configuration) {
        return this.mVivoAppShareManager.needUpdateWindowConfiguration(win, displayId, configuration);
    }

    public void reportImeConfigUpdate() {
        this.mVivoAppShareManager.reportImeConfigUpdate();
    }

    public DisplayContent getDesiredImeFocusStackLocked(DisplayContent preferDisplayContent) {
        return this.mVivoAppShareManager.getDesiredImeFocusStackLocked(preferDisplayContent);
    }

    public void updateConfigurationForInputMethod(int displayId, boolean force) {
        this.mVivoAppShareManager.updateConfigurationForInputMethod(displayId, force);
    }

    public DisplayContent getDesiredImeDisplayContent(DisplayContent preferDisplayContent, int displayId) {
        return this.mVivoAppShareManager.getDesiredImeDisplayContent(preferDisplayContent, displayId);
    }

    public boolean isNeedSkipFreeze(int displayId) {
        return this.mVivoAppShareManager.isNeedSkipFreeze(displayId);
    }

    public boolean needSkipDisplayEffectService(int displayId) {
        return this.mVivoAppShareManager.needSkipDisplayEffectService(displayId);
    }

    public void prepareSharedScreenShot(int displayId) {
        this.mVivoAppShareManager.prepareSharedScreenShot(displayId);
    }

    public boolean isRequestOrientationByShareDisplay(int displayId) {
        return this.mVivoAppShareManager.isRequestOrientationByShareDisplay(displayId);
    }

    public void updateAppSharedRequestedOrientation(boolean request) {
        this.mVivoAppShareManager.updateAppSharedRequestedOrientation(request);
    }

    public void updateAppSharedRotateFreeze(boolean freeze) {
        this.mVivoAppShareManager.updateAppSharedRotateFreeze(freeze);
    }

    public boolean isAppSharedRotateFreeze() {
        return this.mVivoAppShareManager.isAppSharedRotateFreeze();
    }

    public void decideFreezeDisplay(int displayId) {
        this.mVivoAppShareManager.decideFreezeDisplay(displayId);
    }

    public void updateDisplaySecureLockedForAppShare(DisplayContent displayContent) {
        this.mVivoAppShareManager.updateDisplaySecureLockedForAppShare(displayContent);
    }

    public boolean needUpdateInputMethodClientForce() {
        return this.mVivoAppShareManager.needUpdateInputMethodClientForce();
    }

    public boolean isLandscapeOrientation(int orientation, int rotation) {
        return this.mVivoAppShareManager.isLandscapeOrientation(orientation, rotation);
    }

    public boolean isAppSharedRemoving() {
        return this.mVivoAppShareManager.isAppSharedRemoving();
    }

    public void reportFocusedWindowInputMethodStatus(int displayId, WindowState target) {
        this.mVivoAppShareManager.reportFocusedWindowInputMethodStatus(displayId, target);
    }

    public boolean computeImeTarget(int displayId, WindowState target) {
        return this.mVivoAppShareManager.computeImeTarget(displayId, target);
    }

    public boolean isImeShowInShareDisplay() {
        return this.mVivoAppShareManager.isImeShowInShareDisplay();
    }

    public void updatePointEvent(int displayId, int deviceId) {
        this.mVivoAppShareManager.updatePointEvent(displayId, deviceId);
    }

    public void adjustWindowAppShared(WindowState win) {
        this.mVivoAppShareManager.adjustWindowAppShared(win);
    }

    public boolean isAppShareKeepWindow(WindowState win) {
        return this.mVivoAppShareManager.isAppShareKeepWindow(win);
    }

    public boolean checkAlertOpsWhenAddInAppSharedMode(int res, int appOp, String owningPackageName, int displayId) {
        return this.mVivoAppShareManager.checkAlertOpsWhenAddInAppSharedMode(res, appOp, owningPackageName, displayId);
    }

    public void reportNewFocusForDisplay(int displayId, WindowState newFocus) {
        this.mVivoAppShareManager.reportNewFocusForDisplay(displayId, newFocus);
    }

    public void onDisplayAdded(int displayId) {
        this.mVivoAppShareManager.onDisplayAdded(displayId);
    }

    public void onDisplayRemoved(int displayId) {
        this.mVivoAppShareManager.onDisplayRemoved(displayId);
    }

    public void updateLastInputMethodMove(int displayId) {
        this.mVivoAppShareManager.updateLastInputMethodMove(displayId);
    }

    public void onRemovedWindowLocked(WindowState win) {
        this.mVivoAppShareManager.onRemovedWindowLocked(win);
    }

    public boolean needSkipAssignRotation(DisplayContent displayContext) {
        return this.mVivoAppShareManager.needSkipAssignRotation(displayContext);
    }

    public boolean needKeepRotationByAppShare(int displayId, int orientation, int lastRotation) {
        return this.mVivoAppShareManager.needKeepRotationByAppShare(displayId, orientation, lastRotation);
    }

    public void handleAddWindowForAppShare(WindowState win, boolean shown) {
        this.mVivoAppShareManager.handleAddWindowForAppShare(win, shown);
    }

    public boolean canResizeDisplay(int displayId) {
        return this.mVivoAppShareManager.canResizeDisplay(displayId);
    }

    public int adjustDisplayFreezeTime(int displayId, int preferTime) {
        return this.mVivoAppShareManager.adjustDisplayFreezeTime(displayId, preferTime);
    }

    public int adjustDisplayIdForWindowInAppShare(WindowManager.LayoutParams attrs, int displayId) {
        return this.mVivoAppShareManager.adjustDisplayIdForWindowInAppShare(attrs, displayId);
    }

    public DisplayContent adjustDisplayForWindowTokenAppShare(int type, int displayId, DisplayContent dc) {
        return this.mVivoAppShareManager.adjustDisplayForWindowTokenAppShare(type, displayId, dc);
    }

    public void updateMergedConfigurationForAppShareLocked(WindowState win, MergedConfiguration mergedConfiguration) {
        this.mVivoAppShareManager.updateMergedConfigurationForAppShareLocked(win, mergedConfiguration);
    }

    public boolean isNotComputeLoseFocusWindowLocked(DisplayContent displayContent, WindowState loseWin, boolean block) {
        return this.mVivoAppShareManager.isNotComputeLoseFocusWindowLocked(displayContent, loseWin, block);
    }

    public int getAppShareDisplayFocusWindowType() {
        return this.mVivoAppShareManager.getAppShareDisplayFocusWindowType();
    }

    public boolean isKeyguardLockedWithPackageName(String packageName, int uid, int pid) {
        if (this.mVivoAppShareManager.isOnAppShareDisplay(packageName, uid, pid)) {
            return false;
        }
        return this.mWms.isKeyguardLocked();
    }

    public void addWindowReferenceIfNeededLocked(WindowState win) {
        if (AppShareConfig.SUPPROT_APPSHARE && AppShareConfig.APP_SHARE_ACTIVITY_SHORT_STRING.equals(win.mAttrs.getTitle()) && win.getDisplayId() == 0) {
            this.mAppShareWindow = win;
        }
    }

    public void removeWindowReferenceIfNeedLocked(WindowState win) {
        if (AppShareConfig.SUPPROT_APPSHARE && AppShareConfig.APP_SHARE_ACTIVITY_SHORT_STRING.equals(win.mAttrs.getTitle()) && win.getDisplayId() == 0) {
            this.mAppShareWindow = null;
        }
    }

    public WindowState getAppShareWindowLocked() {
        return this.mAppShareWindow;
    }

    public void setAppShareHoldScreenLocked(Session newHoldScreen) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            boolean hold = newHoldScreen != null;
            if (hold && this.mAppShareHoldingScreenOn != newHoldScreen) {
                this.mAppShareHoldingScreenWakeLock.setWorkSource(new WorkSource(newHoldScreen.mUid));
            }
            this.mAppShareHoldingScreenOn = newHoldScreen;
            boolean state = this.mAppShareHoldingScreenWakeLock.isHeld();
            if (hold != state) {
                if (hold) {
                    this.mAppShareHoldingScreenWakeLock.acquire();
                } else {
                    this.mAppShareHoldingScreenWakeLock.release();
                }
            }
        }
    }

    public void initAppShareHoldingScreenWakeLockIfNeed() {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            PowerManager.WakeLock newWakeLock = this.mWms.mPowerManager.newWakeLock(536870922, "WindowManager_AppShareDisplay");
            this.mAppShareHoldingScreenWakeLock = newWakeLock;
            newWakeLock.setReferenceCounted(false);
        }
    }
}