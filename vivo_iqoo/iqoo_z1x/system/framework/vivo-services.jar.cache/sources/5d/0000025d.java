package com.android.server.input;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.multidisplay.MultiDisplayManager;
import android.os.Binder;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.InputEvent;
import android.view.MotionEvent;
import com.android.server.input.InputManagerService;
import com.android.server.policy.motion.VivoInputUsageStatsListener;
import com.android.server.wm.VivoAppShareManager;
import com.android.server.wm.VivoEasyShareManager;
import com.vivo.appshare.AppShareConfig;
import com.vivo.services.autorecover.SystemAutoRecoverService;
import com.vivo.services.touchscreen.TouchScreenService;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoInputManagerServiceImpl implements IVivoInputManagerService {
    private static final int INJECTION_TIMEOUT_MILLIS = 30000;
    private static final int INPUT_EVENT_INJECTION_FAILED = 2;
    private static final int INPUT_EVENT_INJECTION_PERMISSION_DENIED = 1;
    private static final int INPUT_EVENT_INJECTION_SUCCEEDED = 0;
    private static final int INPUT_EVENT_INJECTION_TIMED_OUT = 3;
    static final String TAG = "VivoInputManager";
    private Context mContext;
    private DisplayManager mDisplayManager;
    private Handler mHandler;
    private InputManagerService mInputManagerService;
    private final long mPtr;
    private VivoInputUsageStatsListener mVivoInputUsageStatsListener;
    private InputManagerService.WindowManagerCallbacks mWindowManagerCallbacks;
    private boolean mBackScreenGamepadEnabled = false;
    private int mFocusedDisplayId = 0;
    private VivoAppShareManager mVivoAppShareManager = VivoAppShareManager.getInstance();
    private VivoEasyShareManager mVivoEasyShareManager = VivoEasyShareManager.getInstance();

    public VivoInputManagerServiceImpl(Context context, Handler handler, InputManagerService inputManagerService, long ptr) {
        this.mContext = context;
        this.mHandler = handler;
        this.mInputManagerService = inputManagerService;
        this.mPtr = ptr;
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
    }

    public void registerSettingObserver() {
        registerBackScreenGamepadSettingObserver();
        registerIotGamepadSettingObserver();
    }

    public void systemRunning() {
        VivoInputUsageStatsListener vivoInputUsageStatsListener = new VivoInputUsageStatsListener(this.mContext);
        this.mVivoInputUsageStatsListener = vivoInputUsageStatsListener;
        vivoInputUsageStatsListener.onSystemReady();
    }

    private void registerBackScreenGamepadSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(TouchScreenService.VIRTUAL_GAMEKEY), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.VivoInputManagerServiceImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                try {
                    VivoInputManagerServiceImpl.this.mBackScreenGamepadEnabled = Settings.System.getIntForUser(VivoInputManagerServiceImpl.this.mContext.getContentResolver(), TouchScreenService.VIRTUAL_GAMEKEY, 0, -2) == 1;
                    Log.d("ForceSplitTouch", "mBackScreenGamepadEnabled = " + VivoInputManagerServiceImpl.this.mBackScreenGamepadEnabled);
                } catch (Exception e) {
                }
            }
        }, -1);
    }

    public boolean shouldForceSplitTouch(String packageName, String title) {
        return false;
    }

    private void registerIotGamepadSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("vivo_iot_gamepad"), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.VivoInputManagerServiceImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                try {
                    boolean iotGamePadEnable = Settings.Secure.getIntForUser(VivoInputManagerServiceImpl.this.mContext.getContentResolver(), "vivo_iot_gamepad", 0, -2) == 1;
                    Log.d(VivoInputManagerServiceImpl.TAG, "iotGamePadEnable = " + iotGamePadEnable);
                    VivoInputManagerServiceImpl.this.setIotGamepadEnabled(iotGamePadEnable);
                } catch (Exception e) {
                }
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setIotGamepadEnabled(boolean enabled) {
        InputManagerService.nativeSetIotGamepadEnabled(this.mPtr, enabled);
    }

    public boolean injectInputEventInternal(MotionEvent event, int mode, String ignoredWindow) {
        String ignoredWindow2;
        if (ignoredWindow != null) {
            ignoredWindow2 = ignoredWindow;
        } else {
            ignoredWindow2 = SystemAutoRecoverService.WindowItem.ALL_WINDOW_TAG;
        }
        if (event != null) {
            if (mode != 0 && mode != 2 && mode != 1) {
                throw new IllegalArgumentException("mode is invalid");
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            adjustForMultiDisplay(event);
            try {
                int result = InputManagerService.nativeInjectMotionEvent(this.mPtr, event, pid, uid, mode, (int) INJECTION_TIMEOUT_MILLIS, (int) Dataspace.RANGE_FULL, ignoredWindow2);
                if (result != 0) {
                    if (result == 1) {
                        VSlog.w(TAG, "Input event injection from pid " + pid + " permission denied.");
                        throw new SecurityException("Injecting to another application requires INJECT_EVENTS permission");
                    } else if (result == 3) {
                        VSlog.w(TAG, "Input event injection from pid " + pid + " timed out.");
                        return false;
                    } else {
                        VSlog.w(TAG, "Input event injection from pid " + pid + " failed.");
                        return false;
                    }
                }
                return true;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        throw new IllegalArgumentException("event must not be null");
    }

    public void adjustInputMonitor(float scale, int xOffset, int yOffset) {
        InputManagerService.adjustInputMonitor(this.mPtr, scale, xOffset, yOffset);
    }

    public boolean shouldDisablePilferPointers(String opPackageName) {
        InputManagerService.WindowManagerCallbacks windowManagerCallbacks = this.mWindowManagerCallbacks;
        if (windowManagerCallbacks != null) {
            return windowManagerCallbacks.shouldDisablePilferPointers(opPackageName);
        }
        return false;
    }

    public void setWindowManagerCallbacks(InputManagerService.WindowManagerCallbacks callbacks) {
        this.mWindowManagerCallbacks = callbacks;
    }

    public boolean shouldBlockMotionEventForAppShare(InputEvent event) {
        if (AppShareConfig.SUPPROT_APPSHARE && (event instanceof MotionEvent) && MultiDisplayManager.isAppShareDisplayId(event.getDisplayId())) {
            MotionEvent motionEvent = (MotionEvent) event;
            if (this.mVivoAppShareManager.isLeftButtonRegion(motionEvent.getX(), motionEvent.getY())) {
                this.mVivoAppShareManager.notifyAppShareMoveTaskToBackFromInputManager();
                return true;
            }
            return false;
        }
        return false;
    }

    public void setFocusedDisplay(int displayId) {
        if (MultiDisplayManager.isMultiDisplay) {
            this.mFocusedDisplayId = displayId;
        }
    }

    public void adjustForMultiDisplay(InputEvent event) {
        int i;
        if (MultiDisplayManager.isMultiDisplay && event.getDisplayId() == 0 && (i = this.mFocusedDisplayId) == 4096) {
            event.setDisplayId(i);
        }
    }

    public boolean shouldInterceptMotionEventByPcShare(MotionEvent event) {
        DisplayManager displayManager;
        Display display;
        if (!this.mVivoEasyShareManager.isInPcSharing() && DisplayManagerGlobal.getInstance().isForceBrightnessOff()) {
            VSlog.d(TAG, "light on the screen");
            DisplayManager displayManager2 = this.mDisplayManager;
            if (displayManager2 != null) {
                displayManager2.setForceDisplayBrightnessOff(false, "wakeup by touch");
                return true;
            }
        }
        if (VivoEasyShareManager.SUPPORT_PCSHARE && this.mVivoEasyShareManager.isInPcSharing() && (displayManager = this.mDisplayManager) != null && (display = displayManager.getDisplay(0)) != null && display.getState() == 2 && DisplayManagerGlobal.getInstance().isForceBrightnessOff()) {
            VSlog.d(TAG, "shouldInterceptMotionEventByPcShare: need intercept event by pc share");
            this.mVivoEasyShareManager.notifyInterceptMotionEventInForceBrightnessOffState(event);
            return true;
        }
        return false;
    }

    public void notifyInputFrozenTimeout() {
        InputManagerService.WindowManagerCallbacks windowManagerCallbacks = this.mWindowManagerCallbacks;
        if (windowManagerCallbacks != null) {
            windowManagerCallbacks.notifyInputFrozenTimeout();
        }
    }

    public int getScreenOperationCountDaily() {
        VivoInputUsageStatsListener vivoInputUsageStatsListener = this.mVivoInputUsageStatsListener;
        if (vivoInputUsageStatsListener != null) {
            return vivoInputUsageStatsListener.getScreenOperationCountDaily();
        }
        return 0;
    }
}