package com.android.server.wm;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.Debug;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import com.android.server.wm.DisplayArea;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoTaskDisplayAreaWrapper {
    private static int DEFAULT_DISPLAY = 0;
    private DisplayContent mDisplayContent;
    private DisplayArea.Root mRoot;
    private WindowManagerService mService;
    private SurfaceControl.Transaction mTransaction;
    private String TAG = "VivoTaskDisplayAreaWrapper";
    private TaskDisplayArea mContainers = null;
    SurfaceControl mSplitScreenNavBar = null;
    Rect mLastNavBarRect = new Rect();
    private final Object mSplitLock = new Object();
    boolean mIsSplitScreenNavBarShow = false;
    int mSplitScreenNavBarColor = 0;

    public VivoTaskDisplayAreaWrapper(DisplayArea.Root root, WindowManagerService service, DisplayContent displaycontent) {
        this.mRoot = null;
        this.mService = null;
        this.mDisplayContent = null;
        this.mRoot = root;
        this.mService = service;
        this.mDisplayContent = displaycontent;
    }

    public void updateContainer() {
        DisplayContent displayContent = this.mDisplayContent;
        this.mContainers = displayContent != null ? displayContent.getDefaultTaskDisplayArea() : null;
        DisplayContent displayContent2 = this.mDisplayContent;
        this.mTransaction = displayContent2 != null ? displayContent2.getPendingTransaction() : null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void createSplitScreenNavBar() {
        if (this.mContainers == null) {
            return;
        }
        Rect navBarRect = getNavBarRect();
        if (navBarRect != null) {
            DisplayContent displayContent = this.mDisplayContent;
            this.mSplitScreenNavBar = displayContent.makeSurface(displayContent.getSession()).setCallsite("VivoTaskDisplayAreaWrapper.createSplitScreenNavBar").setName("splitScreenNavBar").setColorLayer().setBufferSize(navBarRect.width(), navBarRect.height()).build();
            String str = this.TAG;
            VSlog.i(str, "createSplitScreenNavBar " + navBarRect);
        } else {
            DisplayContent displayContent2 = this.mDisplayContent;
            this.mSplitScreenNavBar = displayContent2.makeSurface(displayContent2.getSession()).setCallsite("VivoTaskDisplayAreaWrapper.createSplitScreenNavBar").setName("splitScreenNavBar").setColorLayer().build();
            String str2 = this.TAG;
            VSlog.i(str2, "createSplitScreenNavBar null " + navBarRect);
        }
        this.mIsSplitScreenNavBarShow = false;
        if (VivoDisplayContentImpl.DEBUG_SPLIT_DISP || VivoMultiWindowConfig.DEBUG) {
            VSlog.i(this.TAG, "mSplitScreenNavBar creaeted");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeSplitScreenNavBar() {
        if (this.mSplitScreenNavBar != null) {
            ((SurfaceControl.Transaction) this.mService.mTransactionFactory.get()).remove(this.mSplitScreenNavBar);
            this.mSplitScreenNavBar = null;
            VSlog.i(this.TAG, "vivo_multiwindow_navcolor removeSplitScreenNavBar");
        }
        this.mIsSplitScreenNavBarShow = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl getSplitScreenNavBar() {
        return this.mSplitScreenNavBar;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSplitScreenNavBarShow() {
        return this.mSplitScreenNavBar != null && this.mIsSplitScreenNavBarShow;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showSplitScreenNavBar() {
        if (this.mContainers == null || this.mService == null || this.mDisplayContent == null) {
            VSlog.e(this.TAG, "showSplitScreenNavBar bug everything is null");
            return;
        }
        Rect navBarRect = getNavBarRect();
        if (navBarRect == null) {
            VSlog.w(this.TAG, "showSplitScreenNavBar null navrect");
        } else if (navBarRect.width() < 0 || navBarRect.height() < 0 || navBarRect.left < 0 || navBarRect.top < 0) {
            String str = this.TAG;
            VSlog.w(str, "showSplitScreenNavBar zero or negative navrect " + navBarRect);
        } else {
            if (navBarRect.isEmpty()) {
                String str2 = this.TAG;
                VSlog.w(str2, "showSplitScreenNavBar zero or negative navrect " + navBarRect);
            }
            int layerfromType = -1;
            boolean isDockResizing = this.mService.isVivoDockedDividerResizing();
            synchronized (this.mService.mGlobalLock) {
                if (isDockResizing) {
                    if (this.mIsSplitScreenNavBarShow && this.mLastNavBarRect != null && navBarRect.equals(this.mLastNavBarRect)) {
                        return;
                    }
                }
                if (this.mSplitScreenNavBar == null) {
                    SurfaceControl build = this.mDisplayContent.makeSurface(this.mDisplayContent.getSession()).setCallsite("VivoTaskDisplayAreaWrapper.showSplitScreenNavBar").setName("splitScreenNavBar").setColorLayer().build();
                    this.mSplitScreenNavBar = build;
                    this.mTransaction.setLayer(build, -1);
                    this.mTransaction.setColor(this.mSplitScreenNavBar, new float[]{0.0f, 0.0f, 0.0f});
                    this.mTransaction.setColorSpaceAgnostic(this.mSplitScreenNavBar, true);
                    if (VivoDisplayContentImpl.DEBUG_SPLIT_DISP || VivoMultiWindowConfig.DEBUG) {
                        String str3 = this.TAG;
                        VSlog.i(str3, "vivo_multiwindow_navcolor showSplitScreenNavBar force make " + Debug.getCallers(3));
                    }
                }
                if (this.mSplitScreenNavBar != null) {
                    DisplayArea.Tokens belowTokens = getBelowWindowsContainers();
                    if (this.mService.isKeyguardLocked() && belowTokens != null && belowTokens.getSurfaceControl() != null) {
                        this.mTransaction.setRelativeLayer(this.mSplitScreenNavBar, belowTokens.getSurfaceControl(), -1);
                        layerfromType = 0;
                    } else if (this.mContainers.getSplitScreenDividerAnchor() != null) {
                        this.mTransaction.setRelativeLayer(this.mSplitScreenNavBar, this.mContainers.getSplitScreenDividerAnchor(), 3);
                        layerfromType = 1;
                    } else if (this.mContainers.getTopStack() != null && this.mContainers.getTopStack().getSurfaceControl() != null) {
                        this.mTransaction.setRelativeLayer(this.mSplitScreenNavBar, this.mContainers.getTopStack().getSurfaceControl(), 1);
                        layerfromType = 2;
                    } else {
                        String str4 = this.TAG;
                        VSlog.e(str4, "vivo_multiwindow_navcolor showSplitScreenNavBar error " + Debug.getCallers(3));
                    }
                    if (this.mLastNavBarRect == null || !navBarRect.equals(this.mLastNavBarRect)) {
                        this.mTransaction.setPosition(this.mSplitScreenNavBar, navBarRect.left, navBarRect.top);
                        this.mTransaction.setBufferSize(this.mSplitScreenNavBar, navBarRect.width(), navBarRect.height());
                        this.mTransaction.setWindowCrop(this.mSplitScreenNavBar, navBarRect.width(), navBarRect.height());
                        if (this.mLastNavBarRect == null) {
                            this.mLastNavBarRect = new Rect();
                        }
                        this.mLastNavBarRect.set(navBarRect);
                    }
                    this.mTransaction.show(this.mSplitScreenNavBar);
                    this.mTransaction.apply();
                    this.mIsSplitScreenNavBarShow = true;
                }
                if (VivoDisplayContentImpl.DEBUG_SPLIT_DISP) {
                    String str5 = this.TAG;
                    VSlog.i(str5, "vivo_multiwindow_navcolor showSplitScreenNavBar show layerfromAnchor is " + layerfromType + " " + this.mService.isKeyguardLocked() + " " + this.mSplitScreenNavBar.toString() + " " + Debug.getCallers(7));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hideSplitScreenNavBar() {
        if (this.mContainers == null) {
            VSlog.e(this.TAG, "hideSplitScreenNavBar bug everything is null");
            return;
        }
        synchronized (this.mService.mGlobalLock) {
            if (this.mSplitScreenNavBar != null && this.mIsSplitScreenNavBarShow) {
                this.mTransaction.hide(this.mSplitScreenNavBar);
                this.mTransaction.apply();
                this.mIsSplitScreenNavBarShow = false;
            } else if (VivoDisplayContentImpl.DEBUG_SPLIT_DISP) {
                String str = this.TAG;
                VSlog.i(str, "hideSplitScreenNavBar hide not done " + Debug.getCallers(3));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSplitScreenNavBarColor(int navColor) {
        DisplayContent displayContent = this.mDisplayContent;
        boolean isInputMethodVisible = (displayContent == null || displayContent.mInputMethodWindow == null || !this.mDisplayContent.mInputMethodWindow.isVisibleLw()) ? false : true;
        if (isInputMethodVisible && isSplitScreenNavBarShow()) {
            VSlog.i(this.TAG, "!!==>InputMethod is visible and SplitScreenNavBar shows ,so set navColor WHITE");
            navColor = -1;
        }
        if (navColor != this.mSplitScreenNavBarColor && isSplitScreenNavBarShow()) {
            this.mSplitScreenNavBarColor = navColor;
            this.mTransaction.setColor(this.mSplitScreenNavBar, new float[]{Color.red(navColor) / 255.0f, Color.green(navColor) / 255.0f, Color.blue(navColor) / 255.0f});
            this.mTransaction.apply();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSplitScreenNavBarColor() {
        if (isSplitScreenNavBarShow()) {
            return this.mSplitScreenNavBarColor;
        }
        return 0;
    }

    private DisplayArea.Tokens getBelowWindowsContainers() {
        DisplayArea.Root root = this.mRoot;
        if (root != null && this.mContainers != null && root.mChildren != null && this.mRoot.mChildren.contains(this.mContainers)) {
            int index = this.mRoot.mChildren.indexOf(this.mContainers);
            if (index - 1 > -1) {
                for (int i = index - 1; i >= 0; i--) {
                    DisplayArea.Tokens childAt = this.mRoot.getChildAt(i);
                    if (childAt != null && (childAt instanceof DisplayArea.Tokens) && childAt.getChildCount() > 0) {
                        return childAt;
                    }
                }
            }
        }
        VSlog.e(this.TAG, "getBelowWindowsContainers is null");
        return null;
    }

    private int getNavBarPositionInDefaultDisplay() {
        DisplayContent displayContent = this.mDisplayContent;
        if (displayContent != null) {
            return displayContent.getDisplayPolicy().getNavBarPosition();
        }
        return -1;
    }

    Rect getNavBarRect() {
        WindowManagerService windowManagerService = this.mService;
        if (windowManagerService == null || this.mDisplayContent == null) {
            VSlog.e(this.TAG, "showSplitScreenNavBar bug everything is null");
            return null;
        }
        int navBarSize = windowManagerService.mContext.getResources().getDimensionPixelSize(17105334);
        int navBarPosDefault = getNavBarPositionInDefaultDisplay();
        Rect rect = new Rect(0, 0, 0, 0);
        DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
        if (displayInfo.logicalWidth > displayInfo.logicalHeight) {
            if (1 == navBarPosDefault) {
                rect = new Rect(0, 0, navBarSize, displayInfo.logicalHeight);
            } else if (2 == navBarPosDefault) {
                rect = new Rect(displayInfo.logicalWidth - navBarSize, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
            }
        } else if (4 == navBarPosDefault) {
            rect = new Rect(0, displayInfo.logicalHeight - navBarSize, displayInfo.logicalWidth, displayInfo.logicalHeight);
        } else {
            VSlog.e(this.TAG, "getNavBarPosPoint, Get Point Wrong");
        }
        if (VivoDisplayContentImpl.DEBUG_SPLIT_DISP) {
            String str = this.TAG;
            VSlog.i(str, "split nav rect is " + rect);
        }
        return rect;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInDisplay(ActivityStack stack) {
        TaskDisplayArea taskDisplayArea = this.mContainers;
        if (taskDisplayArea != null && taskDisplayArea.mChildren != null) {
            return this.mContainers.mChildren.contains(stack);
        }
        return false;
    }
}