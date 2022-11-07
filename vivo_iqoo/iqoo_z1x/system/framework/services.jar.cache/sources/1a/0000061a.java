package com.android.server.accessibility.gestures;

import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import com.android.server.job.controllers.JobStatus;

/* loaded from: classes.dex */
public class TouchState {
    public static final int ALL_POINTER_ID_BITS = -1;
    private static final String LOG_TAG = "TouchState";
    static final int MAX_POINTER_COUNT = 32;
    public static final int STATE_CLEAR = 0;
    public static final int STATE_DELEGATING = 4;
    public static final int STATE_DRAGGING = 3;
    public static final int STATE_GESTURE_DETECTING = 5;
    public static final int STATE_TOUCH_EXPLORING = 2;
    public static final int STATE_TOUCH_INTERACTING = 1;
    private int mInjectedPointersDown;
    private long mLastInjectedDownEventTime;
    private MotionEvent mLastInjectedHoverEvent;
    private MotionEvent mLastInjectedHoverEventForClick;
    private MotionEvent mLastReceivedEvent;
    private MotionEvent mLastReceivedRawEvent;
    private int mLastTouchedWindowId;
    private int mState = 0;
    private final ReceivedPointerTracker mReceivedPointerTracker = new ReceivedPointerTracker();

    /* loaded from: classes.dex */
    public @interface State {
    }

    public void clear() {
        setState(0);
        MotionEvent motionEvent = this.mLastReceivedEvent;
        if (motionEvent != null) {
            motionEvent.recycle();
            this.mLastReceivedEvent = null;
        }
        this.mLastTouchedWindowId = -1;
        this.mReceivedPointerTracker.clear();
        this.mInjectedPointersDown = 0;
    }

    public void onReceivedMotionEvent(MotionEvent rawEvent) {
        MotionEvent motionEvent = this.mLastReceivedEvent;
        if (motionEvent != null) {
            motionEvent.recycle();
        }
        MotionEvent motionEvent2 = this.mLastReceivedRawEvent;
        if (motionEvent2 != null) {
            motionEvent2.recycle();
        }
        this.mLastReceivedEvent = MotionEvent.obtain(rawEvent);
        this.mReceivedPointerTracker.onMotionEvent(rawEvent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onInjectedMotionEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(event.getActionIndex());
        int pointerFlag = 1 << pointerId;
        if (action != 0) {
            if (action != 1) {
                if (action != 5) {
                    if (action != 6) {
                        if (action == 7 || action == 9) {
                            MotionEvent motionEvent = this.mLastInjectedHoverEvent;
                            if (motionEvent != null) {
                                motionEvent.recycle();
                            }
                            this.mLastInjectedHoverEvent = MotionEvent.obtain(event);
                            return;
                        } else if (action == 10) {
                            MotionEvent motionEvent2 = this.mLastInjectedHoverEvent;
                            if (motionEvent2 != null) {
                                motionEvent2.recycle();
                            }
                            this.mLastInjectedHoverEvent = MotionEvent.obtain(event);
                            MotionEvent motionEvent3 = this.mLastInjectedHoverEventForClick;
                            if (motionEvent3 != null) {
                                motionEvent3.recycle();
                            }
                            this.mLastInjectedHoverEventForClick = MotionEvent.obtain(event);
                            return;
                        } else {
                            return;
                        }
                    }
                }
            }
            int i = this.mInjectedPointersDown & (~pointerFlag);
            this.mInjectedPointersDown = i;
            if (i == 0) {
                this.mLastInjectedDownEventTime = 0L;
                return;
            }
            return;
        }
        this.mInjectedPointersDown |= pointerFlag;
        this.mLastInjectedDownEventTime = event.getDownTime();
    }

    public void onReceivedAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        if (eventType != 32) {
            if (eventType == 128 || eventType == 256) {
                this.mLastTouchedWindowId = event.getWindowId();
                return;
            } else if (eventType != 32768) {
                return;
            }
        }
        MotionEvent motionEvent = this.mLastInjectedHoverEventForClick;
        if (motionEvent != null) {
            motionEvent.recycle();
            this.mLastInjectedHoverEventForClick = null;
        }
        this.mLastTouchedWindowId = -1;
    }

    public void onInjectedAccessibilityEvent(int type) {
        if (type == 512) {
            startTouchExploring();
        } else if (type == 1024) {
            startTouchInteracting();
        } else if (type == 262144) {
            startGestureDetecting();
        } else if (type == 524288) {
            startTouchInteracting();
        } else if (type == 1048576) {
            startTouchInteracting();
        } else if (type == 2097152) {
            clear();
        }
    }

    public int getState() {
        return this.mState;
    }

    public void setState(int state) {
        if (this.mState == state) {
            return;
        }
        this.mState = state;
    }

    public boolean isTouchExploring() {
        return this.mState == 2;
    }

    public void startTouchExploring() {
        setState(2);
    }

    public boolean isDelegating() {
        return this.mState == 4;
    }

    public void startDelegating() {
        setState(4);
    }

    public boolean isGestureDetecting() {
        return this.mState == 5;
    }

    public void startGestureDetecting() {
        setState(5);
    }

    public boolean isDragging() {
        return this.mState == 3;
    }

    public void startDragging() {
        setState(3);
    }

    public boolean isTouchInteracting() {
        return this.mState == 1;
    }

    public void startTouchInteracting() {
        setState(1);
    }

    public boolean isClear() {
        return this.mState == 0;
    }

    public String toString() {
        return "TouchState { mState: " + getStateSymbolicName(this.mState) + " }";
    }

    public static String getStateSymbolicName(int state) {
        if (state != 0) {
            if (state != 1) {
                if (state != 2) {
                    if (state != 3) {
                        if (state != 4) {
                            if (state == 5) {
                                return "STATE_GESTURE_DETECTING";
                            }
                            return "Unknown state: " + state;
                        }
                        return "STATE_DELEGATING";
                    }
                    return "STATE_DRAGGING";
                }
                return "STATE_TOUCH_EXPLORING";
            }
            return "STATE_TOUCH_INTERACTING";
        }
        return "STATE_CLEAR";
    }

    public ReceivedPointerTracker getReceivedPointerTracker() {
        return this.mReceivedPointerTracker;
    }

    public MotionEvent getLastReceivedEvent() {
        return this.mLastReceivedEvent;
    }

    public MotionEvent getLastInjectedHoverEvent() {
        return this.mLastInjectedHoverEvent;
    }

    public long getLastInjectedDownEventTime() {
        return this.mLastInjectedDownEventTime;
    }

    public int getLastTouchedWindowId() {
        return this.mLastTouchedWindowId;
    }

    public int getInjectedPointerDownCount() {
        return Integer.bitCount(this.mInjectedPointersDown);
    }

    public int getInjectedPointersDown() {
        return this.mInjectedPointersDown;
    }

    public boolean isInjectedPointerDown(int pointerId) {
        int pointerFlag = 1 << pointerId;
        return (this.mInjectedPointersDown & pointerFlag) != 0;
    }

    public MotionEvent getLastInjectedHoverEventForClick() {
        return this.mLastInjectedHoverEventForClick;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ReceivedPointerTracker {
        private static final String LOG_TAG_RECEIVED_POINTER_TRACKER = "ReceivedPointerTracker";
        private int mLastReceivedDownEdgeFlags;
        private int mPrimaryPointerId;
        private final PointerDownInfo[] mReceivedPointers = new PointerDownInfo[32];
        private int mReceivedPointersDown;

        ReceivedPointerTracker() {
            clear();
        }

        public void clear() {
            this.mReceivedPointersDown = 0;
            this.mPrimaryPointerId = 0;
            for (int i = 0; i < 32; i++) {
                this.mReceivedPointers[i] = new PointerDownInfo();
            }
        }

        public void onMotionEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == 0) {
                handleReceivedPointerDown(event.getActionIndex(), event);
            } else if (action == 1) {
                handleReceivedPointerUp(event.getActionIndex(), event);
            } else if (action == 5) {
                handleReceivedPointerDown(event.getActionIndex(), event);
            } else if (action == 6) {
                handleReceivedPointerUp(event.getActionIndex(), event);
            }
        }

        public int getReceivedPointerDownCount() {
            return Integer.bitCount(this.mReceivedPointersDown);
        }

        public boolean isReceivedPointerDown(int pointerId) {
            int pointerFlag = 1 << pointerId;
            return (this.mReceivedPointersDown & pointerFlag) != 0;
        }

        public float getReceivedPointerDownX(int pointerId) {
            return this.mReceivedPointers[pointerId].mX;
        }

        public float getReceivedPointerDownY(int pointerId) {
            return this.mReceivedPointers[pointerId].mY;
        }

        public long getReceivedPointerDownTime(int pointerId) {
            return this.mReceivedPointers[pointerId].mTime;
        }

        public int getPrimaryPointerId() {
            if (this.mPrimaryPointerId == -1) {
                this.mPrimaryPointerId = findPrimaryPointerId();
            }
            return this.mPrimaryPointerId;
        }

        public int getLastReceivedDownEdgeFlags() {
            return this.mLastReceivedDownEdgeFlags;
        }

        private void handleReceivedPointerDown(int pointerIndex, MotionEvent event) {
            int pointerId = event.getPointerId(pointerIndex);
            int pointerFlag = 1 << pointerId;
            this.mLastReceivedDownEdgeFlags = event.getEdgeFlags();
            this.mReceivedPointersDown |= pointerFlag;
            this.mReceivedPointers[pointerId].set(event.getX(pointerIndex), event.getY(pointerIndex), event.getEventTime());
            this.mPrimaryPointerId = pointerId;
        }

        private void handleReceivedPointerUp(int pointerIndex, MotionEvent event) {
            int pointerId = event.getPointerId(pointerIndex);
            int pointerFlag = 1 << pointerId;
            this.mReceivedPointersDown &= ~pointerFlag;
            this.mReceivedPointers[pointerId].clear();
            if (this.mPrimaryPointerId == pointerId) {
                this.mPrimaryPointerId = -1;
            }
        }

        private int findPrimaryPointerId() {
            int primaryPointerId = -1;
            long minDownTime = JobStatus.NO_LATEST_RUNTIME;
            int pointerIdBits = this.mReceivedPointersDown;
            while (pointerIdBits > 0) {
                int pointerId = Integer.numberOfTrailingZeros(pointerIdBits);
                pointerIdBits &= ~(1 << pointerId);
                long downPointerTime = this.mReceivedPointers[pointerId].mTime;
                if (downPointerTime < minDownTime) {
                    minDownTime = downPointerTime;
                    primaryPointerId = pointerId;
                }
            }
            return primaryPointerId;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("=========================");
            builder.append("\nDown pointers #");
            builder.append(getReceivedPointerDownCount());
            builder.append(" [ ");
            for (int i = 0; i < 32; i++) {
                if (isReceivedPointerDown(i)) {
                    builder.append(i);
                    builder.append(" ");
                }
            }
            builder.append("]");
            builder.append("\nPrimary pointer id [ ");
            builder.append(getPrimaryPointerId());
            builder.append(" ]");
            builder.append("\n=========================");
            return builder.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class PointerDownInfo {
        private long mTime;
        private float mX;
        private float mY;

        PointerDownInfo() {
        }

        public void set(float x, float y, long time) {
            this.mX = x;
            this.mY = y;
            this.mTime = time;
        }

        public void clear() {
            this.mX = 0.0f;
            this.mY = 0.0f;
            this.mTime = 0L;
        }
    }
}