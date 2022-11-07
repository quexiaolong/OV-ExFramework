package com.android.server.power;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.multidisplay.MultiDisplayManagerInternal;
import android.os.Build;
import android.os.FtBuild;
import android.os.PowerManagerInternal;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.util.EventLog;
import com.android.server.LocalServices;
import com.android.server.policy.WindowManagerPolicy;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoNotifierImpl implements IVivoNotifier {
    private static final boolean DEBUG;
    private static boolean DEBUG_POWER = false;
    public static final int DISPLAY_ID_PRIMARY = 0;
    private static final boolean IS_ENG = Build.TYPE.equals("eng");
    private static final boolean IS_LOG_CTRL_OPEN;
    private static final boolean IS_POWER_LOG_CTRL_OPEN;
    private static final String KEY_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    private static final String KEY_VIVO_POWER_LOG_CTRL = "persist.sys.power.log.ctrl";
    private static final String TAG = "VivoPowerManagerNotifier";
    private final Context mContext;
    DisplayManager mDisplayManager;
    private DisplayManagerInternal mDisplayManagerInternal;
    private FaceWakeHandler mFaceWake;
    private boolean mIsAwake = true;
    private final MultiDisplayManagerInternal mMultiDisplayManagerInternal;
    private PowerManagerInternal mPowerManagerInternal;

    static {
        boolean equals = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
        IS_LOG_CTRL_OPEN = equals;
        DEBUG = equals || IS_ENG;
        boolean equals2 = SystemProperties.get(KEY_VIVO_POWER_LOG_CTRL, "no").equals("yes");
        IS_POWER_LOG_CTRL_OPEN = equals2;
        DEBUG_POWER = equals2;
    }

    public VivoNotifierImpl(Context context) {
        FaceWakeThread faceWakeThread = new FaceWakeThread("facewake", context);
        faceWakeThread.start();
        this.mFaceWake = new FaceWakeHandler(context, faceWakeThread.getLooper(), faceWakeThread);
        this.mContext = context;
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        this.mMultiDisplayManagerInternal = (MultiDisplayManagerInternal) LocalServices.getService(MultiDisplayManagerInternal.class);
    }

    public boolean setDebug() {
        return DEBUG;
    }

    public void handleWakeLock(boolean Acquire, int flags) {
        this.mFaceWake.handleWakeLock(Acquire, flags);
    }

    public void stopFaceWake() {
        this.mFaceWake.stopFaceWake();
    }

    public void handleFaceWake() {
        this.mFaceWake.handleFaceWake();
    }

    public void startedWakingUpForReason(int interactiveChangeReason, WindowManagerPolicy policy) {
        EventLog.writeEvent(2728, 1, 0, 0, 0);
        if (DEBUG) {
            VSlog.d(TAG, "handleEarlyInteractiveChange: mPolicy.startedWakingUp");
        }
        policy.startedWakingUpForReason(interactiveChangeReason);
    }

    public void writeLateInteractiveChangeEvent(int why) {
        EventLog.writeEvent(2728, 0, Integer.valueOf(why), 0, 0);
    }

    public void logUpdatePendingBroadcastLocked(boolean broadcastInProgress, int pendingInteractiveState, boolean pendingGoToSleepBroadcast, int broadcastedInteractiveState) {
        if (DEBUG) {
            VSlog.d(TAG, "updatePendingBroadcastLocked mBroadcastInProgress = " + broadcastInProgress + ", mPendingInteractiveState = " + pendingInteractiveState + ", mPendingGoToSleepBroadcast = " + pendingGoToSleepBroadcast + ", mBroadcastedInteractiveState = " + broadcastedInteractiveState);
        }
    }

    public void logFinishPendingBroadcastLocked() {
        if (DEBUG) {
            VSlog.d(TAG, "finishPendingBroadcastLocked");
        }
    }

    public void logSendNextBroadcast(int broadcastedInteractiveState, int pendingInteractiveState, boolean pendingWakeUpBroadcast, boolean pendingGoToSleepBroadcast) {
        if (DEBUG) {
            VSlog.d(TAG, "sendNextBroadcast mBroadcastedInteractiveState = " + broadcastedInteractiveState + ", mPendingInteractiveState = " + pendingInteractiveState + ", mPendingWakeUpBroadcast = " + pendingWakeUpBroadcast + ", mPendingGoToSleepBroadcast = " + pendingGoToSleepBroadcast);
        }
    }

    public void logWakeUpBroadcastDone() {
        if (DEBUG) {
            VSlog.d(TAG, "mWakeUpBroadcastDone - sendNextBroadcast");
        }
    }

    public void logGoToSleepBroadcastDone() {
        if (DEBUG) {
            VSlog.d(TAG, "mGoToSleepBroadcastDone - sendNextBroadcast");
        }
    }

    public boolean stopPlayChargingStartedFeedback() {
        if (!"vos".equals(FtBuild.getOsName()) || SystemProperties.get("persist.sys.usb.notify.sound", "1").equals("0")) {
            return true;
        }
        return false;
    }

    public void logWakeLockAcquired(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource) {
        if (DEBUG_POWER || (DEBUG && flags != 1)) {
            VSlog.d(TAG, "onWakeLockAcquired: flags=" + flags + ", tag=\"" + tag + "\", packageName=" + packageName + ", ownerUid=" + ownerUid + ", ownerPid=" + ownerPid + ", workSource=" + workSource);
        }
    }

    public void logWakeLockReleased(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource) {
        if (DEBUG_POWER || (DEBUG && flags != 1)) {
            VSlog.d(TAG, "onWakeLockReleased: flags=" + flags + ", tag=\"" + tag + "\", packageName=" + packageName + ", ownerUid=" + ownerUid + ", ownerPid=" + ownerPid + ", workSource=" + workSource);
        }
    }

    public void logWakeLockChanging(int newFlags, String newTag, String newPackageName, int newOwnerUid, int newOwnerPid, WorkSource newWorkSource) {
        if (DEBUG_POWER || (DEBUG && newFlags != 1)) {
            VSlog.d(TAG, "onWakeLockChanging: flags=" + newFlags + ", tag=\"" + newTag + "\", packageName=" + newPackageName + ", ownerUid=" + newOwnerUid + ", ownerPid=" + newOwnerPid + ", workSource=" + newWorkSource);
        }
    }

    public void screenFadingOn(final boolean isOn, final int code, final WindowManagerPolicy policy) {
        if (this.mPowerManagerInternal == null) {
            this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        }
        PowerManagerInternal powerManagerInternal = this.mPowerManagerInternal;
        if (powerManagerInternal != null && powerManagerInternal.getPowerThreadHandler() != null) {
            this.mPowerManagerInternal.getPowerThreadHandler().post(new Runnable() { // from class: com.android.server.power.VivoNotifierImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    WindowManagerPolicy windowManagerPolicy = policy;
                    if (windowManagerPolicy != null) {
                        windowManagerPolicy.screenFadingOn(isOn, code);
                    } else {
                        VSlog.d(VivoNotifierImpl.TAG, "screenFadingOn policy = null !!!");
                    }
                }
            });
        }
    }

    public void screenFadingOnIfNeeded(final WindowManagerPolicy policy) {
        if (this.mDisplayManagerInternal == null) {
            this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        }
        DisplayManagerInternal displayManagerInternal = this.mDisplayManagerInternal;
        if (displayManagerInternal != null && displayManagerInternal.isColorFadeOnAnimationStarted()) {
            if (this.mPowerManagerInternal == null) {
                this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
            }
            PowerManagerInternal powerManagerInternal = this.mPowerManagerInternal;
            if (powerManagerInternal != null && powerManagerInternal.getPowerThreadHandler() != null) {
                this.mPowerManagerInternal.getPowerThreadHandler().post(new Runnable() { // from class: com.android.server.power.VivoNotifierImpl.2
                    @Override // java.lang.Runnable
                    public void run() {
                        if (policy != null) {
                            VSlog.d(VivoNotifierImpl.TAG, "The screen is waking up before finish the last fading-on animation, change too fast, fade true,0");
                            policy.screenFadingOn(true, 0);
                            return;
                        }
                        VSlog.d(VivoNotifierImpl.TAG, "screenFadingOn policy = null !!!");
                    }
                });
            }
        }
    }

    public void dummy() {
    }
}