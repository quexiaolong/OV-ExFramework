package com.android.server.policy.key;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.KeyEvent;
import com.android.internal.telephony.ITelephony;
import com.android.server.LocalServices;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.android.server.policy.IVivoAdjustmentPolicy;
import com.android.server.policy.VivoPolicyUtil;
import com.android.server.policy.VivoWMPHook;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;

/* loaded from: classes.dex */
public final class VivoMenuKeyHandler extends AVivoInterceptKeyCallback {
    private static final String TAG = "VivoMenuKeyHandler";
    private Context mContext;
    private KeyguardManager mKeyguardManager;
    private boolean mMenuKeyForward = checkMenuKeyForward();
    private boolean mMenuKeyLongPressConsumed;
    private boolean mMenuKeyTriggered;
    private StatusBarManagerInternal mStatusBarManagerInternal;
    private IVivoAdjustmentPolicy mVivoPolicy;
    private static byte[] mLock = new byte[0];
    private static boolean ENABLE_DISPATCH_MENU_DELAY = false;

    public VivoMenuKeyHandler(Context context, IVivoAdjustmentPolicy vivoPolicy) {
        this.mContext = context;
        this.mVivoPolicy = vivoPolicy;
    }

    private boolean checkMenuKeyForward() {
        if (SystemProperties.get("ro.vivo.op.entry", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).contains("CMCC_RW")) {
            printf("checkMenuKeyForward true.");
            return true;
        }
        printf("checkMenuKeyForward false.");
        return false;
    }

    private void triggerVirtualKeypress(int keyCode) {
        printf("triggerVirtualKeypress menu");
        InputManager im = InputManager.getInstance();
        long now = SystemClock.uptimeMillis();
        KeyEvent downEvent = new KeyEvent(now, now, 0, keyCode, 0, 0, -1, 0, 8, 257);
        KeyEvent upEvent = KeyEvent.changeAction(downEvent, 1);
        im.injectInputEvent(downEvent, 0);
        im.injectInputEvent(upEvent, 0);
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        boolean isDrop = this.mMenuKeyForward;
        int i = this.mState;
        return isDrop;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        int result = -100;
        synchronized (mLock) {
            int i = this.mState;
            boolean z = true;
            if (i == 0) {
                boolean virtualKey = this.mMenuKeyTriggered;
                if (!virtualKey) {
                    this.mMenuKeyTriggered = true;
                    this.mMenuKeyLongPressConsumed = false;
                }
            } else if (i == 1) {
                if (event.getDeviceId() != -1) {
                    z = false;
                }
                boolean virtualKey2 = z;
                if (virtualKey2) {
                    printf("Let the app handle the key");
                    return 0;
                }
                result = (ENABLE_DISPATCH_MENU_DELAY && this.mMenuKeyTriggered && !this.mMenuKeyLongPressConsumed) ? 50 : -1;
            }
            return result;
        }
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        int result = -100;
        synchronized (mLock) {
            int i = this.mState;
            if (i != 0) {
                boolean z = true;
                if (i == 1) {
                    if (event.getDeviceId() != -1) {
                        z = false;
                    }
                    boolean virtualKey = z;
                    if (virtualKey) {
                        printf("Let the app handle the key");
                        return 0;
                    }
                    if (this.mMenuKeyLongPressConsumed) {
                        this.mMenuKeyLongPressConsumed = false;
                    }
                    if (!event.isCanceled()) {
                        triggerVirtualKeypress(82);
                    }
                    result = -1;
                }
            } else {
                this.mMenuKeyTriggered = false;
                if (this.mMenuKeyLongPressConsumed) {
                    result = 1073741824;
                }
            }
            return result;
        }
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public void onKeyLongPress(int keyCode, KeyEvent event) {
        synchronized (mLock) {
            if (this.mMenuKeyTriggered) {
                this.mMenuKeyLongPressConsumed = true;
                performMenuKeyLongPress();
            }
        }
    }

    private void performMenuKeyLongPress() {
        printf("performMenuKeyLongPress");
        if (!isVivoiceLaunchable()) {
            VLog.w(TAG, "return because of VivoiceLaunchable!");
        } else if (getKeyguardManager().isKeyguardSecure() && getKeyguardManager().isKeyguardLocked()) {
            VLog.w(TAG, "return because of KeyguardSecure!");
        } else {
            this.mVivoPolicy.performHapticFeedback(0, false, true);
            if (isSuperPowerSaverMode()) {
                VLog.w(TAG, "return because of SuperPowerSaverMode!");
            } else if (isDrivingMode()) {
                VLog.w(TAG, "return because of DrivingMode!");
            } else if (!isExport()) {
                Intent intent = new Intent("vivo.intent.action.MENU_KEY_LONG_PRESS");
                intent.setPackage("com.bbk.VoiceAssistant");
                intent.setFlags(268435456);
                try {
                    this.mContext.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                toggleRecentApps();
            }
        }
    }

    private void printf(String msg) {
        VivoWMPHook.printf(msg);
    }

    private boolean isSuperPowerSaverMode() {
        return VivoPolicyUtil.isSPSMode();
    }

    private boolean isDrivingMode() {
        return VivoPolicyUtil.isDrivingMode();
    }

    private ITelephony getTelephonyService() {
        return ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
    }

    private KeyguardManager getKeyguardManager() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        }
        return this.mKeyguardManager;
    }

    private boolean isVivoiceLaunchable() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "vivo_vivoice_launchable", 1) > 0;
    }

    private StatusBarManagerInternal getStatusBarManagerInternal() {
        if (this.mStatusBarManagerInternal == null) {
            this.mStatusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
        }
        return this.mStatusBarManagerInternal;
    }

    private void toggleRecentApps() {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.toggleRecentApps();
        }
    }

    private boolean isExport() {
        return "yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no"));
    }
}