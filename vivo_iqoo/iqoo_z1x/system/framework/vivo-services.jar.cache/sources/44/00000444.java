package com.android.server.policy.key;

import android.app.ActivityManagerNative;
import android.app.ActivityTaskManager;
import android.app.IActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.KeyEvent;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.android.server.policy.IVivoAdjustmentPolicy;
import com.android.server.policy.VivoWMPHook;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public final class VivoBackKeyHandler extends AVivoInterceptKeyCallback {
    private static final String TAG = "VivoBackKeyHandler";
    private boolean mBackKeyLongPressConsumed;
    private boolean mBackKeyTriggered;
    private Context mContext;
    private boolean mIsCTS;
    private IVivoAdjustmentPolicy mVivoPolicy;
    private static byte[] mLock = new byte[0];
    private static boolean ENABLE_DISPATCH_BACK_DELAY = false;

    public VivoBackKeyHandler(Context context, IVivoAdjustmentPolicy vivoPolicy) {
        this.mIsCTS = false;
        this.mContext = context;
        this.mVivoPolicy = vivoPolicy;
        this.mIsCTS = SystemProperties.getBoolean("persist.sys.vivo.adc.cts", false);
    }

    private boolean isBackKeyLongPressEnabled() {
        int result = Settings.System.getInt(this.mContext.getContentResolver(), "floating_window_allow_back_key", 0);
        return result == 1;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        if (!this.mIsCTS) {
            return false;
        }
        VLog.i(TAG, "Istest : " + this.mIsCTS + ", return here.");
        return true;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        int result = -100;
        synchronized (mLock) {
            int i = this.mState;
            if (i == 0) {
                if (!this.mBackKeyTriggered) {
                    this.mBackKeyTriggered = true;
                    this.mBackKeyLongPressConsumed = false;
                }
            } else if (i == 1 && ENABLE_DISPATCH_BACK_DELAY && this.mBackKeyTriggered && !this.mBackKeyLongPressConsumed) {
                result = 50;
            }
        }
        return result;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        int result = -100;
        synchronized (mLock) {
            int i = this.mState;
            if (i == 0) {
                this.mBackKeyTriggered = false;
                if (this.mBackKeyLongPressConsumed) {
                    result = 1073741824;
                }
            } else if (i == 1 && this.mBackKeyLongPressConsumed) {
                this.mBackKeyLongPressConsumed = false;
                result = -1;
            }
        }
        return result;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public void onKeyLongPress(int keyCode, KeyEvent event) {
        synchronized (mLock) {
            if (this.mBackKeyTriggered) {
                performBackKeyLongPress();
            }
        }
    }

    private boolean interceptDismissPinningChord() {
        IActivityManager activityManager = ActivityManagerNative.asInterface(ServiceManager.checkService(VivoFirewall.TYPE_ACTIVITY));
        if (activityManager == null) {
            VLog.w(TAG, "ActivityManager is null ,return.");
            return false;
        }
        try {
        } catch (RemoteException e) {
            VLog.e(TAG, "RemoteException when stopLockTaskModeOnCurrent", e);
        }
        if (!activityManager.isInLockTaskMode()) {
            VLog.w(TAG, "Not in LockTaskMode!");
            return false;
        }
        ActivityTaskManager.getService().stopSystemLockTaskMode();
        this.mVivoPolicy.performHapticFeedback(0, false, true);
        this.mBackKeyLongPressConsumed = true;
        return true;
    }

    private void performBackKeyLongPress() {
        printf("performBackKeyLongPress");
        if (!interceptDismissPinningChord() && isBackKeyLongPressEnabled()) {
            VLog.d(TAG, "start floatingwindow.");
            this.mVivoPolicy.performHapticFeedback(0, false, true);
            Intent intent = new Intent("com.vivo.floatingwindow.state.flip");
            this.mContext.sendBroadcast(intent);
            this.mBackKeyLongPressConsumed = true;
        }
    }

    private void printf(String msg) {
        VivoWMPHook.printf(msg);
    }
}