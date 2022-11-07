package com.android.server.wm;

import android.app.IEasyShareController;
import android.content.ClipData;
import android.database.ContentObserver;
import android.hardware.display.DisplayManagerInternal;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.MotionEvent;
import com.android.server.LocalServices;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoEasyShareManager {
    private static boolean DEBUG = false;
    private static final String EASY_SHARE_FORCE_BRIGHTNESS_OFF = "easy_share_force_brightness_off";
    private static final String EASY_SHARE_PC_SHARING = "easy_share_pc_sharing";
    private static final boolean IS_ENG;
    private static final boolean IS_LOG_CTRL_OPEN;
    private static final int MSG_NOTIFY_DRAG_RESULT = 10002;
    private static final int MSG_NOTIFY_DRAG_STARTED_FROM_PC = 10001;
    private static final int MSG_NOTIFY_FORCE_BRIGHTNESS_OFF_STATE_CHANGED = 10007;
    private static final int MSG_NOTIFY_INTERCEPT_MOTION_EVENT_IN_FORCE_BRIGHTNESS_OFF_STATE = 10006;
    private static final int MSG_NOTIFY_PRIMARY_CLIP = 10008;
    private static final int MSG_NOTIFY_PWD_MODE = 10003;
    private static final int MSG_NOTIFY_TASK_SECURE = 10004;
    private static final int MSG_RESET_PC_SHARE_STATE = 10005;
    public static final boolean SUPPORT_PCSHARE;
    private static final String TAG = "VivoEasyShareManager";
    private ActivityTaskManagerService mActivityTaskManagerService;
    private final IBinder.DeathRecipient mDeathRecipient;
    private DisplayManagerInternal mDisplayManagerInternal;
    private IEasyShareController mEasyShareController;
    private Handler mEasyShareHandler;
    private boolean mIsInPcSharing;
    private final Object mLock;
    private boolean mMainSecure;

    static {
        boolean z = false;
        SUPPORT_PCSHARE = SystemProperties.getInt("persist.vivo.support.pcshare", 0) == 1;
        IS_ENG = Build.TYPE.equals("eng");
        boolean equals = "yes".equals(SystemProperties.get("persist.sys.log.ctrl", "no"));
        IS_LOG_CTRL_OPEN = equals;
        if (equals || IS_ENG) {
            z = true;
        }
        DEBUG = z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class EasyShareHandler extends Handler {
        public EasyShareHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case VivoEasyShareManager.MSG_NOTIFY_DRAG_STARTED_FROM_PC /* 10001 */:
                    VivoEasyShareManager.this.handleNotifyDragStartedFromPC(msg.arg1, msg.arg2, (ClipData) msg.obj);
                    return;
                case VivoEasyShareManager.MSG_NOTIFY_DRAG_RESULT /* 10002 */:
                    VivoEasyShareManager.this.handleNotifyDragResult(msg.arg1, msg.arg2 == 1, (String) msg.obj);
                    return;
                case VivoEasyShareManager.MSG_NOTIFY_PWD_MODE /* 10003 */:
                    VivoEasyShareManager.this.handleNotifyPwdMode(((Boolean) msg.obj).booleanValue());
                    return;
                case VivoEasyShareManager.MSG_NOTIFY_TASK_SECURE /* 10004 */:
                    VivoEasyShareManager.this.handleNotifyTaskSecure(((Boolean) msg.obj).booleanValue());
                    return;
                case VivoEasyShareManager.MSG_RESET_PC_SHARE_STATE /* 10005 */:
                    VivoEasyShareManager.this.handleResetPcShareState();
                    return;
                case VivoEasyShareManager.MSG_NOTIFY_INTERCEPT_MOTION_EVENT_IN_FORCE_BRIGHTNESS_OFF_STATE /* 10006 */:
                    VivoEasyShareManager.this.handleNotifyInterceptMotionEventInForceBrightnessOffState((MotionEvent) msg.obj);
                    return;
                case VivoEasyShareManager.MSG_NOTIFY_FORCE_BRIGHTNESS_OFF_STATE_CHANGED /* 10007 */:
                    VivoEasyShareManager.this.handleNotifyForceBrightnessStateChanged(((Boolean) msg.obj).booleanValue());
                    return;
                case VivoEasyShareManager.MSG_NOTIFY_PRIMARY_CLIP /* 10008 */:
                    VivoEasyShareManager.this.handleNotifyPrimaryClip((ClipData) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    private VivoEasyShareManager() {
        this.mActivityTaskManagerService = null;
        this.mLock = new Object();
        this.mEasyShareHandler = null;
        this.mMainSecure = false;
        this.mIsInPcSharing = false;
        this.mDeathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.wm.VivoEasyShareManager.1
            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                if (VivoEasyShareManager.DEBUG) {
                    VSlog.i(VivoEasyShareManager.TAG, "binderDied()");
                }
                synchronized (VivoEasyShareManager.this.mLock) {
                    VivoEasyShareManager.this.mEasyShareController = null;
                    VivoEasyShareManager.this.mEasyShareHandler.removeMessages(VivoEasyShareManager.MSG_RESET_PC_SHARE_STATE);
                    VivoEasyShareManager.this.mEasyShareHandler.obtainMessage(VivoEasyShareManager.MSG_RESET_PC_SHARE_STATE).sendToTarget();
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class VivoEasyShareManagerHolder {
        private static final VivoEasyShareManager sVivoEasyShareManager = new VivoEasyShareManager();

        private VivoEasyShareManagerHolder() {
        }
    }

    public static VivoEasyShareManager getInstance() {
        return VivoEasyShareManagerHolder.sVivoEasyShareManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initAtms(ActivityTaskManagerService atm) {
        this.mActivityTaskManagerService = atm;
        HandlerThread easyShareThread = new HandlerThread("EasyShare");
        easyShareThread.start();
        this.mEasyShareHandler = new EasyShareHandler(easyShareThread.getLooper());
    }

    public void setEasyShareController(IEasyShareController controller) {
        IBinder binder;
        if (!SUPPORT_PCSHARE) {
            return;
        }
        if (DEBUG) {
            VSlog.i(TAG, "setEasyShareController: controller = " + controller);
        }
        synchronized (this.mLock) {
            if (controller != null) {
                IBinder binder2 = controller.asBinder();
                if (binder2 != null) {
                    try {
                        binder2.linkToDeath(this.mDeathRecipient, 0);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (this.mEasyShareController != null && (binder = this.mEasyShareController.asBinder()) != null) {
                    binder.unlinkToDeath(this.mDeathRecipient, 0);
                }
                this.mEasyShareHandler.removeMessages(MSG_RESET_PC_SHARE_STATE);
                this.mEasyShareHandler.obtainMessage(MSG_RESET_PC_SHARE_STATE).sendToTarget();
            }
            this.mEasyShareController = controller;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotifyDragResult(int action, boolean result, String packageName) {
        if (DEBUG) {
            VSlog.i(TAG, "handleNotifyDragResult: action=" + action + ", result=" + result + ", packageName=" + packageName);
        }
        synchronized (this.mLock) {
            if (this.mEasyShareController != null) {
                this.mEasyShareController.notifyDragResult(action, result, packageName);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotifyDragStartedFromPC(int x, int y, ClipData data) {
        if (DEBUG) {
            VSlog.i(TAG, "notifyDragStartedFromPC: x=" + x + ", y=" + y + ", data=" + data);
        }
        synchronized (this.mLock) {
            if (this.mEasyShareController != null) {
                this.mEasyShareController.notifyDragStartedFromPC(x, y, data);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotifyPwdMode(boolean isPwdMode) {
        if (DEBUG) {
            VSlog.i(TAG, "handleNotifyPwdMode: isPwdMode=" + isPwdMode);
        }
        synchronized (this.mLock) {
            if (this.mEasyShareController != null) {
                this.mEasyShareController.notifyPwdMode(isPwdMode);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotifyTaskSecure(boolean isSecure) {
        if (DEBUG) {
            VSlog.i(TAG, "handleNotifyTaskSecure: isSecure=" + isSecure);
        }
        synchronized (this.mLock) {
            if (this.mEasyShareController != null) {
                this.mEasyShareController.notifyTaskSecure(isSecure);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleResetPcShareState() {
        if (DEBUG) {
            VSlog.i(TAG, "handleResetPcShareState");
        }
        Settings.System.putInt(this.mActivityTaskManagerService.mContext.getContentResolver(), EASY_SHARE_PC_SHARING, 0);
        Settings.System.putInt(this.mActivityTaskManagerService.mContext.getContentResolver(), EASY_SHARE_FORCE_BRIGHTNESS_OFF, 0);
        if (this.mDisplayManagerInternal == null) {
            this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        }
        DisplayManagerInternal displayManagerInternal = this.mDisplayManagerInternal;
        if (displayManagerInternal != null) {
            displayManagerInternal.setForceDisplayBrightnessOff(false, "easyshare");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotifyInterceptMotionEventInForceBrightnessOffState(MotionEvent event) {
        if (DEBUG) {
            VSlog.d(TAG, "handleNotifyInterceptMotionEventInForceBrightnessOffState: event=" + event);
        }
        synchronized (this.mLock) {
            if (this.mEasyShareController != null) {
                this.mEasyShareController.notifyInterceptMotionEventInForceBrightnessOffState(event);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotifyForceBrightnessStateChanged(boolean isOff) {
        if (DEBUG) {
            VSlog.d(TAG, "handleNotifyForceBrightnessStateChanged: isOff=" + isOff);
        }
        synchronized (this.mLock) {
            if (this.mEasyShareController != null) {
                this.mEasyShareController.notifyForceBrightnessOffStateChanged(isOff);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotifyPrimaryClip(ClipData data) {
        if (DEBUG) {
            VSlog.d(TAG, "handleNotifyPrimaryClip: data=" + data);
        }
        synchronized (this.mLock) {
            if (this.mEasyShareController != null) {
                this.mEasyShareController.notifyPrimaryClip(data);
            }
        }
    }

    public void notifyDragStartedFromPC(int x, int y, ClipData data) {
        if (!SUPPORT_PCSHARE) {
            return;
        }
        this.mEasyShareHandler.removeMessages(MSG_NOTIFY_DRAG_STARTED_FROM_PC);
        this.mEasyShareHandler.obtainMessage(MSG_NOTIFY_DRAG_STARTED_FROM_PC, x, y, data).sendToTarget();
    }

    public void notifyDragResult(int action, boolean result, String packageName) {
        if (!SUPPORT_PCSHARE) {
            return;
        }
        this.mEasyShareHandler.removeMessages(MSG_NOTIFY_DRAG_RESULT);
        this.mEasyShareHandler.obtainMessage(MSG_NOTIFY_DRAG_RESULT, action, result ? 1 : 0, packageName).sendToTarget();
    }

    public void notifyPwdMode(boolean isPwdMode) {
        if (!SUPPORT_PCSHARE) {
            return;
        }
        this.mEasyShareHandler.removeMessages(MSG_NOTIFY_PWD_MODE);
        this.mEasyShareHandler.obtainMessage(MSG_NOTIFY_PWD_MODE, Boolean.valueOf(isPwdMode)).sendToTarget();
    }

    public void notifyTaskSecure(DisplayContent displayContent) {
        boolean secure;
        if (!SUPPORT_PCSHARE || displayContent == null || (secure = displayContent.isWindowSecure()) == this.mMainSecure) {
            return;
        }
        this.mMainSecure = secure;
        VSlog.i(TAG, "task is secure : " + secure);
        this.mEasyShareHandler.removeMessages(MSG_NOTIFY_TASK_SECURE);
        this.mEasyShareHandler.obtainMessage(MSG_NOTIFY_TASK_SECURE, Boolean.valueOf(secure)).sendToTarget();
    }

    public void registerPCShareStateObserver() {
        if (!SUPPORT_PCSHARE) {
            return;
        }
        Settings.System.putInt(this.mActivityTaskManagerService.mContext.getContentResolver(), EASY_SHARE_PC_SHARING, 0);
        Settings.System.putInt(this.mActivityTaskManagerService.mContext.getContentResolver(), EASY_SHARE_FORCE_BRIGHTNESS_OFF, 0);
        this.mActivityTaskManagerService.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(EASY_SHARE_PC_SHARING), false, new ContentObserver(this.mEasyShareHandler) { // from class: com.android.server.wm.VivoEasyShareManager.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                VivoEasyShareManager vivoEasyShareManager = VivoEasyShareManager.this;
                vivoEasyShareManager.mIsInPcSharing = Settings.System.getInt(vivoEasyShareManager.mActivityTaskManagerService.mContext.getContentResolver(), VivoEasyShareManager.EASY_SHARE_PC_SHARING, 0) == 1;
                if (VivoEasyShareManager.DEBUG) {
                    VSlog.i(VivoEasyShareManager.TAG, "registerPCShareStateObserver: onChange, mIsInPcSharing" + VivoEasyShareManager.this.mIsInPcSharing);
                }
            }
        });
    }

    public boolean isInPcSharing() {
        if (!SUPPORT_PCSHARE) {
            return false;
        }
        return this.mIsInPcSharing;
    }

    public void notifyInterceptMotionEventInForceBrightnessOffState(MotionEvent event) {
        if (!SUPPORT_PCSHARE) {
            return;
        }
        this.mEasyShareHandler.removeMessages(MSG_NOTIFY_INTERCEPT_MOTION_EVENT_IN_FORCE_BRIGHTNESS_OFF_STATE);
        this.mEasyShareHandler.obtainMessage(MSG_NOTIFY_INTERCEPT_MOTION_EVENT_IN_FORCE_BRIGHTNESS_OFF_STATE, event).sendToTarget();
    }

    public void notifyForceBrightnessOffStateChanged(boolean isOff) {
        if (!SUPPORT_PCSHARE) {
            return;
        }
        this.mEasyShareHandler.removeMessages(MSG_NOTIFY_FORCE_BRIGHTNESS_OFF_STATE_CHANGED);
        this.mEasyShareHandler.obtainMessage(MSG_NOTIFY_FORCE_BRIGHTNESS_OFF_STATE_CHANGED, Boolean.valueOf(isOff)).sendToTarget();
    }

    public void notifyPrimaryClip(ClipData data) {
        if (!SUPPORT_PCSHARE) {
            return;
        }
        this.mEasyShareHandler.removeMessages(MSG_NOTIFY_PRIMARY_CLIP);
        this.mEasyShareHandler.obtainMessage(MSG_NOTIFY_PRIMARY_CLIP, data).sendToTarget();
    }
}