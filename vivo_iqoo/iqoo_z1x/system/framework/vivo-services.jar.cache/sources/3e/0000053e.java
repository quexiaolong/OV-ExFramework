package com.android.server.wm;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.multidisplay.MultiDisplayManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.FtBuild;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.Display;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.internal.policy.DrawableUtils;
import com.android.internal.policy.NavigationBarPolicy;
import com.android.internal.statusbar.IStatusBarService;
import com.android.server.policy.IVivoRatioControllerUtils;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.android.server.policy.WindowManagerPolicy;
import com.vivo.appshare.AppShareConfig;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.services.rms.sdk.Consts;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoDisplayPolicyImpl implements IVivoDisplayPolicy {
    private static final String CONFIG_URI_SOGOU_SWITCH = "content://com.sohu.sogou.inputmethod.gameprovider/switch";
    private static final int DETECTED_DIALOG_TYPE_NONE = 0;
    private static final int DETECTED_DIALOG_TYPE_NORMAL = 1;
    private static final int DETECTED_DIALOG_TYPE_UPSLIDE_OR_FULLSCREEN = 2;
    private static final String DIMLAYER_HOST_NAME = "DisplayArea.Root";
    private static final int MSG_NOTIFY_FULLSCREEN_CHANGE = 101;
    static final int NOTIFY_SYSTEMUI_REASON_UPDATA_COLOR = 1;
    static final String TAG = "VivoDisplayPolicyImpl";
    final boolean DEBUG_MULTIWIN;
    private WindowState mBottomGestureBar;
    private Context mContext;
    private WindowState mDimmingWindowState;
    private boolean mForcingShowNavBarForSecureMethod;
    private final Handler mHandler;
    private boolean mIsPerfBoostFlingAcquired;
    private int mLastAppShareUiMode;
    private int mLastFullscreenStackSysUiFlagsForFreeForm;
    private WindowState mLetterBoxingWindow;
    private WindowManagerPolicy mPhoneWindowManager;
    private DisplayPolicy mPolicy;
    private View mRatioSwitchView;
    WindowState mRealTopFullscreenOpaqueWindowStateForMulti;
    private WindowManagerService mService;
    SnapshotWindow mSnapshotWindow;
    private int mSogouImeGameSwitchEnable;
    private SogouImeGameSwitchObserver mSogouImeGameSwitchObserver;
    WindowState mTopFullscreenOpaqueWindowStateForMulti;
    boolean mVivoHasNavBar;
    private VivoRatioControllerUtilsImpl mVivoRatioControllerUtils;
    private VivoSwipeObserver mVivoSwipeObserver;
    private WindowState mWinResponsibleForGesture;
    static final Rect mTmpRatioSwitchFrame = new Rect();
    private static boolean sDEBUG = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
    private static ArrayList<String> mScrollBoostBlackList = new ArrayList<String>() { // from class: com.android.server.wm.VivoDisplayPolicyImpl.1
        {
            add("com.imangi.templerun2");
        }
    };
    private static boolean SCROLL_BOOST_SS_ENABLE = false;
    private static ArrayList<WindowState> mEdgeShortcutList = new ArrayList<>();
    private static final boolean DEBUG_SHOW_SHORTCUT = SystemProperties.getBoolean("persist.vivo.letterbox.show", false);
    int mLastIconColor = -2;
    int mLastKeyguardisShowing = -2;
    int mLastStatusBarColor = -2;
    int mLastNavBarColor = -2;
    int mLastRatioViewState = 3;
    int mHiddenKGDelayTimes = -2;
    boolean mForceShowNavBar = false;
    boolean mInputmethodTop = false;
    boolean mSystemDialogTop = false;
    boolean mLastInputmethodTop = false;
    int mLastHomeIndicatorState = -1;
    String mLastTopAppPkgName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    int mDialogTopType = 0;
    WindowManager.LayoutParams mDialogTopAttrs = null;
    WindowState mDialogTopWindow = null;
    boolean mSwipeGestureForStatusBar = false;
    boolean mPendingSwipeHideForStatusBar = false;
    boolean mSwipeGestureForNavBar = false;
    boolean mPendingSwipeHideForNavBar = false;
    WindowState mTopFullWin = null;
    WindowState mLastTopFullWin = null;
    WindowState mBottomFullWin = null;
    WindowState mLastBottomFullWin = null;
    boolean mSwipeFromTopGesture = false;
    boolean mPendingSwipeHide = false;
    WindowState mRatioSwitch = null;
    WindowState mHomeIndicator = null;
    private boolean ARDBG = SystemProperties.getBoolean("persist.vivopolicy.debug", false);
    private DisplayFrames mDisplayFrames = null;
    private WindowState mLastWinForSysUiVis = null;
    private boolean mVivoChangeNavPosition = true;
    boolean mVivoForceHideNavBar = false;
    private final ArrayList<ForceHideNavBarToken> mForceHideNavBarTokens = new ArrayList<>();
    private final int[] mVivoStatusBarHeightForRotation = new int[4];
    AbsVivoPerfManager mPerfBoostDrag = null;
    AbsVivoPerfManager mPerfBoostFling = null;
    AbsVivoPerfManager mPerfBoostPrefling = null;
    AbsVivoPerfManager mPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
    boolean mLastInLight = false;
    private final ArrayList<WindowState> mOtherFullOpaqueWindows = new ArrayList<>();
    private boolean mLastKeyguardShowing = false;
    private WindowState mCurrentTopFullWin = null;
    private int mCurDisplayRotation = -1;
    private Rect mLastTopWinBounds = new Rect();
    private boolean mLastEnableLetterBox = false;
    private WindowState mEdgeShortcut = null;
    private boolean mEdgetShortcutShowing = false;
    private final Rect mRestrictedFrame = new Rect();
    private final Rect mTmpRect = new Rect();
    private boolean isTopCutout = false;
    private final ArrayList<Integer> navColors = new ArrayList<>();
    private boolean mNeedUpdateForFocusGained = true;
    private int mVivoNavSwipeCount = 0;
    private int mVivoStatusSwipeCount = 0;
    private boolean mVivoDisableSwape = false;
    private final Runnable mSwipeTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.VivoDisplayPolicyImpl.3
        @Override // java.lang.Runnable
        public void run() {
            VivoDisplayPolicyImpl.this.resetSwipeCount();
        }
    };

    /* loaded from: classes.dex */
    private class VivoPolicyHandler extends Handler {
        VivoPolicyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 101) {
                Settings.System.putIntForUser(VivoDisplayPolicyImpl.this.mContext.getContentResolver(), "vivo_fullscreen_flag", msg.arg1, -2);
            }
        }
    }

    public VivoDisplayPolicyImpl(DisplayPolicy policy, WindowManagerService service, WindowManagerPolicy manager, Context context, final Handler handler, IWindowManager wm, boolean isDefaultDisplay) {
        boolean z;
        if (!SystemProperties.getBoolean("persist.vivo.multiwindow_debug_displaypolicy", false) && !VivoMultiWindowConfig.isDebugAllPrivateInfo()) {
            z = false;
        } else {
            z = true;
        }
        this.DEBUG_MULTIWIN = z;
        this.mLastAppShareUiMode = 0;
        this.mPolicy = policy;
        this.mService = service;
        this.mPhoneWindowManager = manager;
        this.mContext = context;
        VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl = VivoRatioControllerUtilsImpl.getInstance();
        this.mVivoRatioControllerUtils = vivoRatioControllerUtilsImpl;
        if (isDefaultDisplay) {
            vivoRatioControllerUtilsImpl.init(this.mContext, handler, wm, isDefaultDisplay);
        }
        VivoPolicyHandler vivoPolicyHandler = new VivoPolicyHandler(handler.getLooper());
        this.mHandler = vivoPolicyHandler;
        if (isDefaultDisplay) {
            Message msg = vivoPolicyHandler.obtainMessage(101, 0, 0);
            msg.sendToTarget();
        }
        int displayId = this.mPolicy.getDisplayId();
        if (displayId == 0 || 4096 == displayId) {
            handler.post(new Runnable() { // from class: com.android.server.wm.-$$Lambda$VivoDisplayPolicyImpl$JQOKJExG-9h0umNXAc6YMi_U01Y
                @Override // java.lang.Runnable
                public final void run() {
                    VivoDisplayPolicyImpl.this.lambda$new$0$VivoDisplayPolicyImpl(handler);
                }
            });
        }
        if (this.mPerf != null) {
            SCROLL_BOOST_SS_ENABLE = true;
        }
    }

    public /* synthetic */ void lambda$new$0$VivoDisplayPolicyImpl(Handler handler) {
        SogouImeGameSwitchObserver sogouImeGameSwitchObserver = new SogouImeGameSwitchObserver(handler);
        this.mSogouImeGameSwitchObserver = sogouImeGameSwitchObserver;
        sogouImeGameSwitchObserver.observe();
        VivoSwipeObserver vivoSwipeObserver = new VivoSwipeObserver(handler);
        this.mVivoSwipeObserver = vivoSwipeObserver;
        vivoSwipeObserver.observe();
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public void systemReady() {
        Display display;
        View addRatioSwitchView = this.mVivoRatioControllerUtils.addRatioSwitchView();
        this.mRatioSwitchView = addRatioSwitchView;
        if (addRatioSwitchView != null && (display = this.mContext.getDisplay()) != null && (display.getDisplayId() == 0 || 4096 == display.getDisplayId())) {
            this.mVivoRatioControllerUtils.updateRatioView(3, 0, 0, 0);
        }
        this.mSnapshotWindow = SnapshotWindow.getInstance(this.mContext);
    }

    public void systemBooted() {
        VivoRatioControllerUtilsImpl vivoRatioControllerUtilsImpl = this.mVivoRatioControllerUtils;
        if (vivoRatioControllerUtilsImpl != null) {
            vivoRatioControllerUtilsImpl.systemBooted();
        }
    }

    public void removeWindowLw(WindowState win) {
        if (win.getAttrs().type == 2000 || win == this.mBottomGestureBar) {
            this.mPolicy.mDisplayContent.notifyWindowRemoved(win);
        }
        if (this.mRatioSwitch == win) {
            this.mRatioSwitch = null;
        } else if (this.mBottomGestureBar == win) {
            this.mBottomGestureBar = null;
        } else if (mEdgeShortcutList.contains(win)) {
            mEdgeShortcutList.remove(win);
        }
    }

    public void updateSystemBarsLw(int vis, int oldVis, WindowState win, boolean hideNavBarSysui, boolean immersiveSticky) {
        if (vis != oldVis || win != this.mLastWinForSysUiVis) {
            VLog.d(TAG, "updateSystemBarsLw old = 0x" + Integer.toHexString(oldVis) + " ,new = 0x" + Integer.toHexString(vis) + " ,win = " + win + " hideNavBarSysui = " + hideNavBarSysui + " immersiveSticky = " + immersiveSticky);
        }
        this.mLastWinForSysUiVis = win;
    }

    public void beginLayoutLw(DisplayFrames displayFrames, int uiMode, Rect dcf, boolean navVisible, boolean navTranslucent, boolean navAllowedHidden, boolean statusBarExpandedNotKeyguard) {
        this.mDisplayFrames = displayFrames;
        layoutRatioView(displayFrames, uiMode, dcf, navVisible, navTranslucent, navAllowedHidden, statusBarExpandedNotKeyguard);
    }

    public boolean layoutWindowLw(WindowState win, WindowState attached) {
        if (this.ARDBG) {
            VSlog.d(TAG, "DEBUG_ASPECT_RATIO:layoutWindowLw win=" + win + " attached=" + attached);
        }
        if (win == this.mRatioSwitch) {
            return true;
        }
        return mEdgeShortcutList.contains(win) ? false : false;
    }

    public void beginPostLayoutPolicyLw() {
        this.mInputmethodTop = false;
        this.mForceShowNavBar = false;
        this.mDialogTopType = 0;
        this.mDialogTopAttrs = null;
        this.mDialogTopWindow = null;
        WindowState windowState = this.mTopFullWin;
        if (windowState != null) {
            this.mLastTopFullWin = windowState;
            this.mTopFullWin = null;
        }
        WindowState windowState2 = this.mBottomFullWin;
        if (windowState2 != null) {
            this.mLastBottomFullWin = windowState2;
            this.mBottomFullWin = null;
        }
        this.mDimmingWindowState = null;
        this.mOtherFullOpaqueWindows.clear();
        beginSystemUIColor();
    }

    public void applyPostLayoutPolicyLw(WindowState win, WindowManager.LayoutParams attrs) {
        if (this.mDimmingWindowState == null && attrs.dimAmount > 0.001f && (attrs.flags & 2) != 0 && win.getWindowingMode() == 1) {
            String str = win.getDimmer().getHost() + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            if (str.contains(DIMLAYER_HOST_NAME) && win.showToCurrentUser()) {
                this.mDimmingWindowState = win;
            }
        }
        applySystemUIColor(win, attrs);
    }

    public void setTopFullscreenOpaqueWindowStateForMulti(WindowState win) {
        this.mTopFullscreenOpaqueWindowStateForMulti = win;
    }

    public void setRatioSwitch(WindowState ratioSwitch) {
        if (this.mRatioSwitch != ratioSwitch) {
            this.mRatioSwitch = ratioSwitch;
        }
    }

    public int finishPostLayoutPolicyLw() {
        WindowState windowState;
        applySystemUiNotify();
        updateRatioView();
        if (!this.mService.getIsVosProduct() && (windowState = this.mTopFullWin) != null && windowState.getAttrs().layoutInDisplayCutoutMode != 3) {
            this.isTopCutout = true;
            Rect frame = this.mTopFullWin.getFrameLw();
            this.mTmpRect.set(frame.left, frame.top, frame.right, frame.bottom);
            if (this.mRestrictedFrame.equals(this.mTmpRect)) {
                return 0;
            }
            VSlog.d(TAG, "DEBUG_SYSTEMUI:finishPostLayoutPolicyLw  ; mTopFullWin = " + this.mTopFullWin + " ; mRestrictedFrame = " + this.mRestrictedFrame + " ; mTmpRect = " + this.mTmpRect);
            this.mRestrictedFrame.set(this.mTmpRect);
            int changes = 0 | 1;
            return changes;
        }
        this.isTopCutout = false;
        if (this.mRestrictedFrame.isEmpty()) {
            return 0;
        }
        this.mRestrictedFrame.setEmpty();
        int changes2 = 0 | 1;
        return changes2;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mVivoHasNavBar=");
        pw.println(this.mVivoHasNavBar);
        pw.print(prefix);
        pw.print("mVivoForceHideNavBar=");
        pw.println(this.mVivoForceHideNavBar);
        if (this.mForceHideNavBarTokens.size() > 0) {
            pw.print(prefix);
            pw.println("ForceHideNavBarTokens:");
            for (int i = 0; i < this.mForceHideNavBarTokens.size(); i++) {
                pw.print(prefix);
                pw.print(prefix);
                pw.println(this.mForceHideNavBarTokens.get(i));
            }
        }
        if (this.mDimmingWindowState != null) {
            pw.print(prefix);
            pw.print("mDimmingWindowState=");
            pw.println(this.mDimmingWindowState);
        }
    }

    public WindowState getLetterBoxingWindow() {
        return this.mLetterBoxingWindow;
    }

    private void showEdgeShortcut() {
        if (!mEdgeShortcutList.isEmpty() && !this.mEdgetShortcutShowing) {
            if (!DEBUG_SHOW_SHORTCUT) {
                Iterator<WindowState> it = mEdgeShortcutList.iterator();
                while (it.hasNext()) {
                    WindowState win = it.next();
                    win.showLw(true);
                }
            }
            this.mEdgetShortcutShowing = true;
        }
    }

    private void hideEdgeShortcut() {
        if (!mEdgeShortcutList.isEmpty() && this.mEdgetShortcutShowing) {
            Iterator<WindowState> it = mEdgeShortcutList.iterator();
            while (it.hasNext()) {
                WindowState win = it.next();
                win.hideLw(true);
            }
            this.mEdgetShortcutShowing = false;
        }
    }

    public void setInputmethodTop(boolean inputmethodTop) {
        this.mInputmethodTop = inputmethodTop;
    }

    /* JADX WARN: Removed duplicated region for block: B:106:0x019b  */
    /* JADX WARN: Removed duplicated region for block: B:107:0x019d  */
    /* JADX WARN: Removed duplicated region for block: B:110:0x01a2  */
    /* JADX WARN: Removed duplicated region for block: B:114:0x01b0  */
    /* JADX WARN: Removed duplicated region for block: B:118:0x01c3  */
    /* JADX WARN: Removed duplicated region for block: B:119:0x01c5  */
    /* JADX WARN: Removed duplicated region for block: B:122:0x01d2  */
    /* JADX WARN: Removed duplicated region for block: B:129:0x01f0  */
    /* JADX WARN: Removed duplicated region for block: B:133:0x01fa  */
    /* JADX WARN: Removed duplicated region for block: B:137:0x0207  */
    /* JADX WARN: Removed duplicated region for block: B:138:0x0209  */
    /* JADX WARN: Removed duplicated region for block: B:141:0x0213  */
    /* JADX WARN: Removed duplicated region for block: B:142:0x0217  */
    /* JADX WARN: Removed duplicated region for block: B:145:0x021d  */
    /* JADX WARN: Removed duplicated region for block: B:146:0x035e  */
    /* JADX WARN: Removed duplicated region for block: B:148:0x0368 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:171:0x03b8  */
    /* JADX WARN: Removed duplicated region for block: B:172:0x03cf  */
    /* JADX WARN: Removed duplicated region for block: B:175:0x03f0  */
    /* JADX WARN: Removed duplicated region for block: B:176:0x03f5  */
    /* JADX WARN: Removed duplicated region for block: B:50:0x00d0 A[Catch: Exception -> 0x0132, TRY_LEAVE, TryCatch #3 {Exception -> 0x0132, blocks: (B:37:0x0099, B:48:0x00c6, B:50:0x00d0), top: B:185:0x0099 }] */
    /* JADX WARN: Removed duplicated region for block: B:67:0x012d  */
    /* JADX WARN: Removed duplicated region for block: B:76:0x0141  */
    /* JADX WARN: Removed duplicated region for block: B:77:0x0145  */
    /* JADX WARN: Removed duplicated region for block: B:88:0x0175  */
    /* JADX WARN: Removed duplicated region for block: B:94:0x0182  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void applySystemUiNotify() {
        /*
            Method dump skipped, instructions count: 1018
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoDisplayPolicyImpl.applySystemUiNotify():void");
    }

    private void notifySystemUI(int reason, Bundle infos) {
        if (this.mPolicy.mStatusBar == null) {
            return;
        }
        try {
            IStatusBarService statusbar = this.mPhoneWindowManager.getStatusBarService();
            if (statusbar != null) {
                if (WindowManagerDebugConfig.DEBUG) {
                    VSlog.d(TAG, "DEBUG_SYSTEMUI: notifySystemUI fired infos=" + infos);
                }
                statusbar.notifyInfo(reason, infos);
            }
        } catch (RemoteException e) {
        }
    }

    private boolean isNavigationBarGestureOff() {
        try {
            boolean isNavGestureOff = Settings.Secure.getInt(this.mContext.getContentResolver(), VivoRatioControllerUtilsImpl.NAVIGATION_GESTURE_ON) == 0;
            return isNavGestureOff;
        } catch (Exception e) {
            VSlog.w(TAG, "Get navigation bar settings error : SettingNotFoundException");
            return true;
        }
    }

    private void updateRatioView() {
        int newRatioViewState = 3;
        if (this.mPolicy.getDisplayId() != 0) {
            if (this.ARDBG) {
                VSlog.d(TAG, "updateRatioView not in default display");
            }
        } else if (this.mPolicy.mTopFullscreenOpaqueWindowState == null) {
            if (this.ARDBG) {
                VSlog.d(TAG, "updateRatioView mTopFullscreenOpaqueWindowState is null");
            }
            this.mVivoRatioControllerUtils.resetCurrentAppBound();
            this.mCurrentTopFullWin = null;
        } else if (this.mPolicy.mTopFullscreenOpaqueWindowState.getDisplayId() != 0) {
            if (this.ARDBG) {
                VSlog.d(TAG, "updateRatioView mTopFullscreenOpaqueWindowState is not in default display!");
            }
            this.mVivoRatioControllerUtils.resetCurrentAppBound();
        } else {
            boolean maybeCovered = this.mDialogTopWindow != null && this.mDialogTopType == 2;
            WindowManagerPolicy.WindowState windowState = this.mPolicy.mTopFullscreenOpaqueWindowState;
            if (windowState != null) {
                Rect appBounds = new Rect();
                this.mLastTopWinBounds = appBounds;
                this.mPolicy.mTopFullscreenOpaqueWindowState.getAppBounds(appBounds);
                if (!appBounds.isEmpty() && !isInVivoMultiWindowConsiderVisibilityFocusedDisplay() && !maybeCovered) {
                    newRatioViewState = 1;
                }
                ActivityRecord activityRecord = ((WindowState) windowState).mActivityRecord;
                if (activityRecord != null && (activityRecord.mDisableShowRatioView || activityRecord.inSizeCompatMode() || activityRecord.mSizeCompatForLagerWidth)) {
                    newRatioViewState = 3;
                }
                this.mLastRatioViewState = newRatioViewState;
                this.mVivoRatioControllerUtils.updateCurrentWindowState(windowState, windowState.getAttrs(), appBounds);
                if (this.ARDBG) {
                    VSlog.d(TAG, "DEBUG_RATIODIALOG: UPDATE mCurrentPkg=" + windowState.getAttrs().packageName + " newRatioViewState = " + newRatioViewState + " appRestricSize= 0appBounds=" + appBounds);
                }
                this.mVivoRatioControllerUtils.checkPendingDialog();
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:141:0x0469  */
    /* JADX WARN: Removed duplicated region for block: B:147:0x0488  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void layoutRatioView(com.android.server.wm.DisplayFrames r27, int r28, android.graphics.Rect r29, boolean r30, boolean r31, boolean r32, boolean r33) {
        /*
            Method dump skipped, instructions count: 1224
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoDisplayPolicyImpl.layoutRatioView(com.android.server.wm.DisplayFrames, int, android.graphics.Rect, boolean, boolean, boolean, boolean):void");
    }

    int determineResizeType(WindowState win) {
        win.getAttrs();
        if (win == this.mHomeIndicator) {
            if (this.ARDBG) {
                VSlog.d(TAG, "DEBUG_ASPECT_RATIO:RESIZE except win:" + win);
            }
            return 0;
        }
        win.setAspectRatioResizeType(0);
        return 0;
    }

    public void adjustWindowLayoutBounds(WindowState win, WindowState attached, Rect pf, Rect df, Rect of, Rect cf, Rect vf, Rect dcf, Rect sf, Rect osf) {
        WindowState win2;
        if (!IVivoRatioControllerUtils.EAR_PHONE_SUPPORT) {
            return;
        }
        if (attached == null) {
            win2 = win;
        } else {
            win2 = attached;
        }
        int rotation = this.mDisplayFrames.mRotation;
        boolean isLandscape = rotation == 1 || rotation == 3;
        WindowManager.LayoutParams attrs = win2.getAttrs();
        if (attrs.type == 2000) {
            if (!isInVivoMultiWindowConsiderVisibilityFocusedDisplay() && isLandscape && this.isTopCutout) {
                this.mTmpRect.set(this.mRestrictedFrame);
                this.mTmpRect.union(this.mDisplayFrames.mDisplayCutoutSafe);
                cf.intersectUnchecked(this.mTmpRect);
            }
        } else if (attrs.type == 2011) {
            int resizeType = determineResizeType(win2);
            if (isLandscape && (resizeType & 4) != 0 && rotation == 1) {
                int max = Math.max(pf.left, this.mVivoRatioControllerUtils.getAlienScreenCoverInsetTop());
                df.left = max;
                pf.left = max;
            }
        } else if (attrs.type >= 1 && attrs.type <= 1999) {
            win2.setAspectRatioResizeType(0);
            int resizeType2 = determineResizeType(win2);
            int fl = attrs.flags;
            int sysUiFl = PolicyControl.getSystemUiVisibility(win2, (WindowManager.LayoutParams) null);
            int internalFlag = attrs.internalFlag;
            boolean hideStatusBar = ((attrs.type == 1 || attrs.type == 3) && (sysUiFl & 4) == 0 && (fl & Consts.ProcessStates.FOCUS) == 0) ? false : true;
            boolean forceFullVertical = (internalFlag & 1) != 0 || "com.ss.android.ugc.aweme/com.ss.android.ugc.aweme.main.MainActivity".equals(attrs.getTitle().toString());
            boolean forceNotFullVertical = (internalFlag & 2) != 0;
            boolean isInVivoFreeform = false;
            if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isVivoFreeFormStackMax() && win2.inFreeformWindowingMode()) {
                isInVivoFreeform = true;
            }
            if (resizeType2 != 0 && !isInVivoFreeform) {
                if (this.ARDBG) {
                    VSlog.d(TAG, "DEBUG_ASPECT_RATIO: shrink before pf=" + pf + " resizeType=" + resizeType2 + " forceFullVertical = " + forceFullVertical + " forceNotFullVertical = " + forceNotFullVertical);
                }
                if (!isLandscape) {
                    if (!forceFullVertical && (((resizeType2 & 4) != 0 && hideStatusBar) || forceNotFullVertical)) {
                        int max2 = Math.max(pf.top, this.mVivoRatioControllerUtils.getAlienScreenCoverInsetTop());
                        df.top = max2;
                        pf.top = max2;
                    }
                } else if ((resizeType2 & 4) != 0 && rotation == 1) {
                    int max3 = Math.max(pf.left, this.mVivoRatioControllerUtils.getAlienScreenCoverInsetTop());
                    df.left = max3;
                    pf.left = max3;
                }
                if (this.ARDBG) {
                    VSlog.d(TAG, "DEBUG_ASPECT_RATIO: shrink after pf=" + pf + " resizeType=" + resizeType2);
                }
            }
        } else if (attrs.type == 2002 || attrs.type == 2003) {
            int resizeType3 = determineResizeType(win2);
            if (isLandscape) {
                if ((resizeType3 & 4) != 0 && rotation == 1) {
                    int max4 = Math.max(pf.left, this.mVivoRatioControllerUtils.getAlienScreenCoverInsetTop());
                    df.left = max4;
                    pf.left = max4;
                }
            } else if ((resizeType3 & 4) != 0) {
                df.top = Math.max(pf.top, this.mVivoRatioControllerUtils.getAlienScreenCoverInsetTop());
            }
        }
    }

    private void beginSystemUIColor() {
        this.navColors.clear();
        this.mWinResponsibleForGesture = this.mPolicy.mDisplayContent.getWindowContainerForGesture((WindowState) null);
    }

    private void applySystemUIColor(WindowState win, WindowManager.LayoutParams attrs) {
        WindowState windowState = this.mWinResponsibleForGesture;
        if (windowState != null && windowState.getTask() != win.getTask()) {
            if (WindowManagerDebugConfig.DEBUG) {
                VSlog.i(TAG, "applySystemUIColor, skip win = " + win + ", return as task different");
            }
        } else if (!NavigationBarPolicy.hasNavigationBarWindow(win, attrs) && (attrs.flags & 2) == 0) {
        } else {
            int shouldApply = win.shouldApplyNavColor();
            if (shouldApply == NavigationBarPolicy.APPLY_COLOR_NONE) {
                if (WindowManagerDebugConfig.DEBUG) {
                    VSlog.i(TAG, "applySystemUIColor, skip win = " + win + ", color = " + Integer.toHexString(win.getWindowNavColor()) + ", shouldApply = " + win.shouldApplyNavColor() + ", visible = " + win.isVisibleLw() + ", animating = " + win.isAnimatingLw());
                    return;
                }
                return;
            }
            if (shouldApply == NavigationBarPolicy.APPLY_COLOR_FULL) {
                int navcolor = win.getWindowNavColor();
                if (!PixelFormat.formatHasAlpha(attrs.format)) {
                    navcolor |= -16777216;
                }
                if (attrs.alpha < 1.0f) {
                    navcolor = DrawableUtils.blendAlpha(navcolor, attrs.alpha);
                }
                if (WindowManagerDebugConfig.DEBUG) {
                    VSlog.i(TAG, "applySystemUIColor, win = " + win + ", color = " + Integer.toHexString(navcolor));
                }
                this.navColors.add(Integer.valueOf(navcolor));
            }
            if ((attrs.flags & 2) != 0) {
                this.navColors.add(Integer.valueOf(Color.argb((int) (attrs.dimAmount * 255.0f), 0, 0, 0)));
            }
        }
    }

    private int computeNavColor() {
        int color = -16777216;
        int count = this.navColors.size();
        for (int i = count - 1; i >= 0; i--) {
            int maskColor = this.navColors.get(i).intValue();
            color = DrawableUtils.blendColor(color, maskColor);
        }
        return color;
    }

    public int detectDialogType(WindowState win, WindowManager.LayoutParams attrs) {
        int type;
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            VSlog.i(TAG, "DEBUG_SYSTEMUI:detectDialogType win:" + win + " attrs:" + attrs);
        }
        PolicyControl.getWindowFlags(win, attrs);
        DisplayMetrics dm = this.mVivoRatioControllerUtils.getCurrentMetrics(this.mContext);
        Rect rect = win.getFrameLw();
        int height = rect.height();
        int width = rect.width();
        int rotation = this.mDisplayFrames.mRotation;
        boolean portraitRotation = true;
        portraitRotation = (rotation == 1 || rotation == 3) ? false : false;
        int navbarHeight = this.mVivoRatioControllerUtils.getNavigationBarHeight();
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            VSlog.i(TAG, "detectDialogType rect:" + rect + " dm:" + dm + " rotation:" + rotation + " navbarHeight:" + navbarHeight);
        }
        if ((portraitRotation && width >= dm.widthPixels && rect.bottom >= dm.heightPixels - navbarHeight) || (!portraitRotation && height >= dm.heightPixels && rect.right >= dm.widthPixels - navbarHeight)) {
            type = 2;
        } else {
            type = 1;
        }
        this.mDialogTopType = type;
        if ((attrs.privateFlags & Dataspace.TRANSFER_LINEAR) != 0 || type == 2) {
            this.mDialogTopAttrs = attrs;
            this.mDialogTopWindow = win;
        }
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            VSlog.d(TAG, "DEBUG_DIALOG_TOP:detectDialogType win:" + win + " attrs:" + attrs + " type = " + type);
        }
        return type;
    }

    public void adjustFullScreenFlag(boolean topIsFullscreen) {
        this.mHandler.removeMessages(101);
        Message msg = this.mHandler.obtainMessage(101, topIsFullscreen ? 1 : 0, 0);
        msg.sendToTarget();
    }

    public void initVivoNavBar() {
        boolean hasNoBar;
        if (MultiDisplayManager.isVivoDisplay(this.mPolicy.getDisplayId())) {
            this.mPolicy.mHasStatusBar = false;
            this.mPolicy.mHasNavigationBar = true;
            return;
        }
        String valueForNav = "1";
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys.vivo", "1");
        if ("0".equals(navBarOverride)) {
            this.mPolicy.mHasNavigationBar = true;
        } else {
            this.mPolicy.mHasNavigationBar = false;
        }
        int gesture = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), VivoRatioControllerUtilsImpl.NAVIGATION_GESTURE_ON, getDefaultNavBar(), -2);
        if (!this.mService.getIsVosProduct()) {
            hasNoBar = gesture != 0;
        } else {
            hasNoBar = gesture == 1;
        }
        if (!"1".equals(navBarOverride) && !hasNoBar) {
            valueForNav = "0";
        }
        SystemProperties.set("qemu.hw.mainkeys", valueForNav);
        if ("0".equals(valueForNav)) {
            this.mVivoHasNavBar = true;
        } else {
            this.mVivoHasNavBar = false;
        }
    }

    private int getDefaultNavBar() {
        boolean isOverseas = FtBuild.isOverSeas();
        boolean isVosProduct = this.mService.getIsVosProduct();
        if (isOverseas || isVosProduct) {
            VSlog.i(TAG, "getDefaultNavBar = 0, isOverseas = " + isOverseas + " isVosProduct = " + isVosProduct);
            return 0;
        }
        float romVersion = FtBuild.getRomVersion();
        if (romVersion != 11.5f) {
            VSlog.i(TAG, "getDefaultNavBar = 1, as rom version = " + romVersion);
            return 1;
        }
        VSlog.i(TAG, "getDefaultNavBar = 0, as rom version = " + romVersion);
        return 0;
    }

    public void setVivoNavBar(boolean hasNav) {
        if (MultiDisplayManager.isVivoDisplay(this.mPolicy.getDisplayId())) {
            this.mVivoHasNavBar = hasNav;
            return;
        }
        this.mVivoHasNavBar = hasNav;
        String value = hasNav ? "0" : "1";
        SystemProperties.set("qemu.hw.mainkeys", value);
        VSlog.i(TAG, "setVivoNavBar " + hasNav + " " + Debug.getCallers(3));
        if (MultiDisplayManager.isMultiDisplay) {
            int curDisplayId = this.mPolicy.getDisplayId();
            this.mService.mRoot.getDisplayContent(curDisplayId == 0 ? 4096 : 0).getDisplayPolicy().setVivoNavBarAsMultiSync(hasNav);
        }
    }

    public void setVivoNavBarAsMultiSync(boolean hasNav) {
        this.mVivoHasNavBar = hasNav;
        if (hasNav) {
        }
        VSlog.i(TAG, "setVivoNavBarAsMultiSync " + hasNav);
    }

    public void notifyFocusGained() {
        this.mNeedUpdateForFocusGained = true;
        this.mPolicy.mStatusBarController.mDisplayId = this.mPolicy.getDisplayId();
        this.mPolicy.mNavigationBarController.mDisplayId = this.mPolicy.getDisplayId();
    }

    public boolean getVivoHasNavBar() {
        return this.mVivoHasNavBar;
    }

    private boolean isHomeIndicatorOn() {
        return !this.mVivoHasNavBar;
    }

    public void changeNavigationBarPosition(boolean changed) {
        this.mVivoChangeNavPosition = changed;
    }

    public int navigationBarPositionForLand(int displayRotation) {
        if (MultiDisplayManager.isVivoDisplay(this.mPolicy.getDisplayId())) {
            return 1;
        }
        if (!this.mVivoChangeNavPosition || this.mService.getIsVosProduct()) {
            if (displayRotation == 3) {
                return 1;
            }
            return displayRotation == 1 ? 2 : -1;
        }
        return 2;
    }

    public boolean canShowConfirm(WindowState win) {
        int layer = this.mService.mPolicy.getWindowLayerLw(win);
        int confirmLayer = this.mService.mPolicy.getWindowLayerFromTypeLw(2017);
        if (layer > confirmLayer) {
            return false;
        }
        return true;
    }

    public boolean getVivoForceHideNavBar() {
        return this.mVivoForceHideNavBar || isNeededHideNavBarInSplit();
    }

    public boolean isNeededHideNavBarInSplit() {
        if (this.mService.mAtmService.isVivoVosMultiWindowSupport() && this.mService.isInVivoMultiWindowConsiderVisibilityFocusedDisplay() && !isNavigationBarGestureOff() && isNotPortrait()) {
            return true;
        }
        return false;
    }

    public void forceHideNavBar(IBinder token, boolean hide) {
        boolean needUpdate;
        if (MultiDisplayManager.isVivoDisplay(this.mPolicy.getDisplayId())) {
            this.mVivoForceHideNavBar = false;
            return;
        }
        synchronized (this.mForceHideNavBarTokens) {
            VSlog.d(TAG, "pwm state is " + hide);
            boolean lastForceHide = this.mVivoForceHideNavBar;
            this.mVivoForceHideNavBar = hide;
            int pid = Binder.getCallingPid();
            Iterator<ForceHideNavBarToken> it = this.mForceHideNavBarTokens.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ForceHideNavBarToken candidate = it.next();
                if (candidate.pid == pid) {
                    candidate.token.unlinkToDeath(candidate, 0);
                    it.remove();
                    break;
                }
            }
            if (token != null) {
                ForceHideNavBarToken forceHideNavBarToken = new ForceHideNavBarToken(pid, token, hide) { // from class: com.android.server.wm.VivoDisplayPolicyImpl.2
                    @Override // android.os.IBinder.DeathRecipient
                    public void binderDied() {
                        boolean needUpdate2;
                        synchronized (VivoDisplayPolicyImpl.this.mForceHideNavBarTokens) {
                            VSlog.d(VivoDisplayPolicyImpl.TAG, "pwm reset hide state!!");
                            boolean lastForceHide2 = VivoDisplayPolicyImpl.this.mVivoForceHideNavBar;
                            VivoDisplayPolicyImpl.this.mForceHideNavBarTokens.remove(this);
                            int size = VivoDisplayPolicyImpl.this.mForceHideNavBarTokens.size();
                            if (size > 0) {
                                ForceHideNavBarToken lastCallClient = (ForceHideNavBarToken) VivoDisplayPolicyImpl.this.mForceHideNavBarTokens.get(size - 1);
                                VivoDisplayPolicyImpl.this.mVivoForceHideNavBar = lastCallClient.forceHide;
                            } else {
                                VivoDisplayPolicyImpl.this.mVivoForceHideNavBar = false;
                            }
                            needUpdate2 = lastForceHide2 != VivoDisplayPolicyImpl.this.mVivoForceHideNavBar;
                        }
                        if (needUpdate2) {
                            synchronized (VivoDisplayPolicyImpl.this.mService.mGlobalLock) {
                                VivoDisplayPolicyImpl.this.mPolicy.mDisplayContent.setLayoutNeeded();
                                VivoDisplayPolicyImpl.this.mService.mWindowPlacerLocked.requestTraversal();
                            }
                        }
                    }
                };
                try {
                    token.linkToDeath(forceHideNavBarToken, 0);
                    this.mForceHideNavBarTokens.add(forceHideNavBarToken);
                } catch (RemoteException e) {
                    VSlog.w(TAG, "pwm hang: given caller IBinder is already dead.");
                    this.mVivoForceHideNavBar = lastForceHide;
                }
            }
            needUpdate = lastForceHide != this.mVivoForceHideNavBar;
        }
        if (needUpdate) {
            synchronized (this.mService.mGlobalLock) {
                this.mPolicy.mDisplayContent.setLayoutNeeded();
                this.mService.mWindowPlacerLocked.requestTraversal();
            }
        }
    }

    /* loaded from: classes.dex */
    abstract class ForceHideNavBarToken implements IBinder.DeathRecipient {
        final boolean forceHide;
        final int pid;
        final IBinder token;

        ForceHideNavBarToken(int _pid, IBinder _token, boolean _hide) {
            this.pid = _pid;
            this.token = _token;
            this.forceHide = _hide;
        }

        public String toString() {
            return "ForceHideNavBarToken{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.pid + " " + this.token + " " + this.forceHide + " }";
        }
    }

    public WindowState getFocusedWindow() {
        return this.mPolicy.mFocusedWindow;
    }

    private boolean isInVivoMultiWindowConsiderVisibilityFocusedDisplay() {
        return this.mService.isInVivoMultiWindowConsiderVisibilityFocusedDisplay();
    }

    /* loaded from: classes.dex */
    private final class VivoSwipeObserver extends ContentObserver {
        VivoSwipeObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            VivoDisplayPolicyImpl.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("gamecube_disable_navigation_gesture_on"), true, this, -1);
            VivoDisplayPolicyImpl.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("gamecube_shorten_status_bar_trigger_area_on"), true, this, -1);
            VivoDisplayPolicyImpl.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("is_game_mode"), true, this, -1);
            onChange(false);
        }

        void unObserve() {
            try {
                VivoDisplayPolicyImpl.this.mContext.getContentResolver().unregisterContentObserver(this);
            } catch (Exception e) {
                VSlog.e(VivoDisplayPolicyImpl.TAG, "VivoSwipeObserver unObserve cause exception", e);
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VivoDisplayPolicyImpl vivoDisplayPolicyImpl = VivoDisplayPolicyImpl.this;
            boolean shortenStatusBarTriggerArea = false;
            vivoDisplayPolicyImpl.mVivoDisableSwape = Settings.System.getIntForUser(vivoDisplayPolicyImpl.mContext.getContentResolver(), "gamecube_disable_navigation_gesture_on", 0, -2) == 1;
            boolean shortenStatusBarTriggerArea2 = Settings.System.getIntForUser(VivoDisplayPolicyImpl.this.mContext.getContentResolver(), "gamecube_shorten_status_bar_trigger_area_on", 0, -2) == 1;
            boolean isGameMode = Settings.System.getIntForUser(VivoDisplayPolicyImpl.this.mContext.getContentResolver(), "is_game_mode", 0, -2) == 1;
            VSlog.d(VivoDisplayPolicyImpl.TAG, "disableSwape:" + VivoDisplayPolicyImpl.this.mVivoDisableSwape + "gamecube_shorten:" + shortenStatusBarTriggerArea2 + " isGameMode:" + isGameMode);
            if (isGameMode && shortenStatusBarTriggerArea2) {
                shortenStatusBarTriggerArea = true;
            }
            VivoDisplayPolicyImpl.this.mPolicy.setSystemGesturesGameModeRejectionEnable(shortenStatusBarTriggerArea);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetSwipeCount() {
        VSlog.d(TAG, "vivodebug resetSwipeCount.");
        this.mVivoNavSwipeCount = 0;
        this.mVivoStatusSwipeCount = 0;
    }

    private boolean updateSwipeCount(boolean fromtop, boolean fromLeft) {
        VSlog.d(TAG, "vivodebug fromtop = " + fromtop + " ,navCount = " + this.mVivoNavSwipeCount + " ,statusCount = " + this.mVivoStatusSwipeCount);
        if ((!fromtop && this.mVivoNavSwipeCount == 0) || (fromtop && this.mVivoStatusSwipeCount == 0)) {
            resetSwipeCount();
            this.mHandler.removeCallbacks(this.mSwipeTimeoutRunnable);
            this.mHandler.postDelayed(this.mSwipeTimeoutRunnable, 3000L);
        }
        if (fromtop) {
            this.mVivoStatusSwipeCount++;
        }
        if (fromLeft) {
            if (this.mPolicy.mNavigationBarPosition == 1) {
                this.mVivoNavSwipeCount++;
            }
        } else if (this.mPolicy.mNavigationBarPosition == 2) {
            this.mVivoNavSwipeCount++;
        }
        if (this.mVivoNavSwipeCount == 1 || this.mVivoStatusSwipeCount == 1) {
            Toast.makeText(this.mContext, 51249664, 0).show();
        }
        if (this.mVivoNavSwipeCount == 2 || this.mVivoStatusSwipeCount == 2) {
            resetSwipeCount();
            return true;
        }
        return false;
    }

    public boolean onSwipeFromTop() {
        return this.mVivoDisableSwape && !updateSwipeCount(true, false);
    }

    public boolean onSwipeFromRight() {
        return this.mVivoDisableSwape && !updateSwipeCount(false, false);
    }

    public boolean onSwipeFromLeft() {
        return this.mVivoDisableSwape && !updateSwipeCount(false, true);
    }

    public void unRegisterContentDetection() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.post(new Runnable() { // from class: com.android.server.wm.-$$Lambda$VivoDisplayPolicyImpl$G4N5XXydSXCNL97dn_Ok4sBzYDY
                @Override // java.lang.Runnable
                public final void run() {
                    VivoDisplayPolicyImpl.this.lambda$unRegisterContentDetection$1$VivoDisplayPolicyImpl();
                }
            });
        }
    }

    public /* synthetic */ void lambda$unRegisterContentDetection$1$VivoDisplayPolicyImpl() {
        VivoSwipeObserver vivoSwipeObserver = this.mVivoSwipeObserver;
        if (vivoSwipeObserver != null) {
            vivoSwipeObserver.unObserve();
        }
    }

    public int getLastFullscreenStackSysUiFlagsForFreeForm() {
        return this.mLastFullscreenStackSysUiFlagsForFreeForm;
    }

    public void setLastFullscreenStackSysUiFlagsForFreeForm(int flags) {
        this.mLastFullscreenStackSysUiFlagsForFreeForm = flags;
    }

    /* loaded from: classes.dex */
    private final class SogouImeGameSwitchObserver extends ContentObserver {
        public SogouImeGameSwitchObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            try {
                ContentResolver resolver = VivoDisplayPolicyImpl.this.mContext.getContentResolver();
                resolver.registerContentObserver(Uri.parse(VivoDisplayPolicyImpl.CONFIG_URI_SOGOU_SWITCH), true, this);
                VivoDisplayPolicyImpl.this.updateGameSwitchState();
            } catch (Exception e) {
                e.printStackTrace();
                VSlog.e(VivoDisplayPolicyImpl.TAG, "SogouImeGameSwitchObserver observe!!! e=" + e);
            }
        }

        void unObserve() {
            try {
                ContentResolver resolver = VivoDisplayPolicyImpl.this.mContext.getContentResolver();
                resolver.unregisterContentObserver(this);
            } catch (Exception e) {
                e.printStackTrace();
                VSlog.e(VivoDisplayPolicyImpl.TAG, "SogouImeGameSwitchObserver unObserve!!! e=" + e);
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            VSlog.d(VivoDisplayPolicyImpl.TAG, "SogouImeGameSwitchObserver onChange...");
            VivoDisplayPolicyImpl.this.updateGameSwitchState();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateGameSwitchState() {
        VSlog.d(TAG, "updateGameSwitchState...");
        ContentResolver cr = this.mContext.getContentResolver();
        Cursor cursor = null;
        try {
            try {
                cursor = cr.query(Uri.parse(CONFIG_URI_SOGOU_SWITCH), null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    if (cursor.getCount() > 0) {
                        this.mSogouImeGameSwitchEnable = cursor.getInt(0);
                    }
                } else {
                    this.mSogouImeGameSwitchEnable = 0;
                }
                if (cursor == null) {
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                VSlog.e(TAG, "updateGameSwitchState!!! e=" + e);
                if (0 == 0) {
                    return;
                }
            }
            cursor.close();
        } catch (Throwable th) {
            if (0 != 0) {
                cursor.close();
            }
            throw th;
        }
    }

    public boolean isForcingShowNavBarForSecureMethod() {
        return this.mForcingShowNavBarForSecureMethod;
    }

    public void setForcingShowNavBarForSecureMethod(boolean forcingShowNavBarForSecureMethod) {
        this.mForcingShowNavBarForSecureMethod = forcingShowNavBarForSecureMethod;
    }

    public int getSogouImeGameSwitchEnable() {
        return this.mSogouImeGameSwitchEnable;
    }

    public boolean isNavigationbarVisible() {
        return this.mPolicy.mNavigationBar != null && this.mPolicy.mNavigationBar.isVisibleLw();
    }

    public boolean directFreeformNeed() {
        WindowManagerService windowManagerService = this.mService;
        boolean isInDirectFreeform = windowManagerService != null && windowManagerService.isInDirectFreeformState();
        if (isInDirectFreeform && !this.mPolicy.mStatusBar.isVisibleLw()) {
            return false;
        }
        return isInDirectFreeform;
    }

    public boolean isFreeformVisible() {
        return this.mPolicy.mDisplayContent != null && this.mPolicy.mDisplayContent.getDefaultTaskDisplayArea().isStackVisible(5);
    }

    public boolean setDisplayFrameStableTopUnrestricted(DisplayFrames displayFrames) {
        WindowManagerService windowManagerService = this.mService;
        if (windowManagerService == null || !windowManagerService.isVivoFreeFormValid() || !this.mService.isInVivoFreeform() || this.mService.isVivoFreeFormStackMax()) {
            return false;
        }
        boolean isfreeformvisible = isFreeformVisible();
        boolean isInDirectFreeform = directFreeformNeed();
        return isfreeformvisible && displayFrames.mDisplayCutoutSafe.top == Integer.MIN_VALUE && !isInDirectFreeform;
    }

    public boolean setDockFrameUnrestricted() {
        WindowManagerService windowManagerService = this.mService;
        if (windowManagerService != null && windowManagerService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isVivoFreeFormStackMax() && isFreeformVisible() && !directFreeformNeed()) {
            return true;
        }
        return false;
    }

    public boolean setDispalyFrameSystemUnrestricted() {
        WindowManagerService windowManagerService = this.mService;
        if (windowManagerService != null && windowManagerService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isVivoFreeFormStackMax() && isFreeformVisible() && !directFreeformNeed()) {
            return true;
        }
        return false;
    }

    public boolean changeCfInFreeformIfNeed(WindowState win, Rect cf) {
        WindowManagerService windowManagerService = this.mService;
        if (windowManagerService != null && windowManagerService.isVivoFreeFormValid() && win.inFreeformWindowingMode() && this.mPolicy.mStatusBar != null && !this.mPolicy.mStatusBar.isVisibleLw() && cf.top > 0) {
            return true;
        }
        return false;
    }

    public boolean changeDfInFreeformIfNeed(WindowState win, Rect df) {
        WindowManager.LayoutParams attrs = win.getAttrs();
        int fl = PolicyControl.getWindowFlags(win, attrs);
        int cutoutMode = attrs.layoutInDisplayCutoutMode;
        boolean layoutInScreen = (fl & 256) == 256;
        boolean layoutInsetDecor = (fl & Dataspace.STANDARD_BT709) == 65536;
        WindowManagerService windowManagerService = this.mService;
        return windowManagerService != null && windowManagerService.isVivoFreeFormValid() && win.inFreeformWindowingMode() && layoutInScreen && layoutInsetDecor && attrs.keepFullScreen != 1 && (cutoutMode == 0 || cutoutMode == 1) && this.mDisplayFrames.mRotation == 1 && df.left > 0;
    }

    public void setForcingShowNavBar(WindowState win, WindowManager.LayoutParams attrs) {
        if (win.toString().contains("SecureInputMethod") || getSogouImeGameSwitchEnable() == 0) {
            setForcingShowNavBarForSecureMethod(true);
        }
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isVivoFreeFormStackMax() && "com.emoji.keyboard.touchpal.vivo".equals(attrs.packageName)) {
            setForcingShowNavBarForSecureMethod(false);
        }
    }

    public boolean shouldHideStatusBarByVivoFreeform() {
        return this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isVivoFreeFormStackMax();
    }

    public boolean isVivoFreeformMaxState() {
        return this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && this.mService.isVivoFreeFormStackMax();
    }

    public boolean hideStatusBarInFreeform() {
        if (this.mService.isVivoFreeFormValid() && this.mService.isInVivoFreeform()) {
            return true;
        }
        return false;
    }

    public boolean notHideStatusBarInFreeformInSpecial() {
        if (this.mService.isVivoFreeFormStackMax() || !this.mPolicy.mDisplayContent.getDefaultTaskDisplayArea().isStackVisible(5) || this.mService.isInDirectFreeformState()) {
            return true;
        }
        return false;
    }

    public boolean changeOutInsetsInFreeform() {
        WindowManagerService windowManagerService;
        boolean isFreeformVisible = this.mPolicy.mDisplayContent != null && this.mPolicy.mDisplayContent.getDefaultTaskDisplayArea().isStackVisible(5);
        WindowManagerService windowManagerService2 = this.mService;
        boolean isInDirectFreeform = windowManagerService2 != null && windowManagerService2.isInDirectFreeformState();
        return this.mPolicy.mStatusBar != null && (windowManagerService = this.mService) != null && windowManagerService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && isFreeformVisible && !isInDirectFreeform;
    }

    public boolean ignoreForceShowSystemBarsInFreeform(int vis, boolean freeformStackVisible) {
        WindowManagerService windowManagerService = this.mService;
        boolean isInDirectFreeform = windowManagerService != null && windowManagerService.isInDirectFreeformState();
        int lastFullscennVis = 0;
        if (this.mPolicy.mTopFullscreenOpaqueWindowState != null) {
            lastFullscennVis = PolicyControl.getSystemUiVisibility(this.mPolicy.mTopFullscreenOpaqueWindowState, (WindowManager.LayoutParams) null);
        }
        boolean currentWinOrLastWinSetFullscreen = ((vis | lastFullscennVis) & 1540) != 0;
        if (isInDirectFreeform && currentWinOrLastWinSetFullscreen) {
            isInDirectFreeform = false;
        }
        WindowManagerService windowManagerService2 = this.mService;
        return windowManagerService2 != null && windowManagerService2.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isVivoFreeFormStackMax() && freeformStackVisible && !isInDirectFreeform;
    }

    public boolean transientStatusBarAllowedInFreeform() {
        WindowManagerService windowManagerService = this.mService;
        if (windowManagerService != null && windowManagerService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isVivoFreeFormStackMax()) {
            return true;
        }
        return false;
    }

    public boolean transientNavBarAllowedInFreeform() {
        WindowManagerService windowManagerService = this.mService;
        if (windowManagerService != null && windowManagerService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isVivoFreeFormStackMax()) {
            return true;
        }
        return false;
    }

    public boolean changeVisInVivoFreeform(boolean forceShowNavBar, int vis, WindowState win) {
        WindowManagerService windowManagerService = this.mService;
        boolean isInDirectFreeform = windowManagerService != null && windowManagerService.isInDirectFreeformState();
        if (isInDirectFreeform && (vis & 4) != 0) {
            isInDirectFreeform = false;
        }
        int lastFullscreenStackSysUiFlagsForFreeForm = getLastFullscreenStackSysUiFlagsForFreeForm();
        boolean lastimmersive = (lastFullscreenStackSysUiFlagsForFreeForm & Consts.ProcessStates.VIRTUAL_DISPLAY) != 0;
        boolean lastimmersiveSticky = (lastFullscreenStackSysUiFlagsForFreeForm & 4096) != 0;
        boolean lastnavAllowedHidden = lastimmersive || lastimmersiveSticky;
        boolean portrait = this.mPolicy.mDisplayContent.getRotation() == 0 || this.mPolicy.mDisplayContent.getRotation() == 2;
        this.mPolicy.mDisplayContent.getDefaultTaskDisplayArea().isStackVisible(5);
        return this.mService.isInVivoFreeform() && !this.mService.isVivoFreeFormStackMax() && win != null && win.inFreeformWindowingMode() && win.isVisibleLw() && lastnavAllowedHidden && (!portrait ? isForcingShowNavBarForSecureMethod() : forceShowNavBar) && !isInDirectFreeform;
    }

    public boolean ignoreSetNavBarOpaqueFlagInFreeform() {
        WindowManagerService windowManagerService = this.mService;
        if (windowManagerService != null && windowManagerService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isVivoFreeFormStackMax()) {
            return true;
        }
        return false;
    }

    public void unRegisterSogouImeObserver() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.post(new Runnable() { // from class: com.android.server.wm.-$$Lambda$VivoDisplayPolicyImpl$j1RKBBfEiy7e2wPJuG8y_Fc8MTo
                @Override // java.lang.Runnable
                public final void run() {
                    VivoDisplayPolicyImpl.this.lambda$unRegisterSogouImeObserver$2$VivoDisplayPolicyImpl();
                }
            });
        }
    }

    public /* synthetic */ void lambda$unRegisterSogouImeObserver$2$VivoDisplayPolicyImpl() {
        SogouImeGameSwitchObserver sogouImeGameSwitchObserver = this.mSogouImeGameSwitchObserver;
        if (sogouImeGameSwitchObserver != null) {
            sogouImeGameSwitchObserver.unObserve();
        }
    }

    private boolean isScrollBoostBlackListGame() {
        DisplayPolicy displayPolicy = this.mPolicy;
        if (displayPolicy == null) {
            VSlog.i(TAG, "DisplayPolicy mPolicy is null");
            return false;
        }
        WindowState topW = displayPolicy.mTopFullscreenOpaqueWindowState;
        if (topW == null) {
            VSlog.i(TAG, "WindowState topW is null");
            return false;
        }
        String currentPackage = topW.getAttrs().packageName;
        return currentPackage != null && mScrollBoostBlackList.contains(currentPackage);
    }

    /* JADX WARN: Code restructure failed: missing block: B:9:0x002c, code lost:
        if ((r4.flags & android.hardware.graphics.common.V1_0.Dataspace.TRANSFER_HLG) == 33554432) goto L14;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean isTopAppGame() {
        /*
            r7 = this;
            r0 = 0
            r1 = 0
            android.app.IActivityTaskManager r2 = android.app.ActivityTaskManager.getService()     // Catch: java.lang.Exception -> L32
            r3 = 1
            java.util.List r2 = r2.getFilteredTasks(r3, r1)     // Catch: java.lang.Exception -> L32
            java.lang.Object r2 = r2.get(r1)     // Catch: java.lang.Exception -> L32
            android.app.ActivityManager$RunningTaskInfo r2 = (android.app.ActivityManager.RunningTaskInfo) r2     // Catch: java.lang.Exception -> L32
            android.content.Context r4 = r7.mContext     // Catch: java.lang.Exception -> L32
            android.content.pm.PackageManager r4 = r4.getPackageManager()     // Catch: java.lang.Exception -> L32
            android.content.ComponentName r5 = r2.topActivity     // Catch: java.lang.Exception -> L32
            java.lang.String r5 = r5.getPackageName()     // Catch: java.lang.Exception -> L32
            android.content.pm.ApplicationInfo r4 = r4.getApplicationInfo(r5, r1)     // Catch: java.lang.Exception -> L32
            if (r4 == 0) goto L30
            int r5 = r4.category     // Catch: java.lang.Exception -> L32
            if (r5 == 0) goto L2e
            int r5 = r4.flags     // Catch: java.lang.Exception -> L32
            r6 = 33554432(0x2000000, float:9.403955E-38)
            r5 = r5 & r6
            if (r5 != r6) goto L2f
        L2e:
            r1 = r3
        L2f:
            r0 = r1
        L30:
            return r0
        L32:
            r2 = move-exception
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoDisplayPolicyImpl.isTopAppGame():boolean");
    }

    public void onVerticalFlingBoost(int duration) {
        String currentPackage = this.mContext.getPackageName();
        if (isScrollBoostBlackListGame()) {
            VSlog.i(TAG, "isScrollBoostBlackListGame is true");
        } else if (SCROLL_BOOST_SS_ENABLE) {
            if (this.mPerfBoostFling == null) {
                this.mPerfBoostFling = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
                this.mIsPerfBoostFlingAcquired = false;
            }
            AbsVivoPerfManager absVivoPerfManager = this.mPerfBoostFling;
            if (absVivoPerfManager == null) {
                Slog.e(TAG, "Error: boost object null");
                return;
            }
            absVivoPerfManager.perfHint(4224, currentPackage, duration + 160, 1);
            this.mIsPerfBoostFlingAcquired = true;
        }
    }

    public void onScrollBoost(boolean started) {
        String currentPackage = this.mContext.getPackageName();
        boolean isGame = isScrollBoostBlackListGame();
        if (this.mPerfBoostDrag == null) {
            this.mPerfBoostDrag = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        }
        if (this.mPerfBoostDrag == null) {
            VSlog.e(TAG, "Error: boost object null");
            return;
        }
        if (SCROLL_BOOST_SS_ENABLE && !isGame) {
            if (this.mPerfBoostPrefling == null) {
                this.mPerfBoostPrefling = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
            }
            if (this.mPerfBoostPrefling == null) {
                VSlog.e(TAG, "Error: boost object null");
            }
            this.mPerfBoostPrefling.perfHint(4224, currentPackage, -1, 4);
        }
        if (!isGame && started) {
            this.mPerfBoostDrag.perfHint(4231, currentPackage, -1, 1);
        } else {
            this.mPerfBoostDrag.perfLockRelease();
        }
    }

    public void onHorizontalFlingBoost(int duration) {
        int scrollTime = duration + 160 < 500 ? duration + 160 : 500;
        String currentPackage = this.mContext.getPackageName();
        if (isScrollBoostBlackListGame()) {
            VSlog.i(TAG, "isScrollBoostBlackListGame is true");
        } else if (SCROLL_BOOST_SS_ENABLE) {
            if (this.mPerfBoostFling == null) {
                this.mPerfBoostFling = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
                this.mIsPerfBoostFlingAcquired = false;
            }
            AbsVivoPerfManager absVivoPerfManager = this.mPerfBoostFling;
            if (absVivoPerfManager == null) {
                VSlog.e(TAG, "Error: boost object null");
                return;
            }
            absVivoPerfManager.perfHint(4224, currentPackage, scrollTime, 2);
            this.mIsPerfBoostFlingAcquired = true;
        }
    }

    public void onDownBoostRelease() {
        AbsVivoPerfManager absVivoPerfManager;
        if (SCROLL_BOOST_SS_ENABLE && (absVivoPerfManager = this.mPerfBoostFling) != null && this.mIsPerfBoostFlingAcquired) {
            absVivoPerfManager.perfLockRelease();
            this.mIsPerfBoostFlingAcquired = false;
        }
    }

    private boolean isNotPortrait() {
        int rot = this.mService.getDefaultDisplayRotation();
        boolean portrait = rot == 0 || rot == 2;
        return !portrait;
    }

    public boolean shouldHideStatusBarByVivoMultiWindow() {
        if (this.mService.isVivoMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_HIDE_STATUSBAR && this.mService.isInVivoMultiWindowConsiderVisibilityFocusedDisplay() && isNotPortrait()) {
            return true;
        }
        return false;
    }

    public boolean configHideStatusBar() {
        if (this.mService.isVivoMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_HIDE_STATUSBAR && this.mService.isInVivoMultiWindowConsiderVisibilityFocusedDisplay()) {
            return true;
        }
        return false;
    }

    public int vivoChangeVisIfNeeded(int fullscreenAppearance, int dockedAppearance) {
        if (!this.mPolicy.mDisplayContent.getDefaultTaskDisplayArea().isStackVisible(3) || !this.mService.isVivoMultiWindowSupport() || this.mPolicy.mDisplayContent.getRotation() != 0) {
            return fullscreenAppearance;
        }
        int fullscreenAppearanceT = fullscreenAppearance & (-9);
        return fullscreenAppearanceT | (dockedAppearance & 8);
    }

    public boolean ignoreSetNavBarOpaqueFlagInMultiWindow(boolean dockedStackVisible, boolean isDockedDividerResizing) {
        if (this.mService.isVivoMultiWindowSupport()) {
            if (dockedStackVisible || isDockedDividerResizing) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean maybeLayoutCutoutInSplit(WindowState w) {
        if (this.mService.mAtmService.isVivoVosMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_SAFE_LAND_CUTOUT && w != null && w.getAttrs() != null && this.mDisplayFrames != null) {
            WindowManager.LayoutParams attrs = w.getAttrs();
            int type = attrs.type;
            boolean isLandscape = this.mDisplayFrames.mRotation == 1 || this.mDisplayFrames.mRotation == 3;
            boolean isNeedSplitSafe = this.mService.isInVivoMultiWindowIgnoreVisibility() && isLandscape && type != 2011;
            if (isNeedSplitSafe) {
                String title = attrs.getTitle() != null ? attrs.getTitle().toString() : "null win";
                if (w.inSplitScreenPrimaryWindowingMode() && title.contains("com.vivo.settings.secret.PasswordActivityMultiWindowUD")) {
                    if (this.DEBUG_MULTIWIN) {
                        VSlog.d(TAG, title + " maybeLayoutCutoutInSplit");
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean getSplitStatusbarMode() {
        return this.mPolicy.getInsetsPolicy().getSplitStatusbarMode();
    }

    public boolean shouldLayoutSafeWhenSplit(int position, int type, int mode, WindowManager.LayoutParams attrs) {
        if (this.mService.mAtmService.isMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_SAFE_LAND_CUTOUT) {
            boolean isLandscape = this.mDisplayFrames.mRotation == 1 || this.mDisplayFrames.mRotation == 3;
            boolean isAppForceSetCutMode = 3 != mode;
            boolean isNeedSplitSafe = this.mService.isInVivoMultiWindowIgnoreVisibility() && isLandscape && type != 2011;
            boolean isSystemForceSetNoSplit = attrs.keepFullScreen == 2 && 1 == mode;
            boolean forceCutOutApp = false;
            if (isNeedSplitSafe && !isAppForceSetCutMode) {
                VivoMultiWindowConfig vivoMultiWindowConfig = VivoMultiWindowConfig.getInstance();
                forceCutOutApp = vivoMultiWindowConfig != null ? vivoMultiWindowConfig.isForceCutoutApp(attrs.packageName) : false;
            }
            if (this.DEBUG_MULTIWIN) {
                StringBuilder sb = new StringBuilder();
                sb.append("layoutWindowLw, multiwindow isNeedSplitSafe is ");
                sb.append(isNeedSplitSafe);
                sb.append(" forceCutOutApp is");
                sb.append(forceCutOutApp);
                sb.append(",isSystemForceSetNoSplit is ");
                sb.append(isSystemForceSetNoSplit);
                sb.append(" isAppForceSetCutMode is ");
                sb.append(isAppForceSetCutMode);
                sb.append(" internalFlag is ");
                sb.append(attrs.internalFlag);
                sb.append(" 0x");
                sb.append(Integer.toHexString(attrs.internalFlag));
                sb.append(" cutoutMode is ");
                sb.append(mode);
                sb.append(" ");
                sb.append(attrs.getTitle() != null ? attrs.getTitle().toString() : "null win");
                VSlog.i(TAG, sb.toString());
            }
            return this.mService.isVivoMultiWindowSupport() ? (isAppForceSetCutMode || forceCutOutApp) && isNeedSplitSafe : this.mService.mAtmService.isVivoVosMultiWindowSupport() && isNeedSplitSafe && isAppForceSetCutMode;
        }
        return false;
    }

    public boolean addWindowLw(WindowState win) {
        FixedRotationAnimationController controller;
        if (!this.mService.getIsVosProduct() ? "com.vivo.upslide".equals(win.getAttrs().packageName) : FaceUIState.PKG_SYSTEMUI.equals(win.getAttrs().packageName)) {
            if (win.getWindowTag().toString().contains("SideSlideGestureBar-Bottom")) {
                this.mBottomGestureBar = win;
            }
        }
        if (win.getAppToken() == null && (controller = this.mPolicy.mDisplayContent.getFixedRotationAnimationController()) != null) {
            controller.hideWindowToken(win, true);
        }
        if (win.getAttrs().type == 2999) {
            if (this.mRatioSwitch != null) {
                return false;
            }
            this.mRatioSwitch = win;
            VSlog.d(TAG, "DEBUG_RATIOSWITCH:prepareAddWindowLw mRatioSwitch=" + this.mRatioSwitch);
        }
        return true;
    }

    public WindowState getBottomGestureBar() {
        return this.mBottomGestureBar;
    }

    public void saveAppShareLastUiMode(DisplayFrames displayFrames, int mode) {
        if (!AppShareConfig.SUPPROT_APPSHARE || displayFrames == null || !MultiDisplayManager.isAppShareDisplayId(displayFrames.mDisplayId)) {
            return;
        }
        this.mLastAppShareUiMode = mode;
    }

    public void layoutWindowLwAppShared(WindowState win, WindowState attached, DisplayFrames displayFrames) {
        if (!AppShareConfig.SUPPROT_APPSHARE || win == null || displayFrames == null || !MultiDisplayManager.isAppShareDisplayId(displayFrames.mDisplayId)) {
            return;
        }
        int uiMode = this.mLastAppShareUiMode;
        WindowManager.LayoutParams attrs = win.getAttrs();
        if (attrs == null || !attrs.getTitle().toString().contains("com.tencent.tmgp.sgame")) {
            return;
        }
        int sysUiFl = PolicyControl.getSystemUiVisibility(win, attrs);
        boolean hideNavBar = (sysUiFl & 2) != 0;
        if (hideNavBar) {
            return;
        }
        int rotation = displayFrames.mRotation;
        int displayHeight = displayFrames.mDisplayHeight;
        int displayWidth = displayFrames.mDisplayWidth;
        Rect dockFrame = displayFrames.mDock;
        int mNavigationBarPosition = this.mPolicy.navigationBarPosition(displayWidth, displayHeight, rotation);
        Rect cutoutSafeUnrestricted = this.mTmpRect;
        cutoutSafeUnrestricted.set(displayFrames.mUnrestricted);
        cutoutSafeUnrestricted.intersectUnchecked(displayFrames.mDisplayCutoutSafe);
        if (mNavigationBarPosition == 4) {
            int top = cutoutSafeUnrestricted.bottom;
            Rect rect = displayFrames.mStable;
            displayFrames.mStableFullscreen.bottom = top;
            rect.bottom = top;
            displayFrames.mRestricted.bottom = top;
            dockFrame.bottom = top;
            displayFrames.mSystem.bottom = top;
        } else if (mNavigationBarPosition == 2) {
            int left = cutoutSafeUnrestricted.right - this.mPolicy.getNavigationBarWidth(rotation, uiMode);
            Rect rect2 = displayFrames.mStable;
            displayFrames.mStableFullscreen.right = left;
            rect2.right = left;
            displayFrames.mRestricted.right = left;
            dockFrame.right = left;
            displayFrames.mSystem.right = left;
        } else if (mNavigationBarPosition == 1) {
            int right = cutoutSafeUnrestricted.left + this.mPolicy.getNavigationBarWidth(rotation, uiMode);
            Rect rect3 = displayFrames.mStable;
            displayFrames.mStableFullscreen.left = right;
            rect3.left = right;
            displayFrames.mRestricted.left = right;
            dockFrame.left = right;
            displayFrames.mSystem.left = right;
        }
        displayFrames.mCurrent.set(dockFrame);
        displayFrames.mVoiceContent.set(dockFrame);
        displayFrames.mContent.set(dockFrame);
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            VSlog.d(TAG, "layoutWindowLwAppShared(" + ((Object) attrs.getTitle()) + ", mRestricted : " + displayFrames.mRestricted);
        }
    }

    public void layoutStatusBarInnetForAppShare(DisplayFrames displayFrames) {
        if (!AppShareConfig.SUPPROT_APPSHARE || displayFrames == null || !MultiDisplayManager.isAppShareDisplayId(displayFrames.mDisplayId)) {
            return;
        }
        displayFrames.mStable.top = displayFrames.mUnrestricted.top + this.mVivoStatusBarHeightForRotation[displayFrames.mRotation];
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            VSlog.v(TAG, "mStable=" + displayFrames.mStable);
        }
        Rect dockFrame = displayFrames.mDock;
        dockFrame.top = displayFrames.mStable.top;
        displayFrames.mContent.set(dockFrame);
        displayFrames.mVoiceContent.set(dockFrame);
        displayFrames.mCurrent.set(dockFrame);
    }

    public void onConfigurationChangedForAppShare(int portraintRotaion, int portraitRotationStatusBarHeight) {
        if (!AppShareConfig.SUPPROT_APPSHARE || !MultiDisplayManager.isAppShareDisplayId(this.mPolicy.getDisplayId())) {
            return;
        }
        int i = 0;
        while (true) {
            int[] iArr = this.mVivoStatusBarHeightForRotation;
            if (i < iArr.length) {
                iArr[i] = 0;
                i++;
            } else {
                iArr[portraintRotaion] = portraitRotationStatusBarHeight;
                return;
            }
        }
    }

    public int unlockEarlySetStatusBarColor(int fullscreenVisibility, int dockedVisibility) {
        SnapshotWindow snapshotWindow = this.mSnapshotWindow;
        if (snapshotWindow != null) {
            int fullscreenVisibilityT = fullscreenVisibility | snapshotWindow.unlockEarlySetStatusBarColor(this.mPolicy.isKeyguardShowing(), this.mPolicy.mFocusedWindow, fullscreenVisibility, dockedVisibility);
            return fullscreenVisibilityT;
        }
        return fullscreenVisibility;
    }

    public WindowState getDimmingWindowLw() {
        WindowState w;
        synchronized (this.mService.mGlobalLock) {
            w = this.mDimmingWindowState;
        }
        return w;
    }

    public void recordOtherFullscreenOpaqueWinLw(WindowState win) {
        this.mOtherFullOpaqueWindows.add(win);
    }

    public ArrayList<WindowState> getOtherFullscreenOpaqueWindowsLw() {
        return this.mOtherFullOpaqueWindows;
    }
}