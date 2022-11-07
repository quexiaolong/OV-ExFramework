package com.android.server.power;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.multidisplay.MultiDisplayManager;
import android.multidisplay.MultiDisplayManagerInternal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.DisplayInfo;
import com.android.server.LocalServices;
import com.vivo.services.autorecover.SystemAutoRecoverManagerInternal;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoShutdownThreadImpl implements IVivoShutdownThread {
    private static final int BACKLIGHT_STATE_POLL_SLEEP_MSEC = 50;
    private static final int MAX_BLIGHT_OFF_POLL_TIME = 1000;
    private static final String TAG = "VivoShutdownThread";
    private static Handler mDatabaseHandler;
    private static HandlerThread mDatabaseHandlerThread;
    private static MultiDisplayManagerInternal mMultiDisplayManagerInternal;
    private static String savingPasswordValue = "com.bbk.reboot.notify.lock";
    private boolean mBlightOff;
    private ShutdownThread mShutdownThread;

    public VivoShutdownThreadImpl(ShutdownThread shutdownThread) {
        this.mShutdownThread = shutdownThread;
    }

    public void runPre() {
        SystemProperties.set("persist.sys.shutdowndone", "1");
    }

    public boolean shutdownInnerPre() {
        if (SystemProperties.get("sys.bsptest.monkey", "0").equals("1")) {
            VSlog.d(TAG, "forbit shutdownInner while monkey is running");
            return true;
        }
        return false;
    }

    public void skipShowShutdownDialog() {
    }

    public void showShutdownPic(String shutdownReason, Context context) {
        PowerManager pm = (PowerManager) context.getSystemService("power");
        if ((!"recovery-update".equals(shutdownReason) && !"silent".equals(shutdownReason)) || pm.isInteractive()) {
            SystemProperties.set("sys.boot.animshutdown", "0");
            VSlog.d(TAG, "enter showShutdownAnimation !");
            SystemProperties.set("service.bootanim.exit", "0");
            SystemProperties.set("ctl.start", "bootanim");
            VSlog.d(TAG, " leave showShutdownAnimation !");
            return;
        }
        SystemProperties.set("sys.boot.animshutdown", "1");
        VSlog.d(TAG, "The shutdownReason is recovery-update and the screen isn't on, skip shutdown animation here!");
    }

    public void waitForBroadcastAndAnimationDoneLocked(long endTime) {
        synchronized (this.mShutdownThread.mActionDoneSync) {
            boolean shutdownAnimationFinished = false;
            VSlog.i(TAG, "start waiting for shutdown animation!");
            SystemProperties.set("sys.boot.animshutdown", "0");
            while (true) {
                if (this.mShutdownThread.mActionDone && shutdownAnimationFinished) {
                    break;
                }
                long delay = endTime - SystemClock.elapsedRealtime();
                if (!shutdownAnimationFinished && SystemProperties.get("sys.boot.animshutdown", "0").equals("1")) {
                    VSlog.i(TAG, "end waiting for shutdown animation!");
                    shutdownAnimationFinished = true;
                    if (this.mShutdownThread.mActionDone) {
                        break;
                    }
                }
                VSlog.i(TAG, "wait delay : " + delay + ",mActionDone : " + this.mShutdownThread.mActionDone + ", shutdownAnimationFinished : " + shutdownAnimationFinished);
                if (delay <= 0) {
                    VSlog.w(TAG, "Shutdown broadcast timed out");
                    break;
                }
                if (ShutdownThread.mRebootHasProgressBar) {
                    int status = (int) ((((10000 - delay) * 1.0d) * 2.0d) / 10000.0d);
                    ShutdownThread.sInstance.setRebootProgress(status, (CharSequence) null);
                }
                try {
                    this.mShutdownThread.mActionDoneSync.wait(Math.min(delay, 500L));
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void notifyShutdown() {
        if (MultiDisplayManager.isMultiDisplay) {
            getOrCreateMultiDisplay().notifyShutdown();
        }
        try {
            ((SystemAutoRecoverManagerInternal) LocalServices.getService(SystemAutoRecoverManagerInternal.class)).notifyShutDown();
        } catch (Exception e) {
            VSlog.d(TAG, "notifyShutdown cause exception: " + e);
        }
    }

    private static MultiDisplayManagerInternal getOrCreateMultiDisplay() {
        if (mMultiDisplayManagerInternal == null) {
            mMultiDisplayManagerInternal = (MultiDisplayManagerInternal) LocalServices.getService(MultiDisplayManagerInternal.class);
        }
        return mMultiDisplayManagerInternal;
    }

    public boolean isMdmModemType(Context context) {
        boolean isMdmModemType = SystemProperties.get("ro.boot.baseband", "unknown").equals("mdm");
        if (isMdmModemType && context != null) {
            VSlog.d(TAG, "isMdmModemType here, shutdown brightness");
            DisplayManager displayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
            if (displayManager != null) {
                displayManager.setOverrideDisplayBrightness(0, 0);
            }
        }
        return isMdmModemType;
    }

    public void dummy() {
    }

    public void initializeDBHandler() {
        HandlerThread handlerThread = new HandlerThread("mDatabaseHandlerThread");
        mDatabaseHandlerThread = handlerThread;
        handlerThread.start();
        mDatabaseHandler = mDatabaseHandlerThread.getThreadHandler();
    }

    private void notifySavingPassword(final Context context) {
        if (mDatabaseHandler == null) {
            VSlog.e(TAG, "mDatabaseHander doesn't exist!");
        } else if (TextUtils.isEmpty(savingPasswordValue)) {
            VSlog.e(TAG, "The name can't be empty!");
        } else {
            int curUserId = ActivityManager.getCurrentUser();
            if (curUserId != 0) {
                VSlog.e(TAG, "userId " + curUserId + " is invalid for saving!");
                return;
            }
            mDatabaseHandler.post(new Runnable() { // from class: com.android.server.power.-$$Lambda$VivoShutdownThreadImpl$jKqigRe0Ujt3LEZ_j1uJRhXDdbM
                @Override // java.lang.Runnable
                public final void run() {
                    VivoShutdownThreadImpl.lambda$notifySavingPassword$0(context);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifySavingPassword$0(Context context) {
        try {
            Settings.Global.putInt(context.getContentResolver(), savingPasswordValue, 1);
            VSlog.w(TAG, "put db 1");
        } catch (Exception e) {
            VSlog.e(TAG, "Settings throws an exception:" + e.toString());
        }
    }

    public void fingerprintRecordReboot(Context context, String rebootReason) {
        VSlog.i(TAG, "recordReboot reason: " + rebootReason);
        try {
            if ("recovery-update".equals(rebootReason)) {
                ContentResolver resolver = context.getContentResolver();
                if (Settings.Global.getInt(resolver, "com.bbk.updater.silent", 0) == 1) {
                    VSlog.i(TAG, "silent update");
                    SystemProperties.set("persist.vivo.face.reboot", "silent_update");
                    SystemProperties.set("persist.vivo.finger.reboot", "silent_update");
                    Settings.Global.putInt(resolver, "com.bbk.updater.silent", 0);
                    notifySavingPassword(context);
                } else {
                    VSlog.i(TAG, "normal recovery");
                    SystemProperties.set("persist.vivo.face.reboot", "recovery");
                    SystemProperties.set("persist.vivo.finger.reboot", "recovery");
                }
            } else if ("recovery".equals(rebootReason)) {
                VSlog.i(TAG, "normal recovery");
                SystemProperties.set("persist.vivo.face.reboot", "recovery");
                SystemProperties.set("persist.vivo.finger.reboot", "recovery");
            } else if ("silent".equals(rebootReason)) {
                notifySavingPassword(context);
                SystemProperties.set("persist.vivo.face.reboot", "silent");
                SystemProperties.set("persist.vivo.finger.reboot", "silent");
            } else {
                SystemProperties.set("persist.vivo.face.reboot", "user");
                SystemProperties.set("persist.vivo.finger.reboot", "user");
            }
        } catch (Exception e) {
            VSlog.e(TAG, "Settings throws an exception:" + e.toString());
        }
    }

    public void setBacklightOff(Context context) {
        if (context == null || this.mBlightOff) {
            return;
        }
        PowerManager pm = (PowerManager) context.getSystemService("power");
        if (pm == null) {
            VSlog.e(TAG, "check PowerManager: PowerManager service is null");
            return;
        }
        this.mBlightOff = true;
        VSlog.d(TAG, "setBacklightBrightness: Off");
        pm.goToSleep(SystemClock.uptimeMillis(), KernelConfig.DBG_TARGET_REGADDR_VALUE_GET, 0);
    }

    public void pollBacklightOff(Context context) {
        if (context == null) {
            return;
        }
        try {
            DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
            DisplayInfo di = new DisplayInfo();
            long endTime = SystemClock.elapsedRealtime() + 1000;
            long timeOut = endTime - SystemClock.elapsedRealtime();
            while (timeOut > 0) {
                displayManager.getDisplay(0).getDisplayInfo(di);
                if (di.state == 1) {
                    break;
                }
                SystemClock.sleep(50L);
                timeOut = endTime - SystemClock.elapsedRealtime();
            }
            VSlog.i(TAG, "Backlight polling take:" + (1000 - timeOut) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}