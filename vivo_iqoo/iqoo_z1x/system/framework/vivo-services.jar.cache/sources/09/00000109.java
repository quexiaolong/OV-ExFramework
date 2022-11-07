package com.android.server.am.firewall;

import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.server.UiThread;
import com.android.server.am.ActivityManagerService;
import com.android.server.display.VivoDisplayPowerControllerImpl;
import com.android.server.policy.InputExceptionReport;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoFirewall {
    private static final String BRINGUP_FUNCTION = "persist.sys.vivofirewall";
    public static boolean DEBUG = false;
    public static final int EVENT_TYPE_ACTIVITY = 3;
    public static final int EVENT_TYPE_NOTIFICATION_BROADCAST = 2;
    public static final int EVENT_TYPE_NOTIFICATION_SERVICE = 1;
    private static final String KEY_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    public static final String TAG = "VivoFirewall";
    public static final String TYPE_ACTIVITY = "activity";
    public static final String TYPE_INSTRUMENT = "instrument";
    public static final String TYPE_PROVIDER = "provider";
    public static final String TYPE_SERVICE = "service";
    public static final String TYPE_SYSTEM_ACCO = "accountservice";
    public static final String TYPE_SYSTEM_JOB = "jobservice";
    public static final String TYPE_SYSTEM_NOTI = "notificationservice";
    public static final String TYPE_SYSTEM_SYNC = "syncservice";
    private static final String VFW_VERSION = "persist.sys.vfwversion";
    private static volatile VivoFirewall sVivoFirewall;
    ActivityManagerService mAms;
    private VivoBringupController mAppBringupController;
    private VivoAppIsolationController mAppIsolationController;
    private VivoBackgroundActivityController mBackgroundActivityController;
    Context mContext;
    HandlerThread mHandlerThread;
    PowerManager mPowerMgr;
    private Handler mUiHandler;
    private static final boolean IS_ENG = Build.TYPE.equals("eng");
    private static final boolean IS_LOG_CTRL_OPEN = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
    private static final ThreadLocal<Integer> sThreadLocalInteger = new ThreadLocal<>();

    static {
        DEBUG = IS_LOG_CTRL_OPEN || IS_ENG;
    }

    public static VivoFirewall getInstance(Context context) {
        if (sVivoFirewall == null) {
            synchronized (VivoFirewall.class) {
                if (sVivoFirewall == null) {
                    sVivoFirewall = new VivoFirewall(context);
                }
            }
        }
        return sVivoFirewall;
    }

    private VivoFirewall(Context ctx) {
        HandlerThread handlerThread = new HandlerThread("vivo_firewall_thread");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mAppBringupController = new VivoBringupController(ctx, this, this.mHandlerThread.getLooper());
        this.mBackgroundActivityController = new VivoBackgroundActivityController(ctx, this);
        this.mAppIsolationController = new VivoAppIsolationController(ctx, this.mHandlerThread.getLooper());
        this.mUiHandler = UiThread.getHandler();
        this.mContext = ctx;
    }

    public void init(ActivityManagerService ams) {
        this.mAms = ams;
    }

    public void firewallStart() {
        SystemProperties.set(BRINGUP_FUNCTION, "1");
        SystemProperties.set(VFW_VERSION, InputExceptionReport.LEVEL_VERY_LOW);
        this.mAppBringupController.start();
        this.mAppIsolationController.start();
    }

    public void systemReady() {
        this.mAppBringupController.systemReady();
        this.mBackgroundActivityController.startMonitor();
        this.mAppIsolationController.systemReady();
    }

    public boolean shouldPreventStartProcess(String callerPackage, ComponentInfo bringupSide, String type, int callerPid, int callerUid, int realCallerUid) {
        return this.mAppBringupController.shouldPreventStartProcess(callerPackage, bringupSide, type, callerPid, callerUid, realCallerUid);
    }

    public boolean shouldPreventStartProcess(String callerPackage, ComponentInfo bringupSide, String type, int callerPid, int callerUid) {
        return this.mAppBringupController.shouldPreventStartProcess(callerPackage, bringupSide, type, callerPid, callerUid);
    }

    public boolean shouldPreventActivityStart(boolean callerVisible, String callerPackage, ComponentInfo bringupSide, String type, int callerPid, int callerUid) {
        return this.mBackgroundActivityController.shouldPreventActivityStart(callerVisible, callerPackage, bringupSide, type, callerPid, callerUid);
    }

    public int checkActivityStart(boolean callerVisible, String callerPackage, ComponentInfo bringupSide, String type, int callerPid, int callerUid) {
        if (this.mBackgroundActivityController.shouldPreventActivityStart(callerVisible, callerPackage, bringupSide, type, callerPid, callerUid)) {
            return VivoDisplayPowerControllerImpl.COLOR_FADE_ANIMATION_VERTICAL;
        }
        if (callerVisible) {
            return this.mAppBringupController.checkFgActivityCtrlState(callerPackage, callerUid, bringupSide);
        }
        return 0;
    }

    public void showFgActivityDialog(String callerPackage, String calledPackage, IIntentSender positiveSender, IIntentSender negativeSender, int userId, IBinder token, String resultWho, int requestCode, String resolvedType) {
        this.mAppBringupController.showFgActivityDialog(callerPackage, calledPackage, positiveSender, negativeSender, this.mUiHandler, userId, token, resultWho, requestCode, resolvedType);
    }

    public boolean checkBgActivityInWhiteList(ComponentInfo bringupSide, String callingPackage, int callingUid) {
        return this.mBackgroundActivityController.checkBgActivityInWhiteList(bringupSide, callingPackage, callingUid);
    }

    public boolean shouldPreventBringUpBackgroundActivity(String callerPackage, ComponentInfo bringupSide, String type, int callerPid, int callerUid) {
        return this.mBackgroundActivityController.shouldPreventBringUpBackgroundActivity(callerPackage, bringupSide, type, callerPid, callerUid);
    }

    public boolean shouldPreventAppInteraction(String callerPackage, ComponentInfo bringupSide, String type, int callerPid, int callerUid) {
        return this.mAppIsolationController.shouldPreventAppInteraction(callerPackage, bringupSide, type, callerPid, callerUid);
    }

    public boolean shouldValidateSyncType(String syncType) {
        return this.mAppBringupController.shouldValidateSyncType(syncType);
    }

    public void noteImportantEvent(int eventType, String packageName) {
        this.mBackgroundActivityController.noteImportantEvent(eventType, packageName);
    }

    public boolean isSystemAppControlled(String packageName) {
        return this.mAppBringupController.isSystemAppControled(packageName);
    }

    public int getBringupContinuousSwitch() {
        return this.mAppBringupController.getBringupContinuousSwitch();
    }

    public void dumpCachedInfo(FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        this.mAppBringupController.dumpCachedInfo(fd, pw, args, opti);
    }

    public boolean isScreenOff() {
        Context context;
        if (this.mPowerMgr == null && (context = this.mContext) != null) {
            this.mPowerMgr = (PowerManager) context.getSystemService("power");
        }
        PowerManager powerManager = this.mPowerMgr;
        return (powerManager == null || powerManager.isInteractive()) ? false : true;
    }

    public ComponentName getTopAppComponentName() {
        ActivityManagerService activityManagerService = this.mAms;
        if (activityManagerService == null) {
            return null;
        }
        return activityManagerService.getTopAppComponentName();
    }

    public boolean isUidProcessAliveUnsafe(int uid, String callingPackage) {
        ActivityManagerService activityManagerService = this.mAms;
        if (activityManagerService == null) {
            return false;
        }
        return activityManagerService.isUidProcessAliveUnsafe(uid, callingPackage);
    }

    public boolean isUidRunning(int uid) {
        long origId = Binder.clearCallingIdentity();
        boolean isRunning = isUidProcessAliveUnsafe(uid, VivoPermissionUtils.OS_PKG);
        Binder.restoreCallingIdentity(origId);
        return isRunning;
    }

    public boolean hasForegroundWindow(String packageName) {
        ActivityManagerService activityManagerService = this.mAms;
        if (activityManagerService == null) {
            return false;
        }
        return activityManagerService.hasForegroundWindow(packageName);
    }

    public void sendThirdLifeControlIntent(final String callerPkg, final int callerUid, final String calledPkg, final int calledUid, final String calledClassName, final String type) {
        if (!isUidRunning(calledUid) && !TextUtils.equals(callerPkg, calledPkg)) {
            final Intent intent = new Intent("com.vivo.abe.third.lifecontrol");
            intent.putExtra("CALLER_PACKAGE_NAME", callerPkg);
            intent.putExtra("CALLER_UID", callerUid);
            intent.putExtra("CALLING_PACKAGE_NAME", calledPkg);
            intent.putExtra("CALLING_UID", calledUid);
            intent.putExtra("CALLING_CLASS_NAME", calledClassName);
            intent.putExtra("CALLING_TYPE", type);
            this.mHandlerThread.getThreadHandler().post(new Runnable() { // from class: com.android.server.am.firewall.VivoFirewall.1
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        VivoFirewall.this.mAms.broadcastIntent((IApplicationThread) null, intent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, (String[]) null, -1, (Bundle) null, false, false, -1);
                        VivoFirewall.log("Send broadcast to AppBehaviorEngine, callerPackage=" + callerPkg + ", callerUid=" + callerUid + ", calledPackage=" + calledPkg + ", calledUid=" + calledUid + ", calledClassName=" + calledClassName + ", calledType=" + type);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void setThreadLocalInteger(Integer integer) {
        sThreadLocalInteger.set(integer);
    }

    public Integer getThreadLocalInteger() {
        return sThreadLocalInteger.get();
    }

    public void removeThreadLocalInteger() {
        sThreadLocalInteger.remove();
    }

    public static void log(String msg) {
        if (DEBUG) {
            VSlog.d(TAG, msg);
        }
    }

    public static void checkTime(long startTime, String where) {
        long now = SystemClock.uptimeMillis();
        if (now - startTime > 50) {
            VSlog.w(TAG, "Slow operation: " + (now - startTime) + "ms so far, now at " + where);
        }
    }
}