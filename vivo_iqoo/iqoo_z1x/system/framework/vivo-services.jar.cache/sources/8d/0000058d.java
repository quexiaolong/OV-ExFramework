package com.android.server.wm;

import android.graphics.Rect;
import android.graphics.Region;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.MergedConfiguration;
import android.view.DisplayInfo;
import android.view.InputWindowHandle;
import com.android.internal.os.ByteTransferPipe;
import com.android.internal.policy.NavigationBarPolicy;
import com.android.server.policy.IVivoRatioControllerUtils;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.sdk.Consts;
import java.util.List;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoWindowStateImpl implements IVivoWindowState {
    private static final boolean DEBUG_SPLIT_ALWAYS_NOT_SHOW = SystemProperties.getBoolean("persist.vivo.split_alwaysnotshow_navbar", false);
    static final String TAG = "VivoWindowStateImpl";
    private static final String TAG_APPSHARED = "AppShare-WindowState";
    boolean mActualAppOpVisibility;
    boolean mForceHide;
    VivoAppShareManager mVivoAppShareManager;
    private VivoRatioControllerUtilsImpl mVivoRatioControllerUtils;
    private WindowState mWindowState;
    private WindowManagerService mWmService;
    public boolean mScaleEnabled = false;
    public float mScale = 1.0f;
    public boolean mNeedNote = true;
    private final Rect mTmpRect = new Rect();
    boolean mForceHideVisibility = true;
    boolean mHideByFingerPrint = false;
    private int mNavColor = 0;
    private boolean mShouldApplyNavColor = false;
    private boolean mIsLayoutToNav = true;
    int mNavSize = 0;
    int mGestureSize = 0;
    int mBottomBarSize = 0;
    int mAspectRatioResizeType = 0;
    boolean mBlankInNav = false;
    Rect mFrameForLetterbox = new Rect();
    boolean mHideByBiometric = false;

    public VivoWindowStateImpl(WindowState windowState) {
        this.mWmService = null;
        this.mVivoAppShareManager = null;
        if (windowState == null) {
            throw new IllegalArgumentException("windowState must be not null");
        }
        this.mWindowState = windowState;
        this.mWmService = windowState.mWmService;
        this.mVivoRatioControllerUtils = VivoRatioControllerUtilsImpl.getInstance();
        this.mVivoAppShareManager = VivoAppShareManager.getInstance();
    }

    public boolean getScaleEnabled() {
        return this.mScaleEnabled;
    }

    public void setScaleEnabled(boolean scaleEnabled) {
        this.mScaleEnabled = scaleEnabled;
    }

    public float getScale() {
        return this.mScale;
    }

    public void setScale(float scale) {
        this.mScale = scale;
    }

    public boolean getNeedNote() {
        return this.mNeedNote;
    }

    public void setNeedNote(boolean needNote) {
        this.mNeedNote = needNote;
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public boolean setForceHideVisibility(boolean visible) {
        if (this.mForceHideVisibility == visible) {
            return false;
        }
        this.mForceHideVisibility = visible;
        VSlog.i(TAG, "Force hiding visibility changed, visible = " + visible + ", win = " + this.mWindowState);
        return true;
    }

    public boolean isForceHideVisibility() {
        return this.mForceHideVisibility;
    }

    public void hideByFingerPrint(boolean hide) {
        VSlog.i(TAG, "hide by finger print, hide = " + hide + ", w = " + this.mWindowState);
        if (this.mHideByFingerPrint != hide) {
            this.mHideByFingerPrint = hide;
        }
    }

    public boolean isHideByFingerPrint() {
        return this.mHideByFingerPrint;
    }

    public boolean shouldAnimAffectVisible() {
        ActivityRecord ac;
        WindowContainer animatingContainer = this.mWindowState.getAnimatingContainer(3, -1);
        if (animatingContainer == null || !(animatingContainer instanceof TaskDisplayArea) || (ac = this.mWindowState.mActivityRecord) == null || ac.shouldInvolveInDisplayAreaAnim()) {
            return true;
        }
        VSlog.d(TAG, "No shouldAnimAffectVisible for " + this.mWindowState);
        return false;
    }

    public boolean isAspectRestricted() {
        return (this.mWindowState.inAppWindowThatMatchesParentBounds() || this.mWindowState.inMultiWindowMode()) ? false : true;
    }

    public void applyNavColor(int color) {
        this.mNavColor = color;
        this.mShouldApplyNavColor = true;
        if (this.mWindowState.mActivityRecord != null) {
            this.mWindowState.mActivityRecord.updateLetterboxColor();
        }
    }

    private boolean isHomeIndicatorOn() {
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys", "1");
        if (WindowManagerDebugConfig.DEBUG) {
            VSlog.d(TAG, "DEBUG_ALIENSCREEN:isHomeIndicatorOn=" + navBarOverride);
        }
        return !"0".equals(navBarOverride);
    }

    private void updateBottomBarSize() {
        if (isHomeIndicatorOn()) {
            this.mGestureSize = 0;
            this.mBottomBarSize = 0;
            return;
        }
        if (this.mNavSize == 0) {
            this.mNavSize = this.mWindowState.mContext.getResources().getDimensionPixelSize(17105334);
        }
        this.mBottomBarSize = this.mNavSize;
    }

    void getInsetSize(Rect inset) {
        int leftInset;
        int rightInset;
        updateBottomBarSize();
        int fringeCut = this.mVivoRatioControllerUtils.getAlienScreenCoverInsetTop();
        boolean isAspectRestricted = isAspectRestricted();
        int rotation = this.mWindowState.getDisplayInfo().rotation;
        DisplayContent displayContent = this.mWindowState.getDisplayContent();
        int dw = displayContent.getDisplayInfo().logicalWidth;
        int dh = displayContent.getDisplayInfo().logicalHeight;
        if (rotation == 1 || rotation == 3) {
            if (IVivoRatioControllerUtils.EAR_PHONE_SUPPORT || IVivoRatioControllerUtils.HOLE_SCREEN_SUPPORT) {
                if (this.mWindowState.mWmService.isVivoMultiWindowSupport() && this.mWindowState.mWmService.isInVivoMultiWindowIgnoreVisibilityFocusedDisplay() && this.mWindowState.inSplitScreenWindowingMode()) {
                    if (rotation == 1) {
                        leftInset = 1 != 0 ? fringeCut : this.mBottomBarSize;
                    } else {
                        leftInset = 1 != 0 ? this.mBottomBarSize : 1;
                    }
                    if (rotation == 1) {
                        rightInset = 1 != 0 ? this.mBottomBarSize : 1;
                    } else {
                        rightInset = 1 != 0 ? fringeCut : this.mBottomBarSize;
                    }
                    inset.left = leftInset;
                    inset.right = rightInset;
                    inset.bottom = 1;
                    inset.top = 1;
                } else if (shouldShowBackground()) {
                    int decorCropLeft = rotation == 1 ? fringeCut : this.mBottomBarSize;
                    int decorCropRight = rotation == 1 ? this.mBottomBarSize : fringeCut;
                    int appCropLeft = 0;
                    int appCropRight = 0;
                    if (this.mWindowState.mActivityRecord != null && !this.mWindowState.mActivityRecord.matchParentBounds()) {
                        this.mWindowState.mActivityRecord.getBounds(this.mTmpRect);
                        appCropLeft = this.mTmpRect.left;
                        appCropRight = dw - this.mTmpRect.right;
                    }
                    inset.left = Math.max(decorCropLeft, appCropLeft);
                    inset.right = Math.max(decorCropRight, appCropRight);
                } else {
                    inset.bottom = 1;
                    inset.right = 1;
                    inset.top = 1;
                    inset.left = 1;
                }
            } else {
                int i = this.mBottomBarSize;
                inset.right = i;
                inset.left = i;
            }
        } else {
            inset.right = 0;
            inset.left = 0;
            if (this.mWindowState.mAttrs.type == 1) {
                inset.top = this.mVivoRatioControllerUtils.getStatusBarHeight();
            }
            if (isAspectRestricted) {
                this.mWindowState.mActivityRecord.getBounds(this.mTmpRect);
                inset.bottom = dh - this.mTmpRect.bottom;
            } else {
                inset.bottom = this.mBottomBarSize;
            }
        }
        if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE) {
            VSlog.i(TAG, "getInsetSize, win = " + this.mWindowState + ", shouldShowBackground = " + shouldShowBackground() + ", inset = " + inset);
        }
    }

    private boolean shouldShowBackground() {
        Task task = this.mWindowState.getTask();
        if (task == null) {
            if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE) {
                VSlog.i(TAG, "getInsetSize, win = " + this.mWindowState + ", task == null");
            }
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE) {
            VSlog.i(TAG, "getInsetSize, mSystemUiVisibility = " + Integer.toHexString(this.mWindowState.mSystemUiVisibility) + ", type = " + this.mWindowState.mAttrs.type + ", fillsParent = " + this.mWindowState.mActivityRecord.fillsParent() + ", format = " + this.mWindowState.mAttrs.format);
        }
        return this.mWindowState.mAttrs.type == 1;
    }

    public int getWindowNavColor() {
        if (!this.mIsLayoutToNav) {
            return -16777216;
        }
        return this.mNavColor;
    }

    public int shouldApplyNavColor() {
        if ((this.mShouldApplyNavColor || (this.mWindowState.getAttrs().flags & 2) != 0) && this.mWindowState.isReadyForDisplay() && (this.mWindowState.getControllableInsetProvider() == null || this.mWindowState.getControllableInsetProvider().isClientVisible())) {
            getInsetSize(this.mTmpRect);
            Rect rect = this.mWindowState.getFrameLw();
            rect.height();
            rect.width();
            int navWidth = this.mBottomBarSize;
            DisplayInfo displayInfo = this.mWindowState.getDisplayInfo();
            int dw = displayInfo.logicalWidth;
            int dh = displayInfo.logicalHeight;
            int navPos = this.mWindowState.getDisplayContent().getDisplayPolicy().navigationBarPosition(dw, dh, displayInfo.rotation);
            this.mIsLayoutToNav = true;
            if (isAspectRestricted()) {
                if ((navPos == 4 && rect.bottom < dh - navWidth) || ((navPos == 2 && rect.right < dw - navWidth) || (navPos == 1 && rect.left > navWidth))) {
                    this.mIsLayoutToNav = false;
                }
                return NavigationBarPolicy.APPLY_COLOR_FULL;
            } else if (this.mWindowState.getRootTask() != null && this.mWindowState.getRootTask().inFreeformWindowingMode()) {
                return NavigationBarPolicy.APPLY_COLOR_NONE;
            } else {
                if (this.mWmService.isVivoMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_NAV_COLOR && this.mWmService.isInVivoMultiWindowIgnoreVisibilityFocusedDisplay() && this.mWindowState.inSplitScreenWindowingMode() && !isMatchDockNavPos(this.mWindowState)) {
                    return NavigationBarPolicy.APPLY_COLOR_NONE;
                }
                if (this.mWindowState.mActivityRecord == null) {
                    Rect winFrame = new Rect();
                    Rect crop = this.mWindowState.mWinAnimator.mLastClipRect;
                    if (crop.isEmpty()) {
                        winFrame.set(rect);
                    } else {
                        int i = rect.left + crop.left;
                        int i2 = rect.top + crop.top;
                        int i3 = rect.left;
                        int height = crop.right;
                        int i4 = i3 + height;
                        int i5 = rect.top;
                        int width = crop.bottom;
                        winFrame.set(i, i2, i4, i5 + width);
                    }
                    if (navPos == 4) {
                        if (winFrame.left <= 0 && winFrame.right >= dw && winFrame.top <= dh - navWidth && winFrame.bottom >= dh) {
                            return NavigationBarPolicy.APPLY_COLOR_FULL;
                        }
                    } else if (navPos == 2) {
                        if (winFrame.top <= 0 && winFrame.bottom >= dh && winFrame.left <= dw - navWidth && winFrame.right >= dw) {
                            return NavigationBarPolicy.APPLY_COLOR_FULL;
                        }
                    } else if (navPos == 1 && winFrame.top <= 0 && winFrame.bottom >= dh && winFrame.right >= navWidth && winFrame.left <= 0) {
                        return NavigationBarPolicy.APPLY_COLOR_FULL;
                    }
                    return NavigationBarPolicy.APPLY_COLOR_NOT_MATCH;
                }
                if (navPos == 4) {
                    int insetHeight = this.mTmpRect.bottom;
                    int dockBottomEdge = dh - insetHeight;
                    if (rect.left > 0 || rect.right < dw) {
                        if (IVivoRatioControllerUtils.CURVED_SCREEN_SUPPORT) {
                            rect.width();
                        }
                    } else if (dockBottomEdge - rect.top >= 126 && rect.bottom >= dockBottomEdge && (!this.mBlankInNav || rect.bottom > dh - navWidth)) {
                        return NavigationBarPolicy.APPLY_COLOR_FULL;
                    }
                } else if (navPos == 2) {
                    int dockRightEdge = dw - this.mTmpRect.right;
                    if (((rect.top <= 0 && rect.bottom >= dh) || (IVivoRatioControllerUtils.CURVED_SCREEN_SUPPORT && rect.height() < dh)) && dockRightEdge - rect.left >= 126 && rect.right >= dockRightEdge && (!this.mBlankInNav || rect.right > dw - navWidth)) {
                        return NavigationBarPolicy.APPLY_COLOR_FULL;
                    }
                } else if (navPos == 1) {
                    int insetWidth = this.mTmpRect.left;
                    if (((rect.top <= 0 && rect.bottom >= dh) || (IVivoRatioControllerUtils.CURVED_SCREEN_SUPPORT && rect.height() < dh)) && rect.right - insetWidth >= 126 && rect.left <= insetWidth && (!this.mBlankInNav || rect.left < navWidth)) {
                        return NavigationBarPolicy.APPLY_COLOR_FULL;
                    }
                }
                return NavigationBarPolicy.APPLY_COLOR_NOT_MATCH;
            }
        }
        return NavigationBarPolicy.APPLY_COLOR_NONE;
    }

    public int getAspectRatioResizeType() {
        return this.mAspectRatioResizeType;
    }

    public void setAspectRatioResizeType(int apr) {
        this.mAspectRatioResizeType = apr;
    }

    public void getAppBounds(Rect outBounds) {
        if (this.mWindowState.mActivityRecord != null) {
            this.mWindowState.mActivityRecord.getBounds(outBounds);
        } else {
            outBounds.setEmpty();
        }
    }

    private Rect getWinInset() {
        Rect inset = new Rect();
        inset.set(this.mWindowState.getVisibleInsets());
        if ((this.mWindowState.mSystemUiVisibility & 512) != 0 && (this.mWindowState.mSystemUiVisibility & Consts.ProcessStates.FOCUS) != 0) {
            inset.bottom = 0;
        }
        return inset;
    }

    Rect getWinFrame() {
        Rect winFrame = new Rect();
        Rect frame = this.mWindowState.getFrameLw();
        Rect crop = this.mWindowState.mWinAnimator.mLastClipRect;
        if (!crop.isEmpty()) {
            winFrame.set(frame.left + crop.left, frame.top + crop.top, frame.left + crop.right, frame.top + crop.bottom);
        } else {
            winFrame.set(frame);
        }
        if (!frame.contains(winFrame)) {
            Rect inset = getWinInset();
            winFrame.set(frame.left + inset.left, frame.top + inset.top, frame.right - inset.right, frame.bottom - inset.bottom);
        }
        if (this.mScaleEnabled) {
            winFrame.set(frame);
        }
        if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE) {
            VSlog.i(TAG, "getWinFrame, win = " + this.mWindowState + ", winFrame = " + winFrame + " mFrame = " + frame + " crop = " + crop);
        }
        return winFrame;
    }

    public Rect getFrameForLetterbox() {
        return this.mFrameForLetterbox;
    }

    public boolean shouldShowLetterbox(boolean isMainWin) {
        boolean shouldShow = false;
        DisplayInfo displayInfo = this.mWindowState.getDisplayInfo();
        int dw = displayInfo.logicalWidth;
        int dh = displayInfo.logicalHeight;
        this.mFrameForLetterbox.set(0, 0, dw, dh);
        Rect winFrame = getWinFrame();
        Rect insetSize = new Rect();
        getInsetSize(insetSize);
        if (this.mWindowState.mWmService.isVivoMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_NAV_COLOR && this.mWindowState.mWmService.isInVivoMultiWindowIgnoreVisibilityFocusedDisplay() && this.mWindowState.getDisplayId() == 0) {
            if (!isMainWin || this.mWindowState.mActivityRecord == null || this.mWindowState.mActivityRecord.getStack() == null) {
                return false;
            }
        } else if (this.mWindowState.mActivityRecord == null || this.mWindowState.isActivityTypeRecents() || this.mNavColor == 0) {
            return false;
        } else {
            if (this.mWmService.isVivoMultiWindowSupport() && this.mWindowState.isActivityTypeHome() && !this.mWindowState.inMultiWindowMode() && Settings.Global.getInt(this.mWmService.mContext.getContentResolver(), "current_desktop_type", 0) == 1) {
                if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE) {
                    VSlog.d(TAG, this.mWindowState + " shouldShowLetterbox reference asop with isLetterboxedAppWindow ");
                }
                return this.mWindowState.isLetterboxedAppWindow();
            }
            this.mBlankInNav = false;
            int navPos = this.mWindowState.getDisplayContent().getDisplayPolicy().navigationBarPosition(dw, dh, displayInfo.rotation);
            boolean curNavLetterboxNoNeed = false;
            int curSysFlag = this.mWindowState.getDisplayContent().getDisplayPolicy().mLastSystemUiFlags;
            if (((Integer.MIN_VALUE & curSysFlag) != 0 || (32768 & curSysFlag) != 0) && !"com.tencent.mm/com.tencent.mm.plugin.profile.ui.ContactInfoUI".equals(this.mWindowState.getWindowTag().toString())) {
                curNavLetterboxNoNeed = true;
                if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE) {
                    VSlog.d(TAG, this.mWindowState + " shouldShowLetterbox, curNavLetterboxNoNeed = true");
                }
            }
            boolean cutOutAndFill = (this.mWindowState.isLetterboxedForDisplayCutoutLw() && this.mWindowState.mActivityRecord.fillsParent()) || "com.mobile.legends/com.moba.unityplugin.MobaGameUnityActivity".equals(this.mWindowState.getWindowTag().toString());
            if (winFrame.width() == dw && winFrame.top < winFrame.bottom) {
                if (this.mWindowState.getFrameLw().top == insetSize.top && insetSize.top > 0 && (cutOutAndFill || navPos != 4)) {
                    shouldShow = true;
                    this.mFrameForLetterbox.top = winFrame.top;
                }
                if (dh - winFrame.bottom == insetSize.bottom && insetSize.bottom > 0) {
                    if (navPos == 4 && curNavLetterboxNoNeed) {
                        this.mBlankInNav = true;
                    } else {
                        shouldShow = true;
                        this.mFrameForLetterbox.bottom = winFrame.bottom;
                    }
                }
            } else if (winFrame.height() == dh && winFrame.left < winFrame.right) {
                if (dw - winFrame.right == insetSize.right && insetSize.right > 0) {
                    if (navPos == 2 && curNavLetterboxNoNeed) {
                        this.mBlankInNav = true;
                    } else if (cutOutAndFill || navPos != 1) {
                        shouldShow = true;
                        this.mFrameForLetterbox.right = winFrame.right;
                    }
                }
                if (winFrame.left == insetSize.left && insetSize.left > 0) {
                    if (navPos == 1 && curNavLetterboxNoNeed) {
                        this.mBlankInNav = true;
                    } else if (cutOutAndFill || navPos != 2) {
                        shouldShow = true;
                        this.mFrameForLetterbox.left = winFrame.left;
                    }
                }
            }
        }
        if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE) {
            VSlog.d(TAG, this.mWindowState + " shouldShowLetterbox = " + shouldShow);
        }
        return shouldShow;
    }

    public boolean shouldShowSplitNavBar(boolean isMainWin) {
        boolean shouldShow = false;
        if (!isMainWin || this.mWindowState.mActivityRecord == null || this.mWindowState.mActivityRecord.getStack() == null) {
            return false;
        }
        if (this.mWindowState.inSplitScreenSecondaryWindowingMode() && this.mWindowState.isActivityTypeRecents()) {
            if (VivoActivityRecordImpl.DEBUG_SPLIT_NAV_COLOR) {
                VSlog.d(TAG, "|-- shouldShowSplitNavBar skiped in third home for recents " + this.mWindowState);
            }
            return false;
        } else if (this.mWmService.isMinimizedDock()) {
            if (VivoActivityRecordImpl.DEBUG_SPLIT_NAV_COLOR) {
                VSlog.d(TAG, "|-- shouldShowSplitNavBar skiped in minimized of " + this.mWindowState);
            }
            return false;
        } else {
            if (this.mWindowState.inSplitScreenSecondaryWindowingMode()) {
                DisplayContent displayContent = this.mWindowState.getDisplayContent();
                int dw = displayContent.getDisplayInfo().logicalWidth;
                int dh = displayContent.getDisplayInfo().logicalHeight;
                if (VivoMultiWindowConfig.IS_VIVO_LAYOUT_INCLUDE_NAVBAR && VivoMultiWindowConfig.getInstance() != null && VivoMultiWindowConfig.getInstance().isLayoutIncludeNavApp(this.mWindowState.getOwningPackage()) && !this.mWindowState.mWmService.isVivoDockedDividerResizing()) {
                    return false;
                }
                if ((this.mWindowState.getOwningPackage() != null && !this.mWindowState.mWmService.getMiniNavColorState()) || DEBUG_SPLIT_ALWAYS_NOT_SHOW) {
                    if (VivoActivityRecordImpl.DEBUG_SPLIT_NAV_COLOR) {
                        VSlog.i(TAG, "vivo_multiwindow_fmk splitnotshownav minilauncher " + this.mWindowState.getOwningPackage());
                    }
                    return false;
                }
                boolean bHome = false;
                WindowState windowState = this.mWindowState;
                if (windowState != null && windowState.isActivityTypeHome()) {
                    bHome = !VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS.equals(this.mWindowState.mAttrs.packageName);
                }
                if (bHome) {
                    shouldShow = false;
                } else {
                    ActivityStack winStack = this.mWindowState.getRootTask();
                    int navBarPosition = this.mWindowState.mWmService.getNavBarPosition(0);
                    if (dw > dh) {
                        if (navBarPosition == 1 && 1 == winStack.getDockSide()) {
                            shouldShow = true;
                        } else if (navBarPosition == 2 && 3 == winStack.getDockSide()) {
                            shouldShow = true;
                        }
                    } else if (winStack.getDockSide() == 4) {
                        shouldShow = true;
                    }
                }
            }
            if (VivoActivityRecordImpl.DEBUG_SPLIT_NAV_COLOR) {
                VSlog.d(TAG, "|-- shouldShowSplitNavBar " + this.mWindowState + " shouldShowSplitNavBar = " + shouldShow);
            }
            return shouldShow;
        }
    }

    protected boolean isMatchDockNavPos(WindowState win) {
        if (win == null || win.getRootTask() == null) {
            return false;
        }
        int dockSide = win.getRootTask().getDockSide();
        DisplayInfo displayInfo = win.getDisplayInfo();
        return VivoMultiWindowConfig.isMatchDockNavPos(dockSide, displayInfo, this.mWindowState.getDisplayContent().getDisplayPolicy().getNavBarPosition(), win.getRootTask().getActivityType(), win.getRootTask().getWindowingMode());
    }

    public int getCurrentSessionPid() {
        return this.mWindowState.mSession.mPid;
    }

    public ActivityRecord getActivityRecord() {
        return this.mWindowState.mActivityRecord;
    }

    public InputWindowHandle getInputInfo() {
        return this.mWindowState.mInputWindowHandle;
    }

    public void appendWindowViewHierarchy(StringBuilder sb) {
        try {
            ByteTransferPipe pipe = new ByteTransferPipe();
            this.mWindowState.mClient.executeCommand("dump-view-hierarchy", (String) null, pipe.getWriteFd());
            String dump = new String(pipe.get());
            sb.append(dump);
            pipe.kill();
        } catch (Exception e) {
            sb.append(e.getMessage());
        }
    }

    public boolean promoteLayerOrderToImeInSplit(int type) {
        WindowState windowState;
        DisplayContent displayContent;
        boolean imeVisible;
        boolean z;
        if (type == 2005 && this.mWmService.mAtmService != null && this.mWmService.mAtmService.isMultiWindowSupport() && this.mWmService.isInVivoMultiWindowConsiderVisibilityFocusedDisplay() && (windowState = this.mWindowState) != null && windowState.mActivityRecord == null && (displayContent = this.mWindowState.getDisplayContent()) != null) {
            WindowState mInputMethodWindow = displayContent.mInputMethodWindow;
            if (mInputMethodWindow == null || !mInputMethodWindow.isVisibleLw() || !mInputMethodWindow.isDisplayedLw()) {
                imeVisible = false;
            } else {
                imeVisible = true;
            }
            if (VivoMultiWindowConfig.DEBUG && mInputMethodWindow != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("promoteLayerOrderToImeInSplit for w: ");
                sb.append(this.mWindowState);
                sb.append(",imeVisible is ");
                sb.append(imeVisible);
                sb.append(",promote is ");
                if (mInputMethodWindow.compareTo(this.mWindowState) < 0) {
                    z = false;
                } else {
                    z = true;
                }
                sb.append(z);
                sb.append(",mInputMethodWindow is ");
                sb.append(mInputMethodWindow);
                VSlog.d(TAG, sb.toString());
            }
            if (!imeVisible || mInputMethodWindow.compareTo(this.mWindowState) < 0) {
                return false;
            }
            return true;
        }
        return false;
    }

    public int changeTransitWhenMinimizeingFreeform(int winTransit, boolean visible) {
        if (this.mWindowState.mWmService.isVivoFreeFormValid() && this.mWindowState.mWmService.isInVivoFreeform() && this.mWindowState.mWmService.isFreeFormMin() && this.mWindowState.inFreeformWindowingMode() && !visible) {
            return 11;
        }
        return winTransit;
    }

    public boolean changeWindowsAreFloatingForVivoFreeform(Task task, boolean windowsAreFloating) {
        if (task == null || !task.inFreeformWindowingMode() || !this.mWindowState.mWmService.isVivoFreeFormStackMax()) {
            return windowsAreFloating;
        }
        return false;
    }

    public boolean inVivoFreeform() {
        return this.mWindowState.mWmService.isVivoFreeFormValid() && this.mWindowState.inFreeformWindowingMode();
    }

    public void adjustRegionForScaledFreeform(Region region) {
        if (inVivoFreeform() && this.mWmService.getFreeformScale() != 1.0f) {
            Rect rect = new Rect();
            region.getBounds(rect);
            if (rect.left < 0) {
                rect.right -= rect.left;
                rect.left = 0;
            }
            if (rect.top < 0) {
                rect.bottom -= rect.top;
                rect.top = 0;
            }
            if (this.mWindowState.getDisplayContent() != null) {
                Rect bounds = new Rect();
                bounds.set(this.mWindowState.getBounds());
                int width = bounds.width();
                int height = bounds.height();
                int baseDisplayWidth = this.mWindowState.getDisplayContent().mBaseDisplayWidth;
                if (width > 0 && height > 0 && baseDisplayWidth > 0) {
                    if (width > height) {
                        float ratioFreeformLandscape = (height * 1.0f) / width;
                        rect.right = (int) ((baseDisplayWidth / ratioFreeformLandscape) + 0.5f);
                        rect.bottom = baseDisplayWidth;
                    } else {
                        float ratioFreeformPortrait = (height * 1.0f) / width;
                        rect.right = baseDisplayWidth;
                        rect.bottom = (int) ((baseDisplayWidth * ratioFreeformPortrait) + 0.5f);
                    }
                }
                DisplayMetrics displayMetrics = this.mWindowState.getDisplayContent().getDisplayMetrics();
                int delta = WindowManagerService.dipToPixel(30, displayMetrics);
                rect.inset(-delta, -delta);
            }
            region.set(rect);
        }
    }

    public void adjustFramePositionForFreeform(Rect frame, Rect containingFrame, int xAdj, int yAdj, int gravity) {
        if (this.mWindowState.mGlobalScale == 1.0f) {
            return;
        }
        if ((268435456 & gravity) != 0 && this.mWindowState.mActivityRecord != null && "com.tencent.mobileqq".equals(this.mWindowState.mActivityRecord.packageName)) {
            if (frame.left > containingFrame.left) {
                int width = frame.width();
                frame.left = containingFrame.left + ((int) ((frame.left - containingFrame.left) * this.mWindowState.mGlobalScale));
                frame.right = frame.left + width;
            } else {
                int width2 = frame.width();
                frame.left = containingFrame.left + ((int) ((containingFrame.left - frame.left) * this.mWindowState.mGlobalScale));
                frame.right = frame.left + width2;
                if ((gravity & 96) == 32) {
                    frame.left = containingFrame.left + ((int) ((containingFrame.left + xAdj) * this.mWindowState.mGlobalScale));
                    frame.right = frame.left + width2;
                } else if ((gravity & 96) == 64) {
                    frame.left = containingFrame.left + ((int) ((containingFrame.left + xAdj) * this.mWindowState.mGlobalScale));
                    frame.right = frame.left + width2;
                }
            }
            int width3 = frame.top;
            if (width3 > containingFrame.top) {
                int height = frame.height();
                frame.top = containingFrame.top + ((int) ((frame.top - containingFrame.top) * this.mWindowState.mGlobalScale));
                frame.bottom = frame.top + height;
                if ((gravity & 96) == 32) {
                    frame.top = containingFrame.top + ((int) ((yAdj - containingFrame.top) * this.mWindowState.mGlobalScale));
                    frame.bottom = frame.top + height;
                    return;
                } else if ((gravity & 96) == 64) {
                    frame.top = containingFrame.top + ((int) (((containingFrame.bottom - yAdj) - height) * this.mWindowState.mGlobalScale));
                    frame.bottom = frame.top + height;
                    return;
                } else {
                    return;
                }
            }
            int height2 = frame.height();
            frame.top = containingFrame.top + ((int) ((containingFrame.top - frame.top) * this.mWindowState.mGlobalScale));
            frame.bottom = frame.top + height2;
            return;
        }
        if (frame.left > containingFrame.left) {
            int width4 = frame.width();
            frame.left = containingFrame.left + ((int) ((frame.left - containingFrame.left) * this.mWindowState.mGlobalScale));
            frame.right = frame.left + width4;
        }
        int width5 = frame.top;
        if (width5 > containingFrame.top) {
            int height3 = frame.height();
            frame.top = containingFrame.top + ((int) ((frame.top - containingFrame.top) * this.mWindowState.mGlobalScale));
            frame.bottom = frame.top + height3;
        }
    }

    public boolean ignoreCropToDecorOfSplit() {
        WindowState windowState;
        if (this.mWmService.mAtmService != null && this.mWmService.mAtmService.isVivoVosMultiWindowSupport() && this.mWmService.mAtmService.isMultiWindowExitedJustDefaultDisplay() && !this.mWmService.isVivoDockedDividerResizing() && (windowState = this.mWindowState) != null && windowState.mAttrs.type == 3 && this.mWindowState.isVisibleLw() && this.mWindowState.mActivityRecord != null && !this.mWindowState.mActivityRecord.supportsSplitScreenWindowingMode()) {
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.d(TAG, this.mWindowState + ",need ignoreCropToDecorOfSplit");
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean ignoreWallpaperTargetInSplit() {
        DisplayContent displayContent;
        if (this.mWmService.mAtmService != null && this.mWmService.mAtmService.isVivoMultiWindowSupport() && this.mWmService.isInVivoMultiWindowConsiderVisibilityFocusedDisplay() && this.mWindowState.mActivityRecord != null && this.mWindowState.getDisplayId() == 0 && this.mWindowState.inSplitScreenSecondaryWindowingMode() && this.mWindowState.isActivityTypeStandardOrUndefined() && (displayContent = this.mWindowState.getDisplayContent()) != null) {
            boolean pendingClosing = displayContent.mClosingApps.contains(this.mWindowState.mActivityRecord) && !this.mWindowState.isVisibleLw();
            boolean isShowWallpaperTarget = (this.mWindowState.mAttrs.flags & 1048576) != 0;
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.d(TAG, "ignoreWallpaperTargetInSplit for w: " + this.mWindowState + ",isShowWallpaperTarget is " + isShowWallpaperTarget + ",pendingClosing is " + pendingClosing + ",isVisibleLw is " + this.mWindowState.isVisibleLw());
            }
            return (!pendingClosing || isShowWallpaperTarget || this.mWmService.isVivoDockedDividerResizing()) ? false : true;
        }
        return false;
    }

    public boolean getForceHide() {
        return this.mForceHide;
    }

    public boolean updateActualOppVisibility(boolean appOpVisibility) {
        VSlog.i(TAG, this.mWindowState + " updateActualOppVisibility = " + appOpVisibility);
        this.mActualAppOpVisibility = appOpVisibility;
        return false;
    }

    public void setForceHide(boolean forceHide) {
        if (this.mForceHide != forceHide) {
            this.mForceHide = forceHide;
            VSlog.i(TAG, this.mWindowState + " set force hide = " + forceHide);
            if (this.mForceHide) {
                this.mActualAppOpVisibility = this.mWindowState.mAppOpVisibility;
                this.mWindowState.hideLw(false);
                this.mWindowState.mAppOpVisibility = false;
                return;
            }
            this.mWindowState.mAppOpVisibility = this.mActualAppOpVisibility;
            if (this.mActualAppOpVisibility) {
                this.mWindowState.showLw(false);
            }
        }
    }

    public void dispatchWallpaperClientVisibility(boolean visible) {
        VSlog.v(TAG, "dispatchWallpaperClientVisibility " + visible + " orig = " + this.mWindowState.mWallpaperVisible);
        if (!this.mWindowState.mWallpaperVisible) {
            return;
        }
        try {
            this.mWindowState.mClient.dispatchAppVisibility(visible);
        } catch (RemoteException e) {
        }
    }

    public void addWaitingForDrawn(List<WindowState> outWaitingForDrawn) {
        if (this.mWindowState.mActivityRecord == null || this.mWindowState.mWmService == null) {
            return;
        }
        try {
            if (this.mWindowState.mWmService.isGoogleUnlock() && this.mWindowState.mActivityRecord.isTopRunningActivity() && this.mWindowState.mAttrs != null && this.mWindowState.mAttrs.type == 1) {
                VSlog.v(TAG, "addWaitingForDrawn. win = " + this.mWindowState);
                this.mWindowState.mWinAnimator.mDrawState = 1;
                this.mWindowState.resetLastContentInsets();
                outWaitingForDrawn.add(this.mWindowState);
            }
        } catch (Exception e) {
            VSlog.w(TAG, "Exception: ", e);
        }
    }

    public void hideByBiometric(boolean hide) {
        if (this.mHideByBiometric != hide) {
            VSlog.i(TAG, "hide by fingerprint or face, hide = " + hide + ", w = " + this.mWindowState);
            this.mHideByBiometric = hide;
        }
    }

    public boolean isHideByBiometric() {
        return this.mHideByBiometric;
    }

    public void notifyWallpaperClientUnlockPhase(String commandTag, String unlockPhase) {
        try {
            this.mWindowState.mClient.executeCommand(commandTag, unlockPhase, (ParcelFileDescriptor) null);
        } catch (RemoteException e) {
        }
    }

    public void reportImeUnbind() {
        VSlog.d(TAG_APPSHARED, "reportImeUnbind win : " + this.mWindowState);
        try {
            this.mWindowState.mClient.reportImeUnbind();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reportImeHide(int displayId) {
        VSlog.d(TAG_APPSHARED, "reportImeHide displayId : " + displayId + ", win : " + this.mWindowState);
        try {
            this.mWindowState.mClient.reportImeHide(displayId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reportImeAllStatus(int[] status) {
        VSlog.d(TAG_APPSHARED, "reportImeAllStatus to window : " + this.mWindowState);
        try {
            this.mWindowState.mClient.reportImeAllStatus(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reportMoveToDisplayCompleted(int displayId) {
        VSlog.d(TAG_APPSHARED, "reportMoveToDisplayCompleted displayId : " + displayId + ", window : " + this.mWindowState);
        try {
            this.mWindowState.mClient.reportMoveToDisplayCompleted(displayId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reportSpecialDialogRemoveOk() {
        VSlog.d(TAG_APPSHARED, "reportSpecialDialogRemoveOk to window : " + this.mWindowState);
        try {
            this.mWindowState.mClient.reportSpecialDialogRemoveOk();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean checkAlertOpsWhenAddInAppSharedMode(int op, String packageName) {
        return this.mVivoAppShareManager.checkAlertOpsWhenAddInAppSharedMode(this.mWindowState, op, packageName);
    }

    public void updateMergedConfigurationForAppShared(MergedConfiguration mergedConfiguration) {
        this.mVivoAppShareManager.updateMergedConfigurationForAppShared(this.mWindowState, mergedConfiguration);
    }

    public void commitFinishDrawing() {
        RmsInjectorImpl.getInstance().commitFinishDrawing(this.mWindowState.mSession.mPid, this.mWindowState.mWinAnimator.mDrawState, this.mWindowState);
    }
}