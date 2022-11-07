package com.android.server.policy.key;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.IWindowManager;
import android.view.KeyEvent;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class VivoCameraDoubleClickKeyHandler extends AVivoInterceptKeyCallback {
    private static final String TAG = "VivoCameraDoubleClickKeyHandler";
    private PowerManager.WakeLock mCameraClickLock;
    private Context mContext;
    private boolean mIsFbeProject;
    private PowerManager mPowerManager;
    private UserManager mUserManager;
    private IWindowManager mWindowManager;
    private Handler mHandler = new Handler();
    private boolean mCameraDoubleClickConsumed = false;
    private Runnable mStartCameraRunnable = new Runnable() { // from class: com.android.server.policy.key.VivoCameraDoubleClickKeyHandler.1
        @Override // java.lang.Runnable
        public void run() {
            boolean userKeyUnlocked = VivoCameraDoubleClickKeyHandler.this.mUserManager.isUserUnlocked();
            VLog.d(VivoCameraDoubleClickKeyHandler.TAG, "start camera. mIstFbeProject = " + VivoCameraDoubleClickKeyHandler.this.mIsFbeProject + " userKeyUnlocked = " + userKeyUnlocked);
            if (!VivoCameraDoubleClickKeyHandler.this.mIsFbeProject || userKeyUnlocked) {
                try {
                    Intent intent = VivoCameraDoubleClickKeyHandler.this.getCameraIntent(VivoCameraDoubleClickKeyHandler.this.isKeyguardSecureLocked());
                    VivoCameraDoubleClickKeyHandler.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    return;
                } catch (Exception e) {
                    VLog.e(VivoCameraDoubleClickKeyHandler.TAG, "start camera activity cause exception:" + e);
                    return;
                }
            }
            try {
                PowerManager.WakeLock wakeLock = VivoCameraDoubleClickKeyHandler.this.mPowerManager.newWakeLock(268435466, "CameraDoubleClickKey");
                wakeLock.acquire(5000L);
                VivoCameraDoubleClickKeyHandler.this.mWindowManager.dismissKeyguard((IKeyguardDismissCallback) null, (CharSequence) null);
            } catch (RemoteException e2) {
                VLog.d(VivoCameraDoubleClickKeyHandler.TAG, "Fbe project an unlocked, dismiss keyguard cause exception: " + e2);
            }
        }
    };

    public VivoCameraDoubleClickKeyHandler(Context context, IWindowManager windowManager) {
        this.mIsFbeProject = false;
        this.mContext = context;
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManager = powerManager;
        PowerManager.WakeLock newWakeLock = powerManager.newWakeLock(1, "VivoCameraDoubleClickKeyHandler.mCameraClickLock");
        this.mCameraClickLock = newWakeLock;
        newWakeLock.setReferenceCounted(false);
        this.mWindowManager = windowManager;
        this.mIsFbeProject = "file".equals(SystemProperties.get("ro.crypto.type", "unknow"));
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        if (!this.mIsScreenOn) {
            return true;
        }
        return false;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        if (this.mState != 1 || event.getRepeatCount() != 0) {
            return -100;
        }
        this.mCameraClickLock.acquire(200L);
        boolean startCamera = true;
        if (this.mKeyInterceptionInfo != null && "com.android.camera".equals(this.mKeyInterceptionInfo.mOwningPackage) && this.mKeyInterceptionInfo.mLayoutTitle.contains("com.android.camera.CameraActivity")) {
            startCamera = false;
        }
        if (!startCamera) {
            return -100;
        }
        this.mHandler.post(this.mStartCameraRunnable);
        this.mCameraDoubleClickConsumed = true;
        return -1;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        if (this.mState != 0) {
            return -100;
        }
        if (this.mCameraClickLock.isHeld()) {
            this.mCameraClickLock.release();
        }
        if (!this.mCameraDoubleClickConsumed) {
            return -100;
        }
        this.mCameraDoubleClickConsumed = false;
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Intent getCameraIntent(boolean isSecureLocked) {
        Intent intent = new Intent();
        if (isSecureLocked) {
            intent.addFlags(270647296);
            intent.setAction(VivoCameraKeyHandler.ACTION_START_CAMERA_SECURE);
        } else {
            intent.setAction("android.intent.action.MAIN");
            intent.addFlags(807419904);
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.setComponent(new ComponentName("com.android.camera", "com.android.camera.CameraActivity"));
        }
        intent.putExtra("SlideToCamera", "wakeup");
        intent.putExtra("start_camera_by_gimbal", 1);
        return intent;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isKeyguardSecureLocked() {
        KeyguardManager mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (mKeyguardManager.isKeyguardSecure() && mKeyguardManager.isKeyguardLocked()) {
            return true;
        }
        return false;
    }

    private boolean isScreenLocked() {
        KeyguardManager mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (mKeyguardManager.isKeyguardLocked()) {
            return true;
        }
        return false;
    }
}