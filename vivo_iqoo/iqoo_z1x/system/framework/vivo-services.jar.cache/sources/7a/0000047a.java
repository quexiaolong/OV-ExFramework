package com.android.server.policy.motion;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.SQLException;
import android.hardware.input.InputManager;
import android.multidisplay.MultiDisplayManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.InputMonitor;
import android.view.MotionEvent;
import com.android.server.UiThread;
import com.android.server.input.ThreeFingerConfigManager;
import com.android.server.policy.VivoPolicyUtil;
import com.android.server.policy.VivoWMPHook;
import com.vivo.common.utils.VLog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public class ThreeFingerGesture {
    private static final int CHECK_INTERVAL_DELAY;
    private static final boolean DEBUG;
    private static final int DELAY_TIME = 100;
    private static final String DISABLE_SCREEN_CAPTURE_ON = "disable_screen_capture_on";
    private static int FINGER_DISTANCE_MAX_THRESHOLD = 0;
    private static int FINGER_DISTANCE_MAX_THRESHOLD_Y_LANDSCAPE = 0;
    private static int FINGER_DISTANCE_MIN_THRESHOLD = 0;
    private static int FINGER_ROTATION_THRESHOLD = 0;
    private static final boolean IS_NEW_GESTURE_PRODUCT;
    private static final int MAX_TOUCHPOINTS = 3;
    private static final String TAG = "ThreeFingerGesture";
    private static final int THREEPOINTER_TREND;
    private AccessibilityServicesObserver mAccessibilityServicesObserver;
    private Context mContext;
    private InputEventReceiver mDefaultInputEventReceiver;
    private InputMonitor mDefaultInputMonitor;
    private GameModeSettingObserver mGameModeSettingObserver;
    private MiniScreenStatusObserver mMiniScreenStatusObserver;
    private InputEventReceiver mSecondDisplayInputEventReceiver;
    private InputMonitor mSecondDisplayInputMonitor;
    private ShotSettingsObserver mShotSettingsObserver;
    private VivoWMPHook mWMPHook;
    private boolean mThreePointerReady = false;
    private boolean mStartRecord = false;
    private int mPointerTrend = 0;
    private boolean mIsFilterEnable = false;
    private boolean mIsThreePointerEnable = false;
    private boolean mIsMiniScreen = false;
    private boolean mRegister = false;
    private boolean mFingerTooFar = false;
    private boolean mIsThreeFingerChangeDisplayEnabled = false;
    private boolean mDisableDuringGame = false;
    private boolean mTalkBackEnable = false;
    private int[] mFingerStartXs = new int[3];
    private int[] mFingerStartYs = new int[3];
    private int[] mFingerEndXs = new int[3];
    private int[] mFingerEndYs = new int[3];
    private int[] mDeltaXs = new int[3];
    private int[] mDeltaYs = new int[3];
    private Handler mHandler = UiThread.getHandler();
    private Runnable mShotResetRunnable = new Runnable() { // from class: com.android.server.policy.motion.ThreeFingerGesture.1
        @Override // java.lang.Runnable
        public void run() {
            ThreeFingerGesture.this.log("reset screenshot state!");
            ThreeFingerGesture.this.mThreePointerReady = false;
        }
    };

    static {
        DEBUG = SystemProperties.getInt("persist.vivo.shot.debug", 0) == 1;
        THREEPOINTER_TREND = SystemProperties.getInt("persist.vivo.shot.trend", 200);
        CHECK_INTERVAL_DELAY = SystemProperties.getInt("persist.vivo.shot.delay", 500);
        IS_NEW_GESTURE_PRODUCT = SystemProperties.getBoolean("persist.vivo.new_three_finger_gesture", false);
        FINGER_DISTANCE_MAX_THRESHOLD = 1080;
        FINGER_DISTANCE_MAX_THRESHOLD_Y_LANDSCAPE = 150;
        FINGER_DISTANCE_MIN_THRESHOLD = 200;
        FINGER_ROTATION_THRESHOLD = 20;
    }

    public ThreeFingerGesture(Context context, VivoWMPHook vivoWMPHook) {
        this.mContext = context;
        this.mWMPHook = vivoWMPHook;
    }

    public void systemReady() {
        int screenWidth = this.mContext.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = this.mContext.getResources().getDisplayMetrics().heightPixels;
        FINGER_DISTANCE_MAX_THRESHOLD = SystemProperties.getInt("persist.vivo.finger.threshold", Math.min(screenHeight, screenWidth));
        DisplayMetrics displayMetrics = this.mContext.getResources().getDisplayMetrics();
        FINGER_DISTANCE_MAX_THRESHOLD_Y_LANDSCAPE = SystemProperties.getInt("persist.vivo.finger.ythreshold", (int) (displayMetrics.density * 250.0f));
        FINGER_ROTATION_THRESHOLD = SystemProperties.getInt("persist.vivo.finger.rotation", 0);
        log("FINGER_DISTANCE_MAX_THRESHOLD_Y_LANDSCAPE = " + FINGER_DISTANCE_MAX_THRESHOLD_Y_LANDSCAPE);
    }

    public void register() {
        if (this.mRegister) {
            return;
        }
        try {
            registerShotSettingObserver();
            if (this.mDefaultInputMonitor == null) {
                InputManager inputManager = InputManager.getInstance();
                this.mDefaultInputMonitor = inputManager.monitorGestureInput("u" + this.mContext.getUserId() + "_ThreeFingerGesture", 0);
                this.mDefaultInputEventReceiver = new ThreeFingerInputEventReceiver(this.mDefaultInputMonitor.getInputChannel(), this.mHandler.getLooper());
            }
            if (MultiDisplayManager.isMultiDisplay && this.mSecondDisplayInputMonitor == null) {
                InputManager inputManager2 = InputManager.getInstance();
                this.mSecondDisplayInputMonitor = inputManager2.monitorGestureInput("u" + this.mContext.getUserId() + "_ThreeFingerGesture#4096", 4096);
                this.mSecondDisplayInputEventReceiver = new ThreeFingerInputEventReceiver(this.mSecondDisplayInputMonitor.getInputChannel(), this.mHandler.getLooper());
            }
            this.mRegister = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unRegister() {
        if (!this.mRegister) {
            return;
        }
        this.mRegister = false;
        unregisterShotSettingObserver();
    }

    private void registerShotSettingObserver() {
        if (this.mShotSettingsObserver != null) {
            return;
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        try {
            boolean z = true;
            if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "smartkey_shot_switch", 1, -2) != 1) {
                z = false;
            }
            this.mIsThreePointerEnable = z;
            log("registerShotSettingObserver threePointerEnable = " + this.mIsThreePointerEnable);
            this.mShotSettingsObserver = new ShotSettingsObserver(this.mHandler);
            resolver.registerContentObserver(Settings.System.getUriFor("smartkey_shot_switch"), false, this.mShotSettingsObserver, -1);
            readGameModeSettings();
            GameModeSettingObserver gameModeSettingObserver = new GameModeSettingObserver(this.mHandler);
            this.mGameModeSettingObserver = gameModeSettingObserver;
            gameModeSettingObserver.register(resolver);
            readMiniScreenStatus();
            MiniScreenStatusObserver miniScreenStatusObserver = new MiniScreenStatusObserver(this.mHandler);
            this.mMiniScreenStatusObserver = miniScreenStatusObserver;
            miniScreenStatusObserver.register(resolver);
            readAccessibilityServicesStatus();
            AccessibilityServicesObserver accessibilityServicesObserver = new AccessibilityServicesObserver(this.mHandler);
            this.mAccessibilityServicesObserver = accessibilityServicesObserver;
            accessibilityServicesObserver.register(resolver);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void unregisterShotSettingObserver() {
        if (this.mShotSettingsObserver == null) {
            return;
        }
        InputEventReceiver inputEventReceiver = this.mDefaultInputEventReceiver;
        if (inputEventReceiver != null) {
            inputEventReceiver.dispose();
            this.mDefaultInputEventReceiver = null;
        }
        InputMonitor inputMonitor = this.mDefaultInputMonitor;
        if (inputMonitor != null) {
            inputMonitor.dispose();
            this.mDefaultInputMonitor = null;
        }
        if (MultiDisplayManager.isMultiDisplay) {
            InputEventReceiver inputEventReceiver2 = this.mSecondDisplayInputEventReceiver;
            if (inputEventReceiver2 != null) {
                inputEventReceiver2.dispose();
                this.mSecondDisplayInputEventReceiver = null;
            }
            InputMonitor inputMonitor2 = this.mSecondDisplayInputMonitor;
            if (inputMonitor2 != null) {
                inputMonitor2.dispose();
                this.mSecondDisplayInputMonitor = null;
            }
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.unregisterContentObserver(this.mShotSettingsObserver);
        this.mGameModeSettingObserver.unRegister(resolver);
        this.mMiniScreenStatusObserver.unregister(resolver);
        this.mShotSettingsObserver = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onInputEventNotify(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        if (DEBUG) {
            log("processEvent4ThreePointer : action =" + MotionEvent.actionToString(event.getAction()) + " ,pointercount = " + event.getPointerCount());
        }
        int action = event.getAction() & 255;
        if (action == 0) {
            if (DEBUG) {
                log("processDown : " + event);
            }
            this.mThreePointerReady = true;
            this.mStartRecord = false;
            this.mHandler.removeCallbacks(this.mShotResetRunnable);
            this.mHandler.postDelayed(this.mShotResetRunnable, CHECK_INTERVAL_DELAY);
        } else if (action == 1) {
            if (DEBUG) {
                log("processUp : " + event);
            }
            this.mThreePointerReady = false;
            this.mHandler.removeCallbacks(this.mShotResetRunnable);
            if (this.mIsThreePointerEnable && !this.mDisableDuringGame) {
                updateInputFilter(false);
            }
        } else if (action == 2) {
            if (this.mThreePointerReady && !this.mStartRecord && pointerCount == 3) {
                for (int i = 0; i < pointerCount && i < 3; i++) {
                    int locY = (int) event.getY(i);
                    int threshold = SystemProperties.getInt("persist.vivo.shot.threshold", 10);
                    log("break check because fling dis : " + (locY - this.mFingerStartYs[i]));
                    int[] iArr = this.mFingerStartYs;
                    if (iArr[i] - locY > threshold) {
                        this.mStartRecord = true;
                        this.mPointerTrend = -1;
                        return;
                    } else if (locY - iArr[i] > threshold) {
                        this.mStartRecord = true;
                        this.mPointerTrend = 1;
                        return;
                    }
                }
            }
        } else if (action == 5) {
            if (this.mThreePointerReady && pointerCount >= 3) {
                if (pointerCount > 3) {
                    log("reset because down more than three pointers!");
                    this.mThreePointerReady = false;
                } else if (pointerCount == 3) {
                    if (!isFingerTooFar(event)) {
                        if (this.mIsThreePointerEnable && !this.mDisableDuringGame) {
                            updateInputFilter(true, event.getDisplayId());
                        }
                        this.mFingerTooFar = false;
                    } else {
                        this.mFingerTooFar = true;
                    }
                    this.mHandler.removeCallbacks(this.mShotResetRunnable);
                    for (int i2 = 0; i2 < pointerCount && i2 < 3; i2++) {
                        this.mFingerStartXs[i2] = (int) event.getX(i2);
                        this.mFingerStartYs[i2] = (int) event.getY(i2);
                    }
                }
            }
        } else if (action == 6 && this.mThreePointerReady && this.mStartRecord) {
            if (pointerCount != 3) {
                log("reset because up not equals three pointers.");
                this.mThreePointerReady = false;
                return;
            }
            this.mThreePointerReady = false;
            this.mHandler.removeCallbacks(this.mShotResetRunnable);
            boolean tmpY = true;
            boolean isSlideLeftToRight = true;
            boolean isSlideRightToLeft = true;
            for (int i3 = 0; i3 < pointerCount && i3 < 3; i3++) {
                this.mFingerEndXs[i3] = (int) event.getX(i3);
                this.mFingerEndYs[i3] = (int) event.getY(i3);
                this.mDeltaXs[i3] = this.mFingerEndXs[i3] - this.mFingerStartXs[i3];
                int[] iArr2 = this.mDeltaYs;
                iArr2[i3] = this.mFingerEndYs[i3] - this.mFingerStartYs[i3];
                boolean isFlinger = iArr2[i3] * this.mPointerTrend > THREEPOINTER_TREND;
                isSlideLeftToRight &= this.mDeltaXs[i3] > THREEPOINTER_TREND;
                isSlideRightToLeft &= this.mDeltaXs[i3] * (-1) > THREEPOINTER_TREND;
                log(i3 + "~startY : " + this.mFingerStartYs[i3] + " ,endY : " + this.mFingerEndYs[i3] + " ,isFlinger = " + isFlinger);
                log(i3 + "~startX : " + this.mFingerStartXs[i3] + " ,endX : " + this.mFingerEndXs[i3] + " ,isSlideLeftToRight = " + isSlideLeftToRight + " isSlideRightToLeft = " + isSlideRightToLeft);
                tmpY = tmpY && isFlinger;
            }
            if (tmpY) {
                boolean handled = triggerScreenShotOrSplitScreenMode();
                if (handled) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class GameModeSettingObserver extends ContentObserver {
        private final Uri mGameModeDisable;

        public GameModeSettingObserver(Handler handler) {
            super(handler);
            this.mGameModeDisable = Settings.System.getUriFor(ThreeFingerGesture.DISABLE_SCREEN_CAPTURE_ON);
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(this.mGameModeDisable, false, this, -1);
        }

        public void unRegister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            ThreeFingerGesture.this.readGameModeSettings();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readGameModeSettings() {
        try {
            ContentResolver resolver = this.mContext.getContentResolver();
            this.mDisableDuringGame = Settings.System.getIntForUser(resolver, DISABLE_SCREEN_CAPTURE_ON, 0, -2) == 1;
            log("Game Cube 8.1 readGameModeSettings mDisableDuringGame = " + this.mDisableDuringGame);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getArrayListFromLongString(String longString) {
        String[] strTemp;
        List<String> result = new ArrayList<>();
        if (longString != null && (strTemp = longString.split(":")) != null && strTemp.length > 0) {
            for (String str : strTemp) {
                if (!result.contains(str)) {
                    result.add(str);
                }
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readMiniScreenStatus() {
        try {
            ContentResolver resolver = this.mContext.getContentResolver();
            this.mIsMiniScreen = Settings.System.getIntForUser(resolver, "minscreen_state_switch", 0, -2) == 1;
            log("mIsMiniScreen = " + this.mIsMiniScreen);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class MiniScreenStatusObserver extends ContentObserver {
        private final Uri mMiniScreenStatusUri;

        public MiniScreenStatusObserver(Handler handler) {
            super(handler);
            this.mMiniScreenStatusUri = Settings.System.getUriFor("minscreen_state_switch");
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(this.mMiniScreenStatusUri, false, this, -1);
        }

        public void unregister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            ThreeFingerGesture.this.readMiniScreenStatus();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class ShotSettingsObserver extends ContentObserver {
        public ShotSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            try {
                ThreeFingerGesture threeFingerGesture = ThreeFingerGesture.this;
                boolean z = true;
                if (Settings.System.getIntForUser(ThreeFingerGesture.this.mContext.getContentResolver(), "smartkey_shot_switch", 1, -2) != 1) {
                    z = false;
                }
                threeFingerGesture.mIsThreePointerEnable = z;
                ThreeFingerGesture threeFingerGesture2 = ThreeFingerGesture.this;
                threeFingerGesture2.log("onChange threePointerEnable = " + ThreeFingerGesture.this.mIsThreePointerEnable);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class AccessibilityServicesObserver extends ContentObserver {
        private final Uri mEnableAccessibilityServiceUri;

        public AccessibilityServicesObserver(Handler handler) {
            super(handler);
            this.mEnableAccessibilityServiceUri = Settings.Secure.getUriFor("enabled_accessibility_services");
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(this.mEnableAccessibilityServiceUri, false, this, -1);
        }

        public void unregister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            ThreeFingerGesture.this.readAccessibilityServicesStatus();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readAccessibilityServicesStatus() {
        String settingValue = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "enabled_accessibility_services", -2);
        Set<ComponentName> enableAccessibilityComponentSet = new HashSet<>();
        readComponentNamesFromStringLocked(settingValue, enableAccessibilityComponentSet, false);
        if (enableAccessibilityComponentSet.contains(new ComponentName("com.google.android.marvin.talkback", "com.google.android.marvin.talkback.TalkBackService"))) {
            this.mTalkBackEnable = true;
        } else {
            this.mTalkBackEnable = false;
        }
    }

    private void readComponentNamesFromStringLocked(String names, Set<ComponentName> outComponentNames, boolean doMerge) {
        ComponentName enabledService;
        if (!doMerge) {
            outComponentNames.clear();
        }
        if (names != null) {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
            splitter.setString(names);
            while (splitter.hasNext()) {
                String str = splitter.next();
                if (str != null && str.length() > 0 && (enabledService = ComponentName.unflattenFromString(str)) != null) {
                    outComponentNames.add(enabledService);
                }
            }
        }
    }

    private boolean triggerScreenShotOrSplitScreenMode() {
        if (this.mTalkBackEnable) {
            log("ignore when talkback enable");
            return false;
        }
        boolean slideUp = this.mPointerTrend == -1;
        boolean takeScreenShot = IS_NEW_GESTURE_PRODUCT ^ slideUp;
        if (takeScreenShot) {
            if (this.mFingerTooFar) {
                return true;
            }
            log("requestScreenShot.");
            takescreenshot();
        }
        return false;
    }

    private void takescreenshot() {
        if (this.mIsThreePointerEnable && !this.mDisableDuringGame) {
            Intent intent = new Intent("vivo.intent.action.SCREEN_SHOT");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
        }
    }

    private boolean isFingerTooFar(MotionEvent mv) {
        int i;
        float minX = mv.getX(0);
        float maxX = minX;
        float minY = mv.getY(0);
        float maxY = minY;
        int minYIndex = 0;
        int maxYIndex = 0;
        for (int i2 = 0; i2 < 3; i2++) {
            minX = Math.min(mv.getX(i2), minX);
            maxX = Math.max(mv.getX(i2), maxX);
            float tempMinY = minY;
            minY = Math.min(mv.getY(i2), minY);
            if (tempMinY != minY) {
                minYIndex = i2;
            }
            float tempMaxY = maxY;
            maxY = Math.max(mv.getY(i2), maxY);
            if (maxY != tempMaxY) {
                maxYIndex = i2;
            }
        }
        int rotation = getRotationBetweenLines(mv.getX(maxYIndex), mv.getY(maxYIndex), mv.getX(minYIndex), mv.getY(minYIndex));
        boolean isLandScape = this.mContext.getResources().getConfiguration().orientation == 2;
        float deltaX = Math.abs(minX - maxX);
        float deltaY = Math.abs(minY - maxY);
        int deltaXLimit = FINGER_DISTANCE_MAX_THRESHOLD;
        int deltaYLimit = FINGER_DISTANCE_MAX_THRESHOLD;
        if (!this.mIsThreeFingerChangeDisplayEnabled && isLandScape) {
            deltaYLimit = FINGER_DISTANCE_MAX_THRESHOLD_Y_LANDSCAPE;
        }
        if (!this.mIsThreeFingerChangeDisplayEnabled && (rotation < (i = FINGER_ROTATION_THRESHOLD) || rotation > 360 - i)) {
            log("finger rotation too large: " + rotation);
            return true;
        } else if (deltaX > deltaXLimit || deltaY > deltaYLimit) {
            log("finger too far: deltaX:" + deltaX + ", deltaY:" + deltaY + ", deltaXLimit:" + deltaXLimit + ", deltaYLimit:" + deltaYLimit);
            return true;
        } else {
            int i3 = FINGER_DISTANCE_MIN_THRESHOLD;
            if (deltaXLimit < i3 && deltaYLimit < i3) {
                log("finger too near");
                return true;
            }
            return false;
        }
    }

    private static int getRotationBetweenLines(float centerX, float centerY, float xInView, float yInView) {
        double rotation = 0.0d;
        double k1 = (centerY - centerY) / ((2.0f * centerX) - centerX);
        double k2 = (yInView - centerY) / (xInView - centerX);
        double tmpDegree = (Math.atan(Math.abs(k1 - k2) / ((k1 * k2) + 1.0d)) / 3.141592653589793d) * 180.0d;
        if (xInView > centerX && yInView < centerY) {
            rotation = 90.0d - tmpDegree;
        } else if (xInView > centerX && yInView > centerY) {
            rotation = tmpDegree + 90.0d;
        } else if (xInView < centerX && yInView > centerY) {
            rotation = 270.0d - tmpDegree;
        } else if (xInView < centerX && yInView < centerY) {
            rotation = tmpDegree + 270.0d;
        } else if (xInView == centerX && yInView < centerY) {
            rotation = 0.0d;
        } else if (xInView == centerX && yInView > centerY) {
            rotation = 180.0d;
        }
        return (int) rotation;
    }

    public void updateSettings() {
        this.mIsThreePointerEnable = Settings.System.getIntForUser(this.mContext.getContentResolver(), "smartkey_shot_switch", 1, -2) == 1;
        readGameModeSettings();
        readMiniScreenStatus();
    }

    private void updateInputFilter(boolean enable) {
        updateInputFilter(enable, 0);
    }

    private void updateInputFilter(boolean enable, int displayId) {
        InputMonitor inputMonitor;
        log("updateInputFilter enable = " + enable + " ,isFilterEnable = " + this.mIsFilterEnable + " ,isMiniscreen = " + this.mIsMiniScreen + " ,mTalkBackEnable = " + this.mTalkBackEnable);
        if (this.mIsMiniScreen || this.mTalkBackEnable || enable == this.mIsFilterEnable) {
            return;
        }
        this.mIsFilterEnable = enable;
        if (enable && !ThreeFingerConfigManager.getInstance(this.mContext).shouldDisablePilferPointers(this.mWMPHook.mVivoPolicy.getFocusedWindow())) {
            if (displayId == 0) {
                InputMonitor inputMonitor2 = this.mDefaultInputMonitor;
                if (inputMonitor2 != null) {
                    inputMonitor2.pilferPointers();
                }
            } else if (displayId == 4096 && (inputMonitor = this.mSecondDisplayInputMonitor) != null) {
                inputMonitor.pilferPointers();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ThreeFingerInputEventReceiver extends InputEventReceiver {
        ThreeFingerInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        public void onInputEvent(InputEvent event) {
            if (event instanceof MotionEvent) {
                ThreeFingerGesture.this.onInputEventNotify((MotionEvent) event);
            }
            super.onInputEvent(event);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void log(String message) {
        if (VivoPolicyUtil.IS_LOG_OPEN) {
            VLog.d(TAG, message);
        }
    }
}