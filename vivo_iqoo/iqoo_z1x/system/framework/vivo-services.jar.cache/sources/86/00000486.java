package com.android.server.policy.motion;

import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.MathUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import com.android.server.policy.VivoWMPHook;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public final class VivoMSPointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
    private Context mContext;
    private GestureDetector mGestureDetector = null;
    private Handler mHandler;

    public VivoMSPointerEventListener(Context context) {
        this.mContext = null;
        this.mHandler = null;
        this.mContext = context;
        this.mHandler = new Handler();
        new Thread(new Runnable() { // from class: com.android.server.policy.motion.VivoMSPointerEventListener.1
            @Override // java.lang.Runnable
            public void run() {
                while (!ActivityManagerNative.isSystemReady()) {
                    SystemClock.sleep(500L);
                }
                VivoMSPointerEventListener.this.printf("System is ready, createGestureDetector.");
                VivoMSPointerEventListener.this.createGestureDetector();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void createGestureDetector() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.policy.motion.VivoMSPointerEventListener.2
            @Override // java.lang.Runnable
            public void run() {
                VivoMSPointerEventListener.this.mGestureDetector = new GestureDetector(VivoMSPointerEventListener.this.mContext, new MSGestureListener());
            }
        });
    }

    public void onPointerEvent(MotionEvent event) {
        GestureDetector gestureDetector = this.mGestureDetector;
        if (gestureDetector == null) {
            VLog.w(VivoWMPHook.TAG, "VMSGesture Warning: system isn't ready!");
        } else {
            gestureDetector.onTouchEvent(event);
        }
    }

    /* loaded from: classes.dex */
    class MSGestureListener implements GestureDetector.OnGestureListener {
        static final String TAG_MS = "VMSGesture";
        private float mBaceRightX;
        private float mBackLeftX;
        private DisplayMetrics mDisplayMetrics;
        private float mFlingMinVelocityToLeft;
        private float mFlingMinVelocityToRight;
        private float mForthMoveEnouthLength;
        private float mMaxFirstToLastYInterval;
        private ScreenResolutionReceiver mScreenResolutionReveiver;
        private float mStartScrollMaxX;
        private float mStartScrollMaxY;
        private float mStartScrollMinX;
        private float mStartScrollMinY;
        private Vibrator mVibrator;
        private float mXMoveMaxLength;
        private float mXMoveMinLength;
        private float mYMoveMaxLength;
        private final boolean DEBUG_MS = false;
        private final float START_MIN_X = 0.2f;
        private final float START_MAX_X = 0.8f;
        private final float START_MIN_Y = 0.07f;
        private final float START_MAX_Y = 0.9f;
        private final float MOVE_LENGTH_FORTH = 0.185f;
        private final float Y_MOVE_MAX_LENGTH = 0.8f;
        private final float FLING_VELOCITY_RIGHT = 0.3f;
        private final float FLING_VELOCITY_LEFT = 0.5f;
        private final float INVALID_NUM = -0.1f;
        private float mFirstScrollX = -0.1f;
        private float mFirstScrollY = -0.1f;
        private float mLastScrollX = -0.1f;
        private float mLastScrollY = -0.1f;
        private float mChangeOrigentationX = -0.1f;
        private float mChangeOrigentationY = -0.1f;
        private float mMaxY = -0.1f;
        private float mMinY = -0.1f;
        private boolean isStarted = false;
        private boolean isFistMoveToRight = false;
        private boolean isForthLengthEnough = false;
        private boolean isBackLengthEnough = false;
        private boolean isChangeOrigentation = false;
        private int mChangeOrientationTimes = 0;
        private final int VIBRATE_DURATION = 60;
        private final float BACK_LEFT_X = 0.18f;
        private final float BACK_RIGHT_X = 0.82f;
        private final float MAX_FIRST_TO_RIGHT_Y_INTERVAL = 0.22f;
        private float mXMoveLength = 0.0f;
        private final float X_MOVE_MAX_LENGTH = 0.8f;
        private final float X_MOVE_MIN_LENGTH = 0.2f;
        private final String ACTION_SCREEN_RESOLUTION_CHANGED = "com.vivo.pem.setres";

        private void resetFlag(float x, float y) {
            this.mFirstScrollX = x;
            this.mFirstScrollY = y;
            this.mLastScrollX = x;
            this.mLastScrollY = y;
            this.mChangeOrigentationX = -0.1f;
            this.mChangeOrigentationY = -0.1f;
            this.isStarted = false;
            this.isFistMoveToRight = false;
            this.isForthLengthEnough = false;
            this.isBackLengthEnough = false;
            this.isChangeOrigentation = false;
            this.mChangeOrientationTimes = 0;
            this.mMaxY = 0.0f;
            this.mMinY = 0.0f;
            this.mXMoveLength = 0.0f;
        }

        private void setMinScreenShow(boolean windowPosLeft) {
            int msWindowPos;
            boolean isSPS = SystemProperties.getBoolean("sys.super_power_save", false);
            if (isSPS) {
                VLog.v(TAG_MS, "setMinScreenShow enter but isSPS:" + isSPS + " and return");
                return;
            }
            boolean isGestureEnable = Settings.System.getIntForUser(VivoMSPointerEventListener.this.mContext.getContentResolver(), "vivo_mini_screen_gesture_enable", 0, -2) == 1;
            if (isGestureEnable) {
                PowerManager powerManager = (PowerManager) VivoMSPointerEventListener.this.mContext.getSystemService("power");
                boolean screenOn = powerManager.isScreenOn();
                KeyguardManager keyguardManager = (KeyguardManager) VivoMSPointerEventListener.this.mContext.getSystemService("keyguard");
                boolean keyguardLocked = keyguardManager.isKeyguardLocked();
                VLog.v(TAG_MS, "setMinScreenShow enter start");
                if (screenOn && !keyguardLocked) {
                    WindowManager windowManager = (WindowManager) VivoMSPointerEventListener.this.mContext.getSystemService("window");
                    int rotation = windowManager.getDefaultDisplay().getRotation();
                    if (rotation != 0) {
                        VLog.v(TAG_MS, "rotation = " + rotation + "just return and not start minscreen");
                        return;
                    }
                    this.mVibrator.vibrate(60L);
                    if (windowPosLeft) {
                        msWindowPos = 0;
                    } else {
                        msWindowPos = 1;
                    }
                    VLog.v(TAG_MS, "setMinScreenShow success msWindowPos:" + msWindowPos + " VIBRATE_DURATION:60");
                    Intent msIntent = new Intent();
                    msIntent.setAction("android.action.multifloatingtask.showsmallwindow");
                    msIntent.putExtra("windowPosValue", msWindowPos);
                    msIntent.addFlags(Dataspace.TRANSFER_GAMMA2_2);
                    msIntent.addFlags(268435456);
                    VivoMSPointerEventListener.this.mContext.sendBroadcastAsUser(msIntent, UserHandle.CURRENT);
                    return;
                }
                return;
            }
            VLog.v(TAG_MS, "setMinScreenShow enter but isGestureEnable:" + isGestureEnable + " and return");
        }

        private boolean checkFirstMoveAngle() {
            float internalX = Math.abs(this.mFirstScrollX - this.mChangeOrigentationX);
            float internalY = Math.abs(this.mFirstScrollY - this.mChangeOrigentationY);
            if (internalX > internalY) {
                return true;
            }
            return false;
        }

        private boolean checkFirstToLastYInterval() {
            float interval = Math.abs(this.mFirstScrollY - this.mLastScrollY);
            if (interval <= this.mMaxFirstToLastYInterval) {
                return true;
            }
            return false;
        }

        private boolean checkMoveLengthEnough(float endFlingX, float endFlingY) {
            float forthLength = Math.abs(this.mChangeOrigentationX - this.mFirstScrollX);
            return (endFlingX <= this.mBackLeftX || endFlingX >= this.mBaceRightX) && forthLength >= this.mForthMoveEnouthLength;
        }

        private boolean checkMoveYLengthToLarge() {
            float Length = Math.abs(this.mMaxY - this.mMinY);
            if (Length < this.mYMoveMaxLength) {
                VLog.v(TAG_MS, "minscreen checkMoveYLengthToLarge true");
                return true;
            }
            return false;
        }

        private boolean checkMoveXLength() {
            float Length = this.mXMoveLength;
            if (Length < this.mXMoveMaxLength && Length > this.mXMoveMinLength) {
                VLog.v(TAG_MS, "minscreen checkMoveXLength true");
                return true;
            }
            return false;
        }

        private boolean checkFlingVelocity(float velocity) {
            return this.isFistMoveToRight ? Math.abs(velocity) >= this.mFlingMinVelocityToRight : Math.abs(velocity) >= this.mFlingMinVelocityToLeft;
        }

        private boolean checkStartY() {
            float f = this.mFirstScrollY;
            if (f < this.mStartScrollMinY || f > this.mStartScrollMaxY) {
                return false;
            }
            return true;
        }

        @Override // android.view.GestureDetector.OnGestureListener
        public boolean onDown(MotionEvent arg0) {
            return false;
        }

        @Override // android.view.GestureDetector.OnGestureListener
        public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
            if (arg0 == null || arg1 == null) {
                VLog.w(TAG_MS, "onFling arg0=" + arg0 + " arg1=" + arg1);
                resetFlag(-0.1f, -0.1f);
                return false;
            }
            boolean windowPosLeft = this.isFistMoveToRight;
            this.mLastScrollX = arg1.getX();
            this.mLastScrollY = arg1.getY();
            if (arg0.getX() != arg1.getX() || arg0.getY() != arg1.getY()) {
                if (this.isChangeOrigentation && checkMoveLengthEnough(arg1.getX(), arg1.getY()) && this.mChangeOrientationTimes <= 10 && checkFlingVelocity(arg2) && checkMoveYLengthToLarge() && checkStartY() && checkFirstToLastYInterval() && checkFirstMoveAngle() && checkMoveXLength()) {
                    resetFlag(-0.1f, -0.1f);
                    setMinScreenShow(windowPosLeft);
                } else {
                    resetFlag(-0.1f, -0.1f);
                }
            } else {
                resetFlag(-0.1f, -0.1f);
            }
            return false;
        }

        @Override // android.view.GestureDetector.OnGestureListener
        public void onLongPress(MotionEvent arg0) {
        }

        @Override // android.view.GestureDetector.OnGestureListener
        public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
            if (arg0 == null || arg1 == null) {
                VLog.w(TAG_MS, "onScroll arg0=" + arg0 + " arg1=" + arg1);
                resetFlag(-0.1f, -0.1f);
                return false;
            }
            float arg0X = arg0.getX();
            float arg0Y = arg0.getY();
            float arg1X = arg1.getX();
            float arg1Y = arg1.getY();
            if (!this.isStarted) {
                if (arg2 > 0.0f && arg0X > this.mStartScrollMaxX) {
                    this.mMaxY = arg0Y;
                    this.mMinY = arg0Y;
                    this.mFirstScrollX = arg0X;
                    this.mFirstScrollY = arg0Y;
                    this.isFistMoveToRight = false;
                    this.isStarted = true;
                } else if (arg2 < 0.0f && arg0X < this.mStartScrollMinX) {
                    this.mMaxY = arg0Y;
                    this.mMinY = arg0Y;
                    this.mFirstScrollX = arg0X;
                    this.mFirstScrollY = arg0Y;
                    this.isFistMoveToRight = true;
                    this.isStarted = true;
                }
                return false;
            } else if (this.mFirstScrollX != arg0X || this.mFirstScrollY != arg0Y) {
                resetFlag(-0.1f, -0.1f);
                if (arg2 > 0.0f && arg0X > this.mStartScrollMaxX) {
                    this.mMaxY = arg0Y;
                    this.mMinY = arg0Y;
                    this.mFirstScrollX = arg0X;
                    this.mFirstScrollY = arg0Y;
                    this.isFistMoveToRight = false;
                    this.isStarted = true;
                } else if (arg2 < 0.0f && arg0X < this.mStartScrollMinX) {
                    this.mMaxY = arg0Y;
                    this.mMinY = arg0Y;
                    this.mFirstScrollX = arg0X;
                    this.mFirstScrollY = arg0Y;
                    this.isFistMoveToRight = true;
                    this.isStarted = true;
                }
                return false;
            } else {
                if (arg1Y > this.mMaxY) {
                    this.mMaxY = arg1Y;
                } else if (arg1Y < this.mMinY) {
                    this.mMinY = arg1Y;
                }
                if (MathUtils.abs(arg1X - arg0X) >= this.mXMoveLength) {
                    this.mXMoveLength = MathUtils.abs(arg1X - arg0X);
                }
                if (!this.isChangeOrigentation) {
                    if (this.isFistMoveToRight) {
                        if (arg2 < 0.0f) {
                            return false;
                        }
                        this.isChangeOrigentation = true;
                        this.mChangeOrigentationX = arg1X;
                        this.mChangeOrigentationY = arg1Y;
                        return false;
                    } else if (arg2 > 0.0f) {
                        return false;
                    } else {
                        this.isChangeOrigentation = true;
                        this.mChangeOrigentationX = arg1X;
                        this.mChangeOrigentationY = arg1Y;
                        return false;
                    }
                }
                if (arg2 > 0.0f && !this.isFistMoveToRight) {
                    this.mChangeOrientationTimes++;
                } else if (arg2 < 0.0f && this.isFistMoveToRight) {
                    this.mChangeOrientationTimes++;
                }
                return false;
            }
        }

        @Override // android.view.GestureDetector.OnGestureListener
        public void onShowPress(MotionEvent arg0) {
        }

        @Override // android.view.GestureDetector.OnGestureListener
        public boolean onSingleTapUp(MotionEvent arg0) {
            return false;
        }

        /* loaded from: classes.dex */
        private class ScreenResolutionReceiver extends BroadcastReceiver {
            public ScreenResolutionReceiver() {
                IntentFilter filter = new IntentFilter();
                filter.addAction("com.vivo.pem.setres");
                VivoMSPointerEventListener.this.mContext.registerReceiver(this, filter);
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                VLog.i(MSGestureListener.TAG_MS, "Receive " + intent);
                MSGestureListener mSGestureListener = MSGestureListener.this;
                mSGestureListener.mDisplayMetrics = VivoMSPointerEventListener.this.mContext.getResources().getDisplayMetrics();
                MSGestureListener mSGestureListener2 = MSGestureListener.this;
                mSGestureListener2.mStartScrollMinX = mSGestureListener2.mDisplayMetrics.widthPixels * 0.2f;
                MSGestureListener mSGestureListener3 = MSGestureListener.this;
                mSGestureListener3.mStartScrollMaxX = mSGestureListener3.mDisplayMetrics.widthPixels * 0.8f;
                MSGestureListener mSGestureListener4 = MSGestureListener.this;
                mSGestureListener4.mStartScrollMinY = mSGestureListener4.mDisplayMetrics.heightPixels * 0.07f;
                MSGestureListener mSGestureListener5 = MSGestureListener.this;
                mSGestureListener5.mStartScrollMaxY = mSGestureListener5.mDisplayMetrics.heightPixels * 0.9f;
                MSGestureListener mSGestureListener6 = MSGestureListener.this;
                mSGestureListener6.mForthMoveEnouthLength = mSGestureListener6.mDisplayMetrics.widthPixels * 0.185f;
                MSGestureListener mSGestureListener7 = MSGestureListener.this;
                mSGestureListener7.mYMoveMaxLength = mSGestureListener7.mDisplayMetrics.heightPixels * 0.8f;
                MSGestureListener mSGestureListener8 = MSGestureListener.this;
                mSGestureListener8.mFlingMinVelocityToRight = mSGestureListener8.mDisplayMetrics.widthPixels * 0.3f;
                MSGestureListener mSGestureListener9 = MSGestureListener.this;
                mSGestureListener9.mFlingMinVelocityToLeft = mSGestureListener9.mDisplayMetrics.widthPixels * 0.5f;
                MSGestureListener mSGestureListener10 = MSGestureListener.this;
                mSGestureListener10.mBackLeftX = mSGestureListener10.mDisplayMetrics.widthPixels * 0.18f;
                MSGestureListener mSGestureListener11 = MSGestureListener.this;
                mSGestureListener11.mBaceRightX = mSGestureListener11.mDisplayMetrics.widthPixels * 0.82f;
                MSGestureListener mSGestureListener12 = MSGestureListener.this;
                mSGestureListener12.mMaxFirstToLastYInterval = mSGestureListener12.mDisplayMetrics.heightPixels * 0.22f;
                MSGestureListener mSGestureListener13 = MSGestureListener.this;
                mSGestureListener13.mXMoveMaxLength = mSGestureListener13.mDisplayMetrics.widthPixels * 0.8f;
                MSGestureListener mSGestureListener14 = MSGestureListener.this;
                mSGestureListener14.mXMoveMinLength = mSGestureListener14.mDisplayMetrics.widthPixels * 0.2f;
            }
        }

        public MSGestureListener() {
            this.mDisplayMetrics = new DisplayMetrics();
            this.mStartScrollMinX = 54.0f;
            this.mStartScrollMaxX = 1026.0f;
            this.mStartScrollMinY = 0.0f;
            this.mStartScrollMaxY = 0.0f;
            this.mForthMoveEnouthLength = 250.0f;
            this.mYMoveMaxLength = 400.0f;
            this.mFlingMinVelocityToRight = 3000.0f;
            this.mFlingMinVelocityToLeft = 3000.0f;
            this.mBackLeftX = 0.0f;
            this.mBaceRightX = 0.0f;
            this.mMaxFirstToLastYInterval = 0.0f;
            this.mXMoveMaxLength = 400.0f;
            this.mXMoveMinLength = 0.0f;
            this.mVibrator = (Vibrator) VivoMSPointerEventListener.this.mContext.getSystemService("vibrator");
            DisplayMetrics displayMetrics = VivoMSPointerEventListener.this.mContext.getResources().getDisplayMetrics();
            this.mDisplayMetrics = displayMetrics;
            this.mStartScrollMinX = displayMetrics.widthPixels * 0.2f;
            this.mStartScrollMaxX = this.mDisplayMetrics.widthPixels * 0.8f;
            this.mStartScrollMinY = this.mDisplayMetrics.heightPixels * 0.07f;
            this.mStartScrollMaxY = this.mDisplayMetrics.heightPixels * 0.9f;
            this.mForthMoveEnouthLength = this.mDisplayMetrics.widthPixels * 0.185f;
            this.mYMoveMaxLength = this.mDisplayMetrics.heightPixels * 0.8f;
            this.mFlingMinVelocityToRight = this.mDisplayMetrics.widthPixels * 0.3f;
            this.mFlingMinVelocityToLeft = this.mDisplayMetrics.widthPixels * 0.5f;
            this.mBackLeftX = this.mDisplayMetrics.widthPixels * 0.18f;
            this.mBaceRightX = this.mDisplayMetrics.widthPixels * 0.82f;
            this.mMaxFirstToLastYInterval = this.mDisplayMetrics.heightPixels * 0.22f;
            this.mXMoveMaxLength = this.mDisplayMetrics.widthPixels * 0.8f;
            this.mXMoveMinLength = this.mDisplayMetrics.widthPixels * 0.2f;
            this.mScreenResolutionReveiver = new ScreenResolutionReceiver();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void printf(String msg) {
        VivoWMPHook.printf(msg);
    }
}