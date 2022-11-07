package com.android.server.wm;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import android.widget.OverScroller;
import com.android.server.IVivoRmsInjector;
import com.android.server.policy.IVivoRatioControllerUtils;
import com.android.server.usb.descriptors.UsbACInterface;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class SystemGesturesPointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
    private static final boolean DEBUG = false;
    public static final String GAME_MODE_PROP = "sys.battlemode.touchwindow";
    private static final int MAX_FLING_TIME_MILLIS = 5000;
    private static final int MAX_TRACKED_POINTERS = 32;
    private static final int SWIPE_FROM_BOTTOM = 2;
    private static final int SWIPE_FROM_LEFT = 4;
    private static final int SWIPE_FROM_RIGHT = 3;
    private static final int SWIPE_FROM_TOP = 1;
    private static final int SWIPE_NONE = 0;
    private static final long SWIPE_TIMEOUT_MS = 500;
    private static final String TAG = "SystemGestures";
    private static final int UNTRACKED_POINTER = -1;
    private final Callbacks mCallbacks;
    private final Context mContext;
    private boolean mDebugFireable;
    private int mDisplayCutoutTouchableRegionSize;
    private int mDownPointers;
    private GestureDetector mGestureDetector;
    private final Handler mHandler;
    private long mLastFlingTime;
    private boolean mMouseHoveringAtEdge;
    private int mRejectionWidthFromTop;
    private boolean mScrollFired;
    private int mSwipeDistanceThreshold;
    private boolean mSwipeFireable;
    private int mSwipeStartThreshold;
    int screenHeight;
    int screenWidth;
    private static final int PALM_REJECTION_WIDTH = SystemProperties.getInt("persist.vivo.palm.rejection.width", 240);
    private static final int PALM_REJECTION_HEIGHT = SystemProperties.getInt("persist.vivo.palm.rejection.height", (int) IVivoRatioControllerUtils.NAVI_HEIGHT_1080P);
    public boolean mGameModeEnabled = false;
    public boolean mGameModeRejectionEnable = false;
    private final int[] mDownPointerId = new int[32];
    private final float[] mDownX = new float[32];
    private final float[] mDownY = new float[32];
    private final long[] mDownTime = new long[32];

    /* loaded from: classes2.dex */
    interface Callbacks {
        void onDebug();

        void onDown();

        void onFling(int i);

        void onHorizontalFling(int i);

        void onMouseHoverAtBottom();

        void onMouseHoverAtTop();

        void onMouseLeaveFromEdge();

        void onScroll(boolean z);

        void onSwipeFromBottom();

        void onSwipeFromLeft();

        void onSwipeFromRight();

        void onSwipeFromTop();

        void onUpOrCancel();

        void onVerticalFling(int i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemGesturesPointerEventListener(Context context, Handler handler, Callbacks callbacks) {
        this.mContext = (Context) checkNull("context", context);
        this.mHandler = handler;
        this.mCallbacks = (Callbacks) checkNull("callbacks", callbacks);
        onConfigurationChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onConfigurationChanged() {
        this.mSwipeStartThreshold = this.mContext.getResources().getDimensionPixelSize(17105488);
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        DisplayCutout displayCutout = display.getCutout();
        if (displayCutout != null) {
            Rect bounds = displayCutout.getBoundingRectTop();
            if (!bounds.isEmpty()) {
                int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(17105180);
                this.mDisplayCutoutTouchableRegionSize = dimensionPixelSize;
                this.mSwipeStartThreshold += dimensionPixelSize;
            }
        }
        this.mSwipeDistanceThreshold = this.mSwipeStartThreshold;
        this.mRejectionWidthFromTop = this.mContext.getResources().getDimensionPixelSize(51118367);
    }

    private static <T> T checkNull(String name, T arg) {
        if (arg == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return arg;
    }

    public void systemReady() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.-$$Lambda$SystemGesturesPointerEventListener$9Iw39fjTtjXO5kacgrpdxfxjuSY
            @Override // java.lang.Runnable
            public final void run() {
                SystemGesturesPointerEventListener.this.lambda$systemReady$0$SystemGesturesPointerEventListener();
            }
        });
    }

    public /* synthetic */ void lambda$systemReady$0$SystemGesturesPointerEventListener() {
        int displayId = this.mContext.getDisplayId();
        DisplayInfo info = DisplayManagerGlobal.getInstance().getDisplayInfo(displayId);
        if (info == null) {
            Slog.w(TAG, "Cannot create GestureDetector, display removed:" + displayId);
            return;
        }
        this.mGestureDetector = new GestureDetector(this.mContext, new FlingGestureDetector(), this.mHandler) { // from class: com.android.server.wm.SystemGesturesPointerEventListener.1
        };
    }

    public void onPointerEvent(MotionEvent event) {
        if (this.mGestureDetector != null && event.isTouchEvent()) {
            this.mGestureDetector.onTouchEvent(event);
        }
        int actionMasked = event.getActionMasked();
        if (actionMasked == 0) {
            this.mSwipeFireable = true;
            this.mDebugFireable = true;
            this.mScrollFired = false;
            this.mDownPointers = 0;
            captureDown(event, 0);
            if (this.mMouseHoveringAtEdge) {
                this.mMouseHoveringAtEdge = false;
                this.mCallbacks.onMouseLeaveFromEdge();
            }
            this.mCallbacks.onDown();
            return;
        }
        if (actionMasked != 1) {
            if (actionMasked == 2) {
                if (this.mSwipeFireable) {
                    int swipe = detectSwipe(event);
                    this.mSwipeFireable = swipe == 0;
                    if (swipe == 1) {
                        this.mCallbacks.onSwipeFromTop();
                        return;
                    } else if (swipe == 2) {
                        this.mCallbacks.onSwipeFromBottom();
                        return;
                    } else if (swipe == 3) {
                        this.mCallbacks.onSwipeFromRight();
                        return;
                    } else if (swipe == 4) {
                        this.mCallbacks.onSwipeFromLeft();
                        return;
                    } else {
                        return;
                    }
                }
                return;
            } else if (actionMasked != 3) {
                if (actionMasked == 5) {
                    captureDown(event, event.getActionIndex());
                    if (this.mDebugFireable) {
                        boolean z = event.getPointerCount() < 5;
                        this.mDebugFireable = z;
                        if (!z) {
                            this.mCallbacks.onDebug();
                            return;
                        }
                        return;
                    }
                    return;
                } else if (actionMasked == 7 && event.isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
                    if (!this.mMouseHoveringAtEdge && event.getY() == 0.0f) {
                        this.mCallbacks.onMouseHoverAtTop();
                        this.mMouseHoveringAtEdge = true;
                        return;
                    } else if (!this.mMouseHoveringAtEdge && event.getY() >= this.screenHeight - 1) {
                        this.mCallbacks.onMouseHoverAtBottom();
                        this.mMouseHoveringAtEdge = true;
                        return;
                    } else if (this.mMouseHoveringAtEdge && event.getY() > 0.0f && event.getY() < this.screenHeight - 1) {
                        this.mCallbacks.onMouseLeaveFromEdge();
                        this.mMouseHoveringAtEdge = false;
                        return;
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }
        this.mSwipeFireable = false;
        this.mDebugFireable = false;
        if (this.mScrollFired) {
            this.mCallbacks.onScroll(false);
        }
        this.mScrollFired = false;
        this.mCallbacks.onUpOrCancel();
    }

    private void captureDown(MotionEvent event, int pointerIndex) {
        int pointerId = event.getPointerId(pointerIndex);
        int i = findIndex(pointerId);
        if (i != -1) {
            this.mDownX[i] = event.getX(pointerIndex);
            this.mDownY[i] = event.getY(pointerIndex);
            this.mDownTime[i] = event.getEventTime();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean currentGestureStartedInRegion(Region r) {
        return r.contains((int) this.mDownX[0], (int) this.mDownY[0]);
    }

    private int findIndex(int pointerId) {
        int i = 0;
        while (true) {
            int i2 = this.mDownPointers;
            if (i < i2) {
                if (this.mDownPointerId[i] != pointerId) {
                    i++;
                } else {
                    return i;
                }
            } else if (i2 == 32 || pointerId == -1) {
                return -1;
            } else {
                int[] iArr = this.mDownPointerId;
                int i3 = i2 + 1;
                this.mDownPointers = i3;
                iArr[i2] = pointerId;
                return i3 - 1;
            }
        }
    }

    private int detectSwipe(MotionEvent move) {
        int historySize = move.getHistorySize();
        int pointerCount = move.getPointerCount();
        for (int p = 0; p < pointerCount; p++) {
            int pointerId = move.getPointerId(p);
            int i = findIndex(pointerId);
            if (i != -1) {
                for (int h = 0; h < historySize; h++) {
                    long time = move.getHistoricalEventTime(h);
                    float x = move.getHistoricalX(p, h);
                    float y = move.getHistoricalY(p, h);
                    int swipe = detectSwipe(i, time, x, y);
                    if (swipe != 0) {
                        return swipe;
                    }
                }
                int swipe2 = detectSwipe(i, move.getEventTime(), move.getX(p), move.getY(p));
                if (swipe2 != 0) {
                    return swipe2;
                }
            }
        }
        return 0;
    }

    private int detectSwipe(int i, long time, float x, float y) {
        float fromX = this.mDownX[i];
        float fromY = this.mDownY[i];
        long elapsed = time - this.mDownTime[i];
        if (fromY <= this.mSwipeStartThreshold && y > this.mSwipeDistanceThreshold + fromY && elapsed < 500 && !shouldRejectFromTop(fromX)) {
            return 1;
        }
        if (fromY >= this.screenHeight - this.mSwipeStartThreshold && y < fromY - this.mSwipeDistanceThreshold && elapsed < 500) {
            return 2;
        }
        if (fromX >= this.screenWidth - this.mSwipeStartThreshold && x < fromX - this.mSwipeDistanceThreshold && elapsed < 500 && !shouldRejectFromLeftOrRight(fromY)) {
            return 3;
        }
        if (fromX <= this.mSwipeStartThreshold && x > this.mSwipeDistanceThreshold + fromX && elapsed < 500 && !shouldRejectFromLeftOrRight(fromY)) {
            return 4;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class FlingGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private OverScroller mOverscroller;

        FlingGestureDetector() {
            this.mOverscroller = new OverScroller(SystemGesturesPointerEventListener.this.mContext);
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onSingleTapUp(MotionEvent e) {
            if (!this.mOverscroller.isFinished()) {
                this.mOverscroller.forceFinished(true);
            }
            return true;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onFling(MotionEvent down, MotionEvent up, float velocityX, float velocityY) {
            this.mOverscroller.computeScrollOffset();
            long now = SystemClock.uptimeMillis();
            if (SystemGesturesPointerEventListener.this.mLastFlingTime != 0 && now > SystemGesturesPointerEventListener.this.mLastFlingTime + 5000) {
                this.mOverscroller.forceFinished(true);
            }
            this.mOverscroller.fling(0, 0, (int) velocityX, (int) velocityY, Integer.MIN_VALUE, IVivoRmsInjector.QUIET_TYPE_ALL, Integer.MIN_VALUE, IVivoRmsInjector.QUIET_TYPE_ALL);
            int duration = this.mOverscroller.getDuration();
            if (duration > 5000) {
                duration = 5000;
            }
            if (Math.abs(velocityY) >= Math.abs(velocityX)) {
                SystemGesturesPointerEventListener.this.mCallbacks.onVerticalFling(duration);
            } else {
                SystemGesturesPointerEventListener.this.mCallbacks.onHorizontalFling(duration);
            }
            SystemGesturesPointerEventListener.this.mLastFlingTime = now;
            SystemGesturesPointerEventListener.this.mCallbacks.onFling(duration);
            return true;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!SystemGesturesPointerEventListener.this.mScrollFired) {
                SystemGesturesPointerEventListener.this.mCallbacks.onScroll(true);
                SystemGesturesPointerEventListener.this.mScrollFired = true;
            }
            return true;
        }
    }

    private boolean shouldRejectFromTop(float fromX) {
        if (this.mGameModeEnabled && isLandscape()) {
            int i = PALM_REJECTION_WIDTH;
            if (fromX < i || fromX > this.screenWidth - i) {
                Slog.d(TAG, "reject from top because swipe from X:" + fromX);
                if (SystemProperties.getInt(GAME_MODE_PROP, 0) == 1) {
                    return true;
                }
            }
        }
        if (this.mGameModeRejectionEnable && isLandscape()) {
            int i2 = this.screenWidth;
            int i3 = this.mRejectionWidthFromTop;
            float leftBorder = (i2 - i3) / 2.0f;
            float rightBorder = (i2 + i3) / 2.0f;
            if (fromX < leftBorder || fromX > rightBorder) {
                Slog.d(TAG, "reject from top by gamemode rejectionss because swipe from X:" + fromX);
                return true;
            }
        }
        return false;
    }

    private boolean shouldRejectFromLeftOrRight(float fromY) {
        if (this.mGameModeEnabled && isLandscape()) {
            int i = PALM_REJECTION_HEIGHT;
            if (fromY < i || fromY > this.screenHeight - i) {
                Slog.d(TAG, "reject from top because swipe from Y:" + fromY);
                if (SystemProperties.getInt(GAME_MODE_PROP, 0) == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isLandscape() {
        return this.mContext.getResources().getConfiguration().orientation == 2;
    }
}