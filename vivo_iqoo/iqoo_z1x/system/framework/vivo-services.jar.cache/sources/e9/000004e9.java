package com.android.server.wm;

import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.GraphicBuffer;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManagerInternal;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.IWallpaperVisibilityListener;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.widget.ImageView;
import com.android.server.LocalServices;
import com.android.server.am.EmergencyBroadcastManager;
import com.android.server.biometrics.face.FaceInternal;
import com.android.server.display.color.VivoLightColorMatrixControl;
import com.android.server.pm.VivoPKMSLocManager;
import com.vivo.face.common.data.Constants;
import com.vivo.fingerprint.FingerprintConfig;
import com.vivo.fingerprint.SnapshotConfig;
import com.vivo.fingerprint.analysis.AnalysisManager;
import com.vivo.sensor.implement.VivoSensorImpl;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class SnapshotWindow {
    private static final String AOD_WINDOW_TITLE = "AOD";
    private static final String BLACK_WINDOW_TITLE = "VivoDisplayOverlay";
    private static final int CHECK_FULLOPAQUEWIN_ALPHA = 1;
    private static final int CHECK_LAUNCH_DRAWER_ALPHA = 2;
    private static int CHECK_TOPAPP_RETRY_NUM = 0;
    private static final String COMMAND_OFF_FINGERPRINT_UNLOCK = "offFingerprintUnlock";
    private static final String COMMAND_ON_FINGERPRINT_UNLOCK = "onFingerprintUnlock";
    private static long DELAY_CHECK_TOPAPP = 0;
    private static final long DELAY_CREATED_REMOVE_LONG = 5000;
    private static final long DELAY_CREATED_REMOVE_SHORT = 3000;
    private static final String[] DELAY_REMOVE_SNAPSHOT_ACTIVITY_LIST_FOR_FACE;
    private static final int DELAY_RESTRORE_SHOW_KEYGUARD_GOOGLE = 1500;
    private static final int DELAY_RESTRORE_SHOW_KEYGUARD_HWC = 17;
    private static long DELAY_SF_REMOVE = 0;
    private static long DELAY_UNLOCKED_REMOVE = 0;
    private static long DELAY_UNLOCKED_REMOVE_LANDSCAPE = 0;
    private static long DELAY_UNLOCKED_REMOVE_LAUNCHER = 0;
    private static final long DELAY_UNLOCKED_REMOVE_MAX = 600;
    private static final long DELAY_UNLOCKED_REMOVE_PLUS = 50;
    private static final String DIMLAYER_HOST_NAME = "DisplayArea.Root";
    private static final int DYNAMIC_WALLPAPER = 2;
    private static final String FP_UI_ICON_WINDOW_TITLE = "UDfinger";
    private static final int GOOGLE_SPEED_OFF = 3;
    private static final int GOOGLE_SPEED_OFF_DRAWN = 4;
    private static final int GOOGLE_SPEED_ON = 1;
    private static final int GOOGLE_SPEED_ON_DRAWN = 2;
    private static final boolean IS_CAPACITIVE;
    private static final boolean IS_UDFINGERPRINT;
    private static final String KEYGUARD_WINDOW_TITLE = "NotificationShade";
    private static final int KEYGURADVIEW_DISMISS = -200;
    private static final int KEYGURADVIEW_SHOW = 200;
    private static final int MINUS_SCREEN_X = 8;
    public static final int MODULE_SNAPSHOT_FACE = 1;
    public static final int MODULE_SNAPSHOT_FINGERPRINT = 2;
    public static final int MODULE_SNAPSHOT_NOT_REQUIRED = 0;
    private static final int NO_WALLPAPER = 0;
    private static final int OTHER_FULLOPAQUEWIN_MAX_NUM = 4;
    private static final String PHASE_UNLOCK_START = "UNLOCK_START";
    private static final String PHASE_UNLOCK_STOP = "UNLOCK_STOP";
    private static final String PHASE_UNLOCK_SUCCESS = "UNLOCK_SUCCESS";
    private static final String SETTING_FACE_UNLOCK = "face_unlock_keyguard_enabled";
    private static final String SETTING_FINGERPRINT_UNLOCK = "finger_unlock_open";
    private static final String SETTING_START_APP_FROM_KEYGUARD = "startApp_from_keyguard";
    private static long SHOW_DIM_TIMEOUT = 0;
    private static final long SIZE_MISMATCH_MINIMUM_TIME_MS = 550;
    private static final float SNAPSHOTWINDOW_ZERO_ALPHA = 0.0f;
    private static final String SNAPSHOT_WINDOW_TITLE = "SnapshotWindow";
    private static final int STATIC_WALLPAPER = 1;
    private static final String TAG = "SnapshotWindow";
    private static final int TYPE_SECURE = 1;
    private static final int TYPE_SYSTEM = 0;
    private static final String UNLOCK_ENTER_LAUNCHER_ANIMATION = "unlock_enter_launcher_animation";
    private static final int USE_GOOGLE = 2;
    private static final int USE_SNAPSHOT = 3;
    private static final boolean USE_TOPLEVEL = true;
    private static final int USE_VISIBLE = 1;
    private static final int VCD_PKG_STR_MAX = 100;
    private static final long WAIT_SNAPSHOTWIN_FINISHDRAW_TIMEOUT = 200;
    private static SnapshotWindow mInstance;
    private boolean hasMultiFullOpaqueWinShowOnScreen;
    private boolean mAfterUnlockFocusWindowGainedFocus;
    private boolean mAuthInteractive;
    private Drawable mBackgroundDrawable;
    private int mCheckNum;
    private Context mContext;
    private List<NameList> mDelayRemoveWindows;
    private float mDimLayerAlpha;
    private float mDisplayBrightness;
    private DisplayManagerInternal mDisplayManagerInternal;
    private int mDisplayRotation;
    private boolean mFaceEnable;
    private FaceInternal mFaceInternal;
    private boolean mFingerprintEnable;
    private boolean mForceShowingDimLayer;
    private boolean mFpAuthSuccess;
    private int mGoogleSpeedAnalysis;
    private Handler mHandler;
    private boolean mHasSurfaceView;
    private boolean mIsFixedOrientationLandscape;
    private boolean mKeyguardShowing;
    private Bitmap mLandscapeBitmap;
    private CountDownLatch mLatch;
    private boolean mLauncherAnimEnable;
    private VivoLightColorMatrixControl mLigntColorMatrixControl;
    private WindowState mRecordFinishDrawingWin;
    private SurfaceControl mSC;
    private ActivityRecord mScreenOffActivity;
    private boolean mSetStatusBarColor;
    private long mShownTime;
    private boolean mSnapshotAlpha;
    private volatile Bitmap mSnapshotBmp;
    private boolean mSnapshotDrawn;
    private boolean mSnapshotUnlockClosedTransitionAnim;
    private boolean mSnapshotUseWinIsChanged;
    private WindowState mSnapshotUsedWindow;
    private ImageView mSnapshotView;
    private boolean mSnapshotVisible;
    private boolean mStartAppEnable;
    private int mStatusBarColorLog;
    private int mSysUiVis;
    private ActivityRecord mTopActivity;
    private boolean mTopActivityDrawn;
    private WindowState mTopFullscreenOpaqueWin;
    private ActivityRecord mUnlockActivity;
    private boolean mUnlockLauncherPlayAnim;
    private WindowManagerService mWMS;
    private WallpaperVisibility mWallpaper;
    private WallpaperManager mWallpaperManager;
    private boolean mWallpaperVisible;
    private WindowState mWinAod;
    private WindowState mWinBlack;
    private WindowState mWinDimLayer;
    private WindowState mWinFingerprintUiIcon;
    private WindowState mWinNotificationShade;
    private WindowState mWinSnapshot;
    private WindowState mWinWallpaper;
    private WindowManager.LayoutParams mWindowLp;
    private WindowManager mWindowManager;
    private static final boolean DEBUG_SNAPSHOT = SystemProperties.getBoolean("sys.fingerprint.snapshot.debug", false);
    private static final String PLATFORM = SystemProperties.get("ro.vivo.product.platform", "unknown");
    private int mSnapshotCreate = 0;
    private int mWallpaperState = 0;
    private long mStartRemoveWinTime = 0;
    private long mUnlockAndDrawnTime = 0;
    private long mKeyguardExitTime = 0;
    private boolean mDeviceInteractive = true;
    private int mUnlockModule = 0;
    private boolean mSnapshotPortrait = true;
    private boolean mImeWindowVisible = false;
    private boolean mIsHardwareAccelerated = true;
    private final ArrayList<WaitingWindowState> mWinList = new ArrayList<>();
    private String mGoogleReason = "null";
    private VivoSensorImpl mVivoSensorImpl = null;
    private int mStatusbarHeight = 0;
    private ArrayList<WindowState> mOtherFullOpaqueWindows = null;

    static {
        boolean isOpticalFingerprint = FingerprintConfig.isOpticalFingerprint();
        IS_UDFINGERPRINT = isOpticalFingerprint;
        IS_CAPACITIVE = !isOpticalFingerprint;
        DELAY_REMOVE_SNAPSHOT_ACTIVITY_LIST_FOR_FACE = new String[]{"com.tencent.mm/.plugin.brandservice.ui.timeline.preload.ui.TmplWebViewTooLMpUI"};
        DELAY_SF_REMOVE = WAIT_SNAPSHOTWIN_FINISHDRAW_TIMEOUT;
        long j = SystemProperties.getLong("sys.fingerprint.keyguard.timeout", (long) WAIT_SNAPSHOTWIN_FINISHDRAW_TIMEOUT);
        DELAY_UNLOCKED_REMOVE = j;
        DELAY_UNLOCKED_REMOVE_LANDSCAPE = j + 100;
        DELAY_UNLOCKED_REMOVE_LAUNCHER = SystemProperties.getLong("sys.fingerprint.keyguard.timeout", 34L);
        DELAY_CHECK_TOPAPP = 100L;
        CHECK_TOPAPP_RETRY_NUM = 7;
        SHOW_DIM_TIMEOUT = 5000L;
    }

    public static SnapshotWindow getInstance(Context context) {
        synchronized (SnapshotWindow.class) {
            if (mInstance == null) {
                mInstance = new SnapshotWindow(context);
            }
        }
        return mInstance;
    }

    private SnapshotWindow(Context context) {
        this.mLigntColorMatrixControl = null;
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        HandlerThread handlerThread = new HandlerThread("SnapshotWindow");
        handlerThread.start();
        this.mHandler = new AsyncHandler(handlerThread.getLooper());
        ArrayList arrayList = new ArrayList();
        this.mDelayRemoveWindows = arrayList;
        arrayList.add(new NameList("com.tencent.mm/.ui.LauncherUI", 150L));
        this.mLigntColorMatrixControl = VivoLightColorMatrixControl.getExistInstance();
    }

    public void setWindowManagerService(WindowManagerService service) {
        this.mWMS = service;
    }

    public void systemReady() {
        this.mHandler.sendEmptyMessage(2);
        this.mStatusbarHeight = this.mContext.getResources().getDimensionPixelSize(17105488);
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        getFaceInternal();
        this.mIsHardwareAccelerated = ActivityManager.isHighEndGfx();
        this.mVivoSensorImpl = VivoSensorImpl.getInstance(this.mContext);
    }

    public void createSnapshotWindow(int module) {
        if (!checkPermission()) {
            return;
        }
        this.mHandler.obtainMessage(101, module, 0).sendToTarget();
    }

    private void checkDimlayerBeforeShowSnapshotWin() {
        boolean v;
        try {
            this.mForceShowingDimLayer = false;
            DisplayContent displayContent = defaultDisplayContentLw();
            if (displayContent != null) {
                DisplayPolicy policy = displayContent.getDisplayPolicy();
                WindowState dimmingWindowLw = policy.getDimmingWindowLw();
                this.mWinDimLayer = dimmingWindowLw;
                if (this.mSnapshotUsedWindow == dimmingWindowLw) {
                    warning("ignore dimmer, zOrderRelativeOf topFullOpaqueWin", new Object[0]);
                } else if (dimmingWindowLw != null) {
                    Task task = null;
                    if (dimmingWindowLw.mActivityRecord != null) {
                        task = this.mWinDimLayer.getTask();
                        v = task != null ? task.shouldBeVisible((ActivityRecord) null) : false;
                    } else {
                        v = this.mWinDimLayer.wouldBeVisibleIfPolicyIgnored();
                        debug("NonAppWindows v=" + v, new Object[0]);
                    }
                    String str = "; task null";
                    if (v) {
                        this.mForceShowingDimLayer = true;
                        this.mHandler.removeMessages(22);
                        this.mDimLayerAlpha = this.mWinDimLayer.mAttrs.dimAmount;
                        StringBuilder sb = new StringBuilder();
                        sb.append("begin force show dimmer ");
                        sb.append(this.mDimLayerAlpha);
                        sb.append(" ");
                        sb.append(this.mWinDimLayer);
                        if (task != null) {
                            str = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                        }
                        sb.append(str);
                        debug(sb.toString(), new Object[0]);
                        return;
                    }
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("dimmer not visible : ");
                    sb2.append(this.mWinDimLayer);
                    if (task != null) {
                        str = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    }
                    sb2.append(str);
                    debug(sb2.toString(), new Object[0]);
                }
            }
        } catch (Exception ex) {
            warning("get getDimmingWindow Exception: " + ex, new Object[0]);
        }
    }

    public void setSnapshotVisibility(int module, boolean show) {
        if (!checkPermission()) {
            return;
        }
        if (this.mSnapshotAlpha == show) {
            warning("alpha is already set " + show, new Object[0]);
            return;
        }
        if (show) {
            if (this.mLigntColorMatrixControl == null) {
                this.mLigntColorMatrixControl = VivoLightColorMatrixControl.getExistInstance();
            }
            VivoLightColorMatrixControl vivoLightColorMatrixControl = this.mLigntColorMatrixControl;
            if (vivoLightColorMatrixControl != null) {
                vivoLightColorMatrixControl.notifyUdSnapshot();
            }
        }
        float alpha = show ? 1.0f : SNAPSHOTWINDOW_ZERO_ALPHA;
        this.mAuthInteractive = this.mDeviceInteractive;
        this.mFpAuthSuccess = module == 2;
        if (show) {
            AnalysisManager.trace("google_reason", this.mGoogleReason);
            AnalysisManager.trace("speedPkg", toSubString(toActivityString(this.mScreenOffActivity)));
            AnalysisManager.trace("unlockPkg", toSubString(toActivityString(this.mTopActivity)));
            AnalysisManager.trace("lanAnim", String.valueOf(this.mLauncherAnimEnable));
        }
        if (show && !useGoogle() && this.mSnapshotView != null) {
            notifyStatusBarSetColor();
            checkDimlayerBeforeShowSnapshotWin();
            createDimLayerShow();
        }
        setAlpha(alpha, module);
        if (!useGoogle() && this.mSnapshotView != null && show && !this.mSnapshotPortrait) {
            snapshotUnlockCloseAnim(true);
        }
        Message msg = Message.obtain(this.mHandler, 102, module, show ? 1 : 0);
        this.mHandler.sendMessage(msg);
    }

    public int useSnapshotState(int module) {
        if (useGoogle()) {
            info("null not use snapshot %s", moduleToStr(module));
            return 2;
        }
        info("use snapshot %s", moduleToStr(module));
        return 3;
    }

    private boolean isLauncherHome(WindowState win) {
        if (win == null) {
            return false;
        }
        return isLauncherPkg(win.getOwningPackage());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void createSnapshotWindowInternal(int module) {
        if (!this.mSnapshotAlpha) {
            timeoutCreateWindow(module);
        }
        this.mSnapshotCreate |= module;
        info("createWindow(module:%d, snapshotPortrait:%b, fingerprint:%b, face:%b)", Integer.valueOf(module), Boolean.valueOf(this.mSnapshotPortrait), Boolean.valueOf(isModuleCreate(2)), Boolean.valueOf(isModuleCreate(1)));
        if (useGoogle()) {
            info("mSnapshotBmp == null or google unlock:" + this.mGoogleReason, new Object[0]);
        } else if (this.mSnapshotView != null) {
            warning("mSnapshotView exist", new Object[0]);
        } else {
            this.mLatch = new CountDownLatch(1);
            this.mSnapshotVisible = false;
            this.mSnapshotView = new ImageView(this.mContext);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            this.mWindowLp = layoutParams;
            layoutParams.width = -1;
            this.mWindowLp.height = -1;
            this.mWindowLp.format = -3;
            this.mWindowLp.setTitle("SnapshotWindow");
            this.mWindowLp.flags = 1792;
            this.mWindowLp.layoutInDisplayCutoutMode = 3;
            this.mWindowLp.setFitInsetsTypes(0);
            this.mWindowLp.alpha = SNAPSHOTWINDOW_ZERO_ALPHA;
            this.mWindowLp.systemUiVisibility = this.mSysUiVis;
            this.mWindowLp.privateFlags = 16;
            this.mWindowLp.flags |= 24;
            this.mWindowLp.type = 2006;
            int i = this.mWallpaperState;
            if (i == 1) {
                this.mSnapshotView.setBackgroundColor(0);
                WindowManager.LayoutParams layoutParams2 = this.mWindowLp;
                layoutParams2.flags = 1048576 | layoutParams2.flags;
            } else if (i == 2) {
                WindowManager.LayoutParams layoutParams3 = this.mWindowLp;
                layoutParams3.flags = 1048576 | layoutParams3.flags;
                this.mSnapshotView.setBackgroundColor(0);
            } else {
                Drawable drawable = this.mBackgroundDrawable;
                if (drawable != null) {
                    this.mSnapshotView.setBackground(drawable);
                } else {
                    this.mSnapshotView.setBackgroundColor(-16777216);
                }
            }
            boolean launcherAnimEnable = this.mLauncherAnimEnable && isLauncherHome(this.mSnapshotUsedWindow);
            if (launcherAnimEnable && !this.mWinList.isEmpty() && this.mWinList.size() > 1) {
                launcherAnimEnable = false;
            }
            this.mUnlockLauncherPlayAnim = launcherAnimEnable;
            if (!launcherAnimEnable) {
                if (!this.mSnapshotPortrait) {
                    Bitmap bitmap = this.mLandscapeBitmap;
                    if (bitmap != null) {
                        this.mSnapshotView.setImageBitmap(bitmap);
                    }
                } else {
                    this.mSnapshotView.setImageBitmap(this.mSnapshotBmp);
                }
            }
            if (SystemProperties.getInt("persist.snapshot.dbg", 0) == 1) {
                this.mSnapshotView.setImageAlpha(60);
                this.mSnapshotView.setBackgroundColor(2013200384);
            }
            info("add view (mWallpaperState:%d, launcherAnimEnable:%b)", Integer.valueOf(this.mWallpaperState), Boolean.valueOf(launcherAnimEnable));
            this.mSnapshotDrawn = false;
            this.mWindowManager.addView(this.mSnapshotView, this.mWindowLp);
            notifyWallpaper(PHASE_UNLOCK_START);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSnapshotVisibilityInternal(int module, boolean show) {
        if (show) {
            this.mUnlockModule = module;
        }
        if (!useGoogle() && this.mSnapshotView != null && this.mWindowLp != null) {
            info("snapshot set setVisibility(module:%s, show:%b)", moduleToStr(module), Boolean.valueOf(show));
            this.mShownTime = SystemClock.elapsedRealtime();
            this.mSnapshotVisible = show;
            boolean z = this.mUnlockLauncherPlayAnim;
            float f = SNAPSHOTWINDOW_ZERO_ALPHA;
            if (z && show) {
                this.mWindowLp.alpha = SNAPSHOTWINDOW_ZERO_ALPHA;
            } else {
                WindowManager.LayoutParams layoutParams = this.mWindowLp;
                if (show) {
                    f = 1.0f;
                }
                layoutParams.alpha = f;
            }
            if (show) {
                if (!this.mSnapshotPortrait) {
                    if (this.mDisplayRotation == 3) {
                        debug("updateLayoutLandscape REVERSE_LANDSCAPE", new Object[0]);
                        this.mWindowLp.screenOrientation = 8;
                    } else {
                        debug("updateLayoutLandscape LANDSCAPE", new Object[0]);
                        this.mWindowLp.screenOrientation = 0;
                    }
                } else {
                    this.mWindowLp.screenOrientation = 1;
                }
            }
            this.mWindowManager.updateViewLayout(this.mSnapshotView, this.mWindowLp);
        } else if (show && module == 2) {
            this.mGoogleSpeedAnalysis = this.mAuthInteractive ? 1 : 3;
            try {
                ActivityRecord activityRecord = getTopRunningActivity();
                if (activityRecord != null) {
                    this.mRecordFinishDrawingWin = activityRecord.findMainWindow();
                    this.mHandler.sendEmptyMessageDelayed(16, DELAY_CREATED_REMOVE_SHORT);
                }
            } catch (Exception e) {
                warning("Exception: ", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeWindow(boolean isUnlock) {
        if (this.mSnapshotView != null) {
            long curTime = SystemClock.elapsedRealtime();
            long diffTime = curTime - this.mStartRemoveWinTime;
            info("After " + diffTime + "ms remove SnapshotWindow. unlock=" + isUnlock, new Object[0]);
            long j = this.mShownTime;
            if (j != 0) {
                long keyguardExitToRemoveTime = curTime - this.mKeyguardExitTime;
                long showTime = curTime - j;
                info("show:" + showTime + ", unlock diff:" + keyguardExitToRemoveTime, new Object[0]);
            }
            this.mLatch = null;
            this.mSnapshotVisible = false;
            this.mStartRemoveWinTime = 0L;
            this.mUnlockAndDrawnTime = 0L;
            this.mKeyguardExitTime = 0L;
            this.mShownTime = 0L;
            this.mAfterUnlockFocusWindowGainedFocus = false;
            this.mSnapshotCreate = 0;
            hideKeyguard(false);
            setSnapshotVisibility(0, false);
            Trace.traceBegin(2L, "removeSnapshot");
            this.mWindowManager.removeViewImmediate(this.mSnapshotView);
            Trace.traceEnd(2L);
            this.mSnapshotView.setImageDrawable(null);
            this.mSnapshotView = null;
            this.mSC.hide();
        }
        if (isUnlock) {
            this.mHandler.sendEmptyMessage(19);
            debug("clear state", new Object[0]);
            this.mSnapshotBmp = null;
            this.mBackgroundDrawable = null;
            this.mHasSurfaceView = false;
            this.mSnapshotAlpha = false;
        }
    }

    private void firstFastLightLcd() {
        DisplayManagerInternal displayManagerInternal;
        if (IS_CAPACITIVE && (displayManagerInternal = this.mDisplayManagerInternal) != null) {
            displayManagerInternal.unlockFastSetBrightness();
        }
    }

    private void hideWinUponSnapshotWin(int module) {
        synchronized (this.mWMS.mGlobalLock) {
            if (this.mWallpaperState != 0) {
                notifyWallpaper(PHASE_UNLOCK_SUCCESS);
                surfaceAlpha(this.mWinWallpaper, 1.0f);
                surfaceShow(this.mWinWallpaper);
            }
            if (needHideKeyguard()) {
                hideKeyguard(true);
            }
            surfaceHide(this.mWinNotificationShade, false);
            surfaceAlpha(this.mWinAod, SNAPSHOTWINDOW_ZERO_ALPHA);
            if (IS_UDFINGERPRINT && (module == 1 || (module == 2 && !this.mSnapshotPortrait))) {
                surfaceHide(this.mWinFingerprintUiIcon, false);
            }
        }
    }

    private void setAlpha(float alpha, int module) {
        boolean useGoogleUnlock = useGoogle();
        boolean alphaShow = alpha == 1.0f;
        if (!useGoogleUnlock && alphaShow && !this.mSnapshotDrawn) {
            if (this.mLatch == null) {
                warning("set useGoogleUnlock", new Object[0]);
                useGoogleUnlock = true;
            } else {
                warning("wait SnapshotDrawn in", new Object[0]);
                try {
                    this.mLatch.await(WAIT_SNAPSHOTWIN_FINISHDRAW_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e1) {
                    warning("sleep " + e1, new Object[0]);
                    try {
                        Thread.sleep(WAIT_SNAPSHOTWIN_FINISHDRAW_TIMEOUT);
                    } catch (Exception e2) {
                        warning("sleep2 " + e2, new Object[0]);
                    }
                }
                warning("wait SnapshotDrawn end", new Object[0]);
            }
        }
        if (!useGoogleUnlock && surfaceControlNotNull(this.mWinSnapshot)) {
            info("setVisibility setAlpha " + alpha + " " + this.mUnlockLauncherPlayAnim, new Object[0]);
            this.mSnapshotAlpha = alphaShow;
            if (alphaShow) {
                hideWinUponSnapshotWin(module);
                restoreShowKeyguardDelay(VivoPKMSLocManager.MAX_LOCATION_WAIT_TIME);
                if (!this.mDeviceInteractive && module == 2) {
                    firstFastLightLcd();
                }
                AnalysisManager.trace("reqSnapshot");
            }
            synchronized (this.mWMS.mGlobalLock) {
                if (this.mUnlockLauncherPlayAnim && alphaShow) {
                    surfaceAlpha(this.mWinSnapshot, SNAPSHOTWINDOW_ZERO_ALPHA);
                } else {
                    surfaceAlpha(this.mWinSnapshot, alpha);
                }
                if (IS_CAPACITIVE && alphaShow) {
                    surfaceShow(this.mWinSnapshot, false);
                    surfaceAlphaForceApply(this.mWinBlack, SNAPSHOTWINDOW_ZERO_ALPHA, true);
                } else {
                    surfaceShow(this.mWinSnapshot, alphaShow);
                }
            }
            info("setVisibility setAlpha end", new Object[0]);
            if (alphaShow) {
                AnalysisManager.trace("showSnapshot");
                if (IS_CAPACITIVE && this.mAuthInteractive) {
                    AnalysisManager.trace("onSpeedExit");
                }
            }
            if (alphaShow) {
                try {
                    if (!this.mDeviceInteractive && IS_UDFINGERPRINT && this.mVivoSensorImpl != null) {
                        this.mVivoSensorImpl.notifyDisplayState(2, 1);
                        info("setAlpha end, notify display state", new Object[0]);
                    }
                } catch (Exception e) {
                    warning("disableUnderProximity " + e, new Object[0]);
                }
            }
        } else if (1.0f == alpha && !this.mDeviceInteractive) {
            StringBuilder sb = new StringBuilder();
            sb.append("goolge setAlpha 1.0  useGoogle=");
            sb.append(useGoogle() ? "true" : "surfaceControl Null");
            sb.append(" ");
            sb.append(this.mWinSnapshot);
            info(sb.toString(), new Object[0]);
            synchronized (this.mWMS.mGlobalLock) {
                if (needHideKeyguard()) {
                    hideKeyguard(true);
                }
                surfaceHide(this.mWinNotificationShade, true);
                restoreShowKeyguardDelay(VivoPKMSLocManager.MAX_LOCATION_WAIT_TIME);
            }
        }
        if (getFaceInternal() != null) {
            getFaceInternal().systemTime(SystemClock.elapsedRealtime(), 1);
        }
    }

    public void notifyRotationChanged(int rotation) {
        if (!checkPermission()) {
            return;
        }
        this.mHandler.obtainMessage(11, rotation, 0).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyRotationChangedInternal(int rotation) {
        debug("onRotationChanged=" + rotation, new Object[0]);
        if (this.mSnapshotView != null) {
            if (rotation == 1 || rotation == 3) {
                if (DEBUG_SNAPSHOT) {
                    this.mSnapshotView.setBackgroundColor(2012217344);
                }
                if (this.mSnapshotBmp != null) {
                    this.mSnapshotView.setImageBitmap(this.mSnapshotBmp);
                } else {
                    warning("mSnapshotBmp is null", new Object[0]);
                }
            }
        }
    }

    public void snapshotUnlockCloseAnim(boolean closeAnim) {
        debug("snapshotUnlockCloseAnim:" + closeAnim, new Object[0]);
        this.mSnapshotUnlockClosedTransitionAnim = closeAnim;
    }

    public void notifykeyguardViewVisibleChange(int state) {
        if (checkPermission() && state == -200 && this.mHandler.hasMessages(17)) {
            restoreShowKeyguardDelay(17);
        }
    }

    public boolean transitionAnimIsClosed() {
        if (this.mSnapshotPortrait) {
            return false;
        }
        debug("transitionAnimIsClosed:" + this.mSnapshotUnlockClosedTransitionAnim, new Object[0]);
        return this.mSnapshotUnlockClosedTransitionAnim;
    }

    public boolean snapshotWinIsVisible() {
        return this.mSnapshotVisible || isIMEWindowShowingWhenFingerOrFaceUnlock();
    }

    public boolean needCancleAnim() {
        return this.mSnapshotVisible;
    }

    public void onFreezingDisplay(boolean start, int frozenDisplayId, long startFreezeTime) {
        if (start && this.mUnlockModule == 2 && this.mSnapshotAlpha && this.mSnapshotPortrait) {
            surfaceHide(this.mWinFingerprintUiIcon, true);
            debug("Portrait freezing hide FingerprintUiIcon", new Object[0]);
        }
    }

    public float showDimLayerGetForceAlpha(WindowState w, WindowContainer host, float alpha) {
        if (!this.mForceShowingDimLayer || host == null) {
            return alpha;
        }
        if (!host.getName().equals(DIMLAYER_HOST_NAME)) {
            return alpha;
        }
        if (this.mWinDimLayer == w) {
            this.mForceShowingDimLayer = false;
            debug("end force show dimmer " + this.mDimLayerAlpha + " " + w, new Object[0]);
            return alpha;
        }
        return this.mDimLayerAlpha;
    }

    private void createDimLayerShow() {
        DisplayContent displayContent;
        WindowManagerService windowManagerService;
        if (this.mForceShowingDimLayer && surfaceControlNotNull(this.mWinSnapshot) && (displayContent = defaultDisplayContentLw()) != null && (windowManagerService = this.mWMS) != null) {
            try {
                synchronized (windowManagerService.mGlobalLock) {
                    this.mWMS.requestTraversal();
                    SurfaceControl.Transaction t = displayContent.mRootDisplayArea.getPendingTransaction();
                    Dimmer mDimmer = displayContent.mRootDisplayArea.getDimmer();
                    mDimmer.forceShow(t, this.mSnapshotUsedWindow.getRootTask().getSurfaceControl(), this.mDimLayerAlpha);
                    SurfaceControl.mergeToGlobalTransaction(t);
                    this.mHandler.sendEmptyMessageDelayed(22, SHOW_DIM_TIMEOUT);
                }
            } catch (Exception e) {
                warning("requestTraversal ", e);
            }
        }
    }

    private void needTraversal() {
        try {
            synchronized (this.mWMS.mGlobalLock) {
                this.mWMS.requestTraversal();
            }
        } catch (Exception e) {
            warning("requestTraversal ", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showDimTimeOut() {
        if (this.mForceShowingDimLayer) {
            this.mForceShowingDimLayer = false;
            debug("TimeOut end force show dimmer", new Object[0]);
            needTraversal();
        }
    }

    private void showDimLayerAlpha() {
        DisplayContent displayContent;
        if (this.mForceShowingDimLayer && (displayContent = defaultDisplayContentLw()) != null) {
            SurfaceControl.Transaction t = displayContent.mRootDisplayArea.getPendingTransaction();
            Dimmer mDimmer = displayContent.mRootDisplayArea.getDimmer();
            mDimmer.forceShow(t, this.mSnapshotUsedWindow.getRootTask().getSurfaceControl(), this.mDimLayerAlpha);
        }
    }

    public boolean fastUnlockForceShowDimLayer(WindowState w) {
        if (!this.mForceShowingDimLayer || w == null || w.mAttrs == null) {
            return false;
        }
        if (this.mSnapshotView != null) {
            if ("SnapshotWindow".equals(w.mAttrs.getTitle())) {
                showDimLayerAlpha();
                return true;
            }
        } else if (this.mSnapshotUsedWindow == w) {
            Task task = w.getTask();
            if (task == null || !task.shouldBeVisible((ActivityRecord) null)) {
                this.mForceShowingDimLayer = false;
                debug("topWin invisible end force show dimmer " + w, new Object[0]);
                return false;
            }
            showDimLayerAlpha();
            return true;
        }
        return false;
    }

    private void removeDimLayerWin(WindowState win) {
        if (this.mWinDimLayer == win) {
            this.mWinDimLayer = null;
            this.mDimLayerAlpha = SNAPSHOTWINDOW_ZERO_ALPHA;
            VSlog.d("SnapshotWindow", "removeDimLayerWin " + this.mForceShowingDimLayer + " " + win);
            this.mForceShowingDimLayer = false;
        }
    }

    public int unlockEarlySetStatusBarColor(boolean isKeyguardShowing, WindowState focusedWindow, int fullscreenVisibility, int dockedVisibility) {
        if (checkPermission() && this.mSetStatusBarColor && this.mSnapshotAlpha && this.mWinSnapshot != null && !useGoogle()) {
            if (!isKeyguardShowing && this.mStatusBarColorLog > 0) {
                this.mStatusBarColorLog = 0;
            }
            int i = this.mStatusBarColorLog + 1;
            this.mStatusBarColorLog = i;
            if ((i & 15) == 1) {
                debug("unlockEarlySetStatusBarColor " + isKeyguardShowing + " " + focusedWindow, new Object[0]);
                return 8;
            }
            return 8;
        }
        return 0;
    }

    private boolean getTopHasSurfaceView() {
        ActivityRecord focusApp = this.mTopActivity;
        boolean hasSurfaceView = false;
        int windowMode = 1;
        try {
            if (focusApp != null) {
                hasSurfaceView = focusApp.isActivityTypeHome() ? false : focusApp.mHasSurfaceView;
                windowMode = focusApp.getWindowingMode();
                info("getTopHasSurfaceView windowMode=" + windowMode + ",hasSurfaceView=" + hasSurfaceView, new Object[0]);
            } else {
                warning("getTopHasSurfaceView focusApp is null", new Object[0]);
            }
            TaskDisplayArea taskDisplayArea = this.mWMS.mRoot.getDefaultTaskDisplayArea();
            if (taskDisplayArea == null || windowMode != 3) {
                if (taskDisplayArea != null && windowMode == 4) {
                    ActivityStack primaryStack = taskDisplayArea.getTopStackInWindowingMode(3);
                    if (primaryStack != null && primaryStack.getTopChild() != null && primaryStack.getTopMostTask().getTopVisibleActivity() != null) {
                        boolean hasSurfaceView2 = hasSurfaceView | primaryStack.getTopMostTask().getTopVisibleActivity().mHasSurfaceView;
                        info("getTopHasSurfaceView. primaryStack surfaceview =" + hasSurfaceView2, new Object[0]);
                        return hasSurfaceView2;
                    }
                    return hasSurfaceView;
                }
            } else {
                ActivityStack secondStack = taskDisplayArea.getTopStackInWindowingMode(4);
                if (secondStack != null && secondStack.getTopChild() != null && secondStack.getTopMostTask().getTopVisibleActivity() != null) {
                    hasSurfaceView |= secondStack.getTopMostTask().getTopVisibleActivity().mHasSurfaceView;
                    info("getTopHasSurfaceView. secondStack surfaceview =" + hasSurfaceView, new Object[0]);
                }
            }
            return hasSurfaceView;
        } catch (Exception e) {
            warning("getTopHasSurfaceView. Exception: ", e);
            return false;
        }
    }

    private int checkXline0Alpha(ByteBuffer buffer, int width, int y, int pixelStride, int rowStride) {
        int x = 1;
        int alpha = -1;
        int offset = (y * rowStride) + (1 * pixelStride);
        while (true) {
            if (x >= width - 1) {
                break;
            }
            alpha = buffer.get(offset + 3) & 255;
            if (alpha < 255) {
                debug("(" + x + ", " + y + ") alpha=" + alpha + " (0,0)=" + (255 & buffer.get(3)), new Object[0]);
                break;
            }
            offset += pixelStride;
            x++;
        }
        return alpha;
    }

    private int getGraphicBufferAlpha(GraphicBuffer graphicBuffer, ColorSpace colorSpace, int checkType) {
        if (graphicBuffer != null && graphicBuffer.getFormat() == 1) {
            ImageReader ir = ImageReader.newInstance(graphicBuffer.getWidth(), graphicBuffer.getHeight(), graphicBuffer.getFormat(), 1);
            ir.getSurface().attachAndQueueBufferWithColorSpace(graphicBuffer, colorSpace);
            Image image = ir.acquireLatestImage();
            if (image == null || image.getPlanes().length == 0) {
                ir.close();
                return -2;
            }
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            int width = image.getWidth();
            int height = image.getHeight();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int alpha = -1;
            if (checkType == 2) {
                int y = height / 2;
                int offset = (y * rowStride) + (8 * pixelStride);
                int alpha2 = buffer.get(offset + 3) & 255;
                debug("(8, h/2)  alpha=" + alpha2 + "  (0,0)=" + (buffer.get(3) & 255), new Object[0]);
                alpha = alpha2;
            } else if (checkType == 1) {
                int i = this.mStatusbarHeight;
                int y2 = i + (i >> 1);
                int alpha3 = checkXline0Alpha(buffer, width, y2, pixelStride, rowStride);
                if (alpha3 != 255) {
                    alpha = alpha3;
                } else {
                    int y3 = height - (this.mStatusbarHeight << 1);
                    alpha = alpha3;
                    checkXline0Alpha(buffer, width, y3, pixelStride, rowStride);
                }
            }
            ir.close();
            return alpha;
        }
        return -1;
    }

    private ActivityManager.TaskSnapshot findAlpha255TopFullscreenOpaqueWinLw(ActivityRecord activityRecord) {
        ActivityManager.TaskSnapshot taskSnapshot = this.mWMS.mTaskSnapshotController.fingerprintGetSnapshot(activityRecord);
        if (taskSnapshot == null) {
            return null;
        }
        int alpha = getGraphicBufferAlpha(taskSnapshot.getSnapshot(), taskSnapshot.getColorSpace(), 1);
        if (alpha < 0) {
            warning("google : topFullOpaqueWin alpha " + alpha, new Object[0]);
            return null;
        } else if (alpha == 255) {
            return taskSnapshot;
        } else {
            ActivityManager.TaskSnapshot shot = null;
            int size = this.mOtherFullOpaqueWindows.size();
            ActivityRecord fullOpaqueR = null;
            WindowState fullOpaqueW = null;
            int i = 0;
            while (i < size) {
                WindowState fullOpaqueW2 = this.mOtherFullOpaqueWindows.get(i);
                fullOpaqueW = fullOpaqueW2;
                if (fullOpaqueW != null) {
                    try {
                        fullOpaqueR = fullOpaqueW.mActivityRecord;
                    } catch (Exception ex) {
                        fullOpaqueR = null;
                        warning("no Activity of " + fullOpaqueW + ".  " + ex, new Object[0]);
                    }
                }
                if (fullOpaqueR != null) {
                    shot = this.mWMS.mTaskSnapshotController.fingerprintGetSnapshot(fullOpaqueR);
                    if (shot == null) {
                        return null;
                    }
                    if (fullOpaqueR.isActivityTypeHome()) {
                        break;
                    }
                    int alpha2 = getGraphicBufferAlpha(shot.getSnapshot(), shot.getColorSpace(), 1);
                    if (alpha2 < 0) {
                        warning("google : ext topFullOpaqueWin alpha " + alpha2 + ", " + fullOpaqueW, new Object[0]);
                        return null;
                    } else if (alpha2 == 255) {
                        break;
                    }
                }
                i++;
            }
            if (fullOpaqueR != null) {
                this.mTopActivity = fullOpaqueR;
                this.mSnapshotUsedWindow = fullOpaqueW;
                debug("topFullscreenOpaqueWin to [" + i + "] " + fullOpaqueW, new Object[0]);
                this.mSnapshotUseWinIsChanged = true;
            }
            return shot;
        }
    }

    private ActivityManager.TaskSnapshot doSnapshot(ActivityRecord activityRecord) {
        if (activityRecord == null || this.mWMS.mTaskSnapshotController == null) {
            warning("getSnapshot. Class is null " + activityRecord, new Object[0]);
            this.mGoogleReason = "class";
            return null;
        }
        int winMode = activityRecord.getWindowingMode();
        if (winMode == 2 || winMode == 5) {
            Object[] objArr = new Object[1];
            objArr[0] = winMode == 2 ? "PINNED" : "FREEFORM";
            debug("getSnapshot screen %s", objArr);
        } else if (winMode != 1) {
            warning("getSnapshot. Not full screen", new Object[0]);
            this.mGoogleReason = "split";
            return null;
        }
        long startSnapshot = 0;
        if (DEBUG_SNAPSHOT) {
            startSnapshot = SystemClock.elapsedRealtime();
        }
        ActivityManager.TaskSnapshot taskSnapshot = null;
        try {
            synchronized (this.mWMS.mGlobalLock) {
                t1 = DEBUG_SNAPSHOT ? SystemClock.elapsedRealtime() : 0L;
                if (!this.hasMultiFullOpaqueWinShowOnScreen) {
                    taskSnapshot = this.mWMS.mTaskSnapshotController.fingerprintGetSnapshot(activityRecord);
                } else {
                    taskSnapshot = findAlpha255TopFullscreenOpaqueWinLw(activityRecord);
                }
            }
        } catch (Exception e) {
            warning("getSnapshot. Exception: " + e, new Object[0]);
        }
        if (taskSnapshot == null) {
            this.mGoogleReason = "snapshot_null";
        }
        if (DEBUG_SNAPSHOT) {
            long t2 = SystemClock.elapsedRealtime();
            if (t2 - startSnapshot > 100) {
                warning("capturetime " + (t2 - startSnapshot) + ", wait lock " + (t2 - t1), new Object[0]);
            }
        }
        return taskSnapshot;
    }

    private void updateTopActivity(ActivityRecord snapshotActivity, WindowState snapshotWindow) {
        this.mWinList.clear();
        if (snapshotActivity == null || snapshotWindow == null) {
            return;
        }
        try {
            if (snapshotActivity.mChildren != null) {
                ArrayList<WindowState> children = new ArrayList<>((Collection<? extends WindowState>) snapshotActivity.mChildren);
                for (int i = 0; i < children.size(); i++) {
                    WindowState w = children.get(i);
                    if (w != null && w.mAttrs != null && snapshotWindow.mAttrs != null && w.mWinAnimator != null && ((isHomeHiBoard(w) || TextUtils.equals(w.mAttrs.getTitle(), snapshotWindow.mAttrs.getTitle())) && w.mWinAnimator.mShownAlpha > SNAPSHOTWINDOW_ZERO_ALPHA)) {
                        debug("add waiting windowState: " + w, new Object[0]);
                        this.mWinList.add(new WaitingWindowState(w, false));
                    }
                }
            }
        } catch (Exception e) {
            warning("updateTopActivity. Exception: ", e);
        }
    }

    private boolean isHomeHiBoard(WindowState w) {
        return w.isFocused() || (this.mSnapshotUsedWindow.isActivityTypeHome() && w.isVisible() && w.mAttrs.alpha == 1.0f && w.getFrameLw().left >= 0);
    }

    private String toActivityString(ActivityRecord r) {
        if (r != null && r.mActivityComponent != null) {
            return r.mActivityComponent.flattenToShortString();
        }
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    private String toSubString(String str) {
        if (str.isEmpty()) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        return str.substring(0, Math.min(100, str.length()));
    }

    private boolean focusAppIsInActivityList(String[] activityList) {
        String focusActivity = toActivityString(this.mTopActivity);
        if (!focusActivity.isEmpty()) {
            for (String item : activityList) {
                if (focusActivity.startsWith(item)) {
                    info("In use activity list", new Object[0]);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPrivacyActivity(ActivityRecord r) {
        String topActivity = toActivityString(r);
        if (!topActivity.isEmpty() && topActivity.startsWith("com.android.settings/com.vivo.settings.secret.PasswordActivity")) {
            return true;
        }
        return false;
    }

    private void getDefaultDisplayRotation() {
        DisplayContent displayContent = defaultDisplayContentLw();
        if (displayContent != null) {
            this.mDisplayRotation = displayContent.getRotation();
        }
        debug("getDefaultDisplayRotation:" + this.mDisplayRotation, new Object[0]);
    }

    private boolean isDisplayLandscape() {
        int i = this.mDisplayRotation;
        return i == 1 || i == 3;
    }

    private void getAppFixOrientation(ActivityRecord activityRecord) {
        if (activityRecord != null && activityRecord.info != null) {
            int orientation = activityRecord.info.screenOrientation;
            ActivityInfo activityInfo = activityRecord.info;
            this.mIsFixedOrientationLandscape = ActivityInfo.isFixedOrientationLandscape(orientation);
            info("getAppFixOrientation=" + orientation + ", fixedLandscape=" + this.mIsFixedOrientationLandscape, new Object[0]);
        }
    }

    public void onFinishedWakingUp() {
        if (!checkPermission()) {
            return;
        }
        this.mFpAuthSuccess = false;
        this.mHandler.sendEmptyMessage(3);
    }

    public void onStartedGoingToSleep() {
        if (!checkPermission()) {
            return;
        }
        this.mFpAuthSuccess = false;
        this.mForceShowingDimLayer = false;
        this.mSnapshotUseWinIsChanged = false;
        getTopFullscreenOpaqueWinBeforeLock();
        WindowState win = this.mTopFullscreenOpaqueWin;
        this.mHandler.obtainMessage(4, win).sendToTarget();
    }

    private void checkUnlockEndAndRemoveSnapshot(boolean unlockOrDrawn) {
        if (!this.mSnapshotVisible) {
            return;
        }
        boolean unlockEnd = false;
        if (this.mWallpaperState != 0) {
            unlockEnd = this.mAfterUnlockFocusWindowGainedFocus && this.mTopActivityDrawn && !this.mKeyguardShowing;
        } else if (unlockOrDrawn) {
            unlockEnd = this.mTopActivityDrawn && !this.mKeyguardShowing;
        }
        if (unlockEnd) {
            timeoutRemoveSnapshot(getUnlockRemoveTime(), true);
            this.mUnlockAndDrawnTime = SystemClock.elapsedRealtime();
        }
        debug("KeyguardShown:%b, drawn:%b, GainedFocus:%b", Boolean.valueOf(this.mKeyguardShowing), Boolean.valueOf(this.mTopActivityDrawn), Boolean.valueOf(this.mAfterUnlockFocusWindowGainedFocus));
    }

    public void onKeyguardShown(boolean showing) {
        if (!checkPermission()) {
            return;
        }
        Object[] objArr = new Object[3];
        objArr[0] = Boolean.valueOf(showing);
        objArr[1] = Boolean.valueOf(this.mTopActivityDrawn);
        objArr[2] = (showing || !this.mSetStatusBarColor) ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : "end unlockEarlySetStatusBarColor";
        debug("onKeyguardShown(showing:%b, drawn:%b) %s", objArr);
        this.mKeyguardShowing = showing;
        if (!showing) {
            this.mGoogleReason = "null";
            this.mSetStatusBarColor = false;
            this.mStartAppEnable = false;
            this.mKeyguardExitTime = SystemClock.elapsedRealtime();
            AnalysisManager.trace("keyguardShownFalse");
            if (this.mHandler.hasMessages(17)) {
                restoreShowKeyguardDelay(DELAY_RESTRORE_SHOW_KEYGUARD_GOOGLE);
            }
            if (this.mSnapshotView != null) {
                timeoutRemoveSnapshot(DELAY_UNLOCKED_REMOVE_MAX, true);
            }
            checkUnlockEndAndRemoveSnapshot(true);
            if (this.mSnapshotBmp == null) {
                hideKeyguard(false);
            }
            if (this.mGoogleSpeedAnalysis == 2) {
                googleSpeedExit("onSpeedExit");
            }
        }
    }

    private void notifyStatusBarSetColor() {
        DisplayContent displayContent;
        if (this.mSetStatusBarColor && (displayContent = defaultDisplayContentLw()) != null) {
            synchronized (this.mWMS.mGlobalLock) {
                displayContent.getDisplayPolicy().updateSystemUiVisibilityLw();
            }
        }
    }

    private void getTopFullscreenOpaqueWinBeforeLock() {
        DisplayContent displayContent;
        if (this.mKeyguardShowing) {
            warning("KeyguardShowing TopFullscreenOpaqueWin invalid", new Object[0]);
            return;
        }
        int num = 0;
        this.hasMultiFullOpaqueWinShowOnScreen = false;
        boolean holdsLock = false;
        try {
            displayContent = defaultDisplayContentLw();
        } catch (Exception ex) {
            warning("getTopFullscreenOpaqueWindow Exception: " + ex, new Object[0]);
        }
        if (displayContent == null) {
            warning("displayContent null.", new Object[0]);
            return;
        }
        holdsLock = Thread.holdsLock(this.mWMS.mGlobalLock);
        DisplayPolicy policy = displayContent.getDisplayPolicy();
        if (holdsLock) {
            num = getFullscreenOpaqueWin(policy);
        } else {
            synchronized (this.mWMS.mGlobalLock) {
                num = getFullscreenOpaqueWin(policy);
            }
        }
        this.mSetStatusBarColor = false;
        WindowState windowState = this.mTopFullscreenOpaqueWin;
        if (windowState != null) {
            int tmpVisibility = PolicyControl.getSystemUiVisibility(windowState, (WindowManager.LayoutParams) null);
            if ((tmpVisibility & EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP) != 0) {
                this.mSetStatusBarColor = true;
                this.mStatusBarColorLog = 0;
            }
        } else {
            warning("TopFullscreenOpaqueWin null", new Object[0]);
        }
        debug("holds=" + holdsLock + ", statusBar invert color=" + this.mSetStatusBarColor + ", fullW num=" + num, new Object[0]);
    }

    private int getFullscreenOpaqueWin(DisplayPolicy policy) {
        ArrayList<WindowState> otherFullscreenOpaqueWindowsLw;
        this.hasMultiFullOpaqueWinShowOnScreen = false;
        WindowState topFullscreenOpaqueWindow = policy.getTopFullscreenOpaqueWindow();
        this.mTopFullscreenOpaqueWin = topFullscreenOpaqueWindow;
        if (topFullscreenOpaqueWindow != null && isLauncherPkg(topFullscreenOpaqueWindow.getOwningPackage())) {
            return 1;
        }
        int size = 0;
        try {
            otherFullscreenOpaqueWindowsLw = policy.getOtherFullscreenOpaqueWindowsLw();
            this.mOtherFullOpaqueWindows = otherFullscreenOpaqueWindowsLw;
        } catch (Exception ex) {
            warning("get OtherFullscreenOpaqueWin Exception: " + ex, new Object[0]);
        }
        if (otherFullscreenOpaqueWindowsLw != null && !otherFullscreenOpaqueWindowsLw.isEmpty() && (size = this.mOtherFullOpaqueWindows.size()) != 0 && size <= 4) {
            this.hasMultiFullOpaqueWinShowOnScreen = true;
            return size + 1;
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStartedGoingToSleepInternal() {
        this.mDeviceInteractive = false;
        this.mAuthInteractive = false;
        this.mTopActivityDrawn = false;
        this.mImeWindowVisible = false;
        this.mAfterUnlockFocusWindowGainedFocus = false;
        this.mSnapshotUnlockClosedTransitionAnim = false;
        this.mUnlockModule = 0;
        hideKeyguard(false);
        if (!this.mKeyguardShowing) {
            this.mGoogleReason = "null";
            this.mRecordFinishDrawingWin = null;
            this.mGoogleSpeedAnalysis = 0;
            this.mHandler.removeMessages(16);
            timeoutRemoveSnapshot(0L, true);
            getDefaultDisplayRotation();
            ActivityRecord activityOfWindow = activityOfWindow(this.mSnapshotUsedWindow);
            this.mTopActivity = activityOfWindow;
            this.mScreenOffActivity = activityOfWindow;
            getAppFixOrientation(activityOfWindow);
            if (!setGoogleUnlock()) {
                setSnapshotBmp();
            }
            setWallpaper();
        } else {
            timeoutRemoveSnapshot(0L);
        }
        synchronized (this.mWMS.mGlobalLock) {
            surfaceShow(this.mWinFingerprintUiIcon);
            if (this.mHandler.hasMessages(17)) {
                this.mHandler.removeMessages(17);
                surfaceShow(this.mWinNotificationShade);
            }
            info("StartedGoingToSleep show end", new Object[0]);
        }
    }

    private void setWallpaper() {
        if (!this.mWallpaperVisible) {
            WindowState windowState = this.mSnapshotUsedWindow;
            if (windowState != null && windowState.mAttrs != null && (this.mSnapshotUsedWindow.mAttrs.flags & 1048576) != 0) {
                if (getWallpaperManager().getWallpaperInfo() == null) {
                    this.mWallpaperState = 1;
                    return;
                } else {
                    this.mWallpaperState = 2;
                    return;
                }
            }
            this.mWallpaperState = 0;
        } else if (getWallpaperManager().getWallpaperInfo() == null) {
            this.mWallpaperState = 1;
        } else {
            this.mWallpaperState = 2;
        }
    }

    private boolean setGoogleUnlock() {
        boolean isAppExitingOrOpening = false;
        try {
            if (this.mWMS.getRecentsAnimationController() != null) {
                isAppExitingOrOpening = this.mWMS.getRecentsAnimationController().isAnimRunning();
            }
        } catch (Exception e) {
            warning("RecentsAnimationController == null," + e, new Object[0]);
        }
        if (isAppExitingOrOpening && (!this.mLauncherAnimEnable || !isLauncherPkg(toActivityString(this.mTopActivity)))) {
            info("google activity " + isAppExitingOrOpening, new Object[0]);
            this.mGoogleReason = "recent_exit";
            return true;
        } else if (isGoogleActivity()) {
            return true;
        } else {
            if (!isIMEWindowShowing()) {
                return false;
            }
            info("ime window showing", new Object[0]);
            this.mImeWindowVisible = true;
            this.mGoogleReason = "input";
            return true;
        }
    }

    private boolean isLauncherPkg(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.startsWith("com.bbk.launcher") || str.startsWith("com.android.launcher");
    }

    private boolean isLaunchDrawer(ActivityManager.TaskSnapshot taskSnapshot) {
        if (taskSnapshot == null || !isLauncherHome(this.mSnapshotUsedWindow)) {
            return false;
        }
        try {
            if (this.mSnapshotUsedWindow != this.mWMS.mRoot.getTopFocusedDisplayContent().mCurrentFocus) {
                debug("launch -1 ", new Object[0]);
                return false;
            }
        } catch (Exception e) {
            warning("win mCurrentFocus == null" + e, new Object[0]);
        }
        int alpha = getGraphicBufferAlpha(taskSnapshot.getSnapshot(), taskSnapshot.getColorSpace(), 2);
        if (alpha >= 0) {
            return alpha > 96;
        }
        warning("launch alpha " + alpha, new Object[0]);
        return false;
    }

    private boolean isIMEWindowShowing() {
        DisplayContent dc;
        WindowManagerService windowManagerService = this.mWMS;
        return (windowManagerService == null || windowManagerService.mRoot == null || (dc = this.mWMS.mRoot.getDisplayContent(0)) == null || dc.mInputMethodWindow == null || !dc.mInputMethodWindow.isVisibleLw()) ? false : true;
    }

    private boolean isIMEWindowShowingWhenFingerOrFaceUnlock() {
        int i;
        return this.mImeWindowVisible && ((i = this.mUnlockModule) == 1 || i == 2);
    }

    private boolean rCanShowWhenLocked(ActivityRecord r) {
        boolean ret = false;
        synchronized (this.mWMS.mGlobalLock) {
            try {
                ret = r.canShowWhenLocked();
            } catch (Exception e) {
                VSlog.w("SnapshotWindow", "rCanShowWhenLocked err :  " + e);
            }
        }
        return ret;
    }

    private boolean winCanShowWhenLocked(WindowState w) {
        boolean ret = false;
        synchronized (this.mWMS.mGlobalLock) {
            try {
                ret = w.canShowWhenLocked();
            } catch (Exception e) {
                VSlog.w("SnapshotWindow", "winCanShowWhenLocked err :  " + e);
            }
        }
        return ret;
    }

    private boolean isGoogleActivity() {
        int i;
        if (focusAppIsInActivityList(SnapshotConfig.USE_GOOGLE_ACTIVITY_LIST)) {
            this.mGoogleReason = "list";
            return true;
        }
        WindowState windowState = this.mSnapshotUsedWindow;
        if (windowState != null) {
            if (winCanShowWhenLocked(windowState)) {
                this.mGoogleReason = "lock_show";
                return true;
            } else if (this.mSnapshotUsedWindow.isSecureLocked()) {
                this.mGoogleReason = "secure";
                return true;
            } else if (isLauncherHome(this.mSnapshotUsedWindow) && ((i = this.mDisplayRotation) == 1 || i == 3)) {
                debug("launcher is Landscape " + this.mDisplayRotation, new Object[0]);
                this.mGoogleReason = "launcher_land";
                return true;
            }
        }
        return false;
    }

    private void setSnapshotBmp() {
        ActivityManager.TaskSnapshot snapshot = doSnapshot(this.mTopActivity);
        if (snapshot == null) {
            warning("snapshot == null", new Object[0]);
            this.mSnapshotPortrait = true;
            return;
        }
        info("snapshot=" + snapshot, new Object[0]);
        updateTopActivity(this.mTopActivity, this.mSnapshotUsedWindow);
        boolean needSnapshot = true;
        boolean z = 1 == snapshot.getOrientation();
        this.mSnapshotPortrait = z;
        info("snapshotPortrait:%b, realSnapshot:%b, diaplayLandscape:%b, appFixedLandscape:%b", Boolean.valueOf(z), Boolean.valueOf(snapshot.isRealSnapshot()), Boolean.valueOf(isDisplayLandscape()), Boolean.valueOf(this.mIsFixedOrientationLandscape));
        if (this.mSnapshotPortrait && isDisplayLandscape()) {
            if (this.mIsFixedOrientationLandscape == isDisplayLandscape()) {
                this.mSnapshotPortrait = false;
            } else {
                needSnapshot = false;
            }
        }
        if (needSnapshot && snapshot.isRealSnapshot()) {
            Bitmap bmp = snapshotToBitmap(snapshot);
            if (isLauncherHome(this.mSnapshotUsedWindow) && !this.mLauncherAnimEnable && isLaunchDrawer(snapshot)) {
                this.mSnapshotBmp = null;
                this.mSnapshotUsedWindow = null;
                this.mHasSurfaceView = false;
                this.mGoogleReason = "lan_alpha";
                return;
            } else if (bmp != null) {
                this.mSnapshotBmp = bmp;
                this.mSysUiVis = snapshot.getSystemUiVisibility();
                info("snapshot is ok", new Object[0]);
            } else {
                this.mGoogleReason = "bmp_null";
            }
        }
        if (this.mSnapshotPortrait) {
            setSecondBmp();
        } else {
            drawLandscapeBitmap();
        }
        this.mHasSurfaceView = getTopHasSurfaceView();
    }

    private void setSecondBmp() {
    }

    private void drawLandscapeBitmap() {
        if (this.mSnapshotBmp != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(this.mDisplayRotation == 3 ? 270.0f : 90.0f);
            int width = this.mSnapshotBmp.getWidth();
            int height = this.mSnapshotBmp.getHeight();
            try {
                this.mLandscapeBitmap = Bitmap.createBitmap(this.mSnapshotBmp, 0, 0, width, height, matrix, true);
                info("Landscape snapshot is ok", new Object[0]);
            } catch (IllegalArgumentException e) {
                warning("IllegalArgumentException", new Object[0]);
            }
        }
    }

    public void finishDrawingLocked(WindowState win) {
        if (!checkPermission()) {
            return;
        }
        this.mHandler.obtainMessage(12, win).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finishDrawingInternal(WindowState win) {
        WindowState windowState;
        if (this.mWinSnapshot != null && win != null) {
            WindowManager.LayoutParams attr = win.mAttrs;
            CharSequence title = attr != null ? attr.getTitle() : null;
            if (TextUtils.equals(title, "SnapshotWindow")) {
                debug("SnapshotDrawn", new Object[0]);
                try {
                    this.mLatch.countDown();
                } catch (Exception e) {
                    warning("countDown Exception: " + e, new Object[0]);
                }
                this.mSnapshotDrawn = true;
                return;
            }
        }
        if (this.mSnapshotView != null && win != null && this.mSnapshotVisible) {
            if (!this.mWinList.isEmpty()) {
                if (!this.mTopActivityDrawn) {
                    boolean defaultAllActivityDrawn = true;
                    try {
                        ListIterator<WaitingWindowState> it = this.mWinList.listIterator();
                        while (it.hasNext()) {
                            WaitingWindowState wws = it.next();
                            if (activityHasWindow(wws.win)) {
                                if (wws.win == win) {
                                    wws.drawn = true;
                                    it.set(wws);
                                    info("finishDrawingInternal=" + wws + " ,allDrawn=" + defaultAllActivityDrawn + ", showing=" + this.mKeyguardShowing, new Object[0]);
                                }
                                defaultAllActivityDrawn &= wws.drawn;
                            } else {
                                it.remove();
                            }
                        }
                        if (defaultAllActivityDrawn) {
                            this.mTopActivityDrawn = true;
                            checkUnlockEndAndRemoveSnapshot(true);
                            return;
                        }
                        return;
                    } catch (Exception e2) {
                        warning("Exception: ", e2);
                        return;
                    }
                }
                return;
            }
            WindowState windowState2 = this.mSnapshotUsedWindow;
            if (windowState2 != null && windowState2 == win) {
                info("finishDrawingLocked=" + win + ", mKeyguardShowing=" + this.mKeyguardShowing, new Object[0]);
                this.mTopActivityDrawn = true;
                checkUnlockEndAndRemoveSnapshot(true);
            }
        } else if (this.mSnapshotView == null && (windowState = this.mRecordFinishDrawingWin) != null && windowState == win) {
            info("finishDrawingLocked=" + win + ", mKeyguardShowing=" + this.mKeyguardShowing, new Object[0]);
            this.mHandler.removeMessages(16);
            AnalysisManager.trace("finishDrawing");
            int i = this.mGoogleSpeedAnalysis;
            if (i == 3) {
                if (this.mDisplayBrightness > SNAPSHOTWINDOW_ZERO_ALPHA) {
                    googleSpeedExit("offSpeedExit");
                } else {
                    this.mGoogleSpeedAnalysis = 4;
                }
            } else if (i == 1) {
                if (!this.mKeyguardShowing) {
                    googleSpeedExit("onSpeedExit");
                } else {
                    this.mGoogleSpeedAnalysis = 2;
                }
            }
        }
    }

    private boolean activityHasWindow(WindowState win) {
        ActivityRecord activityRecord;
        if (win != null && (activityRecord = this.mTopActivity) != null && activityRecord.mChildren != null) {
            try {
                ArrayList<WindowState> children = new ArrayList<>((Collection<? extends WindowState>) this.mTopActivity.mChildren);
                for (int i = 0; i < children.size(); i++) {
                    WindowState w = children.get(i);
                    if (win == w) {
                        return true;
                    }
                }
            } catch (Exception e) {
                warning("activityHasWindow. Exception: ", e);
            }
        }
        return false;
    }

    public void addSnapshotWinLocked(WindowState win) {
        if (!checkPermission()) {
            return;
        }
        if (this.mHandler.getLooper().isCurrentThread()) {
            addSnapshotWinInternal(win);
        } else {
            this.mHandler.obtainMessage(13, win).sendToTarget();
        }
    }

    public void removeSnapshotWinLocked(WindowState win) {
        if (!checkPermission()) {
            return;
        }
        if (this.mHandler.getLooper().isCurrentThread()) {
            removeSnapshotWinInternal(win);
        } else {
            this.mHandler.obtainMessage(14, win).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reparentSurfaceControlToFocusTask() {
        WindowState windowState;
        WindowState win = this.mWinSnapshot;
        if (win == null || (windowState = this.mSnapshotUsedWindow) == null || windowState.getTask() == null || this.mSnapshotUsedWindow.getTask().getSurfaceControl() == null) {
            return;
        }
        synchronized (this.mWMS.mGlobalLock) {
            try {
                debug("reparent: " + this.mSnapshotUsedWindow.getRootTask().getSurfaceControl(), new Object[0]);
                this.mSnapshotUsedWindow.mToken.getPendingTransaction().reparent(this.mSC, this.mSnapshotUsedWindow.getRootTask().getSurfaceControl());
                win.mToken.getPendingTransaction().reparent(win.getSurfaceControl(), this.mSC);
                this.mSC.setLayer(2147483645);
                this.mSC.show();
            } catch (Exception e) {
                VSlog.w("SnapshotWindow", "reparent err :  ", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addSnapshotWinInternal(WindowState win) {
        WindowState windowState;
        if (!surfaceControlNotNull(win)) {
            return;
        }
        if (win.mAttrs != null && win.mAttrs.getTitle() != null) {
            String title = win.mAttrs.getTitle().toString();
            if ("SnapshotWindow".equals(title) && this.mWinSnapshot == null) {
                this.mWinSnapshot = win;
                this.mHandler.sendEmptyMessage(20);
            } else if (AOD_WINDOW_TITLE.equals(title) && this.mWinAod == null) {
                this.mWinAod = win;
            } else if (IS_CAPACITIVE && BLACK_WINDOW_TITLE.equals(title) && this.mWinBlack == null) {
                this.mWinBlack = win;
            } else if (KEYGUARD_WINDOW_TITLE.equals(title) && this.mWinNotificationShade == null) {
                this.mWinNotificationShade = win;
            } else if (win.mAttrs != null && win.mAttrs.type == 2013 && this.mWinWallpaper == null) {
                this.mWinWallpaper = win;
            } else if (FP_UI_ICON_WINDOW_TITLE.equals(title) && this.mWinFingerprintUiIcon == null) {
                this.mWinFingerprintUiIcon = win;
            }
        }
        if (this.mSnapshotView != null && (windowState = this.mSnapshotUsedWindow) != null && windowState == win && this.mSnapshotVisible && this.mTopActivityDrawn && isDisplayLandscape() && this.mUnlockAndDrawnTime > 0) {
            long diffTime = SystemClock.elapsedRealtime() - this.mUnlockAndDrawnTime;
            if (diffTime > 0 && diffTime < getUnlockRemoveTime()) {
                long totalDelay = (getUnlockRemoveTime() + DELAY_UNLOCKED_REMOVE_PLUS) - diffTime;
                debug("addSnapshotWinLocked:diffTime=" + diffTime + ",delay=" + totalDelay, new Object[0]);
                timeoutRemoveSnapshot(totalDelay, true);
                this.mUnlockAndDrawnTime = 0L;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeSnapshotWinInternal(WindowState win) {
        if (win != null && win.mAttrs != null && win.mAttrs.getTitle() != null) {
            String title = win.mAttrs.getTitle().toString();
            if ("SnapshotWindow".equals(title)) {
                this.mWinSnapshot = null;
            } else if (AOD_WINDOW_TITLE.equals(title)) {
                this.mWinAod = null;
            } else if (IS_CAPACITIVE && BLACK_WINDOW_TITLE.equals(title)) {
                this.mWinBlack = null;
            } else if (KEYGUARD_WINDOW_TITLE.equals(title)) {
                debug("remove mWinNotificationShade:" + win, new Object[0]);
                this.mWinNotificationShade = null;
            } else if (win.mAttrs != null && win.mAttrs.type == 2013) {
                this.mWinWallpaper = null;
            } else if (FP_UI_ICON_WINDOW_TITLE.equals(title)) {
                debug("remove UDfinger:" + win, new Object[0]);
                this.mWinFingerprintUiIcon = null;
            } else if ((win.mAttrs.flags & 2) != 0 && win.mAttrs.dimAmount > 0.01f) {
                removeDimLayerWin(win);
            }
        }
    }

    public void vivoOnWindowFocusChanged(int displayId, WindowState oldFocus, WindowState newFocus) {
        if (!checkPermission() || displayId != 0) {
            return;
        }
        FocusChangedWindowState focusChangedWins = new FocusChangedWindowState(oldFocus, newFocus);
        this.mHandler.obtainMessage(15, focusChangedWins).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFocusWindowInternal(WindowState oldFocus, WindowState newFocus) {
        ActivityRecord focusActivityRecord;
        ActivityRecord activityRecord;
        if (this.mWinSnapshot != null && newFocus != null && (focusActivityRecord = newFocus.mActivityRecord) != null && (activityRecord = this.mTopActivity) != null && activityRecord == focusActivityRecord) {
            this.mAfterUnlockFocusWindowGainedFocus = true;
            checkUnlockEndAndRemoveSnapshot(false);
        }
        checkTopActivityChanged();
    }

    private void checkTopActivityChanged() {
        if (this.mSnapshotView != null && this.mSnapshotVisible) {
            if (!this.mKeyguardShowing) {
                ActivityRecord activityRecord = getTopRunningActivity();
                debug("new focus=" + activityRecord, new Object[0]);
                ActivityRecord activityRecord2 = this.mTopActivity;
                if (activityRecord2 != null && activityRecord != null && activityRecord2 != activityRecord) {
                    timeoutRemoveSnapshot(0L, true);
                }
            }
        } else if (this.mSnapshotBmp != null) {
            checkSnapshotWinChanged(false);
        }
    }

    public void onCheckTopAppChanged() {
        if (!checkPermission() || this.mSnapshotBmp == null) {
            return;
        }
        this.mHandler.removeMessages(18);
        this.mHandler.sendEmptyMessage(18);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAppChangedInternal() {
        checkTopActivityChanged();
    }

    private ActivityRecord activityOfWindow(WindowState w) {
        WindowManagerService windowManagerService = this.mWMS;
        if (windowManagerService == null || windowManagerService.mRoot == null || w == null) {
            return null;
        }
        try {
            ActivityRecord r = w.mActivityRecord;
            return r;
        } catch (Exception ex) {
            warning("get OpaqueWin Activity. Exception: ", ex);
            return null;
        }
    }

    private ActivityRecord getTopRunningActivity() {
        WindowManagerService windowManagerService = this.mWMS;
        if (windowManagerService == null || windowManagerService.mRoot == null) {
            return null;
        }
        try {
            ActivityRecord r = this.mWMS.mRoot.topRunningActivity();
            return r;
        } catch (Exception ex) {
            warning("get TopRunningActivity. Exception: ", ex);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ActivityRecord checkSnapshotWinChanged(boolean isRecheck) {
        WindowManagerService windowManagerService = this.mWMS;
        if (windowManagerService == null || windowManagerService.mRoot == null || this.mSnapshotUsedWindow == null) {
            return null;
        }
        this.mCheckNum = isRecheck ? this.mCheckNum + 1 : 0;
        boolean snapshotUsedWindowVisible = false;
        try {
            snapshotUsedWindowVisible = this.mSnapshotUsedWindow.getTask().shouldBeVisible((ActivityRecord) null);
        } catch (Exception e) {
            warning("window task beVisible. Exception: ", new Object[0]);
        }
        ActivityRecord r = null;
        try {
            r = this.mWMS.mRoot.topRunningActivity();
        } catch (Exception ex) {
            warning("get TopRunningActivity. Exception: " + ex, new Object[0]);
        }
        if ((r == null || (snapshotUsedWindowVisible && (r.isEmpty() || r.getChildCount() == 0))) && this.mCheckNum < CHECK_TOPAPP_RETRY_NUM) {
            warning("empty retry " + this.mCheckNum + " " + r, new Object[0]);
            if (this.mHandler.hasMessages(21)) {
                int i = this.mCheckNum;
                this.mCheckNum = i > 0 ? i - 1 : 0;
                this.mHandler.removeMessages(21);
            }
            this.mHandler.sendEmptyMessageDelayed(21, DELAY_CHECK_TOPAPP);
            return null;
        }
        boolean needGoogleUnlock = false;
        if (snapshotUsedWindowVisible) {
            if (r != null) {
                WindowState w = null;
                try {
                    w = r.findMainWindow();
                } catch (Exception e2) {
                    warning("Exception: " + e2, new Object[0]);
                }
                if ((w != null && winCanShowWhenLocked(w)) || (w == null && rCanShowWhenLocked(r))) {
                    debug("new win show on keyguard, " + r, new Object[0]);
                    needGoogleUnlock = true;
                }
            }
        } else if (this.mLauncherAnimEnable && isLauncherPkg(toActivityString(r))) {
            back2LauncherSetSnapshotUnlock(r);
        } else {
            needGoogleUnlock = true;
        }
        if (needGoogleUnlock) {
            setUseGoolgeUnlock(r, "app_change", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + snapshotUsedWindowVisible);
        }
        return r;
    }

    private void back2LauncherSetSnapshotUnlock(ActivityRecord topRunningActivity) {
        debug("back 2 launcher from " + this.mTopActivity, new Object[0]);
        this.mScreenOffActivity = this.mTopActivity;
        this.mTopActivity = topRunningActivity;
        try {
            WindowState findMainWindow = topRunningActivity.findMainWindow();
            this.mSnapshotUsedWindow = findMainWindow;
            this.mTopFullscreenOpaqueWin = findMainWindow;
        } catch (Exception e) {
            warning("findMainWindow. Exception: " + e, new Object[0]);
        }
        updateTopActivity(topRunningActivity, this.mSnapshotUsedWindow);
        setWallpaper();
        this.mHasSurfaceView = false;
    }

    private void setUseGoolgeUnlock(ActivityRecord topRunningActivity, String reason, String dbgStr) {
        this.mTopFullscreenOpaqueWin = null;
        this.mSnapshotUsedWindow = null;
        this.mTopActivity = null;
        this.mSnapshotBmp = null;
        this.mHasSurfaceView = false;
        this.mGoogleReason = "app_change";
        this.mScreenOffActivity = topRunningActivity;
        info("app change set google unlock." + dbgStr + " : " + topRunningActivity, new Object[0]);
    }

    private DisplayContent defaultDisplayContentLw() {
        DisplayContent displayContent = null;
        try {
            synchronized (this.mWMS.mGlobalLock) {
                displayContent = this.mWMS.getDefaultDisplayContentLocked();
            }
        } catch (Exception ex) {
            warning("getDefaultDisplayContentLocked Exception : " + ex, new Object[0]);
        }
        return displayContent;
    }

    private ActivityRecord getSecondActivity() {
        return null;
    }

    private boolean isModuleCreate(int module) {
        return (this.mSnapshotCreate & module) != 0;
    }

    private boolean useGoogle() {
        if (this.mSnapshotBmp == null) {
            return true;
        }
        if (this.mStartAppEnable && this.mDeviceInteractive) {
            this.mGoogleReason = "keyguard_app";
            return true;
        }
        return false;
    }

    public boolean isGoogleUnlock() {
        return checkPermission() && this.mSnapshotBmp == null && this.mFpAuthSuccess;
    }

    public void focusAppHasDrawn(WindowState win) {
        if (this.mSnapshotAlpha && this.mWinSnapshot != null && win != null && isLauncherHome(win) && this.mSnapshotUsedWindow == win) {
            try {
                if (surfaceControlNotNull(this.mWinSnapshot)) {
                    debug("launcher show do hide " + win, new Object[0]);
                    SurfaceControl.Transaction tmpTransaction = (SurfaceControl.Transaction) this.mWMS.mTransactionFactory.get();
                    tmpTransaction.hide(this.mSC);
                    SurfaceControl.mergeToGlobalTransaction(tmpTransaction);
                }
            } catch (Exception e) {
                warning("hide err:", e);
            }
        }
    }

    public void recentsAnimHideFocusActivity(ActivityStack restoreTargetBehindStack) {
        if (!checkPermission() || this.mSnapshotUsedWindow == null || !surfaceControlNotNull(this.mWinSnapshot)) {
            return;
        }
        ActivityRecord r = restoreTargetBehindStack.topRunningActivity();
        if (r == this.mSnapshotUsedWindow.mActivityRecord) {
            debug("focusActivityRecord hide", new Object[0]);
            this.mSnapshotUsedWindow.mToken.getPendingTransaction().hide(this.mSC);
        }
    }

    public void onDisplayStateChangeFinished(float brightness) {
        if (this.mDisplayBrightness <= SNAPSHOTWINDOW_ZERO_ALPHA && brightness > SNAPSHOTWINDOW_ZERO_ALPHA) {
            this.mHandler.sendEmptyMessage(23);
        }
        this.mDisplayBrightness = brightness;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDisplayStateChangeFinishedInternal() {
        AnalysisManager.trace("fraLight");
        if (this.mGoogleSpeedAnalysis == 4) {
            googleSpeedExit("offSpeedExit");
        }
    }

    private void googleSpeedExit(String speedExit) {
        AnalysisManager.trace(speedExit);
        this.mGoogleSpeedAnalysis = 0;
        this.mRecordFinishDrawingWin = null;
    }

    private boolean needHideKeyguard() {
        WindowState windowState;
        if (!this.mDeviceInteractive && (windowState = this.mWinNotificationShade) != null && windowState.mWinAnimator != null && this.mWinNotificationShade.mWinAnimator.mSurfaceController != null) {
            return !this.mWinNotificationShade.mWinAnimator.mSurfaceController.getShown();
        }
        WindowState windowState2 = this.mWinNotificationShade;
        if (windowState2 == null || windowState2.mWinAnimator == null || this.mWinNotificationShade.mWinAnimator.mSurfaceController != null) {
            return false;
        }
        info("mSurfaceController is null ", new Object[0]);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideKeyguard(boolean hide) {
        WindowState windowState = this.mWinNotificationShade;
        if (windowState != null) {
            windowState.hideByBiometric(hide);
        }
    }

    private void timeoutCreateWindow(int module) {
        if (module == 1) {
            timeoutRemoveSnapshot(5000L);
        } else if (module == 2) {
            if (isModuleCreate(1)) {
                timeoutRemoveSnapshot(5000L);
            } else {
                timeoutRemoveSnapshot(DELAY_CREATED_REMOVE_SHORT);
            }
        }
    }

    private long getUnlockRemoveTime() {
        int i = this.mWallpaperState;
        if (i == 1 || i == 2) {
            return DELAY_UNLOCKED_REMOVE_LAUNCHER;
        }
        boolean sizeMismatch = false;
        WindowState windowState = this.mWinSnapshot;
        if (windowState != null && windowState.mToken != null && this.mSnapshotBmp != null) {
            Rect frame = this.mWinSnapshot.mToken.getBounds();
            sizeMismatch = (frame.width() == this.mSnapshotBmp.getWidth() && frame.height() == this.mSnapshotBmp.getHeight()) ? false : true;
        }
        if (sizeMismatch) {
            long remainingtime = SIZE_MISMATCH_MINIMUM_TIME_MS - (SystemClock.elapsedRealtime() - this.mShownTime);
            return Math.max(DELAY_UNLOCKED_REMOVE, remainingtime);
        }
        String focusActivity = toActivityString(this.mTopActivity);
        if (!focusActivity.isEmpty()) {
            for (NameList name : this.mDelayRemoveWindows) {
                if (focusActivity.startsWith(name.pkgName)) {
                    return name.delay;
                }
            }
        }
        long ms = DELAY_UNLOCKED_REMOVE;
        if (!this.mSnapshotPortrait) {
            ms = DELAY_UNLOCKED_REMOVE_LANDSCAPE;
        } else if (this.mUnlockModule == 1 && focusAppIsInActivityList(DELAY_REMOVE_SNAPSHOT_ACTIVITY_LIST_FOR_FACE)) {
            ms += DELAY_UNLOCKED_REMOVE_PLUS;
        }
        if (this.mHasSurfaceView) {
            return ms + DELAY_SF_REMOVE;
        }
        return ms;
    }

    private void timeoutRemoveSnapshot(long timeout) {
        timeoutRemoveSnapshot(timeout, false);
    }

    private void timeoutRemoveSnapshot(long timeout, boolean isUnlock) {
        this.mStartRemoveWinTime = SystemClock.elapsedRealtime();
        this.mHandler.removeMessages(50);
        if (timeout == 0) {
            removeWindow(isUnlock);
            return;
        }
        Message msg = Message.obtain(this.mHandler, 50, isUnlock ? 1 : 0, 0);
        this.mHandler.sendMessageDelayed(msg, timeout);
    }

    private boolean surfaceControlNotNull(WindowState win) {
        return (win == null || win.mWinAnimator == null || win.mWinAnimator.mSurfaceController == null || win.mWinAnimator.mSurfaceController.mSurfaceControl == null) ? false : true;
    }

    private void surfaceShow(WindowState win) {
        surfaceShow(win, false);
    }

    private void surfaceShow(WindowState win, boolean forceApply) {
        try {
            if (surfaceControlNotNull(win)) {
                debug("show " + win + ", " + forceApply, new Object[0]);
                win.mWinAnimator.mSurfaceController.mSurfaceControl.show();
                if (forceApply) {
                    win.mWinAnimator.mSurfaceController.mSurfaceControl.apply(true);
                }
            }
        } catch (Exception e) {
            warning("show ", e);
        }
    }

    private void surfaceHide(WindowState win, boolean forceApply) {
        try {
            if (surfaceControlNotNull(win)) {
                debug("hide " + win + ", " + forceApply + ", shown:" + win.mWinAnimator.mSurfaceController.getShown(), new Object[0]);
                win.mWinAnimator.mSurfaceController.mSurfaceControl.hide();
                if (forceApply) {
                    win.mWinAnimator.mSurfaceController.mSurfaceControl.apply(true);
                }
            }
        } catch (Exception e) {
            warning("hide ", e);
        }
    }

    private void surfaceAlpha(WindowState win, float alpha) {
        surfaceAlphaForceApply(win, alpha, false);
    }

    private void surfaceAlphaForceApply(WindowState win, float alpha, boolean forceApply) {
        try {
            if (surfaceControlNotNull(win)) {
                debug("setAlpha forceApply=" + forceApply + " " + win + ", " + alpha, new Object[0]);
                win.mWinAnimator.mSurfaceController.mSurfaceControl.setAlpha(alpha);
                if (forceApply) {
                    win.mWinAnimator.mSurfaceController.mSurfaceControl.apply(true);
                }
            }
        } catch (Exception e) {
            warning("alpha ", e);
        }
    }

    private void restoreShowKeyguardDelay(int ms) {
        debug("restore NotificationShade delay : " + ms, new Object[0]);
        if (this.mHandler.hasMessages(17)) {
            this.mHandler.removeMessages(17);
        }
        this.mHandler.sendEmptyMessageDelayed(17, ms);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restoreShowKeyguard() {
        boolean surfaceShown = false;
        hideKeyguard(false);
        synchronized (this.mWMS.mGlobalLock) {
            if (this.mWinNotificationShade != null && this.mWinNotificationShade.mWinAnimator != null) {
                try {
                    surfaceShown = this.mWinNotificationShade.mWinAnimator.mSurfaceController.getShown();
                } catch (Exception e) {
                }
                debug("restore NotificationShade show : " + surfaceShown, new Object[0]);
                if (surfaceShown) {
                    surfaceShow(this.mWinNotificationShade, false);
                }
            }
        }
        if (surfaceShown && this.mWMS != null) {
            debug("restore NotificationShade requestTraversal", new Object[0]);
            needTraversal();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restoreFingerprintUiIcon() {
        if (IS_CAPACITIVE) {
            return;
        }
        boolean surfaceShown = false;
        synchronized (this.mWMS.mGlobalLock) {
            if (this.mWinFingerprintUiIcon != null && this.mWinFingerprintUiIcon.mWinAnimator != null && this.mWinFingerprintUiIcon.mWinAnimator.mSurfaceController != null) {
                try {
                    surfaceShown = this.mWinFingerprintUiIcon.mWinAnimator.mSurfaceController.getShown();
                } catch (Exception e) {
                }
                debug("restore fingerprintUiIcon show : " + surfaceShown, new Object[0]);
                if (surfaceShown) {
                    surfaceShow(this.mWinFingerprintUiIcon, false);
                }
            }
        }
    }

    private void notifyWallpaper(String phase) {
        if (this.mWallpaperState != 2) {
            return;
        }
        try {
            if (this.mWinWallpaper != null) {
                debug("notifyWallpaper " + phase, new Object[0]);
                String commandTag = COMMAND_OFF_FINGERPRINT_UNLOCK;
                if (this.mDeviceInteractive) {
                    commandTag = COMMAND_ON_FINGERPRINT_UNLOCK;
                }
                this.mWinWallpaper.notifyWallpaperClientUnlockPhase(commandTag, phase);
            }
        } catch (Exception e) {
            warning("notifyWallpaper ", e);
        }
    }

    private WallpaperManager getWallpaperManager() {
        if (this.mWallpaperManager == null) {
            this.mWallpaperManager = (WallpaperManager) this.mContext.getSystemService("wallpaper");
        }
        return this.mWallpaperManager;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerWallpaperVisibilityListener() {
        if (this.mWMS == null || this.mWallpaper != null) {
            return;
        }
        WallpaperVisibility wallpaperVisibility = new WallpaperVisibility();
        this.mWallpaper = wallpaperVisibility;
        this.mWMS.registerWallpaperVisibilityListener(wallpaperVisibility, 0);
        info("registerWallpaperVisibilityListener.", new Object[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class WallpaperVisibility extends IWallpaperVisibilityListener.Stub {
        private WallpaperVisibility() {
        }

        public void onWallpaperVisibilityChanged(boolean visible, int displayId) {
            SnapshotWindow.debug("onWallpaperVisibilityChanged=" + visible, new Object[0]);
            SnapshotWindow.this.mWallpaperVisible = visible;
        }
    }

    private boolean checkPermission() {
        return this.mFingerprintEnable || this.mFaceEnable;
    }

    private Bitmap snapshotToBitmap(ActivityManager.TaskSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            Bitmap hardwareBitmap = Bitmap.wrapHardwareBuffer(snapshot.getSnapshot(), snapshot.getColorSpace());
            if (!this.mIsHardwareAccelerated) {
                return hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false);
            }
            return hardwareBitmap;
        } catch (IllegalArgumentException e) {
            warning("IllegalArgumentException", new Object[0]);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerObserve() {
        ContentResolver resolver = this.mContext.getContentResolver();
        ContentObserver ContentObserve = new MyContentObserve(new Handler());
        Uri launcherUri = settingsGetUriFor(UNLOCK_ENTER_LAUNCHER_ANIMATION, 0);
        Uri fingerprintUri = settingsGetUriFor("finger_unlock_open", 0);
        Uri faceUri = settingsGetUriFor("face_unlock_keyguard_enabled", 1);
        Uri startAppUri = settingsGetUriFor(SETTING_START_APP_FROM_KEYGUARD, 0);
        if (launcherUri != null) {
            resolver.registerContentObserver(launcherUri, true, ContentObserve, -1);
            this.mLauncherAnimEnable = settingsGetInt(UNLOCK_ENTER_LAUNCHER_ANIMATION, 0) != 0;
        }
        if (fingerprintUri != null) {
            resolver.registerContentObserver(fingerprintUri, true, ContentObserve, -1);
            this.mFingerprintEnable = settingsGetInt("finger_unlock_open", 0) != 0;
        }
        if (faceUri != null) {
            resolver.registerContentObserver(faceUri, true, ContentObserve, -1);
            this.mFaceEnable = settingsGetInt("face_unlock_keyguard_enabled", 1) != 0;
        }
        if (startAppUri != null) {
            resolver.registerContentObserver(startAppUri, true, ContentObserve, -1);
            this.mStartAppEnable = settingsGetInt(SETTING_START_APP_FROM_KEYGUARD, 0) != 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initSurfaceControl() {
        this.mSC = new SurfaceControl.Builder().setContainerLayer().setName("SnapshotWindowParent").build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Uri settingsGetUriFor(String name, int type) {
        try {
            if (type != 0) {
                if (type != 1) {
                    return null;
                }
                return Settings.Secure.getUriFor(name);
            }
            return Settings.System.getUriFor(name);
        } catch (Exception e) {
            warning("get settings Uri failed, name: " + name, new Object[0]);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int settingsGetInt(String key, int type) {
        if (type != 0) {
            if (type != 1) {
                return 0;
            }
            return Settings.Secure.getInt(this.mContext.getContentResolver(), key, 0);
        }
        return Settings.System.getInt(this.mContext.getContentResolver(), key, 0);
    }

    private FaceInternal getFaceInternal() {
        if (this.mFaceInternal == null) {
            this.mFaceInternal = (FaceInternal) LocalServices.getService(FaceInternal.class);
        }
        return this.mFaceInternal;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyContentObserve extends ContentObserver {
        MyContentObserve(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (!uri.equals(SnapshotWindow.this.settingsGetUriFor(SnapshotWindow.UNLOCK_ENTER_LAUNCHER_ANIMATION, 0))) {
                if (!uri.equals(SnapshotWindow.this.settingsGetUriFor("finger_unlock_open", 0))) {
                    if (!uri.equals(SnapshotWindow.this.settingsGetUriFor("face_unlock_keyguard_enabled", 1))) {
                        if (uri.equals(SnapshotWindow.this.settingsGetUriFor(SnapshotWindow.SETTING_START_APP_FROM_KEYGUARD, 0))) {
                            SnapshotWindow snapshotWindow = SnapshotWindow.this;
                            snapshotWindow.mStartAppEnable = snapshotWindow.settingsGetInt(SnapshotWindow.SETTING_START_APP_FROM_KEYGUARD, 0) != 0;
                            return;
                        }
                        return;
                    }
                    SnapshotWindow snapshotWindow2 = SnapshotWindow.this;
                    snapshotWindow2.mFaceEnable = snapshotWindow2.settingsGetInt("face_unlock_keyguard_enabled", 1) != 0;
                    return;
                }
                SnapshotWindow snapshotWindow3 = SnapshotWindow.this;
                snapshotWindow3.mFingerprintEnable = snapshotWindow3.settingsGetInt("finger_unlock_open", 0) != 0;
                return;
            }
            SnapshotWindow snapshotWindow4 = SnapshotWindow.this;
            snapshotWindow4.mLauncherAnimEnable = snapshotWindow4.settingsGetInt(SnapshotWindow.UNLOCK_ENTER_LAUNCHER_ANIMATION, 0) != 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class WaitingWindowState {
        boolean drawn;
        WindowState win;

        WaitingWindowState(WindowState win, boolean drawn) {
            this.win = win;
            this.drawn = drawn;
        }

        public String toString() {
            return "window:" + this.win + ", drawn:" + this.drawn;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class FocusChangedWindowState {
        WindowState newFocus;
        WindowState oldFocus;

        FocusChangedWindowState(WindowState oldFocus, WindowState newFocus) {
            this.oldFocus = oldFocus;
            this.newFocus = newFocus;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class NameList {
        long delay;
        String pkgName;

        NameList(String pkgName, long delay) {
            this.pkgName = pkgName;
            this.delay = delay;
        }
    }

    /* loaded from: classes.dex */
    final class AsyncHandler extends Handler {
        public static final int MSG_APP_CHANGED = 18;
        public static final int MSG_CLEAR_UNLOCK_FINISH_DRAWING = 16;
        public static final int MSG_CREATE_WINDOW = 101;
        public static final int MSG_DISPLAY_STATE_FINISHED = 23;
        public static final int MSG_FINISHED_WAKING_UP = 3;
        public static final int MSG_FINISH_DRAWING = 12;
        public static final int MSG_RECHECK_ACTIVITY_WIN = 21;
        public static final int MSG_REMOVE_WINDOW = 50;
        public static final int MSG_REPARENT = 20;
        public static final int MSG_RESTORE_FINGERPRINTUI_ICON_WIN = 19;
        public static final int MSG_RESTORE_SHOW_KEYGUARD_WIN = 17;
        public static final int MSG_SHOW_WINDOW = 102;
        public static final int MSG_START_GOINGTO_SLEEP = 4;
        public static final int MSG_STOP_SHOW_DIM = 22;
        public static final int MSG_SYSTEM_READY = 2;
        public static final int MSG_UPDATE_FOCUS_WIN = 15;
        public static final int MSG_WINDOW_ROTATION = 11;
        public static final int MSG_WIN_RELAYOUT_SURFACECONTROL_CREATE = 13;
        public static final int MSG_WIN_REMOVE = 14;

        public AsyncHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 2) {
                SnapshotWindow.this.registerWallpaperVisibilityListener();
                SnapshotWindow.this.registerObserve();
                SnapshotWindow.this.initSurfaceControl();
                return;
            }
            if (i == 3) {
                SnapshotWindow.this.mDeviceInteractive = true;
                if (SnapshotWindow.this.mSnapshotBmp == null) {
                    SnapshotWindow.this.hideKeyguard(false);
                }
            } else if (i == 4) {
                SnapshotWindow.this.mSnapshotUsedWindow = (WindowState) msg.obj;
                SnapshotWindow.this.onStartedGoingToSleepInternal();
            } else if (i == 50) {
                boolean isUnlock = msg.arg1 == 1;
                SnapshotWindow.this.removeWindow(isUnlock);
            } else if (i == 101) {
                int module = msg.arg1;
                SnapshotWindow.this.createSnapshotWindowInternal(module);
            } else if (i != 102) {
                switch (i) {
                    case 11:
                        int rotation = msg.arg1;
                        SnapshotWindow.this.notifyRotationChangedInternal(rotation);
                        return;
                    case 12:
                        WindowState win = (WindowState) msg.obj;
                        SnapshotWindow.this.finishDrawingInternal(win);
                        return;
                    case 13:
                        WindowState win2 = (WindowState) msg.obj;
                        SnapshotWindow.this.addSnapshotWinInternal(win2);
                        return;
                    case 14:
                        WindowState win3 = (WindowState) msg.obj;
                        SnapshotWindow.this.removeSnapshotWinInternal(win3);
                        return;
                    case 15:
                        FocusChangedWindowState focusChangedWins = (FocusChangedWindowState) msg.obj;
                        SnapshotWindow.this.updateFocusWindowInternal(focusChangedWins.oldFocus, focusChangedWins.newFocus);
                        return;
                    case 16:
                        SnapshotWindow.this.mRecordFinishDrawingWin = null;
                        return;
                    case 17:
                        SnapshotWindow.this.restoreShowKeyguard();
                        return;
                    case 18:
                        SnapshotWindow.this.onAppChangedInternal();
                        return;
                    case 19:
                        SnapshotWindow.this.restoreFingerprintUiIcon();
                        return;
                    case 20:
                        SnapshotWindow.this.reparentSurfaceControlToFocusTask();
                        return;
                    case 21:
                        SnapshotWindow.this.checkSnapshotWinChanged(true);
                        return;
                    case 22:
                        SnapshotWindow.this.showDimTimeOut();
                        return;
                    case 23:
                        SnapshotWindow.this.onDisplayStateChangeFinishedInternal();
                        return;
                    default:
                        return;
                }
            } else {
                int module2 = msg.arg1;
                boolean show = msg.arg2 == 1;
                SnapshotWindow.this.setSnapshotVisibilityInternal(module2, show);
            }
        }
    }

    private String moduleToStr(int module) {
        if (module != 0) {
            if (module != 1) {
                if (module == 2) {
                    return "fingerprint";
                }
                String str = Integer.toString(module);
                return str;
            }
            return "face";
        }
        return "default";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void debug(String format, Object... args) {
        VSlog.d("SnapshotWindow", String.format(format, args));
    }

    private static void info(String format, Object... args) {
        VSlog.i("SnapshotWindow", String.format(format, args));
    }

    private static void warning(String format, Object... args) {
        VSlog.w("SnapshotWindow", String.format(format, args));
    }
}