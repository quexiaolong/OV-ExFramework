package com.android.server.policy;

import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.SQLException;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.view.KeyEvent;
import com.android.internal.policy.KeyInterceptionInfo;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.ProcessList;

/* loaded from: classes.dex */
public final class VivoPolicyHelper {
    public static final int AGAIN_LATER = 50;
    public static final int CONTINUE = 0;
    static final int DOUBLE_TAP_HOME_FLINGER_SERVICE = 1;
    static final int DOUBLE_TAP_HOME_NOTHING = 0;
    private static final String FINGER_QUICK_LAUNCH = "fingerprint_quick_launch";
    public static final int INTERUPT = -1;
    private static final String IS_SUPPOT_FINGER_KEY = "persist.vivo.support.fingerkey";
    static final int LONG_PRESS_HOME_NOTHING = 0;
    static final int LONG_PRESS_HOME_RECENT_TASK = 1;
    public static final int MSG_LAUNCH_HOME_BEHAVIOR = 0;
    public static final int MSG_LONGPRESS_HOME_BEHAVIOR = 2;
    public static final int MSG_PRELOAD_HOME_BEHAVIOR = 1;
    public static final int STATE_DOUBLE_TAP = 16;
    public static final int STATE_LONG_PRESS = 32;
    public static final int STATE_MODE_FINGER = 2;
    public static final int STATE_MODE_KEY = 4;
    public static final int STATE_MODE_REST = 1;
    public static final int STATE_SHORT_PRESS = 8;
    private static final String TAG = "VivoPolicyHelper";
    private boolean isSupportFingerKey;
    final AudioManager mAudioManager;
    private Context mContext;
    private boolean mIsFingerFeedback;
    private boolean mIsHomeFromScreenOff;
    private IVivoAdjustmentPolicy mPolicy;
    private StatusBarManager mStatusBarManager;
    private boolean sDebugOriginal;
    private static final int[] WINDOW_TYPES_WHERE_HOME_DOESNT_WORK = {2003, 2010};
    public static int FINGER_PRINT_DELAY = ProcessList.HOME_APP_ADJ;
    public static int FINGER_MISOPERATION_INTERVAL = ProcessList.PREVIOUS_APP_ADJ;
    public static int FINGER_LONGPRESS_DURL = 1000;
    public static int FINGER_LONGPRESS_DELAY = 200;
    public static int HOME_DOUBLE_TAP_TIMEOUT = 200;
    private final String SERVICE_PKGNAME = "com.vivo.quickpay";
    private final String SERVICE_CLSNAME = "com.vivo.quickpay.fingerkey.QuickPayService";
    private boolean mFingerPrint = false;
    private boolean mQuickLaunchOn = false;
    private boolean mHomeConsumed = false;
    private boolean mHomeDoubleTapPending = false;
    private boolean mHomeDoubleTapPost = false;
    private int mLongPressOnHomeBehavior = 1;
    private int mDoubleTapOnHomeBehavior = 1;
    private int mProcState = 1;
    private final Runnable mFingerLongPressRunnable = new Runnable() { // from class: com.android.server.policy.VivoPolicyHelper.1
        @Override // java.lang.Runnable
        public void run() {
            VLog.d(VivoPolicyHelper.TAG, "vFingerLongPress doSomething here(" + VivoPolicyHelper.FINGER_LONGPRESS_DELAY + ")");
            VivoPolicyHelper.this.handleLongPressOnHome(305);
        }
    };
    private Handler mHandler = new Handler();
    private long sLastTime = -1;
    private final Runnable mHomeDoubleTapTimeoutRunnable = new Runnable() { // from class: com.android.server.policy.VivoPolicyHelper.2
        @Override // java.lang.Runnable
        public void run() {
            VLog.d(VivoPolicyHelper.TAG, "vHomeClick doSomething here(" + VivoPolicyHelper.this.mHomeDoubleTapPending + ")");
            if (VivoPolicyHelper.this.mHomeDoubleTapPending) {
                if (VivoPolicyHelper.this.mHomeDoubleTapPost) {
                    VivoPolicyHelper.this.handleShortPressOnHome(3);
                }
                VivoPolicyHelper.this.mHomeDoubleTapPost = true;
                VivoPolicyHelper.this.mHomeDoubleTapPending = false;
            }
        }
    };
    private long sFingerDownTime = -1;

    public VivoPolicyHelper(Context context, IVivoAdjustmentPolicy policy) {
        this.sDebugOriginal = false;
        this.isSupportFingerKey = false;
        this.mPolicy = policy;
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        this.isSupportFingerKey = SystemProperties.getBoolean(IS_SUPPOT_FINGER_KEY, false);
        registerQuickLaunchObserver();
        this.sDebugOriginal = SystemProperties.getBoolean("persist.vivo.debug.origin", false);
        FINGER_PRINT_DELAY = SystemProperties.getInt("persist.vivo.finger.delay", (int) ProcessList.HOME_APP_ADJ);
        FINGER_LONGPRESS_DELAY = SystemProperties.getInt("persist.vivo.longpress.delay", 200);
        HOME_DOUBLE_TAP_TIMEOUT = SystemProperties.getInt("persist.vivo.doubletap.timeout", 200);
        FINGER_MISOPERATION_INTERVAL = SystemProperties.getInt("persist.vivo.misopt.interval", (int) ProcessList.PREVIOUS_APP_ADJ);
    }

    private int getHomekeyState() {
        return 0;
    }

    private boolean checkTimeInterval() {
        if (SystemClock.elapsedRealtime() - this.sLastTime < FINGER_MISOPERATION_INTERVAL) {
            return true;
        }
        return false;
    }

    private boolean checkFingerTimeInterval() {
        if (SystemClock.elapsedRealtime() - this.sFingerDownTime < FINGER_LONGPRESS_DURL) {
            return true;
        }
        return false;
    }

    public boolean isFingerFeedback() {
        VLog.d(TAG, "isFingerFeedback = " + this.mIsFingerFeedback);
        return this.mIsFingerFeedback;
    }

    private boolean isUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleShortPressOnHome(int keyCode) {
        if (this.sLastTime != -1 && checkTimeInterval()) {
            VLog.w(TAG, "Don't post shortPressOnHome overdue timeinterval.!");
            return;
        }
        if (this.sLastTime != -1) {
            this.sLastTime = -1L;
        }
        if (keyCode == 305) {
            this.mProcState |= 2;
        } else {
            this.mProcState |= 8;
        }
        if (this.mFingerPrint) {
            VLog.i(TAG, "Ignoring HOMETAP, unlock from fingerprint.");
            this.mFingerPrint = false;
            resetDoubleTapState();
        } else if (this.mIsHomeFromScreenOff) {
            VLog.i(TAG, "Ignoring HOMETAP, launch home from screen timeout.");
            this.mIsHomeFromScreenOff = false;
        } else {
            if (this.mIsFingerFeedback) {
                performVirtualKeyAudioFeedback();
                this.mPolicy.performHapticFeedback(1, false, true);
            }
            resetDoubleTapState();
            this.mPolicy.doCustomKeyHandler(keyCode, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLongPressOnHome(int keyCode) {
        VLog.d(TAG, "handleLongPressOnHome");
        if (keyCode == 305) {
            this.mProcState |= 2;
        } else {
            this.mProcState |= 32;
        }
        this.mPolicy.doCustomKeyHandler(keyCode, 2);
    }

    private void handleDoubleTapOnHome() {
        this.mProcState |= 16;
        this.mStatusBarManager.collapsePanels();
        this.sLastTime = SystemClock.elapsedRealtime();
        resetDoubleTapState();
        if (isUserSetupComplete()) {
            startAppService();
        } else {
            VLog.d(TAG, "Not starting doubletap because user setup is in progress!");
        }
    }

    private void startAppService() {
        ComponentName cmp = new ComponentName("com.vivo.quickpay", "com.vivo.quickpay.fingerkey.QuickPayService");
        Intent service = new Intent();
        service.setComponent(cmp);
        this.mContext.startService(service);
    }

    private void preloadRecentApps() {
    }

    private void resetDoubleTapState() {
        VLog.d(TAG, "resetDoubleTapState.");
        this.mHomeDoubleTapPost = true;
        this.mHomeDoubleTapPending = false;
        this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
    }

    private TelecomManager getTelecommService() {
        return (TelecomManager) this.mContext.getSystemService("telecom");
    }

    private void registerQuickLaunchObserver() {
        ContentResolver resolver = this.mContext.getContentResolver();
        if (!this.isSupportFingerKey) {
            VLog.d(TAG, "registerQuickLaunchObserver return here.");
            return;
        }
        boolean z = true;
        try {
            if (Settings.System.getInt(resolver, FINGER_QUICK_LAUNCH, 1) != 1) {
                z = false;
            }
            this.mQuickLaunchOn = z;
            VLog.d(TAG, "registerQuickLaunchObserver ret = " + this.mQuickLaunchOn);
            resolver.registerContentObserver(Settings.System.getUriFor(FINGER_QUICK_LAUNCH), false, new QuickLaunchObserver(this.mHandler));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void performVirtualKeyAudioFeedback() {
        AudioManager audioManager = this.mAudioManager;
        if (audioManager == null) {
            VLog.w(TAG, "Couldn't get audio manager");
        } else {
            audioManager.playSoundEffect(0);
        }
    }

    public void reportFingerPrint() {
        VLog.d(TAG, "reportFingerPrint.");
        this.mFingerPrint = true;
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.policy.VivoPolicyHelper.3
            @Override // java.lang.Runnable
            public void run() {
                VLog.d(VivoPolicyHelper.TAG, "reportFingerPrint reset to false..");
                VivoPolicyHelper.this.mFingerPrint = false;
            }
        }, FINGER_PRINT_DELAY);
    }

    public int interceptHomeKeyForVivo(KeyInterceptionInfo keyInterceptionInfo, KeyEvent event, int policyFlags, boolean keyguardOn) {
        boolean isLongPress;
        boolean down;
        if (this.sDebugOriginal) {
            VLog.d(TAG, "Debug original here.");
            return -100;
        }
        boolean interactive = (policyFlags & 536870912) != 0;
        int keyCode = event.getKeyCode();
        int repeatCount = event.getRepeatCount();
        event.getMetaState();
        event.getFlags();
        boolean down2 = event.getAction() == 0;
        boolean canceled = event.isCanceled();
        event.isLongPress();
        if (keyCode != 3) {
            if (keyCode == 305) {
                if (!down2 || repeatCount != 0) {
                    down = down2;
                } else {
                    VLog.i(TAG, "state finger reset here.");
                    this.mProcState = 1;
                    down = down2;
                    this.sFingerDownTime = SystemClock.elapsedRealtime();
                    this.mIsFingerFeedback = true;
                    this.mIsHomeFromScreenOff = false;
                }
                if (!down) {
                    if (!canceled) {
                        if ((this.mProcState & (-2)) != 0) {
                            VLog.i(TAG, "Ignoring FINGER_UP, event was handled.");
                            return -1;
                        } else if (!checkFingerTimeInterval()) {
                            VLog.i(TAG, "Ignoring FINGER_UP, event was overtime.");
                            return -1;
                        } else {
                            TelecomManager telecomManager = getTelecommService();
                            if (telecomManager != null && telecomManager.isRinging() && !SystemProperties.getBoolean("ril.incoming.window", false)) {
                                VLog.i(TAG, "Ignoring HOME; there's a ringing incoming call.");
                                return -1;
                            }
                            if (keyInterceptionInfo != null) {
                                int typeCount = WINDOW_TYPES_WHERE_HOME_DOESNT_WORK.length;
                                int type = keyInterceptionInfo.layoutParamsType;
                                for (int i = 0; i < typeCount; i++) {
                                    if (type == WINDOW_TYPES_WHERE_HOME_DOESNT_WORK[i]) {
                                        VLog.i(TAG, "Do nothing, dropping home key event.");
                                        return -1;
                                    }
                                }
                                int i2 = keyInterceptionInfo.layoutParamsPrivateFlags;
                                if ((i2 & VivoPolicyConstant.PRIVATE_FLAG_HOMEKEY_DISPATCHED) != 0) {
                                    VLog.i(TAG, "Dispatching home key to app window:" + keyInterceptionInfo.windowTitle);
                                    if (!this.mIsFingerFeedback) {
                                        return 0;
                                    }
                                    performVirtualKeyAudioFeedback();
                                    this.mPolicy.performHapticFeedback(1, false, true);
                                    return 0;
                                }
                            }
                            handleShortPressOnHome(keyCode);
                            return -1;
                        }
                    }
                    VLog.i(TAG, "Ignoring FINGER; event canceled.");
                    return -1;
                }
                if (keyInterceptionInfo != null) {
                    int type2 = keyInterceptionInfo.layoutParamsType;
                    if (type2 == 2009) {
                        VLog.i(TAG, "Is keyguard, so give it the key.");
                        return 0;
                    }
                    int typeCount2 = WINDOW_TYPES_WHERE_HOME_DOESNT_WORK.length;
                    for (int i3 = 0; i3 < typeCount2; i3++) {
                        if (type2 == WINDOW_TYPES_WHERE_HOME_DOESNT_WORK[i3]) {
                            VLog.i(TAG, "Don't do anything, but also don't pass it to the app.");
                            return -1;
                        }
                    }
                }
                if (repeatCount == 0) {
                    if (this.mLongPressOnHomeBehavior == 1) {
                        preloadRecentApps();
                        return -1;
                    }
                    return -1;
                }
                event.getFlags();
                return -1;
            }
            return -1;
        }
        boolean down3 = down2;
        if (down3 && repeatCount == 0) {
            VLog.i(TAG, "state home reset here, homekey down is true ,keyguardOn = " + keyguardOn + " ,interactive = " + interactive);
            this.mProcState = 1;
            this.mIsFingerFeedback = false;
            this.mIsHomeFromScreenOff = false;
            boolean virtualKey = event.getDeviceId() == -1;
            if (virtualKey || !this.isSupportFingerKey) {
                VLog.d(TAG, "perform Feedback.");
                performVirtualKeyAudioFeedback();
                isLongPress = true;
                this.mPolicy.performHapticFeedback(1, false, true);
            } else {
                isLongPress = true;
            }
            if (!interactive && !keyguardOn) {
                this.mIsHomeFromScreenOff = isLongPress;
            }
        }
        if (!down3) {
            if (!canceled) {
                if ((this.mProcState & (-2) & (-3)) != 0) {
                    VLog.i(TAG, "Ignoring HOME_UP, event was handled.");
                    return -1;
                }
                TelecomManager telecomManager2 = getTelecommService();
                if (telecomManager2 != null && telecomManager2.isRinging() && !SystemProperties.getBoolean("ril.incoming.window", false)) {
                    VLog.i(TAG, "Ignoring HOME; there's a ringing incoming call.");
                    return -1;
                }
                if (keyInterceptionInfo != null) {
                    int typeCount3 = WINDOW_TYPES_WHERE_HOME_DOESNT_WORK.length;
                    int type3 = keyInterceptionInfo.layoutParamsType;
                    for (int i4 = 0; i4 < typeCount3; i4++) {
                        if (type3 == WINDOW_TYPES_WHERE_HOME_DOESNT_WORK[i4]) {
                            VLog.i(TAG, "Do nothing, dropping home key event, win: " + keyInterceptionInfo.windowTitle);
                            return -1;
                        }
                    }
                }
                if (this.mQuickLaunchOn && this.mDoubleTapOnHomeBehavior != 0) {
                    if (keyInterceptionInfo == null || (keyInterceptionInfo.layoutParamsPrivateFlags & VivoPolicyConstant.PRIVATE_FLAG_HOMEKEY_DISPATCHED) == 0) {
                        this.mHomeDoubleTapPending = true;
                        this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                        this.mHandler.postDelayed(this.mHomeDoubleTapTimeoutRunnable, HOME_DOUBLE_TAP_TIMEOUT);
                        VLog.i(TAG, "Ignoring HOME; a double-tap is possible.");
                        return -1;
                    } else if ((keyInterceptionInfo.layoutParamsPrivateFlags & VivoPolicyConstant.PRIVATE_FLAG_HOMEKEY_DOUBLE_CLICK) != 0) {
                        this.mHomeDoubleTapPost = false;
                        this.mHomeDoubleTapPending = true;
                        this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                        this.mHandler.postDelayed(this.mHomeDoubleTapTimeoutRunnable, HOME_DOUBLE_TAP_TIMEOUT);
                        VLog.i(TAG, "Not luanch home here, Dispatching home key to app window:" + keyInterceptionInfo.windowTitle);
                        return 0;
                    } else {
                        VLog.i(TAG, "Dispatching home key to app window:" + keyInterceptionInfo.windowTitle);
                        return 0;
                    }
                } else if (keyInterceptionInfo != null && (keyInterceptionInfo.layoutParamsPrivateFlags & VivoPolicyConstant.PRIVATE_FLAG_HOMEKEY_DISPATCHED) != 0) {
                    VLog.i(TAG, "Dispatching home key to app window:" + keyInterceptionInfo.windowTitle);
                    return 0;
                } else {
                    handleShortPressOnHome(keyCode);
                    return -1;
                }
            }
            VLog.i(TAG, "Ignoring HOME; event canceled.");
            return -1;
        }
        if (keyInterceptionInfo != null) {
            int type4 = keyInterceptionInfo.layoutParamsType;
            if (type4 == 2009) {
                if (!this.mHomeDoubleTapPending) {
                    VLog.i(TAG, "Is keyguard, so give it the key.");
                    return 0;
                }
                VLog.i(TAG, "A double tap on keyguard.");
            }
            int typeCount4 = WINDOW_TYPES_WHERE_HOME_DOESNT_WORK.length;
            for (int i5 = 0; i5 < typeCount4; i5++) {
                if (type4 == WINDOW_TYPES_WHERE_HOME_DOESNT_WORK[i5]) {
                    VLog.i(TAG, "Don't do anything, but also don't pass it to the app.");
                    return -1;
                }
            }
        }
        if (repeatCount == 0) {
            if (this.mHomeDoubleTapPending) {
                this.mHomeDoubleTapPending = false;
                handleDoubleTapOnHome();
                return -1;
            } else if (this.mLongPressOnHomeBehavior == 1) {
                preloadRecentApps();
                return -1;
            } else {
                return -1;
            }
        } else if ((event.getFlags() & 128) != 0) {
            handleLongPressOnHome(keyCode);
            return -1;
        } else {
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class QuickLaunchObserver extends ContentObserver {
        public QuickLaunchObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            try {
                VivoPolicyHelper.this.mQuickLaunchOn = Settings.System.getInt(VivoPolicyHelper.this.mContext.getContentResolver(), VivoPolicyHelper.FINGER_QUICK_LAUNCH, 0) == 1;
                VLog.d(VivoPolicyHelper.TAG, "QuickLaunchObserver ret = " + VivoPolicyHelper.this.mQuickLaunchOn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}