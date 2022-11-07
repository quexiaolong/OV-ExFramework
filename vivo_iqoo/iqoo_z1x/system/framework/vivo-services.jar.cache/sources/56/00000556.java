package com.android.server.wm;

import android.graphics.Rect;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoFreeformGesturesPointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
    private static final String TAG = "VivoFreeformGesturesPointerEventListener";
    private float mCurrentX;
    private float mCurrentY;
    private int mFullscreenModeWinMaxHeight;
    private int mFullscreenModeWinMaxWidth;
    private int mFullscreenModeWinMinHeight;
    private int mFullscreenModeWinMinWidth;
    private int mLandscapeNormalWinMaxHeight;
    private int mLandscapeNormalWinMaxWidth;
    private int mLandscapeNormalWinMinHeight;
    private int mLandscapeNormalWinMinWidth;
    private float mLastX;
    private float mLastY;
    private int mPortraitNormalWinMaxHeight;
    private int mPortraitNormalWinMaxWidth;
    private int mPortraitNormalWinMinHeight;
    private int mPortraitNormalWinMinWidth;
    private int mResizedBottom;
    private int mResizedLeft;
    private int mResizedRight;
    private int mResizedTop;
    private int mRotation;
    private float mWidthHeightRatio;
    private WindowManagerService mWmService;
    private final List<WindowState> mFreeFormWindows = Collections.synchronizedList(new ArrayList());
    private Rect freeformRect = new Rect();
    private Rect finalRect = new Rect();
    private final SurfaceControl.Transaction mGestureTransaction = new SurfaceControl.Transaction();
    private final HashMap<ActivityRecord, SurfaceControl> mLeashMap = new HashMap<>();
    private boolean inResizeMode = false;
    private boolean exitResizeModeToResize = false;
    private Direction mResizeDirection = null;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public enum Direction {
        LEFT,
        BOTTOM,
        RIGHT
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public VivoFreeformGesturesPointerEventListener(WindowManagerService service) {
        this.mWmService = service;
        this.mLandscapeNormalWinMaxWidth = service.mContext.getResources().getDimensionPixelSize(51118200);
        this.mLandscapeNormalWinMaxHeight = this.mWmService.mContext.getResources().getDimensionPixelSize(51118199);
        this.mLandscapeNormalWinMinWidth = this.mWmService.mContext.getResources().getDimensionPixelSize(51118202);
        this.mLandscapeNormalWinMinHeight = this.mWmService.mContext.getResources().getDimensionPixelSize(51118201);
        this.mPortraitNormalWinMaxWidth = this.mWmService.mContext.getResources().getDimensionPixelSize(51118204);
        this.mPortraitNormalWinMaxHeight = this.mWmService.mContext.getResources().getDimensionPixelSize(51118203);
        this.mPortraitNormalWinMinWidth = this.mWmService.mContext.getResources().getDimensionPixelSize(51118206);
        this.mPortraitNormalWinMinHeight = this.mWmService.mContext.getResources().getDimensionPixelSize(51118205);
        this.mFullscreenModeWinMaxWidth = this.mWmService.mContext.getResources().getDimensionPixelSize(51118196);
        this.mFullscreenModeWinMaxHeight = this.mWmService.mContext.getResources().getDimensionPixelSize(51118195);
        this.mFullscreenModeWinMinWidth = this.mWmService.mContext.getResources().getDimensionPixelSize(51118198);
        this.mFullscreenModeWinMinHeight = this.mWmService.mContext.getResources().getDimensionPixelSize(51118197);
    }

    private boolean detectResizeMode(Rect rect, MotionEvent motionEvent) {
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        this.mWmService.scaleFreeformBack(rect);
        VSlog.d(TAG, "crurrent freeform rect:" + rect + " touch x:" + x + " touch y:" + y + " mLandscapeNormalWinMaxWidth:" + this.mLandscapeNormalWinMaxWidth);
        Rect leftTouchRect = new Rect();
        Rect rightTouchRect = new Rect();
        Rect bottomTouchRect = new Rect();
        leftTouchRect.set(rect.left - 20, rect.top - (20 * 2), rect.left + (20 * 2), rect.bottom + (20 * 2));
        rightTouchRect.set(rect.right - (20 * 2), rect.top - (20 * 2), rect.right + 20, rect.bottom + (20 * 2));
        bottomTouchRect.set(rect.left, rect.bottom - (20 * 2), rect.right, rect.bottom + 20);
        if (leftTouchRect.contains(x, y)) {
            VSlog.d(TAG, "leftTouchRect contains");
            setFreeformResizeBorder(Direction.LEFT);
            return true;
        } else if (rightTouchRect.contains(x, y)) {
            VSlog.d(TAG, "rightTouchRect contains");
            setFreeformResizeBorder(Direction.RIGHT);
            return true;
        } else if (bottomTouchRect.contains(x, y)) {
            VSlog.d(TAG, "bottomTouchRect contains");
            setFreeformResizeBorder(Direction.BOTTOM);
            return true;
        } else if (this.inResizeMode) {
            clearFreeformResizeBorder();
            return false;
        } else {
            return false;
        }
    }

    private void setFreeformResizeBorder(Direction resizeDirection) {
        try {
            Settings.System.putIntForUser(this.mWmService.mContext.getContentResolver(), "in_drag_resize", 1, this.mWmService.mCurrentUserId);
            Settings.System.putIntForUser(this.mWmService.mContext.getContentResolver(), "drag_resize_limited", 0, this.mWmService.mCurrentUserId);
            VSlog.d(TAG, "draw resize border");
        } catch (Exception e) {
            VSlog.e(TAG, "setFreeformResizeBorder e:" + e.getMessage());
        }
        this.inResizeMode = true;
        this.mResizeDirection = resizeDirection;
    }

    private void clearFreeformResizeBorder() {
        try {
            Settings.System.putIntForUser(this.mWmService.mContext.getContentResolver(), "in_drag_resize", 0, this.mWmService.mCurrentUserId);
            Settings.System.putIntForUser(this.mWmService.mContext.getContentResolver(), "drag_resize_limited", 0, this.mWmService.mCurrentUserId);
            VSlog.d(TAG, "clear all resize border");
        } catch (Exception e) {
            VSlog.e(TAG, "clearFreeformResizeBorder e:" + e.getMessage());
        }
        this.inResizeMode = false;
        this.mResizeDirection = null;
        clearLeash();
    }

    private void setFreeformResizeLimitedBorder() {
        try {
            Settings.System.putIntForUser(this.mWmService.mContext.getContentResolver(), "drag_resize_limited", 1, this.mWmService.mCurrentUserId);
            Settings.System.putIntForUser(this.mWmService.mContext.getContentResolver(), "in_drag_resize", 0, this.mWmService.mCurrentUserId);
            VSlog.d(TAG, "draw limited border");
        } catch (Exception e) {
            VSlog.e(TAG, "setFreeformResizeLimitedBorder e:" + e.getMessage());
        }
    }

    private void updateFreeformWindowInfo(MotionEvent motionEvent) {
        if (this.mWmService.isVivoFreeFormValid()) {
            this.mFreeFormWindows.clear();
            this.freeformRect.setEmpty();
            ActivityStack freeformStack = this.mWmService.mRoot.getVivoFreeformStack();
            WindowState freeformMainWindow = freeformStack != null ? freeformStack.getTopVisibleAppMainWindow() : null;
            if (freeformMainWindow != null && freeformMainWindow.isVisibleNow() && freeformMainWindow.mActivityRecord != null && freeformMainWindow.getBounds().width() != 0 && freeformMainWindow.getBounds().height() != 0 && detectResizeMode(freeformMainWindow.getBounds(), motionEvent)) {
                this.mFreeFormWindows.add(freeformMainWindow);
                this.freeformRect.set(freeformMainWindow.getBounds());
                this.mWmService.scaleFreeformBack(this.freeformRect);
                this.mResizedLeft = this.freeformRect.left;
                this.mResizedTop = this.freeformRect.top;
                this.mResizedRight = this.freeformRect.right;
                this.mResizedBottom = this.freeformRect.bottom;
                this.mWidthHeightRatio = (this.freeformRect.width() * 1.0f) / this.freeformRect.height();
                if (freeformMainWindow.getDisplayContent() != null) {
                    this.mRotation = freeformMainWindow.getDisplayContent().getRotation();
                }
            }
        }
    }

    private void createLeash() {
        if (this.mWmService.isVivoFreeFormValid()) {
            clearLeash();
            if (!this.mFreeFormWindows.isEmpty()) {
                for (int i = this.mFreeFormWindows.size() - 1; i >= 0; i--) {
                    WindowState windowState = this.mFreeFormWindows.get(i);
                    synchronized (this) {
                        SurfaceControl leash = createLeashLocked(windowState.mActivityRecord, windowState.mActivityRecord.getSurfaceControl());
                        if (leash != null) {
                            leash.setLayer(windowState.mActivityRecord.getPrefixOrderIndex());
                            this.mLeashMap.put(windowState.mActivityRecord, leash);
                        }
                    }
                }
            }
        }
    }

    private SurfaceControl createLeashLocked(ActivityRecord windowToken, SurfaceControl child) {
        if (windowToken == null || child == null) {
            return null;
        }
        try {
            if (windowToken.hasCommittedReparentToAnimationLeash()) {
                windowToken.cancelAnimation();
            }
            if (windowToken.isAnimating()) {
                windowToken.cancelAnimation();
            }
            int width = windowToken.getSurfaceWidth();
            int height = windowToken.getSurfaceHeight();
            SurfaceControl.Builder parent = windowToken.makeAnimationLeash().setParent(windowToken.getAnimationLeashParent());
            SurfaceControl leash = parent.setName(child + "-freeform-gesture-leash").setBufferSize(width, height).build();
            this.mGestureTransaction.show(leash);
            this.mGestureTransaction.reparent(child, leash);
            return leash;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void clearLeash() {
        VSlog.d(TAG, "clearLeash");
        synchronized (this) {
            for (Map.Entry<ActivityRecord, SurfaceControl> entry : this.mLeashMap.entrySet()) {
                ActivityRecord aToken = entry.getKey();
                SurfaceControl leash = entry.getValue();
                if (aToken != null && leash != null) {
                    SurfaceControl surface = aToken.getSurfaceControl();
                    SurfaceControl parent = aToken.getParentSurfaceControl();
                    if (parent != null && surface != null && surface.isValid() && parent.isValid()) {
                        this.mGestureTransaction.reparent(surface, parent);
                    }
                    this.mGestureTransaction.remove(leash);
                }
            }
            if (this.exitResizeModeToResize) {
                this.exitResizeModeToResize = false;
                if (this.mWmService.getDefaultDisplayContentLocked() != null) {
                    this.mWmService.getDefaultDisplayContentLocked().getPendingTransaction().merge(this.mGestureTransaction);
                }
            } else {
                applyTransaction();
            }
            this.mLeashMap.clear();
        }
    }

    private void applyTransaction() {
        synchronized (this) {
            this.mGestureTransaction.apply();
        }
    }

    private boolean isUserAMonkey() {
        try {
            boolean isMonkey = this.mWmService.mActivityManager.isUserAMonkey();
            return isMonkey;
        } catch (RemoteException e) {
            VSlog.e(TAG, "isUserAMonkey e : " + e.getMessage());
            return false;
        }
    }

    public void onPointerEvent(MotionEvent motionEvent) {
        if (isUserAMonkey()) {
            return;
        }
        VSlog.d(TAG, "onPointerEvent ev:" + motionEvent);
        int action = motionEvent.getAction() & 255;
        if (action == 0) {
            updateFreeformWindowInfo(motionEvent);
            if (this.inResizeMode) {
                createLeash();
                onActionDown(motionEvent);
                return;
            }
            return;
        }
        if (action != 1) {
            if (action == 2) {
                if (this.mWmService.isVivoFreeFormValid() && !this.mWmService.isFreeFormMin() && this.inResizeMode) {
                    onActionMove(motionEvent);
                    return;
                }
                return;
            } else if (action != 3) {
                if (action == 5 && this.inResizeMode) {
                    this.exitResizeModeToResize = true;
                    exitResizeMode();
                    resizeToFinalRect();
                    return;
                }
                return;
            }
        }
        if (this.inResizeMode) {
            this.exitResizeModeToResize = true;
            exitResizeMode();
            resizeToFinalRect();
        }
    }

    private void exitResizeMode() {
        clearFreeformResizeBorder();
    }

    private void resizeToFinalRect() {
        this.finalRect.set(this.mResizedLeft, this.mResizedTop, this.mResizedRight, this.mResizedBottom);
        scaleForFreeformWindow(this.finalRect);
        ActivityStack freeformStack = this.mWmService.mRoot.getVivoFreeformStack();
        if (freeformStack != null) {
            this.mWmService.mAtmService.resizeTask(freeformStack.mTaskId, this.finalRect, 2);
        }
    }

    private void scaleForFreeformWindow(Rect outBounds) {
        DisplayMetrics displayMetrics = this.mWmService.mContext.getResources().getDisplayMetrics();
        int displayWidth = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        int width = outBounds.width();
        int height = outBounds.height();
        float finalScale = 1.0f;
        if (displayWidth != 0) {
            finalScale = VivoFreeformUtils.inFullscreenMode ? (height * 1.0f) / displayWidth : (width * 1.0f) / displayWidth;
        }
        VSlog.d(TAG, "finalScale:" + finalScale + " outBounds:" + outBounds + " displayWidth:" + displayWidth);
        finalScale = (finalScale > 1.0f || finalScale < 0.001f) ? 1.0f : 1.0f;
        int scaledWidth = (int) ((width / finalScale) + 0.5f);
        int scaledHeight = (int) ((height / finalScale) + 0.5f);
        outBounds.set(outBounds.left, outBounds.top, outBounds.left + scaledWidth, outBounds.top + scaledHeight);
        this.mWmService.setFreeformScale(finalScale);
        updateScaleToApp(finalScale);
        VSlog.d(TAG, "Scaled bounds:" + outBounds);
    }

    private void updateScaleToApp(float newScale) {
        try {
            Settings.System.putFloatForUser(this.mWmService.mContext.getContentResolver(), "drag_resize_scale", newScale, this.mWmService.mCurrentUserId);
            VSlog.d(TAG, "updateScaleToApp newScale:" + newScale);
        } catch (Exception e) {
            VSlog.e(TAG, "updateScaleToApp e:" + e.getMessage());
        }
    }

    private void onActionMove(MotionEvent motionEvent) {
        this.mCurrentX = motionEvent.getX();
        float y = motionEvent.getY();
        this.mCurrentY = y;
        float dx = this.mCurrentX - this.mLastX;
        float dy = y - this.mLastY;
        int left = this.mResizedLeft;
        int top = this.mResizedTop;
        int right = this.mResizedRight;
        int bottom = this.mResizedBottom;
        int i = AnonymousClass1.$SwitchMap$com$android$server$wm$VivoFreeformGesturesPointerEventListener$Direction[this.mResizeDirection.ordinal()];
        if (i == 1) {
            left = (int) (left + dx);
            bottom = ((int) (((right - left) / this.mWidthHeightRatio) + 0.5f)) + top;
        } else if (i == 2) {
            right = (int) (right + dx);
            bottom = ((int) (((right - left) / this.mWidthHeightRatio) + 0.5f)) + top;
        } else if (i == 3) {
            int middlePos = ((right - left) / 2) + left;
            bottom = (int) (bottom + dy);
            int newWidth = (int) (((bottom - top) * this.mWidthHeightRatio) + 0.5f);
            left = middlePos - (newWidth / 2);
            right = left + newWidth;
        }
        int width = right - left;
        int height = bottom - top;
        if (VivoFreeformUtils.inFullscreenMode) {
            if (width < this.mFullscreenModeWinMinWidth || height < this.mFullscreenModeWinMinHeight || width > this.mFullscreenModeWinMaxWidth || height > this.mFullscreenModeWinMaxHeight) {
                setFreeformResizeLimitedBorder();
                return;
            }
            setFreeformResizeBorder(this.mResizeDirection);
        } else {
            int i2 = this.mRotation;
            if (i2 == 0 || i2 == 2) {
                if (width < this.mPortraitNormalWinMinWidth || height < this.mPortraitNormalWinMinHeight || width > this.mPortraitNormalWinMaxWidth || height > this.mPortraitNormalWinMaxHeight) {
                    setFreeformResizeLimitedBorder();
                    return;
                }
                setFreeformResizeBorder(this.mResizeDirection);
            } else if (i2 == 1 || i2 == 3) {
                if (width < this.mLandscapeNormalWinMinWidth || height < this.mLandscapeNormalWinMinHeight || width > this.mLandscapeNormalWinMaxWidth || height > this.mLandscapeNormalWinMaxHeight) {
                    setFreeformResizeLimitedBorder();
                    return;
                }
                setFreeformResizeBorder(this.mResizeDirection);
            }
        }
        scaleFreeformLeash(new Rect(left, top, right, bottom));
        this.mResizedLeft = left;
        this.mResizedTop = top;
        this.mResizedRight = right;
        this.mResizedBottom = bottom;
        this.mLastX = this.mCurrentX;
        this.mLastY = this.mCurrentY;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.wm.VivoFreeformGesturesPointerEventListener$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$wm$VivoFreeformGesturesPointerEventListener$Direction;

        static {
            int[] iArr = new int[Direction.values().length];
            $SwitchMap$com$android$server$wm$VivoFreeformGesturesPointerEventListener$Direction = iArr;
            try {
                iArr[Direction.LEFT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$wm$VivoFreeformGesturesPointerEventListener$Direction[Direction.RIGHT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$wm$VivoFreeformGesturesPointerEventListener$Direction[Direction.BOTTOM.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private void scaleFreeformLeash(Rect newRect) {
        SurfaceControl leash;
        float extraScale = (newRect.width() * 1.0f) / this.freeformRect.width();
        if (!this.mFreeFormWindows.isEmpty()) {
            for (int i = this.mFreeFormWindows.size() - 1; i >= 0; i--) {
                WindowState windowState = this.mFreeFormWindows.get(i);
                synchronized (this) {
                    if (windowState.mWinAnimator.mSurfaceController != null && windowState.mActivityRecord != null && this.mLeashMap.containsKey(windowState.mActivityRecord) && (leash = this.mLeashMap.get(windowState.mActivityRecord)) != null) {
                        VSlog.d(TAG, "scaleFreeformLeash new rect:" + newRect + " freeformRect:" + this.freeformRect);
                        this.mGestureTransaction.setPosition(leash, ((float) (newRect.left - this.freeformRect.left)) * 1.0f, ((float) (newRect.top - this.freeformRect.top)) * 1.0f);
                        this.mGestureTransaction.setMatrix(leash, extraScale, windowState.mWinAnimator.mDtDx, windowState.mWinAnimator.mDtDy, extraScale);
                        WindowManager.LayoutParams layoutParams = windowState.mAttrs;
                        layoutParams.privateFlags = layoutParams.privateFlags | 64;
                        applyTransaction();
                    }
                }
            }
        }
    }

    private void onActionDown(MotionEvent motionEvent) {
        this.mCurrentX = motionEvent.getX();
        float y = motionEvent.getY();
        this.mCurrentY = y;
        this.mLastX = this.mCurrentX;
        this.mLastY = y;
    }
}