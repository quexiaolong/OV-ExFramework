package com.android.server.policy.key;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.ServiceManager;
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
public final class VivoTaskKeyHandler extends AVivoInterceptKeyCallback {
    private static final String TAG = "VivoTaskKeyHandler";
    private Context mContext;
    private boolean mEntryOp = SystemProperties.get("ro.vivo.op.entry", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).contains("CMCC_RW");
    private boolean mHasPermanentMenuKey;
    private KeyguardManager mKeyguardManager;
    private boolean mMenuKeyLongPressConsumed;
    private boolean mMenuKeyTriggered;
    private StatusBarManagerInternal mStatusBarManagerInternal;
    private IVivoAdjustmentPolicy mVivoPolicy;
    private static byte[] mLock = new byte[0];
    private static boolean ENABLE_DISPATCH_MENU_DELAY = false;

    public VivoTaskKeyHandler(Context context, IVivoAdjustmentPolicy vivoPolicy) {
        this.mHasPermanentMenuKey = false;
        this.mContext = context;
        this.mVivoPolicy = vivoPolicy;
        this.mHasPermanentMenuKey = SystemProperties.getInt("qemu.hw.mainkeys.vivo", 1) == 1;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        if (this.mHasPermanentMenuKey) {
            return false;
        }
        VLog.i(TAG, "Don't need to implement here when device has navigation bar");
        return true;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        int result = -100;
        synchronized (mLock) {
            int i = this.mState;
            if (i != 0) {
                if (i == 1) {
                    result = -1;
                }
            } else if (!this.mMenuKeyTriggered) {
                this.mMenuKeyTriggered = true;
                this.mMenuKeyLongPressConsumed = false;
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
                this.mMenuKeyTriggered = false;
                if (this.mMenuKeyLongPressConsumed) {
                    result = 1073741824;
                }
            } else if (i == 1) {
                if (this.mMenuKeyLongPressConsumed) {
                    this.mMenuKeyLongPressConsumed = false;
                } else if (!event.isCanceled()) {
                    toggleRecentApps();
                }
                result = -1;
            }
        }
        return result;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public void onKeyLongPress(int keyCode, KeyEvent event) {
        synchronized (mLock) {
            if (this.mMenuKeyTriggered && !this.mEntryOp) {
                this.mMenuKeyLongPressConsumed = true;
                performMenuKeyLongPress();
                return;
            }
            VLog.w(TAG, "Return because of menutriggered : " + this.mMenuKeyTriggered);
        }
    }

    private void performMenuKeyLongPress() {
        VLog.d(TAG, "performMenuKeyLongPress");
        if (isExport()) {
            VLog.w(TAG, "return because of oversea!");
        } else if (isVivoiceLaunchable()) {
            VLog.w(TAG, "return because of VivoiceLaunchable!");
        } else if (getKeyguardManager().isKeyguardSecure() && getKeyguardManager().isKeyguardLocked()) {
            VLog.w(TAG, "return because of KeyguardSecure!");
        } else if (isSuperPowerSaverMode()) {
            VLog.w(TAG, "return because of SuperPowerSaverMode!");
        } else if (isDrivingMode()) {
            VLog.w(TAG, "return because of DrivingMode!");
        } else {
            try {
                startVoiceAssitant();
            } catch (Exception ex) {
                ex.printStackTrace();
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
        boolean unlaunch = Settings.System.getInt(this.mContext.getContentResolver(), "vivo_vivoice_launchable", 1) == 0;
        boolean unwizard = Settings.System.getInt(this.mContext.getContentResolver(), "setup_wizard_has_run", 1) == 0;
        VLog.d(TAG, "isVivoice launchable unlaunch = " + unlaunch + " ,unwizard = " + unwizard);
        return unlaunch && unwizard;
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

    private void startVoiceAssitant() {
        Intent intent = new Intent();
        if (isVoiceAssitantEnabled()) {
            this.mVivoPolicy.performHapticFeedback(0, false, true);
            intent.addFlags(268435456);
            intent.setClassName("com.bbk.VoiceAssistant", "com.bbk.VoiceAssistant.VoiceAssistantActivity");
            this.mContext.startActivity(intent);
        } else if (!isVoiceAssitantRemind()) {
            this.mVivoPolicy.performHapticFeedback(0, false, true);
            intent.addFlags(268435456);
            intent.setClassName("com.bbk.VoiceAssistant", "com.bbk.VoiceAssistant.LauchTipActivity");
            this.mContext.startActivity(intent);
        }
    }

    private boolean isVoiceAssitantEnabled() {
        int val = Settings.Global.getInt(this.mContext.getContentResolver(), "com.bbk.VoiceAssistant.SWITCH", -1);
        return val == 1;
    }

    private boolean isVoiceAssitantRemind() {
        int val = Settings.Global.getInt(this.mContext.getContentResolver(), "com.bbk.VoiceAssistant.TIP", -1);
        return val == 1;
    }
}