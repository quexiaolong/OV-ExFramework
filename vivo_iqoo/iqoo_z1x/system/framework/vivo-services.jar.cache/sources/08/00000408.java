package com.android.server.policy;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.IActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.RotateDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.multidisplay.MultiDisplayManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Debug;
import android.os.FtBuild;
import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.FtFeature;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.am.EmergencyBroadcastManager;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.VivoWindowPolicyControllerImpl;
import com.android.server.wm.WindowManagerInternal;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import vivo.util.VSlog;
import vivo.util.VivoThemeUtil;

/* loaded from: classes.dex */
public class VivoRatioControllerUtilsImpl implements IVivoRatioControllerUtils {
    private static final String CURVEDCUTOUT_APP_MODIFIED_BY_USER = "curvedcutout_app_modified_by_user";
    public static final int CURVED_CUTOUT_POLICY_CLOSE = 0;
    public static final int CURVED_CUTOUT_POLICY_OPEN = 1;
    public static final String CUTOUT_SETTINGS_MODIFIED_BY_USER = "cutout_setting_modified_by_user";
    static final String ENGINE_VERSION = "1.0";
    public static final String FULL_SCREEN_APP_MODIFIED_BY_USER = "full_screen_app_modified_by_user";
    public static final String FULL_SCREEN_APP_RUNNING_ABNORMALLY = "full_screen_app_running_abnormally";
    static final String MODULE_NAME = "MultiDisplayAdapter";
    public static final String NAVIGATION_GESTURE_ON = "navigation_gesture_on";
    private static final String PROP_DEVICE_MAXRATIO = "persist.vivo.device_max_ratio";
    static final String STAND_CONFIG_URI = "content://com.vivo.abe.unifiedconfig.provider/standard_config";
    public static final int TYPE_CURVED_CUTOUT = 2;
    public static final int TYPE_CUTOUT_SETTINGS = 1;
    public static final int TYPE_FULL_SCREEN = 0;
    private static boolean isTierProduct = false;
    static String mChangingPkg = null;
    static boolean mChangingStatus = false;
    private static int mCropWidth = 0;
    private static WindowManagerPolicy.WindowState mCurveCuttingWindow = null;
    private static final String sCurvedScreenBlackListUri = "content://com.vivo.settings.fullscreenprovider/curvedscreenblackpkg";
    private Context mContext;
    private Handler mHandler;
    private boolean mIsDefaultDisplay;
    PackageManager mPackageManager;
    public View mRatioSwitchView;
    private SettingsObserver mSettingsObserver;
    VivoListCenter mVivoListCenter;
    VivoWindowPolicyControllerImpl mVivoWindowPolicyController;
    private IWindowManager mWindowManager;
    private WindowManagerInternal mWindowManagerInternal;
    private int sCurrentInputMethodPid;
    private int sCurrentInputMethodUid;
    private int sNoneSecureInputMethodPid;
    private static String TAG = "VivoRatioControllerUtilsImpl";
    private static String TAG_MULTI = "MultiRatioController";
    private static Map<String, Integer> userModifiedPkgMapForCutout = new ArrayMap();
    private static ArrayList<String> sCurvedScreenBlackList = new ArrayList<>();
    private static Map<String, Integer> userModifiedPkgMapForCurvedCutout = new ArrayMap();
    private static VivoRatioControllerUtilsImpl sInstance = null;
    private static final SparseArray<Integer> mDisplayIdForPids = new SparseArray<>();
    private static final SparseArray<Integer> mDisplayIdForInputMethod = new SparseArray<>();
    private static final SparseArray<Integer> mUidForInputMethod = new SparseArray<>();
    private static final ArrayMap<OnRatioChangeListener, String> mCallbacks = new ArrayMap<>();
    public final int VALUE_GESTURE_ON_HOME_INDICATOR = 3;
    public final String COLON_SEPARATOR = ":";
    public final String COMMA_SEPARATOR = ",";
    private Map<String, Integer> userModifiedPkgMapForRatio = new ArrayMap();
    private boolean is18to9 = false;
    private TextView mRatioSwitchButton = null;
    private TagView mRationTagView = null;
    private View mNaviCover = null;
    public Rect mAppBounds = new Rect();
    public String mCurrentPkg = null;
    public String mSwitchingPkg = null;
    public int mCurrentAppOrientation = -1;
    public int ratioViewMinHeight = 16;
    public boolean isLandscape = false;
    private final long delayLaunch = SystemProperties.getLong("persist.relaunchDelay.debug", 1200);
    private int mCurrentRestricHeight = -1;
    public AlertDialog mCurrentWarningDialog = null;
    public boolean mDialogChangeValue = true;
    public boolean mPendingFlag = false;
    public String mPendingChangePkg = null;
    public boolean mPendingChangeValue = false;
    public String mPendingRestartPkg = null;
    public boolean sIsRelaunching = false;
    private DisplayMetrics mMetrics = null;
    public int alienScreenCoverInsetTop = 0;
    public int alienScreenCoverInsetBottom = 24;
    public int alienScreenCoverInsetLand = 48;
    public int statusBarHeight = 32;
    public int navBarHeight = 42;
    public int gestrueBarHeight = 24;
    private final String sBlackListUri = "content://com.vivo.settings.fullscreenprovider/runningabnormallypkg";
    private final String sWhiteListUri = "content://com.vivo.settings.fullscreenprovider/runningnormallypkg";
    private final String sAdaptAlienScreenListUri = "content://com.vivo.settings.fullscreenprovider/adaptedirregularscreenpkg";
    private ArrayList<String> sBlackList = new ArrayList<>();
    private ArrayList<String> sWhiteList = new ArrayList<>();
    private ArrayList<String> sAdaptAlienScreenList = new ArrayList<>();
    private final String sUpdateUserListAction = "com.vivo.settings.update_database_finish";
    private Handler mIoHandler = IoThread.getHandler();
    private final Uri mNavigationGestureOnUri = Settings.Secure.getUriFor(NAVIGATION_GESTURE_ON);
    public boolean sGestureBarIsOn = false;
    public boolean sVivoHasNavBar = false;
    public float sDeviceRatio = 1.86f;
    public float sDeviceHeightPixel = 1920.0f;
    public float sDeviceWidthPixel = 1080.0f;
    public float sDeviceDensity = 3.0f;
    public int mLastRatioViewState = 3;
    private boolean isDualDisplay = false;
    private final Uri mCurvedCutoutUri = Settings.Secure.getUriFor(CURVEDCUTOUT_APP_MODIFIED_BY_USER);
    private boolean mIsDefaultCurveCut = false;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (IVivoRatioControllerUtils.DEBUG) {
                String str = VivoRatioControllerUtilsImpl.TAG;
                VSlog.d(str, "DEBUG_RATIODIALOG:RatioController Receive  action :" + action);
            }
            if (action.equals("android.intent.action.CLOSE_SYSTEM_DIALOGS")) {
                VivoRatioControllerUtilsImpl.this.dismissRatioSwitchDialog();
            } else if (action.equals("android.intent.action.LOCALE_CHANGED")) {
                VivoRatioControllerUtilsImpl.this.mHandler.post(new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.2.1
                    @Override // java.lang.Runnable
                    public void run() {
                        if (VivoRatioControllerUtilsImpl.this.mRatioSwitchButton != null) {
                            VivoRatioControllerUtilsImpl.this.mRationTagView.updateLanguage();
                        }
                    }
                });
            } else if (action.equals("com.vivo.settings.update_database_finish")) {
                VSlog.e(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIOLIST:sUpdateUserListAction ");
                VivoRatioControllerUtilsImpl.this.mIoHandler.removeCallbacks(VivoRatioControllerUtilsImpl.this.retriveListRunnable);
                VivoRatioControllerUtilsImpl.this.mIoHandler.postDelayed(VivoRatioControllerUtilsImpl.this.retriveListRunnable, 3000L);
            } else if ("android.intent.action.USER_SWITCHED".equals(action)) {
                VSlog.d(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIOLIST:  Intent.ACTION_USER_SWITCHED");
                VivoRatioControllerUtilsImpl.this.handleRatioSettingsChangedLocked();
                VivoRatioControllerUtilsImpl.this.handleCutoutSettingsChangedLocked();
                VivoRatioControllerUtilsImpl.this.handleCurvedCutoutSettingsChangedLocked();
                VivoRatioControllerUtilsImpl.this.mIoHandler.removeCallbacks(VivoRatioControllerUtilsImpl.this.retriveListRunnable);
                VivoRatioControllerUtilsImpl.this.mIoHandler.postDelayed(VivoRatioControllerUtilsImpl.this.retriveListRunnable, 2000L);
            }
        }
    };
    public final boolean ALL_ADAPTER = SystemProperties.get("persist.debug.allAdapter", "false").equals("true");
    private boolean mInputMethodChange = false;
    private ComponentName sCurrentInputMethod = null;
    private ComponentName sNoneSecureInputMethod = null;
    private final String sSecureInputMethodPackageName = "com.vivo.secime.service";
    private final SparseArray<ApplicationInfo> mApplicationInfos = new SparseArray<>();
    private final Runnable mFreezeScreenRunable = new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.6
        @Override // java.lang.Runnable
        public void run() {
            if (IVivoRatioControllerUtils.DEBUG) {
                VSlog.d(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIOSWITCH:mFreezeScreenRunable");
            }
            try {
                VivoRatioControllerUtilsImpl.this.mWindowManager.startFreezingScreen(0, 0);
            } catch (Exception e) {
            }
        }
    };
    private final Runnable mUnfreezeScreenRunable = new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.7
        @Override // java.lang.Runnable
        public void run() {
            if (IVivoRatioControllerUtils.DEBUG) {
                VSlog.d(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIOSWITCH:mUnfreezeScreenRunable");
            }
            try {
                VivoRatioControllerUtilsImpl.this.mWindowManager.stopFreezingScreen();
            } catch (Exception e) {
            }
        }
    };
    private int mLastRatioState = -1;
    private int mLastRotation = -1;
    private int mLastRatioViewBound = -1;
    private final Runnable mShowRatioViewTransparent = new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.11
        @Override // java.lang.Runnable
        public void run() {
            if (IVivoRatioControllerUtils.DEBUG) {
                VSlog.d(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIOSWITCH:mShowRatioViewTransparent");
            }
            if (VivoRatioControllerUtilsImpl.this.mRatioSwitchView == null || VivoRatioControllerUtilsImpl.this.mWindowManagerInternal.isInVivoMultiWindowIgnoreVisibilityFocusedDisplay()) {
                return;
            }
            VivoRatioControllerUtilsImpl.this.mRatioSwitchView.setBackgroundColor(0);
            VivoRatioControllerUtilsImpl.this.mRatioSwitchView.setVisibility(0);
        }
    };
    private final Runnable mShowRatioViewBlack = new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.12
        @Override // java.lang.Runnable
        public void run() {
            if (IVivoRatioControllerUtils.DEBUG) {
                VSlog.d(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIOSWITCH:mShowRatioViewBlack");
            }
            if (VivoRatioControllerUtilsImpl.this.mRatioSwitchView == null) {
                return;
            }
            VivoRatioControllerUtilsImpl.this.mRatioSwitchView.setBackgroundColor(-16777216);
            VivoRatioControllerUtilsImpl.this.mRatioSwitchView.setVisibility(0);
        }
    };
    private final Runnable mShowRatioViewWhite = new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.13
        @Override // java.lang.Runnable
        public void run() {
            if (IVivoRatioControllerUtils.DEBUG) {
                VSlog.d(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIOSWITCH:mShowRatioViewWhite");
            }
            if (VivoRatioControllerUtilsImpl.this.mRatioSwitchView == null) {
                return;
            }
            VivoRatioControllerUtilsImpl.this.mRatioSwitchView.setBackgroundColor(-723724);
            VivoRatioControllerUtilsImpl.this.mRatioSwitchView.setVisibility(0);
        }
    };
    private final Runnable mShowRatioViewFollowSystem = new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.14
        @Override // java.lang.Runnable
        public void run() {
            if (IVivoRatioControllerUtils.DEBUG) {
                VSlog.d(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIOSWITCH:mShowRatioViewFollowSystem");
            }
            if (VivoRatioControllerUtilsImpl.this.mRatioSwitchView == null) {
                return;
            }
            int navColor = -723724;
            if (VivoRatioControllerUtilsImpl.this.mWindowManager != null) {
                String navColorString = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                try {
                    navColorString = VivoRatioControllerUtilsImpl.this.mWindowManager.fetchSystemSetting("nav_color");
                } catch (Exception e) {
                }
                if (navColorString != null && navColorString != Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) {
                    if (IVivoRatioControllerUtils.DEBUG) {
                        String str = VivoRatioControllerUtilsImpl.TAG;
                        VSlog.d(str, "DEBUG_RATIOSWITCH:navColorString=" + navColorString);
                    }
                    navColor = Color.parseColor(navColorString);
                }
            }
            VivoRatioControllerUtilsImpl.this.mRatioSwitchView.setBackgroundColor(navColor);
            VivoRatioControllerUtilsImpl.this.mRatioSwitchView.setVisibility(0);
        }
    };
    private final Runnable mHideRatioView = new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.15
        @Override // java.lang.Runnable
        public void run() {
            if (IVivoRatioControllerUtils.DEBUG) {
                VSlog.d(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIOSWITCH:hideRatioSwitch");
            }
            VivoRatioControllerUtilsImpl.this.mRatioSwitchView.setVisibility(8);
        }
    };
    private Runnable retriveListRunnable = new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.16
        @Override // java.lang.Runnable
        public void run() {
            VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl = VivoRatioControllerUtilsImpl.this;
            vivoRatioControllerUtilsImpl.retriveListFromSettings("content://com.vivo.settings.fullscreenprovider/runningabnormallypkg", vivoRatioControllerUtilsImpl.sBlackList);
            VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl2 = VivoRatioControllerUtilsImpl.this;
            vivoRatioControllerUtilsImpl2.retriveListFromSettings("content://com.vivo.settings.fullscreenprovider/runningnormallypkg", vivoRatioControllerUtilsImpl2.sWhiteList);
            VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl3 = VivoRatioControllerUtilsImpl.this;
            vivoRatioControllerUtilsImpl3.retriveListFromSettings("content://com.vivo.settings.fullscreenprovider/adaptedirregularscreenpkg", vivoRatioControllerUtilsImpl3.sAdaptAlienScreenList);
            VivoRatioControllerUtilsImpl.this.retriveListFromSettings(VivoRatioControllerUtilsImpl.sCurvedScreenBlackListUri, VivoRatioControllerUtilsImpl.sCurvedScreenBlackList);
            if (MultiDisplayManager.isMultiDisplay) {
                VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl4 = VivoRatioControllerUtilsImpl.this;
                vivoRatioControllerUtilsImpl4.retriveListFromSettings("content://com.vivo.settings.fullscreenprovider/adaptedbackscreenpkg", vivoRatioControllerUtilsImpl4.sAdaptedBackScreenList);
                VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl5 = VivoRatioControllerUtilsImpl.this;
                vivoRatioControllerUtilsImpl5.retriveListMapFromSettings("content://com.vivo.settings.fullscreenprovider/adaptedbackscreenblackpkg", vivoRatioControllerUtilsImpl5.mAdaptedBackScreenBlackMap);
            }
        }
    };
    public boolean sBackFullScreenIsOn = false;
    public final String BACK_FULL_SCREEN_HOLD = "back_full_screen_hold_enabled";
    private final Uri mBackFullScreenHoldUri = Settings.Secure.getUriFor("back_full_screen_hold_enabled");
    private final String sAdaptedBackScreenListUri = "content://com.vivo.settings.fullscreenprovider/adaptedbackscreenpkg";
    private ArrayList<String> sAdaptedBackScreenList = new ArrayList<>();
    private final String sAdaptedBackScreenBlackListUri = "content://com.vivo.settings.fullscreenprovider/adaptedbackscreenblackpkg";
    private HashMap<String, ArrayList<String>> mAdaptedBackScreenBlackMap = new HashMap<>();
    private ArrayMap<String, Integer> mUserSettingBackScreenMap = new ArrayMap<>();
    private ArrayMap<String, Integer> mLastUserSettingBackScreenMap = new ArrayMap<>();
    public final String BACK_SCREEN_DISPLAY_MODIFIED_BY_USER = "modified_back_screen_display_apps";
    private final Uri mBackScreenDisplayUri = Settings.Secure.getUriFor("modified_back_screen_display_apps");
    private ArrayList<String> sVivoSignaturesAppList = new ArrayList<>();
    private final Uri mFullScreenAppUri = Settings.Secure.getUriFor(FULL_SCREEN_APP_MODIFIED_BY_USER);
    private final Uri mCutoutSettingsUri = Settings.Secure.getUriFor(CUTOUT_SETTINGS_MODIFIED_BY_USER);
    public boolean mStoryCaptureUIError = true;
    private final String sUpdateStoryCaptureUIAdapter = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_MultiDisplayAdapter";
    private Runnable checkStoryCaptureUIRunnable = new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.17
        @Override // java.lang.Runnable
        public void run() {
            VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl = VivoRatioControllerUtilsImpl.this;
            vivoRatioControllerUtilsImpl.mStoryCaptureUIError = vivoRatioControllerUtilsImpl.checkStoryCaptureUINeedsAdapter(VivoRatioControllerUtilsImpl.STAND_CONFIG_URI, VivoRatioControllerUtilsImpl.MODULE_NAME, "1.0");
            VSlog.e("StoryCaptureUI", "DEBUG_RATIOLIST:UpdateStoryCaptureUIAdapter : mStoryCaptureUIError = " + VivoRatioControllerUtilsImpl.this.mStoryCaptureUIError);
        }
    };
    private Runnable retriveOnceListRunnable = new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.18
        @Override // java.lang.Runnable
        public void run() {
            VivoRatioControllerUtilsImpl.this.retriveVivoSignaturesAppList();
        }
    };
    private final ArrayList<String> mOtherGMSApp = new ArrayList<String>() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.19
        {
            add("com.android.vending");
            add("com.android.chrome");
        }
    };
    private final String mGMSAppPrefix = "com.google.android";

    /* loaded from: classes.dex */
    public interface OnRatioChangeListener {
        void onRatioChange();
    }

    static {
        isTierProduct = FtBuild.getTierLevel() != 0;
    }

    public boolean getIs18to9() {
        return this.is18to9;
    }

    public VivoRatioControllerUtilsImpl() {
        VSlog.d(TAG, "CONSTURE of VivoRatioControllerUtilsImpl");
    }

    public static synchronized VivoRatioControllerUtilsImpl getInstance() {
        VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl;
        synchronized (VivoRatioControllerUtilsImpl.class) {
            if (sInstance == null) {
                sInstance = new VivoRatioControllerUtilsImpl();
            }
            vivoRatioControllerUtilsImpl = sInstance;
        }
        return vivoRatioControllerUtilsImpl;
    }

    public void init(Context context, Handler handler, IWindowManager wm, boolean isDefaultDisplay) {
        this.mContext = context;
        this.mHandler = handler;
        this.is18to9 = is18to9();
        this.mIsDefaultDisplay = isDefaultDisplay;
        this.mWindowManager = wm;
        this.mPackageManager = this.mContext.getPackageManager();
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        initDeviceInfo();
        if (CURVED_SCREEN_SUPPORT) {
            this.mIsDefaultCurveCut = "0".equals(FtFeature.getFeatureAttribute("vivo.hardware.curvedscreen", "is_defaultFullScreen", "1"));
        }
        if (this.mIsDefaultDisplay) {
            if (DEBUG) {
                String str = TAG;
                VSlog.d(str, "before: " + SystemClock.elapsedRealtime());
            }
            this.userModifiedPkgMapForRatio = getUserModifiedPackageMapFromSettings(this.mContext, 0);
            userModifiedPkgMapForCutout = getUserModifiedPackageMapFromSettings(this.mContext, 1);
            mCropWidth = context.getResources().getInteger(51052548);
            userModifiedPkgMapForCurvedCutout = getUserModifiedPackageMapFromSettings(this.mContext, 2);
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("middle: ");
                sb.append(SystemClock.elapsedRealtime());
                sb.append(",userModifiedPkgMapForRatio size: ");
                Map<String, Integer> map = this.userModifiedPkgMapForRatio;
                sb.append(map != null ? Integer.valueOf(map.size()) : "null");
                VSlog.d(str2, sb.toString());
            }
            this.mSettingsObserver = new SettingsObserver(this.mHandler);
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    VivoRatioControllerUtilsImpl.this.startObserver();
                }
            });
        }
        this.mVivoWindowPolicyController = VivoWindowPolicyControllerImpl.getInstance(context);
        this.mVivoListCenter = new VivoListCenter(context);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startObserver() {
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Settings.Secure.getUriFor(FULL_SCREEN_APP_MODIFIED_BY_USER), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor(CUTOUT_SETTINGS_MODIFIED_BY_USER), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor(NAVIGATION_GESTURE_ON), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor(CURVEDCUTOUT_APP_MODIFIED_BY_USER), false, this.mSettingsObserver, -1);
        this.sBackFullScreenIsOn = Settings.Secure.getInt(this.mContext.getContentResolver(), "back_full_screen_hold_enabled", 0) == 1;
        this.mUserSettingBackScreenMap = getUserSettingBackScreenMap(this.mContext);
        if (DEBUG) {
            String str = TAG_MULTI;
            StringBuilder sb = new StringBuilder();
            sb.append("middle2: ");
            sb.append(SystemClock.elapsedRealtime());
            sb.append(",userModifiedPkgMapForRatio size: ");
            Map<String, Integer> map = this.userModifiedPkgMapForRatio;
            sb.append(map != null ? Integer.valueOf(map.size()) : "null");
            VSlog.d(str, sb.toString());
        }
        resolver.registerContentObserver(Settings.Secure.getUriFor("modified_back_screen_display_apps"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor("back_full_screen_hold_enabled"), false, this.mSettingsObserver, -1);
        IntentFilter ratioFilter = new IntentFilter();
        ratioFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        ratioFilter.addAction("android.intent.action.LOCALE_CHANGED");
        ratioFilter.addAction("com.vivo.settings.update_database_finish");
        ratioFilter.addAction("com.vivo.daemonService.unifiedconfig.update_finish_broadcast_MultiDisplayAdapter");
        ratioFilter.addAction("android.intent.action.BOOT_COMPLETED");
        ratioFilter.addAction("android.intent.action.USER_SWITCHED");
        try {
            this.mContext.unregisterReceiver(this.mIntentReceiver);
        } catch (Exception e) {
            VSlog.d(TAG, "StartObserver unregisterReceiver err!");
        }
        this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, ratioFilter, null, this.mHandler);
    }

    private void stopObserver() {
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.unregisterContentObserver(this.mSettingsObserver);
        this.mContext.unregisterReceiver(this.mIntentReceiver);
    }

    public void systemBooted() {
        if (MultiDisplayManager.isMultiDisplay) {
            this.mIoHandler.post(this.retriveOnceListRunnable);
        }
        this.mIoHandler.post(this.retriveListRunnable);
    }

    private void initDeviceInfo() {
        float ratio;
        DisplayMetrics metrics = new DisplayMetrics();
        DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
        boolean z = false;
        displayManager.getDisplay(0).getMetrics(metrics);
        Display display = displayManager.getDisplay(4096);
        if (display != null) {
            this.isDualDisplay = true;
        }
        float width = metrics.widthPixels;
        float height = metrics.heightPixels;
        if (width != 0.0f && height != 0.0f && width < height) {
            ratio = height / width;
        } else {
            ratio = width / height;
        }
        float ratio2 = Math.round(ratio * 100.0f) / 100.0f;
        this.sDeviceHeightPixel = height;
        this.sDeviceWidthPixel = width;
        this.sDeviceRatio = ratio2;
        if (DEBUG) {
            VSlog.d(TAG, "DEBUG_RATIODIALOG:initDeviceMaxRatio ratio :" + ratio2);
        }
        SystemProperties.set(PROP_DEVICE_MAXRATIO, Float.toString(ratio2));
        float density = metrics.density;
        this.sDeviceDensity = density;
        this.statusBarHeight = this.mContext.getResources().getDimensionPixelSize(17105488);
        this.navBarHeight = this.mContext.getResources().getDimensionPixelSize(17105334);
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(51118207);
        this.alienScreenCoverInsetBottom = dimensionPixelSize;
        this.gestrueBarHeight = dimensionPixelSize;
        this.alienScreenCoverInsetLand = (int) (this.alienScreenCoverInsetLand * density);
        this.ratioViewMinHeight = (int) (this.ratioViewMinHeight * density);
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), NAVIGATION_GESTURE_ON, 3) == 3) {
            z = true;
        }
        this.sGestureBarIsOn = z;
        this.sVivoHasNavBar = !SystemProperties.get("qemu.hw.mainkeys", "1").equals("1");
        if (DEBUG) {
            VSlog.d(TAG, "DEBUG_WINDOWPOLICY:initDeviceInfo ratio:" + ratio2 + " density:" + density + " statusBarHeight:" + this.statusBarHeight + " navBarHeight:" + this.navBarHeight + " gestrueBarHeight:" + this.gestrueBarHeight + " alienScreenCoverInsetTop:" + this.alienScreenCoverInsetTop + " alienScreenCoverInsetBottom:" + this.alienScreenCoverInsetBottom + " alienScreenCoverInsetLand:" + this.alienScreenCoverInsetLand + " sGestureBarIsOn:" + this.sGestureBarIsOn);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRatioSettingsChangedLocked() {
        synchronized (this.userModifiedPkgMapForRatio) {
            this.userModifiedPkgMapForRatio.clear();
            this.userModifiedPkgMapForRatio = getUserModifiedPackageMapFromSettings(this.mContext, 0);
            if (DEBUG) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("handleRatioSettingsChangedLocked.userModifiedPkgMapForRatio size:");
                sb.append(this.userModifiedPkgMapForRatio != null ? Integer.valueOf(this.userModifiedPkgMapForRatio.size()) : "null");
                VSlog.d(str, sb.toString());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCutoutSettingsChangedLocked() {
        synchronized (userModifiedPkgMapForCutout) {
            userModifiedPkgMapForCutout.clear();
            userModifiedPkgMapForCutout = getUserModifiedPackageMapFromSettings(this.mContext, 1);
            if (DEBUG) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("handleCutoutSettingsChangedLocked.userModifiedPkgMapForCutout size:");
                sb.append(userModifiedPkgMapForCutout != null ? Integer.valueOf(userModifiedPkgMapForCutout.size()) : "null");
                VSlog.d(str, sb.toString());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCurvedCutoutSettingsChangedLocked() {
        synchronized (userModifiedPkgMapForCurvedCutout) {
            userModifiedPkgMapForCurvedCutout.clear();
            userModifiedPkgMapForCurvedCutout = getUserModifiedPackageMapFromSettings(this.mContext, 2);
            if (DEBUG) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("handleCurvedCutoutSettingsChangedLocked.userModifiedPkgMapForCurvedCutout size:");
                sb.append(userModifiedPkgMapForCurvedCutout != null ? Integer.valueOf(userModifiedPkgMapForCurvedCutout.size()) : "null");
                VSlog.d(str, sb.toString());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            OnRatioChangeListener listener;
            if (!VivoRatioControllerUtilsImpl.this.mNavigationGestureOnUri.equals(uri)) {
                if (VivoRatioControllerUtilsImpl.this.mFullScreenAppUri.equals(uri)) {
                    VivoRatioControllerUtilsImpl.this.handleRatioSettingsChangedLocked();
                    return;
                } else if (VivoRatioControllerUtilsImpl.this.mCutoutSettingsUri.equals(uri)) {
                    VivoRatioControllerUtilsImpl.this.handleCutoutSettingsChangedLocked();
                    return;
                } else if (VivoRatioControllerUtilsImpl.this.mBackScreenDisplayUri.equals(uri)) {
                    VivoRatioControllerUtilsImpl.this.handleSettingsChangedForBackScreenLocked();
                    return;
                } else if (VivoRatioControllerUtilsImpl.this.mCurvedCutoutUri.equals(uri)) {
                    VivoRatioControllerUtilsImpl.this.handleCurvedCutoutSettingsChangedLocked();
                    return;
                } else if (VivoRatioControllerUtilsImpl.this.mBackFullScreenHoldUri.equals(uri)) {
                    int fullScreenValue = Settings.Secure.getInt(VivoRatioControllerUtilsImpl.this.mContext.getContentResolver(), "back_full_screen_hold_enabled", 0);
                    VivoRatioControllerUtilsImpl.this.sBackFullScreenIsOn = fullScreenValue == 1;
                    synchronized (VivoRatioControllerUtilsImpl.mCallbacks) {
                        for (int i = 0; i < VivoRatioControllerUtilsImpl.mCallbacks.size(); i++) {
                            String pkgName = (String) VivoRatioControllerUtilsImpl.mCallbacks.valueAt(i);
                            ApplicationInfo appInfo = null;
                            try {
                                appInfo = VivoRatioControllerUtilsImpl.this.mPackageManager.getApplicationInfo(pkgName, 128);
                            } catch (Exception e) {
                            }
                            if (appInfo != null && !VivoRatioControllerUtilsImpl.this.isVivoApp(appInfo) && (listener = (OnRatioChangeListener) VivoRatioControllerUtilsImpl.mCallbacks.keyAt(i)) != null) {
                                listener.onRatioChange();
                            }
                        }
                    }
                    return;
                } else {
                    return;
                }
            }
            VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl = VivoRatioControllerUtilsImpl.this;
            vivoRatioControllerUtilsImpl.sGestureBarIsOn = Settings.Secure.getInt(vivoRatioControllerUtilsImpl.mContext.getContentResolver(), VivoRatioControllerUtilsImpl.NAVIGATION_GESTURE_ON, 3) == 3;
            if (IVivoRatioControllerUtils.DEBUG) {
                String str = VivoRatioControllerUtilsImpl.TAG;
                VSlog.d(str, "onChange mGestureBarIsOn change to " + VivoRatioControllerUtilsImpl.this.sGestureBarIsOn);
            }
        }
    }

    public String getAlienScreenRatioPolicyForPackage(String pkg) {
        String res = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        synchronized (userModifiedPkgMapForCutout) {
            if (userModifiedPkgMapForCutout.keySet().contains(pkg)) {
                int isFullScreen = userModifiedPkgMapForCutout.get(pkg).intValue();
                if (isFullScreen == 2) {
                    res = "KEEPFULL";
                } else if (isFullScreen == 1) {
                    res = "NOTKEEPFULL";
                } else if (isFullScreen == 0) {
                    res = "AUTOMATCH";
                }
                if (DEBUG) {
                    String str = TAG;
                    VSlog.d(str, pkg + " AlienScreenRatioPolicy keepfull: " + res + ".in userModified.");
                }
            } else if (this.sAdaptAlienScreenList.contains(pkg)) {
                res = "KEEPFULL";
                if (DEBUG) {
                    String str2 = TAG;
                    VSlog.d(str2, pkg + " AlienScreenRatioPolicy keepfull: " + res + ".in sAdaptAlienScreenList.");
                }
            }
        }
        return res;
    }

    public int getRatioPolicyForPackage(String pkg) {
        int res = 0;
        if (pkg == null || pkg.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
            return 0;
        }
        if (this.mVivoListCenter.isSystemOrVivoApp(pkg)) {
            res = 1;
        } else if (this.userModifiedPkgMapForRatio.keySet().contains(pkg)) {
            int isFullScreen = this.userModifiedPkgMapForRatio.get(pkg).intValue();
            if (DEBUG) {
                String str = TAG;
                VSlog.d(str, pkg + " isFullScreen: " + isFullScreen + ".in userModified.");
            }
            res = isFullScreen;
        } else if (this.mVivoListCenter.containsInArrayListTyped(VivoListCenter.VIVO_LIST_TYPE_RATIOCONTROL_WHITELIST, pkg)) {
            if (DEBUG) {
                String str2 = TAG;
                VSlog.d(str2, pkg + " contains in sInternalWhiteListForRatioControl");
            }
            res = 1;
        } else if (this.sWhiteList.contains(pkg) || this.sAdaptAlienScreenList.contains(pkg)) {
            if (DEBUG) {
                String str3 = TAG;
                VSlog.d(str3, pkg + " isFullScreen: contains in sWhiteList.");
            }
            res = 1;
        }
        if (this.sBlackList.contains(pkg) && res == 0) {
            if (DEBUG) {
                String str4 = TAG;
                VSlog.d(str4, pkg + " isFullScreen: contains in sBlackList. NORMALIZE instead of APPSETTING");
            }
            res = 2;
        }
        if (DEBUG) {
            String str5 = TAG;
            VSlog.d(str5, "getRatioPolicyForPackage for " + pkg + " return " + res);
        }
        return res;
    }

    public int getCurrentMaxRestrictHeight() {
        boolean z = !SystemProperties.get("qemu.hw.mainkeys", "1").equals("1");
        this.sVivoHasNavBar = z;
        int res = (int) this.sDeviceHeightPixel;
        if (z && !this.sGestureBarIsOn) {
            res -= this.navBarHeight;
        }
        if (this.sGestureBarIsOn && !this.sVivoHasNavBar) {
            return res - this.gestrueBarHeight;
        }
        return res;
    }

    /* JADX WARN: Code restructure failed: missing block: B:40:0x00a8, code lost:
        if ((r3.flags & 1) != 0) goto L47;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x00ac, code lost:
        if ((r6 & com.android.server.am.EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP) != 0) goto L47;
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x00ae, code lost:
        r7 = new android.view.DisplayInfo(r14);
        r7.logicalWidth = r14.appWidth;
        r7.logicalHeight = r14.appHeight;
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x00bd, code lost:
        if (com.android.server.policy.VivoRatioControllerUtilsImpl.DEBUG == false) goto L39;
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x00bf, code lost:
        vivo.util.VSlog.d(com.android.server.policy.VivoRatioControllerUtilsImpl.TAG, "DEBUG_WINDOWPOLICY:getRestrictedDisplayInfo adjust realSize: " + r7);
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x00d5, code lost:
        return r7;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public android.view.DisplayInfo getRestrictedDisplayInfo(android.content.Context r12, int r13, android.view.DisplayInfo r14) {
        /*
            r11 = this;
            boolean r0 = com.android.server.policy.VivoRatioControllerUtilsImpl.EAR_PHONE_SUPPORT
            if (r0 != 0) goto L5
            return r14
        L5:
            int r0 = r14.appWidth
            int r1 = r14.appHeight
            if (r0 >= r1) goto Lc
            return r14
        Lc:
            android.content.pm.PackageManager r0 = r12.getPackageManager()     // Catch: java.lang.Exception -> Ld6
            java.lang.String[] r0 = r0.getPackagesForUid(r13)     // Catch: java.lang.Exception -> Ld6
            if (r0 == 0) goto Ld7
            int r1 = r0.length     // Catch: java.lang.Exception -> Ld6
            if (r1 <= 0) goto Ld7
            r1 = 0
            r2 = r0[r1]     // Catch: java.lang.Exception -> Ld6
            android.content.pm.PackageManager r3 = r12.getPackageManager()     // Catch: java.lang.Exception -> Ld6
            android.content.pm.ApplicationInfo r3 = r3.getApplicationInfo(r2, r1)     // Catch: java.lang.Exception -> Ld6
            com.android.server.wm.VivoWindowPolicyControllerImpl r4 = r11.mVivoWindowPolicyController     // Catch: java.lang.Exception -> Ld6
            java.lang.String r5 = ""
            if (r4 == 0) goto L31
            com.android.server.wm.VivoWindowPolicyControllerImpl r4 = r11.mVivoWindowPolicyController     // Catch: java.lang.Exception -> Ld6
            java.lang.String r4 = r4.getInternalFlag(r2)     // Catch: java.lang.Exception -> Ld6
            goto L32
        L31:
            r4 = r5
        L32:
            r6 = 0
            if (r4 == 0) goto L76
            boolean r7 = r4.equals(r5)     // Catch: java.lang.Exception -> Ld6
            if (r7 != 0) goto L76
            java.lang.Integer r7 = java.lang.Integer.decode(r4)     // Catch: java.lang.Exception -> Ld6
            int r7 = r7.intValue()     // Catch: java.lang.Exception -> Ld6
            r6 = r7
            boolean r8 = com.android.server.policy.VivoRatioControllerUtilsImpl.DEBUG     // Catch: java.lang.Exception -> Ld6
            if (r8 == 0) goto L76
            java.lang.String r8 = com.android.server.policy.VivoRatioControllerUtilsImpl.TAG     // Catch: java.lang.Exception -> Ld6
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> Ld6
            r9.<init>()     // Catch: java.lang.Exception -> Ld6
            java.lang.String r10 = "DEBUG_WINDOWPOLICY:getRestrictedDisplayInfo internalFlag="
            r9.append(r10)     // Catch: java.lang.Exception -> Ld6
            r9.append(r4)     // Catch: java.lang.Exception -> Ld6
            java.lang.String r10 = " decode to :"
            r9.append(r10)     // Catch: java.lang.Exception -> Ld6
            r9.append(r7)     // Catch: java.lang.Exception -> Ld6
            java.lang.String r10 = " , then iInternalFlag="
            r9.append(r10)     // Catch: java.lang.Exception -> Ld6
            r9.append(r6)     // Catch: java.lang.Exception -> Ld6
            java.lang.String r10 = " packageName="
            r9.append(r10)     // Catch: java.lang.Exception -> Ld6
            r9.append(r2)     // Catch: java.lang.Exception -> Ld6
            java.lang.String r9 = r9.toString()     // Catch: java.lang.Exception -> Ld6
            vivo.util.VSlog.d(r8, r9)     // Catch: java.lang.Exception -> Ld6
        L76:
            com.android.server.wm.VivoWindowPolicyControllerImpl r7 = r11.mVivoWindowPolicyController     // Catch: java.lang.Exception -> Ld6
            if (r7 == 0) goto L81
            com.android.server.wm.VivoWindowPolicyControllerImpl r5 = r11.mVivoWindowPolicyController     // Catch: java.lang.Exception -> Ld6
            java.lang.String r5 = r5.getPolicyAlienScreen(r2)     // Catch: java.lang.Exception -> Ld6
            goto L82
        L81:
        L82:
            java.lang.String r7 = "KEEPFULL"
            boolean r7 = r5.equals(r7)     // Catch: java.lang.Exception -> Ld6
            r8 = 1
            if (r7 == 0) goto L8d
            r1 = r8
            goto La2
        L8d:
            java.lang.String r7 = "NOTKEEPFULL"
            boolean r7 = r5.equals(r7)     // Catch: java.lang.Exception -> Ld6
            if (r7 == 0) goto L96
            goto La2
        L96:
            android.os.Bundle r7 = r3.metaData     // Catch: java.lang.Exception -> Ld6
            if (r7 == 0) goto La2
            android.os.Bundle r7 = r3.metaData     // Catch: java.lang.Exception -> Ld6
            java.lang.String r9 = "android.vendor.full_screen"
            boolean r1 = r7.getBoolean(r9, r1)     // Catch: java.lang.Exception -> Ld6
        La2:
            if (r1 != 0) goto Ld7
            int r7 = r3.flags     // Catch: java.lang.Exception -> Ld6
            r7 = r7 & r8
            if (r7 != 0) goto Ld7
            r7 = r6 & 8192(0x2000, float:1.14794E-41)
            if (r7 != 0) goto Ld7
            android.view.DisplayInfo r7 = new android.view.DisplayInfo     // Catch: java.lang.Exception -> Ld6
            r7.<init>(r14)     // Catch: java.lang.Exception -> Ld6
            int r8 = r14.appWidth     // Catch: java.lang.Exception -> Ld6
            r7.logicalWidth = r8     // Catch: java.lang.Exception -> Ld6
            int r8 = r14.appHeight     // Catch: java.lang.Exception -> Ld6
            r7.logicalHeight = r8     // Catch: java.lang.Exception -> Ld6
            boolean r8 = com.android.server.policy.VivoRatioControllerUtilsImpl.DEBUG     // Catch: java.lang.Exception -> Ld6
            if (r8 == 0) goto Ld5
            java.lang.String r8 = com.android.server.policy.VivoRatioControllerUtilsImpl.TAG     // Catch: java.lang.Exception -> Ld6
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> Ld6
            r9.<init>()     // Catch: java.lang.Exception -> Ld6
            java.lang.String r10 = "DEBUG_WINDOWPOLICY:getRestrictedDisplayInfo adjust realSize: "
            r9.append(r10)     // Catch: java.lang.Exception -> Ld6
            r9.append(r7)     // Catch: java.lang.Exception -> Ld6
            java.lang.String r9 = r9.toString()     // Catch: java.lang.Exception -> Ld6
            vivo.util.VSlog.d(r8, r9)     // Catch: java.lang.Exception -> Ld6
        Ld5:
            return r7
        Ld6:
            r0 = move-exception
        Ld7:
            return r14
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.VivoRatioControllerUtilsImpl.getRestrictedDisplayInfo(android.content.Context, int, android.view.DisplayInfo):android.view.DisplayInfo");
    }

    public DisplayInfo getRestrictedDisplayInfo(Context context, int pid, int uid, DisplayInfo info) {
        if (!EAR_PHONE_SUPPORT) {
            return info;
        }
        if (info.appWidth < info.appHeight) {
            return info;
        }
        if (uid < 10000) {
            return info;
        }
        ApplicationInfo appInfo = getApplicationInfoForPid(context, pid, uid);
        if (appInfo != null) {
            VivoWindowPolicyControllerImpl vivoWindowPolicyControllerImpl = this.mVivoWindowPolicyController;
            String listFullScreen = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            String internalFlag = vivoWindowPolicyControllerImpl != null ? vivoWindowPolicyControllerImpl.getInternalFlag(appInfo.packageName) : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            int iInternalFlag = 0;
            if (internalFlag != null && !internalFlag.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                int internalFlagInt = Integer.decode(internalFlag).intValue();
                iInternalFlag = internalFlagInt;
                if (DEBUG) {
                    VSlog.d(TAG, "DEBUG_WINDOWPOLICY:getRestrictedDisplayInfo internalFlag=" + internalFlag + " decode to :" + internalFlagInt + " , then iInternalFlag=" + iInternalFlag + " packageName=" + appInfo.packageName);
                }
            }
            VivoWindowPolicyControllerImpl vivoWindowPolicyControllerImpl2 = this.mVivoWindowPolicyController;
            if (vivoWindowPolicyControllerImpl2 != null) {
                listFullScreen = vivoWindowPolicyControllerImpl2.getPolicyAlienScreen(appInfo.packageName);
            }
            boolean z = false;
            if (listFullScreen.equals("KEEPFULL")) {
                z = true;
            } else if (!listFullScreen.equals("NOTKEEPFULL") && appInfo.metaData != null) {
                z = appInfo.metaData.getBoolean("android.vendor.full_screen", false);
            }
            boolean keepFullscreen = z;
            if (!keepFullscreen && (1 & appInfo.flags) == 0 && (iInternalFlag & EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP) == 0) {
                DisplayInfo newInfo = new DisplayInfo(info);
                newInfo.logicalWidth = info.appWidth;
                newInfo.logicalHeight = info.appHeight;
                if (PhoneWindowManager.DEBUG_KEYGUARD) {
                    VSlog.d(TAG, "DEBUG_WINDOWPOLICY:getRestrictedDisplayInfo adjust realSize: " + newInfo + ", pkg = " + appInfo.packageName + ", callingUid = " + uid);
                }
                return newInfo;
            }
        }
        return info;
    }

    private ApplicationInfo getApplicationInfoForPid(Context context, int pid, int uid) {
        ApplicationInfo appInfo;
        synchronized (this.mApplicationInfos) {
            appInfo = this.mApplicationInfos.get(pid);
        }
        if (appInfo == null && !isSystemServer(pid)) {
            String str = TAG;
            VSlog.w(str, "getApplicationInfoForPid return null : PID = " + pid + " , uid = " + uid, new RuntimeException("here").fillInStackTrace());
        }
        return appInfo;
    }

    public DisplayInfo getLetterboxedDisplayInfo(int displayId, Context context, int pid, int uid, DisplayInfo info) {
        if (!ENABLE_SECONDARY_DISPLAY_LETTERBOX) {
            return info;
        }
        return info;
    }

    public int adjustDisplayIdForPid(Context context, int displayId, int pid, int uid) {
        if (!MultiDisplayManager.isBuiltInDisplayId(displayId)) {
            return displayId;
        }
        ApplicationInfo appInfo = getApplicationInfoForPid(context, pid, uid);
        if (appInfo != null && ((!isVivoApp(appInfo) && !isAdaptedThirdPartApplication(appInfo)) || isInputMethodProcess(pid))) {
            synchronized (mDisplayIdForPids) {
                Integer runningDisplayId = mDisplayIdForPids.get(pid);
                if (runningDisplayId != null) {
                    int runningDisplayIdInt = runningDisplayId.intValue();
                    if (runningDisplayIdInt != displayId) {
                        String str = TAG;
                        VSlog.i(str, "adjustDisplayIdForPid, pid = " + pid + ", runningDisplayId = " + runningDisplayId + ", displayId = " + displayId + " ; packageName = " + appInfo.packageName);
                    }
                    return (runningDisplayIdInt == displayId || !MultiDisplayManager.isBuiltInDisplayId(runningDisplayIdInt)) ? displayId : runningDisplayIdInt;
                }
            }
        }
        return displayId;
    }

    private boolean isAdaptedThirdPartApplication(ApplicationInfo appInfo) {
        if (this.ALL_ADAPTER) {
            return true;
        }
        if (appInfo == null) {
            VSlog.d(TAG_MULTI, " isAdaptedThirdPartApplication : return false for NULL");
            return false;
        }
        if (DEBUG) {
            String str = TAG_MULTI;
            VSlog.d(str, "isAdaptedThirdPartApplication:: " + appInfo.packageName + " ; sAdaptedBackScreenList.contains : " + this.sAdaptedBackScreenList.contains(appInfo.packageName) + " ; checkMetaData(appInfo) = " + checkMetaData(appInfo));
        }
        return this.sAdaptedBackScreenList.contains(appInfo.packageName) && checkMetaData(appInfo) && !isInAdaptedBackScreenBlackMap(appInfo);
    }

    private boolean checkMetaData(ApplicationInfo appInfo) {
        boolean multidisplaySupport = false;
        if (DEBUG && appInfo != null) {
            String str = TAG_MULTI;
            VSlog.d(str, "checkMetaData , appInfo.metaData = " + appInfo.metaData + " ; packageName = " + appInfo.packageName);
        }
        if (appInfo != null && appInfo.metaData != null) {
            multidisplaySupport = appInfo.metaData.getBoolean("android.vivo_multidisplay_support", false);
            if (DEBUG) {
                String str2 = TAG_MULTI;
                VSlog.d(str2, "checkMetaData , multidisplaySupport = " + multidisplaySupport + " ; packageName = " + appInfo.packageName);
            }
        }
        return multidisplaySupport;
    }

    private boolean isInAdaptedBackScreenBlackMap(ApplicationInfo appInfo) {
        if (this.mAdaptedBackScreenBlackMap.keySet().contains(appInfo.packageName)) {
            int versionCode = appInfo.versionCode;
            String sVersionCode = String.valueOf(versionCode);
            ArrayList<String> versionList = this.mAdaptedBackScreenBlackMap.get(appInfo.packageName);
            if (DEBUG) {
                VSlog.d(TAG_MULTI, "isInAdaptedBackScreenBlackMap : contains " + appInfo.packageName + " ; versionCode = " + versionCode + " ; versionList = " + versionList);
            }
            if (versionList.contains(sVersionCode)) {
                if (DEBUG) {
                    VSlog.d(TAG_MULTI, "isInAdaptedBackScreenBlackMap true,  sVersionCode = " + sVersionCode + " ; packageName = " + appInfo.packageName);
                }
                return true;
            }
            Iterator<String> it = versionList.iterator();
            while (it.hasNext()) {
                String version = it.next();
                int indexPlus = version.indexOf("+");
                if (indexPlus >= 0) {
                    String sVersionNoPlus = version.substring(0, indexPlus) + version.substring(indexPlus + 1);
                    if (versionCode >= Integer.parseInt(sVersionNoPlus)) {
                        if (DEBUG) {
                            VSlog.d(TAG_MULTI, "isInAdaptedBackScreenBlackMap true,  versionCode = " + versionCode + " ; sVersionNoPlus = " + sVersionNoPlus + " ; packageName = " + appInfo.packageName);
                        }
                        return true;
                    }
                }
            }
        }
        if (DEBUG) {
            VSlog.d(TAG_MULTI, "isInAdaptedBackScreenBlackMap RETURN false for " + appInfo.packageName);
        }
        return false;
    }

    private boolean killInputMethodProcessIfNeeded(int pid, int uid, String reason) {
        ApplicationInfo appInfo;
        synchronized (this.mApplicationInfos) {
            appInfo = this.mApplicationInfos.get(pid);
        }
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("killInputMethodProcessIfNeeded, pid = ");
        sb.append(pid);
        sb.append(", appInfo = ");
        sb.append(appInfo != null ? appInfo.packageName : null);
        sb.append(" , current pid = ");
        sb.append(Process.myPid());
        sb.append(", isAdaptedApplication = ");
        sb.append(isVivoApp(appInfo) || isAdaptedThirdPartApplication(appInfo));
        sb.append(", reason = ");
        sb.append(reason);
        VSlog.i(str, sb.toString());
        if (appInfo == null || (!isVivoApp(appInfo) && !isAdaptedThirdPartApplication(appInfo) && !appInfo.packageName.contains("mockime"))) {
            int puid = Process.getUidForPid(pid);
            if (pid != Process.myPid() && puid == uid) {
                String str2 = TAG_MULTI;
                VSlog.d(str2, "real do it!  pid = " + pid + " , uid = " + uid + " , puid = " + puid);
                Process.killProcess(pid);
                return true;
            }
        }
        return false;
    }

    public void setCurrentInputMethod(ComponentName component) {
        String str = TAG;
        VSlog.i(str, "setCurrentInputMethod, component = " + component);
        synchronized (mDisplayIdForPids) {
            if (component != null) {
                if (!component.equals(this.sCurrentInputMethod)) {
                    this.mInputMethodChange = true;
                    this.sCurrentInputMethod = component;
                    if (!"com.vivo.secime.service".equals(component.getPackageName())) {
                        this.sNoneSecureInputMethod = component;
                    }
                }
            }
        }
    }

    public void handleProcessStarted(int pid, int uid, int focusDisplayId, ComponentName componentName) {
        synchronized (mDisplayIdForPids) {
            if (componentName != null) {
                if (this.sCurrentInputMethod != null && componentName.equals(this.sCurrentInputMethod)) {
                    String str = TAG;
                    VSlog.i(str, "handleProcessStarted, input method pid = " + pid + ", componentName = " + componentName + " , uid = " + uid);
                    this.sCurrentInputMethodPid = pid;
                    if (!componentName.getPackageName().equals("com.vivo.secime.service")) {
                        this.sNoneSecureInputMethodPid = pid;
                    }
                    this.sCurrentInputMethodUid = uid;
                    addDisplayId(pid, uid, focusDisplayId, "startIME");
                }
            }
        }
    }

    public void handleInputMethodProcessMoved(int focusDisplayId) {
        String str = TAG;
        VSlog.i(str, "handleInputMethodProcessMoved, input method pid = " + this.sCurrentInputMethodPid + ", focusDisplayId = " + focusDisplayId);
        addDisplayId(this.sCurrentInputMethodPid, this.sCurrentInputMethodUid, focusDisplayId, "moveIME");
    }

    public boolean isInputMethodProcess(int pid) {
        boolean z;
        synchronized (mDisplayIdForPids) {
            z = mDisplayIdForInputMethod.get(pid) != null;
        }
        return z;
    }

    public void addDisplayId(int pid, int displayId, String reason) {
        addDisplayId(pid, 0, displayId, reason);
    }

    public void addDisplayId(int pid, int uid, int displayId, String reason) {
        if (pid <= 0 || displayId < 0) {
            return;
        }
        String str = TAG_MULTI;
        VSlog.i(str, "addDisplayId, pid = " + pid + ", displayId = " + displayId + ", reason = " + reason + " , uid = " + uid);
        synchronized (mDisplayIdForPids) {
            if ("moveIME".equals(reason) || "startIME".equals(reason)) {
                String str2 = TAG;
                VSlog.i(str2, "addDisplayId input method, pid = " + pid + ", displayId = " + displayId + ", reason = " + reason + ", caller = " + Debug.getCallers(3));
                if (isSystemServer(pid)) {
                    String str3 = TAG;
                    VSlog.e(str3, "addDisplayId input method, pid = " + pid + " is System Server!");
                    return;
                }
                if (mDisplayIdForInputMethod.get(pid) != null) {
                    int oldDisplayId = mDisplayIdForInputMethod.get(pid).intValue();
                    if (oldDisplayId != displayId && ((oldDisplayId == 4096 || displayId == 4096) && killInputMethodProcessIfNeeded(pid, uid, reason))) {
                        return;
                    }
                } else if (this.mInputMethodChange && this.isDualDisplay) {
                    this.mInputMethodChange = false;
                    if (killInputMethodProcessIfNeeded(pid, uid, reason)) {
                        return;
                    }
                }
                mDisplayIdForInputMethod.put(pid, new Integer(displayId));
                mUidForInputMethod.put(pid, Integer.valueOf(uid));
            }
            mDisplayIdForPids.put(pid, new Integer(displayId));
        }
    }

    public void removeDisplayId(int pid) {
        synchronized (mDisplayIdForPids) {
            if (this.sNoneSecureInputMethodPid == pid) {
                this.sNoneSecureInputMethodPid = 0;
            }
        }
    }

    public void displayLetterBoxChanged() {
        synchronized (mDisplayIdForPids) {
            for (int i = 0; i < mDisplayIdForInputMethod.size(); i++) {
                int pid = mDisplayIdForInputMethod.keyAt(i);
                int displayId = mDisplayIdForInputMethod.valueAt(i).intValue();
                if (displayId == 4096) {
                    killInputMethodProcessIfNeeded(pid, mUidForInputMethod.valueAt(i).intValue(), "letterBoxChanged");
                }
            }
        }
    }

    public boolean isInputMethodPackageName(ComponentName componentName) {
        boolean z;
        synchronized (mDisplayIdForPids) {
            if (componentName != null) {
                try {
                    z = (this.sCurrentInputMethod != null && componentName.equals(this.sCurrentInputMethod)) ? true : true;
                } finally {
                }
            }
            z = false;
        }
        return z;
    }

    public String getCurrentInputMethodPackageName() {
        String packageName;
        synchronized (mDisplayIdForPids) {
            packageName = this.sCurrentInputMethod != null ? this.sCurrentInputMethod.getPackageName() : null;
        }
        return packageName;
    }

    public String getCurrentNoneSecureInputMethodPackageName() {
        String packageName;
        synchronized (mDisplayIdForPids) {
            packageName = this.sNoneSecureInputMethod != null ? this.sNoneSecureInputMethod.getPackageName() : null;
        }
        return packageName;
    }

    public boolean isRunningInputMethodAll(int pid) {
        boolean z;
        synchronized (mDisplayIdForPids) {
            z = this.sCurrentInputMethodPid == pid || this.sNoneSecureInputMethodPid == pid;
        }
        return z;
    }

    public boolean isCurrentInputMethodDisplayId(int pid, int displayId) {
        Integer runningDisplayId;
        synchronized (mDisplayIdForPids) {
            boolean z = true;
            if (this.sCurrentInputMethodPid == pid) {
                Integer runningDisplayId2 = mDisplayIdForPids.get(pid);
                if (runningDisplayId2 != null) {
                    if (runningDisplayId2.intValue() != displayId) {
                        z = false;
                    }
                    return z;
                }
            } else if (this.sNoneSecureInputMethodPid == pid && (runningDisplayId = mDisplayIdForPids.get(pid)) != null) {
                if (runningDisplayId.intValue() != displayId) {
                    z = false;
                }
                return z;
            }
            return false;
        }
    }

    public int adjustDisplayIdForIME(Context context, int displayId, int pid, int uid) {
        ApplicationInfo appInfo;
        if (!isSystemServer(pid) && isInputMethodProcess(pid) && (appInfo = getApplicationInfoForPid(context, pid, uid)) != null) {
            synchronized (mDisplayIdForPids) {
                Integer runningDisplayId = mDisplayIdForPids.get(pid);
                if (runningDisplayId != null) {
                    int runningDisplayIdInt = runningDisplayId.intValue();
                    if (runningDisplayIdInt != displayId) {
                        String str = TAG;
                        VSlog.i(str, "adjustDisplayIdForIME, pid = " + pid + ", runningDisplayId = " + runningDisplayId + ", displayId = " + displayId + " ; packageName = " + appInfo.packageName);
                    }
                    return runningDisplayIdInt;
                }
            }
        }
        return displayId;
    }

    public void handleProcessStartedLocked(String processName, int pid, String processType, String hostName) {
        String hostName2;
        ComponentName componentName;
        if (pid <= 0 || processType == null || !processType.equals(VivoFirewall.TYPE_SERVICE) || hostName == null || hostName.length() <= 0 || (hostName2 = hostName.replace("{", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).replace("}", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) == null || (componentName = ComponentName.unflattenFromString(hostName2)) == null || this.sCurrentInputMethod == null) {
            return;
        }
        synchronized (mDisplayIdForPids) {
            if (componentName != null) {
                if (this.sCurrentInputMethod != null && componentName.equals(this.sCurrentInputMethod)) {
                    String str = TAG;
                    VSlog.i(str, "handleProcessStartedLocked input method pid = " + pid + ", componentName = " + componentName);
                    this.sCurrentInputMethodPid = pid;
                    if (!componentName.getPackageName().equals("com.vivo.secime.service")) {
                        this.sNoneSecureInputMethodPid = pid;
                    }
                }
            }
        }
    }

    public void addApplicationInfo(int pid, ApplicationInfo appInfo) {
        synchronized (this.mApplicationInfos) {
            String str = TAG;
            VSlog.i(str, "addApplicationInfo pid = " + pid + ", appInfo = " + appInfo);
            this.mApplicationInfos.put(pid, appInfo);
        }
    }

    public void removeApplicationInfo(int pid) {
        synchronized (this.mApplicationInfos) {
            ApplicationInfo appInfo = (ApplicationInfo) this.mApplicationInfos.removeReturnOld(pid);
            String str = TAG;
            VSlog.i(str, "removeApplicationInfo, pid = " + pid + ", appInfo = " + appInfo);
        }
    }

    private ArrayList<String> getAbnormalPackageListFromSettings(Context context) {
        ArrayList<String> pkgList = new ArrayList<>();
        if (context == null) {
            return pkgList;
        }
        String settingsString = Settings.Secure.getString(context.getContentResolver(), FULL_SCREEN_APP_RUNNING_ABNORMALLY);
        if (TextUtils.isEmpty(settingsString)) {
            return pkgList;
        }
        String[] tempString = settingsString.replace(" ", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).split(":");
        if (tempString.length == 0) {
            return pkgList;
        }
        for (String pkg : tempString) {
            if (pkg.length() > 0 && !pkgList.contains(pkg)) {
                pkgList.add(pkg);
            }
        }
        if (DEBUG) {
            VSlog.d(TAG, "getAbnormalPackageListFromSettings: size is " + pkgList.size());
        }
        return pkgList;
    }

    /* JADX WARN: Removed duplicated region for block: B:117:0x01f2 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:89:0x01a8  */
    /* JADX WARN: Removed duplicated region for block: B:94:0x01c8  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private android.util.ArrayMap<java.lang.String, java.lang.Integer> getUserModifiedPackageMapFromSettings(android.content.Context r19, int r20) {
        /*
            Method dump skipped, instructions count: 541
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.VivoRatioControllerUtilsImpl.getUserModifiedPackageMapFromSettings(android.content.Context, int):android.util.ArrayMap");
    }

    public void updateUserModifiedSettings(final Context context, final String pkg, final boolean status) {
        AsyncTask.execute(new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.3
            @Override // java.lang.Runnable
            public void run() {
                if (context == null || TextUtils.isEmpty(pkg)) {
                    return;
                }
                String value = status ? "1" : "0";
                String tempStr = pkg + ",";
                String userModifiedString = Settings.Secure.getStringForUser(context.getContentResolver(), VivoRatioControllerUtilsImpl.FULL_SCREEN_APP_MODIFIED_BY_USER, -2);
                if (TextUtils.isEmpty(userModifiedString)) {
                    userModifiedString = ":" + tempStr + value + ":";
                } else {
                    if (!userModifiedString.contains(":" + tempStr)) {
                        userModifiedString = userModifiedString + tempStr + value + ":";
                    } else {
                        try {
                            Pattern p = Pattern.compile(":" + pkg + ",[0,1]:");
                            Matcher matcher = p.matcher(userModifiedString);
                            userModifiedString = matcher.replaceAll(":" + tempStr + value + ":");
                        } catch (PatternSyntaxException e) {
                            VLog.e(VivoRatioControllerUtilsImpl.TAG, "updateUserModifiedSettings: " + e);
                        }
                    }
                }
                if (IVivoRatioControllerUtils.DEBUG) {
                    VSlog.d(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIODIALOG:userModifiedString=" + userModifiedString);
                }
                VivoRatioControllerUtilsImpl.this.setUserModifiedPackageMapToSettings(context, userModifiedString);
            }
        });
    }

    public void setUserModifiedPackageMapToSettings(Context context, String settingsStr) {
        Settings.Secure.putStringForUser(context.getContentResolver(), FULL_SCREEN_APP_MODIFIED_BY_USER, settingsStr, -2);
    }

    private boolean is18to9() {
        DisplayMetrics metrics = new DisplayMetrics();
        DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
        displayManager.getDisplay(0).getMetrics(metrics);
        float width = metrics.widthPixels;
        float height = metrics.heightPixels;
        return (width == 0.0f || height == 0.0f || width >= height) ? ((double) (width / height)) > 1.78d : ((double) (height / width)) > 1.78d;
    }

    public int getNavigationBarHeight() {
        Resources res = this.mContext.getResources();
        getRatioRestricHeight();
        int navHeight = res.getDimensionPixelSize(17105334);
        if (DEBUG) {
            String str = TAG;
            VSlog.d(str, "getNavigationBarHeight navHeight:" + navHeight);
        }
        return navHeight;
    }

    public int getRatioRestricHeight() {
        int i = this.mCurrentRestricHeight;
        if (i < 0 || !isValidHeight(i)) {
            DisplayMetrics metrics = new DisplayMetrics();
            DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
            displayManager.getDisplay(0).getMetrics(metrics);
            int width = Math.round(metrics.widthPixels);
            int height = Math.round(metrics.heightPixels);
            if (width > height) {
                this.mCurrentRestricHeight = getStandardRatioHeight(height, width);
            } else {
                this.mCurrentRestricHeight = getStandardRatioHeight(width, height);
            }
        }
        if (DEBUG) {
            String str = TAG;
            VSlog.d(str, "getRatioRestricHeight mCurrentRestricHeight:" + this.mCurrentRestricHeight);
        }
        return this.mCurrentRestricHeight;
    }

    public int getShrinkSize(int shortSize, int longSize) {
        return (longSize - getStandardRatioHeight(shortSize, longSize)) - getNavigationBarHeight();
    }

    private boolean isValidHeight(int height) {
        return height == 800 || height == 1280 || height == 1920 || height == 2560;
    }

    public int getStandardRatioHeight(int shortSize, int longSize) {
        if (shortSize != 480) {
            if (shortSize != 720) {
                if (shortSize != 1080) {
                    if (shortSize != 1440) {
                        return longSize;
                    }
                    return 2560;
                }
                return 1920;
            }
            return 1280;
        }
        return 800;
    }

    public void dismissRatioSwitchDialog() {
        AlertDialog alertDialog = this.mCurrentWarningDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
            this.mCurrentWarningDialog = null;
        }
    }

    public void pendingShowRatioSwitchDialog(boolean fullscreen, String pkg) {
        if (DEBUG) {
            String str = TAG;
            VSlog.d(str, "DEBUG_RATIODIALOG:pendingShowRatioSwitchDialog fullscreen=" + fullscreen + " pkg=" + pkg);
        }
        this.mPendingFlag = true;
        this.mPendingChangeValue = fullscreen;
        this.mPendingChangePkg = pkg;
    }

    public void checkPendingDialog() {
        String str;
        if (this.mPendingFlag && (str = this.mCurrentPkg) != null && str.equals(this.mPendingChangePkg)) {
            showRatioSwitchDialog("pendingSwitch", this.mPendingChangeValue);
        }
    }

    public void showRatioSwitchDialog(final String source, final boolean fullscreen) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.4
            @Override // java.lang.Runnable
            public void run() {
                if (VivoRatioControllerUtilsImpl.this.mCurrentWarningDialog != null) {
                    VivoRatioControllerUtilsImpl.this.mCurrentWarningDialog.dismiss();
                    VivoRatioControllerUtilsImpl.this.mCurrentWarningDialog = null;
                }
                if (IVivoRatioControllerUtils.DEBUG) {
                    VSlog.d(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIODIALOG:showRatioSwitchDialog source=" + source + "fullscreen=" + fullscreen);
                }
                VivoRatioControllerUtilsImpl.this.mDialogChangeValue = fullscreen;
                Display display = VivoRatioControllerUtilsImpl.this.mContext.getDisplay();
                if (display != null && (display.getDisplayId() == 0 || 4096 == display.getDisplayId())) {
                    VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl = VivoRatioControllerUtilsImpl.this;
                    ConfirmDialogListener listener = new ConfirmDialogListener(vivoRatioControllerUtilsImpl.mContext, VivoRatioControllerUtilsImpl.this.mHandler);
                    String paddingString = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    if (VivoRatioControllerUtilsImpl.this.getIsTierProduct()) {
                        VivoRatioControllerUtilsImpl.this.mCurrentWarningDialog = new AlertDialog.Builder(VivoRatioControllerUtilsImpl.this.mContext, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT)).setNegativeButton(17039360, listener).setPositiveButton(17039370, listener).create();
                        paddingString = "\n";
                    } else {
                        VivoRatioControllerUtilsImpl.this.mCurrentWarningDialog = new AlertDialog.Builder(VivoRatioControllerUtilsImpl.this.mContext, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT)).setTitle(51249527).setNegativeButton(17039360, listener).setPositiveButton(17039370, listener).create();
                    }
                    if (!"pendingSwitch".equals(source)) {
                        String path1 = VivoRatioControllerUtilsImpl.this.mContext.getString(51249528);
                        String path2 = VivoRatioControllerUtilsImpl.this.mContext.getString(51249529);
                        String path3 = VivoRatioControllerUtilsImpl.this.isDualDisplay ? VivoRatioControllerUtilsImpl.this.mContext.getString(51249531) : VivoRatioControllerUtilsImpl.this.mContext.getString(51249530);
                        VivoRatioControllerUtilsImpl.this.mCurrentWarningDialog.setMessage(paddingString + VivoRatioControllerUtilsImpl.this.mContext.getString(51249524) + "\n\n" + String.format(VivoRatioControllerUtilsImpl.this.mContext.getString(51249525), path1, path2, path3));
                    } else {
                        VivoRatioControllerUtilsImpl.this.mCurrentWarningDialog.setMessage(paddingString + VivoRatioControllerUtilsImpl.this.mContext.getString(51249524));
                    }
                    VivoRatioControllerUtilsImpl.this.mCurrentWarningDialog.getWindow().setType(2003);
                    VivoRatioControllerUtilsImpl.this.mCurrentWarningDialog.getWindow().getAttributes().setTitle("RatioSwitchAlert");
                    VivoRatioControllerUtilsImpl.this.mCurrentWarningDialog.getWindow().setGravity(80);
                    WindowManager.LayoutParams lp = VivoRatioControllerUtilsImpl.this.mCurrentWarningDialog.getWindow().getAttributes();
                    lp.privateFlags |= 16;
                    VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl2 = VivoRatioControllerUtilsImpl.this;
                    vivoRatioControllerUtilsImpl2.mSwitchingPkg = vivoRatioControllerUtilsImpl2.mCurrentPkg;
                    VivoRatioControllerUtilsImpl.this.mCurrentWarningDialog.show();
                }
                VivoRatioControllerUtilsImpl.this.mPendingFlag = false;
            }
        });
    }

    public void reLaunchCurrentApplication(Context context, Handler handler, boolean landscape) {
        if (DEBUG) {
            String str = TAG;
            VSlog.d(str, "DEBUG_RATIODIALOG:reLaunchCurrentApplication mCurrentPkg=" + this.mCurrentPkg + " mSwitchingPkg=" + this.mSwitchingPkg + " mCurrentAppOrientation=" + this.mCurrentAppOrientation);
        }
        try {
            if (this.mSwitchingPkg != null) {
                PackageManager pm = context.getPackageManager();
                int userId = ActivityManager.getCurrentUser();
                pm.getPackageInfoAsUser(this.mSwitchingPkg, 0, userId);
                this.sIsRelaunching = true;
                this.mPendingRestartPkg = this.mSwitchingPkg;
                Intent intent = getLaunchIntentForPackage(this.mSwitchingPkg, userId, context);
                if (DEBUG) {
                    String str2 = TAG;
                    VSlog.d(str2, "DEBUG_RATIODIALOG:reLaunchCurrentApplication intent=" + intent);
                }
                if (intent != null) {
                    intent.addFlags(268435456);
                    intent.addFlags(2097152);
                    intent.addFlags(Dataspace.STANDARD_BT709);
                    context.startActivityAsUser(intent, UserHandle.CURRENT);
                }
                handler.postDelayed(new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.5
                    @Override // java.lang.Runnable
                    public void run() {
                        VSlog.v(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIODIALOG:sIsRelaunching setting false");
                        VivoRatioControllerUtilsImpl.this.sIsRelaunching = false;
                    }
                }, 1000L);
            }
        } catch (Exception e) {
        }
    }

    /* loaded from: classes.dex */
    private class ConfirmDialogListener implements DialogInterface.OnClickListener {
        Context listenerContext;
        Handler mHandler;

        ConfirmDialogListener(Context context, Handler handler) {
            this.listenerContext = null;
            this.mHandler = null;
            this.listenerContext = context;
            this.mHandler = handler;
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                final boolean landscape = VivoRatioControllerUtilsImpl.this.isLandscape;
                if (IVivoRatioControllerUtils.DEBUG) {
                    String str = VivoRatioControllerUtilsImpl.TAG;
                    VSlog.d(str, "DEBUG_RATIODIALOG:onClick " + dialog + " which=" + which + " mDialogChangeValue=" + VivoRatioControllerUtilsImpl.this.mDialogChangeValue + " ,delayLaunch=" + VivoRatioControllerUtilsImpl.this.delayLaunch + " landscape=" + landscape);
                }
                VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl = VivoRatioControllerUtilsImpl.this;
                vivoRatioControllerUtilsImpl.updateUserModifiedSettings(this.listenerContext, vivoRatioControllerUtilsImpl.mCurrentPkg, VivoRatioControllerUtilsImpl.this.mDialogChangeValue);
                if (landscape) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.vivo.daemonService", "com.vivo.daemonService.ratioapsect.RatioMaskActivity"));
                    try {
                        intent.addFlags(268435456);
                        intent.addFlags(2097152);
                        intent.addFlags(Dataspace.STANDARD_BT709);
                        this.listenerContext.startActivity(intent);
                    } catch (Exception e) {
                        String str2 = VivoRatioControllerUtilsImpl.TAG;
                        VSlog.e(str2, "start RatioMaskActivity err=" + e.getMessage());
                    }
                }
                IActivityManager am = ActivityManagerNative.getDefault();
                try {
                    am.forceStopPackage(VivoRatioControllerUtilsImpl.this.mSwitchingPkg, 0);
                } catch (Exception e2) {
                }
                if (IVivoRatioControllerUtils.DEBUG) {
                    VSlog.d(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIODIALOG: forceStopPackage end");
                }
                if (landscape) {
                    this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.ConfirmDialogListener.1
                        @Override // java.lang.Runnable
                        public void run() {
                            VivoRatioControllerUtilsImpl.this.reLaunchCurrentApplication(ConfirmDialogListener.this.listenerContext, ConfirmDialogListener.this.mHandler, landscape);
                        }
                    }, VivoRatioControllerUtilsImpl.this.delayLaunch);
                } else {
                    this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.ConfirmDialogListener.2
                        @Override // java.lang.Runnable
                        public void run() {
                            VivoRatioControllerUtilsImpl.this.reLaunchCurrentApplication(ConfirmDialogListener.this.listenerContext, ConfirmDialogListener.this.mHandler, landscape);
                        }
                    }, 100L);
                }
            } else if (which == -2 && IVivoRatioControllerUtils.DEBUG) {
                String str3 = VivoRatioControllerUtilsImpl.TAG;
                VSlog.d(str3, "DEBUG_RATIODIALOG:onClick " + dialog + " which=" + which);
            }
        }
    }

    public boolean isNavigationBarGestureOff(Context context) {
        try {
            boolean isNavGestureOff = Settings.Secure.getInt(context.getContentResolver(), NAVIGATION_GESTURE_ON) == 0;
            return isNavGestureOff;
        } catch (Exception e) {
            if (DEBUG) {
                VSlog.w(TAG, "Get navigation bar settings error : SettingNotFoundException");
            }
            return true;
        }
    }

    public View addRatioSwitchView() {
        if (this.mIsDefaultDisplay) {
            if (DEBUG) {
                VSlog.d(TAG, "DEBUG_RATIOSWITCH:addRatioSwitchView ");
            }
            if (this.mRatioSwitchView != null) {
                if (DEBUG) {
                    VSlog.e(TAG, "DEBUG_RATIOSWITCH:addRatioSwitchView should be called once!");
                }
                return this.mRatioSwitchView;
            }
            LayoutInflater inflater = LayoutInflater.from(this.mContext);
            final View rootView = inflater.inflate(50528384, (ViewGroup) null);
            rootView.setBackgroundColor(0);
            rootView.setVisibility(8);
            TextView textView = (TextView) rootView.findViewById(51183845);
            this.mRatioSwitchButton = textView;
            textView.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.8
                @Override // android.view.View.OnClickListener
                public void onClick(View v) {
                    if (IVivoRatioControllerUtils.DEBUG) {
                        String str = VivoRatioControllerUtilsImpl.TAG;
                        VSlog.d(str, "DEBUG_RATIODIALOG:onClick " + v + " mSwitchingPkg=" + VivoRatioControllerUtilsImpl.this.mSwitchingPkg);
                    }
                    VivoRatioControllerUtilsImpl.this.showRatioSwitchDialog("buttonTrigger", true);
                }
            });
            DisplayMetrics metrics = this.mContext.getResources().getDisplayMetrics();
            float density = metrics.density;
            int height = (int) (16.0f * density);
            int width = Math.min(metrics.heightPixels, metrics.widthPixels);
            this.mRationTagView = new TagView(this.mRatioSwitchButton, 51249523, width, height);
            View findViewById = rootView.findViewById(51183787);
            this.mNaviCover = findViewById;
            findViewById.setVisibility(8);
            final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2999, 16908552, -2);
            lp.softInputMode = 32;
            lp.screenOrientation = 3;
            lp.privateFlags |= 1;
            lp.privateFlags |= 16;
            lp.setTitle("RatioSwitchButtonView");
            final WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.9
                @Override // java.lang.Runnable
                public void run() {
                    if (IVivoRatioControllerUtils.DEBUG) {
                        VSlog.d(VivoRatioControllerUtilsImpl.TAG, "DEBUG_RATIOSWITCH:addRatioSwitchView addView");
                    }
                    try {
                        wm.addView(rootView, lp);
                    } catch (Exception e) {
                    }
                }
            });
            this.mRatioSwitchView = rootView;
            return rootView;
        }
        return null;
    }

    public void updateCurrentWindowState(WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs, Rect appBounds) {
        this.mCurrentPkg = attrs.packageName;
        win.getAppToken();
        this.mAppBounds.set(appBounds);
        this.mCurrentAppOrientation = -1;
    }

    public void resetCurrentAppBound() {
        this.mAppBounds.set(0, 0, 0, 0);
    }

    public void updateRatioView(int State, final int bgColor, final int rotation, final int ratioViewBound) {
        if (this.mRatioSwitchView == null) {
            addRatioSwitchView();
        }
        View view = this.mNaviCover;
        if (view != null) {
            view.setVisibility(8);
        }
        boolean z = true;
        if (this.mLastRatioState != State) {
            this.mLastRatioState = State;
            if (State == 0) {
                this.mHandler.post(this.mShowRatioViewTransparent);
            } else if (State == 1) {
                this.mHandler.post(this.mShowRatioViewTransparent);
            } else if (State == 2) {
                this.mHandler.post(this.mShowRatioViewTransparent);
            } else if (State == 3) {
                this.mHandler.post(this.mHideRatioView);
            }
        }
        if (rotation != 1 && rotation != 3) {
            z = false;
        }
        this.isLandscape = z;
        if (this.mLastRotation != rotation || this.mLastRatioViewBound != ratioViewBound) {
            this.mLastRotation = rotation;
            this.mLastRatioViewBound = ratioViewBound;
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.VivoRatioControllerUtilsImpl.10
                @Override // java.lang.Runnable
                public void run() {
                    if (VivoRatioControllerUtilsImpl.this.mRatioSwitchButton != null) {
                        VivoRatioControllerUtilsImpl.this.mRationTagView.updateOrientation(rotation, ratioViewBound);
                        VivoRatioControllerUtilsImpl.this.mRationTagView.setTextColor(VivoRatioControllerUtilsImpl.this.isBgColorLight(bgColor, 194) ? -6184543 : -1711276033);
                    }
                }
            });
        }
    }

    private int getGrayValue(int color) {
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;
        int gray = (int) ((r * 0.3d) + (b * 0.59d) + (g * 0.11d));
        return gray;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isBgColorLight(int bgColor, int threshold) {
        int gray = getGrayValue(bgColor);
        if (gray < threshold) {
            return false;
        }
        return true;
    }

    private void hideRatioView() {
        if (DEBUG) {
            VSlog.d(TAG, "DEBUG_RATIOSWITCH:hideRatioSwitch");
        }
        this.mRatioSwitchView.setVisibility(8);
    }

    private void showRatioViewFollowSystem() {
        if (DEBUG) {
            VSlog.d(TAG, "DEBUG_RATIOSWITCH:mShowRatioViewFollowSystem");
        }
        if (this.mRatioSwitchView == null) {
            return;
        }
        int navColor = -723724;
        IWindowManager iWindowManager = this.mWindowManager;
        if (iWindowManager != null) {
            String navColorString = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            try {
                navColorString = iWindowManager.fetchSystemSetting("nav_color");
            } catch (Exception e) {
            }
            if (navColorString != null && navColorString != Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) {
                if (DEBUG) {
                    String str = TAG;
                    VSlog.d(str, "DEBUG_RATIOSWITCH:navColorString=" + navColorString);
                }
                navColor = Color.parseColor(navColorString);
            }
        }
        this.mRatioSwitchView.setBackgroundColor(navColor);
        this.mRatioSwitchView.setVisibility(0);
    }

    private void showRatioViewWhite() {
        if (DEBUG) {
            VSlog.d(TAG, "DEBUG_RATIOSWITCH:mShowRatioViewWhite");
        }
        View view = this.mRatioSwitchView;
        if (view == null) {
            return;
        }
        view.setBackgroundColor(-723724);
        this.mRatioSwitchView.setVisibility(0);
    }

    private void showRatioViewBlack() {
        if (DEBUG) {
            VSlog.d(TAG, "DEBUG_RATIOSWITCH:mShowRatioViewBlack");
        }
        View view = this.mRatioSwitchView;
        if (view == null) {
            return;
        }
        view.setBackgroundColor(-16777216);
        this.mRatioSwitchView.setVisibility(0);
    }

    public DisplayMetrics getCurrentMetrics(Context context) {
        if (this.mMetrics == null) {
            DisplayMetrics metrics = new DisplayMetrics();
            DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
            displayManager.getDisplay(0).getMetrics(metrics);
            this.mMetrics = metrics;
        }
        if (DEBUG) {
            String str = TAG;
            VSlog.e(str, "getCurrentMetrics " + this.mMetrics);
        }
        return this.mMetrics;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void retriveListMapFromSettings(String uriString, HashMap<String, ArrayList<String>> map) {
        Cursor cursor;
        if (DEBUG) {
            String str = TAG_MULTI;
            VSlog.d(str, "DEBUG_RATIOLIST:retriveListMapFromSettings from " + uriString);
        }
        if (map == null) {
            return;
        }
        map.clear();
        ContentResolver resolver = this.mContext.getContentResolver();
        Cursor cursor2 = null;
        try {
            try {
                try {
                    cursor = resolver.query(Uri.parse(uriString), null, null, null, null);
                    if (DEBUG) {
                        String str2 = TAG_MULTI;
                        VSlog.d(str2, "DEBUG_RATIOLIST:retriveListMapFromSettings , cursor =  " + cursor);
                    }
                    if (cursor != null) {
                        cursor.moveToFirst();
                        if (DEBUG) {
                            String str3 = TAG_MULTI;
                            VSlog.d(str3, "DEBUG_RATIOLIST:retriveListMapFromSettings , cursor size =  " + cursor.getCount());
                        }
                        if (cursor.getCount() > 0) {
                            while (!cursor.isAfterLast()) {
                                String pkg = cursor.getString(0);
                                String version = cursor.getString(1);
                                if (DEBUG) {
                                    String str4 = TAG_MULTI;
                                    VSlog.d(str4, "DEBUG_RATIOLIST:retriveListMapFromSettings , pkg =  " + pkg + " ; version = " + version);
                                }
                                if (!map.keySet().contains(pkg)) {
                                    ArrayList<String> versionList = new ArrayList<>();
                                    versionList.add(version);
                                    map.put(pkg, versionList);
                                } else {
                                    map.get(pkg).add(version);
                                }
                                cursor.moveToNext();
                            }
                        } else if (DEBUG) {
                            VSlog.d(TAG_MULTI, "retriveListMapFromSettings:no data!");
                        }
                    }
                } catch (Exception e) {
                }
            } catch (Exception e2) {
                String str5 = TAG_MULTI;
                VSlog.e(str5, "retriveListMapFromSettings:open database error! " + e2.fillInStackTrace());
                if (0 != 0) {
                    cursor2.close();
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            if (DEBUG) {
                String str6 = TAG_MULTI;
                VSlog.d(str6, "retriveListMapFromSettings end  map=" + map);
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    cursor2.close();
                } catch (Exception e3) {
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:59:0x0112  */
    /* JADX WARN: Removed duplicated region for block: B:84:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void retriveListFromSettings(java.lang.String r20, java.util.ArrayList<java.lang.String> r21) {
        /*
            Method dump skipped, instructions count: 328
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.VivoRatioControllerUtilsImpl.retriveListFromSettings(java.lang.String, java.util.ArrayList):void");
    }

    private void checkAddPkg(ArrayList<String> list, String pkgName) {
        if (!list.contains(pkgName)) {
            list.add(pkgName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x00f8, code lost:
        if (r11 == null) goto L25;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x00fb, code lost:
        return r1;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean checkStoryCaptureUINeedsAdapter(java.lang.String r13, java.lang.String r14, java.lang.String r15) {
        /*
            Method dump skipped, instructions count: 258
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.VivoRatioControllerUtilsImpl.checkStoryCaptureUINeedsAdapter(java.lang.String, java.lang.String, java.lang.String):boolean");
    }

    public boolean needsLetterBoxOnSecondaryDisplay(ApplicationInfo appInfo) {
        if (!MultiDisplayManager.isMultiDisplay || !ENABLE_SECONDARY_DISPLAY_LETTERBOX) {
            return false;
        }
        if (appInfo == null) {
            VSlog.d(TAG_MULTI, " needsLetterBoxOnSecondaryDisplay : return true for NULL");
            return true;
        }
        boolean isBackScreenSettingOnForPackage = isBackScreenSettingOnForPackage(appInfo);
        if (DEBUG) {
            String str = TAG_MULTI;
            StringBuilder sb = new StringBuilder();
            sb.append(" needsLetterBoxOnSecondaryDisplay : return ");
            sb.append(!isBackScreenSettingOnForPackage);
            sb.append(" for ");
            sb.append(appInfo.packageName);
            VSlog.d(str, sb.toString());
        }
        return !isBackScreenSettingOnForPackage;
    }

    private boolean isBackScreenSettingOnForPackage(ApplicationInfo appInfo) {
        if (isVivoApp(appInfo)) {
            if (DEBUG) {
                String str = TAG_MULTI;
                VSlog.d(str, " isBackScreenOnForPackage : return TRUE due to isVivoApp for " + appInfo.packageName);
            }
            return true;
        } else if (!isAdaptedThirdPartApplication(appInfo)) {
            if (DEBUG) {
                String str2 = TAG_MULTI;
                VSlog.d(str2, " isBackScreenOnForPackage : return FALSE due to !isAdaptedThirdPartApplication for " + appInfo.packageName);
            }
            return false;
        } else if (this.sBackFullScreenIsOn) {
            if (DEBUG) {
                String str3 = TAG_MULTI;
                VSlog.d(str3, " isBackScreenOnForPackage : return TRUE due to sBackFullScreenIsOn for " + appInfo.packageName);
            }
            return true;
        } else if (this.mUserSettingBackScreenMap.keySet().contains(appInfo.packageName)) {
            int isFullScreenForBack = this.mUserSettingBackScreenMap.get(appInfo.packageName).intValue();
            if (isFullScreenForBack == 1) {
                if (DEBUG) {
                    String str4 = TAG_MULTI;
                    VSlog.d(str4, " isBackScreenOnForPackage : return TRUE due to user modify for " + appInfo.packageName);
                }
                return true;
            }
            if (DEBUG) {
                String str5 = TAG_MULTI;
                VSlog.d(str5, " isBackScreenOnForPackage : return FALSE due to user modify for " + appInfo.packageName);
            }
            return false;
        } else {
            if (DEBUG) {
                String str6 = TAG_MULTI;
                VSlog.d(str6, " isBackScreenOnForPackage : return TRUE due to isAdaptedThirdPartApplication for " + appInfo.packageName);
            }
            return true;
        }
    }

    public boolean shouldRebootWhileSwitch(String pkgName) {
        ApplicationInfo aInfo = null;
        try {
            aInfo = this.mPackageManager.getApplicationInfo(pkgName, 128);
        } catch (PackageManager.NameNotFoundException e) {
            String str = TAG;
            VSlog.w(str, "Error while getApplicationInfo for " + pkgName, e);
        }
        if (aInfo != null) {
            return shouldRebootDueToDisplayChange(aInfo);
        }
        return true;
    }

    private boolean shouldRebootDueToDisplayChange(ApplicationInfo appInfo) {
        return (isVivoApp(appInfo) || isAdaptedThirdPartApplication(appInfo) || !isBackScreenSettingOnForPackage(appInfo)) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSettingsChangedForBackScreenLocked() {
        OnRatioChangeListener listener;
        this.mLastUserSettingBackScreenMap.putAll((ArrayMap<? extends String, ? extends Integer>) this.mUserSettingBackScreenMap);
        this.mUserSettingBackScreenMap.clear();
        if (DEBUG) {
            String str = TAG_MULTI;
            VLog.d(str, "handleSettingsChangedForBackScreenLocked mLastUserSettingBackScreenMap size is:" + this.mLastUserSettingBackScreenMap.size());
        }
        mChangingPkg = null;
        this.mUserSettingBackScreenMap = getUserSettingBackScreenMap(this.mContext);
        if (DEBUG) {
            String str2 = TAG_MULTI;
            StringBuilder sb = new StringBuilder();
            sb.append("handleSettingsChangedForBackScreenLocked.mUserSettingBackScreenMap size:");
            ArrayMap<String, Integer> arrayMap = this.mUserSettingBackScreenMap;
            sb.append(arrayMap != null ? Integer.valueOf(arrayMap.size()) : "null");
            VSlog.d(str2, sb.toString());
        }
        if (mChangingPkg != null) {
            synchronized (mCallbacks) {
                for (int i = 0; i < mCallbacks.size(); i++) {
                    String pkgName = mCallbacks.valueAt(i);
                    if (mChangingPkg.equals(pkgName) && (listener = mCallbacks.keyAt(i)) != null) {
                        if (DEBUG) {
                            String str3 = TAG_MULTI;
                            VSlog.d(str3, "handleSettingsChangedForBackScreenLocked :  ; mChangingPkg = " + mChangingPkg);
                        }
                        listener.onRatioChange();
                    }
                }
            }
        }
    }

    private ArrayMap<String, Integer> getUserSettingBackScreenMap(Context context) {
        ArrayMap<String, Integer> userModifiedMap = new ArrayMap<>();
        if (context == null) {
            return userModifiedMap;
        }
        String settingsString = Settings.Secure.getString(context.getContentResolver(), "modified_back_screen_display_apps");
        if (TextUtils.isEmpty(settingsString)) {
            return userModifiedMap;
        }
        String[] tempString = settingsString.replace(" ", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).split(":");
        if (tempString.length == 0) {
            return userModifiedMap;
        }
        for (String tempMapStr : tempString) {
            if (tempMapStr.length() > 0) {
                String pkg = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                int status = 0;
                try {
                    pkg = tempMapStr.split(",")[0];
                    status = "1".equals(tempMapStr.split(",")[1]) ? 1 : 0;
                } catch (Exception e) {
                    if (DEBUG) {
                        VSlog.w(TAG_MULTI, "getUserSettingBackScreenMap: " + e);
                    }
                }
                if (!userModifiedMap.containsKey(pkg)) {
                    userModifiedMap.put(pkg, Integer.valueOf(status));
                    if (this.mLastUserSettingBackScreenMap.containsKey(pkg)) {
                        if (this.mLastUserSettingBackScreenMap.get(pkg).intValue() != status) {
                            mChangingPkg = pkg;
                            mChangingStatus = status != 0;
                            if (DEBUG) {
                                VSlog.d(TAG_MULTI, "getUserSettingBackScreenMap : packageName change:" + pkg + ",status is:" + status);
                            }
                        }
                        this.mLastUserSettingBackScreenMap.remove(pkg);
                    } else {
                        mChangingPkg = pkg;
                        mChangingStatus = true;
                        if (DEBUG) {
                            VSlog.d(TAG_MULTI, "getUserSettingBackScreenMap : packageName ADD:" + pkg + ",status is:" + status);
                        }
                    }
                    if (DEBUG) {
                        VSlog.d(TAG_MULTI, "getUserSettingBackScreenMap : packageName is:" + pkg + ",status is:" + status);
                    }
                }
            }
        }
        this.mLastUserSettingBackScreenMap.clear();
        if (DEBUG) {
            VLog.d(TAG_MULTI, "getUserSettingBackScreenMap userModifiedMap size is:" + userModifiedMap.size());
        }
        return userModifiedMap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isVivoApp(ApplicationInfo appInfo) {
        if (this.ALL_ADAPTER) {
            return true;
        }
        if (appInfo != null) {
            return (isSystemApp(appInfo) || this.sVivoSignaturesAppList.contains(appInfo.packageName)) && !isGMSApp(appInfo.packageName);
        }
        VSlog.d(TAG_MULTI, " isVivoApp : return false for NULL");
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void retriveVivoSignaturesAppList() {
        if (DEBUG) {
            VSlog.d(TAG_MULTI, "retriveVivoSignaturesAppList START.");
        }
        List<ApplicationInfo> apps = this.mPackageManager.getInstalledApplications(0);
        if (DEBUG) {
            String str = TAG_MULTI;
            VSlog.d(str, "retriveVivoSignaturesAppList for COUNT : " + apps.size());
        }
        for (ApplicationInfo app : apps) {
            if (checkVivoSignaturesApp(app)) {
                if (DEBUG) {
                    String str2 = TAG_MULTI;
                    VSlog.d(str2, "retriveVivoSignaturesAppList: add vivoApp " + app.packageName);
                }
                this.sVivoSignaturesAppList.add(app.packageName);
            }
        }
        if (DEBUG) {
            VSlog.d(TAG_MULTI, "retriveVivoSignaturesAppList END.");
        }
    }

    private boolean checkVivoSignaturesApp(ApplicationInfo appInfo) {
        return this.mPackageManager.checkSignatures(VivoPermissionUtils.OS_PKG, appInfo.packageName) == 0 || this.mPackageManager.checkSignatures("com.android.providers.contacts", appInfo.packageName) == 0 || this.mPackageManager.checkSignatures("com.android.providers.media", appInfo.packageName) == 0;
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        if (appInfo == null) {
            return false;
        }
        return ((appInfo.flags & 1) == 0 && (appInfo.flags & 128) == 0) ? false : true;
    }

    private boolean isGMSApp(String pkgName) {
        if (pkgName != null) {
            if (this.mOtherGMSApp.contains(pkgName) || pkgName.contains("com.google.android")) {
                if (DEBUG) {
                    String str = TAG_MULTI;
                    VSlog.d(str, "isGMSApp : " + pkgName);
                    return true;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public void registerCallback(OnRatioChangeListener callback, String pkgName) {
        synchronized (mCallbacks) {
            if (mCallbacks.get(callback) != null) {
                return;
            }
            mCallbacks.put(callback, pkgName);
        }
    }

    public void unregisterCallback(OnRatioChangeListener callback) {
        if (callback == null) {
            throw new IllegalArgumentException("pkgName must not be null");
        }
        synchronized (mCallbacks) {
            String pkgName = mCallbacks.get(callback);
            if (DEBUG) {
                String str = TAG_MULTI;
                VSlog.d(str, "unregisterCallback : pkgName = " + pkgName + " ; callback = " + callback);
            }
            mCallbacks.remove(callback);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class TagView {
        private Bitmap mBitmap;
        private Context mContext;
        private RotateDrawable mDrawable;
        private int mHeight;
        private StaticLayout.Builder mLayoutBuilder;
        private int mRotation;
        private int mSize;
        private CharSequence mText;
        private int mTextColor;
        private int mTextId;
        private float mTextSize;
        private View mView;
        private int mWidth;
        private int mLevel = 0;
        private final String TEXT_COLOR = "#FF818181";
        private final int TEXT_SIZE = 14;

        public TagView(View tag, int resId, int width, int height) {
            this.mView = tag;
            this.mTextId = resId;
            Context context = tag.getContext();
            this.mContext = context;
            float density = context.getResources().getDisplayMetrics().density;
            this.mTextSize = (int) (14.0f * density);
            this.mTextColor = Color.parseColor("#FF818181");
            this.mText = this.mContext.getString(this.mTextId);
            RotateDrawable rotateDrawable = new RotateDrawable();
            this.mDrawable = rotateDrawable;
            rotateDrawable.setFromDegrees(0.0f);
            this.mDrawable.setToDegrees(360.0f);
            this.mDrawable.setPivotXRelative(false);
            this.mDrawable.setPivotYRelative(false);
            this.mView.setBackground(this.mDrawable);
            this.mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888, true);
            onSizeChanged(0, 0, width, height);
        }

        private void createTextLayout(int color, float size, int width) {
            TextPaint paint = new TextPaint();
            paint.setTextSize(size);
            paint.setColor(color);
            paint.setAntiAlias(true);
            StaticLayout.Builder obtain = StaticLayout.Builder.obtain(new String(), 0, 0, paint, width);
            this.mLayoutBuilder = obtain;
            obtain.setEllipsize(TextUtils.TruncateAt.END);
            this.mLayoutBuilder.setMaxLines(1);
            this.mLayoutBuilder.setAlignment(Layout.Alignment.ALIGN_CENTER);
            this.mLayoutBuilder.setEllipsizedWidth(width);
        }

        private void createTextDrawable(CharSequence str, int height) {
            Canvas canvas = new Canvas(this.mBitmap);
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            this.mLayoutBuilder.setText(str);
            StaticLayout layout = this.mLayoutBuilder.build();
            canvas.save();
            canvas.translate(0.0f, (height - layout.getHeight()) / 2);
            layout.draw(canvas);
            canvas.restore();
            BitmapDrawable drawable = new BitmapDrawable(this.mBitmap);
            drawable.setGravity(17);
            DisplayMetrics metrics = this.mContext.getResources().getDisplayMetrics();
            drawable.setTargetDensity(metrics.densityDpi);
            this.mDrawable.setDrawable(drawable);
        }

        private void updateTextAttribute() {
            updateTextAttribute(this.mTextColor, this.mTextSize, this.mText);
        }

        private void updateTextAttribute(int textColor) {
            updateTextAttribute(textColor, this.mTextSize, this.mText);
        }

        private void updateTextAttribute(float textSize) {
            updateTextAttribute(this.mTextColor, textSize, this.mText);
        }

        private void updateTextAttribute(int textColor, float textSize, CharSequence text) {
            createTextLayout(textColor, textSize, this.mWidth);
            createTextDrawable(text, this.mHeight);
        }

        private void onSizeChanged(int oldWidth, int oldHeight, int newWidth, int newHeight) {
            this.mHeight = newHeight;
            this.mWidth = newWidth;
            this.mBitmap.setWidth(newWidth);
            this.mBitmap.setHeight(this.mHeight);
            if (newWidth != oldWidth) {
                updateTextAttribute();
            } else if (newHeight != oldHeight) {
                createTextDrawable(this.mText, newHeight);
            }
        }

        public void setText(int resId) {
            if (resId == this.mTextId) {
                return;
            }
            this.mTextId = resId;
            String string = this.mContext.getString(resId);
            this.mText = string;
            createTextDrawable(string, this.mHeight);
        }

        public void setText(CharSequence str) {
            if (TextUtils.equals(str, this.mText)) {
                return;
            }
            this.mTextId = 0;
            this.mText = str;
            createTextDrawable(str, this.mHeight);
        }

        public void setTextColor(int color) {
            if (this.mTextColor == color) {
                return;
            }
            this.mTextColor = color;
            updateTextAttribute(color);
        }

        public void setTextSize(float size) {
            if (size == this.mTextSize) {
                return;
            }
            this.mTextSize = size;
            updateTextAttribute(size);
        }

        public void setHeight(int height) {
            int i = this.mHeight;
            if (height == i) {
                return;
            }
            int i2 = this.mWidth;
            onSizeChanged(i2, i, i2, height);
        }

        public void setWidth(int width) {
            int i = this.mWidth;
            if (width == i) {
                return;
            }
            int i2 = this.mHeight;
            onSizeChanged(i, i2, width, i2);
        }

        public void onDestroy() {
            Bitmap bitmap = this.mBitmap;
            if (bitmap != null && !bitmap.isRecycled()) {
                this.mBitmap.recycle();
            }
        }

        public void updateOrientation(int rotation, int size) {
            int level;
            if (this.mRotation == rotation && this.mSize == size) {
                return;
            }
            this.mRotation = rotation;
            this.mSize = size;
            float pivotX = this.mWidth / 2;
            float pivotY = this.mHeight / 2;
            if (rotation == 1 || rotation == 3) {
                pivotX = this.mSize / 2;
                pivotY = this.mWidth / 2;
            }
            this.mDrawable.setPivotX(pivotX);
            this.mDrawable.setPivotY(pivotY);
            int i = this.mRotation;
            if (i == 1) {
                level = 7500;
            } else {
                level = i == 3 ? 2500 : 0;
            }
            this.mDrawable.setLevel(level);
        }

        public void updateLanguage() {
            int i = this.mTextId;
            if (i == 0) {
                return;
            }
            this.mText = this.mContext.getString(i);
            updateTextAttribute();
        }

        public void updateDensity(int density) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) this.mDrawable.getDrawable();
            bitmapDrawable.setTargetDensity(density);
        }
    }

    public void setAlienScreenCoverInsetTop(int top) {
        if (DEBUG) {
            String str = TAG;
            VSlog.d(str, "setAlienScreenCoverInsetTop = " + top);
        }
        this.alienScreenCoverInsetTop = top;
    }

    public Rect getCurrentAppBound() {
        return this.mAppBounds;
    }

    public int getAlienScreenCoverInsetTop() {
        return this.alienScreenCoverInsetTop;
    }

    public boolean isHasNavBar() {
        return this.sVivoHasNavBar;
    }

    public boolean isGestureBarIsOn() {
        return this.sGestureBarIsOn;
    }

    public int getRatioViewMinHeight() {
        return this.ratioViewMinHeight;
    }

    public int getStatusBarHeight() {
        return this.statusBarHeight;
    }

    public float getDeviceRatio() {
        return this.sDeviceRatio;
    }

    private static boolean isSystemServer(int pid) {
        return pid == Process.myPid();
    }

    public int getCurrentInputMethodDisplayId() {
        int intValue;
        synchronized (mDisplayIdForPids) {
            Integer runningDisplayId = mDisplayIdForPids.get(this.sCurrentInputMethodPid);
            intValue = runningDisplayId != null ? runningDisplayId.intValue() : -1;
        }
        return intValue;
    }

    public boolean isImeApplication(Context context, int pid, int uid) {
        return (isSystemServer(pid) || !isInputMethodProcess(pid) || getApplicationInfoForPid(context, pid, uid) == null) ? false : true;
    }

    public boolean getCurvedCutoutPolicyForPackage(String pkg) {
        boolean z = this.mIsDefaultCurveCut;
        synchronized (userModifiedPkgMapForCurvedCutout) {
            if (this.mVivoListCenter.isSystemOrVivoApp(pkg)) {
                z = false;
            } else if (userModifiedPkgMapForCurvedCutout.keySet().contains(pkg)) {
                z = userModifiedPkgMapForCurvedCutout.get(pkg).intValue();
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append(pkg);
                    sb.append(" CurvedCutoutPolicy ISCUTOUT: ");
                    int isCurvedCutout = z ? 1 : 0;
                    sb.append(isCurvedCutout);
                    sb.append(".in userModified.");
                    VSlog.d(str, sb.toString());
                }
            } else if (sCurvedScreenBlackList.contains(pkg)) {
                z = true;
                z = true;
                if (DEBUG) {
                    String str2 = TAG;
                    VSlog.d(str2, pkg + " CurvedCutoutPolicy ISCUTOUT: 1.in curvedScreenBlackList.");
                }
            }
        }
        return z;
    }

    public DisplayInfo getCurvedCutoutDisplayInfo(int displayId, Context context, int pid, int uid, DisplayInfo info) {
        return info;
    }

    public WindowManagerPolicy.WindowState getCurvedCuttingWindow() {
        return mCurveCuttingWindow;
    }

    public void setCurvedCuttingWindow(WindowManagerPolicy.WindowState win) {
        mCurveCuttingWindow = win;
    }

    private static Intent getLaunchIntentForPackage(String packageName, int userId, Context context) {
        Intent intentToResolve = new Intent("android.intent.action.MAIN");
        intentToResolve.addCategory("android.intent.category.INFO");
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = context.getPackageManager().queryIntentActivitiesAsUser(intentToResolve, 0, userId);
        if (ris == null || ris.size() <= 0) {
            intentToResolve.removeCategory("android.intent.category.INFO");
            intentToResolve.addCategory("android.intent.category.LAUNCHER");
            intentToResolve.setPackage(packageName);
            ris = context.getPackageManager().queryIntentActivitiesAsUser(intentToResolve, 0, userId);
        }
        if (ris == null || ris.size() <= 0) {
            return null;
        }
        Intent intent = new Intent(intentToResolve);
        intent.setFlags(268435456);
        intent.setClassName(ris.get(0).activityInfo.packageName, ris.get(0).activityInfo.name);
        return intent;
    }

    public boolean getIsTierProduct() {
        return isTierProduct;
    }
}